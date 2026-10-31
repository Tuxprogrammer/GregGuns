"""The icbm-recipes guide sheet: every ICBM crafting chain, read from a real-pack recipe export.

The rows come from `/gregguns dumprecipes` run on a GT New Horizons server with the ICBM module on (see
docs/gregguns-spec/08-machines-production/icbm-recipe-clash-review.md for the server setup). The sheet reads the dump
from $GREGGUNS_RECIPE_DUMP, or build/infographics/recipe-dump.json(.gz), and prints the rows the pack really registers:
machine, voltage, time, programmed circuit and every input. The dump is the whole pack (about 250,000 rows); only the
rows whose output is ICBM content are used.

Names come from the language files: GregGuns' own, and the English files inside the pack's GregTech, Galacticraft and
NewHorizonsCoreMod jars. GregTech component items (sensors, emitters, field generators...) are looked up by their item
number in `GT_PARTS`; a key the sheet cannot name stops the build, so a new ingredient is never printed as an id.

The dump is checked against the code: each Inert Warhead's input list must match `InertWarheadSpec` (count by count),
and each machine recipe must still be in its Java source, so a dump older than the recipes stops the build.

Icons: GregGuns' own item art (the NTM-derived icons, LGPL-3.0), GregTech's items, hulls and material icons from the GT
jar (LGPL-3.0), Galacticraft Legacy's rocket parts and NewHorizonsCoreMod's Heavy Duty Plate Tier 4 from
inputs/thirdparty/ (MIT and GPL-3.0). Ingredients without a licensed icon are printed by name only.

Run on its own:  python tools/asset-gen/infographics/pages_icbm_recipes.py
"""

from __future__ import annotations

import gzip
import json
import os
import re
import sys
import zipfile
from functools import lru_cache
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from PIL import Image  # noqa: E402

import blocks as B  # noqa: E402
import gt  # noqa: E402
import kit as K  # noqa: E402
import pages_icbm as WH  # noqa: E402
import scenes as S  # noqa: E402
import thirdparty as TP  # noqa: E402
from kit import chip, esc  # noqa: E402
from pages import page_doc  # noqa: E402

JAVA = K.REPO / "src/main/java/com/gregguns"
NTM_ITEMS = K.ASSETS / "textures/items/thirdparty/ntm"
NTM_MACHINES = K.ASSETS / "textures/blocks/machines/thirdparty/ntm"
# GT material colors and icon sets (MaterialsInit at the pack's GT tag) for the ingredients the base table lacks.
gt.MATERIALS.update({
    "Copper": ((255, 100, 0), "DULL"), "Electrum": ((255, 255, 100), "SHINY"), "Platinum": ((255, 255, 200), "SHINY"),
    "Gold": ((255, 255, 30), "SHINY"), "Polytetrafluoroethylene": ((100, 100, 100), "DULL"),
    "Uranium235": ((70, 250, 70), "SHINY"), "Plutonium": ((240, 50, 50), "METALLIC"), "Plutonium241": ((250, 70, 70), "SHINY"),
    "SteelMagnetic": ((128, 128, 128), "MAGNETIC"), "Coal": ((70, 70, 70), "ROUGH"), "Charcoal": ((100, 70, 70), "FINE"),
    "Ruby": ((255, 100, 100), "RUBY"), "Sapphire": ((100, 100, 200), "GEM_VERTICAL"), "GarnetYellow": ((200, 200, 80), "RUBY"),
    "TungstenSteel": gt.MATERIALS["Tungstensteel"],
})
VOLTS = ["ULV", "LV", "MV", "HV", "EV", "IV", "LuV", "ZPM", "UV", "UHV", "UEV"]


# ---------------------------------------------------------------- the dump


def _dump_path() -> Path:
    env = os.environ.get("GREGGUNS_RECIPE_DUMP")
    if env:
        return Path(env)
    for name in ("recipe-dump.json.gz", "recipe-dump.json"):
        if (K.WORK / name).exists():
            return K.WORK / name
    raise SystemExit("No recipe dump. On a GTNH server with debug.recipeDump=true run `/gregguns dumprecipes <file>`, then set "
                     "GREGGUNS_RECIPE_DUMP to the file or copy it to build/infographics/recipe-dump.json.gz")


@lru_cache(maxsize=1)
def dump() -> dict:
    p = _dump_path()
    with (gzip.open(p, "rt", encoding="utf-8") if p.suffix == ".gz" else open(p, encoding="utf-8")) as f:
        return json.load(f)


