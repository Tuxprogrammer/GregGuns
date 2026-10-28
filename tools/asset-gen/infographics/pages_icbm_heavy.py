"""The icbm-warheads-heavy guide sheet: the Tier 3 blast types (exothermic, endothermic, nuclear, EMP, ender,
anti-gravitational), antimatter (T4), red matter (T5) and the launcher warheads. It is the second half of the complete
warhead guide; the first half is `icbm-warheads` (Tier 1 and 2, the fill curve, demolition use and the parts), split in two
because the whole list was too tall to read as one picture.

Numbers come from the warhead dump (radius, center damage, casings, launcher caps) and from the Java constants
(`_const`, `_cfg`), so a changed constant moves the sheet. Recipe inputs are printed in player words and checked against
the Java source by `_verify`, so a changed recipe stops the build.
"""

from __future__ import annotations

import math
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import kit as K  # noqa: E402
import pages_icbm as P  # noqa: E402
from kit import chip, esc  # noqa: E402
from pages import gti, img, page_doc, slot  # noqa: E402

JAVA = K.REPO / "src/main/java/com/gregguns"
NTM_ITEMS = K.ASSETS / "textures/items/thirdparty/ntm"


def _src(rel: str) -> str:
    return (JAVA / rel).read_text(encoding="utf-8")


def _const(rel: str, name: str) -> float:
    m = re.search(rf"\b{name}\s*=\s*(-?[0-9][0-9_]*(?:\.[0-9]+)?)[FfDdLl]?\s*;", _src(rel))
    if not m:
        raise SystemExit(f"{rel}: constant {name} not found; update pages_icbm_heavy.py")
    v = float(m.group(1).replace("_", ""))
    return int(v) if "." not in m.group(1) else v


def _cfg(name: str) -> int:
    m = re.search(rf"public static int {name}\s*=\s*([0-9]+);", _src("core/config/IcbmConfig.java"))
    if not m:
        raise SystemExit(f"IcbmConfig.{name} not found")
    return int(m.group(1))


ENDER = "icbm/warhead/effects/EnderRules.java"
ANTI = "icbm/warhead/effects/AntiGravEffect.java"
RED = "icbm/warhead/effects/RedMatterModel.java"

ENDER_SECONDS = _const(ENDER, "DURATION_TICKS") / 20
ENDER_RANGE = _const(ENDER, "TELEPORT_RANGE")
ENDER_MIN, ENDER_MAX = _const(ENDER, "TARGET_MIN"), _const(ENDER, "TARGET_SPREAD")
ENDER_SENT = _cfg("enderMaxTeleports")
LIFT_SECONDS = _const(ANTI, "LIFT_TICKS") / 20
LIFT_BLOCKS = _cfg("antigravMaxBlocks")
LIFE = _cfg("redMatterLifetimeSeconds")
IDLE = _const(RED, "IDLE_LINGER_TICKS") / 20
RED_START = _const(RED, "START_RADIUS")
ANTI_REACH = int(_const(RED, "ANTIMATTER_REACH"))

KILL = _const("explosion/Annihilation.java", "KILL_ZONE_SHARE")
LAUNCH = P.DUMP["launchers"]
LCH = P.DUMP["launcherCharges"]


