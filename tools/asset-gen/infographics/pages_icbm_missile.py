"""The icbm-first-missile guide sheet: from a missile chip to the first launch from the Galacticraft pad (T1 and T2).

The missile models are the shipped OBJ files (body plus warhead, the warhead seated on the body's nose exactly as the
game seats it), drawn with render3d.obj_scene, the strict Forge reader. The numbers are read from the Java sources at
build time (MissileTier, WarheadCasing, MissileBuildSpec, Scatter, LaunchTimer, FlightPath, MTELaunchControl,
IcbmConfig), and `_verify` checks every recipe fact the text spells out against MissileBuildSpec and ControlMachines,
so a changed constant or recipe stops the build instead of leaving the sheet stale (D50).
The launch pad is Galacticraft's own stepped plate and the plates, fins and engines are its real icons, from Galacticraft
Legacy under the MIT License (D85; the files sit in the thirdparty folders). The Launch Control is drawn with its real
face (pages_icbm_silo.launch_control_tex).

Run on its own (needs no warhead dump):  python tools/asset-gen/infographics/pages_icbm_missile.py
"""

from __future__ import annotations

import hashlib
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import gt  # noqa: E402
import kit as K  # noqa: E402
import render3d as R  # noqa: E402
import scenes as S  # noqa: E402
import thirdparty as TP  # noqa: E402
from kit import chip, esc  # noqa: E402
from pages import gti, img, page_doc, slot  # noqa: E402

NTM = K.ASSETS / "models/thirdparty/ntm"
NTM_TEX = K.ASSETS / "textures/models/thirdparty/ntm"
JAVA = K.REPO / "src/main/java/com/gregguns"


# ---------------------------------------------------------------- numbers, read from the code


def src(rel: str) -> str:
    return (JAVA / rel).read_text(encoding="utf-8")


def const(rel: str, name: str) -> float:
    """The numeric value of `static final <type> NAME = <number>;` in a Java file."""
    m = re.search(rf"\b{name}\s*=\s*(-?[0-9][0-9_]*(?:\.[0-9]+)?)[FfDdLl]?\s*;", src(rel))
    if not m:
        raise SystemExit(f"{rel}: constant {name} not found; update {Path(__file__).name}")
    v = float(m.group(1).replace("_", ""))
    return int(v) if v == int(v) and "." not in m.group(1) else v


def cfg(name: str) -> float:
    """A default from IcbmConfig (`public static int|double name = value;`)."""
    m = re.search(rf"public static (?:int|double) {name}\s*=\s*([0-9.]+);", src("core/config/IcbmConfig.java"))
    if not m:
        raise SystemExit(f"IcbmConfig.{name} not found; update {Path(__file__).name}")
    return float(m.group(1)) if "." in m.group(1) else int(m.group(1))


def missile_tiers() -> dict:
    """{n: dict(fuel_mb, min, rng, speed, scatter, height, hp)} from the MissileTier enum rows."""
    rows = re.findall(r'T(\d)\("missile_body_t\d", ExplosiveTier\.T\d, (\d+), (\d+), (\d+), ([0-9.]+), ([0-9.]+), (\d+), ([0-9.]+)F',
                      src("icbm/missile/MissileTier.java"))
    if len(rows) != 3:
        raise SystemExit("MissileTier rows changed; update pages_icbm_missile.py")
    return {int(t): dict(fuel_mb=int(f), min=int(lo), rng=int(hi), speed=float(sp), scatter=float(sc), height=int(h), hp=float(hp))
            for t, f, lo, hi, sp, sc, h, hp in rows}


def fill_caps() -> dict:
    """{casing number: most charges} from WarheadCasing."""
    rows = re.findall(r'T(\d)\("warhead_t\d", ExplosiveTier\.T\d, (\d+), ', src("icbm/warhead/WarheadCasing.java"))
    if len(rows) < 3:
        raise SystemExit("WarheadCasing rows changed; update pages_icbm_missile.py")
    return {int(t): int(c) for t, c in rows}


