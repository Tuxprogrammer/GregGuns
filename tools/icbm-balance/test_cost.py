"""Unit tests for cost.py on a tiny synthetic dump. Run: python -I tools/icbm-balance/test_cost.py"""
import importlib.util
import os
import sys
import unittest

_spec = importlib.util.spec_from_file_location('cost', os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                                                     'cost.py'))
cost = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(cost)


def row(map_, ins, outs, eut=0, dur=0, fluids=None, kind='machine'):
    return {'source': 'x', 'map': map_, 'kind': kind, 'circuit': 0, 'eut': eut, 'duration': dur,
            'inputs': [{'k': k, 'n': n} for k, n in ins.items()],
            'fluids': [{'k': k, 'n': n} for k, n in (fluids or {}).items()],
            'outputs': [{'k': k, 'n': n} for k, n in outs.items()]}


def dump(rows, itemdata=None, oredict=None):
    return {'rows': rows, 'itemdata': itemdata or {}, 'oredict': oredict or {}}


DATA = {
    'gt:plate:0': {'p': 'plate', 'm': 'Steel', 'u': 1.0, 'by': [], 'ores': ['plateSteel']},
    'gt:circ:0': {'p': 'circuit', 'm': 'Advanced', 'u': 0.0, 'by': [], 'ores': ['circuitAdvanced']},
}


class CostTest(unittest.TestCase):

    def test_plates_count_as_ingots_and_circuits_by_tier(self):
        p = cost.Pricer(dump([row('gt.recipe.assembler', {'gt:plate:0': 4, 'gt:circ:0': 2}, {'m:part:0': 2},
                                  eut=480, dur=100)], DATA))
        c = p.price('m:part:0')
        self.assertAlmostEqual(c.ingots(), 2.0)
        self.assertAlmostEqual(c.circ['Advanced'], 1.0)
        self.assertAlmostEqual(c.eu, 24000.0)

    def test_cheapest_recipe_wins_and_reverse_maps_are_ignored(self):
        rows = [row('gt.recipe.assembler', {'gt:plate:0': 8}, {'m:part:0': 1}, eut=30, dur=1),
                row('gt.recipe.assembler', {'gt:plate:0': 2}, {'m:part:0': 1}, eut=30, dur=1),
                row('gt.recipe.macerator', {}, {'m:part:0': 1}, eut=2, dur=1)]
        p = cost.Pricer(dump(rows, DATA))
        self.assertAlmostEqual(p.price('m:part:0').ingots(), 2.0)

    def test_a_recipe_above_the_players_voltage_is_ignored(self):
        rows = [row('gt.recipe.assembler', {'gt:plate:0': 1}, {'m:part:0': 1}, eut=100000, dur=1),
                row('gt.recipe.assembler', {'gt:plate:0': 6}, {'m:part:0': 1}, eut=480, dur=1)]
        self.assertAlmostEqual(cost.Pricer(dump(rows, DATA), 'HV').price('m:part:0').ingots(), 6.0)
        self.assertAlmostEqual(cost.Pricer(dump(rows, DATA), 'UHV').price('m:part:0').ingots(), 1.0)

    def test_cycles_and_unknowns_end_as_raw_items(self):
        rows = [row('gt.recipe.assembler', {'m:b:0': 1}, {'m:a:0': 1}), row('gt.recipe.assembler', {'m:a:0': 1},
                                                                            {'m:b:0': 1})]
        c = cost.Pricer(dump(rows, DATA)).price('m:a:0')
        self.assertEqual(sum(c.raw.values()), 1)

    def test_an_item_made_only_above_the_tier_is_flagged_not_raw(self):
        # The only recipe of m:a runs at IV: at HV it is above tier, priced at its real cost and flagged (R2-04).
        rows = [row('gt.recipe.assembler', {'gt:plate:0': 6}, {'m:a:0': 1}, eut=7680, dur=1),
                row('gt.recipe.assembler', {'m:a:0': 1, 'gt:plate:0': 1}, {'m:b:0': 1}, eut=480, dur=1)]
        c = cost.Pricer(dump(rows, DATA), 'HV').price('m:b:0')
        self.assertEqual(sum(c.raw.values()), 0)
        self.assertEqual(dict(c.above), {'m:a:0': 1})
        self.assertAlmostEqual(c.ingots(), 7.0)
        self.assertEqual(sum(cost.Pricer(dump(rows, DATA), 'IV').price('m:b:0').above.values()), 0)

    def test_a_route_at_the_tier_beats_a_cheaper_one_above_it(self):
        rows = [row('gt.recipe.assembler', {'gt:plate:0': 1}, {'m:a:0': 1}, eut=7680, dur=1),
                row('gt.recipe.assembler', {'m:a:0': 1}, {'m:b:0': 1}, eut=30, dur=1),
                row('gt.recipe.assembler', {'gt:plate:0': 9}, {'m:b:0': 1}, eut=30, dur=1)]
        c = cost.Pricer(dump(rows, DATA), 'HV').price('m:b:0')
        self.assertEqual(sum(c.above.values()), 0)
        self.assertAlmostEqual(c.ingots(), 9.0)

    def test_molten_metal_is_ingots_and_tools_are_free(self):
        rows = [row('crafting', {'ore:craftingToolSaw': 1, 'gt:plate:0': 1}, {'m:a:0': 1}, kind='shaped'),
                row('gt.recipe.assembler', {'m:a:0': 1}, {'m:b:0': 1}, fluids={'fluid:molten.tin': 288})]
        c = cost.Pricer(dump(rows, DATA)).price('m:b:0')
        self.assertAlmostEqual(c.mats['Tin'], 2.0)
        self.assertAlmostEqual(c.ingots(), 3.0)


if __name__ == '__main__':
    sys.exit(unittest.main())
