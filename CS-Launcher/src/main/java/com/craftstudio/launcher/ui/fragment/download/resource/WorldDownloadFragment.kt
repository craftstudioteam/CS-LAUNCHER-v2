package com.craftstudio.launcher.ui.fragment.download.resource

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.craftstudio.launcher.R
import com.craftstudio.launcher.context.ContextExecutor
import com.craftstudio.launcher.feature.download.enums.Classify
import com.craftstudio.launcher.feature.download.install.UnpackWorldZipHelper
import com.craftstudio.launcher.feature.download.platform.AbstractPlatformHelper.Companion.getWorldPath
import com.craftstudio.launcher.feature.download.utils.CategoryUtils
import com.craftstudio.launcher.task.Task
import com.craftstudio.launcher.task.TaskExecutors
import com.craftstudio.launcher.utils.ZHTools
import com.craftstudio.launcher.utils.file.FileTools
import com.craftstudio.launcher.Tools
import com.craftstudio.launcher.contracts.OpenDocumentWithExtension

class WorldDownloadFragment(parentFragment: Fragment? = null) : AbstractResourceDownloadFragment(
    parentFragment,
    Classify.WORLD,
    CategoryUtils.getWorldCategory(),
    false
) {
    private var openDocumentLauncher: ActivityResultLauncher<Any>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openDocumentLauncher = registerForActivityResult(OpenDocumentWithExtension("zip")) { uris: List<Uri>? ->
            uris?.let { uriList ->
                uriList[0].let { result ->
                    val dialog = ZHTools.showTaskRunningDialog(requireContext())
                    Task.runTask {
                        val worldFile = FileTools.copyFileInBackground(requireContext(), result, getWorldPath().absolutePath)
                        runCatching {
                            UnpackWorldZipHelper.unpackFile(worldFile, getWorldPath())
                        }.getOrElse {
                            ContextExecutor.showToast(R.string.download_install_unpack_world_error, Toast.LENGTH_SHORT)
                        }
                    }.onThrowable { e ->
                        Tools.showErrorRemote(e)
                    }.finallyTask(TaskExecutors.getAndroidUI()) {
                        dialog.dismiss()
                    }.execute()
                }
            }
        }
    }

    override fun initInstallButton(installButton: Button) {
        installButton.setOnClickListener {
            val suffix = ".zip"
            Toast.makeText(
                requireActivity(),
                String.format(getString(R.string.file_add_file_tip), suffix),
                Toast.LENGTH_SHORT
            ).show()
            openDocumentLauncher?.launch(suffix)
        }
    }
}