package com.kuswa.clashtimer.core

import com.kuswa.clashtimer.core.SampleExports.names
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The event Goblin Researcher: assumed to mark its lab item with `"extra": true`, the same flag the
 * Goblin Builder uses on buildings. It gets its own row under the regular lab row.
 */
class GoblinResearcherTest {

    private val t0 = 1793546851L

    /** export_3 (lab: units 4000023) plus a Goblin Researcher job on a spell. */
    private val text = SampleExports.text("export_3.json").replaceFirst(
        "\"spells\":[",
        "\"spells\":[{\"data\":26000000,\"lvl\":11,\"timer\":7200,\"extra\":true},",
    )

    private fun model(now: Long): WidgetModel {
        val o = SyncLogic.apply(AppState(), text)
        assertTrue("expected success, got $o", o is SyncLogic.Outcome.Synced)
        return WidgetModelBuilder.build((o as SyncLogic.Outcome.Synced).state, AppSettings(), names, SampleExports.times, now)
    }

    @Test fun parsedAsGoblinLabJob() {
        val e = (ExportParser.parse(text) as ParseResult.Success).export
        val goblin = e.labJobs.single { it.isGoblin }
        assertEquals(26000000, goblin.dataId)
        assertEquals(4000023, e.labJob?.dataId) // the regular lab job is unaffected
    }

    @Test fun ownRowUnderTheLab() {
        val m = model(t0 + 60)
        assertEquals(2, m.labRows.size)
        assertEquals(false, m.labRow.goblin)
        assertEquals(true, m.goblinLabRows.single().goblin)
        assertEquals(RowKind.RUNNING, m.goblinLabRows.single().kind)
    }

    @Test fun rowGoneWhenDone() {
        val m = model(t0 + 7200 + 600)
        assertTrue(m.goblinLabRows.isEmpty())
        assertEquals(1, m.labRows.size)
    }

    @Test fun finishNotice() {
        val e = (ExportParser.parse(text) as ParseResult.Success).export
        val notice = FinishNotices.notice(e.labJobs.single { it.isGoblin }, names)
        assertTrue(notice.heading, notice.heading.startsWith("Goblin Researcher done"))
    }
}
