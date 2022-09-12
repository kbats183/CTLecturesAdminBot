package ru.kbats.youtube.broadcastscheduler

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.handlers.CallbackQueryHandlerEnvironment
import com.github.kotlintelegrambot.dispatcher.handlers.HandleCallbackQuery
import com.github.kotlintelegrambot.dispatcher.handlers.HandleText
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.google.api.services.youtube.model.LiveBroadcast
import com.google.api.services.youtube.model.LiveBroadcastStatus
import com.google.api.services.youtube.model.LiveStream
import ru.kbats.youtube.broadcastscheduler.states.BotUserState
import ru.kbats.youtube.broadcastscheduler.states.UserStateStorage
import ru.kbats.youtube.broadcastscheduler.youtube.YoutubeApi
import ru.kbats.youtube.broadcastscheduler.youtube.getCredentials

class Application(private val config: Config) {
    private val youtubeApi = YoutubeApi(getCredentials(System.getenv("YT_ENV") ?: "ct_lectures")!!)
    private val admins = listOf(316671439L)
    private val userStates = UserStateStorage()

    fun run() {
        val bot = bot {
            token = config.botApiToken
            dispatch {
                withAdminRight {
                    text {
                        when (userStates[message.chat.id]) {
                            is BotUserState.CreatingNewLiveStream -> {
                                val newLiveStream = youtubeApi.createStream(text)
                                bot.sendMessage(ChatId.fromId(message.chat.id), newLiveStream.infoMessage())
                                userStates[message.chat.id] = BotUserState.Default
                            }
                            else -> {
                                bot.sendMessage(
                                    ChatId.fromId(message.chat.id), text = "Main menu",
                                    replyMarkup = InlineButtons.mainMenu,
                                )
                            }
                        }
                    }
                    callbackQuery("HideCallbackMessageCmd") {
                        bot.deleteMessage(
                            chatId = callbackQuery.message?.chat?.id?.let { ChatId.fromId(it) } ?: return@callbackQuery,
                            messageId = callbackQuery.message?.messageId ?: return@callbackQuery,
                        )
                    }
                    callbackQuery("LiveStreamsCmd") {
                        bot.sendMessage(
                            ChatId.fromId(callbackQuery.from.id),
                            text = "LiveStreams",
                            replyMarkup = InlineButtons.streamsMenu,
                        )
                    }
                    callbackQuery("LiveStreamsNewCmd") {
                        bot.sendMessage(ChatId.fromId(callbackQuery.from.id), text = "Enter name of new stream")
                        userStates[callbackQuery.from.id] = BotUserState.CreatingNewLiveStream
                    }
                    callbackQuery("LiveStreamsListCmd") {
                        val liveStreams = youtubeApi.getStreams()
                        bot.sendMessage(
                            ChatId.fromId(callbackQuery.from.id), text = "List of streams",
                            replyMarkup = InlineButtons.streamsNav(liveStreams)
                        )
                    }
                    callbackQuery("LiveStreamsItemCmd") {
                        val id = callbackQueryId("LiveStreamsItemCmd") ?: return@callbackQuery
                        val stream = youtubeApi.getStream(id)
                        if (stream == null) {
                            bot.sendMessage(ChatId.fromId(callbackQuery.from.id), text = "Stream not found")
                            return@callbackQuery
                        }
                        bot.sendMessage(
                            ChatId.fromId(callbackQuery.from.id),
                            text = stream.infoMessage(),
                            replyMarkup = InlineButtons.streamManage(stream)
                        )
                    }
                    callbackQuery("LiveStreamsItemRefreshCmd") {
                        val id = callbackQueryId("LiveStreamsItemRefreshCmd") ?: return@callbackQuery
                        val stream = youtubeApi.getStream(id) ?: return@callbackQuery
                        bot.editMessageText(
                            chatId = callbackQuery.message?.chat?.id?.let { ChatId.fromId(it) },
                            messageId = callbackQuery.message?.messageId,
                            text = stream.infoMessage(),
                            replyMarkup = InlineButtons.streamManage(stream)
                        )
                    }
                    callbackQuery("BroadcastsCmd") {
                        bot.sendMessage(
                            ChatId.fromId(callbackQuery.from.id), text = "Broadcasts",
                            replyMarkup = InlineButtons.broadcastsMenu
                        )
                    }
                    callbackQuery("BroadcastsActiveCmd") {
                        val broadcasts = youtubeApi.getBroadcasts("active") + youtubeApi.getBroadcasts("upcoming")
                        bot.sendMessage(
                            ChatId.fromId(callbackQuery.from.id), text = "Active and upcoming broadcasts",
                            replyMarkup = InlineButtons.broadcastsNav(broadcasts)
                        )
                    }
                    callbackQuery("BroadcastsItemCmd") {
                        val id = callbackQueryId("BroadcastsItemCmd") ?: return@callbackQuery
                        val item = youtubeApi.getBroadcast(id)
                        if (item == null) {
                            bot.sendMessage(ChatId.fromId(callbackQuery.from.id), text = "Broadcast not found")
                            return@callbackQuery
                        }
                        bot.sendMessage(
                            ChatId.fromId(callbackQuery.from.id),
                            text = item.infoMessage(),
                            replyMarkup = InlineButtons.broadcastManage(item)
                        )
                    }
                    callbackQuery("BroadcastsItemRefreshCmd") {
                        val id = callbackQueryId("BroadcastsItemRefreshCmd") ?: return@callbackQuery
                        val item = youtubeApi.getBroadcast(id) ?: return@callbackQuery
                        bot.editMessageText(
                            chatId = callbackQuery.message?.chat?.id?.let { ChatId.fromId(it) },
                            messageId = callbackQuery.message?.messageId,
                            text = item.infoMessage(),
                            replyMarkup = InlineButtons.broadcastManage(item)
                        )
                    }
                    callbackQuery("BroadcastsItemStartCmd") {
                        val id = callbackQueryId("BroadcastsItemStartCmd") ?: return@callbackQuery
                        val item = youtubeApi.getBroadcast(id) ?: return@callbackQuery
                        bot.editMessageText(
                            chatId = callbackQuery.message?.chat?.id?.let { ChatId.fromId(it) },
                            messageId = callbackQuery.message?.messageId,
                            text = item.infoMessage(),
                            replyMarkup = InlineButtons.broadcastManage(item, confirmStart = true)
                        )
                    }
                    callbackQuery("BroadcastsItemStartConfirmCmd") {
                        val id = callbackQueryId("BroadcastsItemStartConfirmCmd") ?: return@callbackQuery
                        val item = youtubeApi.transitionBroadcast(id, "live") ?: return@callbackQuery
                        bot.editMessageText(
                            chatId = callbackQuery.message?.chat?.id?.let { ChatId.fromId(it) },
                            messageId = callbackQuery.message?.messageId,
                            text = item.infoMessage(),
                            replyMarkup = InlineButtons.broadcastManage(item)
                        )
                    }
                    callbackQuery("BroadcastsItemStopCmd") {
                        val id = callbackQueryId("BroadcastsItemStopCmd") ?: return@callbackQuery
                        val item = youtubeApi.getBroadcast(id) ?: return@callbackQuery
                        bot.editMessageText(
                            chatId = callbackQuery.message?.chat?.id?.let { ChatId.fromId(it) },
                            messageId = callbackQuery.message?.messageId,
                            text = item.infoMessage(),
                            replyMarkup = InlineButtons.broadcastManage(item, confirmStop = true)
                        )
                    }
                    callbackQuery("BroadcastsItemStopConfirmCmd") {
                        val id = callbackQueryId("BroadcastsItemStopConfirmCmd") ?: return@callbackQuery
                        val item = youtubeApi.transitionBroadcast(id, "complete") ?: return@callbackQuery
                        bot.editMessageText(
                            chatId = callbackQuery.message?.chat?.id?.let { ChatId.fromId(it) },
                            messageId = callbackQuery.message?.messageId,
                            text = item.infoMessage(),
                            replyMarkup = InlineButtons.broadcastManage(item)
                        )
                    }
                }
            }
        }
        bot.startPolling()
    }

