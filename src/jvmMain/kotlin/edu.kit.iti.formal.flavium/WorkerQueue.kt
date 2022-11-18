package edu.kit.iti.formal.flavium

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
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

fun startWorkerQueue(): WorkerQueue {
    val em = Database.sessionFactory.createEntityManager()
    val cr = em.criteriaBuilder.createQuery(String::class.java)
    val root = cr.from(Task::class.java)
    cr.select(root.get("pseudonym"))
    val p1 = em.createQuery(cr).resultList
    val p2 = Leaderboard.entries().map { it.pseudonym }
    val pseudonyms = (p1 + p2).toMutableSet()

    val crTasks = em.criteriaBuilder.createQuery(Task::class.java)
    crTasks.from(Task::class.java)
    val tasks = em.createQuery(crTasks).resultList
    em.close()

    val wq = WorkerQueue(tasks, RandomName(pseudonyms))

    val t = Thread(wq)
    t.start()
    return wq
}


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
                    val pbrun = ProcessBuilder()
                        .command(RUN_SCRIPT, WORK_FOLDER.absolutePath)
                        .redirectError(ProcessBuilder.Redirect.PIPE)
                        .redirectOutput(ProcessBuilder.Redirect.PIPE)

                    val start = System.currentTimeMillis()
                    val run = pbrun.start()
                    status = run.waitFor()
                    val time = System.currentTimeMillis() - start

                    stdout += run.inputReader().readText()
                    stderr += run.errorReader().readText()

                    if (status == 0) {
                        val m = REGEX_SUCCESS_RATE.matcher(stdout)
                        val score = if (m.find())
                            m.group(1).toDouble()
                        else 0.0
                        Leaderboard.announce(Entry(task.id, task.pseudonym, time.toInt(), score))
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