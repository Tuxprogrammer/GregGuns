"""The icbm-silos guide sheet: abandoned silo complexes as dungeons.

What a complex is, a map of its layout, the rooms and what each holds, the three activity levels, the guards, the loot
ceiling of every planet, the real machines, how to take a silo over, the radar station and the data stick.

Every number is read from the Java sources at build time (`_const`-style regexes on `ActivityProfile`, `IcbmConfig`,
`PlanetTable`, `SiloCeiling`, `SiloLootTables`, `RoomKind` and the room switch in `ComplexLayout`), and each formula the
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
HEIGHT = _layout_const("HEIGHT")
SLOT_PITCH = _layout_const("SLOT_PITCH")
SLOTS_PER_SIDE = _layout_const("SLOTS_PER_SIDE")
WING_WIDTH = _layout_const("WING_WIDTH")
SILO_H = _layout_const("SILO_HEIGHT")
DEPTH = int(re.search(r"DEPTH\s*=\s*(\d+);", _src("icbm/complex/gen/ComplexPlacer.java")).group(1))
HALL = (_layout_const("HALL_MAX_X") - _layout_const("HALL_MIN_X"), _layout_const("HALL_MAX_Z") - _layout_const("HALL_MIN_Z"))
_lay = _src("icbm/complex/ComplexLayout.java")
_must(_lay, "int roll = rnd.nextInt(100);", "the silo-count roll")
_must(_lay, "roll < 78 ? 1 : roll < 93 ? 2 : roll < 98 ? 3 : 4", "the silo-count odds")
SILO_ODDS = [78, 93 - 78, 98 - 93, 100 - 98]
_must(_lay, "slotCount * (60 + rnd.nextInt(26)) / 100", "the share of room slots used")
SLOTS_USED = (60, 85)
WING_LEN = 90
_must(_lay, "CORRIDOR_LO = 118, CORRIDOR_HI = 123", "the corridor width")
_must(_src("icbm/complex/gen/ComplexPlacer.java"), "MIN_FLOOR", "the floor rule")

LEVELS = ["Abandoned", "Partly abandoned", "Operational"]


def _profiles() -> list[dict]:
    """ActivityProfile's three constants, in the constructor's own argument order."""
    s = _src("icbm/worldgen/ActivityProfile.java")
    out = []
    for m in re.finditer(r"private static final ActivityProfile (\w+) = new ActivityProfile\((.*?)\);", s, re.S):
        body = re.sub(r"new double\[\]\s*\{([^}]*)\}", lambda a: "ARR[" + a.group(1).replace(",", ";") + "]", m.group(2))
        args = [a.strip() for a in body.split(",")]
        arr = [float(x) for x in re.search(r"ARR\[(.*)\]", args[9]).group(1).split(";")]
        out.append(dict(
            name=m.group(1), collapse=float(args[1]), breach=float(args[2]), rubble=float(args[3]), hole=float(args[4]),
            lit=float(args[5]), broken=float(args[6]), dead=float(args[7]), flooded=args[8] == "true", spawn=arr,
            shaft=float(args[10]), richness=int(args[11]), chip=float(args[12]), part=float(args[13])))
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
    _must(cc, "1.0 + 0.5 * (c - SiloCeiling.EV) / (SiloCeiling.UMV - SiloCeiling.EV)", "the guard scale")
    _must(cc, "Math.max(1, Math.min(3, ceiling - 3))", "the salvaged body tier")
    _must(cc, "Math.max(Math.min(3, ceiling), ceiling - below)", "the machine tier rule")
    _must(cc, "Long.remainderUnsigned(h, 3)", "the machine tier spread")
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


def guard_scale(c: int) -> float:
    return 1.0 + 0.5 * (c - 4) / (12 - 4)


def machine_range(c: int) -> str:
    lo = max(min(3, c), c - 2)
    return vname(c) if lo == c else f"{vname(lo)} to {vname(c)}"


def pct(x: float) -> str:
    v = x * 100
    return f"{v:g}%"


# ---------------------------------------------------------------- rooms, from the layout code

ROOMS = re.findall(r"^\s+([A-Z_]+)\((\d+), (\d+), (true|false)\)", _src("icbm/complex/RoomKind.java"), re.M)
ROOM_INFO = {k: dict(length=int(a), depth=int(b), mandatory=c == "true") for k, a, b, c in ROOMS}


def _room_contents() -> dict:
    """{room kind: {chests: {key: n}, machines: {role: n}, posts: n, frames: bool}} from ComplexLayout's room switch."""
    s = _src("icbm/complex/ComplexLayout.java")
    sw = s[s.index("switch (r.kind()) {"):]
    sw = sw[:sw.index("default:")]
    out = {}
    for m in re.finditer(r"case (\w+):(.*?)break;", sw, re.S):
        kind, body = m.group(1), m.group(2)
        d = dict(chests={}, machines={}, posts=0, frames="addFrameWall" in body)
        mult = 1
        for line in re.split(r";\s*\n", body):
            fm = re.search(r"for \(int i = 0; i < (\d+); i\+\+\)", line)
            mult = int(fm.group(1)) if fm else 1
            pm = re.search(r"put\(r, f, MarkKind\.(\w+), [^,]+, [^,]+, ([^)]*\)?)", line)
            if not pm:
                continue
            what, key = pm.group(1), pm.group(2)
            if what == "LOOT_CHEST":
                k = re.search(r"LootKeys\.(\w+)", key).group(1).lower()
                d["chests"][k] = d["chests"].get(k, 0) + mult
            elif what == "MACHINE":
                role = re.search(r"MachineRole\.(\w+)", key).group(1)
                d["machines"][role] = d["machines"].get(role, 0) + mult
            elif what == "SPAWNER":
                d["posts"] += mult
        out[kind] = d
    # The hall's furnishing is its own method.
    h = s[s.index("private static void fillHall"):]
    h = h[:h.index("\n    }\n")]
    d = dict(chests={}, machines={}, posts=0, frames=False)
    for m in re.finditer(r"r\.add\(MarkKind\.(\w+), [^;]*?(LootKeys\.\w+|MachineRole\.\w+\.name\(\)|\"room\")", h):
        what, key = m.group(1), m.group(2)
        if what == "LOOT_CHEST":
            k = key.split(".")[-1].lower()
            d["chests"][k] = d["chests"].get(k, 0) + 1
        elif what == "MACHINE":
            role = re.search(r"MachineRole\.(\w+)", key).group(1)
            d["machines"][role] = d["machines"].get(role, 0) + 1
        elif what == "SPAWNER":
            d["posts"] += 1
    out["ASSEMBLY_BAY"] = d
    return out


