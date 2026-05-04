package com.scanproduto.data.db

import com.scanproduto.model.Produto
import com.scanproduto.utils.TextoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * DataSource para acesso ao banco de dados SQLite local via Room.
 */
class SQLiteDataSource(private val produtoDao: ProdutoDao) {

    /**
     * Busca produto pelo EAN no banco SQLite local.
     * Retorna null se não encontrado.
     */
    suspend fun buscarProdutoPorEan(ean: String): Produto? = withContext(Dispatchers.IO) {
        produtoDao.buscarPorEan(ean)
    }

    /**
     * Busca produtos por parte da descrição no banco local.
     * Insensível a acentuação: "racao", "ração" e "Racão" retornam os mesmos resultados.
     */
    suspend fun buscarPorDescricao(termo: String): List<Produto> = withContext(Dispatchers.IO) {
        val termoNorm = TextoUtils.normalizar(termo)
        produtoDao.listarTodos()
            .filter { TextoUtils.normalizar(it.descricao).contains(termoNorm) }
            .sortedBy { it.descricao }
            .take(30)
    }

    /**
     * Verifica se o banco SQLite está acessível.
     * SQLite local está sempre disponível.
     */
    suspend fun testarConexao(): Boolean = withContext(Dispatchers.IO) {
        try {
            produtoDao.contarProdutos()
            true
        } catch (e: Exception) {
            false
        }
    }
}