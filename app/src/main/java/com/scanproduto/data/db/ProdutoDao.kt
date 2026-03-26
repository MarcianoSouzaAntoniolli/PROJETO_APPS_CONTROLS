package com.scanproduto.data.db

import androidx.room.*
import com.scanproduto.model.Produto

/**
 * DAO (Data Access Object) para operações de banco de dados Room.
 * Cache local de produtos para funcionamento offline.
 */
@Dao
interface ProdutoDao {

    /**
     * Busca produto pelo EAN no cache local
     */
    @Query("SELECT * FROM produtos WHERE ean = :ean LIMIT 1")
    suspend fun buscarPorEan(ean: String): Produto?

    /**
     * Busca produto pelo código interno no cache local
     */
    @Query("SELECT * FROM produtos WHERE codigo = :codigo LIMIT 1")
    suspend fun buscarPorCodigo(codigo: String): Produto?

    /**
     * Insere ou substitui produto no cache
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(produto: Produto)

    /**
     * Insere lista de produtos no cache (importação em batch)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirTodos(produtos: List<Produto>)

    /**
     * Atualiza produto existente no cache
     */
    @Update
    suspend fun atualizar(produto: Produto)

    /**
     * Remove produto do cache pelo EAN
     */
    @Query("DELETE FROM produtos WHERE ean = :ean")
    suspend fun deletarPorEan(ean: String)

    /**
     * Retorna todos os produtos do cache
     */
    @Query("SELECT * FROM produtos ORDER BY descricao ASC")
    suspend fun listarTodos(): List<Produto>

    /**
     * Conta o total de produtos em cache
     */
    @Query("SELECT COUNT(*) FROM produtos")
    suspend fun contarProdutos(): Int

    /**
     * Remove produtos com cache expirado (anterior ao timestamp fornecido)
     */
    @Query("DELETE FROM produtos WHERE ultimaAtualizacao < :timestampMinimo")
    suspend fun limparCacheExpirado(timestampMinimo: Long)

    /**
     * Remove todos os produtos do cache
     */
    @Query("DELETE FROM produtos")
    suspend fun limparCache()
}
