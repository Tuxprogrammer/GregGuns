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
            if b["material"] in pal and pal[b["material"]] is None:
                continue  # a palette entry of None leaves the box unpainted (transparent): cut-out planes
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
            base = hx(pal.get(b["material"], pal.get("default", "#808080")) or "#584124")
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


# ---------------------------------------------------------------------------------------------- the cyberdemon
def _lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t + 0.5) for i in range(3))


def cyberdemon():
    """Skinless red demon with cheap cybernetics.

    Built from docs/techguns-spec/10-entities-npcs/model-notes/cyberdemon.md (clean room: notes only).
    Six vanilla biped pivots. Chest 12x7x6 over a 9x5x5 pelvis, 8x8x7 skull with a 6x5x2 snout and two temple
    implants, bull horns (a base yawed 40 degrees out and forward, a tip pitched 25 degrees up), 5x7x5 upper arms
    flush against the chest with 4x8x4 forearms pitched 30 degrees forward, and Z-shaped legs (thigh and shin both
    pitched 30 degrees, shin shifted back) ending in flat 5x3x5 hooves. Sheet 64x64, one pixel per model unit.
    Horn direction follows the notes' conclusion: out, then forward and up (not swept back).
    The horn tip pivot sits 0.6 higher than the notes (y -4.9) so the tips stay under the skull top (y -8).
    No fullbright part: the notes want only a painted bright-red lens and no glow pass.
    """
    m = Mob("cyberdemon", 64, 64, biped=True)
    # head: skull shifted 1 unit back (z -3..4), snout on the lower front face, temple implants, bull horns
    h = m.part("head")
    h.box("skull", -4, -8, -3, 8, 8, 7, material="skull")
    h.box("snout", -3, -5, -5, 6, 5, 2, material="snout")
    for side, sx in (("r", -1), ("l", 1)):
        mir = side == "l"
        h.box("implant_" + side, 4 if sx > 0 else -6, -7, -2, 2, 4, 4, material="implant", mirror=mir,
              group="implant")
        m.part("horn_base_" + side, parent="head", pivot=(6 * sx, -5, 1.5), rotate=(0, 40 * sx, 0)).box(
            "horn_base_" + side, -1 if sx > 0 else -3, -1.5, -1.5, 4, 3, 3, material="horn", mirror=mir,
            group="horn_base")
        m.part("horn_tip_" + side, parent="head", pivot=(7.5 * sx, -4.9, -1.5), rotate=(-25, 0, 0)).box(
            "horn_tip_" + side, -1, -1, -5, 2, 2, 6, material="horn_tip", mirror=mir, group="horn_tip")
    # torso: chest wider than the pelvis, V-shaped
    b = m.part("body")
    b.box("chest", -6, 0, -3.5, 12, 7, 7, material="chest")
    b.box("pelvis", -4.5, 7, -2.5, 9, 5, 5, material="pelvis")
    # arms: upper arm flush with the chest, forearm a fixed 30 degree forward offset inside the swinging arm node
    for side, sx in (("r", -1), ("l", 1)):
        mir = side == "l"
        arm = "rightArm" if sx < 0 else "leftArm"
        m.part(arm, pivot=(5 * sx, 2, 0)).box("upper_arm_" + side, 1 if sx > 0 else -6, -2, -2.5, 5, 7, 5,
                                              material="upper_arm", mirror=mir, group="upper_arm_l" if mir else "upper_arm")
        m.part("forearm_" + side, parent=arm, rotate=(-30, 0, 0)).box(
            "forearm_" + side, 1.5 if sx > 0 else -5.5, 1, 0.5, 4, 9, 4, material="forearm", mirror=mir,
            group="forearm")
    # legs: thigh and shin pitched 30 degrees forward, shin shifted 1.5 back, flat unrotated hoof
    for side, sx in (("r", -1), ("l", 1)):
        mir = side == "l"
        leg = "rightLeg" if sx < 0 else "leftLeg"
        lx = -1.4 if sx > 0 else -3.6  # thigh and hoof: centered 3 units from the centerline
        m.part(leg, pivot=(1.9 * sx, 12, 0)).box("hoof_" + side, lx, 9, -4, 5, 3, 5, material="hoof", mirror=mir,
                                                 group="hoof")
        m.part("thigh_" + side, parent=leg, rotate=(-30, 0, 0)).box(
            "thigh_" + side, lx, -1, -3.5, 5, 7, 6, material="thigh", mirror=mir, group="thigh")
        m.part("shin_" + side, parent=leg, rotate=(-30, 0, 0)).box(
            "shin_" + side, -0.9 if sx > 0 else -3.1, 3, 1.5, 4, 7, 4, material="shin", mirror=mir, group="shin")
    # motion: vanilla biped walk (legs 1.4 rad, arms 1.0 rad), idle sway, held-item tilt, melee twist; no aimed pose
    m.look("head")
    m.swing("rightLeg", "x", 80.214)
    m.swing("leftLeg", "x", 80.214, phase=180)
    m.swing("rightArm", "x", 57.296, phase=180)
    m.swing("leftArm", "x", 57.296)
    m.idle("rightArm", "z", 2.865, speed=0.09, phase=90, offset=2.865)
    m.idle("leftArm", "z", -2.865, speed=0.09, phase=90, offset=-2.865)
    m.idle("rightArm", "x", 2.865, speed=0.067)
    m.idle("leftArm", "x", -2.865, speed=0.067)
    m.pose("rightArm", "holding", x=-18)
    m.pose("body", "attacking", y=11.5, blend="sine")
    m.pose("rightArm", "attacking", x=-70, blend="sine")

    flesh = {  # material: (shadow, base, highlight)
        "skull": ("#6E0E0A", "#B01A12", "#D03A2A"), "snout": ("#6E0E0A", "#AC1A12", "#CC3626"),
        "implant": ("#5A0C08", "#8E140E", "#B52418"), "chest": ("#6E0E0A", "#B81C14", "#D03A2A"),
        "pelvis": ("#6E0E0A", "#B01A12", "#CC3626"), "upper_arm": ("#6E0E0A", "#BA1E16", "#D63E2C"),
        "forearm": ("#6E0E0A", "#B41C14", "#CE382A"), "thigh": ("#6E0E0A", "#B01A12", "#CC3626")}
    pal = {"default": "#B01A12", "horn": "#867E68", "horn_tip": "#827A64", "shin": "#9C7D71", "hoof": "#655F5D"}
    pal.update({k: v[1] for k, v in flesh.items()})
    m.palettes["cyberdemon"] = pal
    steel, steel_hi, steel_lo, outline = hx("#6D6D6D"), hx("#7B7B7B"), hx("#484848"), hx("#202020")
    cable_a, cable_b, cable_dark = hx("#C0875A"), hx("#B8703F"), hx("#7A4A2A")
    faces = ("top", "bottom", "right", "front", "left", "back")

    def detail(mob, key, put, rect):
        rng = random.Random(seed_for("cyberdemon.detail"))
        boxes = {}
        for _, bx in mob.all_boxes():
            boxes.setdefault(bx["group"], bx)  # a mirrored left copy shares the right one's rectangle
        byname = {bx["name"]: bx for bx in boxes.values()}

        def cable(box, face, pts, cols=None):
            x, y, w, h = rect(box, face)
            ca, cb, cd = cols or (cable_a, cable_b, cable_dark)
            for i, (cx, cy) in enumerate(pts):
                if 0 <= cx < w and 0 <= cy < h:
                    col = cd if i == len(pts) - 1 else (ca if i % 2 == 0 else cb)
                    put(x + cx, y + cy, col)

        # raw red flesh: blotchy noise, vertical fibers, rare wet highlights, lit tops and dark undersides
        for name, bx in byname.items():
            mat = bx["material"]
            if mat not in flesh:
                continue
            sh, ba, hi = (hx(c) for c in flesh[mat])
            for face in faces:
                x, y, w, h = rect(bx, face)
                blob = [[rng.random() for _ in range(w + 2)] for _ in range(h + 2)]
                fib = [rng.random() for _ in range(w)]
                bias = {"top": 0.28, "bottom": -0.2, "front": 0.0, "back": -0.06, "left": -0.04, "right": -0.04}[face]
                for yy in range(h):
                    for xx in range(w):
                        smooth = (blob[yy][xx] + blob[yy + 1][xx] + blob[yy][xx + 1] + blob[yy + 1][xx + 1]
                                  + 4 * blob[yy + 1][xx + 1]) / 8.0
                        t = 0.5 * smooth + 0.3 * fib[xx] + 0.2 * rng.random() + bias
                        if t < 0.36:
                            c = _lerp(sh, ba, max(0.0, t / 0.36) * 0.6)
                        elif t < 0.78:
                            c = _lerp(sh, ba, 0.5 + 0.5 * (t - 0.36) / 0.42)
                        else:
                            c = _lerp(ba, hi, min(1.0, (t - 0.78) / 0.25) * 0.9)
                        put(x + xx, y + yy, c)

        # horns: bone tan with faint darker bands and a lighter top
        for name in ("horn_base_r", "horn_tip_r"):
            bx = byname[name]
            base = hx("#867E68" if "base" in name else "#827A64")
            band, lite = hx("#565144"), hx("#948C76")
            for face in faces:
                x, y, w, h = rect(bx, face)
                for yy in range(h):
                    for xx in range(w):
                        c = base
                        if face in ("front", "back", "top", "bottom") and (xx + 2 * yy) % 5 == 4:
                            c = _lerp(base, band, 0.45)
                        if face == "top" and yy < 2:
                            c = lite
                        if face == "bottom" or (yy == h - 1 and face != "top"):
                            c = _lerp(base, band, 0.35)
                        put(x + xx, y + yy, _lerp(c, lite, 0.15) if rng.random() < 0.2 else c)

        # shins: pink-white scuffed plate, two crossing cables and gray brackets
        shin = byname["shin_r"]
        pa, pl, pd = hx("#9C7D71"), hx("#C1A69C"), hx("#73564B")
        for face in faces:
            x, y, w, h = rect(shin, face)
            for yy in range(h):
                for xx in range(w):
                    c = pa
                    r = rng.random()
                    if r < 0.2:
                        c = pl
                    elif r < 0.32:
                        c = pd
                    if face in ("front", "back", "left", "right"):
                        if yy == 0 or yy == h - 1:
                            c = steel_hi if xx % 2 == 0 else steel
                        elif yy == 1 or yy == h - 2:
                            c = pd
                    put(x + xx, y + yy, c)
        cable(shin, "front", [(0, 2), (1, 3), (2, 3), (3, 4), (3, 5)])
        cable(shin, "front", [(3, 2), (2, 3), (1, 3), (0, 4), (0, 5)])
        cable(shin, "back", [(0, 2), (1, 3), (2, 3), (3, 4), (3, 5)])
        cable(shin, "back", [(3, 2), (2, 3), (1, 3), (0, 4), (0, 5)])
        cable(shin, "left", [(0, 2), (1, 3), (2, 3), (3, 4)])
        cable(shin, "right", [(0, 4), (1, 3), (2, 3), (3, 2)])

        # hooves: gray-brown, lighter band on the top, darker toward the sole
        hoof = byname["hoof_r"]
        ha, hl, hd = hx("#655F5D"), hx("#696361"), hx("#332B28")
        for face in faces:
            x, y, w, h = rect(hoof, face)
            for yy in range(h):
                for xx in range(w):
                    c = hl if face == "top" else ha
                    if face in ("front", "back", "left", "right"):
                        c = hl if yy == 0 else (ha if yy == 1 else hd)
                    if face == "bottom":
                        c = hd
                    if rng.random() < 0.18:
                        c = _lerp(c, hd, 0.3)
                    put(x + xx, y + yy, c)

        # head: cyber eye (gray plate, red lens) on the mob's right = left half of the front face, yellow slit on
        # its left, black mouth with white teeth on the snout, an eye-plate cable over the skull top
        x, y, w, h = rect(byname["skull"], "front")
        for yy in range(3):
            for xx in range(4):
                put(x + xx, y + yy, steel_lo if (yy == 2 or xx == 3) else (steel_hi if yy == 0 else steel))
        for yy in range(2):
            for xx in (1, 2):
                put(x + xx, y + yy, hx("#BE0000"))
        put(x + 3, y, hx("#313131"))
        for xx in range(4, 8):  # the yellow slit on the left eye, with a black outline
            put(x + xx, y + 1, hx("#000000"))
        for xx in (5, 6):
            put(x + xx, y, hx("#000000"))
            put(x + xx, y + 2, hx("#000000"))
            put(x + xx, y + 1, hx("#E8C81C"))
        cable(byname["skull"], "top", [(0, 6), (0, 5), (1, 4), (1, 3), (0, 2), (0, 1)])
        x, y, w, h = rect(byname["snout"], "front")
        black, tooth, tooth2 = hx("#000000"), hx("#F2DDDD"), hx("#D9B9B9")
        for xx in range(w):
            put(x + xx, y + 1, tooth if xx % 2 == 0 else tooth2)  # upper teeth
            put(x + xx, y + 2, black)
            put(x + xx, y + 3, tooth if xx % 2 else tooth2)  # lower jaw teeth
            put(x + xx, y + 4, tooth2 if xx % 2 == 0 else hx("#B01A12"))
        put(x + 1, y, hx("#200402"))  # nostrils
        put(x + 4, y, hx("#200402"))

        # chest: two pec lobes over a dark line, ribs below, a steel implant box with a cable, flank bruise
        chest = byname["chest"]
        x, y, w, h = rect(chest, "front")
        for yy in range(h):
            for xx in range(w):
                if xx in (5, 6):
                    put(x + xx, y + yy, hx("#6E0E0A"))
                elif yy < 3 and 1 <= xx <= 10:
                    put(x + xx, y + yy, hx("#D03A2A") if (xx + yy) % 3 else hx("#C0281A"))
                elif yy == 3:
                    put(x + xx, y + yy, hx("#7E100B"))
                elif yy in (4, 6):
                    put(x + xx, y + yy, hx("#7E100B"))
                elif yy == 5:
                    put(x + xx, y + yy, hx("#D03A2A"))
        for yy in range(3):  # implant box, viewer's right = the mob's left
            for xx in range(3):
                edge = yy in (0, 2) or xx in (0, 2)
                put(x + 8 + xx, y + yy, outline if edge else steel_hi)
        put(x + 9, y + 1, steel)
        gray = (hx("#B8B8B8"), hx("#B0B0B0"), hx("#7A7A7A"))
        cable(chest, "front", [(7, 1), (6, 2), (5, 3), (4, 3), (3, 4), (3, 5), (2, 6)], gray)
        x, y, w, h = rect(chest, "left")
        for yy in range(h):
            for xx in range(w):
                if (xx - 2.5) ** 2 / 6.0 + (yy - 3) ** 2 / 9.0 < 1:
                    put(x + xx, y + yy, hx("#6E0E0A") if rng.random() < 0.7 else hx("#8A120C"))

        # pelvis: dark necrotic patch, small pale-gray striped plate on the belly
        pel = byname["pelvis"]
        x, y, w, h = rect(pel, "front")
        for yy in range(1, 5):
            for xx in range(0, 5):
                if (xx - 2) ** 2 / 6.0 + (yy - 2.5) ** 2 / 4.0 < 1:
                    put(x + xx, y + yy, hx("#7A0F0A") if rng.random() < 0.6 else hx("#8A130D"))
        for yy in range(1, 4):
            for xx in range(5, 8):
                put(x + xx, y + yy, hx("#B8B8B8") if yy % 2 else hx("#7A7A7A"))
        cable(pel, "front", [(1, 0), (1, 1)], gray)

        # back: gray implant plates (head, upper back, mid-back) and a dark round socket low on the back
        def plate(box, face, px, py, n):
            x, y, w, h = rect(box, face)
            for yy in range(n):
                for xx in range(n):
                    edge = yy in (0, n - 1) or xx in (0, n - 1)
                    put(x + px + xx, y + py + yy, hx("#9A9A9A") if edge else hx("#6A6A6A"))

        def socket(box, face, px, py):
            x, y, w, h = rect(box, face)
            for yy in range(3):
                for xx in range(3):
                    ring = (xx, yy) != (1, 1)
                    if (xx, yy) in ((0, 0), (2, 0), (0, 2), (2, 2)):
                        continue
                    put(x + px + xx, y + py + yy, hx("#8A8A8A") if ring else hx("#2A2A2A"))

        plate(byname["skull"], "back", 2, 2, 4)
        plate(chest, "back", 8, 1, 4)
        plate(chest, "back", 2, 3, 3)
        socket(pel, "back", 3, 1)
        # side implant: dark socket ringed with gray on the torso's left and the left upper arm
        socket(chest, "left", 2, 2)
        socket(byname["upper_arm_l"], "front" if False else "right", 1, 2)

        # forearm: one cable
        cable(byname["forearm_r"], "front", [(1, 1), (1, 2), (2, 3), (2, 4), (1, 5), (1, 6)])

    m.detail = detail
    return m


