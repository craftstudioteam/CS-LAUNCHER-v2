package com.craftstudio.launcher.auth.offline

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.security.MessageDigest

/**
 * Wrapper for texture image data (skin or cape PNG/GIF).
 * Stores raw bytes and computed SHA-256 hash.
 */
data class Texture(
    val data: ByteArray,
    val hash: String
) {
    companion object {
        /**
         * Load texture from input stream (PNG/GIF).
         * Computes SHA-256 hash of the image data.
         */
        fun loadTexture(input: InputStream): Texture {
            val bytes = input.readBytes()
            val hash = computeHash(bytes)
            return Texture(bytes, hash)
        }

        private fun computeHash(bytes: ByteArray): String {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            return digest.joinToString("") { "%02x".format(it) }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Texture
        if (!data.contentEquals(other.data)) return false
        if (hash != other.hash) return false
        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + hash.hashCode()
        return result
    }

    /**
     * Convert raw bytes to Android Bitmap for display/rendering.
     */
    fun toBitmap(): Bitmap? {
        return BitmapFactory.decodeByteArray(data, 0, data.size)
    }
}

/**
 * Loaded skin + cape for a player.
 */
data class LoadedSkin(
    val textureModel: TextureModel,
    val skin: Texture?,
    val cape: Texture?
)
