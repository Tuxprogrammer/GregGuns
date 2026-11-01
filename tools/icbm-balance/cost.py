"""Prices ICBM missiles, warheads and Galacticraft rockets from a GregGuns recipe dump of the real pack.

Usage: python -I tools/icbm-balance/cost.py <dump.json[.gz]> <report.md> [--json out.json]

The dump comes from `/gregguns dumprecipes` (see tools/recipe-clash/README.md); it carries GT's unification data
("itemdata") and the NASA Workbench rows. An item is priced by expanding the cheapest recipe that makes it, down to:
- metal and other GT materials, in ingot units (a plate is 1, a rod 0.5, molten metal 144 L = 1);
- circuits, counted by tier and never expanded (the pack's circuit chains are their own economy);
- fluids, in liters, never expanded;
- raw items: items with no recipe at any voltage and no GT data.
An item whose recipes all run above the pricer's voltage (or need such an item) is "above tier": it is priced at the
cheapest route at any voltage and listed in the report's "Above tier" column, never hidden as a raw item.
EU is summed over every machine step. The single "score" used to pick between recipes, and printed as "units", is
ingots + circuit weight (2 ** (tier index + 1): LV 4, HV 16, EV 32, LuV 128) + 1 per 1,000 L of fluid + 4 per raw item.
Between two recipes, one with no above-tier item always wins.
"""
import gzip
import json
import math
import re
import sys
from collections import defaultdict

CIRCUIT_TIERS = ['Primitive', 'Basic', 'Good', 'Advanced', 'Data', 'Elite', 'Master', 'Ultimate', 'SuperconductorUHV',
                 'Infinite', 'Bio', 'Optical', 'Piko', 'Quantum']
CIRCUIT_ALIAS = {'Superconductor': 'SuperconductorUHV'}
VOLTS = ['ULV', 'LV', 'MV', 'HV', 'EV', 'IV', 'LuV', 'ZPM', 'UV', 'UHV', 'UEV', 'UIV', 'UMV', 'UXV']
CIRCUIT_VOLT = {name: VOLTS[i] for i, name in enumerate(CIRCUIT_TIERS)}

# Ore prefixes and the material units they hold (GT OrePrefixes amounts), for ore keys with no item data.
PREFIX_UNITS = [
    ('plateDense', 9), ('plateQuintuple', 5), ('plateQuadruple', 4), ('plateTriple', 3), ('plateDouble', 2),
    ('plate', 1), ('ingotDouble', 2), ('ingot', 1), ('dustTiny', 1 / 9), ('dustSmall', 0.25), ('dust', 1),
    ('nugget', 1 / 9), ('block', 9), ('stickLong', 1), ('stick', 0.5), ('bolt', 0.125), ('screw', 1 / 9),
    ('ring', 0.25), ('round', 1 / 9), ('foil', 0.25), ('wireFine', 0.125), ('gearGtSmall', 1), ('gearGt', 4),
    ('frameGt', 2), ('gem', 1), ('compressed', 1), ('rotor', 4.25), ('spring', 1), ('cell', 1),
]
# Maps that take things apart or only show: never a way to make an item.
SKIP_MAPS = re.compile(r'macerator|arcfurnace|recycler|extractor|unpackager|centrifuge|electrolyzer|'
                       r'thermalcentrifuge|orewash|sifter|scanner|replicator|massfab|disassembl|research|'
                       r'pulveriz|fluidextract|decay|fusion|uuamplifier|byproduct|smelting|cuttingsaw|nanochip|componentassemblyline', re.I)
MAX_DEPTH = 14


def load(path):
    opener = gzip.open if path.endswith('.gz') else open
    with opener(path, 'rt', encoding='utf-8') as f:
        return json.load(f)


class Cost:
    __slots__ = ('mats', 'circ', 'fluid', 'eu', 'raw', 'above')

    def __init__(self):
        self.mats = defaultdict(float)
        self.circ = defaultdict(float)
        self.fluid = defaultdict(float)
        self.eu = 0.0
        self.raw = defaultdict(float)
        self.above = defaultdict(float)  # items made only above the pricer's voltage, by count

    def add(self, other, n=1.0):
        for d, o in ((self.mats, other.mats), (self.circ, other.circ), (self.fluid, other.fluid),
                     (self.raw, other.raw), (self.above, other.above)):
            for k, v in o.items():
                d[k] += v * n
        self.eu += other.eu * n
        return self

    def ingots(self):
        return sum(self.mats.values())

    def circuit_units(self):
        return sum(v * 2 ** (CIRCUIT_TIERS.index(k) + 1) for k, v in self.circ.items())

    def raw_count(self):
        return sum(self.raw.values())

    def score(self):
        return self.ingots() + self.circuit_units() + sum(self.fluid.values()) / 1000.0 + 4 * sum(self.raw.values())

    def rank(self):
        """The key recipes are chosen by: any route at the player's voltage beats one that needs an above-tier item."""
        return (1 if any(v > 0 for v in self.above.values()) else 0, self.score())