ROOM_CONTENTS = _room_contents()
if not ROOM_CONTENTS.get("ARMORY", {}).get("frames") or ROOM_CONTENTS["ASSEMBLY_BAY"]["chests"].get("salvage") != 4:
    raise SystemExit("the room contents read from ComplexLayout look wrong; update pages_icbm_silos._room_contents")

LOOT_TEXT = {
    "salvage": "iron, steel, copper, coal, ceiling-tier plates, a damaged missile body",
    "stockpile": "iron, steel, copper, coal, ceiling-tier plates and ingots",
    "safe": "ceiling-tier plates and ingots, gold, silver, a gun",
    "comms": "plastic, rubber, gold and fine gold wire",
    "armory": "steel and guns of the ceiling tier",
    "workshop": "ceiling-tier plates, rods, screws and gears",
    "medical": "golden apples, paper, potatoes, rubber, plastic",
    "galley": "bread, beef, pork, apples, a bucket",
    "bunks": "leather, wool, paper, books, gold nuggets",
    "fuel": "coal, sulfur, steel and aluminum plates, rocket fuel cells",
    "control": "plastic, gold wire, plates, a gun",
    "magazine": "gunpowder, lead, steel and tungstensteel plates, 4 stainless steel plates",
}
CHEST_NAME = {"salvage": "Salvage", "stockpile": "Stockpile", "safe": "Safe", "comms": "Comms", "armory": "Armory",
              "workshop": "Workshop", "medical": "Medical", "galley": "Galley", "bunks": "Bunk", "fuel": "Fuel",
              "control": "Control", "magazine": "Magazine"}
