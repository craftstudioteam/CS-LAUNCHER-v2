package com.craftstudio.launcher.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.craftstudio.launcher.R
import com.craftstudio.launcher.databinding.FragmentSettingsBinding
import com.craftstudio.launcher.setting.Settings
import com.craftstudio.launcher.ui.fragment.settings.AppearanceSettingsFragment
import com.craftstudio.launcher.ui.fragment.settings.ControlSettingsFragment
import com.craftstudio.launcher.ui.fragment.settings.ExperimentalSettingsFragment
import com.craftstudio.launcher.ui.fragment.settings.GameSettingsFragment
import com.craftstudio.launcher.ui.fragment.settings.LauncherSettingsFragment
import com.craftstudio.launcher.ui.fragment.settings.VideoSettingsFragment

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
        setupNavigation()
    }

    override fun onResume() {
        super.onResume()
        Settings.refreshSettings()
    }

    private fun setupNavigation() {
        binding.cardVideo.setOnClickListener { navigateTo(VideoSettingsFragment()) }
        binding.cardControls.setOnClickListener { navigateTo(ControlSettingsFragment(this)) }
        binding.cardGame.setOnClickListener { navigateTo(GameSettingsFragment()) }
        binding.cardLauncher.setOnClickListener { navigateTo(LauncherSettingsFragment(this)) }
        binding.cardExperimental.setOnClickListener { navigateTo(ExperimentalSettingsFragment()) }
        binding.cardAppearance.setOnClickListener { navigateTo(AppearanceSettingsFragment()) }
    }

    private fun navigateTo(target: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.container_fragment, target)
            .addToBackStack(null)
            .commit()
    }

    override fun slideIn(com.craftstudio.launcher.anim.AnimPlayer animPlayer) {
        // Simple fade in for the main list
        animPlayer.apply(com.craftstudio.launcher.anim.AnimPlayer.Entry(binding.root, com.craftstudio.launcher.anim.animations.Animations.FadeIn))
    }

    override fun slideOut(com.craftstudio.launcher.anim.AnimPlayer animPlayer) {
        animPlayer.apply(com.craftstudio.launcher.anim.AnimPlayer.Entry(binding.root, com.craftstudio.launcher.anim.animations.Animations.FadeOut))
    }
}
