"""One-shot conversion of the radar dish model we take from HBM's Nuclear Tech Mod (LGPL-3.0) into ours.

    python -I tools/asset-gen/icbm/convert_ntm_radar.py <folder with the downloaded radar.obj and radar_dish.png> [<out root>]

NTM's radar.obj holds two objects, "Base" and "Dish". We keep only "Dish" (the GT casings are our base). The vertices
move so the dish turns about the origin (NTM translates the dish 0.125 along x after it rotates, which puts the pivot
at x = 0.125) and its underside sits at y = 0 (NTM's dish starts 1.125 above its base). The group is renamed "dish" and
every face is rewritten as a v/vt/vn triangle, which is Forge's strict grammar. Texture coordinates and normals are
copied; only the vertices and the faces the dish uses are kept, renumbered. The texture is copied unchanged.
Every change is listed in docs/assets/CREDITS.md as a modification of the upstream files.
"""

import sys
from pathlib import Path

PIVOT_X = 0.125
BASE_TOP = 1.125


def convert(src: Path) -> str:
    v, vt, vn = [], [], []
    faces = {}  # object name -> list of corner lists
    cur = None
    for raw in src.read_text().splitlines():
        p = raw.split()
        if not p:
            continue
        if p[0] == "o":
            cur = p[1]
            faces[cur] = []
        elif p[0] == "v":
            v.append([float(x) for x in p[1:4]])
        elif p[0] == "vt":
            vt.append([float(x) for x in p[1:3]])
        elif p[0] == "vn":
            vn.append([float(x) for x in p[1:4]])
        elif p[0] == "f" and cur:
            assert len(p) == 4, "triangles only"
            faces[cur].append([tuple(int(i) for i in c.split("/")) for c in p[1:]])
    dish = faces["Dish"]
    vmap, tmap, nmap = {}, {}, {}
    for tri in dish:
        for a, b, c in tri:
            vmap.setdefault(a, len(vmap) + 1)
            tmap.setdefault(b, len(tmap) + 1)
            nmap.setdefault(c, len(nmap) + 1)
    out = ["# Converted from HBM's Nuclear Tech Mod (LGPL-3.0); the Dish object only, recentered. See CREDITS.md", "g dish"]
    for old in sorted(vmap, key=vmap.get):
        x, y, z = v[old - 1]
        out.append("v %.6f %.6f %.6f" % (x - PIVOT_X, y - BASE_TOP, z))
    for old in sorted(tmap, key=tmap.get):
        out.append("vt %.6f %.6f" % tuple(vt[old - 1]))
    for old in sorted(nmap, key=nmap.get):
        out.append("vn %.6f %.6f %.6f" % tuple(vn[old - 1]))
    for tri in dish:
        out.append("f " + " ".join("%d/%d/%d" % (vmap[a], tmap[b], nmap[c]) for a, b, c in tri))
    return "\n".join(out) + "\n"


def main():
    src = Path(sys.argv[1])
    root = Path(sys.argv[2]) if len(sys.argv) > 2 else Path("src/main/resources/assets/gregguns")
    mo, to = root / "models/thirdparty/ntm", root / "textures/models/thirdparty/ntm"
    mo.mkdir(parents=True, exist_ok=True)
    to.mkdir(parents=True, exist_ok=True)
    (mo / "radar_dish.obj").write_text(convert(src / "radar.obj"))
    (to / "radar_dish.png").write_bytes((src / "radar_dish.png").read_bytes())
    print("wrote radar_dish")


if __name__ == "__main__":
    main()
