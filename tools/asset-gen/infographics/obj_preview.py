"""Preview sheet for Wavefront OBJ models, drawn with the same strict reader the game's loader matches.

    python tools/asset-gen/infographics/obj_preview.py                      # the test ogive
    python tools/asset-gen/infographics/obj_preview.py --obj a.obj --tex a.png [--obj b.obj --tex b.png]
    python tools/asset-gen/infographics/obj_preview.py --out sheet.png

Each model gets a row: three views (3/4, side, top), its face count and size in blocks, and its texture.
A file Forge 1.7.10's WavefrontObject would reject stops the build with the offending line, so a sheet only
ever shows what the game can draw (see the Source note in render3d.obj_scene).

Output goes to the build folder, never into the repository: <GREGGUNS_ASSETS_OUT>/previews/obj.png when
that variable is set, else build/previews/obj.png. Shipped models are picked up from
src/main/resources/assets/gregguns/models/**/*.obj when a texture with the same name exists under
textures/models/ (or beside the OBJ).
"""

import argparse
import os
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))

import render3d as R  # noqa: E402

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[2]
ASSETS = REPO / "src/main/resources/assets/gregguns"
OUTLINE = (10, 12, 16, 230, 2)  # the rim guides use on dark models
VIEWS = (("3/4", 30, 25), ("side", 90, 0), ("top", 0, 70))
BG = (34, 38, 44, 255)
FG = (230, 232, 236, 255)


def default_out() -> Path:
    base = os.environ.get("GREGGUNS_ASSETS_OUT", "").strip()
    return (Path(base).expanduser() / "previews" if base else REPO / "build/previews") / "obj.png"


def discover() -> list:
    """(obj, texture) pairs for shipped OBJ models that have a texture to go with them."""
    pairs = []
    for obj in sorted((ASSETS / "models").rglob("*.obj")) if (ASSETS / "models").is_dir() else []:
        for tex in (obj.with_suffix(".png"), ASSETS / "textures/models" / (obj.stem + ".png")):
            if tex.is_file():
                pairs.append((obj, tex))
                break
    return pairs


def render_sheet(pairs, ppu=160) -> Image.Image:
    rows = []
    for obj, tex in pairs:
        scene = R.obj_scene(obj, tex)  # strict: raises ValueError on anything Forge would refuse
        lo, hi = scene.bounds()
        views = [R.trim(R.render(scene, yaw=y, pitch=p, ppu=ppu, outline=OUTLINE)) for _, y, p in VIEWS]
        size = "x".join(f"{hi[i] - lo[i]:.2f}" for i in range(3))
        rows.append((obj, tex, views, f"{len(scene.quads)} faces, {size} blocks"))
    pad, label_h, tex_w = 20, 22, 160
    row_h = [max(v.height for v in views) + label_h + pad for _, _, views, _ in rows]
    width = pad + max(sum(v.width + pad for v in views) for _, _, views, _ in rows) + tex_w + pad
    sheet = Image.new("RGBA", (width, sum(row_h) + pad), BG)
    d = ImageDraw.Draw(sheet)
    y = pad
    for (obj, tex, views, info), h in zip(rows, row_h):
        d.text((pad, y), f"{obj.name}   {info}", fill=FG)
        x = pad
        for (name, _, _), v in zip(VIEWS, views):
            sheet.alpha_composite(v, (x, y + label_h))
            d.text((x, y + label_h + v.height - 12), name, fill=(150, 156, 166, 255))
            x += v.width + pad
        t = Image.open(tex).convert("RGBA")
        sc = max(1, min(tex_w // t.width, 4)) if t.width <= tex_w else 1
        t = t.resize((min(tex_w, t.width * sc), min(tex_w, t.height * sc)), Image.NEAREST)
        sheet.alpha_composite(t, (width - pad - tex_w, y + label_h))
        y += h
    return sheet


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--obj", action="append", default=[], type=Path)
    ap.add_argument("--tex", action="append", default=[], type=Path)
    ap.add_argument("--out", type=Path)
    a = ap.parse_args()
    if len(a.obj) != len(a.tex):
        ap.error("give one --tex per --obj")
    pairs = list(zip(a.obj, a.tex)) or [(HERE / "testdata/ogive.obj", HERE / "testdata/ogive.png")] + discover()
    out = a.out or default_out()
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet = render_sheet(pairs)
    sheet.convert("RGB").save(out)
    covered = (np.array(sheet.convert("L")) != BG[0]).mean()
    assert covered > 0.02, "preview sheet is empty"
    print("wrote", out.resolve(), f"({len(pairs)} model(s))")


if __name__ == "__main__":
    main()
