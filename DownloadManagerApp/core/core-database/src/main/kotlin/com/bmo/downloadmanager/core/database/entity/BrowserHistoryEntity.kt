package com.bmo.downloadmanager.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Una entrada de historial del navegador embebido. Cada visita a una URL
 * genera o actualiza una fila (ver visitCount: si el usuario vuelve a
 * visitar la misma URL, se actualiza lastVisitedAt y se incrementa el
 * contador en lugar de insertar una fila duplicada — la lógica exacta de
 * upsert vive en el DAO).
 *
 * favicon se guarda como ByteArray (BLOB) en lugar de una URL al favicon
 * remoto: así el historial sigue siendo navegable visualmente sin
 * depender de que el recurso remoto siga disponible, consistente con el
 * resto del proyecto evitando dependencias externas frágiles.
 */
@Entity(
    tableName = "browser_history",
    indices = [Index(value = ["url"])]
)
data class BrowserHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "title")
    val title: String?,

    @ColumnInfo(name = "favicon")
    val favicon: ByteArray? = null,

    @ColumnInfo(name = "last_visited_at")
    val lastVisitedAt: Long,

    @ColumnInfo(name = "visit_count")
    val visitCount: Int = 1
) {
    // Room no usa equals/hashCode para persistencia, pero data class con
    // ByteArray genera equals/hashCode incorrectos por defecto (compara
    // referencia, no contenido) si se usa esta entidad en sets/listas
    // comparadas en memoria (ej. en tests). Se sobreescribe explícitamente
    // para evitar ese bug silencioso.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BrowserHistoryEntity) return false
        return id == other.id &&
            url == other.url &&
            title == other.title &&
            (favicon?.contentEquals(other.favicon) ?: (other.favicon == null)) &&
            lastVisitedAt == other.lastVisitedAt &&
            visitCount == other.visitCount
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (favicon?.contentHashCode() ?: 0)
        result = 31 * result + lastVisitedAt.hashCode()
        result = 31 * result + visitCount
        return result
    }
}
