"""I5 art from HBM's Nuclear Tech Mod (LGPL-3.0): fallout, ender and anti-grav warhead skins, floppy, antidote, launcher icons.

    python -I tools/asset-gen/icbm/convert_ntm_i5.py <dir holding assets/hbm> <repo root>

The dir holds assets/hbm/textures, taken from NTM at tag 1.0.27_X5846 (the repo's src/main/resources works).
Plain copies are renamed only. The ender and anti-grav skins are hue shifts of NTM's HE warhead skin (the
same one the T3 HE warhead uses), so they fit warhead_t3.obj's UVs. The floppy is NTM's empty disk tinted gold.
"""
import colorsys
import shutil
import sys
from pathlib import Path

from PIL import Image

ITEMS = {
    "irradiated_rubble": "fallout",
    "trinitite_shard": "trinitite",
    "fallout_goo": "billet_nuclear_waste",
    "antidote": "syringe_antidote",
    "launcher_warhead_he": "ammo_standard.rocket_he",
    "launcher_warhead_heat": "ammo_standard.rocket_heat",
    "launcher_warhead_incendiary": "ammo_standard.rocket_inc",
    "launcher_warhead_demolition": "ammo_standard.rocket_demo",
    "launcher_warhead_phosphorus": "ammo_standard.rocket_phosphorus",
    "launcher_warhead_nuclear": "ammo_standard.nuke_standard",
    "launcher_warhead_emp": "grenade_filling.emp",
    "launcher_warhead_grenade_he": "grenade_filling.he",
    "launcher_warhead_grenade_cluster": "ammo_grenade_kampf",
}
BLOCKS = {"trinitite": "waste_trinitite", "fallout_goo": "sellafield_0"}


def hsv(img, hue=None, shift=0.0, sat=1.0, val=1.0):
    """Recolor: set every pixel's hue (or shift it), scale saturation and value."""
    out = img.copy()
    px = out.load()
    for x in range(out.width):
        for y in range(out.height):
            r, g, b, a = px[x, y]
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            h = hue if hue is not None else (h + shift) % 1.0
            r, g, b = colorsys.hsv_to_rgb(h, min(1.0, s * sat), min(1.0, v * val))
            px[x, y] = (int(r * 255), int(g * 255), int(b * 255), a)
    return out


def main(src, repo):
    src, repo = Path(src), Path(repo)
    tex = src / "assets/hbm/textures"
    res = repo / "src/main/resources/assets/gregguns/textures"
    items = res / "items/thirdparty/ntm"
    blocks = res / "blocks/thirdparty/ntm"
    models = res / "models/thirdparty/ntm"
    blocks.mkdir(parents=True, exist_ok=True)
    for ours, theirs in ITEMS.items():
        shutil.copyfile(tex / f"items/{theirs}.png", items / f"{ours}.png")
    for ours, theirs in BLOCKS.items():
        shutil.copyfile(tex / f"blocks/{theirs}.png", blocks / f"{ours}.png")
    # the disk is grey (no saturation), so tint it gold by luminance rather than by hue
    g = Image.open(tex / "items/drive.disk_empty.png").convert("RGBA")
    px = g.load()
    for x in range(g.width):
        for y in range(g.height):
            r, gg, b, a = px[x, y]
            if a:
                lum = (r + gg + b) / 3 / 255
                px[x, y] = (int(min(255, 255 * lum * 1.15)), int(min(255, 205 * lum * 1.15)), int(70 * lum), a)
    g.save(items / "golden_dome_floppy.png")
    he = Image.open(tex / "models/missile_parts/warheads/mp_w_10_he.png").convert("RGBA")
    # Paint out the "TNT" lettering (x 20..36, y 52..63) with the body color beside it.
    body = he.getpixel((8, 58))
    for x in range(20, 37):
        for y in range(52, 63):
            he.putpixel((x, y), body)
    hsv(he, hue=0.77, sat=0.9, val=1.0).save(models / "warhead_ender.png")  # violet
    hsv(he, hue=0.52, sat=0.55, val=1.15).save(models / "warhead_antigrav.png")  # pale cyan
    for d in (blocks,):
        shutil.copyfile(items / "LICENSE.txt", d / "LICENSE.txt")
        shutil.copyfile(items / "GPL-3.0.txt", d / "GPL-3.0.txt")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
