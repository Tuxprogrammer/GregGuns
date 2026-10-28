"""Top-down maps of the silo complex for the icbm-silos guide sheet.

The cells come from the `ComplexExportTest` dump (the walking level of a plan from the same `ComplexLayout` and
`ComplexRaster` the world writer uses), so the map is the layout the game builds, not a drawing of it:

    GREGGUNS_EXPORT_COMPLEX="$PWD/build/infographics/complex.json" ./gradlew test --rerun \\
        --tests com.gregguns.tools.ComplexExportTest

`raster()` paints the cells to a cached PNG (rock, wall plating, floors tinted by room role, rails, the silo and its
hatches). `overlay()` returns an SVG laid over it with the room names, the silo outlines and the markers, so the text
is set in the page's own fonts. Both read the dump through `load()`.

Run on its own to write the maps to build/infographics/complex-maps/:  python tools/asset-gen/infographics/complex_map.py
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))

import kit as K  # noqa: E402

DUMP = K.WORK / "complex.json"

# Cell bytes (Cell.java): 0 rock, 1 wall, 2 air, 3/4 rail, 5 silo concrete, 6 silo glass, 7 gantry beam, 8 pad,
# 9 controller, 10 to 13 hatches, 14 ladder, 15 radar controller.
CELL = {
    0: (14, 18, 24), 1: (96, 112, 132), 2: (184, 194, 206), 3: (214, 168, 92), 4: (214, 168, 92),
    5: (138, 150, 164), 6: (126, 214, 240), 7: (236, 170, 44), 8: (230, 232, 236), 9: (56, 198, 217),
    10: (255, 138, 31), 11: (255, 138, 31), 12: (255, 138, 31), 13: (255, 138, 31), 14: (150, 110, 60),
    15: (56, 198, 217),
}

# Room roles: (name, floor tint, the room kinds). The tint replaces the air color inside a room's box.
ROLES = [
    ("Command", (122, 168, 255), ["LAUNCH_CONTROL", "RADAR_TRACKING", "COMMS", "COMMANDER_OFFICE"]),
    ("Weapons and stores", (255, 128, 112), ["WARHEAD_MAGAZINE", "ARMORY", "STOCKPILE", "FUEL_DEPOT"]),
    ("Power and works", (255, 184, 72), ["GENERATOR_HALL", "AIR_HANDLING", "PUMP_ROOM", "WORKSHOP"]),
    ("Crew", (126, 218, 140), ["BUNKS", "MESS_GALLEY", "INFIRMARY"]),
    ("Guard posts and airlock", (196, 150, 240), ["GUARD_POST", "DECON_AIRLOCK"]),
    ("Assembly bay (hall)", (236, 240, 246), ["ASSEMBLY_BAY"]),
]
ROLE_OF = {k: (name, rgb) for name, rgb, kinds in ROLES for k in kinds}

# Player-facing room names.
NAMES = {
    "ASSEMBLY_BAY": "Assembly Bay", "LAUNCH_CONTROL": "Launch Control", "RADAR_TRACKING": "Radar Room",
    "COMMS": "Comms", "COMMANDER_OFFICE": "Commander's Office", "WARHEAD_MAGAZINE": "Warhead Magazine",
    "FUEL_DEPOT": "Fuel Depot", "GENERATOR_HALL": "Generator Hall", "AIR_HANDLING": "Air Handling",
    "PUMP_ROOM": "Pump Room", "WORKSHOP": "Workshop", "BUNKS": "Bunks", "STOCKPILE": "Stockpile",
    "MESS_GALLEY": "Mess Galley", "INFIRMARY": "Infirmary", "ARMORY": "Armory", "DECON_AIRLOCK": "Airlock",
    "GUARD_POST": "Guard",
}

MARKS = {  # kind -> (css color, shape)
    "LOOT_CHEST": ("#ffd23f", "sq"), "SPAWNER": ("#ff4d4d", "dia"), "MACHINE": ("#2fd0ff", "circ"),
}


def load() -> dict:
    if not DUMP.is_file():
        raise SystemExit(f"{DUMP} not found. Export it first: GREGGUNS_EXPORT_COMPLEX=\"$PWD/build/infographics/complex.json\" "
                         "./gradlew test --rerun --tests com.gregguns.tools.ComplexExportTest")
    return json.loads(DUMP.read_text(encoding="utf-8"))


def plan_with(d: dict, silos: int) -> dict:
    for p in d["plans"]:
        if p["siloCount"] == silos:
            return p
    raise SystemExit(f"the complex dump has no plan with {silos} silos")


def raster(d: dict, p: dict, px: int) -> Path:
    """The plan's walking level as a PNG, px pixels per block (north up, x across, z down)."""
    n = d["size"]

    def make():
        a = np.zeros((n, n), np.uint8)
        for x, col in enumerate(p["walk"]):
            a[x] = np.frombuffer(col.encode("ascii"), np.uint8) - 97
        pal = np.zeros((16, 3), np.uint8)
        for k, v in CELL.items():
            pal[k] = v
        rgb = pal[a]                      # indexed [x, z]
        for r in p["rooms"]:
            x0, z0, x1, z1 = r["box"]
            tint = np.array(ROLE_OF[r["kind"]][1], float)
            sub = a[x0:x1, z0:z1]
            mask = sub == 2
            base = rgb[x0:x1, z0:z1].astype(float)
            base[mask] = base[mask] * .30 + tint * .70
            rgb[x0:x1, z0:z1] = base.astype(np.uint8)
        im = Image.fromarray(np.transpose(rgb, (1, 0, 2)), "RGB")      # image rows are z
        return im.resize((n * px, n * px), Image.NEAREST)

    return K._cached("complexmap", dict(seed=p["seed"], px=px, v=1, size=n), make)


