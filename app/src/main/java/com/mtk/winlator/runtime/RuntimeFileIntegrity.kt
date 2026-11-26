package com.mtk.winlator.runtime

import java.io.File
import java.security.MessageDigest
import java.util.ArrayDeque

/**
 * Detects post-install changes against a deterministic local file inventory.
 *
 * This record is generated after the signed archive has been hash-checked.
 * It detects missing/changed files during later checks, but is not itself
 * cryptographically authenticated against an attacker who can rewrite app data.
 */
object RuntimeFileIntegrity {
    private const val MAX_FILES = 20_000

    fun createRecord(installDirectory: File, componentIds: List<String>): String? =
        runCatching {
            val root = installDirectory.canonicalFile
            val records = mutableListOf<String>()
            for (id in componentIds) {
                if (!id.matches(Regex("^[A-Za-z0-9._-]{1,64}$")) || id == "." || id == "..") return null
                val componentRoot = File(root, id)
                if (isLink(componentRoot) || !componentRoot.isDirectory ||
                    componentRoot.canonicalFile.parentFile != root
                ) return null
                val pending = ArrayDeque<File>()
                pending.add(componentRoot)
                while (pending.isNotEmpty()) {
                    val directory = pending.removeLast()
                    val children = directory.listFiles()?.sortedBy { it.name } ?: return null
                    for (child in children) {
                        if (isLink(child)) return null
                        if (child.isDirectory) {
                            pending.add(child)
                        } else if (child.isFile) {
                            if (records.size >= MAX_FILES) return null
                            val relative = child.relativeTo(root).invariantSeparatorsPath
                            if (relative.any { it == '\n' || it == '\r' || it == '\t' || it == '=' }) return null
                            records += relative + "=" + sha256(child)
                        } else return null
                    }
                }
            }
            records.sorted().joinToString("\n", postfix = if (records.isEmpty()) "" else "\n")
        }.getOrNull()

    fun matchesRecord(installDirectory: File, componentIds: List<String>, recordFile: File): Boolean {
        if (isLink(recordFile) || !recordFile.isFile ||
            recordFile.canonicalFile.parentFile != installDirectory.canonicalFile
        ) return false
        val expected = createRecord(installDirectory, componentIds) ?: return false
        return runCatching { recordFile.readText() == expected }.getOrDefault(false)
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(32 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun isLink(file: File): Boolean =
        runCatching { java.nio.file.Files.isSymbolicLink(file.toPath()) }.getOrDefault(true)
}
