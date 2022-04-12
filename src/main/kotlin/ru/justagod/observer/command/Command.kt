package ru.justagod.observer.command

import net.dv8tion.jda.api.entities.Message

abstract class Command(val name: String, vararg val aliases: String) {

    abstract fun execute(args: String, message: Message)

}