"""Tiny pixel-art toolkit for GregGuns 16x16 item icons.

Procedural and deterministic: no randomness except via explicit seeded
``random.Random`` instances passed in by callers. Light comes from the top
left; every sprite gets a dark outline pass.

Concepts
  * ramp(rgb)       -> 5 shades [dark, shade, base, light, highlight]
  * Canvas.fill()   -> bevel-shaded fill of a pixel set (top/left lit,
                       bottom/right shadowed), the GT-style look
  * part helpers    -> barrels, receivers, stocks, magazines, scopes, grips,
                       bullets, shells, plates, emitters, cloth, armour
                       silhouettes, tanks, canisters
Sprites are drawn inside 1..14 so the outline fits inside the 16x16 frame.
"""
from PIL import Image

W = H = 16


# ---------------------------------------------------------------- colours
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
POLY = ramp((82, 86, 98), 0.8)
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
        """cx, cy may be half-integers (centre between pixels)."""
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

        Outline colour is the adjacent pixel darkened, so warm parts get warm
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
    c.poly(pts, rp)


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
    """cx, cy are pixel-centres (ints). r = radius in pixels (>=2)."""
    import math
    cells = set(c.ellipse_coords(cx, cy, r - 0.5, r - 0.5))
    for i in range(teeth):                       # notch the rim between teeth
        a = 2 * math.pi * (i + 0.5) / teeth
        cells.discard((int(round(cx + math.cos(a) * (r - 0.4))), int(round(cy + math.sin(a) * (r - 0.4)))))
    c.fill(cells, rp)
    c.pix(cx, cy, rp[DK])


def spring(c, x0, x1, y, rp):
    for i, x in enumerate(range(x0, x1 + 1)):
        c.pix(x, y + (i % 2), rp[HL] if i % 2 == 0 else rp[BS])


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


# ---------------------------------------------------------------- armour
def helmet_shape(c, rp, flare=True):
    c.poly([(5, 2), (11, 2), (12, 4), (13, 9), (11, 9), (11, 7), (5, 7), (5, 9), (3, 9), (4, 4)], rp)


def armour_piece(c, slot, cw):
    """Draw one armour slot silhouette with per-set details.

    cw keys: main, trim (ramps); acc (ramp, small details); vis (ramp, visor);
    kind (str) selecting a style variant; glow (ramp, emissive) optional.
    """
    kind = cw["kind"]
    M, T, A = cw["main"], cw["trim"], cw.get("acc", cw["trim"])
    V = cw.get("vis")
    G = cw.get("glow")
    if slot == "helmet":
        _helmet(c, kind, M, T, A, V, G)
    elif slot == "chestplate":
        _chest(c, kind, M, T, A, V, G)
    elif slot == "leggings":
        _legs(c, kind, M, T, A, V, G)
    else:
        _boots(c, kind, M, T, A, V, G)


