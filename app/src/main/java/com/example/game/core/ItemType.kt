package com.example.game.core

enum class ItemCategory {
    BLOCKS, TOOLS, MATERIALS, BLUEPRINTS, DECOR
}

data class GameItem(
    val id: String,
    val name: String,
    val category: ItemCategory,
    val blockType: BlockType? = null,
    val toolType: ToolType? = null,
    val maxStack: Int = 64,
    val description: String = ""
)

enum class ToolType(val multiplier: Float, val efficiency: BlockSoundType?) {
    HAND(1.0f, null),
    WOOD_PICKAXE(2.0f, BlockSoundType.STONE),
    STONE_PICKAXE(3.5f, BlockSoundType.STONE),
    IRON_PICKAXE(5.5f, BlockSoundType.STONE),
    DIAMOND_PICKAXE(8.0f, BlockSoundType.STONE),
    WOOD_AXE(2.0f, BlockSoundType.WOOD),
    IRON_AXE(5.0f, BlockSoundType.WOOD),
    BUILDER_WAND(1.0f, null) // Allows multi-block line/wall/floor placement!
}

data class ItemStack(
    val itemId: String,
    var count: Int
) {
    fun copyStack(): ItemStack = ItemStack(itemId, count)
}

data class CraftingIngredient(
    val itemId: String,
    val amount: Int
)

data class CraftingRecipe(
    val id: String,
    val name: String,
    val outputItemId: String,
    val outputAmount: Int,
    val ingredients: List<CraftingIngredient>,
    val category: ItemCategory = ItemCategory.BLOCKS,
    val description: String = ""
)

object ItemRegistry {
    val ITEMS = mutableMapOf<String, GameItem>()

    init {
        // Register all placeable blocks as items
        BlockType.entries.forEach { block ->
            if (block != BlockType.AIR) {
                val id = "block_${block.name.lowercase()}"
                ITEMS[id] = GameItem(
                    id = id,
                    name = block.displayName,
                    category = if (block.isLightEmitter || block == BlockType.FLOWER_ROSE || block == BlockType.FLOWER_DANDELION) ItemCategory.DECOR else ItemCategory.BLOCKS,
                    blockType = block,
                    description = "Bloco de construção 3D"
                )
            }
        }

        // Materials & Drops
        register(GameItem("stick", "Graveto de Madeira", ItemCategory.MATERIALS, description = "Usado para criar tochas e ferramentas"))
        register(GameItem("coal", "Carvão Mineral", ItemCategory.MATERIALS, description = "Combustível e fonte de luz"))
        register(GameItem("iron_ingot", "Barra de Ferro", ItemCategory.MATERIALS, description = "Metal resistente para ferramentas e blocos"))
        register(GameItem("gold_ingot", "Barra de Ouro", ItemCategory.MATERIALS, description = "Metal nobre e brilhante"))
        register(GameItem("diamond", "Diamante Precioso", ItemCategory.MATERIALS, description = "O mineral mais duro e valioso"))
        register(GameItem("energy_crystal", "Cristal de Energia", ItemCategory.MATERIALS, description = "Emite radiação de luz colorida"))

        // Tools
        register(GameItem("tool_wood_pickaxe", "Picareta de Madeira", ItemCategory.TOOLS, toolType = ToolType.WOOD_PICKAXE, maxStack = 1, description = "Minera pedras mais rápido"))
        register(GameItem("tool_stone_pickaxe", "Picareta de Pedra", ItemCategory.TOOLS, toolType = ToolType.STONE_PICKAXE, maxStack = 1, description = "Minera minérios de ferro e carvão"))
        register(GameItem("tool_iron_pickaxe", "Picareta de Ferro", ItemCategory.TOOLS, toolType = ToolType.IRON_PICKAXE, maxStack = 1, description = "Minera ouro e diamantes"))
        register(GameItem("tool_diamond_pickaxe", "Picareta de Diamante", ItemCategory.TOOLS, toolType = ToolType.DIAMOND_PICKAXE, maxStack = 1, description = "Minera qualquer bloco instantaneamente"))
        register(GameItem("tool_wood_axe", "Machado de Madeira", ItemCategory.TOOLS, toolType = ToolType.WOOD_AXE, maxStack = 1, description = "Corta troncos e folhas rapidamente"))
        register(GameItem("tool_iron_axe", "Machado de Ferro", ItemCategory.TOOLS, toolType = ToolType.IRON_AXE, maxStack = 1, description = "Corta madeira em alta velocidade"))
        register(GameItem("tool_builder_wand", "Varinha do Construtor", ItemCategory.TOOLS, toolType = ToolType.BUILDER_WAND, maxStack = 1, description = "Modo de construção rápida de paredes e pisos"))
    }

