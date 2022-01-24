package ru.justagod.observer.qna

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.ThreadChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.Button
import ru.justagod.observer.Main
import ru.justagod.observer.base.ObserverAugment
import ru.justagod.observer.settings.SettingsManager

object TechSupportAugment : ObserverAugment, ListenerAdapter(){

    private const val START_CONV_BUTTON_ID = "ts_start_conv"
    private const val ST_START_CHANNEL_ID = "ts_start_channel"
    private const val ST_MESSAGE_CONTENT = "ts_message_content"
    private const val ST_START_TICKET = "ts_ticket_start"

    private lateinit var channel: TextChannel

    override fun init(jda: JDA): EventListener {
        publishInitMessage()
        return this
    }

    private fun publishInitMessage() {
        channel = Main.guild.getTextChannelById(SettingsManager.getPreference(ST_START_CHANNEL_ID))!!

        val messageContent = SettingsManager.getPreference(ST_MESSAGE_CONTENT)

        val message = MessageBuilder(messageContent)
            .setActionRows(
                ActionRow.of(
                    Button.success(START_CONV_BUTTON_ID, "Создать обращение")
                )
            )
            .build()

        channel.sendMessage(message).complete()
    }

    override fun onButtonClick(event: ButtonClickEvent) {
        if (event.componentId == START_CONV_BUTTON_ID) {
            startTicket(event.user)
            event.reply("Обращение создано").queue()
        }
    }

    private fun startTicket(user: User) {
        channel.createThreadChannel("kek", false).also { it.setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_24_HOURS) }
            .queue {
            val message = MessageBuilder()
                .append(user)
                .append(",")
                .append(SettingsManager.getPreference(ST_START_TICKET))
                .build()

            it.sendMessage(message).queue()
        }

    }

    



}