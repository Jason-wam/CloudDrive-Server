package com.jason.model

import kotlinx.serialization.Serializable

@Serializable
data class CodeMessageRespondEntity(val code: Int, val message: String)