#!/usr/bin/env python3
"""GregGuns armor box models: geometry, UV packing and textures for the five custom-model sets.

Sets: Exo Suit (3 colors), Power Armor (3), Steam Armor (4), Riot Coat (1), Beret (3).

Method (geometry first, see docs/gregguns-spec/05-armor/implementation-notes.md ARM-DEV-20):
  1. Boxes are authored here, by eye, in vanilla biped part space (Minecraft units, Y down; head and body
     rotate about (0,0,0), arms about (+-5,2,0), legs about (+-1.9,12,0)). Left limbs are mirrored from the right.
  2. A shelf packer gives every box a UV rectangle on the sheet (mirrored boxes share it).
  3. The model is written as JSON for the game (assets/gregguns/armor_models/<set>.json) and the textures are
     painted to fit exactly those boxes: flat metal with edge highlights and panel seams, accents on about a tenth
     of the surface, no dot grids.

Original, procedural art. Nothing here reads, traces or copies a Techguns asset.
Run from the repo root:  python -I -X utf8 tools/asset-gen/armor_models.py
Outputs: src/main/resources/assets/gregguns/armor_models/*.json, textures/models/armor/<set sheets>.png,
         tools/asset-gen/credits/armor_models.csv, docs/assets/previews/armor_models.png
"""
import colorsys
import csv
import json
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from modeltex import hx, rng_for, seed_for  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(HERE))
RES = os.path.join(ROOT, "src", "main", "resources", "assets", "gregguns")
TEXDIR = os.path.join(RES, "textures", "models", "armor")
MODELDIR = os.path.join(RES, "armor_models")
PREVIEW = os.path.join(ROOT, "docs", "assets", "previews", "armor_models.png")

PART_ORIGIN = {"head": (0, 0, 0), "body": (0, 0, 0), "rightArm": (-5, 2, 0), "leftArm": (5, 2, 0),
               "rightLeg": (-1.9, 12, 0), "leftLeg": (1.9, 12, 0)}


# ---------------------------------------------------------------------------------------------- geometry DSL
class Model:
    """A set's box model. Boxes are dicts; `group` ties a mirrored copy to the UV rectangle of its original."""

    def __init__(self, name, width, height):
        self.name, self.W, self.H = name, width, height
        self.boxes = []

    def _add(self, part, slot, name, mat, x, y, z, w, h, d, infl=0.0, pivot=None, rot=None, mirror=False, group=None):
        if pivot is not None:
            off = [x - pivot[0], y - pivot[1], z - pivot[2]]
        else:
            off = [x, y, z]
        b = {"name": name, "part": part, "slot": slot, "material": mat, "offset": off, "size": [w, h, d],
             "inflate": infl, "mirror": mirror, "group": group or (part + "/" + name)}
        if pivot is not None:
            b["pivot"] = list(pivot)
        if rot is not None:
            b["rotate"] = list(rot)
        self.boxes.append(b)

    def head(self, slot, name, mat, x, y, z, w, h, d, **kw):
        self._add("head", slot, name, mat, x, y, z, w, h, d, **kw)

    def body(self, slot, name, mat, x, y, z, w, h, d, **kw):
        self._add("body", slot, name, mat, x, y, z, w, h, d, **kw)

    def _pair(self, p_right, p_left, slot, name, mat, x, y, z, w, h, d, infl=0.0, pivot=None, rot=None):
        """Right-limb coordinates; the left limb is the mirror image sharing the same UV rectangle."""
        g = p_right + "/" + name
        self._add(p_right, slot, name, mat, x, y, z, w, h, d, infl, pivot, rot, False, g)
        lx = -x - w
        lp = None if pivot is None else (-pivot[0], pivot[1], pivot[2])
        lr = None if rot is None else (rot[0], -rot[1], -rot[2])
        self._add(p_left, slot, name, mat, lx, y, z, w, h, d, infl, lp, lr, True, g)

    def arm(self, slot, name, mat, x, y, z, w, h, d, **kw):
        self._pair("rightArm", "leftArm", slot, name, mat, x, y, z, w, h, d, **kw)

    def leg(self, slot, name, mat, x, y, z, w, h, d, **kw):
        self._pair("rightLeg", "leftLeg", slot, name, mat, x, y, z, w, h, d, **kw)

    def arm_one(self, side, slot, name, mat, x, y, z, w, h, d, **kw):
        """One arm only (asymmetric parts), in that arm's own coordinates."""
        self._add(side + "Arm", slot, name, mat, x, y, z, w, h, d, **kw)

    def leg_one(self, side, slot, name, mat, x, y, z, w, h, d, **kw):
        self._add(side + "Leg", slot, name, mat, x, y, z, w, h, d, **kw)

    # -- uv packing
    def pack(self):
        groups = {}
        for b in self.boxes:
            groups.setdefault(b["group"], b)
        order = sorted(groups.values(), key=lambda b: (-(b["size"][2] + b["size"][1]), -(2 * b["size"][2] + 2 * b["size"][0]),
                                                       b["group"]))
        x = y = shelf = 0
        pos = {}
        for b in order:
            w, h, d = b["size"]
            fw, fh = 2 * d + 2 * w, d + h
            if x + fw > self.W:
                x, y, shelf = 0, y + shelf, 0
            assert y + fh <= self.H, "%s: sheet %dx%d is full at %s" % (self.name, self.W, self.H, b["group"])
            pos[b["group"]] = (x, y)
            x += fw
            shelf = max(shelf, fh)
        for b in self.boxes:
            b["uv"] = list(pos[b["group"]])
        self.used = y + shelf

    def to_json(self):
        out = {"name": self.name, "textureWidth": self.W, "textureHeight": self.H, "boxes": []}
        for b in self.boxes:
            e = {"name": b["name"], "part": b["part"], "slot": b["slot"], "material": b["material"], "uv": b["uv"],
                 "offset": [round(v, 3) for v in b["offset"]], "size": b["size"], "inflate": b["inflate"],
                 "mirror": b["mirror"]}
            if "pivot" in b:
                e["pivot"] = [round(v, 3) for v in b["pivot"]]
                e["rotate"] = list(b.get("rotate", [0, 0, 0]))
            out["boxes"].append(e)
        return out


