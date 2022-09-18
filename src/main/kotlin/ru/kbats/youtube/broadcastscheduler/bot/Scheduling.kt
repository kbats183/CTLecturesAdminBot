package ru.kbats.youtube.broadcastscheduler.bot

import com.google.api.services.youtube.model.LiveBroadcast
import ru.kbats.youtube.broadcastscheduler.Application
import ru.kbats.youtube.broadcastscheduler.data.Lecture
import ru.kbats.youtube.broadcastscheduler.data.LectureType
import ru.kbats.youtube.broadcastscheduler.getNextDayTimeInstant
import ru.kbats.youtube.broadcastscheduler.thumbnail.Thumbnail
import kotlin.io.path.Path

fun Lecture.currentThumbnailLecture(): String {
    val numbers =
        if (doubleNumeration) " $currentLectureNumber-${currentLectureNumber + 1}" else "$currentLectureNumber"
    return when (lectureType) {
        LectureType.Lecture -> "L"
        LectureType.Practice -> "P"
    } + numbers
}

fun Lecture.currentTitle(): String {
    val numbers =
        if (doubleNumeration) " $currentLectureNumber-${currentLectureNumber + 1}" else "$currentLectureNumber"
    return "$title, " + when (lectureType) {
        LectureType.Lecture -> if (doubleNumeration) "лекции" else "лекция"
        LectureType.Practice -> if (doubleNumeration) "практики" else "практика"
    } + " " + numbers
}

fun Application.scheduleStream(lecture: Lecture): LiveBroadcast? {
    lecture.scheduling ?: return null
    val broadcast = youtubeApi.createBroadcast(
        title = lecture.currentTitle(),
        description = lecture.description,
        startTime = getNextDayTimeInstant(
            lecture.scheduling.startDay,
            lecture.scheduling.startHour,
            lecture.scheduling.startMinute
        ),
        privacy = lecture.scheduling.privacy
    )
    broadcast ?: return null
    if (lecture.thumbnails != null) {
        try {
            val generateFile = Thumbnail.generate(
                Path(Application.thumbnailsDirectory),
                lecture.thumbnails,
                lecture.currentThumbnailLecture()
            )
            youtubeApi.uploadBroadcastThumbnail(broadcast.id, generateFile)
        } catch (e: Thumbnail.ThumbnailGenerationException) {
            println(e)
        }
    }
    if (lecture.scheduling.streamKeyId != null) {
        return youtubeApi.bindBroadcastStream(broadcast.id, lecture.scheduling.streamKeyId)
    }
    return youtubeApi.getBroadcast(broadcast.id)
}

