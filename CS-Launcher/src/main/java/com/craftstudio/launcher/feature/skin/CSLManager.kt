package com.craftstudio.launcher.feature.skin

import android.content.Context
import android.util.Base64
import com.craftstudio.launcher.utils.skin.SkinPreferenceStore
import java.io.File

object CSLManager {
    private const val CSL_CONFIG_FILE = "CustomSkinLoader.json"
    private const val CSL_ASSET_FOLDER = "components/csl"

    fun injectCSL(context: Context, gameDir: File, username: String, skinPngPath: String?) {
        val modsDir = File(gameDir, "mods").also { it.mkdirs() }
        val configDir = File(gameDir, "CustomSkinLoader").also { it.mkdirs() }

        copyCSLJarFromAssets(context, modsDir)
        File(configDir, CSL_CONFIG_FILE).writeText(buildCSLConfig(skinPngPath))

        skinPngPath?.let {
            SkinPreferenceStore.saveSkinPath(context, username, it)
        }
    }

    private fun buildCSLConfig(skinPngPath: String?): String {
        val skinUrl = skinPngPath?.let { File(it).toURI().toString() }
        return if (skinUrl != null) {
            """
            {
              "enable": true,
              "loadlist": [
                {
                  "name": "LocalSkin",
                  "type": "CustomSkinAPI",
                  "userAgent": "CSLauncher",
                  "skin": "$skinUrl",
                  "cape": null
                }
              ]
            }
            """.trimIndent()
        } else {
            """
            {
              "enable": true,
              "loadlist": []
            }
            """.trimIndent()
        }
    }

    private fun copyCSLJarFromAssets(context: Context, modsDir: File) {
        val selectedAsset = runCatching {
            context.assets.list(CSL_ASSET_FOLDER)
                ?.firstOrNull { it.endsWith(".jar", ignoreCase = true) }
        }.getOrNull() ?: return

        val dest = File(modsDir, selectedAsset)
        if (dest.exists() && dest.length() > 0) return

        runCatching {
            context.assets.open("$CSL_ASSET_FOLDER/$selectedAsset").use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}