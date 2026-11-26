package com.mtk.winlator.runtime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RuntimeFileIntegrityTest {
    @Test
    fun recordMatchesUnchangedFilesAndDetectsModifiedContent() {
        val root = createTempDir()
        try {
            val wine = root.resolve("wine").apply { mkdirs() }
            wine.resolve("runtime.bin").writeText("original runtime data")
            val record = RuntimeFileIntegrity.createRecord(root, listOf("wine"))
            assertNotNull(record)
            val recordFile = root.resolve("install-files.sha256").apply { writeText(record!!) }

            assertTrue(RuntimeFileIntegrity.matchesRecord(root, listOf("wine"), recordFile))
            wine.resolve("runtime.bin").writeText("modified runtime data")
            assertFalse(RuntimeFileIntegrity.matchesRecord(root, listOf("wine"), recordFile))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun recordDetectsMissingAndAdditionalFiles() {
        val root = createTempDir()
        try {
            val component = root.resolve("box64").apply { mkdirs() }
            val binary = component.resolve("box64").apply { writeText("binary placeholder") }
            val record = RuntimeFileIntegrity.createRecord(root, listOf("box64"))!!
            val recordFile = root.resolve("install-files.sha256").apply { writeText(record) }

            binary.delete()
            assertFalse(RuntimeFileIntegrity.matchesRecord(root, listOf("box64"), recordFile))
            binary.writeText("binary placeholder")
            component.resolve("unexpected.dat").writeText("extra")
            assertFalse(RuntimeFileIntegrity.matchesRecord(root, listOf("box64"), recordFile))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsMissingComponentAndUnsafeIdentifier() {
        val root = createTempDir()
        try {
            assertTrue(RuntimeFileIntegrity.createRecord(root, listOf("../outside")) == null)
            assertTrue(RuntimeFileIntegrity.createRecord(root, listOf("missing")) == null)
        } finally {
            root.deleteRecursively()
        }
    }
}
