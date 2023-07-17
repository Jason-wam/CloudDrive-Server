package com.jason.model

import kotlinx.serialization.Serializable

@Serializable
data class FileNavigationEntity(val name: String, val hash: String)