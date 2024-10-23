package org.example.noh4uk.socialRating.core

import kotlinx.coroutines.runBlocking
import net.luckperms.api.LuckPerms
import net.luckperms.api.model.user.User
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.example.noh4uk.socialRating.SocialRating
import org.example.noh4uk.socialRating.core.db.Database
import org.example.noh4uk.socialRating.core.db.MySQLDatabase
import org.example.noh4uk.socialRating.core.db.SQLiteDatabase
import org.example.noh4uk.socialRating.extensions.whenAllNotNull
import org.example.noh4uk.socialRating.models.PagedList
import org.example.noh4uk.socialRating.models.RatingHistory
import java.io.File
import java.sql.SQLException

class SocialRatingCore {

    private val db: Database?

    init {
        db = try {
            if(SocialRating.getInstance().config.getBoolean("database.enabled")) {
                createDatabase()
            } else {
                val dbFile = File(SocialRating.getInstance().dataFolder, "database.db")
                dbFile.parentFile?.mkdirs()
                SQLiteDatabase(dbFile)
            }
        } catch (e: SQLException) {
            throw RuntimeException("Failed to initialize database", e)
        }
    }

    private fun createDatabase(): MySQLDatabase? {
        var db: MySQLDatabase? = null

        val host = SocialRating.getInstance().config.getString("database.host")
        val port = SocialRating.getInstance().config.getInt("database.port", 3306)
        val database = SocialRating.getInstance().config.getString("database.database")
        val user = SocialRating.getInstance().config.getString("database.user")
        val password = SocialRating.getInstance().config.getString("database.password")

        whenAllNotNull(host, port, database, user, password) {
            db = MySQLDatabase(
                host = it[0].toString(),
                port = it[1].toString().toInt(),
                database = it[2].toString(),
                user = it[3].toString(),
                password = it[4].toString()
            )
        }
        return db
    }

    fun getUserFromSender(sender: CommandSender, luckPerms: LuckPerms): User {
        return luckPerms.getPlayerAdapter(Player::class.java).getUser(sender as Player)
    }

    fun getUserFromNickname(nickname: String, luckPerms: LuckPerms): User? {
        val uuid = luckPerms.userManager.lookupUniqueId(nickname).get()
        return try {
            luckPerms.userManager.getUser(uuid)
        } catch (e: NullPointerException) {
            null
        }
    }

    fun addPlayerIfNotExists(player: User, rating: Int = 100) = runBlocking {
        db?.addPlayerRatingIfNotExists(player, rating)
    }

    fun addRating(player: User, changerPlayer: User, count: Int, reason: String) = runBlocking {
        db?.addRating(player, changerPlayer, count, reason)
    }

    fun removeRating(player: User, changerPlayer: User, count: Int, reason: String) = runBlocking {
        db?.removeRating(player, changerPlayer, count, reason)
    }

    fun getCurrentRating(player: User): Int? = runBlocking {
        db?.getCurrentRating(player)
    }

    fun getRatingHistory(player: User, page: Int = 1): PagedList<RatingHistory> = runBlocking {
        db?.getRatingHistory(player, page) ?: PagedList(page = 0, pageSize = 0, totalPages = 0, totalItems = 0, elements = emptyList())
    }

    fun close() {
        db?.close()
    }
}