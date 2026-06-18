#!/bin/bash
# ============================================================
# Script: create_data_v2.sh
# 1. Corrige modelos de dominio para que coincidan con entidades Room
# 2. Crea módulo :data completo con mappers y repositorios correctos
# ============================================================

BASE=~/DownloadManagerApp
DOMAIN=$BASE/domain/src/main/kotlin/com/bmo/downloadmanager/domain
DATA=$BASE/data/src/main/kotlin/com/bmo/downloadmanager/data

# ════════════════════════════════════════════════════════════
# PARTE 1 — Corregir modelos de dominio
# ════════════════════════════════════════════════════════════

echo "=== Corrigiendo modelos de dominio ==="

# ── Enums ────────────────────────────────────────────────────
cat > $DOMAIN/enums/SegmentStatus.kt << 'EOF'
package com.bmo.downloadmanager.domain.enums

enum class SegmentStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}
EOF
echo "✓ SegmentStatus.kt (+PAUSED)"

cat > $DOMAIN/enums/SettingValueType.kt << 'EOF'
package com.bmo.downloadmanager.domain.enums

enum class SettingValueType {
    STRING,
    INT,
    LONG,
    BOOLEAN,
    FLOAT
}
EOF
echo "✓ SettingValueType.kt (+LONG)"

# ── Download model ───────────────────────────────────────────
cat > $DOMAIN/model/Download.kt << 'EOF'
package com.bmo.downloadmanager.domain.model

import com.bmo.downloadmanager.domain.enums.DownloadStatus

data class Download(
    val id: Long = 0,
    val sourceUrl: String,
    val fileName: String,
    val destinationUri: String,
    val mimeType: String? = null,
    val totalBytes: Long = -1L,
    val downloadedBytes: Long = 0L,
    val totalSegments: Int = 1,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val refererHeader: String? = null,
    val userAgentHeader: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val errorMessage: String? = null
)
EOF
echo "✓ Download.kt"

# ── DownloadSegment model ────────────────────────────────────
cat > $DOMAIN/model/DownloadSegment.kt << 'EOF'
package com.bmo.downloadmanager.domain.model

import com.bmo.downloadmanager.domain.enums.SegmentStatus

data class DownloadSegment(
    val id: Long = 0,
    val downloadId: Long,
    val segmentIndex: Int,
    val rangeStart: Long,
    val rangeEnd: Long,
    val bytesDownloaded: Long = 0L,
    val segmentUrl: String,
    val status: SegmentStatus = SegmentStatus.PENDING,
    val tempFilePath: String? = null
)
EOF
echo "✓ DownloadSegment.kt"

# ── BrowserHistory model ─────────────────────────────────────
cat > $DOMAIN/model/BrowserHistory.kt << 'EOF'
package com.bmo.downloadmanager.domain.model

data class BrowserHistory(
    val id: Long = 0,
    val url: String,
    val title: String? = null,
    val favicon: ByteArray? = null,
    val visitCount: Int = 1,
    val lastVisitedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BrowserHistory) return false
        return id == other.id &&
            url == other.url &&
            title == other.title &&
            (favicon?.contentEquals(other.favicon ?: byteArrayOf()) ?: (other.favicon == null)) &&
            visitCount == other.visitCount &&
            lastVisitedAt == other.lastVisitedAt
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (favicon?.contentHashCode() ?: 0)
        result = 31 * result + visitCount
        result = 31 * result + lastVisitedAt.hashCode()
        return result
    }
}
EOF
echo "✓ BrowserHistory.kt"

# ── BrowserTab model ─────────────────────────────────────────
cat > $DOMAIN/model/BrowserTab.kt << 'EOF'
package com.bmo.downloadmanager.domain.model

data class BrowserTab(
    val id: Long = 0,
    val currentUrl: String,
    val title: String? = null,
    val favicon: ByteArray? = null,
    val tabOrder: Int,
    val isActive: Boolean = false,
    val webViewState: ByteArray? = null,
    val isSessionRestored: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BrowserTab) return false
        return id == other.id &&
            currentUrl == other.currentUrl &&
            title == other.title &&
            (favicon?.contentEquals(other.favicon ?: byteArrayOf()) ?: (other.favicon == null)) &&
            tabOrder == other.tabOrder &&
            isActive == other.isActive &&
            (webViewState?.contentEquals(other.webViewState ?: byteArrayOf()) ?: (other.webViewState == null)) &&
            isSessionRestored == other.isSessionRestored &&
            createdAt == other.createdAt &&
            lastAccessedAt == other.lastAccessedAt
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + currentUrl.hashCode()
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (favicon?.contentHashCode() ?: 0)
        result = 31 * result + tabOrder
        result = 31 * result + isActive.hashCode()
        result = 31 * result + (webViewState?.contentHashCode() ?: 0)
        result = 31 * result + isSessionRestored.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + lastAccessedAt.hashCode()
        return result
    }
}
EOF
echo "✓ BrowserTab.kt"

