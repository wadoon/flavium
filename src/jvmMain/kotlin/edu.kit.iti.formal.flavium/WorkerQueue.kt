package edu.kit.iti.formal.flavium

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Files
import java.util.*
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

val JAVA_FILENAME: String = System.getProperty("SOLUTION_FILENAME", "MyKuromasuSolver.java")

val RESET_SCRIPT = System.getProperty("RESET_SCRIPT", "reset.sh")
val RUN_SCRIPT = System.getProperty("RUN_SCRIPT", "run.sh")

val REGEX_SUCCESS_RATE: Pattern = System.getProperty("RE_SUCCESS_RATE", "success (.*?) %").let {
    Pattern.compile(it)
}

val WORK_FOLDER = File(System.getProperty("WORK_FOLDER", "work"))
    .absoluteFile
    .apply {
        mkdirs()
    }
val WORK_QUEUE: File = File(System.getProperty("WORK_QUEUE", "workqueue.json"))
    .absoluteFile

val RESULT_FOLDER: File = File(System.getProperty("RESULT_FOLDER", "results"))
    .absoluteFile
    .apply {
        mkdirs()
    }

fun loadWorkerQueue(pseudonyms: MutableSet<String>): WorkerQueue {
    val wq = if (WORK_QUEUE.exists()) {
        WorkerQueue(Json.decodeFromString(WORK_QUEUE.readText()), RandomName(pseudonyms))
    } else
        WorkerQueue()
    val t = Thread(wq)
    t.start()
    return wq
}

@Serializable
data class Task(val id: String, val pseudonym: String, val javaCode: String)

@Serializable
data class Result(val id: String, val pseudonym: String, val stdout: String, val stderr: String, val status: Int)


class WorkerQueue(
    entries: List<Task> = listOf(),
    private val nameGenerator: RandomName = RandomName(mutableSetOf())
) :
    Runnable {
    private val queue = LinkedBlockingDeque(entries)

    @Synchronized
    fun emit(javaCode: String): Pair<Task, Int> {
        val t = Task(UUID.randomUUID().toString(), nameGenerator.getRandomName(), javaCode)
        queue.add(t)
        return t to queue.size
    }

    @Synchronized
    private fun save() = WORK_QUEUE.writeText(Json.encodeToString(queue.toList()))

    override fun run() {
        fun execute(task: Task) {
            var stdout = ""
            var stderr = ""
            var status: Int

            try {
                // fill in the java file
                File(WORK_FOLDER, JAVA_FILENAME).writeText(task.javaCode)

                // prepare stage
                val pb = ProcessBuilder()
                    .command(RESET_SCRIPT, WORK_FOLDER.absolutePath)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                val processPrepare = pb.start()
                status = processPrepare.waitFor()

                stdout = processPrepare.inputReader().readText()
                stderr = processPrepare.errorReader().readText()

                if (status == 0) {
                    val tmpFile = File(WORK_FOLDER.absolutePath, "stdout.out");
                    val tmpErrFile = File(WORK_FOLDER.absolutePath, "stderr.out");
                    val pbrun = ProcessBuilder()
                        .command(RUN_SCRIPT, WORK_FOLDER.absolutePath)
                        .redirectError(ProcessBuilder.Redirect.to(tmpErrFile))
                        .redirectOutput(ProcessBuilder.Redirect.to(tmpFile))

                    val start = System.currentTimeMillis()
                    val run = pbrun.start()
                    status = run.waitFor()
                    val time = System.currentTimeMillis() - start

                    stdout += tmpFile.readText()
                    stderr += tmpErrFile.readText()

                    if (status == 0) {
                        val m = REGEX_SUCCESS_RATE.matcher(stdout)
                        val score = if (m.find())
                            m.group(1).toDouble()
                        else 0.0
                        leaderboard.announce(Entry(task.id, task.pseudonym, time.toInt(), score))
                    }
                } else {
                    stderr += "\nRun aborted due to error during preparation!"
                }
            } catch (e: Exception) {
                status = -1
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                stderr += sw.toString()
            }

            File(RESULT_FOLDER, task.id + ".json")
                .writeText(Json.encodeToString(Result(task.id, task.pseudonym, stdout, stderr, status)))
        }

        while (true) {
            val task: Task? = queue.poll(60, TimeUnit.SECONDS)
            if (task != null) {
                execute(task)
                save()
            }
        }
    }
}