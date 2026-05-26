package com.craftstudio.launcher.ui.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.craftstudio.launcher.anim.AnimPlayer
import com.craftstudio.launcher.anim.animations.Animations
import com.craftstudio.launcher.R
import com.craftstudio.launcher.databinding.FragmentInstallGameBinding
import com.craftstudio.launcher.event.sticky.SelectInstallTaskEvent
import com.craftstudio.launcher.event.value.InstallGameEvent
import com.craftstudio.launcher.utils.LauncherProfiles
import com.craftstudio.launcher.feature.customprofilepath.ProfilePathHome
import com.craftstudio.launcher.feature.version.install.Addon
import com.craftstudio.launcher.feature.version.install.InstallArgsUtils
import com.craftstudio.launcher.feature.version.install.InstallTask
import com.craftstudio.launcher.feature.version.install.InstallTaskItem
import com.craftstudio.launcher.feature.version.VersionsManager
import com.craftstudio.launcher.setting.AllSettings
import com.craftstudio.launcher.ui.dialog.TipDialog
import com.craftstudio.launcher.ui.fragment.download.addon.DownloadFabricApiFragment
import com.craftstudio.launcher.ui.fragment.download.addon.DownloadFabricFragment
import com.craftstudio.launcher.ui.fragment.download.addon.DownloadForgeFragment
import com.craftstudio.launcher.ui.fragment.download.addon.DownloadNeoForgeFragment
import com.craftstudio.launcher.ui.fragment.download.addon.DownloadOptiFineFragment
import com.craftstudio.launcher.ui.fragment.download.addon.DownloadQuiltApiFragment
import com.craftstudio.launcher.ui.fragment.download.addon.DownloadQuiltFragment
import com.craftstudio.launcher.utils.ZHTools
import com.craftstudio.launcher.utils.file.FileTools
import com.craftstudio.launcher.utils.runtime.SelectRuntimeUtils
import com.craftstudio.launcher.JavaGUILauncherActivity
import com.craftstudio.launcher.Tools
import org.apache.commons.io.FileUtils
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.util.EnumMap

class InstallGameFragment : FragmentWithAnim(R.layout.fragment_install_game), View.OnClickListener {
    companion object {
        const val TAG = "InstallGameFragment"
        const val BUNDLE_MC_VERSION = "bundle_mc_version"
    }
    private lateinit var binding: FragmentInstallGameBinding
    private lateinit var mcVersion: String
    private val addonMap: MutableMap<Addon, Pair<String, InstallTask>> = EnumMap(Addon::class.java)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentInstallGameBinding.inflate(layoutInflater)

        EventBus.getDefault().getStickyEvent(SelectInstallTaskEvent::class.java)?.let { event ->
            addonMap[event.addon] = Pair(event.selectedVersion, event.task)
            EventBus.getDefault().removeStickyEvent(event)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mcVersion = arguments?.getString(BUNDLE_MC_VERSION) ?: throw IllegalArgumentException("The Minecraft version is not passed")

        binding.apply {
            nameEdit.setText(mcVersion)

            val clickListener = this@InstallGameFragment
            optifineLayout.setOnClickListener(clickListener)
            optifineDelete.setOnClickListener(clickListener)
            forgeLayout.setOnClickListener(clickListener)
            forgeDelete.setOnClickListener(clickListener)
            neoforgeLayout.setOnClickListener(clickListener)
            neoforgeDelete.setOnClickListener(clickListener)
            fabricLayout.setOnClickListener(clickListener)
            fabricDelete.setOnClickListener(clickListener)
            fabricApiLayout.setOnClickListener(clickListener)
            fabricApiDelete.setOnClickListener(clickListener)
            quiltLayout.setOnClickListener(clickListener)
            quiltDelete.setOnClickListener(clickListener)
            quiltApiLayout.setOnClickListener(clickListener)
            quiltApiDelete.setOnClickListener(clickListener)

            back.setOnClickListener(clickListener)
            install.setOnClickListener(clickListener)
        }
    }

    override fun onResume() {
        super.onResume()
        checkIncompatible()
    }

    /**
     * 检查不兼容的Addon，并禁止用户选择该Addon版本
     */
    @SuppressLint("SetTextI18n")
    private fun checkIncompatible() {
        binding.apply {
            checkIncompatible(Addon.OPTIFINE, optifineLayout, optifineVersion, optifineInstall, optifineDelete, addonMap.size > 1)
            checkIncompatible(Addon.FORGE, forgeLayout, forgeVersion, forgeInstall, forgeDelete)
            checkIncompatible(Addon.NEOFORGE, neoforgeLayout, neoforgeVersion, neoforgeInstall, neoforgeDelete)
            checkIncompatible(Addon.FABRIC, fabricLayout, fabricVersion, fabricInstall, fabricDelete)
            checkIncompatible(Addon.FABRIC_API, fabricApiLayout, fabricApiVersion, fabricApiInstall, fabricApiDelete, true)
            checkIncompatible(Addon.QUILT, quiltLayout, quiltVersion, quiltInstall, quiltDelete)
            checkIncompatible(Addon.QSL, quiltApiLayout, quiltApiVersion, quiltApiInstall, quiltApiDelete, true)

            val loaderName = addonMap.keys
                .firstOrNull { it != Addon.OPTIFINE || addonMap.size == 1 }
                ?.addonName.orEmpty()

            nameEdit.setText("$mcVersion $loaderName".trim())
        }
    }

