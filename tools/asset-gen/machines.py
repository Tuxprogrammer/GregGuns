"""GT-style machine overlays for GregGuns (block.machine.* manifest entries).

GT machines are a tier-colored hull casing with a transparent overlay on top, so every
overlay here is RGBA with a transparent surround: the hull shows through. Overlays only
carry the detail (hoods, slots, belts, lenses, gauges) and a dark outline for contrast.

Animated overlays are returned as a list of frames; blocks.py stacks them into a strip.
"""
from __future__ import annotations

import numpy as np

from blocktex import Canvas, hx, mix, shade

# ---------------------------------------------------------------- shared palette
OUT = hx("#14161a")        # outline / deepest shadow
DK = hx("#26282e")         # dark steel
MD = hx("#4a4d55")         # mid steel
LT = hx("#7c808a")         # light steel
HI = hx("#b4b8c2")         # highlight
BLK = hx("#0b0c0f")        # slot black
AMB = hx("#e8a040")        # GregGuns amber (palette accent)
AMB_D = hx("#9a5c14")
AMB_L = hx("#ffd27a")
WHITE = hx("#fff4d8")
BRONZE = hx("#b5651d")
BRONZE_D = hx("#6e3a0e")
BRONZE_L = hx("#e89a48")
ORANGE = hx("#ff8000")


def art(c: Canvas, ox: int, oy: int, rows: list, legend: dict) -> None:
    """Stamp ASCII art. '.' and ' ' are transparent."""
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in ". ":
                continue
            col = legend[ch]
            c.px(ox + x, oy + y, col)


def plate(c: Canvas) -> None:
    """Bare plate used by the bottom overlays: corner bolts and a faint inset line."""
    for (x, y) in ((1, 1), (13, 1), (1, 13), (13, 13)):
        c.rect(x, y, x + 2, y + 2, OUT)
        c.px(x, y, LT)
    c.hline(4, 12, 1, OUT)
    c.hline(4, 12, 14, OUT)
    c.vline(1, 4, 12, OUT)
    c.vline(14, 4, 12, OUT)


# ================================================================ AMMO PRESS
RAM_OFF = [0, 1, 3, 4, 5, 6, 4, 2]   # ram head travel per frame; the bottom-out is frame 5


def press_front(f: int | None, steam: bool = False) -> Canvas:
    """Front of the Ammo Press. f=None draws the inactive hood; f=0..7 is one ram-stroke frame."""
    c = Canvas(16)
    body, body_d, body_l = (BRONZE, BRONZE_D, BRONZE_L) if steam else (MD, DK, LT)
    # hood and die bed
    c.rect(1, 1, 15, 11, OUT)
    c.rect(2, 2, 14, 10, body)
    c.hline(2, 14, 2, body_l)
    c.vline(2, 2, 10, body_l)
    c.hline(2, 14, 9, body_d)
    c.vline(13, 3, 10, body_d)
    # ram window
    wx0, wx1 = (4, 10) if steam else (4, 12)
    c.rect(wx0 - 1, 2, wx1 + 1, 10, OUT)
    c.rect(wx0, 3, wx1, 9, BLK)
    cx = (wx0 + wx1) // 2
    # die bed + output chute below the window
    c.rect(1, 11, 15, 15, OUT)
    c.rect(2, 11, 14, 13, DK if not steam else BRONZE_D)
    c.hline(2, 14, 11, MD if not steam else BRONZE)
    c.rect(4, 12, 12, 14, BLK)              # output slot
    c.hline(4, 12, 14, AMB_D)               # amber lip
    # rivets / gauge
    if steam:
        for (x, y) in ((2, 3), (13, 3), (2, 8), (13, 8)):
            c.px(x, y, BRONZE_L)
        # pressure gauge at the right of the window
        c.disc(11.5, 6.0, 2.5, OUT)
        c.disc(11.5, 6.0, 1.9, hx("#e8e0c8"))
        ang = (0.4 + 0.9 * (RAM_OFF[f] / 6.0) if f is not None else 0.4)
        nx, ny = 11 + round(1.6 * np.cos(np.pi + ang * 2.2)), 6 + round(1.6 * np.sin(np.pi + ang * 2.2))
        c.px(nx, ny, hx("#a01010"))
        c.px(11, 6, OUT)
    else:
        c.px(13, 2, AMB if f is None else (AMB_L if f % 2 == 0 else AMB))   # status lamp
        c.px(2, 8, HI)
    # ram
    off = 0 if f is None else RAM_OFF[f]
    top = 3 + off
    rod = (cx - 1, cx + 1)
    c.rect(rod[0], 3, rod[1], top + 1, LT if not steam else BRONZE_L)
    c.vline(rod[0], 3, top + 1, HI if not steam else AMB_L)
    c.rect(wx0, top, wx1, top + 2, LT if not steam else BRONZE_L)
    c.hline(wx0, wx1, top, HI)
    c.hline(wx0, wx1, top + 1, body_d)
    # workpiece: a brass blank on the die, squashed on the strike
    by = 8
    if f is not None and f == 5:
        c.rect(cx - 2, by, cx + 2, by + 1, AMB_L)
    else:
        c.rect(cx - 1, by, cx + 1, by + 1, AMB)
    # sparks on the strike and just after
    if f == 5:
        for (dx, dy) in ((-3, -1), (3, -1), (-2, 0), (2, 0), (0, -2)):
            c.px(cx + dx, 8 + dy, WHITE)
        c.px(cx - 3, 8, AMB_L)
        c.px(cx + 3, 8, AMB_L)
    elif f == 6:
        for (dx, dy) in ((-3, -2), (3, -2), (-1, -3)):
            c.px(cx + dx, 8 + dy, AMB)
    # finished round sliding along the output chute
    if f in (6, 7, 0):
        rx = {6: 5, 7: 8, 0: 10}[f]
        c.rect(rx, 12, rx + 3, 14, AMB)
        c.px(rx + 3, 12, AMB_D)
        c.px(rx, 12, AMB_L)
    return c


