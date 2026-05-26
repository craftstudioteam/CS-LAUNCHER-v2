package com.craftstudio.launcher.feature.download.platform.curseforge

import com.kdt.mcgui.ProgressLayout
import com.craftstudio.launcher.R
import com.craftstudio.launcher.feature.download.enums.ModLoader
import com.craftstudio.launcher.feature.download.install.InstallHelper
import com.craftstudio.launcher.feature.download.item.ModLoaderWrapper
import com.craftstudio.launcher.feature.download.item.VersionItem
import com.craftstudio.launcher.feature.download.platform.curseforge.CurseForgeCommonUtils.Companion.getDownloadSha1
import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.feature.mod.modpack.install.ModPackUtils
import com.craftstudio.launcher.utils.ZHTools
import com.craftstudio.launcher.utils.file.FileTools
import com.craftstudio.launcher.utils.stringutils.StringUtils
import com.craftstudio.launcher.Tools
import com.craftstudio.launcher.modloaders.modpacks.api.ApiHandler
import com.craftstudio.launcher.modloaders.modpacks.api.ModDownloader
import com.craftstudio.launcher.modloaders.modpacks.models.CurseManifest
import com.craftstudio.launcher.modloaders.modpacks.models.CurseManifest.CurseMinecraft
import com.craftstudio.launcher.modloaders.modpacks.models.CurseManifest.CurseModLoader
import com.craftstudio.launcher.progresskeeper.ProgressKeeper
import com.craftstudio.launcher.tasks.SpeedCalculator
import com.craftstudio.launcher.utils.FileUtils
import com.craftstudio.launcher.utils.ZipUtils
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile
import kotlin.math.max

class CurseForgeModPackInstallHelper {
    companion object {
        @Throws(Exception::class)
        fun startInstall(api: ApiHandler, versionItem: VersionItem, customName: String): ModLoaderWrapper? {
            return InstallHelper.installModPack(versionItem, customName) { modpackFile, targetPath ->
                installZip(api, modpackFile, targetPath)
            }
        }

        @Throws(Exception::class)
        fun installZip(api: ApiHandler, zipFile: File, targetPath: File): ModLoaderWrapper? {
            ZipFile(zipFile).use { modpackZipFile ->
                val curseManifest = Tools.GLOBAL_GSON.fromJson(
                    Tools.read(ZipUtils.getEntryStream(modpackZipFile, "manifest.json")),
                    CurseManifest::class.java
                )
                if (!ModPackUtils.verifyManifest(curseManifest)) {
                    Logging.i("CurseForgeModPackInstallHelper", "manifest verification failed")
                    return null
                }
                var progressUpdateTime = 0L
                val speedCalculator = SpeedCalculator()
                val modDownloader: ModDownloader = getModDownloader(api, targetPath, curseManifest)
                modDownloader.awaitFinish { count: Int, totalCount: Int, downloadedSize: Long ->
                    val currentTime = ZHTools.getCurrentTimeMillis()
                    if (currentTime - progressUpdateTime < 150) return@awaitFinish
                    progressUpdateTime = currentTime

                    ProgressKeeper.submitProgress(
                        ProgressLayout.INSTALL_RESOURCE,
                        max((count.toFloat() / totalCount * 100).toDouble(), 0.0).toInt(),
                        R.string.modpack_download_downloading_mods_fc,
                        count,
                        FileTools.formatFileSize(downloadedSize),
                        totalCount,
                        FileTools.formatFileSize(speedCalculator.feed(downloadedSize))
                    )
                }
                val overridesDir: String = curseManifest.overrides ?: "overrides"
                ZipUtils.zipExtract(modpackZipFile, overridesDir, targetPath)
                return createInfo(curseManifest.minecraft)
            }
        }

        @Throws(Exception::class)
        private fun getModDownloader(
            api: ApiHandler,
            instanceDestination: File,
            curseManifest: CurseManifest
        ): ModDownloader {
            val modDownloader = ModDownloader(File(instanceDestination, "mods"), true)
            val fileCount = curseManifest.files.size
            for (i in 0 until fileCount) {
                val curseFile = curseManifest.files[i]
                modDownloader.submitDownload {
                    val url = CurseForgeCommonUtils.getDownloadUrl(api, curseFile.projectID, curseFile.fileID)
                    if (url == null && curseFile.required) throw IOException(
                        "Failed to obtain download URL for ${StringUtils.insertSpace(curseFile.projectID, curseFile.fileID)}"
                    )
                    else if (url == null) return@submitDownload null
                    ModDownloader.FileInfo(url, FileUtils.getFileName(url), getDownloadSha1(api, curseFile.projectID, curseFile.fileID))
                }
            }
            return modDownloader
        }

        private fun createInfo(minecraft: CurseMinecraft): ModLoaderWrapper? {
            var primaryModLoader: CurseModLoader? = null
            for (modLoader in minecraft.modLoaders) {
                if (modLoader.primary) {
                    primaryModLoader = modLoader
                    break
                }
            }
            if (primaryModLoader == null) primaryModLoader = minecraft.modLoaders[0]
            val modLoaderId = primaryModLoader!!.id
            val dashIndex = modLoaderId.indexOf('-')
            val modLoaderName = modLoaderId.substring(0, dashIndex)
            val modLoaderVersion = modLoaderId.substring(dashIndex + 1)
            Logging.i("CurseForgeModPackInstallHelper",
                StringUtils.insertSpace(modLoaderId, modLoaderName, modLoaderVersion)
            )
            val modloader: ModLoader
            when (modLoaderName) {
                "forge" -> {
                    Logging.i("ModLoader", "Forge, or Quilt? ...")
                    modloader = ModLoader.FORGE
                }
                "neoforge" -> {
                    Logging.i("ModLoader", "NeoForge")
                    modloader = ModLoader.NEOFORGE
                }
                "fabric" -> {
                    Logging.i("ModLoader", "Fabric")
                    modloader = ModLoader.FABRIC
                }
                else -> return null
            }
            return ModLoaderWrapper(modloader, modLoaderVersion, minecraft.version)
        }
    }
}