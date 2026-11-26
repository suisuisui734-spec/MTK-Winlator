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
        if (!isStructurallySafe(manifest)) return false
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

    /**
     * Bounds and basic shape checks run before serialization/signature work.
     * This prevents oversized or ambiguous untrusted manifest fields from
     * reaching DataOutputStream.writeUTF or the installer.
     */
    private fun isStructurallySafe(manifest: RuntimeManifest): Boolean {
        if (!manifest.id.matches(Regex("^[a-zA-Z0-9._-]{1,64}$"))) return false
        if (manifest.id == "." || manifest.id == "..") return false
        if (manifest.version.isBlank() || manifest.version.length > MAX_FIELD_LENGTH) return false
        if (manifest.architecture != "arm64-v8a") return false
        if (manifest.components.isEmpty() || manifest.components.size > MAX_COMPONENTS) return false
        if (manifest.signatureBase64.length > MAX_SIGNATURE_LENGTH) return false

        val ids = mutableSetOf<String>()
        return manifest.components.all { component ->
            component.id.matches(Regex("^[a-zA-Z0-9._-]{1,64}$")) &&
                component.id != "." && component.id != ".." &&
                ids.add(component.id) &&
                component.version.isNotBlank() && component.version.length <= MAX_FIELD_LENGTH &&
                component.sizeBytes in 1..MAX_ARCHIVE_BYTES &&
                component.sha256.matches(Regex("(?i)^[a-f0-9]{64}$")) &&
                component.downloadUrl.length <= MAX_FIELD_LENGTH &&
                component.downloadUrl.startsWith("https://", ignoreCase = true) &&
                component.licenseId.isNotBlank() && component.licenseId.length <= MAX_FIELD_LENGTH
        }
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

    companion object {
        private const val MAX_COMPONENTS = 12
        private const val MAX_FIELD_LENGTH = 512
        private const val MAX_SIGNATURE_LENGTH = 256
        private const val MAX_ARCHIVE_BYTES = 1024L * 1024L * 1024L
    }
}
