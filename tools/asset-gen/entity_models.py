#!/usr/bin/env python3
"""GregGuns mob models: the box DSL, UV packing, painter and preview for data-driven mob models.

A mob model is plain data (assets/gregguns/entity_models/<name>.json) plus a sheet (textures/entity/<name>.png).
The game builds ModelRenderer parts from the JSON, so adding a mob means writing a function here, not Java.
The format is described in tools/asset-gen/entity_models.md.

Quick start (see demo_biped() below):

    m = Mob("my_mob", 64, 64, biped=True)
    head = m.part("head", pivot=(0, 0, 0))
    head.box("skull", -4, -8, -4, 8, 8, 8, material="skin")
    ...
    m.look("head")
    m.swing("rightLeg", "x", 80.2, phase=0)

Method: boxes are authored in Minecraft model units (1/16 block, Y down, the mob faces -Z, its right side is -X).
A part's pivot is relative to its parent's pivot, a box corner relative to its part's pivot. The packer gives every
box without an explicit `uv` a free rectangle on the sheet; boxes sharing a `group` share one rectangle (a mirrored
left limb reuses the right limb's). The painter fills each box's six faces from a per-material palette with edge
bevels and fine noise, then an optional `detail` hook adds faces and marks.

Original, procedural art. Nothing here reads, traces or copies a Techguns asset.
Run from the repo root:  python -I -X utf8 tools/asset-gen/entity_models.py
Outputs: src/main/resources/assets/gregguns/entity_models/*.json, textures/entity/<sheets>.png,
         tools/asset-gen/credits/entity_models.csv, docs/assets/previews/entity_models.png
Only the JSON needs the standard library; painting and the preview import Pillow when they run.
For the unit test:  python -I -X utf8 tools/asset-gen/entity_models.py --emit-json <dir>
"""
import csv
import hashlib
import json
import math
import os
import random
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
RES = os.path.join(ROOT, "src", "main", "resources", "assets", "gregguns")
TEXDIR = os.path.join(RES, "textures", "entity")
MODELDIR = os.path.join(RES, "entity_models")
PREVIEW = os.path.join(ROOT, "docs", "assets", "previews", "entity_models.png")
CREDITS = os.path.join(HERE, "credits", "entity_models.csv")

BIPED_PARTS = ("head", "body", "rightArm", "leftArm", "rightLeg", "leftLeg")
AXES = {"x": 0, "y": 1, "z": 2}


def _num(v):
    """JSON number: integral values are written as ints so the files stay short and stable."""
    v = round(float(v), 4)
    return int(v) if v == int(v) else v


def _vec(v):
    return [_num(x) for x in v]


def seed_for(key):
    return int(hashlib.sha1(key.encode()).hexdigest()[:8], 16)


def hx(s):
    return tuple(int(s[i:i + 2], 16) for i in (1, 3, 5))


# ---------------------------------------------------------------------------------------------- geometry DSL
class Part:
    """One node of the part tree. Make parts with Mob.part(), boxes with Part.box()."""

    def __init__(self, mob, name, parent, pivot, rotate, fullbright):
        self.mob, self.name, self.parent = mob, name, parent
        self.pivot, self.rotate, self.fullbright = tuple(pivot), tuple(rotate), fullbright
        self.boxes = []

    def box(self, name, x, y, z, w, h, d, uv=None, inflate=0.0, mirror=False, material="", group=None):
        """Add a box. (x, y, z) is the corner relative to this part's pivot; (w, h, d) are whole units.

        uv=None lets the packer place it; `group` names a shared rectangle (give the mirrored copy the same group
        and mirror=True). An explicit uv=(u, v) pins the box, used to match a known layout such as the vanilla
        biped sheet."""
        self.boxes.append({"name": name, "offset": (x, y, z), "size": (w, h, d), "uv": uv, "inflate": inflate,
                           "mirror": mirror, "material": material,
                           "group": group or ("%s/%s" % (self.name, name))})
        return self

    def to_json(self):
        out = {"name": self.name}
        if self.parent is not None:
            out["parent"] = self.parent
        out["pivot"] = _vec(self.pivot)
        out["rotate"] = _vec(self.rotate)
        if self.fullbright:
            out["fullbright"] = True
        out["boxes"] = [{"name": b["name"], "uv": list(b["uv"]), "offset": _vec(b["offset"]), "size": list(b["size"]),
                         "inflate": _num(b["inflate"]), "mirror": b["mirror"], "material": b["material"]}
                        for b in self.boxes]
        return out


