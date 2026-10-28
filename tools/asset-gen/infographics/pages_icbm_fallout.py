"""The icbm-fallout guide sheet: what a nuclear blast leaves, the radiation hazard and what protects, the antidote, the
Centrifuge / Macerator / Chemical Reactor chain with its yields, how yields fall off with distance and tier (D155), the
quarter-of-the-core bound and decay.

Every number is read from the Java sources at build time (`_const`, `_cfg`, the `FalloutYield` tables, the `FalloutRecipes`
machine tiers and times), so a changed constant moves the sheet instead of leaving it stale (D50). Warhead radii come
from the warhead dump (`GREGGUNS_EXPORT_WARHEADS`). Blocks are drawn from Mojang's real dirt, stone, sand, gravel and
cobblestone textures tinted the way the game tints irradiated earth (D55), and our own NTM-derived trinitite and goo art.
"""

from __future__ import annotations

import hashlib
import math
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import kit as K  # noqa: E402
import pages_icbm as P  # noqa: E402
import scenes as S  # noqa: E402
import thirdparty as TP  # noqa: E402
import vanilla  # noqa: E402
from kit import chip, esc  # noqa: E402
from pages import MOJANG_CREDIT, img, page_doc, slot  # noqa: E402

JAVA = K.REPO / "src/main/java/com/gregguns"
NUC = "explosion/nuclear/"
NTM_ITEMS = K.ASSETS / "textures/items/thirdparty/ntm"
NTM_BLOCKS = K.ASSETS / "textures/blocks/thirdparty/ntm"


# ---------------------------------------------------------------- numbers, read from the code


def _src(rel: str) -> str:
    return (JAVA / rel).read_text(encoding="utf-8")


def _num(text: str):
    v = float(text.replace("_", ""))
    return int(v) if "." not in text else v


def _const(rel: str, name: str):
    m = re.search(rf"\b{name}\s*=\s*(-?[0-9][0-9_]*(?:\.[0-9]+)?)[FfDdLl]?\s*;", _src(rel))
    if not m:
        raise SystemExit(f"{rel}: constant {name} not found; update pages_icbm_fallout.py")
    return _num(m.group(1))


def _hex(rel: str, name: str) -> tuple[int, int, int]:
    m = re.search(rf"\b{name}\s*=\s*0x([0-9A-Fa-f]{{6}})\s*;", _src(rel))
    if not m:
        raise SystemExit(f"{rel}: color {name} not found")
    v = int(m.group(1), 16)
    return (v >> 16) & 255, (v >> 8) & 255, v & 255


def _cfg(name: str):
    m = re.search(rf"public static (?:int|double) {name}\s*=\s*([0-9.]+);", _src("core/config/ExplosionsConfig.java"))
    if not m:
        raise SystemExit(f"ExplosionsConfig.{name} not found")
    return _num(m.group(1))


FM, FY, RUB = NUC + "FalloutMath.java", NUC + "FalloutYield.java", NUC + "ItemIrradiatedRubble.java"
INNER = _const(FM, "INNER_SHARE")
RIM = _const(FM, "RIM_SHARE")
GOO_PEAK = _const(FM, "GOO_PEAK")
RADIUS_FACTOR = _const(NUC + "NuclearScale.java", "FALLOUT_RADIUS_FACTOR")
START_DELAY = _const(NUC + "Fallout.java", "START_DELAY_TICKS")
EXPOSE_PERIOD = _const(NUC + "RadiationExposure.java", "PERIOD_TICKS")
CARRY_PERIOD = _const(RUB, "PERIOD_TICKS")
MAX_BLOCKS, MAX_RADIUS, MAX_GOO = _cfg("falloutMaxBlocks"), _cfg("falloutMaxRadius"), _cfg("falloutMaxGoo")
DECAY_MIN, REVERT = _cfg("falloutDecayMinutes"), _cfg("falloutRevertPerTick")
LEVEL = _cfg("falloutRadiationLevel")
TINT, HOT_TINT = _hex(NUC + "BlockIrradiatedEarth.java", "TINT"), _hex(RUB, "HOT_TINT")
CORE_U, CORE_PU = _const(FY, "CORE_U235"), _const(FY, "CORE_PU")
MAX_RETURN = _const(FY, "MAX_RETURN")
HOT_SHARE = _const(FY, "HOT_SHARE")
KRYPTON, XENON = _const(FY, "KRYPTON_LITERS"), _const(FY, "XENON_LITERS")

