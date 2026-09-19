package com.mtkclientandroid.engine

import com.mtkclientandroid.McCommand

enum class ThemeMode { SYSTEM, LIGHT, DARK, OLED }

enum class ColorPreset { MATERIAL_YOU, STOCK_ANDROID, TEAL, GRAPHITE, FOREST }

enum class CommandCategory(val id: String, val titleRes: String) {
    DA_BYPASS("da_bypass", "DA Bypass"),
    PARTITION_IO("partition_io", "Partition I/O"),
    ERASE("erase", "Erase"),
    DEVICE("device", "Device control"),
    ADVANCED("advanced", "Advanced")
}

enum class ConnectionMode { PRELOADER, BOOTROM, DA }

sealed class DeviceUiState {
    data object Disconnected : DeviceUiState()
    data object Detecting : DeviceUiState()
    data class Connected(val mode: ConnectionMode?) : DeviceUiState()
    data class Running(val command: String, val progress: Int?, val progressLabel: String) : DeviceUiState()
    data class Error(val message: String) : DeviceUiState()
}

data class DeviceInfo(
    val vid: Int? = null,
    val pid: Int? = null,
    val chip: String? = null,
    val targetConfig: String? = null
)

data class ProtocolSettings(
    val debugMode: Boolean = false,
    val skipWatchdog: Boolean = false,
    val alreadyInPreloader: Boolean = false,
    val readSocId: Boolean = false,
    val useCustomLoader: Boolean = false,
    val payloadType: String = "",
    val vid: String = "",
    val pid: String = "",
    val sectorSize: String = "0x200",
    val preloaderPath: String? = null,
    val preloaderName: String? = null,
    val loaderPath: String? = null,
    val loaderName: String? = null
)

data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorPreset: ColorPreset = ColorPreset.MATERIAL_YOU
)

data class TerminalSettings(
    val suppressRepeats: Boolean = true,
    val filterWindowSeconds: Long = 5
)

data class AppliedFlag(val flag: String, val label: String)

data class DumpedFile(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val modifiedMs: Long
)

data class LastError(
    val friendly: String,
    val detail: String
)

val DEFAULT_PINNED_COMMANDS = listOf(
    "printgpt",
    "gettargetconfig",
    "dumppreloader",
    "dumpbrom",
    "crash"
)

fun McCommand.categoryId(): CommandCategory {
    return when (cmd) {
        "crash", "payload", "brute", "stage", "plstage",
        "da efuse", "da generatekeys", "da keyserver", "da dumpbrom",
        "da seccfg", "da vbmeta", "da poke", "da peek",
        "da rpmb r", "da rpmb w", "da rpmb e", "da imei" -> CommandCategory.DA_BYPASS
        "gpt", "r", "rl", "rf", "rs", "ro", "footer",
        "w", "wl", "wf", "wo" -> CommandCategory.PARTITION_IO
        "e", "es", "ess" -> CommandCategory.ERASE
        "reset", "meta", "meta2", "printgpt", "gettargetconfig", "devices" -> CommandCategory.DEVICE
        else -> CommandCategory.ADVANCED
    }
}
