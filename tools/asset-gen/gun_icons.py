"""Gun inventory icons derived from the gun's own 3D model, so icon and model always agree.

    GREGGUNS_EXPORT_MODELS=<dir> ./gradlew test --tests '*ModelExportTest'
    python -X utf8 tools/asset-gen/gun_icons.py <dir> [--only name ...]

Side view of the default skin (own renderer, model_render.py), true proportions.
Long guns (length/height >= 2.2) are tilted 45 degrees, muzzle up-right; pistols stay
side-on; akimbo icons are two offset copies of the single gun. Fitted into the
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
STRETCH = 2.1       # side-on guns are a little taller than true proportion so they read in a slot
DIAGONAL_ASPECT = 2.2   # length / height at or above this lies on the 45 degree diagonal
OUTLINE = (22, 22, 30)
EXPORT = None
AKIMBO = {"akimbo_pistols": "pistol_9mm", "akimbo_revolvers": "revolver", "akimbo_mac10s": "mac10",
          "akimbo_golden_revolvers": "golden_revolver", "akimbo_deatomizer_pistols": "deatomizer_pistol"}


def render_view(model, diagonal):
    """(rgb, mask) of the model's right (-Z) side, true proportions; the diagonal view tilts the muzzle up-right 45 degrees."""
    tex = np.array(Image.open(ASSETS / model["textures"][0].replace("gregguns:", "gregguns/")).convert("RGBA"), float)
    quads = [q for b in model["boxes"] for q in R.box_quads(b)]
    pts = np.array([p for q in quads for p in q[2]])
    r, dn, f = R.views()["right (-Z side)"]
    if diagonal:
        k = 2 ** -0.5
        r, dn = (r + dn) * k, (dn - r) * k   # muzzle (+r) goes right and up; depth axis unchanged
    sx, sy = pts @ r, pts @ dn
    bnd = (sx.min(), sy.min(), sx.max(), sy.max())
    scale = 224.0 / max(sx.max() - sx.min(), sy.max() - sy.min(), 1e-6)
    img = np.clip(R.raster(quads, tex, (r, dn, f), scale, 0, bnd), 0, 255)
    sil = R.raster(quads, tex, (r, dn, f), scale, 0, bnd, sil=True)[..., 0] < 128
    return img, sil


def side_aspect(model):
    _, sil = render_view(model, False)
    ys, xs = np.where(sil)
    return (xs.max() - xs.min() + 1) / (ys.max() - ys.min() + 1)


def cells(model, inner, diagonal, stretch):
    """Fit the model into at most inner x inner cells: (solid mask, rgb). Thin parts keep at least 1 cell."""
    img, sil = render_view(model, diagonal)
    ys, xs = np.where(sil)
    img = img[ys.min():ys.max() + 1, xs.min():xs.max() + 1]
    sil = sil[ys.min():ys.max() + 1, xs.min():xs.max() + 1]
    h, w = sil.shape
    if w >= h:
        tw, th = inner, max(3, min(inner, round(inner * h / w * stretch)))
    else:
        th, tw = inner, max(3, min(inner, round(inner * w / h)))
    ye = np.linspace(0, h, th + 1).astype(int)
    xe = np.linspace(0, w, tw + 1).astype(int)
    col = np.zeros((th, tw, 3))
    cov = np.zeros((th, tw))
    for j in range(th):
        for i in range(tw):
            sl = (slice(ye[j], max(ye[j + 1], ye[j] + 1)), slice(xe[i], max(xe[i + 1], xe[i] + 1)))
            m = sil[sl]
            cov[j, i] = m.mean()
            if m.any():
                px = img[sl][m]
                lum = px.sum(1)
                keep = px[lum >= np.median(lum)]
                col[j, i] = 0.5 * px.mean(0) + 0.5 * keep.mean(0)
    # thin barrels: low coverage still counts when the cell is the only one in its row or column
    solid = cov >= (0.2 if diagonal else 0.35)
    return solid, col


