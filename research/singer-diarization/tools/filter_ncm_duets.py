"""Filter NCM lyric files that contain duet annotations.

TTML supplies candidate line-level roles, not verified singer identities.
LYS is a same-source layout cross-check, NOT independent evidence. Its
presence does not verify line-by-line agent agreement. ESLRC/LRC/YRC/QRC carry timing and
text but no singer-role field, so they cannot be used as role evidence.

The script does not copy lyric files. It writes ID lists and JSONL reports
that point back to the original paths.
"""

from __future__ import annotations

import argparse
from collections import Counter
import hashlib
import json
import re
import sys
import unicodedata
from dataclasses import dataclass
from pathlib import Path
import xml.etree.ElementTree as ET


TTM_NS = "http://www.w3.org/ns/ttml#metadata"
XML_NS = "http://www.w3.org/XML/1998/namespace"
AMLL_NS = "http://www.example.com/ns/amll"
EXCLUDED_ROLES = {"x-translation", "x-roman", "x-bg"}
DUET_LYS_PROPERTIES = {1, 2, 4, 5, 7, 8}
LEFT_LYS_PROPERTIES = {1, 4, 7}
RIGHT_LYS_PROPERTIES = {2, 5, 8}
BACKGROUND_LYS_PROPERTIES = {6, 7, 8}
SINGER_AGENT_TYPES = {"person", "other"}
LYS_LINE_RE = re.compile(r"^\[(\d)\]")
FORMAT_SUFFIXES = (".ttml", ".lys", ".eslrc", ".lrc", ".yrc", ".qrc")

DEFAULT_INPUT = Path("/home/ljyh/code/temp/amll-ttml-db-main/ncm-lyrics")
RESEARCH_DIR = Path(__file__).resolve().parents[1]
DEFAULT_OUTPUT = RESEARCH_DIR / "data/candidates/ncm_duets"


@dataclass
class LysInfo:
    exists: bool = False
    property_counts: Counter[int] | None = None
    duet_lines: int = 0
    background_lines: int = 0
    duet_switches: int = 0


def local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def role_of(element: ET.Element) -> str:
    return element.get(f"{{{TTM_NS}}}role", "")


def main_text(element: ET.Element) -> str:
    parts = [element.text or ""]
    for child in element:
        if role_of(child) not in EXCLUDED_ROLES:
            parts.append(main_text(child))
        parts.append(child.tail or "")
    return "".join(parts).strip()


def parse_metadata(root: ET.Element) -> dict[str, list[str]]:
    metadata: dict[str, list[str]] = {}
    for element in root.iter():
        if local_name(element.tag) != "meta" or not element.tag.startswith(
            f"{{{AMLL_NS}}}"
        ):
            continue
        key, value = element.get("key"), element.get("value")
        if key and value:
            metadata.setdefault(key, []).append(value)
    return metadata


def read_lys(path: Path) -> LysInfo:
    if not path.exists():
        return LysInfo()
    properties: list[int] = []
    try:
        text = path.read_text(encoding="utf-8", errors="replace")
    except OSError:
        return LysInfo()

    for line in text.splitlines():
        match = LYS_LINE_RE.match(line.strip())
        if match:
            properties.append(int(match.group(1)))

    counts = Counter(properties)
    # Only explicit left/right values can establish a side change. Default
    # layout is not a third singer and must not be counted as a turn.
    duet_sequence = [
        "left" if prop in LEFT_LYS_PROPERTIES else
        "right" if prop in RIGHT_LYS_PROPERTIES else None
        for prop in properties
    ]
    return LysInfo(
        exists=True,
        property_counts=counts,
        duet_lines=sum(counts[p] for p in DUET_LYS_PROPERTIES),
        background_lines=sum(counts[p] for p in BACKGROUND_LYS_PROPERTIES),
        duet_switches=sum(a is not None and b is not None and a != b
                          for a, b in zip(duet_sequence, duet_sequence[1:])),
    )


