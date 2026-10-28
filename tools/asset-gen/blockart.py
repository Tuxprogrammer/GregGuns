"""Casings and decorative block textures for GregGuns (block.casing.* and block.decor.*).

Every painter takes the manifest size and a seeded numpy Generator and returns a list of Canvas
frames (one frame for static textures). Connected-texture (CTM) sheets follow the 2x2 layout in
docs/techguns-spec/07-blocks/connected-textures.md, written from that description alone.
"""
from __future__ import annotations

import numpy as np

from blocktex import Canvas, fbm, hx, mix, shade, tile_noise
from machines import AMB, AMB_D, AMB_L, BLK, DK, HI, LT, MD, OUT, WHITE


# ---------------------------------------------------------------- shared helpers
def tone_fill(c: Canvas, base, rng, spread=0.1, cells=4, rect=None, steps=4) -> None:
    """Flat-tone value-noise fill: a handful of near colors, GT-ish rather than smooth."""
    x0, y0, x1, y1 = rect or (0, 0, c.w, c.h)
    w, h = x1 - x0, y1 - y0
    f = tile_noise(max(w, h), rng, cells)[:h, :w]
    for yy in range(h):
        for xx in range(w):
            k = int(f[yy, xx] * steps)
            k = min(k, steps - 1)
            fac = 1 + spread * ((k / (steps - 1)) * 2 - 1)
            c.px(x0 + xx, y0 + yy, shade(base, fac) if fac <= 1 else shade(base, fac))


def rivet(c: Canvas, x: int, y: int, base, big: bool = False) -> None:
    if big:
        c.rect(x, y, x + 2, y + 2, shade(base, 0.45))
        c.px(x, y, shade(base, 1.45))
    else:
        c.px(x, y, shade(base, 1.45))
        c.px(x + 1, y + 1, shade(base, 0.5))


def rust(c: Canvas, rng, amount: float = 0.35, cols=("#6b3a1a", "#8a4a1e", "#4d2a12"), cells=5) -> None:
    f = fbm(c.w, c.h, rng, cells, 3)
    cs = [hx(x) for x in cols]
    for y in range(c.h):
        for x in range(c.w):
            if c.a[y, x, 3] and f[y, x] > 1 - amount:
                k = int((f[y, x] - (1 - amount)) / amount * len(cs))
                c.px(x, y, cs[min(k, len(cs) - 1)])


