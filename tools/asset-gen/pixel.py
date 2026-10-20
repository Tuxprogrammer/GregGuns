"""Tiny pixel-art toolkit for GregGuns 16x16 item icons.

Procedural and deterministic: no randomness except via explicit seeded
``random.Random`` instances passed in by callers. Light comes from the top
left; every sprite gets a dark outline pass.

Concepts
  * ramp(rgb)       -> 5 shades [dark, shade, base, light, highlight]
  * Canvas.fill()   -> bevel-shaded fill of a pixel set (top/left lit,
                       bottom/right shadowed), the GT-style look
  * part helpers    -> barrels, receivers, stocks, magazines, scopes, grips,
                       bullets, shells, plates, emitters, cloth, armor
                       silhouettes, tanks, canisters
Sprites are drawn inside 1..14 so the outline fits inside the 16x16 frame.
"""
from PIL import Image

W = H = 16


# ---------------------------------------------------------------- colors
def hexrgb(s):
    s = s.lstrip("#")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def _mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def ramp(rgb, soft=1.0):
    """GT-style 5 shade ramp: dark, shade, base, light, highlight."""
    if isinstance(rgb, str):
        rgb = hexrgb(rgb)
    black, white = (0, 0, 0), (255, 255, 255)
    return [
        _mix(rgb, black, 0.58),
        _mix(rgb, black, 0.30),
        tuple(rgb),
        _mix(rgb, white, 0.24 * soft),
        _mix(rgb, white, 0.48 * soft),
    ]


DK, SH, BS, LT, HL = range(5)


def tint(rgb, f):
    return tuple(max(0, min(255, int(ch * f))) for ch in tuple(rgb)[:3])


def flat(rgb):
    """A ramp with no shading (for glows and decals)."""
    return [tuple(rgb)] * 5


def glow(rgb):
    """Emissive ramp: bright core, saturated edge, never darker than base."""
    rgb = hexrgb(rgb) if isinstance(rgb, str) else rgb
    return [_mix(rgb, (0, 0, 0), 0.25), rgb, rgb, _mix(rgb, (255, 255, 255), 0.45),
            _mix(rgb, (255, 255, 255), 0.8)]


# Shared neutral ramps
BLACK = ramp((60, 62, 72), 0.8)
POLY = ramp((104, 108, 122), 0.9)
WOOD = ramp((130, 84, 40))
DARKWOOD = ramp((96, 60, 30))
BRASS = ramp((214, 160, 40))
LEAD = ramp((88, 90, 104))
RUBBER = ramp((36, 36, 38), 0.6)
OLIVE = ramp((92, 104, 56))
CANVAS = ramp((196, 176, 128))
RED = ramp((190, 40, 36))
WHITE = ramp((236, 236, 240))
GLASS = ramp((150, 200, 215))
CYAN = glow((60, 230, 240))
ORANGE_G = glow((255, 140, 30))
RED_G = glow((255, 50, 40))
GREEN_G = glow((70, 250, 90))
VIOLET_G = glow((190, 90, 255))
GOLD = ramp((240, 200, 60))
BLUE_G = glow((70, 130, 255))