def _helmet(c, kind, M, T, A, V, G):
    if kind == "bandit":       # cloth hood with face mask and studs
        c.poly([(5, 2), (10, 2), (12, 4), (13, 12), (11, 13), (11, 9), (4, 9), (4, 13), (2, 12), (3, 4)], M)
        c.rect(5, 6, 10, 8, T if False else BLACK)               # mask band over eyes/face
        c.rect(5, 5, 10, 5, M[SH], bevel=False)
        c.hline(6, 9, 7, (240, 220, 190))                          # eye slit
        for x in (4, 8, 11):
            c.pix(x, 3, A[HL])
    elif kind == "miner":      # yellow hard hat with lamp
        c.poly([(4, 3), (11, 3), (12, 6), (14, 7), (14, 9), (2, 9), (2, 7), (3, 6)], M)
        c.rect(7, 2, 8, 3, M)
        c.rect(7, 5, 8, 6, CYAN if False else glow((255, 240, 150)), bevel=False)
        c.rect(7, 7, 8, 7, T[SH], bevel=False)
        c.hline(3, 12, 9, M[DK])
        c.pix(5, 5, T[HL]); c.pix(10, 5, T[HL])                    # rivets
        c.rect(4, 10, 11, 12, ramp((50, 80, 150)))                 # blue canvas hood below
        c.rect(6, 10, 9, 11, (200, 160, 130), bevel=False) if False else None
    elif kind in ("steel", "olive"):  # classic dome helmet with cheek plates
        helmet_shape(c, M)
        c.hline(5, 11, 3, M[HL])
        c.rect(6, 5, 9, 5, T[BS], bevel=False)                     # trim band
        c.rect(5, 8, 10, 10, (200, 150, 120)) if False else None
        c.vline(7, 6, 9, T[LT]) if kind == "steel" else None
        c.hline(4, 11, 7, T[LT])
        c.pix(4, 10, M[SH]); c.pix(11, 10, M[SH])
    elif kind == "commando":   # balaclava + goggles, near-black
        c.poly([(4, 2), (11, 2), (12, 4), (12, 11), (10, 13), (5, 13), (3, 11), (3, 4)], M)
        c.rect(4, 6, 11, 7, A, bevel=False)
        c.hline(5, 10, 6, V[HL] if V else (120, 200, 255))
        c.hline(5, 10, 7, V[BS] if V else (60, 120, 180))
        c.hline(4, 11, 3, M[HL])
        c.vline(7, 9, 12, M[DK]); c.vline(8, 9, 12, M[DK])
    elif kind == "riot":       # visored full helmet with shield plates
        c.poly([(4, 2), (11, 2), (13, 4), (13, 11), (11, 13), (4, 13), (2, 11), (2, 4)], M)
        c.rect(4, 5, 11, 8, V or GLASS, bevel=False)
        c.hline(4, 11, 5, (200, 235, 245))
        c.hline(4, 11, 8, (60, 90, 110))
        c.rect(3, 9, 5, 12, T)
        c.rect(10, 9, 12, 12, T)
        c.hline(5, 10, 3, M[HL])
    elif kind == "fibre":      # sleek advanced combat helmet with titanium panels
        c.poly([(5, 2), (10, 2), (12, 4), (12, 10), (11, 12), (4, 12), (3, 10), (3, 4)], M)
        c.rect(5, 5, 10, 7, V or CYAN, bevel=False)
        c.hline(5, 10, 5, V[HL] if V else (200, 255, 255))
        c.rect(4, 8, 5, 11, T); c.rect(10, 8, 11, 11, T)
        c.rect(6, 2, 9, 3, T)
    elif kind == "hev":        # sealed hood with wide glass visor
        c.poly([(4, 2), (11, 2), (13, 4), (13, 11), (11, 13), (4, 13), (2, 11), (2, 4)], M)
        c.rect(4, 4, 11, 8, V, bevel=False)
        c.hline(4, 11, 4, V[HL]); c.vline(4, 4, 8, V[LT])
        c.rect(6, 10, 9, 12, T)
        c.hline(7, 8, 11, T[DK])
        c.pix(3, 3, T[HL]); c.pix(12, 3, T[HL])
    elif kind == "steam":      # bronze pot helm, pipe + vent
        helmet_shape(c, M)
        c.rect(5, 5, 10, 6, V or ramp((250, 200, 80)), bevel=False)
        c.hline(5, 10, 5, (255, 240, 170))
        c.rect(11, 1, 12, 4, A)
        c.pix(12, 0, (235, 235, 245)); c.pix(13, 1, (200, 200, 210))
        c.rect(5, 8, 10, 9, T)
        c.stripes([(a, b) for a in range(5, 11) for b in (8, 9)], T[DK], 2, 0, False)
        for x in (5, 7, 9):
            c.pix(x, 3, T[HL])
    elif kind == "power":      # bulky full helm, glass visor, crest
        c.poly([(4, 1), (11, 1), (13, 3), (14, 12), (12, 14), (3, 14), (1, 12), (2, 3)], M)
        c.rect(4, 5, 11, 7, V or CYAN, bevel=False)
        c.hline(4, 11, 5, (210, 250, 255))
        c.rect(7, 1, 8, 4, T)
        c.rect(3, 9, 5, 12, T); c.rect(10, 9, 12, 12, T)
        c.hline(6, 9, 10, M[DK]); c.hline(6, 9, 12, M[DK])
    elif kind == "exo":        # open-faced frame helm with glowing monocle
        helmet_shape(c, M)
        c.rect(4, 4, 11, 5, T[SH], bevel=False)
        c.rect(7, 4, 11, 5, V or glow((190, 90, 255)), bevel=False)
        c.hline(4, 6, 4, T[HL])
        c.vline(7, 6, 10, A[BS]); c.hline(5, 10, 10, A[LT])
        c.pix(4, 10, M[DK]); c.pix(11, 10, M[DK])
    else:
        helmet_shape(c, M)


