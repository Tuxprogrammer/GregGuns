"""Third-party art for the ICBM guide sheets: Galacticraft's launch pad and rocket parts (MIT, D85), the core mod's
Tier 3 rocket parts (GPL-3.0, D86), the formed launch pad, and the tinted vanilla grass, leaves and logs the worldgen views need.

The Galacticraft and core-mod files are read from inputs/thirdparty/ beside this script, never from a jar and never
from src/main/resources, so the mod jar carries none of them. The grass and leaf textures are Mojang's, tinted green the way the game tints them (a derived render, noted in the sheet's credit).
"""

from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import numpy as np  # noqa: E402
from PIL import Image  # noqa: E402

import kit as K  # noqa: E402
import render3d as R  # noqa: E402
import vanilla  # noqa: E402
from pages import slot  # noqa: E402

INPUTS = Path(__file__).resolve().parent / "inputs/thirdparty"
GC_ITEMS = INPUTS / "galacticraft"
GC_PAD = INPUTS / "galacticraft/launch_pad.png"
NHCORE = INPUTS / "nhcore"
TCONSTRUCT_GLASS = INPUTS / "tconstruct/clear_glass.png"

PAD_HEIGHT = 0.4   # the formed launch pad's top tier ends 0.4 of a block above the floor; a missile stands on it

GC_CREDIT = ("Launch pad and rocket part textures © Team Galacticraft (Galacticraft Legacy), MIT License.")
CORE_CREDIT = ("Heavy Duty Rocket Engine and Fins Tier 3 icons © NewHorizonsCoreMod contributors (GT New Horizons), "
               "GNU GPL v3. This sheet is a combined work and is released under the GPL-3.0.")
TC_CREDIT = ("Clear Glass texture © the Tinkers' Construct authors (GT New Horizons fork), GNU LGPL v3.")
TINT_CREDIT = "Exception to the above: grass and leaves are Mojang's textures tinted green, as the game does."


def gc_icon(name: str) -> Path:
    return GC_ITEMS / f"{name}.png"


def part(path: Path, count: int | None, label: str, px: int = 56) -> str:
    """An inventory-slot icon with a count and a caption under it."""
    n = f"{count} &times; " if count else ""
    return (f'<span class="part">{slot(K.icon(path, 64), px, label)}'
            f'<span class="pl"><b>{n}</b>{label}</span></span>')


PART_CSS = """
.part { display: inline-flex; flex-direction: column; align-items: center; gap: 5px; text-align: center; width: 150px; }
.part .pl { font-size: 16px; line-height: 1.2; color: var(--muted); }
.part .pl b { color: var(--txt); font-weight: 600; }
.parts { display: flex; flex-wrap: wrap; gap: 8px 14px; align-items: flex-start; }
"""


# The formed 3 x 3 launch pad, as Galacticraft draws it: three stacked boxes, every face the launch_pad texture.
# Boxes are (min x, min y, min z, max x, max y, max z) relative to the pad's middle block, in blocks.
# Source: https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/core/client/render/block/BlockRendererLandingPad.java
#   renderBlockLandingPad: setRenderBounds(-1, 0, -1, 2, 0.2, 2), then (-0.5, 0.2, -0.5, 1.5, 0.3, 1.5), then (0, 0.3, 0, 1, 0.4, 1).
# The block (BlockLandingPadFull.getIcon, meta 0) gives all six faces the launch_pad icon. RenderBlocks stretches the
# whole icon over a top face (its bounds leave 0..1), and a side face shows the strip of the icon at that height.
PAD_BOXES = [(-1.0, 0.0, -1.0, 2.0, 0.2, 2.0), (-0.5, 0.2, -0.5, 1.5, 0.3, 1.5), (0.0, 0.3, 0.0, 1.0, 0.4, 1.0)]