class Mob:
    """A mob model: sheet size, part tree, animation list, and the painter that fits the sheet to the boxes."""

    def __init__(self, name, width, height, biped=False):
        self.name, self.W, self.H, self.biped = name, width, height, biped
        self.parts = []
        self.animations = []
        self.palettes = {}  # sheet key -> {material: "#rrggbb"}; the first is the default sheet
        self.detail = None  # optional detail(mob, key, put_pixel, face_rect) hook, see paint()

    # -- parts
    def part(self, name, parent=None, pivot=(0, 0, 0), rotate=(0, 0, 0), fullbright=False):
        """Add a part. `pivot` is relative to the parent's pivot; `rotate` is the rest rotation in degrees."""
        if any(p.name == name for p in self.parts):
            raise ValueError("%s: duplicate part '%s'" % (self.name, name))
        if parent is not None and not any(p.name == parent for p in self.parts):
            raise ValueError("%s: part '%s' names parent '%s' which is not defined yet (define parents first)"
                             % (self.name, name, parent))
        p = Part(self, name, parent, pivot, rotate, fullbright)
        self.parts.append(p)
        return p

    def get(self, name):
        for p in self.parts:
            if p.name == name:
                return p
        raise KeyError("%s: no part '%s'" % (self.name, name))

    # -- animations (angles in degrees; see the .md for the formulas)
    def look(self, part, yaw=1.0, pitch=1.0, max_yaw=None, max_pitch=None):
        """Head turn: adds clamp(netHeadYaw) * yaw to Y and clamp(headPitch) * pitch to X."""
        a = {"type": "look", "part": part}
        for k, v in (("yaw", yaw), ("pitch", pitch)):
            if v != 1.0:
                a[k] = _num(v)
        if max_yaw is not None:
            a["maxYaw"] = _num(max_yaw)
        if max_pitch is not None:
            a["maxPitch"] = _num(max_pitch)
        self.animations.append(a)

    def swing(self, part, axis, amplitude, speed=None, phase=0.0):
        """Walk cycle: adds amplitude * limbSwingAmount * cos(limbSwing * speed + phase) on the axis.
        speed defaults to the vanilla 0.6662 rad per unit of limb swing."""
        a = {"type": "swing", "part": part, "axis": axis, "amplitude": _num(amplitude)}
        if speed is not None:
            a["speed"] = _num(speed)
        if phase:
            a["phase"] = _num(phase)
        self.animations.append(a)

    def idle(self, part, axis, amplitude, speed=0.1, phase=0.0, offset=0.0):
        """Idle drift: adds offset + amplitude * sin(ageInTicks * speed + phase)."""
        a = {"type": "idle", "part": part, "axis": axis, "amplitude": _num(amplitude), "speed": _num(speed)}
        if phase:
            a["phase"] = _num(phase)
        if offset:
            a["offset"] = _num(offset)
        self.animations.append(a)

    def spin(self, part, axis, rate):
        """Constant rotation: adds rate degrees per tick about the axis."""
        self.animations.append({"type": "spin", "part": part, "axis": axis, "rate": _num(rate)})

    def pose(self, part, when, x=None, y=None, z=None, mode="add", follow_yaw=0.0, follow_pitch=0.0, blend=None,
             window=None):
        """Angles that apply while `when` is aiming, attacking or holding.
        mode='add' adds them, mode='set' replaces the axes given (blended by the weight).
        follow_yaw / follow_pitch add that share of the head yaw / pitch to Y / X.
        blend (attacking only): step, cosine or sine; window: [from, to] attack progress."""
        a = {"type": "pose", "part": part, "when": when}
        if mode != "add":
            a["mode"] = mode
        ang = {k: _num(v) for k, v in (("x", x), ("y", y), ("z", z)) if v is not None}
        if ang:
            a["angles"] = ang
        if follow_yaw:
            a["followYaw"] = _num(follow_yaw)
        if follow_pitch:
            a["followPitch"] = _num(follow_pitch)
        if blend:
            a["blend"] = blend
        if window:
            a["window"] = [_num(window[0]), _num(window[1])]
        self.animations.append(a)

    # -- uv packing
    def all_boxes(self):
        for p in self.parts:
            for b in p.boxes:
                yield p, b

    def pack(self):
        """Give every box a uv. Pinned boxes keep theirs; the rest take the first free rectangle, tallest first."""
        occupied = [[False] * self.W for _ in range(self.H)]

        def foot(b):
            w, h, d = b["size"]
            return 2 * (d + w), d + h

        def mark(u, v, fw, fh, who):
            if u < 0 or v < 0 or u + fw > self.W or v + fh > self.H:
                raise ValueError("%s: box %s needs %dx%d at %d,%d which leaves the %dx%d sheet"
                                 % (self.name, who, fw, fh, u, v, self.W, self.H))
            for yy in range(v, v + fh):
                for xx in range(u, u + fw):
                    occupied[yy][xx] = True

        group_uv = {}
        for p, b in self.all_boxes():
            if b["uv"] is not None:
                b["uv"] = tuple(b["uv"])
                fw, fh = foot(b)
                if b["group"] not in group_uv:
                    group_uv[b["group"]] = b["uv"]
                    mark(b["uv"][0], b["uv"][1], fw, fh, "%s/%s" % (p.name, b["name"]))
        todo = {}
        for p, b in self.all_boxes():
            if b["uv"] is None and b["group"] not in group_uv:
                todo.setdefault(b["group"], b)
        order = sorted(todo.values(), key=lambda b: (-foot(b)[1], -foot(b)[0], b["group"]))
        for b in order:
            fw, fh = foot(b)
            spot = None
            for v in range(0, self.H - fh + 1):
                for u in range(0, self.W - fw + 1):
                    if not any(occupied[yy][xx] for yy in range(v, v + fh) for xx in range(u, u + fw)):
                        spot = (u, v)
                        break
                if spot:
                    break
            if spot is None:
                raise ValueError("%s: sheet %dx%d is full, no room for %s (%dx%d)"
                                 % (self.name, self.W, self.H, b["group"], fw, fh))
            mark(spot[0], spot[1], fw, fh, b["group"])
            group_uv[b["group"]] = spot
        for _, b in self.all_boxes():
            b["uv"] = group_uv[b["group"]]

    # -- output
    def to_json(self):
        self.pack()
        out = {"name": self.name, "textureWidth": self.W, "textureHeight": self.H}
        if self.biped:
            out["biped"] = True
        out["parts"] = [p.to_json() for p in self.parts]
        out["animations"] = self.animations
        return out

    def write_json(self, directory):
        os.makedirs(directory, exist_ok=True)
        path = os.path.join(directory, self.name + ".json")
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(self.to_json(), f, indent=1)
            f.write("\n")
        return path

    # -- painting
    def sheet_keys(self):
        return list(self.palettes) or [self.name]

    def paint(self, key=None):
        """Paint a sheet fitted to the boxes: PIL RGBA image. Unused sheet area is transparent."""
        from PIL import Image
        self.pack()
        key = key or self.sheet_keys()[0]
        pal = self.palettes.get(key, {})
        rng = random.Random(seed_for("entity_models." + key))
        img = Image.new("RGBA", (self.W, self.H), (0, 0, 0, 0))
        px = img.load()
        done = set()

        def put(x, y, color):
            if 0 <= x < self.W and 0 <= y < self.H:
                px[x, y] = (color[0], color[1], color[2], 255)

        for p, b in self.all_boxes():
            if b["group"] in done:
                continue
            done.add(b["group"])
            base = hx(pal.get(b["material"], pal.get("default", "#808080")))
            for face, (x, y, w, h) in face_rects(b).items():
                for yy in range(h):
                    for xx in range(w):
                        shade = 1.0 + 0.05 * (rng.random() - 0.5)
                        if w >= 3 and h >= 3:  # edge bevel: lit top-left, shadowed bottom-right
                            if yy == 0:
                                shade *= 1.10
                            elif yy == h - 1:
                                shade *= 0.86
                            if xx == 0:
                                shade *= 1.05
                            elif xx == w - 1:
                                shade *= 0.92
                        put(x + xx, y + yy, [max(0, min(255, int(c * shade + 0.5))) for c in base])
        if self.detail is not None:
            self.detail(self, key, put, lambda box, face: face_rects(box)[face])
        return img


