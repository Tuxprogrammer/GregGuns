#!/usr/bin/env python3
"""GregGuns branding and infographic generator.

Original artwork: nothing here imitates Techguns' logo or art (Techguns is
All Rights Reserved). Every SVG is a hand-authored string built by this script,
so the output is reproducible: run it again and the same files come out.

Usage (from the repo root):
    python tools/asset-gen/branding.py            # SVG + PNG
    python tools/asset-gen/branding.py --svg-only # SVG only, no rasteriser needed

Outputs
    docs/assets/branding/     logo.svg, logo_pixel.svg, banner.svg, social_preview.svg (+ PNGs)
    docs/assets/infographics/ tier-progression, machine-roster, ammo-production-flow,
                              questbook-overview (.svg + .png)

Rasteriser (documented as requested)
    PRIMARY: Playwright for Python driving the installed Microsoft Edge
        (chromium.launch(channel="msedge")). The SVG is inlined into a bare HTML page and
        screenshotted at the exact target size. PNGs for logos keep a transparent background.
    FALLBACK: cairosvg, if Playwright/Edge is unavailable (needs libcairo on the machine).
    Install: `python -m pip install playwright` (Edge already ships with Windows 10/11).
    Pixel-art PNGs (logo_pixel_*.png) are drawn with Pillow, nearest-neighbour, so they stay crisp.

Fonts: lettering in the logo, banner and social card is drawn as pixel-block paths from a
5x7 glyph table below, so it does not depend on any installed font. Infographic body text
uses the generic family `sans-serif` (with Segoe UI/Arial first choices) and is only used
for labels, so small metric differences between machines do not change the meaning.

Facts come from docs/design/0001, 0009, 0010 and docs/gregguns-spec (08 roster, multiblocks,
06 recipes, 12 questbook). If those change, update the tables in this file.
"""
from __future__ import annotations

import math
import re
import sys
import textwrap
from pathlib import Path
from xml.sax.saxutils import escape

ROOT = Path(__file__).resolve().parents[2]
BRAND = ROOT / "docs" / "assets" / "branding"
INFO = ROOT / "docs" / "assets" / "infographics"

# ---------------------------------------------------------------- palette
INK = "#10151b"        # deepest background
PANEL = "#1a222b"
PANEL2 = "#232e3a"
LINE = "#3c4a58"
TXT = "#eaf0f5"
MUTED = "#9fb0bf"
STEEL_HI = "#e4eaf0"
STEEL = "#aab6c2"
BLUE = "#2d8cff"       # GT blue accent
CYAN = "#3fd0ff"
ORANGE = "#ff8a1f"     # GT orange accent
FONT = "'Segoe UI', 'Helvetica Neue', Arial, sans-serif"
MONO = "Consolas, 'Courier New', monospace"

# GT voltage-name colours, readable on a dark card: key, label, EU/t, fill, text on fill
TIERS = [
    ("ULV", "Steam / ULV", "steam", "#7b8794", INK),
    ("LV", "LV", "30 EU/t", "#c5cdd6", INK),
    ("MV", "MV", "120 EU/t", "#2fc4d6", INK),
    ("HV", "HV", "480 EU/t", "#ffb020", INK),
    ("EV", "EV", "1,920 EU/t", "#b04fd8", "#ffffff"),
    ("IV", "IV", "7,680 EU/t", "#4a63ff", "#ffffff"),
    ("LuV", "LuV", "30,720 EU/t", "#ff6fd0", INK),
    ("ZPM", "ZPM", "122,880 EU/t", "#ff5a5a", INK),
]
TIER = {t[0]: t for t in TIERS}


# ---------------------------------------------------------------- tiny SVG toolkit
def wrap(s: str, width_px: float, size: float, factor: float = 0.54) -> list[str]:
    n = max(8, int(width_px / (size * factor)))
    return textwrap.wrap(s, n, break_long_words=False) or [""]


class Svg:
    def __init__(self, w: int, h: int, bg: str | None = INK, radius: int = 0):
        self.w, self.h = w, h
        self.defs: list[str] = []
        self.body: list[str] = []
        if bg:
            self.body.append(f'<rect width="{w}" height="{h}" rx="{radius}" fill="{bg}"/>')

    def add(self, s: str):
        self.body.append(s)

    def rect(self, x, y, w, h, fill, rx=0, stroke=None, sw=1, extra=""):
        st = f' stroke="{stroke}" stroke-width="{sw}"' if stroke else ""
        self.add(f'<rect x="{x:g}" y="{y:g}" width="{w:g}" height="{h:g}" rx="{rx:g}" fill="{fill}"{st} {extra}/>')

    def line(self, x1, y1, x2, y2, stroke=LINE, sw=2, extra=""):
        self.add(f'<line x1="{x1:g}" y1="{y1:g}" x2="{x2:g}" y2="{y2:g}" stroke="{stroke}" stroke-width="{sw}" {extra}/>')

    def poly(self, pts, fill, stroke=None, sw=1, extra=""):
        p = " ".join(f"{x:.1f},{y:.1f}" for x, y in pts)
        st = f' stroke="{stroke}" stroke-width="{sw}" stroke-linejoin="round"' if stroke else ""
        self.add(f'<polygon points="{p}" fill="{fill}"{st} {extra}/>')

    def text(self, x, y, s, size=14, fill=TXT, anchor="start", weight="normal", family=FONT, extra=""):
        self.add(
            f'<text x="{x:g}" y="{y:g}" font-family="{family}" font-size="{size:g}" '
            f'font-weight="{weight}" fill="{fill}" text-anchor="{anchor}" {extra}>{escape(s)}</text>'
        )

    def para(self, x, y, s, width, size=14, fill=TXT, lh=1.3, weight="normal", family=FONT) -> float:
        """Wrapped text; returns the y just below the last line."""
        for line in wrap(s, width, size, 0.6 if family == MONO else 0.54):
            self.text(x, y, line, size, fill, weight=weight, family=family)
            y += size * lh
        return y

    def arrow(self, x1, y1, x2, y2, stroke=MUTED, sw=2.5):
        self.line(x1, y1, x2, y2, stroke, sw, 'marker-end="url(#arr)"')

    def render(self) -> str:
        defs = (
            '<marker id="arr" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="5" markerHeight="5" orient="auto-start-reverse">'
            '<path d="M0 0 L10 5 L0 10 z" fill="context-stroke"/></marker>' + "".join(self.defs)
        )
        return (
            f'<svg xmlns="http://www.w3.org/2000/svg" width="{self.w}" height="{self.h}" viewBox="0 0 {self.w} {self.h}">\n'
            f"<defs>{defs}</defs>\n" + "\n".join(self.body) + "\n</svg>\n"
        )


def chip(s: Svg, x, y, label, fill, tcol=INK, size=13, padx=9, h=24, weight="600") -> float:
    w = len(label) * size * 0.56 + padx * 2
    s.rect(x, y, w, h, fill, rx=h / 2)
    s.text(x + w / 2, y + h / 2 + size * 0.35, label, size, tcol, "middle", weight)
    return w


def tier_badge(s: Svg, x, y, key, w=96, h=30, size=15):
    _, label, _, fill, tcol = TIER[key]
    if w < 80:
        label = key  # narrow badges: no room for "Steam / ULV"
    s.rect(x, y, w, h, fill, rx=6)
    s.text(x + w / 2, y + h / 2 + size * 0.35, label, size, tcol, "middle", "700")


# ---------------------------------------------------------------- pixel lettering
GLYPHS = {
    "G": [".###.", "#...#", "#....", "#.###", "#...#", "#...#", ".###."],
    "R": ["####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#"],
    "E": ["#####", "#....", "#....", "####.", "#....", "#....", "#####"],
    "U": ["#...#"] * 6 + [".###."],
    "N": ["#...#", "##..#", "##..#", "#.#.#", "#..##", "#..##", "#...#"],
    "S": [".####", "#....", "#....", ".###.", "....#", "....#", "####."],
    "T": ["#####"] + ["..#.."] * 6,
    "C": [".###.", "#...#", "#....", "#....", "#....", "#...#", ".###."],
    "H": ["#...#"] * 3 + ["#####"] + ["#...#"] * 3,
    "A": [".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"],
    "I": ["#####", "..#..", "..#..", "..#..", "..#..", "..#..", "#####"],
    "V": ["#...#"] * 5 + [".#.#.", "..#.."],
    "W": ["#...#", "#...#", "#...#", "#.#.#", "#.#.#", "##.##", "#...#"],
    "P": ["####.", "#...#", "#...#", "####.", "#....", "#....", "#...."],
    "O": [".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."],
    "Y": ["#...#", "#...#", ".#.#.", "..#..", "..#..", "..#..", "..#.."],
    "L": ["#...."] * 6 + ["#####"],
    "M": ["#...#", "##.##", "#.#.#", "#.#.#", "#...#", "#...#", "#...#"],
    "Z": ["#####", "....#", "...#.", "..#..", ".#...", "#....", "#####"],
    "-": [".....", ".....", ".....", "#####", ".....", ".....", "....."],
    " ": ["...", "...", "...", "...", "...", "...", "..."],
}


def pixel_text(s: Svg, x, y, word, px, fill, shadow=None, spacing=1, shadow_off=None):
    """Draw `word` with its top-left at (x, y); each glyph cell is `px` px. Returns width."""
    cells = []
    cx = 0.0
    for ch in word:
        g = GLYPHS[ch]
        gw = len(g[0])
        for r, row in enumerate(g):
            c = 0
            while c < gw:  # merge runs into one rect: smaller files, no seams
                if row[c] == "#":
                    c2 = c
                    while c2 < gw and row[c2] == "#":
                        c2 += 1
                    rx, ry, rw = x + (cx + c) * px, y + r * px, (c2 - c) * px
                    cells.append(f"M{rx:g} {ry:g}h{rw:g}v{px:g}h-{rw:g}z")
                    c = c2
                else:
                    c += 1
        cx += gw + spacing
    d = shadow_off if shadow_off is not None else px * 0.5
    d_all = "".join(cells)
    if shadow:
        s.add(f'<path d="{d_all}" transform="translate({d:g} {d:g})" fill="{shadow}"/>')
    s.add(f'<path d="{d_all}" fill="{fill}"/>')
    return (cx - spacing) * px


