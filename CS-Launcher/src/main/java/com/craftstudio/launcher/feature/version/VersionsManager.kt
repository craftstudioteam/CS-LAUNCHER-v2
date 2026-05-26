package com.craftstudio.launcher.feature.version

import android.content.Context
import com.craftstudio.launcher.InfoDistributor
import com.craftstudio.launcher.R
import com.craftstudio.launcher.event.single.RefreshVersionsEvent
import com.craftstudio.launcher.event.single.RefreshVersionsEvent.MODE.END
import com.craftstudio.launcher.event.single.RefreshVersionsEvent.MODE.START
import com.craftstudio.launcher.feature.customprofilepath.ProfilePathHome
import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.feature.version.favorites.FavoritesVersionUtils
import com.craftstudio.launcher.feature.version.utils.VersionInfoUtils
import com.craftstudio.launcher.task.Task
import com.craftstudio.launcher.task.TaskExecutors
import com.craftstudio.launcher.ui.dialog.EditTextDialog
import com.craftstudio.launcher.utils.ZHTools
import com.craftstudio.launcher.utils.file.FileTools
import com.craftstudio.launcher.utils.stringutils.SortStrings
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.craftstudio.launcher.Tools
import org.apache.commons.io.FileUtils
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 所有版本管理者
 * @see Version
 */
object VersionsManager {
    private val versions = CopyOnWriteArrayList<Version>()

    /**
     * @return 获取当前的游戏信息
     */
    lateinit var currentGameInfo: CurrentGameInfo
        private set

    private val coroutineScope = CoroutineScope(Dispatchers.IO + CoroutineName("VersionsManager"))
    private val refreshMutex = Mutex()
    private var isRefreshing: Boolean = false
    private var lastRefreshTime = 0L

    /**
     * @return 检查是否可以刷新
     */
    @JvmStatic
    fun canRefresh() = !isRefreshing && ZHTools.getCurrentTimeMillis() - lastRefreshTime > 500

    /**
     * @return 全部的版本数据
     */
    fun getVersions() = versions.toList()

    /**
     * 检查版本是否已经存在
     */
    fun isVersionExists(versionName: String, checkJson: Boolean = false): Boolean {
        val folder = File(ProfilePathHome.getVersionsHome(), versionName)
        //保证版本文件夹存在的同时，也应保证其版本json文件存在
        return if (checkJson) File(folder, "${folder.name}.json").exists()
        else folder.exists()
    }

    /**
     * 异步刷新当前的版本列表，刷新完成后，将使用一个事件进行通知，不过这个事件并不会在UI线程执行
     * @param tag 标记是谁发起了版本刷新任务，方便debug
     * @see com.craftstudio.launcher.event.single.RefreshVersionsEvent
     */
    fun refresh(tag: String, refreshVersionInfo: Boolean = false) {
        Logging.i("VersionsManager", "$tag initiated the refresh version task")
        coroutineScope.launch {
            refreshMutex.withLock {
                lastRefreshTime = ZHTools.getCurrentTimeMillis()
                handleRefreshOperation(refreshVersionInfo)
            }
        }
    }

    private fun handleRefreshOperation(refreshVersionInfo: Boolean) {
        isRefreshing = true
        EventBus.getDefault().post(RefreshVersionsEvent(START))

        versions.clear()

        val versionsHome: String = ProfilePathHome.getVersionsHome()
        File(versionsHome).listFiles()?.forEach { versionFile ->
            runCatching {
                processVersionFile(versionsHome, versionFile, refreshVersionInfo)
            }
        }

        versions.sortWith { o1, o2 ->
            var sort = -SortStrings.compareClassVersions(
                o1.getVersionInfo()?.minecraftVersion ?: o1.getVersionName(),
                o2.getVersionInfo()?.minecraftVersion ?: o2.getVersionName()
            )
            if (sort == 0) sort = SortStrings.compareChar(o1.getVersionName(), o2.getVersionName())
            sort
        }

        currentGameInfo = CurrentGameInfo.refreshCurrentInfo()

        //使用事件通知版本已刷新
        EventBus.getDefault().post(RefreshVersionsEvent(END))
        isRefreshing = false
    }

