package ru.kbats183.youtube.broadcastscheduler

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Stream(val name: String, val youTubeID: String, val youTubeKey: String?)

@Serializable
data class Course(
    val key: String,
    val nextDateTime: LocalDateTime,
    val nameTemplate: String,
    val description: String,
    val lastNumber: Int,
    val numerationMode: Int,
    val playlistID: String,
)
