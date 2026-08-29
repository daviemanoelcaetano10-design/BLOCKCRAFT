package com.example.game.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.opengl.GLES20
import android.opengl.GLUtils
import com.example.game.core.BlockType

object TextureAtlas {
    const val TILE_SIZE = 16
    const val ATLAS_TILES_PER_ROW = 8
    const val ATLAS_SIZE = TILE_SIZE * ATLAS_TILES_PER_ROW // 128x128 or 256x256

    var textureId: Int = 0
        private set

    fun initTexture(): Int {
        val bitmap = generateAtlasBitmap()
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.recycle()
        return textureId
    }

    fun getUVsForTile(tileIndex: Int): FloatArray {
        val col = tileIndex % ATLAS_TILES_PER_ROW
        val row = tileIndex / ATLAS_TILES_PER_ROW

        val uMin = col.toFloat() / ATLAS_TILES_PER_ROW.toFloat()
        val uMax = (col + 1).toFloat() / ATLAS_TILES_PER_ROW.toFloat()
        val vMin = row.toFloat() / ATLAS_TILES_PER_ROW.toFloat()
        val vMax = (row + 1).toFloat() / ATLAS_TILES_PER_ROW.toFloat()

        return floatArrayOf(
            uMin, vMin, // Top-Left
            uMin, vMax, // Bottom-Left
            uMax, vMax, // Bottom-Right
            uMax, vMin  // Top-Right
        )
    }

