package org.example.noh4uk.socialRating.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer
import net.luckperms.api.model.user.User
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.example.noh4uk.socialRating.SocialRating
import org.example.noh4uk.socialRating.utils.Utils.Companion.getColoredType
import org.example.noh4uk.socialRating.utils.Utils.Companion.getRatingColor
import org.example.noh4uk.socialRating.models.CommandType
import org.example.noh4uk.socialRating.models.PagedList
import org.example.noh4uk.socialRating.models.RatingHistory

private val miniMessage = MiniMessage.miniMessage()
private val config = SocialRating.getInstance().config

fun CommandSender.sendHistoryMessage(user: User, history: PagedList<RatingHistory>) {
    val header = config.getString("messages.history.header") ?: return
    val body = config.getString("messages.history.body") ?: return
    val footer = config.getString("messages.history.footer") ?: return
    val delete = config.getString("messages.history.delete") ?: return

    val parsedHeader = miniMessage.deserialize(header.replace("{player}", user.username ?: return))
    val parsedFooter = miniMessage.deserialize(
        footer
            .replace("{page}", history.page.toString())
            .replace("{totalPages}", history.totalPages.toString())
            .replace("{totalItems}", history.totalItems.toString())
    )
    val parsedBody = Component.text()

    for (element in history.elements) {
        parsedBody.append(JSONComponentSerializer.json().deserialize(delete.replace("{id}", element.id.toString())).appendSpace())
        parsedBody.append(miniMessage.deserialize(
            body
                .replace("{date}", element.date)
                .replace("{player}", "<${getColoredType(element.type)}>${user.username}</${getColoredType(element.type)}>")
                .replace("{count}", element.count.toString())
                .replace("{changer}", element.playerChanger)
                .replace("{type}",  "<${getColoredType(element.type)}>${element.type.name}</${getColoredType(element.type)}>")
                .replace("{reason}", element.reason)
        ))
    }
    parsedBody.build()

    this.sendMessage(parsedHeader.append(parsedBody).append(parsedFooter))
}

fun CommandSender.sendDontExistPlayer(username: String) {
    val dontExistsPlayer = config.getString("messages.dontExistPlayer") ?: return

    this.sendMessage(miniMessage.deserialize(dontExistsPlayer.replace("{username}", username)))
}

fun CommandSender.sendNoPermissionMessage() {
    val noPermission = config.getString("messages.noPermission") ?: return

    this.sendMessage(miniMessage.deserialize(noPermission))
}

fun CommandSender.sendUsageMessage(type: CommandType) {
    val usage = config.getString("messages.usage.${type.name.lowercase()}") ?: return

    this.sendMessage(miniMessage.deserialize(usage))
}

fun CommandSender.sendUnknownCommandMessage(command: String) {
    val unknownCommand = config.getString("messages.unknownCommand") ?: return

    this.sendMessage(miniMessage.deserialize(unknownCommand.replace("{command}", command)))
}

fun CommandSender.sendNoNumberMessage() {
    val noNumber = config.getString("messages.noNumber") ?: return

    this.sendMessage(miniMessage.deserialize(noNumber))
}

fun CommandSender.sendPlayerCurrentRatingMessage(username: String, rating: Int) {
    val currentRating = config.getString("messages.currentRating") ?: return
    var message = currentRating.replace("{username}", username)

    message = if (!config.getBoolean("colors.rating.enabled")) {
        message.replace("{rating}", rating.toString())
    } else {
        val color = getRatingColor(rating)
        if (color != null) {
            message.replace("{rating}", "<$color>$rating</$color>")
        } else {
            message.replace("{rating}", rating.toString())
        }
    }
    this.sendMessage(miniMessage.deserialize(message))
}

fun CommandSender.sendAddRatingMessage(username: String, rating: Int) {
    val addRating = config.getString("messages.rating.add.who") ?: return

    this.sendMessage(miniMessage.deserialize(addRating.replace("{username}", username).replace("{rating}", rating.toString())))
}

fun CommandSender.sendRemoveRatingMessage(username: String, rating: Int) {
    val addRating = config.getString("messages.rating.remove.who") ?: return

    this.sendMessage(miniMessage.deserialize(addRating.replace("{username}", username).replace("{rating}", rating.toString())))
}

fun Player.sendWhomAddRatingMessage(username: String, rating: Int) {
    val addRating = config.getString("messages.rating.add.whom") ?: return

    this.sendMessage(miniMessage.deserialize(addRating.replace("{username}", username).replace("{rating}", rating.toString())))
}

fun Player.sendWhomRemoveRatingMessage(username: String, rating: Int) {
    val addRating = config.getString("messages.rating.remove.whom") ?: return

    this.sendMessage(miniMessage.deserialize(addRating.replace("{username}", username).replace("{rating}", rating.toString())))
}

fun CommandSender.sendRemoveHistoryElementMessage() {
    this.sendMessage("Вы удалили запись о рейтинге")
}