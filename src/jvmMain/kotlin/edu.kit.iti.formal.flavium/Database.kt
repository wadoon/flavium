package edu.kit.iti.formal.flavium

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File
import kotlin.Comparator

val LEADERBOARD = File(System.getProperty("LEADERBOARD_FILE", "leaderboard.json"))

fun loadLeaderboard(): Leaderboard {
    // Deserializing back into objects
    if (LEADERBOARD.exists()) {
        return Json.decodeFromString(LEADERBOARD.readText())
    }
    return Leaderboard(ArrayList())
}


class Leaderboard(private val _entries: MutableList<Entry>) {
    private val comparator by lazy {
        val sr = Comparator.comparingDouble<Entry> { it.successRate }
        val time = Comparator.comparingInt<Entry> { it.time }
        sr.thenComparing(time)
    }

    @Synchronized
    fun entries(): List<Entry> = ArrayList(_entries).apply { sortWith(comparator) }

    fun getPseudonyms(): MutableSet<String> =  (_entries.map {it.pseudonym}).toMutableSet()

    @Synchronized
    fun announce(entry: Entry) {
        _entries.add(entry)
        save()
    }

    @Synchronized
    fun save() = LEADERBOARD.writeText(Json.encodeToString(this))

}


@Serializable
data class Entry(val pseudonym: String, val time: Int, val successRate: Double)