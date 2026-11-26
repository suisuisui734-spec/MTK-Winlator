package com.mtk.winlator.runtime

import java.io.File

/**
 * Read-only inspection of an installed runtime directory.
 *
 * This intentionally never reports VERIFIED: a marker and extracted folders
 * cannot prove that every extracted file still matches its signed archive.
 * The installer/signature verification path must provide that evidence.
 */
enum class RuntimeInventoryState {
    NOT_CONFIGURED,
    NOT_INSTALLED,
    INCOMPLETE,
    FILES_PRESENT_UNVERIFIED,
    INVALID_LAYOUT
}

data class RuntimeInventoryReport(
    val state: RuntimeInventoryState,
    val detail: String,
    val componentIds: List<String> = emptyList()
)

class RuntimeComponentInventory(private val runtimeRoot: File) {
    fun inspect(manifest: RuntimeManifest?): RuntimeInventoryReport {
        if (manifest == null) {
            return RuntimeInventoryReport(
                RuntimeInventoryState.NOT_CONFIGURED,
                "No trusted runtime manifest has been supplied."
            )
        }
        if (!manifest.id.matches(ID_PATTERN) || manifest.id == "." || manifest.id == ".." ||
            manifest.components.isEmpty() ||
            manifest.components.any { !it.id.matches(ID_PATTERN) || it.id == "." || it.id == ".." } ||
            manifest.components.map { it.id }.distinct().size != manifest.components.size
        ) {
            return RuntimeInventoryReport(
                RuntimeInventoryState.INVALID_LAYOUT,
                "Manifest identifiers are invalid or duplicated."
            )
        }

        return runCatching {
            if (runtimeRoot.exists() && (runtimeRoot.isSymbolicLink() || !runtimeRoot.isDirectory)) {
                return RuntimeInventoryReport(
                    RuntimeInventoryState.INVALID_LAYOUT,
                    "Runtime root is not a regular directory."
                )
            }
            val root = runtimeRoot.canonicalFile
            val installDir = File(root, manifest.id)
            if (!installDir.exists()) {
                return RuntimeInventoryReport(
                    RuntimeInventoryState.NOT_INSTALLED,
                    "No installed directory exists for this runtime."
                )
            }
            if (installDir.isSymbolicLink() || !installDir.isDirectory ||
                installDir.canonicalFile.parentFile != root
            ) {
                return RuntimeInventoryReport(
                    RuntimeInventoryState.INVALID_LAYOUT,
                    "Installed runtime directory is not a safe direct child."
                )
            }

            val marker = File(installDir, "install-manifest.txt")
            val expectedMarker = "id=${manifest.id}\nversion=${manifest.version}\narchitecture=${manifest.architecture}\n" +
                manifest.components.joinToString("\n") {
                    "${it.id}=${it.version}:${it.sha256.lowercase()}"
                }
            if (!marker.exists()) {
                return RuntimeInventoryReport(
                    RuntimeInventoryState.INCOMPLETE,
                    "Install metadata is missing."
                )
            }
            if (marker.isSymbolicLink() || !marker.isFile ||
                marker.canonicalFile.parentFile != installDir.canonicalFile ||
                marker.readText() != expectedMarker
            ) {
                return RuntimeInventoryReport(
                    RuntimeInventoryState.INVALID_LAYOUT,
                    "Install metadata does not match the requested manifest."
                )
            }

            val present = mutableListOf<String>()
            for (component in manifest.components) {
                val directory = File(installDir, component.id)
                if (!directory.exists()) {
                    return RuntimeInventoryReport(
                        RuntimeInventoryState.INCOMPLETE,
                        "One or more component directories are missing.",
                        present
                    )
                }
                if (directory.isSymbolicLink() || !directory.isDirectory ||
                    directory.canonicalFile.parentFile != installDir.canonicalFile
                ) {
                    return RuntimeInventoryReport(
                        RuntimeInventoryState.INVALID_LAYOUT,
                        "A component path is not a safe directory.",
                        present
                    )
                }
                val scan = scanTree(directory)
                if (!scan.first) {
                    return RuntimeInventoryReport(
                        RuntimeInventoryState.INVALID_LAYOUT,
                        "A component contains a symbolic link or unsafe filesystem entry.",
                        present
                    )
                }
                if (!scan.second) {
                    return RuntimeInventoryReport(
                        RuntimeInventoryState.INCOMPLETE,
                        "A component directory is empty.",
                        present
                    )
                }
                present += component.id
            }
            RuntimeInventoryReport(
                RuntimeInventoryState.FILES_PRESENT_UNVERIFIED,
                "Expected folders and metadata are present, but extracted file integrity and runtime compatibility are not verified.",
                present
            )
        }.getOrElse {
            RuntimeInventoryReport(
                RuntimeInventoryState.INVALID_LAYOUT,
                "Runtime directory could not be inspected safely."
            )
        }
    }

    private fun scanTree(directory: File): Pair<Boolean, Boolean> {
        var hasRegularFile = false
        val pending = ArrayDeque<File>()
        pending.add(directory)
        while (pending.isNotEmpty()) {
            val current = pending.removeLast()
            val children = current.listFiles() ?: return false to false
            for (child in children) {
                if (child.isSymbolicLink()) return false to false
                if (child.isDirectory) pending.add(child)
                else if (child.isFile) hasRegularFile = true
                else return false to false
            }
        }
        return true to hasRegularFile
    }

    private fun File.isSymbolicLink(): Boolean =
        runCatching { java.nio.file.Files.isSymbolicLink(toPath()) }.getOrDefault(true)

    companion object {
        private val ID_PATTERN = Regex("^[A-Za-z0-9._-]{1,64}$")
    }
}
