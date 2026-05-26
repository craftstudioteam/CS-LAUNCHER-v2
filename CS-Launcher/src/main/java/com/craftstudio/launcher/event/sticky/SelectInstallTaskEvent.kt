package com.craftstudio.launcher.event.sticky

import com.craftstudio.launcher.feature.version.install.Addon
import com.craftstudio.launcher.feature.version.install.InstallTask

/**
 * 选择安装任务后，将使用这个事件进行通知
 * @param addon 选择的是谁的安装任务
 * @param selectedVersion 选择的版本
 * @param task 选择的任务
 * @see com.craftstudio.launcher.feature.version.install.Addon
 */
class SelectInstallTaskEvent(val addon: Addon, val selectedVersion: String, val task: InstallTask)