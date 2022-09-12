package ru.kbats183.youtube.broadcastscheduler

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.FileInputStream
import java.io.FileOutputStream

@Serializable
data class Repository(val streams: MutableList<Stream>, val course: MutableList<Course>)

const val repositoryFileName = "repository.json"

@OptIn(ExperimentalSerializationApi::class)
fun loadRepository(): Repository {
    return FileInputStream(repositoryFileName).buffered().use {
        return@use Json.decodeFromStream<Repository>(it)
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun Repository.store() {
    FileOutputStream("repository.json").buffered().use {
        Json.encodeToStream(this, it)
    }
}
