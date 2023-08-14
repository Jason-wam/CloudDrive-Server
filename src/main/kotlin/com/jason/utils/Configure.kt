package com.jason.utils

import org.json.JSONObject
import java.io.File

object Configure {
    private val tmpDir: File = File(System.getProperty("java.io.tmpdir"))
    val userDir = System.getProperty("user.dir")
    val cacheDir: File = File(tmpDir, "VirtualDrive").also { it.mkdirs() }
    var thumbDir: File = File(cacheDir, "thumbnail").also { it.mkdirs() }
    private val obj: JSONObject

    init {
        val configure = File(userDir, "configure.json")
        if (configure.exists()) {
            obj = JSONObject(configure.readText())
        } else {
            obj = JSONObject()
            obj.put("ffmpeg", "ffmpeg")
            obj.put("ffProbe", "ffprobe")
            obj.put("rootDir", "./VirtualDrive")
            obj.put("countDirSize", false)
            obj.put("mountedDirs", listOf("./VirtualDrive"))
            configure.createNewFile()
            configure.writeText(obj.toString(2))
        }
    }

    val ffmpeg: String by lazy {
        obj.optString("ffmpeg", "ffmpeg").let {
            if (it.startsWith("./")) {
                File(userDir, it.removePrefix("./")).absolutePath
            } else {
                it
            }
        }
    }

    val ffProbe: String by lazy {
        obj.optString("ffProbe", "ffprobe").let {
            if (it.startsWith("./")) {
                File(userDir, it.removePrefix("./")).absolutePath
            } else {
                it
            }
        }
    }

    val rootDir: File by lazy {
        obj.optString("rootDir", "./VirtualDrive").let {
            if (it.startsWith("./")) {
                File(userDir, it.removePrefix("./"))
            } else {
                File(it)
            }
        }
    }

    val mountedDirs: List<File> by lazy {
        ArrayList<File>().apply {
            obj.optJSONArray("mountedDirs")?.let { array ->
                for (i in 0 until array.length()) {
                    val dir = array.getString(i)
                    if (dir.startsWith("./")) {
                        add(File(userDir, dir.removePrefix("./")))
                    } else {
                        add(File(dir))
                    }
                }
            }
        }
    }

    val countDirSize: Boolean by lazy {
        obj.optBoolean("countDirSize", false)
    }
}