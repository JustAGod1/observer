package ru.justagod.observer.qna

import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Channel
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.ThreadChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.Button
import net.dv8tion.jda.internal.entities.ThreadChannelImpl
import ru.justagod.observer.Main

sealed class ChannelState(protected val channel: ThreadChannel, val id: Int) {
    abstract fun yes(user: User) : ChannelState?
    abstract fun no(user: User) : ChannelState?
    abstract fun close(message: Message) : ChannelState?
    abstract fun message(message: Message): ChannelState?
    abstract fun assign(user: User) : ChannelState?

    fun archive() {
        channel.manager.setLocked(true).and(channel.manager.setArchived(true)).queue()
    }
}


class Pending(channel: ThreadChannel, id: Int): ChannelState(channel, id) {
    override fun yes(user: User) : ChannelState? {
        Main.logger.severe("$user said yes to proposal to close ticket before it has been assigned. So I closing it. Ticket: $id")
        archive()
        return null
    }

    override fun no(user: User) : ChannelState {
        Main.logger.severe("$user said no to proposal to close ticket before it has been assigned. Ticket: $id")
        return this
    }

    override fun close(message: Message) : ChannelState {
        channel.sendMessage("Они сказали мне закрыть мне эту дискусию но она даже агенту не назначена")
        Main.logger.severe("${message.author} requested to close ticket before it's been even assigned. Ticket: $id")
        return this
    }

    override fun message(message: Message): ChannelState {
        if (message.member?.roles?.contains(TechSupportAugment.techSupportRole) == true) {
            channel.manager.setName("ticket-$id-${message.author.discriminator}").queue()
            return InProgress(message.author, channel, id)
        }
        return this
    }

    override fun assign(user: User): ChannelState {
        return InProgress(user, channel, id)
    }

}

class InProgress(val agent: User, channel: ThreadChannel, id: Int) : ChannelState(channel, id) {
    override fun yes(user: User) : ChannelState? {
        archive()
        return null
    }

    override fun no(user: User) : ChannelState {
        return this
    }

    override fun close(message: Message) : ChannelState {
        if (message.author.id != agent.id) {
            Main.logger.info("${message.author.name} requested to close ticket $id while agent is ${agent.name}")
        }
        val closingMessage = MessageBuilder("Скажите была ли решена ваша проблема?")
            .setActionRows(
                ActionRow.of(Button.success(TechSupportAugment.END_CONV_BUTTON_YES_ID, "Проблема решена")),
                ActionRow.of(Button.danger(TechSupportAugment.END_CONV_BUTTON_NO_ID, "Проблема не решена"))
            )
            .build()
        message.delete().and(message.channel.sendMessage(closingMessage)).queue()
        return this
    }

    override fun message(message: Message): ChannelState {
        return this
    }

    override fun assign(user: User): ChannelState {
        return InProgress(user, channel, id)
    }

}