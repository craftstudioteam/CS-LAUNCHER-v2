package com.craftstudio.launcher.renderer.renderers

import com.craftstudio.launcher.renderer.RendererInterface

class VulkanZinkRenderer : RendererInterface {
    override fun getRendererId(): String = "vulkan_zink"

    override fun getUniqueIdentifier(): String = "0fa435e2-46df-45c9-906c-b29606aaef00"

    override fun getRendererName(): String = "Zink (Vulkan)"

    override fun getRendererEnv(): Lazy<Map<String, String>> = lazy {
        mapOf(
            "MESA_GL_VERSION_OVERRIDE" to "4.6",
            "MESA_GLSL_VERSION_OVERRIDE" to "460",
            "MESA_LOADER_DRIVER_OVERRIDE" to "zink",
            "ZINK_DESCRIPTORS" to "lazy",
            "GALLIUM_DRIVER" to "zink"
        )
    }

    override fun getDlopenLibrary(): Lazy<List<String>> = lazy { emptyList() }

    override fun getRendererLibrary(): String = "libOSMesa.so"
}