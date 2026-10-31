"""The ICBM module's player guide infographics. build.py adds PAGES to the others.

Warhead models are the shipped OBJ files, drawn with render3d.obj_scene (the strict Forge reader), so the sheet shows
what the game draws. Every number comes from the code (D50): the radius, damage and recipe machines from the dump
WarheadExportTest writes (GREGGUNS_EXPORT_WARHEADS, build/infographics/warheads.json), the recipe inputs and effect
durations from the Java sources, which `_verify_sources` checks so a changed recipe stops the build instead of drifting.
Vanilla icons are the real Minecraft textures, read from the Minecraft jar at build time (D55).
"""

from __future__ import annotations

import hashlib
import json
import math
import os
import re

import blocks as B
import gt
import kit as K
import render3d as R
import scenes as S
import vanilla
from kit import chip, esc
from pages import MOJANG_CREDIT, gti, img, page_doc, slot

NTM = K.ASSETS / "models/thirdparty/ntm"
NTM_TEX = K.ASSETS / "textures/models/thirdparty/ntm"
WARHEAD_PPU = 300


def warhead(name: str, yaw=35, pitch=14, ppu=WARHEAD_PPU, mesh: str | None = None) -> K.Path:
    """A shipped warhead model at ppu pixels per block, so warheads drawn side by side keep their true scale.
    `mesh` names the OBJ when it differs from the texture (the ender and anti-gravitational skins use the T3 mesh)."""
    obj, tex = NTM / f"{mesh or name}.obj", NTM_TEX / f"{name}.png"
    sig = hashlib.sha1(obj.read_bytes() + tex.read_bytes()).hexdigest()[:10]

    def make():
        return R.trim(R.render(R.obj_scene(obj, tex), yaw=yaw, pitch=pitch, ppu=ppu * 2, ss=2, outline=K.RIM), 2)
    return K._cached(f"warhead-{name}", dict(sig=sig, y=yaw, p=pitch, u=ppu, v=1), make)


# ---------------------------------------------------------------- numbers, read from the code


def _dump() -> dict:
    """The warhead dump written by WarheadExportTest."""
    p = K.Path(os.environ.get("GREGGUNS_EXPORT_WARHEADS") or K.WORK / "warheads.json")
    if not p.exists():
        raise SystemExit(f"No warhead dump at {p}. Run:\n  GREGGUNS_EXPORT_WARHEADS={p} ./gradlew test --rerun "
                         "--tests com.gregguns.tools.WarheadExportTest")
    return json.loads(p.read_text(encoding="utf-8"))


DUMP = _dump()
BLAST = {b["id"]: b for b in DUMP["blasts"]}
CAP = {int(c["tier"][1]): c["cap"] for c in DUMP["casings"]}
GAS_SECONDS = DUMP["effects"]["gasTicks"] // 20
CHAIN_SECONDS = DUMP["fuse"]["chainTicks"] / 20

_JAVA = K.REPO / "src/main/java/com/gregguns/icbm/warhead"
_RECIPES = (_JAVA / "WarheadRecipes.java").read_text(encoding="utf-8")
_SPEC = (_JAVA / "WarheadRecipeSpec.java").read_text(encoding="utf-8")
_PARTS = (K.REPO / "src/main/java/com/gregguns/icbm/recipe/IcbmPart.java").read_text(encoding="utf-8")
_EFFECTS = (_JAVA / "effects/Effects.java").read_text(encoding="utf-8")
_INERT = (_JAVA / "InertWarheadSpec.java").read_text(encoding="utf-8")
_CHARGING = (_JAVA / "WarheadCharging.java").read_text(encoding="utf-8")
_DEARM = (_JAVA / "WarheadDearming.java").read_text(encoding="utf-8")
_GC = (K.REPO / "src/main/java/com/gregguns/icbm/compat/GalacticraftParts.java").read_text(encoding="utf-8")


# ---------------------------------------------------------------- recipe checks


def _split(s: str) -> list[str]:
    """Splits a Java argument list at its top-level commas (not inside parentheses or strings)."""
    out, depth, cur, in_str = [], 0, [], False
    for c in s:
        if c == '"':
            in_str = not in_str
        elif not in_str and c in "([{":
            depth += 1
        elif not in_str and c in ")]}":
            depth -= 1
        elif not in_str and c == "," and depth == 0:
            out.append("".join(cur))
            cur = []
            continue
        cur.append(c)
    if "".join(cur).strip():
        out.append("".join(cur))
    return out


def _norm(arg: str) -> list[str]:
    """One argument, whitespace removed. An Ing call keeps its method and the arguments a player sees: the Tier and the
    cost estimate (a decimal) are dropped, so a cost tweak passes and a changed input or count does not. Arrays.asList
    is flattened into its items."""
    a = re.sub(r"\s+", "", arg)
    m = re.fullmatch(r"Arrays\.asList\((.*)\)", a)
    if m:
        return [x for part in _split(m.group(1)) for x in _norm(part)]
    m = re.fullmatch(r"Ing\.(\w+)\((.*)\)", a)
    if m:
        kept = [x for x in _split(m.group(2)) if not re.fullmatch(r"Tier\.\w+|-?[0-9]+\.[0-9]+", x)]
        return [f"{m.group(1)}({','.join(kept)})"]
    return [a]