# ── AppSettings model ────────────────────────────────────────
cat > $DOMAIN/model/AppSettings.kt << 'EOF'
package com.bmo.downloadmanager.domain.model

import com.bmo.downloadmanager.domain.enums.SettingValueType

data class AppSettings(
    val key: String,
    val value: String,
    val type: SettingValueType = SettingValueType.STRING,
    val updatedAt: Long = System.currentTimeMillis()
)
EOF
echo "✓ AppSettings.kt"

# ── Actualizar use cases que usan campos renombrados ─────────
# GetBrowserHistoryUseCase no cambia
# Los repositorios de tab/browser usan los modelos actualizados automáticamente

echo ""
echo "=== Modelos de dominio corregidos ==="

# ════════════════════════════════════════════════════════════
# PARTE 2 — Crear módulo :data
# ════════════════════════════════════════════════════════════

echo ""
echo "=== Creando módulo :data ==="

mkdir -p $DATA/mapper
mkdir -p $DATA/repository
mkdir -p $DATA/di
echo "✓ Directorios creados"

# ── build.gradle.kts ─────────────────────────────────────────
cat > $BASE/data/build.gradle.kts << 'EOF'
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.bmo.downloadmanager.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(project(":core:core-database"))
    implementation(project(":domain"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
}
EOF
echo "✓ data/build.gradle.kts"

mkdir -p $BASE/data/src/main
cat > $BASE/data/src/main/AndroidManifest.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest />
EOF
echo "✓ AndroidManifest.xml"

# ── MAPPERS ──────────────────────────────────────────────────
cat > $DATA/mapper/DownloadMapper.kt << 'EOF'
package com.bmo.downloadmanager.data.mapper

import com.bmo.downloadmanager.core.database.entity.DownloadEntity
import com.bmo.downloadmanager.core.database.entity.DownloadSegmentEntity
import com.bmo.downloadmanager.core.database.entity.DownloadStatus as EntityDownloadStatus
import com.bmo.downloadmanager.core.database.entity.SegmentStatus as EntitySegmentStatus
import com.bmo.downloadmanager.domain.enums.DownloadStatus as DomainDownloadStatus
import com.bmo.downloadmanager.domain.enums.SegmentStatus as DomainSegmentStatus
import com.bmo.downloadmanager.domain.model.Download
import com.bmo.downloadmanager.domain.model.DownloadSegment

fun DownloadEntity.toDomain(): Download = Download(
    id = id,
    sourceUrl = sourceUrl,
    fileName = fileName,
    destinationUri = destinationUri,
    mimeType = mimeType,
    totalBytes = totalBytes,
    downloadedBytes = downloadedBytes,
    totalSegments = totalSegments,
    status = DomainDownloadStatus.valueOf(status.name),
    refererHeader = refererHeader,
    userAgentHeader = userAgentHeader,
    createdAt = createdAt,
    completedAt = completedAt,
    errorMessage = errorMessage
)

fun Download.toEntity(): DownloadEntity = DownloadEntity(
    id = id,
    sourceUrl = sourceUrl,
    fileName = fileName,
    destinationUri = destinationUri,
    mimeType = mimeType,
    totalBytes = totalBytes,
    downloadedBytes = downloadedBytes,
    totalSegments = totalSegments,
    status = EntityDownloadStatus.valueOf(status.name),
    refererHeader = refererHeader,
    userAgentHeader = userAgentHeader,
    createdAt = createdAt,
    completedAt = completedAt,
    errorMessage = errorMessage
)

fun DownloadSegmentEntity.toDomain(): DownloadSegment = DownloadSegment(
    id = id,
    downloadId = downloadId,
    segmentIndex = segmentIndex,
    rangeStart = rangeStart,
    rangeEnd = rangeEnd,
    bytesDownloaded = bytesDownloaded,
    segmentUrl = segmentUrl,
    status = DomainSegmentStatus.valueOf(status.name),
    tempFilePath = tempFilePath
)

fun DownloadSegment.toEntity(): DownloadSegmentEntity = DownloadSegmentEntity(
    id = id,
    downloadId = downloadId,
    segmentIndex = segmentIndex,
    rangeStart = rangeStart,
    rangeEnd = rangeEnd,
    bytesDownloaded = bytesDownloaded,
    segmentUrl = segmentUrl,
    status = EntitySegmentStatus.valueOf(status.name),
    tempFilePath = tempFilePath
)
EOF
echo "✓ DownloadMapper.kt"

cat > $DATA/mapper/BrowserMapper.kt << 'EOF'
package com.bmo.downloadmanager.data.mapper

import com.bmo.downloadmanager.core.database.entity.BrowserHistoryEntity
import com.bmo.downloadmanager.core.database.entity.BrowserTabEntity
import com.bmo.downloadmanager.domain.model.BrowserHistory
import com.bmo.downloadmanager.domain.model.BrowserTab

fun BrowserHistoryEntity.toDomain(): BrowserHistory = BrowserHistory(
    id = id,
    url = url,
    title = title,
    favicon = favicon,
    visitCount = visitCount,
    lastVisitedAt = lastVisitedAt
)

fun BrowserHistory.toEntity(): BrowserHistoryEntity = BrowserHistoryEntity(
    id = id,
    url = url,
    title = title,
    favicon = favicon,
    lastVisitedAt = lastVisitedAt,
    visitCount = visitCount
)

fun BrowserTabEntity.toDomain(): BrowserTab = BrowserTab(
    id = id,
    currentUrl = currentUrl,
    title = title,
    favicon = favicon,
    tabOrder = tabOrder,
    isActive = isActive,
    webViewState = webViewState,
    isSessionRestored = isSessionRestored,
    createdAt = createdAt,
    lastAccessedAt = lastAccessedAt
)

fun BrowserTab.toEntity(): BrowserTabEntity = BrowserTabEntity(
    id = id,
    currentUrl = currentUrl,
    title = title,
    favicon = favicon,
    tabOrder = tabOrder,
    isActive = isActive,
    webViewState = webViewState,
    isSessionRestored = isSessionRestored,
    createdAt = createdAt,
    lastAccessedAt = lastAccessedAt
)
EOF
echo "✓ BrowserMapper.kt"

cat > $DATA/mapper/SettingsMapper.kt << 'EOF'
package com.bmo.downloadmanager.data.mapper

import com.bmo.downloadmanager.core.database.entity.SettingsEntity
import com.bmo.downloadmanager.core.database.entity.SettingValueType as EntitySettingValueType
import com.bmo.downloadmanager.domain.enums.SettingValueType as DomainSettingValueType
import com.bmo.downloadmanager.domain.model.AppSettings

fun SettingsEntity.toDomain(): AppSettings = AppSettings(
    key = key,
    value = value,
    type = DomainSettingValueType.valueOf(valueType.name),
    updatedAt = updatedAt
)

fun AppSettings.toEntity(): SettingsEntity = SettingsEntity(
    key = key,
    value = value,
    valueType = EntitySettingValueType.valueOf(type.name),
    updatedAt = updatedAt
)
EOF
echo "✓ SettingsMapper.kt"

# ── REPOSITORIOS ─────────────────────────────────────────────
cat > $DATA/repository/DownloadRepositoryImpl.kt << 'EOF'
package com.bmo.downloadmanager.data.repository

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.core.database.dao.DownloadDao
import com.bmo.downloadmanager.core.database.dao.DownloadSegmentDao
import com.bmo.downloadmanager.data.mapper.toDomain
import com.bmo.downloadmanager.data.mapper.toEntity
import com.bmo.downloadmanager.domain.model.Download
import com.bmo.downloadmanager.domain.model.DownloadSegment
import com.bmo.downloadmanager.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DownloadRepositoryImpl @Inject constructor(
    private val downloadDao: DownloadDao,
    private val segmentDao: DownloadSegmentDao
) : DownloadRepository {

    override fun getDownloads(): Flow<AppResult<List<Download>>> =
        downloadDao.observeAll()
            .map<_, AppResult<List<Download>>> { entities ->
                AppResult.Success(entities.map { it.toDomain() })
            }
            .catch { emit(AppResult.Error(Exception(it))) }

    override suspend fun getDownloadById(id: Long): AppResult<Download> =
        try {
            val entity = downloadDao.getById(id)
            if (entity != null) AppResult.Success(entity.toDomain())
            else AppResult.Error(Exception("Download not found: $id"))
        } catch (e: Exception) {
            AppResult.Error(e)
        }

    override suspend fun insertDownload(download: Download): AppResult<Long> =
        try {
            val id = downloadDao.insert(download.toEntity())
            AppResult.Success(id)
        } catch (e: Exception) {
            AppResult.Error(e)
        }

    override suspend fun updateDownload(download: Download): AppResult<Unit> =
        try {
            downloadDao.update(download.toEntity())
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(e)
        }

    override suspend fun deleteDownload(id: Long): AppResult<Unit> =
        try {
            downloadDao.deleteById(id)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(e)
        }

    override fun getSegmentsForDownload(downloadId: Long): Flow<AppResult<List<DownloadSegment>>> =
        segmentDao.observeSegmentsForDownload(downloadId)
            .map<_, AppResult<List<DownloadSegment>>> { entities ->
                AppResult.Success(entities.map { it.toDomain() })
            }
            .catch { emit(AppResult.Error(Exception(it))) }

    override suspend fun insertSegment(segment: DownloadSegment): AppResult<Long> =
        try {
            val id = segmentDao.insert(segment.toEntity())
            AppResult.Success(id)
        } catch (e: Exception) {
            AppResult.Error(e)
        }

    override suspend fun updateSegment(segment: DownloadSegment): AppResult<Unit> =
        try {
            segmentDao.update(segment.toEntity())
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(e)
        }
}
EOF
echo "✓ DownloadRepositoryImpl.kt"

cat > $DATA/repository/BrowserHistoryRepositoryImpl.kt << 'EOF'
package com.bmo.downloadmanager.data.repository

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.core.database.dao.BrowserHistoryDao
import com.bmo.downloadmanager.data.mapper.toDomain
import com.bmo.downloadmanager.data.mapper.toEntity
import com.bmo.downloadmanager.domain.model.BrowserHistory
import com.bmo.downloadmanager.domain.repository.BrowserHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BrowserHistoryRepositoryImpl @Inject constructor(
    private val dao: BrowserHistoryDao
) : BrowserHistoryRepository {

    override fun getHistory(): Flow<AppResult<List<BrowserHistory>>> =
        dao.observeRecent()
            .map<_, AppResult<List<BrowserHistory>>> { entities ->
                AppResult.Success(entities.map { it.toDomain() })
            }
            .catch { emit(AppResult.Error(Exception(it))) }

    override suspend fun insertOrUpdateHistory(entry: BrowserHistory): AppResult<Unit> =
        try {
            dao.recordVisit(
                url = entry.url,
                title = entry.title,
                favicon = entry.favicon,
                visitedAt = entry.lastVisitedAt
            )
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(e)
        }

    override suspend fun deleteHistoryEntry(id: Long): AppResult<Unit> =
        try {
            dao.deleteById(id)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(e)
        }

    override suspend fun clearAllHistory(): AppResult<Unit> =
        try {
            dao.clearAll()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(e)
        }

    override fun searchHistory(query: String): Flow<AppResult<List<BrowserHistory>>> =
        dao.search(query)
            .map<_, AppResult<List<BrowserHistory>>> { entities ->
                AppResult.Success(entities.map { it.toDomain() })
            }
            .catch { emit(AppResult.Error(Exception(it))) }
}
EOF
echo "✓ BrowserHistoryRepositoryImpl.kt"

cat > $DATA/repository/BrowserTabRepositoryImpl.kt << 'EOF'
package com.bmo.downloadmanager.data.repository

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.core.database.dao.BrowserTabDao
import com.bmo.downloadmanager.data.mapper.toDomain
import com.bmo.downloadmanager.data.mapper.toEntity
import com.bmo.downloadmanager.domain.model.BrowserTab
import com.bmo.downloadmanager.domain.repository.BrowserTabRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BrowserTabRepositoryImpl @Inject constructor(
    private val dao: BrowserTabDao
) : BrowserTabRepository {

    override fun getTabs(): Flow<AppResult<List<BrowserTab>>> =
        dao.observeAll()
            .map<_, AppResult<List<BrowserTab>>> { entities ->
                AppResult.Success(entities.map { it.toDomain() })
            }
            .catch { emit(AppResult.Error(Exception(it))) }

    override suspend fun getActiveTab(): AppResult<BrowserTab?> =
        try {
            val entity = dao.getActiveTab()
            AppResult.Success(entity?.toDomain())
        } catch (e: Exception) {
            AppResult.Error(e)
        }

    override suspend fun insertTab(tab: BrowserTab): AppResult<Long> =
        try {
            val id = dao.insert(tab.toEntity())
            AppResult.Success(id)
        } catch (e: Exception) {
            AppResult.Error(e)
        }

    override suspend fun updateTab(tab: BrowserTab): AppResult<Unit> =
        try {
            dao.update(tab.toEntity())
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(e)
        }

    override suspend fun deleteTab(id: Long): AppResult<Unit> =
        try {
            dao.deleteById(id)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(e)
        }

    override suspend fun setActiveTab(id: Long): AppResult<Unit> =
        try {
            dao.setActiveTab(id)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(e)
        }
}
EOF
echo "✓ BrowserTabRepositoryImpl.kt"

cat > $DATA/repository/SettingsRepositoryImpl.kt << 'EOF'
package com.bmo.downloadmanager.data.repository

import com.bmo.downloadmanager.core.common.result.AppResult
import com.bmo.downloadmanager.core.database.dao.SettingsDao
import com.bmo.downloadmanager.data.mapper.toDomain
import com.bmo.downloadmanager.data.mapper.toEntity
import com.bmo.downloadmanager.domain.model.AppSettings
import com.bmo.downloadmanager.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val dao: SettingsDao
) : SettingsRepository {

    override fun getAllSettings(): Flow<AppResult<List<AppSettings>>> =
        dao.observeAll()
            .map<_, AppResult<List<AppSettings>>> { entities ->
                AppResult.Success(entities.map { it.toDomain() })
            }
            .catch { emit(AppResult.Error(Exception(it))) }

    override suspend fun getSetting(key: String): AppResult<AppSettings?> =
        try {
            val entity = dao.getByKey(key)
            AppResult.Success(entity?.toDomain())
        } catch (e: Exception) {
            AppResult.Error(e)
        }

    override suspend fun setSetting(setting: AppSettings): AppResult<Unit> =
        try {
            dao.upsert(setting.toEntity())
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(e)
        }

    override suspend fun deleteSetting(key: String): AppResult<Unit> =
        try {
            dao.deleteByKey(key)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(e)
        }
}
EOF
echo "✓ SettingsRepositoryImpl.kt"

