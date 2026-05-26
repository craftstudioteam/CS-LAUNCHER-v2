package com.craftstudio.launcher.setting.unit

import androidx.annotation.CheckResult
import com.craftstudio.launcher.setting.Settings

abstract class AbstractSettingUnit<V>(
    val key: String,
    val defaultValue: V
) {
    /**
     * @return 获取当前的设置值
     */
    abstract fun getValue(): V

    /**
     * @return 存入值，并返回一个设置构建器
     */
    @CheckResult
    fun put(value: V): Settings.Manager.SettingBuilder = Settings.Manager.put(key, value!!)

    /**
     * 重置当前设置单元为默认值
     */
    fun reset() {
        Settings.Manager.put(key, defaultValue!!).save()
    }
}