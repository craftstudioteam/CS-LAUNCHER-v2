package com.craftstudio.launcher.ui.dialog

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import com.craftstudio.launcher.databinding.DialogProgressBinding
import com.craftstudio.launcher.setting.AllSettings
import com.craftstudio.launcher.ui.dialog.DraggableDialog.DialogInitializationListener
import com.craftstudio.launcher.utils.file.FileTools.Companion.formatFileSize

class ProgressDialog(
    context: Context,
    private val listener: OnCancelListener
) : FullScreenDialog(context), DialogInitializationListener {
    private val binding = DialogProgressBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)
        setCancelable(false)

        binding.progressBar.setMax(1000)
        binding.cancelButton.setOnClickListener {
            if (!listener.onClick()) return@setOnClickListener
            dismiss()
        }

        DraggableDialog.initDialog(this)
    }

    fun updateText(text: String?) {
        binding.textView.text = text
    }

    fun updateRate(processingRate: Long) {
        if (processingRate > 0) binding.uploadRate.visibility = View.VISIBLE
        val formatFileSize = formatFileSize(processingRate)
        "$formatFileSize/s".also { binding.uploadRate.text = it }
    }

    fun updateProgress(progress: Double, total: Double) {
        val doubleValue = progress / total * 1000
        val intValue = doubleValue.toInt()

        binding.progressBar.apply {
            visibility = if (doubleValue > 0) View.VISIBLE else View.GONE
            setProgress(intValue, AllSettings.animation.getValue())
        }
    }

    override fun onInit(): Window? = window

    fun interface OnCancelListener {
        fun onClick(): Boolean
    }
}
