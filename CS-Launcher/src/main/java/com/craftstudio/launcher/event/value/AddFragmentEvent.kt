package com.craftstudio.launcher.event.value

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

/**
 * 将一个新的Fragment添加到事务管理中，由LauncherActivity接受并处理
 * 保证Fragment添加的时候，父Fragment一定是当前的Fragment
 * @see com.craftstudio.launcher.LauncherActivity
 * @see com.craftstudio.launcher.utils.ZHTools.addFragment
 */
class AddFragmentEvent(
    val fragmentClass: Class<out Fragment?>,
    val fragmentTag: String?,
    val bundle: Bundle?,
    val fragmentActivityCallback: FragmentActivityCallBack?
) {
    /**
     * 对于当前Fragment的FragmentActivity的一些回调处理
     */
    fun interface FragmentActivityCallBack {
        fun callBack(fragmentActivity: FragmentActivity)
    }
}