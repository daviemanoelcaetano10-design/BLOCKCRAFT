package com.example.game.engine

import com.example.game.core.BlockFace
import com.example.game.core.BlockType
import com.example.game.world.WorldMap
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class VoxelMesh {
    var vertexBuffer: FloatBuffer? = null
    var indexBuffer: ShortBuffer? = null
    var indexCount: Int = 0

    // Water / Translucent mesh
    var transVertexBuffer: FloatBuffer? = null
    var transIndexBuffer: ShortBuffer? = null
    var transIndexCount: Int = 0

    fun isEmpty(): Boolean = indexCount == 0 && transIndexCount == 0
}

object MeshBuilder {
    private const val FLOATS_PER_VERTEX = 9 // x, y, z, nx, ny, nz, u, v, ao

    // Temporary list structures to avoid heavy allocations
    private val solidVertices = FloatArrayList(65536)
    private val solidIndices = ShortArrayList(65536)
    private val transVertices = FloatArrayList(16384)
    private val transIndices = ShortArrayList(16384)

    fun buildWorldMesh(world: WorldMap, outMesh: VoxelMesh) {
        solidVertices.clear()
        solidIndices.clear()
        transVertices.clear()
        transIndices.clear()

        var solidVertexCount = 0
        var transVertexCount = 0

        for (x in 0 until world.sizeX) {
            for (y in 0 until world.sizeY) {
                for (z in 0 until world.sizeZ) {
                    val block = world.getBlock(x, y, z)
                    if (block == BlockType.AIR) continue

                    val isTranslucent = block.isTransparent

                    // Check all 6 faces
                    // TOP (+Y)
                    val topBlock = world.getBlock(x, y + 1, z)
                    if (shouldRenderFace(block, topBlock)) {
                        val uvs = TextureAtlas.getUVsForTile(block.topTextureIndex)
                        val ao = computeAO(world, x, y, z, BlockFace.TOP)
                        if (isTranslucent) {
                            addFace(transVertices, transIndices, transVertexCount, x, y, z, BlockFace.TOP, uvs, ao)
                            transVertexCount += 4
                        } else {
                            addFace(solidVertices, solidIndices, solidVertexCount, x, y, z, BlockFace.TOP, uvs, ao)
                            solidVertexCount += 4
                        }
                    }

                    // BOTTOM (-Y)
                    val botBlock = world.getBlock(x, y - 1, z)
                    if (shouldRenderFace(block, botBlock)) {
                        val uvs = TextureAtlas.getUVsForTile(block.bottomTextureIndex)
                        val ao = computeAO(world, x, y, z, BlockFace.BOTTOM)
                        if (isTranslucent) {
                            addFace(transVertices, transIndices, transVertexCount, x, y, z, BlockFace.BOTTOM, uvs, ao)
                            transVertexCount += 4
                        } else {
                            addFace(solidVertices, solidIndices, solidVertexCount, x, y, z, BlockFace.BOTTOM, uvs, ao)
                            solidVertexCount += 4
                        }
                    }

                    // NORTH (-Z)
                    val northBlock = world.getBlock(x, y, z - 1)
                    if (shouldRenderFace(block, northBlock)) {
                        val uvs = TextureAtlas.getUVsForTile(block.sideTextureIndex)
                        val ao = computeAO(world, x, y, z, BlockFace.NORTH)
                        if (isTranslucent) {
                            addFace(transVertices, transIndices, transVertexCount, x, y, z, BlockFace.NORTH, uvs, ao)
                            transVertexCount += 4
                        } else {
                            addFace(solidVertices, solidIndices, solidVertexCount, x, y, z, BlockFace.NORTH, uvs, ao)
                            solidVertexCount += 4
                        }
                    }

                    // SOUTH (+Z)
                    val southBlock = world.getBlock(x, y, z + 1)
                    if (shouldRenderFace(block, southBlock)) {
                        val uvs = TextureAtlas.getUVsForTile(block.sideTextureIndex)
                        val ao = computeAO(world, x, y, z, BlockFace.SOUTH)
                        if (isTranslucent) {
                            addFace(transVertices, transIndices, transVertexCount, x, y, z, BlockFace.SOUTH, uvs, ao)
                            transVertexCount += 4
                        } else {
                            addFace(solidVertices, solidIndices, solidVertexCount, x, y, z, BlockFace.SOUTH, uvs, ao)
                            solidVertexCount += 4
                        }
                    }

                    // WEST (-X)
                    val westBlock = world.getBlock(x - 1, y, z)
                    if (shouldRenderFace(block, westBlock)) {
                        val uvs = TextureAtlas.getUVsForTile(block.sideTextureIndex)
                        val ao = computeAO(world, x, y, z, BlockFace.WEST)
                        if (isTranslucent) {
                            addFace(transVertices, transIndices, transVertexCount, x, y, z, BlockFace.WEST, uvs, ao)
                            transVertexCount += 4
                        } else {
                            addFace(solidVertices, solidIndices, solidVertexCount, x, y, z, BlockFace.WEST, uvs, ao)
                            solidVertexCount += 4
                        }
                    }

                    // EAST (+X)
                    val eastBlock = world.getBlock(x + 1, y, z)
                    if (shouldRenderFace(block, eastBlock)) {
                        val uvs = TextureAtlas.getUVsForTile(block.sideTextureIndex)
                        val ao = computeAO(world, x, y, z, BlockFace.EAST)
                        if (isTranslucent) {
                            addFace(transVertices, transIndices, transVertexCount, x, y, z, BlockFace.EAST, uvs, ao)
                            transVertexCount += 4
                        } else {
                            addFace(solidVertices, solidIndices, solidVertexCount, x, y, z, BlockFace.EAST, uvs, ao)
                            solidVertexCount += 4
                        }
                    }
                }
            }
        }

        // Convert to direct NIO ByteBuffers
        outMesh.vertexBuffer = solidVertices.toFloatBuffer()
        outMesh.indexBuffer = solidIndices.toShortBuffer()
        outMesh.indexCount = solidIndices.size

        outMesh.transVertexBuffer = transVertices.toFloatBuffer()
        outMesh.transIndexBuffer = transIndices.toShortBuffer()
        outMesh.transIndexCount = transIndices.size
    }

