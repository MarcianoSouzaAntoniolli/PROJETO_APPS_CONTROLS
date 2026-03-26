package com.scanproduto.ui.main

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.scanproduto.data.db.AppDatabase
import com.scanproduto.data.local.PreferencesManager
import com.scanproduto.data.local.TxtLocalDataSource
import com.scanproduto.data.repository.ProdutoRepository
import com.scanproduto.model.Produto
import com.scanproduto.model.Resource
import kotlinx.coroutines.launch

/**
 * ViewModel principal do app.
 * Gerencia o estado da tela principal e a lógica de negócio.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Repositório injetado manualmente (sem DI framework para simplicidade)
    private val repository: ProdutoRepository by lazy {
        val db = AppDatabase.getInstance(application)
        val prefs = PreferencesManager(application)
        val txt = TxtLocalDataSource(application)
        ProdutoRepository(db, prefs, txt)
    }

    // Estado do produto consultado
    private val _produtoState = MutableLiveData<Resource<Produto>>(Resource.Empty)
    val produtoState: LiveData<Resource<Produto>> = _produtoState

    // Último EAN lido (para evitar leituras duplicadas consecutivas)
    private var ultimoEanLido: String = ""
    private var ultimaLeituraTimestamp: Long = 0L
    private val DEBOUNCE_MS = 1500L // Evita leitura dupla em 1.5 segundos

    // Controle do scanner
    private val _scannerAtivo = MutableLiveData<Boolean>(false)
    val scannerAtivo: LiveData<Boolean> = _scannerAtivo

    // Contagem de produtos em cache
    private val _totalCache = MutableLiveData<Int>(0)
    val totalCache: LiveData<Int> = _totalCache

    init {
        atualizarContadorCache()
    }

    /**
     * Chamado quando o scanner lê um código de barras.
     * Implementa debounce para evitar leituras duplicadas.
     */
    fun onCodigoLido(ean: String) {
        val agora = System.currentTimeMillis()

        // Ignora se for o mesmo EAN lido recentemente (debounce)
        if (ean == ultimoEanLido && agora - ultimaLeituraTimestamp < DEBOUNCE_MS) {
            return
        }

        ultimoEanLido = ean
        ultimaLeituraTimestamp = agora

        buscarProduto(ean)
    }

    /**
     * Busca um produto pelo EAN no repositório.
     * Atualiza o LiveData com o estado do resultado.
     */
    fun buscarProduto(ean: String) {
        if (ean.isBlank()) {
            _produtoState.value = Resource.Error("Digite ou escaneie um código de barras")
            return
        }

        viewModelScope.launch {
            // Mostra loading
            _produtoState.value = Resource.Loading

            // Consulta o repositório (API / Postgres / TXT / Cache)
            val resultado = repository.buscarProdutoPorEan(ean.trim())
            _produtoState.value = resultado
        }
    }

    /**
     * Liga/desliga o scanner de câmera.
     */
    fun toggleScanner() {
        _scannerAtivo.value = !(_scannerAtivo.value ?: false)
    }

    /**
     * Para o scanner.
     */
    fun pararScanner() {
        _scannerAtivo.value = false
    }

    /**
     * Inicia o scanner.
     */
    fun iniciarScanner() {
        _scannerAtivo.value = true
    }

    /**
     * Limpa o estado atual (volta para tela inicial).
     */
    fun limparEstado() {
        _produtoState.value = Resource.Empty
        ultimoEanLido = ""
    }

    /**
     * Importa produtos de um arquivo TXT.
     */
    fun importarDoTxt(uri: Uri) {
        viewModelScope.launch {
            _produtoState.value = Resource.Loading

            val resultado = repository.importarDoTxt(uri)

            when (resultado) {
                is Resource.Success -> {
                    atualizarContadorCache()
                    _produtoState.value = Resource.Error(
                        "✅ ${resultado.data} produtos importados com sucesso!"
                    )
                }
                is Resource.Error -> {
                    _produtoState.value = resultado
                }
                else -> {}
            }
        }
    }

    /**
     * Atualiza o contador de produtos no cache.
     */
    private fun atualizarContadorCache() {
        viewModelScope.launch {
            _totalCache.value = repository.contarProdutosEmCache()
        }
    }

    /**
     * Define a URI do arquivo TXT para consultas futuras (sem importar para cache).
     */
    fun definirArquivoTxt(uri: Uri) {
        repository.arquivoTxtUri = uri
    }

    /**
     * Limpa o cache local de produtos.
     */
    fun limparCache() {
        viewModelScope.launch {
            repository.limparCache()
            atualizarContadorCache()
        }
    }
}