    /**
     * 检查传入的Addon是否在AddonMap中有不兼容的Addon
     * @param addon 传入的Addon
     * @param layout Addon的layout
     * @param versionText Addon的版本信息
     * @param installText Addon的安装类型
     */
    private fun checkIncompatible(
        addon: Addon,
        layout: View,
        versionText: TextView,
        installText: TextView,
        imageView: ImageView,
        modInstallType: Boolean = false
    ) {
        val incompatible: MutableSet<Addon> = HashSet()
        addonMap.keys.forEach { selectedAddon ->
            if (Addon.getCompatibles(addon)?.contains(selectedAddon) == false) {
                incompatible.add(selectedAddon)
            }
        }

        if (incompatible.isNotEmpty()) {
            layout.isEnabled = false
            installText.text = getString(R.string.version_install_incompatible, incompatible.joinToString(", ", transform = { it.addonName }))
            versionText.visibility = View.GONE
            imageView.visibility = View.GONE
        } else {
            layout.isEnabled = true
            imageView.visibility = View.VISIBLE
            val version = addonMap[addon]?.first

            if (version != null) {
                versionText.visibility = View.VISIBLE
                versionText.text = version
                installText.setText(if (modInstallType) R.string.version_install_type_mod else R.string.version_install_type_version)
            } else {
                versionText.visibility = View.GONE
                installText.setText(R.string.version_install_not_install)
            }
        }

        val contains = addonMap.containsKey(addon)
        layout.isSelected = contains
        if (contains) {
            imageView.isEnabled = true
            imageView.setImageResource(R.drawable.ic_close)
        } else {
            imageView.isEnabled = false
            imageView.setImageResource(R.drawable.ic_spinner_arrow_right)
        }
    }

    /**
     * 切换至Addon版本选择界面
     */
    private fun swapFragment(fragmentClass: Class<out Fragment>, tag: String) {
        val bundle = Bundle()
        bundle.putString(BUNDLE_MC_VERSION, mcVersion)
        ZHTools.swapFragmentWithAnim(this, fragmentClass, tag, bundle)
    }

    /**
     * 移除Addon，并刷新当前不兼容的Addon
     */
    private fun removeAddon(addon: Addon) {
        addonMap.remove(addon)
        checkIncompatible()
    }

    override fun onClick(v: View) {
        val activity = requireActivity()

        binding.apply {
            when (v) {
                optifineLayout -> swapFragment(DownloadOptiFineFragment::class.java, DownloadOptiFineFragment.TAG)
                forgeLayout -> swapFragment(DownloadForgeFragment::class.java, DownloadForgeFragment.TAG)
                neoforgeLayout -> swapFragment(DownloadNeoForgeFragment::class.java, DownloadNeoForgeFragment.TAG)
                fabricLayout -> swapFragment(DownloadFabricFragment::class.java, DownloadFabricFragment.TAG)
                fabricApiLayout -> swapFragment(DownloadFabricApiFragment::class.java, DownloadFabricApiFragment.TAG)
                quiltLayout -> swapFragment(DownloadQuiltFragment::class.java, DownloadQuiltFragment.TAG)
                quiltApiLayout -> swapFragment(DownloadQuiltApiFragment::class.java, DownloadQuiltApiFragment.TAG)

                optifineDelete -> removeAddon(Addon.OPTIFINE)
                forgeDelete -> removeAddon(Addon.FORGE)
                neoforgeDelete -> removeAddon(Addon.NEOFORGE)
                fabricDelete -> removeAddon(Addon.FABRIC)
                fabricApiDelete -> removeAddon(Addon.FABRIC_API)
                quiltDelete -> removeAddon(Addon.QUILT)
                quiltApiDelete -> removeAddon(Addon.QSL)

                install -> {
                    val string = nameEdit.text?.toString()
                    if (string.isNullOrBlank()) {
                        nameEdit.error = getString(R.string.generic_error_field_empty)
                        return
                    }

                    if (FileTools.isFilenameInvalid(nameEdit)) {
                        return
                    }

                    if (VersionsManager.isVersionExists(string, true)) {
                        nameEdit.error = getString(R.string.version_install_exists)
                        return
                    }

                    if (addonMap.isNotEmpty() && string.equals(mcVersion, true)) {
                        nameEdit.error = getString(R.string.version_install_cannot_use_mc_name)
                        return
                    }

                    fun install() {
                        EventBus.getDefault().post(InstallGameEvent(mcVersion, string, organizeInstallationTasks(string)))
                        Tools.backToMainMenu(activity)
                    }

                    //检查OptiFine与Forge附加包是否同时存在
                    //最后告诉用户兼容性问题
                    if (addonMap.containsKey(Addon.OPTIFINE) && addonMap.containsKey(Addon.FORGE)) {
                        TipDialog.Builder(activity)
                            .setTitle(R.string.generic_warning)
                            .setMessage(R.string.version_install_optifine_and_forge)
                            .setWarning()
                            .setConfirmClickListener { install() }
                            .showDialog()
                    } else install()
                }
                back -> ZHTools.onBackPressed(activity)
                else -> {}
            }
        }
    }

