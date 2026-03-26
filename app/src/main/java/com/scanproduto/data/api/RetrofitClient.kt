package com.scanproduto.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton para criação do cliente Retrofit.
 * Configurável com a URL base da API.
 */
object RetrofitClient {

    private const val TIMEOUT_SEGUNDOS = 15L

    /**
     * Cria uma instância do ProdutoApiService com a URL fornecida.
     * Permite trocar a URL base em tempo de execução (configuração pelo usuário).
     */
    fun criarServico(baseUrl: String): ProdutoApiService {
        val okHttpClient = construirOkHttpClient()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ProdutoApiService::class.java)
    }

    /**
     * Constrói o cliente HTTP com interceptores de log e timeout configurados.
     */
    private fun construirOkHttpClient(): OkHttpClient {
        // Interceptor de log para debug (remover em produção)
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            // Interceptor para adicionar headers padrão
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .build()
    }
}
