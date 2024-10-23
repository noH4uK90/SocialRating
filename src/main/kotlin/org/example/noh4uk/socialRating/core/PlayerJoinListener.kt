package org.example.noh4uk.socialRating.core

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.example.noh4uk.socialRating.SocialRating

class PlayerJoinListener: Listener {
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = SocialRating.getLuckPerms().getPlayerAdapter(Player::class.java).getUser(event.player)
        SocialRating.getCore().addPlayerIfNotExists(player)
    }
}