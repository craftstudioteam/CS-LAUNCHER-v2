package com.craftstudio.launcher.feature.resource

import java.io.File

data class ResourceItem(
    val displayName: String,
    val fileName: String,
    val sizeKb: Long,
    var file: File,
    var isEnabled: Boolean
)

class LocalResourceManager(private val rootDir: File, private val expectedExtensions: List<String>) {

    fun getResources(): List<ResourceItem> {
        if (!rootDir.exists()) rootDir.mkdirs()
        val files = rootDir.listFiles()?.filter { file ->
            file.isFile && isExpectedResource(file.name)
        } ?: emptyList()

        return files.map { file ->
            val isEnabled = !file.name.endsWith(".disabled")
            val baseName = if (isEnabled) file.name else file.name.removeSuffix(".disabled")
            val displayName = stripExtension(baseName)
            val sizeKb = ((file.length() + 1023) / 1024).coerceAtLeast(1)
            ResourceItem(displayName, file.name, sizeKb, file, isEnabled)
        }.sortedBy { it.displayName.lowercase() }
    }

    fun toggleResource(item: ResourceItem, enable: Boolean): Boolean {
        if (item.isEnabled == enable) return true

        val newName = if (enable) {
            item.file.name.removeSuffix(".disabled")
        } else {
            "${item.file.name}.disabled"
        }

        val newFile = File(item.file.parentFile, newName)
        val success = item.file.renameTo(newFile)
        if (success) {
            item.file = newFile
            item.isEnabled = enable
        }
        return success
    }

    fun deleteResource(item: ResourceItem): Boolean {
        return item.file.delete()
    }

    private fun isExpectedResource(fileName: String): Boolean {
        val baseName = fileName.removeSuffix(".disabled")
        return expectedExtensions.any { baseName.endsWith(it) }
    }

    private fun stripExtension(fileName: String): String {
        return expectedExtensions.firstOrNull { fileName.endsWith(it) }
            ?.let { fileName.removeSuffix(it) }
            ?: fileName
    }
}
