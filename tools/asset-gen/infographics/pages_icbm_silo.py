"""The icbm-missile-silo guide sheet: the Missile Silo multiblock, layer by layer, with a T3 missile standing in it.

The silo is built from GregTech's Light Concrete art (walls), GregGuns' Gantry Beam art (rim) and Tinkers' Clear Glass, the hatches from GregTech's
overlays, and the missile from the shipped NTM OBJ models seated as the game seats them. The cells are written out in
`silo_cells` (a 5 x 5 plan with the four corners cut off, but the two front ones in the glass rings, the hatch columns;
a 3 x 3 open shaft, a 21-cell floor, ten wall layers and a ring of Gantry Beams on top), and `_verify` compares them
cell by cell with the `SiloShape.PIECES` strings, so a changed structure stops the build. The closed doors are drawn
from the sizes in RenderSiloDoors. The numbers (T3 fuel, range, speed and scatter, the silo bonus, cycle times, the
door and countdown timings) are read from the Java sources at build time, with pages_icbm_missile's readers.

Run on its own:  python tools/asset-gen/infographics/pages_icbm_silo.py
"""

from __future__ import annotations

import hashlib
import re
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))

import blocks as B  # noqa: E402
import kit as K  # noqa: E402
import gt  # noqa: E402
import render3d as R  # noqa: E402
import scenes as S  # noqa: E402
import thirdparty as TP  # noqa: E402
from kit import chip, esc  # noqa: E402
from pages import MOJANG_CREDIT, gti, img, page_doc  # noqa: E402
from pages_icbm_missile import build_spec, cfg, const, missile_tiers, need, src  # noqa: E402

NTM = K.ASSETS / "models/thirdparty/ntm"
NTM_TEX = K.ASSETS / "textures/models/thirdparty/ntm"
FRONT, BACK, LEFT, RIGHT = S.FRONT, S.BACK, S.LEFT, S.RIGHT
HEIGHT = 12            # layers: y0 floor .. y11 rim
SHAFT = range(1, 4)    # the open 3 x 3 shaft, x and z

# ---------------------------------------------------------------- numbers, read from the code

SILO_SHAPE = "icbm/silo/SiloShape.java"
RULES = "icbm/silo/SiloRules.java"
MT = missile_tiers()
T3 = MT[3]
RANGE_X = cfg("siloRangeFactor")
SCATTER_X = cfg("siloScatterFactor")
CASING_MIN = int(const(SILO_SHAPE, "CASING_MINIMUM"))
BODY_S = const(RULES, "BODY_TICKS") / 20
WARHEAD_S = const(RULES, "WARHEAD_TICKS") / 20
PUMP_S = const(RULES, "PUMP_TICKS") / 20
PUMP_MB = int(const(RULES, "PUMP_MB"))
DOOR_S = cfg("doorOpenTicks") / 20
DOOR_CLOSE = int(cfg("doorCloseHeight"))
COUNTDOWN_S = cfg("countdownTicks") / 20
CLIMB_CLEAR = int(const("icbm/missile/FlightPath.java", "MIN_CLIMB"))
_wh3 = re.search(r'T3\("warhead_t3", ExplosiveTier\.T3, \d+, (\d+),', src("icbm/warhead/WarheadCasing.java"))
if not _wh3:
    raise SystemExit("WarheadCasing.T3 changed; update pages_icbm_silo.py")
STACK_HEIGHT = T3["height"] + int(_wh3.group(1))
T3_NEAR = T3["scatter"] * max(const("icbm/missile/Scatter.java", "MIN_SHARE"), SCATTER_X)
T3_FAR = T3_NEAR + const("icbm/missile/Scatter.java", "DISTANCE_TERM") * SCATTER_X


def n(v: float) -> str:
    return f"{int(v):,}" if float(v).is_integer() else f"{v:g}"


def plan_cells() -> list[tuple[int, int]]:
    """The 5 x 5 plan with its four corner columns cut off."""
    return [(x, z) for x in range(5) for z in range(5) if (x, z) not in ((0, 0), (4, 0), (0, 4), (4, 4))]


