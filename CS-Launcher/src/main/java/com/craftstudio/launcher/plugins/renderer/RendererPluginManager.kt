package com.craftstudio.launcher.plugins.renderer

import android.content.Context
import android.content.pm.ApplicationInfo
import com.craftstudio.launcher.R
import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.feature.update.UpdateUtils
import com.craftstudio.launcher.renderer.Renderers
import com.craftstudio.launcher.utils.path.PathManager
import com.craftstudio.launcher.utils.stringutils.StringUtilsKt
import com.craftstudio.launcher.Architecture
import com.craftstudio.launcher.Tools
import com.craftstudio.launcher.utils.ZipUtils
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipFile

/**
 * FCL、CS-Launcher 渲染器插件，同时支持使用本地渲染器插件
 * [FCL Renderer Plugin](https://github.com/FCL-Team/FCLRendererPlugin)
 */
object RendererPluginManager {
    private val rendererPluginList: MutableList<RendererPlugin> = mutableListOf()
    private val apkRendererPluginList: MutableList<ApkRendererPlugin> = mutableListOf()
    private val localRendererPluginList: MutableList<LocalRendererPlugin> = mutableListOf()

    /**
     * 获取当前渲染器插件加载的所有渲染器
     */
    @JvmStatic
    fun getRendererList() = rendererPluginList

    /**
     * 移除某些已加载的渲染器
     */
    @JvmStatic
    fun removeRenderer(rendererPlugins: Collection<RendererPlugin>) {
        rendererPluginList.removeAll(rendererPlugins)
    }

    /**
     * 获取当前本地渲染器插件加载的所有渲染器
     */
    @JvmStatic
    fun getAllLocalRendererList() = localRendererPluginList

    /**
     * @return 是可用的
     */
    @JvmStatic
    fun isAvailable(): Boolean {
        return rendererPluginList.isNotEmpty()
    }

    /**
     * 当前选择的渲染器插件所加载的渲染器
     * 根据总渲染器管理者选择的渲染器的渲染器唯一标识符进行判断
     */
    @JvmStatic
    val selectedRendererPlugin: RendererPlugin?
        get() {
            val currentRenderer = runCatching {
                Renderers.currentRenderer.getUniqueIdentifier()
            }.getOrNull()
            return rendererPluginList.find { it.uniqueIdentifier == currentRenderer }
        }

    /**
     * 清除渲染器插件
     */
    fun clearPlugin() {
        rendererPluginList.clear()
        apkRendererPluginList.clear()
        localRendererPluginList.clear()
    }

    /**
     * 当前渲染器插件是否带有配置项（软件式插件、白名单包名）
     */
    @JvmStatic
    fun getConfigurablePluginOrNull(rendererUniqueIdentifier: String): RendererPlugin? {
        val renderer = apkRendererPluginList.find { it.uniqueIdentifier == rendererUniqueIdentifier }
        return renderer?.takeIf { it.packageName in setOf(
                "com.bzlzhh.plugin.ngg",
                "com.bzlzhh.plugin.ngg.angleless",
                "com.fcl.plugin.mobileglues"
            ) }
    }