def rows(pred) -> list[dict]:
    return [r for r in dump()["rows"] if r["outputs"] and pred(r)]


def base(key: str) -> str:
    """A dump key without its `#nbt<hash>` suffix (stacks that carry NBT, such as a warhead with its payload)."""
    return key.split("#", 1)[0]


def out_key(r: dict) -> str:
    return base(r["outputs"][0]["k"])


def by_output(key: str, mp: str | None = None, circuit: int | None = None) -> list[dict]:
    got = rows(lambda r: out_key(r) == key and (mp is None or r["map"] == mp) and (circuit is None or r["circuit"] == circuit))
    if not got:
        raise SystemExit(f"the recipe dump has no {mp or 'recipe'} for {key}; dump the pack again or update pages_icbm_recipes")
    return got


def inert_row(t: int) -> dict:
    """The Inert Warhead's own recipe (circuit 2): the one row for it that takes no inert warhead in."""
    got = [r for r in by_output(f"gregguns:inert_warhead_t{t}:0", "gt.recipe.assembler", 2)
           if not any("inert_warhead" in i["k"] for i in r["inputs"])]
    if len(got) != 1:
        raise SystemExit(f"expected one Inert T{t} Warhead recipe in the dump, found {len(got)}")
    return got[0]


# ---------------------------------------------------------------- names


def _lang(text: str) -> dict:
    out = {}
    for ln in text.splitlines():
        if "=" in ln and not ln.startswith("#"):
            k, v = ln.split("=", 1)
            out[k.strip()] = v.strip()
    return out


@lru_cache(maxsize=1)
def our_lang() -> dict:
    return _lang((K.ASSETS / "lang/en_US.lang").read_text(encoding="utf-8"))


def _mods_dir() -> Path:
    for d in gt._reference_dirs():
        for m in sorted(d.glob("*/.minecraft/mods")):
            return m
    raise SystemExit("reference pack not found (python -I tools/gtnh-reference/gtnh_ref.py fetch)")


@lru_cache(maxsize=None)
def jar_lang(jar_glob: str, path: str) -> dict:
    jars = sorted(_mods_dir().glob(jar_glob))
    if not jars:
        raise SystemExit(f"no {jar_glob} in the reference pack")
    with zipfile.ZipFile(jars[0]) as z:
        return _lang(z.read(path).decode("utf-8"))


def gt_lang() -> dict:
    return jar_lang("gregtech-*.jar", "assets/gregtech/lang/en_US.lang")


# GregTech component items by gt.metaitem.01 number: their lang key (MetaGeneratedItem01 at the pack's GT tag).
GT_PARTS = {
    32538: "gt.item.battery.re.hv.lithium.name", 32602: "gt.item.electric_motor.hv.name",
    32612: "gt.item.electric_pump.hv.name", 32652: "gt.item.robot_arm.hv.name",
    32672: "gt.item.field_generator.hv.name", 32673: "gt.item.field_generator.ev.name",
    32675: "gt.item.field_generator.luv.name", 32676: "gt.item.field_generator.zpm.name",
    32677: "gt.item.field_generator.uv.name", 32678: "gt.item.field_generator.uhv.name",
    32682: "gt.item.emitter.hv.name", 32683: "gt.item.emitter.ev.name", 32684: "gt.item.emitter.iv.name",
    32685: "gt.item.emitter.luv.name", 32687: "gt.item.emitter.uv.name", 32688: "gt.item.emitter.uhv.name",
    32692: "gt.item.sensor.hv.name", 32693: "gt.item.sensor.ev.name", 32694: "gt.item.sensor.iv.name",
    32695: "gt.item.sensor.luv.name", 32697: "gt.item.sensor.uv.name", 32698: "gt.item.sensor.uhv.name",
    32708: "gt.item.tool.data_stick.name", 32712: "gt.item.circuit_board.elite.name", 32724: "gt.item.quantum_eye.name",
}
GT_PARTS3 = {32102: "gt.item.circuit_board.epoxy_advanced.name", 32106: "gt.item.circuit_board.plastic_advanced.name"}
# Names a lang key spells differently in the pack, or that no lang file in the pack carries.
FIXED = {
    "gregtech:gt.blockmachines:13": "HV Machine Hull", "gregtech:gt.blockmachines:14": "EV Machine Hull",
    "gregtech:gt.blockmachines:15": "IV Machine Hull",
    "IC2:blockITNT:0": "Industrial TNT", "IC2:itemCellEmpty:0": "Empty Cell", "bartworks:BioLabParts:0": "Petri Dish",
    "harvestcraft:mustardseedsItem:0": "Mustard Seeds", "minecraft:ender_eye:0": "Eye of Ender",
    "minecraft:nether_star:0": "Nether Star",
    "GalacticraftMars:item.null:3": "Heavy Duty Plate (Tier 2)",
}
GC_KEYS = {
    "GalacticraftCore:item.basicItem:9": ("galacticraftcore", "item.basicItem.compressedSteel.name"),
    "GalacticraftCore:item.engine:0": ("galacticraftcore", "item.engine.tier1engine.name"),
    "GalacticraftCore:item.rocketFins:0": ("galacticraftcore", "item.rocketFins.name"),
    "GalacticraftCore:item.noseCone:0": ("galacticraftcore", "item.noseCone.name"),
    "GalacticraftCore:item.heavyPlating:0": ("galacticraftcore", "item.heavyPlating.name"),
    "GalacticraftMars:item.heavyNoseCone:0": ("galacticraftasteroids", "item.heavyNoseCone.name"),
    "GalacticraftMars:item.itemBasicAsteroids:1": ("galacticraftasteroids", "item.engineT2.name"),
    "GalacticraftMars:item.itemBasicAsteroids:2": ("galacticraftasteroids", "item.rocketFinsT2.name"),
    "GalacticraftMars:item.itemBasicAsteroids:6": ("galacticraftasteroids", "item.compressedTitanium.name"),
}
CIRCUIT_TIER = {"Advanced": "HV", "Data": "EV", "Elite": "IV", "Master": "LuV", "Ultimate": "ZPM",
                "Superconductor": "UV", "Infinite": "UHV"}
