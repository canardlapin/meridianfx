#!/usr/bin/env python3
"""Validate and deterministically generate Meridian's UI token resources (stdlib only)."""
import argparse
import hashlib
import json
import math
import os
from pathlib import Path
import re
import sys
import tempfile

ROOT = Path(__file__).resolve().parent.parent
SOURCE = "tokens/meridian.json"
MODES = ("dark", "light")
KINDS = {"text", "essential", "disabled", "decorative"}
ROLE_NAMES = set("""
surface.page surface.shell surface.canvas surface.raised state.hover state.pressed
selection.active selection.inactive selection.marker selection.inactive-marker
text.primary text.secondary text.placeholder text.on-action text.accent
action.default action.hover action.pressed border.decorative border.control border.invalid
focus.ring control.indicator status.success status.warning status.danger status.neutral status.progress
disabled.foreground overlay.shadow drop.fill drop.scrim drop.slot-fill
status.success-emphasis status.warning-emphasis status.danger-emphasis
status.success-subtle status.warning-subtle status.danger-subtle
status.success-muted status.warning-muted status.danger-muted
""".split())
DOCK_NAMES = set("""
-dock-base -dock-surface -dock-surface-raised -dock-surface-veil -dock-outline
-dock-accent -dock-accent-soft -dock-text -dock-text-strong -dock-hover -dock-pressed
-dock-shadow -dock-selected-bar -dock-drop-scrim -dock-slot-fill
""".split())
TYPOGRAPHY = ("general", "table", "metadata", "section", "title")
GEOMETRY = ("control_radius", "popup_radius", "dialog_radius", "divider", "resize_hit_reach", "icon_size")
DENSITY = ("control_min_height", "table_row_min_height", "header_min_height", "icon_target_min_size", "horizontal_padding", "vertical_padding")
MOTION = ("hover_ms", "overlay_ms", "panel_ms")
TOP = {"schema", "provenance", "raw", "roles", "recipes", "mappings", "typography",
       "geometry", "densities", "motion", "contrast_policy", "exclusions"}
DECORATIVE = {"separator.decorative", "shadow.decorative", "drop.fill.decorative"}
DISABLED = {"button.disabled", "field.disabled", "menu.disabled"}
REQUIRED_RECIPES = {
    **{f"body.{role}.{state}": "text" for role in ("primary", "secondary", "accent")
       for state in ("canvas", "shell", "raised", "hover", "pressed", "selected", "inactive-selected")},
    **{f"focus.{state}": "essential" for state in ("canvas", "shell", "raised", "hover", "pressed", "selected", "inactive-selected")},
    **{f"field.{role}.{state}": ("essential" if role == "boundary" else "text")
       for role in ("text", "placeholder", "boundary")
       for state in ("default", "hover", "pressed", "focused", "invalid", "invalid-focused", "read-only")},
    **{f"button.{role}.{state}": ("essential" if role == "secondary-boundary" else "text")
       for role in ("primary", "secondary", "secondary-boundary")
       for state in ("default", "hover", "pressed", "focused")},
    **{f"row.{state}": "text" for state in ("default", "hover", "selected", "selected-focused", "inactive-selected", "disabled-selected")},
    **{f"row.marker.{state}": "essential" for state in ("selected", "selected-focused", "inactive-selected", "disabled-selected")},
    **{f"{control}.selection-cue.{state}.{adjacent}": "essential"
       for control in ("list", "tree") for state in ("active", "inactive") for adjacent in ("cell", "fill")},
    **{f"{group}.{state}": "text" for group in ("dock.tab", "menu")
       for state in ("default", "hover", "selected", "focused", "inactive-selected")},
    **{f"status.{status}.{state}": "text" for status in ("neutral", "progress", "success", "warning", "danger")
       for state in ("canvas", "shell", "raised", "hover", "pressed", "selected", "inactive-selected")},
    **{f"status.{status}.dot.{surface}": "essential" for status in ("neutral", "progress")
       for surface in ("canvas", "shell", "raised")},
    **{f"control.indicator.{state}": "essential" for state in ("canvas", "shell", "raised", "hover", "pressed", "selected", "inactive-selected")},
    **{name: "decorative" for name in DECORATIVE},
    **{name: "disabled" for name in DISABLED},
    "drop.marker.canvas": "essential", "drop.marker.raised": "essential", "drop.slot.marker": "essential",
}