def alien_bug():
    """Sand-tan four-legged insect with a gaping toothed snout, a spiky cut-out frill and an upturned tail.

    Built from docs/techguns-spec/10-entities-npcs/model-notes/alien_bug.md (clean room: notes only).
    Generic living mode (not a biped). Absolute positions from the notes, converted to pivot-relative:
    head group (head 5x5x6; upper jaw 4x4x10 pitched up 10, lower jaw 3x4x9 pitched down 26; crest 6x3x8 pitched
    up 20 toward the back; zero-thickness 14x14 frill, alpha cut-out), fixed thorax 5x5x9, three abdomen segments
    (6, 4, 2 wide), and four legs (3x3x9 upper leg rising 28 degrees and leaning 20 degrees toward its end of the
    body, 2x2x11 bone-gray lower leg bent to the ground). Sheet 64x64, nearly full (the notes' unused two thirds
    does not hold: our unwrap packs tight).
    Notes' inconsistency: the head top is y=9 (not 11), so the crest pivot sits at y=9.5 and the frill (tilt 15,
    so its rear edge lands at the notes' y 3.5) rides on it.
    """
    m = Mob("alien_bug", 64, 64)
    # thorax and abdomen: fixed
    m.part("thorax").box("thorax", -2.5, 14, -2, 5, 5, 9, material="thorax")
    m.part("abdomen_1", pivot=(0, 13, 6), rotate=(34, 0, 0)).box("abdomen_1", -3, 0, 0, 6, 4, 4, material="abdomen")
    m.part("abdomen_2", pivot=(0, 11, 10), rotate=(13, 0, 0)).box("abdomen_2", -2, 0, 0, 4, 3, 4, material="abdomen")
    m.part("tail_tip").box("tail_tip", -1, 10.5, 13.6, 2, 2, 2, material="tip")
    # head group: pivot at the head's center; jaws, crest and frill ride on it
    m.part("head", pivot=(0, 11.5, -1)).box("head", -2.5, -2.5, -3, 5, 5, 6, material="head")
    m.part("upper_jaw", parent="head", pivot=(0, -1, -2), rotate=(-10, 0, 0)).box(
        "upper_jaw", -2, -2.5, -10, 4, 4, 10, material="jaw_u")
    m.part("lower_jaw", parent="head", pivot=(0, -1.5, -2.5), rotate=(26, 0, 0)).box(
        "lower_jaw", -1.5, 0, -9, 3, 4, 9, material="jaw_l")
    m.part("crest", parent="head", pivot=(0, -2, -2), rotate=(20, 0, 0)).box(
        "crest", -3, -3, 0, 6, 3, 8, material="crest")
    m.part("frill", parent="head", pivot=(0, -6, 2.5), rotate=(15, 0, 0)).box(
        "frill", -7, 0, -4.5, 14, 0, 14, material="frill")
    # legs: pivot inside the thorax side at y 17; yaw leans toward the leg's end of the body, roll raises it
    for side, sx in (("r", -1), ("l", 1)):
        mir = side == "l"
        for end, z, lean in (("f", 0, 20), ("b", 4, -20)):
            up = "leg_%s%s" % (end, side)
            m.part(up, pivot=(1.5 * sx, 17, z), rotate=(0, lean * sx, -28 * sx)).box(
                up, 0 if sx > 0 else -9, -1.5, -1.5, 9, 3, 3, material="leg_u", mirror=mir, group="leg_u")
            m.part("shin_%s%s" % (end, side), parent=up, pivot=(7.5 * sx, 0, 0), rotate=(0, 0, 100 * sx)).box(
                "shin_%s%s" % (end, side), 0 if sx > 0 else -11, -1, -1, 11, 2, 2, material="leg_l", mirror=mir,
                group="leg_l")
    # motion
    m.look("head")
    m.idle("upper_jaw", "x", 5.73, speed=0.0997)  # +-0.1 rad, period ~63 ticks
    m.idle("lower_jaw", "x", 5.73, speed=0.2027)  # +-0.1 rad, period ~31 ticks
    m.pose("upper_jaw", "attacking", x=17.2, blend="sine", window=[0, 0.6])  # bite: jaws snap shut, then reopen
    m.pose("lower_jaw", "attacking", x=-22.9, blend="sine", window=[0, 0.6])
    for side, sx in (("r", -1), ("l", 1)):
        for end, ph, lift_speed in (("f", 0, 3), ("b", 180, 4)):
            up = "leg_%s%s" % (end, side)
            m.swing(up, "y", -28.65 * sx, speed=2 * 0.6662, phase=ph)  # sweep 0.5 rad, front and rear opposed
            m.swing(up, "x", 22.92 * sx, speed=2 * 0.6662, phase=ph)  # twist 0.4 rad about the leg's length
            m.swing(up, "z", -17.19 * sx, speed=lift_speed * 0.6662)  # lift 0.3 rad, pairs in unison

    tan = {"head": "#B88F51", "jaw_u": "#B8894E", "jaw_l": "#BC9A5C", "crest": "#CAA968", "thorax": "#C29958",
           "abdomen": "#B68D4F", "tip": "#AC723E", "leg_u": "#B78E50", "leg_l": "#8E8877", "frill": None}
    m.palettes["alien_bug"] = dict(tan, default="#B88F51")
    hi, rust_a, rust_b, band_a, band_b = hx("#CAAF71"), hx("#BA7941"), hx("#AC6D38"), hx("#835E2D"), hx("#A36C38")
    stripe_a, stripe_b, cream, foot = hx("#906934"), hx("#8F5A2E"), hx("#F1DCC8"), hx("#D4CDB9")

    def detail(mob, key, put, rect):
        rng = random.Random(seed_for("alien_bug.detail"))
        groups = {}
        for _, bx in mob.all_boxes():
            groups.setdefault(bx["group"], bx)  # a mirrored left copy shares the right one's rectangle
        byname = {bx["name"]: bx for bx in groups.values()}
        chitin = ("head", "upper_jaw", "lower_jaw", "crest", "thorax", "abdomen_1", "abdomen_2", "tail_tip")
        # chunky orange-rust patches, and pale sand speckle on the top faces
        for name in chitin:
            bx = byname[name]
            for face in ("top", "bottom", "right", "front", "left", "back"):
                x, y, w, h = rect(bx, face)
                for _ in range(max(1, w * h // 9)):
                    cx, cy = rng.randrange(w), rng.randrange(h)
                    col = rng.choice((rust_a, rust_b))
                    for dx, dy in ((0, 0), (1, 0), (0, 1)):
                        if cx + dx < w and cy + dy < h:
                            put(x + cx + dx, y + cy + dy, col)
                if face == "top":
                    for xx in range(w):
                        for yy in range(h):
                            if rng.random() < 0.3:
                                put(x + xx, y + yy, hi)

        def bands(bx, cols, colors):
            for face in ("top", "bottom", "front", "back"):
                x, y, w, h = rect(bx, face)
                for i, c in enumerate(cols):
                    if c < w:
                        for yy in range(h):
                            put(x + c, y + yy, colors[i % len(colors)])

        bands(byname["thorax"], (2, 6), (band_a, band_b))
        bands(byname["abdomen_1"], (1, 3), (band_a, band_b))
        bands(byname["abdomen_2"], (1, 2), (band_a, band_b))
        # ")(" bracket marks on the thorax's right side
        x, y, w, h = rect(byname["thorax"], "right")
        for yy in (1, 2, 3):
            put(x + 3, y + yy, band_a)
            put(x + 6, y + yy, band_a)
        put(x + 4, y + 1, band_a)
        put(x + 4, y + 3, band_a)
        # upper legs: warm tan with dark stripes across; lower legs: bone gray, pale tips (columns 0 to 2)
        for face in ("top", "bottom", "front", "back"):
            x, y, w, h = rect(groups["leg_u"], face)
            for i, c in enumerate((2, 5, 7)):
                for yy in range(h):
                    put(x + c, y + yy, (stripe_a, stripe_b)[i % 2])
            x, y, w, h = rect(groups["leg_l"], face)
            for c in range(3):
                for yy in range(h):
                    put(x + c, y + yy, foot)
        # eyes (1x2, near the head end of each side) and cream teeth along the jaw edges
        uj, lj = byname["upper_jaw"], byname["lower_jaw"]
        x, y, w, h = rect(uj, "right")  # unwrap: the right region's left edge is the rear
        for yy in (1, 2):
            put(x + 2, y + yy, (0, 0, 0))
        x, y, w, h = rect(uj, "left")  # the left region's right edge is the rear
        for yy in (1, 2):
            put(x + w - 3, y + yy, (0, 0, 0))
        for face in ("right", "left"):
            x, y, w, h = rect(uj, face)
            for c in range(3, w, 2):
                put(x + c, y + h - 1, cream)
            x, y, w, h = rect(lj, face)
            for c in range(2, w, 2):
                put(x + c, y, cream)
        # frill: dark oval blob with tan spikes on the top and bottom faces, the rest stays transparent
        dark, dark2, spike = hx("#584124"), hx("#4A351C"), hx("#BFA36D")
        fb = byname["frill"]
        for face in ("top", "bottom"):
            x, y, w, h = rect(fb, face)
            for yy in range(h):
                for xx in range(w):
                    dx, dy = xx - 6.5, yy - 6.5
                    if (dx / 3.4) ** 2 + (dy / 2.8) ** 2 <= 1.0:
                        put(x + xx, y + yy, dark2 if (xx + yy) % 5 == 0 else dark)
            for k in range(8):
                ang = math.radians(k * 45 + 22.5)
                for r in range(3, 7 + (k % 2)):
                    px, py = int(round(6.5 + r * math.cos(ang))), int(round(6.5 + r * math.sin(ang)))
                    if 0 <= px < w and 0 <= py < h:
                        put(x + px, y + py, spike if r > 3 else dark)

    m.detail = detail
    return m


def super_mutant():
    """Mustard-yellow gorilla-built brute: one model, three skins (Basic, Heavy, Elite).

    Built from docs/techguns-spec/10-entities-npcs/model-notes/super_mutant.md (clean room: notes only), then
    corrected in round 2 against the orchestrator's comparison.
    Six biped parts at the vanilla pivots except the legs, which sit 1 unit higher (y 11). Head 8x8x8 with a separate
    0.5-inflated hat layer; torso 8x12x4 with 6x7x1 front and back plates and a 9x4x5 belt hanging to y 13; 4x12x4
    arms wearing 5x7x5 shoulder sleeves; 4x12x4 legs wearing bulky
    6x7x6 boots. Sheet 64x64, one pixel per model unit. The plate and hat boxes exist on every tier; Basic leaves
    them transparent, Heavy paints the plates, Elite adds a helmet on the hat layer and pauldrons on the sleeves.
    The skin is bare on every tier (no clothing): muscle lines, a dark loincloth band with a skull buckle, boots.
    No fullbright part.
    Per-tier render data lives in the renderer, not here: scale 1.35 / 1.50 / 1.65, vertical offset 0.55 / 0.75 /
    0.95 block, shadow radius 0.6 / 0.65 / 0.7 block, gun offset (-0.10, 0.30, 0) / (-0.15, 0.30, 0) / (-0.20, 0.40, 0).
    """
    m = Mob("super_mutant", 64, 64, biped=True)
    m.animations = vanilla_biped("x").animations  # vanilla walk, idle sway, held-item tilt, aimed pose, melee swing
    h = m.part("head")
    h.box("skull", -4, -8, -4, 8, 8, 8, material="skin")
    h.box("hat", -4, -8, -4, 8, 8, 8, inflate=0.5, material="hat")
    b = m.part("body")
    b.box("torso", -4, 0, -2, 8, 12, 4, material="skin")
    b.box("plate_front", -3, 1, -3, 6, 7, 1, material="plate_f")
    b.box("plate_back", -3, 1, 2, 6, 7, 1, material="plate_b")
    b.box("belt", -4.5, 9, -2.5, 9, 4, 5, material="belt")
    for side, sx in (("r", -1), ("l", 1)):
        mir = side == "l"
        arm = m.part("rightArm" if sx < 0 else "leftArm", pivot=(5 * sx, 2, 0))
        arm.box("arm_" + side, -3 if sx < 0 else -1, -2, -2, 4, 12, 4, material="skin", mirror=mir, group="arm")
        arm.box("sleeve_" + side, -3.9 if sx < 0 else -1.1, -2.2, -2.5, 5, 7, 5, material="sleeve", mirror=mir,
                group="sleeve")
    for side, sx in (("r", -1), ("l", 1)):
        mir = side == "l"
        leg = m.part("rightLeg" if sx < 0 else "leftLeg", pivot=(1.9 * sx, 11, 0))
        leg.box("leg_" + side, -2, 0, -2.5, 4, 12, 4, material="skin", mirror=mir, group="leg")
        leg.box("boot_" + side, -3.2 if sx < 0 else -2.8, 6, -3.2, 6, 7, 6, material="boot", mirror=mir, group="boot")
    skin_base = "#C8A21E"
    for tier in (1, 2, 3):
        m.palettes["super_mutant_%d" % tier] = {
            "default": skin_base, "skin": skin_base, "boot": "#4A3C2C", "belt": "#2A2620", "plate_f": "#8C8C8C",
            "plate_b": "#8C8C8C", "hat": "#8C8C8C", "sleeve": "#8C8C8C" if tier == 3 else skin_base}
    faces = ("top", "bottom", "right", "front", "left", "back")

    def detail(mob, key, put, rect):
        tier = int(key[-1])
        rng = random.Random(seed_for("super_mutant.detail." + key))
        srng = random.Random(seed_for("super_mutant.skin"))  # the skin art is identical on all three tiers
        by = {}
        for _, bx in mob.all_boxes():
            by.setdefault(bx["name"][:-2] + "_r" if bx["name"][-2:] in ("_l", "_r") else bx["name"], bx)
        S = {k: hx(v) for k, v in (("base", "#C8A21E"), ("hi", "#E0C040"), ("sh", "#8A6A10"), ("line", "#9A7A18"),
                                   ("eye", "#6AA030"), ("mouth", "#3A3418"), ("teeth", "#E4E0C8"))}

        def fill(bx, face, fn):
            x, y, w, h = rect(bx, face)
            for yy in range(h):
                for xx in range(w):
                    put(x + xx, y + yy, fn(xx, yy, w, h))

        def skin_face(bx, face):
            def fn(xx, yy, w, h):
                r = srng.random()
                return S["hi"] if r < 0.12 else S["sh"] if r < 0.2 else S["base"]
            fill(bx, face, fn)

        def pts(bx, face, coords, col="line"):
            x, y, w, h = rect(bx, face)
            for cx, cy in coords:
                if 0 <= cx < w and 0 <= cy < h:
                    put(x + cx, y + cy, S[col])

        # bare muscular skin on head, torso, arms, legs (and the sleeves where they are skin)
        skin_boxes = [by[n] for n in ("skull", "torso", "arm_r", "leg_r")]
        if tier < 3:
            skin_boxes.append(by["sleeve_r"])
        for bx in skin_boxes:
            for face in faces:
                skin_face(bx, face)
        to = by["torso"]  # pecs, six-pack, back muscles
        pts(to, "front", [(3, 0), (3, 1), (3, 2), (4, 0), (4, 1), (4, 2), (1, 3), (2, 3), (5, 3), (6, 3),
                          (3, 4), (4, 4), (3, 5), (4, 5), (3, 6), (4, 6), (3, 7), (4, 7), (3, 8), (4, 8),
                          (2, 5), (5, 5), (2, 7), (5, 7), (1, 4), (6, 4)])
        pts(to, "back", [(3, y) for y in range(12)] + [(1, 2), (2, 3), (5, 3), (6, 2), (1, 5), (2, 6), (5, 6),
                                                         (6, 5), (2, 9), (5, 9)])
        for side in ("arm_r",):  # biceps / triceps definition, same art on the mirrored arm
            pts(by[side], "front", [(1, 2), (2, 2), (1, 3), (2, 3), (1, 5), (2, 5), (1, 6), (1, 7), (2, 8)])
            pts(by[side], "back", [(1, 3), (2, 3), (1, 5), (2, 6), (1, 8)])
            pts(by[side], "right", [(1, 4), (2, 4), (2, 7), (1, 9)])
        pts(by["leg_r"], "front", [(1, 3), (2, 3), (1, 7), (2, 7)])
        # head: green eyes, dark open mouth with pale teeth, faint swirls and scars on the sides
        sk = by["skull"]
        x, y, w, h = rect(sk, "front")
        for xx in (1, 5):
            put(x + xx, y + 3, S["eye"])
            put(x + xx + 1, y + 3, S["eye"])
            put(x + xx, y + 2, S["sh"])
            put(x + xx + 1, y + 2, S["sh"])
        put(x + 3, y + 4, S["line"])
        put(x + 4, y + 4, S["line"])
        for xx in range(2, 6):
            for yy in (5, 6):
                put(x + xx, y + yy, S["mouth"])
        for xx in (2, 4):
            put(x + xx, y + 5, S["teeth"])
        for xx in (3, 5):
            put(x + xx, y + 6, S["teeth"])
        for face in ("left", "right"):
            pts(sk, face, [(2, 3), (3, 3), (4, 3), (4, 4), (3, 5), (2, 5), (2, 4), (5, 6), (6, 2)])
        # boots: dark leather with shading, stitched seams
        bt = by["boot_r"]
        lb, ld = hx("#4A3C2C"), hx("#2E261C")
        for face in faces:
            def fn(xx, yy, w, h, face=face):
                if yy == h - 1 or (face in ("front", "back", "left", "right") and yy == 2 and xx % 2 == 0):
                    return ld
                return _lerp(lb, ld, 0.35) if rng.random() < 0.2 else lb
            fill(bt, face, fn)
        # loincloth band (dark), 3 units tall at the front; skull buckle; thin gray belt line on the back
        be = by["belt"]
        for face in faces:
            fill(be, face, lambda xx, yy, w, h: hx("#2A2620") if rng.random() > 0.15 else hx("#332E27"))
        for face in ("front", "left", "right", "back"):
            x, y, w, h = rect(be, face)
            for xx in range(w):
                put(x + xx, y + 3, hx("#6A6A6A") if face == "back" else hx("#2A2620"))
        x, y, w, h = rect(be, "front")
        for xx in (3, 4, 5):
            for yy in (0, 1, 2):
                put(x + xx, y + yy, hx("#D8D8D8"))
        put(x + 3, y + 1, hx("#2A2620"))
        put(x + 5, y + 1, hx("#2A2620"))
        put(x + 4, y + 2, hx("#9A9A9A"))
        x, y, w, h = rect(be, "back")
        for xx in range(w):
            put(x + xx, y + 0, hx("#6A6A6A"))

        stone, st_hi, st_lo, groove = hx("#8C8C8C"), hx("#A8A8A8"), hx("#6A6A6A"), hx("#5A5A5A")

        def stone_face(bx, face):
            def fn(xx, yy, w, h):
                r = rng.random()
                return st_hi if r < 0.15 else st_lo if r < 0.35 else stone
            fill(bx, face, fn)

        def grill(bx, face):  # a recessed panel with horizontal grill slits
            x, y, w, h = rect(bx, face)
            for yy in range(1, h - 1):
                for xx in range(1, w - 1):
                    if xx in (1, w - 2) or yy in (1, h - 2) or yy % 2 == 1:
                        put(x + xx, y + yy, groove)

        if tier >= 2:
            for face in faces:
                stone_face(by["plate_front"], face)
                stone_face(by["plate_back"], face)
            grill(by["plate_front"], "front")
            grill(by["plate_back"], "back")
        if tier == 3:
            for face in faces:
                stone_face(by["hat"], face)
                stone_face(by["sleeve_r"], face)
            x, y, w, h = rect(by["hat"], "top")
            for yy in range(2, h - 2):
                for xx in range(2, w - 2):
                    put(x + xx, y + yy, st_lo)
            for face in ("right", "front", "left", "back"):  # a seam around the dome, seams across the pauldron
                x, y, w, h = rect(by["hat"], face)
                for xx in range(w):
                    put(x + xx, y + 1, groove)
                x, y, w, h = rect(by["sleeve_r"], face)
                for xx in range(w):
                    put(x + xx, y + 3, groove)
            skin_face(by["sleeve_r"], "bottom")  # only the underside shows skin

    m.detail = detail
    plain_paint = m.paint

    def paint(key=None):
        """The base paint, then the regions that must stay see-through for the tier."""
        key = key or m.sheet_keys()[0]
        tier = int(key[-1])
        img = plain_paint(key)
        px = img.load()

        def clear(box, face, sub=None):
            x, y, w, h = face_rects(box)[face]
            for yy in range(h):
                for xx in range(w):
                    if sub is None or sub(xx, yy, w, h):
                        px[x + xx, y + yy] = (0, 0, 0, 0)

        boxes = {b["name"]: b for _, b in m.all_boxes()}
        if tier < 3:
            for face in faces:
                clear(boxes["hat"], face)
        else:  # open dome underneath; a 5x5 opening at the bottom of the front shows the face
            clear(boxes["hat"], "bottom")
            clear(boxes["hat"], "front", lambda xx, yy, w, h: yy >= h - 6 and 2 <= xx <= 6)
        for n in ("plate_front", "plate_back"):
            for face in faces:
                if tier == 1 or face in ("top", "bottom", "left", "right"):
                    clear(boxes[n], face)
        return img

    m.paint = paint
    return m


def biped():
    """The standard biped: vanilla proportions and vanilla motion on a 64x64 logical sheet, shared by every plain
    humanoid skin (zombie_soldier, army_soldier, bandit, dictator). It has no sheet of its own: the skins are painted
    by mob_skins.py, at 64x64 or at an integer multiple, since UVs are fractions."""
    m = vanilla_biped("biped", 64, 64)
    m.no_sheet = True
    return m


def skeleton_soldier():
    """Skeleton Soldier: a vanilla-skeleton look on the shared biped conventions.
    Notes: docs/techguns-spec/10-entities-npcs/model-notes/skeleton_soldier.md. Head and torso are the biped's;
    arms and legs are 2x12x2 sticks (arm pivots stay at +-5 so each arm sits flush against the torso; legs at +-2,
    a 2-unit gap). It wears the vanilla skeleton skin (minecraft:textures/entity/skeleton/skeleton.png, 64x32), so
    the UVs below are the vanilla skeleton layout and no sheet is painted. The hat layer is omitted (it is empty in
    that skin). Motion is the vanilla biped's, as the notes ask. The held-gun offset for MobRenderers.biped is
    (0.1, 0, 0.05) blocks, shadow 0.5, scale 1."""
    m = vanilla_biped("skeleton_soldier", 64, 32)
    m.no_sheet = True
    m.get("head").boxes[0]["material"] = "bone"
    m.get("body").boxes[0]["material"] = "bone"
    for name, ox in (("rightArm", -5), ("leftArm", 5)):
        part = m.get(name)
        part.boxes = []
        part.box("arm", -1, -2, -1, 2, 12, 2, uv=(40, 16), mirror=(name == "leftArm"), material="bone", group="arm")
    for name, px in (("rightLeg", -2), ("leftLeg", 2)):
        part = m.get(name)
        part.pivot = (px, 12, 0)
        part.boxes = []
        part.box("leg", -1, 0, -1, 2, 12, 2, uv=(0, 16), mirror=(name == "leftLeg"), material="bone", group="leg")
    return m


def attack_helicopter():
    """Attack Helicopter boss: olive-drab Apache-style gunship, built at FULL SIZE (render scale 1.0).
    Notes: docs/techguns-spec/10-entities-npcs/model-notes/attack_helicopter.md (clean room: notes only).
    Frame: origin at the rotor axis on the ground (f = blocks forward of the axis, h = blocks above the feet,
    w = blocks sideways): model z = -16 f, y = 24 - 16 h, x = +-16 w. Nose tip at f 6.5, fin tip at f -11.75, h 5.0,
    rotor 296 units (18.5 blocks) across. Parts: `airframe` (fixed hull, flat-faced boxes; the canopy slope, the
    nose taper and the boom rise are stepped), `fin` (a swept plate: a box pitched back 28.8 degrees),
    `rotor` (mast, hub, fore and aft blades; spins -24 deg per tick about the mast, counterclockwise from above)
    with child `rotor_side` (the left and right blades, yawed 90 degrees so all four share one texture rectangle),
    and `chin_gun` (pivot f 4.0, h -0.25, rest pitch 25 degrees nose-down). No tail rotor (the notes make it optional).
    Death sink and spin are entity driven; the gun is level in death (the renderer zeroes its rest pitch)."""
    m = Mob("attack_helicopter", 512, 512)

    def B(part, name, f0, f1, h0, h1, w0, w1, mat, mirror=False, group=None):
        part.box(name, 16 * w0, 24 - 16 * h1, -16 * f1, round(16 * (w1 - w0)), round(16 * (h1 - h0)),
                 round(16 * (f1 - f0)), material=mat, mirror=mirror, group=group)

    def sym(part, name, f0, f1, h0, h1, w0, w1, mat):
        """A pair of boxes, right (-X) and a mirrored left (+X) sharing one rectangle; w0..w1 is the +X range."""
        B(part, name + "_r", f0, f1, h0, h1, -w1, -w0, mat, group=name)
        B(part, name + "_l", f0, f1, h0, h1, w0, w1, mat, mirror=True, group=name)

    a = m.part("airframe")
    B(a, "cabin", -3.5, 2.0, 0.75, 2.75, -0.75, 0.75, "cabin")
    B(a, "deck", -1.75, 2.0, 2.75, 3.0, -0.75, 0.75, "hull")
    B(a, "cowling", -1.75, 2.0, 3.0, 3.25, -0.5, 0.5, "sponson")
    B(a, "cowling_aft", -3.0, -1.75, 2.75, 3.125, -0.5, 0.5, "sponson")
    sym(a, "sponson", -3.25, 0.0, 2.25, 3.0, 0.75, 1.5, "sponson")
    B(a, "belly", -2.75, -0.5, 0.5, 0.75, -0.5, 0.5, "hull")
    # nose tub: 2.0 wide with quarter-block chamfered ends, sill above its front half
    B(a, "tub", 0.0, 4.0, 0.5, 1.25, -1.0, 1.0, "hull")
    B(a, "tub_aft", -0.25, 0.0, 0.5, 1.25, -0.75, 0.75, "hull")
    B(a, "tub_fore", 4.0, 4.25, 0.5, 1.25, -0.75, 0.75, "hull")
    B(a, "sill", 2.0, 4.5, 1.25, 1.75, -0.75, 0.75, "hull")
    # nose cone, 1.5 -> 0.5 square over 2.0 blocks, as four steps, centered at h 1.25
    for i, side in enumerate((22, 18, 12, 8)):
        f0 = 4.5 + 0.5 * i
        B(a, "nose%d" % i, f0, f0 + 0.5, 1.25 - side / 32.0, 1.25 + side / 32.0, -side / 32.0, side / 32.0, "hull")
    # canopy: flat rear top at h 2.875, sloping to 1.875 at the front (five steps)
    B(a, "canopy_rear", 2.0, 3.25, 1.75, 2.875, -0.625, 0.625, "glass")
    for i, top in enumerate((45, 42, 39, 36, 33)):
        B(a, "canopy_s%d" % i, 3.25 + 0.25 * i, 3.5 + 0.25 * i, 1.75, top / 16.0, -0.625, 0.625, "glass")
    # tail
    B(a, "transition", -5.0, -3.5, 1.0, 2.5, -0.75, 0.75, "hull")
    B(a, "boom_fore", -7.875, -5.0, 1.25, 2.25, -0.5, 0.5, "hull")
    B(a, "boom_aft", -10.75, -7.875, 1.5, 2.25, -0.5, 0.5, "hull")
    B(a, "spine", -10.75, -5.0, 2.25, 2.5, -0.25, 0.25, "spine")
    B(a, "spine_fore", -5.25, -3.5, 2.5, 2.75, -0.25, 0.25, "spine")
    B(a, "fin_foot", -10.75, -9.0, 2.5, 2.625, -0.25, 0.25, "hull")
    B(a, "tailplane", -11.0, -10.25, 1.75, 2.0, -2.25, 2.25, "hull")
    # stub wings, pylons and rocket pods
    B(a, "wing", -0.75, 0.5, 1.5, 1.75, -3.0, 3.0, "hull")
    sym(a, "pylon", -0.5, 0.25, 1.25, 1.5, 1.75, 2.0, "hull")
    sym(a, "pod", -1.0, 1.0, 0.5, 1.25, 1.5, 2.25, "pod")
    # swept fin: a plate pitched back from its base centre (f -9.875, h 2.5)
    fin = m.part("fin", parent="airframe", pivot=(0, -16, 158), rotate=(-28.8, 0, 0))
    fin.box("fin", -2, -40, -11, 4, 40, 22, material="fin")
    # rotor: mast and hub on the axis (pivot at the hub's underside, h 4.0), blades 0.5..9.25 blocks from the axis
    r = m.part("rotor", pivot=(0, -40, 0))
    r.box("mast", -5.5, 0, -5.5, 11, 12, 11, material="mast")
    r.box("hub", -17, -8, -17, 34, 8, 34, material="hub")
    r.box("blade_fore", -6, 0, -148, 12, 0, 140, material="blade", group="blade")
    r.box("blade_aft", -6, 0, 8, 12, 0, 140, material="blade", group="blade")
    rs = m.part("rotor_side", parent="rotor", rotate=(0, 90, 0))
    rs.box("blade_left", -6, 0, -148, 12, 0, 140, material="blade", group="blade")
    rs.box("blade_right", -6, 0, 8, 12, 0, 140, material="blade", group="blade")
    m.spin("rotor", "y", -24)
    # chin gun: pivot f 4.0, h -0.25; mount under the nose tub, barrel forward; rest pitch 25 degrees nose-down
    g = m.part("chin_gun", pivot=(0, 28, -64), rotate=(25, 0, 0))
    g.box("mount", -4, -12, -8, 8, 8, 16, material="gun")
    g.box("barrel", -3, -11, -32, 6, 6, 24, material="gun")
    m.look("chin_gun", yaw=1, pitch=0, max_yaw=30)

    # material: (shadow, base, highlight)
    tone = {"hull": ("#2B2F1C", "#3B4127", "#4A5036"), "cabin": ("#32381E", "#3A4026", "#454B31"),
            "fin": ("#32381E", "#3B4127", "#454B31"), "spine": ("#373D23", "#40462C", "#4B5137"),
            "pod": ("#3B4127", "#42482E", "#464C32"), "sponson": ("#2B2F1C", "#363B26", "#3B4127")}
    pal = {"default": "#3B4127", "glass": "#6B8896", "blade": "#171714", "hub": "#26281F", "mast": "#575757",
           "gun": "#212121"}
    pal.update({k: v[1] for k, v in tone.items()})
    m.palettes["attack_helicopter"] = pal
    glass_hi, glass_lo, glass_gray = hx("#95BCE7"), hx("#375E89"), hx("#6B8896")
    dark = {"blade": ("#21221D", "#0A0A09"), "hub": ("#3D4032", "#0B0B09"), "mast": ("#6D6D6D", "#535353"),
            "gun": ("#242424", "#1E1E1E")}
    faces = ("top", "bottom", "right", "front", "left", "back")

    def detail(mob, key, put, rect):
        rng = random.Random(seed_for("attack_helicopter.detail"))
        boxes = {}
        for _, bx in mob.all_boxes():
            boxes.setdefault(bx["group"], bx)
        panel = hx("#303030")
        for bx in boxes.values():
            mat = bx["material"]
            for face in faces:
                x, y, w, h = rect(bx, face)
                if w < 1 or h < 1:
                    continue
                if mat in tone:
                    sh, ba, hi = (hx(c) for c in tone[mat])
                    steps = [_lerp(sh, ba, 0.5), ba, _lerp(ba, hi, 0.5), hi]
                    blob = [[rng.random() for _ in range(w // 3 + 2)] for _ in range(h // 3 + 2)]
                    for yy in range(h):
                        for xx in range(w):
                            v = 0.6 * blob[yy // 3][xx // 3] + 0.4 * rng.random()
                            col = steps[min(3, int(v * 4))]
                            if face == "bottom":
                                col = _lerp(col, sh, 0.4)
                            if w >= 10 and h >= 10 and (xx % 16 == 15 or yy % 16 == 15) and rng.random() < 0.8:
                                col = _lerp(col, panel, 0.6)
                            put(x + xx, y + yy, col)
                elif mat == "glass":
                    for yy in range(h):
                        t = 0.0 if face == "top" else (yy + 0.5) / h
                        col = _lerp(_lerp(glass_hi, glass_lo, t), glass_gray, 0.35)
                        for xx in range(w):
                            put(x + xx, y + yy, _lerp(col, (255, 255, 255), 0.06) if rng.random() < 0.08 else col)
                elif mat in dark:
                    base = hx(pal[mat])
                    hi, lo = (hx(c) for c in dark[mat])
                    for yy in range(h):
                        for xx in range(w):
                            v = rng.random()
                            put(x + xx, y + yy, lo if v < 0.12 else hi if v > 0.8 else base)
    m.detail = detail
    return m


MODELS = [demo_biped, biped, cyberdemon, super_mutant, alien_bug, skeleton_soldier, attack_helicopter]


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
        for key in ([] if getattr(m, "no_sheet", False) else m.sheet_keys()):
            m.paint(key).save(os.path.join(TEXDIR, key + ".png"))
            rows.append(["entity.model." + key, "src/main/resources/assets/gregguns/textures/entity/%s.png" % key,
                         "tools/asset-gen/entity_models.py", seed_for("entity_models." + key),
                         "Procedural Pillow: sheet painted to fit the box model assets/gregguns/entity_models/%s.json; "
                         "flat material colors, edge bevels, fine noise" % m.name, "MIT", "2026-10-19"])
        if not getattr(m, "no_sheet", False):
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
