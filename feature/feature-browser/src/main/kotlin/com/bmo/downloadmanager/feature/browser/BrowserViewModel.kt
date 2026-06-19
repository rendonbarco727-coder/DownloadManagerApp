package com.bmo.downloadmanager.feature.browser

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.core.downloader.manager.DownloadManager
import com.bmo.downloadmanager.domain.model.BrowserHistory
import com.bmo.downloadmanager.domain.model.BrowserTab
import com.bmo.downloadmanager.domain.model.Download
import com.bmo.downloadmanager.domain.usecase.browser.InsertBrowserHistoryUseCase
import com.bmo.downloadmanager.domain.usecase.tab.DeleteBrowserTabUseCase
import com.bmo.downloadmanager.domain.usecase.tab.GetBrowserTabsUseCase
import com.bmo.downloadmanager.domain.usecase.tab.InsertBrowserTabUseCase
import com.bmo.downloadmanager.domain.usecase.tab.UpdateBrowserTabUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class BrowserUiState(
    val tabs: List<BrowserTab> = emptyList(),
    val activeTab: BrowserTab? = null,
    val urlBarText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val downloadStartedMessage: String? = null,
    val detectedMediaUrl: Pair<String, String?>? = null, // url, referer
)

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val insertBrowserHistoryUseCase: InsertBrowserHistoryUseCase,
    private val getTabsUseCase: GetBrowserTabsUseCase,
    private val insertTabUseCase: InsertBrowserTabUseCase,
    private val updateTabUseCase: UpdateBrowserTabUseCase,
    private val deleteTabUseCase: DeleteBrowserTabUseCase,
    private val downloadManager: DownloadManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    init {
        getTabsUseCase()
            .onEach { result ->
                when (result) {
                    is AppResult.Success -> {
                        val tabs = result.data
                        val active = tabs.firstOrNull { it.isActive } ?: tabs.firstOrNull()
                        _uiState.update {
                            it.copy(
                                tabs = tabs,
                                activeTab = active,
                                urlBarText = active?.currentUrl ?: "",
                                error = null,
                            )
                        }
                    }
                    is AppResult.Error -> _uiState.update {
                        it.copy(error = result.error.toString())
                    }
                    is AppResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onUrlBarTextChange(text: String) {
        _uiState.update { it.copy(urlBarText = text) }
    }

    fun navigateTo(url: String) {
        val finalUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else if (url.contains(".") && !url.contains(" ")) {
            "https://$url"
        } else {
            "https://www.google.com/search?q=${url.trim().replace(" ", "+")}"
        }

        val active = _uiState.value.activeTab
        if (active != null) {
            viewModelScope.launch {
                updateTabUseCase(active.copy(currentUrl = finalUrl, lastAccessedAt = System.currentTimeMillis()))
            }
        } else {
            viewModelScope.launch {
                insertTabUseCase(
                    BrowserTab(
                        currentUrl = finalUrl,
                        tabOrder = 0,
                        isActive = true,
                    )
                )
            }
        }
        _uiState.update { it.copy(urlBarText = finalUrl) }
    }

    fun newTab() {
        viewModelScope.launch {
            val order = _uiState.value.tabs.size
            insertTabUseCase(
                BrowserTab(
                    currentUrl = "https://www.google.com",
                    tabOrder = order,
                    isActive = true,
                )
            )
        }
    }

    fun closeTab(id: Long) {
        viewModelScope.launch {
            deleteTabUseCase(id)
        }
    }

    fun onPageLoadFinished(url: String, title: String?) {
        val active = _uiState.value.activeTab ?: return
        viewModelScope.launch {
            updateTabUseCase(
                active.copy(
                    currentUrl = url,
                    title = title,
                    lastAccessedAt = System.currentTimeMillis(),
                )
            )
            insertBrowserHistoryUseCase(
                BrowserHistory(
                    url = url,
                    title = title,
                    lastVisitedAt = System.currentTimeMillis(),
                )
            )
        }
        _uiState.update { it.copy(urlBarText = url, isLoading = false) }
    }

    fun onPageStarted() {
        _uiState.update { it.copy(isLoading = true) }
    }

    /**
     * Encolar una descarga detectada por WebView.setDownloadListener.
     *
     * pageUrl es webView.url en el momento de la descarga (la página que
     * disparó la descarga, no la URL del archivo) y se usa como Referer:
     * varios CDNs (mismo patrón ya visto con vimeos en otros proyectos)
     * exigen ese header o devuelven 403/CORS sin él.
     *
     * contentLength llega del propio callback de WebView; si el servidor lo
     * informó ahí, se usa directo como totalBytes para que el motor de
     * descarga no tenga que repetir un HEAD/Range-probe que ya es
     * redundante.
     */
    fun enqueueDownload(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long,
        pageUrl: String?,
    ) {
        val fileName = extractFileName(url, contentDisposition)
        val destinationDir = downloadsDirectory()
        val destinationUri = File(destinationDir, fileName).absolutePath

        val download = Download(
            sourceUrl = url,
            fileName = fileName,
            destinationUri = destinationUri,
            mimeType = mimeType,
            totalBytes = if (contentLength > 0) contentLength else -1L,
            refererHeader = pageUrl,
            userAgentHeader = userAgent,
        )

        viewModelScope.launch {
            downloadManager.enqueue(download)
            _uiState.update { it.copy(downloadStartedMessage = "Descarga iniciada: $fileName") }
        }
    }

    fun onMediaUrlDetected(url: String, referer: String?) {
        _uiState.update { it.copy(detectedMediaUrl = Pair(url, referer)) }
    }

    fun enqueueMediaDownload(url: String, referer: String?) {
        val fileName = extractFileName(url, null)
        val destinationDir = downloadsDirectory()
        val destinationUri = File(destinationDir, fileName).absolutePath
        val download = Download(
            sourceUrl = url,
            fileName = fileName,
            destinationUri = destinationUri,
            mimeType = null,
            totalBytes = -1L,
            refererHeader = referer,
        )
        viewModelScope.launch {
            downloadManager.enqueue(download)
            _uiState.update {
                it.copy(
                    detectedMediaUrl = null,
                    downloadStartedMessage = "Descarga iniciada: $fileName",
                )
            }
        }
    }

    fun dismissMediaDialog() {
        _uiState.update { it.copy(detectedMediaUrl = null) }
    }

    fun consumeDownloadStartedMessage() {
        _uiState.update { it.copy(downloadStartedMessage = null) }
    }

    private fun downloadsDirectory(): File {
        // getExternalFilesDir no está disponible aquí (ViewModel no tiene
        // Context); se resuelve un path estable propio de la app en lugar
        // de depender de Environment.getExternalStoragePublicDirectory,
        // que requiere permisos adicionales en versiones viejas de Android.
        // Este path coincide con el que usa DownloadForegroundService para
        // temporales (mismo árbol de archivos de la app, subcarpeta
        // separada para destinos finales).
        return File(Environment.getExternalStorageDirectory(), "Android/data/com.bmo.downloadmanager/files/Download")
            .apply { mkdirs() }
    }

    /**
     * Extrae el nombre de archivo de Content-Disposition (formato típico
     * `attachment; filename="archivo.zip"`) con fallback al último segmento
     * de la URL, y un fallback final genérico si ninguno de los dos
     * resultó útil (URLs sin extensión ni nombre claro).
     */
    private fun extractFileName(url: String, contentDisposition: String?): String {
        val fromHeader = contentDisposition
            ?.substringAfter("filename=", "")
            ?.trim('"', ' ')
            ?.takeIf { it.isNotBlank() }

        if (fromHeader != null) return fromHeader

        val fromUrl = url.substringAfterLast('/').substringBefore('?').takeIf { it.isNotBlank() }
        return fromUrl ?: "descarga_${System.currentTimeMillis()}"
    }
}
