"""Compare downloaded audio duration with the last timed TTML lyric line."""

import argparse
import json
from pathlib import Path
import shutil
import subprocess
import xml.etree.ElementTree as ET


RESEARCH_DIR = Path(__file__).resolve().parents[1]
DEFAULT_MANIFEST = RESEARCH_DIR / "data/candidates/downloads.jsonl"
DEFAULT_OUTPUT = RESEARCH_DIR / "data/candidates/audio_coverage.jsonl"
DEFAULT_LYRICS = Path("/home/ljyh/code/temp/amll-ttml-db-main/ncm-lyrics")


def ttml_end_seconds(path: Path) -> float:
    root = ET.parse(path).getroot()
    ends = []
    for element in root.iter():
        if not element.tag.endswith("}p"):
            continue
        value = element.get("end")
        if not value:
            continue
        parts = value.split(":")
        ends.append(sum(float(part) * 60 ** index for index, part in enumerate(reversed(parts))))
    if not ends:
        raise ValueError("no timed lyric lines")
    return max(ends)


def audio_duration_seconds(path: Path, ffprobe: str) -> float:
    output = subprocess.check_output(
        [ffprobe, "-v", "error", "-show_entries", "format=duration", "-of", "default=noprint_wrappers=1:nokey=1", str(path)],
        text=True,
    ).strip()
    return float(output)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, default=DEFAULT_MANIFEST)
    parser.add_argument("--lyrics-dir", type=Path, default=DEFAULT_LYRICS)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--minimum-coverage", type=float, default=0.90)
    args = parser.parse_args()
    ffprobe = shutil.which("ffprobe")
    if not ffprobe:
        parser.error("ffprobe is required; install FFmpeg and ensure ffprobe is on PATH")
    if not 0 < args.minimum_coverage <= 1:
        parser.error("--minimum-coverage must be in (0, 1]")

    records = []
    for line in args.manifest.read_text(encoding="utf-8").splitlines():
        item = json.loads(line)
        record = {"id": str(item["id"]), "title": item.get("title"), "file": item.get("file")}
        audio_path = Path(item["file"]) if item.get("file") else None
        ttml_path = args.lyrics_dir / f"{item['id']}.ttml"
        if not audio_path or not audio_path.is_file():
            record["status"] = "missing_audio"
        elif not ttml_path.is_file():
            record["status"] = "missing_ttml"
        else:
            try:
                audio_seconds = audio_duration_seconds(audio_path, ffprobe)
                lyric_seconds = ttml_end_seconds(ttml_path)
                ratio = audio_seconds / lyric_seconds
                record.update({
                    "audio_seconds": round(audio_seconds, 3),
                    "lyric_end_seconds": round(lyric_seconds, 3),
                    "coverage": round(ratio, 4),
                    "status": "complete" if ratio >= args.minimum_coverage else "preview_or_mismatch",
                })
            except (OSError, ValueError, subprocess.SubprocessError, ET.ParseError) as exc:
                record.update({"status": "check_error", "error": type(exc).__name__})
        records.append(record)

    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("w", encoding="utf-8") as output:
        for record in records:
            output.write(json.dumps(record, ensure_ascii=False) + "\n")
    complete = sum(record["status"] == "complete" for record in records)
    previews = sum(record["status"] == "preview_or_mismatch" for record in records)
    print(f"Complete: {complete}; preview/mismatch: {previews}; total: {len(records)}")
    for record in records:
        duration = record.get("audio_seconds")
        coverage = record.get("coverage")
        print(f"{record['id']}: {record['status']}" + (f" ({duration:.1f}s, {coverage:.0%})" if duration is not None else ""))
    print(f"Report: {args.output}")


if __name__ == "__main__":
    main()
