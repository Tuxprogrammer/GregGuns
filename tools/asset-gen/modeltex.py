"""Helpers for GregGuns model textures (guns, armor, skins, machines, projectiles).

Original, procedural art. Nothing here reads or imitates Techguns assets.
Everything is deterministic: randomness comes from rng_for(key), a numpy
Generator seeded from a SHA-1 of the asset id.

Key ideas
  * Tex           : float RGB + alpha canvas, saved as an RGBA PNG.
  * box_uv        : standard Minecraft box unwrap (the same layout vanilla models use).
  * pack_boxes    : shelf-packs a list of boxes into a sheet, scaling them up so the
                    sheet footprint is filled; the result is the documented UV layout.
  * material()    : steel / polymer / wood / fabric / leather / rubber / concrete / glass /
                    carbon / skin / camo fills.
  * panelize/wear : seams, bevels, bolts, scratches and grime.
  * DET           : named detail painters (rails, vents, glow strips, lenses ...).
"""
import hashlib
import numpy as np
from PIL import Image


def seed_for(key):
    return int(hashlib.sha1(key.encode()).hexdigest()[:8], 16)


def rng_for(key):
    return np.random.default_rng(seed_for(key))


def hx(s):
    return np.array([int(s[i:i + 2], 16) for i in (1, 3, 5)], float)


def lighten(c, f):
    return np.clip(np.asarray(c, float) * f, 0, 255)


class Tex:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.rgb = np.zeros((h, w, 3), float)
        self.a = np.zeros((h, w), float)

    def paste(self, x, y, arr, alpha=None):
        h, w = arr.shape[:2]
        x1, y1 = min(self.w, x + w), min(self.h, y + h)
        if x1 <= x or y1 <= y:
            return
        self.rgb[y:y1, x:x1] = arr[:y1 - y, :x1 - x, :3]
        if alpha is None:
            self.a[y:y1, x:x1] = 255
        else:
            self.a[y:y1, x:x1] = alpha[:y1 - y, :x1 - x]

    def view(self, x, y, w, h):
        return self.rgb[y:y + h, x:x + w]

    def image(self):
        out = np.zeros((self.h, self.w, 4), np.uint8)
        out[..., :3] = np.clip(self.rgb + 0.5, 0, 255).astype(np.uint8)
        out[..., 3] = np.clip(self.a + 0.5, 0, 255).astype(np.uint8)
        out[out[..., 3] == 0] = 0
        return Image.fromarray(out, "RGBA")

    def save(self, path):
        self.image().save(path)


def vnoise(r, h, w, cx, cy=None):
    cy = cy or cx
    gh, gw = h // cy + 2, w // cx + 2
    g = (r.random((gh, gw)) * 255).astype(np.uint8)
    im = Image.fromarray(g).resize((gw * cx, gh * cy), Image.BICUBIC)
    return np.asarray(im, float)[:h, :w] / 255.0


def _f(fac, base):
    return np.clip(fac[..., None] * base, 0, 255)


