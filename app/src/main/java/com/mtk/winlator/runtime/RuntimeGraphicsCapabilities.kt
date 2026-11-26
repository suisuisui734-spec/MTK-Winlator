package com.mtk.winlator.runtime

/**
 * Conservative graphics capability model for future backend selection.
 * Feature presence is not proof that a particular Windows game will run.
 */
data class RuntimeGraphicsCapabilities(
    val glEsVersion: String?,
    val gpuVendor: String?,
    val gpuRenderer: String?,
    val vulkanHardwareLevel: Int?,
    val vulkanVersion: Int?
) {
    val hasOpenGles: Boolean
        get() = !glEsVersion.isNullOrBlank()

    val hasVulkan: Boolean
        get() = vulkanHardwareLevel != null && vulkanVersion != null

    val likelyMali: Boolean
        get() = listOfNotNull(gpuVendor, gpuRenderer)
            .any { it.contains("Mali", ignoreCase = true) }

    fun summary(): String = buildList {
        add("OpenGL ES: " + (glEsVersion ?: "Unknown"))
        add("GPU vendor: " + (gpuVendor ?: "Unknown"))
        add("GPU renderer: " + (gpuRenderer ?: "Unknown"))
        add("Vulkan: " + if (hasVulkan) "Advertised (level=$vulkanHardwareLevel, version=$vulkanVersion)" else "Not detected")
        if (likelyMali) add("GPU family: Mali (renderer/vendor string match)")
    }.joinToString("\n")
}