MACHINE_NAME = {
    "GENERATOR": "GregTech generator", "BATTERY_BUFFER": "Battery Buffer", "LAUNCH_CONTROL": "Launch Control",
    "NASA_WORKBENCH": "NASA Workbench", "PUMP": "GregTech pump", "AIR_HANDLER": "air handler (machine hull)",
    "FUEL_TANK": "fuel tank (machine hull)", "WORKBENCH": "Assembler", "RADAR": "radar console (machine hull)",
    "COMMS": "comms console (machine hull)", "MAGAZINE_CASING": "stainless steel frame box",
}
ROOM_NAME = dict(CM.NAMES)
ROOM_NAME["GUARD_POST"] = "Guard Post"
ROOM_NAME["ASSEMBLY_BAY"] = "Assembly Bay"
ROOM_SPECIAL = {
    "WARHEAD_MAGAZINE": "Fallout rubble; no warhead is ever stored.",
    "PUMP_ROOM": "Floods when the complex is not operational.",
    "ARMORY": "Two rows of guns in item frames on the back wall.",
        "GUARD_POST": "One at every wing mouth.",
    "DECON_AIRLOCK": "On one wing, by the hall.",
}
_ROOM_ORDER = ["ASSEMBLY_BAY", "LAUNCH_CONTROL", "COMMS", "COMMANDER_OFFICE", "WARHEAD_MAGAZINE", "GENERATOR_HALL", "ARMORY",
               "RADAR_TRACKING", "FUEL_DEPOT", "AIR_HANDLING", "PUMP_ROOM", "WORKSHOP", "STOCKPILE", "BUNKS", "MESS_GALLEY",
               "INFIRMARY", "DECON_AIRLOCK", "GUARD_POST"]
if sorted(_ROOM_ORDER) != sorted(ROOM_INFO):
    raise SystemExit(f"RoomKind changed: {sorted(set(ROOM_INFO) ^ set(_ROOM_ORDER))}; update pages_icbm_silos._ROOM_ORDER")


def _plural(n: int, w: str) -> str:
    return f"{n} {w}" if n == 1 else f"{n} {w}s"


def room_card(kind: str) -> str:
    info, c = ROOM_INFO[kind], ROOM_CONTENTS.get(kind, dict(chests={}, machines={}, posts=0, frames=False))
    role, rgb = CM.ROLE_OF[kind]
    sw = f"rgb({rgb[0]},{rgb[1]},{rgb[2]})"
    size = f"{info['length']} &times; {info['depth']}"
    tag = ("always" if info["mandatory"] else "sometimes") if kind not in ("ASSEMBLY_BAY", "GUARD_POST", "DECON_AIRLOCK") else \
        {"ASSEMBLY_BAY": "one", "GUARD_POST": "four", "DECON_AIRLOCK": "one"}[kind]
    lines = []
    for k, n in c["chests"].items():
        lines.append(f"<b>{_plural(n, CHEST_NAME[k] + ' chest')}</b>: {LOOT_TEXT[k]}")
    for r, n in c["machines"].items():
        lines.append(f"<b>{MACHINE_NAME[r]}</b>" + (f" &times; {n}" if n > 1 else ""))
    if c["frames"]:
        lines.append("<b>Item frames</b> with guns on the back wall")
    if kind in ROOM_SPECIAL and kind != "ARMORY":
        lines.append(ROOM_SPECIAL[kind])
    if c["posts"]:
        lines.append(f"{_plural(c['posts'], 'guard post')} possible")
    body = "".join(f"<li>{ln}</li>" for ln in lines) or "<li>Empty but for lamps and a sign.</li>"
    return (f'<div class="room"><div class="rh"><i style="background:{sw}"></i><b>{esc(ROOM_NAME[kind])}</b>'
            f'<span>{size} &middot; {tag}</span></div><ul>{body}</ul></div>')


# ---------------------------------------------------------------- blocks and renders

PLAIN = lambda name: K.ASSETS / "textures/blocks" / f"{name}.png"  # noqa: E731


