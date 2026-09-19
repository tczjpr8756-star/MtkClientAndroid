package com.mtkclientandroid.engine

/**
 * Appends saved protocol defaults onto a command line. A flag the user
 * already typed always wins. Commands whose argparse parent does not
 * accept the base flags are left untouched.
 */
object ProtocolInjector {

    private val loaderCapable = setOf(
        "r", "rl", "rf", "rs", "ro", "w", "wf", "wl", "wo",
        "e", "es", "ess", "footer",
        "printgpt", "gpt",
        "dumpbrom", "dumpsram", "dumppreloader",
        "payload", "brute", "stage", "plstage", "peek",
        "da", "crash", "reset", "meta", "meta2", "logs",
        "gettargetconfig"
    )

    private val exploitCommands = setOf("payload", "brute", "stage")

    fun firstToken(cmd: String): String = cmd.trim().substringBefore(' ')

    fun isCapable(cmd: String): Boolean = firstToken(cmd) in loaderCapable

    fun apply(cmd: String, settings: ProtocolSettings): Pair<String, List<AppliedFlag>> {
        if (!isCapable(cmd)) return cmd to emptyList()
        val lower = cmd.lowercase()
        val applied = mutableListOf<AppliedFlag>()
        val extra = StringBuilder()

        fun missing(flag: String) = !lower.contains(flag)

        if (settings.debugMode && missing("--debugmode")) {
            extra.append(" --debugmode")
            applied += AppliedFlag("--debugmode", "Verbose logging")
        }
        if (settings.skipWatchdog && missing("--skipwdt")) {
            extra.append(" --skipwdt")
            applied += AppliedFlag("--skipwdt", "Skip watchdog init")
        }
        if (settings.readSocId && missing("--socid")) {
            extra.append(" --socid")
            applied += AppliedFlag("--socid", "Read SoC ID")
        }
        if (settings.payloadType.isNotBlank() && missing("--ptype")) {
            extra.append(" --ptype ").append(settings.payloadType)
            applied += AppliedFlag("--ptype", "Payload: ${settings.payloadType}")
        }
        if (settings.vid.isNotBlank() && missing("--vid")) {
            extra.append(" --vid ").append(quote(settings.vid))
            applied += AppliedFlag("--vid", "VID ${settings.vid}")
        }
        if (settings.pid.isNotBlank() && missing("--pid")) {
            extra.append(" --pid ").append(quote(settings.pid))
            applied += AppliedFlag("--pid", "PID ${settings.pid}")
        }
        if (settings.sectorSize.isNotBlank() &&
            settings.sectorSize != "0x200" &&
            missing("--sectorsize")
        ) {
            extra.append(" --sectorsize ").append(settings.sectorSize)
            applied += AppliedFlag("--sectorsize", "Sector size ${settings.sectorSize}")
        }
        if (settings.useCustomLoader) {
            settings.preloaderPath?.let { path ->
                if (missing("--preloader")) {
                    extra.append(" --preloader ").append(quote(path))
                    applied += AppliedFlag("--preloader", "Custom preloader")
                }
            }
            settings.loaderPath?.let { path ->
                if (missing("--loader")) {
                    extra.append(" --loader ").append(quote(path))
                    applied += AppliedFlag("--loader", "Custom DA")
                }
            }
        }
        val token = firstToken(cmd)
        if (!settings.alreadyInPreloader &&
            token in exploitCommands &&
            missing("--crash")
        ) {
            extra.append(" --crash")
            applied += AppliedFlag("--crash", "Crash Preloader to BootROM")
        }
        if (settings.alreadyInPreloader && token in exploitCommands) {
            applied += AppliedFlag("skip-crash", "Already in Preloader")
        }
        return (cmd + extra.toString()) to applied
    }

    fun previewApplied(cmd: String, settings: ProtocolSettings): List<AppliedFlag> =
        apply(cmd, settings).second

    private fun quote(value: String): String =
        if (value.any { it.isWhitespace() }) "\"$value\"" else value
}
