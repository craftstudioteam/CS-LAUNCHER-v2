package com.craftstudio.launcher.feature.runtime

import android.os.Build
import com.craftstudio.launcher.utils.path.PathManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class JavaRuntimeDownloader {
    fun isRuntimeInstalled(version: Int): Boolean {
        val runtimeName = "Internal-$version"
        val runtimeDir = File(PathManager.DIR_MULTIRT_HOME, runtimeName)
        return File(runtimeDir, "bin/java").exists()
    }

    suspend fun ensureRuntime(version: Int, onProgress: (Int, String) -> Unit) {
        val runtimeName = "Internal-$version"
        val runtimeDir = File(PathManager.DIR_MULTIRT_HOME, runtimeName)
        val javaBin = File(runtimeDir, "bin/java")
        if (javaBin.exists()) return

        withContext(Dispatchers.IO) {
            runtimeDir.mkdirs()
            val arch = resolveArch()
            val url = URL("https://api.adoptium.net/v3/binary/latest/$version/ga/linux/$arch/jdk/hotspot/normal/eclipse?project=jdk")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val total = connection.contentLengthLong.takeIf { it > 0 } ?: -1
            val tmpFile = File(runtimeDir, "runtime.tar.gz")
            connection.inputStream.use { input ->
                FileOutputStream(tmpFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    var downloaded: Long = 0
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) {
                            val pct = (downloaded * 90 / total).toInt().coerceIn(0, 90)
                            onProgress(pct, "Downloading Java $version...")
                        }
                    }
                }
            }

            onProgress(90, "Extracting Java $version...")
            extractTarGz(tmpFile, runtimeDir)
            tmpFile.delete()

            val binJava = File(runtimeDir, "bin/java")
            if (binJava.exists()) {
                binJava.setExecutable(true, false)
            }
            onProgress(100, "Java $version ready")
        }
    }

    private fun extractTarGz(source: File, destDir: File) {
        FileInputStream(source).use { fis ->
            GzipCompressorInputStream(fis).use { gis ->
                TarArchiveInputStream(gis).use { tis ->
                    var entry = tis.nextTarEntry
                    while (entry != null) {
                        val name = entry.name
                        val trimmedName = name.substringAfter('/', name)
                        if (trimmedName.isNotEmpty()) {
                            val outFile = File(destDir, trimmedName)
                            if (entry.isDirectory) {
                                outFile.mkdirs()
                            } else {
                                outFile.parentFile?.mkdirs()
                                FileOutputStream(outFile).use { output ->
                                    tis.copyTo(output)
                                }
                            }
                        }
                        entry = tis.nextTarEntry
                    }
                }
            }
        }
    }

    private fun resolveArch(): String {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        return when {
            abi.contains("arm64") -> "aarch64"
            abi.contains("armeabi") -> "arm"
            abi.contains("x86_64") -> "x86_64"
            abi.contains("x86") -> "x86"
            else -> "aarch64"
        }
    }
}
