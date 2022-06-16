package ru.justagod.observer.reporter

import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.internal.entities.DataMessage
import ru.justagod.observer.command.Command

object ReportCommand : Command("tell", "report") {
    override fun execute(args: String, message: Message) {
        val target = args.substringBefore('\n').trim()
        val text = message.contentRaw.substringAfter('\n', missingDelimiterValue = "").trim()

        if (text.isBlank() && message.attachments.isEmpty()) {
            message.reply("Не удалось отправить сообщение. Сообщение пустое").queue()
            return
        }

        val channel = message.guild.getTextChannelById(target) ?: message.guild.getNewsChannelById(target)

        if (channel == null) {
            message.reply("Не удалось отправить сообщение. Канал не найден.").queue()
            return
        }

        val intent = if (text.isBlank()) {
            val intent = channel.sendFile(message.attachments.first().retrieveInputStream().get(), message.attachments.first().fileName)

            for (attachment in message.attachments.drop(1)) {
                intent.addFile(attachment.retrieveInputStream().get(), attachment.fileName)
            }
            intent
        } else {
            val newMessage = MessageBuilder(message)
                .setContent(text)
                .build()
            val intent = channel.sendMessage(newMessage)
            message.attachments.forEach {
                intent.addFile(it.retrieveInputStream().get(), it.fileName)
            }

            intent
        }

        intent.complete()

        message.reply("Сообщение отправлено").queue()
    }
}