def entry(src: str, name: str) -> list[str]:
    """The arguments of the enum constant `name(...)` in a Java source, each normalized by _norm, so a check compares the
    whole input list: an added, removed or recounted input changes it."""
    m = re.search(rf"^\s*{name}\(", src, re.M)
    if not m:
        raise SystemExit(f"enum constant {name} not found; update the guide sheet checks")
    i = j = m.end()
    depth, in_str = 1, False
    while depth:
        c = src[j]
        if c == '"':
            in_str = not in_str
        elif not in_str and c == "(":
            depth += 1
        elif not in_str and c == ")":
            depth -= 1
        j += 1
    return [x for a in _split(src[i:j - 1]) for x in _norm(a) if not re.fullmatch(r"-?[0-9]+\.[0-9]+", x)]


def check_entries(label: str, src: str, want: dict[str, list[str]]) -> None:
    """Stops the build when an enum constant's inputs differ from what the sheet prints."""
    bad = {n: entry(src, n) for n, w in want.items() if entry(src, n) != w}
    if bad:
        raise SystemExit(f"{label} changed; update the guide sheet and its checks. Now: {bad}")


def check_snippets(label: str, need: list[tuple[str, str]]) -> None:
    missing = [snippet for src, snippet in need if snippet not in src]
    if missing:
        raise SystemExit(f"{label} changed; update the guide sheet and its checks: {missing}")


# Each casing's inert warhead, in player words (InertWarheadSpec). The nose cone and hull plates are those of the
# casing tier's rocket, named as the game names them; the ids pin which item each name is.
INERT = {
    1: dict(volt=3, chip="HV", plate="StainlessSteel", plate_name="stainless steel", plates=4, plates_const="T1_PLATES",
            fuze="MK1", fuze_name="Mk I", nose=("NOSE_T1", "GalacticraftCore:item.noseCone@0", "a Nose Cone"),
            hull=("HULL_T1", "GalacticraftCore:item.heavyPlating@0", "12 Heavy Duty Plates"), etier="HV", circuit="Advanced"),
    2: dict(volt=4, chip="EV", plate="Titanium", plate_name="titanium", plates=8, plates_const="T2_PLATES",
            fuze="MK2", fuze_name="Mk II", nose=("NOSE_HEAVY", "GalacticraftMars:item.heavyNoseCone@0", "a Heavy Nose Cone"),
            hull=("HULL_T2", "GalacticraftMars:item.null@3", "12 Heavy Duty Plates (Tier 2)"), etier="EV", circuit="Data"),
    3: dict(volt=6, chip="LuV", plate="TungstenSteel", plate_name="tungstensteel", plates=8, plates_const="T3_PLATES",
            fuze="MK3", fuze_name="Mk III", nose=("NOSE_HEAVY", "GalacticraftMars:item.heavyNoseCone@0", "a Heavy Nose Cone"),
            hull=("HULL_T3", "dreamcraft:HeavyDutyPlateTier4@0", "12 Heavy Duty Plates Tier 4"), etier="LuV", circuit="Master"),
    4: dict(volt=8, chip="UV", plate="Naquadria", plate_name="naquadria", plates=8, plates_const="T4_PLATES",
            fuze="MK3", fuze_name="Mk III", nose=("NOSE_T4", "dreamcraft:HeavyDutyNoseConeTier3@0", "a Heavy Duty Nose Cone Tier 3"),
            hull=("HULL_T4", "dreamcraft:HeavyDutyPlateTier6@0", "12 Heavy Duty Plates Tier 6"), etier="UV", circuit="Superconductor"),
    5: dict(volt=9, chip="UHV", plate="Neutronium", plate_name="neutronium", plates=8, plates_const="T5_PLATES",
            fuze="MK3", fuze_name="Mk III", nose=("NOSE_T5", "dreamcraft:HeavyDutyNoseConeTier4@0", "a Heavy Duty Nose Cone Tier 4"),
            hull=("HULL_T5", "dreamcraft:HeavyDutyPlateTier7@0", "12 Heavy Duty Plates Tier 7"), etier="UHV", circuit="Infinite"),
}


def inert_words(t: int, fuze_full=True) -> str:
    """The inputs of a casing's inert warhead in player words."""
    c = INERT[t]
    fuze = f"a Fuze Assembly {c['fuze_name']}" if fuze_full else f"a {c['fuze_name']}"
    return (f"{c['plates']} {c['plate_name']} plates, {fuze}, {c['nose'][2]}, {c['hull'][2]}, "
            f"an {c['etier']} Sensor, an {c['etier']} Emitter and 2 {c['etier']} circuits")


