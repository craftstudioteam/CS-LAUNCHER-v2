package com.craftstudio.launcher.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.animation.ValueAnimator
import androidx.core.app.ActivityCompat
import com.craftstudio.launcher.InfoCenter
import com.craftstudio.launcher.R
import com.craftstudio.launcher.databinding.ActivitySplashBinding
import com.craftstudio.launcher.feature.firebase.LauncherRemoteContentRepository
import com.craftstudio.launcher.feature.unpack.Components
import com.craftstudio.launcher.feature.unpack.Jre
import com.craftstudio.launcher.feature.unpack.UnpackComponentsTask
import com.craftstudio.launcher.feature.unpack.UnpackJreTask
import com.craftstudio.launcher.feature.unpack.UnpackSingleFilesTask
import com.craftstudio.launcher.task.Task
import com.craftstudio.launcher.ui.dialog.ForceUpdateDialog
import com.craftstudio.launcher.ui.dialog.TipDialog
import com.craftstudio.launcher.utils.StoragePermissionsUtils
import com.craftstudio.launcher.LauncherActivity
import com.craftstudio.launcher.MissingStorageActivity
import com.craftstudio.launcher.Tools
import com.craftstudio.launcher.utils.ZHTools

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {
    private var isStarted: Boolean = false
    private lateinit var binding: ActivitySplashBinding
    private lateinit var installableAdapter: InstallableAdapter
    private val items: MutableList<InstallableItem> = ArrayList()

    private var currentFileName: String = ""
    private var progressAnimator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initItems()

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startLogoAnimation()
        startShimmerAnimation()

        if (!Tools.checkStorageRoot()) {
            startActivity(Intent(this, MissingStorageActivity::class.java))
            finish()
            return
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && !StoragePermissionsUtils.hasStoragePermissions(this)) {
            TipDialog.Builder(this)
                .setTitle(R.string.generic_warning)
                .setMessage(InfoCenter.replaceName(this, R.string.permissions_write_external_storage))
                .setWarning()
                .setConfirmClickListener { requestStoragePermissions() }
                .setCancelClickListener { checkEnd() }
                .showDialog()
        } else {
            checkEnd()
        }
    }

    private fun startLogoAnimation() {
        binding.ivLogo.apply {
            scaleX = 0.85f
            scaleY = 0.85f
            animate()
                .scaleX(1.08f)
                .scaleY(1.08f)
                .setDuration(1400)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    animate()
                        .scaleX(0.92f)
                        .scaleY(0.92f)
                        .setDuration(1400)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .withEndAction { startLogoAnimation() }
                        .start()
                }
                .start()
        }
    }

    private fun startShimmerAnimation() {
        binding.root.post {
            val shimmer = binding.progressShimmer
            val parent = shimmer.parent as? View ?: return@post
            val parentWidth = parent.width
            shimmer.translationX = -shimmer.width.toFloat()
            shimmer.animate()
                .translationX(parentWidth.toFloat())
                .setDuration(1800)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    shimmer.animate()
                        .translationX(-shimmer.width.toFloat())
                        .setDuration(1800)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .withEndAction { startShimmerAnimation() }
                        .start()
                }
                .start()
        }
    }

    private fun updateProgress(progress: Int, status: String, fileName: String) {
        animateProgressFill(progress.coerceIn(0, 100))
        binding.tvPercent.text = "${progress.coerceIn(0, 100)}%"
        binding.tvStatus.text = status
        setFileName(fileName)
    }

    private fun animateProgressFill(targetPercent: Int) {
        progressAnimator?.cancel()
        val fill = binding.progressFill
        val parent = fill.parent as? View ?: return
        if (parent.width <= 0) return
        val targetWidth = (parent.width * targetPercent / 100).coerceAtLeast(0)

        val anim = ValueAnimator.ofInt(fill.layoutParams.width, targetWidth)
        anim.duration = 400
        anim.interpolator = OvershootInterpolator(0.6f)
        anim.addUpdateListener { a ->
            fill.layoutParams = fill.layoutParams.apply {
                width = a.animatedValue as Int
            }
        }
        progressAnimator = anim
        anim.start()
    }

    private fun setFileName(fileName: String) {
        val tv = binding.tvFilename
        if (fileName.isBlank()) {
            tv.animate().cancel()
            tv.animate().alpha(0f).setDuration(150).start()
            currentFileName = ""
            return
        }
        if (fileName == currentFileName) return
        currentFileName = fileName

        tv.animate().cancel()
        if (tv.alpha < 0.1f) {
            tv.text = fileName
            tv.animate().alpha(0.8f).setDuration(200).start()
        } else {
            tv.animate().alpha(0f).setDuration(150).withEndAction {
                tv.text = fileName
                tv.animate().alpha(0.8f).setDuration(200).start()
            }.start()
        }
    }

    private fun startInstallIfNeeded() {
        if (isStarted) return
        isStarted = true
        updateProgress(0, getString(R.string.splash_screen_installing), "")

        installableAdapter.setOverallProgressListener(object : InstallableAdapter.OverallProgressListener {
            override fun onOverallProgress(percent: Int, fileName: String) {
                updateProgress(percent, getString(R.string.splash_screen_installing), fileName)
            }
        })

        installableAdapter.startAllTasks()
    }

    private fun requestStoragePermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            STORAGE_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            checkEnd()
        }
    }

    private fun initItems() {
        Components.entries.forEach {
            val unpackComponentsTask = UnpackComponentsTask(this, it)
            if (!unpackComponentsTask.isCheckFailed()) {
                items.add(
                    InstallableItem(
                        it.displayName,
                        it.summary?.let { it1 -> getString(it1) },
                        unpackComponentsTask
                    )
                )
            }
        }
        Jre.entries.forEach {
            val unpackJreTask = UnpackJreTask(this, it)
            if (!unpackJreTask.isCheckFailed()) {
                items.add(
                    InstallableItem(
                        it.jreName,
                        getString(it.summary),
                        unpackJreTask
                    )
                )
            }
        }
        items.sort()
        installableAdapter = InstallableAdapter(items) {
            toMain()
        }
    }

    private fun checkEnd() {
        installableAdapter.checkAllTask()
        Task.runTask {
            UnpackSingleFilesTask(this).run()
        }.execute()

        startInstallIfNeeded()
    }

    private fun toMain() {
        LauncherRemoteContentRepository.load(this) { content ->
            if (content != null && content.shouldForceUpdate(ZHTools.getVersionCode())) {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        ForceUpdateDialog(this, content).show()
                    }
                }
                return@load
            }

            animateProgressFill(100)
            binding.root.postDelayed({
                startActivity(Intent(this, LauncherActivity::class.java))
                finish()
            }, 600)
        }
    }

    companion object {
        private const val STORAGE_PERMISSION_REQUEST_CODE = 1
    }
}
