package org.example.noh4uk.socialRating.core.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection

class MySQLDatabase(host: String, port: Int, database: String, user: String, password: String): Database {
    private val dataSource: HikariDataSource

    init {
        val config = HikariConfig()
        config.jdbcUrl = "jdbc:mysql://$host:$port/$database"
        config.username = user
        config.password = password
        config.maximumPoolSize = 10
        config.addDataSourceProperty("cachePrepStmts", "true")
        config.addDataSourceProperty("prepStmtCacheSize", 250)
        config.addDataSourceProperty("prepStmtCacheSqlLimit", 2048)

        dataSource = HikariDataSource(config)
        createMySQLTableIfNotExists()
    }

    override fun getConnection(): Connection {
        return dataSource.connection
    }

    override fun close() {
        dataSource.close()
    }
}