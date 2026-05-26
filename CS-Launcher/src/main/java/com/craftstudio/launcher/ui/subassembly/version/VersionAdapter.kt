package com.craftstudio.launcher.ui.subassembly.version

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayout
import com.craftstudio.launcher.R
import com.craftstudio.launcher.databinding.ItemVersionBinding
import com.craftstudio.launcher.databinding.ViewVersionManagerBinding
import com.craftstudio.launcher.feature.customprofilepath.ProfilePathManager
import com.craftstudio.launcher.feature.version.Version
import com.craftstudio.launcher.feature.version.utils.VersionIconUtils
import com.craftstudio.launcher.feature.version.VersionsManager
import com.craftstudio.launcher.task.Task
import com.craftstudio.launcher.ui.dialog.TipDialog
import com.craftstudio.launcher.ui.fragment.FilesFragment
import com.craftstudio.launcher.utils.ZHTools
import com.craftstudio.launcher.utils.file.FileDeletionHandler
import com.craftstudio.launcher.Tools

class VersionAdapter(
    private val parentFragment: Fragment,
    private val listener: OnVersionItemClickListener
) : RecyclerView.Adapter<VersionAdapter.ViewHolder>() {
    private val versions: MutableList<Version> = ArrayList()
    //所有的RadioButton的List，其记录了当前所代表的版本路径
    private val radioButtonList: MutableList<RadioButton> = mutableListOf()
    private var currentVersion: String? = null
    private var managerPopupWindow: PopupWindow = PopupWindow().apply {
        isFocusable = true
        isOutsideTouchable = true
    }

    @SuppressLint("NotifyDataSetChanged")
    fun refreshVersions(versions: List<Version>): Int {
        this.versions.clear()
        this.versions.addAll(versions)
        this.radioButtonList.apply {
            forEach { radioButton -> radioButton.isChecked = false }
            clear()
        }
        currentVersion = VersionsManager.getCurrentVersion()?.getVersionPath()?.absolutePath
        //查找当前版本的索引
        val currentIndex = versions.indexOfFirst { it.getVersionPath().absolutePath == currentVersion }
        notifyDataSetChanged()

        return currentIndex
    }

    fun closePopupWindow() {
        managerPopupWindow.dismiss()
    }

    private fun setCurrentVersion(context: Context, version: Version) {
        if (version.isValid()) {
            VersionsManager.saveCurrentVersion(version.getVersionName())
            currentVersion = version.getVersionPath().absolutePath
        } else {
            //版本无效时，不能设置版本，默认点击就会提示用户删除
            deleteVersion(version, context.getString(R.string.version_manager_delete_tip_invalid))
        }
        radioButtonList.forEach { radioButton -> radioButton.isChecked = radioButton.tag.toString() == currentVersion }
    }

    //删除版本前提示用户，如果版本无效，那么默认点击事件就是删除版本
    private fun deleteVersion(version: Version, deleteMessage: String) {
        val context = parentFragment.requireActivity()

        TipDialog.Builder(context)
            .setTitle(context.getString(R.string.version_manager_delete))
            .setMessage(deleteMessage)
            .setWarning()
            .setCancelable(false)
            .setConfirmClickListener {
                FileDeletionHandler(
                    context,
                    listOf(version.getVersionPath()),
                    Task.runTask {
                        VersionsManager.refresh("VersionAdapter:deleteVersion")
                    }
                ).start()
            }.showDialog()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemVersionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(versions[position])
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        radioButtonList.remove(holder.binding.radioButton)
    }

    override fun getItemCount(): Int = versions.size

    inner class ViewHolder(val binding: ItemVersionBinding) : RecyclerView.ViewHolder(binding.root) {
        private val mContext = binding.root.context

        private fun String.addInfoIfNotBlank(setRed: Boolean = false) {
            takeIf { it.isNotBlank() }?.let { string ->
                binding.versionInfoLayout.addView(getInfoTextView(string, setRed))
            }
        }

        fun bind(version: Version) {
            binding.apply {
                versionInfoLayout.removeAllViews()
                versionName.isSelected = true

                val versionPath = version.getVersionPath().absolutePath
                radioButtonList.add(
                    radioButton.apply {
                        tag = versionPath
                        isChecked = currentVersion == versionPath
                    }
                )

                versionName.text = version.getVersionName()

                if (!version.isValid()) {
                    mContext.getString(R.string.version_manager_invalid).addInfoIfNotBlank(true)
                }

                if (version.getVersionConfig().isIsolation()) {
                    mContext.getString(R.string.pedit_isolation_enabled).addInfoIfNotBlank()
                }

                version.getVersionInfo()?.let { versionInfo ->
                    versionInfoLayout.addView(getInfoTextView(versionInfo.minecraftVersion))
                    versionInfo.loaderInfo?.forEach { loaderInfo ->
                        loaderInfo.name.addInfoIfNotBlank()
                        loaderInfo.version.addInfoIfNotBlank()
                    }
                }

                favorite.setOnClickListener { _ ->
                    listener.showFavoritesDialog(version.getVersionName())
                }
                favorite.setImageDrawable(
                    ContextCompat.getDrawable(mContext,
                        if (listener.isVersionFavorited(version.getVersionName())) R.drawable.ic_favorite
                        else R.drawable.ic_favorite_border
                    )
                )

                operate.setOnClickListener { _ ->
                    showPopupWindow(operate, version)
                }

                VersionIconUtils(version).start(versionIcon)

                val onClickListener = View.OnClickListener { _ ->
                    setCurrentVersion(mContext, version)
                }
                radioButton.setOnClickListener(onClickListener)
                root.setOnClickListener(onClickListener)
            }
        }

        private fun getInfoTextView(string: String, setRed: Boolean = false): TextView {
            val textView = TextView(mContext)
            textView.text = string
            val layoutParams = FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(0, 0, Tools.dpToPx(8f).toInt(), 0)
            textView.layoutParams = layoutParams
            if (setRed) textView.setTextColor(Color.RED)
            return textView
        }

        private fun showPopupWindow(
            anchorView: View,
            version: Version
        ) {
            val context = parentFragment.requireActivity()

            val viewBinding = ViewVersionManagerBinding.inflate(LayoutInflater.from(context)).apply {
                val onClickListener = View.OnClickListener { v ->
                    when (v) {
                        gotoView -> swapPath(version.getVersionPath().absolutePath)
                        gamePath -> swapPath(version.getGameDir().absolutePath)
                        rename -> VersionsManager.openRenameDialog(context, version)
                        copy -> VersionsManager.openCopyDialog(context, version)
                        delete -> deleteVersion(version, context.getString(R.string.version_manager_delete_tip, version.getVersionName()))
                        else -> {}
                    }
                    managerPopupWindow.dismiss()
                }
                gotoView.setOnClickListener(onClickListener)
                gamePath.setOnClickListener(onClickListener)
                rename.setOnClickListener(onClickListener)
                copy.setOnClickListener(onClickListener)
                delete.setOnClickListener(onClickListener)
            }
            managerPopupWindow.apply {
                viewBinding.root.measure(0, 0)
                this.contentView = viewBinding.root
                this.width = viewBinding.root.measuredWidth
                this.height = viewBinding.root.measuredHeight
                showAsDropDown(anchorView, anchorView.measuredWidth, 0)
            }
        }

        private fun swapPath(path: String) {
            val bundle = Bundle()
            bundle.putString(FilesFragment.BUNDLE_LOCK_PATH, ProfilePathManager.getCurrentPath())
            bundle.putString(FilesFragment.BUNDLE_LIST_PATH, path)
            bundle.putBoolean(FilesFragment.BUNDLE_QUICK_ACCESS_PATHS, false)
            ZHTools.swapFragmentWithAnim(
                parentFragment,
                FilesFragment::class.java, FilesFragment.TAG, bundle
            )
        }
    }

    interface OnVersionItemClickListener {
        /**
         * 用户点击了“收藏”按钮，检查并展示“收藏”弹窗
         */
        fun showFavoritesDialog(versionName: String)

        /**
         * 检查当前版本是否被收藏了
         */
        fun isVersionFavorited(versionName: String): Boolean
    }
}