package com.jason.plugins

import com.jason.database.DatabaseFactory
import com.jason.model.CodeMessageRespondEntity
import com.jason.model.FileListRespondEntity
import com.jason.model.findFirstMedia
import com.jason.model.toFileEntities
import com.jason.utils.*
import com.jason.utils.ffmpeg.Encoder
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import java.io.File

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        get("/index") {
            LoggerFactory.getLogger("FileVerify").info("正在刷新文件索引...")
            FileIndexer.index2()
            call.respond(CodeMessageRespondEntity(200, "刷新文件索引完毕！"))
        }

        get("/reindex") {
            LoggerFactory.getLogger("FileVerify").info("正在重建文件索引...")
            FileIndexer.reindex()
            call.respond(CodeMessageRespondEntity(200, "重建文件索引完毕！"))
        }

        get("/list") {
            val hash = call.parameters["hash"]
            val sort = call.parameters["sort"].let {
                ListSort.valueOf(it ?: ListSort.DATE.name)
            }

            val showHidden = (call.parameters["showHidden"] ?: "true").toBoolean()
            if (hash.isNullOrBlank() || hash == "%root") {
                if (showHidden) {
                    call.respond(
                        FileListRespondEntity(
                            DatabaseFactory.fileHashDao.getHash(Configure.rootDir.absolutePath),
                            Configure.rootDir.name,
                            Configure.rootDir.absolutePath,
                            Configure.rootDir.children.toFileEntities(sort)
                        )
                    )
                } else {
                    call.respond(
                        FileListRespondEntity(
                            DatabaseFactory.fileHashDao.getHash(Configure.rootDir.absolutePath),
                            Configure.rootDir.name,
                            Configure.rootDir.absolutePath,
                            Configure.rootDir.children.filter { it.isHidden.not() }.toFileEntities(sort)
                        )
                    )
                }
            } else {
                val path = DatabaseFactory.fileHashDao.getPath(hash).let { File(it) }
                if (path.exists().not()) {
                    call.respond(CodeMessageRespondEntity(404, "NotFound，文件路径不存在！"))
                } else {
                    if (showHidden) {
                        call.respond(
                            FileListRespondEntity(
                                hash, path.name, path.absolutePath, path.children.toFileEntities(sort)
                            )
                        )
                    } else {
                        call.respond(
                            FileListRespondEntity(
                                hash,
                                path.name,
                                path.absolutePath,
                                path.children.filter { it.isHidden.not() }.toFileEntities(sort)
                            )
                        )
                    }
                }
            }
        }

        //http://127.0.0.1:8080/thumbnail?hash=3d7ebf2ad60d31a04fa9f2ad9031fb33
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
                    val thumbnail = createThumbnail(path, isGif)
                    if (thumbnail != null) {
                        call.respondFile(thumbnail)
                    } else {
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                }
            }
        }

        //http://127.0.0.1:8080/file?path=D:/x.mp4
        //http://127.0.0.1:8080/file?hash=3d7ebf2ad60d31a04fa9f2ad9031fb33
        get("/file") {
            fun createRespond(path: String): Any {
                return with(File(path)) {
                    if (isFile.not()) {
                        CodeMessageRespondEntity(400, "BadRequest，错误的文件路径！")
                    } else {
                        if (exists()) {
                            this
                        } else {
                            CodeMessageRespondEntity(404, "NotFound，文件不存在！")
                        }
                    }
                }
            }

            val hash = call.parameters["hash"]
            if (hash.isNullOrBlank()) {
                call.respond(CodeMessageRespondEntity(400, "BadRequest，hash不得为空！"))
            } else {
                val path = DatabaseFactory.fileHashDao.getPath(hash)
                if (path.isBlank()) {
                    call.respond(CodeMessageRespondEntity(404, "NotFound，未查找到指定文件路径！"))
                } else {
                    val respond = createRespond(path)
                    if (respond is File) {
                        call.respondFile(respond)
                    } else {
                        call.respond(respond)
                    }
                }
            }
        }

        get("/newFolder") {
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
                            DatabaseFactory.fileHashDao.put(filePath.absolutePath, filePath.absolutePath.toMd5String())
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
            val fileName = call.parameters["fileName"] ?: ""

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

                DatabaseFactory.fileHashDao.put(createLink.absolutePath, fileHash)
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

            if (fileHash.isNullOrBlank()) {
                call.respond(CodeMessageRespondEntity(403, "BadRequest，fileHash不得为空！"))
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
                    val parts = call.receiveMultipart().readAllParts()
                    if (parts.isEmpty()) {
                        call.respond(CodeMessageRespondEntity(403, "未读取到上传的文件内容！"))
                    } else {
                        parts.forEach { part ->
                            if (part is PartData.FileItem) {
                                val fileName = part.originalFileName ?: "${System.currentTimeMillis()}.data"

                                LoggerFactory.getLogger("Upload")
                                    .info("正在接收文件：$fileName to >> ${folder.absolutePath}")

                                part.streamProvider.invoke().use { input ->
                                    val file = File(folder, fileName).also { it.createNewFile() }.also {
                                        LoggerFactory.getLogger("Upload").info("to >> $it")
                                    }
                                    file.outputStream().use { output ->
                                        input.copyTo(output)
                                        DatabaseFactory.fileHashDao.put(file.absolutePath, fileHash)
                                    }
                                }
                            }
                        }
                    }
                    call.respond(CodeMessageRespondEntity(200, "文件上传完毕！"))
                }
            }
        }
    }
}


