package com.craftstudio.launcher.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.craftstudio.launcher.databinding.ModInfoDialogBinding
import com.craftstudio.launcher.feature.mod.parser.ModInfo

class ModInfoDialog : DialogFragment() {
    companion object {
        const val TAG = "ModInfoDialog"

        fun newInstance(modInfo: ModInfo): ModInfoDialog {
            return ModInfoDialog().apply {
                arguments = Bundle().apply {
                    putParcelable("mod_info", modInfo)
                }
            }
        }
    }

    private lateinit var binding: ModInfoDialogBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = ModInfoDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val modInfo = arguments?.getParcelable<ModInfo>("mod_info")
        if (modInfo == null) {
            dismiss()
            return
        }

        setupModInfo(modInfo)
        setupClickListeners()
    }

    private fun setupModInfo(modInfo: ModInfo) {
        binding.apply {
            modName.text = modInfo.name ?: modInfo.file?.name ?: "Unknown"
            modVersion.text = modInfo.version?.let { "v$it" } ?: "Unknown"
            modAuthor.text = modInfo.authors?.joinToString(", ") ?: "Unknown"
            modDescription.text = modInfo.description ?: "No description available"

            modInfo.file?.let { file ->
                val size = java.text.DecimalFormat("#.##").format(file.length().toDouble() / (1024 * 1024))
                modSize.text = "%.2f MB".format(size)
                modFile.text = file.name
            }

            val isEnabled = modInfo.file?.name?.endsWith(".jar") == true
            modStatus.text = if (isEnabled) "Enabled" else "Disabled"
            modStatus.setTextColor(
                if (isEnabled)
                    requireContext().getColor(com.craftstudio.launcher.R.color.green)
                else
                    requireContext().getColor(com.craftstudio.launcher.R.color.red)
            )
        }
    }

    private fun setupClickListeners() {
        binding.closeButton.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
