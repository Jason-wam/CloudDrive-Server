package com.jason.utils

import org.slf4j.LoggerFactory
import java.io.File

object MediaType {
    private val mimeTypeVideo = listOf(".mkv", ".asf", ".avi", ".wm", ".wmp", ".wmv", ".ram", ".rm", ".rmvb", ".mov", ".f4v", ".flv" , ".m2t", ".m2ts", ".mpeg", ".mpg", ".ts", ".m4v", ".mp4", ".3g2", ".3gp", ".vob", ".divx", ".mts", ".webm")
    private val mimeTypeAudio = listOf(".aac", ".ac3", ".amr", ".m4a", ".mid", ".midi", ".mp3", ".ogg", ".wav", ".wma", ".wv", ".flac", ".ape")
    private val mimeTypeImage = listOf(".png", ".jpg", ".jpeg", ".gif", ".tiff", ".psd", ".webp", ".bmp", ".tif", ".eps", ".cdr", ".jfif", ".iff", ".tga", ".pcd", ".mpt", ".raw")
    private val mimeTypeCompress = listOf(".rar", ".zip", ".7z", ".gz", ".apz", ".ar", ".bz", ".car", ".dar", ".cpgz", ".jar", ".000", ".001", ".ace", ".tar", ".tgz", ".txz")
    private val mimeTypeApplication = listOf(".apk", ".exe", ".hap", ".appinstaller", ".ipa", ".sys", ".msi", ".deb", ".rpm")
    private val mimeTypeWord = listOf(".docx", ".dotx", ".dox", ".wps", ".dot", ".wpt", ".docm", ".dotm", ".doc", ".rtf")
    private val mimeTypePPT = listOf(".ppt", ".pps", ".pptx")
    private val mimeTypeText = listOf(".txt", ".text", ".md", ".xml")
    private val mimeTypeExcel = listOf(".xls", ".xlsx", ".csv")
    private val mimeTypeDatabase = listOf(".mdb", ".mdf", ".db", ".dbf", ".wdb", ".sql")
    private val mimeTypeTorrent = listOf(".torrent")
    
    enum class Media {
        VIDEO, IMAGE, AUDIO, COMPRESS, PPT, TEXT, WORD, EXCEL, APPLICATION, DATABASE, TORRENT, UNKNOWN
    }
    
    fun getMediaType(fileName: String): Media {
        val extension = "." + fileName.substringAfterLast('.', "").lowercase()
        LoggerFactory.getLogger("HasImage").info("$fileName >> extension = $extension")
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
            mimeTypeApplication.contains(extension) -> Media.APPLICATION
            mimeTypeTorrent.contains(extension) -> Media.TORRENT
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
        return (getMediaType(file.name) == Media.AUDIO)
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