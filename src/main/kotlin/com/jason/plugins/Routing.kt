package com.jason.plugins

import com.jason.database.DatabaseFactory
import com.jason.database.table.FileIndexTable
import com.jason.model.*
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
            call.respondText("Wink")
        }

        get("/homePage") {
            val recentSize = (call.parameters["recentSize"] ?: "10").toInt()
            val showHidden = (call.parameters["showHidden"] ?: "false").toBoolean()

            LoggerFactory.getLogger("HomePage").info(
                "recentSize = $recentSize, showHidden = $showHidden"
            )

            val mountedDirs = ArrayList<MountedDirEntity>().apply {
                Configure.mountedDirs.forEach { dir ->
                    val usedStorage = dir.totalSpace - dir.freeSpace
                    val totalStorage = dir.totalSpace
                    val selfUsedStorage = DatabaseFactory.dbQuery {
                        DatabaseFactory.fileIndexDao.queryAll().filter {
                            !it[FileIndexTable.isDirectory]
                        }.sumOf {
                            it[FileIndexTable.size]
                        }
                    }
                    add(
                        MountedDirEntity(
                            dir.absolutePath.toMd5String(),
                            dir.absolutePath,
                            usedStorage,
                            totalStorage,
                            selfUsedStorage,
                            usedStorage.toFileSizeString(),
                            totalStorage.toFileSizeString(),
                            selfUsedStorage.toFileSizeString()
                        )
                    )
                }
            }

            call.respond(
                HomePageRespondEntity(
                    mountedDirs,
                    DatabaseFactory.fileIndexDao.recentFiles(recentSize).filter {
                        it.key.exists()
                    }.filter {
                        if (showHidden) true else it.key.isHidden.not()
                    }.toFileEntities(ListSort.DATE_DESC)
                )
            )
        }

        get("/duplication") {
            call.respond(DuplicationRespondEntity(DatabaseFactory.fileIndexDao.findDuplications()))
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
            val showHidden = (call.parameters["showHidden"] ?: "false").toBoolean()
            val sort = call.parameters["sort"].let {
                ListSort.valueOf(it ?: ListSort.DATE_DESC.name)
            }

            val hash = call.parameters["hash"]
            if (hash.isNullOrBlank()) {
                call.respond(CodeMessageRespondEntity(404, "目标目录Hash不得为空！"))
                return@get
            }

            LoggerFactory.getLogger("List").info("list: hash = $hash,sort = $sort,showHidden = $showHidden")

            val path = DatabaseFactory.fileIndexDao.getPath(hash).find { File(it).exists() }.orEmpty()
            if (path.isBlank()) {
                call.respond(CodeMessageRespondEntity(404, "NotFound，未查询到目录索引！"))
                return@get
            }

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

        get("/search") {
            val kw = call.parameters["kw"].orEmpty()
            val page = (call.parameters["page"] ?: "1").toInt()
            val size = (call.parameters["size"] ?: "100").toInt()
            val showHidden = (call.parameters["showHidden"] ?: "false").toBoolean()
            val sort = call.parameters["sort"].let {
                ListSort.valueOf(it ?: ListSort.DATE_DESC.name)
            }

            if (kw.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, "BadRequest，关键词不得为空！")
                return@get
            }

            LoggerFactory.getLogger("Search").info(
                "kw: $kw, page = $page, size = $size, showHidden = $showHidden"
            )

            val files = DatabaseFactory.fileIndexDao.search(kw, page, size, sort)
            if (files.isEmpty()) {
                call.respond(CodeMessageRespondEntity(404, "没有搜索到相关结果！"))
            } else {
                call.respond(
                    SearchRespondEntity(
                        page,
                        files.size,
                        files.size == size,
                        files.filter {
                            it.key.exists()
                        }.filter {
                            if (showHidden) true else it.key.isHidden.not()
                        }.toFileEntities(sort)
                    )
                )
            }
        }

        get("/searchType") {
            val type = call.parameters["type"]?.let { FileType.Media.valueOf(it) }
            val page = (call.parameters["page"] ?: "1").toInt()
            val size = (call.parameters["size"] ?: "100").toInt()
            val showHidden = (call.parameters["showHidden"] ?: "false").toBoolean()
            val sort = call.parameters["sort"].let {
                ListSort.valueOf(it ?: ListSort.DATE.name)
            }

            if (type == null) {
                call.respond(HttpStatusCode.BadRequest, "BadRequest，文件类型不得为空！")
                return@get
            }

            LoggerFactory.getLogger("SearchType").info(
                "type: $type, page = $page, size = $size, showHidden = $showHidden"
            )

            val files = DatabaseFactory.fileIndexDao.searchType(type.name, page, size, sort)
            if (files.isEmpty()) {
                call.respond(CodeMessageRespondEntity(404, "没有查找到相关结果！"))
            } else {
                call.respond(
                    SearchRespondEntity(
                        page,
                        files.size,
                        files.size == size,
                        files.filter {
                            it.key.exists()
                        }.filter {
                            if (showHidden) true else it.key.isHidden.not()
                        }.toFileEntities(ListSort.DATE_DESC)
                    )
                )
            }
        }

        get("/thumbnail") {
            val hash = call.parameters["hash"]
            val isGif = (call.parameters["isGif"] ?: "false").toBoolean()
            val size = (call.parameters["size"] ?: "-1").toInt()

            if (hash.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "BadRequest，hash不得为空！")
                return@get
            }

            val path = DatabaseFactory.fileIndexDao.getPath(hash).find { File(it).exists() }.orEmpty()
            if (path.isBlank()) {
                call.respond(HttpStatusCode.NotFound, "NotFound，未查找到指定文件路径！")
                return@get
            }

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

        get("/file") {
            val hash = call.parameters["hash"]
            if (hash.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "BadRequest，hash不得为空！")
                return@get
            }

            val path = DatabaseFactory.fileIndexDao.getPath(hash).find { File(it).exists() }.orEmpty()
            if (path.isBlank()) {
                call.respond(HttpStatusCode.NotFound, "NotFound，未查找到指定文件路径！")
                return@get
            }

            val file = File(path)
            if (file.exists()) {
                call.setContentDisposition(file.name)
                call.respondFile(file)
            } else {
                call.respond(HttpStatusCode.NotFound, "NotFound，文件不存在！")
            }
        }

        get("/createFolder") {
            val hash = call.parameters["hash"]
            val name = call.parameters["name"]
            if (hash.isNullOrBlank()) {
                call.respond(CodeMessageRespondEntity(400, "BadRequest，hash不得为空！"))
                return@get
            }

            if (name.isNullOrBlank()) {
                call.respond(CodeMessageRespondEntity(400, "文件夹名称不得为空！"))
                return@get
            }

            val path = DatabaseFactory.fileIndexDao.getPath(hash).firstOrNull()
            if (path == null) {
                call.respond(CodeMessageRespondEntity(404, "NotFound，未查找到指定文件路径！"))
                return@get
            }

            val filePath = File(path, name)
            if (filePath.mkdirs()) {
                FileIndexer.indexDirectory(filePath)
                call.respond(CodeMessageRespondEntity(200, "文件夹创建成功！"))
            } else {
                call.respond(CodeMessageRespondEntity(500, "文件夹创建失败！"))
            }
        }

        post("/delete") {
            val path = call.receiveParameters()["path"]
            if (path.isNullOrBlank()) {
                call.respond(CodeMessageRespondEntity(404, "NotFound，未查找到指定文件路径！"))
                return@post
            }

            val filePath = File(path)
            if (filePath.isDirectory) {
                LoggerFactory.getLogger("Delete").info("删除文件夹：{}", filePath.absolutePath)
                if (filePath.deleteRecursively()) {
                    DatabaseFactory.fileIndexDao.delete(path)
                    DatabaseFactory.fileIndexDao.deleteByParent(path)
                    call.respond(CodeMessageRespondEntity(200, "删除成功！"))
                } else {
                    call.respond(CodeMessageRespondEntity(500, "删除失败！"))
                }
            } else {
                LoggerFactory.getLogger("Delete").info("删除文件：{}", filePath.absolutePath)
                if (filePath.delete()) {
                    DatabaseFactory.fileIndexDao.delete(path)
                    call.respond(CodeMessageRespondEntity(200, "删除成功！"))
                } else {
                    call.respond(CodeMessageRespondEntity(500, "删除失败！"))
                }
            }
        }

        post("/rename") {
            val params = call.receiveParameters()
            val path = params["path"]
            val newName = params["newName"]
            if (path.isNullOrBlank()) {
                call.respond(CodeMessageRespondEntity(404, "NotFound，未查找到指定文件路径！"))
                return@post
            }
            if (newName.isNullOrBlank()) {
                call.respond(CodeMessageRespondEntity(403, "新文件名不得为空！"))
                return@post
            }

            val file = File(path)
            val newFile = if (file.isDirectory) {
                File(file.parent, newName)
            } else {
                File(file.parent, newName + "." + file.extension)
            }

            val succeed = if (file.isDirectory) {
                LoggerFactory.getLogger("Rename").info("重命名文件夹：{} >> {}", file, newFile)
                file.renameTo(newFile).also {
                    if (it) {
                        DatabaseFactory.fileIndexDao.delete(path)
                        DatabaseFactory.fileIndexDao.deleteByParent(path)
                        FileIndexer.indexDirectory(file)
                    }
                }
            } else {
                file.renameTo(newFile).also {
                    LoggerFactory.getLogger("Rename").info("重命名文件：{} >> {} = {}", file, newFile, it)
                    if (it) {
                        DatabaseFactory.fileIndexDao.delete(path)
                        FileIndexer.indexDirectory(file)
                    }
                }
            }

            if (succeed) {
                call.respond(CodeMessageRespondEntity(200, "重命名成功！"))
            } else {
                call.respond(CodeMessageRespondEntity(500, "重命名失败！"))
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

            val path = DatabaseFactory.fileIndexDao.getPath(hash).find { File(it).exists() }.orEmpty()
            if (path.isBlank() || File(path).exists().not()) {
                call.respond(CodeMessageRespondEntity(404, "NotFound，未查找到指定文件路径！"))
                return@get
            }

            val filePath = DatabaseFactory.fileIndexDao.getPath(fileHash).find { File(it).exists() }.orEmpty()
            if (filePath.isBlank()) {
                call.respond(CodeMessageRespondEntity(404, "文件闪传失败，不存在文件索引！"))
                return@get
            }

            val originalFile = File(filePath) //已存在的原始文件路径
            if (originalFile.exists().not()) {
                call.respond(CodeMessageRespondEntity(400, "文件闪传失败，原始文件不存在！"))
                return@get
            }

            val createLink = File(path, fileName.ifBlank { originalFile.name })
            if (createLink.exists().not() && originalFile.createSymbolicLink(createLink)?.exists() == true) {
                //创建一个符号链接，防止空间占用
                FileIndexer.addFileIndex(createLink, fileHash)
                LoggerFactory.getLogger("Upload").info(
                    "文件闪传完毕: ${originalFile.absolutePath} linked >> ${createLink.absolutePath}"
                )
            } else {
                LoggerFactory.getLogger("Upload").info(
                    "文件已经存在: ${originalFile.absolutePath} linked >> ${createLink.absolutePath}"
                )
            }

            FileIndexer.addFileIndex(createLink, fileHash)
            call.respond(CodeMessageRespondEntity(200, "文件闪传完毕！"))
        }

        post("/upload") {
            val hash = call.parameters["hash"]
            val fileHash = call.parameters["fileHash"]

            if (hash.isNullOrBlank()) {
                call.respond(CodeMessageRespondEntity(403, "BadRequest，hash不得为空！"))
                return@post
            }

            val path = DatabaseFactory.fileIndexDao.getPath(hash).firstOrNull().orEmpty()
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
            call.receiveMultipart().forEachPart {
                if (it is PartData.FileItem) {
                    try {
                        val fileName = it.originalFileName ?: "${System.currentTimeMillis()}.data"
                        LoggerFactory.getLogger("Upload").info(
                            "正在复制文件：$fileName 到 >> ${folder.absolutePath}"
                        )

                        val file = File(folder, fileName)
                        it.streamProvider().use { input ->
                            file.createNewFile()
                            file.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }

                        if (fileHash.isNullOrBlank()) {
                            FileIndexer.addFileIndex(file)
                        } else {
                            FileIndexer.addFileIndex(file, fileHash)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        it.dispose.invoke()
                    }
                }
            }
            call.respond(CodeMessageRespondEntity(200, "文件上传完毕！"))
        }

        get("/flashBackup") {
            val fileName = call.parameters["fileName"]
            val fileHash = call.parameters["fileHash"]
            val folderHash = call.parameters["folderHash"]
            val deviceName = call.parameters["deviceName"]

            if (fileName.isNullOrBlank()) {
                call.respond(CodeMessageRespondEntity(403, "BadRequest，childName不得为空！"))
                return@get
            }

            if (fileHash.isNullOrBlank()) {
                call.respond(CodeMessageRespondEntity(403, "BadRequest，fileHash不得为空！"))
                return@get
            }

            if (folderHash.isNullOrBlank()) {
                call.respond(CodeMessageRespondEntity(403, "BadRequest，folderHash不得为空！"))
                return@get
            }

            if (deviceName.isNullOrBlank()) {
                call.respond(CodeMessageRespondEntity(403, "BadRequest，deviceName不得为空！"))
                return@get
            }

            val folder = DatabaseFactory.fileIndexDao.getPath(folderHash).first()
            if (folder.isBlank() || File(folder).exists().not()) {
                call.respond(CodeMessageRespondEntity(404, "NotFound，未查找到指定目标目录！"))
                return@get
            }

            val targetFolder = File(folder, "文件备份目录")
            if (targetFolder.exists().not()) {
                targetFolder.mkdirs()
                FileIndexer.addFileIndex(targetFolder, targetFolder.absolutePath.toMd5String())
            }

            val folderDevice = File(targetFolder, "来自${deviceName}的文件")
            if (folderDevice.exists().not()) {
                folderDevice.mkdirs()
                FileIndexer.addFileIndex(folderDevice, folderDevice.absolutePath.toMd5String())
            }

            val childFolder = if (FileType.isImage(fileName)) {
                File(folderDevice, "图片")
            } else if (FileType.isVideo(fileName)) {
                File(folderDevice, "视频")
            } else if (FileType.isAudio(fileName)) {
                File(folderDevice, "音频")
            } else {
                call.respond(CodeMessageRespondEntity(403, "BadRequest，不支持的文件类型！"))
                return@get
            }

            if (childFolder.exists().not()) {
                childFolder.mkdirs()
                FileIndexer.addFileIndex(childFolder, childFolder.absolutePath.toMd5String())
            }

            val path = DatabaseFactory.fileIndexDao.getPath(fileHash).find { File(it).exists() }.orEmpty()
            if (path.isBlank()) {
                call.respond(CodeMessageRespondEntity(404, "文件闪传失败，不存在文件索引！"))
                return@get
            }

            val originalFile = File(path) //已存在的原始文件路径
            val createLink = File(childFolder, fileName)
            if (createLink.exists().not() && originalFile.createSymbolicLink(createLink)?.exists() == true) {
                //创建一个符号链接，防止空间占用
                FileIndexer.addFileIndex(createLink, fileHash)
                LoggerFactory.getLogger("Backup").info(
                    "文件闪传完毕: ${originalFile.absolutePath} linked >> ${createLink.absolutePath}"
                )
            } else {
                LoggerFactory.getLogger("Backup").info(
                    "文件已经存在: ${originalFile.absolutePath} linked >> ${createLink.absolutePath}"
                )
            }
            call.respond(CodeMessageRespondEntity(200, "文件闪传完毕！"))
        }

        post("/backup") {
            val fileName = call.parameters["fileName"]
            val fileHash = call.parameters["fileHash"]
            val folderHash = call.parameters["folderHash"]
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

            if (folderHash.isNullOrBlank()) {
                call.respond(CodeMessageRespondEntity(403, "BadRequest，folderHash不得为空！"))
                return@post
            }

            val folder = DatabaseFactory.fileIndexDao.getPath(folderHash).first()
            if (folder.isBlank() || File(folder).exists().not()) {
                call.respond(CodeMessageRespondEntity(404, "NotFound，未查找到指定目标目录！"))
                return@post
            }

            val targetFolder = File(folder, "文件备份目录")
            if (targetFolder.exists().not()) {
                targetFolder.mkdirs()
                FileIndexer.addFileIndex(targetFolder, targetFolder.absolutePath.toMd5String())
            }

            val folderDevice = File(targetFolder, "来自${deviceName}的文件")

            if (folderDevice.exists().not()) {
                folderDevice.mkdirs()
                FileIndexer.addFileIndex(folderDevice, folderDevice.absolutePath.toMd5String())
            }

            val childFolder = if (FileType.isImage(fileName)) {
                File(folderDevice, "图片")
            } else if (FileType.isVideo(fileName)) {
                File(folderDevice, "视频")
            } else if (FileType.isAudio(fileName)) {
                File(folderDevice, "音频")
            } else {
                call.respond(CodeMessageRespondEntity(403, "BadRequest，不支持的文件类型！"))
                return@post
            }

            if (childFolder.exists().not()) {
                childFolder.mkdirs()
                FileIndexer.addFileIndex(childFolder, childFolder.absolutePath.toMd5String())
            }

            LoggerFactory.getLogger("Backup").info("正在接收 $fileName 到缓存...")
            call.receiveMultipart().forEachPart { part ->
                if (part is PartData.FileItem) {
                    try {
                        LoggerFactory.getLogger("Backup").info(
                            "正在复制文件：$fileName 到 >> ${childFolder.absolutePath}"
                        )

                        part.streamProvider().use { input ->
                            val file = File(childFolder, fileName)
                            file.createNewFile()
                            file.outputStream().use { output ->
                                input.copyTo(output)
                            }

                            LoggerFactory.getLogger("Backup").info("文件复制完毕:{}", file.absolutePath)

                            if (fileHash.isBlank()) {
                                FileIndexer.addFileIndex(file)
                            } else {
                                FileIndexer.addFileIndex(file, fileHash)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        //丢弃缓存文件
                        part.dispose.invoke()
                    }
                }
            }
            call.respond(CodeMessageRespondEntity(200, "文件上传完毕！"))
        }
    }
}


