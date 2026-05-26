package com.craftstudio.launcher.feature.download.platform

import android.content.Context
import android.widget.Toast
import com.kdt.mcgui.ProgressLayout
import com.craftstudio.launcher.R
import com.craftstudio.launcher.context.ContextExecutor
import com.craftstudio.launcher.feature.download.Filters
import com.craftstudio.launcher.feature.download.enums.Classify
import com.craftstudio.launcher.feature.download.item.InfoItem
import com.craftstudio.launcher.feature.download.item.ModLoaderWrapper
import com.craftstudio.launcher.feature.download.item.ScreenshotItem
import com.craftstudio.launcher.feature.download.item.SearchResult
import com.craftstudio.launcher.feature.download.item.VersionItem
import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.feature.mod.modpack.install.ModPackUtils
import com.craftstudio.launcher.feature.version.NoVersionException
import com.craftstudio.launcher.feature.version.VersionConfig
import com.craftstudio.launcher.feature.version.VersionsManager
import com.craftstudio.launcher.setting.AllSettings
import com.craftstudio.launcher.task.Task
import com.craftstudio.launcher.task.TaskExecutors
import com.craftstudio.launcher.ui.dialog.EditTextDialog
import com.craftstudio.launcher.utils.ZHTools
import com.craftstudio.launcher.utils.file.FileTools
import com.craftstudio.launcher.Tools
import com.craftstudio.launcher.modloaders.modpacks.api.ApiHandler
import com.craftstudio.launcher.utils.DownloadUtils
import org.jackhuang.hmcl.ui.versions.ModTranslations
import java.io.File

abstract class AbstractPlatformHelper(val api: ApiHandler) {
    @Throws(Throwable::class)
    fun search(classify: Classify, filters: Filters, lastResult: SearchResult): SearchResult? {
        return when (classify) {
            Classify.ALL -> throw IllegalArgumentException("Cannot be the enum value ${Classify.ALL}")
            Classify.MOD -> searchMod(filters, lastResult)
            Classify.MODPACK -> searchModPack(filters, lastResult)
            Classify.RESOURCE_PACK -> searchResourcePack(filters, lastResult)
            Classify.WORLD -> searchWorld(filters, lastResult)
            Classify.SHADER_PACK -> searchShaderPack(filters, lastResult)
        }
    }

    @Throws(Throwable::class)
    fun getVersions(infoItem: InfoItem, force: Boolean): List<VersionItem>? {
        return when (infoItem.classify) {
            Classify.ALL -> throw IllegalArgumentException("Cannot be the enum value ${Classify.ALL}")
            Classify.MOD -> getModVersions(infoItem, force)
            Classify.MODPACK -> getModPackVersions(infoItem, force)
            Classify.RESOURCE_PACK -> getResourcePackVersions(infoItem, force)
            Classify.WORLD -> getWorldVersions(infoItem, force)
            Classify.SHADER_PACK -> getShaderPackVersions(infoItem, force)
        }
    }

    @Throws(Throwable::class)
    fun install(context: Context, infoItem: InfoItem, version: VersionItem, isTaskRunning: (progressKey: String) -> Boolean) {
        try {
            when (infoItem.classify) {
                Classify.ALL -> throw IllegalArgumentException("Cannot be the enum value ${Classify.ALL}")
                Classify.MOD -> {
                    val mod = ModTranslations.getTranslationsByRepositoryType(infoItem.classify)
                        .getModByCurseForgeId(infoItem.slug)
                    customPath(
                        context, version, getModsPath(), infoItem.title,
                        translatedName = mod?.name, taskRunning = isTaskRunning, install = { targetPath, progressKey ->
                        installMod(infoItem, version, targetPath, progressKey)
                    })
                }
                Classify.MODPACK -> {
                    EditTextDialog.Builder(context)
                        .setTitle(R.string.version_install_new)
                        .setEditText(FileTools.ensureValidFilename(infoItem.title))
                        .setConfirmListener { editText, _ ->
                            val customName = editText.text.toString()
                            if (FileTools.isFilenameInvalid(editText)) {
                                return@setConfirmListener false
                            }

                            if (VersionsManager.isVersionExists(customName, true)) {
                                // ✅ FIX: Cast to EditText to resolve the 'error' property
                                (editText as android.widget.EditText).error = context.getString(R.string.version_install_exists)
                                return@setConfirmListener false
                            }

                            if (!isTaskRunning(ProgressLayout.INSTALL_RESOURCE)) {
                                ProgressLayout.setProgress(ProgressLayout.INSTALL_RESOURCE, 0, R.string.generic_waiting)
                                Task.runTask {
                                    val modloader = installModPack(version, customName)

                                    val versionPath = VersionsManager.getVersionPath(customName)
                                    VersionConfig.createIsolation(versionPath).save()

                                    fun downloadIcon() {
                                        runCatching {
                                            infoItem.iconUrl?.let { DownloadUtils.downloadFile(it, VersionsManager.getVersionIconFile(customName)) }
                                        }.onFailure { e ->
                                            Logging.e("Install Version", "Failed to download the icon.", e)
                                        }
                                    }

                                    modloader?.let { mod ->
                                        mod.getDownloadTask()?.let { downloadTask ->
                                            TaskExecutors.runInUIThread {
                                                Toast.makeText(context, context.getString(R.string.modpack_prepare_mod_loader_installation), Toast.LENGTH_SHORT).show()
                                            }

                                            Logging.i("Install Version", "Installing ModLoader: ${mod.modLoaderVersion}")
                                            downloadTask.run(customName)?.let { file ->
                                                downloadIcon()
                                                return@runTask Pair(mod, file)
                                            }
                                        }
                                    }

                                    downloadIcon()

                                    return@runTask null
                                }.ended { filePair ->
                                    filePair?.let {
                                        ModPackUtils.startModLoaderInstall(it.first, ContextExecutor.getActivity(), it.second, customName)
                                    }
                                }.onThrowable { e ->
                                    Tools.showErrorRemote(context, R.string.modpack_install_download_failed, e)
                                }.execute()
                            }

                            true
                        }.showDialog()
                }
                Classify.RESOURCE_PACK -> {
                    customPath(
                        context, version, getResourcePackPath(), infoItem.title,
                        taskRunning = isTaskRunning, install = { targetPath, progressKey ->
                        installResourcePack(infoItem, version, targetPath, progressKey)
                    })
                }
                Classify.WORLD -> {
                    customPath(
                        context, version, getWorldPath(), infoItem.title,
                        taskRunning = isTaskRunning, install = { targetPath, progressKey ->
                        installWorld(infoItem, version, targetPath, progressKey)
                    })
                }
                Classify.SHADER_PACK -> {
                    customPath(
                        context, version, getShaderPackPath(), infoItem.title,
                        taskRunning = isTaskRunning, install = { targetPath, progressKey ->
                        installShaderPack(infoItem, version, targetPath, progressKey)
                    })
                }
            }
        } catch (e: NoVersionException) {
            Tools.showError(context, context.getString(R.string.version_manager_no_installed_version), e)
        }
    }

