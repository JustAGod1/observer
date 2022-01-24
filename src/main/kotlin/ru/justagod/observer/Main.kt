package ru.justagod.observer

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import ru.justagod.observer.command.CommandManager
import ru.justagod.observer.qna.TechSupportAugment
import ru.justagod.observer.settings.SettingsManager
import java.util.logging.*

object Main {

    val guildId = System.getenv("GUILD_ID")!!
    lateinit var guild: Guild
        private set

    val logger: Logger
    val err: Logger

    lateinit var jda: JDA
        private set

    init {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1\$tF %1\$tT] [%3\$s] [%4\$s] %5\$s %n");

        val file = FileHandler("bot-output.log")
        file.level = Level.ALL
        file.formatter = SimpleFormatter()

        logger = Logger.getLogger("Main")
        logger.level = Level.ALL
        logger.addHandler(object:StreamHandler(System.out,  SimpleFormatter()) {
            init {
                level = Level.ALL
            }
            override fun publish(record: LogRecord?) {
                super.publish(record)
                flush()
            }
        })
        logger.addHandler(file)

        err = Logger.getLogger("Error")
        err.level = Level.ALL
        err.addHandler(file)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        logger.fine("Started")

        val token = System.getenv("BOT_TOKEN")!!

        jda  = JDABuilder.createDefault(token)
            .build()

        jda.addEventListener(object : ListenerAdapter() {
            override fun onReady(event: ReadyEvent) {
                initAugments(jda)
            }
        })

    }

    private fun initAugments(jda: JDA) {
        guild = jda.getGuildById(guildId)!!

        val augments = listOf(
            SettingsManager,
            CommandManager,
            TechSupportAugment
        )


        for (augment in augments) {
            val listener = augment.init(jda) ?: continue

            jda.addEventListener(listener)
        }

    }


}