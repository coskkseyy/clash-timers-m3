package com.kuswa.clashtimer.core

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class FinishClockTest {
    private val z = ZoneId.of("Europe/Istanbul")
    private val now = ZonedDateTime.of(2026, 11, 11, 20, 0, 0, 0, z).toEpochSecond()

    @Test fun today() = assertEquals("21:14", FinishClock.format(now + 74 * 60, now, z))
    @Test fun tomorrow() = assertEquals("Tmrw 09:30", FinishClock.format(now + 13 * 3600 + 30 * 60, now, z))
    @Test fun thisWeek() = assertEquals("Sat 20:00", FinishClock.format(now + 3 * 86400, now, z))
    @Test fun later() = assertEquals("21 Nov 20:00", FinishClock.format(now + 10 * 86400, now, z))
}