    private fun customPath(
        context: Context,
        version: VersionItem,
        targetPath: File,
        name: String,
        translatedName: String? = null,
        taskRunning: (path: String) -> Boolean,
        install: (File, String) -> Unit
    ) {
        val file = File(version.fileName)
        val fileName = "".takeIf { !AllSettings.addFullResourceName.getValue() }
            ?: "[${if (ZHTools.areaChecks("zh") && translatedName?.isNotEmpty() == true) translatedName else name}] "

        EditTextDialog.Builder(context)
            .setTitle(R.string.download_install_custom_name)
            .setEditText(FileTools.ensureValidFilename("$fileName${file.nameWithoutExtension}"))
            .setAsRequired()
            .setConfirmListener { editText, _ ->
                val string = editText.text.toString()
                if (FileTools.isFilenameInvalid(editText)) {
                    return@setConfirmListener false
                }

                val installFile = File(targetPath, "${string}.${file.extension}")

                if (installFile.exists()) {
                    // ✅ FIX: Cast to EditText to resolve the 'error' property
                    (editText as android.widget.EditText).error = context.getString(R.string.file_rename_exitis)
                    return@setConfirmListener false
                }

                val progressKey = installFile.absolutePath
                if (!taskRunning(progressKey)) {
                    install(installFile, progressKey)
                    ProgressLayout.setProgress(progressKey, 0, R.string.generic_waiting)
                }
                true
            }.showDialog()
    }

    abstract fun copy(): AbstractPlatformHelper
    abstract fun getWebUrl(infoItem: InfoItem): String?
    abstract fun getScreenshots(projectId: String): List<ScreenshotItem>

    abstract fun searchMod(filters: Filters, lastResult: SearchResult): SearchResult?
    abstract fun searchModPack(filters: Filters, lastResult: SearchResult): SearchResult?
    abstract fun searchResourcePack(filters: Filters, lastResult: SearchResult): SearchResult?
    abstract fun searchWorld(filters: Filters, lastResult: SearchResult): SearchResult?
    abstract fun searchShaderPack(filters: Filters, lastResult: SearchResult): SearchResult?

    abstract fun getModVersions(infoItem: InfoItem, force: Boolean): List<VersionItem>?
    abstract fun getModPackVersions(infoItem: InfoItem, force: Boolean): List<VersionItem>?
    abstract fun getResourcePackVersions(infoItem: InfoItem, force: Boolean): List<VersionItem>?
    abstract fun getWorldVersions(infoItem: InfoItem, force: Boolean): List<VersionItem>?
    abstract fun getShaderPackVersions(infoItem: InfoItem, force: Boolean): List<VersionItem>?

    abstract fun installMod(infoItem: InfoItem, version: VersionItem, targetPath: File, progressKey: String)
    abstract fun installModPack(version: VersionItem, customName: String): ModLoaderWrapper?
    abstract fun installResourcePack(infoItem: InfoItem, version: VersionItem, targetPath: File, progressKey: String)
    abstract fun installWorld(infoItem: InfoItem, version: VersionItem, targetPath: File, progressKey: String)
    abstract fun installShaderPack(infoItem: InfoItem, version: VersionItem, targetPath: File, progressKey: String)

    companion object {
        @JvmStatic
        fun getDir(): File {
            return VersionsManager.getCurrentVersion()?.getGameDir() ?: throw NoVersionException("There is no installed version")
        }

        @JvmStatic
        fun getModsPath() = File(getDir(), "/mods")

        @JvmStatic
        fun getResourcePackPath() = File(getDir(), "/resourcepacks")

        @JvmStatic
        fun getWorldPath() = File(getDir(), "/saves")

        @JvmStatic
        fun getShaderPackPath() = File(getDir(), "/shaderpacks")
    }
}
