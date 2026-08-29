package com.example.game.world

import com.example.game.core.BlockType
import com.example.game.core.Vector3i
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class BlueprintBlock(
    val relX: Int,
    val relY: Int,
    val relZ: Int,
    val blockType: BlockType
)

data class StructureBlueprint(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val sizeX: Int,
    val sizeY: Int,
    val sizeZ: Int,
    val blocks: List<BlueprintBlock>,
    val requiredMaterialsSummary: Map<BlockType, Int>
)

object StructureBlueprints {
    val ALL_BLUEPRINTS: List<StructureBlueprint> by lazy {
        listOf(
            buildCozyCabin(),
            buildMedievalCastle(),
            buildModernVilla(),
            buildWatchtower(),
            buildArchedBridge(),
            buildAncientPyramid(),
            buildLighthouse(),
            buildGrandFountain()
        )
    }

    fun getBlueprint(id: String): StructureBlueprint? = ALL_BLUEPRINTS.find { it.id == id }

    // 1. Cozy Cabin (Cabana Aconchegante)
    private fun buildCozyCabin(): StructureBlueprint {
        val blocks = mutableListOf<BlueprintBlock>()
        val width = 7
        val length = 7
        val height = 6

        // Foundation & Floor
        for (x in 0 until width) {
            for (z in 0 until length) {
                blocks.add(BlueprintBlock(x, 0, z, BlockType.COBBLESTONE))
                blocks.add(BlueprintBlock(x, 1, z, BlockType.WOOD_PLANK))
            }
        }

        // Walls
        for (y in 2..4) {
            for (x in 0 until width) {
                for (z in 0 until length) {
                    val isCorner = (x == 0 || x == width - 1) && (z == 0 || z == length - 1)
                    val isWall = x == 0 || x == width - 1 || z == 0 || z == length - 1

                    if (isCorner) {
                        blocks.add(BlueprintBlock(x, y, z, BlockType.WOOD_OAK))
                    } else if (isWall) {
                        // Window on sides, door on front
                        if (z == 0 && x == width / 2 && y <= 3) {
                            // Doorway (air)
                        } else if ((x == 0 || x == width - 1 || z == length - 1) && (x == 2 || x == 4 || z == 2 || z == 4) && y == 3) {
                            blocks.add(BlueprintBlock(x, y, z, BlockType.GLASS))
                        } else {
                            blocks.add(BlueprintBlock(x, y, z, BlockType.WOOD_PLANK))
                        }
                    }
                }
            }
        }

        // Slanted Roof
        for (step in 0..3) {
            val y = 4 + step
            val minX = step
            val maxX = width - 1 - step
            for (x in minX..maxX) {
                for (z in 0 until length) {
                    blocks.add(BlueprintBlock(x, y, z, BlockType.BLUE_ROOF))
                }
            }
        }

        // Interior Decor & Chimney
        blocks.add(BlueprintBlock(1, 2, length - 2, BlockType.BOOKSHELF))
        blocks.add(BlueprintBlock(1, 3, length - 2, BlockType.TORCH))
        blocks.add(BlueprintBlock(width - 2, 2, length - 2, BlockType.RED_BRICK)) // Fireplace
        blocks.add(BlueprintBlock(width - 2, 3, length - 2, BlockType.RED_BRICK))
        blocks.add(BlueprintBlock(width - 2, 4, length - 2, BlockType.RED_BRICK))
        blocks.add(BlueprintBlock(width - 2, 5, length - 2, BlockType.RED_BRICK))
        blocks.add(BlueprintBlock(width - 2, 6, length - 2, BlockType.RED_BRICK)) // Chimney top
        blocks.add(BlueprintBlock(width / 2, 3, 1, BlockType.TORCH))

        return createBlueprint(
            id = "cozy_cabin",
            name = "Cabana Alpina do Bosque",
            category = "Residencial",
            description = "Uma charmosa cabana rústica com tábuas de carvalho, telhado azul, lareira de tijolos e estante.",
            sizeX = width, sizeY = 7, sizeZ = length,
            blocks = blocks
        )
    }

