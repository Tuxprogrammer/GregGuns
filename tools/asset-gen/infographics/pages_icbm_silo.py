"""The icbm-missile-silo guide sheet: the Missile Silo multiblock, layer by layer, with a T3 missile standing in it.

The silo is built from GregTech's Light Concrete art (walls), GregGuns' Gantry Beam art (rim) and Tinkers' Clear Glass, the hatches from GregTech's
overlays, and the missile from the shipped NTM OBJ models seated as the game seats them. The cells come from the
`SiloShape` strings when the silo coder's class is present; until then (and for the DRAFT stamp) they are generated
from the layout in the silo sheet: a 5 x 5 plan with the four corners cut off, a 3 x 3 open shaft, a 21-cell floor,
ten wall layers, and a ring of Gantry Beams on top.

Run on its own:  python tools/asset-gen/infographics/pages_icbm_silo.py
"""

from __future__ import annotations

import hashlib
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import blocks as B  # noqa: E402
import kit as K  # noqa: E402
import gt  # noqa: E402
import render3d as R  # noqa: E402
import scenes as S  # noqa: E402
import thirdparty as TP  # noqa: E402
from kit import chip, esc  # noqa: E402
from pages import MOJANG_CREDIT, gti, img, page_doc  # noqa: E402

NTM = K.ASSETS / "models/thirdparty/ntm"
NTM_TEX = K.ASSETS / "textures/models/thirdparty/ntm"
FRONT, BACK, LEFT, RIGHT = S.FRONT, S.BACK, S.LEFT, S.RIGHT
HEIGHT = 12            # layers: y0 floor .. y11 rim
SHAFT = range(1, 4)    # the open 3 x 3 shaft, x and z


def plan_cells() -> list[tuple[int, int]]:
    """The 5 x 5 plan with its four corner columns cut off."""
    return [(x, z) for x in range(5) for z in range(5) if (x, z) not in ((0, 0), (4, 0), (0, 4), (4, 4))]


def silo_cells() -> dict:
    """{(x, y, z): char}: C casing, P launch pad, G glass, B beam, ~ the controller. Floor y0 is a casing ring around the 3 x 3 pad; y1..y3 are glass rings (the controller
    replaces the middle of the front one at y2); y4..y10 are casing rings; y11 is the rim. Matches SiloShape.PIECES."""
    cells = {}
    for x, z in plan_cells():
        in_shaft = x in SHAFT and z in SHAFT
        cells[(x, 0, z)] = "P" if in_shaft else "C"
        if in_shaft:
            continue
        for y in range(1, 11):
            cells[(x, y, z)] = "G" if y <= 3 else "C"
        cells[(x, 11, z)] = "B"
    cells[(2, 2, 0)] = "~"
    return cells


# Where the sheet shows each hatch: in the glass rings or the floor's outer ring.
HATCHES = [((0, 1, 2), "input_bus", LEFT), ((4, 1, 2), "input_hatch", RIGHT), ((2, 0, 4), "energy_hatch", BACK),
           ((3, 1, 0), "maintenance_hatch", FRONT)]


def silo_build() -> S.Build:
    b = S.Build("silo")
    gc = S._casing("gantry_casing")
    b.define("casing", "Light Concrete", gt.iconset("CONCRETE_LIGHT_STONE"), "walls and the floor ring; Light or Dark Concrete, any finish")
    b.define("beam", "Gantry Beam", S._casing("gantry_beam"), "the rim")
    b.define("glass", "Clear Glass", TP.TCONSTRUCT_GLASS, "Tinkers' Clear Glass or EnderIO Quite Clear Glass")
    b.define("ctrl", "Missile Silo", (gc, gc, gc), "controller, middle of the front wall")
    b.put(silo_cells(), {"C": "casing", "G": "glass", "B": "beam", "~": ("ctrl", FRONT)})
    for pos, kind, facing in HATCHES:
        b.hatch(pos, kind, facing, gc)
    return b


