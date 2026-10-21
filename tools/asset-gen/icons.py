"""Render every ``kind == "icon"`` manifest entry as a 16x16 GT-style PNG.

    python -I -X utf8 tools/asset-gen/icons.py            # all icons
    python -I -X utf8 tools/asset-gen/icons.py --sheet    # also contact sheet

Procedural and deterministic (speckle uses crc32-seeded random.Random).
Output: src/main/resources/assets/gregguns/<manifest path>, credits CSV in
tools/asset-gen/credits/icons.csv, contact sheet docs/assets/previews/icons.png.
"""
import csv
import json
import random
import sys
import zlib
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from pixel import *  # noqa: E402,F403
import pixel as px  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/gregguns"

# extra shared ramps
STONE = ramp((142, 142, 146))
TAN = ramp((196, 164, 108))
CARBON = ramp((34, 36, 44), 0.8)
WARMGRAY = ramp((172, 164, 154))
SLATE = ramp((86, 96, 108))
TEAL = glow((60, 220, 200))
LEATHER = ramp((120, 78, 40))
CHROME = ramp((205, 212, 224), 1.2)
AMBER_G = glow((235, 150, 40))
FUEL = ramp((150, 90, 40))
ALIEN = ramp((218, 224, 214))
COPPER = ramp((196, 112, 50))
BRONZE = ramp((205, 127, 50))
IRON = ramp((190, 190, 196))
STEELBLUE = ramp((84, 92, 118))


class Ctx:
    def __init__(self, entry):
        self.id = entry["id"][5:]
        self.pal = [hexrgb(p) for p in entry["palette"]]
        self.m = ramp(self.pal[0])
        self.m2 = ramp(self.pal[1]) if len(self.pal) > 1 else self.m
        self.rng = random.Random(zlib.crc32(entry["id"].encode()))


def speckle(c, ctx, coords, rp, n=6, idx=SH):
    coords = [p for p in coords if p in c.px]
    for _ in range(min(n, len(coords))):
        x, y = ctx.rng.choice(sorted(coords))
        c.pix(x, y, rp[idx])


def weave(c, coords, rp):
    for (x, y) in coords:
        if (x, y) in c.px and (x + y) % 3 == 0:
            c.pix(x, y, rp)


REG = {}


def icon(*names):
    def deco(fn):
        for n in names:
            REG[n] = fn
        return fn
    return deco


# ======================================================== small gun drawers
def pistol_small(c, x):
    c.rect(0, 0, 8, 2, x.m)
    c.rect(0, 3, 6, 3, POLY)
    grip(c, 1, 4, 6, POLY, w=3, lean=1)
    c.pix(9, 1, x.m[DK])
    c.hline(2, 3, 1, x.m[DK])


def revolver_small(c, x, body=None, gr=WOOD):
    body = body or x.m
    c.rect(0, 0, 3, 3, body)
    c.rect(4, 0, 9, 1, body)
    c.vline(2, 0, 3, body[DK])
    grip(c, 0, 4, 6, gr, w=3, lean=1)


def mac_small(c, x):
    c.rect(1, 0, 7, 3, x.m)
    c.rect(8, 1, 9, 2, POLY)
    c.rect(4, 4, 6, 6, POLY)
    c.pix(1, 1, x.m[DK])


def deatom_small(c, x):
    c.rect(0, 0, 7, 3, CHROME)
    c.rect(8, 1, 9, 2, CHROME[DK])
    c.pix(4, 1, BLUE_G[HL])
    c.pix(5, 1, BLUE_G[BS])
    grip(c, 1, 4, 6, POLY, w=3, lean=1)


def akimbo(small, x):
    """Two guns offset diagonally: the front one bright, the one behind dimmed."""
    t = Canvas()
    small(t, x)
    back = Canvas()
    back.px = {p: tint(col, 0.72) + (255,) for p, col in t.shifted(0, 0).px.items()}
    c = Canvas()
    c.paste(back.shifted(1, 7))
    c.paste(t.shifted(5, 1))
    return c


# ======================================================== guns
@icon("handcannon")
def _(c, x):
    c.rect(6, 3, 13, 7, STONE)
    speckle(c, x, [(a, b) for a in range(6, 14) for b in range(3, 8)], STONE, 7, DK)
    speckle(c, x, [(a, b) for a in range(6, 14) for b in range(3, 8)], STONE, 5, HL)
    c.rect(13, 4, 13, 6, x.m)                    # bronze liner
    c.pix(13, 5, x.m[DK])
    c.rect(9, 3, 9, 7, x.m, bevel=False)         # bronze band
    c.rect(2, 4, 6, 7, x.m)                      # bronze frame
    grip(c, 2, 8, 13, WOOD, w=4, lean=2)
    c.pix(1, 3, x.m[DK]); c.pix(2, 3, x.m[DK])   # hammer
    c.pix(6, 8, x.m[DK]); c.pix(7, 9, x.m[DK])


@icon("pistol_9mm")
def _(c, x):
    c.rect(3, 3, 12, 5, x.m)                     # steel slide
    c.pix(13, 4, x.m[DK])
    c.rect(3, 6, 10, 7, POLY)
    grip(c, 4, 8, 12, POLY, w=3, lean=1)
    c.hline(5, 6, 4, x.m[DK]); c.hline(9, 10, 4, x.m[DK])     # serrations
    c.pix(3, 2, x.m[DK]); c.pix(12, 2, x.m[DK])               # sights
    c.hline(7, 9, 8, POLY[DK]); c.pix(9, 9, POLY[DK])         # trigger guard


@icon("revolver")
def _(c, x):
    _revolver(c, x, x.m, WOOD)


def _revolver(c, x, body, gr, long_barrel=False, ornate=False):
    end = 14
    c.rect(8, 4, end, 5, body)
    c.rect(8, 6, 11 if not long_barrel else 13, 6, body)           # under lug
    c.rect(3, 4, 7, 7, body)
    c.rect(5, 3, 8, 7, body)                                       # cylinder
    c.vline(6, 3, 7, body[DK]); c.pix(7, 5, body[DK])
    c.pix(end, 3, body[DK])
    c.pix(2, 3, body[DK]); c.pix(3, 3, body[DK])                   # hammer
    grip(c, 3, 8, 13, gr, w=3, lean=2)
    c.pix(6, 8, body[DK]); c.pix(5, 9, body[DK])                   # trigger
    if ornate:
        c.pix(9, 4, body[HL]); c.pix(11, 4, body[HL]); c.pix(13, 4, body[HL])
        c.pix(4, 5, (120, 230, 240)); c.pix(4, 4, (200, 255, 255))


@icon("sawed_off")
def _(c, x):
    c.rect(7, 4, 14, 5, x.m)
    c.rect(7, 6, 14, 7, x.m)
    c.pix(14, 4, x.m[DK]); c.pix(14, 6, x.m[DK])
    c.rect(4, 4, 8, 8, x.m)
    c.pix(6, 3, x.m[DK])
    stock(c, 0, 5, 4, 8, WOOD, drop=3)
    c.pix(6, 9, x.m[DK]); c.pix(7, 9, x.m[DK])
    c.hline(2, 3, 6, WOOD[DK])


@icon("bolt_action_rifle")
def _(c, x):
    stock(c, 0, 7, 5, 9, WOOD, drop=3)
    c.rect(6, 7, 9, 8, x.m)
    c.hline(10, 14, 7, x.m)
    c.rect(7, 9, 12, 9, WOOD)
    scope(c, 5, 11, 4, BLACK, lens=(130, 200, 230), h=2)
    c.pix(9, 6, BLACK[DK]); c.pix(8, 6, x.m[LT])
    c.pix(6, 9, x.m[DK]); c.pix(13, 6, x.m[DK])
    c.pix(10, 6, x.m[HL])                                          # bolt handle


@icon("combat_knife")
def _(c, x):
    c.poly([(14, 1), (14, 3), (8, 9), (6, 8)], BLACK)
    c.line(6, 8, 13, 1, x.m[HL])
    c.line(7, 8, 12, 3, BLACK[BS])
    c.line(4, 8, 7, 11, x.m[BS])
    c.line(5, 7, 8, 10, x.m[DK])
    c.poly([(5, 9), (7, 10), (3, 14), (1, 12)], RUBBER)
    c.pix(5, 10, RUBBER[HL]); c.pix(3, 12, RUBBER[HL])


@icon("crowbar")
def _(c, x):
    bar = c.line_coords(3, 13, 11, 5, thick=2)
    c.fill(bar, x.m)
    c.fill([(11, 4), (12, 3), (13, 3), (14, 4), (14, 5), (13, 6), (12, 4)], x.m)
    c.pix(13, 4, x.m[DK])
    c.pix(14, 5, x.m[DK])
    c.pix(2, 13, x.m[SH]); c.pix(3, 14, x.m[SH])
    c.pix(5, 11, x.m[HL]); c.pix(7, 9, x.m[HL])


@icon("stielgranate")
def _(c, x):
    c.rect(7, 6, 8, 12, WOOD)
    c.rect(5, 1, 10, 5, x.m)
    c.hline(5, 10, 3, x.m[DK])
    c.rect(6, 1, 9, 1, x.m[LT], bevel=False)
    c.rect(7, 12, 8, 13, WOOD[DK], bevel=False)
    for p in ((10, 12), (11, 12), (11, 13), (10, 14)):
        c.pix(p[0], p[1], x.m[HL])
    c.pix(8, 13, WHITE[SH])


@icon("thompson_smg")
def _(c, x):
    stock(c, 0, 5, 4, 8, WOOD, drop=3)
    c.rect(4, 4, 10, 7, x.m)
    c.rect(11, 4, 14, 6, x.m)
    c.vline(12, 4, 6, x.m[DK]); c.vline(14, 4, 6, x.m[DK])
    c.rect(9, 8, 10, 10, WOOD)                                     # fore grip
    grip(c, 4, 8, 11, WOOD, w=2, lean=1)
    c.rect(6, 8, 7, 13, POLY)                                      # stick mag
    c.pix(5, 3, x.m[DK]); c.pix(14, 3, x.m[DK])


@icon("mac10")
def _(c, x):
    c.rect(3, 3, 11, 7, x.m)
    c.rect(12, 4, 14, 6, POLY)
    c.rect(6, 8, 9, 13, POLY)                                      # grip with mag
    c.hline(6, 9, 9, POLY[DK])
    c.hline(1, 2, 4, x.m[DK]); c.vline(1, 4, 7, x.m[DK]); c.hline(1, 3, 7, x.m[DK])
    c.rect(4, 4, 5, 5, x.m[DK], bevel=False)
    c.pix(10, 2, x.m[DK]); c.hline(9, 11, 5, x.m[SH])


