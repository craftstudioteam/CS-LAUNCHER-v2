package com.craftstudio.launcher.ui.overlay

import android.app.Activity
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.ImageView
import com.craftstudio.launcher.R

class DownloadOverlayController(activity: Activity) {
    private val overlay: View = activity.findViewById(R.id.fl_download_overlay)
    private val logo: ImageView = activity.findViewById(R.id.iv_cs_logo_anim)
    private val label: TextView = activity.findViewById(R.id.tv_download_label)
    private val percent: TextView = activity.findViewById(R.id.tv_download_pct)
    private val step: TextView = activity.findViewById(R.id.tv_download_step)
    private val linearProgress: ProgressBar = activity.findViewById(R.id.pb_linear)

    private var logoAnimator: AnimatorSet? = null

    fun show(labelText: String) {
        label.text = labelText
        overlay.alpha = 0f
        overlay.visibility = View.VISIBLE
        overlay.animate().alpha(1f).setDuration(400L).start()
        startLogoAnimation()
    }

    fun updateProgress(progressValue: Int, stepText: String) {
        percent.text = "$progressValue%"
        linearProgress.progress = progressValue
        if (stepText.isNotEmpty()) step.text = stepText
        if (progressValue % 25 == 0) {
            linearProgress.animate().scaleY(2f).setDuration(150L).withEndAction {
                linearProgress.animate().scaleY(1f).setDuration(150L).start()
            }.start()
        }
    }

    fun hide() {
        overlay.animate().alpha(0f).setDuration(500L).withEndAction {
            overlay.visibility = View.GONE
            stopLogoAnimation()
        }.start()
    }

    private fun startLogoAnimation() {
        stopLogoAnimation()
        val rotate = ObjectAnimator.ofFloat(logo, View.ROTATION, 0f, 360f).apply {
            duration = 3000L
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
        }
        val scaleX = ObjectAnimator.ofFloat(logo, View.SCALE_X, 1f, 1.12f, 1f).apply {
            duration = 1200L
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        val scaleY = ObjectAnimator.ofFloat(logo, View.SCALE_Y, 1f, 1.12f, 1f).apply {
            duration = 1200L
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        logoAnimator = AnimatorSet().apply {
            playTogether(rotate, scaleX, scaleY)
            start()
        }
    }

    private fun stopLogoAnimation() {
        logoAnimator?.cancel()
        logoAnimator = null
    }
}
