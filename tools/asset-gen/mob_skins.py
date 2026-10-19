#!/usr/bin/env python3
"""GregGuns biped NPC skins: zombie_soldier, army_soldier, bandit and dictator, painted for the shared `biped` model.

Design input: docs/techguns-spec/10-entities-npcs/model-notes/{zombie_soldier,army_soldier,bandit,dictator}.md
(the clean-room notes pages). Original procedural art; nothing here reads or copies a Techguns asset.

Layout: the vanilla biped convention (head 0,0; body 16,16; arm 40,16; leg 0,16 on a 64x64 logical sheet), so armor and
held items line up. Only the top half is used, the hat layer is empty, the left limbs mirror the right ones. Painting is
done at the logical 64x64 and the zombie and army skins are then enlarged 2x (each logical texel a 2x2 block) to 128x128.

Run:  python -X utf8 tools/asset-gen/mob_skins.py [--preview <dir>]
Writes src/main/resources/assets/gregguns/textures/entity/<skin>.png; --preview also writes front, back and walk renders.
"""
import math
import os
import random
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import entity_models as em  # noqa: E402

# Vanilla biped unwrap faces on the logical 64x64 sheet: (x, y, w, h).
FACES = {
    "head": {"top": (8, 0, 8, 8), "bottom": (16, 0, 8, 8), "right": (0, 8, 8, 8), "front": (8, 8, 8, 8),
             "left": (16, 8, 8, 8), "back": (24, 8, 8, 8)},
    "body": {"top": (20, 16, 8, 4), "bottom": (28, 16, 8, 4), "right": (16, 20, 4, 12), "front": (20, 20, 8, 12),
             "left": (28, 20, 4, 12), "back": (32, 20, 8, 12)},
    "arm": {"top": (44, 16, 4, 4), "bottom": (48, 16, 4, 4), "right": (40, 20, 4, 12), "front": (44, 20, 4, 12),
            "left": (48, 20, 4, 12), "back": (52, 20, 4, 12)},
    "leg": {"top": (4, 16, 4, 4), "bottom": (8, 16, 4, 4), "right": (0, 20, 4, 12), "front": (4, 20, 4, 12),
            "left": (8, 20, 4, 12), "back": (12, 20, 4, 12)},
}
SKINS = {"zombie_soldier": 2, "army_soldier": 2, "bandit": 1, "dictator": 1}


def hx(s):
    return tuple(int(s[i:i + 2], 16) for i in (1, 3, 5))


