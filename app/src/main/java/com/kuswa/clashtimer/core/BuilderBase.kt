package com.kuswa.clashtimer.core

/**
 * The Builder Base widget's content. Much simpler than the home village: no potions, helpers or
 * Goblins; the only speed-up the export shows is a running Clock Tower boost (everything 10x).
 */
data class BbModel(
    val hasData: Boolean,
    val playerName: String,
    val builderHall: Int?,
    val buildersBusy: Int,
    val buildersTotal: Int,
    val builderRows: List<WidgetRow>,
    val labRow: WidgetRow,
    /** `Clock Tower 12m` while the boost runs, else empty. */
    val footerLeft: String,
    val footerRight: String,
    val syncFailed: Boolean,
) {
    val buildersLabel: String get() = "BUILDERS $buildersBusy/$buildersTotal"
    val allRows: List<WidgetRow> get() = builderRows + labRow
}

object BuilderBase {
    const val CLOCK_TOWER_SPEED = 10

    /** Epoch s when [job] finishes, with a Clock Tower boost of [boost] s running from [t0]. */
    fun finishAt(job: Job, t0: Long, boost: Long): Long {
        val work = job.remainingAtT0
        val boosted = boost * CLOCK_TOWER_SPEED
        return if (work <= boosted) t0 + (work + CLOCK_TOWER_SPEED - 1) / CLOCK_TOWER_SPEED
        else t0 + boost + (work - boosted)
    }

    /** Work seconds left on [job] at [now]. */
    fun workLeft(job: Job, t0: Long, boost: Long, now: Long): Long {
        val elapsed = (now - t0).coerceAtLeast(0)
        val done = if (elapsed <= boost) elapsed * CLOCK_TOWER_SPEED else boost * CLOCK_TOWER_SPEED + (elapsed - boost)
        return (job.remainingAtT0 - done).coerceAtLeast(0)
    }

    fun build(state: AppState, settings: AppSettings, names: NameMap, times: UpgradeTimes, now: Long): BbModel {
        val export = state.export
        val idleLab = WidgetRow(
            kind = RowKind.IDLE, name = "Star Laboratory", levelText = null, showArrow = false,
            iconIds = listOf(GameIds.STAR_LABORATORY), initials = "SL", finishAt = null, remaining = 0,
            progress = 1f, button = RowButton.NONE,
        )
        if (export == null) {
            return BbModel(
                hasData = false, playerName = settings.displayName, builderHall = null, buildersBusy = 0,
                buildersTotal = 1, builderRows = emptyList(), labRow = idleLab, footerLeft = "",
                footerRight = "", syncFailed = state.syncFailed,
            )
        }
        val t0 = export.timestamp
        val boost = export.clockTowerBoostRemaining ?: 0L

        fun row(job: Job): WidgetRow {
            val finishAt = finishAt(job, t0, boost)
            val done = Finish.isDone(finishAt, now)
            val left = if (done) 0L else workLeft(job, t0, boost, maxOf(now, t0))
            val full = times.fullTime(job)?.takeIf { it >= job.remainingAtT0 } ?: job.remainingAtT0
            val title = names.title(job)
            return WidgetRow(
                kind = if (done) RowKind.COMPLETED else RowKind.RUNNING,
                name = title,
                levelText = if (job.isNewBuilding) "(new)" else job.targetLevel.toString(),
                showArrow = !job.isNewBuilding,
                iconIds = listOf(job.dataId),
                initials = WidgetModelBuilder.initials(title),
                finishAt = finishAt,
                remaining = if (done) 0 else finishAt - now,
                progress = if (done || full <= 0) 1f else (1.0 - left.toDouble() / full).coerceIn(0.0, 1.0).toFloat(),
                button = RowButton.NONE,
                jobKey = "BB:" + job.key,
            )
        }

        val builderJobs = export.bbJobs.filter { it.category == JobCategory.BUILDER }
        val builderRows = builderJobs.map(::row).sortedBy { it.finishAt }
        val labRow = export.bbJobs.filter { it.category == JobCategory.LAB }
            .map(::row).minByOrNull { it.finishAt ?: Long.MAX_VALUE } ?: idleLab
        val busy = builderRows.count { it.kind == RowKind.RUNNING }
        val total = maxOf(busy, 1 + if (export.hasOttosOutpost) 1 else 0)
        val boostLeft = (t0 + boost - now).coerceAtLeast(0)
        return BbModel(
            hasData = true,
            playerName = settings.displayName,
            builderHall = export.builderHallLevel,
            buildersBusy = busy,
            buildersTotal = total,
            builderRows = builderRows,
            labRow = labRow,
            footerLeft = if (boostLeft > 0) "Clock Tower ${(boostLeft + 59) / 60}m" else "",
            footerRight = "synced ${WidgetModelBuilder.formatAgo(now - t0)}",
            syncFailed = state.syncFailed,
        )
    }
}
