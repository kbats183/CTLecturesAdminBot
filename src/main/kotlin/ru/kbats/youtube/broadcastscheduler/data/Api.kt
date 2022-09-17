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

enum class LectureType {
    Lecture, Practice
}

data class Lecture(
    val name: String,
    val title: String,
    val description: String,
    val currentLectureNumber: Int,
    val doubleNumeration: Boolean,
    val lectureType: LectureType = LectureType.Lecture,
    val thumbnails: LectureThumbnails? = null,
    val scheduling: LectureBroadcastScheduling? = null,
    @BsonId val id: Id<Lecture> = newId(),
)