# ── MÓDULO HILT ──────────────────────────────────────────────
cat > $DATA/di/DataModule.kt << 'EOF'
package com.bmo.downloadmanager.data.di

import com.bmo.downloadmanager.data.repository.BrowserHistoryRepositoryImpl
import com.bmo.downloadmanager.data.repository.BrowserTabRepositoryImpl
import com.bmo.downloadmanager.data.repository.DownloadRepositoryImpl
import com.bmo.downloadmanager.data.repository.SettingsRepositoryImpl
import com.bmo.downloadmanager.domain.repository.BrowserHistoryRepository
import com.bmo.downloadmanager.domain.repository.BrowserTabRepository
import com.bmo.downloadmanager.domain.repository.DownloadRepository
import com.bmo.downloadmanager.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(
        impl: DownloadRepositoryImpl
    ): DownloadRepository

    @Binds
    @Singleton
    abstract fun bindBrowserHistoryRepository(
        impl: BrowserHistoryRepositoryImpl
    ): BrowserHistoryRepository

    @Binds
    @Singleton
    abstract fun bindBrowserTabRepository(
        impl: BrowserTabRepositoryImpl
    ): BrowserTabRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository
}
EOF
echo "✓ DataModule.kt"

# ── settings.gradle.kts ──────────────────────────────────────
if ! grep -q '":data"' $BASE/settings.gradle.kts; then
    sed -i 's/include(":domain")/include(":domain")\ninclude(":data")/' $BASE/settings.gradle.kts
    echo "✓ :data agregado a settings.gradle.kts"
else
    echo "⚠ :data ya estaba en settings.gradle.kts"
fi

# ── app/build.gradle.kts ─────────────────────────────────────
sed -i 's|    // implementation(project(":data"))  // TODO: módulo pendiente|    implementation(project(":data"))|' $BASE/app/build.gradle.kts
echo "✓ :data descomentado en app/build.gradle.kts (si estaba comentado)"

echo ""
echo "=== TODO LISTO — ejecuta: ==="
echo "cd ~/DownloadManagerApp && ./gradlew --no-daemon :data:compileDebugKotlin 2>&1 | grep -E 'error:|BUILD'"
