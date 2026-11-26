package com.mtk.winlator.runtime

/**
 * Data contract for future modular runtime packages.
 * Runtime artifacts are not downloaded or bundled by the current app.
 */
data class RuntimeManifest(
    val id: String,
    val version: String,
    val architecture: String = "arm64-v8a",
    val components: List<RuntimeComponent>
)

data class RuntimeComponent(
    val id: String,
    val version: String,
    val sizeBytes: Long,
    val sha256: String,
    val downloadUrl: String,
    val licenseId: String
)

enum class RuntimeInstallState { NOT_INSTALLED, DOWNLOADING, VERIFYING, INSTALLED, FAILED }

interface RuntimeInstaller {
    suspend fun install(manifest: RuntimeManifest): RuntimeInstallState
    suspend fun verify(manifest: RuntimeManifest): Boolean
    suspend fun remove(runtimeId: String): Boolean
}
