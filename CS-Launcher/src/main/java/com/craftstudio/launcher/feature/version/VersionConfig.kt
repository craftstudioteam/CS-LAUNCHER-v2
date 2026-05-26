package com.craftstudio.launcher.feature.version

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.google.gson.JsonParser
import com.craftstudio.launcher.R
import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.feature.version.VersionsManager.getCSVersionPath
import com.craftstudio.launcher.setting.AllSettings
import com.craftstudio.launcher.utils.stringutils.StringUtils.getStringNotNull
import com.craftstudio.launcher.Tools
import java.io.File
import java.io.FileNotFoundException
import java.io.FileWriter

class VersionConfig(private var versionPath: File) : Parcelable {
    private var isolationType: IsolationType = IsolationType.FOLLOW_GLOBAL
    private var javaDir: String = ""
    private var javaArgs: String = ""
    private var renderer: String = ""
    private var driver: String = ""
    private var control: String = ""
    private var customPath: String = ""
    private var customInfo: String = ""

    constructor(
        filePath: File,
        isolationType: IsolationType = IsolationType.FOLLOW_GLOBAL,
        javaDir: String = "",
        javaArgs: String = "",
        renderer: String = "",
        driver: String = "",
        control: String = "",
        customPath: String = "",
        customInfo: String = ""
    ) : this(filePath) {
        this.isolationType = isolationType
        this.javaDir = javaDir
        this.javaArgs = javaArgs
        this.renderer = renderer
        this.driver = driver
        this.control = control
        this.customPath = customPath
        this.customInfo = customInfo
    }

    fun copy(): VersionConfig = VersionConfig(versionPath,
        getIsolationTypeNotNull(isolationType),
        getStringNotNull(javaDir),
        getStringNotNull(javaArgs),
        getStringNotNull(renderer),
        getStringNotNull(driver),
        getStringNotNull(control),
        getStringNotNull(customPath),
        getStringNotNull(customInfo)
    )

    fun save() {
        runCatching {
            saveWithThrowable()
        }.onFailure { e ->
            Logging.e("Save Version Config", "$this\n${Tools.printToString(e)}")
        }
    }

    @Throws(Throwable::class)
    fun saveWithThrowable() {
        Logging.i("Save Version Config", "Trying to save: $this")
        val csVersionPath = getCSVersionPath(versionPath)
        val configFile = File(csVersionPath, "VersionConfig.json")
        if (!csVersionPath.exists()) csVersionPath.mkdirs()

        FileWriter(configFile, false).use {
            val json = Tools.GLOBAL_GSON.toJson(this)
            it.write(json)
        }
        Logging.i("Save Version Config", "Saved: $this")
    }

    fun getVersionPath() = versionPath

    fun setVersionPath(versionPath: File) {
        this.versionPath = versionPath
    }

    fun isIsolation(): Boolean = when(getIsolationTypeNotNull(isolationType)) {
        IsolationType.FOLLOW_GLOBAL -> AllSettings.versionIsolation.getValue()
        IsolationType.ENABLE -> true
        IsolationType.DISABLE -> false
    }

    fun getIsolationType() = getIsolationTypeNotNull(isolationType)

    fun setIsolationType(isolationType: IsolationType) { this.isolationType = isolationType }

    fun getJavaDir(): String = getStringNotNull(javaDir)

    fun setJavaDir(dir: String) { this.javaDir = dir }

    fun getJavaArgs(): String = getStringNotNull(javaArgs)

    fun setJavaArgs(args: String) { this.javaArgs = args }

    fun getRenderer(): String = getStringNotNull(renderer)

    fun setRenderer(renderer: String) { this.renderer = renderer }

    fun getDriver(): String = getStringNotNull(driver)

    fun setDriver(driver: String) { this.driver = driver }

    fun getControl(): String = getStringNotNull(control)

    fun setControl(control: String) { this.control = control }

    fun getCustomPath(): String = getStringNotNull(customPath)

    fun setCustomPath(customPath: String) { this.customPath = customPath }

    fun getCustomInfo(): String = getStringNotNull(customInfo)

    fun setCustomInfo(customInfo: String) { this.customInfo = customInfo }

    fun checkDifferent(otherConfig: VersionConfig): Boolean {
        return !(this.getIsolationType() == otherConfig.getIsolationType() &&
                this.getJavaDir() == otherConfig.getJavaDir() &&
                this.getJavaArgs() == otherConfig.getJavaArgs() &&
                this.getRenderer() == otherConfig.getRenderer() &&
                this.getDriver() == otherConfig.getDriver() &&
                this.getControl() == otherConfig.getControl() &&
                this.getCustomPath() == otherConfig.getCustomPath() &&
                this.getCustomInfo() == otherConfig.getCustomInfo())
    }

