package ru.kbats.youtube.broadcastscheduler.data

import org.bson.codecs.pojo.annotations.BsonId
import org.litote.kmongo.Id
import org.litote.kmongo.newId
import kotlin.time.Duration

data class Admin(@BsonId val id: Id<Admin> = newId(), val login: String, val comment: String)

data class LectureThumbnails(
    val fileName: String,
    val textColor: String,
)

enum class LectureBroadcast {
    Public, LinkOnly
}

data class LectureBroadcastScheduling(
    val streamKey: String,
    val startDay: String,
    val startTime: Duration,
    val privacy: LectureBroadcast,
    val enable: Boolean,
    val enableAutoStart: Boolean,
    val enableAutoStop: Boolean,
)

data class Lecture(
    @BsonId val id: Id<Lecture> = newId(),
    val name: String,
    val title: String,
    val description: String,
    val currentLectureNumber: Int,
    val isDoubleNumeration: Boolean,
    val lectureNumberPrefix: String = "L",
    val thumbnails: LectureThumbnails?,
    val scheduling: LectureBroadcastScheduling?,
)