# ---------------------------------------------------------------- canvas
class Canvas:
    def __init__(self):
        self.px = {}

    # --- primitives
    def pix(self, x, y, c):
        if isinstance(c[0], (tuple, list)):
            c = c[BS]  # a whole ramp was passed: use its base shade
        if 0 <= x < W and 0 <= y < H:
            self.px[(x, y)] = tuple(c[:3]) + (255,)

    def erase(self, x, y):
        self.px.pop((x, y), None)

    def get(self, x, y):
        return self.px.get((x, y))

    def fill(self, coords, rp, bevel=True):
        """Fill ``coords`` with ``rp`` using top-left-light bevel shading."""
        if isinstance(rp[0], int):
            rp = [tuple(rp[:3])] * 5
        s = set(coords)
        for (x, y) in s:
            up, lf = (x, y - 1) in s, (x - 1, y) in s
            dn, rt = (x, y + 1) in s, (x + 1, y) in s
            if not bevel:
                c = rp[BS]
            elif (not up or not lf) and (not dn or not rt):
                c = rp[BS] if (up or lf or dn or rt) else rp[BS]
                if not up and not lf and not dn and not rt:
                    c = rp[BS]
                elif not dn and not rt:
                    c = rp[SH]
                elif not up and not lf:
                    c = rp[LT]
                else:
                    c = rp[BS]
            elif not up and not lf:
                c = rp[HL]
            elif not up or not lf:
                c = rp[LT]
            elif not dn and not rt:
                c = rp[DK]
            elif not dn or not rt:
                c = rp[SH]
            else:
                c = rp[BS]
            self.pix(x, y, c)
        return s

    def rect(self, x0, y0, x1, y1, rp, bevel=True):
        return self.fill([(x, y) for x in range(x0, x1 + 1) for y in range(y0, y1 + 1)], rp, bevel)

    def hline(self, x0, x1, y, c):
        for x in range(x0, x1 + 1):
            self.pix(x, y, c)

    def vline(self, x, y0, y1, c):
        for y in range(y0, y1 + 1):
            self.pix(x, y, c)

    def line(self, x0, y0, x1, y1, c):
        dx, dy = abs(x1 - x0), -abs(y1 - y0)
        sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
        err = dx + dy
        while True:
            self.pix(x0, y0, c)
            if x0 == x1 and y0 == y1:
                break
            e2 = 2 * err
            if e2 >= dy:
                err += dy
                x0 += sx
            if e2 <= dx:
                err += dx
                y0 += sy

    def line_coords(self, x0, y0, x1, y1, thick=1):
        out = []
        tmp = Canvas()
        tmp.line(x0, y0, x1, y1, (1, 1, 1))
        out = list(tmp.px)
        if thick > 1:
            out = out + [(x, y + 1) for x, y in out]
        return out

    def ellipse_coords(self, cx, cy, rx, ry):
        """cx, cy may be half-integers (center between pixels)."""
        out = []
        for y in range(H):
            for x in range(W):
                if ((x - cx) / (rx + 0.5)) ** 2 + ((y - cy) / (ry + 0.5)) ** 2 <= 1.0:
                    out.append((x, y))
        return out

    def disc(self, cx, cy, r, rp, bevel=True):
        return self.fill(self.ellipse_coords(cx, cy, r, r), rp, bevel)

    def ellipse(self, cx, cy, rx, ry, rp, bevel=True):
        return self.fill(self.ellipse_coords(cx, cy, rx, ry), rp, bevel)

    def poly_coords(self, pts):
        out = []
        n = len(pts)
        for y in range(H):
            for x in range(W):
                px_, py_ = x + 0.5, y + 0.5
                inside = False
                j = n - 1
                for i in range(n):
                    xi, yi = pts[i]
                    xj, yj = pts[j]
                    if (yi > py_) != (yj > py_) and px_ < (xj - xi) * (py_ - yi) / (yj - yi) + xi:
                        inside = not inside
                    j = i
                if inside:
                    out.append((x, y))
        return out

    def poly(self, pts, rp, bevel=True):
        return self.fill(self.poly_coords(pts), rp, bevel)

    def dither(self, coords, c1, c2, phase=0):
        for (x, y) in coords:
            self.pix(x, y, c1 if (x + y + phase) % 2 == 0 else c2)

    def stripes(self, coords, c, step=3, phase=0, diag=True):
        for (x, y) in coords:
            if ((x + y) if diag else x) % step == phase and (x, y) in self.px:
                self.pix(x, y, c)

    def recolor(self, coords, c):
        for (x, y) in coords:
            if (x, y) in self.px:
                self.pix(x, y, c)

    # --- whole-canvas ops
    def outline(self, strength=0.38, only=None):
        """Dark outline on transparent pixels 4-adjacent to the sprite.

        Outline color is the adjacent pixel darkened, so warm parts get warm
        outlines (GT does this).
        """
        add = {}
        for (x, y), c in self.px.items():
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                p = (x + dx, y + dy)
                if 0 <= p[0] < W and 0 <= p[1] < H and p not in self.px:
                    k = tint(c, strength)
                    if p in add:
                        k = tuple(min(a, b) for a, b in zip(add[p], k))
                    add[p] = k
        for p, k in add.items():
            self.px[p] = k + (255,)

    def flipped(self):
        n = Canvas()
        n.px = {(W - 1 - x, y): c for (x, y), c in self.px.items()}
        return n

    def shifted(self, dx, dy):
        n = Canvas()
        n.px = {(x + dx, y + dy): c for (x, y), c in self.px.items()
                if 0 <= x + dx < W and 0 <= y + dy < H}
        return n

    def rotated(self, deg):
        """Nearest-neighbor rotation (clockwise, y down) about the canvas center."""
        import math
        a = math.radians(deg)
        ca, sa = math.cos(a), math.sin(a)
        n = Canvas()
        for y in range(H):
            for x in range(W):
                dx, dy = x - 7.5, y - 7.5
                sx = ca * dx + sa * dy + 7.5
                sy = -sa * dx + ca * dy + 7.5
                p = (int(round(sx)), int(round(sy)))
                if p in self.px:
                    n.px[(x, y)] = self.px[p]
        return n

    def bbox_center_shift(self):
        xs = [p[0] for p in self.px]
        ys = [p[1] for p in self.px]
        return int(round(7.5 - (min(xs) + max(xs)) / 2)), int(round(7.5 - (min(ys) + max(ys)) / 2))

    def paste(self, other):
        self.px.update(other.px)

    def to_image(self):
        im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        for (x, y), c in self.px.items():
            im.putpixel((x, y), c)
        return im

    def save(self, path):
        self.to_image().save(path, optimize=False)


