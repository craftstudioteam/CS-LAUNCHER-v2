package com.craftstudio.launcher.ui.dialog

import android.content.Context
import android.os.Bundle
import android.view.Window
import android.widget.TextView
import com.craftstudio.launcher.R
import com.craftstudio.launcher.databinding.DialogControlInfoBinding
import com.craftstudio.launcher.task.Task
import com.craftstudio.launcher.ui.dialog.DraggableDialog.DialogInitializationListener
import com.craftstudio.launcher.ui.subassembly.customcontrols.ControlInfoData
import com.craftstudio.launcher.ui.subassembly.customcontrols.EditControlData.Companion.loadCustomControlsFromFile
import com.craftstudio.launcher.ui.subassembly.customcontrols.EditControlData.Companion.saveToFile
import com.craftstudio.launcher.utils.path.PathManager
import java.io.File

class ControlInfoDialog(
    context: Context,
    private val controlInfoData: ControlInfoData,
    private val task: Task<*>
) :
    FullScreenDialog(context), DialogInitializationListener {
    private val binding = DialogControlInfoBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(false)
        setContentView(binding.root)

        init(context, task)
        DraggableDialog.initDialog(this)
    }

    private fun TextView.setTextOrDefault(value: String?) {
        this.text = value?.takeIf { it.isNotEmpty() && it != "null" } ?: context.getString(R.string.generic_unknown)
    }

    private fun init(context: Context, task: Task<*>) {
        binding.apply {
            closeButton.setOnClickListener { dismiss() }
            editButton.setOnClickListener {
                val editControlInfoDialog = EditControlInfoDialog(
                    context, false, controlInfoData.fileName,
                    controlInfoData
                )
                editControlInfoDialog.setTitle(context.getString(R.string.generic_edit))
                editControlInfoDialog.setOnConfirmClickListener { fileName: String, controlInfoData: ControlInfoData ->
                    val controlFile = File(PathManager.DIR_CTRLMAP_PATH, fileName)
                    loadCustomControlsFromFile(context, controlFile)?.let { customControls ->
                        customControls.mControlInfoDataList.name = controlInfoData.name
                        customControls.mControlInfoDataList.author = controlInfoData.author
                        customControls.mControlInfoDataList.version = controlInfoData.version
                        customControls.mControlInfoDataList.desc = controlInfoData.desc

                        saveToFile(context, customControls, controlFile)
                    }

                    task.execute()
                    editControlInfoDialog.dismiss()
                }
                editControlInfoDialog.show()
                dismiss()
            }

            nameText.setTextOrDefault(controlInfoData.name)
            fileNameText.setTextOrDefault(controlInfoData.fileName)
            authorText.setTextOrDefault(controlInfoData.author)
            versionText.setTextOrDefault(controlInfoData.version)
            descText.setTextOrDefault(controlInfoData.desc)

            checkHeight(root, contentView, scrollView)
        }
    }

    override fun onInit(): Window? = window
}