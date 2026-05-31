package com.craftstudio.launcher.renderer.renderers

import com.craftstudio.launcher.renderer.RendererInterface

class LTWRenderer : RendererInterface {
    override fun getRendererId(): String = "ltw_render"

    override fun getUniqueIdentifier(): String = "f4e5d6c7-b8a9-4b3c-9d1e-2f3a4b5c6d7e"

    override fun getRendererName(): String = "LTW Render"

    override fun getRendererEnv(): Lazy<Map<String, String>> = lazy {
        mapOf(
            "MESA_GL_VERSION_OVERRIDE" to "4.6",
            "MESA_GLSL_VERSION_OVERRIDE" to "460",
            "MESA_LOADER_DRIVER_OVERRIDE" to "ltw",
            "GALLIUM_DRIVER" to "ltw"
        )
    }

    override fun getDlopenLibrary(): Lazy<List<String>> = lazy { emptyList() }

    override fun getRendererLibrary(): String = "libltw.so"
}
