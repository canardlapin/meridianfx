"""Regression checks of compiled themes, resource integrity and scientific-color boundaries."""
import json
from pathlib import Path
import re
import shutil
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/"tools"))
import build_themes as themes
import generate_tokens as tokens

class ThemeBuildTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.outputs = themes.compile_outputs(ROOT)

    def fixture(self, root):
        receipt = json.loads(self.outputs[themes.OUTPUT+"/generation.json"])
        lock = json.loads((ROOT/"upstream/lock.json").read_text())
        for name in set(receipt["inputs"]) | set(lock["source_files"]):
            target=root/name; target.parent.mkdir(parents=True,exist_ok=True)
            shutil.copyfile(ROOT/name,target)
        tokens.write_outputs(root,self.outputs)

    def test_independent_paths_compile_identical_bytes(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory); self.fixture(root)
            (root/"theme-build/node_modules").symlink_to(ROOT/"theme-build/node_modules",target_is_directory=True)
            self.assertEqual(themes.compile_outputs(root),self.outputs)

    def test_chart_defaults_identical_across_all_variants(self):
        defaults=[]
        for name,data in self.outputs.items():
            if name.endswith(".css"):
                values=dict(re.findall(r"(-color-chart-[a-z0-9-]+):\s*([^;]+);",data.decode()))
                self.assertEqual(len(values),24)
                defaults.append(values)
        self.assertEqual(len(defaults),4)
        self.assertTrue(all(values==defaults[0] for values in defaults))
        self.assertEqual(defaults[0]["-color-chart-1"],"#f3622d")

    def test_manifest_ranges_match_real_module_boundaries(self):
        for name,data in self.outputs.items():
            if name.endswith(".css.manifest"):
                css=self.outputs[name[:-9]].decode().splitlines()
                entries=data.decode().split("[variables]")[0].splitlines()[1:]
                self.assertGreater(len(entries),40)
                for entry in entries:
                    if not entry: continue
                    module,span=entry.split("="); first,last=map(int,span.split(":"))
                    self.assertIn("@module:"+module+":start",css[first])
                    self.assertGreater(last,first)
                    if module!="meridian": self.assertIn("@module:"+module+":end",css[last])
                    else: self.assertIn("-meridian-text-primary", "\n".join(css[first:last+1]))

    def test_receipt_detects_changed_source_and_output(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory); self.fixture(root)
            themes.verify_receipt(root)
            source=root/tokens.SOURCE
            original=source.read_bytes()
            source.write_bytes(original+b"\n")
            with self.assertRaisesRegex(tokens.TokenError,"Stale theme inputs"): themes.verify_receipt(root)
            source.write_bytes(original)
            output=root/themes.OUTPUT/"meridian-light-compact.css"
            output.write_bytes(output.read_bytes()+b"\n/* hand edit */")
            with self.assertRaisesRegex(tokens.TokenError,"Stale theme outputs"): themes.verify_receipt(root)
            self.assertTrue(themes.output_problems(root,self.outputs))

    def test_missing_and_obsolete_theme_outputs_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory); self.fixture(root)
            file=root/themes.OUTPUT/"obsolete.css"; file.write_text("obsolete")
            self.assertTrue(any("unexpected" in p for p in themes.output_problems(root,self.outputs)))
            with self.assertRaisesRegex(tokens.TokenError,"Unexpected theme outputs"): themes.verify_receipt(root)
            file.unlink()
            (root/themes.OUTPUT/"meridian-dark-compact.css.manifest").unlink()
            self.assertTrue(any("missing" in p for p in themes.output_problems(root,self.outputs)))
            with self.assertRaises(tokens.TokenError): themes.verify_receipt(root)

    def test_changed_locked_upstream_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory); self.fixture(root)
            file=root/"upstream/atlantafx/styles/src/components/_button.scss"
            file.write_bytes(file.read_bytes()+b"\n//changed")
            with self.assertRaisesRegex(tokens.TokenError,"Locked upstream changed"): themes.verify_receipt(root)

if __name__=="__main__": unittest.main()
