"""Self-check for the connected-texture path (ctmsel.py and the shipped CTM files).

Usage: python tools/asset-gen/infographics/check_ctm.py
Exits non-zero if a face picks a different tile than MCPatcher's rules give, or a shipped casing set is malformed.
The expected tiles below are written out by hand from the documented 47-tile layout: tile 0 isolated, 1 open on the
right, 3 open on the left, 36 open on top, 12 open on the bottom, 26 fully surrounded, 45 a notch at the top left.
"""

import sys
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))

import ctmsel as C  # noqa: E402
import render3d as R  # noqa: E402

EAST, WEST, UP, DOWN, SOUTH, NORTH = C.EAST, C.WEST, C.UP, C.DOWN, C.SOUTH, C.NORTH
ORIGIN = (0, 0, 0)

# face -> {direction of the one connected neighbor: tile}; the face is seen from outside, texture up as drawn.
ONE_NEIGHBOR = {
    UP: {EAST: 1, WEST: 3, NORTH: 36, SOUTH: 12},
    DOWN: {EAST: 1, WEST: 3, NORTH: 36, SOUTH: 12},   # the pack flips the bottom face to the top face's axes
    NORTH: {WEST: 1, EAST: 3, UP: 36, DOWN: 12},
    SOUTH: {EAST: 1, WEST: 3, UP: 36, DOWN: 12},
    WEST: {SOUTH: 1, NORTH: 3, UP: 36, DOWN: 12},
    EAST: {NORTH: 1, SOUTH: 3, UP: 36, DOWN: 12},
}


def add(a, b):
    return (a[0] + b[0], a[1] + b[1], a[2] + b[2])


def main() -> None:
    faces = list(ONE_NEIGHBOR)
    # 1. an isolated block shows tile 0 on every face; a block in front of the face does not matter
    for f in faces:
        assert C.tile_for_face({ORIGIN: "a"}, ORIGIN, f, "a") == 0, f
        assert C.tile_for_face({ORIGIN: "a", f: "a"}, ORIGIN, f, "a") == 0, ("in front", f)
    # 2. one neighbor: the open side follows each face's own left/right/up/down
    for f, cases in ONE_NEIGHBOR.items():
        for d, tile in cases.items():
            got = C.tile_for_face({ORIGIN: "a", d: "a"}, ORIGIN, f, "a")
            assert got == tile, (f, d, got, tile)
    # 3. a face with all eight in-plane neighbors shows the plain tile; without the up-left one, tile 45
    for f in faces:
        ring = {add(ORIGIN, o): "a" for o in C.neighbor_offsets(f)}
        cells = {ORIGIN: "a", **ring}
        assert C.tile_for_face(cells, ORIGIN, f, "a") == 26, f
        l, d, r, u = C.FACE_AXES[f]
        del cells[add(u, l)]
        assert C.tile_for_face(cells, ORIGIN, f, "a") == 45, f
    assert C.tile_for_face({ORIGIN: "a", **{add(ORIGIN, o): "a" for o in C.neighbor_offsets(UP)}}, ORIGIN, UP, "a") == 26
    # 4. another block, or the same block with other metadata (another key), does not connect
    for f, cases in ONE_NEIGHBOR.items():
        for d in cases:
            assert C.tile_for_face({ORIGIN: "a", d: "b"}, ORIGIN, f, "a") == 0, (f, d)
    # 5. every shipped set: properties, 47 tiles of 16x16, tile 0 is the base texture, one set per meta
    metas = {}
    sets = sorted(p.name for p in C.CTM_ROOT.iterdir() if p.is_dir())
    assert len(sets) == len(C.gen.CASINGS) and set(sets) == set(C.gen.CASINGS), sets
    for name in sets:
        t = C.CtmTexture(name)
        assert t.props["matchBlocks"] == C.gen.BLOCK and t.meta == C.gen.CASINGS[name][1], name
        assert t.meta not in metas, f"{name} and {metas.get(t.meta)} share meta {t.meta}"
        metas[t.meta] = name
        for tile in t.tiles:
            assert Image.open(tile).size == (16, 16), tile
        base = C.REPO / "src/main/resources/assets/gregguns/textures/blocks/casings" / f"{name}.png"
        assert np.array_equal(np.array(Image.open(base).convert("RGBA")), np.array(Image.open(t.base).convert("RGBA"))), name
    # 6. the voxel path uses it: two casings side by side, the left one is open on its right and the right one on its left
    t = C.CtmTexture("works_casing")
    scene = R.voxel_scene({(0, 0, 0): "w", (1, 0, 0): "w"}, {"w": t})
    tops = [q for q in scene.quads if len({tuple(v)[1] for v in q.v}) == 1 and q.v[0][1] == 1.0]
    got = {int(q.v[:, 0].min()): scene.textures[q.tex] for q in tops}
    for x, tile in ((0, 1), (1, 3)):
        assert np.array_equal(got[x], np.array(Image.open(t.tiles[tile]).convert("RGBA"))), x
    print("ctm self-check ok:", ", ".join(sets))


if __name__ == "__main__":
    main()