    private fun CallbackQueryHandlerEnvironment.callbackQueryId(commandPrefix: String): String? {
        if (callbackQuery.data.startsWith(commandPrefix)) {
            return callbackQuery.data.substring(commandPrefix.length)
        }
        return null
    }

    private fun LiveStream.infoMessage() = "Live Stream " + snippet.title + "\n" + cdn.ingestionInfo.streamName +
            (status?.let { "\n" + "Status: " + it.streamStatus + ", " + it.healthStatus.status }
                ?: "")

    private fun LiveBroadcast.infoMessage(): String = "Broadcast ${snippet.title}\n" +
            "Start time: ${snippet.actualStartTime ?: snippet.scheduledStartTime}\n" +
            "End time: ${snippet.actualEndTime ?: snippet.scheduledEndTime}\n" +
            "Status: ${status.emojy()}${status.lifeCycleStatus}\n" +
            "Privacy: ${status.privacyStatus}\n" +
            "Thumbnails: ${snippet.thumbnails.maxres.url}\n" +
//                    "StramID: ${contentDetails.boundStreamId}\n" +
            "Manage: https://studio.youtube.com/video/${id}/livestreaming"


    private class AdminDispatcher(val admins: List<Long>, val dispatcher: Dispatcher) {
        fun text(text: String? = null, handleText: HandleText) {
            dispatcher.text(text) {
                if (admins.contains(message.from?.id)) {
                    handleText()
                }
            }
        }

