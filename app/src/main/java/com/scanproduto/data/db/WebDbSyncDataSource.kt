package com.scanproduto.data.db

import android.util.Log
import com.scanproduto.model.AppConfig
import com.scanproduto.model.Produto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager

/**
 * Conexão direta ao banco PostgreSQL via JDBC.
 */
class WebDbSyncDataSource {

    companion object {
        private var driverRegistrado = false

        private fun registrarDriver() {
            if (!driverRegistrado) {
                try {
                    Class.forName("org.postgresql.Driver")
                    driverRegistrado = true
                    Log.d("WebDbSync", "Driver PostgreSQL registrado com sucesso")
                } catch (t: Throwable) {
                    Log.e("WebDbSync", "Falha ao registrar driver: ${t::class.java.simpleName}: ${t.message}", t)
                    throw Exception("Driver PostgreSQL não disponível: ${t.message}")
                }
            }
        }
    }

    private fun getConnection(config: AppConfig): Connection {
        registrarDriver()
        val url = "jdbc:postgresql://${config.webDbUrl}:${config.webDbPorta}/${config.webDbNome}" +
                "?ssl=false&sslmode=disable&socketTimeout=30&connectTimeout=15"
        return DriverManager.getConnection(url, config.webDbUsuario, config.webDbSenha)
    }

    /**
     * Consulta produto por EAN no PostgreSQL.
     * Filtra por empresa_id se configurado.
     */
    suspend fun buscarProdutoPorEan(ean: String, config: AppConfig): Produto? = withContext(Dispatchers.IO) {
        var conn: Connection? = null
        try {
            conn = getConnection(config)

            val sql: String
            val paramIndex: Int

            if (config.webDbUsarSinonimo) {
                // Query com JOIN na tabela sinônimo
                val filtroEmpresa = if (config.webDbEmpresaId.isNotBlank())
                    "AND P.${config.webDbCampoEmpresaId} = ?" else ""

                sql = """
                    SELECT B.${config.webDbCampoSinonimoEan} AS ean_col,
                           P.${config.webDbCampoCodigo},
                           P.${config.webDbCampoDescricao},
                           P.${config.webDbCampoEstoque},
                           P.${config.webDbCampoPreco}
                    FROM ${config.webDbTabela} P
                    INNER JOIN ${config.webDbTabelaSinonimo} B
                        ON (P.${config.webDbCampoChave} = B.${config.webDbCampoChave})
                    WHERE B.${config.webDbCampoSinonimoEan} = ? $filtroEmpresa
                    LIMIT 1
                """.trimIndent()
                paramIndex = 2
            } else {
                // Query direta na tabela principal
                val filtroEmpresa = if (config.webDbEmpresaId.isNotBlank())
                    "AND ${config.webDbCampoEmpresaId} = ?" else ""

                sql = """
                    SELECT ${config.webDbCampoEan},
                           ${config.webDbCampoCodigo},
                           ${config.webDbCampoDescricao},
                           ${config.webDbCampoEstoque},
                           ${config.webDbCampoPreco}
                    FROM ${config.webDbTabela}
                    WHERE ${config.webDbCampoEan} = ? $filtroEmpresa
                    LIMIT 1
                """.trimIndent()
                paramIndex = 2
            }

            val stmt = conn.prepareStatement(sql)
            stmt.setString(1, ean)
            if (config.webDbEmpresaId.isNotBlank()) stmt.setString(paramIndex, config.webDbEmpresaId)

            val rs = stmt.executeQuery()
            if (rs.next()) {
                val eanCol = if (config.webDbUsarSinonimo) "ean_col" else config.webDbCampoEan
                Produto(
                    ean = rs.getString(eanCol) ?: ean,
                    codigo = rs.getString(config.webDbCampoCodigo) ?: "",
                    descricao = rs.getString(config.webDbCampoDescricao) ?: "",
                    estoque = rs.getInt(config.webDbCampoEstoque),
                    preco = rs.getDouble(config.webDbCampoPreco),
                    ultimaAtualizacao = System.currentTimeMillis()
                )
            } else null
        } catch (t: Throwable) {
            throw Exception("Erro na consulta: ${t.message}")
        } finally {
            try { conn?.close() } catch (_: Throwable) {}
        }
    }

    /**
     * Testa a conexão com o PostgreSQL.
     */
    suspend fun testarConexao(config: AppConfig): Boolean = withContext(Dispatchers.IO) {
        var conn: Connection? = null
        try {
            conn = getConnection(config)
            conn.isValid(5)
        } catch (t: Throwable) {
            Log.e("WebDbSync", "Erro ao testar conexão: ${t::class.java.simpleName}: ${t.message}", t)
            false
        } finally {
            try { conn?.close() } catch (_: Throwable) {}
        }
    }
}
