package com.mtk.winlator.runtime

import android.content.Context
import android.content.pm.PackageManager

/** Reads Android-advertised graphics features; does not create a GL/Vulkan context. */
object AndroidGraphicsCapabilities {
    fun read(context: Context, glEsVersion: String?, gpuVendor: String?, gpuRenderer: String?): RuntimeGraphicsCapabilities {
        val features = context.packageManager.systemAvailableFeatures.orEmpty()
        val level = features.firstOrNull {
            it.name == PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL
        }?.version
        val version = features.firstOrNull {
            it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION
        }?.version

        return RuntimeGraphicsCapabilities(
            glEsVersion = glEsVersion,
            gpuVendor = gpuVendor,
            gpuRenderer = gpuRenderer,
            vulkanHardwareLevel = level,
            vulkanVersion = version
        )
    }
}
