package com.craftstudio.launcher.ui.dialog

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Window
import android.widget.Toast
import androidx.core.text.HtmlCompat
import com.craftstudio.launcher.R
import com.craftstudio.launcher.databinding.DialogForceUpdateBinding
import com.craftstudio.launcher.feature.firebase.LauncherRemoteContent
import com.craftstudio.launcher.ui.dialog.DraggableDialog.DialogInitializationListener

class ForceUpdateDialog(
    context: Context,
    private val remoteContent: LauncherRemoteContent,
) : FullScreenDialog(context), DialogInitializationListener {
    private val binding = DialogForceUpdateBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        setContentView(binding.root)
        init()
        DraggableDialog.initDialog(this)
    }

    private fun init() {
        binding.forceUpdateTitle.text = context.getString(R.string.force_update_title)

        val message = if (remoteContent.forceUpdateMessageHtml.isBlank()) {
            context.getString(R.string.force_update_message)
        } else {
            remoteContent.forceUpdateMessageHtml
        }

        binding.forceUpdateMessage.text = HtmlCompat.fromHtml(message, HtmlCompat.FROM_HTML_MODE_COMPACT)

        binding.forceUpdateAction.setOnClickListener {
            val downloadUrl = remoteContent.downloadUrl.trim()
            if (downloadUrl.isBlank()) {
                Toast.makeText(context, context.getString(R.string.force_update_unavailable), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }.onFailure {
                Toast.makeText(context, context.getString(R.string.force_update_unavailable), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onInit(): Window? = window
}