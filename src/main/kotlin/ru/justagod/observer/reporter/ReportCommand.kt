package ru.justagod.observer.reporter

import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.internal.entities.DataMessage
import ru.justagod.observer.command.Command

object ReportCommand : Command("tell", "report") {
    override fun execute(args: String, message: Message) {
        val target = args.substringBefore('\n').trim()
        val text = message.contentRaw.substringAfter('\n').trim()

        val newMessage = MessageBuilder(message)
            .setContent(text)
                // adds attachments from message

            .build()


        val channel = message.guild.getTextChannelById(target)

        if (channel == null) {
            message.reply("Не удалось отправить сообщение. Канал не найден.").queue()
            return
        }

        val intent = channel.sendMessage(newMessage)

        message.attachments.forEach {
            intent.addFile(it.retrieveInputStream().get(), it.fileName)
        }

        intent.queue()

        message.reply("Сообщение отправлено").queue()
    }
}