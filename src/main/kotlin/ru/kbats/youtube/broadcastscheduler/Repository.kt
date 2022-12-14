package ru.kbats.youtube.broadcastscheduler

import org.bson.types.ObjectId
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.eq
import org.litote.kmongo.reactivestreams.KMongo
import ru.kbats.youtube.broadcastscheduler.data.Admin
import ru.kbats.youtube.broadcastscheduler.data.Lecture

class Repository(val db: CoroutineDatabase) {
    suspend fun getAdmins(): List<Admin> {
        return db.getCollection<Admin>().find().toList()
    }

    suspend fun addAdmin(login: String, commentary: String) {
        db.getCollection<Admin>().insertOne(Admin(login = login, comment = commentary))
    }

    suspend fun getLectures(): List<Lecture> {
        return db.getCollection<Lecture>().find().toList().sortedBy { it.name }
    }

    suspend fun getLecture(id: String): Lecture? {
        return db.getCollection<Lecture>().findOneById(ObjectId(id))
    }

    suspend fun updateLecture(id: String, mutator: (Lecture) -> Lecture) {
        val lecture = getLecture(id) ?: return
        db.getCollection<Lecture>().updateOne(Lecture::id eq lecture.id, mutator(lecture))
    }
}

fun getRepository(config: Config): Repository {
    val client = KMongo.createClient(config.mongoDBConnectionString).coroutine
    val db = client.getDatabase(config.mongoDBBase)
    return Repository(db)
}
