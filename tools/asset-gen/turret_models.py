#!/usr/bin/env python3
"""Textures for the turret base block and the turret head entity: one 64 x 64 sheet per armor tier.

The geometry lives in Java (`com.gregguns.client.utility.TurretGeometry`, between the BEGIN/END BOX LIST markers); this
script reads that list, so the sheet always matches the boxes. Original, procedural, seeded art painted from the clean
room notes (docs/techguns-spec/09-machines-utility/model-notes/turret_base.md, section 5): speckled steel on the
body, flat dark metal on the frame, a dark recessed port with a plug, and a radial fade on the plate top. No Techguns
input of any kind.

Run from the repo root:  python -I -X utf8 tools/asset-gen/turret_models.py
Outputs: src/main/resources/assets/gregguns/textures/models/machines/turret_sheet[_tier].png and
credits/turret_models.csv.
"""
import csv
import hashlib
import os
import re

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "gregguns", "client", "utility", "TurretGeometry.java")
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "gregguns", "textures", "models", "machines")
SIZE = 64


def rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


# suffix -> (body levels, frame levels, port dark, port plug, plate rim). Levels come from the notes' tier table; the
# first body level is the most common, the later ones are the speckle.
TIERS = {
    "": (["#949494", "#949494", "#A4A4A4", "#ACACAC", "#8C8C8C"], ["#242424", "#282828", "#2C2C2C"], "#101010", "#2C2C2C", "#3C3C3C"),
    "_iron": (["#A4A4A4", "#B0B0B0", "#BCBCBC", "#A8A8A8"], ["#444444", "#484848", "#4C4C4C"], "#141414", "#303030", "#444444"),
    "_steel": (["#848484", "#8C8C8C", "#949494", "#9C9C9C"], ["#1C1C1C", "#282828", "#343434"], "#101010", "#2C2C2C", "#383838"),
    "_aluminum": (["#443444", "#4C3C54", "#54445C", "#483848"], ["#1C141C", "#242024", "#2C2C2C"], "#100C14", "#2C2434", "#3C3040"),
    "_carbon": (["#0C0C0C", "#141414", "#1C1C1C", "#242424"], ["#040404", "#0C0C0C", "#141414"], "#000000", "#141414", "#202020"),
}

BOX_RE = re.compile(r'new Box\("(\w+)",\s*Part\.(\w+),\s*([-\d.]+),\s*([-\d.]+),\s*([-\d.]+),\s*(\d+),\s*(\d+),\s*(\d+),'
                    r'\s*(\d+),\s*(\d+)(?:,\s*([-\d.]+))?\)')


def read_boxes():
    text = open(JAVA, encoding="utf-8").read()
    text = text[text.index("BEGIN BOX LIST"):text.index("END BOX LIST")]
    return [dict(name=m[0], part=m[1], w=int(m[5]), h=int(m[6]), d=int(m[7]), u=int(m[8]), v=int(m[9]))
            for m in BOX_RE.findall(text)]


def faces(b):
    """The six unwrap rectangles (x, y, w, h) of a vanilla box."""
    u, v, w, h, d = b["u"], b["v"], b["w"], b["h"], b["d"]
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "left": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "right": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def paint(suffix, seed):
    body, frame, port_dark, port_plug, rim = TIERS[suffix]
    body = [rgb(c) for c in body]
    frame = [rgb(c) for c in frame]
    rng = np.random.default_rng(seed)
    img = np.zeros((SIZE, SIZE, 4), np.uint8)

    def fill(rect, colors, speckle):
        x, y, w, h = rect
        for j in range(h):
            for i in range(w):
                c = colors[0] if not speckle or rng.random() < 0.55 else colors[int(rng.integers(0, len(colors)))]
                img[y + j, x + i] = (*c, 255)

    def flat(rect, color):
        x, y, w, h = rect
        img[y:y + h, x:x + w] = (*color, 255)

    for b in read_boxes():
        name = b["name"]
        f = faces(b)
        if name == "base_body":
            for r in f.values():
                fill(r, body, True)
        elif name == "base_plate":
            for k, r in f.items():
                if k != "top":
                    fill(r, body, True)
            # The "hole": black in the middle fading to the rim gray, painted only (the plate is solid).
            x, y, w, h = f["top"]
            ctr = (w - 1) / 2.0
            rc = rgb(rim)
            for j in range(h):
                for i in range(w):
                    t = min(1.0, np.hypot(i - ctr, j - ctr) / (ctr * 1.25))
                    img[y + j, x + i] = [*(int(round(t * t * c)) for c in rc), 255]
        elif name.startswith("base_port"):
            dark, plug = rgb(port_dark), rgb(port_plug)
            for r in f.values():
                flat(r, dark)
                x, y, w, h = r
                if w >= 4 and h >= 4:  # the two big faces get the smaller square inside the socket
                    flat((x + 2, y + 2, w - 4, h - 4), plug)
        elif name == "head_mount":
            lighter = tuple(int((a * 2 + c) / 3) for a, c in zip(frame[-1], body[0]))
            for r in f.values():
                flat(r, lighter)
        else:  # posts, struts, rails, foot, uprights: the frame metal, flat with a faint dither
            for r in f.values():
                fill(r, frame, True)
    return Image.fromarray(img)


def seed_for(key):
    return int(hashlib.sha1(key.encode()).hexdigest()[:8], 16)


def main():
    os.makedirs(OUT, exist_ok=True)
    rows = []
    for suffix in TIERS:
        name = "turret_sheet" + suffix
        seed = seed_for("model.machine." + name)
        paint(suffix, seed).save(os.path.join(OUT, name + ".png"))
        rows.append(["model.machine." + name,
                     "src/main/resources/assets/gregguns/textures/models/machines/" + name + ".png",
                     "tools/asset-gen/turret_models.py", seed,
                     "Procedural numpy/Pillow: speckled body steel, flat dark frame, dark port with plug and a painted "
                     "radial plate top, painted onto the unwrap of the Java turret boxes", "MIT", "2026-10-19"])
    with open(os.path.join(HERE, "credits", "turret_models.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["asset", "path", "generator", "seed", "method", "license", "date"])
        w.writerows(rows)
    print("turret sheets:", len(rows))


if __name__ == "__main__":
    main()
