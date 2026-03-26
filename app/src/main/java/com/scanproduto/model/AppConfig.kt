package com.scanproduto.model

/**
 * Configurações de conexão do aplicativo.
 * Salvas em SharedPreferences.
 */
data class AppConfig(
    // Fonte de dados principal
    val fonteConexao: FonteConexao = FonteConexao.API,

    // Configurações de API REST
    val apiBaseUrl: String = "http://192.168.1.100:8080/",

    // Configurações de banco PostgreSQL
    val dbHost: String = "",
    val dbPort: Int = 5432,
    val dbNome: String = "",
    val dbUsuario: String = "",
    val dbSenha: String = "",

    // Configurações de banco PostgreSQL direto (web)
    val webDbUrl: String = "",
    val webDbPorta: Int = 5432,
    val webDbNome: String = "",
    val webDbUsuario: String = "",
    val webDbSenha: String = "",
    val webDbTabela: String = "",
    val webDbCampoEan: String = "ean",
    val webDbCampoCodigo: String = "codigo",
    val webDbCampoDescricao: String = "descricao",
    val webDbCampoEstoque: String = "estoque",
    val webDbCampoPreco: String = "preco",
    val webDbCampoEmpresaId: String = "empresa_id",
    val webDbEmpresaId: String = "",

    // Tabela sinônimo (EAN alternativo)
    val webDbUsarSinonimo: Boolean = false,
    val webDbTabelaSinonimo: String = "sinonimo",
    val webDbCampoSinonimoEan: String = "cod_barr",
    val webDbCampoChave: String = "codipro",

    // Caminho do arquivo TXT local
    val arquivoTxtPath: String = "",

    // Cache offline ativo
    val usarCacheOffline: Boolean = true,

    // Timeout de cache em minutos (0 = sem expiração)
    val cacheTtlMinutos: Int = 60
)

/**
 * Enum para fonte de dados preferencial
 */
enum class FonteConexao(val label: String) {
    API("API REST"),
    BANCO_DADOS("Banco SQLite Local"),
    ARQUIVO_TXT("Arquivo TXT Local"),
    BANCO_WEB("PostgreSQL Direto")
}
