"""Block textures and voxel scenes for the machine and multiblock infographics.

GregGuns casings and machine overlays come from the mod's own textures; the GregTech parts of a build (hulls,
hatches, glass, frame boxes, item pipe casings) come from the GregTech jar through gt.py.

Multiblock layouts are read from the same strings the mod builds its structures from (the *Shape classes), so the
pictures follow the code.
"""

from __future__ import annotations

import re
from pathlib import Path

from PIL import Image

from render3d import ASSETS, REPO

TEX = ASSETS / "textures/blocks"
JAVA = REPO / "src/main/java/com/gregguns"

def composite(base: Path, *overlays: Path, cache: Path, name: str) -> Path:
    """A casing with GregGuns overlays drawn on it (first frame of any animated strip)."""
    im = Image.open(base).convert("RGBA")
    im = im.crop((0, 0, im.width, im.width))
    for o in overlays:
        if o and Path(o).exists():
            ov = Image.open(o).convert("RGBA")
            ov = ov.crop((0, 0, ov.width, ov.width)).resize(im.size, Image.NEAREST)
            im.alpha_composite(ov)
    cache.mkdir(parents=True, exist_ok=True)
    p = cache / f"{name}.png"
    im.save(p)
    return p


def first_frame(path: Path, cache: Path, name: str) -> Path:
    im = Image.open(path).convert("RGBA")
    cache.mkdir(parents=True, exist_ok=True)
    p = cache / f"{name}.png"
    im.crop((0, 0, im.width, im.width)).save(p)
    return p


# ---------------------------------------------------------------- shapes from the Java sources


def java_shape(cls: str, field: str) -> list[list[str]]:
    """Read a String[][] constant out of a *Shape.java file."""
    src = next(JAVA.rglob(f"{cls}.java")).read_text()
    m = re.search(rf"String\[\]\[\]\s+{field}\s*=\s*\{{(.*?)\}};", src, re.S)
    body = m.group(1)
    groups = re.findall(r"\{([^{}]*)\}", body)
    return [re.findall(r'"([^"]*)"', g) for g in groups]


def stack_aisles(pieces, x_from_string=True):
    """Aisle-format pieces (aisle -> rows top to bottom -> chars) laid back to back. Returns {(x, y, z): ch}."""
    cells = {}
    z = 0
    height = max(len(a) for p in pieces for a in p)
    for piece in pieces:
        for aisle in piece:
            pad = height - len(aisle)
            for r, row in enumerate(aisle):
                y = height - 1 - (r + pad)
                for x, ch in enumerate(row):
                    if ch not in " -.":
                        cells[(x, y, z)] = ch
            z += 1
    return cells


def stack_layers(layers):
    """Layer-format pieces (horizontal layers top to bottom, rows = z, chars = x). Returns {(x, y, z): ch}."""
    cells = {}
    n = len(layers)
    for i, layer in enumerate(layers):
        y = n - 1 - i
        for z, row in enumerate(layer):
            for x, ch in enumerate(row):
                if ch not in " -.":
                    cells[(x, y, z)] = ch
    return cells
