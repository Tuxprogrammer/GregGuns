"""Self-test for clash.py: python -I tools/recipe-clash/test_clash.py"""
import json
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import clash  # noqa: E402


def row(map_, source, ins, outs, kind="machine"):
    return {"map": map_, "source": source, "kind": kind, "circuit": 0, "fluids": [], "eut": 30, "duration": 100,
            "inputs": [{"k": k, "n": 1} for k in ins], "outputs": [{"k": k, "n": 1} for k in outs]}


def run(rows):
    d = tempfile.mkdtemp()
    dump, rep = os.path.join(d, "d.json"), os.path.join(d, "r.md")
    with open(dump, "w", encoding="utf-8") as f:
        json.dump({"rows": rows, "oredict": {}}, f)
    clash.main([dump, rep])
    with open(rep, encoding="utf-8") as f:
        return f.read()


class ClashTest(unittest.TestCase):
    def test_cross_map_is_ignored(self):
        text = run([row("gt.recipe.scanner", "gregguns", ["a:1"], ["gregguns:x"]),
                    row("gt.recipe.macerator", "gregtech", ["a:1"], ["b:2"]),
                    row("gt.recipe.assembler", "gregguns", ["a:1"], ["gregguns:y"]),
                    row("gt.recipe.compressor", "gregtech", ["a:1"], ["c:3"])])
        self.assertIn("| exact | 0 |", text)
        self.assertIn("| shadow | 0 |", text)

    def test_same_scanner_input_clashes(self):
        text = run([row("gt.recipe.scanner", "gregguns", ["a:1"], ["gregguns:x"]),
                    row("gt.recipe.scanner", "gregtech", ["a:1"], ["b:2"])])
        self.assertIn("| exact | 1 |", text)

    def test_same_map_shadow(self):
        text = run([row("gt.recipe.assembler", "gregguns", ["a:1", "b:1"], ["gregguns:x"]),
                    row("gt.recipe.assembler", "gregtech", ["b:1", "a:*"], ["c:3"])])
        self.assertIn("| shadow | 1 |", text)


if __name__ == "__main__":
    unittest.main()
