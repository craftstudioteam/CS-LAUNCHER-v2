package com.craftstudio.launcher.utils.file

import android.content.Context
import com.craftstudio.launcher.R
import com.craftstudio.launcher.task.TaskExecutors
import com.craftstudio.launcher.ui.dialog.ProgressDialog
import com.craftstudio.launcher.utils.ZHTools
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.Future

abstract class FileHandler(
    protected val context: Context,
) {
    protected var currentTask: Future<*>? = null
    private var timer: Timer? = null
    private var lastSize: Long = 0
    private var lastTime: Long = ZHTools.getCurrentTimeMillis()

    protected fun start(progress: FileSearchProgress) {
        TaskExecutors.runInUIThread {
            val dialog = ProgressDialog(context) {
                cancelTask()
                onEnd()
                true
            }
            dialog.updateText(context.getString(R.string.file_operation_file, "0 B", "0 B", 0))

            currentTask = TaskExecutors.getDefault().submit {
                TaskExecutors.runInUIThread { dialog.show() }

                timer = Timer()
                timer?.schedule(object : TimerTask() {
                    override fun run() {
                        val pendingSize = progress.getPendingSize()
                        val totalSize = progress.getTotalSize()
                        val processedSize = totalSize - pendingSize

                        val currentTime = ZHTools.getCurrentTimeMillis()
                        val timeElapsed = (currentTime - lastTime) / 1000.0
                        val sizeChange = processedSize - lastSize
                        val rate = (if (timeElapsed > 0) sizeChange / timeElapsed else 0.0).toLong()

                        lastSize = processedSize
                        lastTime = currentTime

                        TaskExecutors.runInUIThread {
                            dialog.updateText(
                                context.getString(
                                    R.string.file_operation_file,
                                    FileTools.formatFileSize(pendingSize),
                                    FileTools.formatFileSize(totalSize),
                                    progress.getCurrentFileCount()
                                )
                            )
                            dialog.updateRate(rate)
                            dialog.updateProgress(
                                processedSize.toDouble(),
                                totalSize.toDouble()
                            )
                        }
                    }
                }, 0, 100)

                searchFilesToProcess()
                currentTask?.let { task -> if (task.isCancelled) return@submit }
                processFile()

                TaskExecutors.runInUIThread { dialog.dismiss() }
                timer?.cancel()
                onEnd()
            }
        }
    }

    abstract fun searchFilesToProcess()

    abstract fun processFile()

    abstract fun onEnd()

    private fun cancelTask() {
        currentTask?.let {
            if (!currentTask!!.isDone) {
                currentTask?.cancel(true)
                timer?.let { timer?.cancel() }
            }
        }
    }
}