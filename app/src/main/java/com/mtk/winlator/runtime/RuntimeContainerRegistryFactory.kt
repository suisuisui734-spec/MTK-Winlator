package com.mtk.winlator.runtime

import android.content.Context
import java.io.File

/** Provides the registry inside app-private internal storage. */
object RuntimeContainerRegistryFactory {
    fun create(context: Context): RuntimeContainerRegistry {
        val directory = File(context.applicationContext.filesDir, "containers")
        return RuntimeContainerRegistry(directory)
    }
}