def _chest(c, kind, M, T, A, V, G):
    def torso(sleeves=True, y1=13, w0=4, w1=11):
        pts = [(w0, 2), (6, 2), (7, 3), (8, 3), (9, 2), (w1 + 1, 2), (w1 + 1, y1 + 1), (w0, y1 + 1)]
        c.poly(pts, M)
        if sleeves:
            c.poly([(1, 3), (4, 2), (4, 10), (2, 11), (1, 10)], M)
            c.poly([(12, 2), (15, 3), (15, 10), (13, 11), (12, 10)], M)

    if kind == "bandit":       # sleeveless cloth vest, studs, lacing
        torso(sleeves=False, y1=13, w0=3, w1=12)
        c.rect(1, 3, 3, 6, M); c.rect(12, 3, 14, 6, M)
        c.vline(7, 4, 13, M[DK]); c.vline(8, 4, 13, M[SH])
        for y in (5, 8, 11):
            c.pix(6, y, A[HL]); c.pix(9, y, A[HL])
        c.hline(4, 11, 12, A[BS])
    elif kind == "miner":      # denim jacket with pocket and rivets
        torso()
        c.vline(7, 3, 13, M[DK]); c.vline(8, 3, 13, M[SH])
        c.rect(4, 8, 6, 10, T[SH], bevel=False); c.hline(4, 6, 8, T[HL])
        c.rect(9, 8, 11, 10, T[SH], bevel=False); c.hline(9, 11, 8, T[HL])
        c.pix(7, 5, A[HL]); c.pix(8, 7, A[HL])
        c.hline(1, 3, 10, M[DK]); c.hline(12, 14, 10, M[DK])
    elif kind in ("steel", "olive", "fibre"):  # plate carrier over cloth
        torso()
        c.rect(5, 3, 10, 9, T)
        c.rect(5, 10, 10, 12, T[SH] and T)
        c.hline(5, 10, 6, T[DK])
        c.vline(7, 4, 9, T[DK]) if kind == "steel" else None
        if kind == "olive":
            c.hline(5, 10, 11, A[BS])
        if kind == "fibre":
            c.rect(2, 3, 3, 4, T); c.rect(12, 3, 13, 4, T)
            c.pix(7, 5, (200, 255, 255)); c.pix(8, 5, (200, 255, 255))
    elif kind == "commando":   # tactical vest with pouches
        torso()
        c.rect(5, 3, 10, 7, A)
        for x in (5, 9):
            c.rect(x, 8, x + 1, 11, A)
            c.hline(x, x + 1, 8, A[HL])
        c.vline(7, 3, 13, M[DK])
        c.pix(2, 5, T[LT]); c.pix(13, 5, T[LT])
    elif kind == "riot":       # long coat with shield plates
        pts = [(4, 2), (6, 2), (7, 3), (8, 3), (9, 2), (12, 2), (13, 14), (2, 14)]
        c.poly(pts, M)
        c.poly([(1, 3), (4, 2), (4, 11), (2, 12), (1, 11)], M)
        c.poly([(12, 2), (15, 3), (15, 11), (13, 12), (12, 11)], M)
        c.rect(5, 3, 10, 7, T)
        c.vline(7, 8, 14, M[DK]); c.vline(8, 8, 14, M[SH])
        c.rect(2, 3, 3, 4, T); c.rect(12, 3, 13, 4, T)
        c.pix(6, 10, A[HL]); c.pix(9, 10, A[HL])
    elif kind == "hev":        # sealed suit with chest hatch and seams
        torso()
        c.rect(5, 4, 10, 8, T)
        c.rect(6, 5, 9, 7, V or glow((255, 190, 60)), bevel=False)
        c.hline(4, 11, 10, M[DK]); c.hline(4, 11, 12, M[DK])
        c.hline(1, 3, 7, M[DK]); c.hline(12, 14, 7, M[DK])
    elif kind == "steam":      # bronze breastplate with pipes and gauge
        torso()
        c.rect(5, 3, 10, 9, T)
        c.disc(7.5, 6.5, 1, ramp((235, 235, 230)))
        c.pix(7, 6, (30, 30, 30))
        c.vline(3, 3, 10, A[BS]); c.vline(12, 3, 10, A[BS])
        c.hline(5, 10, 11, A[LT]); c.hline(5, 10, 12, A[SH])
        c.pix(2, 2, (230, 230, 240)); c.pix(13, 2, (230, 230, 240))
        c.hline(2, 3, 3, T[HL]); c.hline(12, 13, 3, T[HL])
    elif kind == "power":      # heavy pauldrons + glowing core
        c.poly([(0, 2), (4, 1), (11, 1), (15, 2), (15, 6), (12, 6), (12, 14), (3, 14), (3, 6), (0, 6)], M)
        c.rect(0, 2, 3, 5, T); c.rect(12, 2, 15, 5, T)
        c.rect(5, 3, 10, 9, T)
        c.rect(7, 5, 8, 7, G or CYAN, bevel=False)
        c.hline(4, 11, 11, M[DK])
    elif kind == "exo":        # frame struts over undersuit
        torso()
        c.vline(5, 3, 12, T[BS]); c.vline(10, 3, 12, T[BS])
        c.hline(5, 10, 3, T[LT]); c.hline(5, 10, 12, T[SH])
        c.rect(7, 6, 8, 8, V or glow((190, 90, 255)), bevel=False)
        c.pix(2, 4, T[HL]); c.pix(13, 4, T[HL])
    else:
        torso()


