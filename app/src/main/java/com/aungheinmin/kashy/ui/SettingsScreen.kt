package com.aungheinmin.kashy.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aungheinmin.kashy.R
import com.aungheinmin.kashy.data.AmountStyle
import com.aungheinmin.kashy.data.AppLanguage
import com.aungheinmin.kashy.data.Backup
import com.aungheinmin.kashy.data.BackupCodec
import com.aungheinmin.kashy.data.BackupFormat
import com.aungheinmin.kashy.data.Currency
import com.aungheinmin.kashy.data.DigitGrouping
import com.aungheinmin.kashy.data.KyatLabel
import com.aungheinmin.kashy.data.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

private class PendingImport(val fileName: String, val backup: Backup, val format: BackupFormat)

@Composable
fun SettingsScreen(
    viewModel: HomeViewModel,
    themeMode: ThemeMode,
    language: AppLanguage,
    style: AmountStyle,
    onOpenWallets: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val res = context.resources
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val shortAmounts = LocalShortAmounts.current
    var exporting by rememberSaveable { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<PendingImport?>(null) }

    fun notify(message: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
    }

    fun write(format: BackupFormat, uri: Uri?) {
        if (uri == null) return
        scope.launch {
            val result = runCatching {
                val backup = viewModel.exportBackup()
                val text = BackupCodec.encode(backup, format, ZoneId.systemDefault())
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)!!.use { it.write(text.toByteArray()) }
                }
                backup.entries.size
            }
            notify(
                result.fold(
                    { res.getQuantityString(R.plurals.exported_entries, it, it) },
                    { res.getString(R.string.export_failed) },
                )
            )
        }
    }

    val jsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BackupFormat.JSON.mimeType)) {
        write(BackupFormat.JSON, it)
    }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BackupFormat.CSV.mimeType)) {
        write(BackupFormat.CSV, it)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val text = context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
                    val (backup, format) = BackupCodec.decode(text, ZoneId.systemDefault())
                    PendingImport(displayName(context, uri), backup, format)
                }
            }.onSuccess { pendingImport = it }
                .onFailure { notify(res.getString(R.string.import_failed)) }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar, Modifier.padding(bottom = BOTTOM_BAR_SPACE)) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = padding.calculateBottomPadding() + BOTTOM_BAR_SPACE + 8.dp),
        ) {
            Text(
                stringResource(R.string.settings),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp),
            )

            SectionHeader(stringResource(R.string.appearance))
            SettingsCard {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(stringResource(R.string.theme), style = MaterialTheme.typography.bodyMedium)
                    val labels = mapOf(
                        ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                        ThemeMode.LIGHT to stringResource(R.string.theme_light),
                        ThemeMode.DARK to stringResource(R.string.theme_dark),
                    )
                    SegmentedChoice(
                        options = ThemeMode.entries,
                        selected = themeMode,
                        label = { labels.getValue(it) },
                        onSelect = viewModel::setThemeMode,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }

            SectionHeader(stringResource(R.string.language))
            SettingsCard {
                AppLanguage.entries.forEachIndexed { index, option ->
                    if (index > 0) RowDivider()
                    SettingsRow(
                        title = stringResource(
                            when (option) {
                                AppLanguage.SYSTEM -> R.string.system_default
                                AppLanguage.ENGLISH -> R.string.language_english
                                AppLanguage.BURMESE -> R.string.language_burmese
                                AppLanguage.JAPANESE -> R.string.language_japanese
                            }
                        ),
                        onClick = { viewModel.setLanguage(option) },
                    ) { RadioButton(selected = option == language, onClick = null) }
                }
            }

            SectionHeader(stringResource(R.string.amounts))
            SettingsCard {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(stringResource(R.string.preview), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    listOf(Currency.MMK to 1_250_000_00L, Currency.USD to 1_250_50L, Currency.JPY to 125_000_00L).forEach { (currency, minor) ->
                        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            Text(stringResource(currency.label), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text(
                                if (shortAmounts) formatShort(minor, currency) else formatMoney(minor, currency),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                RowDivider()
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(stringResource(R.string.kyat_shown_as), style = MaterialTheme.typography.bodyMedium)
                    SegmentedChoice(
                        options = KyatLabel.entries,
                        selected = style.kyat,
                        label = { it.text },
                        onSelect = { viewModel.setAmountStyle(style.copy(kyat = it)) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
                RowDivider()
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(stringResource(R.string.digit_grouping), style = MaterialTheme.typography.bodyMedium)
                    SegmentedChoice(
                        options = DigitGrouping.entries,
                        selected = style.grouping,
                        label = {
                            when (it) {
                                DigitGrouping.COMMA -> "1,000,000"
                                DigitGrouping.SPACE -> "1 000 000"
                                DigitGrouping.PERIOD -> "1.000.000"
                            }
                        },
                        onSelect = { viewModel.setAmountStyle(style.copy(grouping = it)) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
                RowDivider()
                SettingsRow(
                    title = stringResource(R.string.short_amounts),
                    subtitle = stringResource(R.string.short_amounts_hint),
                    onClick = { viewModel.setShortAmounts(!shortAmounts) },
                ) { Switch(checked = shortAmounts, onCheckedChange = null) }
                RowDivider()
                SettingsRow(
                    title = stringResource(R.string.myanmar_digits),
                    subtitle = stringResource(R.string.myanmar_digits_hint),
                    onClick = { viewModel.setAmountStyle(style.copy(myanmarDigits = !style.myanmarDigits)) },
                ) { Switch(checked = style.myanmarDigits, onCheckedChange = null) }
            }

            SectionHeader(stringResource(R.string.data))
            SettingsCard {
                SettingsRow(
                    title = stringResource(R.string.export_data),
                    subtitle = stringResource(R.string.export_data_hint),
                    onClick = { exporting = true },
                ) { Chevron() }
                RowDivider()
                SettingsRow(
                    title = stringResource(R.string.import_data),
                    subtitle = stringResource(R.string.import_data_hint),
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream")) },
                ) { Chevron() }
            }

            SectionHeader(stringResource(R.string.general))
            SettingsCard {
                SettingsRow(
                    title = stringResource(R.string.wallets),
                    subtitle = pluralStringResource(R.plurals.wallet_count, state.wallets.size, state.wallets.size),
                    onClick = onOpenWallets,
                ) { Chevron() }
            }

            SectionHeader(stringResource(R.string.about))
            SettingsCard {
                val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }
                SettingsRow(title = stringResource(R.string.version)) {
                    Text(version, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (exporting) {
        ExportSheet(
            walletCount = state.wallets.size,
            entryCount = state.wallets.sumOf { it.entryCount },
            onDismiss = { exporting = false },
            onExport = { format ->
                exporting = false
                val name = "kashy-${LocalDate.now()}.${format.extension}"
                when (format) {
                    BackupFormat.JSON -> jsonLauncher.launch(name)
                    BackupFormat.CSV -> csvLauncher.launch(name)
                }
            },
        )
    }

    pendingImport?.let { pending ->
        ImportSheet(
            pending = pending,
            onDismiss = { pendingImport = null },
            onImport = { replace ->
                pendingImport = null
                scope.launch {
                    val added = runCatching { viewModel.importBackup(pending.backup, replace) }
                    notify(
                        added.fold(
                            { res.getQuantityString(R.plurals.imported_entries, it, it) },
                            { res.getString(R.string.import_failed) },
                        )
                    )
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportSheet(walletCount: Int, entryCount: Int, onDismiss: () -> Unit, onExport: (BackupFormat) -> Unit) {
    var format by rememberSaveable { mutableStateOf(BackupFormat.JSON) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.export_as), style = MaterialTheme.typography.titleMedium)
            OptionCard("📦", stringResource(R.string.export_json), stringResource(R.string.export_json_hint), format == BackupFormat.JSON) {
                format = BackupFormat.JSON
            }
            OptionCard("📊", stringResource(R.string.export_csv), stringResource(R.string.export_csv_hint), format == BackupFormat.CSV) {
                format = BackupFormat.CSV
            }
            Text(
                pluralStringResource(R.plurals.wallet_count, walletCount, walletCount) + " · " +
                    pluralStringResource(R.plurals.entry_count, entryCount, entryCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = { onExport(format) }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(stringResource(R.string.export))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportSheet(pending: PendingImport, onDismiss: () -> Unit, onImport: (replace: Boolean) -> Unit) {
    var replace by rememberSaveable { mutableStateOf(false) }
    val walletCount = pending.backup.wallets.size
    val entryCount = pending.backup.entries.size
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.import_data), style = MaterialTheme.typography.titleMedium)
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OptionIcon("📄")
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(pending.fileName, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        stringResource(if (pending.format == BackupFormat.JSON) R.string.kashy_backup else R.string.csv_file) + " · " +
                            pluralStringResource(R.plurals.wallet_count, walletCount, walletCount) + " · " +
                            pluralStringResource(R.plurals.entry_count, entryCount, entryCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                stringResource(R.string.how_to_import),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            OptionCard("＋", stringResource(R.string.import_add), stringResource(R.string.import_add_hint), !replace) { replace = false }
            OptionCard("↺", stringResource(R.string.import_replace), stringResource(R.string.import_replace_hint), replace) { replace = true }
            if (replace) {
                Text(
                    stringResource(R.string.import_replace_warning),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = { onImport(replace) },
                colors = if (replace) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors(),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) { Text(stringResource(if (replace) R.string.replace_and_import else R.string.import_action)) }
        }
    }
}

@Composable
private fun OptionCard(icon: String, title: String, hint: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.outlineVariant, RoundedCornerShape(12.dp))
            .background(if (selected) colors.surfaceContainer else colors.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OptionIcon(icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(hint, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
        RadioButton(selected = selected, onClick = null)
    }
}

@Composable
private fun OptionIcon(icon: String) {
    Box(
        Modifier.size(36.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) { Text(icon, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSecondaryContainer) }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 16.dp, bottom = 6.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp)),
        content = content,
    )
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        trailing()
    }
}

@Composable
private fun RowDivider() = HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun Chevron() = Icon(
    Icons.AutoMirrored.Filled.KeyboardArrowRight,
    contentDescription = null,
    tint = MaterialTheme.colorScheme.onSurfaceVariant,
)

private fun displayName(context: Context, uri: Uri): String =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    } ?: uri.lastPathSegment.orEmpty()