    /**
     * 解析 CS-Launcher、FCL 渲染器插件
     */
    fun parseApkPlugin(context: Context, info: ApplicationInfo) {
        if (info.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
            val metaData = info.metaData ?: return
            if (
                metaData.getBoolean("fclPlugin", false) ||
                metaData.getBoolean("craftStudioRendererPlugin", false)
            ) {
                val rendererString = metaData.getString("renderer") ?: return
                val des = metaData.getString("des") ?: return
                val pojavEnvString = metaData.getString("pojavEnv") ?: return
                val nativeLibraryDir = info.nativeLibraryDir
                val renderer = rendererString.split(":")

                var rendererId: String = renderer[0]
                val envList = mutableMapOf<String, String>()
                val dlopenList = mutableListOf<String>()
                pojavEnvString.split(":").forEach { envString ->
                    if (envString.contains("=")) {
                        val stringList = envString.split("=")
                        val key = stringList[0]
                        val value = stringList[1]
                        when (key) {
                            "POJAV_RENDERER" -> rendererId = value
                            "DLOPEN" -> {
                                value.split(",").forEach { lib ->
                                    dlopenList.add(lib)
                                }
                            }
                            "LIB_MESA_NAME", "MESA_LIBRARY" -> envList[key] = "$nativeLibraryDir/$value"
                            else -> envList[key] = value
                        }
                    }
                }

                val packageName = info.packageName

                val plugin = ApkRendererPlugin(
                    rendererId,
                    "$des (${
                        context.getString(
                            R.string.setting_renderer_from_plugins,
                            runCatching {
                                context.packageManager.getApplicationLabel(info)
                            }.getOrElse {
                                context.getString(R.string.generic_unknown)
                            }
                        )
                    })",
                    packageName,
                    renderer[1],
                    renderer[2].progressEglName(nativeLibraryDir),
                    nativeLibraryDir,
                    envList,
                    dlopenList,
                    packageName
                )

                rendererPluginList.add(plugin)
                apkRendererPluginList.add(plugin)
            }
        }
    }

    /**
     * 从本地 `/files/renderer_plugins/` 目录下尝试解析渲染器插件
     * @return 是否是符合要求的插件
     *
     * 渲染器文件夹格式
     * renderer_plugins/
     * ----文件夹名称/
     * --------renderer_config.json (存放渲染器具体信息的配置文件)
     * --------libs/ (渲染器`.so`文件的存放目录)
     * ------------arm64-v8a/ (arm64架构)
     * ----------------渲染器库文件.so
     * ------------armeabi-v7a/ (arm32架构)
     * ----------------渲染器库文件.so
     * ------------x86/ (x86架构)
     * ----------------渲染器库文件.so
     * ------------x86_64/ (x86_64架构)
     * ----------------渲染器库文件.so
     */
    fun parseLocalPlugin(context: Context, directory: File): Boolean {
        val archModel: String = UpdateUtils.getArchModel(Architecture.getDeviceArchitecture()) ?: return false
        val libsDirectory: File = File(directory, "libs/$archModel").takeIf { it.exists() && it.isDirectory } ?: return false
        val rendererConfigFile: File = File(directory, "config").takeIf { it.exists() && it.isFile } ?: return false
        val rendererConfig: RendererConfig = runCatching {
            Tools.GLOBAL_GSON.fromJson(readLocalRendererPluginConfig(rendererConfigFile), RendererConfig::class.java)
        }.getOrElse { e ->
            Logging.e("LocalRendererPlugin", "Failed to parse the configuration file", e)
            return false
        }
        val uniqueIdentifier = directory.name
        rendererConfig.run {
            val libPath = libsDirectory.absolutePath

            val plugin = LocalRendererPlugin(
                rendererId,
                "$rendererDisplayName (${
                    context.getString(
                        R.string.setting_renderer_from_plugins,
                        uniqueIdentifier
                    )
                })",
                uniqueIdentifier,
                glName,
                eglName.progressEglName(libPath),
                libPath,
                pojavEnv.filter { it.key != "POJAV_RENDERER" },
                dlopenList ?: emptyList(),
                directory
            )

            rendererPluginList.add(plugin)
            localRendererPluginList.add(plugin)
        }
        return true
    }

    private fun String.progressEglName(libPath: String): String =
        if (startsWith("/")) "$libPath$this"
        else this

    private fun readLocalRendererPluginConfig(configFile: File): String {
        return FileInputStream(configFile).use { fileInputStream ->
            DataInputStream(fileInputStream).use { dataInputStream ->
                dataInputStream.readUTF()
            }
        }
    }

    /**
     * 导入本地渲染器插件
     */
    fun importLocalRendererPlugin(pluginFile: File): Boolean {
        if (!pluginFile.exists() || !pluginFile.isFile) {
            Logging.i("importLocalRendererPlugin", "The compressed file does not exist or is not a valid file.")
            return false
        }

        return try {
            ZipFile(pluginFile).use { pluginZip ->
                val configEntry = pluginZip.entries().asSequence().find { it.name == "config" }
                    ?: throw IllegalArgumentException("The plugin package does not meet the requirements!")

                pluginZip.getInputStream(configEntry).use { inputStream ->
                    DataInputStream(inputStream).use { dataInputStream ->
                        val configContent = dataInputStream.readUTF()
                        Tools.GLOBAL_GSON.fromJson(configContent, RendererConfig::class.java)
                    }
                }

                val pluginFolder = File(
                    PathManager.DIR_INSTALLED_RENDERER_PLUGIN,
                    StringUtilsKt.generateUniqueUUID(
                        { string ->
                            string.replace("-", "").substring(0, 8)
                        },
                        { uuid ->
                            File(PathManager.DIR_INSTALLED_RENDERER_PLUGIN, uuid).exists()
                        }
                    )
                )

                ZipUtils.zipExtract(pluginZip, "", pluginFolder)
            }
            true
        } catch (e: Exception) {
            Logging.i("importLocalRendererPlugin", "Error: ${e.message}")
            false
        }
    }
}