class TokenError(ValueError):
    pass

def require(condition, message):
    if not condition:
        raise TokenError(message)

def exact_keys(obj, keys, where):
    require(isinstance(obj, dict) and set(obj) == set(keys), f"{where}: expected keys {sorted(keys)}")

def number(value, where, minimum=0, integer=False):
    require(type(value) in (int, float) and math.isfinite(value) and value >= minimum,
            f"{where}: finite number >= {minimum} required")
    if integer:
        require(type(value) is int, f"{where}: integer required")

def rgba(value):
    require(isinstance(value, str) and re.fullmatch(r"#[0-9A-Fa-f]{6}(?:[0-9A-Fa-f]{2})?", value),
            f"Invalid sRGB hex color: {value!r}")
    channels = tuple(int(value[n:n + 2], 16) / 255 for n in (1, 3, 5))
    return channels + ((int(value[7:9], 16) / 255 if len(value) == 9 else 1.0),)

def over(foreground, background):
    alpha = foreground[3] + background[3] * (1 - foreground[3])
    require(alpha > 0, "Cannot flatten fully transparent layers")
    rgb = tuple((foreground[n] * foreground[3] + background[n] * background[3] *
                 (1 - foreground[3])) / alpha for n in range(3))
    return rgb + (alpha,)

def luminance(color):
    require(abs(color[3] - 1) < 1e-12, "Contrast requires an opaque effective color")
    linear = [v / 12.92 if v <= 0.04045 else ((v + 0.055) / 1.055) ** 2.4 for v in color[:3]]
    return sum(v * w for v, w in zip(linear, (0.2126, 0.7152, 0.0722)))

def ratio(foreground, background):
    a, b = luminance(foreground), luminance(background)
    return (max(a, b) + 0.05) / (min(a, b) + 0.05)

def colors(model, mode):
    return {role: model["raw"][raw].upper() for role, raw in model["roles"][mode].items()}

def background_layers(case):
    backdrop = case.get("backdrop", [])
    return [case["background"]] + ([backdrop] if isinstance(backdrop, str) else backdrop)

def effective_background(palette, case):
    layers = background_layers(case)
    require(abs(rgba(palette[layers[-1]])[3] - 1) < 1e-12,
            "Transparent background requires an explicit opaque backdrop")
    result = rgba(palette[layers[-1]])
    for role in reversed(layers[:-1]):
        result = over(rgba(palette[role]), result)
    return result

