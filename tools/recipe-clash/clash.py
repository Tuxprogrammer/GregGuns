#!/usr/bin/env python
"""Recipe-clash report: GregGuns recipes against the rest of a GTNH pack.

Reads the JSON written by `/gregguns dumprecipes <file>` (config debug.recipeDump=true) and writes a Markdown report.

    python -I tools/recipe-clash/clash.py dump.json report.md [--limit 40]

Rows with source "gregguns" are ours (the mod id of the first output; GT maps whose name starts with "gregguns" are
ours too). Everything else is the pack. Categories:

  exact      machine rows with the same inputs (item/ore keys, stack sizes, fluids, circuit) and a different output,
             in the same map or in another map (a cross-map match means two machines compete for the same input stack)
  shadow     machine rows whose inputs match when ore names are expanded or stack sizes are ignored
  grid       crafting-table rows: shaped grids that match cell for cell (mirror included) with a different output
  shapeless  our shapeless/ore rows whose ingredient multiset also matches a pack shaped or shapeless row, or the
             reverse (our shaped grid vs a pack shapeless row)
  duplicate  same match, same output (harmless but redundant)

Limitation: the dump lists ore dictionary members only for ore names our recipes use, so a pack-side ore name can only
be matched by name equality against our items, not expanded.
"""
import argparse
import collections
import json
import sys

OURS = "gregguns"


def is_ours(row):
    return row["source"] == OURS or row["map"].startswith(OURS)


def keys(row):
    return [i["k"] for i in row["inputs"]]


def base_id(key):
    """minecraft:stick:0 -> minecraft:stick ; ore:plateSteel stays."""
    if key.startswith("ore:") or key.startswith("oreset:"):
        return key
    return key.rsplit(":", 1)[0]


class Matcher:
    def __init__(self, oredict):
        self.members = {"ore:" + n: set(m) for n, m in oredict.items()}
        self.member_ids = {k: {base_id(x) for x in v} for k, v in self.members.items()}

    def one(self, a, b):
        """Can a single ingredient key a and key b be satisfied by the same stack?"""
        if a == b:
            return True
        if a.startswith("ore:") and b.startswith("ore:"):
            return False
        if a.startswith("ore:") or b.startswith("ore:"):
            o, i = (a, b) if a.startswith("ore:") else (b, a)
            if i.startswith("oreset:"):
                return False
            if o not in self.members:
                return False
            return i in self.members[o] or any(
                m.endswith(":*") and base_id(m) == base_id(i) for m in self.members[o]
            ) or (i.endswith(":*") and base_id(i) in self.member_ids[o])
        if base_id(a) == base_id(b):
            return a.endswith(":*") or b.endswith(":*")
        return False

    def multiset(self, xs, ys):
        """Perfect matching between two equal-length key lists (small n)."""
        if len(xs) != len(ys):
            return False
        used = [False] * len(ys)

        def go(i):
            if i == len(xs):
                return True
            for j, y in enumerate(ys):
                if not used[j] and self.one(xs[i], y):
                    used[j] = True
                    if go(i + 1):
                        return True
                    used[j] = False
            return False

        return go(0)

    def grid(self, ga, gb):
        if len(ga) != len(gb) or any(len(r1) != len(r2) for r1, r2 in zip(ga, gb)):
            return False
        for r1, r2 in zip(ga, gb):
            for a, b in zip(r1, r2):
                if (a == "") != (b == ""):
                    return False
                if a and not self.one(a, b):
                    return False
        return True


def trim(grid):
    """Drop empty edge rows/columns so a 2x2 recipe in a 3x3 array compares equal."""
    rows = [r for r in grid if any(c for c in r)]
    if not rows:
        return []
    cols = [c for c in range(len(grid[0])) if any(r[c] for r in rows)]
    return [[r[c] for c in range(min(cols), max(cols) + 1)] for r in rows]


def mirror(grid):
    return [list(reversed(r)) for r in grid]