    private fun processVersionFile(versionsHome: String, versionFile: File, refreshVersionInfo: Boolean) {
        if (versionFile.exists() && versionFile.isDirectory) {
            var isVersion = false

            //通过判断是否存在版本的.json文件，来确定其是否为一个版本
            val jsonFile = File(versionFile, "${versionFile.name}.json")
            if (jsonFile.exists() && jsonFile.isFile) {
                isVersion = true
                val versionInfoFile = File(getCSVersionPath(versionFile), "VersionInfo.json")
                if (refreshVersionInfo) FileUtils.deleteQuietly(versionInfoFile)
                if (!versionInfoFile.exists()) {
                    VersionInfoUtils.parseJson(jsonFile)?.save(versionFile)
                }
            }

            val csVersionPath = getCSVersionPath(versionFile)
            if (!csVersionPath.exists()) csVersionPath.mkdirs()
            val versionConfigFile = File(csVersionPath, "VersionConfig.json")
            if (!versionConfigFile.exists()) {
                VersionConfig(versionFile).save()
            }

            val versionConfig = VersionConfig.parseConfig(versionFile)

            val version = Version(
                versionsHome,
                versionFile.absolutePath,
                versionConfig,
                isVersion
            )
            versions.add(version)

            Logging.i("VersionsManager", "Identified and added version: ${version.getVersionName()}, " +
                    "Path: (${version.getVersionPath()}), " +
                    "Info: ${version.getVersionInfo()?.getInfoString()}")
        }
    }

    /**
     * @return 获取当前的版本
     */
    fun getCurrentVersion(): Version? {
        if (versions.isEmpty()) return null

        fun returnVersionByFirst(): Version? {
            return versions.find { it.isValid() }?.apply {
                //确保版本有效
                saveCurrentVersion(getVersionName())
            }
        }

        return runCatching {
            val versionString = currentGameInfo.version
            getVersion(versionString) ?: run {
                return returnVersionByFirst()
            }
        }.getOrElse { e ->
            Logging.e("Get Current Version", Tools.printToString(e))
            returnVersionByFirst()
        }
    }

    /**
     * @return 通过版本名，判断其版本是否存在
     */
    fun checkVersionExistsByName(versionName: String?) =
        versionName?.let { name -> versions.any { it.getVersionName() == name } } ?: false

    /**
     * @return 获取 CS 启动器版本标识文件夹
     */
    fun getCSVersionPath(version: Version) = File(version.getVersionPath(), InfoDistributor.LAUNCHER_NAME)

    /**
     * @return 通过目录获取 CS 启动器版本标识文件夹
     */
    fun getCSVersionPath(folder: File) = File(folder, InfoDistributor.LAUNCHER_NAME)

    /**
     * @return 通过名称获取 CS 启动器版本标识文件夹
     */
    fun getCSVersionPath(name: String) = File(getVersionPath(name), InfoDistributor.LAUNCHER_NAME)

    /**
     * @return 获取当前版本设置的图标
     */
    fun getVersionIconFile(version: Version) = File(getCSVersionPath(version), "VersionIcon.png")

    /**
     * @return 通过名称获取当前版本设置的图标
     */
    fun getVersionIconFile(name: String) = File(getCSVersionPath(name), "VersionIcon.png")

    /**
     * @return 通过名称获取版本的文件夹路径
     */
    fun getVersionPath(name: String) = File(ProfilePathHome.getVersionsHome(), name)

    /**
     * 保存当前选择的版本
     */
    fun saveCurrentVersion(versionName: String) {
        runCatching {
            currentGameInfo.apply {
                version = versionName
                saveCurrentInfo()
            }
        }.onFailure { e -> Logging.e("Save Current Version", Tools.printToString(e)) }
    }

    private fun validateVersionName(
        context: Context,
        newName: String,
        versionInfo: VersionInfo?
    ): String? {
        return when {
            isVersionExists(newName, true) ->
                context.getString(R.string.version_install_exists)
            versionInfo?.loaderInfo?.takeIf { it.isNotEmpty() }?.let {
                //如果这个版本是有ModLoader加载器信息的，则不允许修改为与原版名称一致的名称，防止冲突
                newName == versionInfo.minecraftVersion
            } ?: false ->
                context.getString(R.string.version_install_cannot_use_mc_name)
            else -> null
        }
    }

    /**
     * 打开重命名版本的弹窗，需要确保在UI线程运行
     * @param beforeRename 在重命名前一步的操作
     */
    fun openRenameDialog(context: Context, version: Version, beforeRename: (() -> Unit)? = null) {
        EditTextDialog.Builder(context)
            .setTitle(R.string.version_manager_rename)
            .setEditText(version.getVersionName())
            .setAsRequired()
            .setConfirmListener { editText, _ ->
                val string = editText.text.toString()

                //与原始名称一致
                if (string == version.getVersionName()) return@setConfirmListener true

                if (FileTools.isFilenameInvalid(editText)) {
                    return@setConfirmListener false
                }

                val error = validateVersionName(context, string, version.getVersionInfo())
                error?.let {
                    // ✅ 修复：显式转换为 EditText
                    (editText as android.widget.EditText).error = it
                    return@setConfirmListener false
                }

                beforeRename?.invoke()
                renameVersion(version, string)

                true
            }.showDialog()
    }

