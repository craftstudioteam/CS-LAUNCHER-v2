package com.craftstudio.launcher.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.craftstudio.launcher.anim.AnimPlayer
import com.craftstudio.launcher.anim.animations.Animations
import com.craftstudio.launcher.R
import com.craftstudio.launcher.databinding.FragmentVersionManagerBinding
import com.craftstudio.launcher.feature.version.NoVersionException
import com.craftstudio.launcher.feature.version.Version
import com.craftstudio.launcher.feature.version.VersionsManager
import com.craftstudio.launcher.task.Task
import com.craftstudio.launcher.task.TaskExecutors
import com.craftstudio.launcher.ui.dialog.TipDialog
import com.craftstudio.launcher.utils.ZHTools
import com.craftstudio.launcher.utils.file.FileDeletionHandler
import com.craftstudio.launcher.Tools
import java.io.File

class VersionManagerFragment : FragmentWithAnim(R.layout.fragment_version_manager), View.OnClickListener {
    companion object {
        const val TAG: String = "VersionManagerFragment"
    }

    private lateinit var binding: FragmentVersionManagerBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVersionManagerBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Wire up back button
        view.findViewById<View>(R.id.btn_back)?.setOnClickListener {
            ZHTools.onBackPressed(requireActivity())
        }

        val fragment = this
        binding.apply {
            shortcutsMods.setOnClickListener(fragment)
            gamePath.setOnClickListener(fragment)
            resourcePath.setOnClickListener(fragment)
            worldPath.setOnClickListener(fragment)
            shaderPath.setOnClickListener(fragment)
            screenshotPath.setOnClickListener(fragment)
            logsPath.setOnClickListener(fragment)
            crashReportPath.setOnClickListener(fragment)
            versionSettings.setOnClickListener(fragment)
            versionRename.setOnClickListener(fragment)
            versionCopy.setOnClickListener(fragment)
            versionDelete.setOnClickListener(fragment)
        }
    }

    private fun File.mustExists(): File {
        if (!exists()) {
            mkdirs()
        }
        return this
    }

    private fun swapFilesFragment(lockPath: File, listPath: File) {
        val bundle = Bundle()
        bundle.putString(FilesFragment.BUNDLE_LOCK_PATH, lockPath.mustExists().absolutePath)
        bundle.putString(FilesFragment.BUNDLE_LIST_PATH, listPath.mustExists().absolutePath)
        bundle.putBoolean(FilesFragment.BUNDLE_QUICK_ACCESS_PATHS, false)

        ZHTools.swapFragmentWithAnim(this, FilesFragment::class.java, FilesFragment.TAG, bundle)
    }

    override fun onClick(v: View) {
        val activity = requireActivity()
        val version: Version = VersionsManager.getCurrentVersion() ?: run {
            Tools.showError(activity, getString(R.string.version_manager_no_installed_version), NoVersionException("There is no installed version"))
            return
        }
        val gameDirPath = version.getGameDir()

        binding.apply {
            when (v) {
                shortcutsMods -> {
                    val bundle = Bundle()
                    bundle.putString(ManageModsFragment.BUNDLE_ROOT_PATH, File(gameDirPath, "/mods").mustExists().absolutePath)
                    ZHTools.swapFragmentWithAnim(this@VersionManagerFragment, ManageModsFragment::class.java, ManageModsFragment.TAG, bundle)
                }
                gamePath -> swapFilesFragment(gameDirPath, gameDirPath)
                resourcePath -> swapFilesFragment(gameDirPath, File(gameDirPath, "/resourcepacks"))
                worldPath -> swapFilesFragment(gameDirPath, File(gameDirPath, "/saves"))
                shaderPath -> swapFilesFragment(gameDirPath, File(gameDirPath, "/shaderpacks"))
                screenshotPath -> swapFilesFragment(gameDirPath, File(gameDirPath, "/screenshots"))
                logsPath -> swapFilesFragment(gameDirPath, File(gameDirPath, "/logs"))
                crashReportPath -> swapFilesFragment(gameDirPath, File(gameDirPath, "/crash-reports"))

                versionSettings -> ZHTools.swapFragmentWithAnim(this@VersionManagerFragment, VersionConfigFragment::class.java, VersionConfigFragment.TAG, null)
                versionRename -> {
                    VersionsManager.openRenameDialog(activity, version) {
                        Tools.backToMainMenu(activity) //重命名前，为了不出现问题，需要退出当前Fragment
                    }
                }
                versionCopy -> VersionsManager.openCopyDialog(activity, version)
                versionDelete -> {
                    TipDialog.Builder(activity)
                        .setTitle(R.string.generic_warning)
                        .setMessage(activity.getString(R.string.version_manager_delete_tip, version.getVersionName()))
                        .setWarning()
                        .setConfirmClickListener {
                            FileDeletionHandler(
                                activity,
                                listOf(version.getVersionPath()),
                                Task.runTask {
                                    VersionsManager.refresh("VersionManagerFragment:versionDelete")
                                }.ended(TaskExecutors.getAndroidUI()) {
                                    Tools.backToMainMenu(activity)
                                }
                            ).start()
                        }
                        .showDialog()
                }
                else -> {}
            }
        }
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(shortcutsLayout, Animations.BounceInRight))
                .apply(AnimPlayer.Entry(editLayout, Animations.BounceInLeft))
        }
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(shortcutsLayout, Animations.FadeOutLeft))
                .apply(AnimPlayer.Entry(editLayout, Animations.FadeOutRight))
        }
    }
}
