"""The ICBM module's player guide infographics. build.py adds PAGES to the others.

Warhead models are the shipped OBJ files, drawn with render3d.obj_scene (the strict Forge reader), so the sheet shows
what the game draws. A sheet marked DRAFT takes its numbers from the spec's plan and carries a visible stamp; the
final sheet reads them from the code and drops it (D50).
"""

from __future__ import annotations

import hashlib

import blocks as B
import gt
import kit as K
import render3d as R
import scenes as S
import vanilla
from kit import chip, esc
from pages import gti, img, page_doc, slot

NTM = K.ASSETS / "models/thirdparty/ntm"
NTM_TEX = K.ASSETS / "textures/models/thirdparty/ntm"
WARHEAD_PPU = 300


def warhead(name: str, yaw=35, pitch=14, ppu=WARHEAD_PPU) -> K.Path:
    """A shipped warhead model at ppu pixels per block, so warheads drawn side by side keep their true scale."""
    obj, tex = NTM / f"{name}.obj", NTM_TEX / f"{name}.png"
    sig = hashlib.sha1(obj.read_bytes() + tex.read_bytes()).hexdigest()[:10]

    def make():
        return R.trim(R.render(R.obj_scene(obj, tex), yaw=yaw, pitch=pitch, ppu=ppu * 2, ss=2, outline=K.RIM), 2)
    return K._cached(f"warhead-{name}", dict(sig=sig, y=yaw, p=pitch, u=ppu, v=1), make)


# ---------------------------------------------------------------- numbers (PLACEHOLDERS until the code lands)
# From the I1 spec sheets (warheads-and-fill, blast-types): full-casing radius R and center damage D per type, fill caps
# T1 5 and T2 20, radius = max(3, R * f^(1/3)) and center damage = D * f^(1/2) with f = fill / cap.

CAP = {1: 5, 2: 20}
MIN_RADIUS = 3


def radius(R_full: float, fill: int, cap: int) -> float:
    return max(MIN_RADIUS, R_full * (fill / cap) ** (1 / 3))


def center(D_full: float, fill: int, cap: int) -> float:
    return D_full * (fill / cap) ** 0.5


