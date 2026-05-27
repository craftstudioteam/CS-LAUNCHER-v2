package com.craftstudio.launcher.ui.fragment.settings

import android.content.res.Configuration
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import com.craftstudio.launcher.anim.AnimPlayer
import com.craftstudio.launcher.anim.animations.Animations
import com.craftstudio.launcher.R
import com.craftstudio.launcher.databinding.SettingsFragmentVideoBinding
import com.craftstudio.launcher.event.single.LauncherIgnoreNotchEvent
import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.plugins.driver.DriverPluginManager
import com.craftstudio.launcher.plugins.renderer.RendererPluginManager
import com.craftstudio.launcher.renderer.Renderers
import com.craftstudio.launcher.setting.AllSettings
import com.craftstudio.launcher.setting.AllStaticSettings
import com.craftstudio.launcher.task.Task
import com.craftstudio.launcher.task.TaskExecutors
import com.craftstudio.launcher.ui.dialog.LocalRendererPluginDialog
import com.craftstudio.launcher.ui.dialog.TipDialog
import com.craftstudio.launcher.ui.fragment.settings.wrapper.BaseSettingsWrapper
import com.craftstudio.launcher.ui.fragment.settings.wrapper.ListSettingsWrapper
import com.craftstudio.launcher.ui.fragment.settings.wrapper.SeekBarSettingsWrapper
import com.craftstudio.launcher.ui.fragment.settings.wrapper.SwitchSettingsWrapper
import com.craftstudio.launcher.utils.ZHTools
import com.craftstudio.launcher.utils.file.FileTools
import com.craftstudio.launcher.utils.path.PathManager
import com.craftstudio.launcher.utils.path.UrlManager
import com.craftstudio.launcher.Tools
import com.craftstudio.launcher.contracts.OpenDocumentWithExtension
import org.apache.commons.io.FileUtils
import org.greenrobot.eventbus.EventBus
import java.io.File

class VideoSettingsFragment : AbstractSettingsFragment(R.layout.settings_fragment_video, SettingCategory.VIDEO) {
    private lateinit var binding: SettingsFragmentVideoBinding
    private lateinit var openDocumentLauncher: ActivityResultLauncher<Any>
    private var isImportingDriver = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openDocumentLauncher = registerForActivityResult(OpenDocumentWithExtension("zip", true)) { uris: List<Uri>? ->
            uris?.let { uriList ->
                val dialog = ZHTools.showTaskRunningDialog(requireActivity())
                Task.runTask {
                    val pluginFiles = mutableListOf<File>()
                    uriList.forEach { uri ->
                        val file = FileTools.copyFileInBackground(requireActivity(), uri, PathManager.DIR_CACHE.absolutePath)
                        pluginFiles.add(file)
                    }
                    pluginFiles.takeIf { it.isNotEmpty() }
                }.beforeStart(TaskExecutors.getAndroidUI()) {
                    dialog.show()
                }.ended { pluginFiles ->
                    pluginFiles?.let { files ->
                        var requiresRestart = false
                        files.forEach { pluginFile ->
                            val success = if (isImportingDriver) {
                                DriverPluginManager.importLocalDriverPlugin(pluginFile)
                            } else {
                                RendererPluginManager.importLocalRendererPlugin(pluginFile)
                            }

                            if (success) {
                                requiresRestart = true
                                Logging.i("VideoSettings", "The plugin has been successfully imported!")
                            } else {
                                Logging.i("VideoSettings", "The plugin import failed!")
                            }
                            FileUtils.deleteQuietly(pluginFile)
                        }
                        TaskExecutors.runInUIThread {
                            if (requiresRestart) {
                                TipDialog.Builder(requireActivity())
                                    .setTitle(R.string.generic_warning)
                                    .setMessage(R.string.setting_renderer_local_import_restart)
                                    .setWarning()
                                    .setConfirmClickListener { ZHTools.killProcess() }
                                    .showDialog()
                            } else {
                                TipDialog.Builder(requireActivity())
                                    .setTitle(R.string.generic_tip)
                                    .setMessage(R.string.setting_renderer_local_import_failed)
                                    .showDialog()
                            }
                        }
                    }
                }.onThrowable { e ->
                    Tools.showErrorRemote(e)
                }.finallyTask(TaskExecutors.getAndroidUI()) {
                    dialog.dismiss()
                    isImportingDriver = false
                }.execute()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SettingsFragmentVideoBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = requireActivity()

        val renderers = Renderers.getCompatibleRenderers(context).first
        ListSettingsWrapper(
            context,
            AllSettings.renderer,
            binding.rendererLayout,
            binding.rendererTitle,
            binding.rendererValue,
            renderers.rendererNames.toTypedArray(),
            renderers.rendererIdentifier.toTypedArray()
        ).setOnSaveListener {
            computeVisibility()
        }

        binding.rendererDownload.setOnClickListener { ZHTools.openLink(context, UrlManager.URL_FCL_RENDERER_PLUGIN) }

        BaseSettingsWrapper(
            context,
            binding.rendererLocalImportLayout
        ) {
            isImportingDriver = false
            openDocumentLauncher.launch("zip")
        }

        binding.rendererLocalImportManage.setOnClickListener {
            if (RendererPluginManager.getAllLocalRendererList().isNotEmpty()) {
                LocalRendererPluginDialog(requireActivity()).show()
            }
        }

        val driverNames = DriverPluginManager.getDriverNameList().toTypedArray()
        ListSettingsWrapper(
            context,
            AllSettings.driver,
            binding.driverLayout,
            binding.driverTitle,
            binding.driverValue,
            driverNames,
            driverNames
        )

        binding.driverDownload.setOnClickListener { ZHTools.openLink(context, UrlManager.URL_FCL_DRIVER_PLUGIN) }

        val vulkanDriverNames = DriverPluginManager.getDriverNameList().toTypedArray()
        ListSettingsWrapper(
            context,
            AllSettings.vulkanDriver,
            binding.vulkanDriverLayout,
            binding.vulkanDriverTitle,
            binding.vulkanDriverValue,
            vulkanDriverNames,
            vulkanDriverNames
        ).setOnSaveListener {
            DriverPluginManager.setDriverByName(AllSettings.vulkanDriver.getValue())
        }

        binding.vulkanDriverImportManage.setOnClickListener {
            isImportingDriver = true
            openDocumentLauncher.launch("zip")
        }

        val ignoreNotch = SwitchSettingsWrapper(
            context,
            AllSettings.ignoreNotch,
            binding.ignoreNotchLayout,
            binding.ignoreNotch
        )

        val ignoreNotchLauncher = SwitchSettingsWrapper(
            context,
            AllSettings.ignoreNotchLauncher,
            binding.ignoreNotchLauncherLayout,
            binding.ignoreNotchLauncher
        ).setOnCheckedChangeListener { _, _, listener ->
            listener.onSave()
            EventBus.getDefault().post(LauncherIgnoreNotchEvent())
        }

        if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && AllStaticSettings.notchSize > 0)) {
            ignoreNotch.setGone()
            ignoreNotchLauncher.setGone()
        }

