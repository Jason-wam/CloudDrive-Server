package com.jason

import com.jason.database.DatabaseFactory
import com.jason.plugins.configureHTTP
import com.jason.plugins.configureRouting
import com.jason.plugins.configureSerialization
import com.jason.utils.Configure
import com.jason.utils.FileIndexer
import com.jason.utils.extension.MB
import com.jason.utils.extension.createSketchedMD5String
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.io.File

fun main() {
    DatabaseFactory.init()

    runBlocking {
        FileIndexer.indexFiles()
    }
    LoggerFactory.getLogger("Main").info("启动服务器...")
    embeddedServer(Netty, port = 8820, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    configureHTTP()
    configureSerialization()
    configureRouting()
}

fun testBlake2() {
    val time = System.currentTimeMillis()
    val file = File("D:/VirtualDrive/CloudMusic/BEYOND - 海阔天空.flac")
    val blake2 = file.createSketchedMD5String(20.MB)
    println("blake2 = $blake2")
    println("blake2 used:${System.currentTimeMillis() - time}")
}
