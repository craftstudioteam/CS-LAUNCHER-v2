package com.craftstudio.launcher.ui.fragment.settings.wrapper

import android.annotation.SuppressLint
import android.content.Context
import android.text.InputType
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.craftstudio.launcher.R
import com.craftstudio.launcher.feature.log.Logging.e
import com.craftstudio.launcher.setting.unit.IntSettingUnit
import com.craftstudio.launcher.ui.dialog.EditTextDialog

@SuppressLint("UseSwitchCompatOrMaterialCode", "StringFormatInvalid")
class SeekBarSettingsWrapper(
    val context: Context,
    val unit: IntSettingUnit,
    val mainView: View,
    val titleView: TextView,
    val summaryView: TextView,
    val valueView: TextView,
    val seekbarView: SeekBar,
    val suffix: String,
    onStartListener: OnStartInit?
) : AbstractSettingsWrapper(mainView) {
    private var listener: OnSeekBarProgressChangeListener? = null

    constructor(
        context: Context,
        unit: IntSettingUnit,
        mainView: View,
        titleView: TextView,
        summaryView: TextView,
        valueView: TextView,
        seekbarView: SeekBar,
        suffix: String,
    ) : this(
        context,
        unit,
        mainView,
        titleView,
        summaryView,
        valueView,
        seekbarView,
        suffix,
        null
    )

    init {
        onStartListener?.onStart(this)

        seekbarView.progress = unit.getValue()
        valueView.background = ContextCompat.getDrawable(context, R.drawable.background_text)
        setSeekBarValueTextView()

        seekbarView.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            fun updateSeekbarValue(saveValue: Boolean) {
                val progress = seekbarView.progress

                listener?.onChange(progress)
                if (saveValue) unit.put(progress).save()
                setSeekBarValueTextView()
                checkShowRebootDialog(context)
            }

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateSeekbarValue(!fromUser)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                updateSeekbarValue(true)
            }
        })

        mainView.setOnClickListener {
            EditTextDialog.Builder(context)
                .setEditText(seekbarView.progress.toString())
                .setInputType(InputType.TYPE_CLASS_NUMBER)
                .setTitle(titleView.text.toString())
                .setMessage(summaryView.text.toString())
                .setAsRequired()
                .setConfirmListener { editBox, _ ->
                    val string = editBox.text.toString()

                    val value: Int
                    try {
                        value = string.toInt()
                    } catch (e: NumberFormatException) {
                        e("SeekBarSettingsWrapper", "The data is illegal", e)

                        editBox.error = context.getString(R.string.generic_input_invalid)
                        return@setConfirmListener false
                    }

                    if (value < seekbarView.min) {
                        val minValue =
                            String.format("%s %s", seekbarView.min, suffix)
                        editBox.error =
                            context.getString(R.string.generic_input_too_small, minValue)
                        return@setConfirmListener false
                    }
                    if (value > seekbarView.max) {
                        val maxValue =
                            String.format("%s %s", seekbarView.max, suffix)
                        editBox.error = context.getString(R.string.generic_input_too_big, maxValue)
                        return@setConfirmListener false
                    }

                    seekbarView.progress = value
                    true
                }.showDialog()
        }
    }

    fun setSeekBarValueTextView() {
        val text = "${seekbarView.progress} $suffix".trim()
        valueView.text = text
    }

    fun setOnSeekBarProgressChangeListener(listener: OnSeekBarProgressChangeListener) {
        this.listener = listener
    }

    //部分场景需要在seekbar被包装前，完成一些操作，比如动态调整最大值或最小值
    fun interface OnStartInit {
        fun onStart(wrapper: SeekBarSettingsWrapper)
    }

    fun interface OnSeekBarProgressChangeListener {
        fun onChange(progress: Int)
    }
}