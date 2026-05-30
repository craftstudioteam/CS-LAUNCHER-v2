package com.craftstudio.launcher.renderer.renderers

import com.craftstudio.launcher.renderer.RendererInterface

class GalliumGenericRenderer : RendererInterface {
    override fun getRendererId(): String = "gallium_generic"

    override fun getUniqueIdentifier(): String = "b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e"

    override fun getRendererName(): String = "Gallium Generic (Mesa)"

    override fun getRendererEnv(): Lazy<Map<String, String>> = lazy {
        mapOf()
    }

    override fun getDlopenLibrary(): Lazy<List<String>> = lazy { emptyList() }

    override fun getRendererLibrary(): String = "libOSMesa.so"
}
