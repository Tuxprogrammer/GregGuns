"""Textures of the two explosive charges (TNT charge, advanced "exp" charge), plus their armed variants.

Clean-room: painted from the palettes in docs/techguns-spec/09-machines-utility/model-notes/{tnt_charge,exp_charge}.md.
Deterministic numpy/Pillow; each texture is seeded from its asset id. Each sheet is 32x32 and unwraps boxes the way
ChargeModel documents (top at (u+d, v), bottom at (u+d+w, v), left/front/right/back in a row at v+d):

  TNT: stick box 4x12x4 at (0, 0)  -> 16x16;  plate box 8x4x2 at (0, 16) -> 20x6
  EXP: brick box 10x10x5 at (0, 0) -> 30x15;  plate box 8x4x2 at (0, 16) -> 20x6

The armed variant recolors the plate's top, bottom, left, front and right faces (80 pixels) to red-pink.

Run: python -I -X utf8 tools/asset-gen/charge_models.py
Writes src/main/resources/assets/gregguns/textures/models/charges/*.png and tools/asset-gen/credits/charges.csv.
"""
import csv
import os
import zlib

import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "gregguns", "textures", "models", "charges")
CREDITS = os.path.join(ROOT, "tools", "asset-gen", "credits", "charges.csv")
SIZE = 32
DATE = "2026-10-19"


def hx(s):
    s = s.lstrip("#")
    return np.array([int(s[i:i + 2], 16) for i in (0, 2, 4)], float)


def seed_for(key):
    return zlib.crc32(key.encode()) & 0xFFFFFFFF


class Sheet:
    def __init__(self, rng):
        self.px = np.zeros((SIZE, SIZE, 4), float)
        self.rng = rng

    def put(self, x, y, c, jitter=0.0):
        n = self.rng.integers(-1, 2) * jitter if jitter else 0.0
        self.px[y, x, :3] = np.clip(np.asarray(c, float) + n, 0, 255)
        self.px[y, x, 3] = 255

    def save(self, path):
        img = Image.fromarray(np.clip(self.px + 0.5, 0, 255).astype(np.uint8))
        img.save(path)


def box_rects(u, v, w, h, d):
    """Face rectangles (x, y, w, h) of an unwrapped box."""
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "left": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "right": (u + d + w, v + d, d, h),
        "back": (u + 2 * d + w, v + d, w, h),
    }


# ------------------------------------------------------------------ TNT sticks

STICK_COLS = [0.80, 1.00, 1.20, 0.88]  # shade per column: lit centre, darker edges (about three red levels)


def paint_stick(sh):
    base, hi, lo = hx("#841414"), hx("#A41818"), hx("#4C0C0C")
    for name, (x, y, w, h) in box_rects(0, 0, 4, 12, 4).items():
        for j in range(h):
            for i in range(w):
                if name in ("top", "bottom"):
                    centre = 1 <= i <= w - 2 and 1 <= j <= h - 2
                    c = hx("#7C1414") if centre else hx("#580C0C")
                    sh.put(x + i, y + j, c, 2)
                    continue
                k = STICK_COLS[i % 4]
                c = base * k if k <= 1 else base + (hi - base) * (k - 1) / 0.2 * 0.8
                if j in (0, h - 1):  # paper edge at the ends
                    c = c * 0.8
                if j in (3, 8):  # wrap seams
                    c = c * 0.85
                sh.put(x + i, y + j, np.maximum(c, lo), 3)


# ------------------------------------------------------------------ plate

GLYPHS = {  # 2 wide, 4 tall, blocky "RDT" (the plate front is only 8 x 4)
    "R": ["##", "##", "#.", "#."],
    "D": ["#.", "##", "##", "#."],
    "T": ["##", ".#", ".#", ".#"],
}


