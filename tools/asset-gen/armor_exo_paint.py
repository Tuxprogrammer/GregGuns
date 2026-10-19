#!/usr/bin/env python3
"""Palettes and painter for the Exo Suit sheets (t3_exo, t3_exo_silver, t3_exo_green).

Imported by armor_models.py. The painter follows docs/techguns-spec/05-armor/model-notes/t3_exo.md: a near-black
undersuit (the vanilla-shaped layers) under flat gray bolt-on plates, a pale open-faced helmet with a painted visor,
small red (or white) marks and a chest indicator light. All art is procedural; each material row is
(base, light, dark) taken from the notes' tables.

Painting frame: every limb box is authored for the right limb, so face "right" (the u..u+d rectangle) is the outer
side of both limbs and column 0 of a front or back face is the outer edge; the left limb is the mirrored copy.
"""
import numpy as np
from PIL import Image

from modeltex import hx

# material -> (base, light, dark). Undersuit rows: helmet, torso, arms, legs (chest/boots sheet), lbody, llegs.
_DEFAULT = dict(
    helmet=("#A7B3BE", "#EDF5F8", "#2C2C2C"), torso=("#737F86", "#E0E9EC", "#161C25"),
    arms=("#59656E", "#C9D9DF", "#0F151E"), legs=("#556169", "#B3CCD3", "#181818"),
    pauldron=("#C2D1D8", "#EBF0F2", "#8EAFBB"), cuff=("#ABABAB", "#E8E8E8", "#777777"),
    rod=("#949494", "#B0B0B0", "#777777"), joint=("#848484", "#9D9D9D", "#737373"),
    bar=("#828282", "#AFAFAF", "#4A4A4A"), spine=("#7F7F7F", "#ABABAB", "#4C4C4C"),
    shin=("#898989", "#BABABA", "#727272"), ankle=("#ACACAC", "#E8E8E8", "#7B7B7B"),
    lbody=("#1D232B", "#2B313A", "#14181E"), llegs=("#232A33", "#2E343D", "#15191F"),
    belt=("#B0B0B0", "#E9E9E9", "#7B7B7B"), crotch=("#909090", "#AEAEAE", "#747474"),
    knee=("#A8A8A8", "#E3E3E3", "#787878"), legrod=("#989898", "#B5B5B5", "#797979"))

_SILVER = dict(
    helmet=("#ADABA4", "#E9E9E9", "#2E2E2E"), torso=("#6D6D6C", "#D7D7D7", "#131313"),
    arms=("#545454", "#C2C2C2", "#0E0E0E"), legs=("#515151", "#AFAFAF", "#131313"),
    pauldron=("#A8A8A8", "#DADADA", "#707070"), cuff=("#858585", "#CBCBCB", "#484848"),
    rod=("#696969", "#888888", "#464646"), joint=("#5B5B5B", "#787878", "#454545"),
    bar=("#5C5C5C", "#888888", "#393939"), spine=("#5D5D5D", "#888888", "#3F3F3F"),
    shin=("#5D5D5D", "#8D8D8D", "#454545"), ankle=("#858585", "#CBCBCB", "#4E4E4E"),
    lbody=("#111111", "#1D1D1D", "#080808"), llegs=("#171717", "#1F1F1F", "#090909"),
    belt=("#8A8A8A", "#CBCBCB", "#494949"), crotch=("#656565", "#888888", "#454545"),
    knee=("#808080", "#CBCBCB", "#484848"), legrod=("#6C6C6C", "#898989", "#484848"))

_GREEN = dict(
    helmet=("#1C2018", "#2E3327", "#0C0D0A"), torso=("#171C12", "#272B22", "#0A0C0A"),
    arms=("#13180E", "#1E2E14", "#060804"), legs=("#10150C", "#212A18", "#060705"),
    pauldron=("#0F100D", "#151712", "#050605"), cuff=("#181818", "#1F1F1F", "#111111"),
    rod=("#1B1B1B", "#1F1F1F", "#181818"), joint=("#1D1D1D", "#292929", "#191919"),
    bar=("#1D1D1D", "#2A2A2A", "#181818"), spine=("#1C1C1C", "#282828", "#181818"),
    shin=("#1C1C1C", "#1F1F1F", "#181818"), ankle=("#181818", "#1F1F1F", "#111111"),
    lbody=("#151F0E", "#243417", "#0A0F07"), llegs=("#151F0E", "#233216", "#0A0F08"),
    belt=("#171717", "#1F1F1F", "#111111"), crotch=("#1B1B1B", "#1F1F1F", "#181818"),
    knee=("#181818", "#1F1F1F", "#111111"), legrod=("#1A1A1A", "#1F1F1F", "#181818"))

