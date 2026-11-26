package com.mtk.winlator.runtime

/**
 * Rejects unsafe, duplicate, and file/directory-conflicting ZIP entry paths.
 * Returns a normalized relative path only when the entry is safe to extract.
 */
internal class RuntimeArchivePathGuard {
    private val entries = mutableMapOf<String, Boolean>()

    fun accept(name: String, isDirectory: Boolean): String? {
        if (name.isBlank() || '\\' in name || name.startsWith('/') ||
            name.contains('\u0000') || Regex("^[A-Za-z]:").containsMatchIn(name)
        ) return null

        val candidate = if (isDirectory) name.removeSuffix("/") else name
        if (candidate.isBlank()) return null
        val segments = candidate.split('/')
        if (segments.any { it.isBlank() || it == "." || it == ".." }) return null
        val normalized = segments.joinToString("/")

        if (entries.containsKey(normalized)) return null
        var parent = normalized.substringBeforeLast('/', "")
        while (parent.isNotEmpty()) {
            if (entries[parent] == false) return null
            parent = parent.substringBeforeLast('/', "")
        }
        if (!isDirectory && entries.keys.any { it.startsWith("$normalized/") }) return null

        entries[normalized] = isDirectory
        return normalized
    }
}