@icon("combat_shotgun")
def _(c, x):
    stock(c, 0, 5, 5, 8, POLY, drop=3)
    c.rect(5, 5, 9, 7, x.m)
    c.rect(9, 4, 14, 5, x.m)
    c.rect(9, 6, 14, 6, x.m[SH] and ramp(tint(x.pal[0], 0.75)))
    c.rect(9, 7, 12, 8, POLY)
    c.stripes([(a, b) for a in range(9, 13) for b in (7, 8)], POLY[DK], 2, 0, True)
    grip(c, 5, 8, 11, POLY, w=2, lean=1)
    c.pix(14, 4, x.m[HL]); c.pix(6, 4, x.m[DK])


def _m4(c, x, suppressed=False):
    c.rect(1, 6, 4, 8, POLY)
    c.rect(5, 5, 10, 7, x.m)
    c.rect(5, 8, 9, 8, POLY)
    c.rect(6, 2, 9, 4, BLACK)                                      # optic
    c.pix(9, 3, (255, 70, 60)); c.pix(6, 3, BLACK[HL])
    if suppressed:
        c.rect(10, 4, 14, 7, RUBBER)
        c.stripes([(a, b) for a in range(10, 15) for b in range(4, 8)], RUBBER[HL], 2, 0, False)
        c.rect(10, 5, 11, 6, POLY)
    else:
        c.rect(10, 5, 13, 7, POLY)
        c.rect(14, 5, 14, 5, x.m)
        c.pix(14, 4, x.m[DK]); c.pix(14, 6, x.m[DK])
        c.pix(13, 4, x.m[DK])
    magazine(c, 7, 9, 13, POLY, w=2, curve=1)
    grip(c, 4, 9, 12, POLY, w=2, lean=1)


@icon("m4")
def _(c, x):
    _m4(c, x)


@icon("infiltrator")
def _(c, x):
    _m4(c, x, suppressed=True)


@icon("ak47")
def _(c, x):
    stock(c, 0, 5, 4, 8, WOOD, drop=3)
    c.rect(4, 5, 9, 7, x.m)
    c.hline(4, 9, 4, x.m[LT])
    c.rect(9, 4, 13, 4, x.m)                                       # gas tube
    c.rect(10, 5, 14, 5, x.m)
    c.rect(10, 6, 13, 7, WOOD)
    c.pix(14, 3, x.m[DK]); c.pix(14, 4, x.m[DK])
    magazine(c, 6, 8, 13, BLACK, w=2, curve=3)
    grip(c, 4, 8, 11, WOOD, w=2, lean=1)
    c.pix(8, 6, x.m[DK])


@icon("aug")
def _(c, x):
    c.rect(1, 5, 2, 8, RUBBER)                                     # butt
    c.rect(3, 5, 10, 8, x.m)
    c.rect(4, 3, 8, 4, BLACK)
    c.pix(8, 3, (255, 90, 70)); c.pix(4, 3, BLACK[HL])
    c.rect(11, 5, 14, 5, x.m)
    c.rect(10, 5, 11, 7, POLY)
    c.rect(9, 8, 10, 10, POLY)                                     # foregrip
    magazine(c, 4, 9, 12, ramp(tint(x.pal[0], 0.7)), w=2, curve=0)
    c.hline(6, 7, 9, x.m[DK]); c.pix(7, 10, x.m[DK])
    c.hline(4, 9, 6, x.m[SH])


@icon("scar")
def _(c, x):
    stock(c, 0, 5, 4, 8, TAN, drop=1)
    c.rect(1, 4, 3, 4, TAN[DK], bevel=False)
    c.rect(4, 5, 10, 8, TAN)
    c.rect(4, 4, 12, 4, BLACK)
    c.rect(6, 2, 8, 3, BLACK)
    c.pix(8, 2, (255, 90, 70))
    c.rect(10, 5, 13, 8, TAN)
    c.rect(14, 5, 14, 6, x.m)
    c.pix(14, 4, x.m[DK])
    magazine(c, 6, 9, 13, BLACK, w=3, curve=1)
    grip(c, 3, 9, 12, TAN, w=2, lean=1)
    c.hline(11, 12, 7, TAN[DK])


@icon("vector")
def _(c, x):
    c.poly([(3, 4), (12, 4), (13, 7), (10, 9), (3, 8)], x.m)
    c.rect(13, 5, 14, 6, POLY)
    c.rect(2, 4, 2, 6, POLY[DK], bevel=False)
    c.hline(0, 1, 5, POLY[SH]); c.vline(0, 5, 8, POLY[SH]); c.hline(0, 2, 8, POLY[SH])
    c.poly([(7, 9), (9, 9), (11, 14), (9, 14)], POLY)               # forward-raked mag
    grip(c, 3, 9, 13, POLY, w=2, lean=1)
    c.hline(5, 9, 5, x.m[DK])
    c.pix(8, 3, x.m[DK]); c.pix(10, 3, x.m[DK])


@icon("as50")
def _(c, x):
    c.rect(0, 6, 2, 10, RUBBER)
    c.rect(3, 6, 9, 9, x.m)
    scope(c, 3, 11, 2, BLACK, lens=(130, 200, 230), h=3)
    c.pix(5, 3, BLACK[HL]); c.hline(3, 10, 2, BLACK[HL])
    c.rect(9, 7, 13, 8, x.m)
    c.rect(13, 6, 14, 9, x.m)                                      # muzzle brake
    c.pix(13, 7, x.m[DK]); c.pix(13, 8, x.m[DK])
    magazine(c, 5, 10, 12, POLY, w=3)
    c.line(10, 9, 8, 13, x.m[SH]); c.line(11, 9, 13, 13, x.m[SH])  # bipod


@icon("lmg")
def _(c, x):
    stock(c, 0, 5, 3, 8, POLY, drop=1)
    c.rect(3, 5, 9, 8, x.m)
    c.rect(4, 3, 8, 3, x.m[LT], bevel=False)
    c.vline(4, 3, 4, x.m[DK]); c.vline(8, 3, 4, x.m[DK])
    c.rect(10, 4, 14, 6, x.m)
    c.stripes([(a, b) for a in range(10, 14) for b in (4, 5, 6)], x.m[DK], 2, 0, False)
    c.rect(5, 9, 8, 12, OLIVE)                                     # box
    c.hline(5, 8, 10, OLIVE[DK])
    for p in ((9, 9), (10, 10), (10, 11), (9, 12)):
        c.pix(p[0], p[1], BRASS[BS])
    c.line(12, 7, 11, 12, x.m[SH]); c.line(12, 7, 14, 12, x.m[SH])  # bipod


@icon("grenade_launcher")
def _(c, x):
    stock(c, 0, 6, 3, 9, POLY, drop=1)
    c.rect(4, 6, 6, 8, x.m)
    c.rect(6, 3, 10, 8, x.m)                                       # drum
    for xx in (7, 9):
        c.vline(xx, 4, 7, x.m[DK])
    c.rect(11, 5, 14, 7, x.m)
    c.rect(14, 5, 14, 7, POLY)
    c.pix(14, 6, POLY[DK])
    grip(c, 4, 9, 13, POLY, w=2, lean=1)
    c.pix(12, 4, x.m[DK])


@icon("rocket_launcher")
def _(c, x):
    c.rect(2, 4, 12, 7, x.m)
    c.rect(0, 3, 2, 8, x.m)                                        # rear flare
    c.rect(12, 3, 14, 8, x.m)                                      # front flare
    c.rect(13, 4, 14, 7, BLACK[DK], bevel=False)
    c.rect(14, 5, 14, 6, RED[BS], bevel=False)
    c.vline(6, 4, 7, x.m[DK]); c.vline(9, 4, 7, x.m[DK])
    c.rect(7, 1, 8, 3, BLACK)
    grip(c, 5, 8, 12, POLY, w=2, lean=1)
    c.rect(9, 8, 9, 10, POLY)


@icon("flamethrower")
def _(c, x):
    c.rect(3, 4, 8, 7, x.m)
    c.rect(9, 5, 12, 6, x.m)                                       # nozzle
    for xx in (9, 11):
        c.pix(xx, 4, ORANGE_G[BS]); c.pix(xx + 1, 4, ORANGE_G[SH])
    c.fill([(13, 4), (13, 5), (13, 6), (14, 5), (14, 6), (14, 4)], ORANGE_G, bevel=False)
    c.pix(14, 5, (255, 240, 120)); c.pix(13, 5, (255, 220, 80)); c.pix(14, 3, (255, 90, 30))
    c.rect(2, 9, 8, 12, FUEL)                                      # tank
    c.vline(5, 9, 12, FUEL[DK]); c.hline(2, 8, 10, FUEL[LT])
    c.vline(6, 8, 8, x.m[DK])
    grip(c, 4, 8, 8, POLY, w=2, lean=0)
    c.hline(9, 11, 7, x.m[DK]); c.vline(9, 7, 9, x.m[SH]); c.hline(9, 9, 9, x.m[SH])


@icon("chainsaw")
def _(c, x):
    c.rect(2, 4, 8, 10, ramp((214, 100, 30)))
    c.rect(8, 6, 14, 8, x.m)
    for xx in range(8, 15):
        c.pix(xx, 5, BLACK[BS] if xx % 2 == 0 else x.m[HL])
        c.pix(xx, 9, BLACK[BS] if xx % 2 == 1 else x.m[SH])
    c.hline(3, 6, 2, POLY[BS]); c.vline(3, 2, 4, POLY[BS]); c.vline(6, 2, 4, POLY[BS])
    c.rect(1, 7, 2, 11, POLY)
    c.rect(4, 6, 6, 8, BLACK, bevel=False)
    c.pix(3, 10, WHITE[BS]); c.pix(2, 9, WHITE[BS])


def _hammer(c, x, adv=False):
    c.rect(3, 1, 12, 5, x.m)
    c.rect(1, 2, 2, 4, x.m[DK] and ramp(tint(x.pal[0], .8)))
    c.rect(13, 2, 14, 4, ramp(tint(x.pal[0], .8)))
    c.rect(7, 6, 8, 13, POLY)
    c.hline(7, 8, 13, POLY[DK])
    for p in ((9, 7), (10, 8), (10, 9), (9, 10), (9, 11)):
        c.pix(p[0], p[1], RUBBER[HL] if p[1] % 2 else RUBBER[BS])
    if adv:
        for xx in range(4, 12, 2):
            c.pix(xx, 3, (110, 245, 245))
            c.pix(xx, 2, (210, 255, 255))
        c.rect(5, 6, 10, 6, GOLD[BS], bevel=False)
    else:
        c.vline(5, 1, 5, x.m[DK])
        c.vline(10, 1, 5, x.m[DK])


@icon("power_hammer")
def _(c, x):
    _hammer(c, x)


