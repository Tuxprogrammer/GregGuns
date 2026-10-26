"""The recipes sheet: the crafting chains a player uses in the Steam age and at LV.

Machine rows (Assembler and Ammo Press) are read from the mod's recipe tables through the test-side RecipeExporter
(build/infographics/recipes.json), so the sheet prints what the mod registers. The crafting-table recipes are not in a
table; they are copied here from the classes named beside each one.
"""

from __future__ import annotations

import json
import os
import re
from pathlib import Path

import gt
import kit as K
import scenes as S
import vanilla as V
from kit import chip, esc, url
from pages import MOJANG_CREDIT, POINT, img, page_doc

RECIPE_CSS = """
.slot { position: relative; }
.slot .cnt { position: absolute; right: 2px; bottom: -1px; color: #fff; font: 700 17px Bahnschrift, sans-serif;
  text-shadow: 2px 2px 0 #3f3f3f; line-height: 1; }
.cgrid { display: grid; grid-template-columns: repeat(3, auto); gap: 0; background: #8b8b8b; padding: 3px;
  border-radius: 3px; box-shadow: inset 2px 2px 0 #373737, inset -2px -2px 0 #fff; }
.cgrid .slot { box-shadow: inset 2px 2px 0 #373737, inset -2px -2px 0 #fff; border-radius: 0; }
.ccard { display: flex; flex-direction: column; gap: 10px; }
.ccard .row { display: flex; align-items: center; gap: 12px; }
.ccard h3 { font-size: 21px; }
.legend { display: flex; flex-wrap: wrap; gap: 4px 12px; font-size: 15px; color: var(--muted); }
.legend span { display: inline-flex; align-items: center; gap: 5px; }
.legend img { width: 20px; height: 20px; image-rendering: pixelated; }
.mrow { display: grid; grid-template-columns: var(--inw, 300px) 30px auto 1fr; align-items: center; gap: 12px; padding: 10px 14px;
  border-radius: 10px; background: rgba(255,255,255,.035); border: 1px solid var(--line); }
.mrow .ins { display: flex; flex-wrap: wrap; gap: 4px; max-width: 300px; }
.mrow .what b { font-size: 18px; }
.mrow .what .m { color: var(--muted); font-size: 15px; margin-top: 2px; line-height: 1.3; }
.sec { display: flex; align-items: center; gap: 12px; margin: 26px 0 12px; }
.sec h2 { font-size: 28px; }
"""

LANG = K.ASSETS / "lang/en_US.lang"

# GT display names for the part prefixes the low-tier recipes use.
PREFIX = {"plate": "{} Plate", "screw": "{} Screw", "springSmall": "Small {} Spring", "ring": "{} Ring",
          "stickLong": "Long {} Rod", "gearGtSmall": "Small {} Gear", "foil": "{} Foil", "round": "{} Round",
          "nugget": "{} Nugget", "lens": "{} Lens", "stick": "{} Stick", "pipeTiny": "Tiny {} Fluid Pipe",
          "pipeSmall": "Small {} Fluid Pipe"}
VANILLA = {"cobblestone": ("cobblestone", "Cobblestone"), "stone": ("stone", "Stone"), "logWood": ("log", "Log"),
           "blockWool": ("wool", "Wool"), "leather": ("leather", "Leather"), "mc:string": ("string", "String"),
           "slimeball": ("slimeball", "Slimeball"), "flint_and_steel": ("flint_and_steel", "Flint and Steel"),
           "craftingPiston": ("piston", "Piston"), "crafting_table": ("crafting_table", "Crafting Table")}
META = {"motor_lv": "LV Electric Motor", "conveyor_lv": "LV Conveyor Module", "piston_lv": "LV Electric Piston",
        "robot_arm_lv": "LV Robot Arm", "circuit_lv": "LV Circuit"}
TOOLS = {"File": "File", "Hammer": "Hammer", "Saw": "Saw"}


def _lang():
    out = {}
    for line in LANG.read_text(encoding="utf-8").splitlines():
        m = re.match(r"item\.gregguns\.(\w+)\.name=(.+)", line)
        if m:
            out[m.group(1)] = m.group(2)
    return out


NAMES = _lang()


def recipes_json() -> dict:
    """The rows written by RecipeExportTest (GREGGUNS_EXPORT_RECIPES)."""
    p = Path(os.environ.get("GREGGUNS_EXPORT_RECIPES") or K.WORK / "recipes.json")
    if not p.exists():
        raise SystemExit(f"No recipe dump at {p}. Run:\n  GREGGUNS_EXPORT_RECIPES={p} ./gradlew test --rerun "
                         "--tests com.gregguns.tools.RecipeExportTest")
    return json.loads(p.read_text(encoding="utf-8"))


# ---------------------------------------------------------------- what an ingredient looks like and is called


