#!/usr/bin/env python3
"""Compile the theme and native selection probe without resolving ScalaDock or running sbt."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=Path, help="New evidence directory")
    parser.add_argument("--java-home", type=Path, default=os.environ.get("MERIDIAN_JAVA_HOME", "/opt/homebrew/opt/openjdk@25"))
    parser.add_argument("--classpath-report", type=Path, default=ROOT / "modules/theme/target/foundation/runtime-classpath.txt")
    parser.add_argument("--resources", type=Path, default=ROOT / "modules/theme/src/main/resources")
    parser.add_argument("--probe", choices=["SelectionContrastProbe", "DataFocusProbe"], default="SelectionContrastProbe")
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    jars = [Path(p) for p in args.classpath_report.read_text().splitlines() if p.endswith(".jar")]
    fx = [p for p in jars if p.name.startswith("javafx-") and len(p.stem.split("-")) > 3]
    atlanta = [p for p in jars if p.name == "atlantafx-base-3.0.0.jar"]
    if len(fx) != 3 or len(atlanta) != 1 or not all(p.is_file() for p in fx + atlanta):
        raise SystemExit("Require three cached JavaFX 27 native JARs and AtlantaFX 3.0.0 from a foundation report")
    if not all("-27-" in p.name for p in fx):
        raise SystemExit("Incoherent JavaFX version")
    module_path = os.pathsep.join(map(str, fx))
    sources = sorted((ROOT / "modules/theme/src/main/java").rglob("*.java"))
    sources.append(ROOT / f"modules/sampler/src/main/java/meridian/sampler/{args.probe}.java")
    receipt = {"base_head": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip(),
               "sources": {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest() for p in sources},
               "resources": {str(p.relative_to(args.resources)): hashlib.sha256(p.read_bytes()).hexdigest()
                             for p in sorted(args.resources.rglob("*")) if p.is_file()},
               "dependencies": {str(p): hashlib.sha256(p.read_bytes()).hexdigest() for p in fx + atlanta}, "runs": []}
    with tempfile.TemporaryDirectory(prefix="meridian-selection-") as temp:
        commands = [
            [str(args.java_home / "bin/javac"), "--release", "25", "-Xlint:all", "--module-path", module_path,
             "--add-modules", "javafx.controls", "-cp", str(atlanta[0]), "-d", temp, *map(str, sources)],
            [str(args.java_home / "bin/java"), "--module-path", module_path, "--add-modules", "javafx.controls",
             "--enable-native-access=javafx.graphics", "-Dprism.verbose=true", "-cp",
             os.pathsep.join([temp, str(args.resources.resolve()), str(atlanta[0])]),
             "meridian.sampler." + args.probe, str(output / "native")],
        ]
        for index, command in enumerate(commands):
            with (output / f"{index}.log").open("w") as log:
                try:
                    result = subprocess.run(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, timeout=90)
                    code = result.returncode
                except subprocess.TimeoutExpired:
                    code = 124
                    log.write("\nProbe exceeded 90 seconds; subprocess terminated.\n")
            receipt["runs"].append({"command": command, "exit_code": code})
            (output / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
            print(f"{args.probe}: step {index}, exit {code}; {output / f'{index}.log'}", flush=True)
            if code:
                return code
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
