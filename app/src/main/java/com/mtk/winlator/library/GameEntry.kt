package com.mtk.winlator.library

/** Persistable launcher library record. Execution support is not implemented yet. */
data class GameEntry(
    val id: String,
    val title: String,
    val executableUri: String,
    val profileId: String = "balanced",
    val fileSizeBytes: Long? = null,
    /** Saved preference only; it is not applied until a runtime exists. */
    val graphicsBackend: String = "AUTO"
)