PREFIX = {"plate": "{} Plate", "ingot": "{} Ingot", "stick": "{} Rod", "round": "{} Round", "lens": "{} Lens",
          "dust": "{} Dust", "wireFine": "Fine {} Wire"}
MATERIAL = {"StainlessSteel": "Stainless Steel", "TungstenSteel": "Tungstensteel", "Polytetrafluoroethylene": "Polytetrafluoroethylene",
            "SteelMagnetic": "Magnetic Steel", "Uranium235": "Uranium 235", "Plutonium241": "Plutonium 241",
            "GarnetYellow": "Yellow Garnet", "Plutonium": "Plutonium 239"}
FLUID = {"fluid:lubricant": "Lubricant", "fluid:fluid.kerosene": "Kerosene", "fluid:nitricacid": "Nitric Acid",
         "fluid:gasoline": "Gasoline", "fluid:nitrousoxide": "Nitrous Oxide", "fluid:ammonia": "Ammonia",
         "fluid:creosote": "Creosote", "fluid:helium": "Helium", "fluid:biomass": "Biomass"}
MACHINE_NAME = {20140: "launchcontrol", 20141: "missilesilo", 20142: "radar.hv", 20143: "radar.ev", 20144: "radartower",
                20145: "airdefense", 20146: "emptower"}
MAP_NAME = {"gt.recipe.assembler": "Assembler", "gregguns.missileWorkbench": "NASA Workbench",
            "gt.recipe.circuitassembler": "Circuit Assembler", "gt.recipe.laserengraver": "Laser Engraver",
            "gt.recipe.fakeAssemblylineProcess": "Assembly Line", "gt.recipe.scanner": "Scanner",
            "gt.recipe.chemicalreactor": "Chemical Reactor", "gt.recipe.macerator": "Macerator"}


