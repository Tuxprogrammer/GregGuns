#!/usr/bin/env python3
"""GregGuns armor box models: geometry, UV packing and textures for the five custom-model sets.

Sets: Exo Suit (3 colors), Power Armor (3), Steam Armor (4), Riot Coat (1), Beret (3).

Method (geometry first, see docs/gregguns-spec/05-armor/implementation-notes.md ARM-DEV-24):
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
import armor_exo_paint as exo_paint  # noqa: E402

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
        self.painter = None  # optional per-set painter(model, key); the default painter is paint_model's own
        self.check = None  # optional per-set variant check(model, keys); the default is check_variants

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
# Clean-room build from docs/techguns-spec/05-armor/model-notes/t3_exo.md. Coordinates are in the vanilla biped part
# frame (Y down, player faces -Z, right side -X); limbs are authored for the right side and mirrored. The vanilla
# layers carry the notes' per-slot inflation (1.0 helmet, 0.75 chest and boots, 0.5 leggings) in each box's own
# `inflate`; every bolt-on plate is un-inflated and axis-aligned.
def exo():
    """Powered exoskeleton: near-black undersuit under cube pauldrons, rods, joint blocks, bars and bands."""
    m = Model("t3_exo", 128, 64)
    m.painter = exo_paint.paint_exo
    # undersuit: the vanilla boxes, inflated per slot
    m.head("helmet", "shell", "helmet", -4, -8, -4, 8, 8, 8, infl=1.0)
    m.body("chestplate", "torso", "torso", -4, 0, -2, 8, 12, 4, infl=0.75)
    m.arm("chestplate", "arm", "arms", -3, -2, -2, 4, 12, 4, infl=0.75)
    m.body("leggings", "hips", "lbody", -4, 0, -2, 8, 12, 4, infl=0.5)
    m.leg("leggings", "thigh", "llegs", -2, 0, -2, 4, 12, 4, infl=0.5)
    m.leg("boots", "shank", "legs", -2, 0, -2, 4, 12, 4, infl=0.75)
    # chestplate plates
    m.arm("chestplate", "pauldron", "pauldron", -4.5, -3.5, -3, 6, 5, 6)
    m.arm("chestplate", "arm_rod", "rod", -4.5, 1.5, -1, 2, 8, 2)
    m.arm("chestplate", "elbow", "joint", -5, 4, -1.5, 2, 3, 3)
    m.arm("chestplate", "cuff", "cuff", -4, 4.5, -3, 6, 2, 6)
    m.body("chestplate", "back_bar", "bar", -5, 1, 1.5, 10, 2, 2)
    m.body("chestplate", "back_spine", "spine", -1.5, 3, 1.5, 3, 9, 2)
    # leggings plates
    m.body("leggings", "belt", "belt", -5, 11, -3, 10, 2, 6)
    m.body("leggings", "crotch", "crotch", -1, 13, -3, 2, 2, 6)
    m.leg("leggings", "thigh_joint", "joint", -4.1, 4.5, -1.5, 2, 3, 3)
    m.leg("leggings", "leg_rod", "legrod", -3.6, 0.5, -1, 2, 10, 2)
    m.leg("leggings", "knee_band", "knee", -3.1, 5, -3, 5, 2, 6)
    # boots plates
    m.leg("boots", "shin_plate", "shin", -1.5, 9.5, -3.5, 3, 3, 1)
    m.leg("boots", "ankle_ring", "ankle", -3, 10, -3, 6, 2, 6)
    return m


# Clean-room build from docs/techguns-spec/05-armor/model-notes/t3_power.md (sections 3, 5 and 6).
# Every limb is inflated by half a unit per side in the box sizes themselves; sizes are whole units and offsets
# carry the half units. Arm and leg numbers are local to that limb's rotation point, as in the notes.
def power():
    """Heavy gray bolted-plate suit: fat helmet dome with a hazard collar, 30 degree two-layer wing pauldrons,
    a deep chest block, twin back tanks, diamond knee pads and separate boots."""
    m = Model("t3_power", 128, 64)
    m.painter = paint_power
    # -- helmet (head): 9-cube dome, respirator, four-bar collar with back bump, right lamp, front stud
    m.head("helmet", "dome", "plate", -4.5, -8.5, -4.5, 9, 9, 9)
    m.head("helmet", "respirator", "panel", -2, -3, -6, 4, 4, 3)
    m.head("helmet", "collar_front_r", "ring", -5, -2, -5.5, 3, 2, 2, group="head/collar_front")
    m.head("helmet", "collar_front_l", "ring", 2, -2, -5.5, 3, 2, 2, mirror=True, group="head/collar_front")
    m.head("helmet", "collar_side_r", "ring", -5, -2, -3.5, 1, 2, 8, group="head/collar_side")
    m.head("helmet", "collar_side_l", "ring", 4, -2, -3.5, 1, 2, 8, mirror=True, group="head/collar_side")
    m.head("helmet", "collar_back", "ring", -4.5, -2, 4, 9, 2, 1)
    m.head("helmet", "collar_bump", "ring", -2, -2.5, 4.5, 4, 3, 1)
    # lamp: 2x2x4 bar turned 45 degrees about its front-to-back (Z) axis, centered x -5.5, y -6, z -3..1 (right only)
    m.head("helmet", "lamp", "mid", -6.5, -7, -3, 2, 2, 4, pivot=(-5.5, -6, -1), rot=(0, 0, 45))
    m.head("helmet", "stud", "mid", 1, -8, -5, 2, 2, 1)
    # -- chestplate, torso: core, chest block, slanted abdomen plate, twin tanks, pipe bridge, back diamond
    m.body("chestplate", "core", "plate", -4.5, -0.5, -2.5, 9, 12, 5)
    m.body("chestplate", "chest", "plate2", -4, 2, -5, 8, 5, 3)
    # abdomen: hinged at its top edge (y 7), tilted 21 degrees about X so the lower end tucks back toward the core
    m.body("chestplate", "abdomen", "plate2", -3.5, 7, -4.5, 7, 5, 2, pivot=(0, 7, -3.5), rot=(21, 0, 0))
    m.body("chestplate", "tank_r", "panel", -3.5, 5, 2, 3, 5, 2, group="body/tank")
    m.body("chestplate", "tank_l", "panel", 0.5, 5, 2, 3, 5, 2, mirror=True, group="body/tank")
    m.body("chestplate", "bridge_bar", "mid", -2.5, 2, 2.5, 5, 1, 1)
    m.body("chestplate", "bridge_leg_r", "mid", -2.5, 3, 2.5, 1, 2, 1, group="body/bridge_leg")
    m.body("chestplate", "bridge_leg_l", "mid", 1.5, 3, 2.5, 1, 2, 1, mirror=True, group="body/bridge_leg")
    m.body("chestplate", "diamond", "mid", -1, 1.5, 3, 2, 2, 1, pivot=(0, 2.5, 3.5), rot=(0, 0, 45))
    # -- chestplate, arms: 5x13x5 sleeve, shoulder cap, two stacked 30 degree wing plates, side and back plates
    m.arm("chestplate", "sleeve", "plate", -3.5, -2.5, -2.5, 5, 13, 5)
    m.arm("chestplate", "cap", "plate2", -3, -2, -3, 3, 3, 6)
    # wing plates share one hinge at the inner top edge (arm-local x 0, y -4); the right arm turns -30 about Z so
    # the outer end drops (Y is down). The upper plate lies directly on the lower one in the same rotated frame.
    m.arm("chestplate", "wing_low", "plate2", -8, -4, -4, 8, 2, 8, pivot=(0, -4, 0), rot=(0, 0, -30))
    m.arm("chestplate", "wing_up", "plate2", -8, -5, -3, 8, 1, 6, pivot=(0, -4, 0), rot=(0, 0, -30))
    m.arm("chestplate", "side_plate", "plate2", -4.5, 4.5, -2, 1, 5, 4)
    m.arm("chestplate", "back_plate", "plate2", -3, 2, 2, 3, 4, 1)
    # -- leggings, hips: pelvis block, belt, crotch plate
    m.body("leggings", "pelvis", "plate", -4.5, 11.5, -2.5, 9, 3, 5)
    m.body("leggings", "belt", "plate2", -5, 11, -3, 10, 2, 6)
    m.body("leggings", "crotch", "plate2", -1.5, 13, -3, 3, 2, 6)
    # -- leggings, legs: thigh sleeve, diamond knee pad, side stud and two struts (straight bars: the 8.5 degree
    # bow of the original is under half a unit and the notes allow it)
    m.leg("leggings", "thigh", "plate", -2.5, -0.5, -2.5, 5, 9, 5)
    m.leg("leggings", "knee", "mid", -1.9, 3.1, -3, 3, 3, 1, pivot=(-0.4, 4.6, -2.5), rot=(0, 0, 45))
    m.leg("leggings", "stud", "mid", -3.1, 3.5, -1, 1, 2, 2, pivot=(-2.6, 4.5, 0), rot=(45, 0, 0))
    m.leg("leggings", "strut_up", "mid", -3.1, 0.5, -1, 1, 3, 2)
    m.leg("leggings", "strut_low", "mid", -3.1, 5.5, -1, 1, 3, 2)
    # -- boots (leg parts, boots piece only): shaft and toe cap
    m.leg("boots", "shaft", "plate", -2.5, 8.5, -2.5, 5, 4, 5)
    m.leg("boots", "toe", "plate2", -2, 10, -4, 4, 2, 2)
    return m


# Clean-room build from docs/techguns-spec/05-armor/model-notes/steam.md (the analyst's design notes; section numbers
# below refer to that page). Every number is the notes' own, in the vanilla biped part frame.
def steam():
    """Tan steampunk exosuit: yellow brass rings and rails, tilted gray shoulder chimneys, a twin-tank back pack."""
    m = Model("steam", 128, 64)
    m.painter = paint_steam
    m.check = check_steam

    def hp(slot, name, mat, x, y, z, w, h, d):
        """Head box at x (player's right, x < 0) plus its mirror image sharing the UV rectangle."""
        m.head(slot, name + "_r", mat, x, y, z, w, h, d, group="head/" + name)
        m.head(slot, name + "_l", mat, -x - w, y, z, w, h, d, mirror=True, group="head/" + name)

    def bp(slot, name, mat, x, y, z, w, h, d):
        """Body box at x (player's right, x < 0) plus its mirror image sharing the UV rectangle."""
        m.body(slot, name + "_r", mat, x, y, z, w, h, d, group="body/" + name)
        m.body(slot, name + "_l", mat, -x - w, y, z, w, h, d, mirror=True, group="body/" + name)

    # -- helmet (section 3, helmet): a 9 cube, snout, jaw strip, ear plates, clamped back pipes
    m.head("helmet", "skull", "helm", -4.5, -8.5, -4.5, 9, 9, 9)
    m.head("helmet", "snout", "gray", -2, -3, -6, 4, 4, 2)
    m.head("helmet", "jaw", "gray", -3.5, -1.5, -5.5, 7, 2, 1)
    hp("helmet", "ear", "helm", -5.5, -7, -3, 1, 6, 6)
    hp("helmet", "pipe", "gray", -3, -6.5, 5.5, 1, 6, 1)
    # clamps: 2x2x1 on the skull's back face, one at each pipe's top and bottom (the lower pair reuses the top rectangle)
    m.head("helmet", "clamp_top_r", "gray", -3.5, -7, 4.5, 2, 2, 1, group="head/clamp")
    m.head("helmet", "clamp_top_l", "gray", 1.5, -7, 4.5, 2, 2, 1, mirror=True, group="head/clamp")
    m.head("helmet", "clamp_bot_r", "gray", -3.5, -2, 4.5, 2, 2, 1, group="head/clamp")
    m.head("helmet", "clamp_bot_l", "gray", 1.5, -2, 4.5, 2, 2, 1, mirror=True, group="head/clamp")

    # -- chestplate, torso (section 3, chestplate body)
    m.body("chestplate", "shell", "torso", -4.5, -0.5, -2.5, 9, 12, 5)
    m.body("chestplate", "chest", "torso", -3.5, 1.5, -3.5, 7, 4, 1)
    m.body("chestplate", "abdomen", "torso", -2.5, 6.5, -3.5, 5, 4, 1)
    m.body("chestplate", "backplate", "backp", -4, 0.5, 2.5, 8, 10, 2)
    bp("chestplate", "casing", "gray", -3.5, 3.5, 4.5, 3, 4, 2)
    bp("chestplate", "core", "window", -3, 4.5, 4, 2, 3, 2)
    bp("chestplate", "cap", "gray", -3, 1.5, 4, 2, 1, 2)
    bp("chestplate", "foot", "gray", -3, 8.5, 4, 2, 1, 2)
    bp("chestplate", "rung_hi", "brass", -3.5, 2.5, 4.5, 3, 1, 2)
    bp("chestplate", "rung_lo", "brass", -3.5, 7.5, 4.5, 3, 1, 2)

    # -- chestplate, arms (right-arm frame; the left arm is the mirror). The 0.01 inflate on the shell keeps its faces
    #    off the torso shell's coplanar ones (they overlap by about 1)
    m.arm("chestplate", "shell", "limb", -3.5, -2.5, -2.5, 5, 13, 5, infl=0.01)
    m.arm("chestplate", "ring", "brass", -4.5, -1.5, -3, 6, 2, 6)
    m.arm("chestplate", "crossbar", "brass", -1.5, -3.5, -3, 2, 2, 6)
    # chimney: base 3x2x3 then tube 2x2x2, hinged at (-2,-1.5,0) and tipped 30 degrees outward about Z
    # (negative Z angle leans toward -X on the right arm; the mirrored left arm leans toward +X)
    m.arm("chestplate", "chim_base", "gray", -3.5, -3.5, -1.5, 3, 2, 3, pivot=(-2, -1.5, 0), rot=(0, 0, -30))
    m.arm("chestplate", "chim_tube", "gray", -3, -5.5, -1, 2, 2, 2, pivot=(-2, -1.5, 0), rot=(0, 0, -30))
    m.arm("chestplate", "elbow_band", "brass", -4, 4.5, -3, 6, 2, 6)
    m.arm("chestplate", "rail", "brass", -4.5, 0.5, -1, 2, 9, 2)
    m.arm("chestplate", "elbow_block", "gray", -5, 4, -1.5, 2, 3, 3)

    # -- leggings (section 3, leggings): pelvis on the body frame, sleeves, knee gear and rails on the leg frame
    m.body("leggings", "pelvis_ring", "brass", -5, 11, -3, 10, 2, 6)
    m.body("leggings", "skirt", "limb", -4.5, 11.5, -2.5, 9, 3, 5)
    m.body("leggings", "crotch", "brass", -1, 13, -3, 2, 2, 6)
    m.leg("leggings", "sleeve", "limb", -2.5, -0.5, -2.5, 5, 9, 5, infl=0.01)
    m.leg("leggings", "knee_band", "brass", -3, 5, -3, 5, 2, 6)
    m.leg("leggings", "knee_block", "gray", -4, 4.5, -1.5, 2, 3, 3)
    m.leg("leggings", "rail", "brass", -3.5, 0.5, -1, 2, 10, 2)

    # -- boots (section 3, boots): short and nearly cubic, no long foot
    m.leg("boots", "boot_sleeve", "limb", -2.5, 8.5, -2.5, 5, 4, 5)
    m.leg("boots", "sole_band", "brass", -3, 10, -3, 6, 2, 6)
    m.leg("boots", "toe", "brass", -1.5, 9.5, -3.5, 3, 3, 1)
    return m


# Clean-room geometry from docs/techguns-spec/05-armor/model-notes/t2_riot.md (frame: vanilla biped part space,
# Y down, player faces -Z, right is -X). Every slot draws its own vanilla-part boxes at that slot's inflation
# (helmet 1.0 plus a 1.5 hat layer, chest 0.5, leggings 0.5, boots 0.75) and the extra coat boxes on top.
def riot():
    """Long brown leather duster: side panels and a thin back sheet, sloped shoulder pads, leg-borne skirt sleeves."""
    m = Model("t2_riot", 128, 64)
    m.painter = paint_riot
    # helmet: the head box at 1.0 and the hat layer at 1.5; studs, band and mask are paint, not boxes
    m.head("helmet", "head", "helm", -4, -8, -4, 8, 8, 8, infl=1.0)
    m.head("helmet", "hat", "helm_band", -4, -8, -4, 8, 8, 8, infl=1.5)
    # chestplate: body and arms at 0.5, two side panels, a back sheet, a tilted pad on each arm
    m.body("chestplate", "body", "skin", -4, 0, -2, 8, 12, 4, infl=0.5)
    m.arm("chestplate", "arm", "leather", -3, -2, -2, 4, 12, 4, infl=0.5)
    # panel: 2x12x5 grown by 0.5 = 3 wide, outer face at x -5.5, inner face at -2.5, z -3..3, y -0.6..12.4
    m.body("chestplate", "panel_r", "leather", -5.0, -0.1, -2.5, 2, 12, 5, infl=0.5, group="body/panel")
    m.body("chestplate", "panel_l", "leather", 3.0, -0.1, -2.5, 2, 12, 5, infl=0.5, mirror=True, group="body/panel")
    # back sheet: a 10x12 sheet grown by 0.5 = 11 wide, 13 tall (as the panels), 1 thick in the plane z 2..3, flush with the panels' back edges
    m.body("chestplate", "back", "leather", -5.5, -0.6, 2.0, 11, 13, 1)
    # pad: 5x3x6 grown by 0.5 = 6x4x7, centered over the arm (world x +-6), turned 13 degrees about the front-back
    # axis through the arm pivot so the outer end drops
    m.arm("chestplate", "pad", "leather", -3.5, -3.0, -3.0, 5, 3, 6, infl=0.5, pivot=(0, 0, 0), rot=(0, 0, -13))
    # leggings: the belt zone of the body, the legs, and one skirt sleeve on each leg (5x10x5 grown by 0.5 = 6x11x6,
    # centered 0.6 further out than the leg, world y 11.5..22.5)
    m.body("leggings", "belt", "skin", -4, 0, -2, 8, 12, 4, infl=0.5)
    m.leg("leggings", "leg", "leather", -2, 0, -2, 4, 12, 4, infl=0.5)
    m.leg("leggings", "sleeve", "leather", -3.1, 0, -2.5, 5, 10, 5, infl=0.5)
    # boots: the legs at 0.75, nothing else
    m.leg("boots", "boot", "boot", -2, 0, -2, 4, 12, 4, infl=0.75)
    return m


# Clean-room geometry from docs/techguns-spec/05-armor/model-notes/t2_beret.md. Exactly two head boxes, no head box
# of its own and no lining; the vanilla hat layer is not drawn (BoxArmorModel empties it). Variants are recolors.
def beret():
    """A flat thick cap shifted toward the player's left, with a slab drooping over the right temple."""
    m = Model("t2_beret", 64, 32)
    m.painter = paint_beret
    # crown: 6x2x8 grown by 0.6 = 7.2x3.2x9.2, x -2.6..4.6, y -9.1..-5.9, z -4.6..4.6
    m.head("helmet", "crown", "cloth", -2.0, -8.5, -4.0, 6, 2, 8, infl=0.6)
    # droop (x, y are part coordinates; the pivot is subtracted by the DSL): 3x2x8 grown by 0.55 = 4.1x3.1x9.1, hinged on the crown's right upper edge at (-2.7, -8.3) and turned
    # 35 degrees so the outer end falls to about eye level (y -4), about 2 past the head side
    m.head("helmet", "droop", "cloth", -5.75, -8.25, -4.0, 3, 2, 8, infl=0.55, pivot=(-2.7, -8.3, 0), rot=(0, 0, -35))
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
    # power armor (clean-room, notes t3_power.md section 5): gray plate, black panels, hazard-green collar;
    # the camos are near-black with dark red (dark) or blood-rust splatter (dark2). `stripes` is the collar's
    # four-step diagonal hazard run; None means "no such detail in this variant".
    "t3_power": dict(plate="#A1A1A1", plate_hi="#CFCFCF", plate_lo="#6B6B6B",
                     plate2="#A4A4A4", plate2_hi="#BDBDBD", plate2_lo="#767676",
                     mid="#707070", mid_hi="#A1A1A1", mid_lo="#313131",
                     panel="#171717", panel_hi="#3F3F3F", panel_lo="#070707",
                     ring="#4B6D00", stripes=["#4B6D00", "#304400", "#171717", "#171717"],
                     band="#171717", top_stripe="#8C8C8C", side_line=None, knee_light="#A1A1A1",
                     boot_band="#767676", splat=None),
    "t3_power_dark": dict(plate="#1B1B1B", plate_hi="#434343", plate_lo="#101010",
                          plate2="#1E1E1E", plate2_hi="#2A2A2A", plate2_lo="#141414",
                          mid="#151515", mid_hi="#2A2A2A", mid_lo="#0A0A0A",
                          panel="#050505", panel_hi="#101010", panel_lo="#000000",
                          ring="#581300", stripes=["#581300", "#4C0000", "#2D0600", "#000000"],
                          band="#581300", top_stripe="#581300", side_line="#4C0000", knee_light="#581300",
                          boot_band="#581300", splat=None),
    "t3_power_dark2": dict(plate="#1A1A1A", plate_hi="#3A3A3A", plate_lo="#0E0E0E",
                           plate2="#1E1E1E", plate2_hi="#2A2A2A", plate2_lo="#121212",
                           mid="#161616", mid_hi="#2A2A2A", mid_lo="#080808",
                           panel="#060606", panel_hi="#121212", panel_lo="#000000",
                           ring="#581300", stripes=["#2D0600", "#581300", "#2D0600", "#000000"],
                           band=None, top_stripe=None, side_line=None, knee_light=None, boot_band=None,
                           splat=["#380000", "#400808", "#460E0E", "#481010", "#5A3B3B"]),
    "t2_riot": dict(leather="#54391C", leather_hi="#573B1D", leather_lo="#422D16", leather_lo2="#4B3319",
                    helm="#64664B", helm_hi="#6E7052", helm_lo="#606248", helm_top="#757757", helm_top_hi="#7A7C5C",
                    trim="#3E3F2E", trim_lo="#323326", skin="#483F2B", skin_lo="#3C3423", stud="#C8861E",
                    boot="#422D16"),
    "t2_beret": dict(cloth="#6A0000", dark="#200000"),
    "t2_beret_black": dict(cloth="#353535", dark="#101010"),
    "t2_beret_green": dict(cloth="#0C3107", dark="#051503"),
}
PAL.update(exo_paint.PALETTES)  # the Exo Suit rows live with its painter


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


