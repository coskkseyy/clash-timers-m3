package com.kuswa.clashtimer.core

/** What happens to the stored state when the player taps Sync. */
object SyncLogic {

    sealed interface Outcome {
        val state: AppState

        data class Synced(override val state: AppState, val export: VillageExport) : Outcome
        data class Failed(override val state: AppState, val reason: String) : Outcome
    }

    fun apply(old: AppState, clipboard: String?): Outcome {
        fun fail(reason: String) = Outcome.Failed(old.copy(syncFailed = true), reason)

        if (clipboard.isNullOrBlank()) return fail(Lang.t("Clipboard is empty", "Pano boş"))
        val export = when (val r = ExportParser.parse(clipboard)) {
            is ParseResult.Invalid -> return fail(Lang.t("Not village data: ", "Köy verisi değil: ") + r.reason)
            is ParseResult.Success -> r.export
        }
        val previous = old.export
        if (previous != null && export.timestamp < previous.timestamp) {
            return fail(Lang.t("Clipboard export is older than the last sync", "Panodaki veri son sync'ten eski"))
        }

        val links = HelperLinks(
            apprenticeJobKey = linkFor(JobCategory.BUILDER, export, previous, old.links.apprenticeJobKey),
            labAssistantJobKey = linkFor(JobCategory.LAB, export, previous, old.links.labAssistantJobKey),
        )
        var boosts = old.boosts.filter { it.end > export.timestamp }
        // `boosts.builder_boost` is the game's own remaining builder-potion time (stacked potions included),
        // so it replaces any builder-potion windows tapped on the widget instead of adding to them.
        // Same for research and pet potions read from the export.
        for ((type, left) in listOf(
            BoostType.BUILDER_POTION to export.builderBoostRemaining,
            BoostType.RESEARCH_POTION to export.researchBoostRemaining,
            BoostType.PET_POTION to export.petBoostRemaining,
        )) {
            if (left == null) continue
            boosts = boosts.filterNot { it.type == type } + BoostUse(type, export.timestamp, export.timestamp + left)
        }

        return Outcome.Synced(
            AppState(
                exportJson = clipboard.trim(),
                links = links,
                boosts = boosts,
                syncFailed = false,
                firstRemaining = export.jobs.associate { job ->
                    // max(): a key can be reused by a different instance (e.g. the second of two
                    // identical upgrades after the first finished); never shrink the bar's length.
                    job.key to maxOf(old.firstRemaining[job.key] ?: 0L, job.remainingAtT0)
                },
            ),
            export,
        )
    }

    /**
     * `helper_recurrent: true` on a running job sets the link. On a finished item (no `timer`) the helper
     * is idle → no link. Otherwise a manual link from the widget is carried over if its job still runs.
     */
    private fun linkFor(category: JobCategory, export: VillageExport, previous: VillageExport?, oldKey: String?): String? {
        val candidates = export.jobs.filter { it.category == category }
        candidates.firstOrNull { it.helperLinked }?.let { return it.key }
        if (category in export.helperOnFinishedItem) return null
        return carryOver(oldKey, previous, candidates)
    }

    /**
     * Keeps a manual link only if the same job (category + data + target level) still exists.
     * Prefers the exact same occurrence (e.g. the second of two identical Wizard Towers).
     */
    private fun carryOver(key: String?, previous: VillageExport?, candidates: List<Job>): String? {
        if (key == null) return null
        candidates.firstOrNull { it.key == key }?.let { return it.key }
        val old = previous?.jobs?.firstOrNull { it.key == key } ?: return null
        return candidates.firstOrNull {
            it.category == old.category && it.dataId == old.dataId && it.targetLevel == old.targetLevel
        }?.key
    }
}

/** Plain-text summary of a sync outcome for the toast and the launcher screen. */
fun SyncLogic.Outcome.describe(names: NameMap): String = when (this) {
    is SyncLogic.Outcome.Failed -> Lang.t("Sync failed: ", "Sync olmadı: ") + reason
    is SyncLogic.Outcome.Synced -> buildString {
        val e = export
        append(Lang.t("Synced", "Sync tamam"))
        e.townHallLevel?.let { append(" TH$it") }
        val goblin = e.builderJobs.count { it.isGoblin }
        append(" · ${e.regularBuilderJobs.size} " + Lang.t("builder job", "inşaat"))
        if (e.regularBuilderJobs.size != 1 && !Lang.tr) append("s")
        if (goblin > 0) append(" + Goblin")
        append(" · " + Lang.t("lab", "lab") + ": ").append(e.labJob?.let(names::label) ?: Lang.t("idle", "boşta"))
        append(" · " + Lang.t("pet", "pet") + ": ").append(e.petJob?.let(names::label) ?: Lang.t("idle", "boşta"))
        e.builderBoostRemaining?.let { append(Lang.t(" · builder potion ${WidgetModelBuilder.formatRemaining(it)} left", " · inşaat iksiri ${WidgetModelBuilder.formatRemaining(it)} kaldı")) }
        e.researchBoostRemaining?.let { append(Lang.t(" · research potion ${WidgetModelBuilder.formatRemaining(it)} left", " · araştırma iksiri ${WidgetModelBuilder.formatRemaining(it)} kaldı")) }
        e.petBoostRemaining?.let { append(Lang.t(" · pet potion ${WidgetModelBuilder.formatRemaining(it)} left", " · pet iksiri ${WidgetModelBuilder.formatRemaining(it)} kaldı")) }
        if (e.boostKeys.isNotEmpty()) append(" · boosts: " + e.boostKeys.joinToString(","))
    }
}
