package ru.justagod.observer.base

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.hooks.EventListener

interface ObserverAugment {

    fun init(jda: JDA) : EventListener?

}