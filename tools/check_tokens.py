#!/usr/bin/env python3
"""Check A2 generation determinism, coverage, contrast and optional native observations."""
import argparse
import hashlib
import json
from pathlib import Path
import sys
import tempfile
import generate_tokens as tokens

ROOT = Path(__file__).resolve().parent.parent

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--native-report", type=Path)
    parser.add_argument("--report", type=Path, help="Write an acceptance summary (does not change generated files)")
    args = parser.parse_args()
    try:
        root = args.root.resolve()
        data = (root / tokens.SOURCE).read_bytes()
        outputs = tokens.rendered_outputs(data)
        tokens.require(not tokens.check_outputs(root, outputs), "\n".join(tokens.check_outputs(root, outputs)))
        # Compare materialized results from two independent write passes in an isolated directory.
        with tempfile.TemporaryDirectory(prefix="meridian-a2-") as directory:
            isolated = Path(directory)
            tokens.write_outputs(isolated, outputs)
            first = {name: (isolated / name).read_bytes() for name in outputs}
            tokens.write_outputs(isolated, tokens.rendered_outputs(data))
            second = {name: (isolated / name).read_bytes() for name in outputs}
            tokens.require(first == second == outputs, "Generation is not byte-deterministic")
            target = isolated / "modules/theme/src/main/resources/meridian/theme/tokens-dark.css"
            original = target.read_bytes()
            target.write_bytes(original + b"/* unauthorized manual edit */\n")
            tokens.require(any("stale/edited" in problem for problem in tokens.check_outputs(isolated, outputs)),
                           "Staleness checker accepted a manual edit")
            target.write_bytes(original)
            target.unlink()
            tokens.require(any("missing:" in problem for problem in tokens.check_outputs(isolated, outputs)),
                           "Staleness checker accepted a missing output")
        contrast = json.loads(outputs["tokens/generated/contrast.json"])
        manifest = json.loads(outputs["tokens/generated/manifest.json"])
        summary = {
            "schema": "meridian.a2-acceptance.v1", "source_sha256": manifest["source_sha256"],
            "generated_outputs": len(outputs), "determinism": "two materialized passes identical",
            "manual_edit_detection": "passed", "missing_output_detection": "passed",
            "contrast_required": contrast["required"], "contrast_passed": contrast["passed"],
            "explicit_exceptions": contrast["exempt"],
            "generated_hashes": {name: hashlib.sha256(content).hexdigest() for name, content in sorted(outputs.items())},
            "scientific_colors": "excluded",
        }
        if args.native_report:
            expected = {}
            for line in outputs["tokens/generated/native-expected.tsv"].decode().splitlines():
                scope, appearance, key, color = line.split("\t")
                expected[(scope, appearance, key)] = tokens.rgba(color)
            observed = {}
            for line in args.native_report.read_text().splitlines():
                scope, appearance, key, red, green, blue, alpha = line.split("\t")
                identity = (scope, appearance, key)
                tokens.require(identity not in observed, f"Duplicate native observation: {identity}")
                observed[identity] = tuple(float(v) for v in (red, green, blue, alpha))
            tokens.require(set(observed) == set(expected), "Native observation coverage mismatch")
            for identity, actual in observed.items():
                tokens.require(all(abs(a - b) < 1e-7 for a, b in zip(actual, expected[identity])),
                               f"Native RGBA mismatch: {identity} {actual} != {expected[identity]}")
            summary["native_colors"] = {"observations": len(observed), "appearances": ["dark", "light"],
                                        "semantic_and_upstream_aliases": "matched", "tolerance": 1e-7}
        if args.report:
            args.report.parent.mkdir(parents=True, exist_ok=True)
            args.report.write_text(json.dumps(summary, indent=2, sort_keys=True) + "\n")
        print(json.dumps({k: v for k, v in summary.items() if k != "generated_hashes"}, indent=2))
        return 0
    except (tokens.TokenError, OSError, ValueError) as error:
        print(str(error), file=sys.stderr)
        return 1

if __name__ == "__main__":
    sys.exit(main())