def thing(key: str) -> tuple[Path, str]:
    """An ingredient key as (icon, display name). Keys: gt:prefix:Material, mod:name, v:name, meta:name, tool:Kind,
    hull:Tier, cable:Material, machine:name:Tier."""
    kind, _, rest = key.partition(":")
    if kind == "gt":
        prefix, mat = rest.split(":")
        nice = {"Aluminium": "Aluminum", "Wood": "Wooden"}.get(mat, mat)
        if prefix == "dust":
            return gt.item("dust", mat), mat
        icon = gt.pipe(prefix, mat) if prefix.startswith("pipe") else gt.item(prefix, mat)
        return icon, "Stick" if (prefix, mat) == ("stick", "Wood") else PREFIX[prefix].format(nice)
    if kind == "mod":
        return K.item(rest, 64), NAMES.get(rest, rest)
    if kind == "v":
        name, label = VANILLA[rest]
        return V.icon(name), label
    if kind == "meta":
        return gt.metaitem(rest), META[rest]
    if kind == "tool":
        return gt.tool(rest), TOOLS[rest]
    if kind == "hull":
        return S.machine_block("hull", rest, greg=True), ("Bronze Hull" if rest == "Steam" else f"{rest} Machine Hull")
    if kind == "cable":
        return gt.cable(rest), f"1x {rest} Cable"
    if kind == "machine":
        name, tier = rest.split(":")
        return S.machine_block(name, tier), f"{'Steam' if tier == 'Steam' else tier} {name.replace('_', ' ').title()}"
    raise KeyError(key)


def row_key(inp: dict) -> str:
    """An exported row input as a thing() key."""
    if inp["prefix"]:
        return f"gt:{inp['prefix']}:{inp['material']}"
    if inp["item"]:
        return f"mod:{inp['item']}"
    other = inp["special"] or inp["ore"]
    return f"v:{other}"


