package com.kuswa.clashtimer.core

/** Raw `data` IDs from the village export that the app logic depends on. */
object GameIds {
    const val TOWN_HALL = 1000001
    const val CLAN_CASTLE = 1000014
    const val SQUAD_BARRACKS = 1000106
    const val SPIKY_TOWER = 1000107
    const val FIRECRACKER_TOWER = 1000108
    /** icons/91000014.webp: the Clan Castle as it looks up to level 14 (Town Hall 19 changed the art at 15). */
    private const val CLAN_CASTLE_OLD_ART = 91000014

    /**
     * Icon id for the look a building has at [targetLevel] when we have per-level art for it, else null
     * (the main icon is the newest look). Squad Barracks has art for every level: icons/<dataId*100+level>.webp.
     */
    fun olderArt(dataId: Int, targetLevel: Int): Int? = when {
        dataId == CLAN_CASTLE && targetLevel < 15 -> CLAN_CASTLE_OLD_ART
        dataId == SQUAD_BARRACKS && targetLevel in 1..12 -> dataId * 100 + targetLevel
        (dataId == SPIKY_TOWER || dataId == FIRECRACKER_TOWER) && targetLevel in 1..2 -> dataId * 100 + targetLevel
        else -> null
    }
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
