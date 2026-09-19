package com.mtkclientandroid.ui.console

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtkclientandroid.engine.CommandSupport
import com.mtkclientandroid.engine.DeviceUiState
import com.mtkclientandroid.engine.MtkViewModel
import com.mtkclientandroid.ui.components.StatusIndicator

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConsoleScreen(
    vm: MtkViewModel,
    modifier: Modifier = Modifier,
    onOpenFiles: () -> Unit
) {
    val text by vm.terminalText.collectAsState()
    val state by vm.deviceState.collectAsState()
    val info by vm.deviceInfo.collectAsState()
    val lastError by vm.lastError.collectAsState()
    val history by vm.history.collectAsState()
    val pinned = remember(vm.pinned.collectAsState().value) { vm.pinnedCommands() }
    var input by remember { mutableStateOf("") }
    var partition by remember { mutableStateOf("") }
    var historyOpen by remember { mutableStateOf(false) }
    var pendingDestructive by remember { mutableStateOf<String?>(null) }
    val scroll = rememberScrollState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(text) { scroll.animateScrollTo(scroll.maxValue) }

    val dumpLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> vm.onDumpUri(uri) }

    val flashLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> vm.onFlashUri(uri) }

    val exportLogLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
        } catch (_: Exception) { }
    }

    fun submit(cmd: String) {
        if (CommandSupport.isDestructive(cmd)) pendingDestructive = cmd
        else vm.submitTyped(cmd)
    }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (info.vid != null) {
                val vid = info.vid!!
                val pid = info.pid ?: 0
                val chip = info.chip
                val cfg = info.targetConfig
                val label = buildString {
                    append("VID %04X  PID %04X".format(vid, pid))
                    if (!chip.isNullOrBlank()) append("  ·  $chip")
                    if (!cfg.isNullOrBlank()) append("  ·  $cfg")
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Text(
                    " ",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f)
                )
            }
            StatusIndicator(state)
        }

        SelectionContainer(Modifier.weight(1f)) {
            Text(
                text = text,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (state is DeviceUiState.Running) {
            val running = state as DeviceUiState.Running
            if (running.progress != null) {
                LinearProgressIndicator(
                    progress = { running.progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        lastError?.let { err ->
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        err.friendly,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { clipboard.setText(AnnotatedString(err.detail)) }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy error details")
                    }
                }
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (pinned.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pinned.forEach { cmd ->
                        AssistChip(
                            onClick = { submit(cmd.cmd) },
                            label = { Text(cmd.title) }
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(onClick = { vm.detectDevice() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Usb, contentDescription = null)
                    Text("Detect device", modifier = Modifier.padding(start = 8.dp))
                }
                if (state is DeviceUiState.Running) {
                    OutlinedButton(onClick = { vm.cancelCommand() }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Stop, contentDescription = null)
                        Text("Stop", modifier = Modifier.padding(start = 8.dp))
                    }
                } else {
                    OutlinedButton(onClick = onOpenFiles, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Folder, contentDescription = null)
                        Text("Dumped files", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = partition,
                    onValueChange = { partition = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Partition") },
                    placeholder = { Text("boot") }
                )
                Button(onClick = {
                    vm.beginDump(partition)?.let { dumpLauncher.launch(it) }
                }) { Text("Dump") }
                OutlinedButton(onClick = {
                    if (vm.beginFlash(partition)) {
                        pendingDestructive = "w ${partition.trim()}"
                    }
                }) { Text("Flash") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Command") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = {
                        val cmd = input
                        input = ""
                        submit(cmd)
                    })
                )
                Button(
                    onClick = {
                        val cmd = input
                        input = ""
                        submit(cmd)
                    },
                    enabled = state !is DeviceUiState.Running,
                    modifier = Modifier.padding(start = 8.dp)
                ) { Text("Run") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                BoxWithHistory(history, historyOpen, { historyOpen = it }) { cmd ->
                    input = cmd
                    historyOpen = false
                }
                TextButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(send, "Share log"))
                }) {
                    Icon(Icons.Outlined.IosShare, contentDescription = null)
                    Text("Share log", modifier = Modifier.padding(start = 6.dp))
                }
                TextButton(onClick = { exportLogLauncher.launch("mtk-client-log.txt") }) {
                    Text("Export log")
                }
            }
        }
    }

    pendingDestructive?.let { cmd ->
        AlertDialog(
            onDismissRequest = {
                if (cmd.startsWith("w ")) vm.onFlashUri(null)
                pendingDestructive = null
            },
            title = { Text("Confirm destructive command") },
            text = {
                Text(
                    "\"$cmd\" can erase or overwrite data on the connected device and cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val toRun = cmd
                    pendingDestructive = null
                    if (toRun.startsWith("w ") && vm.pendingFlashPartition != null) {
                        flashLauncher.launch(arrayOf("*/*"))
                    } else {
                        vm.submitTyped(toRun)
                    }
                }) { Text("Run") }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (cmd.startsWith("w ")) vm.onFlashUri(null)
                    pendingDestructive = null
                }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun BoxWithHistory(
    history: List<String>,
    open: Boolean,
    onOpen: (Boolean) -> Unit,
    onPick: (String) -> Unit
) {
    androidx.compose.foundation.layout.Box {
        TextButton(onClick = { onOpen(true) }, enabled = history.isNotEmpty()) {
            Text("History")
        }
        DropdownMenu(expanded = open, onDismissRequest = { onOpen(false) }) {
            history.take(12).forEach { cmd ->
                DropdownMenuItem(text = { Text(cmd, fontFamily = FontFamily.Monospace) }, onClick = { onPick(cmd) })
            }
        }
    }
}
