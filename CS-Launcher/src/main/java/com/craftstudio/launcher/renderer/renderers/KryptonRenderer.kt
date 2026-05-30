package com.craftstudio.launcher.renderer.renderers

import com.craftstudio.launcher.renderer.RendererInterface

class KryptonRenderer : RendererInterface {
    override fun getRendererId(): String = "krypton"

    override fun getUniqueIdentifier(): String = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d"

    override fun getRendererName(): String = "Krypton Wrapper (OpenGL ES 3)"

    override fun getRendererEnv(): Lazy<Map<String, String>> = lazy {
        mapOf()
    }

    override fun getDlopenLibrary(): Lazy<List<String>> = lazy { emptyList() }

    override fun getRendererLibrary(): String = "libkrypton.so"
}
