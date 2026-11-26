package com.mtk.winlator.runtime

import java.io.File
import java.security.MessageDigest

/**
 * Integrity helper for runtime archives.
 *
 * SHA-256 only establishes that a file matches a trusted expected digest.
 * The expected digest must come from a trusted, authenticated release manifest.
 */
object RuntimePackageVerifier {
    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    fun matchesExpectedSha256(file: File, expected: String): Boolean {
        val normalized = expected.trim().lowercase()
        if (!normalized.matches(Regex("^[a-f0-9]{64}$"))) return false
        return runCatching { sha256(file) == normalized }.getOrDefault(false)
    }

    fun isSecureDownloadUrl(url: String): Boolean =
        runCatching { java.net.URI(url).scheme.equals("https", ignoreCase = true) }.getOrDefault(false)
}
