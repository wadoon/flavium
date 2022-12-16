package edu.kit.iti.formal.flavium

import io.ktor.server.html.*
import kotlinx.html.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction
import java.text.SimpleDateFormat
import java.util.*

abstract class BaseLayout : Template<HTML> {
    override fun HTML.apply() {
        lang = "en"
        head {
            meta { charset = "utf-8" }
            meta {
                name = "viewport"
                content = "width=device-width,initial-scale=1"
            }
            styleLink("/static/pico.min.css")
            styleLink("/static/facss/fontawesome.min.css")
            styleLink("/static/facss/brands.min.css")
            styleLink("/static/facss/solid.min.css")
            styleLink("/static/custom.css")
            title("Flavium: Test and compare your solution of the SAT exercise.")
        }
        body(classes = javaClass.simpleName) {
            main("container") {
                header {
                    h1 {
                        a("/") {
                            img {
                                src = "/static/kolosseum.webp"
                                style = "height :1em; margin-right:1em;"
                            }
                            +"Flavium"
                        }
                        span("sub") {
                            +"Test and compare your solution of the SAT exercise."
                        }
                    }
                }
                content()
                footer {
                    +"This software is open-source and available at "
                    a("https://github.com/wadoon/flavium") {
                        i("fa-brands fa-github") {}
                        +" wadoon/flavium"
                    }
                }
            }
        }
    }

    protected abstract fun MAIN.content()
    fun render(html: HTML) = html.apply()
}

@Serializable
data class Submission(val id: String, val pseudonym: String, val time: Long)

class IndexPage(
    private val tenantConfig: TenantConfig,
    private val submissions: List<Submission> = listOf()
) : BaseLayout() {

    override fun MAIN.content() {
        p {
            unsafe { +tenantConfig.introduction }
        }
        sectionUpload()
        sectionSubmissions()
        sectionLeaderboard()
    }

    private val leaderboardEntries: List<Entry> by lazy {
        transaction {
            Entry.find { Leaderboard.tennant eq tenantConfig.id }
                .sortedWith(comparator)
        }
    }


    private fun MAIN.sectionSubmissions() {
        if (submissions.isNotEmpty()) {
            section("submissions") {
                h3 { +"Your submissions" }
                ol {
                    submissions.sortedWith(compareByDescending { it.time }).forEach {
                        val rank = leaderboardEntries.rank(it)
                        li("submission") {
                            a("/results?id=${it.id}") {
                                +it.time.asDateTime()
                                span {
                                    +" Current Rank:  ${if (rank < 0) "n/a" else rank + 1} "
                                }
                                if (rank == 0) faIconSolid("fa-crown")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun MAIN.sectionUpload() {
        section("upload") {
            h2 {
                i("fa-brands fa-java") {}
                +" Upload your solution (Java file)"
            }
            div("grid") {
                div {
                    form("/submit", FormEncType.multipartFormData, method = FormMethod.post) {
                        label {
                            fileInput {
                                name = "javaFile"
                                required = true
                            }
                        }
                        button {
                            faIconSolid("fa-upload")
                        }
                    }
                }
                div {
                    unsafe { +tenantConfig.privacyNote }
                }
            }
        }
    }


    private fun MAIN.sectionLeaderboard() {
        section("leaderboard") {
            h2 {
                i("fa-solid fa-ranking-star") {}
                +" Leaderboard"
            }

            p("leaderboard-hints") { unsafe { +tenantConfig.leaderboardHints } }

            table {
                role = "grid"
                tr {
                    th(classes = "right") { +"Rank" }
                    th { +"Pseudonym" }
                    th(classes = "right") { +"Score" }
                    th(classes = "right") { +"Time" }
                }

                leaderboardEntries.forEachIndexed { index, it ->
                    tr {
                        td(classes = "right") { +"$index" }
                        td { +it.pseudonym }
                        td(classes = "right") {
                            +"%3.3f".format(it.score)
                        }
                        td(classes = "right") {
                            +"%10.3f seconds".format(it.time / 1000.0)
                        }
                    }
                }
            }
        }
    }
}

fun FlowOrPhrasingContent.faIconSolid(icon: String) {
    span("fa-solid $icon") { +"" }
}

private fun List<Entry>.rank(s: Submission): Int = indexOfFirst { it.id.value.toString() == s.id }

private fun Long.asDateTime(): String {
    val f = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
    return f.format(Date(this))
}

class ErrorPage(private val message: String) : BaseLayout() {
    override fun MAIN.content() {
        div("error") {
            style = "background:red;"
            +message
        }
    }
}


class SubmitPage(private val task: Job, private val pos: Long) : BaseLayout() {
    override fun MAIN.content() {
        section("") {
            h3 { +"Upload successful" }
            a {
                href = "/results?id=${task.id}"
                +"Your results will appear here soon."
            }
            br { }
            span { +"You are at position $pos of the queue." }
        }
    }
}

class DetailPage(private val result: Job) : BaseLayout() {
    override fun MAIN.content() {
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

private val comparator by lazy {
    val sr = Comparator.comparingDouble<Entry> { -it.score }
    val time = Comparator.comparingInt<Entry> { it.time }
    sr.thenComparing(time)
}
