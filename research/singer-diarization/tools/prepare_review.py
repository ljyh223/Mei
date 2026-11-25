"""Build reproducible local review inputs; never overwrite legacy labels.

No network access. Audio, lyrics and generated review files stay under data/.
Only imported, explicit per-line decisions are eligible for evaluation.
"""
from __future__ import annotations

import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import shutil

from check_audio_coverage import audio_duration_seconds
from filter_ncm_duets import inspect_file, write_jsonl
from import_ttml import extract

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_LYRICS = Path('/home/ljyh/code/temp/amll-ttml-db-main/ncm-lyrics')
LABELS = {'A', 'B', 'A+B', 'unknown', 'skip'}


def read_jsonl(path: Path) -> list[dict]:
    if not path.exists():
        return []
    return [json.loads(line) for line in path.read_text().splitlines() if line.strip()]


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open('rb') as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b''):
            h.update(chunk)
    return h.hexdigest()


def validate_review(review: dict, song: dict) -> list[dict]:
    """Validate before writing anything. A/B are song-local identities."""
    if review.get('schema_version') != 1 or review.get('song_id') != song['song_id']:
        raise ValueError('Wrong review schema or song ID')
    if review.get('revision') != song['revision']:
        raise ValueError('Audio/lyrics/labels changed; reopen the current review page')
    if review.get('audio_version_confirmed') is not True:
        raise ValueError('Confirm the audio/lyric version first')
    offset = review.get('offset_ms')
    if type(offset) is not int or abs(offset) > 600_000:
        raise ValueError('offset_ms must be an integer within +/-600000')
    decision = review.get('song_decision')
    if decision not in {'two_singers', 'uncertain', 'exclude'}:
        raise ValueError('Choose a song-level decision')
    originals = {line['line_index']: line for line in song['lines']}
    result, seen = [], set()
    decisions = review.get('lines')
    if not isinstance(decisions, list):
        raise ValueError('lines must be an array')
    for row in decisions:
        index = row.get('line_index')
        if type(index) is not int or index not in originals or index in seen:
            raise ValueError('Unknown or duplicate line index')
        seen.add(index)
        if row.get('label') not in LABELS or row.get('reviewed') is not True:
            raise ValueError('Only explicit manual labels are accepted')
        if row.get('timing_checked') is not True:
            raise ValueError('Each submitted line needs timing confirmation')
        original = originals[index]
        if any(row.get(k) != original[k] for k in ('start_ms', 'end_ms')):
            raise ValueError('Original times must be preserved; use offset_ms for global offset')
        start, end = original['start_ms'] + offset, original['end_ms'] + offset
        if start < 0 or end > song['audio_duration_ms']:
            raise ValueError('Reviewed interval is outside the available audio')
        result.append({
            **original, 'suggested_label': original['label'], 'label': row['label'],
            'reviewed': True, 'label_source': 'manual',
            'timing_checked': True, 'audio_version_confirmed': True,
            'offset_ms': offset, 'song_decision': decision,
            'revision': song['revision'], 'note': str(row.get('note', '')),
            'eligible_for_evaluation': decision == 'two_singers' and row['label'] in {'A', 'B', 'A+B'},
        })
    return result