    /**
     * 重命名当前版本，但并不会在这里对即将重命名的名称，进行非法性判断
     */
    private fun renameVersion(version: Version, name: String) {
        val currentVersionName = getCurrentVersion()?.getVersionName()
        //如果当前的版本是即将被重命名的版本，那么就把将要重命名的名字设置为当前版本
        if (version.getVersionName() == currentVersionName) saveCurrentVersion(name)

        //尝试刷新收藏夹内的版本名称
        FavoritesVersionUtils.renameVersion(version.getVersionName(), name)

        val versionFolder = version.getVersionPath()
        val renameFolder = File(ProfilePathHome.getVersionsHome(), name)

        //不管重命名之后的文件夹是什么，只要这个文件夹存在，那么就必须删除
        //否则将出现问题
        FileUtils.deleteQuietly(renameFolder)

        val originalName = versionFolder.name

        FileTools.renameFile(versionFolder, renameFolder)

        val versionJsonFile = File(renameFolder, "$originalName.json")
        val versionJarFile = File(renameFolder, "$originalName.jar")
        val renameJsonFile = File(renameFolder, "$name.json")
        val renameJarFile = File(renameFolder, "$name.jar")

        FileTools.renameFile(versionJsonFile, renameJsonFile)
        FileTools.renameFile(versionJarFile, renameJarFile)

        FileUtils.deleteQuietly(versionFolder)

        //重命名后，需要刷新列表
        refresh("VersionsManager:renameVersion")
    }

    /**
     * 打开复制版本的名称输入框，将选中的版本复制为一个新的版本
     */
    fun openCopyDialog(context: Context, version: Version) {
        val dialog = ZHTools.createTaskRunningDialog(context)
        EditTextDialog.Builder(context)
            .setTitle(R.string.version_manager_copy)
            .setMessage(R.string.version_manager_copy_tip)
            .setCheckBoxText(R.string.version_manager_copy_all)
            .setShowCheckBox(true)
            .setEditText(version.getVersionName())
            .setAsRequired()
            .setConfirmListener { editText, checked ->
                val string = editText.text.toString()

                //与原始名称一致
                if (string == version.getVersionName()) return@setConfirmListener true

                if (FileTools.isFilenameInvalid(editText)) {
                    return@setConfirmListener false
                }

                val error = validateVersionName(context, string, version.getVersionInfo())
                error?.let {
                    // ✅ 修复：显式转换为 EditText
                    (editText as android.widget.EditText).error = it
                    return@setConfirmListener false
                }

                Task.runTask {
                    copyVersion(version, string, checked)
                }.beforeStart(TaskExecutors.getAndroidUI()) {
                    dialog.show()
                }.onThrowable { e ->
                    Tools.showErrorRemote(e)
                }.finallyTask(TaskExecutors.getAndroidUI()) {
                    dialog.dismiss()
                    refresh("VersionsManager:openCopyDialog")
                }.execute()
                true
            }.showDialog()
    }

    /**
     * 将选中的版本复制为一个新的版本
     * @param version 选中的版本
     * @param name 新的版本的名称
     * @param copyAllFile 是否复制全部文件
     */
    private fun copyVersion(version: Version, name: String, copyAllFile: Boolean) {
        val versionsFolder = version.getVersionsFolder()
        val newVersion = File(versionsFolder, name)

        val originalName = version.getVersionName()

        //新版本的json与jar文件
        val newJsonFile = File(newVersion, "$name.json")
        val newJarFile = File(newVersion, "$name.jar")

        val originalVersionFolder = version.getVersionPath()
        if (copyAllFile) {
            //启用复制所有文件时，直接将原文件夹整体复制到新版本
            FileUtils.copyDirectory(originalVersionFolder, newVersion)
            //重命名json、jar文件
            val jsonFile = File(newVersion, "$originalName.json")
            val jarFile = File(newVersion, "$originalName.jar")
            if (jsonFile.exists()) jsonFile.renameTo(newJsonFile)
            if (jarFile.exists()) jarFile.renameTo(newJarFile)
        } else {
            //不复制所有文件时，仅复制并重命名json、jar文件
            val originalJsonFile = File(originalVersionFolder, "$originalName.json")
            val originalJarFile = File(originalVersionFolder, "$originalName.jar")
            newVersion.mkdirs()
            // versions/1.21.3/1.21.3.json -> versions/name/name.json
            if (originalJsonFile.exists()) originalJsonFile.copyTo(newJsonFile)
            // versions/1.21.3/1.21.3.jar -> versions/name/name.jar
            if (originalJarFile.exists()) originalJarFile.copyTo(newJarFile)
        }

        //保存版本配置文件
        version.getVersionConfig().copy().let { config ->
            config.setVersionPath(newVersion)
            config.setIsolationType(VersionConfig.IsolationType.ENABLE)
            config.saveWithThrowable()
        }
    }

    private fun getVersion(name: String?): Version? {
        name?.let { versionName ->
            return versions.find { it.getVersionName() == versionName }?.takeIf { it.isValid() }
        }
        return null
    }
}
