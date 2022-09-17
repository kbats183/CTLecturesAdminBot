package ru.kbats.youtube.broadcastscheduler

import kotlinx.coroutines.runBlocking
import ru.kbats.youtube.broadcastscheduler.data.Lecture
import ru.kbats.youtube.broadcastscheduler.data.LectureThumbnails
import ru.kbats.youtube.broadcastscheduler.data.LectureType

fun main() {
    val config = config()
    val repository = getRepository(config)

    runBlocking {
        println("Hello")
        repository.db.getCollection<Lecture>().insertOne(
            Lecture(
                name = "MStat39",
                title = "[s5 | 2022] Математическая статистика, Иван Лимар",
                description = "",
                currentLectureNumber = 3,
                doubleNumeration = false,
                lectureType = LectureType.Lecture,
                thumbnails = LectureThumbnails("math_stat_3338_9.png", "tart"),
            )
        )
//        val lecture = repository.db.getCollection<Lecture>().findOne(Lecture::name eq "MStat39")!!
//        repository.db.getCollection<Lecture>().updateOne(Lecture::name eq "MStat39",
//            lecture.copy(
//                thumbnails = LectureThumbnails("math_stat_3338_9.png", "tart")
//            )
//        )
    }
}
