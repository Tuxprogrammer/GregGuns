"""Variant diff check for the armor sheets (review chunk J, finding 6).

For every variant family in armor_repaint.FAMILIES it compares each painted region of each variant against the
base set, per layer, on the actual PNGs. A region passes when its mean color differs from the base in value
(>= 0.05), saturation (>= 0.15) or, for two saturated colors, hue (>= 20 degrees). This catches the "variant
forgot to recolor the helmet" bug. Regions listed in armor_repaint.SHARED are intentionally common to the family
and print as SHARED (with the reason) instead of FAIL; a shared region that does differ is still printed as such.
Exit status is 1 if any non-shared region fails.
Run: python -I -X utf8 tools/asset-gen/armor_variant_check.py [--dir DIR]
"""

import argparse
import colorsys
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import armor_repaint as AR  # noqa: E402

V_MIN, S_MIN, H_MIN = 0.05, 0.15, 20.0


def region_color(img, name, layer):
    """Mean RGB (0..1) over the opaque pixels of a region, or None if it has none."""
    px = []
    for x0, y0, x1, y1 in AR.region_rects(name, layer):
        blk = img[y0:y1, x0:x1].reshape(-1, 4)
        px.append(blk[blk[:, 3] > 0, :3])
    px = np.vstack(px) if px else np.zeros((0, 3))
    return px.mean(axis=0) / 255.0 if len(px) else None


def differs(a, b):
    ha, sa, va = colorsys.rgb_to_hsv(*a)
    hb, sb, vb = colorsys.rgb_to_hsv(*b)
    dh = min(abs(ha - hb), 1 - abs(ha - hb)) * 360
    why = []
    if abs(va - vb) >= V_MIN:
        why.append("value %.2f->%.2f" % (va, vb))
    if abs(sa - sb) >= S_MIN:
        why.append("sat %.2f->%.2f" % (sa, sb))
    if sa > 0.2 and sb > 0.2 and dh >= H_MIN:
        why.append("hue %d deg" % dh)
    return why


def load(dirpath, name, layer):
    return np.array(Image.open(os.path.join(dirpath, "%s_%d.png" % (name, layer))).convert("RGBA"))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dir", default=AR.ARMOR_DIR)
    args = ap.parse_args()
    bad = 0
    for base, variants in AR.FAMILIES.items():
        for var in variants:
            shared = AR.SHARED.get(var, {})
            for layer in (1, 2):
                a, b = load(args.dir, base, layer), load(args.dir, var, layer)
                regions = [r for r in AR.GEOM if r in AR.SETS[base] and (layer == 1 or r in AR.LAYER2)]
                for r in regions:
                    ca, cb = region_color(a, r, layer), region_color(b, r, layer)
                    why = differs(ca, cb) if ca is not None and cb is not None else ["n/a"]
                    if why and why != ["n/a"]:
                        status = "PASS  " if r not in shared else "PASS* "
                    elif r in shared:
                        status = "SHARED"
                    else:
                        status = "FAIL  "
                        bad += 1
                    note = ", ".join(why) if why else (shared.get(r, "same as base"))
                    print("%s %-18s L%d %-10s %s" % (status, var, layer, r, note))
    print("\n%s: %d region(s) failed" % ("FAIL" if bad else "PASS", bad))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