def press_front_glow(f: int) -> Canvas:
    """Glow layer: the ram head heats orange around the strike (frames 3..7), sparks at the strike."""
    c = Canvas(16)
    cx = 7
    heat = {3: 70, 4: 150, 5: 255, 6: 190, 7: 90}.get(f, 0)
    if heat:
        top = 3 + RAM_OFF[f]
        for x in range(4, 12):
            c.px(x, top, hx("#ff9020"), heat)
            c.px(x, top + 1, hx("#ff5008"), int(heat * 0.8))
        if f in (5, 6):
            for dx in (-3, -2, 2, 3):
                c.px(cx + dx + 1, 8, hx("#ffe080"), heat)
            c.hline(4, 12, 9, hx("#ff6a10"))
            c.a[9, 4:12, 3] = int(heat * 0.7)
        if f == 5:
            for (dx, dy) in ((-4, -1), (4, -1), (0, -3), (-3, -2), (3, -2)):
                c.px(cx + dx + 1, 8 + dy, hx("#fff0a0"), 220)
    return c


def belt(c: Canvas, f: int | None, y0: int = 5, h: int = 7, steam: bool = False) -> None:
    """Horizontal ammo belt across a full 16 px side. f=None: static guide rail; f=0..3: boxes scroll."""
    rail = BRONZE_D if steam else DK
    c.rect(0, y0, 16, y0 + h, OUT)
    c.rect(0, y0 + 1, 16, y0 + h - 1, rail)
    c.hline(0, 16, y0, LT if not steam else BRONZE_L)       # top flange
    c.hline(0, 16, y0 + h - 1, MD)
    c.hline(0, 16, y0 + 1, MD)
    belt_y0, belt_y1 = y0 + 2, y0 + h - 2
    c.rect(0, belt_y0, 16, belt_y1, hx("#1c1e24"))
    if f is None:
        for x in range(1, 16, 4):
            c.vline(x, belt_y0, belt_y1, hx("#2c2f37"))
        # studs on the flange
        for x in (2, 13):
            c.px(x, y0, AMB)
        return
    shift = 2 * f
    for k in range(-1, 3):
        bx = k * 8 + shift
        # boxed rounds: amber carton with a darker lid line and highlight
        c.rect(bx, belt_y0, bx + 6, belt_y1, AMB_D)
        c.rect(bx + 1, belt_y0, bx + 5, belt_y1 - 1, AMB)
        c.hline(bx + 1, bx + 5, belt_y0, AMB_L)
        c.px(bx + 2, belt_y0 + 1, AMB_D)
        c.px(bx + 3, belt_y0 + 1, AMB_D)
        # belt chevron in the gap
        gx = bx + 7
        c.px(gx, belt_y0 + 1, hx("#555a66"))
    # clip stray pixels: flanges are repainted over the boxes
    c.hline(0, 16, y0, LT if not steam else BRONZE_L)
    c.hline(0, 16, y0 + h - 1, MD)
    for x in (2, 13):
        c.px(x, y0, AMB)


