"""Tests for the reward-tier rule (GGS-12-19) and the shipped UV and UHV rewards.

    python -I -m unittest discover -s tools/questbook -p 'test_*.py'
"""
import copy
import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import build_quests as bq  # noqa: E402

DATA = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'quests')
LUV_CIRCUIT = {'id': 'gregtech:gt.metaitem.01', 'count': 4, 'damage': 32706}
UHV_PLATE = {'id': 'gregtech:gt.metaitem.01', 'count': 8, 'damage': 17129}


class RewardTiers(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.data = bq.load_data(DATA)

    def quest(self, data, key):
        return next(q for q in data['quests'] if q['key'] == key)

    def with_choice_item(self, key, stack):
        data = copy.deepcopy(self.data)
        choice = next(r for r in self.quest(data, key)['rewards'] if r['type'] == 'choice')
        choice['items'].append(stack)
        return data

    def test_shipped_data_is_clean(self):
        problems, warnings = bq.check(self.data)
        self.assertEqual([], [p for p in problems if 'GGS-12-19' in p])
        self.assertEqual([], [w for w in warnings if 'GGS-12-19' in w])

    def test_below_tier_reward_warns(self):
        _, warnings = bq.check(self.with_choice_item('gg.icbm.antimatter', LUV_CIRCUIT))
        self.assertTrue(any('gg.icbm.antimatter' in w and 'GGS-12-19' in w for w in warnings), warnings)

    def test_above_tier_reward_is_an_error(self):
        problems, _ = bq.check(self.with_choice_item('gg.icbm.antimatter', UHV_PLATE))
        self.assertTrue(any('gg.icbm.antimatter' in p and 'GGS-12-19' in p for p in problems), problems)

    def test_uv_and_uhv_pay_their_own_crates(self):
        for key, bag in (('gg.icbm.antimatter', 'ordnance4'), ('gg.icbm.redmatter', 'ordnance5')):
            bags = [s['bag'] for r in self.quest(self.data, key)['rewards'] if r['type'] == 'item'
                    for s in r['items'] if 'bag' in s]
            self.assertEqual([bag], bags, key)

    def test_uv_and_uhv_choices_hold_no_lower_tier_gate_items(self):
        for key in ('gg.icbm.antimatter', 'gg.icbm.redmatter'):
            choice = next(r for r in self.quest(self.data, key)['rewards'] if r['type'] == 'choice')
            for s in choice['items']:
                self.assertNotIn(s.get('damage'), (32706, 17325, 32088), '%s pays %s' % (key, s))
                self.assertNotEqual('gregguns:fuze_assembly_mk3', s['id'], key)


if __name__ == '__main__':
    unittest.main()
