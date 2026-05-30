package com.craftstudio.launcher.ui.fragment

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import com.craftstudio.launcher.anim.AnimPlayer
import com.craftstudio.launcher.anim.animations.Animations
import com.craftstudio.launcher.R
import com.craftstudio.launcher.databinding.FragmentVersionConfigBinding
import com.craftstudio.launcher.event.sticky.FileSelectorEvent
import com.craftstudio.launcher.feature.customprofilepath.ProfilePathManager
import com.craftstudio.launcher.feature.version.NoVersionException
import com.craftstudio.launcher.feature.version.Version
import com.craftstudio.launcher.feature.version.VersionConfig
import com.craftstudio.launcher.feature.version.VersionConfig.CREATOR.getIsolationString
import com.craftstudio.launcher.feature.version.VersionConfig.IsolationType
import com.craftstudio.launcher.feature.version.VersionsManager.getCurrentVersion
import com.craftstudio.launcher.feature.version.utils.VersionIconUtils
import com.craftstudio.launcher.plugins.driver.DriverPluginManager
import com.craftstudio.launcher.renderer.Renderers
import com.craftstudio.launcher.setting.AllSettings.Companion.versionIsolation
import com.craftstudio.launcher.task.Task
import com.craftstudio.launcher.task.TaskExecutors.Companion.getAndroidUI
import com.craftstudio.launcher.ui.dialog.TipDialog
import com.craftstudio.launcher.ui.subassembly.adapter.ObjectSpinnerAdapter
import com.craftstudio.launcher.utils.ZHTools
import com.craftstudio.launcher.utils.file.FileTools.Companion.copyFileInBackground
import com.skydoves.powerspinner.DefaultSpinnerAdapter
import com.skydoves.powerspinner.OnSpinnerItemSelectedListener
import com.skydoves.powerspinner.PowerSpinnerView
import com.craftstudio.launcher.Tools
import com.craftstudio.launcher.multirt.MultiRTUtils
import com.craftstudio.launcher.multirt.Runtime
import org.greenrobot.eventbus.EventBus
import kotlin.enums.EnumEntries

class VersionConfigFragment : FragmentWithAnim(R.layout.fragment_version_config), View.OnClickListener {
    private lateinit var binding: FragmentVersionConfigBinding
    private lateinit var currentVersion: Version
    private lateinit var mVersionIconUtils: VersionIconUtils
    private var mBackupConfig: VersionConfig? = null
    private var mTempConfig: VersionConfig? = null

    private val resetIconAnimPlayer = AnimPlayer()
    private val openDocumentLauncher = registerForActivityResult<Array<String>, Uri>(ActivityResultContracts.OpenDocument()) { result: Uri? ->
        result?.let {
            val dialog = ZHTools.showTaskRunningDialog(requireActivity())
            Task.runTask {
                copyFileInBackground(requireActivity(), it, mVersionIconUtils.getIconFile())
            }.ended(getAndroidUI()) { refreshIcon(false) }
             .finallyTask(getAndroidUI()) { dialog.dismiss() }
             .onThrowable { Tools.showErrorRemote(it) }
             .execute()
        }
    }
    private var mSelectPathMark = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val fileSelectorEvent = EventBus.getDefault().getStickyEvent(FileSelectorEvent::class.java)

        fileSelectorEvent?.path?.let { path ->
            when (mSelectPathMark) {
                SELECT_CONTROL -> mTempConfig?.setControl(path)
                SELECT_CUSTOM_PATH -> mTempConfig?.setCustomPath(path)
                else -> {}
            }
        }

        fileSelectorEvent?.let { EventBus.getDefault().removeStickyEvent(it) }

        binding = FragmentVersionConfigBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val version = getCurrentVersion()
        version ?: run {
            Tools.showError(requireActivity(), getString(R.string.version_manager_no_installed_version), NoVersionException("There is no installed version"))
            ZHTools.onBackPressed(requireActivity())
            return
        }
        currentVersion = version

        val listener = this
        binding.apply {
            cancelButton.setOnClickListener(listener)
            saveButton.setOnClickListener(listener)
            iconLayout.setOnClickListener(listener)
            iconReset.setOnClickListener(listener)

            controlName.setOnClickListener(listener)
            resetControl.setOnClickListener(listener)
            customPath.setOnClickListener(listener)
            resetCustomPath.setOnClickListener(listener)

            customInfoEdit.doAfterTextChanged { text ->
                mTempConfig?.setCustomInfo(getEditableValue(text))
            }
            jvmArgsEdit.doAfterTextChanged { text ->
                mTempConfig?.setJavaArgs(getEditableValue(text))
            }

            initSpinners(isolationType, rendererSpinner, driverSpinner, runtimeSpinner)
            
            // IMPROVEMENT 2: VulkanMod info banner
            checkVulkanMod()
        }

