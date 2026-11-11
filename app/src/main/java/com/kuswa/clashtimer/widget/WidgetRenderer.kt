package com.kuswa.clashtimer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.util.LruCache
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import com.kuswa.clashtimer.R
import com.kuswa.clashtimer.core.BbModel
import com.kuswa.clashtimer.core.BoostPill
import com.kuswa.clashtimer.core.FinishClock
import com.kuswa.clashtimer.core.Lang
import com.kuswa.clashtimer.core.WidgetActions
import com.kuswa.clashtimer.core.Countdown
import com.kuswa.clashtimer.core.Finish
import com.kuswa.clashtimer.core.FooterTone
import com.kuswa.clashtimer.core.RowButton
import com.kuswa.clashtimer.core.RowKind
import com.kuswa.clashtimer.core.WidgetModel
import com.kuswa.clashtimer.core.WidgetModelBuilder
import com.kuswa.clashtimer.core.WidgetRow
import com.kuswa.clashtimer.sync.SyncActivity
import kotlin.math.roundToInt

/** Maps a [WidgetModel] onto `widget_root.xml` + one `widget_row*.xml` per row. */
class WidgetRenderer(private val context: Context, private val finishTime: Boolean = false) {

    private val pkg = context.packageName
    private val icons = WidgetIcons.get(context)
    private val density = context.resources.displayMetrics.density

    fun render(model: WidgetModel, liveCountdown: Boolean, options: Bundle?): RemoteViews {
        val rv = RemoteViews(pkg, R.layout.widget_root)
        val sync = syncIntent()
        rv.setOnClickPendingIntent(R.id.header, sync)
        rv.setOnClickPendingIntent(R.id.sync_button, sync)
        rv.setTextViewText(R.id.who, title(model))
        rv.setTextViewText(R.id.sync_text, "Sync")
        rv.setTextViewText(R.id.label_lab, Lang.t("LAB", "LABORATUVAR"))
        rv.setTextViewText(R.id.label_pets, Lang.t("PETS", "EVCİL HAYVANLAR"))
        rv.setTextViewText(R.id.empty_hint, Lang.t("Clash: Settings, Data Export, Copy", "Clash: Ayarlar, Veri Dışa Aktarma, Kopyala"))

        if (!model.hasData) {
            rv.setViewVisibility(R.id.content, View.GONE)
            rv.setViewVisibility(R.id.empty, View.VISIBLE)
            rv.setOnClickPendingIntent(R.id.empty, sync)
            rv.setTextViewText(R.id.empty_text, if (model.syncFailed) WidgetModelBuilder.SYNC_FAILED else WidgetModelBuilder.NO_DATA)
            return rv
        }
        rv.setViewVisibility(R.id.empty, View.GONE)
        rv.setViewVisibility(R.id.content, View.VISIBLE)
        // Tapping anywhere below the header (except the pills and row buttons) opens Clash of Clans.
        clashIntent()?.let { rv.setOnClickPendingIntent(R.id.content, it) }

        rv.setTextViewText(R.id.builders_label, buildersLabel(model))
        pill(rv, PillIds(R.id.pill_bite, R.id.pill_bite_add, R.id.pill_bite_undo, R.id.pill_bite_icon, R.id.pill_bite_text),
            model.bite, "builder_bite", R.drawable.ic_builder_bite)
        pill(rv, PillIds(R.id.pill_bpot, R.id.pill_bpot_add, R.id.pill_bpot_undo, R.id.pill_bpot_icon, R.id.pill_bpot_text),
            model.builderPotion, "builder_potion", R.drawable.ic_builder_potion)
        pill(rv, PillIds(R.id.pill_rpot, R.id.pill_rpot_add, R.id.pill_rpot_undo, R.id.pill_rpot_icon, R.id.pill_rpot_text),
            model.researchPotion, "research_potion", R.drawable.ic_research_potion)
        pill(rv, PillIds(R.id.pill_ppot, R.id.pill_ppot_add, R.id.pill_ppot_undo, R.id.pill_ppot_icon, R.id.pill_ppot_text),
            model.petPotion, "pet_potion", R.drawable.ic_pet_potion)

        val builderRows = model.builderRows.take(MAX_BUILDER_ROWS)
        val labRows = model.labRows
        val petRows = listOf(model.petRow)
        val fit = fit(reportedHeightDp(options), builderRows.size, labRows.size, petRows.size)
        val compact = fit.compact
        val nowMs = System.currentTimeMillis()
        val elapsed = SystemClock.elapsedRealtime()
        fun fill(@IdRes container: Int, rows: List<WidgetRow>) {
            rv.removeAllViews(container)
            rows.forEach { rv.addView(container, row(it, compact, liveCountdown, nowMs, elapsed)) }
        }
        // Rows are sorted soonest first, so a short widget keeps the most urgent ones.
        fill(R.id.rows_builders, builderRows.take(fit.builders))
        fill(R.id.rows_lab, labRows.take(fit.lab))
        fill(R.id.rows_pet, petRows.take(fit.pets))
        val labVis = if (fit.lab > 0) View.VISIBLE else View.GONE
        val petVis = if (fit.pets > 0) View.VISIBLE else View.GONE
        rv.setViewVisibility(R.id.section_lab, labVis)
        rv.setViewVisibility(R.id.rows_lab, labVis)
        rv.setViewVisibility(R.id.section_pets, petVis)
        rv.setViewVisibility(R.id.rows_pet, petVis)
        rv.setViewVisibility(R.id.footer, if (fit.footer) View.VISIBLE else View.GONE)

        rv.setTextViewText(R.id.foot_left, footer(model))
        rv.setTextViewText(R.id.foot_right, model.footerRight)
        return rv
    }

