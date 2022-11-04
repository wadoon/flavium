package edu.kit.iti.formal.flavium

import io.ktor.http.HttpStatusCode
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.Netty
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.util.regex.Pattern

val leaderboard = loadLeaderboard()
val workerQueue = loadWorkerQueue(leaderboard.getPseudonyms())


fun HTML.index() {
    lang = "en"
    head {
        meta { charset = "utf-8" }
        meta {
            name = "viewport"
            content = "width=device-width,initial-scale=1"
        }
        styleLink("/static/pico.min.css")
        title("Flavium: Test and compare your solution of the SAT excercise.")
    }
    body {
        main("container") {
            div {
                h1 { +"Flavium" }
                h2 { +"Test and compare your solution of the SAT excercise." }
            }

            section("upload") {
                form("/submit", FormEncType.multipartFormData, method = FormMethod.post) {
                    label {
                        +"Upload your solution (Java file):"
                        fileInput {
                            name = "javaFile"
                            required = true
                        }
                    }
                    submitInput {}
                }
            }

            section("leaderboard") {
                h2 { +"Leaderboard" }
                p("warning") {
                    +"Leaderboard is sorted firstly by the score (successful solving of sat/unsat instances), then after the run time."
                }
                table {
                    role = "grid"
                    tr {
                        th { +"Pseudonym" }
                        th { +"Score" }
                        th { +"Time" }
                    }

                    leaderboard.entries().forEach {
                        tr {
                            td { +it.pseudonym }
                            td { +"%3.3f".format(it.score) }
                            td { +"%d".format(it.time) }
                        }
                    }
                }
            }
        }
        //script(src = "/static/competitor.js") {}
    }
}

fun HTML.error(message: String) {
    lang = "en"
    head {
        meta { charset = "utf-8" }
        meta {
            name = "viewport"
            content = "width=device-width,initial-scale=1"
        }
        styleLink("/static/pico.min.css")
        title("Flavium: Test and compare your solution of the SAT excercise.")
    }
    body {
        main("container") {
            div("error") {
                style = "background:red;"
                +message
            }
        }
    }
}

fun HTML.submit(task: Task, pos: Int) {
    lang = "en"
    head {
        meta { charset = "utf-8" }
        meta {
            name = "viewport"
            content = "width=device-width,initial-scale=1"
        }
        styleLink("/static/pico.min.css")
        title("Flavium: Test and compare your solution of the SAT excercise.")
    }
    body {
        main("container") {
            div {
                h1 { +"Flavium" }
                h2 { +"Test and compare your solution of the SAT excercise." }
            }

            section("") {
                h3 { +"Upload successfull" }
                a {
                    href = "/results?id=${task.id}"
                    +"Your results will appear here soon."
                }
                br { }
                span { +"You are at position $pos of the queue." }
            }
        }
        //script(src = "/static/competitor.js") {}
    }
}

fun HTML.detail(result: Result) {
    lang = "en"
    head {
        meta { charset = "utf-8" }
        meta {
            name = "viewport"
            content = "width=device-width,initial-scale=1"
        }
        styleLink("/static/pico.min.css")
        title("Flavium: Test and compare your solution of the SAT excercise.")
    }
    body {
        main("container") {
            span { +"Id: ${result.id}" }
            br { }
            span { +"Pseudonym: ${result.pseudonym}" }
            br { }
            span { +"Status: ${result.status}" }

            h3 { +"stdout" }
            pre { +result.stdout }

            h3 { +"stderr" }
            pre { +result.stderr }
        }
    }
}

val PORT = System.getProperty("PORT", "8080").toInt()

fun main() {
    embeddedServer(Netty, port = PORT) {
        routing {
            get("/") {
                call.respondHtml(HttpStatusCode.OK, HTML::index)
            }
            post("/submit") {
                val multipart = call.receiveMultipart()
                try {
                    multipart.forEachPart { partData ->
                        when (partData) {
                            is PartData.FormItem -> {
                                //to read additional parameters that we sent with the image
                                //if (partData.name == "text") {}
                            }

                            is PartData.FileItem -> {
                                val content = partData.provider().readText()
                                val (task, pos) = workerQueue.emit(content)
                                call.respondHtml(HttpStatusCode.OK) { submit(task, pos) }
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
            get("/results") {
                val id = this.context.request.queryParameters["id"]
                val uuidre = Pattern.compile("[a-f0-9-]+").asMatchPredicate()

                if (id == null || !uuidre.test(id)) {
                    call.respondHtml(HttpStatusCode.InternalServerError) {
                        error("Invalid id given")
                    }
                    return@get
                }

                val file = File(RESULT_FOLDER, "$id.json")
                if (!file.exists()) {
                    call.respondHtml(HttpStatusCode.NotFound) {
                        error("No data entry given for $id")
                    }
                    return@get
                }

                try {
                    val result = Json.decodeFromString<Result>(file.readText())
                    call.respondHtml(HttpStatusCode.OK) { detail(result) }
                } catch (e: IOException) {
                    call.respondHtml(HttpStatusCode.InternalServerError) {
                        error(e.message ?: "Exception!")
                    }
                    return@get
                }
            }
            static("/static") {
                resources()
            }
        }
    }.start(wait = true)
}