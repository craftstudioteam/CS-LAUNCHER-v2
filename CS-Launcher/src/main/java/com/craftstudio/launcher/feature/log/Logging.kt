package com.craftstudio.launcher.feature.log

import android.util.Log
import com.craftstudio.launcher.BuildConfig
import com.craftstudio.launcher.InfoDistributor
import com.craftstudio.launcher.utils.path.PathManager.Companion.DIR_LAUNCHER_LOG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.craftstudio.launcher.Tools
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 启动器日志记录，将软件日志及时写入本地文件存储
 */
object Logging {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val coroutineScope = CoroutineScope(Dispatchers.IO.limitedParallelism(1) + SupervisorJob())
    private val loggerMutex = Mutex()

    @Volatile
    var minLogLevel: Tag = Tag.VERBOSE

    @Volatile
    private var isLauncherInfoWritten = false
    private var FILE_LAUNCHER_LOG: File? = null

    init {
        FILE_LAUNCHER_LOG = getLogFile()
    }

    private fun getLogFile(): File {
        val logPrefix = "log"
        val logSuffix = ".txt"
        val maxLogIndex = 10

        val launcherLogDir = File(DIR_LAUNCHER_LOG).apply { if (!exists()) mkdirs() }

        val logFiles = launcherLogDir.listFiles { file ->
            file.isFile && file.name.startsWith(logPrefix) && file.name.endsWith(logSuffix)
        } ?: emptyArray()

        if (logFiles.isEmpty()) {
            return File(launcherLogDir, "${logPrefix}1$logSuffix")
        }

        val latestFile = logFiles.maxByOrNull { it.lastModified() } ?: return File(launcherLogDir, "${logPrefix}1$logSuffix")
        val latestIndex: Int = latestFile.name.removePrefix(logPrefix).removeSuffix(logSuffix).toIntOrNull() ?: 0
        val nextIndex = (latestIndex % maxLogIndex) + 1

        val nextLogFileName = "$logPrefix$nextIndex$logSuffix"
        val file = File(launcherLogDir, nextLogFileName)
        if (file.exists()) file.delete()
        return file
    }

    private fun writeToFile(log: String, tag: Tag, mark: String) {
        coroutineScope.launch {
            val date = Date(System.currentTimeMillis())
            val timeString = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(date)
            val logString = "[$timeString] (${tag.name}) <$mark> $log"

            appendToFile(logString)
        }
    }

    private suspend fun appendToFile(string: String) {
        loggerMutex.withLock {
            runCatching {
                FILE_LAUNCHER_LOG?.let { file ->
                    if (file.exists() && file.length() >= 15 * 1024 * 1024) { // 15MB
                        FILE_LAUNCHER_LOG = getLogFile()
                        isLauncherInfoWritten = false
                    }
                }

                BufferedWriter(FileWriter(FILE_LAUNCHER_LOG, true)).use { writer ->
                    if (!isLauncherInfoWritten) {
                        isLauncherInfoWritten = true
                        writer.append(getLauncherInfo()).append("\r\n\r\n")
                    }
                    writer.append(string).append("\r\n")
                }
            }.getOrElse { e ->
                Log.e("Logging", "Failed to write log: ${Tools.printToString(e)}")
            }
        }
    }

    private fun getLauncherInfo(): String = """
        =============== ${InfoDistributor.APP_NAME} ===============
        - Version Name : ${BuildConfig.VERSION_NAME}
        - Version Code : ${BuildConfig.VERSION_CODE}
        - Build Type : ${BuildConfig.BUILD_TYPE}
        """.trimIndent()

    @JvmStatic
    fun v(mark: String, verbose: String) {
        if (Tag.VERBOSE.ordinal < minLogLevel.ordinal) return
        Log.v(mark, verbose)
        writeToFile(verbose, Tag.VERBOSE, mark)
    }

    @JvmStatic
    fun v(mark: String, verbose: String, throwable: Throwable) {
        if (Tag.VERBOSE.ordinal < minLogLevel.ordinal) return
        Log.v(mark, verbose, throwable)
        writeToFile("$verbose\n${Tools.printToString(throwable)}", Tag.VERBOSE, mark)
    }

    @JvmStatic
    fun d(mark: String, debug: String) {
        if (Tag.DEBUG.ordinal < minLogLevel.ordinal) return
        Log.d(mark, debug)
        writeToFile(debug, Tag.DEBUG, mark)
    }

    @JvmStatic
    fun d(mark: String, debug: String, throwable: Throwable) {
        if (Tag.DEBUG.ordinal < minLogLevel.ordinal) return
        Log.d(mark, debug, throwable)
        writeToFile("$debug\n${Tools.printToString(throwable)}", Tag.DEBUG, mark)
    }

    @JvmStatic
    fun i(mark: String, info: String) {
        if (Tag.INFO.ordinal < minLogLevel.ordinal) return
        Log.i(mark, info)
        writeToFile(info, Tag.INFO, mark)
    }

    @JvmStatic
    fun i(mark: String, info: String, throwable: Throwable) {
        if (Tag.INFO.ordinal < minLogLevel.ordinal) return
        Log.i(mark, info, throwable)
        writeToFile("$info\n${Tools.printToString(throwable)}", Tag.INFO, mark)
    }

    @JvmStatic
    fun w(mark: String, warn: String) {
        if (Tag.WARN.ordinal < minLogLevel.ordinal) return
        Log.w(mark, warn)
        writeToFile(warn, Tag.WARN, mark)
    }

    @JvmStatic
    fun w(mark: String, warn: String, throwable: Throwable) {
        if (Tag.WARN.ordinal < minLogLevel.ordinal) return
        Log.w(mark, warn, throwable)
        writeToFile("$warn\n${Tools.printToString(throwable)}", Tag.WARN, mark)
    }

    @JvmStatic
    fun e(mark: String, error: String) {
        Log.e(mark, error)
        writeToFile(error, Tag.ERROR, mark)
    }

    @JvmStatic
    fun e(mark: String, error: String, throwable: Throwable) {
        Log.e(mark, error, throwable)
        writeToFile("$error\n${Tools.printToString(throwable)}", Tag.ERROR, mark)
    }

    enum class Tag { VERBOSE, DEBUG, INFO, WARN, ERROR }
}
