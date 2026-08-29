package com.example.game.engine

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.game.core.BlockType
import com.example.game.core.RaycastHit
import com.example.game.core.Vector3f
import com.example.game.core.Vector3i
import com.example.game.world.StructureBlueprint
import com.example.game.world.WorldMap
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

class GameRenderer(val context: Context) : GLSurfaceView.Renderer {

    var worldMap: WorldMap = WorldMap()
    private val worldMesh = VoxelMesh()
    private val shader = VoxelShader()

    // Camera state
    val playerPos = Vector3f(24f, 15f, 24f)
    var playerYaw = 0f   // Horizontal look angle in degrees (0 = North/South)
    var playerPitch = 0f // Vertical look angle in degrees (-89 to +89)
    var eyeHeight = 1.6f

    // Day / Night cycle (0.0 = midnight, 0.25 = sunrise, 0.5 = noon, 0.75 = sunset)
    var timeOfDay = 0.35f
    var timeSpeed = 0.0003f // cycle progression speed

    // Target block & mining state
    var currentTargetHit: RaycastHit? = null
    var miningProgress: Float = 0f // 0.0 to 1.0
    var toolSwingProgress: Float = 0f // 0.0 to 1.0

    // Blueprint hologram preview
    var activeBlueprint: StructureBlueprint? = null
    var blueprintOrigin: Vector3i? = null
    var isHologramEnabled: Boolean = false

    // Particle system
    private val particles = mutableListOf<Particle3D>()

    // Matrices
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    // Wireframe line buffer for target highlight
    private var wireframeBuffer: FloatBuffer? = null

    // Hand/Tool buffer
    private var handToolBuffer: FloatBuffer? = null

    // Lighting colors
    private val sunDir = FloatArray(3)
    private val sunColor = FloatArray(3)
    private val ambientColor = FloatArray(3)
    private val fogColor = FloatArray(3)

    var fps: Int = 60
    private var frameCount = 0
    private var lastFpsTime = System.currentTimeMillis()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // Init Texture Atlas & Shaders
        TextureAtlas.initTexture()
        shader.compile()

        initWireframeBox()
        initHandTool()

