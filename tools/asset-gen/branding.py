#!/usr/bin/env python3
"""GregGuns branding generator (logo, banner, social card).

Original artwork: nothing here imitates Techguns' logo or art (Techguns is
All Rights Reserved). Every SVG is a hand-authored string built by this script,
so the output is reproducible: run it again and the same files come out.

Usage (from the repo root):
    python tools/asset-gen/branding.py            # SVG + PNG
    python tools/asset-gen/branding.py --svg-only # SVG only, no rasterizer needed

Outputs
    docs/assets/branding/     logo.svg, logo_pixel.svg, banner.svg, social_preview.svg (+ PNGs)

The player guide infographics are built by tools/asset-gen/infographics/build.py.

Rasterizer (documented as requested)
    PRIMARY: Playwright for Python driving the installed Microsoft Edge on Windows
        (chromium.launch(channel="msedge")) and Playwright's bundled Chromium elsewhere. The SVG is inlined into a bare HTML page and
        screenshotted at the exact target size. PNGs for logos keep a transparent background.
    FALLBACK: cairosvg, if Playwright/Edge is unavailable (needs libcairo on the machine).
    Install: `python -m pip install playwright` (Edge already ships with Windows 10/11; elsewhere also run `python -m playwright install chromium`).
    Pixel-art PNGs (logo_pixel_*.png) are drawn with Pillow, nearest-neighbor, so they stay crisp.

Fonts: lettering in the logo, banner and social card is drawn as pixel-block paths from a
5x7 glyph table below, so it does not depend on any installed font. The banner's tagline
uses the generic family `sans-serif` (Segoe UI, Helvetica Neue, Arial, then Liberation Sans, the list in `FONT` below).
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
FONT = "'Segoe UI', 'Helvetica Neue', Arial, 'Liberation Sans', sans-serif"
MONO = "Consolas, 'Courier New', monospace"

# GT voltage-name colors, readable on a dark card: key, label, EU/t, fill, text on fill
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
    """32x32 pixel-art contents (rifle, cable, bullet) as (x, y, w, h, color) rectangles.

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
    """Eight small hexes in the GT voltage-name colors (ULV..ZPM) as a row."""
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
    s.text(324, 232, "Tiered guns, armor and ammo for GT New Horizons (Minecraft 1.7.10).", 21, MUTED)
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
    for line in ("Guns, armor and ammo that climb the GregTech tiers.",
                 "Built for GT New Horizons: GT materials, recipe maps and EU."):
        s.text(524, y, line, 27, TXT)
        y += 40
    tier_hex_strip(s, 524, 505, 24, gap=6)
    s.text(524, 568, "ULV to ZPM", 22, MUTED, weight="600")
    s.rect(0, H - 14, W, 14, BLUE)
    s.rect(0, H - 14, 360, 14, ORANGE)
    return s.render()


# ---------------------------------------------------------------- isometric helper
def _rasterize_playwright(jobs):
    from playwright.sync_api import sync_playwright

    with sync_playwright() as p:
        # Edge on Windows (ships with the OS); Playwright's bundled Chromium elsewhere.
        browser = p.chromium.launch(channel="msedge") if sys.platform == "win32" else p.chromium.launch()
        for svg_text, png_path, w, h, transparent in jobs:
            page = browser.new_page(viewport={"width": w, "height": h}, device_scale_factor=1)
            inline = re.sub(r'(<svg [^>]*?)width="\d+" height="\d+"', r'\1width="%d" height="%d"' % (w, h), svg_text, count=1)
            page.set_content(f'<!doctype html><meta charset="utf-8"><body style="margin:0;background:transparent">{inline}</body>')
            page.screenshot(path=str(png_path), omit_background=transparent, clip={"x": 0, "y": 0, "width": w, "height": h})
            page.close()
        browser.close()


def _rasterize_cairo(jobs):
    import cairosvg

    for svg_text, png_path, w, h, _ in jobs:
        cairosvg.svg2png(bytestring=svg_text.encode("utf-8"), write_to=str(png_path), output_width=w, output_height=h)


def rasterize(jobs):
    try:
        _rasterize_playwright(jobs)
    except Exception as e:  # noqa: BLE001 - fall back to cairosvg
        print("Playwright/Edge unavailable (%s); trying cairosvg" % e, file=sys.stderr)
        _rasterize_cairo(jobs)


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
    jobs = []

    def emit(path: Path, text: str, sizes):
        path.write_text(text, encoding="utf-8", newline="\n")
        for suffix, w, h, tr in sizes:
            jobs.append((text, path.with_name(path.stem + suffix + ".png"), w, h, tr))

    emit(BRAND / "logo.svg", make_logo(), [("_128", 128, 128, True), ("_512", 512, 512, True)])
    (BRAND / "logo_pixel.svg").write_text(pixel_logo_svg(), encoding="utf-8", newline="\n")
    emit(BRAND / "banner.svg", make_banner(), [("", 1280, 320, False)])
    emit(BRAND / "social_preview.svg", make_social(), [("", 1280, 640, False)])
    if not svg_only:
        rasterize(jobs)
        pixel_pngs()
    print("wrote", len(jobs), "PNG jobs" if not svg_only else "SVGs only")


if __name__ == "__main__":
    main()
