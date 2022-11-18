package edu.kit.iti.formal.flavium

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.date.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.util.*
import java.util.function.Predicate
import java.util.regex.Pattern

val workerQueue = startWorkerQueue()

val PORT = System.getProperty("PORT", "8080").toInt()

fun main() {
    embeddedServer(Netty, port = PORT) {
        routing {
            get("/") {
                val submissions: List<Submission> = call.request.submissions()
                call.respondHtml(HttpStatusCode.OK) { IndexPage(submissions).render(this) }
            }
            post("/submit") { this.submitController() }
            get("/results") { detailsController() }
            static("/static") {
                resources()
            }
        }
    }.start(wait = true)
}

val uuidTest: Predicate<String> = Pattern.compile("[a-f0-9-]+").asMatchPredicate()

private suspend fun PipelineContext<Unit, ApplicationCall>.detailsController() {
    val id = this.context.request.queryParameters["id"]

    if (id == null || !uuidTest.test(id)) {
        call.respondHtml(HttpStatusCode.InternalServerError) {
            ErrorPage("No data entry given for $id").render(this)
        }
        return
    }

    val file = File(RESULT_FOLDER, "$id.json")
    if (!file.exists()) {
        call.respondHtml(HttpStatusCode.NotFound) { ErrorPage("No data entry given for $id").render(this) }
        return
    }

    try {
        val result = Json.decodeFromString<Result>(file.readText())
        call.respondHtml(HttpStatusCode.OK) { DetailPage(result).render(this) }
    } catch (e: IOException) {
        call.respondHtml(HttpStatusCode.InternalServerError) {
            error(e.message ?: "Exception!")
        }
        return
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.submitController() {
    val multipart = call.receiveMultipart()
    val submissions = call.request.submissions().toMutableList()
    try {
        multipart.forEachPart { partData ->
            when (partData) {
                is PartData.FormItem -> {
                    //to read additional parameters that we sent with the image
                    //if (partData.name == "text") {}
                }

                is PartData.FileItem -> {
                    val content = partData.streamProvider().reader().readText()
                    val (task, pos) = workerQueue.emit(content)
                    submissions.add(Submission(task.id, task.pseudonym, Date().time))
                    val cookie =
                        Cookie(
                            "submissions", Json.encodeToString(submissions),
                            expires = GMTDate(0, 0, 0, 1, Month.FEBRUARY, 2023)
                        )
                    call.response.cookies.append(cookie)
                    call.respondHtml(HttpStatusCode.OK) { SubmitPage(task, pos).render(this) }
                    partData.dispose()
                }

                is PartData.BinaryItem -> Unit
                is PartData.BinaryChannelItem -> Unit
            }
        }
    } catch (ex: Exception) {
        call.respond(HttpStatusCode.InternalServerError, "Error")
        call.application.environment.log.error(ex.stackTraceToString())
    }
}

private fun ApplicationRequest.submissions(): List<Submission> =
    cookies["submissions"]?.let {
        Json.decodeFromString(it)
    } ?: listOf()
