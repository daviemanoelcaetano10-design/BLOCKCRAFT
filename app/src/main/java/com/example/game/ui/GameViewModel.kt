package com.example.game.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.game.audio.GameAudioEngine
import com.example.game.core.BlockFace
import com.example.game.core.BlockType
import com.example.game.core.CraftingRecipe
import com.example.game.core.ItemCategory
import com.example.game.core.ItemRegistry
import com.example.game.core.ItemStack
import com.example.game.core.RaycastHit
import com.example.game.core.ToolType
import com.example.game.core.Vector3f
import com.example.game.core.Vector3i
import com.example.game.data.GameDatabase
import com.example.game.data.GameRepository
import com.example.game.data.WorldEntity
import com.example.game.engine.GameRenderer
import com.example.game.world.StructureBlueprint
import com.example.game.world.StructureBlueprints
import com.example.game.world.WorldMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.*
import kotlin.random.Random

enum class GameMode {
    SURVIVAL, CREATIVE
}

enum class BuildToolMode {
    SINGLE_BLOCK,
    LINE_WALL,
    FLOOR_AREA,
    HOLLOW_BOX
}

data class GameUiState(
    val currentWorldId: Long = 0L,
    val worldName: String = "Mundo Aberto 1",
    val gameMode: GameMode = GameMode.SURVIVAL,
    val health: Int = 100,
    val maxHealth: Int = 100,
    val stamina: Int = 100,
    val isFlying: Boolean = false,
    val isRunning: Boolean = false,
    val targetBlock: BlockType? = null,
    val targetPos: Vector3i? = null,
    val targetDistance: Float = 0f,
    val miningProgress: Float = 0f,
    val isMining: Boolean = false,
    val hotbarSlots: List<ItemStack?> = List(9) { null },
    val selectedHotbarIndex: Int = 0,
    val inventory: List<ItemStack> = emptyList(),
    val activeBlueprint: StructureBlueprint? = null,
    val isHologramActive: Boolean = false,
    val buildToolMode: BuildToolMode = BuildToolMode.SINGLE_BLOCK,
    val macroAnchor1: Vector3i? = null,
    val macroAnchor2: Vector3i? = null,
    val timeOfDay: Float = 0.35f,
    val playerCoordinates: Vector3i = Vector3i(24, 15, 24),
    val cardinalDirection: String = "Norte",
    val fps: Int = 60,
    val blocksMinedCount: Int = 0,
    val blocksPlacedCount: Int = 0,
    val structuresBuiltCount: Int = 0,
    val activeModal: ActiveModal? = null,
    val bannerMessage: String? = null
)

enum class ActiveModal {
    INVENTORY,
    CRAFTING,
    BLUEPRINTS,
    BUILDER_TOOLS,
    WORLDS_MANAGER,
    SETTINGS
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GameRepository(GameDatabase.getDatabase(application).gameDao())

    val savedWorlds: StateFlow<List<WorldEntity>> = repository.allWorlds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val renderer = GameRenderer(application)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // Physics & Movement input state
    var joystickX = 0f
    var joystickY = 0f
    private var verticalVelocity = 0f
    private var isGrounded = false

    private var gameLoopJob: Job? = null
    private var miningJob: Job? = null

    // Macro builder anchor
    private var firstAnchorPos: Vector3i? = null

    init {
        initDefaultWorld()
        startGameLoop()
    }

