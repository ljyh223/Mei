"""Export BeatNet's published model_1_weights.pt to a compact Android asset.

The upstream PyTorch checkpoint is a ZIP containing seven little-endian float
storages. This exporter reads those storages directly, so building the asset
does not require PyTorch or execute checkpoint pickle data.

Usage: python3 tools/export_beatnet_model.py /path/to/model_1_weights.pt
"""

from __future__ import annotations

import hashlib
import sys
import zipfile
from pathlib import Path

SOURCE_SHA256 = "619091bc317ca3e83b45591d46f6de3d5a41588bcb39fe9fe7be30cffa6aca84"
STORAGE_BYTES = (80, 8, 157200, 600, 1449600, 1800, 12)
OUTPUT = Path(__file__).resolve().parents[1] / "app/src/main/assets/beatnet/model_1_f32.bin"


def export(source: Path, destination: Path = OUTPUT) -> None:
    if hashlib.sha256(source.read_bytes()).hexdigest() != SOURCE_SHA256:
        raise ValueError("Unexpected BeatNet checkpoint; inspect its layout and license first")
    with zipfile.ZipFile(source) as archive:
        tensors = [archive.read(f"archive/data/{index}") for index in range(len(STORAGE_BYTES))]
    if tuple(map(len, tensors)) != STORAGE_BYTES:
        raise ValueError("Unexpected BeatNet tensor sizes")
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(b"".join(tensors))
    print(f"Wrote {destination} ({destination.stat().st_size} bytes)")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit(__doc__)
    export(Path(sys.argv[1]))
