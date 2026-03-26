package com.scanproduto.ui.product

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.scanproduto.data.db.AppDatabase
import com.scanproduto.data.local.PreferencesManager
import com.scanproduto.data.local.TxtLocalDataSource
import com.scanproduto.data.repository.ProdutoRepository
import com.scanproduto.databinding.ActivityImportBinding
import com.scanproduto.model.Resource
import kotlinx.coroutines.launch

/**
 * Tela de importação de produtos via arquivo TXT.
 * Permite selecionar um arquivo .txt e importar os produtos para o cache local.
 */
class ImportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImportBinding
    private lateinit var repository: ProdutoRepository
    private var uriSelecionada: Uri? = null

    // Launcher para seleção de arquivo TXT
    private val selecionarArquivoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            uriSelecionada = it
            binding.tvArquivoSelecionado.text = obterNomeArquivo(it)
            binding.cardArquivo.visibility = View.VISIBLE
            binding.btnImportar.isEnabled = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Importar Produtos"

        repository = ProdutoRepository(
            AppDatabase.getInstance(this),
            PreferencesManager(this),
            TxtLocalDataSource(this)
        )

        configurarListeners()
    }

    private fun configurarListeners() {
        // Selecionar arquivo
        binding.btnSelecionarArquivo.setOnClickListener {
            selecionarArquivoLauncher.launch("text/plain")
        }

        // Importar para cache
        binding.btnImportar.setOnClickListener {
            uriSelecionada?.let { uri -> importarArquivo(uri) }
        }
    }

    /**
     * Importa os produtos do arquivo TXT para o cache local (Room).
     */
    private fun importarArquivo(uri: Uri) {
        binding.progressImport.visibility = View.VISIBLE
        binding.btnImportar.isEnabled = false
        binding.cardResultado.visibility = View.GONE

        lifecycleScope.launch {
            val resultado = repository.importarDoTxt(uri)

            binding.progressImport.visibility = View.GONE
            binding.btnImportar.isEnabled = true

            when (resultado) {
                is Resource.Success -> {
                    binding.tvResultado.text =
                        "✅ ${resultado.data} produto(s) importado(s) com sucesso!"
                    binding.tvResultado.setTextColor(android.graphics.Color.parseColor("#1B5E20"))
                    binding.cardResultado.setCardBackgroundColor(android.graphics.Color.parseColor("#E8F5E9"))
                    binding.cardResultado.visibility = View.VISIBLE
                }
                is Resource.Error -> {
                    binding.tvResultado.text = "❌ ${resultado.message}"
                    binding.tvResultado.setTextColor(android.graphics.Color.parseColor("#B71C1C"))
                    binding.cardResultado.setCardBackgroundColor(android.graphics.Color.parseColor("#FFEBEE"))
                    binding.cardResultado.visibility = View.VISIBLE
                }
                else -> {}
            }
        }
    }

    /**
     * Obtém o nome do arquivo a partir da URI.
     */
    private fun obterNomeArquivo(uri: Uri): String {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex =
                    cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            } ?: uri.lastPathSegment ?: "arquivo.txt"
        } catch (e: Exception) {
            uri.lastPathSegment ?: "arquivo.txt"
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
