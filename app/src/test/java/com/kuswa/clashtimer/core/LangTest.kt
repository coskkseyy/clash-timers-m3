package com.kuswa.clashtimer.core

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class LangTest {
    @After fun reset() { Lang.tr = false }

    @Test fun turkishDurations() {
        Lang.apply(AppSettings(language = "tr"))
        assertEquals("1g 2s", WidgetModelBuilder.formatRemaining(26L * 3600))
        assertEquals("5dk önce", WidgetModelBuilder.formatAgo(300))
    }

    @Test fun englishDefault() {
        Lang.apply(AppSettings(language = "en"))
        assertEquals("1d 2h", WidgetModelBuilder.formatRemaining(26L * 3600))
    }

    @Test fun clockTowerCooldown() {
        val e = SampleExports.export1
        assertEquals(1808L, e.clockTowerBoostRemaining)
        assertEquals(81008L, e.clockTowerCooldown)
    }
}
