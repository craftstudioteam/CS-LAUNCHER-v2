package com.craftstudio.launcher.ui.fragment

import androidx.fragment.app.Fragment
import com.craftstudio.launcher.progresskeeper.ProgressKeeper
import com.craftstudio.launcher.progresskeeper.TaskCountListener

abstract class BaseFragment : Fragment, TaskCountListener {
    private var mIsTaskRunning: Boolean = false

    constructor() : super()

    constructor(contentLayoutId: Int) : super(contentLayoutId)

    open fun onBackPressed(): Boolean = true

    fun isTaskRunning() = mIsTaskRunning

    fun forceBack() {
        requireActivity().supportFragmentManager.popBackStackImmediate()
    }

    override fun onStart() {
        super.onStart()
        ProgressKeeper.addTaskCountListener(this)
    }

    override fun onStop() {
        super.onStop()
        ProgressKeeper.removeTaskCountListener(this)
    }

    override fun onUpdateTaskCount(taskCount: Int) {
        this.mIsTaskRunning = taskCount != 0
    }
}