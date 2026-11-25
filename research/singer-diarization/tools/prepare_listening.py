"""Inventory every candidate and build detailed listening tasks without relabeling.

Local-only generation. Re-running never writes under data/detailed_annotations/.
Duplicate lyric content is grouped; alternate NCM IDs are retained as versions.
"""
from __future__ import annotations

import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import xml.etree.ElementTree as ET

from detailed_annotations import validate
from import_ttml import extract
from prepare_review import read_jsonl, sha256
from check_audio_coverage import audio_duration_seconds

ROOT = Path(__file__).resolve().parents[1]
AUDIO_SUFFIXES = {'.mp3', '.flac', '.wav', '.m4a', '.ogg', '.opus', '.aac', '.audio'}
BUCKETS = {
    'standard': '已粗筛标准候选', 'main': '已粗筛轮唱候选',
    'challenge': '相似音色困难样本', 'negative': '独唱负例',
    'segments': '仅选片段', 'excluded': '历史排除／可复议',
    'screen': '尚待歌曲级筛选',
}


def bucket(feedback):
    return {'standard_candidate':'standard', 'main_candidate':'main', 'challenge':'challenge',
            'negative_control':'negative', 'selected_segments_only':'segments'}.get(
                feedback.get('dataset_usage'), 'excluded' if feedback.get('audio_review') == 'exclude' else 'screen')


def dump(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + '.tmp')
    temporary.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')
    temporary.replace(path)


def source_rows(root):
    data = root/'data'
    rows = read_jsonl(data/'candidates/ncm_duets/report.jsonl')
    by_id = {r['id']:r for r in rows}
    downloads = read_jsonl(data/'candidates/downloads.jsonl')
    for path in sorted((data/'batches').glob('*/downloads.jsonl')):
        downloads += read_jsonl(path)
    extras = [{'song_id':str(r['id']), 'title':r.get('title'), 'artists_from_metadata':r.get('artists', [])} for r in downloads]
    for path in sorted((data/'batches').glob('*/candidates.jsonl')):
        extras += read_jsonl(path)
    for row in extras:
        sid = row['song_id']
        if sid in by_id:
            continue
        source = row.get('source_ttml')
        if not source:
            source = str(Path('/home/ljyh/code/temp/amll-ttml-db-main/ncm-lyrics')/f'{sid}.ttml')
        by_id[sid] = {'id':sid, 'title':row.get('title') or sid,
                     'artists':row.get('artists_from_metadata', []), 'paths':{'ttml':source},
                     'content_hash':None, 'tiers':[], 'review_flags':['outside_current_filter']}
    for row in downloads:
        by_id[str(row['id'])]['acquisition_requested'] = True
    return list(by_id.values())


