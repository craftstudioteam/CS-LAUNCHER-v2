package com.craftstudio.launcher.utils.skin

import android.content.Context
import com.craftstudio.launcher.auth.offline.Skin
import com.craftstudio.launcher.auth.offline.TextureModel

object SkinPreferenceStore {
    private const val PREFS_NAME = "local_skin_prefs"
    private const val KEY_SKIN_PREFIX = "skin_path_"
    private const val KEY_CAPE_PREFIX = "cape_path_"
    private const val KEY_TEXTURE_MODEL_PREFIX = "texture_model_"
    private const val KEY_SKIN_URL_PREFIX = "skin_url_"
    private const val KEY_SKIN_TYPE_PREFIX = "skin_type_"

    /**
     * Save local skin file path for an account.
     */
    fun saveSkinPath(context: Context, username: String, path: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SKIN_PREFIX + username.lowercase(), path)
            .apply()
    }

    /**
     * Save local cape file path for an account.
     */
    fun saveCapePath(context: Context, username: String, path: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CAPE_PREFIX + username.lowercase(), path)
            .apply()
    }

    /**
     * Save texture model (STEVE or ALEX) for an account.
     */
    fun saveTextureModel(context: Context, username: String, model: TextureModel) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TEXTURE_MODEL_PREFIX + username.lowercase(), model.name)
            .apply()
    }

    /**
     * Save skin URL for an account (CUSTOM_SKIN_LOADER_API type).
     */
    fun saveSkinUrl(context: Context, username: String, url: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SKIN_URL_PREFIX + username.lowercase(), url)
            .putString(KEY_SKIN_TYPE_PREFIX + username.lowercase(), Skin.Type.CUSTOM_SKIN_LOADER_API.name)
            .apply()
    }

    /**
     * Clear skin URL for an account (revert to local file or default).
     */
    fun clearSkinUrl(context: Context, username: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SKIN_URL_PREFIX + username.lowercase())
            .remove(KEY_SKIN_TYPE_PREFIX + username.lowercase())
            .apply()
    }

    /**
     * Get local skin file path for an account.
     */
    fun getSkinPath(context: Context, username: String): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SKIN_PREFIX + username.lowercase(), null)
    }

    /**
     * Get local cape file path for an account.
     */
    fun getCapePath(context: Context, username: String): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CAPE_PREFIX + username.lowercase(), null)
    }

    /**
     * Get texture model (STEVE or ALEX) for an account. Defaults to STEVE.
     */
    fun getTextureModel(context: Context, username: String): TextureModel {
        val modelStr = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TEXTURE_MODEL_PREFIX + username.lowercase(), null)
        return try {
            TextureModel.valueOf(modelStr ?: "STEVE")
        } catch (e: Exception) {
            TextureModel.STEVE
        }
    }

    /**
     * Get skin URL for an account. Returns null if not set.
     */
    fun getSkinUrl(context: Context, username: String): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SKIN_URL_PREFIX + username.lowercase(), null)
    }

    /**
     * Get skin type for an account. Defaults to LOCAL_FILE if path exists, else DEFAULT.
     */
    fun getSkinType(context: Context, username: String): Skin.Type {
        val typeStr = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SKIN_TYPE_PREFIX + username.lowercase(), null)
        return try {
            typeStr?.let { Skin.Type.valueOf(it) } ?: run {
                // Fallback logic
                val skinPath = getSkinPath(context, username)
                val skinUrl = getSkinUrl(context, username)
                when {
                    skinUrl != null -> Skin.Type.CUSTOM_SKIN_LOADER_API
                    skinPath != null -> Skin.Type.LOCAL_FILE
                    else -> Skin.Type.DEFAULT
                }
            }
        } catch (e: Exception) {
            Skin.Type.DEFAULT
        }
    }

    /**
     * Clear all skin preferences for an account.
     */
    fun clearSkinPreferences(context: Context, username: String) {
        val key = username.lowercase()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SKIN_PREFIX + key)
            .remove(KEY_CAPE_PREFIX + key)
            .remove(KEY_TEXTURE_MODEL_PREFIX + key)
            .remove(KEY_SKIN_URL_PREFIX + key)
            .remove(KEY_SKIN_TYPE_PREFIX + key)
            .apply()
    }
}
