"""One-shot conversion of the missile body models we take from HBM's Nuclear Tech Mod (LGPL-3.0) into ours.

    python -I tools/asset-gen/icbm/convert_ntm_missiles.py <folder with the downloaded .obj/.png> [<out root>]

NTM builds a missile from separate parts (thruster, fuselage, stabilizer fins), each with its own texture. The
game loads one OBJ with one texture per model (D43), so for each tier this script:
  - stacks the parts (thruster at the bottom, fuselage on top of it, fins around the fuselage's foot),
  - scales them uniformly so the fuselage's top ring matches the matching warhead model's base ring,
  - centers the footprint at (0.5, 0.5) of its block with the fins' tips at the block edge or inside it, base at y = 0,
  - packs the three part textures into one atlas (256x128, nearest placement, no resampling) and remaps the UVs,
  - writes Forge-strict OBJ: 6-decimal vertices, one group named "body", triangles only, v/vt/vn corners.
The nose seat (where the warhead's y = 0 sits) is the model's top, printed for each tier. Every change is listed
in docs/assets/CREDITS.md as a modification of the upstream files.
"""

import sys
from pathlib import Path

from PIL import Image

# tier: (out name, scale, thruster, fuselage, stabilizer)
TABLE = {
    "missile_body_t1": ("missile_body_t1", 0.5, "mp_t_10_kerosene", "mp_f_10_kerosene", "mp_s_10_flat"),
    "missile_body_t2": ("missile_body_t2", 0.4, "mp_t_10_kerosene", "mp_f_10_long_kerosene", "mp_s_10_cruise"),
}
TEX_DIR = {"mp_t": "thrusters", "mp_f": "fuselages", "mp_s": "stability"}
ATLAS = (256, 128)
THRUSTER_H = 1.0  # raw units; the fuselage stands on the thruster


def part_png(src: Path, name: str) -> Path:
    return src / f"{name}.png"


def convert(src: Path, scale: float, parts) -> tuple[str, Image.Image, float]:
    atlas = Image.new("RGBA", ATLAS, (0, 0, 0, 0))
    x0 = 0
    vs, vts, vns, faces = [], [], [], []
    top = 0.0
    for idx, name in enumerate(parts):
        im = Image.open(part_png(src, name)).convert("RGBA")
        w, h = im.size
        atlas.paste(im, (x0, 0))
        rows = (src / f"{name}.obj").read_text().splitlines()
        # fuselage (idx 1) is lifted onto the thruster; thruster and fins stay where NTM draws them
        dy = THRUSTER_H if idx == 1 else 0.0
        vbase, tbase, nbase = len(vs), len(vts), len(vns)
        for raw in rows:
            p = raw.split()
            if not p:
                continue
            if p[0] == "v":
                x, y, z = float(p[1]), float(p[2]) + dy, float(p[3])
                vs.append((x * scale + 0.5, y * scale, z * scale + 0.5))
                if idx == 1:
                    top = max(top, y * scale)
            elif p[0] == "vt":
                u, v = float(p[1]), float(p[2])
                # image row of this uv inside the tile, then inside the atlas (tile pasted at the top-left)
                vts.append(((x0 + u * w) / ATLAS[0], 1.0 - ((1.0 - v) * h) / ATLAS[1]))
            elif p[0] == "vn":
                vns.append(tuple(float(c) for c in p[1:4]))
            elif p[0] == "f":
                assert len(p) == 4, "triangles only"
                cs = []
                for c in p[1:]:
                    a, b, n = (int(t) for t in c.split("/"))
                    cs.append("%d/%d/%d" % (a + vbase, b + tbase, n + nbase))
                faces.append("f " + " ".join(cs))
        x0 += w
    out = ["# Converted from HBM's Nuclear Tech Mod (LGPL-3.0); parts stacked, scaled, recentered, one atlas. See CREDITS.md", "g body"]
    out += ["v %.6f %.6f %.6f" % v for v in vs]
    out += ["vt %.6f %.6f" % t for t in vts]
    out += ["vn %.6f %.6f %.6f" % n for n in vns]
    return "\n".join(out + faces) + "\n", atlas, top


def main():
    src = Path(sys.argv[1])
    root = Path(sys.argv[2]) if len(sys.argv) > 2 else Path("src/main/resources/assets/gregguns")
    mo, to = root / "models/thirdparty/ntm", root / "textures/models/thirdparty/ntm"
    for name, (out, scale, th, fu, st) in TABLE.items():
        obj, atlas, top = convert(src, scale, (th, fu, st))
        (mo / f"{out}.obj").write_text(obj)
        atlas.save(to / f"{out}.png")
        print("wrote", out, "nose seat y = %.4f" % top)


if __name__ == "__main__":
    main()
