"""Extract candidate line annotations from TTML; review them against audio."""

import argparse
import json
import re
import xml.etree.ElementTree as ET
from pathlib import Path


TTM_NS = "http://www.w3.org/ns/ttml#metadata"
XML_NS = "http://www.w3.org/XML/1998/namespace"
TIME_RE = re.compile(r"^\d+(?:\.\d+)?$")


def parse_time_ms(value: str) -> int:
    parts = value.split(":")
    if len(parts) == 1 and TIME_RE.fullmatch(parts[0]):
        total_seconds = float(parts[0])
    elif len(parts) == 2 and parts[0].isdigit() and TIME_RE.fullmatch(parts[1]):
        total_seconds = int(parts[0]) * 60 + float(parts[1])
    elif (
        len(parts) == 3
        and parts[0].isdigit()
        and parts[1].isdigit()
        and TIME_RE.fullmatch(parts[2])
    ):
        total_seconds = int(parts[0]) * 3600 + int(parts[1]) * 60 + float(parts[2])
    else:
        raise ValueError(f"Unsupported TTML time: {value!r}")
    return round(total_seconds * 1000)


def local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1].rsplit(":", 1)[-1]


def role_of(element: ET.Element) -> str:
    return element.get(f"{{{TTM_NS}}}role", element.get("ttm:role", ""))


def main_text(element: ET.Element) -> str:
    parts = [element.text or ""]
    for child in element:
        if role_of(child) not in {"x-translation", "x-roman", "x-bg"}:
            parts.append(main_text(child))
        parts.append(child.tail or "")
    return "".join(parts).strip()


def extract(ttml: Path, song_id: str) -> list[dict]:
    root = ET.parse(ttml).getroot()
    agent_types = {}
    for element in root.iter():
        if local_name(element.tag) == "agent":
            agent_id = element.get(f"{{{XML_NS}}}id", element.get("xml:id"))
            if agent_id:
                agent_types[agent_id] = element.get("type", "unknown")
    rows = []
    for element in root.iter():
        if local_name(element.tag) != "p":
            continue
        begin, end = element.get("begin"), element.get("end")
        if not begin or not end:
            continue
        text = main_text(element)
        if not text:
            continue
        start_ms, end_ms = parse_time_ms(begin), parse_time_ms(end)
        if start_ms >= end_ms:
            raise ValueError(f"Invalid line interval: {begin} .. {end}")
        agent = element.get(f"{{{TTM_NS}}}agent", element.get("ttm:agent"))
        rows.append({
            "song_id": song_id,
            "line_index": len(rows),
            "start_ms": start_ms,
            "end_ms": end_ms,
            "text": text,
            "agent_ids": [agent] if agent else [],
            "agent_type": agent_types.get(agent, "unknown") if agent else "unknown",
            "label": "unknown",
            "label_source": "ttml",
            "reviewed": False,
        })
    return rows


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--song-id", required=True)
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    rows = extract(args.input, args.song_id)
    if not rows:
        raise SystemExit("No timed lyric lines found in TTML")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("w", encoding="utf-8") as output:
        for row in rows:
            output.write(json.dumps(row, ensure_ascii=False) + "\n")
    print(f"Wrote {len(rows)} candidate lines to {args.output}")


if __name__ == "__main__":
    main()
