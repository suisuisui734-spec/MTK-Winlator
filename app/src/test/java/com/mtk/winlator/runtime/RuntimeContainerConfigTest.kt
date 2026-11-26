package com.mtk.winlator.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeContainerConfigTest {
    @Test
    fun acceptsDefaultArm64ContainerPreferences() {
        assertEquals(RuntimeContainerConfig.Validation.Valid, RuntimeContainerConfig("main", "Default").validate())
    }

    @Test
    fun rejectsPathLikeOrOverlongIds() {
        assertTrue(RuntimeContainerConfig("../outside", "Bad").validate() is RuntimeContainerConfig.Validation.Invalid)
        assertTrue(RuntimeContainerConfig("a".repeat(49), "Bad").validate() is RuntimeContainerConfig.Validation.Invalid)
    }

    @Test
    fun rejectsUnsupportedArchitecture() {
        val config = RuntimeContainerConfig("main", "Default", architecture = "x86")
        assertTrue(config.validate() is RuntimeContainerConfig.Validation.Invalid)
    }

    @Test
    fun rejectsOutOfRangeResolutionAndMemoryLimit() {
        assertTrue(RuntimeContainerConfig("main", "Default", resolution = RuntimeContainerConfig.Resolution(100, 100)).validate() is RuntimeContainerConfig.Validation.Invalid)
        assertTrue(RuntimeContainerConfig("main", "Default", memoryLimitPercent = 99).validate() is RuntimeContainerConfig.Validation.Invalid)
    }

    @Test
    fun rejectsBlankOrLongDisplayName() {
        assertTrue(RuntimeContainerConfig("main", " ").validate() is RuntimeContainerConfig.Validation.Invalid)
        assertTrue(RuntimeContainerConfig("main", "x".repeat(65)).validate() is RuntimeContainerConfig.Validation.Invalid)
    }
}