INF = None  # marks a cycle


def normalize(row):
    """Dump rows list inputs and outputs as [{"k": key, "n": count}]; fold them into dicts and pairs."""
    if isinstance(row.get('inputs'), list):
        ins = {}
        for e in row['inputs']:
            ins[e['k']] = ins.get(e['k'], 0) + e['n']
        row['inputs'] = ins
    if isinstance(row.get('fluids'), list):
        fl = {}
        for e in row['fluids']:
            fl[e['k']] = fl.get(e['k'], 0) + e['n']
        row['fluids'] = fl
    if row.get('outputs') and isinstance(row['outputs'][0], dict):
        row['outputs'] = [[e['k'], e['n']] for e in row['outputs']]
    return row


VOLTAGE = {name: 8 * 4 ** i for i, name in enumerate(VOLTS)}


class Pricer:
    """Prices items for a player at one voltage tier: only machine recipes at or below its voltage count."""

    def __init__(self, dump, tier='UHV'):
        self.dump = dump
        self.tier = tier
        self.max_eut = VOLTAGE[tier]
        self.rows = dump['rows'] if dump.get('_normalized') else [normalize(r) for r in dump['rows']]
        dump['rows'] = self.rows
        dump['_normalized'] = True
        self.oredict = dump.get('oredict', {})
        self.itemdata = dump.get('itemdata', {})
        self.makers = defaultdict(list)
        self.made_above = set()  # items with a recipe, but only above this voltage
        self.top = None  # the uncapped pricer that prices above-tier items, made on first use
        for r in self.rows:
            if SKIP_MAPS.search(r.get('map', '')):
                continue
            if r.get('kind', '').startswith('special:') and not r['map'].startswith(('gc.', 'gs.')):
                continue  # custom IRecipe rows carry outputs only
            above = abs(r.get('eut', 0)) > self.max_eut
            ins = set(r.get('inputs', {}))
            for o in r.get('outputs', []):
                key, n = o[0], int(o[1])
                if key in ins or key.rsplit(':', 1)[0] + ':*' in ins:
                    continue  # an NBT reset or a refit of the same item
                if n > 0 and not key.startswith('fluid:'):
                    if above:
                        self.made_above.add(key)
                    else:
                        self.makers[key].append((r, n))
        self.made_above.difference_update(self.makers)
        self.memo = {}
        self.busy = set()
        self.choice = {}

    def above_tier(self, key):
        """An item made only above this voltage: priced at its cheapest route at any voltage, and flagged."""
        if self.top is None:
            self.top = self if self.tier == VOLTS[-1] else Pricer(self.dump, VOLTS[-1])
        c = Cost().add(self.top.price(key))
        c.above[key] += 1
        return c

    # ---- leaves ----
    def circuit_of(self, key):
        names = []
        if key.startswith('ore:'):
            names = [key[4:]]
        else:
            names = self.itemdata.get(key, {}).get('ores', [])
        for n in names:
            if n.startswith('circuit'):
                t = CIRCUIT_ALIAS.get(n[7:], n[7:])
                if t in CIRCUIT_TIERS:
                    return t
        return None

    def material_of(self, key):
        d = self.itemdata.get(key)
        if d and d.get('p') and d.get('m') and d['m'] not in ('_NULL', 'Empty', 'Any'):
            out = {d['m']: d['u']}
            for m, u in d.get('by', []):
                out[m] = out.get(m, 0) + u
            return out
        if key.startswith('ore:'):
            name = key[4:]
            for member in self.oredict.get(name, []):
                got = self.material_of(member)
                if got:
                    return got
            for p, u in PREFIX_UNITS:
                if name.startswith(p) and len(name) > len(p) and name[len(p)].isupper():
                    return {name[len(p):]: u}
        return None

    # ---- expansion ----
    def price(self, key, depth=0):
        if key in self.memo:
            return self.memo[key]
        c = Cost()
        if key.startswith('ore:craftingTool'):
            self.memo[key] = c  # a tool is not used up
            return c
        circ = self.circuit_of(key)
        if circ:
            c.circ[circ] = 1
            self.memo[key] = c
            return c
        mat = self.material_of(key)
        if mat:
            for m, u in mat.items():
                c.mats[m] += u
            self.memo[key] = c
            return c
        if key.startswith('ore:'):
            members = self.oredict.get(key[4:], [])
            best = None
            for m in members[:6]:
                got = self.price(m, depth + 1)
                if got is not INF and (best is None or got.rank() < best.rank()):
                    best = got
            if best is None:
                c.raw[key] = 1
                best = c
            self.memo[key] = best
            return best
        if depth > MAX_DEPTH or key in self.busy:
            return INF
        self.busy.add(key)
        best, best_row = None, None
        for r, n in self.makers.get(key, []):
            got = self.price_row(r, n, depth)
            if got is INF:
                continue
            if best is None or got.rank() < best.rank():
                best, best_row = got, r
        self.busy.discard(key)
        if best is None and key in self.made_above:
            best = self.above_tier(key)
        elif best is None:
            c.raw[key] = 1
            best = c
        else:
            self.choice[key] = best_row
        self.memo[key] = best
        return best

    def price_row(self, r, n, depth):
        c = Cost()
        for k, cnt in r.get('inputs', {}).items():
            if k.endswith(':*'):
                k = k[:-2] + ':0'
            got = self.price(k, depth + 1)
            if got is INF:
                return INF
            c.add(got, cnt)
        for f, amt in r.get('fluids', {}).items():
            name = f[6:] if f.startswith('fluid:') else f
            if name.startswith('molten.'):
                c.mats[name[7:].capitalize()] += amt / 144.0
            else:
                c.fluid[name] += amt
        if r.get('kind') == 'machine':
            c.eu += abs(r.get('eut', 0)) * r.get('duration', 0)
        out = Cost().add(c, 1.0 / n)
        return out


