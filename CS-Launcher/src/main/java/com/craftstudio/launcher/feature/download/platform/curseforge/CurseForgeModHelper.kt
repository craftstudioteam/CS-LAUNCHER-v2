package com.craftstudio.launcher.feature.download.platform.curseforge

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.craftstudio.launcher.feature.download.Filters
import com.craftstudio.launcher.feature.download.InfoCache
import com.craftstudio.launcher.feature.download.enums.Category
import com.craftstudio.launcher.feature.download.enums.Classify
import com.craftstudio.launcher.feature.download.enums.ModLoader
import com.craftstudio.launcher.feature.download.enums.Platform
import com.craftstudio.launcher.feature.download.item.DependenciesInfoItem
import com.craftstudio.launcher.feature.download.item.InfoItem
import com.craftstudio.launcher.feature.download.item.ModInfoItem
import com.craftstudio.launcher.feature.download.item.ModLikeVersionItem
import com.craftstudio.launcher.feature.download.item.ModVersionItem
import com.craftstudio.launcher.feature.download.item.SearchResult
import com.craftstudio.launcher.feature.download.item.VersionItem
import com.craftstudio.launcher.feature.download.platform.PlatformNotSupportedException
import com.craftstudio.launcher.feature.download.utils.DependencyUtils
import com.craftstudio.launcher.feature.download.utils.ModLoaderUtils
import com.craftstudio.launcher.feature.download.utils.PlatformUtils
import com.craftstudio.launcher.feature.download.utils.VersionTypeUtils
import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.utils.MCVersionRegex.Companion.RELEASE_REGEX
import com.craftstudio.launcher.utils.ZHTools
import com.craftstudio.launcher.Tools
import com.craftstudio.launcher.modloaders.modpacks.api.ApiHandler
import com.craftstudio.launcher.utils.GsonJsonUtils
import java.util.TreeSet