def _verify() -> None:
    """The payload text printed below, checked against the recipe source."""
    spec = P._SPEC
    need = [
        "ENDER(WarheadBlast.ENDER, false, 5, plates(Tier.LUV, 8), Ing.industrialTnt(8), pe(3),",
        'Ing.gtItem("Emitter_LuV", 1, Tier.LUV, 10.0), Ing.gt("gem", "EnderEye", 16',
        "ANTIGRAV(WarheadBlast.ANTIGRAV, false, 6, plates(Tier.LUV, 8), Ing.industrialTnt(6), pe(2),",
        'Ing.gtItem("Field_Generator_LuV", 2, Tier.LUV, 12.0), Ing.gt("gem", "NetherStar", 4',
        "EMP(WarheadBlast.EMP, false, 4, plates(Tier.LUV, 8), Ing.industrialTnt(4), pe(2),",
        'Ing.gtItem("Field_Generator_LuV", 2, Tier.LUV, 12.0), Ing.gtItem("Emitter_LuV", 2',
        "NUCLEAR(WarheadBlast.NUCLEAR, false, 3, plates(Tier.LUV, 8), Ing.industrialTnt(12), pe(4),",
        'Ing.gt("ingot", "Uranium235", 8, Tier.HV, 6.0), Ing.gt("ingot", "Plutonium", 4',
        "EXOTHERMIC(WarheadBlast.EXOTHERMIC, false, 1, plates(Tier.LUV, 8), Ing.industrialTnt(16), pe(6),",
        "Ing.part(IcbmPart.NAPALM_CANISTER, 4), fuze3(), Ing.kerosene(8000)",
        "Ing.part(IcbmPart.DISPERSAL_CANISTER, 4), fuze3(), Ing.gas(\"Helium\", 8000",
        "ANTIMATTER(WarheadBlast.ANTIMATTER, false, 1, naquadria(8), Ing.industrialTnt(8), pe(4),",
        'Ing.gtItem("Field_Generator_UV", 2', 'Ing.gtItem("QuantumEye", 1', 'Ing.gt("ingot", "Plutonium241", 4',
        "REDMATTER(WarheadBlast.REDMATTER, false, 2, neutronium(8), Ing.industrialTnt(8), pe(4),",
        'Ing.gtItem("Field_Generator_UHV", 2', 'Ing.gtItem("QuantumEye", 2', 'Ing.gtItem("Emitter_UHV", 2',
        "fuze3()",
    ]
    missing = [n for n in need if n not in spec]
    if missing:
        raise SystemExit(f"WarheadRecipeSpec changed; update icbm-warheads-heavy: {missing}")


_verify()

# Machine tier per blast, from the dump (GT voltage names; the dump's antimatter and red matter say ZPM because the Tier enum
# stops there, so the real voltages UV and UHV are the recipes' own, D134).
MACHINE = {"exothermic": "LuV", "endothermic": "LuV", "nuclear": "ZPM", "emp": "LuV", "ender": "LuV", "antigrav": "LuV",
          "antimatter": "UV", "redmatter": "UHV"}

TIER_NAME = {"HV": "HV", "EV": "EV", "LUV": "LuV"}
PAY = {
    "exothermic": "16 Industrial TNT, 6 Plastic Explosive, 4 Napalm Canisters, 8,000 L kerosene",
    "endothermic": "16 Industrial TNT, 6 Plastic Explosive, 4 Dispersal Canisters, 8,000 L helium",
    "nuclear": "12 Industrial TNT, 4 Plastic Explosive, 8 uranium-235 ingots, 4 plutonium ingots",
    "emp": "4 Industrial TNT, 2 Plastic Explosive, 2 LuV Field Generators, 2 LuV Emitters",
    "ender": "8 Industrial TNT, 3 Plastic Explosive, a LuV Emitter, 16 Eyes of Ender",
    "antigrav": "6 Industrial TNT, 2 Plastic Explosive, 2 LuV Field Generators, 4 Nether Stars",
    "antimatter": "8 Industrial TNT, 4 Plastic Explosive, 2 UV Field Generators, a Quantum Eye, 4 plutonium-241 ingots",
    "redmatter": "8 Industrial TNT, 4 Plastic Explosive, 2 UHV Field Generators, 2 Quantum Eyes, 2 UHV Emitters",
}
CAT_COLOR = {"Crater": "#ff8a1f", "Annihilate": "#ff5555", "Utility": "#38c6d9", "Teleport": "#b88cff", "Lift": "#6fd36b"}

