package com.craftstudio.launcher.utils.stringutils

import java.util.UUID

class StringUtilsKt {
    companion object {
        @JvmStatic
        fun getNonEmptyOrBlank(string: String?): String? {
            return string?.takeIf { it.isNotEmpty() && it.isNotBlank() }
        }

        @JvmStatic
        fun isBlank(string: String?): Boolean = string.isNullOrBlank()

        @JvmStatic
        fun isNotBlank(string: String?): Boolean = string?.isNotBlank() ?: false

        @JvmStatic
        fun isEmptyOrBlank(string: String): Boolean = string.isEmpty() || string.isBlank()

        @JvmStatic
        fun removeSuffix(string: String, suffix: String) = string.removeSuffix(suffix)

        @JvmStatic
        fun removePrefix(string: String, prefix: String) = string.removePrefix(prefix)

        @JvmStatic
        fun decodeUnicode(input: String): String {
            val regex = """\\u([0-9a-fA-F]{4})""".toRegex()
            var result = input
            regex.findAll(input).forEach { match ->
                val unicode = match.groupValues[1]
                val char = Character.toChars(unicode.toInt(16))[0]
                result = result.replace(match.value, char.toString())
            }
            return result
        }

        /**
         * 生成一个唯一UUID，以及防止与已存在的UUID冲突
         * @param processString 若需要操作字符串，可以使用它
         * @param checkForConflict 若需要防止与已存在的UUID冲突，可以用它检查是否有冲突，如果返回true，则递归重新生成一个
         */
        @JvmStatic
        fun generateUniqueUUID(
            processString: ((String) -> String)? = null,
            checkForConflict: ((String) -> Boolean)? = null
        ): String {
            val uuid = UUID.randomUUID().toString().lowercase()
            val progressedUuid = processString?.invoke(uuid) ?: uuid
            return if (checkForConflict?.invoke(progressedUuid) == true) {
                generateUniqueUUID(processString, checkForConflict)
            } else {
                progressedUuid
            }
        }
    }
}