def silo_cells() -> dict:
    """{(x, y, z): char}: C casing, P launch pad, G glass, B beam, ~ the controller. Floor y0 is a casing ring around the 3 x 3 pad; y1..y3 are glass rings (the controller
    replaces the middle of the front one at y2); y4..y10 are casing rings; y11 is the rim. `_verify` checks it against SiloShape.PIECES."""
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
    # The front is five wide in the glass rings: a hatch column of concrete on each side of the 3 x 3 window.
    for y in range(1, 4):
        cells[(0, y, 0)] = "C"
        cells[(4, y, 0)] = "C"
    return cells


def shape_cells() -> dict:
    """{(x, y, z): char} from SiloShape.PIECES: aisles front to back (z), rows top to bottom (y = 11 - row), columns
    with column 0 on the right of a viewer facing the controller (x = column, as the renderer draws it). The wall,
    floor-ring and hatch-column characters (C, F, H) all read as casing."""
    body = re.search(r"String\[\]\[\] SILO\(\)\s*\{\s*return new String\[\]\[\]\s*\{(.*?)\};\s*\}", src(SILO_SHAPE), re.S)
    if not body:
        raise SystemExit("SiloShape.SILO() not found; update pages_icbm_silo.py")
    aisles = [re.findall(r'"([^"]*)"', g) for g in re.findall(r"\{([^{}]*)\}", body.group(1))]
    if len(aisles) != 5 or any(len(a) != HEIGHT or any(len(r) != 5 for r in a) for a in aisles):
        raise SystemExit("SiloShape layout changed; update pages_icbm_silo.py")
    cells = {}
    for z, aisle in enumerate(aisles):
        for row, line in enumerate(aisle):
            for x, ch in enumerate(line):
                if ch != " ":
                    cells[(x, HEIGHT - 1 - row, z)] = {"F": "C", "H": "C"}.get(ch, ch)
    return cells


def _verify() -> None:
    if shape_cells() != silo_cells():
        raise SystemExit("silo_cells() no longer matches SiloShape.PIECES; update pages_icbm_silo.py")
    cells = silo_cells()
    assert sum(1 for v in cells.values() if v == "C") - 6 - 12 == CASING_MIN, "the walls are no longer the casing minimum"
    hc = re.search(r"HATCH_COLUMN_RIGHT = (\d+);", src(SILO_SHAPE))
    assert hc and int(hc.group(1)) == 0 and LAUNCH_CONTROL[0] == 0, "the right-hand hatch column moved"
    t3 = build_spec(3)
    need(t3, "Tier.LUV", '"craftingLensYellow"', "GalacticraftParts.HULL_T3, 4,", "GalacticraftParts.FINS_HEAVY, 4,", "GalacticraftParts.ENGINE_HEAVY, 1,",
         "Arrays.asList(IcbmPart.GUIDANCE_COMPUTER_MK3)", "180, Tier.LUV,", '"Circuit_Board_Multifiberglass", 1',
         'Ing.gt("circuit", "Master", 2', 'Ing.gt("wireFine", "Platinum"', where="MissileBuildSpec.T3")
    need(src("icbm/missile/MissileTier.java"), 'T3("missile_body_t3", ExplosiveTier.T3, 8000, 20, 8000, 1.6, 6.0, 6, 60.0F, "fluid.rocketfuelmixc",\n        "fluid.rocketfuelmixa")',
         where="MissileTier.T3 (Purple and Green fuel)")
    need(src("icbm/silo/SiloGlass.java"), "EIO_ENLIGHTENED_META = 3", where="SiloGlass")
    need(src("icbm/silo/MTEMissileSilo.java"), "checkHasAnyEnergy(errors)", "checkHasMaintenanceHatch(errors)",
         "checkHasInputBus(errors)", "checkHasInputHatch(errors)", where="MTEMissileSilo.checkMachine")
    need(src("icbm/silo/SiloRules.java"), "weakest energy hatch", where="SiloRules")
    lang = (K.REPO / "src/main/resources/assets/gregguns/lang/en_US.lang").read_text(encoding="utf-8")
    need(lang, "gregguns_silo_no_power=§cNo power", "gregguns_silo_waiting_body=§fWaiting for a missile body",
         "gregguns_silo_inert_warhead=§fThe warhead in the Input Bus is inert", where="en_US.lang")
    door = src("client/icbm/RenderSiloDoors.java")
    need(door, "HINGE = 1.5;", "LENGTH = 1.5;", "DEPTH = 3.0;", "THICK = 0.125;", "gantry_casing.png", where="RenderSiloDoors")