def fmt_mats(c, top=6):
    items = sorted(c.mats.items(), key=lambda kv: -kv[1])
    s = ', '.join('%s %.1f' % (m, v) for m, v in items[:top] if v >= 0.05)
    rest = sum(v for _, v in items[top:])
    return s + (', other %.1f' % rest if rest >= 0.05 else '')


def fmt_circ(c):
    return ', '.join('%g %s' % (round(v, 2), CIRCUIT_VOLT.get(k, k)) for k, v in
                     sorted(c.circ.items(), key=lambda kv: CIRCUIT_TIERS.index(kv[0])) if v > 0) or '-'


def fmt_fluid(c):
    return ', '.join('%s %d L' % (k, v) for k, v in sorted(c.fluid.items(), key=lambda kv: -kv[1]) if v >= 1) or '-'


def find_rows(rows, out_prefix, map_re=None, circuit=None, needs=None):
    found = []
    for r in rows:
        if map_re and not re.search(map_re, r.get('map', '')):
            continue
        if circuit is not None and r.get('circuit') != circuit:
            continue
        if not any(o[0].startswith(out_prefix) for o in r.get('outputs', [])):
            continue
        if needs and not any(k.startswith(needs) for k in r.get('inputs', {})):
            continue
        found.append(r)
    return found


# Blast id, casing tier and its circuit: WarheadRecipeSpec (the payload and fill rows share the circuit).
WARHEADS = [
    ('condensed', 1, 1), ('shrapnel', 1, 3), ('anvil', 1, 4), ('incendiary', 1, 5), ('debilitation', 1, 6),
    ('chemical', 1, 7), ('smoke', 1, 8), ('repulsive', 1, 9), ('attractive', 1, 10),
    ('fragmentation', 2, 19), ('thermobaric', 2, 24), ('sonic', 2, 21), ('breaching', 2, 22), ('contagious', 2, 23),
    ('exothermic', 3, 1), ('endothermic', 3, 2), ('nuclear', 3, 3), ('emp', 3, 4), ('ender', 3, 5),
    ('antigrav', 3, 6), ('antimatter', 4, 1), ('redmatter', 5, 2),
]
INERT_CIRCUIT = 2