@icon("power_hammer_adv")
def _(c, x):
    _hammer(c, x, True)


# ---------------------------------------------------------------- energy guns
@icon("laser_rifle")
def _(c, x):
    stock(c, 0, 6, 3, 8, WARMGRAY, drop=1)
    c.rect(3, 5, 10, 8, WARMGRAY)
    c.rect(4, 4, 9, 4, x.m)
    c.rect(11, 6, 13, 7, x.m)
    c.rect(14, 5, 14, 8, RED_G, bevel=False)
    c.rect(5, 6, 8, 7, RED_G, bevel=False)
    c.pix(5, 6, RED_G[HL])
    c.vline(11, 5, 8, x.m[DK]); c.vline(13, 5, 8, x.m[DK])
    grip(c, 4, 9, 12, POLY, w=2, lean=1)


@icon("pdw")
def _(c, x):
    c.rect(1, 6, 3, 7, CARBON)
    c.rect(4, 4, 11, 8, CARBON)
    weave(c, [(a, b) for a in range(4, 12) for b in range(4, 9)], CARBON[LT])
    c.rect(12, 6, 14, 6, x.m)
    c.rect(11, 5, 12, 7, x.m)
    c.hline(5, 10, 4, x.m[BS])
    c.pix(8, 3, (255, 70, 60)); c.hline(7, 9, 2, BLACK[BS])
    c.poly([(6, 9), (9, 9), (10, 14), (7, 14)], CARBON)
    grip(c, 4, 9, 12, CARBON, w=2, lean=1)
    c.pix(5, 6, TEAL[HL]); c.pix(6, 6, TEAL[BS])


@icon("pulse_rifle")
def _(c, x):
    stock(c, 0, 6, 3, 9, CARBON, drop=0)
    c.rect(3, 5, 11, 8, CARBON)
    weave(c, [(a, b) for a in range(3, 12) for b in range(5, 9)], CARBON[LT])
    c.rect(12, 6, 14, 7, x.m)
    c.rect(11, 5, 11, 8, x.m, bevel=False)
    # diamond lens fire control
    for p, s in (((7, 2), HL), ((6, 3), BS), ((7, 3), HL), ((8, 3), SH), ((7, 4), BS)):
        c.pix(p[0], p[1], CYAN[s])
    c.hline(5, 9, 4, x.m[BS])
    magazine(c, 6, 9, 12, BLACK, w=3)
    c.pix(7, 11, CYAN[BS])
    grip(c, 3, 9, 12, CARBON, w=2, lean=1)


@icon("minigun")
def _(c, x):
    c.rect(3, 3, 7, 9, POLY)
    for yy in (3, 5, 7):
        c.rect(8, yy, 14, yy + 1, x.m)
    c.vline(10, 2, 9, x.m[DK]); c.vline(14, 2, 9, x.m[DK])
    c.pix(14, 4, BLACK[DK]); c.pix(14, 6, BLACK[DK]); c.pix(14, 8, BLACK[DK])
    c.disc(5.5, 11.5, 2, ramp(tint(x.pal[0], 0.8)))
    c.pix(5, 11, x.m[DK]); c.pix(6, 11, x.m[DK])
    c.hline(1, 3, 2, POLY[BS]); c.vline(1, 2, 5, POLY[BS])                     # carry handle
    c.hline(4, 6, 5, x.m[HL])


@icon("tesla_rifle")
def _(c, x):
    stock(c, 0, 6, 3, 9, x.m, drop=1)
    c.rect(3, 6, 9, 9, x.m)
    c.hline(10, 14, 7, x.m[DK])
    for xx in (10, 12, 14):
        c.rect(xx, 5, xx, 9, COPPER, bevel=False)
        c.pix(xx, 5, COPPER[HL])
    for p in ((11, 4), (12, 3), (13, 4), (14, 3), (11, 10), (12, 11), (13, 10)):
        c.pix(p[0], p[1], CYAN[HL] if p[1] < 5 else CYAN[BS])
    c.pix(12, 4, CYAN[BS])
    grip(c, 4, 10, 13, POLY, w=2, lean=1)
    c.rect(5, 7, 6, 8, CYAN, bevel=False)


@icon("sonic_rifle")
def _(c, x):
    stock(c, 0, 6, 3, 8, SLATE, drop=1)
    c.rect(3, 5, 10, 8, SLATE)
    c.hline(4, 9, 6, TEAL[BS])
    c.pix(4, 6, TEAL[HL]); c.pix(5, 6, TEAL[HL])
    c.poly([(10, 5), (12, 5), (12, 8), (10, 8)], x.m)
    c.rect(13, 2, 14, 11, SLATE)
    c.rect(13, 4, 14, 9, TEAL[SH] and ramp((30, 150, 140)), bevel=False)
    for yy in (5, 8):
        c.pix(14, yy, TEAL[HL])
    c.pix(13, 6, TEAL[BS]); c.pix(13, 7, TEAL[BS])
    grip(c, 4, 9, 12, SLATE, w=2, lean=1)
    c.hline(11, 12, 6, TEAL[BS])


@icon("bio_gun")
def _(c, x):
    c.rect(3, 6, 10, 8, x.m)
    c.rect(4, 1, 8, 5, GLASS)
    c.rect(4, 3, 8, 5, GREEN_G, bevel=False)
    c.hline(4, 8, 3, GREEN_G[HL]); c.hline(4, 8, 1, x.m[LT]); c.vline(4, 1, 5, x.m[HL])
    c.rect(11, 6, 14, 7, x.m)
    c.pix(14, 6, BLACK[DK]); c.pix(14, 7, BLACK[DK])
    c.pix(14, 8, GREEN_G[BS]); c.pix(14, 9, GREEN_G[SH])
    c.line(9, 4, 11, 5, GREEN_G[SH])
    grip(c, 4, 9, 13, POLY, w=3, lean=1)
    stock(c, 0, 6, 2, 8, POLY, drop=1)


@icon("blaster_rifle")
def _(c, x):
    c.rect(0, 5, 1, 8, POLY)
    c.rect(2, 4, 11, 9, x.m)
    for xx in (3, 5, 7):
        c.vline(xx, 3, 3, x.m[BS])
    c.rect(11, 4, 12, 9, x.m[DK] and ramp(tint(x.pal[0], 0.6)))
    c.rect(13, 5, 14, 8, ORANGE_G, bevel=False)
    c.pix(14, 6, (255, 240, 150)); c.pix(13, 6, (255, 200, 80))
    c.hline(4, 9, 6, x.m[DK])
    c.rect(5, 10, 8, 12, POLY)
    c.pix(6, 11, ORANGE_G[BS]); c.pix(7, 11, ORANGE_G[BS])
    grip(c, 9, 10, 13, POLY, w=2, lean=1)


@icon("deatomizer_pistol")
def _(c, x):
    c.rect(3, 3, 11, 7, CHROME)
    c.rect(12, 4, 14, 6, CHROME)
    for xx in (12, 14):
        c.vline(xx, 3, 7, CHROME[DK])
    c.rect(6, 4, 7, 5, BLUE_G, bevel=False)
    c.pix(6, 4, (200, 230, 255))
    c.stripes([(a, b) for a in range(3, 6) for b in range(3, 8)], CHROME[DK], 2, 0, False)
    grip(c, 4, 8, 13, POLY, w=3, lean=1)
    c.pix(8, 8, CHROME[DK]); c.pix(9, 8, CHROME[DK])
    c.pix(8, 2, x.m[HL]); c.pix(10, 2, x.m[HL])


@icon("scatter_laser_rifle")
def _(c, x):
    stock(c, 0, 6, 3, 8, x.m, drop=1)
    c.rect(3, 5, 9, 8, x.m)
    c.rect(10, 3, 14, 4, x.m)
    c.rect(10, 6, 14, 7, x.m)
    c.rect(10, 9, 14, 10, x.m)
    c.rect(9, 3, 9, 10, x.m[DK] and ramp(tint(x.pal[0], 0.65)), bevel=False)
    for yy in (3, 6, 9):
        c.pix(14, yy, RED_G[HL]); c.pix(14, yy + 1, RED_G[BS])
    c.rect(5, 6, 7, 7, RED_G, bevel=False)
    grip(c, 4, 9, 12, POLY, w=2, lean=1)


@icon("golden_revolver")
def _(c, x):
    _revolver(c, x, GOLD, ramp((60, 40, 30)), long_barrel=True, ornate=True)
    c.hline(3, 3, 3, GOLD[HL])


@icon("nether_blaster")
def _(c, x):
    body = ramp((116, 110, 104))
    c.rect(0, 6, 2, 8, body)
    c.rect(3, 3, 11, 9, body)
    speckle(c, x, [(a, b) for a in range(3, 12) for b in range(3, 10)], body, 9, DK)
    for p in ((4, 4), (10, 8), (9, 3), (5, 9)):
        c.pix(p[0], p[1], (140, 70, 40))
    c.rect(6, 4, 9, 8, ORANGE_G, bevel=False)
    c.pix(7, 5, ORANGE_G[HL]); c.pix(8, 6, ORANGE_G[HL]); c.pix(7, 7, (255, 240, 170)); c.pix(6, 8, ORANGE_G[SH])
    c.rect(12, 4, 14, 8, body)
    c.pix(14, 6, ORANGE_G[BS]); c.pix(13, 6, ORANGE_G[SH])
    c.vline(12, 4, 8, body[DK])
    grip(c, 4, 10, 13, POLY, w=3, lean=1)


@icon("alien_blaster")
def _(c, x):
    c.poly([(2, 6), (5, 3), (11, 3), (14, 5), (14, 7), (10, 8), (8, 8), (7, 13), (4, 13), (5, 8), (3, 8)], ALIEN)
    c.hline(7, 12, 5, VIOLET_G[BS])
    c.pix(6, 5, VIOLET_G[HL]); c.pix(13, 6, VIOLET_G[HL])
    c.pix(5, 10, VIOLET_G[SH]); c.pix(6, 11, VIOLET_G[SH])
    c.pix(4, 5, ALIEN[SH])


@icon("grim_reaper")
def _(c, x):
    for yy in (2, 4, 6, 8):
        c.rect(2, yy, 13, yy + 1, x.m)
        c.pix(13, yy, RED[HL]); c.pix(13, yy + 1, RED[BS])
        c.pix(2, yy, x.m[DK])
    c.vline(5, 2, 9, x.m[DK]); c.vline(10, 2, 9, x.m[DK])
    grip(c, 6, 10, 13, POLY, w=3, lean=1)
    c.rect(3, 10, 4, 11, POLY)


