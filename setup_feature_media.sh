#!/usr/bin/env bash
# =============================================================================
# setup_feature_media.sh
# Crea el módulo :feature:feature-media en DownloadManagerApp
# Ejecutar desde: ~/DownloadManagerApp/
# =============================================================================
set -e

MODULE_DIR="feature/feature-media"
SRC="$MODULE_DIR/src/main/kotlin/com/bmo/downloadmanager/feature/media"

echo ">>> Creando estructura de directorios..."
mkdir -p "$SRC"

# =============================================================================
# 1. build.gradle.kts
# =============================================================================
cat > "$MODULE_DIR/build.gradle.kts" << 'EOF'
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.bmo.downloadmanager.feature.media"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(project(":core:core-ui"))
    implementation(project(":domain"))

    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    debugImplementation(libs.compose.ui.tooling)
}
EOF
echo "    [OK] build.gradle.kts"

# =============================================================================
# 2. AndroidManifest.xml
# =============================================================================
mkdir -p "$MODULE_DIR/src/main"
cat > "$MODULE_DIR/src/main/AndroidManifest.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest />
EOF
echo "    [OK] AndroidManifest.xml"

# =============================================================================
# 3. MediaModule.kt
# =============================================================================
cat > "$SRC/MediaModule.kt" << 'EOF'
package com.bmo.downloadmanager.feature.media

import com.bmo.downloadmanager.domain.repository.DownloadRepository
import com.bmo.downloadmanager.domain.usecase.download.GetDownloadByIdUseCase
import com.bmo.downloadmanager.domain.usecase.download.GetDownloadsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    @Provides
    @Singleton
    fun provideGetDownloadsUseCase(repo: DownloadRepository): GetDownloadsUseCase =
        GetDownloadsUseCase(repo)

    @Provides
    @Singleton
    fun provideGetDownloadByIdUseCase(repo: DownloadRepository): GetDownloadByIdUseCase =
        GetDownloadByIdUseCase(repo)
}
EOF
echo "    [OK] MediaModule.kt"

# =============================================================================
# 4. MediaViewModel.kt
# =============================================================================
cat > "$SRC/MediaViewModel.kt" << 'EOF'
package com.bmo.downloadmanager.feature.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.enums.DownloadStatus
import com.bmo.downloadmanager.domain.model.Download
import com.bmo.downloadmanager.domain.model.DownloadSegment
import com.bmo.downloadmanager.domain.usecase.download.GetDownloadByIdUseCase
import com.bmo.downloadmanager.domain.usecase.download.GetDownloadsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

// ---------------------------------------------------------------------------
// UI States
// ---------------------------------------------------------------------------

sealed interface MediaLibraryUiState {
    data object Loading : MediaLibraryUiState
    data class Success(val files: List<Download>) : MediaLibraryUiState
    data class Error(val message: String) : MediaLibraryUiState
}

sealed interface ActiveDownloadsUiState {
    data object Loading : ActiveDownloadsUiState
    data class Success(val downloads: List<Download>) : ActiveDownloadsUiState
    data class Error(val message: String) : ActiveDownloadsUiState
}

sealed interface SegmentsUiState {
    data object Idle : SegmentsUiState
    data object Loading : SegmentsUiState
    data class Success(val segments: List<DownloadSegment>) : SegmentsUiState
    data class Error(val message: String) : SegmentsUiState
}

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

