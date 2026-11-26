package com.mtk.winlator.library

import com.mtk.winlator.runtime.RuntimeContainerRegistry

/**
 * Reconciles launcher game entries with their declarative container metadata.
 *
 * Only IDs in the "game-" namespace are managed here. All desired entries are
 * saved before stale managed entries are removed, so a failed save never causes
 * cleanup of metadata that may still be needed. This does not start containers.
 */
class GameContainerConfigSynchronizer(
    private val registry: RuntimeContainerRegistry
) {
    data class SyncResult(
        val savedCount: Int,
        val removedCount: Int,
        val failedIds: List<String>
    ) {
        val successful: Boolean get() = failedIds.isEmpty()
    }

    fun synchronize(entries: Iterable<GameEntry>): SyncResult {
        val desired = entries.map { it.toRuntimeContainerConfig() }
            .distinctBy { it.id }
        val desiredIds = desired.mapTo(mutableSetOf()) { it.id }
        var savedCount = 0
        val failures = mutableListOf<String>()

        desired.forEach { config ->
            when (registry.save(config)) {
                RuntimeContainerRegistry.SaveResult.Saved -> savedCount++
                is RuntimeContainerRegistry.SaveResult.Rejected -> failures += config.id
            }
        }

        // Fail closed: do not delete old metadata if any desired save failed.
        if (failures.isNotEmpty()) {
            return SyncResult(savedCount, removedCount = 0, failedIds = failures.distinct())
        }

        var removedCount = 0
        registry.list()
            .asSequence()
            .filter { it.id.startsWith(GAME_ID_PREFIX) && it.id !in desiredIds }
            .forEach { config ->
                if (registry.delete(config.id)) removedCount++ else failures += config.id
            }

        return SyncResult(savedCount, removedCount, failures.distinct())
    }

    private companion object {
        const val GAME_ID_PREFIX = "game-"
    }
}
