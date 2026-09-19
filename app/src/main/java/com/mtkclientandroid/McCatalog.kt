package com.mtkclientandroid

import com.mtkclientandroid.engine.CommandCategory

data class McArg(
    val label: String,
    val hint: String = "",
    val default: String = "",
    val optional: Boolean = false,
    val flag: String? = null,
    val pathUnderDumps: Boolean = false
)

data class McCommand(
    val cmd: String,
    val title: String,
    val description: String,
    val args: List<McArg> = emptyList()
)

data class McGroup(val category: CommandCategory, val commands: List<McCommand>)

object McCatalog {

    private val partitionsArg = McArg(
        "Partition name(s)", "userdata  (comma-separated, e.g. boot,recovery)"
    )

    val groups: List<McGroup> = listOf(

        McGroup(CommandCategory.DA_BYPASS, listOf(
            McCommand(
                "crash", "Crash Preloader to BootROM",
                "Forces a device in Preloader back into BootROM so a payload can run."
            ),
            McCommand(
                "payload", "Run bypass payload",
                "Disables serial-link and download-agent authorization so further commands can talk to a locked device.",
                args = listOf(
                    McArg("Exploit method", "kamakiri / kamakiri2 / carbonara / amonet",
                        optional = true, flag = "--ptype"),
                    McArg("var1 override (hex)", "0xa", optional = true, flag = "--var1"),
                    McArg("Watchdog address (hex)", "0x10007000", optional = true, flag = "--wdt"),
                    McArg("Crash mode", "0, 1 or 2", optional = true, flag = "--mode")
                )
            ),
            McCommand("brute", "Bruteforce kamakiri var1",
                "Sweeps var1 values automatically when the default does not work."),
            McCommand("stage", "Run stage2 payload (BootROM)",
                "Loads and jumps to a stage2 payload via BootROM."),
            McCommand("plstage", "Run stage2 payload (Preloader)",
                "Loads and jumps to a stage2 payload via Preloader send_da."),
            McCommand("da efuse", "Read efuses", "Reads eFuse contents through the Download Agent."),
            McCommand("da generatekeys", "Generate hardware keys", "Derives this device's hardware keys via the DA."),
            McCommand("da keyserver", "Enable key server", "Starts the DA key server."),
            McCommand("da dumpbrom", "Dump BootROM (via DA)", "Dumps BootROM through the Download Agent path."),
            McCommand(
                "da seccfg", "Unlock or lock bootloader",
                "Sets the seccfg unlock flag. A confirmation is shown before this runs.",
                args = listOf(McArg("unlock or lock", "unlock"))
            ),
            McCommand(
                "da vbmeta", "Patch vbmeta",
                "Patches the vbmeta partition verification mode.",
                args = listOf(McArg("Mode", "0=locked 1=disable_verity 2=disable_verification 3=both"))
            ),
            McCommand(
                "da poke", "Write memory (via DA)",
                "Writes raw data to a memory address through the DA.",
                args = listOf(McArg("Address (hex)", "0x..."), McArg("Data", ""))
            ),
            McCommand(
                "da peek", "Read memory (via DA)",
                "Reads raw memory through the Download Agent.",
                args = listOf(
                    McArg("Address (hex)", "0x..."),
                    McArg("Length", "0x100"),
                    McArg("Filename (optional)", "peek.bin", optional = true, flag = "--filename",
                        pathUnderDumps = true)
                )
            ),
            McCommand(
                "da rpmb r", "Read RPMB",
                "Reads the Replay Protected Memory Block.",
                args = listOf(McArg("Output filename", "rpmb.bin", default = "rpmb.bin", pathUnderDumps = true))
            ),
            McCommand(
                "da rpmb w", "Write RPMB",
                "Writes the RPMB partition from a file.",
                args = listOf(McArg("Input filename", "rpmb.bin", default = "rpmb.bin", pathUnderDumps = true))
            ),
            McCommand("da rpmb e", "Erase RPMB", "Erases the RPMB partition."),
            McCommand(
                "da imei", "Read or write IMEI",
                "Reads IMEI data via the DA. Supply a value to write.",
                args = listOf(McArg("IMEIs (optional, comma-separated)", "", optional = true))
            )
        )),

        McGroup(CommandCategory.PARTITION_IO, listOf(
            McCommand(
                "gpt", "Save GPT table",
                "Writes the raw partition table to a folder.",
                args = listOf(McArg("Folder name", "gpt", default = "gpt", pathUnderDumps = true))
            ),
            McCommand(
                "r", "Read partition(s)",
                "Dumps one or more named partitions to files.",
                args = listOf(
                    McArg("Partition name(s)", "userdata  (comma-separated)"),
                    McArg("Output filename(s)", "userdata.img  (comma-separated, matching partitions)",
                        pathUnderDumps = true)
                )
            ),
            McCommand(
                "rl", "Read all partitions",
                "Dumps every reachable partition into one folder.",
                args = listOf(McArg("Folder name", "backup", default = "backup", pathUnderDumps = true))
            ),
            McCommand(
                "rf", "Read whole flash",
                "Dumps the entire raw flash to a single file.",
                args = listOf(McArg("Output filename", "full_flash.bin", pathUnderDumps = true))
            ),
            McCommand(
                "rs", "Read sectors",
                "Reads a raw sector range to a file.",
                args = listOf(
                    McArg("Start sector", "0"),
                    McArg("Sector count", "2048"),
                    McArg("Output filename", "sectors.bin", pathUnderDumps = true)
                )
            ),
            McCommand(
                "ro", "Read byte range",
                "Reads a raw byte offset and length to a file.",
                args = listOf(
                    McArg("Byte offset", "0x0"),
                    McArg("Length", "0x100000"),
                    McArg("Output filename", "range.bin", pathUnderDumps = true)
                )
            ),
            McCommand(
                "footer", "Read crypto footer",
                "Reads userdata encryption metadata to a file.",
                args = listOf(McArg("Output filename", "footer.bin", pathUnderDumps = true))
            ),
            McCommand(
                "w", "Write partition(s)",
                "Overwrites named partitions from local files. Confirmation is required.",
                args = listOf(
                    partitionsArg,
                    McArg("Input filename(s)", "userdata.img  (comma-separated, matching partitions)",
                        pathUnderDumps = true)
                )
            ),
            McCommand(
                "wl", "Write folder to flash",
                "Writes every image in a folder to its matching partition.",
                args = listOf(McArg("Folder name", "restore", pathUnderDumps = true))
            ),
            McCommand(
                "wf", "Write whole flash",
                "Overwrites the entire raw flash from one file.",
                args = listOf(McArg("Input filename", "full_flash.bin", pathUnderDumps = true))
            ),
            McCommand(
                "wo", "Write byte range",
                "Writes a file to a raw byte offset and length.",
                args = listOf(
                    McArg("Byte offset", "0x0"),
                    McArg("Length", "0x100000"),
                    McArg("Input filename", "range.bin", pathUnderDumps = true)
                )
            )
        )),

        McGroup(CommandCategory.ERASE, listOf(
            McCommand("e", "Erase partition", "Erases a named partition.",
                args = listOf(partitionsArg)),
            McCommand(
                "es", "Erase partition (sector count)",
                "Erases a partition for a given sector count.",
                args = listOf(partitionsArg, McArg("Sectors", "2048"))
            ),
            McCommand(
                "ess", "Erase sectors",
                "Erases a raw sector range.",
                args = listOf(McArg("Start sector", "0"), McArg("Sector count", "2048"))
            )
        )),

        McGroup(CommandCategory.DEVICE, listOf(
            McCommand("reset", "Reset device", "Sends a reset command to the target."),
            McCommand(
                "meta", "Enter meta mode",
                "Switches the device into meta mode.",
                args = listOf(McArg("Meta mode (optional)", "off / usb / uart", optional = true))
            ),
            McCommand("meta2", "Enter meta mode (watchdog)", "Switches into meta mode via a watchdog reset."),
            McCommand(
                "printgpt", "Print partition table",
                "Lists every partition visible on the device."
            ),
            McCommand(
                "gettargetconfig", "Get target config",
                "Shows secure boot, serial-link auth, and download-agent auth state."
            ),
            McCommand("devices", "List supported devices", "Prints the built-in chipset database.")
        )),

        McGroup(CommandCategory.ADVANCED, listOf(
            McCommand(
                "peek", "Read memory (patched preloader)",
                "Reads memory directly once the preloader has been patched.",
                args = listOf(
                    McArg("Address", "0x..."),
                    McArg("Length", "0x100"),
                    McArg("Filename (optional)", "peek.bin", optional = true, flag = "--filename",
                        pathUnderDumps = true)
                )
            ),
            McCommand(
                "script", "Run a text script",
                "Runs a newline-separated list of commands from a file.",
                args = listOf(McArg("Script filename", "commands.txt", pathUnderDumps = true))
            ),
            McCommand(
                "multi", "Run multiple commands",
                "Runs several commands back to back, separated by semicolons.",
                args = listOf(McArg("Commands", "printgpt; gettargetconfig"))
            ),
            McCommand(
                "dumppreloader", "Dump preloader",
                "Saves the device Preloader image to a file.",
                args = listOf(McArg("Filename (optional)", "preloader.bin", optional = true,
                    flag = "--filename", pathUnderDumps = true))
            ),
            McCommand(
                "dumpbrom", "Dump BootROM",
                "Attempts to dump the on-chip BootROM (chip-dependent).",
                args = listOf(McArg("Filename (optional)", "bootrom.bin", optional = true,
                    flag = "--filename", pathUnderDumps = true))
            ),
            McCommand(
                "dumpsram", "Dump SRAM",
                "Attempts to dump on-chip SRAM contents.",
                args = listOf(McArg("Filename (optional)", "sram.bin", optional = true,
                    flag = "--filename", pathUnderDumps = true))
            ),
            McCommand("logs", "Get target logs", "Reads the diagnostic log buffer exposed by the target.")
        ))
    )

    fun allCommands(): List<McCommand> = groups.flatMap { it.commands }

    fun find(cmd: String): McCommand? = allCommands().find { it.cmd == cmd }
}