def validate(model):
    exact_keys(model, TOP, "source")
    require(model["schema"] == "meridian.tokens.v1", "Unsupported token schema")
    exact_keys(model["roles"], MODES, "roles")
    require(isinstance(model["raw"], dict) and model["raw"], "raw: colors required")
    for value in model["raw"].values():
        rgba(value)
    used = set()
    for mode in MODES:
        exact_keys(model["roles"][mode], ROLE_NAMES, f"roles.{mode}")
        for role, raw in model["roles"][mode].items():
            require(raw in model["raw"], f"{mode}.{role}: unknown raw color {raw}")
            used.add(raw)
    for mode in MODES:
        palette = colors(model, mode)
        interactive = {value for role, value in palette.items() if role.startswith(("selection.", "focus."))}
        require(palette["status.neutral"] != palette["status.progress"] and
                all(palette["status." + status] not in interactive for status in ("neutral", "progress")),
                "Status roles must be distinct from one another and selection/focus colors")
    require(set(model["raw"]) == used, "Unreferenced raw colors are forbidden, including scientific palette inputs")
    exact_keys(model["mappings"], ("atlantafx", "scaladock", "browser"), "mappings")
    exact_keys(model["mappings"]["scaladock"], DOCK_NAMES, "ScalaDock public token mapping")
    require(model["mappings"]["atlantafx"], "AtlantaFX semantic mapping required")
    for group, mapping in model["mappings"].items():
        require(isinstance(mapping, dict), f"{group}: mapping required")
        for key, role in mapping.items():
            prefix = "-color-" if group == "atlantafx" else "-dock-" if group == "scaladock" else "--"
            require(key.startswith(prefix) and re.fullmatch(r"--?[a-z][a-z0-9-]*", key), f"Invalid CSS mapping: {key}")
            require(role in ROLE_NAMES, f"{group}.{key}: unknown semantic role")
            require(not any(part in key for part in ("chart", "data-series", "scientific", "lut", "threshold")),
                    "Scientific/general chart palettes are outside UI tokens")
    exact_keys(model["typography"], TYPOGRAPHY, "typography")
    for key, value in model["typography"].items():
        number(value, f"typography.{key}", 12)
    require(model["typography"]["general"] >= 14 and model["typography"]["table"] >= 13,
            "General/table typography floors are 14/13 logical px")
    exact_keys(model["geometry"], GEOMETRY, "geometry")
    for key, value in model["geometry"].items():
        number(value, f"geometry.{key}", 0.01)
    exact_keys(model["densities"], ("comfortable", "compact"), "densities")
    for mode, profile in model["densities"].items():
        exact_keys(profile, DENSITY, f"density.{mode}")
        for key, value in profile.items():
            number(value, f"density.{mode}.{key}", 0.01)
        require(profile["icon_target_min_size"] >= model["geometry"]["icon_size"],
                "Interactive icon target must be at least its visible icon size")
    for mode, controls, rows in (("comfortable", 32, 40), ("compact", 28, 32)):
        require(model["densities"][mode]["control_min_height"] == controls and
                model["densities"][mode]["table_row_min_height"] == rows,
                "v1 density defaults must preserve 32/28 controls and 40/32 rows")
    exact_keys(model["motion"], MOTION, "motion")
    for key, value in model["motion"].items():
        number(value, f"motion.{key}", 0, integer=True)
    policy = model["contrast_policy"]
    exact_keys(policy, ("text_minimum", "essential_minimum", "method", "ordinary_text_includes",
                        "disabled_exception", "decorative_exception"), "contrast_policy")
    number(policy["text_minimum"], "contrast_policy.text_minimum", 4.5)
    number(policy["essential_minimum"], "contrast_policy.essential_minimum", 3)
    require(policy["text_minimum"] >= 4.5 and policy["essential_minimum"] >= 3,
            "Contrast minima cannot be weakened below 4.5/3.0")
    require(isinstance(model["exclusions"], dict) and
            all(isinstance(model["exclusions"].get(k), str) and model["exclusions"][k].strip()
                for k in ("scientific_colors", "support_claim")), "Scientific policy and qualification limits required")
    require(isinstance(model["recipes"], dict) and set(REQUIRED_RECIPES) <= set(model["recipes"]),
            "Required primitive state/contrast contract is incomplete")
    for name, case in model["recipes"].items():
        require(isinstance(case, dict) and {"kind", "foreground", "background"} <= set(case)
                and set(case) <= {"kind", "foreground", "background", "backdrop", "exception"},
                f"{name}: invalid recipe fields")
        require(case["kind"] in KINDS, f"{name}: unknown contrast kind")
        if name in REQUIRED_RECIPES:
            require(case["kind"] == REQUIRED_RECIPES[name], f"{name}: required contrast class cannot change")
        if case["kind"] == "decorative":
            require(name in DECORATIVE, f"{name}: decorative exemption is not authorized")
        if case["kind"] == "disabled":
            require(name in DISABLED and case["foreground"] == "disabled.foreground",
                    f"{name}: disabled exemption is not authorized for meaningful data")
        exempt = case["kind"] in ("decorative", "disabled")
        require(("exception" in case) == exempt, f"{name}: exemption must be explicit and scoped")
        if exempt:
            require(isinstance(case["exception"], str) and case["exception"].strip(), f"{name}: missing exemption rationale")
        require(case["foreground"] in ROLE_NAMES, f"{name}: unknown foreground")
        require(isinstance(case.get("backdrop", []), (str, list)), f"{name}: invalid backdrop")
        for role in background_layers(case):
            require(isinstance(role, str) and role in ROLE_NAMES, f"{name}: unknown background/backdrop")
        for mode in MODES:
            effective_background(colors(model, mode), case)
    return model

