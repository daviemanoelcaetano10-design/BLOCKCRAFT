package com.example.game.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.core.BlockType
import com.example.game.core.ItemRegistry
import com.example.game.core.ItemStack
import com.example.game.world.StructureBlueprint
import com.example.game.world.StructureBlueprints

@Composable
fun GameHud(
    uiState: GameUiState,
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {

        // 1. Top Status & Menu Bar
        TopGameBar(
            uiState = uiState,
            viewModel = viewModel,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )

        // 2. Banner Notification Toast
        AnimatedVisibility(
            visible = uiState.bannerMessage != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 70.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xDD0D111A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uiState.bannerMessage ?: "",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 3. Center Crosshair & Target Block Info
        CenterCrosshair(
            uiState = uiState,
            modifier = Modifier.align(Alignment.Center)
        )

        // 4. Blueprint Active Hologram Action Bar (if active)
        if (uiState.activeBlueprint != null) {
            BlueprintActionBar(
                blueprint = uiState.activeBlueprint,
                onBuild = { viewModel.instantBuildActiveBlueprint() },
                onCancel = {
                    viewModel.renderer.activeBlueprint = null
                    viewModel.renderer.isHologramEnabled = false
                    viewModel.closeModal()
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 80.dp)
            )
        }

        // 5. Left Virtual Movement Joystick & Sprint button
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 16.dp, bottom = 80.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Sprint / Run Button
                IconButton(
                    onClick = { viewModel.toggleRunning() },
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (uiState.isRunning) Color(0xFFFF9800) else Color(0x66000000))
                        .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                        .testTag("sprint_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = "Correr",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                VirtualJoystick(
                    onMove = { x, y ->
                        viewModel.joystickX = x
                        viewModel.joystickY = y
                    }
                )
            }
        }

        // 6. Right Action Buttons (Mine, Place, Jump, Fly)
        RightActionControls(
            uiState = uiState,
            viewModel = viewModel,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 80.dp)
        )

        // 7. Bottom Hotbar (9 selectable slots)
        BottomHotbar(
            slots = uiState.hotbarSlots,
            selectedIndex = uiState.selectedHotbarIndex,
            onSelectSlot = { viewModel.selectHotbarSlot(it) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
        )
    }
}

@Composable
fun TopGameBar(
    uiState: GameUiState,
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xBB0D111A),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left info: Coordinates, Cardinal Direction & Mode
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = uiState.worldName,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (uiState.gameMode == GameMode.CREATIVE) Color(0xFF9C27B0) else Color(0xFF388E3C)
                    ) {
                        Text(
                            text = if (uiState.gameMode == GameMode.CREATIVE) "CRIATIVO" else "SOBREVIVÊNCIA",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "X:${uiState.playerCoordinates.x} Y:${uiState.playerCoordinates.y} Z:${uiState.playerCoordinates.z} • ${uiState.cardinalDirection} • ${uiState.fps} FPS",
                    color = Color(0xFFB0BEC5),
                    fontSize = 11.sp
                )
            }

            // Quick Menu Action Icons
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Backpack / Inventory
                MenuIconButton(
                    icon = Icons.Default.Backpack,
                    label = "Inventário",
                    onClick = { viewModel.openModal(ActiveModal.INVENTORY) },
                    testTag = "btn_inventory"
                )

                // Crafting Table
                MenuIconButton(
                    icon = Icons.Default.Handyman,
                    label = "Criar",
                    onClick = { viewModel.openModal(ActiveModal.CRAFTING) },
                    testTag = "btn_crafting"
                )

                // Blueprints / Structures
                MenuIconButton(
                    icon = Icons.Default.AccountBalance,
                    label = "Estruturas",
                    badgeCount = StructureBlueprints.ALL_BLUEPRINTS.size,
                    onClick = { viewModel.openModal(ActiveModal.BLUEPRINTS) },
                    testTag = "btn_blueprints"
                )

                // Builder Wand Tools
                MenuIconButton(
                    icon = Icons.Default.AutoFixHigh,
                    label = "Varinha",
                    onClick = { viewModel.openModal(ActiveModal.BUILDER_TOOLS) },
                    testTag = "btn_builder_tools"
                )

                // Worlds Save/Load
                MenuIconButton(
                    icon = Icons.Default.Public,
                    label = "Mundos",
                    onClick = { viewModel.openModal(ActiveModal.WORLDS_MANAGER) },
                    testTag = "btn_worlds"
                )

                // Settings
                MenuIconButton(
                    icon = Icons.Default.Settings,
                    label = "Config",
                    onClick = { viewModel.openModal(ActiveModal.SETTINGS) },
                    testTag = "btn_settings"
                )
            }
        }
    }
}

