package com.craftstudio.launcher.feature.background

import android.content.Context
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.craftstudio.launcher.InfoDistributor
import com.craftstudio.launcher.R
import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.utils.path.PathManager
import com.craftstudio.launcher.utils.file.FileTools.Companion.mkdirs
import com.craftstudio.launcher.utils.image.ImageUtils.Companion.isImage
import com.craftstudio.launcher.Tools
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.Properties

object BackgroundManager {
    private val FILE_BACKGROUND_PROPERTIES: File = File(PathManager.DIR_DATA, "background.properties")
    const val NULL: String = "null"

    private val defaultProperties: Properties
        get() {
            val properties = Properties()
            properties.setProperty(BackgroundType.MAIN_MENU.name, NULL)
            properties.setProperty(BackgroundType.CUSTOM_CONTROLS.name, NULL)
            properties.setProperty(BackgroundType.IN_GAME.name, NULL)
            return properties
        }

    val properties: Properties
        get() {
            FILE_BACKGROUND_PROPERTIES.apply {
                if (!exists()) {
                    return@apply
                }

                val properties = Properties()
                runCatching {
                    FileReader(this).use { fileReader ->
                        properties.load(fileReader)
                    }
                }.getOrElse { e ->
                    Logging.e("BackgroundManager", Tools.printToString(e))
                    return@apply
                }

                return properties
            }

            return defaultProperties
        }

    @JvmStatic
    fun setBackgroundImage(
        context: Context,
        backgroundType: BackgroundType,
        backgroundView: ImageView,
        callback: CallbackDrawableImageViewTarget.Callback? = null
    ) {
        backgroundView.setImageDrawable(
            ContextCompat.getDrawable(context, R.color.background_app)
        )

        val backgroundImage = getBackgroundImage(backgroundType) ?: run {
            callback?.callback(false)
            return
        }

        Glide.with(context).load(backgroundImage)
            .override(backgroundView.width, backgroundView.height)
            .centerCrop()
            .into(CallbackDrawableImageViewTarget(backgroundView, callback))
    }

    @JvmStatic
    fun clearBackgroundImage(
        backgroundView: ImageView
    ) {
        backgroundView.background = null
        backgroundView.setImageDrawable(null)
    }

    @JvmStatic
    fun hasBackgroundImage(backgroundType: BackgroundType): Boolean {
        val pngName = properties[backgroundType.name] as String?
        return pngName != null && pngName != NULL
    }

    fun getBackgroundImage(backgroundType: BackgroundType): File? {
        if (!hasBackgroundImage(backgroundType)) return null

        val pngName = properties[backgroundType.name] as String

        val backgroundImage = File(PathManager.DIR_BACKGROUND, pngName)
        if (!backgroundImage.exists() || !isImage(backgroundImage)) return null
        return backgroundImage
    }

    private fun saveProperties(properties: Properties) {
        PathManager.DIR_BACKGROUND.apply {
            if (!exists()) mkdirs(this)
        }

        runCatching {
            properties.store(
                FileWriter(FILE_BACKGROUND_PROPERTIES),
                "${InfoDistributor.APP_NAME} Background Properties File"
            )
        }.getOrElse { e -> Logging.e("saveProperties", Tools.printToString(e)) }
    }

    fun saveProperties(map: Map<BackgroundType, String>) {
        val properties = Properties()
        properties.setProperty(
            BackgroundType.MAIN_MENU.name,
            map[BackgroundType.MAIN_MENU] ?: NULL
        )
        properties.setProperty(
            BackgroundType.CUSTOM_CONTROLS.name,
            map[BackgroundType.CUSTOM_CONTROLS] ?: NULL
        )
        properties.setProperty(
            BackgroundType.IN_GAME.name,
            map[BackgroundType.IN_GAME] ?: NULL
        )

        saveProperties(properties)
    }
}