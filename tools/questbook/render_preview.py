#!/usr/bin/env python3
"""Render quests.json the way BetterQuesting draws a quest line, to a PNG.

Dev-only tool: it needs Pillow (`pip install pillow`), unlike the rest of tools/questbook, which is standard
library only. Nothing in the build depends on it.

usage: python -I tools/questbook/render_preview.py OUT.png [--data quests.json] [--zoom N] [--implicit]

What it mirrors from BetterQuesting 3.8.89-GTNH (see README.md, "How BetterQuesting draws a line"):
- a node is a square of `size` canvas units at (x, y); a prerequisite line runs from the center of the
  prerequisite to the center of the quest that needs it, behind the nodes;
- the line is 8 units thick when the DEPENDENT quest is `main`, otherwise 4;
- a prerequisite of type IMPLICIT is drawn only while one of its two ends is hovered or Shift is held, so it is
  left out here unless --implicit is given; prerequisites that are not in this line (pack quests) are not drawn;
- the canvas zooms to fit the window with a 24 unit margin. The default zoom fits a 1650 x 940 viewport.
"""
import argparse
import json
import os
import sys

from PIL import Image, ImageDraw, ImageFont

COLORS = {'start': (200, 200, 200), 'steam': (176, 124, 70), 'lv': (150, 170, 190), 'mv': (230, 160, 70),
          'hv': (230, 200, 60), 'ev': (150, 90, 200), 'iv': (70, 120, 220), 'luv': (220, 80, 160),
          'zpm': (230, 70, 70), 'npc': (110, 170, 90), 'site': (90, 150, 150), 'mach': (160, 160, 110),
          'gear': (190, 130, 110), 'ammo': (200, 190, 150), 'icbm': (220, 120, 80)}
MARGIN = 24


def short(q):
    name = q['icon']['id'].split(':', 1)[-1]
    return name[:9]


def render(data, out, zoom=None, implicit=False, view=(1650, 940)):
    qs = [q for q in data['quests'] if not q.get('skip')]  # triggers are in no line, so BQ never draws them
    by = {q['key']: q for q in qs}
    x0 = min(q['x'] for q in qs) - MARGIN
    y0 = min(q['y'] for q in qs) - MARGIN
    x1 = max(q['x'] + q['size'] for q in qs) + MARGIN
    y1 = max(q['y'] + q['size'] for q in qs) + MARGIN
    z = zoom or min(view[0] / (x1 - x0), view[1] / (y1 - y0))
    w, h = int((x1 - x0) * z) + 1, int((y1 - y0) * z) + 1
    img = Image.new('RGB', (w, h), (28, 30, 34))
    d = ImageDraw.Draw(img)
    font = ImageFont.load_default()

    def c(q):
        return ((q['x'] + q['size'] / 2 - x0) * z, (q['y'] + q['size'] / 2 - y0) * z)

    for q in qs:
        imp = set(q.get('implicit', []))
        for p in q['pre']:
            if p not in by or (p in imp and not implicit):
                continue
            width = max(1, round((8 if q.get('main', q['size'] > 24) else 4) * z))
            col = (120, 120, 120) if p in imp else (210, 30, 30)
            d.line([c(by[p]), c(q)], fill=col, width=width)
    for q in qs:
        l, t = (q['x'] - x0) * z, (q['y'] - y0) * z
        s = q['size'] * z
        d.rectangle([l, t, l + s, t + s], fill=COLORS.get(q['chapter'], (150, 150, 150)), outline=(20, 20, 20))
        if z * 24 >= 28:
            d.text((l + 2, t + s / 2 - 5), short(q), fill=(0, 0, 0), font=font)
    img.save(out)
    return z, (w, h)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('out')
    ap.add_argument('--data', default=os.path.join(os.path.dirname(os.path.abspath(__file__)), 'quests.json'))
    ap.add_argument('--zoom', type=float)
    ap.add_argument('--implicit', action='store_true', help='also draw implicit prerequisites (grey)')
    a = ap.parse_args()
    with open(a.data, encoding='utf-8') as f:
        z, size = render(json.load(f), a.out, a.zoom, a.implicit)
    print('zoom %.3f, %dx%d' % (z, *size))


if __name__ == '__main__':
    sys.exit(main())
