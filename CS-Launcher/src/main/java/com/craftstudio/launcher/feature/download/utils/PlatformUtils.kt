package com.craftstudio.launcher.feature.download.utils

import com.craftstudio.launcher.feature.download.Filters
import com.craftstudio.launcher.feature.download.enums.Classify
import com.craftstudio.launcher.utils.stringutils.StringUtils.containsChinese
import com.craftstudio.launcher.utils.stringutils.StringUtilsKt
import com.craftstudio.launcher.modloaders.modpacks.api.ApiHandler
import org.jackhuang.hmcl.ui.versions.ModTranslations
import org.jackhuang.hmcl.util.StringUtils

class PlatformUtils {
    companion object {
        fun createCurseForgeApi() = ApiHandler(
            "https://api.curseforge.com/v1",
            "" // Humne yahan CURSEFORGE_API_KEY ko khali dabba "" kar diya hai
        )

        fun searchModLikeWithChinese(
            filters: Filters,
            isMod: Boolean
        ): String? {
            if (!containsChinese(filters.name)) return null
            val classify = if (isMod) Classify.MOD else Classify.MODPACK

            val englishSearchFiltersSet: MutableSet<String> = HashSet(16)

            for ((count, mod) in ModTranslations.getTranslationsByRepositoryType(classify)
                .searchMod(filters.name).withIndex()
            ) {
                for (englishWord in StringUtils.tokenize(if (StringUtilsKt.isNotBlank(mod.subname)) mod.subname else mod.name)) {
                    if (englishSearchFiltersSet.contains(englishWord)) continue
                    englishSearchFiltersSet.add(englishWord)
                }
                if (count >= 3) break
            }

            return englishSearchFiltersSet.joinToString(" ")
        }

        inline fun <T> ApiHandler.safeRun(block: ApiHandler.() -> T): T? =
            runCatching(block).getOrNull()
    }
}