def fmt_row(r):
    ins = ", ".join("%dx %s" % (i["n"], i["k"]) for i in r["inputs"])
    if r["fluids"]:
        ins += " + " + ", ".join("%dmB %s" % (i["n"], i["k"][6:]) for i in r["fluids"])
    if r["circuit"]:
        ins += " [circuit %d]" % r["circuit"]
    outs = ", ".join("%sx %s" % (o["n"], o["k"]) for o in r["outputs"]) or "(none)"
    tail = ""
    if r["kind"] == "machine":
        tail = " @ %d EU/t, %d ticks" % (r["eut"], r["duration"])
    return "`%s` (%s, %s): %s -> %s%s" % (r["map"], r["source"], r["kind"], ins, outs, tail)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("dump")
    ap.add_argument("report")
    ap.add_argument("--limit", type=int, default=40, help="max listed findings per category")
    args = ap.parse_args()
    with open(args.dump, encoding="utf-8") as f:
        data = json.load(f)
    rows = data["rows"]
    m = Matcher(data.get("oredict", {}))
    ours = [r for r in rows if is_ours(r)]
    pack = [r for r in rows if not is_ours(r)]

    findings = collections.defaultdict(list)  # (category) -> [(map, ours_row, other_row)]

    # --- machine rows -----------------------------------------------------------------------------------------
    def msig(r):
        return (tuple((i["k"], i["n"]) for i in r["inputs"]), tuple((i["k"], i["n"]) for i in r["fluids"]), r["circuit"])

    mach_pack = [r for r in pack if r["kind"] == "machine" and r["inputs"] + r["fluids"]]
    by_sig = collections.defaultdict(list)
    index = collections.defaultdict(list)  # base id / ore key -> rows
    for r in mach_pack:
        by_sig[msig(r)].append(r)
        for k in set(map(base_id, keys(r))):
            index[k].append(r)
    mach_ours = [r for r in ours if r["kind"] == "machine"]
    own_sig = collections.defaultdict(list)
    for r in mach_ours:
        own_sig[msig(r)].append(r)
    for r in mach_ours:
        sig = msig(r)
        exact_ids = set()
        for o in by_sig.get(sig, []):
            exact_ids.add(id(o))
            same_out = o["outputs"] == r["outputs"]
            cat = "duplicate" if same_out else "exact"
            findings[cat].append((o["map"] if o["map"] == r["map"] else r["map"] + " vs " + o["map"], r, o))
        for o in own_sig[sig]:
            if o is not r and id(o) > id(r) and o["map"] == r["map"] and o["outputs"] != r["outputs"]:
                findings["exact"].append((r["map"] + " (own)", r, o))
        # shadow: expand ore names / ignore stack sizes, same fluids and circuit
        if not r["inputs"]:
            continue
        cand = {}
        probe = keys(r)[0]
        ids = {base_id(probe)}
        if probe.startswith("ore:"):
            ids |= m.member_ids.get(probe, set())
        for i in ids:
            for o in index.get(i, []):
                cand[id(o)] = o
        for o in cand.values():
            if id(o) in exact_ids or o["circuit"] != r["circuit"]:
                continue
            if [i["k"] for i in o["fluids"]] != [i["k"] for i in r["fluids"]]:
                continue
            if m.multiset(keys(r), keys(o)):
                cat = "duplicate" if o["outputs"] == r["outputs"] else "shadow"
                findings[cat].append((o["map"] if o["map"] == r["map"] else r["map"] + " vs " + o["map"], r, o))

    # --- crafting rows ----------------------------------------------------------------------------------------
    craft_pack = [r for r in pack if r["map"] == "crafting" and r["kind"] in ("shaped", "shapeless")]
    shaped_by_dim = collections.defaultdict(list)
    shapeless_by_n = collections.defaultdict(list)
    for r in craft_pack:
        if r["kind"] == "shaped":
            g = trim(r["grid"])
            shaped_by_dim[(len(g), len(g[0]) if g else 0)].append((g, r))
        n = sum(i["n"] for i in r["inputs"])
        shapeless_by_n[n].append(r)
    for r in (x for x in ours if x["map"] == "crafting"):
        outs_same = lambda o: o["outputs"] == r["outputs"]  # noqa: E731
        if r["kind"] == "shaped":
            g = trim(r["grid"])
            for cand, o in shaped_by_dim.get((len(g), len(g[0]) if g else 0), []):
                if m.grid(g, cand) or m.grid(mirror(g), cand):
                    findings["duplicate" if outs_same(o) else "grid"].append(("crafting", r, o))
        # shapeless overlap (ours shapeless vs theirs shaped/shapeless, or ours shaped vs theirs shapeless)
        ks = [k for i in r["inputs"] for k in [i["k"]] * i["n"]]
        if not ks:
            continue  # special recipes (dye, refill) have no listable ingredients
        for o in shapeless_by_n.get(len(ks), []):
            if r["kind"] == "shaped" and o["kind"] != "shapeless":
                continue
            ok = [k for i in o["inputs"] for k in [i["k"]] * i["n"]]
            if m.multiset(ks, ok):
                findings["duplicate" if outs_same(o) else "shapeless"].append(("crafting", r, o))
        if r["kind"] == "shapeless":
            pass
    # reverse: pack shapeless vs our shaped is covered above; pack shaped vs our shapeless is covered by n-bucket.

    order = ["exact", "shadow", "grid", "shapeless", "duplicate"]
    maps_ours = collections.Counter(r["map"] for r in ours)
    out = []
    out.append("# Recipe-clash report\n")
    out.append("Rows in dump: %d (ours: %d, pack: %d). Pack machine rows indexed: %d; crafting rows: %d.\n"
               % (len(rows), len(ours), len(pack), len(mach_pack), len(craft_pack)))
    out.append("## Summary\n")
    out.append("| Category | Findings |\n|---|---|")
    for c in order:
        out.append("| %s | %d |" % (c, len(findings[c])))
    out.append("")
    out.append("Our rows per map: " + ", ".join("`%s` %d" % kv for kv in sorted(maps_ours.items())) + "\n")
    out.append("## Per-map counts\n")
    per = collections.defaultdict(collections.Counter)
    for c in order:
        for mp, _, _ in findings[c]:
            per[mp][c] += 1
    out.append("| Map | " + " | ".join(order) + " |\n|---|" + "---|" * len(order))
    for mp in sorted(per):
        out.append("| `%s` | " % mp + " | ".join(str(per[mp][c]) for c in order) + " |")
    out.append("")
    for c in order:
        out.append("## %s (%d)\n" % (c, len(findings[c])))
        for mp, a, b in findings[c][: args.limit]:
            out.append("- **%s**\n  - ours: %s\n  - other: %s" % (mp, fmt_row(a), fmt_row(b)))
        if len(findings[c]) > args.limit:
            out.append("- ... %d more" % (len(findings[c]) - args.limit))
        out.append("")
    with open(args.report, "w", encoding="utf-8") as f:
        f.write("\n".join(out))
    print("wrote %s: %s" % (args.report, ", ".join("%s=%d" % (c, len(findings[c])) for c in order)))


if __name__ == "__main__":
    sys.exit(main())
