package com.mtkclientandroid.engine

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class Prefs(context: Context) {
    private val p: SharedPreferences =
        context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun protocol(): ProtocolSettings = ProtocolSettings(
        debugMode = p.getBoolean("proto_debug", false),
        skipWatchdog = p.getBoolean("proto_skipwdt", false),
        alreadyInPreloader = p.getBoolean("proto_already_pl", false),
        readSocId = p.getBoolean("proto_socid", false),
        useCustomLoader = p.getBoolean("proto_use_loader", false),
        payloadType = p.getString("proto_ptype", "") ?: "",
        vid = p.getString("proto_vid", "") ?: "",
        pid = p.getString("proto_pid", "") ?: "",
        sectorSize = p.getString("proto_sectorsize", "0x200") ?: "0x200",
        preloaderPath = p.getString("preloader_path", null),
        preloaderName = p.getString("preloader_name", null),
        loaderPath = p.getString("loader_path", null),
        loaderName = p.getString("loader_name", null)
    )

    fun saveProtocol(s: ProtocolSettings) {
        p.edit()
            .putBoolean("proto_debug", s.debugMode)
            .putBoolean("proto_skipwdt", s.skipWatchdog)
            .putBoolean("proto_already_pl", s.alreadyInPreloader)
            .putBoolean("proto_socid", s.readSocId)
            .putBoolean("proto_use_loader", s.useCustomLoader)
            .putString("proto_ptype", s.payloadType)
            .putString("proto_vid", s.vid)
            .putString("proto_pid", s.pid)
            .putString("proto_sectorsize", s.sectorSize)
            .putString("preloader_path", s.preloaderPath)
            .putString("preloader_name", s.preloaderName)
            .putString("loader_path", s.loaderPath)
            .putString("loader_name", s.loaderName)
            .apply()
    }

    fun appearance(): AppearanceSettings = AppearanceSettings(
        themeMode = runCatching {
            ThemeMode.valueOf(p.getString("theme_mode", ThemeMode.SYSTEM.name)!!)
        }.getOrDefault(ThemeMode.SYSTEM),
        colorPreset = runCatching {
            ColorPreset.valueOf(p.getString("color_preset", ColorPreset.MATERIAL_YOU.name)!!)
        }.getOrDefault(ColorPreset.MATERIAL_YOU)
    )

    fun saveAppearance(s: AppearanceSettings) {
        p.edit()
            .putString("theme_mode", s.themeMode.name)
            .putString("color_preset", s.colorPreset.name)
            .apply()
    }

    fun terminal(): TerminalSettings = TerminalSettings(
        suppressRepeats = p.getBoolean("dedup_enabled", true),
        filterWindowSeconds = p.getLong("dedup_window_ms", 5_000L) / 1000L
    )

    fun saveTerminal(s: TerminalSettings) {
        p.edit()
            .putBoolean("dedup_enabled", s.suppressRepeats)
            .putLong("dedup_window_ms", s.filterWindowSeconds.coerceAtLeast(0L) * 1000L)
            .apply()
    }

    fun commandHistory(): List<String> {
        val raw = p.getString("cmd_history", "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) add(arr.getString(i))
            }
        }.getOrDefault(emptyList())
    }

    fun saveCommandHistory(items: List<String>) {
        val arr = JSONArray()
        items.take(50).forEach { arr.put(it) }
        p.edit().putString("cmd_history", arr.toString()).apply()
    }

    fun pinned(): List<String> {
        val raw = p.getString("pinned_cmds", null)
        if (raw.isNullOrBlank()) return DEFAULT_PINNED_COMMANDS
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) add(arr.getString(i))
            }
        }.getOrDefault(DEFAULT_PINNED_COMMANDS)
    }

    fun savePinned(items: List<String>) {
        val arr = JSONArray()
        items.forEach { arr.put(it) }
        p.edit().putString("pinned_cmds", arr.toString()).apply()
    }

    fun exportJson(): String {
        val o = JSONObject()
        o.put("version", 1)
        o.put("protocol", JSONObject().apply {
            val s = protocol()
            put("debugMode", s.debugMode)
            put("skipWatchdog", s.skipWatchdog)
            put("alreadyInPreloader", s.alreadyInPreloader)
            put("readSocId", s.readSocId)
            put("useCustomLoader", s.useCustomLoader)
            put("payloadType", s.payloadType)
            put("vid", s.vid)
            put("pid", s.pid)
            put("sectorSize", s.sectorSize)
        })
        o.put("appearance", JSONObject().apply {
            val s = appearance()
            put("themeMode", s.themeMode.name)
            put("colorPreset", s.colorPreset.name)
        })
        o.put("terminal", JSONObject().apply {
            val s = terminal()
            put("suppressRepeats", s.suppressRepeats)
            put("filterWindowSeconds", s.filterWindowSeconds)
        })
        o.put("pinned", JSONArray(pinned()))
        return o.toString(2)
    }

    fun importJson(json: String) {
        val o = JSONObject(json)
        o.optJSONObject("protocol")?.let { proto ->
            val cur = protocol()
            saveProtocol(
                cur.copy(
                    debugMode = proto.optBoolean("debugMode", cur.debugMode),
                    skipWatchdog = proto.optBoolean("skipWatchdog", cur.skipWatchdog),
                    alreadyInPreloader = proto.optBoolean("alreadyInPreloader", cur.alreadyInPreloader),
                    readSocId = proto.optBoolean("readSocId", cur.readSocId),
                    useCustomLoader = proto.optBoolean("useCustomLoader", cur.useCustomLoader),
                    payloadType = proto.optString("payloadType", cur.payloadType),
                    vid = proto.optString("vid", cur.vid),
                    pid = proto.optString("pid", cur.pid),
                    sectorSize = proto.optString("sectorSize", cur.sectorSize)
                )
            )
        }
        o.optJSONObject("appearance")?.let { a ->
            saveAppearance(
                AppearanceSettings(
                    themeMode = runCatching {
                        ThemeMode.valueOf(a.optString("themeMode", ThemeMode.SYSTEM.name))
                    }.getOrDefault(ThemeMode.SYSTEM),
                    colorPreset = runCatching {
                        ColorPreset.valueOf(a.optString("colorPreset", ColorPreset.MATERIAL_YOU.name))
                    }.getOrDefault(ColorPreset.MATERIAL_YOU)
                )
            )
        }
        o.optJSONObject("terminal")?.let { t ->
            saveTerminal(
                TerminalSettings(
                    suppressRepeats = t.optBoolean("suppressRepeats", true),
                    filterWindowSeconds = t.optLong("filterWindowSeconds", 5L)
                )
            )
        }
        o.optJSONArray("pinned")?.let { arr ->
            val items = buildList {
                for (i in 0 until arr.length()) add(arr.getString(i))
            }
            savePinned(items)
        }
    }
}
