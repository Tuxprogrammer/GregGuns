"""The icbm-missile-silo guide sheet: the Missile Silo multiblock, layer by layer, with a T3 missile standing in it.

The silo is built from GregGuns' own Gantry Casing and Gantry Beam art (no new textures), the hatches from GregTech's
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
from kit import chip, esc  # noqa: E402
from pages import gti, img, page_doc  # noqa: E402

NTM = K.ASSETS / "models/thirdparty/ntm"
NTM_TEX = K.ASSETS / "textures/models/thirdparty/ntm"
FRONT, BACK, LEFT, RIGHT = S.FRONT, S.BACK, S.LEFT, S.RIGHT
HEIGHT = 12            # layers: y0 floor .. y11 rim
SHAFT = range(1, 4)    # the open 3 x 3 shaft, x and z


def plan_cells() -> list[tuple[int, int]]:
    """The 5 x 5 plan with its four corner columns cut off."""
    return [(x, z) for x in range(5) for z in range(5) if (x, z) not in ((0, 0), (4, 0), (0, 4), (4, 4))]


def silo_cells() -> dict:
    """{(x, y, z): char}: C casing, G glass, B beam, ~ the controller. Floor y0 is the full plate; y1..y3 are glass rings (the controller
    replaces the middle of the front one at y2); y4..y10 are casing rings; y11 is the rim. Matches SiloShape.PIECES."""
    cells = {}
    for x, z in plan_cells():
        in_shaft = x in SHAFT and z in SHAFT
        cells[(x, 0, z)] = "C"
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
    b.define("casing", "Gantry Casing", gc, "walls and floor")
    b.define("beam", "Gantry Beam", S._casing("gantry_beam"), "the rim")
    b.define("glass", "Borosilicate Glass", gt.bartworks("BoronSilicateGlassBlock"), "any glass tier works")
    b.define("ctrl", "Missile Silo", (gc, gc, gc), "controller, middle of the front wall")
    b.put(silo_cells(), {"C": "casing", "G": "glass", "B": "beam", "~": ("ctrl", FRONT)})
    for pos, kind, facing in HATCHES:
        b.hatch(pos, kind, facing, gc)
    return b


def missile_scene():
    """The T3 body on the shaft floor with the T3 warhead seated on its nose (y = 6), centered in the shaft."""
    body = R.obj_scene(NTM / "missile_body_t3.obj", NTM_TEX / "missile_body_t3.png")
    head = R.obj_scene(NTM / "warhead_t3.obj", NTM_TEX / "warhead_t3.png")
    sc = R.Scene()
    sc.extend(body, R.tr(2 - 0.5 + 0, 1, 2 - 0.5))
    sc.extend(head, R.tr(2 - 0.5 + 0, 1 + 6, 2 - 0.5))
    return sc


def render(b: S.Build, yaw=-35, pitch=28, ppu=34, cut=None, missile=True, marks=True) -> Path:
    tex_sig = hashlib.sha1((NTM / "missile_body_t3.obj").read_bytes() + (NTM / "warhead_t3.obj").read_bytes()).hexdigest()[:8]

    def make():
        bl = b.blocks if cut is None else {p: v for p, v in b.blocks.items() if not cut(*p)}
        tags = {k: (2 if k == "ctrl" else 1) for k in b.tex if k.startswith("h_") or k == "ctrl"} if marks else None
        scene = R.voxel_scene(bl, b.tex, tags)
        if missile:
            scene.extend(missile_scene())
        return R.trim(R.render(scene, yaw=yaw, pitch=pitch, ppu=ppu, ss=2, outline=K.RIM,
                               marks={1: K.MARK_HATCH, 2: K.MARK_CONTROLLER} if marks else None), 3)
    params = dict(k=sorted(map(str, b.blocks.items())), t=sorted(map(str, b.tex.items())), y=yaw, p=pitch, u=ppu, m=missile,
                  c=cut is not None, g=marks, s=tex_sig, v=1)
    return K._cached("silo", params, make)


# ---------------------------------------------------------------- layer diagrams

COLORS = {"C": "#6f7d8d", "G": "#8fd3e8", "B": "#d99a2b", "~": "#38c6d9", "h": "#ff8a1f", "S": "#2a3340", ".": "transparent"}


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
            elif ch is not None and ch in "CG":
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
    hero = render(b, yaw=-38, pitch=26, ppu=34, missile=False)
    cutaway = render(b, yaw=-38, pitch=26, ppu=34, cut=lambda x, y, z: z <= 1 and x >= 1 and x <= 3 and y >= 1)
    n_casing = sum(1 for v in cells.values() if v == "C")
    n_glass = sum(1 for v in cells.values() if v == "G")
    n_beam = sum(1 for v in cells.values() if v == "B")
    layers = (layer_grid(0, cells, hp, "<b>Floor</b>layer 1 &middot; 21 casing")
              + layer_grid(1, cells, hp, "<b>Glass ring</b>layer 2 &middot; 12 glass")
              + layer_grid(2, cells, hp, "<b>Glass ring</b>layer 3 &middot; controller in front")
              + layer_grid(3, cells, hp, "<b>Glass ring</b>layer 4 &middot; 12 glass")
              + layer_grid(4, cells, hp, "<b>Wall</b>layers 5 to 11 &middot; 12 casing each")
              + layer_grid(11, cells, {}, "<b>Rim</b>layer 12 &middot; 12 beams"))
    hatch_rows = "".join(
        f'<div class="req"><div class="t" style="grid-column:1/3"><b>{esc(name)}</b> &middot; {esc(why)}</div></div>'
        for name, why in [
            ("Input Bus (at least 1)", "carries missile bodies and warheads; the silo mates them itself"),
            ("Input Hatch (at least 1)", "the fuel. It must be the fuel the missile's tier needs, or better"),
            ("Energy Hatch (at least 1)", "its tier sets the heaviest body the silo can launch: HV for Tier 1, EV for Tier 2, LuV for Tier 3"),
            ("Maintenance Hatch (1)", "as on any GregTech multiblock"),
        ])
    body = f"""<div style="display:grid;grid-template-columns:1fr 470px;gap:20px">