# (name, tier, kind, radius, center damage or None, what it does in player words)
BLASTS = [
    ("Condensed", 1, "Crater", 9, 1200, "The plain high explosive. A crater, and a lot of damage at the center."),
    ("Shrapnel", 1, "Crater", 10, 1200, "A crater that also sets everything in the radius on fire for 5 seconds."),
    ("Anvil", 1, "Crater", 8, 1200, "A crater that leaves everything caught in it with Slowness III for 10 seconds."),
    ("Incendiary", 1, "Fire", 12, None, "No crater. Everything in the radius burns for 10 seconds, and fire spreads across up to 400 solid-topped blocks."),
    ("Debilitation", 1, "Gas", 12, None, "A 30-second cloud: Slowness II, Weakness II and nausea."),
    ("Chemical", 1, "Gas", 12, None, "A 30-second cloud of Poison II."),
    ("Smoke", 1, "Gas", 10, None, "A 30-second marker cloud. Harmless, so use it to signal or screen."),
    ("Repulsive", 1, "Push", 12, None, "Throws everything outward, hardest at the center."),
    ("Attractive", 1, "Push", 12, None, "Drags everything inward toward the center."),
    ("Fragmentation", 2, "Crater", 14, 3000, "A round crater that sets everything in the radius on fire for 5 seconds."),
    ("Thermobaric", 2, "Crater", 20, 3000, "The widest conventional crater. Nothing extra, just a huge, round hole."),
    ("Contagious", 2, "Gas", 14, None, "A 30-second cloud: Poison IV and Hunger II."),
    ("Sonic", 2, "Sound", 14, None, "Nausea for 10 seconds and Slowness III for 5, and every glass block in the radius shatters."),
    ("Breaching", 2, "Tunnel", 12, None, "Bores a 3 by 3 tunnel 24 blocks straight down, through anything of hardness 50 or less."),
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
.bt .what { color: var(--muted); font-size: 16.5px; line-height: 1.3; }
.fillrow { display: grid; grid-template-columns: 74px 1fr 112px 112px; gap: 12px; align-items: center; font-size: 17px; padding: 3px 0; }
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


def _implosion_compressor() -> K.Path:
    """The Implosion Compressor controller: GregTech's solid steel casing with its front overlay (a multiblock)."""
    casing = gt.iconset("MACHINE_CASING_SOLID_STEEL")
    front = B.composite(casing, gt.iconset("OVERLAY_FRONT_IMPLOSION_COMPRESSOR"), gt.iconset("OVERLAY_FRONT_IMPLOSION_COMPRESSOR_GLOW"),
                        cache=K.TEXCACHE, name="implosion_compressor_ctrl")
    top = B.composite(casing, cache=K.TEXCACHE, name="implosion_compressor_top")
    return K.voxels("implosion-compressor", {(0, 0, 0): ("m", S.FRONT)}, {"m": (top, casing, front)}, yaw=-38, pitch=28, ppu=150,
                    key="implosion-compressor-v1")


def _fill_table(label, tier, R_full, D_full, color) -> str:
    cap = CAP[tier]
    fills = {1: (1, 3, 5), 2: (1, 5, 10, 20)}[tier]
    rows = "".join(
        f'<div class="fillrow" style="--bc:{color}"><span>{f} of {cap}</span><div class="bar"><i style="width:{round(100 * radius(R_full, f, cap) / R_full)}%"></i></div>'
        f'<b>{radius(R_full, f, cap):.1f} blocks</b><b>{center(D_full, f, cap):,.0f} dmg</b></div>'
        for f in fills)
    return (f'<div class="label" style="margin-bottom:6px">{esc(label)}</div>'
            f'<div class="fillrow"><span class="hd">Fill</span><span class="hd">Radius vs full</span><span class="hd" style="text-align:right">Radius</span>'
            f'<span class="hd" style="text-align:right">Center</span></div>{rows}')


def _tier_card(tier, chip_tier, chip_text, plate, plate_name, model, cap, blurb, extra) -> str:
    stats = "".join(f'<div class="stat"><span>{esc(k)}</span><b>{v}</b></div>' for k, v in extra)
    return f"""<div class="card tier-card">
<div class="spot">{img(warhead(model), h=300)}</div>
<div><div style="display:flex;align-items:center;gap:12px;margin-bottom:6px"><h2>Tier {tier}</h2>{chip(chip_tier, chip_text)}</div>
<div class="muted" style="font-size:18px;margin-bottom:8px">{blurb}</div>
<div class="stat"><span>Casing</span><b>{gti("plate", plate, 28)} {esc(plate_name)} plates</b></div>
<div class="stat"><span>Fill cap</span><b>{cap} charges</b></div>
{stats}</div></div>"""


def _blast_card(name, tier, kind, rad, dmg, what) -> str:
    dmg_html = f'<span>Center <b>{dmg:,}</b></span>' if dmg else '<span>No direct damage</span>'
    return f"""<div class="bt" style="--kc:{KIND_COLOR[kind]}"><div class="top"><h3>{esc(name)}</h3><span class="kind">{esc(kind)}</span></div>
<div class="nums"><span>Radius <b>{rad}</b></span>{dmg_html}</div><div class="what">{esc(what)}</div></div>"""


def warheads() -> str:
    t1 = _tier_card(1, "HV", "HV", "StainlessSteel", "Stainless steel", "warhead_t1", 5,
                    "The starter casing: small, cheap and bounded. Takes up one block.",
                    [("Full-fill center damage", "1,200"), ("Blast radius, full", "8 to 12 blocks")])
    t2 = _tier_card(2, "EV", "EV to IV", "Titanium", "Titanium", "warhead_t2", 20,
                    "A heavy casing that holds four times the charge. Still one block.",
                    [("Full-fill center damage", "3,000"), ("Blast radius, full", "12 to 20 blocks")])

    t1_cards = "".join(_blast_card(*b) for b in BLASTS if b[1] == 1)
    t2_cards = "".join(_blast_card(*b) for b in BLASTS if b[1] == 2)

    fill = f"""<div class="card"><h2>Fill sets the size</h2>
<div class="muted" style="font-size:18px;margin:6px 0 12px">A warhead is filled up to its casing's cap. A part-filled casing is a smaller bang: the radius grows slower than the fill, so even a few charges make a useful blast.</div>
{_fill_table("Tier 1 casing, Condensed", 1, 9, 1200, "var(--hv)")}
<div style="height:12px"></div>
{_fill_table("Tier 2 casing, Thermobaric", 2, 20, 3000, "var(--ev)")}
<div class="muted" style="font-size:16px;margin-top:10px">Bars show the radius against a full casing. The radius never drops below 3 blocks.</div></div>"""

    falloff = """<div class="card"><h2>Damage fades to the edge</h2>
<div class="muted" style="font-size:18px;margin:6px 0 4px">Anything at the center takes the full hit. The damage drops as you move out and reaches zero at the edge of the blast. Armor still applies.</div>
<svg viewBox="0 0 560 190" style="width:100%;margin-top:6px">
<defs><linearGradient id="fg" x1="0" x2="0" y1="0" y2="1"><stop offset="0" stop-color="#ff8a1f" stop-opacity=".55"/><stop offset="1" stop-color="#ff8a1f" stop-opacity="0"/></linearGradient></defs>
<path d="M60 30 C 190 40 330 110 500 160 L500 160 L60 160 Z" fill="url(#fg)"/>
<path d="M60 30 C 190 40 330 110 500 160" fill="none" stroke="#ffb35c" stroke-width="4"/>
<line x1="60" y1="160" x2="520" y2="160" stroke="#97a3b1" stroke-width="2"/><line x1="60" y1="20" x2="60" y2="160" stroke="#97a3b1" stroke-width="2"/>
<circle cx="60" cy="30" r="7" fill="#ffd23f"/><circle cx="500" cy="160" r="7" fill="#ffd23f"/>
<text x="74" y="26" fill="#e9edf2" font-size="18" font-family="Segoe UI, Fira Sans, sans-serif">Full damage at the center</text>
<text x="510" y="96" fill="#e9edf2" font-size="18" text-anchor="end" font-family="Segoe UI, Fira Sans, sans-serif">Zero at the edge</text>
<text x="60" y="184" fill="#b4bfcb" font-size="16" font-family="Segoe UI, Fira Sans, sans-serif">Ground zero</text>
<text x="520" y="184" fill="#b4bfcb" font-size="16" text-anchor="end" font-family="Segoe UI, Fira Sans, sans-serif">Blast radius</text></svg></div>"""

    flint = slot(vanilla.icon("flint_and_steel"), 30, "Flint and Steel")
    tnt = slot(vanilla.icon("tnt"), 28, "TNT")
    demo = f"""<div class="card"><h2>Use one as a demolition charge</h2>
<div class="muted" style="font-size:18px;margin:6px 0 14px">A warhead on its own is a charge. Set it down where you want the hole.</div>
<div style="display:flex;flex-direction:column;gap:14px">
<div class="step"><span class="num">1</span><div class="t"><b>Place it</b> on the ground or against a wall. It stands nose up in one block.</div></div>
<div class="step"><span class="num">2</span><div class="t"><b>Light it.</b> Right-click with {flint} Flint and Steel or a Fire Charge, or send it a <b>redstone pulse</b>. A bare-handed click does nothing, so nobody trips it by accident.</div></div>
<div class="step"><span class="num">3</span><div class="t"><b>Run.</b> The fuse is 5 seconds, and the warhead beeps once a second. Break it while it burns and it defuses and drops.</div></div>
<div class="step"><span class="num">4</span><div class="t"><b>Careful.</b> Another explosion nearby sets it off with a half-second fuse.</div></div></div>
<div class="muted" style="font-size:16px;margin-top:12px">On a missile body the same warhead flies instead; the blast is the same.</div></div>"""

    asm_ico = img(S.machine_block("assembler", "HV", greg=True), h=104)
    chem_ico = img(S.machine_block("chemical_reactor", "HV", greg=True), h=104)
    imp_ico = img(_implosion_compressor(), h=104)
    ss, ti = gti("plate", "StainlessSteel", 28), gti("plate", "Titanium", 28)
    make = f"""<div class="card"><h2>How warheads are made</h2>
<div class="muted" style="font-size:18px;margin:6px 0 12px">All on GregTech machines, each recipe gated by its machine's voltage. A recipe makes a full-casing warhead; one cheap starter recipe makes a fill of 1.</div>
<div style="display:flex;flex-direction:column;gap:12px">
<div class="mrow"><div class="spot" style="display:flex;justify-content:center">{asm_ico}</div><div><h3>Assembler {chip("HV")}</h3><div class="muted" style="font-size:17px">Condensed, Shrapnel, Anvil, Repulsive and Attractive: {ss} 2 stainless steel plates, {tnt} 8 TNT, the payload and 1,000 L of glyceryl trinitrate. The starter charge (fill 1) is just 2 plates and 2 TNT.</div></div></div>
<div class="mrow"><div class="spot" style="display:flex;justify-content:center">{chem_ico}</div><div><h3>Chemical Reactor {chip("HV")} {chip("EV")}</h3><div class="muted" style="font-size:17px">HV: Incendiary, Debilitation, Chemical and Smoke, from {ss} 2 stainless steel plates, the payload and a reagent. EV: Contagious, from {ti} 4 titanium plates, the payload and sulfuric acid.</div></div></div>
<div class="mrow"><div class="spot" style="display:flex;justify-content:center">{imp_ico}</div><div><h3>Implosion Compressor {chip("EV")} <span class="label" style="color:var(--orange)">multiblock</span></h3><div class="muted" style="font-size:17px">Fragmentation, Sonic, Breaching and Thermobaric: {ti} 4 titanium plates, the payload and 16 units of explosive additive.</div></div></div></div></div>"""

    ladder = "".join(
        f'<span class="{"" if on else "off"}">{chip(t) if t in K.TIERS else f"<span class=chip style=background:#8a8f98>{t}</span>"}</span>'
        for t, on in [("HV", True), ("EV", True), ("IV", True), ("LuV", False), ("ZPM", False), ("UV", False), ("UHV", False)])
    later = f"""<div class="card"><div style="display:flex;align-items:center;gap:22px;flex-wrap:wrap"><h2>Bigger warheads sit higher on the ladder</h2><div class="ladder">{ladder}</div></div>
<div class="muted" style="font-size:18px;margin-top:8px">Tiers 1 and 2 are the conventional starter casings. Tier 3 (LuV to ZPM) is a two-block-tall warhead that holds up to 64 charges. Nuclear, antimatter and red matter warheads sit at the top of the ladder, and everything in their blast zone dies.</div></div>"""

    body = f"""<div class="grid" style="grid-template-columns:1fr 1fr">{t1}{t2}</div>
<div class="label" style="margin:26px 0 10px">Tier 1 blast types &middot; HV casing</div>
<div class="grid" style="grid-template-columns:repeat(3,minmax(0,1fr));gap:14px">{t1_cards}</div>
<div class="label" style="margin:22px 0 10px">Tier 2 blast types &middot; EV to IV casing</div>
<div class="grid" style="grid-template-columns:repeat(3,minmax(0,1fr));gap:14px">{t2_cards}</div>
<div class="muted" style="font-size:16px;margin-top:10px">Radius is in blocks at a full casing. Center damage is before armor.</div>
<div class="grid" style="grid-template-columns:1.2fr 1fr;margin-top:26px">{fill}{falloff}</div>
<div class="grid" style="grid-template-columns:1fr 1.15fr;margin-top:18px">{demo}{make}</div>
<div style="margin-top:18px">{later}</div>"""
    return page_doc("Warheads", "Tier 1 and Tier 2: what each one does, how big the bang is, and how to make and set one off.", body,
                    extra=CSS, overlay='<div class="ribbon">Draft: numbers pending</div>',
                    credit="Warhead models and textures © HBM's Nuclear Tech Mod contributors, GNU LGPL v3; modified.")


PAGES = {"icbm-warheads": warheads}
