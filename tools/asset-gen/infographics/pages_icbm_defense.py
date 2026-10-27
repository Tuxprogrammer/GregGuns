"""The icbm-detection-and-defense guide sheet: radars, the radar tower, the air-defense battery and its SAMs, the EMP
tower and the T3 EMP warhead, the handheld tools, and the redstone, cover and OpenComputers outputs.

Every number is read from the Java sources at build time (`_const`), so a changed constant moves the sheet instead of
leaving it stale (D50). Blocks are drawn from the GregTech hulls with the committed NTM overlays; the tower from the
cells of `RadarTowerShape`; the SAM and the T3 warhead from the shipped OBJ models.

Run on its own:  python tools/asset-gen/infographics/pages_icbm_defense.py
"""

from __future__ import annotations

import hashlib
import math
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import blocks as B  # noqa: E402
import gt  # noqa: E402
import kit as K  # noqa: E402
import render3d as R  # noqa: E402
import scenes as S  # noqa: E402
from kit import chip, esc  # noqa: E402
from pages import img, page_doc, slot  # noqa: E402

NTM = K.ASSETS / "models/thirdparty/ntm"
NTM_TEX = K.ASSETS / "textures/models/thirdparty/ntm"
NTM_ITEMS = K.ASSETS / "textures/items/thirdparty/ntm"
NTM_MACHINES = B.TEX / "machines/thirdparty/ntm"
NTM_CASINGS = B.TEX / "casings/thirdparty/ntm"
JAVA = K.REPO / "src/main/java/com/gregguns"
FRONT, BACK, LEFT, RIGHT = S.FRONT, S.BACK, S.LEFT, S.RIGHT


# ---------------------------------------------------------------- numbers, read from the code


def _src(rel: str) -> str:
    return (JAVA / rel).read_text(encoding="utf-8")


def _const(rel: str, name: str) -> float:
    """The numeric value of `static final <type> NAME = <number>;` in a Java file."""
    m = re.search(rf"\b{name}\s*=\s*(-?[0-9][0-9_]*(?:\.[0-9]+)?)[FfDdLl]?\s*;", _src(rel))
    if not m:
        raise SystemExit(f"{rel}: constant {name} not found; update pages_icbm_defense.py")
    v = float(m.group(1).replace("_", ""))
    return int(v) if v == int(v) and "." not in m.group(1) else v


def _cfg(name: str) -> float:
    m = re.search(rf"public static (?:int|double) {name}\s*=\s*([0-9.]+);", _src("core/config/IcbmConfig.java"))
    if not m:
        raise SystemExit(f"IcbmConfig.{name} not found")
    return float(m.group(1)) if "." in m.group(1) else int(m.group(1))


RAD = "icbm/defense/RadarRules.java"
SAM = "icbm/defense/SamRules.java"
EMP = "icbm/emp/EmpRules.java"
HAND = "icbm/tools/HandheldRules.java"

SCAN_TICKS = _const(RAD, "SCAN_TICKS")
DEFAULT_RADIUS = _const(RAD, "DEFAULT_RADIUS")
MIN_RADIUS = _const(RAD, "MIN_RADIUS")
MAX_REDSTONE = _const(RAD, "MAX_REDSTONE")
_RANGE_SRC = re.search(r"static int range\(int tier\)\s*\{(.*?)\n    \}", _src(RAD), re.S).group(1)
RANGES = [int(n) for n in re.findall(r"return (?:tier > 8 \? )?(\d+)", _RANGE_SRC)]   # HV, EV, IV, LuV, ZPM, UV, UHV
assert RANGES == [128, 256, 512, 768, 1024, 1280, 1536], RANGES            # the lang strings and the table below agree
TIER_V = {"HV": 512, "EV": 2048, "IV": 8192, "LuV": 32768, "ZPM": 131072, "UV": 524288, "UHV": 2097152}   # GT's V[]

SAM_SPEED = _const(SAM, "SPEED")
SAM_BURST = _const(SAM, "KILL_RADIUS")
SAM_LIFE = int(_const(SAM, "LIFETIME"))
SAM_DAMAGE = int(_const(SAM, "DAMAGE"))
SAM_SLOTS = int(_const(SAM, "CAPACITY"))
SAM_STACK = int(_const("icbm/defense/ItemSam.java", "MAX_STACK"))
SAM_INTERVAL = int(_const(SAM, "FIRE_INTERVAL"))
SAM_EU = int(_const(SAM, "EU_PER_SHOT"))
SAM_RANGE = int(_const(SAM, "ENGAGE_RANGE"))

_TIERS = re.findall(r'T(\d)\("missile_body_t\d", ExplosiveTier\.T\d, (\d+), \d+, (\d+), ([0-9.]+), [0-9.]+, \d+, ([0-9.]+)F',
                    _src("icbm/missile/MissileTier.java"))
