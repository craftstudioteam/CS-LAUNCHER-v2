package com.craftstudio.launcher.feature.unpack

interface OnTaskRunningListener {
    fun onTaskStart()
    fun onTaskProgress(progress: Int, fileName: String)
    fun onTaskEnd()
}
