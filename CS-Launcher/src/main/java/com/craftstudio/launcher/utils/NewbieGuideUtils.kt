package com.craftstudio.launcher.utils

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.getkeepsafe.taptargetview.TapTarget
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.craftstudio.launcher.R
import com.craftstudio.launcher.feature.log.Logging.e
import com.craftstudio.launcher.utils.path.PathManager
import com.craftstudio.launcher.Tools
import java.io.FileWriter

class NewbieGuideUtils {
    @SuppressLint("NonConstantResourceId")
    companion object {
        private const val TEXT_COLOR = R.color.black_or_white
        private const val TARGET_CIRCLE_COLOR = R.color.background_menu_element
        private val NEWBIE_TAGS: MutableList<String> = ArrayList()

        init {
            PathManager.FILE_NEWBIE_GUIDE.apply {
                runCatching {
                    if (!exists()) Tools.write(absolutePath, "[]")

                    val read = Tools.read(this).trim()
                    val jsonArray = if (read.isEmpty()) {
                        JsonArray()
                    } else {
                        val parsed = JsonParser.parseString(read)
                        if (parsed != null && parsed.isJsonArray) parsed.asJsonArray else JsonArray()
                    }
                    val tags: MutableList<String> = ArrayList()
                    for (jsonElement in jsonArray) {
                        tags.add(jsonElement.asString)
                    }
                    if (tags.isNotEmpty()) NEWBIE_TAGS.addAll(tags)
                }.getOrElse { e ->
                    e("Newbie Guide Utils", Tools.printToString(e))
                }
            }
        }

        @JvmStatic
        fun showOnlyOne(tag: String): Boolean {
            println(tag)
            if (!NEWBIE_TAGS.contains(tag)) {
                NEWBIE_TAGS.add(tag)
                saveTags()
                return false
            }
            return true
        }

        private fun saveTags() {
            val jsonArray = JsonArray()
            for (tag in NEWBIE_TAGS) {
                println(tag)
                jsonArray.add(tag)
            }
            runCatching {
                FileWriter(PathManager.FILE_NEWBIE_GUIDE).use { fileWriter ->
                    println(jsonArray)
                    Gson().toJson(jsonArray, fileWriter)
                }
            }.getOrElse { e -> e("Write Newbie Guide Tags", Tools.printToString(e)) }
        }

        @JvmStatic
        fun getSimpleTarget(context: Context, view: View?, title: String, desc: String): TapTarget {
            return TapTarget.forView(view, title, desc)
                .titleTextColor(TEXT_COLOR)
                .descriptionTextColor(TEXT_COLOR)
                .targetCircleColorInt(ContextCompat.getColor(context, TARGET_CIRCLE_COLOR))
        }

        @JvmStatic
        fun getSimpleTarget(context: Context, view: View?, title: String): TapTarget {
            return TapTarget.forView(view, title)
                .titleTextColor(TEXT_COLOR)
                .targetCircleColorInt(ContextCompat.getColor(context, TARGET_CIRCLE_COLOR))
        }
    }
}