def build_spec(n: int) -> str:
    """The MissileBuildSpec enum entry of tier n, as source text."""
    m = re.search(rf"\n    T{n}\({n}, (.*?)\)\)\)[,;]", src("icbm/production/MissileBuildSpec.java"), re.S)
    if not m:
        raise SystemExit(f"MissileBuildSpec.T{n} not found; update pages_icbm_missile.py")
    return m.group(1)


def body_ticks(n: int) -> int:
    m = re.search(r"\),\s*(\d+), (?:null|Tier\.\w+), (\d+), \d+, \d+,", build_spec(n))
    if not m:
        raise SystemExit(f"MissileBuildSpec.T{n}: body ticks not found")
    return int(m.group(2))


def need(text: str, *facts: str, where: str = "") -> None:
    """Stops the build when a recipe fact the sheet prints is gone from the source."""
    for f in facts:
        if f not in text:
            raise SystemExit(f"{where}: '{f}' not found; the sheet's text no longer matches the code")


TIERS_SRC = missile_tiers()
CAPS = fill_caps()
MIN_RANGE = TIERS_SRC[1]["min"]
COUNTDOWN_S = cfg("countdownTicks") / 20
FLAME_S = const("icbm/missile/LaunchTimer.java", "FLAME_TICKS") / 20
FUEL_BONUS = cfg("fuelBonusMax")
MAX_IN_FLIGHT = cfg("maxMissilesInFlight")
SHRINK = const("icbm/missile/Scatter.java", "SHRINK_PER_TIER")
DIST_TERM = const("icbm/missile/Scatter.java", "DISTANCE_TERM")
CLIMB_CLEAR = int(const("icbm/missile/FlightPath.java", "MIN_CLIMB"))
SHOT_EU = const("icbm/control/MTELaunchControl.java", "EU_PER_SHOT")
FIT_S = const("icbm/production/MissileBuildSpec.java", "FIT_TICKS") / 20
LUBE_L = const("icbm/production/MissileBuildSpec.java", "LUBRICANT_LITERS")
BODY_S = {n: body_ticks(n) / 20 for n in (1, 2)}


def _verify() -> None:
    """Every recipe fact the text spells out, checked against the source."""
    t1, t2 = build_spec(1), build_spec(2)
    need(t1, 'Tier.HV', '"craftingLensRed"', 'Ing.gt("compressed", "Steel", 3', "FINS_T1, 4", "ENGINE_T1, 1",
         "GUIDANCE_COMPUTER_MK1, IcbmPart.GUIDANCE_COMPUTER_MK2, IcbmPart.GUIDANCE_COMPUTER_MK3",
         '"Circuit_Board_Plastic_Advanced", 1', 'Ing.gt("circuit", "Advanced", 2', 'Ing.gt("wireFine", "Gold"', where="MissileBuildSpec.T1")
    need(t2, 'Tier.EV', '"craftingLensBlue"', 'Ing.gt("compressed", "Titanium", 4', "FINS_T2, 4", "ENGINE_T2, 1",
         "Arrays.asList(IcbmPart.GUIDANCE_COMPUTER_MK2, IcbmPart.GUIDANCE_COMPUTER_MK3)",
         '"Circuit_Board_Epoxy_Advanced", 1', 'Ing.gt("circuit", "Data", 2', 'Ing.gt("wireFine", "Electrum"', where="MissileBuildSpec.T2")
    lc = src("icbm/control/ControlMachines.java")
    need(lc, "Hull_HV", "Sensor_HV", "Emitter_HV", "Tool_DataStick", '"Advanced", 2', "RECIPE_HV", where="ControlMachines")
    need(src("icbm/missile/MissileTier.java"), '"nitrofuel", "fluid.rocketfuelmixb"', where="MissileTier T1 fuels")
    lang = (K.REPO / "src/main/resources/assets/gregguns/lang/en_US.lang").read_text(encoding="utf-8")
    need(lang, "gregguns.icbm.control.gui.mypos=Use my position", "gregguns.icbm.control.gui.save=Save target",
         "gregguns.icbm.control.gui.load=Load target", "gregguns.icbm.control.gui.abort=Abort",
         "gregguns.icbm.control.gui.fire=Launch", "gregguns.icbm.control.status.not_fueled=Tank is not full",
         "item.gregguns.radar_gun.name=Radar Gun", where="en_US.lang")
    assert COUNTDOWN_S == int(COUNTDOWN_S) and FLAME_S == 1, (COUNTDOWN_S, FLAME_S)
    assert SHRINK == 0.25, f"Scatter.SHRINK_PER_TIER is {SHRINK}; the guidance sentence says a quarter per tier"