def hero_room() -> Path:
    """A generator hall cutaway: Silo Complex Wall (connected) walls and floor, EV machine hulls, lamps, a Silo Guard Post, a
    breach in the wall and a hole in the floor."""
    b = S.Build("silos-hall")
    cw = S._wall("complex_wall")
    side, top = gt.hull_tinted("EV"), gt.hull_tinted("EV", "TOP")
    b.define("w", "Silo Complex Wall", cw)
    b.define("hull", "EV Machine Hull", (top, side, side))
    b.define("lamp", "Lamp", S.lamp_tex())
    b.define("post", "Silo Guard Post", PLAIN("concrete_hole"))
    X, Z = 12, 9
    cells = {}
    for x in range(X):
        for z in range(Z):
            cells[(x, 0, z)] = "w"
    for y in range(1, 6):
        for z in range(Z):
            cells[(X - 1, y, z)] = "w"
        for x in range(X):
            cells[(x, y, Z - 1)] = "w"
    for y in (2, 3, 4):      # the doorway into the wing
        for x in (5, 6):
            cells.pop((x, y, Z - 1), None)
    for p in [(X - 1, 3, 2), (X - 1, 4, 2), (X - 1, 3, 3), (X - 1, 2, 3)]:     # a breach in the east wall
        cells.pop(p, None)
    for p in [(3, 0, 1), (3, 0, 2), (4, 0, 2)]:                                # a hole in the floor
        cells.pop(p, None)
    for x in (2, 5, 8):
        cells[(x, 1, 4)] = "hull"
    for x in (2, 9):
        cells[(x, 5, Z - 1)] = "lamp"
    cells[(X - 2, 1, Z - 2)] = "post"
    b.blocks.update(cells)
    return b.render(yaw=-34, pitch=30, ppu=50, tag="")


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
    """The front wall of a complex silo, with the controller and hatches where the complex places them. missing: None, "controller" or "hatch"."""
    b = SILO.silo_build()
    cells = SILO.silo_cells()
    for pos, _, _ in SILO.HATCHES:
        b.blocks[pos] = {"C": "casing", "G": "glass", "B": "beam"}[cells[pos]]
    b.hatches = []
    gc = S._casing("gantry_casing")
    b.blocks[(2, 2, 0)] = "casing" if missing == "controller" else ("ctrl", FRONT)
    b.hatch((1, 2, 0), "input_bus", FRONT, gc)
    b.hatch((3, 2, 0), "maintenance_hatch", FRONT, gc)
    b.hatch((3, 1, 0), "input_hatch", FRONT, gc)
    if missing != "hatch":
        b.hatch((2, 1, 0), "energy_hatch", FRONT, gc)
    else:
        b.blocks[(2, 1, 0)] = "glass"
    return SILO.render(b, yaw=-22, pitch=22, ppu=84, ground=False, missile=False, marks=True, cut=lambda x, y, z: y > 3 or z > 1)


def hull_block(tier: str) -> Path:
    side, top = gt.hull_tinted(tier), gt.hull_tinted(tier, "TOP")
    return K.voxels(f"silos-hull-{tier}", {(0, 0, 0): "h"}, {"h": (top, side, side)}, yaw=-34, pitch=28, ppu=96)


def post_block(name: str) -> Path:
    t = PLAIN(name)
    return K.voxels(f"silos-post-{name}", {(0, 0, 0): "h"}, {"h": (t, t, t)}, yaw=-34, pitch=28, ppu=96)


def wall_block() -> Path:
    cw = S._wall("complex_wall")
    return K.voxels("silos-wall", {(0, 0, 0): "w"}, {"w": cw}, yaw=-34, pitch=28, ppu=96)


def _crop(d: dict, p: dict, px: int) -> Path:
    """The silo cluster (north-east quadrant) of a plan, outlined in orange."""
    box = (136, 52, 206, 112)

    def make():
        im = Image.open(CM.raster(d, p, px)).convert("RGB").crop(tuple(v * px for v in box))
        dr = ImageDraw.Draw(im)
        for s in p["silos"]:
            x0, z0, x1, z1 = s["box"]
            dr.rectangle([(x0 - box[0]) * px - 3, (z0 - box[1]) * px - 3, (x1 - box[0]) * px + 2, (z1 - box[1]) * px + 2],
                         outline=(255, 138, 31), width=3)
        return im

    return K._cached("silos-crop", dict(seed=p["seed"], px=px, v=2), make)


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
    "Dark and ruined: rubble, fallen rooms, breached walls, nothing on guard.",
    "Half dead: some lamps burn, some rooms have fallen in, guards hold the deeper rooms.",
    "Lit and running: intact walls, a dry pump room, guards at the posts and the silo walkways.",
]


