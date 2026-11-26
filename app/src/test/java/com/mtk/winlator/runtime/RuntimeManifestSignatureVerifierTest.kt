package com.mtk.winlator.runtime

import java.util.Base64
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec

class RuntimeManifestSignatureVerifierTest {
    @Test
    fun verifiesTrustedSignatureAndRejectsTampering() {
        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec("secp256r1"))
        val pair = generator.generateKeyPair()
        val unsigned = sampleManifest()
        val signer = Signature.getInstance("SHA256withECDSA")
        val canonicalizer = RuntimeManifestSignatureVerifier(
            Base64.getEncoder().encodeToString(pair.public.encoded)
        )
        signer.initSign(pair.private)
        signer.update(canonicalizer.canonicalPayload(unsigned))
        val signed = unsigned.copy(
            signatureBase64 = Base64.getEncoder().encodeToString(signer.sign())
        )

        assertTrue(canonicalizer.verify(signed))
        assertFalse(canonicalizer.verify(signed.copy(version = "2.0")))
        assertFalse(canonicalizer.verify(signed.copy(signatureBase64 = "invalid")))
    }

    @Test
    fun missingPinnedKeyOrSignatureFailsClosed() {
        val manifest = sampleManifest()
        assertFalse(RuntimeManifestSignatureVerifier("").verify(manifest))
        assertFalse(RuntimeManifestSignatureVerifier("not-a-key").verify(manifest))
    }

    @Test
    fun rejectsOversizedAndAmbiguousManifestBeforeSignatureVerification() {
        val verifier = RuntimeManifestSignatureVerifier("not-a-key")
        val manifest = sampleManifest()
        assertFalse(verifier.verify(manifest.copy(version = "v".repeat(513))))
        assertFalse(verifier.verify(manifest.copy(components = manifest.components + manifest.components)))
        assertFalse(verifier.verify(manifest.copy(
            components = listOf(manifest.components.first().copy(downloadUrl = "http://example.org/a.zip"))
        )))
    }

    private fun sampleManifest() = RuntimeManifest(
        id = "wine",
        version = "1.0",
        components = listOf(
            RuntimeComponent(
                id = "core",
                version = "1.0",
                sizeBytes = 123,
                sha256 = "a".repeat(64),
                downloadUrl = "https://example.org/core.zip",
                licenseId = "LGPL-2.1"
            )
        )
    )
}