@Composable
fun MenuIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    badgeCount: Int = 0,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x44FFFFFF))
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        if (badgeCount > 0) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFFF5722),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(14.dp)
            ) {
                Text(
                    text = "$badgeCount",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun CenterCrosshair(
    uiState: GameUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Crosshair Icon
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            // Horizontal line
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.85f))
            )
            // Vertical line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(18.dp)
                    .background(Color.White.copy(alpha = 0.85f))
            )
            // Center dot
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(Color(0xFF00E5FF), CircleShape)
            )
        }

        // Targeted Block Info Tooltip
        if (uiState.targetBlock != null && uiState.targetBlock != BlockType.AIR) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xCC000000),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(uiState.targetBlock.mapColor, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${uiState.targetBlock.displayName} (${String.format("%.1fm", uiState.targetDistance)})",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun BlueprintActionBar(
    blueprint: StructureBlueprint,
    onBuild: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xEE1A2035),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF)),
        shadowElevation = 10.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Holograma 3D: ${blueprint.name}",
                color = Color(0xFF00E5FF),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Dimensões: ${blueprint.sizeX}x${blueprint.sizeY}x${blueprint.sizeZ} (${blueprint.blocks.size} blocos)",
                color = Color.LightGray,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Button(
                    onClick = onBuild,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_confirm_blueprint")
                ) {
                    Icon(imageVector = Icons.Default.Construction, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Construir Agora", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancelar", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun RightActionControls(
    uiState: GameUiState,
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Toggle Fly Mode button
        IconButton(
            onClick = { viewModel.toggleFlyMode() },
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (uiState.isFlying) Color(0xFF9C27B0) else Color(0x66000000))
                .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                .testTag("fly_toggle_button")
        ) {
            Icon(
                imageVector = Icons.Default.Flight,
                contentDescription = "Alternar Voo",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Fly Down Button (visible if flying)
        if (uiState.isFlying) {
            IconButton(
                onClick = { viewModel.onFlyDown() },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0x88673AB7))
                    .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                    .testTag("fly_down_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "Descer",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Jump / Fly Up Button
        IconButton(
            onClick = { viewModel.onJump() },
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF2196F3), Color(0xFF0D47A1))))
                .border(1.5.dp, Color.White, CircleShape)
                .testTag("jump_button")
        ) {
            Icon(
                imageVector = if (uiState.isFlying) Icons.Default.ArrowUpward else Icons.Default.North,
                contentDescription = "Pular / Subir",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        // Place Block Action Button
        IconButton(
            onClick = { viewModel.onPlaceAction() },
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF4CAF50), Color(0xFF1B5E20))))
                .border(1.5.dp, Color.White, CircleShape)
                .testTag("place_block_button")
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.AddBox,
                    contentDescription = "Colocar Bloco",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = "COLOCAR",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Big Mine / Break Action Button (Hold or Tap to mine)
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(
                    if (uiState.isMining) Brush.radialGradient(listOf(Color(0xFFFF1744), Color(0xFFB71C1C)))
                    else Brush.radialGradient(listOf(Color(0xFFFF5722), Color(0xFFBF360C)))
                )
                .border(2.dp, Color.White, CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            viewModel.startMining()
                            tryAwaitRelease()
                            viewModel.stopMining()
                        }
                    )
                }
                .testTag("mine_block_button"),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Hardware,
                    contentDescription = "Minerar Bloco",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = if (uiState.isMining) "${(uiState.miningProgress * 100).toInt()}%" else "MINERAR",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BottomHotbar(
    slots: List<ItemStack?>,
    selectedIndex: Int,
    onSelectSlot: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xDD0D111A),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF)),
        shadowElevation = 8.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            slots.forEachIndexed { index, stack ->
                val isSelected = index == selectedIndex
                HotbarSlotItem(
                    index = index,
                    stack = stack,
                    isSelected = isSelected,
                    onClick = { onSelectSlot(index) }
                )
            }
        }
    }
}

@Composable
fun HotbarSlotItem(
    index: Int,
    stack: ItemStack?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val item = stack?.let { ItemRegistry.getItem(it.itemId) }

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0x6600E5FF) else Color(0x33FFFFFF))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFF00E5FF) else Color(0x22FFFFFF),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .testTag("hotbar_slot_$index"),
        contentAlignment = Alignment.Center
    ) {
        if (item != null) {
            // Block color box or tool icon
            if (item.blockType != null) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(item.blockType.mapColor, RoundedCornerShape(4.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                )
            } else {
                Icon(
                    imageVector = when {
                        item.id.contains("pickaxe") -> Icons.Default.Hardware
                        item.id.contains("axe") -> Icons.Default.Carpenter
                        item.id.contains("wand") -> Icons.Default.AutoFixHigh
                        else -> Icons.Default.Category
                    },
                    contentDescription = item.name,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Stack count badge
            if (stack != null && stack.count > 1) {
                Text(
                    text = "${stack.count}",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                )
            }
        }
    }
}
