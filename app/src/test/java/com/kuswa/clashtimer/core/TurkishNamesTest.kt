package com.kuswa.clashtimer.core

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class TurkishNamesTest {
    @After fun reset() { Lang.tr = false }

    @Test fun gameTurkishNames() {
        val tr = SampleExports.file("src/main/assets/names_tr.json", "app/src/main/assets/names_tr.json").readText()
        val names = SampleExports.names.withTurkish(tr)
        Lang.tr = true
        assertEquals("Altın Madeni", names.name(1000004))
        assertEquals("Okçu Kraliçe", names.name(28000001))
        Lang.tr = false
        assertEquals("Gold Mine", names.name(1000004))
    }
}
