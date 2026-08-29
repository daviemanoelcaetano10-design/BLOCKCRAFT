package com.example.game.world

import com.example.game.core.BlockFace
import com.example.game.core.BlockType
import com.example.game.core.RaycastHit
import com.example.game.core.Vector3f
import com.example.game.core.Vector3i
import kotlin.math.*
import kotlin.random.Random

class WorldMap(
    val sizeX: Int = 48,
    val sizeY: Int = 28,
    val sizeZ: Int = 48,
    val seed: Long = 1337L
) {
    // 3D flat array for fast voxel lookup
    val blocks = ByteArray(sizeX * sizeY * sizeZ)

    // Tracks modifications to enable efficient serialization / saving
    val modifiedBlocks = mutableMapOf<Int, Byte>()

    var isMeshDirty = true

    init {
        generateTerrain()
    }

    private fun getIndex(x: Int, y: Int, z: Int): Int {
        return (y * sizeZ + z) * sizeX + x
    }

    fun isValid(x: Int, y: Int, z: Int): Boolean {
        return x in 0 until sizeX && y in 0 until sizeY && z in 0 until sizeZ
    }

    fun getBlock(x: Int, y: Int, z: Int): BlockType {
        if (!isValid(x, y, z)) return BlockType.AIR
        val id = blocks[getIndex(x, y, z)]
        return BlockType.fromId(id)
    }

    fun setBlock(x: Int, y: Int, z: Int, type: BlockType, recordModification: Boolean = true): Boolean {
        if (!isValid(x, y, z)) return false
        val idx = getIndex(x, y, z)
        blocks[idx] = type.id
        if (recordModification) {
            modifiedBlocks[idx] = type.id
        }
        isMeshDirty = true
        return true
    }

    fun getHighestSolidBlockY(x: Int, z: Int): Int {
        for (y in sizeY - 1 downTo 0) {
            val b = getBlock(x, y, z)
            if (b != BlockType.AIR && b != BlockType.WATER && b != BlockType.LEAVES && b != BlockType.FLOWER_ROSE && b != BlockType.FLOWER_DANDELION) {
                return y
            }
        }
        return 5
    }

    fun generateTerrain() {
        val rand = Random(seed)
        val waterLevel = 6

        // Generate heightmap using smooth multi-octave sine/cosine harmonics
        val heightMap = Array(sizeX) { IntArray(sizeZ) }

        for (x in 0 until sizeX) {
            for (z in 0 until sizeZ) {
                val nx = (x + seed % 100).toFloat() * 0.08f
                val nz = (z + (seed / 100) % 100).toFloat() * 0.08f

                val h1 = sin(nx) * cos(nz) * 4f
                val h2 = sin(nx * 2.2f + 1.2f) * cos(nz * 2.2f + 0.8f) * 2f
                val h3 = sin(nx * 0.4f) * sin(nz * 0.4f) * 6f

                val baseHeight = 9f + h1 + h2 + h3
                val clampedH = baseHeight.toInt().coerceIn(3, sizeY - 8)
                heightMap[x][z] = clampedH
            }
        }

        // Fill blocks
        for (x in 0 until sizeX) {
            for (z in 0 until sizeZ) {
                val terrainH = heightMap[x][z]

                // Bedrock / Bottom
                setBlock(x, 0, z, BlockType.OBSIDIAN, recordModification = false)

                for (y in 1..terrainH) {
                    if (y == terrainH) {
                        if (y <= waterLevel) {
                            setBlock(x, y, z, BlockType.SAND, recordModification = false)
                        } else {
                            setBlock(x, y, z, BlockType.GRASS, recordModification = false)
                        }
                    } else if (y >= terrainH - 2) {
                        if (terrainH <= waterLevel) {
                            setBlock(x, y, z, BlockType.SAND, recordModification = false)
                        } else {
                            setBlock(x, y, z, BlockType.DIRT, recordModification = false)
                        }
                    } else {
                        // Underground stone with ore veins
                        val oreRoll = rand.nextFloat()
                        val block = when {
                            y < 5 && oreRoll < 0.04f -> BlockType.DIAMOND_ORE
                            y < 8 && oreRoll < 0.07f -> BlockType.GOLD_ORE
                            y < 12 && oreRoll < 0.12f -> BlockType.REDSTONE_ORE
                            y < 16 && oreRoll < 0.18f -> BlockType.IRON_ORE
                            oreRoll < 0.25f -> BlockType.COAL_ORE
                            else -> BlockType.STONE
                        }
                        setBlock(x, y, z, block, recordModification = false)
                    }
                }

                // Fill water bodies
                if (terrainH < waterLevel) {
                    for (wy in (terrainH + 1)..waterLevel) {
                        setBlock(x, wy, z, BlockType.WATER, recordModification = false)
                    }
                }
            }
        }

        // Generate Trees and Flowers in open grass areas
        for (x in 4 until sizeX - 4 step 3) {
            for (z in 4 until sizeZ - 4 step 3) {
                val terrainH = heightMap[x][z]
                if (terrainH > waterLevel && getBlock(x, terrainH, z) == BlockType.GRASS) {
                    val roll = rand.nextFloat()
                    if (roll < 0.22f) {
                        // Spawn Oak Tree
                        spawnTree(x, terrainH + 1, z, rand)
                    } else if (roll < 0.35f) {
                        // Spawn Flower
                        val flower = if (rand.nextBoolean()) BlockType.FLOWER_ROSE else BlockType.FLOWER_DANDELION
                        setBlock(x, terrainH + 1, z, flower, recordModification = false)
                    }
                }
            }
        }

        isMeshDirty = true
    }

    private fun spawnTree(rootX: Int, rootY: Int, rootZ: Int, rand: Random) {
        val treeHeight = 4 + rand.nextInt(3)
        if (rootY + treeHeight + 2 >= sizeY) return

        // Trunk
        for (y in 0 until treeHeight) {
            setBlock(rootX, rootY + y, rootZ, BlockType.WOOD_OAK, recordModification = false)
        }

        // Foliage Crown
        val leafStartY = rootY + treeHeight - 2
        for (ly in leafStartY..(rootY + treeHeight + 1)) {
            val radius = if (ly >= rootY + treeHeight) 1 else 2
            for (dx in -radius..radius) {
                for (dz in -radius..radius) {
                    if (abs(dx) == radius && abs(dz) == radius && rand.nextBoolean()) continue
                    val tx = rootX + dx
                    val tz = rootZ + dz
                    if (isValid(tx, ly, tz) && getBlock(tx, ly, tz) == BlockType.AIR) {
                        setBlock(tx, ly, tz, BlockType.LEAVES, recordModification = false)
                    }
                }
            }
        }
    }

    // Voxel DDA 3D Raycaster
    fun raycast(
        origin: Vector3f,
        direction: Vector3f,
        maxDistance: Float = 7.0f
    ): RaycastHit? {
        var x = floor(origin.x).toInt()
        var y = floor(origin.y).toInt()
        var z = floor(origin.z).toInt()

        val stepX = if (direction.x >= 0) 1 else -1
        val stepY = if (direction.y >= 0) 1 else -1
        val stepZ = if (direction.z >= 0) 1 else -1

        val tDeltaX = if (direction.x != 0f) abs(1f / direction.x) else Float.MAX_VALUE
        val tDeltaY = if (direction.y != 0f) abs(1f / direction.y) else Float.MAX_VALUE
        val tDeltaZ = if (direction.z != 0f) abs(1f / direction.z) else Float.MAX_VALUE

        var tMaxX = if (direction.x > 0) (x + 1 - origin.x) * tDeltaX else (origin.x - x) * tDeltaX
        var tMaxY = if (direction.y > 0) (y + 1 - origin.y) * tDeltaY else (origin.y - y) * tDeltaY
        var tMaxZ = if (direction.z > 0) (z + 1 - origin.z) * tDeltaZ else (origin.z - z) * tDeltaZ

        var hitFace = BlockFace.TOP
        var distance = 0f

        while (distance <= maxDistance) {
            if (isValid(x, y, z)) {
                val block = getBlock(x, y, z)
                if (block != BlockType.AIR && block != BlockType.WATER) {
                    val hitPos = Vector3i(x, y, z)
                    val placePos = Vector3i(
                        x + hitFace.normalX,
                        y + hitFace.normalY,
                        z + hitFace.normalZ
                    )
                    val hitPoint = Vector3f(
                        origin.x + direction.x * distance,
                        origin.y + direction.y * distance,
                        origin.z + direction.z * distance
                    )
                    return RaycastHit(
                        blockPos = hitPos,
                        face = hitFace,
                        placePos = placePos,
                        hitPoint = hitPoint,
                        distance = distance,
                        blockType = block
                    )
                }
            }

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    distance = tMaxX
                    tMaxX += tDeltaX
                    x += stepX
                    hitFace = if (stepX > 0) BlockFace.WEST else BlockFace.EAST
                } else {
                    distance = tMaxZ
                    tMaxZ += tDeltaZ
                    z += stepZ
                    hitFace = if (stepZ > 0) BlockFace.NORTH else BlockFace.SOUTH
                }
            } else {
                if (tMaxY < tMaxZ) {
                    distance = tMaxY
                    tMaxY += tDeltaY
                    y += stepY
                    hitFace = if (stepY > 0) BlockFace.BOTTOM else BlockFace.TOP
                } else {
                    distance = tMaxZ
                    tMaxZ += tDeltaZ
                    z += stepZ
                    hitFace = if (stepZ > 0) BlockFace.NORTH else BlockFace.SOUTH
                }
            }
        }
        return null
    }

    // Build Complex Structure Blueprint at origin
    fun placeBlueprint(
        blueprint: StructureBlueprint,
        originX: Int,
        originY: Int,
        originZ: Int
    ): Int {
        var count = 0
        blueprint.blocks.forEach { b ->
            val tx = originX + b.relX
            val ty = originY + b.relY
            val tz = originZ + b.relZ
            if (isValid(tx, ty, tz)) {
                setBlock(tx, ty, tz, b.blockType)
                count++
            }
        }
        isMeshDirty = true
        return count
    }

    // Quick builder: Fill 3D Box (Walls / Floor / Pillars)
    fun fillVolume(
        startX: Int, startY: Int, startZ: Int,
        endX: Int, endY: Int, endZ: Int,
        blockType: BlockType
    ): Int {
        val minX = min(startX, endX).coerceIn(0, sizeX - 1)
        val maxX = max(startX, endX).coerceIn(0, sizeX - 1)
        val minY = min(startY, endY).coerceIn(0, sizeY - 1)
        val maxY = max(startY, endY).coerceIn(0, sizeY - 1)
        val minZ = min(startZ, endZ).coerceIn(0, sizeZ - 1)
        val maxZ = max(startZ, endZ).coerceIn(0, sizeZ - 1)

        var count = 0
        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    setBlock(x, y, z, blockType)
                    count++
                }
            }
        }
        isMeshDirty = true
        return count
    }

    fun applySavedModifications(savedMap: Map<Int, Byte>) {
        savedMap.forEach { (idx, blockId) ->
            if (idx in blocks.indices) {
                blocks[idx] = blockId
                modifiedBlocks[idx] = blockId
            }
        }
        isMeshDirty = true
    }
}