@icon("nuclear_death_ray")
def _(c, x):
    body = ramp((70, 70, 78))
    c.rect(1, 3, 11, 10, body)
    c.rect(11, 4, 12, 9, ramp((50, 50, 58)))
    c.disc(6, 6.5, 2, GOLD)
    c.disc(6, 6.5, 1, glow((255, 235, 120)), bevel=False)
    c.pix(5, 5, (255, 255, 220))
    c.rect(13, 5, 14, 8, glow((255, 210, 70)), bevel=False)
    for yy in (4, 9):
        c.hline(2, 4, yy, body[LT])
    trefoil(c, 9, 9)
    c.rect(2, 11, 4, 13, body[DK] and POLY)
    grip(c, 7, 11, 13, POLY, w=2, lean=0)


@icon("nuke_launcher")
def _(c, x):
    tube = OLIVE
    c.rect(2, 3, 12, 8, tube)
    c.rect(0, 2, 2, 9, tube)
    c.rect(12, 2, 14, 9, tube)
    c.rect(13, 4, 14, 7, BLACK[DK], bevel=False)
    c.pix(14, 5, x.m[BS]); c.pix(14, 6, x.m[BS])
    c.vline(5, 3, 8, tube[DK]); c.vline(10, 3, 8, tube[DK])
    trefoil(c, 7, 5)
    c.rect(7, 0, 8, 1, BLACK)
    grip(c, 5, 9, 13, POLY, w=2, lean=1)
    c.rect(9, 9, 9, 11, POLY)


# akimbo pairs ----------------------------------------------------------
@icon("akimbo_pistols")
def _(c, x):
    c.paste(akimbo(pistol_small, x))


@icon("akimbo_revolvers")
def _(c, x):
    c.paste(akimbo(revolver_small, x))


@icon("akimbo_mac10s")
def _(c, x):
    c.paste(akimbo(mac_small, x))


@icon("akimbo_deatomizer_pistols")
def _(c, x):
    c.paste(akimbo(deatom_small, x))


@icon("akimbo_golden_revolvers")
def _(c, x):
    c.paste(akimbo(lambda t, xx: revolver_small(t, xx, GOLD, ramp((60, 40, 30))), x))


@icon("frag_grenade")
def _(c, x):
    c.ellipse(7.5, 8.5, 4, 5, OLIVE)
    c.hline(4, 11, 7, OLIVE[DK]); c.hline(4, 11, 10, OLIVE[DK])
    c.rect(6, 2, 9, 3, x.m)
    c.rect(10, 3, 11, 3, x.m[HL], bevel=False)                      # spoon
    c.pix(11, 4, x.m[BS]); c.pix(11, 5, x.m[SH])
    for p in ((5, 1), (4, 1), (4, 2), (4, 3)):
        c.pix(p[0], p[1], x.m[HL])                                  # pin ring


@icon("flashbang")
def _(c, x):
    c.rect(5, 3, 10, 13, x.m)
    c.rect(5, 6, 10, 8, WHITE)
    c.hline(5, 10, 7, WHITE[LT])
    c.rect(6, 1, 9, 2, x.m[LT], bevel=False)
    c.rect(7, 1, 8, 1, x.m[DK], bevel=False)
    c.pix(10, 2, x.m[HL]); c.pix(11, 2, x.m[SH]); c.pix(11, 3, x.m[SH])
    c.hline(5, 10, 12, x.m[DK])


# ======================================================== parts
@icon("part_barrel_stone")
def _(c, x):
    c.rect(1, 5, 13, 9, STONE)
    pts = [(a, b) for a in range(1, 14) for b in range(5, 10)]
    speckle(c, x, pts, STONE, 8, DK); speckle(c, x, pts, STONE, 5, HL)
    c.rect(12, 5, 13, 9, x.m)
    c.rect(13, 6, 13, 8, BLACK[DK], bevel=False)
    c.vline(11, 5, 9, STONE[DK])


@icon("part_barrel_iron")
def _(c, x):
    c.rect(1, 5, 13, 9, IRON)
    for xx in (4, 9):
        c.rect(xx, 4, xx + 1, 10, x.m2)
    c.rect(13, 6, 13, 8, BLACK[DK], bevel=False)
    c.hline(1, 3, 6, IRON[HL])


@icon("part_barrel_hardened")
def _(c, x):
    dark = ramp(tint(x.pal[0], 0.62))
    c.rect(1, 6, 13, 8, dark)
    c.rect(4, 4, 10, 10, x.m)
    c.hline(5, 9, 7, x.m[DK])
    for p in ((5, 5), (9, 5), (5, 9), (9, 9)):
        c.pix(p[0], p[1], x.m[HL])
    c.rect(13, 7, 13, 7, BLACK[DK])


@icon("part_barrel_carbon")
def _(c, x):
    c.rect(1, 5, 13, 9, CARBON)
    weave(c, [(a, b) for a in range(1, 14) for b in range(5, 10)], CARBON[HL])
    c.rect(1, 4, 2, 10, x.m2)
    c.rect(12, 4, 13, 10, x.m2)
    c.pix(13, 7, BLACK[DK])


@icon("part_barrel_laser")
def _(c, x):
    c.rect(1, 6, 10, 8, x.m)
    for xx in (3, 6):
        c.rect(xx, 5, xx, 9, ramp(tuple(x.pal[1])) if len(x.pal) > 1 else GOLD, bevel=False)
    c.rect(11, 4, 13, 10, GLASS)
    c.rect(12, 5, 13, 9, CYAN, bevel=False)
    c.pix(12, 6, (230, 255, 255)); c.pix(11, 5, (230, 255, 255))
    c.vline(10, 4, 10, x.m[DK])


@icon("part_stock_wooden")
def _(c, x):
    c.poly([(1, 4), (4, 3), (14, 5), (14, 9), (7, 9), (4, 13), (1, 13)], WOOD)
    for yy in (6, 8, 11):
        c.hline(3, 11, yy, WOOD[SH]) if yy < 10 else c.hline(2, 3, yy, WOOD[SH])
    c.rect(1, 4, 1, 13, RUBBER, bevel=False)
    c.hline(11, 13, 7, WOOD[DK])
    c.pix(10, 5, WOOD[HL])


@icon("part_stock_plastic")
def _(c, x):
    pl = ramp((226, 226, 218))
    c.poly([(1, 3), (5, 3), (14, 5), (14, 8), (8, 8), (5, 13), (1, 13)], pl)
    for p in ((5, 6), (6, 6), (5, 7), (6, 7)):
        c.erase(*p)
    c.rect(1, 3, 2, 13, x.m2 if len(x.pal) > 1 else x.m)
    c.hline(10, 13, 6, pl[DK])
    c.pix(3, 5, pl[HL])


@icon("part_stock_carbon")
def _(c, x):
    c.poly([(1, 3), (4, 3), (14, 5), (14, 9), (9, 9), (7, 13), (1, 13)], CARBON)
    weave(c, [(a, b) for a in range(1, 15) for b in range(3, 14)], CARBON[HL])
    for p in ((5, 6), (6, 6), (5, 7), (6, 7), (7, 7)):
        c.erase(*p)
    c.rect(1, 3, 2, 13, x.m)


@icon("part_pump_mechanism")
def _(c, x):
    c.rect(2, 3, 10, 7, x.m)
    c.stripes([(a, b) for a in range(2, 11) for b in range(3, 8)], x.m[DK], 3, 0, False)
    c.rect(11, 4, 13, 6, RUBBER)
    c.pix(11, 4, RUBBER[HL])
    spring(c, 2, 12, 10, x.m2)
    c.vline(1, 9, 12, x.m[SH])


@icon("part_receiver_iron")
def _(c, x):
    c.rect(2, 4, 13, 10, x.m)
    c.rect(7, 5, 11, 6, x.m[DK], bevel=False)
    c.hline(2, 13, 3, x.m[LT])
    for p in ((3, 5), (3, 9), (12, 9)):
        c.pix(p[0], p[1], x.m[DK])
    c.vline(5, 4, 10, x.m[SH])
    c.rect(4, 11, 5, 12, x.m[SH])


@icon("part_receiver_steel")
def _(c, x):
    c.rect(2, 3, 13, 11, x.m)
    c.rect(5, 5, 11, 5, x.m[DK], bevel=False)
    c.rect(5, 7, 9, 9, x.m[LT], bevel=False)
    c.rect(6, 8, 8, 8, x.m[DK], bevel=False)
    c.hline(2, 13, 3, x.m[HL])
    c.pix(3, 4, x.m[DK]); c.pix(12, 10, x.m[DK])
    c.vline(10, 7, 10, x.m[SH])


@icon("part_receiver_hardened")
def _(c, x):
    dk = ramp(tint(x.pal[0], 0.55))
    c.rect(2, 4, 13, 10, dk)
    c.rect(3, 5, 12, 5, x.m, bevel=False)
    c.rect(8, 7, 12, 9, BLACK[DK], bevel=False)
    c.hline(3, 6, 8, dk[LT])
    c.vline(7, 5, 10, dk[DK])
    c.rect(2, 11, 6, 12, dk)


@icon("part_receiver_carbon")
def _(c, x):
    c.rect(2, 3, 13, 10, CARBON)
    weave(c, [(a, b) for a in range(2, 14) for b in range(3, 11)], CARBON[HL])
    c.rect(2, 3, 13, 3, x.m)
    c.rect(8, 6, 11, 7, BLACK[DK], bevel=False)
    c.rect(3, 11, 7, 12, CARBON)
    c.hline(3, 7, 12, x.m2)


@icon("part_mechanical_1")
def _(c, x):                       # one big bronze gear, one small gear, short spring
    gear(c, 6, 7, 4, x.m, 8)
    gear(c, 12, 11, 2, x.m, 6)
    spring(c, 2, 6, 13, x.m)


@icon("part_mechanical_2")
def _(c, x):                       # steel: twin gears, spring below, two screws
    gear(c, 5, 5, 3, x.m, 8)
    gear(c, 11, 8, 3, x.m, 8)
    spring(c, 3, 8, 12, x.m)
    c.pix(12, 13, x.m[HL])
    c.pix(14, 12, x.m[SH])


@icon("part_mechanical_3")
def _(c, x):                       # titanium: large gear + cog, vertical spring, screw
    gear(c, 9, 6, 4, x.m, 10)
    gear(c, 3, 11, 2, x.m, 6)
    spring(c, 7, 12, 13, x.m)
    c.pix(3, 3, x.m[HL])
    c.pix(3, 4, x.m[DK])


@icon("part_laser_focus")
def _(c, x):
    c.rect(2, 10, 13, 13, x.m)
    c.rect(3, 9, 12, 9, x.m[LT], bevel=False)
    c.poly([(8, 1), (12, 5), (8, 10), (4, 5)], ramp((200, 255, 255)))
    c.line(8, 1, 8, 9, (230, 255, 255))
    c.line(4, 5, 12, 5, (110, 230, 240))
    c.pix(6, 3, (255, 255, 255))
    c.pix(3, 11, x.m[DK]); c.pix(12, 12, x.m[DK])


