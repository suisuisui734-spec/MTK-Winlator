package com.mtk.winlator.runtime

import android.content.Context
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Installs ZIP-based runtime components into app-private storage.
 *
 * A manifest must be supplied by trusted app code. HTTPS and SHA-256 checks do
 * not authenticate a manifest that an attacker can replace.
 */
class ModularRuntimeInstaller(context: Context) : RuntimeInstaller {
    private val root = File(context.filesDir, "runtimes")

    override suspend fun install(manifest: RuntimeManifest): RuntimeInstallState {
        val validation = validate(manifest) ?: return RuntimeInstallState.FAILED
        val staging = File(root, ".${manifest.id}.staging")
        val destination = File(root, manifest.id)
        return runCatching {
            root.mkdirs()
            staging.deleteRecursively()
            check(staging.mkdirs())
            for (component in validation) {
                val archive = File(staging, "${component.id}.zip")
                download(component, archive)
                check(RuntimePackageVerifier.matchesExpectedSha256(archive, component.sha256))
                val output = File(staging, component.id)
                check(output.mkdirs())
                extractSafely(archive, output)
                archive.delete()
            }
            val marker = File(staging, "install-manifest.txt")
            marker.writeText(
                "id=${manifest.id}\nversion=${manifest.version}\narchitecture=${manifest.architecture}\n" +
                    validation.joinToString("\n") { "${it.id}=${it.version}:${it.sha256.lowercase()}" }
            )
            val backup = File(root, ".${manifest.id}.previous")
            backup.deleteRecursively()
            if (destination.exists()) check(destination.renameTo(backup))
            if (!staging.renameTo(destination)) {
                if (backup.exists()) backup.renameTo(destination)
                error("Could not activate runtime directory")
            }
            backup.deleteRecursively()
            RuntimeInstallState.INSTALLED
        }.getOrElse {
            staging.deleteRecursively()
            RuntimeInstallState.FAILED
        }
    }

    override suspend fun verify(manifest: RuntimeManifest): Boolean {
        val components = validate(manifest) ?: return false
        val installed = File(root, manifest.id)
        val marker = File(installed, "install-manifest.txt")
        if (!marker.isFile) return false
        val expected = "id=${manifest.id}\nversion=${manifest.version}\narchitecture=${manifest.architecture}\n" +
            components.joinToString("\n") { "${it.id}=${it.version}:${it.sha256.lowercase()}" }
        return runCatching { marker.readText() == expected }.getOrDefault(false) &&
            components.all { File(installed, it.id).isDirectory }
    }

    override suspend fun remove(runtimeId: String): Boolean {
        if (!runtimeId.matches(Regex("^[a-zA-Z0-9._-]{1,64}$"))) return false
        val target = File(root, runtimeId).canonicalFile
        if (target.parentFile != root.canonicalFile) return false
        return !target.exists() || target.deleteRecursively()
    }

    private fun validate(manifest: RuntimeManifest): List<RuntimeComponent>? {
        if (!manifest.id.matches(Regex("^[a-zA-Z0-9._-]{1,64}$"))) return null
        if (manifest.version.isBlank() || manifest.architecture != "arm64-v8a") return null
        if (manifest.components.isEmpty() || manifest.components.size > MAX_COMPONENTS) return null
        val ids = mutableSetOf<String>()
        for (component in manifest.components) {
            if (!component.id.matches(Regex("^[a-zA-Z0-9._-]{1,64}$")) || !ids.add(component.id)) return null
            if (component.version.isBlank() || component.licenseId.isBlank()) return null
            if (component.sizeBytes !in 1..MAX_ARCHIVE_BYTES) return null
            if (!RuntimePackageVerifier.isSecureDownloadUrl(component.downloadUrl)) return null
            if (!component.sha256.matches(Regex("(?i)^[a-f0-9]{64}$"))) return null
        }
        return manifest.components
    }

    private fun download(component: RuntimeComponent, target: File) {
        val connection = (URL(component.downloadUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = false
            requestMethod = "GET"
        }
        try {
            check(connection.responseCode in 200..299)
            val declared = connection.contentLengthLong
            if (declared >= 0) check(declared == component.sizeBytes)
            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        check(total <= component.sizeBytes && total <= MAX_ARCHIVE_BYTES)
                        output.write(buffer, 0, count)
                    }
                    check(total == component.sizeBytes)
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun extractSafely(archive: File, destination: File) {
        val canonicalRoot = destination.canonicalFile
        var totalWritten = 0L
        var entries = 0
        ZipInputStream(BufferedInputStream(archive.inputStream())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries++
                check(entries <= MAX_ZIP_ENTRIES)
                val output = File(destination, entry.name).canonicalFile
                check(output.path.startsWith(canonicalRoot.path + File.separator))
                if (entry.isDirectory) {
                    check(output.mkdirs() || output.isDirectory)
                } else {
                    check(output.parentFile?.let { it.mkdirs() || it.isDirectory } == true)
                    FileOutputStream(output).use { stream ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val count = zip.read(buffer)
                            if (count < 0) break
                            totalWritten += count
                            check(totalWritten <= MAX_EXTRACTED_BYTES)
                            stream.write(buffer, 0, count)
                        }
                    }
                }
                zip.closeEntry()
            }
        }
    }

    companion object {
        private const val MAX_COMPONENTS = 12
        private const val MAX_ARCHIVE_BYTES = 1024L * 1024L * 1024L
        private const val MAX_EXTRACTED_BYTES = 2L * 1024L * 1024L * 1024L
        private const val MAX_ZIP_ENTRIES = 20_000
    }
}
