"""Self-check for render3d.obj_scene: loads the hand-made ogive fixture and renders it.

Usage: python tools/asset-gen/infographics/check_obj.py [preview.png]
Exits non-zero if parsing or the render looks wrong. With a path it also writes a three-view preview.
"""

import sys
import tempfile
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))

import render3d as R  # noqa: E402

DATA = Path(__file__).resolve().parent / "testdata"
OUTLINE = (10, 12, 16, 230, 2)  # the rim guides use on dark models


HEADER = """v 0.0 0.0 0.0
v 1.0 0.0 0.0
v 1.0 1.0 0.0
v 0.0 1.0 0.0
v 0.5 2.0 0.0
vt 0.0 0.0
vt 1.0 0.0
vt 1.0 1.0
vt 0.0 1.0
vt 0.5 1.0
vn 0.0 0.0 1.0
"""


def _load(body: str, tex: Path, **kw):
    with tempfile.TemporaryDirectory() as d:
        p = Path(d) / "t.obj"
        p.write_text(body)
        return R.obj_scene(p, tex, **kw)


def check_parser(tex: Path):
    """Files Forge 1.7.10's WavefrontObject loads are read right; files it would reject are rejected."""
    ok = """# comment
mtllib x.mtl
o t
usemtl m
s off
""" + HEADER + """g quads
f 1/1/1 2/2/1 3/3/1 4/4/1
f 1/1 2/2 3/3 4/4
g tris
f 1//1 2//1 3//1
f 1 2 3
"""
    s = _load(ok, tex)
    assert len(s.quads) == 4, f"expected 4 faces, got {len(s.quads)}"
    # The first face's V=1 corner (vertex 3, vt 1 1) must land on texture row 0 (OBJ V runs bottom-up).
    assert abs(s.quads[0].uv[2, 1]) < 1e-9 and abs(s.quads[0].uv[0, 1] - 1.0) < 1e-9
    assert np.allclose(s.quads[2].v[3], s.quads[2].v[2]), "a triangle repeats its last corner"

    rejected = {
        "5-gon": HEADER + "f 1 2 3 4 5\n",
        "negative index": HEADER + "f -4 -3 -2 -1\n",
        "index past the end": HEADER + "f 1 2 9\n",
        "zero index": HEADER + "f 0 1 2\n",
        "mixed corner forms": HEADER + "f 1/1 2 3\n",
        "two-corner face": HEADER + "f 1 2\n",
        "tri and quad in one group": HEADER + "g a\nf 1 2 3\nf 1 2 3 4\n",
        "integer vertex": "v 0 0 0\nv 1.0 0.0 0.0\nv 0.0 1.0 0.0\nf 1 2 3\n",
        "four-part vertex": "v 0.0 0.0 0.0 1.0\nv 1.0 0.0 0.0\nv 0.0 1.0 0.0\nf 1 2 3\n",
        "exponent number": "v 1e-3 0.0 0.0\nv 1.0 0.0 0.0\nv 0.0 1.0 0.0\nf 1 2 3\n",
        "group name with a dash": HEADER + "g a-b\nf 1 2 3\n",
    }
    for name, body in rejected.items():
        try:
            _load(body, tex)
        except ValueError:
            continue
        raise AssertionError(f"strict mode accepted a file Forge rejects: {name}")
    # The lenient reader still takes what the strict one refuses.
    assert len(_load(HEADER + "f 1 2 3 4 5\nf -4 -3 -2 -1\n", tex, strict=False).quads) == 3 + 1


def main():
    tex = DATA / "ogive.png"
    check_parser(tex)
    scene = R.obj_scene(DATA / "ogive.obj", tex)
    lo, hi = scene.bounds()
    assert 1.5 < hi[1] - lo[1] < 1.7, "ogive should be about 1.6 blocks long"
    views = [R.trim(R.render(scene, yaw=y, pitch=p, ppu=160, outline=OUTLINE)) for y, p in ((30, 25), (90, 0), (0, 70))]
    for img in views:
        a = np.array(img.getchannel("A"))
        covered = (a > 40).mean()
        assert 0.2 < covered < 1.0 and a.max() == 255, f"implausible coverage {covered:.2f}"
        assert img.width > 40 and img.height > 40, "render is too small"
    px = np.array(views[0])[..., :3][np.array(views[0].getchannel("A")) > 200]
    assert len(np.unique(px.reshape(-1, 3), axis=0)) > 8, "texture does not show up in the render"
    if len(sys.argv) > 1:
        sheet = Image.new("RGBA", (sum(v.width for v in views) + 20 * (len(views) + 1), max(v.height for v in views) + 40),
                          (34, 38, 44, 255))
        x = 20
        for v in views:
            sheet.alpha_composite(v, (x, 20))
            x += v.width + 20
        sheet.save(sys.argv[1])
        print("wrote", sys.argv[1])
    print("obj_scene ok:", len(scene.quads), "faces")


if __name__ == "__main__":
    main()
