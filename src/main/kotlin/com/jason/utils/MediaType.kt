package com.jason.utils

import java.io.File


object MediaType {
    private val mimeTypeAPK = listOf(".apk", ".xapk")
    private val mimeTypeExcel = listOf(".xls", ".xlsx")
    private val mimeTypePPT = listOf(".ppt", ".pps", ".pptx")
    private val mimeTypeDatabase = listOf(".mdb", ".mdf", ".db", ".dbf", ".wdb", ".sql")
    private val mimeTypeTorrent = listOf(".torrent")
    private val mimeTypeText = listOf(
        ".txt", ".text", ".md", ".xml", ".ini", ".log", ".csv", ".json", ".lrc",".yml",
        ".markdown"
    )
    private val mimeTypeAudio = listOf(
        ".aac", ".ac3", ".amr", ".m4a", ".mid", ".midi", ".mp3", ".ogg", ".wav", ".wma", ".wv",
        ".flac", ".ape"
    )
    private val mimeTypeVideo = listOf(
        ".mp4", ".m4v", ".mkv", ".avi", ".wm", ".wmv", ".wmp", ".3g2", ".3gp", ".ogv", ".mpg",
        ".mpeg", ".mov", ".mqv", ".webm", ".asf", ".rm", ".rmvb", ".ts", ".vob", ".m2t", ".m2ts"
    )
    private val mimeTypeImage = listOf(
        ".xpm", ".png", ".jpg", ".jxl", ".jp2", ".jpf", ".jxs", ".gif", ".webp", ".tiff", ".bmp",
        ".ico", ".djvu", ".bpg", ".dwg", ".icns", ".heic", ".heif", ".hdr", ".xcf", ".pat", ".gbr",
        ".glb", ".avif", ".jxr", ".svg", ".jpeg"
    )
    private val mimeTypeCompress = listOf(
        ".rar", ".zip", ".7z", ".gz", ".lz", ".xz", ".apz", ".ar", ".bz", ".jar",
        ".car", ".cbz", ".cbr", ".dar", ".cpgz", ".jar", ".000", ".001", ".ace", ".tar", ".tgz",
        ".txz"
    )
    private val mimeTypeWord = listOf(
        ".docx", ".dotx", ".dox", ".wps", ".dot", ".wpt", ".docm", ".dotm", ".doc", ".rtf"
    )
    private val mimeTypeEXE = listOf(
        ".exe", ".cmd", ".bat", ".reg", ".vds", ".shell", ".com",".dll",".sys", ".dmg", ".app"
    )

    enum class Media {
        VIDEO, IMAGE, AUDIO, COMPRESS, PPT, TEXT, WORD, EXCEL, APPLICATION, DATABASE, TORRENT, EXE, UNKNOWN
    }

    fun getMediaType(fileName: String): Media {
        val extension = "." + fileName.substringAfterLast('.', "").lowercase()
        return when {
            mimeTypePPT.contains(extension) -> Media.PPT
            mimeTypeText.contains(extension) -> Media.TEXT
            mimeTypeWord.contains(extension) -> Media.WORD
            mimeTypeAudio.contains(extension) -> Media.AUDIO
            mimeTypeVideo.contains(extension) -> Media.VIDEO
            mimeTypeImage.contains(extension) -> Media.IMAGE
            mimeTypeExcel.contains(extension) -> Media.EXCEL
            mimeTypeCompress.contains(extension) -> Media.COMPRESS
            mimeTypeDatabase.contains(extension) -> Media.DATABASE
            mimeTypeAPK.contains(extension) -> Media.APPLICATION
            mimeTypeTorrent.contains(extension) -> Media.TORRENT
            mimeTypeEXE.contains(extension) -> Media.EXE
            else -> Media.UNKNOWN
        }
    }

    fun isMedia(file: File): Boolean {
        if (getMediaType(file.name) == Media.AUDIO) {
            return true
        }
        return getMediaType(file.name) == Media.VIDEO
    }

    fun isMedia(fileName: String): Boolean {
        if (getMediaType(fileName) == Media.AUDIO) {
            return true
        }
        return getMediaType(fileName) == Media.VIDEO
    }

    fun isMedia(type: Media): Boolean {
        if (type == Media.AUDIO) {
            return true
        }
        return type == Media.VIDEO
    }

    fun isVideo(file: File): Boolean {
        return getMediaType(file.name) == Media.VIDEO
    }

    fun isVideo(fileName: String): Boolean {
        return getMediaType(fileName) == Media.VIDEO
    }

    fun isAudio(file: File): Boolean {
        return getMediaType(file.name) == Media.AUDIO
    }

    fun isAudio(fileName: String): Boolean {
        return getMediaType(fileName) == Media.AUDIO
    }

    fun isImage(file: File): Boolean {
        return getMediaType(file.name) == Media.IMAGE
    }

    fun isImage(fileName: String): Boolean {
        return getMediaType(fileName) == Media.IMAGE
    }

    fun isTorrent(file: File): Boolean {
        return getMediaType(file.name) == Media.TORRENT
    }

    fun isTorrent(fileName: String): Boolean {
        return getMediaType(fileName) == Media.TORRENT
    }
}