def rslot(key: str | None, n: int = 1, px: int = 36) -> str:
    """An inventory slot holding an ingredient, with Minecraft's stack count in the corner."""
    pad = max(3, px // 9)
    if key is None:
        return f'<span class="slot" style="padding:{pad}px"><span style="display:block;width:{px}px;height:{px}px"></span></span>'
    icon, name = thing(key)
    cnt = f'<span class="cnt">{n}</span>' if n > 1 else ""
    return (f'<span class="slot" style="padding:{pad}px" title="{esc(name)}"><img class="ico" src="{url(icon)}" '
            f'style="width:{px}px;height:{px}px;display:block;object-fit:contain">{cnt}</span>')


def legend(keys) -> str:
    seen, out = set(), []
    for k in keys:
        if k and k not in seen:
            seen.add(k)
            icon, name = thing(k)
            out.append(f'<span><img src="{url(icon)}">{esc(name)}</span>')
    return f'<div class="legend">{"".join(out)}</div>'


ARROW = '<span class="arrow" style="font-size:30px">&rarr;</span>'


def craft_card(title: str, pattern: list[str], keys: dict, out: str, n: int = 1, note: str = "",
               shapeless: bool = False) -> str:
    """A crafting-table recipe: the 3x3 grid, an arrow and the result, with the ingredients named below."""
    cells = []
    for r in range(3):
        line = pattern[r] if r < len(pattern) else ""
        for c in range(3):
            ch = line[c] if c < len(line) else " "
            cells.append(rslot(keys.get(ch) if ch != " " else None))
    used = [keys[ch] for line in pattern for ch in line if ch != " "]
    tag = '<span class="muted" style="font-size:15px">any order</span>' if shapeless else ""
    note_html = f'<div class="muted" style="font-size:16px">{note}</div>' if note else ""
    return f"""<div class="card ccard"><h3>{esc(title)} {tag}</h3>
<div class="row"><div class="cgrid">{''.join(cells)}</div>{ARROW}{rslot(out, n, 48)}</div>
{legend(used)}{note_html}</div>"""


def machine_row(r: dict, machine: str, note: str = "") -> str:
    """One machine recipe from the dump: the inputs, the output and how it runs."""
    ins = "".join(rslot(row_key(i), i["count"], 32) for i in r["inputs"])
    out_key = f"mod:{r['output']}"
    names = ", ".join(f"{i['count']} &times; {esc(thing(row_key(i))[1])}" if i["count"] > 1 else esc(thing(row_key(i))[1])
                      for i in r["inputs"])
    circuit = f" &middot; Circuit {r['circuit']}" if r["circuit"] else ""
    count = f"{r['count']} &times; " if r["count"] > 1 else ""
    extra = f"<div class=\"m\" style=\"color:var(--txt)\">{note}</div>" if note else ""
    return f"""<div class="mrow"><div class="ins">{ins}</div>{ARROW}{rslot(out_key, r['count'], 44)}
<div class="what"><b>{count}{esc(NAMES.get(r['output'], r['output']))}</b>
<div class="m">{machine} &middot; {r['tier'].title() if r['tier'] == 'STEAM' else r['tier']} &middot; {r['seconds']} s{circuit}</div>
<div class="m">{names}</div>{extra}</div></div>"""


def sec(title: str, *tiers: str) -> str:
    return f'<div class="sec"><h2>{title}</h2>{"".join(chip(t) for t in tiers)}</div>'


# ---------------------------------------------------------------- the crafting-table recipes (not in a table)

STEAM_CRAFTS = [
    # PartCraftingRecipes.registerStoneBarrel (RC-01)
    dict(title="Stone Barrel", pattern=["SSS", "fPh", "SSS"],
         keys={"S": "v:stone", "f": "tool:File", "P": "gt:pipeTiny:Bronze", "h": "tool:Hammer"},
         out="mod:part_barrel_stone"),
    # PartCraftingRecipes.registerWoodenStock (RC-02)
    dict(title="Wooden Stock", pattern=["WW", "sW"], keys={"W": "v:logWood", "s": "tool:Saw"},
         out="mod:part_stock_wooden"),
    # PartCraftingRecipes.registerMechanicalParts (RC-03); Wrought Iron works too
    dict(title="Mechanical Parts", pattern=["hGf", "GSG", " G "],
         keys={"h": "tool:Hammer", "G": "gt:gearGtSmall:Bronze", "f": "tool:File", "S": "gt:springSmall:Bronze"},
         out="mod:part_mechanical_1", note="Wrought Iron gears and springs work too."),
    # HandCannonRecipes.registerGun
    dict(title="Hand Cannon", pattern=["BFS", " Ph"],
         keys={"B": "mod:part_barrel_stone", "F": "v:flint_and_steel", "S": "mod:part_stock_wooden",
               "P": "gt:plate:Bronze", "h": "tool:Hammer"},
         out="mod:handcannon", note="A Wrought Iron plate works too. It comes unloaded."),
    # HandCannonRecipes.registerStoneBulletByHand (RC-27)
    dict(title="Stone Bullets", pattern=["ccc", "gn"], keys={"c": "v:cobblestone", "g": "gt:dust:Gunpowder",
                                                            "n": "gt:nugget:Bronze"},
         out="mod:ammo_stone_bullet", n=16, shapeless=True, note="{press}"),
    # AmmoPressMachines.addCraftingRecipes (the Steam Ammo Press)
    dict(title="Steam Ammo Press", pattern=["XXX", "PMP", "BBB"],
         keys={"X": "gt:pipeSmall:Bronze", "P": "v:craftingPiston", "M": "hull:Steam", "B": "gt:plate:Bronze"},
         out="machine:ammo_press:Steam", note="Runs on steam from your boilers, and runs every LV ammo recipe below too."),
    # ArmorParts.addRecipes (RC-21)
    dict(title="Heavy Cloth", pattern=[" W ", "WLW", " W "], keys={"W": "v:blockWool", "L": "v:leather"},
         out="mod:part_heavy_cloth", n=3, note="Any color of wool."),
    # ArmorTable.SCOUT (AR-01): the rest of the set follows the armor shapes
    dict(title="Bandit Mask", pattern="hand:AR-01", out="mod:t1_scout_helmet",
         note="The rest of the Bandit set takes the usual armor shapes in Heavy Cloth with one Bronze Plate. "
              "The Miner set uses Cast Iron."),
]

LV_CRAFTS = [
    # AmmoPressMachines (the electric press, GT's basic machine pattern VPV / CMC / WEW at LV)
    dict(title="LV Ammo Press", pattern=["VPV", "CMC", "WEW"],
         keys={"V": "meta:conveyor_lv", "P": "meta:piston_lv", "C": "meta:circuit_lv", "M": "hull:LV",
               "W": "cable:Tin", "E": "meta:motor_lv"},
         out="machine:ammo_press:LV"),
    # RepairMachines.addCraftingRecipes, LV row (PRP / WMW / CTC; the plate is the tier's gate material)
    dict(title="LV Repair Bench", pattern=["PRP", "WMW", "CTC"],
         keys={"P": "gt:plate:Steel", "R": "meta:robot_arm_lv", "W": "cable:Tin", "M": "hull:LV",
               "C": "meta:circuit_lv", "T": "v:crafting_table"},
         out="machine:repair_bench:LV"),
]


def _hand(dump, rid, title, out, note):
    h = next(x for x in dump["hand"] if x["id"] == rid)
    keys = {}
    for ch, v in h["keys"].items():
        kind, _, name = v.partition(":")
        if kind == "mod":
            keys[ch] = f"mod:{name}"
        else:
            m = re.match(r"(plate|nugget)([A-Z]\w+)", name)
            keys[ch] = f"gt:{m.group(1)}:{m.group(2)}" if m else f"v:{name}"
    return craft_card(title, h["pattern"], keys, out, note=note)


def recipes():
    dump = recipes_json()
    rows = {r["id"]: r for r in dump["machine"]}
    press = rows["RC-27b"]

    steam = []
    for c in STEAM_CRAFTS:
        if c["pattern"] == "hand:AR-01":
            steam.append(_hand(dump, "AR-01", c["title"], c["out"], c["note"]))
            continue
        note = c.get("note", "").replace(
            "{press}", f"Or {press['count']} per run in the Steam Ammo Press, from the same "
                       f"{press['inputs'][0]['count']} cobblestone, gunpowder and nugget.")
        steam.append(craft_card(c["title"], c["pattern"], c["keys"], c["out"], c.get("n", 1), note,
                                c.get("shapeless", False)))

    machines = "".join(craft_card(c["title"], c["pattern"], c["keys"], c["out"]) for c in LV_CRAFTS)
    gt_machines = "".join(
        f'<div class="stack">{img(S.machine_block(m, "LV", greg=True), h=86)}<small>{n}</small></div>'
        for m, n in (("assembler", "Assembler"), ("lathe", "Lathe"), ("bender", "Bending Machine"),
                     ("extruder", "Extruder")))
    workshop = f"""<div class="grid" style="grid-template-columns:repeat(3,minmax(0,1fr))">{machines}
<div class="card ccard"><h3>GregTech machines you'll use</h3>
<div style="display:grid;grid-template-columns:1fr 1fr;gap:8px">{gt_machines}</div>
<div class="muted" style="font-size:16px">The Assembler builds every part, gun and armor piece below. The others make the GT plates, rings, screws, foil and rounds.</div></div></div>"""

    asm = "GT Assembler"
    parts = "".join(machine_row(rows[i], asm) for i in ("RC-05", "RC-04", "RC-11", "RC-13"))
    guns = "".join(machine_row(rows[i], asm) for i in ("GR-01", "GR-02", "GR-03", "GR-04"))
    ammo = "".join(machine_row(rows[i], "Ammo Press") for i in ("RC-28", "RC-29", "RC-30", "RC-37", "RC-44"))
    combat = [rows[i] for i in ("AR-09", "AR-10", "AR-11", "AR-12")]
    rest = ", ".join(f"{NAMES.get(r['output'], r['output'])}: {r['inputs'][0]['count']} plates and "
                     f"{r['inputs'][1]['count']} cloth" for r in combat[1:])
    gear = "".join([machine_row(rows["GR-05"], asm), machine_row(rows["GR-06"], asm),
                    machine_row(rows["GR-07"], asm), machine_row(combat[0], asm, note=f"The rest of the set: {rest}."),
                    machine_row(rows["RC-61"], asm)])

    tools = legend(["tool:File", "tool:Hammer", "tool:Saw"])
    body = f"""{sec("Steam age &middot; at the crafting table", "Steam")}
<div class="muted" style="font-size:17px;margin:-4px 0 12px;display:flex;align-items:center;gap:14px">GregTech tools in a recipe lose a little durability each craft: {tools}</div>
<div class="grid" style="grid-template-columns:repeat(4,minmax(0,1fr))">{''.join(steam)}</div>
{sec("LV &middot; set up the workshop", "LV")}
{workshop}
<div class="grid" style="grid-template-columns:1fr 1fr;gap:18px">
<div>{sec("Parts in the Assembler", "LV")}<div style="display:flex;flex-direction:column;gap:8px;--inw:140px">{parts}</div></div>
<div>{sec("Guns in the Assembler", "LV")}<div style="display:flex;flex-direction:column;gap:8px">{guns}</div></div>
<div>{sec("Ammo in the Ammo Press", "Steam", "LV")}<div style="display:flex;flex-direction:column;gap:8px;--inw:140px">{ammo}</div></div>
<div>{sec("Gear in the Assembler", "LV")}<div style="display:flex;flex-direction:column;gap:8px;--inw:140px">{gear}</div></div></div>
{_guide()}"""
    return page_doc("Recipes &middot; <b>Steam and LV</b>",
                    "The crafting chains behind your first guns, ammo and armor. Past LV, NEI shows the way.",
                    body, extra=RECIPE_CSS, credit=MOJANG_CREDIT)


def _guide():
    from pages_more import guide_row
    return guide_row("Hover any item in NEI and press <b>R</b> for its recipe or <b>U</b> for what it makes. "
                     "&ldquo;Circuit 2&rdquo; means a Programmed Circuit set to 2 goes in the Assembler with the parts.",
                     pose=POINT, flip=True, yaw=-20)