@icon("part_uv_emitter")
def _(c, x):
    c.rect(1, 3, 14, 12, x.m)
    for p in ((2, 4), (13, 4), (2, 11), (13, 11)):
        c.pix(p[0], p[1], x.m[HL])
    c.disc(7.5, 7.5, 3, VIOLET_G)
    c.disc(7.5, 7.5, 1, glow((230, 190, 255)), bevel=False)
    c.pix(6, 6, (255, 255, 255))


@icon("part_sonic_emitter")
def _(c, x):
    c.disc(7.5, 7.5, 6, x.m)
    c.disc(7.5, 7.5, 4, ramp(tint(x.pal[0], 0.5)), bevel=False)
    c.disc(7.5, 7.5, 3, x.m)
    c.disc(7.5, 7.5, 1, TEAL, bevel=False)
    c.pix(7, 7, (230, 255, 250))
    c.pix(3, 3, x.m[HL]); c.pix(12, 12, x.m[DK])


@icon("part_rad_emitter")
def _(c, x):
    c.rect(2, 3, 13, 12, LEAD)
    c.rect(5, 5, 10, 10, BLACK[DK], bevel=False)
    c.disc(7.5, 7.5, 2, GREEN_G)
    c.pix(7, 7, (230, 255, 230))
    for p in ((3, 4), (12, 4), (3, 11), (12, 11)):
        c.pix(p[0], p[1], LEAD[HL])


@icon("part_anti_gravity_core")
def _(c, x):
    c.disc(7.5, 7.5, 3, CYAN)
    c.pix(6, 6, (230, 255, 255)); c.pix(7, 6, (190, 250, 255))
    for xx in (2, 13):
        c.vline(xx, 2, 13, x.m[BS])
    for yy in (2, 13):
        c.hline(2, 13, yy, x.m[BS])
    c.pix(2, 2, x.m[HL]); c.pix(13, 13, x.m[DK])
    c.vline(5, 3, 12, x.m[SH]); c.vline(10, 3, 12, x.m[SH])


@icon("part_cybernetic")
def _(c, x):
    wg = ramp((170, 166, 160))
    c.rect(2, 3, 8, 7, wg)
    c.rect(7, 8, 13, 12, wg)
    c.pix(3, 4, wg[DK]); c.pix(7, 11, wg[DK])
    pts = [(9, 3), (10, 4), (11, 3), (12, 4), (13, 3)]
    for p in pts:
        c.pix(p[0], p[1], GOLD[BS])
    pts = [(3, 9), (4, 10), (3, 11), (4, 12), (5, 13), (6, 12)]
    for p in pts:
        c.pix(p[0], p[1], VIOLET_G[BS])
    c.pix(5, 5, CYAN[HL])


@icon("part_heavy_cloth")
def _(c, x):
    cl = ramp((128, 88, 52))
    c.rect(2, 3, 13, 12, cl)
    c.hline(2, 13, 6, cl[SH]); c.hline(2, 13, 9, cl[SH])
    c.hline(2, 13, 7, cl[HL]); c.hline(2, 13, 10, cl[LT])
    c.rect(11, 3, 13, 12, cl[SH], bevel=False)
    c.vline(11, 3, 12, cl[HL])
    c.dither([(a, b) for a in range(2, 11) for b in (4, 5)], cl[BS], cl[LT])


@icon("part_treated_leather")
def _(c, x):
    lt = ramp((52, 66, 54))
    c.poly([(2, 4), (5, 2), (11, 3), (14, 6), (13, 12), (8, 14), (3, 12)], lt)
    speckle(c, x, [(a, b) for a in range(2, 14) for b in range(3, 14)], lt, 10, DK)
    speckle(c, x, [(a, b) for a in range(2, 14) for b in range(3, 14)], lt, 6, LT)
    for yy in (5, 8, 11):
        c.pix(4, yy, lt[HL]); c.pix(11, yy, lt[HL]) if yy < 11 else None


@icon("part_steam_armor_plating")
def _(c, x):
    c.rect(1, 3, 14, 12, x.m)
    c.rect(1, 3, 7, 12, x.m2 if len(x.pal) > 1 else x.m)
    c.vline(8, 3, 12, x.m[DK])
    for yy in (4, 11):
        for xx in (2, 5, 10, 13):
            c.pix(xx, yy, ramp(tint(x.pal[0], 1))[HL])
    c.hline(2, 13, 7, x.m[SH]); c.hline(2, 13, 8, x.m[LT])


@icon("part_power_plating")
def _(c, x):
    c.rect(1, 2, 14, 13, x.m)
    c.rect(2, 4, 13, 11, CARBON)
    weave(c, [(a, b) for a in range(2, 14) for b in range(4, 12)], CARBON[HL])
    c.rect(4, 6, 11, 9, x.m)
    for p in ((2, 3), (13, 3), (2, 12), (13, 12)):
        c.pix(p[0], p[1], x.m[HL])


@icon("part_gas_mask_filter")
def _(c, x):
    c.disc(7.5, 7.5, 6, x.m)
    c.disc(7.5, 7.5, 4, ramp(tint(x.pal[0], 0.5)), bevel=False)
    for yy in (5, 7, 9):
        c.hline(5, 10, yy, x.m[LT])
    c.rect(6, 1, 9, 2, BLACK)


@icon("part_nuclear_warhead")
def _(c, x):
    c.poly([(8, 1), (11, 4), (12, 13), (4, 13), (5, 4)], OLIVE)
    c.rect(4, 8, 12, 10, ramp((240, 200, 40)))
    c.stripes([(a, b) for a in range(4, 13) for b in range(8, 11)], (30, 30, 30), 3, 0, True)
    c.rect(6, 13, 10, 14, BLACK)
    c.pix(7, 4, OLIVE[HL])


@icon("part_glider_backpack")
def _(c, x):
    c.rect(3, 3, 12, 13, CANVAS)
    c.hline(3, 12, 6, CANVAS[SH])
    c.vline(3, 2, 13, x.m); c.vline(12, 2, 13, x.m)
    c.hline(3, 12, 2, x.m)
    c.rect(6, 8, 9, 11, CANVAS[SH], bevel=False)
    c.hline(6, 9, 8, CANVAS[LT])
    c.rect(6, 1, 9, 1, x.m)
    c.pix(7, 10, BRASS[BS]); c.pix(8, 10, BRASS[BS])


@icon("part_glider_wing")
def _(c, x):
    c.poly([(1, 12), (3, 4), (13, 2), (14, 5), (14, 12)], CANVAS)
    c.line(2, 12, 14, 3, x.m[BS])
    c.line(5, 12, 13, 6, x.m[SH])
    c.line(9, 12, 14, 8, x.m[SH])
    c.hline(1, 14, 13, x.m[DK])
    c.pix(3, 5, CANVAS[HL])


# ======================================================== ammo
@icon("ammo_stone_bullet")
def _(c, x):
    for (cx, cy, r) in ((5, 9, 2), (10.5, 9.5, 2), (7.5, 4.5, 2)):
        c.disc(cx, cy, r, STONE)
    pts = [(a, b) for a in range(2, 14) for b in range(2, 13)]
    speckle(c, x, pts, STONE, 8, DK)


@icon("ammo_9mm_bullet")
def _(c, x):
    for (xx, y0) in ((3, 5), (7, 4), (11, 5)):
        bullet(c, xx, y0, 13, BRASS, ramp((150, 150, 156)), tip=3, w=2)


@icon("golden_round")
def _(c, x):
    # One gold cartridge: bright pointed tip, darker gold case, base rim.
    case = ramp((190, 138, 24))
    bullet(c, 6, 2, 12, case, ramp((255, 236, 150)), tip=4, w=3)
    c.rect(5, 13, 9, 14, case)          # wider rim at the base
    c.hline(5, 9, 13, case[DK])         # extractor groove
    c.hline(6, 8, 2, ramp((255, 236, 150))[HL])


@icon("ammo_shotgun_shell")
def _(c, x):
    shell(c, 2, 4, 13, RED, BRASS, w=4, base_h=3)
    shell(c, 8, 2, 11, RED, BRASS, w=4, base_h=3)
    c.hline(9, 11, 7, RED[DK])
    c.hline(3, 5, 9, RED[DK])


@icon("ammo_rifle_bullet")
def _(c, x):
    for xx, y0 in ((4, 1), (9, 2)):
        bullet(c, xx, y0, 14, BRASS, ramp((120, 120, 128)), tip=4, w=2)
        c.hline(xx, xx + 1, 5 + y0, BRASS[DK])


@icon("ammo_high_caliber_round")
def _(c, x):
    for xx, y0 in ((2, 1), (8, 2)):
        bullet(c, xx, y0, 14, BRASS, x.m2, tip=5, w=3)
        c.hline(xx, xx + 2, y0 + 7, BRASS[DK])
        c.hline(xx, xx + 2, 14, BRASS[DK])


@icon("ammo_advanced_bullet")
def _(c, x):
    for xx, y0 in ((2, 2), (5, 1), (8, 3), (11, 2)):
        bullet(c, xx, y0, 14, ramp((90, 90, 100)), VIOLET_G, tip=4, w=2)
    # slim: halve visual width by darkening right column
    for xx in (2, 5, 8, 11):
        for yy in range(1, 15):
            if (xx + 1, yy) in c.px:
                c.pix(xx + 1, yy, tint(c.px[(xx + 1, yy)], 0.75))


@icon("ammo_40mm_grenade")
def _(c, x):
    c.rect(4, 7, 11, 13, BRASS)
    c.rect(4, 9, 11, 10, RED)
    c.poly([(6, 3), (9, 3), (11, 7), (4, 7)], ramp((120, 120, 128)))
    c.hline(4, 11, 13, BRASS[DK])
    c.pix(7, 4, (200, 200, 210))


@icon("ammo_rocket")
def _(c, x):
    c.poly([(8, 1), (10, 4), (10, 4), (6, 4)], RED)
    c.rect(6, 4, 9, 11, x.m)
    c.hline(6, 9, 6, x.m[DK])
    c.poly([(6, 8), (3, 12), (3, 14), (6, 12)], RED)
    c.poly([(9, 8), (12, 12), (12, 14), (9, 12)], RED)
    c.rect(6, 12, 9, 13, BLACK)


@icon("ammo_rocket_nuke")
def _(c, x):
    c.poly([(8, 0), (11, 3), (5, 3)], OLIVE)
    c.rect(5, 3, 10, 11, OLIVE)
    trefoil(c, 7, 7)
    c.poly([(5, 8), (2, 12), (2, 14), (5, 12)], BLACK)
    c.poly([(10, 8), (13, 12), (13, 14), (10, 12)], BLACK)
    c.rect(5, 12, 10, 13, BLACK)


