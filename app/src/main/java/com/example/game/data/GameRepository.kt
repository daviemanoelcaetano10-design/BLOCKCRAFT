package com.example.game.data

import kotlinx.coroutines.flow.Flow

class GameRepository(private val dao: GameDao) {
    val allWorlds: Flow<List<WorldEntity>> = dao.getAllWorlds()

    suspend fun getWorld(id: Long): WorldEntity? = dao.getWorldById(id)

    suspend fun saveWorld(world: WorldEntity): Long = dao.insertWorld(world)

    suspend fun updateWorld(world: WorldEntity) = dao.updateWorld(world)

    suspend fun deleteWorld(id: Long) = dao.deleteWorldById(id)
}