# ---------------------------------------------------------------- parts
def barrel(c, x0, x1, y, rp, h=1, rings=None, muzzle=None):
    """Horizontal barrel. rings: list of (x, ramp) collars. muzzle: ramp."""
    c.rect(x0, y, x1, y + h - 1, rp)
    for rx, rr in (rings or []):
        c.rect(rx, y - 1, rx, y + h, rr, bevel=False)
    if muzzle:
        c.rect(x1, y - 1, x1, y + h, muzzle)


def receiver(c, x0, y0, x1, y1, rp, port=True):
    c.rect(x0, y0, x1, y1, rp)
    if port and x1 - x0 >= 3 and y1 - y0 >= 2:
        c.hline(x0 + 1, x0 + 2, y0 + 1, rp[DK])


def stock(c, x0, y0, x1, y1, rp, drop=1):
    """Stock sloping down toward the butt (left). drop = rows lost at the butt."""
    pts = [(x0, y0), (x1 + 1, y0), (x1 + 1, y1 + 1), (x0, y1 + 1 + drop)]
    c.poly(pts, rp)


def grip(c, x, y0, y1, rp, w=2, lean=1):
    """Pistol grip leaning backward (down-left)."""
    pts = [(x, y0), (x + w, y0), (x + w - lean, y1 + 1), (x - lean, y1 + 1)]
    cells = c.poly(pts, rp)
    for (px_, py_) in cells:                      # rim highlight on the lit (left) edge
        if (px_ - 1, py_) not in cells:
            c.pix(px_, py_, rp[HL])


def magazine(c, x, y0, y1, rp, w=2, curve=0, rounds=None):
    """Magazine hanging from y0 to y1; curve shifts bottom forward (+x)."""
    pts = []
    for y in range(y0, y1 + 1):
        t = (y - y0) / max(1, y1 - y0)
        o = int(round(curve * t * t))
        pts += [(x + o + i, y) for i in range(w)]
    c.fill(pts, rp)
    if rounds:
        c.pix(x, y0, rounds)


def scope(c, x0, x1, y, rp, lens=None, h=2, mounts=True):
    c.rect(x0, y, x1, y + h - 1, rp)
    if lens:
        c.pix(x1, y, lens)
        if h > 1:
            c.pix(x1, y + 1, lens)
    if mounts:
        c.pix(x0 + 1, y + h, rp[DK])
        c.pix(x1 - 1, y + h, rp[DK])


def bullet(c, x, y0, y1, case_rp, tip_rp, tip=2, w=1):
    """Vertical cartridge, tip at top."""
    for dx in range(w):
        c.fill([(x + dx, y) for y in range(y0 + tip, y1 + 1)], case_rp, bevel=False)
        c.fill([(x + dx, y) for y in range(y0, y0 + tip)], tip_rp, bevel=False)
    # rim at the base
    c.hline(x, x + w - 1, y1, case_rp[DK])
    # highlight stripe on the left column
    for y in range(y0 + tip, y1):
        c.pix(x, y, case_rp[HL] if w > 1 else case_rp[LT])


def shell(c, x, y0, y1, hull_rp, base_rp, w=3, base_h=2):
    c.rect(x, y0, x + w - 1, y1 - base_h, hull_rp)
    c.rect(x, y1 - base_h + 1, x + w - 1, y1, base_rp)
    c.hline(x, x + w - 1, y0, hull_rp[SH])


def plate(c, x0, y0, x1, y1, rp, rivets=True, rivet_c=None):
    c.rect(x0, y0, x1, y1, rp)
    if rivets:
        rc = rivet_c or rp[HL]
        for (x, y) in ((x0 + 1, y0 + 1), (x1 - 1, y0 + 1), (x0 + 1, y1 - 1), (x1 - 1, y1 - 1)):
            c.pix(x, y, rc)


def gear(c, cx, cy, r, rp, teeth=8):
    """Solid cog: a disc with four square teeth and a dark axle hole."""
    cells = set(c.ellipse_coords(cx, cy, r - 1, r - 1))
    for (dx, dy) in ((r, 0), (-r, 0), (0, r), (0, -r)):
        cells.add((cx + dx, cy + dy))
        if r >= 3:
            cells.add((cx + dx + (1 if dy else 0), cy + dy + (1 if dx else 0)))
    c.fill(cells, rp)
    c.pix(cx, cy, rp[DK])


def spring(c, x0, x1, y, rp):
    """Coil seen side-on: alternating light and dark turns on one row."""
    for i, x in enumerate(range(x0, x1 + 1)):
        c.pix(x, y, rp[HL] if i % 2 == 0 else rp[DK])


def trefoil(c, cx, cy, fg=(250, 210, 30), bg=(30, 30, 30)):
    for (dx, dy) in ((-1, -1), (1, -1), (0, 1)):
        c.pix(cx + dx, cy + dy, fg)
    c.pix(cx, cy, bg)


def cross(c, cx, cy, col, r=2):
    for i in range(-r, r + 1):
        c.pix(cx + i, cy, col)
        c.pix(cx, cy + i, col)


def lens(c, x, y, col_rp=CYAN):
    c.pix(x, y, col_rp[HL])
    c.pix(x + 1, y, col_rp[BS])
    c.pix(x, y + 1, col_rp[BS])
    c.pix(x + 1, y + 1, col_rp[SH])


