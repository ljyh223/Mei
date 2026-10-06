"""Find NCM TTML files with lyric lines assigned to multiple people."""

import argparse
from collections import Counter
import json
from pathlib import Path
import sys
import xml.etree.ElementTree as ET


TTM_NS = "http://www.w3.org/ns/ttml#metadata"
XML_NS = "http://www.w3.org/XML/1998/namespace"
AMLL_NS = "http://www.example.com/ns/amll"
EXCLUDED_ROLES = {"x-translation", "x-roman", "x-bg"}
DEFAULT_INPUT = Path("/home/ljyh/code/temp/amll-ttml-db-main/ncm-lyrics")
RESEARCH_DIR = Path(__file__).resolve().parents[1]


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


def inspect_ttml(path: Path, include_other: bool, min_lines: int, max_agents: int | None) -> dict | None:
    if not path.stem.isdecimal() or path.stem == "0":
        return None
    root = ET.parse(path).getroot()
    agents: dict[str, str] = {}
    metadata: dict[str, list[str]] = {}
    for element in root.iter():
        if local_name(element.tag) == "agent":
            agent_id = element.get(f"{{{XML_NS}}}id")
            if agent_id:
                agents[agent_id] = element.get("type", "unknown")
        elif local_name(element.tag) == "meta" and element.tag.startswith(f"{{{AMLL_NS}}}"):
            key, value = element.get("key"), element.get("value")
            if key and value:
                metadata.setdefault(key, []).append(value)

    allowed_types = {"person", "other"} if include_other else {"person"}
    counts: Counter[str] = Counter()
    sequence: list[str] = []
    group_lines = 0
    background_spans = 0
    for element in root.iter():
        if local_name(element.tag) != "p" or not main_text(element):
            continue
        agent_id = element.get(f"{{{TTM_NS}}}agent")
        agent_type = agents.get(agent_id or "", "unknown")
        if agent_id and agent_type in allowed_types:
            counts[agent_id] += 1
            sequence.append(agent_id)
        elif agent_type == "group":
            group_lines += 1
        background_spans += sum(
            1 for child in element.iter() if role_of(child) == "x-bg"
        )

    selected = {agent_id: count for agent_id, count in counts.items() if count >= min_lines}
    if len(selected) < 2:
        return None
    if max_agents is not None and len(selected) > max_agents:
        return None
    ncm_meta_ids = metadata.get("ncmMusicId", [])
    if ncm_meta_ids and path.stem not in ncm_meta_ids:
        raise ValueError(f"filename ID {path.stem} is absent from ncmMusicId metadata")
    return {
        "id": path.stem,
        "ttml_path": str(path),
        "title": metadata.get("musicName", [None])[0],
        "artists": metadata.get("artists", []),
        "agents": [
            {"id": agent_id, "type": agents[agent_id], "line_count": count}
            for agent_id, count in sorted(selected.items())
        ],
        "switches": sum(a != b for a, b in zip(sequence, sequence[1:])),
        "group_lines": group_lines,
        "background_spans": background_spans,
        "ncm_meta_ids": ncm_meta_ids,
    }


def scan(
    input_dir: Path, include_other: bool, min_lines: int, max_agents: int | None
) -> tuple[list[dict], Counter[str]]:
    rows = []
    stats: Counter[str] = Counter()
    for path in input_dir.glob("*.ttml"):
        stats["files"] += 1
        try:
            row = inspect_ttml(path, include_other, min_lines, max_agents)
        except (ET.ParseError, OSError, ValueError) as exc:
            stats["errors"] += 1
            print(f"Skipped {path.name}: {exc}", file=sys.stderr)
            continue
        if row:
            rows.append(row)
    rows.sort(key=lambda row: int(row["id"]))
    stats["matches"] = len(rows)
    return rows, stats


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input-dir", type=Path, default=DEFAULT_INPUT)
    parser.add_argument("--ids-output", type=Path, default=RESEARCH_DIR / "data/candidates/ids.txt")
    parser.add_argument("--report-output", type=Path, default=RESEARCH_DIR / "data/candidates/report.jsonl")
    parser.add_argument("--min-lines-per-agent", type=int, default=1)
    parser.add_argument("--max-agents", type=int, help="Keep candidates with at most this many active agents")
    parser.add_argument("--include-other", action="store_true", help="Also include type=other agents; expect more false positives")
    parser.add_argument("--no-write", action="store_true", help="Only print summary and first matching IDs")
    args = parser.parse_args()
    if not args.input_dir.is_dir():
        parser.error(f"Input directory does not exist: {args.input_dir}")
    if args.min_lines_per_agent < 1:
        parser.error("--min-lines-per-agent must be >= 1")
    if args.max_agents is not None and args.max_agents < 2:
        parser.error("--max-agents must be >= 2")
    rows, stats = scan(args.input_dir, args.include_other, args.min_lines_per_agent, args.max_agents)
    if not args.no_write:
        args.ids_output.parent.mkdir(parents=True, exist_ok=True)
        args.report_output.parent.mkdir(parents=True, exist_ok=True)
        args.ids_output.write_text("".join(row["id"] + "\n" for row in rows), encoding="utf-8")
        with args.report_output.open("w", encoding="utf-8") as output:
            for row in rows:
                output.write(json.dumps(row, ensure_ascii=False) + "\n")
        print(f"IDs: {args.ids_output}")
        print(f"Report: {args.report_output}")
    print(f"Scanned {stats['files']} TTML files; found {stats['matches']} candidates; skipped {stats['errors']} errors")
    print("First IDs:", ", ".join(row["id"] for row in rows[:20]))


if __name__ == "__main__":
    main()
