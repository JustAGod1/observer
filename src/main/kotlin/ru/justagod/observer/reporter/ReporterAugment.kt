package ru.justagod.observer.reporter

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.hooks.ListenerAdapter
import ru.justagod.observer.base.ObserverAugment
import ru.justagod.observer.command.CommandManager

object ReporterAugment : ObserverAugment{

    override fun init(jda: JDA): EventListener? {
        CommandManager.registerCommand(ReportCommand)

        return null
    }
}