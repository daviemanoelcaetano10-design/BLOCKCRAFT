package com.example.game.core

import kotlin.math.*

data class Vector3f(
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f
) {
    fun set(nx: Float, ny: Float, nz: Float) {
        x = nx
        y = ny
        z = nz
    }

    fun add(dx: Float, dy: Float, dz: Float) {
        x += dx
        y += dy
        z += dz
    }

    fun length(): Float = sqrt(x * x + y * y + z * z)

    fun distanceTo(other: Vector3f): Float {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun copy(): Vector3f = Vector3f(x, y, z)
}

data class Vector3i(
    val x: Int = 0,
    val y: Int = 0,
    val z: Int = 0
) {
    operator fun plus(other: Vector3i): Vector3i = Vector3i(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3i): Vector3i = Vector3i(x - other.x, y - other.y, z - other.z)

    fun distanceTo(other: Vector3i): Float {
        val dx = (x - other.x).toFloat()
        val dy = (y - other.y).toFloat()
        val dz = (z - other.z).toFloat()
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}

enum class BlockFace(val normalX: Int, val normalY: Int, val normalZ: Int) {
    TOP(0, 1, 0),
    BOTTOM(0, -1, 0),
    NORTH(0, 0, -1),
    SOUTH(0, 0, 1),
    WEST(-1, 0, 0),
    EAST(1, 0, 0);

    companion object {
        fun fromNormal(nx: Int, ny: Int, nz: Int): BlockFace {
            return when {
                ny > 0 -> TOP
                ny < 0 -> BOTTOM
                nz < 0 -> NORTH
                nz > 0 -> SOUTH
                nx < 0 -> WEST
                else -> EAST
            }
        }
    }
}

data class RaycastHit(
    val blockPos: Vector3i,
    val face: BlockFace,
    val placePos: Vector3i,
    val hitPoint: Vector3f,
    val distance: Float,
    val blockType: BlockType
)
