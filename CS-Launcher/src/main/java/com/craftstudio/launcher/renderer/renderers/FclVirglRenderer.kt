package com.craftstudio.launcher.renderer.renderers

import com.craftstudio.launcher.renderer.RendererInterface

class FclVirglRenderer : RendererInterface {
    override fun getRendererId(): String = "fclplugin_virgl"

    override fun getUniqueIdentifier(): String = "e5f6a78b-9c0d-1e2f-3a4b-5c6d7e8f9a0b"

    override fun getRendererName(): String = "Holy VirGL (FCL Plugin)"

    override fun getRendererEnv(): Lazy<Map<String, String>> = lazy {
        mapOf()
    }

    override fun getDlopenLibrary(): Lazy<List<String>> = lazy { emptyList() }

    override fun getRendererLibrary(): String = "libOSMesa.so" // Managed via plugin
}
