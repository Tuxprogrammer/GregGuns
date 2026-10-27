"""Machine, turret and multiblock scenes for the infographics.

Each multiblock builder returns the voxel blocks, their textures and a legend of what the player has to craft, with
counts taken from the same layout strings the mod checks the structure against.
"""

from __future__ import annotations

import re
from collections import Counter

import numpy as np

import blocks as B
import ctmsel
import gt
import kit as K
import render3d as R
import vanilla

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


def _wall(name):
    """A casing whose faces join their neighbors, as in game (ctmsel.py); the legend shows its isolated tile."""
    return ctmsel.CtmTexture(name)


def _ctrl(casing, machine):
    return B.composite(_casing(casing), _ov(machine, "FRONT"), cache=K.TEXCACHE, name=f"{machine}_ctrl")


def _hatch_tex(casing_path, hatch):
    return (casing_path, casing_path, gt.hatch(hatch, casing_path))


HATCH_LABEL = {
    "input_bus": "Input Bus", "output_bus": "Output Bus", "energy_hatch": "Energy Hatch",
    "maintenance_hatch": "Maintenance Hatch", "muffler_hatch": "Muffler Hatch", "input_hatch": "Input Hatch",
    "output_hatch": "Output Hatch",
}


def lamp_tex():
    """The interior lamp: vanilla glowstone, read from the vanilla jar (the build also accepts a redstone lamp or any
    light level 12+ block)."""
    return vanilla.texture("blocks/glowstone")


def natura_tex():
    """Natura's redwood workbench (top, side, face), read from the Natura jar in the reference pack into the
    git-ignored build cache. Natura workbench textures are CC BY 3.0 (credited in docs/assets/CREDITS.md)."""
    import zipfile
    out = K.TEXCACHE / "natura"
    names = ("top", "side", "face")
    if not all((out / f"{n}.png").exists() for n in names):
        jars = [j for d in gt._reference_dirs() for j in sorted(d.glob("*/.minecraft/mods/Natura-*.jar"))]
        if not jars:
            raise SystemExit("Natura jar not found under reference/gtnh/")
        out.mkdir(parents=True, exist_ok=True)
        z = zipfile.ZipFile(jars[0])
        for n in names:
            (out / f"{n}.png").write_bytes(z.read(f"assets/natura/textures/blocks/redwood_workbench_{n}.png"))
    return tuple(out / f"{n}.png" for n in names)


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
        def icon(t):
            return t.base if isinstance(t, ctmsel.CtmTexture) else t[-1] if isinstance(t, tuple) else t
        return [(k, self.labels[k], icon(self.tex[k]), c[k], self.notes.get(k, "")) for k in self.tex if c[k]]

    def render(self, yaw=-35, pitch=30, ppu=36, tag="", marks=False, cut=None):
        """marks: ring the hatches (orange), the controller (blue) and the lamps (yellow) so a builder can find them.
        cut: a function (x, y, z) -> bool; blocks it returns True for are left out, to open the hall up."""
        tags = {k: (2 if k == "ctrl" else 3 if k == "lamp" else 1)
                for k in self.tex if k.startswith("h_") or k in ("ctrl", "mount", "lamp")} if marks else None
        bl = self.blocks if cut is None else {p: v for p, v in self.blocks.items() if not cut(*p)}
        return K.voxels(f"{self.name}{tag}", bl, self.tex, yaw=yaw, pitch=pitch, ppu=ppu, tags=tags)


