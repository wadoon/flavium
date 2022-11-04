package edu.kit.iti.formal.flavium

import io.ktor.server.html.*
import kotlinx.html.*

abstract class BaseLayout : Template<HTML>{
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
            styleLink("/static/custom.css")
            title("Flavium: Test and compare your solution of the SAT excercise.")
        }
        body(classes = javaClass.simpleName) {
            main("container") {
                header {
                    h1 { +"Flavium" }
                    h2 { +"Test and compare your solution of the SAT excercise." }
                }
                content()
            }
        }
    }

    protected abstract fun MAIN.content()
    fun render(html: HTML) = html.apply()
}

class IndexPage : BaseLayout() {
    override fun MAIN.content() {
        sectionUpload()
        sectionLeaderboard()
    }

    private fun MAIN.sectionUpload() {
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
    }

    private fun MAIN.sectionLeaderboard() {
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
}

class ErrorPage(val message: String) : BaseLayout() {
    override fun MAIN.content() {
        div("error") {
            style = "background:red;"
            +message
        }
    }
}


class SubmitPage(val task: Task, val pos: Int) : BaseLayout() {
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

class DetailPage(val result: Result) : BaseLayout() {
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
