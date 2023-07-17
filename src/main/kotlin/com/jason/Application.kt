package com.jason

import com.jason.database.DatabaseFactory
import com.jason.plugins.configureHTTP
import com.jason.plugins.configureRouting
import com.jason.plugins.configureSerialization
import com.jason.utils.Configure
import com.jason.utils.FileIndexer
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

fun main() {
    Configure.init()
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
