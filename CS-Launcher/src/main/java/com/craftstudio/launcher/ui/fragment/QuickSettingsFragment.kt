package com.craftstudio.launcher.ui.fragment

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.craftstudio.launcher.anim.AnimPlayer
import com.craftstudio.launcher.anim.animations.Animations
import com.craftstudio.launcher.Architecture
import com.craftstudio.launcher.R
import com.craftstudio.launcher.Tools
import com.craftstudio.launcher.multirt.MultiRTUtils
import com.craftstudio.launcher.renderer.Renderers
import com.craftstudio.launcher.setting.AllSettings
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlin.math.min

class QuickSettingsFragment : BaseFragment(R.layout.fragment_quick_settings) {

    companion object {
        const val TAG = "QuickSettingsFragment"
    }

    private lateinit var ramValueText: TextView
    private lateinit var ramSeekbar: SeekBar
    private lateinit var ramMaxText: TextView
    private lateinit var javaChipGroup: ChipGroup
    private lateinit var rendererSpinner: Spinner
    private lateinit var resolutionValueText: TextView
    private lateinit var resolutionSeekbar: SeekBar
    private lateinit var btnApply: TextView
    private lateinit var btnBack: ImageView

    private var currentRamValue: Int = 0
    private var maxRam: Int = 0
    private var selectedRuntime: String = ""
    private var selectedRenderer: String = ""
    private var currentResolution: Int = 70