_verify()


def _minutes(seconds: float) -> str:
    m = round(seconds / 60)
    return "about a minute" if m <= 1 else f"about {['', 'one', 'two', 'three', 'four'][m] if m < 5 else m} minutes"


def _words(n: int) -> str:
    return ["zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve"][n] if n <= 12 else str(n)


TIERS = {
    1: dict(body="missile_body_t1", warhead="warhead_t1", volt="HV", fuel="RP-1 or Cetane-Boosted Diesel", warheads="T1",
            plates="3 compressed steel", fins="4 Galacticraft Tier 1", engine="Galacticraft Tier 1 engine"),
    2: dict(body="missile_body_t2", warhead="warhead_t2", volt="EV", fuel="Dense Hydrazine", warheads="T1 or T2",
            plates="4 compressed titanium", fins="4 Galacticraft Tier 2", engine="Galacticraft Tier 2 engine"),
}
for _n, _t in TIERS.items():
    s = TIERS_SRC[_n]
    _t.update(fuel_mb=s["fuel_mb"], rng=s["rng"], speed=f"{s['speed']:g}", scatter=int(s["scatter"]), cap=CAPS[_n],
              flight=_minutes(s["rng"] / s["speed"] / 20))
PARTS = {1: dict(plate="compressed_steel", plate_n=3, plate_l="Compressed Steel Plate", fins="rocket_fins_t1", engine="rocket_engine_t1", ft="Tier 1"),
         2: dict(plate="compressed_titanium", plate_n=4, plate_l="Compressed Titanium Plate", fins="rocket_fins_t2", engine="rocket_engine_t2", ft="Tier 2")}


def body_parts(n: int, px: int = 52) -> str:
    """The body recipe's parts as Galacticraft's own icons."""
    q = PARTS[n]
    return (TP.part(TP.gc_icon(q["plate"]), q["plate_n"], q["plate_l"], px) + TP.part(TP.gc_icon(q["fins"]), 4, f"{q['ft']} Rocket Fins", px)
            + TP.part(TP.gc_icon(q["engine"]), 1, f"{q['ft']} Rocket Engine", px))

CSS = """
.stat .chip.t-HV, .stat .chip.t-MV { color: #1a1200; }
.hero { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
.hero .spot { height: 560px; display: flex; align-items: flex-end; justify-content: center; padding-bottom: 14px; }
.stat { display: flex; justify-content: space-between; gap: 14px; padding: 5px 0; border-bottom: 1px solid var(--line); font-size: 19px; }
.stat:last-child { border-bottom: 0; }
.stat span:first-child { color: var(--muted); }
.stat b { font-weight: 600; text-align: right; }
.steps { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; margin-top: 22px; }
.sc { display: flex; flex-direction: column; gap: 10px; }
.sc .hd { display: flex; align-items: center; gap: 14px; }
.sc .vis { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; min-height: 96px; }
.sc .t { font-size: 18px; color: var(--muted); line-height: 1.34; }
.sc .t b { color: var(--txt); }
.pill { display: inline-block; background: rgba(255,255,255,.07); border: 1px solid var(--line); border-radius: 8px; padding: 1px 9px; font-size: 16px; color: var(--txt); white-space: nowrap; }
.tl { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; margin-top: 12px; }
.tl > div { background: rgba(255,255,255,.035); border: 1px solid var(--line); border-radius: 12px; padding: 12px 14px; }
.tl .k { font-family: Bahnschrift, "Fira Sans Condensed", "Noto Sans", sans-serif; font-weight: 700; font-size: 14px; letter-spacing: .1em; text-transform: uppercase; color: var(--orange); }
.tl .v { font-size: 17px; color: var(--muted); margin-top: 4px; line-height: 1.3; }
"""


