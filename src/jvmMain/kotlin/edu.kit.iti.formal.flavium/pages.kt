package edu.kit.iti.formal.flavium

import io.ktor.server.html.*
import kotlinx.html.*
import kotlinx.serialization.Serializable
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

abstract class BaseLayout : Template<HTML> {
    val content = Placeholder<MAIN>()
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
                        img {
                            src = "/static/kolosseum.webp"
                            style = "height :1em; margin-right:1em;"
                        }
                        +"Flavium"
                        span("sub") {
                            +"Test and compare your solution of the SAT exercise."
                        }
                    }
                    p {
                        +"""This service tries to be data-minimalistic and privacy compliant as possible. Therefore we limited the user input to minimal 
                             require information and avoid personal data everywhere. The only required data is your solution (Java file). Please avoid 
                              also to add personal data in this file. The Java file is stored until the benchmark is processed."""
                        br { }
                        +"""Cookie-Disclaimer: When uploading a solution, this site uses a cookie to store your submission id. 
                            This allows you to easily see your position and access the log of your submitted solutions."""
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

class IndexPage(val submissions: List<Submission> = listOf()) : BaseLayout() {
    override fun MAIN.content() {
        sectionUpload()
        sectionSubmissions()
        sectionLeaderboard()
    }

    private fun MAIN.sectionSubmissions() {
        if (submissions.isNotEmpty()) {
            section("submissions") {
                h3 { +"Your submissions" }
                ol {
                    val lb = leaderboard.entries()
                    submissions.sortedWith(compareBy { it.time }).forEach {
                        val rank = lb.rank(it)
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
                    +"Note:"
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
            p("warning") {
                +"Leaderboard is sorted firstly by the score (successful solving of sat/unsat instances), then after the run time."
            }
            table {
                role = "grid"
                tr {
                    th(classes = "right") { +"Rank" }
                    th { +"Pseudonym" }
                    th(classes = "right") { +"Score" }
                    th(classes = "right") { +"Time" }
                }

                leaderboard.entries().forEachIndexed { index, it ->
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

private fun List<Entry>.rank(s: Submission): Int = indexOfFirst { it.id == s.id }

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


class SubmitPage(private val task: Task, private val pos: Int) : BaseLayout() {
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

class DetailPage(private val result: Result) : BaseLayout() {
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
