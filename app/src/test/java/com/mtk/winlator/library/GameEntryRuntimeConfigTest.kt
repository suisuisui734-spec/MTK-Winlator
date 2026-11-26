package com.mtk.winlator.library

import com.mtk.winlator.runtime.RuntimeContainerConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEntryRuntimeConfigTest {
    @Test
    fun mapsSavedPreferencesIntoValidDeclarativeConfig() {
        val entry = GameEntry(
            id = "content://games/one",
            title = "Example Game",
            executableUri = "content://games/one.exe",
            graphicsBackend = "VULKAN",
            resolutionWidth = 1600,
            resolutionHeight = 900,
            memoryLimitPercent = 90
        )

        val config = entry.toRuntimeContainerConfig()

        assertEquals(RuntimeContainerConfig.Validation.Valid, config.validate())
        assertEquals(RuntimeContainerConfig.GraphicsBackend.VULKAN, config.graphicsBackend)
        assertEquals(RuntimeContainerConfig.Resolution(1600, 900), config.resolution)
        assertEquals(90, config.memoryLimitPercent)
        assertEquals("Example Game", config.displayName)
    }

    @Test
    fun createsSafeStableIdsFromUriLikeLibraryIds() {
        val entry = GameEntry("content://provider/game?id=1", "Game", "content://provider/game.exe")
        val first = entry.toRuntimeContainerConfig()
        val second = entry.toRuntimeContainerConfig()

        assertEquals(first.id, second.id)
        assertTrue(first.id.matches(Regex("^[a-zA-Z0-9][a-zA-Z0-9_-]{0,47}$")))
        assertEquals(RuntimeContainerConfig.Validation.Valid, first.validate())
    }

    @Test
    fun differentEntriesReceiveDifferentContainerIds() {
        val first = GameEntry("one", "One", "content://one.exe").toRuntimeContainerConfig()
        val second = GameEntry("two", "Two", "content://two.exe").toRuntimeContainerConfig()

        assertNotEquals(first.id, second.id)
    }

    @Test
    fun fallsBackToSupportedDefaultsForInvalidSavedValues() {
        val config = GameEntry(
            id = "legacy",
            title = " ",
            executableUri = "content://legacy.exe",
            graphicsBackend = "UNKNOWN",
            resolutionWidth = 1234,
            resolutionHeight = 567,
            memoryLimitPercent = 99
        ).toRuntimeContainerConfig()

        assertEquals("Game", config.displayName)
        assertEquals(RuntimeContainerConfig.GraphicsBackend.AUTO, config.graphicsBackend)
        assertEquals(RuntimeContainerConfig.Resolution(1280, 720), config.resolution)
        assertEquals(75, config.memoryLimitPercent)
        assertEquals(RuntimeContainerConfig.Validation.Valid, config.validate())
    }
}
