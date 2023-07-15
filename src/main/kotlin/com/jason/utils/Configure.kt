package com.jason.utils

import org.json.JSONObject
import java.io.File

object Configure {
    val userDir: File = File(System.getProperty("user.dir"))
    var rootDir: File = File(userDir, "root")
    var cacheDir: File = File(userDir, "cache")
    var thumbDir: File = File(cacheDir, "thumbnail")
    var ffmpeg: String = "ffmpeg"
    var ffprobe: String = "ffprobe"

    fun init() {

        var configureJSON = ClassLoader.getSystemResource("configure.json").readText()

        val configure = File(userDir, "configure.json")
        if (configure.exists()) {
            configureJSON = configure.readText()
        } else {
            configure.createNewFile()
            configure.writeText(configureJSON)
        }

        println("Configure: $configureJSON")

        val obj = JSONObject(configureJSON)
        val root = obj.getString("rootDir")

        ffmpeg = obj.getString("ffmpeg")
        ffprobe = obj.getString("ffprobe")
        rootDir = if (root.startsWith("%")) {
            File(userDir, root.removePrefix("%"))
        } else {
            File(root)
        }

        rootDir.mkdirs()
        cacheDir = File(userDir, "cache").also { it.mkdirs() }
        thumbDir = File(cacheDir, "thumbnail").also { it.mkdirs() }
    }
}