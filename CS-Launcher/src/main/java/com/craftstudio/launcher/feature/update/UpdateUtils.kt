package com.craftstudio.launcher.feature.update

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.craftstudio.launcher.R
import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.feature.update.LauncherVersion.FileSize
import com.craftstudio.launcher.setting.AllSettings
import com.craftstudio.launcher.setting.AllSettings.Companion.ignoreUpdate
import com.craftstudio.launcher.task.TaskExecutors.Companion.runInUIThread
import com.craftstudio.launcher.ui.dialog.TipDialog
import com.craftstudio.launcher.ui.dialog.UpdateDialog
import com.craftstudio.launcher.utils.path.PathManager
import com.craftstudio.launcher.utils.ZHTools
import com.craftstudio.launcher.utils.http.CallUtils
import com.craftstudio.launcher.utils.http.CallUtils.CallbackListener
import com.craftstudio.launcher.utils.http.NetworkUtils
import com.craftstudio.launcher.utils.path.UrlManager
import com.craftstudio.launcher.utils.stringutils.StringUtils
import com.craftstudio.launcher.Architecture
import com.craftstudio.launcher.Tools
import okhttp3.Call
import okhttp3.Response
import org.apache.commons.io.FileUtils
import org.json.JSONObject
import java.io.File
import java.io.IOException

class UpdateUtils {
    companion object {
        @JvmField
        val sApkFile: File = File(PathManager.DIR_APP_CACHE, "cache.apk")
        private var LAST_UPDATE_CHECK_TIME: Long = 0

        /**
         * 启动软件的更新检测是5分钟的冷却，避免频繁检测导致Github限制访问
         * @param force 强制检测（用于设置内更新检测）
         */
        @JvmStatic
        fun checkDownloadedPackage(context: Context, force: Boolean, ignore: Boolean) {
            if (force && !NetworkUtils.isNetworkAvailable(context)) {
                Toast.makeText(context, context.getString(R.string.generic_no_network), Toast.LENGTH_SHORT).show()
                return
            }

            val isRelease = (ZHTools.isRelease() || ZHTools.isPreRelease()) && !ZHTools.isDebug()

            if (sApkFile.exists()) {
                val packageManager = context.packageManager
                val packageInfo = packageManager.getPackageArchiveInfo(sApkFile.absolutePath, 0)

                if (isRelease && packageInfo != null) {
                    val packageName = packageInfo.packageName
                    val versionCode = packageInfo.versionCode
                    val thisVersionCode = ZHTools.getVersionCode()

                    if (packageName == ZHTools.getPackageName() && versionCode > thisVersionCode) {
                        installApk(context, sApkFile)
                    } else {
                        FileUtils.deleteQuietly(sApkFile)
                    }
                } else {
                    FileUtils.deleteQuietly(sApkFile)
                }
            } else {
                if (isRelease && (force || checkCooling())) {
                    AllSettings.updateCheck.put(ZHTools.getCurrentTimeMillis()).save()
                    Logging.i("Check Update", "Checking new update!")

                    //如果安装包不存在，那么将自动获取更新
                    updateCheckerMainProgram(context, ignore)
                }
            }
        }

        private fun checkCooling(): Boolean {
            return ZHTools.getCurrentTimeMillis() - AllSettings.updateCheck.getValue() > 5 * 60 * 1000 //5分钟冷却
        }

        @Synchronized
        fun updateCheckerMainProgram(context: Context, ignore: Boolean) {
            if (ZHTools.getCurrentTimeMillis() - LAST_UPDATE_CHECK_TIME <= 5000) return
            LAST_UPDATE_CHECK_TIME = ZHTools.getCurrentTimeMillis()

            CallUtils(object : CallbackListener {
                override fun onFailure(call: Call?) {
                    showFailToast(context, context.getString(R.string.update_fail))
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call?, response: Response?) {
                    if (!response!!.isSuccessful) {
                        showFailToast(context, context.getString(R.string.update_fail_code, response.code))
                        Logging.e("UpdateLauncher", "Unexpected code " + response.code)
                    } else {
                        try {
                            val jsonObject = JSONObject(response.body!!.string())
                            val rawBase64 = jsonObject.getString("content")
                            val rawJson = StringUtils.decodeBase64(rawBase64)

                            val launcherVersion = Tools.GLOBAL_GSON.fromJson(rawJson, LauncherVersion::class.java)

                            val versionName = launcherVersion.versionName
                            if (ignore && versionName == ignoreUpdate.getValue()) return  //忽略此版本

                            val versionCode = launcherVersion.versionCode
                            fun checkPreRelease(): Boolean {
                                return if (!launcherVersion.isPreRelease) true
                                else ZHTools.isPreRelease() || AllSettings.acceptPreReleaseUpdates.getValue()
                            }
                            if (checkPreRelease() && ZHTools.getVersionCode() < versionCode) {
                                runInUIThread {
                                    UpdateDialog(context, launcherVersion).show()
                                }
                            } else if (!ignore) {
                                runInUIThread {
                                    val nowVersionName = ZHTools.getVersionName()
                                    runInUIThread {
                                        Toast.makeText(
                                            context,
                                            StringUtils.insertSpace(context.getString(R.string.update_without), nowVersionName),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Logging.e("Check Update", Tools.printToString(e))
                        }
                    }
                }
            }, "${UrlManager.URL_GITHUB_HOME}launcher_version.json", null).enqueue()
        }

        @JvmStatic
        fun showFailToast(context: Context, resString: String) {
            runInUIThread {
                Toast.makeText(context, resString, Toast.LENGTH_SHORT).show()
            }
        }

        @JvmStatic
        fun getArchModel(arch: Int = Tools.DEVICE_ARCHITECTURE): String? {
            if (arch == Architecture.ARCH_ARM64) return "arm64-v8a"
            if (arch == Architecture.ARCH_ARM) return "armeabi-v7a"
            if (arch == Architecture.ARCH_X86_64) return "x86_64"
            if (arch == Architecture.ARCH_X86) return "x86"
            return null
        }

        @JvmStatic
        fun getFileSize(fileSize: FileSize): Long {
            val arch = Tools.DEVICE_ARCHITECTURE
            if (arch == Architecture.ARCH_ARM64) return fileSize.arm64
            if (arch == Architecture.ARCH_ARM) return fileSize.arm
            if (arch == Architecture.ARCH_X86_64) return fileSize.x86_64
            if (arch == Architecture.ARCH_X86) return fileSize.x86
            return fileSize.all
        }

        @JvmStatic
        fun getDownloadUrl(launcherVersion: LauncherVersion): String {
            val archModel = getArchModel()
            return "https://github.com/CS-Launcher/CS-Launcher/releases/download/" +
                    "${launcherVersion.versionCode}/CS-Launcher-${launcherVersion.versionName}" +
                    "${(if (archModel != null) String.format("-%s", archModel) else "")}.apk"
        }

        @JvmStatic
        fun installApk(context: Context, outputFile: File) {
            runInUIThread {
                TipDialog.Builder(context)
                    .setTitle(R.string.update)
                    .setMessage(StringUtils.insertNewline(context.getString(R.string.update_success), outputFile.absolutePath))
                    .setCenterMessage(false)
                    .setCancelable(false)
                    .setConfirmClickListener {
                        //安装
                        val intent = Intent(Intent.ACTION_VIEW)
                        val apkUri = FileProvider.getUriForFile(context, context.packageName + ".provider", outputFile)
                        intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        context.startActivity(intent)
                    }.showDialog()
            }
        }
    }
}