def pixel_width(word, px, spacing=1):
    return (sum(len(GLYPHS[c][0]) + spacing for c in word) - spacing) * px


# ---------------------------------------------------------------- the mark
def hex_points(cx, cy, r, rot=-90):
    return [(cx + r * math.cos(math.radians(rot + 60 * k)), cy + r * math.sin(math.radians(rot + 60 * k))) for k in range(6)]


def logo_mark(uid: str, size: float = 256) -> tuple[str, str]:
    """Return (defs, group) for the vector mark drawn in a 256x256 box scaled to `size`.

    Idea: a GT-style steel hex casing (the shape of a GT machine casing / bolt head) holding a
    side-on rifle silhouette, with a blue cable trace (EU) running under it and a
    bullet in GT orange.
    """
    k = size / 256
    outer = hex_points(128, 128, 122)
    mid = hex_points(128, 128, 104)
    inner = hex_points(128, 128, 94)
    pts = lambda p: " ".join(f"{x:.1f},{y:.1f}" for x, y in p)
    defs = (
        f'<linearGradient id="{uid}s" x1="0" y1="0" x2="0" y2="1">'
        f'<stop offset="0" stop-color="#8795a3"/><stop offset="0.5" stop-color="#4b5764"/><stop offset="1" stop-color="#2a323b"/></linearGradient>'
        f'<linearGradient id="{uid}r" x1="0" y1="0" x2="0" y2="1">'
        f'<stop offset="0" stop-color="#f4f8fb"/><stop offset="1" stop-color="#aab6c2"/></linearGradient>'
        f'<linearGradient id="{uid}o" x1="0" y1="0" x2="1" y2="0">'
        f'<stop offset="0" stop-color="#ffb347"/><stop offset="1" stop-color="#ff7a00"/></linearGradient>'
    )
    rivets = "".join(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="5" fill="#cfd8e0" stroke="#1b2128" stroke-width="2"/>' for x, y in hex_points(128, 128, 111))
    g = f"""<g transform="scale({k:g})">
<polygon points="{pts(outer)}" fill="url(#{uid}s)" stroke="#0e1216" stroke-width="5" stroke-linejoin="round"/>
<polygon points="{pts(mid)}" fill="none" stroke="{BLUE}" stroke-width="5" stroke-linejoin="round"/>
<polygon points="{pts(inner)}" fill="#141a21" stroke="#0e1216" stroke-width="3" stroke-linejoin="round"/>
{rivets}
<path d="M40 176 H88 L100 188 H152" fill="none" stroke="{CYAN}" stroke-width="5" stroke-linecap="round" stroke-linejoin="round"/>
<circle cx="40" cy="176" r="6" fill="{CYAN}"/><circle cx="152" cy="188" r="6" fill="{BLUE}" stroke="#0e1216" stroke-width="2"/>
<path d="M48 114 L74 108 L84 104 L170 104 L170 100 L182 100 L182 104 L190 104 L190 112 L214 112 L214 120 L190 120 L190 128 L124 128 L120 134 L104 134 L102 160 L84 160 L86 134 L78 134 L70 148 L50 154 Z"
 fill="url(#{uid}r)" stroke="#0e1216" stroke-width="4" stroke-linejoin="round"/>
<path d="M118 92 H162 V100 H118 Z" fill="#cfd8e0" stroke="#0e1216" stroke-width="3" stroke-linejoin="round"/>
<path d="M124 134 H148 L146 160 H126 Z" fill="#7d8a97" stroke="#0e1216" stroke-width="3" stroke-linejoin="round"/>
<rect x="132" y="108" width="24" height="8" fill="{BLUE}"/>
<path d="M170 168 H196 L208 175 L196 182 H170 Z" fill="url(#{uid}o)" stroke="#0e1216" stroke-width="3" stroke-linejoin="round"/>
<rect x="158" y="168" width="12" height="14" fill="#d9a441" stroke="#0e1216" stroke-width="3"/>
</g>"""
    return defs, g


PIX_OUTER, PIX_RING, PIX_IN = 15.6, 13.4, 12.0


def pixel_logo_rects() -> list[tuple[int, int, int, int, str]]:
    """32x32 pixel-art contents (rifle, cable, bullet) as (x, y, w, h, colour) rectangles.

    The hex casing behind them is vector (anti-aliased) so its edge is smooth; the
    contents stay hard-edged pixels.
    """
    grid: list[list] = [[None] * 32 for _ in range(32)]

    def fill(x, y, w, h, c):
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                grid[yy][xx] = c

    fill(7, 14, 3, 3, "#dfe6ec")    # stock
    fill(9, 13, 11, 3, "#f4f8fb")   # receiver
    fill(20, 13, 6, 2, "#dfe6ec")   # barrel
    fill(26, 12, 1, 4, "#aab6c2")   # muzzle brake
    fill(12, 11, 5, 1, "#aab6c2")   # scope
    fill(15, 16, 2, 4, "#7d8a97")   # magazine
    fill(10, 16, 2, 3, "#dfe6ec")   # grip
    fill(17, 14, 3, 1, CYAN)        # EU accent on receiver
    fill(8, 22, 5, 1, CYAN)         # cable
    fill(12, 22, 1, 2, CYAN)
    fill(17, 22, 4, 2, "#d9a441")   # casing
    fill(21, 22, 2, 2, ORANGE)      # tip
    fill(23, 22, 1, 2, "#ffb347")
    out = []
    for y in range(32):  # merge horizontal runs
        x = 0
        while x < 32:
            c = grid[y][x]
            if c is None:
                x += 1
                continue
            x2 = x
            while x2 < 32 and grid[y][x2] == c:
                x2 += 1
            out.append((x, y, x2 - x, 1, c))
            x = x2
    return out


def pixel_logo_svg(px=8) -> str:
    s = Svg(32 * px, 32 * px, bg=None)
    for r, col in ((PIX_OUTER, "#5b6876"), (PIX_RING, BLUE), (PIX_IN, "#141a21")):
        s.poly([(x * px, y * px) for x, y in hex_points(16, 16, r)], col)
    s.add('<g shape-rendering="crispEdges">')
    for x, y, w, h, c in pixel_logo_rects():
        s.add(f'<rect x="{x*px}" y="{y*px}" width="{w*px}" height="{h*px}" fill="{c}"/>')
    s.add("</g>")
    return s.render()


def tier_hex_strip(s: Svg, x, y, r, gap=4):
    """Eight small hexes in the GT voltage-name colours (ULV..ZPM) as a row."""
    w = math.sqrt(3) * r
    for i, t in enumerate(TIERS):
        cx = x + i * (w + gap) + w / 2
        s.poly(hex_points(cx, y, r, rot=-90), t[3], stroke="#0e1216", sw=2)


def hex_grid_bg(s: Svg, w, h, r=34, opacity=0.07):
    hw = math.sqrt(3) * r
    d = []
    row = 0
    y = 0.0
    while y < h + r * 2:
        x = -hw if row % 2 else -hw / 2
        while x < w + hw:
            pts = hex_points(x + hw / 2, y, r - 1.5)
            d.append("M" + "L".join(f"{px:.1f} {py:.1f}" for px, py in pts) + "z")
            x += hw
        y += r * 1.5
        row += 1
    s.add(f'<path d="{"".join(d)}" fill="none" stroke="#9fb0bf" stroke-opacity="{opacity}" stroke-width="1.5"/>')


# ---------------------------------------------------------------- branding assets
def make_logo() -> str:
    s = Svg(256, 256, bg=None)
    defs, g = logo_mark("l")
    s.defs.append(defs)
    s.add(g)
    return s.render()


def wordmark(s: Svg, x, y, px):
    wg = pixel_width("GREG", px)
    pixel_text(s, x, y, "GREG", px, STEEL_HI, "#0b0f13", shadow_off=px * 0.6)
    pixel_text(s, x + wg + px * 1.2, y, "GUNS", px, ORANGE, "#0b0f13", shadow_off=px * 0.6)
    return wg + px * 1.2 + pixel_width("GUNS", px)


def make_banner() -> str:
    W, H = 1280, 320
    s = Svg(W, H, bg=None)
    s.defs.append(
        '<linearGradient id="bbg" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#1e2832"/><stop offset="1" stop-color="#0d1217"/></linearGradient>'
    )
    s.rect(0, 0, W, H, "url(#bbg)")
    hex_grid_bg(s, W, H)
    s.rect(0, H - 8, W, 8, BLUE)
    s.rect(0, H - 8, 220, 8, ORANGE)
    defs, g = logo_mark("b", 236)
    s.defs.append(defs)
    s.add(f'<g transform="translate(46 38)">{g}</g>')
    wordmark(s, 322, 58, 12)
    pixel_text(s, 324, 168, "GREGTECH-NATIVE WEAPONRY", 4.6, STEEL, "#0b0f13", shadow_off=2.5)
    s.text(324, 232, "Tiered guns, armour and ammo for GT New Horizons (Minecraft 1.7.10).", 21, MUTED)
    tier_hex_strip(s, 324, 268, 14)
    s.text(324 + 8 * 31 + 14, 273, "ULV to ZPM, one voltage tier at a time", 15, MUTED)
    return s.render()


def make_social() -> str:
    W, H = 1280, 640
    s = Svg(W, H, bg=None)
    s.defs.append(
        '<radialGradient id="sbg" cx="0.3" cy="0.3" r="1.1"><stop offset="0" stop-color="#26323e"/><stop offset="1" stop-color="#0b0f14"/></radialGradient>'
    )
    s.rect(0, 0, W, H, "url(#sbg)")
    hex_grid_bg(s, W, H, r=46, opacity=0.08)
    defs, g = logo_mark("s", 380)
    s.defs.append(defs)
    s.add(f'<g transform="translate(80 130)">{g}</g>')
    wordmark(s, 520, 160, 13)
    pixel_text(s, 524, 280, "GREGTECH-NATIVE WEAPONRY", 4.8, STEEL, "#0b0f13", shadow_off=2.5)
    y = 362
    for line in ("Guns, armour and ammo that climb the GregTech tiers.",
                 "Built for GT New Horizons: GT materials, recipe maps and EU."):
        s.text(524, y, line, 27, TXT)
        y += 40
    tier_hex_strip(s, 524, 505, 24, gap=6)
    s.text(524, 568, "ULV to ZPM", 22, MUTED, weight="600")
    s.rect(0, H - 14, W, 14, BLUE)
    s.rect(0, H - 14, 360, 14, ORANGE)
    return s.render()


# ---------------------------------------------------------------- isometric helper
class Iso:
    def __init__(self, u=12):
        self.u = u
        self.boxes: list[tuple] = []

    def box(self, x, y, z, dx, dy, dz, base, op=1.0):
        self.boxes.append((x, y, z, dx, dy, dz, base, op))

    @staticmethod
    def shade(hexcol, f):
        c = hexcol.lstrip("#")
        r, g, b = (int(c[i:i + 2], 16) for i in (0, 2, 4))
        r, g, b = (max(0, min(255, int(v * f))) for v in (r, g, b))
        return f"#{r:02x}{g:02x}{b:02x}"

    def _p(self, X, Y, Z):
        # Oblique projection: x runs right and slightly down, y comes toward the viewer
        # (down-left), z is up. Long strips stay wide instead of running steeply diagonal.
        u = self.u
        return (X * u * 0.95 - Y * u * 0.55, X * u * 0.30 + Y * u * 0.42 - Z * u)

    def draw(self, s: Svg, cx, cy, maxw, maxh):
        faces = []
        order = sorted(self.boxes, key=lambda b: (b[0] + b[1] + b[2] * 0.01, b[2]))
        P = self._p
        for (x, y, z, dx, dy, dz, base, op) in order:
            top = [P(x, y, z + dz), P(x + dx, y, z + dz), P(x + dx, y + dy, z + dz), P(x, y + dy, z + dz)]
            left = [P(x, y + dy, z), P(x + dx, y + dy, z), P(x + dx, y + dy, z + dz), P(x, y + dy, z + dz)]
            right = [P(x + dx, y, z), P(x + dx, y + dy, z), P(x + dx, y + dy, z + dz), P(x + dx, y, z + dz)]
            faces.append((top, self.shade(base, 1.18), op))
            faces.append((left, self.shade(base, 0.78), op))
            faces.append((right, self.shade(base, 0.58), op))
        xs = [p[0] for f in faces for p in f[0]]
        ys = [p[1] for f in faces for p in f[0]]
        bw, bh = max(xs) - min(xs), max(ys) - min(ys)
        sc = min(maxw / bw, maxh / bh)
        ox = cx - (min(xs) + bw / 2) * sc
        oy = cy - (min(ys) + bh / 2) * sc
        for pts, col, op in faces:
            t = [(ox + px * sc, oy + py * sc) for px, py in pts]
            s.poly(t, col, stroke="#0b0f13", sw=0.8, extra=f'fill-opacity="{op}"')
        return lambda X, Y, Z: (ox + P(X, Y, Z)[0] * sc, oy + P(X, Y, Z)[1] * sc)


def scene_works(iso: Iso):
    cas, ram, hop, pack = "#7b8794", "#ff8a1f", "#c5cdd6", "#5b6a79"
    iso.box(0, 0, 0, 3, 5, 2, cas)
    iso.box(0.5, 0.6, 2, 1.2, 1.2, 1.6, hop)
    iso.box(0.5, 3.2, 2, 1.2, 1.2, 1.6, hop)
    for i in range(4):
        iso.box(3 + 2 * i, 0, 0, 2, 5, 2, cas if i % 2 == 0 else "#6b7885")
        iso.box(3.5 + 2 * i, 1.5, 2, 1, 2, 1.5, ram)
    iso.box(3, 5, 0, 8, 0.4, 0.6, "#3fd0ff")           # belt
    iso.box(11, 0, 0, 3, 5, 3.6, pack)
    iso.box(12, 1.8, 3.6, 1, 1.4, 1.6, "#ffb020")


def scene_retort(iso: Iso):
    steel, cas, glass = "#8e9aa6", "#b04fd8", "#59b8ff"
    for (fx, fy) in ((0, 0), (4, 0), (0, 4), (4, 4)):
        iso.box(fx, fy, 0, 1, 1, 1.4, steel)
    iso.box(0.5, 0.5, 1.4, 4, 4, 0.8, cas)
    iso.box(0, 1, 2.2, 5, 3, 2.6, glass, 0.78)
    iso.box(1, 0, 2.2, 3, 5, 2.6, glass, 0.78)
    iso.box(1, 1, 4.8, 3, 3, 0.9, cas)
    iso.box(1.5, 1.5, 5.7, 2, 2, 1.8, glass, 0.78)
    iso.box(1.2, 1.2, 7.5, 2.6, 2.6, 0.7, cas)


def scene_gantry(iso: Iso):
    cas, leg, beam, hoist, cradle = "#7b8794", "#d8dee4", "#ffb020", "#ff8a1f", "#3fd0ff"
    iso.box(0, 0, 0, 2, 5, 3, cas)
    for i in range(4):
        x = 2.5 + 2.4 * i
        iso.box(x, 0, 0, 0.5, 0.5, 4, leg)
        iso.box(x, 4.5, 0, 0.5, 0.5, 4, leg)
        iso.box(x, 1.5, 0, 1.2, 2, 0.25, cradle)
        iso.box(x, 0, 4, 0.5, 5, 0.5, beam)
        iso.box(x, 2, 4.5, 0.5, 1, 1.4, hoist)
    iso.box(2.5, 0, 4.5, 9.2, 0.4, 0.4, beam)
    iso.box(12.0, 0, 0, 1, 5, 3, cas)


def scene_rampart(iso: Iso):
    wall, hatch, tur = "#8e9aa6", "#ff8a1f", "#3fd0ff"
    iso.box(0, 0, 0, 3, 3, 6, "#6b7885")
    for i in range(6):
        x = 3 + 2 * i
        iso.box(x, 0, 0, 1, 3, 5, wall)
        iso.box(x + 1, 0, 0, 1, 3, 3, wall)
        iso.box(x + 1.15, 0.9, 3, 0.7, 1.2, 0.35, hatch)
        iso.box(x + 1.3, 1.1, 3.35, 0.4, 0.8, 0.7, tur)
    iso.box(15, 0, 0, 1, 3, 5, wall)


# ---------------------------------------------------------------- infographic a: tier progression
TIER_ROWS = [
    ("ULV", "Bronze, wrought iron",
     ("Hand Cannon", ""),
     ("T1 Scout and T1 Miner sets", ""),
     ("Stone bullets", "")),
    ("LV", "Steel",
     ("9mm Pistol, Revolver, Sawed Off, Bolt Action Rifle; Combat Knife, Crowbar", ""),
     ("T1 Combat set", "Bandages"),
     ("9mm rounds and pistol magazines, shotgun shells, rifle bullets", "")),
    ("MV", "Aluminium",
     ("Thompson SMG, Mac10, Combat Shotgun, M4, AK47, akimbo pistols and revolvers; frag grenade, Flashbang*", ""),
     ("T2 Combat, Commando and Riot sets; gas mask, night-vision goggles, Medkit*", "Beret"),
     ("SMG and assault-rifle magazines, 40 mm grenades", "")),
    ("HV", "Stainless steel",
     ("AUG, SCAR, Vector, Infiltrator, AS50, LMG, Grenade Launcher, Rocket Launcher, Flamethrower, Chainsaw, Power Hammer; akimbo Mac10", ""),
     ("Jump pack, glider, oxygen mask and tanks", "Tactical Mask"),
     ("LMG and AS50 magazines, rockets, fuel tanks (GT fuels), compressed-air tanks", "High Caliber Rounds")),
    ("EV", "Titanium",
     ("Laser Rifle, PDW, Pulse Rifle, Minigun, Tesla Rifle, Sonic Rifle, Bio Gun, Power Hammer (Advanced)", ""),
     ("T3 Combat set, steam armour (no jetpack, dropped by 0009)", "HEV suit"),
     ("Advanced magazines, minigun drums, bio tanks; UV emitter component", "Advanced Bullets, Cybernetic Parts")),
    ("IV", "Tungstensteel",
     ("Blaster Rifle, De-Atomizer Pistol and its akimbo, Scatter Laser Rifle*, Golden Revolver and its akimbo", ""),
     ("Power armour, exo suit, anti-grav device", ""),
     ("none", "")),
    ("LuV", "HSS-G or LuV alloys",
     ("Nether Blaster, Alien Blaster, Grim Reaper", ""),
     ("none", ""),
     ("Nether charges", "")),
    ("ZPM", "Naquadah alloy",
     ("Nuclear Death Ray, Nuke Launcher", ""),
     ("none", ""),
     ("Nuclear power cells, tactical nuke rockets", "")),
]


def make_tier_progression() -> str:
    W = 1760
    cols = [(330, 560), (910, 400), (1330, 400)]  # x, width for weapons, armour, ammo
    size, lh = 14, 18.5
    rows_h = []
    for r in TIER_ROWS:
        n = 1
        for (x, w), cell in zip(cols, r[2:]):
            lines = len(wrap(cell[0], w - 8, size))
            if cell[1]:
                lines += len(wrap("+ " + cell[1], w - 8, size))
            n = max(n, lines)
        rows_h.append(max(72, n * lh + 26))
    top = 150
    H = int(top + sum(rows_h) + 8 * len(rows_h) + 100)
    s = Svg(W, H, bg=INK, radius=14)
    s.text(36, 54, "GregGuns tier progression", 32, TXT, weight="700")
    s.text(36, 84, "Every item sits at the GT tier of its final recipe machine AND needs a gate material that cannot be had earlier (docs/design/0001, amended by 0009 and 0010).", 15, MUTED)
    for x, t in ((330, "WEAPONS"), (910, "ARMOUR AND EQUIPMENT"), (1330, "AMMO AND CONSUMABLES"), (36, "TIER AND GATE MATERIAL")):
        s.text(x, 128, t, 13, MUTED, weight="700", extra='letter-spacing="1.5"')
    y = top - 6
    for r, h in zip(TIER_ROWS, rows_h):
        key, gate = r[0], r[1]
        col = TIER[key][3]
        s.rect(24, y, W - 48, h, PANEL, rx=10)
        s.rect(24, y, 8, h, col, rx=3)
        tier_badge(s, 46, y + 12, key, 108, 30, 15)
        s.text(46 + 120, y + 32, TIER[key][2], 12.5, MUTED)
        s.text(46, y + 64, "Gate: " + gate, 14, TXT, weight="600")
        for (x, w), cell in zip(cols, r[2:]):
            ty = y + 28
            main = cell[0]
            if main == "none":
                s.text(x, ty, "none", size, "#6c7a88", extra='font-style="italic"')
                continue
            for line in wrap(main, w - 8, size):
                s.text(x, ty, line, size, TXT)
                ty += lh
            if cell[1]:
                for line in wrap("+ " + cell[1], w - 8, size):
                    s.text(x, ty, line, size, ORANGE, weight="600")
                    ty += lh
        y += h + 8
    ly = y + 14
    s.rect(24, ly, W - 48, 74, PANEL2, rx=10)
    s.text(46, ly + 28, "*  Unfinished in Techguns, built as a finished design here: Flashbang (MV), Medkit (MV), Scatter Laser Rifle (IV).", 14, TXT)
    s.text(46, ly + 54, "+  Tier added by record 0010 (orange). Dropped: the EV jetpack (GT Mech Armor has an HV jetpack augment, 0009) and the Scrapper (0002).", 14, MUTED)
    tier_hex_strip(s, W - 40 - 8 * 28, ly + 22, 11, gap=3)
    s.text(W - 40, ly + 62, "Tier colours follow GT voltage names", 12, MUTED, "end")
    return s.render()


# ---------------------------------------------------------------- infographic b: machine roster
ROSTER = [
    dict(single="Ammo Press", sk="Single block, animated", st=["ULV", "LV", "MV", "HV", "EV"], cls="Bottleneck",
         sdet="Own recipe map gregguns.ammoPress. IDs 20100-20104",
         multi="Ammunition Works", mk="Multiblock, 5 x 7 x 19 (grows by press bays, 2 to 8)", mt=["HV", "EV", "IV"], mcls="Bottleneck",
         mdet="Controller HV, hatches LV to IV. Belt tier sets speed. ID 20105", scene=scene_works, ctrl="HV"),
    dict(single="Reaction Chamber", sk="Single block", st=["EV", "IV"], cls="Bottleneck",
         sdet="Laser Focus and Anti Gravity Core, the only source of both. IDs 20106-20107",
         multi="Retort", mk="Multiblock, 5 x 10 x 5 (taller by bulb layers, 2 to 6)", mt=["EV", "IV", "LuV", "ZPM"], mcls="Bottleneck",
         mdet="Controller EV, hatches EV to ZPM. Glass tier is the pressure rating. ID 20108", scene=scene_retort, ctrl="EV"),
    dict(single="Repair Bench", sk="Single block", st=["LV", "MV", "HV", "EV", "IV"], cls="Regular",
         sdet="Mends armour and guns with GT materials; bench tier at least the item tier. IDs 20109-20113",
         multi="Armoury Gantry", mk="Multiblock, 7 x 7 x 11 (more bays, 2 to 8)", mt=["HV", "EV", "IV", "LuV", "ZPM"], mcls="Regular",
         mdet="Controller HV, hatches up to ZPM; lowest hatch tier is the top item tier it repairs. ID 20114", scene=scene_gantry, ctrl="HV"),
    dict(single="Turret", sk="Single block", st=["MV", "HV", "EV", "IV"], cls="Regular",
         sdet="One gun on a platform, 30 HP turret entity; turret tier at least the gun tier. IDs 20116-20119",
         multi="Rampart", mk="Multiblock, 16 x 6 x 3 (more mounts, 2 to 10)", mt=["MV", "HV", "EV", "IV"], mcls="Regular",
         mdet="Controller HV; each mount is a Gun Mount Hatch, MV to IV. IDs 20120-20124", scene=scene_rampart, ctrl="HV"),
]
CLS_COL = {"Bottleneck": ORANGE, "Regular": CYAN}


def tier_ruler(s: Svg, x, y, lit: list[str], sz=22, gap=4, ctrl: str | None = None):
    cw = sz + 12
    for i, t in enumerate(TIERS):
        fill = t[3] if t[0] in lit else "#2a3541"
        tc = t[4] if t[0] in lit else "#566473"
        xx = x + i * (cw + gap)
        s.rect(xx, y, cw, sz, fill, rx=4, stroke=("#ffffff" if ctrl == t[0] else None), sw=2)
        s.text(xx + cw / 2, y + sz / 2 + 3.8, t[0], 10.5 * sz / 22, tc, "middle", "700")


def make_machine_roster() -> str:
    W, H = 1760, 1290
    s = Svg(W, H, bg=INK, radius=14)
    s.text(36, 54, "GregGuns machine roster", 32, TXT, weight="700")
    s.text(36, 84, "Policy: every machine is a Techguns-style single block, and every machine used regularly also gets a multiblock for scale (docs/gregguns-spec/08).", 15, MUTED)
    lx = 36
    for k, c in CLS_COL.items():
        lx += chip(s, lx, 104, k, c, INK, 12.5) + 10
    s.text(lx + 6, 121, "Bottleneck = gates progression, multiblock mandatory.  Regular = used every session.", 13, MUTED)
    y0 = 160
    rhs = [200, 280, 200, 236]
    s.text(48, y0 - 2, "SINGLE BLOCK", 12.5, MUTED, weight="700", extra='letter-spacing="1.5"')
    s.text(712, y0 - 2, "MULTIBLOCK", 12.5, MUTED, weight="700", extra='letter-spacing="1.5"')
    s.text(1360, y0 - 2, "SKETCH (isometric, not to scale)", 12.5, MUTED, weight="700", extra='letter-spacing="1.5"')
    for i, m in enumerate(ROSTER):
        rh = rhs[i]
        y = y0 + 10 + sum(rhs[:i]) + i * 10
        s.rect(24, y, W - 48, rh, PANEL, rx=12)
        s.rect(40, y + 14, 590, rh - 28, PANEL2, rx=10)
        s.text(60, y + 50, m["single"], 25, TXT, weight="700")
        chip(s, 60 + len(m["single"]) * 14.5 + 20, y + 30, m["cls"], CLS_COL[m["cls"]], INK, 12)
        s.text(60, y + 74, m["sk"], 14, MUTED)
        tier_ruler(s, 60, y + 92, m["st"])
        s.para(60, y + 142, m["sdet"], 540, 13.5, MUTED)
        s.arrow(636, y + rh / 2, 684, y + rh / 2, ORANGE, 4)
        s.rect(692, y + 14, 640, rh - 28, PANEL2, rx=10)
        s.text(712, y + 50, m["multi"], 25, TXT, weight="700")
        chip(s, 712 + len(m["multi"]) * 14.5 + 20, y + 30, m["mcls"], CLS_COL[m["mcls"]], INK, 12)
        s.text(712, y + 74, m["mk"], 14, MUTED)
        tier_ruler(s, 712, y + 92, m["mt"], ctrl=m["ctrl"])
        s.para(712, y + 142, m["mdet"], 600, 13.5, MUTED)
        iso = Iso(u=14)
        m["scene"](iso)
        s.rect(1348, y + 14, 376, rh - 28, "#121920", rx=10)
        iso.draw(s, 1536, y + rh / 2 + 2, 352, rh - 40)
    by = y0 + 10 + sum(rhs) + len(ROSTER) * 10
    s.rect(24, by, W - 48, 120, PANEL, rx=12)
    s.text(48, by + 34, "No multiblock, by design", 18, TXT, weight="700")
    items = [
        ("Camo Bench", "LV single block. Cosmetic: one click per item, a bigger block would not be faster (ID 20115)."),
        ("Explosive charges", "MV and HV items placed and fired once. Blocks with a tile entity, not MTEs."),
        ("Weapon and Reinforced Chests", "Decorative storage, no process, no tier. Not MTEs."),
        ("Gun Mount Hatch", "Part of the Rampart, MV to IV (IDs 20121-20124). A hatch is not a machine."),
    ]
    xx = 48
    for t, d in items:
        s.text(xx, by + 62, t, 14, ORANGE, weight="700")
        s.para(xx, by + 82, d, 400, 12.5, MUTED, lh=1.25)
        xx += 428
    s.text(36, H - 16, "Lit chips = tiers the machine (or its hatches) supports; white outline = controller tier. MTE ID block 20100-20163 is a proposal pending GTNH team agreement.", 12.5, MUTED)
    return s.render()


# ---------------------------------------------------------------- infographic c: ammo production flow
def make_ammo_flow() -> str:
    W, H = 1800, 1110
    s = Svg(W, H, bg=INK, radius=14)
    s.text(36, 54, "From GT raw materials to a loaded gun", 32, TXT, weight="700")
    s.text(36, 84, "Ammo is the highest-volume consumable, so it is the one place GregGuns adds its own machine and recipe map (gregguns.ammoPress). Everything upstream is plain GregTech.", 15, MUTED)
    colx = [30, 405, 800, 1245, 1570]
    colw = [310, 340, 400, 270, 200]
    heads = ["1  GT RAW MATERIALS", "2  GT PARTS", "3  AMMO PRESS OR AMMUNITION WORKS", "4  AMMO AND MAGAZINES", "5  GUNS"]
    for x, t in zip(colx, heads):
        s.text(x, 132, t, 13, MUTED, weight="700", extra='letter-spacing="1.5"')
    top = 150
    colH = 520
    for x, w in zip(colx, colw):
        s.rect(x - 6, top - 6, w + 12, colH, PANEL, rx=12)

    mats = [("Brass (Cu + Zn)", "#d9a441", "LV ammo casings"), ("Lead", "#7f8fa6", "LV bullets, shells"),
            ("Steel", TIER["LV"][3], "LV rifle bullets, magazines"), ("Aluminium", TIER["MV"][3], "MV grenades, magazines"),
            ("Stainless steel", TIER["HV"][3], "HV rounds, rockets"), ("Titanium", TIER["EV"][3], "EV advanced ammo"),
            ("Gunpowder, TNT dust", "#9aa3ad", "every propellant"), ("Bronze, cobblestone", TIER["ULV"][3], "Stone bullets (Steam)")]
    y = top + 14
    for n, c, d in mats:
        s.rect(colx[0] + 10, y, colw[0] - 20, 52, PANEL2, rx=8)
        s.rect(colx[0] + 10, y, 8, 52, c, rx=3)
        s.text(colx[0] + 30, y + 22, n, 15, TXT, weight="600")
        s.text(colx[0] + 30, y + 41, d, 12.5, MUTED)
        y += 60
    parts = [("round.<Material>", "Bullet slug, 1/9 ingot", "Lathe, from a nugget"), ("foil.<Material>", "Case, 1/4 ingot", "Bending Machine"),
             ("itemCasing.<Material>", "Large case, 1/2 ingot (AS50)", "Cutting Machine, Extruder, Alloy Smelter"),
             ("dust.Gunpowder / dust.TNT", "Vanilla gunpowder / TNT dust", "GT dust recipes"),
             ("plate + springSmall", "Magazine bodies and springs", "GT plate and spring recipes"),
             ("part_mechanical_2 / _3", "Feed mechanism part", "Assembler (RC-11)")]
    y = top + 14
    for n, d, mch in parts:
        s.rect(colx[1] + 10, y, colw[1] - 20, 66, PANEL2, rx=8)
        s.text(colx[1] + 26, y + 26, n, 14.5, CYAN, weight="600", family=MONO)
        s.text(colx[1] + 26, y + 46, d, 13, TXT)
        s.text(colx[1] + 26, y + 61, "made by: " + mch, 11, MUTED)
        y += 72
    s.text(colx[1] + 12, y + 6, "Made by GT's own recipes.", 12, MUTED)
    s.text(colx[1] + 12, y + 22, "Ammo itself: GregGuns Ammo Press", 12, MUTED)
    s.text(colx[1] + 12, y + 38, "(gregguns.ammoPress, per 0011).", 12, MUTED)

    x3, w3 = colx[2], colw[2]
    s.rect(x3 + 10, top + 14, w3 - 20, 200, PANEL2, rx=10, stroke=ORANGE, sw=2)
    s.text(x3 + 28, top + 48, "Ammo Press", 24, TXT, weight="700")
    s.text(x3 + 28, top + 70, "Single block, animated ram and belt", 13.5, MUTED)
    tier_ruler(s, x3 + 28, top + 86, ["ULV", "LV", "MV", "HV", "EV"], sz=16, gap=1)
    s.rect(x3 + 28, top + 134, 150, 58, "#394755", rx=4)
    s.rect(x3 + 70, top + 116, 24, 34, "#c5cdd6", rx=2)
    s.rect(x3 + 64, top + 148, 36, 8, ORANGE, rx=2)
    for i in range(4):
        s.rect(x3 + 114 + i * 16, top + 162, 10, 14, "#d9a441", rx=2)
    s.text(x3 + 196, top + 150, "A programmed circuit", 12.5, MUTED)
    s.text(x3 + 196, top + 166, "is only a selector;", 12.5, MUTED)
    s.text(x3 + 196, top + 182, "the map is separate.", 12.5, MUTED)
    s.rect(x3 + 10, top + 232, w3 - 20, 200, PANEL2, rx=10, stroke=BLUE, sw=2)
    s.text(x3 + 28, top + 266, "Ammunition Works", 24, TXT, weight="700")
    s.text(x3 + 28, top + 288, "Multiblock: feeder, press bays, packing station", 13.5, MUTED)
    tier_ruler(s, x3 + 28, top + 304, ["HV", "EV", "IV"], sz=16, gap=1, ctrl="HV")
    iso = Iso(u=9)
    scene_works(iso)
    iso.draw(s, x3 + 112, top + 385, 190, 70)
    s.text(x3 + 232, top + 366, "High parallels.", 12.5, TXT, weight="600")
    s.text(x3 + 232, top + 384, "Add press bays", 12.5, MUTED)
    s.text(x3 + 232, top + 400, "(2 to 8) to scale.", 12.5, MUTED)
    s.text(x3 + 232, top + 422, "Belt tier = speed.", 12.5, MUTED)
    s.rect(x3 + 10, top + 444, w3 - 20, 58, "#121920", rx=8)
    s.text(x3 + w3 / 2, top + 468, "Same recipe map: gregguns.ammoPress", 14, ORANGE, "middle", "700", MONO)
    s.text(x3 + w3 / 2, top + 488, "Voltage = the ammo's tier. Not in the Assembler map.", 12, MUTED, "middle")

    outs = [("Loose ammo", "9mm, shotgun shell, rifle bullet,", "40 mm grenade, rocket, high-calibre,", "advanced bullet, stone bullet", "#d9a441"),
            ("Empty magazines", "pistol, SMG, assault rifle, AS50,", "LMG, advanced, minigun drum", "", "#8e9aa6"),
            ("Filled magazines", "empty magazine + loose rounds", "(hand refill at a crafting table)", "", ORANGE)]
    y = top + 14
    for n, a, b, c, col in outs:
        s.rect(colx[3] + 10, y, colw[3] - 20, 110, PANEL2, rx=10)
        s.rect(colx[3] + 10, y, 8, 110, col, rx=3)
        s.text(colx[3] + 32, y + 30, n, 17, TXT, weight="700")
        s.text(colx[3] + 32, y + 56, a, 12, MUTED)
        s.text(colx[3] + 32, y + 72, b, 12, MUTED)
        if c:
            s.text(colx[3] + 32, y + 88, c, 12, MUTED)
        y += 140
    for k in range(2):
        yy = top + 14 + 110 + k * 140
        s.arrow(colx[3] + colw[3] / 2, yy + 2, colx[3] + colw[3] / 2, yy + 28, MUTED, 2)
    s.text(colx[3] + 12, top + 14 + 3 * 140 - 8, "Three-step chain: bullets,", 12.5, MUTED)
    s.text(colx[3] + 12, top + 14 + 3 * 140 + 9, "empty magazine, filled magazine.", 12.5, MUTED)

    guns = [("Hand Cannon", "ULV", "Stone bullet"), ("9mm Pistol, SMGs", "LV", "9mm, pistol/SMG mags"), ("Shotguns, Bolt Rifle", "LV", "Shells, rifle bullets"),
            ("M4, AK47, Grenade L.", "MV", "AR mags, 40 mm"), ("AS50, LMG, Rocket L.", "HV", "HC rounds, rockets"), ("PDW, Minigun", "EV", "Advanced mags, drums")]
    y = top + 14
    for n, t, d in guns:
        s.rect(colx[4] + 10, y, colw[4] - 20, 70, PANEL2, rx=8)
        tier_badge(s, colx[4] + 20, y + 8, t, 52, 20, 11.5)
        s.text(colx[4] + 20, y + 42, n, 13, TXT, weight="600")
        s.text(colx[4] + 20, y + 58, d, 11, MUTED)
        y += 80
    for i in range(4):
        x1 = colx[i] + colw[i] + 6
        x2 = colx[i + 1] - 6
        s.arrow(x1, top + 250, x2, top + 250, ORANGE, 4)

    ey = top + colH + 24
    s.rect(24, ey, W - 48, 232, PANEL, rx=12)
    s.text(48, ey + 34, "Three real recipes (docs/gregguns-spec/06-items/recipes.md: RC-28, RC-37, RC-44)", 17, TXT, weight="700")
    ex = [("RC-28  9mm Bullets", "16 foil.Brass + 16 round.Lead + 2 dust.Gunpowder", "16 x 9mm Bullets", "LV, 30 EU/t, 5 s", "LV"),
          ("RC-37  Empty pistol magazine", "2 plate.Steel + 8 springSmall.Steel + 1 part_mechanical_2", "8 x empty 9mm magazines", "LV, 30 EU/t, 4 s", "LV"),
          ("RC-44  Filled pistol magazine", "4 empty 9mm magazines + 12 9mm Bullets", "4 x 9mm Pistol Magazines", "LV, 30 EU/t, 2 s", "LV")]
    cx = 48
    for t, inp, out, meta, tk in ex:
        s.rect(cx, ey + 52, 540, 164, PANEL2, rx=10)
        s.text(cx + 18, ey + 82, t, 15, ORANGE, weight="700")
        s.para(cx + 18, ey + 108, inp, 500, 13.5, TXT, family=MONO)
        s.arrow(cx + 270, ey + 128, cx + 270, ey + 154, MUTED, 2)
        s.text(cx + 18, ey + 176, out, 16, CYAN, weight="700")
        tier_badge(s, cx + 18, ey + 186, tk, 44, 20, 11.5)
        s.text(cx + 70, ey + 201, meta, 12.5, MUTED)
        cx += 560
    by = ey + 252
    s.rect(24, by, W - 48, 150, PANEL, rx=12)
    s.text(48, by + 32, "Ammo by tier", 17, TXT, weight="700")
    ladder = [("ULV", "Stone bullet"), ("LV", "9mm, shell, rifle bullet, pistol magazine"), ("MV", "40 mm grenade, SMG and assault-rifle magazines"),
              ("HV", "High-calibre round, rocket, AS50 and LMG magazines"), ("EV", "Advanced bullet, advanced magazine, minigun drum")]
    lx = 48
    for k, d in ladder:
        tier_badge(s, lx, by + 48, k, 70, 24, 13)
        s.para(lx, by + 94, d, 240, 12.5, MUTED, lh=1.25)
        lx += 272
    s.rect(1420, by + 40, 330, 96, "#121920", rx=8)
    s.text(1436, by + 64, "Outside the Press", 13, ORANGE, weight="700")
    for i, t in enumerate(("Nether Charge (LuV) and nuke rocket (ZPM):", "Assembly Line, one-off crafts.",
                           "Fuel, bio and air tanks: Assembler + Fluid Canner.")):
        s.text(1436, by + 84 + i * 16, t, 12, MUTED)
    return s.render()


# ---------------------------------------------------------------- infographic d: questbook
MAIN_PATH = [
    ("Start here", "ULV", 5, "11 Monster Hunter or 44 Welcome to Tier 0.5, Steam! (either one)"),
    ("Steam / ULV", "ULV", 7, "44 Welcome to Tier 0.5, Steam! + 948 Better Than Iron"),
    ("LV", "LV", 11, "88 Welcome to Tier 1, LV + 76 Time to Get Some Steel"),
    ("MV", "MV", 10, "1148 Tier 2 (MV) + 1598 MV assembler"),
    ("HV", "HV", 9, "160 Tier 3 (HV) + 753 Stainless Steel EBF processing"),
    ("EV", "EV", 9, "176 Tier 4 (EV) + 860 Titanium, Finally"),
    ("IV", "IV", 9, "213 Tier 5 (IV) + 861 Tungstensteel"),
    ("LuV", "LuV", 6, "1493 Tier 6 (LuV) + 1491 HSS-G + 1412 Assembly Line"),
    ("ZPM", "ZPM", 5, "2610 Tier 7 (ZPM) + 1667 Your First Naquadah Ingot"),
]
BRANCHES = [
    ("NPCs and bosses", 14, "Pack 11 (Monster Hunter), then each rank by its tier quest", "Factions, loot, Infernal ranks; 3 boss quests are SECRET"),
    ("Structures", 6, "Pack 11 (Monster Hunter), then tier quests", "Where tiered chests are; all checkbox quests"),
    ("Machines", 9, "Pack 97 (Avengers, Assemble!, the LV assembler), then a tier quest per machine", "Ammo Press, Reaction Chamber, Repair Bench, Turret"),
    ("Armour and equipment", 9, "Pack 88 (Welcome to Tier 1, LV), then tier quests", "Baubles slots, hazard and oxygen gear, back devices"),
    ("Ammo logistics", 9, "Pack 88 (Welcome to Tier 1, LV), then tier quests", "Ammo as a production line; exact-tier charging"),
]


def make_questbook() -> str:
    W, H = 1760, 1130
    s = Svg(W, H, bg=INK, radius=14)
    s.text(36, 54, "GregGuns questbook: 118 quests, 14 chapters", 32, TXT, weight="700")
    s.text(36, 84, "One BetterQuesting tab appended after Kill All the Things. It gates on GTNH progression quests, never on its own chapter order (docs/gregguns-spec/12-questbook).", 15, MUTED)
    s.text(48, 134, "GTNH PACK GATE QUESTS (existing)", 13, MUTED, weight="700", extra='letter-spacing="1.5"')
    s.text(600, 134, "GREGGUNS MAIN PATH (quests per chapter)", 13, MUTED, weight="700", extra='letter-spacing="1.5"')
    s.text(1210, 134, "SIDE BRANCHES (one early gate, then per tier)", 13, MUTED, weight="700", extra='letter-spacing="1.5"')
    y = 150
    rh = 78
    maxq = 11
    for i, (name, tk, n, gate) in enumerate(MAIN_PATH):
        col = TIER[tk][3]
        yy = y + i * (rh + 8)
        s.rect(30, yy, 520, rh, PANEL, rx=10, stroke=LINE, sw=1.5, extra='stroke-dasharray="6 4"')
        s.text(48, yy + 28, "PACK QUESTS", 11, MUTED, weight="700", extra='letter-spacing="1.2"')
        s.para(48, yy + 50, gate, 480, 14, TXT, lh=1.25)
        s.arrow(552, yy + rh / 2, 596, yy + rh / 2, col, 3.5)
        s.rect(600, yy, 560, rh, PANEL2, rx=10)
        s.rect(600, yy, 12, rh, col, rx=4)
        s.text(630, yy + 32, name, 22, TXT, weight="700")
        if i > 0:
            tier_badge(s, 630, yy + 44, tk, 70, 22, 12.5)
        else:
            s.text(630, yy + 62, "orientation, no rewards", 13, MUTED)
        bw = 280 * n / maxq
        s.rect(790, yy + 46, 280, 16, "#121920", rx=8)
        s.rect(790, yy + 46, bw, 16, col, rx=8)
        s.text(1082, yy + 59, f"{n} quests", 14, TXT, weight="600")
        if i < len(MAIN_PATH) - 1:
            s.arrow(880, yy + rh, 880, yy + rh + 8, MUTED, 2.5)
    bx, bw0 = 1210, 520
    for i, (name, n, gate, teach) in enumerate(BRANCHES):
        yy = y + i * 128
        s.rect(bx, yy, bw0, 118, PANEL2, rx=10)
        s.rect(bx, yy, 12, 118, CYAN, rx=4)
        s.text(bx + 30, yy + 32, name, 20, TXT, weight="700")
        chip(s, bx + bw0 - 106, yy + 14, f"{n} quests", CYAN, INK, 12.5)
        s.para(bx + 30, yy + 56, "Gate: " + gate, 470, 13.5, ORANGE, lh=1.25)
        s.para(bx + 30, yy + 100, teach, 470, 13, MUTED, lh=1.25)
    s.arrow(1164, y + 40, 1204, y + 40, CYAN, 3)
    s.text(bx, y + 5 * 128 + 12, "5 branches = 47 quests; 9 main chapters = 71.", 13, MUTED)
    s.text(bx, y + 5 * 128 + 32, "Every chapter also requires gg.start.welcome.", 13, MUTED)
    s.text(bx, y + 5 * 128 + 52, "Tab visibility UNLOCKED; icon gregguns:pistol_9mm.", 13, MUTED)
    fy = y + 9 * (rh + 8) + 10
    s.rect(24, fy, W - 48, 118, PANEL, rx=12)
    facts = [
        ("Tier gates are pack quests", "Each tier chapter needs the pack's tier quest AND its gate-material quest. Steel (76) and wrought iron (948) are not implied by the tier quest, so they are listed."),
        ("Rewards never outrun the gate", "A quest pays nothing above its own tier. Tier bags (EnhancedLootBags): Steam 2, LV 4, MV 5, HV 6, EV 7, IV 8, LuV 41, ZPM 42."),
        ("26 bag quests, 45 checkbox quests", "Bags: Steam 2, LV 6, MV 7, HV 3, EV 4, IV 2, LuV 1, ZPM 1. Machine-made items use retrieval tasks; crafting tasks only in the Steam chapter."),
        ("One mechanic per quest", "Reloading, filling a magazine, pressing a part, charging at the right tier, mending a weapon, wearing a Baubles item. No command rewards."),
    ]
    fx = 48
    for t, d in facts:
        s.text(fx, fy + 36, t, 15, ORANGE, weight="700")
        s.para(fx, fy + 60, d, 380, 12.5, MUTED, lh=1.3)
        fx += 420
    return s.render()


# ---------------------------------------------------------------- multiblock explainers
# Layer diagrams are copied from docs/gregguns-spec/08-machines-production/multiblocks.md
# (layers left to right as in the spec; rows run front to back).
CELL_COL = {
    "~": ("#ffffff", "Controller"),
}


def layer_grid(s: Svg, x, y, rows, cmap, c=11):
    """One layer as a small top-down grid. '.' = anything (small dot), '-' = air (dark)."""
    for r, row in enumerate(rows):
        for q, ch in enumerate(row):
            cx, cy = x + q * c, y + r * c
            if ch == ".":
                s.rect(cx + 4, cy + 4, c - 8, c - 8, "#2c3743", rx=1)
            elif ch == "-":
                s.rect(cx + 0.5, cy + 0.5, c - 1, c - 1, "#0d1217", rx=1, stroke="#2c3743", sw=0.8)
            elif ch == "~":
                s.rect(cx + 0.5, cy + 0.5, c - 1, c - 1, "#ffffff", rx=2, stroke=ORANGE, sw=2)
            else:
                s.rect(cx + 0.5, cy + 0.5, c - 1, c - 1, cmap[ch][0], rx=1)


MB = {
    "works": dict(
        title="Ammunition Works", cls="Bottleneck", id="MTE 20105", scene=scene_works,
        tiers=["LV", "MV", "HV", "EV", "IV"], ctrl="HV",
        sub="Multiblock form of the Ammo Press. A production hall: feeder, a run of press bays, a packing station. Reads the same gregguns.ammoPress map.",
        dims=[("Footprint", "5 wide x 7 tall x (7 + 3n) long"), ("Default n = 4", "5 x 7 x 19"),
              ("Layers", "feeder 6, press bay 6, packing station 7"), ("Blocks", "feeder 18 F, 40 A, 2 I; each bay 44 A, 2 M, 3 I; station 16 O, 67 A, 3 I")],
        legend={"F": ("#7b8794", "Works Casing, feeder (hatches)"), "A": ("#4c5865", "Works Casing, plain"), "O": ("#c5cdd6", "Works Casing, packing (output)"),
                "M": (ORANGE, "Ram Housing (animated)"), "I": (CYAN, "GT item pipe casing (belt, any tier)")},
        pieces=[("Feeder (3 long, once)", [("y5", ["F...F", ".....", "F...F"]), ("y4", ["AAAAA"] * 3), ("y3", ["FFFFF", "A---A", "A---A"]),
                                            ("y2", ["FFFFF", "A---A", "A---A"]), ("y1", ["FF~FF", "A---A", "A---A"]), ("y0", ["AAAAA", "AAIAA", "AAIAA"])]),
                ("Press bay (3 long, repeat n = 2 to 8)", [("y5", [".....", "..M..", "....."]), ("y4", ["AAAAA", "AAMAA", "AAAAA"]), ("y3", ["A---A"] * 3),
                                                           ("y2", ["A---A"] * 3), ("y1", ["A---A"] * 3), ("y0", ["AAIAA"] * 3)]),
                ("Packing station (4 long, once)", [("y6", [".....", "..O..", "..O..", "....."]), ("y5", ["AAAAA", "AAOAA", "AAOAA", "AAAAA"]),
                                                    ("y4", ["A---A", "A---A", "A---A", "AOOOA"]), ("y3", ["A---A", "A---A", "A---A", "AOOOA"]),
                                                    ("y2", ["A---A", "A---A", "A---A", "AOOOA"]), ("y1", ["A---A", "A---A", "A---A", "AOOOA"]),
                                                    ("y0", ["AAIAA", "AAIAA", "AAIAA", "AAAAA"])])],
        hatches=["Controller: front of the feeder, middle, second layer from the bottom (the ~ cell).",
                 "Input Bus (1+): on any F; hopper tops are the intended spot. Input Hatch 0+.",
                 "Energy, Maintenance and Muffler (one each, Muffler required): any F.",
                 "Output Bus (1+): on the O cells of the back wall of the packing station.",
                 "Belt: one tier of GT item pipe casing for the whole I line; mixed tiers fail."],
        grow=["Add press bays: n = 2 to 8, each 3 blocks long (+3 length).",
              "Hologram stack size selects n; survival build places feeder, bays in order, station.",
              "A structure with fewer than 2 or more than 8 bays fails with a named error."],
        mech=["Parallels = 4 x n x tier (tier of the max input voltage). HV: 48 at n = 4, 96 at n = 8. Capped by the power the Energy Hatches supply.",
              "Belt speed: duration x 1 / (1 + 0.25 x (beltTier + 1)).",
              "Worked example (proposal): HV, 8 bays, belt tier 2: 256 rifle bullets per 3.4 s, about 4,500 a minute against 160 a minute from one LV Ammo Press.",
              "Needs Energy, Maintenance, Muffler (40 pollution/s proposal), Input Bus, Output Bus."],
    ),
    "retort": dict(
        title="Retort", cls="Bottleneck", id="MTE 20108", scene=scene_retort,
        tiers=["EV", "IV", "LuV", "ZPM"], ctrl="EV",
        sub="Multiblock form of the Reaction Chamber: a glass lab retort on stilts. Reads the gregguns.recipe.reactionchamber map.",
        dims=[("Footprint", "5 wide x (7 + n) tall x 5 deep"), ("Default n = 3", "5 x 10 x 5"),
              ("Layers", "7 fixed (cap, 2 neck, shoulder, low ring, floor, feet) + n bulb layers"), ("Blocks", "R 61, G 16 + 12n, F 4; casing minimum 51, independent of n")],
        legend={"R": ("#b04fd8", "Reaction Casing (hatches)"), "G": ("#59b8ff", "GT borosilicate glass, any tier"), "F": ("#8e9aa6", "frameGt Steel (feet)")},
        pieces=[("Layers, top to bottom (mid layer repeats n = 2 to 6 times)", [
            ("cap", [".....", ".RRR.", ".RRR.", ".RRR.", "....."]), ("neck", [".....", ".GGG.", ".G-G.", ".GGG.", "....."]),
            ("neck", [".....", ".GGG.", ".G-G.", ".GGG.", "....."]), ("shoulder", [".RRR.", "RRRRR", "RR-RR", "RRRRR", ".RRR."]),
            ("mid x n", [".GGG.", "G---G", "G---G", "G---G", ".GGG."]), ("low ring", [".RRR.", "R---R", "R---R", "R---R", ".RRR."]),
            ("floor", [".R~R.", "RRRRR", "RRRRR", "RRRRR", ".RRR."]), ("feet", [".....", ".F.F.", ".....", ".F.F.", "....."])])],
        hatches=["Controller: front, middle of the floor layer (the ~ cell).",
                 "Input Bus (holds the non-consumed Heat Ray Focus and inputs), Output Bus, Input Hatch, Energy, Maintenance: on R in the floor and low ring.",
                 "Muffler Hatch: on the cap.",
                 "All hatches are GT's. Needs Energy, Maintenance, Muffler, Input Bus, Output Bus, Input Hatch."],
        grow=["Add bulb layers: n = 2 to 6, each 1 block taller; the shape stays a bulb and neck.",
              "Pieces: base (low ring, floor, feet), bulb x n, top (cap, neck, shoulder).",
              "Hologram stack size selects n; survival build places base, bulbs, then top."],
        mech=["Parallels = 2 x n (4 to 12), capped by power: six Laser Focus runs need 11,520 EU/t.",
              "Pressure rating: the lowest glass tier must be at least the recipe tier (Laser Focus EV, Anti Gravity Core IV), else insufficientMachineTier.",
              "One shared intensity for the batch: a failed batch applies its risk once, but still loses all its inputs. A bigger batch is a bigger gamble.",
              "Same focus, drift and completion rules as the single Reaction Chamber."],
    ),
    "gantry": dict(
        title="Armoury Gantry", cls="Regular", id="MTE 20114", scene=scene_gantry,
        tiers=["HV", "EV", "IV", "LuV", "ZPM"], ctrl="HV",
        sub="Multiblock form of the Repair Bench: a row of portal-frame repair bays. No recipe map; repair cost is read from item NBT.",
        dims=[("Footprint", "7 wide x 7 tall x (3 + 2n) long"), ("Default n = 4", "7 x 7 x 11"),
              ("Layers", "7 tall (y0 to y6); head 2 long, bay 2 long, tail 1 long"), ("Blocks", "head B 14, L 20, G 10, R 19, H 1; bay B 14, L 10, R 6, T 6, H 1")],
        legend={"R": ("#7b8794", "Gantry Casing (hatches)"), "B": (TIER["HV"][3], "Gantry Beam"), "H": (ORANGE, "Gantry Hoist"), "L": ("#d8dee4", "frameGt Titanium (legs)"),
                "G": ("#59b8ff", "GT borosilicate glass"), "T": (CYAN, "Repair Cradle (one per bay)")},
        pieces=[("Head (2 long, booth)", [("y6", ["...H...", "......."]), ("y5", ["BBBBBBB"] * 2), ("y4", ["LGGGGGL", "L-----L"]), ("y3", ["LGGGGGL", "L-----L"]),
                                          ("y2", ["LRR~RRL", "L-----L"]), ("y1", ["LRRRRRL", "L-----L"]), ("y0", ["LRRRRRL", "LRRRRRL"])]),
                ("Bay (2 long, repeat n = 2 to 8)", [("y6", ["...H...", "......."]), ("y5", ["BBBBBBB"] * 2), ("y4", ["L-----L", "-------"]), ("y3", ["L-----L", "-------"]),
                                                     ("y2", ["L-----L", "-------"]), ("y1", ["L-----L", "-------"]), ("y0", ["LRTTTRL", "RRTTTRR"])]),
                ("Tail (1 long, once)", [("y6", ["......."]), ("y5", ["BBBBBBB"]), ("y4", ["LRRRRRL"]), ("y3", ["LRRRRRL"]), ("y2", ["LRRRRRL"]), ("y1", ["LRRRRRL"]), ("y0", ["LRRRRRL"])])],
        hatches=["Controller: in the front wall of the head booth, middle height (the ~ cell).",
                 "Input Bus (1+), Output Bus (1+), Energy (1+), Maintenance (1): on R in the head and tail walls and floors.",
                 "No Muffler and no pollution: repairing makes none."],
        grow=["Add repair bays: n = 2 to 8, each 2 blocks long (+2 length), one cradle and one hoist per bay.",
              "Hologram stack size selects n. Default 4 bays; 8 bays clear a locker in one cycle."],
        mech=["Parallels = n bays: each repairs one item per cycle, scanned from the Input Bus in order.",
              "Tier gate: the lowest Energy Hatch tier is the highest item tier it repairs; a LuV or ZPM item needs a LuV or ZPM hatch.",
              "Cycle = 2 s + 8 s x largest damage fraction; draws RECIPE_<tier>/2 x items repaired (proposal).",
              "Each item is all-or-nothing against its repair list. Output keeps every NBT key (enchants, camo, IC2 charge); GT tools are never touched."],
    ),
    "rampart": dict(
        title="Rampart", cls="Regular", id="MTE 20120", scene=scene_rampart,
        tiers=["MV", "HV", "EV", "IV"], ctrl="HV",
        sub="Multiblock form of the Turret: a crenellated wall of turret mounts on one ammo feed and one power feed. No recipe; it sets its own EU draw.",
        dims=[("Footprint", "(4 + 2n) long x 6 tall x 3 deep"), ("Default n = 6", "16 x 6 x 3"),
              ("Layers", "gatehouse 6 tall, wall 5 tall; gate 3 long, bay 2 long, cap 1 long"), ("Blocks", "gatehouse T 47, G 2; bay P 21, H 1; cap P 15")],
        legend={"T": ("#6b7885", "Bastion Plating, gatehouse (hatches)"), "P": ("#8e9aa6", "Bastion Plating, wall"), "G": ("#59b8ff", "GT borosilicate glass"),
                "H": (ORANGE, "Gun Mount Hatch (one per bay)")},
        pieces=[("Gatehouse (3 long, once)", [("y5", ["TTT"] * 3), ("y4", ["TGT", "T-T", "TTT"]), ("y3", ["TGT", "T-T", "TTT"]), ("y2", ["TTT", "T-T", "TTT"]),
                                              ("y1", ["T~T", "T-T", "TTT"]), ("y0", ["TTT"] * 3)]),
                ("Bay (2 long, repeat n = 2 to 10)", [("y4", ["P-"] * 3), ("y3", ["P-"] * 3), ("y2", ["PH", "P-", "P-"]), ("y1", ["PP"] * 3), ("y0", ["PP"] * 3)]),
                ("End cap (1 long, once)", [("y4", ["P"] * 3), ("y3", ["P"] * 3), ("y2", ["P"] * 3), ("y1", ["P"] * 3), ("y0", ["P"] * 3)])],
        hatches=["Controller: front of the gatehouse, middle, second layer (the ~ cell).",
                 "Input Bus, Output Bus, Energy, Maintenance: on T in the gatehouse. Input Buses are one shared ammo store for every mount.",
                 "Gun Mount Hatch (MV to IV, IDs 20121-20124): exactly one per bay, on the sill at H; holds one gun.",
                 "No Muffler and no pollution. Never explodes in rain."],
        grow=["Add mounts: n = 2 to 10, each 2 blocks long (+2 length): a merlon, a crenel and a hatch.",
              "Hologram stack size selects n; survival build places gatehouse, bays in order, then cap.",
              "A missing hatch in any bay is a structure error."],
        mech=["Parallel = n mounts, no energy discount: upkeep 4 EU/t per mount plus 120 EU per HP healed (proposal). Ten mounts = 40 EU/t, inside an MV Energy Hatch.",
              "Gun Mount Hatch tier caps the gun tier a mount accepts.",
              "Armour = lowest Bastion Plating meta: Iron 5, Steel 10, Aluminium 15, Carbon 20, for every mount.",
              "One owner, team and animal-target setting on the controller; breaking a hatch kills that mount's 30 HP turret."],
    ),
}

# 3D positions for callout markers, per structure: (number, (x, y, z))
MB_MARKS = {
    "works": [(1, (1.5, 5, 1.0)), (2, (1.1, 1.2, 3.6)), (3, (2.4, 5, 1.7)), (4, (14, 2.5, 1.8)), (5, (7, 5.4, 0.6))],
    "retort": [(1, (2.5, 4.5, 1.8)), (2, (5, 2.5, 1.8)), (3, (2.5, 2.5, 8.2))],
    "gantry": [(1, (1, 5, 2.5)), (2, (1, 5, 0.6)), (3, (3.1, 3.4, 0.3)), (4, (2.75, 2.5, 5.9))],
    "rampart": [(1, (1.5, 3, 1.5)), (2, (1.5, 3, 4.4)), (3, (4.5, 2.1, 3.4))],
}
MB_BRACKET = {
    "works": ((3, 6.6, 0), (11, 6.6, 0), "n press bays (2 to 8)"),
    "retort": ((-0.6, 5.6, 2.2), (-0.6, 5.6, 4.8), "n bulb layers (2 to 6)"),
    "gantry": ((2.5, 5.9, 0), (12, 5.9, 0), "n bays (2 to 8)"),
    "rampart": ((3, 3.8, 0), (15, 3.8, 0), "n mounts (2 to 10)"),
}
MB_HATCH_KEY = {
    "works": ["Controller", "Input Bus (hopper tops)", "Energy, Maintenance, Muffler", "Output Bus", "Belt (item pipe casing)"],
    "retort": ["Controller (floor, front)", "Buses, Input Hatch, Energy, Maintenance", "Muffler (cap)"],
    "gantry": ["Controller (head booth)", "Buses, Energy, Maintenance", "Repair Cradle", "Gantry Hoist"],
    "rampart": ["Controller (gatehouse)", "Buses, Energy, Maintenance", "Gun Mount Hatch"],
}


def make_multiblock(key: str) -> str:
    m = MB[key]
    W, H = 1600, 1120
    s = Svg(W, H, bg=INK, radius=14)
    s.text(36, 56, m["title"], 36, TXT, weight="700")
    cx = 36 + len(m["title"]) * 20 + 24
    chip(s, cx, 32, m["cls"], CLS_COL[m["cls"]], INK, 13, h=28)
    s.text(cx + len(m["cls"]) * 8 + 50, 52, m["id"], 15, MUTED, family=MONO)
    s.para(36, 86, m["sub"], 1100, 15, MUTED)
    tier_ruler(s, W - 36 - 8 * 34 - 7 * 4, 36, m["tiers"], ctrl=m["ctrl"])
    s.text(W - 36, 84, "lit = hatch tiers, white outline = controller tier", 12, MUTED, "end")
    # left: sketch
    s.rect(24, 130, 800, 500, PANEL, rx=12)
    s.text(44, 160, "ISOMETRIC SKETCH (default size)", 12.5, MUTED, weight="700", extra='letter-spacing="1.5"')
    iso = Iso(u=14)
    m["scene"](iso)
    P = iso.draw(s, 424, 400, 700, 380)
    (a, b, lab) = MB_BRACKET[key]
    pa, pb = P(*a), P(*b)
    s.line(pa[0], pa[1], pb[0], pb[1], ORANGE, 3)
    for q in (pa, pb):
        s.line(q[0] - 6, q[1] - 6, q[0] + 6, q[1] + 6, ORANGE, 3)
    mx, my = (pa[0] + pb[0]) / 2, (pa[1] + pb[1]) / 2
    if key == "retort":
        s.text(mx - 14, my + 5, lab, 15, ORANGE, "end", "700")
    else:
        s.text(pb[0] + 8, pb[1] + 28, lab, 15, ORANGE, "end", "700")
    for num, pos in MB_MARKS[key]:
        q = P(*pos)
        s.add(f'<circle cx="{q[0]:.1f}" cy="{q[1]:.1f}" r="11" fill="#ffffff" stroke="{ORANGE}" stroke-width="3"/>')
        s.text(q[0], q[1] + 4.5, str(num), 13, INK, "middle", "700")
    # right: layers
    s.rect(844, 130, 732, 500, PANEL, rx=12)
    s.text(864, 160, "LAYERS (each grid is one layer from above; rows run front to back)", 12.5, MUTED, weight="700", extra='letter-spacing="1"')
    y = 182
    cell = int(min(20, min((692 - len(L) * 14) // (len(L) * len(L[0][1][0])) for _, L in m["pieces"])))
    for name, layers in m["pieces"]:
        s.text(864, y + 12, name, 14, TXT, weight="700")
        y += 22
        x = 864
        gh = len(layers[0][1]) * cell
        for lname, rows in layers:
            layer_grid(s, x, y, rows, m["legend"] | CELL_COL, cell)
            s.text(x + len(rows[0]) * cell / 2, y + gh + 13, lname, 10.5, MUTED, "middle")
            x += len(rows[0]) * cell + 14
        y += gh + 30
    ly = y + 10
    lx = 864
    items = [("~", "Controller")] + [(k, v[1]) for k, v in m["legend"].items()]
    for k, lab in items:
        w = 26 + (len(lab) + 2) * 6.4 + 14
        if lx + w > 1560:
            lx = 864
            ly += 22
        if k == "~":
            s.rect(lx, ly - 11, 13, 13, "#ffffff", rx=2, stroke=ORANGE, sw=2)
        else:
            s.rect(lx, ly - 11, 13, 13, m["legend"][k][0], rx=2)
        s.text(lx + 19, ly, f"{k} {lab}" if k != "~" else lab, 11.5, TXT)
        lx += w
    cards = [("Dimensions and layers", [f"{a}: {b}" for a, b in m["dims"]], None),
             ("Controller and hatches", m["hatches"], MB_HATCH_KEY[key]),
             ("Expandable segments", m["grow"], None),
             ("Tier and parallel mechanic", m["mech"], None)]
    x = 24
    cw = (W - 48 - 3 * 14) / 4
    for title, lines, keys in cards:
        s.rect(x, 650, cw, 420, PANEL, rx=12)
        s.text(x + 18, 684, title, 17, ORANGE, weight="700")
        yy = 712
        for ln in lines:
            yy = s.para(x + 18, yy, ln, cw - 36, 13.5, TXT, lh=1.32) + 9
        if keys:
            yy += 8
            s.text(x + 18, yy, "SKETCH MARKERS", 12, MUTED, weight="700", extra='letter-spacing="1"')
            yy += 22
            for i, k in enumerate(keys):
                s.add(f'<circle cx="{x + 28}" cy="{yy - 4}" r="9" fill="#ffffff" stroke="{ORANGE}" stroke-width="2.5"/>')
                s.text(x + 28, yy, str(i + 1), 11, INK, "middle", "700")
                s.text(x + 46, yy, k, 13, TXT)
                yy += 24
        x += cw + 14
    s.text(36, H - 14, "Source: docs/gregguns-spec/08-machines-production/multiblocks.md and machine-roster.md. Layouts are designs, not Java; figures marked proposal are not settled.", 12, MUTED)
    return s.render()


# ---------------------------------------------------------------- rasterising
def _rasterise_playwright(jobs):
    from playwright.sync_api import sync_playwright

    with sync_playwright() as p:
        browser = p.chromium.launch(channel="msedge")
        for svg_text, png_path, w, h, transparent in jobs:
            page = browser.new_page(viewport={"width": w, "height": h}, device_scale_factor=1)
            inline = re.sub(r'(<svg [^>]*?)width="\d+" height="\d+"', r'\1width="%d" height="%d"' % (w, h), svg_text, count=1)
            page.set_content(f'<!doctype html><meta charset="utf-8"><body style="margin:0;background:transparent">{inline}</body>')
            page.screenshot(path=str(png_path), omit_background=transparent, clip={"x": 0, "y": 0, "width": w, "height": h})
            page.close()
        browser.close()


def _rasterise_cairo(jobs):
    import cairosvg

    for svg_text, png_path, w, h, _ in jobs:
        cairosvg.svg2png(bytestring=svg_text.encode("utf-8"), write_to=str(png_path), output_width=w, output_height=h)


def rasterise(jobs):
    try:
        _rasterise_playwright(jobs)
    except Exception as e:  # noqa: BLE001 - fall back to cairosvg
        print("Playwright/Edge unavailable (%s); trying cairosvg" % e, file=sys.stderr)
        _rasterise_cairo(jobs)


def pixel_pngs():
    from PIL import Image, ImageDraw

    def hexed(size):
        ss = 8  # supersample the vector hex, then reduce: smooth edge
        big = Image.new("RGBA", (size * ss, size * ss), (0, 0, 0, 0))
        d = ImageDraw.Draw(big)
        k = size * ss / 32
        for r, col in ((PIX_OUTER, "#5b6876"), (PIX_RING, BLUE), (PIX_IN, "#141a21")):
            d.polygon([(x * k, y * k) for x, y in hex_points(16, 16, r)], fill=col)
        return big.resize((size, size), Image.LANCZOS)

    for size in (32, 128, 256):
        img = hexed(size)
        k = size // 32
        d = ImageDraw.Draw(img)
        for x, y, w, h, c in pixel_logo_rects():
            d.rectangle([x * k, y * k, (x + w) * k - 1, (y + h) * k - 1], fill=c)
        img.save(BRAND / f"logo_pixel_{size}.png")


def main():
    svg_only = "--svg-only" in sys.argv
    BRAND.mkdir(parents=True, exist_ok=True)
    INFO.mkdir(parents=True, exist_ok=True)
    jobs = []

    def emit(path: Path, text: str, sizes):
        path.write_text(text, encoding="utf-8", newline="\n")
        for suffix, w, h, tr in sizes:
            jobs.append((text, path.with_name(path.stem + suffix + ".png"), w, h, tr))

    emit(BRAND / "logo.svg", make_logo(), [("_128", 128, 128, True), ("_512", 512, 512, True)])
    (BRAND / "logo_pixel.svg").write_text(pixel_logo_svg(), encoding="utf-8", newline="\n")
    emit(BRAND / "banner.svg", make_banner(), [("", 1280, 320, False)])
    emit(BRAND / "social_preview.svg", make_social(), [("", 1280, 640, False)])
    for name, fn in (("tier-progression", make_tier_progression), ("machine-roster", make_machine_roster),
                     ("ammo-production-flow", make_ammo_flow), ("questbook-overview", make_questbook),
                     ("multiblock-ammunition-works", lambda: make_multiblock("works")), ("multiblock-retort", lambda: make_multiblock("retort")),
                     ("multiblock-armoury-gantry", lambda: make_multiblock("gantry")), ("multiblock-rampart", lambda: make_multiblock("rampart"))):
        text = fn()
        m = re.search(r'width="(\d+)" height="(\d+)"', text)
        emit(INFO / f"{name}.svg", text, [("", int(m.group(1)), int(m.group(2)), False)])
    if not svg_only:
        rasterise(jobs)
        pixel_pngs()
    print("wrote", len(jobs), "PNG jobs" if not svg_only else "SVGs only")


if __name__ == "__main__":
    main()
