package com.craftstudio.launcher.feature.unpack

import android.content.Context
import com.craftstudio.launcher.feature.log.Logging.e
import com.craftstudio.launcher.utils.CopyDefaultFromAssets.Companion.copyFromAssets
import com.craftstudio.launcher.utils.path.PathManager
import com.craftstudio.launcher.Tools

class UnpackSingleFilesTask(val context: Context) : AbstractUnpackTask() {
    override fun isNeedUnpack(): Boolean = true

    override fun run() {
        runCatching {
            copyFromAssets(context)
            Tools.copyAssetFile(context, "resolv.conf", PathManager.DIR_DATA, false)
        }.getOrElse { e("AsyncAssetManager", "Failed to unpack critical components !") }
    }
}