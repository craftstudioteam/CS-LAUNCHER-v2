package com.craftstudio.launcher.feature.unpack

import java.io.FilterInputStream
import java.io.InputStream

class ProgressInputStream(
    inputStream: InputStream,
    private val totalBytes: Long,
    private val baseOffset: Long = 0L,
    private val onProgress: (cumulativeBytes: Long) -> Unit
) : FilterInputStream(inputStream) {
    private var readBytes: Long = 0

    override fun read(): Int {
        val b = `in`.read()
        if (b >= 0) {
            readBytes++
            onProgress(baseOffset + readBytes)
        }
        return b
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val n = `in`.read(b, off, len)
        if (n > 0) {
            readBytes += n
            onProgress(baseOffset + readBytes)
        }
        return n
    }

    override fun read(b: ByteArray): Int {
        val n = `in`.read(b)
        if (n > 0) {
            readBytes += n
            onProgress(baseOffset + readBytes)
        }
        return n
    }
}
