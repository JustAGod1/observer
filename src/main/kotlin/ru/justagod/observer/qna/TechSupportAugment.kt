package ru.justagod.observer.qna

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.Button
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import ru.justagod.observer.Main
import ru.justagod.observer.base.ObserverAugment
import ru.justagod.observer.command.Command
import ru.justagod.observer.command.CommandManager
import ru.justagod.observer.db.DatabaseManager
import ru.justagod.observer.settings.SettingsManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object TechSupportAugment : ObserverAugment, ListenerAdapter() {

    private const val START_CONV_BUTTON_ID = "ts_start_conv"
    const val END_CONV_BUTTON_YES_ID = "ts_end_conv_yes"
    const val END_CONV_BUTTON_NO_ID = "ts_end_conv_no"

    private const val ST_START_CHANNEL_ID = "ts_start_channel"
    private const val ST_MESSAGE_CONTENT = "ts_message_content"
    private const val ST_START_TICKET = "ts_ticket_start"
    private const val ST_SUPPORT_ROLE = "ts_support_role"

    private lateinit var channel: TextChannel

    lateinit var techSupportRole: Role
        private set

    private val openTickets = ConcurrentHashMap<Long, ChannelState>()

    override fun init(jda: JDA): EventListener {
        CommandManager.registerCommand(CloseTicketCommand)
        CommandManager.registerCommand(AssignTicketCommand)

        techSupportRole = Main.guild.getRoleById(SettingsManager.getPreference(ST_SUPPORT_ROLE))!!

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

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (!event.message.isEphemeral && event.channel.id == SettingsManager.getPreference(ST_START_CHANNEL_ID)) {
            if (event.message.type == MessageType.THREAD_CREATED) event.message.delete().queue()

        }

        val state = openTickets[event.channel.idLong] ?: return

        onState(event.channel.idLong) { state.message(event.message) }
    }

    override fun onButtonClick(event: ButtonClickEvent) {
        when (event.componentId) {
            START_CONV_BUTTON_ID -> {
                ticketsService.runCatching {
                    startTicket(event.user)
                }
                event.reply("Обращение создано").setEphemeral(true).queue()
            }
            END_CONV_BUTTON_NO_ID -> {
                ticketsService.runCatching {
                    val state = openTickets[event.channel.idLong]
                    event.message.editMessage(MessageBuilder(event.message).setActionRows(emptyList()).build()).queue() {
                        if (state == null) {
                            return@queue
                        }
                        onState(event.channel.idLong) { state.no(event.user) }
                    }
                }
            }
            END_CONV_BUTTON_YES_ID -> {
                ticketsService.runCatching {
                    val state = openTickets[event.channel.idLong]
                    event.message.editMessage(MessageBuilder(event.message).setActionRows(emptyList()).build()).queue() {
                        if (state == null) {
                            return@queue
                        }
                        onState(event.channel.idLong) { state.yes(event.user) }
                    }
                }
            }
        }
    }

    private val ticketsService = Executors.newSingleThreadExecutor()

    private fun ExecutorService.runCatching(block: () -> Unit) {
        this.execute {
            try {
                block()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startTicket(user: User) {
        val id = transaction {
            DatabaseManager.TicketsCreation.slice(DatabaseManager.TicketsCreation.id).selectAll()
                .orderBy(DatabaseManager.TicketsCreation.id, SortOrder.DESC)
                .limit(1)
                .firstOrNull()?.get(DatabaseManager.TicketsCreation.id)?.plus(1) ?: 1
        }

        val channel = channel.createThreadChannel("ticket-$id", true)
            .also {
                it.setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_24_HOURS)
                it.setInvitable(false)
            }.complete()


        val message = MessageBuilder()
            .append(user)
            .append(",")
            .append(SettingsManager.getPreference(ST_START_TICKET))
            .build()

        channel.sendMessage(message).queue()

        openTickets[channel.idLong] = Pending(channel, id)

        transaction {
            DatabaseManager.TicketsCreation.insert {
                it[this.user] = user.id
                it[this.channelId] = channel.idLong
            }
        }
    }

    private fun onState(id: Long, block: () -> ChannelState?) {
        val result = block()

        if (result == null) {
            this.openTickets -= id
        } else {
            this.openTickets[id] = result
        }
    }

    private abstract class TicketCommand(name: String, vararg aliases: String) : Command(name, *aliases) {
        override fun execute(args: String, message: Message) {
            ticketsService.runCatching {
                if (message.member?.roles?.contains(techSupportRole) != true) return@runCatching

                val state = openTickets[message.channel.idLong]

                if (state == null) {
                    val msg = MessageBuilder("Данный канал не модерируется как тикет").build()
                    message.channel.sendMessage(msg)
                    return@runCatching
                }

                onState(message.channel.idLong) { doExecute(args, message, state) }
            }
        }

        abstract fun doExecute(args: String, message: Message, state: ChannelState): ChannelState?
    }


    private object CloseTicketCommand : TicketCommand("close-ticket", "зт") {
        override fun doExecute(args: String, message: Message, state: ChannelState): ChannelState? {
            return state.close(message)
        }
    }

    private object AssignTicketCommand : TicketCommand("assign-ticket", "переназнач") {
        override fun doExecute(args: String, message: Message, state: ChannelState): ChannelState? {
            return state.assign(message.author)
        }
    }

}