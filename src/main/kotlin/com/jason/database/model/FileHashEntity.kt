package com.jason.database.model

import kotlinx.serialization.Serializable

@Serializable
class FileHashEntity {
    var id: String = ""
    var path: String = ""
    var hash: String = ""
    var timestamp: Long = 0
}