# Where the sheet shows each hatch (the generated layout): the hatch columns beside the 3 x 3 glass window, two high.
# Seen from the front: Input Hatch over Maintenance Hatch on the left, Input Bus over Energy Hatch on the right, and the
# Launch Control on top of the right column. The renderer draws x = 0 on the right of a viewer facing the front (z = 0)
# face, as SiloShape's column 0 is, so the right-hand column is x = 0.
HATCHES = [((4, 2, 0), "input_hatch", FRONT), ((4, 1, 0), "maintenance_hatch", FRONT),
           ((0, 2, 0), "input_bus", FRONT), ((0, 1, 0), "energy_hatch", FRONT)]
LAUNCH_CONTROL = (0, 3, 0)
LC_DIR = "thirdparty/ntm/launch_control"

_verify()


def launch_control_tex():
    """The Launch Control as the game draws it: an HV hull with its NTM-derived overlays (top, side, front)."""
    faces = {}
    for face in ("TOP", "SIDE", "FRONT"):
        faces[face] = B.composite(gt.hull("HV", "TOP" if face == "TOP" else "SIDE"), S._ov(LC_DIR, face),
                                  cache=K.TEXCACHE, name=f"launch_control_HV_{face}")
    return (faces["TOP"], faces["SIDE"], faces["FRONT"])


def silo_build() -> S.Build:
    b = S.Build("silo")
    gc = S._casing("gantry_casing")
    b.define("casing", "Light Concrete", gt.iconset("CONCRETE_LIGHT_STONE"), "walls and the floor ring; Light or Dark Concrete, any finish")
    b.define("beam", "Gantry Beam", S._casing("gantry_beam"), "the rim")
    b.define("glass", "Clear Glass", TP.TCONSTRUCT_GLASS, "Tinkers' Clear Glass or EnderIO Quite Clear Glass")
    face = B.composite(gc, S._ov("thirdparty/ntm/missile_silo", "FRONT"), cache=K.TEXCACHE, name="missile_silo_FRONT")
    b.define("ctrl", "Missile Silo", (gc, gc, face), "controller, middle of the front wall")
    b.put(silo_cells(), {"C": "casing", "G": "glass", "B": "beam", "~": ("ctrl", FRONT)})
    for pos, kind, facing in HATCHES:
        b.hatch(pos, kind, facing, gc)
    b.define("h_launch_control", "Launch Control", launch_control_tex(), "on top of the right hatch column; it runs on the silo's EU")
    b.blocks[LAUNCH_CONTROL] = ("h_launch_control", FRONT)
    b.hatches.append(("h_launch_control", LAUNCH_CONTROL))
    return b


def missile_scene():
    """The T3 body standing on the formed pad with the T3 warhead seated on its nose (y = 6), centered in the shaft."""
    body = R.obj_scene(NTM / "missile_body_t3.obj", NTM_TEX / "missile_body_t3.png")
    head = R.obj_scene(NTM / "warhead_t3.obj", NTM_TEX / "warhead_t3.png")
    sc = R.Scene()
    sc.extend(body, R.tr(2 - 0.5 + 0, TP.PAD_HEIGHT, 2 - 0.5))
    sc.extend(head, R.tr(2 - 0.5 + 0, TP.PAD_HEIGHT + T3["height"], 2 - 0.5))
    return sc


