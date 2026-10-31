#!/usr/bin/env python3
"""Send standard Android media key events to a running Waydroid session.

Start Waydroid, open Mei and load a playback queue first. This bypasses host
keyboard shortcuts, so it tests Android/Mei rather than the physical Fn key path.
Run as your desktop user with --sudo when shell access needs root.
"""

import argparse
import math
import os
import shlex
import shutil
import subprocess
import sys
import time


# Android KeyEvent constants, not Linux input or XKB key codes.
KEYS = {
    "play-pause": 85,
    "next": 87,
    "previous": 88,
    "rewind": 89,
    "fast-forward": 90,
    "play": 126,
    "pause": 127,
    "stop": 86,
}


def count_value(value):
    count = int(value)
    if not 1 <= count <= 20:
        raise argparse.ArgumentTypeError("count must be between 1 and 20")
    return count


def interval_value(value):
    interval = float(value)
    if not math.isfinite(interval) or not 0.1 <= interval <= 30:
        raise argparse.ArgumentTypeError("interval must be between 0.1 and 30 seconds")
    return interval


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=KEYS, help="one media action to send")
    parser.add_argument("--sudo", action="store_true", help="use sudo only for waydroid shell")
    parser.add_argument("--dry-run", action="store_true", help="print commands without running anything")
    parser.add_argument("--count", type=count_value, default=1, help="independent key presses (1-20; default: 1)")
    parser.add_argument("--interval", type=interval_value, default=1.0, help="seconds between presses (default: 1)")
    args = parser.parse_args()

    waydroid = shutil.which("waydroid") or "waydroid"
    command = (["sudo"] if args.sudo and os.geteuid() != 0 else []) + [
        waydroid, "shell", "--", "input", "keyevent", str(KEYS[args.action]),
    ]
    if args.dry_run:
        for index in range(args.count):
            print(f"[{index + 1}/{args.count}] {shlex.join(command)}")
        if args.count > 1:
            print(f"Interval: {args.interval:g}s (independent presses, not a held key)")
        return 0

    if not shutil.which("waydroid"):
        print("waydroid was not found on PATH.", file=sys.stderr)
        return 2
    status = subprocess.run([waydroid, "status"], capture_output=True, text=True, timeout=15)
    running = any(
        line.partition(":")[0].strip() == "Session"
        and line.partition(":")[2].strip() == "RUNNING"
        for line in status.stdout.splitlines()
    )
    if status.returncode != 0 or not running:
        print("No running Waydroid session. Start Waydroid as your desktop user, open Mei and load a queue first.", file=sys.stderr)
        print("For example: waydroid show-full-ui", file=sys.stderr)
        return 2
    if os.geteuid() != 0 and not args.sudo:
        print("waydroid shell requires root. Run this script as your desktop user with --sudo.", file=sys.stderr)
        return 2
    if args.sudo and os.geteuid() != 0 and not shutil.which("sudo"):
        print("sudo was not found on PATH.", file=sys.stderr)
        return 2

    for index in range(args.count):
        print(f"[{index + 1}/{args.count}] {args.action}: Android keycode {KEYS[args.action]}", flush=True)
        # Let sudo prompt on the terminal. Never invoke a shell or change sudo policy.
        result = subprocess.run(command, timeout=60)
        if result.returncode != 0:
            print("Waydroid rejected the command; stopping the test.", file=sys.stderr)
            return 1
        if index + 1 < args.count:
            time.sleep(args.interval)
    print("Key event command completed. Check Mei for the expected playback change.")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print("\nTest interrupted.", file=sys.stderr)
        sys.exit(130)
    except (OSError, subprocess.TimeoutExpired) as error:
        print(f"Unable to run the test: {error}", file=sys.stderr)
        sys.exit(1)
