package ru.kbats.youtube.broadcastscheduler

import kotlinx.coroutines.runBlocking
import org.litote.kmongo.eq
import ru.kbats.youtube.broadcastscheduler.data.*

fun main() {
    val config = config()
    val repository = getRepository(config)

    runBlocking {
        println("Executing ...")
        repository.db.getCollection<Lecture>().insertOne(
            Lecture(
                name = "MStat37pr",
                title = "[s5 | 2022] Математическая статистика, Алексей Блаженов",
                description = "Записи курса математической статистики, который читается для студентов третьего года обучения программы «Прикладная математика и информатика» факультета ИТиП университета ИТМО в пятом семестре\n" +
                        "Лектор: Алексей Блаженов\n" +
                        "Все видео: https://www.youtube.com/playlist?list=PLd7QXkfmSY7az1ji61udRscvJ_Zwnhl51",
                currentLectureNumber = 3,
                doubleNumeration = false,
                lectureType = LectureType.Practice,
                thumbnails = LectureThumbnails("math_stat_3334_7_pr.png", "capri"),
                scheduling = LectureBroadcastScheduling(
                    0,
                    15,
                    20,
                    LectureBroadcastPrivacy.Public,
                    streamKeyId = "c8_XiJXPMz699NvDmtGoTA1631688763462572"
                )
            )
        )
//        val lecture = repository.db.getCollection<Lecture>().findOne(Lecture::name eq "MStat39")!!
//        repository.db.getCollection<Lecture>().updateOne(
//            Lecture::name eq "MStat39",
//            lecture.copy(
//                description = "Записи курса математической статистики, который читается для студентов третьего года обучения программы «Прикладная математика и информатика» факультета ИТиП университета ИТМО в пятом семестре\n" +
//                        "Лектор: Иван Лимар\n" +
//                        "Все видео: https://www.youtube.com/playlist?list=PLd7QXkfmSY7a2qy9JsJIe-sFhiS5M1cs9"
//            )
//        )
        println("Ready")
    }
}
