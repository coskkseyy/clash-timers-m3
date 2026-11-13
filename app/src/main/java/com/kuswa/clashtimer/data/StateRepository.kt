package com.kuswa.clashtimer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kuswa.clashtimer.core.AppSettings
import com.kuswa.clashtimer.core.AppState
import com.kuswa.clashtimer.core.NameMap
import com.kuswa.clashtimer.core.SyncLog
import com.kuswa.clashtimer.core.SyncLogEntry
import com.kuswa.clashtimer.core.UpgradeTimes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "clash_timers")

/**
 * All persisted state lives in one Preferences DataStore: [AppState] and [AppSettings],
 * each stored as a JSON string under its own key.
 */
class StateRepository(context: Context, account: String = "") {
    private val store = context.applicationContext.dataStore

    /** Each account has its own state; "" is the first one (and every widget placed before accounts existed). */
    private val stateKey = if (account.isEmpty()) STATE else stringPreferencesKey("state_json_$account")

    val state: Flow<AppState> = store.data.map { AppState.decode(it[stateKey]) }.distinctUntilChanged()
    val settings: Flow<AppSettings> = store.data.map { AppSettings.decode(it[SETTINGS]) }.distinctUntilChanged()
    val syncLog: Flow<SyncLog> = store.data.map { SyncLog.decode(it[SYNC_LOG]) }.distinctUntilChanged()

    suspend fun currentState(): AppState = state.first()
    suspend fun currentSettings(): AppSettings = settings.first()

    /** Atomically read-modify-write the app state; returns the value [transform] produced. */
    suspend fun <R> updateState(transform: (AppState) -> Pair<AppState, R>): R {
        var result: R? = null
        store.edit { prefs ->
            val (next, r) = transform(AppState.decode(prefs[stateKey]))
            prefs[stateKey] = next.encode()
            result = r
        }
        @Suppress("UNCHECKED_CAST")
        return result as R
    }

    /** Appends to the on-screen sync log; never throws (it is diagnostics only). */
    suspend fun log(source: String, ok: Boolean?, message: String) {
        runCatching {
            store.edit { prefs ->
                val entry = SyncLogEntry(System.currentTimeMillis() / 1000, source, ok, message)
                prefs[SYNC_LOG] = SyncLog.decode(prefs[SYNC_LOG]).add(entry).encode()
            }
        }
    }

    /** Which account a widget shows; widgets that were never configured show the first one (""). */
    suspend fun accountFor(widgetId: Int): String = bindingsOf(store.data.first())[widgetId] ?: ""

    suspend fun bind(widgetId: Int, account: String) {
        store.edit { prefs ->
            val all = bindingsOf(prefs) + (widgetId to account)
            prefs[BINDINGS] = all.entries.joinToString(";") { "${it.key}=${it.value}" }
        }
    }

    /** "" plus every account a widget has been bound to. */
    suspend fun knownAccounts(): List<String> = (listOf("") + bindingsOf(store.data.first()).values).distinct()

    suspend fun newAccountId(): String = ((knownAccounts().mapNotNull { it.toIntOrNull() }.maxOrNull() ?: 1) + 1).toString()

    private fun bindingsOf(prefs: Preferences): Map<Int, String> =
        prefs[BINDINGS].orEmpty().split(';').mapNotNull { entry ->
            val i = entry.indexOf('=')
            if (i <= 0) null else entry.substring(0, i).toIntOrNull()?.let { it to entry.substring(i + 1) }
        }.toMap()

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        store.edit { prefs -> prefs[SETTINGS] = transform(AppSettings.decode(prefs[SETTINGS])).encode() }
    }

    private companion object {
        val STATE = stringPreferencesKey("state_json")
        val SETTINGS = stringPreferencesKey("settings_json")
        val SYNC_LOG = stringPreferencesKey("sync_log_json")
        val BINDINGS = stringPreferencesKey("widget_accounts")
    }
}

/** Bundled `assets/names.json`, loaded once per process. */
object Names {
    @Volatile private var cached: NameMap? = null

    fun get(context: Context): NameMap = cached ?: synchronized(this) {
        cached ?: context.applicationContext.assets.open("names.json").bufferedReader().use {
            NameMap.fromJson(it.readText())
        }.let { en ->
            runCatching {
                context.applicationContext.assets.open("names_tr.json").bufferedReader().use { en.withTurkish(it.readText()) }
            }.getOrDefault(en)
        }.also { cached = it }
    }
}

/** Bundled `assets/upgrade_times.json` (full upgrade durations for the progress bars), loaded once. */
object Times {
    @Volatile private var cached: UpgradeTimes? = null

    fun get(context: Context): UpgradeTimes = cached ?: synchronized(this) {
        cached ?: runCatching {
            context.applicationContext.assets.open("upgrade_times.json").bufferedReader().use {
                UpgradeTimes.fromJson(it.readText())
            }
        }.getOrDefault(UpgradeTimes.EMPTY).also { cached = it }
    }
}