def inspect_file(
    path: Path, max_extra_ratio: float
) -> tuple[dict | None, dict | None]:
    """Return (candidate row, lys-only row)."""
    if path.stem == "0" or not path.stem.isdecimal():
        return None, None

    try:
        root = ET.parse(path).getroot()
    except (ET.ParseError, OSError) as error:
        raise ValueError(f"无法解析 TTML: {error}") from error

    agents: dict[str, str] = {}
    for element in root.iter():
        if local_name(element.tag) != "agent":
            continue
        agent_id = element.get(f"{{{XML_NS}}}id")
        if agent_id:
            agents[agent_id] = element.get("type", "unknown")

    agent_counts: Counter[str] = Counter()
    sequence: list[str | None] = []
    content_lines: list[tuple[str, str]] = []
    group_lines = 0
    unknown_agent_lines = 0
    background_spans = 0

    for element in root.iter():
        if local_name(element.tag) != "p":
            continue
        if role_of(element) in EXCLUDED_ROLES:
            continue
        line_text = main_text(element)
        if not line_text:
            continue
        agent_id = element.get(f"{{{TTM_NS}}}agent") or ""
        agent_type = agents.get(agent_id, "unknown")
        content_lines.append((agent_type, line_text))
        if agent_type in SINGER_AGENT_TYPES:
            agent_counts[agent_id] += 1
            sequence.append(agent_id)
        elif agent_type == "group":
            group_lines += 1
            sequence.append(None)
        else:
            unknown_agent_lines += 1
            sequence.append(None)
        background_spans += sum(
            1 for child in element.iter() if role_of(child) == "x-bg"
        )

    lys = read_lys(path.with_suffix(".lys"))
    metadata = parse_metadata(root)
    ncm_meta_ids = metadata.get("ncmMusicId", [])
    metadata_id_mismatch = bool(ncm_meta_ids) and path.stem not in ncm_meta_ids

    active = {
        agent_id: count
        for agent_id, count in agent_counts.items()
        if agents.get(agent_id) in SINGER_AGENT_TYPES and count > 0
    }
    ranked = sorted(active.items(), key=lambda pair: (-pair[1], pair[0]))
    primary = ranked[:2]
    primary_ids = {agent_id for agent_id, _ in primary}
    # Unknown/group/third-singer rows break adjacency; do not invent a turn
    # by deleting those rows from the sequence.
    switches = sum(
        a in primary_ids and b in primary_ids and a != b
        for a, b in zip(sequence, sequence[1:])
    )
    total_active_lines = sum(active.values())
    primary_lines = sum(count for _, count in primary)
    extra_lines = total_active_lines - primary_lines
    total_main_lines = len(content_lines)
    primary_ratio = primary_lines / total_main_lines if total_main_lines else 0
    min_primary = min((count for _, count in primary), default=0)
    max_primary = max((count for _, count in primary), default=0)
    has_two_singers = len(primary) == 2

    formats = {
        suffix.lstrip("."): path.with_suffix(suffix).exists()
        for suffix in FORMAT_SUFFIXES
    }

    # Group the same lyrics even when role annotations or timings differ.
    # This is a split/leakage hint, NOT an assertion of identical recordings.
    normalized_text = [
        re.sub(r"\s+", "", unicodedata.normalize("NFKC", text)).casefold()
        for _, text in content_lines
    ]
    content_hash = hashlib.sha256(
        json.dumps(normalized_text, ensure_ascii=False).encode("utf-8")
    ).hexdigest()[:16]

    row = {
        "id": path.stem,
        "content_hash": content_hash,
        "title": (metadata.get("musicName") or ["未命名"])[0],
        "artists": metadata.get("artists", []),
        "album": (metadata.get("album") or [None])[0],
        "ncm_meta_ids": ncm_meta_ids,
        "metadata_id_mismatch": metadata_id_mismatch,
        "paths": {
            "ttml": str(path),
            "lys": str(path.with_suffix(".lys")) if lys.exists else None,
            "eslrc": (
                str(path.with_suffix(".eslrc"))
                if path.with_suffix(".eslrc").exists()
                else None
            ),
        },
        "formats": formats,
        "agents": [
            {
                "id": agent_id,
                "type": agents.get(agent_id, "unknown"),
                "lines": count,
                "primary": agent_id in primary_ids,
            }
            for agent_id, count in sorted(
                active.items(), key=lambda pair: (-pair[1], pair[0])
            )
        ],
        "primary_agents": [
            {"id": agent_id, "lines": count} for agent_id, count in primary
        ],
        "primary_line_counts": [count for _, count in primary],
        "primary_ratio": round(primary_ratio, 4),
        "extra_lines": extra_lines,
        "total_active_lines": total_active_lines,
        "total_main_lines": total_main_lines,
        "unknown_agent_lines": unknown_agent_lines,
        "switches": switches,
        "group_lines": group_lines,
        "background_spans": background_spans,
        "lys": {
            "exists": lys.exists,
            "property_counts": {
                str(key): value
                for key, value in sorted((lys.property_counts or {}).items())
            },
            "duet_lines": lys.duet_lines,
            "background_lines": lys.background_lines,
            "duet_switches": lys.duet_switches,
        },
    }

    broad = bool(
        has_two_singers
        and min_primary >= 1
        and switches >= 1
        and primary_ratio >= 1 - max_extra_ratio
    )
    lys_duet = lys.exists and lys.duet_lines > 0
    lys_conflict = lys.exists and broad and not lys_duet
    confirmed = bool(broad and (lys_duet or not lys.exists))
    balanced = bool(
        confirmed
        and min_primary >= 5
        and switches >= 5
        and primary_ratio >= 0.8
    )
    strict = bool(
        confirmed
        and min_primary >= 8
        and switches >= 5
        and primary_ratio >= 0.9
        and group_lines == 0
        and unknown_agent_lines == 0
        and len(active) == 2
        and not metadata_id_mismatch
    )

    tiers: list[str] = []
    if broad:
        tiers.append("broad")
    if confirmed:
        tiers.append("confirmed")
    if balanced:
        tiers.append("balanced")
    if strict:
        tiers.append("strict")

    row["tiers"] = tiers
    row["lys_duet"] = lys_duet
    row["lys_conflict"] = lys_conflict
    row["confidence"] = (
        "ttml_with_lys_layout"
        if confirmed and lys_duet
        else "ttml_only"
        if confirmed
        else "conflict"
        if lys_conflict
        else "low_support"
    )
    row["annotation_status"] = "candidate_only"
    row["lys_agent_agreement"] = "not_checked"
    row["review_flags"] = [
        name for name, present in {
            "unknown_roles": unknown_agent_lines > 0,
            "group_roles": group_lines > 0,
            "extra_roles": len(active) > 2,
            "background_vocals": background_spans > 0,
            "metadata_id_mismatch": metadata_id_mismatch,
            "imbalanced_roles": min_primary < 5 or (max_primary > 0 and min_primary / max_primary < 0.2),
        }.items() if present
    ]

    if lys_duet and not broad:
        if len(active) < 2:
            row["unmatched_reason"] = "single_singer"
        elif switches == 0:
            row["unmatched_reason"] = "no_singer_switch"
        elif primary_ratio < 1 - max_extra_ratio:
            row["unmatched_reason"] = "insufficient_primary_coverage"
        else:
            row["unmatched_reason"] = "too_many_extra_singers"
        return None, row
    return (row if broad else None), None


