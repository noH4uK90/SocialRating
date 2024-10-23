package org.example.noh4uk.socialRating.command

import net.luckperms.api.LuckPerms
import net.luckperms.api.model.user.User
import org.bukkit.command.CommandSender
import org.example.noh4uk.socialRating.SocialRating
import org.example.noh4uk.socialRating.core.SocialRatingCore
import org.example.noh4uk.socialRating.core.Utils.Companion.isNumeric
import org.example.noh4uk.socialRating.core.models.ChangingRatingType
import org.example.noh4uk.socialRating.core.models.CommandType
import org.example.noh4uk.socialRating.core.models.Permissions
import org.example.noh4uk.socialRating.core.sendDontExistPlayer
import org.example.noh4uk.socialRating.core.sendHistoryMessage
import org.example.noh4uk.socialRating.core.sendNoNumberMessage
import org.example.noh4uk.socialRating.core.sendNoPermissionMessage
import org.example.noh4uk.socialRating.core.sendPlayerCurrentRatingMessage
import org.example.noh4uk.socialRating.core.sendUnknownCommandMessage
import org.example.noh4uk.socialRating.core.sendUsageMessage

class RatingCommand() : AbstractCommand("rating") {
    private val luckPerms: LuckPerms = SocialRating.getLuckPerms()
    private val core: SocialRatingCore = SocialRating.getCore()

    override fun execute(
        sender: CommandSender,
        label: String,
        args: Array<String>
    ) {
        val executor = core.getUserFromSender(sender, luckPerms)
        if(args.isEmpty()) {
            val currentRating = core.getCurrentRating(executor) ?: return
            sender.sendPlayerCurrentRatingMessage(currentRating)
            return
        }

        when (args[0]) {
            CommandType.Add.name.lowercase() -> {
                handleAddRating(sender, args)
                return
            }
            CommandType.Remove.name.lowercase() -> {
                handleRemoveRating(sender, args)
                return
            }
            CommandType.History.name.lowercase() -> {
                handleHistoryCommand(sender, args)
                return
            }
            else -> {
                handleCurrentRating(sender, args)
                return
            }
        }

        sender.sendUnknownCommandMessage(args[0])
        return
    }

    override fun complete(sender: CommandSender, arg: Array<String>): List<String> {
        val completions = mutableListOf<String>()

        when (arg.size) {
            1 -> {
                listOf(
                    Permissions.Add to "add",
                    Permissions.Remove to "remove",
                )
                    .filter { (permission, _) -> sender.hasPermission(permission.full) }
                    .mapTo(completions) { (_, command) -> command }
                completions.add("history")
                if (sender.hasPermission(Permissions.Current.full)) {
                    completions.addAll(luckPerms.userManager.loadedUsers.mapNotNull { it.username })
                }
            }

            2 -> {
                when (arg[0].lowercase()) {
                    "add", "remove", "history" -> {
                        if (listOf(Permissions.Add, Permissions.Remove, Permissions.History).any { permission -> sender.hasPermission(permission.full) }) {
                            completions.addAll(luckPerms.userManager.loadedUsers.mapNotNull { it.username })
                        }
                    }
                }
            }
        }

        return completions.filter { it.startsWith(arg.last(), ignoreCase = true) }
    }

    private fun handleAddRating(sender: CommandSender, args: Array<String>) {
        if(!sender.checkPermission(Permissions.Add)) return

        if (args.size < 4) {
            sender.sendUsageMessage(CommandType.Add)
            return
        }

        handleChangeRating(sender, args, ChangingRatingType.Add)
        return
    }

    private fun handleRemoveRating(sender: CommandSender, args: Array<String>) {
        if(!sender.checkPermission(Permissions.Remove)) return

        if (args.size < 4) {
            sender.sendUsageMessage(CommandType.Remove)
            return
        }

        handleChangeRating(sender, args, ChangingRatingType.Remove)
        return
    }

    private fun handleHistoryCommand(sender: CommandSender, args: Array<String>) {
        when (args.size) {
            1 -> showHistory(sender)
            2 -> {
                val userOrPage = args[1]
                when {
                    userOrPage.isNumeric() -> showHistory(sender, null, userOrPage.toInt())
                    else -> {
                        if(!sender.checkPermission(Permissions.History)) return

                        showHistory(sender, userOrPage)
                    }
                }
            }
            3 -> {
                if(!sender.checkPermission(Permissions.History)) return

                showHistory(sender, args[1], args[2].toIntOrNull())
            }
            else -> sender.sendUsageMessage(CommandType.History)
        }
    }

    private fun handleCurrentRating(sender: CommandSender, args: Array<String>) {
        if (!sender.checkPermission(Permissions.Current)) return

        val user = checkPlayerExists(sender, args[0]) ?: return

        val rating = core.getCurrentRating(user) ?: return
        sender.sendPlayerCurrentRatingMessage(rating)
    }

    private fun showHistory(sender: CommandSender, targetPlayer: String? = null, page: Int? = null) {
        val user = when {
            targetPlayer != null -> core.getUserFromNickname(targetPlayer, luckPerms)
            else -> core.getUserFromSender(sender, luckPerms)
        }

        if (user == null) {
            sender.sendDontExistPlayer(targetPlayer ?: "")
            return
        }

        val history = when (page) {
            null -> core.getRatingHistory(user)
            else -> core.getRatingHistory(user, page)
        }

        sender.sendHistoryMessage(user, history)
    }

    private fun checkPlayerExists(sender: CommandSender, username: String): User? {
        val player = core.getUserFromNickname(username, luckPerms)

        if (player == null) {
            sender.sendDontExistPlayer(username)
            return null
        }
        return player
    }

    private fun handleChangeRating(sender: CommandSender, args: Array<String>, type: ChangingRatingType) {
        val user = checkPlayerExists(sender, args[1]) ?: return
        val changerPlayer = core.getUserFromSender(sender, luckPerms)

        if (!args[2].isNumeric()) {
            sender.sendNoNumberMessage()
            return
        }
        val count = args[2].toInt()
        val reason = args.slice(3 until args.size).joinToString(" ")
        when (type) {
            ChangingRatingType.Add -> core.addRating(user, changerPlayer, count, reason)
            ChangingRatingType.Remove -> core.removeRating(user, changerPlayer, count, reason)
        }
    }

    private fun CommandSender.checkPermission(permissions: Permissions): Boolean {
        if (!this.hasPermission(permissions.full)) {
            this.sendNoPermissionMessage()
            return false
        }
        return true
    }
}