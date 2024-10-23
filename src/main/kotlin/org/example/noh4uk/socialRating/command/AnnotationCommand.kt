package org.example.noh4uk.socialRating.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.example.noh4uk.socialRating.SocialRating
import org.example.noh4uk.socialRating.command.annotation.Parameter
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.stream.Collectors

abstract class AnnotationCommand(open val command: String): CommandExecutor, TabCompleter {
    var methods: MutableMap<String, Method> = mutableMapOf()

    init {
        val pluginCommand = SocialRating.getInstance().getCommand(command)
        pluginCommand?.setExecutor(this)
        pluginCommand?.tabCompleter = this
        findMethods()
    }

    private fun findMethods() {
        for (method in this.javaClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Parameter::class.java)) {
                val parameter = method.getDeclaredAnnotation(Parameter::class.java)
                val modifiers = method.modifiers

                if (!Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)) {
                    return
                }

                this.methods.put(parameter.value, method)
            }
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        val param = if (args.isNotEmpty()) args[0] else ""
        val method = methods[param.lowercase()]

        if (method != null) {
            method.invoke(this)
        } else {
            sender.sendMessage("Usage: /{label} <${methods.keys.stream().filter({methodName -> !methodName.isNotEmpty()}).collect(
                Collectors.toList()).joinToString("|")}>")
            return false
        }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): List<String> {
        var result = mutableListOf<String>()
        if (args.size == 1) {
            val lastArg = if (args.isNotEmpty()) args[args.size - 1] else ""

            for (key in methods.keys) {
                if ("" != key) {
                    result.add(key)
                }
            }
            return complete(lastArg, result)
        }

        return listOf()
    }

    fun complete(partialName: String, all: Iterable<String>): List<String> {
        val tab = all.toMutableList()
        val lowerPartialName = partialName.lowercase()

        tab.removeAll { !it.lowercase().startsWith(lowerPartialName) }
        return tab.sorted()
    }
}