package com.craftstudio.launcher.utils.stringutils

import kotlin.math.min

class SortStrings {
    companion object {
        @JvmStatic
        fun compareChar(thisName: String, otherName: String): Int {
            val firstLength = thisName.length
            val secondLength = otherName.length

            //遍历两个字符串的字符
            for (i in 0 until min(firstLength.toDouble(), secondLength.toDouble()).toInt()) {
                val firstChar = thisName[i].lowercaseChar()
                val secondChar = otherName[i].lowercaseChar()

                val compare = firstChar.compareTo(secondChar)
                if (compare != 0) {
                    return compare
                }
            }

            return firstLength.compareTo(secondLength)
        }

        /**
         * [FCL JavaManageDialog.kt](https://github.com/FCL-Team/FoldCraftLauncher/blob/47aa35e/FCL/src/main/java/com/mio/ui/dialog/JavaManageDialog.kt#L196-L204)
         */
        @JvmStatic
        fun compareClassVersions(thisName: String, otherName: String): Int {
            val parts1 = thisName.split('.').map { it.toIntOrNull() ?: 0 }
            val parts2 = otherName.split('.').map { it.toIntOrNull() ?: 0 }
            val maxLength = maxOf(parts1.size, parts2.size)
            for (i in 0 until maxLength) {
                val p1 = parts1.getOrElse(i) { 0 }
                val p2 = parts2.getOrElse(i) { 0 }
                if (p1 != p2) return p1.compareTo(p2)
            }
            return 0
        }
    }
}