def contrast_report(model):
    result = []
    for mode in MODES:
        palette = colors(model, mode)
        for name, case in sorted(model["recipes"].items()):
            bg = effective_background(palette, case)
            fg = over(rgba(palette[case["foreground"]]), bg)
            value = ratio(fg, bg)
            minimum = (model["contrast_policy"]["text_minimum"] if case["kind"] == "text"
                       else model["contrast_policy"]["essential_minimum"] if case["kind"] == "essential" else None)
            result.append({
                "appearance": mode, "recipe": name, "kind": case["kind"],
                "foreground_role": case["foreground"], "foreground_srgb": palette[case["foreground"]],
                "background_layers": [{"role": role, "srgb": palette[role]} for role in background_layers(case)],
                "effective_foreground_rgba": list(fg), "effective_background_rgba": list(bg),
                "ratio": value, "minimum": minimum,
                "passed": value >= minimum if minimum is not None else None,
                "exception": case.get("exception"),
            })
    required = [row for row in result if row["minimum"] is not None]
    return {"schema": "meridian.contrast.v1", "required": len(required),
            "passed": sum(row["passed"] for row in required),
            "exempt": len(result) - len(required), "pairs": result}

def camel(name):
    first, *parts = name.split("_")
    return first + "".join(part.title() for part in parts)

def java_number(value):
    return repr(float(value))

def java_metrics(model, sha):
    def record(name, keys, grow=False):
        fields = ", ".join(f"double {camel(key)}" for key in keys)
        values = ", ".join(camel(key) for key in keys)
        result = [f"    public record {name}({fields}) {{",
                  f"        public {name} {{",
                  f"            for (double value : new double[] {{{values}}}) positive(value);"]
        if name == "Typography":
            result.append('            if (general < 14 || table < 13 || metadata < 12 || section < 12 || title < 12) throw new IllegalArgumentException("Typography floor");')
        result += ["        }"]
        if grow:
            changes = [camel(key) + " * scale" if name == "Typography" or key in
                       ("control_min_height", "table_row_min_height", "header_min_height", "icon_target_min_size")
                       else camel(key) for key in keys]
            result += [f"        public {name} scaledForText(double scale) {{",
                       "            textScale(scale);",
                       f"            return new {name}({', '.join(changes)});",
                       "        }"]
        if name == "DensityMetrics":
            for method, field in (("controlHeightFor", "controlMinHeight"), ("tableRowHeightFor", "tableRowMinHeight"), ("headerHeightFor", "headerMinHeight")):
                result += [f"        public double {method}(double measuredContentHeight) {{",
                           "            nonnegative(measuredContentHeight);",
                           f"            return Math.max({field}, measuredContentHeight);",
                           "        }"]
        result += ["    }"]
        return "\n".join(result)
    lines = ["package meridian.theme.metrics;", "",
             f"/** Generated from {SOURCE}; source SHA-256 {sha}. Logical pixels, never hard ceilings. */",
             "public final class MeridianMetrics {",
             "    private MeridianMetrics() {}",
             "    public enum DensityMode { COMFORTABLE, COMPACT }",
             record("Typography", TYPOGRAPHY, True),
             record("Geometry", GEOMETRY),
             record("DensityMetrics", DENSITY, True),
             "    public record Motion(int hoverMs, int overlayMs, int panelMs) {",
             "        public Motion {",
             "            if (hoverMs < 0 || overlayMs < 0 || panelMs < 0) throw new IllegalArgumentException(\"Negative duration\");",
             "        }",
             "        public Motion reduced() { return new Motion(0, 0, 0); }",
             "    }"]
    for name, cls, values, keys in [
        ("TYPOGRAPHY", "Typography", model["typography"], TYPOGRAPHY),
        ("GEOMETRY", "Geometry", model["geometry"], GEOMETRY),
        ("COMFORTABLE", "DensityMetrics", model["densities"]["comfortable"], DENSITY),
        ("COMPACT", "DensityMetrics", model["densities"]["compact"], DENSITY),
    ]:
        lines.append(f"    public static final {cls} {name} = new {cls}({', '.join(java_number(values[k]) for k in keys)});")
    lines += [
        f"    public static final Motion MOTION = new Motion({', '.join(str(model['motion'][k]) for k in MOTION)});",
        "    public static DensityMetrics density(DensityMode mode) {",
        "        return switch (java.util.Objects.requireNonNull(mode, \"density\")) {",
        "            case COMFORTABLE -> COMFORTABLE;",
        "            case COMPACT -> COMPACT;",
        "        };",
        "    }",
        "    private static void positive(double value) {",
        "        if (!Double.isFinite(value) || value <= 0) throw new IllegalArgumentException(\"Expected finite positive metric\");",
        "    }",
        "    private static void nonnegative(double value) {",
        "        if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException(\"Expected finite nonnegative height\");",
        "    }",
        "    private static void textScale(double value) {",
        "        if (!Double.isFinite(value) || value < 1) throw new IllegalArgumentException(\"Text scale must be at least 1\");",
        "    }", "}", "",
    ]
    return "\n".join(lines)

