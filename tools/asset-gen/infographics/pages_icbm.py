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
_EFFECTS = (_JAVA / "effects/Effects.java").read_text(encoding="utf-8")


def _verify_sources() -> None:
    """The recipe inputs and effect durations the sheet prints are not in the dump; each is checked against the Java source
    here, so a change to the recipe or the effect fails the build and the sheet is fixed with it."""
    need = [
        (_RECIPES, "TNT_FULL = 8"), (_RECIPES, "TNT_STARTER = 2"), (_RECIPES, "t2 ? \"Titanium\" : \"StainlessSteel\", t2 ? 4 : 2"),
        (_RECIPES, "Materials.Glyceryl.getFluid(1000)"), (_RECIPES, "ADDITIVE_AMOUNT, 16"), (_RECIPES, "plates(\"StainlessSteel\", 2)"),
        (_RECIPES, "RecipeMaps.assemblerRecipes"), (_RECIPES, "RecipeMaps.chemicalReactorRecipes"),
        (_RECIPES, "RecipeMaps.implosionRecipes"),
        (_RECIPES, "OrePrefixes.bolt, Materials.Steel, blast == WarheadBlast.SHRAPNEL ? 16 : 32"),
        (_RECIPES, "new ItemStack(Blocks.anvil)"), (_RECIPES, "Items.blaze_powder, 8"), (_RECIPES, "Items.spider_eye, 4"),
        (_RECIPES, "Items.fermented_spider_eye, 4"), (_RECIPES, "Items.poisonous_potato, 8"), (_RECIPES, "Items.coal, 8, 1"),
        (_RECIPES, "Items.slime_ball, 8"), (_RECIPES, "Items.ender_pearl, 4"), (_RECIPES, "OrePrefixes.plate, Materials.Brass, 4"),
        (_RECIPES, "OrePrefixes.stick, Materials.TungstenSteel, 4"), (_RECIPES, "OrePrefixes.dust, Materials.Gunpowder, 8"),
        (_RECIPES, "Materials.Oil.getFluid(1000)"), (_RECIPES, "Materials.SulfuricAcid.getFluid(1000)"),
        (_RECIPES, "Materials.SulfuricAcid.getFluid(500)"),
        (_EFFECTS, "new Burn(5, 0, 0)"), (_EFFECTS, "new Burn(0, 200, 2)"), (_EFFECTS, "e.setFire(10)"),
        (_EFFECTS, "Potion.moveSlowdown.id, 60, 1"), (_EFFECTS, "Potion.weakness.id, 60, 1"), (_EFFECTS, "Potion.confusion.id, 80, 0"),
        (_EFFECTS, "Potion.poison.id, 100, 1"), (_EFFECTS, "Potion.poison.id, 120, 3"), (_EFFECTS, "Potion.hunger.id, 120, 1"),
        (_EFFECTS, "Potion.confusion.id, 200, 0"), (_EFFECTS, "Potion.moveSlowdown.id, 100, 2"),
    ]
    missing = [snippet for src, snippet in need if snippet not in src]
    if missing:
        raise SystemExit(f"WarheadRecipes/Effects changed; update icbm-warheads and _verify_sources: {missing}")


_verify_sources()


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