def doors_scene() -> R.Scene:
    """The two closed door panels as RenderSiloDoors draws them: Gantry Casing slabs 1.5 x 3 blocks and 2/16 thick,
    lying flat across the 3 x 3 opening with their top face on the rim's top plane, split down the middle."""
    s = R.Scene()
    t = s.texture(S._casing("gantry_casing"))
    top, y0 = float(HEIGHT), HEIGHT - 0.125
    for ax, bx in ((1.0, 2.5), (2.5, 4.0)):
        az, bz = 1.0, 4.0
        full = np.array([(0, 1), (1, 1), (1, 0), (0, 0)], float)
        edge = np.array([(0, 1), (1, 1), (1, 1 - 0.125), (0, 1 - 0.125)], float)
        for verts, uv, bright in (
                ([(ax, top, bz), (bx, top, bz), (bx, top, az), (ax, top, az)], full, 1.0),
                ([(bx, y0, az), (ax, y0, az), (ax, top, az), (bx, top, az)], edge, 0.8),
                ([(ax, y0, bz), (bx, y0, bz), (bx, top, bz), (ax, top, bz)], edge, 0.8),
                ([(ax, y0, az), (ax, y0, bz), (ax, top, bz), (ax, top, az)], edge, 0.6),
                ([(bx, y0, bz), (bx, y0, az), (bx, top, az), (bx, top, bz)], edge, 0.6)):
            s.quads.append(R.Quad(np.array(verts, float), uv, t, bright))
    return s


SURFACE = 11   # the top layer of the silo (y = 11) is level with the ground: grass at y = 11, the rim just under its top face
SAND = {(x, z) for x in range(5, 9) for z in range(-4, -1)} | {(x, z) for x in range(-4, -1) for z in range(5, 8)}
TREES = [(6, 6), (-3, 3), (7, 0)]


def ground_cells(extent=range(-4, 9), bottom=-3) -> dict:
    """The earth the silo is dug into: grass on dirt on stone, a sand patch or two, the silo's rim level with the
    grass, and a few oaks standing around it."""
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


