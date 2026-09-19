package com.mtkclientandroid.engine

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.mtkclientandroid.McCatalog
import com.mtkclientandroid.McCommand
import com.mtkclientandroid.UiBridge
import com.mtkclientandroid.UsbBridge
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class MtkViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    private val terminal = TerminalBuffer()
    private val logFilter = LogFilter()
    private val executor = Executors.newSingleThreadExecutor()
    private val running = AtomicBoolean(false)

    val dumpsDir: File = File(app.filesDir, "dumps").apply { mkdirs() }
    val loadersDir: File = File(app.filesDir, "loaders").apply { mkdirs() }

    private val _terminalText = MutableStateFlow("")
    val terminalText: StateFlow<String> = _terminalText

    private val _deviceState = MutableStateFlow<DeviceUiState>(DeviceUiState.Disconnected)
    val deviceState: StateFlow<DeviceUiState> = _deviceState

    private val _deviceInfo = MutableStateFlow(DeviceInfo())
    val deviceInfo: StateFlow<DeviceInfo> = _deviceInfo

    private val _protocol = MutableStateFlow(prefs.protocol().sanitized())
    val protocol: StateFlow<ProtocolSettings> = _protocol

    private val _appearance = MutableStateFlow(prefs.appearance())
    val appearance: StateFlow<AppearanceSettings> = _appearance

    private val _terminalSettings = MutableStateFlow(prefs.terminal())
    val terminalSettings: StateFlow<TerminalSettings> = _terminalSettings

    private val _history = MutableStateFlow(prefs.commandHistory())
    val history: StateFlow<List<String>> = _history

    private val _pinned = MutableStateFlow(prefs.pinned())
    val pinned: StateFlow<List<String>> = _pinned

    private val _lastError = MutableStateFlow<LastError?>(null)
    val lastError: StateFlow<LastError?> = _lastError

    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbar: StateFlow<String?> = _snackbar

    private val _dumpedFiles = MutableStateFlow<List<DumpedFile>>(emptyList())
    val dumpedFiles: StateFlow<List<DumpedFile>> = _dumpedFiles

    private val _gridView = MutableStateFlow(true)
    val gridView: StateFlow<Boolean> = _gridView

    val commandRunning: Boolean get() = running.get()

    var pendingDumpPartition: String? = null
    var pendingFlashPartition: String? = null
    var pendingExportFile: File? = null
    var pendingLoaderKind: LoaderKind? = null

    enum class LoaderKind { PRELOADER, DA }

    init {
        val ts = _terminalSettings.value
        logFilter.enabled = ts.suppressRepeats
        logFilter.windowMs = ts.filterWindowSeconds * 1000L
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(app))
        }
        UsbBridge.init(app)
        UiBridge.logListener = { msg -> appendLog(msg) }
        UiBridge.progressListener = { pct, label ->
            _deviceState.update { cur ->
                if (cur is DeviceUiState.Running) cur.copy(progress = pct, progressLabel = label) else cur
            }
        }
        UiBridge.errorDetailListener = { detail ->
            _lastError.update { prev ->
                LastError(friendly = prev?.friendly ?: "The command failed.", detail = detail)
            }
        }
        refreshDumpedFiles()
    }

    fun consumeSnackbar() {
        _snackbar.value = null
    }

    fun setGridView(value: Boolean) {
        _gridView.value = value
    }

    fun appendLog(msg: String) {
        val out = logFilter.filter(msg)
        if (out.isEmpty()) return
        _terminalText.value = terminal.append(out)
        ingestStatus(out)
    }

    fun clearTerminal() {
        terminal.clear()
        logFilter.reset()
        _terminalText.value = ""
        _lastError.value = null
    }

    private fun ingestStatus(chunk: String) {
        val lower = chunk.lowercase()
        ModeParser.inferChip(chunk)?.let { chip ->
            _deviceInfo.update { it.copy(chip = chip) }
        }
        ModeParser.inferTargetConfig(chunk)?.let { cfg ->
            _deviceInfo.update { it.copy(targetConfig = cfg) }
        }
        val mode = ModeParser.inferMode(chunk)
        if (running.get()) {
            if (mode != null) {
                _deviceState.update { cur ->
                    if (cur is DeviceUiState.Running) cur else cur
                }
            }
            return
        }
        if ("permission denied" in lower) {
            _deviceState.value = DeviceUiState.Error("USB permission denied")
            return
        }
        if ("permission granted" in lower || "already have permission" in lower) {
            val vid = UsbBridge.getVid().takeIf { it > 0 }
            val pid = UsbBridge.getPid().takeIf { it > 0 }
            if (vid != null) _deviceInfo.update { it.copy(vid = vid, pid = pid) }
            _deviceState.value = DeviceUiState.Connected(mode)
            return
        }
        if (UsbBridge.isConnected()) {
            _deviceInfo.update {
                it.copy(
                    vid = UsbBridge.getVid().takeIf { v -> v > 0 } ?: it.vid,
                    pid = UsbBridge.getPid().takeIf { v -> v > 0 } ?: it.pid
                )
            }
            _deviceState.update { cur ->
                if (cur is DeviceUiState.Connected) {
                    cur.copy(mode = mode ?: cur.mode)
                } else {
                    DeviceUiState.Connected(mode)
                }
            }
        }
    }

    fun onUsbAttached() {
        if (running.get()) return
        _deviceState.value = DeviceUiState.Detecting
        appendLog("USB device attached.\n")
    }

    fun onUsbDetached() {
        appendLog("USB device detached.\n")
        UsbBridge.close(false)
        _deviceInfo.value = DeviceInfo()
        _deviceState.value = if (running.get()) {
            DeviceUiState.Error("Connection lost")
        } else {
            DeviceUiState.Disconnected
        }
    }

    fun onPermissionResult(device: UsbDevice?, granted: Boolean) {
        val name = device?.deviceName ?: "device"
        if (granted) {
            appendLog("USB permission granted for $name\n")
            device?.let {
                _deviceInfo.update { info ->
                    info.copy(vid = it.vendorId, pid = it.productId)
                }
            }
            if (!running.get()) _deviceState.value = DeviceUiState.Connected(null)
        } else {
            appendLog("USB permission denied for $name\n")
            _deviceState.value = DeviceUiState.Error("USB permission denied")
            _lastError.value = LastError(
                friendly = "USB permission was denied. Detect the device and grant access, then try again.",
                detail = "UsbManager.EXTRA_PERMISSION_GRANTED=false device=$name"
            )
        }
    }

    fun detectDevice() {
        val manager = getApplication<Application>().getSystemService(Context.USB_SERVICE) as UsbManager
        val devices = manager.deviceList.values
        if (devices.isEmpty()) {
            appendLog("No USB devices found.\n")
            _deviceState.value = DeviceUiState.Disconnected
            return
        }
        _deviceState.value = DeviceUiState.Detecting
        for (device in devices) {
            appendLog(
                "Found device vid=0x%04x pid=0x%04x name=%s\n".format(
                    device.vendorId, device.productId, device.deviceName
                )
            )
            for (i in 0 until device.interfaceCount) {
                val itf = device.getInterface(i)
                var bulkIn = 0
                var bulkOut = 0
                for (e in 0 until itf.endpointCount) {
                    val ep = itf.getEndpoint(e)
                    if (ep.type == android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_BULK) {
                        if (ep.direction == android.hardware.usb.UsbConstants.USB_DIR_IN) bulkIn++
                        else bulkOut++
                    }
                }
                appendLog(
                    "  iface[$i] class=0x%02x bulkIN=$bulkIn bulkOUT=$bulkOut\n".format(itf.interfaceClass)
                )
            }
            if (manager.hasPermission(device)) {
                appendLog("Already have permission for ${device.deviceName}\n")
                _deviceInfo.update { it.copy(vid = device.vendorId, pid = device.productId) }
                _deviceState.value = DeviceUiState.Connected(null)
            } else {
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or (
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
                )
                val permissionIntent = android.content.Intent(ACTION_USB_PERMISSION).apply {
                    setPackage(getApplication<Application>().packageName)
                }
                val pi = PendingIntent.getBroadcast(getApplication(), 0, permissionIntent, flags)
                manager.requestPermission(device, pi)
            }
        }
    }

    fun submitTyped(raw: String) {
        val cmd = raw.trim()
        if (cmd.isEmpty()) return
        if (cmd == "clear") {
            clearTerminal()
            return
        }
        pushHistory(cmd)
        runCommand(cmd)
    }

    fun runCommand(cmd: String, onComplete: ((Boolean) -> Unit)? = null) {
        if (!running.compareAndSet(false, true)) {
            appendLog("A command is already running.\n")
            onComplete?.invoke(false)
            return
        }
        val (finalCmd, _) = ProtocolInjector.apply(cmd, _protocol.value)
        appendLog("$ $finalCmd\n")
        _deviceState.value = DeviceUiState.Running(finalCmd, null, "")
        UiBridge.clearCancel()
        executor.execute {
            var ok = true
            try {
                val py = Python.getInstance()
                val module = py.getModule("android_entry")
                module.callAttr("run_command", finalCmd, getApplication<Application>().filesDir.absolutePath)
            } catch (e: Exception) {
                ok = false
                val friendly = friendlyException(e)
                appendLog("$friendly\n")
                _lastError.postValueSafe(LastError(friendly, e.stackTraceToString()))
            } finally {
                running.set(false)
                val cancelled = UiBridge.isCancelled()
                val err = _deviceState.value is DeviceUiState.Error
                if (!err) {
                    _deviceState.value = if (UsbBridge.isConnected()) {
                        DeviceUiState.Connected(ModeParser.inferMode(_terminalText.value))
                    } else if (cancelled) {
                        DeviceUiState.Disconnected
                    } else {
                        val last = _lastError.value
                        if (!ok && last != null) DeviceUiState.Error(last.friendly)
                        else DeviceUiState.Connected(null)
                    }
                }
                refreshDumpedFiles()
                onComplete?.invoke(ok && !cancelled)
            }
        }
    }

    fun cancelCommand() {
        if (!running.get()) return
        UiBridge.requestCancel()
        UsbBridge.abort()
        appendLog("Stopping the running command…\n")
    }

    fun appliedFlags(cmd: String): List<AppliedFlag> =
        ProtocolInjector.previewApplied(cmd, _protocol.value)

    fun updateProtocol(s: ProtocolSettings) {
        _protocol.value = s
    }

    fun saveProtocol() {
        val cleaned = _protocol.value.sanitized()
        _protocol.value = cleaned
        prefs.saveProtocol(cleaned)
        _snackbar.value = "Protocol settings saved."
    }

    fun updateAppearance(s: AppearanceSettings) {
        _appearance.value = s
        prefs.saveAppearance(s)
    }

    fun updateTerminalSettings(s: TerminalSettings) {
        _terminalSettings.value = s
        logFilter.enabled = s.suppressRepeats
        logFilter.windowMs = s.filterWindowSeconds * 1000L
        logFilter.reset()
        prefs.saveTerminal(s)
    }

    fun togglePin(command: McCommand) {
        val cur = _pinned.value.toMutableList()
        if (command.cmd in cur) cur.remove(command.cmd) else cur.add(command.cmd)
        _pinned.value = cur
        prefs.savePinned(cur)
    }

    fun pinnedCommands(): List<McCommand> {
        val all = McCatalog.allCommands()
        return _pinned.value.mapNotNull { id -> all.find { it.cmd == id } }
    }

    fun copyPickedLoader(uri: Uri, kind: LoaderKind) {
        val stored = if (kind == LoaderKind.PRELOADER) "preloader.bin" else "loader.bin"
        val display = queryDisplayName(uri) ?: stored
        val dest = File(loadersDir, stored)
        try {
            getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { out -> input.copyTo(out) }
            } ?: run {
                appendLog("Could not open the selected file.\n")
                return
            }
        } catch (e: Exception) {
            appendLog("Could not read the selected file.\n")
            _lastError.value = LastError(
                "Could not read the selected file.",
                e.stackTraceToString()
            )
            return
        }
        _protocol.update { cur ->
            if (kind == LoaderKind.PRELOADER) {
                cur.copy(preloaderPath = dest.absolutePath, preloaderName = display, useCustomLoader = true)
            } else {
                cur.copy(loaderPath = dest.absolutePath, loaderName = display, useCustomLoader = true)
            }
        }
        prefs.saveProtocol(_protocol.value)
        appendLog("Using $display as ${if (kind == LoaderKind.PRELOADER) "preloader" else "DA"} for future commands.\n")
    }

    fun clearLoader(kind: LoaderKind) {
        if (kind == LoaderKind.PRELOADER) {
            File(loadersDir, "preloader.bin").delete()
            _protocol.update { it.copy(preloaderPath = null, preloaderName = null) }
        } else {
            File(loadersDir, "loader.bin").delete()
            _protocol.update { it.copy(loaderPath = null, loaderName = null) }
        }
        prefs.saveProtocol(_protocol.value)
    }

    fun beginDump(partition: String): String? {
        val name = partition.trim()
        if (name.isEmpty()) {
            appendLog("Enter a partition name first.\n")
            return null
        }
        pendingDumpPartition = name
        return "$name.img"
    }

    fun onDumpUri(uri: Uri?) {
        val partition = pendingDumpPartition
        pendingDumpPartition = null
        if (uri == null || partition == null) {
            appendLog("Dump cancelled.\n")
            return
        }
        val tempFile = File(dumpsDir, "$partition.img")
        runCommand("r $partition dumps/${tempFile.name}") { ok ->
            if (ok && tempFile.exists()) {
                try {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                        tempFile.inputStream().use { it.copyTo(out) }
                    }
                    appendLog("Saved ${tempFile.name} (${tempFile.length()} bytes).\n")
                } catch (e: Exception) {
                    appendLog("Export failed.\n")
                    _lastError.value = LastError("Could not export the dump.", e.stackTraceToString())
                }
            }
        }
    }

    fun beginFlash(partition: String): Boolean {
        val name = partition.trim()
        if (name.isEmpty()) {
            appendLog("Enter a partition name first.\n")
            return false
        }
        pendingFlashPartition = name
        return true
    }

    fun onFlashUri(uri: Uri?) {
        val partition = pendingFlashPartition
        pendingFlashPartition = null
        if (uri == null || partition == null) {
            appendLog("Flash cancelled.\n")
            return
        }
        val tempFile = File(dumpsDir, "${partition}_flash_input.img")
        try {
            getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { out -> input.copyTo(out) }
            } ?: run {
                appendLog("Could not open the selected file.\n")
                return
            }
        } catch (e: Exception) {
            appendLog("Could not read the selected file.\n")
            _lastError.value = LastError("Could not read the selected file.", e.stackTraceToString())
            return
        }
        runCommand("w $partition dumps/${tempFile.name}") {
            tempFile.delete()
        }
    }

    fun refreshDumpedFiles() {
        val files = dumpsDir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()
        _dumpedFiles.value = files.map {
            DumpedFile(it.name, it.absolutePath, it.length(), it.lastModified())
        }
    }

    fun renameDumped(file: DumpedFile, newName: String) {
        val dest = File(dumpsDir, newName)
        File(file.path).renameTo(dest)
        refreshDumpedFiles()
    }

    fun deleteDumped(file: DumpedFile) {
        File(file.path).delete()
        refreshDumpedFiles()
    }

    fun exportSettings(): String = prefs.exportJson()

    fun importSettings(json: String) {
        try {
            prefs.importJson(json)
            _protocol.value = prefs.protocol().sanitized()
            _appearance.value = prefs.appearance()
            _terminalSettings.value = prefs.terminal()
            _pinned.value = prefs.pinned()
            val ts = _terminalSettings.value
            logFilter.enabled = ts.suppressRepeats
            logFilter.windowMs = ts.filterWindowSeconds * 1000L
            _snackbar.value = "Settings imported."
        } catch (e: Exception) {
            _snackbar.value = "Could not import settings."
            _lastError.value = LastError("Could not import settings.", e.stackTraceToString())
        }
    }

    private fun pushHistory(cmd: String) {
        val next = listOf(cmd) + _history.value.filter { it != cmd }
        _history.value = next.take(50)
        prefs.saveCommandHistory(_history.value)
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            getApplication<Application>().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun friendlyException(e: Exception): String {
        val msg = e.message.orEmpty().lowercase()
        return when {
            "permission" in msg -> "USB permission was denied or revoked."
            "cancelled" in msg || "canceled" in msg -> "Command cancelled."
            "disconnected" in msg || "detach" in msg -> "The device disconnected while the command was running."
            "no mtk" in msg || "no device" in msg -> "No MediaTek device is attached."
            else -> e.message?.lineSequence()?.firstOrNull()?.take(240)
                ?: "The command failed."
        }
    }

    private fun MutableStateFlow<LastError?>.postValueSafe(value: LastError) {
        try {
            this.value = value
        } catch (_: Exception) {
            // Called from a worker thread; StateFlow is thread-safe.
            this.value = value
        }
    }

    private fun ProtocolSettings.sanitized(): ProtocolSettings {
        val pathOk = preloaderPath?.let { File(it).exists() } == true
        val loaderOk = loaderPath?.let { File(it).exists() } == true
        return copy(
            preloaderPath = preloaderPath.takeIf { pathOk },
            preloaderName = preloaderName.takeIf { pathOk },
            loaderPath = loaderPath.takeIf { loaderOk },
            loaderName = loaderName.takeIf { loaderOk }
        )
    }

    override fun onCleared() {
        UiBridge.logListener = null
        UiBridge.progressListener = null
        UiBridge.errorDetailListener = null
        UsbBridge.close(false)
        executor.shutdownNow()
        super.onCleared()
    }

    companion object {
        const val ACTION_USB_PERMISSION = "com.mtkclientandroid.USB_PERMISSION"
    }
}
