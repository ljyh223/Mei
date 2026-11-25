"""Validation for manual, audio-bound detailed annotations (schema v2).

Candidate TTML roles never count as manual answers. Overlapping lyric lines
remain independent utterances; union identities across overlapping segments
when deriving global singer activity. A/B have song-local scope only.
"""
from __future__ import annotations

ACTIVITIES = {'singing', 'speech', 'no_vocal', 'uncertain'}
ARRANGEMENTS = {'solo', 'unison', 'harmony', 'adlib', 'uncertain', 'not_applicable'}


def integer(value):
    return type(value) is int


def union_ms(intervals):
    total, end = 0, None
    for start, stop in sorted(intervals):
        total += max(0, stop - max(start, end if end is not None else start))
        end = max(stop, end if end is not None else stop)
    return total


def covered(start, end, segments):
    intervals = [(max(start, s['start_ms']), min(end, s['end_ms'])) for s in segments
                 if s['start_ms'] < end and s['end_ms'] > start]
    return union_ms(intervals) >= end - start


def validate(document: dict, task: dict, *, draft=False) -> dict:
    """Strict import is final/manual; draft exports may be incomplete.

    This validator measures completeness, never certifies that a listener's
    identity judgments are correct. Unknown labels stay in the denominator.
    """
    def require(condition, message):
        if not condition:
            raise ValueError(message)

    require(document.get('schema_version') == 2, 'Expected annotation schema 2')
    require(document.get('song_id') == task['song_id'], 'Wrong song ID')
    require(document.get('revision') == task['revision'], 'Audio/lyric revision differs; do not relabel a different recording')
    require(document.get('audio_sha256') == task['audio'].get('sha256'), 'Audio fingerprint differs')
    require(document.get('lyric_sha256') == task.get('lyric_sha256'), 'Lyric fingerprint differs')
    require(document.get('song_decision') in {'single', 'two', 'multiple', 'uncertain', 'exclude'}, 'Choose a song-level conclusion')
    if not draft:
        require(document.get('completion') in {'partial', 'complete'}, 'Draft must remain a backup, not a confirmed annotation')
        require(task['audio'].get('ready_for_detail') is True, 'Fixed full local audio is required for detailed confirmation')
        require(document.get('reviewer', '').strip(), 'Fill a reviewer name or stable alias')
        require(document.get('version_confirmed') is True, 'Confirm recording version')
        require(document.get('task_checks', {}).get('whole_song_listened') is True, 'Listen to the entire recording, including gaps')
        require(document.get('task_checks', {}).get('unlyric_vocals_checked') is True, 'Check adlibs and vocals outside lyric lines')
    offset = document.get('offset_ms')
    require(integer(offset) and abs(offset) <= 600000, 'Invalid global offset')
    duration = task['audio'].get('duration_ms')
    require(duration is None or integer(duration) and duration > 0, 'Invalid audio duration')
    speakers = document.get('speakers')
    require(isinstance(speakers, list), 'Speaker registry is required')
    speaker_ids = set()
    for speaker in speakers:
        sid = speaker.get('id')
        require(isinstance(sid, str) and sid.strip() == sid and bool(sid), 'Speaker ID is empty')
        require(sid not in speaker_ids and sid not in {'unknown', 'A+B'}, 'Duplicate/reserved speaker ID')
        speaker_ids.add(sid)
        require(speaker.get('production') in {'natural', 'synthetic', 'uncertain'}, 'Choose singer production type')
        refs = speaker.get('references', [])
        require(isinstance(refs, list), 'Singer references must be a list')
        for ref in refs:
            require(integer(ref.get('start_ms')) and integer(ref.get('end_ms')) and
                    0 <= ref['start_ms'] < ref['end_ms'] and
                    (duration is None or ref['end_ms'] <= duration), 'Invalid singer reference interval')
        if not draft:
            require(bool(refs) or bool(speaker.get('reference_unavailable_reason', '').strip()),
                    f'Provide a clear reference for {sid}, or explain why none exists')
    anchors = document.get('anchors', [])
    require(isinstance(anchors, list), 'Alignment anchors must be a list')
    positions = set()
    for anchor in anchors:
        require(anchor.get('position') in {'beginning', 'middle', 'ending'}, 'Invalid anchor position')
        require(anchor['position'] not in positions, 'Duplicate alignment anchor')
        positions.add(anchor['position'])
        require(integer(anchor.get('lyric_ms')) and integer(anchor.get('audio_ms')) and
                anchor['lyric_ms'] >= 0 and anchor['audio_ms'] >= 0 and
                (duration is None or anchor['audio_ms'] <= duration), 'Invalid anchor timestamp')
        if not draft:
            require(anchor.get('checked') is True and bool(anchor.get('evidence', '').strip()), 'Each alignment anchor needs manual evidence')
    if not draft:
        require(positions == {'beginning', 'middle', 'ending'}, 'Check beginning, middle and ending alignment')
    originals = {r['line_index']: r for r in task['lines']}
    seen, checked_lines, complete_lines, known_segments, segment_count = set(), 0, 0, 0, 0

    def check_segments(segments):
        nonlocal known_segments, segment_count
        require(isinstance(segments, list), 'Segments must be a list')
        for segment in segments:
            start, end = segment.get('start_ms'), segment.get('end_ms')
            require(integer(start) and integer(end) and 0 <= start < end and
                    (duration is None or end <= duration), 'Segment outside audio or invalid interval')
            require(segment.get('activity') in ACTIVITIES, 'Choose vocal activity')
            require(segment.get('arrangement') in ARRANGEMENTS, 'Choose arrangement')
            require(segment.get('self_overdub') in {'present', 'absent', 'uncertain', 'not_applicable'}, 'Choose self-overdub state')
            require(segment.get('certainty') in {'certain', 'uncertain'}, 'Choose identity certainty')
            require(segment.get('boundary_quality') in {'precise', 'approximate', 'uncertain'}, 'Choose boundary quality')
            for field in ['foreground_ids', 'background_ids']:
                values = segment.get(field)
                require(isinstance(values, list) and all(isinstance(s, str) for s in values), 'Speaker IDs must be lists')
                require(len(values) == len(set(values)) and set(values) <= speaker_ids, 'Unknown/duplicate speaker ID')
            for field in ['unknown_foreground', 'unknown_background', 'reviewed']:
                require(type(segment.get(field)) is bool, 'Explicit boolean confirmation required')
            active = set(segment['foreground_ids']) | set(segment['background_ids'])
            unknown = segment['unknown_foreground'] or segment['unknown_background']
            if segment['activity'] == 'no_vocal':
                require(not active and not unknown and segment['arrangement'] == 'not_applicable', 'No-vocal interval cannot have singers')
            elif segment['activity'] in {'singing', 'speech'}:
                require(bool(active) or unknown, 'Vocal interval needs an identity or explicit unknown')
            if segment['activity'] == 'uncertain':
                require(segment['certainty'] == 'uncertain', 'Uncertain vocal activity cannot be a certain identity answer')
            if not draft:
                require(segment['reviewed'] is True, 'Every imported segment must be listened to')
                if segment['certainty'] == 'uncertain' or unknown or segment['boundary_quality'] == 'uncertain':
                    require(bool(segment.get('note', '').strip()), 'Uncertain segment needs a reason')
            segment_count += 1
            known_segments += int(segment['reviewed'] and segment['certainty'] == 'certain' and not unknown and
                                  segment['activity'] in {'singing', 'speech'})

    rows = document.get('lines')
    require(isinstance(rows, list), 'Line reviews must be a list')
    for line in rows:
        index = line.get('line_index')
        require(integer(index) and index in originals and index not in seen, 'Unknown/duplicate lyric line')
        seen.add(index)
        require(line.get('status') in {'pending', 'partial', 'complete', 'uncertain', 'not_vocal', 'excluded_text'}, 'Invalid line review state')
        segments = line.get('segments', [])
        check_segments(segments)
        if line['status'] == 'excluded_text':
            require(bool(line.get('note', '').strip()) and not segments,
                    'Excluded display text needs a reason and must not carry invented audio segments')
            complete_lines += 1
        if line['status'] in {'complete', 'not_vocal'}:
            start, end = line.get('window_start_ms'), line.get('window_end_ms')
            require(integer(start) and integer(end) and 0 <= start < end and
                    (duration is None or end <= duration), 'Set the actual audio window for each finished line')
            require(covered(start, end, [s for s in segments if s['reviewed']]), 'Finished line has unheard gaps')
            if line['status'] == 'not_vocal':
                require(all(s['activity'] == 'no_vocal' for s in segments), 'not_vocal line contains vocal activity')
            complete_lines += 1
        if segments:
            checked_lines += 1
    check_segments(document.get('extra_segments', []))
    if document.get('completion') == 'complete' and not draft:
        require(seen == set(originals) and complete_lines == len(originals), 'Cannot finish a song with unfinished lyric lines')
        require(document.get('task_checks', {}).get('final_consistency_checked') is True, 'Check song-wide identity consistency')
        expected = document.get('song_decision')
        require(expected != 'single' or len(speaker_ids) == 1, 'Single-singer song must have one registered identity')
        require(expected != 'two' or len(speaker_ids) == 2, 'Two-singer song must have two registered identities')
        require(expected != 'multiple' or len(speaker_ids) >= 3, 'Multiple-singer song must have at least three identities')
    require(document.get('completion') in {'draft', 'partial', 'complete'}, 'Choose draft/partial/complete')
    offsets = [a['audio_ms'] - a['lyric_ms'] for a in anchors]
    return {'total_lines': len(originals), 'lines_with_segments': checked_lines,
            'complete_lines': complete_lines, 'segments': segment_count,
            'certain_identity_segments': known_segments,
            'unreviewed_lines': len(originals) - complete_lines,
            'anchor_offsets_ms': offsets,
            'anchor_offset_spread_ms': max(offsets) - min(offsets) if offsets else None,
            'manual_completeness_only': True}
