package org.example.noh4uk.socialRating.core.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.luckperms.api.model.user.User
import org.example.noh4uk.socialRating.core.models.ChangingRatingType
import org.example.noh4uk.socialRating.core.models.PagedList
import org.example.noh4uk.socialRating.core.models.RatingHistory
import java.sql.Connection
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.SQLException
import java.text.SimpleDateFormat
import java.util.UUID
import kotlin.use

interface Database {
    suspend fun addRating(
        player: User,
        changerPlayer: User,
        rating: Int,
        reason: String
    ) = withContext(Dispatchers.IO) {
        prepareStatement("""
            UPDATE player_rating
            SET current_rating = current_rating + ?
            WHERE player_id = ?
        """.trimIndent()) { connection, statement ->
            statement.setInt(1, rating)
            statement.setString(2, player.uniqueId.toString())
            val changedRows = statement.executeUpdate()

            if (changedRows > 0) {
                connection.commit()
            } else {
                connection.rollback()
            }
        }
    }

    suspend fun removeRating(
        player: User,
        changerPlayer: User,
        rating: Int,
        reason: String,
    ) = withContext(Dispatchers.IO) {
        prepareStatement("""
            UPDATE player_rating
            SET current_rating = current_rating - ?
            WHERE player_id = ?
        """.trimIndent()) { connection, statement ->
            statement.setInt(1, rating)
            statement.setString(2, player.uniqueId.toString())
            val changedRows = statement.executeUpdate()

            if (changedRows > 0) {
                connection.commit()
            } else {
                connection.rollback()
            }
        }
    }

    suspend fun getCurrentRating(player: User): Int? = withContext(Dispatchers.IO) {
        prepareStatement("""
                SELECT current_rating
                FROM player_rating
                WHERE player_id = ?
            """.trimIndent()) { _, statement ->
            statement.setString(1, player.uniqueId.toString())
            val result = statement.executeQuery()
            if (result.next()) {
                result.getInt("current_rating")
            } else {
                null
            }
        }
    }

    suspend fun getRatingHistory(
        player: User,
        page: Int
    ): PagedList<RatingHistory> {
        val pageSize = 3
        val offset = (page - 1) * pageSize

        return prepareConnection { connection ->
            val countStatement = connection.prepareStatement(
                "SELECT COUNT(*) FROM rating_history WHERE player_id = ?"
            )
            countStatement.setString(1, player.uniqueId.toString())
            val countResultSet = countStatement.executeQuery()
            val totalItems = if (countResultSet.next()) countResultSet.getInt(1) else 0

            val historyStatement = connection.prepareStatement("""
                SELECT date, player_id_changer, player_changer, count, reason, type
                FROM rating_history
                WHERE player_id = ?
                ORDER BY date DESC
                LIMIT ? OFFSET ?
            """)
            historyStatement.setString(1, player.uniqueId.toString())
            historyStatement.setInt(2, pageSize)
            historyStatement.setInt(3, offset)

            val resultSet = historyStatement.executeQuery()
            val historyItems = mutableListOf<RatingHistory>()

            while (resultSet.next()) {
                historyItems.add(
                    RatingHistory(
                        date = resultSet.getString("date"),
                        playerIdChanger = UUID.fromString(resultSet.getString("player_id_changer")),
                        playerChanger = resultSet.getString("player_changer"),
                        count = resultSet.getInt("count"),
                        reason = resultSet.getString("reason"),
                        type = ChangingRatingType.valueOf(resultSet.getString("type"))
                    )
                )
            }

            val totalPages = (totalItems + pageSize - 1) / pageSize

            PagedList(
                page = page,
                pageSize = pageSize,
                totalPages = totalPages,
                totalItems = totalItems,
                elements = historyItems
            )
        }
    }

    suspend fun addPlayerRatingIfNotExists(player: User, rating: Int = 100) = withContext(Dispatchers.IO) {
        prepareStatement("""
            INSERT INTO player_rating (id, player_id, username, current_rating)
            SELECT ?, ?, ?, ?
            WHERE NOT EXISTS (
                SELECT 1 FROM player_rating WHERE player_id = ?
            )
        """.trimIndent()) { connection, statement ->
            statement.setString(1, UUID.randomUUID().toString())
            statement.setString(2, player.uniqueId.toString())
            statement.setString(3, player.username)
            statement.setInt(4, rating)
            statement.setString(5, player.uniqueId.toString())

            val rowsAffected = statement.executeUpdate()
            if (rowsAffected > 0) {
                connection.commit()
            }
        }
    }

