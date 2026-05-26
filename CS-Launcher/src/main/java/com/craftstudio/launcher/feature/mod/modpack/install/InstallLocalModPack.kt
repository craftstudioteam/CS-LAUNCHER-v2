package com.craftstudio.launcher.feature.mod.modpack.install

import android.content.Context
import com.craftstudio.launcher.R
import com.craftstudio.launcher.feature.download.item.ModLoaderWrapper
import com.craftstudio.launcher.feature.download.platform.curseforge.CurseForgeModPackInstallHelper
import com.craftstudio.launcher.feature.download.platform.modrinth.ModrinthModPackInstallHelper
import com.craftstudio.launcher.feature.download.utils.PlatformUtils
import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.feature.mod.models.MCBBSPackMeta
import com.craftstudio.launcher.feature.mod.modpack.MCBBSModPack
import com.craftstudio.launcher.feature.mod.modpack.install.ModPackUtils.ModPackEnum
import com.craftstudio.launcher.feature.version.VersionConfig
import com.craftstudio.launcher.feature.version.VersionsManager
import com.craftstudio.launcher.task.TaskExecutors
import com.craftstudio.launcher.ui.dialog.TipDialog
import com.craftstudio.launcher.utils.stringutils.StringUtils
import com.craftstudio.launcher.Tools
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.zip.ZipFile

class InstallLocalModPack {
    companion object {
        @JvmStatic
        @Throws(Exception::class)
        fun installModPack(
            context: Context,
            type: ModPackEnum?,
            zipFile: File,
            customVersionName: String
        ): ModLoaderWrapper? {
            try {
                runCatching {
                    ZipFile(zipFile)
                }.getOrElse {
                    Logging.e("Install local ModPack", "This file doesn't seem to be a proper archive", it)
                    TaskExecutors.runInUIThread {
                        showUnSupportDialog(context)
                    }
                    return null
                }.use { modpackZipFile ->
                    val modLoader: ModLoaderWrapper?
                    val versionPath = VersionsManager.getVersionPath(customVersionName)

                    when (type) {
                        ModPackEnum.CURSEFORGE -> {
                            modLoader = curseforgeModPack(zipFile, versionPath) ?: return null
                            VersionConfig.createIsolation(versionPath).save()

                            return modLoader
                        }

                        ModPackEnum.MCBBS -> {
                            val mcbbsEntry = modpackZipFile.getEntry("mcbbs.packmeta")

                            val mcbbsPackMeta = Tools.GLOBAL_GSON.fromJson(
                                Tools.read(
                                    modpackZipFile.getInputStream(mcbbsEntry)
                                ), MCBBSPackMeta::class.java
                            )

                            modLoader = mcbbsModPack(context, zipFile, versionPath) ?: return null
                            VersionConfig.createIsolation(versionPath).apply {
                                setJavaArgs(StringUtils.insertSpace(null, *mcbbsPackMeta.launchInfo.javaArgument))
                            }.save()

                            return modLoader
                        }

                        ModPackEnum.MODRINTH -> {
                            modLoader = modrinthModPack(zipFile, versionPath) ?: return null
                            VersionConfig.createIsolation(versionPath).save()

                            return modLoader
                        }

                        else -> {
                            TaskExecutors.runInUIThread {
                                showUnSupportDialog(context)
                            }
                            return null
                        }
                    }
                }
            } finally {
                FileUtils.deleteQuietly(zipFile) // 删除文件（虽然文件通常来说并不会很大）
            }
        }

        @JvmStatic
        fun showUnSupportDialog(context: Context) {
            TipDialog.Builder(context)
                .setTitle(R.string.generic_warning)
                .setMessage(R.string.select_modpack_local_not_supported) //弹窗提醒
                .setWarning()
                .setShowCancel(true)
                .setShowConfirm(false)
                .showDialog()
        }

        @Throws(Exception::class)
        private fun curseforgeModPack(
            zipFile: File,
            versionPath: File
        ): ModLoaderWrapper? {
            return CurseForgeModPackInstallHelper.installZip(
                PlatformUtils.createCurseForgeApi(),
                zipFile,
                versionPath
            )
        }

        @Throws(Exception::class)
        private fun modrinthModPack(
            zipFile: File,
            versionPath: File
        ): ModLoaderWrapper? {
            return ModrinthModPackInstallHelper.installZip(
                zipFile,
                versionPath
            )
        }

        @Throws(Exception::class)
        private fun mcbbsModPack(context: Context, zipFile: File, versionPath: File): ModLoaderWrapper? {
            val mcbbsModPack = MCBBSModPack(context, zipFile)
            return mcbbsModPack.install(versionPath)
        }
    }
}
