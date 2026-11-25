import copy
import sys
from pathlib import Path
import unittest

sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'tools'))
from detailed_annotations import validate, union_ms, covered


class DetailedAnnotationTest(unittest.TestCase):
    def setUp(self):
        self.task={'song_id':'123','revision':'r','lyric_sha256':'lyric',
                   'audio':{'sha256':'audio','duration_ms':10000,'ready_for_detail':True},
                   'lines':[{'line_index':0,'start_ms':1000,'end_ms':4000}]}
        segment={'start_ms':1000,'end_ms':4000,'activity':'singing','foreground_ids':['A'],
                 'background_ids':[],'unknown_foreground':False,'unknown_background':False,
                 'arrangement':'solo','self_overdub':'absent','certainty':'certain',
                 'boundary_quality':'precise','reviewed':True,'note':''}
        self.doc={'schema_version':2,'song_id':'123','revision':'r','audio_sha256':'audio',
                  'lyric_sha256':'lyric','reviewer':'fixture','version_confirmed':True,
                  'song_decision':'single','offset_ms':0,'completion':'complete',
                  'speakers':[{'id':'A','production':'natural','references':[{'start_ms':1000,'end_ms':3000}]}],
                  'anchors':[{'position':p,'lyric_ms':t,'audio_ms':t,'checked':True,'evidence':'fixture'}
                             for p,t in [('beginning',1000),('middle',5000),('ending',9000)]],
                  'task_checks':{'whole_song_listened':True,'unlyric_vocals_checked':True,'final_consistency_checked':True},
                  'lines':[{'line_index':0,'status':'complete','window_start_ms':1000,'window_end_ms':4000,'segments':[segment]}],
                  'extra_segments':[]}

    def test_valid_fully_bound_manual_annotation(self):
        report=validate(self.doc,self.task)
        self.assertEqual(report['complete_lines'],1)
        self.assertEqual(report['certain_identity_segments'],1)

    def test_intervals_union_not_negative_for_nested_or_duplicate(self):
        self.assertEqual(union_ms([(0,100),(10,20),(50,150),(0,100)]),150)
        self.assertTrue(covered(10,50,[{'start_ms':0,'end_ms':100}]))

    def test_sequential_singers_are_two_intervals_not_automatic_overlap(self):
        doc=copy.deepcopy(self.doc);doc['song_decision']='two'
        doc['speakers'].append({'id':'B','production':'natural','reference_unavailable_reason':'fixture'})
        first=doc['lines'][0]['segments'][0];first['end_ms']=2500
        second={**copy.deepcopy(first),'start_ms':2500,'end_ms':4000,'foreground_ids':['B']}
        doc['lines'][0]['segments'].append(second)
        self.assertEqual(validate(doc,self.task)['segments'],2)

    def test_same_identity_can_be_in_foreground_and_background(self):
        self.doc['lines'][0]['segments'][0].update(background_ids=['A'],self_overdub='present')
        self.assertEqual(validate(self.doc,self.task)['certain_identity_segments'],1)

    def test_uncertain_additional_voice_retained_not_known_truth(self):
        self.doc['lines'][0]['segments'][0].update(unknown_background=True,note='Cannot identify backing voice')
        self.assertEqual(validate(self.doc,self.task)['certain_identity_segments'],0)

    def test_completed_line_cannot_have_unheard_gap(self):
        self.doc['lines'][0]['segments'][0]['end_ms']=3000
        with self.assertRaisesRegex(ValueError,'gaps'):validate(self.doc,self.task)

    def test_draft_cannot_be_imported_as_formal_result(self):
        self.doc['completion']='draft'
        with self.assertRaisesRegex(ValueError,'Draft'):validate(self.doc,self.task)

    def test_excluded_display_text_keeps_reason_without_invented_audio(self):
        row=self.doc['lines'][0];row['status']='excluded_text';row['segments']=[]
        with self.assertRaisesRegex(ValueError,'reason'):validate(self.doc,self.task)
        row['note']='Production credit, not sung'
        self.assertEqual(validate(self.doc,self.task)['complete_lines'],1)

    def test_alignment_drift_is_reported_rather_than_silently_normalized(self):
        self.doc['anchors'][2]['audio_ms']+=700
        report=validate(self.doc,self.task)
        self.assertEqual(report['anchor_offset_spread_ms'],700)

    def test_source_roles_cannot_supply_identity(self):
        self.doc['lines'][0]['segments'][0]['foreground_ids']=['v1']
        with self.assertRaisesRegex(ValueError,'speaker'):validate(self.doc,self.task)

    def test_missing_local_audio_or_any_fingerprint_rejected(self):
        for key in ['revision','audio_sha256','lyric_sha256']:
            doc=copy.deepcopy(self.doc);doc[key]='other'
            with self.assertRaises(ValueError):validate(doc,self.task)
        self.task['audio']['ready_for_detail']=False
        with self.assertRaisesRegex(ValueError,'local audio'):validate(self.doc,self.task)

    def test_partial_export_does_not_become_full_completion(self):
        self.doc['lines'][0]['status']='partial';self.doc['completion']='partial'
        self.assertEqual(validate(self.doc,self.task)['unreviewed_lines'],1)
        self.doc['completion']='complete'
        with self.assertRaisesRegex(ValueError,'unfinished'):validate(self.doc,self.task)

    def test_uncertainty_requires_reason(self):
        s=self.doc['lines'][0]['segments'][0];s['certainty']='uncertain'
        with self.assertRaisesRegex(ValueError,'reason'):validate(self.doc,self.task)
        s['note']='similar voice';validate(self.doc,self.task)

    def test_no_vocal_cannot_contain_known_identity(self):
        self.doc['lines'][0]['segments'][0]['activity']='no_vocal'
        with self.assertRaisesRegex(ValueError,'No-vocal'):validate(self.doc,self.task)

    def test_no_vocal_without_singers_is_valid(self):
        self.doc['lines'][0]['status']='not_vocal'
        self.doc['lines'][0]['segments'][0].update(activity='no_vocal',foreground_ids=[],arrangement='not_applicable')
        self.assertEqual(validate(self.doc,self.task)['certain_identity_segments'],0)

    def test_stable_person_id_not_gender_or_song_class(self):
        self.doc['speakers'].append({'id':'B','production':'natural','reference_unavailable_reason':'fixture'})
        with self.assertRaisesRegex(ValueError,'one registered'):validate(self.doc,self.task)

    def test_three_alignment_anchors_are_required(self):
        self.doc['anchors'].pop()
        with self.assertRaisesRegex(ValueError,'beginning'):validate(self.doc,self.task)

    def test_out_of_audio_or_bool_timestamps_rejected(self):
        for invalid in [True,10001,-1,1.5]:
            d=copy.deepcopy(self.doc);d['lines'][0]['segments'][0]['end_ms']=invalid
            with self.assertRaises(ValueError):validate(d,self.task)


if __name__=='__main__':unittest.main()
