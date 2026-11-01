"""The icbm-silos guide sheet: abandoned silo complexes as dungeons.

What a complex is, a map of its layout, the rooms and what each holds, the three activity levels, the guards, the loot
ceiling of every planet, the real machines, how to take a silo over, the radar station and the data stick.

Every number is read from the Java sources at build time (`_const`-style regexes on `ActivityProfile`, `IcbmConfig`,
`PlanetTable`, `SiloCeiling`, `SiloLootTables`, `RoomKind`, `RoomLoot`, `RoomLootDraw`, `FurnitureWear` and the room
blueprints in `complex/blueprint/rooms/`), and each formula the
sheet restates is checked against the source text, so a changed constant moves the sheet and a changed rule stops the
build instead of leaving it stale (D50). The map is drawn from the `ComplexExportTest` dump (complex_map.py):

    GREGGUNS_EXPORT_COMPLEX="$PWD/build/infographics/complex.json" ./gradlew test --rerun \\
        --tests com.gregguns.tools.ComplexExportTest

Blocks are the shipped art: the Silo Complex Wall with connected textures, GregTech hulls, Light Concrete and the NTM-derived
Radar Casing and dish. The sheet carries no in-game screenshots.

Run on its own:  python tools/asset-gen/infographics/pages_icbm_silos.py
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from PIL import Image, ImageDraw  # noqa: E402

import blocks as B  # noqa: E402
import complex_map as CM  # noqa: E402
import gt  # noqa: E402
import kit as K  # noqa: E402
import pages_icbm_defense as DEF  # noqa: E402
import pages_icbm_silo as SILO  # noqa: E402
import pages_more as M  # noqa: E402
import scenes as S  # noqa: E402
import thirdparty as TP  # noqa: E402
import vanilla  # noqa: E402
from kit import chip, esc  # noqa: E402
from pages import MOJANG_CREDIT, img, page_doc  # noqa: E402

JAVA = K.REPO / "src/main/java/com/gregguns"
FRONT, BACK, LEFT, RIGHT, UP = S.FRONT, S.BACK, S.LEFT, S.RIGHT, S.UP


# ---------------------------------------------------------------- numbers, read from the code


def _src(rel: str) -> str:
    return (JAVA / rel).read_text(encoding="utf-8")


def _must(text: str, needle: str, what: str):
    """A rule the sheet restates: the needle must still be in the source, or the sheet is out of date."""
    if needle not in text:
        raise SystemExit(f"{what} changed (expected `{needle}`); update pages_icbm_silos.py")


def _nums(text: str) -> list[float]:
    return [float(x) for x in re.findall(r"-?[0-9]+(?:\.[0-9]+)?", text)]


def _ints_of_config(name: str) -> list[int]:
    m = re.search(rf"{name}\s*=\s*\{{([^}}]*)\}}", _src("core/config/IcbmConfig.java"))
    if not m:
        raise SystemExit(f"IcbmConfig.{name} not found")
    return [int(x) for x in _nums(m.group(1))]


def _int_config(name: str) -> int:
    m = re.search(rf"{name}\s*=\s*(\d+);", _src("core/config/IcbmConfig.java"))
    if not m:
        raise SystemExit(f"IcbmConfig.{name} not found")
    return int(m.group(1))


def _layout_const(name: str) -> int:
    m = re.search(rf"\b{name}\s*=\s*(\d+)\s*[;,]", _src("icbm/complex/ComplexLayout.java"))
    if not m:
        raise SystemExit(f"ComplexLayout.{name} not found")
    return int(m.group(1))


ACT_WEIGHTS = _ints_of_config("siloComplexActivityWeights")
ROSS_WEIGHTS = _ints_of_config("siloComplexRoss128Weights")
CHANCE_PCT = _int_config("siloComplexChancePercent")
SIZE = _layout_const("SIZE")
HUB = (_layout_const("HUB_LONG"), _layout_const("HUB_SHORT"))
MAJOR_W, MINOR_W = _layout_const("MAJOR_WIDTH"), _layout_const("MINOR_WIDTH")
MIN_SILOS, MAX_SILOS = _layout_const("MIN_SILOS"), _layout_const("MAX_SILOS")
MIN_ROOMS, MAX_ROOMS = _layout_const("MIN_ROOMS"), _layout_const("MAX_ROOMS")
COVERAGE = _layout_const("MIN_COVERAGE_PERCENT")
HUB_H, MAJOR_H, GALLERY_H, ROOM_H, MINOR_H = (_layout_const(n) for n in (
    "HUB_HEIGHT", "MAJOR_HEIGHT", "GALLERY_HEIGHT", "ROOM_HEIGHT", "HEIGHT"))
DEPTH = int(re.search(r"DEPTH\s*=\s*(\d+);", _src("icbm/complex/gen/ComplexPlacer.java")).group(1))
_gr = _src("icbm/complex/ComplexGrower.java")
SILO_ODDS = [int(x) for x in re.search(r"SILO_WEIGHTS = \{([^}]*)\}", _gr).group(1).split(",")]
if sum(SILO_ODDS) != 100 or len(SILO_ODDS) != MAX_SILOS - MIN_SILOS + 1:
    raise SystemExit("ComplexGrower.SILO_WEIGHTS no longer gives a percentage for each of 4 to 10 silos")
SILO_PITCH = int(re.search(r"SILO_PITCH = (\d+);", _gr).group(1))
_must(_gr, "static final int CAP_PERCENT = 150", "the room growth cap")
_must(_src("icbm/complex/gen/ComplexPlacer.java"), "MIN_FLOOR", "the floor rule")
CHEST_TARGETS = _ints_of_config("siloComplexChestTargets")
if len(CHEST_TARGETS) != 3:
    raise SystemExit("IcbmConfig.siloComplexChestTargets no longer gives one total per activity level")

LEVELS = ["Abandoned", "Partly abandoned", "Operational"]


def _profiles() -> list[dict]:
    """ActivityProfile's three constants, in the constructor's own argument order."""
    s = _src("icbm/worldgen/ActivityProfile.java")
    out = []
    for m in re.finditer(r"private static final ActivityProfile (\w+) = new ActivityProfile\((.*?)\);", s, re.S):
        body = re.sub(r"new double\[\]\s*\{([^}]*)\}", lambda a: "ARR[" + a.group(1).replace(",", ";") + "]", m.group(2))
        args = [a.strip() for a in body.split(",")]
        if len(args) != 14:
            raise SystemExit(f"ActivityProfile's constructor changed ({len(args)} arguments); update pages_icbm_silos._profiles")
        spawn = [float(x) for x in re.search(r"ARR\[(.*)\]", args[9]).group(1).split(";")]
        if len(spawn) != 3:
            raise SystemExit("ActivityProfile.spawnerWeights no longer gives the odds of 0, 1 and 2 spawners a room")
        out.append(dict(
            name=m.group(1), collapse=float(args[1]), breach=float(args[2]), rubble=float(args[3]), hole=float(args[4]),
            lit=float(args[5]), broken=float(args[6]), dead=float(args[7]), flooded=args[8] == "true", spawn=spawn,
            chip=float(args[11]), part=float(args[12]), absent=float(args[13])))
    _must(s, "double[] spawnerWeights, int richness, double chip,", "ActivityProfile's argument order")
    _must(s, "double siloPart, double absentMachine)", "ActivityProfile's argument order")
    names = [p["name"] for p in out]
    if names != ["ABANDONED", "PARTLY", "OPERATIONAL"]:
        raise SystemExit(f"ActivityProfile constants changed: {names}")
    return out


PROFILES = _profiles()


def _ceiling_rules():
    """The loot ceiling table: voltage names, the planets of each rocket tier, and the formulas the sheet restates."""
    sc = _src("icbm/worldgen/SiloCeiling.java")
    names = re.findall(r'"([A-Za-z]+)"', re.search(r"NAMES = \{(.*?)\};", sc, re.S).group(1))
    _must(sc, "return ceiling <= 5 ? 3 : ceiling <= 7 ? 4 : 5;", "the guard danger rule")
    _must(sc, "Math.max(1, Math.min(7, ceiling))", "the gun table rule")
    cc = _src("icbm/worldgen/ComplexContents.java")
    _must(cc, "Math.max(1, Math.min(3, ceiling - 3))", "the salvaged body tier")
    _must(cc, "Math.max(Math.min(3, ceiling), ceiling - below)", "the radar and comms tier rule")
    _must(cc, "Long.remainderUnsigned(h, 3)", "the radar and comms tier spread")
    _must(cc, "default:\n                return roomMachineTier(ceiling, h);", "the room machine tier rule")
    _must(cc, "total += com.gregguns.icbm.complex.gen.MetalProps.weight(t, c);", "the room machine tier ladder")
    mp = _src("icbm/complex/gen/MetalProps.java")
    _must(mp, "if (tier >= ceiling - 1) return 1;\n        if (tier <= 2) return 4;\n        return 2;", "the metal ladder's weights")
    _must(cc, "boolean elite = p.level() != ActivityLevel.ABANDONED && band >= 2", "the elite spawner rule")
    _must(cc, "&& Mix.unit(Mix.of(seed, S_ELITE, m.x() * 4096L + m.z(), m.y())) < 0.4;", "the elite spawner share")
    _must(cc, "return 1 + Math.min(2, d / 30);", "the depth bands")
    _must(cc, "if (collapsed) {\n            return;", "no spawners in a caved-in room")
    _must(_src("worldgen/spawner/SpawnerSettings.java"), "STANDARD = new SpawnerSettings(3, 4, 100, 120, 5, 24, 40);", "the guard spawner settings")
    pt = _src("icbm/complex/PlanetTable.java")
    _must(pt, "return 4 + Math.max(0, t - 1);", "the planet ceiling formula")
    _must(pt, "public static final int OVERWORLD = 0;", "the Overworld tier")
    _must(_src("icbm/complex/gen/ComplexPlacer.java"), "SiloCeiling.roll(seedBase, rossWeights)", "the Ross128 roll")
    tiers = {}
    for m in re.finditer(r'put\(\s*(\w+)\s*,\s*((?:"[^"]*"\s*,?\s*)+)\)', pt):
        t = {"OVERWORLD": 0, "ROLLED": -1}.get(m.group(1)) if not m.group(1).isdigit() else int(m.group(1))
        tiers[t] = re.findall(r'"([^"]*)"', m.group(2))
    return names, tiers


VNAMES, PLANET_TIERS = _ceiling_rules()


# The plate/ingot ladder by voltage index (null below HV).
_lad_raw = re.search(r"LADDER = \{(.*?)\};", _src("icbm/worldgen/SiloLootTables.java"), re.S).group(1)
LADDER = [None if t.strip() == "null" else t.strip().strip('"') for t in _lad_raw.replace("\n", " ").split(",")]

METAL = {"StainlessSteel": "Stainless Steel", "Titanium": "Titanium", "TungstenSteel": "Tungstensteel", "HSSG": "HSS-G",
         "NaquadahAlloy": "Naquadah Alloy", "Neutronium": "Neutronium", "CosmicNeutronium": "Cosmic Neutronium",
         "Infinity": "Infinity", "TranscendentMetal": "Transcendent Metal", "SpaceTime": "Space-Time"}

PLANET_NAMES = {
    "Overworld": "Overworld", "moon": "Moon", "mars": "Mars", "phobos": "Phobos", "deimos": "Deimos", "ceres": "Ceres",
    "callisto": "Callisto", "europa": "Europa", "ganymed": "Ganymede", "iojupiter": "Io", "mercury": "Mercury",
    "venus": "Venus", "enceladus": "Enceladus", "titan": "Titan", "miranda": "Miranda", "oberon": "Oberon",
    "proteus": "Proteus", "triton": "Triton", "pluto": "Pluto", "haumea": "Haumea", "makemake": "Makemake",
    "barnarda2": "Barnarda C", "barnarda4": "Barnarda E", "barnarda5": "Barnarda F", "centauribb": "α Centauri Bb",
    "tcetie": "T Ceti E", "vega1": "Vega B", "anubis": "Anubis", "horus": "Horus", "maahes": "Maahes", "neper": "Neper",
    "seth": "Seth", "ross128b": "Ross128b", "ross128ba": "Ross128ba",
}
for _names in PLANET_TIERS.values():
    for _n in _names:
        if _n not in PLANET_NAMES:
            raise SystemExit(f"planet {_n} has no display name in pages_icbm_silos.PLANET_NAMES")


def vname(v: int) -> str:
    return VNAMES[v]


def ceiling_of(tier: int) -> int:
    return 4 + max(0, tier - 1)


def danger(c: int) -> int:
    return 3 if c <= 5 else 4 if c <= 7 else 5


def _ladder_weight(t: int, c: int) -> int:
    """MetalProps.weight: LV and MV weigh 4, the tiers between 2, the ceiling and the tier under it 1."""
    if t < 1 or t > c:
        return 0
    if t >= c - 1:
        return 1
    return 4 if t <= 2 else 2


def machine_share(c: int) -> tuple[float, float]:
    """(LV and MV share, the ceiling tier's share) of the room machines under a ceiling."""
    tot = sum(_ladder_weight(t, c) for t in range(1, c + 1))
    return (_ladder_weight(1, c) + _ladder_weight(2, c)) / tot, _ladder_weight(c, c) / tot


def pct(x: float) -> str:
    v = x * 100
    return f"{v:g}%"


# ---------------------------------------------------------------- rooms, from the layout code

ROOMS = re.findall(r"^\s+([A-Z_]+)\((\d+), (\d+), (true|false)(?:, \d+){0,4}\)[,;]", _src("icbm/complex/RoomKind.java"), re.M)
ROOM_INFO = {k: dict(length=int(a), depth=int(b), mandatory=c == "true") for k, a, b, c in ROOMS}
_must(_src("icbm/complex/RoomKind.java"), "UNDERGROUND_FARM(44, 34, false, 20, 16, 0, 0)", "the farm's minimum size")

_BP = JAVA / "icbm/complex/blueprint/rooms"
_BP_REG = _src("icbm/complex/blueprint/RoomBlueprints.java")
BLUEPRINT = dict(re.findall(r'put\("([A-Z_]+)", new (\w+)\(\)\);', _BP_REG))


def _room_contents() -> dict:
    """{room kind: {machines: set of MachineRole names, nasa, displays}} read from each kind's blueprint class.
    Counts follow a room's size, so the sheet names what a room holds and not how many."""
    out = {}
    for kind, cls in BLUEPRINT.items():
        body = (_BP / f"{cls}.java").read_text(encoding="utf-8")
        out[kind] = dict(machines=set(re.findall(r"MachineRole\.(\w+)", body)),
                         nasa="nasa" in body.lower(), displays="GUN_DISPLAY" in body or "displays(" in body)
    return out


ROOM_CONTENTS = _room_contents()
if sorted(ROOM_CONTENTS) != sorted(ROOM_INFO):
    raise SystemExit(f"rooms without a blueprint: {sorted(set(ROOM_INFO) ^ set(ROOM_CONTENTS))}; update pages_icbm_silos")
if (not ROOM_CONTENTS["ARMORY"]["displays"] or not ROOM_CONTENTS["MISSILE_ASSEMBLY"]["nasa"]
        or "CHEM_REACTOR" not in ROOM_CONTENTS["CHEM_LAB"]["machines"] or "LAUNCH_CONTROL" not in ROOM_CONTENTS["LAUNCH_CONTROL"]["machines"]):
    raise SystemExit("the room contents read from the blueprints look wrong; update pages_icbm_silos._room_contents")

# Each room kind's chest table (RoomLoot's rows); the commander's safe keeps its own table.
_rl = _src("icbm/worldgen/RoomLoot.java")
_KEY_VALUE = dict(re.findall(r'public static final String (\w+) = "([a-z_]+)";', _src("icbm/complex/LootKeys.java")))
ROOM_TABLE = {k: _KEY_VALUE[t] for k, t in re.findall(r'row\("([A-Z_]+)", LootKeys\.(\w+), \d+, Group\.\w+\);', _rl)}
if sorted(ROOM_TABLE) != sorted(ROOM_INFO):
    raise SystemExit(f"RoomLoot rows changed: {sorted(set(ROOM_INFO) ^ set(ROOM_TABLE))}; update pages_icbm_silos")
_must(_rl, "if (LootKeys.SAFE.equals(markKey) || LootKeys.ARMORY_AMMO.equals(markKey))", "the safe and armory display tables")

# What each table holds, in a player's words. The pools each line names are SiloLootTables.mix's for that table.
LOOT_TEXT = {
    "salvage": "metal ingots and plates, hardware, spare parts, tools, canned food, a damaged missile body",
    "control": "maps, clipboards, guidance computers, circuits and fine wire",
    "radar": "circuits, sensors, cables and guidance computers",
    "comms": "circuits, fine wire, cables, paper and maps",
    "server": "circuits, batteries and cables, fans",
    "office": "books, maps, clipboards, glasses, gold and silver, a beret",
    "safe": "the planet's top plates and ingots, gold, silver, coins, diamonds, a gun and its ammo",
    "magazine": "inert warheads, fuzes, liners and sleeves, explosives, four stainless steel plates",
    "assembly": "missile body parts (plating, fins, engines, nose cones, guidance), warhead parts, rarely a whole missile body",
    "fuel": "fuel canisters and tanks, coke, lubricant and creosote cells",
    "power": "batteries, cables, rotors, redstone, coke",
    "hvac": "fans, vents, gas mask filters, rotors, pipes",
    "pumps": "pumps, pipes, rubber, water and chlorine cells",
    "workshop": "screws, bolts, gears, springs, motors, pumps and pistons, tools, gun parts",
    "motorpool": "motors and pistons, buggy parts, batteries, fuel",
    "chemlab": "acid, solvent and gas cells, salts and dusts, hazmat gear",
    "incinerator": "ash, scrap boxes, metal nuggets, coal, bones",
    "stockpile": "metal ingots and plates, rubber, plastic, wool, logs, canned food",
    "pantry": "raw meat, canned and dried food, fresh produce, packed ice",
    "galley": "ingredients and meals (Pam's HarvestCraft and vanilla), produce, cookware",
    "canteen": "meals, snacks and drinks",
    "farm": "seeds, produce, bone meal, fertilizer, hoes and hay",
    "cellar": "mushrooms and mushroom dishes, mycelium",
    "bunks": "armor from leather to power armor, books, coins, a sidearm's ammo, snacks",
    "laundry": "wool, cotton, linens, armor",
    "lounge": "music discs, snacks and drinks, books, coins",
    "medical": "bandages, medkits, antidote, healing potions, golden apples",
    "decon": "hazmat suits, gas masks, filters, air tanks, medkits",
    "guard": "sidearms and their ammo, coffee, snacks, bandages",
    "brig": "confiscated knives, crowbars and swords, coins, gold, now and then a diamond",
    "range": "guns, ammo, grenades and gun parts, targets",
    "armory": "ammo, guns, grenades, gun parts and combat armor",
}
for _k in set(ROOM_TABLE.values()) | {"safe"}:
    if _k not in LOOT_TEXT:
        raise SystemExit(f"loot table {_k} has no text in pages_icbm_silos.LOOT_TEXT")
_slt = _src("icbm/worldgen/SiloLootTables.java")
for _case, _needle in [("GALLEY", ".pool(30, SiloPools.ingredients())"), ("BUNKS", ".pool(50, SiloPools.bunkarmor())"),
                       ("RANGE", ".pool(45, SiloArms.ammo(c))"), ("ASSEMBLY", ".pool(50, SiloPools.missilebody(c, true))"),
                       ("MAGAZINE", ".pool(45, SiloPools.warheadparts())"), ("CANTEEN", ".pool(35, SiloPools.meals())")]:
    _seg = _slt[_slt.index(f"case LootKeys.{_case}:"):]
    if _needle not in _seg[:_seg.index("break;")]:
        raise SystemExit(f"the {_case.lower()} table changed (expected `{_needle}`); update pages_icbm_silos.LOOT_TEXT")

MACHINE_NAME = {
    "GENERATOR": "GregTech generators", "BATTERY_BUFFER": "Battery Buffers", "LAUNCH_CONTROL": "Launch Control",
    "NASA_WORKBENCH": "NASA Workbench", "PUMP": "GregTech pumps", "AIR_HANDLER": "air handlers (machine hulls)",
    "FUEL_TANK": "fuel tanks (machine hulls)", "WORKBENCH": "Assemblers", "RADAR": "radar console (machine hull)",
    "COMMS": "comms consoles (machine hulls)", "MAGAZINE_CASING": "stainless steel frame boxes",
    "CHEM_REACTOR": "Chemical Reactors", "LAB": "Centrifuges, Electrolyzers, Mixers and other lab machines",
    "RECYCLER": "Recyclers, Electric and Arc Furnaces", "MACHINE_TOOL": "Lathes, Benders, Presses, Wiremills, Cutters",
}
_mp = _src("icbm/worldgen/MachinePicker.java")
for _n in ("Machine_{T}_ChemicalReactor", "Machine_{T}_Centrifuge", "Machine_{T}_Recycler", "Machine_{T}_ArcFurnace",
           "Machine_{T}_Lathe", "Machine_{T}_Cutter", "Machine_{T}_Assembler"):
    _must(_mp, f'"{_n}"', "the machine picker's roles")
for _k, _c in ROOM_CONTENTS.items():
    for _r in _c["machines"]:
        if _r not in MACHINE_NAME:
            raise SystemExit(f"MachineRole.{_r} has no name in pages_icbm_silos.MACHINE_NAME")

ROOM_NAME = dict(CM.NAMES)
ROOM_NAME["GUARD_POST"] = "Guard Post"
ROOM_NAME["ASSEMBLY_BAY"] = "Assembly Bay"
# How each room is laid out (its blueprint, as silo-layout.md's Furnishing table describes it), in a few words.
ROOM_LOOK = {
    "ASSEMBLY_BAY": "The hub: a container yard, shipping containers along the walls and in rows, crate pallets, clear lanes to every hall.",
    "LAUNCH_CONTROL": "A console row with chairs at the windows over the silo gallery; plot tables, console pods, server racks.",
    "MISSILE_ASSEMBLY": "Beside the silo gallery: casing racks, parts chests and body-part containers around the NASA Workbench.",
    "COMMS": "Four-seat operator pods with typewriters, a console wall, racks and files.",
    "COMMANDER_OFFICE": "The desk and chairs, the safe, a meeting table, a couch corner, a map table, bookcases and filing chests.",
    "WARHEAD_MAGAZINE": "Racks of inert casing frame boxes, steel containers; fallout rubble. No warhead is ever stored.",
    "GENERATOR_HALL": "Generator sets with turbine housings, fuel containers, spares pallets.",
    "ARMORY": "Display walls across the room, a gun in an item frame every fifth block over a chest of its ammo; rifle racks and armor stands.",
    "RADAR_TRACKING": "Plot tables and back-to-back console pods, a console wall.",
    "FUEL_DEPOT": "Tank islands ringed by drums, fuel containers, a drum wall.",
    "AIR_HANDLING": "Air-handler units inside rings of vent risers, filter crates.",
    "PUMP_ROOM": "Two pools sunk 3 deep either side of the aisle, the pumps along the aisle between them, drums. Floods unless the complex is operational.",
    "WATER_TREATMENT": "Strips of standing water with filter tanks and dosing drums.",
    "WORKSHOP": "A row of GT machine tools, crafting-table and anvil islands, tool racks, the NASA Workbench in a corner.",
    "VEHICLE_BAY": "A drive lane to the back wall, shipping containers, charging buffers, a fuel point.",
    "STOCKPILE": "Pallet racks of chests and crate stacks, containers, metal props.",
    "SERVER_ROOM": "Two-high rack pairs, backup batteries, the operator's desk.",
    "BUNKS": "Real beds in head-to-head pairs sharing a locker, a chest at the foot of each bed; rifle racks.",
    "MESS_GALLEY": "A kitchen line of furnaces, cauldrons and counters; a serving counter; tables with chairs.",
    "CAFETERIA": "A serving line and rows of tables with chairs.",
    "INFIRMARY": "Bed pairs behind curtains, treatment bays, medicine cabinets, the nurse's station.",
    "DECON_AIRLOCK": "A decon shower, a bench, suits on stands. In the corner between the hub and a hall.",
    "GUARD_POST": "The duty desk, lockers, a rifle rack, a card table and ammo chests. Beside a hall mouth.",
    "UNDERGROUND_FARM": "Lit wheat, carrot and potato fields between water channels, hay bales and seed chests.",
    "MUSHROOM_CELLAR": "Mycelium beds framed by mushroom stems, crate racks.",
    "COLD_STORAGE": "Freezer racks of packed ice with chests in them, reefer containers.",
    "SHOOTING_RANGE": "Firing lanes down to wool targets on a hay backstop, booths at the firing line, ammo chests.",
    "BRIG": "Barred cells with bunks and iron doors, guard desks, evidence lockers, an interrogation table.",
    "CHEM_LAB": "Working Chemical Reactors on the back wall, lab benches, safety showers.",
    "SHOWERS_LAUNDRY": "Shower stalls, lockers and a bench, a laundry corner with wash tubs.",
    "REC_LOUNGE": "Pool tables, card tables, a bar and a sofa corner facing a screen wall, bookcases.",
    "INCINERATOR": "A furnace bank with chimney risers, GT recyclers, ash heaps, coal bunkers, sorting stations.",
}
_ROOM_ORDER = ["ASSEMBLY_BAY", "LAUNCH_CONTROL", "MISSILE_ASSEMBLY", "COMMS", "COMMANDER_OFFICE", "WARHEAD_MAGAZINE",
               "GENERATOR_HALL", "ARMORY", "RADAR_TRACKING", "SERVER_ROOM", "FUEL_DEPOT", "AIR_HANDLING", "PUMP_ROOM",
               "WATER_TREATMENT", "WORKSHOP", "VEHICLE_BAY", "CHEM_LAB", "INCINERATOR", "STOCKPILE", "COLD_STORAGE",
               "MESS_GALLEY", "CAFETERIA", "UNDERGROUND_FARM", "MUSHROOM_CELLAR", "BUNKS", "INFIRMARY",
               "SHOWERS_LAUNDRY", "REC_LOUNGE", "SHOOTING_RANGE", "BRIG", "GUARD_POST", "DECON_AIRLOCK"]
if sorted(_ROOM_ORDER) != sorted(ROOM_INFO) or sorted(ROOM_LOOK) != sorted(ROOM_INFO) or any(k not in ROOM_NAME for k in ROOM_INFO):
    raise SystemExit(f"RoomKind changed: {sorted(set(ROOM_INFO) ^ set(_ROOM_ORDER))}; update pages_icbm_silos._ROOM_ORDER and ROOM_LOOK")


def room_card(kind: str) -> str:
    info = ROOM_INFO[kind]
    c = ROOM_CONTENTS[kind]
    role, rgb = CM.ROLE_OF[kind]
    sw = f"rgb({rgb[0]},{rgb[1]},{rgb[2]})"
    size = f"about {info['length']} &times; {info['depth']}" if kind != "ASSEMBLY_BAY" else f"{HUB[0]} &times; {HUB[1]}"
    tag = {"ASSEMBLY_BAY": "one", "GUARD_POST": "3 to 5", "DECON_AIRLOCK": "one"}.get(
        kind, "always" if info["mandatory"] else "dealt")
    lines = [esc(ROOM_LOOK[kind])]
    lines.append(f"<b>Chests</b>: {LOOT_TEXT[ROOM_TABLE[kind]]}")
    if kind == "COMMANDER_OFFICE":
        lines.append(f"<b>The safe</b>: {LOOT_TEXT['safe']}")
    machines = sorted(c["machines"] | ({"NASA_WORKBENCH"} if c["nasa"] else set()))
    if machines:
        lines.append("<b>" + ", ".join(MACHINE_NAME[r] for r in machines) + "</b>")
    body = "".join(f"<li>{ln}</li>" for ln in lines)
    return (f'<div class="room"><div class="rh"><i style="background:{sw}"></i><b>{esc(ROOM_NAME[kind])}</b>'
            f'<span>{size} &middot; {tag}</span></div><ul>{body}</ul></div>')


# ---------------------------------------------------------------- room states, chests and wear


def _draw_profiles() -> dict:
    """RoomLootDraw's numbers per room state: real picks, junk picks."""
    s = _src("icbm/worldgen/RoomLootDraw.java")
    out = {}
    for name, args in re.findall(r"private static final Profile (\w+) = new Profile\(([^)]*)\);", s):
        a = [x.strip() for x in args.split(",")]
        out[name] = dict(real=(int(a[0]), int(a[1])), junk=(int(a[2]), int(a[3])))
    if sorted(out) != ["GUTTED", "IN_SERVICE", "MOTHBALLED", "STAFFED"]:
        raise SystemExit(f"RoomLootDraw profiles changed: {sorted(out)}")
    _must(s, "Profile(int realMin, int realMax, int junkMin, int junkMax,", "RoomLootDraw.Profile's argument order")
    return out


DRAW = _draw_profiles()
_fw = _src("icbm/worldgen/FurnitureWear.java")
NORMAL_LOST = [float(x) for x in re.search(r"NORMAL_LOST = \{([^}]*)\}", _fw).group(1).split(",")]
_must(_src("icbm/worldgen/RoomState.java"), "STAFFED,", "the room states")
_cc = _src("icbm/worldgen/ComplexContents.java")
IN_SERVICE_DEAD = float(re.search(r"IN_SERVICE_DEAD = ([0-9.]+);", _cc).group(1))
MOTHBALLED_DEAD = float(re.search(r"MOTHBALLED_DEAD = ([0-9.]+);", _cc).group(1))
_must(_cc, "a caved-in room holds neither chests nor crates", "the caved-in room rule")
_must(_cc, "display frames sit outside the chest budget", "the armory displays outside the chest total")
_must(_cc, "c.addBonus(Bonus.CASING_PLATES);", "the magazine's plates")
_must(_cc, "c.addBonus(Bonus.DAMAGED_BODY);", "the hub's damaged bodies")


# ---------------------------------------------------------------- blocks and renders



def hero_room() -> Path:
    """A lit generator hall cutaway: one Silo Complex Wall (connected) all round, a checkered floor with light tiles under the
    ceiling lamps, a framed doorway, EV machine hulls, a guard post (a vanilla monster spawner), and a breach in the wall and a
    hole in the floor."""
    b = S.Build("silos-hall")
    cw = S._wall("complex_wall")
    side, top = gt.hull_tinted("EV"), gt.hull_tinted("EV", "TOP")
    dark, light = gt.iconset("CONCRETE_DARK_BRICKS"), gt.iconset("CONCRETE_LIGHT_BRICKS")
    b.define("w", "Silo Complex Wall", cw)
    b.define("fd", "Dark Concrete Bricks", dark)
    b.define("fl", "Light Concrete Bricks", light)
    b.define("hull", "EV Machine Hull", (top, side, side))
    b.define("lamp", "Lamp", S.lamp_tex())
    b.define("post", "Monster Spawner (guard post)", vanilla.texture("blocks/mob_spawner"))
    X, Z = 13, 10
    cells = {}
    for x in range(X):
        for z in range(Z):
            cells[(x, 0, z)] = "fd" if (x + z) % 2 else "fl"
    for y in range(1, 6):
        for z in range(Z):
            cells[(X - 1, y, z)] = "w"
        for x in range(X):
            cells[(x, y, Z - 1)] = "w"
    for y in (2, 3, 4):      # the doorway into the next section
        for x in (5, 6, 7):
            cells.pop((x, y, Z - 1), None)
    for p in [(X - 1, 3, 2), (X - 1, 4, 2), (X - 1, 3, 3), (X - 1, 2, 3)]:     # a breach in the east wall
        cells.pop(p, None)
    for p in [(3, 0, 1), (3, 0, 2), (4, 0, 2)]:                                # a hole in the floor
        cells.pop(p, None)
    for x in (2, 5, 8):
        cells[(x, 1, 4)] = "hull"
    for y in (6,):           # a strip of ceiling along the two walls holds the lamp grid, flush
        for x in range(X):
            for z in range(Z - 3, Z):
                cells[(x, y, z)] = "w"
        for z in range(Z - 3):
            for x in range(X - 3, X):
                cells[(x, y, z)] = "w"
    for x in (2, 6, 10):
        cells[(x, 6, Z - 2)] = "lamp"
        cells[(x, 0, Z - 2)] = "lamp"      # the floor light tile under it
    for z in (2, 5):
        cells[(X - 2, 6, z)] = "lamp"
        cells[(X - 2, 0, z)] = "lamp"
    cells[(X - 2, 1, Z - 2)] = "post"
    b.blocks.update(cells)
    return b.render(yaw=-34, pitch=30, ppu=48, tag="")


def shaft(variant: str) -> Path:
    """The surface mouth of a silo shaft in a patch of ground: a 5 x 5 concrete ring around a 3 x 3 bore, open, half open or capped."""
    tex = TP.terrain_textures()
    tex["ring"] = gt.iconset("CONCRETE_LIGHT_STONE")
    cells = {}
    n = 9
    c0 = 2
    bore = {(x, z) for x in range(3, 6) for z in range(3, 6)}
    ring = {(x, z) for x in range(c0, c0 + 5) for z in range(c0, c0 + 5)}
    for x in range(n):
        for z in range(n):
            for y in range(-5, 1):
                if (x, z) in bore and y < 0:
                    continue
                if (x, z) in ring and (x, z) not in bore:
                    cells[(x, y, z)] = "ring"
                elif (x, z) in bore:
                    continue
                else:
                    cells[(x, y, z)] = "grass" if y == 0 else "dirt" if y >= -3 else "stone"
    caps = set()
    if variant == "half":
        caps = {(x, z) for (x, z) in bore if x <= 4}
    elif variant == "closed":
        outer = sorted(bore - {(4, 4)})
        caps = bore - {outer[2], outer[5]}
    for x, z in caps:
        cells[(x, 0, z)] = "ring"
    return K.voxels(f"silos-shaft-{variant}", cells, tex, yaw=-34, pitch=34, ppu=40)


def radar_station(formed: bool) -> Path:
    """The surface radar station: a 5 x 5 hut of Works Casing three high with a Gantry Casing roof and a doorway, the small radar on
    the roof, its Energy Hatch place a Radar Casing until a player adds one."""
    b = S.Build(f"silos-radar-{int(formed)}")
    works, roof = S._wall("works_casing"), S._wall("gantry_casing")
    casing = DEF.NTM_CASINGS / "radar_casing.png"
    ctrl = B.composite(casing, DEF.NTM_MACHINES / "radar_hv" / "OVERLAY_FRONT.png", cache=K.TEXCACHE, name="silos-radar-ctrl")
    b.define("works", "Works Casing", works)
    b.define("roof", "Gantry Casing", roof)
    b.define("rc", "Radar Casing", S._wall("radar_casing"))
    b.define("ctrl", "Radar controller", (casing, casing, ctrl))
    cells = {}
    for x in range(5):
        for z in range(5):
            cells[(x, 4, z)] = "roof"
            if x in (0, 4) or z in (0, 4):
                for y in (1, 2, 3):
                    cells[(x, y, z)] = "works"
    for y in (1, 2):
        cells.pop((2, y, 4), None)          # the doorway, south side
    radar, _ = DEF.radar_cells()
    for (x, y, z), ch in radar.items():
        p = (x + 1, y + 5, z + 1)
        if ch == "~":
            cells[p] = ("ctrl", FRONT)
        elif ch in "CH":
            cells[p] = "rc"
    b.blocks.update(cells)
    b.hatch((3, 5, 1), "maintenance_hatch", FRONT, casing)
    if formed:
        b.hatch((2, 5, 1), "energy_hatch", FRONT, casing)
        return DEF.render_with_dish(b, "silos-radar-formed", DEF.dish_spot(b, "RadarShape"), 1.0, -60, yaw=-34, pitch=24, ppu=46)
    return b.render(yaw=-34, pitch=24, ppu=46, tag="", marks=True)


def silo_front(missing: str | None) -> Path:
    """The front wall of a complex silo, with the controller, hatches and Launch Control where the complex places them
    (D299: hatch columns beside a 3 x 3 glass window). missing: None, "controller" or "hatch"."""
    b = SILO.silo_build()
    b.blocks[(2, 2, 0)] = "casing" if missing == "controller" else ("ctrl", FRONT)
    if missing == "hatch":
        # The Energy Hatch's cell keeps the hatch column's concrete.
        pos = next(p for p, k, _ in SILO.HATCHES if k == "energy_hatch")
        b.blocks[pos] = "casing"
        b.hatches = [h for h in b.hatches if h[1] != pos]
    return SILO.render(b, yaw=-22, pitch=22, ppu=84, ground=False, missile=False, marks=True,
                       cut=lambda x, y, z: y > 3 or z > 1)


def hull_block(tier: str) -> Path:
    side, top = gt.hull_tinted(tier), gt.hull_tinted(tier, "TOP")
    return K.voxels(f"silos-hull-{tier}", {(0, 0, 0): "h"}, {"h": (top, side, side)}, yaw=-34, pitch=28, ppu=96)


def spawner_block() -> Path:
    """The vanilla monster spawner every guard post is (its Mojang texture, read from the Minecraft jar)."""
    t = vanilla.texture("blocks/mob_spawner")
    return K.voxels("silos-post-mob_spawner", {(0, 0, 0): "h"}, {"h": (t, t, t)}, yaw=-34, pitch=28, ppu=96)


def wall_block() -> Path:
    cw = S._wall("complex_wall")
    return K.voxels("silos-wall", {(0, 0, 0): "w"}, {"w": cw}, yaw=-34, pitch=28, ppu=96)


def palette_swatches() -> str:
    """A row of the blocks that dress a room with no finish mod installed, each as one rendered block."""
    cw = S._wall("complex_wall")
    quartz = (vanilla.texture("blocks/quartz_block_top"), vanilla.texture("blocks/quartz_block_side"), vanilla.texture("blocks/quartz_block_side"))
    items = [
        ("Silo Complex Wall", cw),
        ("Light Concrete Bricks", gt.iconset("CONCRETE_LIGHT_BRICKS")),
        ("Dark Concrete Bricks", gt.iconset("CONCRETE_DARK_BRICKS")),
        ("Smooth Concrete", gt.iconset("CONCRETE_LIGHT_SMOOTH")),
        ("Quartz", quartz),
        ("Lamp", vanilla.texture("blocks/glowstone")),
    ]
    cells = []
    for i, (name, tex) in enumerate(items):
        t = (tex, tex, tex) if isinstance(tex, Path) else tex
        pic = K.voxels(f"silos-sw-{i}", {(0, 0, 0): "h"}, {"h": t}, yaw=-34, pitch=28, ppu=96)
        cells.append(f'<div style="text-align:center">{img(pic, h=58)}<div class="cap" style="margin-top:0;font-size:13px">{name}</div></div>')
    return '<div style="display:flex;justify-content:space-between;gap:6px">' + "".join(cells) + "</div>"


def _crop(d: dict, p: dict, px: int) -> Path:
    """The plan's first silo gallery, silos outlined in orange, turned so the hall runs up and down."""
    g = next(h for h in p["halls"] if h["gallery"])
    x0, z0, x1, z1 = g["box"]
    box = (x0 - 4, z0 - 4, x1 + 4, z1 + 4)

    def make():
        im = Image.open(CM.raster(d, p, px)).convert("RGB").crop(tuple(v * px for v in box))
        dr = ImageDraw.Draw(im)
        for s in p["silos"]:
            sx0, sz0, sx1, sz1 = s["box"]
            if sx1 < box[0] or sx0 > box[2] or sz1 < box[1] or sz0 > box[3]:
                continue
            dr.rectangle([(sx0 - box[0]) * px - 2, (sz0 - box[1]) * px - 2, (sx1 - box[0]) * px + 1, (sz1 - box[1]) * px + 1],
                         outline=(255, 138, 31), width=3)
        if im.width > im.height:
            im = im.rotate(90, expand=True)
        return im

    return K._cached("silos-crop", dict(seed=p["seed"], px=px, v=3), make)


# ---------------------------------------------------------------- blast doors and service runs, from the fixtures code

_fx = _src("icbm/worldgen/ComplexFixtures.java")
_fw2 = _src("icbm/worldgen/FixtureWriter.java")
GUTTED_OPEN = float(re.search(r"GUTTED_OPEN = ([0-9.]+);", _fx).group(1))
BREACH_MIN, BREACH_MAX = (int(x) for x in re.search(r"BREACH_MIN = (\d+), BREACH_MAX = (\d+);", _fx).groups())
_must(_fx, "int torn = (int) (Mix.unit(Mix.of(seed, S_DOOR, id, 1)) * 3); // 0 to 2", "the stuck-open door's torn slats")
_must(_fx, "hang[i] = Math.min(height - 1, 2 + (int) (Mix.unit(Mix.of(seed, S_DOOR, id, 4 + side)) * 4));", "the jammed columns")
_must(_fx, "int gap = 1 + (int) (Mix.unit(Mix.of(seed, S_RUN, t.id() * 4096L + k, 2)) * 3);", "the tray gaps")
_must(_fx, "i += gap + 6 + (int) (Mix.unit(Mix.of(seed, S_RUN, t.id() * 4096L + k, 4)) * 9);", "the tray gap spacing")
CABLE_STRIP = float(re.search(r"strip\(cable, seed, t\.id\(\), 5, ([0-9.]+),", _fx).group(1))
PIPE_STRIP = float(re.search(r"strip\(pipe, seed, t\.id\(\), 6, ([0-9.]+),", _fx).group(1))
_must(_fx, "boolean[] pipe = t.major() && hi > lo ?", "the pipe in major halls only")
_must(_fx, "if (!t.gallery()) runs(", "no service runs in the silo gallery")
_must(_gr, "return h <= ComplexLayout.MINOR_HEIGHT ? h - 2 : h - 3;", "the portal height")
for _st in ("case IN_SERVICE:", "case STAFFED:", "case MOTHBALLED:"):
    _must(_fx, _st, "the door states")
SERVICE = [tuple(m) for m in re.findall(r'\{ "(\w+)", "(\w+)" \}', re.search(r"SERVICE_MATERIALS = \{(.*?)\};", _fw2, re.S).group(1))]
_must(_fw2, "OrePrefixes.cableGt04 : OrePrefixes.pipeLarge", "the 4x cable and the large pipe")
_must(_fw2, "OrePrefixes.frameGt, Materials.Steel", "the steel tray")
_must(_fw2, "ItemList[] casings = ItemList.MACHINE_CASINGS;", "the Ross128 tier casing frames")
GT_NAME = {"Aluminium": "Aluminium", "Platinum": "Platinum", "VanadiumGallium": "Vanadium-Gallium", "Naquadah": "Naquadah",
           "NaquadahAlloy": "Naquadah Alloy", "Steel": "Steel", "StainlessSteel": "Stainless Steel", "Titanium": "Titanium",
           "TungstenSteel": "Tungstensteel", "Iridium": "Iridium"}


def door_svg(kind: str, casing: Path | None = None, w_cols: int = 18, h_rows: int = 8, cell: int = 13, slat: str = "#a9b4c1") -> str:
    """A blast-door portal face on, as a diagram: jambs and lintel, the door's slat columns by state. `casing` fills the
    frame with a GregTech machine casing texture (the Ross128 tier frame)."""
    W, H = (w_cols + 2) * cell, (h_rows + 1) * cell + cell
    out = [f'<svg viewBox="0 0 {W} {H}" width="{W}" height="{H}" style="display:block;margin:0 auto">']
    fill = "#4a5562"
    if casing is not None:
        pid = "cas" + casing.stem.replace("-", "")
        out.append(f'<defs><pattern id="{pid}" width="{cell}" height="{cell}" patternUnits="userSpaceOnUse">'
                   f'<image href="{K.url(casing)}" width="{cell}" height="{cell}" style="image-rendering:pixelated"/></pattern></defs>')
        fill = f"url(#{pid})"
    frame = [(0, r) for r in range(h_rows + 1)] + [(w_cols + 1, r) for r in range(h_rows + 1)] + [(c, 0) for c in range(1, w_cols + 1)]
    for c, r in frame:
        out.append(f'<rect x="{c * cell}" y="{r * cell}" width="{cell}" height="{cell}" fill="{fill}" stroke="#1b2129" stroke-width=".6"/>')
    floor_y = (h_rows + 1) * cell
    out.append(f'<rect x="0" y="{floor_y}" width="{W}" height="{cell}" fill="#3a424c"/>')
    rows = {c: h_rows for c in range(w_cols)}       # pieces hanging from the top, per column
    piles = set()
    if kind == "open":
        rows = {c: 1 for c in range(w_cols)}
    elif kind == "stuck":
        rows = {c: 1 for c in range(w_cols)}
        rows[4] = rows[13] = 0
    elif kind == "breached":
        for c in range(6, 12):
            rows[c] = 0
        rows[5], rows[12] = 3, 4
        piles = {4, 5, 6, 11, 12, 13}
    for c in range(w_cols):
        for r in range(rows[c]):
            x, y = (c + 1) * cell, (r + 1) * cell
            out.append(f'<rect x="{x + .5}" y="{y + .5}" width="{cell - 1}" height="{cell - 1}" fill="{slat}" stroke="#3e4651" stroke-width="1"/>'
                       f'<line x1="{x + 1}" y1="{y + cell / 2}" x2="{x + cell - 1}" y2="{y + cell / 2}" stroke="#7b8693" stroke-width="1"/>')
    for c in piles:
        x = (c + 1) * cell
        out.append(f'<polygon points="{x},{floor_y} {x + cell / 2},{floor_y - cell * .8} {x + cell},{floor_y}" fill="#8a7a66" stroke="#4d4337"/>')
    out.append("</svg>")
    return "".join(out)


def gt_block(tex: Path, tag: str) -> Path:
    return K.voxels(f"silos-gt-{tag}", {(0, 0, 0): "h"}, {"h": (tex, tex, tex)}, yaw=-34, pitch=28, ppu=96)


PLUS = "<b style='color:var(--txt)'> +</b>"


def fixtures_section() -> str:
    states = [("shut", None, "Shut", "Staffed and mothballed wings"), ("open", None, "Rolled up", "Wings in service"),
              ("stuck", None, "Stuck open", f"{GUTTED_OPEN * 100:.0f}% of a ruin's doors"),
              ("breached", None, "Breached", f"{(1 - GUTTED_OPEN) * 100:.0f}% of a ruin's doors")]
    panels = "".join(f'<div style="text-align:center">{door_svg(k, c)}<div class="cap" style="margin-top:4px"><b style="color:var(--txt)">{t}</b> &middot; {sub}</div></div>'
                     for k, c, t, sub in states)
    ross_tiers = [vname(4 + i) for i in range(len(ROSS_WEIGHTS))]
    ross = "".join(f'<div style="text-align:center">{img(gt_block(gt.hull_tinted(t), "casing-" + t), h=48)}<div style="margin-top:2px">{chip(t)}</div></div>'
                   for t in ross_tiers)
    ross_door = door_svg("shut", gt.hull_tinted("LuV"), slat="#6f7a88")
    cable_rows = "".join(f'<tr><td style="white-space:nowrap">{chip(vname(4 + i)) if 4 + i < len(VNAMES) else ""}{PLUS if i == len(SERVICE) - 1 else ""}</td>'
                         f'<td style="color:var(--txt)">{GT_NAME[c]} 4x Cable</td><td style="color:var(--txt)">Large {GT_NAME[p]} Fluid Pipe</td></tr>'
                         for i, (c, p) in enumerate(SERVICE))
    blocks = "".join(f'<div style="text-align:center"><div class="spot" style="padding:8px 14px 4px">{img(gt_block(t, n), h=64)}</div><div class="cap" style="margin-top:4px">{lab}</div></div>' for t, n, lab in [
        (gt.frame("Steel"), "frame", "Steel Frame Box tray"), (gt.cable(SERVICE[0][0]), "cable", "4x Cable under it"),
        (gt.pipe("pipeLarge", SERVICE[0][1]), "pipe", "Large Fluid Pipe")])
    return f"""<div class="sec"><h2>Blast doors and service runs</h2>
<div class="card" style="display:grid;grid-template-columns:1.25fr 1fr;gap:26px">
<div><div class="pt" style="margin-bottom:10px">Every major hall leaves the hub through a <b>blast door</b> the full width of the hall, {MAJOR_W - 2} columns wide and {MAJOR_H - 3} high
({min(HUB_H, GALLERY_H) - 3} for the silo gallery). It is a Carpenter's Blocks garage door covered in the hall's own plating: right-click it to roll the whole door up or down. Its slats are thin and come away under a pickaxe.</div>
<div style="display:grid;grid-template-columns:1fr 1fr;gap:12px 18px">{panels}</div>
<div class="pt" style="margin-top:10px">A ruined door is either <b>stuck open</b> with up to two slats torn out, or <b>breached</b>: shut, with {BREACH_MIN} to {BREACH_MAX} columns torn out and the column at each side of the hole jammed 2 to 5 slats down over a rubble pile, so there is always a way through.
Without Carpenter's Blocks the hall mouths simply stand open.</div>
<div style="display:grid;grid-template-columns:auto 1fr;gap:16px;align-items:center;margin-top:14px">{ross_door}<div class="pt"><b>Ross128b and Ross128ba.</b> The jambs and lintel of every blast door, on both faces, are GregTech machine casings of the complex's rolled class, so the frame tells you what you drew. Here, a LuV complex.</div></div></div>
<div><div class="pt"><b>Service runs.</b> Every hall but the silo gallery carries a cable tray under the ceiling along one wall: Steel Frame Boxes with a GregTech <b>4x cable</b> hung under them. Major halls also run a <b>large fluid pipe</b> along the other wall. The metal follows the complex's ceiling:</div>
<table class="t" style="margin-top:8px"><tr><th>Ceiling</th><th>Cable</th><th>Pipe</th></tr>{cable_rows}</table>
<div style="display:flex;justify-content:space-around;margin-top:10px">{blocks}</div>
<div class="pt" style="margin-top:8px">They run whole in a staffed or mothballed wing. In a ruin the tray has gaps of 1 to 3 blocks, with fallen sections on the floor, about {CABLE_STRIP * 100:.0f}% of the cable is stripped and {PIPE_STRIP * 100:.0f}% of the pipe is missing. What is left mines like any GregTech cable or pipe.</div>
<div class="pt" style="margin-top:12px">The Ross128 frame casings, one per class:</div>
<div style="display:flex;justify-content:space-between;gap:4px;margin-top:6px">{ross}</div></div></div></div>
"""


# ---------------------------------------------------------------- page pieces

CSS = """
.sec { margin-top: 26px; }
.sec > h2 { margin: 0 0 12px; }
.lead { font-size: 20px; color: var(--muted); line-height: 1.4; }
.lead b, .pt b { color: var(--txt); }
.pt { font-size: 18px; color: var(--muted); line-height: 1.36; }
.step { display: flex; gap: 14px; align-items: flex-start; }
.step .t { font-size: 18px; color: var(--muted); line-height: 1.32; } .step .t b { color: var(--txt); }
.stat { background: rgba(255,255,255,.04); border: 1px solid var(--line); border-radius: 12px; padding: 12px 14px; }
.stat .v { font-family: Bahnschrift, "Fira Sans Condensed", sans-serif; font-stretch: condensed; font-size: 36px; font-weight: 700; color: var(--orange); line-height: 1; }
.stat .k { font-size: 16px; color: var(--muted); margin-top: 4px; line-height: 1.25; }
.t-UV { background: #2ee6c8; } .t-UHV { background: #b6f060; } .t-UEV { background: #ff9a5c; } .t-UIV { background: #9ad0ff; } .t-UMV { background: #f2f4f7; }
.cap { font-size: 16px; color: var(--muted); margin-top: 8px; line-height: 1.3; }
.lk { display: flex; align-items: center; gap: 10px; font-size: 17px; color: var(--txt); margin: 4px 0; }
.lk i { width: 20px; height: 20px; border-radius: 4px; flex: none; border: 1px solid rgba(255,255,255,.35); }
.lk i.d { transform: rotate(45deg) scale(.78); } .lk i.c { border-radius: 50%; }
.mapbox { position: relative; width: 960px; height: 960px; border-radius: 12px; overflow: hidden; border: 1px solid var(--line); flex: none; }
.mapbox img { display: block; position: absolute; left: 0; top: 0; image-rendering: pixelated; }
.rooms { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; }
.room { background: rgba(255,255,255,.035); border: 1px solid var(--line); border-radius: 12px; padding: 9px 12px 8px; }
.room .rh { display: flex; align-items: center; gap: 9px; flex-wrap: wrap; }
.room .rh i { width: 18px; height: 18px; border-radius: 4px; flex: none; border: 1px solid rgba(255,255,255,.4); }
.room .rh b { font-size: 18px; color: var(--txt); font-family: Bahnschrift, "Fira Sans Condensed", sans-serif; font-stretch: condensed; letter-spacing: .01em; }
.room .rh span { margin-left: auto; font-size: 14px; color: var(--dim); }
.room ul { margin: 4px 0 0 16px; padding: 0; font-size: 14.5px; color: var(--muted); line-height: 1.26; }
.room li { margin: 2px 0; } .room li b { color: #dfe5ec; font-weight: 600; }
table.t { width: 100%; border-collapse: collapse; font-size: 17px; }
table.t th { text-align: left; font-family: Bahnschrift, "Fira Sans Condensed", sans-serif; letter-spacing: .1em; text-transform: uppercase;
  font-size: 14px; color: var(--muted); padding: 7px 10px; border-bottom: 1px solid var(--line); }
table.t td { padding: 5px 10px; border-bottom: 1px solid var(--line); color: var(--muted); vertical-align: middle; line-height: 1.28; }
table.t td:first-child { color: var(--txt); font-weight: 600; }
table.t td:first-child .dim { display: block; }
table.t tr:last-child td { border-bottom: 0; }
table.t td b { color: var(--txt); }
table.t td.n { text-align: center; color: var(--txt); font-weight: 600; white-space: nowrap; }
.meter { display: flex; align-items: center; gap: 9px; justify-content: center; }
.meter .tr { width: 78px; height: 12px; background: rgba(255,255,255,.10); border-radius: 4px; overflow: hidden; flex: none; }
.meter .fl { height: 100%; }
.lvl { border-radius: 14px; border: 1px solid var(--line); padding: 14px 16px; background: linear-gradient(180deg, rgba(255,255,255,.05), rgba(255,255,255,.015)); }
.lvl .hd { display: flex; align-items: baseline; gap: 12px; }
.lvl .hd h3 { font-size: 26px; }
.lvl .hd .w { margin-left: auto; font-family: Bahnschrift, sans-serif; font-weight: 700; font-size: 30px; }
.bar { display: grid; grid-template-columns: 70px 1fr 80px; gap: 12px; align-items: center; margin: 6px 0; font-size: 18px; }
.bar .tr { height: 22px; background: rgba(255,255,255,.08); border-radius: 5px; overflow: hidden; }
.bar .fl { height: 100%; border-radius: 5px; }
.bar .n { text-align: right; font-weight: 700; color: var(--txt); }
.post { display: grid; grid-template-columns: 110px 1fr; gap: 16px; align-items: center; }
.mob { display: flex; flex-direction: column; align-items: center; gap: 2px; }
.mob b { font-size: 17px; color: var(--txt); }
"""

LEVEL_COLOR = ["#9aa6b5", "#ffb020", "#ff5a4d"]


def _meter(frac: float, color: str, label: str | None = None) -> str:
    return (f'<div class="meter"><span class="tr"><span class="fl" style="display:block;width:{min(100, frac * 100):.0f}%;'
            f'background:{color}"></span></span><span>{label if label is not None else pct(frac)}</span></div>')


FLAVOR = [
    "<b>Alpha</b>, a gutted ruin: a few lit lamp lines, fallen rooms, rubble piles, furniture gone, dead fields, a guard spawner in about one room in five.",
    "<b>Bravo</b>, half-staffed: lit rooms still in service, dark rooms mothballed and shut down, a guard spawner in most rooms.",
    "<b>Charlie</b>, fully staffed: every lamp burns, furnaces lit, crops growing, every machine in working order, one or two guard spawners in every room.",
]


def _finds(state: str) -> str:
    lo, hi = DRAW[state]["real"]
    return f"{lo} to {hi}"


def activity_table() -> str:
    def row(label, vals, note=""):
        cells = "".join(f'<td class="n">{v}</td>' for v in vals)
        n = f' <span class="dim" style="font-size:14px;font-weight:400">{note}</span>' if note else ""
        return f'<tr><td>{label}{n}</td>{cells}</tr>'

    P = PROFILES
    M_ = lambda key, i: _meter(P[i][key], LEVEL_COLOR[i])  # noqa: E731
    M_spawn = lambda i, n: _meter(P[i]["spawn"][n], LEVEL_COLOR[i])  # noqa: E731
    rows = [
        row("Room collapses", [M_("collapse", i) for i in range(3)], "rooms only; halls stay walkable"),
        row("Wall breaches", [M_("breach", i) for i in range(3)], "per damaged wall cell"),
        row("Floor holes", [M_("hole", i) for i in range(3)], "per damaged floor cell"),
        row("Rubble", [M_("rubble", i) for i in range(3)], "per wall or floor cell"),
        row("Lamps lit", [M_("lit", i) for i in range(3)], "by lamp line, by section, all"),
        row("Rooms", ["gutted", "in service where lit, mothballed where dark", "staffed"], "every room follows its state"),
        row("Furniture gone", [_meter(NORMAL_LOST[3], LEVEL_COLOR[0]),
                               f"{pct(NORMAL_LOST[1])} lit, {pct(NORMAL_LOST[2])} dark", _meter(NORMAL_LOST[0], LEVEL_COLOR[2])],
            "a ruin also gets rubble piles"),
        row("Machine slots left empty", [M_("absent", i) for i in range(3)], "stripped to the floor"),
        row("Dead machines", [M_("dead", 0), f"{pct(IN_SERVICE_DEAD)} lit, {pct(MOTHBALLED_DEAD)} dark", _meter(0, LEVEL_COLOR[2])],
            "a dead machine is a bare hull"),
        row("Pump room flooded", ["Yes" if p["flooded"] else "No" for p in P]),
        row("Loot chests in the whole complex", [f"<b>{n}</b>" for n in CHEST_TARGETS], "never an empty one; spare spots are empty crates"),
        row("Finds per chest", [_finds("GUTTED"), f"{_finds('IN_SERVICE')} lit, {_finds('MOTHBALLED')} dark", _finds("STAFFED")],
            "plus junk and spoiled food in dark and ruined rooms"),
        row("Rooms with no guard spawner", [M_spawn(i, 0) for i in range(3)], "a room rolls once"),
        row("Rooms with one", [M_spawn(i, 1) for i in range(3)]),
        row("Rooms with two", [M_spawn(i, 2) for i in range(3)], "the hub counts as a room"),
        row("Elite spawners", ["none", "2 in 5, deep rooms", "2 in 5, deep rooms"], "rooms 30+ blocks from the hub"),
        row("Schematic chip in a chest", [pct(p["chip"]) for p in P], "Safe, Comms or Control chest"),
        row("The silo's missing part in a chest", [pct(p["part"]) for p in P], "one part per silo"),
        row("Hub sign, Activity Level", ["Alpha", "Bravo", "Charlie"], "under the Silo Complex sign"),
    ]
    tot = sum(ACT_WEIGHTS)
    ths = "".join(f'<th style="text-align:center;color:{LEVEL_COLOR[i]};vertical-align:top;width:25%"><span style="font-size:22px">{lv}</span>'
                  f'<span style="font-size:22px;margin-left:8px">{pct(ACT_WEIGHTS[i] / tot)}</span>'
                  f'<div style="text-transform:none;letter-spacing:0;font-size:15px;font-family:inherit;font-weight:400;color:var(--muted);margin-top:4px;line-height:1.25">{FLAVOR[i]}</div></th>' for i, lv in enumerate(LEVELS))
    return f'<table class="t"><tr><th style="vertical-align:top;width:25%">Share of complexes, and what each level does</th>{ths}</tr>{"".join(rows)}</table>'


def planet_rows() -> str:
    rows = []
    for t in sorted(k for k in PLANET_TIERS if k >= 0):
        c = ceiling_of(t)
        planets = ", ".join(PLANET_NAMES[n] for n in PLANET_TIERS[t])
        rocket = "None" if t == 0 else f"T{t}"
        metal = METAL[LADDER[c]]
        body = f"T{max(1, min(3, c - 3))} body"
        guns = vname(min(c, 7))
        lvmv, top = machine_share(c)
        rows.append(f"""<tr><td style="white-space:nowrap">{rocket}</td><td style="color:var(--txt)">{planets}</td>
<td class="n">{chip(vname(c))}</td><td style="white-space:nowrap"><b>{metal}</b></td><td style="white-space:nowrap">{vname(1)}&ndash;{vname(c)}</td><td class="n">{lvmv * 100:.0f}%</td><td class="n">{top * 100:.0f}%</td><td>{guns}</td><td style="white-space:nowrap">{body}</td>
<td class="n">{danger(c)}</td></tr>""")
    return "".join(rows)


def ross_card() -> str:
    tot = sum(ROSS_WEIGHTS)
    cols = []
    mx = max(ROSS_WEIGHTS)
    for i, w in enumerate(ROSS_WEIGHTS):
        c = 4 + i
        h = int(round(w / mx * 120)) + 6
        cols.append(f"""<div style="display:flex;flex-direction:column;align-items:center;justify-content:flex-end;gap:5px;width:78px">
<b style="color:var(--txt);font-size:17px">{w / tot * 100:g}%</b><div style="width:46px;height:{h}px;background:var(--orange);border-radius:5px 5px 0 0"></div>{chip(vname(c))}</div>""")
    return "".join(cols)


def build_page() -> str:
    d = CM.load()
    main = CM.plan_with(d, 6)
    px = 4
    mp = CM.raster(d, main, px)
    ov = CM.overlay(d, main, px, fs=11.5)
    crops = "".join(f'<div style="text-align:center">{img(_crop(d, CM.plan_with(d, n), 4), h=250, cls="px", style="border-radius:8px;border:1px solid var(--line)")}'
                    f'<div class="cap" style="margin-top:3px"><b style="color:var(--txt)">{n}</b> &middot; {SILO_ODDS[n - MIN_SILOS]}%</div></div>'
                    for n in range(MIN_SILOS, MAX_SILOS + 1))
    key_rows = "".join(f'<div class="lk"><i style="background:rgb{rgb}"></i>{esc(name)}</div>' for name, rgb, _ in CM.ROLES)
    hall_key = "".join(f'<div class="lk"><i style="background:rgb{rgb}"></i>{name}</div>' for name, rgb in [
        ("Major hall", CM.HALLS["major"]), ("Minor hallway", CM.HALLS["minor"]), ("Silo gallery", CM.HALLS["gallery"])])

    stats = "".join(f'<div class="stat"><div class="v">{v}</div><div class="k">{k}</div></div>' for v, k in [
        (f"{SIZE} &times; {SIZE}", "blocks across, one flat level"),
        (f"{DEPTH}", "blocks under the ground"),
        (f"{MIN_SILOS} to {MAX_SILOS}", "real silos, each missing one part"),
        (f"{MIN_ROOMS} to {MAX_ROOMS}", "rooms, around a hub and its halls"),
    ])
    hero = hero_room()
    shafts = "".join(f'<div style="text-align:center">{img(shaft(v), h=190, style="margin:0 auto")}<div class="cap" style="margin-top:2px"><b style="color:var(--txt)">{n}</b></div></div>'
                     for v, n in [("open", "Open shaft"), ("half", "Half open"), ("closed", "Closed, two air gaps")])
    radar_a, radar_b = radar_station(False), radar_station(True)
    swatches = palette_swatches()

    # ---- rooms
    rooms = "".join(room_card(k) for k in _ROOM_ORDER)

    # ---- guards
    soldier = M._enemy_pic("Army Soldier", dict(model="biped", skin="army_soldier", pose=K.AIM, gun_name="m4", armor=None))
    commando = M._enemy_pic("Commando", dict(model="biped", skin="army_soldier", pose=K.AIM, gun_name="m4", armor=None))
    storm = M._enemy_pic("Stormtrooper", dict(model="biped", skin="army_soldier", pose=K.AIM, gun_name="blaster_rifle", armor=None))
    guard_post = elite_post = spawner_block()

    # ---- machines
    hulls = "".join(f'<div style="text-align:center">{img(hull_block(t), h=64)}<div class="cap" style="margin-top:0">{t}</div></div>' for t in ("LV", "MV", "HV", "EV"))

    # ---- take over
    front = [silo_front("controller"), silo_front("hatch"), silo_front(None)]

    # ---- data stick, ceilings
    levels = ""

    body = f"""
<div style="display:grid;grid-template-columns:690px 1fr;gap:22px">
<div class="card"><div class="lead">Out in the world there are <b>complexes</b>: old missile bases, dug in and forgotten. From the surface you see only a few
concrete shafts, sometimes a small radar hut, and a trapdoor. Under them is a whole base, with <b>real missile silos</b> that need one part to work again.</div>
<div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:16px">{stats}</div>
<div class="pt" style="margin-top:14px">A <b>hub</b>, the Assembly Bay ({HUB[0]} &times; {HUB[1]}), sends out three or four <b>major halls</b> {MAJOR_W} wide. They branch and bend, and narrow <b>minor hallways</b> leave their sides. The rooms hang off all of them,
and one major hall is the <b>silo gallery</b>. Complexes are rare:
a quarter of the grid squares ({CHANCE_PCT}%), one square per 1,024 &times; 1,024 blocks, and never on water or a steep slope. On the Overworld they keep out of hills and mountains. They generate in the Overworld
and on every planet with solid ground.</div></div>
<div class="card" style="display:flex;flex-direction:column"><div class="spot" style="flex:1;display:flex;align-items:center;justify-content:center;padding:6px">{img(hero, h=440, style="max-width:100%")}</div>
<div class="cap">A generator hall. One block makes the walls, here the <b style="color:var(--txt)">Silo Complex Wall</b>, which joins into one plated mass; the floor is a pattern; lamps sit flush in the ceiling with a light tile in the floor under each; the machines are GregTech
hulls; a guard post, a monster spawner, waits in the corner. A breach in the wall and a hole in the floor are the marks of a complex that lost its crew long ago.</div></div></div>

<div class="sec"><h2>On the surface</h2>
<div class="card" style="display:grid;grid-template-columns:1.55fr 1fr;gap:26px">
<div><div class="pt" style="margin-bottom:6px">Every silo reaches the surface as a <b>shaft</b>: a ring of light concrete around a 3 &times; 3 bore, flush with the ground. Some are open, some half capped and some sealed
with two air gaps left to climb.</div>
<div style="display:flex;justify-content:space-around;align-items:flex-end;gap:10px">{shafts}</div></div>
<div class="pt"><b>Radar stations.</b> Up to two stand on the surface, well apart from each other: a small hut of works casing with a radar on its roof. A station is inert until you add an Energy Hatch (see the end of this sheet).
<div style="margin-top:12px"><b>Escape ladder.</b> The base has no door but one: a 1 &times; 1 ladder shaft at a dead end, with a closed trapdoor level with the ground.</div>
<div style="margin-top:12px"><b>Nothing else</b> marks the place. A player walking past sees a few concrete squares in the grass.</div></div></div></div>

<div class="sec"><h2>The layout</h2>
<div style="display:grid;grid-template-columns:960px 1fr;gap:22px;align-items:start">
<div class="mapbox"><img src="{K.url(mp)}" width="{SIZE * px}" height="{SIZE * px}">{ov}</div>
<div class="card"><div class="label" style="margin-bottom:8px">How to read the map</div>
<div class="pt" style="margin-bottom:10px">Looking down from above, north up. This is one real complex with {main["siloCount"]} silos, drawn from the same plan the game builds. Each room is tinted by what it is for.</div>
{key_rows}
<div class="lk"><i class="d" style="background:#ff4d4d"></i>Guard spawner spot (a room fills 0 to 2)</div>
<div class="lk"><i style="background:#1b2129;border:2px solid #ffd23f;height:9px;border-radius:2px"></i>Blast door</div>
<div class="lk"><i style="background:#ffd23f"></i>Chest spot (a chest or a crate)</div>
<div class="lk"><i class="c" style="background:#2fd0ff"></i>Machine</div>
<div class="lk"><i style="background:#ff8a1f;border-color:#ff8a1f"></i>Silo outline</div>
<div class="lk"><i style="background:#22d36b"></i>Escape ladder</div>
<div class="lk"><i class="c" style="background:rgba(47,208,255,.3);border:2px dashed #2fd0ff"></i>Radar station, on the surface</div>
<div class="label" style="margin:12px 0 6px">Halls</div>
{hall_key}</div></div>
<div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:16px;margin-top:16px">
<div class="card"><h3>The hub</h3><div class="pt" style="margin-top:6px">The Assembly Bay sits near the middle, {HUB[0]} &times; {HUB[1]} and {HUB_H} high, a container yard with salvage chests, a workbench and guards. Its sign names the complex's <b>class</b>, its loot ceiling, and the sign under it its <b>Activity Level</b>: Alpha, Bravo or Charlie.
A decontamination airlock waits in the corner between the hub and a hall.</div></div>
<div class="card"><h3>Halls and rooms</h3><div class="pt" style="margin-top:6px">Major halls are {MAJOR_W} wide and {MAJOR_H} high, with a rail down the middle; minor hallways are {MINOR_W} wide and {MINOR_H} high. Rooms are {ROOM_H} high, grow to fill the rock around them and
can hang off other rooms, so not every room has a straight line to the hub. About {COVERAGE}% of the footprint is built. Each major hall leaves the hub through a <b>blast door</b> and past a sign naming its direction. Three to five small guard rooms stand beside the hall mouths and branches.</div></div>
<div class="card"><h3>The silo gallery</h3><div class="pt" style="margin-top:6px">The silos line a {GALLERY_H}-high hall on both sides, in facing pairs {SILO_PITCH} blocks apart, each turned so its controller faces the hall. A walkway niche opens in front of each controller.
<b>Launch Control</b> caps the far end behind glass, so you look down on the silos from it. The <b>Missile Assembly</b> floor opens off the gallery's side. With seven or more silos the gallery can split across a second straight hall.</div></div></div>
<div class="label" style="margin:16px 0 8px">Galleries by silo count, and how often each count turns up</div>
<div style="display:flex;justify-content:space-between;align-items:flex-end;gap:8px">{crops}</div></div>

<div class="sec"><h2>The rooms</h2>
<div class="pt" style="margin-bottom:10px">A complex holds {MIN_ROOMS} to {MAX_ROOMS} rooms besides the hub. Seven kinds are always present: Launch Control, Missile Assembly, Comms, the Commander's Office, the Warhead Magazine, the Generator Hall and the Armory. The rest are dealt like a deck of cards: every kind that fits turns up before any kind repeats. The sign by every door names the room. Every room is laid out to its own plan, in rows and islands between the lamp lines, with a clear way from the door to every chest, machine and door.</div>
<div class="rooms">{rooms}</div>
<div class="cap">Sizes are the room's starting footprint, length &times; depth in blocks; rooms then grow into the rock around them. "Dealt" rooms come from the deck. A whole complex holds exactly {CHEST_TARGETS[0]}, {CHEST_TARGETS[1]} or {CHEST_TARGETS[2]} loot chests (Alpha, Bravo, Charlie), shared out by room size; the chest spots left over are empty wooden crates, and a caved-in room holds none. Each chest draws its room's table, up to the planet's ceiling (see below). The armory's display chests come on top of the count.</div></div>

<div class="sec"><h2>Light and finish</h2>
<div class="card" style="display:grid;grid-template-columns:1fr 1fr;gap:26px">
<div><h3>Lamps</h3><ul class="pt" style="margin:8px 0 0 18px;line-height:1.45">
<li>Lamps sit <b>flush in the ceiling</b> on an even grid, a line every 4 to 7 blocks as the room's layout needs, and an extra floor light wherever furniture would leave a dark corner.</li>
<li>Rooms and halls taller than {MINOR_H} also have a <b>light tile in the floor</b> under each ceiling lamp, so a fully lit section is never dark at your feet.</li>
<li>Minor hallways have ceiling lamps only, every 3 blocks.</li>
<li>Lamps go out by circuit: an abandoned complex keeps a few lamp lines burning, a partly abandoned one lights whole rooms and halls, an operational one lights everything. A dead lamp stays in place as a dark panel.</li></ul></div>
<div><h3>Finish</h3><div class="pt" style="margin-top:8px">Every room has <b>one wall block</b> all round, with its own floor and ceiling pattern, so rooms are told apart by look as well as by sign. The finish comes from the blocks of the pack: with Chisel and Ztones installed, rooms are dressed in laboratory panels, factory plating, marble, concrete and dark server plate; without them the same patterns are laid in GregTech concrete and plain vanilla blocks.
On the Moon and Mars, rugged rooms use local rock and brick. All of it mines to ordinary stone-grade blocks: no casings, frames or machines are part of a room's finish. The halls are the exception: their service runs and blast doors are real blocks.</div>
<div style="margin-top:10px">{swatches}</div></div></div></div>

{fixtures_section()}

<div class="sec"><h2>Three activity levels</h2>
<div class="pt" style="margin-bottom:10px">Each complex rolls its level once, and it never changes. The layout is the same at every level; the level decides how each room looks, what its chests hold, how much is broken and lit, and how many guards wait. Wear and rubble never block the way to a chest, a machine or a door.</div>
<div class="card" style="padding:12px 18px 6px">{activity_table()}</div>
<div class="cap">The share of each level is a server setting. Spawner odds are the same on every planet; the planet sets how well the guards are armed (see Loot by planet). Halls and caved-in rooms hold no spawner.</div></div>

<div class="sec"><h2>Guards</h2>
<div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:16px">
<div class="card"><div class="post"><div style="text-align:center">{img(guard_post, h=100)}<div class="cap" style="margin-top:0">Inside: Army Soldier or Commando</div></div><div><h3>Silo Guard Post</h3>
<div class="pt">A monster spawner of <b>Army Soldiers</b> and <b>Commandos</b>. It stands on open floor in the rooms, the hub included. The figure turning in the cage is the guard it sends next.</div></div></div>
<div style="display:flex;justify-content:space-around;align-items:flex-end;margin-top:12px"><div class="mob"><div class="guide" style="padding:0 6px">{soldier}</div><b>Army Soldier</b></div><div class="mob"><div class="guide" style="padding:0 6px">{commando}</div><b>Commando</b></div></div></div>
<div class="card"><div class="post"><div style="text-align:center">{img(elite_post, h=100)}<div class="cap" style="margin-top:0">Inside: Commando or Stormtrooper</div></div><div><h3>Silo Elite Post</h3>
<div class="pt">A monster spawner of <b>Commandos</b> and <b>Stormtroopers</b>. Two in five spawners in rooms 30 or more blocks from the hub are elite, never in an abandoned complex. A Stormtrooper in the cage marks one.</div></div></div>
<div style="display:flex;justify-content:space-around;align-items:flex-end;margin-top:12px"><div class="mob"><div class="guide" style="padding:0 6px">{commando}</div><b>Commando</b></div><div class="mob"><div class="guide" style="padding:0 6px">{storm}</div><b>Stormtrooper</b></div></div></div>
<div class="card"><h3>How the posts behave</h3>
<ul class="pt" style="margin:8px 0 0 18px;line-height:1.45">
<li>Each post is a Minecraft monster spawner with the guards inside. It wakes only when a player is within <b>24 blocks</b>, in lit rooms too.</li>
<li>At most <b>5</b> of each guard type near it, up to <b>3</b> at a time, every <b>5 to 6 seconds</b>.</li>
<li><b>Break the spawner and it is gone</b> for good. It drops nothing. By hand it is slow, as every spawner in the pack is; a missile warhead destroys it outright.</li>
<li>Guards are armed to match the planet: danger <b>3</b> up to IV, <b>4</b> at LuV and ZPM, <b>5</b> from UV up.</li>
<li>Each room rolls <b>none, one or two</b> spawners for the complex's level (see the table above), set far apart on open floor. Halls hold none.</li></ul></div></div></div>

<div class="sec"><h2>Loot by planet</h2>
<div class="pt" style="margin-bottom:10px">Nothing in a complex, chest or machine, goes above its planet's <b>ceiling</b>. The Overworld and the Moon sit at EV, and each rocket tier above the Moon lifts the ceiling by one voltage tier, to UMV for Tier 9. Chests fill with a ladder of GregTech metals, and the top rung at the ceiling is the most common. Guns come from the weapon tables and stop at ZPM, so above UHV the loot is GregTech material only.</div>
<div class="card" style="padding:10px 14px"><table class="t"><tr><th>Rocket</th><th>Planets</th><th style="text-align:center">Ceiling</th><th>Top metal</th><th>Machines</th><th style="text-align:center">LV or MV</th><th style="text-align:center">At the ceiling</th><th>Guns to</th><th>Salvage</th><th style="text-align:center">Danger</th></tr>
{planet_rows()}</table></div>
<div class="card" style="margin-top:14px;display:grid;grid-template-columns:1fr 620px;gap:22px;align-items:center">
<div><h3>Ross128b and Ross128ba</h3><div class="pt" style="margin-top:6px">The challenge-start planets have no fixed ceiling. <b>Each complex rolls its own</b>, from EV to UMV, weighted to the low end: about half are EV to IV, one in a hundred is UMV. The hall's sign and the blast door frames tell you the class you drew.</div></div>
<div style="display:flex;align-items:flex-end;justify-content:space-between">{ross_card()}</div></div></div>

<div class="sec"><h2>Real machines, real salvage</h2>
<div class="card" style="display:grid;grid-template-columns:1fr 1fr;gap:26px">
<div><div class="pt">The machines in the rooms are <b>real GregTech machines</b>: generators, Battery Buffers, Assemblers, pumps, Chemical Reactors and lab machines, recyclers and furnaces, and machine tools, plus a
<b>Launch Control</b> and Galacticraft's <b>NASA Workbench</b>. Most are LV or MV, and a machine at the planet's ceiling is a rare find (see the table above); radar and comms consoles sit at the ceiling or one or two tiers under it, never under HV. Air handlers, fuel tanks and consoles are machine hulls. Working machines sit idle, often holding a few batches of finished goods. You can pick up what you find, though a dead machine is only the bare hull: nearly all of them in a ruin and in a partly abandoned complex's dark rooms, a few in its lit rooms. About {pct(PROFILES[0]["absent"])} of a ruin's machine slots are simply gone, so a working machine there is a rare find.</div>
<div style="display:flex;justify-content:space-around;align-items:flex-end;margin-top:8px">{hulls}</div></div>
<div><div class="pt"><b>Nothing live is left behind.</b> The magazine holds inert warheads, fuzes and stainless steel frame boxes, never a live warhead. The hub's chests hold a <b>damaged missile body</b> (too broken to stand; it recycles for its plates), the missile assembly floor now and then a <b>whole missile body</b> (a damaged one in a ruin),
the fuel depot <b>rocket fuel cells</b>, and the safe, comms and control chests now and then a <b>schematic chip</b>. The <b>Silo Complex Wall</b> mines to itself: a handsome plated block for your own base, though no machine takes it.</div>
<div style="display:flex;justify-content:center;margin-top:8px">{img(wall_block(), h=64)}</div></div></div></div>

<div class="sec"><h2>Take over a silo</h2>
<div class="card"><div class="pt" style="margin-bottom:10px">Every silo in a complex is a real <b>Missile Silo</b>, built from the same structure you would build yourself, with its bus and hatches in place. Each is missing <b>exactly one part</b>. Put it back and the silo forms.</div>
<div style="display:grid;grid-template-columns:repeat(3,1fr);gap:18px">
<div style="text-align:center"><div class="spot">{img(front[0], h=250)}</div><div class="cap"><b style="color:var(--txt)">No controller.</b> A concrete block stands in the middle of the front wall.</div></div>
<div style="text-align:center"><div class="spot">{img(front[1], h=250)}</div><div class="cap"><b style="color:var(--txt)">No Energy Hatch.</b> A concrete block stands where the Energy Hatch goes, at the foot of the right-hand hatch column.</div></div>
<div style="text-align:center"><div class="spot">{img(front[2], h=250)}</div><div class="cap"><b style="color:var(--txt)">Repaired.</b> The controller (blue ring), the hatches and the silo&#39;s own Launch Control (orange rings) in their places.</div></div></div>
<div style="display:grid;grid-template-columns:repeat(4,1fr);gap:18px;margin-top:16px">
<div class="step"><span class="num">1</span><div class="t"><b>Clear the way.</b> The silo gallery holds no spawners, but Launch Control and Missile Assembly roll theirs like any room, and elite posts are likely that deep. Mine the spawners (slow by hand), blow them up with a warhead, or fight what they spawn.</div></div>
<div class="step"><span class="num">2</span><div class="t"><b>Find the part.</b> Check the Safe, Stockpile, Workshop and Control chests: about {pct(PROFILES[0]["part"])} of abandoned, {pct(PROFILES[1]["part"])} of partly abandoned and {pct(PROFILES[2]["part"])} of operational silos have theirs lying in one. A missing part is not lost: the controller and a GregTech Energy Hatch are both craftable.</div></div>
<div class="step"><span class="num">3</span><div class="t"><b>Put it in.</b> Mine the concrete and set the controller facing out, or mine the concrete at the foot of the right-hand hatch column and set the Energy Hatch there. The silo forms and switches itself on.</div></div>
<div class="step"><span class="num">4</span><div class="t"><b>Power and launch.</b> Run EU to the Energy Hatch. A found hatch is EV, which launches up to Tier 2; fit a LuV hatch for Tier 3. Then fuel it, load a missile and fire it from the silo&#39;s own Launch Control, on top of the right-hand hatch column: it runs on the silo&#39;s EU.</div></div></div></div></div>

<div class="sec"><div style="display:grid;grid-template-columns:1.25fr 1fr;gap:20px">
<div class="card"><h2>The radar station</h2>
<div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-top:6px">
<div style="text-align:center"><div class="spot">{img(radar_a, h=250)}</div><div class="cap"><b style="color:var(--txt)">As found.</b> A Radar Casing sits where the Energy Hatch goes. The Maintenance Hatch is in place.</div></div>
<div style="text-align:center"><div class="spot">{img(radar_b, h=250)}</div><div class="cap"><b style="color:var(--txt)">With a hatch.</b> Add an Energy Hatch, power it, and the radar forms and its dish starts to turn.</div></div></div>
<div class="pt" style="margin-top:10px">Mine the casing directly under the radar controller and put an <b>Energy Hatch</b> there. The controller is HV, so it takes an HV Energy Hatch. A working radar flags incoming missiles for your batteries, as usual.</div></div>
<div class="card"><h2>The data stick</h2>
<div class="pt" style="margin-top:6px">A Comms room chest sometimes holds a <b>Data Stick</b> named <b>Silo Complex Coordinates</b>. Its lore names the X and Z of the <b>nearest other complex</b>, the middle of its Assembly Bay, within about three grid squares. The chance is {pct(PROFILES[0]["chip"] / 2)} in an abandoned complex, {pct(PROFILES[1]["chip"] / 2)} in a partly abandoned one and {pct(PROFILES[2]["chip"] / 2)} in an operational one, and only when another complex is near.</div>
<div style="display:flex;align-items:center;justify-content:center;gap:14px;margin:14px 0 6px">
<div class="stat" style="text-align:center"><div class="v" style="font-size:24px">This complex</div><div class="k">Comms chest</div></div><div class="arrow">&rarr;</div>
<div class="stat" style="text-align:center"><div class="v" style="font-size:24px">X, Z</div><div class="k">Data Stick lore</div></div><div class="arrow">&rarr;</div>
<div class="stat" style="text-align:center"><div class="v" style="font-size:24px">Next complex</div><div class="k">Assembly Bay</div></div></div>
<div class="cap">Dig down under the coordinates to find it. A stick can point at a place where the terrain turned the complex away, so go prepared.</div></div></div></div>
"""
    credit = [
        "Silo Complex Wall: Steel Bastion Plating art, with connected textures. Radar Casing, dish, radar controller, Missile Silo and Launch Control overlays: derived from HBM's Nuclear Tech Mod, GNU LGPL v3; modified.",
        "Machine hulls, hatch overlays and Light and Dark Concrete Bricks: GregTech 5 Unofficial textures, LGPL-3.0. Hut casings and the Silo Complex Wall: GregGuns art, MIT. Guard posts: the Minecraft monster spawner (Mojang).",
        TP.GC_CREDIT, TP.TC_CREDIT, MOJANG_CREDIT, TP.TINT_CREDIT,
    ]
    return page_doc("Abandoned <b>silos</b>", "Underground missile bases to find, clear and take over: what is in them, what guards them, and what you can carry out.",
                    body, extra=CSS + TP.PART_CSS, credit=credit)


def silos() -> str:
    return build_page()


PAGES = {"icbm-silos": silos}

if __name__ == "__main__":
    import build
    build.main(["icbm-silos"])
