package com.kuswa.clashtimer.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuilderBaseTest {
    @Test fun parsesBuilderBaseJobs() {
        val e = SampleExports.export4
        val job = e.bbJobs.single()
        assertEquals(1000041, job.dataId)
        assertEquals(82597L, job.remainingAtT0)
        assertTrue((e.builderHallLevel ?: 0) > 0)
        assertEquals("Double Cannon", SampleExports.names.title(job))
    }

    private fun job(rem: Long) = Job(JobCategory.BUILDER, JobSource.BUILDING, 1, 1, rem, false)

    @Test fun noBoost() = assertEquals(1000L + 5000, BuilderBase.finishAt(job(5000), 1000, 0))

    @Test fun boostCoversAll() = assertEquals(1000L + 500, BuilderBase.finishAt(job(5000), 1000, 600))

    @Test fun boostThenNormal() {
        // 100 s of boost does 1000 s of work, 4000 s left at normal speed.
        assertEquals(1000L + 100 + 4000, BuilderBase.finishAt(job(5000), 1000, 100))
        assertEquals(5000L - 1000 - 50, BuilderBase.workLeft(job(5000), 1000, 100, 1150))
    }
}
