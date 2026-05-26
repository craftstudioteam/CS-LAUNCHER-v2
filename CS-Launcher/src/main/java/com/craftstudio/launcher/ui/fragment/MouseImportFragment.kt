package com.craftstudio.launcher.ui.fragment

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.craftstudio.launcher.R
import com.craftstudio.launcher.databinding.FragmentMouseImportBinding
import com.craftstudio.launcher.setting.AllSettings
import com.craftstudio.launcher.task.Task
import com.craftstudio.launcher.task.TaskExecutors
import com.craftstudio.launcher.ui.dialog.FilesDialog
import com.craftstudio.launcher.ui.dialog.FilesDialog.FilesButton
import com.craftstudio.launcher.ui.subassembly.filelist.FileIcon
import com.craftstudio.launcher.ui.subassembly.filelist.FileItemBean
import com.craftstudio.launcher.ui.subassembly.filelist.FileRecyclerViewCreator
import com.craftstudio.launcher.utils.path.PathManager
import com.craftstudio.launcher.utils.ZHTools
import com.craftstudio.launcher.utils.file.FileTools
import com.craftstudio.launcher.utils.file.FileTools.Companion.mkdirs
import com.craftstudio.launcher.utils.image.ImageUtils.Companion.isImage
import com.craftstudio.launcher.utils.stringutils.StringUtils
import com.craftstudio.launcher.Tools
import java.io.File

class MouseImportFragment : Fragment() {

    private var _binding: FragmentMouseImportBinding? = null
    private val binding get() = _binding!!

    private lateinit var openDocumentLauncher: ActivityResultLauncher<Array<String>>
    private var fileRecyclerViewCreator: FileRecyclerViewCreator? = null
    private var onDataChangedListener: (() -> Unit)? = null

    fun setOnDataChangedListener(listener: () -> Unit) {
        onDataChangedListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openDocumentLauncher = registerForActivityResult<Array<String>, Uri>(ActivityResultContracts.OpenDocument()) { result: Uri? ->
            result?.let { uri ->
                val dialog = ZHTools.showTaskRunningDialog(requireContext())
                Task.runTask {
                    FileTools.copyFileInBackground(requireActivity(), uri, mousePath().absolutePath)
                }.ended(TaskExecutors.getAndroidUI()) {
                    Toast.makeText(requireActivity(), getString(R.string.file_added), Toast.LENGTH_SHORT).show()
                    loadData()
                }.onThrowable { e ->
                    Tools.showErrorRemote(e)
                }.finallyTask(TaskExecutors.getAndroidUI()) {
                    dialog.dismiss()
                }.execute()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMouseImportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initViews()
        loadData()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun loadData() {
        val fileItemBeans = FileRecyclerViewCreator.loadItemBeansFromPath(
            requireActivity(),
            mousePath(),
            FileIcon.FILE,
            showFile = true,
            showFolder = false
        )
        fileItemBeans.add(0, FileItemBean(
            getString(R.string.custom_mouse_default),
            ContextCompat.getDrawable(requireActivity(), R.drawable.ic_mouse_pointer)
        ))
        TaskExecutors.runInUIThread {
            fileRecyclerViewCreator?.loadData(fileItemBeans)
            onDataChangedListener?.invoke()
        }
    }

    private fun mousePath(): File {
        val path = File(PathManager.DIR_CUSTOM_MOUSE)
        if (!path.exists()) mkdirs(path)
        return path
    }

    private fun initViews() {
        fileRecyclerViewCreator = FileRecyclerViewCreator(requireActivity(), binding.recyclerView, { position: Int, fileItemBean: FileItemBean ->
            val file = fileItemBean.file
            val fileName = file?.name
            val isDefaultMouse = position == 0

            val filesButton = FilesButton()
            filesButton.setButtonVisibility(false, false,
                !isDefaultMouse, !isDefaultMouse, !isDefaultMouse, (isDefaultMouse || isImage(file)))

            var message = getString(R.string.file_message)
            if (isDefaultMouse) message += """

      ${getString(R.string.custom_mouse_message_default)}
      """.trimIndent()
            filesButton.setMessageText(message)
            filesButton.setMoreButtonText(getString(R.string.generic_select))

            val filesDialog = FilesDialog(requireActivity(), filesButton, Task.runTask { loadData() }, mousePath(), file)
            filesDialog.setMoreButtonClick {
                AllSettings.customMouse.put(fileName ?: "").save()
                Toast.makeText(requireActivity(),
                    StringUtils.insertSpace(getString(R.string.custom_mouse_added), (fileName ?: getString(R.string.custom_mouse_default))),
                    Toast.LENGTH_SHORT).show()
                filesDialog.dismiss()
            }
            filesDialog.show()
        }, null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}