def activity_table() -> str:
    def row(label, vals, note=""):
        cells = "".join(f'<td class="n">{v}</td>' for v in vals)
        n = f' <span class="dim" style="font-size:14px;font-weight:400">{note}</span>' if note else ""
        return f'<tr><td>{label}{n}</td>{cells}</tr>'

    P = PROFILES
    M_ = lambda key, i: _meter(P[i][key], LEVEL_COLOR[i])  # noqa: E731
    shaft = [_meter(p["shaft"], LEVEL_COLOR[i]) if p["shaft"] else ("none" if not any(p["spawn"]) else "by depth") for i, p in enumerate(P)]
    rows = [
        row("Room collapses", [M_("collapse", i) for i in range(3)], "rooms only; wings stay walkable"),
        row("Wall breaches", [M_("breach", i) for i in range(3)], "per damaged wall cell"),
        row("Floor holes", [M_("hole", i) for i in range(3)], "per damaged floor cell"),
        row("Rubble", [M_("rubble", i) for i in range(3)], "per wall or floor cell"),
        row("Lamps lit", [M_("lit", i) for i in range(3)]),
        row("Dead machines", [M_("dead", i) for i in range(3)], "a dead machine is a bare hull"),
        row("Pump room flooded", ["Yes" if p["flooded"] else "No" for p in P]),
        row("Chest richness", [f"{p['richness']} of 2" for p in P], "more picks, heavier top-metal lines"),
        row("Guard posts at the hall", [_meter(p["spawn"][0], LEVEL_COLOR[i]) for i, p in enumerate(P)], "chance per post, EV planet"),
        row("Guard posts under 30 blocks out", [_meter(p["spawn"][1], LEVEL_COLOR[i]) for i, p in enumerate(P)]),
        row("Guard posts 30 to 59 blocks out", [_meter(p["spawn"][2], LEVEL_COLOR[i]) for i, p in enumerate(P)]),
        row("Guard posts 60 blocks and more out", [_meter(p["spawn"][3], LEVEL_COLOR[i]) for i, p in enumerate(P)], "the far ends of the wings"),
        row("Silo walkways, Launch Control", shaft, "at least this chance"),
        row("Schematic chip in a chest", [pct(p["chip"]) for p in P], "Safe, Comms or Control chest"),
        row("The silo's missing part in a chest", [pct(p["part"]) for p in P], "one part per silo"),
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
        rows.append(f"""<tr><td style="white-space:nowrap">{rocket}</td><td style="color:var(--txt)">{planets}</td>
<td class="n">{chip(vname(c))}</td><td style="white-space:nowrap"><b>{metal}</b></td><td style="white-space:nowrap">{machine_range(c).replace(" to ", "&ndash;")}</td><td>{guns}</td><td style="white-space:nowrap">{body}</td>
<td class="n">{danger(c)}</td><td class="n">&times; {guard_scale(c):.2f}</td></tr>""")
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
    main = CM.plan_with(d, 1)
    px = 4
    mp = CM.raster(d, main, px)
    ov = CM.overlay(d, main, px, fs=11.5)
    crops = "".join(f'<div style="text-align:center">{img(_crop(d, CM.plan_with(d, n), 4), w=212, cls="px", style="border-radius:8px;border:1px solid var(--line)")}'
                    f'<div class="cap" style="margin-top:3px"><b style="color:var(--txt)">{n} silo{"s" if n > 1 else ""}</b> &middot; {SILO_ODDS[n - 1]}%</div></div>'
                    for n in range(1, 5))
    key_rows = "".join(f'<div class="lk"><i style="background:rgb{rgb}"></i>{esc(name)}</div>' for name, rgb, _ in CM.ROLES)
    n_rooms_lo = 4 * 2 * SLOTS_PER_SIDE * SLOTS_USED[0] // 100
    n_rooms_hi = 4 * 2 * SLOTS_PER_SIDE * SLOTS_USED[1] // 100

    stats = "".join(f'<div class="stat"><div class="v">{v}</div><div class="k">{k}</div></div>' for v, k in [
        (f"{SIZE} &times; {SIZE}", "blocks across, one flat level"),
        (f"{DEPTH}", "blocks under the ground"),
        ("1 to 4", "real silos, each missing one part"),
        (f"{n_rooms_lo} to {n_rooms_hi}", f"rooms, plus the hall, four guard posts and an airlock"),
    ])
    hero = hero_room()
    shafts = "".join(f'<div style="text-align:center">{img(shaft(v), h=190, style="margin:0 auto")}<div class="cap" style="margin-top:2px"><b style="color:var(--txt)">{n}</b></div></div>'
                     for v, n in [("open", "Open shaft"), ("half", "Half open"), ("closed", "Closed, two air gaps")])
    radar_a, radar_b = radar_station(False), radar_station(True)

    # ---- rooms
    rooms = "".join(room_card(k) for k in _ROOM_ORDER)

    # ---- guards
    soldier = M._enemy_pic("Army Soldier", dict(model="biped", skin="army_soldier", pose=K.AIM, gun_name="m4", armor=None))
    commando = M._enemy_pic("Commando", dict(model="biped", skin="army_soldier", pose=K.AIM, gun_name="m4", armor=None))
    storm = M._enemy_pic("Stormtrooper", dict(model="biped", skin="army_soldier", pose=K.AIM, gun_name="blaster_rifle", armor=None))
    guard_post, elite_post = post_block("concrete_hole"), post_block("metalpanel_hazard")

    # ---- machines
    hulls = "".join(f'<div style="text-align:center">{img(hull_block(t), h=64)}<div class="cap" style="margin-top:0">{t}</div></div>' for t in ("HV", "EV", "IV"))

    # ---- take over
    front = [silo_front("controller"), silo_front("hatch"), silo_front(None)]

    # ---- data stick, ceilings
    levels = ""

    body = f"""
<div style="display:grid;grid-template-columns:690px 1fr;gap:22px">
<div class="card"><div class="lead">Out in the world there are <b>complexes</b>: old missile bases, dug in and forgotten. From the surface you see only a few
concrete shafts, sometimes a small radar hut, and a trapdoor. Under them is a whole base, with <b>real missile silos</b> that need one part to work again.</div>
<div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:16px">{stats}</div>
<div class="pt" style="margin-top:14px">A <b>central hall</b> ({HALL[0]} &times; {HALL[1]}) has <b>four wings</b>, each a {WING_WIDTH}-wide rail corridor {WING_LEN} blocks long, with
rooms on both sides and a guard post at its mouth. The silos stand in a cluster to the north-east, joined to the hall by their own corridor. Complexes are rare:
a quarter of the grid squares ({CHANCE_PCT}%), one square per 1,024 &times; 1,024 blocks, and never on water or a steep slope. On the Overworld they keep out of hills and mountains. They generate in the Overworld
and on every planet with solid ground.</div></div>
<div class="card" style="display:flex;flex-direction:column"><div class="spot" style="flex:1;display:flex;align-items:center;justify-content:center;padding:6px">{img(hero, h=440, style="max-width:100%")}</div>
<div class="cap">A generator hall in a ruined complex. The walls and floor are the <b style="color:var(--txt)">Silo Complex Wall</b>, which joins into one plated mass; the machines are GregTech
hulls; a Silo Guard Post waits in the corner. A breach in the wall and a hole in the floor are the marks of a complex that lost its crew long ago.</div></div></div>

<div class="sec"><h2>On the surface</h2>
<div class="card" style="display:grid;grid-template-columns:1.55fr 1fr;gap:26px">
<div><div class="pt" style="margin-bottom:6px">Every silo reaches the surface as a <b>shaft</b>: a ring of light concrete around a 3 &times; 3 bore, flush with the ground. Some are open, some half capped and some sealed
with two air gaps left to climb.</div>
<div style="display:flex;justify-content:space-around;align-items:flex-end;gap:10px">{shafts}</div></div>
<div class="pt"><b>Radar stations.</b> Up to two stand in the south-west, apart from each other: a small hut of works casing with a radar on its roof. A station is inert until you add an Energy Hatch (see the end of this sheet).
<div style="margin-top:12px"><b>Escape ladder.</b> The base has no door but one: a 1 &times; 1 shaft at the end of a random wing, with a closed trapdoor level with the ground.</div>
<div style="margin-top:12px"><b>Nothing else</b> marks the place. A player walking past sees a few concrete squares in the grass.</div></div></div></div>

<div class="sec"><h2>The layout</h2>
<div style="display:grid;grid-template-columns:960px 1fr;gap:22px;align-items:start">
<div class="mapbox"><img src="{K.url(mp)}" width="{SIZE * px}" height="{SIZE * px}">{ov}</div>
<div class="card"><div class="label" style="margin-bottom:8px">How to read the map</div>
<div class="pt" style="margin-bottom:10px">Looking down from above, north up. This is one real complex with a single silo, drawn from the same plan the game builds.</div>
{key_rows}
<div class="lk"><i class="d" style="background:#ff4d4d"></i>Possible guard post</div>
<div class="lk"><i style="background:#ffd23f"></i>Loot chest</div>
<div class="lk"><i class="c" style="background:#2fd0ff"></i>Machine</div>
<div class="lk"><i style="background:#ff8a1f;border-color:#ff8a1f"></i>Silo outline</div>
<div class="lk"><i style="background:#22d36b"></i>Escape ladder</div>
<div class="lk"><i class="c" style="background:rgba(47,208,255,.3);border:2px dashed #2fd0ff"></i>Radar station, on the surface</div>
<div class="label" style="margin:12px 0 6px">The silo cluster</div>
<div class="pt" style="margin-bottom:6px;font-size:16px">Always in the north-east, on a 2 &times; 2 grid.</div>
<div style="display:grid;grid-template-columns:1fr 1fr;gap:10px 10px">{crops}</div></div></div></div>

<div class="sec"><h2>The rooms</h2>
<div class="pt" style="margin-bottom:10px">A wing has {SLOTS_PER_SIDE} room slots on each side, {SLOT_PITCH} blocks apart; between {SLOTS_USED[0]}% and {SLOTS_USED[1]}% of the {4 * 2 * SLOTS_PER_SIDE} slots are used. Six rooms are always present; the rest are drawn from the others, so a complex can hold two bunk rooms and no infirmary. The sign by every door names the room, and the hall's sign names the complex's <b>class</b>, its loot ceiling.</div>
<div class="rooms">{rooms}</div>
<div class="cap">Sizes are length &times; depth along the wing, in blocks. Chests hold what the activity level and the planet's ceiling allow (see below).</div></div>

<div class="sec"><h2>Three activity levels</h2>
<div class="pt" style="margin-bottom:10px">Each complex rolls its level once, and it never changes. The rooms are the same at every level; the level decides how much is broken, how much is lit and how many guards wait.</div>
<div class="card" style="padding:12px 18px 6px">{activity_table()}</div>
<div class="cap">The share of each level is a server setting. Guard chances rise with the planet, up to 1.5 times at the top, and guard posts at wing mouths get 25 points more when the complex is not abandoned.</div></div>

<div class="sec"><h2>Guards</h2>
<div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:16px">
<div class="card"><div class="post"><div style="text-align:center">{img(guard_post, h=100)}</div><div><h3>Silo Guard Post</h3>
<div class="pt">A spawner block for <b>Army Soldiers</b> and <b>Commandos</b>. It stands at the wing mouths and in the rooms.</div></div></div>
<div style="display:flex;justify-content:space-around;align-items:flex-end;margin-top:12px"><div class="mob"><div class="guide" style="padding:0 6px">{soldier}</div><b>Army Soldier</b></div><div class="mob"><div class="guide" style="padding:0 6px">{commando}</div><b>Commando</b></div></div></div>
<div class="card"><div class="post"><div style="text-align:center">{img(elite_post, h=100)}</div><div><h3>Silo Elite Post</h3>
<div class="pt">A spawner block for <b>Commandos</b> and <b>Stormtroopers</b>. It holds the silo walkways, the end of the trunk to the silos, and the deep rooms.</div></div></div>
<div style="display:flex;justify-content:space-around;align-items:flex-end;margin-top:12px"><div class="mob"><div class="guide" style="padding:0 6px">{commando}</div><b>Commando</b></div><div class="mob"><div class="guide" style="padding:0 6px">{storm}</div><b>Stormtrooper</b></div></div></div>
<div class="card"><h3>How the posts behave</h3>
<ul class="pt" style="margin:8px 0 0 18px;line-height:1.45">
<li>They wake only when a player is within <b>24 blocks</b>, and never load a chunk to do it.</li>
<li>At most <b>5</b> guards of a post's kind near it, <b>3</b> per try, one try every <b>5 seconds</b>.</li>
<li><b>Break a post and it stays broken</b>, for good, even after a restart. It drops nothing.</li>
<li>Guards are armed to match the planet: danger <b>3</b> up to IV, <b>4</b> at LuV and ZPM, <b>5</b> from UV up.</li>
<li>An abandoned complex has no posts at all.</li></ul></div></div></div>

<div class="sec"><h2>Loot by planet</h2>
<div class="pt" style="margin-bottom:10px">Nothing in a complex, chest or machine, goes above its planet's <b>ceiling</b>. The Overworld and the Moon sit at EV, and each rocket tier above the Moon lifts the ceiling by one voltage tier, to UMV for Tier 9. Chests fill with a ladder of GregTech metals, and the top rung at the ceiling is the most common. Guns come from the weapon tables and stop at ZPM, so above UHV the loot is GregTech material only.</div>
<div class="card" style="padding:10px 14px"><table class="t"><tr><th>Rocket</th><th>Planets</th><th style="text-align:center">Ceiling</th><th>Top metal</th><th>Machines</th><th>Guns to</th><th>Salvage</th><th style="text-align:center">Danger</th><th style="text-align:center">Guard rate</th></tr>
{planet_rows()}</table></div>
<div class="card" style="margin-top:14px;display:grid;grid-template-columns:1fr 620px;gap:22px;align-items:center">
<div><h3>Ross128b and Ross128ba</h3><div class="pt" style="margin-top:6px">The challenge-start planets have no fixed ceiling. <b>Each complex rolls its own</b>, from EV to UMV, weighted to the low end: about half are EV to IV, one in a hundred is UMV. The hall's sign tells you the class you drew.</div></div>
<div style="display:flex;align-items:flex-end;justify-content:space-between">{ross_card()}</div></div></div>

<div class="sec"><h2>Real machines, real salvage</h2>
<div class="card" style="display:grid;grid-template-columns:1fr 1fr;gap:26px">
<div><div class="pt">The machines in the rooms are <b>real GregTech machines</b>: generators, Battery Buffers, Assemblers and pumps at the ceiling's tier or one or two below it (never under HV), plus a
<b>Launch Control</b> and Galacticraft's <b>NASA Workbench</b>. Air handlers, fuel tanks and consoles are machine hulls. You can pick up what you find, though a dead machine, about a third to a half of them in a ruined complex, is only the bare hull.</div>
<div style="display:flex;justify-content:space-around;align-items:flex-end;margin-top:8px">{hulls}</div></div>
<div><div class="pt"><b>Nothing live is left behind.</b> The magazine holds stainless steel frame boxes and four plates, never a warhead. Chests hold a <b>damaged missile body</b> (too broken to stand; it recycles for its plates),
<b>rocket fuel cells</b> in the fuel depot, and now and then a <b>schematic chip</b>. The <b>Silo Complex Wall</b> mines to itself: a handsome plated block for your own base, though no machine takes it.</div>
<div style="display:flex;justify-content:center;margin-top:8px">{img(wall_block(), h=64)}</div></div></div></div>

<div class="sec"><h2>Take over a silo</h2>
<div class="card"><div class="pt" style="margin-bottom:10px">Every silo in a complex is a real <b>Missile Silo</b>, built from the same structure you would build yourself, with its bus and hatches in place. Each is missing <b>exactly one part</b>. Put it back and the silo forms.</div>
<div style="display:grid;grid-template-columns:repeat(3,1fr);gap:18px">
<div style="text-align:center"><div class="spot">{img(front[0], h=250)}</div><div class="cap"><b style="color:var(--txt)">No controller.</b> A concrete block stands in the middle of the front wall.</div></div>
<div style="text-align:center"><div class="spot">{img(front[1], h=250)}</div><div class="cap"><b style="color:var(--txt)">No Energy Hatch.</b> A clear glass block stands under where the controller is.</div></div>
<div style="text-align:center"><div class="spot">{img(front[2], h=250)}</div><div class="cap"><b style="color:var(--txt)">Repaired.</b> The controller (blue ring) and the hatches (orange rings) in their places.</div></div></div>
<div style="display:grid;grid-template-columns:repeat(4,1fr);gap:18px;margin-top:16px">
<div class="step"><span class="num">1</span><div class="t"><b>Clear the way.</b> An operational complex always has an elite post at each silo walkway, and a partly abandoned one often does. Break the posts, or fight what they spawn.</div></div>
<div class="step"><span class="num">2</span><div class="t"><b>Find the part.</b> Check the Safe, Stockpile, Workshop and Control chests: about {pct(PROFILES[0]["part"])} of abandoned, {pct(PROFILES[1]["part"])} of partly abandoned and {pct(PROFILES[2]["part"])} of operational silos have theirs lying in one. A missing part is not lost: the controller and a GregTech Energy Hatch are both craftable.</div></div>
<div class="step"><span class="num">3</span><div class="t"><b>Put it in.</b> Mine the concrete and set the controller facing out, or mine the glass and set the Energy Hatch in its place. The silo forms and switches itself on.</div></div>
<div class="step"><span class="num">4</span><div class="t"><b>Power and launch.</b> Run EU to the Energy Hatch. A found hatch is EV, which launches up to Tier 2; fit a LuV hatch for Tier 3. Then add a Launch Control, fuel and a missile as for any silo.</div></div></div></div></div>

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
        "Silo Complex Wall: Steel Bastion Plating art, with connected textures. Radar Casing, dish and radar controller overlay: derived from HBM's Nuclear Tech Mod, GNU LGPL v3; modified.",
        "Machine hulls, hatch overlays and Light Concrete: GregTech 5 Unofficial textures, LGPL-3.0. Hut casings, Silo Guard Posts and the Silo Complex Wall: GregGuns art, MIT.",
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
