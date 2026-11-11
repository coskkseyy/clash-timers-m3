package com.kuswa.clashtimer.core

import org.junit.Assert.assertEquals
import org.junit.Test

class GoldPassTest {
    @Test fun scalesBuilderAndLabButNotPets() {
        val s = AppSettings(goldPassBuilderPct = 20, goldPassResearchPct = 15)
        assertEquals(80_000L, s.goldPassScaled(JobCategory.BUILDER, 100_000L))
        assertEquals(85_000L, s.goldPassScaled(JobCategory.LAB, 100_000L))
        assertEquals(100_000L, s.goldPassScaled(JobCategory.PET, 100_000L))
    }

    @Test fun offByDefault() {
        assertEquals(100_000L, AppSettings().goldPassScaled(JobCategory.BUILDER, 100_000L))
    }
}
