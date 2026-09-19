#!/usr/bin/env python3
# -*- coding: utf-8 -*-
#
# Called from Kotlin via Chaquopy. Runs mtk.py with sys.argv built from
# the command line the user submitted, so every 2.1.4.1 subcommand keeps
# working without a duplicated CLI.

import os
import re
import sys
import shlex
import importlib
import traceback

from com.mtkclientandroid import UiBridge

_ANSI_RE = re.compile(r"\x1b\[[0-9;]*[a-zA-Z]")


class CommandCancelled(Exception):
    """Raised when the user stops a running command from the UI."""


class _UiStream:
    """File-like object so print()/logging reach the Android terminal.

    ANSI is stripped (the Compose terminal is not a VT emulator). '\\r' is
    passed through so the terminal can redraw progress in place.
    """

    def write(self, s):
        if not s:
            return
        if UiBridge.isCancelled():
            raise CommandCancelled("Command cancelled")
        UiBridge.appendLog(_ANSI_RE.sub("", s))

    def flush(self):
        pass


def _friendly_error(exc: BaseException) -> str:
    text = str(exc) or exc.__class__.__name__
    lowered = text.lower()
    if isinstance(exc, CommandCancelled):
        return "Command cancelled."
    if "permission" in lowered and "usb" in lowered:
        return "USB permission was denied or revoked. Detect the device and grant access, then try again."
    if "no mtk usb device" in lowered or "no device" in lowered:
        return "No MediaTek device is attached. Connect the target over USB OTG and detect it first."
    if "couldn't open" in lowered or "opendevice" in lowered:
        return "The USB device could not be opened. Unplug it, detect the device again, and retry."
    if "detached" in lowered or "disconnect" in lowered or "connection reset" in lowered:
        return "The device disconnected while the command was running."
    if "unrecognized arguments" in lowered or "invalid choice" in lowered or "too few arguments" in lowered:
        return "That command is not valid. Check the arguments and try again."
    if "required" in lowered and ("argument" in lowered or "cmd" in lowered):
        return "That command is missing a required argument."
    if "cancelled" in lowered:
        return "Command cancelled."
    # Keep a short, readable last-resort line. Full traceback is stored
    # separately for "Copy error details".
    first = text.strip().splitlines()[0] if text.strip() else "The command failed."
    if len(first) > 240:
        first = first[:237] + "..."
    return first


def run_command(cmdline: str, workdir: str):
    UiBridge.clearCancel()
    os.environ["MTK_USE_ANDROID_USB"] = "1"
    os.environ.setdefault("QT_QPA_PLATFORM", "offscreen")

    sys.stdout = _UiStream()
    sys.stderr = _UiStream()

    try:
        os.makedirs(workdir, exist_ok=True)
        os.chdir(workdir)
    except Exception as e:
        UiBridge.appendLog("Could not open the working directory: %s\n" % e)
        return

    try:
        sys.argv = ["mtk.py"] + shlex.split(cmdline)
    except ValueError as e:
        UiBridge.appendLog("The command line could not be parsed: %s\n" % e)
        return

    try:
        if UiBridge.isCancelled():
            raise CommandCancelled("Command cancelled")
        sys.modules.pop("mtk", None)
        mtk_module = importlib.import_module("mtk")
        mtk_module.main()
    except CommandCancelled:
        UiBridge.appendLog("Command cancelled.\n")
    except SystemExit as e:
        code = e.code
        if UiBridge.isCancelled():
            UiBridge.appendLog("Command cancelled.\n")
        elif code not in (0, None):
            # argparse writes its own message to stderr already; add a
            # concise wrap-up rather than a stack trace.
            if code not in (1, 2):
                UiBridge.appendLog("The command exited with status %s.\n" % code)
    except Exception as exc:
        friendly = _friendly_error(exc)
        UiBridge.appendLog(friendly + "\n")
        UiBridge.setErrorDetail(traceback.format_exc())
