package com.craftstudio.launcher.feature.mod.modpack.install

/**
 * 整合包关键信息
 * @param name 整合包的名称，创建新版本的时候将采用这个名称
 * @param type 整合包的类别（CurseForge、Modrinth、MCBBS）
 */
data class ModPackInfo(val name: String?, val type: ModPackUtils.ModPackEnum)
