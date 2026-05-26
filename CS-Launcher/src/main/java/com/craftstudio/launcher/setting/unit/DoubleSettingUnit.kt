package com.craftstudio.launcher.setting.unit

import com.craftstudio.launcher.setting.Settings.Manager

class DoubleSettingUnit(key: String, defaultValue: Double) : AbstractSettingUnit<Double>(key, defaultValue) {
    override fun getValue() = Manager.getValue(key, defaultValue) { it.toDoubleOrNull() }
}