# ---------------------------------------------------------------------------------------------- steam painter
# Palette from docs/techguns-spec/05-armor/model-notes/steam.md section 5. Each material is (base, highlight, shadow).
# Materials: helm (helmet cube, ear plates), limb (arm and leg shells), torso (torso shell and chest plates),
# backp (back plate), brass (yellow trim), gray (chimneys, blocks, pipes, clamps, tank casings), dark (visor slit and
# chimney mouth), window (blue gauge glass, the same in every variant).
STEAM_BLUE = ("#3340FF", "#FFFFFF", "#1E28C8")
STEAM_PAL = {
    "steam": dict(helm=("#CDB38F", "#D2B48B", "#A5855C"), limb=("#B59A6E", "#D2B48B", "#594935"),
                  torso=("#CDB38F", "#D2B48B", "#594935"), backp=("#A5855C", "#B09168", "#7B5E45"),
                  brass=("#E8E35A", "#FFFF7B", "#BCB848"), gray=("#CBC7C4", "#FFFFFF", "#9E9891"),
                  dark=("#1F1F1F", "#2A2A2A", "#141414"), window=STEAM_BLUE),
    # rusty: gold-brown plates with orange rust patches, dark rust trim, weathered gray
    "steam_rusty": dict(helm=("#C8963F", "#DDB55E", "#A34711"), limb=("#C08B3E", "#DDB55E", "#A34711"),
                        torso=("#C8963F", "#DDB55E", "#A34711"), backp=("#A97832", "#C08B3E", "#612502"),
                        brass=("#6E2E0A", "#8A4015", "#3F1603"), gray=("#9E9891", "#B2ACA5", "#807A75"),
                        dark=("#1F1F1F", "#2A2A2A", "#141414"), window=STEAM_BLUE,
                        rust=("#A34711", "#612502"), deep=("#300F02",)),
    # iron: cool steel plates, very dark trim, dark gunmetal blocks
    "steam_iron": dict(helm=("#848489", "#8E8F94", "#767679"), limb=("#7D7D7D", "#84858A", "#767679"),
                       torso=("#848489", "#8E8F94", "#767679"), backp=("#6F6F72", "#7D7D7D", "#5E5E61"),
                       brass=("#272422", "#4E4A41", "#1A1817"), gray=("#403C38", "#55504A", "#322E2C"),
                       dark=("#1F1F1F", "#2A2A2A", "#141414"), window=STEAM_BLUE),
    # hazard: near-black plates with yellow caution bars, yellow-over-black trim, red chimneys and blocks
    "steam_hazard": dict(helm=("#0B0B0B", "#C3C000", "#0B0B0B"), limb=("#0B0B0B", "#CECB00", "#0B0B0B"),
                         torso=("#0B0B0B", "#C3C000", "#0B0B0B"), backp=("#101010", "#C3C000", "#0B0B0B"),
                         brass=("#A5A600", "#E5E22D", "#101010"), gray=("#DA201B", "#EB4A44", "#72110E"),
                         dark=("#1F1F1F", "#2A2A2A", "#141414"), window=STEAM_BLUE,
                         bars={"helm": (10, 3), "limb": (10, 3), "torso": (10, 3), "backp": (10, 3), "brass": (5, 1)}),
}


