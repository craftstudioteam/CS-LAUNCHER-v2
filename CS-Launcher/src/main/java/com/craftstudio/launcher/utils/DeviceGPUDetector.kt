package com.craftstudio.launcher.utils

import android.os.Build

object DeviceGPUDetector {
    enum class GPUFamily {
        ADRENO,
        MALI,
        XCLIPSE,
        UNKNOWN
    }

    fun getGPUFamily(): GPUFamily {
        val hardware = Build.HARDWARE.lowercase()
        val board = Build.BOARD.lowercase()
        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.lowercase()
        } else {
            ""
        }

        return when {
            hardware.contains("qcom") || board.contains("msm") || soc.contains("adreno") || soc.contains("snapdragon") -> GPUFamily.ADRENO
            hardware.contains("mali") || board.contains("mali") || soc.contains("mali") || hardware.contains("mt") || hardware.contains("exynos") -> {
                 if (soc.contains("xclipse") || hardware.contains("xclipse")) GPUFamily.XCLIPSE else GPUFamily.MALI
            }
            else -> GPUFamily.UNKNOWN
        }
    }

    fun getGPUName(): String {
        return when (getGPUFamily()) {
            GPUFamily.ADRENO -> "Adreno (Snapdragon)"
            GPUFamily.MALI -> "Mali (MediaTek/Exynos)"
            GPUFamily.XCLIPSE -> "Xclipse (Samsung Exynos)"
            GPUFamily.UNKNOWN -> "Unknown GPU"
        }
    }
}