def _legs(c, kind, M, T, A, V, G):
    wide = kind in ("power", "riot")
    xl0, xl1, xr0, xr1 = (3, 7, 8, 12) if wide else (4, 7, 8, 11)
    if kind == "riot":
        c.poly([(2, 2), (13, 2), (14, 13), (8, 13), (8, 5), (7, 5), (7, 13), (1, 13)], M)
    else:
        c.rect(xl0, 2, xr1, 4, M)                      # waist
        c.rect(xl0, 5, xl1, 13, M)
        c.rect(xr0, 5, xr1, 13, M)
    c.hline(xl0, xr1, 3, T[LT] if kind not in ("commando",) else M[HL])
    if kind == "bandit":
        c.pix(5, 8, A[HL]); c.pix(10, 8, A[HL]); c.hline(xl0, xr1, 4, A[BS])
        c.hline(xl0, xl1, 12, A[SH]); c.hline(xr0, xr1, 12, A[SH])
    elif kind == "miner":
        c.rect(xl0, 7, xl1, 9, T[SH], bevel=False) if False else None
        c.hline(xl0, xl1, 8, M[DK]); c.hline(xr0, xr1, 8, M[DK])
        c.pix(xl0 + 1, 3, A[HL]); c.pix(xr1 - 1, 3, A[HL])
        c.rect(xl0, 11, xl1, 13, T[SH] if False else ramp((150, 110, 60)))
        c.rect(xr0, 11, xr1, 13, ramp((150, 110, 60)))
    elif kind in ("steel", "olive", "fibre"):
        for (a, b) in ((xl0, xl1), (xr0, xr1)):
            c.rect(a, 6, b, 8, T)
            c.hline(a, b, 12, T[SH])
        if kind == "fibre":
            c.pix(5, 7, (200, 255, 255)); c.pix(10, 7, (200, 255, 255))
    elif kind == "commando":
        for (a, b) in ((xl0, xl1), (xr0, xr1)):
            c.rect(a, 7, b, 9, A)
            c.hline(a, b, 7, A[HL])
        c.rect(xl0, 11, xl1, 11, T[LT], bevel=False); c.rect(xr0, 11, xr1, 11, T[LT], bevel=False)
    elif kind == "riot":
        c.rect(3, 6, 6, 8, T); c.rect(9, 6, 12, 8, T)
        c.vline(7, 6, 13, M[DK])
    elif kind == "hev":
        for (a, b) in ((xl0, xl1), (xr0, xr1)):
            c.hline(a, b, 8, M[DK])
            c.rect(a, 6, b, 7, T)
        c.hline(xl0, xr1, 4, M[DK])
    elif kind == "steam":
        for (a, b) in ((xl0, xl1), (xr0, xr1)):
            c.rect(a, 6, b, 8, T)
            c.hline(a, b, 12, A[BS])
        c.vline(xl0, 9, 12, A[BS]); c.vline(xr1, 9, 12, A[BS])
        c.pix(5, 4, (230, 230, 240))
    elif kind == "power":
        for (a, b) in ((xl0, xl1), (xr0, xr1)):
            c.rect(a, 6, b, 9, T)
            c.hline(a, b, 12, T[DK])
        c.rect(7, 4, 8, 5, G or CYAN, bevel=False)
    elif kind == "exo":
        c.vline(xl0, 5, 13, T[BS]); c.vline(xr1, 5, 13, T[BS])
        c.hline(xl0, xl1, 8, T[LT]); c.hline(xr0, xr1, 8, T[LT])
        c.pix(5, 8, V[HL] if V else (200, 130, 255)); c.pix(10, 8, V[HL] if V else (200, 130, 255))
    if kind != "riot":
        c.vline(xl1 + 0 if False else 7, 6, 13, M[DK]) if False else None
        c.vline(7, 5, 13, M[DK])
        c.vline(8, 5, 13, M[SH])