# ---------------------------------------------------------------- tanks
def canister(c, x0, y0, x1, y1, body_rp, fill_rp=None, fill_frac=0.75, cap_rp=None, glass=False):
    """Upright canister; fill_rp None = empty."""
    cap_rp = cap_rp or body_rp
    c.rect(x0 + 1, y0, x1 - 1, y0 + 1, cap_rp)           # cap
    body = [(x, y) for x in range(x0, x1 + 1) for y in range(y0 + 2, y1 + 1)]
    c.fill(body, body_rp)
    # inner window
    ix0, ix1, iy0, iy1 = x0 + 1, x1 - 1, y0 + 3, y1 - 1
    inner_h = iy1 - iy0 + 1
    if glass:
        # make the window glassy
        c.rect(ix0, iy0, ix1, iy1, GLASS, bevel=False)
    if fill_rp is not None:
        n = max(1, int(round(inner_h * fill_frac)))
        for y in range(iy1 - n + 1, iy1 + 1):
            for x in range(ix0, ix1 + 1):
                c.pix(x, y, fill_rp[LT] if x == ix0 else (fill_rp[SH] if x == ix1 else fill_rp[BS]))
        # meniscus
        c.hline(ix0, ix1, iy1 - n + 1, fill_rp[HL])
    else:
        for y in range(iy0, iy1 + 1):
            for x in range(ix0, ix1 + 1):
                c.pix(x, y, (112, 120, 128) if x > ix0 else (150, 158, 166))
        c.pix(ix0 + 1, iy0, (180, 188, 196)); c.pix(ix0 + 1, iy0 + 1, (166, 174, 182))


# ---------------------------------------------------------------- armor
def armor_piece(c, slot, cw):
    """Draw one armor slot with a per-set silhouette and details.

    cw keys: main, trim (ramps); acc (ramp, small details); vis (ramp, visor);
    kind (str) selecting the set's shape; glow (ramp, emissive) optional.
    """
    kind = cw["kind"]
    M, T = cw["main"], cw["trim"]
    A = cw.get("acc", T)
    args = (c, kind, M, T, A, cw.get("vis"), cw.get("glow"))
    {"helmet": _helmet, "chestplate": _chest, "leggings": _legs, "boots": _boots}[slot](*args)


def _dome(c, rp):
    c.poly([(5, 2), (11, 2), (12, 4), (13, 9), (11, 9), (11, 7), (5, 7), (5, 9), (3, 9), (4, 4)], rp)