@icon("ammo_nether_charge")
def _(c, x):
    tan = ramp((200, 170, 120))
    c.rect(3, 5, 12, 12, tan)
    c.rect(5, 7, 10, 10, ORANGE_G, bevel=False)
    c.vline(5, 7, 10, ORANGE_G[HL]); c.hline(5, 10, 7, ORANGE_G[LT]); c.vline(10, 7, 10, ORANGE_G[SH])
    c.vline(4, 5, 12, tan[DK]); c.vline(11, 5, 12, tan[DK])
    c.pix(9, 1, ORANGE_G[HL])


# ======================================================== magazines
def _mag(c, x, kind, loaded):
    M = x.m
    if kind == "9mm":
        bx, by0, by1, w, curve = 6, 4, 13, 4, 0
    elif kind == "smg":
        bx, by0, by1, w, curve = 6, 1, 14, 4, 0
    elif kind == "ar":
        bx, by0, by1, w, curve = 4, 2, 14, 4, 4
    elif kind == "as50":
        bx, by0, by1, w, curve = 4, 3, 13, 7, 0
    elif kind == "lmg":
        bx, by0, by1, w, curve = 3, 3, 14, 9, 0
    elif kind == "adv":
        bx, by0, by1, w, curve = 5, 1, 14, 5, 2
    if kind == "drum":
        c.disc(7.5, 8.5, 5, M)
        c.disc(7.5, 8.5, 2, ramp(tint(x.pal[0], 0.5)), bevel=False)
        for yy, xx in ((3, 7), (13, 7), (8, 2), (8, 12)):
            pass
        c.pix(7, 8, M[HL]); c.pix(8, 8, M[SH])
        c.rect(6, 1, 9, 2, M)
        for p in ((4, 5), (11, 5), (4, 12), (11, 12)):
            c.pix(p[0], p[1], M[DK])
        if loaded:
            for xx in (6, 8):
                c.pix(xx, 0, BRASS[HL]); c.pix(xx + 1, 0, BRASS[BS])
            c.hline(6, 9, 1, BRASS[BS])
            c.rect(6, 2, 9, 2, BRASS[DK], bevel=False)
            for p in ((5, 6), (10, 6), (5, 11), (10, 11), (7, 4), (8, 12)):
                c.pix(p[0], p[1], BRASS[BS])
        else:
            c.rect(6, 1, 9, 2, BLACK[DK], bevel=False)
            c.rect(6, 0, 9, 0, M[LT], bevel=False)
        return
    top = by0
    body_y0 = by0 + 2
    pts = []
    for y in range(body_y0, by1 + 1):
        t = (y - body_y0) / max(1, by1 - body_y0)
        o = int(round(curve * t * t))
        pts += [(bx + o + i, y) for i in range(w)]
    c.fill(pts, M)
    # base plate
    for i in range(w):
        c.pix(bx + int(round(curve)) + i, by1, M[DK])
    # window / ribs
    for y in range(body_y0 + 2, by1 - 1, 3):
        t = (y - body_y0) / max(1, by1 - body_y0)
        o = int(round(curve * t * t))
        c.hline(bx + o + 1, bx + o + w - 2, y, M[SH])
    if kind == "adv":
        for y in range(body_y0 + 1, by1 - 1):
            t = (y - body_y0) / max(1, by1 - body_y0)
            o = int(round(curve * t * t))
            c.pix(bx + o + 2, y, VIOLET_G[BS])
    if kind == "lmg":
        c.rect(bx + 1, body_y0 + 1, bx + w - 2, body_y0 + 2, M[LT], bevel=False)
    # top: rounds or empty slot
    if loaded:
        for i in range(0, w - 1, 2) if w <= 5 else range(0, w - 1, 2):
            c.pix(bx + i, top + 1, BRASS[BS]); c.pix(bx + i + 1, top + 1, BRASS[SH])
            c.pix(bx + i, top, BRASS[HL]); c.pix(bx + i + 1, top, BRASS[LT])
        c.hline(bx, bx + w - 1, top + 2, M[LT])
        if w % 2 == 1:
            c.pix(bx + w - 1, top + 1, BRASS[SH]); c.pix(bx + w - 1, top, BRASS[BS])
    else:
        c.hline(bx, bx + w - 1, top + 2, M[HL])
        c.rect(bx + 1, top + 1, bx + w - 2, top + 1, BLACK[DK], bevel=False)
        c.hline(bx, bx + w - 1, top + 2, M[LT])
        c.pix(bx + 1, top + 2, BLACK[DK]); c.pix(bx + w - 2, top + 2, BLACK[DK])


for _name, _kind in (("mag_9mm_pistol", "9mm"), ("mag_smg", "smg"), ("mag_assault_rifle", "ar"),
                     ("mag_as50", "as50"), ("mag_lmg", "lmg"), ("mag_advanced", "adv"),
                     ("mag_minigun_drum", "drum")):
    REG[_name] = (lambda k: (lambda c, x: _mag(c, x, k, True)))(_kind)
    REG[_name + "_empty"] = (lambda k: (lambda c, x: _mag(c, x, k, False)))(_kind)


# ======================================================== tanks and cells
def _tank_canister(c, x, body, fill, full, cap=None):
    canister(c, 3, 1, 12, 14, body, fill if full else None, 0.8, cap_rp=cap or x.m)
    c.rect(6, 0, 9, 0, x.m[LT], bevel=False)


@icon("tank_fuel")
def _(c, x):
    _tank_canister(c, x, FUEL, glow((222, 130, 40)), True)


@icon("tank_fuel_empty")
def _(c, x):
    _tank_canister(c, x, FUEL, None, False)


@icon("tank_bio")
def _(c, x):
    canister(c, 3, 1, 12, 14, x.m, GREEN_G, 0.8, cap_rp=x.m)
    c.rect(6, 0, 9, 0, x.m[LT], bevel=False)
    c.pix(5, 8, GREEN_G[HL]); c.pix(8, 10, GREEN_G[HL])


@icon("tank_bio_empty")
def _(c, x):
    canister(c, 3, 1, 12, 14, x.m, None, 0, cap_rp=x.m)
    c.rect(6, 0, 9, 0, x.m[LT], bevel=False)


def _air(c, x, full):
    blue = ramp((150, 200, 230))
    c.rect(6, 0, 9, 1, x.m)                                        # valve
    c.rect(5, 2, 10, 3, x.m)
    c.poly([(5, 3), (10, 3), (12, 5), (12, 14), (3, 14), (3, 5)], blue if True else x.m)
    c.rect(4, 5, 11, 13, GLASS, bevel=False)
    if full:
        c.rect(4, 6, 11, 13, ramp((190, 230, 250)), bevel=False)
        c.vline(4, 6, 13, (230, 248, 255)); c.hline(4, 11, 6, (240, 252, 255))
        c.vline(11, 7, 13, (140, 190, 225))
    else:
        c.rect(4, 5, 11, 13, (112, 120, 128), bevel=False)
        c.vline(4, 5, 13, (150, 158, 166)); c.pix(5, 5, (180, 188, 196)); c.pix(5, 6, (166, 174, 182))
    c.hline(3, 12, 14, blue[DK])
    c.hline(5, 10, 3, x.m[DK])


@icon("tank_air")
def _(c, x):
    _air(c, x, True)


@icon("tank_air_empty")
def _(c, x):
    _air(c, x, False)


@icon("cell_nuclear")
def _(c, x):
    c.rect(4, 1, 11, 14, LEAD)
    c.rect(5, 4, 10, 11, GREEN_G, bevel=False)
    c.vline(5, 4, 11, GREEN_G[HL]); c.vline(6, 4, 11, GREEN_G[LT]); c.vline(10, 4, 11, GREEN_G[SH])
    c.pix(8, 6, GREEN_G[HL]); c.pix(8, 9, GREEN_G[HL])
    c.hline(4, 11, 3, LEAD[DK]); c.hline(4, 11, 12, LEAD[DK])
    c.pix(5, 2, LEAD[HL]); c.pix(10, 13, LEAD[DK])
    c.pix(8, 2, (250, 210, 30)); c.pix(7, 13, (250, 210, 30))


@icon("cell_nuclear_depleted")
def _(c, x):
    dull = ramp((104, 94, 82))
    c.rect(4, 1, 11, 14, LEAD)
    c.rect(5, 4, 10, 11, dull, bevel=False)
    c.line(6, 5, 8, 8, dull[DK]); c.line(8, 8, 7, 10, dull[DK])
    c.hline(4, 11, 3, LEAD[DK]); c.hline(4, 11, 12, LEAD[DK])
    c.pix(5, 2, LEAD[HL]); c.pix(10, 13, LEAD[DK])


# ======================================================== misc items
@icon("bandages")
def _(c, x):
    c.ellipse(7.5, 8.5, 5, 4, WHITE)
    c.ellipse(7.5, 8.5, 2, 1, WHITE[SH] and ramp((205, 205, 212)))
    c.pix(7, 8, WHITE[DK]); c.pix(8, 8, WHITE[SH])
    c.hline(5, 10, 10, WHITE[SH])
    c.poly([(11, 8), (14, 10), (14, 13), (11, 11)], WHITE)
    c.rect(6, 4, 9, 4, RED[BS], bevel=False)
    c.vline(7, 3, 5, RED[BS]); c.vline(8, 3, 5, RED[SH])


@icon("medkit")
def _(c, x):
    c.rect(2, 4, 13, 13, ramp((240, 240, 244)))
    c.rect(5, 1, 10, 3, ramp((200, 200, 208)))
    c.rect(6, 2, 9, 2, ramp((140, 140, 150))[SH], bevel=False)
    cross(c, 7, 8, RED[BS], 2)
    c.hline(2, 13, 6, (200, 200, 208)); c.pix(3, 6, (160, 160, 168)); c.pix(12, 6, (160, 160, 168))
    c.vline(8, 6, 10, RED[BS])
    c.hline(5, 9, 8, RED[BS])


def _camo(c, x, base, blobs, seed):
    rng = random.Random(seed)
    cl = ramp(base)
    c.rect(4, 3, 13, 12, cl)
    c.ellipse(4, 7.5, 2, 4, cl)
    # blotches
    for col in blobs:
        for _ in range(4):
            bx, by = rng.randint(3, 12), rng.randint(3, 11)
            for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)) if rng.random() < 0.5 else ((0, 0), (1, 0), (2, 0)):
                if (bx + dx, by + dy) in c.px:
                    c.pix(bx + dx, by + dy, col)
    # net mesh: darker crosshatch dots
    for (px_, py_) in list(c.px):
        if px_ >= 5 and (px_ + 2 * py_) % 5 == 0:
            c.pix(px_, py_, tint(c.px[(px_, py_)], 0.8))
    c.vline(9, 3, 12, tint(cl[DK], 1.0))                                 # strap
    c.vline(10, 3, 12, tint(cl[SH], 1.0))
    c.pix(9, 7, BRASS[BS]); c.pix(10, 7, BRASS[SH])
    c.pix(4, 7, cl[DK]); c.pix(4, 8, cl[DK])
    c.ellipse(4, 7.5, 1, 2, cl[DK] and ramp(tint(base, 0.6)), bevel=False)