MISSILE = {int(t): dict(speed=float(s), hp=int(float(h))) for t, _f, _r, s, h in _TIERS}   # speed in blocks per tick

EMP_MIN = int(_const(EMP, "MIN_RADIUS"))
EMP_COOL = int(_const(EMP, "COOLDOWN_TICKS"))
EMP_EU = _const(EMP, "EU_PER_AREA")
EMP_MAX_R = int(_cfg("empMaxRadius"))
EMP_DEVICES = int(_cfg("empMaxDevices"))
P_KILL, P_FRY, P_TRIG = _cfg("empMissileKillChance"), _cfg("empMissileGuidanceChance"), _cfg("empMissileTriggerChance")
EMP_WAR_R = 48       # WarheadBlast.EMP radius at full fill (the dump's emp curve ends at 48.0 at fill 64)
RANGE_RD, RANGE_RE, RANGE_RI = int(_const(HAND, "RANGE_HV")), int(_const(HAND, "RANGE_EV")), int(_const(HAND, "RANGE_IV"))
DEFUSER_USES = int(_const(HAND, "DEFUSER_USES"))
GUN_RANGE = int(_const("icbm/defense/ItemRadarGun.java", "RANGE"))

assert MISSILE[1]["hp"] == 20 and MISSILE[2]["hp"] == 40 and MISSILE[3]["hp"] == 60, MISSILE


def emp_cost(r: float) -> int:
    return math.ceil(r * r * EMP_EU)


def sams_needed(hp: int) -> int:
    return math.ceil(hp / SAM_DAMAGE)


def n(v: float) -> str:
    return f"{int(v):,}" if float(v).is_integer() else f"{v:g}"


def tier_chip(t: str) -> str:
    return chip(t) if t in K.TIERS else f'<span class="chip" style="background:#8a8f98">{t}</span>'


# ---------------------------------------------------------------- pictures


def machine_block(folder: str, tier: str, ppu=150) -> K.Path:
    """A single block: the GT tier hull with the NTM overlays of folder, the way the machine draws."""
    faces = {}
    for face in ("FRONT", "SIDE", "TOP"):
        hull = gt.hull(tier, "TOP" if face == "TOP" else "SIDE")
        faces[face] = B.composite(hull, NTM_MACHINES / folder / f"OVERLAY_{face}.png", cache=K.TEXCACHE, name=f"d4-{folder}-{tier}-{face}")
    return K.voxels(f"d4-{folder}-{tier}", {(0, 0, 0): ("m", FRONT)}, {"m": (faces["TOP"], faces["SIDE"], faces["FRONT"])},
                    yaw=-38, pitch=28, ppu=ppu)


def tower_cells() -> dict:
    """{(x, y, z): char} from RadarTowerShape.PIECES (aisles front to back, rows top to bottom)."""
    src = _src("icbm/defense/RadarTowerShape.java")
    body = re.search(r"String\[\]\[\]\[\]\s+PIECES\s*=\s*\{(.*?)\};", src, re.S).group(1)
    aisles = [re.findall(r'"([^"]*)"', g) for g in re.findall(r"\{([^{}]*)\}", body)]
    assert len(aisles) == 3 and all(len(a) == 7 for a in aisles), "RadarTowerShape layout changed"
    return B.stack_aisles([aisles]), aisles


def tower_build() -> S.Build:
    cells, _ = tower_cells()
    casing = NTM_CASINGS / "radar_casing.png"
    gc_beam = S._casing("gantry_beam")
    ctrl = B.composite(casing, NTM_MACHINES / "radar_tower/OVERLAY_FRONT.png", cache=K.TEXCACHE, name="d4-tower-ctrl")
    b = S.Build("d4-tower")
    b.define("casing", "Radar Casing", casing)
    b.define("beam", "Gantry Beam", gc_beam)
    b.define("ctrl", "Radar Tower", (casing, casing, ctrl))
    b.put(cells, {"C": "casing", "H": "casing", "B": "beam", "~": ("ctrl", FRONT)})
    b.hatch((0, 0, 1), "energy_hatch", LEFT, casing)
    b.hatch((2, 0, 1), "maintenance_hatch", RIGHT, casing)
    return b


def render_obj(obj: str, tex: str, name: str, yaw=35, pitch=14, ppu=110) -> K.Path:
    o, t = NTM / f"{obj}.obj", NTM_TEX / f"{tex}.png"
    sig = hashlib.sha1(o.read_bytes() + t.read_bytes()).hexdigest()[:10]

    def make():
        return R.trim(R.render(R.obj_scene(o, t), yaw=yaw, pitch=pitch, ppu=ppu * 2, ss=2, outline=K.RIM), 2)
    return K._cached(f"d4-{name}", dict(sig=sig, y=yaw, p=pitch, u=ppu, v=1), make)


