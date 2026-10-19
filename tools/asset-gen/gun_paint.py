"""Geometry-driven gun texture painter: geometry first, texture second.

The older `models.py` paints a generic part archetype and the Java model is then fitted to that atlas. This tool turns
that around. A gun is designed as boxes (`gun-paint/<gun>.layout.json`); the boxes are packed into a sheet, the Java
BoxSpec rows are generated from them, and every box face is then painted for what the box is (a role map,
`gun-paint/<gun>.json`). Original procedural art: nothing here reads or imitates Techguns assets.

Sub-commands (run from the repo root, `python -I -X utf8 tools/asset-gen/gun_paint.py ...`):

  pack  <gun> [--repack] [--java FILE]
        Reads gun-paint/<gun>.layout.json. Assigns UV origins (shelf packing, stable: existing "uv" entries are kept
        unless --repack) and writes them back into the layout. Writes an exporter-style JSON model to
        build/gun-paint/<gun>.json (for the offline renderer). With --java, rewrites the region between
        "// BEGIN GENERATED BOXES" and "// END GENERATED BOXES" in that profile with BoxSpec rows.
  paint <gun> [--model FILE]
        Paints the sheet, and its variants, from an exported model JSON (default: the one `pack` wrote; the real
        export of `ModelExportTest` has the same shape) plus the role map. Writes the PNGs named in the role map.

Layout file: {"gun", "texture": [w, h], "scale", "bodyRotationYDegrees", "boxes": [{"name", "size": [w, h, d],
"pos": [x, y, z], "part", "pivot", "tilt", "uv"}]}. Positions are texels in model space (x toward the muzzle, y down).

Role map (`gun-paint/<gun>.json`): {"gun", "textures": [{"file": "handcannon.png", "variant": null | {...}}],
"roles": {role: {"material", "base", ...}}, "boxes": {name: {"role", "grain", "wear", "painters": [...]}}}.
Materials: steel (flat, edge highlights, faint scratches), wood (grain streaks along the box's grain axis, dark end
grain), flat. Painters (each takes "faces": [...] and its own keys): rect, bore, bands, gradient, tone, seam, inset.
A steel role may set "scratches": false and "noise": 0.04 for a satin finish. A box may be tilted about Z ("tilt") or
about its long axis X ("tiltX", for a diamond bar), both about "pivot"; a size with a 0 is a zero-thickness plane.
A variant (camo or recolor) overrides role colors: {"roles": {role: {"base": ..}}} or "camo" with a palette.
"""
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from modeltex import Tex, box_uv, hx, mul, rect, rng_for, vnoise  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
PAINT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "gun-paint")
TEX_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", "gregguns", "textures", "models", "guns")
FACES = ("top", "bottom", "right", "front", "left", "back")

# Default role colors; a role map overrides any of them.
ROLES = {
    "metal": {"material": "steel", "base": "#9AA0A6"},
    "dark": {"material": "steel", "base": "#3E4248"},
    "furn": {"material": "wood", "base": "#6B4A2A"},
    "glow": {"material": "flat", "base": "#FF8000"},
}


# ------------------------------------------------------------------ packing / export
def load_json(path):
    with open(path, encoding="utf8") as f:
        return json.load(f)


def dump_layout(layout):
    """Compact JSON: one line per box so layout diffs read well."""
    head = [json.dumps(k) + ": " + json.dumps(v) for k, v in layout.items() if k != "boxes"]
    boxes = ",\n".join("  " + json.dumps(b) for b in layout["boxes"])
    return "{\n " + ",\n ".join(head) + ',\n "boxes": [\n' + boxes + "\n ]\n}\n"


def pack(layout, repack=False):
    """Shelf-pack unwraps, tallest first. Boxes keep their uv unless repack; new boxes go to free space."""
    W, H = layout["texture"]
    boxes = layout["boxes"]
    used = []

    def fits(u, v, fw, fh):
        if u + fw > W or v + fh > H:
            return False
        return all(u + fw <= a or a + aw <= u or v + fh <= b or b + ah <= v for a, b, aw, ah in used)

    todo = []
    for b in boxes:
        w, h, d = b["size"]
        fw, fh = 2 * d + 2 * w, d + h
        if b.get("uv") and not repack:
            used.append((b["uv"][0], b["uv"][1], fw, fh))
        else:
            todo.append((b, fw, fh))
    for b, fw, fh in sorted(todo, key=lambda t: (-t[2], -t[1], t[0]["name"])):
        spot = None
        for v in range(0, H - fh + 1):
            for u in range(0, W - fw + 1):
                if fits(u, v, fw, fh):
                    spot = (u, v)
                    break
            if spot:
                break
        if spot is None:
            raise SystemExit("cannot pack %s (%dx%d) into %dx%d" % (b["name"], fw, fh, W, H))
        b["uv"] = list(spot)
        used.append((spot[0], spot[1], fw, fh))
    return layout


