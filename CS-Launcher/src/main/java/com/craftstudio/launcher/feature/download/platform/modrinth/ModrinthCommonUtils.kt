package com.craftstudio.launcher.feature.download.platform.modrinth

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.craftstudio.launcher.feature.download.Filters
import com.craftstudio.launcher.feature.download.InfoCache
import com.craftstudio.launcher.feature.download.enums.Category
import com.craftstudio.launcher.feature.download.enums.Classify
import com.craftstudio.launcher.feature.download.enums.Platform
import com.craftstudio.launcher.feature.download.item.InfoItem
import com.craftstudio.launcher.feature.download.item.ScreenshotItem
import com.craftstudio.launcher.feature.download.item.SearchResult
import com.craftstudio.launcher.feature.download.item.VersionItem
import com.craftstudio.launcher.feature.download.platform.PlatformNotSupportedException
import com.craftstudio.launcher.feature.download.utils.CategoryUtils
import com.craftstudio.launcher.feature.download.utils.PlatformUtils.Companion.safeRun
import com.craftstudio.launcher.feature.download.utils.VersionTypeUtils
import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.utils.ZHTools
import com.craftstudio.launcher.utils.stringutils.StringUtilsKt
import com.craftstudio.launcher.Tools
import com.craftstudio.launcher.modloaders.modpacks.api.ApiHandler
import java.util.StringJoiner
import java.util.TreeSet

