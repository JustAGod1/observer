package ru.justagod.observer.command

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.hooks.ListenerAdapter
import ru.justagod.observer.Main
import ru.justagod.observer.base.ObserverAugment
import ru.justagod.observer.settings.SettingsManager

object CommandManager : ObserverAugment, ListenerAdapter() {

    private const val ST_PREFIX = "command_prefix"

    private lateinit var prefix: String
    private val commands = hashMapOf<String, Command>()

    fun registerCommand(command: Command) {
        commands[command.name] = command
        for (alias in command.aliases) {
            commands[alias] = command
        }
    }

    override fun init(jda: JDA): EventListener {
        prefix = SettingsManager.getPreference(ST_PREFIX)
        return this
    }

    private fun checkPerm(commandName: String, event: MessageReceivedEvent): Boolean {
        val user = event.member ?: return false
        val groups = SettingsManager.getPreference("perm.$commandName").split(",").toSet()
        if (groups.isEmpty()) {
            Main.logger.finest("User ${event.author.name} has no permissions for command $commandName")
            return false
        }

        return user.roles.any { it.id in groups }

    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.guild.id == Main.guildId) {
            val content = event.message.contentRaw

            if (content.startsWith(prefix)) {
                val commandName = content.substringAfter(prefix).substringBefore(' ')
                val command = commands[commandName] ?: return

                if (checkPerm(commandName, event)) {
                    command.execute(content.substringAfter(commandName), event.message)
                }
            }

        }
    }
}