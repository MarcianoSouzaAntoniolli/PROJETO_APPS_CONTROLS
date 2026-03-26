package com.scanproduto.data.api

import com.scanproduto.model.Produto
import retrofit2.Response
import retrofit2.http.*

/**
 * Interface Retrofit para a API REST de produtos.
 * Define todos os endpoints disponíveis.
 */
interface ProdutoApiService {

    /**
     * Busca produto pelo código EAN
     * GET /produto/{ean}
     */
    @GET("produto/{ean}")
    suspend fun getProdutoPorEan(
        @Path("ean") ean: String
    ): Response<Produto>

    /**
     * Busca produto pelo código interno
     * GET /produto/codigo/{codigo}
     */
    @GET("produto/codigo/{codigo}")
    suspend fun getProdutoPorCodigo(
        @Path("codigo") codigo: String
    ): Response<Produto>

    /**
     * Lista todos os produtos (paginado)
     * GET /produtos?page=0&size=50
     */
    @GET("produtos")
    suspend fun listarProdutos(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<List<Produto>>

    /**
     * Cadastra ou atualiza um produto
     * POST /produto
     */
    @POST("produto")
    suspend fun salvarProduto(
        @Body produto: Produto
    ): Response<Produto>

    /**
     * Atualiza um produto existente
     * PUT /produto/{ean}
     */
    @PUT("produto/{ean}")
    suspend fun atualizarProduto(
        @Path("ean") ean: String,
        @Body produto: Produto
    ): Response<Produto>

    /**
     * Envia lista de produtos em batch
     * POST /produtos/batch
     */
    @POST("produtos/batch")
    suspend fun salvarProdutosBatch(
        @Body produtos: List<Produto>
    ): Response<Map<String, Int>>
}