def _sig(*paths: Path) -> str:
    return hashlib.sha1(b"".join(p.read_bytes() for p in paths)).hexdigest()[:10]


def missile_on_pad(tier: int, ppu=170) -> K.Path:
    """The stacked missile (body plus warhead) standing on Galacticraft's 3 x 3 launch pad, three steps 0.4 of a block tall."""
    t = TIERS[tier]
    files = [NTM / f"{t['body']}.obj", NTM_TEX / f"{t['body']}.png", NTM / f"{t['warhead']}.obj", NTM_TEX / f"{t['warhead']}.png"]

    def make():
        body = R.obj_scene(*files[:2])
        seat = body.bounds()[1][1]
        sc = TP.pad_formed(0, 0)
        sc.extend(body, R.tr(1, TP.PAD_HEIGHT, 1))
        sc.extend(R.obj_scene(*files[2:]), R.tr(1, TP.PAD_HEIGHT + seat, 1))
        return R.trim(R.render(sc, yaw=-30, pitch=14, ppu=ppu, ss=2, outline=K.RIM), 2)
    return K._cached(f"missile-pad-{tier}", dict(sig=_sig(*files, TP.GC_PAD), u=ppu, v=3), make)


def block_icon(tag: str, tier: str, overlay: str | None) -> K.Path:
    """A GregTech single block: the tier hull, with a basic machine's overlay when one is named."""
    return S.machine_block(overlay, tier, greg=True) if overlay else K.voxels(
        f"hull-{tag}-{tier}", {(0, 0, 0): ("m", S.FRONT)}, {"m": (gt.hull(tier, "TOP"), gt.hull(tier, "SIDE"), gt.hull(tier, "SIDE"))},
        yaw=-38, pitch=28, ppu=150)


def launch_control_icon() -> K.Path:
    """The Launch Control block with its real faces (an HV hull with its overlays), as the silo sheet draws it."""
    import pages_icbm_silo as SILO  # lazy: the silo sheet imports this module's readers
    return K.voxels("launch-control-HV", {(0, 0, 0): ("m", S.FRONT)}, {"m": SILO.launch_control_tex()}, yaw=-38, pitch=28, ppu=150)


def _tier_card(n: int) -> str:
    t = TIERS[n]
    stats = [("Chip", f"{chip(t['volt'])} Circuit Assembler"),
             ("Fuel", f"{esc(t['fuel'])} or better"), ("Tank", f"{t['fuel_mb']:,} mB, filled to the top"), ("Range", f"{MIN_RANGE} to {t['rng']:,} blocks"),
             ("Flight", f"{t['flight']} at the longest range"), ("Warhead", f"{t['warheads']}, up to {t['cap']} charges")]
    rows = "".join(f'<div class="stat"><span>{k}</span><b>{v}</b></div>' for k, v in stats)
    return f"""<div class="card"><div style="display:flex;align-items:center;gap:12px"><h2>Tier {n} missile</h2>{chip(t['volt'], t['volt'] if n == 1 else 'EV to IV')}</div>
<div class="spot">{img(missile_on_pad(n), h=520)}</div><div class="stat" style="align-items:center"><span>Body parts</span><div class="parts" style="justify-content:flex-end;flex-wrap:nowrap;gap:2px">{body_parts(n)}</div></div>{rows}</div>"""


def _step(n: int, title: str, vis: str, text: str) -> str:
    return f"""<div class="card sc"><div class="hd"><span class="num">{n}</span><h2>{esc(title)}</h2></div><div class="vis">{vis}</div><div class="t">{text}</div></div>"""