    /** The Builder Base widget: builders, Star Laboratory, Clock Tower boost and sync age. */
    fun renderBb(model: BbModel, liveCountdown: Boolean, options: Bundle?): RemoteViews {
        val rv = RemoteViews(pkg, R.layout.widget_bb)
        val sync = syncIntent()
        rv.setOnClickPendingIntent(R.id.header, sync)
        rv.setOnClickPendingIntent(R.id.sync_button, sync)
        val title = SpannableStringBuilder(model.playerName)
        model.builderHall?.let { bh ->
            val start = title.length
            title.append(" BH$bh")
            title.setSpan(AbsoluteSizeSpan(12, true), start, title.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            title.setSpan(ForegroundColorSpan(color(R.color.m_primary)), start, title.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        rv.setTextViewText(R.id.who, title)
        rv.setTextViewText(R.id.sync_text, "Sync")
        rv.setTextViewText(R.id.section_lab, Lang.t("STAR LAB", "YILDIZ LAB"))
        if (!model.hasData) {
            rv.setViewVisibility(R.id.content, View.GONE)
            rv.setViewVisibility(R.id.empty, View.VISIBLE)
            rv.setOnClickPendingIntent(R.id.empty, sync)
            rv.setTextViewText(R.id.empty, if (model.syncFailed) WidgetModelBuilder.SYNC_FAILED else WidgetModelBuilder.NO_DATA)
            return rv
        }
        rv.setViewVisibility(R.id.empty, View.GONE)
        rv.setViewVisibility(R.id.content, View.VISIBLE)
        clashIntent()?.let { rv.setOnClickPendingIntent(R.id.content, it) }
        rv.setTextViewText(R.id.builders_label, withFree(model.buildersLabel, model.buildersTotal - model.buildersBusy))

        // Same height logic as the big widget, with one row section fewer.
        val fit = fit(reportedHeightDp(options)?.plus(FIT_SECTION), model.builderRows.size, 1, 0)
        val nowMs = System.currentTimeMillis()
        val elapsed = SystemClock.elapsedRealtime()
        rv.removeAllViews(R.id.rows_builders)
        model.builderRows.take(fit.builders).forEach { rv.addView(R.id.rows_builders, row(it, fit.compact, liveCountdown, nowMs, elapsed)) }
        rv.removeAllViews(R.id.rows_lab)
        val labVis = if (fit.lab > 0) View.VISIBLE else View.GONE
        if (fit.lab > 0) rv.addView(R.id.rows_lab, row(model.labRow, fit.compact, liveCountdown, nowMs, elapsed))
        rv.setViewVisibility(R.id.section_lab, labVis)
        rv.setViewVisibility(R.id.rows_lab, labVis)
        rv.setViewVisibility(R.id.footer, if (fit.footer) View.VISIBLE else View.GONE)
        val clock = SpannableStringBuilder(model.footerLeft)
        if (model.clockTowerReady) {
            clock.setSpan(ForegroundColorSpan(color(R.color.m_primary)), 0, clock.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        rv.setTextViewText(R.id.foot_left, clock)
        rv.setTextViewText(R.id.foot_right, model.footerRight)
        return rv
    }

    /** The 2x2 widget: free-builder count, Sync, and the next jobs to finish as icon + countdown. */
    fun renderMini(model: WidgetModel, liveCountdown: Boolean, options: Bundle?): RemoteViews {
        val rv = RemoteViews(pkg, R.layout.widget_mini)
        rv.setOnClickPendingIntent(R.id.mini_sync, syncIntent())
        if (!model.hasData) {
            rv.setTextViewText(R.id.mini_title, "Clash Timer")
            rv.setViewVisibility(R.id.mini_rows, View.GONE)
            rv.setViewVisibility(R.id.mini_empty, View.VISIBLE)
            rv.setTextViewText(R.id.mini_empty, if (model.syncFailed) WidgetModelBuilder.SYNC_FAILED else WidgetModelBuilder.NO_DATA)
            rv.setOnClickPendingIntent(R.id.mini_panel, syncIntent())
            return rv
        }
        // Launchers reapply onto the existing views, so undo the no-data state explicitly.
        rv.setViewVisibility(R.id.mini_empty, View.GONE)
        rv.setViewVisibility(R.id.mini_rows, View.VISIBLE)
        clashIntent()?.let { rv.setOnClickPendingIntent(R.id.mini_panel, it) }
        rv.setTextViewText(R.id.mini_title, withFree(model.buildersLabel, model.buildersTotal - model.buildersBusy))

        val nowMs = System.currentTimeMillis()
        val elapsed = SystemClock.elapsedRealtime()
        val count = miniRowCount(reportedHeightDp(options))
        rv.removeAllViews(R.id.mini_rows)
        miniRows(model).take(count).forEach { rv.addView(R.id.mini_rows, miniRow(it, liveCountdown, nowMs, elapsed)) }
        return rv
    }

    private fun miniRow(model: WidgetRow, live: Boolean, nowMs: Long, elapsed: Long): RemoteViews {
        val row = if (model.kind == RowKind.RUNNING && model.finishAt != null && Finish.isDone(model.finishAt, nowMs / 1000)) {
            model.copy(kind = RowKind.COMPLETED, remaining = 0, progress = 1f)
        } else {
            model
        }
        val v = RemoteViews(pkg, R.layout.widget_row_mini)
        val iconPx = (18 * density).roundToInt()
        row.iconIds.firstNotNullOfOrNull { icons.bitmap(it.toString(), iconPx) }?.let { v.setImageViewBitmap(R.id.row_icon, it) }
        if (row.goblin) {
            icons.bitmap("goblin", (14 * density).roundToInt())?.let {
                v.setImageViewBitmap(R.id.row_goblin_icon, it)
                v.setViewVisibility(R.id.row_goblin_icon, View.VISIBLE)
            }
        }
        when (row.kind) {
            RowKind.RUNNING -> {
                v.setProgressBar(R.id.row_fill, 1000, (row.progress * 1000).roundToInt(), false)
                val segment = if (live && !finishTime) row.finishAt?.let { Countdown.segment(it, nowMs) } else null
                if (segment != null) {
                    v.setChronometer(R.id.row_chrono, elapsed + segment.chronometerMs, segment.format, true)
                    v.setChronometerCountDown(R.id.row_chrono, true)
                } else {
                    v.setViewVisibility(R.id.row_chrono, View.GONE)
                    v.setViewVisibility(R.id.row_text, View.VISIBLE)
                    v.setTextViewText(R.id.row_text, runningText(row))
                }
            }
            RowKind.COMPLETED, RowKind.IDLE -> {
                v.setViewVisibility(R.id.row_fill, View.GONE)
                v.setInt(R.id.row, "setBackgroundResource", R.drawable.row_done)
                v.setViewVisibility(R.id.row_chrono, View.GONE)
                v.setViewVisibility(R.id.row_text, View.VISIBLE)
                v.setTextViewText(R.id.row_text, if (row.kind == RowKind.COMPLETED) Lang.t("Done", "Bitti") else row.staticStatus)
            }
        }
        return v
    }

    private fun row(model: WidgetRow, compact: Boolean, live: Boolean, nowMs: Long, elapsed: Long): RemoteViews {
        // The model already marks a row done Finish.GRACE_SEC early. This also covers a model built a
        // moment before this render, so a chronometer is never started at or past its finish.
        val row = if (model.kind == RowKind.RUNNING && model.finishAt != null && Finish.isDone(model.finishAt, nowMs / 1000)) {
            model.copy(kind = RowKind.COMPLETED, button = RowButton.NONE, remaining = 0, progress = 1f)
        } else {
            model
        }
        val v =RemoteViews(pkg, if (compact) R.layout.widget_row_compact else R.layout.widget_row)
        val iconPx = ((if (compact) 20 else 24) * density).roundToInt()

        val icon = row.iconIds.firstNotNullOfOrNull { icons.bitmap(it.toString(), iconPx) }
        if (icon != null) {
            v.setImageViewBitmap(R.id.row_icon, icon)
            v.setViewVisibility(R.id.row_icon, View.VISIBLE)
            v.setViewVisibility(R.id.row_initials, View.GONE)
        } else {
            v.setTextViewText(R.id.row_initials, row.initials)
        }

        if (row.goblin) {
            v.setViewVisibility(R.id.row_goblin, View.VISIBLE)
            // Pull the badge right up to the building icon (layout margins are settable from Android 12).
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                v.setViewLayoutMargin(R.id.row_icon_slot, RemoteViews.MARGIN_END, 2f, android.util.TypedValue.COMPLEX_UNIT_DIP)
            }
            // No Goblin Builder art exists in the data package; the Goblin troop portrait stands in.
            // Face first, then a small "Goblin" note.
            val goblinPx = ((if (compact) 16 else 19) * density).roundToInt()
            (icons.bitmap("goblin", goblinPx) ?: icons.bitmap(GOBLIN_TROOP_ID, goblinPx))?.let {
                v.setImageViewBitmap(R.id.row_goblin_icon, it)
                v.setViewVisibility(R.id.row_goblin_icon, View.VISIBLE)
            }
        }
        v.setTextViewText(R.id.row_name, row.name)
        // Finished rows show just the name (e.g. "Inferno Tower   Completed ✓").
        val level = row.levelText?.takeIf { row.kind == RowKind.RUNNING }
        v.setViewVisibility(R.id.row_arrow, if (level != null && row.showArrow) View.VISIBLE else View.GONE)
        if (level != null) v.setTextViewText(R.id.row_level, level)
        else v.setViewVisibility(R.id.row_level, View.GONE)

        when (row.kind) {
            RowKind.RUNNING -> {
                v.setProgressBar(R.id.row_fill, 1000, (row.progress * 1000).roundToInt(), false)
                icons.bitmap("clock", (12 * density).roundToInt())?.let { v.setImageViewBitmap(R.id.row_clock, it) }
                val segment = if (live && !finishTime) row.finishAt?.let { Countdown.segment(it, nowMs) } else null
                if (segment != null) {
                    v.setChronometer(R.id.row_chrono, elapsed + segment.chronometerMs, segment.format, true)
                    v.setChronometerCountDown(R.id.row_chrono, true)
                } else {
                    v.setViewVisibility(R.id.row_chrono, View.GONE)
                    v.setViewVisibility(R.id.row_text, View.VISIBLE)
                    v.setTextViewText(R.id.row_text, runningText(row))
                }
            }
            RowKind.COMPLETED, RowKind.IDLE -> {
                v.setViewVisibility(R.id.row_fill, View.GONE)
                v.setInt(R.id.row, "setBackgroundResource", R.drawable.row_done)
                v.setViewVisibility(R.id.row_clock, View.GONE)
                v.setViewVisibility(R.id.row_chrono, View.GONE)
                v.setViewVisibility(R.id.row_text, View.VISIBLE)
                v.setTextViewText(R.id.row_text, row.staticStatus)
                if (row.kind == RowKind.COMPLETED) v.setViewVisibility(R.id.row_check, View.VISIBLE)
            }
        }

        if (row.button != RowButton.NONE && row.jobKey != null) {
            v.setOnClickPendingIntent(R.id.row_btn, tapIntent(WidgetActions.HELPER, row.jobKey))
        }
        when (row.button) {
            RowButton.NONE -> v.setViewVisibility(R.id.row_btn, View.INVISIBLE)
            RowButton.LINK -> Unit // green + is the layout default
            RowButton.APPRENTICE, RowButton.LAB_ASSISTANT -> {
                v.setInt(R.id.row_btn, "setBackgroundResource", R.drawable.btn_helper)
                val name = if (row.button == RowButton.APPRENTICE) "apprentice" else "lab_assistant"
                val badge = icons.bitmap(name, ((if (compact) 19 else 22) * density).roundToInt())
                if (badge != null) {
                    v.setImageViewBitmap(R.id.row_btn_icon, badge)
                    v.setViewVisibility(R.id.row_btn_icon, View.VISIBLE)
                    v.setViewVisibility(R.id.row_btn_text, View.GONE)
                } else {
                    v.setTextViewText(R.id.row_btn_text, "H")
                    textColor(v, R.id.row_btn_text, R.color.m_on_tert_cont)
                    v.setTextViewTextSize(R.id.row_btn_text, android.util.TypedValue.COMPLEX_UNIT_DIP, 10f)
                }
            }
        }
        return v
    }

    private class PillIds(
        @param:IdRes val box: Int, @param:IdRes val add: Int, @param:IdRes val undo: Int,
        @param:IdRes val icon: Int, @param:IdRes val text: Int,
    )

    private fun pill(rv: RemoteViews, ids: PillIds, pill: BoostPill, asset: String, @DrawableRes fallback: Int) {
        val bmp = icons.bitmap(asset, (14 * density).roundToInt())
        if (bmp != null) rv.setImageViewBitmap(ids.icon, bmp) else rv.setImageViewResource(ids.icon, fallback)
        rv.setTextViewText(ids.text, pill.text)
        textColor(rv, ids.text, if (pill.active) R.color.m_primary else R.color.m_on_pill)
        rv.setInt(ids.box, "setBackgroundResource", if (pill.active || pill.canUndo) R.drawable.pill_on else R.drawable.pill_off)
        rv.setOnClickPendingIntent(ids.add, tapIntent(WidgetActions.ADD_BOOST, pill.type.name))
        rv.setViewVisibility(ids.undo, if (pill.canUndo) View.VISIBLE else View.GONE)
        if (pill.canUndo) rv.setOnClickPendingIntent(ids.undo, tapIntent(WidgetActions.UNDO_BOOST, pill.type.name))
    }

    /**
     * Broadcast to [ClashWidgetReceiver] for a button tap, handled in the background (no activity, no popup).
     * The URI makes every action + argument a distinct PendingIntent.
     */
    private fun tapIntent(action: String, arg: String): PendingIntent {
        val uri = Uri.Builder().scheme("clashtimer").authority("tap").appendPath(action).appendPath(arg).build()
        val intent = Intent(context, ClashWidgetReceiver::class.java)
            .setAction(ClashWidgetReceiver.ACTION_TAP)
            .setData(uri)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /** Player name + small primary-colored `TH18`. */
    private fun title(model: WidgetModel): CharSequence {
        val sb = SpannableStringBuilder(model.playerName)
        model.townHall?.let { th ->
            val start = sb.length
            sb.append(" TH$th")
            sb.setSpan(AbsoluteSizeSpan(12, true), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.setSpan(ForegroundColorSpan(color(R.color.m_primary)), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return sb
    }

    /** `BUILDERS 6/6` + tertiary ` + Goblin`. */
    private fun buildersLabel(model: WidgetModel): CharSequence {
        val sb = SpannableStringBuilder(withFree(model.buildersLabel, model.buildersTotal - model.buildersBusy))
        if (model.goblinWorking) {
            val start = sb.length
            sb.append(" + Goblin")
            sb.setSpan(ForegroundColorSpan(color(R.color.m_tertiary)), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return sb
    }

    private fun footer(model: WidgetModel): CharSequence {
        val sb = SpannableStringBuilder()
        for (part in model.footerLeft) {
            val start = sb.length
            sb.append(part.text)
            val color = when (part.tone) {
                FooterTone.NORMAL -> null
                FooterTone.IDLE -> color(R.color.m_tertiary)
                FooterTone.ERROR -> color(R.color.m_error)
            }
            color?.let { sb.setSpan(ForegroundColorSpan(it), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
        }
        return sb
    }

    /** `BUILDERS 5/6` + an error-colored ` · 1 FREE` when builders are idle. */
    private fun withFree(label: String, free: Int): CharSequence {
        val sb = SpannableStringBuilder(label)
        if (free > 0) {
            val start = sb.length
            sb.append(Lang.t(" · $free FREE", " · $free BOŞ"))
            sb.setSpan(ForegroundColorSpan(color(R.color.m_error)), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return sb
    }

    /** A running row's text without a live chronometer: finish clock time, or the static `1d 20h`. */
    private fun runningText(row: WidgetRow): String =
        if (finishTime && row.finishAt != null) FinishClock.format(row.finishAt, System.currentTimeMillis() / 1000)
        else row.staticStatus

    /** Resolved now; used for text spans, which can't hold a resource reference. */
    private fun color(@ColorRes res: Int): Int = context.getColor(res)

    /**
     * On Android 12+ the launcher resolves the color resource itself, so the text follows
     * wallpaper (Material You) changes without waiting for the next render.
     */
    private fun textColor(rv: RemoteViews, @IdRes view: Int, @ColorRes res: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) rv.setColorStateList(view, "setTextColor", res)
        else rv.setTextColor(view, color(res))
    }

    /** Opens Clash of Clans, or null if it isn't installed. */
    private fun clashIntent(): PendingIntent? {
        val launch = context.packageManager.getLaunchIntentForPackage(CLASH_PACKAGE) ?: return null
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(context, 1, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun syncIntent(): PendingIntent {
        val intent = Intent(context, SyncActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_CLEAR_TASK,
        )
        return PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val MAX_BUILDER_ROWS = 8
        private const val CLASH_PACKAGE = "com.supercell.clashofclans"
        private const val GOBLIN_TROOP_ID = "4000002"


        /** Estimated panel height in dp with 28 dp rows (see widget_root.xml / widget_row.xml). */
        fun normalHeightDp(rows: Int): Int = 173 + 31 * rows

        /** Widget height in dp as reported by the launcher (portrait), or null if unknown. */
        fun reportedHeightDp(options: Bundle?): Int? =
            options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)?.takeIf { it > 0 }

        fun reportedWidthDp(options: Bundle?): Int? =
            options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)?.takeIf { it > 0 }

        /** 25 dp rows when the launcher says 30 dp rows won't fit. */
        fun useCompactRows(options: Bundle?, rows: Int): Boolean =
            reportedHeightDp(options)?.let { it < normalHeightDp(rows) } ?: false

        // Height estimates in dp, matching widget_root.xml / widget_row*.xml.
        private const val FIT_BASE = 68      // panel padding (10 top, 18 bottom) + header
        const val FIT_SECTION = 29   // BUILDERS / LAB / PETS line with its pills
        private const val FIT_ROW = 31
        private const val FIT_ROW_COMPACT = 27
        private const val FIT_FOOTER = 20

        /** How many rows of each section are shown, and whether the footer is. */
        data class Fit(val compact: Boolean, val builders: Int, val lab: Int, val pets: Int, val footer: Boolean)

        /**
         * Everything at 30 dp rows if it fits, else everything at 25 dp, else 25 dp rows cut so
         * nothing is clipped: builders first, then lab, then pets (a section needs room for its
         * line and one row), then the footer. Unknown height = show everything.
         */
        fun fit(heightDp: Int?, builders: Int, lab: Int, pets: Int): Fit {
            val all = Fit(false, builders, lab, pets, true)
            if (heightDp == null) return all
            val fixed = FIT_BASE + 3 * FIT_SECTION + FIT_FOOTER
            val n = builders + lab + pets
            if (fixed + n * FIT_ROW <= heightDp) return all
            if (fixed + n * FIT_ROW_COMPACT <= heightDp) return all.copy(compact = true)
            var budget = heightDp - FIT_BASE - FIT_SECTION
            val b = (budget / FIT_ROW_COMPACT).coerceIn(0, builders)
            budget -= b * FIT_ROW_COMPACT
            fun section(rows: Int): Int {
                if (rows == 0 || budget < FIT_SECTION + FIT_ROW_COMPACT) return 0
                val k = ((budget - FIT_SECTION) / FIT_ROW_COMPACT).coerceIn(1, rows)
                budget -= FIT_SECTION + k * FIT_ROW_COMPACT
                return k
            }
            val l = if (b == builders) section(lab) else 0
            val p = if (l == lab) section(pets) else 0
            return Fit(true, b, l, p, b == builders && l == lab && p == pets && budget >= FIT_FOOTER)
        }

        /** 2x2 rows: done first, then idle lab / pet house, then running soonest first. */
        fun miniRows(model: WidgetModel): List<WidgetRow> = model.allRows.sortedWith(
            compareBy<WidgetRow> {
                when (it.kind) { RowKind.COMPLETED -> 0; RowKind.IDLE -> 1; RowKind.RUNNING -> 2 }
            }.thenBy { it.finishAt ?: Long.MAX_VALUE },
        )

        /** 2x2: header ~30 dp + panel padding, 27 dp per row. Unknown height = 4 rows. */
        fun miniRowCount(heightDp: Int?): Int = heightDp?.let { ((it - 46) / 27).coerceIn(2, 8) } ?: 4
    }
}

/**
 * Icons from `assets/icons/` (64×64 WebP made by tools/gen_icons.cjs; see tools/icon_checklist.txt),
 * scaled to their on-screen size and cached. A hand-added `<name>.png` works too.
 */
class WidgetIcons private constructor(private val context: Context) {

    /** File names present in `assets/icons/`. */
    val available: Set<String> by lazy {
        runCatching { context.assets.list("icons")?.toSet() }.getOrNull() ?: emptySet()
    }

    private val cache = LruCache<String, Bitmap>(64)

    /** `assets/icons/<name>.webp` (or `.png`) scaled to [sizePx], or null if there is no such file. */
    fun bitmap(name: String, sizePx: Int): Bitmap? {
        val file = EXTENSIONS.map { "$name.$it" }.firstOrNull { it in available } ?: return null
        val key = "$file@$sizePx"
        cache.get(key)?.let { return it }
        val decoded = runCatching {
            context.assets.open("icons/$file").use { BitmapFactory.decodeStream(it) }
        }.getOrNull() ?: return null
        val scaled = if (decoded.width == sizePx && decoded.height == sizePx) decoded else {
            val scale = sizePx.toFloat() / maxOf(decoded.width, decoded.height)
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).roundToInt().coerceAtLeast(1),
                (decoded.height * scale).roundToInt().coerceAtLeast(1),
                true,
            )
        }
        cache.put(key, scaled)
        return scaled
    }

    companion object {
        private val EXTENSIONS = listOf("webp", "png")

        @Volatile private var instance: WidgetIcons? = null

        fun get(context: Context): WidgetIcons = instance ?: synchronized(this) {
            instance ?: WidgetIcons(context.applicationContext).also { instance = it }
        }
    }
}