    private lateinit var rendererIdentifiers: Array<String>
    private lateinit var rendererNames: Array<String>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        loadSettings()
        setupSeekbars()
        setupJavaChips()
        setupRendererSpinner()
        setupButtons()
    }

    private fun initViews(view: View) {
        ramValueText = view.findViewById(R.id.ram_value_text)
        ramSeekbar = view.findViewById(R.id.ram_seekbar)
        ramMaxText = view.findViewById(R.id.ram_max_text)
        javaChipGroup = view.findViewById(R.id.java_chip_group)
        rendererSpinner = view.findViewById(R.id.renderer_spinner)
        resolutionValueText = view.findViewById(R.id.resolution_value_text)
        resolutionSeekbar = view.findViewById(R.id.resolution_seekbar)
        btnApply = view.findViewById(R.id.btn_apply)
        btnBack = view.findViewById(R.id.btn_back)
    }

    private fun loadSettings() {
        val context = requireContext()

        // RAM
        val deviceRam = Tools.getTotalDeviceMemory(context)
        maxRam = if (Architecture.is32BitsDevice() || deviceRam < 2048) {
            min(1024.0, deviceRam.toDouble()).toInt()
        } else {
            deviceRam - (if (deviceRam < 3064) 800 else 1024)
        }

        currentRamValue = AllSettings.ramAllocation.value.getValue()
        if (currentRamValue > maxRam) currentRamValue = maxRam
        if (currentRamValue < 256) currentRamValue = 256

        // Java Runtime
        selectedRuntime = AllSettings.defaultRuntime.getValue()
        if (selectedRuntime.isBlank()) selectedRuntime = "auto"

        // Renderer
        val renderers = Renderers.getCompatibleRenderers(context).first
        rendererIdentifiers = renderers.rendererIdentifier.toTypedArray()
        rendererNames = renderers.rendererNames.toTypedArray()

        selectedRenderer = AllSettings.renderer.getValue()

        // Resolution
        currentResolution = AllSettings.resolutionRatio.getValue()
        if (currentResolution < 50) currentResolution = 50
        if (currentResolution > 100) currentResolution = 100
    }

    private fun setupSeekbars() {
        // RAM Seekbar
        ramSeekbar.max = maxRam - 256
        ramSeekbar.progress = currentRamValue - 256
        updateRamText()
        ramMaxText.text = "${maxRam} MB"

        ramSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentRamValue = progress + 256
                updateRamText()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Resolution Seekbar
        resolutionSeekbar.progress = currentResolution
        updateResolutionText()

        resolutionSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentResolution = progress.coerceIn(50, 100)
                updateResolutionText()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateRamText() {
        ramValueText.text = "Allocated RAM: ${currentRamValue} MB"
    }

    private fun updateResolutionText() {
        val metrics = Tools.currentDisplayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val progressFloat = currentResolution.toFloat() / 100F
        val scaledWidth = (width * progressFloat).toInt()
        val scaledHeight = (height * progressFloat).toInt()
        resolutionValueText.text = "Resolution: ${currentResolution}% (${scaledWidth} x ${scaledHeight})"
    }

    private fun setupJavaChips() {
        javaChipGroup.removeAllViews()

        val runtimes = MultiRTUtils.getRuntimes()

        // Auto chip
        addChip("Auto Select", "auto")

        // Installed runtimes
        for (runtime in runtimes) {
            addChip(runtime.name, runtime.name)
        }

        // Check the selected chip
        for (i in 0 until javaChipGroup.childCount) {
            val chip = javaChipGroup.getChildAt(i) as? Chip
            if (chip?.tag == selectedRuntime) {
                chip.isChecked = true
                break
            }
        }
    }

    private fun addChip(label: String, value: String) {
        val context = requireContext()
        val chip = Chip(context).apply {
            text = label
            tag = value
            isCheckable = true
            isCheckedIconVisible = true
            setTextColor(context.getColor(R.color.quick_settings_text))
            chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                context.getColor(R.color.quick_settings_bg)
            )
            chipStrokeColor = android.content.res.ColorStateList.valueOf(
                context.getColor(R.color.quick_settings_green)
            )
            chipStrokeWidth = 1f * context.resources.displayMetrics.density
            chipCornerRadius = 20f * context.resources.displayMetrics.density
            checkedIconTint = android.content.res.ColorStateList.valueOf(
                context.getColor(R.color.quick_settings_green)
            )
            rippleColor = android.content.res.ColorStateList.valueOf(
                context.getColor(R.color.quick_settings_green_ripple)
            )

            setOnClickListener {
                selectedRuntime = value
                updateChipStates()
            }
        }
        javaChipGroup.addView(chip)
    }

    private fun updateChipStates() {
        for (i in 0 until javaChipGroup.childCount) {
            val chip = javaChipGroup.getChildAt(i) as? Chip ?: continue
            val isSelected = chip.tag == selectedRuntime
            chip.isChecked = isSelected
            chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                if (isSelected) requireContext().getColor(R.color.quick_settings_green)
                else requireContext().getColor(R.color.quick_settings_bg)
            )
            chip.setTextColor(
                if (isSelected) requireContext().getColor(R.color.quick_settings_bg)
                else requireContext().getColor(R.color.quick_settings_text)
            )
        }
    }

    private fun setupRendererSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, rendererNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        rendererSpinner.adapter = adapter

        // Select current renderer
        val currentIndex = rendererIdentifiers.indexOf(selectedRenderer)
        if (currentIndex >= 0) {
            rendererSpinner.setSelection(currentIndex)
        }

        rendererSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedRenderer = rendererIdentifiers[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupButtons() {
        btnApply.setOnClickListener {
            saveSettings()
        }

        btnBack.setOnClickListener {
            forceBack()
        }
    }

    private fun saveSettings() {
        // Save RAM
        AllSettings.ramAllocation.value.put(currentRamValue).save()

        // Save Java Runtime
        AllSettings.defaultRuntime.put(selectedRuntime).save()

        // Save Renderer
        AllSettings.renderer.put(selectedRenderer).save()

        // Save Resolution
        AllSettings.resolutionRatio.put(currentResolution).save()

        Toast.makeText(requireContext(), "Quick settings applied", Toast.LENGTH_SHORT).show()
        forceBack()
    }

    fun slideIn(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(requireView(), Animations.BounceInRight))
    }

    fun slideOut(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(requireView(), Animations.FadeOutLeft))
    }
}
