package com.craftstudio.launcher.ui.fragment.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.craftstudio.launcher.anim.AnimPlayer
import com.craftstudio.launcher.anim.animations.Animations
import com.craftstudio.launcher.R
import com.craftstudio.launcher.databinding.SettingsFragmentExperimentalBinding
import com.craftstudio.launcher.setting.AllSettings
import com.craftstudio.launcher.ui.fragment.settings.wrapper.SeekBarSettingsWrapper
import com.craftstudio.launcher.ui.fragment.settings.wrapper.SwitchSettingsWrapper

class ExperimentalSettingsFragment :
    AbstractSettingsFragment(R.layout.settings_fragment_experimental, SettingCategory.EXPERIMENTAL) {
    private lateinit var binding: SettingsFragmentExperimentalBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SettingsFragmentExperimentalBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = requireContext()

        SwitchSettingsWrapper(
            context,
            AllSettings.dumpShaders,
            binding.dumpShadersLayout,
            binding.dumpShaders
        )

        SwitchSettingsWrapper(
            context,
            AllSettings.bigCoreAffinity,
            binding.bigCoreAffinityLayout,
            binding.bigCoreAffinity
        )

        SeekBarSettingsWrapper(
            context,
            AllSettings.tcVibrateDuration,
            binding.tcVibrateDurationLayout,
            binding.tcVibrateDurationTitle,
            binding.tcVibrateDurationSummary,
            binding.tcVibrateDurationValue,
            binding.tcVibrateDuration,
            "ms"
        )
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.root, Animations.BounceInDown))
    }
}