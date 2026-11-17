package com.kuswa.clashtimer.core

import java.util.Locale

/**
 * English / Turkish texts for the widgets, notifications and app screen. Item names (Gold Mine,
 * Archer Queen…) stay English: the data package has no Turkish names.
 * [tr] is set from [AppSettings.language] before anything is drawn; tests run in English.
 */
object Lang {
    @Volatile var tr: Boolean = false

    fun apply(settings: AppSettings) {
        tr = when (settings.language) {
            "tr" -> true
            "en" -> false
            else -> Locale.getDefault().language == "tr"
        }
    }

    fun t(en: String, tr: String): String = if (this.tr) tr else en

    // Duration units: 1d 20h 5m / 1g 20sa 5dk.
    val d: String get() = t("d", "g")
    val h: String get() = t("h", "sa")
    val m: String get() = t("m", "dk")
}
