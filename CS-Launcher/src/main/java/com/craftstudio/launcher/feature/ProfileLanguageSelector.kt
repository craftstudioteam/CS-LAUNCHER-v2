package com.craftstudio.launcher.feature

import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.feature.version.Version
import com.craftstudio.launcher.setting.AllSettings
import com.craftstudio.launcher.utils.MCVersionRegex
import com.craftstudio.launcher.utils.ZHTools
import org.jackhuang.hmcl.util.versioning.VersionNumber
import org.jackhuang.hmcl.util.versioning.VersionRange

class ProfileLanguageSelector {
    companion object {
        private fun getOlderLanguage(lang: String): String {
            val underscoreIndex = lang.indexOf('_')
            return if (underscoreIndex != -1) {
                //只将下划线后面的字符转换为大写
                val builder = StringBuilder(lang.substring(0, underscoreIndex + 1))
                builder.append(lang.substring(underscoreIndex + 1).uppercase())
                builder.toString()
            } else lang
        }

        private fun getLanguage(minecraftVersion: Version, rawLang: String): String {
            val lang = if (rawLang == "system") ZHTools.getSystemLanguage() else rawLang

            val version: String = minecraftVersion.getVersionInfo()?.minecraftVersion ?: "1.11"

            val versionId = VersionNumber.asVersion(version).canonical
            Logging.i("ProfileLanguageSelector", "Version Id : $versionId")

            return when {
                versionId.contains('.') -> {
                    if (isOlderVersionRelease(versionId)) getOlderLanguage(lang) // 1.10 -
                    else lang
                }
                MCVersionRegex.SNAPSHOT_REGEX.matcher(versionId).matches() -> { // 快照版本 "24w09a" "16w20a"
                    if (isOlderVersionSnapshot(versionId)) getOlderLanguage(lang)
                    else lang
                }
                else -> lang
            }
        }

        private fun isOlderVersionRelease(versionName: String): Boolean {
            return VersionRange.atMost(VersionNumber.asVersion("1.10.2")).contains(VersionNumber.asVersion(versionName))
        }

        private fun isOlderVersionSnapshot(versionName: String): Boolean {
            return VersionRange.atMost(VersionNumber.asVersion("16w32a")).contains(VersionNumber.asVersion(versionName))
        }

        @JvmStatic
        fun setGameLanguage(version: Version, overridden: Boolean) {
            if (MCOptions.containsKey("lang") && !overridden) return
            val language = getLanguage(version, AllSettings.setGameLanguage.getValue())
            Logging.i("ProfileLanguageSelector", "The game language has been set to: $language")
            MCOptions.set("lang", language)
        }
    }
}
