package com.mtk.winlator.runtime

/**
 * Declarative per-container preferences only. This model does not create a
 * Wine prefix, mount filesystems, launch processes, or apply GPU settings.
 */
data class RuntimeContainerConfig(
    val id: String,
    val displayName: String,
    val architecture: String = "arm64",
    val graphicsBackend: GraphicsBackend = GraphicsBackend.AUTO,
    val resolution: Resolution = Resolution(1280, 720),
    val memoryLimitPercent: Int = 75
) {
    enum class GraphicsBackend { AUTO, OPENGL, VULKAN }

    data class Resolution(val width: Int, val height: Int)

    fun validate(): Validation {
        if (!ID_PATTERN.matches(id)) return Validation.Invalid("Invalid container ID")
        if (displayName.isBlank() || displayName.length > 64) {
            return Validation.Invalid("Display name must contain 1-64 characters")
        }
        if (architecture != "arm64") return Validation.Invalid("Only arm64 is currently supported")
        if (resolution.width !in 320..7680 || resolution.height !in 240..4320) {
            return Validation.Invalid("Resolution is outside supported bounds")
        }
        if (memoryLimitPercent !in 25..90) {
            return Validation.Invalid("Memory limit must be between 25 and 90 percent")
        }
        return Validation.Valid
    }

    sealed interface Validation {
        data object Valid : Validation
        data class Invalid(val reason: String) : Validation
    }

    companion object {
        private val ID_PATTERN = Regex("^[a-zA-Z0-9][a-zA-Z0-9_-]{0,47}$")
    }
}
