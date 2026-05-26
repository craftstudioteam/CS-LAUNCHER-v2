package com.craftstudio.launcher.launch

import android.app.Activity
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kdt.mcgui.ProgressLayout
import com.craftstudio.launcher.R
import com.craftstudio.launcher.event.single.AccountUpdateEvent
import com.craftstudio.launcher.utils.OptionsOptimizer
import com.craftstudio.launcher.feature.accounts.AccountType
import com.craftstudio.launcher.feature.accounts.AccountUtils
import com.craftstudio.launcher.feature.accounts.AccountsManager
import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.feature.version.Version
import com.craftstudio.launcher.renderer.Renderers
import com.craftstudio.launcher.setting.AllSettings
import com.craftstudio.launcher.setting.AllStaticSettings
import com.craftstudio.launcher.support.touch_controller.ControllerProxy
import com.craftstudio.launcher.task.TaskExecutors
import com.craftstudio.launcher.ui.dialog.LifecycleAwareTipDialog
import com.craftstudio.launcher.ui.dialog.TipDialog
import com.craftstudio.launcher.utils.ZHTools
import com.craftstudio.launcher.utils.http.NetworkUtils
import com.craftstudio.launcher.utils.stringutils.StringUtils
import com.craftstudio.launcher.Architecture
import com.craftstudio.launcher.JMinecraftVersionList
import com.craftstudio.launcher.Logger
import com.craftstudio.launcher.Tools
import com.craftstudio.launcher.utils.OfflineUuidUtils
import com.craftstudio.launcher.authenticator.microsoft.PresentedException
import com.craftstudio.launcher.lifecycle.ContextAwareDoneListener
import com.craftstudio.launcher.multirt.MultiRTUtils
import com.craftstudio.launcher.plugins.FFmpegPlugin
import com.craftstudio.launcher.progresskeeper.ProgressKeeper
import com.craftstudio.launcher.services.GameService
import com.craftstudio.launcher.tasks.AsyncMinecraftDownloader
import com.craftstudio.launcher.tasks.MinecraftDownloader
import com.craftstudio.launcher.utils.JREUtils
import com.craftstudio.launcher.value.MinecraftAccount
import org.greenrobot.eventbus.EventBus

