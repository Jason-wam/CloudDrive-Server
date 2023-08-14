package com.jason.utils

import java.io.File

object FileType {
    private val apkRegex by lazy {
        "^.+\\.(apk|xapk)\$".toRegex(RegexOption.IGNORE_CASE)
    }
    private val pptRegex by lazy {
        "^.+\\.(ppt|pps|pptx)\$".toRegex(RegexOption.IGNORE_CASE)
    }
    private val excelRegex by lazy {
        "^.+\\.(xls|xlsx)\$".toRegex(RegexOption.IGNORE_CASE)
    }
    private val torrentRegex by lazy {
        "^.+\\.(torrent)\$".toRegex(RegexOption.IGNORE_CASE)
    }
    private val fontRegex by lazy {
        "^.+\\.(ttf|otf|eot|font|ttc|woff)\$".toRegex(RegexOption.IGNORE_CASE)
    }
    private val databaseRegex by lazy {
        "^.+\\.(mdb|mdf|db|dbf|wdb|sql)\$".toRegex(RegexOption.IGNORE_CASE)
    }
    private val webRegex by lazy {
        "^.+\\.(html|htm|url|php|jsp|css|js)\$".toRegex(RegexOption.IGNORE_CASE)
    }
    private val exeRegex by lazy {
        "^.+\\.(exe|cmd|bat|reg|vds|shell|com|dll|sys|dmg|app)\$".toRegex(
            RegexOption.IGNORE_CASE
        )
    }
    private val wordRegex by lazy {
        "^.+\\.(docx|dotx|dox|wps|dot|wpt|docm|dotm|doc|rtf)\$".toRegex(
            RegexOption.IGNORE_CASE
        )
    }
    private val textRegex by lazy {
        "^.+\\.(txt|text|md|xml|ini|log|csv|json|lrc|yml|markdown)\$".toRegex(
            RegexOption.IGNORE_CASE
        )
    }
    private val audioRegex by lazy {
        "^.+\\.(aac|ac3|amr|m4a|mid|midi|mp3|ogg|wav|wma|wv|flac|ape)\$".toRegex(
            RegexOption.IGNORE_CASE
        )
    }
    private val videoRegex by lazy {
        "^.+\\.(mp4|m4v|mkv|avi|wm|wmv|wmp|3g2|3gp|ogv|mpg|mpeg|mov|mqv|webm|asf|rm|rmvb|ts|vob|m2t|m2ts)\$".toRegex(
            RegexOption.IGNORE_CASE
        )
    }
    private val imageRegex by lazy {
        "^.+\\.(xpm|png|jpg|jxl|jp2|jpf|jxs|gif|webp|tiff|bmp|ico|djvu|bpg|dwg|icns|heic|heif|hdr|xcf|pat|gbr|glb|avif|jxr|svg|jpeg)\$".toRegex(
            RegexOption.IGNORE_CASE
        )
    }
    private val compressRegex by lazy {
        "^.+\\.(rar|zip|7z|gz|lz|xz|apz|ar|bz|jar|car|cbz|cbr|dar|cpgz|000|001|ace|tar|tgz|txz)\$".toRegex(
            RegexOption.IGNORE_CASE
        )
    }

    enum class Media {
        VIDEO, IMAGE, AUDIO, COMPRESS, PPT, TEXT, WORD, EXCEL, APPLICATION, DATABASE, TORRENT, EXE, WEB,
        FONT, FOLDER, DOCUMENTS, UNKNOWN
    }

    fun getMediaType(fileName: String): Media {
        return when {
            fileName.matches(exeRegex) -> Media.EXE
            fileName.matches(webRegex) -> Media.WEB
            fileName.matches(fontRegex) -> Media.FONT
            fileName.matches(pptRegex) -> Media.PPT
            fileName.matches(textRegex) -> Media.TEXT
            fileName.matches(wordRegex) -> Media.WORD
            fileName.matches(audioRegex) -> Media.AUDIO
            fileName.matches(videoRegex) -> Media.VIDEO
            fileName.matches(imageRegex) -> Media.IMAGE
            fileName.matches(excelRegex) -> Media.EXCEL
            fileName.matches(apkRegex) -> Media.APPLICATION
            fileName.matches(torrentRegex) -> Media.TORRENT
            fileName.matches(compressRegex) -> Media.COMPRESS
            fileName.matches(databaseRegex) -> Media.DATABASE
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