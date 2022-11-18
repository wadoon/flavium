package edu.kit.iti.formal.flavium

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration
import java.util.*


object Database {
    val databaseConfig = Properties().apply {
        setProperty("hibernate.connection.url", "jdbc:derby:flavium_derby.db;create=true")
        setProperty("dialect", "org.hibernate.dialect.DerbyDialect")
        setProperty("hibernate.connection.driver_class", "org.apache.derby.iapi.jdbc.AutoloadedDriver")
        setProperty("show_sql", true.toString())
        //will create tables. But it will not create database. Change the connection url to generate the database.
        setProperty("hibernate.hbm2ddl.auto", "create")
    }

    val config = Configuration().apply {
        addProperties(databaseConfig)
        addAnnotatedClass(Task::class.java)
        addAnnotatedClass(Entry::class.java)
        addAnnotatedClass(Result::class.java)
    }
    val sessionFactory: SessionFactory = config.buildSessionFactory()
}

@Entity
@Table(name = "leaderboard")
data class Entry(@Id val id: String = "", val pseudonym: String = "", val time: Int = 0, val score: Double = 0.0)

object Leaderboard {
    private val comparator by lazy {
        val sr = Comparator.comparingDouble<Entry> { -it.score }
        val time = Comparator.comparingInt<Entry> { it.time }
        sr.thenComparing(time)
    }

    fun entries(): List<Entry> {
        val session = Database.sessionFactory.openSession()
        //val em: EntityManager = getEntityManager()
        val cb = session.getCriteriaBuilder()
        val cr = cb.createQuery(Entry::class.java)
        val root = cr.from(Entry::class.java)
        cr.orderBy(
            cb.desc(root.get<Double>("score")),
            cb.desc(root.get<Int>("time"))
        )
        val query = session.createQuery(cr)
        val results = query.getResultList()
        return results//.apply { sortWith(comparator) }
    }

    fun getPseudonyms() = entries().map { it.pseudonym }.toMutableSet()

    fun announce(entry: Entry) {
        val em = Database.sessionFactory.openSession()
        em.beginTransaction();
        em.persist(entry)
        em.transaction.commit()
    }
}


@Entity
@Table(name = "tasks")
data class Task(@Id val id: String = "", val pseudonym: String = "", val javaCode: String = "")

@Entity
@Table(name = "results")
data class Result(
    @Id val id: String = "",
    val pseudonym: String = "",
    val stdout: String = "",
    val stderr: String = "",
    val status: Int = -1
)
