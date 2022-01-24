package ru.justagod.observer.util

import net.dv8tion.jda.api.requests.RestAction


fun <T>RestAction<T>.completeOrNull(): T? {
    return try {
        complete()
    } catch (e: Exception) {
        null
    }
}