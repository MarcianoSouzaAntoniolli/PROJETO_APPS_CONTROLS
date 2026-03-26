package com.scanproduto.data.repository

import android.net.Uri
import com.scanproduto.data.api.ProdutoApiService
import com.scanproduto.data.api.RetrofitClient
import com.scanproduto.data.db.AppDatabase
import com.scanproduto.data.db.SQLiteDataSource
import com.scanproduto.data.db.WebDbSyncDataSource
import com.scanproduto.data.local.PreferencesManager
import com.scanproduto.data.local.TxtLocalDataSource
import com.scanproduto.model.AppConfig
import com.scanproduto.model.FonteConexao
import com.scanproduto.model.Produto
import com.scanproduto.model.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositório principal de produtos.
 * Orquestra as 3 fontes de dados: API REST, PostgreSQL e TXT local.
 * Implementa cache com Room para funcionamento offline.
 */
class ProdutoRepository(
    private val database: AppDatabase,
    private val prefsManager: PreferencesManager,
    private val txtDataSource: TxtLocalDataSource
) {

    private val produtoDao = database.produtoDao()

    // URI do arquivo TXT selecionado pelo usuário (memória apenas)
    var arquivoTxtUri: Uri? = null

    /**
     * Busca produto pelo EAN.
     * Segue a fonte configurada nas preferências com fallback para cache.
     */
    suspend fun buscarProdutoPorEan(ean: String): Resource<Produto> {
        val config = prefsManager.carregarConfig()

        // 1. Tenta buscar na fonte principal configurada
        val resultado = buscarNaFontePrincipal(ean, config)

        return when (resultado) {
            is Resource.Success -> {
                // Salva no cache para uso offline
                if (config.usarCacheOffline) {
                    salvarNoCache(resultado.data, config)
                }
                resultado
            }

            is Resource.Error -> {
                // Falhou na fonte principal - tenta o cache local
                if (config.usarCacheOffline) {
                    buscarNoCache(ean, config)
                        ?: resultado // Retorna o erro original se não há cache
                } else {
                    resultado
                }
            }

            else -> resultado
        }
    }

    /**
     * Busca na fonte de dados configurada pelo usuário.
     */
    private suspend fun buscarNaFontePrincipal(
        ean: String,
        config: AppConfig
    ): Resource<Produto> {
        return when (config.fonteConexao) {
            FonteConexao.API -> buscarViaApi(ean, config)
            FonteConexao.BANCO_DADOS -> buscarViaSQLite(ean)
            FonteConexao.ARQUIVO_TXT -> buscarViaTxt(ean)
            FonteConexao.BANCO_WEB -> buscarViaWebDb(ean, config)
        }
    }

    /**
     * Busca produto via API REST com Retrofit.
     */
    private suspend fun buscarViaApi(ean: String, config: AppConfig): Resource<Produto> {
        return try {
            val apiService: ProdutoApiService = RetrofitClient.criarServico(config.apiBaseUrl)
            val response = apiService.getProdutoPorEan(ean)

            when {
                response.isSuccessful -> {
                    val produto = response.body()
                    if (produto != null) {
                        Resource.Success(produto)
                    } else {
                        Resource.Error("Produto não encontrado (resposta vazia)")
                    }
                }

                response.code() == 404 -> {
                    Resource.Error("Produto com EAN $ean não encontrado")
                }

                response.code() == 401 || response.code() == 403 -> {
                    Resource.Error("Acesso não autorizado à API")
                }

                else -> {
                    Resource.Error("Erro na API: HTTP ${response.code()} - ${response.message()}")
                }
            }
        } catch (e: java.net.ConnectException) {
            Resource.Error("Sem conexão com a API. Verifique o endereço: ${config.apiBaseUrl}", e)
        } catch (e: java.net.SocketTimeoutException) {
            Resource.Error("Timeout: A API demorou muito para responder", e)
        } catch (e: Exception) {
            Resource.Error("Erro ao consultar API: ${e.message}", e)
        }
    }

    /**
     * Busca produto no banco de dados SQLite local via Room.
     */
    private suspend fun buscarViaSQLite(ean: String): Resource<Produto> {
        return try {
            val sqliteDs = SQLiteDataSource(produtoDao)
            val produto = sqliteDs.buscarProdutoPorEan(ean)

            if (produto != null) {
                Resource.Success(produto)
            } else {
                Resource.Error("Produto com EAN $ean não encontrado no banco de dados local")
            }
        } catch (e: Exception) {
            Resource.Error("Erro ao consultar banco de dados local: ${e.message}", e)
        }
    }

    /**
     * Busca produto em arquivo TXT local.
     */
    private suspend fun buscarViaTxt(ean: String): Resource<Produto> {
        val uri = arquivoTxtUri
            ?: return Resource.Error("Nenhum arquivo TXT selecionado. Acesse Configurações para selecionar.")

        val produto = txtDataSource.buscarPorEan(uri, ean)

        return if (produto != null) {
            Resource.Success(produto)
        } else {
            Resource.Error("Produto com EAN $ean não encontrado no arquivo TXT")
        }
    }

    /**
     * Busca produto no cache local (Room) com verificação de TTL.
     */
    private suspend fun buscarNoCache(ean: String, config: AppConfig): Resource<Produto>? {
        val produto = produtoDao.buscarPorEan(ean) ?: return null

        // Verifica se o cache expirou (se TTL > 0)
        if (config.cacheTtlMinutos > 0) {
            val ttlMs = config.cacheTtlMinutos * 60 * 1000L
            val agora = System.currentTimeMillis()
            if (agora - produto.ultimaAtualizacao > ttlMs) {
                // Cache expirado
                return null
            }
        }

        return Resource.Success(produto.copy(
            descricao = "⚠ (Cache) ${produto.descricao}"
        ))
    }

    /**
     * Salva produto no cache local.
     */
    private suspend fun salvarNoCache(produto: Produto, config: AppConfig) {
        try {
            produtoDao.inserir(produto.copy(ultimaAtualizacao = System.currentTimeMillis()))
        } catch (e: Exception) {
            // Silencia erros de cache - não deve afetar o fluxo principal
        }
    }

    /**
     * Importa lista de produtos do arquivo TXT para o cache local.
     */
    suspend fun importarDoTxt(uri: Uri): Resource<Int> {
        arquivoTxtUri = uri

        return try {
            val resultado = txtDataSource.lerArquivo(uri)

            resultado.fold(
                onSuccess = { produtos ->
                    produtoDao.inserirTodos(produtos)
                    Resource.Success(produtos.size)
                },
                onFailure = { erro ->
                    Resource.Error(erro.message ?: "Erro ao importar arquivo")
                }
            )
        } catch (e: Exception) {
            Resource.Error("Erro ao importar: ${e.message}", e)
        }
    }

    /**
     * Envia lista de produtos para a API (cadastro em batch).
     */
    suspend fun enviarProdutosParaApi(produtos: List<Produto>): Resource<Int> {
        return try {
            val config = prefsManager.carregarConfig()
            val apiService = RetrofitClient.criarServico(config.apiBaseUrl)
            val response = apiService.salvarProdutosBatch(produtos)

            if (response.isSuccessful) {
                val total = response.body()?.get("total") ?: produtos.size
                Resource.Success(total)
            } else {
                Resource.Error("Erro ao enviar para API: HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            Resource.Error("Erro de conexão ao enviar produtos: ${e.message}", e)
        }
    }

    /**
     * Retorna o total de produtos no cache local.
     */
    suspend fun contarProdutosEmCache(): Int = produtoDao.contarProdutos()

    /**
     * Limpa o cache local de produtos.
     */
    suspend fun limparCache() = produtoDao.limparCache()

    /**
     * Testa a conexão com a API configurada.
     */
    suspend fun testarConexaoApi(): Boolean {
        return try {
            val config = prefsManager.carregarConfig()
            val apiService = RetrofitClient.criarServico(config.apiBaseUrl)
            val response = apiService.listarProdutos(page = 0, size = 1)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Testa o acesso ao banco de dados SQLite local.
     */
    suspend fun testarConexaoSQLite(): Boolean = withContext(Dispatchers.IO) {
        try {
            SQLiteDataSource(produtoDao).testarConexao()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Consulta direta ao banco web por EAN, sem gravar localmente.
     */
    private suspend fun buscarViaWebDb(ean: String, config: AppConfig): Resource<Produto> {
        return try {
            if (config.webDbUrl.isBlank()) {
                return Resource.Error("URL do banco web não configurada")
            }
            val produto = WebDbSyncDataSource().buscarProdutoPorEan(ean, config)
            if (produto != null) {
                Resource.Success(produto)
            } else {
                Resource.Error("Produto com EAN $ean não encontrado no banco web")
            }
        } catch (e: Exception) {
            Resource.Error("Erro ao consultar banco web: ${e.message}", e)
        }
    }

    suspend fun testarConexaoWebDb(): Boolean = withContext(Dispatchers.IO) {
        try {
            val config = prefsManager.carregarConfig()
            if (config.webDbUrl.isBlank()) return@withContext false
            WebDbSyncDataSource().testarConexao(config)
        } catch (e: Exception) { false }
    }
}