def paint_plate(sh, armed):
    gray, hl, sd = hx("#4C4C4C"), hx("#606060"), hx("#3C3C3C")
    field, green = hx("#103020"), hx("#18A030")
    if armed:  # indicator going red-pink on the five outward faces
        gray, hl, sd = hx("#B03848"), hx("#D05060"), hx("#802030")
        field, green = hx("#3A1018"), hx("#FF6070")
    for name, (x, y, w, h) in box_rects(0, 16, 8, 4, 2).items():
        for j in range(h):
            for i in range(w):
                c = gray.copy()
                if name == "back":
                    c = hx("#4C4C4C")  # the rear never recolors
                    sd_c, hl_c = hx("#3C3C3C"), hx("#606060")
                else:
                    sd_c, hl_c = sd, hl
                if j == 0 or i == 0:
                    c = hl_c
                elif j == h - 1 or i == w - 1:
                    c = sd_c
                if name == "front":
                    c = field
                sh.put(x + i, y + j, c, 2)
    # lettering on the front face: R D T, one blank column between letters (x offsets 0, 3, 6)
    fx, fy = box_rects(0, 16, 8, 4, 2)["front"][:2]
    for letter, ox in zip("RDT", (0, 3, 6)):
        for j, row in enumerate(GLYPHS[letter]):
            for i, ch in enumerate(row):
                if ch == "#":
                    sh.put(fx + ox + i, fy + j, green)


def tnt(armed):
    sh = Sheet(np.random.default_rng(seed_for("model.charge.tnt" + (".armed" if armed else ""))))
    paint_stick(sh)
    paint_plate(sh, armed)
    return sh


# ------------------------------------------------------------------ exp brick


def paint_brick(sh):
    olive_a, olive_b = hx("#242C24"), hx("#2C342C")
    strap, strap_hi, strap_lo = hx("#344444"), hx("#3C4C4C"), hx("#2C3C3C")
    edge = hx("#1C241C")
    for name, (x, y, w, h) in box_rects(0, 0, 10, 10, 5).items():
        for j in range(h):
            for i in range(w):
                c = olive_a if (i + j) % 2 == 0 else olive_b
                on = False
                if name in ("front", "back"):
                    on = 3 <= j <= 6 or 1 <= i <= 3 or 6 <= i <= 8  # mid band + a strap near each side edge
                elif name in ("top", "bottom"):
                    on = 1 <= i <= 3 or 6 <= i <= 8  # two stripes near the left and right ends
                else:  # left, right: horizontal strap at mid height
                    on = 3 <= j <= 6
                if on:
                    c = strap
                    if name in ("front", "back", "left", "right") and (j == 3 or j == 6) and 3 <= j <= 6:
                        c = strap_hi if j == 3 else strap_lo
                elif i in (0, w - 1) or j in (0, h - 1):
                    c = edge
                sh.put(x + i, y + j, c, 2)


def exp(armed):
    sh = Sheet(np.random.default_rng(seed_for("model.charge.exp" + (".armed" if armed else ""))))
    paint_brick(sh)
    paint_plate(sh, armed)
    return sh


def main():
    os.makedirs(OUT, exist_ok=True)
    rows = []
    for name, fn in (("tnt_charge", tnt), ("exp_charge", exp)):
        for armed in (False, True):
            stem = name + ("_armed" if armed else "")
            fn(armed).save(os.path.join(OUT, stem + ".png"))
            key = "model.charge." + stem
            seed = seed_for("model.charge.%s%s" % (name.split("_")[0], ".armed" if armed else ""))
            rows.append([key, "src/main/resources/assets/gregguns/textures/models/charges/%s.png" % stem,
                         "tools/asset-gen/charge_models.py", seed,
                         "Procedural numpy/Pillow: 32x32 box unwrap painted from the clean-room model notes palette; "
                         "armed variant recolors the detonator plate", "MIT", DATE])
    with open(CREDITS, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["asset", "path", "generator", "seed", "method", "license", "date"])
        w.writerows(rows)
    print("wrote", len(rows), "textures to", OUT)


if __name__ == "__main__":
    main()