class Face:
    """A drawing handle on one face rectangle; coordinates are local to the face (0,0 = top-left texel)."""

    def __init__(self, img, rect):
        self.img, (self.x, self.y, self.w, self.h) = img, rect

    def put(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.img[self.y + y, self.x + x] = hx(c) + (255,) if isinstance(c, str) else tuple(c) + (255,)

    def fill(self, x, y, w, h, c):
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                self.put(xx, yy, c)

    def get(self, x, y):
        return tuple(int(v) for v in self.img[self.y + y, self.x + x][:3])

    def mottle(self, x, y, w, h, base, hi, sh, rng, lo=0.33, up=0.68, rough=0.0):
        """Fill with three close colors laid out by smoothed noise (blotchy, never flat)."""
        field = np.array([[rng.random() for _ in range(w + 2)] for _ in range(h + 2)])
        sm = np.zeros((h, w))
        for yy in range(h):
            for xx in range(w):
                sm[yy, xx] = field[yy:yy + 3, xx:xx + 3].mean()
        sm = (sm - sm.min()) / max(1e-6, sm.max() - sm.min())
        for yy in range(h):
            for xx in range(w):
                v = sm[yy, xx] + (rng.random() - 0.5) * rough
                self.put(x + xx, y + yy, sh if v < lo else hi if v > up else base)

    def grain(self, x, y, w, h, base, hi, sh, rng, p=0.22):
        """Vertical-grain fill: each column gets a tendency, plus scattered flecks."""
        for xx in range(w):
            tone = rng.choice((base, base, hi, sh))
            for yy in range(h):
                r = rng.random()
                self.put(x + xx, y + yy, tone if r > p else (hi if r < p / 2 else sh))


def rng_for(key):
    return random.Random(em.seed_for("mob_skins." + key))


def sheet_faces(img, part):
    return {k: Face(img, r) for k, r in FACES[part].items()}


# ------------------------------------------------------------------------------------------------ zombie soldier
def paint_zombie(img):
    G0, G1, G2 = "#488838", "#78A858", "#386828"
    F0 = "#689848"
    BLOOD, BLOOD_HI, BLOOD_DK = "#980700", "#C02020", "#581810"
    r = rng_for("zombie")
    # head
    h = sheet_faces(img, "head")
    h["top"].mottle(0, 0, 8, 8, G0, "#589848", G2, r)
    h["bottom"].mottle(0, 0, 8, 8, G0, G0, G2, r)
    h["bottom"].fill(2, 2, 4, 4, G2)  # dark square patch (hidden)
    for side in ("right", "left", "back"):
        h[side].mottle(0, 0, 8, 8, G2, G0, G2, r)
    f = h["front"]
    f.mottle(0, 0, 8, 8, G0, G0, G2, r)  # darker green frame
    f.mottle(1, 1, 6, 6, "#689848", G1, G0, r, lo=0.25, up=0.6)  # lighter face patch
    for ex in (1, 5):  # two bright red eyes, two texels wide, two apart
        f.fill(ex, 3, 2, 1, "#E50000")
        f.put(ex, 3, "#F00100")
        f.fill(ex, 4, 2, 1, "#B80200")
    f.fill(2, 6, 4, 1, "#281818")  # thin dark mouth line
    f.put(1, 6, "#386828")
    f.put(6, 6, "#386828")
    # blood on the mob's right side face (large), the back (large) and a scar on the left side
    for (x, y, w, hh) in ((1, 1, 5, 4), (2, 5, 3, 2), (0, 3, 2, 2)):
        h["right"].fill(x, y, w, hh, BLOOD)
    h["right"].mottle(1, 1, 5, 4, BLOOD, BLOOD_HI, BLOOD_DK, r)
    h["right"].fill(2, 5, 3, 2, BLOOD_DK)
    for (x, y, w, hh) in ((2, 0, 5, 3), (3, 3, 3, 3), (1, 4, 1, 3)):
        h["back"].fill(x, y, w, hh, BLOOD)
    h["back"].mottle(3, 3, 3, 3, BLOOD_DK, BLOOD, BLOOD_DK, r)
    h["back"].put(2, 1, BLOOD_HI)
    for i in range(4):  # scar: short diagonal dark line
        h["left"].put(2 + i, 2 + i // 2, "#201010")
    # body
    b = sheet_faces(img, "body")
    SH, SH_HI, SH_DK = "#483828", "#585848", "#382818"
    for k in ("front", "back", "right", "left"):
        w, hh = b[k].w, b[k].h
        b[k].mottle(0, 0, w, hh, SH, SH_HI, SH_DK, r, rough=0.2)
        b[k].fill(0, hh - 2, w, 1, "#383838")
        b[k].fill(0, hh - 1, w, 1, "#282828")  # darker hem band
    b["top"].mottle(0, 0, 8, 4, SH, SH_HI, SH_DK, r)
    b["bottom"].fill(0, 0, 8, 4, "#282828")
    fr = b["front"]
    fr.fill(2, 0, 4, 1, G1)  # pale green neck patch
    fr.fill(3, 1, 2, 1, "#689848")
    fr.fill(0, 3, 8, 1, BLOOD)  # horizontal blood band at chest height, ragged
    for x in range(8):
        if r.random() < 0.6:
            fr.put(x, 4, BLOOD_DK)
    fr.fill(5, 3, 2, 4, BLOOD)  # the wound, off center toward the mob's left
    fr.fill(6, 4, 1, 3, BLOOD_DK)
    fr.put(5, 7, BLOOD_DK)
    fr.put(5, 4, BLOOD_HI)
    fr.put(6, 3, BLOOD_HI)
    for x, ln in ((1, 4), (3, 3), (7, 5)):  # thin vertical streaks
        fr.fill(x, 4, 1, ln, BLOOD_DK)
    bk = b["back"]
    for x, ln in ((1, 6), (3, 3), (6, 5), (5, 2)):
        bk.fill(x, 1, 1, ln, BLOOD_DK if x % 2 else BLOOD)
    bk.put(3, 1, BLOOD_HI)
    bk.fill(0, 9, 8, 1, "#382818")  # darker lower band
    # arm: ragged dark sleeve on the upper third, then bare green forearm
    a = sheet_faces(img, "arm")
    edge = [3, 5, 4, 3, 5, 4, 5, 3, 4, 5, 3, 4, 4, 5, 3, 4]  # per-column sleeve length, around the four sides
    for ki, k in enumerate(("right", "front", "left", "back")):
        face = a[k]
        face.mottle(0, 0, 4, 12, "#689848", G1, G0, r, rough=0.3)
        face.mottle(0, 4, 4, 5, G0, "#689848", G2, r)
        for x in range(4):
            n = edge[ki * 4 + x]
            face.fill(x, 0, 1, n, "#382828")
            for y in range(n):
                if r.random() < 0.3:
                    face.put(x, y, "#582818")
            face.put(x, n - 1, "#281818")
        for _ in range(4):  # blood spatter on the forearm
            face.put(r.randrange(4), r.randrange(5, 12), BLOOD)
        for x in range(4):  # darker blood pool toward the hand
            for y in (10, 11):
                if r.random() < 0.6:
                    face.put(x, y, BLOOD_DK if y == 11 else BLOOD)
    a["front"].fill(1, 1, 2, 2, BLOOD)  # red patch near the shoulder
    a["front"].put(1, 1, BLOOD_HI)
    a["top"].mottle(0, 0, 4, 4, "#382828", "#582818", "#281818", r)
    a["bottom"].mottle(0, 0, 4, 4, G0, "#689848", G2, r)
    # leg: gray-brown trousers, near-black lower third (boots)
    lg = sheet_faces(img, "leg")
    for k in ("right", "front", "left", "back"):
        face = lg[k]
        face.grain(0, 0, 4, 8, "#585848", "#686050", "#484838", r)
        for x in range(4):  # fine vertical wear
            if r.random() < 0.6:
                face.fill(x, r.randrange(0, 3), 1, r.randrange(2, 5), "#484838")
        face.grain(0, 8, 4, 4, "#282828", "#383838", "#181818", r, p=0.3)
        face.fill(0, 8, 4, 1, "#181818")  # cuff line
    lg["front"].fill(1, 2, 2, 2, BLOOD)
    lg["front"].put(1, 3, BLOOD_DK)
    lg["top"].fill(0, 0, 4, 4, "#484838")
    lg["bottom"].fill(0, 0, 4, 4, "#181818")


# ------------------------------------------------------------------------------------------------ army soldier
def paint_army(img):
    HAIR, HAIR_HI, HAIR_DK = "#382818", "#483828", "#281808"
    SK, SK_HI, SK_DK = "#A87858", "#B88868", "#824C32"
    CL, CL_HI, CL_DK = "#485848", "#586858", "#383838"
    r = rng_for("army")
    h = sheet_faces(img, "head")
    h["top"].mottle(0, 0, 8, 8, HAIR, HAIR_HI, HAIR_DK, r)
    h["bottom"].mottle(0, 0, 8, 8, SK_DK, SK_DK, "#754533", r)
    # sides and back: skin, hair across the upper rows with a ragged lower edge
    for k, rows in (("right", 3), ("left", 3), ("back", 4)):
        f = h[k]
        f.mottle(0, 0, 8, 8, SK, SK_HI, SK_DK, r, lo=0.25, up=0.75)
        for x in range(8):
            n = rows + (1 if r.random() < 0.4 else 0) - (1 if r.random() < 0.3 else 0)
            f.mottle(x, 0, 1, n, HAIR, HAIR_HI, HAIR_DK, r)
        if k != "back":
            f.fill(4, 4, 2, 2, SK_DK)  # ear shading
            f.put(4, 4, SK_HI)
    f = h["front"]
    f.mottle(0, 0, 8, 8, SK, SK_HI, SK_DK, r, lo=0.2, up=0.8)
    f.mottle(0, 0, 8, 2, HAIR, HAIR_HI, HAIR_DK, r)
    for x in range(8):  # ragged fringe over the forehead
        if r.random() < 0.6:
            f.put(x, 2, HAIR)
    for x in (0, 7):
        f.fill(x, 2, 1, 2, HAIR_DK)  # hair running down the temples
    for x in (1, 2, 5, 6):  # low dark brows
        f.put(x, 3, HAIR_DK)
    f.put(1, 4, "#F0F0F0")  # eyes: white, blue iris toward the middle
    f.put(2, 4, "#2040A0")
    f.put(5, 4, "#2040A0")
    f.put(6, 4, "#F0F0F0")
    f.put(3, 5, SK_DK)  # nose smudge
    f.put(4, 5, "#9A6648")
    f.fill(3, 6, 2, 1, SK_DK)  # mouth smudge
    # body
    b = sheet_faces(img, "body")
    for k in ("front", "back", "right", "left"):
        w, hh = b[k].w, b[k].h
        b[k].mottle(0, 0, w, hh, CL, CL_HI, CL_DK, r, lo=0.18, up=0.8, rough=0.1)
        b[k].fill(0, 8, w, 1, "#383838")  # belt / webbing wraps all four sides
        b[k].fill(0, 9, w, 1, "#586858")
        b[k].fill(0, 10, w, 1, "#383838")
        b[k].grain(0, 8, w, 1, "#383838", "#383838", "#181818", r, p=0.3)
        b[k].fill(0, 9, w, 1, "#586858")
        for x in range(w):
            if r.random() < 0.25:
                b[k].put(x, 9, "#485848")
    b["top"].mottle(0, 0, 8, 4, CL, CL_HI, CL_DK, r)
    b["bottom"].fill(0, 0, 8, 4, CL_DK)
    fr = b["front"]
    fr.fill(2, 0, 4, 1, SK)  # rounded tan neckline
    fr.fill(3, 1, 2, 1, SK_HI)
    fr.put(2, 0, SK_DK)
    fr.put(5, 0, SK_DK)
    for x0 in (1, 5):  # two chest pouches: lighter flap, dark body
        fr.fill(x0, 3, 2, 1, "#707868")
        fr.fill(x0, 4, 2, 2, "#383838")
        fr.fill(x0, 6, 2, 1, "#181818")
    for k in ("right", "left"):  # dark vertical oval slot on each side
        s = b[k]
        s.fill(1, 3, 2, 4, "#383838")
        s.put(1, 3, CL)
        s.put(2, 3, CL)
        s.put(1, 6, CL)
        s.put(2, 6, CL)
        s.fill(1, 4, 1, 2, "#181818")
    # arm: sleeve to a third of the way, cuff line, then bare tan forearm
    a = sheet_faces(img, "arm")
    for k in ("right", "front", "left", "back"):
        face = a[k]
        face.mottle(0, 0, 4, 4, CL, CL_HI, CL_DK, r, lo=0.2, up=0.8)
        face.fill(0, 3, 4, 1, CL_DK)
        face.grain(0, 4, 4, 8, "#B88878", "#BE8B70", "#A87868", r, p=0.28)
        for x in range(4):
            if r.random() < 0.35:
                face.put(x, r.randrange(6, 12), "#754533")
    a["top"].mottle(0, 0, 4, 4, CL, CL_HI, CL_DK, r)
    a["bottom"].fill(0, 0, 4, 4, "#A87868")
    p = a["right"]  # tiny flag patch (outer face): dark roundel with red, white and blue squares
    p.fill(1, 0, 3, 3, "#101010")
    p.put(1, 1, "#C01818")
    p.put(2, 1, "#F0F0F0")
    p.put(3, 1, "#2040A0")
    # leg
    lg = sheet_faces(img, "leg")
    for k in ("right", "front", "left", "back"):
        face = lg[k]
        face.mottle(0, 0, 4, 8, CL, CL_HI, CL_DK, r, lo=0.2, up=0.8, rough=0.1)
        face.grain(0, 8, 4, 4, "#181818", "#282828", "#080808", r, p=0.25)
        face.fill(0, 11, 4, 1, "#282828")
    lg["front"].fill(1, 1, 2, 3, CL_DK)  # dark patch on the thigh
    for k in ("back", "right"):  # flap pockets
        lg[k].fill(1, 2, 2, 1, "#707868")
        lg[k].fill(1, 3, 2, 2, "#383838")
    lg["top"].fill(0, 0, 4, 4, CL_DK)
    lg["bottom"].fill(0, 0, 4, 4, "#080808")


# ------------------------------------------------------------------------------------------------ bandit / dictator
def paint_bandit(img, dictator=False):
    HAIR, HAIR_HI, HAIR_DK = "#181818", "#282828", "#180808"
    SK, SK_HI, SK_DK = "#886858", "#A88878", "#683838"
    SHIRT, SHIRT_HI, SHIRT_DK = "#A8A8A8", "#AEAEAE", "#8C8C8C"
    V, V_HI, V_DK = "#684838", "#785848", "#483828"
    ARM, ARM_HI, ARM_DK = "#886858", "#806657", "#6C4B36"
    TR, TR_HI, TR_DK = "#483828", "#514031", "#2D241C"
    BOOT = "#282828"
    h = sheet_faces(img, "head")
    r = rng_for("bandit.head")
    h["top"].mottle(0, 0, 8, 8, HAIR, HAIR_HI, HAIR_DK, r, lo=0.3, up=0.72)
    h["bottom"].mottle(0, 0, 8, 8, SK_DK, SK_DK, "#483828", r)
    h["back"].mottle(0, 0, 8, 8, HAIR, HAIR_HI, HAIR_DK, r, lo=0.3, up=0.72)
    r2 = rng_for("bandit.sides")
    for k, ear in (("right", (5, 4)), ("left", (1, 4))):  # hair to the ears; skin patch low near the front
        f = h[k]
        f.mottle(0, 0, 8, 8, HAIR, HAIR_HI, HAIR_DK, r2, lo=0.3, up=0.72)
        f.fill(ear[0], ear[1], 2, 2, SK)
        f.put(ear[0], ear[1], SK_HI)
        f.put(ear[0] + (1 if k == "right" else 0), ear[1] + 1, SK_DK)
    f = h["front"]
    f.mottle(0, 0, 8, 8, HAIR, HAIR_HI, HAIR_DK, r, lo=0.3, up=0.72)  # hair frame
    f.mottle(1, 2, 6, 6, SK, SK_HI, "#785A4A", rng_for("bandit.face"), lo=0.2, up=0.85)
    f.fill(1, 2, 6, 1, SK_HI)  # pale forehead strip under the fringe
    f.put(1, 1, HAIR)
    f.put(6, 1, HAIR)
    f.fill(1, 3, 6, 1, "#302020")  # heavy brows
    f.fill(1, 5, 1, 1, SK_HI)
    f.fill(6, 5, 1, 1, SK_HI)
    f.fill(3, 5, 2, 1, "#785040")  # nose
    f.fill(2, 6, 4, 1, "#483828")  # mouth and beard
    f.fill(3, 6, 2, 1, "#301818")
    f.fill(2, 7, 4, 1, "#483828")
    f.put(1, 7, SK)
    f.put(6, 7, SK)
    if dictator:
        f.fill(0, 3, 8, 2, "#080808")  # black sunglasses bar, one texel past each side of the face
        f.put(2, 3, "#181818")
        f.put(5, 3, "#181818")
        f.fill(3, 4, 2, 1, "#181818")  # bridge
        f.fill(3, 4, 2, 1, "#080808")
        f.fill(1, 6, 6, 1, "#483828")  # mustache band, darker than the face
        f.put(3, 6, "#684838")
        f.put(4, 6, "#684838")
        f.mottle(1, 7, 6, 1, "#684838", "#886858", "#483828", rng_for("dictator.chin"), lo=0.3, up=0.7)
        f.fill(2, 7, 4, 1, "#684838")
        for x in (1, 3, 6):
            f.put(x, 7, "#886858")
        f.put(2, 7, "#483828")
        f.put(5, 7, "#483828")
        h["right"].fill(4, 3, 4, 1, "#080808")  # sunglasses arms running back toward the hair
        h["left"].fill(0, 3, 4, 1, "#080808")
    # body
    b = sheet_faces(img, "body")
    rb = rng_for("bandit.body")
    for k in ("front", "back", "right", "left"):
        w, hh = b[k].w, b[k].h
        base, hi, dk = (SHIRT, SHIRT_HI, SHIRT_DK) if k in ("front", "back") else (SHIRT_DK, SHIRT_DK, "#7C7C7C")
        b[k].mottle(0, 0, w, hh, base, hi, dk, rb, lo=0.1, up=0.9, rough=0.05)
        for x in range(w):  # faint vertical folds
            if rb.random() < 0.45:
                b[k].fill(x, rb.randrange(0, 3), 1, rb.randrange(4, 8), dk if rb.random() < 0.5 else hi)
        b[k].fill(0, 10, w, 1, "#483828")  # trousers and belt line showing under the shirt
        b[k].fill(0, 11, w, 1, "#282828")
        b[k].put(rb.randrange(w), 10, TR_DK)
    b["top"].mottle(0, 0, 8, 4, SHIRT_DK, SHIRT, "#7C7C7C", rb)
    b["bottom"].fill(0, 0, 8, 4, TR_DK)
    fr = b["front"]
    fr.fill(2, 0, 4, 1, V)  # brown V collar
    fr.fill(3, 1, 2, 1, V)
    fr.put(2, 0, V_HI)
    fr.put(5, 0, V_DK)
    fr.put(4, 1, V_DK)
    b["back"].fill(3, 0, 2, 1, V)
    if dictator:  # near-black belt line on all four sides (the one differing area of the torso)
        for k in ("front", "back", "right", "left"):
            b[k].fill(0, 10, b[k].w, 2, "#080808")
            for x in range(0, b[k].w, 3):
                b[k].put(x, 10, "#181818")
    # arm
    a = sheet_faces(img, "arm")
    ra = rng_for("bandit.arm")
    for k in ("right", "front", "left", "back"):
        face = a[k]
        face.grain(0, 4, 4, 8, ARM, ARM_HI, ARM_DK, ra, p=0.18)
        face.mottle(0, 0, 4, 4, SHIRT, SHIRT_HI, SHIRT_DK, ra)
        face.fill(0, 3, 4, 1, SHIRT_DK)
    a["top"].mottle(0, 0, 4, 4, SHIRT, SHIRT_HI, SHIRT_DK, ra)
    a["bottom"].mottle(0, 0, 4, 4, ARM_DK, ARM, ARM_DK, ra)
    # leg
    lg = sheet_faces(img, "leg")
    rl = rng_for("bandit.leg")
    for k in ("right", "front", "left", "back"):
        face = lg[k]
        face.mottle(0, 0, 4, 12, TR, TR_HI, TR_DK, rl, lo=0.2, up=0.8)
        for x in range(4):  # darker vertical wrinkle lines
            if rl.random() < 0.7:
                face.fill(x, rl.randrange(0, 4), 1, rl.randrange(3, 7), TR_DK)
        face.fill(0, 10, 4, 2, BOOT)
        face.put(rl.randrange(4), 10, "#262626")
        face.put(rl.randrange(4), 11, "#151515")
    lg["top"].fill(0, 0, 4, 4, TR_DK)
    lg["bottom"].fill(0, 0, 4, 4, "#151515")


PAINTERS = {
    "zombie_soldier": paint_zombie,
    "army_soldier": paint_army,
    "bandit": lambda img: paint_bandit(img, False),
    "dictator": lambda img: paint_bandit(img, True),
}


def paint(name):
    """The finished RGBA sheet: painted at 64x64 logical, enlarged to the skin's size (2x for zombie and army)."""
    img = np.zeros((64, 64, 4), dtype=np.uint8)
    PAINTERS[name](img)
    k = SKINS[name]
    if k > 1:
        img = np.repeat(np.repeat(img, k, axis=0), k, axis=1)
    return Image.fromarray(img, "RGBA")


# ------------------------------------------------------------------------------------------------ preview renderer
def _box_faces(part_name, box):
    """Faces of a model box as (name, 4 corner points in part space, uv rect, u-axis, v-axis texel directions)."""
    x, y, z = box["offset"]
    w, hh, d = box["size"]
    x1, y1, z1 = x + w, y + hh, z + d
    return {  # corners: texel (0,0), (w,0), (w,h), (0,h) in face-local coordinates
        "front": [(x, y, z), (x1, y, z), (x1, y1, z), (x, y1, z)],
        "back": [(x1, y, z1), (x, y, z1), (x, y1, z1), (x1, y1, z1)],
        "right": [(x, y, z1), (x, y, z), (x, y1, z), (x, y1, z1)],
        "left": [(x1, y, z), (x1, y, z1), (x1, y1, z1), (x1, y1, z)],
        "top": [(x, y, z1), (x1, y, z1), (x1, y, z), (x, y, z)],
        "bottom": [(x, y1, z), (x1, y1, z), (x1, y1, z1), (x, y1, z1)],
    }


NORMALS = {"front": (0, 0, -1), "back": (0, 0, 1), "right": (-1, 0, 0), "left": (1, 0, 0), "top": (0, -1, 0),
           "bottom": (0, 1, 0)}
SHADE = {"front": 1.0, "back": 0.82, "right": 0.9, "left": 0.9, "top": 1.05, "bottom": 0.6}


def render(name, yaw=0.0, walk=0.0, scale=14, aim=False, pitch=18.0):
    """Orthographic textured render of the `biped` model. yaw 0 = seen from the front (-Z); walk = limb swing amount
    at the cosine peak (1 = full stride); aim = the aimed-gun pose."""
    m = em.biped()
    m.pack()
    tex = paint(name)
    tw = tex.width
    k = tw // 64
    px = tex.load()
    legs = 80.214 * walk
    arms = 57.296 * walk
    rot = {"rightLeg": legs, "leftLeg": -legs, "rightArm": -arms, "leftArm": arms}
    if aim:
        rot["rightArm"] = -90.0
        rot["leftArm"] = -90.0
    yawr, pr = math.radians(yaw), math.radians(pitch)
    polys = []
    for part in m.parts:
        ang = math.radians(rot.get(part.name, 0.0))
        yaw_arm = {"rightArm": -5.73, "leftArm": 28.65}.get(part.name, 0.0) if aim else 0.0
        for box in part.boxes:
            fcs = _box_faces(part.name, box)
            u0, v0 = box["uv"]
            kind = "head" if part.name == "head" else "body" if part.name == "body" else "arm" if "Arm" in part.name else "leg"
            for fname, corners in fcs.items():
                mir = box["mirror"]
                swap = {"left": "right", "right": "left"}
                fx, fy, fw, fh = FACES[kind][swap.get(fname, fname) if mir else fname]
                nrm = NORMALS[fname]
                if fw == 0:
                    continue

                def xf(p):
                    a = p
                    if ang:  # rotate about X at the part pivot
                        yy = a[1] * math.cos(ang) - a[2] * math.sin(ang)
                        zz = a[1] * math.sin(ang) + a[2] * math.cos(ang)
                        a = (a[0], yy, zz)
                    if yaw_arm:
                        ya = math.radians(yaw_arm)
                        a = (a[0] * math.cos(ya) + a[2] * math.sin(ya), a[1], -a[0] * math.sin(ya) + a[2] * math.cos(ya))
                    a = (a[0] + part.pivot[0], a[1] + part.pivot[1], a[2] + part.pivot[2])
                    # view: yaw about Y, then tilt for the elevated 3/4 view
                    x2 = a[0] * math.cos(yawr) + a[2] * math.sin(yawr)
                    z2 = -a[0] * math.sin(yawr) + a[2] * math.cos(yawr)
                    y3 = a[1] * math.cos(pr) - z2 * math.sin(pr)
                    z3 = a[1] * math.sin(pr) + z2 * math.cos(pr)
                    return (x2, y3, z3)

                def rotn(n):
                    nn = n
                    if ang:
                        nn = (nn[0], nn[1] * math.cos(ang) - nn[2] * math.sin(ang), nn[1] * math.sin(ang) + nn[2] * math.cos(ang))
                    x2 = nn[0] * math.cos(yawr) + nn[2] * math.sin(yawr)
                    z2 = -nn[0] * math.sin(yawr) + nn[2] * math.cos(yawr)
                    return z2 * math.cos(pr) + nn[1] * math.sin(pr)

                if rotn(nrm) >= 0.0:  # facing away from the camera (camera looks along +depth)
                    continue
                c00, c10, c11, c01 = corners
                for iy in range(fh):
                    for ix in range(fw):
                        a = ix / fw
                        b2 = iy / fh
                        a1 = (ix + 1) / fw
                        b1 = (iy + 1) / fh

                        def pt(s, t):
                            return tuple(
                                (c00[i] * (1 - s) + c10[i] * s) * (1 - t) + (c01[i] * (1 - s) + c11[i] * s) * t
                                for i in range(3))

                        # left limbs mirror: texel column flipped
                        tx = fx + (fw - 1 - ix if mir else ix)
                        ty = fy + iy
                        col = [px[tx * k + dx, ty * k + dy] for dx in range(k) for dy in range(k)]
                        col = tuple(sum(c[i] for c in col) // len(col) for i in range(4))
                        if col[3] == 0:
                            continue
                        quad = [xf(pt(a, b2)), xf(pt(a1, b2)), xf(pt(a1, b1)), xf(pt(a, b1))]
                        depth = sum(q[2] for q in quad) / 4
                        s = SHADE[fname]
                        polys.append((depth, quad, tuple(min(255, int(col[i] * s)) for i in range(3))))
    polys.sort(key=lambda t: -t[0])
    W, H = 22 * scale, 34 * scale
    im = Image.new("RGB", (W, H), (58, 66, 74))
    dr = ImageDraw.Draw(im)
    for _, quad, col in polys:
        pts = [(W / 2 + q[0] * scale, scale + (q[1] + 8) * scale) for q in quad]
        dr.polygon(pts, fill=col, outline=col)
    return im


def preview_sheet(name, path):
    views = [render(name, 0, 0.0, aim=False, pitch=0), render(name, 180, 0.0, pitch=0),
             render(name, -90, 1.0, pitch=0), render(name, 35, 1.0, pitch=14), render(name, 25, 0.0, aim=True, pitch=14)]
    W = sum(v.width for v in views) + 6 * (len(views) + 1)
    out = Image.new("RGB", (W, views[0].height + 12), (40, 44, 50))
    x = 6
    for v in views:
        out.paste(v, (x, 6))
        x += v.width + 6
    out.save(path)
    return out


def main(argv):
    outdir = em.TEXDIR
    os.makedirs(outdir, exist_ok=True)
    for name in SKINS:
        img = paint(name)
        img.save(os.path.join(outdir, name + ".png"))
        print("%-15s %dx%d -> %s" % (name, img.width, img.height, name + ".png"))
    if "--preview" in argv:
        d = argv[argv.index("--preview") + 1]
        os.makedirs(d, exist_ok=True)
        for name in SKINS:
            preview_sheet(name, os.path.join(d, name + ".png"))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
