package com.craftstudio.launcher.feature.mod.modloader

import com.kdt.mcgui.ProgressLayout
import com.craftstudio.launcher.R
import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.feature.mod.modloader.NeoForgeUtils.Companion.downloadNeoForgeVersions
import com.craftstudio.launcher.feature.mod.modloader.NeoForgeUtils.Companion.downloadNeoForgedForgeVersions
import com.craftstudio.launcher.feature.mod.modloader.NeoForgeUtils.Companion.getNeoForgeInstallerUrl
import com.craftstudio.launcher.feature.mod.modloader.NeoForgeUtils.Companion.getNeoForgedForgeInstallerUrl
import com.craftstudio.launcher.feature.version.install.InstallTask
import com.craftstudio.launcher.utils.path.PathManager
import com.craftstudio.launcher.Tools.DownloaderFeedback
import com.craftstudio.launcher.progresskeeper.ProgressKeeper
import com.craftstudio.launcher.utils.DownloadUtils
import java.io.File
import java.io.IOException

class NeoForgeDownloadTask(neoforgeVersion: String) : InstallTask, DownloaderFeedback {
    private var mDownloadUrl: String? = null
    private var mLoaderVersion: String? = neoforgeVersion
    private var mGameVersion: String? = null

    init {
        if (neoforgeVersion.contains("1.20.1")) {
            this.mDownloadUrl = getNeoForgedForgeInstallerUrl(neoforgeVersion)
        } else {
            this.mDownloadUrl = getNeoForgeInstallerUrl(neoforgeVersion)
        }
        Logging.i("NeoForgeDownloadTask", "Version : $mLoaderVersion, Download Url : $mDownloadUrl")
    }

    @Throws(Exception::class)
    override fun run(customName: String): File? {
        var outputFile: File? = null
        if (if (mLoaderVersion!!.contains("1.20.1")) determineNeoForgedForgeDownloadUrl() else determineNeoForgeDownloadUrl()) {
            outputFile = downloadNeoForge()
        }
        ProgressLayout.clearProgress(ProgressLayout.INSTALL_RESOURCE)
        return outputFile
    }

    override fun updateProgress(curr: Long, max: Long) {
        val progress100 = ((curr.toFloat() / max.toFloat()) * 100f).toInt()
        ProgressKeeper.submitProgress(
            ProgressLayout.INSTALL_RESOURCE,
            progress100,
            R.string.mod_download_progress,
            mLoaderVersion
        )
    }

    @Throws(Exception::class)
    private fun downloadNeoForge(): File {
        ProgressKeeper.submitProgress(
            ProgressLayout.INSTALL_RESOURCE,
            0,
            R.string.mod_download_progress,
            mLoaderVersion
        )
        val destinationFile = File(PathManager.DIR_CACHE, "neoforge-installer.jar")
        val buffer = ByteArray(8192)
        DownloadUtils.downloadFileMonitored(mDownloadUrl, destinationFile, buffer, this)
        return destinationFile
    }

    private fun determineDownloadUrl(findVersion: Boolean): Boolean {
        if (mDownloadUrl != null && mLoaderVersion != null) return true
        ProgressKeeper.submitProgress(
            ProgressLayout.INSTALL_RESOURCE,
            0,
            R.string.mod_neoforge_searching
        )
        if (!findVersion) {
            throw IOException("Version not found")
        }
        return true
    }

    @Throws(Exception::class)
    fun determineNeoForgeDownloadUrl(): Boolean {
        return determineDownloadUrl(findNeoForgeVersion())
    }

    @Throws(Exception::class)
    fun determineNeoForgedForgeDownloadUrl(): Boolean {
        return determineDownloadUrl(findNeoForgedForgeVersion())
    }

    private fun findVersion(neoForgeUtils: List<String>?, installerUrl: String): Boolean {
        if (neoForgeUtils == null) return false
        val versionStart = "$mGameVersion-$mLoaderVersion"
        for (versionName in neoForgeUtils) {
            if (!versionName.startsWith(versionStart)) continue
            mLoaderVersion = versionName
            mDownloadUrl = installerUrl
            return true
        }
        return false
    }

    @Throws(Exception::class)
    fun findNeoForgeVersion(): Boolean {
        return findVersion(downloadNeoForgeVersions(false), getNeoForgeInstallerUrl(mLoaderVersion))
    }

    @Throws(Exception::class)
    fun findNeoForgedForgeVersion(): Boolean {
        return findVersion(
            downloadNeoForgedForgeVersions(false),
            getNeoForgedForgeInstallerUrl(mLoaderVersion)
        )
    }
}