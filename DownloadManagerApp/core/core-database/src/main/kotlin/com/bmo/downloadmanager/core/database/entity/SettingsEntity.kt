package com.bmo.downloadmanager.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Configuración de la app como pares key-value tipados, en lugar de una
 * columna fija por cada setting. Esto evita una migración de schema de
 * Room cada vez que se agrega una preferencia nueva (ej. "tema oscuro",
 * "carpeta de descargas por defecto", "máximo de descargas
 * concurrentes") — se agrega una fila nueva en runtime sin tocar el
 * schema.
 *
 * Nota de diseño pendiente de confirmar: si las preferencias terminan
 * siendo solo simples (booleanos, strings, ints sin relaciones entre
 * sí), probablemente conviene migrar esto a DataStore (ya está en el
 * version catalog: datastore-preferences 1.2.1) en lugar de Room, que es
 * la herramienta recomendada por Google específicamente para este caso
 * de uso y evita el overhead de SQL para datos que no se consultan con
 * JOINs ni queries complejas. Se deja en Room por ahora porque la tarea
 * pedía explícitamente 5 entidades de Room; revisar esta decisión antes
 * de implementar la lógica real de settings en un módulo posterior.
 */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey
    @ColumnInfo(name = "key")
    val key: String,

    @ColumnInfo(name = "value")
    val value: String,

    @ColumnInfo(name = "value_type")
    val valueType: SettingValueType,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)

enum class SettingValueType {
    STRING,
    BOOLEAN,
    INT,
    LONG,
    FLOAT
}
