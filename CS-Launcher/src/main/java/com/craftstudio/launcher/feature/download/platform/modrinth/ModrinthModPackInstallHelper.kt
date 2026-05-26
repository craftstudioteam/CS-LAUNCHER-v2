package com.craftstudio.launcher.feature.download.platform.modrinth

import com.kdt.mcgui.ProgressLayout
import com.craftstudio.launcher.R
import com.craftstudio.launcher.feature.download.enums.ModLoader
import com.craftstudio.launcher.feature.download.install.InstallHelper
import com.craftstudio.launcher.feature.download.item.ModLoaderWrapper
import com.craftstudio.launcher.feature.download.item.VersionItem
import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.feature.mod.modpack.install.ModPackUtils.Companion.verifyModrinthIndex
import com.craftstudio.launcher.Tools
import com.craftstudio.launcher.modloaders.modpacks.api.ModDownloader
import com.craftstudio.launcher.modloaders.modpacks.models.ModrinthIndex
import com.craftstudio.launcher.progresskeeper.DownloaderProgressWrapper
import com.craftstudio.launcher.utils.ZipUtils
import java.io.File
import java.util.zip.ZipFile

class ModrinthModPackInstallHelper {
    companion object {
        @Throws(Exception::class)
        fun startInstall(versionItem: VersionItem, customName: String): ModLoaderWrapper? {
            return InstallHelper.installModPack(versionItem, customName) { modpackFile, targetPath ->
                installZip(modpackFile, targetPath)
            }
        }

        @Throws(Exception::class)
        fun installZip(packFile: File, targetPath: File): ModLoaderWrapper? {
            ZipFile(packFile).use { modpackZipFile ->
                val modrinthIndex = Tools.GLOBAL_GSON.fromJson(
                    Tools.read(ZipUtils.getEntryStream(modpackZipFile, "modrinth.index.json")),
                    ModrinthIndex::class.java
                )
                if (!verifyModrinthIndex(modrinthIndex)) {
                    Logging.i("ModrinthModPackInstallHelper", "manifest verification failed")
                    return null
                }
                val modDownloader = ModDownloader(targetPath)
                for (indexFile in modrinthIndex.files) {
                    modDownloader.submitDownload(
                        indexFile.fileSize,
                        indexFile.path,
                        indexFile.hashes.sha1,
                        *indexFile.downloads
                    )
                }
                modDownloader.awaitFinish(
                    DownloaderProgressWrapper(
                        R.string.modpack_download_downloading_mods,
                        ProgressLayout.INSTALL_RESOURCE
                    )
                )
                ProgressLayout.setProgress(
                    ProgressLayout.INSTALL_RESOURCE,
                    0,
                    R.string.modpack_download_applying_overrides,
                    1,
                    2
                )
                ZipUtils.zipExtract(modpackZipFile, "overrides/", targetPath)
                ProgressLayout.setProgress(ProgressLayout.INSTALL_RESOURCE, 50, R.string.modpack_download_applying_overrides, 2, 2)
                ZipUtils.zipExtract(modpackZipFile, "client-overrides/", targetPath)
                return createInfo(modrinthIndex)
            }
        }

        private fun createInfo(modrinthIndex: ModrinthIndex?): ModLoaderWrapper? {
            if (modrinthIndex == null) return null
            val dependencies = modrinthIndex.dependencies
            val mcVersion = dependencies["minecraft"] ?: return null
            dependencies["forge"]?.let {
                Logging.i("ModLoader", "Forge")
                return ModLoaderWrapper(ModLoader.FORGE, it, mcVersion)
            }
            dependencies["neoforge"]?.let {
                Logging.i("ModLoader", "NeoForge")
                return ModLoaderWrapper(ModLoader.NEOFORGE, it, mcVersion)
            }
            dependencies["fabric-loader"]?.let {
                Logging.i("ModLoader", "Fabric")
                return ModLoaderWrapper(ModLoader.FABRIC, it, mcVersion)
            }
            dependencies["quilt-loader"]?.let {
                Logging.i("ModLoader", "Quilt")
                return ModLoaderWrapper(ModLoader.QUILT, it, mcVersion)
            }
            return null
        }
    }
}