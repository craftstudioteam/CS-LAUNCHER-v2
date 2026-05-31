package com.craftstudio.launcher.renderer

import android.content.Context
import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.setting.AllSettings

enum class Renderers(
    val id: String,
    val displayName: String,
    val library: String,
    val egl: String? = null
) : RendererInterface {
    ZINK(
        id = "vulkan_zink",
        displayName = "Zink (Vulkan)",
        library = "libOSMesa.so"
    ),
    GL4ES(
        id = "opengles2", 
        displayName = "Holy GL4ES",
        library = "libgl4es_114.so"
    ),
    LTW(
        id = "ltw_render",
        displayName = "LTW (OpenGL ES 3)",
        library = "libltw.so",
        egl = "libltw.so"
    ),
    GALLIUM_GENERIC(
        id = "gallium_generic",
        displayName = "Gallium Generic (Mesa)",
        library = "libOSMesa.so"
    ),
    FCL_GL4ES(
        id = "fclplugin_gl4es",
        displayName = "Holy GL4ES (FCL Plugin)",
        library = "libgl4es_114.so"
    ),
    FCL_VIRGL(
        id = "fclplugin_virgl",
        displayName = "Holy VirGL (FCL Plugin)",
        library = "libOSMesa.so"
    );

    override fun getRendererId(): String = id
    override fun getUniqueIdentifier(): String = id
    override fun getRendererName(): String = displayName
    override fun getRendererEnv(): Lazy<Map<String, String>> = lazy { emptyMap() }
    override fun getDlopenLibrary(): Lazy<List<String>> = lazy { emptyList() }
    override fun getRendererLibrary(): String = library
    override fun getRendererEGL(): String? = egl

    companion object {
        @JvmField
        val INSTANCE = this

        @JvmStatic
        var currentRenderer: Renderers = ZINK
        
        @JvmStatic
        fun fromId(id: String): Renderers {
            return values().find { it.id == id } ?: ZINK
        }
        
        @JvmStatic
        fun getRendererList(): List<Renderers> = values().toList()

        @JvmStatic
        fun init(reset: Boolean = false) {
            currentRenderer = fromId(AllSettings.renderer.getValue())
            Logging.i("RENDERER", "Initialized with: " + currentRenderer.id)
        }

        @JvmStatic
        fun isCurrentRendererValid(): Boolean = true

        @JvmStatic
        fun setCurrentRenderer(context: Context, id: String, retryToFirstOnFailure: Boolean = true) {
            currentRenderer = fromId(id)
            AllSettings.renderer.put(currentRenderer.id).save()
            Logging.i("RENDERER", "Current renderer set & saved to ${currentRenderer.id}")
        }

        @JvmStatic
        fun getCompatibleRenderers(context: Context): Pair<RenderersList, List<Renderers>> {
            val list = values().toList()
            val ids = list.map { it.id }
            val names = list.map { it.displayName }
            return Pair(RenderersList(ids, names), list)
        }

        @JvmStatic
        fun addRenderer(renderer: RendererInterface): Boolean {
            return false // Stubs for PluginLoader.kt
        }
    }
}