def name(key: str) -> str:
    """The in-game name of a dump item key (mod:item:meta)."""
    key = base(key)
    if key in FIXED:
        return FIXED[key]
    if key in FLUID:
        return FLUID[key]
    mod, item, meta = key.rsplit(":", 2) if key.count(":") >= 2 else (key, "", "0")
    if mod == "gregguns":
        lang = our_lang()
        for k in (f"item.gregguns.{item}.{meta}.name", f"item.gregguns.{item}.name", f"tile.gregguns.{item}.name",
                  f"gregguns.{item}.{meta}.name"):
            if k in lang:
                return lang[k]
    if mod == "gregtech" and item == "gt.blockmachines" and int(meta) in MACHINE_NAME:
        return our_lang()[f"gt.blockmachines.gregguns.icbm.{MACHINE_NAME[int(meta)]}.name"]
    data = dump()["itemdata"].get(key, {})
    if data.get("p") == "circuit" and data.get("m") in CIRCUIT_TIER:
        return f"{CIRCUIT_TIER[data['m']]} circuit"
    if data.get("p") in PREFIX and data.get("m"):
        m = data["m"]
        return PREFIX[data["p"]].format(MATERIAL.get(m, re.sub(r"(?<=[a-z])(?=[A-Z])", " ", m)))
    if mod == "gregtech" and item == "gt.metaitem.01" and int(meta) in GT_PARTS:
        return gt_lang()[GT_PARTS[int(meta)]]
    if mod == "gregtech" and item == "gt.metaitem.03" and int(meta) in GT_PARTS3:
        return gt_lang()[GT_PARTS3[int(meta)]]
    if key in GC_KEYS:
        folder, k = GC_KEYS[key]
        return jar_lang("Galacticraft-*.jar", f"assets/{folder}/lang/en_US.lang")[k]
    if mod == "dreamcraft":
        n = jar_lang("GTNewHorizonsCoreMod-*.jar", "assets/dreamcraft/lang/en_US.lang").get(f"item.{item}.name")
        if n:
            return n
    raise SystemExit(f"no name for {key}; add it to pages_icbm_recipes (FIXED, GT_PARTS or GC_KEYS)")


# ---------------------------------------------------------------- icons


def _square(p: Path, tag: str) -> Path:
    """The first frame of an animated strip, as its own file."""
    im = Image.open(p).convert("RGBA")
    if im.height > im.width:
        im = im.crop((0, 0, im.width, im.width))
    out = K.TEXCACHE / f"rc-{tag}.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    im.save(out)
    return out


def _gt_item_tex(folder: str, n: int) -> Path | None:
    path = f"assets/gregtech/textures/items/{folder}/{n}.png"
    if not gt._has(path):
        return None
    return gt._save(gt._read(path).crop((0, 0, 16, 16)) if gt._read(path).height > gt._read(path).width else gt._read(path), f"{folder}-{n}")


GT_CIRCUIT_ICON = {"HV": 82, "EV": 85, "IV": 89, "LuV": 92, "ZPM": 93, "UV": 94, "UHV": 95}  # one GT circuit of the tier
NHCORE_ICONS = {"dreamcraft:HeavyDutyPlateTier4:0": TP.NHCORE / "heavy_duty_plate_t4.png"}
GC_ICONS = {"GalacticraftCore:item.basicItem:9": "compressed_steel", "GalacticraftCore:item.engine:0": "rocket_engine_t1",
            "GalacticraftCore:item.rocketFins:0": "rocket_fins_t1", "GalacticraftMars:item.itemBasicAsteroids:1": "rocket_engine_t2",
            "GalacticraftMars:item.itemBasicAsteroids:2": "rocket_fins_t2", "GalacticraftMars:item.itemBasicAsteroids:6": "compressed_titanium"}
OWN_ICONS = {"remote_detonator_hv": "remote_detonator", "remote_detonator_ev": "remote_detonator",
             "remote_detonator_iv": "remote_detonator", "sam": "sam_missile", "fallout_goo_item": "fallout_goo"}
MACHINE_ICON = {20140: ("launch_control", "HV"), 20141: ("missile_silo", "HV"), 20142: ("radar_hv", "HV"),
                20143: ("radar_ev", "EV"), 20144: ("radar_tower", "IV"), 20145: ("air_defense_battery", "HV"),
                20146: ("emp_tower", "EV")}


def machine_icon(folder: str, tier: str) -> Path:
    faces = {}
    for face in ("FRONT", "SIDE", "TOP"):
        hull = gt.hull(tier, "TOP" if face == "TOP" else "SIDE")
        faces[face] = B.composite(hull, NTM_MACHINES / folder / f"OVERLAY_{face}.png", cache=K.TEXCACHE, name=f"rc-{folder}-{tier}-{face}")
    return K.voxels(f"rc-{folder}-{tier}", {(0, 0, 0): ("m", S.FRONT)}, {"m": (faces["TOP"], faces["SIDE"], faces["FRONT"])},
                    yaw=-38, pitch=28, ppu=110)


def cube(tex: Path, tag: str) -> Path:
    return K.voxels(f"rc-cube-{tag}", {(0, 0, 0): ("m", S.FRONT)}, {"m": (tex, tex, tex)}, yaw=-38, pitch=28, ppu=110)


