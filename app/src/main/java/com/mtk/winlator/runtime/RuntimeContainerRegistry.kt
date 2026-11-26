package com.mtk.winlator.runtime

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties

/**
 * Persistent registry for declarative container metadata.
 * It does NOT create Wine prefixes, mount storage, extract runtimes, or launch processes.
 */
class RuntimeContainerRegistry(private val rootDirectory: File) {
    sealed interface SaveResult {
        data object Saved : SaveResult
        data class Rejected(val reason: String) : SaveResult
    }

    @Synchronized
    fun save(config: RuntimeContainerConfig): SaveResult {
        val validation = config.validate()
        if (validation is RuntimeContainerConfig.Validation.Invalid) {
            return SaveResult.Rejected(validation.reason)
        }
        return runCatching {
            ensureSafeRoot()
            val target = entryFile(config.id)
            if (Files.isSymbolicLink(target.toPath())) {
                return SaveResult.Rejected("Metadata path cannot be a symbolic link")
            }
            val properties = Properties().apply {
                setProperty("schema_version", SCHEMA_VERSION)
                setProperty("id", config.id)
                setProperty("display_name", config.displayName)
                setProperty("architecture", config.architecture)
                setProperty("graphics_backend", config.graphicsBackend.name)
                setProperty("resolution_width", config.resolution.width.toString())
                setProperty("resolution_height", config.resolution.height.toString())
                setProperty("memory_limit_percent", config.memoryLimitPercent.toString())
            }
            val temporary = File(rootDirectory, ".${config.id}.${Thread.currentThread().id}.tmp")
            try {
                FileOutputStream(temporary).use { output ->
                    properties.store(output, "MTK Winlator container preferences")
                    output.fd.sync()
                }
                try {
                    Files.move(temporary.toPath(), target.toPath(),
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                } catch (_: AtomicMoveNotSupportedException) {
                    Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            } finally {
                temporary.delete()
            }
            SaveResult.Saved
        }.getOrElse { SaveResult.Rejected("Unable to save container metadata safely") }
    }

    @Synchronized
    fun get(id: String): RuntimeContainerConfig? {
        if (!ID_PATTERN.matches(id)) return null
        return runCatching {
            ensureSafeRoot()
            val file = entryFile(id)
            if (!file.isFile || Files.isSymbolicLink(file.toPath())) return null
            FileInputStream(file).use { input -> decode(Properties().apply { load(input) }, id) }
        }.getOrNull()
    }

    @Synchronized
    fun list(): List<RuntimeContainerConfig> = runCatching {
        ensureSafeRoot()
        rootDirectory.listFiles()
            ?.asSequence()
            ?.filter { it.isFile && it.name.endsWith(SUFFIX) && !Files.isSymbolicLink(it.toPath()) }
            ?.mapNotNull { file ->
                val id = file.name.removeSuffix(SUFFIX)
                if (!ID_PATTERN.matches(id)) null
                else runCatching {
                    FileInputStream(file).use { input -> decode(Properties().apply { load(input) }, id) }
                }.getOrNull()
            }
            ?.sortedBy { it.id }
            ?.toList() ?: emptyList()
    }.getOrDefault(emptyList())

    @Synchronized
    fun delete(id: String): Boolean {
        if (!ID_PATTERN.matches(id)) return false
        return runCatching {
            ensureSafeRoot()
            val file = entryFile(id)
            if (Files.isSymbolicLink(file.toPath())) false else !file.exists() || file.delete()
        }.getOrDefault(false)
    }

    private fun decode(properties: Properties, expectedId: String): RuntimeContainerConfig? {
        if (properties.getProperty("schema_version") != SCHEMA_VERSION ||
            properties.getProperty("id") != expectedId) return null
        val backend = runCatching {
            RuntimeContainerConfig.GraphicsBackend.valueOf(properties.getProperty("graphics_backend", "AUTO"))
        }.getOrNull() ?: return null
        val width = properties.getProperty("resolution_width")?.toIntOrNull() ?: return null
        val height = properties.getProperty("resolution_height")?.toIntOrNull() ?: return null
        val memory = properties.getProperty("memory_limit_percent")?.toIntOrNull() ?: return null
        val config = RuntimeContainerConfig(
            id = expectedId,
            displayName = properties.getProperty("display_name") ?: return null,
            architecture = properties.getProperty("architecture") ?: return null,
            graphicsBackend = backend,
            resolution = RuntimeContainerConfig.Resolution(width, height),
            memoryLimitPercent = memory
        )
        return config.takeIf { it.validate() == RuntimeContainerConfig.Validation.Valid }
    }

    private fun ensureSafeRoot() {
        if (!rootDirectory.exists() && !rootDirectory.mkdirs()) {
            throw IllegalStateException("Cannot create registry directory")
        }
        if (!rootDirectory.isDirectory || Files.isSymbolicLink(rootDirectory.toPath())) {
            throw IllegalStateException("Registry root is not a safe directory")
        }
    }

    private fun entryFile(id: String): File {
        require(ID_PATTERN.matches(id))
        val root = rootDirectory.canonicalFile
        val target = File(root, "$id$SUFFIX")
        require(target.parentFile?.canonicalFile == root) { "Invalid registry path" }
        return target
    }

    companion object {
        private const val SCHEMA_VERSION = "1"
        private const val SUFFIX = ".properties"
        private val ID_PATTERN = Regex("^[a-zA-Z0-9][a-zA-Z0-9_-]{0,47}$")
    }
}
