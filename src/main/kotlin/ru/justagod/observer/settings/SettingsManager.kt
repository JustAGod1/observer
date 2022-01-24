package ru.justagod.observer.settings

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Channel
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.message.GenericMessageEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.hooks.ListenerAdapter
import ru.justagod.observer.Main
import ru.justagod.observer.base.ObserverAugment
import kotlin.system.measureNanoTime

object SettingsManager : ObserverAugment, ListenerAdapter(){

    private const val settingsChannel = "935182057838104606"
    private lateinit var jda : JDA
    private lateinit var channel: TextChannel

    private val cache = hashMapOf<String, String>()

    fun getPreferenceOrNull(name: String): String? {
        return cache[name]
    }

    fun getPreference(name: String): String {
        return getPreferenceOrNull(name) ?: error("Can't find mandatory preference $name")
    }

    override fun init(jda: JDA): EventListener? {
        this.jda = jda
        channel = Main.guild.getTextChannelById(settingsChannel)!!
        updateCache()
        return null
    }

    override fun onGenericMessage(event: GenericMessageEvent) {
        if (event.channel.id == settingsChannel && event.guild.id == Main.guildId) updateCache()
    }

    private fun updateCache() {
        cache.clear()

        val messages = channel.history.retrievePast(100).complete()

        for (message in messages) {
            val content = message.contentRaw

            if (content.matches("[a-zA-Z_][0-9a-zA-Z_]*:(.|\\n)*".toRegex())) {
                cache[content.substringBefore(":")] = content.substringAfter(':').trim()
            }
        }
    }



}