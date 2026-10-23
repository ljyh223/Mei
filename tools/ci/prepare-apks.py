#!/usr/bin/env python3
"""Verify the two signed ABI APKs and prepare named files for CI artifact or Release upload."""

import argparse
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
from zipfile import ZipFile


EXPECTED_ABIS = {"arm64-v8a", "x86_64"}


def prepare_apks(source: Path, output: Path, name: str, checksums: bool) -> None:
    if not name or name in {".", ".."} or any(c in name for c in "/\\\r\n"):
        raise SystemExit("APK name must be a non-empty filename prefix.")
    signed_apks = sorted(source.glob("*-signed.apk"))
    if len(signed_apks) != len(EXPECTED_ABIS):
        raise SystemExit(f"Expected exactly two signed APKs, found {len(signed_apks)}.")

    apksigner = Path(os.environ["ANDROID_HOME"]) / "build-tools/34.0.0/apksigner"
    seen = set()
    verified = []
    for apk in signed_apks:
        subprocess.run([str(apksigner), "verify", str(apk)], check=True)
        with ZipFile(apk) as archive:
            abis = {
                parts[1]
                for entry in archive.namelist()
                if len(parts := entry.split("/")) == 3
                and parts[0] == "lib" and entry.endswith(".so")
            }
        if len(abis) != 1 or not abis <= EXPECTED_ABIS or abis & seen:
            raise SystemExit(f"Unexpected or duplicate native ABIs in {apk}: {abis}")
        abi = abis.pop()
        seen.add(abi)
        verified.append((abi, apk))
    if seen != EXPECTED_ABIS:
        raise SystemExit(f"Missing APKs: {EXPECTED_ABIS - seen}")

    # Validate all inputs before producing any uploadable output.
    output.mkdir(parents=True, exist_ok=True)
    for abi, apk in verified:
        target = output / f"{name}_{abi}.apk"
        shutil.copyfile(apk, target)
        if checksums:
            digest = hashlib.sha256(target.read_bytes()).hexdigest()
            target.with_suffix(".apk.sha256").write_text(f"{digest}  {target.name}\n")
        print(f"Prepared {target.name} ({target.stat().st_size} bytes)")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=Path("app/build/outputs/apk/release"))
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--name", required=True)
    parser.add_argument("--checksums", action="store_true")
    args = parser.parse_args()
    prepare_apks(args.source, args.output, args.name, args.checksums)


if __name__ == "__main__":
    main()