def missile_scene():
    """The T3 body standing on the formed pad with the T3 warhead seated on its nose (y = 6), centered in the shaft."""
    body = R.obj_scene(NTM / "missile_body_t3.obj", NTM_TEX / "missile_body_t3.png")
    head = R.obj_scene(NTM / "warhead_t3.obj", NTM_TEX / "warhead_t3.png")
    sc = R.Scene()
    sc.extend(body, R.tr(2 - 0.5 + 0, TP.PAD_HEIGHT, 2 - 0.5))
    sc.extend(head, R.tr(2 - 0.5 + 0, TP.PAD_HEIGHT + 6, 2 - 0.5))
    return sc


SURFACE = 11   # the top layer of the silo (y = 11) is level with the ground: grass at y = 11, the rim just under its top face
SAND = {(x, z) for x in range(5, 9) for z in range(-4, -1)} | {(x, z) for x in range(-4, -1) for z in range(5, 8)}
TREES = [(6, 6), (-3, 3), (7, 0)]


def ground_cells(extent=range(-4, 9), bottom=-3) -> dict:
    """The earth the silo is dug into, as worldgen leaves it: grass on dirt on stone, a sand patch or two, the silo's
    rim level with the grass, and a few oaks standing around it."""
    own = set(plan_cells())
    cells = {}
    for x in extent:
        for z in extent:
            for y in range(bottom, SURFACE + 1):
                if y >= 0 and (x, z) in own:
                    continue
                if (x, z) in SAND and y >= SURFACE - 2:
                    cells[(x, y, z)] = "sand"
                elif y == SURFACE:
                    cells[(x, y, z)] = "grass"
                else:
                    cells[(x, y, z)] = "dirt" if y >= SURFACE - 3 else "stone"
    for x, z in TREES:
        if (x, z) not in SAND:
            cells.update(TP.tree(x, z, SURFACE + 1))
    return cells


def render(b: S.Build, yaw=-35, pitch=28, ppu=34, cut=None, missile=True, marks=True, ground=False, bottom=-3) -> Path:
    files = [NTM / "missile_body_t3.obj", NTM / "warhead_t3.obj", TP.GC_PAD]
    tex_sig = hashlib.sha1(b"".join(f.read_bytes() for f in files)).hexdigest()[:8]
    bl = dict(b.blocks)
    tex = dict(b.tex)
    if ground:
        bl.update(ground_cells(bottom=bottom))
        tex.update(TP.terrain_textures())
    if cut is not None:
        bl = {p: v for p, v in bl.items() if not cut(*p)}

    def make():
        tags = {k: (2 if k == "ctrl" else 1) for k in tex if k.startswith("h_") or k == "ctrl"} if marks else None
        scene = R.voxel_scene(bl, tex, tags)
        scene.extend(TP.pad_formed(1, 1))
        if missile:
            scene.extend(missile_scene())
        return R.trim(R.render(scene, yaw=yaw, pitch=pitch, ppu=ppu, ss=2, outline=K.RIM,
                               marks={1: K.MARK_HATCH, 2: K.MARK_CONTROLLER} if marks else None), 3)
    params = dict(k=sorted(map(str, bl.items())), t=sorted(map(str, tex.items())), y=yaw, p=pitch, u=ppu, m=missile,
                  g=marks, s=tex_sig, v=7)
    return K._cached("silo", params, make)


# ---------------------------------------------------------------- layer diagrams

COLORS = {"P": "#c9ced6", "C": "#6f7d8d", "G": "#8fd3e8", "B": "#d99a2b", "~": "#38c6d9", "h": "#ff8a1f", "S": "#2a3340", ".": "transparent"}


def layer_grid(y: int, cells: dict, hatch_pos: dict, label: str) -> str:
    rows = []
    for z in range(5):
        row = []
        for x in range(5):
            ch = cells.get((x, y, z))
            if (x, y, z) in hatch_pos:
                ch, txt = "h", hatch_pos[(x, y, z)]
            elif ch == "~":
                txt = "Ctrl"
            elif ch is not None and ch in "CGP":
                txt = ""
            elif ch == "B":
                txt = ""
            elif x in SHAFT and z in SHAFT:
                ch, txt = "S", ""
            else:
                ch, txt = ".", ""
            row.append(f'<div class="c" style="background:{COLORS[ch]}{";border:0" if ch == "." else ""}"><b>{txt}</b></div>')
        rows.append("".join(row))
    return f'<div class="lay"><div class="g">{"".join(rows)}</div><div class="lb">{label}</div></div>'


