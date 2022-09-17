package ru.kbats.youtube.broadcastscheduler.bot

import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.entities.ChatId
import ru.kbats.youtube.broadcastscheduler.Application
import ru.kbats.youtube.broadcastscheduler.states.BotUserState

fun Application.setupDispatcher(dispatcher: Dispatcher) {
    dispatcher.withAdminRight(repository) {
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
                        replyMarkup = Application.Companion.InlineButtons.mainMenu,
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
                replyMarkup = Application.Companion.InlineButtons.streamsMenu,
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
                replyMarkup = Application.Companion.InlineButtons.streamsNav(liveStreams)
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
                replyMarkup = Application.Companion.InlineButtons.streamManage(stream)
            )
        }
        callbackQuery("LiveStreamsItemRefreshCmd") {
            val id = callbackQueryId("LiveStreamsItemRefreshCmd") ?: return@callbackQuery
            val stream = youtubeApi.getStream(id) ?: return@callbackQuery
            bot.editMessageText(
                chatId = callbackQuery.message?.chat?.id?.let { ChatId.fromId(it) },
                messageId = callbackQuery.message?.messageId,
                text = stream.infoMessage(),
                replyMarkup = Application.Companion.InlineButtons.streamManage(stream)
            )
        }
        callbackQuery("BroadcastsCmd") {
            bot.sendMessage(
                ChatId.fromId(callbackQuery.from.id), text = "Broadcasts",
                replyMarkup = Application.Companion.InlineButtons.broadcastsMenu
            )
        }
        callbackQuery("BroadcastsActiveCmd") {
            val broadcasts = youtubeApi.getBroadcasts("active") + youtubeApi.getBroadcasts("upcoming")
            bot.sendMessage(
                ChatId.fromId(callbackQuery.from.id), text = "Active and upcoming broadcasts",
                replyMarkup = Application.Companion.InlineButtons.broadcastsNav(broadcasts)
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
                replyMarkup = Application.Companion.InlineButtons.broadcastManage(item)
            )
        }
        callbackQuery("BroadcastsItemRefreshCmd") {
            val id = callbackQueryId("BroadcastsItemRefreshCmd") ?: return@callbackQuery
            val item = youtubeApi.getBroadcast(id) ?: return@callbackQuery
            bot.editMessageText(
                chatId = callbackQuery.message?.chat?.id?.let { ChatId.fromId(it) },
                messageId = callbackQuery.message?.messageId,
                text = item.infoMessage(),
                replyMarkup = Application.Companion.InlineButtons.broadcastManage(item)
            )
        }
        callbackQuery("BroadcastsItemStartCmd") {
            val id = callbackQueryId("BroadcastsItemStartCmd") ?: return@callbackQuery
            val item = youtubeApi.getBroadcast(id) ?: return@callbackQuery
            bot.editMessageText(
                chatId = callbackQuery.message?.chat?.id?.let { ChatId.fromId(it) },
                messageId = callbackQuery.message?.messageId,
                text = item.infoMessage(),
                replyMarkup = Application.Companion.InlineButtons.broadcastManage(item, confirmStart = true)
            )
        }
        callbackQuery("BroadcastsItemStartConfirmCmd") {
            val id = callbackQueryId("BroadcastsItemStartConfirmCmd") ?: return@callbackQuery
            val item = youtubeApi.transitionBroadcast(id, "live") ?: return@callbackQuery
            bot.editMessageText(
                chatId = callbackQuery.message?.chat?.id?.let { ChatId.fromId(it) },
                messageId = callbackQuery.message?.messageId,
                text = item.infoMessage(),
                replyMarkup = Application.Companion.InlineButtons.broadcastManage(item)
            )
        }
        callbackQuery("BroadcastsItemStopCmd") {
            val id = callbackQueryId("BroadcastsItemStopCmd") ?: return@callbackQuery
            val item = youtubeApi.getBroadcast(id) ?: return@callbackQuery
            bot.editMessageText(
                chatId = callbackQuery.message?.chat?.id?.let { ChatId.fromId(it) },
                messageId = callbackQuery.message?.messageId,
                text = item.infoMessage(),
                replyMarkup = Application.Companion.InlineButtons.broadcastManage(item, confirmStop = true)
            )
        }
        callbackQuery("BroadcastsItemStopConfirmCmd") {
            val id = callbackQueryId("BroadcastsItemStopConfirmCmd") ?: return@callbackQuery
            val item = youtubeApi.transitionBroadcast(id, "complete") ?: return@callbackQuery
            bot.editMessageText(
                chatId = callbackQuery.message?.chat?.id?.let { ChatId.fromId(it) },
                messageId = callbackQuery.message?.messageId,
                text = item.infoMessage(),
                replyMarkup = Application.Companion.InlineButtons.broadcastManage(item)
            )
        }
    }
}
