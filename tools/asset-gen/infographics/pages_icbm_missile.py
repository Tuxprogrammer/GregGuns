"""The icbm-first-missile guide sheet: from a missile chip to the first launch from the Galacticraft pad (T1 and T2).

The missile models are the shipped OBJ files (body plus warhead, the warhead seated on the body's nose exactly as the
game seats it), drawn with render3d.obj_scene, the strict Forge reader. The numbers are read from the I2 code
(MissileTier, MissileBuildSpec, EntityMissile, MTELaunchControl, IcbmConfig); the draft stamp is gone (D50).
Vanilla textures are the real Minecraft ones read from the jar at build time (D55). No Galacticraft texture is used
(its license is "explicit permission", so it can't be redistributed in a committed sheet): the pad is drawn with
vanilla iron blocks and named in the text.

Run on its own (needs no warhead dump):  python tools/asset-gen/infographics/pages_icbm_missile.py
"""

from __future__ import annotations

import hashlib
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import gt  # noqa: E402
import kit as K  # noqa: E402
import render3d as R  # noqa: E402
import scenes as S  # noqa: E402
import vanilla  # noqa: E402
from kit import chip, esc  # noqa: E402
from pages import MOJANG_CREDIT, gti, img, page_doc, slot  # noqa: E402

NTM = K.ASSETS / "models/thirdparty/ntm"
NTM_TEX = K.ASSETS / "textures/models/thirdparty/ntm"

# Numbers from the I2 code: MissileTier (fuel, range, speed, scatter), MissileBuildSpec (plates, fins, engine, caps).
TIERS = {
    1: dict(body="missile_body_t1", warhead="warhead_t1", volt="HV", fuel="RP-1 or Cetane-Boosted Diesel", fuel_mb=2000, rng=1000, speed="0.8",
            flight="about a minute", scatter=12, plates="4 stainless steel", fins="4 steel", engine="Missile Engine Mk I", cap=5),
    2: dict(body="missile_body_t2", warhead="warhead_t2", volt="EV", fuel="Dense Hydrazine", fuel_mb=4000, rng=3000, speed="1.2",
            flight="about two minutes", scatter=8, plates="6 titanium", fins="4 stainless steel", engine="Missile Engine Mk II", cap=20),
}
MIN_RANGE = 20

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
    """The stacked missile (body plus warhead) standing on a 3 x 3 launch pad, drawn with vanilla iron blocks."""
    t = TIERS[tier]
    files = [NTM / f"{t['body']}.obj", NTM_TEX / f"{t['body']}.png", NTM / f"{t['warhead']}.obj", NTM_TEX / f"{t['warhead']}.png"]
    iron = vanilla.texture("blocks/iron_block")

    def make():
        body = R.obj_scene(*files[:2])
        seat = body.bounds()[1][1]
        sc = R.voxel_scene({(x, 0, z): "pad" for x in range(3) for z in range(3)}, {"pad": iron})
        sc.extend(body, R.tr(1, 1, 1))
        sc.extend(R.obj_scene(*files[2:]), R.tr(1, 1 + seat, 1))
        return R.trim(R.render(sc, yaw=-30, pitch=14, ppu=ppu, ss=2, outline=K.RIM), 2)
    return K._cached(f"missile-pad-{tier}", dict(sig=_sig(*files, iron), u=ppu, v=1), make)


def block_icon(tag: str, tier: str, overlay: str | None) -> K.Path:
    """A GregTech single block: the tier hull, with a basic machine's overlay when one is named."""
    return S.machine_block(overlay, tier, greg=True) if overlay else K.voxels(
        f"hull-{tag}-{tier}", {(0, 0, 0): ("m", S.FRONT)}, {"m": (gt.hull(tier, "TOP"), gt.hull(tier, "SIDE"), gt.hull(tier, "SIDE"))},
        yaw=-38, pitch=28, ppu=150)


