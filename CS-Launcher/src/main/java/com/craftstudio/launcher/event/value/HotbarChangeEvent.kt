package com.craftstudio.launcher.event.value

/**
 * 当快捷栏判定框变更时，会广播这个事件
 * @param width 变更后的宽度
 * @param height 变更后的高度
 */
class HotbarChangeEvent(val width: Int, val height: Int)