def _label(name: str, w: float, fs: float) -> list[str]:
    """One line if it fits in w pixels at font size fs, else split at the middle space."""
    if len(name) * fs * .5 <= w or " " not in name:
        return [name]
    words = name.split(" ")
    best = min(range(1, len(words)), key=lambda i: abs(len(" ".join(words[:i])) - len(" ".join(words[i:]))))
    return [" ".join(words[:best]), " ".join(words[best:])]


def overlay(d: dict, p: dict, px: int, labels: bool = True, markers: bool = True, fs: float = 12) -> str:
    """The SVG that sits on the raster: silo outlines, room names, chest/guard/machine marks, the ladder, the radar."""
    n = d["size"] * px
    out = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{n}" height="{n}" viewBox="0 0 {n} {n}" '
           f'style="position:absolute;left:0;top:0;font-family:\'Segoe UI\',\'Fira Sans\',\'Noto Sans\',sans-serif">']
    halo = 'paint-order="stroke" stroke="#0e1217" stroke-width="3.5" stroke-linejoin="round"'
    if markers:
        for m in p["marks"]:
            if m["kind"] not in MARKS:
                continue
            color, shape = MARKS[m["kind"]]
            cx, cz = (m["x"] + .5) * px, (m["z"] + .5) * px
            r = px * .62
            if shape == "sq":
                out.append(f'<rect x="{cx - r:.1f}" y="{cz - r:.1f}" width="{2 * r:.1f}" height="{2 * r:.1f}" fill="{color}" stroke="#0e1217" stroke-width="1.2"/>')
            elif shape == "dia":
                r *= 1.25
                out.append(f'<polygon points="{cx:.1f},{cz - r:.1f} {cx + r:.1f},{cz:.1f} {cx:.1f},{cz + r:.1f} {cx - r:.1f},{cz:.1f}" fill="{color}" stroke="#0e1217" stroke-width="1.2"/>')
            else:
                out.append(f'<circle cx="{cx:.1f}" cy="{cz:.1f}" r="{r:.1f}" fill="{color}" stroke="#0e1217" stroke-width="1.2"/>')
    for s in p["silos"]:
        x0, z0, x1, z1 = s["box"]
        out.append(f'<rect x="{x0 * px - 4}" y="{z0 * px - 4}" width="{(x1 - x0) * px + 8}" height="{(z1 - z0) * px + 8}" fill="none" stroke="#ff8a1f" stroke-width="3"/>')
        if labels:
            miss = "controller" if s["missing"] == "CONTROLLER" else "Energy Hatch"
            out.append(f'<text x="{(x0 + x1) / 2 * px:.1f}" y="{z1 * px + 24}" text-anchor="middle" font-size="{fs}" font-weight="700" fill="#ffb066" {halo}>Silo, no {miss}</text>')
    if labels:
        for r in p["rooms"]:
            x0, z0, x1, z1 = r["box"]
            name = NAMES[r["kind"]]
            if r["kind"] == "GUARD_POST":
                continue
            w = (x1 - x0) * px
            lines = _label(name, w, fs)
            cx, cz = (x0 + x1) / 2 * px, (z0 + z1) / 2 * px
            big = r["kind"] == "ASSEMBLY_BAY"
            f = fs * 1.35 if big else fs
            for i, ln in enumerate(lines):
                y = cz + (i - (len(lines) - 1) / 2) * f * 1.05 + f * .35
                out.append(f'<text x="{cx:.1f}" y="{y:.1f}" text-anchor="middle" font-size="{f:.1f}" font-weight="{800 if big else 700}" fill="#ffffff" {halo}>{ln.replace("&", "&amp;")}</text>')
    ex, ez = p["escape"]
    out.append(f'<rect x="{ex * px - 7}" y="{ez * px - 7}" width="{px + 14}" height="{px + 14}" fill="#22d36b" stroke="#0e1217" stroke-width="2"/>')
    if labels:
        out.append(f'<text x="{ex * px - 12}" y="{ez * px + px * .5 + fs * .35:.1f}" text-anchor="end" font-size="{fs}" font-weight="700" fill="#7dffae" {halo}>Escape ladder</text>')
    for rx, rz in p["radar"]:
        cx, cz = (rx + .5) * px, (rz + .5) * px
        out.append(f'<circle cx="{cx:.1f}" cy="{cz:.1f}" r="{px * 2.6:.1f}" fill="rgba(47,208,255,.22)" stroke="#2fd0ff" stroke-width="2.5" stroke-dasharray="6 4"/>')
        if labels:
            out.append(f'<text x="{cx:.1f}" y="{cz - px * 3.2:.1f}" text-anchor="middle" font-size="{fs}" font-weight="700" fill="#8fe6ff" {halo}>Radar station (surface)</text>')
    out.append("</svg>")
    return "".join(out)


if __name__ == "__main__":
    data = load()
    outdir = K.WORK / "complex-maps"
    outdir.mkdir(parents=True, exist_ok=True)
    for plan in data["plans"]:
        src = raster(data, plan, 4)
        Image.open(src).save(outdir / f"complex-{plan['siloCount']}-silo.png")
        print("wrote", outdir / f"complex-{plan['siloCount']}-silo.png")
