package edu.kit.iti.formal.flavium

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*


object Tenants : IdTable<String>() {
    override val id = varchar("id", 32).entityId()
    //override val primaryKey = PrimaryKey(id)
}

class Tenant(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, Tenant>(Tenants)
}

object Leaderboard : UUIDTable() {
    val pseudonym = varchar("pseudonym", 128)
    val time = integer("time")
    val score = double("score")
    val tenant = reference("tenant", Tenants)
}

class Entry(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Entry>(Leaderboard)

    var pseudonym by Leaderboard.pseudonym
    var time by Leaderboard.time
    var score by Leaderboard.score
}


object Jobs : UUIDTable() {
    val pseudonym = varchar("pseudonym", 128)
    val fileContent = text("file_content")

    val rollingNumber = integer("rolling_number").autoIncrement()

    val stdout = Jobs.text("stdout").default("AWAITING EXECUTION...")
    val stderr = Jobs.text("stderr").default("AWAITING EXECUTION...")
    val status = Jobs.integer("status").default(-1)

    val tenant = Jobs.reference("tenant", Tenants)
}

class Job(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Job>(Jobs)

    var pseudonym by Jobs.pseudonym
    var fileContent by Jobs.fileContent

    var rollingNumber by Jobs.rollingNumber

    var stdout by Jobs.stdout
    var stderr by Jobs.stderr
    var status by Jobs.status
    var tenant by Jobs.tenant
}

