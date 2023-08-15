package com.jason.utils.extension

import com.jason.utils.Configure
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder


fun ApplicationCall.setContentLength(length: Long) {
    response.header("Content-Length", length)
}

suspend inline fun ApplicationCall.setContentDisposition(name: String) {
    response.header(
        "Content-Disposition", "attachment; filename=${
            withContext(Dispatchers.IO) {
                URLEncoder.encode(name, "utf-8")
            }
        }"
    )
}

fun ApplicationCall.checkPassword(): Boolean {
    if (Configure.password.isBlank()) return true
    if (parameters["password"]?.isNotBlank() == true) {
        return Configure.password == parameters["password"]
    }
    return Configure.password == request.headers["password"]
}