    private fun getIsolationTypeNotNull(type: IsolationType?) = type ?: IsolationType.FOLLOW_GLOBAL

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(versionPath.absolutePath)
        dest.writeInt(getIsolationTypeNotNull(isolationType).ordinal)
        dest.writeString(getStringNotNull(javaDir))
        dest.writeString(getStringNotNull(javaArgs))
        dest.writeString(getStringNotNull(renderer))
        dest.writeString(getStringNotNull(driver))
        dest.writeString(getStringNotNull(control))
        dest.writeString(getStringNotNull(customPath))
        dest.writeString(getStringNotNull(customInfo))
    }

    companion object CREATOR : Parcelable.Creator<VersionConfig> {
        override fun createFromParcel(parcel: Parcel): VersionConfig {
            val versionPath = File(parcel.readString().orEmpty())
            val isolationType = IsolationType.entries.getOrNull(parcel.readInt()) ?: IsolationType.FOLLOW_GLOBAL
            val javaDir = parcel.readString().orEmpty()
            val javaArgs = parcel.readString().orEmpty()
            val renderer = parcel.readString().orEmpty()
            val driver = parcel.readString().orEmpty()
            val control = parcel.readString().orEmpty()
            val customPath = parcel.readString().orEmpty()
            val customInfo = parcel.readString().orEmpty()
            return VersionConfig(versionPath, isolationType, javaDir, javaArgs, renderer, driver, control, customPath, customInfo)
        }

        override fun newArray(size: Int): Array<VersionConfig?> {
            return arrayOfNulls(size)
        }

        @JvmStatic
        fun parseConfig(versionPath: File): VersionConfig {
            //兼容旧版本的版本隔离文件（识别并保存为新版本后，旧的版本隔离文件将被删除）
            val oldConfigFile = File(getCSVersionPath(versionPath), "ZalithVersion.cfg")
            val configFile = File(getCSVersionPath(versionPath), "VersionConfig.json")

            return runCatching getConfig@{
                if (oldConfigFile.exists()) {
                    runCatching {
                        Tools.GLOBAL_GSON.fromJson(Tools.read(oldConfigFile), VersionConfig::class.java).apply {
                            setIsolationType(IsolationType.ENABLE)
                            setVersionPath(versionPath)
                            save()
                        }
                    }.getOrNull().let { config ->
                        //移除旧的配置文件
                        oldConfigFile.delete()
                        config?.let { return@getConfig it }
                    }
                }

                if (!configFile.exists()) {
                    val config = VersionConfig(versionPath)
                    config.save()
                    return@getConfig config
                }

                //读取此文件的内容，并解析为VersionConfig
                val configString = Tools.read(configFile)
                val config = Tools.GLOBAL_GSON.fromJson(configString, VersionConfig::class.java)
                runCatching {
                    JsonParser.parseString(configString).asJsonObject.apply {
                        if (has("isolation")) {
                            config.setIsolationType(
                                if (get("isolation").asBoolean) IsolationType.ENABLE
                                else IsolationType.DISABLE
                            )
                        }
                    }
                }.onFailure { Logging.e("Refresh Versions", "Failed to parse the version isolation field of the old version.", it) }
                config.setVersionPath(versionPath)
                config
            }.recoverCatching {
                throw if (it is FileNotFoundException) {
                    FileNotFoundException("Missing version config: ${configFile.absolutePath}")
                } else it
            }.getOrElse { e ->
                Logging.e("Refresh Versions", Tools.printToString(e))
                val config = VersionConfig(versionPath)
                config.save()
                config
            }
        }

        @JvmStatic
        fun createIsolation(versionPath: File): VersionConfig {
            val config = VersionConfig(versionPath)
            config.setIsolationType(IsolationType.ENABLE)
            return config
        }

        @JvmStatic
        fun getIsolationString(context: Context, type: IsolationType): String = when (type) {
            IsolationType.FOLLOW_GLOBAL -> context.getString(R.string.version_manager_isolation_type_follow_global)
            IsolationType.ENABLE -> context.getString(R.string.generic_open)
            IsolationType.DISABLE -> context.getString(R.string.generic_close)
        }
    }

    enum class IsolationType {
        FOLLOW_GLOBAL, ENABLE, DISABLE
    }
}