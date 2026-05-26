package com.craftstudio.launcher.ui.fragment.settings.wrapper

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.craftstudio.launcher.R
import com.craftstudio.launcher.setting.unit.StringSettingUnit

@SuppressLint("UseSwitchCompatOrMaterialCode")
class ListSettingsWrapper(
    val context: Context,
    val unit: StringSettingUnit,
    val mainView: View,
    val titleView: TextView,
    val valueView: TextView,
    val entries: Array<String>,
    val entryValues: Array<String>
) : AbstractSettingsWrapper(mainView) {

    constructor(
        context: Context,
        unit: StringSettingUnit,
        mainView: View,
        titleView: TextView,
        valueView: TextView,
        itemsId: Int,
        itemValuesId: Int
    ) : this(
        context, unit, mainView, titleView, valueView,
        context.resources.getStringArray(itemsId),
        context.resources.getStringArray(itemValuesId)
    )

    init {
        updateListViewValue()
        mainView.setOnClickListener { createAListDialog() }
    }

    private fun createAListDialog() {
        val index = entryValues.indexOf(unit.getValue())
        AlertDialog.Builder(context, R.style.CustomAlertDialogTheme)
            .setTitle(titleView.text)
            .setSingleChoiceItems(entries, index) { dialog, which ->
                if (which != index) {
                    val selectedValue = entryValues[which]
                    unit.put(selectedValue).save()
                    updateListViewValue()
                    checkShowRebootDialog(context)
                }
                dialog.dismiss()
            }
            .setPositiveButton(android.R.string.cancel, null)
            .show()
    }

    private fun updateListViewValue() {
        val index = entryValues.indexOf(unit.getValue()).takeIf { it in entryValues.indices } ?: run {
            unit.reset()
            0
        }
        valueView.text = entries[index]
    }
}