# ---------------------------------------------------------------------------------------------- the five sets
# INTERIM geometry, fitted by eye against renders (retired method). To be replaced by the clean-room builder
# from docs/techguns-spec/05-armor/model-notes/t3_exo.md. Do not polish.
def exo():
    """Lean, visored exoskeleton: a bright shell over a dark lattice undersuit, vented thigh and shoulder pods."""
    m = Model("t3_exo", 128, 64)
    # helmet: boxy visored shell, a raised crest with two light strips, a rear vent and a chin guard
    m.head("helmet", "shell", "plate", -4.5, -7.5, -4.5, 9, 7, 9)
    m.head("helmet", "crest", "plate2", -2.5, -8.5, -3, 5, 1, 6)
    m.head("helmet", "strip_l", "light", -3.5, -8.0, -4.6, 3, 1, 1)
    m.head("helmet", "strip_r", "light", 0.5, -8.0, -4.6, 3, 1, 1)
    m.head("helmet", "visor", "glass", -4, -5.0, -5.5, 8, 2, 1)
    m.head("helmet", "vent", "vent", -2.5, -6.0, 4.5, 5, 3, 1)
    m.head("helmet", "chin", "plate2", -3.5, -1.5, -5.0, 7, 1, 2)
    m.head("helmet", "neck", "dark", -3, -1, -2.5, 6, 1, 5)
    # chestplate: dark undersuit, a chest plate, a back unit, two spine pods
    m.body("chestplate", "torso", "dark", -4, 0, -2, 8, 12, 4)
    m.body("chestplate", "chest", "plate", -4, 0.5, -4, 8, 6, 2)
    m.body("chestplate", "chest_vent", "vent", -3, 6.5, -3.5, 6, 3, 1)
    m.body("chestplate", "back", "plate2", -3.5, 1, 2, 7, 8, 3)
    m.body("chestplate", "back_light", "light", -1, 3, 5, 2, 4, 1)
    m.arm("chestplate", "pad", "plate", -3.5, -3.5, -3, 5, 4, 6)
    m.arm("chestplate", "upper", "plate2", -3, 0, -2, 4, 5, 4)
    m.arm("chestplate", "elbow", "accent", -4.5, 4.5, -1.5, 2, 3, 3)
    m.arm("chestplate", "fore", "plate", -3, 6, -2, 4, 4, 4)
    m.arm("chestplate", "fore_vent", "vent", -3.5, 6.5, -1.5, 1, 3, 3)
    # leggings: belt, vented thighs, knee guards
    m.body("leggings", "belt", "plate2", -4.5, 10, -2.5, 9, 2, 5)
    m.leg("leggings", "thigh", "plate", -2, 0, -3, 4, 6, 6)
    m.leg("leggings", "thigh_vent", "vent", -2, 1, -3.5, 4, 4, 1)
    m.leg("leggings", "knee", "plate2", -2, 5, -5, 4, 2, 8)
    # boots: shin, ankle, long toe
    m.leg("boots", "shin", "dark", -2, 7, -2.5, 4, 3, 5)
    m.leg("boots", "shin_plate", "plate", -2, 7, -3.5, 4, 3, 1)
    m.leg("boots", "foot", "plate2", -2, 10, -4.5, 4, 2, 8)
    return m


