package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.game.core.BlockType
import com.example.game.core.CraftingRecipe
import com.example.game.core.ItemCategory
import com.example.game.core.ItemRegistry
import com.example.game.core.ItemStack
import com.example.game.data.WorldEntity
import com.example.game.world.StructureBlueprint
import com.example.game.world.StructureBlueprints
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GameModalsHost(
    uiState: GameUiState,
    viewModel: GameViewModel,
    savedWorlds: List<WorldEntity>
) {
    val modal = uiState.activeModal ?: return

    Dialog(
        onDismissRequest = { viewModel.closeModal() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xF00D111A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)),
            shadowElevation = 16.dp
        ) {
            when (modal) {
                ActiveModal.INVENTORY -> InventoryModalContent(uiState, viewModel)
                ActiveModal.CRAFTING -> CraftingModalContent(uiState, viewModel)
                ActiveModal.BLUEPRINTS -> BlueprintsModalContent(uiState, viewModel)
                ActiveModal.BUILDER_TOOLS -> BuilderToolsModalContent(uiState, viewModel)
                ActiveModal.WORLDS_MANAGER -> WorldsManagerModalContent(uiState, viewModel, savedWorlds)
                ActiveModal.SETTINGS -> SettingsModalContent(uiState, viewModel)
            }
        }
    }
}