def _helmet(c, kind, M, T, A, V, G):
    if kind == "bandit":       # pointed cowl, eye-slit mask, bronze studs
        c.poly([(8, 0), (10, 2), (11, 4), (12, 8), (14, 14), (11, 14), (11, 11), (5, 11), (5, 14),
                (2, 14), (4, 8), (5, 4), (6, 2)], M)
        c.rect(5, 6, 10, 8, BLACK)
        c.hline(6, 9, 7, (240, 220, 190))
        for p in ((8, 2), (6, 4), (10, 4)):
            c.pix(p[0], p[1], A[HL])
    elif kind == "miner":      # hard hat: wide brim, ridge, lamp
        c.poly([(5, 3), (10, 3), (12, 6), (15, 7), (15, 9), (0, 9), (0, 7), (3, 6)], M)
        c.rect(7, 1, 8, 3, M)
        c.rect(7, 5, 8, 6, glow((255, 240, 150)), bevel=False)
        c.hline(1, 14, 9, M[DK])
        c.pix(5, 5, T[HL])
        c.pix(10, 5, T[HL])
        c.rect(4, 10, 11, 12, ramp((50, 80, 150)))
    elif kind == "steel":      # open dome, nasal bar, cheek flaps
        c.poly([(4, 3), (11, 3), (12, 5), (12, 8), (3, 8), (3, 5)], M)
        c.rect(7, 8, 8, 11, T)
        c.rect(3, 9, 5, 12, T)
        c.rect(10, 9, 12, 12, T)
        c.hline(4, 11, 4, M[HL])
        c.hline(3, 12, 8, T[DK])
    elif kind == "olive":      # low dome, broad brim, net straps, mount
        c.poly([(5, 3), (10, 3), (12, 5), (12, 8), (3, 8), (3, 5)], M)
        c.rect(1, 8, 14, 9, M)
        c.rect(7, 2, 8, 4, T)
        c.stripes([(a, b) for a in range(4, 12) for b in range(4, 8)], M[DK], 3, 0, True)
        c.vline(3, 10, 12, A[BS])
        c.vline(12, 10, 12, A[BS])
        c.hline(4, 11, 12, A[BS])
    elif kind == "commando":   # tall balaclava, wide goggle bar
        c.poly([(5, 1), (10, 1), (11, 4), (11, 14), (4, 14), (4, 4)], M)
        c.rect(1, 5, 14, 7, BLACK)
        c.rect(2, 5, 6, 7, V or ramp((110, 200, 255)), bevel=False)
        c.rect(9, 5, 13, 7, V or ramp((110, 200, 255)), bevel=False)
        c.hline(2, 13, 5, (210, 240, 255))
        c.vline(7, 9, 13, M[DK])
        c.vline(8, 9, 13, M[DK])
    elif kind == "riot":       # flat-topped box helm, huge visor, jaw shield
        c.rect(2, 2, 13, 13, M)
        c.rect(3, 4, 12, 8, V or GLASS, bevel=False)
        c.hline(3, 12, 4, (200, 235, 245))
        c.hline(3, 12, 8, (60, 90, 110))
        c.rect(3, 10, 12, 13, T)
        c.hline(4, 11, 11, T[DK])
        c.hline(4, 11, 13, T[DK])
        c.rect(5, 2, 10, 2, M[HL], bevel=False)
    elif kind == "fiber":      # angular faceted helm with chin guard
        c.poly([(6, 1), (10, 1), (13, 5), (12, 11), (9, 14), (6, 14), (3, 11), (2, 5)], M)
        c.poly([(4, 5), (11, 5), (12, 7), (3, 7)], V or CYAN)
        c.hline(4, 11, 5, (200, 255, 255))
        c.poly([(6, 1), (9, 1), (8, 4), (7, 4)], T)
        c.rect(5, 10, 10, 13, T)
        c.pix(3, 8, T[HL])
        c.pix(12, 8, T[HL])
    elif kind == "hev":        # round sealed hood, big bubble visor
        c.ellipse(7.5, 7.5, 6, 6.5, M)
        c.ellipse(7.5, 6.5, 4, 3, V)
        c.hline(5, 9, 4, V[HL])
        c.vline(4, 5, 7, V[LT])
        c.hline(4, 11, 2, T[BS])                 # headband across the brow (worn: blue band, black glass)
        c.hline(3, 12, 3, T[SH])
        c.rect(5, 12, 10, 14, M[SH])
        c.hline(6, 9, 13, M[DK])
        c.pix(2, 8, M[HL])
        c.pix(13, 8, M[HL])
    elif kind == "steam":      # bronze pot helm, round porthole, steam pipe
        _dome(c, M)
        c.disc(7.5, 6.5, 2, V or ramp((250, 200, 80)))
        c.pix(6, 5, (255, 245, 190))
        c.rect(11, 0, 12, 4, A)
        c.pix(13, 0, (235, 235, 245))
        c.pix(14, 1, (205, 205, 215))
        c.rect(5, 9, 10, 10, T)
        c.stripes([(a, b) for a in range(5, 11) for b in (9, 10)], T[DK], 2, 0, False)
        for p in ((4, 3), (11, 3), (4, 8)):
            c.pix(p[0], p[1], T[HL])
    elif kind == "power":      # wide crested full helm, wide visor
        c.poly([(4, 2), (11, 2), (14, 4), (15, 11), (13, 14), (2, 14), (0, 11), (1, 4)], M)
        c.rect(7, 0, 8, 3, T)
        c.rect(3, 5, 12, 7, V or CYAN, bevel=False)
        c.hline(3, 12, 5, (210, 250, 255))
        c.rect(6, 8, 9, 13, T)
        c.rect(2, 9, 3, 12, T)
        c.rect(12, 9, 13, 12, T)
    elif kind == "exo":        # skeletal frame, side monocle
        c.hline(4, 11, 2, M[HL])
        c.poly([(4, 3), (11, 3), (12, 6), (3, 6)], M)
        c.vline(3, 6, 12, T[BS])
        c.vline(12, 6, 12, T[BS])
        c.hline(3, 12, 12, T[SH])
        c.vline(7, 6, 11, A[BS])
        c.rect(8, 7, 11, 9, V or glow((190, 90, 255)), bevel=False)
        c.pix(8, 7, (250, 220, 255))
    else:
        _dome(c, M)


def _sleeve_l(c, M, y1=10):
    c.poly([(1, 3), (4, 2), (4, y1), (2, y1 + 1), (1, y1)], M)


def _sleeve_r(c, M, y1=10):
    c.poly([(12, 2), (15, 3), (15, y1), (13, y1 + 1), (12, y1)], M)


def _torso(c, M, w0=4, w1=11, y1=13):
    c.poly([(w0, 2), (6, 2), (7, 3), (8, 3), (9, 2), (w1 + 1, 2), (w1 + 1, y1 + 1), (w0, y1 + 1)], M)