# INTERIM geometry, fitted by eye against renders (retired method). To be replaced by the clean-room builder
# from docs/techguns-spec/05-armor/model-notes/t3_power.md. Do not polish.
def power():
    """Bulk: big white helmet, hazard-green gorget, wide chest, one huge left pauldron, angled right plate."""
    m = Model("t3_power", 128, 64)
    m.head("helmet", "shell", "plate", -4.75, -8.0, -4.5, 9, 7, 9)
    m.head("helmet", "visor", "glass", -4.0, -5.0, -5.5, 8, 2, 1)
    m.head("helmet", "lamp", "plate2", -6.0, -5.5, -2.5, 2, 2, 3)
    m.head("helmet", "ring", "ring", -5.5, -1.5, -5.0, 11, 2, 10)
    m.head("helmet", "filter", "dark", -2.0, -1.0, -6.0, 4, 3, 2)
    m.body("chestplate", "torso", "plate", -4.5, 0, -3, 9, 12, 6)
    m.body("chestplate", "chest", "plate2", -4, 2.5, -4, 8, 5, 1)
    m.body("chestplate", "vent", "vent", -4, 7.5, -4, 8, 4, 1)
    m.body("chestplate", "pack", "plate2", -2.25, 3, 3, 4, 6, 3)
    m.body("chestplate", "pack_gauge", "gauge", -1.5, 4, 6, 3, 3, 1)
    # left (screen right in front view): huge pauldron-arm block and a fist
    m.arm_one("left", "chestplate", "pauldron", "plate", 0, 1.5, -3, 6, 10, 6)
    m.arm_one("left", "chestplate", "pauldron_band", "accent", 0, 7, -3.5, 6, 1, 7)
    m.arm_one("left", "leggings", "fist", "plate2", 0, 11.5, -3, 6, 4, 5)
    # right: a small shoulder cap and a long angled plate that points out and down
    m.arm_one("right", "chestplate", "cap", "plate", -3, -1, -2.5, 3, 3, 5)
    m.arm_one("right", "chestplate", "slab", "plate2", -1, 1.5, 0.5, 1, 9, 9, pivot=(-1, 1.5, 0), rot=(0, 0, 55))
    m.arm_one("left", "chestplate", "slab", "plate2", 0, 1.5, -4.5, 1, 9, 9, pivot=(0, 1.5, 0), rot=(0, 0, -55))
    m.head("helmet", "antenna", "dark", -2, -2, -1, 2, 2, 2, pivot=(-6, -8, 0), rot=(0, 0, 45))
    m.body("leggings", "belt", "dark", -4.5, 10.5, -3, 9, 2, 6)
    m.leg("leggings", "thigh", "plate", -2.5, 0, -3, 5, 6, 6)
    m.leg("leggings", "hip_plate", "plate2", -4.5, 0, -2, 2, 5, 4)
    m.leg("boots", "shin", "plate2", -2.5, 6, -3, 5, 6, 6)
    m.leg("boots", "foot", "plate", -2.5, 10, -3.5, 5, 2, 6)
    return m


