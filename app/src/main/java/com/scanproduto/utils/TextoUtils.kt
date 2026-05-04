package com.scanproduto.utils

import java.text.Normalizer

object TextoUtils {

    /**
     * Remove acentos e converte para minúsculas.
     * Usado para busca insensível a acentuação.
     * Ex: "Ração" → "racao", "Café" → "cafe"
     */
    fun normalizar(texto: String): String =
        Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .lowercase()
}