def warhead_costs(p, only_tier=None):
    out = {}
    rows = [r for r in p.rows if r.get('map', '').endswith('assembler')]
    bare = {}
    for t in range(1, 6):
        if only_tier is not None and t != only_tier:
            continue
        rs = [r for r in find_rows(rows, 'gregguns:inert_warhead_t%d:' % t, circuit=INERT_CIRCUIT)
              if not any(k.startswith('gregguns:inert_warhead') for k in r['inputs'])]
        if rs:
            bare[t] = p.price_row(rs[0], 1, 0)
    for blast, t, circ in WARHEADS:
        if (only_tier is not None and t != only_tier) or t not in bare:
            continue
        c = Cost().add(bare[t])
        pay = [r for r in find_rows(rows, 'gregguns:inert_warhead_t%d:' % t, circuit=circ)
               if any(k.startswith('gregguns:inert_warhead') for k in r['inputs'])]
        fill = find_rows(rows, 'gregguns:warhead_t%d:' % t, circuit=circ)
        if blast != 'condensed' and not pay:
            continue
        if not fill:
            continue
        for r in pay[:1] + fill[:1]:
            rc = Cost()
            for k, cnt in r['inputs'].items():
                if k.startswith('gregguns:inert_warhead'):
                    continue
                rc.add(p.price(k), cnt)
            for f, amt in r.get('fluids', {}).items():
                rc.fluid[f[6:]] += amt
            rc.eu += abs(r.get('eut', 0)) * r.get('duration', 0)
            c.add(rc)
        out[blast] = (t, c)
    return out


def fmt_above(c):
    return ', '.join('%g %s' % (round(v, 2), k) for k, v in sorted(c.above.items()) if v > 0) or '-'


def row(label, c, extra=''):
    return '| %s | %.0f | %s | %s | %s | %.1fM | %.0f | %s |%s' % (
        label, c.ingots(), fmt_mats(c), fmt_circ(c), fmt_fluid(c), c.eu / 1e6, c.score(), fmt_above(c), extra)


HEAD = '| Item | Ingots | Main materials | Circuits | Fluids | EU | Units | Above tier |'
SEP = '|---|---|---|---|---|---|---|---|'


ROCKETS = [('t1', 'HV'), ('t2', 'EV'), ('t3', 'IV'), ('t4', 'LuV'), ('t5', 'ZPM'), ('t6', 'UV'), ('t7', 'UHV')]
BODIES = [(1, 'HV'), (2, 'EV'), (3, 'LuV')]
WARHEAD_TIER = {1: 'HV', 2: 'EV', 3: 'LuV', 4: 'UV', 5: 'UHV'}
OWN_TIER = {'nuclear': 'ZPM'}


