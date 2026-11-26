package com.mtk.winlator.runtime

import org.junit.Assert.assertEquals
import org.junit.Test

class RuntimeAbiCompatibilityTest {
    @Test
    fun acceptsArm64DeviceWith64BitProcess() {
        assertEquals(
            RuntimeAbiCompatibility.Status.SUPPORTED,
            RuntimeAbiCompatibility.evaluate(listOf("arm64-v8a", "armeabi-v7a"), true)
        )
    }

    @Test
    fun rejectsDeviceWithoutArm64Abi() {
        assertEquals(
            RuntimeAbiCompatibility.Status.NO_ARM64_DEVICE_ABI,
            RuntimeAbiCompatibility.evaluate(listOf("armeabi-v7a"), true)
        )
    }

    @Test
    fun rejects32BitProcessEvenOnArm64CapableDevice() {
        assertEquals(
            RuntimeAbiCompatibility.Status.PROCESS_NOT_64_BIT,
            RuntimeAbiCompatibility.evaluate(listOf("arm64-v8a", "armeabi-v7a"), false)
        )
    }
}
