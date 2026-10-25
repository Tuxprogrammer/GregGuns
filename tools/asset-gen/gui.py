#!/usr/bin/env python3
"""GregGuns GUI sprite sheets (kind: gui). Original art, procedural, no Techguns input.

Machines use GT's ModularUI2 backgrounds, so each sheet only carries the extra
widget sprites (bars, gauges, buttons, slot overlays, icons), in GT's look:
light-gray panels, white top-left bevel, dark bottom-right bevel.

Run from repo root:  python -I -X utf8 tools/asset-gen/gui.py
Writes src/main/resources/assets/gregguns/textures/gui/*.png (256x256 RGBA),
tools/asset-gen/credits/gui.csv and docs/assets/previews/gui.png.
Sprite coordinates are exposed as LAYOUT (models.py appends them to model_layouts.md).
Deterministic: no randomness is used.
"""
import csv
import math
import os
import numpy as np
from PIL import Image, ImageDraw

import sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import outroot  # noqa: E402  (GREGGUNS_ASSETS_OUT redirects every output below)

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RES = str(outroot.RES)
PRE = str(outroot.PRE)
CREDITS_DIR = str(outroot.CREDITS_DIR)

PANEL = (198, 198, 198, 255); HI = (255, 255, 255, 255); LO = (85, 85, 85, 255)
DARK = (40, 40, 44, 255); SLOT = (139, 139, 139, 255)
RED = (200, 30, 30, 255); AMBER = (255, 180, 0, 255); GREEN = (60, 200, 70, 255)
BLUE = (64, 192, 255, 255); ORANGE = (255, 96, 32, 255); CLEAR = (0, 0, 0, 0)
OUT = (20, 20, 24, 255)
BRASS = (220, 170, 60, 255); BR2 = (160, 110, 30, 255); GRAY = (150, 150, 160, 255); G2 = (90, 90, 100, 255)


def sprite(w, h):
    im = Image.new("RGBA", (w, h), CLEAR)
    return im, ImageDraw.Draw(im)


def bevel(d, x0, y0, x1, y1, face=PANEL, hi=HI, lo=LO, edge=None):
    """Raised beveled box, inclusive coords."""
    if edge:
        d.rectangle([x0, y0, x1, y1], fill=edge)
        x0, y0, x1, y1 = x0 + 1, y0 + 1, x1 - 1, y1 - 1
    d.rectangle([x0, y0, x1, y1], fill=face)
    d.line([x0, y0, x1, y0], fill=hi); d.line([x0, y0, x0, y1], fill=hi)
    d.line([x0, y1, x1, y1], fill=lo); d.line([x1, y0, x1, y1], fill=lo)


def inset(d, x0, y0, x1, y1, face=SLOT):
    """Sunken slot/bar well (dark top-left, white bottom-right), like GT slots."""
    d.rectangle([x0, y0, x1, y1], fill=face)
    d.line([x0, y0, x1, y0], fill=LO); d.line([x0, y0, x0, y1], fill=LO)
    d.line([x0, y1, x1, y1], fill=HI); d.line([x1, y0, x1, y1], fill=HI)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (255,)


def slot(w=18, h=18):
    im, d = sprite(w, h); inset(d, 0, 0, w - 1, h - 1); return im


def slot_hover(w=18, h=18):
    im, d = sprite(w, h); inset(d, 0, 0, w - 1, h - 1, (200, 200, 220, 255)); return im


def bar_well(w, h):
    im, d = sprite(w, h); inset(d, 0, 0, w - 1, h - 1, (60, 60, 64, 255)); return im


def bar_fill(w, h, c):
    """Vertical-gradient fill (lighter on top), no outline; sits inside a well."""
    im, d = sprite(w, h)
    for i in range(h):
        d.line([0, i, w - 1, i], fill=shade(c, 1.25 - 0.5 * i / max(1, h - 1)))
    return im


def button(w, h, state):
    """state: 0 normal, 1 hover, 2 pressed, 3 disabled."""
    im, d = sprite(w, h)
    face = {0: PANEL, 1: (215, 215, 230, 255), 2: (165, 165, 165, 255), 3: (150, 150, 150, 255)}[state]
    if state == 2:
        bevel(d, 0, 0, w - 1, h - 1, face, LO, HI)
    else:
        bevel(d, 0, 0, w - 1, h - 1, face)
    return im


def glyph(im, rows, x, y, c):
    d = ImageDraw.Draw(im)
    for j, r in enumerate(rows):
        for i, ch in enumerate(r):
            if ch == "#":
                d.point((x + i, y + j), fill=c)


