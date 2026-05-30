package com.craftstudio.launcher.renderer.renderers

import com.craftstudio.launcher.renderer.RendererInterface

class FclGl4esRenderer : RendererInterface {
    override fun getRendererId(): String = "fclplugin_gl4es"

    override fun getUniqueIdentifier(): String = "d4e5f6a7-8b9c-0d1e-2f3a-4b5c6d7e8f9a"

    override fun getRendererName(): String = "Holy GL4ES (FCL Plugin)"

    override fun getRendererEnv(): Lazy<Map<String, String>> = lazy {
        mapOf()
    }

    override fun getDlopenLibrary(): Lazy<List<String>> = lazy { emptyList() }

    override fun getRendererLibrary(): String = "libgl4es_114.so" // Managed via plugin actually
}
