#!/usr/bin/env python3
"""Textures for the worn accessory models (masks, goggles, back devices).

The geometry lives in Java (`com.gregguns.client.accessory.AccessoryDefs`); a test exports it to
`tools/asset-gen/accessory_layouts/<id>.json` (box sizes plus the texture origin the sheet packer chose), and this
script paints one sheet per texture from those layouts. Original, procedural, seeded art: flat faces, a one-pixel edge
highlight and shadow, panel seams and a few details. No Techguns input of any kind.

Refresh the layouts after changing a model:
    GREGGUNS_EXPORT_MODELS=<dir> ./gradlew test --tests com.gregguns.tools.ModelExportTest
    copy <dir>/accessories/*.json to tools/asset-gen/accessory_layouts/
Run from the repo root:  python -I -X utf8 tools/asset-gen/accessory_models.py
Outputs: src/main/resources/assets/gregguns/textures/models/armor/<texture>.png, credits/accessory_models.csv,
accessory_layouts.md and docs/assets/previews/accessories.png.
"""
import csv
import hashlib
import json
import os

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
LAYOUTS = os.path.join(HERE, "accessory_layouts")
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "gregguns", "textures", "models", "armor")
PREVIEW = os.path.join(ROOT, "docs", "assets", "previews")

# Textures this script owns (models.py skips them).
OWNED = ["gas_mask", "oxygen_mask", "tactical_mask", "tactical_mask_green", "tactical_mask_white",
         "tactical_mask_lightgreen", "night_vision_goggles", "oxygen_tanks", "jump_pack", "anti_gravity_pack",
         "anti_gravity_pack_bronze", "anti_gravity_pack_black", "anti_gravity_pack_dark_green",
         "anti_gravity_pack_silver", "glider"]


def seed_for(key):
    return int(hashlib.sha1(key.encode()).hexdigest()[:8], 16)


def hexc(s):
    return np.array([int(s[i:i + 2], 16) for i in (1, 3, 5)], float)


class Sheet:
    def __init__(self, w, h, rng):
        self.w, self.h, self.rng = w, h, rng
        self.rgb = np.zeros((h, w, 3), float)
        self.a = np.zeros((h, w), float)

    def fill(self, x, y, w, h, color):
        x0, y0, x1, y1 = max(0, x), max(0, y), min(self.w, x + w), min(self.h, y + h)
        if x1 > x0 and y1 > y0:
            self.rgb[y0:y1, x0:x1] = np.clip(np.asarray(color, float), 0, 255)
            self.a[y0:y1, x0:x1] = 255

    def px(self, x, y, color):
        self.fill(x, y, 1, 1, color)

    def mul(self, x, y, w, h, f):
        x0, y0, x1, y1 = max(0, x), max(0, y), min(self.w, x + w), min(self.h, y + h)
        if x1 > x0 and y1 > y0:
            self.rgb[y0:y1, x0:x1] = np.clip(self.rgb[y0:y1, x0:x1] * f, 0, 255)

    def image(self):
        out = np.zeros((self.h, self.w, 4), np.uint8)
        out[..., :3] = np.clip(self.rgb + 0.5, 0, 255).astype(np.uint8)
        out[..., 3] = np.where(self.a > 0, 255, 0).astype(np.uint8)
        return Image.fromarray(out, "RGBA")


# Face shading, as vanilla's directional light would give a lit box: top bright, bottom dark.
SHADE = {"top": 1.14, "bottom": 0.66, "front": 1.0, "back": 0.9, "left": 0.86, "right": 0.94}


def faces(b):
    u, v = b["uv"]
    w, h, d = b["size"]
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def paint_box(sh, b, base, overrides=None, seams=True):
    """Flat shaded faces with a one-pixel highlight on the top and left edges and a shadow on the bottom and right.
    overrides: face name -> color."""
    overrides = overrides or {}
    for name, (x, y, w, h) in faces(b).items():
        c = np.asarray(overrides.get(name, base), float)
        sh.fill(x, y, w, h, c * SHADE[name])
        # tiny seeded value drift per face so large flats are not perfectly dead
        sh.mul(x, y, w, h, 1.0 + (sh.rng.random() - 0.5) * 0.04)
        if w >= 3 and h >= 3 and seams:
            sh.mul(x, y, w, 1, 1.18)
            sh.mul(x, y, 1, h, 1.10)
            sh.mul(x, y + h - 1, w, 1, 0.78)
            sh.mul(x + w - 1, y, 1, h, 0.86)


