#!/usr/bin/env python3
"""Verify B1 token coverage and bind a passing native adapter run to source digests."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess

from verify_provider import ROOT, digest, read_resolution, require, validate_resolution

EVIDENCE = ROOT / "docs/evidence/adapter"


def token_coverage(documentation, mappings, sheets, palettes):
    documented = set(re.findall(r"\| `(-dock-[a-z-]+)` \|", documentation))
    require(documented and set(mappings) == documented, "Provider token mapping is incomplete or stale")
    report = {}
    for mode in ("dark", "light"):
        actual = dict(re.findall(r"(-dock-[a-z-]+):\s*(#[0-9A-Fa-f]+);", sheets[mode]))
        require(set(actual) == documented, "Generated dock tokens are incomplete")
        for name, role in mappings.items():
            require(actual[name].upper() == palettes[mode][role].upper(), "Wrong generated token: " + name)
        report[mode] = {name: {"role": role, "value": actual[name]} for name, role in sorted(mappings.items())}
    return report


def validate_native(result):
    require(result["status"] == "pass" and result["java"].split(".")[0] == "25" and result["javafx"].split("+")[0] == "27", "Wrong native result/runtime")
    modules = result["modules"]
    require(len(modules) == 3 and {m["name"] for m in modules} == {"javafx.base", "javafx.graphics", "javafx.controls"} and all(m["version"] == "27" for m in modules), "Mixed/incomplete JavaFX graph")
    for key, expected in {"changes": 100, "rebinds": 20, "retainedWindows": 0, "subscriptions": 0, "registrations": 0, "built": 4, "disposed": 4}.items():
        require(result[key] == expected, "Native lifecycle mismatch: " + key)
    require(result["retiredWindows"] >= 20, "Missing floating-window lifecycle coverage")
    require(len(result["phases"]) == 4 and len({p["phase"] for p in result["phases"]}) == 4, "Incomplete appearance matrix")
    require(all(p["tokens"] == 15 and p["mainFloatMenuGhostPaints"] == p["motionDragContinuity"] == "pass" for p in result["phases"]), "Missing paint/drag evidence")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--capture", type=Path)
    parser.add_argument("--log", type=Path)
    parser.add_argument("--check", type=Path)
    args = parser.parse_args()
    if args.check:
        receipt = json.loads(args.check.read_text())
        for section in ("sources", "artifacts"):
            for name, expected in receipt[section].items():
                require(digest(ROOT / name) == expected, "Changed input: " + name)
        print("B1 source/evidence hashes verified; no native rerun claimed")
        return
    resolution = read_resolution(EVIDENCE / "provider-resolution.tsv")
    pin = json.loads((ROOT / "upstream/lock.json").read_text())["scaladock"]["commit"]
    validate_resolution(resolution, pin)
    provider = Path(resolution["checkout"])
    model = json.loads((ROOT / "tokens/meridian.json").read_text())
    palettes = json.loads((ROOT / "modules/theme/src/main/resources/meridian/theme/tokens.json").read_text())["appearances"]
    sheets = {mode: (ROOT / f"modules/scaladock/src/main/resources/meridian/scaladock/tokens-{mode}.css").read_text() for mode in ("dark", "light")}
    coverage = token_coverage((provider / "docs/styling.md").read_text(), model["mappings"]["scaladock"], sheets, palettes)
    (EVIDENCE / "token-coverage.json").write_text(json.dumps(coverage, indent=2) + "\n")
    require(args.capture and args.log, "Pass --capture and --log")
    result = json.loads((args.capture / "result.json").read_text())
    validate_native(result)
    meta = json.loads(Path(str(args.log) + ".meta.json").read_text())
    require(meta["exit_code"] == 0 and meta["reason"] == "exited", "Native command did not exit successfully")
    log = args.log.read_text()
    require("B1_ADAPTER_OK" in log and "CSS Error" not in log and "Could not resolve" not in log, "Native completion or CSS errors")
    for name, expected in result["providerClassSha256"].items():
        module = "fx" if name.startswith("scaladock/fx/") else "core"
        require(digest(provider / "modules" / module / "target/scala-3.7.4/classes" / name) == expected, "Executing provider differs from resolved checkout")
    inputs = [ROOT / name for name in ("build.sbt", "upstream/lock.json", "project/build.properties", "project/plugins.sbt", "tools/sbt", "tools/verify_provider.py", "tools/verify_adapter.py", "tools/tests/test_adapter_contract.py", "tokens/meridian.json", "docs/SCALADOCK.md")]
    for folder in ("modules/theme/src", "modules/scaladock/src"):
        inputs.extend(p for p in (ROOT / folder).rglob("*") if p.is_file())
    sources = {str(p.relative_to(ROOT)): digest(p) for p in sorted(inputs)}
    artifacts = {str(p.relative_to(ROOT)): digest(p) for p in sorted(EVIDENCE.rglob("*")) if p.is_file() and p.name not in {"verification.json", "README.md"}}
    source_digest = hashlib.sha256(json.dumps(sources, sort_keys=True, separators=(",", ":")).encode()).hexdigest()
    receipt = {"schema": "meridian-adapter-acceptance.v1", "status": "pass", "provider": resolution,
               "consumer_base_head": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip(),
               "candidate": "uncommitted working tree identified by source_digest", "source_digest": source_digest,
               "digest_method": "SHA-256 of sorted compact JSON path-to-SHA256 map", "sources": sources,
               "native_result": str((args.capture.resolve() / "result.json").relative_to(ROOT)), "command": meta,
               "artifacts": artifacts, "limitations": ["Local macOS/aarch64 native JavaFX dispatch and scene captures", "B2/B3 workspace and D6 packaged/clean consumers remain open", "No full build below disk threshold; no new hosted CI or screen-reader qualification"]}
    (EVIDENCE / "verification.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print(json.dumps({"status": "pass", "source_digest": source_digest, "documented_tokens": 15, "sources": len(sources), "artifacts": len(artifacts)}, indent=2))


if __name__ == "__main__":
    main()