// -------------------------------------------------------------
// 1. INVENTORY MODAL
// -------------------------------------------------------------
@Composable
fun InventoryModalContent(uiState: GameUiState, viewModel: GameViewModel) {
    var selectedCategory by remember { mutableStateOf<ItemCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val allRegistered = ItemRegistry.ITEMS.values.toList()
    val isCreative = uiState.gameMode == GameMode.CREATIVE

    val displayedItems = remember(selectedCategory, searchQuery, isCreative, uiState.inventory) {
        if (isCreative) {
            allRegistered.filter { item ->
                (selectedCategory == null || item.category == selectedCategory) &&
                        (searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true))
            }
        } else {
            uiState.inventory.mapNotNull { stack ->
                ItemRegistry.getItem(stack.itemId)
            }.filter { item ->
                (selectedCategory == null || item.category == selectedCategory) &&
                        (searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true))
            }.distinctBy { it.id }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ModalHeader(
            title = if (isCreative) "Catálogo Criativo de Itens" else "Mochila do Explorador",
            subtitle = if (isCreative) "Selecione qualquer item ou bloco para equipar" else "Gerencie seus recursos coletados",
            onClose = { viewModel.closeModal() }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("Todos", fontSize = 11.sp) }
            )
            ItemCategory.entries.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = {
                        Text(
                            when (cat) {
                                ItemCategory.BLOCKS -> "Blocos"
                                ItemCategory.TOOLS -> "Ferramentas"
                                ItemCategory.MATERIALS -> "Minérios"
                                ItemCategory.DECOR -> "Decoração"
                                ItemCategory.BLUEPRINTS -> "Plantas"
                            },
                            fontSize = 11.sp
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Items Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 68.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(displayedItems) { item ->
                val countInInventory = uiState.inventory.find { it.itemId == item.id }?.count ?: (if (isCreative) 64 else 0)

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x33FFFFFF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                    modifier = Modifier
                        .height(80.dp)
                        .clickable {
                            if (isCreative) {
                                viewModel.addItemToInventory(item.id, 64)
                            } else {
                                // Put in hotbar active slot
                                val currentHotbar = uiState.hotbarSlots.toMutableList()
                                currentHotbar[uiState.selectedHotbarIndex] = ItemStack(item.id, countInInventory)
                                viewModel.selectHotbarSlot(uiState.selectedHotbarIndex)
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (item.blockType != null) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(item.blockType.mapColor, RoundedCornerShape(4.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            )
                        } else {
                            Icon(
                                imageVector = when {
                                    item.id.contains("pickaxe") -> Icons.Default.Hardware
                                    item.id.contains("axe") -> Icons.Default.Carpenter
                                    item.id.contains("wand") -> Icons.Default.AutoFixHigh
                                    else -> Icons.Default.Diamond
                                },
                                contentDescription = item.name,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.name,
                            color = Color.White,
                            fontSize = 9.sp,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (isCreative) "∞" else "x$countInInventory",
                            color = Color(0xFF00E5FF),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "💡 Dica: Toque em um item para equipá-lo no slot selecionado da barra de acesso rápido.",
            color = Color.Gray,
            fontSize = 11.sp
        )
    }
}

// -------------------------------------------------------------
// 2. CRAFTING MODAL
// -------------------------------------------------------------
@Composable
fun CraftingModalContent(uiState: GameUiState, viewModel: GameViewModel) {
    val recipes = ItemRegistry.RECIPES

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ModalHeader(
            title = "Mesa de Criação & Engenharia",
            subtitle = "Transforme recursos brutos em materiais nobres e ferramentas",
            onClose = { viewModel.closeModal() }
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(recipes) { recipe ->
                CraftingRecipeCard(recipe = recipe, uiState = uiState, onCraft = { viewModel.craftRecipe(recipe) })
            }
        }
    }
}

@Composable
fun CraftingRecipeCard(
    recipe: CraftingRecipe,
    uiState: GameUiState,
    onCraft: () -> Unit
) {
    val outputItem = ItemRegistry.getItem(recipe.outputItemId)
    val isCreative = uiState.gameMode == GameMode.CREATIVE

    // Check if player has all ingredients
    val canCraft = remember(uiState.inventory, recipe, isCreative) {
        if (isCreative) true
        else recipe.ingredients.all { ing ->
            val count = uiState.inventory.filter { it.itemId == ing.itemId }.sumOf { it.count }
            count >= ing.amount
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0x33FFFFFF),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (canCraft) Color(0xFF4CAF50) else Color(0x33FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Output icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0x4400E5FF), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (outputItem?.blockType != null) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(outputItem.blockType.mapColor, RoundedCornerShape(4.dp))
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Handyman,
                        contentDescription = recipe.name,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Recipe info & required ingredients
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${recipe.name} (x${recipe.outputAmount})",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = recipe.description,
                    color = Color.LightGray,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Ingredients checklist
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    recipe.ingredients.forEach { ing ->
                        val ingItem = ItemRegistry.getItem(ing.itemId)
                        val inInv = uiState.inventory.filter { it.itemId == ing.itemId }.sumOf { it.count }
                        val hasEnough = isCreative || inInv >= ing.amount

                        Text(
                            text = "${ingItem?.name ?: ing.itemId}: ${if (isCreative) "∞" else "$inInv/${ing.amount}"}",
                            color = if (hasEnough) Color(0xFF81C784) else Color(0xFFE57373),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Craft Button
            Button(
                onClick = onCraft,
                enabled = canCraft,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_craft_${recipe.id}")
            ) {
                Text("Criar", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// 3. BLUEPRINTS / ESTRUTURAS COMPLEXAS MODAL
// -------------------------------------------------------------
@Composable
fun BlueprintsModalContent(uiState: GameUiState, viewModel: GameViewModel) {
    val blueprints = StructureBlueprints.ALL_BLUEPRINTS

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ModalHeader(
            title = "Projetos Arquitetônicos & Estruturas",
            subtitle = "Gere castelos, mansões, pirâmides, torres e pontes no mapa aberto",
            onClose = { viewModel.closeModal() }
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(blueprints) { bp ->
                BlueprintCard(
                    blueprint = bp,
                    uiState = uiState,
                    onSelect = {
                        viewModel.selectBlueprint(bp)
                        viewModel.closeModal()
                    },
                    onInstantBuild = {
                        viewModel.selectBlueprint(bp)
                        viewModel.instantBuildActiveBlueprint()
                    }
                )
            }
        }
    }
}

@Composable
fun BlueprintCard(
    blueprint: StructureBlueprint,
    uiState: GameUiState,
    onSelect: () -> Unit,
    onInstantBuild: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0x33FFFFFF),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = blueprint.name,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF00E5FF)
                        ) {
                            Text(
                                text = blueprint.category.uppercase(),
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Dimensões: ${blueprint.sizeX}x${blueprint.sizeY}x${blueprint.sizeZ} (${blueprint.blocks.size} blocos)",
                        color = Color(0xFF81D4FA),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = blueprint.description,
                color = Color.LightGray,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Materials Preview Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                blueprint.requiredMaterialsSummary.entries.take(4).forEach { (block, count) ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0x33000000),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.Gray)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(8.dp).background(block.mapColor, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${block.displayName.take(8)}.. x$count",
                                color = Color.White,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onSelect,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("btn_hologram_${blueprint.id}")
                ) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Guia Holograma", color = Color(0xFF00E5FF), fontSize = 11.sp)
                }

                Button(
                    onClick = onInstantBuild,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("btn_build_${blueprint.id}")
                ) {
                    Icon(imageVector = Icons.Default.Construction, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Construir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. BUILDER WAND / MACRO TOOLS MODAL
// -------------------------------------------------------------
@Composable
fun BuilderToolsModalContent(uiState: GameUiState, viewModel: GameViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ModalHeader(
            title = "Varinha do Macro-Construtor",
            subtitle = "Construa muralhas, plataformas e pisos inteiros com 2 cliques",
            onClose = { viewModel.closeModal() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        val modes = listOf(
            Triple(BuildToolMode.SINGLE_BLOCK, "Bloco Único", "Colocação padrão de 1 em 1 bloco"),
            Triple(BuildToolMode.LINE_WALL, "Muralha Contínua", "Define 2 pontos e ergue uma parede inteira"),
            Triple(BuildToolMode.FLOOR_AREA, "Piso e Plataforma", "Preenche toda a área do solo entre 2 pontos"),
            Triple(BuildToolMode.HOLLOW_BOX, "Estrutura Cúbica", "Cria uma caixa sólida 3D de madeira")
        )

        modes.forEach { (mode, name, desc) ->
            val isSelected = uiState.buildToolMode == mode
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) Color(0x4400E5FF) else Color(0x22FFFFFF),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isSelected) Color(0xFF00E5FF) else Color(0x33FFFFFF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clickable { viewModel.setBuildToolMode(mode) }
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isSelected) Color(0xFF00E5FF) else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(desc, color = Color.LightGray, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = { viewModel.closeModal() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Confirmar Ferramenta", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

// -------------------------------------------------------------
// 5. WORLDS MANAGER MODAL (ROOM DB PERSISTENCE)
// -------------------------------------------------------------
@Composable
fun WorldsManagerModalContent(
    uiState: GameUiState,
    viewModel: GameViewModel,
    savedWorlds: List<WorldEntity>
) {
    var newWorldName by remember { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ModalHeader(
            title = "Gerenciador de Mundos Salvos",
            subtitle = "Persistência local no Room Database de todos os seus blocos e construções",
            onClose = { viewModel.closeModal() }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Save Current World Action Bar
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0x444CAF50),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Mundo Atual: ${uiState.worldName}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Blocos colocados: ${uiState.blocksPlacedCount} | Estruturas: ${uiState.structuresBuiltCount}", color = Color.LightGray, fontSize = 10.sp)
                }
                Button(
                    onClick = { viewModel.saveCurrentWorld() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_save_world")
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Salvar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Create New World Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newWorldName,
                onValueChange = { newWorldName = it },
                placeholder = { Text("Nome do Novo Mundo...", fontSize = 12.sp, color = Color.Gray) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color.Gray
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    viewModel.createNewWorld(newWorldName)
                    newWorldName = ""
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Gerar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Mundos Salvos no Dispositivo (${savedWorlds.size}):", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(savedWorlds) { world ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x33FFFFFF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(world.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Modo: ${world.gameMode} • Salvo em: ${dateFormat.format(Date(world.updatedAt))}", color = Color.LightGray, fontSize = 10.sp)
                        }
                        Row {
                            Button(
                                onClick = { viewModel.loadWorld(world) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Text("Carregar", fontSize = 11.sp)
                            }
                            IconButton(onClick = { viewModel.deleteSavedWorld(world.id) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir", tint = Color(0xFFEF5350), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 6. SETTINGS MODAL
// -------------------------------------------------------------
@Composable
fun SettingsModalContent(uiState: GameUiState, viewModel: GameViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ModalHeader(
            title = "Configurações do Jogo",
            subtitle = "Ajuste o modo de jogo, ciclo de iluminação e preferências",
            onClose = { viewModel.closeModal() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Game Mode Switcher
        Text("Modo de Jogo:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.setGameMode(GameMode.SURVIVAL) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.gameMode == GameMode.SURVIVAL) Color(0xFF388E3C) else Color(0x33FFFFFF)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("🌲 Sobrevivência", fontSize = 12.sp)
            }

            Button(
                onClick = { viewModel.setGameMode(GameMode.CREATIVE) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.gameMode == GameMode.CREATIVE) Color(0xFF9C27B0) else Color(0x33FFFFFF)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("✨ Criativo (Infinito)", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Time of Day Slider
        Text("Hora do Dia (Iluminação Solar):", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Slider(
            value = uiState.timeOfDay,
            onValueChange = { viewModel.setTimeOfDay(it) },
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFFD54F),
                activeTrackColor = Color(0xFFFF9800)
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("🌙 Meia-noite", color = Color.LightGray, fontSize = 10.sp)
            Text("🌅 Amanhecer", color = Color.LightGray, fontSize = 10.sp)
            Text("☀️ Meio-dia", color = Color.LightGray, fontSize = 10.sp)
            Text("🌇 Pôr do Sol", color = Color.LightGray, fontSize = 10.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Stats summary
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0x22FFFFFF),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Estatísticas da Sessão:", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• Blocos Minerados: ${uiState.blocksMinedCount}", color = Color.White, fontSize = 11.sp)
                Text("• Blocos Posicionados: ${uiState.blocksPlacedCount}", color = Color.White, fontSize = 11.sp)
                Text("• Estruturas Construídas: ${uiState.structuresBuiltCount}", color = Color.White, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun ModalHeader(title: String, subtitle: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, color = Color(0xFF00E5FF), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = Color.LightGray, fontSize = 11.sp)
        }
        IconButton(onClick = onClose) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
        }
    }
}