def press_side(f: int | None, steam: bool = False) -> Canvas:
    c = Canvas(16)
    if steam:
        # copper steam pipe along the top, rivet flanges, belt below
        c.rect(0, 1, 16, 5, OUT)
        for y, col in ((2, BRONZE_L), (3, BRONZE), (4, BRONZE_D)):
            c.hline(0, 16, y, col)
        for x in (3, 12):
            c.rect(x, 0, x + 2, 6, OUT)
            c.rect(x, 1, x + 2, 5, BRONZE_D)
            c.px(x, 2, BRONZE_L)
        if f is not None:      # steam streaks scroll along the pipe
            for k in range(0, 16, 8):
                sx = (k + 2 * f) % 16
                c.px(sx, 2, hx("#f4f4f4"))
                c.px((sx + 1) % 16, 3, hx("#d6d6d6"))
        belt(c, f, y0=7, h=7, steam=True)
    else:
        belt(c, f)
    return c


def hopper_top(f: int | None, steam: bool = False) -> Canvas:
    c = Canvas(16)
    if steam:
        # boiler cap: bolted round lid with a vent; steam puffs rise on the active frames
        c.disc(8, 8, 6.2, OUT)
        c.disc(8, 8, 5.4, BRONZE)
        c.ring(8, 8, 4.0, 5.4, BRONZE_L)
        c.disc(8, 8, 3.6, BRONZE_D)
        c.disc(8, 8, 2.2, OUT)
        for a in range(8):
            x = 8 + round(4.6 * np.cos(a * np.pi / 4))
            y = 8 + round(4.6 * np.sin(a * np.pi / 4))
            c.px(x, y, AMB_L)
        if f is not None:
            puffs = [(7, 6, 1), (8, 4, 2), (6, 2, 2), (8, 1, 3), (7, 0, 3), (9, 3, 1)]
            for i, (px_, py_, sz) in enumerate(puffs):
                ph = (i * 1 + f) % 6
                y = 8 - ph * 2
                if y < 0:
                    continue
                al = 220 - ph * 30
                for dx in range(-(sz // 2), sz // 2 + 1):
                    for dy in range(0, 2):
                        c.blend(px_ + dx + (ph % 2), y + dy, hx("#f0f0f0"), max(40, al))
        return c
    # hopper mouth: beveled rim, funnel, black opening
    c.rect(1, 1, 15, 15, OUT)
    c.rect(2, 2, 14, 14, MD)
    c.bevel(2, 2, 14, 14, HI, DK)
    c.rect(3, 3, 13, 13, OUT)
    c.rect(4, 4, 12, 12, DK)
    c.rect(5, 5, 11, 11, hx("#1a1c21"))
    c.rect(6, 6, 10, 10, BLK)
    c.hline(4, 12, 4, hx("#3a3d46"))
    c.vline(4, 4, 12, hx("#3a3d46"))
    if f is not None:
        for i in range(2):
            ph = (f + 3 * i) % 6
            rx = 6 + 3 * i
            ry = 3 + ph * 2
            if ry > 10:
                continue
            for dy in range(3):
                yy = ry + dy
                if yy > 10:
                    continue
                col = AMB_L if dy == 0 else (AMB if dy == 1 else AMB_D)
                c.rect(rx, yy, rx + 2, yy + 1, col)
            c.px(rx, ry, WHITE)
        # a dust spark where the stream enters the throat
        if f % 3 == 0:
            c.px(5, 5, AMB)
            c.px(10, 5, AMB)
    return c


# ================================================================ REACTION CHAMBER
HEAT_RED = hx("#8a1c08")
HEAT_OR = hx("#ff6020")
HEAT_YL = hx("#ffc050")
HEAT_WH = hx("#fff2c0")
GLASS = hx("#0e1620")


def reaction_front(f: int | None) -> Canvas:
    c = Canvas(16)
    c.disc(8, 8, 7.0, OUT)
    c.disc(8, 8, 6.3, LT)
    c.ring(8, 8, 5.3, 6.3, MD)
    # bevel the bezel top-left
    for (x, y) in ((4, 3), (5, 2), (6, 2), (3, 4), (3, 5), (2, 6)):
        c.px(x, y, HI)
    c.disc(8, 8, 4.8, OUT)
    c.disc(8, 8, 4.2, GLASS)
    for a in range(4):               # four bezel bolts
        x = 8 + round(5.6 * np.cos(a * np.pi / 2 + np.pi / 4))
        y = 8 + round(5.6 * np.sin(a * np.pi / 2 + np.pi / 4))
        c.px(x, y, OUT)
    if f is None:
        c.px(6, 6, hx("#4a6a88"))     # lens glint
        c.px(7, 5, hx("#2a4258"))
        return c
    # active: heat-ray flame in the viewing port
    hts = [4, 5, 6, 5, 6, 4]
    wid = [1.7, 2.1, 2.5, 2.2, 2.6, 1.8]
    ht, w = hts[f % 6], wid[f % 6]
    sway = [0, 0, 1, 0, -1, 0][f % 6]
    yy, xx = np.mgrid[0:16, 0:16]
    for y in range(4, 13):
        for x in range(4, 13):
            dx, dy = x + 0.5 - 8, y + 0.5 - 8
            if dx * dx + dy * dy > 4.2 * 4.2:
                continue
            t = (11.5 - (y + 0.5)) / ht          # 0 at the base, 1 at the tip
            amb = max(0.0, 1 - (dx * dx + dy * dy) / 18.0)
            col = mix(GLASS, HEAT_RED, 0.35 + 0.45 * amb)
            if 0 <= t <= 1:
                half = w * (1 - t) ** 0.8
                d = abs(dx - sway * t)
                if d < half:
                    k = d / max(half, 0.01)
                    if k < 0.35 and t < 0.6:
                        col = HEAT_WH
                    elif k < 0.6:
                        col = HEAT_YL
                    elif k < 0.85:
                        col = HEAT_OR
                    else:
                        col = HEAT_RED
            c.px(x, y, col)
    c.px(6, 6, hx("#7a9ab8"))             # glass glint stays
    return c


def pipe_h(c: Canvas, y: int, x0: int, x1: int, f: int | None) -> None:
    c.rect(x0, y, x1, y + 4, OUT)
    c.hline(x0, x1, y + 1, HI)
    c.hline(x0, x1, y + 2, LT)
    c.hline(x0, x1, y + 3 - 1 + 0, MD)
    c.hline(x0, x1, y + 3, DK)
    if f is not None:                    # bright dashes of flow
        for k in range(0, 16, 8):
            sx = (k + 2 * f) % 16
            if x0 <= sx < x1:
                c.px(sx, y + 2, HEAT_YL)
            if x0 <= (sx + 1) % 16 < x1:
                c.px((sx + 1) % 16, y + 2, HEAT_OR)


def reaction_side(f: int | None) -> Canvas:
    c = Canvas(16)
    pipe_h(c, 2, 0, 16, f)
    pipe_h(c, 11, 0, 16, f)
    # riser joining the pipes and flange rings
    c.rect(11, 5, 14, 11, OUT)
    c.rect(12, 5, 13, 11, LT)
    c.vline(11, 5, 11, MD)
    for y in (2, 11):
        c.rect(10, y - 1, 12, y + 5, OUT)
        c.vline(10, y, y + 4, HI)
    # gauge
    c.disc(5.5, 8.0, 3.2, OUT)
    c.disc(5.5, 8.0, 2.5, hx("#dad4c0"))
    pulse = [0.2, 0.55, 0.9, 0.55][f % 4] if f is not None else 0.25
    ang = np.pi * (1.0 + pulse)
    for r in (1, 2):
        c.px(5 + round(r * np.cos(ang)), 8 + round(r * np.sin(ang)), hx("#b01010"))
    c.px(5, 8, OUT)
    if f is not None:                    # rim lamp pulses with the gauge
        glow = [90, 160, 255, 160][f % 4]
        for (x, y) in ((3, 5), (7, 5), (3, 10), (7, 10)):
            pass
        c.blend(5, 5, HEAT_OR, glow)
        c.blend(5, 11, HEAT_OR, glow)
    return c


def reaction_top(f: int | None) -> Canvas:
    c = Canvas(16)
    c.rect(1, 1, 15, 15, OUT)
    c.rect(2, 2, 14, 14, DK)
    c.bevel(2, 2, 14, 14, LT, OUT)
    for i, y in enumerate(range(3, 13, 2)):      # slats
        c.hline(3, 13, y, MD)
        c.hline(3, 13, y + 1, BLK)
        if f is not None:
            hot = [60, 110, 160, 110][(f + i) % 4]
            for x in range(3, 13):
                c.blend(x, y + 1, HEAT_OR, hot)
    if f is not None:                             # shimmer: pale wavy cells rising
        for k in range(3):
            y = 12 - ((f * 3 + k * 4) % 12)
            x = 4 + k * 3 + (1 if (y // 2) % 2 else 0)
            c.blend(x, y, hx("#ffd8a8"), 110)
            c.blend(x + 1, y - 1, hx("#ffd8a8"), 70)
    return c


# ================================================================ BENCHES
WOOD = hx("#705030")
WOOD_D = hx("#463018")
WOOD_L = hx("#94703f")


def repair_front(f: int | None) -> Canvas:
    c = Canvas(16)
    c.rect(1, 1, 15, 15, OUT)
    c.rect(2, 2, 14, 14, hx("#33353b"))              # pegboard
    for y in range(3, 14, 3):
        for x in range(3, 14, 3):
            c.px(x, y, hx("#1f2126"))
    # hung wrench
    art(c, 3, 3, [
        ".OO.....",
        "OHHO....",
        "OHO.....",
        ".OHO....",
        "..OHO...",
        "...OHO..",
        "....OHO.",
        ".....OLO",
        ".....OOO",
    ], {"O": OUT, "H": HI, "L": LT})
    # hung hammer
    art(c, 9, 3, [
        "OOOO",
        "OLLO",
        "OOOO",
        ".OWO",
        ".OWO",
    ], {"O": OUT, "L": LT, "W": WOOD_L})
    # vice on the bench ledge
    c.rect(2, 11, 14, 14, OUT)
    c.rect(3, 12, 13, 14, MD)
    c.hline(3, 13, 12, LT)
    c.rect(6, 9, 10, 12, OUT)
    c.rect(7, 10, 9, 12, DK)
    c.px(7, 10, LT)
    if f is not None:                                # welding spark flicker at the jaw
        sp = [(8, 9, 255), (8, 8, 200), (7, 8, 160), (9, 8, 160)]
        pat = [[0], [0, 1, 2, 3], [0, 1], [0, 2, 3]][f % 4]
        for i in pat:
            x, y, a = sp[i]
            c.px(x, y, hx("#c8f0ff") if i == 0 else WHITE)
        if f % 4 == 1:
            for (x, y) in ((6, 7), (10, 7), (5, 9), (11, 9)):
                c.px(x, y, hx("#7ac8ff"))
    return c


def repair_side(f: int | None = None) -> Canvas:
    """Clamp arm. Active (f=0..3): the arm lowers onto the work and lifts again, status lamp changes color."""
    c = Canvas(16)
    off = 0 if f is None else [0, 1, 2, 1][f]
    c.rect(2, 2, 7, 14, OUT)                         # fixed column
    c.rect(3, 3, 6, 13, MD)
    c.vline(3, 3, 13, LT)
    c.rect(2, 2 + off, 12, 6 + off, OUT)             # moving arm
    c.rect(3, 3 + off, 11, 5 + off, MD)
    c.hline(3, 11, 3 + off, LT)
    c.rect(9, 5 + off, 12, 11, OUT)                  # screw
    c.rect(10, 6 + off, 11, 11, LT)
    c.rect(8, 11, 14, 14, OUT)                       # handle (fixed)
    c.rect(9, 12, 13, 13, WOOD_L)
    c.px(9, 12, WOOD)
    if f is None:
        c.px(13, 3, AMB_D)
    else:
        c.px(13, 3, [AMB, AMB_L, hx("#44ff66"), AMB_L][f])
        c.px(13, 2, [AMB_D, AMB, hx("#1f9a3a"), AMB][f])
    return c


def repair_top(f: int | None = None) -> Canvas:
    """Bench top. Active (f=0..3): a pale scanning light bar sweeps across the planks, LED blinks."""
    c = Canvas(16)
    c.rect(1, 1, 15, 15, OUT)
    c.rect(2, 2, 14, 14, WOOD)
    for y in (5, 8, 11):
        c.hline(2, 14, y, WOOD_D)
    for y in (3, 6, 9, 12):
        c.hline(2, 14, y, WOOD_L)
    for (x, y) in ((3, 4), (10, 7), (5, 10), (12, 12)):
        c.px(x, y, WOOD_D)
    c.rect(9, 3, 13, 5, OUT)                         # laid-out wrench head
    c.hline(10, 12, 3, HI)
    c.px(10, 4, LT)
    c.px(12, 4, LT)
    if f is None:
        c.px(3, 12, hx("#1f6a33"))
        return c
    c.px(3, 12, hx("#44ff66") if f % 2 == 0 else hx("#1f6a33"))
    sx = 3 + f * 3
    for y in range(2, 14):                           # scan bar with a soft trailing edge
        c.blend(sx, y, hx("#cfeaff"), 170)
        c.blend(sx - 1, y, hx("#9ac8ff"), 70)
        c.blend(sx + 1, y, hx("#9ac8ff"), 40)
    return c


def camo_front(f: int | None = None) -> Canvas:
    c = Canvas(16)
    c.rect(1, 1, 15, 15, OUT)
    c.rect(2, 2, 14, 14, hx("#3a3b40"))
    # paint palette with four dabs
    c.disc(7, 8, 5, OUT)
    c.disc(7, 8, 4.2, hx("#c9b27c"))
    for (x, y, col) in ((5, 6, "#c03030"), (8, 5, "#3c5a28"), (9, 8, "#b49664"), (5, 10, "#e6ebef")):
        c.rect(x, y, x + 2, y + 2, hx(col))
    c.disc(7, 9, 1.2, OUT)
    # spray can
    c.rect(11, 5, 14, 14, OUT)
    c.rect(12, 6, 14, 14, hx("#c03030"))
    c.hline(12, 14, 6, hx("#f06060"))
    c.rect(12, 3, 14, 5, OUT)
    c.px(12, 4, HI)
    if f is not None:                                # status LEDs chase along the base; nozzle puffs paint
        cols = ["#c03030", "#3c8a28", "#b49664", "#e6ebef"]
        for i, col in enumerate(cols):
            c.px(3 + i * 2, 14, hx(col) if i == f else shade(hx(col), 0.3))
        if f in (1, 2):
            c.blend(13, 2, hx("#f06060"), 200 if f == 1 else 110)
            c.blend(12, 1, hx("#f06060"), 120 if f == 1 else 60)
            c.blend(14, 1, hx("#f06060"), 90)
    return c


def camo_side() -> Canvas:
    c = Canvas(16)
    c.rect(1, 2, 15, 14, OUT)
    c.rect(2, 3, 14, 13, hx("#33343a"))
    c.hline(2, 14, 8, OUT)
    c.hline(2, 14, 9, MD)
    cols = ["#c03030", "#3c5a28", "#b49664", "#e6ebef"]
    for i, col in enumerate(cols):
        x = 3 + i * 3
        c.rect(x, 4, x + 2, 8, hx(col))
        c.px(x, 4, shade(hx(col), 1.4))
        c.rect(x, 10, x + 2, 13, shade(hx(col), 0.8))
        c.px(x, 10, shade(hx(col), 1.3))
    return c


def camo_top(f: int | None = None) -> Canvas:
    c = Canvas(16)
    c.rect(1, 1, 15, 15, OUT)
    c.rect(2, 2, 14, 14, hx("#3f4047"))
    cols = ["#c03030", "#3c5a28", "#b49664", "#e6ebef"]
    for i, col in enumerate(cols):               # stencil tray, 2x2 wells with a dab each
        x, y = 3 + (i % 2) * 6, 3 + (i // 2) * 6
        c.rect(x, y, x + 5, y + 5, OUT)
        c.rect(x + 1, y + 1, x + 4, y + 4, hx(col))
        c.px(x + 1, y + 1, shade(hx(col), 1.4))
    if f is not None:                                # scanning light bar over the stencil tray
        sy = 3 + f * 3
        for x in range(2, 14):
            c.blend(x, sy, hx("#ffffff"), 150)
            c.blend(x, sy + 1, hx("#ffffff"), 55)
    return c


# ================================================================ MULTIBLOCK CONTROLLERS
def lights(c: Canvas, y: int, n: int, lit: set, col: tuple, x0: int = 3, step: int = 2, glow: bool = False) -> None:
    for i in range(n):
        x = x0 + i * step
        if glow:
            if i in lit:
                c.px(x, y, mix(col, WHITE, 0.3))
                c.px(x, y, mix(col, WHITE, 0.3))
                for (dx, dy) in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                    c.blend(x + dx, y + dy, col, 110)
        else:
            c.rect(x - 1, y - 1, x + 2, y + 2, OUT)
            c.px(x, y, mix(col, WHITE, 0.2) if i in lit else shade(col, 0.35))


def works_front(f: int | None, glow: bool = False) -> Canvas:
    c = Canvas(16)
    lit = set() if f is None else {f, (f + 1) % 4}
    if not glow:
        # ammo crate (wood slats, brass stencil) over a press-ram bracket
        c.rect(2, 2, 14, 10, OUT)
        c.rect(3, 3, 13, 9, hx("#6a5532"))
        for y in (5, 7):
            c.hline(3, 13, y, hx("#4a3a20"))
        c.hline(3, 13, 3, hx("#8d7446"))
        for x in range(4, 13, 2):
            c.rect(x, 4, x + 1, 5, AMB if x % 4 == 0 else AMB_L)   # rounds poking out the top row
        c.rect(7, 6, 9, 9, OUT)                                        # press ram emblem
        c.rect(7, 6, 9, 7, HI)
        c.px(8, 8, LT)
        c.rect(2, 11, 14, 14, OUT)
        c.rect(3, 12, 13, 13, DK)
    lights(c, 12, 4, lit, AMB, x0=4, step=3, glow=glow)
    return c


def retort_front(f: int | None, glow: bool = False) -> Canvas:
    c = Canvas(16)
    viol = hx("#c070e8")
    # round-bottom flask: neck, shoulders, belly
    liquid_y = 9
    if not glow:
        c.rect(6, 1, 10, 4, OUT)
        c.rect(7, 1, 9, 4, hx("#c8d4e0"))
        c.disc(8, 10, 5.6, OUT)
        c.disc(8, 10, 4.8, hx("#aebccc"))
        c.rect(5, 4, 11, 6, OUT)
        c.rect(6, 4, 10, 6, hx("#aebccc"))
        # liquid
        for y in range(liquid_y, 15):
            for x in range(3, 14):
                if (x + 0.5 - 8) ** 2 + (y + 0.5 - 10) ** 2 <= 4.8 ** 2:
                    c.px(x, y, viol if y > liquid_y else mix(viol, WHITE, 0.4))
        c.px(5, 7, WHITE)
        c.px(5, 8, WHITE)
        c.px(6, 6, WHITE)
        c.rect(7, 0, 10, 1, hx("#7a5634"))              # cork
    if f is not None:
        # bubbles rise (period 4 frames)
        bubs = [(7, 13), (10, 12), (8, 11)]
        for i, (bx, by) in enumerate(bubs):
            y = by - ((f + i) % 4) * 2
            if y < liquid_y - 4 or y > 14:
                continue
            if glow:
                c.blend(bx, y, hx("#f0c8ff"), 230)
                c.blend(bx + 1, y, hx("#c070e8"), 120)
                c.blend(bx, y + 1, hx("#c070e8"), 120)
            else:
                c.px(bx, y, hx("#f6e6ff"))
        if glow:
            for x in range(4, 13):
                for y in range(liquid_y + 1, 15):
                    if (x + 0.5 - 8) ** 2 + (y + 0.5 - 10) ** 2 <= 4.4 ** 2:
                        c.blend(x, y, viol, 80 + 30 * ((f + x) % 2))
    return c


def gantry_front(f: int | None, glow: bool = False) -> Canvas:
    c = Canvas(16)
    tx = 4 if f is None else [3, 6, 9, 6][f % 4]          # trolley position
    cab = 5 if f is None else [5, 6, 7, 6][f % 4]          # cable length
    if not glow:
        # overhead rail with end stops
        c.rect(1, 1, 15, 4, OUT)
        c.rect(2, 2, 14, 3, MD)
        c.hline(2, 14, 2, LT)
        for x in (2, 13):
            c.rect(x, 1, x + 1, 4, AMB_D)
        # trolley, cable, hook block
        c.rect(tx, 1, tx + 4, 5, OUT)
        c.rect(tx + 1, 2, tx + 3, 4, hx("#c8a020"))
        c.px(tx + 1, 2, AMB_L)
        c.vline(tx + 2, 5, 5 + cab, OUT)
        hy = 5 + cab
        c.rect(tx, hy, tx + 5, hy + 3, OUT)
        c.rect(tx + 1, hy + 1, tx + 4, hy + 2, hx("#c8a020"))
        # a rifle silhouette slung under the hook
        c.rect(1, 13, 15, 15, OUT)
        c.rect(2, 13, 14, 14, DK)
        c.rect(4, 13, 12, 14, MD)
        c.px(3, 13, LT)
    else:
        if f is not None:
            c.blend(tx + 2, 2, AMB_L, 255)
            for (dx, dy) in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                c.blend(tx + 2 + dx, 2 + dy, hx("#ffb400"), 120)
    return c


def rampart_front(f: int | None, glow: bool = False) -> Canvas:
    c = Canvas(16)
    cy = hx("#80c8f0")
    if not glow:
        # crenellated bastion shield
        c.rect(1, 1, 15, 3, OUT)
        for x in (2, 6, 10):
            c.rect(x, 1, x + 4, 4, OUT)
            c.rect(x + 1, 1, x + 3, 3, LT)
            c.px(x + 1, 1, HI)
        c.rect(2, 3, 14, 13, OUT)
        c.rect(3, 4, 13, 12, MD)
        c.bevel(3, 4, 13, 12, LT, DK)
        c.rect(5, 6, 11, 11, OUT)
        c.rect(6, 7, 10, 10, hx("#20507a"))
        c.px(6, 7, cy)
        c.rect(7, 13, 9, 15, OUT)
        c.px(8, 13, LT)
    if f is not None:
        sy = 7 + (f % 4)                                    # scan line sweeps the core
        if glow:
            for x in range(6, 10):
                c.blend(x, min(sy, 9), cy, 240)
            for x in range(6, 10):
                c.blend(x, 10 if sy < 9 else 9, cy, 70)
            c.blend(8, 14, cy, 120)
        else:
            for x in range(6, 10):
                c.px(x, min(sy, 9), mix(cy, WHITE, 0.5))
    return c


def hatch_front(lit: bool, f: int | None = None) -> Canvas:
    c = Canvas(16)
    c.disc(8, 8, 7, OUT)
    c.disc(8, 8, 6.2, MD)
    c.ring(8, 8, 5.2, 6.2, LT)
    c.disc(8, 8, 4.4, OUT)
    c.disc(8, 8, 3.4, DK)
    c.rect(7, 1, 9, 5, OUT)                                  # keyed notch
    c.rect(7, 2, 9, 4, AMB if lit else AMB_D)
    c.rect(7, 6, 9, 11, BLK)                                 # mounting socket
    c.hline(7, 9, 6, LT)
    for k in range(4):
        x = 8 + round(6.0 * np.cos(k * np.pi / 2 + np.pi / 4))
        y = 8 + round(6.0 * np.sin(k * np.pi / 2 + np.pi / 4))
        c.px(x, y, OUT)
        if f is not None:                            # clamp lamps chase around the ring
            c.px(x, y, AMB_L if k == f else AMB_D)
            if k == f:
                c.blend(x + (1 if x < 8 else -1), y, AMB, 120)
    if lit:
        for (x, y) in ((7, 7), (8, 7)):
            c.px(x, y, mix(AMB, WHITE, 0.3) if f is None or f % 2 == 0 else AMB)
    return c


def bottom_plate() -> Canvas:
    c = Canvas(16)
    plate(c)
    return c


# ================================================================ dispatch
def frames_for(machine: str, part: str, n: int) -> list:
    """Return the list of Canvas frames for a block.machine.<machine>.<part> id (n frames)."""
    active = part.endswith("_active") or "_active_" in part
    glow = part.endswith("glow")
    base = part.replace("overlay_", "").replace("_active", "").replace("_glow", "")
    rng = range(n) if n > 1 else [None]
    fr = [None] * n if not active else list(range(n))
    if machine in ("ammo_press", "ammo_press_steam"):
        steam = machine.endswith("steam")
        if base == "front":
            return [press_front_glow(i) if glow else press_front(i if active else None, steam) for i in fr]
        if base == "side":
            return [press_side(i if active else None, steam) for i in fr]
        if base == "top":
            return [hopper_top(i if active else None, steam) for i in fr]
        if base == "bottom":
            return [bottom_plate()]
    if machine == "reaction_chamber":
        fn = {"front": reaction_front, "side": reaction_side, "top": reaction_top}.get(base)
        if fn:
            return [fn(i if active else None) for i in fr]
        return [bottom_plate()]
    if machine == "repair_bench":
        if base == "front":
            return [repair_front(i if active else None) for i in fr]
        if base == "side":
            return [repair_side(i if active else None) for i in fr]
        if base == "top":
            return [repair_top(i if active else None) for i in fr]
        return [bottom_plate()]
    if machine == "camo_bench":
        fn = {"front": camo_front, "side": camo_side, "top": camo_top}.get(base, bottom_plate)
        if base in ("front", "top"):
            return [fn(i if active else None) for i in fr]
        return [fn()]
    if machine == "ammunition_works":
        return [works_front(i if active else None, glow) for i in fr]
    if machine == "retort":
        return [retort_front(i if active else None, glow) for i in fr]
    if machine == "armory_gantry":
        return [gantry_front(i if active else None, glow) for i in fr]
    if machine == "rampart":
        return [rampart_front(i if active else None, glow) for i in fr]
    if machine == "gun_mount_hatch":
        return [hatch_front(active, i if active else None) for i in fr]
    raise KeyError(f"{machine}.{part}")
