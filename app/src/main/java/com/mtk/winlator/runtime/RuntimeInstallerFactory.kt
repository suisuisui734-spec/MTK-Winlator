package com.mtk.winlator.runtime

import android.content.Context
import com.mtk.winlator.BuildConfig

/**
 * Creates the installer with the public key pinned at build time.
 *
 * If the Gradle property is unset, the verifier has no trusted key and rejects
 * every manifest. This is intentional fail-closed behavior for development builds.
 */
object RuntimeInstallerFactory {
    fun create(context: Context): RuntimeInstaller {
        val verifier = RuntimeManifestSignatureVerifier(
            BuildConfig.RUNTIME_MANIFEST_PUBLIC_KEY_BASE64
        )
        return ModularRuntimeInstaller(context.applicationContext, verifier)
    }
}
