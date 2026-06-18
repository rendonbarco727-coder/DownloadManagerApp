package com.bmo.downloadmanager.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bmo.downloadmanager.core.database.entity.BrowserTabEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BrowserTabDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(tab: BrowserTabEntity): Long

    @Update
    suspend fun update(tab: BrowserTabEntity)

    @Delete
    suspend fun delete(tab: BrowserTabEntity)

    @Query("SELECT * FROM browser_tabs ORDER BY tab_order ASC")
    fun observeAll(): Flow<List<BrowserTabEntity>>

    @Query("SELECT * FROM browser_tabs WHERE id = :id")
    suspend fun getById(id: Long): BrowserTabEntity?

    @Query("SELECT * FROM browser_tabs WHERE is_active = 1 LIMIT 1")
    suspend fun getActiveTab(): BrowserTabEntity?

    /**
     * Pestañas que todavía no han re-hidratado su WebView en esta sesión
     * de la app (ver comentario de diseño en BrowserTabEntity sobre lazy
     * restore). El feature de browser consulta esto al arrancar para
     * decidir cuáles restaurar inmediatamente (ej. solo la activa) y
     * cuáles dejar en estado "pendiente" hasta que el usuario las
     * seleccione.
     */
    @Query("SELECT * FROM browser_tabs WHERE is_session_restored = 0 ORDER BY tab_order ASC")
    suspend fun getTabsPendingRestore(): List<BrowserTabEntity>

    @Query("UPDATE browser_tabs SET is_session_restored = 1 WHERE id = :id")
    suspend fun markAsRestored(id: Long)

    @Query("UPDATE browser_tabs SET is_active = 0")
    suspend fun clearActiveFlag()

    @androidx.room.Transaction
    suspend fun setActiveTab(id: Long) {
        clearActiveFlag()
        markActive(id)
    }

    @Query("UPDATE browser_tabs SET is_active = 1 WHERE id = :id")
    suspend fun markActive(id: Long)

    @Query(
        """
        UPDATE browser_tabs
        SET current_url = :url, title = :title, favicon = :favicon,
            web_view_state = :webViewState, last_accessed_at = :accessedAt
        WHERE id = :id
        """
    )
    suspend fun updateTabState(
        id: Long,
        url: String,
        title: String?,
        favicon: ByteArray?,
        webViewState: ByteArray?,
        accessedAt: Long
    )

    @Query("DELETE FROM browser_tabs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM browser_tabs")
    suspend fun clearAll()
}
