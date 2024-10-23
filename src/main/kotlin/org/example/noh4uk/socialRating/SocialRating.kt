package org.example.noh4uk.socialRating

import net.luckperms.api.LuckPerms
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.example.noh4uk.socialRating.command.RatingCommand
import org.example.noh4uk.socialRating.core.PlayerJoinListener
import org.example.noh4uk.socialRating.core.SocialRatingCore

class SocialRating : JavaPlugin() {
    private lateinit var core: SocialRatingCore
    private lateinit var luckPerms: LuckPerms

    companion object {
        @JvmStatic
        private lateinit var instance: SocialRating

        @JvmStatic
        fun getInstance(): SocialRating {
            return this.instance
        }

        @JvmStatic
        fun getCore(): SocialRatingCore {
            return instance.core
        }

        @JvmStatic
        fun getLuckPerms(): LuckPerms {
            return instance.luckPerms
        }
    }

    override fun onEnable() {

        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        instance = this

        val provider = Bukkit.getServicesManager().getRegistration(LuckPerms::class.java)
        if (provider != null) {
            luckPerms = provider.provider
        } else {
            logger.severe("Не удалось получить LuckPerms API")
            server.pluginManager.disablePlugin(this)
            return
        }

        saveDefaultConfig()
        core = SocialRatingCore()
        server.pluginManager.registerEvents(PlayerJoinListener(), this)
        RatingCommand()
        addPlayersIfNotExists()
    }

    private fun addPlayersIfNotExists() {
        val users = luckPerms.userManager.loadedUsers
        for (user in users) {
            core.addPlayerIfNotExists(user)
        }
    }

    override fun onDisable() {
        getCore().close()
        Bukkit.getScheduler().cancelTasks(this)
    }
}
