"""Render the five GregGuns Coin icons (1, 10, 100, 1000, 10K) as 16x16 GT-style PNGs.

    python -I -X utf8 tools/asset-gen/coins.py            # the five icons and their credit rows
    python -I -X utf8 tools/asset-gen/coins.py --sheet    # also a contact sheet (8x scale)

The coins follow the GTNH pack's coin convention (a round coin whose rim color names the denomination: gold 1, green
10, blue 100, orange 1000, black 10K) with GregGuns' own emblem in the middle: a brass round standing on a crosshair.
Original procedural art built from pixel.py; nothing is copied or traced from another mod. Deterministic.

Output: src/main/resources/assets/gregguns/textures/items/coin*.png (the item's subtype icons, see ItemCoin and
CoinDenomination), credit rows in tools/asset-gen/credits/coins.csv, contact sheet docs/assets/previews/coins.png.
"""
import csv
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from pixel import Canvas, ramp, tint, DK, SH, BS, LT, HL  # noqa: E402

import outroot  # noqa: E402  (GREGGUNS_ASSETS_OUT redirects every output)

ROOT = outroot.ROOT
ASSETS = outroot.RES

# (texture name, rim color). The names match CoinDenomination.iconName() in Java.
COINS = [
    ("coin", (226, 178, 40)),        # 1: gold
    ("coin_10", (60, 176, 76)),      # 10: green
    ("coin_100", (56, 132, 214)),    # 100: blue
    ("coin_1000", (226, 110, 36)),   # 1000: orange
    ("coin_10000", (92, 92, 108)),   # 10K: black (gunmetal)
]

FIELD = ramp((58, 62, 72), 0.9)      # the dark face inside the rim
BRASS = ramp((222, 168, 52))
COPPER = ramp((200, 108, 54))
CENTER = 7.5


def _ring(r_out, r_in):
    out = []
    for y in range(16):
        for x in range(16):
            d = ((x - CENTER) ** 2 + (y - CENTER) ** 2) ** 0.5
            if r_in <= d < r_out:
                out.append((x, y))
    return out


def _disc(r):
    return [(x, y) for y in range(16) for x in range(16) if ((x - CENTER) ** 2 + (y - CENTER) ** 2) ** 0.5 < r]


def draw(rim_rgb):
    c = Canvas()
    rim = ramp(rim_rgb, 1.1)
    # Face, then the rim over it; the rim's bevel gives the minted edge.
    c.fill(_disc(7.0), FIELD)
    c.fill(_ring(7.6, 5.6), rim)
    # Crosshair ticks in the rim color, at the four compass points of the face.
    for (x, y) in ((7, 3), (8, 3), (7, 12), (8, 12), (3, 7), (3, 8), (12, 7), (12, 8)):
        c.pix(x, y, rim[LT])
    # The round: a two-pixel brass case with a copper bullet tip and a dark primer line.
    c.rect(7, 7, 8, 11, BRASS)
    c.pix(7, 6, COPPER[LT])
    c.pix(8, 6, COPPER[BS])
    c.pix(7, 5, COPPER[HL])
    c.pix(8, 5, COPPER[SH])
    c.hline(7, 8, 9, BRASS[SH])
    c.pix(7, 11, BRASS[DK])
    c.pix(8, 11, BRASS[DK])
    # A small rim glint top-left, like a struck coin.
    c.pix(4, 2, rim[HL])
    c.pix(3, 3, rim[HL])
    c.outline(0.32)
    return c


def main(argv):
    rows = []
    images = []
    for name, rgb in COINS:
        c = draw(rgb)
        path = ASSETS / "textures" / "items" / (name + ".png")
        path.parent.mkdir(parents=True, exist_ok=True)
        c.save(path)
        images.append(c.to_image())
        rows.append({
            "asset": "icon." + name,
            "path": str(path.relative_to(ROOT)) if path.is_relative_to(ROOT) else str(path),
            "generator": "tools/asset-gen/coins.py",
            "seed": "none",
            "method": "Procedural 16x16 pixel art (pixel.py discs, bevel shading, outline pass); pack coin color convention",
            "license": "MIT",
            "date": "2026-10-29",
        })
    credits = outroot.CREDITS_DIR / "coins.csv"
    credits.parent.mkdir(parents=True, exist_ok=True)
    with open(credits, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(rows[0].keys()))
        w.writeheader()
        w.writerows(rows)
    if "--sheet" in argv:
        from PIL import Image
        scale = 8
        sheet = Image.new("RGBA", (16 * scale * len(images) + 8 * (len(images) + 1), 16 * scale + 16), (48, 48, 52, 255))
        for i, im in enumerate(images):
            big = im.resize((16 * scale, 16 * scale), Image.NEAREST)
            sheet.alpha_composite(big, (8 + i * (16 * scale + 8), 8))
        out = outroot.PRE / "coins.png"
        out.parent.mkdir(parents=True, exist_ok=True)
        sheet.save(out)
        print("sheet:", out)
    for r in rows:
        print(r["path"])


if __name__ == "__main__":
    main(sys.argv[1:])
