package org.example.noh4uk.socialRating.core.db

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

class SQLiteDatabase(file: File): Database {
    private val url: String = "jdbc:sqlite:${file.absolutePath}"

    init {
        Class.forName("org.sqlite.JDBC").getConstructor().newInstance()
        createSQLiteTableIfNotExists()
    }

    override fun getConnection(): Connection {
        return DriverManager.getConnection(url)
    }

    override fun close() {}
}