package com.craftstudio.launcher.ui.fragment.settings

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class AppearanceSettingsFragment : Fragment() {
    companion object {
        private const val DEFAULT_ACCENT = "#24B538"
        private const val PREFS_NAME = "appearance_prefs"
        private const val KEY_ACCENT_COLOR = "app_accent_color"

        @JvmStatic
        fun getAccentColor(context: Context): String {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_ACCENT_COLOR, DEFAULT_ACCENT) ?: DEFAULT_ACCENT
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return View(requireContext()).apply {
            setBackgroundColor(Color.parseColor("#121212"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }
}