def verify_inert(tiers) -> None:
    """Each listed casing's inert warhead recipe, entry by entry, its plate count and the items its nose and hull name."""
    want = {}
    for t in tiers:
        c = INERT[t]
        want[f"T{t}"] = [f"WarheadCasing.T{t}", str(c["volt"]), f'gt("plate","{c["plate"]}",WarheadRecipes.{c["plates_const"]})',
                         f"part(IcbmPart.FUZE_ASSEMBLY_{c['fuze']},1)", f"modItem(GalacticraftParts.{c['nose'][0]},1)",
                         f"modItem(GalacticraftParts.{c['hull'][0]},WarheadRecipes.HULL_PLATES)",
                         f'gtItem("Sensor_{c["etier"]}",1)', f'gtItem("Emitter_{c["etier"]}",1)', f'gt("circuit","{c["circuit"]}",2)']
    check_entries("InertWarheadSpec", _INERT, want)
    need = [(_RECIPES, "HULL_PLATES = 12;"), (_INERT, "RETURNED_INPUTS = 4;"), (_INERT, "CIRCUIT = 2;")]
    for t in tiers:
        c = INERT[t]
        need += [(_RECIPES, f"{c['plates_const']} = {c['plates']};"),
                 (_GC, f'{c["nose"][0]} = "{c["nose"][1]}";'), (_GC, f'{c["hull"][0]} = "{c["hull"][1]}";')]
    check_snippets("InertWarheadSpec/WarheadRecipes/GalacticraftParts", need)


# The payload recipe of each Tier 1 and 2 blast (WarheadRecipeSpec), as _norm writes it; PAYLOAD below says the same.
PAYLOAD_ENTRIES = {
    "CONDENSED": ["WarheadBlast.CONDENSED", "1"],
    "SHRAPNEL": ["WarheadBlast.SHRAPNEL", "3", "part(IcbmPart.FRAGMENTATION_SLEEVE_STEEL,1)"],
    "ANVIL": ["WarheadBlast.ANVIL", "4", 'gt("plate","Lead",12)'],
    "INCENDIARY": ["WarheadBlast.INCENDIARY", "5", "part(IcbmPart.NAPALM_CANISTER,2)"],
    "DEBILITATION": ["WarheadBlast.DEBILITATION", "6", "part(IcbmPart.DISPERSAL_CANISTER,2)", 'gas("NitrousOxide",2000)'],
    "CHEMICAL": ["WarheadBlast.CHEMICAL", "7", "part(IcbmPart.DISPERSAL_CANISTER,2)", 'optionalMod("harvestcraft:mustardseedsItem",4)',
                 'gas("Ammonia",2000)'],
    "SMOKE": ["WarheadBlast.SMOKE", "8", "part(IcbmPart.DISPERSAL_CANISTER,2)", 'gt("dust","Coal",8)', 'fluid("Creosote",2000)'],
    "REPULSIVE": ["WarheadBlast.REPULSIVE", "9", 'gtItem("Field_Generator_HV",1)'],
    "ATTRACTIVE": ["WarheadBlast.ATTRACTIVE", "10", 'gtItem("Field_Generator_HV",1)', 'gt("stick","SteelMagnetic",4)'],
    "FRAGMENTATION": ["WarheadBlast.FRAGMENTATION", "19", "part(IcbmPart.FRAGMENTATION_SLEEVE_TITANIUM,1)"],
    "THERMOBARIC": ["WarheadBlast.THERMOBARIC", "24", "part(IcbmPart.DISPERSAL_CANISTER,2)", "kerosene(4000)"],
    "SONIC": ["WarheadBlast.SONIC", "21", 'gtItem("Emitter_EV",2)'],
    "BREACHING": ["WarheadBlast.BREACHING", "22", "part(IcbmPart.SHAPED_CHARGE_LINER_CONE,2)", 'gt("stick","Titanium",6)'],
    "CONTAGIOUS": ["WarheadBlast.CONTAGIOUS", "23", "bioDish(4)", 'fluid("Biomass",2000)'],
}

# The parts the sheet names, with what it says they are made of (IcbmPart).
PART_ENTRIES = {
    "PLASTIC_EXPLOSIVE": ['"plastic_explosive"', "Tier.HV", "4", "11", 'gt("plate","Polytetrafluoroethylene",2)', "industrialTnt(2)"],
    "FUZE_ASSEMBLY_MK1": ['"fuze_assembly_mk1"', "Tier.HV", "1", "12", 'gt("circuit","Advanced",1)', 'gt("plate","Steel",1)',
                          'gt("wireFine","Copper",4)'],
    "FUZE_ASSEMBLY_MK2": ['"fuze_assembly_mk2"', "Tier.EV", "1", "13", 'gt("circuit","Data",1)', 'gt("plate","Titanium",1)',
                          'gt("wireFine","Electrum",4)'],
    "SHAPED_CHARGE_LINER_CONE": ['"shaped_charge_liner_cone"', "Tier.HV", "2", "14", 'gt("plate","Copper",2)'],
    "NAPALM_CANISTER": ['"napalm_canister"', "Tier.HV", "1", "18", "part(DISPERSAL_CANISTER,1)", 'fluid("Gasoline",1000)'],
}


