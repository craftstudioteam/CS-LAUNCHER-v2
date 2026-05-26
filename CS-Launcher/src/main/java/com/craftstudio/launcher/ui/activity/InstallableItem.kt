package com.craftstudio.launcher.ui.activity

import com.craftstudio.launcher.feature.unpack.AbstractUnpackTask

class InstallableItem(
    val name: String,
    val summary: String?,
    val task: AbstractUnpackTask,
    var isRunning: Boolean = false,
    var isFinished: Boolean = false
) : Comparable<InstallableItem> {

    override fun compareTo(other: InstallableItem): Int {
        return name.compareTo(other.name, ignoreCase = true)
    }
}