"""Paints the Camp Flag post sheet (64x64) for the box model in FlagPostGeometry.java.

Palette and artwork come from docs/techguns-spec/09-machines-utility/model-notes/flag_post.md (clean room).
Seeded and deterministic. Run: python -I -X utf8 tools/asset-gen/flagpost_paint.py
Box unwrap (Minecraft ModelBox): top (u+d, v), bottom (u+d+w, v), west (u, v+d), north (u+d, v+d),
east (u+d+w, v+d), south (u+2d+w, v+d). A flat panel (d = 0) has only north (u..u+w) and south (u+w..u+2w).
"""
import csv
import math
import os

import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "gregguns", "textures", "models", "machines", "flagpost.png")
SEED = 7120416
SIZE = 64


def hx(s):
    s = s.lstrip("#")
    return np.array([int(s[i:i + 2], 16) for i in (0, 2, 4)], float)


CONCRETE, CONC_HI, CONC_LO, RUST = hx("5F5D58"), hx("7A7570"), hx("3C3A37"), hx("6B4A38")
METAL, METAL_HI, METAL_LO = hx("C4C4C4"), hx("DCDCDC"), hx("949494")
WHITE, WHITE_HI, WHITE_LO = hx("D0D0D0"), hx("E0E0E0"), hx("A4A4A4")
RED, RED_HI, RED_LO = hx("B80808"), hx("C80808"), hx("980808")
BLUE, BLUE_HI, BLUE_LO = hx("0808A8"), hx("0808C8"), hx("080898")
STAR = hx("D8D8D8")


def put(img, x, y, c):
    img[y, x, :3] = np.clip(c, 0, 255)
    img[y, x, 3] = 255


def lerp(a, b, t):
    return a + (b - a) * t


def paint_face(img, x0, y0, w, h, kind, side, rng, top_lit=0.0):
    """One side face (w x h). kind: 'conc' or 'metal'. side: 0 lit .. 1 shaded (left to right shading)."""
    for j in range(h):
        for i in range(w):
            if kind == "conc":
                # darker at the bottom, lighter toward the top; grimy speckles
                t = 1.0 - j / max(1, h - 1)
                c = lerp(CONC_LO, CONC_HI, 0.25 + 0.5 * t * (0.4 + top_lit))
                c = lerp(CONCRETE, c, 0.6)
                c = c * (1.0 - 0.12 * side * (i / max(1, w - 1)))
                c = c + rng.normal(0, 4.0)
                if rng.random() < 0.05:
                    c = lerp(c, CONC_LO, 0.7)
                if rng.random() < 0.025:
                    c = RUST + rng.normal(0, 3.0)
            else:
                # light brushed metal: faint vertical streaks, lit left edge, shaded right edge
                c = METAL + (rng.normal(0, 1.0) if j == 0 else 0)
                c = c + streak(img, i)
                if w >= 3 and i == 0:
                    c = lerp(c, METAL_HI, 0.8)
                if i == w - 1:
                    c = lerp(c, METAL_LO, 0.7 if side > 0.4 else 0.45)
            put(img, x0 + i, y0 + j, c)


_streaks = {}


def streak(img, i):
    return _streaks.setdefault(i, ((i * 37) % 5 - 2) * 1.5)


def paint_top(img, x0, y0, w, d, kind, inset, rng):
    for j in range(d):
        for i in range(w):
            if kind == "conc":
                c = lerp(CONCRETE, CONC_HI, 0.55) + rng.normal(0, 3.0)
                if inset and 1 <= i < w - 1 and 1 <= j < d - 1:
                    c = lerp(c, CONC_HI, 0.65)  # lighter square inset on the plinth top
            else:
                c = lerp(METAL, METAL_HI, 0.9) + rng.normal(0, 1.5)
            put(img, x0 + i, y0 + j, c)


def paint_bottom(img, x0, y0, w, d, kind, rng):
    for j in range(d):
        for i in range(w):
            put(img, x0 + i, y0 + j, (CONC_LO if kind == "conc" else METAL_LO) + rng.normal(0, 2.0))


