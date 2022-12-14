package ru.kbats.youtube.broadcastscheduler.bot

import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.handlers.CallbackQueryHandlerEnvironment
import com.github.kotlintelegrambot.dispatcher.handlers.TextHandlerEnvironment
import com.github.kotlintelegrambot.dispatcher.text
import kotlinx.coroutines.runBlocking
import ru.kbats.youtube.broadcastscheduler.Repository
import java.lang.Integer.min

typealias HandleTextSuspend = suspend TextHandlerEnvironment.() -> Unit
typealias HandleCallbackQuery = suspend CallbackQueryHandlerEnvironment.() -> Unit


class AdminDispatcher(private val repository: Repository, private val dispatcher: Dispatcher) {
    fun text(text: String? = null, handleText: HandleTextSuspend) {
        dispatcher.text(text) {
            runBlocking {
                if (repository.getAdmins().any { it.login == message.from?.username }) {
                    println("User ${message.chat.username} send text `${message.text}`")
                    handleText()
                }
            }
        }
    }

    fun callbackQuery(data: String? = null, handleCallbackQuery: HandleCallbackQuery) {
        dispatcher.callbackQuery(data) {
            runBlocking {
                if (repository.getAdmins().any { it.login == callbackQuery.from.username }) {
                    println(
                        "User ${callbackQuery.from.username} send callback ${callbackQuery.data} from message " +
                                "`${callbackQuery.message?.text?.let { it.substring(0, min(50, it.length)) }}`"
                    )
                    handleCallbackQuery()
                }
            }
        }
    }
}

fun Dispatcher.withAdminRight(repository: Repository, body: AdminDispatcher.() -> Unit) {
    AdminDispatcher(repository, this).body()
}