    private fun generateAtlasBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(ATLAS_SIZE, ATLAS_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Draw each texture tile
        // Tile 0: Grass Top (Vibrant green with slight speckles)
        drawTile(canvas, 0, 0) { c, ox, oy ->
            fillBase(c, ox, oy, 0xFF4CAF50.toInt())
            drawSpeckles(c, ox, oy, 0xFF388E3C.toInt(), 0xFF66BB6A.toInt())
        }

        // Tile 1: Grass Side (Dirt with green top 4px)
        drawTile(canvas, 1, 0) { c, ox, oy ->
            fillBase(c, ox, oy, 0xFF795548.toInt())
            drawSpeckles(c, ox, oy, 0xFF5D4037.toInt(), 0xFF8D6E63.toInt())
            // Top green band with ragged fringe
            paint.color = 0xFF4CAF50.toInt()
            c.drawRect(ox.toFloat(), oy.toFloat(), (ox + 16).toFloat(), (oy + 4).toFloat(), paint)
            c.drawRect((ox + 2).toFloat(), (oy + 4).toFloat(), (ox + 5).toFloat(), (oy + 6).toFloat(), paint)
            c.drawRect((ox + 8).toFloat(), (oy + 4).toFloat(), (ox + 11).toFloat(), (oy + 7).toFloat(), paint)
            c.drawRect((ox + 13).toFloat(), (oy + 4).toFloat(), (ox + 15).toFloat(), (oy + 5).toFloat(), paint)
        }

        // Tile 2: Dirt (Rich earthy brown)
        drawTile(canvas, 2, 0) { c, ox, oy ->
            fillBase(c, ox, oy, 0xFF795548.toInt())
            drawSpeckles(c, ox, oy, 0xFF5D4037.toInt(), 0xFF8D6E63.toInt())
        }

        // Tile 3: Stone (Smooth natural rock)
        drawTile(canvas, 3, 0) { c, ox, oy ->
            fillBase(c, ox, oy, 0xFF9E9E9E.toInt())
            drawSpeckles(c, ox, oy, 0xFF757575.toInt(), 0xFFBDBDBD.toInt())
        }

        // Tile 4: Cobblestone (Cracked stone bricks with mortar)
        drawTile(canvas, 4, 0) { c, ox, oy ->
            fillBase(c, ox, oy, 0xFF757575.toInt())
            paint.color = 0xFF424242.toInt()
            c.drawLine(ox.toFloat(), (oy + 5).toFloat(), (ox + 16).toFloat(), (oy + 5).toFloat(), paint)
            c.drawLine(ox.toFloat(), (oy + 10).toFloat(), (ox + 16).toFloat(), (oy + 10).toFloat(), paint)
            c.drawLine((ox + 8).toFloat(), oy.toFloat(), (ox + 8).toFloat(), (oy + 5).toFloat(), paint)
            c.drawLine((ox + 4).toFloat(), (oy + 5).toFloat(), (ox + 4).toFloat(), (oy + 10).toFloat(), paint)
            c.drawLine((ox + 12).toFloat(), (oy + 5).toFloat(), (ox + 12).toFloat(), (oy + 10).toFloat(), paint)
            c.drawLine((ox + 8).toFloat(), (oy + 10).toFloat(), (ox + 8).toFloat(), (oy + 16).toFloat(), paint)
        }

        // Tile 5: Wood Oak Top (Tree rings)
        drawTile(canvas, 5, 0) { c, ox, oy ->
            fillBase(c, ox, oy, 0xFF8D6E63.toInt())
            paint.color = 0xFF5D4037.toInt()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            c.drawRect((ox + 2).toFloat(), (oy + 2).toFloat(), (ox + 14).toFloat(), (oy + 14).toFloat(), paint)
            c.drawRect((ox + 5).toFloat(), (oy + 5).toFloat(), (ox + 11).toFloat(), (oy + 11).toFloat(), paint)
            paint.style = Paint.Style.FILL
            c.drawPoint((ox + 8).toFloat(), (oy + 8).toFloat(), paint)
        }

        // Tile 6: Wood Oak Side (Vertical tree bark)
        drawTile(canvas, 6, 0) { c, ox, oy ->
            fillBase(c, ox, oy, 0xFF5D4037.toInt())
            paint.color = 0xFF3E2723.toInt()
            c.drawLine((ox + 3).toFloat(), oy.toFloat(), (ox + 3).toFloat(), (oy + 16).toFloat(), paint)
            c.drawLine((ox + 8).toFloat(), oy.toFloat(), (ox + 8).toFloat(), (oy + 16).toFloat(), paint)
            c.drawLine((ox + 13).toFloat(), oy.toFloat(), (ox + 13).toFloat(), (oy + 16).toFloat(), paint)
            paint.color = 0xFF6D4C41.toInt()
            c.drawLine((ox + 5).toFloat(), (oy + 3).toFloat(), (ox + 5).toFloat(), (oy + 12).toFloat(), paint)
            c.drawLine((ox + 10).toFloat(), (oy + 4).toFloat(), (ox + 10).toFloat(), (oy + 14).toFloat(), paint)
        }

        // Tile 7: Wood Planks (Wooden horizontal floor/wall panels)
        drawTile(canvas, 7, 0) { c, ox, oy ->
            fillBase(c, ox, oy, 0xFFBCAAA4.toInt())
            paint.color = 0xFF8D6E63.toInt()
            c.drawLine(ox.toFloat(), (oy + 4).toFloat(), (ox + 16).toFloat(), (oy + 4).toFloat(), paint)
            c.drawLine(ox.toFloat(), (oy + 8).toFloat(), (ox + 16).toFloat(), (oy + 8).toFloat(), paint)
            c.drawLine(ox.toFloat(), (oy + 12).toFloat(), (ox + 16).toFloat(), (oy + 12).toFloat(), paint)
            c.drawLine((ox + 6).toFloat(), oy.toFloat(), (ox + 6).toFloat(), (oy + 4).toFloat(), paint)
            c.drawLine((ox + 11).toFloat(), (oy + 4).toFloat(), (ox + 11).toFloat(), (oy + 8).toFloat(), paint)
            c.drawLine((ox + 5).toFloat(), (oy + 8).toFloat(), (ox + 5).toFloat(), (oy + 12).toFloat(), paint)
        }

        // Tile 8: Leaves (Lush green foliage pattern)
        drawTile(canvas, 0, 1) { c, ox, oy ->
            fillBase(c, ox, oy, 0xFF2E7D32.toInt())
            drawSpeckles(c, ox, oy, 0xFF1B5E20.toInt(), 0xFF43A047.toInt())
        }

        // Tile 9: Sand (Smooth golden grains)
        drawTile(canvas, 1, 1) { c, ox, oy ->
            fillBase(c, ox, oy, 0xFFFFE082.toInt())
            drawSpeckles(c, ox, oy, 0xFFFFCA28.toInt(), 0xFFFFF8E1.toInt())
        }

        // Tile 10: Water (Cyan blue waves)
        drawTile(canvas, 2, 1) { c, ox, oy ->
            fillBase(c, ox, oy, 0xDD1976D2.toInt())
            paint.color = 0xAA64B5F6.toInt()
            c.drawLine(ox.toFloat(), (oy + 4).toFloat(), (ox + 16).toFloat(), (oy + 4).toFloat(), paint)
            c.drawLine(ox.toFloat(), (oy + 11).toFloat(), (ox + 16).toFloat(), (oy + 11).toFloat(), paint)
        }

        // Tile 11: Glass (Translucent frame with shine line)
        drawTile(canvas, 3, 1) { c, ox, oy ->
            fillBase(c, ox, oy, 0x44E0F7FA.toInt())
            paint.color = 0xCCD4E6F1.toInt()
            paint.style = Paint.Style.STROKE
            c.drawRect(ox.toFloat(), oy.toFloat(), (ox + 15).toFloat(), (oy + 15).toFloat(), paint)
            c.drawLine((ox + 3).toFloat(), (oy + 3).toFloat(), (ox + 7).toFloat(), (oy + 7).toFloat(), paint)
            c.drawLine((ox + 9).toFloat(), (oy + 9).toFloat(), (ox + 13).toFloat(), (oy + 13).toFloat(), paint)
            paint.style = Paint.Style.FILL
        }

        // Tile 12: Stone Bricks (Clean rectangular castle stones)
        drawTile(canvas, 4, 1) { c, ox, oy ->
            fillBase(c, ox, oy, 0xFF757575.toInt())
            paint.color = 0xFF424242.toInt()
            c.drawLine(ox.toFloat(), (oy + 8).toFloat(), (ox + 16).toFloat(), (oy + 8).toFloat(), paint)
            c.drawLine((ox + 8).toFloat(), oy.toFloat(), (ox + 8).toFloat(), (oy + 8).toFloat(), paint)
            c.drawLine(ox.toFloat(), (oy + 8).toFloat(), ox.toFloat(), (oy + 16).toFloat(), paint)
        }

        // Tile 13: Red Bricks
        drawTile(canvas, 5, 1) { c, ox, oy ->
            fillBase(c, ox, oy, 0xFFC62828.toInt())
            paint.color = 0xFFEEEEEE.toInt()
            c.drawLine(ox.toFloat(), (oy + 5).toFloat(), (ox + 16).toFloat(), (oy + 5).toFloat(), paint)
            c.drawLine(ox.toFloat(), (oy + 11).toFloat(), (ox + 16).toFloat(), (oy + 11).toFloat(), paint)
            c.drawLine((ox + 8).toFloat(), oy.toFloat(), (ox + 8).toFloat(), (oy + 5).toFloat(), paint)
            c.drawLine((ox + 4).toFloat(), (oy + 5).toFloat(), (ox + 4).toFloat(), (oy + 11).toFloat(), paint)
            c.drawLine((ox + 12).toFloat(), (oy + 5).toFloat(), (ox + 12).toFloat(), (oy + 11).toFloat(), paint)
        }

        // Tile 14: Coal Ore
        drawTile(canvas, 6, 1) { c, ox, oy ->
            fillBase(c, ox, oy, 0xFF9E9E9E.toInt())
            drawOreSpecs(c, ox, oy, 0xFF212121.toInt())
        }

        // Tile 15: Iron Ore
        drawTile(canvas, 7, 1) { c, ox, oy ->
            fillBase(c, ox, oy, 0xFF9E9E9E.toInt())
            drawOreSpecs(c, ox, oy, 0xFFFFCC80.toInt())
        }

        // Tile 16: Gold Ore
        drawTile(canvas, 0, 2) { c, ox, oy ->
            fillBase(c, ox, oy, 0xFF9E9E9E.toInt())
            drawOreSpecs(c, ox, oy, 0xFFFFD700.toInt())
        }

        // Tile 17: Diamond Ore
        drawTile(canvas, 1, 2) { c, ox, oy ->
            fillBase(c, ox, oy, 0xFF9E9E9E.toInt())
            drawOreSpecs(c, ox, oy, 0xFF00E5FF.toInt())
        }

        // Tile 18: Redstone Crystal Ore
        drawTile(canvas, 2, 2) { c, ox, oy ->
            fillBase(c, ox, oy, 0xFF757575.toInt())
            drawOreSpecs(c, ox, oy, 0xFFFF1744.toInt())
        }

        // Tile 19: Torch
        drawTile(canvas, 3, 2) { c, ox, oy ->
            fillBase(c, ox, oy, 0x00000000)
            paint.color = 0xFF5D4037.toInt()
            c.drawRect((ox + 7).toFloat(), (oy + 6).toFloat(), (ox + 9).toFloat(), (oy + 15).toFloat(), paint)
            // Flame
            paint.color = 0xFFFF9800.toInt()
            c.drawRect((ox + 6).toFloat(), (oy + 2).toFloat(), (ox + 10).toFloat(), (oy + 6).toFloat(), paint)
            paint.color = 0xFFFFEB3B.toInt()
            c.drawRect((ox + 7).toFloat(), (oy + 3).toFloat(), (ox + 9).toFloat(), (oy + 5).toFloat(), paint)
        }

        // Tile 20: Gold Block
        drawTile(canvas, 4, 2) { c, ox, oy ->
            fillBase(c, ox, oy, 0xFFFFD700.toInt())
            paint.color = 0xFFFFEA00.toInt()
            c.drawRect((ox + 2).toFloat(), (oy + 2).toFloat(), (ox + 14).toFloat(), (oy + 14).toFloat(), paint)
            paint.color = 0xFFFFAB00.toInt()
            c.drawLine((ox + 1).toFloat(), (oy + 1).toFloat(), (ox + 15).toFloat(), (oy + 1).toFloat(), paint)
        }

        // Tile 21: Diamond Block
        drawTile(canvas, 5, 2) { c, ox, oy ->
            fillBase(c, ox, oy, 0xFF00E5FF.toInt())
            paint.color = 0xFF84FFFF.toInt()
            c.drawRect((ox + 2).toFloat(), (oy + 2).toFloat(), (ox + 14).toFloat(), (oy + 14).toFloat(), paint)
        }

        // Tile 22: Obsidian
        drawTile(canvas, 6, 2) { c, ox, oy ->
            fillBase(c, ox, oy, 0xFF1A0C2E.toInt())
            drawSpeckles(c, ox, oy, 0xFF311B92.toInt(), 0xFF4A148C.toInt())
        }

        // Tile 23: Bookshelf side
        drawTile(canvas, 7, 2) { c, ox, oy ->
            fillBase(c, ox, oy, 0xFF8D6E63.toInt())
            paint.color = 0xFF5D4037.toInt()
            c.drawLine(ox.toFloat(), (oy + 7).toFloat(), (ox + 16).toFloat(), (oy + 7).toFloat(), paint)
            // Books
            val colors = intArrayOf(0xFFD32F2F.toInt(), 0xFF1976D2.toInt(), 0xFF388E3C.toInt(), 0xFFFFA000.toInt())
            for (i in 0..3) {
                paint.color = colors[i % colors.size]
                c.drawRect((ox + 2 + i * 3).toFloat(), (oy + 1).toFloat(), (ox + 4 + i * 3).toFloat(), (oy + 7).toFloat(), paint)
                c.drawRect((ox + 2 + i * 3).toFloat(), (oy + 8).toFloat(), (ox + 4 + i * 3).toFloat(), (oy + 15).toFloat(), paint)
            }
        }

        // Tile 24: Neon Lamp
        drawTile(canvas, 0, 3) { c, ox, oy ->
            fillBase(c, ox, oy, 0xFF76FF03.toInt())
            paint.color = 0xFFCCFF90.toInt()
            c.drawRect((ox + 3).toFloat(), (oy + 3).toFloat(), (ox + 13).toFloat(), (oy + 13).toFloat(), paint)
        }

        // Tile 25: White Concrete
        drawTile(canvas, 1, 3) { c, ox, oy ->
            fillBase(c, ox, oy, 0xFFECEFF1.toInt())
            drawSpeckles(c, ox, oy, 0xFFCFD8DC.toInt(), 0xFFFFFFFF.toInt())
        }

        // Tile 26: Blue Roof
        drawTile(canvas, 2, 3) { c, ox, oy ->
            fillBase(c, ox, oy, 0xFF1565C0.toInt())
            paint.color = 0xFF0D47A1.toInt()
            c.drawLine(ox.toFloat(), (oy + 4).toFloat(), (ox + 16).toFloat(), (oy + 4).toFloat(), paint)
            c.drawLine(ox.toFloat(), (oy + 8).toFloat(), (ox + 16).toFloat(), (oy + 8).toFloat(), paint)
            c.drawLine(ox.toFloat(), (oy + 12).toFloat(), (ox + 16).toFloat(), (oy + 12).toFloat(), paint)
        }

        // Tile 27: Dark Tiles
        drawTile(canvas, 3, 3) { c, ox, oy ->
            fillBase(c, ox, oy, 0xFF37474F.toInt())
            paint.color = 0xFF263238.toInt()
            paint.style = Paint.Style.STROKE
            c.drawRect(ox.toFloat(), oy.toFloat(), (ox + 8).toFloat(), (oy + 8).toFloat(), paint)
            c.drawRect((ox + 8).toFloat(), (oy + 8).toFloat(), (ox + 16).toFloat(), (oy + 16).toFloat(), paint)
            paint.style = Paint.Style.FILL
        }

        // Tile 28: Flower Rose
        drawTile(canvas, 4, 3) { c, ox, oy ->
            fillBase(c, ox, oy, 0x00000000)
            paint.color = 0xFF388E3C.toInt()
            c.drawLine((ox + 8).toFloat(), (oy + 7).toFloat(), (ox + 8).toFloat(), (oy + 15).toFloat(), paint)
            paint.color = 0xFFE91E63.toInt()
            c.drawCircle((ox + 8).toFloat(), (oy + 5).toFloat(), 4f, paint)
        }

        // Tile 29: Flower Dandelion
        drawTile(canvas, 5, 3) { c, ox, oy ->
            fillBase(c, ox, oy, 0x00000000)
            paint.color = 0xFF388E3C.toInt()
            c.drawLine((ox + 8).toFloat(), (oy + 7).toFloat(), (ox + 8).toFloat(), (oy + 15).toFloat(), paint)
            paint.color = 0xFFFFEB3B.toInt()
            c.drawCircle((ox + 8).toFloat(), (oy + 5).toFloat(), 4f, paint)
        }

        return bitmap
    }

