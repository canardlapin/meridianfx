"""Independent numerical/reference and failure-path tests for the token pipeline."""
import copy
from decimal import Decimal, localcontext
import json
from pathlib import Path
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
import generate_tokens as tokens

def decimal_contrast(fg_hex, bg_hex):
    # A high-precision independent implementation of the normative luminance definition.
    def luminance(color):
        parts = [Decimal(int(color[n:n + 2], 16)) / Decimal(255) for n in (1, 3, 5)]
        linear = [p / Decimal("12.92") if p <= Decimal("0.04045") else
                  ((p + Decimal("0.055")) / Decimal("1.055")) ** Decimal("2.4") for p in parts]
        return sum(p * weight for p, weight in zip(linear, map(Decimal, ("0.2126", "0.7152", "0.0722"))))
    with localcontext() as context:
        context.prec = 60
        a, b = luminance(fg_hex), luminance(bg_hex)
        return float((max(a, b) + Decimal(".05")) / (min(a, b) + Decimal(".05")))

class TokenTests(unittest.TestCase):
    def setUp(self):
        self.data = (ROOT / tokens.SOURCE).read_bytes()
        self.model = json.loads(self.data)

    def test_status_roles_do_not_alias_selection_or_focus(self):
        for mode in tokens.MODES:
            altered = copy.deepcopy(self.model)
            altered["roles"][mode]["status.neutral"] = altered["roles"][mode]["selection.inactive-marker"]
            with self.subTest(mode=mode), self.assertRaisesRegex(tokens.TokenError, "Status roles"):
                tokens.validate(altered)

    def test_status_text_and_dot_contrast_independent_reference(self):
        for mode in tokens.MODES:
            palette = tokens.colors(self.model, mode)
            for status in ("neutral", "progress"):
                for surface in ("canvas", "shell", "raised"):
                    with self.subTest(mode=mode, status=status, surface=surface):
                        value = decimal_contrast(palette["status." + status], palette["surface." + surface])
                        self.assertGreaterEqual(value, 4.5)
                        self.assertEqual(self.model["recipes"][f"status.{status}.dot.{surface}"]["kind"], "essential")
        self.model["recipes"].pop("status.progress.dot.raised")
        with self.assertRaises(tokens.TokenError):
            tokens.validate(self.model)

    def test_white_black_reference(self):
        self.assertEqual(tokens.ratio(tokens.rgba("#FFFFFF"), tokens.rgba("#000000")), 21.0)

    def test_independent_decimal_reference(self):
        for fg, bg in [("#777777", "#FFFFFF"), ("#767676", "#FFFFFF"), ("#6258D5", "#FFFFFF"),
                       ("#A7AFBE", "#17191F"), ("#7B8595", "#E4E8EF"), ("#040A13", "#AAAAAA")]:
            with self.subTest(fg=fg, bg=bg):
                self.assertAlmostEqual(tokens.ratio(tokens.rgba(fg), tokens.rgba(bg)),
                                       decimal_contrast(fg, bg), places=11)

    def test_threshold_is_not_rounded(self):
        value = tokens.ratio(tokens.rgba("#777777"), tokens.rgba("#FFFFFF"))
        self.assertEqual(round(value, 1), 4.5)
        self.assertLess(value, 4.5)
        self.assertGreater(tokens.ratio(tokens.rgba("#767676"), tokens.rgba("#FFFFFF")), 4.5)

    def test_source_over_8bit_alpha(self):
        color = tokens.over(tokens.rgba("#FFFFFF80"), tokens.rgba("#000000"))
        self.assertAlmostEqual(color[0], 128 / 255, places=14)
        self.assertEqual(color[3], 1)
        expected = decimal_contrast("#808080", "#000000")
        self.assertAlmostEqual(tokens.ratio(color, tokens.rgba("#000000")), expected, places=11)

    def test_two_layer_alpha_order(self):
        p = {"tint": "#FFFFFF80", "scrim": "#00000080", "base": "#FFFFFF"}
        c = tokens.effective_background(p, {"background": "tint", "backdrop": ["scrim", "base"]})
        a = 128 / 255
        self.assertAlmostEqual(c[0], a + (1 - a) * (1 - a), places=14)

    def test_missing_alpha_backdrop_rejected(self):
        self.model["recipes"]["drop.marker.canvas"].pop("backdrop")
        with self.assertRaises(tokens.TokenError):
            tokens.validate(self.model)

    def test_required_state_cannot_be_removed(self):
        self.model["recipes"].pop("field.placeholder.invalid-focused")
        with self.assertRaises(tokens.TokenError):
            tokens.validate(self.model)

    def test_meaningful_data_cannot_use_disabled_exemption(self):
        self.model["recipes"]["row.disabled-selected"]["kind"] = "disabled"
        self.model["recipes"]["row.disabled-selected"]["exception"] = "Attempted exemption"
        with self.assertRaises(tokens.TokenError):
            tokens.validate(self.model)

    def test_focus_cannot_be_decorative(self):
        self.model["recipes"]["focus.selected"]["kind"] = "decorative"
        self.model["recipes"]["focus.selected"]["exception"] = "Attempted exemption"
        with self.assertRaises(tokens.TokenError):
            tokens.validate(self.model)

    def test_lower_contrast_policy_rejected(self):
        for key, value in [("text_minimum", 4.499), ("essential_minimum", 2.999)]:
            with self.subTest(key=key):
                altered = copy.deepcopy(self.model)
                altered["contrast_policy"][key] = value
                with self.assertRaises(tokens.TokenError):
                    tokens.validate(altered)

    def test_unknown_color_and_role_rejected(self):
        for path, value in [("color", "#GGGGGG"), ("role", "missing.raw")]:
            altered = copy.deepcopy(self.model)
            if path == "color":
                altered["raw"]["dark.text"] = value
            else:
                altered["roles"]["dark"]["text.primary"] = value
            with self.subTest(path=path), self.assertRaises(tokens.TokenError):
                tokens.validate(altered)

    def test_scientific_mapping_rejected(self):
        self.model["mappings"]["atlantafx"]["-color-chart-1"] = "action.default"
        with self.assertRaises(tokens.TokenError):
            tokens.validate(self.model)

    def test_unreferenced_scientific_raw_color_rejected(self):
        self.model["raw"]["data.series.a"] = "#FF0000"
        with self.assertRaises(tokens.TokenError):
            tokens.validate(self.model)

    def test_typography_floors_and_density_defaults(self):
        for key, value in [("metadata", 11), ("table", 12), ("general", 13)]:
            altered = copy.deepcopy(self.model)
            altered["typography"][key] = value
            with self.subTest(key=key), self.assertRaises(tokens.TokenError):
                tokens.validate(altered)
        self.model["densities"]["compact"]["control_min_height"] = 27
        with self.assertRaises(tokens.TokenError):
            tokens.validate(self.model)

    def test_nonfinite_and_boolean_metrics_rejected(self):
        for value in [float("nan"), float("inf"), True, -1]:
            altered = copy.deepcopy(self.model)
            altered["geometry"]["divider"] = value
            with self.subTest(value=value), self.assertRaises(tokens.TokenError):
                tokens.validate(altered)

    def test_fractional_metrics_are_not_rounded_in_java(self):
        self.model["geometry"]["control_radius"] = 6.125
        outputs = tokens.rendered_outputs(json.dumps(self.model).encode())
        java = outputs["modules/theme/src/main/java/meridian/theme/metrics/MeridianMetrics.java"].decode()
        self.assertIn("new Geometry(6.125,", java)
        self.assertIn("--meridian-control-radius: 6.125px;", outputs["design-study/generated/tokens.css"].decode())

    def test_nonfinite_and_boolean_contrast_policy_rejected(self):
        for key in ("text_minimum", "essential_minimum"):
            for value in (float("inf"), float("nan"), True):
                altered = copy.deepcopy(self.model)
                altered["contrast_policy"][key] = value
                with self.subTest(key=key, value=value), self.assertRaises(tokens.TokenError):
                    tokens.validate(altered)

    def test_invalid_color_does_not_generate(self):
        self.model["raw"]["light.text"] = "#FFFFFF"
        with self.assertRaisesRegex(tokens.TokenError, "Contrast failed"):
            tokens.rendered_outputs(json.dumps(self.model).encode())

    def test_duplicate_json_keys_rejected(self):
        data = self.data.decode().replace('"schema": "meridian.tokens.v1"',
                                          '"schema": "unexpected", "schema": "meridian.tokens.v1"', 1)
        with self.assertRaisesRegex(tokens.TokenError, "Duplicate JSON key"):
            tokens.rendered_outputs(data.encode())

    def test_two_materialized_generations_identical(self):
        expected = tokens.rendered_outputs(self.data)
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            tokens.write_outputs(root, expected)
            first = {name: (root / name).read_bytes() for name in expected}
            tokens.write_outputs(root, tokens.rendered_outputs(self.data))
            self.assertEqual(first, {name: (root / name).read_bytes() for name in expected})

    def test_manual_edit_missing_and_stale_source_detected(self):
        expected = tokens.rendered_outputs(self.data)
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            tokens.write_outputs(root, expected)
            target = root / "design-study/generated/tokens.css"
            target.write_bytes(target.read_bytes() + b"\n/* hand edit */\n")
            self.assertTrue(any("stale/edited" in p for p in tokens.check_outputs(root, expected)))
            target.unlink()
            self.assertTrue(any("missing:" in p for p in tokens.check_outputs(root, expected)))
            tokens.write_outputs(root, expected)
            self.model["motion"]["hover_ms"] += 1
            changed = tokens.rendered_outputs(json.dumps(self.model).encode())
            self.assertTrue(tokens.check_outputs(root, changed))

    def test_unexpected_generated_output_detected(self):
        expected = tokens.rendered_outputs(self.data)
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            tokens.write_outputs(root, expected)
            (root / "tokens/generated/obsolete.css").write_text("obsolete")
            self.assertTrue(any("unexpected" in p for p in tokens.check_outputs(root, expected)))

if __name__ == "__main__":
    unittest.main()
