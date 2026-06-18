package com.bmo.downloadmanager.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Una pestaña abierta del navegador embebido. Existe mientras la pestaña
 * existe (a diferencia de BrowserHistory, que persiste indefinidamente):
 * cerrar una pestaña borra su fila aquí, pero no borra las entradas que
 * esa pestaña ya generó en BrowserHistory.
 *
 * webViewState es el resultado de WebView.saveState() serializado
 * (Bundle -> ByteArray vía Parcel), no solo la URL actual. Esto es lo que
 * permite restaurar scroll position, historial de back/forward dentro de
 * la pestaña, y form data al reabrir la app — guardar solo currentUrl
 * perdería todo ese estado y equivaldría a recargar la página desde cero.
 *
 * isSessionRestored distingue una pestaña que el usuario abrió en esta
 * sesión activa de una que todavía espera ser restaurada al iniciar la
 * app (lazy restore: no todas las pestañas necesitan re-hidratar su
 * WebView inmediatamente al arrancar, especialmente si hay muchas).
 */
@Entity(tableName = "browser_tabs")
data class BrowserTabEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "current_url")
    val currentUrl: String,

    @ColumnInfo(name = "title")
    val title: String?,

    @ColumnInfo(name = "favicon")
    val favicon: ByteArray? = null,

    @ColumnInfo(name = "tab_order")
    val tabOrder: Int,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = false,

    @ColumnInfo(name = "web_view_state")
    val webViewState: ByteArray? = null,

    @ColumnInfo(name = "is_session_restored")
    val isSessionRestored: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "last_accessed_at")
    val lastAccessedAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BrowserTabEntity) return false
        return id == other.id &&
            currentUrl == other.currentUrl &&
            title == other.title &&
            (favicon?.contentEquals(other.favicon) ?: (other.favicon == null)) &&
            tabOrder == other.tabOrder &&
            isActive == other.isActive &&
            (webViewState?.contentEquals(other.webViewState) ?: (other.webViewState == null)) &&
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