def rect_on(sh, b, face, rx, ry, rw, rh, color, factor=1.0):
    x, y, w, h = faces(b)[face]
    sh.fill(x + rx, y + ry, min(rw, w - rx), min(rh, h - ry), np.asarray(color, float) * factor)


def stripes_on(sh, b, face, c1, c2, step=2):
    """Diagonal hazard stripes."""
    x, y, w, h = faces(b)[face]
    for j in range(h):
        for i in range(w):
            sh.px(x + i, y + j, c1 if ((i + j) // step) % 2 == 0 else c2)


def by_name(layout):
    return {b["name"]: b for b in layout["boxes"]}


# ---------------------------------------------------------------- models

def gas_mask(layout, rng, p):
    sh = Sheet(layout["textureWidth"], layout["textureHeight"], rng)
    B = by_name(layout)
    shell = hexc("#3a4a36")
    dark = hexc("#222623")
    paint_box(sh, B["shell"], shell, {"top": hexc("#43543e"), "bottom": hexc("#2c372a")})
    f = B["shell"]
    # Two eye lenses behind dark rims on the front face, a vent row above the chin.
    for lx in (1, 5):
        rect_on(sh, f, "front", lx, 2, 3, 3, dark)
        rect_on(sh, f, "front", lx + 1, 3, 1, 1, hexc("#56806a"))
        rect_on(sh, f, "front", lx, 2, 3, 1, hexc("#171a18"))
    rect_on(sh, f, "front", 2, 6, 5, 1, hexc("#2b352a"))
    for sx in (3, 5):
        rect_on(sh, f, "front", sx, 7, 1, 2, dark)
    # side straps
    for face in ("left", "right"):
        rect_on(sh, f, face, 0, 3, 9, 1, hexc("#1e211f"))
        rect_on(sh, f, face, 0, 6, 9, 1, hexc("#1e211f"))
    paint_box(sh, B["mouthpiece"], hexc("#2c2e30"))
    for sx in (1, 3):
        rect_on(sh, B["mouthpiece"], "front", sx, 0, 1, 4, hexc("#141516"))
    paint_box(sh, B["filter"], hexc("#5d6368"), {"front": hexc("#4a5054")})
    fl = B["filter"]
    rect_on(sh, fl, "front", 1, 1, 2, 2, hexc("#1c1f21"))
    rect_on(sh, fl, "front", 1, 1, 1, 1, hexc("#7b8388"))
    for face in ("left", "right", "back"):
        rect_on(sh, fl, face, 0, 1, 4, 1, hexc("#33383b"))
    return sh


def oxygen_mask(layout, rng, p):
    sh = Sheet(layout["textureWidth"], layout["textureHeight"], rng)
    B = by_name(layout)
    light = hexc("#b4b9bf")
    for n in ("strap_front", "strap_back", "strap_left", "strap_right"):
        paint_box(sh, B[n], light, seams=False)
    paint_box(sh, B["cup"], hexc("#a9aeb5"))
    cup = B["cup"]
    for sx in (1, 3):
        rect_on(sh, cup, "front", sx, 0, 1, 3, hexc("#3b3f44"))
    rect_on(sh, cup, "front", 0, 0, 5, 1, hexc("#d2d6db"))
    paint_box(sh, B["valve"], hexc("#7e848a"))
    paint_box(sh, B["hose"], hexc("#53585d"))
    for face in ("left", "right", "top", "bottom"):
        x, y, w, h = faces(B["hose"])[face]
        for k in range(0, max(w, h), 2):
            if w >= h:
                sh.fill(x + k, y, 1, h, hexc("#3a3e42"))
            else:
                sh.fill(x, y + k, w, 1, hexc("#3a3e42"))
    return sh


TACTICAL = {
    "tactical_mask": ("#1f2022", "#7c2222", "#141516"),
    "tactical_mask_green": ("#3b5a3a", "#6b8b4a", "#243624"),
    "tactical_mask_white": ("#d6dade", "#8a9097", "#b3b8be"),
    "tactical_mask_lightgreen": ("#8fae74", "#d6e0a8", "#6a8a52"),
}


def tactical_mask(layout, rng, p):
    sh = Sheet(layout["textureWidth"], layout["textureHeight"], rng)
    base, band, trim = (hexc(c) for c in TACTICAL[p])
    hood = by_name(layout)["hood"]
    paint_box(sh, hood, base, {"top": base * 1.05})
    for face in ("front", "left", "right", "back"):
        x, y, w, h = faces(hood)[face]
        # lower-face wrap: a darker, seamed lower third with a seam line above it
        sh.fill(x, y + 6, w, 3, base * 0.82)
        sh.fill(x, y + 6, w, 1, trim)
        # visor slit across the eyes
        sh.fill(x, y + 3, w, 2, band)
        sh.mul(x, y + 3, w, 1, 1.2)
        sh.mul(x, y + 4, w, 1, 0.8)
    # the slit is deeper over the nose on the front, vents low on the wrap
    rect_on(sh, hood, "front", 3, 3, 3, 2, band * 0.55)
    for sx in (2, 4, 6):
        rect_on(sh, hood, "front", sx, 7, 1, 1, base * 0.5)
    return sh


def goggles(layout, rng, p):
    sh = Sheet(layout["textureWidth"], layout["textureHeight"], rng)
    B = by_name(layout)
    band = hexc("#25272b")
    for n in ("band_front", "band_left", "band_right", "mount_left", "mount_right"):
        paint_box(sh, B[n], band)
    paint_box(sh, B["strap"], hexc("#1c1d20"))
    paint_box(sh, B["bridge"], hexc("#2a2c30"))
    rect_on(sh, B["band_front"], "front", 3, 0, 4, 1, hexc("#3b3e43"))
    for n in ("lens_left", "lens_right"):
        lens = B[n]
        paint_box(sh, lens, hexc("#26282b"))
        # the front end (-z) is the glass: green fill, a bright corner glint; the back end is dark glass
        rect_on(sh, lens, "front", 0, 0, 2, 2, hexc("#2f8f3a"))
        rect_on(sh, lens, "front", 0, 0, 1, 1, hexc("#a6ffb0"))
        rect_on(sh, lens, "back", 0, 0, 2, 2, hexc("#1f2438"))
        for face in ("top", "bottom"):
            x, y, w, h = faces(lens)[face]
            sh.fill(x, y + h - 1, w, 1, hexc("#17181a"))
    return sh


def cylinder(sh, b, body, hi, shadow, ends):
    """A square-section box painted round: a lighter vertical center stripe, darker edge columns, darker ends."""
    paint_box(sh, b, body, {"top": ends, "bottom": ends * 0.8}, seams=False)
    for face in ("front", "back", "left", "right"):
        x, y, w, h = faces(b)[face]
        sh.fill(x, y, w, h, body)
        sh.fill(x + w // 2, y, 1 if w % 2 else 2, h, hi)
        sh.fill(x, y, 1, h, shadow)
        sh.fill(x + w - 1, y, 1, h, shadow)
        sh.fill(x, y + h - 1, w, 1, ends)


def band(sh, b, rows, color, hi=None):
    """A strap ring: full-width rows across the four side faces."""
    for face in ("front", "back", "left", "right"):
        x, y, w, h = faces(b)[face]
        for r in rows:
            sh.fill(x, y + r, w, 1, color)
            if hi is not None:
                sh.px(x + w // 2, y + r, hi)


# Source: docs/techguns-spec/05-armor/model-notes/oxygen_tanks.md, section 5 (palette).
def oxygen_tanks(layout, rng, p):
    sh = Sheet(layout["textureWidth"], layout["textureHeight"], rng)
    B = by_name(layout)
    for n in ("tank_left", "tank_right"):
        cylinder(sh, B[n], hexc("#888888"), hexc("#989898"), hexc("#585858"), hexc("#686868"))
        band(sh, B[n], (2, 6), hexc("#181818"), hexc("#282828"))
    paint_box(sh, B["harness"], hexc("#080808"), seams=False)
    for face, (x, y, w, h) in faces(B["harness"]).items():
        sh.fill(x, y, w, 1, hexc("#181818"))
    for n in ("valve_left", "valve_right"):
        paint_box(sh, B[n], hexc("#989898"), {"top": hexc("#A8A8A8"), "bottom": hexc("#686868")}, seams=False)
    return sh


# Source: docs/techguns-spec/05-armor/model-notes/jump_pack.md, section 5 (palette).
def jump_pack(layout, rng, p):
    sh = Sheet(layout["textureWidth"], layout["textureHeight"], rng)
    B = by_name(layout)
    for n in ("cylinder_left", "cylinder_right"):
        cylinder(sh, B[n], hexc("#A8A8A8"), hexc("#B8B8B8"), hexc("#686868"), hexc("#A8A8A8"))
        # dark nozzle ends: the lowest two rows of every side face and the bottom
        for face in ("front", "back", "left", "right"):
            x, y, w, h = faces(B[n])[face]
            sh.fill(x, y + h - 2, w, 2, hexc("#282828"))
            sh.fill(x, y + h - 2, w, 1, hexc("#383838"))
        x, y, w, h = faces(B[n])["bottom"]
        sh.fill(x, y, w, h, hexc("#181818"))
    for n in ("cap_left", "cap_right"):
        for face, (x, y, w, h) in faces(B[n]).items():
            for j in range(h):
                for i in range(w):
                    sh.px(x + i, y + j, hexc("#F0D818") if (i + j) % 2 == 0 else hexc("#181818"))
        x, y, w, h = faces(B[n])["bottom"]
        sh.fill(x, y, w, h, hexc("#787878"))
    sp = B["spine"]
    paint_box(sh, sp, hexc("#382828"), {"top": hexc("#484038")}, seams=False)
    for face in ("front", "back", "left", "right"):
        x, y, w, h = faces(sp)[face]
        sh.fill(x, y, w, 1, hexc("#484038"))
        sh.fill(x, y + h - 1, w, 1, hexc("#282018"))
    # three lighter strap bands (rows 1, 3 and 5 of the 7 tall spine) around the sides
    band(sh, sp, (1, 3, 5), hexc("#888888"), hexc("#A8A8A8"))
    return sh


# Source: docs/techguns-spec/05-armor/model-notes/anti_gravity_pack.md, section 5 (palette and camos).
# body, highlight, shadow, plate, plate highlight, core base, core light, core dark, clamp, accent
ANTIGRAV = {
    "anti_gravity_pack": ("#A8A8A8", "#C8C8C8", "#686868", "#787878", "#989898", "#10B0D8", "#50D8F0", "#0C6A80",
                          "#282828", None),
    "anti_gravity_pack_bronze": ("#C8A858", "#D8B858", "#887838", "#685828", "#887838", "#10B0D8", "#50D8F0",
                                 "#0C6A80", "#282818", None),
    "anti_gravity_pack_black": ("#181818", "#383838", "#080808", "#282828", "#484848", "#C81818", "#F05848",
                                "#881808", "#282828", "#981818"),
    "anti_gravity_pack_dark_green": ("#182818", "#183818", "#081808", "#183818", "#284828", "#10B0D8", "#50D8F0",
                                     "#0C6A80", "#081808", "#D8D8C8"),
    "anti_gravity_pack_silver": ("#D8D8D8", "#F8F8F8", "#989898", "#B8B8B8", "#E8E8E8", "#10B0D8", "#50D8F0",
                                 "#0C6A80", "#383838", None),
}


def brushed(sh, b, amp=0.07):
    """Brushed-metal variation: per-texel light and dark jitter plus faint streaks along the long axis of each face."""
    for name, (x, y, w, h) in faces(b).items():
        streak = 1.0 + (sh.rng.random((h, 1)) - 0.5) * amp * 1.2
        jitter = 1.0 + (sh.rng.random((h, w)) - 0.5) * amp * 2
        sh.rgb[y:y + h, x:x + w] = np.clip(sh.rgb[y:y + h, x:x + w] * (streak * jitter * 1.14)[..., None], 0, 255)


def anti_gravity(layout, rng, p):
    sh = Sheet(layout["textureWidth"], layout["textureHeight"], rng)
    B = by_name(layout)
    body, hi, shadow, plate_c, plate_hi, core_c, core_hi, core_lo, clamp_c, accent = (
        hexc(c) if c else None for c in ANTIGRAV[p])
    for n in ("slab", "pillar_left", "pillar_right"):
        paint_box(sh, B[n], body, seams=False)
        for face in ("front", "back", "left", "right"):
            x, y, w, h = faces(B[n])[face]
            sh.fill(x, y, w, 1, hi)
            if n != "slab":
                sh.fill(x, y, 1, h, shadow)
            sh.fill(x + w - 1, y, 1, h, shadow)
            sh.fill(x, y + h - 1, w, 1, shadow)
    for n in ("slab", "pillar_left", "pillar_right"):
        brushed(sh, B[n])
    pl = B["plate"]
    paint_box(sh, pl, plate_c, seams=False)
    x, y, w, h = faces(pl)["back"]
    sh.fill(x, y, w, h, plate_c)
    sh.fill(x, y, w, 1, plate_hi)
    sh.fill(x, y + h - 1, w, 1, shadow)
    if accent is not None:
        sh.px(x, y + h // 2, accent)
        sh.px(x + w - 1, y + h // 2, accent)
    brushed(sh, pl, 0.09)
    core = B["core"]
    paint_box(sh, core, core_c, seams=False)
    # faceted: light upper-left, dark lower-right on every face
    for face, (x, y, w, h) in faces(core).items():
        for j in range(h):
            for i in range(w):
                t = (i + j) / max(1, w + h - 2)
                sh.px(x + i, y + j, core_hi * (1 - t) + core_lo * t)
        sh.px(x + w // 2, y + h // 2, core_c)
    for n in ("clamp_left_upper", "clamp_left_lower", "clamp_right_upper", "clamp_right_lower"):
        paint_box(sh, B[n], clamp_c, seams=False)
        rect_on(sh, B[n], "back", 0, 0, 2, 1, clamp_c * 1.4)
    return sh


# Source: docs/techguns-spec/05-armor/model-notes/glider.md, section 5 (palette).
def glider(layout, rng, p):
    sh = Sheet(layout["textureWidth"], layout["textureHeight"], rng)
    B = by_name(layout)
    brown, brown_hi, brown_lo = hexc("#584838"), hexc("#685838"), hexc("#382828")
    frame, frame_hi, frame_lo = hexc("#787878"), hexc("#989898"), hexc("#585858")
    sail, sail_hi, sail_lo = hexc("#C8C8A0"), hexc("#D8D8B0"), hexc("#A0A080")
    pk = B["pack"]
    paint_box(sh, pk, brown, {"top": brown_hi, "bottom": brown_lo}, seams=False)
    for face in ("front", "back", "left", "right"):
        x, y, w, h = faces(pk)[face]
        sh.fill(x, y, w, h, brown)
        sh.fill(x, y, w, 1, brown_hi)
        sh.fill(x, y + h - 1, w, 1, brown_lo)
        sh.fill(x + w - 1, y, 1, h, brown_lo)
        sh.fill(x, y + h // 2, w, 1, brown_lo)  # canvas seam row
    for face in ("left", "right"):
        # the lighter gray strip inset on each side face
        x, y, w, h = faces(pk)[face]
        sh.fill(x + 1, y + 1, w - 2, h - 2, frame)
        sh.fill(x + 1, y + 1, w - 2, 1, frame_hi)
        sh.fill(x + 1, y + h - 2, w - 2, 1, frame_lo)
    for n, b in B.items():
        if n == "pack":
            continue
        if "sail" in n:
            paint_box(sh, b, frame, seams=False)
            for face in ("front", "back"):
                x, y, w, h = faces(b)[face]
                sh.fill(x, y, w, h, sail)
                sh.fill(x, y, w, 1, sail_hi)
                sh.fill(x, y + h - 1, w, 1, sail_lo)
                sh.fill(x + w // 2, y, 1, h, sail_lo)
        else:
            paint_box(sh, b, frame, seams=False)
            for face, (x, y, w, h) in faces(b).items():
                sh.fill(x, y, w, 1, frame_hi)
                sh.fill(x, y + h - 1, w, 1, frame_lo)
    return sh


PAINTERS = {
    "gas_mask": gas_mask, "oxygen_mask": oxygen_mask, "tactical_mask": tactical_mask,
    "night_vision_goggles": goggles, "oxygen_tanks": oxygen_tanks, "jump_pack": jump_pack,
    "anti_gravity_pack": anti_gravity, "glider": glider,
}


def main():
    os.makedirs(OUT, exist_ok=True)
    rows, made = [], []
    names = sorted(n for n in os.listdir(LAYOUTS) if n.endswith(".json"))
    for name in names:
        layout = json.load(open(os.path.join(LAYOUTS, name), encoding="utf-8"))
        for tex in layout["textures"]:
            assert tex in OWNED, tex
            rng = np.random.default_rng(seed_for("accessory." + tex))
            sheet = PAINTERS[layout["name"]](layout, rng, tex)
            im = sheet.image()
            assert im.size == (layout["textureWidth"], layout["textureHeight"])
            im.save(os.path.join(OUT, tex + ".png"))
            made.append((tex, im))
            rows.append(["model.armor." + tex, "src/main/resources/assets/gregguns/textures/models/armor/" + tex + ".png",
                         "tools/asset-gen/accessory_models.py", seed_for("accessory." + tex),
                         "Procedural numpy/Pillow: flat shaded box faces, edge highlights, panel seams and a few details "
                         "painted onto the unwrap of the Java accessory model", "MIT", "2026-10-18"])
    with open(os.path.join(HERE, "credits", "accessory_models.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["asset", "path", "generator", "seed", "method", "license", "date"])
        w.writerows(rows)
    lines = ["# Accessory model layouts", "",
             "Generated by `tools/asset-gen/accessory_models.py` from the Java models (`AccessoryDefs`). Units are texels; "
             "`u v` is the unwrap origin the sheet packer chose, boxes unwrap the vanilla way.", ""]
    for name in names:
        lay = json.load(open(os.path.join(LAYOUTS, name), encoding="utf-8"))
        lines.append("## %s (%dx%d, textures: %s)" % (lay["name"], lay["textureWidth"], lay["textureHeight"],
                                                      ", ".join(lay["textures"])))
        for b in lay["boxes"]:
            lines.append("- `%s` u=%d v=%d w=%d h=%d d=%d" % (b["name"], b["uv"][0], b["uv"][1], *b["size"]))
        lines.append("")
    with open(os.path.join(HERE, "accessory_layouts.md"), "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines))
    os.makedirs(PREVIEW, exist_ok=True)
    cols, k = 5, 4
    rows_n = (len(made) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * (64 * k + 8), rows_n * (64 * k + 8)), (60, 62, 68, 255))
    for i, (tex, im) in enumerate(made):
        big = im.resize((64 * k, 64 * k), Image.NEAREST)
        sheet.alpha_composite(big, ((i % cols) * (64 * k + 8) + 4, (i // cols) * (64 * k + 8) + 4))
    sheet.convert("RGB").save(os.path.join(PREVIEW, "accessories.png"))
    print("accessory textures:", len(made))


if __name__ == "__main__":
    main()
