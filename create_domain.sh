#!/bin/bash
# ============================================================
# Script: create_domain.sh
# Crea el módulo :domain completo en DownloadManagerApp
# ============================================================

BASE=~/DownloadManagerApp
DOMAIN=$BASE/domain/src/main/kotlin/com/bmo/downloadmanager/domain

# ── Crear directorios ────────────────────────────────────────
mkdir -p $DOMAIN/model
mkdir -p $DOMAIN/enums
mkdir -p $DOMAIN/repository
mkdir -p $DOMAIN/usecase/download
mkdir -p $DOMAIN/usecase/browser
mkdir -p $DOMAIN/usecase/tab
mkdir -p $DOMAIN/usecase/settings

echo "✓ Directorios creados"

# ── build.gradle.kts ────────────────────────────────────────
cat > $BASE/domain/build.gradle.kts << 'EOF'
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(libs.kotlinx.coroutines.core)
}
EOF
echo "✓ domain/build.gradle.kts"

# ── ENUMS ────────────────────────────────────────────────────
cat > $DOMAIN/enums/DownloadStatus.kt << 'EOF'
package com.bmo.downloadmanager.domain.enums

enum class DownloadStatus {
    PENDING,
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}
EOF
echo "✓ DownloadStatus.kt"

cat > $DOMAIN/enums/SegmentStatus.kt << 'EOF'
package com.bmo.downloadmanager.domain.enums

enum class SegmentStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED
}
EOF
echo "✓ SegmentStatus.kt"

cat > $DOMAIN/enums/SettingValueType.kt << 'EOF'
package com.bmo.downloadmanager.domain.enums

enum class SettingValueType {
    STRING,
    INT,
    BOOLEAN,
    FLOAT
}
EOF
echo "✓ SettingValueType.kt"

# ── MODELOS ──────────────────────────────────────────────────
cat > $DOMAIN/model/Download.kt << 'EOF'
package com.bmo.downloadmanager.domain.model

import com.bmo.downloadmanager.domain.enums.DownloadStatus

data class Download(
    val id: Long = 0,
    val url: String,
    val fileName: String,
    val fileSize: Long = -1L,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val segmentCount: Int = 1,
    val refererHeader: String? = null,
    val userAgentHeader: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
EOF
echo "✓ Download.kt"

cat > $DOMAIN/model/DownloadSegment.kt << 'EOF'
package com.bmo.downloadmanager.domain.model

import com.bmo.downloadmanager.domain.enums.SegmentStatus

data class DownloadSegment(
    val id: Long = 0,
    val downloadId: Long,
    val segmentIndex: Int,
    val startByte: Long,
    val endByte: Long,
    val downloadedBytes: Long = 0L,
    val status: SegmentStatus = SegmentStatus.PENDING
)
EOF
echo "✓ DownloadSegment.kt"

cat > $DOMAIN/model/BrowserHistory.kt << 'EOF'
package com.bmo.downloadmanager.domain.model

data class BrowserHistory(
    val id: Long = 0,
    val url: String,
    val title: String = "",
    val visitCount: Int = 1,
    val lastVisited: Long = System.currentTimeMillis()
)
EOF
echo "✓ BrowserHistory.kt"

cat > $DOMAIN/model/BrowserTab.kt << 'EOF'
package com.bmo.downloadmanager.domain.model

data class BrowserTab(
    val id: Long = 0,
    val url: String,
    val title: String = "",
    val isActive: Boolean = false,
    val webViewState: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BrowserTab) return false
        return id == other.id &&
            url == other.url &&
            title == other.title &&
            isActive == other.isActive &&
            webViewState.contentEquals(other.webViewState ?: byteArrayOf())
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + isActive.hashCode()
        result = 31 * result + (webViewState?.contentHashCode() ?: 0)
        return result
    }
}
EOF
echo "✓ BrowserTab.kt"

cat > $DOMAIN/model/AppSettings.kt << 'EOF'
package com.bmo.downloadmanager.domain.model

import com.bmo.downloadmanager.domain.enums.SettingValueType

data class AppSettings(
    val id: Long = 0,
    val key: String,
    val value: String,
    val type: SettingValueType = SettingValueType.STRING
)
EOF
echo "✓ AppSettings.kt"

# ── REPOSITORIOS ─────────────────────────────────────────────
cat > $DOMAIN/repository/DownloadRepository.kt << 'EOF'
package com.bmo.downloadmanager.domain.repository

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.model.Download
import com.bmo.downloadmanager.domain.model.DownloadSegment
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun getDownloads(): Flow<AppResult<List<Download>>>
    suspend fun getDownloadById(id: Long): AppResult<Download>
    suspend fun insertDownload(download: Download): AppResult<Long>
    suspend fun updateDownload(download: Download): AppResult<Unit>
    suspend fun deleteDownload(id: Long): AppResult<Unit>
    fun getSegmentsForDownload(downloadId: Long): Flow<AppResult<List<DownloadSegment>>>
    suspend fun insertSegment(segment: DownloadSegment): AppResult<Long>
    suspend fun updateSegment(segment: DownloadSegment): AppResult<Unit>
}
EOF
echo "✓ DownloadRepository.kt"