def icon(key: str) -> Path | None:
    """A licensed icon for a dump key, or None (the ingredient is then named only)."""
    key = base(key)
    mod, item, meta = key.rsplit(":", 2) if key.count(":") >= 2 else (key, "", "0")
    if mod == "gregguns":
        if item.startswith(("warhead_t", "inert_warhead_t")):
            return WH.warhead("warhead_t" + item[-1], ppu=60)
        if item.startswith("missile_body_t"):
            return None
        if item == "casings" and meta == "11":
            return cube(K.ASSETS / "textures/blocks/casings/thirdparty/ntm/radar_casing.png", "radar_casing")
        stem = OWN_ICONS.get(item, item)
        for p in (NTM_ITEMS / f"{stem}.png", K.ASSETS / f"textures/items/{stem}.png"):
            if p.exists():
                return _square(p, stem)
        return None
    if mod == "gregtech" and item == "gt.blockmachines":
        m = int(meta)
        if m in MACHINE_ICON:
            return machine_icon(*MACHINE_ICON[m])
        return cube(gt.hull_tinted({13: "HV", 14: "EV", 15: "IV"}[m]), f"hull-{m}")
    data = dump()["itemdata"].get(key, {})
    if data.get("p") == "circuit" and data.get("m") in CIRCUIT_TIER:
        return _gt_item_tex("gt.metaitem.03", GT_CIRCUIT_ICON[CIRCUIT_TIER[data["m"]]])
    if mod == "gregtech" and item == "gt.metaitem.01" and int(meta) >= 32000:
        return _gt_item_tex("gt.metaitem.01", int(meta) - 32000)
    if mod == "gregtech" and item == "gt.metaitem.03":
        return _gt_item_tex("gt.metaitem.03", int(meta) - 32000)
    if data.get("p") in ("plate", "ingot", "stick", "round", "lens", "dust", "wireFine") and data.get("m") in gt.MATERIALS:
        return gt.item(data["p"], data["m"])
    if key in GC_ICONS:
        return TP.gc_icon(GC_ICONS[key])
    if key in NHCORE_ICONS:
        return NHCORE_ICONS[key]
    return None


# ---------------------------------------------------------------- freshness: the dump must match the code


def _src(rel: str) -> str:
    return (JAVA / rel).read_text(encoding="utf-8")


def _verify() -> None:
    """The dump is a snapshot of a server; the code moves on. Each Inert Warhead row must hold the inputs, count for count,
    that InertWarheadSpec gives it, and each machine recipe's inputs must still be named in its source."""
    spec = _src("icbm/warhead/InertWarheadSpec.java")
    consts = {k: int(v) for k, v in re.findall(r"static final int (\w+) = (\d+);", _src("icbm/warhead/WarheadRecipes.java"))}
    for t in range(1, 6):
        counts = []
        for x in WH.entry(spec, f"T{t}"):
            m = re.fullmatch(r"\w+\((.*)\)", x)
            if m:
                c = m.group(1).split(",")[-1]
                counts.append(consts[c.split(".")[-1]] if not c.isdigit() else int(c))
        got = sorted(i["n"] for i in inert_row(t)["inputs"])
        if sorted(counts) != got:
            raise SystemExit(f"the recipe dump's Inert T{t} Warhead ({got}) differs from InertWarheadSpec ({sorted(counts)}); dump the pack again")
    machines = {"icbm/control/ControlMachines.java": ("Sensor_HV", "Emitter_HV", "Tool_DataStick"),
                "icbm/defense/DefenseMachines.java": ("Robot_Arm_HV", "Sensor_IV", "Emitter_IV"),
                "icbm/emp/EmpMachines.java": ("Field_Generator_EV", "Battery_RE_HV_Lithium"),
                "icbm/silo/SiloMachines.java": ("Electric_Pump_HV", "Robot_Arm_HV")}
    for rel, names in machines.items():
        s = _src(rel)
        for n in names:
            if n not in s:
                raise SystemExit(f"{rel} no longer uses {n}; dump the pack again and check icbm-recipes")


# ---------------------------------------------------------------- drawing


def volt(eut: int) -> str:
    v = 8
    for i, n in enumerate(VOLTS):
        if eut <= v:
            return n
        v *= 4
    return VOLTS[-1]