def face_rects(b):
    """The six faces of a box's unwrap as (x, y, w, h) in sheet pixels: the standard Minecraft layout."""
    u, v = b["uv"]
    w, h, d = b["size"]
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


# ---------------------------------------------------------------------------------------------- preview
def _rot(v, rot):
    """Apply a part's rotation as ModelRenderer does: Z, then Y, then X are the GL calls, so X acts first."""
    x, y, z = v
    rx, ry, rz = (math.radians(a) for a in rot)
    y, z = y * math.cos(rx) - z * math.sin(rx), y * math.sin(rx) + z * math.cos(rx)
    x, z = x * math.cos(ry) + z * math.sin(ry), -x * math.sin(ry) + z * math.cos(ry)
    x, y = x * math.cos(rz) - y * math.sin(rz), x * math.sin(rz) + y * math.cos(rz)
    return (x, y, z)


def world_boxes(mob):
    """Every box's eight corners in model space at the rest pose, with its part and material."""
    by_name = {p.name: p for p in mob.parts}

    def to_model(p, v):
        while True:
            v = _rot(v, p.rotate)
            v = (v[0] + p.pivot[0], v[1] + p.pivot[1], v[2] + p.pivot[2])
            if p.parent is None:
                return v
            p = by_name[p.parent]

    out = []
    for p in mob.parts:
        for b in p.boxes:
            x, y, z = b["offset"]
            w, h, d = b["size"]
            i = b["inflate"]
            lo, hi = (x - i, y - i, z - i), (x + w + i, y + h + i, z + d + i)
            corners = [to_model(p, (cx, cy, cz)) for cx in (lo[0], hi[0]) for cy in (lo[1], hi[1])
                       for cz in (lo[2], hi[2])]
            out.append((p, b, corners))
    return out


