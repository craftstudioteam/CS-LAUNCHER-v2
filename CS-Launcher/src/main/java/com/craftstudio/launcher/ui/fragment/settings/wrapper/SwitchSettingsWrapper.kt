package com.craftstudio.launcher.ui.fragment.settings.wrapper

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.CompoundButton
import android.widget.Switch
import com.craftstudio.launcher.setting.unit.BooleanSettingUnit

@SuppressLint("UseSwitchCompatOrMaterialCode")
class SwitchSettingsWrapper(
    private val context: Context,
    private val unit: BooleanSettingUnit,
    val mainView: View,
    val switchView: Switch
) : AbstractSettingsWrapper(mainView) {
    private var listener: OnCheckedChangeListener? = null

    init {
        switchView.isChecked = unit.getValue()

        switchView.setOnCheckedChangeListener { buttonView, isChecked ->
            val switchChangeListener = OnSwitchSaveListener {
                unit.put(isChecked).save()
                checkShowRebootDialog(context)
            }
            listener?.onChange(buttonView, isChecked, switchChangeListener) ?: switchChangeListener.onSave()
        }
        mainView.setOnClickListener {
            switchView.isChecked = !switchView.isChecked
        }
    }

    fun setOnCheckedChangeListener(listener: OnCheckedChangeListener): SwitchSettingsWrapper {
        this.listener = listener
        return this
    }

    fun interface OnSwitchSaveListener {
        fun onSave()
    }

    fun interface OnCheckedChangeListener {
        fun onChange(buttonView: CompoundButton, isChecked: Boolean, listener: OnSwitchSaveListener)
    }
}