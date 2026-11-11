package com.kuswa.clashtimer.core

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Finish times for the "show finish time" mode: `21:14`, `Tmrw 09:30`, `Thu 09:30`, `14 Nov 09:30`. */
object FinishClock {
    private val time = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
    private val weekday = DateTimeFormatter.ofPattern("EEE HH:mm", Locale.ENGLISH)
    private val date = DateTimeFormatter.ofPattern("d MMM HH:mm", Locale.ENGLISH)

    fun format(finishAt: Long, now: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val at = Instant.ofEpochSecond(finishAt).atZone(zone)
        val days = ChronoUnit.DAYS.between(Instant.ofEpochSecond(now).atZone(zone).toLocalDate(), at.toLocalDate())
        return when {
            days <= 0 -> at.format(time)
            days == 1L -> "Tmrw " + at.format(time)
            days < 7 -> at.format(weekday)
            else -> at.format(date)
        }
    }
}
