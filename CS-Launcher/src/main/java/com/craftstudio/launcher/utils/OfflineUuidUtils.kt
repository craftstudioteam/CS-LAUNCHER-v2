package com.craftstudio.launcher.utils

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID

object OfflineUuidUtils {
    fun fromUsername(username: String): String {
        val md5 = MessageDigest.getInstance("MD5")
        val hash = md5.digest("OfflinePlayer:$username".toByteArray(StandardCharsets.UTF_8))
        hash[6] = (hash[6].toInt() and 0x0f or 0x30).toByte()
        hash[8] = (hash[8].toInt() and 0x3f or 0x80).toByte()
        val bb = java.nio.ByteBuffer.wrap(hash)
        return UUID(bb.long, bb.long).toString()
    }
}