PLUS = ["..#..", "..#..", "#####", "..#..", "..#.."]
MINUS = [".....", ".....", "#####", ".....", "....."]
ARROW_L = ["...#", "..##", ".###", "####", ".###", "..##", "...#"]
ARROW_R = ["#...", "##..", "###.", "####", "###.", "##..", "#..."]
LOCK_C = [".####.", "#....#", "#....#", "######", "##..##", "##..##", "######"]
LOCK_O = [".####.", "#....#", "#.....", "######", "##..##", "##..##", "######"]


def icon_button(w, h, state, rows, col=DARK):
    im = button(w, h, state)
    rw, rh = len(rows[0]), len(rows)
    off = 1 if state == 2 else 0
    glyph(im, rows, (w - rw) // 2 + off, (h - rh) // 2 + off, col if state != 3 else (110, 110, 110, 255))
    return im


def icon(rows, pal, w=None):
    h = len(rows); w = w or len(rows[0])
    im, d = sprite(w, h)
    for j, r in enumerate(rows):
        for i, ch in enumerate(r):
            if ch in pal:
                d.point((i, j), fill=pal[ch])
    return im


SEG = {"0": "abcdef", "1": "bc", "2": "abged", "3": "abgcd", "4": "fgbc", "5": "afgcd", "6": "afgedc",
       "7": "abc", "8": "abcdefg", "9": "abcdfg"}


def digit(ch, c, off=(30, 10, 10, 255), w=8, h=13):
    im, d = sprite(w, h)
    segs = {"a": (1, 0, w - 2, 0), "g": (1, h // 2, w - 2, h // 2), "d": (1, h - 1, w - 2, h - 1),
            "f": (0, 1, 0, h // 2 - 1), "b": (w - 1, 1, w - 1, h // 2 - 1),
            "e": (0, h // 2 + 1, 0, h - 2), "c": (w - 1, h // 2 + 1, w - 1, h - 2)}
    for k, (x0, y0, x1, y1) in segs.items():
        d.line([x0, y0, x1, y1], fill=c if k in SEG[ch] else off)
    return im


def colon(c):
    im, d = sprite(3, 13)
    d.rectangle([1, 3, 2, 4], fill=c); d.rectangle([1, 8, 2, 9], fill=c)
    return im


def pip(w, h, c, state):
    im, d = sprite(w, h)
    if state == "full":
        d.rectangle([0, 0, w - 1, h - 1], fill=c)
        d.line([0, 0, w - 1, 0], fill=shade(c, 1.4)); d.line([0, h - 1, w - 1, h - 1], fill=shade(c, 0.6))
    else:
        d.rectangle([0, 0, w - 1, h - 1], fill=(50, 50, 54, 255), outline=OUT)
    return im


SHIELD = ["#########", "#########", "#########", "#########", ".#######.", "..#####..", "...###...", "....#...."]


def shield_pip(state, c=(120, 170, 220, 255)):
    im, d = sprite(9, 9)
    for j, r in enumerate(SHIELD):
        for i, ch in enumerate(r):
            if ch != "#":
                continue
            if state == "full":
                col = shade(c, 1.2 if j < 2 else 0.9)
            elif state == "half":
                col = shade(c, 1.1) if i < 5 else (60, 60, 66, 255)
            else:
                col = (60, 60, 66, 255)
            d.point((i, j), fill=col)
    snap = im.copy()
    for j in range(9):
        for i in range(9):
            if snap.getpixel((i, j))[3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                x, y = i + dx, j + dy
                if 0 <= x < 9 and 0 <= y < 9 and snap.getpixel((x, y))[3]:
                    d.point((i, j), fill=(15, 15, 20, 255)); break
    return im


def ammo_icon(kind):
    P = {"o": OUT, "b": BRASS, "d": BR2, "g": GRAY, "h": G2, "r": (200, 50, 40, 255), "w": (240, 240, 240, 255),
         "c": (64, 192, 255, 255), "k": (40, 130, 200, 255), "y": (255, 200, 40, 255), "n": (60, 180, 60, 255),
         "m": (30, 110, 40, 255), "f": (255, 120, 20, 255)}
    S = {
        "pistol": [".....oo.....", "....obbo....", "....obbo....", "....obbo....", "....obdo....", "....obdo....", "....oooo....", "...obbbbo...", "...oddddo...", "...oooooo..."],
        "rifle": ["......oo......", ".....ogho.....", ".....ogho.....", ".....ogho.....", ".....obdo.....", ".....obdo.....", ".....obdo.....", ".....obdo.....", "....oooooo....", "...obbbbbbo.."],
        "shell": ["..oooooo..", ".orrrrrro.", ".orrrrrro.", ".orrrrrro.", ".orwwwwro.", ".obbbbbbo.", ".obbbbbbo.", ".obddddbo.", "..oooooo.."],
        "rocket": ["....oo....", "...orro...", "...orro...", "..ogggho..", "..ogggho..", "..ogggho..", "..ogggho..", "..ohhhho..", ".ooggggoo.", ".of.oo.fo.", "..f....f.."],
        "cell": ["..oooooo..", ".occccccco".replace("occcccccco", "ocwccccco."), ".ocwcckko.", ".ocwcckko.", ".occckkko.", ".occckkko.", ".ockkkkko.", ".oggggggo.", "..oooooo.."],
        "grenade": ["....oog...", "...ohgo...", "..onnnno..", ".onnmmnno.", ".onmnnmno.", ".onnmmnno.", ".onmnnmno.", ".onnnnnno.", "..onnnno..", "...oooo..."],
        "fuel": ["...oggo...", "...oggo...", "..oyyyyo..", ".oyyffyyo.", ".oyffffyo.", ".oyffffyo.", ".oyyffyyo.", "..oyyyyo..", "...oooo..."],
        "bio": ["....oo....", "...onno...", "..onwnno..", ".onnnnnno.", ".onnnmnno.", ".onnmmnno.", "..onnnno..", "...oooo..."],
    }[kind]
    w = max(len(r) for r in S)
    return icon([r.ljust(w, ".") for r in S], P)


def hitmarker():
    im, d = sprite(16, 16)
    for a, b in (((1, 1), (5, 5)), ((14, 1), (10, 5)), ((1, 14), (5, 10)), ((14, 14), (10, 10))):
        d.line([a, b], fill=(255, 255, 255, 255))
        dx = 1 if a[0] < 8 else -1
        d.line([(a[0] + dx, a[1]), (b[0] + dx, b[1])], fill=(20, 20, 20, 160))
    return im


def crosshair(kind):
    im, d = sprite(16, 16); W = (255, 255, 255, 255); K = (0, 0, 0, 140)
    if kind == "cross":
        for a in ((7, 1, 7, 5), (7, 10, 7, 14), (1, 7, 5, 7), (10, 7, 14, 7)):
            d.line(a, fill=W)
        d.point((8, 8), fill=RED)
    elif kind == "dot":
        d.rectangle([6, 6, 9, 9], outline=K); d.rectangle([7, 7, 8, 8], fill=W)
    else:
        d.ellipse([2, 2, 13, 13], outline=W); d.rectangle([7, 7, 8, 8], fill=RED)
    return im


def arrow_prog(filled):
    im, d = sprite(24, 16)
    c = (232, 160, 64, 255) if filled else (95, 95, 100, 255)
    d.rectangle([0, 5, 15, 10], fill=c); d.polygon([(16, 1), (23, 7), (23, 8), (16, 14)], fill=c)
    d.line([0, 4, 15, 4], fill=OUT); d.line([0, 11, 15, 11], fill=OUT); d.line([0, 4, 0, 11], fill=OUT)
    d.line([16, 4, 16, 0], fill=OUT); d.line([16, 11, 16, 15], fill=OUT)
    d.line([16, 0, 23, 7], fill=OUT); d.line([16, 15, 23, 8], fill=OUT)
    if filled:
        d.line([1, 5, 15, 5], fill=(255, 215, 130, 255))
    return im


def pip_gauge(on):
    im, d = sprite(6, 10)
    d.rectangle([0, 0, 5, 9], fill=(232, 160, 64, 255) if on else (70, 70, 74, 255), outline=OUT)
    if on:
        d.line([1, 1, 4, 1], fill=(255, 220, 140, 255))
    return im


def ghost(kind):
    """Faint silhouette overlay for empty input slots (16x16, semi-transparent)."""
    S = {
        "bullet": ["......##........", ".....####.......", ".....####.......", ".....####.......", ".....####.......", ".....####.......", "....######......", "....######......"],
        "casing": ["....######......", "....#....#......", "....#....#......", "....#....#......", "....#....#......", "....#....#......", "....######......", "...########....."],
        "powder": ["....######......", "...#......#.....", "..#........#....", "..#..####..#....", "..#.######.#....", "..#.######.#....", "...########.....", "....######......"],
        "magazine": ["....######......", "....#....#......", "....######......", "....#....#......", "....######......", "....#....#......", "....######......", "....######......"],
    }[kind]
    rows = [r.ljust(16, ".") for r in S] + ["." * 16] * (16 - len(S))
    return icon(rows, {"#": (60, 60, 70, 110)}, 16)


LAYOUT = {}


def pack_sheet(sid, sprites, size=256, gap=1):
    """Shelf-pack (tallest first) and record coordinates in LAYOUT."""
    order = sorted(sprites, key=lambda s: (-s[1].height, -s[1].width))
    sheet = Image.new("RGBA", (size, size), CLEAR)
    x = y = rowh = 0; pos = {}
    for name, im in order:
        if x + im.width > size:
            x, y, rowh = 0, y + rowh + gap, 0
        assert y + im.height <= size, (sid, name)
        sheet.paste(im, (x, y)); pos[name] = (x, y, im.width, im.height)
        x += im.width + gap; rowh = max(rowh, im.height)
    LAYOUT[sid] = pos
    return sheet


def sheet_hud():
    s = [("ammo_" + k, ammo_icon(k)) for k in ("pistol", "rifle", "shell", "rocket", "cell", "grenade", "fuel", "bio")]
    s += [("mag_pip_full", pip(4, 8, AMBER, "full")), ("mag_pip_empty", pip(4, 8, AMBER, "empty")),
          ("clip_pip_full", pip(3, 3, AMBER, "full")), ("clip_pip_empty", pip(3, 3, AMBER, "empty"))]
    s += [("armor_pip_" + st, shield_pip(st)) for st in ("full", "half", "empty")]
    s.append(("hitmarker", hitmarker()))
    s += [("crosshair_" + k, crosshair(k)) for k in ("cross", "dot", "ring")]
    s += [("bar_cap_left", bar_well(3, 8)), ("bar_cap_right", bar_well(3, 8)), ("bar_empty", bar_well(40, 8)),
          ("bar_fill_sprint", bar_fill(38, 6, AMBER)), ("bar_fill_charge", bar_fill(38, 6, BLUE)),
          ("bar_fill_danger", bar_fill(38, 6, RED))]
    return pack_sheet("hud", s)


def sheet_ammo_press():
    s = [("progress_arrow_empty", arrow_prog(False)), ("progress_arrow_full", arrow_prog(True)),
         ("ram_pip_off", pip_gauge(False)), ("ram_pip_on", pip_gauge(True)),
         ("output_frame", slot(26, 26)), ("slot", slot())]
    s += [("ghost_" + k, ghost(k)) for k in ("bullet", "casing", "powder", "magazine")]
    return pack_sheet("ammo_press", s)


def sheet_pouch():
    fr, d = sprite(62, 44)
    bevel(d, 0, 0, 61, 43, (107, 74, 43, 255), (160, 120, 80, 255), (55, 36, 20, 255), edge=OUT)
    for i in range(0, 60, 4):
        d.point((i + 1, 2), fill=(150, 112, 74, 255)); d.point((i + 3, 41), fill=(70, 48, 28, 255))
    for r in range(2):
        for c in range(3):
            inset(d, 4 + c * 19, 4 + r * 19, 21 + c * 19, 21 + r * 19, (84, 58, 33, 255))
    flap, d = sprite(62, 8)
    bevel(d, 0, 0, 61, 7, (120, 84, 48, 255), (170, 128, 88, 255), (60, 40, 22, 255), edge=OUT)
    d.rectangle([28, 2, 33, 7], fill=BRASS, outline=OUT)
    s = [("pouch_frame", fr), ("pouch_flap", flap), ("slot", slot()), ("slot_hover", slot_hover()),
         ("slot_overlay_leather", icon(["#" * 18] * 18, {"#": (107, 74, 43, 90)}, 18))]
    return pack_sheet("ammo_pouch", s)


def tier_badge(n):
    c = [(150, 150, 150), (110, 190, 120), (90, 150, 230), (200, 120, 230), (240, 190, 60)][n] + (255,)
    im, d = sprite(12, 12)
    bevel(d, 0, 0, 11, 11, c, shade(c, 1.4), shade(c, 0.5), edge=OUT)
    for i in range(n + 1):
        d.point((3 + (i % 3) * 2, 3 + (i // 3) * 3), fill=(255, 255, 255, 255))
        d.point((3 + (i % 3) * 2, 4 + (i // 3) * 3), fill=OUT)
    return im


def sheet_repair():
    wrench = icon(["..###...", ".#..#...", "..#.#...", "...###..", "....###.", ".....###", "......##"], {"#": (70, 50, 30, 255)})
    s = [("damage_bar_well", bar_well(52, 8)), ("damage_fill_ok", bar_fill(50, 6, GREEN)),
         ("damage_fill_mid", bar_fill(50, 6, AMBER)), ("damage_fill_bad", bar_fill(50, 6, RED)),
         ("input_frame", slot(20, 20)), ("output_frame", slot(26, 26)), ("wrench_icon", wrench)]
    s += [("tier_badge_%d" % (n + 1), tier_badge(n)) for n in range(5)]
    return pack_sheet("repair_bench", s)


def sheet_camo():
    s = []
    for n, st in (("normal", 0), ("hover", 1), ("pressed", 2)):
        s.append(("arrow_left_" + n, icon_button(12, 18, st, ARROW_L)))
        s.append(("arrow_right_" + n, icon_button(12, 18, st, ARROW_R)))
    pf, d = sprite(34, 34)
    inset(d, 0, 0, 33, 33, (70, 74, 70, 255))
    for j in range(6):
        for i in range(6):
            if (i + j) % 2 == 0:
                d.rectangle([2 + i * 5, 2 + j * 5, 6 + i * 5, 6 + j * 5], fill=(78, 82, 78, 255))
    s += [("preview_frame", pf), ("slot", slot())]
    return pack_sheet("camo_bench", s)


def toggle(on, rows):
    im = button(14, 14, 2 if on else 0)
    o = 1 if on else 0
    glyph(im, rows, 4 + o, 4 + o, GREEN if on else (120, 40, 40, 255))
    ImageDraw.Draw(im).point((11, 2), fill=GREEN if on else RED)
    return im


def sheet_turret():
    s = [("slot", slot()), ("slot_hover", slot_hover()), ("health_well", bar_well(62, 8)),
         ("health_fill", bar_fill(60, 6, RED)), ("health_fill_low", bar_fill(60, 6, (120, 20, 20, 255)))]
    shapes = {"players": ["..##..", ".####.", "..##..", ".####.", "######", "#....#"],
              "hostile": ["#....#", "##..##", ".####.", "..##..", ".####.", "##..##"],
              "animals": ["#..#", "####", "####", ".##.", "####", "#..#"]}
    for k, rows in shapes.items():
        s.append(("toggle_%s_off" % k, toggle(False, rows))); s.append(("toggle_%s_on" % k, toggle(True, rows)))
    s += [("slot9_frame", slot(56, 56))]
    return pack_sheet("turret", s)


def arm_button(w, state, c):
    im = button(w, 16, state)
    d = ImageDraw.Draw(im); o = 1 if state == 2 else 0
    c = c if state != 3 else (110, 110, 110, 255)
    d.polygon([(w // 2 - 5 + o, 4 + o), (w // 2 + 4 + o, 8 + o), (w // 2 - 5 + o, 12 + o)], fill=c)
    return im


def lock_icon(closed, c=(70, 70, 76, 255)):
    return icon(LOCK_C if closed else LOCK_O, {"#": c})


def sheet_charge(adv=False):
    c = (255, 90, 20, 255) if adv else (255, 40, 30, 255)
    s = [("digit_" + ch, digit(ch, c)) for ch in "0123456789"]
    s += [("colon", colon(c)), ("digit_off", digit("8", (40, 12, 12, 255), (40, 12, 12, 255)))]
    for n, st in (("normal", 0), ("hover", 1), ("pressed", 2), ("disabled", 3)):
        s.append(("arm_button_" + n, arm_button(44, st, RED)))
    s += [("lock_closed", lock_icon(True)), ("lock_open", lock_icon(False))]
    if adv:
        r = ["....#...", "...###..", "..#.#.#.", ".#..#..#", "....#...", "....#...", "...###.."]
        s += [("remote_unlinked", icon(r, {"#": (110, 110, 116, 255)})), ("remote_linked", icon(r, {"#": (80, 230, 110, 255)})),
              ("signal_arc", icon([".######.", "#......#", "..####..", ".#....#.", "...##...", "........"], {"#": (255, 90, 20, 255)}))]
    s.append(("display_bg", bar_well(52, 17)))
    return pack_sheet("advanced_explosive_charge" if adv else "explosive_charge", s)


def sheet_chest():
    gr, d = sprite(166, 58)
    bevel(d, 0, 0, 165, 57, PANEL, HI, LO)
    for r in range(3):
        for c in range(9):
            inset(d, 4 + c * 18, 4 + r * 18, 21 + c * 18, 21 + r * 18)
    s = [("slot_grid_frame", gr), ("slot", slot())]
    sortc = [(100, 160, 230), (230, 180, 60), (120, 200, 120)]
    for k in range(3):
        for n, st in (("normal", 0), ("hover", 1), ("pressed", 2)):
            im = button(14, 14, st); d = ImageDraw.Draw(im); o = 1 if st == 2 else 0
            for i in range(3 - (k == 2)):
                d.line([3 + o, 4 + i * 3 + o, 10 - i * (k == 0) * 2 + o, 4 + i * 3 + o], fill=sortc[k] + (255,))
            s.append(("sort_%d_%s" % (k, n), im))
    return pack_sheet("storage_chest", s)


def gauge_dial():
    im, d = sprite(32, 32)
    d.ellipse([0, 0, 31, 31], fill=OUT); d.ellipse([2, 2, 29, 29], fill=(215, 215, 220, 255))
    d.ellipse([3, 3, 28, 28], outline=(130, 130, 140, 255))
    for i in range(11):
        a = math.radians(225 - 27 * i)
        d.line([(16 + 11 * math.cos(a), 16 - 11 * math.sin(a)), (16 + 13 * math.cos(a), 16 - 13 * math.sin(a))], fill=RED if i >= 8 else OUT)
    return im


def needle():
    im, d = sprite(13, 13)
    d.line([(6, 6), (1, 11)], fill=RED); d.line([(7, 6), (2, 11)], fill=(150, 20, 20, 255)); d.rectangle([5, 5, 7, 7], fill=OUT)
    return im


def sheet_multiblock():
    s = [("pressure_dial", gauge_dial()), ("pressure_needle", needle())]
    belt = [(150, 150, 160), (110, 190, 120), (90, 150, 230), (200, 120, 230), (240, 190, 60), (255, 120, 60)]
    for i in range(7):
        bt, d = sprite(54, 10)
        inset(d, 0, 0, 53, 9, (50, 50, 56, 255))
        for k in range(6):
            col = belt[k] if k < i else DARK[:3]
            d.rectangle([2 + k * 8, 2, 8 + k * 8, 7], fill=tuple(col) + (255,), outline=OUT)
        s.append(("tier_belt_%d" % i, bt))
    for n, c in (("off", (50, 50, 56)), ("on", (60, 220, 90)), ("warn", (255, 190, 0)), ("fault", (230, 40, 30))):
        for kind, sz in (("bay", 8), ("led", 5)):
            im, d = sprite(sz, sz)
            d.rectangle([0, 0, sz - 1, sz - 1], fill=c + (255,), outline=OUT)
            if n != "off":
                d.point((1, 1), fill=(255, 255, 255, 220))
            s.append(("%s_%s" % (kind, n), im))
    s.append(("slot", slot()))
    return pack_sheet("multiblock_widgets", s)


def gun_ghost():
    rows = [".##########.........", "#############........", "..##########.####....", ".....##.....########.",
            "....###.....####.....", "....###..............", "....###.............."]
    return icon([r.ljust(21, ".") for r in rows], {"#": (60, 60, 70, 110)}, 21)


def sheet_hatch():
    s = [("gun_slot", slot(26, 26)), ("gun_slot_hover", slot_hover(26, 26)), ("gun_ghost", gun_ghost()),
         ("lock_closed", lock_icon(True)), ("lock_open", lock_icon(False)), ("lock_closed_red", lock_icon(True, RED)),
         ("led_off", pip(5, 5, GREEN, "empty")), ("led_on", pip(5, 5, GREEN, "full"))]
    return pack_sheet("gun_mount_hatch", s)


SHEETS = {
    "hud": sheet_hud, "ammo_press": sheet_ammo_press, "ammo_pouch": sheet_pouch,
    "repair_bench": sheet_repair, "camo_bench": sheet_camo, "turret": sheet_turret,
    "explosive_charge": lambda: sheet_charge(False), "advanced_explosive_charge": lambda: sheet_charge(True),
    "storage_chest": sheet_chest, "multiblock_widgets": sheet_multiblock, "gun_mount_hatch": sheet_hatch,
}


# ---- full-screen scope overlays (textures/gui/scopes/*.png, 512x512) ----------------------
SCOPES = {
    "sniper": "Sniper scope: circular lens, hard black surround with soft vignette ring, fine crosshair, mil-dots.",
    "assault": "Assault / reflex scope: smaller view circle in a thick housing, red chevron reticle, drop ticks.",
    "energy": "Energy / tech scope: cyan HUD brackets, ring reticle with ticks, scanline vignette (alpha blended).",
}
SC = 512


def _radial(r0, r1, power=1.6):
    """Alpha (0..255) = 0 inside r0, ramping to 255 at r1."""
    yy, xx = np.mgrid[0:SC, 0:SC]
    r = np.hypot(xx - 255.5, yy - 255.5)
    return np.clip((r - r0) / (r1 - r0), 0, 1) ** power * 255, r


def _compose(alpha, color=(0, 0, 0), extra=None):
    base = np.zeros((SC, SC, 4), np.uint8)
    base[..., :3] = color
    base[..., 3] = np.clip(alpha, 0, 255).astype(np.uint8)
    im = Image.fromarray(base, "RGBA")
    if extra is not None:
        im.alpha_composite(extra)
    return im


def scope_sniper():
    a, r = _radial(200, 238, 1.4)
    a = np.where(r >= 238, 255, a)
    a = np.maximum(a, np.where((r > 226) & (r < 236), 255, 0))
    lay = Image.new("RGBA", (SC, SC), (0, 0, 0, 0)); d = ImageDraw.Draw(lay); K = (0, 0, 0, 255)
    c = 256
    # hairlines edge to edge, heavy posts outside 60 px
    d.rectangle([16, c - 1, c - 60, c], fill=K); d.rectangle([c + 60, c - 1, 496, c], fill=K)
    d.rectangle([c - 1, 16, c, c - 60], fill=K); d.rectangle([c - 1, c + 60, c, 496], fill=K)
    for s0, s1 in ((-100, -60), (60, 100)):
        d.rectangle([c + s0, c - 3, c + s1, c + 2], fill=K); d.rectangle([c - 3, c + s0, c + 2, c + s1], fill=K)
    for i in range(1, 5):
        for sgn in (-1, 1):
            o = sgn * (6 + i * 10)
            d.ellipse([c + o - 2, c - 2, c + o + 2, c + 2], fill=K); d.ellipse([c - 2, c + o - 2, c + 2, c + o + 2], fill=K)
    for k in range(1, 6):
        d.rectangle([c - 9, c + 100 + k * 15, c + 9, c + 101 + k * 15], fill=K) if 100 + k * 15 < 200 else None
    d.ellipse([c - 1, c - 1, c + 1, c + 1], fill=(0, 0, 0, 0))
    return _compose(a, extra=lay)


def scope_assault():
    a, r = _radial(120, 150, 1.0)
    a = np.where(r >= 150, 255, a)
    lay = Image.new("RGBA", (SC, SC), (0, 0, 0, 0)); d = ImageDraw.Draw(lay)
    c = 256
    RED = (255, 40, 30, 255); GLOW = (255, 40, 30, 70)
    d.line([(c - 28, c + 22), (c, c - 4), (c + 28, c + 22)], fill=GLOW, width=7)
    d.line([(c - 28, c + 22), (c, c - 4), (c + 28, c + 22)], fill=RED, width=3)
    d.ellipse([c - 2, c + 2, c + 2, c + 6], fill=RED)
    for k in range(1, 5):
        y = c + 22 + k * 20; w = 22 - k * 3
        d.line([(c - w, y + 6), (c, y), (c + w, y + 6)], fill=(255, 60, 40, 230), width=2)
    d.line([(c - 100, c), (c - 40, c)], fill=(0, 0, 0, 255), width=2); d.line([(c + 40, c), (c + 100, c)], fill=(0, 0, 0, 255), width=2)
    for k in range(5):
        d.line([(c - 100 + k * 15, c - 4), (c - 100 + k * 15, c + 4)], fill=(0, 0, 0, 255), width=2)
        d.line([(c + 100 - k * 15, c - 4), (c + 100 - k * 15, c + 4)], fill=(0, 0, 0, 255), width=2)
    im = _compose(a, color=(8, 8, 10), extra=lay)
    px = np.array(im)
    px[(r >= 146) & (r < 152)] = (60, 60, 68, 255)   # lens ring highlight
    px[(r >= 170) & (r < 176)] = (40, 40, 46, 255)   # housing bevel
    return Image.fromarray(px, "RGBA")


def scope_energy():
    a, r = _radial(180, 260, 1.5)
    yy, xx = np.mgrid[0:SC, 0:SC]
    a = np.clip(a + np.where((yy % 4 == 0) & (r > 190), 40, 0), 0, 235)
    lay = Image.new("RGBA", (SC, SC), (0, 0, 0, 0)); d = ImageDraw.Draw(lay)
    C = (64, 224, 255, 255); G = (64, 224, 255, 60); c = 256
    d.ellipse([c - 60, c - 60, c + 60, c + 60], outline=C, width=2)
    d.ellipse([c - 64, c - 64, c + 64, c + 64], outline=G, width=4)
    bb = [c - 110, c - 110, c + 110, c + 110]
    d.arc(bb, 200, 250, fill=C, width=3); d.arc(bb, 20, 70, fill=C, width=3)
    d.arc(bb, 290, 340, fill=G, width=3); d.arc(bb, 110, 160, fill=G, width=3)
    for ang in range(0, 360, 30):
        ca, sa = math.cos(math.radians(ang)), math.sin(math.radians(ang))
        d.line([(c + 60 * ca, c + 60 * sa), (c + 72 * ca, c + 72 * sa)], fill=C, width=2)
    for sgn in (-1, 1):
        d.line([(c + sgn * 20, c), (c + sgn * 52, c)], fill=C, width=2); d.line([(c, c + sgn * 20), (c, c + sgn * 52)], fill=C, width=2)
    d.rectangle([c - 1, c - 1, c + 1, c + 1], fill=C)
    for sx in (-1, 1):
        for sy in (-1, 1):
            x, y = c + sx * 210, c + sy * 210
            d.line([(x, y), (x - sx * 46, y)], fill=C, width=3); d.line([(x, y), (x, y - sy * 46)], fill=C, width=3)
            d.line([(x - sx * 8, y - sy * 8), (x - sx * 30, y - sy * 8)], fill=G, width=3)
    for k in range(-5, 6):
        w = 14 if k % 5 == 0 else 8
        d.line([(c - 232, c + k * 18), (c - 232 + w, c + k * 18)], fill=C, width=2)
        d.line([(c + 232 - w, c + k * 18), (c + 232, c + k * 18)], fill=C, width=2)
    for i in range(6):
        d.rectangle([c - 28 + i * 10, c + 190, c - 22 + i * 10, c + 198], fill=C if i < 4 else G)
    return _compose(a, color=(0, 10, 16), extra=lay)


def write_scopes():
    os.makedirs(os.path.join(RES, "textures", "gui", "scopes"), exist_ok=True)
    out = []
    for name, fn in (("sniper", scope_sniper), ("assault", scope_assault), ("energy", scope_energy)):
        im = fn(); im.save(os.path.join(RES, "textures", "gui", "scopes", name + ".png")); out.append((name, im))
    return out


def layout_markdown():
    out = ["## GUI sprite sheets (kind gui)", "",
           "Each sheet is 256x256 RGBA; only the listed rectangles are painted (x, y, w, h in pixels). "
           "Backgrounds come from GT ModularUI2. Generated by `gui.py`.", ""]
    for sid, pos in LAYOUT.items():
        out += ["### gui/%s.png" % sid, "", ", ".join("`%s` (%d,%d,%d,%d)" % ((n,) + p) for n, p in pos.items()), ""]
    return "\n".join(out)


def build_all():
    """Returns [(sid, image)] and fills LAYOUT."""
    LAYOUT.clear()
    return [(sid, fn()) for sid, fn in SHEETS.items()]


def main():
    os.makedirs(os.path.join(RES, "textures", "gui"), exist_ok=True); os.makedirs(PRE, exist_ok=True)
    rows = []; tiles = build_all()
    for sid, im in tiles:
        im.save(os.path.join(RES, "textures", "gui", sid + ".png"))
        rows.append(["gui.%s" % sid, "src/main/resources/assets/gregguns/textures/gui/%s.png" % sid, "tools/asset-gen/gui.py", "none",
                     "Procedural PIL sprite sheet in GT-style gray bevels; shelf-packed widget sprites", "MIT", "2026-10-18"])
    scopes = write_scopes()
    for name, im in scopes:
        rows.append(["gui.scope_%s" % name, "src/main/resources/assets/gregguns/textures/gui/scopes/%s.png" % name, "tools/asset-gen/gui.py", "none",
                     "Procedural numpy/Pillow 512x512 scope overlay: radial vignette plus drawn reticle", "MIT", "2026-10-18"])
    os.makedirs(CREDITS_DIR, exist_ok=True)
    with open(os.path.join(CREDITS_DIR, "gui.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["asset", "path", "generator", "seed", "method", "license", "date"]); w.writerows(rows)
    sc = 3; W = 1500; x = y = rowh = 0; items = []
    tiles = tiles + [("scopes/" + n, i.resize((160, 160), Image.LANCZOS)) for n, i in scopes]
    for sid, im in tiles:
        bb = im.getbbox(); c = im.crop(bb)
        bg = Image.new("RGBA", c.size, (70, 110, 90, 255)); bg.alpha_composite(c)
        items.append((sid, bg.resize((c.width * sc, c.height * sc), Image.NEAREST).convert("RGB")))
    pos = []
    for sid, t in items:
        if x + t.width > W: x, y, rowh = 0, y + rowh + 20, 0
        pos.append((x, y + 14, sid, t)); x += t.width + 10; rowh = max(rowh, t.height + 14)
    sheet = Image.new("RGB", (W, y + rowh + 6), (30, 30, 34)); d = ImageDraw.Draw(sheet)
    for x, y, sid, t in pos:
        sheet.paste(t, (x, y)); d.text((x, y - 12), "gui/%s" % sid, fill=(235, 235, 235))
    sheet.save(os.path.join(PRE, "gui.png"))
    print("gui sheets:", len(tiles))


if __name__ == "__main__":
    main()
