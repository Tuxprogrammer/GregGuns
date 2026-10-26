"""Third-party art for the ICBM guide sheets: Galacticraft's launch pad and rocket parts (MIT, D85), the core mod's
Tier 3 rocket parts (GPL-3.0, D86), a thin-slab pad, and the tinted vanilla grass, leaves and logs the worldgen views need.

The Galacticraft and core-mod files are read from the repo's thirdparty folders, never from a jar. The grass and
leaf textures are Mojang's, tinted green the way the game tints them (a derived render, noted in the sheet's credit).
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

GC_ITEMS = K.ASSETS / "textures/items/thirdparty/galacticraft"
GC_PAD = K.ASSETS / "textures/blocks/thirdparty/galacticraft/launch_pad.png"
NHCORE = K.ASSETS / "textures/items/thirdparty/nhcore"

PAD_HEIGHT = 3 / 16   # Galacticraft's launch pad is a slab about 3/16 of a block tall

GC_CREDIT = ("Launch pad and rocket part textures © Team Galacticraft (Galacticraft Legacy), MIT License.")
CORE_CREDIT = ("Heavy Duty Rocket Engine and Fins Tier 3 icons © NewHorizonsCoreMod contributors (GT New Horizons), "
               "GNU GPL v3. This sheet is a combined work and is released under the GPL-3.0.")
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


def pad_slab(x0: int, z0: int, w: int = 3, d: int = 3, y: float = 0.0) -> R.Scene:
    """The launch pad: a w x d slab of Galacticraft's pad texture, PAD_HEIGHT tall, its footprint at (x0, z0), floor at y."""
    base = R.voxel_scene({(x, 0, z): "pad" for x in range(w) for z in range(d)}, {"pad": GC_PAD})
    s = R.Scene()
    s.extend(base, R.tr(x0, y, z0) @ R.sc(1, PAD_HEIGHT, 1))
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
