package com.jason.utils

/**
 * 转换为文件单位
 */
fun Long.toFileSizeString(): String {
    val fileSize = this
    val sizeMB = 1024L * 1024L
    val sizeGB = sizeMB * 1024L
    return if (fileSize >= sizeGB) {
        String.format("%.2f GB", fileSize.toFloat() / sizeGB.toFloat())
    } else {
        when {
            fileSize >= sizeMB -> {
                val size = fileSize.toFloat() / sizeMB.toFloat()
                if (size > 100.0f) {
                    String.format("%.0f MB", size)
                } else {
                    String.format("%.1f MB", size)
                }
            }
            fileSize >= 1024L -> {
                val size = fileSize.toFloat() / 1024L.toFloat()
                if (size > 100.0f) {
                    String.format("%.0f KB", size)
                } else {
                    String.format("%.1f KB", size)
                }
            }
            else -> {
                String.format("%d B", fileSize)
            }
        }
    }
}

inline val Double.KB: Long
    get() = run {
        return (this * 1024).toLong()
    }

inline val Double.MB: Long
    get() = run {
        return (this * 1024 * 1024).toLong()
    }

inline val Double.GB: Long
    get() = run {
        return (this * 1024 * 1024 * 1024).toLong()
    }

inline val Int.KB: Long
    get() = run {
        return (this * 1024).toLong()
    }

inline val Int.MB: Long
    get() = run {
        return (this * 1024 * 1024).toLong()
    }

inline val Int.GB: Long
    get() = run {
        return (this.toFloat() * 1024 * 1024 * 1024).toLong()
    }

inline val Float.KB: Long
    get() = run {
        return (this * 1024).toLong()
    }

inline val Float.MB: Long
    get() = run {
        return (this * 1024 * 1024).toLong()
    }

inline val Float.GB: Long
    get() = run {
        return (this * 1024 * 1024 * 1024).toLong()
    }
