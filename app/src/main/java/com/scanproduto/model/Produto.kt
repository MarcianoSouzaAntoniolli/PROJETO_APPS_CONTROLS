package com.scanproduto.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * Modelo de dados do Produto.
 * Usado tanto para resposta da API (via Gson) quanto para cache local (via Room).
 */
@Entity(tableName = "produtos")
data class Produto(

    @PrimaryKey
    @SerializedName("ean")
    val ean: String,

    @SerializedName("codigo")
    val codigo: String,

    @SerializedName("descricao")
    val descricao: String,

    @SerializedName("estoque")
    val estoque: Int,

    @SerializedName("preco")
    val preco: Double,

    // Timestamp do último acesso (para cache)
    val ultimaAtualizacao: Long = System.currentTimeMillis()
)
