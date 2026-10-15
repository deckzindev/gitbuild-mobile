package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BuildRecordDao {

    @Query("SELECT * FROM build_history ORDER BY timestamp DESC")
    fun getAllBuilds(): Flow<List<BuildRecord>>

    @Query("SELECT * FROM build_history WHERE id = :id LIMIT 1")
    suspend fun getBuildById(id: String): BuildRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: BuildRecord)

    @Query("DELETE FROM build_history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM build_history")
    suspend fun clearAll()
}
