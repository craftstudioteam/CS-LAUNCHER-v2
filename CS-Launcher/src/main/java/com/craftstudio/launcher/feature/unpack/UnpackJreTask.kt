package com.craftstudio.launcher.feature.unpack

import android.content.Context
import android.content.res.AssetManager
import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.Architecture
import com.craftstudio.launcher.Tools
import com.craftstudio.launcher.multirt.MultiRTUtils

class UnpackJreTask(val context: Context, val jre: Jre) : AbstractUnpackTask() {
    private lateinit var assetManager: AssetManager
    private lateinit var launcherRuntimeVersion: String
    private var isCheckFailed: Boolean = false

    init {
        runCatching {
            assetManager = context.assets
            launcherRuntimeVersion = Tools.read(assetManager.open(jre.jrePath + "/version"))
        }.getOrElse {
            isCheckFailed = true
        }
    }

    fun isCheckFailed() = isCheckFailed

    override fun isNeedUnpack(): Boolean {
        if (isCheckFailed) return false

        runCatching {
            val installedRuntimeVersion = MultiRTUtils.readInternalRuntimeVersion(jre.jreName)
            return launcherRuntimeVersion != installedRuntimeVersion
        }.getOrElse { e ->
            Logging.e("CheckInternalRuntime", Tools.printToString(e))
            return false
        }
    }

    override fun run() {
        listener?.onTaskStart()

        val arch = Architecture.archAsString(Tools.DEVICE_ARCHITECTURE)
        val universalPath = jre.jrePath + "/universal.tar.xz"
        val binPath = jre.jrePath + "/bin-$arch.tar.xz"
        val paths = arrayOf(universalPath, binPath)
        val labels = arrayOf("universal.tar.xz", "bin-$arch.tar.xz")

        val sizes = LongArray(2)
        var totalSize = 0L

        for (i in 0..1) {
            try {
                val s = assetManager.open(paths[i])
                val avail = s.available().toLong()
                s.close()
                sizes[i] = if (avail > 0) avail else -1
                if (sizes[i] > 0) totalSize += sizes[i]
            } catch (_: Exception) {
                sizes[i] = -1
            }
        }

        val hasByteSizes = totalSize > 0

        runCatching {
            val raw1 = assetManager.open(universalPath)
            val raw2 = assetManager.open(binPath)

            val s1size = if (hasByteSizes && sizes[0] > 0) sizes[0] else 1L
            val s2size = if (hasByteSizes && sizes[1] > 0) sizes[1] else 1L
            val effectiveTotal = if (hasByteSizes) totalSize else (s1size + s2size)

            val stream1 = ProgressInputStream(raw1, s1size, 0L) { cum ->
                listener?.onTaskProgress(((cum * 100) / effectiveTotal).toInt().coerceIn(0, 99), labels[0])
            }

            val stream2 = ProgressInputStream(raw2, s2size, s1size) { cum ->
                listener?.onTaskProgress(((cum * 100) / effectiveTotal).toInt().coerceIn(0, 99), labels[1])
            }

            MultiRTUtils.installRuntimeNamedBinpack(
                stream1, stream2,
                jre.jreName, launcherRuntimeVersion
            )
            MultiRTUtils.postPrepare(jre.jreName)
        }.getOrElse { e -> Logging.e("UnpackJREAuto", "Internal JRE unpack failed", e) }

        listener?.onTaskProgress(100, "")
        listener?.onTaskEnd()
    }
}
