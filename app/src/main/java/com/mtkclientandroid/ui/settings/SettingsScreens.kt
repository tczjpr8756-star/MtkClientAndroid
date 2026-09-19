package com.mtkclientandroid.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mtkclientandroid.BuildConfig
import com.mtkclientandroid.engine.ColorPreset
import com.mtkclientandroid.engine.MtkViewModel
import com.mtkclientandroid.engine.ThemeMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsHomeScreen(
    vm: MtkViewModel,
    modifier: Modifier = Modifier,
    onProtocols: () -> Unit,
    onAppearance: () -> Unit,
    onTerminal: () -> Unit,
    onAbout: () -> Unit,
    onFiles: () -> Unit
) {
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(vm.exportSettings().toByteArray())
            }
        } catch (_: Exception) { }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return@rememberLauncherForActivityResult
            vm.importSettings(json)
        } catch (_: Exception) { }
    }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopAppBar(title = { Text("Settings") })
        ListItem(
            headlineContent = { Text("Terminal") },
            supportingContent = { Text("Repeat filter, window, and log clearing") },
            leadingContent = { Icon(Icons.Outlined.Terminal, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onTerminal)
        )
        ListItem(
            headlineContent = { Text("Protocols") },
            supportingContent = { Text("Persistent defaults for mtkclient flags") },
            leadingContent = { Icon(Icons.Outlined.Memory, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onProtocols)
        )
        ListItem(
            headlineContent = { Text("Appearance") },
            supportingContent = { Text("Theme and color palette") },
            leadingContent = { Icon(Icons.Outlined.Palette, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onAppearance)
        )
        ListItem(
            headlineContent = { Text("Dumped files") },
            supportingContent = { Text("Share, export, rename, or delete dumps") },
            leadingContent = { Icon(Icons.Outlined.Folder, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onFiles)
        )
        ListItem(
            headlineContent = { Text("About") },
            supportingContent = { Text("Version and credits") },
            leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onAbout)
        )
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        ListItem(
            headlineContent = { Text("Export settings") },
            supportingContent = { Text("Save a backup of preferences") },
            modifier = Modifier.clickable { exportLauncher.launch("mtk-client-settings.json") }
        )
        ListItem(
            headlineContent = { Text("Import settings") },
            supportingContent = { Text("Restore preferences from a backup") },
            modifier = Modifier.clickable { importLauncher.launch(arrayOf("application/json", "*/*")) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TerminalSettingsScreen(vm: MtkViewModel, modifier: Modifier = Modifier, onBack: () -> Unit) {
    val s by vm.terminalSettings.collectAsState()
    var window by remember(s.filterWindowSeconds) { mutableStateOf(s.filterWindowSeconds.toString()) }
    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Terminal") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingSwitch(
                title = "Suppress repeated lines",
                description = "Hide identical log lines for the duration of the filter window.",
                checked = s.suppressRepeats,
                onChecked = { vm.updateTerminalSettings(s.copy(suppressRepeats = it)) }
            )
            OutlinedTextField(
                value = window,
                onValueChange = {
                    window = it.filter { ch -> ch.isDigit() }
                    val secs = window.toLongOrNull() ?: return@OutlinedTextField
                    vm.updateTerminalSettings(s.copy(filterWindowSeconds = secs))
                },
                label = { Text("Filter window (seconds)") },
                supportingText = { Text("Identical lines are suppressed for this many seconds. Use 0 to show every line.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(onClick = { vm.clearTerminal() }, modifier = Modifier.fillMaxWidth()) {
                Text("Clear terminal")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProtocolsScreen(vm: MtkViewModel, modifier: Modifier = Modifier, onBack: () -> Unit) {
    val s by vm.protocol.collectAsState()
    var ptypeOpen by remember { mutableStateOf(false) }
    val pickPreloader = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.copyPickedLoader(uri, MtkViewModel.LoaderKind.PRELOADER)
    }
    val pickDa = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.copyPickedLoader(uri, MtkViewModel.LoaderKind.DA)
    }
    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Protocols") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingSwitch(
                "Verbose logging",
                "Writes detailed engine output to the terminal.",
                s.debugMode
            ) { vm.updateProtocol(s.copy(debugMode = it)) }
            SettingSwitch(
                "Skip watchdog initialization",
                "Skips watchdog timer setup during the BootROM and Preloader handshake.",
                s.skipWatchdog
            ) { vm.updateProtocol(s.copy(skipWatchdog = it)) }
            SettingSwitch(
                "Already in Preloader",
                "Skip the BootROM crash step before running a payload. Use when the device is already waiting in Preloader.",
                s.alreadyInPreloader
            ) { vm.updateProtocol(s.copy(alreadyInPreloader = it)) }
            SettingSwitch(
                "Read SoC identifier",
                "Reads the device SoC identifier as part of the connection sequence.",
                s.readSocId
            ) { vm.updateProtocol(s.copy(readSocId = it)) }
            SettingSwitch(
                "Use custom Preloader / DA",
                "Use the files picked below instead of the auto-detected loader for supported commands.",
                s.useCustomLoader
            ) { vm.updateProtocol(s.copy(useCustomLoader = it)) }

            val ptypeLabel = if (s.payloadType.isBlank()) "Auto" else s.payloadType
            Column {
                Text("Payload type", style = MaterialTheme.typography.titleSmall)
                Text("Exploit method used to reach BootROM or Preloader.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { ptypeOpen = true }) { Text(ptypeLabel) }
                DropdownMenu(expanded = ptypeOpen, onDismissRequest = { ptypeOpen = false }) {
                    listOf("" to "Auto", "amonet" to "amonet", "kamakiri" to "kamakiri", "kamakiri2" to "kamakiri2", "carbonara" to "carbonara").forEach { (value, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = {
                            vm.updateProtocol(s.copy(payloadType = value))
                            ptypeOpen = false
                        })
                    }
                }
            }
            OutlinedTextField(
                value = s.vid, onValueChange = { vm.updateProtocol(s.copy(vid = it)) },
                label = { Text("USB vendor ID") },
                supportingText = { Text("Override used during device detection. Leave empty for auto-detect.") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = s.pid, onValueChange = { vm.updateProtocol(s.copy(pid = it)) },
                label = { Text("USB product ID") },
                supportingText = { Text("Override used during device detection. Leave empty for auto-detect.") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = s.sectorSize, onValueChange = { vm.updateProtocol(s.copy(sectorSize = it)) },
                label = { Text("Sector size") },
                supportingText = { Text("Default storage sector size for dump and flash addressing.") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Text("Preloader file: ${s.preloaderName ?: "Not set"}", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pickPreloader.launch(arrayOf("*/*")) }) { Text("Choose preloader") }
                TextButton(onClick = { vm.clearLoader(MtkViewModel.LoaderKind.PRELOADER) }, enabled = s.preloaderPath != null) { Text("Clear") }
            }
            Text("DA file: ${s.loaderName ?: "Not set"}", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pickDa.launch(arrayOf("*/*")) }) { Text("Choose DA") }
                TextButton(onClick = { vm.clearLoader(MtkViewModel.LoaderKind.DA) }, enabled = s.loaderPath != null) { Text("Clear") }
            }
            Button(onClick = { vm.saveProtocol() }, modifier = Modifier.fillMaxWidth()) {
                Text("Save protocol settings")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(vm: MtkViewModel, modifier: Modifier = Modifier, onBack: () -> Unit) {
    val s by vm.appearance.collectAsState()
    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Appearance") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("Theme", style = MaterialTheme.typography.titleMedium)
            ThemeMode.entries.forEach { mode ->
                val label = when (mode) {
                    ThemeMode.SYSTEM -> "System"
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.DARK -> "Dark"
                    ThemeMode.OLED -> "OLED"
                }
                val desc = when (mode) {
                    ThemeMode.SYSTEM -> "Follow the device light or dark setting."
                    ThemeMode.LIGHT -> "Light surfaces."
                    ThemeMode.DARK -> "Dark surfaces."
                    ThemeMode.OLED -> "True black backgrounds for OLED displays."
                }
                ListItem(
                    headlineContent = { Text(label) },
                    supportingContent = { Text(desc) },
                    leadingContent = { RadioButton(selected = s.themeMode == mode, onClick = { vm.updateAppearance(s.copy(themeMode = mode)) }) },
                    modifier = Modifier.clickable { vm.updateAppearance(s.copy(themeMode = mode)) }
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Text("Color palette", style = MaterialTheme.typography.titleMedium)
            ColorPreset.entries.forEach { preset ->
                val (label, desc) = when (preset) {
                    ColorPreset.MATERIAL_YOU -> "Material You" to "Wallpaper-derived colors on Android 12 and later."
                    ColorPreset.STOCK_ANDROID -> "Stock Android" to "Google’s default Material You palette. Colors only."
                    ColorPreset.TEAL -> "Teal" to "Cool teal accent on near-neutral surfaces."
                    ColorPreset.GRAPHITE -> "Graphite" to "Neutral gray hardware palette."
                    ColorPreset.FOREST -> "Forest" to "Muted green accent."
                }
                ListItem(
                    headlineContent = { Text(label) },
                    supportingContent = { Text(desc) },
                    leadingContent = { RadioButton(selected = s.colorPreset == preset, onClick = { vm.updateAppearance(s.copy(colorPreset = preset)) }) },
                    modifier = Modifier.clickable { vm.updateAppearance(s.copy(colorPreset = preset)) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(modifier: Modifier = Modifier, onBack: () -> Unit) {
    val context = LocalContext.current
    fun open(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("About") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("MTK Client", style = MaterialTheme.typography.headlineMedium)
            Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "This application provides an Android interface to mtkclient, supporting partition read and write, bootloader unlock, and Download Agent bypass operations on supported MediaTek devices over USB, without requiring a desktop computer.",
                style = MaterialTheme.typography.bodyLarge
            )
            HorizontalDivider()
            Text("Credits", style = MaterialTheme.typography.titleLarge)
            ListItem(
                headlineContent = { Text("mtkclient") },
                supportingContent = { Text("(c) B. Kerler, 2018–2025, licensed under GPLv3.") },
                modifier = Modifier.clickable { open("https://github.com/bkerler/mtkclient") }
            )
            ListItem(
                headlineContent = { Text("bypass_utility") },
                supportingContent = { Text("(c) Dinolek, 2021, licensed under the MIT License.") },
                modifier = Modifier.clickable { open("https://github.com/MTK-bypass/bypass_utility") }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChecked) }
    )
}