        fun callbackQuery(data: String? = null, handleCallbackQuery: HandleCallbackQuery) {
            dispatcher.callbackQuery(data) {
                if (admins.contains(callbackQuery.from.id)) {
                    handleCallbackQuery()
                }
            }
        }
    }

    private fun Dispatcher.withAdminRight(body: AdminDispatcher.() -> Unit) {
        AdminDispatcher(admins, this).body()
    }

    companion object {
        private fun LiveBroadcastStatus.emojy(): String = when (this.lifeCycleStatus) {
            "complete" -> "☑️"
            "live" -> "\uD83D\uDFE2"
            "created" -> "\uD83D\uDD54"
            "ready" -> "\uD83D\uDD34"
            "testing", "testStarting", "liveStarting" -> "\uD83D\uDFE1"
            else -> "[" + this.lifeCycleStatus + "]"
        } + " "

        object InlineButtons {
            val mainMenu = InlineKeyboardMarkup.createSingleRowKeyboard(
                InlineKeyboardButton.CallbackData("Streams", "LiveStreamsCmd"),
                InlineKeyboardButton.CallbackData("Broadcasts", "BroadcastsCmd"),
            )
            val streamsMenu = InlineKeyboardMarkup.createSingleRowKeyboard(
                InlineKeyboardButton.CallbackData("List", "LiveStreamsListCmd"),
                InlineKeyboardButton.CallbackData("New", "LiveStreamsNewCmd"),
                InlineKeyboardButton.CallbackData("Hide", "HideCallbackMessageCmd"),
            )
            val broadcastsMenu = InlineKeyboardMarkup.createSingleRowKeyboard(
                InlineKeyboardButton.CallbackData("Active and upcoming", "BroadcastsActiveCmd"),
                InlineKeyboardButton.CallbackData("New", "BroadcastsNewCmd"),
                InlineKeyboardButton.CallbackData("Hide", "HideCallbackMessageCmd"),
            )

            private fun gridNav(
                commandPrefix: String,
                items: List<Pair<String, String>>,
                itemsPerRow: Int = 4
            ): InlineKeyboardMarkup {
                return InlineKeyboardMarkup.create(items.map {
                    InlineKeyboardButton.CallbackData(
                        it.second,
                        commandPrefix + it.first
                    )
                }.fold(mutableListOf<MutableList<InlineKeyboardButton>>()) { rows, it ->
                    (rows.lastOrNull()?.takeIf { it.size < itemsPerRow }
                        ?: mutableListOf<InlineKeyboardButton>().also { rows += it }) += it
                    return@fold rows
                } + mutableListOf(mutableListOf(InlineKeyboardButton.CallbackData("Hide", "HideCallbackMessageCmd"))))
            }

            fun streamsNav(liveStreams: List<LiveStream>) =
                gridNav("LiveStreamsItemCmd", liveStreams.map { it.id to it.snippet.title })

            fun streamManage(stream: LiveStream) =
                InlineKeyboardMarkup.createSingleRowKeyboard(
                    InlineKeyboardButton.CallbackData("Refresh", "LiveStreamsItemRefreshCmd${stream.id}"),
                    InlineKeyboardButton.CallbackData("Hide", "HideCallbackMessageCmd"),
                )

            fun broadcastsNav(liveStreams: List<LiveBroadcast>) =
                gridNav(
                    "BroadcastsItemCmd",
                    liveStreams.map { it.id to (it.status.emojy() + it.snippet?.title) }, 1
                )

            fun <T> MutableList<T>.addIf(condition: Boolean, value: T) {
                if (condition) add(value)
            }

            fun broadcastManage(
                broadcast: LiveBroadcast,
                confirmStart: Boolean = false,
                confirmStop: Boolean = false
            ): InlineKeyboardMarkup {
                val buttons = mutableListOf<List<InlineKeyboardButton>>(
                    listOf(
                        InlineKeyboardButton.CallbackData("Refresh", "BroadcastsItemRefreshCmd${broadcast.id}"),
                        InlineKeyboardButton.CallbackData("Hide", "HideCallbackMessageCmd")
                    )
                )
                buttons.addIf(
                    broadcast.contentDetails.boundStreamId != null, listOf(
                        InlineKeyboardButton.CallbackData(
                            "Bound stream",
                            "LiveStreamsItemCmd${broadcast.contentDetails.boundStreamId}"
                        )
                    )
                )
                buttons.addIf(
                    broadcast.status.lifeCycleStatus == "testing" && !confirmStart, listOf(
                        InlineKeyboardButton.CallbackData(
                            "Start stream \uD83D\uDFE2", "BroadcastsItemStartCmd${broadcast.id}"
                        )
                    )
                )
                buttons.addIf(
                    broadcast.status.lifeCycleStatus == "testing" && confirmStart, listOf(
                        InlineKeyboardButton.CallbackData(
                            "Confirm start stream", "BroadcastsItemStartConfirmCmd${broadcast.id}"
                        )
                    )
                )
                buttons.addIf(
                    broadcast.status.lifeCycleStatus == "live" && !confirmStop, listOf(
                        InlineKeyboardButton.CallbackData(
                            "Stop stream \uD83D\uDFE5", "BroadcastsItemStopCmd${broadcast.id}"
                        )
                    )
                )
                buttons.addIf(
                    broadcast.status.lifeCycleStatus == "live" && confirmStop, listOf(
                        InlineKeyboardButton.CallbackData(
                            "Confirm stop stream", "BroadcastsItemStopConfirmCmd${broadcast.id}"
                        )
                    )
                )
                return InlineKeyboardMarkup.create(buttons)
            }
        }
    }
}


fun main() {
    val application = Application(config())
    println("Hello!")
    application.run()
//    println(youtubeApi.createLiveStram("Aboba"))
//    youtubeApi.getLiveStreams()
//    val repo = loadRepository()
//    repo.course += Course(
//        "y2020.M32367.MathAn",
//        LocalDateTime(2022, 2, 21, 11, 40, 0),
//        "[s4 | 2022] Математический анализ, О. Л. Семенова, лекция {number}",
//        "aboba",
//        4,
//        1,
//        "dsfsa"
//
//    )
//    repo.store()
}
