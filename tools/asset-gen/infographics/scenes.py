"""Machine, turret and multiblock scenes for the infographics.

Each multiblock builder returns the voxel blocks, their textures and a legend of what the player has to craft, with
counts taken from the same layout strings the mod checks the structure against.
"""

from __future__ import annotations

import re
from collections import Counter

import numpy as np

import blocks as B
import gt
import kit as K
import render3d as R

T = B.TEX
UP, FRONT, BACK, LEFT, RIGHT = (0, 1, 0), (0, 0, -1), (0, 0, 1), (-1, 0, 0), (1, 0, 0)


def _ov(machine, face):
    p = T / "machines" / machine / f"OVERLAY_{face}.png"
    return p if p.exists() else None


def machine_block(machine: str, tier: str, active=False, greg=False) -> K.Path:
    """A single block GT machine: a tier hull with the GregGuns overlays, or with GregTech's own basic machine
    overlays when greg is set (machine is then GT's folder name: lathe, bender, extruder, assembler...)."""
    sfx = "_ACTIVE" if active else ""
    ov_of = gt.machine_overlay if greg else _ov
    faces = {}
    for face in ("FRONT", "SIDE", "TOP"):
        hull = gt.hull(tier, "TOP" if face == "TOP" else "SIDE")
        ov = ov_of(machine, face + sfx) or ov_of(machine, face)
        # GT draws a face's glow layer at full brightness, so the idle glow shows on an idle machine too.
        glow = ov_of(machine, face + ("_ACTIVE_GLOW" if active else "_GLOW"))
        faces[face] = B.composite(hull, ov, glow, cache=K.TEXCACHE,
                                  name=f"{'gt-' if greg else ''}{machine}_{tier}_{face}{sfx}")
    blocks_ = {(0, 0, 0): ("m", FRONT)}
    tex = {"m": (faces["TOP"], faces["SIDE"], faces["FRONT"])}
    return K.voxels(f"machine-{'gt-' if greg else ''}{machine}-{tier}{sfx}", blocks_, tex, yaw=-38, pitch=28, ppu=150)


# ---------------------------------------------------------------- turret


def turret_scene(plate="steel", gun=None, pitch_deg=0.0) -> R.Scene:
    src = next(B.JAVA.rglob("TurretGeometry.java")).read_text()
    boxes = re.findall(r'new Box\("(\w+)", Part\.(\w+), ([-\d.]+)F?, ([-\d.]+)F?, ([-\d.]+)F?, (\d+), (\d+), (\d+), '
                       r'(\d+), (\d+)(?:, ([-\d.]+)F?)?\)', src)
    s = R.Scene()
    sfx = "" if plate is None else f"_{plate}"
    tb = s.texture(K.ASSETS / f"textures/models/machines/turret_base{sfx}.png")
    th = s.texture(K.ASSETS / f"textures/models/machines/turret_sheet{sfx}.png")
    body = R.MODEL_TO_WORLD @ R.tr(0, -16, 0)
    mount = body @ R.tr(0, -9.5, 0) @ R.rx(np.radians(pitch_deg))
    for name, part, x, y, z, w, h, d, u, v, rot in boxes:
        x, y, z = float(x), float(y), float(z)
        w, h, d, u, v = int(w), int(h), int(d), int(u), int(v)
        if part == "BASE":
            M = R.MODEL_TO_WORLD @ R.tr(x + w / 2, 0, -(z + d / 2)) @ R.ry(np.radians(float(rot or 0)))
            s.quads += R.box_quads(M, (-w / 2, -(y + h), -d / 2), (w, h, d), (u, v), 64, 64, False, 0, tb)
        elif part == "PITCH":
            s.quads += R.box_quads(mount, (x, 9.5 - y - h, -(z + d)), (w, h, d), (u, v), 64, 64, False, 0, th)
        else:
            s.quads += R.box_quads(body, (x, -y - h, -(z + d)), (w, h, d), (u, v), 64, 64, False, 0, th)
    if gun:
        g = R.gun_scene(K.export_dir() / f"{gun}.json")
        mn, mx = g.bounds()
        L = mx[0] - mn[0]
        k = K.HELD_LENGTH.get(gun, 1.2) * 0.9 / L
        c = (mn + mx) / 2
        pivot = (mount @ np.array([0, 0, 0, 1.0]))[:3]
        # muzzle (the gun's -X) along the turret's facing (-Z), sitting on the mount plate
        Bm = np.eye(4)
        Bm[:3, 0], Bm[:3, 1], Bm[:3, 2] = (0, 0, 1), (0, 1, 0), (-1, 0, 0)
        s.extend(g, R.tr(pivot[0], pivot[1] + 0.12, pivot[2] - 0.1) @ Bm @ R.sc(k) @ R.tr(-c[0], -mn[1], -c[2]))
    return s


