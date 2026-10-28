"""What the CTM scene of the testing.ctm_casings scenario should look like, rendered from the shipped tiles through
ctmsel (the engine's tile choice). Compare with the dev client's shots (mp_check.py --angelica). The layouts mirror
CtmCasingsScenario; keep them in step.

    python -I tools/asset-gen/infographics/ctm_expect.py <out-dir>

Writes expect-wall.png, expect-plating.png and expect-ceiling.png (16x enlarged), each as the camera sees the scene,
and prints the tile number of every cell, so a mismatch can be reported precisely.
"""

from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image

import ctmsel as C

NORTH, DOWN = C.NORTH, C.DOWN
SCALE = 16


def tile_img(tex, n, rot=0):
    """rot: 0, or "mirror" (the tile flipped left to right)."""
    im = Image.open(tex.tiles[n]).convert("RGBA")
    return im.transpose(Image.FLIP_LEFT_RIGHT) if rot == "mirror" else im


def render(cells, face, project, rot, size, textures):
    """cells: {(x,y,z): key}; project(pos) -> (col,row) on screen; textures: key -> CtmTexture."""
    w, h = size
    img = Image.new("RGBA", (w * 16, h * 16), (60, 60, 60, 255))
    rows = {}
    for pos, key in cells.items():
        n = C.tile_for_face(cells, pos, face, key)
        c, r = project(pos)
        img.paste(tile_img(textures[key], n, rot), (c * 16, r * 16))
        rows.setdefault(r, {})[c] = n
    return img.resize((w * 16 * SCALE // 4, h * 16 * SCALE // 4), Image.NEAREST), rows


def main(out):
    out = Path(out)
    out.mkdir(parents=True, exist_ok=True)
    works = C.CtmTexture("works_casing")
    plating = [C.CtmTexture("bastion_plating_" + m) for m in ("iron", "steel", "aluminum", "carbon")]
    # wall: z = 7 seen from -z (yaw 0): screen right is -x, screen up is +y
    wall = {(x, y, 7): "w" for x in range(-3, 4) for y in range(5)
            if not (x == 3 and y == 4) and not (x == -3 and y == 0) and not (x == 0 and y == 2)}
    img, rows = render(wall, NORTH, lambda p: (3 - p[0], 4 - p[1]), 0, (7, 5), {"w": works})
    img.save(out / "expect-wall.png")
    print("wall tiles (row = top to bottom, col = screen left to right; '.' = controller hole or gap)")
    for r in range(5):
        print(" ".join("%2s" % rows.get(r, {}).get(c, ".") for c in range(7)))
    # plating: z = 9, x -6..5, y 0..2, meta by column group
    pl = {(x, y, 9): "p%d" % ((x + 6) // 3) for x in range(-6, 6) for y in range(3)}
    img, rows = render(pl, NORTH, lambda p: (5 - p[0], 2 - p[1]), 0, (12, 3), {"p%d" % i: plating[i] for i in range(4)})
    img.save(out / "expect-plating.png")
    print("plating tiles (left to right on screen: Carbon, Aluminum, Steel, Iron)")
    for r in range(3):
        print(" ".join("%2s" % rows.get(r, {}).get(c, ".") for c in range(12)))
    # ceiling: y = 6 seen from below with yaw 0, pitch -90: screen right is -x, screen up is -z (looking up from
    # facing +z tips the camera's up vector to -z). The bottom face uses the top face's axes (left = west, up = north),
    # so the texture's left sits at screen right: the tile appears mirrored left to right.
    ce = {(x, 6, z): "w" for x in range(-3, 4) for z in range(-2, 3)
          if (x, z) not in ((3, -2), (2, -2), (3, -1), (-1, 0))}
    img, rows = render(ce, DOWN, lambda p: (3 - p[0], p[2] + 2), "mirror", (7, 5), {"w": works})
    img.save(out / "expect-ceiling.png")
    print("ceiling tiles (as seen looking up; top of picture = -z)")
    for r in range(5):
        print(" ".join("%2s" % rows.get(r, {}).get(c, ".") for c in range(7)))


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "expect")
