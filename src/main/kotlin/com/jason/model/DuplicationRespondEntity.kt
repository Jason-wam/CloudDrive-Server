package com.jason.model

import kotlinx.serialization.Serializable

@Serializable
data class DuplicationRespondEntity(val list: List<DuplicationFileEntity>)