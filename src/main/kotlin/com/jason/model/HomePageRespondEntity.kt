package com.jason.model

import kotlinx.serialization.Serializable

@Serializable
data class HomePageRespondEntity(val mountedDirs: List<MountedDirEntity>, val recentFiles: List<FileEntity>)