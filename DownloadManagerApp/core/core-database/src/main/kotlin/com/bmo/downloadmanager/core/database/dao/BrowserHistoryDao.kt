package com.bmo.downloadmanager.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bmo.downloadmanager.core.database.entity.BrowserHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BrowserHistoryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: BrowserHistoryEntity): Long

    @Query("SELECT * FROM browser_history WHERE url = :url LIMIT 1")
    suspend fun findByUrl(url: String): BrowserHistoryEntity?

    /**
     * Registra una visita: si la URL ya existe en el historial, incrementa
     * visitCount y actualiza lastVisitedAt; si no existe, inserta una fila
     * nueva. No se usa @Insert(OnConflictStrategy.REPLACE) porque url no
     * es la PrimaryKey (puede haber múltiples títulos/favicons históricos
     * para la misma URL si cambia con el tiempo) — el upsert se resuelve
     * explícitamente aquí en dos pasos en lugar de delegarlo a un
     * conflict strategy de Room que no aplica a este caso.
     */
    @androidx.room.Transaction
    suspend fun recordVisit(url: String, title: String?, favicon: ByteArray?, visitedAt: Long) {
        val existing = findByUrl(url)
        if (existing != null) {
            incrementVisit(existing.id, visitedAt, title, favicon)
        } else {
            insert(
                BrowserHistoryEntity(
                    url = url,
                    title = title,
                    favicon = favicon,
                    lastVisitedAt = visitedAt,
                    visitCount = 1
                )
            )
        }
    }

    @Query(
        """
        UPDATE browser_history
        SET last_visited_at = :visitedAt,
            visit_count = visit_count + 1,
            title = COALESCE(:title, title),
            favicon = COALESCE(:favicon, favicon)
        WHERE id = :id
        """
    )
    suspend fun incrementVisit(id: Long, visitedAt: Long, title: String?, favicon: ByteArray?)

    @Query("SELECT * FROM browser_history ORDER BY last_visited_at DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<BrowserHistoryEntity>>

    @Query("SELECT * FROM browser_history WHERE url LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' ORDER BY last_visited_at DESC")
    fun search(query: String): Flow<List<BrowserHistoryEntity>>

    @Query("DELETE FROM browser_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM browser_history")
    suspend fun clearAll()
}