cat > $DOMAIN/repository/BrowserHistoryRepository.kt << 'EOF'
package com.bmo.downloadmanager.domain.repository

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.model.BrowserHistory
import kotlinx.coroutines.flow.Flow

interface BrowserHistoryRepository {
    fun getHistory(): Flow<AppResult<List<BrowserHistory>>>
    suspend fun insertOrUpdateHistory(entry: BrowserHistory): AppResult<Unit>
    suspend fun deleteHistoryEntry(id: Long): AppResult<Unit>
    suspend fun clearAllHistory(): AppResult<Unit>
    fun searchHistory(query: String): Flow<AppResult<List<BrowserHistory>>>
}
EOF
echo "✓ BrowserHistoryRepository.kt"

cat > $DOMAIN/repository/BrowserTabRepository.kt << 'EOF'
package com.bmo.downloadmanager.domain.repository

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.model.BrowserTab
import kotlinx.coroutines.flow.Flow

interface BrowserTabRepository {
    fun getTabs(): Flow<AppResult<List<BrowserTab>>>
    suspend fun getActiveTab(): AppResult<BrowserTab?>
    suspend fun insertTab(tab: BrowserTab): AppResult<Long>
    suspend fun updateTab(tab: BrowserTab): AppResult<Unit>
    suspend fun deleteTab(id: Long): AppResult<Unit>
    suspend fun setActiveTab(id: Long): AppResult<Unit>
}
EOF
echo "✓ BrowserTabRepository.kt"

cat > $DOMAIN/repository/SettingsRepository.kt << 'EOF'
package com.bmo.downloadmanager.domain.repository

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getAllSettings(): Flow<AppResult<List<AppSettings>>>
    suspend fun getSetting(key: String): AppResult<AppSettings?>
    suspend fun setSetting(setting: AppSettings): AppResult<Unit>
    suspend fun deleteSetting(key: String): AppResult<Unit>
}
EOF
echo "✓ SettingsRepository.kt"

# ── USE CASES: DOWNLOAD ──────────────────────────────────────
cat > $DOMAIN/usecase/download/GetDownloadsUseCase.kt << 'EOF'
package com.bmo.downloadmanager.domain.usecase.download

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.model.Download
import com.bmo.downloadmanager.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDownloadsUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    operator fun invoke(): Flow<AppResult<List<Download>>> = repository.getDownloads()
}
EOF
echo "✓ GetDownloadsUseCase.kt"

cat > $DOMAIN/usecase/download/GetDownloadByIdUseCase.kt << 'EOF'
package com.bmo.downloadmanager.domain.usecase.download

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.model.Download
import com.bmo.downloadmanager.domain.repository.DownloadRepository
import javax.inject.Inject

class GetDownloadByIdUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(id: Long): AppResult<Download> =
        repository.getDownloadById(id)
}
EOF
echo "✓ GetDownloadByIdUseCase.kt"

cat > $DOMAIN/usecase/download/InsertDownloadUseCase.kt << 'EOF'
package com.bmo.downloadmanager.domain.usecase.download

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.model.Download
import com.bmo.downloadmanager.domain.repository.DownloadRepository
import javax.inject.Inject

class InsertDownloadUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(download: Download): AppResult<Long> =
        repository.insertDownload(download)
}
EOF
echo "✓ InsertDownloadUseCase.kt"

cat > $DOMAIN/usecase/download/UpdateDownloadUseCase.kt << 'EOF'
package com.bmo.downloadmanager.domain.usecase.download

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.model.Download
import com.bmo.downloadmanager.domain.repository.DownloadRepository
import javax.inject.Inject

class UpdateDownloadUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(download: Download): AppResult<Unit> =
        repository.updateDownload(download)
}
EOF
echo "✓ UpdateDownloadUseCase.kt"

cat > $DOMAIN/usecase/download/DeleteDownloadUseCase.kt << 'EOF'
package com.bmo.downloadmanager.domain.usecase.download

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.repository.DownloadRepository
import javax.inject.Inject

class DeleteDownloadUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(id: Long): AppResult<Unit> =
        repository.deleteDownload(id)
}
EOF
echo "✓ DeleteDownloadUseCase.kt"

# ── USE CASES: BROWSER ───────────────────────────────────────
cat > $DOMAIN/usecase/browser/GetBrowserHistoryUseCase.kt << 'EOF'
package com.bmo.downloadmanager.domain.usecase.browser

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.model.BrowserHistory
import com.bmo.downloadmanager.domain.repository.BrowserHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBrowserHistoryUseCase @Inject constructor(
    private val repository: BrowserHistoryRepository
) {
    operator fun invoke(): Flow<AppResult<List<BrowserHistory>>> = repository.getHistory()
}
EOF
echo "✓ GetBrowserHistoryUseCase.kt"

