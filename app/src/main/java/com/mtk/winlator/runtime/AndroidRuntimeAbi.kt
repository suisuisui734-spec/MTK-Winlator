package com.mtk.winlator.runtime

import android.os.Build
import android.os.Process

/** Android platform adapter for the pure ABI compatibility check. */
object AndroidRuntimeAbi {
    fun status(): RuntimeAbiCompatibility.Status =
        RuntimeAbiCompatibility.evaluate(
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            processIs64Bit = Process.is64Bit()
        )
}