<div class="card" style="display:flex;flex-direction:column;min-height:680px">
<div style="display:flex;gap:28px;align-items:baseline;flex-wrap:wrap"><span class="label">Size</span><span style="font-size:18px">5 &times; 5 &times; 12 blocks, corners cut off</span><span>Controller {chip("LuV")}</span>
<span style="font-size:16px;margin-left:auto"><b style="color:#38c6d9">&#9632;</b> controller &nbsp; <b style="color:var(--orange)">&#9632;</b> hatches</span></div>
<div class="spot" style="flex:1;display:flex;align-items:center;justify-content:center;padding:10px 0;margin-top:8px">{img(hero, h=620, style="max-width:100%")}</div>
<div class="muted" style="font-size:16px;margin-top:8px">The shaft in the middle is open to the sky: the missile climbs out of the top. The glass rings let you see the missile; the cutaway below shows it inside.</div></div>
<div class="card"><div class="label" style="margin-bottom:6px">Blocks</div>
<div class="req"><div class="t" style="grid-column:1/3"><b>{n_casing} Gantry Casing</b> &middot; the floor and the seven wall rings. You need at least 95; hatches may take the rest of the floor's outer ring</div></div>
<div class="req"><div class="t" style="grid-column:1/3"><b>{n_glass + 1} Glass</b> &middot; the three rings above the floor, any GregTech or vanilla glass, so the missile shows. Hatches and the controller replace glass panes</div></div>
<div class="req"><div class="t" style="grid-column:1/3"><b>{n_beam} Gantry Beam</b> &middot; the rim on top</div></div>
<div class="req"><div class="t" style="grid-column:1/3"><b>1 Missile Silo controller</b> &middot; middle of the front wall, in the middle glass ring; it faces away from the shaft</div></div>
<div class="label" style="margin:14px 0 4px">Hatches &middot; replace any glass pane in the three rings, or sit in the floor&#39;s outer ring</div>{hatch_rows}</div></div>
<h2 style="margin:24px 0 12px">Layer by layer</h2>
<div class="card" style="display:flex;justify-content:space-around;gap:10px;padding:22px 14px">{layers}</div>
<div class="muted" style="font-size:16px;margin-top:8px">Dark squares are the open shaft; light blue is glass. The front wall is the top row. Looking down from above.</div>
<div class="grid" style="grid-template-columns:1fr 1fr;margin-top:18px">
<div class="card"><h2 style="margin-bottom:8px">Inside the shaft</h2><div class="spot" style="display:flex;justify-content:center;padding:10px">{img(cutaway, h=420, style="max-width:100%")}</div>
<div class="muted" style="font-size:16px;margin-top:6px">Front wall cut away. The missile stands on the shaft floor, 8 blocks tall with its warhead.</div></div>
<div class="card"><h2 style="margin-bottom:10px">Loading and launching</h2><div style="display:flex;flex-direction:column;gap:14px">
<div class="step"><span class="num">1</span><div class="t"><b>Feed it.</b> Put a missile body and a warhead into the Input Bus, by hand or by conveyor. The silo seats the warhead on the nose and stands the missile in the shaft.</div></div>
<div class="step"><span class="num">2</span><div class="t"><b>Fuel it.</b> Pipe the right fuel into the Input Hatch. Tier 3 takes Purple fuel or better. Power the Energy Hatch.</div></div>
<div class="step"><span class="num">3</span><div class="t"><b>Link it.</b> Put a Launch Control within 6 blocks sideways and 4 up or down of the silo controller. It finds the silo the way it finds a pad.</div></div>
<div class="step"><span class="num">4</span><div class="t"><b>Launch.</b> Pick the target on the Launch Control and fire. You never touch the missile.</div></div></div></div></div>"""
    return page_doc("Missile Silo", "The Tier 3 launch tower: a 5 by 5 multiblock of Gantry Casing and glass.", body,
                    extra=CSS, overlay='<div class="ribbon">Draft: numbers pending</div>',
                    credit=["Missile and warhead models and textures © HBM's Nuclear Tech Mod contributors, GNU LGPL v3; modified.",
                            "Hatch overlays: GregTech 5 Unofficial textures, LGPL-3.0."])


PAGES = {"icbm-missile-silo": silo}

if __name__ == "__main__":
    import build
    build.main(["icbm-missile-silo"])
