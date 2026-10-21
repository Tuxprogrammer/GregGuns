"""The Golden Gun's model sheet: the 9mm Pistol sheet with its luminance remapped onto a polished gold palette.

    python -X utf8 tools/asset-gen/golden_gun_texture.py

Reads textures/models/guns/pistol_9mm.png and writes golden_gun.png (same 64x32 layout, so the Golden Gun can wear the
pistol's boxes). Shading is kept: each pixel's luminance is stretched over the sheet's own range and looked up in a
gold ramp (deep #7a5a10 through #d4a017 and #f5d76e to highlight #fff4c2). The grip panels (the grip box's UV region)
use a darker gold-brown ramp. Deterministic, no randomness. Writes credits/models.csv's golden_gun row.
"""
import csv
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
GUNS = ROOT / "src/main/resources/assets/gregguns/textures/models/guns"
CREDITS = ROOT / "tools/asset-gen/credits/models.csv"

GOLD = [(0.0, "#7a5a10"), (0.45, "#d4a017"), (0.8, "#f5d76e"), (1.0, "#fff4c2")]
GRIP = [(0.0, "#3e2a08"), (0.5, "#6e4e12"), (1.0, "#a67c1a")]
# The grip box is 3x7x2 at UV (0, 0): its unfolded net spans 10 x 9 texels.
GRIP_REGION = (0, 0, 10, 9)


def ramp(stops, t):
    xs = [s[0] for s in stops]
    cols = np.array([[int(h[i:i + 2], 16) for i in (1, 3, 5)] for _, h in stops], float)
    return np.stack([np.interp(t, xs, cols[:, c]) for c in range(3)], axis=-1)


def main():
    a = np.array(Image.open(GUNS / "pistol_9mm.png").convert("RGBA"))
    solid = a[..., 3] > 0
    lum = a[..., :3].astype(float).mean(axis=2)
    lo, hi = lum[solid].min(), lum[solid].max()
    t = np.clip((lum - lo) / (hi - lo), 0, 1) ** 0.8
    out = a.copy()
    gold = ramp(GOLD, t)
    grip = ramp(GRIP, t)
    x0, y0, x1, y1 = GRIP_REGION
    region = np.zeros(solid.shape, bool)
    region[y0:y1, x0:x1] = True
    rgb = np.where(region[..., None], grip, gold)
    out[..., :3] = np.where(solid[..., None], np.round(rgb), a[..., :3]).astype(np.uint8)
    Image.fromarray(out, "RGBA").save(GUNS / "golden_gun.png")
    row = ["model.gun.golden_gun", "src/main/resources/assets/gregguns/textures/models/guns/golden_gun.png",
           "tools/asset-gen/golden_gun_texture.py", 0,
           "Pistol sheet luminance remapped onto a polished gold ramp, grip panels gold-brown", "MIT", "2026-10-21"]
    rows = list(csv.reader(open(CREDITS, encoding="utf-8", newline="")))
    rows = [r for r in rows if r and r[0] != row[0]] + [row]
    with open(CREDITS, "w", encoding="utf-8", newline="") as f:
        csv.writer(f, lineterminator="\n").writerows(rows)
    print("wrote golden_gun.png")


if __name__ == "__main__":
    main()
