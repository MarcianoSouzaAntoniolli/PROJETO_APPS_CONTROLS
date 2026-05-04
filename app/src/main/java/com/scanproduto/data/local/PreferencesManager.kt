package com.scanproduto.data.local

import android.content.Context
import android.content.SharedPreferences
import com.scanproduto.model.AppConfig
import com.scanproduto.model.FonteConexao

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "scanproduto_prefs"

        private const val KEY_FONTE_CONEXAO = "fonte_conexao"
        private const val KEY_API_BASE_URL = "api_base_url"
        private const val KEY_DB_HOST = "db_host"
        private const val KEY_DB_PORT = "db_port"
        private const val KEY_DB_NOME = "db_nome"
        private const val KEY_DB_USUARIO = "db_usuario"
        private const val KEY_DB_SENHA = "db_senha"
        private const val KEY_ARQUIVO_TXT = "arquivo_txt_path"
        private const val KEY_USAR_CACHE = "usar_cache_offline"
        private const val KEY_CACHE_TTL = "cache_ttl_minutos"

        private const val KEY_WEB_DB_URL = "web_db_url"
        private const val KEY_WEB_DB_PORTA = "web_db_porta"
        private const val KEY_WEB_DB_NOME = "web_db_nome"
        private const val KEY_WEB_DB_USUARIO = "web_db_usuario"
        private const val KEY_WEB_DB_SENHA = "web_db_senha"
        private const val KEY_WEB_DB_TABELA = "web_db_tabela"
        private const val KEY_WEB_DB_CAMPO_EAN = "web_db_campo_ean"
        private const val KEY_WEB_DB_CAMPO_CODIGO = "web_db_campo_codigo"
        private const val KEY_WEB_DB_CAMPO_DESCRICAO = "web_db_campo_descricao"
        private const val KEY_WEB_DB_CAMPO_ESTOQUE = "web_db_campo_estoque"
        private const val KEY_WEB_DB_CAMPO_PRECO = "web_db_campo_preco"
        private const val KEY_WEB_DB_CAMPO_EMPRESA_ID = "web_db_campo_empresa_id"
        private const val KEY_WEB_DB_EMPRESA_ID = "web_db_empresa_id"
        private const val KEY_HABILITAR_BUSCA_DESCRICAO = "habilitar_busca_descricao"
        private const val KEY_WEB_DB_USAR_SINONIMO = "web_db_usar_sinonimo"
        private const val KEY_WEB_DB_TABELA_SINONIMO = "web_db_tabela_sinonimo"
        private const val KEY_WEB_DB_CAMPO_SINONIMO_EAN = "web_db_campo_sinonimo_ean"
        private const val KEY_WEB_DB_CAMPO_CHAVE = "web_db_campo_chave"

        private const val DEFAULT_API_URL = "http://192.168.1.100:8080/"
        private const val DEFAULT_DB_PORT = 5432
        private const val DEFAULT_WEB_DB_PORTA = 5432
        private const val DEFAULT_CACHE_TTL = 60
    }

    fun salvarConfig(config: AppConfig) {
        prefs.edit().apply {
            putString(KEY_FONTE_CONEXAO, config.fonteConexao.name)
            putString(KEY_API_BASE_URL, config.apiBaseUrl)
            putString(KEY_DB_HOST, config.dbHost)
            putInt(KEY_DB_PORT, config.dbPort)
            putString(KEY_DB_NOME, config.dbNome)
            putString(KEY_DB_USUARIO, config.dbUsuario)
            putString(KEY_DB_SENHA, config.dbSenha)
            putString(KEY_ARQUIVO_TXT, config.arquivoTxtPath)
            putBoolean(KEY_USAR_CACHE, config.usarCacheOffline)
            putInt(KEY_CACHE_TTL, config.cacheTtlMinutos)
            putString(KEY_WEB_DB_URL, config.webDbUrl)
            putInt(KEY_WEB_DB_PORTA, config.webDbPorta)
            putString(KEY_WEB_DB_NOME, config.webDbNome)
            putString(KEY_WEB_DB_USUARIO, config.webDbUsuario)
            putString(KEY_WEB_DB_SENHA, config.webDbSenha)
            putString(KEY_WEB_DB_TABELA, config.webDbTabela)
            putString(KEY_WEB_DB_CAMPO_EAN, config.webDbCampoEan)
            putString(KEY_WEB_DB_CAMPO_CODIGO, config.webDbCampoCodigo)
            putString(KEY_WEB_DB_CAMPO_DESCRICAO, config.webDbCampoDescricao)
            putString(KEY_WEB_DB_CAMPO_ESTOQUE, config.webDbCampoEstoque)
            putString(KEY_WEB_DB_CAMPO_PRECO, config.webDbCampoPreco)
            putString(KEY_WEB_DB_CAMPO_EMPRESA_ID, config.webDbCampoEmpresaId)
            putString(KEY_WEB_DB_EMPRESA_ID, config.webDbEmpresaId)
            putBoolean(KEY_HABILITAR_BUSCA_DESCRICAO, config.habilitarBuscaPorDescricao)
            putBoolean(KEY_WEB_DB_USAR_SINONIMO, config.webDbUsarSinonimo)
            putString(KEY_WEB_DB_TABELA_SINONIMO, config.webDbTabelaSinonimo)
            putString(KEY_WEB_DB_CAMPO_SINONIMO_EAN, config.webDbCampoSinonimoEan)
            putString(KEY_WEB_DB_CAMPO_CHAVE, config.webDbCampoChave)
            apply()
        }
    }

    fun carregarConfig(): AppConfig {
        val fonteNome = prefs.getString(KEY_FONTE_CONEXAO, FonteConexao.API.name)
        val fonte = try {
            FonteConexao.valueOf(fonteNome ?: FonteConexao.API.name)
        } catch (e: IllegalArgumentException) {
            FonteConexao.API
        }

        return AppConfig(
            fonteConexao = fonte,
            apiBaseUrl = prefs.getString(KEY_API_BASE_URL, DEFAULT_API_URL) ?: DEFAULT_API_URL,
            dbHost = prefs.getString(KEY_DB_HOST, "") ?: "",
            dbPort = prefs.getInt(KEY_DB_PORT, DEFAULT_DB_PORT),
            dbNome = prefs.getString(KEY_DB_NOME, "") ?: "",
            dbUsuario = prefs.getString(KEY_DB_USUARIO, "") ?: "",
            dbSenha = prefs.getString(KEY_DB_SENHA, "") ?: "",
            arquivoTxtPath = prefs.getString(KEY_ARQUIVO_TXT, "") ?: "",
            usarCacheOffline = prefs.getBoolean(KEY_USAR_CACHE, true),
            cacheTtlMinutos = prefs.getInt(KEY_CACHE_TTL, DEFAULT_CACHE_TTL),
            webDbUrl = prefs.getString(KEY_WEB_DB_URL, "") ?: "",
            webDbPorta = prefs.getInt(KEY_WEB_DB_PORTA, DEFAULT_WEB_DB_PORTA),
            webDbNome = prefs.getString(KEY_WEB_DB_NOME, "") ?: "",
            webDbUsuario = prefs.getString(KEY_WEB_DB_USUARIO, "") ?: "",
            webDbSenha = prefs.getString(KEY_WEB_DB_SENHA, "") ?: "",
            webDbTabela = prefs.getString(KEY_WEB_DB_TABELA, "") ?: "",
            webDbCampoEan = prefs.getString(KEY_WEB_DB_CAMPO_EAN, "ean") ?: "ean",
            webDbCampoCodigo = prefs.getString(KEY_WEB_DB_CAMPO_CODIGO, "codigo") ?: "codigo",
            webDbCampoDescricao = prefs.getString(KEY_WEB_DB_CAMPO_DESCRICAO, "descricao") ?: "descricao",
            webDbCampoEstoque = prefs.getString(KEY_WEB_DB_CAMPO_ESTOQUE, "estoque") ?: "estoque",
            webDbCampoPreco = prefs.getString(KEY_WEB_DB_CAMPO_PRECO, "preco") ?: "preco",
            webDbCampoEmpresaId = prefs.getString(KEY_WEB_DB_CAMPO_EMPRESA_ID, "empresa_id") ?: "empresa_id",
            webDbEmpresaId = prefs.getString(KEY_WEB_DB_EMPRESA_ID, "") ?: "",
            habilitarBuscaPorDescricao = prefs.getBoolean(KEY_HABILITAR_BUSCA_DESCRICAO, false),
            webDbUsarSinonimo = prefs.getBoolean(KEY_WEB_DB_USAR_SINONIMO, false),
            webDbTabelaSinonimo = prefs.getString(KEY_WEB_DB_TABELA_SINONIMO, "sinonimo") ?: "sinonimo",
            webDbCampoSinonimoEan = prefs.getString(KEY_WEB_DB_CAMPO_SINONIMO_EAN, "cod_barr") ?: "cod_barr",
            webDbCampoChave = prefs.getString(KEY_WEB_DB_CAMPO_CHAVE, "codipro") ?: "codipro"
        )
    }
}
