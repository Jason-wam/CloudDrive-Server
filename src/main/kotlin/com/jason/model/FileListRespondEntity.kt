package com.jason.model

import kotlinx.serialization.Serializable

@Serializable
data class FileListRespondEntity(val hash: String, val name: String, val path: String, val list: List<FileEntity>)