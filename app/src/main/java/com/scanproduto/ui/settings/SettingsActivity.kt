package com.scanproduto.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.scanproduto.R
import com.scanproduto.data.db.AppDatabase
import com.scanproduto.data.local.PreferencesManager
import com.scanproduto.data.local.TxtLocalDataSource
import com.scanproduto.data.repository.ProdutoRepository
import com.scanproduto.databinding.ActivitySettingsBinding
import com.scanproduto.model.AppConfig
import com.scanproduto.model.FonteConexao
import kotlinx.coroutines.launch

/**
 * Tela de configurações de conexão.
 * Permite configurar API, PostgreSQL e outras opções.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefsManager: PreferencesManager
    private lateinit var repository: ProdutoRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Configurações"

        prefsManager = PreferencesManager(this)
        repository = ProdutoRepository(
            AppDatabase.getInstance(this),
            prefsManager,
            TxtLocalDataSource(this)
        )

        configurarSpinnerFonte()
        carregarConfiguracoes()
        configurarListeners()
    }

    /**
     * Configura o Spinner de seleção de fonte de dados.
     */
    private fun configurarSpinnerFonte() {
        val fontes = FonteConexao.values().map { it.label }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fontes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFonte.adapter = adapter

        binding.spinnerFonte.setOnItemSelectedListener(
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    atualizarVisibilidadePaineis(FonteConexao.values()[position])
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
        )
    }

    /**
     * Mostra/esconde os painéis de configuração conforme a fonte selecionada.
     */
    private fun atualizarVisibilidadePaineis(fonte: FonteConexao) {
        binding.panelApi.visibility = if (fonte == FonteConexao.API) View.VISIBLE else View.GONE
        binding.panelDb.visibility = View.GONE
    }

    /**
     * Carrega as configurações salvas e preenche os campos.
     */
    private fun carregarConfiguracoes() {
        val config = prefsManager.carregarConfig()

        // Seleciona a fonte atual no spinner
        val fonteIndex = FonteConexao.values().indexOf(config.fonteConexao)
        binding.spinnerFonte.setSelection(if (fonteIndex >= 0) fonteIndex else 0)

        // Preenche campos da API
        binding.etApiUrl.setText(config.apiBaseUrl)

        // Preenche campos do PostgreSQL
        binding.etDbHost.setText(config.dbHost)
        binding.etDbPort.setText(config.dbPort.toString())
        binding.etDbNome.setText(config.dbNome)
        binding.etDbUsuario.setText(config.dbUsuario)
        binding.etDbSenha.setText(config.dbSenha)

        // Opções gerais
        binding.switchBuscaDescricao.isChecked = config.habilitarBuscaPorDescricao
        binding.switchCache.isChecked = config.usarCacheOffline
        binding.etCacheTtl.setText(config.cacheTtlMinutos.toString())

        // Campos PostgreSQL Direto
        binding.etWebDbUrl.setText(config.webDbUrl)
        binding.etWebDbPorta.setText(config.webDbPorta.toString())
        binding.etWebDbNome.setText(config.webDbNome)
        binding.etWebDbUsuario.setText(config.webDbUsuario)
        binding.etWebDbSenha.setText(config.webDbSenha)
        binding.etWebDbTabela.setText(config.webDbTabela)
        binding.etWebDbCampoEan.setText(config.webDbCampoEan)
        binding.etWebDbCampoCodigo.setText(config.webDbCampoCodigo)
        binding.etWebDbCampoDescricao.setText(config.webDbCampoDescricao)
        binding.etWebDbCampoEstoque.setText(config.webDbCampoEstoque)
        binding.etWebDbCampoPreco.setText(config.webDbCampoPreco)
        binding.etWebDbCampoEmpresaId.setText(config.webDbCampoEmpresaId)
        binding.etWebDbEmpresaId.setText(config.webDbEmpresaId)

        // Sinônimo
        binding.switchUsarSinonimo.isChecked = config.webDbUsarSinonimo
        binding.etWebDbTabelaSinonimo.setText(config.webDbTabelaSinonimo)
        binding.etWebDbCampoSinonimoEan.setText(config.webDbCampoSinonimoEan)
        binding.etWebDbCampoChave.setText(config.webDbCampoChave)
    }

    /**
     * Configura os listeners de ação dos botões.
     */
    private fun configurarListeners() {
        // Salvar configurações
        binding.btnSalvar.setOnClickListener {
            salvarConfiguracoes()
        }

        // Testar conexão
        binding.btnTestarConexao.setOnClickListener {
            testarConexao()
        }

        binding.btnSincronizarWebDb.setOnClickListener {
            sincronizarWebDb()
        }
    }

    /**
     * Coleta os dados dos campos e salva nas preferências.
     */
    private fun salvarConfiguracoes() {
        val fonteIndex = binding.spinnerFonte.selectedItemPosition
        val fonte = FonteConexao.values()[fonteIndex]

        val portaStr = binding.etDbPort.text.toString()
        val porta = portaStr.toIntOrNull() ?: 5432

        val cacheTtlStr = binding.etCacheTtl.text.toString()
        val cacheTtl = cacheTtlStr.toIntOrNull() ?: 60

        val config = AppConfig(
            fonteConexao = fonte,
            apiBaseUrl = binding.etApiUrl.text.toString().trim().let {
                if (!it.endsWith("/")) "$it/" else it
            },
            dbHost = binding.etDbHost.text.toString().trim(),
            dbPort = porta,
            dbNome = binding.etDbNome.text.toString().trim(),
            dbUsuario = binding.etDbUsuario.text.toString().trim(),
            dbSenha = binding.etDbSenha.text.toString(),
            habilitarBuscaPorDescricao = binding.switchBuscaDescricao.isChecked,
            usarCacheOffline = binding.switchCache.isChecked,
            cacheTtlMinutos = cacheTtl,
            webDbUrl = binding.etWebDbUrl.text.toString().trim(),
            webDbPorta = binding.etWebDbPorta.text.toString().toIntOrNull() ?: 5432,
            webDbNome = binding.etWebDbNome.text.toString().trim(),
            webDbUsuario = binding.etWebDbUsuario.text.toString().trim(),
            webDbSenha = binding.etWebDbSenha.text.toString(),
            webDbTabela = binding.etWebDbTabela.text.toString().trim(),
            webDbCampoEan = binding.etWebDbCampoEan.text.toString().trim().ifBlank { "ean" },
            webDbCampoCodigo = binding.etWebDbCampoCodigo.text.toString().trim().ifBlank { "codigo" },
            webDbCampoDescricao = binding.etWebDbCampoDescricao.text.toString().trim().ifBlank { "descricao" },
            webDbCampoEstoque = binding.etWebDbCampoEstoque.text.toString().trim().ifBlank { "estoque" },
            webDbCampoPreco = binding.etWebDbCampoPreco.text.toString().trim().ifBlank { "preco" },
            webDbCampoEmpresaId = binding.etWebDbCampoEmpresaId.text.toString().trim().ifBlank { "empresa_id" },
            webDbEmpresaId = binding.etWebDbEmpresaId.text.toString().trim(),
            webDbUsarSinonimo = binding.switchUsarSinonimo.isChecked,
            webDbTabelaSinonimo = binding.etWebDbTabelaSinonimo.text.toString().trim().ifBlank { "sinonimo" },
            webDbCampoSinonimoEan = binding.etWebDbCampoSinonimoEan.text.toString().trim().ifBlank { "cod_barr" },
            webDbCampoChave = binding.etWebDbCampoChave.text.toString().trim().ifBlank { "codipro" }
        )

        prefsManager.salvarConfig(config)

        Snackbar.make(
            binding.root,
            "Configurações salvas com sucesso!",
            Snackbar.LENGTH_SHORT
        ).show()
    }

    /**
     * Testa a conexão com a fonte configurada.
     */
    private fun testarConexao() {
        val fonteIndex = binding.spinnerFonte.selectedItemPosition
        val fonte = FonteConexao.values()[fonteIndex]

        binding.btnTestarConexao.isEnabled = false
        binding.progressTestar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val sucesso = when (fonte) {
                FonteConexao.API -> repository.testarConexaoApi()
                FonteConexao.BANCO_DADOS -> repository.testarConexaoSQLite()
                FonteConexao.ARQUIVO_TXT -> true // TXT não precisa de teste de conexão
                FonteConexao.BANCO_WEB -> repository.testarConexaoWebDb()
            }

            binding.btnTestarConexao.isEnabled = true
            binding.progressTestar.visibility = View.GONE

            val mensagem = if (sucesso) {
                "✅ Conexão bem-sucedida!"
            } else {
                "❌ Falha na conexão. Verifique as configurações."
            }

            Snackbar.make(binding.root, mensagem, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun sincronizarWebDb() {
        val url = binding.etWebDbUrl.text.toString().trim()
        if (url.isBlank()) {
            Snackbar.make(binding.root, "Informe a URL do banco web", Snackbar.LENGTH_SHORT).show()
            return
        }
        salvarConfiguracoes()
        binding.btnSincronizarWebDb.isEnabled = false
        binding.progressSync.visibility = View.VISIBLE
        binding.tvSyncResultado.visibility = View.GONE

        lifecycleScope.launch {
            val sucesso = repository.testarConexaoWebDb()
            binding.btnSincronizarWebDb.isEnabled = true
            binding.progressSync.visibility = View.GONE
            binding.tvSyncResultado.visibility = View.VISIBLE
            if (sucesso) {
                binding.tvSyncResultado.text = "✅ Conexão com o banco web estabelecida com sucesso!"
                binding.tvSyncResultado.setTextColor(android.graphics.Color.parseColor("#1B5E20"))
            } else {
                binding.tvSyncResultado.text = "❌ Falha na conexão. Verifique a URL e as configurações."
                binding.tvSyncResultado.setTextColor(android.graphics.Color.parseColor("#B71C1C"))
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