    private fun initDefaultWorld() {
        val defaultWorld = WorldMap(48, 28, 48, seed = System.currentTimeMillis())
        renderer.worldMap = defaultWorld

        // Setup starting player position on terrain top
        val spawnX = 24
        val spawnZ = 24
        val spawnY = defaultWorld.getHighestSolidBlockY(spawnX, spawnZ) + 1
        renderer.playerPos.set(spawnX + 0.5f, spawnY.toFloat(), spawnZ + 0.5f)

        // Starting Hotbar & Inventory
        val startingHotbar = mutableListOf<ItemStack?>()
        startingHotbar.add(ItemStack("tool_wood_pickaxe", 1))
        startingHotbar.add(ItemStack("tool_wood_axe", 1))
        startingHotbar.add(ItemStack("block_wood_oak", 32))
        startingHotbar.add(ItemStack("block_wood_plank", 48))
        startingHotbar.add(ItemStack("block_cobblestone", 64))
        startingHotbar.add(ItemStack("block_torch", 16))
        startingHotbar.add(ItemStack("tool_builder_wand", 1))
        startingHotbar.add(ItemStack("block_glass", 20))
        startingHotbar.add(null)

        val startingInventory = mutableListOf<ItemStack>()
        startingInventory.addAll(startingHotbar.filterNotNull())
        startingInventory.add(ItemStack("block_dirt", 32))
        startingInventory.add(ItemStack("block_sand", 16))
        startingInventory.add(ItemStack("coal", 8))
        startingInventory.add(ItemStack("stick", 12))

        _uiState.value = _uiState.value.copy(
            hotbarSlots = startingHotbar,
            inventory = startingInventory,
            selectedHotbarIndex = 0
        )
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var lastTime = System.nanoTime()
            var stepTimer = 0f

            while (isActive) {
                val now = System.nanoTime()
                val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastTime = now

                updatePlayerPhysics(dt)
                updateRaycastTarget()

                // Step sound when moving on ground
                if ((abs(joystickX) > 0.2f || abs(joystickY) > 0.2f) && isGrounded && !_uiState.value.isFlying) {
                    stepTimer += dt
                    if (stepTimer >= (if (_uiState.value.isRunning) 0.3f else 0.45f)) {
                        GameAudioEngine.playFootstep()
                        stepTimer = 0f
                    }
                }

                // Update UI state with player coordinates & cardinal heading
                val px = renderer.playerPos.x.toInt()
                val py = renderer.playerPos.y.toInt()
                val pz = renderer.playerPos.z.toInt()

                val yawNorm = ((renderer.playerYaw % 360f) + 360f) % 360f
                val cardinal = when {
                    yawNorm in 315f..360f || yawNorm in 0f..45f -> "Sul"
                    yawNorm in 45f..135f -> "Oeste"
                    yawNorm in 135f..225f -> "Norte"
                    else -> "Leste"
                }

                _uiState.value = _uiState.value.copy(
                    playerCoordinates = Vector3i(px, py, pz),
                    cardinalDirection = cardinal,
                    timeOfDay = renderer.timeOfDay,
                    fps = renderer.fps
                )

                delay(16) // ~60 ticks per second
            }
        }
    }

    private fun updatePlayerPhysics(dt: Float) {
        val isFlying = _uiState.value.isFlying
        val isRunning = _uiState.value.isRunning
        val speed = if (isFlying) 12.0f else (if (isRunning) 6.5f else 4.2f)

        // Calculate move vector based on camera Yaw
        val yawRad = Math.toRadians(renderer.playerYaw.toDouble()).toFloat()
        val forwardX = sin(yawRad)
        val forwardZ = -cos(yawRad)
        val rightX = cos(yawRad)
        val rightZ = sin(yawRad)

        // Movement from joystick
        val moveX = (forwardX * joystickY + rightX * joystickX) * speed * dt
        val moveZ = (forwardZ * joystickY + rightZ * joystickX) * speed * dt

        val world = renderer.worldMap
        val playerRadius = 0.35f
        val playerHeight = 1.8f

        // Horizontal Movement & Voxel AABB Collision
        val newX = renderer.playerPos.x + moveX
        if (!checkBlockCollision(newX, renderer.playerPos.y, renderer.playerPos.z, playerRadius, playerHeight)) {
            renderer.playerPos.x = newX
        }

        val newZ = renderer.playerPos.z + moveZ
        if (!checkBlockCollision(renderer.playerPos.x, renderer.playerPos.y, newZ, playerRadius, playerHeight)) {
            renderer.playerPos.z = newZ
        }

        // Vertical Movement (Gravity or Flying)
        if (isFlying) {
            verticalVelocity = 0f
            isGrounded = false
        } else {
            // Apply Gravity
            verticalVelocity -= 22f * dt
            val newY = renderer.playerPos.y + verticalVelocity * dt

            if (verticalVelocity < 0) {
                // Falling down: check floor collision
                val checkY = newY
                if (checkBlockCollision(renderer.playerPos.x, checkY, renderer.playerPos.z, playerRadius, 0.1f)) {
                    // Landed on floor
                    renderer.playerPos.y = floor(checkY) + 1.0f
                    verticalVelocity = 0f
                    isGrounded = true
                } else {
                    renderer.playerPos.y = newY
                    isGrounded = false
                }
            } else if (verticalVelocity > 0) {
                // Jumping up: check ceiling collision
                val checkY = newY + playerHeight
                if (checkBlockCollision(renderer.playerPos.x, checkY, renderer.playerPos.z, playerRadius, 0.1f)) {
                    verticalVelocity = 0f
                } else {
                    renderer.playerPos.y = newY
                    isGrounded = false
                }
            }
        }

        // Clamp player inside world boundaries
        renderer.playerPos.x = renderer.playerPos.x.coerceIn(1f, world.sizeX - 1.5f)
        renderer.playerPos.y = renderer.playerPos.y.coerceIn(1f, world.sizeY - 2.5f)
        renderer.playerPos.z = renderer.playerPos.z.coerceIn(1f, world.sizeZ - 1.5f)
    }

    private fun checkBlockCollision(
        px: Float, py: Float, pz: Float,
        radius: Float, height: Float
    ): Boolean {
        val world = renderer.worldMap
        val minX = floor(px - radius).toInt()
        val maxX = floor(px + radius).toInt()
        val minY = floor(py).toInt()
        val maxY = floor(py + height).toInt()
        val minZ = floor(pz - radius).toInt()
        val maxZ = floor(pz + radius).toInt()

        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    val block = world.getBlock(x, y, z)
                    if (block.isSolid) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun updateRaycastTarget() {
        val eyePos = Vector3f(
            renderer.playerPos.x,
            renderer.playerPos.y + renderer.eyeHeight,
            renderer.playerPos.z
        )
        val lookDir = renderer.getCameraLookVector()

        val hit = renderer.worldMap.raycast(eyePos, lookDir, maxDistance = 6.5f)
        renderer.currentTargetHit = hit

        if (hit != null) {
            _uiState.value = _uiState.value.copy(
                targetBlock = hit.blockType,
                targetPos = hit.blockPos,
                targetDistance = hit.distance
            )

            // If blueprint hologram preview is enabled, update origin
            if (_uiState.value.isHologramActive && _uiState.value.activeBlueprint != null) {
                renderer.blueprintOrigin = hit.placePos
            }
        } else {
            _uiState.value = _uiState.value.copy(
                targetBlock = null,
                targetPos = null,
                targetDistance = 0f
            )
        }
    }

    // Touch Look (Yaw/Pitch)
    fun onLookDelta(deltaX: Float, deltaY: Float) {
        val sensitivity = 0.22f
        renderer.playerYaw += deltaX * sensitivity
        renderer.playerPitch = (renderer.playerPitch - deltaY * sensitivity).coerceIn(-89f, 89f)
    }

    // Jump / Fly Up Action
    fun onJump() {
        if (_uiState.value.isFlying) {
            renderer.playerPos.y += 0.8f
        } else if (isGrounded) {
            verticalVelocity = 7.5f
            isGrounded = false
            GameAudioEngine.playJump()
        }
    }

    // Fly Down Action
    fun onFlyDown() {
        if (_uiState.value.isFlying) {
            renderer.playerPos.y = (renderer.playerPos.y - 0.8f).coerceAtLeast(1f)
        }
    }

    // Toggle Fly / Walk Mode
    fun toggleFlyMode() {
        val next = !_uiState.value.isFlying
        _uiState.value = _uiState.value.copy(isFlying = next)
        showBanner(if (next) "Modo Voo Ativado" else "Modo Terrestre Ativado")
    }

    // Toggle Run Sprint
    fun toggleRunning() {
        val next = !_uiState.value.isRunning
        _uiState.value = _uiState.value.copy(isRunning = next)
    }

    // Select Hotbar Slot (0..8)
    fun selectHotbarSlot(index: Int) {
        if (index in 0..8) {
            _uiState.value = _uiState.value.copy(selectedHotbarIndex = index)
        }
    }

    // Mining / Block Breaking
    fun startMining() {
        val hit = renderer.currentTargetHit ?: return
        if (miningJob?.isActive == true) return

        _uiState.value = _uiState.value.copy(isMining = true)

        miningJob = viewModelScope.launch {
            val block = hit.blockType
            val currentTool = getEquippedTool()
            val efficiency = calculateToolEfficiency(block, currentTool)

            val totalTime = if (_uiState.value.gameMode == GameMode.CREATIVE) 0.05f else (block.hardness / efficiency)
            var elapsed = 0f

            while (elapsed < totalTime && isActive && _uiState.value.isMining) {
                // Swing tool animation
                renderer.toolSwingProgress = (renderer.toolSwingProgress + 0.2f) % 1.0f
                renderer.miningProgress = (elapsed / totalTime).coerceIn(0f, 1f)
                _uiState.value = _uiState.value.copy(miningProgress = renderer.miningProgress)

                GameAudioEngine.playMineHit()
                delay(120)
                elapsed += 0.12f
            }

            if (_uiState.value.isMining) {
                // Complete mining block
                breakTargetBlock(hit)
            }

            renderer.miningProgress = 0f
            renderer.toolSwingProgress = 0f
            _uiState.value = _uiState.value.copy(isMining = false, miningProgress = 0f)
        }
    }

    fun stopMining() {
        miningJob?.cancel()
        renderer.miningProgress = 0f
        renderer.toolSwingProgress = 0f
        _uiState.value = _uiState.value.copy(isMining = false, miningProgress = 0f)
    }

    private fun breakTargetBlock(hit: RaycastHit) {
        val pos = hit.blockPos
        val block = hit.blockType

        // Set to AIR
        renderer.worldMap.setBlock(pos.x, pos.y, pos.z, BlockType.AIR)
        renderer.spawnBlockParticles(pos, block)
        GameAudioEngine.playBlockBreak()

        // Drop item in survival mode
        if (_uiState.value.gameMode == GameMode.SURVIVAL) {
            val dropItemId = getDropItemForBlock(block)
            if (dropItemId != null) {
                addItemToInventory(dropItemId, 1)
            }
        }

        _uiState.value = _uiState.value.copy(
            blocksMinedCount = _uiState.value.blocksMinedCount + 1
        )
    }

    private fun getDropItemForBlock(block: BlockType): String? {
        return when (block) {
            BlockType.GRASS -> "block_dirt"
            BlockType.STONE -> "block_cobblestone"
            BlockType.COAL_ORE -> "coal"
            BlockType.IRON_ORE -> "iron_ingot"
            BlockType.GOLD_ORE -> "gold_ingot"
            BlockType.DIAMOND_ORE -> "diamond"
            BlockType.REDSTONE_ORE -> "energy_crystal"
            BlockType.LEAVES -> if (Random.nextFloat() < 0.2f) "stick" else null
            else -> ItemRegistry.getBlockItem(block)?.id
        }
    }

    private fun calculateToolEfficiency(block: BlockType, tool: ToolType?): Float {
        if (tool == null) return 1.0f
        if (tool.efficiency != null && block.soundType == tool.efficiency) {
            return tool.multiplier
        }
        return if (tool == ToolType.DIAMOND_PICKAXE) 5.0f else 1.0f
    }

    private fun getEquippedTool(): ToolType? {
        val selected = getSelectedHotbarItem() ?: return null
        val item = ItemRegistry.getItem(selected.itemId) ?: return null
        return item.toolType
    }

    private fun getSelectedHotbarItem(): ItemStack? {
        val idx = _uiState.value.selectedHotbarIndex
        return _uiState.value.hotbarSlots.getOrNull(idx)
    }

    // Place Block / Macro Build Action
    fun onPlaceAction() {
        val hit = renderer.currentTargetHit ?: return
        val selectedStack = getSelectedHotbarItem() ?: return
        val item = ItemRegistry.getItem(selectedStack.itemId) ?: return

        // Swing tool
        renderer.toolSwingProgress = 1.0f

        // Check if equipped with Builder Wand (Macro Tool)
        if (item.toolType == ToolType.BUILDER_WAND) {
            handleMacroBuilder(hit.placePos)
            return
        }

        // Regular block placement
        val blockType = item.blockType ?: return
        val placePos = hit.placePos

        if (!renderer.worldMap.isValid(placePos.x, placePos.y, placePos.z)) return

        // Prevent placing inside player body
        val distToPlayer = Vector3f(
            placePos.x + 0.5f,
            placePos.y + 0.5f,
            placePos.z + 0.5f
        ).distanceTo(renderer.playerPos)

        if (distToPlayer < 0.8f && blockType.isSolid) {
            showBanner("Espaço ocupado pelo jogador!")
            return
        }

        // Place block in world
        renderer.worldMap.setBlock(placePos.x, placePos.y, placePos.z, blockType)
        GameAudioEngine.playBlockPlace()

        // Consume 1 in survival
        if (_uiState.value.gameMode == GameMode.SURVIVAL) {
            consumeItemFromHotbar(_uiState.value.selectedHotbarIndex, 1)
        }

        _uiState.value = _uiState.value.copy(
            blocksPlacedCount = _uiState.value.blocksPlacedCount + 1
        )
    }

    private fun handleMacroBuilder(pos: Vector3i) {
        if (firstAnchorPos == null) {
            firstAnchorPos = pos
            _uiState.value = _uiState.value.copy(macroAnchor1 = pos)
            GameAudioEngine.playCraftSuccess()
            showBanner("Ponto 1 definido em (${pos.x}, ${pos.y}, ${pos.z}). Clique no Ponto 2 para construir!")
        } else {
            val p1 = firstAnchorPos!!
            val p2 = pos
            firstAnchorPos = null
            _uiState.value = _uiState.value.copy(macroAnchor1 = null, macroAnchor2 = null)

            val buildBlock = BlockType.STONE_BRICK
            val count = when (_uiState.value.buildToolMode) {
                BuildToolMode.LINE_WALL -> renderer.worldMap.fillVolume(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, buildBlock)
                BuildToolMode.FLOOR_AREA -> renderer.worldMap.fillVolume(p1.x, p1.y, p1.z, p2.x, p1.y, p2.z, BlockType.DARK_TILES)
                BuildToolMode.HOLLOW_BOX -> renderer.worldMap.fillVolume(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, BlockType.WOOD_PLANK)
                else -> renderer.worldMap.fillVolume(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, buildBlock)
            }

            GameAudioEngine.playStructureBuild()
            showBanner("Construção Macro concluída: $count blocos inseridos!")
            _uiState.value = _uiState.value.copy(
                blocksPlacedCount = _uiState.value.blocksPlacedCount + count
            )
        }
    }

    fun setBuildToolMode(mode: BuildToolMode) {
        _uiState.value = _uiState.value.copy(buildToolMode = mode)
        firstAnchorPos = null
    }

    // Complex Structure Blueprint Builder
    fun selectBlueprint(blueprint: StructureBlueprint) {
        _uiState.value = _uiState.value.copy(
            activeBlueprint = blueprint,
            isHologramActive = true
        )
        renderer.activeBlueprint = blueprint
        renderer.isHologramEnabled = true
        showBanner("Estrutura '${blueprint.name}' selecionada. Aponte no chão e clique em Construir!")
    }

    fun instantBuildActiveBlueprint() {
        val blueprint = _uiState.value.activeBlueprint ?: return
        val hit = renderer.currentTargetHit
        val origin = hit?.placePos ?: Vector3i(
            renderer.playerPos.x.toInt() + 2,
            renderer.worldMap.getHighestSolidBlockY(renderer.playerPos.x.toInt() + 2, renderer.playerPos.z.toInt()) + 1,
            renderer.playerPos.z.toInt()
        )

        // Check materials if survival mode
        if (_uiState.value.gameMode == GameMode.SURVIVAL) {
            val missing = checkMissingMaterials(blueprint)
            if (missing.isNotEmpty()) {
                showBanner("Faltam recursos: ${missing.entries.joinToString { "${it.key.displayName} (${it.value})" }}")
                return
            }
            // Consume materials
            blueprint.requiredMaterialsSummary.forEach { (block, count) ->
                val itemId = ItemRegistry.getBlockItem(block)?.id ?: return@forEach
                consumeItemFromInventory(itemId, count)
            }
        }

        val placed = renderer.worldMap.placeBlueprint(blueprint, origin.x, origin.y, origin.z)
        GameAudioEngine.playStructureBuild()

        _uiState.value = _uiState.value.copy(
            structuresBuiltCount = _uiState.value.structuresBuiltCount + 1,
            blocksPlacedCount = _uiState.value.blocksPlacedCount + placed,
            activeBlueprint = null,
            isHologramActive = false,
            activeModal = null
        )
        renderer.activeBlueprint = null
        renderer.isHologramEnabled = false

        showBanner("🎉 '${blueprint.name}' construído com sucesso ($placed blocos)!")
    }

    private fun checkMissingMaterials(blueprint: StructureBlueprint): Map<BlockType, Int> {
        val missing = mutableMapOf<BlockType, Int>()
        blueprint.requiredMaterialsSummary.forEach { (block, required) ->
            val itemId = ItemRegistry.getBlockItem(block)?.id
            val currentCount = _uiState.value.inventory.filter { it.itemId == itemId }.sumOf { it.count }
            if (currentCount < required) {
                missing[block] = required - currentCount
            }
        }
        return missing
    }

    // Crafting System
    fun craftRecipe(recipe: CraftingRecipe) {
        // In survival, check and consume ingredients
        if (_uiState.value.gameMode == GameMode.SURVIVAL) {
            // Check all ingredients available
            for (ing in recipe.ingredients) {
                val available = _uiState.value.inventory.filter { it.itemId == ing.itemId }.sumOf { it.count }
                if (available < ing.amount) {
                    showBanner("Recursos insuficientes para criar ${recipe.name}!")
                    return
                }
            }

            // Consume ingredients
            for (ing in recipe.ingredients) {
                consumeItemFromInventory(ing.itemId, ing.amount)
            }
        }

        // Add crafted output
        addItemToInventory(recipe.outputItemId, recipe.outputAmount)
        GameAudioEngine.playCraftSuccess()
        showBanner("Criado com sucesso: ${recipe.name} x${recipe.outputAmount}!")
    }

    // Inventory Helpers
    fun addItemToInventory(itemId: String, amount: Int) {
        val currentInv = _uiState.value.inventory.toMutableList()
        val existing = currentInv.find { it.itemId == itemId }
        if (existing != null) {
            existing.count += amount
        } else {
            currentInv.add(ItemStack(itemId, amount))
        }

        // Also populate hotbar if there's an empty slot
        val currentHotbar = _uiState.value.hotbarSlots.toMutableList()
        val hotbarExisting = currentHotbar.find { it?.itemId == itemId }
        if (hotbarExisting != null) {
            hotbarExisting.count += amount
        } else {
            val emptyIdx = currentHotbar.indexOfFirst { it == null }
            if (emptyIdx != -1) {
                currentHotbar[emptyIdx] = ItemStack(itemId, amount)
            }
        }

        _uiState.value = _uiState.value.copy(
            inventory = currentInv,
            hotbarSlots = currentHotbar
        )
    }

    private fun consumeItemFromInventory(itemId: String, amount: Int) {
        var remaining = amount
        val currentInv = _uiState.value.inventory.toMutableList()
        val it = currentInv.iterator()
        while (it.hasNext() && remaining > 0) {
            val stack = it.next()
            if (stack.itemId == itemId) {
                if (stack.count <= remaining) {
                    remaining -= stack.count
                    it.remove()
                } else {
                    stack.count -= remaining
                    remaining = 0
                }
            }
        }

        // Update hotbar
        val currentHotbar = _uiState.value.hotbarSlots.toMutableList()
        for (i in currentHotbar.indices) {
            val stack = currentHotbar[i]
            if (stack?.itemId == itemId) {
                val inInv = currentInv.find { it.itemId == itemId }?.count ?: 0
                if (inInv <= 0) {
                    currentHotbar[i] = null
                } else {
                    stack.count = inInv
                }
            }
        }

        _uiState.value = _uiState.value.copy(
            inventory = currentInv,
            hotbarSlots = currentHotbar
        )
    }

    private fun consumeItemFromHotbar(slotIndex: Int, amount: Int) {
        val currentHotbar = _uiState.value.hotbarSlots.toMutableList()
        val stack = currentHotbar.getOrNull(slotIndex) ?: return

        stack.count -= amount
        val itemId = stack.itemId

        if (stack.count <= 0) {
            currentHotbar[slotIndex] = null
        }

        // Sync with inventory
        val currentInv = _uiState.value.inventory.toMutableList()
        val invStack = currentInv.find { it.itemId == itemId }
        if (invStack != null) {
            invStack.count -= amount
            if (invStack.count <= 0) {
                currentInv.remove(invStack)
            }
        }

        _uiState.value = _uiState.value.copy(
            hotbarSlots = currentHotbar,
            inventory = currentInv
        )
    }

    // Modal Control
    fun openModal(modal: ActiveModal) {
        _uiState.value = _uiState.value.copy(activeModal = modal)
    }

    fun closeModal() {
        _uiState.value = _uiState.value.copy(activeModal = null)
    }

    fun setGameMode(mode: GameMode) {
        _uiState.value = _uiState.value.copy(gameMode = mode)
        showBanner("Modo alterado para: ${if (mode == GameMode.CREATIVE) "Criativo (Recursos Infinitos)" else "Sobrevivência"}")
    }

    fun setTimeOfDay(time: Float) {
        renderer.timeOfDay = time.coerceIn(0f, 1f)
    }

    // World Save & Load via Room Database
    fun saveCurrentWorld() {
        viewModelScope.launch {
            val state = _uiState.value
            val modifiedJson = JSONObject()
            renderer.worldMap.modifiedBlocks.forEach { (k, v) ->
                modifiedJson.put(k.toString(), v.toInt())
            }

            val invArray = JSONArray()
            state.inventory.forEach {
                val obj = JSONObject()
                obj.put("itemId", it.itemId)
                obj.put("count", it.count)
                invArray.put(obj)
            }

            val hotbarArray = JSONArray()
            state.hotbarSlots.forEach {
                val obj = JSONObject()
                if (it != null) {
                    obj.put("itemId", it.itemId)
                    obj.put("count", it.count)
                }
                hotbarArray.put(obj)
            }

            val entity = WorldEntity(
                id = state.currentWorldId,
                name = state.worldName,
                seed = renderer.worldMap.seed,
                gameMode = state.gameMode.name,
                timeOfDay = renderer.timeOfDay,
                playerX = renderer.playerPos.x,
                playerY = renderer.playerPos.y,
                playerZ = renderer.playerPos.z,
                playerYaw = renderer.playerYaw,
                playerPitch = renderer.playerPitch,
                modifiedBlocksData = modifiedJson.toString(),
                inventoryData = invArray.toString(),
                hotbarData = hotbarArray.toString(),
                selectedHotbarIndex = state.selectedHotbarIndex,
                blocksMined = state.blocksMinedCount,
                blocksPlaced = state.blocksPlacedCount,
                structuresCount = state.structuresBuiltCount,
                updatedAt = System.currentTimeMillis()
            )

            val id = repository.saveWorld(entity)
            _uiState.value = _uiState.value.copy(currentWorldId = id)
            showBanner("💾 Mundo salvo com sucesso no banco de dados!")
        }
    }

    fun loadWorld(world: WorldEntity) {
        viewModelScope.launch {
            val newMap = WorldMap(48, 28, 48, seed = world.seed)

            // Restore modified blocks
            val json = JSONObject(world.modifiedBlocksData)
            val mods = mutableMapOf<Int, Byte>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                mods[k.toInt()] = json.getInt(k).toByte()
            }
            newMap.applySavedModifications(mods)

            renderer.worldMap = newMap
            renderer.playerPos.set(world.playerX, world.playerY, world.playerZ)
            renderer.playerYaw = world.playerYaw
            renderer.playerPitch = world.playerPitch
            renderer.timeOfDay = world.timeOfDay

            // Restore inventory
            val inv = mutableListOf<ItemStack>()
            val invArray = JSONArray(world.inventoryData)
            for (i in 0 until invArray.length()) {
                val obj = invArray.getJSONObject(i)
                inv.add(ItemStack(obj.getString("itemId"), obj.getInt("count")))
            }

            // Restore hotbar
            val hotbar = mutableListOf<ItemStack?>()
            val hotbarArray = JSONArray(world.hotbarData)
            for (i in 0 until hotbarArray.length()) {
                val obj = hotbarArray.getJSONObject(i)
                if (obj.has("itemId")) {
                    hotbar.add(ItemStack(obj.getString("itemId"), obj.getInt("count")))
                } else {
                    hotbar.add(null)
                }
            }

            _uiState.value = _uiState.value.copy(
                currentWorldId = world.id,
                worldName = world.name,
                gameMode = GameMode.valueOf(world.gameMode),
                inventory = inv,
                hotbarSlots = hotbar,
                selectedHotbarIndex = world.selectedHotbarIndex,
                blocksMinedCount = world.blocksMined,
                blocksPlacedCount = world.blocksPlaced,
                structuresBuiltCount = world.structuresCount,
                activeModal = null
            )

            showBanner("Mundo '${world.name}' carregado!")
        }
    }

    fun createNewWorld(name: String, seed: Long = System.currentTimeMillis()) {
        val newWorld = WorldMap(48, 28, 48, seed = seed)
        renderer.worldMap = newWorld

        val spawnX = 24
        val spawnZ = 24
        val spawnY = newWorld.getHighestSolidBlockY(spawnX, spawnZ) + 1
        renderer.playerPos.set(spawnX + 0.5f, spawnY.toFloat(), spawnZ + 0.5f)

        _uiState.value = _uiState.value.copy(
            currentWorldId = 0L,
            worldName = name.ifBlank { "Novo Mundo Criativo" },
            blocksMinedCount = 0,
            blocksPlacedCount = 0,
            structuresBuiltCount = 0,
            activeModal = null
        )

        showBanner("Novo mundo gerado com semente $seed!")
    }

    fun deleteSavedWorld(id: Long) {
        viewModelScope.launch {
            repository.deleteWorld(id)
            showBanner("Mundo excluído do armazenamento.")
        }
    }

    private fun showBanner(msg: String) {
        _uiState.value = _uiState.value.copy(bannerMessage = msg)
        viewModelScope.launch {
            delay(3000)
            if (_uiState.value.bannerMessage == msg) {
                _uiState.value = _uiState.value.copy(bannerMessage = null)
            }
        }
    }
}