# GT recipe voltages (VP) of the tiers the chain uses.
VP = {"HV": 480, "EV": 1920, "LuV": 30720}
TIER_NAMES = {"HV": "HV", "EV": "EV", "LUV": "LuV"}

ELEMENT_NAME = {"Uranium": "Uranium", "Uranium235": "Uranium-235", "Plutonium": "Plutonium", "Plutonium241": "Plutonium-241"}


def _tables() -> dict:
    """The yield tables of FalloutYield: {name: [(element, size, chance in 1/10000)]}."""
    src = _src(FY)
    out = {}
    for name, body in re.findall(r"public static final Out\[\] (\w+) = \{(.*?)\};", src, re.S):
        out[name] = [(e, s, int(c)) for e, s, c in re.findall(r'out\("(\w+)", Size\.(\w+), (\d+)\)', body)]
    return out


TABLE = _tables()
assert set(TABLE) == {"RUBBLE", "HOT_RUBBLE", "CRUSHED_TRINITITE", "DISSOLVED_GOO", "DISSOLVED_HOT_GOO"}, TABLE.keys()
SIZE = {"TINY": 1 / 9, "SMALL": 0.25}


def _recipes() -> dict:
    """Machine tier of each step of FalloutRecipes, and the recipe times."""
    src = _src(NUC + "FalloutRecipes.java")
    cent = dict(re.findall(r"centrifuge\(\w+, FalloutYield\.(\w+), Tier\.(\w+), (?:true|false)\)", src))
    assert set(cent) == {"RUBBLE", "HOT_RUBBLE", "CRUSHED_TRINITITE", "DISSOLVED_GOO", "DISSOLVED_HOT_GOO"}, cent
    mac = re.search(r"\.duration\((\d+)\)\s*\.eut\(GTValues\.VP\[Tier\.(\w+)\.voltageIndex\(\)\]\)\s*\.addTo\(RecipeMaps\.maceratorRecipes\)", src)
    bath = re.search(r"\.duration\((\d+)\)\s*\.eut\(GTValues\.VP\[Tier\.(\w+)\.voltageIndex\(\)\]\)\s*\.addTo\(RecipeMaps\.chemicalReactorRecipes\)", src)
    ticks = int(re.search(r"CENTRIFUGE_TICKS = (\d+)", src).group(1))
    acid = int(re.search(r"ACID_LITERS = (\d+)", src).group(1))
    if not (mac and bath):
        raise SystemExit("FalloutRecipes changed; update pages_icbm_fallout.py")
    return dict(cent=cent, cent_ticks=ticks, mac=(int(mac.group(1)), mac.group(2)), bath=(int(bath.group(1)), bath.group(2)), acid=acid)


REC = _recipes()
assert "nitric" not in _src(NUC + "FalloutRecipes.java").lower() or "NitricAcid" in _src(NUC + "FalloutRecipes.java")


def _tier(name: str) -> str:
    return TIER_NAMES[name.upper()]


# the antidote
AD = "icbm/antidote/AntidoteRules.java"
GUARD_MIN = _const(AD, "PROTECTION_TICKS") / 20 / 60
USE_S = _const(AD, "USE_TICKS") / 20
PER_RUN = _const(AD, "PER_RUN")
STACK = int(re.search(r"setMaxStackSize\((\d+)\)", _src("icbm/antidote/ItemAntidote.java")).group(1))
_ad_reg = _src("icbm/antidote/AntidoteRegistration.java")
for snippet in ('Ing.gt("plate", "Rubber", 1', 'Ing.gt("plate", "StainlessSteel", 1', 'Ing.gt("dust", "Charcoal", 2', "Tier MACHINE_TIER = Tier.HV"):
    if snippet not in _ad_reg:
        raise SystemExit(f"AntidoteRegistration changed; update icbm-fallout: {snippet}")