def preview(mob, key=None, scale=6):
    """Front, side and top views of the rest pose (flat shaded boxes) beside the painted sheet. PIL image."""
    from PIL import Image, ImageDraw
    key = key or mob.sheet_keys()[0]
    pal = mob.palettes.get(key, {})
    boxes = world_boxes(mob)
    pts = [c for _, _, cs in boxes for c in cs]
    if not pts:
        return Image.new("RGB", (8, 8))
    xs, ys, zs = [c[0] for c in pts], [c[1] for c in pts], [c[2] for c in pts]
    pad = 4
    views = {  # name: (screen x, screen y, depth) from a model-space point; depth larger = farther
        "front": lambda c: (c[0], c[1], c[2]),
        "side": lambda c: (c[2], c[1], c[0]),
        "top": lambda c: (c[0], c[2], c[1]),
    }
    quads = [(0, 1, 3, 2), (4, 5, 7, 6), (0, 1, 5, 4), (2, 3, 7, 6), (0, 2, 6, 4), (1, 3, 7, 5)]
    tiles = []
    for name, f in views.items():
        proj = [f(c) for c in pts]
        minx, maxx = min(p[0] for p in proj), max(p[0] for p in proj)
        miny, maxy = min(p[1] for p in proj), max(p[1] for p in proj)
        w, h = int((maxx - minx + 2 * pad) * scale), int((maxy - miny + 2 * pad) * scale)
        im = Image.new("RGB", (w, h), (46, 50, 56))
        dr = ImageDraw.Draw(im)
        faces = []
        for p, b, cs in boxes:
            base = hx(pal.get(b["material"], pal.get("default", "#808080")))
            for qi, q in enumerate(quads):
                pr = [f(cs[i]) for i in q]
                depth = sum(v[2] for v in pr) / 4.0
                shade = (1.0, 0.85, 0.95, 0.7, 0.9, 0.75)[qi]
                faces.append((depth, [((v[0] - minx + pad) * scale, (v[1] - miny + pad) * scale) for v in pr],
                              tuple(int(c * shade) for c in base)))
        for _, poly, col in sorted(faces, key=lambda t: -t[0]):
            dr.polygon(poly, fill=col, outline=(20, 22, 26))
        tiles.append(im)
    sheet = mob.paint(key).resize((mob.W * 4, mob.H * 4), Image.NEAREST)
    tiles.append(sheet.convert("RGB"))
    height = max(t.height for t in tiles)
    out = Image.new("RGB", (sum(t.width for t in tiles) + 8 * len(tiles), height + 8), (30, 32, 36))
    x = 4
    for t in tiles:
        out.paste(t, (x, 4))
        x += t.width + 8
    return out


