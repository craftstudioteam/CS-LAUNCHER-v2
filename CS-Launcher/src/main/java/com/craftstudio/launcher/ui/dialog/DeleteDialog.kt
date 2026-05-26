package com.craftstudio.launcher.ui.dialog

import android.annotation.SuppressLint
import android.content.Context
import com.craftstudio.launcher.R
import com.craftstudio.launcher.task.Task
import com.craftstudio.launcher.utils.file.FileDeletionHandler
import java.io.File

@SuppressLint("CheckResult")
class DeleteDialog(private val context: Context, endTask: Task<*>, files: List<File>) {
    private val mDialog: TipDialog = TipDialog.Builder(context).apply {
        setCancelable(false)

        val singleFile = files.size == 1
        val file = files[0]
        val isFolder = file.isDirectory

        setTitle(
            if (singleFile) (
                    if (isFolder) R.string.file_delete_dir
                    else R.string.file_tips
            ) else R.string.file_delete_multiple_items_title
        )

        setMessage(
            if (singleFile) (
                    if (isFolder) R.string.file_delete_dir_message
                    else R.string.file_delete
            ) else R.string.file_delete_multiple_items_message
        )

        setConfirm(R.string.generic_delete)
        setWarning()

        setConfirmClickListener {
            FileDeletionHandler(context, files, endTask).start()
        }
    }.buildDialog()

    fun show() {
        mDialog.show()
    }
}
