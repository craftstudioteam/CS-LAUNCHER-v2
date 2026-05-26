package com.craftstudio.launcher.event.value

/**
 * 下载页面的一些事件
 */
class DownloadPageEvent {
    /**
     * 切换下载页面时，使用这个事件通知Fragment播放动画
     * @param index Fragment的类别索引
     * @param classify 动画类型（IN：进入动画，OUT：退出动画）
     */
    class PageSwapEvent(val index: Int, val classify: Int) {
        companion object {
            const val IN = 0
            const val OUT = 1
        }
    }

    /**
     * 下载页面已销毁事件
     */
    class PageDestroyEvent

    /**
     * 是否禁用RecyclerView
     */
    class RecyclerEnableEvent(val enable: Boolean)
}