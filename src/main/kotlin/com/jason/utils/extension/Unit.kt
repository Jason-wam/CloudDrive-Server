package com.jason.utils.extension

/**
 * 转换为文件单位
 */
fun Long.toFileSizeString(): String {
    if (this <= 0) return "0 B"
    val fileSize = this
    val sizes = listOf(1L, 1024L, 1024L * 1024L, 1024L * 1024L * 1024L, 1024L * 1024L * 1024L * 1024L)
    val names = listOf("B", "KB", "MB", "GB", "TB")

    return sizes.indexOfLast { fileSize >= it }.let {
        val size = fileSize.toFloat() / sizes[it].toFloat()
        if (size > 100.0f) {
            String.format("%.0f %s", size, names[it])
        } else {
            String.format("%.2f %s", size, names[it])
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

inline val Int.TB: Long
    get() = run {
        return (this.toFloat() * 1024 * 1024 * 1024 * 1024).toLong()
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

inline val Float.TB: Long
    get() = run {
        return (this * 1024 * 1024 * 1024 * 1024).toLong()
    }
