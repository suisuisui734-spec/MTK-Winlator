package com.mtk.winlator.runtime

import java.util.Base64
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/**
 * Verifies detached SHA256withECDSA signatures using an app-pinned P-256 public
 * key encoded as X.509 SubjectPublicKeyInfo and then Base64.
 *
 * The matching private key must stay outside this repository and CI logs.
 * Never accept the public key from the manifest or a network response.
 */
class RuntimeManifestSignatureVerifier(
    private val pinnedPublicKeyBase64: String
) {
    fun verify(manifest: RuntimeManifest): Boolean {
        if (pinnedPublicKeyBase64.isBlank() || manifest.signatureBase64.isBlank()) return false
        return runCatching {
            val keyBytes = Base64.getDecoder().decode(pinnedPublicKeyBase64)
            val signatureBytes = Base64.getDecoder().decode(manifest.signatureBase64)
            val publicKey = KeyFactory.getInstance("EC")
                .generatePublic(X509EncodedKeySpec(keyBytes))
            Signature.getInstance("SHA256withECDSA").run {
                initVerify(publicKey)
                update(canonicalPayload(manifest))
                verify(signatureBytes)
            }
        }.getOrDefault(false)
    }

    internal fun canonicalPayload(manifest: RuntimeManifest): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeUTF("MTK-WINLATOR-RUNTIME-MANIFEST-V1")
            out.writeUTF(manifest.id)
            out.writeUTF(manifest.version)
            out.writeUTF(manifest.architecture)
            val components = manifest.components.sortedBy { it.id }
            out.writeInt(components.size)
            components.forEach { component ->
                out.writeUTF(component.id)
                out.writeUTF(component.version)
                out.writeLong(component.sizeBytes)
                out.writeUTF(component.sha256.lowercase())
                out.writeUTF(component.downloadUrl)
                out.writeUTF(component.licenseId)
            }
        }
        return bytes.toByteArray()
    }
}
