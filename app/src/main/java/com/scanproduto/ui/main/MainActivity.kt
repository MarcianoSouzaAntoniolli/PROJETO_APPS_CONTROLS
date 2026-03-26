package com.scanproduto.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.scanproduto.R
import com.scanproduto.databinding.ActivityMainBinding
import com.scanproduto.model.Produto
import com.scanproduto.model.Resource
import com.scanproduto.ui.settings.SettingsActivity
import com.scanproduto.ui.product.ImportActivity
import com.scanproduto.utils.FeedbackUtils
import com.scanproduto.utils.FormatUtils
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Tela principal do aplicativo.
 * Contém o scanner de câmera e a exibição dos dados do produto.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private var cameraExecutor: ExecutorService? = null
    private var cameraProvider: ProcessCameraProvider? = null

    // Launcher para permissão de câmera
    private val permissaoCameraLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedida ->
        if (concedida) {
            iniciarCamera()
        } else {
            mostrarErroPermissao()
        }
    }

    // Launcher para seleção de arquivo TXT
    private val selecionarArquivoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importarDoTxt(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        configurarObservers()
        configurarListeners()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    /**
     * Configura os observers do ViewModel (padrão MVVM).
     */
    private fun configurarObservers() {
        // Observa estado do produto
        viewModel.produtoState.observe(this) { state ->
            when (state) {
                is Resource.Loading -> mostrarLoading()
                is Resource.Success -> mostrarProduto(state.data)
                is Resource.Error -> mostrarErro(state.message)
                is Resource.Empty -> mostrarEstadoVazio()
            }
        }

        // Observa estado do scanner
        viewModel.scannerAtivo.observe(this) { ativo ->
            if (ativo) {
                verificarPermissaoCamera()
                binding.btnScanner.text = getString(R.string.parar_scanner)
                binding.btnScanner.setIconResource(R.drawable.ic_stop)
                binding.previewView.visibility = View.VISIBLE
            } else {
                pararCamera()
                binding.btnScanner.text = getString(R.string.iniciar_scanner)
                binding.btnScanner.setIconResource(R.drawable.ic_camera)
                binding.previewView.visibility = View.GONE
            }
        }

        // Observa contagem do cache
        viewModel.totalCache.observe(this) { total ->
            binding.tvCacheInfo.text = getString(R.string.cache_info, total)
        }
    }

    /**
     * Configura os listeners de clique dos botões.
     */
    private fun configurarListeners() {
        // Botão scanner (câmera)
        binding.btnScanner.setOnClickListener {
            viewModel.toggleScanner()
        }

        // Botão buscar por código digitado
        binding.btnBuscar.setOnClickListener {
            val codigo = binding.etCodigo.text.toString().trim()
            if (codigo.isNotBlank()) {
                viewModel.buscarProduto(codigo)
                binding.etCodigo.clearFocus()
            }
        }

        // Enter no campo de texto
        binding.etCodigo.setOnEditorActionListener { _, _, _ ->
            binding.btnBuscar.performClick()
            true
        }

        // Botão limpar
        binding.btnLimpar.setOnClickListener {
            viewModel.limparEstado()
            binding.etCodigo.setText("")
        }
    }

    /**
     * Verifica se a permissão de câmera foi concedida e inicia a câmera.
     */
    private fun verificarPermissaoCamera() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                iniciarCamera()
            }
            else -> {
                permissaoCameraLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    /**
     * Inicializa o CameraX com ML Kit para leitura de código de barras.
     */
    private fun iniciarCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            // Preview da câmera
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            // Análise de imagem para leitura de barcode
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor!!, { imageProxy ->
                        processarImagem(imageProxy)
                    })
                }

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                mostrarErro("Erro ao iniciar câmera: ${e.message}")
            }

        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * Processa o frame da câmera com ML Kit Barcode Scanner.
     * Detecta EAN-13 e EAN-8.
     */
    @androidx.camera.core.ExperimentalGetImage
    private fun processarImagem(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        val scanner = BarcodeScanning.getClient()

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    // Filtra EAN-13, EAN-8, Code 128, Code 39 e Code 93
                    if (barcode.format == Barcode.FORMAT_EAN_13 ||
                        barcode.format == Barcode.FORMAT_EAN_8 ||
                        barcode.format == Barcode.FORMAT_CODE_128 ||
                        barcode.format == Barcode.FORMAT_CODE_39 ||
                        barcode.format == Barcode.FORMAT_CODE_93
                    ) {
                        val ean = barcode.rawValue ?: continue

                        // Feedback de leitura bem-sucedida
                        FeedbackUtils.vibrar(this)

                        // Notifica o ViewModel
                        runOnUiThread {
                            binding.etCodigo.setText("")
                            viewModel.onCodigoLido(ean)
                        }
                        break
                    }
                }
            }
            .addOnFailureListener { /* Ignora falhas de frame */ }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    /**
     * Para a câmera e libera recursos.
     */
    private fun pararCamera() {
        cameraProvider?.unbindAll()
    }

    // ===================== UI States =====================

    private fun mostrarLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.cardProduto.visibility = View.GONE
        binding.tvErro.visibility = View.GONE
        binding.tvEstadoVazio.visibility = View.GONE
    }

    private fun mostrarProduto(produto: Produto) {
        binding.progressBar.visibility = View.GONE
        binding.tvErro.visibility = View.GONE
        binding.tvEstadoVazio.visibility = View.GONE
        binding.cardProduto.visibility = View.VISIBLE
        binding.etCodigo.setText("")

        binding.tvDescricao.text = produto.descricao
        binding.tvPreco.text = FormatUtils.formatarPreco(produto.preco)
        binding.tvEstoque.text = getString(R.string.estoque_label, produto.estoque)
        binding.tvCodigo.text = getString(R.string.codigo_label, produto.codigo)
        binding.tvEan.text = getString(R.string.ean_label, produto.ean)

        // Destaca estoque baixo (< 10 unidades)
        val corEstoque = if (produto.estoque < 10) {
            ContextCompat.getColor(this, R.color.estoque_baixo)
        } else {
            ContextCompat.getColor(this, R.color.estoque_normal)
        }
        binding.tvEstoque.setTextColor(corEstoque)
    }

    private fun mostrarErro(mensagem: String) {
        binding.progressBar.visibility = View.GONE
        binding.cardProduto.visibility = View.GONE
        binding.tvEstadoVazio.visibility = View.GONE
        binding.tvErro.visibility = View.VISIBLE
        binding.tvErroTexto.text = mensagem
        binding.etCodigo.setText("")

        FeedbackUtils.vibrarErro(this)
    }

    private fun mostrarEstadoVazio() {
        binding.progressBar.visibility = View.GONE
        binding.cardProduto.visibility = View.GONE
        binding.tvErro.visibility = View.GONE
        binding.tvEstadoVazio.visibility = View.VISIBLE
    }

    private fun mostrarErroPermissao() {
        mostrarErro("Permissão de câmera negada. Use o campo de texto para digitar o código.")
        viewModel.pararScanner()
    }

    // ===================== Menu =====================

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                mostrarDialogSenha()
                true
            }
            R.id.action_import -> {
                startActivity(Intent(this, ImportActivity::class.java))
                true
            }
            R.id.action_limpar_cache -> {
                viewModel.limparCache()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun mostrarDialogSenha() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Senha"
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Configurações")
            .setMessage("Digite a senha para acessar as configurações:")
            .setView(input)
            .setPositiveButton("Entrar") { _, _ ->
                if (input.text.toString() == "Control@0181") {
                    startActivity(Intent(this, SettingsActivity::class.java))
                } else {
                    Toast.makeText(this, "Senha incorreta", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor?.shutdown()
    }
}