    private fun register(item: GameItem) {
        ITEMS[item.id] = item
    }

    fun getItem(id: String): GameItem? = ITEMS[id]
    fun getBlockItem(block: BlockType): GameItem? = ITEMS["block_${block.name.lowercase()}"]

    val RECIPES = listOf(
        CraftingRecipe(
            id = "planks_from_wood",
            name = "Tábuas de Madeira",
            outputItemId = "block_wood_plank",
            outputAmount = 4,
            ingredients = listOf(CraftingIngredient("block_wood_oak", 1)),
            category = ItemCategory.BLOCKS,
            description = "1 Tronco rende 4 Tábuas de Madeira"
        ),
        CraftingRecipe(
            id = "sticks_from_planks",
            name = "Gravetos",
            outputItemId = "stick",
            outputAmount = 4,
            ingredients = listOf(CraftingIngredient("block_wood_plank", 2)),
            category = ItemCategory.MATERIALS,
            description = "2 Tábuas rendem 4 Gravetos"
        ),
        CraftingRecipe(
            id = "stone_bricks",
            name = "Tijolos de Pedra Refinados",
            outputItemId = "block_stone_brick",
            outputAmount = 4,
            ingredients = listOf(CraftingIngredient("block_stone", 4)),
            category = ItemCategory.BLOCKS,
            description = "Estrutura resistente para castelos e muralhas"
        ),
        CraftingRecipe(
            id = "red_bricks",
            name = "Tijolos Vermelhos Cerâmicos",
            outputItemId = "block_red_brick",
            outputAmount = 4,
            ingredients = listOf(CraftingIngredient("block_dirt", 2), CraftingIngredient("block_sand", 2)),
            category = ItemCategory.BLOCKS,
            description = "Perfeito para casas coloniais e chaminés"
        ),
        CraftingRecipe(
            id = "glass_from_sand",
            name = "Vidro Translúcido",
            outputItemId = "block_glass",
            outputAmount = 2,
            ingredients = listOf(CraftingIngredient("block_sand", 2), CraftingIngredient("coal", 1)),
            category = ItemCategory.BLOCKS,
            description = "Vidro refinado para janelas panorâmicas"
        ),
        CraftingRecipe(
            id = "torches",
            name = "Tochas Luminescentes",
            outputItemId = "block_torch",
            outputAmount = 4,
            ingredients = listOf(CraftingIngredient("coal", 1), CraftingIngredient("stick", 1)),
            category = ItemCategory.DECOR,
            description = "Ilumina ambientes escuros e cavernas"
        ),
        CraftingRecipe(
            id = "neon_lamp",
            name = "Lâmpada Neon Futurista",
            outputItemId = "block_neon_lamp",
            outputAmount = 2,
            ingredients = listOf(CraftingIngredient("energy_crystal", 2), CraftingIngredient("block_glass", 2)),
            category = ItemCategory.DECOR,
            description = "Emite iluminação vibrante de alta potência"
        ),
        CraftingRecipe(
            id = "bookshelf",
            name = "Estante de Biblioteca",
            outputItemId = "block_bookshelf",
            outputAmount = 1,
            ingredients = listOf(CraftingIngredient("block_wood_plank", 6), CraftingIngredient("stick", 3)),
            category = ItemCategory.DECOR,
            description = "Decoração refinada para salas e castelos"
        ),
        CraftingRecipe(
            id = "white_concrete",
            name = "Concreto Arquitetônico Branco",
            outputItemId = "block_white_concrete",
            outputAmount = 4,
            ingredients = listOf(CraftingIngredient("block_sand", 2), CraftingIngredient("block_stone", 2)),
            category = ItemCategory.BLOCKS,
            description = "Acabamento minimalista e moderno"
        ),
        CraftingRecipe(
            id = "blue_roof",
            name = "Telhado Colonial Azul",
            outputItemId = "block_blue_roof",
            outputAmount = 4,
            ingredients = listOf(CraftingIngredient("block_stone_brick", 2), CraftingIngredient("energy_crystal", 1)),
            category = ItemCategory.BLOCKS,
            description = "Telhas elegantes para coberturas complexas"
        ),
        CraftingRecipe(
            id = "dark_tiles",
            name = "Ladrilhos Escuros de Ardósia",
            outputItemId = "block_dark_tiles",
            outputAmount = 4,
            ingredients = listOf(CraftingIngredient("block_cobblestone", 4)),
            category = ItemCategory.BLOCKS,
            description = "Piso clássico para pátios e vilas"
        ),
        CraftingRecipe(
            id = "gold_block",
            name = "Bloco de Ouro Maciço",
            outputItemId = "block_gold_block",
            outputAmount = 1,
            ingredients = listOf(CraftingIngredient("gold_ingot", 4)),
            category = ItemCategory.BLOCKS,
            description = "Estrutura pura de ouro para monumentos"
        ),
        CraftingRecipe(
            id = "diamond_block",
            name = "Bloco de Diamante Imperial",
            outputItemId = "block_diamond_block",
            outputAmount = 1,
            ingredients = listOf(CraftingIngredient("diamond", 4)),
            category = ItemCategory.BLOCKS,
            description = "O bloco mais nobre e brilhante do mundo"
        ),
        CraftingRecipe(
            id = "tool_wood_pickaxe",
            name = "Picareta de Madeira",
            outputItemId = "tool_wood_pickaxe",
            outputAmount = 1,
            ingredients = listOf(CraftingIngredient("block_wood_plank", 3), CraftingIngredient("stick", 2)),
            category = ItemCategory.TOOLS,
            description = "Ferramenta básica para quebrar pedra"
        ),
        CraftingRecipe(
            id = "tool_stone_pickaxe",
            name = "Picareta de Pedra",
            outputItemId = "tool_stone_pickaxe",
            outputAmount = 1,
            ingredients = listOf(CraftingIngredient("block_cobblestone", 3), CraftingIngredient("stick", 2)),
            category = ItemCategory.TOOLS,
            description = "Minera com muito mais rapidez"
        ),
        CraftingRecipe(
            id = "tool_iron_pickaxe",
            name = "Picareta de Ferro",
            outputItemId = "tool_iron_pickaxe",
            outputAmount = 1,
            ingredients = listOf(CraftingIngredient("iron_ingot", 3), CraftingIngredient("stick", 2)),
            category = ItemCategory.TOOLS,
            description = "Minera ouro e diamantes"
        ),
        CraftingRecipe(
            id = "tool_diamond_pickaxe",
            name = "Picareta de Diamante",
            outputItemId = "tool_diamond_pickaxe",
            outputAmount = 1,
            ingredients = listOf(CraftingIngredient("diamond", 3), CraftingIngredient("stick", 2)),
            category = ItemCategory.TOOLS,
            description = "Mineração ultra rápida e indestrutível"
        ),
        CraftingRecipe(
            id = "tool_builder_wand",
            name = "Varinha de Macro-Construção",
            outputItemId = "tool_builder_wand",
            outputAmount = 1,
            ingredients = listOf(CraftingIngredient("diamond", 1), CraftingIngredient("energy_crystal", 2), CraftingIngredient("stick", 2)),
            category = ItemCategory.TOOLS,
            description = "Permite construir paredes e plataformas inteiras em 1 clique"
        )
    )
}
