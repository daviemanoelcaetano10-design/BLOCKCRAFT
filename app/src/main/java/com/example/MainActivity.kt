package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.game.ui.Game3DViewport
import com.example.game.ui.GameHud
import com.example.game.ui.GameModalsHost
import com.example.game.ui.GameViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme(darkTheme = true, dynamicColor = false) {
                GameScreen(viewModel = gameViewModel)
            }
        }
    }
}

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val savedWorlds by viewModel.savedWorlds.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. 3D OpenGL First-Person Viewport
        Game3DViewport(
            renderer = viewModel.renderer,
            onLookDelta = { dx, dy ->
                viewModel.onLookDelta(dx, dy)
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. In-game HUD (Crosshair, Joystick, Action Buttons, Hotbar, Top Status Bar)
        GameHud(
            uiState = uiState,
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )

        // 3. Modals & Dialogs (Inventory, Crafting, Blueprints, Builder Tools, Saved Worlds, Settings)
        GameModalsHost(
            uiState = uiState,
            viewModel = viewModel,
            savedWorlds = savedWorlds
        )
    }
}
