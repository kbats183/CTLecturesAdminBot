package ru.kbats.youtube.broadcastscheduler.data

import org.bson.codecs.pojo.annotations.BsonId
import org.litote.kmongo.Id
import org.litote.kmongo.newId

data class Admin(@BsonId val id: Id<Admin> = newId(), val login: String, val comment: String)

data class LectureThumbnails(
    val fileName: String,
    val textColor: String,
)

enum class LectureBroadcastPrivacy {
    Public, Unlisted
}

data class LectureBroadcastScheduling(
    val startDay: Int,
    val startHour: Int,
    val startMinute: Int,
    val privacy: LectureBroadcastPrivacy,
    val enableScheduling: Boolean = false,
    val enableAutoStart: Boolean = false,
    val enableAutoStop: Boolean = false,
    val streamKeyId: String? = null,
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
    val playlistId: String? = null,
    val thumbnails: LectureThumbnails? = null,
    val scheduling: LectureBroadcastScheduling? = null,
    @BsonId val id: Id<Lecture> = newId(),
)
