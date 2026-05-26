package com.craftstudio.launcher.feature.firebase

import android.content.Context
import com.craftstudio.launcher.BuildConfig
import com.craftstudio.launcher.feature.log.Logging
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

data class LauncherRemoteContent(
    @JvmField val announcementVisible: Boolean,
    @JvmField val announcementTitleHtml: String,
    @JvmField val announcementBodyHtml: String,
    @JvmField val announcementDate: String,
    val minimumVersionCode: Int,
    val downloadUrl: String,
    val forceUpdateMessageHtml: String,
) {
    fun shouldForceUpdate(currentVersionCode: Int): Boolean {
        return minimumVersionCode > 0 && currentVersionCode < minimumVersionCode
    }
}

fun interface LauncherRemoteContentCallback {
    fun onResult(content: LauncherRemoteContent?)
}

object LauncherRemoteContentRepository {
    @Volatile
    private var cachedContent: LauncherRemoteContent? = null

    @JvmStatic
    fun getCachedContent(): LauncherRemoteContent? = cachedContent

    @JvmStatic
    fun load(context: Context, callback: LauncherRemoteContentCallback) {
        val firebaseApp = ensureFirebaseApp(context)
        if (firebaseApp == null) {
            callback.onResult(cachedContent)
            return
        }

        FirebaseDatabase.getInstance(firebaseApp).reference.child(BuildConfig.FIREBASE_LAUNCHER_PATH)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val content = parseContent(snapshot)
                    if (content != null) {
                        cachedContent = content
                    }
                    callback.onResult(content ?: cachedContent)
                }

                override fun onCancelled(error: DatabaseError) {
                    Logging.e("FirebaseContent", "Failed to load launcher content: ${error.message}")
                    callback.onResult(cachedContent)
                }
            })
    }

    private fun parseContent(snapshot: DataSnapshot): LauncherRemoteContent? {
        if (!snapshot.exists()) return null

        return LauncherRemoteContent(
            announcementVisible = snapshot.child("announcement_visible").getValue(Boolean::class.java) ?: false,
            announcementTitleHtml = snapshot.child("announcement_title_html").getValue(String::class.java).orEmpty(),
            announcementBodyHtml = snapshot.child("announcement_body_html").getValue(String::class.java).orEmpty(),
            announcementDate = snapshot.child("announcement_date").getValue(String::class.java).orEmpty(),
            minimumVersionCode = snapshot.child("minimum_version_code").getValue(Long::class.java)?.toInt() ?: 0,
            downloadUrl = snapshot.child("download_url").getValue(String::class.java).orEmpty(),
            forceUpdateMessageHtml = snapshot.child("force_update_message_html").getValue(String::class.java).orEmpty(),
        )
    }

    private fun ensureFirebaseApp(context: Context): FirebaseApp? {
        FirebaseApp.getApps(context).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }?.let { return it }

        if (
            BuildConfig.FIREBASE_API_KEY.isBlank() ||
            BuildConfig.FIREBASE_APP_ID.isBlank() ||
            BuildConfig.FIREBASE_PROJECT_ID.isBlank() ||
            BuildConfig.FIREBASE_DATABASE_URL.isBlank()
        ) {
            return null
        }

        return try {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApiKey(BuildConfig.FIREBASE_API_KEY)
                    .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                    .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                    .setDatabaseUrl(BuildConfig.FIREBASE_DATABASE_URL)
                    .build()
            )
        } catch (throwable: Throwable) {
            Logging.e("FirebaseContent", "Failed to initialize Firebase", throwable)
            null
        }
    }
}
