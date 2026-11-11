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

    @Test fun veryShortKeepsSoonestBuilders() {
        val f = WidgetRenderer.fit(150, 7, 2, 1)
        assertEquals(2, f.builders)
        assertEquals(0, f.lab)
    }
}
