package com.craftstudio.launcher.renderer

import android.content.Context
import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.setting.AllSettings

enum class Renderers(
    val rendererId: String,
    val displayName: String,
    val rendererLibrary: String,
    val rendererEGL: String? = null
) : RendererInterface {
    ZINK(
        rendererId = "vulkan_zink",
        displayName = "Zink (Vulkan)",
        rendererLibrary = "libOSMesa.so"
    ),
    GL4ES(
        rendererId = "opengles2", 
        displayName = "Holy GL4ES",
        rendererLibrary = "libgl4es_114.so"
    ),
    LTW(
        rendererId = "ltw_render",
        displayName = "LTW (OpenGL ES 3)",
        rendererLibrary = "libltw.so",
        rendererEGL = "libltw.so"
    ),
    MOBILEGLUES(
        rendererId = "mobileglues",
        displayName = "MobileGlues (OpenGL ES 3)",
        rendererLibrary = "libMobileGlues.so"
    ),
    KRYPTON(
        rendererId = "krypton",
        displayName = "Krypton Wrapper (OpenGL ES 3)",
        rendererLibrary = "libkrypton.so"
    ),
    GALLIUM_GENERIC(
        rendererId = "gallium_generic",
        displayName = "Gallium Generic (Mesa)",
        rendererLibrary = "libOSMesa.so"
    ),
    FCL_GL4ES(
        rendererId = "fclplugin_gl4es",
        displayName = "Holy GL4ES (FCL Plugin)",
        rendererLibrary = "libgl4es_114.so"
    ),
    FCL_VIRGL(
        rendererId = "fclplugin_virgl",
        displayName = "Holy VirGL (FCL Plugin)",
        rendererLibrary = "libOSMesa.so"
    );

    override fun getRendererId(): String = rendererId
    override fun getUniqueIdentifier(): String = rendererId
    override fun getRendererName(): String = displayName
    override fun getRendererEnv(): Lazy<Map<String, String>> = lazy { emptyMap() }
    override fun getDlopenLibrary(): Lazy<List<String>> = lazy { emptyList() }
    override fun getRendererLibrary(): String = rendererLibrary
    override fun getRendererEGL(): String? = rendererEGL

    companion object {
        @JvmField
        val INSTANCE = this

        @JvmStatic
        var currentRenderer: Renderers = ZINK
        
        @JvmStatic
        fun fromId(id: String): Renderers {
            return values().find { it.rendererId == id } ?: ZINK
        }
        
        @JvmStatic
        fun getRendererList(): List<Renderers> = values().toList()

        @JvmStatic
        fun init(reset: Boolean = false) {
            currentRenderer = fromId(AllSettings.renderer.getValue())
            Logging.i("RENDERER", "Initialized with: " + currentRenderer.rendererId)
        }

        @JvmStatic
        fun isCurrentRendererValid(): Boolean = true

        @JvmStatic
        fun getCurrentRenderer(): Renderers = currentRenderer

        @JvmStatic
        fun setCurrentRenderer(context: Context, id: String, retryToFirstOnFailure: Boolean = true) {
            currentRenderer = fromId(id)
            AllSettings.renderer.put(currentRenderer.rendererId).save()
            Logging.i("RENDERER", "Current renderer set & saved to ${currentRenderer.rendererId}")
        }

        @JvmStatic
        fun getCompatibleRenderers(context: Context): Pair<RenderersList, List<Renderers>> {
            val list = values().toList()
            val ids = list.map { it.rendererId }
            val names = list.map { it.displayName }
            return Pair(RenderersList(ids, names), list)
        }

        @JvmStatic
        fun addRenderer(renderer: RendererInterface): Boolean {
            return false // Stubs for PluginLoader.kt
        }
    }
}