    // 2. Medieval Castle (Castelo Medieval)
    private fun buildMedievalCastle(): StructureBlueprint {
        val blocks = mutableListOf<BlueprintBlock>()
        val size = 13
        val wallHeight = 5
        val towerHeight = 8

        // Courtyard floor
        for (x in 0 until size) {
            for (z in 0 until size) {
                blocks.add(BlueprintBlock(x, 0, z, BlockType.STONE_BRICK))
                if (x in 2..(size - 3) && z in 2..(size - 3)) {
                    blocks.add(BlueprintBlock(x, 1, z, BlockType.DARK_TILES))
                }
            }
        }

        // Outer Walls with crenellations
        for (y in 1..wallHeight) {
            for (x in 0 until size) {
                for (z in 0 until size) {
                    val isEdge = x == 0 || x == size - 1 || z == 0 || z == size - 1
                    if (isEdge) {
                        // Gate entrance on front
                        if (z == 0 && (x in 5..7) && y <= 3) {
                            // Open arched gate
                        } else {
                            blocks.add(BlueprintBlock(x, y, z, BlockType.STONE_BRICK))
                        }
                    }
                }
            }
        }

        // Wall Crenellations (top battlements)
        for (x in 0 until size) {
            for (z in 0 until size) {
                val isEdge = x == 0 || x == size - 1 || z == 0 || z == size - 1
                if (isEdge && (x % 2 == 0 || z % 2 == 0)) {
                    blocks.add(BlueprintBlock(x, wallHeight + 1, z, BlockType.STONE_BRICK))
                }
            }
        }

        // 4 Corner Towers (3x3 each, height 8)
        val cornerOffsets = listOf(
            Pair(0, 0),
            Pair(size - 3, 0),
            Pair(0, size - 3),
            Pair(size - 3, size - 3)
        )

        for ((cx, cz) in cornerOffsets) {
            for (y in 1..towerHeight) {
                for (dx in 0..2) {
                    for (dz in 0..2) {
                        val isTowerWall = dx == 0 || dx == 2 || dz == 0 || dz == 2
                        if (isTowerWall) {
                            blocks.add(BlueprintBlock(cx + dx, y, cz + dz, BlockType.STONE_BRICK))
                        } else if (y == towerHeight - 1) {
                            blocks.add(BlueprintBlock(cx + dx, y, cz + dz, BlockType.WOOD_PLANK))
                        }
                    }
                }
            }
            // Tower roof battlements & torch
            for (dx in 0..2) {
                for (dz in 0..2) {
                    if ((dx == 0 && dz == 0) || (dx == 2 && dz == 0) || (dx == 0 && dz == 2) || (dx == 2 && dz == 2)) {
                        blocks.add(BlueprintBlock(cx + dx, towerHeight + 1, cz + dz, BlockType.STONE_BRICK))
                    }
                }
            }
            blocks.add(BlueprintBlock(cx + 1, towerHeight, cz + 1, BlockType.TORCH))
        }

        // Central Keep / Throne inside
        val midX = size / 2
        val midZ = size / 2
        blocks.add(BlueprintBlock(midX, 1, midZ, BlockType.GOLD_BLOCK))
        blocks.add(BlueprintBlock(midX, 2, midZ, BlockType.DIAMOND_BLOCK))
        blocks.add(BlueprintBlock(midX - 1, 2, midZ, BlockType.TORCH))
        blocks.add(BlueprintBlock(midX + 1, 2, midZ, BlockType.TORCH))

        return createBlueprint(
            id = "medieval_castle",
            name = "Castelo Imperial Fortificado",
            category = "Fortalezas",
            description = "Uma imponente fortaleza de tijolos de pedra com 4 torres de vigia, ameias militares, portão arqueado e trono central.",
            sizeX = size, sizeY = towerHeight + 2, sizeZ = size,
            blocks = blocks
        )
    }

