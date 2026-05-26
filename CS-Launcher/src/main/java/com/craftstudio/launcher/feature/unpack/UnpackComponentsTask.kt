package com.craftstudio.launcher.feature.unpack

import android.content.Context
import android.content.res.AssetManager
import com.craftstudio.launcher.feature.log.Logging.i
import com.craftstudio.launcher.utils.path.PathManager
import com.craftstudio.launcher.Tools
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

class UnpackComponentsTask(val context: Context, val component: Components) : AbstractUnpackTask() {
    private lateinit var am: AssetManager
    private lateinit var rootDir: String
    private lateinit var versionFile: File
    private lateinit var input: InputStream
    private var isCheckFailed: Boolean = false

    init {
        runCatching {
            am = context.assets
            rootDir = if (component.privateDirectory) PathManager.DIR_DATA else PathManager.DIR_GAME_HOME
            versionFile = File("$rootDir/${component.component}/version")
            input = am.open("components/${component.component}/version")
        }.getOrElse {
            isCheckFailed = true
        }
    }

    fun isCheckFailed() = isCheckFailed

    override fun isNeedUnpack(): Boolean {
        if (isCheckFailed) return false

        if (!versionFile.exists()) {
            requestEmptyParentDir(versionFile)
            i("Unpack Components", "${component.component}: Pack was installed manually, or does not exist...")
            return true
        } else {
            val fis = FileInputStream(versionFile)
            val release1 = Tools.read(input)
            val release2 = Tools.read(fis)
            if (release1 != release2) {
                requestEmptyParentDir(versionFile)
                return true
            } else {
                i("UnpackPrep", "${component.component}: Pack is up-to-date with the launcher, continuing...")
                return false
            }
        }
    }

    override fun run() {
        listener?.onTaskStart()
        val fileList = am.list("components/${component.component}") ?: emptyArray()
        val totalFiles = fileList.size
        if (totalFiles == 0) {
            listener?.onTaskEnd()
            return
        }

        val sizes = LongArray(totalFiles)
        var totalBytes = 0L

        for (i in fileList.indices) {
            try {
                val s = am.open("components/${component.component}/${fileList[i]}")
                val avail = s.available().toLong()
                s.close()
                sizes[i] = if (avail > 0) avail else -1
                if (sizes[i] > 0) totalBytes += sizes[i]
            } catch (_: Exception) {
                sizes[i] = -1
            }
        }

        val hasByteSizes = totalBytes > 0
        var knownBytesProcessed = 0L
        var filesProcessed = 0

        for (i in fileList.indices) {
            val fileName = fileList[i]
            val filePath = "components/${component.component}/$fileName"
            val baseDir = "$rootDir/${component.component}"
            val destFile = File(baseDir, fileName)
            destFile.parentFile?.mkdirs()

            if (hasByteSizes && sizes[i] > 0) {
                val fileStream = am.open(filePath)
                val progressInput = ProgressInputStream(fileStream, sizes[i]) { readBytes ->
                    val overallBytes = knownBytesProcessed + readBytes
                    val pct = ((overallBytes * 100) / totalBytes).toInt()
                    listener?.onTaskProgress(pct.coerceIn(0, 99), fileName)
                }
                FileOutputStream(destFile).use { out -> IOUtils.copy(progressInput, out) }
                fileStream.close()
                knownBytesProcessed += sizes[i]
            } else {
                val pct = if (hasByteSizes) {
                    ((knownBytesProcessed * 100) / totalBytes).toInt()
                } else {
                    (filesProcessed * 100) / totalFiles
                }
                listener?.onTaskProgress(pct.coerceIn(0, 99), fileName)
                Tools.copyAssetFile(context, filePath, baseDir, true)
                knownBytesProcessed += if (sizes[i] > 0) sizes[i] else 1
                totalBytes = maxOf(totalBytes, knownBytesProcessed)
            }
            filesProcessed++
        }

        listener?.onTaskProgress(100, "")
        listener?.onTaskEnd()
    }

    private fun requestEmptyParentDir(file: File) {
        file.parentFile!!.apply {
            if (exists() and isDirectory) {
                FileUtils.deleteDirectory(this)
            }
            mkdirs()
        }
    }
}
