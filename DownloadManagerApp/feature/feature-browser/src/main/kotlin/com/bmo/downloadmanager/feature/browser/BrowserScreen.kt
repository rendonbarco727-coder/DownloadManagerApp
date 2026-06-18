package com.bmo.downloadmanager.feature.browser

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
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

    Column(modifier = Modifier.fillMaxSize()) {
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
                }
                webChromeClient = WebChromeClient()
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
