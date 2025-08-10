package com.minapps.trackeditor.feature_map_editor.presentation.util

import java.io.InputStream

class CountingInputStream(private val inputStream: InputStream) : InputStream() {
    var bytesRead = 0L
        private set

    override fun read(): Int {
        val byte = inputStream.read()
        if (byte != -1) bytesRead++
        return byte
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val count = inputStream.read(b, off, len)
        if (count != -1) bytesRead += count
        return count
    }

    override fun close() {
        inputStream.close()
    }
}