class CurseForgeModHelper {
    companion object {
        @Throws(Throwable::class)
        internal fun modLikeSearch(api: ApiHandler, lastResult: SearchResult, filters: Filters, type: Int, classify: Classify): SearchResult? {
            if (filters.category != Category.ALL && filters.category.curseforgeID == null) {
                throw PlatformNotSupportedException("The platform does not support the ${filters.category} category!")
            }

            PlatformUtils.searchModLikeWithChinese(filters, type == CurseForgeCommonUtils.CURSEFORGE_MOD_CLASS_ID)?.let {
                filters.name = it
            }

            val params = HashMap<String, Any>()
            CurseForgeCommonUtils.putDefaultParams(params, filters, lastResult.previousCount)
            params["classId"] = type
            filters.modloader?.let { params["modLoaderTypes"] = "[${it.curseforgeId}]" }

            val response = api.get("mods/search", params, JsonObject::class.java) ?: return null
            val dataArray = response.getAsJsonArray("data") ?: return null

            val infoItems: MutableList<InfoItem> = ArrayList()
            for (data in dataArray) {
                val dataElement = data.asJsonObject
                CurseForgeCommonUtils.getInfoItem(dataElement, classify)?.let { item ->
                    infoItems.add(
                        ModInfoItem(
                            item.classify,
                            Platform.CURSEFORGE,
                            item.projectId,
                            item.slug,
                            item.author,
                            item.title,
                            item.description,
                            item.downloadCount,
                            item.uploadDate,
                            item.iconUrl,
                            item.category,
                            getModLoaders(dataElement.getAsJsonArray("latestFilesIndexes"))
                        )
                    )
                }
            }

            return CurseForgeCommonUtils.returnResults(lastResult, infoItems, dataArray, response)
        }

        @Throws(Throwable::class)
        internal fun <T : VersionItem> getModOrModPackVersions(
            api: ApiHandler,
            infoItem: InfoItem,
            force: Boolean,
            cache: InfoCache.CacheBase<MutableList<T>>,
            createVersionItem: (
                projectId: String,
                title: String,
                downloadCount: Long,
                fileDate: String,
                mcVersions: List<String>,
                releaseType: String,
                fileHash: String?,
                downloadUrl: String,
                modloaders: List<ModLoader>,
                fileName: String,
                dependencies: List<DependenciesInfoItem>?
            ) -> T
        ): List<T>? {
            if (!force && cache.containsKey(infoItem.projectId)) return cache.get(infoItem.projectId)

            val allModData = CurseForgeCommonUtils.getPaginatedData(api, infoItem.projectId)

            val versionItems: MutableList<T> = ArrayList()
            val invalidDependencies: MutableList<String> = ArrayList()
            for (modData in allModData) {
                try {
                    // 获取版本信息
                    val mcVersions: MutableSet<String> = TreeSet()
                    for (gameVersionElement in modData.getAsJsonArray("gameVersions")) {
                        val gameVersion = gameVersionElement.asString
                        mcVersions.add(gameVersion)
                    }

                    val modloaders: MutableList<ModLoader> = ArrayList()
                    mcVersions.forEach { ModLoaderUtils.addModLoaderToList(modloaders, it) }

                    // 过滤非MC版本的元素
                    val releaseRegex = RELEASE_REGEX
                    val nonMCVersion: MutableSet<String> = TreeSet()
                    mcVersions.forEach { string: String ->
                        if (!releaseRegex.matcher(string).find()) nonMCVersion.add(string)
                    }
                    if (nonMCVersion.isNotEmpty()) mcVersions.removeAll(nonMCVersion)

                    val dependencies = modData.get("dependencies")?.asJsonArray
                    val dependencyInfoList: MutableList<DependenciesInfoItem> = ArrayList()
                    if (dependencies != null && dependencies.size() != 0) {
                        for (dependency in dependencies) {
                            val dObject = dependency.asJsonObject
                            val modId = dObject.get("modId").asString
                            if (invalidDependencies.contains(modId)) continue

                            if (!InfoCache.DependencyInfoCache.containsKey(modId)) {
                                val response = CurseForgeCommonUtils.searchModFromID(api, modId)
                                val hit = GsonJsonUtils.getJsonObjectSafe(response, "data")

                                if (hit != null) {
                                    val dModLoaders = getModLoaders(hit.getAsJsonArray("latestFilesIndexes"))
                                    InfoCache.DependencyInfoCache.put(
                                        modId, DependenciesInfoItem(
                                            infoItem.classify,
                                            Platform.CURSEFORGE,
                                            modId,
                                            hit.get("slug").asString,
                                            CurseForgeCommonUtils.getAuthors(hit.get("authors").asJsonArray).toTypedArray(),
                                            hit.get("name").asString,
                                            hit.get("summary").asString,
                                            hit.get("downloadCount").asLong,
                                            ZHTools.getDate(hit.get("dateCreated").asString),
                                            CurseForgeCommonUtils.getIconUrl(hit),
                                            CurseForgeCommonUtils.getAllCategories(hit).toList(),
                                            dModLoaders,
                                            DependencyUtils.getDependencyTypeFromCurseForge(dObject.get("relationType").asString)
                                        )
                                    )
                                } else invalidDependencies.add(modId)
                            }

                            val cacheItem = InfoCache.DependencyInfoCache.get(modId)
                            cacheItem?.let { dependencyInfoList.add(it) }
                        }
                    }

                    versionItems.add(
                        createVersionItem(
                            infoItem.projectId,
                            modData.get("displayName").asString,
                            modData.get("downloadCount").asLong,
                            modData.get("fileDate").asString,
                            mcVersions.toList(),
                            modData.get("releaseType").asString,
                            CurseForgeCommonUtils.getSha1FromData(modData),
                            modData.get("downloadUrl").asString,
                            modloaders,
                            modData.get("fileName").asString,
                            dependencyInfoList.ifEmpty { null }
                        )
                    )
                } catch (e: Exception) {
                    Logging.e("CurseForgeHelper", Tools.printToString(e))
                    continue
                }
            }

            cache.put(infoItem.projectId, versionItems)
            return versionItems
        }

        @Throws(Throwable::class)
        internal fun getModVersions(api: ApiHandler, infoItem: InfoItem, force: Boolean): List<ModVersionItem>? {
            return getModOrModPackVersions(
                api,
                infoItem,
                force,
                InfoCache.ModVersionCache
            ) { projectId, title, downloadCount, fileDate, mcVersions, releaseType, fileHash, downloadUrl, modloaders, fileName, dependencies ->
                ModVersionItem(
                    projectId,
                    title,
                    downloadCount,
                    ZHTools.getDate(fileDate),
                    mcVersions,
                    VersionTypeUtils.getVersionType(releaseType),
                    fileName,
                    fileHash,
                    downloadUrl,
                    modloaders,
                    dependencies ?: emptyList()
                )
            }
        }

        @Throws(Throwable::class)
        internal fun getModPackVersions(api: ApiHandler, infoItem: InfoItem, force: Boolean): List<ModLikeVersionItem>? {
            return getModOrModPackVersions(
                api,
                infoItem,
                force,
                InfoCache.ModPackVersionCache
            ) { projectId, title, downloadCount, fileDate, mcVersions, releaseType, fileHash, downloadUrl, modloaders, fileName, _ ->
                ModLikeVersionItem(
                    projectId,
                    title,
                    downloadCount,
                    ZHTools.getDate(fileDate),
                    mcVersions,
                    VersionTypeUtils.getVersionType(releaseType),
                    fileName,
                    fileHash,
                    downloadUrl,
                    modloaders
                )
            }
        }

        private fun getModLoaders(data: JsonArray): List<ModLoader> {
            val modLoaders: MutableSet<ModLoader> = HashSet()
            for (element in data) {
                val jsonObject = element.asJsonObject
                val modloader = jsonObject.get("modLoader")?.asString ?: continue
                ModLoaderUtils.getModLoaderByCurseForge(modloader)?.let {
                    modLoaders.add(it)
                }
            }
            return modLoaders.toList()
        }
    }
}