package com.mtk.winlator.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuntimeArchivePathGuardTest {
    @Test
    fun acceptsNormalNestedPathsAndExplicitParentDirectory() {
        val guard = RuntimeArchivePathGuard()
        assertEquals("bin/wine", guard.accept("bin/wine", false))
        assertEquals("bin", guard.accept("bin/", true))
    }

    @Test
    fun rejectsTraversalAbsoluteWindowsAndMalformedPaths() {
        val guard = RuntimeArchivePathGuard()
        assertNull(guard.accept("../escape", false))
        assertNull(guard.accept("bin/../../escape", false))
        assertNull(guard.accept("/absolute", false))
        assertNull(guard.accept("C:/runtime.exe", false))
        assertNull(guard.accept("bin\\runtime.exe", false))
        assertNull(guard.accept("bin//runtime.exe", false))
    }

    @Test
    fun rejectsDuplicateAndConflictingEntries() {
        val guard = RuntimeArchivePathGuard()
        assertEquals("bin/wine", guard.accept("bin/wine", false))
        assertNull(guard.accept("bin/wine", false))
        assertNull(guard.accept("bin/wine", true))

        val fileFirst = RuntimeArchivePathGuard()
        assertEquals("bin", fileFirst.accept("bin", false))
        assertNull(fileFirst.accept("bin/wine", false))

        val childFirst = RuntimeArchivePathGuard()
        assertEquals("bin/wine", childFirst.accept("bin/wine", false))
        assertNull(childFirst.accept("bin", false))
    }
}