def _tier_card(n: int) -> str:
    t = TIERS[n]
    stats = [("Chip", f"{chip(t['volt'])} Circuit Assembler"), ("Body", f"{t['plates']} plates, {t['fins']} plate fins"),
             ("Fuel", f"{esc(t['fuel'])} or better"), ("Tank", f"{t['fuel_mb']:,} mB"), ("Range", f"{MIN_RANGE} to {t['rng']:,} blocks"),
             ("Flight", f"{t['flight']} at the longest range"), ("Warhead", f"up to {t['cap']} charges")]
    rows = "".join(f'<div class="stat"><span>{k}</span><b>{v}</b></div>' for k, v in stats)
    return f"""<div class="card"><div style="display:flex;align-items:center;gap:12px"><h2>Tier {n} missile</h2>{chip(t['volt'], t['volt'] if n == 1 else 'EV to IV')}</div>
<div class="spot">{img(missile_on_pad(n), h=520)}</div>{rows}</div>"""


def _step(n: int, title: str, vis: str, text: str) -> str:
    return f"""<div class="card sc"><div class="hd"><span class="num">{n}</span><h2>{esc(title)}</h2></div><div class="vis">{vis}</div><div class="t">{text}</div></div>"""


def first_missile() -> str:
    arrow = '<span class="arrow">&rarr;</span>'
    plus = '<span class="plus">+</span>'
    ca = img(block_icon("ca", "HV", "circuitassembler"), h=92)
    le = img(block_icon("le", "HV", "laser_engraver"), h=92)
    lc = img(block_icon("lc", "HV", None), h=92)
    plate = lambda m: gti("plate", m, 34)  # noqa: E731
    s1 = _step(1, "Make a missile chip",
               f"{ca}{arrow}<span class='pill'>Missile Chip T1 &middot; HV</span><span class='pill'>Missile Chip T2 &middot; EV</span>",
               "The chip comes from the GregTech <b>Circuit Assembler</b>, at the voltage of its tier: a circuit board, 2 circuits and fine wire. "
               "<b>T1</b> is an Advanced Plastic board, 2 Advanced circuits and gold wire at HV. <b>T2</b> is an Advanced Epoxy board, 2 Data circuits and electrum wire at EV.")
    s2 = _step(2, "Engrave the schematic",
               f"<span class='pill'>Missile Chip</span>{plus}<span class='pill'>Red / Blue lens</span>{arrow}{le}{arrow}<span class='pill'>Missile Schematic</span>",
               "Put the chip and a lens in the <b>Laser Engraver</b>: a Red lens for T1, a Blue lens for T2. The lens is not used up. "
               "Slot the schematic into the Galacticraft <b>NASA Workbench</b> and the page for that missile opens.")
    s3 = _step(3, "Build the body by hand",
               f"{plate('StainlessSteel')}{plate('Titanium')}{plus}<span class='pill'>4 fins</span>{plus}<span class='pill'>Engine</span>{plus}<span class='pill'>Guidance Computer</span>{arrow}<span class='pill'>Missile Body</span>",
               "On the NASA Workbench page: <b>body plates</b> (T1 four stainless steel, T2 six titanium), <b>four fins</b>, a <b>Missile Engine</b> and a "
               "<b>Guidance Computer</b>. A better computer than the body asks for makes the missile land closer. There is no warhead slot: a warhead goes on at the pad.")
    s3b = _step(4, "Or automate it",
                f"<span class='pill'>Scan a body</span>{arrow}<span class='pill'>Data Stick</span>{arrow}<span class='pill'>Assembly Line</span>{arrow}<span class='pill'>Body</span>{plus}<span class='pill'>Computer</span>{arrow}<span class='pill'>Assembler</span>",
                "Scan one hand-built body into a <b>Data Stick</b> in the Scanner (T1 takes a minute at HV, T2 two minutes at EV). The <b>Assembly Line</b> then builds that body "
                "from plates, fins, an engine, 2 circuits (Advanced for T1, Data for T2) and 250 L of lubricant, in 30 seconds (T1) or 45 seconds (T2). It makes the basic body; "
                "a better <b>Guidance Computer</b> goes in afterwards in the <b>Assembler</b> (15 seconds, at the body's voltage).")
    s4 = _step(5, "Stack it on the launch pad",
               f"{img(missile_on_pad(1, 100), h=150)}{img(missile_on_pad(2, 100), h=150)}",
               "Place the <b>Missile Body</b> on the middle of a Galacticraft <b>Launch Pad</b>, then set a <b>warhead</b> on its nose. It seats flush. "
               "<b>Sneak and right-click the body with an empty hand</b> to arm it. The stack becomes the flying missile; "
               "its model does not change.")
    s5 = _step(6, "Fuel it",
               f"<span class='pill'>Fuel Loader</span>{arrow}<span class='pill'>T1 &middot; 2,000 mB RP-1</span><span class='pill'>T2 &middot; 4,000 mB Dense Hydrazine</span>",
               "Attach a Galacticraft <b>Fuel Loader</b> to the pad and feed it. Each tier takes its own fuel <b>or better</b>: RP-1 or Cetane-Boosted Diesel for T1, Dense Hydrazine for T2. "
               "A closer target burns less, and <b>whatever fuel is left makes the blast bigger</b>, up to 25 percent.")
    s6 = _step(7, "Aim and fire",
               f"{lc}{arrow}<span class='pill'>X and Z</span><span class='pill'>Data stick</span><span class='pill'>Launch</span>",
               f"Put a <b>Launch Control</b> block (HV) within 4 blocks of the pad. Type the target <b>X and Z</b>, or load them from a <b>Data Stick</b>, and press <b>Launch</b>. The screen shows the range and the blast radius to expect. "
               f"The target must be at least <b>{MIN_RANGE} blocks</b> away. A <b>redstone pulse</b> or a machine-controller cover fires it too. One shot costs 300 EU.")
    flight = f"""<div class="card" style="margin-top:22px"><h2>Launch, flight and impact</h2>
<div class="tl"><div><div class="k">1 &middot; Warm-up</div><div class="v">Two seconds of countdown with the engines lit. Keep clear of the pad.</div></div>
<div><div class="k">2 &middot; Climb and arc</div><div class="v">It climbs, then flies a high arc. Tier 1 covers 0.8 blocks a tick, Tier 2 1.2.</div></div>
<div><div class="k">3 &middot; Scatter</div><div class="v">It never misses entirely, but it lands off target: up to {TIERS[1]['scatter']} blocks either way for T1 and {TIERS[2]['scatter']} for T2, more at long range, less with a better guidance computer.</div></div>
<div><div class="k">4 &middot; Impact</div><div class="v">It detonates on contact, never in the air. The warhead decides the blast; leftover fuel makes it bigger.</div></div>
<div><div class="k">5 &middot; The pad</div><div class="v">The pad, the fuel loader and the launch control are never harmed. Stack the next one.</div></div></div>
<div class="muted" style="font-size:16px;margin-top:12px">Only 20 missiles can be in the air on a server at once by default. Missiles load only the ground they are flying over.</div></div>"""
    body = f"""<div class="hero">{_tier_card(1)}{_tier_card(2)}</div>
<div class="steps">{s1}{s2}{s3}{s3b}{s4}{s5}{s6}<div class="card sc" style="justify-content:center"><div class="muted" style="font-size:20px;line-height:1.4"><b style="color:var(--txt)">Order of play.</b> Chip, schematic, body, pad, warhead, fuel, target, launch. The warhead is the expensive part: see the warhead sheet for what each one does.</div></div></div>
{flight}"""
    return page_doc("Your First Missile", "Tier 1 and Tier 2 missiles: from a chip to a launch from the Galacticraft launch pad.", body, extra=CSS,
                    credit=["Missile and warhead models and textures © HBM's Nuclear Tech Mod contributors, GNU LGPL v3; modified.",
                            "GregTech textures from GregTech 5 Unofficial, LGPL-3.0.", MOJANG_CREDIT])


PAGES = {"icbm-first-missile": first_missile}

if __name__ == "__main__":
    K.rasterize([(n, f(), 1600) for n, f in PAGES.items()])
