"""Pixel-art helpers for the GregGuns block and particle generators (blocks.py, fx.py).

Everything here is deterministic: callers pass a numpy Generator seeded from the asset id
(see seed_for). No external art is read; all textures are painted from code.
"""
from __future__ import annotations

import zlib
from typing import Iterable, Sequence

import numpy as np
from PIL import Image

Color = tuple  # (r, g, b) or (r, g, b, a)


def seed_for(asset_id: str) -> int:
    """Stable 32-bit seed derived from the manifest id."""
    return zlib.crc32(asset_id.encode("utf-8")) & 0xFFFFFFFF


def rng_for(asset_id: str) -> np.random.Generator:
    return np.random.default_rng(seed_for(asset_id))


def hx(s: str) -> tuple:
    s = s.lstrip("#")
    if len(s) == 3:
        s = "".join(ch * 2 for ch in s)
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def shade(c: Sequence[int], f: float) -> tuple:
    """Multiply brightness (f<1 darker, f>1 lighter, lightening blends toward white a little)."""
    r, g, b = c[:3]
    if f <= 1:
        return (int(r * f), int(g * f), int(b * f))
    t = f - 1
    return (min(255, int(r + (255 - r) * t * 0.9)), min(255, int(g + (255 - g) * t * 0.9)),
            min(255, int(b + (255 - b) * t * 0.9)))


def mix(a: Sequence[int], b: Sequence[int], t: float) -> tuple:
    return tuple(int(round(a[i] * (1 - t) + b[i] * t)) for i in range(3))


def ramp(base: Sequence[int]) -> list:
    """Five GT-style tones from outline to highlight, built around a base colour."""
    return [shade(base, 0.38), shade(base, 0.68), tuple(base[:3]), shade(base, 1.18), shade(base, 1.4)]


class Canvas:
    """RGBA pixel canvas with small drawing primitives. Origin top-left, rects are [x0,x1) x [y0,y1)."""

    def __init__(self, w: int, h: int | None = None, fill: Color | None = None):
        h = w if h is None else h
        self.w, self.h = w, h
        self.a = np.zeros((h, w, 4), np.uint8)
        if fill is not None:
            self.rect(0, 0, w, h, fill)

    # -- primitives -------------------------------------------------
    @staticmethod
    def _c(col: Color, a: int | None = None) -> tuple:
        if len(col) == 3:
            return (col[0], col[1], col[2], 255 if a is None else a)
        return tuple(col) if a is None else (col[0], col[1], col[2], a)

    def px(self, x: int, y: int, col: Color, a: int | None = None) -> None:
        if 0 <= x < self.w and 0 <= y < self.h:
            self.a[y, x] = self._c(col, a)

    def blend(self, x: int, y: int, col: Color, a: int) -> None:
        """Alpha-composite one pixel over the canvas."""
        if not (0 <= x < self.w and 0 <= y < self.h):
            return
        dst = self.a[y, x].astype(float)
        sa = a / 255.0
        da = dst[3] / 255.0
        oa = sa + da * (1 - sa)
        if oa <= 0:
            return
        for i in range(3):
            dst[i] = (col[i] * sa + dst[i] * da * (1 - sa)) / oa
        dst[3] = oa * 255
        self.a[y, x] = np.clip(np.rint(dst), 0, 255).astype(np.uint8)

    def rect(self, x0: int, y0: int, x1: int, y1: int, col: Color, a: int | None = None) -> None:
        x0c, y0c, x1c, y1c = max(x0, 0), max(y0, 0), min(x1, self.w), min(y1, self.h)
        if x1c > x0c and y1c > y0c:
            self.a[y0c:y1c, x0c:x1c] = self._c(col, a)

    def frame(self, x0: int, y0: int, x1: int, y1: int, col: Color) -> None:
        self.hline(x0, x1, y0, col)
        self.hline(x0, x1, y1 - 1, col)
        self.vline(x0, y0, y1, col)
        self.vline(x1 - 1, y0, y1, col)

    def hline(self, x0: int, x1: int, y: int, col: Color) -> None:
        self.rect(x0, y, x1, y + 1, col)

    def vline(self, x: int, y0: int, y1: int, col: Color) -> None:
        self.rect(x, y0, x + 1, y1, col)

    def bevel(self, x0: int, y0: int, x1: int, y1: int, hi: Color, lo: Color) -> None:
        """Top-left light edge, bottom-right shadow edge (1 px)."""
        self.hline(x0, x1 - 1, y0, hi)
        self.vline(x0, y0, y1 - 1, hi)
        self.hline(x0 + 1, x1, y1 - 1, lo)
        self.vline(x1 - 1, y0 + 1, y1, lo)

    def disc(self, cx: float, cy: float, r: float, col: Color) -> None:
        yy, xx = np.mgrid[0:self.h, 0:self.w]
        m = (xx + 0.5 - cx) ** 2 + (yy + 0.5 - cy) ** 2 <= r * r
        self.a[m] = self._c(col)

    def ring(self, cx: float, cy: float, r0: float, r1: float, col: Color) -> None:
        yy, xx = np.mgrid[0:self.h, 0:self.w]
        d = (xx + 0.5 - cx) ** 2 + (yy + 0.5 - cy) ** 2
        self.a[(d <= r1 * r1) & (d >= r0 * r0)] = self._c(col)

    def speckle(self, x0: int, y0: int, x1: int, y1: int, cols: Sequence[Color], rng: np.random.Generator,
                density: float = 0.2) -> None:
        for y in range(max(y0, 0), min(y1, self.h)):
            for x in range(max(x0, 0), min(x1, self.w)):
                if self.a[y, x, 3] and rng.random() < density:
                    self.a[y, x] = self._c(cols[int(rng.integers(len(cols)))])

    def paste(self, other: "Canvas", x: int, y: int, mask_alpha: bool = True) -> None:
        for yy in range(other.h):
            for xx in range(other.w):
                p = other.a[yy, xx]
                if p[3] == 0:
                    continue
                if p[3] == 255 or not mask_alpha:
                    self.px(x + xx, y + yy, tuple(p))
                else:
                    self.blend(x + xx, y + yy, tuple(p[:3]), int(p[3]))

    def crop(self, x0: int, y0: int, x1: int, y1: int) -> "Canvas":
        c = Canvas(x1 - x0, y1 - y0)
        c.a[:] = self.a[y0:y1, x0:x1]
        return c

    def copy(self) -> "Canvas":
        c = Canvas(self.w, self.h)
        c.a[:] = self.a
        return c

    def image(self) -> Image.Image:
        return Image.fromarray(self.a, "RGBA")