        // Build initial mesh
        MeshBuilder.buildWorldMesh(worldMap, worldMesh)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / max(1, height).toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 70f, aspect, 0.1f, 150f)
    }

    override fun onDrawFrame(gl: GL10?) {
        // Calculate FPS
        frameCount++
        val now = System.currentTimeMillis()
        if (now - lastFpsTime >= 1000) {
            fps = frameCount
            frameCount = 0
            lastFpsTime = now
        }

        // Advance Day / Night cycle
        timeOfDay = (timeOfDay + timeSpeed) % 1.0f

        // Compute sun position & sky / lighting colors
        updateDayNightAtmosphere()

        // Clear screen with sky/fog color
        GLES20.glClearColor(fogColor[0], fogColor[1], fogColor[2], 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // Rebuild mesh if world modified
        if (worldMap.isMeshDirty) {
            MeshBuilder.buildWorldMesh(worldMap, worldMesh)
            worldMap.isMeshDirty = false
        }

        // Setup Camera View Matrix
        val eyeX = playerPos.x
        val eyeY = playerPos.y + eyeHeight
        val eyeZ = playerPos.z

        val yawRad = Math.toRadians(playerYaw.toDouble()).toFloat()
        val pitchRad = Math.toRadians(playerPitch.toDouble()).toFloat()

        val forwardX = cos(pitchRad) * sin(yawRad)
        val forwardY = sin(pitchRad)
        val forwardZ = -cos(pitchRad) * cos(yawRad)

        Matrix.setLookAtM(
            viewMatrix, 0,
            eyeX, eyeY, eyeZ,
            eyeX + forwardX, eyeY + forwardY, eyeZ + forwardZ,
            0f, 1f, 0f
        )

        // Use shader
        GLES20.glUseProgram(shader.programId)

        // Pass lighting & fog uniforms
        GLES20.glUniform3fv(shader.uSunDirLoc, 1, sunDir, 0)
        GLES20.glUniform3fv(shader.uSunColorLoc, 1, sunColor, 0)
        GLES20.glUniform3fv(shader.uAmbientColorLoc, 1, ambientColor, 0)
        GLES20.glUniform3fv(shader.uFogColorLoc, 1, fogColor, 0)
        GLES20.glUniform1f(shader.uFogStartLoc, 28.0f)
        GLES20.glUniform1f(shader.uFogEndLoc, 65.0f)
        GLES20.glUniform1f(shader.uAlphaLoc, 1.0f)
        GLES20.glUniform1f(shader.uMiningCrackLoc, 0.0f)

        // Bind texture atlas
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, TextureAtlas.textureId)
        GLES20.glUniform1i(shader.uTextureLoc, 0)

        // Model matrix for world is Identity
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, modelMatrix, 0)

        GLES20.glUniformMatrix4fv(shader.uMVPMatrixLoc, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(shader.uModelMatrixLoc, 1, false, modelMatrix, 0)

        // 1. Draw Solid Chunk Mesh
        drawMeshQuads(worldMesh.vertexBuffer, worldMesh.indexBuffer, worldMesh.indexCount)

        // 2. Draw Translucent (Water, Glass) Mesh with disabled culling for double-sided look
        if (worldMesh.transIndexCount > 0) {
            GLES20.glDisable(GLES20.GL_CULL_FACE)
            GLES20.glUniform1f(shader.uAlphaLoc, 0.85f)
            drawMeshQuads(worldMesh.transVertexBuffer, worldMesh.transIndexBuffer, worldMesh.transIndexCount)
            GLES20.glEnable(GLES20.GL_CULL_FACE)
            GLES20.glUniform1f(shader.uAlphaLoc, 1.0f)
        }

        // 3. Draw Target Block Highlight Box (if focused)
        val hit = currentTargetHit
        if (hit != null) {
            drawTargetHighlight(hit.blockPos)
        }

        // 4. Draw Blueprint Hologram Preview (if active)
        if (isHologramEnabled && activeBlueprint != null && blueprintOrigin != null) {
            drawBlueprintHologram(activeBlueprint!!, blueprintOrigin!!)
        }

        // 5. Draw 3D Particles
        updateAndDrawParticles()

        // 6. Draw First Person Tool / Hand in view space
        drawFirstPersonTool()
    }

    private fun updateDayNightAtmosphere() {
        // Sun angle: 0.0 = -PI (below horizon), 0.5 = +PI/2 (zenith)
        val sunAngle = (timeOfDay - 0.25f) * 2.0 * Math.PI
        sunDir[0] = cos(sunAngle).toFloat()
        sunDir[1] = sin(sunAngle).toFloat()
        sunDir[2] = 0.35f

        val sunHeight = sunDir[1]

        if (sunHeight > 0.1f) {
            // Day time: Bright blue sky, warm sunlight
            val dayIntensity = (sunHeight / 1.0f).coerceIn(0.2f, 1.0f)
            fogColor[0] = 0.52f * dayIntensity
            fogColor[1] = 0.75f * dayIntensity
            fogColor[2] = 0.95f * dayIntensity

            sunColor[0] = 1.0f
            sunColor[1] = 0.95f
            sunColor[2] = 0.85f

            ambientColor[0] = 0.45f * dayIntensity
            ambientColor[1] = 0.45f * dayIntensity
            ambientColor[2] = 0.50f * dayIntensity
        } else if (sunHeight in -0.2f..0.1f) {
            // Sunset / Sunrise: Vibrant golden-orange hues
            val t = (sunHeight + 0.2f) / 0.3f
            fogColor[0] = 0.85f * (1f - t) + 0.52f * t
            fogColor[1] = 0.42f * (1f - t) + 0.75f * t
            fogColor[2] = 0.25f * (1f - t) + 0.95f * t

            sunColor[0] = 1.0f
            sunColor[1] = 0.6f
            sunColor[2] = 0.3f

            ambientColor[0] = 0.35f
            ambientColor[1] = 0.28f
            ambientColor[2] = 0.30f
        } else {
            // Night time: Deep mystical indigo sky with dim moonlight
            fogColor[0] = 0.05f
            fogColor[1] = 0.07f
            fogColor[2] = 0.14f

            sunColor[0] = 0.2f
            sunColor[1] = 0.25f
            sunColor[2] = 0.4f

            ambientColor[0] = 0.18f
            ambientColor[1] = 0.18f
            ambientColor[2] = 0.25f
        }
    }

    private fun drawMeshQuads(vBuf: FloatBuffer?, iBuf: ShortBuffer?, count: Int) {
        if (vBuf == null || iBuf == null || count == 0) return

        val stride = 9 * 4 // 9 floats per vertex (x,y,z, nx,ny,nz, u,v, ao)
        val vBuffer = vBuf as Buffer
        val iBuffer = iBuf as Buffer

        vBuffer.position(0)
        GLES20.glEnableVertexAttribArray(shader.aPositionLoc)
        GLES20.glVertexAttribPointer(shader.aPositionLoc, 3, GLES20.GL_FLOAT, false, stride, vBuf)

        vBuffer.position(3)
        GLES20.glEnableVertexAttribArray(shader.aNormalLoc)
        GLES20.glVertexAttribPointer(shader.aNormalLoc, 3, GLES20.GL_FLOAT, false, stride, vBuf)

        vBuffer.position(6)
        GLES20.glEnableVertexAttribArray(shader.aTexCoordLoc)
        GLES20.glVertexAttribPointer(shader.aTexCoordLoc, 2, GLES20.GL_FLOAT, false, stride, vBuf)

        vBuffer.position(8)
        GLES20.glEnableVertexAttribArray(shader.aAOFactorLoc)
        GLES20.glVertexAttribPointer(shader.aAOFactorLoc, 1, GLES20.GL_FLOAT, false, stride, vBuf)

        iBuffer.position(0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, count, GLES20.GL_UNSIGNED_SHORT, iBuf)

        GLES20.glDisableVertexAttribArray(shader.aPositionLoc)
        GLES20.glDisableVertexAttribArray(shader.aNormalLoc)
        GLES20.glDisableVertexAttribArray(shader.aTexCoordLoc)
        GLES20.glDisableVertexAttribArray(shader.aAOFactorLoc)
    }

    private fun initWireframeBox() {
        val min = -0.01f
        val max = 1.01f

        // 12 lines of unit cube wireframe
        val lines = floatArrayOf(
            min, min, min,  max, min, min,
            max, min, min,  max, max, min,
            max, max, min,  min, max, min,
            min, max, min,  min, min, min,

            min, min, max,  max, min, max,
            max, min, max,  max, max, max,
            max, max, max,  min, max, max,
            min, max, max,  min, min, max,

            min, min, min,  min, min, max,
            max, min, min,  max, min, max,
            max, max, min,  max, max, max,
            min, max, min,  min, max, max
        )

        val bb = ByteBuffer.allocateDirect(lines.size * 4).order(ByteOrder.nativeOrder())
        wireframeBuffer = bb.asFloatBuffer().apply {
            put(lines)
            position(0)
        }
    }

    private fun drawTargetHighlight(pos: Vector3i) {
        val wf = wireframeBuffer ?: return

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat())

        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, modelMatrix, 0)

        GLES20.glUniformMatrix4fv(shader.uMVPMatrixLoc, 1, false, mvpMatrix, 0)
        GLES20.glUniform1f(shader.uAlphaLoc, 0.9f)
        GLES20.glUniform1f(shader.uMiningCrackLoc, miningProgress)

        (wf as Buffer).position(0)
        GLES20.glEnableVertexAttribArray(shader.aPositionLoc)
        GLES20.glVertexAttribPointer(shader.aPositionLoc, 3, GLES20.GL_FLOAT, false, 3 * 4, wf)

        // Black wireframe box lines
        GLES20.glLineWidth(3.0f)
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, 24)

        GLES20.glDisableVertexAttribArray(shader.aPositionLoc)
    }

    private fun drawBlueprintHologram(blueprint: StructureBlueprint, origin: Vector3i) {
        GLES20.glUniform1f(shader.uAlphaLoc, 0.45f)
        blueprint.blocks.forEach { b ->
            if (b.blockType != BlockType.AIR) {
                val bx = origin.x + b.relX
                val by = origin.y + b.relY
                val bz = origin.z + b.relZ

                // Only draw if target cell is air
                if (worldMap.getBlock(bx, by, bz) == BlockType.AIR) {
                    drawTargetHighlight(Vector3i(bx, by, bz))
                }
            }
        }
        GLES20.glUniform1f(shader.uAlphaLoc, 1.0f)
    }

    private fun initHandTool() {
        // Small 3D box representing pickaxe/held block in first-person view
        val s = 0.25f
        val verts = floatArrayOf(
            // x, y, z, nx, ny, nz, u, v, ao
            // Front
            -s,  s,  s,  0f, 0f, 1f, 0f, 0f, 1f,
            -s, -s,  s,  0f, 0f, 1f, 0f, 1f, 1f,
             s, -s,  s,  0f, 0f, 1f, 1f, 1f, 1f,
             s,  s,  s,  0f, 0f, 1f, 1f, 0f, 1f
        )
        val bb = ByteBuffer.allocateDirect(verts.size * 4).order(ByteOrder.nativeOrder())
        handToolBuffer = bb.asFloatBuffer().apply {
            put(verts)
            (this as Buffer).position(0)
        }
    }

    private fun drawFirstPersonTool() {
        // Tool in bottom right corner with swing animation
        val swing = sin(toolSwingProgress * Math.PI.toFloat()) * 35f

        Matrix.setIdentityM(modelMatrix, 0)
        // Position tool in front of camera
        Matrix.translateM(modelMatrix, 0, 0.45f, -0.4f, -0.7f)
        Matrix.rotateM(modelMatrix, 0, -45f + swing, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, 30f, 0f, 1f, 0f)

        // Use custom HUD projection matrix
        val hudProj = FloatArray(16)
        Matrix.perspectiveM(hudProj, 0, 60f, 1.0f, 0.1f, 10f)

        val toolMvp = FloatArray(16)
        Matrix.multiplyMM(toolMvp, 0, hudProj, 0, modelMatrix, 0)

        GLES20.glUniformMatrix4fv(shader.uMVPMatrixLoc, 1, false, toolMvp, 0)

        // Clear depth so tool is always drawn on top of world geometry
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT)

        val tb = handToolBuffer ?: return
        val tBuf = tb as Buffer
        tBuf.position(0)
        GLES20.glEnableVertexAttribArray(shader.aPositionLoc)
        GLES20.glVertexAttribPointer(shader.aPositionLoc, 3, GLES20.GL_FLOAT, false, 9 * 4, tb)

        tBuf.position(3)
        GLES20.glEnableVertexAttribArray(shader.aNormalLoc)
        GLES20.glVertexAttribPointer(shader.aNormalLoc, 3, GLES20.GL_FLOAT, false, 9 * 4, tb)

        tBuf.position(6)
        GLES20.glEnableVertexAttribArray(shader.aTexCoordLoc)
        GLES20.glVertexAttribPointer(shader.aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 9 * 4, tb)

        tBuf.position(8)
        GLES20.glEnableVertexAttribArray(shader.aAOFactorLoc)
        GLES20.glVertexAttribPointer(shader.aAOFactorLoc, 1, GLES20.GL_FLOAT, false, 9 * 4, tb)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4)

        GLES20.glDisableVertexAttribArray(shader.aPositionLoc)
        GLES20.glDisableVertexAttribArray(shader.aNormalLoc)
        GLES20.glDisableVertexAttribArray(shader.aTexCoordLoc)
        GLES20.glDisableVertexAttribArray(shader.aAOFactorLoc)
    }

    fun spawnBlockParticles(pos: Vector3i, blockType: BlockType) {
        val rand = kotlin.random.Random
        for (i in 0..12) {
            val p = Particle3D(
                x = pos.x + 0.5f + (rand.nextFloat() - 0.5f) * 0.6f,
                y = pos.y + 0.5f + (rand.nextFloat() - 0.5f) * 0.6f,
                z = pos.z + 0.5f + (rand.nextFloat() - 0.5f) * 0.6f,
                vx = (rand.nextFloat() - 0.5f) * 0.15f,
                vy = rand.nextFloat() * 0.2f + 0.05f,
                vz = (rand.nextFloat() - 0.5f) * 0.15f,
                life = 1.0f,
                decay = 0.04f + rand.nextFloat() * 0.03f
            )
            particles.add(p)
        }
    }

    private fun updateAndDrawParticles() {
        if (particles.isEmpty()) return

        val it = particles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.x += p.vx
            p.y += p.vy
            p.z += p.vz
            p.vy -= 0.01f // gravity
            p.life -= p.decay

            if (p.life <= 0f) {
                it.remove()
            }
        }
    }

    fun getCameraLookVector(): Vector3f {
        val yawRad = Math.toRadians(playerYaw.toDouble()).toFloat()
        val pitchRad = Math.toRadians(playerPitch.toDouble()).toFloat()
        return Vector3f(
            cos(pitchRad) * sin(yawRad),
            sin(pitchRad),
            -cos(pitchRad) * cos(yawRad)
        )
    }
}

data class Particle3D(
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float,
    var vy: Float,
    var vz: Float,
    var life: Float,
    var decay: Float
)
