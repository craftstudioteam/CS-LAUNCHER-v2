package com.craftstudio.launcher.utils

import java.io.File

object OptionsOptimizer {
    @JvmStatic
    fun forceMaxFPS(gameDir: File) {
        val optionsFile = File(gameDir, "options.txt")
        if (!optionsFile.exists()) {
            optionsFile.parentFile?.mkdirs()
            optionsFile.writeText("enableVsync:false\nmaxFps:260\n")
            return
        }

        val lines = optionsFile.readLines().toMutableList()
        var vsyncFound = false
        var maxFpsFound = false

        for (i in lines.indices) {
            if (lines[i].startsWith("enableVsync:")) {
                lines[i] = "enableVsync:false"
                vsyncFound = true
            } else if (lines[i].startsWith("maxFps:")) {
                lines[i] = "maxFps:260"
                maxFpsFound = true
            }
        }

        if (!vsyncFound) lines.add("enableVsync:false")
        if (!maxFpsFound) lines.add("maxFps:260")

        optionsFile.writeText(lines.joinToString("\n") + "\n")
    }
}
