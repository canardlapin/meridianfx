#!/usr/bin/env python3
"""Verify the actual qualified native CI runtime after checksum-checked extraction."""
import argparse
import json
import platform
from pathlib import Path
import re
import subprocess
import sys


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--java", type=Path, required=True)
    args = parser.parse_args()
    result = subprocess.run([str(args.java), "-XshowSettings:properties", "-version"],
                            text=True, capture_output=True, check=True)
    properties = dict(re.findall(r"^\s*(java.runtime.version|java.vendor|os.arch)\s*=\s*(.+?)\s*$",
                                 result.stdout + result.stderr, re.MULTILINE))
    expected = {"java.runtime.version": "25.0.4.1+1-LTS", "java.vendor": "Eclipse Adoptium"}
    for name, value in expected.items():
        if properties.get(name) != value:
            raise ValueError("Qualified native CI JDK differs: " + name)
    architectures = {"arm64": {"aarch64", "arm64"}, "x86_64": {"x86_64", "amd64"}}
    if sys.platform != "darwin" or properties.get("os.arch") not in architectures.get(platform.machine(), set()):
        raise ValueError("Qualified native CI JDK architecture differs")
    print(json.dumps({"status": "passed", **properties}, sort_keys=True))


if __name__ == "__main__":
    main()
