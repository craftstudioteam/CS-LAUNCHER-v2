package com.craftstudio.launcher.plugins.driver

import android.content.Context
import android.content.pm.ApplicationInfo
import com.craftstudio.launcher.setting.AllSettings
import com.craftstudio.launcher.utils.path.PathManager
import com.craftstudio.launcher.utils.ZipUtils
import java.io.File

/**
 * FCL 驱动器插件
 * [FCL DriverPlugin.kt](https://github.com/FCL-Team/FoldCraftLauncher/blob/main/FCLauncher/src/main/java/com/tungsten/fclauncher/plugins/DriverPlugin.kt)
 */
object DriverPluginManager {
    private val driverList: MutableList<Driver> = mutableListOf()

    @JvmStatic
    fun getDriverNameList(): List<String> = driverList.map { it.driver }

    private lateinit var currentDriver: Driver

    @JvmStatic
    fun setDriverByName(driverName: String) {
        currentDriver = driverList.find { it.driver == driverName } ?: driverList[0]
    }

    @JvmStatic
    fun getDriver(): Driver = currentDriver

    /**
     * 导入本地驱动器插件 (.zip)
     */
    fun importLocalDriverPlugin(pluginFile: File): Boolean {
        if (!pluginFile.exists() || !pluginFile.isFile) return false
        
        return try {
            val driverFolder = File(PathManager.DIR_INSTALLED_RENDERER_PLUGIN, "vulkan_drivers/" + pluginFile.nameWithoutExtension)
            if (!driverFolder.exists()) driverFolder.mkdirs()
            
            java.util.zip.ZipFile(pluginFile).use { zipFile ->
                ZipUtils.zipExtract(zipFile, "", driverFolder)
            }
            
            // Add to list if not already there
            if (driverList.none { it.driver == pluginFile.nameWithoutExtension }) {
                driverList.add(Driver(pluginFile.nameWithoutExtension, driverFolder.absolutePath))
            }
            true
        } catch (e: Exception) {
            com.craftstudio.launcher.feature.log.Logging.e("DriverImport", "Failed to import driver", e)
            false
        }
    }

    /**
     * 初始化驱动器
     */
    fun initDriver(context: Context, reset: Boolean) {
        if (reset) driverList.clear()
        driverList.add(Driver("System Default", ""))
        driverList.add(Driver("Custom Turnip / Freedreno", context.applicationInfo.nativeLibraryDir))
        
        // Scan for installed drivers
        val driverBaseDir = File(PathManager.DIR_INSTALLED_RENDERER_PLUGIN, "vulkan_drivers")
        if (driverBaseDir.exists() && driverBaseDir.isDirectory) {
            driverBaseDir.listFiles()?.forEach { 
                if (it.isDirectory) {
                    driverList.add(Driver(it.name, it.absolutePath))
                }
            }
        }
        
        setDriverByName(AllSettings.vulkanDriver.getValue())
    }

    /**
     * 通用 FCL 插件
     */
    fun parsePlugin(info: ApplicationInfo) {
        if (info.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
            val metaData = info.metaData ?: return
            if (metaData.getBoolean("fclPlugin", false)) {
                val driver = metaData.getString("driver") ?: return
                val nativeLibraryDir = info.nativeLibraryDir
                driverList.add(
                    Driver(
                        driver,
                        nativeLibraryDir
                    )
                )
            }
        }
    }
}