def material(kind, r, h, w, base, pal=None):
    b = hx(base) if isinstance(base, str) else np.asarray(base, float)
    fine = vnoise(r, h, w, 1)
    if kind == "steel":
        streak = vnoise(r, h, w, max(4, w // 2), 1)
        return _f(0.92 + 0.16 * (streak - .5) * 2 * .6 + 0.07 * (fine - .5), b)
    if kind == "polymer":
        return _f(0.95 + 0.07 * (fine - .5) + 0.07 * (vnoise(r, h, w, 3) - .5), b)
    if kind == "wood":
        grain = vnoise(r, h, w, max(6, w // 2), 1)
        rings = 0.5 + 0.5 * np.sin((np.arange(h)[:, None] * 1.7) + 6 * vnoise(r, h, w, 8, 3))
        return _f(0.78 + 0.18 * grain + 0.14 * rings + 0.04 * (fine - .5), b)
    if kind == "fabric":
        yy, xx = np.mgrid[0:h, 0:w]
        return _f(0.94 + 0.05 * (((xx + yy) % 2) - .5) + 0.08 * (vnoise(r, h, w, 3) - .5) + 0.04 * (fine - .5), b)
    if kind == "leather":
        return _f(0.88 + 0.22 * (vnoise(r, h, w, 2) - .5) + 0.06 * (fine - .5), b)
    if kind == "rubber":
        return _f(0.93 + 0.1 * (fine - .5), b)
    if kind == "concrete":
        sp = (r.random((h, w)) > 0.96) * 0.18
        return _f(0.86 + 0.24 * (vnoise(r, h, w, 2) - .5) + 0.1 * (fine - .5) - sp, b)
    if kind == "skin":
        return _f(0.94 + 0.1 * (vnoise(r, h, w, 2) - .5) + 0.04 * (fine - .5), b)
    if kind == "carbon":
        yy, xx = np.mgrid[0:h, 0:w]
        tw = (((xx + yy) // 2) % 2) * 0.22
        return _f(0.72 + tw + 0.06 * (fine - .5), b)
    if kind == "glass":
        yy, xx = np.mgrid[0:h, 0:w]
        d = (xx + yy) / max(1, w + h)
        return _f(0.55 + 0.7 * np.clip(1.2 - d * 2.2, 0, 1) ** 2 + 0.05 * (fine - .5), b)
    if kind == "camo":
        pattern = "blotch"
        if isinstance(pal, tuple):
            pattern, pal = pal
        pal = [hx(c) for c in pal]
        yy0, xx0 = np.mgrid[0:h, 0:w]
        if pattern == "digital":      # blocky pixel camo
            gh, gw = h // 3 + 2, w // 3 + 2
            n = np.kron(r.random((gh, gw)) * .6 + vnoise(r, gh, gw, 4) * .4, np.ones((3, 3)))[:h, :w]
        elif pattern == "splinter":   # angular sheared shards
            big = vnoise(r, h, w + h, 9, 3)
            n = np.stack([np.roll(big[y], int(y * 0.9))[:w] for y in range(h)])
        elif pattern == "tiger":      # vertical stripes with wobble
            n = 0.5 + 0.5 * np.sin((xx0 + 7 * vnoise(r, h, w, 6, 10)) * 0.9) * 0.9 + 0.1 * vnoise(r, h, w, 2)
        elif pattern == "bark":       # long vertical streaks
            n = vnoise(r, h, w, 2, 14) * .7 + vnoise(r, h, w, 1, 4) * .3
        elif pattern == "flecks":     # mostly one color, sparse specks
            n = np.where(r.random((h, w)) > 0.82, 0.9, 0.15) + 0.2 * vnoise(r, h, w, 5)
        else:
            n = vnoise(r, h, w, 6) * .6 + vnoise(r, h, w, 3) * .4
        q = np.quantile(n, [.28, .55, .82])
        idx = (n > q[0]).astype(int) + (n > q[1]) + (n > q[2])
        out = np.array(pal)[np.clip(idx, 0, len(pal) - 1)]
        yy, xx = np.mgrid[0:h, 0:w]
        return np.clip(out * (0.96 + 0.05 * (((xx + yy) % 2) - .5) + 0.05 * (fine - .5))[..., None], 0, 255)
    return np.tile(b, (h, w, 1))


def rect(a, x, y, w, h, col, alpha=1.0):
    H, W = a.shape[:2]
    x0, y0, x1, y1 = max(0, x), max(0, y), min(W, x + w), min(H, y + h)
    if x1 <= x0 or y1 <= y0:
        return
    col = hx(col) if isinstance(col, str) else np.asarray(col, float)
    a[y0:y1, x0:x1] = a[y0:y1, x0:x1] * (1 - alpha) + col * alpha


def mul(a, x, y, w, h, f):
    H, W = a.shape[:2]
    x0, y0, x1, y1 = max(0, x), max(0, y), min(W, x + w), min(H, y + h)
    if x1 > x0 and y1 > y0:
        a[y0:y1, x0:x1] = np.clip(a[y0:y1, x0:x1] * f, 0, 255)


def panelize(rgb, r, lo=6, hi=14, seam=0.55, bolts=True):
    h, w = rgb.shape[:2]
    leaves = []

    def split(x, y, ww, hh):
        can_x, can_y = ww >= lo * 2, hh >= lo * 2
        if (ww <= hi and hh <= hi) or not (can_x or can_y):
            leaves.append((x, y, ww, hh)); return
        if can_x and (ww >= hh or not can_y):
            s = int(r.integers(lo, ww - lo + 1)); split(x, y, s, hh); split(x + s, y, ww - s, hh)
        else:
            s = int(r.integers(lo, hh - lo + 1)); split(x, y, ww, s); split(x, y + s, ww, hh - s)
    split(0, 0, w, h)
    for x, y, ww, hh in leaves:
        mul(rgb, x, y, ww, hh, 1 + (r.random() - .5) * 0.12)
        if x > 0:
            mul(rgb, x, y, 1, hh, seam)
        if y > 0:
            mul(rgb, x, y, ww, 1, seam)
        if ww > 3 and hh > 3:
            mul(rgb, x + 1, y + 1, ww - 1, 1, 1.13); mul(rgb, x + 1, y + 1, 1, hh - 1, 1.08)
            mul(rgb, x + 1, y + hh - 1, ww - 1, 1, 0.86)
        if bolts and ww >= 8 and hh >= 8:
            for bx, by in ((x + 2, y + 2), (x + ww - 3, y + 2), (x + 2, y + hh - 3), (x + ww - 3, y + hh - 3)):
                if 0 <= bx < w - 1 and 0 <= by < h - 1:
                    mul(rgb, bx, by, 1, 1, 0.45); mul(rgb, bx + 1, by, 1, 1, 1.25)


def wear(rgb, r, amount=0.3):
    h, w = rgb.shape[:2]
    for _ in range(int(amount * w * h / 45)):
        x, y = int(r.integers(0, w)), int(r.integers(0, h)); n = int(r.integers(2, 6))
        if r.random() < 0.6:
            mul(rgb, x, y, n, 1, 1.32)
        else:
            mul(rgb, x, y, 1, n, 1.32)
    g = vnoise(r, h, w, 4)
    m = g > (0.78 - amount * 0.2)
    rgb[m] *= 0.86
    edge = r.random((h, w)) < amount * 0.35
    for sl in (np.s_[0, :], np.s_[-1, :]):
        rgb[sl][edge[sl]] *= 1.2
    for sl in (np.s_[:, 0], np.s_[:, -1]):
        rgb[sl][edge[sl]] *= 1.2
    np.clip(rgb, 0, 255, out=rgb)


def ao(rgb):
    h, w = rgb.shape[:2]
    if h < 3 or w < 3:
        return
    mul(rgb, 0, h - 1, w, 1, 0.8); mul(rgb, w - 1, 0, 1, h, 0.86)
    mul(rgb, 0, 0, w, 1, 1.1); mul(rgb, 0, 0, 1, h, 1.06)


# ---- details ---------------------------------------------------------------
def glow_col(ctx):
    return ctx.get("glow") or "#FF8000"


def d_rails(rgb, r, face, ctx):
    h, w = rgb.shape[:2]
    if face in ("top", "bottom", "front") and w >= 6:
        for x in range(1, w - 1, 2):
            rect(rgb, x, 0, 1, max(1, h // 4), (20, 20, 22), .55)
        mul(rgb, 0, max(1, h // 4), w, 1, 0.7)


def d_rings(rgb, r, face, ctx):
    h, w = rgb.shape[:2]
    if w >= 8 and h >= 2:
        for x in range(2, w - 1, 5):
            mul(rgb, x, 0, 1, h, 0.55); mul(rgb, x + 1, 0, 1, h, 1.18)


def d_knurl(rgb, r, face, ctx):
    h, w = rgb.shape[:2]
    yy, xx = np.mgrid[0:h, 0:w]
    m = ((xx + yy) % 3 == 0) & (xx > 0) & (xx < w - 1)
    rgb[m] *= 0.72


def d_vents(rgb, r, face, ctx):
    h, w = rgb.shape[:2]
    if h < 4 or w < 8:
        return
    n = max(2, (w - 4) // 3)
    y0 = h // 2 - 1
    for i in range(n):
        x = 2 + i * 3
        if x + 1 < w - 1:
            rect(rgb, x, y0, 2, max(1, min(3, h - 3)), (12, 12, 14), .85)


def d_label(rgb, r, face, ctx):
    h, w = rgb.shape[:2]
    if w < 8 or h < 5:
        return
    x = 2; y = h // 2
    while x < w - 4:
        gw = int(r.integers(1, 3)); gh = 2 if r.random() < .6 else 3
        rect(rgb, x, y, gw, gh, (225, 225, 215), .55)
        x += gw + int(r.integers(1, 3))
    rect(rgb, 1, h - 3, max(1, w // 3), 1, ctx.get("accent", "#C8A040"), .8)


def d_strip(rgb, r, face, ctx):
    h, w = rgb.shape[:2]
    g = hx(glow_col(ctx))
    if w >= h and h >= 3:
        y = h // 2 - (1 if h > 4 else 0)
        rect(rgb, 1, y - 1, w - 2, 1, g, .3); rect(rgb, 1, y, w - 2, 2 if h > 4 else 1, g * 0.7 + 90, 1.0)
        rect(rgb, 1, y + 2, w - 2, 1, g, .3)
    elif h > w and w >= 3:
        x = w // 2 - 1
        rect(rgb, x - 1, 1, 1, h - 2, g, .3); rect(rgb, x, 1, 2, h - 2, g * 0.7 + 90, 1.0); rect(rgb, x + 2, 1, 1, h - 2, g, .3)


def d_hazard(rgb, r, face, ctx):
    h, w = rgb.shape[:2]
    if h < 4:
        return
    y0 = h - max(2, h // 4)
    yy, xx = np.mgrid[0:h, 0:w]
    m = (yy >= y0) & (((xx + yy) // 2) % 2 == 0)
    rgb[m] = hx("#E0B020")
    m2 = (yy >= y0) & ~m
    rgb[m2] = hx("#1A1A1C")


def d_lens(rgb, r, face, ctx):
    h, w = rgb.shape[:2]
    if h < 4 or w < 4:
        return
    cx, cy, rad = w / 2 - .5, h / 2 - .5, min(w, h) / 2 - 1
    yy, xx = np.mgrid[0:h, 0:w]
    d = np.hypot(xx - cx, yy - cy)
    g = hx(ctx.get("lens", "#55B0E0"))
    m = d <= rad
    rgb[m] = g * (0.5 + 0.5 * (1 - d[m] / max(rad, 1)))[:, None]
    rgb[(d > rad) & (d <= rad + 1)] = (15, 15, 18)
    if rad > 1.5:
        rgb[int(cy - rad / 2), int(cx - rad / 2)] = (240, 250, 255)


def d_coil(rgb, r, face, ctx):
    h, w = rgb.shape[:2]
    c = hx(ctx.get("accent", "#B87333"))
    for x in range(1, w - 1, 3):
        rect(rgb, x, 0, 2, h, c, .85); mul(rgb, x, 0, 1, h, 1.25); mul(rgb, x + 1, 0, 1, h, 0.7)


def d_cells(rgb, r, face, ctx):
    h, w = rgb.shape[:2]
    g = hx(glow_col(ctx))
    if h < 3:
        return
    y = h // 2 - 1
    for x in range(2, w - 2, 3):
        rect(rgb, x, y, 2, 2, g * 0.6 + 100, 1.0)
        rect(rgb, x - 1, y - 1, 4, 4, g, .18)


def d_grill(rgb, r, face, ctx):
    h, w = rgb.shape[:2]
    yy, xx = np.mgrid[0:h, 0:w]
    m = ((xx % 3 == 1) | (yy % 3 == 1)) & (xx > 0) & (yy > 0) & (xx < w - 1) & (yy < h - 1)
    rgb[m] *= 0.45


def d_stripe(rgb, r, face, ctx):
    h, w = rgb.shape[:2]
    if h < 4:
        return
    rect(rgb, 0, h // 2 - 1, w, max(1, h // 5), ctx.get("accent", "#C8A040"), .9)


def d_cross(rgb, r, face, ctx):
    """Hazard/targeting marks: thin crosses used on heavy ordnance."""
    h, w = rgb.shape[:2]
    for x in range(3, w - 2, 7):
        rect(rgb, x, h // 2, 3, 1, (230, 230, 220), .55); rect(rgb, x + 1, h // 2 - 1, 1, 3, (230, 230, 220), .55)


DET = {n[2:]: f for n, f in list(globals().items()) if n.startswith("d_")}


# ---- unwrap and packing ----------------------------------------------------
def box_uv(u, v, w, h, d, s=1):
    f = {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
         "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}
    return {k: tuple(int(t * s) for t in val) for k, val in f.items()}


def pack_boxes(parts, W, H, x0=0, y0=0):
    """parts: [(name, w, h, d)] relative sizes. Returns [(name,u,v,w,h,d)] in texels,
    scaled up as far as a shelf packing still fits inside the W x H region."""
    def attempt(k):
        dims = [(n, max(1, round(w * k)), max(1, round(h * k)), max(1, round(d * k))) for n, w, h, d in parts]
        items = sorted(dims, key=lambda t: -(t[2] + t[3]))
        x = y = rowh = 0; out = []
        for n, w, h, d in items:
            fw, fh = 2 * d + 2 * w, d + h
            if fw > W:
                return None
            if x + fw > W:
                x, y, rowh = 0, y + rowh, 0
            if y + fh > H:
                return None
            out.append((n, x0 + x, y0 + y, w, h, d)); x += fw; rowh = max(rowh, fh)
        return out
    lo, hi, best = 0.02, 60.0, None
    for _ in range(34):
        mid = (lo + hi) / 2
        res = attempt(mid)
        if res:
            best, lo = res, mid
        else:
            hi = mid
    if best is None:
        raise ValueError("cannot pack")
    return best


def paint_box(tex, u, v, w, h, d, mat, base, r, details=(), ctx=None, cell=(5, 12), wear_amt=0.3,
              panel=True, pal=None, seam=0.55):
    ctx = ctx or {}
    for fname, (x, y, fw, fh) in box_uv(u, v, w, h, d).items():
        if fw <= 0 or fh <= 0:
            continue
        rgb = material(mat, r, fh, fw, base, pal)
        if panel and min(fw, fh) >= 4:
            panelize(rgb, r, cell[0], cell[1], seam)
        for dn in details:
            DET[dn](rgb, r, fname, ctx)
        wear(rgb, r, wear_amt)
        ao(rgb)
        tex.paste(x, y, rgb)
