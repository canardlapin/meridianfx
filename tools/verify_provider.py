#!/usr/bin/env python3
"""Validate a native B0 run and bind its evidence to source and runtime digests."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
from urllib.parse import unquote, urlparse

ROOT = Path(__file__).resolve().parent.parent
EVIDENCE = ROOT / "docs/evidence/provider-acceptance"


def require(condition, message):
    if not condition:
        raise ValueError(message)


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def read_resolution(path):
    return dict(line.split("\t", 1) for line in path.read_text().splitlines())


def validate_resolution(record, pin):
    require(re.fullmatch(r"[0-9a-f]{40}", pin), "Provider pin must be a full SHA")
    require(record["configured"] == record["observed"] == pin, "Configured/observed provider mismatch")
    require(record["uri"] == f"https://github.com/canardlapin/scaladock.git#{pin}", "Provider was not resolved from the pinned remote")
    require(not record["override"], "Local provider override is not acceptance evidence")
    require(not record["tracked-diff"], "Provider checkout has tracked changes")
    require(Path(record["checkout"]).resolve() != (ROOT.parent / "scaladock").resolve(), "Sibling checkout used")


def validate_result(result):
    require(result["status"] == "pass", "Native run failed")
    runtime = result["runtime"]
    require(runtime["java.version"].split(".")[0] == "25", "Wrong executing JDK")
    require(runtime["javafx.runtime.version"].split("+")[0] == "27", "Wrong executing JavaFX")
    modules = runtime["modules"]
    require(len(modules) == 3 and {m["name"] for m in modules} == {"javafx.base", "javafx.graphics", "javafx.controls"}, "Incomplete named-module graph")
    require(all(m["version"] == "27" for m in modules), "Mixed JavaFX module versions")
    fx_jars = [Path(p).name for p in runtime["classpath"] if Path(p).name.startswith("javafx-")]
    require(fx_jars and all(re.fullmatch(r"javafx-(base|graphics|controls)-27(?:-(?:mac|linux|win)[^.]+)?\.jar", p) for p in fx_jars), "Mixed JavaFX classpath")
    require(len(result["phases"]) == 4 and len({p["phase"] for p in result["phases"]}) == 4, "Incomplete phase matrix")
    require(result["built"] == result["disposed"] == 4, "Pane lifecycle mismatch")
    require(result["keyboardActions"] == 6 and result["keyboardMenus"] == 4, "Incomplete keyboard evidence")
    require(result["styleLeasesReleased"] and result["contentConsumedKeyPreserved"], "Lifecycle or key ownership failed")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--capture", type=Path)
    parser.add_argument("--log", type=Path)
    parser.add_argument("--check", type=Path, help="Check retained evidence/source hashes; does not rerun JavaFX")
    args = parser.parse_args()
    if args.check:
        receipt = json.loads(args.check.read_text())
        for section in ("sources", "artifacts"):
            for name, expected in receipt[section].items():
                require(digest(ROOT / name) == expected, "Changed receipt input: " + name)
        print("B0 retained source/evidence hashes verified (not a native rerun)")
        return
    require(args.capture and args.log, "Pass --capture and --log, or --check")
    pin = json.loads((ROOT / "upstream/lock.json").read_text())["scaladock"]["commit"]
    baseline = read_resolution(EVIDENCE / "baseline-resolution.tsv")
    resolution = read_resolution(EVIDENCE / "accepted-resolution.tsv")
    validate_resolution(baseline, baseline["configured"])
    validate_resolution(resolution, pin)
    require(baseline["observed"] != pin and baseline["checkout"] != resolution["checkout"], "Pin-change experiment did not change resolved provider")
    result = json.loads((args.capture / "result.json").read_text())
    validate_result(result)
    metadata = json.loads(Path(str(args.log) + ".meta.json").read_text())
    require(metadata["exit_code"] == 0 and metadata["reason"] == "exited", "Consumer command did not exit successfully")
    require("B0_CONSUMER_OK" in args.log.read_text(), "Consumer completion marker missing")
    provider = Path(resolution["checkout"])
    for name, expected in result["runtime"]["providerClassSha256"].items():
        module = "fx" if name.startswith("scaladock/fx/") else "core"
        require(digest(provider / "modules" / module / "target/scala-3.7.4/classes" / name) == expected,
                "Executing provider bytes differ from resolved checkout: " + name)
    runtime_modules = []
    for module in result["runtime"]["modules"]:
        path = Path(unquote(urlparse(module["location"]).path))
        runtime_modules.append({**module, "sha256": digest(path)})
    inputs = [ROOT / name for name in ("build.sbt", "upstream/lock.json", "project/build.properties", "project/plugins.sbt", "tools/sbt", "tools/verify_provider.py", "tools/tests/test_provider_receipt.py")]
    for folder in ("modules/theme/src", "modules/scaladock/src"):
        inputs.extend(p for p in (ROOT / folder).rglob("*") if p.is_file())
    sources = {str(p.relative_to(ROOT)): digest(p) for p in sorted(inputs)}
    artifacts = {str(p.relative_to(ROOT)): digest(p) for p in sorted(EVIDENCE.rglob("*")) if p.is_file() and p.name not in {"verification.json", "README.md"}}
    receipt = {
        "schema": "meridian-provider-acceptance.v1", "status": "pass",
        "consumer_base_head": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip(),
        "consumer_candidate": "working tree identified by source_digest, not an invented commit",
        "source_digest": hashlib.sha256(json.dumps(sources, sort_keys=True, separators=(",", ":")).encode()).hexdigest(),
        "source_digest_method": "SHA-256 of sorted compact JSON path-to-SHA256 map",
        "provider": resolution, "baseline_provider": baseline,
        "native_result": str((args.capture.resolve() / "result.json").relative_to(ROOT)),
        "command": metadata, "runtime_modules": runtime_modules,
        "sources": sources, "artifacts": artifacts,
        "limitations": ["macOS aarch64 consumer run only; provider hosted CI is separate evidence",
                        "Synthetic JavaFX key dispatch and owned scene captures, not OS input or full VoiceOver qualification",
                        "Upstream macOS AXEnabled=false limitation remains accepted",
                        "Manual fixture appearance wiring; production adapter B1 and packaged/clean consumers D6 remain open"]
    }
    (EVIDENCE / "verification.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print(json.dumps({"status": "pass", "provider": pin, "source_digest": receipt["source_digest"], "sources": len(sources), "artifacts": len(artifacts)}, indent=2))


if __name__ == "__main__":
    main()
