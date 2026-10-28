"""One-shot conversion of the warhead models we take from HBM's Nuclear Tech Mod (LGPL-3.0) into ours.

    python -I tools/asset-gen/icbm/convert_ntm_warheads.py <folder with the downloaded .obj/.png> [<out root> [<entry names>]]

Reads the upstream files (Blender 2.76 OBJ exports, already Forge-strict), then for each entry in TABLE:
  - scales the vertices uniformly, moves the footprint center to its block-local spot (0.5, 0.5 for a 1x1
    footprint, 1.0, 1.0 for 2x2) with the base at y = 0, and rewrites every line in Forge's grammar
    (6-decimal vertices, one group named "warhead", triangles only, v/vt/vn corners);
  - optionally rotates the texture's hue (antimatter, red matter) and paints out the red matter's NTM lettering;
    nothing else about the pixels changes.
Every change is listed in docs/assets/CREDITS.md as a modification of the upstream files.
"""

import colorsys
import sys
from pathlib import Path

import numpy as np
from PIL import Image

# out name: (upstream name, scale, footprint (cells per side), hue shift in degrees or None)
TABLE = {
    "warhead_t1": ("mp_w_10_he", 0.5, 1, None),
    "warhead_t2": ("mp_w_10_incendiary", 0.4, 1, None),
    "warhead_t3": ("mp_w_10_he", 1.0, 1, None),
    "warhead_nuclear": ("mp_w_10_nuclear_large", 0.8, 1, None),
    "warhead_antimatter": ("mp_w_15_nuclear", 1.0, 2, 140),
    "warhead_redmatter": ("mp_w_15_balefire", 1.2, 2, -120),
    # The T4 and T5 casings carry antimatter and red matter in a 1x1x2 column, so they get the same models, smaller.
    "warhead_t4": ("mp_w_15_nuclear", 0.55, 1, 140),
    "warhead_t5": ("mp_w_15_balefire", 0.6, 1, -120),
    # The T3 EMP warhead: NTM's cloud warhead (magenta bands, no lettering) on its taint-warhead model.
    "warhead_emp": ("mp_w_10_taint", 0.8, 1, None),
    # The T3 exothermic and endothermic warheads: NTM's incendiary (orange and black bands) and nitrogen (the "N2" tank)
    # 15-series warheads, each with its own model and skin and no "TNT" lettering. Scale 0.64 keeps their 1.5-wide
    # barrels inside the casing's 0.96-wide box.
    "warhead_exothermic": ("mp_w_15_incendiary", 0.64, 1, None),
    "warhead_endothermic": ("mp_w_15_n2", 0.64, 1, None),
}

# Where a texture comes from when it is not the model's own name (NTM draws its cloud warhead on the taint model).
TEXTURE_FROM = {"warhead_emp": "mp_w_10_cloud"}


def convert_obj(src: Path, scale: float, cells: int) -> str:
    out = ["# Converted from HBM's Nuclear Tech Mod (LGPL-3.0); scaled and recentered. See CREDITS.md", "g warhead"]
    vt, vn, faces = [], [], []
    for raw in src.read_text().splitlines():
        p = raw.split()
        if not p:
            continue
        if p[0] == "v":
            out.append("v %.6f %.6f %.6f" % (float(p[1]) * scale + cells / 2, float(p[2]) * scale, float(p[3]) * scale + cells / 2))
        elif p[0] == "vt":
            vt.append("vt %.6f %.6f" % (float(p[1]), float(p[2])))
        elif p[0] == "vn":
            vn.append("vn %.6f %.6f %.6f" % tuple(float(x) for x in p[1:4]))
        elif p[0] == "f":
            assert len(p) == 4, "triangles only"
            faces.append("f " + " ".join(p[1:]))
    return "\n".join(out + vt + vn + faces) + "\n"


def paint_out_label(im: np.ndarray) -> None:
    """Hand edit: the red matter (balefire) texture's lore lettering is NTM's, so rows 65-71, which carry only
    that lettering below the last stripe, are overwritten with row 58 of the dark casing beside them."""
    im[65:72] = im[58]


def shift_hue(src: Path, dst: Path, deg: float, label_out: bool = False) -> None:
    im = np.array(Image.open(src).convert("RGBA"))
    if label_out:
        paint_out_label(im)
    rgb = im[..., :3] / 255.0
    flat = rgb.reshape(-1, 3)
    res = np.empty_like(flat)
    for i, (r, g, b) in enumerate(flat):
        h, s, v = colorsys.rgb_to_hsv(r, g, b)
        res[i] = colorsys.hsv_to_rgb((h + deg / 360.0) % 1.0, s, v)
    im[..., :3] = np.round(res.reshape(rgb.shape) * 255).astype(np.uint8)
    Image.fromarray(im, "RGBA").save(dst)


def main():
    src = Path(sys.argv[1])
    root = Path(sys.argv[2]) if len(sys.argv) > 2 else Path("src/main/resources/assets/gregguns")
    mo, to = root / "models/thirdparty/ntm", root / "textures/models/thirdparty/ntm"
    mo.mkdir(parents=True, exist_ok=True)
    to.mkdir(parents=True, exist_ok=True)
    only = sys.argv[3:]  # optional: convert just these entries
    for name, (up, scale, cells, hue) in TABLE.items():
        if only and name not in only:
            continue
        tex = TEXTURE_FROM.get(name, up)
        (mo / f"{name}.obj").write_text(convert_obj(src / f"{up}.obj", scale, cells))
        if hue is None:
            (to / f"{name}.png").write_bytes((src / f"{tex}.png").read_bytes())
        else:
            shift_hue(src / f"{tex}.png", to / f"{name}.png", hue, label_out=(up == "mp_w_15_balefire"))
        print("wrote", name)


if __name__ == "__main__":
    main()
