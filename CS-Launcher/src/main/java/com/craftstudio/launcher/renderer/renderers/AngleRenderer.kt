package com.craftstudio.launcher.renderer.renderers

import com.craftstudio.launcher.renderer.RendererInterface

class AngleRenderer : RendererInterface {
    override fun getRendererId(): String = "angle"

    override fun getUniqueIdentifier(): String = "c3f8e5d2-1a2b-4c3d-bd5f-e7a9b0c1d2e3"

    override fun getRendererName(): String = "OpenGL ES (Angle)"

    override fun getRendererEnv(): Lazy<Map<String, String>> = lazy {
        mapOf(
            "MESA_GL_VERSION_OVERRIDE" to "3.2",
            "MESA_GLSL_VERSION_OVERRIDE" to "150"
        )
    }

    override fun getDlopenLibrary(): Lazy<List<String>> = lazy { 
        listOf("libEGL_angle.so", "libGLESv2_angle.so")
    }

    override fun getRendererLibrary(): String = "libGLESv2_angle.so"
}