@icon("camo_net_placer_0")
def _(c, x):
    _camo(c, x, x.pal[0], [(84, 112, 56), (44, 62, 30), x.pal[1]], 1)


@icon("camo_net_placer_1")
def _(c, x):
    _camo(c, x, x.pal[0], [(255, 255, 255), x.pal[1], (200, 210, 222)], 2)


@icon("camo_net_placer_2")
def _(c, x):
    _camo(c, x, x.pal[0], [x.pal[1], (210, 188, 140), (142, 108, 66)], 3)


@icon("bunker_door")
def _(c, x):
    c.rect(2, 1, 13, 14, x.m)
    c.rect(4, 3, 11, 12, ramp(tint(x.pal[0], 0.85)))
    for p in ((3, 2), (12, 2), (3, 13), (12, 13), (3, 7), (12, 7)):
        c.pix(p[0], p[1], x.m[HL])
    c.rect(9, 7, 10, 8, BLACK)
    c.rect(2, 4, 2, 5, BLACK[BS], bevel=False)
    c.rect(2, 10, 2, 11, BLACK[BS], bevel=False)
    c.hline(5, 7, 5, x.m[LT])


@icon("ammo_pouch")
def _(c, x):
    for xx in (4, 7, 10):
        bullet(c, xx, 1, 6, BRASS, ramp((150, 150, 156)), tip=2, w=2)
    c.poly([(3, 6), (12, 6), (13, 12), (11, 14), (4, 14), (2, 12)], LEATHER)
    c.rect(3, 6, 12, 8, ramp((98, 62, 32)))
    c.hline(3, 12, 8, LEATHER[DK])
    c.pix(5, 9, BRASS[HL]); c.pix(10, 9, BRASS[HL])
    c.hline(5, 10, 11, LEATHER[SH])


@icon("spawn_egg")
def _(c, x):
    egg = ramp((150, 150, 150))
    pts = c.ellipse_coords(7.5, 8.5, 5, 6)
    pts = [p for p in pts if p[1] > 2 or 6 <= p[0] <= 9]
    c.fill(pts, egg)
    rng = x.rng
    for _ in range(14):
        p = rng.choice(sorted(pts))
        c.pix(p[0], p[1], egg[rng.choice((SH, LT, HL, DK))])
    # grayscale: no outline pass colorization issue (outline also gray)


@icon("spawn_egg_overlay")
def _(c, x):
    G = ["XXX", "X..", "X.X", "X.X", "XXX"]
    for ox in (4, 9):
        for j, row in enumerate(G):
            for i, ch in enumerate(row):
                if ch == "X":
                    c.pix(ox + i - 1 + 1, 5 + j, (255, 255, 255))


def _turret(c, x, weave_c=False):
    c.poly([(1, 4), (4, 2), (11, 2), (14, 4), (14, 9), (11, 12), (4, 12), (1, 9)], x.m)
    c.poly([(2, 5), (4, 3), (11, 3), (13, 5), (13, 8), (11, 10), (4, 10), (2, 8)], ramp(tint(x.pal[0], 0.85)), bevel=True)
    for p in ((3, 4), (12, 4), (3, 9), (12, 9), (7, 3)):
        c.pix(p[0], p[1], BLACK[DK])
    c.hline(4, 11, 6, x.m[LT])
    c.hline(5, 11, 13, x.m[DK])


@icon("turret_armor_iron")
def _(c, x):
    _turret(c, x)


@icon("turret_armor_steel")
def _(c, x):
    _turret(c, x)
    c.vline(7, 4, 9, x.m[DK]); c.vline(8, 4, 9, x.m[LT])


@icon("turret_armor_aluminum")
def _(c, x):
    _turret(c, x)


@icon("turret_armor_carbon")
def _(c, x):
    _turret(c, x)
    weave(c, [(a, b) for a in range(2, 14) for b in range(3, 11)], x.m[HL])


# ======================================================== armor
# Armor icons take their colors from the worn textures' own sources, so a repaint of a worn sheet flows through to
# the inventory icon on the next run: the region colors in armor_repaint.SETS (biped sets) and the palettes in
# armor_models / armor_exo_paint (custom-model sets). Silhouettes are the per-kind drawers in pixel.py.
import armor_exo_paint as _EXO  # noqa: E402
import armor_models as _AM  # noqa: E402
import armor_repaint as _AR  # noqa: E402

LIFT_FLOOR = 68  # luma floor so near-black worn colors stay readable on a dark inventory slot


def _luma(rgb):
    return 0.299 * rgb[0] + 0.587 * rgb[1] + 0.114 * rgb[2]


def lift(h, floor=LIFT_FLOOR):
    """Hex or rgb -> rgb, scaled up (hue kept) until its luma reaches ``floor``."""
    rgb = hexrgb(h) if isinstance(h, str) else tuple(h)
    lu = _luma(rgb)
    if lu >= floor:
        return tuple(rgb)
    if lu < 1:
        return (floor, floor, floor)
    k = floor / lu
    return tuple(min(255, int(round(ch * k))) for ch in rgb)


def _rp(h, floor=LIFT_FLOOR):
    return ramp(lift(h, floor))


def _spec_hex(spec):
    h = spec[1]
    return h[0] if isinstance(h, list) else h


# slot -> worn region names, in priority order, for each icon color role
_ROLES = {
    "helmet": dict(main=["helmet"], trim=["headband", "strap", "glasses", "lamp", "visor"],
                   acc=["headband", "strap", "lamp", "visor"]),
    "chestplate": dict(main=["torso"], trim=["chestband", "vest", "shoulder"], acc=["vest", "belt", "band", "emblem"]),
    "leggings": dict(main=["pants"], trim=["thigh", "knee", "pouch"], acc=["belt", "knee", "pouch"]),
    "boots": dict(main=["boot"], trim=["knee", "trim", "glove"], acc=["glove", "knee"]),
}
_VIS_REGIONS = ["visor", "glasses", "lamp"]
# the few slots whose worn look is not described by the region table above
_SLOT_FIX = {
    ("t3_hev", "boots"): dict(trim="#E08A12"),
    ("t3_hev", "leggings"): dict(main="#2A2C30", trim="#E08A12"),  # worn: charcoal with two orange bands
    ("t1_miner", "chestplate"): dict(trim="#E6C81E"),
}


def _pick(cfg, names):
    for n in names:
        if n in cfg:
            return _spec_hex(cfg[n])
    return None


def biped_colorway(set_id, kind, **extra):
    cfg = _AR.SETS[set_id]
    cw = dict(kind=kind, slots={})
    for slot, roles in _ROLES.items():
        main = _pick(cfg, roles["main"])
        trim = _pick(cfg, roles["trim"]) or main
        acc = _pick(cfg, roles["acc"]) or trim
        fix = _SLOT_FIX.get((set_id, slot), {})
        trim = fix.get("trim", trim)
        main = fix.get("main", main)
        if slot == "boots" and set_id != "t3_hev" and "pants" in cfg:
            # the worn boots show the set's leg color above the boot band: main is the leg color, the boot band trims
            trim, main = main, _pick(cfg, ["pants"])
        d = dict(main=_rp(main), trim=_rp(trim), acc=_rp(acc))
        if slot == "helmet":
            v = _pick(cfg, _VIS_REGIONS)
            if v:
                d["vis"] = ramp(hexrgb(v))  # glass keeps its true tone (black stays black)
        cw["slots"][slot] = d
    cw.update(main=cw["slots"]["chestplate"]["main"], trim=cw["slots"]["chestplate"]["trim"],
              acc=cw["slots"]["chestplate"]["acc"])
    cw.update(extra)
    return cw


def _x(key, name):
    return _EXO.PALETTES[key][name]


_EX = "t3_exo"
_PW = _AM.PAL["t3_power"]
_RT = _AM.PAL["t2_riot"]
_SP = _AM.STEAM_PAL["steam"]
COLORWAYS = {
    "t1_scout": biped_colorway("t1_scout", "bandit"),
    "t1_miner": biped_colorway("t1_miner", "miner"),
    "t1_combat": biped_colorway("t1_combat", "steel"),
    "t2_combat": biped_colorway("t2_combat", "olive"),
    "t2_commando": biped_colorway("t2_commando", "commando"),
    "t3_combat": biped_colorway("t3_combat", "fiber"),
    "t3_hev": biped_colorway("t3_hev", "hev"),
    "t2_riot": dict(kind="riot", slots={
        "helmet": dict(main=_rp(_RT["helm"]), trim=_rp(_RT["trim"]), acc=_rp(_RT["stud"]), vis=ramp((70, 90, 100))),
        "chestplate": dict(main=_rp(_RT["leather"]), trim=_rp(_RT["helm"]), acc=_rp(_RT["stud"])),
        "leggings": dict(main=_rp(_RT["leather"]), trim=_rp(_RT["helm"]), acc=_rp(_RT["stud"])),
        "boots": dict(main=_rp(_RT["boot"]), trim=_rp(_RT["helm"]), acc=_rp(_RT["stud"]))}),
    "steam": dict(kind="steam", main=_rp(_SP["torso"][0]), trim=_rp(_SP["brass"][0]), acc=_rp(_SP["gray"][0]),
                  vis=ramp(lift(_SP["dark"][1], 30))),
    "t3_power": dict(kind="power", main=_rp(_PW["plate"]), trim=_rp(_PW["panel"]), acc=_rp(_PW["ring"]),
                     vis=ramp((24, 26, 30)), glow=_rp(_PW["ring"])),
    "t3_exo": dict(kind="exo", slots={
        "helmet": dict(main=_rp(_x(_EX, "helmet")), trim=_rp(_x(_EX, "arms")), acc=_rp(_x(_EX, "pauldron")),
                       vis=_rp(_x(_EX, "visor")[1], 90)),
        "chestplate": dict(main=_rp(_x(_EX, "torso")), trim=_rp(_x(_EX, "pauldron")), acc=_rp(_x(_EX, "arms")),
                           vis=_rp(_x(_EX, "mark")[0], 90)),
        "leggings": dict(main=_rp(_x(_EX, "legs")), trim=_rp(_x(_EX, "knee")), acc=_rp(_x(_EX, "legrod")),
                         vis=_rp(_x(_EX, "visor")[1], 90)),
        "boots": dict(main=_rp(_x(_EX, "shin")), trim=_rp(_x(_EX, "ankle")), acc=_rp(_x(_EX, "legrod")),
                      vis=_rp(_x(_EX, "visor")[1], 90))}),
}


