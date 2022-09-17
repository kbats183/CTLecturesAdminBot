package ru.kbats.youtube.broadcastscheduler

import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.handlers.CallbackQueryHandlerEnvironment
import com.github.kotlintelegrambot.dispatcher.handlers.TextHandlerEnvironment
import com.github.kotlintelegrambot.dispatcher.text
import kotlinx.coroutines.runBlocking
import ru.kbats183.youtube.broadcastscheduler.Repository

typealias HandleTextSuspend = suspend TextHandlerEnvironment.() -> Unit
typealias HandleCallbackQuery = suspend CallbackQueryHandlerEnvironment.() -> Unit


class AdminDispatcher(private val repository: Repository, private val dispatcher: Dispatcher) {
    fun text(text: String? = null, handleText: HandleTextSuspend) {
        dispatcher.text(text) {
            runBlocking {
                if (repository.getUser().any { it.login == message.from?.username}) {
                    handleText()
                }
            }
        }
    }

    fun callbackQuery(data: String? = null, handleCallbackQuery: HandleCallbackQuery) {
        dispatcher.callbackQuery(data) {
            runBlocking {
                if (repository.getUser().any { it.login == callbackQuery.from.username }) {
                    handleCallbackQuery()
                }
            }
        }
    }
}

fun Dispatcher.withAdminRight(repository: Repository, body: AdminDispatcher.() -> Unit) {
    AdminDispatcher(repository, this).body()
}
