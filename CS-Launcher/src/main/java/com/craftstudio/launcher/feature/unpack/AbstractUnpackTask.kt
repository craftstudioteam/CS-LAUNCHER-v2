package com.craftstudio.launcher.feature.unpack

abstract class AbstractUnpackTask : Runnable {
    abstract fun isNeedUnpack(): Boolean
    protected var listener: OnTaskRunningListener? = null

    fun setTaskRunningListener(listener: OnTaskRunningListener) {
        this.listener = listener
    }

    protected fun reportProgress(progress: Int, fileName: String) {
        listener?.onTaskProgress(progress, fileName)
    }
}
