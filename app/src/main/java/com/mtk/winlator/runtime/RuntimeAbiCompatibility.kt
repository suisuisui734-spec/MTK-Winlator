package com.mtk.winlator.runtime

/**
 * Read-only ABI gate for future native compatibility components.
 *
 * This does not imply that Wine, Box64, or a graphics backend is installed.
 * It only determines whether the current Android process can load ARM64
 * native libraries.
 */
object RuntimeAbiCompatibility {
    const val REQUIRED_ABI = "arm64-v8a"

    enum class Status {
        SUPPORTED,
        NO_ARM64_DEVICE_ABI,
        PROCESS_NOT_64_BIT
    }

    fun evaluate(supportedAbis: List<String>, processIs64Bit: Boolean): Status {
        if (REQUIRED_ABI !in supportedAbis) return Status.NO_ARM64_DEVICE_ABI
        if (!processIs64Bit) return Status.PROCESS_NOT_64_BIT
        return Status.SUPPORTED
    }
}
