"""Fetch song details and audio for candidate NetEase Cloud Music IDs."""

import argparse
import json
import os
from pathlib import Path
import time
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode, urlparse
from urllib.request import Request, urlopen


RESEARCH_DIR = Path(__file__).resolve().parents[1]
DEFAULT_IDS = RESEARCH_DIR / "data/candidates/two_person_ids.txt"
DEFAULT_AUDIO_DIR = RESEARCH_DIR / "data/audio"
DEFAULT_MANIFEST = RESEARCH_DIR / "data/candidates/downloads.jsonl"
VALID_LEVELS = {
    "standard", "higher", "exhigh", "lossless", "hires", "jyeffect",
    "dolby", "vivid", "jymaster", "sky",
}
EXTENSIONS = {"mp3", "flac", "m4a", "aac", "wav", "ogg", "opus"}


def api_get(base: str, endpoint: str, params: dict, cookie: str | None, timeout: int) -> dict:
    url = f"{base.rstrip('/')}/{endpoint.lstrip('/')}?{urlencode(params)}"
    headers = {"Accept": "application/json", "User-Agent": "Mei-singer-diarization-research/0.1"}
    if cookie:
        headers["Cookie"] = f"MUSIC_U={cookie}"
    request = Request(url, headers=headers)
    with urlopen(request, timeout=timeout) as response:
        return json.loads(response.read().decode("utf-8"))


def read_ids(path: Path, limit: int) -> list[str]:
    seen = set()
    result = []
    for raw in path.read_text(encoding="utf-8").splitlines():
        value = raw.strip()
        if not value or not value.isdecimal() or value in seen:
            continue
        seen.add(value)
        result.append(value)
        if len(result) >= limit:
            break
    return result


def values_from_response(response: dict, key: str) -> list[dict]:
    value = response.get(key)
    if isinstance(value, dict):
        return [value]
    return value if isinstance(value, list) else []


def audio_extension(audio: dict) -> str:
    candidate = str(audio.get("type") or "").lower().lstrip(".")
    if candidate in EXTENSIONS:
        return candidate
    path_suffix = Path(urlparse(str(audio.get("url") or "")).path).suffix.lower().lstrip(".")
    return path_suffix if path_suffix in EXTENSIONS else "audio"


def download_file(url: str, destination: Path, timeout: int) -> None:
    request = Request(url, headers={"User-Agent": "Mei-singer-diarization-research/0.1"})
    partial = destination.with_suffix(destination.suffix + ".part")
    try:
        with urlopen(request, timeout=timeout) as response, partial.open("wb") as output:
            while chunk := response.read(1024 * 1024):
                output.write(chunk)
        if partial.stat().st_size == 0:
            raise ValueError("empty audio response")
        partial.replace(destination)
    except Exception:
        partial.unlink(missing_ok=True)
        raise


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--api-base", required=True, help="Local api-enhanced base URL, e.g. http://127.0.0.1:3000")
    parser.add_argument("--ids-file", type=Path, default=DEFAULT_IDS)
    parser.add_argument("--output-dir", type=Path, default=DEFAULT_AUDIO_DIR)
    parser.add_argument("--manifest", type=Path, default=DEFAULT_MANIFEST)
    parser.add_argument("--limit", type=int, default=20, help="Maximum number of IDs per run (default: 20)")
    parser.add_argument("--level", choices=sorted(VALID_LEVELS), default="exhigh")
    parser.add_argument("--unblock", action="store_true", help="Ask api-enhanced to use song unlocking")
    parser.add_argument("--overwrite", action="store_true")
    parser.add_argument("--timeout", type=int, default=45)
    parser.add_argument("--delay", type=float, default=0.2, help="Pause between API requests in seconds")
    parser.add_argument("--cookie-env", default="MUSIC_U", help="Environment variable holding the MUSIC_U value")
    args = parser.parse_args()
    if args.limit < 1 or args.timeout < 1 or args.delay < 0:
        parser.error("limit and timeout must be positive; delay must be >= 0")
    if not args.ids_file.is_file():
        parser.error(f"ID file does not exist: {args.ids_file}")

    ids = read_ids(args.ids_file, args.limit)
    if not ids:
        parser.error(f"No numeric IDs found in {args.ids_file}")
    cookie = os.environ.get(args.cookie_env, "").strip() or None
    args.output_dir.mkdir(parents=True, exist_ok=True)
    args.manifest.parent.mkdir(parents=True, exist_ok=True)

    detail_by_id = {}
    url_by_id = {}
    for endpoint, key, destination in (
        ("song/detail", "songs", detail_by_id),
        ("song/url/v1", "data", url_by_id),
    ):
        try:
            response = api_get(
                args.api_base,
                endpoint,
                {"ids" if endpoint == "song/detail" else "id": ",".join(ids),
                 **({"level": args.level, "unblock": str(args.unblock).lower()} if endpoint != "song/detail" else {})},
                cookie,
                args.timeout,
            )
            for item in values_from_response(response, key):
                item_id = item.get("id")
                if item_id is not None:
                    destination[str(item_id)] = item
        except (HTTPError, URLError, TimeoutError, ValueError, json.JSONDecodeError) as exc:
            if isinstance(exc, HTTPError):
                detail = f"HTTP {exc.code}"
            elif isinstance(exc, URLError):
                detail = "connection failed"
            else:
                detail = type(exc).__name__
            parser.error(f"{endpoint} request failed ({detail}); check --api-base and local API status")
        time.sleep(args.delay)

    records = []
    success = 0
    for song_id in ids:
        detail = detail_by_id.get(song_id, {})
        audio = url_by_id.get(song_id, {})
        record = {
            "id": song_id,
            "title": detail.get("name"),
            "artists": [artist.get("name") for artist in detail.get("ar", []) if artist.get("name")],
            "level": args.level,
            "file": None,
            "status": "unavailable",
        }
        url = audio.get("url")
        if not url:
            record["status"] = "no_audio_url"
        else:
            extension = audio_extension(audio)
            destination = args.output_dir / f"{song_id}.{extension}"
            existing = list(args.output_dir.glob(f"{song_id}.*"))
            if existing and not args.overwrite:
                record["file"] = str(existing[0])
                record["status"] = "already_downloaded"
                success += 1
            else:
                try:
                    download_file(url, destination, args.timeout)
                    record["file"] = str(destination)
                    record["bytes"] = destination.stat().st_size
                    record["status"] = "downloaded"
                    success += 1
                except (HTTPError, URLError, TimeoutError, OSError, ValueError) as exc:
                    record["status"] = "download_failed"
                    record["error"] = f"HTTP {exc.code}" if isinstance(exc, HTTPError) else type(exc).__name__
        records.append(record)
        print(f"{song_id}: {record['status']}" + (f" — {record['title']}" if record["title"] else ""))
        time.sleep(args.delay)

    with args.manifest.open("w", encoding="utf-8") as output:
        for record in records:
            output.write(json.dumps(record, ensure_ascii=False) + "\n")
    print(f"Fetched {success}/{len(ids)} audio files. Manifest: {args.manifest}")


if __name__ == "__main__":
    main()
