package com.craftstudio.launcher.support.touch_controller

import android.util.SparseIntArray
import android.view.MotionEvent
import android.view.View
import top.fifthlight.touchcontroller.proxy.client.LauncherProxyClient
import top.fifthlight.touchcontroller.proxy.data.Offset

/**
 * 单独在这里处理触点，为TouchController模组的控制代理提供信息
 */
object ContactHandler {
    private val pointerIdMap = SparseIntArray()
    private var nextPointerId = 1

    private fun MotionEvent.getOffset(index: Int, view: View) = Offset(
        getX(index) / view.width,
        getY(index) / view.height
    )

    fun progressEvent(event: MotionEvent, view: View) {
        val client = ControllerProxy.getProxyClient() ?: return

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> handlePointerDown(event, client, 0, view)

            MotionEvent.ACTION_POINTER_DOWN -> handlePointerDown(event, client, event.actionIndex, view)

            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pointerId = pointerIdMap.get(event.getPointerId(i))
                    client.addPointer(pointerId, event.getOffset(i, view))
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                client.clearPointer()
                pointerIdMap.clear()
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val i = event.actionIndex
                val pointerId = pointerIdMap.get(event.getPointerId(i))
                if (pointerId != 0) {
                    pointerIdMap.delete(pointerId)
                    client.removePointer(pointerId)
                }
            }
        }
    }

    private fun handlePointerDown(event: MotionEvent, client: LauncherProxyClient, index: Int, view: View) {
        val pointerId = nextPointerId++
        pointerIdMap.put(event.getPointerId(index), pointerId)
        client.addPointer(pointerId, event.getOffset(index, view))
    }
}