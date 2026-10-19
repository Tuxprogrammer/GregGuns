"""Region-based repaint of the vanilla-layout armor sheets (review chunk J).

Paints the 128x64 sheets (the vanilla 64x32 armor unwrap at 2x) for the nine Overhaul sets and the Adjust
variants, both layers (`_1` = helmet/chestplate/boots, `_2` = leggings). Style rules: flat materials with edge
highlights and a few panel seams, smooth low-contrast blotches for camo, no dot grids, no high-contrast speckle,
accents on roughly a tenth of the surface.

Every sheet is a set of named *regions* (helmet, visor, torso, vest, ...). `GEOM` maps a region name to
rectangles on the unwrap; each set config maps region names to a material. Variants are derived from their base
config, which is what `armor_variant_check.py` relies on.

Seeded and deterministic: re-running reproduces the files byte for byte.
Run: python -I -X utf8 tools/asset-gen/armor_repaint.py [--out DIR]
Run it AFTER models.py: models.py also writes these sheets (its output is superseded).
"""

import argparse
import csv
import os
import zlib

import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ARMOR_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", "gregguns", "textures", "models", "armor")
CREDITS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "credits", "armor_repaint.csv")
SCALE = 2  # 128 / 64
W, H = 128, 64

# ---- geometry (vanilla armor unwrap, logical 64x32 units) -----------------------------------------
PARTS = {"head": (0, 0, 8, 8, 8), "body": (16, 16, 8, 12, 4), "arm": (40, 16, 4, 12, 4), "leg": (0, 16, 4, 12, 4)}
ALL = ("top", "bottom", "front", "back", "left", "right")
RING = ("front", "back", "left", "right")
FB = ("front", "back")
SIDES = ("left", "right")
NOFRONT = ("back", "left", "right")
FRONT = ("front",)


def faces(part):
    u, v, w, h, d = PARTS[part]
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


# region -> [(part, row0, row1, cols|None, face selection)]  (rows/cols in logical units, face-local)
GEOM = {
    "helmet": [("head", 0, 8, None, ALL)],
    "headband": [("head", 2, 3.5, None, RING)],
    "strap": [("head", 3.5, 5.5, None, NOFRONT)],
    "visor": [("head", 3.5, 6, (1, 7), FRONT)],
    "glasses": [("head", 3.5, 5, (1, 7), FRONT)],
    "lamp": [("head", 1, 3, (3, 5), FRONT)],
    "torso": [("body", 0, 12, None, ALL)],
    "chestband": [("body", 0, 2, None, ALL)],
    "vest": [("body", 0, 9, (1, 7), FB), ("body", 1, 8, None, SIDES)],
    "belt": [("body", 9, 10.5, None, RING)],
    "emblem": [("body", 2, 4, (3, 5), FRONT)],
    "sleeve": [("arm", 0, 12, None, ALL)],
    "shoulder": [("arm", 0, 3.5, None, ALL)],
    "band": [("arm", 3.5, 5.5, None, RING)],
    "band2": [("arm", 7, 8.5, None, RING)],
    "glove": [("arm", 9.5, 12, None, ALL)],
    "pants": [("leg", 0, 12, None, ALL)],
    "thigh": [("leg", 0, 5, None, ALL)],
    "pouch": [("leg", 1, 4.5, (0.5, 3.5), FRONT)],
    "knee": [("leg", 5, 7, None, RING)],
    "boot": [("leg", 8.5, 12, None, ALL)],
    "trim": [("arm", 9, 9.5, None, RING), ("leg", 8, 8.5, None, RING)],
}
LAYER2 = ("torso", "belt", "sleeve", "pants", "thigh", "pouch", "knee")  # leggings layer: body and legs


def region_rects(name, layer=1):
    """Pixel rectangles (x0, y0, x1, y1), x1/y1 exclusive, of a region on the sheet."""
    out = []
    for part, r0, r1, cols, sel in GEOM[name]:
        ph = PARTS[part][3]
        for f in sel:
            if f in ("top", "bottom"):
                if (f == "top" and r0 > 0) or (f == "bottom" and r1 < ph):
                    continue
            fx, fy, fw, fh = faces(part)[f]
            if f in ("top", "bottom"):
                x0, x1, y0, y1 = 0, fw, 0, fh
            else:
                x0, x1 = (cols if (cols and f in FB) else (0, fw))
                y0, y1 = r0, r1
            out.append((int((fx + x0) * SCALE), int((fy + y0) * SCALE), int((fx + x1) * SCALE), int((fy + y1) * SCALE)))
    return out


