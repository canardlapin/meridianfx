"""Adverse cases for B0's provenance gate, independent of native UI assertions."""
import copy
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from verify_provider import validate_resolution


class ProviderResolutionTest(unittest.TestCase):
    def setUp(self):
        self.pin = "a" * 40
        self.record = {"configured": self.pin, "observed": self.pin,
                       "uri": f"https://github.com/canardlapin/scaladock.git#{self.pin}",
                       "override": "", "tracked-diff": "", "checkout": "/tmp/b0-remote-fixture"}

    def test_exact_default(self):
        validate_resolution(self.record, self.pin)

    def test_rejects_false_provenance(self):
        for key, value in (("observed", "b" * 40), ("configured", "b" * 40),
                           ("override", "../scaladock"), ("uri", "file:/tmp/provider"),
                           ("tracked-diff", "Dock.scala | 1 +")):
            with self.subTest(key=key):
                record = copy.deepcopy(self.record)
                record[key] = value
                with self.assertRaises(ValueError):
                    validate_resolution(record, self.pin)

    def test_rejects_abbreviated_pin(self):
        with self.assertRaises(ValueError):
            validate_resolution(self.record, "aaaaaaa")


if __name__ == "__main__":
    unittest.main()
