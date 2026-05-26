package com.craftstudio.launcher.feature.version.install

import android.app.Activity
import java.io.File

/**
 * InstallTask的包装类，用于记录更详细的信息
 * @see InstallTask
 */
class InstallTaskItem(
    val selectedVersion: String,
    val isMod: Boolean,
    val task: InstallTask,
    val endTask: EndTask?
) {
    override fun toString(): String {
        return "InstallTaskItem{selectedVersion='$selectedVersion', isMod='$isMod'}"
    }

    fun interface EndTask {
        /**
         * 使用这个任务执行ModLoader的安装
         * @param activity 当前的Activity，用来调出jre选择弹窗、切换至JavaGUI界面
         * @param file 上一个任务执行完成后输出的文件
         */
        @Throws(Throwable::class)
        fun endTask(activity: Activity, file: File)
    }
}