# The payload, the one input that makes each type itself (WarheadRecipes.payload), and the reagent when the type is made in
# the Chemical Reactor. In player words.
PAYLOAD = {
    "condensed": "8 gunpowder", "shrapnel": "16 steel bolts", "anvil": "1 anvil", "repulsive": "8 slimeballs",
    "attractive": "4 ender pearls", "incendiary": "8 blaze powder, 1,000 L oil", "debilitation": "4 spider eyes, 500 L sulfuric acid",
    "chemical": "4 fermented spider eyes, 500 L sulfuric acid", "smoke": "8 charcoal, 500 L sulfuric acid",
    "fragmentation": "32 steel bolts", "sonic": "4 brass plates", "breaching": "4 tungsten steel rods",
    "thermobaric": "8 gunpowder", "contagious": "8 poisonous potatoes, 1,000 L sulfuric acid",
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


def _implosion_compressor() -> K.Path:
    """The Implosion Compressor controller: GregTech's solid steel casing with its front overlay (a multiblock)."""
    casing = gt.iconset("MACHINE_CASING_SOLID_STEEL")
    front = B.composite(casing, gt.iconset("OVERLAY_FRONT_IMPLOSION_COMPRESSOR"), gt.iconset("OVERLAY_FRONT_IMPLOSION_COMPRESSOR_GLOW"),
                        cache=K.TEXCACHE, name="implosion_compressor_ctrl")
    top = B.composite(casing, cache=K.TEXCACHE, name="implosion_compressor_top")
    # Keyed by the texture pixels, so a render made before the overlay was composited can never be served again.
    sig = hashlib.sha1(b"".join(K.Path(t).read_bytes() for t in (top, casing, front))).hexdigest()[:10]
    return K.voxels("implosion-compressor", {(0, 0, 0): ("m", S.FRONT)}, {"m": (top, casing, front)}, yaw=-38, pitch=28, ppu=150,
                    key=f"implosion-compressor-v2-{sig}")


def _tnt_icon() -> K.Path:
    """TNT as the game draws it in an inventory: a small cube of the real vanilla textures (D55)."""
    top, side = vanilla.real("blocks/tnt_top"), vanilla.real("blocks/tnt_side")
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
<div class="spot">{img(warhead(model), h=300)}</div>
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
    return f"""<div class="bt" style="--kc:{KIND_COLOR[kind]}"><div class="top"><h3>{esc(name)}</h3><span class="kind">{esc(kind)}</span></div>
<div class="nums">{size}{dmg_html}{fuse}</div><div class="what">{esc(what)}</div><div class="pay"><span>Payload</span> {esc(PAYLOAD[blast_id])}</div></div>"""


def warheads() -> str:
    def span(tier):  # the radius range of the types that blast a radius; breaching bores a tunnel instead
        radii = [full(b["id"])[0] for b in DUMP["blasts"] if b["tier"] == f"T{tier}" and b["id"] != "breaching"]
        return f"{_n(min(radii))} to {_n(max(radii))} blocks"

    def top_damage(tier):
        return max(full(b["id"])[1] for b in DUMP["blasts"] if b["tier"] == f"T{tier}")
    t1 = _tier_card(1, "HV", "HV", "StainlessSteel", "Stainless steel", "warhead_t1", CAP[1],
                    "The starter casing: small, cheap and bounded. Takes up one block.",
                    [("Full-fill center damage", f"{top_damage(1):,.0f}"), ("Blast radius, full", span(1))])
    t2 = _tier_card(2, "EV", "EV to IV", "Titanium", "Titanium", "warhead_t2", CAP[2],
                    f"A heavy casing that holds {CAP[2] // CAP[1]} times the charge. Still one block.",
                    [("Full-fill center damage", f"{top_damage(2):,.0f}"), ("Blast radius, full", span(2))])

    tier_of = {b[1]: BLAST[b[1]]["tier"] for b in BLASTS}
    t1_cards = "".join(_blast_card(*b) for b in BLASTS if tier_of[b[1]] == "T1")
    t2_cards = "".join(_blast_card(*b) for b in BLASTS if tier_of[b[1]] == "T2")

    fill = f"""<div class="card"><h2>Fill sets the size</h2>
<div class="muted" style="font-size:18px;margin:6px 0 12px">A warhead is filled up to its casing's cap. A part-filled casing is a smaller bang: the radius grows slower than the fill, so even a few charges make a useful blast.</div>
{_fill_table("Tier 1 casing, Condensed", "condensed", "var(--hv)")}
<div style="height:12px"></div>
{_fill_table("Tier 2 casing, Thermobaric", "thermobaric", "var(--ev)")}
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

    flint = slot(vanilla.real("items/flint_and_steel"), 30, "Flint and Steel")
    charge = slot(vanilla.real("items/fireball", "fire_charge"), 30, "Fire Charge")
    tnt = slot(_tnt_icon(), 30, "TNT")
    demo = f"""<div class="card"><h2>Use one as a demolition charge</h2>
<div class="muted" style="font-size:18px;margin:6px 0 14px">A warhead on its own is a charge. Set it down where you want the hole.</div>
<div style="display:flex;flex-direction:column;gap:14px">
<div class="step"><span class="num">1</span><div class="t"><b>Place it</b> on the ground or against a wall. It stands nose up in one block.</div></div>
<div class="step"><span class="num">2</span><div class="t"><b>Light it.</b> Right-click with {flint} Flint and Steel or a {charge} Fire Charge, or send it a <b>redstone pulse</b>. A bare-handed click does nothing, so nobody trips it by accident.</div></div>
<div class="step"><span class="num">3</span><div class="t"><b>Run.</b> The fuse is <b>3 seconds plus 1 second for every 3 blocks of blast radius</b>, rounded up: {fuse_seconds(full('condensed')[0])} seconds for a full Condensed warhead, {fuse_seconds(full('thermobaric')[0])} for a full Thermobaric. A smaller fill burns shorter. It beeps once a second. Break it while it burns and it defuses and drops.</div></div>
<div class="step"><span class="num">4</span><div class="t"><b>Careful.</b> Another explosion nearby sets it off with a {CHAIN_SECONDS:g}-second fuse, so keep charges apart.</div></div></div>
<div class="muted" style="font-size:16px;margin-top:12px">On a missile body the same warhead flies instead; the blast is the same.</div></div>"""

    asm_ico = img(S.machine_block("assembler", "HV", greg=True), h=104)
    chem_ico = img(S.machine_block("chemical_reactor", "HV", greg=True), h=104)
    imp_ico = img(_implosion_compressor(), h=104)
    ss, ti = gti("plate", "StainlessSteel", 28), gti("plate", "Titanium", 28)
    make = f"""<div class="card"><h2>How warheads are made</h2>
<div class="muted" style="font-size:18px;margin:6px 0 12px">All on GregTech machines, each recipe gated by its machine's voltage. A recipe makes a full-casing warhead; one cheap starter recipe makes a fill of 1.</div>
<div style="display:flex;flex-direction:column;gap:12px">
<div class="mrow"><div class="spot" style="display:flex;justify-content:center">{asm_ico}</div><div><h3>Assembler {chip("HV")}</h3><div class="muted" style="font-size:17px">Condensed, Shrapnel, Anvil, Repulsive and Attractive: {ss} 2 stainless steel plates, {tnt} 8 TNT, the payload and 1,000 L of glyceryl trinitrate. The starter charge (fill 1) is just 2 plates and 2 TNT.</div></div></div>
<div class="mrow"><div class="spot" style="display:flex;justify-content:center">{chem_ico}</div><div><h3>Chemical Reactor {chip("HV")} {chip("EV")}</h3><div class="muted" style="font-size:17px">HV: Incendiary, Debilitation, Chemical and Smoke, from {ss} 2 stainless steel plates, the payload and a fluid (oil for Incendiary, sulfuric acid for the rest). EV: Contagious, from {ti} 4 titanium plates, 8 poisonous potatoes and 1,000 L of sulfuric acid.</div></div></div>
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
                    extra=CSS,
                    credit=["Warhead models and textures \u00a9 HBM's Nuclear Tech Mod contributors, GNU LGPL v3; modified.", vanilla.MC_CREDIT])


PAGES = {"icbm-warheads": warheads}
