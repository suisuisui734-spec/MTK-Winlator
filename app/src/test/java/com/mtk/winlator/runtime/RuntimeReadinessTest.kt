package com.mtk.winlator.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeReadinessTest {
    @Test
    fun emptyEvidenceFailsClosedAndListsEveryBlocker() {
        val report = RuntimeReadinessEvaluator.evaluate(RuntimeReadinessEvidence())

        assertFalse(report.isReadyForWindowsExecution)
        assertEquals(0, report.readyCount)
        assertEquals(8, report.totalCount)
        assertEquals(8, report.blockingChecks.size)
    }

    @Test
    fun completeEvidenceIsReady() {
        val report = RuntimeReadinessEvaluator.evaluate(
            RuntimeReadinessEvidence(
                arm64DeviceSupported = true,
                arm64ProcessSupported = true,
                productionManifestKeyConfigured = true,
                signedRuntimeManifestAvailable = true,
                runtimePackageInstalled = true,
                wineAvailable = true,
                box64Available = true,
                graphicsBackendAvailable = true
            )
        )

        assertTrue(report.isReadyForWindowsExecution)
        assertEquals(8, report.readyCount)
        assertTrue(report.blockingChecks.isEmpty())
    }

    @Test
    fun oneMissingPrerequisiteKeepsExecutionBlocked() {
        val report = RuntimeReadinessEvaluator.evaluate(
            RuntimeReadinessEvidence(
                arm64DeviceSupported = true,
                arm64ProcessSupported = true,
                productionManifestKeyConfigured = true,
                signedRuntimeManifestAvailable = true,
                runtimePackageInstalled = true,
                wineAvailable = true,
                box64Available = false,
                graphicsBackendAvailable = true
            )
        )

        assertFalse(report.isReadyForWindowsExecution)
        assertEquals(listOf("box64"), report.blockingChecks.map { it.id })
    }
}
