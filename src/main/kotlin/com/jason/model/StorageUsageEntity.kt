package com.jason.model

import kotlinx.serialization.Serializable

@Serializable
data class StorageUsageEntity(
    val code: Int,
    val usedStorage: Long,
    val totalStorage: Long,
    val usedStorageText: String,
    val totalStorageText: String,
    val selfUsedStorage: Long,
    val selfUsedStorageText: String
)