def export_json(layout):
    """The same shape ModelExporter writes (rotation point + offset, radians about Z)."""
    out = []
    for b in layout["boxes"]:
        tilt = b.get("tilt", 0)
        tilt_x = b.get("tiltX", 0)  # a turn about the gun's long axis (X), e.g. a square bar shown as a diamond
        pos = b["pos"]
        rp = b["pivot"] if (tilt or tilt_x) else pos
        off = [pos[i] - rp[i] for i in range(3)] if (tilt or tilt_x) else [0, 0, 0]
        out.append({"name": b["name"], "part": b.get("part", 0), "uv": b["uv"], "rotationPoint": rp, "offset": off,
                    "size": b["size"], "rotate": [float(np.radians(tilt_x)), 0, float(np.radians(tilt))],
                    "mirror": False})
    return {"name": layout["gun"], "kind": "gun", "textureWidth": layout["texture"][0],
            "textureHeight": layout["texture"][1], "scale": layout["scale"],
            "bodyRotationYDegrees": layout.get("bodyRotationYDegrees", 90), "muzzle": layout.get("muzzle", [0, 0, 0]),
            "textures": ["gregguns:textures/models/guns/%s.png" % layout["gun"]], "note": "", "boxes": out}


def fnum(v):
    s = ("%g" % v)
    return s + "f"


def java_rows(layout):
    rows = []
    for b in layout["boxes"]:
        w, h, d = b["size"]
        x, y, z = b["pos"]
        tilt = b.get("tilt", 0)
        tilt_x = b.get("tiltX", 0)
        piv = "new float[] { %s, %s, %s }" % tuple(fnum(t) for t in b["pivot"]) if (tilt or tilt_x) else "NO_PIVOT"
        extra = ", " + fnum(tilt_x) if tilt_x else ""
        rows.append("        new BoxSpec(\"%s\", %d, %d, %d, %d, %d, %d, %s, %s, %s, %s, %s%s)," % (
            b["name"], b.get("part", 0), b["uv"][0], b["uv"][1], w, h, d, fnum(x), fnum(y), fnum(z), piv, fnum(tilt),
            extra))
    return "\n".join(rows)


def splice_java(path, rows):
    with open(path, encoding="utf8") as f:
        src = f.read()
    a, b = "// BEGIN GENERATED BOXES", "// END GENERATED BOXES"
    i, j = src.index(a), src.index(b)
    src = src[:i] + a + " (tools/asset-gen/gun_paint.py pack; edit the layout, not this)\n" + rows + "\n        " + src[j:]
    with open(path, "w", encoding="utf8") as f:
        f.write(src)


