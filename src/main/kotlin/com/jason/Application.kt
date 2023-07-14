package com.jason

import com.jason.database.DatabaseFactory
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.jason.plugins.*
import com.jason.utils.*
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

fun main() {
    Configure.init()
    DatabaseFactory.init()

    println(File("D:\\VirtualDrive\\Fc2 PPV 2223919.mp4").isSymlink())

    verifyFileMd5()
    println()
    println("启动服务器...")
    embeddedServer(Netty, port = 8820, host = "0.0.0.0", module = Application::module).start(wait = true)
}

/**
 * 校验文件
 */
fun verifyFileMd5() = runBlocking {
    println("正在枚举待校验文件...")

    addFileIndex(Configure.rootDir)
    Configure.rootDir.allChildren().run {
        forEachIndexed { index, file ->
            print(
                "\r正在建立文件索引: $index/${size - 1} .."
            )
            addFileIndex(file)
        }
    }
}

suspend fun addFileIndex(file:File){
    try {
        if (DatabaseFactory.fileHashDao.isExist(file.absolutePath).not()) {
            if (file.isFile) {
                DatabaseFactory.fileHashDao.put(file.absolutePath, file.createSketchedMD5String())
            } else {
                DatabaseFactory.fileHashDao.put(file.absolutePath, file.absolutePath.toMd5String())
            }
        }
    } catch (ignore: Exception) {
    }
}


fun Application.module() {
    configureHTTP()
    configureSerialization()
    configureRouting()
}
