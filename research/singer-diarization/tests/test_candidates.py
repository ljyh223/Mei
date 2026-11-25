import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "tools"))
from filter_ncm_duets import inspect_file, read_lys


class CandidateTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.path = Path(self.temp.name) / "123.ttml"

    def write(self, extra="", second_type="other"):
        lines = ''.join(f'<p ttm:agent="v{i % 2 + 1}">line {i}</p>' for i in range(20))
        self.path.write_text(
            '<tt xmlns:ttm="http://www.w3.org/ns/ttml#metadata"><head><metadata>'
            '<ttm:agent xml:id="v1" type="person"/>'
            f'<ttm:agent xml:id="v2" type="{second_type}"/>'
            '<ttm:agent xml:id="g" type="group"/>'
            '</metadata></head><body>' + lines + extra + '</body></tt>')

    def test_clean_roles_are_candidates_never_verified(self):
        self.write()
        row, _ = inspect_file(self.path, .2)
        self.assertIn("strict", row["tiers"])
        self.assertEqual(row["annotation_status"], "candidate_only")
        self.assertEqual(row["lys_agent_agreement"], "not_checked")

    def test_missing_and_group_roles_are_in_denominator(self):
        for extra in ['<p>unknown</p>' * 100, '<p ttm:agent="g">group</p>' * 100]:
            self.write(extra)
            row, _ = inspect_file(self.path, .2)
            self.assertIsNone(row)

    def test_even_small_unknown_count_excludes_strict(self):
        self.write('<p>unknown</p>')
        row, _ = inspect_file(self.path, .2)
        self.assertAlmostEqual(row["primary_ratio"], 20 / 21, places=4)
        self.assertNotIn("strict", row["tiers"])

    def test_role_type_does_not_change_lyric_duplicate_group(self):
        self.write(second_type="other")
        a, _ = inspect_file(self.path, .2)
        self.write(second_type="person")
        b, _ = inspect_file(self.path, .2)
        self.assertEqual(a["content_hash"], b["content_hash"])

    def test_explicit_lys_sides_not_default_layout_count_turns(self):
        lys = self.path.with_suffix('.lys')
        lys.write_text('[1]left\n[2]right\n[4]left\n[5]right\n')
        self.assertEqual(read_lys(lys).duet_switches, 3)
        lys.write_text('[0]default\n[2]right\n[0]default\n[2]right\n')
        self.assertEqual(read_lys(lys).duet_switches, 0)

    def test_unknown_role_breaks_turn_adjacency(self):
        self.write()
        self.path.write_text(self.path.read_text().replace('</p><p', '</p><p>unknown</p><p'))
        row, _ = inspect_file(self.path, .99)
        self.assertIsNone(row)


if __name__ == '__main__':
    unittest.main()
