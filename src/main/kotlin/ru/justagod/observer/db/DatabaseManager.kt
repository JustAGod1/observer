package ru.justagod.observer.db

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseManager {

    object TicketsCreation : Table("obs_tickets_creation") {
        val time = datetime("time").defaultExpression(CurrentDateTime())
        val id = integer("id").autoIncrement()
        val channelId = long("channel_id").index()
        val user = varchar("user",256)

        override val primaryKey: PrimaryKey = PrimaryKey(id)
    }


    fun init() {
        val database = System.getenv("DATABASE_URL")!!
        val databaseUser = System.getenv("DATABASE_USER")!!
        val databasePassword = System.getenv("DATABASE_PASSWORD")!!

        Database.connect(database, user = databaseUser, password = databasePassword)

        transaction {
            SchemaUtils.create(TicketsCreation)
        }
    }

}