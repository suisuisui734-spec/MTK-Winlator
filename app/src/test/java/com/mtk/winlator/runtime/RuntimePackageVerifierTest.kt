package com.mtk.winlator.runtime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RuntimePackageVerifierTest {
    @Test
    fun sha256MatchesExpectedDigestAndRejectsMalformedDigest() {
        val temp = File.createTempFile("mtk-verifier", ".txt")
        try {
            temp.writeText("MTK Winlator verifier test")
            val expected = RuntimePackageVerifier.sha256(temp)
            assertTrue(RuntimePackageVerifier.matchesExpectedSha256(temp, expected))
            assertFalse(RuntimePackageVerifier.matchesExpectedSha256(temp, "not-a-hash"))
            assertFalse(RuntimePackageVerifier.matchesExpectedSha256(temp, "0".repeat(64)))
        } finally {
            temp.delete()
        }
    }

    @Test
    fun secureUrlValidationRequiresHttpsHostAndNoCredentials() {
        assertTrue(RuntimePackageVerifier.isSecureDownloadUrl("https://example.org/runtime.zip"))
        assertFalse(RuntimePackageVerifier.isSecureDownloadUrl("http://example.org/runtime.zip"))
        assertFalse(RuntimePackageVerifier.isSecureDownloadUrl("https:///missing-host"))
        assertFalse(RuntimePackageVerifier.isSecureDownloadUrl("https://user:pass@example.org/runtime.zip"))
        assertFalse(RuntimePackageVerifier.isSecureDownloadUrl("not a url"))
    }
}