# ------------------------------------------------------------------ painting
def steel(r, h, w, base, grain_along_x=True, scratches=True, noise=0.0):
    """Flat steel: a gentle large-scale tonal drift, faint hairline scratches, no speckle.

    `scratches` false skips the hairlines (a satin finish); `noise` adds per-pixel grain of that relative amplitude.
    """
    b = hx(base) if isinstance(base, str) else np.asarray(base, float)
    drift = vnoise(r, h, w, max(3, w // 2), max(3, h // 2))
    rgb = np.tile(b, (h, w, 1)) * (0.96 + 0.07 * (drift - .5) * 2)[..., None]
    for _ in range(max(1, (w * h) // 40) if scratches else 0):
        x, y, n = int(r.integers(0, w)), int(r.integers(0, h)), int(r.integers(3, 7))
        if grain_along_x:
            mul(rgb, x, y, n, 1, 1.06 if r.random() < .5 else 0.94)
        else:
            mul(rgb, x, y, 1, n, 1.06 if r.random() < .5 else 0.94)
    if noise:
        rgb = np.clip(rgb * (1.0 + noise * (r.random((h, w)) - .5) * 2)[..., None], 0, 255)
    return rgb


def wood(r, h, w, base, along_x, end_grain=False):
    """Wood: streaks running along the grain axis, a few dark pores, or concentric rings for end grain."""
    b = hx(base) if isinstance(base, str) else np.asarray(base, float)
    if end_grain:
        yy, xx = np.mgrid[0:h, 0:w]
        d = np.hypot(xx - w / 2.0, yy - h / 2.0)
        f = 0.74 + 0.12 * np.sin(d * 2.1 + 3 * vnoise(r, h, w, 4))
        return np.clip(b * f[..., None], 0, 255)
    n, m = (h, w) if along_x else (w, h)  # n lines, each m long
    lines = 0.82 + 0.28 * r.random(n)
    wob = vnoise(r, 1, m, 5, 1)[0]
    f = lines[:, None] * (0.93 + 0.14 * wob[None, :])
    for k in range(1, n, int(r.integers(3, 5))):
        f[k, :] *= 0.80  # dark grain line
    f = f if along_x else f.T
    rgb = b * f[..., None]
    for _ in range(max(1, w * h // 60)):
        x, y = int(r.integers(0, w)), int(r.integers(0, h))
        mul(rgb, x, y, int(r.integers(2, 5)) if along_x else 1, 1 if along_x else int(r.integers(2, 5)), 0.72)
    return np.clip(rgb, 0, 255)


def bevel(rgb, hi=1.14, lo=0.82):
    h, w = rgb.shape[:2]
    if h < 3 or w < 3:
        return
    mul(rgb, 0, 0, w, 1, hi)
    mul(rgb, 0, 0, 1, h, hi * .96)
    mul(rgb, 0, h - 1, w, 1, lo)
    mul(rgb, w - 1, 0, 1, h, lo * 1.04)


def face_xywh(painter, w, h):
    x, y = painter.get("x", 0), painter.get("y", 0)
    pw, ph = painter.get("w", w), painter.get("h", h)
    if painter.get("center"):
        x, y = (w - pw) // 2 + x, (h - ph) // 2 + y
    elif x < 0:
        x = w + x - pw + 1
    if y < 0 and not painter.get("center"):
        y = h + y - ph + 1
    return x, y, pw, ph


def apply_painter(rgb, p, face):
    h, w = rgb.shape[:2]
    kind = p["type"]
    if kind == "rect":
        x, y, pw, ph = face_xywh(p, w, h)
        rect(rgb, x, y, pw, ph, p.get("color", "#101012"), p.get("alpha", 1.0))
    elif kind == "inset":  # a recessed panel: dark rim on its top/left, light on its bottom/right
        x, y, pw, ph = face_xywh(p, w, h)
        mul(rgb, x, y, pw, ph, p.get("shade", 0.78))
        mul(rgb, x, y, pw, 1, 0.7)
        mul(rgb, x, y, 1, ph, 0.7)
        mul(rgb, x, y + ph - 1, pw, 1, 1.15)
        mul(rgb, x + pw - 1, y, 1, ph, 1.15)
    elif kind == "bore":  # a round-ish dark opening with a lit rim
        pw = p.get("size", max(2, min(w, h) - 2 * p.get("inset", 1)))
        cx, cy = w / 2.0, h / 2.0
        yy, xx = np.mgrid[0:h, 0:w]
        dist = np.maximum(abs(xx + .5 - cx), abs(yy + .5 - cy))
        circ = np.hypot(xx + .5 - cx, yy + .5 - cy)
        inside = circ <= pw / 2.0 + .45
        rim = (circ <= pw / 2.0 + 1.2) & ~inside
        rgb[rim] = rgb[rim] * 0.8
        rgb[inside] = np.array(hx(p.get("color", "#050506")))
        core = circ <= max(0.8, pw / 2.0 - 0.8)
        rgb[inside & core] = np.array(hx("#000000"))
    elif kind == "bands":  # evenly spaced ring lines across the long axis
        for x in range(p.get("start", 2), w - 1, p.get("every", 5)):
            mul(rgb, x, 0, 1, h, 0.8)
            mul(rgb, x + 1, 0, 1, h, 1.06)
    elif kind == "gradient":  # top-to-bottom shading: rounds off a tube
        top, bot = p.get("top", 1.12), p.get("bottom", 0.8)
        for y in range(h):
            f = top + (bot - top) * (y / max(1, h - 1))
            mul(rgb, 0, y, w, 1, f)
    elif kind == "tone":  # lighten (f > 1) or darken (f < 1) a rectangle, default the whole face
        x, y, pw, ph = face_xywh(p, w, h)
        mul(rgb, x, y, pw, ph, p.get("f", 1.0))
    elif kind == "seam":  # a thin panel line across or along
        dark, light = p.get("dark", 0.62), p.get("light", 1.1)  # a faint seam: dark 0.78, light 1.0
        if p.get("axis", "v") == "v":
            mul(rgb, p["at"], 0, 1, h, dark)
            mul(rgb, p["at"] + 1, 0, 1, h, light)
        else:
            mul(rgb, 0, p["at"], w, 1, dark)
            mul(rgb, 0, p["at"] + 1, w, 1, light)
    else:
        raise SystemExit("unknown painter " + kind)


def camo(r, h, w, pal, pattern="blotch"):
    pal = [hx(c) for c in pal]
    n = vnoise(r, h, w, 5) * .6 + vnoise(r, h, w, 3) * .4
    q = np.quantile(n, np.linspace(0, 1, len(pal) + 1)[1:-1])
    idx = np.zeros((h, w), int)
    for t in q:
        idx += (n > t)
    return np.array(pal)[idx]


def paint_face(box_spec, role_def, name, dims, face, fw, fh, seed_key):
    r = rng_for(seed_key)
    mat = role_def.get("material", "steel")
    base = role_def["base"]
    w, h, d = dims
    if role_def.get("camo"):
        rgb = camo(r, fh, fw, role_def["camo"])
    elif mat == "wood":
        axis = box_spec.get("grain", "x")
        if axis == "x":
            end = face in ("left", "right")
        else:
            end = face in ("top", "bottom")
        rgb = wood(r, fh, fw, base, along_x=(axis == "x"), end_grain=end)
    elif mat == "flat":
        rgb = np.tile(hx(base), (fh, fw, 1))
    else:
        rgb = steel(r, fh, fw, base, grain_along_x=box_spec.get("grain", "x") == "x",
                    scratches=role_def.get("scratches", True), noise=role_def.get("noise", 0.0))
    for p in box_spec.get("painters", []):
        if face in p.get("faces", FACES):
            apply_painter(rgb, p, face)
    bevel(rgb, *box_spec.get("bevel", (1.12, 0.84)))
    return rgb


def paint(gun, model_path=None):
    rm = load_json(os.path.join(PAINT_DIR, gun + ".json"))
    model = load_json(model_path or os.path.join(ROOT, "build", "gun-paint", gun + ".json"))
    W, H = model["textureWidth"], model["textureHeight"]
    for entry in rm["textures"]:
        roles = {k: dict(v) for k, v in ROLES.items()}
        for k, v in rm.get("roles", {}).items():
            roles.setdefault(k, {}).update(v)
        for k, v in (entry.get("variant") or {}).get("roles", {}).items():
            roles.setdefault(k, {}).update(v)
        tex = Tex(W, H)
        for b in model["boxes"]:
            spec = rm["boxes"][b["name"]]
            w, h, d = b["size"]
            u, v = b["uv"]
            for face, (x, y, fw, fh) in box_uv(u, v, w, h, d).items():
                if fw <= 0 or fh <= 0:
                    continue
                key = "gunpaint:%s:%s:%s:%s" % (gun, entry["file"], b["name"], face)
                tex.paste(x, y, paint_face(spec, roles[spec["role"]], b["name"], (w, h, d), face, fw, fh, key))
        out = os.path.join(TEX_DIR, entry["file"])
        tex.save(out)
        print("wrote", out)


def main(argv):
    if len(argv) < 3 or argv[1] not in ("pack", "paint"):
        raise SystemExit(__doc__)
    cmd, gun = argv[1], argv[2]
    if cmd == "pack":
        path = os.path.join(PAINT_DIR, gun + ".layout.json")
        layout = pack(load_json(path), "--repack" in argv)
        with open(path, "w", encoding="utf8") as f:
            f.write(dump_layout(layout))
        os.makedirs(os.path.join(ROOT, "build", "gun-paint"), exist_ok=True)
        with open(os.path.join(ROOT, "build", "gun-paint", gun + ".json"), "w", encoding="utf8") as f:
            json.dump(export_json(layout), f, indent=1)
        if "--java" in argv:
            splice_java(argv[argv.index("--java") + 1], java_rows(layout))
        print(java_rows(layout))
    else:
        paint(gun, argv[argv.index("--model") + 1] if "--model" in argv else None)


if __name__ == "__main__":
    main(sys.argv)
