#!/usr/bin/env python3
"""Verify immutable foundation inputs and observed runtime dependency boundaries."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import struct

ROOT = Path(__file__).resolve().parent.parent

def require(condition, message):
    if not condition:
        raise SystemExit(message)

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--classpath", action="store_true", help="Check generated module reports")
    parser.add_argument("--capture", type=Path, help="Check a completed native smoke directory")
    args = parser.parse_args()
    lock = json.loads((ROOT / "upstream/lock.json").read_text())
    for relative, expected in lock["source_files"].items():
        path = ROOT / relative
        require(path.is_file(), "Missing locked source: " + relative)
        require(digest(path) == expected, "Changed locked source: " + relative)
    for relative, expected in lock["design_references"].items():
        require(digest(ROOT / relative) == expected, "Changed design reference: " + relative)
    build = (ROOT / "build.sbt").read_text()
    for pin in ["3.7.4", "27", "3.0.0", lock["scaladock"]["commit"]]:
        require('"' + pin + '"' in build, "Missing build pin: " + pin)
    require((ROOT / "project/build.properties").read_text().strip() == "sbt.version=1.12.14",
            "Unexpected sbt pin")
    checked = {"source_files": len(lock["source_files"]), "design_references": 3}
    if args.classpath:
        reports = {}
        for module in ["theme", "workbench", "scaladock", "sampler"]:
            paths = (ROOT / "modules" / module / "target/foundation/runtime-classpath.txt").read_text().splitlines()
            versions = set()
            for path in paths:
                require(Path(path).exists(), "Missing classpath entry: " + path)
                match = re.search(r"/javafx-(?:base|graphics|controls)-([^/]+?)(?:-(?:mac|linux|win)[^/]*)?\.jar$", path)
                if match:
                    versions.add(match.group(1))
            require(versions == {"27"}, module + ": incoherent JavaFX runtime: " + str(versions))
            for required in ["base", "graphics", "controls"]:
                require(any("javafx-" + required + "-27" in p for p in paths),
                        module + ": missing JavaFX " + required)
            if module in ["theme", "workbench"]:
                require(not any("scaladock" in p.lower() or "scalafx" in p.lower()
                                or "/scala-library-" in p or "/scala3-library_" in p for p in paths),
                        module + ": forbidden docking/ScalaFX/Scala runtime dependency")
            reports[module] = {"classpath_entries": len(paths), "javafx_versions": sorted(versions)}
        checked["runtime_reports"] = reports
    if args.capture:
        capture = args.capture if args.capture.is_absolute() else ROOT / args.capture
        props = {}
        for line in (capture / "runtime.properties").read_text().splitlines():
            if not line.startswith("#") and "=" in line:
                key, value = line.split("=", 1)
                props[key] = value
        require(props.get("java.version", "").startswith("25"), "Smoke did not use JDK 25")
        require(props.get("javafx.version") == "27", "Smoke did not use JavaFX 27")
        png = (capture / "foundation.png").read_bytes()
        require(png[:8] == b"\x89PNG\r\n\x1a\n", "Invalid native PNG")
        width, height = struct.unpack(">II", png[16:24])
        require(width > 0 and height > 0, "Empty native capture")
        checked["capture"] = {"width": width, "height": height,
                              "java": props["java.version"], "javafx": props["javafx.version"],
                              "sha256": hashlib.sha256(png).hexdigest()}
    print(json.dumps(checked, indent=2))

if __name__ == "__main__":
    main()