def turret(plate="steel", gun="m4", yaw=-40, pitch=20, h=320):
    def make():
        return R.trim(R.render(turret_scene(plate, gun), yaw=yaw, pitch=pitch, ppu=h / 1.9, ss=2, outline=K.RIM), 3)
    return K._cached("turret", dict(p=plate, g=gun, y=yaw, pi=pitch, h=h, gm=K.export_sig(gun), gl=K.HELD_LENGTH.get(gun), v=4), make)


# ---------------------------------------------------------------- multiblocks


def _casing(name):
    return T / "casings" / f"{name}.png"


def _ctrl(casing, machine):
    return B.composite(_casing(casing), _ov(machine, "FRONT"), cache=K.TEXCACHE, name=f"{machine}_ctrl")


def _hatch_tex(casing_path, hatch):
    return (casing_path, casing_path, gt.hatch(hatch, casing_path))


HATCH_LABEL = {
    "input_bus": "Input Bus", "output_bus": "Output Bus", "energy_hatch": "Energy Hatch",
    "maintenance_hatch": "Maintenance Hatch", "muffler_hatch": "Muffler Hatch", "input_hatch": "Input Hatch",
    "output_hatch": "Output Hatch",
}


class Build:
    """Voxels plus a legend: [(key, label, texture, count, note)]."""

    def __init__(self, name):
        self.name = name
        self.blocks = {}
        self.tex = {}
        self.labels = {}
        self.notes = {}
        self.hatches = []  # (key, position)

    def define(self, key, label, tex, note=""):
        self.tex[key] = tex
        self.labels[key] = label
        self.notes[key] = note

    def put(self, cells: dict, mapping: dict):
        for p, ch in cells.items():
            if ch in mapping:
                self.blocks[p] = mapping[ch]

    def hatch(self, pos, kind, facing, casing_path):
        key = f"h_{kind}"
        if key not in self.tex:
            self.define(key, HATCH_LABEL[kind], _hatch_tex(casing_path, kind))
        self.blocks[pos] = (key, facing)
        self.hatches.append((key, pos))

    def legend(self):
        c = Counter(b[0] if isinstance(b, tuple) else b for b in self.blocks.values())
        return [(k, self.labels[k], self.tex[k][-1] if isinstance(self.tex[k], tuple) else self.tex[k], c[k],
                 self.notes.get(k, "")) for k in self.tex if c[k]]

    def render(self, yaw=-35, pitch=30, ppu=36, tag="", marks=False):
        """marks: ring the hatches (orange) and the controller (blue) so a builder can find them."""
        tags = {k: (2 if k == "ctrl" else 1) for k in self.tex if k.startswith("h_") or k in ("ctrl", "mount")} if marks else None
        return K.voxels(f"{self.name}{tag}", self.blocks, self.tex, yaw=yaw, pitch=pitch, ppu=ppu, tags=tags)