def item_icon(name: str, px=64, title="") -> str:
    return slot(K.icon(NTM_ITEMS / f"{name}.png", 64), px, title or name)


# ---------------------------------------------------------------- panels


CSS = """
.sec { display: grid; gap: 18px; margin-top: 18px; }
.sec .card h2 { margin-bottom: 6px; }
.kv { display: grid; grid-template-columns: auto 1fr; gap: 6px 14px; font-size: 18px; align-items: baseline; }
.kv .k { color: var(--muted); } .kv .v { color: var(--txt); font-weight: 600; }
.pt { font-size: 18px; color: var(--muted); line-height: 1.34; } .pt b { color: var(--txt); }
.step { display: flex; gap: 14px; align-items: flex-start; }
.step .t { font-size: 18px; color: var(--muted); line-height: 1.32; } .step .t b { color: var(--txt); }
.bar { display: grid; grid-template-columns: 70px 1fr 120px; gap: 12px; align-items: center; margin: 7px 0; font-size: 18px; }
.bar .tr { height: 22px; background: rgba(255,255,255,.07); border-radius: 5px; overflow: hidden; }
.bar .fl { height: 100%; border-radius: 5px; }
.bar .n { text-align: right; font-weight: 700; color: var(--txt); }
.stk { display: flex; height: 40px; border-radius: 8px; overflow: hidden; border: 1px solid var(--line); }
.stk div { display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 18px; color: #10151b; }
.lay { display: flex; flex-direction: column; align-items: center; gap: 8px; }
.lay .g { display: grid; grid-template-columns: repeat(3, 44px); grid-auto-rows: 36px; gap: 3px; }
.lay .c { border-radius: 4px; display: flex; align-items: center; justify-content: center; border: 1px solid rgba(255,255,255,.14); }
.lay .c b { font-size: 11px; color: #10151b; font-weight: 700; }
.lay .lb { font-size: 16px; color: var(--muted); text-align: center; } .lay .lb b { color: var(--txt); display: block; font-size: 18px; }
table.t { width: 100%; border-collapse: collapse; font-size: 17px; }
table.t th { text-align: left; font-family: Bahnschrift, "Fira Sans Condensed", sans-serif; letter-spacing: .1em; text-transform: uppercase;
  font-size: 14px; color: var(--muted); padding: 6px 10px; border-bottom: 1px solid var(--line); }
table.t td { padding: 9px 10px; border-bottom: 1px solid var(--line); color: var(--muted); vertical-align: top; line-height: 1.3; }
table.t td:first-child { color: var(--txt); font-weight: 600; white-space: nowrap; }
table.t tr:last-child td { border-bottom: 0; }
table.t td b { color: var(--txt); }
.mini { display: flex; flex-direction: column; align-items: center; gap: 6px; }
.big { font-family: Bahnschrift, "Fira Sans Condensed", sans-serif; font-stretch: condensed; font-size: 34px; font-weight: 700; color: var(--orange); line-height: 1.05; }
.hp { display: flex; align-items: center; gap: 12px; margin: 6px 0; font-size: 18px; color: var(--muted); }
.hp .pill { flex: none; min-width: 150px; } .hp .pill b { color: var(--txt); }
"""


def pip(color: str) -> str:
    return f'<b style="color:{color}">&#9632;</b>'


def threat_diagram() -> str:
    """Top-down view: the radar's range ring, its protected radius, one missile that lands inside it, one that does not."""
    return f"""<svg viewBox="0 0 520 380" width="100%" style="display:block;max-height:380px" role="img" aria-label="A missile counts as incoming when it lands inside the protected radius">
<defs><marker id="ah" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="7" markerHeight="7" orient="auto"><path d="M0 0L10 5L0 10z" fill="#ff7b7b"/></marker>
<marker id="ag" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="7" markerHeight="7" orient="auto"><path d="M0 0L10 5L0 10z" fill="#c9d1da"/></marker></defs>
<circle cx="260" cy="190" r="178" fill="rgba(56,198,217,.07)" stroke="#38c6d9" stroke-width="2.5" stroke-dasharray="9 7"/>
<circle cx="260" cy="190" r="82" fill="rgba(255,138,31,.16)" stroke="#ff8a1f" stroke-width="3"/>
<rect x="249" y="179" width="22" height="22" rx="3" fill="#e9edf2" stroke="#10151b" stroke-width="2"/>
<path d="M176 56 Q218 118 241 165" fill="none" stroke="#ff7b7b" stroke-width="4" marker-end="url(#ah)"/>
<path d="M404 70 Q386 190 356 318" fill="none" stroke="#c9d1da" stroke-width="4" stroke-dasharray="3 7" marker-end="url(#ag)"/>
<path d="M231 161l14 14M245 161l-14 14" stroke="#ff7b7b" stroke-width="5" stroke-linecap="round"/>
<path d="M349 311l14 14M363 311l-14 14" stroke="#c9d1da" stroke-width="5" stroke-linecap="round"/>
<text x="8" y="26" fill="#ff9a9a" font-size="19" font-weight="700">INCOMING</text><text x="8" y="46" fill="#b4bfcb" font-size="15">lands inside</text><text x="8" y="64" fill="#b4bfcb" font-size="15">the protected</text><text x="8" y="82" fill="#b4bfcb" font-size="15">radius</text>
<text x="392" y="318" fill="#e9edf2" font-size="19" font-weight="700">DETECTED</text><text x="392" y="338" fill="#b4bfcb" font-size="15">lands outside,</text><text x="392" y="356" fill="#b4bfcb" font-size="15">tracked only</text>
<text x="260" y="228" fill="#ffb066" font-size="15" font-weight="700" text-anchor="middle">protected radius</text>
<text x="260" y="40" fill="#6fdbe9" font-size="16" font-weight="700" text-anchor="middle">range</text>
</svg>"""


