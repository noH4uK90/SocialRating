package org.example.noh4uk.socialRating.command

import net.luckperms.api.LuckPerms
import net.luckperms.api.model.user.User
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.example.noh4uk.socialRating.SocialRating
import org.example.noh4uk.socialRating.core.SocialRatingCore
import org.example.noh4uk.socialRating.utils.Utils.Companion.isNumeric
import org.example.noh4uk.socialRating.models.ChangingRatingType
import org.example.noh4uk.socialRating.models.CommandType
import org.example.noh4uk.socialRating.utils.Permission
import org.example.noh4uk.socialRating.utils.Permissions
import org.example.noh4uk.socialRating.utils.sendAddRatingMessage
import org.example.noh4uk.socialRating.utils.sendDontExistPlayer
import org.example.noh4uk.socialRating.utils.sendHistoryMessage
import org.example.noh4uk.socialRating.utils.sendNoNumberMessage
import org.example.noh4uk.socialRating.utils.sendNoPermissionMessage
import org.example.noh4uk.socialRating.utils.sendPlayerCurrentRatingMessage
import org.example.noh4uk.socialRating.utils.sendRemoveHistoryElementMessage
import org.example.noh4uk.socialRating.utils.sendRemoveRatingMessage
import org.example.noh4uk.socialRating.utils.sendUnknownCommandMessage
import org.example.noh4uk.socialRating.utils.sendUsageMessage
import org.example.noh4uk.socialRating.utils.sendWhomAddRatingMessage
import org.example.noh4uk.socialRating.utils.sendWhomRemoveRatingMessage

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
            sender.sendPlayerCurrentRatingMessage(sender.name, currentRating)
            return
        }

        when (args[0]) {
            CommandType.Add.name.lowercase() -> {
                handleAddRating(sender, args)
                return
            }
            CommandType.Remove.name.lowercase() -> {
                if (args.size == 2) {
                    handleRemoveHistoryElement(sender, args)
                } else {
                    handleRemoveRating(sender, args)
                }
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
        sender.hasAnyPermission(Permissions.Add, Permissions.All) {
            if (args.size < 4) {
                sender.sendUsageMessage(CommandType.Add)
                return@hasAnyPermission
            }

            handleChangeRating(sender, args, ChangingRatingType.Add)
        }
    }

    private fun handleRemoveRating(sender: CommandSender, args: Array<String>) {
        sender.hasAnyPermission(Permissions.Remove, Permissions.All) {
            if (args.size < 4) {
                sender.sendUsageMessage(CommandType.Remove)
                return@hasAnyPermission
            }

            handleChangeRating(sender, args, ChangingRatingType.Remove)
        }
    }

    private fun handleHistoryCommand(sender: CommandSender, args: Array<String>) {
        when (args.size) {
            1 -> showHistory(sender)
            2 -> {
                val userOrPage = args[1]
                when {
                    userOrPage.isNumeric() -> showHistory(sender, null, userOrPage.toInt())
                    else -> {
                        sender.hasAnyPermission(Permissions.History, Permissions.All) {
                            showHistory(sender, userOrPage)
                        }
                    }
                }
            }
            3 -> {
                sender.hasAnyPermission(Permissions.History, Permissions.All) {
                    showHistory(sender, args[1], args[2].toIntOrNull())
                }
            }
            else -> sender.sendUsageMessage(CommandType.History)
        }
    }

    private fun handleCurrentRating(sender: CommandSender, args: Array<String>) {
        sender.hasAnyPermission(Permissions.Current, Permissions.All) {
            val user = checkPlayerExists(sender, args[0]) ?: return@hasAnyPermission

            val rating = core.getCurrentRating(user) ?: return@hasAnyPermission
            sender.sendPlayerCurrentRatingMessage(user.username ?: "", rating)
        }
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
        val receiver = Bukkit.getPlayer(user.uniqueId)

        if (!args[2].isNumeric()) {
            sender.sendNoNumberMessage()
            return
        }
        val count = args[2].toInt()
        val reason = args.slice(3 until args.size).joinToString(" ")
        when (type) {
            ChangingRatingType.Add ->  {
                core.addRating(user, changerPlayer, count, reason)
                sender.sendAddRatingMessage(user.username ?: "", count)
                if (receiver?.isOnline ?: false) {
                    receiver.sendWhomAddRatingMessage(changerPlayer.username ?: "", count)
                }
            }
            ChangingRatingType.Remove -> {
                core.removeRating(user, changerPlayer, count, reason)
                sender.sendRemoveRatingMessage(user.username ?: "", count)
                if (receiver?.isOnline ?: false) {
                    receiver.sendWhomRemoveRatingMessage(changerPlayer.username ?: "", count)
                }
            }
        }
    }

    private fun handleRemoveHistoryElement(sender: CommandSender, args: Array<String>) {
        sender.hasAnyPermission(Permissions.RemoveHistory, Permissions.All) {
            if (args.size < 2) {
                return@hasAnyPermission
            }

            core.removeHistoryElement(args[1])
            sender.sendRemoveHistoryElementMessage()
        }
    }

    fun CommandSender.hasPermission(permission: Permission, sendNoPermissionMessage: Boolean = true, completion: () -> Unit) {
        if (this.hasPermission(permission.full)) {
            completion()
            return
        }

        if (sendNoPermissionMessage) {
            this.sendNoPermissionMessage()
        }
        return
    }

    fun CommandSender.hasAnyPermission(vararg permissions: Permission, sendNoPermissionMessage: Boolean = true, completion: () -> Unit) {
        if (permissions.any { permission -> this.hasPermission(permission.full) }) {
            completion()
            return
        }

        if (sendNoPermissionMessage) {
            this.sendNoPermissionMessage()
        }
        return
    }
}