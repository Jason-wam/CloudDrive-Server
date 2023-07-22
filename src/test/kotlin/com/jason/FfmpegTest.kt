package com.jason

import com.jason.plugins.configureRouting
import com.jason.utils.ffmpeg.MediaInfo
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.Test
import java.io.File

class FfmpegTest {
    @Test
    fun test() {
        MediaInfo.create("D:\\VirtualDrive\\The.Flash.2023.2160p.WEB-DL.DDP5.1.Atmos.H.265-Archie.mkv")
    }
}