# ---------------------------------------------------------------------------------------------- the demo
def vanilla_biped(name, width=64, height=32):
    """A biped with vanilla proportions and the vanilla biped animations (walk, look, held-item and aimed pose).
    Start a new humanoid from this and add or reshape boxes; the animations already reference the six parts."""
    m = Mob(name, width, height, biped=True)
    m.part("head").box("skull", -4, -8, -4, 8, 8, 8, uv=(0, 0), material="skin")
    m.part("body").box("torso", -4, 0, -2, 8, 12, 4, uv=(16, 16), material="shirt")
    m.part("rightArm", pivot=(-5, 2, 0)).box("arm", -3, -2, -2, 4, 12, 4, uv=(40, 16), material="shirt",
                                             group="arm")
    m.part("leftArm", pivot=(5, 2, 0)).box("arm", -1, -2, -2, 4, 12, 4, uv=(40, 16), material="shirt", mirror=True,
                                           group="arm")
    m.part("rightLeg", pivot=(-1.9, 12, 0)).box("leg", -2, 0, -2, 4, 12, 4, uv=(0, 16), material="pants",
                                                group="leg")
    m.part("leftLeg", pivot=(1.9, 12, 0)).box("leg", -2, 0, -2, 4, 12, 4, uv=(0, 16), material="pants", mirror=True,
                                              group="leg")
    # Vanilla ModelBiped motion in degrees: legs swing 1.4 rad, arms 1.0 rad, the left half in antiphase.
    m.look("head")
    m.swing("rightLeg", "x", 80.214)
    m.swing("leftLeg", "x", 80.214, phase=180)
    m.swing("rightArm", "x", 57.296, phase=180)
    m.swing("leftArm", "x", 57.296)
    # Idle sway: z = +-(0.05 + 0.05 cos(age 0.09)), x = +-0.05 sin(age 0.067), mirrored left to right.
    m.idle("rightArm", "z", 2.865, speed=0.09, phase=90, offset=2.865)
    m.idle("leftArm", "z", -2.865, speed=0.09, phase=90, offset=-2.865)
    m.idle("rightArm", "x", 2.865, speed=0.067)
    m.idle("leftArm", "x", -2.865, speed=0.067)
    # Holding any item: right arm tips forward 18 degrees (the swing stays full size here).
    m.pose("rightArm", "holding", x=-18)
    # Aiming a gun: both arms level, converging on the weapon, following the head pitch and yaw.
    m.pose("rightArm", "aiming", x=-90, y=-5.73, z=0, mode="set", follow_yaw=1, follow_pitch=1)
    m.pose("leftArm", "aiming", x=-90, y=28.65, z=0, mode="set", follow_yaw=1, follow_pitch=1)
    # Attacking: a body twist and a right-arm sweep, out and back.
    m.pose("body", "attacking", y=11.5, blend="sine")
    m.pose("rightArm", "attacking", x=-70, blend="sine")
    return m


def demo_biped():
    """The plain biped the tests use: vanilla proportions plus one fullbright visor part (to exercise the flag)."""
    m = vanilla_biped("demo_biped")
    m.part("visor", parent="head", fullbright=True).box("slit", -3, -5, -4.5, 6, 1, 1, uv=(32, 0), material="glow")
    m.palettes["demo_biped"] = {"default": "#808080", "skin": "#c89a78", "shirt": "#3f6fa8", "pants": "#35486b",
                                "glow": "#9be8ff"}

    def detail(mob, key, put, rect):
        head = mob.get("head").boxes[0]
        x, y, w, h = rect(head, "front")
        for ex in (1, w - 2):  # two dark eyes and a mouth line on the face
            put(x + ex, y + 3, hx("#241c18"))
        for mx in range(2, w - 2):
            put(x + mx, y + 6, hx("#8a5a4a"))

    m.detail = detail
    return m


MODELS = [demo_biped]


# ---------------------------------------------------------------------------------------------- main
def main(argv):
    if len(argv) >= 3 and argv[1] == "--emit-json":
        for fn in MODELS:
            fn().write_json(argv[2])
        return 0
    os.makedirs(TEXDIR, exist_ok=True)
    rows, previews = [], []
    for fn in MODELS:
        m = fn()
        path = m.write_json(MODELDIR)
        used = max(v + b["size"][2] + b["size"][1] for _, b in m.all_boxes() for v in [b["uv"][1]])
        print("%-12s %d parts, %d boxes, %d animations, %dx%d sheet (%d rows used) -> %s"
              % (m.name, len(m.parts), sum(len(p.boxes) for p in m.parts), len(m.animations), m.W, m.H, used,
                 os.path.relpath(path, ROOT)))
        for key in m.sheet_keys():
            m.paint(key).save(os.path.join(TEXDIR, key + ".png"))
            rows.append(["entity.model." + key, "src/main/resources/assets/gregguns/textures/entity/%s.png" % key,
                         "tools/asset-gen/entity_models.py", seed_for("entity_models." + key),
                         "Procedural Pillow: sheet painted to fit the box model assets/gregguns/entity_models/%s.json; "
                         "flat material colors, edge bevels, fine noise" % m.name, "MIT", "2026-10-19"])
        previews.append(preview(m))
    os.makedirs(os.path.dirname(CREDITS), exist_ok=True)
    with open(CREDITS, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["asset", "path", "generator", "seed", "method", "license", "date"])
        w.writerows(rows)
    if previews:
        from PIL import Image
        wide = max(p.width for p in previews)
        sheet = Image.new("RGB", (wide, sum(p.height for p in previews)), (30, 32, 36))
        y = 0
        for p in previews:
            sheet.paste(p, (0, y))
            y += p.height
        os.makedirs(os.path.dirname(PREVIEW), exist_ok=True)
        sheet.save(PREVIEW)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