    private inline fun drawTile(canvas: Canvas, col: Int, row: Int, drawBlock: (Canvas, Int, Int) -> Unit) {
        val ox = col * TILE_SIZE
        val oy = row * TILE_SIZE
        drawBlock(canvas, ox, oy)
    }

    private fun fillBase(c: Canvas, ox: Int, oy: Int, color: Int) {
        val paint = Paint()
        paint.color = color
        c.drawRect(ox.toFloat(), oy.toFloat(), (ox + TILE_SIZE).toFloat(), (oy + TILE_SIZE).toFloat(), paint)
    }

    private fun drawSpeckles(c: Canvas, ox: Int, oy: Int, colorDark: Int, colorLight: Int) {
        val paint = Paint()
        paint.color = colorDark
        c.drawPoint((ox + 3).toFloat(), (oy + 4).toFloat(), paint)
        c.drawPoint((ox + 11).toFloat(), (oy + 2).toFloat(), paint)
        c.drawPoint((ox + 7).toFloat(), (oy + 9).toFloat(), paint)
        c.drawPoint((ox + 13).toFloat(), (oy + 12).toFloat(), paint)
        c.drawPoint((ox + 2).toFloat(), (oy + 14).toFloat(), paint)

        paint.color = colorLight
        c.drawPoint((ox + 5).toFloat(), (oy + 6).toFloat(), paint)
        c.drawPoint((ox + 10).toFloat(), (oy + 7).toFloat(), paint)
        c.drawPoint((ox + 4).toFloat(), (oy + 11).toFloat(), paint)
    }

    private fun drawOreSpecs(c: Canvas, ox: Int, oy: Int, oreColor: Int) {
        val paint = Paint()
        paint.color = oreColor
        c.drawRect((ox + 3).toFloat(), (oy + 3).toFloat(), (ox + 6).toFloat(), (oy + 6).toFloat(), paint)
        c.drawRect((ox + 10).toFloat(), (oy + 4).toFloat(), (ox + 13).toFloat(), (oy + 7).toFloat(), paint)
        c.drawRect((ox + 5).toFloat(), (oy + 9).toFloat(), (ox + 8).toFloat(), (oy + 12).toFloat(), paint)
        c.drawRect((ox + 11).toFloat(), (oy + 11).toFloat(), (ox + 14).toFloat(), (oy + 14).toFloat(), paint)
    }
}
