package com.mtk.winlator.library

/** Returns true when a display name identifies a Windows executable file. */
fun isWindowsExecutableName(displayName: String?): Boolean {
    val name = displayName?.trim().orEmpty()
    return name.substringBeforeLast('.', "").isNotBlank() && name.substringAfterLast('.', "").equals("exe", ignoreCase = true)
}
