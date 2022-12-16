package edu.kit.iti.formal.flavium

import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

fun startWorkerQueue(): WorkerQueue {
    val wq = WorkerQueue()
    val t = Thread(wq)
    t.start()
    return wq
}


class WorkerQueue() : Runnable {
    private val lock = ReentrantLock()
    private val barrier = lock.newCondition()

    fun wakeUp() {
        lock.withLock {
            barrier.signal()
        }
    }

    override fun run() {
        fun execute(task: Job) {
            val tenantConfig = config.tenants.find { it.id == task.tenant.value }

            var stdout = ""
            var stderr = ""
            var status: Int
            var userScore: Double = -1.0
            var runtime: Long = Long.MAX_VALUE

            if (tenantConfig == null) {
                stderr = "Tenant with '${task.tenant.value}' not found. Server configuration error."
                status = 1
            } else {
                try {
                    // fill in the java file
                    File(tenantConfig.workFolder, tenantConfig.uploadFilename).writeText(task.fileContent)

                    // prepare stage
                    val pb = ProcessBuilder()
                        .command(tenantConfig.resetScript, tenantConfig.workFolder.toString())
                        .redirectError(ProcessBuilder.Redirect.PIPE)
                        .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    val processPrepare = pb.start()
                    status = processPrepare.waitFor()

                    stdout = processPrepare.inputReader().readText()
                    stderr = processPrepare.errorReader().readText()

                    if (status == 0) {
                        val pbrun = ProcessBuilder()
                            .command(tenantConfig.runScript, tenantConfig.workFolder.toString())
                            .redirectError(ProcessBuilder.Redirect.PIPE)
                            .redirectOutput(ProcessBuilder.Redirect.PIPE)

                        val start = System.currentTimeMillis()
                        val run = pbrun.start()
                        status = run.waitFor()
                        runtime = System.currentTimeMillis() - start

                        stdout += run.inputReader().readText()
                        stderr += run.errorReader().readText()

                        if (status == 0) {
                            val m = tenantConfig.regexScore.matcher(stdout)
                            userScore = if (m.find())
                                m.group(1).toDouble()
                            else 0.0
                        }
                    } else {
                        stderr += "\nRun aborted due to error during preparation!"
                    }
                } catch (e: Exception) {
                    status = 1
                    userScore = -1.0
                    val sw = StringWriter()
                    e.printStackTrace(PrintWriter(sw))
                    stderr += sw.toString()
                }
            }

            transaction {
                task.stdout = stdout
                task.stderr = stderr
                task.status = status
                require(status >= 0)
                Leaderboard.insert {
                    it[id] = task.id
                    it[pseudonym] = task.pseudonym
                    it[time] = runtime.toInt()
                    it[score] = userScore
                }
            }
        }

        try {
            while (true) {
                val task: Job? = getNextOpenJob()
                if (task != null) {
                    execute(task)
                } else {
                    lock.withLock {
                        barrier.await(10, TimeUnit.SECONDS)
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.error("WorkerQueue died!", e)
        }
    }

    private fun getNextOpenJob(): Job? {
        return transaction {
            Job.find(Jobs.status neq -1).minByOrNull { it.rollingNumber }
        }
    }
}