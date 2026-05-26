package com.craftstudio.launcher.feature.download

import com.craftstudio.launcher.feature.download.item.DependenciesInfoItem
import com.craftstudio.launcher.feature.download.item.ModLikeVersionItem
import com.craftstudio.launcher.feature.download.item.ModVersionItem
import com.craftstudio.launcher.feature.download.item.VersionItem

/**
 * 将搜索得到的信息缓存在内存中，下次加载时可直接从内存中拿到上次的搜索结果
 */
class InfoCache {
    abstract class CacheBase<V> {
        private val cache: MutableMap<String, V> = HashMap()

        /**
         * 根据ModId，将搜索到的值存入内存
         */
        fun put(modId: String, value: V) {
            cache[modId] = value
        }

        /**
         * 根据ModId，拿到内存中存储的值，若没有，则返回空
         */
        fun get(modId: String): V? {
            return cache[modId]
        }

        /**
         * 检查内存中是否存在已经存入的ModId
         */
        fun containsKey(modId: String): Boolean {
            return cache.containsKey(modId)
        }
    }

    object DependencyInfoCache : CacheBase<DependenciesInfoItem>()
    object VersionCache : CacheBase<MutableList<VersionItem>>()
    object ModVersionCache : CacheBase<MutableList<ModVersionItem>>()
    object ModPackVersionCache : CacheBase<MutableList<ModLikeVersionItem>>()
}