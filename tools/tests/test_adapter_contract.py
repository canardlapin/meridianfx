"""Adverse cases for provider-to-generated-token coverage, including legacy aliases."""
import copy
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from verify_adapter import token_coverage


class AdapterCoverageTests(unittest.TestCase):
    def setUp(self):
        self.docs = "| `-dock-base` | canvas |\n| `-dock-surface-veil` | legacy alias |"
        self.mapping = {"-dock-base": "surface.canvas", "-dock-surface-veil": "surface.shell"}
        self.sheets = {mode: ".dock { -dock-base: #FFFFFF; -dock-surface-veil: #EEEEEE; }" for mode in ("dark", "light")}
        self.palettes = {mode: {"surface.canvas": "#FFFFFF", "surface.shell": "#EEEEEE"} for mode in self.sheets}

    def test_alias_is_covered(self):
        report = token_coverage(self.docs, self.mapping, self.sheets, self.palettes)
        self.assertIn("-dock-surface-veil", report["dark"])

    def test_new_provider_token_requires_mapping(self):
        with self.assertRaises(ValueError):
            token_coverage(self.docs + "\n| `-dock-new-token` | new |", self.mapping, self.sheets, self.palettes)

    def test_omitted_alias_is_rejected(self):
        mapping = dict(self.mapping)
        del mapping["-dock-surface-veil"]
        with self.assertRaises(ValueError):
            token_coverage(self.docs, mapping, self.sheets, self.palettes)

    def test_stale_generated_color_is_rejected(self):
        sheets = copy.deepcopy(self.sheets)
        sheets["dark"] = sheets["dark"].replace("#EEEEEE", "#DDDDDD")
        with self.assertRaises(ValueError):
            token_coverage(self.docs, self.mapping, sheets, self.palettes)


if __name__ == "__main__":
    unittest.main()
