"""GregTech textures for the infographics, read from the pack's GregTech jar (GT5 Unofficial, LGPL-3.0).

The jar is the one in the extracted reference pack (reference/gtnh/, git-ignored; tools/gtnh-reference recreates it).
Material items are drawn the way GregTech draws them: the texture set's grayscale icon multiplied by the material
color, with the set's _OVERLAY drawn on top untinted. Icons a set lacks fall back to the NONE set.
"""

from __future__ import annotations

import subprocess
import zipfile
from functools import lru_cache
from pathlib import Path

import numpy as np
from PIL import Image

from render3d import REPO

OUT = REPO / "build/infographics/gt"

# GregTech material colors and texture sets (GT5u Materials).
MATERIALS = {
    "Brass": ((255, 180, 0), "METALLIC"), "Bronze": ((255, 128, 0), "METALLIC"), "Lead": ((140, 100, 140), "DULL"),
    "Steel": ((128, 128, 128), "METALLIC"), "Aluminium": ((128, 200, 240), "DULL"),
    "StainlessSteel": ((200, 200, 220), "SHINY"), "Titanium": ((220, 160, 240), "METALLIC"),
    "Gunpowder": ((128, 128, 128), "DULL"), "WroughtIron": ((200, 180, 180), "METALLIC"),
    "Tungstensteel": ((100, 100, 160), "METALLIC"), "Iron": ((200, 200, 200), "METALLIC"),
    "Rubber": ((0, 0, 0), "SHINY"), "Glass": ((250, 250, 250), "GLASS"), "Wood": ((100, 50, 0), "WOOD"),
    "Tin": ((220, 220, 220), "METALLIC"),
}

# GregTech items by their gt.metaitem.01 texture number (the item id less 32000).
METAITEMS = {"motor_lv": 600, "conveyor_lv": 630, "piston_lv": 640, "robot_arm_lv": 650, "circuit_lv": 701}


def _reference_dirs():
    """reference/gtnh in this checkout, then in the main checkout when this is a git worktree."""
    yield REPO / "reference/gtnh"
    try:
        common = subprocess.run(["git", "rev-parse", "--git-common-dir"], cwd=REPO, capture_output=True, text=True,
                                check=True).stdout.strip()
        yield (REPO / common).resolve().parent / "reference/gtnh"
    except (OSError, subprocess.CalledProcessError):
        pass


@lru_cache(maxsize=1)
def _jar() -> zipfile.ZipFile:
    jars = [j for d in _reference_dirs() for j in sorted(d.glob("*/.minecraft/mods/gregtech-*.jar"))]
    if not jars:
        raise SystemExit("GregTech jar not found under reference/gtnh/. Run: python -I tools/gtnh-reference/gtnh_ref.py fetch")
    return zipfile.ZipFile(jars[0])


def _has(name: str) -> bool:
    try:
        _jar().getinfo(name)
        return True
    except KeyError:
        return False


def _read(name: str) -> Image.Image:
    with _jar().open(name) as f:
        im = Image.open(f).convert("RGBA")
        im.load()
    return im.crop((0, 0, im.width, im.width))  # first frame of an animated strip


def _save(im: Image.Image, name: str) -> Path:
    OUT.mkdir(parents=True, exist_ok=True)
    p = OUT / f"{name}.png"
    im.save(p)
    return p


def _tint(im: Image.Image, rgb) -> Image.Image:
    a = np.array(im).astype(float)
    a[..., :3] *= np.array(rgb, float) / 255.0
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def _material_icon(kind: str, material: str, folder: str) -> Path:
    rgb, tset = MATERIALS[material]
    base = f"assets/gregtech/textures/{folder}/materialicons"
    pick = tset if _has(f"{base}/{tset}/{kind}.png") else "NONE"
    im = _tint(_read(f"{base}/{pick}/{kind}.png"), rgb)
    ov = f"{base}/{pick}/{kind}_OVERLAY.png"
    if _has(ov):
        im.alpha_composite(_read(ov))
    return _save(im, f"{folder}-{kind}-{material}")


def item(kind: str, material: str) -> Path:
    """A material item icon, e.g. item("foil", "Brass"), item("round", "Lead"), item("springSmall", "Steel")."""
    return _material_icon(kind, material, "items")


