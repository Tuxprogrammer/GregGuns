"""Self-test for clash.py: python -I tools/recipe-clash/test_clash.py"""
import json
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import clash  # noqa: E402


def row(map_, source, ins, outs, kind="machine", circuit=0, fluids=(), counts=None):
    counts = counts or {}
    return {"map": map_, "source": source, "kind": kind, "circuit": circuit, "eut": 30, "duration": 100,
            "fluids": [{"k": k, "n": n} for k, n in fluids],
            "inputs": [{"k": k, "n": counts.get(k, 1)} for k in ins], "outputs": [{"k": k, "n": 1} for k in outs]}


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

    def test_pack_row_with_fewer_inputs_shadows_ours(self):
        # The Blaster Rifle case: the pack row (circuit 1, two of our items) fires on our inputs.
        asm = "gt.recipe.assembler"
        text = run([row(asm, "gregguns", ["gregguns:a:0", "b:1", "c:2"], ["gregguns:gun:0"], circuit=1),
                    row(asm, "AFSU", ["b:1", "c:2"], ["AFSU:ALC:0"], circuit=1)])
        self.assertIn("| subset | 1 |", text)
        self.assertIn("shadowed by", text)

    def test_other_circuit_removes_the_subset(self):
        asm = "gt.recipe.assembler"
        text = run([row(asm, "gregguns", ["gregguns:a:0", "b:1", "c:2"], ["gregguns:gun:0"], circuit=5),
                    row(asm, "AFSU", ["b:1", "c:2"], ["AFSU:ALC:0"], circuit=1)])
        self.assertIn("| subset | 0 |", text)

    def test_circuit_zero_pack_row_still_shadows(self):
        asm = "gt.recipe.assembler"
        text = run([row(asm, "gregguns", ["gregguns:a:0", "b:1"], ["gregguns:gun:0"], circuit=3),
                    row(asm, "gregtech", ["b:1"], ["c:3"], circuit=0)])
        self.assertIn("| subset | 1 |", text)

    def test_counts_decide_a_subset(self):
        # Ours takes 2 plates and 9 springs, theirs 3 plates and 8: neither fires on the other's inputs.
        press = "gregguns.ammoPress"
        smg = row(press, "gregguns", ["plate", "spring"], ["gregguns:smg:0"], counts={"plate": 2, "spring": 9})
        ar = row(press, "gregguns", ["plate", "spring"], ["gregguns:ar:0"], counts={"plate": 3, "spring": 8})
        self.assertIn("| subset | 0 |", run([smg, ar]))
        smg8 = row(press, "gregguns", ["plate", "spring"], ["gregguns:smg:0"], counts={"plate": 2, "spring": 8})
        text = run([smg8, ar])
        self.assertIn("| subset | 1 |", text)
        self.assertIn("(own)", text)

    def test_ignore_counts_finds_near_misses(self):
        eng = "gt.recipe.laserengraver"
        rows = [row(eng, "gregguns", ["lens", "diamond"], ["gregguns:focus:0"]),
                row(eng, "gregtech", ["diamond"], ["x:1"], counts={"diamond": 3})]
        self.assertIn("| subset | 0 |", run(rows))
        clash.IGNORE_COUNTS = False
        d = tempfile.mkdtemp()
        dump, rep = os.path.join(d, "d.json"), os.path.join(d, "r.md")
        with open(dump, "w", encoding="utf-8") as f:
            json.dump({"rows": rows, "oredict": {}}, f)
        clash.main([dump, rep, "--ignore-counts"])
        clash.IGNORE_COUNTS = False
        with open(rep, encoding="utf-8") as f:
            self.assertIn("| subset | 1 |", f.read())

    def test_fluid_only_recipe_counts_as_ours(self):
        vat = "bw.recipe.BacteriaVat"
        ours = row(vat, "unknown", [], [], circuit=2,
                   fluids=[("fluid:biomass", 1000), ("fluid:ic2distilledwater", 100)])
        ours["outputs"] = [{"k": "fluid:escherichiakolifluid", "n": 1000}]
        theirs = row(vat, "unknown", [], [], fluids=[("fluid:biomass", 100)])
        theirs["outputs"] = [{"k": "fluid:fermentedbiomass", "n": 100}]
        # The pack also makes the same culture fluid from sugar and water: that row is not ours.
        sugar = row(vat, "unknown", ["minecraft:sugar:0"], [], circuit=1, fluids=[("fluid:water", 1000)])
        sugar["outputs"] = [{"k": "fluid:escherichiakolifluid", "n": 10}]
        text = run([ours, theirs, sugar])
        self.assertIn("| subset | 1 |", text)
        self.assertIn("special slot: verify in game", text)
        self.assertIn("| exact | 0 |", text)
        self.assertTrue(clash.is_ours(ours))
        self.assertFalse(clash.is_ours(sugar))


if __name__ == "__main__":
    unittest.main()
