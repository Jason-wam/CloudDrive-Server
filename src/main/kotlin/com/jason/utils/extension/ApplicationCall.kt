package com.jason.utils.extension

import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

suspend inline fun ApplicationCall.setContentDisposition(name: String) {
    response.header(
        "Content-Disposition", "inline;filename=${
            withContext(Dispatchers.IO) {
                URLEncoder.encode(name, "utf-8")
            }
        }"
    )
}