def ammo_works(bays=4, only=None, bench=False) -> Build:
    b = Build(f"ammo_works{bays}{only or ''}{'-bench' if bench else ''}")
    wc = _casing("works_casing")
    b.define("works", "Works Casing", _wall("works_casing"))
    b.define("ram", "Ram Housing", B.first_frame(_casing("ram_housing"), K.TEXCACHE, "ram_housing"),
             "one on top of every press bay")
    b.define("belt", "Item Pipe Casing", gt.iconset("MACHINE_CASING_ITEM_PIPE_TIN"), "the belt: one tier along the whole floor line")
    b.define("glass", "Glass", gt.bartworks("BoronSilicateGlassBlock"), "the side windows; any tier works")
    b.define("frame", "Steel Frame Box", gt.frame("Steel"), "the press rods")
    b.define("pipe", "Steel Pipe Casing", gt.iconset("MACHINE_CASING_PIPE_STEEL"), "pipe runs under the roof")
    b.define("gearbox", "Steel Gear Box Casing", gt.iconset("MACHINE_CASING_GEARBOX_STEEL"))
    b.define("motor", "Motor Casing", gt.iconset("MACHINE_CASING_MOTOR"), "the feed motors")
    b.define("assembler", "Assembler Casing", gt.iconset("MACHINE_CASING_ASSEMBLER"), "the packing arch")
    b.define("ctrl", "Ammunition Works", (wc, wc, _ctrl("works_casing", "ammunition_works")), "controller, HV")
    b.define("lamp", "Interior Lamp", lamp_tex(), "glowstone, a redstone lamp or any light 12+ block; one under the roof of the feeder, every bay and the station")
    pieces = ([B.java_shape("AmmoWorksShape", "FEEDER")] + [B.java_shape("AmmoWorksShape", "BAY")] * bays
              + [B.java_shape("AmmoWorksShape", "STATION")])
    if bench:
        b.define("table", "Bench Table", natura_tex(), "Natura Workbench; a crafting table or Dire Crafting Table also works")
        pieces = pieces + [B.java_shape("AmmoWorksShape", "BENCH")]
    if only:
        pieces = [B.java_shape("AmmoWorksShape", only)] + ([B.java_shape("AmmoWorksShape", "BENCH")] if bench else [])
    cells = B.stack_aisles(pieces)
    b.put(cells, {"S": "lamp", "T": "table", "K": "works", "F": "works", "A": "works", "O": "works", "M": "ram", "I": "belt", "G": "glass", "R": "frame",
                  "P": "pipe", "B": "gearbox", "N": "motor", "Y": "assembler", "~": ("ctrl", FRONT)})
    if only == "STATION" and bench:
        # the close-up of the annex: the station's plain Output Bus and the bench's two buses
        last = max(p[2] for p in cells)
        b.hatch((2, 3, 3), "output_bus", BACK, wc)
        b.hatch((1, 1, last), "input_bus", BACK, wc)
        b.hatch((3, 1, last), "output_bus", BACK, wc)
        return b
    if only:
        return b
    last = max(p[2] for p in cells)
    b.hatch((0, 5, 0), "input_bus", UP, wc)
    b.hatch((4, 5, 0), "input_hatch", UP, wc)
    b.hatch((4, 5, 2), "muffler_hatch", UP, wc)
    b.hatch((0, 2, 0), "energy_hatch", FRONT, wc)
    b.hatch((4, 2, 0), "maintenance_hatch", FRONT, wc)
    if bench:
        # the station's back wall ends where the bench starts; the bench's own buses sit on its side walls
        sl = B.stack_aisles(pieces[:-1])
        sl = max(p[2] for p in sl)
        b.hatch((2, 3, sl), "output_bus", BACK, wc)
        b.hatch((1, 4, sl), "output_hatch", BACK, wc)
        b.hatch((1, 1, last), "input_bus", BACK, wc)
        b.hatch((3, 1, last), "output_bus", BACK, wc)
    else:
        b.hatch((2, 2, last), "output_bus", BACK, wc)
        b.hatch((1, 3, last), "output_hatch", BACK, wc)
    b.bays = bays
    b.size = (5, 7, last + 1)
    return b


def gantry(bays=4, only=None) -> Build:
    b = Build(f"gantry{bays}{only or ''}")
    gc = _casing("gantry_casing")
    b.define("casing", "Gantry Casing", _wall("gantry_casing"))
    b.define("beam", "Gantry Beam", _casing("gantry_beam"))
    b.define("hoist", "Gantry Hoist", _casing("gantry_hoist"), "one on top of every bay")
    b.define("cradle", "Repair Cradle", (_casing("repair_cradle_top"), _casing("repair_cradle_side")),
             "three in every bay floor")
    b.define("frame", "Titanium Frame Box", gt.frame("Titanium"), "the legs")
    b.define("glass", "Borosilicate Glass", gt.bartworks("BoronSilicateGlassBlock"), "booth windows")
    b.define("ctrl", "Armory Gantry", (gc, gc, _ctrl("gantry_casing", "armory_gantry")), "controller, HV")
    b.define("lamp", "Interior Lamp", lamp_tex(), "glowstone, a redstone lamp or any light 12+ block; one in the booth and one in every bay")
    b.define("rail", "Steel Frame Box", gt.frame("Steel"), "floor rails and the hoist hooks")
    b.define("gearbox", "Steel Gear Box Casing", gt.iconset("MACHINE_CASING_GEARBOX_STEEL"), "the hoist drive")
    b.define("motor", "Motor Casing", gt.iconset("MACHINE_CASING_MOTOR"), "the hoist motors")
    b.define("pipe", "Steel Pipe Casing", gt.iconset("MACHINE_CASING_PIPE_STEEL"), "hydraulic runs under the beam")
    pieces = ([B.java_shape("GantryShape", "HEAD")] + [B.java_shape("GantryShape", "BAY")] * bays
              + [B.java_shape("GantryShape", "TAIL")])
    if only:
        pieces = [B.java_shape("GantryShape", only)]
    cells = B.stack_aisles(pieces)
    b.put(cells, {"R": "casing", "B": "beam", "H": "hoist", "T": "cradle", "L": "frame", "G": "glass",
                  "S": "lamp", "F": "rail", "X": "gearbox", "N": "motor", "P": "pipe", "~": ("ctrl", FRONT)})
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
    b.define("plating", f"Bastion Plating ({plating.capitalize()})", _wall(f"bastion_plating_{plating}"),
             "any of Iron, Steel, Aluminum or Carbon")
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
