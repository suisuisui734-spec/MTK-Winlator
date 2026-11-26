package com.mtk.winlator.runtime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeGraphicsCapabilitiesTest {
    @Test
    fun detectsMaliFromRendererOrVendorText() {
        val capabilities = RuntimeGraphicsCapabilities("OpenGL ES 3.2", "ARM", "Mali-G78", null, null)
        assertTrue(capabilities.likelyMali)
    }

    @Test
    fun doesNotAssumeVulkanWhenFeatureDataIsMissing() {
        val capabilities = RuntimeGraphicsCapabilities("OpenGL ES 3.2", "ARM", "Mali-G57", null, null)
        assertFalse(capabilities.hasVulkan)
    }

    @Test
    fun reportsVulkanOnlyWhenBothFeatureValuesExist() {
        val capabilities = RuntimeGraphicsCapabilities("OpenGL ES 3.2", "ARM", "Mali-G78", 1, 4198400)
        assertTrue(capabilities.hasVulkan)
    }

    @Test
    fun unknownGpuDoesNotBecomeMali() {
        val capabilities = RuntimeGraphicsCapabilities(null, null, null, null, null)
        assertFalse(capabilities.likelyMali)
        assertFalse(capabilities.hasOpenGles)
    }
}
