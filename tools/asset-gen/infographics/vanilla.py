"""Vanilla item icons for the infographics.

D55: Minecraft content is fine to show as long as Mojang's usage guidelines are followed. A sheet may show the real
vanilla textures, read at build time from the Minecraft jar (`mc_texture`, `real`) and never committed as loose files or
shipped in the mod jar, and it carries the footer line `MC_CREDIT`. The simple 16x16 drawings below (`icon`) are our own
stand-ins, from before D55, for the sheets that have not been rebuilt with the real textures yet; they stay until then.
"""

from __future__ import annotations

import io
import os
import zipfile
from functools import lru_cache
from pathlib import Path

import numpy as np
from PIL import Image

from render3d import REPO

OUT = REPO / "build/infographics/vanilla"
MC_OUT = REPO / "build/infographics/vanilla-mc"
MC_CREDIT = "Minecraft textures \u00a9 Mojang / Microsoft; not an official Minecraft product."


def _reference_dirs():
    """reference/gtnh in this checkout, then in the main checkout when this is a git worktree."""
    yield REPO / "reference/gtnh"
    main = next((Path(line[9:]) for line in _worktree_list() if line.startswith("worktree ")), None)
    if main:
        yield main / "reference/gtnh"


def _worktree_list():
    import subprocess
    try:
        return subprocess.run(["git", "worktree", "list", "--porcelain"], cwd=REPO, capture_output=True, text=True,
                              check=True).stdout.splitlines()
    except (OSError, subprocess.CalledProcessError):
        return []


@lru_cache(maxsize=1)
def _mc_jar() -> zipfile.ZipFile:
    """The Minecraft 1.7.10 client jar the pack runs on. Looked up in $GREGGUNS_MC_JAR, then in the reference pack
    (reference/gtnh/), then in the Prism Launcher libraries that the pack's instance uses. Never copied into the repo."""
    cands = [Path(os.environ["GREGGUNS_MC_JAR"])] if os.environ.get("GREGGUNS_MC_JAR") else []
    for d in _reference_dirs():
        cands += sorted(d.glob("**/minecraft*1.7.10*.jar"))
    home = Path.home()
    for base in (home / ".local/share/PrismLauncher", home / ".var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher",
                 home / "AppData/Roaming/PrismLauncher"):
        cands += sorted(base.glob("libraries/com/mojang/minecraft/1.7.10/minecraft-1.7.10-client.jar"))
    for c in cands:
        if c.exists():
            return zipfile.ZipFile(c)
    raise SystemExit("Minecraft 1.7.10 client jar not found. Set GREGGUNS_MC_JAR to it (the pack's Prism instance "
                     "downloads it under libraries/com/mojang/minecraft/1.7.10/).")


def mc_texture(path: str) -> Image.Image:
    """A vanilla texture from the Minecraft jar, e.g. "items/flint_and_steel" or "blocks/tnt_side" (first frame)."""
    with _mc_jar().open(f"assets/minecraft/textures/{path}.png") as f:
        im = Image.open(io.BytesIO(f.read())).convert("RGBA")
    return im.crop((0, 0, im.width, im.width)) if im.height > im.width else im


def real(path: str, name: str | None = None) -> Path:
    """A vanilla texture written under build/ (git-ignored) so the page and the voxel renderer can load it."""
    MC_OUT.mkdir(parents=True, exist_ok=True)
    p = MC_OUT / f"{name or path.replace('/', '_')}.png"
    mc_texture(path).save(p)
    return p


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