def _slot(p: Path | None, px: int = 34) -> str:
    if p is None:
        return ""
    return f'<span class="ri"><img src="{K.url(p)}" style="width:{px - 6}px;height:{px - 6}px"></span>'


def pill(key: str, n: int, fluid: bool = False) -> str:
    amount = f"{n:,} L" if fluid else (f"{n} &times;" if n > 1 else "")
    pic = "" if fluid else _slot(icon(key))
    return f'<span class="ing">{pic}<span><b>{amount}</b> {esc(name(key))}</span></span>'


def meta(r: dict) -> str:
    m = MAP_NAME.get(r["map"], r["map"])
    parts = [f"<b>{m}</b>"]
    if r["eut"]:
        parts.append(chip(volt(r["eut"])))
        parts.append(f'{r["duration"] / 20:g} s')
    if r["circuit"]:
        parts.append(f'circuit {r["circuit"]}')
    return '<span class="meta">' + " &middot; ".join(parts) + "</span>"


def recipe(r: dict, title: str | None = None, big: Path | None = None, note: str = "") -> str:
    o = r["outputs"][0]
    pic = big if big is not None else icon(o["k"])
    head = (f'<div class="rh">{_slot(pic, 52) if pic else ""}<div><h3>{esc(title or name(o["k"]))}'
            f'{f" <span class=cnt>&times; {o[chr(110)]}</span>" if o["n"] > 1 else ""}</h3>{meta(r)}</div></div>')
    ins = "".join(pill(i["k"], i["n"]) for i in r["inputs"]) + "".join(pill(f["k"], f["n"], fluid=True) for f in r["fluids"])
    n = f'<div class="rn">{note}</div>' if note else ""
    return f'<div class="rc">{head}<div class="ins">{ins}</div>{n}</div>'


CSS = """
.sec { margin-top: 24px; } .sec > h2 { margin: 0 0 10px; }
.lead { font-size: 19px; color: var(--muted); line-height: 1.38; margin-bottom: 12px; } .lead b { color: var(--txt); }
.rgrid { display: grid; gap: 12px; }
.rc { background: rgba(255,255,255,.035); border: 1px solid var(--line); border-radius: 12px; padding: 10px 12px 10px; }
.rh { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.rh h3 { font-size: 21px; margin: 0 0 3px; } .rh .cnt { color: var(--orange); font-size: 18px; }
.meta { font-size: 15px; color: var(--muted); display: flex; align-items: center; gap: 6px; flex-wrap: wrap; } .meta b { color: var(--txt); font-weight: 600; }
.ri { display: inline-flex; align-items: center; justify-content: center; flex: none; background: #8b8b8b; border: 2px solid;
  border-color: #373737 #ffffff #ffffff #373737; border-radius: 2px; }
.ri img { image-rendering: pixelated; }
.ins { display: flex; flex-wrap: wrap; gap: 6px 8px; }
.ing { display: inline-flex; align-items: center; gap: 6px; font-size: 15.5px; color: var(--muted); background: rgba(255,255,255,.05);
  border: 1px solid var(--line); border-radius: 8px; padding: 2px 9px 2px 3px; min-height: 36px; }
.ing b { color: var(--txt); font-weight: 600; }
.rn { font-size: 15px; color: var(--muted); margin-top: 7px; line-height: 1.3; } .rn b { color: var(--txt); }
.arrow { font-size: 34px; color: var(--orange); text-align: center; align-self: center; }
table.t { width: 100%; border-collapse: collapse; font-size: 16px; }
table.t th { text-align: left; font-family: Bahnschrift, "Fira Sans Condensed", sans-serif; letter-spacing: .1em; text-transform: uppercase;
  font-size: 13px; color: var(--muted); padding: 6px 8px; border-bottom: 1px solid var(--line); }
table.t td { padding: 7px 8px; border-bottom: 1px solid var(--line); color: var(--muted); vertical-align: middle; line-height: 1.25; }
table.t td:first-child { color: var(--txt); font-weight: 600; white-space: nowrap; }
table.t tr:last-child td { border-bottom: 0; } table.t td b { color: var(--txt); }
.cell { display: flex; align-items: center; gap: 6px; }
.t-UV { background: #2ee6c8; } .t-UHV { background: #b6f060; }
"""


