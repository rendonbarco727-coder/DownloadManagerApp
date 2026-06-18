package com.bmo.downloadmanager.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bmo.downloadmanager.core.database.converter.Converters
import com.bmo.downloadmanager.core.database.dao.BrowserHistoryDao
import com.bmo.downloadmanager.core.database.dao.BrowserTabDao
import com.bmo.downloadmanager.core.database.dao.DownloadDao
import com.bmo.downloadmanager.core.database.dao.DownloadSegmentDao
import com.bmo.downloadmanager.core.database.dao.SettingsDao
import com.bmo.downloadmanager.core.database.entity.BrowserHistoryEntity
import com.bmo.downloadmanager.core.database.entity.BrowserTabEntity
import com.bmo.downloadmanager.core.database.entity.DownloadEntity
import com.bmo.downloadmanager.core.database.entity.DownloadSegmentEntity
import com.bmo.downloadmanager.core.database.entity.SettingsEntity

/**
 * version = 1: este es el primer schema del proyecto, no hay datos
 * previos que migrar. A partir de la primera release real con usuarios,
 * cualquier cambio de schema (agregar/quitar columna, cambiar tipo)
 * requiere una Migration explícita en lugar de incrementar la versión
 * sin más — Room lanza una excepción en runtime si detecta un schema
 * distinto al esperado sin una ruta de migración registrada, a menos que
 * se use fallbackToDestructiveMigration() (NO recomendado fuera de
 * desarrollo temprano, porque borra todos los datos del usuario).
 *
 * exportSchema = true (default) junto con room.schemaLocation
 * configurado en build.gradle.kts genera el JSON del schema en
 * /schemas, que sirve como base para escribir y testear Migrations
 * futuras.
 */
@Database(
    entities = [
        DownloadEntity::class,
        DownloadSegmentEntity::class,
        BrowserHistoryEntity::class,
        BrowserTabEntity::class,
        SettingsEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun downloadDao(): DownloadDao
    abstract fun downloadSegmentDao(): DownloadSegmentDao
    abstract fun browserHistoryDao(): BrowserHistoryDao
    abstract fun browserTabDao(): BrowserTabDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        const val DATABASE_NAME = "download_manager.db"
    }
}