def fissile_share() -> float:
    """The most fissile material one blast's fallout returns, as a share of the core, at the default block cap (the same
    arithmetic as FalloutYield.maxFissileShare)."""
    def exp(table, el):
        return sum(SIZE[s] * c / 10000 for e, s, c in TABLE[table] if e == el)
    per_hot = exp("HOT_RUBBLE", "Uranium235") + exp("HOT_RUBBLE", "Plutonium")
    per_trin = exp("CRUSHED_TRINITITE", "Uranium235") + exp("CRUSHED_TRINITITE", "Plutonium")
    return MAX_BLOCKS * HOT_SHARE * max(per_hot, per_trin) / (CORE_U + CORE_PU)


SHARE = fissile_share()
assert SHARE <= MAX_RETURN, SHARE


def fallout_radius(blast_radius: float) -> int:
    return min(math.ceil(RADIUS_FACTOR * blast_radius), MAX_RADIUS)


NUKE = P.BLAST["nuclear"]
NUKE_R = {f: P.curve("nuclear", f)[0] for f in (1, 8, NUKE["cap"])}


# ---------------------------------------------------------------- pictures


def _cube(tag: str, tex: K.Path, rgb=None) -> K.Path:
    if rgb:
        tex = TP._tinted(tex, rgb, f"fo-{tag}")
    key = f"{tag}-" + hashlib.sha1(tex.read_bytes()).hexdigest()[:8]
    return K.voxels(f"fo-{tag}", {(0, 0, 0): ("m", S.FRONT)}, {"m": (tex, tex, tex)}, yaw=-38, pitch=28, ppu=70, key=key)


def _earth(name: str, hot=False) -> K.Path:
    return _cube(f"{name}{'-hot' if hot else ''}", vanilla.texture(f"blocks/{name}"), HOT_TINT if hot else TINT)


def _item(name: str, rgb=None, px=64) -> K.Path:
    p = NTM_ITEMS / f"{name}.png"
    return K.icon(TP._tinted(p, rgb, f"fo-i-{name}") if rgb else p, px)


def block_row() -> str:
    cells = [(_earth("dirt"), "Irradiated Dirt"), (_earth("stone"), "Stone"), (_earth("sand"), "Sand"), (_earth("gravel"), "Gravel"),
             (_earth("cobblestone"), "Cobblestone")]
    plain = "".join(f'<div class="bk">{img(p, h=64)}<small>{esc(t)}</small></div>' for p, t in cells)
    hot = "".join(f'<div class="bk">{img(p, h=64)}<small>{esc(t)}</small></div>' for p, t in [
        (_earth("dirt", True), "Hot Irradiated Dirt, Stone, Sand..."),
        (_cube("trinitite", NTM_BLOCKS / "trinitite.png"), "Trinitite"),
        (_cube("goo", NTM_BLOCKS / "fallout_goo.png"), "Fallout Goo"),
        (_cube("goo-hot", NTM_BLOCKS / "fallout_goo.png", HOT_TINT), "Hot Fallout Goo")])
    return plain, hot


def disc(radius_full: int, inner: float) -> str:
    """A top-down picture of the zones around ground zero, to scale (fallout radius R, inner fireball INNER R)."""
    cx = cy = 150
    R = 132
    ri = R * inner
    return f"""<svg viewBox="0 0 300 300" style="width:100%;max-width:300px">
<defs><radialGradient id="rg" cx="50%" cy="50%" r="50%"><stop offset="0" stop-color="#9ce070" stop-opacity=".95"/><stop offset="1" stop-color="#9ce070" stop-opacity=".30"/></radialGradient></defs>
<circle cx="{cx}" cy="{cy}" r="{R}" fill="url(#rg)" stroke="#9ce070" stroke-width="3"/>
<circle cx="{cx}" cy="{cy}" r="{ri:.1f}" fill="#e8f04a" fill-opacity=".85" stroke="#fff3a0" stroke-width="3" stroke-dasharray="7 5"/>
<circle cx="{cx}" cy="{cy}" r="6" fill="#161b21" stroke="#fff" stroke-width="2"/>
<text x="{cx}" y="{cy - ri - 8:.0f}" text-anchor="middle" fill="#0c2a00" font-size="15" font-weight="700" font-family="Segoe UI, Fira Sans, sans-serif">hot zone</text>
<text x="{cx}" y="{cy + R - 22}" text-anchor="middle" fill="#0c2a00" font-size="15" font-weight="700" font-family="Segoe UI, Fira Sans, sans-serif">irradiated earth</text>
<text x="{cx}" y="{cy + 25}" text-anchor="middle" fill="#161b21" font-size="14" font-weight="700" font-family="Segoe UI, Fira Sans, sans-serif">ground zero</text></svg>"""


