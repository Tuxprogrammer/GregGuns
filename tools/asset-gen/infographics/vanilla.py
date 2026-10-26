"""Simple 16x16 icons for the vanilla items recipes use (stone, logs, wool, a piston...).

Minecraft's own textures are Mojang's, so the infographics never show them. These are our own flat drawings: enough
to tell the items apart in a crafting grid, each one labeled on the sheet.
"""

from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image

from render3d import REPO

OUT = REPO / "build/infographics/vanilla"


def _noise(seed, base, spread, shape=(16, 16)):
    rng = np.random.default_rng(seed)
    a = np.zeros(shape + (4,), np.int16)
    a[..., :3] = np.array(base) + rng.integers(-spread, spread + 1, shape + (1,))
    a[..., 3] = 255
    return a


def _blank():
    return np.zeros((16, 16, 4), np.int16)


def _put(a, ys, xs, rgb):
    a[ys, xs, :3] = rgb
    a[ys, xs, 3] = 255


def _stone():
    return _noise(1, (160, 160, 160), 12)


def _cobblestone():
    a = _noise(2, (120, 120, 120), 14)
    for y, x0, x1 in ((3, 0, 7), (3, 10, 16), (8, 3, 12), (12, 0, 5), (12, 9, 16)):
        _put(a, y, slice(x0, x1), (70, 70, 70))
    for x, y0, y1 in ((7, 0, 4), (10, 3, 9), (3, 8, 13), (12, 8, 13), (5, 12, 16)):
        _put(a, slice(y0, y1), x, (70, 70, 70))
    return a


def _log():
    a = _noise(3, (104, 80, 48), 8)
    for x in (2, 6, 9, 13):
        _put(a, slice(0, 16), x, (72, 54, 30))
    return a


def _wool():
    return _noise(4, (232, 232, 228), 9)


def _leather():
    a = _blank()
    for y in range(2, 14):
        w = 6 - abs(y - 8) // 2
        _put(a, y, slice(8 - w, 8 + w), (150, 88, 48))
    _put(a, slice(4, 12), 8, (120, 66, 34))
    return a


def _string():
    a = _blank()
    for x in range(1, 15):
        y = int(8 + 3 * np.sin(x / 2.2))
        _put(a, y, x, (240, 240, 240))
        _put(a, y + 1, x, (200, 200, 200))
    return a


def _slimeball():
    a = _blank()
    yy, xx = np.mgrid[0:16, 0:16]
    m = (yy - 8.5) ** 2 + (xx - 7.5) ** 2 <= 30
    a[m, :3] = (110, 190, 90)
    a[m, 3] = 255
    _put(a, slice(5, 7), slice(5, 7), (180, 240, 160))
    return a


def _flint_and_steel():
    a = _blank()
    for i in range(8):
        _put(a, 14 - i, slice(1 + i, 4 + i), (205, 205, 212))  # the steel striker
        _put(a, 15 - i, slice(1 + i, 3 + i), (120, 120, 128))
    _put(a, slice(2, 9), slice(9, 15), (40, 40, 46))  # the flint
    _put(a, slice(3, 5), slice(10, 13), (100, 100, 108))
    return a


def _piston():
    a = _noise(5, (118, 118, 118), 8)
    a[0:5, :, :3] = np.array((170, 136, 84)) + np.random.default_rng(6).integers(-8, 9, (5, 16, 1))
    _put(a, slice(5, 16), slice(6, 10), (90, 90, 90))
    return a


def _crafting_table():
    a = _noise(7, (150, 110, 62), 8)
    a[0:4, :, :3] = (110, 78, 42)
    _put(a, slice(6, 14), 4, (90, 90, 96))  # a saw and a hammer hanging on the front
    _put(a, 6, slice(9, 14), (90, 90, 96))
    _put(a, slice(6, 13), 11, (110, 78, 42))
    return a


def _tnt():
    a = _noise(9, (200, 52, 44), 10)
    a[6:10, :, :3] = (226, 222, 214)  # the white label band
    _put(a, slice(7, 9), slice(3, 5), (40, 40, 44))  # a few letters
    _put(a, slice(7, 9), slice(7, 9), (40, 40, 44))
    _put(a, slice(7, 9), slice(11, 13), (40, 40, 44))
    return a


ICONS = {"tnt": _tnt, "stone": _stone, "cobblestone": _cobblestone, "log": _log, "wool": _wool, "leather": _leather,
         "string": _string, "slimeball": _slimeball, "flint_and_steel": _flint_and_steel, "piston": _piston,
         "crafting_table": _crafting_table}


BLOCKS = {"tnt", "stone", "cobblestone", "log", "wool", "piston", "crafting_table"}


def icon(name: str) -> Path:
    """One of ICONS as a 16x16 PNG. Blocks get a dark edge so they stand off a gray inventory slot."""
    OUT.mkdir(parents=True, exist_ok=True)
    p = OUT / f"{name}.png"
    a = ICONS[name]()
    if name in BLOCKS:
        for edge in (np.s_[0, :], np.s_[15, :], np.s_[:, 0], np.s_[:, 15]):
            a[edge][..., :3] = (a[edge][..., :3] * 0.45).astype(np.int16)
    Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA").save(p)
    return p