    // 3. Modern Villa (Mansão Moderna)
    private fun buildModernVilla(): StructureBlueprint {
        val blocks = mutableListOf<BlueprintBlock>()
        val width = 11
        val length = 9
        val height = 7

        // Platform base & swimming pool
        for (x in 0 until width) {
            for (z in 0 until length) {
                blocks.add(BlueprintBlock(x, 0, z, BlockType.WHITE_CONCRETE))
                // Swimming pool on front-right
                if (x in 6..9 && z in 1..4) {
                    blocks.add(BlueprintBlock(x, 0, z, BlockType.WATER))
                } else {
                    blocks.add(BlueprintBlock(x, 1, z, BlockType.DARK_TILES))
                }
            }
        }

        // First Floor (White concrete frame & glass walls)
        for (y in 2..4) {
            for (x in 0..6) {
                for (z in 2 until length) {
                    val isBorder = x == 0 || x == 6 || z == 2 || z == length - 1
                    if (isBorder) {
                        if ((x in 1..5 && z == 2) || (z in 3..7 && x == 6)) {
                            blocks.add(BlueprintBlock(x, y, z, BlockType.GLASS))
                        } else {
                            blocks.add(BlueprintBlock(x, y, z, BlockType.WHITE_CONCRETE))
                        }
                    }
                }
            }
        }

        // Second Floor Balcony & Overhanging Master Suite
        for (x in 0..8) {
            for (z in 3 until length) {
                blocks.add(BlueprintBlock(x, 4, z, BlockType.WOOD_PLANK))
            }
        }

        for (y in 5..6) {
            for (x in 1..8) {
                for (z in 4 until length) {
                    val isBorder = x == 1 || x == 8 || z == 4 || z == length - 1
                    if (isBorder) {
                        if (z == 4 && x in 3..6) {
                            blocks.add(BlueprintBlock(x, y, z, BlockType.GLASS))
                        } else {
                            blocks.add(BlueprintBlock(x, y, z, BlockType.WHITE_CONCRETE))
                        }
                    }
                }
            }
        }

        // Modern Flat Roof & Lighting
        for (x in 0..9) {
            for (z in 3 until length) {
                blocks.add(BlueprintBlock(x, 7, z, BlockType.WHITE_CONCRETE))
            }
        }
        blocks.add(BlueprintBlock(4, 3, 5, BlockType.NEON_LAMP))
        blocks.add(BlueprintBlock(4, 6, 6, BlockType.NEON_LAMP))
        blocks.add(BlueprintBlock(8, 2, 2, BlockType.NEON_LAMP))

        return createBlueprint(
            id = "modern_villa",
            name = "Mansão Contemporânea com Piscina",
            category = "Residencial",
            description = "Arquitetura arrojada em concreto branco, amplos painéis de vidro panorâmicos, deck de madeira e piscina integrada.",
            sizeX = width, sizeY = 8, sizeZ = length,
            blocks = blocks
        )
    }