def radar_card(title: str, tier: str, pic: K.Path, rng: int, extra: str) -> str:
    amp = TIER_V[tier] // 4
    return (f'<div class="card"><div style="display:flex;align-items:center;gap:12px"><h2>{esc(title)}</h2>{chip(tier)}</div>'
            f'<div class="spot" style="display:flex;justify-content:center;padding:6px 0;margin:6px 0">{img(pic, h=150)}</div>'
            f'<div class="kv"><span class="k">Range</span><span class="v">{n(rng)} blocks</span>'
            f'<span class="k">Power</span><span class="v">{n(amp)} EU/t while it scans</span>'
            f'<span class="k">Scan</span><span class="v">every {n(SCAN_TICKS)} ticks</span>{extra}</div></div>')


def range_bars() -> str:
    names = ["HV", "EV", "IV", "LuV", "ZPM", "UV", "UHV"]
    who = {"HV": "Radar (HV)", "EV": "Radar (EV)"}
    rows = []
    for t, r in zip(names, RANGES):
        col = "#c9ced6" if t == "UV" or t == "UHV" else f"var(--{t.lower()})"
        label = who.get(t, "Radar Tower")
        rows.append(f'<div class="bar"><span>{tier_chip(t)}</span><div class="tr"><div class="fl" style="width:{r / RANGES[-1] * 100:.1f}%;background:{col}"></div></div>'
                    f'<span class="n">{n(r)}</span></div><div class="muted" style="font-size:15px;margin:-4px 0 4px 82px">{label}</div>')
    return "".join(rows)


def aisle_grid(rows: list[str], label: str) -> str:
    colors = {"C": "#7d8da0", "H": "#7d8da0", "B": "#d99a2b", "~": "#38c6d9", " ": "transparent"}
    out = []
    for row in rows:
        for ch in row:
            style = f"background:{colors[ch]}" + (";border:2px solid #ff8a1f" if ch == "H" else ";border:0" if ch == " " else "")
            txt = {"~": "Ctrl", "B": "Beam"}.get(ch, "")
            out.append(f'<div class="c" style="{style}"><b>{txt}</b></div>')
    return f'<div class="lay"><div class="g">{"".join(out)}</div><div class="lb">{label}</div></div>'


