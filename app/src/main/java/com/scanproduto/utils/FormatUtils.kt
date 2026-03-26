package com.scanproduto.utils

import java.text.NumberFormat
import java.util.Locale

/**
 * Utilitários de formatação de valores para exibição na UI.
 */
object FormatUtils {

    private val localeBR = Locale("pt", "BR")
    private val formatoMoeda = NumberFormat.getCurrencyInstance(localeBR)
    private val formatoNumero = NumberFormat.getNumberInstance(localeBR)

    /**
     * Formata um Double como moeda brasileira (R$ 29,90).
     */
    fun formatarPreco(preco: Double): String {
        return formatoMoeda.format(preco)
    }

    /**
     * Formata estoque com indicador de baixo estoque.
     */
    fun formatarEstoque(estoque: Int): String {
        return when {
            estoque <= 0 -> "⚠ Sem estoque"
            estoque < 10 -> "⚠ $estoque un (baixo)"
            else -> "$estoque un"
        }
    }
}
