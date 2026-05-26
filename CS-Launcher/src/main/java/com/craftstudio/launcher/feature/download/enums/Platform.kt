package com.craftstudio.launcher.feature.download.enums

import com.craftstudio.launcher.feature.download.platform.AbstractPlatformHelper
import com.craftstudio.launcher.feature.download.platform.curseforge.CurseForgeHelper
import com.craftstudio.launcher.feature.download.platform.modrinth.ModrinthHelper

enum class Platform(val pName: String, val helper: AbstractPlatformHelper) {
    MODRINTH("Modrinth", ModrinthHelper()),
    CURSEFORGE("CurseForge", CurseForgeHelper())
}