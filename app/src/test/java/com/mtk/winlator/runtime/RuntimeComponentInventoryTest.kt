package com.mtk.winlator.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RuntimeComponentInventoryTest {
    private fun manifest() = RuntimeManifest(
        id = "test-runtime",
        version = "1.0",
        components = listOf(
            RuntimeComponent("wine", "9.0", 10, "a".repeat(64), "https://example.org/wine.zip", "LGPL"),
            RuntimeComponent("box64", "0.3", 10, "b".repeat(64), "https://example.org/box64.zip", "MIT")
        )
    )

    private fun writeMarker(install: File, manifest: RuntimeManifest) {
        install.resolve("install-manifest.txt").writeText(
            "id=${manifest.id}\nversion=${manifest.version}\narchitecture=${manifest.architecture}\n" +
                manifest.components.joinToString("\n") {
                    "${it.id}=${it.version}:${it.sha256.lowercase()}"
                }
        )
    }

    @Test
    fun missingManifestAndRuntimeAreNotReady() {
        val root = createTempDir()
        try {
            val inventory = RuntimeComponentInventory(root)
            assertEquals(RuntimeInventoryState.NOT_CONFIGURED, inventory.inspect(null).state)
            assertEquals(RuntimeInventoryState.NOT_INSTALLED, inventory.inspect(manifest()).state)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun missingComponentIsIncomplete() {
        val root = createTempDir()
        try {
            val m = manifest()
            val install = root.resolve(m.id).apply { mkdirs() }
            writeMarker(install, m)
            install.resolve("wine").mkdirs()
            install.resolve("wine/runtime.bin").writeText("placeholder")
            assertEquals(
                RuntimeInventoryState.INCOMPLETE,
                RuntimeComponentInventory(root).inspect(m).state
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun presentFilesAreNeverReportedAsVerified() {
        val root = createTempDir()
        try {
            val m = manifest()
            val install = root.resolve(m.id).apply { mkdirs() }
            writeMarker(install, m)
            m.components.forEach { component ->
                install.resolve(component.id).mkdirs()
                install.resolve(component.id).resolve("placeholder.bin").writeText("not a real runtime")
            }
            val report = RuntimeComponentInventory(root).inspect(m)
            assertEquals(RuntimeInventoryState.FILES_PRESENT_UNVERIFIED, report.state)
            assertTrue(report.componentIds.containsAll(listOf("wine", "box64")))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun mismatchedMarkerIsRejected() {
        val root = createTempDir()
        try {
            val m = manifest()
            val install = root.resolve(m.id).apply { mkdirs() }
            writeMarker(install, m)
            install.resolve("install-manifest.txt").appendText("\nchanged=true")
            assertEquals(
                RuntimeInventoryState.INVALID_LAYOUT,
                RuntimeComponentInventory(root).inspect(m).state
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun duplicateManifestIdsAreRejected() {
        val m = manifest()
        val duplicate = m.copy(components = m.components + m.components.first())
        assertEquals(
            RuntimeInventoryState.INVALID_LAYOUT,
            RuntimeComponentInventory(createTempDir()).inspect(duplicate).state
        )
    }
}