def _verify_sources() -> None:
    """The recipe inputs and effect durations the sheet prints are not in the dump; each is checked against the Java source
    here (recipes as whole entries, cost estimates aside), so a change to a recipe or an effect fails the build and the sheet is fixed with it."""
    verify_inert((1, 2))
    check_entries("WarheadRecipeSpec", _SPEC, PAYLOAD_ENTRIES)
    check_entries("IcbmPart", _PARTS, PART_ENTRIES)
    check_snippets("WarheadRecipes/WarheadCharging/WarheadDearming/Effects", [
        (_RECIPES, "RecipeMaps.assemblerRecipes"), (_RECIPES, ".nbtSensitive()"), (_CHARGING, "MAX_PER_CRAFT = 8;"),
        (_DEARM, "MAX_PER_CRAFT = WarheadCharging.MAX_PER_CRAFT;"), (_DEARM, "DEFUSER_WEAR_PER_CRAFT = 1;"),
        (_EFFECTS, "new Burn(5, 0, 0)"), (_EFFECTS, "new Burn(0, 200, 2)"), (_EFFECTS, "e.setFire(10);"),
        (_EFFECTS, "Potion.moveSlowdown.id, 60, 1)"), (_EFFECTS, "Potion.weakness.id, 60, 1)"), (_EFFECTS, "Potion.confusion.id, 80, 0)"),
        (_EFFECTS, "Potion.poison.id, 100, 1)"), (_EFFECTS, "Potion.poison.id, 120, 3)"), (_EFFECTS, "Potion.hunger.id, 120, 1)"),
        (_EFFECTS, "Potion.confusion.id, 200, 0)"), (_EFFECTS, "Potion.moveSlowdown.id, 100, 2)"),
    ])


_verify_sources()
# The most charges one Defuse Kit craft takes out: the same as one grid load puts in (checked above).
DEARM_PER_CRAFT = int(re.search(r"MAX_PER_CRAFT = (\d+);", _CHARGING).group(1))


def curve(blast_id: str, fill: int) -> tuple[float, float]:
    """(radius, center damage) of a blast at a fill, from the engine's own definition."""
    r, d = BLAST[blast_id]["curve"][str(fill)]
    return r, d


def fuse_seconds(radius: float) -> int:
    """D51: 3 seconds to react plus 1 second per 3 blocks of the actual blast radius, rounded up."""
    return 3 + math.ceil(round(radius / 3, 6))


def full(blast_id: str) -> tuple[float, float]:
    return curve(blast_id, BLAST[blast_id]["cap"])


def _n(v: float) -> str:
    return f"{v:.0f}" if abs(v - round(v)) < 1e-6 else f"{v:.1f}"


# The payload each type fits to an inert warhead (WarheadRecipeSpec, D282). The explosive is always the Plastic
# Explosive charges. In player words.
PAYLOAD = {
    "condensed": "none: Plastic Explosive alone", "shrapnel": "a steel Fragmentation Sleeve",
    "anvil": "12 lead plates", "repulsive": "an HV Field Generator",
    "attractive": "an HV Field Generator, 4 magnetic steel rods", "incendiary": "2 Napalm Canisters",
    "debilitation": "2 Dispersal Canisters, 2,000 L nitrous oxide",
    "chemical": "2 Dispersal Canisters, 2,000 L ammonia gas, and a few mustard seeds if HarvestCraft is installed",
    "smoke": "2 Dispersal Canisters, 8 coal dust, 2,000 L creosote",
    "fragmentation": "a titanium Fragmentation Sleeve",
    "sonic": "2 EV Emitters",
    "breaching": "2 Shaped-Charge Liner Cones, 6 titanium rods",
    "thermobaric": "2 Dispersal Canisters, 4,000 L kerosene",
    "contagious": "4 Bio Lab culture dishes, 2,000 L biomass",
}

# (name, id, kind, what it does in player words), in the order the sheet lists them. Durations come from the effects
# checked in _verify_sources.
BLASTS = [
    ("Condensed", "condensed", "Crater", "The plain high explosive. A crater, and a lot of damage at the center."),
    ("Shrapnel", "shrapnel", "Crater", "A crater that also sets everything in the radius on fire for 5 seconds."),
    ("Anvil", "anvil", "Crater", "A crater that leaves everything caught in it with Slowness III for 10 seconds."),
    ("Incendiary", "incendiary", "Fire", f"No crater. Everything in the radius burns for 10 seconds, and fire spreads across up to {DUMP['effects']['fireMaxBlocks']} blocks with a solid top."),
    ("Debilitation", "debilitation", "Gas", f"A {GAS_SECONDS}-second cloud: Slowness II, Weakness II and nausea."),
    ("Chemical", "chemical", "Gas", f"A {GAS_SECONDS}-second cloud of Poison II."),
    ("Smoke", "smoke", "Gas", f"A {GAS_SECONDS}-second marker cloud. Harmless, so use it to signal or screen."),
    ("Repulsive", "repulsive", "Push", "Throws everything outward, hardest at the center."),
    ("Attractive", "attractive", "Push", "Drags everything inward toward the center."),
    ("Fragmentation", "fragmentation", "Crater", "A round crater that sets everything in the radius on fire for 5 seconds."),
    ("Thermobaric", "thermobaric", "Crater", "The widest conventional crater. Nothing extra, just a huge, round hole."),
    ("Contagious", "contagious", "Gas", f"A {GAS_SECONDS}-second cloud: Poison IV and Hunger II."),
    ("Sonic", "sonic", "Sound", "Nausea for 10 seconds and Slowness III for 5, and every glass block in the radius shatters."),
    ("Breaching", "breaching", "Tunnel", f"Bores a 3 by 3 tunnel {DUMP['effects']['breachDepth']} blocks straight down, through anything of hardness {DUMP['effects']['breachMaxHardness']:.0f} or less."),
]