def falloff_chart() -> str:
    """Chance against distance, each curve as a share of its own ground-zero value, from the same formulas the engine uses."""
    W, H, L, B, T = 560, 250, 58, 48, 16
    pw, ph = W - L - 16, H - B - T

    def X(share):  # share of the fallout radius
        return L + share * pw

    def Y(v):
        return T + (1 - v) * ph
    pts_plain = [(0, 1.0), (1, RIM)]
    pts_hot = [(u * INNER, 1 - u * u) for u in [i / 20 for i in range(21)]]
    pts_goo = [(0, 1.0), (INNER, 0.0)]

    def path(pts):
        return " ".join(f"{'M' if i == 0 else 'L'}{X(a):.1f} {Y(b):.1f}" for i, (a, b) in enumerate(pts))
    ticks = "".join(f'<line x1="{X(s):.1f}" y1="{T}" x2="{X(s):.1f}" y2="{T + ph}" stroke="#2b3542"/><text x="{X(s):.1f}" y="{T + ph + 20}" text-anchor="middle" fill="#b4bfcb" font-size="15" font-family="Segoe UI, Fira Sans, sans-serif">{round(s * 100)}%</text>' for s in (0, 0.25, 0.5, 0.75, 1.0))
    ticks += "".join(f'<text x="{L - 8}" y="{Y(v) + 5:.1f}" text-anchor="end" fill="#b4bfcb" font-size="15" font-family="Segoe UI, Fira Sans, sans-serif">{round(v * 100)}%</text>' for v in (0, 0.5, 1.0))
    return f"""<svg viewBox="0 0 {W} {H}" style="width:100%">
{ticks}<line x1="{L}" y1="{T + ph}" x2="{L + pw}" y2="{T + ph}" stroke="#97a3b1" stroke-width="2"/>
<line x1="{X(INNER):.1f}" y1="{T}" x2="{X(INNER):.1f}" y2="{T + ph}" stroke="#e8f04a" stroke-width="2" stroke-dasharray="6 5"/>
<path d="{path(pts_plain)}" fill="none" stroke="#9ce070" stroke-width="4"/>
<path d="{path(pts_hot)}" fill="none" stroke="#e8f04a" stroke-width="4"/>
<path d="{path(pts_goo)}" fill="none" stroke="#ff8a1f" stroke-width="4" stroke-dasharray="2 7" stroke-linecap="round"/>
<text x="{X(0.5):.1f}" y="{Y(0.8):.1f}" fill="#9ce070" font-size="16" font-weight="700" font-family="Segoe UI, Fira Sans, sans-serif">plain earth</text>
<text x="{X(INNER) + 8:.1f}" y="{Y(0.36):.1f}" fill="#e8f04a" font-size="16" font-weight="700" font-family="Segoe UI, Fira Sans, sans-serif">hot earth, trinitite</text>
<text x="{X(INNER) + 8:.1f}" y="{Y(0.10):.1f}" fill="#ffb35c" font-size="16" font-weight="700" font-family="Segoe UI, Fira Sans, sans-serif">goo</text>
<text x="{L + pw / 2:.0f}" y="{H - 2}" text-anchor="middle" fill="#97a3b1" font-size="15" font-family="Segoe UI, Fira Sans, sans-serif">distance from ground zero, as a share of the fallout radius</text></svg>"""


