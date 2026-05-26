package com.craftstudio.launcher.utils

import android.content.Context
import android.widget.Toast
import com.craftstudio.launcher.R
import com.craftstudio.launcher.task.Task
import com.craftstudio.launcher.task.TaskExecutors
import com.craftstudio.launcher.utils.file.FileTools
import com.craftstudio.launcher.utils.path.PathManager
import org.apache.commons.io.FileUtils
import java.io.File

class CleanUpCache {
    companion object {
        private var isCleaning = false

        @JvmStatic
        fun start(context: Context) {
            if (isCleaning) return
            isCleaning = true

            var totalSize: Long = 0
            var fileCount = 0
            try {
                Task.runTask {
                    val list = PathManager.DIR_CACHE.listFiles()?.let {
                        PathManager.DIR_APP_CACHE.listFiles()?.let { it1 ->
                            getList(it, it1)
                        }
                    }

                    PathManager.FILE_VERSION_LIST.let {
                        val file = File(it)
                        if (file.exists()) list?.add(file)
                    }

                    list?.let{
                        for (file in list) {
                            ++fileCount
                            totalSize += FileUtils.sizeOf(file)
                            FileUtils.deleteQuietly(file)
                        }
                    }
                }.ended(TaskExecutors.getAndroidUI()) {
                    if (fileCount != 0) {
                        Toast.makeText(context,
                            context.getString(R.string.clear_up_cache_clean_up, FileTools.formatFileSize(totalSize)),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(context,
                            context.getString(R.string.clear_up_cache_not_found),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }.execute()
            } finally {
                isCleaning = false
            }
        }

        private fun getList(vararg filesArray: Array<File>): MutableList<File> {
            val filesList: MutableList<File> = ArrayList()
            for (fileArray in filesArray) {
                filesList.addAll(listOf(*fileArray))
            }

            return filesList
        }
    }
}