# ---------------------------------------------------------------- noise


def fbm(w: int, h: int, rng: np.random.Generator, cells: int = 4, octaves: int = 3) -> np.ndarray:
    """Smooth value noise in 0..1 (float32), tileable-free. cells = lattice cells of the first octave."""
    out = np.zeros((h, w), np.float32)
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        n = cells * (2 ** o)
        g = rng.random((n + 1, n + 1)).astype(np.float32)
        im = Image.fromarray(g, "F").resize((w, h), Image.BICUBIC)
        out += amp * np.asarray(im, np.float32)
        tot += amp
        amp *= 0.5
    out /= tot
    lo, hi = float(out.min()), float(out.max())
    return (out - lo) / max(hi - lo, 1e-6)


def tile_noise(n: int, rng: np.random.Generator, cells: int = 4) -> np.ndarray:
    """Seamlessly tileable value noise (n x n, 0..1) by wrapping the lattice."""
    g = rng.random((cells, cells)).astype(np.float32)
    big = np.tile(g, (3, 3))
    im = Image.fromarray(big, "F").resize((n * 3, n * 3), Image.BICUBIC)
    a = np.asarray(im, np.float32)[n:2 * n, n:2 * n]
    lo, hi = float(a.min()), float(a.max())
    return (a - lo) / max(hi - lo, 1e-6)


def quantize_ramp(field: np.ndarray, cols: Sequence[Color]) -> np.ndarray:
    """Map a 0..1 field to the nearest of len(cols) flat colours -> (h,w,3) uint8."""
    idx = np.clip((field * len(cols)).astype(int), 0, len(cols) - 1)
    pal = np.array([c[:3] for c in cols], np.uint8)
    return pal[idx]


# ---------------------------------------------------------------- animation helpers


def strip(frames: Iterable[Canvas]) -> Canvas:
    fr = list(frames)
    w, h = fr[0].w, fr[0].h
    out = Canvas(w, h * len(fr))
    for i, f in enumerate(fr):
        out.a[i * h:(i + 1) * h] = f.a
    return out


def mcmeta(frametime: int) -> str:
    return '{"animation":{"frametime":%d}}\n' % frametime


def upscale(img: Image.Image, k: int) -> Image.Image:
    return img.resize((img.width * k, img.height * k), Image.NEAREST)
