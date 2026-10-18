"""Particle and FX sprite sheets for GregGuns (particle.* manifest entries).

All sheets are painted from analytic fields plus seeded value noise. Two output flavours, as the
manifest brief says: additive sheets have an opaque black background (RGB carries the light), alpha
sheets are transparent. Each painter returns a PIL RGBA image of exactly the manifest size.
"""
from __future__ import annotations

import numpy as np
from PIL import Image

from blocktex import fbm, hx, tile_noise

F32 = np.float32


# ---------------------------------------------------------------- field helpers
def grid(n: int, w: int | None = None):
    """Centred coordinates in [-1, 1]: returns U, V, R, TH for an (h=n, w) cell."""
    w = n if w is None else w
    v = (np.arange(n, dtype=F32) + 0.5) / n * 2 - 1
    u = (np.arange(w, dtype=F32) + 0.5) / w * 2 - 1
    U, V = np.meshgrid(u, v)
    return U, V, np.sqrt(U * U + V * V), np.arctan2(V, U)


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0 + 1e-9), 0, 1)
    return t * t * (3 - 2 * t)


def glow(r, radius, power=2.0):
    return np.exp(-np.power(np.maximum(r, 0) / radius, power)).astype(F32)


def heat_ramp(i, stops):
    """Map intensity (any shape, 0..1) through (pos, '#hex') stops -> (...,3) float 0..255."""
    pos = [s[0] for s in stops]
    cols = np.array([hx(s[1]) for s in stops], F32)
    out = np.empty(i.shape + (3,), F32)
    for k in range(3):
        out[..., k] = np.interp(np.clip(i, 0, 1), pos, cols[:, k])
    return out


FIRE = [(0.0, "#000000"), (0.12, "#3a0600"), (0.3, "#b02404"), (0.5, "#ff6a10"), (0.72, "#ffc040"), (0.9, "#fff0b0"),
        (1.0, "#ffffff")]


def tint(col: str, i):
    """Light of a single hue: white core fading to the colour then black."""
    c = np.array(hx(col), F32)
    i = np.clip(i, 0, 1)[..., None]
    base = c * np.clip(i * 1.4, 0, 1)
    white = np.clip((i - 0.6) / 0.4, 0, 1) * (255 - c)
    return base + white


def edge_fade(fr, frac=0.1):
    """Fade a cell to nothing over its outer 10 percent (and zero the outermost 2 px) so animated grid
    sheets never bleed light or opacity into the neighbouring cell."""
    h, w = fr.shape[:2]
    yy = np.minimum(np.arange(h), h - 1 - np.arange(h))[:, None].astype(F32)
    xx = np.minimum(np.arange(w), w - 1 - np.arange(w))[None, :].astype(F32)
    d = np.minimum(yy, xx)
    m = np.clip((d - 1.5) / max(frac * min(h, w), 1), 0, 1)
    m = m * m * (3 - 2 * m)
    out = fr.astype(F32).copy()
    if fr.shape[2] == 4:
        out[..., 3] = out[..., 3] * m
    else:
        out = out * m[..., None]
    return out


