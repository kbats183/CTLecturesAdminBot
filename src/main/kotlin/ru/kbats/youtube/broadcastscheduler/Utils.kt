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