def render(b: S.Build, yaw=-35, pitch=28, ppu=34, cut=None, missile=True, marks=True, ground=False, bottom=-3, doors=False) -> Path:
    files = [NTM / "missile_body_t3.obj", NTM / "warhead_t3.obj", TP.GC_PAD]
    tex_sig = hashlib.sha1(b"".join(f.read_bytes() for f in files)).hexdigest()[:8]
    bl = {}
    tex = dict(b.tex)
    if ground:
        bl.update(ground_cells(bottom=bottom))
        tex.update(TP.terrain_textures())
    # The silo wins over the earth it is dug into (its front hatch columns stand in the cut corners).
    bl.update(b.blocks)
    if cut is not None:
        bl = {p: v for p, v in bl.items() if not cut(*p)}

    def make():
        tags = {k: (2 if k == "ctrl" else 1) for k in tex if k.startswith("h_") or k == "ctrl"} if marks else None
        scene = R.voxel_scene(bl, tex, tags)
        scene.extend(TP.pad_formed(1, 1))
        if missile:
            scene.extend(missile_scene())
        if doors:
            scene.extend(doors_scene())
        return R.trim(R.render(scene, yaw=yaw, pitch=pitch, ppu=ppu, ss=2, outline=K.RIM,
                               marks={1: K.MARK_HATCH, 2: K.MARK_CONTROLLER} if marks else None), 3)
    params = dict(k=sorted(map(str, bl.items())), t=sorted(map(str, tex.items())), y=yaw, p=pitch, u=ppu, m=missile,
                  g=marks, s=tex_sig, d=doors, v=9)
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
    hp[LAUNCH_CONTROL] = "LC"
    # View 2: a viewing trench dug in front of the glass rings (from the grass down to the shaft floor), two blocks wider
    # on the right-hand side (x < 0) so the camera, looking in from the left, sees the right-hand hatch column too.
    hero = render(b, yaw=-22, pitch=36, ppu=46, ground=True, bottom=-1, cut=lambda x, y, z: z < 0 and -2 <= x <= 4 and y >= 1)
    # View 1: just the top shows, with the doors closed over the shaft.
    surface = render(b, yaw=-38, pitch=30, ppu=44, ground=True, missile=False, marks=False, doors=True)
    n_casing = sum(1 for v in cells.values() if v == "C")
    n_glass = sum(1 for v in cells.values() if v == "G")
    n_beam = sum(1 for v in cells.values() if v == "B")
    layers = (layer_grid(0, cells, hp, "<b>Floor</b>layer 1 &middot; 12 concrete, 9 pad")
              + layer_grid(1, cells, hp, "<b>Glass ring</b>layer 2 &middot; 12 glass, 2 hatches")
              + layer_grid(2, cells, hp, "<b>Glass ring</b>layer 3 &middot; controller in front")
              + layer_grid(3, cells, hp, "<b>Glass ring</b>layer 4 &middot; Launch Control, concrete")
              + layer_grid(4, cells, hp, "<b>Wall</b>layers 5 to 11 &middot; 12 concrete each")
              + layer_grid(11, cells, {}, "<b>Rim</b>layer 12 &middot; 12 beams"))
    hatch_rows = "".join(
        f'<div class="req"><div class="t" style="grid-column:1/3"><b>{esc(name)}</b> &middot; {esc(why)}</div></div>'
        for name, why in [
            ("Input Bus (at least 1)", "carries missile bodies and warheads; the silo builds the missile itself"),
            ("Input Hatch (at least 1)", "the fuel. It must be the fuel the missile's tier needs, or better"),
            ("Energy Hatch (at least 1)", "the weakest one sets the heaviest missile the silo handles: HV for Tier 1, EV for Tier 2, LuV for Tier 3. A bigger missile is refused"),
            ("Maintenance Hatch (1)", "as on any GregTech multiblock"),
        ])
    parts = "".join([
        TP.part(TP.NHCORE / "heavy_duty_plate_t4.png", 4, "Heavy Duty Plate Tier 4", 72),
        TP.part(TP.gc_icon("rocket_fins_t2"), 4, "Heavy Rocket Fins", 72),
        TP.part(TP.gc_icon("rocket_engine_t2"), 1, "Heavy Rocket Engine", 72),
        f'<span class="part" style="width:420px"><span class="pl"><b style="display:block">Also needed</b>a LuV missile chip (Multifiberglass board, 2 Master circuits and platinum fine wire in the Circuit Assembler), engraved with a Yellow lens, and a Guidance Computer Mk III.</span></span>'])
    t1r, t2r, t3r = (MT[k]["rng"] * RANGE_X for k in (1, 2, 3))
    body = f"""<div style="display:grid;grid-template-columns:1fr 470px;gap:20px">
<div class="card" style="display:flex;flex-direction:column;min-height:680px">
<div style="display:flex;gap:28px;align-items:baseline;flex-wrap:wrap"><span class="label">Size</span><span style="font-size:18px">5 &times; 5 &times; 12 blocks, corners cut off</span><span>Controller {chip("HV")}</span>
<span style="font-size:16px;margin-left:auto"><b style="color:#38c6d9">&#9632;</b> controller &nbsp; <b style="color:var(--orange)">&#9632;</b> hatches</span></div>
<div class="spot" style="flex:1;display:flex;align-items:center;justify-content:center;padding:10px 0;margin-top:8px">{img(hero, h=620, style="max-width:100%")}</div>
<div class="muted" style="font-size:16px;margin-top:8px">A silo built into the ground, with a trench dug in front of it so the three glass rings show. The rim is level with the grass, and the missile stands on the Galacticraft launch pad, a stepped plate at the bottom of the shaft. The doors over the shaft are left out of this view.</div></div>
<div class="card"><div class="label" style="margin-bottom:6px">Blocks</div>
<div class="req"><div class="t" style="grid-column:1/3"><b>{n_casing} Light or Dark Concrete</b> &middot; the seven wall rings, the floor's outer ring and the two hatch columns beside the front window, any finish except cobblestone, mixed freely. You need at least {CASING_MIN}; hatches may replace the rest</div></div>
<div class="req"><div class="t" style="grid-column:1/3"><b>9 Galacticraft Launch Pad</b> &middot; the 3 &times; 3 floor of the shaft, a stepped plate. Place the nine pad blocks and Galacticraft joins them into the full pad</div></div>
<div class="req"><div class="t" style="grid-column:1/3"><b>{n_glass} Clear Glass</b> &middot; the three rings above the floor, so the missile shows; the front is a 3 &times; 3 window around the controller. Use Tinkers&#39; Construct Clear Glass or EnderIO Quite Clear or Enlightened Clear Glass, mixed freely; no other glass fits</div></div>
<div class="req"><div class="t" style="grid-column:1/3"><b>{n_beam} Gantry Beam</b> &middot; the rim on top</div></div>
<div class="req"><div class="t" style="grid-column:1/3"><b>1 Missile Silo controller</b> (HV) &middot; middle of the front wall, in the middle glass ring; it faces away from the shaft</div></div>
<div class="label" style="margin:14px 0 4px">Hatches &middot; in the hatch columns beside the front window; any wall, glass or floor-ring cell works too</div>{hatch_rows}</div></div>
<h2 style="margin:24px 0 12px">Layer by layer</h2>
<div class="card" style="display:flex;justify-content:space-around;gap:10px;padding:22px 14px">{layers}</div>
<div class="muted" style="font-size:16px;margin-top:8px">Dark squares are the open shaft; light blue is glass; silver is the launch pad. The front wall is the top row, five wide in the glass rings. Looking down from above, so the hatch column on your right as you face the controller is on the left here.</div>
<div class="grid" style="grid-template-columns:1fr 1fr;margin-top:18px">
<div class="card"><h2 style="margin-bottom:8px">From above</h2><div class="spot" style="display:flex;justify-content:center;padding:10px">{img(surface, h=420, style="max-width:100%")}</div>
<div class="muted" style="font-size:16px;margin-top:6px">Built into the ground, a silo shows only its ring of beams and two door panels lying across the shaft; the other 11 layers are underground. The doors swing open for a launch, and the missile, {STACK_HEIGHT} blocks tall with its warhead, climbs out.</div>
<div class="muted" style="font-size:16px;margin-top:8px"><b style="color:var(--txt)">In a silo complex</b> the silos stand in pairs along the silo gallery, deep underground, each with a concrete shaft up to an opening at the surface. Every one is missing its controller or its Energy Hatch for you to put in.</div></div>
<div class="card"><h2 style="margin-bottom:10px">Loading and launching</h2><div style="display:flex;flex-direction:column;gap:13px">
<div class="step"><span class="num">1</span><div class="t"><b>Feed it.</b> Put a missile body and a loaded warhead into the Input Bus, by hand or by conveyor. The silo stands the body on the pad and runs a {n(BODY_S)}-second cycle, then seats the warhead on its nose and runs another {n(WARHEAD_S)} seconds, which arms the missile. Parts may come in any order: a warhead alone shows &ldquo;Waiting for a missile body&rdquo;. An inert warhead is refused.</div></div>
<div class="step"><span class="num">2</span><div class="t"><b>Fuel it.</b> Pipe the right fuel into the Input Hatch; the silo pumps up to {PUMP_MB:,} mB every {n(PUMP_S)} seconds. A Tier 3 missile takes {T3['fuel_mb']:,} mB of Purple or Green fuel.</div></div>
<div class="step"><span class="num">3</span><div class="t"><b>Power it.</b> Every cycle draws the missile tier&#39;s voltage from the Energy Hatches. With too little EU the silo takes nothing and shows &ldquo;No power&rdquo;, then starts by itself when power arrives.</div></div>
<div class="step"><span class="num">4</span><div class="t"><b>Link it.</b> Build a Launch Control into the silo, on top of the right-hand hatch column or in place of any wall, glass or floor-ring block, one per silo. It fires that silo and no other, draws its EU from the silo, and its screen names it.</div></div>
<div class="step"><span class="num">5</span><div class="t"><b>Launch.</b> Pick the target on the Launch Control and press Launch. The doors swing open ({n(DOOR_S)} seconds), a {n(COUNTDOWN_S)}-second countdown runs, and the doors close once the missile is {DOOR_CLOSE} blocks above the rim. <b>Abort</b> before ignition stands it down, armed and fueled. Keep the {CLIMB_CLEAR} blocks above the missile&#39;s nose clear. You never touch the missile.</div></div></div></div></div>
<div class="card" style="margin-top:18px"><h2 style="margin-bottom:6px">Building a Tier 3 body</h2>
<div class="muted" style="font-size:18px;margin-bottom:12px">A Tier 3 body is built like the smaller ones: by hand on the NASA Workbench, or on a LuV Assembly Line once a hand-built body has been scanned for its research. It needs the silo: a pad refuses it. It carries every warhead casing, from Tier 1 up to antimatter and red matter.</div>
<div class="parts">{parts}</div></div>
<div class="card" style="margin-top:18px"><h2 style="margin-bottom:6px">Why launch from a silo</h2>
<div class="muted" style="font-size:18px;margin-bottom:10px">A missile fired from the silo flies farther and lands closer to where you aimed than one on an open pad. Tiers 1 and 2 can launch from it too, if the Energy Hatch is high enough.</div>
<div class="grid" style="grid-template-columns:repeat(4,minmax(0,1fr));gap:14px">
<div class="wcard"><div class="label">Range</div><div class="cond o" style="font-size:30px;font-weight:700">&times; {n(RANGE_X)}</div><div class="muted" style="font-size:16px">Tier 3 reaches {n(t3r)} blocks from the silo, Tier 2 {n(t2r)} and Tier 1 {n(t1r)}.</div></div>
<div class="wcard"><div class="label">Scatter</div><div class="cond o" style="font-size:30px;font-weight:700">&times; {n(SCATTER_X)}</div><div class="muted" style="font-size:16px">Half the usual miss distance. A Tier 3 missile lands within {n(T3_NEAR)} blocks of the aim point at short range, {n(T3_FAR)} at its longest.</div></div>
<div class="wcard"><div class="label">Fuel</div><div class="cond o" style="font-size:30px;font-weight:700">{T3['fuel_mb']:,} mB</div><div class="muted" style="font-size:16px">Purple or Green. Flies at {n(T3['speed'])} blocks per tick.</div></div>
<div class="wcard"><div class="label">Energy Hatch</div><div class="cond o" style="font-size:30px;font-weight:700">HV / EV / LuV</div><div class="muted" style="font-size:16px">Tier 1, Tier 2 and Tier 3 missiles. The weakest hatch counts; below the missile&#39;s voltage the silo refuses it.</div></div></div></div>"""
    return page_doc("Missile Silo", "The Tier 3 launch tower: a 5 by 5 multiblock of concrete and clear glass.", body,
                    extra=CSS + TP.PART_CSS,
                    credit=["Missile and warhead models and textures © HBM's Nuclear Tech Mod contributors, GNU LGPL v3; modified.",
                            "Missile Silo and Launch Control overlays: derived from HBM's Nuclear Tech Mod, GNU LGPL v3.",
                            "Hatch overlays: GregTech 5 Unofficial textures, LGPL-3.0.", TP.GC_CREDIT, TP.CORE_CREDIT, TP.TC_CREDIT,
                            MOJANG_CREDIT, TP.TINT_CREDIT])


PAGES = {"icbm-missile-silo": silo}

if __name__ == "__main__":
    import build
    build.main(["icbm-missile-silo"])
