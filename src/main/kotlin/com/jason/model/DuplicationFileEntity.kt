package com.jason.model

import com.jason.utils.FileType
import kotlinx.serialization.Serializable

@Serializable
data class DuplicationFileEntity(
    val name: String,
    val path: String,
    val hash: String,
    val size: Long,
    val date: Long,
    val type: FileType.Media
)
