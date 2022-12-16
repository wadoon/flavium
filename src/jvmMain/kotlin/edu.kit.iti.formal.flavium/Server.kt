package edu.kit.iti.formal.flavium

import com.charleskorn.kaml.Yaml
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.date.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.io.File
import java.io.PrintWriter
import java.nio.file.Paths
import java.util.*
import java.util.function.Predicate
import java.util.regex.Pattern
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText


val CONFIG = Paths.get(System.getProperty("CONFIG", "config.yaml"))

val PORT = System.getProperty("PORT", "8080").toInt()

val config: Config by lazy {
    if (CONFIG.exists())
        Yaml.default.decodeFromString<Config>(CONFIG.readText())
    else {
        val c = Config()
        CONFIG.writeText(Yaml.default.encodeToString(c))
        c
    }
}

val db: Database = null.let {
    //val dbfile = System.getProperty("DATABASE", File("flavium.sqlite").absolutePath)
    //TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    //Database.connect("jdbc:sqlite:$dbfile", "org.sqlite.JDBC")
    val dbfile = System.getProperty("DATABASE", File("db.h2").absolutePath)
    Database.connect("jdbc:h2:file:$dbfile;MODE=MYSQL")
}

lateinit var workerQueue: WorkerQueue

val LOGGER = LoggerFactory.getLogger("app")

fun main() {
    require(config.tenants.isNotEmpty()) { "You need atleast one tennant/site." }
    LOGGER.info("Database vendor: {}, version: {}", db.vendor, db.version)

    transaction {
        addLogger(Slf4jSqlDebugLogger)

        SchemaUtils.create(Jobs, Leaderboard, Tenants)
        config.tenants.forEach { tc ->
            try {
                Tenants.insert {
                    it[id] = tc.id
                }
            } catch (ignore: ExposedSQLException) {
            }
        }
    }

    workerQueue = startWorkerQueue()


    embeddedServer(Netty, port = PORT, module = Application::flaviumModule).start(wait = true)
}

private suspend fun PipelineContext<Unit, ApplicationCall>.handleIndexPage(tenant: String?) {
    val tc = config.tenants.find { it.id == tenant }
    if (tenant == null || tc == null) {
        call.respondHtml(HttpStatusCode.NotFound) { ErrorPage("Not found: $tenant ($tc)").render(this) }
    } else {
        val submissions: List<Submission> = call.request.submissions()
        call.respondHtml(HttpStatusCode.OK) { IndexPage(tc, submissions).render(this) }
    }
}

val uuidTest: Predicate<String> = Pattern.compile("[a-f0-9-]+").asMatchPredicate()

private suspend fun PipelineContext<Unit, ApplicationCall>.detailsController(tenant: String?) {
    val tc = config.tenants.find { it.id == tenant }
    val id = this.context.request.queryParameters["id"]

    if (tenant == null || tc == null) {
        call.respondHtml(HttpStatusCode.InternalServerError) {
            ErrorPage("No tenant given").render(this)
        }
        return
    }

    if (id == null || !uuidTest.test(id)) {
        call.respondHtml(HttpStatusCode.InternalServerError) {
            ErrorPage("No data entry given for $id").render(this)
        }
        return
    }

    val result = transaction { Job.findById(UUID.fromString(id)) }

    if (result == null)
        call.respondHtml(HttpStatusCode.NotFound) { ErrorPage("No data entry given for $id").render(this) }
    else
        call.respondHtml(HttpStatusCode.OK) { DetailPage(result).render(this) }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.submitController(tenant: String?) {
    if (tenant == null) throw Exception("tenant is null")

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

                    val (task, pos) = transaction {
                        val task = Job.new {
                            this.pseudonym = findFreePseudonym(tenant)
                            this.fileContent = content
                        }
                        workerQueue.wakeUp()
                        val pos = Jobs.select(
                            (Jobs.status eq -1) and
                                    (Jobs.rollingNumber less task.rollingNumber)
                        ).count()
                        task to pos
                    }

                    submissions.add(Submission(task.id.value.toString(), task.pseudonym, Date().time))
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

fun findFreePseudonym(tenant: String): String {
    val used = Jobs.slice(Jobs.pseudonym).select(Jobs.tenant eq tenant)
        .map { it[Jobs.pseudonym] }
        .toSet()
    val ng = RandomName(used)
    return ng.getRandomName(" ")
}

private fun ApplicationRequest.submissions(): List<Submission> =
    cookies["submissions"]?.let {
        Json.decodeFromString(it)
    } ?: listOf()

fun Application.flaviumModule() {
    val firstTenant = config.tenants.first().id

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }
    install(AutoHeadResponse)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondTextWriter(status = HttpStatusCode.InternalServerError) {
                write("500: ${cause.message}\n")
                cause.printStackTrace(PrintWriter(this))
            }
        }
    }


    routing {
        get("/") { handleIndexPage(firstTenant) }
        get("/{tenant}/") { handleIndexPage(call.parameters["tenant"]) }

        post("/submit") { this.submitController(firstTenant) }
        post("/{tenant}/submit") { this.submitController(call.parameters["tenant"]) }

        get("/results") { detailsController(firstTenant) }
        get("/{tenant}/results") { detailsController(call.parameters["tenant"]) }

        static("/static") {
            resources()
        }
    }
}