def _chest(c, kind, M, T, A, V, G):
    if kind == "bandit":       # sleeveless cloth vest, laced front, studs
        _torso(c, M, 3, 12, 12)
        c.rect(1, 3, 3, 5, M)
        c.rect(12, 3, 14, 5, M)
        c.vline(7, 4, 12, M[DK])
        c.vline(8, 4, 12, M[SH])
        for y in (5, 7, 9, 11):
            c.pix(6, y, A[HL])
            c.pix(9, y, A[HL])
        c.hline(3, 12, 12, A[BS])
    elif kind == "miner":      # denim jacket: flap pockets, cuffs
        _torso(c, M)
        _sleeve_l(c, M)
        _sleeve_r(c, M)
        c.vline(7, 3, 13, M[DK])
        c.vline(8, 3, 13, M[SH])
        for x in (4, 9):
            c.rect(x, 8, x + 2, 10, T[SH], bevel=False)
            c.hline(x, x + 2, 8, T[HL])
        yel = ramp((228, 188, 38))
        c.hline(1, 3, 10, yel[BS])
        c.hline(12, 14, 10, yel[BS])
        c.pix(7, 5, A[HL])
        c.pix(8, 7, A[HL])
    elif kind == "steel":      # steel breast + back plates, short cloth sleeves
        _torso(c, M)
        c.rect(1, 3, 3, 6, M)
        c.rect(12, 3, 14, 6, M)
        c.rect(5, 3, 10, 7, T)
        c.hline(5, 10, 5, T[DK])
        c.rect(5, 9, 10, 12, T)
        c.hline(5, 10, 10, T[DK])
        c.rect(2, 3, 3, 4, T)
        c.rect(12, 3, 13, 4, T)
    elif kind == "olive":      # padded epaulettes, chest rig with straps
        _torso(c, M)
        _sleeve_l(c, M, 11)
        _sleeve_r(c, M, 11)
        c.rect(0, 1, 3, 3, T)
        c.rect(12, 1, 15, 3, T)
        c.vline(5, 3, 13, A[BS])
        c.vline(10, 3, 13, A[BS])
        c.rect(6, 6, 9, 9, M[DK], bevel=False)
        c.hline(4, 11, 12, A[BS])
        c.hline(6, 9, 6, A[HL])
    elif kind == "commando":   # slim tactical vest: ammo pouches, shoulder straps
        _torso(c, M, 4, 11, 13)
        _sleeve_l(c, M, 9)
        _sleeve_r(c, M, 9)
        for x in (4, 7, 10):
            c.rect(x, 8, x + 1, 11, A)
            c.hline(x, x + 1, 8, A[HL])
        c.rect(5, 3, 6, 6, A)
        c.rect(9, 3, 10, 6, A)
        c.pix(2, 5, T[LT])
        c.pix(13, 5, T[LT])
    elif kind == "riot":       # long coat to the knee, lapels, shield plate
        c.poly([(4, 2), (6, 2), (7, 4), (8, 4), (9, 2), (12, 2), (14, 15), (1, 15)], M)
        _sleeve_l(c, M, 11)
        _sleeve_r(c, M, 11)
        c.rect(5, 3, 10, 7, T)
        c.vline(7, 8, 15, M[DK])
        c.vline(8, 8, 15, M[SH])
        c.pix(6, 10, A[HL])
        c.pix(9, 10, A[HL])
        c.hline(2, 13, 14, M[DK])
    elif kind == "fiber":      # angular chest, high collar, shoulder armor
        c.poly([(5, 1), (10, 1), (13, 3), (15, 6), (13, 7), (12, 14), (3, 14), (2, 7), (0, 6), (2, 3)], M)
        c.poly([(5, 1), (10, 1), (9, 4), (6, 4)], T)
        c.poly([(5, 5), (10, 5), (11, 9), (4, 9)], T)
        c.pix(7, 6, (200, 255, 255))
        c.pix(8, 6, (200, 255, 255))
        c.rect(0, 3, 2, 5, T)
        c.rect(13, 3, 15, 5, T)
    elif kind == "hev":        # bulky sealed suit, collar ring, chest hatch
        _torso(c, M, 3, 12, 13)
        _sleeve_l(c, M, 11)
        _sleeve_r(c, M, 11)
        c.rect(4, 2, 11, 3, T)                   # chest band (worn: orange across the collar)
        c.rect(5, 5, 10, 9, A)                   # vest panel, no hatch
        c.hline(5, 10, 5, A[HL])
        c.hline(3, 12, 11, T[BS])                # belt band
        c.hline(1, 3, 7, T[BS])                  # arm bands
        c.hline(12, 14, 7, T[BS])
    elif kind == "steam":      # bronze cuirass, shoulder pipes venting, gauge
        _torso(c, M)
        _sleeve_l(c, M, 9)
        _sleeve_r(c, M, 9)
        c.rect(5, 3, 10, 9, T)
        c.disc(7.5, 6.5, 1, ramp((235, 235, 230)))
        c.pix(7, 6, (30, 30, 30))
        c.rect(2, 0, 3, 3, A)
        c.rect(12, 0, 13, 3, A)
        c.pix(1, 0, (235, 235, 245))
        c.pix(14, 0, (235, 235, 245))
        c.hline(5, 10, 11, A[LT])
        c.hline(5, 10, 12, A[SH])
        c.rect(1, 9, 3, 10, T)
        c.rect(12, 9, 14, 10, T)
    elif kind == "power":      # massive pauldrons, armored torso, glowing core
        c.poly([(0, 1), (5, 0), (10, 0), (15, 1), (15, 6), (12, 7), (12, 14), (3, 14), (3, 7), (0, 6)], M)
        c.rect(0, 1, 3, 5, T)
        c.rect(12, 1, 15, 5, T)
        c.rect(5, 3, 10, 9, T)
        c.rect(7, 5, 8, 7, G or CYAN, bevel=False)
        c.hline(4, 11, 11, M[DK])
    elif kind == "exo":        # thin torso with outer frame struts and spine light
        _torso(c, M, 5, 10, 13)
        c.vline(3, 2, 13, T[BS])
        c.vline(12, 2, 13, T[BS])
        c.hline(3, 12, 2, T[LT])
        c.hline(3, 12, 7, T[SH])
        c.hline(3, 12, 13, T[SH])
        c.rect(7, 4, 8, 11, V or glow((190, 90, 255)), bevel=False)
        c.vline(1, 3, 8, T[SH])
        c.vline(14, 3, 8, T[SH])
    else:
        _torso(c, M)
        _sleeve_l(c, M)
        _sleeve_r(c, M)


