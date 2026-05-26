package com.craftstudio.launcher.ui.fragment.settings

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.craftstudio.launcher.R
import com.google.android.material.card.MaterialCardView
import android.widget.Button

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

    private var selectedColor: String = DEFAULT_ACCENT
    private lateinit var colorViews: Map<String, MaterialCardView>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings_appearance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        selectedColor = getAccentColor(requireContext())
        
        colorViews = mapOf(
            "#24B538" to view.findViewById(R.id.color_green),
            "#FFA500" to view.findViewById(R.id.color_orange),
            "#DC143C" to view.findViewById(R.id.color_red),
            "#00BFFF" to view.findViewById(R.id.color_blue),
            "#8A2BE2" to view.findViewById(R.id.color_purple)
        )

        updateSelection()

        colorViews.forEach { (color, card) ->
            card.setOnClickListener {
                selectedColor = color
                updateSelection()
            }
        }

        view.findViewById<View>(R.id.btn_apply_theme).setOnClickListener {
            saveAndApply()
        }
    }

    private fun updateSelection() {
        colorViews.forEach { (color, card) ->
            if (color == selectedColor) {
                card.strokeWidth = (3 * resources.displayMetrics.density).toInt()
            } else {
                card.strokeWidth = 0
            }
        }
    }

    private fun saveAndApply() {
        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACCENT_COLOR, selectedColor)
            .apply()

        // Absolutely recreate activity to apply theme globally
        requireActivity().recreate()
    }
}