        SeekBarSettingsWrapper(
            context,
            AllSettings.resolutionRatio,
            binding.resolutionRatioLayout,
            binding.resolutionRatioTitle,
            binding.resolutionRatioSummary,
            binding.resolutionRatioValue,
            binding.resolutionRatio,
            "%"
        ) { wrapper ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                wrapper.seekbarView.min = 50
            }
            wrapper.seekbarView.max = 100

            val clamped = AllSettings.resolutionRatio.getValue().coerceIn(50, 100)
            if (clamped != AllSettings.resolutionRatio.getValue()) {
                AllSettings.resolutionRatio.put(clamped).save()
            }
            wrapper.seekbarView.progress = clamped
        }
            .setOnSeekBarProgressChangeListener { progress ->
                val clamped = progress.coerceIn(50, 100)
                if (binding.resolutionRatio.progress != clamped) {
                    binding.resolutionRatio.progress = clamped
                }
                changeResolutionRatioPreview(clamped)
            }

        SwitchSettingsWrapper(
            context,
            AllSettings.sustainedPerformance,
            binding.sustainedPerformanceLayout,
            binding.sustainedPerformance
        )

        SwitchSettingsWrapper(
            context,
            AllSettings.alternateSurface,
            binding.alternateSurfaceLayout,
            binding.alternateSurface
        )

        SwitchSettingsWrapper(
            context,
            AllSettings.forceVsync,
            binding.forceVsyncLayout,
            binding.forceVsync
        )

        SwitchSettingsWrapper(
            context,
            AllSettings.vsyncInZink,
            binding.vsyncInZinkLayout,
            binding.vsyncInZink
        )

        val zinkPreferSystemDriver = SwitchSettingsWrapper(
            context,
            AllSettings.zinkPreferSystemDriver,
            binding.zinkPreferSystemDriverLayout,
            binding.zinkPreferSystemDriver
        )
        if (!Tools.checkVulkanSupport(context.packageManager)) {
            zinkPreferSystemDriver.setGone()
        } else {
            zinkPreferSystemDriver.setOnCheckedChangeListener { buttonView, isChecked, listener ->
                if (isChecked and ZHTools.isAdrenoGPU()) {
                    TipDialog.Builder(requireActivity())
                        .setTitle(R.string.generic_warning)
                        .setMessage(R.string.setting_zink_driver_adreno)
                        .setWarning()
                        .setCancelable(false)
                        .setConfirmClickListener { listener.onSave() }
                        .setCancelClickListener { buttonView.isChecked = false }
                        .showDialog()
                } else {
                    listener.onSave()
                }
            }
        }

        changeResolutionRatioPreview(AllSettings.resolutionRatio.getValue())
        computeVisibility()
    }

    private fun changeResolutionRatioPreview(progress: Int) {
        binding.resolutionRatioPreview.text = getResolutionRatioPreview(resources, progress)
    }

    override fun onChange() {
        super.onChange()
        computeVisibility()
    }

    private fun computeVisibility() {
        binding.apply {
            binding.forceVsyncLayout.visibility = if (AllSettings.alternateSurface.getValue()) View.VISIBLE else View.GONE
            
            val currentRendererId = AllSettings.renderer.getValue()
            val isVulkan = currentRendererId == "0fa435e2-46df-45c9-906c-b29606aaef00" || // Zink
                           currentRendererId == "1e7845f3-3158-469b-980b-967969149492"    // Freedreno (if any)
            
            binding.vulkanDriverLayout.visibility = if (isVulkan) View.VISIBLE else View.GONE
            binding.vulkanDriverImportLayout.visibility = if (isVulkan) View.VISIBLE else View.GONE
        }
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.root, Animations.BounceInDown))
    }

    companion object {
        @JvmStatic
        fun getResolutionRatioPreview(resources: Resources, progress: Int): String {
            val metrics = Tools.currentDisplayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE || width > height

            val progressFloat = progress.toFloat() / 100F
            val previewWidth = Tools.getDisplayFriendlyRes((if (isLandscape) width else height), progressFloat)
            val previewHeight = Tools.getDisplayFriendlyRes((if (isLandscape) height else width), progressFloat)

            return "$previewWidth x $previewHeight"
        }
    }
}