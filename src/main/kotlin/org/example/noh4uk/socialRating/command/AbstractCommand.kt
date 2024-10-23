package org.example.noh4uk.socialRating.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.example.noh4uk.socialRating.SocialRating

abstract class AbstractCommand(open val command: String) : CommandExecutor, TabCompleter {

    init {
        val pluginCommand = SocialRating.getInstance().getCommand(command)
        pluginCommand?.setExecutor(this)
        pluginCommand?.tabCompleter = this
    }

    abstract fun execute(sender: CommandSender, label: String, args: Array<String>)

    open fun complete(sender: CommandSender, arg: Array<String>): List<String> {
        return listOf()
    }

    override fun onCommand(p0: CommandSender, p1: Command, p2: String, p3: Array<String>): Boolean {
        execute(sender = p0, label = p2, args = p3)
        return true
    }

    override fun onTabComplete(p0: CommandSender, p1: Command, p2: String, p3: Array<String>): List<String> {
        return filter(complete(p0, p3), p3)
    }

    private fun filter(list: List<String>, args: Array<String>): List<String> {
        if (list.isEmpty()) {
            return listOf()
        }
        val last = args[args.size - 1]
        var result = mutableListOf<String>()
        for (arg in list) {
            if (arg.lowercase().startsWith(last.lowercase())) {
                result.add(arg)
            }
        }
        return result
    }
}