def stripes(c: Canvas, x0, y0, x1, y1, a, b, period: int = 8, slope: int = 1) -> None:
    for y in range(y0, y1):
        for x in range(x0, x1):
            c.px(x, y, a if ((x + slope * y) // (period // 2)) % 2 == 0 else b)


def noisy(c: Canvas, rng, d=0.06) -> None:
    """Per-pixel +/- brightness jitter on opaque pixels (very light grain)."""
    for y in range(c.h):
        for x in range(c.w):
            if c.a[y, x, 3] == 255:
                v = rng.random()
                if v < 0.12:
                    c.a[y, x, :3] = shade(c.a[y, x, :3], 1 - d)
                elif v > 0.9:
                    c.a[y, x, :3] = shade(c.a[y, x, :3], 1 + d)


# ================================================================ CASINGS (16x16 opaque)
class Edges:
    """Which sides of a casing tile carry the outer frame, and which inner corners carry a notch.

    The connected-texture tiles (ctm.py) are cut from these: a side that touches another block of the same casing
    has no frame there, so a wall reads as one panel. Corners: 'tl', 'tr', 'bl', 'br'. A notch marks a corner whose
    two sides connect but whose diagonal neighbor is missing (the corner of a hole or an L-bend)."""

    __slots__ = ("t", "b", "l", "r", "notch")

    def __init__(self, t=True, b=True, l=True, r=True, notch=()):
        self.t, self.b, self.l, self.r, self.notch = int(t), int(b), int(l), int(r), tuple(notch)


ALL_EDGES = Edges()


def bevel_edges(c: Canvas, fr: Edges, hi, lo) -> None:
    """Canvas.bevel on the framed sides only. A side that ends at a framed corner stops one pixel short, exactly as
    Canvas.bevel does, so the all-framed case is the original bevel."""
    if fr.t:
        c.hline(0, 16 - fr.r, 0, hi)
    if fr.l:
        c.vline(0, 0, 16 - fr.b, hi)
    if fr.b:
        c.hline(fr.l, 16, 15, lo)
    if fr.r:
        c.vline(15, fr.t, 16, lo)


def notch_marks(c: Canvas, fr: Edges, hi, lo, ring=None) -> None:
    """The inner-corner pixel(s): the frame lines of the two neighbors meet here. A two-pixel frame adds its ring
    color one pixel in."""
    spots = {"tl": (0, 0, hi, 1, 1), "tr": (15, 0, lo, -1, 1), "bl": (0, 15, lo, 1, -1), "br": (15, 15, lo, -1, -1)}
    for k in fr.notch:
        x, y, col, dx, dy = spots[k]
        c.px(x, y, col)
        if ring is not None:
            c.px(x + dx, y + dy, ring)


def corner_rivets(c: Canvas, fr: Edges, base, spots, big: bool = False) -> None:
    """Rivets that belong to a frame corner: drawn only where both sides of that corner are framed.
    spots: ((x, y, 'tl'|'tr'|'bl'|'br'), ...)."""
    has = {"tl": fr.t and fr.l, "tr": fr.t and fr.r, "bl": fr.b and fr.l, "br": fr.b and fr.r}
    for (x, y, k) in spots:
        if has[k]:
            rivet(c, x, y, base, big)


def works_casing(rng, fr: Edges = ALL_EDGES):
    base = hx("#8f93a0")
    c = Canvas(16, fill=base)
    tone_fill(c, base, rng, 0.06, 3)
    for y in (3, 7, 11):                       # horizontal ribs
        c.hline(0, 16, y, shade(base, 0.55))
        c.hline(0, 16, y + 1, shade(base, 1.3))
    bevel_edges(c, fr, shade(base, 1.45), shade(base, 0.4))
    notch_marks(c, fr, shade(base, 1.45), shade(base, 0.4))
    corner_rivets(c, fr, base, ((1, 1, "tl"), (13, 1, "tr"), (1, 13, "bl"), (13, 13, "br")))
    for (x, y) in ((7, 5), (7, 9)):
        rivet(c, x, y, base)
    return [c]


def _ram_housing(f):
    base = hx("#34373f")
    c = Canvas(16, fill=base)
    c.bevel(0, 0, 16, 16, hx("#5b606b"), hx("#15171b"))
    c.rect(2, 1, 14, 12, OUT)                  # hood
    c.rect(3, 2, 13, 11, hx("#41454e"))
    c.hline(3, 13, 2, hx("#6a707c"))
    c.rect(4, 3, 12, 9, BLK)                   # slot
    c.hline(4, 12, 3, hx("#1e2026"))
    # piston rod and head in the slot
    top = 3 if f is None else [3, 4, 6, 4][f % 4]
    c.rect(7, 3, 9, top + 1, hx("#9aa0ac"))
    c.rect(5, top, 11, top + 2, hx("#b6bcc8"))
    c.hline(5, 11, top + 1, hx("#7c8290"))
    if f is not None and f % 4 in (1, 2):      # glow strip on the downstroke
        gl = [0, 170, 255, 0][f % 4]
        for x in range(4, 12):
            c.px(x, 8, mix(hx("#4a2408"), hx("#ff9020"), gl / 255))
        c.px(5, 8, mix(hx("#4a2408"), WHITE, gl / 255))
        c.hline(4, 12, 9, mix(hx("#26160a"), hx("#c05010"), gl / 255))
    # vents + rivets below the hood
    for x in (3, 5, 7, 9, 11, 13):
        c.vline(x, 13, 15, hx("#0d0e11"))
    for (x, y) in ((1, 1), (14, 1), (1, 12), (14, 12)):
        rivet(c, x, y, base)
    return c


def ram_housing(rng):
    return [_ram_housing(None)]


def ram_housing_active(rng):
    return [_ram_housing(i) for i in range(4)]


def reaction_casing(rng):
    base = hx("#d3d5da")
    c = Canvas(16, fill=base)
    tone_fill(c, base, rng, 0.04, 2)
    c.bevel(0, 0, 16, 16, WHITE, hx("#7d818c"))
    c.bevel(2, 2, 14, 14, hx("#a4a8b2"), WHITE)
    c.frame(1, 1, 15, 15, hx("#a9adb7"))
    for (cx, cy) in ((1, 1), (11, 1), (1, 11), (11, 11)):     # hazard-striped corner marks
        for y in range(3):
            for x in range(3):
                c.px(cx + x, cy + y, hx("#e0b020") if (x + y) % 2 == 0 else hx("#2a2a30"))
    return [c]


def gantry_casing(rng, fr: Edges = ALL_EDGES):
    base = hx("#5f7690")
    c = Canvas(16, fill=base)
    tone_fill(c, base, rng, 0.07, 3)
    bevel_edges(c, fr, hx("#8aa2bc"), hx("#2c3a4c"))
    notch_marks(c, fr, hx("#8aa2bc"), hx("#2c3a4c"))
    if fr.b:                                                   # yellow edge stripe: the wall's foot only
        c.rect(0, 13, 16, 15, hx("#e0b020"))
        c.hline(0, 16, 13, hx("#f4d04a"))
        c.hline(0, 16, 15, hx("#1c2430"))
    top, bottom = (1 if fr.t else 0), (12 if fr.b else 16)     # the seam groove runs on through a connected side
    c.vline(7, top, bottom, shade(base, 0.6))
    c.vline(8, top, bottom, shade(base, 1.3))
    corner_rivets(c, fr, base, ((2, 2, "tl"), (12, 2, "tr"), (2, 10, "bl"), (12, 10, "br")))
    return [c]


def gantry_beam(rng):
    c = Canvas(16, fill=hx("#2a2c31"))
    steel = hx("#7d838f")
    c.rect(0, 0, 16, 3, steel)                                 # top flange
    c.hline(0, 16, 0, hx("#b4bac6"))
    c.hline(0, 16, 2, hx("#4a4f59"))
    c.rect(0, 13, 16, 16, steel)                               # bottom flange
    c.hline(0, 16, 13, hx("#b4bac6"))
    c.hline(0, 16, 15, hx("#4a4f59"))
    # Yellow-black safety stripe on the web. The stripe period (8) and the rivet spacing (8) divide the 16-pixel
    # tile, so a row of beams reads as one unbroken stripe with no mark at the block seams.
    for y in range(3, 13):
        for x in range(16):
            c.px(x, y, hx("#e0b020") if ((x + y) // 4) % 2 == 0 else hx("#202226"))
    c.hline(0, 16, 3, hx("#14161a"))
    c.hline(0, 16, 12, hx("#14161a"))
    for x in (3, 11):
        rivet(c, x, 1, steel)
        rivet(c, x, 14, steel)
    return [c]


def gantry_hoist(rng):
    base = hx("#4b4f58")
    c = Canvas(16, fill=base)
    c.bevel(0, 0, 16, 16, hx("#7a808d"), hx("#1a1c21"))
    c.rect(1, 2, 15, 9, OUT)                                    # cable drum, wound cable
    for x in range(2, 14):
        col = hx("#b8bcc6") if x % 2 == 0 else hx("#6d727d")
        c.vline(x, 3, 8, col)
    c.hline(2, 14, 3, hx("#d6dae2"))
    c.hline(2, 14, 7, hx("#4c515c"))
    c.vline(8, 9, 12, hx("#c8ccd6"))                            # cable falling to the hook block
    c.rect(5, 12, 12, 15, OUT)
    c.rect(6, 12, 11, 14, hx("#d2a41a"))
    c.hline(6, 11, 12, hx("#f0d060"))
    c.rect(7, 14, 10, 15, hx("#9aa0ac"))
    c.px(2, 11, AMB)
    c.px(13, 11, AMB_D)
    return [c]


def repair_cradle_side(rng):
    base = hx("#454a54")
    c = Canvas(16, fill=base)
    c.bevel(0, 0, 16, 16, hx("#7a808d"), hx("#16181c"))
    c.rect(2, 2, 14, 14, OUT)                                    # padded insert
    c.rect(3, 3, 13, 13, hx("#17181c"))
    for y in (6, 9):
        for x in range(3, 13):
            if x % 2 == 0:
                c.px(x, y, hx("#2b2d34"))                        # stitching
    c.hline(3, 13, 3, hx("#2f323b"))
    for (x, y) in ((1, 1), (13, 1), (1, 13), (13, 13)):
        rivet(c, x, y, base)
    return [c]


def repair_cradle_top(rng):
    base = hx("#4d4f55")
    c = Canvas(16, fill=base)
    tone_fill(c, base, rng, 0.08, 4)
    c.speckle(0, 0, 16, 16, [hx("#3a3c42"), hx("#5d6068")], rng, 0.25)
    c.frame(0, 0, 16, 16, hx("#2b2d32"))
    for (x, y) in ((2, 3), (11, 3), (2, 10), (11, 10)):          # clamp slots
        c.rect(x, y, x + 3, y + 3, OUT)
        c.px(x, y, hx("#6a6d75"))
    return [c]


def _plate_base(base, rng, spread=0.05):
    c = Canvas(16, fill=base)
    tone_fill(c, base, rng, spread, 3)
    return c


def _ring(c: Canvas, fr: Edges, col) -> None:
    """Canvas.frame(1, 1, 15, 15) on the framed sides only: the inner ring behind the bevel."""
    x0, x1 = (1 if fr.l else 0), (15 if fr.r else 16)
    y0, y1 = (1 if fr.t else 0), (15 if fr.b else 16)
    if fr.t:
        c.hline(x0, x1, 1, col)
    if fr.b:
        c.hline(x0, x1, 14, col)
    if fr.l:
        c.vline(1, y0, y1, col)
    if fr.r:
        c.vline(14, y0, y1, col)


def bastion_iron(rng, fr: Edges = ALL_EDGES):
    base = hx("#a7a9ab")
    c = _plate_base(base, rng, 0.07)
    bevel_edges(c, fr, shade(base, 1.4), shade(base, 0.45))
    _ring(c, fr, shade(base, 0.7))
    notch_marks(c, fr, shade(base, 1.4), shade(base, 0.45), shade(base, 0.7))
    corner_rivets(c, fr, base, ((2, 2, "tl"), (12, 2, "tr"), (2, 12, "bl"), (12, 12, "br")))
    c.hline(2 if fr.l else 0, 14 if fr.r else 16, 8, shade(base, 0.75))
    return [c]


def bastion_steel(rng, fr: Edges = ALL_EDGES):
    base = hx("#6c7078")
    c = _plate_base(base, rng, 0.08)
    bevel_edges(c, fr, shade(base, 1.5), shade(base, 0.4))
    _ring(c, fr, shade(base, 0.62))
    notch_marks(c, fr, shade(base, 1.5), shade(base, 0.4), shade(base, 0.62))
    for i in range(2, 14):                                       # cross-brace seams
        c.px(i, i, shade(base, 0.6))
        c.px(15 - i, i, shade(base, 0.6))
        c.px(i + 1, i, shade(base, 1.25))
    corner_rivets(c, fr, base, ((2, 2, "tl"), (12, 2, "tr"), (2, 12, "bl"), (12, 12, "br")), big=True)
    rivet(c, 7, 7, base, big=True)
    return [c]


def bastion_aluminum(rng, fr: Edges = ALL_EDGES):
    base = hx("#8ab4d0")
    c = _plate_base(base, rng, 0.05)
    # diamond tread: raised lozenges alternating direction
    for by in range(0, 16, 4):
        for bx in range(0, 16, 4):
            d = ((bx // 4) + (by // 4)) % 2
            for k in range(3):
                x, y = (bx + k, by + k) if d == 0 else (bx + 2 - k, by + k)
                c.px(x, y, shade(base, 1.45))
                c.px(x + 1 if d == 0 else x - 1, y, shade(base, 0.75))
    bevel_edges(c, fr, shade(base, 1.5), shade(base, 0.5))
    notch_marks(c, fr, shade(base, 1.5), shade(base, 0.5))
    return [c]


def bastion_carbon(rng, fr: Edges = ALL_EDGES):
    base = hx("#1c1d21")
    c = Canvas(16, fill=base)
    for y in range(16):                                          # twill weave
        for x in range(16):
            k = (x + (y // 2) * 2) % 4
            c.px(x, y, hx("#26282e") if k < 2 else hx("#111215"))
            if k == 0 and y % 2 == 0:
                c.px(x, y, hx("#3a3d46"))
    bevel_edges(c, fr, hx("#4a4d57"), hx("#08080a"))
    notch_marks(c, fr, hx("#4a4d57"), hx("#08080a"))
    has = {"tl": fr.t and fr.l, "tr": fr.t and fr.r, "bl": fr.b and fr.l, "br": fr.b and fr.r}
    for (x, y, k) in ((1, 1, "tl"), (13, 1, "tr"), (1, 13, "bl"), (13, 13, "br")):   # bronze corner brackets
        if has[k]:
            c.rect(x, y, x + 2, y + 2, hx("#a07010"))
            c.px(x, y, hx("#d09a30"))
    return [c]


# ================================================================ CTM sheet builder
def border(c: Canvas, n: int, t: int, edges: str, notch: bool, base, style: str = "bar") -> None:
    """Paint a frame border of thickness t on the listed edges ('L','R','T','B') of an n x n tile."""
    dark, light, mid = shade(base, 0.5), shade(base, 1.35), base

    def hz(y0):
        for y in range(y0, y0 + t):
            col = mid
            if style == "hazard":
                for x in range(n):
                    c.px(x, y, hx("#e0b020") if ((x + y) // 2) % 2 == 0 else hx("#202226"))
                continue
            c.hline(0, n, y, col)

    def vt(x0):
        for x in range(x0, x0 + t):
            if style == "hazard":
                for y in range(n):
                    c.px(x, y, hx("#e0b020") if ((x + y) // 2) % 2 == 0 else hx("#202226"))
                continue
            c.vline(x, 0, n, mid)

    if "T" in edges:
        hz(0)
        if style != "hazard":
            c.hline(0, n, 0, light)
            c.hline(0, n, t - 1, dark)
    if "B" in edges:
        hz(n - t)
        if style != "hazard":
            c.hline(0, n, n - t, light)
            c.hline(0, n, n - 1, dark)
    if "L" in edges:
        vt(0)
        if style != "hazard":
            c.vline(0, 0, n, light)
            c.vline(t - 1, 0, n, dark)
    if "R" in edges:
        vt(n - t)
        if style != "hazard":
            c.vline(n - t, 0, n, light)
            c.vline(n - 1, 0, n, dark)
    if notch:
        for (x, y) in ((0, 0), (n - t, 0), (0, n - t), (n - t, n - t)):
            c.rect(x, y, x + t, y + t, mid)
            c.px(x, y, light)
            c.px(x + t - 1, y + t - 1, dark)


def ctm_set(n: int, rng_seed_fn, interior, base, t: int, style: str = "bar"):
    """Return (base_tile, ctm_sheet). interior(n, rng)->Canvas is re-seeded per tile so it is identical in all
    tiles and the interior continues seamlessly between connected blocks."""
    def tile(edges, notch):
        c = interior(n, rng_seed_fn())
        border(c, n, t, edges, notch, base, style)
        return c

    b = tile("LRTB", False)
    sheet = Canvas(2 * n)
    sheet.paste(tile("", False), 0, 0)         # A: fully connected
    sheet.paste(tile("LR", False), n, 0)       # B: connected up/down -> left/right borders
    sheet.paste(tile("TB", False), 0, n)       # C: connected left/right -> top/bottom borders
    sheet.paste(tile("", True), n, n)          # D: inner corners only
    return b, sheet


# ---------------------------------------------------------------- interior painters
def corrugated(color):
    def f(n, rng):
        c = Canvas(n, fill=color)
        per = 4 if n >= 32 else 2
        for x in range(n):
            k = (x // (per // 2)) % 2
            c.vline(x, 0, n, shade(color, 1.18) if k == 0 else shade(color, 0.82))
        if n >= 32:
            for y in (n // 4, n // 2, 3 * n // 4):
                pass
        noisy(c, rng, 0.05)
        return c
    return f


def concrete_plain(base, spread=0.08, cells=4, pits=0.0, cracks=0):
    def f(n, rng):
        c = Canvas(n, fill=base)
        tone_fill(c, base, rng, spread, cells)
        if pits:
            c.speckle(0, 0, n, n, [shade(base, 0.7), shade(base, 0.8)], rng, pits)
        for _ in range(cracks):
            x, y = int(rng.integers(0, n)), int(rng.integers(0, n))
            for _ in range(int(n * 0.6)):
                c.px(x % n, y % n, shade(base, 0.5))
                x += int(rng.integers(-1, 2))
                y += int(rng.integers(0, 2))
        return c
    return f


def steel_frame_interior(base, light=True):
    def f(n, rng):
        c = Canvas(n, fill=base)
        tone_fill(c, base, rng, 0.05, 3)
        c.speckle(0, 0, n, n, [shade(base, 0.8), shade(base, 1.15)], rng, 0.1)
        return c
    return f


# ================================================================ DECOR
def air_marker(rng):
    n = 32
    c = Canvas(n, fill=hx("#303030"))
    tone_fill(c, hx("#303030"), rng, 0.06, 3)
    c.ring(16, 16, 11, 13, hx("#8c8c8c"))
    c.ring(16, 16, 6, 7.5, hx("#5a5a5a"))
    c.disc(16, 16, 2.4, hx("#d8d8d8"))
    for k in range(4):                                         # chevrons pointing in
        for s in range(4):
            if k == 0:
                x, y = 12 + s, 2 + s
                c.px(x, y, hx("#b0b0b0")); c.px(31 - x, y, hx("#b0b0b0"))
            if k == 1:
                x, y = 12 + s, 29 - s
                c.px(x, y, hx("#b0b0b0")); c.px(31 - x, y, hx("#b0b0b0"))
    for s in range(4):
        c.px(2 + s, 12 + s, hx("#b0b0b0")); c.px(2 + s, 31 - (12 + s), hx("#b0b0b0"))
        c.px(29 - s, 12 + s, hx("#b0b0b0")); c.px(29 - s, 31 - (12 + s), hx("#b0b0b0"))
    c.frame(0, 0, n, n, hx("#1c1c1c"))
    return [c]


SAND = hx("#b0a070")


def _sand(rng, n=16):
    c = Canvas(n, fill=SAND)
    tone_fill(c, SAND, rng, 0.1, 4)
    c.speckle(0, 0, n, n, [hx("#8d7e54"), hx("#c8b888")], rng, 0.18)
    return c


def bugnestblock(rng):
    c = _sand(rng)
    for _ in range(6):                               # small burrow pits
        x, y = int(rng.integers(1, 14)), int(rng.integers(1, 14))
        c.rect(x, y, x + 2, y + 2, hx("#5c5230"))
        c.px(x, y, hx("#303020"))
    return [c]


def bugnestblock_hole(rng):
    c = _sand(rng)
    c.disc(8, 8, 5.2, hx("#7b6c40"))                 # raised rim
    c.disc(8, 8, 4.2, hx("#303020"))
    c.disc(8, 8, 2.8, hx("#14140c"))
    c.px(5, 6, hx("#50482c"))
    c.px(10, 10, hx("#50482c"))
    return [c]


def bugnestslimy(rng):
    c = _sand(rng)
    slime = hx("#b3e571")
    for _ in range(3):
        x = int(rng.integers(1, 13))
        c.rect(x, 0, x + 3, int(rng.integers(5, 11)), slime)
    f = fbm(16, 16, rng, 4, 2)
    for y in range(16):
        for x in range(16):
            if f[y, x] > 0.64:
                c.px(x, y, mix(slime, hx("#8bc24e"), 0.5))
            elif f[y, x] > 0.58:
                c.px(x, y, slime)
    for (x, y) in ((4, 4), (10, 7), (6, 11)):
        c.px(x, y, hx("#f2ffd0"))                    # wet highlights
    return [c]


def bugnesteggs(rng):
    frames = []
    pos = [(3, 4), (9, 3), (6, 9), (11, 10), (2, 11)]
    for f in range(4):
        c = _sand(np.random.default_rng(777))
        c.rect(0, 0, 16, 16, hx("#5c5230"), 160)
        for i, (x, y) in enumerate(pos):
            ph = (f + i) % 4
            egg = [hx("#e8e4a8"), hx("#f4f0b8"), hx("#fff8c8"), hx("#f4f0b8")][ph]
            c.rect(x, y + 1, x + 3, y + 4, shade(egg, 0.6))
            c.rect(x + 1, y, x + 3, y + 3, egg)
            c.px(x + 1, y, WHITE)
            if ph == 2:
                c.px(x + 2, y + 2, hx("#7cd044"))    # wriggle inside the shell
        frames.append(c)
    return frames


def camo_net(kind):
    pal = {"forest": ("#3c5a28", "#516f34", "#2a3d1c", "#5a4a30"),
           "desert": ("#b49664", "#c9ae7c", "#8d7448", "#6a5836"),
           "snow": ("#e6ebef", "#cfd8df", "#a9b6c2", "#8a96a2")}[kind]

    def f(rng):
        n = 32
        c = Canvas(n)
        cord = hx("#2c2a22" if kind != "snow" else "#6b7480")
        for k in range(-n, n, 6):                              # diagonal mesh cords
            for s in range(n):
                c.px(k + s, s, cord)
        for k in range(0, 2 * n, 6):
            for s in range(n):
                c.px(k - s, s, cord)
        fld = fbm(n, n, rng, 5, 3)
        cs = [hx(x) for x in pal]
        for y in range(n):
            for x in range(n):
                v = fld[y, x]
                if v > 0.52:                                    # leaf patches over the net
                    k = int((v - 0.52) / 0.48 * 3.99)
                    c.px(x, y, cs[min(k, 2)] if rng.random() > 0.08 else cs[3])
        # scatter a few frayed strips
        for _ in range(10):
            x, y = int(rng.integers(0, n - 4)), int(rng.integers(0, n))
            c.hline(x, x + 4, y, cs[int(rng.integers(0, 4))])
        return [c]
    return f


def concrete_numbered(i):
    def f(rng):
        n = 32
        spec = {1: (hx("#b3afa6"), 0.05, 4, 0.04, 0), 2: (hx("#89847b"), 0.1, 5, 0.12, 0),
                3: (hx("#776f69"), 0.1, 4, 0.1, 3), 4: (hx("#aca49d"), 0.08, 3, 0.05, 0)}[i]
        c = concrete_plain(*spec)(n, rng)
        if i == 4:                                              # formwork board lines and stains
            for y in (8, 16, 24):
                c.hline(0, n, y, shade(spec[0], 0.72))
                c.hline(0, n, y + 1, shade(spec[0], 1.1))
            for x in (5, 19, 26):
                for y in range(int(rng.integers(0, 8)), n, 1):
                    if rng.random() < 0.9:
                        c.px(x, y, shade(spec[0], 0.85))
        if i == 1:
            c.speckle(0, 0, n, n, [shade(spec[0], 1.12)], rng, 0.05)
        if i == 2:
            for _ in range(8):
                x, y = int(rng.integers(1, n - 3)), int(rng.integers(1, n - 3))
                c.rect(x, y, x + 2, y + 2, shade(spec[0], 0.6))
                c.px(x, y, shade(spec[0], 0.45))
        return [c]
    return f


def concrete_hole(rng):
    n = 32
    base = hx("#8a867e")
    c = concrete_plain(base, 0.08, 4, 0.08)(n, rng)
    c.disc(16, 16, 11.5, shade(base, 0.7))
    c.disc(16, 16, 10, hx("#2c2a28"))
    c.disc(16, 16, 8, hx("#14130f"))
    for _ in range(26):                                         # rubble on the rim
        a = rng.random() * 6.28
        r = 10 + rng.random() * 3
        c.px(int(16 + r * np.cos(a)), int(16 + r * np.sin(a)), shade(base, 0.55 + 0.5 * rng.random()))
    return [c]


def concrete_old2_top(rng):
    n = 32
    base = hx("#b8b3ab")
    c = concrete_plain(base, 0.06, 3, 0.06)(n, rng)
    c.ring(16, 16, 13, 15, shade(base, 0.7))
    c.ring(16, 16, 11, 12.5, shade(base, 1.15))
    c.disc(16, 16, 3, shade(base, 0.62))
    c.disc(16, 16, 2, hx("#3a3835"))
    return [c]


def concrete_old2_ctmv(rng):
    n = 32
    base = hx("#9e968b")
    sheet = Canvas(64)

    def col(rimtop: bool, ruststain: bool, fade: bool):
        c = concrete_plain(base, 0.08, 4, 0.08)(n, np.random.default_rng(4242))
        if rimtop:
            c.rect(0, 0, n, 4, shade(base, 0.62))
            c.hline(0, n, 4, shade(base, 0.8))
            c.hline(0, n, 0, shade(base, 1.2))
        if fade:
            for y in range(5, 14):
                for x in range(n):
                    if rng.random() < (14 - y) / 14 * 0.7:
                        c.px(x, y, shade(base, 0.8))
        if ruststain:
            for y in range(16, n):
                for x in range(n):
                    p = (y - 16) / 16
                    if rng.random() < p * 0.8:
                        c.px(x, y, mix(hx("#7b5230"), hx("#5a3a20"), rng.random()))
        return c
    sheet.paste(col(True, True, False), 0, 0)      # single block: rim top, rust bottom
    sheet.paste(col(True, False, True), 32, 0)     # top of column
    sheet.paste(col(False, False, False), 0, 32)   # middle
    sheet.paste(col(False, True, False), 32, 32)   # bottom
    return [sheet]


def _concrete_frame(light: bool):
    cbase = hx("#8d8a85") if light else hx("#6e6a66")
    sbase = hx("#6f7480") if light else hx("#444850")

    def f(rng):
        n = 32
        seedfn = lambda: np.random.default_rng(99 if light else 101)
        b, sheet = ctm_set(n, seedfn, concrete_plain(cbase, 0.09, 4, 0.07), sbase, 4)
        return b, sheet
    return f


def container_ctm(color: str, n: int):
    base = hx(color)

    def f(rng):
        seedfn = lambda: np.random.default_rng(31)
        return ctm_set(n, seedfn, corrugated(base), shade(base, 0.62), max(2, n // 8))
    return f


def metalpanel2_ctm(kind: str):
    n = 32

    def interior_rusty(nn, rng):
        c = Canvas(nn, fill=hx("#7c6c58"))
        tone_fill(c, hx("#7c6c58"), rng, 0.1, 4)
        rust(c, rng, 0.4)
        return c

    def interior_haz(nn, rng):
        c = Canvas(nn, fill=hx("#4a4640"))
        tone_fill(c, hx("#4a4640"), rng, 0.08, 4)
        c.hline(0, nn, nn // 2, hx("#2a2824"))
        c.hline(0, nn, nn // 2 + 1, hx("#6a665e"))
        return c

    def f(rng):
        seedfn = lambda: np.random.default_rng(55)
        if kind == "rusty":
            return ctm_set(n, seedfn, interior_rusty, hx("#6b5a46"), 4)
        return ctm_set(n, seedfn, interior_haz, hx("#e0b020"), 4, "hazard")
    return f


def metalpanel16_largeborder():
    n = 16

    def interior(nn, rng):
        c = Canvas(nn, fill=hx("#5c5e62"))
        for x in range(0, nn, 3):                           # silver bars
            c.vline(x, 0, nn, hx("#b8bcc4"))
            c.vline(x + 1, 0, nn, hx("#40424a"))
        return c

    def f(rng):
        return ctm_set(n, lambda: np.random.default_rng(8), interior, hx("#8a8e98"), 2)
    return f


def steelframe(kind: str):
    n = 16
    bases = {"blue": hx("#5a7a9a"), "dark": hx("#3c3f46"), "scaffold": hx("#8a8d92")}
    base = bases[kind]

    def interior(nn, rng):
        c = Canvas(nn, fill=shade(base, 0.7) if kind != "scaffold" else (0, 0, 0))
        if kind == "scaffold":
            c.a[:] = 0
            for i in range(nn):                               # open cross bracing, transparent between
                c.px(i, i, shade(base, 0.9))
                c.px(nn - 1 - i, i, shade(base, 0.9))
                c.px(i + 1, i, shade(base, 1.3))
            return c
        tone_fill(c, shade(base, 0.7), rng, 0.08, 3)
        for i in range(2, nn - 2):                            # X brace
            c.px(i, i, shade(base, 1.25))
            c.px(nn - 1 - i, i, shade(base, 1.25))
        return c

    def f(rng):
        return ctm_set(n, lambda: np.random.default_rng(12), interior, base, 2)
    return f


GLYPHS = {   # 3x5 pixel font for stenciled labels
    "0": "111101101101111", "1": "010110010010111", "2": "111001111100111", "3": "111001111001111",
    "4": "101101111001001", "5": "111100111001111", "6": "111100111101111", "7": "111001010010010",
    "8": "111101111101111", "9": "111101111001111", "-": "000000111000000", "G": "111100101101111",
    "S": "111100111001111", "C": "111100100100111", "R": "110101110101101", "M": "101111111101101",
    "A": "010101111101101", "X": "101101010101101", "F": "111100110100100", "L": "100100100100111",
}


def text(c: Canvas, x: int, y: int, msg: str, col) -> None:
    for i, ch in enumerate(msg):
        g = GLYPHS.get(ch)
        if not g:
            continue
        for k, bit in enumerate(g):
            if bit == "1":
                c.px(x + i * 4 + k % 3, y + k // 3, col)


def container_scifi(rusty: bool):
    n = 32
    base = hx("#777262") if not rusty else hx("#826947")
    code = "G7" if not rusty else "R3"
    accent = hx("#40c8d8") if not rusty else hx("#c88a40")

    def interior(nn, rng):
        c = Canvas(nn, fill=base)
        tone_fill(c, base, rng, 0.08, 4)
        # panel seams: a vertical seam at 15 and the horizontal center seam, beveled
        c.vline(15, 0, nn, shade(base, 0.55))
        c.vline(16, 0, nn, shade(base, 1.3))
        c.hline(0, nn, nn // 2 - 1, shade(base, 0.55))
        c.hline(0, nn, nn // 2, shade(base, 1.3))
        # upper-left: louverd vent with a dark plenum and slats
        c.rect(5, 5, 14, 12, OUT)
        for y in range(6, 11, 2):
            c.hline(6, 13, y, shade(base, 0.35))
            c.hline(6, 13, y + 1, shade(base, 1.2))
        c.px(5, 5, shade(base, 1.4))
        # upper-right: label plate with stencil code, barcode and an accent stripe
        c.rect(18, 5, 29, 15, shade(base, 0.45))
        c.rect(19, 6, 28, 14, hx("#cfcab8") if not rusty else hx("#b8a888"))
        text(c, 21, 7, code, hx("#25262a"))
        for i, x in enumerate(range(20, 28)):
            if (i * 5) % 3 != 1:
                c.vline(x, 12, 14, hx("#25262a"))
        c.hline(19, 28, 6, accent)
        # lower-left: recessed hatch panel with a lever handle and corner bolts
        c.rect(5, 19, 14, 28, shade(base, 0.62))
        c.frame(5, 19, 14, 28, shade(base, 0.4))
        c.rect(7, 22, 12, 25, shade(base, 0.82))
        c.hline(8, 11, 23, hx("#c8ccd4"))
        for (x, y) in ((6, 20), (12, 20), (6, 26), (12, 26)):
            rivet(c, x, y, base)
        # lower-right: glowing strip lights and a small hazard badge
        for k in range(3):
            c.rect(19, 19 + k * 3, 29, 20 + k * 3, accent)
        for y in range(25, 29):
            for x in range(23, 29):
                c.px(x, y, hx("#e0b020") if ((x + y) // 2) % 2 == 0 else hx("#202226"))
        c.frame(23, 25, 29, 29, hx("#14161a"))
        if rusty:
            rust(c, rng, 0.2)
        return c

    def f(rng):
        return ctm_set(n, lambda: np.random.default_rng(66), interior, shade(base, 0.55), 4, "bar")
    return f


# ---------------------------------------------------------------- doors, ladders
def _door_full(rng):
    w, h = 32, 64
    base = hx("#888e82")
    c = Canvas(w, h, fill=base)
    tone_fill(c, base, rng, 0.06, 4)
    c.bevel(0, 0, w, h, shade(base, 1.4), shade(base, 0.45))
    c.frame(2, 2, w - 2, h - 2, shade(base, 0.7))
    c.rect(7, 6, 25, 22, OUT)                                  # window
    c.rect(8, 7, 24, 21, hx("#2c4a5e"))
    for k in range(6):
        c.px(9 + k, 8 + k // 2, hx("#6a90a8"))
    c.hline(7, 25, 22, shade(base, 1.4))
    c.rect(4, 38, 28, 40, shade(base, 0.6))                    # midrail
    c.hline(4, 28, 40, shade(base, 1.3))
    c.rect(23, 30, 27, 34, OUT)                                # lever handle
    c.rect(24, 31, 26, 33, hx("#c8ccd4"))
    c.rect(5, 52, 27, 61, shade(base, 0.78))                   # kick plate with rivets
    for x in (7, 14, 21, 25):
        rivet(c, x, 54, base)
    for y in (28, 46):
        c.rect(0, y, 3, y + 6, OUT)                            # hinges
        c.px(1, y + 1, LT)
    return c


def door01(rng):
    d = _door_full(rng)
    c = Canvas(32)
    for y in range(32):
        c.a[y] = d.a[y * 2]
    return [c]


def door01_upper(rng):
    return [_door_full(rng).crop(0, 0, 32, 32)]


def door01_lower(rng):
    return [_door_full(rng).crop(0, 32, 32, 64)]


def _ladder(rails, rungs, shades, wear=0.0, rng=None, carbon=False, chrome=False, rail_w=3, rung_gap=7):
    n = 32
    c = Canvas(n)
    dark, mid, lite = shades
    xs = (5, n - 5 - rail_w)
    for x0 in xs:
        c.rect(x0 - 1, 0, x0 + rail_w + 1, n, shade(dark, 0.5))
        c.rect(x0, 0, x0 + rail_w, n, mid)
        c.vline(x0, 0, n, lite)
        c.vline(x0 + rail_w - 1, 0, n, dark)
    for y in range(3, n, rung_gap):
        c.rect(xs[0] + rail_w, y - 1, xs[1], y + 2, shade(dark, 0.5))
        c.rect(xs[0] + rail_w, y, xs[1], y + 1, mid)
        c.hline(xs[0] + rail_w, xs[1], y, lite)
    if carbon:
        for y in range(0, n, 2):
            for x in xs:
                c.px(x + (y // 2) % rail_w, y, shade(lite, 0.9))
    if chrome:
        for x0 in xs:
            c.vline(x0 + 1, 0, n, hx("#ffffff"))
    if wear and rng is not None:
        for _ in range(int(wear * 120)):
            x, y = int(rng.integers(0, n)), int(rng.integers(0, n))
            if c.a[y, x, 3]:
                c.px(x, y, hx("#7b4a22") if rng.random() < 0.7 else hx("#4d2d12"))
    return c


def ladder01_inv(rng):
    return [_ladder(None, None, (hx("#606368"), hx("#8d9097"), hx("#c3c6cc")))]


def metal_ladder(rng):
    return [_ladder(None, None, (hx("#3e5058"), hx("#627076"), hx("#98acb4")), rail_w=2, rung_gap=6)]


def metal_ladder_carbon(rng):
    return [_ladder(None, None, (hx("#0d0d10"), hx("#1c1c22"), hx("#4a4c55")), carbon=True, rail_w=4, rung_gap=8)]


def metal_ladder_rusty(rng):
    c = _ladder(None, None, (hx("#4a2a12"), hx("#6a3d1a"), hx("#a06530")), wear=0.6, rng=rng, rail_w=3, rung_gap=7)
    for x in range(8, 24):                                     # a rotted-through rung
        c.a[17, x, 3] = 0 if rng.random() < 0.8 else c.a[17, x, 3]
        c.a[16, x, 3] = 0 if rng.random() < 0.5 else c.a[16, x, 3]
    return [c]


def metal_ladder_shiny(rng):
    return [_ladder(None, None, (hx("#8c8e94"), hx("#d9dbe0"), hx("#ffffff")), chrome=True, rail_w=2, rung_gap=5)]


# ---------------------------------------------------------------- lamps and lanterns
def lamp01(rng):
    n = 32
    c = Canvas(n, fill=hx("#1c1c20"))
    c.bevel(0, 0, n, n, hx("#3e4048"), hx("#0a0a0c"))
    for y in range(3, n - 3):
        for x in range(3, n - 3):
            d = max(abs(x - 15.5), abs(y - 15.5)) / 12.5
            c.px(x, y, mix(hx("#fff8c0"), hx("#e8c040"), min(1, d * d)))
    for k in range(3, n - 3, 8):                              # diffuser ribs
        c.vline(k, 3, n - 3, hx("#d2a830"))
        c.hline(3, n - 3, k, hx("#d2a830"))
    c.frame(3, 3, n - 3, n - 3, hx("#8a6a14"))
    return [c]


def lamp02(rng):
    n = 32
    c = Canvas(n, fill=hx("#c4c6cc"))
    c.bevel(0, 0, n, n, WHITE, hx("#8a8c94"))
    c.rect(3, 3, n - 3, n - 3, hx("#8a8c94"))
    for y in range(4, n - 4):
        for x in range(4, n - 4):
            d = max(abs(x - 15.5), abs(y - 15.5)) / 11.5
            c.px(x, y, mix(hx("#ffffff"), hx("#e4eaf0"), min(1, d * d)))
    c.hline(4, n - 4, 15, hx("#d0d8e0"))
    c.hline(4, n - 4, 16, hx("#d0d8e0"))
    return [c]


def _lantern_side(warm: bool):
    n = 32
    glow = hx("#ffe88a") if warm else hx("#f4f8ff")
    c = Canvas(n, fill=hx("#1a1a1c") if warm else hx("#c8ccd2"))
    frame = hx("#2c2c30") if warm else hx("#9aa0aa")
    for y in range(n):
        for x in range(6, 26):
            d = abs(x - 15.5) / 10
            c.px(x, y, mix(glow, shade(glow, 0.72), d * d))
    for x in (4, 5, 26, 27):
        c.vline(x, 0, n, frame)
    for y in (0, 1, 15, 16, 30, 31):
        c.hline(0, n, y, frame)
    if warm:
        for x in (10, 15, 21):                                 # cage bars
            c.vline(x, 2, 30, hx("#3a3a3e"))
    else:
        c.hline(6, 26, 8, hx("#dfe4ea"))
        c.hline(6, 26, 24, hx("#dfe4ea"))
    return c


def lantern01_side(rng):
    return [_lantern_side(True)]


def lantern02_side(rng):
    return [_lantern_side(False)]


def lantern01_top(rng):
    n = 32
    c = Canvas(n, fill=hx("#1b1b1b"))
    tone_fill(c, hx("#1b1b1b"), rng, 0.2, 3)
    c.frame(2, 2, n - 2, n - 2, hx("#3a3a3e"))
    c.disc(16, 16, 6, hx("#2c2c30"))
    c.disc(16, 16, 3, hx("#0c0c0e"))
    return [c]


def lantern02_top(rng):
    n = 32
    c = Canvas(n, fill=hx("#d4d4d4"))
    tone_fill(c, hx("#d4d4d4"), rng, 0.08, 3)
    c.bevel(0, 0, n, n, WHITE, hx("#8e9096"))
    c.ring(16, 16, 7, 9, hx("#9aa0aa"))
    c.disc(16, 16, 5, hx("#f0f0f0"))
    return [c]


# ---------------------------------------------------------------- grates and panels
def _grate(kind: str):
    def f(rng):
        n = 32
        c = Canvas(n, fill=hx("#0e0e0e"))
        if kind == "dark":
            for y in range(0, n, 8):
                c.rect(0, y, n, y + 3, hx("#4a4c52"))
                c.hline(0, n, y, hx("#7a7d85"))
            for x in range(0, n, 8):
                c.rect(x, 0, x + 3, n, hx("#3a3c42"))
                c.vline(x, 0, n, hx("#6c6f78"))
        elif kind == "hazard":
            for y in range(0, n, 8):
                for x in range(n):
                    c.px(x, y, hx("#e0b020") if (x // 4) % 2 == 0 else hx("#222"))
                    c.px(x, y + 1, hx("#e0b020") if (x // 4) % 2 == 0 else hx("#222"))
                    c.px(x, y + 2, hx("#a47c10") if (x // 4) % 2 == 0 else hx("#111"))
            for x in range(0, n, 8):
                c.rect(x, 0, x + 2, n, hx("#3a3a3e"))
        elif kind == "rusty":
            base = hx("#6a4a30")
            for y in range(0, n, 4):                                  # round-hole perforated plate
                for x in range(0, n, 4):
                    pass
            c.rect(0, 0, n, n, base)
            tone_fill(c, base, rng, 0.14, 4)
            for y in range(2, n, 6):
                for x in range(2 + 3 * ((y // 6) % 2), n, 6):
                    c.disc(x + 0.5, y + 0.5, 1.8, hx("#0e0e0e"))
            rust(c, rng, 0.3)
        else:
            base = hx("#5a3e2a")                                      # rusty2: diamond expanded mesh
            c.rect(0, 0, n, n, hx("#0e0e0e"))
            for y in range(n):
                for x in range(n):
                    d = (abs(((x + y) % 8) - 4) + abs(((x - y) % 8) - 4))
                    if d >= 5 or (x + y) % 8 == 0 or (x - y) % 8 == 0:
                        c.px(x, y, mix(base, hx("#8a5a34"), rng.random() * 0.8))
            rust(c, rng, 0.2)
        c.frame(0, 0, n, n, hx("#2a2a2e"))
        return [c]
    return f


def _metalpanel(kind: str):
    def f(rng):
        n = 32
        if kind == "dark":
            base = hx("#2d2b27")
        elif kind == "hazard":
            base = hx("#645e31")
        elif kind == "rusty":
            base = hx("#676154")
        else:
            base = hx("#4f4334")
        c = Canvas(n, fill=base)
        tone_fill(c, base, rng, 0.1, 4)
        if kind == "hazard":
            for y in range(n):
                for x in range(n):
                    if 11 <= y < 21:
                        c.px(x, y, hx("#d8aa18") if ((x + y) // 4) % 2 == 0 else hx("#1c1c1e"))
            c.hline(0, n, 10, hx("#14120c"))
            c.hline(0, n, 21, hx("#14120c"))
        c.bevel(0, 0, n, n, shade(base, 1.4), shade(base, 0.4))
        c.hline(0, n, 15, shade(base, 0.55)) if kind in ("dark", "rusty") else None
        c.vline(15, 0, n, shade(base, 0.55)) if kind in ("dark", "rusty2") else None
        if kind in ("rusty", "rusty2"):
            rust(c, rng, 0.5 if kind == "rusty" else 0.35, cells=5)
            if kind == "rusty2":
                for _ in range(10):                                  # scratches
                    x, y = int(rng.integers(2, 26)), int(rng.integers(2, 30))
                    for s in range(int(rng.integers(3, 7))):
                        c.px(x + s, y + s // 2, hx("#a0a098"))
        for (x, y) in ((2, 2), (28, 2), (2, 28), (28, 28)):
            rivet(c, x, y, base)
        return [c]
    return f


def techgrid_v9(rng):
    """48x48 = a 3x3 block of 16 px tiles that repeat seamlessly; each tile has its own circuit motif."""
    n = 48
    base = hx("#9ea1a2")
    c = Canvas(n, fill=base)
    f = tile_noise(n, rng, 3)
    for y in range(n):
        for x in range(n):
            c.px(x, y, shade(base, 0.9 + 0.2 * float(f[y, x])))
    cyan, cyan_d = hx("#4ac0c8"), hx("#2c7f86")
    dark = shade(base, 0.5)
    chip = hx("#2a2d33")

    def chipat(ox, oy, w, h):
        c.rect(ox, oy, ox + w, oy + h, chip)
        c.hline(ox, ox + w, oy, hx("#4a4e57"))
        for k in range(1, w, 2):
            c.px(ox + k, oy - 1, hx("#c8ccd4"))
            c.px(ox + k, oy + h, hx("#c8ccd4"))
        c.px(ox + 1, oy + 1, hx("#d8dce4"))

    def led(x, y, col):
        c.px(x, y, col)
        c.px(x + 1, y, shade(col, 0.6))

    for ty in range(3):
        for tx in range(3):
            ox, oy = tx * 16, ty * 16
            k = tx * 3 + ty
            c.rect(ox + 3, oy + 3, ox + 5, oy + 5, dark)
            c.px(ox + 3, oy + 3, shade(base, 1.4))
            if k == 0:
                c.hline(ox + 3, ox + 13, oy + 8, cyan)
                c.vline(ox + 8, oy + 3, oy + 13, cyan)
                c.rect(ox + 7, oy + 7, ox + 10, oy + 10, dark)
                c.px(ox + 8, oy + 8, cyan)
            elif k == 1:
                chipat(ox + 5, oy + 6, 6, 4)
                c.hline(ox + 6, ox + 13, oy + 12, cyan)
                c.vline(ox + 12, oy + 8, oy + 13, cyan)
                c.hline(ox + 3, ox + 6, oy + 5, cyan_d)
            elif k == 2:
                for i, col in enumerate(("#4ac0c8", "#e0b020", "#4ac0c8", "#d04030")):
                    led(ox + 3 + (i % 2) * 6, oy + 7 + (i // 2) * 3, hx(col))
                c.hline(ox + 3, ox + 13, oy + 13, cyan_d)
            elif k == 3:
                c.hline(ox + 3, ox + 8, oy + 9, cyan)
                for d in range(3):
                    c.px(ox + 8 + d, oy + 9 - d, cyan)
                c.hline(ox + 11, ox + 14, oy + 6, cyan)
                c.rect(ox + 6, oy + 11, ox + 12, oy + 13, dark)
            elif k == 4:
                c.rect(ox + 5, oy + 5, ox + 11, oy + 11, shade(base, 0.7))
                c.frame(ox + 5, oy + 5, ox + 11, oy + 11, cyan)
                c.rect(ox + 7, oy + 7, ox + 9, oy + 9, hx("#c8f6f8"))
                c.px(ox + 8, oy + 8, WHITE)
            elif k == 5:
                for y in range(7, 13, 2):
                    c.hline(ox + 6, ox + 13, oy + y, hx("#202328"))
                    c.hline(ox + 6, ox + 13, oy + y + 1, shade(base, 1.25))
            elif k == 6:
                c.hline(ox + 3, ox + 12, oy + 5, cyan)
                c.vline(ox + 12, oy + 5, oy + 12, cyan)
                c.rect(ox + 11, oy + 11, ox + 14, oy + 14, dark)
                c.px(ox + 12, oy + 12, cyan)
            elif k == 7:
                chipat(ox + 4, oy + 7, 4, 3)
                chipat(ox + 10, oy + 7, 4, 3)
                c.hline(ox + 8, ox + 10, oy + 8, cyan_d)
                c.hline(ox + 3, ox + 13, oy + 13, cyan)
            else:
                for i in range(5):
                    h = 2 + (i * 2) % 5
                    c.rect(ox + 5 + i * 2, oy + 12 - h, ox + 6 + i * 2, oy + 12, cyan if h > 3 else cyan_d)
                for y in range(3):
                    for x in range(3):
                        c.px(ox + 11 + x, oy + 3 + y, hx("#e0b020") if (x + y) % 2 == 0 else hx("#2a2a30"))
    for g in range(0, n, 16):
        c.hline(0, n, g, shade(base, 0.62))
        c.vline(g, 0, n, shade(base, 0.62))
        c.hline(0, n, (g + 1) % n, shade(base, 1.3))
        c.vline((g + 1) % n, 0, n, shade(base, 1.3))
    return [c]


def sandbags_side(rng):
    n = 32
    base = hx("#b49664")
    c = Canvas(n, fill=hx("#4a3c26"))
    for row in range(4):
        y0 = row * 8
        off = 0 if row % 2 == 0 else 8
        for bx in range(-8, n, 16):
            x0 = bx + off
            for y in range(y0, y0 + 8):
                for x in range(x0, x0 + 16):
                    if 0 <= x < n:
                        ex = min(x - x0, x0 + 15 - x) / 7.5
                        ey = min(y - y0, y0 + 7 - y) / 3.5
                        e = min(ex, ey)
                        col = shade(base, 0.6 + 0.5 * min(1, e) + 0.12 * (1 - (y - y0) / 8))
                        c.px(x, y, col)
            # stitched tie ends
            c.px(x0 + 1, y0 + 4, shade(base, 0.55))
            c.px(x0 + 14, y0 + 4, shade(base, 0.55))
    noisy(c, rng, 0.07)
    return [c]


def sandbags_top(rng):
    n = 32
    base = hx("#b49664")
    c = Canvas(n, fill=hx("#4a3c26"))
    for by in range(0, n, 16):
        for bx in range(0, n, 16):
            for y in range(by, by + 16):
                for x in range(bx, bx + 16):
                    e = min(min(x - bx, bx + 15 - x), min(y - by, by + 15 - y)) / 7.5
                    c.px(x, y, shade(base, 0.62 + 0.45 * min(1, e)))
            for k in range(3):                                         # fold wrinkles
                yy = by + 4 + k * 4
                c.hline(bx + 3, bx + 13, yy, shade(base, 0.72))
                c.hline(bx + 4, bx + 12, yy + 1, shade(base, 1.2))
    noisy(c, rng, 0.07)
    return [c]


# ---------------------------------------------------------------- screens and control panels
GREEN = hx("#30f060")


def _housing(n, base, rng):
    c = Canvas(n, fill=base)
    tone_fill(c, base, rng, 0.08, 3)
    c.bevel(0, 0, n, n, shade(base, 1.6), shade(base, 0.4))
    return c


def screen_front(rng):
    n = 32
    c = _housing(n, hx("#2c2a2b"), rng)
    c.rect(3, 4, 29, 27, OUT)
    c.rect(4, 5, 28, 26, hx("#0a1210"))
    c.hline(4, 28, 5, hx("#16241f"))
    c.px(5, 6, hx("#20382e"))
    c.px(29, 29, GREEN)
    c.px(27, 29, hx("#d0a020"))
    return [c]


def screen_side(rng):
    n = 32
    c = _housing(n, hx("#2f2d2e"), rng)
    for y in range(6, 26, 4):
        c.hline(4, 28, y, hx("#141313"))
        c.hline(4, 28, y + 1, hx("#464344"))
    return [c]


def screen_top(rng):
    n = 32
    c = _housing(n, hx("#323031"), rng)
    c.rect(6, 6, 26, 26, hx("#1a1919"))
    for x in range(8, 25, 3):
        c.vline(x, 8, 24, hx("#0a0a0a"))
    return [c]


def screen_overlay(rng):
    frames = []
    for f in range(8):
        c = Canvas(32)
        # scanning line + scrolling text rows + a waveform, inside the glass area
        for y in range(6, 26, 4):
            run = 6 + ((y * 7 + f * 3) % 14)
            c.hline(6, 6 + run, y, GREEN)
            c.hline(6, 6 + run, y, shade(GREEN, 0.8))
        sy = 6 + (f * 3) % 20
        c.rect(5, sy, 28, sy + 1, GREEN, 70)
        c.hline(5, 28, sy, mix(GREEN, WHITE, 0.4))
        for x in range(6, 27):                                          # waveform on the bottom row
            yy = 23 + int(round(1.6 * np.sin((x + f * 4) * 0.5)))
            c.px(x, yy, mix(GREEN, WHITE, 0.3))
        if f % 2 == 0:
            c.rect(24, 8, 27, 11, GREEN)
        frames.append(c)
    return frames


def controlpanel_front(rng):
    n = 32
    c = _housing(n, hx("#4a3d3a"), rng)
    c.rect(3, 3, 29, 12, OUT)
    c.rect(4, 4, 28, 11, hx("#101a14"))                                 # status window (overlay animates here)
    for i in range(4):                                                  # knobs
        cx = 7 + i * 6
        c.disc(cx + 0.5, 18.5, 2.8, OUT)
        c.disc(cx + 0.5, 18.5, 2.0, hx("#a8a29a"))
        c.px(cx, 17, WHITE)
    for i in range(6):                                                  # switches
        x = 5 + i * 4
        c.rect(x, 24, x + 2, 29, OUT)
        c.rect(x, 24 + (i % 2) * 2, x + 2, 26 + (i % 2) * 2, hx("#d03030") if i % 3 == 0 else hx("#c8c8c0"))
    return [c]


def controlpanel_side(rng):
    n = 32
    c = _housing(n, hx("#4f413d"), rng)
    for y in range(5, 28, 5):
        c.hline(4, 28, y, shade(hx("#4f413d"), 0.55))
        c.hline(4, 28, y + 1, shade(hx("#4f413d"), 1.3))
    for (x, y) in ((2, 2), (28, 2), (2, 28), (28, 28)):
        rivet(c, x, y, hx("#4f413d"))
    return [c]


def controlpanel_top(rng):
    n = 32
    c = _housing(n, hx("#534540"), rng)
    c.rect(8, 8, 24, 24, shade(hx("#534540"), 0.6))
    for i in range(4):
        c.hline(10, 22, 11 + i * 4, hx("#1a1412"))
    return [c]


def controlpanel_overlay(rng):
    frames = []
    for f in range(8):
        c = Canvas(32)
        for i in range(8):                                              # bar graph lights chase
            h = 1 + int(3.2 * (1 + np.sin((i + f) * 0.9)) / 2 * 2)
            for k in range(min(h, 6)):
                col = GREEN if k < 3 else (hx("#e0c030") if k < 5 else hx("#e04030"))
                c.px(6 + i * 3, 10 - k, col)
                c.px(7 + i * 3, 10 - k, shade(col, 0.8))
        if f % 4 < 2:
            c.rect(25, 5, 27, 7, GREEN)
        else:
            c.rect(25, 5, 27, 7, hx("#d03030"))
        frames.append(c)
    return frames
