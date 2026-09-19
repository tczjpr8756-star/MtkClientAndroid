package com.mtkclientandroid.ui.files

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.mtkclientandroid.engine.DumpedFile
import com.mtkclientandroid.engine.MtkViewModel
import java.io.File
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DumpedFilesScreen(vm: MtkViewModel, modifier: Modifier = Modifier, onBack: () -> Unit) {
    val files by vm.dumpedFiles.collectAsState()
    val context = LocalContext.current
    var selected by remember { mutableStateOf<DumpedFile?>(null) }
    var renaming by remember { mutableStateOf<DumpedFile?>(null) }
    var newName by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<DumpedFile?>(null) }

    LaunchedEffect(Unit) { vm.refreshDumpedFiles() }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val file = vm.pendingExportFile
        vm.pendingExportFile = null
        if (uri == null || file == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            }
        } catch (_: Exception) { }
    }

    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Dumped files") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
            }
        )
        if (files.isEmpty()) {
            Text(
                "No dumped files yet. Read commands write here.",
                modifier = Modifier.padding(24.dp)
            )
        } else {
            val df = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
            LazyColumn {
                items(files, key = { it.path }) { file ->
                    ListItem(
                        headlineContent = { Text(file.name) },
                        supportingContent = {
                            Text("${formatSize(file.sizeBytes)}  ·  ${df.format(Date(file.modifiedMs))}")
                        },
                        modifier = Modifier.clickable { selected = file }
                    )
                }
            }
        }
    }

    selected?.let { file ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(file.name) },
            text = { Text("Share, export, rename, or delete this file.") },
            confirmButton = {},
            dismissButton = {
                Column(Modifier.fillMaxWidth()) {
                    TextButton(onClick = {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            File(file.path)
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/octet-stream"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share ${file.name}"))
                        selected = null
                    }) { Text("Share") }
                    TextButton(onClick = {
                        vm.pendingExportFile = File(file.path)
                        exportLauncher.launch(file.name)
                        selected = null
                    }) { Text("Export") }
                    TextButton(onClick = {
                        renaming = file
                        newName = file.name
                        selected = null
                    }) { Text("Rename") }
                    TextButton(onClick = {
                        deleting = file
                        selected = null
                    }) { Text("Delete") }
                    TextButton(onClick = { selected = null }) { Text("Close") }
                }
            }
        )
    }

    renaming?.let { file ->
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(value = newName, onValueChange = { newName = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) vm.renameDumped(file, newName.trim())
                    renaming = null
                }) { Text("Rename") }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Cancel") } }
        )
    }

    deleting?.let { file ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete ${file.name}?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteDumped(file)
                    deleting = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
        )
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB")
    var value = bytes / 1024.0
    var i = 0
    while (value >= 1024 && i < units.size - 1) {
        value /= 1024.0
        i++
    }
    return String.format("%.1f %s", value, units[i])
}