def json_bytes(value):
    return (json.dumps(value, indent=2, sort_keys=True, allow_nan=False) + "\n").encode()

def unique_object(pairs):
    result = {}
    for key, value in pairs:
        require(key not in result, f"Duplicate JSON key: {key}")
        result[key] = value
    return result

def rendered_outputs(source_bytes):
    try:
        model = json.loads(source_bytes, object_pairs_hook=unique_object, parse_constant=lambda value: (_ for _ in ()).throw(TokenError(f"Non-finite JSON: {value}")))
    except (json.JSONDecodeError, UnicodeDecodeError) as error:
        raise TokenError(f"Invalid JSON: {error}") from error
    validate(model)
    report = contrast_report(model)
    failed = [row for row in report["pairs"] if row["passed"] is False]
    require(not failed, "Contrast failed: " + "; ".join(f"{r['appearance']} {r['recipe']}={r['ratio']:.6f}<{r['minimum']}" for r in failed))
    sha = hashlib.sha256(source_bytes).hexdigest()
    outputs = {}
    browser = [f"/* Generated from {SOURCE}; SHA-256 {sha}. Do not edit. */",
               ":root {",
               "  --meridian-weight-normal: 400;",
               "  --meridian-weight-strong: 700;"]
    for key, value in sorted(model["typography"].items()):
        browser.append(f"  --meridian-font-{key}: {value}px;")
    for key, value in sorted(model["geometry"].items()):
        browser.append(f"  --meridian-{key.replace('_', '-')}: {value}px;")
    for key, value in sorted(model["motion"].items()):
        browser.append(f"  --meridian-{key.replace('_', '-')}: {value}ms;")
    browser.append("}")
    expected_native = []
    normalized = {"schema": "meridian.resolved-tokens.v1", "source_sha256": sha,
                  "appearances": {}, **{k: model[k] for k in ("typography", "geometry", "densities", "motion", "recipes", "exclusions")}}
    for mode in MODES:
        palette = colors(model, mode)
        normalized["appearances"][mode] = palette
        header = f"/* Generated from {SOURCE}; SHA-256 {sha}. Do not edit. */"
        native = [header, ".root {"]
        for role, value in sorted(palette.items()):
            key = "-meridian-" + role.replace(".", "-")
            native.append(f"  {key}: {value};")
            expected_native.append(f"theme\t{mode}\t{key}\t{value}")
        for key, role in sorted(model["mappings"]["atlantafx"].items()):
            native.append(f"  {key}: -meridian-{role.replace('.', '-')};")
            expected_native.append(f"theme\t{mode}\t{key}\t{palette[role]}")
        native += ["}", ""]
        outputs[f"modules/theme/src/main/resources/meridian/theme/tokens-{mode}.css"] = "\n".join(native).encode()
        dock = [header, ".dock {"]
        for key, role in sorted(model["mappings"]["scaladock"].items()):
            dock.append(f"  {key}: {palette[role]};")
            expected_native.append(f"dock\t{mode}\t{key}\t{palette[role]}")
        dock += ["}", ""]
        outputs[f"modules/scaladock/src/main/resources/meridian/scaladock/tokens-{mode}.css"] = "\n".join(dock).encode()
        browser += [(':root,' if mode == "dark" else "") + f':root[data-theme="{mode}"] {{']
        for role, value in sorted(palette.items()):
            browser.append(f"  --meridian-{role.replace('.', '-')}: {value};")
        for key, role in sorted(model["mappings"]["browser"].items()):
            browser.append(f"  {key}: var(--meridian-{role.replace('.', '-')});")
        browser += [f"  color-scheme: {mode};", "}"]
        scss = [header, "$roles: ("]
        scss += [f'  "{role}": {value},' for role, value in sorted(palette.items())]
        scss += [");", "$atlantafx: ("]
        scss += [f'  "{key}": {palette[role]},' for key, role in sorted(model["mappings"]["atlantafx"].items())]
        scss += [");", ""]
        outputs[f"tokens/generated/_palette-{mode}.scss"] = "\n".join(scss).encode()
    for mode, metrics in sorted(model["densities"].items()):
        browser.append((':root,' if mode == "comfortable" else "") + f':root[data-density="{mode}"] {{')
        for key, value in sorted(metrics.items()):
            browser.append(f"  --meridian-{key.replace('_', '-')}: {value}px;")
        browser += ["  --control: var(--meridian-control-min-height);",
                    "  --row: var(--meridian-table-row-min-height);", "}"]
    browser += ["@media (prefers-reduced-motion: reduce) {",
                "  :root { --meridian-hover-ms: 0ms; --meridian-overlay-ms: 0ms; --meridian-panel-ms: 0ms; }", "}", ""]
    outputs["design-study/generated/tokens.css"] = "\n".join(browser).encode()
    outputs["modules/theme/src/main/java/meridian/theme/metrics/MeridianMetrics.java"] = java_metrics(model, sha).encode()
    outputs["modules/theme/src/main/resources/meridian/theme/tokens.json"] = json_bytes(normalized)
    outputs["tokens/generated/contrast.json"] = json_bytes({"source_sha256": sha, **report})
    outputs["tokens/generated/native-expected.tsv"] = ("\n".join(sorted(expected_native)) + "\n").encode()
    manifest = {"schema": "meridian.generation.v1", "source": SOURCE, "source_sha256": sha,
                "outputs": {key: hashlib.sha256(value).hexdigest() for key, value in sorted(outputs.items())},
                "scientific_colors": "excluded", "timestamps": "intentionally absent"}
    outputs["tokens/generated/manifest.json"] = json_bytes(manifest)
    return outputs

