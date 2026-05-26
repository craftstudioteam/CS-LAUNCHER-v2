package com.craftstudio.launcher.utils.file

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.EditText
import com.craftstudio.launcher.R
import com.craftstudio.launcher.task.Task
import com.craftstudio.launcher.ui.dialog.EditTextDialog
import com.craftstudio.launcher.ui.dialog.EditTextDialog.ConfirmListener
import com.craftstudio.launcher.utils.ZHTools
import com.craftstudio.launcher.Tools
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileInputStream
import java.io.FilenameFilter
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FileTools {
    companion object {
        const val INVALID_CHARACTERS_REGEX = "[\\\\/:*?\"<>|\\t\\n]"

        @JvmStatic
        fun mkdir(dir: File): Boolean {
            return dir.mkdir()
        }

        @JvmStatic
        fun mkdirs(dir: File): Boolean {
            return dir.mkdirs()
        }

        @JvmStatic
        fun copyFileInBackground(context: Context, fileUri: Uri, rootPath: String): File {
            val fileName = Tools.getFileName(context, fileUri)
            val outputFile = File(rootPath, fileName)
            return copyFileInBackground(context, fileUri, outputFile)
        }

        @JvmStatic
        fun copyFileInBackground(context: Context, fileUri: Uri, outputFile: File): File {
            context.contentResolver.openInputStream(fileUri).use { inputStream ->
                FileUtils.copyInputStreamToFile(inputStream, outputFile)
            }
            return outputFile
        }

        @JvmStatic
        fun ensureValidFilename(str: String): String =
            str.trim().replace(INVALID_CHARACTERS_REGEX.toRegex(), "-").run {
                if (length > 255) substring(0, 255)
                else this
            }

        @Throws(InvalidFilenameException::class)
        @JvmStatic
        fun checkFilenameValidity(str: String) {
            val illegalCharsRegex = INVALID_CHARACTERS_REGEX.toRegex()

            val illegalChars = illegalCharsRegex.findAll(str)
                .map { it.value }
                .distinct()
                .joinToString("")

            if (illegalChars.isNotEmpty()) {
                throw InvalidFilenameException("The filename contains illegal characters", illegalChars)
            }

            if (str.length > 255) {
                throw InvalidFilenameException("Invalid filename length", str.length)
            }
        }

        @JvmStatic
        fun isFilenameInvalid(
            str: String,
            containsIllegalCharacters: (illegalCharacters: String) -> Unit,
            isInvalidLength: (invalidLength: Int) -> Unit
        ): Boolean {
            try {
                checkFilenameValidity(str)
            } catch (e: InvalidFilenameException) {
                if (e.containsIllegalCharacters()) {
                    containsIllegalCharacters(e.illegalCharacters)
                    return true
                } else if (e.isInvalidLength) {
                    isInvalidLength(e.invalidLength)
                    return true
                }
            }
            return false
        }

        @JvmStatic
        fun isFilenameInvalid(editText: EditText): Boolean {
            val str = editText.text.toString()
            return isFilenameInvalid(
                str,
                { illegalCharacters ->
                    editText.error = editText.context.getString(R.string.generic_input_invalid_character, illegalCharacters)
                },
                { invalidLength ->
                    editText.error = editText.context.getString(R.string.file_invalid_length, invalidLength, 255)
                }
            )
        }

        @JvmStatic
        fun getLatestFile(folderPath: String?, modifyTime: Int): File? {
            if (folderPath == null) return null
            return getLatestFile(File(folderPath), modifyTime.toLong())
        }

        @JvmStatic
        fun getLatestFile(folder: File?, modifyTime: Long): File? {
            if (folder == null || !folder.isDirectory) {
                return null
            }

            val files = folder.listFiles(FilenameFilter { _: File?, name: String ->
                !name.startsWith(
                    "."
                )
            })
            if (files == null || files.isEmpty()) {
                return null
            }

            val fileList: List<File> = listOf(*files)
            fileList.sortedWith(Comparator.comparingLong { obj: File -> obj.lastModified() }
                .reversed())

            if (modifyTime > 0) {
                val difference =
                    (ZHTools.getCurrentTimeMillis() - fileList[0].lastModified()) / 1000 //转换为秒
                if (difference >= modifyTime) {
                    return null
                }
            }

            return fileList[0]
        }

        @JvmStatic
        fun shareFile(context: Context, file: File) {
            shareFile(context, file.name, file.absolutePath)
        }

        @JvmStatic
        fun shareFile(context: Context, fileName: String, filePath: String) {
            val contentUri = DocumentsContract.buildDocumentUri(
                context.getString(R.string.storageProviderAuthorities),
                filePath
            )

            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
            shareIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            shareIntent.setDataAndType(contentUri, "*/*")

            val sendIntent = Intent.createChooser(shareIntent, fileName)
            context.startActivity(sendIntent)
        }

        @JvmStatic
        @SuppressLint("UseCompatLoadingForDrawables")
        fun renameFileListener(context: Context, endTask: Task<*>?, file: File, suffix: String) {
            val fileParent = file.parent
            val fileName = file.name

            EditTextDialog.Builder(context)
                .setTitle(R.string.generic_rename)
                .setEditText(getFileNameWithoutExtension(fileName, suffix))
                .setAsRequired()
                .setConfirmListener(ConfirmListener { editBox, _ ->
                    val newName = editBox.text.toString()

                    if (isFilenameInvalid(editBox)) {
                        return@ConfirmListener false
                    }

                    if (fileName == newName) {
                        return@ConfirmListener true
                    }

                    val newFile = File(fileParent, newName + suffix)
                    if (newFile.exists()) {
                        editBox.error = context.getString(R.string.file_rename_exitis)
                        return@ConfirmListener false
                    }

                    val renamed = file.renameTo(newFile)
                    if (renamed) {
                        endTask?.execute()
                    }
                    true
                }).showDialog()
        }

        @JvmStatic
        @SuppressLint("UseCompatLoadingForDrawables")
        fun renameFileListener(context: Context, endTask: Task<*>?, file: File) {
            val fileParent = file.parent
            val fileName = file.name

            EditTextDialog.Builder(context)
                .setTitle(R.string.generic_rename)
                .setEditText(fileName)
                .setAsRequired()
                .setConfirmListener(ConfirmListener { editBox, _ ->
                    val newName = editBox.text.toString()

                    if (isFilenameInvalid(editBox)) {
                        return@ConfirmListener false
                    }

                    if (fileName == newName) {
                        return@ConfirmListener true
                    }

                    val newFile = File(fileParent, newName)
                    if (newFile.exists()) {
                        editBox.error = context.getString(R.string.file_rename_exitis)
                        return@ConfirmListener false
                    }

                    val renamed = renameFile(file, newFile)
                    if (renamed) {
                        endTask?.execute()
                    }
                    true
                }).showDialog()
        }

        @JvmStatic
        fun renameFile(origin: File, target: File): Boolean {
            return origin.renameTo(target)
        }

        @JvmStatic
        fun copyFile(file :File, target: File) {
            if (file.isFile) FileUtils.copyFile(file, target)
            else if (file.isDirectory) FileUtils.copyDirectory(file, target)
        }

        @JvmStatic
        fun moveFile(file :File, target: File) {
            if (file.isFile) FileUtils.moveFile(file, target)
            else if (file.isDirectory) FileUtils.moveDirectory(file, target)
        }

        @JvmStatic
        fun getFileNameWithoutExtension(fileName: String, fileExtension: String?): String {
            val dotIndex = if (fileExtension == null) {
                fileName.lastIndexOf('.')
            } else {
                fileName.lastIndexOf(fileExtension)
            }
            return if (dotIndex == -1) fileName else fileName.substring(0, dotIndex)
        }

        @JvmStatic
        fun getFileNameWithoutExtension(file: File): String = file.nameWithoutExtension

        @JvmStatic
        @SuppressLint("DefaultLocale")
        fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"

            val units = arrayOf("B", "KB", "MB", "GB")
            var unitIndex = 0
            var value = bytes.toDouble()
            //循环获取合适的单位
            while (value >= 1024 && unitIndex < units.size - 1) {
                value /= 1024.0
                unitIndex++
            }
            return String.format("%.2f %s", value, units[unitIndex])
        }

        @JvmStatic
        @Throws(IOException::class)
        fun zipDirectory(folder: File, parentPath: String, filter: (File) -> Boolean, zos: ZipOutputStream) {
            val files = folder.listFiles()?.filter(filter) ?: return
            for (file in files) {
                if (file.isDirectory) {
                    zipDirectory(file, parentPath + file.name + "/", filter, zos)
                } else {
                    zipFile(file, parentPath + file.name, zos)
                }
            }
        }

        @JvmStatic
        @Throws(IOException::class)
        fun zipFile(file: File, entryName: String, zos: ZipOutputStream) {
            FileInputStream(file).use { fis ->
                val zipEntry = ZipEntry(entryName)
                zipEntry.time = file.lastModified() //保留文件的修改时间
                zos.putNextEntry(zipEntry)

                val buffer = ByteArray(4096)
                var length: Int
                while ((fis.read(buffer).also { length = it }) >= 0) {
                    zos.write(buffer, 0, length)
                }
                zos.closeEntry()
            }
        }

        @JvmStatic
        @Throws(Exception::class)
        fun calculateFileHash(file: File, algorithm: String = "SHA-256"): String {
            return calculateFileHash(file.inputStream(), algorithm)
        }

        @JvmStatic
        @Throws(Exception::class)
        fun calculateFileHash(inputStream: InputStream, algorithm: String = "SHA-256"): String {
            val digest = MessageDigest.getInstance(algorithm)
            inputStream.use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            return digest.digest().toHex()
        }

        /**
         * 字节数组转十六进制字符串（高效实现）
         */
        private fun ByteArray.toHex(): String {
            val hexChars = "0123456789abcdef"
            return joinToString("") { byte ->
                "${hexChars[byte.toInt() shr 4 and 0x0F]}${hexChars[byte.toInt() and 0x0F]}"
            }
        }
    }
}