def sheet() -> str:
    hv = machine_block("radar_hv", "HV")
    ev = machine_block("radar_ev", "EV")
    bat = machine_block("air_defense_battery", "HV", ppu=170)
    emp = machine_block("emp_tower", "EV", ppu=170)
    tb = tower_build()
    tower_img = tb.render(yaw=-32, pitch=22, ppu=52, tag="", marks=True)
    cells, aisles = tower_cells()
    n_c, n_h = sum(r.count("C") for a in aisles for r in a), sum(r.count("H") for a in aisles for r in a)
    n_b = sum(r.count("B") for a in aisles for r in a)
    min_casing = int(_const("icbm/defense/RadarTowerShape.java", "CASING_MINIMUM"))
    sam_img = render_obj("sam_missile", "sam_missile", "sam", yaw=40, pitch=18, ppu=64)
    war_img = render_obj("warhead_t3", "warhead_t3", "emp-warhead", yaw=35, pitch=14, ppu=100)

    # ---- the three radars
    tower_extra = ""
    radars = (radar_card("Radar", "HV", hv, RANGES[0], f'<span class="k">Protected</span><span class="v">{n(DEFAULT_RADIUS)} blocks, set in its screen</span>')
              + radar_card("Radar", "EV", ev, RANGES[1], f'<span class="k">Protected</span><span class="v">{n(DEFAULT_RADIUS)} blocks, set in its screen</span>')
              + f"""<div class="card"><h2>What it tells you</h2><div class="pt" style="margin-top:6px">A missile is a <b>contact</b> from the moment it climbs until it lands. The radar sorts every contact in range, nearest first, and flags as <b>incoming</b> only those whose <b>predicted impact point</b> is inside your protected radius, edge included. A missile that passes right over the radar but lands elsewhere is no threat; one that flies past and lands inside is.</div>
<div class="label" style="margin:14px 0 4px">Its screen shows</div><div class="pt">Offline, scanning, tracking or <b>threat</b>; contacts in range and incoming; the nearest contact; the protected radius field; Save link to stick.</div></div>""")

    link_steps = f"""<div class="step"><span class="num">1</span><div class="t"><b>Power the radar and set its protected radius.</b> Open its screen and type the radius: {n(MIN_RADIUS)} blocks up to its range, {n(DEFAULT_RADIUS)} to begin with.</div></div>
<div class="step"><span class="num">2</span><div class="t"><b>Save its link on a Data Stick.</b> Right-click the radar (or the tower&#39;s controller) with the stick, or press Save link in a radar&#39;s screen. The stick keeps the radar&#39;s dimension and position.</div></div>
<div class="step"><span class="num">3</span><div class="t"><b>Put the stick in the battery&#39;s stick slot.</b> The battery now listens to that radar. The radar must be powered, loaded and in the same dimension; a link never loads a chunk.</div></div>"""

    # ---- the radar tower
    layers = (aisle_grid(aisles[0], "<b>Front</b>controller in the second row from the bottom")
              + aisle_grid(aisles[1], "<b>Middle</b>the mast stands in the center")
              + aisle_grid(aisles[2], "<b>Back</b>"))
    tower_rows = "".join(
        f'<tr><td>{tier_chip(t)}</td><td>{n(r)} blocks</td><td>{n(TIER_V[t] // 4)} EU/t</td></tr>'
        for t, r in zip(["IV", "LuV", "ZPM", "UV", "UHV"], RANGES[2:]))
    tower_card = f"""<div style="display:grid;grid-template-columns:1fr 520px;gap:18px;margin-top:18px">
<div class="card" style="display:flex;flex-direction:column"><div style="display:flex;align-items:center;gap:14px;flex-wrap:wrap"><h2>Radar Tower</h2>{chip("IV")}
<span class="muted" style="font-size:18px">3 &times; 3 &times; 7 blocks, a mast on top</span>
<span style="font-size:16px;margin-left:auto" class="muted">{pip("#38c6d9")} controller &nbsp; {pip("#ff8a1f")} hatches</span></div>
<div class="spot" style="flex:1;display:flex;align-items:center;justify-content:center;padding:10px 0;margin-top:8px">{img(tower_img, h=520, style="max-width:100%")}</div>
<div class="muted" style="font-size:16px;margin-top:6px">One tower reaches farther than any single-block radar, and the better its Energy Hatch, the farther it sees. It reads the same missiles and gives the same outputs as the single blocks.</div></div>
<div class="card"><div class="label" style="margin-bottom:6px">Build it</div>
<div class="pt"><b>{n_c + n_h} Radar Casing</b> (at least {min_casing}; hatches may replace the rest) &middot; the walls. Make four at a time in the Assembler at IV from 6 tungsten steel plates.</div>
<div class="pt" style="margin-top:8px"><b>{n_b} Gantry Beam</b> &middot; the mast up the middle, standing two blocks above the walls.</div>
<div class="pt" style="margin-top:8px"><b>1 Radar Tower controller</b> (IV) &middot; the front wall, second row up from the bottom.</div>
<div class="pt" style="margin-top:8px"><b>Hatches</b> go in the {n_h} outlined casing blocks of the bottom ring: at least one <b>Energy Hatch</b> and one <b>Maintenance Hatch</b>. Below an IV Energy Hatch the tower stays off and says why.</div>
<div class="label" style="margin:14px 0 4px">The Energy Hatch sets the range</div>
<table class="t"><tr><th>Hatch</th><th>Range</th><th>Power</th></tr>{tower_rows}</table>
<div class="muted" style="font-size:15px;margin-top:6px">The weakest hatch counts. Power is a quarter amp.</div></div></div>
<div class="card" style="margin-top:14px"><div class="label" style="margin-bottom:8px">Layout, looking from the front: columns left to right, top row first</div>
<div style="display:flex;justify-content:space-around;gap:10px;padding:6px 0">{layers}</div>
<div class="muted" style="font-size:16px;margin-top:6px">Gray is Radar Casing, orange-outlined casing may be replaced by a hatch, gold is the Gantry Beam mast. The tower&#39;s settings (protected radius, range, contacts) open from the R button on its controller screen.</div></div>"""

    # ---- battery and SAM
    hp_rows = "".join(
        f'<div class="hp"><span class="pill">{lab} <b>{MISSILE[t]["hp"]} hp</b></span>'
        f'{"".join(f"<span class=chip style=background:#ff8a1f>SAM {i + 1}</span>" for i in range(sams_needed(MISSILE[t]["hp"])))}'
        f'<span>{"one SAM" if sams_needed(MISSILE[t]["hp"]) == 1 else "two SAMs"}</span></div>'
        for t, lab in ((1, "Tier 1 missile"), (2, "Tier 2 missile"), (3, "Tier 3 missile")))
    battery = f"""<div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:18px;margin-top:18px">
<div class="card"><div style="display:flex;align-items:center;gap:12px"><h2>Air-Defense Battery</h2>{chip("HV")}</div>
<div style="display:flex;justify-content:space-around;align-items:center;gap:6px;margin:6px 0" class="spot">{img(bat, h=170)}{img(sam_img, h=190)}</div>
<div class="kv"><span class="k">Holds</span><span class="v">{SAM_SLOTS} stacks of {SAM_STACK} SAMs, plus one Data Stick</span>
<span class="k">Fires</span><span class="v">1 SAM every {n(SAM_INTERVAL / 20)} s at most</span>
<span class="k">Cost</span><span class="v">{n(SAM_EU)} EU a SAM, paid as it leaves</span>
<span class="k">Reach</span><span class="v">{n(SAM_RANGE)} blocks from the battery</span></div></div>
<div class="card"><h2>What a SAM does</h2>
<div class="kv" style="margin-top:6px"><span class="k">Speed</span><span class="v">{n(SAM_SPEED)} blocks per tick (a Tier 3 missile flies {n(MISSILE[3]["speed"])})</span>
<span class="k">Bursts</span><span class="v">within {n(SAM_BURST)} blocks of the missile</span>
<span class="k">Damage</span><span class="v">{n(SAM_DAMAGE)} to the missile</span>
<span class="k">Lives</span><span class="v">{n(SAM_LIFE // 20)} seconds, then it is gone</span></div>
<div class="label" style="margin:14px 0 4px">How many it takes</div>{hp_rows}
<div class="muted" style="font-size:16px;margin-top:6px">A shot-down missile&#39;s warhead goes off where it is hit, so intercept far from what you protect. The battery counts SAMs already in the air and never wastes one on a missile that is already doomed.</div></div>
<div class="card"><h2>Set it up</h2><div style="display:flex;flex-direction:column;gap:12px;margin-top:8px">{link_steps}
<div class="step"><span class="num">4</span><div class="t"><b>Load SAMs and power it.</b> The battery fires by itself at the nearest threat the linked radar reports. It never fires at a missile aimed outside the radar&#39;s protected radius. It works on its own, with no silo.</div></div></div></div></div>"""

    # ---- EMP
    cost_rows = "".join(
        f'<div class="bar"><span class="n" style="text-align:left">{r}</span><div class="tr"><div class="fl" style="width:{emp_cost(r) / emp_cost(EMP_MAX_R) * 100:.1f}%;background:var(--ev)"></div></div>'
        f'<span class="n">{n(emp_cost(r))} EU</span></div>' for r in (10, 20, 40, 60, EMP_MAX_R))
    p_fry = (1 - P_KILL) * P_FRY
    p_trig = (1 - P_KILL) * (1 - P_FRY) * P_TRIG
    p_safe = (1 - P_KILL) * (1 - P_FRY) * (1 - P_TRIG)
    stack = "".join(f'<div style="flex:{max(p, 0.045):.4f};background:{c}">{lab}</div>'
                    for p, c, lab in ((P_KILL, "#ff7b7b", f"{P_KILL * 100:.0f}%"), (p_fry, "#ffb066", f"{p_fry * 100:.0f}%"),
                                      (p_trig, "#ffd23f", ""), (p_safe, "#9fe0a8", f"{p_safe * 100:.0f}%")))
    emp_block = f"""<div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:18px;margin-top:18px">
<div class="card"><div style="display:flex;align-items:center;gap:12px"><h2>EMP Tower</h2>{chip("EV")}</div>
<div class="spot" style="display:flex;justify-content:center;margin:6px 0">{img(emp, h=170)}</div>
<div class="pt">A single block. Set a radius from {EMP_MIN} to {EMP_MAX_R} blocks, press Fire, and one pulse goes out. It holds the charge for its biggest pulse, and an idle tower costs nothing. It waits {n(EMP_COOL / 20)} seconds between pulses.</div>
<div class="label" style="margin:14px 0 4px">Cost: {n(EMP_EU)} EU per square block of radius</div>{cost_rows}
<div class="muted" style="font-size:15px;margin-top:4px">It stores enough for the largest radius ({n(emp_cost(EMP_MAX_R))} EU) and takes the cost after the pulse. A refused pulse costs nothing.</div></div>
<div class="card"><h2>Against machines</h2><div class="pt" style="margin-top:6px">Hits the <b>{EMP_DEVICES} nearest devices</b> in range, nearest first, so a big base never stalls the server. The tower itself is spared; loaded chunks only.</div>
<div style="display:flex;flex-direction:column;gap:10px;margin-top:12px">
<div class="step"><span class="num">&bull;</span><div class="t"><b>Drains stored EU</b> to zero in each GregTech machine, IC2 energy store, Battery Buffer (and the batteries inside it) and Lapotronic Supercapacitor. Cables hold none, and the batteries in your pockets are safe.</div></div>
<div class="step"><span class="num">&bull;</span><div class="t"><b>One maintenance issue</b> on every multiblock it hits: a wrench, screwdriver, soft mallet, hard hammer, soldering iron or crowbar problem to fix by hand.</div></div></div>
<div class="label" style="margin:14px 0 4px">Modes (the Mode button)</div><div class="pt">Machines and missiles, missiles only, or machines only.</div></div>
<div class="card"><h2>Against missiles</h2><div class="pt" style="margin-top:6px">Every missile in flight inside the radius rolls once. Missiles still on a pad or counting down are out of reach.</div>
<div class="stk" style="margin:12px 0 8px">{stack}</div>
<div class="hp"><span>{pip("#ff7b7b")}</span><span><b style="color:var(--txt)">{P_KILL * 100:.0f}% destroyed</b> in the air, the warhead does not go off</span></div>
<div class="hp"><span>{pip("#ffb066")}</span><span><b style="color:var(--txt)">{p_fry * 100:.0f}% guidance fried:</b> no thrust, it falls and blows up where it lands</span></div>
<div class="hp"><span>{pip("#ffd23f")}</span><span><b style="color:var(--txt)">{p_trig * 100:.0f}% warhead triggered</b> where it flies</span></div>
<div class="hp"><span>{pip("#9fe0a8")}</span><span><b style="color:var(--txt)">{p_safe * 100:.0f}% unaffected</b></span></div>
<div class="muted" style="font-size:16px;margin-top:6px">The three chances can be changed in the server config.</div>
<div class="label" style="margin:14px 0 4px">Fire it from</div><div class="pt">The Fire button, a redstone pulse, a machine controller cover (switching the tower off and on again), or an OpenComputers command. One pulse per press: a held signal does not re-fire.</div></div></div>
<div class="card" style="margin-top:18px;display:grid;grid-template-columns:240px 1fr;gap:22px;align-items:center">
<div class="spot" style="display:flex;justify-content:center;padding:8px">{img(war_img, h=210)}</div>
<div><div style="display:flex;align-items:center;gap:12px"><h2>The T3 EMP warhead</h2>{chip("LuV")}</div>
<div class="pt" style="margin-top:6px">The tower&#39;s pulse on a missile. A Tier 3 casing (LuV, made in the Assembler) that, when it goes off, sends one EMP pulse in <b>machines and missiles</b> mode, centered on the blast. A full warhead reaches <b>{EMP_WAR_R} blocks</b>, less with a smaller fill. It breaks no blocks and hurts nobody, but every device in reach drains exactly as it does for the tower, within the same {EMP_DEVICES}-device cap, and missiles in the air roll the same table. It costs no EU and, like every Tier 3 warhead, flies from the silo on a Tier 3 missile.</div></div></div>"""

    # ---- handheld
    hand = f"""<div style="display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:18px;margin-top:18px">
<div class="card"><div style="display:flex;align-items:center;gap:12px">{item_icon("radar_gun", 56, "Radar Gun")}<h2>Radar Gun</h2>{chip("HV")}</div>
<div class="pt" style="margin-top:8px">Right-click to aim at a spot up to <b>{GUN_RANGE} blocks</b> away. It stores that spot&#39;s X and Z, the same target a Data Stick carries. Right-click a <b>Launch Control</b> to copy the target there. Sneak and right-click to clear it.</div></div>
<div class="card"><div style="display:flex;align-items:center;gap:12px">{item_icon("remote_detonator", 56, "Remote Detonator")}<h2>Remote Detonator</h2>{chip("HV")}{chip("EV")}{chip("IV")}</div>
<div class="pt" style="margin-top:8px"><b>Sneak-click a placed warhead</b> to link it; sneak-click the air to clear. A plain right-click <b>lights its fuse</b> from a distance, and the warhead counts down and beeps as if lit by hand. Reach: <b>{n(RANGE_RD)}</b> (HV), <b>{n(RANGE_RE)}</b> (EV), <b>{n(RANGE_RI)}</b> (IV) blocks, same dimension, loaded chunk only. Only the warhead&#39;s owner can link or fire it.</div></div>
<div class="card"><div style="display:flex;align-items:center;gap:12px">{item_icon("defuser", 56, "Defuser")}<h2>Defuser</h2>{chip("HV")}</div>
<div class="pt" style="margin-top:8px"><b>Right-click a lit warhead</b> to put its fuse out. <b>Left-click a missile</b> on a pad or in a silo to take it apart: body and warhead drop as items, the fuel is lost. Too late once the countdown runs or the engines have lit; abort at the Launch Control first. It breaks after <b>{DEFUSER_USES} uses</b>; refusals cost nothing.</div></div></div>"""

    # ---- outputs
    outputs = f"""<div class="card" style="margin-top:18px"><h2>Redstone, covers and computers</h2>
<table class="t" style="margin-top:6px"><tr><th>Machine</th><th>Redstone</th><th>GT covers</th><th>OpenComputers (through an Adapter)</th></tr>
<tr><td>Radars and Radar Tower</td><td>Out: strength 1 to {MAX_REDSTONE} on every side, the number of incoming missiles; 0 when none</td><td>A machine-state cover reads the single blocks&#39; threat flag</td><td><b>gg_radar</b>: contacts, incoming, protected radius; signals on contact, incoming, lost</td></tr>
<tr><td>Air-Defense Battery</td><td>Out: 15 while it engages a threat in range, else 0</td><td>A machine-state cover reads engaging</td><td><b>gg_air_defense</b>: SAM count, fire at a contact, hold fire or auto; signals on fire and hit</td></tr>
<tr><td>EMP Tower</td><td>In: a rising edge fires one pulse</td><td>A machine controller cover fires on a rising edge; a machine-state cover reads ready</td><td><b>gg_emp_tower</b>: fire; signals when ready and when fired</td></tr></table>
<div class="muted" style="font-size:16px;margin-top:8px">A radar and a battery link through a Data Stick, never the air: the stick carries the radar&#39;s address. The handheld tools use no links but the warhead the detonator points at.</div></div>"""

    top = f"""<div class="card" style="display:grid;grid-template-columns:1.5fr 1fr;gap:26px;align-items:center">
<div><h2>See it coming, shoot it down, switch it off</h2><div class="pt" style="margin-top:8px">A <b>radar</b> watches the missiles in flight and flags the ones that will <b>land</b> inside the radius you protect. A linked <b>Air-Defense Battery</b> fires <b>SAMs</b> at them. An <b>EMP Tower</b> kills missiles by chance and wipes the charge of machines in reach. The <b>handheld tools</b> aim, light and put out charges you place yourself. Everything runs on GregTech EU at HV and up.</div></div>
<div class="spot" style="display:flex;justify-content:space-around;align-items:center;padding:6px">{img(hv, h=110)}{img(bat, h=110)}{img(emp, h=110)}</div></div>"""

    body = f"""{top}
<div class="label" style="margin:22px 0 10px">Radars &middot; the single blocks</div>
<div style="display:grid;grid-template-columns:1fr 1fr 1.4fr;gap:18px">{radars}</div>
<div class="grid" style="grid-template-columns:1fr 1fr;margin-top:18px">
<div class="card"><h2>Range by tier</h2><div class="muted" style="font-size:17px;margin-bottom:6px">A flat circle: height does not matter. The two blocks are HV and EV; a Radar Tower is IV and up, by its Energy Hatch.</div>{range_bars()}</div>
<div class="card"><h2>How a radar decides</h2><div style="margin-top:4px">{threat_diagram()}</div></div></div>
{tower_card}
<div class="label" style="margin:26px 0 10px">Air defense</div>{battery}
<div class="label" style="margin:26px 0 10px">The EMP tower</div>{emp_block}
<div class="label" style="margin:26px 0 10px">Handheld tools</div>{hand}
{outputs}"""
    return page_doc("Detection and Defense", "Radars, the Radar Tower, the Air-Defense Battery, the EMP Tower and the tools you carry.", body,
                    extra=CSS,
                    credit=["Machine overlays, casing, missile and warhead models and item icons © HBM's Nuclear Tech Mod contributors, GNU LGPL v3; modified.",
                            "Hulls, hatches and iconsets: GregTech 5 Unofficial textures, LGPL-3.0."])


PAGES = {"icbm-detection-and-defense": sheet}

if __name__ == "__main__":
    import build
    build.main(["icbm-detection-and-defense"])