@HiltViewModel
class MediaViewModel @Inject constructor(
    private val getDownloadsUseCase: GetDownloadsUseCase,
    private val getDownloadByIdUseCase: GetDownloadByIdUseCase,
) : ViewModel() {

    // ID del download cuyo detalle de segmentos se está mostrando (null = ninguno)
    private val _selectedDownloadId = MutableStateFlow<Long?>(null)

    // Todos los downloads completados → tab "Biblioteca"
    val libraryUiState: StateFlow<MediaLibraryUiState> = getDownloadsUseCase()
        .map<AppResult<List<Download>>, MediaLibraryUiState> { result ->
            when (result) {
                is AppResult.Loading -> MediaLibraryUiState.Loading
                is AppResult.Error   -> MediaLibraryUiState.Error(result.error.toString())
                is AppResult.Success -> MediaLibraryUiState.Success(
                    result.data.filter { it.status == DownloadStatus.COMPLETED }
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MediaLibraryUiState.Loading,
        )

    // Downloads activos (DOWNLOADING / PAUSED / QUEUED) → tab "En progreso"
    val activeDownloadsUiState: StateFlow<ActiveDownloadsUiState> = getDownloadsUseCase()
        .map<AppResult<List<Download>>, ActiveDownloadsUiState> { result ->
            when (result) {
                is AppResult.Loading -> ActiveDownloadsUiState.Loading
                is AppResult.Error   -> ActiveDownloadsUiState.Error(result.error.toString())
                is AppResult.Success -> ActiveDownloadsUiState.Success(
                    result.data.filter { it.status in activeStatuses }
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ActiveDownloadsUiState.Loading,
        )

    // Segmentos del download seleccionado
    @OptIn(ExperimentalCoroutinesApi::class)
    val segmentsUiState: StateFlow<SegmentsUiState> = _selectedDownloadId
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(SegmentsUiState.Idle)
            } else {
                // Necesitamos el repositorio vía use case; aquí obtenemos segmentos
                // a través del repositorio expuesto desde GetDownloadByIdUseCase no lo da,
                // así que usamos el repositorio directamente inyectado desde el módulo.
                // Por ahora retornamos Idle; se conectará cuando el repo esté disponible.
                flowOf(SegmentsUiState.Idle)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SegmentsUiState.Idle,
        )

    fun selectDownload(id: Long?) {
        _selectedDownloadId.update { id }
    }

    companion object {
        private val activeStatuses = setOf(
            DownloadStatus.DOWNLOADING,
            DownloadStatus.PAUSED,
            DownloadStatus.QUEUED,
        )
    }
}
EOF
echo "    [OK] MediaViewModel.kt"

# =============================================================================
# 5. MediaScreen.kt
# =============================================================================
cat > "$SRC/MediaScreen.kt" << 'EOF'
package com.bmo.downloadmanager.feature.media

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bmo.downloadmanager.domain.enums.DownloadStatus
import com.bmo.downloadmanager.domain.model.Download

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaScreen(
    viewModel: MediaViewModel = hiltViewModel(),
) {
    val libraryState    by viewModel.libraryUiState.collectAsStateWithLifecycle()
    val activeState     by viewModel.activeDownloadsUiState.collectAsStateWithLifecycle()
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val tabs = listOf("Biblioteca", "En progreso")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Media") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick  = { selectedTabIndex = index },
                        text     = { Text(title) },
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> LibraryTab(state = libraryState)
                1 -> ActiveTab(state = activeState)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Tab: Biblioteca (archivos COMPLETED)
// ---------------------------------------------------------------------------

@Composable
private fun LibraryTab(state: MediaLibraryUiState) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        when (state) {
            is MediaLibraryUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is MediaLibraryUiState.Error -> {
                Text(
                    text = "Error: ${state.message}",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            is MediaLibraryUiState.Success -> {
                if (state.files.isEmpty()) {
                    Text(
                        text = "No hay archivos completados",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(items = state.files, key = { it.id }) { download ->
                            MediaFileItem(
                                download = download,
                                onClick  = {
                                    val uri    = Uri.parse(download.destinationUri)
                                    val mime   = download.mimeType ?: "*/*"
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, mime)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    runCatching {
                                        context.startActivity(intent)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaFileItem(
    download: Download,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = download.fileName,
                    style    = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text  = download.mimeType ?: "archivo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text  = formatBytes(download.totalBytes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Tab: En progreso (DOWNLOADING / PAUSED / QUEUED)
// ---------------------------------------------------------------------------

@Composable
private fun ActiveTab(state: ActiveDownloadsUiState) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (state) {
            is ActiveDownloadsUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is ActiveDownloadsUiState.Error -> {
                Text(
                    text = "Error: ${state.message}",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            is ActiveDownloadsUiState.Success -> {
                if (state.downloads.isEmpty()) {
                    Text(
                        text = "No hay descargas activas",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(items = state.downloads, key = { it.id }) { download ->
                            ActiveDownloadItem(download = download)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveDownloadItem(download: Download) {
    val progress = if (download.totalBytes > 0L) {
        (download.downloadedBytes.toFloat() / download.totalBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text     = download.fileName,
                style    = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatusLabel(status = download.status)
                Text(
                    text  = "${(progress * 100).toInt()}% · ${formatBytes(download.downloadedBytes)} / ${formatBytes(download.totalBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )

            if (download.totalSegments > 1) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text  = "${download.totalSegments} segmentos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatusLabel(status: DownloadStatus) {
    val (label, color) = when (status) {
        DownloadStatus.PENDING     -> "Pendiente"   to MaterialTheme.colorScheme.outline
        DownloadStatus.QUEUED      -> "En cola"     to MaterialTheme.colorScheme.secondary
        DownloadStatus.DOWNLOADING -> "Descargando" to MaterialTheme.colorScheme.primary
        DownloadStatus.PAUSED      -> "Pausado"     to MaterialTheme.colorScheme.tertiary
        DownloadStatus.COMPLETED   -> "Completado"  to MaterialTheme.colorScheme.primary
        DownloadStatus.FAILED      -> "Error"       to MaterialTheme.colorScheme.error
        DownloadStatus.CANCELLED   -> "Cancelado"   to MaterialTheme.colorScheme.outline
    }
    SuggestionChip(
        onClick = {},
        label   = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors  = SuggestionChipDefaults.suggestionChipColors(
            containerColor = color.copy(alpha = 0.12f),
            labelColor     = color,
        ),
    )
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun formatBytes(bytes: Long): String = when {
    bytes < 0L          -> "?"
    bytes < 1_024L      -> "$bytes B"
    bytes < 1_048_576L  -> "${"%.1f".format(bytes / 1_024f)} KB"
    bytes < 1_073_741_824L -> "${"%.1f".format(bytes / 1_048_576f)} MB"
    else                -> "${"%.2f".format(bytes / 1_073_741_824f)} GB"
}
EOF
echo "    [OK] MediaScreen.kt"

# =============================================================================
# 6. Registrar en settings.gradle.kts
# =============================================================================
echo ""
echo ">>> Verificando registro en settings.gradle.kts..."
if grep -q "feature-media" settings.gradle.kts; then
    echo "    [SKIP] feature-media ya estaba en settings.gradle.kts"
else
    # Insertar justo después de feature-downloads
    python3 - << 'PYEOF'
path = "settings.gradle.kts"
with open(path, "r") as f:
    content = f.read()

old = 'include(":feature:feature-downloads")'
new = 'include(":feature:feature-downloads")\ninclude(":feature:feature-media")'

if old in content:
    content = content.replace(old, new, 1)
    with open(path, "w") as f:
        f.write(content)
    print("    [OK] feature-media añadido a settings.gradle.kts")
else:
    print("    [WARN] No se encontró ':feature:feature-downloads' — añade manualmente:")
    print('           include(":feature:feature-media")')
PYEOF
fi

# =============================================================================
# 7. Habilitar en app/build.gradle.kts
# =============================================================================
echo ""
echo ">>> Habilitando dependencia en app/build.gradle.kts..."
python3 - << 'PYEOF'
path = "app/build.gradle.kts"
with open(path, "r") as f:
    content = f.read()

old = '    // implementation(project(":feature:feature-media"))  // TODO: módulo pendiente'
new = '    implementation(project(":feature:feature-media"))'

if new in content:
    print("    [SKIP] ya estaba habilitado")
elif old in content:
    content = content.replace(old, new, 1)
    with open(path, "w") as f:
        f.write(content)
    print("    [OK] feature-media habilitado en app/build.gradle.kts")
else:
    print("    [WARN] Línea comentada no encontrada — añade manualmente en app/build.gradle.kts:")
    print('           implementation(project(":feature:feature-media"))')
PYEOF

# =============================================================================
# 8. Compilar
# =============================================================================
echo ""
echo ">>> Compilando :feature:feature-media ..."
./gradlew --no-daemon :feature:feature-media:compileDebugKotlin 2>&1 | tail -25

echo ""
echo "========================================"
echo " feature-media listo."
echo "========================================"
