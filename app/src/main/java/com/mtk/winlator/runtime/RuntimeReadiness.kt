package com.mtk.winlator.runtime

/**
 * Evidence supplied by runtime discovery/installation code.
 * Defaults are false: missing evidence must never imply readiness.
 */
data class RuntimeReadinessEvidence(
    val arm64DeviceSupported: Boolean = false,
    val arm64ProcessSupported: Boolean = false,
    val productionManifestKeyConfigured: Boolean = false,
    val signedRuntimeManifestAvailable: Boolean = false,
    val runtimePackageInstalled: Boolean = false,
    val wineAvailable: Boolean = false,
    val box64Available: Boolean = false,
    val graphicsBackendAvailable: Boolean = false
)

data class RuntimeReadinessReport(val checks: List<Check>) {
    data class Check(
        val id: String,
        val title: String,
        val ready: Boolean,
        val detail: String
    )

    val readyCount: Int get() = checks.count { it.ready }
    val totalCount: Int get() = checks.size
    val isReadyForWindowsExecution: Boolean
        get() = checks.isNotEmpty() && checks.all { it.ready }
    val blockingChecks: List<Check> get() = checks.filterNot { it.ready }
}

/**
 * Conservative prerequisite evaluator only. It does not install components,
 * verify package provenance, create prefixes, or launch Windows applications.
 */
object RuntimeReadinessEvaluator {
    fun evaluate(evidence: RuntimeReadinessEvidence): RuntimeReadinessReport {
        fun check(id: String, title: String, ready: Boolean, yes: String, no: String) =
            RuntimeReadinessReport.Check(id, title, ready, if (ready) yes else no)

        return RuntimeReadinessReport(
            listOf(
                check("device_abi", "ARM64 device support", evidence.arm64DeviceSupported,
                    "Device ABI reports ARM64 support", "ARM64 device support is not confirmed"),
                check("process_abi", "ARM64 process support", evidence.arm64ProcessSupported,
                    "Application process is ARM64", "Application process is not confirmed as ARM64"),
                check("manifest_key", "Production manifest key", evidence.productionManifestKeyConfigured,
                    "Production verification key is configured", "Release verification key is not configured"),
                check("signed_manifest", "Signed runtime manifest", evidence.signedRuntimeManifestAvailable,
                    "Signed runtime manifest is available", "No verified signed runtime manifest is available"),
                check("runtime_package", "Runtime package", evidence.runtimePackageInstalled,
                    "Runtime package is installed", "Runtime package is not installed"),
                check("wine", "Wine compatibility layer", evidence.wineAvailable,
                    "Wine component is available", "Wine component is not integrated"),
                check("box64", "Box64 translation layer", evidence.box64Available,
                    "Box64 component is available", "Box64 component is not integrated"),
                check("graphics", "Compatible graphics backend", evidence.graphicsBackendAvailable,
                    "Compatible graphics backend is available", "Usable Windows graphics backend is not confirmed")
            )
        )
    }
}
