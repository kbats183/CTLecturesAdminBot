package ru.kbats.youtube.broadcastscheduler

class Config(val botApiToken: String)

fun config(): Config {
    val botApiToken = System.getenv("BOT_TOKEN")
    return Config(botApiToken)
}
