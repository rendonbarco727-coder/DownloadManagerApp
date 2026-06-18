package com.bmo.downloadmanager.core.database.converter

import androidx.room.TypeConverter
import com.bmo.downloadmanager.core.database.entity.DownloadStatus
import com.bmo.downloadmanager.core.database.entity.SegmentStatus
import com.bmo.downloadmanager.core.database.entity.SettingValueType

/**
 * Convierte los enums de las entidades a/desde String para persistirlos
 * en SQLite (que no tiene un tipo enum nativo). Se persiste el nombre
 * del enum, no su ordinal, para que la base de datos sea legible
 * directamente y para que agregar un valor nuevo al enum en medio de la
 * lista no reasigne accidentalmente el significado de filas existentes.
 */
class Converters {

    @TypeConverter
    fun fromDownloadStatus(status: DownloadStatus): String = status.name

    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus = DownloadStatus.valueOf(value)

    @TypeConverter
    fun fromSegmentStatus(status: SegmentStatus): String = status.name

    @TypeConverter
    fun toSegmentStatus(value: String): SegmentStatus = SegmentStatus.valueOf(value)

    @TypeConverter
    fun fromSettingValueType(type: SettingValueType): String = type.name

    @TypeConverter
    fun toSettingValueType(value: String): SettingValueType = SettingValueType.valueOf(value)
}