    private fun organizeInstallationTasks(customVersionName: String): Map<Addon, InstallTaskItem> {
        val mapSize = addonMap.size
        val taskMap: MutableMap<Addon, InstallTaskItem> = EnumMap(Addon::class.java)

        fun getModPath(): File {
            return if (AllSettings.versionIsolation.getValue()) //启用了版本隔离
                File(
                    ProfilePathHome.getGameHome(),
                    "versions${File.separator}$customVersionName${File.separator}mods"
                )
            else File(ProfilePathHome.getGameHome(), "mods")
        }

        addonMap.forEach { (addon, taskPair) ->
            when (addon) {
                Addon.OPTIFINE -> {
                    val endTask: InstallTaskItem.EndTask = if (mapSize < 2) { //安装为一个版本
                        InstallTaskItem.EndTask { activity, file ->
                            installInGUITask(activity, addon.addonName, taskPair.first) { intent, argUtils ->
                                argUtils.setOptiFine(intent, file, customVersionName)
                            }
                        }
                    } else {
                        InstallTaskItem.EndTask { _, file ->
                            moveFile(file, File(getModPath(), "${taskPair.first}.jar"))
                        }
                    }
                    taskMap[addon] = InstallTaskItem(taskPair.first, mapSize > 1, taskPair.second, endTask)
                }
                Addon.FORGE -> {
                    taskMap[addon] = InstallTaskItem(taskPair.first, false, taskPair.second) {  activity, file ->
                        installInGUITask(activity, addon.addonName, taskPair.first) { intent, argUtils ->
                            argUtils.setForge(intent, file, customVersionName)
                        }
                    }
                }
                Addon.NEOFORGE -> {
                    taskMap[addon] = InstallTaskItem(taskPair.first, false, taskPair.second) {  activity, file ->
                        installInGUITask(activity, addon.addonName, taskPair.first) { intent, argUtils ->
                            argUtils.setNeoForge(intent, file, customVersionName)
                        }
                    }
                }
                Addon.FABRIC -> {
                    taskMap[addon] = InstallTaskItem(taskPair.first, false, taskPair.second) {  activity, file ->
                        installInGUITask(activity, addon.addonName, taskPair.first) { intent, argUtils ->
                            argUtils.setFabric(intent, file, customVersionName)
                        }
                    }
                }
                Addon.FABRIC_API -> taskMap[addon] = InstallTaskItem(taskPair.first, true, taskPair.second) { _, file ->
                    moveFile(file, File(getModPath(), "${taskPair.first}.jar"))
                }
                Addon.QUILT -> taskMap[addon] = InstallTaskItem(taskPair.first, false, taskPair.second, null)
                Addon.QSL -> taskMap[addon] = InstallTaskItem(taskPair.first, true, taskPair.second) { _, file ->
                    moveFile(file, File(getModPath(), "${taskPair.first}.jar"))
                }
            }
        }
        return taskMap
    }

    @Throws(Throwable::class)
    private fun moveFile(file: File, file1: File) {
        if (file1.exists()) FileUtils.deleteQuietly(file1)
        FileUtils.moveFile(file, file1)
    }

    /**
     * 在JavaGUI内进行安装，作为EndTask，需要在UI线程内运行
     * @param activity **此处必须使用activity的上下文！不能调用Fragment的上下文！！因为调用到这里的时候，Fragment早就被销毁了！！！**
     */
    @Throws(Throwable::class)
    private fun installInGUITask(activity: Activity, addonName: String, selectVersion: String, setArgs: (Intent, InstallArgsUtils) -> Unit) {
        val intent = Intent(activity, JavaGUILauncherActivity::class.java)

        val argUtils = InstallArgsUtils(mcVersion, selectVersion)
        setArgs(intent, argUtils)

        SelectRuntimeUtils.selectRuntime(activity, activity.getString(R.string.version_install_new_modloader, addonName)) { jreName ->
            LauncherProfiles.generateLauncherProfiles()
            intent.putExtra(JavaGUILauncherActivity.EXTRAS_JRE_NAME, jreName)
            activity.startActivity(intent)
        }
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.nameLayout, Animations.BounceInDown))
            .apply(AnimPlayer.Entry(binding.addonsLayout, Animations.BounceInUp))
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.nameLayout, Animations.FadeOutUp))
            .apply(AnimPlayer.Entry(binding.addonsLayout, Animations.FadeOutDown))
    }
}