class LaunchGame {
    companion object {
        /**
         * 改为启动游戏前进行的操作
         * - 进行登录，同时也能及时的刷新账号的信息（这明显更合理不是吗，PojavLauncher？）
         * - 复制 options.txt 文件到游戏目录
         * @param version 选择的版本
         */
        @JvmStatic
        fun preLaunch(context: Context, version: Version) {
            val networkAvailable = NetworkUtils.isNetworkAvailable(context)

            fun launch(setOfflineAccount: Boolean = false) {
                version.offlineAccountLogin = setOfflineAccount

                val versionName = version.getVersionName()
                val mcVersion = AsyncMinecraftDownloader.getListedVersion(versionName)
                val listener = ContextAwareDoneListener(context, version)
                //若网络未连接，跳过下载任务直接启动
                if (!networkAvailable) {
                    listener.onDownloadDone()
                } else {
                    MinecraftDownloader().start(mcVersion, versionName, listener)
                }
            }

            fun setGameProgress(pull: Boolean) {
                if (pull) {
                    ProgressKeeper.submitProgress(ProgressLayout.CHECKING_MODS, 0, R.string.mod_check_progress_message, 0, 0, 0)
                    ProgressKeeper.submitProgress(ProgressLayout.DOWNLOAD_MINECRAFT, 0, R.string.newdl_downloading_game_files, 0, 0, 0)
                } else {
                    ProgressLayout.clearProgress(ProgressLayout.DOWNLOAD_MINECRAFT)
                    ProgressLayout.clearProgress(ProgressLayout.CHECKING_MODS)
                }
            }

            if (!networkAvailable) {
                // 网络未链接，无法登录，但是依旧允许玩家启动游戏 (临时创建一个同名的离线账号启动游戏)
                Toast.makeText(context, context.getString(R.string.account_login_no_network), Toast.LENGTH_SHORT).show()
                launch(true)
                return
            }

            if (AccountUtils.isNoLoginRequired(AccountsManager.currentAccount)) {
                launch()
                return
            }

            AccountsManager.performLogin(
                context, AccountsManager.currentAccount!!,
                { _ ->
                    EventBus.getDefault().post(AccountUpdateEvent())
                    TaskExecutors.runInUIThread {
                        Toast.makeText(context, context.getString(R.string.account_login_done), Toast.LENGTH_SHORT).show()
                    }
                    //登录完成，正式启动游戏！
                    launch()
                },
                { exception ->
                    val errorMessage = if (exception is PresentedException) exception.toString(context)
                    else exception.message

                    TaskExecutors.runInUIThread {
                        TipDialog.Builder(context)
                            .setTitle(R.string.generic_error)
                            .setMessage("${context.getString(R.string.account_login_skip)}\r\n$errorMessage")
                            .setWarning()
                            .setConfirmClickListener { launch(true) }
                            .setCenterMessage(false)
                            .showDialog()
                    }

                    setGameProgress(false)
                }
            )
            setGameProgress(true)
        }

        @Throws(Throwable::class)
        @JvmStatic
        fun runGame(activity: AppCompatActivity, minecraftVersion: Version, version: JMinecraftVersionList.Version) {
            if (!Renderers.isCurrentRendererValid()) {
                Renderers.setCurrentRenderer(activity, AllSettings.renderer.getValue())
            }

            var account = AccountsManager.currentAccount!!
            if (minecraftVersion.offlineAccountLogin) {
                account = MinecraftAccount().apply {
                    this.username = account.username
                    this.accountType = AccountType.LOCAL.type
                    this.profileId = OfflineUuidUtils.fromUsername(this.username)
                    this.accessToken = "0"
                    this.clientToken = "0"
                }
            }
            if (AccountUtils.isNoLoginRequired(account)) {
                account.profileId = OfflineUuidUtils.fromUsername(account.username)
                account.accessToken = "0"
            }

            val customArgs = minecraftVersion.getJavaArgs().takeIf { it.isNotBlank() } ?: ""

            val javaRuntime = getRuntime(activity, minecraftVersion, version.javaVersion?.majorVersion ?: 8)

            printLauncherInfo(
                minecraftVersion,
                customArgs.takeIf { it.isNotBlank() } ?: "NONE",
                javaRuntime,
                account
            )

            minecraftVersion.modCheckResult?.let { modCheckResult ->
                if (modCheckResult.hasTouchController) {
                    Logger.appendToLog("Mod Perception: TouchController Mod found, attempting to automatically enable control proxy!")
                    ControllerProxy.startProxy(activity)
                    AllStaticSettings.useControllerProxy = true
                }

                if (modCheckResult.hasSodiumOrEmbeddium) {
                    Logger.appendToLog("Mod Perception: Sodium or Embeddium Mod found, attempting to load the disable warning tool later!")
                }
            }

            JREUtils.redirectAndPrintJRELog()

            // Localhost skin rendering removed — no authlib args, no CSL injection
            val authlibArgs = emptyList<String>()

            launch(activity, account, minecraftVersion, javaRuntime, customArgs, authlibArgs)

            GameService.setActive(false)
        }

        private fun getRuntime(activity: Activity, version: Version, targetJavaVersion: Int): String {
            val versionRuntime = version.getJavaDir()
                .takeIf { it.isNotEmpty() && it.startsWith(Tools.LAUNCHERPROFILES_RTPREFIX) }
                ?.removePrefix(Tools.LAUNCHERPROFILES_RTPREFIX)
                ?: ""

            if (versionRuntime.isNotEmpty()) return versionRuntime

            //如果版本未选择Java环境，则自动选择合适的环境
            var runtime = AllSettings.defaultRuntime.getValue()
            val pickedRuntime = MultiRTUtils.read(runtime)
            if (pickedRuntime.javaVersion == 0 || pickedRuntime.javaVersion < targetJavaVersion) {
                runtime = MultiRTUtils.getNearestJreName(targetJavaVersion) ?: run {
                    activity.runOnUiThread {
                        Toast.makeText(activity, activity.getString(R.string.game_autopick_runtime_failed), Toast.LENGTH_SHORT).show()
                    }
                    return runtime
                }
            }
            return runtime
        }

        private fun printLauncherInfo(
            minecraftVersion: Version,
            javaArguments: String,
            javaRuntime: String,
            account: MinecraftAccount
        ) {
            var mcInfo = minecraftVersion.getVersionName()
            minecraftVersion.getVersionInfo()?.let { info ->
                mcInfo = info.getInfoString()
            }

            Logger.appendToLog("--------- Start launching the game")
            Logger.appendToLog("Info: Launcher version: ${ZHTools.getVersionName()} (${ZHTools.getVersionCode()})")
            Logger.appendToLog("Info: Architecture: ${Architecture.archAsString(Tools.DEVICE_ARCHITECTURE)}")
            Logger.appendToLog("Info: Device model: ${StringUtils.insertSpace(Build.MANUFACTURER, Build.MODEL)}")
            Logger.appendToLog("Info: API version: ${Build.VERSION.SDK_INT}")
            Logger.appendToLog("Info: Renderer: ${Renderers.getCurrentRenderer().getRendererName()}")
            Logger.appendToLog("Info: Selected Minecraft version: ${minecraftVersion.getVersionName()}")
            Logger.appendToLog("Info: Minecraft Info: $mcInfo")
            Logger.appendToLog("Info: Game Path: ${minecraftVersion.getGameDir().absolutePath} (Isolation: ${minecraftVersion.isIsolation()})")
            Logger.appendToLog("Info: Custom Java arguments: $javaArguments")
            Logger.appendToLog("Info: Java Runtime: $javaRuntime")
            Logger.appendToLog("Info: Account: ${account.username} (${account.accountType})")
            Logger.appendToLog("---------\r\n")
        }

        @Throws(Throwable::class)
        @JvmStatic
        private fun launch(
            activity: AppCompatActivity,
            account: MinecraftAccount,
            minecraftVersion: Version,
            javaRuntime: String,
            customArgs: String,
            authlibArgs: List<String> = emptyList()
        ) {
            checkMemory(activity)

            val runtime = MultiRTUtils.forceReread(javaRuntime)

            val versionInfo = Tools.getVersionInfo(minecraftVersion)
            val gameDirPath = minecraftVersion.getGameDir()

            //预处理
            Tools.disableSplash(gameDirPath)
            OptionsOptimizer.forceMaxFPS(gameDirPath)
            val launchClassPath = Tools.generateLaunchClassPath(versionInfo, minecraftVersion)

            val launchArgs = LaunchArgs(
                account,
                gameDirPath,
                minecraftVersion,
                versionInfo,
                minecraftVersion.getVersionName(),
                runtime,
                launchClassPath,
                authlibArgs
            ).getAllArgs()

            FFmpegPlugin.discover(activity)

            // FIX: Removed account.clientToken from here, now passing exactly 5 arguments.
            JREUtils.launchWithUtils(activity, runtime, minecraftVersion, launchArgs, customArgs)
        }

        private fun checkMemory(activity: AppCompatActivity) {
            var freeDeviceMemory = Tools.getFreeDeviceMemory(activity)
            val freeAddressSpace =
                if (Architecture.is32BitsDevice())
                    Tools.getMaxContinuousAddressSpaceSize()
                else -1
            Logging.i("MemStat",
                "Free RAM: $freeDeviceMemory Addressable: $freeAddressSpace")

            val stringId: Int = if (freeDeviceMemory > freeAddressSpace && freeAddressSpace != -1) {
                freeDeviceMemory = freeAddressSpace
                R.string.address_memory_warning_msg
            } else R.string.memory_warning_msg

            if (AllSettings.ramAllocation.value.getValue() > freeDeviceMemory) {
                val builder = TipDialog.Builder(activity)
                    .setTitle(R.string.generic_warning)
                    .setMessage(activity.getString(stringId, freeDeviceMemory, AllSettings.ramAllocation.value.getValue()))
                    .setWarning()
                    .setCenterMessage(false)
                    .setShowCancel(false)
                if (LifecycleAwareTipDialog.haltOnDialog(activity.lifecycle, builder)) return
                // If the dialog's lifecycle has ended, return without
                // actually launching the game, thus giving us the opportunity
                // to start after the activity is shown again
            }
        }
    }
}
