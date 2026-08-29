package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.core.BlockType
import com.example.game.core.Vector3f
import com.example.game.world.StructureBlueprints
import com.example.game.world.WorldMap
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("BlockCraft 3D", appName)
    }

    @Test
    fun `test world generation and terrain height`() {
        val world = WorldMap(32, 24, 32, seed = 12345L)
        val y = world.getHighestSolidBlockY(16, 16)
        assertTrue("Terrain should have solid ground", y > 0)
        assertEquals(BlockType.BEDROCK, world.getBlock(16, 0, 16))
    }

    @Test
    fun `test structure blueprints and placement`() {
        val world = WorldMap(32, 24, 32, seed = 9999L)
        val castle = StructureBlueprints.MEDIEVAL_CASTLE
        assertNotNull(castle)
        assertTrue(castle.blocks.isNotEmpty())

        val placedCount = world.placeBlueprint(castle, 5, 5, 5)
        assertTrue(placedCount > 0)
    }

    @Test
    fun `test 3D raycast finds terrain block`() {
        val world = WorldMap(32, 24, 32, seed = 42L)
        world.setBlock(10, 10, 10, BlockType.STONE_BRICK)

        val rayOrigin = Vector3f(10.5f, 15.0f, 10.5f)
        val rayDir = Vector3f(0.0f, -1.0f, 0.0f) // Looking directly down
        val hit = world.raycast(rayOrigin, rayDir, maxDistance = 10f)

        assertNotNull("Raycast should hit block directly below", hit)
        assertEquals(10, hit?.blockPos?.x)
        assertEquals(10, hit?.blockPos?.y)
        assertEquals(10, hit?.blockPos?.z)
    }
}
