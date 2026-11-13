package com.kuswa.clashtimer.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.kuswa.clashtimer.core.AppSettings
import com.kuswa.clashtimer.core.Lang
import com.kuswa.clashtimer.data.StateRepository
import com.kuswa.clashtimer.ui.theme.ClashTimerTheme
import kotlinx.coroutines.launch

/**
 * Opens when a widget is placed and from the widget's long-press menu: picks the account the
 * widget shows (an existing one or a new one) and the name on its header. The widget's Sync
 * button then only ever writes that account.
 */
class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Backing out cancels, and the launcher drops a widget that was being placed.
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        setContent {
            ClashTimerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ConfigScreen(widgetId) {
                        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                        finish()
                    }
                }
            }
        }
    }
}

private const val NEW_ACCOUNT = "+"

private data class AccountItem(val id: String, val name: String, val tag: String?)

@Composable
private fun ConfigScreen(widgetId: Int, onSaved: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { StateRepository(context) }
    var items by remember { mutableStateOf<List<AccountItem>?>(null) }
    var selected by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val settings = repo.currentSettings()
        Lang.apply(settings)
        val loaded = repo.knownAccounts().map { id ->
            val state = StateRepository(context, id).currentState()
            AccountItem(id, settings.forAccount(id, state).displayName, state.export?.tag)
        }
        val current = repo.accountFor(widgetId)
        items = loaded
        selected = current
        name = loaded.firstOrNull { it.id == current }?.name.orEmpty()
    }

    val list = items
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(Lang.t("Which account?", "Hangi hesap?"), style = MaterialTheme.typography.headlineSmall)
        Text(
            Lang.t(
                "This widget shows one account. Its Sync button only updates that account.",
                "Bu widget tek bir hesabı gösterir. Sync tuşu sadece o hesabı günceller.",
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        if (list == null) return@Column
        val choices = list.map { it.id to (it.name + (it.tag?.let { t -> "  ($t)" } ?: "")) } +
            (NEW_ACCOUNT to Lang.t("New account", "Yeni hesap"))
        choices.forEach { (id, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selected = id
                        name = list.firstOrNull { it.id == id }?.name.orEmpty()
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = selected == id, onClick = {
                    selected = id
                    name = list.firstOrNull { it.id == id }?.name.orEmpty()
                })
                Text(label, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(AppSettings.MAX_PLAYER_NAME) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(Lang.t("Name on the widget", "Widget'taki isim")) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                scope.launch {
                    val id = if (selected == NEW_ACCOUNT) repo.newAccountId() else selected
                    val clean = AppSettings.cleanName(name)
                    if (id.isEmpty()) {
                        // The first account keeps using the name from Settings.
                        repo.updateSettings { it.copy(playerName = clean) }
                    } else {
                        val fallback = Lang.t("Account", "Hesap") + " $id"
                        StateRepository(context, id).updateState { it.copy(name = clean.ifEmpty { fallback }) to Unit }
                    }
                    repo.bind(widgetId, id)
                    WidgetUpdater.updateAll(context, "config")
                    onSaved()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(Lang.t("Done", "Tamam")) }
    }
}