    // 4. Watchtower (Torre de Vigia)
    private fun buildWatchtower(): StructureBlueprint {
        val blocks = mutableListOf<BlueprintBlock>()
        val towerSize = 5
        val towerHeight = 12

        for (y in 0 until towerHeight) {
            for (x in 0 until towerSize) {
                for (z in 0 until towerSize) {
                    val isBorder = x == 0 || x == towerSize - 1 || z == 0 || z == towerSize - 1
                    if (y == 0) {
                        blocks.add(BlueprintBlock(x, y, z, BlockType.COBBLESTONE))
                    } else if (y < towerHeight - 3) {
                        if (isBorder) {
                            if ((x == 0 && z == 0) || (x == towerSize - 1 && z == 0) || (x == 0 && z == towerSize - 1) || (x == towerSize - 1 && z == towerSize - 1)) {
                                blocks.add(BlueprintBlock(x, y, z, BlockType.WOOD_OAK))
                            } else if (y % 3 == 0) {
                                blocks.add(BlueprintBlock(x, y, z, BlockType.GLASS))
                            } else {
                                blocks.add(BlueprintBlock(x, y, z, BlockType.STONE_BRICK))
                            }
                        } else if (y % 3 == 0) {
                            // Internal platform
                            blocks.add(BlueprintBlock(x, y, z, BlockType.WOOD_PLANK))
                        }
                    } else if (y == towerHeight - 3) {
                        // Expanded observation deck floor
                        blocks.add(BlueprintBlock(x, y, z, BlockType.WOOD_PLANK))
                    }
                }
            }
        }

        // Expanded Lookout platform (7x7) at top
        val deckY = towerHeight - 3
        for (x in -1..towerSize) {
            for (z in -1..towerSize) {
                blocks.add(BlueprintBlock(x + 1, deckY, z + 1, BlockType.WOOD_PLANK))
                val isGuardRail = x == -1 || x == towerSize || z == -1 || z == towerSize
                if (isGuardRail) {
                    blocks.add(BlueprintBlock(x + 1, deckY + 1, z + 1, BlockType.WOOD_OAK))
                }
            }
        }

        // Canopy Roof & Torches
        for (x in -1..towerSize) {
            for (z in -1..towerSize) {
                blocks.add(BlueprintBlock(x + 1, deckY + 4, z + 1, BlockType.BLUE_ROOF))
            }
        }
        blocks.add(BlueprintBlock(towerSize / 2 + 1, deckY + 2, towerSize / 2 + 1, BlockType.NEON_LAMP))
        blocks.add(BlueprintBlock(0, deckY + 2, 0, BlockType.TORCH))
        blocks.add(BlueprintBlock(towerSize + 1, deckY + 2, 0, BlockType.TORCH))
        blocks.add(BlueprintBlock(0, deckY + 2, towerSize + 1, BlockType.TORCH))
        blocks.add(BlueprintBlock(towerSize + 1, deckY + 2, towerSize + 1, BlockType.TORCH))

        return createBlueprint(
            id = "watchtower",
            name = "Torre de Vigia Celestial",
            category = "Defensiva",
            description = "Uma alta torre medieval com mirante panorâmico no topo, iluminação e suporte de troncos maciços.",
            sizeX = towerSize + 2, sizeY = towerHeight + 2, sizeZ = towerSize + 2,
            blocks = blocks
        )
    }

    // 5. Grand Arched Bridge (Grande Ponte em Arco)
    private fun buildArchedBridge(): StructureBlueprint {
        val blocks = mutableListOf<BlueprintBlock>()
        val length = 15
        val width = 5
        val maxHeight = 5

        for (x in 0 until length) {
            val progress = (x - length / 2f) / (length / 2f)
            val archHeight = ((1f - progress * progress) * 3f).toInt()
            val roadY = 4

            for (z in 0 until width) {
                // Pillars and arch
                for (y in archHeight..roadY) {
                    if (z == 0 || z == width - 1 || y == roadY) {
                        blocks.add(BlueprintBlock(x, y, z, BlockType.STONE_BRICK))
                    }
                }
                // Road floor
                blocks.add(BlueprintBlock(x, roadY, z, BlockType.DARK_TILES))

                // Balustrades
                if (z == 0 || z == width - 1) {
                    blocks.add(BlueprintBlock(x, roadY + 1, z, BlockType.COBBLESTONE))
                    if (x % 3 == 0) {
                        blocks.add(BlueprintBlock(x, roadY + 2, z, BlockType.TORCH))
                    }
                }
            }
        }

        return createBlueprint(
            id = "arched_bridge",
            name = "Ponte Monumental em Arco",
            category = "Infraestrutura",
            description = "Ponte clássica com sustentação em arco de pedra, piso pavimentado e tochas decorativas nas balaustradas.",
            sizeX = length, sizeY = maxHeight + 3, sizeZ = width,
            blocks = blocks
        )
    }