def paint_box(img, u, v, w, h, d, kind, rng, inset=False):
    paint_top(img, u + d, v, w, d, kind, inset, rng)
    paint_bottom(img, u + d + w, v, w, d, kind, rng)
    paint_face(img, u, v + d, d, h, kind, 0.3, rng)              # west
    paint_face(img, u + d, v + d, w, h, kind, 0.6, rng)          # north
    paint_face(img, u + d + w, v + d, d, h, kind, 1.0, rng)      # east
    paint_face(img, u + 2 * d + w, v + d, w, h, kind, 0.8, rng)  # south


def flag_art(rng):
    """The 18x14 cloth in flag coordinates: column 0 is at the pole, row 0 is the top. Returns float RGB."""
    W, H = 18, 14
    art = np.zeros((H, W, 3))
    cx, cy = 9.0, 7.0
    pts = []  # five-pointed star, one point up
    for k in range(10):
        r = 3.4 if k % 2 == 0 else 1.45
        a = -math.pi / 2 + k * math.pi / 5
        pts.append((cx + r * math.cos(a), cy + 0.2 + r * math.sin(a)))

    def in_star(px, py):
        inside = False
        j = len(pts) - 1
        for i in range(len(pts)):
            xi, yi = pts[i]
            xj, yj = pts[j]
            if (yi > py) != (yj > py) and px < (xj - xi) * (py - yi) / (yj - yi) + xi:
                inside = not inside
            j = i
        return inside

    for y in range(H):
        for x in range(W):
            px, py = x + 0.5, y + 0.5
            red = y in (1, 2, 11, 12)
            base, hi, lo = (RED, RED_HI, RED_LO) if red else (WHITE, WHITE_HI, WHITE_LO)
            if (px - cx) ** 2 + (py - cy) ** 2 <= 4.5 ** 2:
                base, hi, lo = BLUE, BLUE_HI, BLUE_LO
                if in_star(px, py):
                    base, hi, lo = STAR, STAR, STAR * 0.92
            # soft folds: a travelling ripple across the cloth plus fine crumple
            fold = math.sin(x * 0.9 + y * 0.35) * 0.5 + math.sin(x * 2.1 - y * 0.8) * 0.25
            c = base + (hi - base) * max(0.0, fold) - (base - lo) * max(0.0, -fold)
            c = c + rng.normal(0, 2.0)
            art[y, x] = c
    return art


def main():
    rng = np.random.default_rng(SEED)
    img = np.zeros((SIZE, SIZE, 4), np.uint8)
    paint_box(img, 0, 0, 10, 2, 10, "conc", rng)
    paint_box(img, 0, 12, 5, 14, 5, "conc", rng, inset=True)
    paint_box(img, 20, 12, 3, 23, 3, "metal", rng)
    paint_box(img, 32, 12, 2, 23, 2, "metal", rng)
    paint_box(img, 40, 0, 3, 2, 3, "metal", rng)
    art = flag_art(rng)
    for p in range(3):
        u0 = p * 12
        for c in range(6):
            for r in range(14):
                # north side (u..u+6): u grows toward +X, the flag grows toward -X, so it is mirrored.
                put(img, u0 + c, 50 + r, art[r, p * 6 + (5 - c)])
                # south side (u+6..u+12): u grows toward -X, with the flag.
                put(img, u0 + 6 + c, 50 + r, art[r, p * 6 + c] * 0.97)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    Image.fromarray(img, "RGBA").save(OUT)
    path = os.path.join(ROOT, "tools", "asset-gen", "credits", "flagpost.csv")
    with open(path, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["asset", "path", "generator", "seed", "method", "license", "date"])
        w.writerow(["model.machine.flagpost", "src/main/resources/assets/gregguns/textures/models/machines/flagpost.png",
                    "tools/asset-gen/flagpost_paint.py", SEED,
                    "Procedural numpy/Pillow: weathered concrete plinth, brushed-metal poles, white/red/blue star flag cloth painted onto the Java box unwrap",
                    "MIT", "2026-10-19"])
    print("wrote", OUT)


if __name__ == "__main__":
    main()
