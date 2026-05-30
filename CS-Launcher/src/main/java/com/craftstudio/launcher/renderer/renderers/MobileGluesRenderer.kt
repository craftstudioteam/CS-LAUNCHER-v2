package com.craftstudio.launcher.renderer.renderers

import com.craftstudio.launcher.renderer.RendererInterface

class MobileGluesRenderer : RendererInterface {
    override fun getRendererId(): String = "mobileglues"

    override fun getUniqueIdentifier(): String = "c2b3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e" // Unique UUID

    override fun getRendererName(): String = "MobileGlues (OpenGL ES 3)"

    override fun getRendererEnv(): Lazy<Map<String, String>> = lazy {
        mapOf() // Envs are handled in JREUtils.java
    }

    override fun getDlopenLibrary(): Lazy<List<String>> = lazy { emptyList() }

    override fun getRendererLibrary(): String = "libMobileGlues.so"
}