def inert_table() -> str:
    head = "<tr><th>Inert warhead</th><th>Casing</th><th>Fuze</th><th>Nose</th><th>Hull</th><th>Sensor and emitter</th><th>Circuits</th><th>Assembler</th></tr>"
    body = []
    for t in range(1, 6):
        r = inert_row(t)
        ins = {i["k"]: i["n"] for i in r["inputs"]}

        def pick(test):
            ks = [k for k in ins if test(k)]
            return "".join(f'<div class="cell">{_slot(icon(k), 30)}<span><b>{ins[k]}</b> {esc(name(k))}</span></div>' for k in ks)
        data = dump()["itemdata"]
        casing = pick(lambda k: data.get(k, {}).get("p") == "plate")
        fuze = pick(lambda k: "fuze_assembly" in k)
        nose = pick(lambda k: "NoseCone" in k or "noseCone" in k)
        hull = pick(lambda k: "heavyPlating" in k or "HeavyDutyPlate" in k or k == "GalacticraftMars:item.null:3")
        elec = pick(lambda k: k.startswith("gregtech:gt.metaitem.01:326") or k.startswith("gregtech:gt.metaitem.01:3269"))
        circ = pick(lambda k: data.get(k, {}).get("p") == "circuit")
        if len(ins) != 7 or not all((casing, fuze, nose, hull, elec, circ)):
            raise SystemExit(f"Inert T{t} Warhead's inputs no longer fit the table: {sorted(ins)}")
        body.append(f'<tr><td><div class="cell">{_slot(icon(f"gregguns:inert_warhead_t{t}:0"), 44)}T{t}</div></td><td>{casing}</td><td>{fuze}</td><td>{nose}</td>'
                    f'<td>{hull}</td><td>{elec}</td><td>{circ}</td><td>{chip(volt(r["eut"]))} {r["duration"] / 20:g} s</td></tr>')
    return f'<table class="t">{head}{"".join(body)}</table>'


