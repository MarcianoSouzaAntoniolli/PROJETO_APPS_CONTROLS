package com.scanproduto

import android.app.Application

/**
 * Classe Application do ScanProduto.
 * Ponto de entrada global para inicialização de dependências.
 */
class ScanProdutoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Inicializações globais aqui se necessário (ex: Timber, analytics)
    }
}
