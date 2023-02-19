package ru.kbats.youtube.broadcastscheduler

import kotlinx.datetime.*
import kotlin.time.Duration.Companion.days

val timeZone = TimeZone.of("UTC+3")

fun getNextDayTimeInstant(dayOfWeek: Int, timeHour: Int, timeMinute: Int): Instant {
    val now = Clock.System.now()
    val nowLocal = now.toLocalDateTime(timeZone)
    val withTime = LocalDateTime(nowLocal.year, nowLocal.monthNumber, nowLocal.dayOfMonth, timeHour, timeMinute, 0)
        .toInstant(timeZone)
    val notResult = withTime + ((dayOfWeek - nowLocal.dayOfWeek.ordinal + 7) % 7).days
    return notResult + (if (notResult < now) 7 else 0).days
}

fun String?.withUpdateUrlSuffix(): String? = this?.let { "$this?" + Clock.System.now().epochSeconds }

object YoutubeVideoIDMatcher {
    fun match(string: String): String? {
        return VIDEO_URL_PATTERN.group1(string)
            ?: LIVE_VIDEO_URL_PATTERN.group1(string)
            ?: BE_VIDEO_URL_PATTERN.group1(string)
            ?: VIDEO_ID_PATTERN.find(string)?.value
    }

    private fun Regex.group1(string: String): String? {
        return find(string)?.groups?.get(1)?.value
    }

    private val VIDEO_URL_PATTERN = Regex("^https:\\/\\/www\\.youtube\\.com\\/watch\\?v=([^\\&]+)(\\&.*)?\$")
    private val LIVE_VIDEO_URL_PATTERN = Regex("^https:\\/\\/youtube\\.com\\/live\\/([^\\?]+).*\$")
    private val BE_VIDEO_URL_PATTERN = Regex("^https:\\/\\/youtu\\.be\\/([^\\?]+).*\$")
    private val VIDEO_ID_PATTERN = Regex("^[A-Za-z0-9_\\-]{11}\$")
}