def _slot_cw(cw, slot):
    out = {k: v for k, v in cw.items() if k != "slots"}
    out.update(cw.get("slots", {}).get(slot, {}))
    return out


for _set, _cw in COLORWAYS.items():
    for _slot in ("helmet", "chestplate", "leggings", "boots"):
        REG[f"{_set}_{_slot}"] = (lambda s, cw: (lambda c, x: armor_piece(c, s, _slot_cw(cw, s))))(_slot, _cw)


@icon("t2_beret")
def _(c, x):
    cloth = _AM.PAL["t2_beret"]  # worn: deep maroon cloth over a near-black band
    red = _rp(cloth["cloth"], 50)
    c.ellipse(7.5, 8, 6, 3.5, red)
    c.rect(2, 10, 13, 11, _rp(cloth["dark"], 30))
    c.hline(2, 13, 10, red[DK])
    c.pix(7, 4, red[DK]); c.pix(8, 4, red[SH])
    c.rect(5, 7, 6, 8, x.m, bevel=False)                      # badge
    c.pix(5, 7, x.m[HL]); c.pix(6, 8, x.m[DK])


@icon("gas_mask")
def _(c, x):
    rub = _rp("#263124", 52)                                  # worn: dark olive-black rubber hood
    lens = ramp((30, 40, 44))                                 # worn: near-black lenses
    c.poly([(3, 2), (12, 2), (13, 8), (11, 13), (8, 14), (4, 13), (2, 8)], rub)
    c.rect(3, 5, 6, 7, lens)
    c.rect(9, 5, 12, 7, lens)
    c.pix(3, 5, (120, 140, 146)); c.pix(9, 5, (120, 140, 146))
    c.disc(7.5, 11, 2, ramp((84, 84, 88)))                    # filter (worn: gunmetal)
    c.pix(7, 10, x.m[HL]); c.hline(5, 10, 9, rub[DK])
    c.hline(4, 11, 3, rub[LT])


@icon("night_vision_goggles")
def _(c, x):
    c.rect(1, 6, 14, 7, BLACK)                                # strap
    c.rect(2, 4, 6, 10, BLACK)
    c.rect(9, 4, 13, 10, BLACK)
    lens = ramp((47, 123, 21))                                # worn: dark green lenses, no glow
    c.rect(3, 6, 5, 8, lens, bevel=False)
    c.rect(10, 6, 12, 8, lens, bevel=False)
    c.pix(3, 6, (150, 220, 130)); c.pix(10, 6, (150, 220, 130))
    c.rect(7, 5, 8, 6, BLACK[SH])
    c.hline(2, 6, 4, BLACK[HL]); c.hline(9, 13, 4, BLACK[HL])
    c.hline(2, 6, 10, BLACK[DK]); c.hline(9, 13, 10, BLACK[DK])


@icon("oxygen_mask")
def _(c, x):
    gm = ramp((150, 154, 160))
    c.poly([(2, 4), (13, 4), (13, 9), (10, 13), (5, 13), (2, 9)], gm)
    c.rect(5, 8, 10, 11, BLACK, bevel=False)
    c.rect(6, 9, 9, 10, x.m[SH], bevel=False)
    c.rect(12, 9, 14, 11, x.m)                                # hose port
    c.line(13, 12, 14, 14, BLACK[BS])
    c.hline(1, 3, 6, RUBBER[BS]); c.hline(12, 14, 6, RUBBER[BS])


@icon("oxygen_tanks")
def _(c, x):
    tank = ramp((88, 88, 92))                                 # worn: dark steel shells, light end caps
    cap = ramp((151, 151, 151))
    for ox in (2, 8):
        c.rect(ox, 4, ox + 4, 14, tank)
        c.rect(ox + 1, 1, ox + 3, 3, cap, bevel=False)
        c.rect(ox, 3, ox + 4, 4, cap[SH], bevel=False)
        c.hline(ox, ox + 4, 7, tank[DK])
    c.hline(2, 12, 10, BLACK[BS])
    c.hline(2, 12, 11, BLACK[DK])
    c.pix(5, 2, RED[BS]); c.pix(11, 2, RED[BS])


@icon("glider")
def _(c, x):
    c.poly([(1, 5), (14, 5), (13, 10), (2, 10)], CANVAS)
    c.hline(1, 14, 7, CANVAS[SH]); c.hline(2, 13, 8, CANVAS[LT])
    fr = ramp((86, 86, 86))                                   # worn: gray frame
    c.rect(2, 11, 13, 13, fr)
    c.vline(5, 5, 13, ramp((88, 72, 56))[BS]); c.vline(10, 5, 13, ramp((88, 72, 56))[BS])
    c.hline(2, 13, 13, fr[DK])
    c.pix(7, 12, BRASS[BS]); c.pix(8, 12, BRASS[BS])
    c.vline(3, 3, 5, fr[BS]); c.vline(12, 3, 5, fr[BS])


@icon("jump_pack")
def _(c, x):
    steel = ramp((104, 104, 108))                             # worn: gray steel body, dark panel, hazard-marked tops
    dark = _rp("#332a28", 50)
    c.rect(3, 1, 12, 9, steel)
    c.rect(4, 3, 11, 5, dark)
    c.hline(5, 10, 4, dark[HL])
    c.rect(2, 3, 2, 8, BLACK[BS], bevel=False); c.rect(13, 3, 13, 8, BLACK[BS], bevel=False)
    for ox in (3, 9):
        c.poly([(ox, 10), (ox + 4, 10), (ox + 3, 14), (ox + 1, 14)], ramp((88, 88, 92)))
        c.pix(ox + 1, 13, ORANGE_G[BS]); c.pix(ox + 2, 13, ORANGE_G[SH])
    for px_, col in ((3, (230, 200, 20)), (4, BLACK[DK]), (11, (230, 200, 20)), (12, BLACK[DK])):
        c.pix(px_, 1, col)                                    # caution squares on the tank tops
    c.pix(5, 7, RED[BS]); c.pix(10, 7, GREEN_G[BS])


@icon("tactical_mask")
def _(c, x):
    c.poly([(3, 1), (12, 1), (13, 8), (11, 13), (8, 14), (4, 13), (2, 8)], BLACK)
    band = _rp("#6A1A1A", 70)                                # worn default: black mask, dark-red band
    c.rect(4, 4, 11, 6, band, bevel=False)
    c.hline(4, 11, 4, band[HL]); c.pix(4, 5, band[LT])
    c.hline(4, 11, 7, BLACK[DK])
    c.stripes([(a, b) for a in range(5, 11) for b in range(9, 13)], BLACK[HL], 2, 0, False)
    c.pix(3, 2, BLACK[HL])


@icon("anti_gravity_device")
def _(c, x):
    c.rect(2, 2, 13, 13, ramp((130, 134, 142)))
    c.rect(2, 2, 13, 3, ramp((150, 154, 162)))
    c.disc(7.5, 7.5, 3, CYAN)
    c.pix(6, 6, (230, 255, 255)); c.pix(7, 6, (200, 252, 255))
    c.rect(1, 4, 1, 11, BLACK[BS], bevel=False); c.rect(14, 4, 14, 11, BLACK[BS], bevel=False)
    c.hline(3, 12, 12, ramp((90, 94, 102))[SH])
    for p in ((3, 4), (12, 4), (3, 11), (12, 11)):
        c.pix(p[0], p[1], x.m[BS])


# ======================================================== driver
def render(entry):
    ctx = Ctx(entry)
    fn = REG.get(ctx.id)
    if fn is None:
        return None
    c = Canvas()
    fn(c, ctx)
    c.outline()
    return c


def main():
    manifest = json.loads((ROOT / "tools/asset-gen/manifest.json").read_text(encoding="utf-8"))
    # Gun icons are derived from the 3D models by gun_icons.py, not drawn here.
    import gun_icons
    gun_done = gun_icons.gun_icon_names()
    icons = [e for e in manifest if e["kind"] == "icon" and e["id"][5:] not in gun_done]
    order = {"P1": 0, "P2": 1, "P3": 2}
    icons.sort(key=lambda e: order.get(e["priority"], 9))
    rows, made, missing, sheet = [], 0, [], []
    for e in icons:
        c = render(e)
        if c is None:
            missing.append(e["id"])
            continue
        out = ASSETS / e["path"]
        out.parent.mkdir(parents=True, exist_ok=True)
        c.save(out)
        made += 1
        sheet.append((e["id"][5:], c.to_image()))
        rows.append([e["id"], "src/main/resources/assets/gregguns/" + e["path"], "tools/asset-gen/icons.py",
                     zlib.crc32(e["id"].encode()), "Procedural 16x16 pixel art (pixel.py parts, bevel shading, outline pass)",
                     "MIT", "2026-10-18"])
    cdir = ROOT / "tools/asset-gen/credits"
    cdir.mkdir(parents=True, exist_ok=True)
    with open(cdir / "icons.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["asset", "path", "generator", "seed", "method", "license", "date"])
        w.writerows(rows)
    print(f"rendered {made}/{len(icons)}; missing: {missing}")
    if "--sheet" in sys.argv or True:
        contact_sheet(sheet)


def contact_sheet(items, cols=12, scale=8, path=None):
    from PIL import Image, ImageDraw, ImageFont
    cell_w, cell_h = 16 * scale + 8, 16 * scale + 30
    rows = (len(items) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cell_w, rows * cell_h), (46, 48, 54, 255))
    d = ImageDraw.Draw(sheet)
    font = ImageFont.load_default()
    # canonical order: manifest order
    for i, (name, im) in enumerate(items):
        cx, cy = (i % cols) * cell_w, (i // cols) * cell_h
        bg = Image.new("RGBA", (16 * scale, 16 * scale), (92, 96, 104, 255))
        up = im.resize((16 * scale, 16 * scale), Image.NEAREST)
        bg.alpha_composite(up)
        sheet.paste(bg, (cx + 4, cy + 4))
        label = name if len(name) <= 21 else name[:20] + "~"
        d.text((cx + 4, cy + 16 * scale + 8), label[:21], fill=(230, 230, 230, 255), font=font)
        if len(name) > 21:
            d.text((cx + 4, cy + 16 * scale + 18), name[20:41], fill=(180, 180, 190, 255), font=font)
    out = Path(path) if path else ROOT / "docs/assets/previews/icons.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.convert("RGB").save(out)


if __name__ == "__main__":
    main()