def sheet(size: int, cell: int, cols: int, frames, additive: bool, count: int | None = None, h: int | None = None):
    """Assemble frame arrays into a sheet. frames: list of (cell,cell,4 or 3) float arrays 0..255."""
    H = size if h is None else h
    out = np.zeros((H, size, 4), F32)
    if additive:
        out[..., 3] = 255
    for idx, fr in enumerate(frames):
        cx, cy = (idx % cols) * cell, (idx // cols) * cell
        fh, fw = fr.shape[0], fr.shape[1]
        if len(frames) > 1:
            fr = edge_fade(fr)
        if fr.shape[2] == 3:
            out[cy:cy + fh, cx:cx + fw, :3] = fr
            out[cy:cy + fh, cx:cx + fw, 3] = 255
        else:
            out[cy:cy + fh, cx:cx + fw] = fr
    return Image.fromarray(np.clip(np.rint(out), 0, 255).astype(np.uint8), "RGBA")


def rgba(rgb, a):
    return np.concatenate([np.clip(rgb, 0, 255), np.clip(a, 0, 1)[..., None] * 255], axis=-1)


# ================================================================ muzzle flashes
def star_cell(n, t, rng, col, spikes=4, rot=0.0, ratio=0.15):
    """Muzzle-flash star: bright core plus `spikes` tapering blades; grows then fades over t in 0..1."""
    U, V, R, TH = grid(n)
    grow = 0.45 + 0.55 * np.sin(min(t * 1.6, 1.0) * np.pi / 2)
    fade = 1.0 - smooth(0.45, 1.0, t)
    jit = 0.88 + 0.24 * rng.random()
    ang = TH - rot - rng.random() * 0.12
    # blade profile: narrow in angle, long in radius
    blade = np.zeros_like(R)
    for k in range(spikes):
        d = np.angle(np.exp(1j * (ang - 2 * np.pi * k / spikes)))
        width = ratio * (1.0 - np.clip(R / (0.95 * grow * jit), 0, 1)) + 0.012
        blade = np.maximum(blade, np.exp(-(d / width) ** 2) * np.clip(1 - R / (0.95 * grow * jit), 0, 1) ** 1.2)
    core = glow(R, 0.22 * grow, 1.6)
    halo = 0.35 * glow(R, 0.5 * grow, 2.0)
    i = np.clip((blade * 0.95 + core + halo) * fade, 0, 1)
    return tint(col, i)


def muzzleflash2(rng):
    frames = [star_cell(64, f / 7, rng, "#f0d050", 4, 0.0, 0.14) for f in range(8)]
    # add diagonal minor blades on the early frames for a spiky, hand-painted feel
    return sheet(256, 64, 4, frames, True)


def muzzleflash_blue(rng):
    frames = [star_cell(64, f / 7, rng, "#9cb5ff", 3, -np.pi / 2, 0.2) for f in range(8)]
    return sheet(256, 64, 4, frames, True)


def muzzleflash4x4(rng):
    frames = []
    for f in range(10):
        n = 64
        t = f / 9
        U, V, R, TH = grid(n)
        nz = fbm(n, n, rng, 4, 3)
        rad = 0.35 + 0.55 * np.sin(min(t * 1.5, 1) * np.pi / 2)
        d = R + (nz - 0.5) * 0.5 * (0.4 + t)
        puff = smooth(rad, rad * 0.35, d)
        jet = np.exp(-((V / (0.2 + 0.1 * t)) ** 2)) * np.clip(U * 1.1 + 0.2, 0, 1) * np.clip(1 - (U) / (1.0 + 0.0), 0, 1)
        i = np.clip((puff * (0.9 - 0.45 * t) + 0.5 * jet * (1 - t)) , 0, 1) * (1 - smooth(0.6, 1.0, t))
        # orange explosive puff: shift ramp toward red for lower intensity
        frames.append(heat_ramp(i * 1.0, [(0, "#000000"), (0.2, "#5a1204"), (0.45, "#f07050"), (0.75, "#ffb060"),
                                           (1.0, "#ffffff")]))
    return sheet(256, 64, 4, frames, True)


# ================================================================ fireballs
def fireball_cell(n, t, rng, kind="explosion"):
    U, V, R, TH = grid(n)
    nz = fbm(n, n, rng, 5, 4)
    nz2 = fbm(n, n, rng, 9, 3)
    if kind == "explosion":
        rad = 0.25 + 0.7 * smooth(0.0, 0.5, t)
        heat = 1.0 - smooth(0.3, 1.0, t)
    else:
        rad = 0.3 + 0.55 * smooth(0.0, 0.7, t)
        heat = 1.0 - smooth(0.5, 1.0, t)
    d = R + (nz - 0.5) * 0.6 + (nz2 - 0.5) * 0.18
    body = smooth(rad, rad * 0.25, d)
    core = smooth(rad * 0.55, 0.0, d) * heat
    i = np.clip((body * 0.75 + core * 0.55) * (0.35 + 0.65 * heat), 0, 1)
    rgb = heat_ramp(i, FIRE)
    # late wisps: dim grey smoke curls that stay after the fire cools
    smoke = smooth(0.35, 0.9, t) * smooth(rad * 1.15, rad * 0.2, R + (nz2 - 0.5) * 0.7) * (0.35 - 0.2 * smooth(0.8, 1.0, t))
    rgb = np.maximum(rgb, smoke[..., None] * np.array([120, 112, 108], F32) * (0.4 + nz[..., None] * 0.9))
    return rgb


def explosion(rng):
    frames = []
    for f in range(20):
        t = f / 19
        n = 96
        if f < 3:                                     # flicker: tiny seed sparks
            U, V, R, TH = grid(n)
            i = glow(R, 0.07 + 0.05 * f, 1.5) * (0.6 + 0.4 * rng.random())
            frames.append(heat_ramp(i, FIRE))
        else:
            frames.append(fireball_cell(n, t, rng, "explosion"))
    return sheet(480, 96, 5, frames, True)


def fireblast(rng):
    frames = []
    n = 102
    for f in range(20):
        U, V, R, TH = grid(n)
        nz = fbm(n, n, rng, 5, 4)
        if f < 2:                                     # seed
            a = glow(R, 0.1 + 0.08 * f, 1.8)
            rgb = heat_ramp(a, [(0, "#ff8010"), (1, "#ffffff")])
            frames.append(rgba(rgb, a))
        elif f < 5:                                   # white flash
            k = (f - 1) / 3
            a = smooth(0.35 + 0.5 * k, 0.0, R + (nz - 0.5) * 0.3)
            rgb = heat_ramp(a, [(0, "#ffd070"), (0.5, "#fff4d0"), (1, "#ffffff")])
            frames.append(rgba(rgb, a))
        elif f < 10:                                  # fire
            k = (f - 5) / 4
            rad = 0.85 - 0.1 * k
            a = smooth(rad, rad * 0.3, R + (nz - 0.5) * 0.6)
            hot = np.clip(a * (1.1 - 0.5 * k), 0, 1)
            rgb = heat_ramp(hot, FIRE)
            frames.append(rgba(rgb, np.clip(a * 1.2, 0, 1)))
        elif f < 14:                                  # char: dark with embers
            k = (f - 10) / 3
            rad = 0.85 + 0.05 * k
            a = smooth(rad, rad * 0.35, R + (nz - 0.5) * 0.7) * (1 - 0.25 * k)
            ember = np.clip((nz - 0.6) * 4, 0, 1) * (1 - k) * a
            rgb = np.stack([30 + 200 * ember, 22 + 90 * ember, 18 + 20 * ember], -1)
            frames.append(rgba(rgb, a))
        else:                                         # grey smoke dissipating
            k = (f - 14) / 5
            rad = 0.9 - 0.05 * k
            a = smooth(rad, rad * 0.2, R + (nz - 0.5) * 0.8) * (0.7 - 0.65 * k)
            g = 80 + 70 * nz
            rgb = np.stack([g, g * 0.97, g * 0.95], -1)
            frames.append(rgba(rgb, a))
    return sheet(512, n, 5, frames, False)


def _smoke_cell(n, rng, density, rgb_base=(120, 120, 120), puffs=6, spread=0.45, shade_dir=True):
    U, V, R, TH = grid(n)
    fld = np.zeros_like(R)
    for _ in range(puffs):
        cx, cy = (rng.random(2) - 0.5) * 2 * spread
        rad = 0.35 + 0.3 * rng.random()
        fld = np.maximum(fld, smooth(rad, rad * 0.2, np.sqrt((U - cx) ** 2 + (V - cy) ** 2)))
    nz = fbm(n, n, rng, 5, 4)
    a = np.clip(fld * (0.5 + nz * 0.9) * density - 0.05, 0, 1)
    a *= smooth(1.0, 0.65, R)
    light = 1.0 - 0.35 * (U * 0.5 + V * 0.5) if shade_dir else 1.0
    g = (np.array(rgb_base, F32)[None, None, :] * (0.7 + 0.6 * nz[..., None]) * np.asarray(light)[..., None]
         if shade_dir else np.array(rgb_base, F32)[None, None, :] * (0.7 + 0.6 * nz[..., None]))
    return rgba(g, a)


def dustclouds02(rng):
    n = 85
    frames = []
    for f in range(9):
        t = f / 8
        U, V, R, TH = grid(n)
        nz = fbm(n, n, rng, 5, 4)
        rad = 0.3 + 0.6 * smooth(0, 0.7, t)
        d = R + (nz - 0.5) * 0.7
        a = smooth(rad, rad * 0.1, d) * (0.85 - 0.8 * smooth(0.35, 1.0, t))
        g = 105 + 60 * nz - 30 * (U * 0.5 + V * 0.5)
        frames.append(rgba(np.stack([g, g, g * 0.97], -1), a))
    return sheet(256, n, 3, frames, False)


def largesmoke(rng):
    frames = []
    for v, dens in enumerate([1.5, 1.1, 0.75, 0.45]):
        frames.append(_smoke_cell(256, rng, dens, (149, 127, 128), puffs=9, spread=0.4))
    return sheet(512, 256, 2, frames, False)


def shockwave(rng):
    U, V, R, TH = grid(64)
    ring = np.exp(-(((R - 0.78) / 0.05) ** 2)) * 0.9 + 0.2 * np.exp(-(((R - 0.7) / 0.14) ** 2))
    a = np.clip(ring, 0, 1) * smooth(1.0, 0.9, R)
    return sheet(64, 64, 1, [rgba(np.full(R.shape + (3,), 235, F32), a * 0.8)], False)


def _flame_ball(n, t, rng, tongues=False):
    U, V, R, TH = grid(n)
    nz = fbm(n, n, rng, 4, 4)
    nz2 = fbm(n, n, rng, 8, 3)
    if tongues:
        base = 0.9 - (V + 1) / 2 * 0.0
        sway = (nz - 0.5) * 0.9
        h = 0.55 + 0.35 * np.sin(t * np.pi * 2) ** 2
        yy = (0.95 - V) / 1.9                                     # 0 at the bottom -> 1 at the top
        tip = np.clip(yy / h, 0, 1)
        width = (0.5 * (1 - tip) ** 0.9 + 0.04) * (0.6 + 0.8 * nz2)
        d = np.abs(U + sway * tip)
        i = smooth(width, width * 0.2, d) * (1 - tip ** 1.5) * (0.8 + 0.4 * nz2)
        i = np.clip(i * smooth(-1.0, -0.8, V) * (1 - smooth(0.85, 1.0, V)), 0, 1)
    else:
        rad = 0.45 + 0.4 * np.sin(t * np.pi * 0.9)
        d = R + (nz - 0.5) * 0.7 + (nz2 - 0.5) * 0.25
        i = smooth(rad, rad * 0.15, d) * (1 - 0.5 * smooth(0.6, 1.0, t))
        i = np.clip(i * (0.5 + 0.5 * smooth(rad * 0.9, 0.0, d)) * 1.25, 0, 1)
    return i


def flamethrower(rng):
    frames = [heat_ramp(_flame_ball(128, f / 15, rng), FIRE) for f in range(16)]
    return sheet(512, 128, 4, frames, True)


def flamethrower2(rng):
    frames = [heat_ramp(_flame_ball(128, f / 15, rng, tongues=True), FIRE) for f in range(16)]
    return sheet(512, 128, 4, frames, True)


def flamethrower_a(rng):
    frames = []
    n = 128
    for f in range(16):
        i = _flame_ball(n, f / 15, rng)
        U, V, R, TH = grid(n)
        nz = fbm(n, n, rng, 4, 4)
        halo = smooth(0.95, 0.3, R + (nz - 0.5) * 0.6) * 0.5
        rgb_f = heat_ramp(i, [(0, "#802010"), (0.4, "#ff9020"), (0.75, "#ffc040"), (1, "#ffffff")])
        smoke = np.array([70, 62, 58], F32) * (0.7 + nz[..., None] * 0.7)
        w = np.clip(i * 1.6, 0, 1)[..., None]
        rgb = rgb_f * w + smoke * (1 - w)
        a = np.clip(np.maximum(i * 1.25, halo * (0.8 - 0.35 * f / 15)), 0, 1)
        frames.append(rgba(rgb, a))
    return sheet(512, 128, 4, frames, False)


# ================================================================ scopes
def testscope(rng):
    n = 3072
    v = (np.arange(n, dtype=F32) + 0.5) / n * 2 - 1
    U, V = np.meshgrid(v, v)
    R = np.sqrt(U * U + V * V)
    win = 0.82                                           # window radius, as a share of half-height
    edge = 2.5 / (n / 2)
    alpha = smooth(win - edge, win + edge, R)             # 0 inside the window, 1 in the black surround
    px = 1.0 / (n / 2)
    thin = 1.6 * px
    cross = ((np.abs(U) < thin) | (np.abs(V) < thin)) & (R < win)
    gap = R < 0.025                                      # open centre
    cross &= ~gap
    # fine reticle ticks along both axes
    ticks = np.zeros_like(R, bool)
    for k in range(1, 8):
        d = 0.1 * k
        ticks |= (np.abs(np.abs(U) - d) < thin) & (np.abs(V) < 0.012 + (0.012 if k % 5 == 0 else 0))
        ticks |= (np.abs(np.abs(V) - d) < thin) & (np.abs(U) < 0.012 + (0.012 if k % 5 == 0 else 0))
    ticks &= (R < win - 0.02)
    out = np.zeros((n, n, 4), np.uint8)
    a = np.clip(alpha, 0, 1)
    a = np.where(cross | ticks, 1.0, a)
    out[..., 3] = np.rint(a * 255).astype(np.uint8)
    # the surround and reticle are black; a very faint dark grey rim line just outside the window
    rim = np.exp(-(((R - (win + 0.012)) / 0.004) ** 2))
    out[..., :3] = np.rint(np.clip(rim, 0, 1)[..., None] * 24).astype(np.uint8)
    return Image.fromarray(out, "RGBA")


def techscope(rng):
    n = 2048
    v = (np.arange(n, dtype=F32) + 0.5) / n * 2 - 1
    U, V = np.meshgrid(v, v)
    R = np.sqrt(U * U + V * V)
    px = 1.0 / (n / 2)
    cy = np.array(hx("#00e0e0"), F32)
    win = 0.7
    border = hx("#021616")
    alpha = smooth(win - 2 * px, win + 2 * px, R)
    out = np.zeros((n, n, 4), F32)
    out[..., :3] = np.array(border, F32)
    out[..., 3] = alpha * 255
    glowl = np.zeros((n, n), F32)

    def add_line(mask_i):
        nonlocal glowl
        glowl = np.maximum(glowl, mask_i)

    # hexagon reticle (flat-top hexagon distance)
    def hexd(U, V):
        ax, ay = np.abs(U), np.abs(V)
        return np.maximum(ay * 0.8660254 + ax * 0.5, ax)
    hd = hexd(U, V)
    for rad, w in ((0.30, 2.0), (0.46, 1.2)):
        add_line(np.exp(-(((hd - rad) / (w * px)) ** 2)))
    # cross-hair with a clear centre and ticks
    for axis in (U, V):
        other = V if axis is U else U
        line = np.exp(-((axis / (1.4 * px)) ** 2)) * (np.abs(other) > 0.04) * (np.abs(other) < 0.64)
        add_line(line)
    for k in range(1, 6):
        d = 0.1 * k
        for a_, o_ in ((U, V), (V, U)):
            tick = np.exp(-(((np.abs(a_) - d) / (1.4 * px)) ** 2)) * (np.abs(o_) < (0.02 if k % 2 else 0.035))
            add_line(tick)
    # corner brackets in the border
    b = 0.78
    L = 0.14
    for sx in (-1, 1):
        for sy in (-1, 1):
            hline = np.exp(-(((V - sy * b) / (2 * px)) ** 2)) * ((sx * U > b - L) & (sx * U < b))
            vline = np.exp(-(((U - sx * b) / (2 * px)) ** 2)) * ((sy * V > b - L) & (sy * V < b))
            add_line(np.maximum(hline, vline))
    # dim ring edge of the window
    add_line(0.8 * np.exp(-(((R - win) / (2.2 * px)) ** 2)))
    out[..., :3] = out[..., :3] * (1 - glowl[..., None]) + cy * glowl[..., None]
    out[..., 3] = np.maximum(out[..., 3], glowl * 255)
    return Image.fromarray(np.clip(np.rint(out), 0, 255).astype(np.uint8), "RGBA")


# ================================================================ energy flares
def flare_cell(n, t, rng, kind, col):
    U, V, R, TH = grid(n)
    life = np.sin(np.clip(t, 0, 1) * np.pi)                         # 0 -> 1 -> 0
    fade = 1 - smooth(0.55, 1.0, t)
    grow = 0.35 + 0.6 * smooth(0.0, 0.6, t)
    core = glow(R, 0.18 * (0.6 + life), 1.6)
    halo = 0.5 * glow(R, 0.55 * grow, 2.0)
    i = core + halo
    if kind == "alien":                                              # spiral arms
        arms = 0.5 + 0.5 * np.cos(3 * (TH + 5.0 * R - t * 5.0))
        i = i + 0.9 * arms * glow(R, 0.7 * grow, 1.5) * np.clip(R * 3, 0, 1)
    elif kind == "blaster":                                          # irregular spikes
        k = 6
        spikes = np.abs(np.cos(k / 2 * (TH + 0.3 * t))) ** 14 * (0.8 + 0.4 * fbm(n, n, rng, 3, 2))
        i = i + 1.1 * spikes * glow(R, 0.8 * grow, 1.3)
    elif kind == "tesla":
        # radial jagged arcs + a ring mid-phase
        nz = fbm(n, n, rng, 8, 3)
        arcs = np.abs(np.cos(5 * TH + 6 * nz)) ** 18
        i = i + 1.0 * arcs * glow(R, 0.7 * grow, 1.4) * (0.4 + life)
        ringc = 0.25 + 0.55 * t
        i = i + 1.2 * np.exp(-(((R - ringc) / 0.05) ** 2)) * smooth(0.25, 0.5, t) * (1 - smooth(0.7, 1.0, t))
    elif kind == "nuke":
        for k in range(4):                                            # concentric rings, golden plasma orb
            rr = 0.18 + 0.2 * k + 0.12 * t
            i = i + 0.55 * np.exp(-(((R - rr) / (0.035 + 0.01 * k)) ** 2)) * (1 - 0.18 * k) * (0.5 + life)
        nz = fbm(n, n, rng, 5, 3)
        i = i + 0.35 * nz * glow(R, 0.5, 2.0)
    elif kind == "laser":
        streak = np.exp(-((V / 0.035) ** 2)) * glow(np.abs(U), 0.95 * grow, 1.1)
        radial = np.abs(np.cos(8 * TH)) ** 40 * glow(R, 0.6 * grow, 1.5)
        i = i + 0.9 * streak + 0.5 * radial
    i = np.clip(i * (0.25 + 0.75 * fade) * (0.7 + 0.5 * life), 0, 1.15)
    return tint(col, np.clip(i, 0, 1))


def _flares(kind, col, nframes, size=512, cell=128, cols=4):
    def f(rng):
        frames = [flare_cell(cell, i / (nframes - 1), rng, kind, col) for i in range(nframes)]
        return sheet(size, cell, cols, frames, True)
    return f


alienflare = _flares("alien", "#c040ff", 14)
blasterflare01 = _flares("blaster", "#ffa030", 14)
teslaflare01 = _flares("tesla", "#40a0ff", 14)
nukebeamflare = _flares("nuke", "#ffd040", 16)
laserflare02 = _flares("laser", "#ff2020", 7)


# ================================================================ splats, blood, slime, impact
def _blob(U, V, cx, cy, r, wob):
    d = np.sqrt((U - cx) ** 2 + (V - cy) ** 2)
    return smooth(r * (1 + wob), r * (1 + wob) * 0.82, d)


def blobs(rng):
    n = 128
    frames = []
    for f in range(16):
        U, V, R, TH = grid(n)
        nz = fbm(n, n, rng, 4, 3)
        a = np.zeros_like(R)
        shade_f = np.zeros_like(R)
        main = 0.28 + 0.2 * rng.random()
        k = 3 + f % 4
        for j in range(k):
            ang = rng.random() * 6.283
            dist = (0.1 + 0.45 * rng.random()) * (0.4 + 0.6 * (f / 15))
            r = (main if j == 0 else 0.07 + 0.14 * rng.random())
            cx, cy = (0, 0) if j == 0 else (dist * np.cos(ang), dist * np.sin(ang))
            d = np.sqrt((U - cx) ** 2 + (V - cy) ** 2) + (nz - 0.5) * 0.08
            m = smooth(r, r * 0.88, d)
            a = np.maximum(a, m)
            depth = np.clip(1 - d / r, 0, 1)
            shade_f = np.maximum(shade_f, m * depth)
        lime = np.array(hx("#80e020"), F32)
        dark = lime * 0.45
        rgb = dark + (lime - dark) * np.clip(shade_f * 2.2, 0, 1)[..., None]
        # specular highlight, top-left of each blob core
        hl = np.exp(-(((U + 0.1) ** 2 + (V + 0.1) ** 2) / 0.006)) * 0.0
        spec = np.exp(-(((U - (-0.09)) ** 2 + (V - (-0.1)) ** 2) / 0.003)) * (a > 0.5)
        rgb = rgb + spec[..., None] * (np.array(hx("#a0ffff"), F32) - rgb) * 0.9
        frames.append(rgba(rgb, a))
    return sheet(512, n, 4, frames, False)


def _veins(n, rng, cx=0.0, cy=0.0, branches=9, length=0.85):
    img = np.zeros((n, n), F32)
    ys, xs = np.mgrid[0:n, 0:n].astype(F32)

    def stamp(px, py, rad):
        r = max(1, int(round(rad)))
        x0, y0 = int(px), int(py)
        for dy in range(-r, r + 1):
            for dx in range(-r, r + 1):
                if dx * dx + dy * dy <= r * r + 0.5 and 0 <= y0 + dy < n and 0 <= x0 + dx < n:
                    img[y0 + dy, x0 + dx] = 1.0

    def walk(x, y, ang, steps, w, depth):
        for s in range(steps):
            ang += (rng.random() - 0.5) * 0.7
            x += np.cos(ang) * 1.3
            y += np.sin(ang) * 1.3
            stamp(x, y, w * (1 - s / steps) + 0.2)
            if depth < 2 and rng.random() < 0.06:
                walk(x, y, ang + (rng.random() - 0.5) * 2.0, int(steps * 0.5), w * 0.6, depth + 1)
    c = n / 2
    for b in range(branches):
        walk(c + cx * c, c + cy * c, 2 * np.pi * b / branches + rng.random() * 0.5,
             int(length * n / 2 / 1.3 * (0.6 + 0.5 * rng.random())), 1.6, 0)
    return img


def splat(rng):
    n = 128
    frames = []
    for f in range(6):
        img = _veins(n, rng, branches=7 + f)
        # drip dots around the veins
        U, V, R, TH = grid(n)
        a = img * smooth(1.0, 0.7, R)
        for _ in range(6 + f * 2):
            x, y = (rng.random(2) - 0.5) * 1.6
            a = np.maximum(a, smooth(0.035, 0.02, np.sqrt((U - x) ** 2 + (V - y) ** 2)) * 0.9)
        g = np.full(a.shape + (3,), 230, F32)
        frames.append(rgba(g, a))
    return sheet(384, n, 3, frames, False)


def blood4x4(rng):
    n = 128
    frames = []
    for f in range(4):
        U, V, R, TH = grid(n)
        nz = fbm(n, n, rng, 5, 3)
        a = np.zeros_like(R)
        main = 0.2 + 0.07 * f
        a = np.maximum(a, smooth(main, main * 0.55, R + (nz - 0.5) * 0.35))
        for _ in range(8 + 3 * f):
            ang = rng.random() * 6.283
            dist = 0.3 + 0.55 * rng.random()
            r = 0.02 + 0.06 * rng.random()
            cx, cy = dist * np.cos(ang), dist * np.sin(ang)
            a = np.maximum(a, smooth(r, r * 0.5, np.sqrt((U - cx) ** 2 + (V - cy) ** 2)))
            # elongated drip toward the centre
            for s in np.linspace(0.2, 0.9, 5):
                a = np.maximum(a, smooth(r * 0.5, r * 0.2, np.sqrt((U - cx * (1 - s * 0.3)) ** 2 + (V - cy * (1 - s * 0.3)) ** 2)) * 0.8)
        g = 133 + 40 * (nz - 0.5) - 30 * (U * 0.5 + V * 0.5) * (a > 0.4)
        frames.append(rgba(np.stack([g, g, g], -1), a * 0.95))
    return sheet(256, n, 2, frames, False)


def impact(rng):
    n = 128
    frames = []
    for f in range(4):
        U, V, R, TH = grid(n)
        a = np.zeros_like(R)
        k = 9 + f
        for j in range(k):
            ang = 2 * np.pi * j / k + (rng.random() - 0.5) * 0.4
            L = 0.45 + 0.5 * rng.random()
            wid = 0.07 + 0.04 * rng.random()
            d = np.angle(np.exp(1j * (TH - ang)))
            sh = (np.abs(d) < wid * np.clip(1 - R / L, 0, 1) ** 0.8 / np.maximum(R, 0.05) * 0.5) & (R < L)
            a = np.maximum(a, sh.astype(F32))
        a = np.maximum(a, smooth(0.14, 0.05, R))
        # soften the edges
        im = Image.fromarray((a * 255).astype(np.uint8), "L")
        from PIL import ImageFilter
        a = np.asarray(im.filter(ImageFilter.GaussianBlur(0.7)), F32) / 255
        frames.append(rgba(np.full(a.shape + (3,), 245, F32), np.clip(a * 1.2, 0, 1)))
    return sheet(256, n, 2, frames, False)


# ================================================================ sonic, beams, flares
def sonicwave4x4(rng):
    n = 256
    frames = []
    for f in range(16):
        t = f / 15
        U, V, R, TH = grid(n)
        if f < 4:                                          # tiny ring growing
            rr = 0.05 + 0.08 * f
            i = np.exp(-(((R - rr) / 0.018) ** 2)) * (0.6 + 0.1 * f)
        elif f < 9:                                        # bright donut
            k = (f - 4) / 4
            rr = 0.25 + 0.4 * k
            wd = 0.05 + 0.05 * k
            i = np.exp(-(((R - rr) / wd) ** 2)) * (1.0 - 0.15 * k) + 0.25 * np.exp(-(((R - rr * 0.8) / (wd * 2)) ** 2))
        else:                                              # sparkles on a fading ring
            k = (f - 9) / 6
            rr = 0.65 + 0.28 * k
            i = np.exp(-(((R - rr) / (0.05 + 0.03 * k)) ** 2)) * (0.8 - 0.8 * k)
            spark = np.zeros_like(R)
            for _ in range(14):
                ang = rng.random() * 6.283
                rr2 = rr + (rng.random() - 0.5) * 0.12
                cx, cy = rr2 * np.cos(ang), rr2 * np.sin(ang)
                spark = np.maximum(spark, np.exp(-(((U - cx) ** 2 + (V - cy) ** 2) / 0.0007)))
            i = i + spark * (1 - k)
        i = i * smooth(1.0, 0.85, R)
        frames.append(tint("#8090ff", np.clip(i, 0, 1)))
    return sheet(1024, n, 4, frames, True)


def nukebeam(rng):
    w, h, nf = 128, 64, 17
    # a looping band: noise periodic in u, shifted by 128/17 px per frame
    base_nz = tile_noise(w, rng, 8)
    nz2 = tile_noise(w, rng, 16)
    v = (np.arange(h, dtype=F32) + 0.5) / h * 2 - 1
    frames = []
    for f in range(nf):
        shift = int(round(f * w / nf))
        n1 = np.roll(base_nz, -shift, axis=1)[:h, :] if False else np.roll(np.resize(base_nz, (h, w)), -shift, axis=1)
        n2 = np.roll(np.resize(nz2, (h, w)), shift * 2, axis=1)
        U = np.zeros((h, w), F32)
        V = np.tile(v[:, None], (1, w))
        wob = (n1 - 0.5) * 0.35
        d = np.abs(V + wob)
        core = np.exp(-((d / (0.16 + 0.05 * n2)) ** 2))
        outer = np.exp(-((d / (0.5 + 0.12 * n1)) ** 2)) * (0.45 + 0.5 * n2)
        i = np.clip(core * 1.05 + outer * 0.6, 0, 1)
        rgb = heat_ramp(i, [(0, "#000000"), (0.25, "#6a3010"), (0.5, "#b06020"), (0.8, "#ffd040"), (1, "#ffffff")])
        pad = np.zeros(((w - h) // 2, w, 3), F32)          # square frame: band in the middle, black above/below
        frames.append(np.concatenate([pad, rgb, pad], 0))
    img = np.concatenate(frames, 0)
    out = np.dstack([img, np.full(img.shape[:2], 255, F32)])
    return Image.fromarray(np.clip(np.rint(out), 0, 255).astype(np.uint8), "RGBA")


def lensflare1(rng):
    U, V, R, TH = grid(128)
    i = glow(R, 0.35, 1.7) * 0.85 + 0.35 * glow(R, 0.12, 1.5)
    i = np.clip(i, 0, 1) * smooth(1.0, 0.8, R)
    g = np.clip(i * 255, 0, 255)
    return sheet(128, 128, 1, [np.stack([g, g, g], -1)], True)


def lensflare3(rng):
    U, V, R, TH = grid(128)
    streak = np.exp(-((V / 0.03) ** 2)) * glow(np.abs(U), 0.7, 1.3)
    streak2 = 0.35 * np.exp(-((V / 0.08) ** 2)) * glow(np.abs(U), 0.8, 1.6)
    star = np.exp(-((U * V) ** 2 / 0.00006)) * glow(R, 0.35, 1.5) * 0.7 * 0 + (
        np.abs(np.cos(2 * TH)) ** 60 * glow(R, 0.22, 1.5)) * 0.5
    core = glow(R, 0.1, 1.6)
    i = np.clip(streak + streak2 + star + core, 0, 1) * smooth(1.0, 0.92, np.abs(U))
    g = i * 255
    return sheet(128, 128, 1, [np.stack([g, g, g], -1)], True)


def _beam(rng, w, h, col_edge, col_core, wobble):
    v = (np.arange(h, dtype=F32) + 0.5) / h * 2 - 1
    u = np.arange(w, dtype=F32) / w
    V = np.tile(v[:, None], (1, w))
    ph = np.sin(u * 2 * np.pi * 2)[None, :] * wobble
    d = np.abs(V + ph * 0.1)
    core = np.exp(-((d / 0.13) ** 2))
    mid = np.exp(-((d / 0.32) ** 2))
    edge = np.exp(-((d / 0.62) ** 2))
    flick = 0.9 + 0.1 * np.cos(u * 2 * np.pi * 4)[None, :]
    e = np.array(hx(col_edge), F32)
    c = np.array(hx(col_core), F32)
    rgb = (e * (edge * 0.55 + mid * 0.45)[..., None] + (255 - e) * 0.0) * flick[..., None]
    rgb = rgb + (c - rgb) * np.clip(core * 1.3, 0, 1)[..., None]
    rgb = np.clip(rgb, 0, 255)
    return sheet(w, w, 1, [rgb], True, h=h)


def laser3(rng):
    return _beam(rng, 128, 64, "#ff4040", "#ffd0d0", 0.5)


def laser_blue(rng):
    img = _beam(rng, 128, 64, "#2060ff", "#e0ffff", 0.7)
    a = np.asarray(img, F32)
    # tesla beams crackle: add a thin white-cyan jitter line along the core
    for x in range(128):
        y = 32 + int(round(3 * np.sin(x * 2 * np.pi / 128 * 3) + 2 * np.sin(x * 2 * np.pi / 128 * 7)))
        a[y, x, :3] = [230, 255, 255]
    return Image.fromarray(np.clip(np.rint(a), 0, 255).astype(np.uint8), "RGBA")


def _wrap_voronoi(n, rng, pts):
    P = rng.random((pts, 2)).astype(F32) * n
    ys, xs = np.mgrid[0:n, 0:n].astype(F32)
    ds = []
    for px, py in P:
        dx = np.abs(xs - px)
        dy = np.abs(ys - py)
        dx = np.minimum(dx, n - dx)
        dy = np.minimum(dy, n - dy)
        ds.append(np.sqrt(dx * dx + dy * dy))
    ds = np.sort(np.stack(ds, 0), axis=0)
    return ds[0], ds[1]


def laser_deathfx(rng):
    n = 64
    d1, d2 = _wrap_voronoi(n, rng, 14)
    crack = np.clip(1 - (d2 - d1) / 2.6, 0, 1)                # near cell boundaries
    nz = tile_noise(n, rng, 6)
    heat = np.clip(crack ** 1.5 * (0.7 + 0.6 * nz), 0, 1)
    rgb = heat_ramp(heat, [(0, "#1c0606"), (0.25, "#3a0a06"), (0.55, "#c03a0a"), (0.8, "#ff8a20"), (1, "#ffd070")])
    rgb *= (0.8 + 0.3 * nz)[..., None]
    return sheet(n, n, 1, [rgb], False)


def bio(rng):
    """Tileable mottled slime: wrapped cell membranes, glossy nuclei, spore specks."""
    n = 64
    a = tile_noise(n, rng, 4)
    b = tile_noise(n, rng, 8)
    c = tile_noise(n, rng, 16)
    f = 0.5 * a + 0.3 * b + 0.2 * c
    cols = [hx("#2c7a1c"), hx("#3d9a28"), hx("#5db049"), hx("#82cc5c")]
    idx = np.clip((f * 4 * 1.05).astype(int), 0, 3)
    rgb = np.array(cols, F32)[idx]
    d1, d2 = _wrap_voronoi(n, rng, 11)
    membrane = (d2 - d1) < 1.6                              # dark cell walls
    rgb = np.where(membrane[..., None], np.array(hx("#1c5a14"), F32), rgb)
    wall_hi = ((d2 - d1) >= 1.6) & ((d2 - d1) < 2.6)        # lit inner lip of each cell
    rgb = np.where(wall_hi[..., None], rgb * 0.82, rgb)
    nuc = d1 < 3.2                                           # nucleus: darker core with a glossy spot
    rgb = np.where(nuc[..., None], np.array(hx("#256c1a"), F32) + 18 * (d1 < 2.2)[..., None], rgb)
    # gloss highlight on the upper-left of each nucleus: nearest point direction is unknown, so use noise gates
    gate = tile_noise(n, rng, 10) > 0.74
    rgb = np.where((nuc & gate)[..., None], np.array(hx("#c8f6a0"), F32), rgb)
    specks = tile_noise(n, rng, 20) > 0.86
    rgb = np.where((specks & ~membrane)[..., None], np.array(hx("#b8e890"), F32), rgb)
    return sheet(n, n, 1, [rgb], False)


def heatray(rng):
    n = 128
    U, V, R, TH = grid(n)
    nz = fbm(n, n, rng, 5, 3)
    # teardrop: round head at the bottom, tip curling up
    t = np.clip((0.75 - V) / 1.5, 0, 1)                      # 0 at the head, 1 at the tip
    half = 0.38 * (1 - t) ** 0.9
    half = np.where(V > 0.75, 0, half)
    d = np.abs(U + 0.12 * np.sin(t * 5) * t + (nz - 0.5) * 0.15 * t)
    body = smooth(half, half * 0.3, d) * (half > 0.01) * (V < 0.78)
    head = smooth(0.4, 0.15, np.sqrt(U * U + (V - 0.38) ** 2))
    i = np.clip(np.maximum(body * (1 - t ** 2), head) * (0.75 + 0.5 * nz), 0, 1)
    i = i * smooth(0.98, 0.8, np.abs(V)) * smooth(0.98, 0.8, np.abs(U))
    rgb = heat_ramp(i, [(0, "#000000"), (0.2, "#606020"), (0.45, "#c08a20"), (0.8, "#ffe070"), (1, "#ffffff")])
    return sheet(128, 128, 1, [rgb], True)


PAINTERS = {
    "muzzleflash2": muzzleflash2, "muzzleflash4x4": muzzleflash4x4, "explosion": explosion, "fireblast": fireblast,
    "dustclouds02": dustclouds02, "largesmoke": largesmoke, "shockwave": shockwave, "flamethrower": flamethrower,
    "flamethrower2": flamethrower2, "flamethrower_a": flamethrower_a, "testscope": testscope,
    "alienflare": alienflare, "blasterflare01": blasterflare01, "teslaflare01": teslaflare01,
    "laserflare02": laserflare02, "blobs": blobs, "splat": splat, "blood4x4": blood4x4, "impact": impact,
    "muzzleflash_blue": muzzleflash_blue, "nukebeamflare": nukebeamflare, "sonicwave4x4": sonicwave4x4,
    "nukebeam": nukebeam, "lensflare1": lensflare1, "lensflare3": lensflare3, "laser3": laser3,
    "laser_blue": laser_blue, "laser_deathfx": laser_deathfx, "bio": bio, "heatray": heatray, "techscope": techscope,
}
