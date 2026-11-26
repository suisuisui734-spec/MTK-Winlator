package com.mtk.winlator.library

import com.mtk.winlator.runtime.RuntimeContainerConfig
import com.mtk.winlator.runtime.RuntimeContainerRegistry
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameContainerConfigSynchronizerTest {
    private lateinit var root: java.io.File
    private lateinit var registry: RuntimeContainerRegistry
    private lateinit var synchronizer: GameContainerConfigSynchronizer

    @Before
    fun setUp() {
        root = Files.createTempDirectory("mtk-game-sync").toFile()
        registry = RuntimeContainerRegistry(root)
        synchronizer = GameContainerConfigSynchronizer(registry)
    }

    @After
    fun tearDown() {
        root.walkBottomUp().forEach { it.delete() }
    }

    @Test
    fun savesAllGamesAndRemovesStaleGameMetadata() {
        val oldGame = GameEntry("old", "Old", "content://old.exe")
            .toRuntimeContainerConfig()
        val retained = GameEntry("retained", "Retained", "content://retained.exe")
        registry.save(oldGame)
        registry.save(RuntimeContainerConfig("manual-container", "Manual"))

        val result = synchronizer.synchronize(listOf(retained))

        assertTrue(result.successful)
        assertEquals(1, result.savedCount)
        assertEquals(1, result.removedCount)
        assertEquals(listOf("game-" + retained.toRuntimeContainerConfig().id.removePrefix("game-"),
            "manual-container"), registry.list().map { it.id }.sorted())
        assertEquals("Retained", registry.get(retained.toRuntimeContainerConfig().id)?.displayName)
    }

    @Test
    fun updatesSavedPreferencesOnSubsequentSynchronization() {
        val initial = GameEntry("one", "Game", "content://one.exe")
        val changed = initial.copy(
            graphicsBackend = "OPENGL",
            resolutionWidth = 960,
            resolutionHeight = 540,
            memoryLimitPercent = 50
        )

        synchronizer.synchronize(listOf(initial))
        val result = synchronizer.synchronize(listOf(changed))
        val stored = registry.get(changed.toRuntimeContainerConfig().id)

        assertTrue(result.successful)
        assertEquals(RuntimeContainerConfig.GraphicsBackend.OPENGL, stored?.graphicsBackend)
        assertEquals(RuntimeContainerConfig.Resolution(960, 540), stored?.resolution)
        assertEquals(50, stored?.memoryLimitPercent)
        assertEquals(1, registry.list().count { it.id.startsWith("game-") })
    }

    @Test
    fun emptyLibraryRemovesOnlyManagedGameMetadata() {
        registry.save(GameEntry("one", "Game", "content://one.exe").toRuntimeContainerConfig())
        registry.save(RuntimeContainerConfig("manual", "Manual"))

        val result = synchronizer.synchronize(emptyList())

        assertTrue(result.successful)
        assertEquals(1, result.removedCount)
        assertEquals(listOf("manual"), registry.list().map { it.id })
    }
}
