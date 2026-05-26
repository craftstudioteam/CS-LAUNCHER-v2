package com.craftstudio.launcher.utils.runtime

import android.content.Context
import com.craftstudio.launcher.setting.AllSettings
import com.craftstudio.launcher.task.TaskExecutors
import com.craftstudio.launcher.ui.dialog.SelectRuntimeDialog

class SelectRuntimeUtils {
    companion object {
        @JvmStatic
        fun selectRuntime(context: Context, dialogTitle: String?, selectedListener: RuntimeSelectedListener) {
            TaskExecutors.runInUIThread {
                when (AllSettings.selectRuntimeMode.getValue()) {
                    "ask_me" -> SelectRuntimeDialog(context, selectedListener).apply {
                        dialogTitle?.let { setTitleText(it) }
                    }.show()
                    "default" -> selectedListener.onSelected(AllSettings.defaultRuntime.getValue().takeIf { it.isNotEmpty() })
                    "auto" -> selectedListener.onSelected(null)
                    else -> {}
                }
            }
        }
    }
}