def build(root: Path, lyrics_dir: Path) -> dict:
    data = root / 'data'
    output = data / 'review'
    output.mkdir(parents=True, exist_ok=True)
    (data / 'lyrics').mkdir(parents=True, exist_ok=True)
    manual = {r['song_id']: r for r in read_jsonl(data / 'candidates/manual_audio_review.jsonl')}
    bundle, manifest = [], []
    # Each acquisition batch keeps its own manifest; never replace the pilot log.
    downloads = {str(r['id']): r for r in read_jsonl(data / 'candidates/downloads.jsonl')}
    for path in sorted((data / 'batches').glob('*/downloads.jsonl')):
        downloads.update({str(r['id']): r for r in read_jsonl(path)})
    for download in downloads.values():
        song_id = str(download['id'])
        if not song_id.isdecimal() or int(song_id) <= 0:
            raise ValueError(f'Invalid song ID: {song_id!r}')
        # Relocatable: use the actual local audio, not old absolute manifest paths.
        audio = next(iter(sorted((data / 'audio').glob(f'{song_id}.*'))), None)
        source = lyrics_dir / f'{song_id}.ttml'
        record = {
            'schema_version': 2, 'song_id': song_id, 'title': download.get('title', song_id),
            'artists': download.get('artists', []), 'split': 'unassigned',
            'offset_ms': None, 'audio_version_verified': False,
            'lyric_source': 'amll-ttml-db', 'source_lyric_path': str(source),
            'prior_song_review': manual.get(song_id, {}),
            'status': 'missing_audio' if not audio else 'missing_lyrics',
            'eligible_lines': 0,
        }
        if not audio or not source.exists():
            manifest.append(record)
            continue
        lyric = data / 'lyrics' / source.name
        if not lyric.exists() or sha256(lyric) != sha256(source):
            shutil.copyfile(source, lyric)
        legacy = data / 'labels' / f'{song_id}.jsonl'
        lines = read_jsonl(legacy) or extract(lyric, song_id)
        extracted = extract(lyric, song_id)
        if len(lines) != len(extracted) or any(
            any(a.get(k) != b.get(k) for k in ('line_index', 'start_ms', 'end_ms', 'text', 'agent_ids'))
            for a, b in zip(lines, extracted)
        ):
            raise ValueError(f'Legacy labels no longer match source TTML: {song_id}; originals were not changed')
        if not lines:
            raise ValueError(f'No timed lyric lines: {song_id}')
        if any(r['song_id'] != song_id or r['line_index'] != i or
               type(r['start_ms']) is not int or type(r['end_ms']) is not int or
               r['start_ms'] < 0 or r['start_ms'] >= r['end_ms'] or r.get('label') not in LABELS
               for i, r in enumerate(lines)):
            raise ValueError(f'Invalid legacy label rows: {song_id}')
        duration = round(audio_duration_seconds(audio, 'ffprobe') * 1000)
        end = max(r['end_ms'] for r in lines)
        audio_hash, lyric_hash = sha256(audio), sha256(lyric)
        revision = hashlib.sha256((audio_hash + lyric_hash + json.dumps(lines, sort_keys=True)).encode()).hexdigest()
        candidate, _ = inspect_file(lyric, .2)
        prior = manual.get(song_id, {})
        status = ('negative_control' if prior.get('audio_review') == 'single_singer' else
                  'segment_review_only' if prior.get('audio_review') == 'segment_only' else
                  'excluded_by_prior_review' if prior.get('audio_review') == 'exclude' else
                  'preview_or_mismatch' if duration < .9 * end else 'pending_manual_review')
        record.update({
            'status': status, 'audio_path': str(audio.relative_to(root)),
            'dataset_usage': prior.get('dataset_usage', 'candidate'),
            'role_labels_rejected': prior.get('audio_review') == 'single_singer',
            'lyric_path': str(lyric.relative_to(root)),
            'audio_sha256': audio_hash, 'lyric_sha256': lyric_hash, 'revision': revision,
            'recording_file_group_id': audio_hash,
            'audio_duration_ms': duration, 'duration_ratio': round(duration / end, 4),
            'duration_check_only': True, 'lyric_end_ms': end,
            'line_count': len(lines), 'suggested_labels': dict(Counter(r['label'] for r in lines)),
            'legacy_reviewed_lines': sum(r.get('reviewed') is True for r in lines),
            'review_flags': candidate['review_flags'] if candidate else ['outside_current_candidate_filter'],
            'lyric_group_id': candidate['content_hash'] if candidate else 'text-' + hashlib.sha256(
                json.dumps([r['text'] for r in lines], ensure_ascii=False).encode()).hexdigest()[:16],
        })
        verified = read_jsonl(data / 'verified' / f'{song_id}.jsonl')
        record['eligible_lines'] = sum(status == 'pending_manual_review' and
                                      r.get('revision') == revision and
                                      r.get('eligible_for_evaluation') is True for r in verified)
        if verified and all(r.get('revision') == revision for r in verified):
            record['manual_decision'] = verified[0]['song_decision']
            record['offset_ms'] = verified[0]['offset_ms']
            record['audio_version_verified'] = True
        record['manually_reviewed_lines'] = sum(r.get('revision') == revision for r in verified)
        record['line_review_status'] = ('complete' if record['manually_reviewed_lines'] == len(lines) else
                                        'partial' if record['manually_reviewed_lines'] else 'pending')
        manifest.append(record)
        if status == 'pending_manual_review':
            bundle.append({**record, 'audio_url': '../audio/' + audio.name, 'lines': lines,
                           'previous_decisions': verified if all(r.get('revision') == revision for r in verified) else []})
    # Canonical manifest records blockers as well as usable songs; never auto-split.
    write_jsonl(data / 'songs.jsonl', manifest)
    payload = {'schema_version': 1, 'songs': bundle}
    (output / 'bundle.json').write_text(json.dumps(payload, ensure_ascii=False, indent=2) + '\n')
    template = (root / 'tools/review.html').read_text()
    embedded = json.dumps(payload, ensure_ascii=False).replace('<', '\\u003c')
    (output / 'index.html').write_text(template.replace('__BUNDLE_JSON__', embedded))
    shutil.copyfile(root / 'tools/review.mjs', output / 'review.mjs')
    with (output / 'queue.tsv').open('w') as f:
        f.write('song_id\ttitle\tstatus\tline_count\tflags\n')
        for r in sorted(manifest, key=lambda r: r['status'] != 'pending_manual_review'):
            cells = [r['song_id'], r['title'], r['status'], r.get('line_count', 0), ','.join(r.get('review_flags', []))]
            f.write('\t'.join(str(v).replace('\t', ' ').replace('\n', ' ') for v in cells) + '\n')
    summary = {'songs': len(manifest), 'statuses': dict(Counter(r['status'] for r in manifest)),
               'review_page_songs': len(bundle), 'review_page_lines': sum(len(r['lines']) for r in bundle),
               'eligible_lines': sum(r['eligible_lines'] for r in manifest)}
    (output / 'summary.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2) + '\n')
    return summary


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=ROOT)
    parser.add_argument('--lyrics-dir', type=Path, default=DEFAULT_LYRICS)
    parser.add_argument('--import-review', type=Path, help='JSON exported by the local listening page')
    args = parser.parse_args()
    if not shutil.which('ffprobe'):
        parser.error('FFmpeg/ffprobe is required')
    print(json.dumps(build(args.root, args.lyrics_dir), ensure_ascii=False))
    if args.import_review:
        review = json.loads(args.import_review.read_text())
        bundle = json.loads((args.root / 'data/review/bundle.json').read_text())
        song = next((s for s in bundle['songs'] if s['song_id'] == review.get('song_id')), None)
        if song is None:
            parser.error('Song is not in the current review queue')
        try:
            rows = validate_review(review, song)
        except (ValueError, TypeError, KeyError) as error:
            parser.error(str(error))
        if not rows:
            parser.error('No explicitly reviewed lines; previous results were not changed')
        # Exports are cumulative; reject accidental loss of prior line decisions.
        previous = read_jsonl(args.root / 'data/verified' / f"{song['song_id']}.jsonl")
        old = {r['line_index'] for r in previous if r.get('revision') == song['revision']}
        if not old.issubset({r['line_index'] for r in rows}):
            parser.error('Export omits previously imported lines; reload the page and restore decisions')
        target = args.root / 'data/verified' / f"{song['song_id']}.jsonl"
        target.parent.mkdir(parents=True, exist_ok=True)
        temporary = target.with_suffix('.jsonl.tmp')
        write_jsonl(temporary, rows)
        temporary.replace(target)
        print(json.dumps(build(args.root, args.lyrics_dir), ensure_ascii=False))
        print(f'Imported {len(rows)} explicit decisions; legacy labels unchanged')


if __name__ == '__main__':
    main()
