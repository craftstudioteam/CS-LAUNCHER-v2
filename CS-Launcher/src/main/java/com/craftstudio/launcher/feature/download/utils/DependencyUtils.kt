package com.craftstudio.launcher.feature.download.utils

import android.content.Context
import com.craftstudio.launcher.R
import com.craftstudio.launcher.feature.download.enums.DependencyType

class DependencyUtils {
    companion object {
        fun getDependencyTypeFromModrinth(type: String?): DependencyType {
            return DependencyType.entries.find { it.modrinth != null && it.modrinth == type }
                ?: DependencyType.REQUIRED
        }

        fun getDependencyTypeFromCurseForge(type: String?): DependencyType {
            return DependencyType.entries.find { it.curseforge != null && it.curseforge == type }
                ?: DependencyType.REQUIRED
        }

        fun getTextFromType(context: Context, type: DependencyType?): String {
            return when (type) {
                DependencyType.OPTIONAL -> context.getString(R.string.download_install_dependencies_optional)
                DependencyType.INCOMPATIBLE -> context.getString(R.string.download_install_dependencies_incompatible)
                DependencyType.EMBEDDED -> context.getString(R.string.download_install_dependencies_embedded)
                DependencyType.TOOL -> context.getString(R.string.download_install_dependencies_tool)
                DependencyType.INCLUDE -> context.getString(R.string.download_install_dependencies_include)
                DependencyType.REQUIRED -> context.getString(R.string.download_install_dependencies_required)
                else -> context.getString(R.string.download_install_dependencies_required)
            }
        }
    }
}