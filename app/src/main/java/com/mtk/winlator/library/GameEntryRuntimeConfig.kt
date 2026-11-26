package com.mtk.winlator.library

import com.mtk.winlator.runtime.RuntimeContainerConfig
import java.security.MessageDigest

/**
 * Converts saved launcher preferences into a validated declarative container
 * configuration. This is only a bridge model; it does not create or start a
 * container or apply settings to a Windows process.
 */
fun GameEntry.toRuntimeContainerConfig(): RuntimeContainerConfig {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest((id + "|" + executableUri).toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
    val safeTitle = title.trim().take(64).ifBlank { "Game" }
    val resolution = when (resolutionWidth to resolutionHeight) {
        960 to 540 -> RuntimeContainerConfig.Resolution(960, 540)
        1600 to 900 -> RuntimeContainerConfig.Resolution(1600, 900)
        else -> RuntimeContainerConfig.Resolution(1280, 720)
    }
    val backend = when (graphicsBackend.uppercase()) {
        "OPENGL" -> RuntimeContainerConfig.GraphicsBackend.OPENGL
        "VULKAN" -> RuntimeContainerConfig.GraphicsBackend.VULKAN
        else -> RuntimeContainerConfig.GraphicsBackend.AUTO
    }
    val memory = memoryLimitPercent.takeIf { it in setOf(50, 75, 90) } ?: 75

    return RuntimeContainerConfig(
        id = "game-${digest.take(24)}",
        displayName = safeTitle,
        architecture = "arm64",
        graphicsBackend = backend,
        resolution = resolution,
        memoryLimitPercent = memory
    )
}