    private fun shouldRenderFace(current: BlockType, neighbor: BlockType): Boolean {
        if (neighbor == BlockType.AIR) return true
        if (current == BlockType.WATER && neighbor == BlockType.WATER) return false
        if (neighbor.isTransparent && current != neighbor) return true
        return false
    }

    private fun computeAO(world: WorldMap, x: Int, y: Int, z: Int, face: BlockFace): Float {
        // Quick ambient occlusion based on neighbor corner occlusion
        var occluders = 0
        when (face) {
            BlockFace.TOP -> {
                if (world.getBlock(x + 1, y + 1, z) != BlockType.AIR) occluders++
                if (world.getBlock(x - 1, y + 1, z) != BlockType.AIR) occluders++
                if (world.getBlock(x, y + 1, z + 1) != BlockType.AIR) occluders++
                if (world.getBlock(x, y + 1, z - 1) != BlockType.AIR) occluders++
            }
            else -> {
                if (world.getBlock(x, y + 1, z) != BlockType.AIR) occluders++
            }
        }
        return (1.0f - occluders * 0.08f).coerceIn(0.65f, 1.0f)
    }

    private fun addFace(
        verts: FloatArrayList,
        indices: ShortArrayList,
        vOffset: Int,
        x: Int, y: Int, z: Int,
        face: BlockFace,
        uvs: FloatArray,
        ao: Float
    ) {
        val fx = x.toFloat()
        val fy = y.toFloat()
        val fz = z.toFloat()

        when (face) {
            BlockFace.TOP -> {
                // Normal 0, 1, 0
                addVertex(verts, fx, fy + 1f, fz, 0f, 1f, 0f, uvs[0], uvs[1], ao)
                addVertex(verts, fx, fy + 1f, fz + 1f, 0f, 1f, 0f, uvs[2], uvs[3], ao)
                addVertex(verts, fx + 1f, fy + 1f, fz + 1f, 0f, 1f, 0f, uvs[4], uvs[5], ao)
                addVertex(verts, fx + 1f, fy + 1f, fz, 0f, 1f, 0f, uvs[6], uvs[7], ao)
            }
            BlockFace.BOTTOM -> {
                // Normal 0, -1, 0
                addVertex(verts, fx, fy, fz + 1f, 0f, -1f, 0f, uvs[0], uvs[1], ao)
                addVertex(verts, fx, fy, fz, 0f, -1f, 0f, uvs[2], uvs[3], ao)
                addVertex(verts, fx + 1f, fy, fz, 0f, -1f, 0f, uvs[4], uvs[5], ao)
                addVertex(verts, fx + 1f, fy, fz + 1f, 0f, -1f, 0f, uvs[6], uvs[7], ao)
            }
            BlockFace.NORTH -> {
                // Normal 0, 0, -1
                addVertex(verts, fx + 1f, fy + 1f, fz, 0f, 0f, -1f, uvs[0], uvs[1], ao)
                addVertex(verts, fx + 1f, fy, fz, 0f, 0f, -1f, uvs[2], uvs[3], ao)
                addVertex(verts, fx, fy, fz, 0f, 0f, -1f, uvs[4], uvs[5], ao)
                addVertex(verts, fx, fy + 1f, fz, 0f, 0f, -1f, uvs[6], uvs[7], ao)
            }
            BlockFace.SOUTH -> {
                // Normal 0, 0, 1
                addVertex(verts, fx, fy + 1f, fz + 1f, 0f, 0f, 1f, uvs[0], uvs[1], ao)
                addVertex(verts, fx, fy, fz + 1f, 0f, 0f, 1f, uvs[2], uvs[3], ao)
                addVertex(verts, fx + 1f, fy, fz + 1f, 0f, 0f, 1f, uvs[4], uvs[5], ao)
                addVertex(verts, fx + 1f, fy + 1f, fz + 1f, 0f, 0f, 1f, uvs[6], uvs[7], ao)
            }
            BlockFace.WEST -> {
                // Normal -1, 0, 0
                addVertex(verts, fx, fy + 1f, fz, -1f, 0f, 0f, uvs[0], uvs[1], ao)
                addVertex(verts, fx, fy, fz, -1f, 0f, 0f, uvs[2], uvs[3], ao)
                addVertex(verts, fx, fy, fz + 1f, -1f, 0f, 0f, uvs[4], uvs[5], ao)
                addVertex(verts, fx, fy + 1f, fz + 1f, -1f, 0f, 0f, uvs[6], uvs[7], ao)
            }
            BlockFace.EAST -> {
                // Normal 1, 0, 0
                addVertex(verts, fx + 1f, fy + 1f, fz + 1f, 1f, 0f, 0f, uvs[0], uvs[1], ao)
                addVertex(verts, fx + 1f, fy, fz + 1f, 1f, 0f, 0f, uvs[2], uvs[3], ao)
                addVertex(verts, fx + 1f, fy, fz, 1f, 0f, 0f, uvs[4], uvs[5], ao)
                addVertex(verts, fx + 1f, fy + 1f, fz, 1f, 0f, 0f, uvs[6], uvs[7], ao)
            }
        }

        // Two triangles for quad face (0, 1, 2) and (0, 2, 3)
        val v = vOffset.toShort()
        indices.add(v)
        indices.add((v + 1).toShort())
        indices.add((v + 2).toShort())

        indices.add(v)
        indices.add((v + 2).toShort())
        indices.add((v + 3).toShort())
    }

