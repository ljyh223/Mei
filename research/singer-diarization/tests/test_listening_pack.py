import json
from pathlib import Path
import shutil
import sys
import tempfile
import unittest

sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'tools'))
from import_ttml import extract
from prepare_listening import build
from prepare_review import write_jsonl


class ListeningPackTest(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup)
        self.root=Path(self.temp.name)
        self.source=self.root/'source';self.source.mkdir()
        (self.root/'tools').mkdir()
        shutil.copytree(Path(__file__).resolve().parents[1]/'tools/listening_assets',self.root/'tools/listening_assets')
        self.xml='<tt xmlns:ttm="http://www.w3.org/ns/ttml#metadata"><ttm:agent xml:id="v1" type="person"/><p begin="0" end="0">credit</p><p begin="1" end="2" ttm:agent="v1">fixture</p></tt>'
        self.rows=[]
        for sid in ['123','456']:
            path=self.source/(sid+'.ttml');path.write_text(self.xml)
            self.rows.append({'id':sid,'title':'fixture','artists':['fixture'],
                             'paths':{'ttml':str(path)},'content_hash':'same-lyrics','tiers':['strict'],'review_flags':[]})
        write_jsonl(self.root/'data/candidates/ncm_duets/report.jsonl',self.rows)
        write_jsonl(self.root/'data/candidates/manual_audio_review.jsonl',[
            {'song_id':'456','audio_review':'usable','dataset_usage':'standard_candidate'}])

    def test_all_versions_retained_and_screened_id_is_group_representative(self):
        summary=build(self.root)
        self.assertEqual(summary['recording_ids'],2)
        self.assertEqual(summary['lyric_groups'],1)
        songs=json.loads((self.root/'data/listening/catalog.json').read_text())['songs']
        self.assertEqual({r['representative_id'] for r in songs},{'456'})
        self.assertTrue(all(r['audio_status']=='missing' for r in songs))

    def test_zero_length_source_is_flagged_not_deleted_or_repaired(self):
        with self.assertRaises(ValueError):extract(self.source/'123.ttml','123')
        build(self.root)
        task=json.loads((self.root/'data/listening/tasks/123.json').read_text())
        self.assertEqual(len(task['lines']),2)
        self.assertEqual(task['lines'][0]['start_ms'],task['lines'][0]['end_ms'])
        self.assertIn('invalid_source_interval',task['lines'][0]['source_risks'])
        self.assertTrue(all(l['reviewed'] is False for l in task['lines']))

    def test_rebuild_uses_snapshots_after_relocation_and_preserves_manual_files(self):
        build(self.root)
        manual=self.root/'data/detailed_annotations/123.json';manual.parent.mkdir()
        manual.write_text(json.dumps({'revision':'old','note':'do not lose'}));before=manual.read_bytes()
        shutil.rmtree(self.source)
        summary=build(self.root)
        self.assertEqual(summary['recording_ids'],2)
        self.assertEqual(manual.read_bytes(),before)
        task=json.loads((self.root/'data/listening/tasks/123.json').read_text())
        self.assertEqual(task['manual_blocker'],'previous_annotation_for_different_revision')


if __name__=='__main__':unittest.main()
