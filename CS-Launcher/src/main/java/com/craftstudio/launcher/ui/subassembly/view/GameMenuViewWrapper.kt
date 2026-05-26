package com.craftstudio.launcher.ui.subassembly.view

import android.app.Activity
import android.view.View
import android.widget.TextView
import com.getkeepsafe.taptargetview.TapTargetView
import com.craftstudio.launcher.R
import com.craftstudio.launcher.setting.AllSettings
import com.craftstudio.launcher.task.TaskExecutors
import com.craftstudio.launcher.utils.NewbieGuideUtils
import com.craftstudio.launcher.utils.file.FileTools.Companion.formatFileSize
import com.craftstudio.launcher.utils.platform.MemoryUtils
import com.petterp.floatingx.assist.FxGravity
import com.petterp.floatingx.assist.helper.FxScopeHelper
import com.petterp.floatingx.listener.IFxViewLifecycle
import com.petterp.floatingx.listener.control.IFxScopeControl
import com.petterp.floatingx.view.FxViewHolder
import org.lwjgl.glfw.CallbackBridge
import java.util.Timer
import java.util.TimerTask

class GameMenuViewWrapper(
    private val activity: Activity,
    private val listener: View.OnClickListener,
    private val showInfo: Boolean
) {
    companion object {
        private const val TAG = "GameMenuViewWrapper"
    }

    private var timer: Timer? = null
    private val memoryText: String = AllSettings.gameMenuMemoryText.getValue()
    private var showMemory: Boolean = false
    private var showFPS: Boolean = false
    private var visible: Boolean = false

    private var scopeFx: IFxScopeControl? = null

    init {
        refreshState()
    }

    private fun getWindow(): IFxScopeControl {
        return FxScopeHelper.Builder().apply {
            setLayout(R.layout.view_game_menu_window)
            setOnClickListener(0L, listener)
            setEnableEdgeAdsorption(false)
            addViewLifecycle(object : IFxViewLifecycle {
                override fun initView(holder: FxViewHolder) {
                    holder.view.alpha = AllSettings.gameMenuAlpha.getValue().toFloat() / 100f

                    updateInfoText(holder.view)
                }

                override fun detached(view: View) {
                    cancelInfoTimer()
                }
            })
            setGravity(getCurrentGravity())
        }.build().toControl(activity)
    }

    private fun startNewbieGuide(mainView: View) {
        if (NewbieGuideUtils.showOnlyOne(TAG)) return
        TapTargetView.showFor(
            activity,
            NewbieGuideUtils.getSimpleTarget(activity, mainView,
                activity.getString(R.string.setting_category_game_menu),
                activity.getString(R.string.newbie_guide_game_menu)
            )
        )
    }

    fun setVisibility(visible: Boolean) {
        this.visible = visible
        thinkForVisibility()
    }

    fun refreshSettingsState() {
        refreshState()
        thinkForVisibility()
    }

    /**
     * 根据三个条件判断是否显示悬浮窗（是否想要显示、是否展示内存信息、是否展示FPS）
     */
    private fun thinkForVisibility() {
        val v1 = visible || showMemory || showFPS
        if (v1) {
            if (scopeFx != null) {
                updateInfoText()
            } else {
                scopeFx = getWindow().apply {
                    updateInfoText()
                    show()
                    getView()?.let { startNewbieGuide(it) }
                }
            }
        } else {
            scopeFx?.cancel()
            scopeFx = null
            cancelInfoTimer()
        }
    }

    private fun refreshState() {
        showMemory = AllSettings.gameMenuShowMemory.getValue()
        showFPS = AllSettings.gameMenuShowFPS.getValue()
    }

    private fun updateInfoText() {
        scopeFx?.getView()?.apply {
            updateInfoText(this)
        }
    }

    private fun updateInfoText(view: View) {
        cancelInfoTimer()

        val memoryText: TextView = view.findViewById(R.id.memoryText)
        val fpsText: TextView = view.findViewById(R.id.fpsText)

        fun updateInfoText() {
            if (showMemory) {
                val memoryString = "${this@GameMenuViewWrapper.memoryText} ${getUsedDeviceMemory()}/${getTotalDeviceMemory()}".let { string ->
                    if (string.length > 40) return@let string.take(40)
                    string
                }
                TaskExecutors.runInUIThread { memoryText.text = memoryString }
            }
            if (showFPS) {
                val fpsString = "FPS: ${CallbackBridge.getCurrentFps()}"
                TaskExecutors.runInUIThread { fpsText.text = fpsString }
            }
        }

        updateInfoText()

        if (showInfo) {
            memoryText.visibility = if (showMemory) View.VISIBLE else View.GONE
            fpsText.visibility = if (showFPS) View.VISIBLE else View.GONE

            if (showMemory || showFPS) {
                timer = Timer().apply {
                    schedule(object : TimerTask() {
                        override fun run() {
                            updateInfoText()
                        }
                    }, 0, AllSettings.gameMenuInfoRefreshRate.getValue().toLong())
                }
            }
        }
    }

    private fun getUsedDeviceMemory(): String = formatFileSize(MemoryUtils.getUsedDeviceMemory(activity))

    private fun getTotalDeviceMemory(): String = formatFileSize(MemoryUtils.getTotalDeviceMemory(activity))

    private fun cancelInfoTimer() {
        timer?.cancel()
        timer = null
    }

    private fun getCurrentGravity(): FxGravity {
        return when(AllSettings.gameMenuLocation.getValue()) {
            "left_or_top" -> FxGravity.LEFT_OR_TOP
            "left_or_bottom" -> FxGravity.LEFT_OR_BOTTOM
            "right_or_top" -> FxGravity.RIGHT_OR_TOP
            "right_or_bottom" -> FxGravity.RIGHT_OR_BOTTOM
            "top_or_center" -> FxGravity.TOP_OR_CENTER
            "bottom_or_center" -> FxGravity.BOTTOM_OR_CENTER
            "center" -> FxGravity.CENTER
            else -> FxGravity.CENTER
        }
    }
}