    private inline fun addVertex(
        verts: FloatArrayList,
        x: Float, y: Float, z: Float,
        nx: Float, ny: Float, nz: Float,
        u: Float, v: Float,
        ao: Float
    ) {
        verts.add(x)
        verts.add(y)
        verts.add(z)
        verts.add(nx)
        verts.add(ny)
        verts.add(nz)
        verts.add(u)
        verts.add(v)
        verts.add(ao)
    }
}

// Ultra-fast primitive array wrappers for zero GC overhead during mesh building
class FloatArrayList(initialCapacity: Int = 1024) {
    var data = FloatArray(initialCapacity)
    var size = 0
        private set

    fun clear() { size = 0 }

    fun add(value: Float) {
        if (size >= data.size) {
            data = data.copyOf(data.size * 2)
        }
        data[size++] = value
    }

    fun toFloatBuffer(): FloatBuffer {
        val bb = ByteBuffer.allocateDirect(size * 4)
        bb.order(ByteOrder.nativeOrder())
        val fb = bb.asFloatBuffer()
        fb.put(data, 0, size)
        fb.position(0)
        return fb
    }
}

class ShortArrayList(initialCapacity: Int = 1024) {
    var data = ShortArray(initialCapacity)
    var size = 0
        private set

    fun clear() { size = 0 }

    fun add(value: Short) {
        if (size >= data.size) {
            data = data.copyOf(data.size * 2)
        }
        data[size++] = value
    }

    fun toShortBuffer(): ShortBuffer {
        val bb = ByteBuffer.allocateDirect(size * 2)
        bb.order(ByteOrder.nativeOrder())
        val sb = bb.asShortBuffer()
        sb.put(data, 0, size)
        sb.position(0)
        return sb
    }
}