KIND_COLOR = {"Crater": "#ff8a1f", "Fire": "#ff5555", "Gas": "#6fd36b", "Push": "#3d8bff", "Sound": "#ff6fcf", "Tunnel": "#ffd23f"}

CSS = """
.ribbon { position: absolute; top: 40px; right: 56px; transform: rotate(-3deg); text-align: center;
  background: #ffd23f; color: #1a1200; font-family: Bahnschrift, "Fira Sans Condensed", "Noto Sans", sans-serif; font-stretch: condensed;
  font-weight: 800; font-size: 38px; letter-spacing: .06em; text-transform: uppercase; padding: 12px 28px 10px; border-radius: 10px;
  border: 4px solid #1a1200; outline: 3px solid #ffd23f; box-shadow: 0 8px 22px rgba(0,0,0,.6); z-index: 5; }
.tier-card { display: grid; grid-template-columns: 250px 1fr; gap: 22px; align-items: center; }
.tier-card .spot { height: 360px; display: flex; align-items: flex-end; justify-content: center; padding-bottom: 14px; }
.stat { display: flex; justify-content: space-between; gap: 14px; padding: 5px 0; border-bottom: 1px solid var(--line); font-size: 19px; }
.stat:last-child { border-bottom: 0; }
.stat span:first-child { color: var(--muted); }
.stat b { font-weight: 600; text-align: right; }
.bt { background: rgba(255,255,255,.035); border: 1px solid var(--line); border-radius: 12px; padding: 12px 14px 12px; border-left: 6px solid var(--kc); }
.bt .top { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.bt h3 { font-size: 23px; }
.bt .kind { font-family: Bahnschrift, "Fira Sans Condensed", "Noto Sans", sans-serif; font-weight: 700; font-size: 14px; letter-spacing: .1em;
  text-transform: uppercase; color: var(--kc); }
.bt .nums { display: flex; gap: 16px; margin: 6px 0 4px; font-size: 17px; color: var(--txt); }
.bt .nums b { color: var(--kc); font-weight: 700; }
.bt .pay { color: var(--dim); font-size: 15px; margin-top: 6px; }
.bt .pay span { font-family: Bahnschrift, "Fira Sans Condensed", "Noto Sans", sans-serif; font-weight: 700; font-size: 13px; letter-spacing: .1em; text-transform: uppercase; color: var(--muted); margin-right: 6px; }
.bt .what { color: var(--muted); font-size: 16.5px; line-height: 1.3; }
.fillrow { display: grid; grid-template-columns: 74px 1fr 104px 96px 56px; gap: 12px; align-items: center; font-size: 17px; padding: 3px 0; }
.fillrow .bar { height: 14px; border-radius: 7px; background: #26303c; position: relative; overflow: hidden; }
.fillrow .bar i { position: absolute; inset: 0 auto 0 0; border-radius: 7px; background: var(--bc); }
.fillrow b { text-align: right; font-weight: 600; }
.fillrow .hd { color: var(--dim); font-size: 14px; letter-spacing: .1em; text-transform: uppercase; }
.step { display: flex; gap: 14px; align-items: flex-start; }
.step .t { font-size: 18px; color: var(--muted); line-height: 1.32; }
.step .t b { color: var(--txt); }
.mrow { display: grid; grid-template-columns: 130px 1fr; gap: 16px; align-items: center; }
.ladder { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.ladder .off { opacity: .5; }
"""


def _tnt_icon() -> K.Path:
    """TNT as the game draws it in an inventory: a small cube of the real vanilla textures (D55)."""
    top, side = vanilla.texture("blocks/tnt_top"), vanilla.texture("blocks/tnt_side")
    cube = K.voxels("tnt-cube", {(0, 0, 0): ("m", S.FRONT)}, {"m": (top, side, side)}, yaw=-38, pitch=28, ppu=60,
                    key="tnt-cube-v1" + hashlib.sha1(top.read_bytes() + side.read_bytes()).hexdigest()[:8])
    return cube


def _fill_table(label, blast_id, color) -> str:
    cap = BLAST[blast_id]["cap"]
    fills = {5: (1, 3, 5), 20: (1, 5, 10, 20)}[cap]
    r_full = full(blast_id)[0]
    rows = "".join(
        f'<div class="fillrow" style="--bc:{color}"><span>{f} of {cap}</span><div class="bar"><i style="width:{round(100 * curve(blast_id, f)[0] / r_full)}%"></i></div>'
        f'<b>{curve(blast_id, f)[0]:.1f} blocks</b><b>{curve(blast_id, f)[1]:,.0f} dmg</b><b>{fuse_seconds(curve(blast_id, f)[0])} s</b></div>'
        for f in fills)
    return (f'<div class="label" style="margin-bottom:6px">{esc(label)}</div>'
            f'<div class="fillrow"><span class="hd">Fill</span><span class="hd">Radius vs full</span><span class="hd" style="text-align:right">Radius</span>'
            f'<span class="hd" style="text-align:right">Center</span><span class="hd" style="text-align:right">Fuse</span></div>{rows}')


