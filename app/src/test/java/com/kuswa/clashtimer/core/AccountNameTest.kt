package com.kuswa.clashtimer.core

import org.junit.Assert.assertEquals
import org.junit.Test

class AccountNameTest {
    private val settings = AppSettings(playerName = "Cosk")

    @Test fun firstAccountUsesTheSettingsName() =
        assertEquals("Cosk", settings.forAccount("", AppState(name = "X")).displayName)

    @Test fun otherAccountsUseTheirOwnName() =
        assertEquals("Ahmet", settings.forAccount("2", AppState(name = "Ahmet")).displayName)

    @Test fun otherAccountWithoutAName() =
        assertEquals("Cosk", settings.forAccount("2", AppState()).displayName)

    @Test fun syncKeepsTheAccountName() {
        val text = """{"tag":"#T","timestamp":100,"buildings":[]}"""
        val synced = SyncLogic.apply(AppState(name = "Ahmet"), text)
        assertEquals("Ahmet", synced.state.name)
    }
}