def _boots(c, kind, M, T, A, V, G):
    # pair of boots, side-on; left faces left, right faces right? both face right-ish
    for ox in (0, 7):
        shaft_h = 3 if kind in ("bandit", "miner") else 4
        c.rect(2 + ox, 3, 5 + ox, 9, M)                # shaft
        c.poly([(1 + ox, 9), (6 + ox, 9), (7 + ox, 11), (7 + ox, 13), (1 + ox, 13)], M)  # foot
        c.rect(1 + ox, 12, 7 + ox, 13, A[SH] if kind not in ("hev",) else T[SH], bevel=False)  # sole
        c.hline(1 + ox, 7 + ox, 13, BLACK[DK])
        c.hline(2 + ox, 5 + ox, 3, T[LT] if kind not in ("commando",) else M[HL])
        if kind == "bandit":
            c.pix(3 + ox, 5, A[HL]); c.pix(3 + ox, 7, A[HL])
        elif kind == "miner":
            c.rect(2 + ox, 3, 5 + ox, 4, ramp((50, 80, 150)))
            c.pix(3 + ox, 8, A[HL])
        elif kind in ("steel", "olive", "fibre"):
            c.rect(2 + ox, 5, 5 + ox, 7, T)
            c.rect(6 + ox, 10, 7 + ox, 11, T)
        elif kind == "commando":
            c.hline(2 + ox, 5 + ox, 6, A[BS]); c.hline(2 + ox, 5 + ox, 8, A[BS])
        elif kind == "riot":
            c.rect(2 + ox, 5, 5 + ox, 6, T)
            c.rect(6 + ox, 10, 7 + ox, 11, T)
        elif kind == "hev":
            c.hline(2 + ox, 5 + ox, 8, M[DK]); c.rect(6 + ox, 10, 7 + ox, 11, T)
        elif kind == "steam":
            c.rect(2 + ox, 5, 5 + ox, 6, T)
            c.pix(3 + ox, 8, A[HL]); c.pix(4 + ox, 8, A[HL]); c.rect(6 + ox, 10, 7 + ox, 11, T)
        elif kind == "power":
            c.rect(1 + ox, 4, 6 + ox, 7, T)
            c.pix(3 + ox, 5, G[HL] if G else (150, 255, 255))
        elif kind == "exo":
            c.vline(2 + ox, 4, 8, T[BS]); c.pix(6 + ox, 10, V[HL] if V else (200, 130, 255))
