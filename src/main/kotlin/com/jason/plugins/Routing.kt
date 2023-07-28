package com.jason.plugins

import com.jason.database.DatabaseFactory
import com.jason.model.CodeMessageRespondEntity
import com.jason.model.FileListRespondEntity
import com.jason.model.toFileEntities
import com.jason.utils.Configure
import com.jason.utils.FileIndexer
import com.jason.utils.FileType
import com.jason.utils.ListSort
import com.jason.utils.extension.*
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

                call.respond(
                    FileListRespondEntity(
                        DatabaseFactory.fileHashDao.getHash(Configure.rootDir.absolutePath),
                        Configure.rootDir.absolutePath,
                        Configure.rootDir.absolutePath,
                        Configure.rootDir.children.filter { if (showHidden) true else it.isHidden.not() }
                            .toFileEntities(Configure.rootDir.absolutePath, sort),
                        Configure.rootDir.toNavigation()
                    )
                )
            } else {
                val path = DatabaseFactory.fileHashDao.getPath(hash).find { File(it).exists() }.orEmpty()
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
                                    .toFileEntities(file.absolutePath, sort),
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
            val size = (call.parameters["size"] ?: "-1").toInt()

            if (hash.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "BadRequest，hash不得为空！")
            } else {
                val path = DatabaseFactory.fileHashDao.getPath(hash).find { File(it).exists() }.orEmpty()
                if (path.isBlank()) {
                    call.respond(HttpStatusCode.NotFound, "NotFound，未查找到指定文件路径！")
                } else {
                    val file = File(path)
                    val thumbnail = if (isGif) file.createGif(size) else file.createThumbnail(size)
                    if (thumbnail != null) {
                        call.setContentDisposition(thumbnail.name)
                        call.respondFile(thumbnail)
                    } else {
                        LoggerFactory.getLogger("Encoder").error("thumbnail创建未成功！")
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
                val path = DatabaseFactory.fileHashDao.getPath(hash).find { File(it).exists() }.orEmpty()
                if (path.isBlank()) {
                    call.respond(HttpStatusCode.NotFound, "NotFound，未查找到指定文件路径！")
                } else {
                    val file = File(path)
                    if (file.exists()) {
                        call.setContentDisposition(file.name)
                        call.setContentLength(file.length())
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
                        DatabaseFactory.fileHashDao.getPath(hash).find { File(it).exists() }.orEmpty()
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

        get("/delete") {
            val hash = call.parameters["hash"]
            val childHash = call.parameters["childHash"]
            if (hash.isNullOrBlank()) {
                call.respond(CodeMessageRespondEntity(403, "BadRequest，hash不得为空！"))
                return@get
            }
            if (childHash.isNullOrBlank()) {
                call.respond(CodeMessageRespondEntity(403, "BadRequest，childHash不得为空！"))
                return@get
            }

            val parent = DatabaseFactory.fileHashDao.getPath(hash).let {
                if (it.isEmpty()) "" else it.first()
            }

            val path = DatabaseFactory.fileHashDao.getPath(childHash, parent)
            if (path.isBlank()) {
                call.respond(CodeMessageRespondEntity(404, "NotFound，未查找到指定文件路径！"))
            } else {
                val filePath = File(path)
                if (filePath.isDirectory) {
                    LoggerFactory.getLogger("Delete").info("删除文件夹：{}", filePath.absolutePath)
                    if (filePath.deleteRecursively()) {
                        DatabaseFactory.fileHashDao.delete(path)
                        call.respond(CodeMessageRespondEntity(200, "删除成功！"))
                    } else {
                        call.respond(CodeMessageRespondEntity(500, "删除失败！"))
                    }
                } else {
                    LoggerFactory.getLogger("Delete").info("删除文件：{}", filePath.absolutePath)
                    if (filePath.delete()) {
                        DatabaseFactory.fileHashDao.delete(path)
                        call.respond(CodeMessageRespondEntity(200, "删除成功！"))
                    } else {
                        call.respond(CodeMessageRespondEntity(500, "删除失败！"))
                    }
                }
            }
        }

        get("/flashTransfer") {
            val hash = call.parameters["hash"]
            val fileHash = call.parameters["fileHash"]
            val fileName = call.parameters["fileName"].orEmpty()

            if (hash.isNullOrBlank()) {
                call.respond(CodeMessageRespondEntity(403, "BadRequest，hash不得为空！"))
                return@get
            }

            if (fileHash.isNullOrBlank()) {
                call.respond(CodeMessageRespondEntity(403, "BadRequest，childHash不得为空！"))
                return@get
            }

            val path = if (hash == "%root") {
                Configure.rootDir.absolutePath
            } else {
                DatabaseFactory.fileHashDao.getPath(hash).find { File(it).exists() }.orEmpty()
            }

            if (path.isBlank() || File(path).exists().not()) {
                call.respond(CodeMessageRespondEntity(404, "NotFound，未查找到指定文件路径！"))
                return@get
            }

            val filePath = DatabaseFactory.fileHashDao.getPath(fileHash).find { File(it).exists() }.orEmpty()
            if (filePath.isBlank()) {
                call.respond(CodeMessageRespondEntity(404, "文件闪传失败，不存在文件索引！"))
                return@get
            }

            val originalFile = File(filePath) //已存在的原始文件路径
            if (originalFile.exists()) {
                val createLink = File(path, fileName.ifBlank { originalFile.name })
                if (createLink.exists().not()) {//创建一个符号链接，防止空间占用
                    originalFile.createSymbolicLink(createLink)
                    LoggerFactory.getLogger("Upload").info(
                        "文件闪传完毕: ${originalFile.absolutePath} linked >> ${createLink.absolutePath}"
                    )
                } else {
                    LoggerFactory.getLogger("Upload").info(
                        "文件已经存在: ${originalFile.absolutePath} linked >> ${createLink.absolutePath}"
                    )
                }
                FileIndexer.addFileIndex(createLink, Configure.rootDir.absolutePath)
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
                DatabaseFactory.fileHashDao.getPath(hash).find { File(it).exists() }.orEmpty()
            }

            if (path.isBlank() || File(path).exists().not()) {
                LoggerFactory.getLogger("Upload").info("未查询到上传目录：$hash")
                call.respond(CodeMessageRespondEntity(404, "NotFound，未查找到指定文件路径！"))
                return@post
            }

            val folder = File(path)
            if (folder.exists().not()) {
                call.respond(CodeMessageRespondEntity(404, "NotFound，未查找到指定文件路径！"))
                return@post
            }

            if (folder.isDirectory.not()) {
                call.respond(CodeMessageRespondEntity(403, "指定的路径非目录类型！"))
                return@post
            }

            LoggerFactory.getLogger("Upload").info("正在接收文件到缓存...")
            val parts = call.receiveMultipart().readAllParts()
            if (parts.isEmpty()) {
                call.respond(CodeMessageRespondEntity(403, "未读接收到上传内容！"))
                return@post
            }

            val fileItemList = parts.filterIsInstance<PartData.FileItem>()
            if (fileItemList.isEmpty()) {
                call.respond(CodeMessageRespondEntity(403, "未读取到文件内容！"))
                return@post
            }

            fileItemList.forEach { part ->
                try {
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
                            FileIndexer.addFileIndex(file, Configure.rootDir.absolutePath)
                        } else {
                            FileIndexer.addFileIndex(
                                file,
                                fileHash,
                                Configure.rootDir.absolutePath
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    //丢弃缓存文件
                    part.dispose.invoke()
                }
            }
            call.respond(CodeMessageRespondEntity(200, "文件上传完毕！"))
        }

        get("/flashBackup") {
            val fileName = call.parameters["fileName"]
            val fileHash = call.parameters["fileHash"]
            val deviceName = call.parameters["deviceName"]

            if (fileName.isNullOrBlank()) {
                call.respond(CodeMessageRespondEntity(403, "BadRequest，childName不得为空！"))
                return@get
            }

            if (fileHash.isNullOrBlank()) {
                call.respond(CodeMessageRespondEntity(403, "BadRequest，fileHash不得为空！"))
                return@get
            }

            if (deviceName.isNullOrBlank()) {
                call.respond(CodeMessageRespondEntity(403, "BadRequest，deviceName不得为空！"))
                return@get
            }

            val folder = File(Configure.rootDir, "文件备份目录").also { it.mkdirs() }
            val folderDevice = File(folder, "来自${deviceName}的文件").also { it.mkdirs() }
            val childFolder = if (FileType.isImage(fileName)) {
                File(folderDevice, "图片").also { it.mkdirs() }
            } else if (FileType.isVideo(fileName)) {
                File(folderDevice, "视频").also { it.mkdirs() }
            } else if (FileType.isAudio(fileName)) {
                File(folderDevice, "音频").also { it.mkdirs() }
            } else {
                call.respond(CodeMessageRespondEntity(403, "BadRequest，不支持的文件类型！"))
                return@get
            }

            val path = DatabaseFactory.fileHashDao.getPath(fileHash).find { File(it).exists() }.orEmpty()
            if (path.isBlank()) {
                call.respond(CodeMessageRespondEntity(404, "文件闪传失败，不存在文件索引！"))
                return@get
            }

            val originalFile = File(path) //已存在的原始文件路径
            val createLink = File(childFolder, fileName)
            if (createLink.exists().not()) {//创建一个符号链接，防止空间占用
                originalFile.createSymbolicLink(createLink)
                LoggerFactory.getLogger("Upload").info(
                    "文件闪传完毕: ${originalFile.absolutePath} linked >> ${createLink.absolutePath}"
                )
            } else {
                LoggerFactory.getLogger("Upload").info(
                    "文件已经存在: ${originalFile.absolutePath} linked >> ${createLink.absolutePath}"
                )
            }
            FileIndexer.addFileIndex(createLink, Configure.rootDir.absolutePath)
            call.respond(CodeMessageRespondEntity(200, "文件闪传完毕！"))
        }

        post("/backup") {
            val fileName = call.parameters["fileName"]
            val fileHash = call.parameters["fileHash"]
            val deviceName = call.parameters["deviceName"]

            if (fileName.isNullOrBlank()) {
                call.respond(CodeMessageRespondEntity(403, "BadRequest，fileName不得为空！"))
                return@post
            }
            if (deviceName.isNullOrBlank()) {
                call.respond(CodeMessageRespondEntity(403, "BadRequest，deviceName不得为空！"))
                return@post
            }
            if (fileHash.isNullOrBlank()) {
                call.respond(CodeMessageRespondEntity(403, "BadRequest，fileHash不得为空！"))
                return@post
            }


            val folder = File(Configure.rootDir, "文件备份目录").also { it.mkdirs() }
            val folderDevice = File(folder, "来自${deviceName}的文件").also { it.mkdirs() }
            val childFolder = if (FileType.isImage(fileName)) {
                File(folderDevice, "图片").also { it.mkdirs() }
            } else if (FileType.isVideo(fileName)) {
                File(folderDevice, "视频").also { it.mkdirs() }
            } else if (FileType.isAudio(fileName)) {
                File(folderDevice, "音频").also { it.mkdirs() }
            } else {
                call.respond(CodeMessageRespondEntity(403, "BadRequest，不支持的文件类型！"))
                return@post
            }


            LoggerFactory.getLogger("Upload").info("正在接收 $fileName 到缓存...")
            val parts = call.receiveMultipart().readAllParts()
            if (parts.isEmpty()) {
                call.respond(CodeMessageRespondEntity(403, "未读接收到上传内容！"))
                return@post
            }

            val fileItemList = parts.filterIsInstance<PartData.FileItem>()
            if (fileItemList.isEmpty()) {
                call.respond(CodeMessageRespondEntity(403, "未读取到文件内容！"))
                return@post
            }

            fileItemList.forEach { part ->
                try {
                    LoggerFactory.getLogger("Upload").info(
                        "正在复制文件：$fileName 到 >> ${childFolder.absolutePath}"
                    )

                    part.streamProvider().use { input ->
                        val file = File(childFolder, fileName)
                        file.createNewFile()
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }

                        LoggerFactory.getLogger("Upload").info("文件复制完毕:{}", file.absolutePath)

                        if (fileHash.isBlank()) {
                            FileIndexer.addFileIndex(file, Configure.rootDir.absolutePath)
                        } else {
                            FileIndexer.addFileIndex(
                                file,
                                fileHash,
                                Configure.rootDir.absolutePath
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    //丢弃缓存文件
                    part.dispose.invoke()
                }
            }
            call.respond(CodeMessageRespondEntity(200, "文件上传完毕！"))
        }
    }
}