def _leg_pair(c, M, xl0, xl1, xr0, xr1, y0=5, y1=13):
    c.rect(xl0, y0, xl1, y1, M)
    c.rect(xr0, y0, xr1, y1, M)


def _legs(c, kind, M, T, A, V, G):
    if kind == "bandit":       # ragged short trousers with rope belt
        c.rect(4, 2, 11, 4, M)
        _leg_pair(c, M, 4, 7, 8, 11, 5, 11)
        c.hline(4, 11, 3, A[BS])
        for x in (4, 6, 9, 11):
            c.pix(x, 12, M[SH])
        c.pix(5, 8, A[HL])
        c.pix(10, 8, A[HL])
    elif kind == "miner":      # overalls: bib, straps, turn-up cuffs
        c.rect(4, 1, 11, 4, M)
        _leg_pair(c, M, 4, 7, 8, 11)
        c.vline(5, 0, 2, A[BS])
        c.vline(10, 0, 2, A[BS])
        c.rect(4, 12, 7, 13, ramp((150, 110, 60)))
        c.rect(8, 12, 11, 13, ramp((150, 110, 60)))
        c.rect(6, 2, 9, 3, T[SH], bevel=False)
    elif kind == "steel":      # trousers with steel knee plates
        c.rect(4, 2, 11, 4, M)
        _leg_pair(c, M, 4, 7, 8, 11)
        for (a, b) in ((4, 7), (8, 11)):
            c.rect(a, 6, b, 8, T)
            c.hline(a, b, 12, T[SH])
        c.hline(4, 11, 3, T[LT])
    elif kind == "olive":      # wide cargo pants with side pouches
        c.rect(3, 2, 12, 4, M)
        _leg_pair(c, M, 3, 7, 8, 12)
        c.rect(1, 6, 3, 9, A)
        c.rect(12, 6, 14, 9, A)
        c.hline(1, 3, 6, A[HL])
        c.hline(12, 14, 6, A[HL])
        c.hline(3, 12, 3, T[LT])
        c.hline(3, 7, 11, T[BS])
        c.hline(8, 12, 11, T[BS])
    elif kind == "commando":   # slim legs, thigh holster, knee pads
        c.rect(5, 2, 10, 4, M)
        _leg_pair(c, M, 5, 7, 8, 10)
        c.rect(3, 7, 4, 10, A)
        c.rect(5, 8, 7, 9, T[LT], bevel=False)
        c.rect(8, 8, 10, 9, T[LT], bevel=False)
        c.hline(5, 10, 3, M[HL])
    elif kind == "riot":       # coat skirt flaps over armored shins
        c.poly([(2, 2), (13, 2), (15, 11), (8, 11), (8, 14), (7, 14), (7, 11), (0, 11)], M)
        c.rect(4, 12, 6, 14, T)
        c.rect(9, 12, 11, 14, T)
        c.vline(7, 3, 11, M[DK])
        c.vline(8, 3, 11, M[SH])
        c.hline(2, 13, 4, T[LT])
    elif kind == "fiber":      # angular faceted thigh and knee guards
        c.poly([(4, 2), (11, 2), (12, 5), (11, 7), (11, 13), (8, 13), (8, 8), (7, 8), (7, 13), (4, 13), (4, 7), (3, 5)], M)
        for (a, b) in ((4, 7), (8, 11)):
            c.poly([(a, 5), (b, 5), (b, 8), (a + 1, 9)], T)
            c.hline(a, b, 12, T[SH])
        c.pix(5, 7, (200, 255, 255))
        c.pix(10, 7, (200, 255, 255))
    elif kind == "hev":        # baggy sealed trousers with strap bands
        c.rect(3, 2, 12, 4, M)
        _leg_pair(c, M, 3, 7, 8, 12)
        for y in (7, 11):               # two orange bands (worn: charcoal legs, orange bands)
            c.hline(3, 7, y, T[BS])
            c.hline(8, 12, y, T[BS])
        c.hline(3, 12, 3, T[SH])
    elif kind == "steam":      # bronze greaves with piping
        c.rect(4, 2, 11, 4, M)
        _leg_pair(c, M, 4, 7, 8, 11)
        c.rect(4, 6, 7, 8, T)
        c.rect(8, 6, 11, 8, T)
        c.pix(5, 7, A[HL])
        c.pix(10, 7, A[HL])
        c.vline(3, 4, 12, A[BS])
        c.vline(12, 4, 12, A[BS])
        c.rect(4, 12, 7, 13, A)
        c.rect(8, 12, 11, 13, A)
    elif kind == "power":      # wide heavy plated legs, glowing hip
        c.rect(2, 2, 13, 5, M)
        _leg_pair(c, M, 2, 7, 8, 13, 6, 13)
        c.rect(2, 7, 7, 10, T)
        c.rect(8, 7, 13, 10, T)
        c.rect(7, 3, 8, 4, G or CYAN, bevel=False)
        c.hline(2, 7, 13, T[DK])
        c.hline(8, 13, 13, T[DK])
    elif kind == "exo":        # thin legs with outer struts and joint lights
        c.rect(5, 2, 10, 4, M)
        _leg_pair(c, M, 5, 6, 9, 10)
        c.vline(4, 4, 13, T[BS])
        c.vline(11, 4, 13, T[BS])
        c.hline(4, 6, 8, T[LT])
        c.hline(9, 11, 8, T[LT])
        lamp = V[HL] if V else (200, 130, 255)
        c.pix(5, 8, lamp)
        c.pix(10, 8, lamp)
    else:
        c.rect(4, 2, 11, 4, M)
        _leg_pair(c, M, 4, 7, 8, 11)