def build(root):
    data = root/'data'
    output = data/'listening'
    (output/'tasks').mkdir(parents=True, exist_ok=True)
    (output/'lyrics').mkdir(exist_ok=True)
    feedback = {r['song_id']:r for r in read_jsonl(data/'candidates/manual_audio_review.jsonl')}
    old_audio = {r['song_id']:r for r in read_jsonl(data/'songs.jsonl')}
    groups = {}
    errors = []
    for row in source_rows(root):
        source = Path(row['paths']['ttml'])
        local = data/'lyrics'/source.name
        if not source.exists():
            for fallback in [local, output/'lyrics'/source.name]:
                if fallback.exists():
                    source = fallback
                    break
        if not source.exists():
            errors.append({'song_id':row['id'], 'error':'missing_ttml', 'source':str(source)})
            continue
        try:
            lines = extract(source, row['id'], strict=False)
        except (ValueError, ET.ParseError, OSError) as exc:
            errors.append({'song_id':row['id'], 'error':type(exc).__name__, 'source':str(source)})
            continue
        if not lines:
            errors.append({'song_id':row['id'], 'error':'no_timed_lines', 'source':str(source)})
            continue
        group = row.get('content_hash') or 'source-'+sha256(source)[:16]
        groups.setdefault(group, []).append((row, source, lines))
    catalogue = []
    for group, versions in groups.items():
        # Prefer an explicitly screened ID, then an ID with local audio. Do not
        # discard other IDs: another ID can point to a different recording.
        def order(item):
            sid=item[0]['id']
            return (sid not in feedback, not any((data/'audio').glob(sid+'.*')), int(sid))
        versions.sort(key=order)
        representative = versions[0][0]['id']
        for row, source, lines in versions:
            sid = row['id']; fb=feedback.get(sid, {})
            audio_files = sorted(p for p in (data/'audio').glob(sid+'.*') if p.suffix.lower() in AUDIO_SUFFIXES)
            audio = {'status':'missing', 'ready_for_detail':False, 'sha256':None, 'duration_ms':None, 'url':None}
            if len(audio_files) > 1:
                audio['status']='multiple_recordings_need_selection'
            elif audio_files:
                file=audio_files[0]; digest=sha256(file); old=old_audio.get(sid,{})
                try:
                    duration=old.get('audio_duration_ms') if old.get('audio_sha256')==digest else None
                    duration=duration or round(audio_duration_seconds(file,'ffprobe')*1000)
                    end=max(r['end_ms'] for r in lines)
                    enough=duration>=end
                    audio.update(status='ready_version_pending' if enough else 'partial_or_mismatch',
                                 ready_for_detail=enough, sha256=digest,duration_ms=duration,
                                 url='../audio/'+file.name, path=str(file.relative_to(root)))
                except (ValueError, OSError, subprocess.SubprocessError) as exc:
                    # Preserve every candidate in the inventory even if one
                    # decode probe fails; failure is an explicit blocker.
                    audio['status']='probe_failed';audio['error']=type(exc).__name__
            lyric_hash=sha256(source)
            snapshot=output/'lyrics'/f'{sid}.ttml'
            if not snapshot.exists() or sha256(snapshot)!=lyric_hash:
                shutil.copyfile(source,snapshot)
            revision=hashlib.sha256((lyric_hash+(audio['sha256'] or 'no-audio')).encode()).hexdigest()
            # Keep source suggestions separate from the blank manual answers.
            risks=[]
            for i,line in enumerate(lines):
                events=[]
                if line['end_ms']-line['start_ms']<=2000: events.append('short_source_line')
                if line.get('source_invalid_interval'):events.append('invalid_source_interval')
                if line.get('source_background_spans'):events.append('source_background_voice')
                overlapping=[p['line_index'] for p in lines if p['line_index']!=line['line_index'] and
                             p['start_ms']<line['end_ms'] and p['end_ms']>line['start_ms']]
                if overlapping: events.append('source_timeline_overlap')
                if not line['agent_ids']:events.append('missing_source_role')
                risks.append({**line, 'candidate_only':True,'reviewed':False,
                              'source_risks':events,'overlapping_line_indices':overlapping})
            task={'schema_version':2,'song_id':sid,'title':row['title'],'artists':row.get('artists',[]),
                  'revision':revision,'lyric_sha256':lyric_hash,'lyric_snapshot_url':'lyrics/'+snapshot.name,
                  'audio':audio,'bucket':bucket(fb),'song_screen':fb,'source_flags':row.get('review_flags',[]),
                  'lyric_group_id':group,'representative_id':representative,
                  'alternate_ids':[v[0]['id'] for v in versions if v[0]['id']!=sid],
                  'lines':risks,'previous_manual':None}
            manual=data/'detailed_annotations'/f'{sid}.json'
            if manual.exists():
                document=json.loads(manual.read_text())
                if document.get('revision')==revision:
                    task['previous_manual']=document
                else:task['manual_blocker']='previous_annotation_for_different_revision'
            dump(output/'tasks'/f'{sid}.json',task)
            catalogue.append({'song_id':sid,'title':row['title'],'artists':row.get('artists',[]),
                              'bucket':task['bucket'],'bucket_name':BUCKETS[task['bucket']],
                              'audio_status':audio['status'],'ready_for_detail':audio['ready_for_detail'],
                              'line_count':len(lines),'source_role_counts':dict(Counter(','.join(r['agent_ids']) or 'missing' for r in lines)),
                              'source_short_lines':sum('short_source_line' in r['source_risks'] for r in risks),
                              'source_invalid_intervals':sum('invalid_source_interval' in r['source_risks'] for r in risks),
                              'source_overlap_lines':sum('source_timeline_overlap' in r['source_risks'] for r in risks),
                              'source_flags':task['source_flags'],'lyric_group_id':group,
                              'representative_id':representative,'is_representative':sid==representative,
                              'tier':next((t for t in ['strict','balanced','confirmed','broad'] if t in row.get('tiers',[])),'legacy'),
                              'manual_revision_matches':task['previous_manual'] is not None,
                              'acquisition_requested':row.get('acquisition_requested',False),
                              'task_url':'tasks/'+sid+'.json'})
    priority={name:i for i,name in enumerate(['standard','main','negative','challenge','segments','screen','excluded'])}
    catalogue.sort(key=lambda r:(priority[r['bucket']],r['song_id']!='108576',not r['is_representative'],
                                 not r['ready_for_detail'],r['tier']!='strict',int(r['song_id'])))
    counts=Counter(r['bucket'] for r in catalogue)
    summary={'schema_version':2,'recording_ids':len(catalogue),'lyric_groups':len(groups),
             'bucket_counts':dict(counts),'audio_counts':dict(Counter(r['audio_status'] for r in catalogue)),
             'representative_tasks':sum(r['is_representative'] for r in catalogue),
             'songs_with_invalid_source_intervals':sum(r['source_invalid_intervals']>0 for r in catalogue),
             'parse_errors':errors,'no_inferred_manual_truth':True}
    dump(output/'catalog.json',{'summary':summary,'songs':catalogue})
    dump(output/'summary.json',summary)
    with (output/'catalog.jsonl').open('w') as f:
        for row in catalogue:f.write(json.dumps(row,ensure_ascii=False)+'\n')
    with (output/'all_pending.tsv').open('w') as f:
        fields=['song_id','title','artists','bucket_name','audio_status','line_count','source_role_counts','source_short_lines','source_overlap_lines','source_invalid_intervals','source_flags','representative_id','tier']
        f.write('\t'.join(fields)+'\n')
        for row in catalogue:f.write('\t'.join(str(row[k]).replace('\n',' ').replace('\t',' ') for k in fields)+'\n')
    for name in BUCKETS:
        (output/f'ids_{name}.txt').write_text(''.join(r['song_id']+'\n' for r in catalogue if r['bucket']==name and r['is_representative']))
    known=[r for r in catalogue if r['song_id'] in feedback or r['audio_status']!='missing' or r['acquisition_requested']]
    with (output/'KNOWN_SONGS.md').open('w') as f:
        f.write('# 已采集或已反馈歌曲\n\n歌曲级通过不表示逐句完成。短句和重叠数量来自原歌词时间，仅作检查提示。\n\n')
        f.write('| ID | 歌曲 | 用途 | 本地音频状态 | 行数 | 原短行数 | 原重叠行数 |\n|---|---|---|---|---:|---:|---:|\n')
        for r in known:
            cells=[r['song_id'],r['title'],r['bucket_name'],r['audio_status'],r['line_count'],r['source_short_lines'],r['source_overlap_lines']]
            f.write('| '+' | '.join(str(c).replace('|','／').replace('\n',' ') for c in cells)+' |\n')
    for asset in ['index.html','app.mjs']:
        shutil.copyfile(root/'tools/listening_assets'/asset,output/asset)
    return summary


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root',type=Path,default=ROOT)
    parser.add_argument('--import-annotation',type=Path)
    args=parser.parse_args()
    if args.import_annotation:
        doc=json.loads(args.import_annotation.read_text())
        sid=doc.get('song_id','')
        if not isinstance(sid,str) or not sid.isdecimal():parser.error('Invalid song ID')
        path=args.root/'data/listening/tasks'/f'{sid}.json'
        if not path.exists():parser.error('Generate the listening pack first')
        task=json.loads(path.read_text())
        # Compare live inputs as well: a stale generated page cannot certify a
        # file that changed after generation.
        if task['audio'].get('path') and sha256(args.root/task['audio']['path'])!=task['audio']['sha256']:
            parser.error('Local audio changed; regenerate tasks')
        if sha256(args.root/'data/listening/lyrics'/f'{sid}.ttml')!=task['lyric_sha256']:
            parser.error('Lyric snapshot changed; regenerate tasks')
        try: report=validate(doc,task)
        except (ValueError,KeyError,TypeError) as exc:parser.error(str(exc))
        target=args.root/'data/detailed_annotations'/f'{sid}.json'
        if target.exists():
            old=target.read_bytes();history=target.parent/'history';history.mkdir(parents=True,exist_ok=True)
            (history/f'{sid}-{hashlib.sha256(old).hexdigest()[:16]}.json').write_bytes(old)
        dump(target,doc)
        print(json.dumps({'imported':sid,'quality_report':report},ensure_ascii=False))
    print(json.dumps(build(args.root),ensure_ascii=False))


if __name__=='__main__':main()
