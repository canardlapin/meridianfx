#!/usr/bin/env python3
"""Compile pinned AtlantaFX SASS plus a reviewable Meridian delta; stdlib orchestration."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import generate_tokens as tokens

ROOT = Path(__file__).resolve().parent.parent
OUTPUT = "modules/theme/src/main/resources/meridian/theme/themes"
SASS_VERSION = "1.93.2"
COLOR_VARS = {
    "fg-default": "text.primary",
    "fg-muted": "text.secondary",
    "fg-subtle": "text.placeholder",
    "fg-onEmphasis": "text.on-action",
    "canvas-default": "surface.canvas",
    "canvas-overlay": "surface.raised",
    "canvas-inset": "surface.shell",
    "canvas-subtle": "surface.shell",
    "border-default": "border.control",
    "border-muted": "border.decorative",
    "border-subtle": "border.decorative",
    "shadow-default": "overlay.shadow",
    "accent-fg": "text.accent",
    "accent-emphasis": "action.default",
    "accent-muted": "selection.active",
    "accent-subtle": "selection.active",
    "success-fg": "status.success",
    "warning-fg": "status.warning",
    "danger-fg": "status.danger",
}
BUTTON = {
    "color-bg": "-meridian-surface-raised",
    "color-bg-hover": "-meridian-state-hover",
    "color-bg-focused": "-meridian-surface-raised",
    "color-border-focused": "-meridian-focus-ring",
    "color-bg-pressed": "-meridian-state-pressed",
    "color-border-pressed": "-meridian-border-control",
    "color-accent-bg-hover": "-meridian-action-hover",
    "color-accent-border-hover": "-meridian-action-hover",
    "color-accent-bg-focused": "-meridian-action-default",
    "color-accent-border-focused": "-meridian-action-default",
    "color-accent-bg-pressed": "-meridian-action-pressed",
    "color-accent-border-pressed": "-meridian-action-pressed",
    "opacity-hover": "1",
}
DATA = {
    "color-cell-bg-selected": "-meridian-selection-inactive",
    "color-cell-bg-selected-focused": "-meridian-selection-active",
    "color-cell-border": "-meridian-border-decorative",
}


def digest(data):
    return hashlib.sha256(data).hexdigest()


def forward(module, values):
    return (
        '@use "'
        + module
        + '" as '
        + module.split("/")[-1].replace("-", "_")
        + " with (\n"
        + ",\n".join(f"  ${key}: {value}" for key, value in values.items())
        + "\n);\n"
    )


def metrics_source(model, density):
    t, g, d = model["typography"], model["geometry"], model["densities"][density]
    values = {
        **{k: f"{v}px" for k, v in t.items()},
        "control": f'{d["control_min_height"]}px',
        "row": f'{d["table_row_min_height"]}px',
        "header": f'{d["header_min_height"]}px',
        "padding-x": f'{d["horizontal_padding"]}px',
        "padding-y": f'{d["vertical_padding"]}px',
        "popup-radius": f'{g["popup_radius"]}px',
        "dialog-radius": f'{g["dialog_radius"]}px',
        "hover-ms": f'{model["motion"]["hover_ms"]}ms',
    }
    return "".join(f"${key}: {value};\n" for key, value in values.items())


def entry_source(model, mode, density):
    p = tokens.colors(model, mode)
    d, t, g = model["densities"][density], model["typography"], model["geometry"]
    entry = forward(
        "settings/color-vars", {key: p[role] for key, role in COLOR_VARS.items()}
    )
    entry += forward(
        "settings/config",
        {
            "darkMode": str(mode == "dark").lower(),
            "font-default": f'{t["general"]}px',
            "font-small": f'{t["metadata"]/t["general"]}em',
            "padding-x": f'{d["horizontal_padding"]}px',
            "padding-y": f'{d["vertical_padding"]}px',
            "border-radius": f'{g["control_radius"]}px',
            "popup-radius": f'{g["popup_radius"]}px',
            "icon-size": f'{g["icon_size"]}px',
        },
    )
    entry += '@use "general";\n'
    entry += forward("components/button", BUTTON)
    entry += forward(
        "components/text-input",
        {
            "color-border-focused": "-meridian-focus-ring",
            "color-fg-danger": "-meridian-text-primary",
            "color-border-danger": "-meridian-border-invalid",
        },
    )
    entry += forward("components/data", DATA)
    entry += forward(
        "components/menu",
        {
            "color-menuitem-bg": "-meridian-surface-raised",
            "color-menuitem-bg-hover": "-meridian-state-hover",
            "color-menubar-bg-hover": "-meridian-state-hover",
        },
    )
    entry += '@use "components";\n@use "recipes";\n'
    entry += ".root {\n" + "".join(
        f'  -meridian-{k.replace(".", "-")}: {v};\n' for k, v in sorted(p.items())
    )
    # Bind every mapped AtlantaFX lookup to its Meridian role so no upstream (Primer) ramp value survives.
    entry += (
        "".join(
            f'  {k}: -meridian-{r.replace(".", "-")};\n'
            for k, r in sorted(model["mappings"]["atlantafx"].items())
        )
        + "}\n"
    )
    return entry


def manifest(css):
    modules, opened, variables = {}, {}, {}
    for i, line in enumerate(css.splitlines()):
        hit = re.search(r"@module:([a-z-]+):(start|end)", line)
        if hit:
            name, boundary = hit.groups()
            if boundary == "start":
                opened[name] = i
            else:
                modules[name] = (opened.pop(name), i)
        hit = re.search(r"@var:([^=]+)=(.*?)\s*\*/", line)
        if hit:
            variables[hit[1]] = hit[2]
    tokens.require(
        not opened
        and {
            "root",
            "text",
            "button",
            "text-input",
            "data",
            "menu",
            "dialog",
            "meridian",
        }
        <= set(modules),
        "Compiled theme lacks required AtlantaFX modules",
    )
    modules["meridian"] = (modules["meridian"][0], len(css.splitlines()) - 1)
    return (
        "[modules]\n"
        + "".join(
            f"{k}={a}:{b}\n"
            for k, (a, b) in sorted(modules.items(), key=lambda x: x[1])
        )
        + "\n[variables]\n"
        + "".join(f"{k}={v}\n" for k, v in sorted(variables.items()))
    ).encode()


def compile_outputs(root):
    source = (root / tokens.SOURCE).read_bytes()
    model = tokens.validate(json.loads(source, object_pairs_hook=tokens.unique_object))
    tokens.rendered_outputs(source)
    lock = json.loads((root / "upstream/lock.json").read_text())
    for path, sha in lock["source_files"].items():
        tokens.require(
            digest((root / path).read_bytes()) == sha,
            "Locked upstream changed: " + path,
        )
    sass = root / "theme-build/node_modules/sass/sass.js"
    tokens.require(
        sass.is_file(), "Run npm ci --prefix theme-build --ignore-scripts first"
    )
    version = subprocess.check_output(
        ["node", str(sass), "--version"], text=True
    ).split()[0]
    tokens.require(version == SASS_VERSION, "Unexpected Sass version: " + version)
    inputs = {
        tokens.SOURCE: digest(source),
        "upstream/lock.json": digest((root / "upstream/lock.json").read_bytes()),
    }
    for name in (
        "theme-build/_recipes.scss",
        "theme-build/package.json",
        "theme-build/package-lock.json",
        "tools/build_themes.py",
    ):
        inputs[name] = digest((root / name).read_bytes())
    outputs = {}
    with tempfile.TemporaryDirectory(prefix="meridian-themes-") as tmp:
        temp = Path(tmp)
        for mode in tokens.MODES:
            for density in ("comfortable", "compact"):
                (temp / "_metrics.scss").write_text(metrics_source(model, density))
                entry = temp / "theme.scss"
                entry.write_text(entry_source(model, mode, density))
                command = [
                    "node",
                    str(sass),
                    "--no-source-map",
                    "--style=expanded",
                    "--silence-deprecation=global-builtin,color-functions,import",
                    "--load-path=" + str(root / "upstream/atlantafx/styles/src"),
                    "--load-path=" + str(root / "theme-build"),
                    "--load-path=" + str(temp),
                    str(entry),
                ]
                result = subprocess.run(
                    command, text=True, capture_output=True, check=True
                )
                if result.stderr:
                    print(result.stderr, file=sys.stderr)
                css = result.stdout.replace("\r\n", "\n")
                css = (
                    f'/* MeridianFX; source SHA-256 {digest(source)}; AtlantaFX {lock["atlantafx"]["commit"]}; Sass {version}. */\n'
                    + css
                )
                name = f"{OUTPUT}/meridian-{mode}-{density}.css"
                outputs[name] = css.encode()
                outputs[name + ".manifest"] = manifest(css)
    outputs[OUTPUT + "/generation.json"] = tokens.json_bytes(
        {
            "schema": "meridian.themes.v1",
            "compiler": {"name": "dart-sass", "version": version},
            "inputs": inputs,
            "upstream_commit": lock["atlantafx"]["commit"],
            "outputs": {k: digest(v) for k, v in sorted(outputs.items())},
            "scientific_colors": "upstream generic chart defaults preserved identically; no UI-token scientific palette",
        }
    )
    return outputs


def output_problems(root, outputs):
    problems = []
    for relative, expected in outputs.items():
        path = root / relative
        if not path.is_file():
            problems.append("missing: " + relative)
        elif path.read_bytes() != expected:
            problems.append("stale/edited: " + relative)
    actual = {
        str(p.relative_to(root)) for p in (root / OUTPUT).rglob("*") if p.is_file()
    }
    problems += ["unexpected theme output: " + p for p in sorted(actual - set(outputs))]
    return problems


def verify_receipt(root):
    receipt = json.loads((root / OUTPUT / "generation.json").read_text())
    tokens.require(
        receipt["schema"] == "meridian.themes.v1"
        and receipt["compiler"] == {"name": "dart-sass", "version": SASS_VERSION},
        "Unexpected theme receipt",
    )
    for section in ("inputs", "outputs"):
        for name, sha in receipt[section].items():
            tokens.require(
                (root / name).is_file() and digest((root / name).read_bytes()) == sha,
                "Stale theme " + section + ": " + name,
            )
    expected = {
        f"{OUTPUT}/meridian-{mode}-{density}.css" + suffix
        for mode in tokens.MODES
        for density in ("comfortable", "compact")
        for suffix in ("", ".manifest")
    }
    tokens.require(set(receipt["outputs"]) == expected, "Incomplete theme receipt")
    actual = {
        str(p.relative_to(root)) for p in (root / OUTPUT).rglob("*") if p.is_file()
    }
    tokens.require(
        actual == expected | {OUTPUT + "/generation.json"}, "Unexpected theme outputs"
    )
    lock = json.loads((root / "upstream/lock.json").read_text())
    for name, sha in lock["source_files"].items():
        tokens.require(
            digest((root / name).read_bytes()) == sha,
            "Locked upstream changed: " + name,
        )
    return receipt


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    modes = parser.add_mutually_exclusive_group()
    modes.add_argument("--check", action="store_true")
    modes.add_argument(
        "--verify",
        action="store_true",
        help="Verify stored hashes without Node/Sass (normal sbt build)",
    )
    args = parser.parse_args()
    try:
        if args.verify:
            verify_receipt(ROOT)
            print("Meridian theme resource hashes verified (no Sass runtime needed)")
            return 0
        outputs = compile_outputs(ROOT)
        if args.check:
            tokens.require(
                not output_problems(ROOT, outputs),
                "\n".join(output_problems(ROOT, outputs)),
            )
        else:
            tokens.write_outputs(ROOT, outputs)
        print(
            json.dumps(
                {
                    "themes": 4,
                    "outputs": len(outputs),
                    "mode": "check" if args.check else "generate",
                    "source_sha256": digest((ROOT / tokens.SOURCE).read_bytes()),
                }
            )
        )
        return 0
    except (
        tokens.TokenError,
        OSError,
        KeyError,
        subprocess.CalledProcessError,
    ) as error:
        print(str(error), file=sys.stderr)
        if isinstance(error, subprocess.CalledProcessError) and error.stderr:
            print(error.stderr, file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
