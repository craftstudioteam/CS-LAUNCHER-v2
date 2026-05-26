package com.craftstudio.launcher.feature.download.item

import com.craftstudio.launcher.feature.download.enums.Category
import com.craftstudio.launcher.feature.download.enums.Classify
import com.craftstudio.launcher.feature.download.enums.DependencyType
import com.craftstudio.launcher.feature.download.enums.ModLoader
import com.craftstudio.launcher.feature.download.enums.Platform
import java.util.Date

open class DependenciesInfoItem(
    classify: Classify,
    platform: Platform,
    projectId: String,
    slug: String,
    author: Array<String>?,
    title: String,
    description: String,
    downloadCount: Long,
    uploadDate: Date,
    iconUrl: String?,
    category: List<Category>,
    modloaders: List<ModLoader>,
    val dependencyType: DependencyType
) : ModInfoItem (
    classify, platform, projectId, slug, author, title, description, downloadCount, uploadDate, iconUrl, category, modloaders
), Comparable<DependenciesInfoItem> {
    override fun toString(): String {
        return "InfoItem(" +
                "classify='$classify', " +
                "platform='$platform', " +
                "projectId='$projectId', " +
                "slug='$slug', " +
                "author=${author.contentToString()}, " +
                "title='$title', " +
                "description='$description', " +
                "downloadCount=$downloadCount, " +
                "uploadDate=$uploadDate, " +
                "iconUrl='$iconUrl', " +
                "category=$category" +
                "modloaders=$modloaders" +
                "dependencyType=$dependencyType" +
                ")"
    }

    override fun compareTo(other: DependenciesInfoItem): Int {
        return dependencyType.compareTo(other.dependencyType)
    }
}