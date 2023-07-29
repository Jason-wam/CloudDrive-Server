package com.jason.model

import kotlinx.serialization.Serializable

@Serializable
data class SearchRespondEntity(
    val page: Int,
    val count: Int,
    val hasMore: Boolean,
    val list: List<FileEntity>
)