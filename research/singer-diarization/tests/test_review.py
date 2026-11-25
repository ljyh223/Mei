import copy
import json
import shutil
import tempfile
from pathlib import Path
import sys
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'tools'))
from prepare_review import validate_review, build, write_jsonl, read_jsonl
from import_ttml import extract


class ManualReviewTest(unittest.TestCase):
    def setUp(self):
        self.song = {'song_id':'123', 'revision':'version1', 'audio_duration_ms':10000,
                     'lines':[{'song_id':'123','line_index':0,'start_ms':1000,'end_ms':3000,
                               'text':'fixture','label':'A','reviewed':True,'label_source':'ttml'}]}
        self.review = {'schema_version':1,'song_id':'123','revision':'version1',
                       'audio_version_confirmed':True, 'song_decision':'two_singers','offset_ms':0,
                       'lines':[{'line_index':0,'start_ms':1000,'end_ms':3000,'label':'B',
                                 'reviewed':True,'timing_checked':True}]}

    def test_manual_correction_keeps_original_untouched(self):
        before = copy.deepcopy(self.song)
        rows = validate_review(self.review,self.song)
        self.assertEqual(self.song,before)
        self.assertEqual(rows[0]['label'],'B')
        self.assertEqual(rows[0]['suggested_label'],'A')
        self.assertTrue(rows[0]['eligible_for_evaluation'])

    def test_stale_audio_or_labels_rejected(self):
        self.review['revision']='old'
        with self.assertRaises(ValueError): validate_review(self.review,self.song)

    def test_song_level_confirmation_does_not_confirm_lines(self):
        self.review['lines']=[]
        self.assertEqual(validate_review(self.review,self.song),[])

    def test_missing_confirmations_rejected(self):
        for field in ['reviewed','timing_checked']:
            r=copy.deepcopy(self.review); r['lines'][0][field]=False
            with self.assertRaises(ValueError): validate_review(r,self.song)
        self.review['audio_version_confirmed']=False
        with self.assertRaises(ValueError): validate_review(self.review,self.song)

    def test_unresolved_unknown_and_skip_are_not_scored(self):
        for label in ['unknown','skip']:
            self.review['lines'][0]['label']=label
            self.assertFalse(validate_review(self.review,self.song)[0]['eligible_for_evaluation'])
        self.review['lines'][0]['label']='A'
        self.review['song_decision']='uncertain'
        self.assertFalse(validate_review(self.review,self.song)[0]['eligible_for_evaluation'])

    def test_shift_outside_audio_rejected(self):
        for offset in [-1001,8000,True,0.5]:
            self.review['offset_ms']=offset
            with self.assertRaises(ValueError): validate_review(self.review,self.song)

    def test_legacy_gender_label_is_not_accepted_as_identity(self):
        self.review['lines'][0]['label']='male'
        with self.assertRaises(ValueError): validate_review(self.review,self.song)

    def test_duplicate_or_changed_intervals_rejected(self):
        r=copy.deepcopy(self.review); r['lines']*=2
        with self.assertRaises(ValueError): validate_review(r,self.song)
        self.review['lines'][0]['end_ms']=4000
        with self.assertRaises(ValueError): validate_review(self.review,self.song)


class ReviewBuildTest(unittest.TestCase):
    def test_rebuild_preserves_legacy_and_requires_explicit_import(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root/'data/audio').mkdir(parents=True)
            (root/'data/labels').mkdir()
            (root/'data/candidates').mkdir()
            (root/'source').mkdir()
            (root/'tools').mkdir()
            for name in ['review.html','review.mjs']:
                shutil.copyfile(Path(__file__).resolve().parents[1]/'tools'/name, root/'tools'/name)
            (root/'data/audio/123.mp3').write_bytes(b'fixture audio')
            lyric=root/'source/123.ttml'
            lyric.write_text('<tt xmlns:ttm="http://www.w3.org/ns/ttml#metadata">'
                             '<ttm:agent xml:id="v1" type="person"/>'
                             '<p begin="1" end="3" ttm:agent="v1">test</p></tt>')
            rows=extract(lyric,'123'); rows[0].update(label='A',reviewed=True)
            legacy=root/'data/labels/123.jsonl'
            write_jsonl(legacy,rows); original=legacy.read_bytes()
            write_jsonl(root/'data/candidates/downloads.jsonl',[{'id':'123','title':'fixture'}])
            with patch('prepare_review.audio_duration_seconds',return_value=10):
                first=build(root,root/'source')
                second=build(root,root/'source')
                self.assertEqual(first,second)
                self.assertEqual(first['eligible_lines'],0)
                self.assertEqual(legacy.read_bytes(),original)
                bundle=json.loads((root/'data/review/bundle.json').read_text())
                song=bundle['songs'][0]
                review={'schema_version':1,'song_id':'123','revision':song['revision'],
                        'audio_version_confirmed':True,'offset_ms':0,'song_decision':'two_singers',
                        'lines':[{'line_index':0,'start_ms':1000,'end_ms':3000,'label':'A',
                                  'reviewed':True,'timing_checked':True}]}
                write_jsonl(root/'data/verified/123.jsonl',validate_review(review,song))
                self.assertEqual(build(root,root/'source')['eligible_lines'],1)
                manifest=read_jsonl(root/'data/songs.jsonl')[0]
                self.assertEqual(manifest['split'],'unassigned')
                self.assertEqual(manifest['line_review_status'],'complete')
                write_jsonl(root/'data/candidates/manual_audio_review.jsonl',[
                    {'song_id':'123','audio_review':'single_singer','dataset_usage':'negative_control'}])
                self.assertEqual(build(root,root/'source')['eligible_lines'],0)
                self.assertEqual(read_jsonl(root/'data/songs.jsonl')[0]['status'],'negative_control')
                self.assertEqual(json.loads((root/'data/review/bundle.json').read_text())['songs'],[])
                pilot=(root/'data/candidates/downloads.jsonl').read_bytes()
                write_jsonl(root/'data/batches/batch02/downloads.jsonl',[{'id':'456','title':'next batch'}])
                self.assertEqual(build(root,root/'source')['songs'],2)
                self.assertEqual((root/'data/candidates/downloads.jsonl').read_bytes(),pilot)
                (root/'data/audio/123.mp3').write_bytes(b'changed audio')
                self.assertEqual(build(root,root/'source')['eligible_lines'],0)
                self.assertEqual(legacy.read_bytes(),original)


if __name__=='__main__': unittest.main()