def pad_formed(x0: float = 0, z0: float = 0, y: float = 0.0) -> R.Scene:
    """The formed Galacticraft launch pad as one stepped plate: a 3 x 3 base, a 2 x 2 tier and a 1 x 1 tier.
    Its footprint starts at (x0, z0) and its floor is at y; the middle block is (x0 + 1, z0 + 1)."""
    s = R.Scene()
    t = s.texture(GC_PAD)
    for (ax, ay, az, bx, by, bz) in PAD_BOXES:
        ax, bx, az, bz = ax + x0 + 1, bx + x0 + 1, az + z0 + 1, bz + z0 + 1
        ay, by = ay + y, by + y
        v0, v1 = 1 - (by - y), 1 - (ay - y)   # side faces use the strip of the icon at their height
        top = np.array([(0, 1), (1, 1), (1, 0), (0, 0)], float)
        side = np.array([(0, v1), (1, v1), (1, v0), (0, v0)], float)
        faces = [  # corners ccw seen from outside, brightness as the game lights a block
            ([(ax, by, bz), (bx, by, bz), (bx, by, az), (ax, by, az)], top, 1.0),
            ([(bx, ay, az), (ax, ay, az), (ax, by, az), (bx, by, az)], side, 0.8),
            ([(ax, ay, bz), (bx, ay, bz), (bx, by, bz), (ax, by, bz)], side, 0.8),
            ([(ax, ay, az), (ax, ay, bz), (ax, by, bz), (ax, by, az)], side, 0.6),
            ([(bx, ay, bz), (bx, ay, az), (bx, by, az), (bx, by, bz)], side, 0.6),
        ]
        for verts, uv, bright in faces:
            s.quads.append(R.Quad(np.array(verts, float), uv, t, bright))
    return s


def _tinted(src: Path, rgb, name: str) -> Path:
    out = K.TEXCACHE / f"{name}.png"
    if not out.exists():
        out.parent.mkdir(parents=True, exist_ok=True)
        a = np.array(Image.open(src).convert("RGBA")).astype(float)
        a[..., :3] = a[..., :3] * (np.array(rgb) / 255.0)
        Image.fromarray(a.astype(np.uint8)).save(out)
    return out


GRASS_RGB, LEAF_RGB = (145, 189, 89), (72, 181, 24)


def grass_top() -> Path:
    return _tinted(vanilla.texture("blocks/grass_top"), GRASS_RGB, "i3-grass-top")


def grass_side() -> Path:
    """grass_side with its overlay tinted over it, the way the game draws the block's side."""
    out = K.TEXCACHE / "i3-grass-side.png"
    if not out.exists():
        out.parent.mkdir(parents=True, exist_ok=True)
        side = Image.open(vanilla.texture("blocks/grass_side")).convert("RGBA")
        ov = Image.open(_tinted(vanilla.texture("blocks/grass_side_overlay"), GRASS_RGB, "i3-grass-ov"))
        side.alpha_composite(ov)
        side.save(out)
    return out


def leaves() -> Path:
    return _tinted(vanilla.texture("blocks/leaves_oak"), LEAF_RGB, "i3-leaves")


def terrain_textures() -> dict:
    """Voxel texture specs (top, side) for the worldgen ground and trees."""
    dirt, sand = vanilla.texture("blocks/dirt"), vanilla.texture("blocks/sand")
    return {"grass": (grass_top(), grass_side()), "dirt": dirt, "sand": sand, "stone": vanilla.texture("blocks/stone"),
            "log": (vanilla.texture("blocks/log_oak_top"), vanilla.texture("blocks/log_oak")), "leaves": leaves()}


def tree(x: int, z: int, ground_top: int, trunk: int = 4) -> dict:
    """An oak: a trunk standing on the ground (its first log at y = ground_top) with the usual four leaf layers."""
    c = {}
    for i in range(trunk):
        c[(x, ground_top + i, z)] = "log"
    top = ground_top + trunk
    for dy, r in ((-2, 2), (-1, 2), (0, 1), (1, 1)):
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                if r == 2 and abs(dx) == 2 and abs(dz) == 2:
                    continue
                if r == 1 and abs(dx) == 1 and abs(dz) == 1 and dy == 1:
                    continue
                c.setdefault((x + dx, top + dy, z + dz), "leaves")
    c[(x, top - 2, z)] = c[(x, top - 1, z)] = "log"
    return c
