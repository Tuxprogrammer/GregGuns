#!/usr/bin/env python3
"""GregGuns model textures (kind: model): guns, armor layers, mob skins, projectiles, machines.

Original procedural art, fixed seeds, no Techguns input. We do not have Techguns' UV layouts,
so:
  * armor layers and biped mob skins use the standard Minecraft 1.7.10 / modern skin unwrap
    (scaled by width/64) so they map correctly on vanilla-layout models;
  * everything else (guns, packs, masks, turret, charges, atlases) is a *packed box atlas*:
    named boxes are unwrapped with the standard box layout and shelf-packed to fill the
    footprint. The exact rectangles are written to tools/asset-gen/model_layouts.md so a
    modeler can UV to them.

Run from repo root:  python -I -X utf8 tools/asset-gen/models.py
Outputs: src/main/resources/assets/gregguns/<manifest path>, tools/asset-gen/credits/models.csv,
tools/asset-gen/model_layouts.md (includes the GUI sprite map from gui.py),
docs/assets/previews/models.png.
"""
import csv
import json
import os
import re
import sys

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gui  # noqa: E402
from modeltex import (DET, Tex, box_uv, hx, material, mul, pack_boxes, paint_box, panelize,  # noqa: E402
                      rect, rng_for, seed_for, vnoise, wear)

ROOT = gui.ROOT
RES = gui.RES
PRE = gui.PRE
MANIFEST = os.path.join(ROOT, "tools", "asset-gen", "manifest.json")

LAYOUT_DOC = {}  # asset id -> list of lines


