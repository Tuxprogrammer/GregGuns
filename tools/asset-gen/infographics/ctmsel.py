"""Which connected-texture tile a casing face shows, read from the shipped CTM files.

The mod ships MCPatcher-format connected textures for the base casings (assets/gregguns/mcpatcher/ctm/<name>/). In the
pack, Angelica's bundled MCPatcherForge picks a tile per rendered face; this module repeats that choice for the
voxel scenes, reading the same properties files and tile images from src/main/resources, so a sheet shows what the
game shows and cannot drift from the shipped art.

What the renderer does (Angelica 2.2.28, evidence only; nothing is copied):
- A face looks at the eight blocks around it in the face's own plane (left, down-left, down, down-right, right,
  up-right, up, up-left). It does not look at what is in front of them (innerSeams defaults to false).
- A neighbor counts when it is the same block with the same metadata (connect=block, metadata=N).
- The six faces have their own left/down/right/up (FACE_AXES below). The bottom face uses the top face's axes: the
  engine flips it unless Hodgepodge's fixBottomFaceUV is on, and the pack ships it off.
- The eight bits pick one of the 47 tiles; the held and inventory icon is tile 0.
Sources:
  https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/prupe/mcpatcher/ctm/BlockOrientation.java#L16-L30
  https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/prupe/mcpatcher/ctm/TileOverride.java#L407-L450
  https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/prupe/mcpatcher/ctm/TileOverrideImpl.java#L56-L63
  https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/prupe/mcpatcher/ctm/RenderBlockState.java#L97-L111
  (the pack's hodgepodge.cfg: B:fixBottomFaceUV=false)
"""

from __future__ import annotations

import hashlib
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[2]
CTM_ROOT = REPO / "src/main/resources/assets/gregguns/mcpatcher/ctm"
sys.path.insert(0, str(HERE.parent))   # the tile layout lives with the generator

import ctm as gen  # noqa: E402

EAST, WEST, UP, DOWN, SOUTH, NORTH = (1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)

# face normal -> (left, down, right, up) as seen looking at the face from outside, as the texture is laid on it.
FACE_AXES = {
    UP: (WEST, SOUTH, EAST, NORTH),
    DOWN: (WEST, SOUTH, EAST, NORTH),     # flipped to the top face's axes (see the module docstring)
    NORTH: (EAST, DOWN, WEST, UP),
    SOUTH: (WEST, DOWN, EAST, UP),
    WEST: (NORTH, DOWN, SOUTH, UP),
    EAST: (SOUTH, DOWN, NORTH, UP),
}


def _add(a, b):
    return (a[0] + b[0], a[1] + b[1], a[2] + b[2])


def neighbor_offsets(normal):
    """The eight neighbor offsets in MCPatcher's bit order: L, DL, D, DR, R, UR, U, UL."""
    l, d, r, u = FACE_AXES[normal]
    return [l, _add(l, d), d, _add(d, r), r, _add(r, u), u, _add(u, l)]


def tile_for_face(cells: dict, pos, normal, key) -> int:
    """The tile a face of the block at pos shows. cells maps position -> block key; two cells connect when their
    keys are equal (the scenes use one key per block and metadata)."""
    bits = [cells.get(_add(pos, o)) == key for o in neighbor_offsets(normal)]
    return gen.tile_index(*bits)


def parse_properties(path: Path) -> dict:
    props = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line and not line.startswith("#") and "=" in line:
            k, v = line.split("=", 1)
            props[k.strip()] = v.strip()
    return props


class CtmTexture:
    """A casing whose faces pick a tile by their neighbors. base is tile 0, the held icon."""

    def __init__(self, name: str):
        self.name = name
        self.dir = CTM_ROOT / name
        self.props = parse_properties(self.dir / f"{name}.properties")
        p = self.props
        assert p.get("method") == "ctm" and p.get("tiles") == "0-46" and p.get("connect", "block") == "block", p
        self.meta = int(p.get("metadata", 0))   # a plain block (the Silo Complex Wall) has no metadata line
        self.tiles = [self.dir / f"{i}.png" for i in range(47)]
        self.base = self.tiles[0]
        h = hashlib.sha1()
        for t in self.tiles:
            h.update(t.read_bytes())
        h.update((self.dir / f"{name}.properties").read_bytes())
        self.digest = h.hexdigest()[:12]

    def __repr__(self):
        return f"CtmTexture({self.name},{self.digest})"
