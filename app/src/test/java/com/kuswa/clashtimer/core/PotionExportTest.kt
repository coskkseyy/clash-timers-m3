package com.kuswa.clashtimer.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PotionExportTest {
    private fun export(boosts: String) = """{"tag":"#T","timestamp":1000,"buildings":[],"boosts":{$boosts}}"""

    @Test fun readsResearchAndPetPotions() {
        val o = SyncLogic.apply(AppState(), export("\"lab_boost\":600,\"pet_boost\":300"))
        assertTrue(o is SyncLogic.Outcome.Synced)
        val b = o.state.boosts.associateBy { it.type }
        assertEquals(1600L, b.getValue(BoostType.RESEARCH_POTION).end)
        assertEquals(1300L, b.getValue(BoostType.PET_POTION).end)
    }

    @Test fun variantKeys() {
        val o = SyncLogic.apply(AppState(), export("\"research_boost\":60,\"pet_house_boost\":30"))
        val b = o.state.boosts.associateBy { it.type }
        assertEquals(1060L, b.getValue(BoostType.RESEARCH_POTION).end)
        assertEquals(1030L, b.getValue(BoostType.PET_POTION).end)
    }
}