def metaitem(name: str) -> Path:
    """A GregTech component item from METAITEMS, e.g. metaitem("motor_lv")."""
    n = METAITEMS[name]
    return _save(_read(f"assets/gregtech/textures/items/gt.metaitem.01/{n}.png"), f"meta-{name}")


def tool(kind: str, head: str = "Iron", handle_material: str = "Wood") -> Path:
    """A GregTech hand tool as its item icon: the head icon over the handle, e.g. tool("File"), tool("Hammer")."""
    handle = {"File": "handleFile", "Saw": "handleSaw"}.get(kind, "stick")
    im = Image.open(item(handle, handle_material)).convert("RGBA")
    im.alpha_composite(Image.open(item(f"toolHead{kind}", head)).convert("RGBA").resize(im.size, Image.NEAREST))
    return _save(im, f"tool-{kind}-{head}")


def pipe(size: str, material: str) -> Path:
    """A GregTech pipe's face (pipeTiny, pipeSmall...), which is what its item shows."""
    return _material_icon(size, material, "blocks")


def cable(material: str) -> Path:
    """A 1x cable: the wire tinted by its metal under GregTech's insulation overlay."""
    im = Image.open(_material_icon("wire", material, "blocks")).convert("RGBA")
    for ins in ("INSULATION_TINY", "INSULATION_SMALL", "INSULATION_MEDIUM"):
        if _has(f"assets/gregtech/textures/blocks/iconsets/{ins}.png"):
            ring = _read(f"assets/gregtech/textures/blocks/iconsets/{ins}.png").resize(im.size, Image.NEAREST)
            im.alpha_composite(_tint(ring, (64, 64, 64)))  # GregTech's rubber insulation color
            break
    return _save(im, f"cable-{material}")


def frame(material: str) -> Path:
    return _material_icon("frameGt", material, "blocks")


def iconset(name: str) -> Path:
    return _save(_read(f"assets/gregtech/textures/blocks/iconsets/{name}.png"), name)


def machine_overlay(machine: str, face: str) -> Path | None:
    """A GregTech basic machine's face overlay (basicmachines/<machine>/OVERLAY_<face>), or None if it has none."""
    name = f"assets/gregtech/textures/blocks/basicmachines/{machine}/OVERLAY_{face}.png"
    return _save(_read(name), f"bm-{machine}-{face}") if _has(name) else None


def bartworks(name: str) -> Path:
    return _save(_read(f"assets/bartworks/textures/blocks/{name}.png"), f"bw-{name}")


def layered(base: Path, *overlays: str, name: str) -> Path:
    """A block face: base texture with GregTech iconset overlays on top."""
    im = Image.open(base).convert("RGBA")
    im = im.crop((0, 0, im.width, im.width))
    for o in overlays:
        ov = Image.open(iconset(o)).convert("RGBA").resize(im.size, Image.NEAREST)
        im.alpha_composite(ov)
    return _save(im, name)


HATCH_OVERLAYS = {
    "input_bus": ("OVERLAY_PIPE_IN", "ITEM_IN_SIGN"), "output_bus": ("OVERLAY_PIPE_OUT", "ITEM_OUT_SIGN"),
    "input_hatch": ("OVERLAY_PIPE_IN", "FLUID_IN_SIGN"), "output_hatch": ("OVERLAY_PIPE_OUT", "FLUID_OUT_SIGN"),
    "energy_hatch": ("OVERLAY_ENERGY_IN_HV",), "maintenance_hatch": ("OVERLAY_MAINTENANCE",),
    "muffler_hatch": ("OVERLAY_MUFFLER",),
}


def hatch(kind: str, casing: Path) -> Path:
    """A hatch as it shows on a multiblock: the multiblock's casing with the hatch's GregTech overlay."""
    return layered(casing, *HATCH_OVERLAYS[kind], name=f"{kind}-{Path(casing).stem}")


def hull(tier: str, face="SIDE") -> Path:
    name = "MACHINE_BRONZE" if tier == "Steam" else f"MACHINE_{tier}"
    return iconset(f"{name}_{face}" if _has(f"assets/gregtech/textures/blocks/iconsets/{name}_{face}.png") else f"{name}_SIDE")