def grade(col):
    """Lift dark colors and add contrast so black guns still show their form."""
    lum = col.mean(2, keepdims=True)
    col = lum + (col - lum) * 1.25
    return (col * 0.85 + 34 - 110) * 1.3 + 110


def paint(canvas, solid, col, ox, oy, dim=1.0):
    """Paint a layer: its outline ring first (over anything below), then bevel-shaded body."""
    body = np.zeros((16, 16), bool)
    rgb = np.zeros((16, 16, 3))
    th, tw = solid.shape
    body[oy:oy + th, ox:ox + tw] = solid
    rgb[oy:oy + th, ox:ox + tw] = col
    for y in range(16):
        for x in range(16):
            if body[y, x]:
                continue
            for dy, dx in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                yy, xx = y + dy, x + dx
                if 0 <= yy < 16 and 0 <= xx < 16 and body[yy, xx]:
                    canvas[y, x] = (*OUTLINE, 255)
                    break
    for y in range(16):
        for x in range(16):
            if not body[y, x]:
                continue
            up = y == 0 or not body[y - 1, x]
            left = x == 0 or not body[y, x - 1]
            down = y == 15 or not body[y + 1, x]
            right = x == 15 or not body[y, x + 1]
            d = (18 if up else 0) + (10 if left else 0) - (16 if down else 0) - (8 if right else 0)
            canvas[y, x] = (*np.clip((rgb[y, x] + d) * dim, 0, 255).astype(np.uint8), 255)


def icon_from_model(model, name=""):
    canvas = np.zeros((16, 16, 4), np.uint8)
    if name in AKIMBO:
        single = json.loads((EXPORT / (AKIMBO[name] + ".json")).read_text())
        solid, col = cells(single, 11, False, STRETCH)
        col = grade(col)
        th, tw = solid.shape
        paint(canvas, solid, col, 15 - tw, 0, 0.75)          # back gun, upper right, darker
        paint(canvas, solid, col, 0, 16 - th)  # front gun, lower left
    else:
        diagonal = side_aspect(model) >= DIAGONAL_ASPECT
        solid, col = cells(model, INNER, diagonal, 1.0 if diagonal else STRETCH)
        th, tw = solid.shape
        paint(canvas, solid, grade(col), (16 - tw) // 2, (16 - th) // 2)
    return Image.fromarray(canvas, "RGBA")


def gun_icon_names():
    """Icon names this generator owns (read back from its credits file); icons.py skips them."""
    f = ROOT / "tools/asset-gen/credits/gun_icons.csv"
    if not f.exists():
        return set()
    with open(f, encoding="utf-8", newline="") as h:
        return {r["asset"][5:] for r in csv.DictReader(h)}


def main():
    global EXPORT
    export = EXPORT = Path(sys.argv[1])
    only = sys.argv[sys.argv.index("--only") + 1:] if "--only" in sys.argv else []
    if only:
        # New gun: draw just these icons and upsert their credit rows; every other icon stays untouched.
        names = [n for n in only if (export / (n + ".json")).exists()]
    else:
        names = [n for n in json.loads((export / "index.json").read_text())["models"] if (ITEMS / (n + ".png")).exists()]
    rows = []
    for n in names:
        model = json.loads((export / (n + ".json")).read_text())
        icon_from_model(model, n).save(ITEMS / (n + ".png"))
        rows.append(["icon." + n, "src/main/resources/assets/gregguns/textures/items/%s.png" % n,
                     "tools/asset-gen/gun_icons.py", 0,
                     "Side render of the 3D model + default skin (model_render.py), 14x14 box average, bevel, outline",
                     "MIT", "2026-10-19"])
    cdir = ROOT / "tools/asset-gen/credits"
    if only:
        with open(cdir / "gun_icons.csv", encoding="utf-8", newline="") as f:
            old = list(csv.reader(f))
        keep = [r for r in old[1:] if r[0] not in {x[0] for x in rows}]
        rows = keep + rows
    with open(cdir / "gun_icons.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["asset", "path", "generator", "seed", "method", "license", "date"])
        w.writerows(rows)
    print("wrote", len(names), "gun icons")


if __name__ == "__main__":
    main()