# INTERIM geometry, fitted by eye against renders (retired method). To be replaced by the clean-room builder
# from docs/techguns-spec/05-armor/model-notes/steam.md. Do not polish.
def steam():
    """Fat tan wood-and-brass suit: boiler tanks on the back, shoulder chimneys, a chest gauge."""
    m = Model("steam", 128, 64)
    m.head("helmet", "shell", "wood", -4.25, -8.0, -4.25, 8, 7, 8, infl=0.5)
    m.head("helmet", "visor", "glass", -4.0, -5.5, -5.1, 8, 2, 1)
    m.head("helmet", "band", "brass", -4.75, -2.5, -4.75, 9, 1, 9, infl=0.5)
    m.head("helmet", "pipe_l", "pipe", -3.0, -7.5, 4.5, 1, 6, 1)
    m.head("helmet", "pipe_r", "pipe", 2.0, -7.5, 4.5, 1, 6, 1)
    m.head("helmet", "lamp", "plate", -5.5, -5.0, -1.5, 1, 3, 3)
    m.body("chestplate", "torso", "wood", -4, 0, -2, 8, 12, 4, infl=0.5)
    m.body("chestplate", "gauge", "gauge", -2, 0.5, -3.0, 4, 3, 1, infl=0.5)
    m.body("chestplate", "belt", "brass", -4.5, 6, -2.5, 9, 2, 5, infl=0.5)
    m.body("chestplate", "tank_l", "plate", -3.75, 2, 2.5, 3, 6, 3)
    m.body("chestplate", "tank_r", "plate", 0.75, 2, 2.5, 3, 6, 3)
    m.body("chestplate", "tank_cap", "brass", -4.25, 1.5, 2.25, 8, 1, 4)
    m.arm("chestplate", "pad", "brass", -3.5, -3.0, -3, 6, 3, 6)
    m.arm("chestplate", "upper", "wood", -3, -0.5, -2, 4, 6, 4, infl=0.5)
    m.arm("chestplate", "band", "brass", -3.5, 4.5, -2.5, 5, 1, 5, infl=0.5)
    m.arm("chestplate", "fore", "wood", -3, 6, -2, 4, 5, 4, infl=0.5)
    m.arm("chestplate", "chimney", "plate", -2.5, -6.5, -1.0, 2, 3, 2, pivot=(-1.5, -3.0, 0), rot=(0, 0, -30))
    m.body("leggings", "hip", "brass", -4.5, 9.5, -2.5, 9, 2, 5, infl=0.5)
    m.leg("leggings", "thigh", "wood", -2, 0, -2, 4, 6, 4, infl=0.5)
    m.leg("leggings", "strap", "brass", -2.5, 3, -2.5, 5, 1, 5, infl=0.5)
    m.leg("boots", "shin", "wood", -2, 6, -2, 4, 4, 4, infl=0.5)
    m.leg("boots", "cuff", "brass", -2.5, 9, -2.5, 5, 1, 5, infl=0.5)
    m.leg("boots", "foot", "brass", -2.5, 10, -3.5, 5, 2, 6, infl=0.5)
    return m


# INTERIM geometry, fitted by eye against renders (retired method). To be replaced by the clean-room builder
# from docs/techguns-spec/05-armor/model-notes/t2_riot.md. Do not polish.
def riot():
    """Long brown coat with a green hooded cap, belt of beads and cuffed sleeves."""
    m = Model("t2_riot", 128, 64)
    m.head("helmet", "cap", "helm", -4.5, -8.5, -4.5, 9, 8, 9)
    m.head("helmet", "band", "bead", -4.75, -3.5, -4.75, 9, 1, 9, infl=0.5)
    m.head("helmet", "mask", "olive", -4.5, -3.0, -4.75, 9, 3, 1)
    m.body("chestplate", "coat", "cloth", -5.5, -0.5, -3, 11, 13, 6)
    m.body("chestplate", "collar", "cloth2", -5.0, -1.0, -3.5, 10, 2, 7)
    m.arm("chestplate", "sleeve", "cloth", -3.4, -2.0, -2, 3, 11, 4)
    m.arm("chestplate", "cuff", "tan", -3.4, 8.5, -2.5, 3, 2, 5)
    m.body("leggings", "belt", "bead", -5.5, 9.5, -3, 11, 1, 6, infl=0.5)
    m.body("leggings", "tail_f", "cloth2", -5.5, 12.5, -3, 11, 5, 1)
    m.body("leggings", "tail_b", "cloth2", -5.5, 12.5, 2, 11, 5, 1)
    m.leg("leggings", "thigh", "cloth", -2, 0, -2, 4, 7, 4)
    m.leg("boots", "shin", "leather", -2, 7, -2, 4, 5, 4)
    m.leg("boots", "cuff", "tan", -2.5, 9, -2.5, 5, 1, 5)
    return m