# ---- extra detail painters -------------------------------------------------
def d_spots(rgb, r, face, ctx):
    h, w = rgb.shape[:2]
    for _ in range(max(1, w * h // 40)):
        x, y = int(r.integers(0, w)), int(r.integers(0, h)); n = int(r.integers(1, 3))
        rect(rgb, x, y, n, n, ctx.get("spot", (40, 25, 15)), .5)


def d_flag(rgb, r, face, ctx):
    h, w = rgb.shape[:2]
    if h < 6:
        return
    t = h // 3
    rect(rgb, 0, 0, w, t, "#C03030"); rect(rgb, 0, t, w, t, "#E8E8E8"); rect(rgb, 0, 2 * t, w, h - 2 * t, "#203060")


def d_display(rgb, r, face, ctx):
    h, w = rgb.shape[:2]
    if w < 6 or h < 4:
        return
    rect(rgb, 1, 1, w - 2, h - 2, (14, 10, 10))
    on = ctx.get("armed")
    g = hx(ctx.get("glow", "#FF2020"))
    for i in range(4):
        x = 2 + i * max(2, (w - 4) // 4)
        for y in (2, h - 3):
            rect(rgb, x, y, 1, 1, g if on else (60, 20, 20), 1.0)
        if on and h > 5:
            rect(rgb, x, h // 2, 1, 1, g * 0.8 + 40, 1.0)


def d_latch(rgb, r, face, ctx):
    h, w = rgb.shape[:2]
    if face != "front" or h < 6:
        return
    rect(rgb, w // 2 - 3, h - 5, 6, 5, "#D0B050"); rect(rgb, w // 2 - 3, h - 5, 6, 1, "#F0E080")
    rect(rgb, w // 2 - 1, h - 3, 2, 2, "#2A2A2A")


def d_bands(rgb, r, face, ctx):
    h, w = rgb.shape[:2]
    c = ctx.get("accent", "#643200")
    for x in (w // 5, w - w // 5 - 3):
        if x > 0:
            rect(rgb, x, 0, 3, h, c, 1.0); mul(rgb, x, 0, 3, 1, 1.2); mul(rgb, x, 0, 1, h, 1.15); mul(rgb, x + 2, 0, 1, h, .7)


def d_stencil(rgb, r, face, ctx):
    h, w = rgb.shape[:2]
    if h < 8 or w < 10:
        return
    for k in range(3):
        rect(rgb, w // 2 - 7 + k * 5, h // 2 - 2, 3, 5, "#C8C8A0", .75)


for _n, _f in list(globals().items()):
    if _n.startswith("d_"):
        DET[_n[2:]] = _f

CAMO = [
    ("blotch", ["#4B5320", "#6B7D3A", "#3A2F1E", "#20260F"]),    # 0 woodland blotches
    ("digital", ["#C2A878", "#A88C5A", "#7D6842", "#DCC9A0"]),   # 1 desert pixel camo
    ("splinter", ["#E8EEF2", "#B8C4CC", "#8896A0", "#5A6670"]),  # 2 arctic splinter shards
    ("tiger", ["#6E7075", "#2E3033", "#4A4C50", "#16171A"]),     # 3 urban tiger stripes
    ("bark", ["#4A3A24", "#2E3A1E", "#6A5232", "#1E1A10"]),      # 4 bark streaks (wood)
    ("flecks", ["#EEF2F4", "#D8DEE2", "#A8B0B8", "#FFFFFF"]),    # 5 snow flecks
]
SOLID = {"black": "#26262A", "green": "#4E6B3A", "dark_green": "#2F4A32", "red": "#8A2A24", "silver": "#B8BCC4",
         "bronze": "#B8733A", "white": "#E8E8E8", "lightgreen": "#8FBF6A", "iron": "#9A9A9E", "rusty": "#8A5230",
         "dark": "#2C2F3A", "dark2": "#3A2C2C", "swat": "#1C2030", "security": "#1F2E4A", "1": "#4A8A70",
         "hazard": "#D0A020"}
CAMO_NAMES = {"forest": 0, "wood": 4, "desert": 1, "snow": 5, "arctic": 2}


def tone(c, f):
    return "#%02X%02X%02X" % tuple(int(max(0, min(255, v * f))) for v in hx(c))


def mix(a, b, t):
    return "#%02X%02X%02X" % tuple(int(x * (1 - t) + y * t) for x, y in zip(hx(a), hx(b)))


# ---- generic packed-atlas painter -------------------------------------------
def paint_atlas(aid, W, H, parts, matmap, ctx, r_key=None, region=None, camo=None, cell=(5, 12), wear_amt=0.3, tex=None):
    """parts: [(name, role, w, h, d, details)]. matmap: role -> (material, base, pal|None).
    Returns the Tex; documents the layout."""
    r = rng_for(r_key or aid)
    x0, y0, rw, rh = region or (0, 0, W, H)
    tex = tex or Tex(W, H)
    pl = pack_boxes([(p[0], p[2], p[3], p[4]) for p in parts], rw, rh, x0, y0)
    pos = {n: (u, v, w, h, d) for n, u, v, w, h, d in pl}
    lines = []
    for name, role, _, _, _, details in parts:
        u, v, w, h, d = pos[name]
        mat, base, pal = matmap[role]
        if camo is not None and role in camo[1]:
            mat, base, pal = "camo", "#808080", CAMO[camo[0]]
        pr = rng_for("%s/%s" % (r_key or aid, name))
        c = dict(ctx)
        if role == "glow":
            c["glow"] = ctx.get("glow", "#FF8000")
        paint_box(tex, u, v, w, h, d, mat, base, pr, details, c, cell=cell, wear_amt=wear_amt, pal=pal)
        lines.append("`%s` box u=%d v=%d w=%d h=%d d=%d" % (name, u, v, w, h, d))
    LAYOUT_DOC[aid] = lines
    return tex


# ---- guns -------------------------------------------------------------------
ARCH = {
    "pistol": [("slide", "metal", 12, 3, 2.4, ["rails", "label"]), ("frame", "dark", 10, 2, 2.2, ["label"]),
               ("barrel", "metal", 5, 1.6, 1.6, ["rings"]), ("grip", "furn", 3, 7, 2.4, ["knurl"]),
               ("magazine", "dark", 2.6, 6, 2, ["label"]), ("sight", "dark", 3, 1, 1, []), ("guard", "dark", 4, 2, 1, [])],
    "revolver": [("frame", "metal", 8, 4, 2.2, ["label"]), ("barrel", "metal", 10, 2, 2, ["rails"]),
                 ("cylinder", "dark", 4, 4, 4, ["rings"]), ("grip", "furn", 3, 7, 2.4, ["knurl"]),
                 ("hammer", "dark", 2, 2, 1, []), ("sight", "dark", 1, 1, 1, [])],
    "smg": [("receiver", "metal", 12, 4, 3, ["vents", "label"]), ("barrel", "dark", 8, 1.5, 1.5, ["rings"]),
            ("stock", "furn", 8, 3, 2, ["knurl"]), ("grip", "furn", 3, 6, 2.4, ["knurl"]),
            ("magazine", "dark", 3, 8, 2, ["label"]), ("sight", "dark", 4, 1, 1.5, ["rails"]),
            ("handguard", "metal", 7, 3, 3, ["vents"])],
    "rifle": [("receiver", "metal", 14, 4, 3, ["rails", "label"]), ("handguard", "furn", 12, 3, 3, ["vents"]),
              ("barrel", "dark", 14, 1.5, 1.5, ["rings"]), ("stock", "furn", 12, 5, 2.5, ["knurl"]),
              ("grip", "furn", 3, 6, 2.4, ["knurl"]), ("magazine", "dark", 4, 8, 2, ["label"]),
              ("optic", "dark", 6, 2, 2, ["lens"]), ("muzzle", "metal", 3, 2, 2, ["grill"])],
    "shotgun": [("receiver", "metal", 10, 4, 2.5, ["label"]), ("barrel", "dark", 16, 2, 2.5, ["rings"]),
                ("pump", "furn", 8, 2.5, 3, ["knurl"]), ("stock", "furn", 10, 4, 2, ["knurl"]),
                ("grip", "furn", 3, 5, 2, ["knurl"]), ("tube", "dark", 14, 1.5, 1.5, [])],
    "sniper": [("receiver", "metal", 14, 4, 3, ["label"]), ("barrel", "dark", 22, 1.6, 1.6, ["rings"]),
               ("stock", "furn", 14, 5, 2.5, ["knurl"]), ("scope", "dark", 14, 2.5, 2.5, ["lens", "rings"]),
               ("magazine", "dark", 3, 5, 2, ["label"]), ("bipod", "metal", 5, 5, 1, [])],
    "lmg": [("receiver", "metal", 16, 5, 4, ["vents", "label"]), ("barrel", "dark", 14, 2, 2, ["rings"]),
            ("handguard", "metal", 10, 3, 3.5, ["vents"]), ("stock", "furn", 10, 5, 3, ["knurl"]),
            ("grip", "furn", 3, 6, 2.4, ["knurl"]), ("ammo_box", "dark", 7, 7, 5, ["hazard", "label"]),
            ("carry_handle", "metal", 8, 2, 1, []), ("bipod", "metal", 5, 5, 1, [])],
    "minigun": [("motor", "metal", 10, 6, 6, ["vents", "label"]), ("barrel_cluster", "dark", 22, 5, 5, ["grill", "rings"]),
                ("housing", "metal", 8, 7, 7, ["hazard"]), ("grip", "furn", 3, 6, 3, ["knurl"]),
                ("feed", "dark", 10, 3, 3, ["rings"]), ("handle", "metal", 8, 2, 2, []), ("battery", "dark", 5, 6, 4, ["cells"])],
    "launcher": [("tube", "metal", 24, 5, 5, ["rings", "label"]), ("rear_cone", "dark", 5, 6, 6, ["grill"]),
                 ("grip", "furn", 3, 6, 2.5, ["knurl"]), ("sight", "dark", 5, 2, 1.5, ["rails"]),
                 ("foregrip", "furn", 3, 5, 2.5, ["knurl"]), ("warhead", "dark", 6, 4, 4, ["hazard"]),
                 ("shoulder", "furn", 6, 3, 3, ["knurl"])],
    "energy": [("body", "metal", 14, 5, 3.5, ["label", "vents"]), ("emitter", "dark", 8, 3, 3, ["coil"]),
               ("core", "glow", 6, 3, 3, ["strip"]), ("grip", "furn", 3, 6, 2.4, ["knurl"]),
               ("cell", "dark", 4, 6, 3, ["cells"]), ("fins", "metal", 8, 4, 1, ["strip"]), ("stock", "furn", 8, 4, 2.5, ["knurl"])],
    "grenade": [("body", "metal", 8, 12, 8, ["grill", "label"]), ("cap", "dark", 4, 3, 4, []),
                ("lever", "dark", 2, 8, 1, []), ("ring", "metal", 3, 3, 1, []), ("band", "furn", 8, 2, 8, ["stripe"])],
    "stielgranate": [("head", "metal", 7, 8, 7, ["label", "rings"]), ("handle", "furn", 3, 14, 3, ["knurl"]),
                     ("cap", "dark", 3, 2, 3, []), ("fuse", "dark", 2, 3, 2, [])],
    "chainsaw": [("bar", "metal", 18, 2, 1, ["rings"]), ("chain", "dark", 18, 1, 1, ["grill"]),
                 ("motor", "metal", 10, 7, 5, ["vents", "hazard"]), ("handle", "furn", 3, 8, 3, ["knurl"]),
                 ("guard", "dark", 3, 5, 5, []), ("tank", "dark", 6, 4, 4, ["label"])],
    "hammer": [("head", "metal", 8, 6, 6, ["hazard", "cells"]), ("shaft", "furn", 3, 14, 3, ["knurl"]),
               ("grip", "dark", 3, 5, 3, ["knurl"]), ("battery", "dark", 5, 5, 4, ["cells"]), ("coil", "metal", 6, 3, 3, ["coil"])],
    "flame": [("tank_a", "dark", 5, 10, 5, ["rings", "hazard"]), ("tank_b", "dark", 5, 10, 5, ["rings", "label"]),
              ("nozzle", "metal", 10, 2.5, 2.5, ["rings"]), ("hose", "dark", 8, 1, 1, ["grill"]),
              ("grip", "furn", 3, 6, 2.4, ["knurl"]), ("frame", "metal", 12, 3, 3, ["label"]), ("pilot", "glow", 2, 2, 2, ["strip"])],
}

# gun id -> (archetype, furniture, glow)
GUNS = {
    "aug": ("rifle", "polymer", None), "bio_gun": ("energy", "polymer", "#6EFF3C"), "flamethrower": ("flame", "polymer", "#FF8A20"),
    "laser_rifle": ("energy", "polymer", "#FF3A3A"), "nuclear_death_ray": ("energy", "polymer", "#C8FF32"),
    "pdw": ("smg", "polymer", None), "tesla_rifle": ("energy", "polymer", "#78C8FF"), "ak47": ("rifle", "wood", None),
    "alien_blaster": ("energy", "polymer", "#BEFF3C"), "as50": ("sniper", "polymer", None),
    "blaster_rifle": ("energy", "polymer", "#5078FF"), "bolt_action_rifle": ("sniper", "wood", None),
    "chainsaw": ("chainsaw", "polymer", None), "combat_shotgun": ("shotgun", "polymer", None),
    "nether_blaster": ("energy", "polymer", "#FF5A14"), "frag_grenade": ("grenade", "polymer", None),
    "golden_revolver": ("revolver", "wood", None), "grenade_launcher": ("launcher", "polymer", None),
    "grim_reaper": ("energy", "polymer", "#96FFAA"), "handcannon": ("revolver", "wood", None),
    "scatter_laser_rifle": ("energy", "polymer", "#FF3C3C"), "m4": ("rifle", "polymer", None),
    "infiltrator": ("rifle", "polymer", None), "mac10": ("smg", "polymer", None), "lmg": ("lmg", "polymer", None),
    "deatomizer_pistol": ("pistol", "polymer", "#B478FF"), "minigun": ("minigun", "polymer", None),
    "nuke_launcher": ("launcher", "polymer", "#C8FF32"), "pistol_9mm": ("pistol", "polymer", None),
    "power_hammer": ("hammer", "polymer", "#FFB020"), "power_hammer_adv": ("hammer", "polymer", "#C878FF"),
    "pulse_rifle": ("energy", "polymer", "#FF9628"), "revolver": ("revolver", "wood", None),
    "rocket_launcher": ("launcher", "polymer", None), "sawed_off": ("shotgun", "wood", None),
    "scar": ("rifle", "polymer", None), "sonic_rifle": ("energy", "polymer", "#78FFE6"),
    "stielgranate": ("stielgranate", "wood", None), "thompson_smg": ("smg", "wood", None),
    "vector": ("smg", "polymer", None),
}
GLOW_ARCH_EXTRA = {"pistol": [("core", "glow", 3, 1.5, 1.5, ["strip"])]}


def gun_texture(gid, w, h):
    base_id = re.sub(r"_camo\d+$", "", gid)
    camo_n = re.search(r"_camo(\d+)$", gid)
    arch, furn, glow = GUNS[base_id]
    mf = [p for p in ARCH[arch]]
    if glow and not any(p[1] == "glow" for p in mf):
        mf = mf + GLOW_ARCH_EXTRA.get(arch, [("core", "glow", 3, 1.5, 1.5, ["strip"])])
    return mf, arch, furn, glow, base_id, int(camo_n.group(1)) if camo_n else None


def make_gun(man):
    gid = man["id"].replace("model.gun.", "")
    w, h = man["width"], man["height"]
    parts, arch, furn, glow, base_id, camo_n = gun_texture(gid, w, h)
    pal = man["palette"]
    metal = tone(pal[0], 0.82)
    sec = pal[1] if len(pal) > 1 else "#303030"
    m_dark = tone(mix(metal, "#202226", 0.55), 1.0)
    if furn == "wood":
        furn_m = ("wood", tone(sec, 1.45), None)
    else:
        furn_m = ("polymer", tone(sec, 1.15), None)
    mm = {"metal": ("steel", metal, None), "dark": ("steel", m_dark, None), "furn": furn_m,
          "glow": ("plain", glow or "#FF8000", None)}
    ctx = {"glow": glow or "#FF8000", "accent": tone(pal[0], .6) if arch != "energy" else (glow or "#FF8000"),
           "lens": "#3A9AD0" if not glow else glow}
    camo = ((camo_n - 1) % 4, ("furn", "dark")) if camo_n else None
    if camo_n:
        tint = ["#5A6A3A", "#B89A60", "#C8D0D8", "#34363A"][(camo_n - 1) % 4]
        mm["metal"] = ("steel", mix(metal, tint, .55), None)
    t = paint_atlas("model.gun." + gid, w, h, parts, mm, ctx, r_key="model.gun." + base_id, camo=camo)
    return t


# ---- armor -----------------------------------------------------------------
SETS = {
    "t1_combat": dict(cloth="#5C6150", cmat="fabric", plate=None, accent="#C8A040", flags=["vest"]),
    "t1_miner": dict(cloth="#7A6648", cmat="fabric", plate=None, accent="#E0C030", flags=["lamp", "hazard"]),
    "t1_scout": dict(cloth="#7A6342", cmat="leather", plate="#B8733A", accent="#FF8000", flags=["goggles"]),
    "t2_combat": dict(cloth="#4B5648", cmat="fabric", plate=None, accent="#E8E8E8", visor="#203040", flags=["vest", "visor"]),
    "t2_commando": dict(cloth="#2E3A30", cmat="fabric", plate=None, accent="#B02020", flags=["bandana", "vest"]),
    "t2_riot": dict(cloth="#5B4330", cmat="leather", plate=None, accent="#C8A040", visor="#203040", flags=["coat", "visor"]),
    "t3_combat": dict(cloth="#2B2B35", cmat="fabric", plate=None, accent="#A07010", glow="#FFC040", heavy=True, visor="#101820", flags=["visor"]),
    "t3_hev": dict(cloth="#2A2A30", cmat="fabric", plate="#C06A18", accent="#2A2A30", glow="#FFD040", heavy=True, visor="#101820", flags=["visor", "spine"]),
    "t3_exo": dict(cloth="#23262F", cmat="fabric", plate=None, accent="#DCA0F0", glow="#DCA0F0", flags=["frame", "spine", "visor"], visor="#101018"),
    "t3_power": dict(cloth="#262A38", cmat="fabric", plate=None, accent="#40E0FF", glow="#40E0FF", heavy=True, visor="#081820", flags=["visor", "spine"]),
    "steam": dict(cloth="#3A3430", cmat="leather", plate="#9A6A3A", accent="#FF8000", glow="#FF8000", heavy=True, visor="#302418", flags=["pipes", "visor"]),
}
PLATE_DEFAULT = {"t1_combat": "#808080", "t1_miner": "#C8B4B4", "t2_combat": "#80C8F0", "t2_commando": "#80C8F0",
                 "t2_riot": "#80C8F0", "t3_combat": "#DCA0F0", "t3_exo": "#6464A0", "t3_power": "#6464A0"}


def parse_armor(name):
    layer = None
    m = re.match(r"^(.*)_([12])$", name)
    core = name
    if m and not name.startswith("steam_"):
        core, layer = m.group(1), int(m.group(2))
    elif m and name == "t2_riot_1":
        core, layer = "t2_riot", 1
    best = max((k for k in SETS if core == k or core.startswith(k + "_")), key=len, default=None)
    var = core[len(best) + 1:] if best and core != best else ""
    return best, var, layer


def armor_cfg(set_key, var, man):
    c = dict(SETS[set_key]); c.setdefault("heavy", False)
    pal = man["palette"]
    c["plate"] = c.get("plate") or PLATE_DEFAULT.get(set_key, pal[0])
    c["plate"] = tone(c["plate"], 0.85)
    c["camo"] = None; c["hazard_var"] = False; c["wear"] = 0.15
    if var in CAMO_NAMES:
        c["camo"] = CAMO[CAMO_NAMES[var]]
    elif var in SOLID:
        col = SOLID[var]
        if var == "hazard":
            c["plate"] = col; c["hazard_var"] = True
        elif var in ("swat", "security"):
            c["cloth"] = col; c["plate"] = tone(mix(col, "#808890", .4), 1.0)
            c["accent"] = "#E0E0E0" if var == "swat" else "#D0B030"
        elif var in ("silver", "iron", "rusty", "bronze", "dark", "dark2", "1") or c["heavy"]:
            c["plate"] = col if var != "dark" else tone(c["plate"], .45)
            if var == "rusty":
                c["wear"] = 0.9
            if var == "dark2":
                c["plate"] = "#3A2C2C"
        else:
            c["cloth"] = col; c["plate"] = tone(c["plate"], .55 if var == "black" else 1)
        if var in ("black", "green", "red", "dark_green") and c["heavy"]:
            c["cloth"] = tone(col, .6)
    return c


def band_fill(t, faces, bands, r, s, skip=()):
    """bands: [(y0u, y1u, spec)] ; spec = (mat, base, pal, panel_cell|None, wear)."""
    for fn, (x, y, fw, fh) in faces.items():
        if fw <= 0 or fh <= 0:
            continue
        if fn in ("top", "bottom"):
            specs = [(0, fh, bands[0][2] if fn == "top" else bands[-1][2])]
            ys = [(0, fh)]
        else:
            specs = None
        if fn in ("top", "bottom"):
            sp = bands[0][2] if fn == "top" else bands[-1][2]
            _paint(t, x, y, fw, fh, sp, r)
        else:
            for y0, y1, sp in bands:
                a, b = int(y0 * s), min(fh, int(y1 * s))
                if b > a:
                    _paint(t, x, y + a, fw, b - a, sp, r)


def _paint(t, x, y, w, h, sp, r):
    mat, base, pal, cell, wr = sp
    rgb = material(mat, r, h, w, base, pal)
    if cell:
        panelize(rgb, r, cell[0], cell[1], cell[2] if len(cell) > 2 else .6, cell[3] if len(cell) > 3 else True)
    if wr:
        wear(rgb, r, wr)
    mul(rgb, 0, h - 1, w, 1, .85) if h > 2 else None
    t.paste(x, y, rgb)


def armor_layout_parts(s):
    return {"head": (0, 0, 8, 8, 8), "body": (16, 16, 8, 12, 4), "arm": (40, 16, 4, 12, 4), "leg": (0, 16, 4, 12, 4)}


def paint_armor(t, r, cfg, s, layer, aid, var_name=""):
    cloth = ("camo", "#808080", cfg["camo"], (4 * s, 9 * s, .75, False), .1) if cfg["camo"] else \
        (cfg["cmat"], cfg["cloth"], None, (5 * s, 12 * s, .78, False), .12)
    plate = ("steel", cfg["plate"], None, (3 * s + 2, 7 * s, .55, True), cfg["wear"])
    dark = ("rubber", "#202226", None, None, .1)
    leather = ("leather", "#3A2C20", None, None, .1)
    heavy = cfg["heavy"]
    body_main = plate if heavy else cloth
    P = armor_layout_parts(s)
    flags = cfg["flags"]
    for kind, (u, v, w, h, d) in P.items():
        faces = box_uv(u, v, w, h, d, s)
        if kind == "head":
            bands = [(0, 8, plate if (heavy or "lamp" in flags or "visor" in flags) else cloth)]
            if "bandana" in flags or "goggles" in flags:
                bands = [(0, 4, cloth), (4, 8, cloth)]
        elif kind == "body":
            bands = [(0, 9, body_main), (9, 11, dark), (11, 12, body_main)]
            if "coat" in flags:
                bands = [(0, 12, cloth)]
        elif kind == "arm":
            bands = [(0, 4, plate if (heavy or "frame" in flags or "vest" in flags or "pipes" in flags) else cloth),
                     (4, 9, body_main), (9, 12, leather if not heavy else dark)]
            if "coat" in flags:
                bands = [(0, 4, plate), (4, 12, cloth)]
        else:
            bands = [(0, 4, body_main), (4, 8, plate if not heavy else plate), (8, 9, body_main), (9, 12, dark)]
            if "coat" in flags:
                bands = [(0, 9, cloth), (9, 12, dark)]
        band_fill(t, faces, bands, r, s)
    # details in unit coordinates
    A, G = cfg["accent"], cfg.get("glow")
    for kind, (u, v, w, h, d) in P.items():
        faces = box_uv(u, v, w, h, d, s)
        fx, fy, fw, fh = faces["front"]
        view = t.rgb
        if kind == "head":
            if "visor" in flags:
                rect(view, fx + s, fy + 3 * s, 6 * s, 2 * s, cfg.get("visor", "#101820"))
                rect(view, fx + s, fy + 3 * s, 6 * s, s, mix(cfg.get("visor", "#101820"), "#80C0E0", .35))
                if G:
                    rect(view, fx + 2 * s, fy + 4 * s, 4 * s, max(1, s // 2), G, .9)
            if "lamp" in flags:
                rect(view, fx + 3 * s, fy, 2 * s, 2 * s, "#FFE070"); rect(view, fx + 3 * s - 1, fy - 0 + 2 * s, 2 * s + 2, 1, "#403010")
            if "bandana" in flags:
                rect(view, fx, fy + 3 * s, 8 * s, 2 * s, A); mul(view, fx, fy + 3 * s, 8 * s, 1, .75)
            if "goggles" in flags:
                rect(view, fx, fy + 3 * s, 8 * s, 2 * s, "#2A1E14")
                for gx in (1, 5):
                    rect(view, fx + gx * s, fy + 3 * s, 2 * s, 2 * s, "#7FD0E8"); rect(view, fx + gx * s, fy + 3 * s, 2 * s, max(1, s // 2), "#E8FAFF")
            for fname in ("left", "right"):
                lx, ly, lw, lh = faces[fname]
                rect(view, lx + 2 * s, ly + 3 * s, 3 * s, 2 * s, tone(cfg["plate"], .7))
            tx, ty, tw, th = faces["top"]
            for i in range(1, tw // s * 1 - 1, 2):
                rect(view, tx + i * s, ty + s, max(1, s // 2), th - 2 * s, "#101012", .6)
        elif kind == "body" and layer != 2:
            if "vest" in flags:
                rect(view, fx + s, fy + s, 6 * s, 6 * s, cfg["plate"]); mul(view, fx + s, fy + s, 6 * s, 1, 1.2)
                for px in (1, 4):
                    rect(view, fx + px * s, fy + 7 * s, 3 * s - max(1, s // 2), 2 * s, tone(cfg["cloth"], .6)); mul(view, fx + px * s, fy + 7 * s, 3 * s, 1, 1.3)
            if "coat" in flags:
                rect(view, fx + 3 * s, fy, 2 * s, 12 * s, "#2A1E14")
                for by in (2, 5, 8):
                    rect(view, fx + 3 * s, fy + by * s, 2 * s, max(1, s), A)
                rect(view, fx, fy + s, 3 * s, 3 * s, cfg["plate"]); rect(view, fx + 5 * s, fy + s, 3 * s, 3 * s, cfg["plate"])
            if "frame" in flags:
                for px in (1, 6):
                    rect(view, fx + px * s, fy, s, 9 * s, cfg["plate"])
                rect(view, fx + 2 * s, fy + 2 * s, 4 * s, s, cfg["plate"]); rect(view, fx + 2 * s, fy + 5 * s, 4 * s, s, cfg["plate"])
            if "pipes" in flags:
                for px in (1, 6):
                    _pipe(view, fx + px * s, fy, s, 9 * s, cfg["plate"])
                for ry in (3, 7):
                    rect(view, fx + s, fy + ry * s, 6 * s, max(1, s // 2), tone(cfg["plate"], 1.3))
            if "lamp" in flags or "hazard" in flags:
                rect(view, fx, fy + 9 * s, 8 * s, 2 * s, "#1A1A1C")
                for hx_ in range(0, 8 * s, 2 * s):
                    rect(view, fx + hx_, fy + 9 * s, s, 2 * s, "#E0B020")
            if G and heavy or (G and "frame" in flags):
                rect(view, fx + 3 * s, fy + 3 * s, 2 * s, 2 * s, G); rect(view, fx + 3 * s - 1, fy + 3 * s - 1, 2 * s + 2, 2 * s + 2, G, .25)
            bx, by_, bw, bh = faces["back"]
            if "spine" in flags and G:
                rect(view, bx + 3 * s, by_ + s, 2 * s, 9 * s, G, .35); rect(view, bx + 3 * s + s // 2, by_ + s, max(1, s), 9 * s, mix(G, "#FFFFFF", .5))
            if "frame" in flags or heavy:
                rect(view, bx + s, by_ + s, 6 * s, 2 * s, tone(cfg["plate"], .8)); mul(view, bx + s, by_ + s, 6 * s, 1, 1.2)
            if "vest" in flags:
                rect(view, bx + s, by_ + s, 6 * s, 5 * s, cfg["plate"], .9)
            # belt buckle
            rect(view, fx + 3 * s, fy + 9 * s, 2 * s, 2 * s, A if "hazard" not in flags else "#B0B0B0")
        elif kind == "arm":
            for fname in ("front", "back", "left", "right"):
                px, py, pw, ph = faces[fname]
                rect(view, px, py + 3 * s, pw, max(1, s // 2), A, .9) if "vest" in flags or heavy else None
                if G and heavy and fname == "front":
                    rect(view, px + pw // 2 - s // 2, py + 5 * s, max(1, s), 2 * s, G, .9)
                if "pipes" in flags and fname in ("front", "back"):
                    _pipe(view, px + pw // 2 - s // 2, py + 4 * s, max(1, s), 5 * s, cfg["plate"])
        elif kind == "leg":
            for fname in ("front", "back", "left", "right"):
                px, py, pw, ph = faces[fname]
                if fname == "front":
                    rect(view, px + s // 2, py + 4 * s, pw - s, 3 * s, tone(cfg["plate"], 1.05)); mul(view, px + s // 2, py + 4 * s, pw - s, 1, 1.25)
                    if G and heavy:
                        rect(view, px + pw // 2 - s // 2, py + 5 * s, max(1, s), s, G)
                    if "pipes" in flags:
                        _pipe(view, px + pw // 2 - s // 2, py, max(1, s), 4 * s, cfg["plate"])
                rect(view, px, py + 9 * s, pw, max(1, s // 2), "#4A4A50", .8)
    if cfg["hazard_var"]:
        for kind, (u, v, w, h, d) in P.items():
            for fname, (x, y, fw, fh) in box_uv(u, v, w, h, d, s).items():
                if fname in ("top", "bottom") or kind == "head":
                    continue
                view = t.rgb[y:y + fh, x:x + fw]
                DET["hazard"](view, r, fname, {})
    variant_overlay(t, r, s, var_name, cfg, P)
    return t


GLYPH = {"S": ["###", "#..", "###", "..#", "###"], "W": ["#.#", "#.#", "#.#", "###", "#.#"], "A": [".#.", "#.#", "###", "#.#", "#.#"],
         "T": ["###", ".#.", ".#.", ".#.", ".#."], "E": ["###", "#..", "##.", "#..", "###"], "C": ["###", "#..", "#..", "#..", "###"],
         "U": ["#.#", "#.#", "#.#", "#.#", "###"]}


def glyph_text(V, x, y, letters, col, sc):
    for k, chh in enumerate(letters):
        gx, gy = x + (k % 2) * 4 * sc, y + (k // 2) * 6 * sc
        for j, row in enumerate(GLYPH[chh]):
            for i, c in enumerate(row):
                if c == "#":
                    rect(V, gx + i * sc, gy + j * sc, sc, sc, col)


def variant_overlay(t, r, s, var, cfg, P):
    """Distinct patterns per color variant (not just a recolor)."""
    if not var:
        return
    for kind, (u, v, w, h, d) in P.items():
        if kind == "head":
            continue
        for fn, (x, y, fw, fh) in box_uv(u, v, w, h, d, s).items():
            if fw < 2 or fh < 2:
                continue
            V = t.rgb[y:y + fh, x:x + fw]
            yy, xx = np.mgrid[0:fh, 0:fw]
            side = fn in ("front", "back", "left", "right")
            if var == "black":
                m = ((xx + yy) // max(1, s)) % 7 == 0
                V[m] = np.clip(V[m] * 1.5 + 8, 0, 255)
                if side:
                    rect(V, 0, fh // 3, fw, max(1, s // 2) + 1, "#D04010", .9)
            elif var in ("green", "dark_green", "lightgreen"):
                pat = ("digital", ["#3A5A2A", "#5E8A3E", "#2A4020", "#7AA050"]) if var == "green" else \
                      ("tiger", ["#1E3A24", "#2F5A38", "#10241A", "#3E6A44"])
                V[...] = V * .35 + material("camo", r, fh, fw, "#808080", pat) * .65
            elif var == "red":
                if side:
                    cx = fw // 2
                    rect(V, cx - s, 0, s, fh, "#EDEDED", .95); rect(V, cx + s, 0, max(1, s // 2), fh, "#EDEDED", .95)
                    rect(V, 0, fh - 3 * s, fw, max(1, s // 2) + 1, "#1A1A1A", .9)
            elif var == "silver":
                streak = vnoise(r, fh, fw, max(3, fw // 2), 1)
                V[...] = np.clip(V * (0.9 + 0.25 * streak[..., None]), 0, 255)
                if side:
                    rect(V, 0, 2 * s, fw, max(1, s // 2), "#3A6AD0", .95)
            elif var in ("dark", "dark2"):
                V[...] = V * .55 + material("carbon", r, fh, fw, "#202024") * .6
            elif var == "iron":
                m = (xx % (4 * s) == s) & (yy % (4 * s) == s)
                V[m] = (30, 30, 34)
                V[(xx % (8 * s) == 0)] *= .75
            elif var == "rusty":
                n = vnoise(r, fh, fw, max(2, 2 * s)) * .6 + vnoise(r, fh, fw, 1) * .4
                m = n > .52
                V[m] = np.array(hx("#8A4A20")) * (0.7 + 0.5 * n[m])[:, None]
                st = (vnoise(r, fh, fw, 1, 9) > .72)
                V[st] = V[st] * .55 + np.array(hx("#6A2E10")) * .45
            elif var == "1":
                n = vnoise(r, fh, fw, max(2, 2 * s))
                m = n > .6
                V[m] = np.array(hx("#9A6A3A")) * (0.8 + 0.4 * n[m])[:, None]
            elif var == "bronze":
                if side:
                    for ly in range(2 * s, fh, 6 * s):
                        rect(V, 0, ly, fw, max(1, s // 2), "#E8C060", .8)
            elif var == "swat":
                if side:
                    rect(V, 0, 4 * s, fw, 2 * s, "#E8E8E8", .9) if kind in ("arm", "leg") else None
                if kind == "body" and fn == "back":
                    glyph_text(V, 0, 2 * s, "SWAT", "#E8E8E8", s)
                if kind == "body" and fn == "front":
                    rect(V, 0, 0, fw, s, "#E8E8E8", .9)
            elif var == "security":
                if side:
                    for k in range(0, fw, 4 * s):
                        rect(V, k, 5 * s, 2 * s, 2 * s, "#E0C020", .95)
                    rect(V, 0, 5 * s, fw, 0, "#E0C020")
                if kind == "body" and fn == "back":
                    glyph_text(V, 0, 2 * s, "SECU", "#E0C020", s)
                if kind == "body" and fn == "front":
                    rect(V, 0, 0, fw, s, "#E0C020", .9)


def _pipe(view, x, y, w, h, col):
    c = hx(col)
    for i in range(w):
        f = 1.35 - 0.8 * (i / max(1, w - 1)) if w > 1 else 1.0
        rect(view, x + i, y, 1, h, np.clip(c * f * 1.05, 0, 255))
    for ry in range(2, h - 1, max(4, h // 3)):
        rect(view, x - 1, y + ry, w + 2, 1, np.clip(c * .55, 0, 255))


def make_armor(man):
    name = man["id"].replace("model.armor.", "")
    W, H = man["width"], man["height"]
    if name in ACCESSORIES or any(name == a or name.startswith(a + "_") for a in ACCESSORIES):
        return make_accessory(man)
    set_key, var, layer = parse_armor(name)
    cfg = armor_cfg(set_key, var, man)
    s = max(1, W // 64)
    r = rng_for("model.armor." + (name if var else name))
    t = Tex(W, H)
    paint_armor(t, r, cfg, s, layer, name, var)
    lines = ["standard 64x32 armor unwrap scaled x%d (head 0,0 8x8x8; body 16,16 8x12x4; arm 40,16 4x12x4 (mirrored for left); leg 0,16 4x12x4 (mirrored for left))." % s]
    t.a[:32 * s, :64 * s] = 255
    if H > 32 * s:
        ex = [("pauldron", "plate", 8, 4, 8, ["rivets"] if False else ["label"]), ("knee_guard", "plate", 5, 5, 5, ["rings"]),
              ("back_plate", "plate", 10, 12, 2, ["vents"]), ("belt_pouch", "cloth", 4, 4, 3, ["label"]),
              ("shin", "plate", 5, 8, 5, []), ("collar", "plate", 9, 3, 9, []), ("holster", "cloth", 4, 6, 3, [])]
        if cfg["camo"]:
            cm = ("camo", "#808080", cfg["camo"])
        else:
            cm = (cfg["cmat"], cfg["cloth"], None)
        mm = {"plate": ("steel", cfg["plate"], None), "cloth": cm}
        paint_atlas("model.armor." + name + "#extra", W, H, ex, mm, {"accent": cfg["accent"], "glow": cfg.get("glow", "#FF8000")},
                    r_key="model.armor." + name + "#extra", region=(0, 32 * s, W, H - 32 * s), tex=t)
        lines += ["extra boxes below the vanilla layout (free for pauldrons, knee guards, back plate etc.):"] + LAYOUT_DOC.pop("model.armor." + name + "#extra")
    LAYOUT_DOC["model.armor." + name] = lines
    return t


# ---- armor accessories (packed atlases, 64x64) --------------------------------
ACCESSORIES = {
    "gas_mask": ([("faceplate", "rubber", 8, 8, 2, ["rings"]), ("filter", "metal", 3, 3, 3, ["grill", "rings"]), ("lens_l", "glass", 3, 2, 1, ["lens"]),
                  ("lens_r", "glass", 3, 2, 1, ["lens"]), ("strap", "rubber", 9, 2, 9, []), ("hose", "rubber", 1, 5, 1, ["rings"]),
                  ("valve", "metal", 2, 2, 2, [])], "#2A2A2E"),
    "night_vision_goggles": ([("housing", "dark", 8, 3, 3, ["label"]), ("tube_l", "metal", 2, 2, 3, ["rings"]), ("tube_r", "metal", 2, 2, 3, ["rings"]),
                              ("lens_l", "glow", 2, 2, 1, ["lens"]), ("lens_r", "glow", 2, 2, 1, ["lens"]), ("strap", "rubber", 9, 2, 9, []),
                              ("mount", "metal", 3, 2, 2, ["rails"])], "#1E1E22"),
    "oxygen_tanks": ([("tank_l", "main", 3, 10, 3, ["rings"]), ("tank_r", "main", 3, 10, 3, ["rings"]), ("valve_block", "metal", 5, 2, 3, ["label"]),
                      ("harness", "rubber", 8, 10, 2, ["stripe"]), ("hose", "rubber", 1, 8, 1, ["rings"]), ("gauge", "glass", 2, 2, 1, ["lens"]),
                      ("cap_l", "metal", 2, 1, 2, []), ("cap_r", "metal", 2, 1, 2, [])], "#B8BCC8"),
    "oxygen_mask": ([("mask", "main", 8, 6, 2, ["rings"]), ("visor", "glass", 6, 3, 1, ["lens"]), ("regulator", "metal", 3, 3, 2, ["grill", "rings"]),
                     ("strap", "rubber", 9, 2, 9, []), ("hose", "rubber", 1, 6, 1, ["rings"]), ("clip_l", "metal", 1, 2, 1, []), ("clip_r", "metal", 1, 2, 1, [])], "#C8C8D4"),
    "tactical_mask": ([("faceplate", "main", 8, 8, 1, ["vents", "stripe"]), ("jaw", "dark", 8, 3, 2, ["grill"]), ("brow", "dark", 8, 2, 2, []),
                       ("strap", "rubber", 9, 2, 9, []), ("eye_l", "glow", 2, 1, 1, ["strip"]), ("eye_r", "glow", 2, 1, 1, ["strip"])], "#C8C8C8"),
    "jump_pack": ([("back_plate", "main", 10, 12, 3, ["label", "vents"]), ("thruster_l", "dark", 3, 8, 3, ["rings", "hazard"]), ("thruster_r", "dark", 3, 8, 3, ["rings", "hazard"]),
                   ("nozzle_l", "metal", 3, 2, 3, ["grill"]), ("nozzle_r", "metal", 3, 2, 3, ["grill"]), ("strap", "rubber", 8, 2, 2, []),
                   ("fuel_cell", "glow", 4, 3, 2, ["cells"])], "#B8B8CC"),
    "glider": ([("wing_l", "cloth", 22, 1, 12, ["stripe"]), ("wing_r", "cloth", 22, 1, 12, ["stripe"]), ("spar_l", "metal", 24, 1, 1, []),
                ("spar_r", "metal", 24, 1, 1, []), ("harness", "leather", 10, 10, 2, ["stripe"]), ("buckle", "metal", 3, 2, 1, [])], "#643200"),
    "anti_gravity_pack": ([("back_plate", "main", 10, 12, 3, ["label", "vents"]), ("ring_l", "dark", 4, 4, 4, ["coil"]), ("ring_r", "dark", 4, 4, 4, ["coil"]),
                           ("core", "glow", 4, 5, 3, ["strip"]), ("emitter_l", "glow", 3, 1, 3, ["cells"]), ("emitter_r", "glow", 3, 1, 3, ["cells"]),
                           ("strap", "rubber", 8, 2, 2, []), ("battery", "dark", 5, 4, 2, ["cells"])], "#6464A0"),
    "t2_beret": ([("crown", "cloth", 9, 2, 9, []), ("band", "leather", 9, 1, 9, []), ("badge", "metal", 3, 3, 1, ["stripe"]), ("nub", "cloth", 1, 1, 1, [])], "#80C8F0"),
}


def make_accessory(man):
    name = man["id"].replace("model.armor.", "")
    key = max((k for k in ACCESSORIES if name == k or name.startswith(k + "_")), key=len)
    var = name[len(key) + 1:]
    parts, basecol = ACCESSORIES[key]
    main = SOLID.get(var, basecol)
    if key == "t2_beret":
        main = SOLID.get(var, "#4A5A3A") if var else "#6B3A2A"
    if var in ("black", "bronze", "dark_green", "silver", "green", "white", "lightgreen"):
        main = SOLID[var]
    pal0 = man["palette"][0]
    glowc = "#40E0FF" if key in ("anti_gravity_pack",) else ("#7CFF7C" if key == "night_vision_goggles" else "#FF9030")
    mm = {"main": ("steel" if key not in ("tactical_mask",) else "polymer", tone(main, .85), None),
          "metal": ("steel", "#8A8E96", None), "dark": ("steel", "#2A2C32", None), "rubber": ("rubber", "#26262A", None),
          "glass": ("glass", "#3A8AB8" if key != "night_vision_goggles" else "#2A5A2A", None),
          "glow": ("plain", glowc, None), "cloth": ("fabric" if key != "glider" else "fabric", tone(main, 1.0), None),
          "leather": ("leather", "#4A3322", None)}
    VPAT = {"black": ("carbon", "#26262A", None), "green": ("camo", "#808080", ("digital", ["#3A5A2A", "#5E8A3E", "#2A4020", "#7AA050"])),
            "dark_green": ("camo", "#808080", ("tiger", ["#1E3A24", "#2F5A38", "#10241A", "#3E6A44"])),
            "lightgreen": ("camo", "#808080", ("splinter", ["#8FBF6A", "#6A9A4A", "#B8DC90", "#4A7A32"])),
            "white": ("camo", "#808080", ("flecks", ["#F0F0F0", "#D8DCE0", "#A8B0B8", "#FFFFFF"])),
            "silver": ("steel", "#C8CCD4", None), "bronze": ("camo", "#808080", ("bark", ["#B8733A", "#8A5226", "#D8944A", "#6A3E1C"]))}
    if var in VPAT:
        mm["main"] = VPAT[var]
        if key == "t2_beret":
            mm["cloth"] = VPAT[var] if var != "black" else ("fabric", "#1E1E22", None)
    ctx = {"glow": glowc, "accent": "#C8A040", "lens": "#4AA8D8" if key != "night_vision_goggles" else "#40FF60"}
    return paint_atlas("model.armor." + name, 64, 64, parts, mm, ctx, r_key="model.armor." + key, cell=(4, 9), wear_amt=.25)


# ---- biped mob skins ----------------------------------------------------------
def biped(man, cfg):
    mid = man["id"]
    r = rng_for(mid)
    W = man["width"]; s = W // 64
    t = Tex(W, W)
    P = {"head": (0, 0, 8, 8, 8), "body": (16, 16, 8, 12, 4), "rarm": (40, 16, 4, 12, 4), "rleg": (0, 16, 4, 12, 4),
         "larm": (32, 48, 4, 12, 4), "lleg": (16, 48, 4, 12, 4)}
    skin = ("skin", cfg["skin"], None, None, .08)
    cloth = ("camo", "#808080", cfg["camo"], (4 * s, 9 * s, .8, False), .15) if cfg.get("camo") else \
        ("fabric", cfg["shirt"], None, (5 * s, 12 * s, .8, False), .15)
    pants = ("fabric", cfg["pants"], None, (5 * s, 12 * s, .8, False), .15) if not cfg.get("camo") else cloth
    boots = ("leather", cfg["boots"], None, None, .2)
    hair = ("skin", cfg["hair"], None, None, .1)
    plate = ("steel", cfg.get("plate", "#606468"), None, (3 * s + 2, 7 * s, .55, True), .35)
    sl = cfg.get("sleeve", 9)
    for k, (u, v, w, h, d) in P.items():
        faces = box_uv(u, v, w, h, d, s)
        if k == "head":
            band_fill(t, faces, [(0, 8, skin)], r, s)
        elif k == "body":
            band_fill(t, faces, [(0, 12, cloth)], r, s)
        elif k.endswith("arm"):
            band_fill(t, faces, [(0, sl, cloth), (sl, 12, ("skin", cfg.get("glove") or cfg["skin"], None, None, .1))], r, s)
        else:
            band_fill(t, faces, [(0, 9, pants), (9, 12, boots)], r, s)
    V = t.rgb
    hf = box_uv(*P["head"], s)
    fx, fy, fw, fh = hf["front"]
    # hair: top, upper face rows, back, sides
    if cfg["hair"] and not cfg.get("bald"):
        for fn in ("top",):
            x, y, w_, h_ = hf[fn]; _paint(t, x, y, w_, h_, hair, r)
        _paint(t, fx, fy, fw, 2 * s, hair, r)
        for fn in ("left", "right"):
            x, y, w_, h_ = hf[fn]; _paint(t, x, y, w_, 3 * s, hair, r)
        x, y, w_, h_ = hf["back"]; _paint(t, x, y, w_, 5 * s, hair, r)
    # face
    eye = cfg.get("eye")
    for ex in (1, 5):
        rect(V, fx + ex * s, fy + 4 * s, 2 * s, s, "#F0F0F0" if not eye else eye)
        rect(V, fx + (ex + (1 if ex == 1 else 0)) * s, fy + 4 * s, s, s, cfg.get("iris", "#2A4A7A") if not eye else "#FFFFFF")
        rect(V, fx + ex * s, fy + 3 * s, 2 * s, max(1, s // 2) + (s // 2), tone(cfg["hair"] or "#201810", .9), .85)
    if eye:
        for ex in (1, 5):
            rect(V, fx + ex * s - 1, fy + 4 * s - 1, 2 * s + 2, s + 2, eye, .3)
    rect(V, fx + 3 * s, fy + 5 * s, 2 * s, s, tone(cfg["skin"], .8), .8)
    rect(V, fx + 2 * s, fy + 6 * s + s // 2, 4 * s, max(1, s // 2) + (s // 2), cfg.get("mouth", tone(cfg["skin"], .55)))
    if cfg.get("mustache"):
        rect(V, fx + 2 * s, fy + 5 * s + s // 2, 4 * s, max(1, s // 2), cfg["hair"])
    if cfg.get("teeth"):
        for i in range(3):
            rect(V, fx + (2 + i) * s + s // 2, fy + 6 * s + s // 2, max(1, s // 2), max(1, s // 2), "#E0DCC0")
    # head gear
    gear = cfg.get("helmet")
    if gear:
        x, y, w_, h_ = hf["top"]; _paint(t, x, y, w_, h_, ("steel", gear, None, (3 * s, 8 * s, .6, True), .3), r)
        _paint(t, fx, fy, fw, 3 * s, ("steel", gear, None, (3 * s, 8 * s, .6, True), .3), r)
        for fn in ("left", "right"):
            x, y, w_, h_ = hf[fn]; _paint(t, x, y, w_, 4 * s, ("steel", gear, None, (3 * s, 8 * s, .6, True), .3), r)
        x, y, w_, h_ = hf["back"]; _paint(t, x, y, w_, 5 * s, ("steel", gear, None, (3 * s, 8 * s, .6, True), .3), r)
        rect(V, fx, fy + 3 * s - 1, fw, 1, tone(gear, .5))
    if cfg.get("cap"):
        x, y, w_, h_ = hf["top"]; _paint(t, x, y, w_, h_, ("fabric", cfg["cap"], None, None, .1), r)
        _paint(t, fx, fy, fw, 2 * s, ("fabric", cfg["cap"], None, None, .1), r)
        rect(V, fx, fy + 2 * s, fw, s, tone(cfg["cap"], .6)); rect(V, fx + 3 * s, fy + s - s // 2, 2 * s, s, "#D8B848")
        for fn in ("left", "right", "back"):
            x, y, w_, h_ = hf[fn]; _paint(t, x, y, w_, 3 * s, ("fabric", cfg["cap"], None, None, .1), r)
    if cfg.get("mask"):
        rect(V, fx, fy + 5 * s, fw, 3 * s, cfg["mask"]); mul(V, fx, fy + 5 * s, fw, 1, .7)
        for fn in ("left", "right"):
            x, y, w_, h_ = hf[fn]; rect(V, x, y + 5 * s, w_, 3 * s, cfg["mask"])
    if cfg.get("bandana"):
        rect(V, fx, fy + 2 * s, fw, 2 * s - s // 2, cfg["bandana"])
        for fn in ("left", "right", "back"):
            x, y, w_, h_ = hf[fn]; rect(V, x, y + 2 * s, w_, 2 * s - s // 2, cfg["bandana"])
    if cfg.get("horns"):
        x, y, w_, h_ = hf["top"]
        rect(V, x, y, s, h_, cfg["horns"]); rect(V, x + w_ - s, y, s, h_, cfg["horns"])
        for fn in ("left", "right"):
            xx, yy, ww, hh = hf[fn]; rect(V, xx + 3 * s, yy, 2 * s, s, cfg["horns"])
    # ---- recognisable markings: face paint, eyewear, tusks, jaw plates, faction symbols ----
    skin_c = cfg["skin"]
    if cfg.get("mohawk"):
        x, y, w_, h_ = hf["top"]
        _paint(t, x, y, w_, h_, skin, r); rect(V, x + 3 * s, y, 2 * s, h_, cfg["mohawk"])
        for fn in ("left", "right"):
            xx, yy, ww, hh = hf[fn]; _paint(t, xx, yy, ww, 4 * s, skin, r)
        _paint(t, fx, fy, fw, 2 * s, skin, r); rect(V, fx + 3 * s, fy, 2 * s, 2 * s, cfg["mohawk"])
        xx, yy, ww, hh = hf["back"]; _paint(t, xx, yy, ww, 5 * s, skin, r); rect(V, xx + 3 * s, yy, 2 * s, 5 * s, cfg["mohawk"])
    if cfg.get("face_paint"):
        for ex in (1, 5):
            rect(V, fx + ex * s, fy + 5 * s, 2 * s, max(1, s // 2) + s // 2, cfg["face_paint"], .9)
        rect(V, fx, fy + 3 * s, fw, max(1, s // 2), cfg["face_paint"], .7)
        rect(V, fx + 3 * s, fy + 5 * s, 2 * s, s, cfg["face_paint"], .6)
    if cfg.get("one_eye"):  # zombie: milky blind left eye, glowing right
        rect(V, fx + 5 * s, fy + 4 * s, 2 * s, s, "#C8D0C0"); rect(V, fx + 5 * s, fy + 3 * s, 2 * s, max(1, s // 2), "#4A5A3A")
    if cfg.get("sunglasses"):
        for ex in (1, 5):
            rect(V, fx + ex * s, fy + 4 * s, 2 * s, s + s // 2, "#0C0C10"); rect(V, fx + ex * s, fy + 4 * s, max(1, s // 2) + 1, 1, "#7A8AA0")
        rect(V, fx + 3 * s, fy + 4 * s, 2 * s, max(1, s // 2), "#0C0C10"); rect(V, fx, fy + 4 * s, s, max(1, s // 2), "#0C0C10"); rect(V, fx + 7 * s, fy + 4 * s, s, max(1, s // 2), "#0C0C10")
    if cfg.get("eyepatch"):
        rect(V, fx + 5 * s, fy + 3 * s, 3 * s, 3 * s - s // 2, "#0E0E10"); rect(V, fx, fy + 3 * s, fw, max(1, s // 2), "#0E0E10")
    if cfg.get("tusks"):
        for tx in (2, 5):
            rect(V, fx + tx * s, fy + 6 * s - s // 2, max(1, s // 2) + s // 2, 2 * s, "#E8E0C0"); rect(V, fx + tx * s, fy + 6 * s - s // 2, 1, 2 * s, "#FFFFFF")
    if cfg.get("brow"):
        rect(V, fx + s // 2, fy + 3 * s - s // 2, fw - s, s, tone(skin_c, .55)); rect(V, fx + s // 2, fy + 4 * s - s // 2, 2 * s + s, max(1, s // 2), tone(skin_c, .7), .8)
    if cfg.get("jaw_plate"):
        rect(V, fx + s // 2, fy + 6 * s, fw - s, 2 * s, cfg["jaw_plate"])
        for i in range(1, 8):
            rect(V, fx + i * s - 1, fy + 6 * s + s // 2, 1, 2 * s - s // 2, "#101012")
        if cfg.get("jaw_glow"):
            rect(V, fx + s, fy + 7 * s - s // 2, fw - 2 * s, max(1, s // 2), cfg["jaw_glow"], .85)
        for fn in ("left", "right"):
            xx, yy, ww, hh = hf[fn]; rect(V, xx + 3 * s, yy + 5 * s, 4 * s, 3 * s, cfg["jaw_plate"])
    if cfg.get("headband"):
        rect(V, fx, fy + 2 * s, fw, max(1, s // 2) + s // 2, cfg["headband"])
        for fn in ("left", "right", "back"):
            xx, yy, ww, hh = hf[fn]; rect(V, xx, yy + 2 * s, ww, max(1, s // 2) + s // 2, cfg["headband"])
    if cfg.get("bandage"):
        rect(V, fx + s, fy + s, 5 * s, max(1, s // 2) + s // 2, "#CFC8B0"); rect(V, fx + 5 * s, fy + 5 * s, 2 * s, 2 * s, "#CFC8B0", .9)
        rect(V, fx + 5 * s + s // 2, fy + 5 * s, max(1, s // 2), 2 * s, "#7A2010", .8)
    if cfg.get("chinstrap"):
        for fn in ("left", "right"):
            xx, yy, ww, hh = hf[fn]; rect(V, xx + 2 * s, yy + 3 * s, max(1, s // 2) + s // 2, 5 * s, "#1E1E1A")
        rect(V, fx + s, fy + 7 * s, 6 * s, max(1, s // 2), "#1E1E1A")
    if cfg.get("net"):
        for fn in ("top", "left", "right", "back", "front"):
            xx, yy, ww, hh = hf[fn]; hh2 = hh if fn == "top" else (3 * s if fn == "front" else 4 * s)
            sub = V[yy:yy + hh2, xx:xx + ww]
            m = vnoise(r, hh2, ww, max(2, s * 2)) > .55
            sub[m] = np.array(hx(cfg["net"])) * (0.8 + 0.3 * vnoise(r, hh2, ww, 1)[m])[:, None]
    bf = box_uv(*P["body"], s); bx, by, bw, bh = bf["front"]
    SYM = {"star": ["#.#", ".#.", "#.#"], "cross": [".#.", "###", ".#."], "skull": ["###", "#.#", ".#."],
           "chevron": ["#.#", ".#.", "#.#"], "gear": ["###", "#.#", "###"], "bar": ["###", "...", "###"]}

    def symbol(x0, y0, kind, col, sc):
        for j, row in enumerate(SYM[kind]):
            for i, chh in enumerate(row):
                if chh == "#":
                    rect(V, x0 + i * sc, y0 + j * sc, sc, sc, col)
    if cfg.get("faction"):
        kind, col = cfg["faction"]
        for k in ("rarm", "larm"):
            for fn in ("left", "right"):
                xx, yy, ww, hh = box_uv(*P[k], s)[fn]
                rect(V, xx + s // 2, yy + s // 2, 3 * s, 3 * s, cfg.get("patch_bg", "#1A1A1C")); symbol(xx + s // 2, yy + s // 2, kind, col, s)
        xx, yy, ww, hh = bf["back"]
        symbol(xx + 2 * s, yy + 2 * s, kind, col, 2 * s)
    if cfg.get("bandolier"):
        for fn in ("front", "back"):
            xx, yy, ww, hh = bf[fn]
            for i in range(ww):
                yv = yy + int(i * (hh - 2 * s) / ww) if fn == "front" else yy + int((ww - 1 - i) * (hh - 2 * s) / ww)
                rect(V, xx + i, yv, 1, max(1, s) + s // 2, "#4A3320"); rect(V, xx + i, yv, 1, 1, "#6A4A2E")
                if i % max(2, s * 2) == 0:
                    rect(V, xx + i, yv + 1, 1, max(1, s), "#D8B040")
    if cfg.get("epaulettes"):
        for k in ("rarm", "larm"):
            for fn in ("front", "back", "left", "right", "top"):
                xx, yy, ww, hh = box_uv(*P[k], s)[fn]; hh2 = hh if fn == "top" else s * 2
                rect(V, xx, yy, ww, hh2, "#D8B040"); rect(V, xx, yy + hh2 - 1, ww, 1, "#8A6A18")
                if fn != "top":
                    for i in range(0, ww, 2):
                        rect(V, xx + i, yy + hh2, 1, max(1, s // 2) + 1, "#D8B040")
    if cfg.get("spikes"):
        for k in ("rarm", "larm"):
            for fn in ("left", "right"):
                xx, yy, ww, hh = box_uv(*P[k], s)[fn]
                for j in range(3):
                    rect(V, xx + ww // 2 - (2 - j) * max(1, s // 2), yy + j * s, (2 - j) * max(1, s) + 1, s, "#D8D0B0")
                rect(V, xx + ww // 2, yy, max(1, s // 2), s, "#6A6050")
    if cfg.get("cracks"):
        for _ in range(9):
            k = ("body", "rarm", "larm", "rleg", "lleg")[int(r.integers(0, 5))]
            xx, yy, ww, hh = box_uv(*P[k], s)[("front", "back")[int(r.integers(0, 2))]]
            cx, cy = int(r.integers(0, ww)), int(r.integers(0, max(1, hh // 2)))
            for _s in range(int(r.integers(4, 9)) * s):
                rect(V, xx + cx, yy + cy, 1, 1, cfg["cracks"]); cy = min(hh - 1, cy + 1)
                cx = max(0, min(ww - 1, cx + int(r.integers(-1, 2))))
    # body / limbs details
    if cfg.get("vest"):
        for fn in ("front", "back"):
            x, y, w_, h_ = bf[fn]
            _paint(t, x + s // 2, y + s, w_ - s, 7 * s, ("fabric", cfg["vest"], None, (4 * s, 8 * s, .7, True), .2), r)
        for px in (1, 4):
            rect(V, bx + px * s, by + 5 * s, 3 * s - s // 2, 3 * s, tone(cfg["vest"], .7)); mul(V, bx + px * s, by + 5 * s, 3 * s, 1, 1.3)
            rect(V, bx + px * s + s // 2, by + 5 * s + s, max(1, s), max(1, s), "#A09060")
    if cfg.get("chest_plate"):
        x, y, w_, h_ = bf["front"]; _paint(t, x, y, w_, 8 * s, ("steel", cfg["chest_plate"], None, (3 * s, 8 * s, .55, True), .4), r)
        x, y, w_, h_ = bf["back"]; _paint(t, x, y, w_, 8 * s, ("steel", cfg["chest_plate"], None, (3 * s, 8 * s, .55, True), .4), r)
    rect(V, bx, by + 9 * s, bw, s, cfg.get("belt", "#2A1E14")); rect(V, bx + 3 * s, by + 9 * s, 2 * s, s, "#C8B050")
    if cfg.get("medals"):
        for i, c in enumerate(("#E0C040", "#C03030", "#4060C0", "#E0C040")):
            rect(V, bx + (4 + (i % 2) * 2) * s - s, by + (1 + i // 2 * 2) * s, s, s, c)
        rect(V, bx, by, bw, s, "#E0C040", .8)
    if cfg.get("glow_chest"):
        rect(V, bx + 3 * s, by + 2 * s, 2 * s, 2 * s, cfg["glow_chest"]); rect(V, bx + 3 * s - 1, by + 2 * s - 1, 2 * s + 2, 2 * s + 2, cfg["glow_chest"], .25)
    if cfg.get("pads"):
        for k in ("rarm", "larm"):
            for fn in ("front", "back", "left", "right"):
                x, y, w_, h_ = box_uv(*P[k], s)[fn]; _paint(t, x, y, w_, 3 * s, ("steel", cfg["pads"], None, (3 * s, 8 * s, .55, True), .35), r)
        for k in ("rleg", "lleg"):
            x, y, w_, h_ = box_uv(*P[k], s)["front"]; _paint(t, x, y + 4 * s, w_, 3 * s, ("steel", cfg["pads"], None, (3 * s, 8 * s, .55, True), .35), r)
    # damage / decay / blood / scars
    dec = cfg.get("decay", 0)
    if dec:
        for k, (u, v, w, h, d) in P.items():
            for fn, (x, y, fw_, fh_) in box_uv(u, v, w, h, d, s).items():
                n = vnoise(r, fh_, fw_, max(2, s * 2))
                m = n > (1 - dec)
                V[y:y + fh_, x:x + fw_][m] = np.array(hx(cfg.get("rot", "#6A7A4A"))) * (0.8 + 0.3 * n[m])[:, None]
                m2 = (r.random((fh_, fw_)) < dec * .12)
                V[y:y + fh_, x:x + fw_][m2] = hx("#5A1212")
                # torn lower edge of cloth
                if k in ("body", "rarm", "larm") and fn in ("front", "back", "left", "right"):
                    for xx in range(fw_):
                        if r.random() < .35:
                            rect(V, x + xx, y + fh_ - 1 - int(r.integers(0, max(1, s))), 1, 1 + s, cfg["skin"], .8)
    if cfg.get("blood"):
        for _ in range(cfg["blood"]):
            k = list(P)[int(r.integers(0, 6))]
            u, v, w, h, d = P[k]
            fn = list(box_uv(u, v, w, h, d, s).items())[int(r.integers(2, 6))]
            x, y, fw_, fh_ = fn[1]
            rect(V, x + int(r.integers(0, fw_)), y + int(r.integers(0, fh_)), int(r.integers(1, 3 * s)), int(r.integers(1, 3 * s)), "#7A1010", .8)
    for k in ("body", "rarm", "larm"):
        if cfg.get("scars") and k == "body":
            for _ in range(3):
                x, y = bx + int(r.integers(0, bw - 3)), by + int(r.integers(0, 6 * s))
                rect(V, x, y, 3 * s, max(1, s // 2), tone(cfg["skin"], .6)); rect(V, x, y + 1, 3 * s, 1, tone(cfg["skin"], 1.2), .6)
    # mark used regions opaque
    for k, (u, v, w, h, d) in P.items():
        for fn, (x, y, fw_, fh_) in box_uv(u, v, w, h, d, s).items():
            t.a[y:y + fh_, x:x + fw_] = 255
    np.clip(V, 0, 255, out=V)
    LAYOUT_DOC[mid] = ["modern 64x64 skin unwrap scaled x%d: head 0,0 8x8x8; body 16,16 8x12x4; right arm 40,16; right leg 0,16; left arm 32,48; left leg 16,48 (each 4x12x4). Overlay (hat/jacket/sleeve) regions left transparent." % s]
    return t


SKINS = {
    "army_soldier": dict(skin="#C79A78", hair="#3A2A1C", shirt="#5C4D3E", pants="#4E4A38", boots="#241A12", helmet="#4A5238", vest="#3E4430", camo=CAMO[0], iris="#3A5A3A", sleeve=12),
    "zombie_soldier": dict(skin="#7E9A6A", hair="#2A2A20", shirt="#484E2B", pants="#3C4028", boots="#201C14", helmet="#3C4230", vest="#34381F", decay=.42, blood=14, eye="#E0E070", teeth=True, mouth="#3A1010", sleeve=7, rot="#667A4A"),
    "bandit": dict(skin="#C09070", hair="#2A1E14", shirt="#5B514B", pants="#3E3830", boots="#2A1E14", bandana="#7A2A22", vest="#3A2E24", belt="#1E1610", iris="#503010", sleeve=9, scars=True),
    "dictator": dict(skin="#D4A484", hair="#1E1812", shirt="#594F49", pants="#3E382F", boots="#18120E", cap="#4E5A3A", medals=True, mustache=True, iris="#403020", belt="#C8B050", glove="#E8E8E8", sleeve=12),
    "cyberdemon": dict(skin="#8A2418", hair=None, bald=True, shirt="#6A1A12", pants="#38120D", boots="#222226", eye="#FFD030", horns="#D8CFA8", chest_plate="#4A4C54", pads="#4A4C54", glow_chest="#FF7020", teeth=True, mouth="#2A0808", scars=True, sleeve=6, belt="#222226", rot="#5A1810"),
    "supermutant_1": dict(skin="#987A22", hair="#2A2210", shirt="#5A4A30", pants="#46402C", boots="#2A2218", bald=True, hair_="", scars=True, teeth=True, mouth="#3A1A10", sleeve=5, eye="#F0F0A0", pads="#6A6258"),
    "supermutant_2": dict(skin="#907525", hair="#3A2A14", shirt="#4A3C28", pants="#3A3A30", boots="#2A2218", scars=True, mouth="#3A1A10", sleeve=4, belt="#5A4020", chest_plate="#6E5A3A", iris="#A03010"),
    "supermutant_3": dict(skin="#877130", hair="#1E1608", shirt="#50462E", pants="#4A3E28", boots="#2A2218", bandana="#7A4A2A", scars=True, teeth=True, mouth="#3A1A10", sleeve=6, vest="#3A3022", eye="#F0E080", blood=10),
}

# Recognisability pass: faction symbols, face/eye treatment per NPC type.
SKIN_EXTRA = {
    "army_soldier": dict(face_paint="#2A3A1E", net="#2C3A1E", chinstrap=True, faction=("star", "#E8E8E0"), patch_bg="#3A4A28", helmet_camo=True),
    "zombie_soldier": dict(one_eye=True, bandage=True, faction=("star", "#7A8A6A"), patch_bg="#2A301C", jaw_ragged=True),
    "bandit": dict(face_paint=None, headband="#7A2A22", mask="#2A2622", bandolier=True, faction=("skull", "#D8D0B8"), patch_bg="#2A2420"),
    "dictator": dict(sunglasses=True, epaulettes=True, faction=("bar", "#E0C040"), patch_bg="#4A1A1A"),
    "cyberdemon": dict(brow=True, jaw_plate="#4A4C54", jaw_glow="#FF7020", cracks="#FF9A30", tusks=True),
    "supermutant_1": dict(brow=True, tusks=True, spikes=True, face_paint=None),
    "supermutant_2": dict(mohawk="#3A5A2A", brow=True, eyepatch=True, faction=("cross", "#C03030"), patch_bg="#2A2218"),
    "supermutant_3": dict(headband="#B03030", brow=True, spikes=True, bandolier=True, faction=("gear", "#E0C040"), patch_bg="#2A2218"),
}
for _k, _v in SKIN_EXTRA.items():
    SKINS[_k].update({a: b for a, b in _v.items() if b is not None})
SKINS["bandit"].pop("bandana", None)
SKINS["bandit"]["mask"] = "#2A2622"


# ---- non-biped entity atlases ----------------------------------------------------
def make_alienbug(man):
    parts = [("thorax", "chitin", 16, 8, 12, ["spots", "rings"]), ("abdomen", "chitin", 20, 10, 14, ["spots"]), ("head", "chitin", 10, 8, 10, ["spots"]),
             ("mandible_l", "bone", 6, 2, 3, ["rings"]), ("mandible_r", "bone", 6, 2, 3, ["rings"]), ("leg_front", "chitin", 14, 2, 2, ["rings"]),
             ("leg_mid", "chitin", 14, 2, 2, ["rings"]), ("leg_back", "chitin", 14, 2, 2, ["rings"]), ("wing_l", "wing", 16, 1, 8, ["spots"]),
             ("wing_r", "wing", 16, 1, 8, ["spots"]), ("eye_l", "glow", 3, 3, 2, ["strip"]), ("eye_r", "glow", 3, 3, 2, ["strip"]), ("stinger", "bone", 3, 3, 8, [])]
    mm = {"chitin": ("polymer", "#AF8A56", None), "bone": ("polymer", "#D8C8A0", None), "wing": ("glass", "#B8A070", None), "glow": ("plain", "#FF4020", None)}
    return paint_atlas(man["id"], 64, 64, parts, mm, {"glow": "#FF4020", "spot": (60, 36, 18), "accent": "#6A4A20"}, cell=(5, 10), wear_amt=.15)


def make_gore(man):
    parts = [("chunk_a", "meat", 12, 8, 10, ["spots"]), ("chunk_b", "meat", 10, 6, 8, ["spots"]), ("chunk_c", "meat", 8, 8, 8, ["spots"]), ("chunk_d", "meat", 14, 5, 6, ["spots"]),
             ("organ", "organ", 10, 6, 6, ["spots"]), ("rib_cage", "bone", 12, 10, 6, ["rings", "grill"]), ("bone_a", "bone", 14, 2, 2, ["rings"]), ("bone_b", "bone", 10, 2, 2, ["rings"]),
             ("skull", "bone", 8, 8, 8, ["spots"]), ("gristle", "organ", 6, 4, 4, ["spots"]), ("limb", "meat", 16, 4, 4, ["spots"])]
    mm = {"meat": ("leather", "#761E18", None), "organ": ("skin", "#8A3A4A", None), "bone": ("leather", "#D8D0B8", None)}
    return paint_atlas(man["id"], 128, 128, parts, mm, {"spot": (50, 5, 5)}, cell=(6, 14), wear_amt=.2)


def make_apache(man):
    parts = [("fuselage", "hull", 40, 10, 12, ["vents", "label", "cross"]), ("tail_boom", "hull", 40, 4, 4, ["label"]), ("nose", "hull", 10, 6, 8, ["vents"]),
             ("canopy", "glass", 14, 6, 8, ["lens"]), ("rotor_blade", "dark", 50, 1, 5, ["stripe"]), ("rotor_hub", "dark", 6, 4, 6, ["rings", "label"]),
             ("tail_rotor", "dark", 10, 1, 3, []), ("stub_wing_l", "hull", 22, 1, 8, ["cross"]), ("stub_wing_r", "hull", 22, 1, 8, ["cross"]),
             ("missile_pod_l", "dark", 12, 4, 4, ["grill", "hazard"]), ("missile_pod_r", "dark", 12, 4, 4, ["grill", "hazard"]),
             ("engine_l", "hull", 12, 6, 6, ["vents"]), ("engine_r", "hull", 12, 6, 6, ["vents"]), ("skid_l", "dark", 30, 1, 2, ["rings"]),
             ("skid_r", "dark", 30, 1, 2, ["rings"]), ("chin_gun", "dark", 6, 4, 4, ["rings"]), ("tail_fin", "hull", 8, 8, 1, ["label"])]
    mm = {"hull": ("steel", "#3A402A", None), "dark": ("steel", "#22261A", None), "glass": ("glass", "#2A4A4A", None)}
    return paint_atlas(man["id"], 256, 256, parts, mm, {"lens": "#3A6A6A", "accent": "#C8C8A0"}, cell=(8, 20), wear_amt=.4)


# ---- projectiles ------------------------------------------------------------
def capsule(t, x, y, w, h, body, tip, glow=None):
    a = t.rgb
    if glow:
        for k in range(3, 0, -1):
            rect(a, x - k, y - k, w + 2 * k, h + 2 * k, glow, .10 * (4 - k))
            t.a[max(0, y - k):y + h + k, max(0, x - k):x + w + k] = np.maximum(t.a[max(0, y - k):y + h + k, max(0, x - k):x + w + k], 60 * (4 - k))
    for i in range(h):
        f = 1.3 - 0.8 * i / max(1, h - 1)
        c = np.clip(hx(body) * f, 0, 255)
        rect(a, x, y + i, w, 1, c)
    rect(a, x + w - h // 2 - 1, y, h // 2 + 1, h, tip)
    for i in range(h):
        pass
    t.a[y:y + h, x:x + w] = 255
    for cx, cy in ((x, y), (x, y + h - 1), (x + w - 1, y), (x + w - 1, y + h - 1)):
        t.a[cy, cx] = 0


def make_bullet(man):
    t = Tex(32, 32)
    capsule(t, 6, 5, 20, 6, "#D8A838", "#F4D878", "#FFB040")
    rect(t.rgb, 6, 5, 4, 6, "#8A5A18")
    capsule(t, 6, 20, 20, 6, "#FF8A20", "#FFE090", "#FF6010")
    rect(t.rgb, 6, 20, 4, 6, "#A03808")
    LAYOUT_DOC[man["id"]] = ["two sprites: brass capsule tracer y=5..10, orange twin y=20..25 (both x=6..25, tip at +x)."]
    return t


def make_handgun_bullet(man):
    t = Tex(16, 16)
    capsule(t, 3, 6, 10, 4, "#CDCDD2", "#EEEEF2", None)
    rect(t.rgb, 3, 6, 3, 4, "#B07038")
    LAYOUT_DOC[man["id"]] = ["silver slug with copper base, x=3..12 y=6..9."]
    return t


def make_launcher_grenade(man):
    t = Tex(32, 16); r = rng_for(man["id"])
    body = material("steel", r, 12, 22, "#5A6048"); panelize(body, r, 5, 10)
    t.paste(0, 2, body)
    rect(t.rgb, 8, 2, 4, 12, "#C04030"); mul(t.rgb, 8, 2, 4, 1, 1.25); mul(t.rgb, 8, 13, 4, 1, .7)
    nose = material("steel", r, 12, 8, "#3A3E2C"); t.paste(22, 2, nose); rect(t.rgb, 22, 2, 1, 12, "#D8B848")
    rect(t.rgb, 29, 5, 3, 6, "#202220")
    t.a[2:14, 0:32] = 255
    for y in range(2, 14):
        t.rgb[y, :22] *= 1.12 - 0.35 * (y - 2) / 11
    LAYOUT_DOC[man["id"]] = ["strip map: body 0..21 x 2..13 with red band at x=8..11; nose 22..31; flat 32x16 (no seams)."]
    return t


def make_beam(man, core, mid, edge):
    t = Tex(32, 32)
    yy = np.abs(np.arange(32) - 15.5) / 15.5
    xx = np.minimum(np.arange(32), 31 - np.arange(32)) / 4.0
    for y in range(32):
        d = yy[y]
        f = max(0.0, 1 - d)
        col = np.array(hx(edge)) * (1 - f) + np.array(hx(mid)) * f
        if d < .32:
            col = np.array(hx(mid)) * (d / .32) + np.array(hx(core)) * (1 - d / .32)
        t.rgb[y, :] = col
        t.a[y, :] = 255 * (f ** 0.8) * np.clip(xx, 0.25, 1)[:] if True else 255
    LAYOUT_DOC[man["id"]] = ["beam gradient across rows (white-hot core at y=15..16, fading to transparent at y=0/31); ends fade over 4 px."]
    return t


def make_bioblob(man):
    t = Tex(64, 64); r = rng_for(man["id"])
    yy, xx = np.mgrid[0:64, 0:64]
    d = np.hypot(xx - 31.5, yy - 31.5) / 30 + (vnoise(r, 64, 64, 8) - .5) * .35
    m = d < 1
    g = np.clip(1 - d, 0, 1)
    t.rgb[...] = (np.array(hx("#408000")) * (1 - g[..., None]) + np.array(hx("#A0E0A0")) * g[..., None]) * (0.9 + 0.2 * vnoise(r, 64, 64, 3))[..., None]
    t.a[m] = 120 + 135 * np.clip(g[m] * 2.2, 0, 1)
    for _ in range(10):
        x, y = int(r.integers(10, 54)), int(r.integers(10, 54))
        if m[y, x]:
            rect(t.rgb, x, y, 3, 3, "#206000", .7); rect(t.rgb, x, y, 1, 1, "#E8FFE8")
    LAYOUT_DOC[man["id"]] = ["radial translucent blob filling the 64x64 sheet (single sprite)."]
    return t


def make_bio_blob_machine(man):
    t = Tex(16, 16); r = rng_for(man["id"])
    yy, xx = np.mgrid[0:16, 0:16]
    d = np.hypot(xx - 7.5, yy - 7.5) / 7.5 + (vnoise(r, 16, 16, 3) - .5) * .3
    g = np.clip(1 - d, 0, 1)
    t.rgb[...] = np.array(hx("#1E6A08")) * (1 - g[..., None]) + np.array(hx("#7CE04C")) * g[..., None]
    t.a[d < 1] = 255
    rect(t.rgb, 4, 4, 2, 2, "#E4FFD4")
    LAYOUT_DOC[man["id"]] = ["single 16x16 gel sprite (the blob block-entity renders it as one face)."]
    return t


# ---- structured projectiles (override the plain versions above) ---------------------------
def make_beam(man, core, mid, edge):
    """Beam quad: white-hot core, colored inner sheath, exponential glow falloff, traveling
    energy bands along x and bright knots; ends fade."""
    t = Tex(32, 32)
    y = np.abs(np.arange(32) - 15.5)[:, None] / 15.5
    x = np.arange(32)[None, :]
    glow = np.exp(-(y / 0.75) ** 2)
    sheath = np.exp(-(y / 0.34) ** 2)
    hot = np.exp(-(y / 0.11) ** 2)
    band = 0.78 + 0.22 * np.sin(x * 2 * np.pi / 8.0)
    knot = np.exp(-((x % 16 - 8) / 2.2) ** 2) * 0.5
    inner = np.clip(sheath * band + knot * sheath, 0, 1)
    ring = np.exp(-((y - 0.55) / 0.06) ** 2) * 0.55 * (0.6 + 0.4 * np.sin(x * 2 * np.pi / 8.0 + 1.5))
    E, M, C = np.array(hx(edge)), np.array(hx(mid)), np.array(hx(core))
    rgb = E * glow[..., None] * 1.0 + M * np.clip(inner + ring, 0, 1)[..., None]
    rgb = rgb / np.maximum(1, (glow + inner + ring)[..., None]) * 1.6
    rgb = rgb * (1 - hot[..., None]) + C * hot[..., None]
    cap = np.clip(np.minimum(x, 31 - x) / 4.0, 0.15, 1)
    a = np.clip(glow * 0.55 + inner * 0.6 + ring * 0.4 + hot, 0, 1) * cap * 255
    t.rgb[...] = np.clip(rgb, 0, 255); t.a[...] = a
    LAYOUT_DOC[man["id"]] = ["beam quad: white-hot core rows 14..17, colored sheath, exponential glow to transparent at y=0/31, 8 px energy bands with bright knots every 16 px, 4 px end fade."]
    return t


def make_bioblob(man):
    t = Tex(64, 64); r = rng_for(man["id"])
    yy, xx = np.mgrid[0:64, 0:64]
    wob = (vnoise(r, 64, 64, 8) - .5) * .3
    d = np.hypot(xx - 31.5, yy - 31.5) / 30 + wob
    inside = d < 1
    membrane = np.exp(-((d - 0.9) / 0.07) ** 2)
    body = np.clip(1 - d, 0, 1)
    nuc_d = np.hypot(xx - 28, yy - 28) / 12 + wob * .5
    nucleus = np.exp(-(nuc_d / 0.8) ** 3)
    band = 0.9 + 0.1 * np.sin(d * 28)
    base = np.array(hx("#2E6A10")) * (0.6 + 0.6 * body)[..., None] * band[..., None]
    rgb = base * (1 - nucleus[..., None] * .5) + np.array(hx("#A0F070")) * membrane[..., None] * .9 + np.array(hx("#D8FFC0")) * (nucleus ** 3)[..., None] * .6
    t.rgb[...] = np.clip(rgb, 0, 255)
    t.a[inside] = np.clip(110 + 90 * body[inside] + 120 * membrane[inside] + 80 * nucleus[inside], 0, 255)
    for _ in range(9):  # bubbles
        bx, by = int(r.integers(12, 52)), int(r.integers(12, 52)); br = int(r.integers(2, 4))
        if inside[by, bx]:
            dd = np.hypot(xx - bx, yy - by)
            t.rgb[(dd > br - 1) & (dd <= br)] = (210, 255, 190); t.a[(dd > br - 1) & (dd <= br)] = 230
            t.rgb[by - 1, bx - 1] = (255, 255, 255)
    LAYOUT_DOC[man["id"]] = ["membrane ring, darker nucleus, concentric banding and bubbles on a translucent 64x64 blob (single sprite)."]
    return t


def make_bullet(man):
    t = Tex(32, 32)
    for y0, body, tip, glow in ((5, "#D8A838", "#F4D878", "#FFB040"), (20, "#FF8A20", "#FFE090", "#FF6010")):
        capsule(t, 9, y0, 18, 6, body, tip, glow)
        for gx in (12, 14):   # casing grooves
            rect(t.rgb, gx, y0, 1, 6, (70, 40, 10), .6)
        rect(t.rgb, 9, y0, 3, 6, "#8A5A18"); rect(t.rgb, 9, y0 + 2, 3, 1, "#C89038")   # base band + primer ring
        rect(t.rgb, 11, y0 + 1, 15, 1, (255, 255, 255), .35)                           # specular line
        for i in range(9):                                                              # tracer trail, fading left
            a = int(190 * (i + 1) / 9)
            t.a[y0 + 2:y0 + 4, i] = np.maximum(t.a[y0 + 2:y0 + 4, i], a)
            t.rgb[y0 + 2:y0 + 4, i] = np.array(hx(glow)) * (0.7 + 0.3 * i / 9)
    LAYOUT_DOC[man["id"]] = ["two sprites: brass tracer at y=5..10, orange twin y=20..25 (capsule x=9..26, tip +x, fading trail x=0..8)."]
    return t


def make_handgun_bullet(man):
    t = Tex(16, 16)
    capsule(t, 3, 6, 10, 4, "#CDCDD2", "#EEEEF2", None)
    rect(t.rgb, 3, 6, 3, 4, "#B07038"); rect(t.rgb, 5, 6, 1, 4, (80, 40, 10), .7)          # copper base with crimp groove
    rect(t.rgb, 6, 6, 5, 1, (255, 255, 255), .5); rect(t.rgb, 11, 7, 2, 2, "#8A8A92", .6)    # jacket highlight, nose shade
    LAYOUT_DOC[man["id"]] = ["silver jacketed slug, copper base with groove, x=3..12 y=6..9."]
    return t


def make_launcher_grenade(man):
    t = Tex(32, 16); r = rng_for(man["id"])
    body = material("steel", r, 12, 22, "#5A6048"); panelize(body, r, 5, 10)
    t.paste(0, 2, body)
    for y in range(2, 14):
        t.rgb[y, :22] *= 1.12 - 0.35 * (y - 2) / 11
    rect(t.rgb, 8, 2, 4, 12, "#C04030"); mul(t.rgb, 8, 2, 4, 1, 1.25); mul(t.rgb, 8, 13, 4, 1, .7)   # warhead band
    for bx in (3, 14, 17):                                                                           # banding: yellow rings + driving band
        rect(t.rgb, bx, 2, 1, 12, "#D8B848", .9)
    rect(t.rgb, 19, 2, 3, 12, "#B8742E"); mul(t.rgb, 19, 2, 1, 12, 1.2)
    nose = material("steel", r, 12, 8, "#3A3E2C"); t.paste(22, 2, nose)
    rect(t.rgb, 22, 2, 1, 12, "#D8B848"); rect(t.rgb, 25, 2, 2, 12, "#E8E8E0", .6)
    rect(t.rgb, 29, 5, 3, 6, "#202220"); rect(t.rgb, 29, 7, 3, 2, "#C04030")
    t.a[2:14, 0:32] = 255
    LAYOUT_DOC[man["id"]] = ["strip map: body 0..21 x 2..13 (red band x=8..11, yellow rings at x=3/14/17, copper driving band x=19..21); nose 22..31 (white stencil ring, red fuse tip)."]
    return t


# ---- machines ---------------------------------------------------------------------
def make_ammo_press_parts(man):
    parts = [("ram_shaft", "steel", 4, 12, 4, ["rings"]), ("ram_head", "dark", 8, 3, 8, ["label"]), ("ram_glow_face", "glow", 8, 1, 8, ["cells"]),
             ("hood", "steel", 20, 8, 16, ["vents", "hazard"]), ("belt_box_a", "dark", 14, 4, 6, ["grill"]), ("belt_box_b", "dark", 14, 4, 6, ["grill", "rings"]),
             ("round_a", "brass", 1, 3, 1, []), ("round_b", "brass", 1, 3, 1, []), ("round_c", "brass", 1, 3, 1, []),
             ("casing_a", "brass", 2, 3, 2, ["rings"]), ("casing_b", "brass", 2, 3, 2, ["rings"])]
    mm = {"steel": ("steel", "#808080", None), "dark": ("steel", "#404246", None), "glow": ("plain", "#E8A040", None), "brass": ("steel", "#C8A040", None)}
    t = paint_atlas(man["id"], 256, 128, parts, mm, {"glow": "#E8A040", "accent": "#E8A040"}, region=(0, 0, 256, 96))
    r = rng_for(man["id"] + "/steam")
    puff = Tex(64, 32)
    yy, xx = np.mgrid[0:32, 0:64]
    for _ in range(9):
        cx, cy, rad = r.integers(6, 58), r.integers(6, 26), r.integers(4, 9)
        dd = np.hypot(xx - cx, yy - cy) / rad
        a = np.clip(1.1 - dd, 0, 1) * 200
        puff.a = np.maximum(puff.a, a)
    puff.rgb[...] = (235, 238, 242)
    t.paste(0, 96, puff.rgb, puff.a)
    LAYOUT_DOC[man["id"]].append("`steam_puff` plane region u=0 v=96 w=64 h=32 (soft white blobs, alpha).")
    return t


def make_turret(man):
    name = man["id"].replace("model.machine.", "")
    base = man["palette"][0]
    carbon = "carbon" in name
    mat = "carbon" if carbon else "steel"
    col = "#2A2A2E" if carbon else tone(base, 0.8)
    parts = [("base_plate", "hull", 16, 2, 16, ["hazard"]), ("pedestal", "hull", 6, 10, 6, ["rings"]), ("swivel", "hull", 10, 3, 10, ["label"]),
             ("cradle", "hull", 12, 6, 8, ["vents"]), ("shield", "dark", 12, 8, 2, ["stripe"]), ("ammo_box", "dark", 6, 5, 8, ["label", "hazard"]),
             ("servo", "glow", 4, 4, 4, ["cells"]), ("barrel_sleeve", "dark", 14, 3, 3, ["rings"]), ("foot_a", "hull", 4, 2, 4, []), ("foot_b", "hull", 4, 2, 4, [])]
    mm = {"hull": (mat, col, None), "dark": ("steel", tone(col, .5) if not carbon else "#141416", None), "glow": ("plain", "#40FF70" if not carbon else "#FFA010", None)}
    ctx = {"glow": mm["glow"][1], "accent": "#E0B020" if not carbon else "#A07010"}
    return paint_atlas(man["id"], 128, 128, parts, mm, ctx, r_key="model.machine.turret_base_shared" if False else man["id"], cell=(6, 14), wear_amt=.35)


def make_charge(man):
    name = man["id"].replace("model.machine.", "")
    armed = name.endswith("_armed")
    adv = "advanced" in name
    base = "#901010" if adv else "#303030"
    parts = [("charge_block_a", "main", 16, 8, 10, ["hazard", "label"]), ("charge_block_b", "main", 16, 8, 10, ["label", "stripe"]),
             ("timer_panel", "dark", 10, 6, 2, ["display"]), ("antenna", "dark", 1, 8, 1, []), ("led_a", "glow", 2, 2, 2, ["strip"]), ("led_b", "glow", 2, 2, 2, ["strip"]),
             ("strap", "dark", 16, 1, 10, ["stripe"]), ("detonator", "dark", 4, 3, 4, ["rings"])]
    if adv:
        parts.append(("dish", "dark", 6, 1, 6, ["coil"]))
    mm = {"main": ("polymer", base, None), "dark": ("steel", "#1C1C20", None),
          "glow": ("plain", "#FF2010" if armed else "#4A1008", None)}
    ctx = {"armed": armed, "glow": "#FF2020", "accent": "#C8A020"}
    return paint_atlas(man["id"], 64, 64, parts, mm, ctx, r_key="model.machine." + name.replace("_armed", "") + "/shared", cell=(4, 9), wear_amt=.25)


def make_chest(man):
    name = man["id"].replace("model.machine.", "")
    W, H = man["width"], man["height"]
    double = "double" in name
    weapon = name.startswith("weapon")
    r = rng_for(man["id"])
    t = Tex(W, H)
    w = 60 if double else 28
    base, bands = ("#303010", "#101010") if weapon else ("#808080", "#643200")
    mat = "steel"
    ctx = {"accent": bands, "glow": "#FF8000"}
    det_l = ["bands", "latch"] if not weapon else ["stencil", "latch", "hazard"]
    det_b = ["bands"] if not weapon else ["stencil", "rivets"] if False else ["stencil"]
    paint_box(t, 0, 0, w, 10, 28, mat, base, rng_for(man["id"] + "/lid"), det_l, ctx, cell=(6, 14), wear_amt=.35)
    paint_box(t, 0, 38, w, 20, 28, mat, base, rng_for(man["id"] + "/base"), det_b, ctx, cell=(6, 14), wear_amt=.35)
    LAYOUT_DOC[man["id"]] = ["vanilla chest layout scaled x2: lid box u=0 v=0 %dx10x28; base box u=0 v=38 %dx20x28 (lid latch painted on lid front; the vanilla knob region overlaps the lid and is left to the lid art)." % (w, w)]
    return t


def make_flagpost(man):
    parts = [("foot", "conc", 16, 4, 16, ["rings"]), ("pole_lower", "pole", 2, 40, 2, ["rings"]), ("pole_upper", "pole", 2, 40, 2, ["rings"]),
             ("cloth", "cloth", 24, 14, 1, ["flag"]), ("cap", "pole", 3, 2, 3, []), ("rope", "dark", 1, 30, 1, [])]
    mm = {"conc": ("concrete", "#9A9A98", None), "pole": ("steel", "#D0D0D0", None), "cloth": ("fabric", "#C8C8C8", None), "dark": ("rubber", "#303030", None)}
    return paint_atlas(man["id"], 128, 128, parts, mm, {}, cell=(6, 14), wear_amt=.3)


# ---- dispatch ----------------------------------------------------------------------
def build(man):
    i = man["id"]
    if i.startswith("model.gun."):
        return make_gun(man)
    if i.startswith("model.armor."):
        return make_armor(man)
    if i.startswith("model.entity."):
        n = i[len("model.entity."):]
        if n in SKINS:
            return biped(man, SKINS[n])
        return {"alienbug": make_alienbug, "gore": make_gore, "apache": make_apache, "bullet": make_bullet,
                "handgun_bullet": make_handgun_bullet, "launcher_grenade": make_launcher_grenade,
                "laser_beam": lambda m: make_beam(m, "#FFFFFF", "#FF3030", "#C00000"),
                "blue_beam": lambda m: make_beam(m, "#E8F4FF", "#3C88FF", "#004080"), "bioblob": make_bioblob}[n](man)
    if i.startswith("model.machine."):
        n = i[len("model.machine."):]
        if n == "ammo_press_parts":
            return make_ammo_press_parts(man)
        if n.startswith("turret_base"):
            return make_turret(man)
        if "charge" in n:
            return make_charge(man)
        if "chest" in n:
            return make_chest(man)
        if n == "flagpost":
            return make_flagpost(man)
        if n == "bio_blob":
            return make_bio_blob_machine(man)
    raise KeyError(i)


def write_layout_doc(order):
    out = ["# Model texture layouts", "",
           "Generated by `tools/asset-gen/models.py` and `gui.py`. Techguns' UV layouts are not used (originality rule), so these are the",
           "layouts of the GregGuns textures; build or adapt the models to them.", "",
           "## Conventions", "",
           "- **Box unwrap.** For a box of size w x h x d at (u, v): top (u+d, v, w x d); bottom (u+d+w, v, w x d); right (u, v+d, d x h); "
           "front (u+d, v+d, w x h); left (u+d+w, v+d, d x h); back (u+2d+w, v+d, w x h). This is the vanilla `ModelBox` layout.",
           "- **Packed atlases** (guns, accessories, machines, non-biped mobs): each named box below was shelf-packed into the sheet, scaled up so the "
           "footprint is filled. The listed `u v w h d` are in texels; model the part at that size (or scale UVs).",
           "- **Armor layers** use the vanilla 64x32 armor unwrap scaled by width/64 (head 0,0 8x8x8; body 16,16 8x12x4; right arm 40,16 4x12x4; "
           "right leg 0,16 4x12x4; left limbs mirror the right). Taller sheets keep the same layout in the top half and hold extra boxes below.",
           "- **Biped mob skins** use the modern 64x64 unwrap scaled by width/64 (left limbs at 32,48 and 16,48).",
           "- **Gun roles.** `metal` = gate-material steel, `dark` = darkened steel, `furn` = polymer or wood furniture, `glow` = emissive "
           "color (bright, intended for a lightmap/emissive pass). `_camoN` sheets share the base gun's layout exactly.",
           "- Camo and color variants share the base layout; only colors/patterns differ.", ""]
    out.append("## Model textures")
    out.append("")
    for aid in order:
        if aid not in LAYOUT_DOC:
            continue
        out.append("### %s" % aid.replace("model.", ""))
        for ln in LAYOUT_DOC[aid]:
            out.append("- " + ln)
        out.append("")
    out.append("`model.obj.*` entries (apache.obj, launcher_grenade.obj) are meshes, not painted: skipped by this generator.")
    out.append("")
    gui.build_all()
    out.append(gui.layout_markdown())
    with open(os.path.join(ROOT, "tools", "asset-gen", "model_layouts.md"), "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(out) + "\n")


def contact_sheet(items):
    cell_w, cell_h, cols = 168, 150, 8
    rows = (len(items) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * cell_w, rows * cell_h), (28, 28, 32))
    d = ImageDraw.Draw(sheet)
    for i, (aid, im) in enumerate(items):
        x, y = (i % cols) * cell_w, (i // cols) * cell_h
        bg = Image.new("RGBA", im.size, (74, 96, 84, 255)); bg.alpha_composite(im)
        sc = min(160 / im.width, 128 / im.height)
        if sc >= 1:
            sc = max(1, int(sc)); rs = Image.NEAREST
        else:
            rs = Image.BOX
        tw, th = max(1, int(im.width * sc)), max(1, int(im.height * sc))
        sheet.paste(bg.resize((tw, th), rs).convert("RGB"), (x + 4, y + 16))
        d.text((x + 4, y + 3), "%s %dx%d" % (aid.replace("model.", "")[:24], im.width, im.height), fill=(235, 235, 235))
    return sheet


ARMOR_MODEL_PREFIXES = tuple("model.armor." + k for k in ("t3_exo", "t3_power", "steam", "t2_riot", "t2_beret"))


def main():
    man = [m for m in json.load(open(MANIFEST, encoding="utf-8")) if m["kind"] == "model" and m["width"]]
    order = {"P1": 0, "P2": 1, "P3": 2}
    man.sort(key=lambda m: (order[m["priority"]], m["id"]))
    items, rows, ids = [], [], []
    counts = {}
    # The five custom-model armor sets (Exo, Power, Steam, Riot, Beret) are painted to fit their box models by
    # armor_models.py; skip the old vanilla-unwrap sheets here so a re-run never overwrites them.
    man = [m for m in man if not m["id"].startswith(ARMOR_MODEL_PREFIXES)]
    for m in man:
        t = build(m)
        out = os.path.join(RES, m["path"])
        os.makedirs(os.path.dirname(out), exist_ok=True)
        im = t.image()
        assert im.size == (m["width"], m["height"]), (m["id"], im.size)
        im.save(out)
        items.append((m["id"], im)); ids.append(m["id"])
        counts[m["priority"]] = counts.get(m["priority"], 0) + 1
        rows.append([m["id"], "src/main/resources/assets/gregguns/" + m["path"], "tools/asset-gen/models.py", seed_for(m["id"]),
                     "Procedural numpy/Pillow: packed box atlas or vanilla unwrap, panels, bolts, wear, emissive strips", "MIT", "2026-10-18"])
    with open(os.path.join(ROOT, "tools", "asset-gen", "credits", "models.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["asset", "path", "generator", "seed", "method", "license", "date"]); w.writerows(rows)
    write_layout_doc(ids)
    os.makedirs(PRE, exist_ok=True)
    contact_sheet(items).save(os.path.join(PRE, "models.png"))
    print("models:", len(items), counts)


if __name__ == "__main__":
    main()
