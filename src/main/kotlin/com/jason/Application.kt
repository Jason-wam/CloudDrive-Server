package com.jason

import com.jason.database.DatabaseFactory
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.jason.plugins.*
import com.jason.utils.*
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Paths

fun main() {
    Configure.init()
    DatabaseFactory.init()

    runBlocking {
        FileIndexer.index2()
    }
    println()
    println("启动服务器...")
    embeddedServer(Netty, port = 8820, host = "0.0.0.0", module = Application::module).start(wait = true)
}


fun Application.module() {
    configureHTTP()
    configureSerialization()
    configureRouting()
}