# INTERIM geometry, fitted by eye against renders (retired method). To be replaced by the clean-room builder
# from docs/techguns-spec/05-armor/model-notes/t2_beret.md. Do not polish.
def beret():
    """A thick flat cap, dark lining underneath, with one side drooping down at an angle."""
    m = Model("t2_beret", 64, 32)
    m.head("helmet", "crown", "cloth", -4.0, -11.0, -5.0, 8, 3, 10)
    m.head("helmet", "lining", "dark", -4.0, -8.0, -5.0, 8, 1, 10)
    m.head("helmet", "flap", "cloth", -7.0, -11.0, -5.0, 3, 4, 10, pivot=(-4.0, -11.0, 0), rot=(0, 0, -35))
    return m


MODELS = [exo, power, steam, riot, beret]

# texture file base -> palette key; every sheet is single (one file for all pieces of the camo)
VARIANTS = {
    "t3_exo": ["t3_exo", "t3_exo_silver", "t3_exo_green"],
    "t3_power": ["t3_power", "t3_power_dark", "t3_power_dark2"],
    "steam": ["steam", "steam_rusty", "steam_iron", "steam_hazard"],
    "t2_riot": ["t2_riot"],
    "t2_beret": ["t2_beret", "t2_beret_black", "t2_beret_green"],
}

# ---------------------------------------------------------------------------------------------- palettes
PAL = {
    # exo: bright shell, dark lattice, blue light strips, a little red
    "t3_exo": dict(plate="#D9DEE3", plate2="#B3BAC3", dark="#3A3F48", vent="#2C3138", glass="#0D131C",
                   light="#2F6BFF", accent="#C23A32", gauge="#2F6BFF"),
    "t3_exo_silver": dict(plate="#9EA5AE", plate2="#7C838C", dark="#2A2E34", vent="#1D2126", glass="#0E1620",
                          light="#46D2E8", accent="#E8E8EE", gauge="#46D2E8"),
    "t3_exo_green": dict(plate="#3F6F43", plate2="#2E5232", dark="#1D2A21", vent="#141D17", glass="#0B140E",
                         light="#E3CF3C", accent="#C98A2A", gauge="#E3CF3C"),
    # power armor: white plate, hazard-green ring
    "t3_power": dict(plate="#D2D2D0", plate2="#A9A9A8", dark="#3B3B3D", vent="#2A2A2C", glass="#0A0A0C",
                     ring="#6C9A2C", accent="#6C9A2C", gauge="#6C9A2C"),
    "t3_power_dark": dict(plate="#2E2E32", plate2="#44444A", dark="#18181B", vent="#101012", glass="#050506",
                          ring="#B02424", accent="#B02424", gauge="#B02424"),
    "t3_power_dark2": dict(plate="#3A2C28", plate2="#54403A", dark="#1B1412", vent="#120D0C", glass="#060404",
                           ring="#D07A22", accent="#D07A22", gauge="#D07A22"),
    # steam: tan wood and bright brass
    "steam": dict(wood="#C9A877", plate="#A9AAAE", brass="#E8D21E", dark="#3A2F22", glass="#0C0C10",
                  gauge="#2D45E8", pipe="#B8BABE"),
    "steam_rusty": dict(wood="#8A5A34", plate="#6D5A50", brass="#B8742A", dark="#2A1C12", glass="#0C0A08",
                        gauge="#D8861F", pipe="#8C6E58"),
    "steam_iron": dict(wood="#8E9298", plate="#5C6066", brass="#C6CACF", dark="#26292D", glass="#0C0E12",
                       gauge="#46A8E8", pipe="#6B7078"),
    "steam_hazard": dict(wood="#D8BC22", plate="#55585C", brass="#1C1C1C", dark="#2A2610", glass="#0C0C0C",
                         gauge="#E84A2D", pipe="#8A8C90"),
    "t2_riot": dict(cloth="#4A2E16", cloth2="#38210F", helm="#2F5A3C", olive="#6F6A3E", bead="#D08A2A",
                    leather="#5A3A1C", tan="#8E8A5A"),
    "t2_beret": dict(cloth="#8E1212", dark="#2C0606"),
    "t2_beret_black": dict(cloth="#262628", dark="#0A0A0B"),
    "t2_beret_green": dict(cloth="#46602E", dark="#1B2610"),
}


# ---------------------------------------------------------------------------------------------- painting
def face_rects(b):
    u, v = b["uv"]
    w, h, d = b["size"]
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def _bevel(f, strength=1.0):
    h, w = f.shape[:2]
    if w >= 3 and h >= 3:
        f[0, :] *= 1 + 0.16 * strength
        f[:, 0] *= 1 + 0.08 * strength
        f[-1, :] *= 1 - 0.20 * strength
        f[:, -1] *= 1 - 0.12 * strength
    return f