CSS = """
.ribbon { position: absolute; top: 40px; right: 56px; transform: rotate(-3deg); text-align: center;
  background: #ffd23f; color: #1a1200; font-family: Bahnschrift, "Fira Sans Condensed", "Noto Sans", sans-serif; font-stretch: condensed;
  font-weight: 800; font-size: 38px; letter-spacing: .06em; text-transform: uppercase; padding: 12px 28px 10px; border-radius: 10px;
  border: 4px solid #1a1200; outline: 3px solid #ffd23f; box-shadow: 0 8px 22px rgba(0,0,0,.6); z-index: 5; }
.lay { display: flex; flex-direction: column; align-items: center; gap: 8px; }
.lay .g { display: grid; grid-template-columns: repeat(5, 40px); grid-auto-rows: 40px; gap: 3px; }
.lay .c { border-radius: 4px; display: flex; align-items: center; justify-content: center; border: 1px solid rgba(255,255,255,.12); }
.lay .c b { font-size: 10px; color: #1a1200; font-weight: 700; text-align: center; line-height: 1.05; }
.lay .lb { font-size: 16px; color: var(--muted); text-align: center; }
.lay .lb b { color: var(--txt); display: block; font-size: 18px; }
.req { display: grid; grid-template-columns: 56px 1fr; gap: 14px; align-items: center; padding: 9px 0; border-bottom: 1px solid var(--line); }
.req:last-child { border-bottom: 0; }
.req .t { font-size: 18px; color: var(--muted); line-height: 1.3; }
.req .t b { color: var(--txt); }
.step { display: flex; gap: 14px; align-items: flex-start; }
.step .t { font-size: 18px; color: var(--muted); line-height: 1.32; }
.step .t b { color: var(--txt); }
"""

HATCH_NAME = {"input_bus": "Bus", "input_hatch": "Fuel", "energy_hatch": "EU", "maintenance_hatch": "Maint"}


