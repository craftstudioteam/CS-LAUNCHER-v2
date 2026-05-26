package com.craftstudio.launcher.feature.customprofilepath

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.feature.version.VersionsManager
import com.craftstudio.launcher.setting.AllSettings
import com.craftstudio.launcher.ui.subassembly.customprofilepath.ProfileItem
import com.craftstudio.launcher.utils.path.PathManager
import com.craftstudio.launcher.utils.StoragePermissionsUtils
import com.craftstudio.launcher.Tools
import java.io.File
import java.io.FileWriter

object ProfilePathManager {
    private val defaultPath: String = PathManager.DIR_GAME_HOME
    private var profilePathData: MutableList<ProfileItem> = mutableListOf()

    fun setCurrentPathId(id: String) {
        AllSettings.launcherProfile.put(id).save()
        VersionsManager.refresh("ProfilePathManager:setCurrentPathId")
    }

    fun refreshPath() {
        val configFile = PathManager.FILE_PROFILE_PATH
        if (!configFile.exists()) return

        val json = Tools.read(configFile).takeIf { it.isNotEmpty() } ?: return
        profilePathData = parseProfileData(json)
    }

    private fun parseProfileData(json: String): MutableList<ProfileItem> {
        val jsonObject = JsonParser.parseString(json).asJsonObject
        return jsonObject.entrySet().mapNotNull { (key, value) ->
            runCatching {
                val profilePath = Tools.GLOBAL_GSON.fromJson(value, ProfilePathJsonObject::class.java)
                ProfileItem(key, profilePath.title, profilePath.path)
            }.onFailure { e ->
                Logging.e("parseProfileItem", "Failed to parse profile item: $key", e)
            }.getOrNull()
        }.toMutableList()
    }

    fun getCurrentPath(): String {
        if (!StoragePermissionsUtils.checkPermissions()) return defaultPath

        val profileId = AllSettings.launcherProfile.getValue()
        val path = if (profileId == "default") defaultPath else findProfilePath(profileId) ?: defaultPath

        createNoMediaFile(path)
        return path
    }

    fun getAllPath(): List<ProfileItem> = profilePathData.toList()

    fun addPath(profile: ProfileItem) {
        profilePathData.add(profile)
        save()
    }

    fun containsPath(path: String): Boolean = profilePathData.any { it.path == path }

    private fun findProfilePath(profileId: String): String? {
        if (profilePathData.isEmpty()) refreshPath()
        return profilePathData.firstOrNull { it.id == profileId }?.path
    }

    private fun createNoMediaFile(path: String) {
        val noMediaFile = File(path, ".nomedia")
        if (!noMediaFile.exists()) {
            runCatching { noMediaFile.createNewFile() }
                .onFailure { e -> Logging.e("createNoMedia", "Failed to create .nomedia in $path", e) }
        }
    }

    fun save() {
        save(profilePathData)
    }

    fun save(items: List<ProfileItem>) {
        val jsonObject = JsonObject()

        items.forEach { item ->
            if (item.id == "default") return@forEach
            val profilePathJsonObject = ProfilePathJsonObject(item.title, item.path)
            jsonObject.add(item.id, Tools.GLOBAL_GSON.toJsonTree(profilePathJsonObject))
        }

        runCatching {
            FileWriter(PathManager.FILE_PROFILE_PATH).use { writer ->
                Tools.GLOBAL_GSON.toJson(jsonObject, writer)
            }
        }.onFailure { e ->
            Logging.e("Write Profile", "Failed to write to game path configuration", e)
        }
    }
}
