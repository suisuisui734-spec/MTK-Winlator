package com.mtk.winlator.runtime

import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RuntimeContainerRegistryTest {
    private lateinit var root: java.io.File
    private lateinit var registry: RuntimeContainerRegistry

    @Before
    fun setUp() {
        root = Files.createTempDirectory("mtk-container-registry").toFile()
        registry = RuntimeContainerRegistry(root)
    }

    @After
    fun tearDown() {
        root.walkBottomUp().forEach { it.delete() }
    }

    @Test
    fun savesAndReloadsAllContainerPreferences() {
        val config = RuntimeContainerConfig(
            id = "game_123",
            displayName = "Example Game",
            graphicsBackend = RuntimeContainerConfig.GraphicsBackend.OPENGL,
            resolution = RuntimeContainerConfig.Resolution(1600, 900),
            memoryLimitPercent = 90
        )

        assertEquals(RuntimeContainerRegistry.SaveResult.Saved, registry.save(config))
        assertEquals(config, registry.get("game_123"))
    }

    @Test
    fun listsEntriesInStableIdOrder() {
        val second = RuntimeContainerConfig("z-game", "Z")
        val first = RuntimeContainerConfig("a-game", "A")

        registry.save(second)
        registry.save(first)

        assertEquals(listOf("a-game", "z-game"), registry.list().map { it.id })
    }

    @Test
    fun rejectsInvalidConfigurationAndUnsafeIds() {
        val invalid = RuntimeContainerConfig("../outside", "Bad")
        assertTrue(registry.save(invalid) is RuntimeContainerRegistry.SaveResult.Rejected)
        assertNull(registry.get("../outside"))
        assertFalse(registry.delete("../outside"))
    }

    @Test
    fun deletesOnlyRequestedMetadataEntry() {
        registry.save(RuntimeContainerConfig("first", "First"))
        registry.save(RuntimeContainerConfig("second", "Second"))

        assertTrue(registry.delete("first"))
        assertNull(registry.get("first"))
        assertEquals(listOf("second"), registry.list().map { it.id })
    }

    @Test
    fun ignoresMetadataWithUnknownSchemaVersion() {
        registry.save(RuntimeContainerConfig("legacy", "Legacy"))
        val file = java.io.File(root, "legacy.properties")
        file.writeText(file.readText().replace("schema_version=1", "schema_version=999"))

        assertNull(registry.get("legacy"))
        assertTrue(registry.list().isEmpty())
    }

    @Test
    fun overwritesExistingMetadataAtomically() {
        val original = RuntimeContainerConfig("same", "Original")
        val updated = original.copy(displayName = "Updated", memoryLimitPercent = 50)

        assertEquals(RuntimeContainerRegistry.SaveResult.Saved, registry.save(original))
        assertEquals(RuntimeContainerRegistry.SaveResult.Saved, registry.save(updated))
        assertEquals(updated, registry.get("same"))
        assertEquals(1, registry.list().size)
    }
}