def _pct(c: int) -> str:
    v = c / 100
    return f"{v:g}%"


def outs(table: str, extra="") -> str:
    parts = "".join(f'<span class="yo"><b>{esc(ELEMENT_NAME.get(e, e))}</b> {"small" if s == "SMALL" else "tiny"} &middot; {_pct(c)}</span>' for e, s, c in TABLE[table])
    return parts + extra


def lane(inputs: str, machine_pic: K.Path | None, mname: str, mtier: str, secs: float, eut: int, result: str, note="") -> str:
    pic = img(machine_pic, h=74) if machine_pic else ""
    return f"""<div class="ln"><div class="li">{inputs}</div><div class="la">&rarr;</div>
<div class="lm"><div>{pic}</div><div><b>{esc(mname)}</b> {chip(mtier)}<div class="muted" style="font-size:15px">{secs:g} s &middot; {eut:,} EU/t</div></div></div>
<div class="la">&rarr;</div><div class="lo">{result}{note}</div></div>"""


CSS = P.CSS + """
.bk { display: flex; flex-direction: column; align-items: center; gap: 2px; text-align: center; }
.bk small { color: var(--muted); font-size: 14.5px; line-height: 1.15; max-width: 120px; }
.ln { display: grid; grid-template-columns: 330px 24px 300px 24px 1fr; gap: 10px; align-items: center; padding: 9px 4px; border-bottom: 1px solid var(--line); }
.ln:last-child { border-bottom: 0; }
.ln .la { color: var(--orange); font-size: 28px; font-weight: 700; text-align: center; }
.ln .li { display: flex; align-items: center; gap: 10px; font-size: 17.5px; line-height: 1.2; }
.ln .lm { display: flex; align-items: center; gap: 10px; font-size: 18px; }
.ln .lo { display: flex; flex-wrap: wrap; gap: 6px; font-size: 16px; align-items: center; }
.yo { background: rgba(255,255,255,.06); border: 1px solid var(--line); border-radius: 7px; padding: 2px 9px; color: var(--muted); white-space: nowrap; }
.yo b { color: var(--txt); font-weight: 600; }
.yo.liq { border-color: #38c6d9; }
.fx { display: flex; flex-wrap: wrap; gap: 8px; margin: 8px 0 4px; }
.fx span { background: rgba(255,90,90,.14); border: 1px solid #ff7777; color: #ffd0d0; border-radius: 7px; padding: 3px 10px; font-size: 16.5px; }
.ok { color: #7be08a; font-weight: 700; }
.bad { color: #ff9a9a; font-weight: 700; }
.pt { color: var(--muted); font-size: 18px; line-height: 1.34; }
.pt b { color: var(--txt); }
.big { font-family: Bahnschrift, "Fira Sans Condensed", sans-serif; font-weight: 800; font-size: 50px; line-height: 1; color: var(--orange); }
.tbl { width: 100%; border-collapse: collapse; font-size: 17px; }
.tbl th { text-align: left; color: var(--dim); font-size: 14px; letter-spacing: .1em; text-transform: uppercase; padding: 5px 8px; border-bottom: 2px solid var(--line); font-weight: 600; }
.tbl td { padding: 6px 8px; border-bottom: 1px solid var(--line); color: var(--muted); }
.tbl td:first-child { color: var(--txt); font-weight: 600; }
.yes { color: #7be08a; font-weight: 700; } .no { color: #8b95a1; }
"""


