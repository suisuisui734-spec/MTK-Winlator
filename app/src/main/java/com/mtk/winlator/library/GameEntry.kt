package com.mtk.winlator.library

/** Persistable launcher library record. Execution support is not implemented yet. */
data class GameEntry(
    val id: String,
    val title: String,
    val executableUri: String,
    val profileId: String = "balanced",
    val fileSizeBytes: Long? = null,
    /** Saved preference only; it is not applied until a runtime exists. */
    val graphicsBackend: String = "AUTO",
    /** Declarative container preferences; not applied to a running process. */
    val resolutionWidth: Int = 1280,
    val resolutionHeight: Int = 720,
    val memoryLimitPercent: Int = 75
)
