package com.scanproduto.data.db

import com.scanproduto.model.Produto
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