def fallout() -> str:
    plain, hot = block_row()
    R_FULL = fallout_radius(NUKE_R[NUKE["cap"]])
    sizes = "".join(f"<tr><td>{f} of {NUKE['cap']} charges</td><td>{P._n(NUKE_R[f])} blocks</td><td>{fallout_radius(NUKE_R[f])} blocks</td><td>{fallout_radius(NUKE_R[f]) * INNER:.0f} blocks</td></tr>"
                    for f in (1, 8, NUKE["cap"]))
    # ---- what a strike leaves
    leaves = f"""<div class="card"><h2>What a nuclear blast leaves</h2>
<div class="pt" style="margin:6px 0 10px">Once the fireball is over, a disc of ground up to <b>{RADIUS_FACTOR:g} times the blast radius</b> (never past {MAX_RADIUS} blocks) turns into poisoned earth. Only the <b>surface</b> changes: the top solid block of each column, and up to two blocks under it, so caves and deep stone are never touched. At most {MAX_BLOCKS:,} blocks change per strike, and it never spreads. It starts {START_DELAY / 20:.0f} seconds after the blast.</div>
<div style="display:grid;grid-template-columns:300px 1fr;gap:22px;align-items:center">
<div style="display:flex;flex-direction:column;align-items:center">{disc(R_FULL, INNER)}<div class="muted" style="font-size:15px;margin-top:4px;text-align:center">The hot zone is the inner {round(INNER * 100)}% of the radius</div></div>
<div><div class="label" style="margin-bottom:6px">Irradiated earth &middot; the whole disc</div><div style="display:flex;gap:12px;flex-wrap:wrap;margin-bottom:14px">{plain}</div>
<div class="label" style="margin-bottom:6px">The hot zone only &middot; yellow</div><div style="display:flex;gap:12px;flex-wrap:wrap">{hot}</div></div></div>
<div class="pt" style="margin-top:12px"><b>Hot earth</b> and <b>trinitite</b> (fused sand) appear only near ground zero of a ZPM or better blast. <b>Fallout goo</b> is rare: at most {GOO_PEAK * 100:g}% of the top blocks at ground zero, fewer further out, and never more than {MAX_GOO} per strike. Mine them and they drop <b>Irradiated Rubble</b>, <b>Trinitite Shards</b> and <b>Fallout Goo</b> for the processing chain below. Antimatter and red matter leave no fallout at all.</div>
<div class="label" style="margin:16px 0 4px">A nuclear warhead by fill</div>
<table class="tbl"><tr><th>Fill</th><th>Blast radius</th><th>Fallout radius</th><th>Hot zone radius</th></tr>{sizes}</table></div>"""
    # ---- hazard
    hazard = f"""<div class="card"><h2>The radiation hazard</h2>
<div class="pt" style="margin:6px 0 8px">Stand on or beside irradiated ground (the 3 &times; 3 around your feet, or the layer below) and you are exposed <b>every {EXPOSE_PERIOD / 20:g} second</b>. Carrying rubble exposes you every {CARRY_PERIOD / 20:g} seconds, and hot rubble hits harder than plain. Exposure is level {LEVEL} (a server setting).</div>
<div class="fx"><span>Slowness</span><span>Mining Fatigue</span><span>Nausea</span><span>Weakness</span><span>Hunger</span><span>Radiation damage</span></div>
<div class="pt" style="margin-top:10px"><span class="ok">Protected:</span> anyone in <b>full hazmat armor</b>. Any hazmat-capable armor counts: GregTech's hazmat suit, a full HEV set or other GregGuns armor flagged for radiation, or armor carrying the Hazmat enchantment. Creative players are never exposed.</div></div>"""
    # ---- antidote
    ant = f"""<div class="card"><div style="display:flex;gap:16px;align-items:center"><span>{slot(_item("antidote"), 56, "Antidote")}</span><h2>The Antidote</h2>{chip("HV")}</div>
<div class="pt" style="margin:8px 0 6px">A syringe, stack of {STACK}. Hold use for {USE_S:g} seconds, then:</div>
<div class="pt"><b>1. Cures at once:</b> poison, hunger, weakness, slowness, nausea, mining fatigue and radiation.<br><b>2. Guards for {GUARD_MIN:g} minutes:</b> the same effects are wiped again every half second, so you can cross fallout and gas. You are still exposed, so armor stays the real answer.</div>
<div class="pt" style="margin-top:8px"><span class="bad">Does nothing</span> against fire, explosions, EMP or annihilation. Beneficial effects are never touched; it also clears the slowness of status weapons.</div>
<div class="muted" style="font-size:16px;margin-top:8px">Craft in the Assembler ({chip("HV")}): 1 rubber plate, 1 stainless steel plate and 2 charcoal dust make {PER_RUN:g} antidotes.</div></div>"""

    # ---- distance and tier
    fall = f"""<div class="card"><h2>Yields fall off with distance and tier</h2>
<div class="pt" style="margin:6px 0 8px">The closer to ground zero, the richer the ground. Each curve is a share of its own value at the center.</div>
{falloff_chart()}
<div class="pt" style="margin-top:8px">Hot earth and trinitite: <b>certain at ground zero, none at the edge of the hot zone</b>, thinning smoothly in between. Goo: {GOO_PEAK * 100:g}% at the center, none at the edge of the hot zone. Plain earth gets patchier toward the rim, down to {round(RIM * 100)}% as dense.</div></div>"""
    tier = f"""<div class="card"><h2>It scales with the warhead</h2>
<table class="tbl"><tr><th>Blast</th><th>Plain earth</th><th>Hot earth, trinitite, hot goo</th><th>Plain goo</th></tr>
<tr><td>Nuclear warhead (ZPM)</td><td class="yes">yes</td><td class="yes">yes</td><td class="yes">yes</td></tr>
<tr><td>Nuke Launcher</td><td class="yes">yes, a small disc</td><td class="no">no</td><td class="yes">yes</td></tr>
<tr><td>Antimatter, red matter</td><td class="no">none</td><td class="no">none</td><td class="no">none</td></tr></table>
<div class="pt" style="margin-top:14px">A bigger warhead makes a <b>wider</b> hot zone and more blocks, not richer ones: what one block gives from the Centrifuge is the same at any size. Plain rubble has no uranium-235 or plutonium at all; only hot rubble and trinitite carry a trace, which is why the ZPM warhead matters.</div></div>"""

    # ---- chain
    cent = REC["cent"]
    ct, cent_s = REC["cent_ticks"], REC["cent_ticks"] / 20
    mt, mtier = REC["mac"]
    bt, btier = REC["bath"]

    def icon(name, rgb=None, label=""):
        return slot(_item(name, rgb), 38, label)
    cm = {t: S.machine_block("centrifuge", t, greg=True) for t in ("HV", "EV")}
    mm = S.machine_block("macerator", _tier(mtier), greg=True)
    rm = S.machine_block("chemical_reactor", _tier(btier), greg=True)
    gases = f'<span class="yo liq"><b>Krypton</b> {KRYPTON:g} L</span><span class="yo liq"><b>Xenon</b> {XENON:g} L</span>'
    lanes = "".join([
        lane(f'{icon("irradiated_rubble", None, "Irradiated Rubble")}<span><b>Irradiated Rubble</b></span>', cm["HV"], "Centrifuge", _tier(cent["RUBBLE"]), cent_s, VP[_tier(cent["RUBBLE"])], outs("RUBBLE")),
        lane(f'{icon("irradiated_rubble", HOT_TINT, "Hot Irradiated Rubble")}<span><b>Hot Irradiated Rubble</b></span>', cm["HV"], "Centrifuge", _tier(cent["HOT_RUBBLE"]), cent_s, VP[_tier(cent["HOT_RUBBLE"])], outs("HOT_RUBBLE")),
        lane(f'{icon("trinitite_shard", None, "Trinitite Shard")}<span><b>Trinitite Shard</b></span>', mm, "Macerator", _tier(mtier), mt / 20, VP[_tier(mtier)], '<span class="yo"><b>Crushed Trinitite</b></span>'),
        lane(f'{icon("trinitite_shard", None, "Crushed Trinitite")}<span><b>Crushed Trinitite</b></span>', cm["HV"], "Centrifuge", _tier(cent["CRUSHED_TRINITITE"]), cent_s, VP[_tier(cent["CRUSHED_TRINITITE"])], outs("CRUSHED_TRINITITE")),
        lane(f'{icon("fallout_goo", None, "Fallout Goo")}<span><b>Goo or Hot Goo</b> + {REC["acid"]:,} L nitric acid</span>', rm, "Chemical Reactor", _tier(btier), bt / 20, VP[_tier(btier)], '<span class="yo"><b>Dissolved Goo</b> (or Dissolved Hot Goo)</span>'),
        lane(f'{icon("fallout_goo", None, "Dissolved Goo")}<span><b>Dissolved Goo</b></span>', cm["EV"], "Centrifuge", _tier(cent["DISSOLVED_GOO"]), cent_s, VP[_tier(cent["DISSOLVED_GOO"])], outs("DISSOLVED_GOO", gases)),
        lane(f'{icon("fallout_goo", HOT_TINT, "Dissolved Hot Goo")}<span><b>Dissolved Hot Goo</b></span>', cm["EV"], "Centrifuge", _tier(cent["DISSOLVED_HOT_GOO"]), cent_s, VP[_tier(cent["DISSOLVED_HOT_GOO"])], outs("DISSOLVED_HOT_GOO", gases)),
    ])
    chain = f"""<div class="card"><h2>Cleaning it up: the processing chain</h2>
<div class="pt" style="margin:6px 0 4px">Everything runs in ordinary GregTech machines, so each recipe shows in NEI. Tiers are the lowest the machine can be. Chances are per item, and the dusts are small (a quarter of a dust) or tiny (a ninth).</div>{lanes}
<div class="muted" style="font-size:16px;margin-top:8px">Rubble and trinitite give the base metals and a trace of fissile material; goo gives the heavy actinides and fission products that GregTech has no other free source for. Items carry no recycling value of their own, so nothing here is a shortcut into fuel.</div></div>"""

    # ---- bound and decay
    bound = f"""<div class="card"><h2>It never pays for the bomb</h2>
<div style="display:flex;gap:22px;align-items:center"><div class="big" style="white-space:nowrap;flex:none">&le;&nbsp;{round(MAX_RETURN * 100)}%</div>
<div class="pt" style="font-size:18.5px">One strike's whole fallout can return <b>at most a quarter</b> of the fissile material in the warhead's core ({CORE_U:g} uranium-235 ingots and {CORE_PU:g} plutonium ingots), however big the blast. At the default settings it is about <b>{SHARE * 100:.0f}%</b>. A warhead is never a net source of fuel; the value of fallout is in the heavier elements and the gases.</div></div></div>"""
    decay = f"""<div class="card"><h2>It heals, unless there is goo</h2>
<div class="pt" style="margin:6px 0 4px">Once a strike's ground is placed, it fades back to normal <b>{DECAY_MIN:g} minutes</b> later, up to {REVERT:g} blocks a tick, back to what it was (grass returns as dirt). Ground in chunks nobody has loaded heals when someone comes back.</div>
<div class="pt"><b>Fallout goo stops the clock.</b> While any goo stands in a chunk, that chunk's irradiated earth does not heal, however long you wait. <b>Dig the goo up</b> (shovel) and the land is released to decay. Process it in the chain below, or store it safely.</div></div>"""

    body = f"""<div class="grid" style="grid-template-columns:1.35fr 1fr;align-items:stretch">{leaves}<div style="display:flex;flex-direction:column;gap:18px">{hazard}{ant}</div></div>
<div class="grid" style="grid-template-columns:1.15fr 1fr;margin-top:18px">{fall}{tier}</div>
<div style="margin-top:18px">{chain}</div>
<div class="grid" style="grid-template-columns:1fr 1fr;margin-top:18px">{bound}{decay}</div>"""
    return page_doc("Fallout", "What a nuclear blast leaves behind, how to survive it, and how to turn it into something useful.", body, extra=CSS,
                    credit=["Item and block art © HBM's Nuclear Tech Mod contributors, GNU LGPL v3; modified. Warhead sheet models likewise.",
                            "Machine hulls and overlays: GregTech 5 Unofficial textures, LGPL-3.0.", MOJANG_CREDIT,
                            TP.TINT_CREDIT.replace("grass and leaves are", "the irradiated earth blocks are") + " (green for irradiated, yellow for hot.)"])


PAGES = {"icbm-fallout": fallout}

if __name__ == "__main__":
    import build
    build.main(["icbm-fallout"])