def first_missile() -> str:
    arrow = '<span class="arrow">&rarr;</span>'
    plus = '<span class="plus">+</span>'
    ca = img(block_icon("ca", "HV", "circuitassembler"), h=92)
    le = img(block_icon("le", "HV", "laser_engraver"), h=92)
    lc = img(launch_control_icon(), h=92)
    asm = img(block_icon("as", "HV", "assembler"), h=92)
    cd = _words(int(COUNTDOWN_S))
    s1 = _step(1, "Make a missile chip",
               f"{ca}{arrow}<span class='pill'>Missile Chip T1 &middot; HV</span><span class='pill'>Missile Chip T2 &middot; EV</span>",
               "The chip comes from the GregTech <b>Circuit Assembler</b>, at the voltage of its tier: a circuit board, 2 circuits and fine wire. "
               "<b>T1</b> is an Advanced Plastic board, 2 Advanced circuits and gold wire at HV. <b>T2</b> is an Advanced Epoxy board, 2 Data circuits and electrum wire at EV.")
    s2 = _step(2, "Engrave the schematic",
               f"<span class='pill'>Missile Chip</span>{plus}<span class='pill'>Red / Blue lens</span>{arrow}{le}{arrow}<span class='pill'>Missile Schematic</span>",
               "Put the chip and a lens in the <b>Laser Engraver</b>: a Red lens for T1, a Blue lens for T2. The lens is not used up. "
               "Slot the schematic into the Galacticraft <b>NASA Workbench</b> and the page for that missile opens.")
    s3 = _step(3, "Build the body by hand",
               f"<div class='parts' style='gap:2px'>{body_parts(1, 44)}</div><span class='plus'>or</span><div class='parts' style='gap:2px'>{body_parts(2, 44)}</div>{plus}<span class='pill'>Guidance Computer</span>{arrow}<span class='pill'>Missile Body</span>",
               "On the NASA Workbench page: <b>compressed plates</b> in one column (T1 three steel, T2 four titanium), <b>four Galacticraft rocket fins</b> and a <b>Galacticraft rocket engine</b> of the same tier, and a "
               "<b>Guidance Computer</b>: Mk I, II or III for T1, Mk II or III for T2. Make compressed plates from a plate in the Implosion Compressor. There is no warhead slot: a warhead goes on at the pad.")
    s3b = _step(4, "Or automate it",
                f"<span class='pill'>Body parts</span>{plus}<span class='pill'>2 circuits</span>{plus}<span class='pill'>Lubricant</span>{arrow}{asm}{arrow}<span class='pill'>Missile Body</span>",
                f"The GregTech <b>Assembler</b> builds the body too, with no research: the same plates, fins and engine, 2 circuits (Advanced for T1, Data for T2) in place of the computer "
                f"and {LUBE_L:,} L of lubricant, in {BODY_S[1]:g} seconds at HV (T1) or {BODY_S[2]:g} seconds at EV (T2). It makes the basic body; "
                f"a better <b>Guidance Computer</b> goes in afterwards in the <b>Assembler</b> again ({FIT_S:g} seconds, at the body's voltage). "
                "<b>Each computer tier above the body's own cuts its base miss by a quarter</b>: Mk II on a T1 body by a quarter, Mk III by half.")
    s4 = _step(5, "Stack it on the launch pad",
               f"{img(missile_on_pad(1, 100), h=150)}{img(missile_on_pad(2, 100), h=150)}",
               "Place the <b>Missile Body</b> on the middle of a Galacticraft <b>Launch Pad</b>, then set a loaded <b>warhead</b> on its nose (an inert one cannot be placed). It seats flush. "
               "<b>Sneak and right-click the body with an empty hand</b> to arm it. The stack becomes the flying missile; "
               "its model does not change.")
    s5 = _step(6, "Fuel it",
               f"<span class='pill'>Fuel Loader</span>{arrow}<span class='pill'>T1 &middot; {TIERS[1]['fuel_mb']:,} mB RP-1 or Cetane-Boosted Diesel</span><span class='pill'>T2 &middot; {TIERS[2]['fuel_mb']:,} mB Dense Hydrazine</span>",
               "Attach a Galacticraft <b>Fuel Loader</b> to the pad and feed it. Each tier takes its own fuel <b>or better</b>: RP-1 or Cetane-Boosted Diesel for T1, Dense Hydrazine for T2. "
               f"<b>Fill the tank to the top</b>: a missile launches only when it is full. A closer target burns less, and <b>whatever fuel is left makes the blast bigger</b>, up to {FUEL_BONUS * 100:g} percent.")
    s6 = _step(7, "Aim it",
               f"{lc}{arrow}<span class='pill'>Radar Gun</span><span class='pill'>X and Z</span><span class='pill'>Use my position</span>",
               "Put a <b>Launch Control</b> block (HV) against an edge of the pad, level with it (a corner does not count). Aim a <b>Radar Gun</b> at the target and right-click the Launch Control with it, "
               "or drop the gun in its slot and press <b>Load target</b>. You can also type the target <b>X and Z</b> or press <b>Use my position</b>; <b>Save target</b> copies the target onto the gun in the slot. "
               f"The screen shows the range and the blast radius to expect. The target must be at least <b>{MIN_RANGE} blocks</b> away.")
    s7 = _step(8, "Launch",
               f"<span class='pill'>Launch</span><span class='pill'>Redstone pulse</span><span class='pill'>Cover</span>{arrow}<span class='pill'>{COUNTDOWN_S:g} s countdown</span><span class='pill'>Abort</span>",
               f"Press <b>Launch</b>, send a <b>redstone pulse</b> or use a machine controller cover. One shot costs {SHOT_EU:,} EU. Keep the <b>{CLIMB_CLEAR} blocks above the missile&#39;s nose clear</b>: a block there stops the launch. "
               f"A <b>{cd}-second countdown</b> runs first; <b>Abort</b> stops it and leaves the missile armed and fueled. "
               "<b>Order of play:</b> chip, schematic, body, pad, warhead, fuel, target, launch. The warhead is the expensive part: see the warhead sheet for what each one does.")
    flight = f"""<div class="card" style="margin-top:22px"><h2>Launch, flight and impact</h2>
<div class="tl"><div><div class="k">1 &middot; Countdown</div><div class="v">{cd.capitalize()} seconds; the engines light for the last {"second" if FLAME_S == 1 else f"{FLAME_S:g} seconds"}. Abort works until then. Keep clear of the pad.</div></div>
<div><div class="k">2 &middot; Climb and arc</div><div class="v">It climbs straight up to the top of the world, then arcs over and down onto the target. Tier 1 covers {TIERS[1]['speed']} blocks a tick, Tier 2 {TIERS[2]['speed']}.</div></div>
<div><div class="k">3 &middot; Scatter</div><div class="v">It never misses entirely, but it lands off target: up to {TIERS[1]['scatter']} blocks either way for T1 and {TIERS[2]['scatter']} for T2, up to {DIST_TERM:g} more at the longest range, less with a better guidance computer.</div></div>
<div><div class="k">4 &middot; Impact</div><div class="v">It detonates on contact, never in the air. The warhead decides the blast; leftover fuel makes it bigger.</div></div>
<div><div class="k">5 &middot; The pad</div><div class="v">The pad, the fuel loader and the launch control are never harmed. Stack the next one.</div></div></div>
<div class="muted" style="font-size:16px;margin-top:12px">Only {MAX_IN_FLIGHT} missiles can be in the air on a server at once by default. Missiles load only the ground they are flying over.</div></div>"""
    body = f"""<div class="hero">{_tier_card(1)}{_tier_card(2)}</div>
<div class="steps">{s1}{s2}{s3}{s3b}{s4}{s5}{s6}{s7}</div>
{flight}"""
    return page_doc("Your First Missile", "Tier 1 and Tier 2 missiles: from a chip to a launch from the Galacticraft launch pad.", body, extra=CSS + TP.PART_CSS,
                    credit=["Missile and warhead models and textures © HBM's Nuclear Tech Mod contributors, GNU LGPL v3; modified.",
                            "Launch Control overlays: derived from HBM's Nuclear Tech Mod, GNU LGPL v3.",
                            "GregTech textures from GregTech 5 Unofficial, LGPL-3.0.", TP.GC_CREDIT])


PAGES = {"icbm-first-missile": first_missile}

if __name__ == "__main__":
    K.rasterize([(n, f(), 1600) for n, f in PAGES.items()])
