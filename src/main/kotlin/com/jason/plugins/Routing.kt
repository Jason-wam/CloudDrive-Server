package com.jason.plugins

import com.jason.database.DatabaseFactory
import com.jason.model.CodeMessageRespondEntity
import com.jason.model.FileListRespondEntity
import com.jason.model.toFileEntities
import com.jason.utils.*
import com.jason.utils.extension.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.flow.asFlow
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.io.use

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        get("/index") {
            FileIndexer.indexFiles()
            call.respond(CodeMessageRespondEntity(200, "刷新文件索引完毕！"))
        }

        get("/reindex") {
            FileIndexer.reindex()
            call.respond(CodeMessageRespondEntity(200, "重建文件索引完毕！"))
        }

        get("/scanDatabase") {
            FileIndexer.scanDatabaseRows()
            call.respond(CodeMessageRespondEntity(200, "数据库整理完毕！"))
        }

        get("/list") {
            val hash = call.parameters["hash"]
            val sort = call.parameters["sort"].let {
                ListSort.valueOf(it ?: ListSort.DATE.name)
            }

            val showHidden = (call.parameters["showHidden"] ?: "true").toBoolean()

            LoggerFactory.getLogger("List").info("list: hash = $hash,sort = $sort,showHidden = $showHidden ...")
            if (hash.isNullOrBlank() || hash == "%root") {
                FileIndexer.indexDirectory(Configure.rootDir)
                File.pathSeparator

                call.respond(
                    FileListRespondEntity(
                        DatabaseFactory.fileHashDao.getHash(Configure.rootDir.absolutePath),
                        Configure.rootDir.absolutePath,
                        Configure.rootDir.absolutePath,
                        Configure.rootDir.children.filter { if (showHidden) true else it.isHidden.not() }
                            .toFileEntities(sort),
                        Configure.rootDir.toNavigation()
                    )
                )
            } else {
                val path = DatabaseFactory.fileHashDao.getPath(hash)
                if (path.isBlank()) {
                    call.respond(CodeMessageRespondEntity(404, "NotFound，未查询到目录索引！"))
                } else {
                    val file = File(path)
                    if (file.exists().not()) {
                        call.respond(CodeMessageRespondEntity(404, "NotFound，文件路径不存在！"))
                    } else {
                        FileIndexer.indexDirectory(file)
                        call.respond(
                            FileListRespondEntity(
                                hash,
                                file.name,
                                file.absolutePath,
                                file.children.filter { if (showHidden) true else it.isHidden.not() }
                                    .toFileEntities(sort),
                                file.toNavigation()
                            )
                        )
                    }
                }
            }
        }

        get("/thumbnail") {
            val hash = call.parameters["hash"]
            val isGif = (call.parameters["isGif"] ?: "false").toBoolean()
            if (hash.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "BadRequest，hash不得为空！")
            } else {
                val path = DatabaseFactory.fileHashDao.getPath(hash)
                if (path.isBlank()) {
                    call.respond(HttpStatusCode.NotFound, "NotFound，未查找到指定文件路径！")
                } else {
                    val file = File(path)
                    val thumbnail = if (isGif) file.createGif() else file.createThumbnail()
                    if (thumbnail != null) {
                        call.setContentDisposition(file.name)
                        call.respondFile(thumbnail)
                    } else {
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                }
            }
        }

        get("/file") {
            val hash = call.parameters["hash"]
            if (hash.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "BadRequest，hash不得为空！")
            } else {
                val path = DatabaseFactory.fileHashDao.getPath(hash)
                if (path.isBlank()) {
                    call.respond(HttpStatusCode.NotFound, "NotFound，未查找到指定文件路径！")
                } else {
                    val file = File(path)
                    if (file.exists()) {
                        call.setContentDisposition(file.name)
                        call.respondFile(file)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "NotFound，文件不存在！")
                    }
                }
            }
        }

        get("/createFolder") {
            val hash = call.parameters["hash"]
            val name = call.parameters["name"]
            if (hash.isNullOrBlank()) {
                call.respond(CodeMessageRespondEntity(400, "BadRequest，hash不得为空！"))
            } else {
                if (name.isNullOrBlank()) {
                    call.respond(CodeMessageRespondEntity(400, "文件夹名称不得为空！"))
                } else {
                    val path = if (hash == "%root") {
                        Configure.rootDir.absolutePath
                    } else {
                        DatabaseFactory.fileHashDao.getPath(hash)
                    }
                    if (path.isBlank()) {
                        call.respond(CodeMessageRespondEntity(404, "NotFound，未查找到指定文件路径！"))
                    } else {
                        val filePath = File(path, name)
                        if (filePath.mkdirs()) {
                            FileIndexer.indexDirectory(filePath)
                            call.respond(CodeMessageRespondEntity(200, "文件夹创建成功！"))
                        } else {
                            call.respond(CodeMessageRespondEntity(500, "文件夹创建失败！"))
                        }
                    }
                }
            }
        }

        delete("/delete") {
            val hash = call.parameters["hash"]
            if (hash.isNullOrBlank()) {
                call.respond(CodeMessageRespondEntity(400, "BadRequest，hash不得为空！"))
            } else {
                val path = if (hash == "%root") {
                    Configure.rootDir.absolutePath
                } else {
                    DatabaseFactory.fileHashDao.getPath(hash)
                }
                if (path.isBlank()) {
                    call.respond(CodeMessageRespondEntity(404, "NotFound，未查找到指定文件路径！"))
                } else {
                    val filePath = File(path)
                    if (filePath.isDirectory) {
                        if (filePath.deleteRecursively()) {
                            DatabaseFactory.fileHashDao.delete(path)
                            call.respond(CodeMessageRespondEntity(200, "删除成功！"))
                        } else {
                            call.respond(CodeMessageRespondEntity(500, "删除失败！"))
                        }
                    } else {
                        if (filePath.delete()) {
                            DatabaseFactory.fileHashDao.delete(path)
                            call.respond(CodeMessageRespondEntity(200, "删除成功！"))
                        } else {
                            call.respond(CodeMessageRespondEntity(500, "删除失败！"))
                        }
                    }
                }
            }
        }

        get("/flash") {
            val hash = call.parameters["hash"]
            val fileHash = call.parameters["fileHash"]
            val fileName = call.parameters["fileName"].orEmpty()

            if (hash.isNullOrBlank()) {
                call.respond(CodeMessageRespondEntity(403, "BadRequest，hash不得为空！"))
                return@get
            }

            if (fileHash.isNullOrBlank()) {
                call.respond(CodeMessageRespondEntity(403, "BadRequest，fileHash不得为空！"))
                return@get
            }

            val path = if (hash == "%root") {
                Configure.rootDir.absolutePath
            } else {
                DatabaseFactory.fileHashDao.getPath(hash)
            }

            if (path.isBlank() || File(path).exists().not()) {
                LoggerFactory.getLogger("Upload").info("未查询到上传目录：$hash")
                call.respond(CodeMessageRespondEntity(404, "NotFound，未查找到指定文件路径！"))
                return@get
            }

            val filePath = DatabaseFactory.fileHashDao.getPath(fileHash)
            if (filePath.isBlank()) {
                call.respond(CodeMessageRespondEntity(404, "文件闪传失败，不存在文件索引！"))
                return@get
            }

            val originalFile = File(filePath) //已存在的原始文件路径
            if (originalFile.exists()) {
                val createLink = File(path, fileName.ifBlank { originalFile.name })
                if (createLink.exists().not()) {//创建一个符号链接，防止空间占用
                    originalFile.createSymbolicLink(createLink)
                }
                LoggerFactory.getLogger("Upload")
                    .info("文件闪传完毕: ${originalFile.absolutePath} linked >> ${createLink.absolutePath}")

                DatabaseFactory.fileHashDao.put(createLink.absolutePath, fileHash, path)
                call.respond(CodeMessageRespondEntity(200, "文件闪传完毕！"))
            } else {
                call.respond(CodeMessageRespondEntity(400, "文件闪传失败，原始文件不存在！"))
            }
        }

        post("/upload") {
            val hash = call.parameters["hash"]
            val fileHash = call.parameters["fileHash"]
            if (hash.isNullOrBlank()) {
                call.respond(CodeMessageRespondEntity(403, "BadRequest，hash不得为空！"))
                return@post
            }

            val path = if (hash == "%root") {
                Configure.rootDir.absolutePath
            } else {
                DatabaseFactory.fileHashDao.getPath(hash)
            }

            if (path.isBlank() || File(path).exists().not()) {
                LoggerFactory.getLogger("Upload").info("未查询到上传目录：$hash")
                call.respond(CodeMessageRespondEntity(404, "NotFound，未查找到指定文件路径！"))
                return@post
            }

            val folder = File(path)
            if (folder.exists().not()) {
                call.respond(CodeMessageRespondEntity(404, "NotFound，未查找到指定文件路径！"))
            } else {
                if (folder.isDirectory.not()) {
                    call.respond(CodeMessageRespondEntity(403, "指定的路径非目录类型！"))
                } else {
                    LoggerFactory.getLogger("Upload").info("正在接收文件到缓存...")
                    val parts = call.receiveMultipart().readAllParts()
                    if (parts.isEmpty()) {
                        call.respond(CodeMessageRespondEntity(403, "未读取到上传的文件内容！"))
                    } else {
                        parts.forEach { part ->
                            if (part is PartData.FileItem) {
                                val fileName = part.originalFileName ?: "${System.currentTimeMillis()}.data"

                                LoggerFactory.getLogger("Upload").info(
                                    "正在复制文件：$fileName 到 >> ${folder.absolutePath}"
                                )
                                part.streamProvider().use { input ->
                                    val file = File(folder, fileName)
                                    file.createNewFile()
                                    file.outputStream().use { output ->
                                        input.copyTo(output)
                                    }

                                    LoggerFactory.getLogger("Upload").info("文件复制完毕 >> $file")

                                    if (fileHash.isNullOrBlank()) {
                                        FileIndexer.addFileIndex(file)
                                    } else {
                                        DatabaseFactory.fileHashDao.put(
                                            file.absolutePath,
                                            fileHash,
                                            folder.absolutePath
                                        )
                                    }
                                }
                                //丢弃缓存文件
                                part.dispose.invoke()
                            }
                        }
                    }
                    call.respond(CodeMessageRespondEntity(200, "文件上传完毕！"))
                }
            }
        }
    }
}