def check_outputs(root, outputs):
    problems = []
    for relative, content in outputs.items():
        path = root / relative
        if not path.is_file():
            problems.append(f"missing: {relative}")
        elif path.read_bytes() != content:
            problems.append(f"stale/edited: {relative}")
    generated = root / "tokens/generated"
    if generated.exists():
        expected = {path for path in outputs if path.startswith("tokens/generated/")}
        actual = {str(path.relative_to(root)) for path in generated.rglob("*") if path.is_file()}
        problems += ["unexpected generated output: " + path for path in sorted(actual - expected)]
    return problems

def write_outputs(root, outputs):
    for relative, content in sorted(outputs.items()):
        path = root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        if path.is_file() and path.read_bytes() == content:
            continue
        temporary = None
        try:
            with tempfile.NamedTemporaryFile(dir=path.parent, prefix="." + path.name + ".", delete=False) as stream:
                temporary = Path(stream.name)
                stream.write(content)
            os.replace(temporary, path)
        finally:
            if temporary is not None and temporary.exists():
                temporary.unlink()

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Fail on missing, edited or stale outputs; do not write")
    parser.add_argument("--root", type=Path, default=ROOT, help="Output/source root (supports isolated validation)")
    args = parser.parse_args()
    try:
        root = args.root.resolve()
        outputs = rendered_outputs((root / SOURCE).read_bytes())
        if args.check:
            problems = check_outputs(root, outputs)
            require(not problems, "\n".join(problems))
        else:
            write_outputs(root, outputs)
            problems = check_outputs(root, outputs)
            require(not problems, "\n".join(problems))
        report = json.loads(outputs["tokens/generated/contrast.json"])
        print(json.dumps({"outputs": len(outputs), "source_sha256": report["source_sha256"],
                          "contrast_required": report["required"], "contrast_passed": report["passed"],
                          "explicit_exceptions": report["exempt"], "mode": "check" if args.check else "generate"}))
    except (TokenError, OSError, KeyError, TypeError) as error:
        print(str(error), file=sys.stderr)
        return 1
    return 0

if __name__ == "__main__":
    sys.exit(main())
