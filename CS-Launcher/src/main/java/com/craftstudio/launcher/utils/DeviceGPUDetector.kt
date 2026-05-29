package com.craftstudio.launcher.utils

import android.os.Build

/**
 * Utility to detect the device's GPU/SoC family and provide recommendations.
 */
object DeviceGPUDetector {
    enum class GPUFamily {
        ADRENO,
        MALI,
        XCLIPSE,
        UNKNOWN
    }

    /**
     * Detects the GPU family based on hardware, board, and SoC model properties.
     */
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
            soc.contains("xclipse") || hardware.contains("xclipse") -> GPUFamily.XCLIPSE
            hardware.contains("mali") || board.contains("mali") || soc.contains("mali") || hardware.contains("mt") || hardware.contains("exynos") || hardware.contains("kirin") -> GPUFamily.MALI
            else -> GPUFamily.UNKNOWN
        }
    }

    /**
     * Returns a human-readable name for the detected GPU family.
     */
    fun getGPUName(): String {
        return when (getGPUFamily()) {
            GPUFamily.ADRENO -> "Adreno (Snapdragon)"
            GPUFamily.MALI -> "Mali (MediaTek/Exynos/Kirin)"
            GPUFamily.XCLIPSE -> "Xclipse (Samsung Exynos)"
            GPUFamily.UNKNOWN -> "Unknown GPU"
        }
    }

    /**
     * Returns true if the device uses an Adreno GPU.
     */
    fun isAdreno(): Boolean = getGPUFamily() == GPUFamily.ADRENO
}
