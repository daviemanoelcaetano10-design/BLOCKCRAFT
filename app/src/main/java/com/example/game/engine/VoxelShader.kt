package com.example.game.engine

import android.opengl.GLES20
import android.util.Log

class VoxelShader {
    var programId: Int = 0
        private set

    // Uniform locations
    var uMVPMatrixLoc: Int = 0
    var uModelMatrixLoc: Int = 0
    var uSunDirLoc: Int = 0
    var uSunColorLoc: Int = 0
    var uAmbientColorLoc: Int = 0
    var uFogColorLoc: Int = 0
    var uFogStartLoc: Int = 0
    var uFogEndLoc: Int = 0
    var uTextureLoc: Int = 0
    var uAlphaLoc: Int = 0
    var uMiningCrackLoc: Int = 0

    // Attribute locations
    var aPositionLoc: Int = 0
    var aNormalLoc: Int = 0
    var aTexCoordLoc: Int = 0
    var aAOFactorLoc: Int = 0

    fun compile(): Boolean {
        val vertexCode = """
            uniform mat4 uMVPMatrix;
            uniform mat4 uModelMatrix;
            uniform vec3 uSunDir;
            uniform vec3 uSunColor;
            uniform vec3 uAmbientColor;
            
            attribute vec4 aPosition;
            attribute vec3 aNormal;
            attribute vec2 aTexCoord;
            attribute float aAOFactor;
            
            varying vec2 vTexCoord;
            varying vec3 vLightColor;
            varying float vDistance;
            
            void main() {
                gl_Position = uMVPMatrix * aPosition;
                vTexCoord = aTexCoord;
                
                // Normal directional lighting
                float diff = max(dot(aNormal, normalize(uSunDir)), 0.0);
                
                // Top faces are brighter, bottom faces darker
                float faceMultiplier = 1.0;
                if (aNormal.y > 0.5) faceMultiplier = 1.1;
                else if (aNormal.y < -0.5) faceMultiplier = 0.5;
                else if (abs(aNormal.x) > 0.5) faceMultiplier = 0.8;
                else faceMultiplier = 0.7;
                
                vec3 finalLight = (uAmbientColor + uSunColor * diff) * (aAOFactor * faceMultiplier);
                vLightColor = clamp(finalLight, 0.05, 1.2);
                
                vDistance = gl_Position.z;
            }
        """.trimIndent()

        val fragmentCode = """
            precision mediump float;
            
            uniform sampler2D uTexture;
            uniform vec3 uFogColor;
            uniform float uFogStart;
            uniform float uFogEnd;
            uniform float uAlpha;
            uniform float uMiningCrack;
            
            varying vec2 vTexCoord;
            varying vec3 vLightColor;
            varying float vDistance;
            
            void main() {
                vec4 texColor = texture2D(uTexture, vTexCoord);
                
                // Discard transparent pixels (for foliage/flowers)
                if (texColor.a < 0.1) {
                    discard;
                }
                
                // Apply lighting
                vec3 litColor = texColor.rgb * vLightColor;
                
                // Overlay mining crack darkness if being mined
                if (uMiningCrack > 0.0) {
                    float crackPattern = sin(vTexCoord.x * 40.0) * cos(vTexCoord.y * 40.0);
                    if (crackPattern > 0.3) {
                        litColor *= (1.0 - uMiningCrack * 0.7);
                    }
                }
                
                // Atmospheric distance fog
                float fogFactor = clamp((vDistance - uFogStart) / (uFogEnd - uFogStart), 0.0, 1.0);
                vec3 finalColor = mix(litColor, uFogColor, fogFactor);
                
                gl_FragColor = vec4(finalColor, texColor.a * uAlpha);
            }
        """.trimIndent()

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentCode)

        programId = GLES20.glCreateProgram()
        GLES20.glAttachShader(programId, vertexShader)
        GLES20.glAttachShader(programId, fragmentShader)
        GLES20.glLinkProgram(programId)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            Log.e("VoxelShader", "Shader linking failed: " + GLES20.glGetProgramInfoLog(programId))
            GLES20.glDeleteProgram(programId)
            programId = 0
            return false
        }

        // Get uniform locations
        uMVPMatrixLoc = GLES20.glGetUniformLocation(programId, "uMVPMatrix")
        uModelMatrixLoc = GLES20.glGetUniformLocation(programId, "uModelMatrix")
        uSunDirLoc = GLES20.glGetUniformLocation(programId, "uSunDir")
        uSunColorLoc = GLES20.glGetUniformLocation(programId, "uSunColor")
        uAmbientColorLoc = GLES20.glGetUniformLocation(programId, "uAmbientColor")
        uFogColorLoc = GLES20.glGetUniformLocation(programId, "uFogColor")
        uFogStartLoc = GLES20.glGetUniformLocation(programId, "uFogStart")
        uFogEndLoc = GLES20.glGetUniformLocation(programId, "uFogEnd")
        uTextureLoc = GLES20.glGetUniformLocation(programId, "uTexture")
        uAlphaLoc = GLES20.glGetUniformLocation(programId, "uAlpha")
        uMiningCrackLoc = GLES20.glGetUniformLocation(programId, "uMiningCrack")

        // Get attribute locations
        aPositionLoc = GLES20.glGetAttribLocation(programId, "aPosition")
        aNormalLoc = GLES20.glGetAttribLocation(programId, "aNormal")
        aTexCoordLoc = GLES20.glGetAttribLocation(programId, "aTexCoord")
        aAOFactorLoc = GLES20.glGetAttribLocation(programId, "aAOFactor")

        return true
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            Log.e("VoxelShader", "Could not compile shader $type: " + GLES20.glGetShaderInfoLog(shader))
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }
}