# Extras per variant: undersuit band colors (two alternating rows), helmet accents, marks, visor.
_EXTRA = {
    "t3_exo": dict(torso_b=("#282838", "#181828"), arms_b=("#0F151E", "#0F151E"), legs_b=("#181818", "#181818"),
                   lbody_b=("#14181E", "#14181E"), llegs_b=("#15191F", "#15191F"),
                   helmet_peak="#FFFFFF", helmet_spark="#EDF5F8",
                   visor=("#0A0C14", "#00127A", "#0B43FF"), mark=("#E10000", "#FF0000"), glyph=None),
    "t3_exo_silver": dict(torso_b=("#282828", "#181818"), arms_b=("#0E0E0E", "#0E0E0E"),
                          legs_b=("#131313", "#131313"), lbody_b=("#080808", "#080808"),
                          llegs_b=("#090909", "#090909"), helmet_peak="#FFFFFF", helmet_spark="#E9E9E9",
                          visor=("#0B0B0B", "#161616", "#262626"), mark=None, glyph=None),
    "t3_exo_green": dict(torso_b=("#464F3C", "#2F3829"), arms_b=("#2E451F", "#1E2E14"),
                         legs_b=("#273B1B", "#1A2612"), lbody_b=("#2D411E", "#243417"),
                         llegs_b=("#395524", "#2D411E"), helmet_peak="#4E5845", helmet_spark="#3A4231",
                         visor=("#050505", "#0B0B0B", "#141414"), mark=None, glyph="#FFFFFF"),
}


def _flatten(rows, extra):
    pal = {}
    for k, (b, l, d) in rows.items():
        pal[k], pal[k + "_l"], pal[k + "_d"] = b, l, d
    pal.update({k: v for k, v in extra.items()})
    return pal


PALETTES = {"t3_exo": _flatten(_DEFAULT, _EXTRA["t3_exo"]),
            "t3_exo_silver": _flatten(_SILVER, _EXTRA["t3_exo_silver"]),
            "t3_exo_green": _flatten(_GREEN, _EXTRA["t3_exo_green"])}

UNDERSUIT = ("torso", "arms", "legs", "lbody", "llegs")

_GLYPHS = {"7": ["111", "001", "010", "010", "010"], "3": ["111", "001", "011", "001", "111"]}


def face_rects(b):
    u, v = b["uv"]
    w, h, d = b["size"]
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def _mix(a, b, t):
    return np.asarray(a, float) * (1 - t) + np.asarray(b, float) * t


class _Face:
    """A float RGB tile plus an alpha mask, with small drawing helpers (rows and columns clip silently)."""

    def __init__(self, w, h, base):
        self.w, self.h = w, h
        self.c = np.tile(np.asarray(base, float), (h, w, 1))
        self.a = np.ones((h, w), bool)

    def px(self, x, y, col, t=1.0):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.c[y, x] = _mix(self.c[y, x], col, t)

    def row(self, y, col, t, x0=0, x1=None):
        if 0 <= y < self.h:
            x1 = self.w if x1 is None else x1
            self.c[y, x0:x1] = _mix(self.c[y, x0:x1], col, t)

    def col(self, x, col, t, y0=0, y1=None):
        if 0 <= x < self.w:
            y1 = self.h if y1 is None else y1
            self.c[y0:y1, x] = _mix(self.c[y0:y1, x], col, t)

    def rect(self, x0, y0, x1, y1, col, t=1.0):
        x0, y0 = max(x0, 0), max(y0, 0)
        x1, y1 = min(x1, self.w), min(y1, self.h)
        if x1 > x0 and y1 > y0:
            self.c[y0:y1, x0:x1] = _mix(self.c[y0:y1, x0:x1], col, t)

    def noise(self, r, amp):
        self.c *= 1 + amp * (r.random((self.h, self.w, 1)) - 0.5)

    def bevel(self, light, dark, k=1.0):
        if self.w >= 3 and self.h >= 3:
            self.row(0, light, 0.45 * k)
            self.col(0, light, 0.22 * k)
            self.row(self.h - 1, dark, 0.45 * k)
            self.col(self.w - 1, dark, 0.25 * k)