def _tier_card(tier, chip_tier, chip_text, plate, plate_name, model, cap, blurb, extra) -> str:
    stats = "".join(f'<div class="stat"><span>{esc(k)}</span><b>{v}</b></div>' for k, v in extra)
    return f"""<div class="card tier-card">
<div class="spot" style="display:flex;gap:2px;align-items:flex-end">{"".join(img(warhead(m), h=(300 if isinstance(model, str) else 190)) for m in ([model] if isinstance(model, str) else model))}</div>
<div><div style="display:flex;align-items:center;gap:12px;margin-bottom:6px"><h2>Tier {tier}</h2>{chip(chip_tier, chip_text)}</div>
<div class="muted" style="font-size:18px;margin-bottom:8px">{blurb}</div>
<div class="stat"><span>Casing</span><b>{gti("plate", plate, 28)} {esc(plate_name)} plates</b></div>
<div class="stat"><span>Fill cap</span><b>{cap} charges</b></div>
{stats}</div></div>"""


def _blast_card(name, blast_id, kind, what) -> str:
    b = BLAST[blast_id]
    rad, dmg = full(blast_id)
    if kind == "Tunnel":
        size = "<span>Tunnel <b>3 &times; 3</b></span>"  # the tunnel replaces the radius: it bores straight down
    else:
        size = f"<span>Radius <b>{_n(rad)}</b></span>"
    dmg_html = f'<span>Center <b>{dmg:,.0f}</b></span>' if dmg else '<span>No direct damage</span>'
    fuse = f"<span>Fuse <b>{fuse_seconds(rad)} s</b></span>"
    return f"""<div class="bt" style="--kc:{KIND_COLOR[kind]}"><div class="top"><h3>{esc(name)} T{BLAST[blast_id]["tier"][1:]} Warhead</h3><span class="kind">{esc(kind)}</span></div>
<div class="nums">{size}{dmg_html}{fuse}</div><div class="what">{esc(what)}</div><div class="pay"><span>Payload</span> {esc(PAYLOAD[blast_id])}</div></div>"""


def _hi_chip(t: str) -> str:
    """UV and UHV chips, past the base chip colors."""
    bg = {"UV": "#2ec4b6", "UHV": "#ff9a3d"}[t]
    return f'<span class="chip" style="background:{bg}">{t}</span>'


