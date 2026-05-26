package com.craftstudio.launcher.ui.fragment

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.angcyo.tablayout.DslTabLayout
import com.craftstudio.launcher.anim.AnimPlayer
import com.craftstudio.launcher.anim.animations.Animations
import com.craftstudio.launcher.R
import com.craftstudio.launcher.databinding.FragmentSettingsBinding
import com.craftstudio.launcher.event.value.SettingsPageSwapEvent
import com.craftstudio.launcher.setting.Settings
import com.craftstudio.launcher.ui.fragment.settings.AppearanceSettingsFragment
import com.craftstudio.launcher.ui.fragment.settings.ControlSettingsFragment
import com.craftstudio.launcher.ui.fragment.settings.ExperimentalSettingsFragment
import com.craftstudio.launcher.ui.fragment.settings.GameSettingsFragment
import com.craftstudio.launcher.ui.fragment.settings.LauncherSettingsFragment
import com.craftstudio.launcher.ui.fragment.settings.VideoSettingsFragment
import org.greenrobot.eventbus.EventBus

class SettingsFragment : FragmentWithAnim(R.layout.fragment_settings) {
    companion object {
        const val TAG: String = "SettingsFragment"
    }

    private lateinit var binding: FragmentSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initViewPager()

        binding.settingsTab.observeIndexChange { _, toIndex, reselect, fromUser ->
            if (reselect) return@observeIndexChange
            if (fromUser) binding.settingsViewpager.setCurrentItem(toIndex, false)
        }
    }

    override fun onResume() {
        super.onResume()
        Settings.refreshSettings()
    }

    private fun initViewPager() {
        binding.settingsViewpager.apply {
            adapter = ViewPagerAdapter(this@SettingsFragment)
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
            offscreenPageLimit = 6
            isUserInputEnabled = false
            registerOnPageChangeCallback(object: OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    binding.settingsTab.setCurrentItem(position)
                    EventBus.getDefault().post(SettingsPageSwapEvent(position))
                }
            })
        }

        // Force tab population for vertical sidebar
        val tabTitles = listOf("Video", "Controls", "Game", "Launcher", "Experimental", "Appearance")
        binding.settingsTab.removeAllViews()
        tabTitles.forEach { title ->
            val textView = TextView(requireContext()).apply {
                text = title
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER_VERTICAL
                setPadding(40, 50, 40, 50)
                setTextColor(Color.WHITE)
                layoutParams = DslTabLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            binding.settingsTab.addView(textView)
        }
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.settingsLayout, Animations.BounceInRight))
            .apply(AnimPlayer.Entry(binding.settingsViewpager, Animations.BounceInDown))
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.settingsLayout, Animations.FadeOutLeft))
            .apply(AnimPlayer.Entry(binding.settingsViewpager, Animations.FadeOutUp))
    }

    private class ViewPagerAdapter(val fragment: FragmentWithAnim): FragmentStateAdapter(fragment.requireActivity()) {
        override fun getItemCount(): Int = 6
        override fun createFragment(position: Int): Fragment {
            return when(position) {
                1 -> ControlSettingsFragment(fragment)
                2 -> GameSettingsFragment()
                3 -> LauncherSettingsFragment(fragment)
                4 -> ExperimentalSettingsFragment()
                5 -> AppearanceSettingsFragment()
                else -> VideoSettingsFragment()
            }
        }
    }
}