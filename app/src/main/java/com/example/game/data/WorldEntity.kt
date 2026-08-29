package com.example.game.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "worlds")
data class WorldEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val seed: Long,
    val gameMode: String = "SURVIVAL", // "SURVIVAL" or "CREATIVE"
    val timeOfDay: Float = 0.35f,
    val playerX: Float = 24.0f,
    val playerY: Float = 15.0f,
    val playerZ: Float = 24.0f,
    val playerYaw: Float = 0.0f,
    val playerPitch: Float = 0.0f,
    val modifiedBlocksData: String = "{}", // JSON map of index -> blockId
    val inventoryData: String = "[]", // JSON list of ItemStack
    val hotbarData: String = "[]",
    val selectedHotbarIndex: Int = 0,
    val blocksMined: Int = 0,
    val blocksPlaced: Int = 0,
    val structuresCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
