package com.craftstudio.launcher.auth.offline

import android.content.Context
import com.craftstudio.launcher.feature.log.Logging

/**
 * Offline account skin management — localhost Yggdrasil server disabled.
 * All skin/cape localhost rendering has been removed.
 */
object OfflineAccount {
    fun initialize(context: Context, username: String, uuid: String): YggdrasilServer? {
        Logging.i("OfflineAccount", "Localhost skin server disabled — skipping initialization")
        return null
    }

    fun getAuthlibArgs(): List<String> = emptyList()

    fun close() {
        Logging.i("OfflineAccount", "Localhost skin server disabled — no-op")
    }
}