def silo() -> str:
    b = silo_build()
    cells = silo_cells()
    hp = {pos: HATCH_NAME[k] for pos, k, _ in HATCHES}
    # View 2: a viewing trench dug in front of the glass rings (x 0..4, from the grass down to the shaft floor).
    hero = render(b, yaw=-22, pitch=36, ppu=46, ground=True, bottom=-1, cut=lambda x, y, z: z < 0 and 0 <= x <= 4 and y >= 1)
    # View 1: just the top shows.
    surface = render(b, yaw=-38, pitch=30, ppu=44, ground=True, missile=False, marks=False)
    n_casing = sum(1 for v in cells.values() if v == "C")
    n_glass = sum(1 for v in cells.values() if v == "G")
    n_beam = sum(1 for v in cells.values() if v == "B")
    layers = (layer_grid(0, cells, hp, "<b>Floor</b>layer 1 &middot; 12 concrete, 9 pad")
              + layer_grid(1, cells, hp, "<b>Glass ring</b>layer 2 &middot; 12 glass")
              + layer_grid(2, cells, hp, "<b>Glass ring</b>layer 3 &middot; controller in front")
              + layer_grid(3, cells, hp, "<b>Glass ring</b>layer 4 &middot; 12 glass")
              + layer_grid(4, cells, hp, "<b>Wall</b>layers 5 to 11 &middot; 12 concrete each")
              + layer_grid(11, cells, {}, "<b>Rim</b>layer 12 &middot; 12 beams"))
    hatch_rows = "".join(
        f'<div class="req"><div class="t" style="grid-column:1/3"><b>{esc(name)}</b> &middot; {esc(why)}</div></div>'
        for name, why in [
            ("Input Bus (at least 1)", "carries missile bodies and warheads; the silo mates them itself"),
            ("Input Hatch (at least 1)", "the fuel. It must be the fuel the missile's tier needs, or better"),
            ("Energy Hatch (at least 1)", "its tier sets the heaviest missile the silo launches: HV for Tier 1, EV for Tier 2, LuV for Tier 3. A bigger missile is refused"),
            ("Maintenance Hatch (1)", "as on any GregTech multiblock"),
        ])
    parts = "".join([
        TP.part(TP.gc_icon("compressed_titanium"), 4, "Compressed Titanium Plate", 72),
        TP.part(TP.NHCORE / "heavy_duty_rocket_fins_t3.png", 4, "Heavy Duty Rocket Fins Tier 3", 72),
        TP.part(TP.NHCORE / "heavy_duty_rocket_engine_t3.png", 1, "Heavy Duty Rocket Engine Tier 3", 72),
        f'<span class="part" style="width:260px"><span class="pl"><b style="display:block">Also needed</b>a LuV missile chip (Multifiberglass board, 2 Master circuits and platinum fine wire in the Circuit Assembler), engraved with a Yellow lens.</span></span>'])
    body = f"""<div style="display:grid;grid-template-columns:1fr 470px;gap:20px">
<div class="card" style="display:flex;flex-direction:column;min-height:680px">
<div style="display:flex;gap:28px;align-items:baseline;flex-wrap:wrap"><span class="label">Size</span><span style="font-size:18px">5 &times; 5 &times; 12 blocks, corners cut off</span><span>Controller {chip("HV")}</span>
<span style="font-size:16px;margin-left:auto"><b style="color:#38c6d9">&#9632;</b> controller &nbsp; <b style="color:var(--orange)">&#9632;</b> hatches</span></div>
<div class="spot" style="flex:1;display:flex;align-items:center;justify-content:center;padding:10px 0;margin-top:8px">{img(hero, h=620, style="max-width:100%")}</div>
<div class="muted" style="font-size:16px;margin-top:8px">The silo as you find it in the world, with a trench dug in front of it so the three glass rings show. The rim is level with the grass, the shaft is open to the sky, and the missile stands on the Galacticraft launch pad, a stepped plate at the bottom of the shaft.</div></div>
<div class="card"><div class="label" style="margin-bottom:6px">Blocks</div>
<div class="req"><div class="t" style="grid-column:1/3"><b>{n_casing} Light or Dark Concrete</b> &middot; the seven wall rings and the floor's outer ring, any finish except cobblestone, mixed freely. You need at least 84; hatches may replace the rest</div></div>
<div class="req"><div class="t" style="grid-column:1/3"><b>9 Galacticraft Launch Pad</b> &middot; the 3 &times; 3 floor of the shaft, a stepped plate. Place the nine pad blocks and Galacticraft joins them into the full pad</div></div>
<div class="req"><div class="t" style="grid-column:1/3"><b>{n_glass + 1} Clear Glass</b> &middot; the three rings above the floor, so the missile shows. Use Tinkers&#39; Construct Clear Glass or EnderIO Quite Clear Glass, mixed freely; no other glass fits. Hatches and the controller replace glass panes</div></div>
<div class="req"><div class="t" style="grid-column:1/3"><b>{n_beam} Gantry Beam</b> &middot; the rim on top</div></div>
<div class="req"><div class="t" style="grid-column:1/3"><b>1 Missile Silo controller</b> (HV) &middot; middle of the front wall, in the middle glass ring; it faces away from the shaft</div></div>
<div class="label" style="margin:14px 0 4px">Hatches &middot; go anywhere except the top rim and the pad: in the walls, the glass rings or the floor&#39;s outer ring</div>{hatch_rows}</div></div>
<h2 style="margin:24px 0 12px">Layer by layer</h2>
<div class="card" style="display:flex;justify-content:space-around;gap:10px;padding:22px 14px">{layers}</div>
<div class="muted" style="font-size:16px;margin-top:8px">Dark squares are the open shaft; light blue is glass; silver is the launch pad. The front wall is the top row. Looking down from above.</div>
<div class="grid" style="grid-template-columns:1fr 1fr;margin-top:18px">
<div class="card"><h2 style="margin-bottom:8px">Found in the world</h2><div class="spot" style="display:flex;justify-content:center;padding:10px">{img(surface, h=560, style="max-width:100%")}</div>
<div class="muted" style="font-size:16px;margin-top:6px">Walk past a silo and you see only a ring of beams and the open shaft. The other 11 layers are underground. The missile, 8 blocks tall with its warhead, climbs out of it.</div></div>
<div class="card"><h2 style="margin-bottom:10px">Loading and launching</h2><div style="display:flex;flex-direction:column;gap:14px">
<div class="step"><span class="num">1</span><div class="t"><b>Feed it.</b> Put a missile body and a warhead into the Input Bus, by hand or by conveyor. The silo seats the warhead on the nose and stands the missile on the pad.</div></div>
<div class="step"><span class="num">2</span><div class="t"><b>Fuel it.</b> Pipe the right fuel into the Input Hatch. A Tier 3 missile takes 8,000 mB of Purple or Green fuel. Power the Energy Hatch.</div></div>
<div class="step"><span class="num">3</span><div class="t"><b>Link it.</b> Put a Launch Control within 6 blocks sideways and 4 up or down of the silo controller. It finds the silo the way it finds a pad.</div></div>
<div class="step"><span class="num">4</span><div class="t"><b>Launch.</b> Pick the target on the Launch Control and fire. You never touch the missile.</div></div></div></div></div>
<div class="card" style="margin-top:18px"><h2 style="margin-bottom:6px">Building a Tier 3 body</h2>
<div class="muted" style="font-size:18px;margin-bottom:12px">A Tier 3 body is built like the smaller ones, by hand on the NASA Workbench or on a LuV Assembly Line. It needs the silo: a pad refuses it. It carries every warhead casing, from Tier 1 up to antimatter and red matter.</div>
<div class="parts">{parts}</div></div>
<div class="card" style="margin-top:18px"><h2 style="margin-bottom:8px">Why launch from a silo</h2>
<div class="muted" style="font-size:18px;margin-bottom:10px">A missile fired from the silo flies farther and lands closer to where you aimed than one on an open pad. Tiers 1 and 2 can launch from it too, if the Energy Hatch is high enough.</div>
<div class="grid" style="grid-template-columns:repeat(4,minmax(0,1fr));gap:14px">
<div class="wcard"><div class="label">Range</div><div class="cond o" style="font-size:30px;font-weight:700">&times; 1.25</div><div class="muted" style="font-size:16px">A Tier 3 missile has a base range of 8,000 blocks; from the silo, 10,000.</div></div>
<div class="wcard"><div class="label">Scatter</div><div class="cond o" style="font-size:30px;font-weight:700">&times; 0.5</div><div class="muted" style="font-size:16px">Half the usual miss distance. A Tier 3 body scatters 6 blocks before guidance.</div></div>
<div class="wcard"><div class="label">Fuel</div><div class="cond o" style="font-size:30px;font-weight:700">8,000 mB</div><div class="muted" style="font-size:16px">Purple or Green. Flies at 1.6 blocks per tick.</div></div>
<div class="wcard"><div class="label">Energy Hatch</div><div class="cond o" style="font-size:30px;font-weight:700">HV / EV / LuV</div><div class="muted" style="font-size:16px">Tier 1, Tier 2 and Tier 3 missiles. Lower than the missile, and the silo refuses the launch.</div></div></div></div>"""
    return page_doc("Missile Silo", "The Tier 3 launch tower: a 5 by 5 multiblock of concrete and clear glass.", body,
                    extra=CSS + TP.PART_CSS,
                    credit=["Missile and warhead models and textures © HBM's Nuclear Tech Mod contributors, GNU LGPL v3; modified.",
                            "Hatch overlays: GregTech 5 Unofficial textures, LGPL-3.0.", TP.GC_CREDIT, TP.CORE_CREDIT, TP.TC_CREDIT,
                            MOJANG_CREDIT, TP.TINT_CREDIT])


PAGES = {"icbm-missile-silo": silo}

if __name__ == "__main__":
    import build
    build.main(["icbm-missile-silo"])
