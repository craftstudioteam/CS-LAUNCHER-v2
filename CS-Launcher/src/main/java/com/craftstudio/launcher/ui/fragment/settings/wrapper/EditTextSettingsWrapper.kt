package com.craftstudio.launcher.ui.fragment.settings.wrapper

import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import com.craftstudio.launcher.setting.unit.StringSettingUnit

class EditTextSettingsWrapper(
    private val unit: StringSettingUnit,
    val mainView: View,
    private val editText: EditText
) : AbstractSettingsWrapper(mainView) {
    private var listener: OnTextChangedListener? = null

    init {
        editText.apply {
            setText(unit.getValue())
            inputType = InputType.TYPE_CLASS_TEXT
            gravity = Gravity.TOP or Gravity.START
            setOnEditorActionListener { _, _, _ ->
                clearFocus()
                false
            }

            doAfterTextChanged { text ->
                val string = text?.toString() ?: ""
                unit.put(string).save()
                listener?.onChanged(string)
            }
        }
    }

    fun setOnTextChangedListener(listener: OnTextChangedListener): EditTextSettingsWrapper {
        this.listener = listener
        return this
    }

    fun setMaxLength(maxLength: Int): EditTextSettingsWrapper {
        val filters = arrayOf<InputFilter>(LengthFilter(maxLength))
        editText.filters = filters
        return this
    }

    fun interface OnTextChangedListener {
        fun onChanged(text: String)
    }
}