def warheads() -> str:
    def span(tier):  # the radius range of the types that blast a radius; breaching bores a tunnel instead
        radii = [full(b["id"])[0] for b in DUMP["blasts"] if b["tier"] == f"T{tier}" and b["id"] != "breaching"]
        return f"{_n(min(radii))} to {_n(max(radii))} blocks"

    def top_damage(tier):
        return max(full(b["id"])[1] for b in DUMP["blasts"] if b["tier"] == f"T{tier}")
    t1 = _tier_card(1, "HV", "HV", "StainlessSteel", "Stainless steel", "warhead_t1", CAP[1],
                    "The starter casing: small and bounded. Takes up one block.",
                    [("Full-fill center damage", f"{top_damage(1):,.0f}"), ("Blast radius, full", span(1))])
    t2 = _tier_card(2, "EV", "EV to IV", "Titanium", "Titanium", "warhead_t2", CAP[2],
                    f"A heavy casing that holds {CAP[2] // CAP[1]} times the charge. Still one block.",
                    [("Full-fill center damage", f"{top_damage(2):,.0f}"), ("Blast radius, full", span(2))])

    tier_of = {b[1]: BLAST[b[1]]["tier"] for b in BLASTS}
    t1_cards = "".join(_blast_card(*b) for b in BLASTS if tier_of[b[1]] == "T1")
    t2_cards = "".join(_blast_card(*b) for b in BLASTS if tier_of[b[1]] == "T2")

    fill = f"""<div class="card"><h2>Fill sets the size</h2>
<div class="muted" style="font-size:18px;margin:6px 0 12px">A warhead is filled up to its casing's cap. A part-filled casing is a smaller bang: the radius grows slower than the fill, so even a few charges make a useful blast.</div>
{_fill_table("Condensed T1 Warhead", "condensed", "var(--hv)")}
<div style="height:12px"></div>
{_fill_table("Thermobaric T2 Warhead", "thermobaric", "var(--ev)")}
<div class="muted" style="font-size:16px;margin-top:10px">Bars show the radius against a full casing. The fuse is for a hand-lit warhead (see the demolition steps).</div></div>"""

    falloff = """<div class="card"><h2>Damage fades to the edge</h2>
<div class="muted" style="font-size:18px;margin:6px 0 4px">Anything in the inner tenth of the blast takes the full hit. Past that the damage falls in a straight line and reaches zero at the edge. Armor still applies.</div>
<svg viewBox="0 0 560 190" style="width:100%;margin-top:6px">
<defs><linearGradient id="fg" x1="0" x2="0" y1="0" y2="1"><stop offset="0" stop-color="#ff8a1f" stop-opacity=".55"/><stop offset="1" stop-color="#ff8a1f" stop-opacity="0"/></linearGradient></defs>
<path d="M60 30 L106 30 L500 160 L60 160 Z" fill="url(#fg)"/>
<path d="M60 30 L106 30 L500 160" fill="none" stroke="#ffb35c" stroke-width="4" stroke-linejoin="round"/>
<line x1="60" y1="160" x2="520" y2="160" stroke="#97a3b1" stroke-width="2"/><line x1="60" y1="20" x2="60" y2="160" stroke="#97a3b1" stroke-width="2"/>
<circle cx="106" cy="30" r="7" fill="#ffd23f"/><circle cx="500" cy="160" r="7" fill="#ffd23f"/>
<text x="124" y="26" fill="#e9edf2" font-size="18" font-family="Segoe UI, Fira Sans, sans-serif">Full damage in the core</text>
<text x="200" y="148" fill="#e9edf2" font-size="18" font-family="Segoe UI, Fira Sans, sans-serif">Straight-line fall to zero</text>
<text x="60" y="184" fill="#b4bfcb" font-size="16" font-family="Segoe UI, Fira Sans, sans-serif">Ground zero</text>
<text x="520" y="184" fill="#b4bfcb" font-size="16" text-anchor="end" font-family="Segoe UI, Fira Sans, sans-serif">Blast radius</text></svg></div>"""

    flint = slot(vanilla.texture("items/flint_and_steel"), 30, "Flint and Steel")
    charge = slot(vanilla.texture("items/fireball"), 30, "Fire Charge")
    tnt = slot(_tnt_icon(), 30, "TNT")
    demo = f"""<div class="card"><h2>Use one as a demolition charge</h2>
<div class="muted" style="font-size:18px;margin:6px 0 14px">A warhead on its own is a charge. Set it down where you want the hole.</div>
<div style="display:flex;flex-direction:column;gap:14px">
<div class="step"><span class="num">1</span><div class="t"><b>Place it</b> on the ground or against a wall. It stands nose up in one block.</div></div>
<div class="step"><span class="num">2</span><div class="t"><b>Light it.</b> Right-click with {flint} Flint and Steel or a {charge} Fire Charge, or send it a <b>redstone pulse</b>. A bare-handed click does nothing, so nobody trips it by accident.</div></div>
<div class="step"><span class="num">3</span><div class="t"><b>Run.</b> The fuse is <b>3 seconds plus 1 second for every 3 blocks of blast radius</b>, rounded up: {fuse_seconds(full('condensed')[0])} seconds for a full Condensed warhead, {fuse_seconds(full('thermobaric')[0])} for a full Thermobaric. A smaller fill burns shorter. It beeps once a second. Break it while it burns and it defuses and drops.</div></div>
<div class="step"><span class="num">4</span><div class="t"><b>Careful.</b> Another explosion nearby sets it off with a {CHAIN_SECONDS:g}-second fuse, so keep charges apart.</div></div></div>
<div class="muted" style="font-size:16px;margin-top:12px">On a missile body the same warhead flies instead; the blast is the same.</div></div>"""

    kit_ico = slot(K.icon(K.ASSETS / "textures/items/thirdparty/ntm/defuser.png", 64), 72, "Defuse Kit")
    cut_ico = img(S.machine_block("cutter", "HV", greg=True), h=96)
    dearm = f"""<div class="card"><h2>Take one apart</h2>
<div class="muted" style="font-size:18px;margin:6px 0 12px">Built the wrong warhead, or want the parts back? Every step runs backwards.</div>
<div style="display:flex;flex-direction:column;gap:12px">
<div class="mrow"><div class="spot" style="display:flex;justify-content:center">{kit_ico}</div><div class="muted" style="font-size:17px">Put a live warhead and a <b>Defuse Kit</b> in a crafting grid: each craft takes out up to {DEARM_PER_CRAFT} <b>Plastic Explosive</b>, whole, for one use of the kit. The last charge out leaves the inert warhead with its payload, ready for a new load.</div></div>
<div class="mrow"><div class="spot" style="display:flex;justify-content:center">{cut_ico}</div><div class="muted" style="font-size:17px">The <b>Cutting Machine</b>, at the warhead's own voltage, empties a full warhead at once and takes a payload out: its parts come back, any fluid in it is spent. A bare inert warhead cuts back into its <b>plates, Fuze Assembly, nose cone and hull plates</b>; the sensor, emitter and circuits are spent.</div></div></div></div>"""

    asm_ico = img(S.machine_block("assembler", "HV", greg=True), h=104)
    pe_ico = slot(K.icon(K.ASSETS / "textures/items/thirdparty/ntm/plastic_explosive.png", 64), 72, "Plastic Explosive")
    ss, ti = gti("plate", "StainlessSteel", 28), gti("plate", "Titanium", 28)
    tp = K.ASSETS / "textures/items/thirdparty/ntm"
    parts_icons = "".join(slot(K.icon(f, 64), 36, t) for f, t in [
        (tp / "plastic_explosive.png", "Plastic Explosive"), (tp / "fuze_assembly_mk1.png", "Fuze Assembly Mk I"),
        (tp / "fuze_assembly_mk2.png", "Fuze Assembly Mk II"), (tp / "dispersal_canister.png", "Dispersal Canister"),
        (tp / "napalm_canister.png", "Napalm Canister"), (tp / "fragmentation_sleeve_steel.png", "Fragmentation Sleeve (steel)"),
        (tp / "fragmentation_sleeve_titanium.png", "Fragmentation Sleeve (titanium)"),
        (K.ASSETS / "textures/items/shaped_charge_liner_cone.png", "Shaped-Charge Liner Cone")])
    make = f"""<div class="card"><h2>How warheads are made</h2>
<div class="muted" style="font-size:18px;margin:6px 0 12px">Three steps: an inert casing, a payload, then Plastic Explosive charges. One charge is one unit of fill. The casing and the payload are Assembler recipes at the casing's voltage.</div>
<div style="display:flex;flex-direction:column;gap:12px">
<div class="mrow"><div class="spot" style="display:flex;justify-content:center">{asm_ico}</div><div><h3>1. Inert warhead &middot; 2. Payload {chip("HV")} {chip("EV")}</h3><div class="muted" style="font-size:17px">The warhead is the expensive part of a missile: it carries the nose cone and hull of its tier's Galacticraft rocket. The Assembler makes the <b>Inert T1 Warhead</b> from {ss} {esc(inert_words(1))}, and the <b>Inert T2 Warhead</b> from {ti} {esc(inert_words(2, fuze_full=False))}. An inert warhead cannot be placed, launched or set off. Then it fits a <b>payload</b>, the blast type's own parts (see each card), to the inert warhead. Condensed needs none.</div></div></div>
<div class="mrow"><div class="spot" style="display:flex;justify-content:center">{pe_ico}</div><div><h3>3. Charges</h3><div class="muted" style="font-size:17px">Put the inert warhead and 1 to 8 <b>Plastic Explosive</b> in a crafting grid. It comes out as a live warhead with that many charges, up to the cap ({CAP[1]} for Tier 1, {CAP[2]} for Tier 2), and topping up works the same way. Or the Assembler fills a whole casing at once. Charges alone in a Tier 1 casing make Condensed. A Tier 2 casing needs a payload first.</div></div></div>
<div class="mrow"><div class="spot" style="display:flex;justify-content:center;flex-wrap:wrap;gap:6px;max-width:240px">{parts_icons}</div><div><h3>The parts, each an Assembler recipe too</h3><div class="muted" style="font-size:17px"><b>Plastic Explosive</b>: PTFE plates and Industrial TNT, the charge of every warhead. <b>Fuze Assembly</b> Mk I (HV, Advanced circuit) and Mk II (EV, Data circuit). <b>Dispersal Canister</b> holds a fluid fill; the <b>Napalm Canister</b> is one filled with gasoline. <b>Fragmentation Sleeve</b> (steel or titanium) and <b>Shaped-Charge Liner Cone</b> (copper).</div></div></div></div></div>"""

    ladder = "".join(
        f'<span>{chip(t) if t in K.TIERS else _hi_chip(t)}</span>'
        for t in ["HV", "EV", "IV", "LuV", "ZPM", "UV", "UHV"])
    later = f"""<div class="card"><div style="display:flex;align-items:center;gap:22px;flex-wrap:wrap"><h2>Where each tier sits on the ladder</h2><div class="ladder">{ladder}</div></div>
<div class="muted" style="font-size:18px;margin-top:8px">Tiers 1 and 2 are the conventional starter casings, and they are on this sheet. Tier 3 (LuV to ZPM) and the nuclear warhead are the heavy two-block casings; antimatter and red matter sit at the very top, where everything in the blast zone dies. They have their own sheet, <b>Heavy Warheads</b>, along with the launcher warheads.</div></div>"""

    body = f"""<div class="grid" style="grid-template-columns:1fr 1fr">{t1}{t2}</div>
<div class="label" style="margin:26px 0 10px">Tier 1 blast types &middot; HV casing</div>
<div class="grid" style="grid-template-columns:repeat(3,minmax(0,1fr));gap:14px">{t1_cards}</div>
<div class="label" style="margin:22px 0 10px">Tier 2 blast types &middot; EV to IV casing</div>
<div class="grid" style="grid-template-columns:repeat(3,minmax(0,1fr));gap:14px">{t2_cards}</div>
<div class="muted" style="font-size:16px;margin-top:10px">Radius is in blocks at a full casing. Center damage is before armor.</div>
<div class="grid" style="grid-template-columns:1.2fr 1fr;margin-top:26px">{fill}{falloff}</div>
<div class="grid" style="grid-template-columns:1fr 1.15fr;margin-top:18px"><div style="display:flex;flex-direction:column;gap:18px">{demo}{dearm}</div>{make}</div>
<div style="margin-top:18px">{later}</div>"""
    return page_doc("Warheads", "Tiers 1 and 2: what each blast type does, how big the bang is, and how to make one.", body,
                    extra=CSS,
                    credit=["Warhead models and textures \u00a9 HBM's Nuclear Tech Mod contributors, GNU LGPL v3; modified.",
                            "Plate and machine icons: GregTech 5 Unofficial textures, LGPL-3.0.", MOJANG_CREDIT])


PAGES = {"icbm-warheads": warheads}
