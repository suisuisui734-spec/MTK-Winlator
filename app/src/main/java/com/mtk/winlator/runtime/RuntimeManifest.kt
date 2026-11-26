package com.mtk.winlator.runtime

/** Manifest model for future runtime packages. No packages are bundled in this scaffold. */
data class RuntimeManifest(val id: String, val version: String, val abi: String = "arm64-v8a", val components: List<RuntimeComponent>)

data class RuntimeComponent(
    val id: String,
    val version: String,
    val sizeBytes: Long,
    val sha256: String,
    val downloadUrl: String,
    val licenseId: String
)