def _face_noise(f, r, amp=0.03):
    h, w = f.shape[:2]
    return f * (1 + amp * (r.random((h, w, 1)) - 0.5))


def paint_steam_face(rgb, rect, b, face, pal, r):
    x, y, w, h = rect
    mat = b["material"]
    base, hi, sh = (hx(c) for c in pal[mat])
    f = np.tile(base, (h, w, 1)).astype(float)
    f = _face_noise(f, r, 0.04 if mat != "window" else 0.0)
    ys, xs = np.mgrid[0:h, 0:w]
    if mat == "torso":  # soft vertical shading, like a sculpted chest
        k = 0.5 - 0.5 * np.cos(2 * np.pi * (xs + 0.5) / max(w, 1) * (1 if w < 8 else 2))
        f = f * (0.95 + 0.07 * k[..., None])
    if mat == "gray" and w * h >= 4:  # a few specular dots
        for _ in range(max(1, w * h // 14)):
            f[int(r.random() * h), int(r.random() * w)] = hi
    if "bars" in pal and mat in pal["bars"]:  # hazard caution bars, diagonal; hi is the bar color
        period, wid = pal["bars"][mat]
        if mat == "brass":  # yellow trim with thin black stripes; rows alternate the two yellows, bars run diagonally
            f[:] = np.where((ys % 2 == 0)[..., None], hi, base)
            f[((xs + ys) % period) < wid] = hx(pal["brass"][2])
        else:
            bar = ((xs + ys) % period) < wid
            f[bar] = hi * (0.96 + 0.08 * r.random((int(bar.sum()), 1)))
    if "rust" in pal and mat in ("helm", "limb", "torso", "backp", "gray"):  # orange rust patches
        ra, rb = hx(pal["rust"][0]), hx(pal["rust"][1])
        for _ in range(max(1, w * h // 9)):
            cx, cy = int(r.random() * w), int(r.random() * h)
            col = ra if r.random() < 0.7 else rb
            for dx, dy in ((0, 0), (1, 0), (0, 1)):
                if cx + dx < w and cy + dy < h:
                    f[cy + dy, cx + dx] = col * (0.95 + 0.1 * r.random())
    if "rust" in pal and mat == "brass":  # darker burnt streaks on the trim
        for _ in range(max(1, w * h // 8)):
            f[int(r.random() * h), int(r.random() * w)] = hx(pal["deep"][0])
    # bevel: highlight on the top and left rows, shadow on the bottom and right (only where the face is big enough)
    if mat not in ("dark",) and w >= 3 and h >= 3:
        mix = 0.35 if "bars" not in pal else 0.0
        f[0, :] = f[0, :] * (1 - mix) + hi * mix
        f[:, 0] = f[:, 0] * (1 - mix * 0.6) + hi * mix * 0.6
        f[-1, :] = f[-1, :] * (1 - mix) + sh * mix
        f[:, -1] = f[:, -1] * (1 - mix * 0.6) + sh * mix * 0.6
    # panel seam and rivets on the big plates
    if mat in ("helm", "limb", "torso", "backp") and "bars" not in pal:
        if w >= 6 and h >= 6:
            f[h // 2, 1:w - 1] = f[h // 2, 1:w - 1] * 0.55 + sh * 0.45
            for px, py in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)):
                f[py, px] = sh * 0.8
    # --- face-specific marks
    nm = b["name"]
    if nm == "skull" and face == "front":  # the visor slit: very dark band with a thin light rim above and below
        rim = hx(pal["helm"][1]) * 0.45 + 255 * 0.55
        f[2, 1:w - 1] = rim
        f[5, 1:w - 1] = rim
        f[3:5, 1:w - 1] = hx(pal["dark"][2])
    if nm == "snout" and face == "front":  # trim-colored frame, gray inner face, small dark center
        f[:] = hx(pal["brass"][0])
        f[1:h - 1, 1:w - 1] = hx(pal["gray"][0])
        f[h // 2, w // 2] = hx(pal["dark"][0])
    if nm == "chim_tube" and face == "top":  # the dark mouth
        f[:] = hx(pal["dark"][0])
    if nm == "chim_base" and face == "top":
        f[:] = hx(pal["dark"][1])
    if nm.startswith("casing") and face == "back":  # deep blue window, dark border, diagonal white glint
        wb, wh, ws = (hx(c) for c in pal["window"])
        dk = np.array([14.0, 14.0, 22.0])
        f[0, :] = dk
        f[h - 1, :] = dk
        f[1:3, :] = wb
        f[1:3, 0] = dk
        f[2, 1] = wh
        f[1, 2] = wh
        f[2, 2] = ws
    if mat == "window":
        f[:] = hx(pal["window"][0])
        f[0, 0] = hx(pal["window"][1])
    rgb[y:y + h, x:x + w] = np.clip(f, 0, 255)


def paint_steam(model, key):
    pal = STEAM_PAL[key]
    r = rng_for("armor_models." + key)
    rgb = np.zeros((model.H, model.W, 3), float)
    alpha = np.zeros((model.H, model.W), bool)
    done = set()
    for b in model.boxes:
        if b["group"] in done:
            continue
        done.add(b["group"])
        for face, rect in face_rects(b).items():
            paint_steam_face(rgb, rect, b, face, pal, r)
            x, y, w, h = rect
            alpha[y:y + h, x:x + w] = True
    out = np.zeros((model.H, model.W, 4), np.uint8)
    out[..., :3] = np.clip(rgb + 0.5, 0, 255).astype(np.uint8)
    out[..., 3] = np.where(alpha, 255, 0)
    out[~alpha] = 0
    return Image.fromarray(out, "RGBA")


def check_steam(model, keys):
    """Each variant must recolor every non-blue, non-black region: the mean color of a material's base differs clearly
    from the base variant's (the blue window is identical in all four by design)."""
    bad = []
    for k in keys[1:]:
        for mat, spec in STEAM_PAL[k].items():
            if mat in ("window", "dark", "rust", "deep", "bars"):
                continue
            a, b = hx(STEAM_PAL[keys[0]][mat][0]), hx(spec[0])
            if "bars" in STEAM_PAL[k] and mat in STEAM_PAL[k]["bars"]:
                p, wid = STEAM_PAL[k]["bars"][mat]
                if mat == "brass":
                    b = ((hx(spec[0]) + hx(spec[1])) / 2 * (p - wid) + hx(spec[2]) * wid) / p
                else:
                    b = (hx(spec[0]) * (p - wid) + hx(spec[1]) * wid) / p
            if float(np.linalg.norm(a - b)) < 40:
                bad.append((k, mat))
    assert not bad, "steam variant regions too close to the base: %s" % bad


# power armor painter: its own palette keys and per-box details (notes t3_power.md section 5)
def _mix(a, b, t):
    return np.asarray(a, float) * (1 - t) + np.asarray(b, float) * t


def _soft_inset(rgb, x, y, w, h, color, edge=0.45):
    """A dark recessed window with a soft, slightly blurred edge: the ring around it is half-blended."""
    H, W = rgb.shape[:2]
    for yy in range(y - 1, y + h + 1):
        for xx in range(x - 1, x + w + 1):
            if not (0 <= xx < W and 0 <= yy < H):
                continue
            inside = x <= xx < x + w and y <= yy < y + h
            corner = (xx in (x - 1, x + w)) and (yy in (y - 1, y + h))
            if inside:
                rgb[yy, xx] = color
            elif not corner:
                rgb[yy, xx] = _mix(rgb[yy, xx], color, edge)


def _power_face(rgb, rect, b, face, pal, r):
    """Paint one face of one box into `rgb` (a sheet-sized float canvas)."""
    x, y, w, h = rect
    mat = b["material"]
    if mat in ("plate", "plate2", "mid", "panel"):
        base, hi, lo = hx(pal[mat]), hx(pal[mat + "_hi"]), hx(pal[mat + "_lo"])
    else:
        base, hi, lo = hx(pal["plate"]), hx(pal["plate_hi"]), hx(pal["plate_lo"])
    f = np.tile(base, (h, w, 1)).astype(float)
    # rough painted metal: a few gray levels around the base
    for yy in range(h):
        for xx in range(w):
            f[yy, xx] = base + (hi - lo) * 0.07 * float(r.choice([-1.0, -0.5, 0.0, 0.0, 0.0, 0.5, 1.0]))
    if face == "top":
        f[:] = _mix(f, hi, 0.5)
    elif face == "bottom":
        f[:] = _mix(f, lo, 0.7)
    elif w >= 3 and h >= 3:
        f[0, :] = _mix(f[0, :], hi, 0.55)  # highlights on top edges only
        f[-1, :] = _mix(f[-1, :], lo, 0.6)
        f[:, 0] = _mix(f[:, 0], hi, 0.2)
        f[:, -1] = _mix(f[:, -1], lo, 0.3)
    n = b["name"]
    sides = ("right", "front", "left", "back")
    if mat == "ring":
        st = [hx(c) for c in pal["stripes"]]
        for yy in range(h):
            for xx in range(w):
                f[yy, xx] = st[((x + xx + y + yy) // 2) % 4]
    elif mat == "panel" and n.startswith("tank"):
        if w >= 3 and h >= 3:  # dark tank with a lighter rim
            ph = hx(pal["panel_hi"])
            f[0, :], f[-1, :], f[:, 0], f[:, -1] = ph, ph, ph, ph
    if n == "dome":
        if face == "front":
            _soft_inset(f, 1, 3, 7, 2, hx(pal["panel"]))  # visor slit, painted not recessed
        elif face == "top" and pal["top_stripe"]:
            f[:, w // 2] = hx(pal["top_stripe"])
        elif face in ("right", "left") and pal["side_line"]:
            f[4, :] = hx(pal["side_line"])
    elif n == "respirator" and face == "front":
        g = hx(pal["panel_hi"])
        for yy in range(1, h - 1, 2):  # grille hint
            f[yy, 1:w - 1] = _mix(f[yy, 1:w - 1], g, 0.7)
    elif n == "chest" and face == "front":
        _soft_inset(f, 0, 1, 4, 3, hx(pal["panel"]))  # two big dark panels, the left one bigger
        _soft_inset(f, 5, 1, 3, 3, hx(pal["panel"]))
    elif n == "abdomen" and face == "front":
        _soft_inset(f, 1, 2, 5, 1, hx(pal["panel"]))  # vent slot
    elif n == "sleeve" and face in sides:
        if pal["band"]:
            for row in (4, 8):
                f[row, :] = hx(pal["band"])
        for col in range(1, w - 1, 2):
            f[11, col] = hx(pal["panel"])  # rivets
    elif n == "wing_low" and face == "top":
        rv = hx(pal["panel"])
        for cx in (1, w - 2):
            for cy in (1, h - 2):
                f[cy, cx] = rv
    elif n == "knee" and face == "front" and pal["knee_light"]:
        f[1, 1] = hx(pal["knee_light"])
    elif n == "shaft" and face in sides and pal["boot_band"]:
        f[1, :] = hx(pal["boot_band"])
    rgb[y:y + h, x:x + w] = f


SPLAT_TARGETS = ("dome", "collar_front_r", "collar_side_r", "collar_back", "sleeve", "tank_r", "belt", "shaft")


def _red_count(img):
    a = np.asarray(img).astype(int)
    return int(((a[..., 3] > 0) & (a[..., 0] > a[..., 1] + 15)).sum())


def paint_power(model, key, splat=True):
    """Sheet for one Power Armor camo. `dark2` gets blood-rust splatter on top, about two thirds as many red
    pixels in total as the clean `dark` camo."""
    pal = PAL[key]
    r = rng_for("armor_models." + key)
    W, H = model.W, model.H
    rgb = np.zeros((H, W, 3), float)
    alpha = np.zeros((H, W), bool)
    done = set()
    targets = []
    for b in model.boxes:
        if b["group"] in done:
            continue
        done.add(b["group"])
        for face, rect in face_rects(b).items():
            _power_face(rgb, rect, b, face, pal, r)
            x, y, w, h = rect
            alpha[y:y + h, x:x + w] = True
            if b["name"] in SPLAT_TARGETS and face != "bottom" and w * h >= 4:
                targets.append(rect)
    if pal["splat"] and splat:
        dark = paint_power(model, "t3_power_dark", splat=False)
        want = int(0.66 * _red_count(dark)) - _red_count(paint_power(model, key, splat=False))
        cols = [hx(c) for c in pal["splat"]]
        placed = 0
        sr = rng_for("armor_models.splat." + key)
        weights = np.array([w * h for _, _, w, h in targets], float)
        weights /= weights.sum()
        while placed < want:
            x, y, w, h = targets[int(sr.choice(len(targets), p=weights))]
            px, py = int(x + sr.integers(w)), int(y + sr.integers(h))
            for _ in range(int(sr.integers(1, 5))):  # a smear: a short random walk inside the face
                if x <= px < x + w and y <= py < y + h:
                    rgb[py, px] = cols[int(sr.integers(len(cols)))]
                    placed += 1
                px += int(sr.integers(-1, 2))
                py += int(sr.integers(-1, 2))
    out = np.zeros((H, W, 4), np.uint8)
    out[..., :3] = np.clip(rgb + 0.5, 0, 255).astype(np.uint8)
    out[..., 3] = np.where(alpha, 255, 0)
    out[~alpha] = 0
    return Image.fromarray(out)


def _noise(r, color, w, h, lo=0.955, hi=1.045):
    """A w x h patch of `color` with per-pixel brightness noise, as floats."""
    return np.tile(hx(color), (h, w, 1)).astype(float) * (lo + (hi - lo) * r.random((h, w, 1)))


def _finish(rgb, alpha):
    out = np.zeros(alpha.shape + (4,), np.uint8)
    out[..., :3] = np.clip(rgb + 0.5, 0, 255).astype(np.uint8)
    out[..., 3] = np.where(alpha, 255, 0)
    out[~alpha] = 0
    return Image.fromarray(out, "RGBA")


def paint_riot(model, key):
    """Flat brown leather, an olive helmet (lighter on top) with a row of orange studs, olive-brown body skin.
    The helmet's hat layer is mostly clear: only the brow band, studs and mask are painted on it. The belt zone and
    the boots are clear except where they are worn."""
    pal = PAL[key]
    r = rng_for("armor_models." + key)
    rgb = np.zeros((model.H, model.W, 3), float)
    alpha = np.zeros((model.H, model.W), bool)

    def put(rect, f, a=None):
        x, y, w, h = rect
        rgb[y:y + h, x:x + w] = np.where(a[..., None], f, 0) if a is not None else f
        alpha[y:y + h, x:x + w] = True if a is None else a

    stud = hx(pal["stud"])
    done = set()
    for b in model.boxes:
        if b["group"] in done:
            continue
        done.add(b["group"])
        n = b["name"]
        for face, rect in face_rects(b).items():
            x, y, w, h = rect
            if w <= 0 or h <= 0:
                continue
            side = face in ("front", "back", "left", "right")
            if n == "head":
                if face == "top":
                    f = _noise(r, pal["helm_top"], w, h)
                elif face == "bottom":
                    f = _noise(r, pal["trim"], w, h)
                else:
                    f = _noise(r, pal["helm"], w, h)
                    f[-1, :] = hx(pal["trim"])  # lower rim
                    if face != "front":
                        f *= 0.985
                put(rect, f)
            elif n == "hat":
                f = np.zeros((h, w, 3))
                a = np.zeros((h, w), bool)
                if side:
                    f[2:4] = _noise(r, pal["helm_hi"], w, 2)  # brow band
                    a[2:4] = True
                    f[4] = hx(pal["helm_lo"])
                    a[4] = True
                    for xx in range(1, w, 2):  # studs along the band
                        f[3, xx] = stud
                    if face == "front":  # mask: a dark guard over the lower face
                        f[5:8, 1:w - 1] = _noise(r, pal["trim"], w - 2, 3)
                        f[7, 1:w - 1] = hx(pal["trim_lo"])
                        a[5:8, 1:w - 1] = True
                elif face == "top":  # a light rim only; the head box shows through
                    f[:] = _noise(r, pal["helm_top_hi"], w, h)
                    a[:] = True
                    a[1:-1, 1:-1] = False
                put(rect, f, a)
            elif n == "body":
                f = _noise(r, pal["skin"], w, h)
                if face in ("front", "back"):
                    f[:, w // 2] = hx(pal["skin_lo"])
                put(rect, f)
            elif n == "belt":
                f = np.zeros((h, w, 3))
                a = np.zeros((h, w), bool)
                if side:
                    a[8:] = True
                    f[8:] = _noise(r, pal["skin"], w, h - 8)
                    f[9] = hx(pal["skin_lo"])
                    for xx in range(0, w, 2):  # belt beads
                        f[10, xx] = stud
                elif face == "bottom":
                    a[:] = True
                    f = _noise(r, pal["skin_lo"], w, h)
                put(rect, f, a)
            elif n == "boot":
                f = np.zeros((h, w, 3))
                a = np.zeros((h, w), bool)
                if side:
                    a[6:] = True
                    f[6:] = _noise(r, pal["boot"], w, h - 6)
                    f[6] = hx(pal["leather_lo2"])  # cuff
                    f[7] = hx(pal["leather"])
                elif face == "bottom":
                    a[:] = True
                    f = _noise(r, pal["leather_lo"], w, h)
                put(rect, f, a)
            else:  # leather: arm, panel, back, pad, leg, sleeve
                if face in ("top", "bottom"):
                    f = _noise(r, pal["leather_hi"] if face == "top" else pal["leather_lo"], w, h)
                else:
                    f = _noise(r, pal["leather"], w, h)
                    f[-1, :] = hx(pal["leather_lo"])  # hem
                    if n in ("sleeve", "arm") and h > 4:
                        f[-2, :] = hx(pal["leather_lo2"])
                    if n == "back" and face in ("front", "back"):
                        f[:, w // 2] = hx(pal["leather_lo"])  # center seam
                        f[4, :] = hx(pal["leather_lo2"])  # stitched yoke line
                    if n.startswith("panel") and face in ("front", "back"):
                        f[:, 0] = hx(pal["leather_lo2"])
                    if n == "pad":
                        f[0, :] = hx(pal["leather_hi"])
                put(rect, f)
    return _finish(rgb, alpha)


def paint_beret(model, key):
    """Matte cloth with a little noise, a darker underside, and a thin dark seam where the droop meets the crown."""
    pal = PAL[key]
    r = rng_for("armor_models." + key)
    rgb = np.zeros((model.H, model.W, 3), float)
    alpha = np.zeros((model.H, model.W), bool)
    for b in model.boxes:
        for face, rect in face_rects(b).items():
            x, y, w, h = rect
            if w <= 0 or h <= 0:
                continue
            if face == "bottom":
                f = _noise(r, pal["dark"], w, h, 0.9, 1.1)
            else:
                f = _noise(r, pal["cloth"], w, h, 0.96, 1.06)
                if face in ("front", "back", "left", "right"):
                    f[-1, :] = hx(pal["cloth"]) * 0.8  # lower edge in shade
                if b["name"] == "droop" and face == "top":
                    f[:, -1] = hx(pal["dark"]) * 1.5  # seam against the crown
                    f[:, -2] *= 0.88
                if b["name"] == "crown" and face == "top":
                    f[:, 0] = hx(pal["dark"]) * 1.5
                if b["name"] == "droop" and face == "left":  # the inner end that meets the crown
                    f *= 0.8
            rgb[y:y + h, x:x + w] = f
            alpha[y:y + h, x:x + w] = True
    return _finish(rgb, alpha)


def paint_model(model, key):
    if model.painter is not None:
        return model.painter(model, key)
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
        (m.check or check_variants)(m, VARIANTS[m.name])
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
