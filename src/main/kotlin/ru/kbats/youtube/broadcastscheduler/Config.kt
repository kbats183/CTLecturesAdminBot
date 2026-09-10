package ru.kbats.youtube.broadcastscheduler

import ru.kbats.youtube.broadcastscheduler.platforms.vk.VKApi

private fun requiredEnv(name: String): String =
    requireNotNull(System.getenv(name)) { "Missing required env: $name" }

private fun requiredLongEnv(name: String): Long =
    requiredEnv(name).toLongOrNull()
        ?: error("Env $name must be Long")

private fun optionalEnv(name: String, default: String): String =
    System.getenv(name) ?: default

private fun optionalIntEnv(name: String, default: String): Int =
    optionalEnv(name, default).toIntOrNull()
        ?: error("Env $name must be Int")

data class Config(
    val botApiToken: String,
    val mongoDBConnectionString: String,
    val mongoDBBase: String,
    val publicFilesUrl: String,
    val restreamerApiUrl: String,
    val vkConfig: VKApi.VKApiConfig,
    val ytEnv: String,
    val serverHost: String,
    val serverPort: Int
)

fun config(): Config {
    return Config(
        botApiToken = requiredEnv("BOT_TOKEN"),
        mongoDBConnectionString = requiredEnv("MONGO"),
        mongoDBBase = requiredEnv("MONGO_BASE"),
        publicFilesUrl = requiredEnv("FILES_ENV"),
        restreamerApiUrl = requiredEnv("RESTREAMER_API"),
        vkConfig = VKApi.VKApiConfig(
            userToken = requiredEnv("VK_TOKEN"),
            userId = requiredLongEnv("VK_USER"),
            groupId = requiredLongEnv("VK_GROUP"),
        ),
        ytEnv = optionalEnv("YT_ENV", "ct_lectures"),
        serverHost = optionalEnv("SERVER_HOST", "0.0.0.0"),
        serverPort = optionalIntEnv("SERVER_PORT", "8080")
    )
}

