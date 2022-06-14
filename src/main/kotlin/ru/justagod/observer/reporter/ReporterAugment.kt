package ru.justagod.observer.reporter

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.hooks.ListenerAdapter
import ru.justagod.observer.base.ObserverAugment

object ReporterAugment : ObserverAugment, ListenerAdapter(){

    override fun init(jda: JDA): EventListener {
        return this
    }
}