package com.craftstudio.launcher.ui.fragment.settings

import androidx.annotation.CallSuper
import com.craftstudio.launcher.anim.AnimPlayer
import com.craftstudio.launcher.event.single.SettingsChangeEvent
import com.craftstudio.launcher.event.value.SettingsPageSwapEvent
import com.craftstudio.launcher.ui.fragment.FragmentWithAnim
import com.craftstudio.launcher.prefs.LauncherPreferences
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

abstract class AbstractSettingsFragment(layoutId: Int, private val category: SettingCategory) : FragmentWithAnim(layoutId) {
    @Subscribe
    fun event(event: SettingsChangeEvent) {
        onChange()
    }

    @Subscribe
    fun event(event: SettingsPageSwapEvent) {
        if (event.index == category.ordinal) {
            slideIn()
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    @CallSuper
    protected open fun onChange() {
        LauncherPreferences.loadPreferences(requireContext())
    }

    override fun slideOut(animPlayer: AnimPlayer) {}
}