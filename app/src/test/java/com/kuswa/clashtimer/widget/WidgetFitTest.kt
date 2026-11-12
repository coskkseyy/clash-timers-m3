package com.kuswa.clashtimer.widget

import com.kuswa.clashtimer.widget.WidgetRenderer.Companion.Fit
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetFitTest {
    @Test fun unknownHeightShowsAll() = assertEquals(Fit(false, 7, 2, 1, true), WidgetRenderer.fit(null, 7, 2, 1))

    @Test fun tallIsNormal() = assertEquals(Fit(false, 7, 2, 1, true), WidgetRenderer.fit(600, 7, 2, 1))

    @Test fun mediumIsCompact() = assertEquals(Fit(true, 7, 2, 1, true), WidgetRenderer.fit(440, 7, 2, 1))

    @Test fun shortCutsLabAndPetsFirst() {
        val f = WidgetRenderer.fit(300, 7, 2, 1)
        assertEquals(7, f.builders)
        assertEquals(0, f.pets)
        assertEquals(false, f.footer)
    }

    // Dynamic rows (Android 12+): everything shown, rows sized to the widget's height.
    @Test fun tallStretchesRowsToFillTheWidget() =
        assertEquals(Fit(false, 7, 2, 1, true, 32), WidgetRenderer.fit(519, 7, 2, 1, dynamicRows = true))

    @Test fun rowStretchHasALimit() =
        assertEquals(Fit(false, 7, 2, 1, true, 34), WidgetRenderer.fit(900, 7, 2, 1, dynamicRows = true))

    @Test fun shrunkSqueezesRowsSoTheFooterStaysVisible() =
        assertEquals(Fit(true, 7, 2, 1, true, 23), WidgetRenderer.fit(412, 7, 2, 1, dynamicRows = true))

    @Test fun tooShortForSqueezedRowsStillCutsRows() {
        val f = WidgetRenderer.fit(300, 7, 2, 1, dynamicRows = true)
        assertEquals(null, f.rowDp)
        assertEquals(7, f.builders)
        assertEquals(0, f.pets)
        assertEquals(false, f.footer)
    }

    @Test fun veryShortKeepsSoonestBuilders() {
        val f = WidgetRenderer.fit(160, 7, 2, 1)
        assertEquals(2, f.builders)
        assertEquals(0, f.lab)
    }
}
