package com.bmo.downloadmanager.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bmo.downloadmanager.core.database.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {

    // REPLACE es correcto aquí porque key SÍ es la PrimaryKey: a
    // diferencia de BrowserHistory (donde url no es PK y el upsert se
    // resuelve manualmente), un setting siempre tiene un único valor
    // vigente por key, así que sobreescribir directamente es la
    // semántica correcta.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setting: SettingsEntity)

    @Query("SELECT * FROM settings WHERE `key` = :key")
    suspend fun getByKey(key: String): SettingsEntity?

    @Query("SELECT * FROM settings WHERE `key` = :key")
    fun observeByKey(key: String): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings")
    fun observeAll(): Flow<List<SettingsEntity>>

    @Query("DELETE FROM settings WHERE `key` = :key")
    suspend fun deleteByKey(key: String)
}
