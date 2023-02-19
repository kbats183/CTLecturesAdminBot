package ru.kbats.youtube.broadcastscheduler.bot

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.handlers.CallbackQueryHandlerEnvironment
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.TelegramFile
import ru.kbats.youtube.broadcastscheduler.Application
import ru.kbats.youtube.broadcastscheduler.YoutubeVideoIDMatcher
import ru.kbats.youtube.broadcastscheduler.data.Lecture
import ru.kbats.youtube.broadcastscheduler.states.BotUserState
import ru.kbats.youtube.broadcastscheduler.thumbnail.Thumbnail
import kotlin.io.path.Path

private fun Lecture.infoMessage(): String = "Lecture ${name}\n" +
        "Title: ${currentTitle()}\n" +
        "Description: ${description}\n" +
        "Lecture type: ${lectureType}\n" +
        "Lecture numeration: ${if (doubleNumeration) "double" else "single"}\n" +
        ""

fun Application.setupDispatcher(dispatcher: Dispatcher) {
    dispatcher.withAdminRight(repository) {
        text {
            val chatId = ChatId.fromId(message.chat.id)
            if (text.startsWith("/addAdmin")) {
                val components = text.split("\n")
                if (components.size != 3) {
                    bot.sendMessage(chatId, "Incorrect input")
                    return@text
                }
                repository.addAdmin(components[1], components[2])
                bot.sendMessage(chatId, "Ok")
                return@text
            }
            if ("/cancel" in text) {
                userStates[message.chat.id] = BotUserState.Default
                return@text
            }
            when (val state = userStates[message.chat.id]) {
                is BotUserState.CreatingNewLiveStream -> {
                    val newLiveStream = youtubeApi.createStream(text)
                    bot.sendMessage(chatId, newLiveStream.infoMessage())
                    userStates[message.chat.id] = BotUserState.Default
                }

                is BotUserState.ApplyingTemplateToVideo -> {
                    val videoId = YoutubeVideoIDMatcher.match(text) ?: return@text Unit.also {
                        bot.sendMessage(chatId, text = "Invalid video id or url")
                    }
                    if (youtubeApi.getVideo(videoId) == null) {
                        bot.sendMessage(chatId, text = "No video with id $videoId")
                        return@text
                    }
                    val lecture = repository.getLecture(state.lectureId) ?: return@text Unit.also {
                        bot.sendMessage(chatId, text = "No such lecture")
                    }
                    val applyingMessage = bot.sendMessage(chatId, text = "Applying ...")
                    val video = applyTemplateToVideo(videoId, lecture)
                    if (video == null) {
                        bot.sendMessage(chatId, "Failed to schedule stream")
                        applyingMessage.getOrNull()?.delete(bot)
                        return@text
                    }
                    applyingMessage.getOrNull()?.delete(bot)
                    bot.sendMessage(chatId, text = video.infoMessage())
                    userStates[message.chat.id] = BotUserState.Default
                }

                else -> {
                    bot.sendMessage(
                        chatId, text = "Main menu",
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
        callbackQuery("BroadcastsItemTestCmd") {
            val id = callbackQueryId("BroadcastsItemTestCmd") ?: return@callbackQuery
            val item = youtubeApi.transitionBroadcast(id, "testing") ?: return@callbackQuery
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
        callbackQuery("LecturesCmd") {
            bot.sendMessage(
                ChatId.fromId(callbackQuery.from.id), text = "Lectures",
                replyMarkup = Application.Companion.InlineButtons.lecturesNav(repository.getLectures())
            )
        }
        callbackQuery("LecturesRefreshCmd") {
            bot.editMessageText(
                chatId = callbackQuery.message?.chat?.id?.let { ChatId.fromId(it) },
                messageId = callbackQuery.message?.messageId,
                text = "Lectures",
                replyMarkup = Application.Companion.InlineButtons.lecturesNav(repository.getLectures()),
            )
        }
        callbackQuery("LecturesItemCmd") {
            val id = callbackQueryId("LecturesItemCmd") ?: return@callbackQuery
            val lecture = repository.getLecture(id) ?: return@callbackQuery
            bot.sendMessage(
                ChatId.fromId(callbackQuery.from.id), text = lecture.infoMessage(),
                replyMarkup = Application.Companion.InlineButtons.lectureManage(lecture)
            )
        }
        fun CallbackQueryHandlerEnvironment.editLectureMessage(lecture: Lecture) {
            bot.editMessageText(
                chatId = callbackQuery.message?.chat?.id?.let { ChatId.fromId(it) },
                messageId = callbackQuery.message?.messageId,
                text = lecture.infoMessage(),
                replyMarkup = Application.Companion.InlineButtons.lectureManage(lecture)
            )
        }
        callbackQuery("LecturesItemRefreshCmd") {
            val id = callbackQueryId("LecturesItemRefreshCmd") ?: return@callbackQuery
            val lecture = repository.getLecture(id) ?: return@callbackQuery
            editLectureMessage(lecture)
        }
        callbackQuery("LecturesItemPrevNumberCmd") {
            val id = callbackQueryId("LecturesItemPrevNumberCmd") ?: return@callbackQuery
            repository.updateLecture(id) { it.copy(currentLectureNumber = it.currentLectureNumber - 1) }
            val lecture = repository.getLecture(id) ?: return@callbackQuery
            editLectureMessage(lecture)
        }
        callbackQuery("LecturesItemNextNumberCmd") {
            val id = callbackQueryId("LecturesItemNextNumberCmd") ?: return@callbackQuery
            repository.updateLecture(id) { it.copy(currentLectureNumber = it.currentLectureNumber + 1) }
            val lecture = repository.getLecture(id) ?: return@callbackQuery
            editLectureMessage(lecture)
        }
        callbackQuery("LecturesItemThumbnailsCmd") {
            val id = callbackQueryId("LecturesItemThumbnailsCmd") ?: return@callbackQuery
            val lecture = repository.getLecture(id) ?: return@callbackQuery
            if (lecture.thumbnails == null) {
                bot.sendMessage(ChatId.fromId(callbackQuery.from.id), text = "No thumbnails info")
                return@callbackQuery
            }

            val generatingMessage = bot.sendMessage(ChatId.fromId(callbackQuery.from.id), text = "Generating ...")
            try {
                val generateFile = Thumbnail.generate(
                    Path(Application.thumbnailsDirectory),
                    lecture.thumbnails,
                    lecture.currentThumbnailLecture()
                )
                bot.sendDocument(
                    ChatId.fromId(callbackQuery.from.id),
                    TelegramFile.ByFile(generateFile),
                )
                generatingMessage.getOrNull()?.delete(bot)
            } catch (e: Throwable) {
                println(e.message ?: e::class.java.name)
                bot.sendMessage(ChatId.fromId(callbackQuery.from.id), text = e.message ?: e::class.java.name)
            }
        }
        callbackQuery("LecturesItemSchedulingCmd") {
            val id = callbackQueryId("LecturesItemSchedulingCmd") ?: return@callbackQuery
            val lecture = repository.getLecture(id) ?: return@callbackQuery
            if (lecture.scheduling == null) {
                bot.sendMessage(ChatId.fromId(callbackQuery.from.id), text = "No scheduling info")
                return@callbackQuery
            }
            val schedulingMessage = bot.sendMessage(ChatId.fromId(callbackQuery.from.id), text = "Scheduling ...")
            val scheduledStream = scheduleStream(lecture)
            if (scheduledStream == null) {
                schedulingMessage.getOrNull()?.delete(bot)
                bot.sendMessage(ChatId.fromId(callbackQuery.from.id), "Failed to schedule stream")
                return@callbackQuery
            }
            schedulingMessage.getOrNull()?.delete(bot)
            bot.sendMessage(
                ChatId.fromId(callbackQuery.from.id),
                text = scheduledStream.infoMessage(),
                replyMarkup = Application.Companion.InlineButtons.broadcastManage(scheduledStream)
            )
        }
        callbackQuery("LecturesItemApplyTemplateCmd") {
            val id = callbackQueryId("LecturesItemApplyTemplateCmd") ?: return@callbackQuery
            repository.getLecture(id) ?: return@callbackQuery
            userStates[callbackQuery.from.id] = BotUserState.ApplyingTemplateToVideo(id)
            bot.sendMessage(
                ChatId.fromId(callbackQuery.from.id),
                text = "Send video id or url to apply lecture template or click /cancel"
            )
        }

    }
}

private fun Message.delete(bot: Bot) {
    bot.deleteMessage(ChatId.fromId(this.chat.id), this.messageId)
}