    // 6. Ancient Golden Pyramid (Pirâmide Ancestral)
    private fun buildAncientPyramid(): StructureBlueprint {
        val blocks = mutableListOf<BlueprintBlock>()
        val baseSize = 13
        val steps = baseSize / 2 + 1

        for (step in 0 until steps) {
            val y = step
            val minCoord = step
            val maxCoord = baseSize - 1 - step

            for (x in minCoord..maxCoord) {
                for (z in minCoord..maxCoord) {
                    val isOuter = x == minCoord || x == maxCoord || z == minCoord || z == maxCoord
                    if (isOuter || y == 0) {
                        blocks.add(BlueprintBlock(x, y, z, BlockType.SAND))
                    } else if (y == steps - 1) {
                        blocks.add(BlueprintBlock(x, y, z, BlockType.GOLD_BLOCK))
                    } else if (y in 1..2 && x in (baseSize / 2 - 1)..(baseSize / 2 + 1) && z in (baseSize / 2 - 1)..(baseSize / 2 + 1)) {
                        // Inner treasure chamber
                        if (x == baseSize / 2 && z == baseSize / 2 && y == 1) {
                            blocks.add(BlueprintBlock(x, y, z, BlockType.DIAMOND_BLOCK))
                        } else {
                            blocks.add(BlueprintBlock(x, y, z, BlockType.TORCH))
                        }
                    } else {
                        blocks.add(BlueprintBlock(x, y, z, BlockType.STONE_BRICK))
                    }
                }
            }
        }

        // Capstone apex
        blocks.add(BlueprintBlock(baseSize / 2, steps, baseSize / 2, BlockType.GOLD_BLOCK))
        blocks.add(BlueprintBlock(baseSize / 2, steps + 1, baseSize / 2, BlockType.REDSTONE_ORE))

        return createBlueprint(
            id = "ancient_pyramid",
            name = "Pirâmide Mística de Ouro",
            category = "Monumentos",
            description = "Uma colossal pirâmide em degraus com câmara secreta de tesouros em diamante e topo sagrado dourado.",
            sizeX = baseSize, sizeY = steps + 2, sizeZ = baseSize,
            blocks = blocks
        )
    }

    // 7. Coastal Lighthouse (Farol Marítimo)
    private fun buildLighthouse(): StructureBlueprint {
        val blocks = mutableListOf<BlueprintBlock>()
        val radius = 3
        val height = 14
        val centerX = 4
        val centerZ = 4

        // Base foundation
        for (x in 0..8) {
            for (z in 0..8) {
                val dist = sqrt(((x - centerX) * (x - centerX) + (z - centerZ) * (z - centerZ)).toDouble())
                if (dist <= 4.0) {
                    blocks.add(BlueprintBlock(x, 0, z, BlockType.COBBLESTONE))
                }
            }
        }

        // Cylinder Tower with alternating Red and White Stripes
        for (y in 1 until height - 2) {
            val stripeColor = if ((y / 2) % 2 == 0) BlockType.RED_BRICK else BlockType.WHITE_CONCRETE
            for (x in 0..8) {
                for (z in 0..8) {
                    val dist = sqrt(((x - centerX) * (x - centerX) + (z - centerZ) * (z - centerZ)).toDouble())
                    if (dist in 2.0..3.2) {
                        blocks.add(BlueprintBlock(x, y, z, stripeColor))
                    } else if (y % 4 == 0 && dist < 2.0) {
                        blocks.add(BlueprintBlock(x, y, z, BlockType.WOOD_PLANK))
                    }
                }
            }
        }

        // Lantern Room (Glass & High Intensity Glow)
        val lanternY = height - 2
        for (x in 0..8) {
            for (z in 0..8) {
                val dist = sqrt(((x - centerX) * (x - centerX) + (z - centerZ) * (z - centerZ)).toDouble())
                if (dist in 2.0..3.2) {
                    blocks.add(BlueprintBlock(x, lanternY, z, BlockType.GLASS))
                    blocks.add(BlueprintBlock(x, lanternY + 1, z, BlockType.GLASS))
                }
            }
        }
        blocks.add(BlueprintBlock(centerX, lanternY, centerZ, BlockType.NEON_LAMP))
        blocks.add(BlueprintBlock(centerX, lanternY + 1, centerZ, BlockType.GOLD_BLOCK))

        // Dome Roof
        for (x in 0..8) {
            for (z in 0..8) {
                val dist = sqrt(((x - centerX) * (x - centerX) + (z - centerZ) * (z - centerZ)).toDouble())
                if (dist <= 3.0) {
                    blocks.add(BlueprintBlock(x, height, z, BlockType.DARK_TILES))
                }
            }
        }

        return createBlueprint(
            id = "coastal_lighthouse",
            name = "Farol Marítimo Luminoso",
            category = "Infraestrutura",
            description = "Farol costeiro listrado em vermelho e branco, com câmara de luz rotativa e cúpula protetora.",
            sizeX = 9, sizeY = height + 2, sizeZ = 9,
            blocks = blocks
        )
    }

