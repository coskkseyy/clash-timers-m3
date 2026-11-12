package com.kuswa.clashtimer.ui

import com.kuswa.clashtimer.core.Lang
import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kuswa.clashtimer.core.AppSettings
import com.kuswa.clashtimer.core.BoostSettings
import com.kuswa.clashtimer.core.GameIds
import com.kuswa.clashtimer.core.VillageExport
import com.kuswa.clashtimer.notify.Notifier

private const val H = 3600L

/**
 * Settings on the launcher screen: boost amounts, helper interval,
 * total builders, the Goblin potion toggle and notifications. Every change is saved at once and
 * the widget is redrawn by [onChange]'s caller.
 */
@Composable
fun SettingsSection(
    settings: AppSettings,
    export: VillageExport?,
    /** The screen's 1-second clock, so the notification permission state is re-read after a trip to settings. */
    now: Long,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
) {
    val context = LocalContext.current
    val b = settings.boosts
    fun boosts(f: (BoostSettings) -> BoostSettings) = onChange { it.copy(boosts = f(it.boosts)) }

    // Re-read notification permission after the system dialog or a trip to the settings app.
    var permissionTick by remember { mutableIntStateOf(0) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionTick++ }
    val notificationsAllowed = remember(now, permissionTick) { Notifier.allowed(context) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(Lang.t("Settings", "Ayarlar"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

        var editName by remember { mutableStateOf(false) }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(Lang.t("Player name", "Oyuncu adı"))
                Text(settings.displayName, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = { editName = true }) { Text(Lang.t("Change", "Değiştir")) }
        }
        if (editName) {
            PlayerNameDialog(
                current = settings.playerName,
                firstLaunch = false,
                onDone = { name -> onChange { it.copy(playerName = name) }; editName = false },
                onDismiss = { editName = false },
            )
        }

        Text(Lang.t("Potions and snacks: hours saved per hour of use", "İksirler ve atıştırmalar: 1 saat kullanımda kazanılan saat"), style = MaterialTheme.typography.labelLarge)
        HoursStepper(Lang.t("Builder potion", "İnşaat iksiri"), b.builderPotionSavingSec, 1..47) { v -> boosts { it.copy(builderPotionSavingSec = v) } }
        HoursStepper(Lang.t("Builder Bite", "İnşaatçı Atıştırması"), b.builderBiteSavingSec, 1..23) { v -> boosts { it.copy(builderBiteSavingSec = v) } }
        HoursStepper(Lang.t("Research potion", "Araştırma iksiri"), b.researchPotionSavingSec, 1..47) { v -> boosts { it.copy(researchPotionSavingSec = v) } }
        HoursStepper(Lang.t("Pet potion", "Evcil hayvan iksiri"), b.petPotionSavingSec, 1..47) { v -> boosts { it.copy(petPotionSavingSec = v) } }
        HoursStepper("Study Soup", b.studySoupSavingSec, 1..23) { v -> boosts { it.copy(studySoupSavingSec = v) } }
        SwitchRow(Lang.t("Study Soup also speeds up the Pet House", "Study Soup Hayvan Barınağı'nı da hızlandırır"), b.soupAppliesToPets) { on ->
            boosts { it.copy(soupAppliesToPets = on) }
        }

        Text(Lang.t("Helpers", "Yardımcılar"), style = MaterialTheme.typography.labelLarge)
        AutoHoursStepper(
            Lang.t("Builder's Apprentice saves per session", "İnşaatçı Çırağı bir seansta kazandırır"),
            b.apprenticeSavingSecOverride,
            auto = export?.helper(GameIds.HELPER_BUILDERS_APPRENTICE)?.level?.times(H),
            range = 1..24,
        ) { v -> boosts { it.copy(apprenticeSavingSecOverride = v) } }
        AutoHoursStepper(
            Lang.t("Lab Assistant saves per session", "Laboratuvar Asistanı bir seansta kazandırır"),
            b.labAssistantSavingSecOverride,
            auto = export?.helper(GameIds.HELPER_LAB_ASSISTANT)?.level?.times(H),
            range = 1..24,
        ) { v -> boosts { it.copy(labAssistantSavingSecOverride = v) } }
        Stepper(
            label = Lang.t("New helper session every", "Yeni yardımcı seansı her"),
            value = "${b.helperPeriodSec / H} h",
            hint = Lang.t("23 h, as measured in game", "23 saat, oyunda ölçüldü"),
            onMinus = (b.helperPeriodSec - H).takeIf { it >= 2 * H }?.let { v -> { boosts { it.copy(helperPeriodSec = v) } } },
            onPlus = (b.helperPeriodSec + H).takeIf { it <= 48 * H }?.let { v -> { boosts { it.copy(helperPeriodSec = v) } } },
        )

        Text(Lang.t("Builders", "İnşaatçılar"), style = MaterialTheme.typography.labelLarge)
        val derived = export?.derivedTotalBuilders
        val total = settings.totalBuildersOverride
        Stepper(
            label = Lang.t("Total builders", "Toplam inşaatçı"),
            value = total?.toString() ?: "Auto (${derived ?: AppSettings.DEFAULT_TOTAL_BUILDERS})",
            hint = if (total == null) Lang.t("From Builder's Huts + B.O.B's Hut in the export", "Export'taki İnşaatçı Kulübeleri + B.O.B'un Kulübesi") else null,
            onMinus = ((total ?: derived ?: AppSettings.DEFAULT_TOTAL_BUILDERS) - 1).takeIf { it >= 1 }
                ?.let { v -> { onChange { it.copy(totalBuildersOverride = v) } } },
            onPlus = ((total ?: derived ?: AppSettings.DEFAULT_TOTAL_BUILDERS) + 1).takeIf { it <= 10 }
                ?.let { v -> { onChange { it.copy(totalBuildersOverride = v) } } },
            onAuto = if (total != null) ({ onChange { it.copy(totalBuildersOverride = null) } }) else null,
        )
        SwitchRow(
            Lang.t("Potions and Bites also speed up the Goblin Builder", "İksir ve atıştırmalar Goblin İnşaatçıyı da hızlandırır"),
            b.boostsApplyToGoblin,
        ) { on -> boosts { it.copy(boostsApplyToGoblin = on) } }

        Text(Lang.t("Language", "Dil"), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((code, label) in listOf("auto" to Lang.t("Phone", "Telefon"), "en" to "English", "tr" to "Türkçe")) {
                if (settings.language == code) {
                    Button(onClick = {}) { Text(label) }
                } else {
                    OutlinedButton(onClick = { onChange { it.copy(language = code) } }) { Text(label) }
                }
            }
        }

        Text(Lang.t("Widget", "Widget"), style = MaterialTheme.typography.labelLarge)
        SwitchRow(Lang.t("Show finish time (21:14) instead of a countdown", "Geri sayım yerine bitiş saatini göster (21:14)"), settings.showFinishTime) { on ->
            onChange { it.copy(showFinishTime = on) }
        }

        Text(Lang.t("Gold Pass", "Altın Bilet"), style = MaterialTheme.typography.labelLarge)
        GoldPassStepper(Lang.t("Builder boost", "İnşaat hızlandırma"), settings.goldPassBuilderPct) { v -> onChange { it.copy(goldPassBuilderPct = v) } }
        GoldPassStepper(Lang.t("Research boost", "Araştırma hızlandırma"), settings.goldPassResearchPct) { v -> onChange { it.copy(goldPassResearchPct = v) } }
        Text(
            Lang.t("Countdowns already include it (the game shortens the timer). This only keeps the progress bars right.", "Geri sayımlar bunu zaten içeriyor (oyun süreyi kısaltıyor). Bu sadece ilerleme çubuklarını düzeltir."),
            style = MaterialTheme.typography.bodySmall,
        )

        Text(Lang.t("Notifications", "Bildirimler"), style = MaterialTheme.typography.labelLarge)
        SwitchRow(Lang.t("Notify when a builder, the lab or the pet house is free", "İnşaatçı, laboratuvar ya da Hayvan Barınağı boşalınca bildir"), settings.notificationsEnabled) { on ->
            onChange { it.copy(notificationsEnabled = on) }
            if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notifier.allowed(context)) {
                askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (settings.notificationsEnabled && !notificationsAllowed) {
            Text(
                Lang.t("Notifications are blocked for this app, so nothing will be shown.", "Bu uygulamanın bildirimleri engelli, hiçbir şey gösterilmeyecek."),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    OutlinedButton(onClick = { askPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                        Text(Lang.t("Allow", "İzin ver"))
                    }
                }
                OutlinedButton(onClick = { openNotificationSettings(context); permissionTick++ }) { Text(Lang.t("Open settings", "Ayarları aç")) }
            }
        }
        Text(
            Lang.t(
                "If notifications arrive late or not after a reboot, allow Auto-start and set battery use to " +
                    "\"No restrictions\" for Clash Timer in the phone's app settings.",
                "Bildirimler geç geliyorsa ya da yeniden başlatınca gelmiyorsa, telefonun uygulama ayarlarından " +
                    "Clash Timer için otomatik başlatmaya izin ver ve pil kullanımını \"Kısıtlama yok\" yap.",
            ),
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedButton(onClick = { openAppSettings(context) }) { Text(Lang.t("App settings (battery, auto-start)", "Uygulama ayarları (pil, otomatik başlatma)")) }

        TextButton(onClick = {
            onChange { AppSettings(liveCountdown = it.liveCountdown, playerName = it.playerName) }
        }) { Text(Lang.t("Reset settings to defaults", "Ayarları sıfırla")) }
    }
}

@Composable
private fun HoursStepper(label: String, seconds: Long, range: IntRange, onSet: (Long) -> Unit) {
    val h = (seconds / H).toInt()
    Stepper(
        label = label,
        value = "$h h (${h + 1}x)",
        onMinus = (h - 1).takeIf { it in range }?.let { v -> { onSet(v * H) } },
        onPlus = (h + 1).takeIf { it in range }?.let { v -> { onSet(v * H) } },
    )
}

/** null = automatic (helper level in hours from the export). */
@Composable
private fun AutoHoursStepper(label: String, override: Long?, auto: Long?, range: IntRange, onSet: (Long?) -> Unit) {
    val current = ((override ?: auto ?: 0L) / H).toInt()
    Stepper(
        label = label,
        value = if (override == null) "Auto (${auto?.let { "${it / H} h" } ?: "level"})" else "$current h",
        hint = if (override == null) Lang.t("Helper level = hours saved", "Yardımcı seviyesi = kazanılan saat") else null,
        onMinus = (current - 1).takeIf { it in range }?.let { v -> { onSet(v * H) } },
        onPlus = (current + 1).takeIf { it in range }?.let { v -> { onSet(v * H) } },
        onAuto = if (override != null) ({ onSet(null) }) else null,
    )
}

private val GOLD_PASS_STEPS = listOf(0, 10, 15, 20)

@Composable
private fun GoldPassStepper(label: String, pct: Int, onSet: (Int) -> Unit) {
    val i = GOLD_PASS_STEPS.indexOf(pct).coerceAtLeast(0)
    Stepper(
        label = label,
        value = if (pct == 0) Lang.t("Off", "Kapalı") else "$pct %",
        onMinus = GOLD_PASS_STEPS.getOrNull(i - 1)?.let { v -> { onSet(v) } },
        onPlus = GOLD_PASS_STEPS.getOrNull(i + 1)?.let { v -> { onSet(v) } },
    )
}

@Composable
private fun Stepper(
    label: String,
    value: String,
    onMinus: (() -> Unit)?,
    onPlus: (() -> Unit)?,
    hint: String? = null,
    onAuto: (() -> Unit)? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(label)
            hint?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        if (onAuto != null) TextButton(onClick = onAuto) { Text(Lang.t("Auto", "Otomatik")) }
        // ASCII "-" / "+": the phone's font mangles some symbols.
        TextButton(onClick = { onMinus?.invoke() }, enabled = onMinus != null) { Text("-", fontWeight = FontWeight.Bold) }
        Text(value, textAlign = TextAlign.Center, modifier = Modifier.width(96.dp))
        TextButton(onClick = { onPlus?.invoke() }, enabled = onPlus != null) { Text("+", fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }.onFailure { openAppSettings(context) }
}

fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
