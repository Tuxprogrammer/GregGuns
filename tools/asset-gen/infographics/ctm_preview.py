"""Preview sheet for the casing connected textures: build/previews/ctm.png (never committed).

Usage: python tools/asset-gen/infographics/ctm_preview.py [out.png]
One row per connected casing: an isolated block, a 2x1 strip, a 3x3 wall, an L, a wall with a hole, and a 3D corner
(floor plus two walls) through the same voxel path the infographics use. The flat shapes are the top faces of
blocks laid on the ground, so they use the same tile choice the game makes.
"""

import sys
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))

import ctmsel as C  # noqa: E402
import render3d as R  # noqa: E402

SHAPES = {
    "isolated": ["#"],
    "2x1": ["##"],
    "3x3": ["###", "###", "###"],
    "L": ["#..", "#..", "###"],
    "hole": ["####", "#..#", "#..#", "####"],
    "plus": [".#.", "###", ".#."],
}
Z = 5   # preview scale


def flat(tex: C.CtmTexture, rows) -> Image.Image:
    cells = {(x, 0, z): "a" for z, row in enumerate(rows) for x, ch in enumerate(row) if ch == "#"}
    im = Image.new("RGBA", (16 * len(rows[0]), 16 * len(rows)), (0, 0, 0, 0))
    for (x, _, z) in cells:
        tile = Image.open(tex.tiles[C.tile_for_face(cells, (x, 0, z), C.UP, "a")]).convert("RGBA")
        im.alpha_composite(tile, (16 * x, 16 * z))
    return im.resize((im.width * Z // 2, im.height * Z // 2), Image.NEAREST)


def corner(tex: C.CtmTexture) -> Image.Image:
    cells = {}
    for x in range(4):
        for z in range(4):
            cells[(x, 0, z)] = "a"
    for y in range(1, 4):
        for i in range(4):
            cells[(i, y, 0)] = "a"
            cells[(0, y, i)] = "a"
    scene = R.voxel_scene(cells, {"a": tex})
    return R.trim(R.render(scene, yaw=35, pitch=28, ppu=34, ss=2), 2)


def main() -> None:
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else C.REPO / "build/previews/ctm.png"
    names = sorted(p.name for p in C.CTM_ROOT.iterdir() if p.is_dir())
    rows = []
    for name in names:
        tex = C.CtmTexture(name)
        parts = [flat(tex, s) for s in SHAPES.values()] + [corner(tex)]
        rows.append((name, parts))
    pad = 14
    width = max(sum(p.width for p in parts) + pad * (len(parts) + 1) for _, parts in rows)
    height = sum(max(p.height for p in parts) + 2 * pad + 12 for _, parts in rows)
    sheet = Image.new("RGBA", (width, height), (40, 44, 52, 255))
    d = ImageDraw.Draw(sheet)
    y = 0
    for name, parts in rows:
        d.text((pad, y + 2), name, fill=(220, 224, 230, 255))
        h = max(p.height for p in parts)
        x = pad
        for p in parts:
            sheet.alpha_composite(p, (x, y + 14 + pad))
            x += p.width + pad
        y += h + 2 * pad + 12
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.convert("RGB").save(out)
    print("wrote", out)


if __name__ == "__main__":
    main()
