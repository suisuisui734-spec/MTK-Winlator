package com.mtk.winlator.library

/** Persistable launcher library record. Execution support is not implemented yet. */
data class GameEntry(
    val id: String,
    val title: String,
    val executableUri: String,
    val profileId: String = "balanced"
)