def _seam(f, r, light=True):
    h, w = f.shape[:2]
    if max(w, h) >= 8 and min(w, h) >= 3:
        if w >= h:
            x = int(w * (0.35 + 0.3 * r.random()))
            if 1 <= x < w - 1:
                f[:, x] *= 0.78
                f[:, x + 1] *= 1.05 if x + 1 < w - 1 else 1.0
        else:
            y = int(h * (0.35 + 0.3 * r.random()))
            if 1 <= y < h - 1:
                f[y, :] *= 0.78
                f[y + 1, :] *= 1.05 if y + 1 < h - 1 else 1.0
    return f


def paint_face(rgb, rect, mat, pal, r, face):
    x, y, w, h = rect
    if w <= 0 or h <= 0:
        return
    base = hx(pal[mat]) if mat in pal else hx(pal["plate"])
    f = np.tile(base, (h, w, 1)).astype(float)
    if mat in ("plate", "plate2", "brass", "cloth", "cloth2", "leather", "tan", "olive", "helm"):
        f *= (1 + 0.025 * (r.random((h, w, 1)) - 0.5))
        f = _bevel(f, 0.6 if mat in ("cloth", "cloth2", "leather") else 1.0)
        if mat in ("cloth", "cloth2"):
            for xx in range(2, w - 1, 3):  # soft vertical folds
                f[:, xx] *= 0.93
        else:
            f = _seam(f, r)
    elif mat == "dark":
        for xx in range(w):
            f[:, xx] *= 0.9 if (xx // 2) % 2 else 1.06  # ribbed lattice
        f = _bevel(f, 0.5)
    elif mat == "vent":
        for yy in range(1, h - 1):
            if yy % 2 == 1:
                f[yy, 1:w - 1] *= 1.9
        f = _bevel(f, 0.4)
    elif mat == "glass":
        for yy in range(h):
            f[yy] *= 0.8 + 0.5 * (1 - yy / max(1, h - 1))
        if w >= 3 and h >= 2:
            f[0, 1:w // 2 + 1] *= 2.4  # a single highlight, not a pattern
        f = _bevel(f, 0.3)
    elif mat in ("light", "ring"):
        if mat == "ring":
            dark = hx(pal.get("dark", "#202020"))
            for xx in range(w):
                if xx % 2:
                    f[:, xx] = dark * 0.9  # hazard-stripe ring
        else:
            f[h // 2, :] = np.clip(f[h // 2, :] * 0.4 + 255 * 0.6, 0, 255)
        f = _bevel(f, 0.3)
    elif mat == "gauge":
        case = hx(pal.get("dark", "#202020"))
        f[:] = case
        if w >= 3 and h >= 3:
            f[1:h - 1, 1:w - 1] = base * 0.85
            for k in range(min(w, h) - 2):
                f[1 + k, 1 + k] = (235, 235, 235)  # needle
        f = _bevel(f, 0.4)
    elif mat == "pipe":
        long_axis_x = w > h
        n = w if not long_axis_x else h
        for i in range(n):
            k = 0.62 + 0.55 * np.sin(np.pi * (i + 0.5) / n)
            if long_axis_x:
                f[i, :] *= k
            else:
                f[:, i] *= k
        for j in range(3, (h if not long_axis_x else w), 4):
            if long_axis_x:
                f[:, j] *= 0.7
            else:
                f[j, :] *= 0.7
    elif mat == "wood":
        step = 3
        for j, yy in enumerate(range(0, h, step)):
            f[yy:yy + step] *= 0.94 + 0.12 * r.random()
            f[yy, :] *= 0.72  # plank seam
            if w > 3:
                xx = int(r.random() * w)
                f[yy + 1:yy + step, xx] *= 0.78  # butt joint
        f = _bevel(f, 0.5)
    elif mat == "bead":
        for xx in range(0, w, 2):
            f[:, xx] *= 0.7
        f = _bevel(f, 0.4)
    elif mat == "accent":
        f = _bevel(f, 1.0)
    rgb[y:y + h, x:x + w] = np.clip(f, 0, 255)


def hazard_overlay(rgb, rect, pal):
    """Diagonal hazard stripes over a face (yellow ground, black stripes), used by the hazard steam color."""
    x, y, w, h = rect
    blk = hx(pal["brass"])
    for yy in range(h):
        for xx in range(w):
            if ((xx + yy) // 2) % 4 == 3:
                rgb[y + yy, x + xx] = blk


def paint_model(model, key):
    pal = PAL[key]
    r = rng_for("armor_models." + key)
    W, H = model.W, model.H
    rgb = np.zeros((H, W, 3), float)
    alpha = np.zeros((H, W), bool)
    done = set()
    for b in model.boxes:
        if b["group"] in done:
            continue
        done.add(b["group"])
        for face, rect in face_rects(b).items():
            paint_face(rgb, rect, b["material"], pal, r, face)
            x, y, w, h = rect
            alpha[y:y + h, x:x + w] = True
            if key == "steam_hazard" and b["material"] == "wood" and face in ("front", "back", "left", "right") and b["name"] in ("upper", "thigh", "shin"):
                hazard_overlay(rgb, rect, pal)
    out = np.zeros((H, W, 4), np.uint8)
    out[..., :3] = np.clip(rgb + 0.5, 0, 255).astype(np.uint8)
    out[..., 3] = np.where(alpha, 255, 0)
    out[~alpha] = 0
    return Image.fromarray(out, "RGBA")


# ---------------------------------------------------------------------------------------------- checks
def _hsv(c):
    return colorsys.rgb_to_hsv(*(np.asarray(c, float) / 255.0))


def check_variants(model, keys):
    """Every box of a variant must differ from the base variant in hue or lightness (review finding 6)."""
    base = PAL[keys[0]]
    bad = []
    for k in keys[1:]:
        for b in model.boxes:
            m = b["material"]
            if m not in base:
                continue
            ha, sa, va = _hsv(hx(base[m]))
            hb, sb, vb = _hsv(hx(PAL[k][m]))
            dh = min(abs(ha - hb), 1 - abs(ha - hb))
            if va < 0.3 and vb < 0.3:
                continue  # near-black parts (visors, vents, undersuit) stay dark in every color by design
            if dh < 0.04 and abs(va - vb) < 0.12:
                bad.append((k, b["name"], m))
    assert not bad, "variant regions equal to the base: %s" % sorted(set(bad))


def main():
    os.makedirs(TEXDIR, exist_ok=True)
    os.makedirs(MODELDIR, exist_ok=True)
    rows, sheets = [], []
    for fn in MODELS:
        m = fn()
        m.pack()
        with open(os.path.join(MODELDIR, m.name + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump(m.to_json(), f, indent=1)
            f.write("\n")
        check_variants(m, VARIANTS[m.name])
        for key in VARIANTS[m.name]:
            im = paint_model(m, key)
            p = os.path.join(TEXDIR, key + ".png")
            im.save(p)
            sheets.append((key, im))
            rows.append(["model.armor." + key, "src/main/resources/assets/gregguns/textures/models/armor/" + key + ".png",
                         "tools/asset-gen/armor_models.py", seed_for("armor_models." + key),
                         "Procedural numpy/Pillow: sheet painted to fit the box model assets/gregguns/armor_models/%s.json; "
                         "flat metal, edge highlights, panel seams" % m.name, "MIT", "2026-10-18"])
        print("%-9s %d boxes, %dx%d, %d rows used" % (m.name, len(m.boxes), m.W, m.H, m.used))
    with open(os.path.join(HERE, "credits", "armor_models.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["asset", "path", "generator", "seed", "method", "license", "date"])
        w.writerows(rows)
    # contact sheet of every generated sheet, scaled x3
    tiles = [im.resize((im.width * 3, im.height * 3), Image.NEAREST) for _, im in sheets]
    colw = max(t.width for t in tiles)
    cols = 3
    rowsn = (len(tiles) + cols - 1) // cols
    rowh = [max(t.height for t in tiles[i * cols:(i + 1) * cols]) for i in range(rowsn)]
    sheet = Image.new("RGBA", (cols * (colw + 8), sum(rowh) + 8 * rowsn), (60, 64, 70, 255))
    y = 0
    for i in range(rowsn):
        for j, t in enumerate(tiles[i * cols:(i + 1) * cols]):
            sheet.paste(t, (j * (colw + 8), y), t)
        y += rowh[i] + 8
    os.makedirs(os.path.dirname(PREVIEW), exist_ok=True)
    sheet.convert("RGB").save(PREVIEW)


if __name__ == "__main__":
    main()
