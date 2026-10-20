"""Gun inventory icons derived from the gun's own 3D model, so icon and model always agree.

    GREGGUNS_EXPORT_MODELS=<dir> ./gradlew test --tests '*ModelExportTest'
    python -X utf8 tools/asset-gen/gun_icons.py <dir> [--sheet out.png]

Side view of the default skin (own renderer, model_render.py), fitted into the
inner 14x14 of a 16x16 canvas, box-averaged, lifted so dark guns stay readable,
with a top-left bevel highlight and a 1 px dark outline (the style of icons.py).
Deterministic; no randomness. Writes credits/gun_icons.csv.
"""
import csv
import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import model_render as R  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets"
ITEMS = ASSETS / "gregguns/textures/items"
INNER = 14          # inner box; the outline takes the last pixel on each side
STRETCH = 2.1       # long guns are taller than true proportion so they read in a slot
OUTLINE = (22, 22, 30)


def render_side(model):
    """RGBA float array of the model's right (-Z) side at about 160 px wide, plus the transparent mask."""
    tex = np.array(Image.open(ASSETS / model["textures"][0].replace("gregguns:", "gregguns/")).convert("RGBA"), float)
    quads = [q for b in model["boxes"] for q in R.box_quads(b)]
    pts = np.array([p for q in quads for p in q[2]])
    r, dn, f = R.views()["right (-Z side)"]
    sx, sy = pts @ r, pts @ dn
    bnd = (sx.min(), sy.min(), sx.max(), sy.max())
    scale = 160.0 / max(sx.max() - sx.min(), sy.max() - sy.min(), 1e-6)
    img = R.raster(quads, tex, (r, dn, f), scale, 0, bnd)
    sil = R.raster(quads, tex, (r, dn, f), scale, 0, bnd, sil=True)[..., 0] < 128
    return np.clip(img, 0, 255), sil


def icon_from_model(model):
    img, sil = render_side(model)
    ys, xs = np.where(sil)
    img, sil = img[ys.min():ys.max() + 1, xs.min():xs.max() + 1], sil[ys.min():ys.max() + 1, xs.min():xs.max() + 1]
    h, w = sil.shape
    aspect = h / w
    if aspect <= 1:
        tw = INNER
        th = max(3, min(INNER, round(INNER * aspect * STRETCH)))
    else:
        th = INNER
        tw = max(3, min(INNER, round(INNER / aspect)))
    # box-average into th x tw cells
    ye = np.linspace(0, h, th + 1).astype(int)
    xe = np.linspace(0, w, tw + 1).astype(int)
    cell = np.zeros((th, tw, 3))
    cov = np.zeros((th, tw))
    for j in range(th):
        for i in range(tw):
            m = sil[ye[j]:max(ye[j + 1], ye[j] + 1), xe[i]:max(xe[i + 1], xe[i] + 1)]
            c = img[ye[j]:max(ye[j + 1], ye[j] + 1), xe[i]:max(xe[i + 1], xe[i] + 1)]
            cov[j, i] = m.mean()
            if m.any():
                # favour the brighter half so thin light details survive the average
                px = c[m]
                lum = px.sum(1)
                keep = px[lum >= np.median(lum)]
                cell[j, i] = 0.5 * px.mean(0) + 0.5 * keep.mean(0)
    solid = cov >= 0.35
    out = np.zeros((16, 16, 4), np.uint8)
    oy, ox = (16 - th) // 2, (16 - tw) // 2
    body = np.zeros((16, 16), bool)
    col = np.zeros((16, 16, 3))
    body[oy:oy + th, ox:ox + tw] = solid
    col[oy:oy + th, ox:ox + tw] = cell
    # lift dark colors and saturate a touch so black guns still show their form
    lum = col.mean(2, keepdims=True)
    col = lum + (col - lum) * 1.25
    col = (col * 0.85 + 34 - 110) * 1.3 + 110
    # top-left bevel: edge pixels facing up or left catch light, down or right fall off
    base = col.copy()
    for y in range(16):
        for x in range(16):
            if not body[y, x]:
                continue
            up = y == 0 or not body[y - 1, x]
            left = x == 0 or not body[y, x - 1]
            down = y == 15 or not body[y + 1, x]
            right = x == 15 or not body[y, x + 1]
            d = (18 if up else 0) + (10 if left else 0) - (16 if down else 0) - (8 if right else 0)
            base[y, x] = col[y, x] + d
    base = np.clip(base, 0, 255)
    for y in range(16):
        for x in range(16):
            if body[y, x]:
                out[y, x] = (*base[y, x].astype(np.uint8), 255)
    for y in range(16):
        for x in range(16):
            if body[y, x]:
                continue
            for dy, dx in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                yy, xx = y + dy, x + dx
                if 0 <= yy < 16 and 0 <= xx < 16 and body[yy, xx]:
                    out[y, x] = (*OUTLINE, 255)
                    break
    return Image.fromarray(out, "RGBA")


def gun_icon_names():
    """Icon names this generator owns (read back from its credits file); icons.py skips them."""
    f = ROOT / "tools/asset-gen/credits/gun_icons.csv"
    if not f.exists():
        return set()
    with open(f, encoding="utf-8", newline="") as h:
        return {r["asset"][5:] for r in csv.DictReader(h)}


def main():
    export = Path(sys.argv[1])
    names = [n for n in json.loads((export / "index.json").read_text())["models"] if (ITEMS / (n + ".png")).exists()]
    rows = []
    for n in names:
        model = json.loads((export / (n + ".json")).read_text())
        icon_from_model(model).save(ITEMS / (n + ".png"))
        rows.append(["icon." + n, "src/main/resources/assets/gregguns/textures/items/%s.png" % n,
                     "tools/asset-gen/gun_icons.py", 0,
                     "Side render of the 3D model + default skin (model_render.py), 14x14 box average, bevel, outline",
                     "MIT", "2026-10-19"])
    cdir = ROOT / "tools/asset-gen/credits"
    with open(cdir / "gun_icons.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["asset", "path", "generator", "seed", "method", "license", "date"])
        w.writerows(rows)
    print("wrote", len(names), "gun icons")


if __name__ == "__main__":
    main()