def report(dump):
    pricers = {}

    def at(tier):
        if tier not in pricers:
            pricers[tier] = Pricer(dump, tier)
        return pricers[tier]

    lines = ['# ICBM balance: measured costs', '',
             'From the recipe dump of the real pack (`/gregguns dumprecipes`), priced by `tools/icbm-balance/cost.py`',
             'for a player at the item\'s own tier (only recipes at or below that voltage).',
             'Units = ingots + circuits (2^(tier+1) each: HV 16, EV 32, IV 64, LuV 128) + 1 per 1,000 L + 4 per raw item.',
             '"Above tier" lists the items in the chain that no recipe at that voltage makes (priced at any voltage).',
             '']
    data = {}
    lines += ['## Rockets (GalaxySpace NASA Workbench, no chest), priced at the rocket\'s tier', '', HEAD, SEP]
    for t, v in ROCKETS:
        p = at(v)
        rs = [r for r in p.rows if r.get('map') == 'gs.nasa_workbench.' + t]
        if not rs:
            continue
        c = p.price_row(rs[0], 1, 0)
        data['rocket_' + t] = c
        lines.append(row('Rocket %s (%s)' % (t.upper(), v), c))
        # The reference for a missile: the rocket without its lander module (a missile does not come back).
        bare = dict(rs[0])
        bare['inputs'] = {k: n for k, n in rs[0]['inputs'].items() if 'ModuleLander' not in k}
        c = p.price_row(bare, 1, 0)
        data['rocketref_' + t] = c
        lines.append(row('Rocket %s (%s) without lander' % (t.upper(), v), c))
    lines += ['', '## Missile bodies (cheapest route at the body tier)', '', HEAD, SEP]
    for t, v in BODIES:
        p = at(v)
        k = 'gregguns:missile_body_t%d:0' % t
        c = p.price(k)
        data['body_t%d' % t] = c
        ch = p.choice.get(k)
        lines.append(row('Missile Body T%d (%s, %s)' % (t, v, ch['map'] if ch else 'no recipe'), c))
    lines += ['', '## Warheads (inert + payload + full fill), priced at the warhead tier', '', HEAD, SEP]
    for t in range(1, 6):
        for blast, (bt, c) in warhead_costs(at(WARHEAD_TIER[t]), only_tier=t).items():
            if blast in OWN_TIER:
                bt, c = warhead_costs(at(OWN_TIER[blast]), only_tier=t)[blast]
            data['warhead_' + blast] = c
            lines.append(row('%s T%d' % (blast, bt), c))
    lines += ['', '## Parts, priced at their own tier', '', HEAD, SEP]
    p = at('LuV')
    for k in sorted(k for k in p.makers if k.startswith('gregguns:') and (
            'fuze' in k or 'plastic' in k or 'guidance' in k or 'canister' in k or 'sleeve' in k or 'liner' in k
            or 'chip' in k or 'schematic' in k or ('inert_warhead' in k and '#' not in k))):
        tier = 'HV' if k.endswith(('_t1:0', 'mk1:0', 'plastic_explosive:0', 'canister:0', 'steel:0', 'cone:0')) \
            else 'EV' if k.endswith(('_t2:0', 'mk2:0', 'titanium:0')) else 'IV' if 'guidance_computer_mk3' in k \
            else 'LuV' if k.endswith(('_t3:0', 'mk3:0')) else 'UV' if k.endswith('_t4:0') else 'UHV'
        c = at(tier).price(k)
        data[k] = c
        lines.append(row('%s (%s)' % (k, tier), c))
    lines += ['', '## Summary: a missile against the rocket of its tier (without the lander)', '',
              '| Missile | Body units | Full warhead units | Missile / rocket | Warhead / body | Above tier |',
              '|---|---|---|---|---|---|']
    pairs = [('T1 conventional vs Rocket T1 (HV)', 1, 'rocketref_t1', [b for b, t, _ in WARHEADS if t == 1]),
             ('T2 conventional vs Rocket T2 (EV)', 2, 'rocketref_t2', [b for b, t, _ in WARHEADS if t == 2]),
             ('T3 conventional vs Rocket T4 (LuV)', 3, 'rocketref_t4',
              ['exothermic', 'endothermic', 'emp', 'ender', 'antigrav']),
             ('Nuclear (T3 body) vs Rocket T5 (ZPM)', 3, 'rocketref_t5', ['nuclear']),
             ('Antimatter (T3 body) vs Rocket T6 (UV)', 3, 'rocketref_t6', ['antimatter']),
             ('Red matter (T3 body) vs Rocket T7 (UHV)', 3, 'rocketref_t7', ['redmatter'])]
    for label, t, rk, blasts in pairs:
        if rk not in data:
            continue
        body = data['body_t%d' % t].score()
        ws = [data['warhead_' + b].score() for b in blasts if 'warhead_' + b in data]
        if not ws:
            continue
        flagged = Cost().add(data['body_t%d' % t])
        for b in blasts:
            if 'warhead_' + b in data:
                flagged.above.update({k: 1 for k, v in data['warhead_' + b].above.items() if v > 0})
        lo, hi = min(ws), max(ws)
        rocket = data[rk].score()
        rng = (lambda a, b, f='%.2f': (f % a) if abs(a - b) < 1e-9 else (f + ' - ' + f) % (a, b))
        lines.append('| %s | %.0f | %s | %s | %s | %s |' % (
            label, body, rng(lo, hi, '%.0f'), rng((body + lo) / rocket, (body + hi) / rocket), rng(lo / body, hi / body),
            ', '.join(sorted(k for k, v in flagged.above.items() if v > 0)) or '-'))
    return lines, data, pricers


def main(argv):
    if len(argv) < 3:
        print(__doc__)
        return 2
    dump = load(argv[1])
    lines, data, _ = report(dump)
    with open(argv[2], 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines) + '\n')
    if '--json' in argv:
        out = {k: {'ingots': c.ingots(), 'units': c.score(), 'eu': c.eu, 'circuits': dict(c.circ),
                   'fluids': dict(c.fluid), 'mats': dict(c.mats), 'raw': dict(c.raw), 'above': dict(c.above)}
               for k, c in data.items()}
        with open(argv[argv.index('--json') + 1], 'w') as f:
            json.dump(out, f, indent=1, sort_keys=True)
    print('\n'.join(lines))
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv))
