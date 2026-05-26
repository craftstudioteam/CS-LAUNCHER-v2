package com.craftstudio.launcher.auth.offline

import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.utils.path.UrlManager
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.File

/**
 * Skin type definition for offline accounts.
 * Matches FCL's Skin enum structure.
 */
data class Skin(
    val type: Type,
    val textureModel: TextureModel = TextureModel.STEVE,
    val localSkinPath: String? = null,
    val localCapePath: String? = null,
    val skinUrl: String? = null
) {
    enum class Type {
        DEFAULT,                // No custom skin
        STEVE,                  // Default Steve model
        ALEX,                   // Default Alex (slim) model
        LOCAL_FILE,             // Custom file-based skin
        CUSTOM_SKIN_LOADER_API  // URL-based skin (FCL style)
    }

    /**
     * Load this skin's textures asynchronously.
     * Returns null if type is DEFAULT (no custom loading).
     */
    fun loadTextures(): LoadedSkin? {
        return when (type) {
            Type.DEFAULT -> null
            Type.STEVE -> LoadedSkin(TextureModel.STEVE, null, null)
            Type.ALEX -> LoadedSkin(TextureModel.ALEX, null, null)
            Type.LOCAL_FILE -> {
                var skin: Texture? = null
                var cape: Texture? = null
                
                localSkinPath?.let { path ->
                    try {
                        val fis = java.io.FileInputStream(path)
                        skin = Texture.loadTexture(fis)
                        fis.close()
                    } catch (e: Exception) {
                        // Skin load failed, continue without it
                    }
                }
                
                localCapePath?.let { path ->
                    try {
                        val fis = java.io.FileInputStream(path)
                        cape = Texture.loadTexture(fis)
                        fis.close()
                    } catch (e: Exception) {
                        // Cape load failed, continue without it
                    }
                }
                
                LoadedSkin(textureModel, skin, cape)
            }
            Type.CUSTOM_SKIN_LOADER_API -> {
                var skin: Texture? = null
                var cape: Texture? = null
                
                skinUrl?.let { url ->
                    try {
                        val client = UrlManager.createOkHttpClient()
                        val secureUrl = url.replace("http://", "https://")
                        val request = Request.Builder().url(secureUrl).build()
                        val response = client.newCall(request).execute()
                        
                        if (response.isSuccessful) {
                            val bytes = response.body?.bytes() ?: ByteArray(0)
                            if (bytes.isNotEmpty()) {
                                skin = Texture.loadTexture(ByteArrayInputStream(bytes))
                            }
                        }
                        response.close()
                    } catch (e: Exception) {
                        Logging.e("Skin", "Failed to load skin from URL: $url", e)
                    }
                }
                
                localCapePath?.let { path ->
                    try {
                        val fis = java.io.FileInputStream(path)
                        cape = Texture.loadTexture(fis)
                        fis.close()
                    } catch (e: Exception) {
                        // Cape load failed, continue without it
                    }
                }
                
                LoadedSkin(textureModel, skin, cape)
            }
        }
    }
    
    /**
     * Check if this skin requires authlib-injector (Yggdrasil server).
     * URL-based skins need the server to serve textures to the game.
     */
    fun requiresAuthlibInjector(): Boolean {
        return type == Type.CUSTOM_SKIN_LOADER_API && skinUrl != null
    }
    
    /**
     * Get the display URL for UI preview (Glide/Picasso compatible).
     */
    fun getDisplayUrl(): String? {
        return when (type) {
            Type.CUSTOM_SKIN_LOADER_API -> skinUrl?.replace("http://", "https://")
            Type.STEVE -> "https://minotar.net/skin/MHF_Steve"
            Type.ALEX -> "https://minotar.net/skin/MHF_Alex"
            else -> null
        }
    }
}