        val versionConfig = currentVersion.getVersionConfig()
        if (mTempConfig == null) mTempConfig = versionConfig
        if (mBackupConfig == null) mBackupConfig = versionConfig.copy()
        mVersionIconUtils = VersionIconUtils(currentVersion)

        disableCustomPath(versionConfig.isIsolation())

        loadValues(view.context)
        refreshIcon(true)
    }

    private fun disableCustomPath(disable: Boolean) {
        binding.customPath.isEnabled = !disable
        binding.resetCustomPath.isEnabled = !disable
    }

    private fun initSpinners(vararg spinners: PowerSpinnerView) {
        spinners.forEach { spinner ->
            spinner.apply {
                setIsFocusable(true)
                lifecycleOwner = this@VersionConfigFragment
            }
        }
    }

    private fun closeSpinner() {
        binding.isolationType.dismiss()
        binding.rendererSpinner.dismiss()
        binding.driverSpinner.dismiss()
        binding.runtimeSpinner.dismiss()
    }

    override fun onClick(v: View?) {
        val activity = requireActivity()
        binding.apply {
            when (v) {
                cancelButton -> ZHTools.onBackPressed(activity)
                saveButton -> {
                    save()
                    Tools.backToMainMenu(requireActivity())
                }
                iconLayout -> openDocumentLauncher.launch(arrayOf("image/*"))
                iconReset -> resetIcon()

                controlName -> {
                    mSelectPathMark = SELECT_CONTROL
                    ZHTools.swapFragmentWithAnim(
                        this@VersionConfigFragment,
                        ControlButtonFragment::class.java, ControlButtonFragment.TAG,
                        Bundle().apply {
                            putBoolean(ControlButtonFragment.BUNDLE_SELECT_CONTROL, true)
                        }
                    )
                }
                resetControl -> {
                    controlName.text = ""
                    mTempConfig?.setControl("")
                }
                customPath -> {
                    mSelectPathMark = SELECT_CUSTOM_PATH
                    ZHTools.swapFragmentWithAnim(
                        this@VersionConfigFragment,
                        FilesFragment::class.java, FilesFragment.TAG,
                        Bundle().apply {
                            putBoolean(FilesFragment.BUNDLE_SELECT_FOLDER_MODE, true)
                            putBoolean(FilesFragment.BUNDLE_SHOW_FILE, false)
                            putBoolean(FilesFragment.BUNDLE_REMOVE_LOCK_PATH, false)
                            putBoolean(FilesFragment.BUNDLE_QUICK_ACCESS_PATHS, false)
                            putString(FilesFragment.BUNDLE_LOCK_PATH, ProfilePathManager.getCurrentPath())
                            putString(FilesFragment.BUNDLE_LIST_PATH, currentVersion.getVersionsFolder())
                        }
                    )
                }
                resetCustomPath -> {
                    customPath.text = ""
                    mTempConfig?.setCustomPath("")
                }
                else -> {}
            }
        }
    }

    override fun onPause() {
        closeSpinner()
        super.onPause()
    }

    override fun onDestroyView() {
        closeSpinner()
        super.onDestroyView()
    }

    override fun onBackPressed(): Boolean {
        val tempConfig = mTempConfig ?: return true
        val backupConfig = mBackupConfig ?: return true

        if (tempConfig.checkDifferent(backupConfig)) {
            TipDialog.Builder(requireActivity())
                .setTitle(R.string.generic_warning)
                .setMessage(R.string.pedit_unsaved_tip)
                .setWarning()
                .setConfirmClickListener { forceBack() }
                .showDialog()
            return false
        }
        return true
    }

    /**
     * 刷新图标，并对重置图标的按钮播放显示或隐藏的动画
     * @param init 首次刷新不需要对重置按钮播放动画
     */
    private fun refreshIcon(init: Boolean) {
        binding.apply {
            val isCustomIcon = mVersionIconUtils.start(icon)
            if (init) {
                iconReset.visibility = if (isCustomIcon) View.VISIBLE else View.GONE
            } else {
                resetIconAnimPlayer.clearEntries()
                resetIconAnimPlayer.apply(
                    AnimPlayer.Entry(iconReset, if (isCustomIcon) Animations.BounceEnlarge else Animations.BounceShrink)
                ).setOnStart {
                    iconReset.visibility = View.VISIBLE
                    iconReset.isEnabled = false
                }.setOnEnd {
                    iconReset.visibility = if (isCustomIcon) View.VISIBLE else View.GONE
                    iconReset.isEnabled = isCustomIcon
                }.start()
            }
        }
    }

    private fun resetIcon() {
        TipDialog.Builder(requireActivity())
            .setTitle(R.string.generic_warning)
            .setMessage(R.string.pedit_reset_icon)
            .setWarning()
            .setConfirmClickListener {
                mVersionIconUtils.resetIcon()
                refreshIcon(false)
            }.showDialog()
    }

    private fun loadValues(context: Context) {
        mTempConfig?.let { config ->
            binding.apply {
                //版本隔离
                val isolationTypes: EnumEntries<IsolationType> = IsolationType.entries
                val isolationAdapter = ObjectSpinnerAdapter<IsolationType>(isolationType) { getIsolationString(requireActivity(), it) }
                isolationAdapter.setItems(isolationTypes)
                isolationType.setSpinnerAdapter(isolationAdapter)
                isolationType.selectItemByIndex(
                    isolationTypes.indexOf(config.getIsolationType())
                        .coerceAtLeast(0)
                        .coerceAtMost(isolationTypes.size - 1)
                )
                isolationType.setOnSpinnerItemSelectedListener(
                    OnSpinnerItemSelectedListener { _: Int, _: IsolationType?, _: Int, newItem: IsolationType ->
                        config.setIsolationType(newItem)
                        when (newItem) {
                            IsolationType.ENABLE -> disableCustomPath(true)
                            IsolationType.DISABLE -> disableCustomPath(false)
                            else -> disableCustomPath(versionIsolation.getValue())
                        }
                    })

                //控制布局
                controlName.text = config.getControl()
                //自定义路径
                customPath.text = config.getCustomPath().replaceFirst(ProfilePathManager.getCurrentPath().toRegex(), ".")

                //渲染器
                val renderersList = Renderers.getCompatibleRenderers(context).first
                val rendererNames: MutableList<String> = ArrayList(renderersList.rendererIdentifier)
                val renderList: MutableList<String> = ArrayList(renderersList.rendererNames.size + 1)
                renderList.addAll(renderersList.rendererNames)
                renderList.add(context.getString(R.string.generic_default))
                var rendererIndex = renderList.size - 1
                if (config.getRenderer().isNotEmpty()) {
                    val index = rendererNames.indexOf(config.getRenderer())
                    if (index != -1) rendererIndex = index
                }
                val rendererAdapter = DefaultSpinnerAdapter(rendererSpinner)
                rendererAdapter.setItems(renderList)
                rendererSpinner.setSpinnerAdapter(rendererAdapter)
                rendererSpinner.selectItemByIndex(rendererIndex)
                rendererSpinner.setOnSpinnerItemSelectedListener(
                    OnSpinnerItemSelectedListener { _: Int, _: String?, i1: Int, _: String? ->
                        if (i1 == renderList.size - 1) config.setRenderer("")
                        else config.setRenderer(rendererNames[i1])
                    })

                //驱动器
                val driverNames = DriverPluginManager.getDriverNameList()
                val driverList = ArrayList(driverNames)
                driverList.add(context.getString(R.string.generic_default))
                var driverIndex = driverList.size - 1
                if (config.getDriver().isNotEmpty()) {
                    val index = driverNames.indexOf(config.getDriver())
                    if (index != -1) driverIndex = index
                }
                val driverAdapter = DefaultSpinnerAdapter(driverSpinner)
                driverAdapter.setItems(driverList)
                driverSpinner.setSpinnerAdapter(driverAdapter)
                driverSpinner.selectItemByIndex(driverIndex)
                driverSpinner.setOnSpinnerItemSelectedListener(
                    OnSpinnerItemSelectedListener { _: Int, _: String?, i1: Int, _: String? ->
                        if (i1 == driverList.size - 1) config.setDriver("")
                        else config.setDriver(driverNames[i1])
                    })

                //自定义信息
                customInfoEdit.setText(config.getCustomInfo())
                //JVM 启动参数
                jvmArgsEdit.setText(config.getJavaArgs())

                //Java 运行环境
                val runtimes = MultiRTUtils.getRuntimes()
                val runtimeNames: MutableList<String> = ArrayList()
                runtimes.forEach { v: Runtime ->
                    runtimeNames.add(String.format("%s - %s", v.name, v.versionString ?: getString(R.string.multirt_runtime_corrupt)))
                }
                runtimeNames.add(getString(R.string.install_auto_select))
                var jvmIndex = runtimeNames.size - 1
                if (config.getJavaDir().isNotEmpty()) {
                    val selectedRuntime = config.getJavaDir().substring(Tools.LAUNCHERPROFILES_RTPREFIX.length)
                    val index = runtimes.indexOf(Runtime(selectedRuntime))
                    if (index != -1) jvmIndex = index
                }
                val runtimeAdapter = DefaultSpinnerAdapter(runtimeSpinner)
                runtimeAdapter.setItems(runtimeNames)
                runtimeSpinner.setSpinnerAdapter(runtimeAdapter)
                runtimeSpinner.selectItemByIndex(jvmIndex)
                runtimeSpinner.setOnSpinnerItemSelectedListener(
                    OnSpinnerItemSelectedListener { _: Int, _: String?, i1: Int, _: String? ->
                        if (i1 == runtimeNames.size - 1) config.setJavaDir("")
                        else {
                            val runtime = runtimes[i1]
                            config.setJavaDir(if (runtime.versionString == null) "" else Tools.LAUNCHERPROFILES_RTPREFIX + runtime.name)
                        }
                    })
            }
        }
    }

    private fun save() {
        mTempConfig?.let {
            it.save()
            mBackupConfig = it
        }
        Toast.makeText(requireActivity(), getString(R.string.generic_saved), Toast.LENGTH_SHORT).show()
    }

    private fun getEditableValue(editable: Editable?): String {
        return editable?.toString() ?: ""
    }

    private fun checkVulkanMod() {
        val modsFolder = File(currentVersion.getGameDir(), "mods")
        val hasVulkanMod = modsFolder.listFiles()?.any { it.name.lowercase().contains("vulkanmod") } ?: false
        val isModern = isModernMinecraft(currentVersion.getVersionName())
        val isFabric = currentVersion.getVersionName().lowercase().contains("fabric")

        binding.apply {
            if (hasVulkanMod) {
                vulkanModBanner.visibility = View.VISIBLE
                vulkanModText.text = "✅ VulkanMod detected — Best FPS for MC 1.17+"
                vulkanModText.setTextColor(requireContext().getColor(R.color.green))
            } else if (isModern) {
                vulkanModBanner.visibility = View.VISIBLE
                if (isFabric) {
                    vulkanModText.text = "💡 VulkanMod install karo maximum FPS ke liye"
                } else {
                    vulkanModText.text = "💡 Tip: MC 1.17+ pe Fabric + VulkanMod use karein best FPS ke liye"
                }
                vulkanModText.setTextColor(requireContext().getColor(R.color.gray))
                vulkanModBanner.setOnClickListener {
                    ZHTools.openLink(requireContext(), "https://modrinth.com/mod/vulkan-mod")
                }
            } else {
                vulkanModBanner.visibility = View.GONE
            }
        }
    }

    private fun isModernMinecraft(version: String): Boolean {
        val versionRegex = """(\d+)\.(\d+)(\.(\d+))?""".toRegex()
        val mcVer = version.replace("""-(?i)(forge|fabric|quilt|optifine).*""".toRegex(), "")
        val match = versionRegex.find(mcVer) ?: return false
        val major = match.groupValues[1].toIntOrNull() ?: 0
        val minor = match.groupValues[2].toIntOrNull() ?: 0
        return (major > 1) || (major == 1 && minor >= 17)
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.editorLayout, Animations.BounceInDown))
            .apply(AnimPlayer.Entry(binding.operateLayout, Animations.BounceInLeft))
            .apply(AnimPlayer.Entry(binding.iconLayout, Animations.Wobble))
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.editorLayout, Animations.FadeOutUp))
            .apply(AnimPlayer.Entry(binding.operateLayout, Animations.FadeOutRight))
    }

    companion object {
        const val TAG: String = "VersionConfigFragment"
        private const val SELECT_CONTROL = "SELECT_CONTROL"
        private const val SELECT_CUSTOM_PATH = "SELECT_CUSTOM_PATH"
    }
}