def build_page() -> str:
    _verify()
    a = "gt.recipe.assembler"
    parts = ["plastic_explosive", "fuze_assembly_mk1", "fuze_assembly_mk2", "fuze_assembly_mk3", "dispersal_canister",
             "napalm_canister", "fragmentation_sleeve_steel", "fragmentation_sleeve_titanium", "shaped_charge_liner_cone"]
    part_cards = "".join(recipe(by_output(f"gregguns:{p}:0", a)[0]) for p in parts)

    fill1 = by_output("gregguns:warhead_t1:0", a)[0]
    fills = {t: by_output(f"gregguns:warhead_t{t}:0", a)[0] for t in range(1, 6)}
    fill_txt = ", ".join(f"T{t} {next(i['n'] for i in r['inputs'] if 'plastic_explosive' in i['k'])}" for t, r in fills.items())

    guidance = "".join(recipe(by_output(f"gregguns:guidance_computer_mk{n}:0", a)[0]) for n in (1, 2, 3))
    chips = "".join(recipe(by_output(f"gregguns:missile_chip_t{n}:0", "gt.recipe.circuitassembler")[0]) for n in (1, 2, 3))
    schem = "".join(recipe(by_output(f"gregguns:missile_schematic_t{n}:0", "gt.recipe.laserengraver")[0]) for n in (1, 2, 3))
    bodies = []
    for n in (1, 2, 3):
        wb = by_output(f"gregguns:missile_body_t{n}:0", "gregguns.missileWorkbench")
        comps = sorted({name(i["k"]) for r in wb for i in r["inputs"] if "guidance_computer" in i["k"]})
        bodies.append(recipe(wb[0], note="With the schematic in the NASA Workbench. " + (
            f"The Guidance Computer can be {' or '.join(comps)}." if len(comps) > 1 else "")))
    for n in (1, 2):
        bodies.append(recipe(by_output(f"gregguns:missile_body_t{n}:0", a)[0], note="The Assembler builds it too, with no schematic."))
    al = by_output("gregguns:missile_body_t3:0", "gt.recipe.fakeAssemblylineProcess")[0]
    scan = by_output("gregguns:missile_body_t3:0", "gt.recipe.scanner")[0]
    bodies.append(recipe(al, note=f"Unlocked by scanning a hand-built Missile Body T3 onto a Data Stick in the <b>Scanner</b> "
                                  f"({chip(volt(scan['eut']))} {scan['duration'] / 20:g} s)."))

    machines = "".join(recipe(by_output(f"gregtech:gt.blockmachines:{m}", a)[0]) for m in range(20140, 20147))
    machines += recipe(by_output("gregguns:casings:11", a)[0]) + recipe(by_output("gregguns:sam:0", a)[0])

    tools = "".join(recipe(by_output(f"gregguns:{t}:0", a)[0]) for t in
                    ("radar_gun", "remote_detonator_hv", "remote_detonator_ev", "remote_detonator_iv", "defuser", "antidote"))
    lw = sorted({out_key(r) for r in rows(lambda r: out_key(r).startswith("gregguns:launcher_warhead_") and r["map"] == a)})
    launcher = "".join(recipe(by_output(k, a)[0]) for k in lw)

    body = f"""
<div class="lead">Every ICBM part, warhead, missile body and machine, as the pack builds it: the machine and its voltage, the time, the programmed circuit and every input. Most of it is <b>Assembler</b> work; the missile bodies add the <b>NASA Workbench</b>, and the Tier 3 body the <b>Assembly Line</b>. NEI shows the same recipes in game.</div>

<div class="sec"><h2>1 &middot; Warhead parts</h2>
<div class="rgrid" style="grid-template-columns:repeat(3,minmax(0,1fr))">{part_cards}</div></div>

<div class="sec"><h2>2 &middot; Inert warheads</h2>
<div class="lead">The casing, the nose of its rocket tier and the arming electronics, all in one Assembler recipe (circuit 2). An inert warhead cannot be set off: it is the starting point of every warhead.</div>
<div class="card" style="padding:8px 12px">{inert_table()}</div></div>

<div class="sec"><h2>3 &middot; Payload and charges</h2>
<div class="rgrid" style="grid-template-columns:1fr 60px 1fr">
<div class="card"><h3>Payload</h3><div class="lead" style="margin:6px 0 0">The Assembler fits a blast type's payload to an inert warhead: a sleeve, canisters, field generators, nuclear fuel and so on, one recipe per blast type at the casing's voltage. Each is listed on the <b>Warheads</b> and <b>Heavy Warheads</b> guides. Condensed needs no payload.</div></div>
<div class="arrow">&rarr;</div>
{recipe(fill1, title="Charged warhead", note=f"Plastic Explosive fills the casing: a full load is {fill_txt} charges. The circuit picks the blast type. In a crafting grid, 1 to 8 charges go in at a time.")}
</div></div>

<div class="sec"><h2>4 &middot; Missile bodies</h2>
<div class="lead">A <b>Missile Chip</b> from the Circuit Assembler, engraved into a <b>Missile Schematic</b>, unlocks the body's page in Galacticraft's NASA Workbench. Each body takes a <b>Guidance Computer</b>.</div>
<div class="rgrid" style="grid-template-columns:repeat(3,minmax(0,1fr))">{chips}{schem}{guidance}</div>
<div class="rgrid" style="grid-template-columns:repeat(3,minmax(0,1fr));margin-top:12px">{"".join(bodies)}</div></div>

<div class="sec"><h2>5 &middot; Machines</h2>
<div class="rgrid" style="grid-template-columns:repeat(3,minmax(0,1fr))">{machines}</div></div>

<div class="sec"><h2>6 &middot; Tools and supplies</h2>
<div class="rgrid" style="grid-template-columns:repeat(3,minmax(0,1fr))">{tools}</div></div>

<div class="sec"><h2>7 &middot; Launcher warheads</h2>
<div class="lead">Charges for the Rocket Launcher, Grim Reaper, Grenade Launcher and Nuke Launcher. Craft one onto its launcher in a crafting grid; NEI's <b>Launcher Loading</b> page shows which goes where.</div>
<div class="rgrid" style="grid-template-columns:repeat(3,minmax(0,1fr))">{launcher}</div>
<div class="lead" style="margin-top:12px">Fallout cleanup (Trinitite and Fallout Goo) has its own guide, <b>Fallout</b>.</div></div>
"""
    credit = [
        "ICBM item and machine art derived from HBM's Nuclear Tech Mod, GNU LGPL v3; modified.",
        "GregTech items, circuits, hulls and material icons: GregTech 5 Unofficial textures, LGPL-3.0.",
        TP.GC_CREDIT, TP.CORE_CREDIT,
    ]
    return page_doc("ICBM <b>recipes</b>", "Every ICBM crafting chain in GT New Horizons, from GregTech parts to a missile on the pad.",
                    body, extra=CSS, credit=credit)


def recipes() -> str:
    return build_page()


PAGES = {"icbm-recipes": recipes}

if __name__ == "__main__":
    import build
    build.main(["icbm-recipes"])