cat > $DOMAIN/usecase/browser/InsertBrowserHistoryUseCase.kt << 'EOF'
package com.bmo.downloadmanager.domain.usecase.browser

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.model.BrowserHistory
import com.bmo.downloadmanager.domain.repository.BrowserHistoryRepository
import javax.inject.Inject

class InsertBrowserHistoryUseCase @Inject constructor(
    private val repository: BrowserHistoryRepository
) {
    suspend operator fun invoke(entry: BrowserHistory): AppResult<Unit> =
        repository.insertOrUpdateHistory(entry)
}
EOF
echo "✓ InsertBrowserHistoryUseCase.kt"

# ── USE CASES: TAB ───────────────────────────────────────────
cat > $DOMAIN/usecase/tab/GetBrowserTabsUseCase.kt << 'EOF'
package com.bmo.downloadmanager.domain.usecase.tab

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.model.BrowserTab
import com.bmo.downloadmanager.domain.repository.BrowserTabRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBrowserTabsUseCase @Inject constructor(
    private val repository: BrowserTabRepository
) {
    operator fun invoke(): Flow<AppResult<List<BrowserTab>>> = repository.getTabs()
}
EOF
echo "✓ GetBrowserTabsUseCase.kt"

cat > $DOMAIN/usecase/tab/InsertBrowserTabUseCase.kt << 'EOF'
package com.bmo.downloadmanager.domain.usecase.tab

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.repository.BrowserTabRepository
import com.bmo.downloadmanager.domain.model.BrowserTab
import javax.inject.Inject

class InsertBrowserTabUseCase @Inject constructor(
    private val repository: BrowserTabRepository
) {
    suspend operator fun invoke(tab: BrowserTab): AppResult<Long> =
        repository.insertTab(tab)
}
EOF
echo "✓ InsertBrowserTabUseCase.kt"

cat > $DOMAIN/usecase/tab/UpdateBrowserTabUseCase.kt << 'EOF'
package com.bmo.downloadmanager.domain.usecase.tab

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.model.BrowserTab
import com.bmo.downloadmanager.domain.repository.BrowserTabRepository
import javax.inject.Inject

class UpdateBrowserTabUseCase @Inject constructor(
    private val repository: BrowserTabRepository
) {
    suspend operator fun invoke(tab: BrowserTab): AppResult<Unit> =
        repository.updateTab(tab)
}
EOF
echo "✓ UpdateBrowserTabUseCase.kt"

cat > $DOMAIN/usecase/tab/DeleteBrowserTabUseCase.kt << 'EOF'
package com.bmo.downloadmanager.domain.usecase.tab

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.repository.BrowserTabRepository
import javax.inject.Inject

class DeleteBrowserTabUseCase @Inject constructor(
    private val repository: BrowserTabRepository
) {
    suspend operator fun invoke(id: Long): AppResult<Unit> =
        repository.deleteTab(id)
}
EOF
echo "✓ DeleteBrowserTabUseCase.kt"

# ── USE CASES: SETTINGS ──────────────────────────────────────
cat > $DOMAIN/usecase/settings/GetSettingUseCase.kt << 'EOF'
package com.bmo.downloadmanager.domain.usecase.settings

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.model.AppSettings
import com.bmo.downloadmanager.domain.repository.SettingsRepository
import javax.inject.Inject

class GetSettingUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(key: String): AppResult<AppSettings?> =
        repository.getSetting(key)
}
EOF
echo "✓ GetSettingUseCase.kt"

cat > $DOMAIN/usecase/settings/SetSettingUseCase.kt << 'EOF'
package com.bmo.downloadmanager.domain.usecase.settings

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.domain.model.AppSettings
import com.bmo.downloadmanager.domain.repository.SettingsRepository
import javax.inject.Inject

class SetSettingUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(setting: AppSettings): AppResult<Unit> =
        repository.setSetting(setting)
}
EOF
echo "✓ SetSettingUseCase.kt"

# ── settings.gradle.kts — agregar :domain ───────────────────
if ! grep -q '"domain"' $BASE/settings.gradle.kts; then
    sed -i 's/include(":core:core-ui")/include(":core:core-ui")\ninclude(":domain")/' $BASE/settings.gradle.kts
    echo "✓ :domain agregado a settings.gradle.kts"
else
    echo "⚠ :domain ya estaba en settings.gradle.kts"
fi

# ── app/build.gradle.kts — descomentar :domain ──────────────
sed -i 's|    // implementation(project(":domain"))  // TODO: módulo pendiente|    implementation(project(":domain"))|' $BASE/app/build.gradle.kts
echo "✓ :domain descomentado en app/build.gradle.kts"

# ── Verificar alias kotlin.jvm en libs.versions.toml ────────
echo ""
echo "=== Verificando alias kotlin.jvm en libs.versions.toml ==="
grep -n "kotlin.jvm\|kotlin-jvm\|jvm" $BASE/gradle/libs.versions.toml | head -10

echo ""
echo "=== DONE — ahora ejecuta: ==="
echo "cd ~/DownloadManagerApp && ./gradlew --no-daemon :domain:compileKotlin 2>&1 | grep -E 'error:|BUILD'"
