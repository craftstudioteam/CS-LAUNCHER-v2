package com.craftstudio.launcher.feature.mod.modloader

import com.craftstudio.launcher.modloaders.ForgeVersionListHandler
import com.craftstudio.launcher.utils.DownloadUtils
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.IOException
import java.io.StringReader
import javax.xml.parsers.SAXParserFactory

class NeoForgeUtils {
    companion object {
        private const val NEOFORGE_METADATA_URL =
            "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml"
        private const val NEOFORGE_INSTALLER_URL =
            "https://maven.neoforged.net/releases/net/neoforged/neoforge/%1\$s/neoforge-%1\$s-installer.jar"
        private const val NEOFORGED_FORGE_METADATA_URL =
            "https://maven.neoforged.net/releases/net/neoforged/forge/maven-metadata.xml"
        private const val NEOFORGED_FORGE_INSTALLER_URL =
            "https://maven.neoforged.net/releases/net/neoforged/forge/%1\$s/forge-%1\$s-installer.jar"

        @Throws(Exception::class)
        private fun downloadVersions(metaDataUrl: String, name: String, force: Boolean): List<String> {
            val parserFactory = SAXParserFactory.newInstance()
            val saxParser = parserFactory.newSAXParser()

            return DownloadUtils.downloadStringCached<List<String>>(
                metaDataUrl,
                name,
                force,
            ) { input: String? ->
                try {
                    val handler = ForgeVersionListHandler()
                    saxParser.parse(InputSource(StringReader(input)), handler)
                    return@downloadStringCached handler.versions
                    // IOException is present here StringReader throws it only if the parser called close()
                    // sooner than needed, which is a parser issue and not an I/O one
                } catch (e: SAXException) {
                    throw DownloadUtils.ParseException(e)
                } catch (e: IOException) {
                    throw DownloadUtils.ParseException(e)
                }
            }
        }

        @JvmStatic
        @Throws(Exception::class)
        fun downloadNeoForgeVersions(force: Boolean): List<String> {
            return downloadVersions(NEOFORGE_METADATA_URL, "neoforge_versions", force)
        }

        @JvmStatic
        @Throws(Exception::class)
        fun downloadNeoForgedForgeVersions(force: Boolean): List<String> {
            return downloadVersions(NEOFORGED_FORGE_METADATA_URL, "neoforged_forge_versions", force)
        }

        @JvmStatic
        fun getNeoForgeInstallerUrl(version: String?): String {
            return String.format(NEOFORGE_INSTALLER_URL, version)
        }

        @JvmStatic
        fun getNeoForgedForgeInstallerUrl(version: String?): String {
            return String.format(NEOFORGED_FORGE_INSTALLER_URL, version)
        }

        @JvmStatic
        fun formatGameVersion(neoForgeVersion: String): String {
            return when {
                neoForgeVersion.contains("1.20.1") -> "1.20.1"
                //暂时认为0开头代表特殊版本
                neoForgeVersion.startsWith("0.") -> {
                    //特殊版本
                    val versionPart = neoForgeVersion.replace("0.", "").substringBefore("-")
                    //"25w14craftmine.3" -> "25w14craftmine"
                    versionPart.substringBeforeLast(".")
                }
                else -> {
                    val version = when {
                        neoForgeVersion.contains("1.20.1") -> {
                            val versionPart = neoForgeVersion.replace("1.20.1-", "")
                            ForgeBuildVersion.parse("19.$versionPart")
                        }
                        else -> ForgeBuildVersion.parse(neoForgeVersion.substringBefore("-"))
                    }
                    buildString {
                        append("1.").append(version.major)
                        if (version.minor != 0) append(".").append(version.minor)
                    }
                }
            }
        }
    }
}