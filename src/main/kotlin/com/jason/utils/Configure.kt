package com.jason.utils

import java.io.File
import java.util.*

object Configure {
    val tmpDir: File = File(System.getProperty("java.io.tmpdir"))
    val userDir: File = File(System.getProperty("user.dir"))
    val cacheDir: File = File(tmpDir, "VirtualDrive").also { it.mkdirs() }
    var thumbDir: File = File(cacheDir, "thumbnail").also { it.mkdirs() }
    val properties by lazy {
        Properties().apply {
            val configure = File(userDir, "configure.xml")
            if (configure.exists()) {
                configure.inputStream().use {
                    loadFromXML(it)
                }
            } else {
                setProperty("ffmpeg", "ffmpeg")
                setProperty("ffprobe", "ffprobe")
                setProperty("rootDir", "%VirtualDrive")
                storeToXML(configure.outputStream(), "虚拟云盘配置文件")
            }
        }
    }

    val ffmpeg: String by lazy {
        properties.getProperty("ffmpeg", "ffmpeg")
    }

    val ffProbe: String by lazy {
        properties.getProperty("ffprobe", "ffprobe")
    }

    val rootDir: File by lazy {
        properties.getProperty("rootDir", "%VirtualDrive").let {
            if (it.startsWith("%")) {
                File(userDir, it.removePrefix("%"))
            } else {
                File(it)
            }
        }
    }
}