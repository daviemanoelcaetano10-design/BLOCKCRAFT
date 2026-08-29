package com.example.game.ui

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.game.engine.GameRenderer

@Composable
fun Game3DViewport(
    renderer: GameRenderer,
    onLookDelta: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            TouchGLSurfaceView(context, renderer, onLookDelta)
        },
        modifier = modifier
    )
}

class TouchGLSurfaceView(
    context: Context,
    private val renderer: GameRenderer,
    private val onLookDelta: (Float, Float) -> Unit
) : GLSurfaceView(context) {

    private var previousX = 0f
    private var previousY = 0f
    private var activePointerId = -1

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Touch on right half of screen controls camera orientation (Yaw/Pitch)
        val pointerIndex = event.actionIndex
        val x = event.getX(pointerIndex)
        val y = event.getY(pointerIndex)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                // If touch is on right 60% of the screen, capture it for camera look
                if (x > width * 0.35f && activePointerId == -1) {
                    activePointerId = event.getPointerId(pointerIndex)
                    previousX = x
                    previousY = y
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (activePointerId != -1) {
                    val idx = event.findPointerIndex(activePointerId)
                    if (idx != -1) {
                        val curX = event.getX(idx)
                        val curY = event.getY(idx)

                        val dx = curX - previousX
                        val dy = curY - previousY

                        if (Math.abs(dx) > 0.5f || Math.abs(dy) > 0.5f) {
                            onLookDelta(dx, dy)
                            previousX = curX
                            previousY = curY
                        }
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val id = event.getPointerId(pointerIndex)
                if (id == activePointerId) {
                    activePointerId = -1
                }
            }
        }
        return true
    }
}
