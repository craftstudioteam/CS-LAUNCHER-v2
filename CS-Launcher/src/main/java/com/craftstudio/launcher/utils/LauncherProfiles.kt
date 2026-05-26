package com.craftstudio.launcher.utils

import com.craftstudio.launcher.feature.customprofilepath.ProfilePathHome
import com.craftstudio.launcher.feature.log.Logging
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException

class LauncherProfiles {
    companion object {
        /**
         * 写入一个默认的 launcher_profiles.json 文件，不存在将会导致 Forge、NeoForge 等无法正常安装
         */
        @JvmStatic
        fun generateLauncherProfiles() {
            runCatching {
                File(ProfilePathHome.getGameHome(), "launcher_profiles.json").apply {
                    if (!exists()) {
                        if (parentFile?.exists() == false) parentFile?.mkdirs()
                        if (!createNewFile()) throw IOException("Failed to create launcher_profiles.json file!")
                        //开始写入内容
                        val profilesJsonString = """{"profiles":{"default":{"lastVersionId":"latest-release"}},"selectedProfile":"default"}""".trimIndent()
                        FileUtils.write(this, profilesJsonString)
                        Logging.i(
                            "Write launcher_profiles.json",
                            "The content has already been written! \r\nFile Location: $absolutePath\r\nContents: $profilesJsonString"
                        )
                    }
                }
            }.getOrElse { e ->
                Logging.e("Write launcher_profiles.json", "Unable to generate launcher_profiles.json file!", e)
            }
        }
    }
}