    // 8. Grand Fountain (Fonte Imperial)
    private fun buildGrandFountain(): StructureBlueprint {
        val blocks = mutableListOf<BlueprintBlock>()
        val size = 9
        val center = size / 2

        // Outer pool rim & basin
        for (x in 0 until size) {
            for (z in 0 until size) {
                val dist = sqrt(((x - center) * (x - center) + (z - center) * (z - center)).toDouble())
                if (dist <= 4.2) {
                    blocks.add(BlueprintBlock(x, 0, z, BlockType.STONE_BRICK))
                    if (dist > 3.2) {
                        blocks.add(BlueprintBlock(x, 1, z, BlockType.STONE_BRICK))
                    } else {
                        blocks.add(BlueprintBlock(x, 1, z, BlockType.WATER))
                    }
                }
            }
        }

        // Tiered Central Pillar with cascading water
        blocks.add(BlueprintBlock(center, 2, center, BlockType.WHITE_CONCRETE))
        blocks.add(BlueprintBlock(center, 3, center, BlockType.WHITE_CONCRETE))
        blocks.add(BlueprintBlock(center, 4, center, BlockType.WHITE_CONCRETE))
        blocks.add(BlueprintBlock(center, 5, center, BlockType.GOLD_BLOCK))

        // Tier 2 bowl
        for (dx in -1..1) {
            for (dz in -1..1) {
                if (dx != 0 || dz != 0) {
                    blocks.add(BlueprintBlock(center + dx, 3, center + dz, BlockType.WHITE_CONCRETE))
                    blocks.add(BlueprintBlock(center + dx, 4, center + dz, BlockType.WATER))
                }
            }
        }

        // Surrounding flower pots
        blocks.add(BlueprintBlock(center - 3, 1, center - 3, BlockType.FLOWER_ROSE))
        blocks.add(BlueprintBlock(center + 3, 1, center - 3, BlockType.FLOWER_DANDELION))
        blocks.add(BlueprintBlock(center - 3, 1, center + 3, BlockType.FLOWER_DANDELION))
        blocks.add(BlueprintBlock(center + 3, 1, center + 3, BlockType.FLOWER_ROSE))

        return createBlueprint(
            id = "grand_fountain",
            name = "Fonte Imperial dos Jardins",
            category = "Monumentos",
            description = "Uma magnífica fonte de água em cascata com múltiplos níveis de mármore e ornamentação floral.",
            sizeX = size, sizeY = 6, sizeZ = size,
            blocks = blocks
        )
    }

    private fun createBlueprint(
        id: String,
        name: String,
        category: String,
        description: String,
        sizeX: Int,
        sizeY: Int,
        sizeZ: Int,
        blocks: List<BlueprintBlock>
    ): StructureBlueprint {
        val summary = mutableMapOf<BlockType, Int>()
        blocks.forEach { b ->
            if (b.blockType != BlockType.AIR) {
                summary[b.blockType] = (summary[b.blockType] ?: 0) + 1
            }
        }
        return StructureBlueprint(
            id = id,
            name = name,
            category = category,
            description = description,
            sizeX = sizeX,
            sizeY = sizeY,
            sizeZ = sizeZ,
            blocks = blocks,
            requiredMaterialsSummary = summary
        )
    }
}