# ---- noise fields ---------------------------------------------------------------------------------
def smooth(rng, h, w, cell):
    g = rng.random((h // cell + 3, w // cell + 3))
    ys, xs = np.arange(h) / cell, np.arange(w) / cell
    y0, x0 = ys.astype(int), xs.astype(int)
    fy, fx = ys - y0, xs - x0
    fy, fx = fy * fy * (3 - 2 * fy), fx * fx * (3 - 2 * fx)
    a = g[y0][:, x0] * (1 - fx) + g[y0][:, x0 + 1] * fx
    b = g[y0 + 1][:, x0] * (1 - fx) + g[y0 + 1][:, x0 + 1] * fx
    return a * (1 - fy[:, None]) + b * fy[:, None]


class Fields:
    def __init__(self, seed):
        rng = np.random.default_rng(seed)
        self.n6 = smooth(rng, H, W, 6)
        self.n3 = smooth(rng, H, W, 3)
        self.grain = rng.random((H, W)) - 0.5
        # horizontal streaks: low-resolution noise stretched along x
        self.streak = np.repeat(smooth(rng, H, W // 6 + 3, 1)[:, : W // 6 + 1], 6, axis=1)[:, :W]
        f = 0.65 * self.n6 + 0.35 * self.n3
        order = f.ravel().argsort().argsort()
        self.rank = (order / f.size).reshape(H, W)


def hexrgb(h):
    h = h.lstrip("#")
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], float)


# ---- materials ------------------------------------------------------------------------------------
def material(spec, F, x0, y0, x1, y1):
    kind = spec[0]
    sl = (slice(y0, y1), slice(x0, x1))
    h, w = y1 - y0, x1 - x0
    if kind == "camo":
        cols = [hexrgb(c) for c in spec[1]]
        idx = np.minimum((F.rank[sl] * len(cols)).astype(int), len(cols) - 1)
        base = np.array(cols)[idx]
        return base * (1 + 0.025 * F.grain[sl])[..., None]
    c = hexrgb(spec[1])
    if kind == "flat":
        m = 1 + 0.03 * F.grain[sl]
    elif kind == "cloth":
        m = 1 + 0.10 * (F.n3[sl] - 0.5) + 0.03 * F.grain[sl]
    elif kind == "leather":
        m = 1 + 0.20 * (F.streak[sl] - 0.5) + 0.03 * F.grain[sl]
    elif kind == "plate":
        m = 1 + 0.06 * (F.n6[sl] - 0.5) + 0.02 * F.grain[sl]
    elif kind == "glass":
        m = (1.12 - 0.3 * np.linspace(0, 1, max(h, 2))[:h])[:, None] * np.ones((1, w)) + 0.02 * F.grain[sl]
    elif kind == "mark":  # small emblem: spec = (mark, bg, fg, shape)
        m = 1 + 0.03 * F.grain[sl]
    else:
        raise ValueError(kind)
    return c * m[..., None]


SHAPES = {  # 4x4 pixel masks, rows top to bottom
    "L": ["X...", "X...", "X...", "XXX."],
    "ring": [".XX.", "X..X", "X..X", ".XX."],
}


def paint_region(img, alpha, rect, spec, F):
    x0, y0, x1, y1 = rect
    px = material(spec, F, x0, y0, x1, y1)
    h, w = y1 - y0, x1 - x0
    kind = spec[0]
    if kind == "mark":
        px = material(("flat", spec[1]), F, x0, y0, x1, y1)
        mask = SHAPES[spec[3]]
        for j in range(min(h, 4)):
            for i in range(min(w, 4)):
                if mask[j][i] == "X":
                    px[j, i] = hexrgb(spec[2])
    if kind in ("plate", "glass", "leather") and h >= 4 and w >= 4:
        px[0] *= 1.12  # edge highlight on the top row
        px[-1] *= 0.84  # shadow on the bottom row
        px[1:-1, 0] *= 1.04
        if kind == "plate" and h >= 16 and w >= 10:
            px[int(h * 0.62)] *= 0.80  # one panel seam
    img[y0:y1, x0:x1] = np.clip(px, 0, 255)
    alpha[y0:y1, x0:x1] = 255


# ---- set configs ----------------------------------------------------------------------------------
def derive(base, **over):
    mat = dict(base)
    for k, v in over.items():
        if v is None:
            mat.pop(k, None)
        else:
            mat[k] = v
    return mat


OLIVE = ("camo", ["#6A6E4C", "#5C6142", "#767A58", "#4F5339"])
ARCTIC = ("camo", ["#E4E8EA", "#C4CBD0", "#9CA5AC", "#737C84"])
DESERT = ("camo", ["#CDB48C", "#B79C74", "#DFCCA6", "#9A8260"])
WOOD = ("camo", ["#36512A", "#46633A", "#27381C", "#5A4630"])

T2_HARD = dict(belt=("leather", "#1E1F20"), glove=("cloth", "#25272A"), pouch=("plate", "#2A2B2B"),
               knee=("plate", "#2B2D2D"), boot=("leather", "#25231F"), visor=("glass", "#12161C"))


def t2(camo, vest, helmet=None):
    return dict(T2_HARD, helmet=helmet or camo, torso=camo, sleeve=camo, pants=camo, vest=("plate", vest))


SETS = {}
SETS["t1_combat"] = dict(
    helmet=("cloth", "#CDC8B4"), visor=("cloth", "#A8A290"), torso=("leather", "#6E4C2E"),
    vest=("cloth", "#7C5C3A"), belt=("leather", "#2B2118"), sleeve=("leather", "#6A492C"),
    shoulder=("leather", "#4F3622"), glove=("leather", "#33281E"), pants=("leather", "#7A5A38"),
    pouch=("leather", "#33281E"), knee=("plate", "#2D2A27"), boot=("leather", "#25201A"))

MINER = dict(
    helmet=("plate", "#E6C81E"), lamp=("glass", "#EDE6B8"), torso=("cloth", "#2E3A98"), vest=("plate", "#3C48B4"),
    belt=("leather", "#2A2B30"), sleeve=("cloth", "#2A2C38"), glove=("cloth", "#26272B"),
    pants=("cloth", "#2A3490"), knee=("plate", "#3A3B42"), boot=("leather", "#2C2D32"), trim=("flat", "#8A6A14"))
SETS["t1_miner"] = MINER
SETS["t1_miner_black"] = derive(MINER, helmet=("plate", "#E8731C"), torso=("cloth", "#1C1C20"),
                                vest=("plate", "#2C2C32"), pants=("cloth", "#1E1E22"))
SETS["t1_miner_green"] = derive(MINER, helmet=("plate", "#20C4D8"), torso=("cloth", "#1F4A28"),
                                vest=("plate", "#2E6C3C"), pants=("cloth", "#1C4424"))
SETS["t1_miner_red"] = derive(MINER, helmet=("plate", "#6CD11C"), torso=("cloth", "#A01C24"),
                              vest=("plate", "#C03540"), pants=("cloth", "#A01C24"))

SETS["t1_scout"] = dict(
    helmet=("cloth", "#B09A6C"), strap=("leather", "#2A2A28"), visor=("glass", "#5EA8B8"),
    torso=("cloth", "#C2AC7C"), vest=("cloth", "#A89468"), belt=("leather", "#2E2A24"),
    sleeve=("cloth", "#BBA574"), glove=("leather", "#2E2A24"), pants=("cloth", "#C0AA7A"),
    pouch=("leather", "#3A3328"), knee=("leather", "#4A4034"), boot=("leather", "#2A2824"))

SETS["t2_combat"] = t2(OLIVE, "#2F3530")
SETS["t2_combat_arctic"] = t2(ARCTIC, "#131416")
SETS["t2_combat_desert"] = t2(DESERT, "#4A3524")
SETS["t2_combat_wood"] = t2(WOOD, "#101214")
NAVY = ("cloth", "#1B2468")
SETS["t2_combat_security"] = dict(t2(NAVY, "#101113", helmet=("plate", "#16171A")))
SETS["t2_commando"] = dict(
    helmet=("plate", "#141516"), glasses=("glass", "#B8BEC4"), torso=("cloth", "#1A1B1D"),
    vest=("plate", "#26272A"), belt=("leather", "#2A2A2C"), sleeve=("cloth", "#18191B"),
    glove=("cloth", "#101112"), pants=("cloth", "#1A1B1D"), pouch=("plate", "#222325"),
    knee=("plate", "#262729"), boot=("leather", "#141414"))

T3C = dict(
    helmet=("plate", "#E4E8EE"), visor=("glass", "#2E6CE0"), torso=("cloth", "#202226"), vest=("plate", "#DCE2EA"),
    emblem=("mark", "#DCE2EA", "#C42A24", "L"), belt=("leather", "#16171A"), sleeve=("cloth", "#1C1E22"),
    shoulder=("plate", "#E0E6EE"), glove=("cloth", "#16171A"), pants=("cloth", "#1E2024"),
    knee=("plate", "#E4E8EE"), boot=("plate", "#26282C"))
SETS["t3_combat"] = T3C
SETS["t3_combat_green"] = derive(
    T3C, helmet=("plate", "#2C4A2A"), visor=("glass", "#0C0E10"), torso=("cloth", "#1E3A22"),
    vest=("plate", "#33563A"), emblem=("mark", "#33563A", "#9AA89A", "L"), sleeve=("cloth", "#27452B"),
    shoulder=("plate", "#33563A"), pants=("cloth", "#1C341F"), knee=("plate", "#2A4A2E"))
SETS["t3_combat_silver"] = derive(
    T3C, helmet=("plate", "#BFC6CE"), visor=("glass", "#F0C020"), vest=("plate", "#B8BFC8"), emblem=None,
    shoulder=("plate", "#B6BDC6"), knee=("plate", "#BCC3CB"))

HEV = dict(
    helmet=("plate", "#2A2C30"), headband=("flat", "#2C7FB0"), visor=("glass", "#0A0B0D"),
    torso=("cloth", "#2A2C30"), chestband=("plate", "#E68A10"), vest=("plate", "#303236"),
    belt=("plate", "#E68A10"), sleeve=("cloth", "#2A2C30"), band=("plate", "#E68A10"), band2=("plate", "#E68A10"),
    glove=("cloth", "#15161A"), pants=("plate", "#E08A12"), thigh=("plate", "#3A3C42"), knee=("plate", "#15161A"),
    boot=("plate", "#18191C"))
SETS["t3_hev"] = HEV
SETS["t3_hev_black"] = derive(
    HEV, helmet=("plate", "#0E0F10"), headband=("flat", "#B01818"), visor=("glass", "#B01818"),
    torso=("cloth", "#0E0F10"), chestband=("plate", "#161718"), vest=("plate", "#18191A"), belt=("plate", "#161718"),
    emblem=("mark", "#18191A", "#B01818", "ring"), sleeve=("cloth", "#101112"), band=("plate", "#161718"),
    pants=("cloth", "#0E0F10"), thigh=("plate", "#161718"), band2=("plate", "#161718"))
SETS["t3_hev_silver"] = derive(
    HEV, helmet=("plate", "#121315"), chestband=("plate", "#B8BEC6"), vest=("plate", "#B2B8C0"),
    belt=("plate", "#8C929A"), sleeve=("cloth", "#161719"), band=("plate", "#A8AEB6"),
    pants=("cloth", "#161719"), thigh=("plate", "#1A1B1E"), band2=("plate", "#A8AEB6"))

SOLO = ("t1_combat",)  # sets painted without a family (documentation only)

# Variant families: base -> variants. SHARED[variant][region] = reason the region is intentionally the same as
# the base set (checked by armor_variant_check.py).
FAMILIES = {
    "t1_miner": ["t1_miner_black", "t1_miner_green", "t1_miner_red"],
    "t2_combat": ["t2_combat_arctic", "t2_combat_desert", "t2_combat_wood", "t2_combat_security", "t2_combat_swat"],
    "t3_combat": ["t3_combat_green", "t3_combat_silver"],
    "t3_hev": ["t3_hev_black", "t3_hev_silver"],
    "t1_scout": ["t1_scout_black", "t1_scout_forest", "t1_scout_snow"],
}
_MINER_HW = "suit hardware (belt, sleeves, gloves, boots, knee pads, trim, lamp lens) is common to the miner family, as in the reference"
_T2_HW = "tactical hardware (belt, gloves, pouches, knee pads, boots, visor) is common to the combat family"
SHARED = {
    "t1_miner_black": {r: _MINER_HW for r in ("belt", "sleeve", "glove", "knee", "boot", "trim", "lamp")},
    "t1_miner_green": {r: _MINER_HW for r in ("belt", "sleeve", "glove", "knee", "boot", "trim", "lamp")},
    "t1_miner_red": {r: _MINER_HW for r in ("belt", "sleeve", "glove", "knee", "boot", "trim", "lamp")},
}
for _v in FAMILIES["t2_combat"]:
    SHARED[_v] = {r: _T2_HW for r in ("belt", "glove", "pouch", "knee", "boot", "visor")}
SHARED["t2_combat_swat"].update({r: "swat is an unrepainted dark set; dark gear regions coincide with the base's dark gear"
                                 for r in ("vest", "torso", "sleeve", "pants", "helmet")})
_T3_SUIT = "black under-suit (belt, gloves, boots, sleeves, pants) is common to the t3 combat family"
SHARED["t3_combat_green"] = {r: _T3_SUIT for r in ("belt", "glove", "boot")}
SHARED["t3_combat_silver"] = {r: _T3_SUIT for r in ("belt", "glove", "boot", "torso", "sleeve", "pants")}
_HEV_HW = "dark hev hardware (gloves, boots, knee pads) is common to the hev family"
SHARED["t3_hev_black"] = {r: _HEV_HW for r in ("glove", "boot", "knee")}
SHARED["t3_hev_silver"] = {r: _HEV_HW for r in ("glove", "boot", "knee", "headband", "visor", "torso")}
SHARED["t3_hev_silver"]["headband"] = "blue headband is the family signature (reference keeps it on the silver set)"
SHARED["t3_hev_silver"]["visor"] = "black visor glass is common to the hev family"
for _v in FAMILIES["t1_scout"]:
    SHARED[_v] = {r: "dark gear (belt, boots, strap) is black on every scout variant, as in the reference" for r in ("belt", "boot", "strap")}


def build(name, layer):
    """Return an (H, W, 4) uint8 array for set `name`, layer 1 or 2."""
    spec = SETS[name]
    seed = zlib.crc32(("%s_%d" % (name, layer)).encode())
    F = Fields(seed)
    img = np.zeros((H, W, 3), float)
    alpha = np.zeros((H, W), np.uint8)
    regions = [r for r in GEOM if r in spec]
    if layer == 2:
        regions = [r for r in regions if r in LAYER2]
    # the whole unwrap of every part gets a base cloth so no face is ever transparent
    for part in ("body", "arm", "leg") + (("head",) if layer == 1 else ()):
        base = spec["helmet"] if part == "head" else spec["torso"] if part == "body" else spec["sleeve"] if part == "arm" else spec["pants"]
        for f, (fx, fy, fw, fh) in faces(part).items():
            paint_region(img, alpha, (fx * SCALE, fy * SCALE, (fx + fw) * SCALE, (fy + fh) * SCALE), base, F)
    for r in regions:
        for rect in region_rects(r, layer):
            paint_region(img, alpha, rect, spec[r], F)
    out = np.dstack([img.astype(np.uint8), alpha])
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=ARMOR_DIR)
    args = ap.parse_args()
    os.makedirs(args.out, exist_ok=True)
    rows = []
    for name in SETS:
        for layer in (1, 2):
            path = os.path.join(args.out, "%s_%d.png" % (name, layer))
            Image.fromarray(build(name, layer), "RGBA").save(path, optimize=True)
            rel = "src/main/resources/assets/gregguns/textures/models/armor/%s_%d.png" % (name, layer)
            rows.append(["armor.%s_%d" % (name, layer), rel, "tools/asset-gen/armor_repaint.py",
                         zlib.crc32(("%s_%d" % (name, layer)).encode()),
                         "Procedural numpy/Pillow: region-based repaint of the vanilla armor unwrap, flat materials with edge highlights, panel seams, smooth camo blotches",
                         "MIT", "2026-10-18"])
    if args.out == ARMOR_DIR:
        with open(CREDITS, "w", encoding="utf-8", newline="") as f:
            w = csv.writer(f, lineterminator="\n")
            w.writerow(["asset", "path", "generator", "seed", "method", "license", "date"])
            w.writerows(rows)
    print("painted %d sheets" % len(rows))


if __name__ == "__main__":
    main()
