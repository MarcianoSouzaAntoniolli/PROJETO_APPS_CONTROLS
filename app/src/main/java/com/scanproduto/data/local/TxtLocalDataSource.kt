package com.scanproduto.data.local

import android.content.Context
import android.net.Uri
import com.scanproduto.model.Produto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * DataSource para leitura de arquivo TXT local com produtos.
 *
 * Layout esperado do arquivo:
 * EAN;CODIGO;DESCRICAO;ESTOQUE;PRECO
 * 7891234567890;001;Arroz 5kg;100;29.90
 * 7890000000001;002;Feijão 1kg;50;8.50
 *
 * Separador: ponto-e-vírgula (;)
 * Primeira linha pode ser cabeçalho (ignorada se começar com letras)
 */
class TxtLocalDataSource(private val context: Context) {

    companion object {
        // Índices das colunas no arquivo TXT
        private const val COL_EAN = 0
        private const val COL_CODIGO = 1
        private const val COL_DESCRICAO = 2
        private const val COL_ESTOQUE = 3
        private const val COL_PRECO = 4

        private const val SEPARADOR = ";"
        private const val NUM_COLUNAS = 5
    }

    /**
     * Lê e parseia arquivo TXT a partir de uma URI (selecionado pelo usuário).
     * Retorna lista de produtos parseados e ignora linhas inválidas.
     */
    suspend fun lerArquivo(uri: Uri): Result<List<Produto>> = withContext(Dispatchers.IO) {
        try {
            val produtos = mutableListOf<Produto>()
            val erros = mutableListOf<String>()

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var numeroLinha = 0
                    var linha: String?

                    while (reader.readLine().also { linha = it } != null) {
                        numeroLinha++
                        val linhaAtual = linha ?: continue

                        // Ignora linhas vazias
                        if (linhaAtual.isBlank()) continue

                        // Ignora cabeçalho (linha que começa com EAN ou CODIGO)
                        if (numeroLinha == 1 && linhaAtual.uppercase().startsWith("EAN")) continue

                        // Parseia a linha
                        parsearLinha(linhaAtual, numeroLinha)
                            .onSuccess { produto -> produtos.add(produto) }
                            .onFailure { erro -> erros.add("Linha $numeroLinha: ${erro.message}") }
                    }
                }
            } ?: return@withContext Result.failure(Exception("Não foi possível abrir o arquivo"))

            if (produtos.isEmpty() && erros.isNotEmpty()) {
                Result.failure(Exception("Nenhum produto válido encontrado. Erros:\n${erros.take(5).joinToString("\n")}"))
            } else {
                Result.success(produtos)
            }

        } catch (e: Exception) {
            Result.failure(Exception("Erro ao ler arquivo: ${e.message}"))
        }
    }

    /**
     * Busca um produto específico pelo EAN em um arquivo TXT.
     * Percorre o arquivo linha a linha até encontrar o EAN.
     */
    suspend fun buscarPorEan(uri: Uri, ean: String): Produto? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var linha: String?
                    var primeiraLinha = true

                    while (reader.readLine().also { linha = it } != null) {
                        val linhaAtual = linha ?: continue

                        // Ignora cabeçalho
                        if (primeiraLinha) {
                            primeiraLinha = false
                            if (linhaAtual.uppercase().startsWith("EAN")) continue
                        }

                        val colunas = linhaAtual.split(SEPARADOR)
                        if (colunas.size >= NUM_COLUNAS && colunas[COL_EAN].trim() == ean) {
                            return@withContext parsearLinha(linhaAtual, 0).getOrNull()
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parseia uma linha do arquivo TXT para um objeto Produto.
     */
    private fun parsearLinha(linha: String, numeroLinha: Int): Result<Produto> {
        return try {
            val colunas = linha.split(SEPARADOR)

            if (colunas.size < NUM_COLUNAS) {
                return Result.failure(
                    Exception("Esperado $NUM_COLUNAS colunas, encontrado ${colunas.size}")
                )
            }

            val ean = colunas[COL_EAN].trim()
            val codigo = colunas[COL_CODIGO].trim()
            val descricao = colunas[COL_DESCRICAO].trim()
            val estoqueStr = colunas[COL_ESTOQUE].trim()
            val precoStr = colunas[COL_PRECO].trim()

            // Validações básicas
            if (ean.isBlank()) return Result.failure(Exception("EAN vazio"))
            if (descricao.isBlank()) return Result.failure(Exception("Descrição vazia"))

            // Parseia estoque (aceita inteiro)
            val estoque = estoqueStr.toIntOrNull()
                ?: return Result.failure(Exception("Estoque inválido: '$estoqueStr'"))

            // Parseia preço (aceita vírgula ou ponto como separador decimal)
            val precoNormalizado = precoStr.replace(",", ".")
            val preco = precoNormalizado.toDoubleOrNull()
                ?: return Result.failure(Exception("Preço inválido: '$precoStr'"))

            Result.success(
                Produto(
                    ean = ean,
                    codigo = codigo,
                    descricao = descricao,
                    estoque = estoque,
                    preco = preco
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Erro ao parsear linha: ${e.message}"))
        }
    }
}
