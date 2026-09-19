package com.mtkclientandroid.ui.commands

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.mtkclientandroid.McCatalog
import com.mtkclientandroid.McCommand
import com.mtkclientandroid.engine.CommandCategory
import com.mtkclientandroid.engine.CommandSupport
import com.mtkclientandroid.engine.DeviceUiState
import com.mtkclientandroid.engine.MtkViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CommandsScreen(
    vm: MtkViewModel,
    modifier: Modifier = Modifier,
    review: McCommand?,
    onOpenReview: (McCommand) -> Unit,
    onCloseReview: () -> Unit
) {
    if (review != null) {
        CommandReviewScreen(vm, review, modifier, onCloseReview)
        return
    }
    var query by remember { mutableStateOf("") }
    var categoryIndex by remember { mutableIntStateOf(0) }
    val grid by vm.gridView.collectAsState()
    val pinned by vm.pinned.collectAsState()
    val state by vm.deviceState.collectAsState()
    val categories = CommandCategory.entries
    val selected = categories[categoryIndex]
    val commands = McCatalog.groups
        .first { it.category == selected }
        .commands
        .filter {
            query.isBlank() ||
                it.title.contains(query, true) ||
                it.cmd.contains(query, true) ||
                it.description.contains(query, true)
        }

    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true,
            label = { Text("Search commands") }
        )
        ScrollableTabRow(selectedTabIndex = categoryIndex, edgePadding = 16.dp) {
            categories.forEachIndexed { i, cat ->
                Tab(
                    selected = i == categoryIndex,
                    onClick = { categoryIndex = i },
                    text = { Text(cat.titleRes) }
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state is DeviceUiState.Running) {
                TextButton(onClick = { vm.cancelCommand() }) {
                    Icon(Icons.Outlined.Stop, contentDescription = null)
                    Text("Stop command", modifier = Modifier.padding(start = 6.dp))
                }
            } else {
                Spacer(Modifier.height(8.dp))
            }
            IconButton(onClick = { vm.setGridView(!grid) }) {
                Icon(
                    if (grid) Icons.Outlined.ViewList else Icons.Outlined.GridView,
                    contentDescription = if (grid) "List view" else "Grid view"
                )
            }
        }
        if (grid) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(commands, key = { it.cmd }) { cmd ->
                    CommandTile(cmd, vm.appliedFlags(cmd).isNotEmpty(), cmd.cmd in pinned, true) {
                        onOpenReview(cmd)
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(commands, key = { it.cmd }) { cmd ->
                    CommandTile(cmd, vm.appliedFlags(cmd).isNotEmpty(), cmd.cmd in pinned, false) {
                        onOpenReview(cmd)
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandTile(
    command: McCommand,
    hasDefaults: Boolean,
    pinned: Boolean,
    compact: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(command.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (pinned) Icon(Icons.Outlined.PushPin, contentDescription = "Pinned", tint = MaterialTheme.colorScheme.primary)
            }
            Text(command.cmd, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!compact) {
                Text(command.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (hasDefaults) {
                FilterChip(selected = true, onClick = onClick, label = { Text("Protocol defaults") })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommandReviewScreen(
    vm: MtkViewModel,
    command: McCommand,
    modifier: Modifier,
    onClose: () -> Unit
) {
    val values = remember {
        mutableStateMapOf<Int, String>().apply {
            command.args.forEachIndexed { i, arg -> put(i, arg.default) }
        }
    }
    var missing by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    val assembled = remember(values.toMap()) { buildLine(command, values) }
    val applied = assembled?.let { vm.appliedFlags(it) } ?: emptyList()
    val pinned by vm.pinned.collectAsState()
    val isPinned = command.cmd in pinned

    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(command.title) },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { vm.togglePin(command) }) {
                    Icon(
                        Icons.Outlined.PushPin,
                        contentDescription = if (isPinned) "Unpin" else "Pin",
                        tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(command.description, style = MaterialTheme.typography.bodyMedium)
            Text(command.cmd, style = MaterialTheme.typography.labelLarge, fontFamily = FontFamily.Monospace)
            command.args.forEachIndexed { i, arg ->
                OutlinedTextField(
                    value = values[i].orEmpty(),
                    onValueChange = { values[i] = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(arg.label + if (arg.optional) " (optional)" else "") },
                    placeholder = { if (arg.hint.isNotBlank()) Text(arg.hint) },
                    singleLine = true
                )
            }
            if (applied.isNotEmpty()) {
                Text("Protocol defaults that will be applied", style = MaterialTheme.typography.titleSmall)
                applied.forEach { Text("· ${it.label}", style = MaterialTheme.typography.bodySmall) }
            }
            assembled?.let {
                Text("Command", style = MaterialTheme.typography.titleSmall)
                Text(it, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
            }
            if (missing) {
                Text("Fill in every required field.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = {
                    val line = buildLine(command, values)
                    if (line == null) {
                        missing = true
                    } else if (CommandSupport.isDestructive(line)) {
                        confirm = true
                    } else {
                        vm.runCommand(line)
                        onClose()
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text("Run") }
        }
    }

    if (confirm) {
        val line = buildLine(command, values) ?: command.cmd
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Confirm destructive command") },
            text = { Text("\"$line\" can erase or overwrite data on the connected device and cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    vm.runCommand(line)
                    onClose()
                }) { Text("Run") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } }
        )
    }
}

private fun buildLine(command: McCommand, values: Map<Int, String>): String? {
    val tokens = mutableListOf(command.cmd)
    for ((i, arg) in command.args.withIndex()) {
        val value = values[i].orEmpty().trim()
        if (value.isEmpty()) {
            if (!arg.optional) return null
            continue
        }
        val finalValue = if (arg.pathUnderDumps && !value.contains("/")) "dumps/$value" else value
        val quoted = if (finalValue.contains(" ")) "\"$finalValue\"" else finalValue
        if (arg.flag != null) {
            tokens.add(arg.flag)
            tokens.add(quoted)
        } else {
            tokens.add(quoted)
        }
    }
    return tokens.joinToString(" ")
}