suspend fun createThumbnail(path: String, isGif: Boolean = false): File? {
    LoggerFactory.getLogger("Thumbnail").info("createThumbnail >> $path")
    with(File(path)) {
        if (exists().not()) {
            LoggerFactory.getLogger("Thumbnail").info("file not exist >> $path")
            return null
        }

        if (isDirectory) {
            LoggerFactory.getLogger("Thumbnail").info("get image from children >> $absolutePath")
            val mediaFile = children.findFirstMedia()
            if (mediaFile != null) {
                return createThumbnail(mediaFile.absolutePath)
            } else { //如果不存在视频或图片则尝试从音频中读取专辑封面
                val audioFile = children.sortedByDescending { it.lastModified() }.find { file ->
                    MediaType.isAudio(file)
                } ?: return null

                return createThumbnail(audioFile.absolutePath)
            }
        } else {
            if (isGif) {
                if (name.endsWith(".gif", true)) {
                    LoggerFactory.getLogger("Thumbnail").info("return original gif >> $absolutePath")
                    return this
                } else {
                    if (MediaType.isVideo(this).not()) return null

                    LoggerFactory.getLogger("Thumbnail").info("get gif from video >> $absolutePath")
                    val image = File(Configure.thumbDir, "${path.toMd5String()}.gif")
                    val succeed = Encoder(Configure.ffmpeg).input(this).fps(10).t(10f).resize(720)
                        .threads(3)
                        .startAtHalfDuration(true).execute(image)
                    return if (succeed) image else null
                }
            } else {
                if (name.endsWith(".gif", true)) {
                    LoggerFactory.getLogger("Thumbnail").info("resize image from gif >> $absolutePath")
                    val image = File(Configure.thumbDir, "${path.toMd5String()}.gif")
                    val succeed = Encoder(Configure.ffmpeg).input(this).resize(320).execute(image)
                    return if (succeed) image else this
                } else {
                    if (MediaType.isVideo(this)) {
                        LoggerFactory.getLogger("Thumbnail").info("get image from video >> $absolutePath")
                        val image = File(Configure.thumbDir, "${path.toMd5String()}.jpg")
                        val succeed = Encoder(Configure.ffmpeg).input(this).param("-frames 1").resize(320)
                            .format("mjpeg").startAtHalfDuration(true).execute(image)
                        return if (succeed) image else null
                    } else if (MediaType.isImage(this)) {
                        LoggerFactory.getLogger("Thumbnail").info("resize image >> $absolutePath")
                        val image = File(Configure.thumbDir, "${path.toMd5String()}.jpg")
                        val succeed = Encoder(Configure.ffmpeg).input(this).resize(320).execute(image)
                        return if (succeed) image else this
                    } else if (MediaType.isAudio(this)) {
                        LoggerFactory.getLogger("Thumbnail").info("get image from audio >> $absolutePath")
                        val image = File(Configure.thumbDir, "${path.toMd5String()}.jpg")
                        val succeed = Encoder(Configure.ffmpeg).input(this).resize(320).execute(image)
                        return if (succeed) image else this
                    } else {
                        LoggerFactory.getLogger("Thumbnail").info("unsupported media type >> $absolutePath")
                        return null
                    }
                }
            }
        }
    }
}
