package com.craftstudio.launcher.feature.runtime

import androidx.appcompat.app.AppCompatActivity
import com.craftstudio.launcher.ui.overlay.DownloadOverlayController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class JavaRuntimeBootstrap(
    private val activity: AppCompatActivity,
    private val overlay: DownloadOverlayController
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val downloader = JavaRuntimeDownloader()

    fun start() {
        scope.launch {
            val needs17 = !downloader.isRuntimeInstalled(17)
            val needs21 = !downloader.isRuntimeInstalled(21)
            if (!needs17 && !needs21) return@launch

            overlay.show("Preparing CS Launcher...")
            try {
                if (needs17) {
                    downloader.ensureRuntime(17) { pct, step ->
                        activity.runOnUiThread { overlay.updateProgress(pct, step) }
                    }
                }
                if (needs21) {
                    downloader.ensureRuntime(21) { pct, step ->
                        activity.runOnUiThread { overlay.updateProgress(pct, step) }
                    }
                }
            } finally {
                activity.runOnUiThread { overlay.hide() }
            }
        }
    }
}