def ammo_works(bays=4, only=None) -> Build:
    b = Build(f"ammo_works{bays}{only or ''}")
    wc = _casing("works_casing")
    b.define("works", "Works Casing", wc)
    b.define("ram", "Ram Housing", B.first_frame(_casing("ram_housing"), K.TEXCACHE, "ram_housing"),
             "one on top of every press bay")
    b.define("belt", "Item Pipe Casing", gt.iconset("MACHINE_CASING_ITEM_PIPE_TIN"), "the belt: one tier along the whole floor line")
    b.define("ctrl", "Ammunition Works", (wc, wc, _ctrl("works_casing", "ammunition_works")), "controller, HV")
    pieces = ([B.java_shape("AmmoWorksShape", "FEEDER")] + [B.java_shape("AmmoWorksShape", "BAY")] * bays
              + [B.java_shape("AmmoWorksShape", "STATION")])
    if only:
        pieces = [B.java_shape("AmmoWorksShape", only)]
    cells = B.stack_aisles(pieces)
    b.put(cells, {"F": "works", "A": "works", "O": "works", "M": "ram", "I": "belt", "~": ("ctrl", FRONT)})
    if only:
        return b
    last = max(p[2] for p in cells)
    b.hatch((0, 5, 0), "input_bus", UP, wc)
    b.hatch((4, 5, 0), "input_hatch", UP, wc)
    b.hatch((4, 5, 2), "muffler_hatch", UP, wc)
    b.hatch((0, 2, 0), "energy_hatch", FRONT, wc)
    b.hatch((4, 2, 0), "maintenance_hatch", FRONT, wc)
    b.hatch((2, 2, last), "output_bus", BACK, wc)
    b.hatch((1, 3, last), "output_hatch", BACK, wc)
    b.bays = bays
    b.size = (5, 7, last + 1)
    return b


def retort(bulbs=3, only=None) -> Build:
    b = Build(f"retort{bulbs}{only or ''}")
    rc = _casing("reaction_casing")
    b.define("casing", "Reaction Chamber Casing", rc)
    b.define("glass", "Borosilicate Glass", gt.bartworks("BoronSilicateGlassBlock"), "any tier; the lowest glass sets the pressure rating")
    b.define("frame", "Steel Frame Box", gt.frame("Steel"), "the four feet")
    b.define("ctrl", "Retort", (rc, rc, _ctrl("reaction_casing", "retort")), "controller, EV")
    layers = (B.java_shape("RetortShape", "TOP") + B.java_shape("RetortShape", "BULB") * bulbs
              + B.java_shape("RetortShape", "BASE"))
    if only:
        layers = B.java_shape("RetortShape", only)
    cells = B.stack_layers(layers)
    b.put(cells, {"B": "casing", "C": "casing", "S": "casing", "G": "glass", "F": "frame", "~": ("ctrl", FRONT)})
    if only:
        return b
    # floor layer is y=1 (feet at y=0); the controller sits in the front row of the floor layer
    b.hatch((1, 1, 0), "input_bus", FRONT, rc)
    b.hatch((3, 1, 0), "energy_hatch", FRONT, rc)
    b.hatch((0, 1, 1), "maintenance_hatch", LEFT, rc)
    b.hatch((4, 1, 1), "output_bus", RIGHT, rc)
    b.hatch((0, 1, 2), "input_hatch", LEFT, rc)
    top = max(p[1] for p in cells)
    b.hatch((2, top, 2), "muffler_hatch", UP, rc)
    b.size = (5, top + 1, 5)
    return b


