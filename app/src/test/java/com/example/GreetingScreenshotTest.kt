package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.game.core.ItemStack
import com.example.game.ui.BottomHotbar
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GameScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun hotbar_screenshot() {
        val sampleSlots = listOf(
            ItemStack("tool_wood_pickaxe", 1),
            ItemStack("block_wood_oak", 32),
            ItemStack("block_cobblestone", 64),
            null, null, null, null, null, null
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                BottomHotbar(
                    slots = sampleSlots,
                    selectedIndex = 0,
                    onSelectSlot = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/hotbar.png")
    }
}