class ModrinthCommonUtils {
    companion object {
        private const val MODRINTH_SEARCH_COUNT = 20

        private fun getCategories(filters: Filters): String {
            val categories = mutableListOf<String>().apply {
                filters.modloader?.let { add(it.modrinthName) }
                if (filters.category != Category.ALL) {
                    add(filters.category.modrinthName!!)
                }
            }
            return if (categories.isEmpty()) ""
            else categories.joinToString { "[\"categories:$it\"]" }
        }

        private fun putDefaultParams(params: HashMap<String, Any>, filters: Filters, previousCount: Int) {
            params["query"] = filters.name
            params["limit"] = MODRINTH_SEARCH_COUNT
            params["index"] = filters.sort.modrinth
            params["offset"] = previousCount
        }

        internal fun getAllCategories(hit: JsonObject): Set<Category> {
            val list: MutableSet<Category> = TreeSet()
            for (categories in hit["categories"].asJsonArray) {
                val name = categories.asString
                CategoryUtils.getCategoryByModrinth(name)?.let { list.add(it) }
            }
            return list
        }

        internal fun getIconUrl(hit: JsonObject): String? {
            return runCatching {
                hit.get("icon_url").asString
            }.getOrNull()
        }

        internal fun getScreenshots(api: ApiHandler, projectId: String): List<ScreenshotItem> {
            searchModFromID(api, projectId)?.let { hit ->
                val screenshotItems: MutableList<ScreenshotItem> = ArrayList()
                hit.getAsJsonArray("gallery").forEach { element ->
                    runCatching {
                        val screenshotObject = element.asJsonObject
                        val url = screenshotObject.get("url").asString

                        val titleElement = screenshotObject.get("title")
                        val titleString = if (titleElement.isJsonNull) null
                        else StringUtilsKt.getNonEmptyOrBlank(titleElement.asString)

                        val descriptionElement = screenshotObject.get("description")
                        val descriptionString = if (descriptionElement.isJsonNull) null
                        else StringUtilsKt.getNonEmptyOrBlank(descriptionElement.asString)

                        screenshotItems.add(ScreenshotItem(url, titleString, descriptionString))
                    }.getOrElse { e ->
                        val error = Tools.printToString(e)
                        Logging.e("ModrinthCommonUtils", "There was an exception while getting the screenshot information!\n$error")
                    }
                }
                return screenshotItems
            }
            return emptyList()
        }

        internal fun getResults(api: ApiHandler, lastResult: SearchResult, filters: Filters, type: String, classify: Classify): SearchResult? {
            if (filters.category != Category.ALL && filters.category.modrinthName == null) {
                throw PlatformNotSupportedException("The platform does not support the ${filters.category} category!")
            }

            val response = api.get("search", getParams(lastResult, filters, type), JsonObject::class.java) ?: return null
            val responseHits = response.getAsJsonArray("hits") ?: return null

            val infoItems: MutableList<InfoItem> = ArrayList()
            for (responseHit in responseHits) {
                val hit = responseHit.asJsonObject
                getInfoItem(hit, classify)?.let { item ->
                    infoItems.add(item)
                }
            }

            return returnResults(lastResult, infoItems, response, responseHits)
        }

        internal fun getParams(lastResult: SearchResult, filters: Filters, type: String): HashMap<String, Any> {
            val params = HashMap<String, Any>()
            val facetString = StringJoiner(",", "[", "]")
            facetString.add("[\"project_type:$type\"]")

            filters.mcVersion?.let { facetString.add("[\"versions:$it\"]") }
            getCategories(filters).let { if (it.isNotBlank()) facetString.add(it) }

            params["facets"] = facetString.toString()
            putDefaultParams(params, filters, lastResult.previousCount)

            return params
        }

        private fun getInfoItem(hit: JsonObject, classify: Classify): InfoItem? {
            val categories = hit.get("categories").asJsonArray
            for (category in categories) {
                if (category.asString == "datapack") return null //没有数据包安装的需求，一律排除
            }
            return InfoItem(
                classify,
                Platform.MODRINTH,
                hit.get("project_id").asString,
                hit.get("slug").asString,
                arrayOf(hit.get("author").asString),
                hit.get("title").asString,
                hit.get("description").asString,
                hit.get("downloads").asLong,
                ZHTools.getDate(hit.get("date_created").asString),
                getIconUrl(hit),
                getAllCategories(hit).toList(),
            )
        }

        fun getInfo(api: ApiHandler, classify: Classify, projectId: String): InfoItem? {
            searchModFromID(api, projectId)?.let { hit ->
                return InfoItem(
                    classify,
                    Platform.MODRINTH,
                    projectId,
                    hit.get("slug").asString,
                    null,
                    hit.get("title").asString,
                    hit.get("description").asString,
                    hit.get("downloads").asLong,
                    ZHTools.getDate(hit.get("published").asString),
                    getIconUrl(hit),
                    getAllCategories(hit).toList()
                )
            }
            return null
        }

        @Throws(Throwable::class)
        internal fun <T> getCommonVersions(
            api: ApiHandler,
            infoItem: InfoItem,
            force: Boolean,
            cache: InfoCache.CacheBase<MutableList<T>>,
            createItem: (JsonObject, JsonObject, MutableList<String>) -> T
        ): List<T>? {
            if (!force && cache.containsKey(infoItem.projectId))
                return cache.get(infoItem.projectId)

            val response = api.get("project/${infoItem.projectId}/version", JsonArray::class.java) ?: return null

            val items: MutableList<T> = ArrayList()
            //如果第一次获取依赖信息失败，则记录其id，之后不再尝试获取
            val invalidDependencies: MutableList<String> = ArrayList()
            for (element in response) {
                try {
                    val versionObject = element.asJsonObject
                    val filesJsonObject: JsonObject = versionObject.getAsJsonArray("files").get(0).asJsonObject

                    items.add(createItem(versionObject, filesJsonObject, invalidDependencies))
                } catch (e: Exception) {
                    Logging.e("ModrinthHelper", Tools.printToString(e))
                    continue
                }
            }

            cache.put(infoItem.projectId, items)
            return items
        }

        @Throws(Throwable::class)
        internal fun getVersions(api: ApiHandler, infoItem: InfoItem, force: Boolean): List<VersionItem>? {
            return getCommonVersions(
                api, infoItem, force, InfoCache.VersionCache
            ) { versionObject, filesJsonObject, _ ->
                VersionItem(
                    infoItem.projectId,
                    versionObject.get("name").asString,
                    versionObject.get("downloads").asLong,
                    ZHTools.getDate(versionObject.get("date_published").asString),
                    getMcVersions(versionObject.getAsJsonArray("game_versions")),
                    VersionTypeUtils.getVersionType(versionObject.get("version_type").asString),
                    filesJsonObject.get("filename").asString,
                    getSha1Hash(filesJsonObject),
                    filesJsonObject.get("url").asString
                )
            }
        }

        internal fun getMcVersions(gameVersionJson: JsonArray): List<String> {
            val mcVersions: MutableList<String> = java.util.ArrayList()
            for (gameVersion in gameVersionJson) {
                mcVersions.add(gameVersion.asString)
            }
            return mcVersions
        }

        internal fun getSha1Hash(filesJsonObject: JsonObject): String? {
            val hashesMap = filesJsonObject.getAsJsonObject("hashes")
            return if ((hashesMap != null && hashesMap.has("sha1"))) hashesMap["sha1"].asString else null
        }

        internal fun searchModFromID(api: ApiHandler, id: String): JsonObject? {
            return api.safeRun { get("project/$id", JsonObject::class.java) }?.also {
                Logging.i("Modrinth_searchModFromID", it.toString())
            }
        }

        internal fun returnResults(
            lastResult: SearchResult,
            infoItems: List<InfoItem>,
            response: JsonObject,
            responseHits: JsonArray
        ): SearchResult = lastResult.apply {
            this.infoItems.addAll(infoItems)
            this.previousCount += responseHits.size()
            this.totalResultCount = response.get("total_hits").asInt
            this.isLastPage = responseHits.size() < MODRINTH_SEARCH_COUNT
        }
    }
}