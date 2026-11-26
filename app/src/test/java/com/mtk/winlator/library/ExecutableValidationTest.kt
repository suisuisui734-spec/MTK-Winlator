package com.mtk.winlator.library

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExecutableValidationTest {
    @Test
    fun acceptsExeExtensionCaseInsensitively() {
        assertTrue(isWindowsExecutableName("game.exe"))
        assertTrue(isWindowsExecutableName("GAME.EXE"))
        assertTrue(isWindowsExecutableName("  setup.Exe  "))
    }

    @Test
    fun rejectsMissingOrDifferentExtensions() {
        assertFalse(isWindowsExecutableName(null))
        assertFalse(isWindowsExecutableName(""))
        assertFalse(isWindowsExecutableName("game"))
        assertFalse(isWindowsExecutableName("game.exe.txt"))
        assertFalse(isWindowsExecutableName(".exe"))
    }
}