def gantry(bays=4, only=None) -> Build:
    b = Build(f"gantry{bays}{only or ''}")
    gc = _casing("gantry_casing")
    b.define("casing", "Gantry Casing", gc)
    b.define("beam", "Gantry Beam", _casing("gantry_beam"))
    b.define("hoist", "Gantry Hoist", _casing("gantry_hoist"), "one on top of every bay")
    b.define("cradle", "Repair Cradle", (_casing("repair_cradle_top"), _casing("repair_cradle_side")),
             "three in every bay floor")
    b.define("frame", "Titanium Frame Box", gt.frame("Titanium"), "the legs")
    b.define("glass", "Borosilicate Glass", gt.bartworks("BoronSilicateGlassBlock"), "booth windows")
    b.define("ctrl", "Armory Gantry", (gc, gc, _ctrl("gantry_casing", "armory_gantry")), "controller, HV")
    pieces = ([B.java_shape("GantryShape", "HEAD")] + [B.java_shape("GantryShape", "BAY")] * bays
              + [B.java_shape("GantryShape", "TAIL")])
    if only:
        pieces = [B.java_shape("GantryShape", only)]
    cells = B.stack_aisles(pieces)
    b.put(cells, {"R": "casing", "B": "beam", "H": "hoist", "T": "cradle", "L": "frame", "G": "glass",
                  "~": ("ctrl", FRONT)})
    if only:
        return b
    last = max(p[2] for p in cells)
    b.hatch((1, 1, 0), "input_bus", FRONT, gc)
    b.hatch((5, 1, 0), "energy_hatch", FRONT, gc)
    b.hatch((1, 0, 0), "maintenance_hatch", FRONT, gc)
    b.hatch((3, 2, last), "output_bus", BACK, gc)
    b.size = (7, 7, last + 1)
    return b


def rampart(mounts=6, plating="iron", only=None) -> Build:
    """The Rampart's strings run along its length, so x comes from the string and z from the aisle."""
    b = Build(f"rampart{mounts}{plating}{only or ''}")
    pl = _casing(f"bastion_plating_{plating}")
    b.define("plating", f"Bastion Plating ({plating.capitalize()})", pl, "any of Iron, Steel, Aluminum or Carbon")
    b.define("glass", "Borosilicate Glass", gt.bartworks("BoronSilicateGlassBlock"))
    gm = B.composite(pl, _ov("gun_mount_hatch", "FRONT"), cache=K.TEXCACHE, name="gun_mount")
    b.define("mount", "Gun Mount Hatch", (pl, pl, gm), "exactly one per bay, MV to IV")
    b.define("ctrl", "Rampart", (pl, pl, _ctrl(f"bastion_plating_{plating}", "rampart")), "controller, HV")
    gate = B.java_shape("RampartShape", "GATE")
    bay = B.java_shape("RampartShape", "BAY")
    cap = B.java_shape("RampartShape", "CAP")
    H = 6
    x0 = 0

    def lay(piece, x0):
        for z, aisle in enumerate(piece):
            for r, row in enumerate(aisle):
                y = len(aisle) - 1 - r
                for dx, ch in enumerate(row):
                    if ch not in " -.":
                        yield (x0 + dx, y, z), ch

    if only:
        piece = {"GATE": gate, "BAY": bay, "CAP": cap}[only]
        cells = dict(lay(piece, 0))
        b.put(cells, {"T": "plating", "P": "plating", "G": "glass", "H": ("mount", FRONT), "~": ("ctrl", FRONT)})
        return b
    cells = dict(lay(gate, 0))
    x0 = len(gate[0][0])
    for _ in range(mounts):
        cells.update(lay(bay, x0))
        x0 += len(bay[0][0])
    cells.update(lay(cap, x0))
    b.put(cells, {"T": "plating", "P": "plating", "G": "glass", "H": ("mount", FRONT), "~": ("ctrl", FRONT)})
    b.hatch((0, 1, 0), "input_bus", FRONT, pl)
    b.hatch((2, 1, 0), "output_bus", FRONT, pl)
    b.hatch((0, 3, 0), "energy_hatch", FRONT, pl)
    b.hatch((2, 3, 0), "maintenance_hatch", FRONT, pl)
    b.size = (x0 + 1, H, 3)
    return b
