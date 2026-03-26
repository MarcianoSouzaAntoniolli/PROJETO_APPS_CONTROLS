package com.scanproduto.model

/**
 * Classe selada para representar estados de resultado de operações assíncronas.
 * Padrão Resource Pattern para MVVM.
 */
sealed class Resource<out T> {

    /** Operação bem-sucedida com dados */
    data class Success<T>(val data: T) : Resource<T>()

    /** Erro com mensagem e dados opcionais */
    data class Error(val message: String, val throwable: Throwable? = null) : Resource<Nothing>()

    /** Carregando */
    object Loading : Resource<Nothing>()

    /** Estado inicial / vazio */
    object Empty : Resource<Nothing>()
}
