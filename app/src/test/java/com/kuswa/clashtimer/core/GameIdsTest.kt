package com.kuswa.clashtimer.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameIdsTest {
    @Test fun clanCastleKeepsItsOlderArtUntilLevel15() {
        assertEquals(91000014, GameIds.olderArt(GameIds.CLAN_CASTLE, 14))
        assertNull(GameIds.olderArt(GameIds.CLAN_CASTLE, 15))
        assertNull(GameIds.olderArt(GameIds.TOWN_HALL, 14))
    }

    @Test fun squadBarracksUsesItsLevelArt() {
        assertEquals(100010601, GameIds.olderArt(GameIds.SQUAD_BARRACKS, 1))
        assertEquals(100010612, GameIds.olderArt(GameIds.SQUAD_BARRACKS, 12))
        assertEquals(100010701, GameIds.olderArt(GameIds.SPIKY_TOWER, 1))
        assertNull(GameIds.olderArt(GameIds.SQUAD_BARRACKS, 13))
    }
}
