package com.github.m5rian.hodaka

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URL
import java.util.concurrent.Executors

object Utilities {
    val EXECUTOR_SERVICE = Executors.newScheduledThreadPool(1)
    fun urlToByteArray(url: String?): ByteArray {
        var byteArray = ByteArray(0)
        try {
            val uri = URL(url)
            val baos = ByteArrayOutputStream()
            try {
                uri.openStream().use { `is` ->
                    val byteChunk = ByteArray(4096) // Or whatever size you want to read in at a time.
                    var n: Int
                    while (`is`.read(byteChunk).also { n = it } > 0) {
                        baos.write(byteChunk, 0, n)
                    }
                    byteArray = baos.toByteArray()
                }
            } catch (e: IOException) {
                System.err.printf("Failed while reading bytes from %s: %s", uri.toExternalForm(), e.message)
                e.printStackTrace()
            }
        } catch (e: IOException) {
            println("bruh")
            e.printStackTrace()
        }
        return byteArray
    }
}