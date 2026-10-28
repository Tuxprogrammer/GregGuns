"""Irradiated earth tiles from HBM's Nuclear Tech Mod (LGPL-3.0) block art, with a procedural fallout treatment.

    python -I tools/asset-gen/icbm/make_fallout_earth.py <dir holding assets/hbm> <repo root>

Cool earth: NTM's dead dirt, dry waste sand and slaked sellafield stone, each pulled to its own hue and dusted with
pale green fallout specks, so no variant reads as plain grass or vanilla dirt. Hot earth: the cool tile darkened, with
glowing lime veins cut through it by NTM's sellafield_3 pattern (shifted per variant) and a few white-hot specks, so hot earth is
unmistakable even in daylight. Output: irradiated_<variant>.png and irradiated_<variant>_hot.png in
textures/blocks/thirdparty/ntm. Deterministic (fixed seed).
"""
import colorsys
import random
import sys
from pathlib import Path

from PIL import Image

# variant -> (NTM source, hue shift target as (hue or None, saturation scale, value scale))
SOURCES = {
    "dirt": ("waste_earth_bottom", None, 1.0, 0.85),
    "stone": ("sellafield_slaked", 0.58, 0.35, 1.0),
    "sand": ("waste_grass_top", None, 1.1, 1.05),
    "gravel": ("sellafield_slaked_1", 0.08, 0.30, 0.62),
    "cobble": ("sellafield_slaked_2", 0.0, 0.0, 1.12),
}
DUST = (214, 236, 120)
GLOW = (150, 255, 50)
WHITE_HOT = (240, 255, 190)


def recolor(img, hue, sat, val):
    out = img.convert("RGBA")
    px = out.load()
    for x in range(out.width):
        for y in range(out.height):
            r, g, b, a = px[x, y]
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if hue is not None:
                h = hue
            r, g, b = colorsys.hsv_to_rgb(h, min(1.0, s * sat), min(1.0, v * val))
            px[x, y] = (int(r * 255), int(g * 255), int(b * 255), 255)
    return out


def cool(base, rnd):
    out = base.copy()
    px = out.load()
    for _ in range(18):
        x, y = rnd.randrange(16), rnd.randrange(16)
        r, g, b, _a = px[x, y]
        px[x, y] = tuple(int(c * 0.35 + d * 0.65) for c, d in zip((r, g, b), DUST)) + (255,)
    return out


def hot(base, glow_src, rnd, shift):
    out = base.copy()
    px = out.load()
    raw = glow_src.convert("RGBA").load()
    gp = {(x, y): raw[(x + shift * 5) % 16, (y + shift * 3) % 16] for x in range(16) for y in range(16)}
    greens = sorted(gp[x, y][1] for x in range(16) for y in range(16))
    cut = greens[int(len(greens) * 0.55)]  # veins are the brightest 45% of sellafield_3's green
    for x in range(16):
        for y in range(16):
            r, g, b, _a = px[x, y]
            gg = gp[x, y][1]
            if gg > cut:
                k = 0.7 + 0.3 * (gg - cut) / max(1, greens[-1] - cut)
                px[x, y] = tuple(int(c * k) for c in GLOW) + (255,)
            else:
                px[x, y] = (int(r * 0.5), int(g * 0.5 + 8), int(b * 0.5)) + (255,)
    for _ in range(6):
        px[rnd.randrange(16), rnd.randrange(16)] = WHITE_HOT + (255,)
    return out


def main(src, repo):
    tex = Path(src) / "assets/hbm/textures/blocks"
    dest = Path(repo) / "src/main/resources/assets/gregguns/textures/blocks/thirdparty/ntm"
    glow_src = Image.open(tex / "sellafield_3.png")
    for shift, (name, (ntm, hue, sat, val)) in enumerate(SOURCES.items()):
        rnd = random.Random(name)
        base = recolor(Image.open(tex / f"{ntm}.png"), hue, sat, val)
        cool(base, rnd).save(dest / f"irradiated_{name}.png")
        hot(base, glow_src, rnd, shift).save(dest / f"irradiated_{name}_hot.png")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
