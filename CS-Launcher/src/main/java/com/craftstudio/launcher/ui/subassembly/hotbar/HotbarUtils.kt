package com.craftstudio.launcher.ui.subassembly.hotbar

import com.craftstudio.launcher.setting.AllSettings.Companion.hotbarType

class HotbarUtils {
    companion object {
        @JvmStatic
        fun getCurrentType(): HotbarType {
            val hotbarType = hotbarType.getValue()
            return when (hotbarType) {
                "manually" -> HotbarType.MANUALLY
                "auto" -> HotbarType.AUTO
                else -> HotbarType.AUTO
            }
        }

        @JvmStatic
        fun getCurrentTypeIndex(): Int {
            val hotbarType = hotbarType.getValue()
            return when (hotbarType) {
                "manually" -> 1
                "auto" -> 0
                else -> 0
            }
        }
    }
}