package com.craftstudio.launcher.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.craftstudio.launcher.Architecture
import com.craftstudio.launcher.R
import com.craftstudio.launcher.Tools
import com.craftstudio.launcher.multirt.MultiRTUtils
import com.craftstudio.launcher.setting.AllSettings
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlin.math.min

class QuickSettingsDialog(context: Context) : Dialog(context, R.style.CustomDialogStyle) {

    private lateinit var ramValueText: TextView
    private lateinit var ramSeekbar: SeekBar
    private lateinit var ramMaxText: TextView
    private lateinit var javaChipGroup: ChipGroup
    private lateinit var btnApply: TextView
    private lateinit var btnClose: ImageView

    private var currentRamValue: Int = 0
    private var maxRam: Int = 0
    private var selectedRuntime: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_quick_settings)

        window?.apply {
            setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.CENTER)
            attributes?.windowAnimations = R.style.QuickSettingsAnimation
        }

        initViews()
        loadSettings()
        setupSeekbar()
        setupJavaChips()
        setupButtons()
    }

    private fun initViews() {
        ramValueText = findViewById(R.id.ram_value_text)
        ramSeekbar = findViewById(R.id.ram_seekbar)
        ramMaxText = findViewById(R.id.ram_max_text)
        javaChipGroup = findViewById(R.id.java_chip_group)
        btnApply = findViewById(R.id.btn_apply)
        btnClose = findViewById(R.id.btn_close)
    }

    private fun loadSettings() {
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
    }

    private fun setupSeekbar() {
        ramSeekbar.max = maxRam - 256 // offset so min is 256
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
    }

    private fun updateRamText() {
        ramValueText.text = "Allocated RAM: ${currentRamValue} MB"
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

            // Ripple
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
                if (isSelected) context.getColor(R.color.quick_settings_green)
                else context.getColor(R.color.quick_settings_bg)
            )
            chip.setTextColor(
                if (isSelected) context.getColor(R.color.quick_settings_bg)
                else context.getColor(R.color.quick_settings_text)
            )
        }
    }

    private fun setupButtons() {
        btnApply.setOnClickListener {
            saveSettings()
            dismiss()
        }

        btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun saveSettings() {
        // Save RAM
        AllSettings.ramAllocation.value.put(currentRamValue).save()

        // Save Java Runtime
        AllSettings.defaultRuntime.put(selectedRuntime).save()

        Toast.makeText(context, "Quick settings applied", Toast.LENGTH_SHORT).show()
    }
}
