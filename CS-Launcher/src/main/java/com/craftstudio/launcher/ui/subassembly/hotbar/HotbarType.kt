package com.craftstudio.launcher.ui.subassembly.hotbar

import com.craftstudio.launcher.R

/**
 * 快捷栏判定类型
 * @param nameId 类型的本地化名称id
 * @param valueName 类型的设置存储值
 */
enum class HotbarType(val nameId: Int, val valueName: String) {
    /**
     * 自适应：根据屏幕分辨率、GUI缩放尺寸，为判定框自动计算出合适的宽与高（可能会不精准）
     */
    AUTO(R.string.option_hotbar_type_auto, "auto"),

    /**
     * 手动：让用户自行调整判定框的宽与高
     */
    MANUALLY(R.string.option_hotbar_type_manually, "manually")
}