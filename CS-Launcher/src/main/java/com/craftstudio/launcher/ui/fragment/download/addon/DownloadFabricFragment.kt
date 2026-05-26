package com.craftstudio.launcher.ui.fragment.download.addon

import com.craftstudio.launcher.R
import com.craftstudio.launcher.feature.mod.modloader.FabricLikeUtils

class DownloadFabricFragment : DownloadFabricLikeFragment(FabricLikeUtils.FABRIC_UTILS, R.drawable.ic_fabric) {
    companion object {
        const val TAG: String = "DownloadFabricFragment"
    }
}