def write_jsonl(path: Path, rows: list[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as output:
        for row in rows:
            output.write(json.dumps(row, ensure_ascii=False) + "\n")


def write_ids(path: Path, rows: list[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        "".join(f"{row['id']}\n" for row in rows), encoding="utf-8"
    )


def write_preview(path: Path, rows: list[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as output:
        output.write(
            "id\ttitle\tartists\tprimary_lines\tswitches\tprimary_ratio\tlys_duet\tcontent_hash\tsame_content_count\n"
        )
        for row in rows:
            lines = "/".join(str(count) for count in row["primary_line_counts"])
            output.write(
                "\t".join(
                    [
                        row["id"],
                        row["title"],
                        "、".join(row["artists"]),
                        lines,
                        str(row["switches"]),
                        f"{row['primary_ratio']:.3f}",
                        str(int(row["lys_duet"])),
                        row["content_hash"],
                        str(len(row["same_content_ids"])),
                    ]
                )
                + "\n"
            )


def summarize(
    input_dir: Path,
    rows: list[dict],
    lys_unmatched_rows: list[dict],
    errors: list[str],
    formats_count: Counter[str],
) -> dict:
    broad = [row for row in rows if "broad" in row["tiers"]]
    confirmed = [row for row in rows if "confirmed" in row["tiers"]]
    balanced = [row for row in rows if "balanced" in row["tiers"]]
    strict = [row for row in rows if "strict" in row["tiers"]]
    exact_two = [
        row for row in broad if len(row["primary_agents"]) == 2 and row["extra_lines"] == 0
    ]
    return {
        "input_dir": str(input_dir),
        "files": dict(sorted(formats_count.items())),
        "ttml_files": formats_count.get("ttml", 0),
        "lys_files": formats_count.get("lys", 0),
        "eslrc_files": formats_count.get("eslrc", 0),
        "broad": len(broad),
        "confirmed": len(confirmed),
        "balanced": len(balanced),
        "strict": len(strict),
        "broad_unique_content": len({row["content_hash"] for row in broad}),
        "balanced_unique_content": len({row["content_hash"] for row in balanced}),
        "strict_unique_content": len({row["content_hash"] for row in strict}),
        "broad_exact_two_singers": len(exact_two),
        "lys_confirmed": sum(row["confidence"] == "ttml_with_lys_layout" for row in broad),
        "ttml_only": sum(row["confidence"] == "ttml_only" for row in broad),
        "lys_conflict": sum(row["lys_conflict"] for row in broad),
        "lys_duet_unmatched": len(lys_unmatched_rows),
        "lys_duet_unmatched_reasons": dict(
            Counter(row.get("unmatched_reason") for row in lys_unmatched_rows)
        ),
        "errors": len(errors),
        "schema_version": 2,
        "coverage_denominator": "all_nonempty_main_lyric_lines",
        "independent_annotation_sources": 1,
        "verified_songs": 0,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--input-dir",
        type=Path,
        default=DEFAULT_INPUT,
        help="包含 <ncm-id>.ttml/.lys/.eslrc 的目录",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=DEFAULT_OUTPUT,
        help="输出目录",
    )
    parser.add_argument(
        "--max-extra-ratio",
        type=float,
        default=0.2,
        help="允许非主两位歌手的歌词行占比上限，默认 0.2",
    )
    parser.add_argument(
        "--no-write",
        action="store_true",
        help="只输出统计，不写文件",
    )
    args = parser.parse_args()

    if not args.input_dir.is_dir():
        parser.error(f"输入目录不存在: {args.input_dir}")
    if not 0 <= args.max_extra_ratio < 1:
        parser.error("--max-extra-ratio 必须在 [0, 1) 内")

    rows: list[dict] = []
    lys_unmatched_rows: list[dict] = []
    errors: list[str] = []
    formats_count: Counter[str] = Counter()

    ttml_paths = sorted(
        args.input_dir.glob("*.ttml"),
        key=lambda path: int(path.stem) if path.stem.isdecimal() else 10**20,
    )
    for path in ttml_paths:
        formats_count["ttml"] += 1
        for suffix in FORMAT_SUFFIXES[1:]:
            if path.with_suffix(suffix).exists():
                formats_count[suffix.lstrip(".")] += 1
        try:
            row, lys_unmatched = inspect_file(path, args.max_extra_ratio)
        except ValueError as error:
            errors.append(f"{path.name}: {error}")
            continue
        if row:
            rows.append(row)
        if lys_unmatched:
            lys_unmatched_rows.append(lys_unmatched)

    rows.sort(key=lambda row: int(row["id"]))
    lys_unmatched_rows.sort(key=lambda row: int(row["id"]))

    content_groups: dict[str, list[dict]] = {}
    for row in rows:
        content_groups.setdefault(row["content_hash"], []).append(row)
    for group in content_groups.values():
        ids = [row["id"] for row in group]
        for row in group:
            row["same_content_ids"] = ids
            row["content_duplicate_count"] = len(ids) - 1

    summary = summarize(
        args.input_dir, rows, lys_unmatched_rows, errors, formats_count
    )

    print(
        f"扫描 {summary['ttml_files']} 个 TTML；"
        f"broad={summary['broad']}，confirmed={summary['confirmed']}，"
        f"balanced={summary['balanced']}，strict={summary['strict']}"
    )
    print(
        f"同源格式辅助标记（非人工确认）：TTML+LYS {summary['lys_confirmed']}，仅 TTML {summary['ttml_only']}，"
        f"LYS 冲突 {summary['lys_conflict']}，"
        f"LYS 有对唱但未进入 broad {summary['lys_duet_unmatched']}"
    )
    if errors:
        print(f"解析失败 {len(errors)} 个；示例：{errors[0]}", file=sys.stderr)

    if not args.no_write:
        args.output_dir.mkdir(parents=True, exist_ok=True)
        confirmed = [row for row in rows if "confirmed" in row["tiers"]]
        balanced = [row for row in rows if "balanced" in row["tiers"]]
        strict = [row for row in rows if "strict" in row["tiers"]]

        write_jsonl(args.output_dir / "report.jsonl", rows)
        write_jsonl(args.output_dir / "confirmed_report.jsonl", confirmed)
        write_jsonl(args.output_dir / "balanced_report.jsonl", balanced)
        write_jsonl(args.output_dir / "strict_report.jsonl", strict)
        write_jsonl(args.output_dir / "lys_unmatched.jsonl", lys_unmatched_rows)
        write_ids(args.output_dir / "ids_broad.txt", rows)
        write_ids(args.output_dir / "ids_confirmed.txt", confirmed)
        write_ids(
            args.output_dir / "ids_confirmed_lys.txt",
            [row for row in confirmed if row["confidence"] == "ttml_with_lys_layout"],
        )
        write_ids(
            args.output_dir / "ids_confirmed_ttml_only.txt",
            [row for row in confirmed if row["confidence"] == "ttml_only"],
        )
        write_ids(args.output_dir / "ids_balanced.txt", balanced)
        write_ids(args.output_dir / "ids_strict.txt", strict)

        def unique_rows(group: list[dict]) -> list[dict]:
            by_hash: dict[str, list[dict]] = {}
            for row in group:
                by_hash.setdefault(row["content_hash"], []).append(row)
            return sorted(
                (
                    min(
                        items,
                        key=lambda row: (
                            row["confidence"] != "ttml_with_lys_layout",
                            int(row["id"]),
                        ),
                    )
                    for items in by_hash.values()
                ),
                key=lambda row: int(row["id"]),
            )

        write_ids(args.output_dir / "ids_broad_unique.txt", unique_rows(rows))
        write_ids(
            args.output_dir / "ids_balanced_unique.txt", unique_rows(balanced)
        )
        write_ids(args.output_dir / "ids_strict_unique.txt", unique_rows(strict))
        write_preview(args.output_dir / "preview.tsv", rows)
        (args.output_dir / "summary.json").write_text(
            json.dumps(summary, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
        print(f"输出：{args.output_dir}")
        print(f"预览：{args.output_dir / 'preview.tsv'}")

    for row in rows[:8]:
        counts = "/".join(str(count) for count in row["primary_line_counts"])
        print(
            f"  {row['id']}  {row['title']}  "
            f"{'、'.join(row['artists']) or '未知歌手'}  "
            f"lines={counts} switches={row['switches']} "
            f"lys={'yes' if row['lys_duet'] else 'no'}"
        )


if __name__ == "__main__":
    main()