    fun close()

    fun getConnection(): Connection

    private suspend fun addHistory(
        player: User,
        changerPlayer: User,
        rating: Int,
        reason: String,
        type: ChangingRatingType
    ) = withContext(Dispatchers.IO) {
        prepareStatement("""
            INSERT INTO rating_history (id, date, player_id, player_id_changer, player_changer, count, reason, type)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()) { connection, statement ->
            statement.setString(1, UUID.randomUUID().toString())
            statement.setString(2, SimpleDateFormat("dd.MM.yyyy").format(Date(System.currentTimeMillis())))
            statement.setString(3, player.uniqueId.toString())
            statement.setString(4, changerPlayer.uniqueId.toString())
            statement.setString(5, changerPlayer.username)
            statement.setInt(6, rating)
            statement.setString(7, reason)
            statement.setString(8, type.name)

            val updatedRows = statement.executeUpdate()
            if (updatedRows > 0) {
                connection.commit()
            } else {
                connection.rollback()
            }
        }
    }

    fun createSQLiteTableIfNotExists() = runBlocking {
        prepareConnection { connection ->
            val statement = connection.createStatement()
            statement.execute("""
                CREATE TABLE IF NOT EXISTS player_rating (
                    id TEXT PRIMARY KEY,
                    player_id TEXT UNIQUE NOT NULL,
                    username TEXT NOT NULL,
                    current_rating INT NOT NULL DEFAULT 100
                )
            """)
            statement.execute("""
                CREATE TABLE IF NOT EXISTS rating_history (
                    id TEXT PRIMARY KEY,
                    date TEXT NOT NULL,
                    player_id TEXT NOT NULL,
                    player_id_changer TEXT NOT NULL,
                    player_changer TEXT NOT NULL,
                    count INT NOT NULL,
                    reason TEXT NOT NULL,
                    type TEXT CHECK(type IN ('Add', 'Remove')) NOT NULL,
                    FOREIGN KEY (player_id) REFERENCES player_rating(player_id) ON UPDATE CASCADE
                )
            """)
            statement.execute("""
                CREATE INDEX IF NOT EXISTS idx_player_id_history ON rating_history(player_id)
            """)
            statement.execute("""
                CREATE INDEX IF NOT EXISTS idx_player_id_rating ON player_rating(player_id)
            """)
            connection.commit()
        }
    }

    fun createMySQLTableIfNotExists() = runBlocking {
        prepareConnection { connection ->
            val statement = connection.createStatement()
            statement.execute("""
                CREATE TABLE IF NOT EXISTS player_rating (
                    id VARCHAR(36) PRIMARY KEY,
                    player_id VARCHAR(36) UNIQUE NOT NULL,
                    username VARCHAR(255) NOT NULL,
                    current_rating INT NOT NULL DEFAULT 100
                )
            """)
            statement.execute("""
                CREATE TABLE IF NOT EXISTS rating_history (
                    id VARCHAR(36) PRIMARY KEY,
                    date VARCHAR(255) NOT NULL,
                    player_id VARCHAR(36) NOT NULL,
                    player_id_changer VARCHAR(36) NOT NULL,
                    player_changer VARCHAR(255) NOT NULL,
                    count INT NOT NULL,
                    reason TEXT NOT NULL,
                    type VARCHAR(255) CHECK(type IN ('Add', 'Remove')) NOT NULL,
                    FOREIGN KEY (player_id) REFERENCES player_rating(player_id) ON UPDATE CASCADE
                )
            """)
            connection.commit()
        }
    }

    private suspend fun <T> prepareConnection(completion: (connection: Connection) -> T) = withContext(Dispatchers.IO) {
        getConnection().use { connection ->
            try {
                connection.autoCommit = false
                completion(connection)
            } catch (e: SQLException) {
                connection.rollback()
                throw e
            }
            finally {
                connection.autoCommit = true
            }
        }
    }

    private suspend fun <T> prepareStatement(sql: String, completion: (connection: Connection, statement: PreparedStatement) -> (T)) = withContext(Dispatchers.IO) {
        prepareConnection { connection ->
            connection.prepareStatement(sql).use { statement ->
                completion(connection, statement)
            }
        }
    }
}