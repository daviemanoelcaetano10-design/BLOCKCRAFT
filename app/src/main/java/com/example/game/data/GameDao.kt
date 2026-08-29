package com.example.game.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM worlds ORDER BY updatedAt DESC")
    fun getAllWorlds(): Flow<List<WorldEntity>>

    @Query("SELECT * FROM worlds WHERE id = :id LIMIT 1")
    suspend fun getWorldById(id: Long): WorldEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorld(world: WorldEntity): Long

    @Update
    suspend fun updateWorld(world: WorldEntity)

    @Query("DELETE FROM worlds WHERE id = :id")
    suspend fun deleteWorldById(id: Long)
}
