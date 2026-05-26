package com.craftstudio.launcher.feature.version.favorites

import com.craftstudio.launcher.feature.version.VersionsManager
import java.util.concurrent.ConcurrentHashMap

class FavoritesVersionUtils private constructor() {
    companion object {
        private inline fun modifyFavorites(action: (MutableMap<String, MutableSet<String>>) -> Unit) {
            VersionsManager.currentGameInfo.apply {
                action(favoritesMap)
                saveCurrentInfo()
            }
        }

        /**
         * 原子化重命名版本
         */
        fun renameVersion(oldName: String, newName: String) = modifyFavorites { map ->
            map.values.forEach { versions ->
                if (oldName in versions) {
                    versions.remove(oldName)
                    versions.add(newName)
                }
            }
        }

        /**
         * 添加一个收藏夹
         */
        fun addFolder(name: String) = modifyFavorites { map ->
            map.putIfAbsent(name, ConcurrentHashMap.newKeySet())
        }

        /**
         * 移除一个收藏夹
         */
        fun removeFolder(name: String) = modifyFavorites { map ->
            map.remove(name)
        }

        /**
         * 更新版本收藏夹
         * @param version 目标版本
         * @param targetFolders 需要包含该版本的收藏夹集合
         */
        fun updateVersionFolders(version: String, targetFolders: Set<String>) = modifyFavorites { map ->
            //添加至目标收藏夹
            targetFolders.forEach { folder ->
                map.getOrPut(folder) { ConcurrentHashMap.newKeySet() }.add(version)
            }

            //从非目标收藏夹移除
            map.keys.filterNot { it in targetFolders }.forEach { folder ->
                map[folder]?.remove(version)
            }
        }

        /**
         * 获取有效收藏夹结构
         */
        fun getFavoritesStructure(): Map<String, Set<String>> =
            VersionsManager.currentGameInfo.favoritesMap.let { map ->
                map.entries.associate { (k, v) -> k to v.toSet() }
            }

        /**
         * 获取指定收藏夹的有效版本
         */
        fun getValidVersions(folder: String): Set<String> =
            VersionsManager.currentGameInfo.favoritesMap[folder]
                ?.filter { VersionsManager.checkVersionExistsByName(it) }
                .orEmpty()
                .toSet()
    }
}