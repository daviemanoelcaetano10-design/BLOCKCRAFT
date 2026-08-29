package com.example.game.core

import androidx.compose.ui.graphics.Color

enum class BlockSoundType {
    GRASS, DIRT, STONE, WOOD, GLASS, SAND, METAL, WOOL
}

enum class BlockType(
    val id: Byte,
    val displayName: String,
    val hardness: Float, // seconds to break with basic hand
    val isTransparent: Boolean = false,
    val isSolid: Boolean = true,
    val isLightEmitter: Boolean = false,
    val soundType: BlockSoundType = BlockSoundType.STONE,
    val mapColor: Color = Color.Gray,
    val topTextureIndex: Int = 0,
    val sideTextureIndex: Int = 0,
    val bottomTextureIndex: Int = 0
) {
    AIR(
        id = 0,
        displayName = "Ar",
        hardness = 0f,
        isTransparent = true,
        isSolid = false,
        mapColor = Color.Transparent
    ),
    GRASS(
        id = 1,
        displayName = "Bloco de Grama",
        hardness = 0.6f,
        soundType = BlockSoundType.GRASS,
        mapColor = Color(0xFF4CAF50),
        topTextureIndex = 0,
        sideTextureIndex = 1,
        bottomTextureIndex = 2
    ),
    DIRT(
        id = 2,
        displayName = "Terra",
        hardness = 0.5f,
        soundType = BlockSoundType.DIRT,
        mapColor = Color(0xFF795548),
        topTextureIndex = 2,
        sideTextureIndex = 2,
        bottomTextureIndex = 2
    ),
    STONE(
        id = 3,
        displayName = "Pedra Natural",
        hardness = 1.5f,
        soundType = BlockSoundType.STONE,
        mapColor = Color(0xFF9E9E9E),
        topTextureIndex = 3,
        sideTextureIndex = 3,
        bottomTextureIndex = 3
    ),
    COBBLESTONE(
        id = 4,
        displayName = "Pedregulho",
        hardness = 1.2f,
        soundType = BlockSoundType.STONE,
        mapColor = Color(0xFF757575),
        topTextureIndex = 4,
        sideTextureIndex = 4,
        bottomTextureIndex = 4
    ),
    WOOD_OAK(
        id = 5,
        displayName = "Tronco de Carvalho",
        hardness = 1.0f,
        soundType = BlockSoundType.WOOD,
        mapColor = Color(0xFF5D4037),
        topTextureIndex = 5,
        sideTextureIndex = 6,
        bottomTextureIndex = 5
    ),
    WOOD_PLANK(
        id = 6,
        displayName = "Tábuas de Madeira",
        hardness = 0.8f,
        soundType = BlockSoundType.WOOD,
        mapColor = Color(0xFFBCAAA4),
        topTextureIndex = 7,
        sideTextureIndex = 7,
        bottomTextureIndex = 7
    ),
    LEAVES(
        id = 7,
        displayName = "Folhas de Carvalho",
        hardness = 0.2f,
        isTransparent = true,
        soundType = BlockSoundType.GRASS,
        mapColor = Color(0xFF2E7D32),
        topTextureIndex = 8,
        sideTextureIndex = 8,
        bottomTextureIndex = 8
    ),
    SAND(
        id = 8,
        displayName = "Areia Fina",
        hardness = 0.4f,
        soundType = BlockSoundType.SAND,
        mapColor = Color(0xFFFFD54F),
        topTextureIndex = 9,
        sideTextureIndex = 9,
        bottomTextureIndex = 9
    ),
    WATER(
        id = 9,
        displayName = "Água Cristalina",
        hardness = 100f,
        isTransparent = true,
        isSolid = false,
        soundType = BlockSoundType.DIRT,
        mapColor = Color(0xFF2196F3),
        topTextureIndex = 10,
        sideTextureIndex = 10,
        bottomTextureIndex = 10
    ),
    GLASS(
        id = 10,
        displayName = "Vidro Transparente",
        hardness = 0.3f,
        isTransparent = true,
        soundType = BlockSoundType.GLASS,
        mapColor = Color(0xFFE0F7FA),
        topTextureIndex = 11,
        sideTextureIndex = 11,
        bottomTextureIndex = 11
    ),
    STONE_BRICK(
        id = 11,
        displayName = "Tijolos de Pedra",
        hardness = 1.3f,
        soundType = BlockSoundType.STONE,
        mapColor = Color(0xFF616161),
        topTextureIndex = 12,
        sideTextureIndex = 12,
        bottomTextureIndex = 12
    ),
    RED_BRICK(
        id = 12,
        displayName = "Tijolos Vermelhos",
        hardness = 1.2f,
        soundType = BlockSoundType.STONE,
        mapColor = Color(0xFFD32F2F),
        topTextureIndex = 13,
        sideTextureIndex = 13,
        bottomTextureIndex = 13
    ),
    COAL_ORE(
        id = 13,
        displayName = "Minério de Carvão",
        hardness = 1.8f,
        soundType = BlockSoundType.STONE,
        mapColor = Color(0xFF212121),
        topTextureIndex = 14,
        sideTextureIndex = 14,
        bottomTextureIndex = 14
    ),
    IRON_ORE(
        id = 14,
        displayName = "Minério de Ferro",
        hardness = 2.2f,
        soundType = BlockSoundType.STONE,
        mapColor = Color(0xFFFFCC80),
        topTextureIndex = 15,
        sideTextureIndex = 15,
        bottomTextureIndex = 15
    ),
    GOLD_ORE(
        id = 15,
        displayName = "Minério de Ouro",
        hardness = 2.5f,
        soundType = BlockSoundType.STONE,
        mapColor = Color(0xFFFFD700),
        topTextureIndex = 16,
        sideTextureIndex = 16,
        bottomTextureIndex = 16
    ),
    DIAMOND_ORE(
        id = 16,
        displayName = "Minério de Diamante",
        hardness = 3.0f,
        soundType = BlockSoundType.STONE,
        mapColor = Color(0xFF00E5FF),
        topTextureIndex = 17,
        sideTextureIndex = 17,
        bottomTextureIndex = 17
    ),
    REDSTONE_ORE(
        id = 17,
        displayName = "Cristal Energético",
        hardness = 2.0f,
        isLightEmitter = true,
        soundType = BlockSoundType.STONE,
        mapColor = Color(0xFFFF1744),
        topTextureIndex = 18,
        sideTextureIndex = 18,
        bottomTextureIndex = 18
    ),
    TORCH(
        id = 18,
        displayName = "Tocha Iluminadora",
        hardness = 0.1f,
        isTransparent = true,
        isSolid = false,
        isLightEmitter = true,
        soundType = BlockSoundType.WOOD,
        mapColor = Color(0xFFFF9800),
        topTextureIndex = 19,
        sideTextureIndex = 19,
        bottomTextureIndex = 19
    ),
    GOLD_BLOCK(
        id = 19,
        displayName = "Bloco de Ouro Puro",
        hardness = 2.0f,
        soundType = BlockSoundType.METAL,
        mapColor = Color(0xFFFFC107),
        topTextureIndex = 20,
        sideTextureIndex = 20,
        bottomTextureIndex = 20
    ),
    DIAMOND_BLOCK(
        id = 20,
        displayName = "Bloco de Diamante",
        hardness = 3.5f,
        soundType = BlockSoundType.METAL,
        mapColor = Color(0xFF18FFFF),
        topTextureIndex = 21,
        sideTextureIndex = 21,
        bottomTextureIndex = 21
    ),
    OBSIDIAN(
        id = 21,
        displayName = "Obsidiana Vulcânica",
        hardness = 5.0f,
        soundType = BlockSoundType.STONE,
        mapColor = Color(0xFF311B92),
        topTextureIndex = 22,
        sideTextureIndex = 22,
        bottomTextureIndex = 22
    ),
    BOOKSHELF(
        id = 22,
        displayName = "Estante de Livros",
        hardness = 0.9f,
        soundType = BlockSoundType.WOOD,
        mapColor = Color(0xFF8D6E63),
        topTextureIndex = 7,
        sideTextureIndex = 23,
        bottomTextureIndex = 7
    ),
    NEON_LAMP(
        id = 23,
        displayName = "Lâmpada Neon",
        hardness = 0.5f,
        isLightEmitter = true,
        soundType = BlockSoundType.GLASS,
        mapColor = Color(0xFF76FF03),
        topTextureIndex = 24,
        sideTextureIndex = 24,
        bottomTextureIndex = 24
    ),
    WHITE_CONCRETE(
        id = 24,
        displayName = "Concreto Branco",
        hardness = 1.1f,
        soundType = BlockSoundType.STONE,
        mapColor = Color(0xFFECEFF1),
        topTextureIndex = 25,
        sideTextureIndex = 25,
        bottomTextureIndex = 25
    ),
    BLUE_ROOF(
        id = 25,
        displayName = "Telhas Azuis",
        hardness = 1.0f,
        soundType = BlockSoundType.STONE,
        mapColor = Color(0xFF1976D2),
        topTextureIndex = 26,
        sideTextureIndex = 26,
        bottomTextureIndex = 26
    ),
    DARK_TILES(
        id = 26,
        displayName = "Ladrilhos Escuros",
        hardness = 1.3f,
        soundType = BlockSoundType.STONE,
        mapColor = Color(0xFF37474F),
        topTextureIndex = 27,
        sideTextureIndex = 27,
        bottomTextureIndex = 27
    ),
    FLOWER_ROSE(
        id = 27,
        displayName = "Rosa Vermelha",
        hardness = 0.1f,
        isTransparent = true,
        isSolid = false,
        soundType = BlockSoundType.GRASS,
        mapColor = Color(0xFFE91E63),
        topTextureIndex = 28,
        sideTextureIndex = 28,
        bottomTextureIndex = 28
    ),
    FLOWER_DANDELION(
        id = 28,
        displayName = "Flor Dourada",
        hardness = 0.1f,
        isTransparent = true,
        isSolid = false,
        soundType = BlockSoundType.GRASS,
        mapColor = Color(0xFFFFEB3B),
        topTextureIndex = 29,
        sideTextureIndex = 29,
        bottomTextureIndex = 29
    );

    companion object {
        private val ID_MAP = entries.associateBy { it.id }
        fun fromId(id: Byte): BlockType = ID_MAP[id] ?: AIR

        val PLACEABLE_BLOCKS = entries.filter { it != AIR && it != WATER }
    }
}
