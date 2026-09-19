package com.mtkclientandroid.engine

object CommandSupport {
    private val destructiveSubcommands = setOf("w", "wf", "wl", "wo", "e", "es", "ess")
    private val destructiveDaSubcommands = setOf("seccfg", "poke", "vbmeta", "patchmodem")
    private val destructiveDaRpmb = setOf("w", "e")
    private val transferSubcommands = setOf(
        "r", "rl", "rf", "rs", "ro", "w", "wf", "wl", "wo",
        "gpt", "footer", "dumppreloader", "dumpbrom", "dumpsram"
    )

    fun isDestructive(cmd: String): Boolean {
        val tokens = cmd.trim().split(Regex("\\s+"))
        if (tokens.isEmpty()) return false
        val first = tokens[0]
        if (first in destructiveSubcommands) return true
        if (first == "da") {
            val sub = tokens.getOrNull(1) ?: return false
            if (sub in destructiveDaSubcommands) return true
            if (sub == "rpmb" && tokens.getOrNull(2) in destructiveDaRpmb) return true
            if (sub == "imei" && tokens.any { it == "--write" }) return true
        }
        return false
    }

    fun isTransfer(cmd: String): Boolean {
        val tokens = cmd.trim().split(Regex("\\s+"))
        if (tokens.isEmpty()) return false
        if (tokens[0] in transferSubcommands) return true
        if (tokens[0] == "da" && tokens.getOrNull(1) in setOf("rpmb", "dumpbrom", "peek")) return true
        return false
    }

    val commonPartitions = listOf(
        "boot", "recovery", "vbmeta", "vbmeta_system", "vbmeta_vendor",
        "dtbo", "super", "system", "vendor", "userdata", "metadata",
        "cache", "frp", "nvram", "nvdata", "seccfg", "preloader", "lk",
        "md1img", "para", "logo", "misc"
    )
}

object ModeParser {
    fun inferMode(chunk: String): ConnectionMode? {
        val lower = chunk.lowercase()
        return when {
            "xflash" in lower || "legacy da" in lower || "xmlflash" in lower ||
                "da.handler" in lower || "upload da" in lower ||
                "download agent" in lower -> ConnectionMode.DA
            "bootrom" in lower || "boot rom" in lower || "brom mode" in lower ||
                "connected to brom" in lower -> ConnectionMode.BOOTROM
            "preloader" in lower || "preloader vcom" in lower -> ConnectionMode.PRELOADER
            else -> null
        }
    }

    fun inferChip(chunk: String): String? {
        val hw = Regex("(?i)\\b(?:HW|Chip(?:set)?)\\s*[:=]\\s*(MT\\w+)").find(chunk)
        if (hw != null) return hw.groupValues[1]
        val mt = Regex("\\b(MT[0-9]{3,5}\\w*)\\b").find(chunk)
        return mt?.groupValues?.get(1)
    }

    fun inferTargetConfig(chunk: String): String? {
        val lower = chunk.lowercase()
        if ("sbc:" in lower || "daa:" in lower || "sla:" in lower || "target config" in lower) {
            val line = chunk.lineSequence().firstOrNull {
                val l = it.lowercase()
                "sbc" in l || "daa" in l || "sla" in l || "target config" in l
            }
            return line?.trim()?.take(80)
        }
        return null
    }
}
