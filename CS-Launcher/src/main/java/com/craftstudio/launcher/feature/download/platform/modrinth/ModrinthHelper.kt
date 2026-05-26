package com.craftstudio.launcher.feature.download.platform.modrinth

import com.craftstudio.launcher.feature.download.Filters
import com.craftstudio.launcher.feature.download.enums.Classify
import com.craftstudio.launcher.feature.download.install.InstallHelper
import com.craftstudio.launcher.feature.download.item.InfoItem
import com.craftstudio.launcher.feature.download.item.ModLoaderWrapper
import com.craftstudio.launcher.feature.download.item.ScreenshotItem
import com.craftstudio.launcher.feature.download.item.SearchResult
import com.craftstudio.launcher.feature.download.item.VersionItem
import com.craftstudio.launcher.feature.download.platform.AbstractPlatformHelper
import com.craftstudio.launcher.feature.download.platform.PlatformNotSupportedException
import com.craftstudio.launcher.modloaders.modpacks.api.ApiHandler
import java.io.File

class ModrinthHelper : AbstractPlatformHelper(ApiHandler("https://api.modrinth.com/v2")) {
    override fun copy(): AbstractPlatformHelper {
        return ModrinthHelper()
    }

    override fun getWebUrl(infoItem: InfoItem): String? {
        return "https://modrinth.com/${
            when (infoItem.classify) {
                Classify.ALL -> return null
                Classify.MOD -> "mod"
                Classify.MODPACK -> "modpack"
                Classify.RESOURCE_PACK -> "resourcepack"
                Classify.WORLD -> return null
                Classify.SHADER_PACK -> "shader"
            }
        }/${infoItem.slug}"
    }

    override fun getScreenshots(projectId: String): List<ScreenshotItem> {
        return ModrinthCommonUtils.getScreenshots(api, projectId)
    }

    @Throws(Throwable::class)
    override fun searchMod(filters: Filters, lastResult: SearchResult): SearchResult? {
        return ModrinthModHelper.modLikeSearch(api, lastResult, filters, "mod", Classify.MOD)
    }

    @Throws(Throwable::class)
    override fun searchModPack(filters: Filters, lastResult: SearchResult): SearchResult? {
        return ModrinthModHelper.modLikeSearch(api, lastResult, filters, "modpack", Classify.MODPACK)
    }

    @Throws(Throwable::class)
    override fun searchResourcePack(filters: Filters, lastResult: SearchResult): SearchResult? {
        return ModrinthCommonUtils.getResults(api, lastResult, filters, "resourcepack", Classify.RESOURCE_PACK)
    }

    @Throws(Throwable::class)
    override fun searchWorld(filters: Filters, lastResult: SearchResult): SearchResult? {
        throw PlatformNotSupportedException("Modrinth does not provide archive download support.") //Modrinth不提供MC存档
    }

    @Throws(Throwable::class)
    override fun searchShaderPack(filters: Filters, lastResult: SearchResult): SearchResult? {
        return ModrinthCommonUtils.getResults(api, lastResult, filters, "shader", Classify.SHADER_PACK)
    }

    @Throws(Throwable::class)
    override fun getModVersions(infoItem: InfoItem, force: Boolean): List<VersionItem>? {
        return ModrinthModHelper.getModVersions(api, infoItem, force)
    }

    @Throws(Throwable::class)
    override fun getModPackVersions(infoItem: InfoItem, force: Boolean): List<VersionItem>? {
        return ModrinthModHelper.getModPackVersions(api, infoItem, force)
    }

    @Throws(Throwable::class)
    override fun getResourcePackVersions(infoItem: InfoItem, force: Boolean): List<VersionItem>? {
        return ModrinthCommonUtils.getVersions(api, infoItem, force)
    }

    @Throws(Throwable::class)
    override fun getWorldVersions(infoItem: InfoItem, force: Boolean): List<VersionItem>? {
        throw PlatformNotSupportedException("Modrinth does not provide archive download support.") //Modrinth不提供MC存档
    }

    @Throws(Throwable::class)
    override fun getShaderPackVersions(infoItem: InfoItem, force: Boolean): List<VersionItem>? {
        return ModrinthCommonUtils.getVersions(api, infoItem, force)
    }

    @Throws(Throwable::class)
    override fun installMod(infoItem: InfoItem, version: VersionItem, targetPath: File, progressKey: String) {
        InstallHelper.downloadFile(version, targetPath, progressKey)
    }

    @Throws(Throwable::class)
    override fun installModPack(version: VersionItem, customName: String): ModLoaderWrapper? {
        return ModrinthModPackInstallHelper.startInstall(version, customName)
    }

    @Throws(Throwable::class)
    override fun installResourcePack(infoItem: InfoItem, version: VersionItem, targetPath: File, progressKey: String) {
        InstallHelper.downloadFile(version, targetPath, progressKey)
    }

    @Throws(Throwable::class)
    override fun installWorld(infoItem: InfoItem, version: VersionItem, targetPath: File, progressKey: String) {
        throw PlatformNotSupportedException("Modrinth does not provide archive download support.")
    }

    @Throws(Throwable::class)
    override fun installShaderPack(infoItem: InfoItem, version: VersionItem, targetPath: File, progressKey: String) {
        InstallHelper.downloadFile(version, targetPath, progressKey)
    }
}