def _boot(c, ox, shaft_top, M, sole, toe_x=7):
    """One side-on boot: shaft from shaft_top, foot to y=13."""
    c.rect(2 + ox, shaft_top, 5 + ox, 9, M)
    c.poly([(1 + ox, 9), (6 + ox, 9), (toe_x + ox, 11), (toe_x + ox, 13), (1 + ox, 13)], M)
    c.rect(1 + ox, 12, toe_x + ox, 13, sole, bevel=False)
    c.hline(1 + ox, toe_x + ox, 13, BLACK[DK])


def _boots(c, kind, M, T, A, V, G):
    tops = {"bandit": 6, "miner": 2, "steel": 4, "olive": 4, "commando": 3, "riot": 3,
            "fiber": 4, "hev": 3, "steam": 4, "power": 2, "exo": 5}
    lamp = V[HL] if V else (200, 130, 255)
    for ox in (0, 7):
        top = tops.get(kind, 4)
        sole = A[SH] if kind != "hev" else T[SH]
        _boot(c, ox, top, M, sole)
        if kind == "bandit":        # ankle wraps
            c.hline(2 + ox, 5 + ox, 6, A[BS])
            c.pix(3 + ox, 8, A[HL])
            c.pix(4 + ox, 7, A[HL])
        elif kind == "miner":       # tall rubber waders, blue cuff
            c.rect(2 + ox, 2, 5 + ox, 3, ramp((50, 80, 150)))
            c.pix(3 + ox, 7, A[HL])
        elif kind == "steel":
            c.rect(2 + ox, 5, 5 + ox, 7, T)
            c.rect(6 + ox, 10, 7 + ox, 11, T)
        elif kind == "olive":
            c.hline(2 + ox, 5 + ox, 5, A[BS])
            c.hline(2 + ox, 5 + ox, 7, A[BS])
            c.pix(6 + ox, 10, T[HL])
        elif kind == "commando":
            c.hline(2 + ox, 5 + ox, 5, A[BS])
            c.hline(2 + ox, 5 + ox, 8, A[BS])
            c.vline(3 + ox, 3, 9, A[SH])
        elif kind == "riot":
            c.rect(2 + ox, 5, 5 + ox, 7, T)
            c.rect(6 + ox, 10, 7 + ox, 11, T)
        elif kind == "fiber":
            c.poly([(2 + ox, 5), (5 + ox, 5), (5 + ox, 8), (3 + ox, 8)], T)
            c.pix(6 + ox, 11, (200, 255, 255))
        elif kind == "hev":
            c.hline(2 + ox, 5 + ox, 6, T[BS])
            c.hline(2 + ox, 5 + ox, 8, M[DK])
            c.rect(6 + ox, 10, 7 + ox, 11, T)
        elif kind == "steam":
            c.rect(2 + ox, 5, 5 + ox, 6, T)
            c.rect(6 + ox, 10, 7 + ox, 11, T)
            c.pix(3 + ox, 8, A[HL])
            c.pix(4 + ox, 8, A[HL])
        elif kind == "power":
            c.rect(1 + ox, 3, 6 + ox, 7, T)
            c.pix(3 + ox, 5, G[HL] if G else (150, 255, 255))
        elif kind == "exo":
            c.vline(2 + ox, 5, 9, T[BS])
            c.pix(6 + ox, 10, lamp)
