package com.kuswa.clashtimer.core

/** Raw `data` IDs from the village export that the app logic depends on. */
object GameIds {
    const val TOWN_HALL = 1000001
    const val CLAN_CASTLE = 1000014
    /** icons/91000014.webp: the Clan Castle as it looks up to level 14 (Town Hall 19 changed the art at 15). */
    private const val CLAN_CASTLE_OLD_ART = 91000014

    /** Icon id for the look a building had before its newest art, or null when the main icon fits. */
    fun olderArt(dataId: Int, targetLevel: Int): Int? =
        if (dataId == CLAN_CASTLE && targetLevel < 15) CLAN_CASTLE_OLD_ART else null
    const val BUILDERS_HUT = 1000015
    const val BOBS_HUT = 1000064

    // Builder Base (clash-of-clans-data `data/builder`).
    const val BUILDER_HALL = 1000034
    const val STAR_LABORATORY = 1000046
    const val OTTOS_OUTPOST = 1000078

    // Helper IDs, confirmed against clash-of-clans-data@0.19.1 (`data/home/other/helpers`).
    const val HELPER_BUILDERS_APPRENTICE = 93000000
    const val HELPER_LAB_ASSISTANT = 93000001
    const val HELPER_PROSPECTOR = 93000002

    // Not in the data package; Alchemist by elimination (only four helpers exist). Unused by the app.
    const val HELPER_ALCHEMIST_GUESS = 93000003
}
