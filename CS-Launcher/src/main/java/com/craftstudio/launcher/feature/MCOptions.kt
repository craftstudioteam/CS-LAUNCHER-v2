package com.craftstudio.launcher.feature

import android.content.Context
import android.os.Build
import android.os.FileObserver
import com.craftstudio.launcher.event.single.MCOptionChangeEvent
import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.feature.version.Version
import com.craftstudio.launcher.Tools
import org.greenrobot.eventbus.EventBus
import org.lwjgl.glfw.CallbackBridge.windowHeight
import org.lwjgl.glfw.CallbackBridge.windowWidth
import java.io.File
import java.io.IOException

object MCOptions {
    private val parameterMap = mutableMapOf<String, String>()
    private var fileObserver: FileObserver? = null
    private lateinit var versionGetter: MinecraftVersionGetter

    /**
     * 初始化 MCOptions
     * 检查 options.txt 是否存在，如果不存在，将会复制一份默认的 options.txt 文件
     */
    fun setup(context: Context, versionGetter: MinecraftVersionGetter) {
        this.versionGetter = versionGetter
        parameterMap.clear()
        fileObserver?.stopWatching()
        fileObserver = null

        getOptionsFile().apply {
            if (!exists()) {
                try {
                    Tools.copyAssetFile(
                        context,
                        "options.txt",
                        versionGetter.getVersion().getGameDir().absolutePath,
                        false
                    )
                } catch (e: Exception) {
                    Logging.e("MCOptions", "Failed to copy the default options.txt file.", e)
                }
            }
        }

        load()
    }

    private fun load() {
        val optionFile = getOptionsFile().apply {
            if (!exists()) {
                try {
                    createNewFile()
                } catch (e: IOException) {
                    Logging.e("MCOptions", Tools.printToString(e))
                }
            }
        }

        if (fileObserver == null) {
            setupFileObserver()
        }

        parameterMap.clear()

        try {
            optionFile.forEachLine { line ->
                line.indexOf(':').takeIf { it >= 0 }?.let { colonIndex ->
                    parameterMap[line.substring(0, colonIndex)] = line.substring(colonIndex + 1)
                } ?: Logging.w("MCOptions", "Invalid line format: $line")
            }
        } catch (e: IOException) {
            Logging.w("MCOptions", "Could not load options.txt", e)
        }
    }

    fun set(key: String, value: String) {
        parameterMap[key] = value
    }

    fun get(key: String): String? = parameterMap[key]

    fun containsKey(key: String): Boolean = key in parameterMap

    fun save() {
        getOptionsFile().takeIf { it.exists() }?.let { optionsFile ->
            val optionsString = parameterMap.entries.joinToString("\n") { "${it.key}:${it.value}" }
            try {
                fileObserver?.stopWatching()
                optionsFile.writeText(optionsString)
            } catch (e: IOException) {
                Logging.w("MCOptions", "Could not save options.txt", e)
            } finally {
                fileObserver?.startWatching()
            }
        }
    }

    val mcScale: Int
        get() {
            val guiScale = get("guiScale")?.toIntOrNull() ?: 0
            val scale = minOf(windowWidth / 320, windowHeight / 240).coerceAtLeast(1)
            return if (guiScale == 0 || scale < guiScale) scale else guiScale
        }

    private fun getOptionsFile() = File(versionGetter.getVersion().getGameDir(), "options.txt")

    private fun setupFileObserver() {
        fileObserver = createFileObserver(getOptionsFile()).apply {
            startWatching()
        }
    }

    private fun createFileObserver(file: File): FileObserver {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            object : FileObserver(file, MODIFY) {
                override fun onEvent(event: Int, path: String?) {
                    handleFileChange()
                }
            }
        } else {
            object : FileObserver(file.absolutePath, MODIFY) {
                override fun onEvent(event: Int, path: String?) {
                    handleFileChange()
                }
            }
        }
    }

    private fun handleFileChange() {
        load()
        EventBus.getDefault().post(MCOptionChangeEvent())
    }

    /**
     * 这个接口用于获取 Minecraft 版本信息
     */
    fun interface MinecraftVersionGetter {
        fun getVersion(): Version
    }
}