CSS = P.CSS + """
.hv-card { display: grid; grid-template-columns: 150px 1fr; gap: 14px; align-items: center; }
.hv-card .spot { height: 232px; display: flex; align-items: flex-end; justify-content: center; padding-bottom: 8px; }
.cas-card { display: grid; grid-template-columns: 170px 1fr; gap: 18px; align-items: center; }
.cas-card .spot { height: 300px; display: flex; align-items: flex-end; justify-content: center; padding-bottom: 12px; }
.mech { display: flex; flex-direction: column; gap: 9px; margin-top: 10px; }
.mech .row { display: flex; gap: 12px; align-items: flex-start; font-size: 17.5px; color: var(--muted); line-height: 1.3; }
.mech .row b { color: var(--txt); }
.mech .dot { flex: none; width: 12px; height: 12px; border-radius: 50%; margin-top: 7px; background: var(--kc); }
.lw { display: grid; grid-template-columns: 52px 1.6fr 1.15fr .8fr .7fr .7fr; gap: 10px; align-items: center; padding: 7px 4px;
  border-bottom: 1px solid var(--line); font-size: 17.5px; }
.lw.hd { color: var(--dim); font-size: 14px; letter-spacing: .1em; text-transform: uppercase; border-bottom: 2px solid var(--line); }
.lw b { font-weight: 700; }
.bt .pay span.chip { color: #0e1217; font-size: 14px; letter-spacing: 0; text-transform: none; margin: 0 4px 0 0; padding: 2px 9px 1px; }
.bt .pay span.t-EV, .bt .pay span.t-IV { color: #fff; }
.tl { position: relative; height: 34px; margin: 10px 0 4px; }
.tl .seg { position: absolute; top: 0; height: 30px; display: flex; align-items: center; padding: 0 10px; font-size: 15.5px; font-weight: 700; color: #11151a; white-space: nowrap; overflow: hidden; }
.tl .tk { position: absolute; top: 34px; font-size: 14.5px; color: var(--muted); transform: translateX(-50%); white-space: nowrap; }
"""


def tchip(t: str) -> str:
    """A tier chip; UV and UHV are past the base chip colors."""
    if t in K.TIERS:
        return chip(t)
    col = {"UV": ("#2ec4b6", "#0e1217"), "UHV": ("#ff9a3d", "#0e1217")}[t]
    return f'<span class="chip" style="background:{col[0]};color:{col[1]}">{t}</span>'


def _fuse(rad: float) -> int:
    return P.fuse_seconds(rad)


def _t3_card(bid, name, kind, what, size_html=None):
    rad, dmg = P.full(bid)
    mesh = "warhead_t3" if bid in ("ender", "antigrav") else None
    pic = img(P.warhead(f"warhead_{bid}", ppu=150, mesh=mesh), h=210)
    dmg_html = f'<span>Center <b>{dmg:,.0f}</b></span>' if dmg else "<span>No direct damage</span>"
    return f"""<div class="bt" style="--kc:{CAT_COLOR[kind]}"><div class="hv-card"><div class="spot">{pic}</div><div>
<div class="top"><h3>{esc(name)}</h3><span class="kind">{esc(kind)}</span></div>
<div class="nums"><span>Radius <b>{P._n(rad)}</b></span>{dmg_html}<span>Fuse <b>{_fuse(rad)} s</b></span></div>
<div class="what">{what}</div>
<div class="pay"><span>Payload</span> {esc(PAY[bid])}</div><div class="pay"><span>Made at</span> {tchip(MACHINE[bid])} in the Assembler</div></div></div></div>"""


