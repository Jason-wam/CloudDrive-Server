package com.jason

import com.jason.database.DatabaseFactory
import com.jason.plugins.configureHTTP
import com.jason.plugins.configureRouting
import com.jason.utils.Configure
import com.jason.utils.FileIndexer
import com.jason.utils.NetworkUtils
import com.jason.utils.ffmpeg.Encoder
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

fun main() {
    DatabaseFactory.init()

    LoggerFactory.getLogger("Configure").info("ffmpeg = ${Configure.ffmpeg}")
    LoggerFactory.getLogger("Configure").info("ffProbe = ${Configure.ffProbe}")
    LoggerFactory.getLogger("Configure")
        .info("version = ${Encoder(Configure.ffmpeg).param("-version").execute().lines().first()}")
    LoggerFactory.getLogger("Configure").info("TempDir = ${Configure.cacheDir}")

    runBlocking {
        FileIndexer.indexFiles()
    }


    NetworkUtils.getLocalIPAddresses().forEach {
        LoggerFactory.getLogger("Main").info("本机IP：$it:8820")
    }
    LoggerFactory.getLogger("Main").info("登录密码：${Configure.password}")
    LoggerFactory.getLogger("Main").info("启动服务器...")
    embeddedServer(Netty, port = 8820, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    configureHTTP()
    configureRouting()
}
