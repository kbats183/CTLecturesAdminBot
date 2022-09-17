package ru.kbats.youtube.broadcastscheduler

import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import ru.kbats.youtube.broadcastscheduler.data.Admin

class Repository(val db: CoroutineDatabase) {
    suspend fun getUser(): List<Admin> {
        return db.getCollection<Admin>().find().toList()
    }
}

fun getRepository(config: Config): Repository {
    val client = KMongo.createClient(config.mongoDBConnectionString).coroutine
    val db = client.getDatabase(config.mongoDBBase)
    return Repository(db)
}