def _casing(tier, label, tier_chip, mesh, tex, plate_html, cap, extra, blurb, ppu=150):
    pic = img(P.warhead(tex, ppu=ppu, mesh=mesh), h=290)
    stats = "".join(f'<div class="stat"><span>{esc(k)}</span><b>{v}</b></div>' for k, v in extra)
    return f"""<div class="card cas-card"><div class="spot">{pic}</div><div>
<div style="display:flex;align-items:center;gap:10px;margin-bottom:4px"><h2>{label}</h2>{tier_chip}</div>
<div class="muted" style="font-size:17px;margin-bottom:6px">{blurb}</div>
<div class="stat"><span>Casing plates</span><b style="white-space:nowrap">{plate_html}</b></div><div class="stat"><span>Fill cap</span><b>{cap} charges</b></div>
<div class="stat"><span>Size</span><b>1 &times; 1 &times; 2 blocks</b></div>{stats}</div></div>"""


def heavy() -> str:
    # ----- casings
    nuke_r, nuke_d = P.full("nuclear")
    kill = nuke_r * KILL
    t3 = _casing(3, "Tier 3", tchip("LuV") + " " + tchip("ZPM"), "warhead_t3", "warhead_t3", f'{gti("plate", "Tungstensteel", 28)} Tungsten steel',
                 P.CAP[3], [], "Two blocks tall, for the silo. Fill and radius work as on the smaller casings.")
    t4 = _casing(4, "Tier 4", tchip("UV"), "warhead_antimatter", "warhead_antimatter", f'{gti("plate", "Naquadria", 28)} Naquadria',
                 P.CAP[4], [], "Antimatter only. The Tier 3 missile body carries it.", ppu=190)
    t5 = _casing(5, "Tier 5", tchip("UHV"), "warhead_redmatter", "warhead_redmatter", f'{gti("plate", "Neutronium", 28)} Neutronium',
                 P.CAP[5], [], "Red matter only. The Tier 3 missile body carries it, too.", ppu=190)

    # ----- Tier 3 types
    cards = "".join([
        _t3_card("exothermic", "Exothermic T3 Warhead", "Crater", "A huge crater where everything in the radius burns for 5 seconds."),
        _t3_card("endothermic", "Endothermic T3 Warhead", "Crater", "A huge crater that leaves everything in it slowed (Slowness III) for 10 seconds."),
        _t3_card("nuclear", "Nuclear Warhead", "Annihilate",
                 f"A crater, and a <b>kill zone of {kill:.0f} blocks</b> (60% of the radius) where every living thing dies through armor, bosses included. Outside it {nuke_d:,.0f} damage fades to the edge. The ground it leaves is poisoned: see the fallout sheet."),
        _t3_card("emp", "EMP T3 Warhead", "Utility", "No crater and no damage. One EMP pulse centered on the blast: it drains machines in reach and can bring down missiles in flight (the detection sheet has the details)."),
        _t3_card("ender", "Ender T3 Warhead", "Teleport",
                 f"No crater. For {ENDER_SECONDS:.0f} seconds it pulls every living thing toward the center. Anything within {ENDER_RANGE:.0f} blocks of the middle is sent to <b>one landing spot</b> {ENDER_MIN:.0f} to {ENDER_MAX:.0f} blocks away, up to {ENDER_SENT} per blast. Bosses are pulled but never sent."),
        _t3_card("antigrav", "Anti-Gravitational T3 Warhead", "Lift",
                 f"No crater. For {LIFT_SECONDS:.0f} seconds it lifts plain blocks off the ground, top layer first (up to {LIFT_BLOCKS}), and floats the living things inside. Machines and chests stay put. The blocks drift, then fall and land again."),
    ])

    # ----- antimatter and red matter
    am_r, am_d = P.full("antimatter")
    rm_r, _ = P.full("redmatter")
    am_kill = am_r * KILL
    am = f"""<div class="card" style="--kc:#ff5555">
<div style="display:flex;align-items:center;gap:12px"><h2>Antimatter</h2>{tchip("UV")}<span class="muted" style="margin-left:auto;font-size:17px">Radius {P._n(am_r)} &middot; Fuse {_fuse(am_r)} s</span></div>
<div class="muted" style="font-size:18px;margin:6px 0 4px">The widest crater of any warhead: a sphere {P._n(am_r)} blocks wide at a full casing. Everything living inside the kill zone ({am_kill:.0f} blocks, 60% of the radius) dies, bosses included. Outside it the damage starts at {am_d:,.0f} and fades to the edge.</div>
<div class="mech">
<div class="row"><span class="dot"></span><span><b>It ends red matter.</b> A red matter ball within {ANTI_REACH} blocks is destroyed the moment antimatter goes off.</span></div>
<div class="row"><span class="dot"></span><span><b>No fallout.</b> Antimatter leaves the ground clean.</span></div>
<div class="row"><span class="dot"></span><span><b>Made at {tchip("UV")}</b> in the Assembler. Payload: {esc(PAY["antimatter"])}.</span></div></div></div>"""
    seg = lambda pct, col, text: f'<div class="seg" style="left:{pct[0]}%;width:{pct[1]}%;background:{col}">{text}</div>'
    tl = f"""<div class="tl">{seg((0, 52), "#ff8a8a", "Grows and eats")}{seg((52, 20), "#ffd23f", "Idles")}{seg((72, 28), "#6b7480", "Hard stop")}
</div><div class="muted" style="font-size:15px;margin:-6px 0 4px">Not to scale: how long it takes to grow depends on the server. After it stops growing it idles for {IDLE:.0f} seconds and ends; {LIFE} seconds is the most it can ever live.</div>"""
    rm = f"""<div class="card" style="--kc:#ff9a3d">
<div style="display:flex;align-items:center;gap:12px"><h2>Red Matter</h2>{tchip("UHV")}<span class="muted" style="margin-left:auto;font-size:17px">Radius {P._n(rm_r)} &middot; Fuse {_fuse(rm_r)} s</span></div>
<div class="muted" style="font-size:18px;margin:6px 0 4px">No crater at first. A dark ball starts at a radius of {RED_START:.0f} and grows one block at a time up to {P._n(rm_r)} at a full casing, eating the blocks it covers. Everything living inside it dies, bosses included, and loose things inside are removed. Things near it are pulled in.</div>
{tl}
<div class="mech">
<div class="row"><span class="dot"></span><span><b>It cannot run away.</b> A red matter ball lives {LIFE} seconds at most (a server setting) and ends {IDLE:.0f} seconds after it stops growing. It survives a save and a reload, then carries on.</span></div>
<div class="row"><span class="dot"></span><span><b>It leaves nothing behind:</b> no fallout. Liquids and bedrock are not eaten.</span></div>
<div class="row"><span class="dot"></span><span><b>Made at {tchip("UHV")}</b> in the Assembler. Payload: {esc(PAY["redmatter"])}.</span></div></div></div>"""

    # ----- launcher warheads
    lw_names = {"he": "High Explosive", "heat": "Heavy Slug", "incendiary": "Incendiary", "phosphorus": "Phosphorus",
                "demolition": "Demolition", "emp": "EMP", "grenade_he": "Grenade HE", "grenade_cluster": "Grenade Cluster"}
    fits = {"ROCKET": "Rocket Launcher, Grim Reaper", "GRENADE": "Grenade Launcher", "NUKE": "Nuke Launcher"}
    does = {"he": "Blast and shrapnel at the hit point", "heat": "Slows what it catches", "incendiary": "Sets the area on fire",
            "phosphorus": "Blast that sets the area alight", "demolition": "Bores a tunnel down through hard blocks",
            "emp": "An EMP pulse, no other damage", "grenade_he": "Blast at the hit point", "grenade_cluster": "A round burst with fire"}
    rows = ""
    for w in LAUNCH:
        r1, d1 = P.curve(w["blast"], 1)
        dmg = min(d1, w["cap"])
        dmg_s = f"{dmg:,.0f}" if dmg else "none"
        icon = slot(K.icon(NTM_ITEMS / f"launcher_warhead_{w['key']}.png", 64), 30, lw_names[w["key"]])
        rows += (f'<div class="lw"><span>{icon}</span><span><b>{esc(lw_names[w["key"]])}</b><br><span class="muted" style="font-size:15.5px">{esc(does[w["key"]])}</span></span>'
                 f'<span>{esc(fits[w["kind"]])}</span><span>{P._n(round(r1, 1))} blocks</span><span>{dmg_s}</span><span>{tchip(TIER_NAME[w["tier"]])}</span></div>')
    caps = {}
    for w in LAUNCH:
        caps[w["kind"]] = w["cap"]
    icons = "".join(slot(K.item(n, 64), 34, t) for n, t in [("rocket_launcher", "Rocket Launcher"), ("grim_reaper", "Grim Reaper"),
                                                          ("grenade_launcher", "Grenade Launcher"), ("nuke_launcher", "Nuke Launcher")])
    lp = f"""<div class="card"><div style="display:flex;align-items:center;gap:14px;flex-wrap:wrap"><h2>Launcher warheads</h2><span style="display:flex;gap:6px">{icons}</span></div>
<div class="muted" style="font-size:18px;margin:6px 0 10px">No new launcher: the guns you already have can carry a blast type. A <b>launcher warhead</b> gives {LCH["perItem"]} charges. Craft it with a matching launcher (shapeless, in the crafting grid) to load it, up to {LCH["max"]} charges. The same type adds more; a different type or a load past {LCH["max"]} is refused, so nothing is ever lost. Each shot still uses the launcher's own rocket or grenade, spends <b>one charge</b>, and goes off as a small blast instead of the normal one. It changes <b>what</b> the shot does, not how hard it hits: center damage is capped at <b>{LCH["capFactor"]:g} times the launcher's own damage</b> ({P._n(caps["ROCKET"])} Rocket Launcher and Grim Reaper, {P._n(caps["GRENADE"])} Grenade Launcher, {P._n(caps["NUKE"])} Nuke Launcher). The radius is always the smallest-fill radius. Walls only break if the server allows explosion damage.</div>
<div class="lw hd"><span></span><span>Warhead</span><span>Fits</span><span>Radius</span><span>Center</span><span>Made at</span></div>{rows}
<div class="muted" style="font-size:16px;margin-top:10px">Made in the Assembler. Gas, push, smoke and the Tier 2 specials are left out on purpose, and the Nuke Launcher keeps its own tactical nuke. Needs the missile add-on switched on; with it off a loaded launcher fires normally.</div></div>"""

    body = f"""<div class="grid" style="grid-template-columns:repeat(3,minmax(0,1fr))">{t3}{t4}{t5}</div>
<div class="muted" style="font-size:17px;margin:12px 0 0">All three fly only on a Tier 3 missile launched from the missile silo. Radius and center damage are at a full casing, before armor; a part-filled casing is a smaller blast, as on the other sheet.</div>
<div class="label" style="margin:26px 0 10px">Tier 3 blast types &middot; LuV to ZPM</div>
<div class="grid" style="grid-template-columns:repeat(2,minmax(0,1fr));gap:14px">{cards}</div>
<div class="label" style="margin:26px 0 10px">Tier 4 and Tier 5 &middot; everything in the zone dies</div>
<div class="grid" style="grid-template-columns:1fr 1.12fr;gap:18px">{am}{rm}</div>
<div style="margin-top:26px">{lp}</div>"""
    return page_doc("Heavy Warheads",
                    "Tier 3 to red matter, and the launcher warheads: what each does, how big the bang is, and how to make one. The Tier 1 and 2 warheads are on the Warheads sheet.",
                    body, extra=CSS,
                    credit=["Warhead models, textures and item icons © HBM's Nuclear Tech Mod contributors, GNU LGPL v3; modified.",
                            "Plate icons: GregTech 5 Unofficial textures, LGPL-3.0."])


PAGES = {"icbm-warheads-heavy": heavy}

if __name__ == "__main__":
    import build
    build.main(["icbm-warheads-heavy"])