def _mark(f, x, y, pal, key):
    """A small L (red) or glyph (white) accent; silver has none."""
    if pal["glyph"]:
        g = _GLYPHS["7" if (x + y) % 2 else "3"]
        for j, line in enumerate(g):
            for i, ch in enumerate(line):
                if ch == "1":
                    f.px(x + i, y + j, hx(pal["glyph"]))
    elif pal["mark"]:
        m0, m1 = hx(pal["mark"][0]), hx(pal["mark"][1])
        for (i, j) in ((0, 0), (0, 1), (1, 1), (2, 1)):
            f.px(x + i, y + j, m0)
        f.px(x, y, m1)


def _plate_face(b, face, w, h, pal, r):
    """Flat gray plate: light top, dark underside, bevel, a panel seam, and per-part ribbing."""
    m = b["material"]
    base, light, dark = hx(pal[m]), hx(pal[m + "_l"]), hx(pal[m + "_d"])
    f = _Face(w, h, base)
    f.noise(r, 0.05)
    if face == "top":
        f.c[:] = _mix(f.c, light, 0.7)
    elif face == "bottom":
        f.c[:] = _mix(f.c, dark, 0.35)
    f.bevel(light, dark)
    big = max(w, h) >= 5 and min(w, h) >= 3
    side = face in ("front", "back", "right", "left")
    if m in ("rod", "legrod") and side:
        for y in range(2, h - 1, 3):
            f.row(y, dark, 0.6)
    elif m == "spine":
        if face == "back":
            c = (w - 1) / 2.0
            for y0 in range(0, h - 2, 3):
                for x in range(w):
                    f.px(x, y0 + int(round(abs(x - c))), dark, 0.75)
        elif side:
            for y in range(2, h - 1, 3):
                f.row(y, dark, 0.5)
    elif m == "bar" and side and face in ("front", "back"):
        f.col(1, dark, 0.55)
        f.col(w - 2, dark, 0.55)
    elif m == "joint" and side and w >= 3 and h >= 3:
        f.px(w // 2, h // 2, dark, 0.9)
        f.px(w // 2 - 1, h // 2 - 1, light, 0.5)
    elif m == "shin" and face == "front":
        f.col(w // 2, dark, 0.5, 1, h - 1)
    elif m in ("pauldron", "belt", "cuff", "knee", "ankle", "crotch") and big and side:
        f.row(h // 2 if h >= 4 else 1, dark, 0.45)
        if m == "pauldron" and face in ("front", "back"):
            f.col(w // 2, dark, 0.4)
    if m == "pauldron" and face == "top":
        _mark(f, 1, 1, pal, "p")
    return f


def _bands(f, face, rows, cols):
    if face in ("front", "back", "right", "left"):
        for y, c in rows:
            f.row(y, c, 0.9)


def _undersuit_face(b, face, w, h, pal, r):
    m = b["material"]
    base, light, dark = hx(pal[m]), hx(pal[m + "_l"]), hx(pal[m + "_d"])
    f = _Face(w, h, base)
    f.noise(r, 0.05)
    if face == "top":
        f.c[:] = _mix(f.c, light, 0.75 if m in ("torso", "arms") else 0.4)
        f.bevel(light, dark, 0.6)
        return f
    if face == "bottom":
        f.c[:] = _mix(f.c, light if m == "legs" else dark, 0.45)
        f.bevel(light, dark, 0.5)
        return f
    f.bevel(light, dark, 0.5)
    b1 = hx(pal[m + "_b"][0]) if (m + "_b") in pal else dark
    b2 = hx(pal[m + "_b"][1]) if (m + "_b") in pal else dark
    if m == "torso":
        _bands(f, face, [(h - 3, b1), (h - 2, b2)], None)
        if face == "front":
            # painted chest plate: a lighter panel framed in dark, with the indicator light
            f.rect(1, 1, w - 1, 7, light, 0.85)
            f.rect(1, 1, w - 1, 7, base, 0.25)
            f.row(1, dark, 0.6, 1, w - 1)
            f.row(6, dark, 0.6, 1, w - 1)
            f.col(1, dark, 0.6, 1, 7)
            f.col(w - 2, dark, 0.6, 1, 7)
            f.col(w // 2 - 1, dark, 0.35, 2, 6)
            f.rect(2, 2, 6, 4, (14, 14, 16))
            for x, c in ((2, "#E10000"), (3, "#FFDD00"), (4, "#22CC22"), (5, "#E10000")):
                f.px(x, 3, hx(c))
            _mark(f, 1, 7, pal, "t")
    elif m == "arms":
        _bands(f, face, [(h - 3, b1), (h - 2, b2)], None)
        if face in ("front", "back"):
            f.col(0, dark, 0.3)
    elif m == "legs":
        _bands(f, face, [(h - 3, b1), (h - 2, b2)], None)
        if face == "front":
            f.col(1, light, 0.7, 1, h - 3)  # a pale front stripe
            f.col(w - 1, light, 0.35, 5, h - 3)
    elif m == "lbody":
        _bands(f, face, [(h - 2, b1), (h - 1, b2)], None)
    elif m == "llegs":
        _bands(f, face, [(3, b1), (h - 4, b2)], None)
        if face == "front":
            _mark(f, 1, 1, pal, "t")
    return f


def _helmet_face(b, face, w, h, pal, r):
    base, light, dark = hx(pal["helmet"]), hx(pal["helmet_l"]), hx(pal["helmet_d"])
    peak, spark = hx(pal["helmet_peak"]), hx(pal["helmet_spark"])
    f = _Face(w, h, base)
    f.noise(r, 0.05)
    if face == "top":
        f.c[:] = _mix(f.c, light, 0.9)
        for _ in range(4):  # a few brightest pixels
            f.px(int(r.integers(1, w - 1)), int(r.integers(1, h - 1)), peak, 0.9)
        f.bevel(light, dark, 0.5)
        return f
    if face == "bottom":
        f.a[:] = False
        f.a[0, :] = f.a[-1, :] = True
        f.a[:, 0] = f.a[:, -1] = True
        f.c[:] = _mix(f.c, dark, 0.8)
        return f
    f.row(0, light, 0.55)
    f.row(1, light, 0.2)
    f.bevel(light, dark, 0.5)
    for _ in range(2):
        f.px(int(r.integers(1, w - 1)), int(r.integers(1, 4)), spark, 0.8)
    if face == "front":
        vbase, vdeep, vhi = hx(pal["visor"][0]), hx(pal["visor"][1]), hx(pal["visor"][2])
        f.row(2, dark, 0.95)
        f.rect(0, 3, w, 5, vbase)
        f.col(0, dark, 0.95, 3, 5)
        f.col(w - 1, dark, 0.95, 3, 5)
        f.col(3, dark, 0.95, 3, 5)
        f.col(4, dark, 0.95, 3, 5)
        for x in (1, 2, 5, 6):
            f.px(x, 3, vdeep)
        f.px(1, 3, vhi)
        f.px(5, 3, vhi)
        f.a[6:, :] = False
    else:
        f.row(4, dark, 0.35)
        f.a[h - 1:, :] = False
    return f


def paint_exo(model, key):
    """Paint the sheet for variant `key` to fit the model's current UV layout."""
    from modeltex import rng_for
    pal = PALETTES[key]
    r = rng_for("armor_models." + key)
    rgb = np.zeros((model.H, model.W, 3), float)
    alpha = np.zeros((model.H, model.W), bool)
    done = set()
    for b in model.boxes:
        if b["group"] in done:
            continue
        done.add(b["group"])
        m = b["material"]
        for face, (x, y, w, h) in face_rects(b).items():
            if m == "helmet":
                f = _helmet_face(b, face, w, h, pal, r)
            elif m in UNDERSUIT:
                f = _undersuit_face(b, face, w, h, pal, r)
            else:
                f = _plate_face(b, face, w, h, pal, r)
            rgb[y:y + h, x:x + w] = f.c
            alpha[y:y + h, x:x + w] = f.a
    out = np.zeros((model.H, model.W, 4), np.uint8)
    out[..., :3] = np.clip(rgb + 0.5, 0, 255).astype(np.uint8)
    out[..., 3] = np.where(alpha, 255, 0)
    out[~alpha] = 0
    return Image.fromarray(out, "RGBA")
