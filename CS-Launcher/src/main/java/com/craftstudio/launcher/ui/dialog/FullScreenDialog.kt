package com.craftstudio.launcher.ui.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import com.craftstudio.launcher.R

abstract class FullScreenDialog(context: Context) : Dialog(context, R.style.CustomDialogStyle) {
    override fun onCreate(savedInstanceState: Bundle?) {
        setupFullScreenDialog()
    }

    private fun setupFullScreenDialog() {
        window?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            )
            setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )

            setSystemUiVisibility(decorView)
        }
    }

    private fun setSystemUiVisibility(decorView: View) {
        val flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        decorView.systemUiVisibility = flags
    }

    /**
     * 修改自FCL [FCLAlertDialog.java](https://github.com/FCL-Team/FoldCraftLauncher/blob/e7d82af/FCLLibrary/src/main/java/com/tungsten/fcllibrary/component/dialog/FCLAlertDialog.java#L62-L77)
     */
    protected fun checkHeight(root: View, content: View, scrollView: View, dpValue: Int = 50) {
        root.post {
            val wm = requireNotNull(window?.windowManager) { "WindowManager is null" }
            val point = Point()
            wm.defaultDisplay.getSize(point)
            val maxHeight = (point.y - dpValue * context.resources.displayMetrics.density + 0.5f).toInt()

            val isRootHeightLessThanMax = root.measuredHeight < maxHeight
            val layoutParams = scrollView.layoutParams

            if (isRootHeightLessThanMax) {
                layoutParams.height = content.measuredHeight
                scrollView.layoutParams = layoutParams
                window?.setLayout(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
            } else {
                window?.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, maxHeight)
            }
        }
    }
}