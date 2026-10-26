"""Copies the I2 item icons and the Launch Control overlays from HBM's Nuclear Tech Mod (LGPL-3.0).

Usage: python convert_ntm_icons.py <extracted ntm jar dir> <repo root>

The jar dir holds assets/hbm/textures/{items,blocks}, taken from the NTM jar at tag 1.0.27_X5846.
Items are plain copies under new names. The overlays are the NTM terminal and controller faces, with the
active versions animated by this script (the green screen pixels pulse; the top gets a blinking green lamp).
"""
import shutil
import sys
from pathlib import Path

from PIL import Image

ITEMS = {
    "missile_engine_mk1": "mp_thruster",
    "missile_engine_mk2": "thruster_medium",
    "guidance_computer_mk1": "mp_c_2",
    "guidance_computer_mk2": "mp_c_3",
    "guidance_computer_mk3": "mp_c_4",
    "missile_chip_t1": "circuit.chip",
    "missile_chip_t2": "circuit.advanced",
    "missile_schematic_t1": "blueprints",
    "missile_schematic_t2": "blueprint_folder",
}
OVERLAYS = {  # our name: NTM block texture
    "OVERLAY_FRONT": "cm_terminal_front",
    "OVERLAY_SIDE": "machine_controller_side",
    "OVERLAY_TOP": "machine_controller_top",
    "OVERLAY_BOTTOM": "machine_controller_back",
}


def is_green(px):
    r, g, b, a = px
    return a > 0 and g > r + 40 and g > b + 40


def pulse(src, level):
    """A copy of src with every green pixel scaled by level (0..1)."""
    out = src.copy()
    for x in range(src.width):
        for y in range(src.height):
            px = src.getpixel((x, y))
            if is_green(px):
                out.putpixel((x, y), tuple(int(c * level) for c in px[:3]) + (px[3],))
    return out


def lamp(src, on):
    out = src.copy()
    colour = (60, 255, 80, 255) if on else (20, 70, 28, 255)
    for x in (2, 3):
        for y in (2, 3):
            out.putpixel((x, y), colour)
    return out


def strip(frames):
    out = Image.new("RGBA", (16, 16 * len(frames)))
    for i, f in enumerate(frames):
        out.paste(f, (0, 16 * i))
    return out


def main(jar, repo):
    jar, repo = Path(jar), Path(repo)
    res = repo / "src/main/resources/assets/gregguns/textures"
    items = res / "items/thirdparty/ntm"
    for ours, theirs in ITEMS.items():
        shutil.copyfile(jar / f"assets/hbm/textures/items/{theirs}.png", items / f"{ours}.png")
    dst = res / "blocks/machines/thirdparty/ntm/launch_control"
    dst.mkdir(parents=True, exist_ok=True)
    loaded = {}
    for ours, theirs in OVERLAYS.items():
        img = Image.open(jar / f"assets/hbm/textures/blocks/{theirs}.png").convert("RGBA")
        loaded[ours] = img
        img.save(dst / f"{ours}.png")
    strip([pulse(loaded["OVERLAY_FRONT"], v) for v in (1.0, 0.7, 0.45, 0.7)]).save(dst / "OVERLAY_FRONT_ACTIVE.png")
    strip([lamp(loaded["OVERLAY_TOP"], on) for on in (True, True, False, False)]).save(dst / "OVERLAY_TOP_ACTIVE.png")
    for n in ("OVERLAY_FRONT_ACTIVE", "OVERLAY_TOP_ACTIVE"):
        (dst / f"{n}.png.mcmeta").write_text('{"animation":{"frametime":10}}\n')
    shutil.copyfile(items / "LICENSE.txt", dst / "LICENSE.txt")
    shutil.copyfile(items / "GPL-3.0.txt", dst / "GPL-3.0.txt")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
