package com.craftstudio.launcher.ui.fragment.download.addon

import com.craftstudio.launcher.R
import com.craftstudio.launcher.feature.mod.modloader.FabricLikeUtils

class DownloadQuiltFragment : DownloadFabricLikeFragment(FabricLikeUtils.QUILT_UTILS, R.drawable.ic_quilt) {
    companion object {
        const val TAG: String = "DownloadQuiltFragment"
    }
}