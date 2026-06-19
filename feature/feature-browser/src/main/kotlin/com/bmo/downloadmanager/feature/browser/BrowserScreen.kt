package com.bmo.downloadmanager.feature.browser

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.downloadStartedMessage) {
        val message = uiState.downloadStartedMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeDownloadStartedMessage()
        }
    }

    val detectedMedia = uiState.detectedMediaUrl
    if (detectedMedia != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissMediaDialog,
            title = { Text("URL de video detectada") },
            text = {
                Text(
                    "Se detectó una URL de media. ¿Deseas descargarla?\n\n" +
                        detectedMedia.first.take(80) +
                        (if (detectedMedia.first.length > 80) "…" else "")
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.enqueueMediaDownload(detectedMedia.first, detectedMedia.second)
                }) { Text("⬇ Descargar") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissMediaDialog) { Text("Ignorar") }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding),
        ) {
            // Barra superior
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    OutlinedTextField(
                        value = uiState.urlBarText,
                        onValueChange = viewModel::onUrlBarTextChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("Buscar o escribir URL") },
                        shape = RoundedCornerShape(24.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = { viewModel.navigateTo(uiState.urlBarText) }
                        ),
                        textStyle = MaterialTheme.typography.bodySmall,
                    )
                    TextButton(onClick = viewModel::newTab) {
                        Text("+", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            // Indicador de carga
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Tabs row
            if (uiState.tabs.size > 1) {
                TabsRow(
                    tabs = uiState.tabs,
                    activeTab = uiState.activeTab,
                    onClose = viewModel::closeTab,
                )
            }

            // WebView
            val currentUrl = uiState.activeTab?.currentUrl
            if (currentUrl != null) {
                BrowserWebView(
                    url = currentUrl,
                    modifier = Modifier.weight(1f),
                    onPageStarted = viewModel::onPageStarted,
                    onPageFinished = viewModel::onPageLoadFinished,
                    onDownloadRequested = { downloadUrl, userAgent, contentDisposition, mimeType, contentLength, pageUrl ->
                        viewModel.enqueueDownload(
                            url = downloadUrl,
                            userAgent = userAgent,
                            contentDisposition = contentDisposition,
                            mimeType = mimeType,
                            contentLength = contentLength,
                            pageUrl = pageUrl,
                        )
                    },
                    onMediaUrlDetected = viewModel::onMediaUrlDetected,
                )
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Escribe una URL o búsqueda arriba",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TabsRow(
    tabs: List<com.bmo.downloadmanager.domain.model.BrowserTab>,
    activeTab: com.bmo.downloadmanager.domain.model.BrowserTab?,
    onClose: (Long) -> Unit,
) {
    ScrollableTabRow(
        selectedTabIndex = tabs.indexOfFirst { it.id == activeTab?.id }.coerceAtLeast(0),
        modifier = Modifier.fillMaxWidth(),
        edgePadding = 0.dp,
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = tab.id == activeTab?.id,
                onClick = {},
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tab.title ?: tab.currentUrl.take(20),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                        )
                        Spacer(Modifier.width(4.dp))
                        TextButton(
                            onClick = { onClose(tab.id) },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(18.dp),
                        ) {
                            Text("×", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun BrowserWebView(
    url: String,
    modifier: Modifier = Modifier,
    onPageStarted: () -> Unit,
    onPageFinished: (String, String?) -> Unit,
    onDownloadRequested: (
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long,
        pageUrl: String?,
    ) -> Unit,
    onMediaUrlDetected: (url: String, referer: String?) -> Unit,
) {
    var webView by remember { mutableStateOf<WebView?>(null) }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                        onPageStarted()
                    }
                    override fun onPageFinished(view: WebView, url: String) {
                        onPageFinished(url, view.title)
                    }
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest,
                    ): WebResourceResponse? {
                        val url = request.url.toString()
                        val acceptHeader = request.requestHeaders["Accept"] ?: ""
                        val isMedia = url.contains(".m3u8") ||
                            url.contains(".mp4") ||
                            url.contains(".mkv") ||
                            url.contains(".avi") ||
                            acceptHeader.contains("video")
                        if (isMedia) {
                            onMediaUrlDetected(url, request.requestHeaders["Referer"])
                        }
                        return null
                    }
                }
                webChromeClient = WebChromeClient()
                // DownloadListener captura cualquier respuesta que el
                // WebView no puede renderizar él mismo (típicamente
                // Content-Disposition: attachment, o un mimeType que no es
                // HTML/imagen/etc). pageUrl se toma de this.url, que en
                // este callback ya apunta a la página que originó la
                // descarga -- se usa como Referer en DownloadManager.
                setDownloadListener { downloadUrl, userAgent, contentDisposition, mimeType, contentLength ->
                    onDownloadRequested(
                        downloadUrl,
                        userAgent,
                        contentDisposition,
                        mimeType,
                        contentLength,
                        this.url,
                    )
                }
                loadUrl(url)
                webView = this
            }
        },
        update = { view ->
            if (view.url != url) view.loadUrl(url)
        },
        modifier = modifier,
    )

    DisposableEffect(Unit) {
        onDispose { webView?.destroy() }
    }
}
