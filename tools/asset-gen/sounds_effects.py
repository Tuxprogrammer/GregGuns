"""Effects: charges, impacts, explosions, gore, disintegration, steam jump.

Original synthesis only; see sounds_guns.py for the round-1 design rules.  Round-2 rules for this family
(the recurring problems in the round-1 A/B reviews and what fixes them):

* Soft swell-ins where the original hits instantly.  Every recipe here starts its crack at the real
  onset, and ``_contour`` rewrites the head of the loudness contour so ``conform`` cannot ramp it back.
* Flat plateaus with a gated cut-off.  Bodies are built from exponential decays, and ``_contour``
  smooths the original's coarse 11-point envelope so the conformed result is a continuous roll-off.
* Stray late clicks and blips.  Tails are smooth, low-passed noise; discrete events only where the
  original has them, at its onset times.
* Static or periodic amplitude modulation.  ``_flutter`` jitters rate and depth so nothing beats.
* Missing layers: the 800 Hz metal ring in the impacts, the continuous rumble bed under the charges, the
  wet micro-burst squelch in the gore sounds.

Everything is seeded through ``c.rng``, so regeneration is byte-for-byte reproducible.  The C4 / demolition
charge sounds are the one place where the originals really contain electronic beeps, so they use gated
beeps with click-sharp attacks; everything else is shaped noise.
"""
from __future__ import annotations

import numpy as np

import sfx
from sndlib import Mix, get_tg, recipe, REGISTRY, crack_burst, sub_drop, mech_click
from sndengines import (fit, blast, events, rumble, churn, tone_burst, click_thump, foley_event, rel_env_at, _sum)


# =========================================================================== private helpers
def _contour(c, head=None, t_from=0.0, smooth=0.0, extra=None):
    """Replace the target loudness contour that ``conform`` pins the recipe to.

    Starts from the original's measured contour; ``smooth`` (s) low-passes it in dB so the coarse
    11-point envelope cannot produce plateaus and steps; ``head`` = [(t, dB re peak), ...] replaces
    everything before ``t_from`` (sharp onsets); ``extra`` points are merged in last."""
    tg = get_tg(c)
    base = tg.rel_env_pts()
    if base is None:
        return
    ts, db = np.asarray(base[0], float), np.asarray(base[1], float)
    if smooth:
        grid = np.arange(0.0, ts[-1] + 1e-9, 0.01)
        d = np.interp(grid, ts, db)
        k = int(smooth / 0.01) | 1
        d = np.convolve(np.pad(d, (k // 2, k // 2), mode="edge"), np.ones(k) / k, mode="valid")
        ts, db = grid, d
    if head:
        keep = ts > t_from
        ts = np.concatenate([[h[0] for h in head], ts[keep]])
        db = np.concatenate([[h[1] for h in head], db[keep]])
    if extra:
        lo_t, hi_t = min(t for t, _ in extra), max(t for t, _ in extra)
        pts = {round(t, 4): v for t, v in zip(ts, db) if not (lo_t < t < hi_t)}
        for t, v in extra:
            pts[round(t, 4)] = v
        ts = np.array(sorted(pts))
        db = np.array([pts[t] for t in ts])
    tg.rel_env_pts = lambda: (ts, db)


def _flutter(n, sr, rng, rate, depth, jitter=0.25):
    """Irregular amplitude flutter: two random modulators at jittered rates, so no steady beat remains."""
    a = sfx.rand_mod(n, sr, rng, rate * (1 + jitter * rng.uniform(-1, 1)), depth)
    b = sfx.rand_mod(n, sr, rng, rate * 0.55 * (1 + jitter * rng.uniform(-1, 1)), depth * 0.6)
    return a * b


def _lp(x, sr, f, order=4):
    return sfx.fftfilt(x, sr, lp=f, order=order)


def _hp(x, sr, f, order=2):
    return sfx.fftfilt(x, sr, hp=f, order=order)


def _noise_click(sr, rng, dur=0.01, lo=2000, hi=12000, tau=None, attack=0.0003):
    """Short broadband noise tick: near-instant attack, ~dur long exponential decay."""
    n = sfx.n_of(sr, max(dur * 6, 0.02))
    g = sfx.noise(n, rng) * sfx.exp_env(n, sr, tau or dur * 0.4, attack=attack)
    return sfx.fftfilt(g, sr, hp=lo, lp=min(hi, sr * 0.45), order=2)


def _thump(sr, f0, f1, dur, tau, k=0.03):
    return sub_drop(sr, f0, f1, dur, tau, k=k, attack=0.0015)


def _gated(sr, f, dur, partials=((1.0, 1.0),), ramp=0.0015):
    """Constant-amplitude tone with short linear ramps (a hard electronic gate, no decay)."""
    n = sfx.n_of(sr, dur)
    t = sfx.tvec(n, sr)
    out = np.zeros(n)
    for r, g in partials:
        out += g * np.sin(sfx.TAU * f * r * t)
    env = np.minimum(t / ramp, 1.0) * np.minimum((dur - t) / ramp, 1.0)
    return out * np.clip(env, 0, 1)


def _modes(sr, rng, n_modes=4, lo=600, hi=3500, tau=(0.008, 0.02), dur=0.08, hi_tau=0.006):
    """3-6 damped inharmonic resonances (mostly 600-3500 Hz, short decays; anything above 5 kHz dies in under
    10 ms) on a 1-3 ms noise transient: the metal-click recipe.  Each mode is a narrow *noise* band rather
    than a sine, so no tonal line forms (the tonality gate sees none) while the click still reads as metal."""
    n = sfx.n_of(sr, dur)
    t = sfx.tvec(n, sr)
    out = sfx.noise(n, rng) * np.exp(-t / 0.0015)
    for f in np.exp(rng.uniform(np.log(lo), np.log(hi), n_modes)):
        tt = hi_tau if f > 5000 else rng.uniform(*tau)
        band = sfx.fftfilt(sfx.noise(n, rng), sr, hp=f * 0.88, lp=min(f * 1.14, sr * 0.45), order=2)
        out += rng.uniform(0.5, 1.0) * band / (np.std(band) + 1e-9) * 0.8 * np.exp(-t / tt)
    return out


def _dc(x, sr, f=20.0):
    return sfx.fftfilt(x, sr, hp=f, order=2)


# =========================================================================== C4 / demolition charges
def _charge_flags(c, bands=False):
    """Charges are spiky electronic events: keep the hand-built dynamics, let only the normalizer act.
    Band conforming is opt-in (it would erase the deliberately faint low rumble beds)."""
    c.conform_env = False
    c.conform_bands = bands


def _snap(sr, rng, g=1.0, thump_f=(130, 60), thump_g=0.5, thump_dur=0.04, crack_dur=0.009, hi=14000, lo=2000, sizzle=0.8):
    """Hard mechanical snap: broadband noise crack, a 6-10 kHz sizzle on top, and a short low thump."""
    cr = _noise_click(sr, rng, dur=crack_dur, lo=lo, hi=hi)
    sz = _noise_click(sr, rng, dur=0.006, lo=6000, hi=16000)
    th = _thump(sr, thump_f[0], thump_f[1], thump_dur * 4, thump_dur * 0.6, k=0.02)
    return _sum(_sum(cr * g, sz * g * sizzle), th * g * thump_g)


@recipe("c4charge_init")
def c4_init(c, N):
    """Three hard clicks (0, 0.10, 0.21 s, falling level) over a low rumble bed, then a flat 3.85 kHz
    beep gated 0.29-0.43 s with a hard cut, and a very faint tail that is silent for the last 0.2 s."""
    _charge_flags(c)
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    m.add(_snap(sr, rng, 1.0, thump_g=0.12, crack_dur=0.012, lo=2500, hi=9000, sizzle=0.5), 0.0, 4.0)
    m.add(_snap(sr, rng, 1.0, thump_g=0.10, crack_dur=0.011, lo=2500, hi=9000, sizzle=0.5), 0.10, 4.0)
    m.add(_snap(sr, rng, 0.8, thump_g=0.08, crack_dur=0.010, lo=2500, hi=9000, sizzle=0.5), 0.21, 2.2)
    m.add(_modes(sr, rng, 3, 800, 1000, dur=0.05), 0.0, 0.05)   # weak ~900 Hz resonance under the clicks
        # continuous low rumble bed under everything, 0-0.45 s, dying away by ~0.6 s
    nb = sfx.n_of(sr, 0.8)
    tb = sfx.tvec(nb, sr)
    bed = _lp(sfx.noise(nb, rng), sr, 200, order=2) * np.clip((0.8 - tb) / 0.25, 0, 1) ** 2
    bed = _hp(bed, sr, 30)
    m.add(bed / (np.std(bed) + 1e-9), 0.0, 0.014)
    bp = _gated(sr, 3825, 0.14, ((1.0, 1.0),), ramp=0.005)
    nbp = sfx.n_of(sr, 0.14)   # slight grit so the line is not a 100 dB digital sine
    bp = bp + 0.03 * _hp(sfx.noise(nbp, rng), sr, 1500) * np.minimum(sfx.tvec(nbp, sr) / 0.002, 1.0)
    m.add(bp, 0.29, 0.28)
    return m.out


@recipe("c4charge_plant")
def c4_plant(c, N):
    """Crunchy clipped slap with a heavy thump, two sharp clicks, a scraping rustle, three short noisy
    4.5 kHz blips and one flat-topped confirmation beep, then a faint room-noise tail."""
    _charge_flags(c, bands=True)
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    m.add(crack_burst(sr, rng, tau=0.012, lo=400, hi=12000, dur=0.15), 0.0, 1.1)
    nb = sfx.n_of(sr, 0.2)
    m.add(sfx.dark_noise(nb, sr, rng, 6000, 500, steps=5, hp=200) * sfx.exp_env(nb, sr, 0.04, attack=0.002), 0.0, 1.2)
    nc = sfx.n_of(sr, 0.16)   # 1-6 kHz crunch under the thump
    m.add(sfx.band_noise(nc, sr, rng, 1000, 6000, order=1) * sfx.exp_env(nc, sr, 0.03), 0.0, 0.5)
    m.add(_thump(sr, 150, 50, 0.2, 0.03, k=0.03), 0.0, 1.0)
    m.add(_snap(sr, rng, 1.0, thump_f=(150, 100), thump_g=0.35, thump_dur=0.012, lo=2000, hi=8000), 0.21, 0.55)
    m.add(_snap(sr, rng, 1.0, thump_f=(130, 100), thump_g=0.30, thump_dur=0.012, lo=2000, hi=8000), 0.28, 0.5)
    # handling rustle 0.37-0.6 s: a handful of bursty 1-5 kHz grains at irregular spacing plus tiny clicks
    for t, d in ((0.40, 0.03), (0.47, 0.05), (0.53, 0.025), (0.60, 0.035)):
        t += rng.uniform(-0.01, 0.01)
        ng = sfx.n_of(sr, d)
        gr = sfx.band_noise(ng, sr, rng, 1000, 5000, order=1) * sfx.smooth_env(ng, sr, d * 0.3, d * 0.6)
        m.add(gr, t, 0.32 * rng.uniform(0.7, 1.0))
    nbr = sfx.n_of(sr, 0.09)   # soft scuff bridging the gap after the second click
    for t in (0.45, 0.49, 0.58):
        m.add(_noise_click(sr, rng, dur=0.004, lo=1500, hi=7000), t, 0.3)
    # beeps buried in thumps and noise: three short noisy blips on A's 3.75 kHz line, then one flat-topped
    # confirmation tone on A's 4.525 kHz line
    for t, d, f in ((0.66, 0.045, 3750), (0.72, 0.045, 3750), (0.78, 0.045, 3750), (0.86, 0.085, 4525)):
        ramp = 0.004 if d < 0.06 else 0.003
        n_ = sfx.n_of(sr, d)
        grit = _hp(sfx.noise(n_, rng), sr, 1500) * np.minimum(sfx.tvec(n_, sr) / 0.002, 1.0)
        if d < 0.06:   # the short blips are noisy 4.5 kHz bursts (band 3.4-5.8 kHz), not sine lines
            bb = sfx.band_noise(n_, sr, rng, 3400, 5800, order=2) * sfx.smooth_env(n_, sr, 0.004, 0.012)
            m.add(bb / (np.std(bb) + 1e-9) * 0.35, t, 0.2)
        else:
            m.add(_gated(sr, f, d, ((1.0, 1.0), (3.0, 0.02)), ramp=ramp) * 0.7 + 0.18 * grit, t, 0.2)
        nn = sfx.n_of(sr, d + 0.01)
        m.add(sfx.band_noise(nn, sr, rng, 1000, 6000, order=1) * sfx.smooth_env(nn, sr, 0.004, 0.01), t, 0.07)
        m.add(_thump(sr, 190, 110, 0.06, 0.02, k=0.02), t, 0.30)
    # room-noise tail 1.0-1.4 s: faint hiss, no discrete events
    nt = sfx.n_of(sr, 0.5)
    tt = sfx.tvec(nt, sr)
    tail = sfx.band_noise(nt, sr, rng, 250, 5000, order=1) * np.clip((0.42 - tt) / 0.2, 0, 1) ** 1.5 * np.minimum(tt / 0.03, 1.0)
    m.add(_lp(tail, sr, 3000, order=2), 0.96, 0.012)
    return m.out


@recipe("c4charge_tick")
def c4_tick(c, N):
    """One clean 1.7 kHz beep (harmonics at 3.4 and 6.9 kHz) with two accent bumps, dropping ~15 dB into a
    continuous soft ring that fades monotonically to silence by ~0.55 s.  No sub, no stray events."""
    _charge_flags(c)
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    m.add(_thump(sr, 140, 70, 0.1, 0.03, k=0.03), 0.05, 0.02)   # weak low thump
    m.add(_noise_click(sr, rng, dur=0.004, lo=1500, hi=8000), 0.05, 0.25)
    m.add(_noise_click(sr, rng, dur=0.003, lo=1500, hi=6000), 0.0, 0.03)   # faint pre-blip
    d = 0.15
    n = sfx.n_of(sr, d)
    t = sfx.tvec(n, sr)
    b = (np.sin(sfx.TAU * 1700 * t) + 0.35 * np.sin(sfx.TAU * 3400 * t) + 0.6 * np.sin(sfx.TAU * 6900 * t))
    hump = (1.0 + 0.25 * np.exp(-((t - 0.02) / 0.02) ** 2) - 0.2 * np.exp(-((t - 0.085) / 0.025) ** 2)
            + 0.3 * np.exp(-((t - 0.135) / 0.014) ** 2))
    env = np.minimum(t / 0.007, 1.0) * np.minimum((d - t) / 0.004, 1.0)
    b = b + 0.14 * sfx.fftfilt(sfx.noise(n, rng), sr, hp=1500, lp=8000, order=2)   # faint grit so lines are not 100 dB pure
    m.add(b * hump * np.clip(env, 0, 1), 0.05, 0.5)
    # continuous ring tail: 1.7 kHz + 3.4 kHz (-15 dB), starting ~13 dB under the beep, sagging to -40 dB by
    # ~0.4 s after the beep and gone by 0.55 s, with a slow 17 Hz flutter of ~2 dB
    nr = sfx.n_of(sr, 0.36)
    tr = sfx.tvec(nr, sr)
    flut = 1.0 + 0.18 * np.sin(sfx.TAU * 17.0 * tr + 0.7)
    ring = (np.sin(sfx.TAU * 1700 * tr) + 0.18 * np.sin(sfx.TAU * 3400 * tr) + 0.3 * np.sin(sfx.TAU * 6900 * tr)) * flut
    ring *= 10 ** (np.interp(tr, [0.0, 0.15, 0.3, 0.4, 0.5], [-4.0, -12.0, -24.0, -40.0, -70.0]) / 20)
    ring *= np.minimum(tr / 0.004, 1.0)
    ring = ring + 0.1 * sfx.fftfilt(sfx.noise(nr, rng), sr, hp=1500, lp=8000, order=2)
    m.add(ring, 0.2, 0.17)
    return _hp(m.out, sr, 200)


@recipe("democharge_init")
def demo_init(c, N):
    """Latch rattle of four sharp clicks (0.05, 0.11, 0.16, 0.24 s) over a crackle bed, two harmonic beeps
    near 1.4 kHz, a low thump swell, and a faint tail that fades out smoothly."""
    _charge_flags(c, bands=True)
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    for t, g, hi in ((0.05, 0.55, 9000), (0.11, 0.7, 10000), (0.16, 1.0, 10000), (0.24, 0.8, 9000), (0.085, 0.2, 12000), (0.20, 0.25, 12000), (0.285, 0.2, 12000)):
        k = _noise_click(sr, rng, dur=rng.uniform(0.004, 0.008), lo=1500, hi=hi, tau=rng.uniform(0.003, 0.006))
        m.add(k, t, g)
        m.add(_noise_click(sr, rng, dur=0.004, lo=6000, hi=13000), t, g * 0.5)
    # crackle bed 0-0.6 s: 4-12 kHz micro grains, no dead gap between the clicks
    nbd = sfx.n_of(sr, 0.62)
    bed = sfx.grain_cloud(nbd, sr, rng, np.linspace(260, 90, nbd), 0.0025, 4000, 12000, env=sfx.exp_env(nbd, sr, 0.35), amp_var=0.9)
    m.add(bed, 0.0, 0.14)
    for t in (0.34, 0.46):
        b = tone_burst(sr, rng, 1400, 0.08, ratios=(1.0,), gains=(1.0,), attack=0.002, release=0.002, grit=0.25)
        m.add(b, t, 0.06)
    nr = sfx.n_of(sr, 0.6)
    sw = _lp(sfx.noise(nr, rng), sr, 160, order=2)
    sw = _hp(sw, sr, 50) * sfx.smooth_env(nr, sr, 0.15, 0.4)
    m.add(sw / (np.std(sw) + 1e-9), 0.35, 0.10)
    m.add(_thump(sr, 120, 62, 0.3, 0.08), 0.35, 0.28)
    # nearly inaudible tail: 800 Hz line plus low-passed noise, fading to nothing by ~1.2 s
    nt = sfx.n_of(sr, 0.95)
    tt = sfx.tvec(nt, sr)
    tail = np.sin(sfx.TAU * 800 * tt) * 0.15 + _lp(sfx.noise(nt, rng), sr, 1200, order=2) * 0.6
    m.add(tail * 10 ** (np.interp(tt, [0.0, 0.5, 0.8, 0.95], [-4.0, -14.0, -28.0, -60.0]) / 20) * np.minimum(tt / 0.02, 1.0), 0.45, 0.08)
    return m.out


@recipe("democharge_plant")
def demo_plant(c, N):
    """One heavy clipped slap (hard click, 200 Hz-4 kHz noise peaking by 0.04 s, 150-60 Hz thump, driven into
    a soft clipper like the original), then very faint arming ticks over a low rumble bed and a soft
    25-30 Hz-pulsing low hum."""
    _charge_flags(c)
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    sl = Mix(c, sfx.n_of(sr, 0.4))
    sl.add(_noise_click(sr, rng, dur=0.006, lo=1000, hi=4000), 0.0, 1.5)
    nb = sfx.n_of(sr, 0.35)
    slap = _lp(sfx.dark_noise(nb, sr, rng, 4000, 600, steps=6, hp=160), sr, 5000, order=2) * sfx.decay_env(nb, sr, 0.17, 0.28, attack=0.003) * (0.55 + 0.45 * sfx.rand_mod(nb, sr, rng, 40, 1.0))
    sl.add(slap, 0.0, 1.0)
    nm = sfx.n_of(sr, 0.2)   # lift 1-4 kHz so the slap is crunchy rather than boomy
    sl.add(sfx.band_noise(nm, sr, rng, 1000, 4000, order=1) * sfx.exp_env(nm, sr, 0.05, attack=0.002), 0.0, 0.1)
    sl.add(_thump(sr, 150, 60, 0.2, 0.06, k=0.025), 0.0, 0.9)
    s0 = sl.out
    s0 = np.tanh(3.2 * s0 / (np.std(s0[:sfx.n_of(sr, 0.15)]) + 1e-9))   # clipped, dense slap (crest ~4 dB)
    s0 *= np.clip((0.4 - sfx.tvec(len(s0), sr)) / 0.12, 0, 1)
    m.add(s0, 0.0, 1.0)
    # faint arming ticks, 20-30 dB under the slap
    m.add(_noise_click(sr, rng, dur=0.007, lo=1500, hi=4000), 0.35, 0.09)
    m.add(_noise_click(sr, rng, dur=0.006, lo=1500, hi=4000), 0.45, 0.05)
    m.add(_noise_click(sr, rng, dur=0.015, lo=1500, hi=4000, tau=0.006), 0.50, 0.22)
    # low rumble bed under the gaps (no digital silence), 0.25-0.55 s
    nr = sfx.n_of(sr, 0.32)
    bed = _lp(sfx.noise(nr, rng), sr, 300, order=2) * sfx.smooth_env(nr, sr, 0.08, 0.1)
    m.add(_hp(bed, sr, 40) / (np.std(bed) + 1e-9), 0.25, 0.02)
    # hum: 90-130 Hz with ~27 Hz amplitude modulation, rising -30 -> -14 dB over 0.55-0.76 s
    nh = sfx.n_of(sr, 0.21)
    th = sfx.tvec(nh, sr)
    hum = (np.sin(sfx.TAU * 110 * th) + 0.4 * np.sin(sfx.TAU * 220 * th)) * (0.4 + 0.6 * (0.5 + 0.5 * np.sin(sfx.TAU * 27 * th)))
    m.add(hum * 10 ** (np.linspace(-20, -3, nh) / 20), 0.55, 0.5)
    m.add(_lp(sfx.noise(sfx.n_of(sr, 0.12), rng), sr, 600, order=2) * sfx.smooth_env(sfx.n_of(sr, 0.12), sr, 0.03, 0.08), 0.62, 0.1)   # onset bump
    return _dc(m.out, sr)


@recipe("democharge_tick")
def demo_tick(c, N):
    """Spiky buzzy opener (stacked partials near 1.1/2.2/3.4/4.6/7-11 kHz, 130 Hz pulse buzz, sub thump)
    held flat for ~0.085 s and hard-gated, then nine crisp 20-30 ms clicks every 0.1 s that are ~45 dB under it."""
    _charge_flags(c)
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    d = 0.09
    n = sfx.n_of(sr, d)
    t = sfx.tvec(n, sr)
    st = np.zeros(n)
    for f, g in ((1625, 0.5), (2450, 0.45), (3225, 0.6), (4650, 0.8), (6050, 0.5), (7675, 0.5), (9275, 0.4), (10900, 0.4), (12325, 0.3)):
        st += g * np.sin(sfx.TAU * f * t + rng.uniform(0, 6.28))
    buzz = 0.45 + 0.55 * (np.sin(sfx.TAU * 130 * t) > 0)       # 130 Hz pulse amplitude modulation
    st = np.tanh(1.6 * st / 2.2 * buzz) + 0.15 * sfx.fftfilt(sfx.noise(n, rng), sr, hp=900, lp=12000, order=1)
    env = np.minimum(t / 0.0006, 1.0) * np.minimum((0.085 - t) / 0.0025, 1.0)
    st *= np.clip(env, 0, 1)
    m.add(st, 0.0, 0.35)
    m.add(_noise_click(sr, rng, dur=0.002, lo=1500, hi=12000), 0.0, 4.0)   # spike so the peak leads the loudness
    th = _thump(sr, 140, 60, 0.09, 0.03, k=0.02) * np.clip((0.085 - sfx.tvec(sfx.n_of(sr, 0.09), sr)) / 0.0025, 0, 1)
    m.add(th, 0.0, 0.4)
    for i in range(9):
        t0 = 0.1 * (i + 1)
        nn = sfx.n_of(sr, 0.03)
        tt = sfx.tvec(nn, sr)
        g = np.sin(sfx.TAU * 9340 * tt) * 0.4
        g = (g + 0.8 * sfx.noise(nn, rng)) * sfx.exp_env(nn, sr, 0.012, attack=0.0015)
        m.add(sfx.fftfilt(g, sr, hp=4000, lp=11500, order=2), t0, 0.0065)
    return m.out


# =========================================================================== impacts
@recipe("bullet_impact_*")
def bullet_impact(c, N):
    """Quiet pre-tick, then at 0.05 s a hard gritty burst (damped mid-band metal modes, gravel-gated noise),
    a falling 600-250 Hz sweep, separate pitch-dropping sub thumps with dips between them, and a faint ~790 Hz
    ring with a weak partial that fades in and decays exponentially under noisy residue."""
    sr, rng = c.sr, c.rng
    v = ord(c.name[-1]) - ord("a")
    t0 = 0.05
    m = Mix(c, N)
    c.fill_tail = False
    m.add(_noise_click(sr, rng, dur=0.003, lo=1500, hi=6000), 0.022 if v != 2 else 0.01, 0.04)   # pre-tick
    m.add(_noise_click(sr, rng, dur=0.008, lo=3000, hi=8000, tau=0.005, attack=0.0004), t0, 1.0)
    m.add(_modes(sr, rng, 5, 700, 3500, dur=0.09), t0, 0.7)       # damped metal modes (15-35 ms)
    m.add(_modes(sr, rng, 4, 900, 3000, dur=0.09), t0 + 0.03 + 0.01 * v, 0.35)
    nb = sfx.n_of(sr, 0.3)
    burst = sfx.band_noise(nb, sr, rng, 300, 6000, order=1) * sfx.decay_env(nb, sr, 0.08, 0.18, attack=0.0015)
    gate = (rng.random(nb // 40 + 2) > 0.35).astype(float)     # gravel: randomly gated 1-3 ms grains
    gate = np.interp(np.linspace(0, len(gate) - 1, nb), np.arange(len(gate)), gate)
    burst = burst * (0.45 + 0.55 * gate) * (0.6 + 0.4 * sfx.rand_mod(nb, sr, rng, 250, 1.0))
    m.add(burst, t0, 1.35)
    nd = sfx.n_of(sr, 0.25)
    deb = sfx.grain_cloud(nd, sr, rng, np.linspace(600, 80, nd), 0.003, 500, 5500, env=sfx.exp_env(nd, sr, 0.07), amp_var=0.9)
    m.add(_lp(deb, sr, 6500, order=2), t0, 0.65)
    ns = sfx.n_of(sr, 0.22)
    sweep = np.sin(sfx.sweep(650, 250, ns, sr, k=0.08)) * sfx.exp_env(ns, sr, 0.1, attack=0.004) * np.clip((0.22 - sfx.tvec(ns, sr)) / 0.05, 0, 1)
    m.add(sweep, t0, 0.14)
    sets = [((0.08, 1.0, 120), (0.17, 0.8, 110), (0.27, 0.5, 100)),
            ((0.07, 1.0, 110), (0.17, 0.8, 100), (0.29, 0.4, 90)),
            ((0.07, 1.0, 100), (0.175, 0.8, 90)),
            ((0.08, 1.0, 110), (0.18, 0.8, 100), (0.28, 0.55, 90)),
            ((0.06, 1.0, 100), (0.18, 0.7, 90))][v % 5]
    for tt, g, f0 in sets:
        m.add(_thump(sr, f0 * rng.uniform(0.95, 1.05), 50, 0.12, 0.022, k=0.02), tt, 0.55 * g)
    ntl = sfx.n_of(sr, 0.7)
    tl = _lp(sfx.noise(ntl, rng), sr, 1800, order=2) * sfx.decay_env(ntl, sr, 0.12, 0.3, attack=0.01)
    m.add(_hp(tl, sr, 200), t0 + 0.05, 0.06)
    # faint metallic ring (A's ~780 Hz line): 20-40 ms fade-in, one stable partial, weak 1.25 kHz partial,
    # sag of ~3%, exponential decay, fully gone by ~0.7 s; mixed with its own noisy residue
    nr = sfx.n_of(sr, 0.7)
    tr = sfx.tvec(nr, sr)
    f_ring = (780, 790, 800, 785, 795)[v % 5]
    drift = f_ring * (1.0 - 0.03 * np.minimum(tr / 0.6, 1.0))
    ring = np.sin(np.cumsum(sfx.TAU * drift / sr)) + 0.3 * np.sin(np.cumsum(sfx.TAU * drift * 1.6 / sr))
    ring *= np.exp(-tr / 0.2) * np.clip(tr / 0.03, 0, 1) * np.clip((0.7 - tr) / 0.15, 0, 1)
    res = sfx.band_noise(nr, sr, rng, 500, 1400, order=2) * np.exp(-tr / 0.2)
    m.add(ring * 0.5 + res * 0.4, 0.22 if v != 2 else 0.05, 0.016)
    _contour(c, head=[(0.0, -44.0), (0.04, -44.0), (0.048, -30.0), (0.056, -1.0), (0.075, 0.0), (0.12, -2.0), (0.14, -9.0)],
             t_from=0.14, smooth=0.04)
    c.env_smooth = 0.012
    return m.out


# =========================================================================== blasts (shared private engine)
def _boom(c, N, *, att=0.004, crack=1.0, crack_hi=12000, crack_dur=0.02, sub=(90.0, 36.0, 0.3), sub_gain=1.0,
          body_f=(1500.0, 250.0), body_lp=700.0, t20=0.6, t40=1.4, body_gain=1.0, rumble_gain=0.6, churn_rate=9,
          churn_depth=0.5, bumps=(), bump_lp=1500, bump_dur=0.06, tail_gain=0.6, tail_end=None, grit=0.0,
          hiss_hi=8000, hiss_gain=0.0, hiss_tau=0.3, debris=0.0, debris_hi=1500):
    """Private blast engine: one coherent event, no gating.  A crack at 0, a sub boom that sweeps down, a
    noise body whose low-pass closes from ``body_f[0]`` to ``body_f[1]`` (so the tail darkens), surges at
    ``bumps`` = [(t, gain)], and a smooth low-passed tail.  ``conform`` pins the contour afterwards."""
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    full = N / sr
    if crack:
        m.add(crack_burst(sr, rng, tau=rng.uniform(0.005, 0.009), lo=700, hi=crack_hi, dur=crack_dur * 4), 0.0, 1.5 * crack)
        nk = sfx.n_of(sr, crack_dur * 2.5)
        m.add(sfx.band_noise(nk, sr, rng, 1500, 6000, order=1) * sfx.exp_env(nk, sr, crack_dur * 0.9, attack=0.001), 0.0, 0.7 * crack)
    n = N
    body = sfx.dark_noise(n, sr, rng, body_f[0], body_f[1], steps=8, hp=60)
    body = _lp(body, sr, max(body_lp, body_f[1]), order=2)
    body = body * sfx.decay_env(n, sr, t20, t40, attack=att)
    body = body * churn(n, sr, rng, churn_rate, churn_depth)
    if grit:
        sd = np.std(body) + 1e-9
        body = sfx.waveshape(body / sd, 1.0 + grit * 3) * sd
    m.add(body, 0.0, 1.2 * body_gain)
    sb0, sb1, sbd = sub
    m.add(sub_drop(sr, sb0, sb1, max(0.5, sbd * 4), sbd, k=sbd * 0.12, attack=max(att, 0.0015)), 0.0, sub_gain)
    if rumble_gain:
        rb = sfx.fftfilt(sfx.noise(n, rng), sr, hp=24, lp=140, order=2) * sfx.decay_env(n, sr, t20 * 1.2, t40, attack=att + 0.005)
        m.add(rb * churn(n, sr, rng, 7, 0.45), 0.0, rumble_gain)
    for (tb, gb) in bumps:
        nb = sfx.n_of(sr, bump_dur * 2)
        bp = sfx.dark_noise(nb, sr, rng, bump_lp, 250, steps=4, hp=50) * sfx.smooth_env(nb, sr, bump_dur * 0.25, bump_dur * 1.3)
        m.add(bp, tb - bump_dur * 0.2, gb)
    if debris:
        nd = sfx.n_of(sr, min(t40 * 1.1, full))
        dd = sfx.grain_cloud(nd, sr, rng, np.linspace(70, 10, nd), 0.008, 250, debris_hi, env=sfx.decay_env(nd, sr, t20, t40), amp_var=0.9)
        m.add(dd, 0.05, debris)
    if hiss_gain:
        nh = sfx.n_of(sr, min(2.5, full))
        hs = sfx.band_noise(nh, sr, rng, 4000, hiss_hi, order=1) * sfx.exp_env(nh, sr, hiss_tau, attack=0.01)
        m.add(hs, 0.0, hiss_gain)
    te = tail_end if tail_end else min(t40 * 1.5 + 0.3, full)
    nt = sfx.n_of(sr, te)
    tail = sfx.dark_noise(nt, sr, rng, 900, 160, steps=6, hp=45) * sfx.decay_env(nt, sr, t20 * 1.0, t40, attack=0.03)
    m.add(tail * churn(nt, sr, rng, 8, 0.3), 0.0, tail_gain)
    return m.out


@recipe("biodeath")
def biodeath(c, N):
    """Hard organic blast: 10-20 ms crack and 80->35 Hz thump, a sub body that holds to ~1.4 s then fades,
    wet slime squelch (granular 500 Hz-6 kHz bursts with fast random modulation, accents at 0.87/1.15/1.23 s)
    and a smooth dark tail that fades by ~3.5 s."""
    sr, rng = c.sr, c.rng
    tg = get_tg(c)
    tg.trail = 0.35   # A is still audible at ~3.2 s; fade comes from the contour, not conform's cut
    out = _boom(c, N, att=0.03, crack=1.5, crack_dur=0.018, sub=(95.0, 36.0, 0.5), sub_gain=1.1, body_f=(4500.0, 500.0),
                body_lp=2500, t20=1.7, t40=3.0, body_gain=1.4, rumble_gain=1.1, churn_rate=12, churn_depth=0.4, tail_gain=0.9,
                tail_end=3.7, hiss_gain=0.02, hiss_tau=1.2, hiss_hi=12000)
    m = Mix(c, N)
    # wet squelch layer: bandpassed noise with 20-60 ms granular bursts and a fast 15-30 Hz slimy modulation
    nq = sfx.n_of(sr, 2.2)
    sq = sfx.band_noise(nq, sr, rng, 500, 6000, order=1)
    sq = sq * (_flutter(nq, sr, rng, 22, 0.9, jitter=0.35) ** 2) * sfx.decay_env(nq, sr, 1.1, 1.9, attack=0.01)
    m.add(sq, 0.05, 0.9)
    for tb, gb, d in ((0.87, 1.0, 0.07), (1.15, 0.85, 0.05), (1.23, 0.9, 0.06)):
        nb = sfx.n_of(sr, d)
        bu = sfx.band_noise(nb, sr, rng, 500, 5000, order=1) * sfx.smooth_env(nb, sr, d * 0.2, d * 0.7)
        bu = bu * sfx.rand_mod(nb, sr, rng, 28, 0.8)
        m.add(bu, tb, 0.55 * gb)
    # a couple of tiny late blips only, then a faint high hiss that lifts the late centroid
    for tb in (2.4, 2.6, 3.05):   # quiet wet blips, 10-15 dB under the body
        nb_ = sfx.n_of(sr, 0.03)
        m.add(sfx.band_noise(nb_, sr, rng, 500, 4000, order=1) * sfx.smooth_env(nb_, sr, 0.006, 0.02), tb, 0.03)
    nh = sfx.n_of(sr, 1.6)
    m.add(sfx.band_noise(nh, sr, rng, 6000, 9000, order=1) * sfx.exp_env(nh, sr, 0.5, attack=0.05), 0.5, 0.003)
    _contour(c, smooth=0.12, head=[(0.0, -20.0), (0.02, -8.0), (0.09, 0.0), (0.3, -1.0)], t_from=0.3,
             extra=[(1.5, -20.0), (2.9, -40.0), (3.4, -62.0), (3.8, -80.0)])
    c.env_smooth = 0.03
    return out + fit(m.out, len(out))


def _splat(sr, rng, g=1.0, dur=0.03, lo=600, hi=1800):
    """Soft wet drip: a 25-40 ms low-Q 600-1800 Hz band burst (never above ~4 kHz) with a faint low body."""
    n = sfx.n_of(sr, 0.1)
    sp = sfx.fftfilt(sfx.noise(n, rng), sr, hp=lo * rng.uniform(0.8, 1.3), lp=hi * rng.uniform(0.8, 1.3), order=1)
    sp = sp * sfx.exp_env(n, sr, dur * 0.5, attack=0.0006)
    th = sub_drop(sr, rng.uniform(200, 320), 120, 0.03, 0.01, k=0.01)
    return _sum(sp * g, th * g * 0.15)


def _gore_wet(sr, rng, dur, lo, hi, rate, t20, t40, depth=0.9):
    """Continuous wet squelch: bandpassed noise broken into 20-40 ms micro bursts, decaying."""
    n = sfx.n_of(sr, dur)
    w = sfx.band_noise(n, sr, rng, lo, hi, order=1)
    return w * (_flutter(n, sr, rng, rate, depth, jitter=0.3) ** 2) * sfx.decay_env(n, sr, t20, t40, attack=0.002)


@recipe("gore_*")
def gore(c, N):
    """Gore family.  Instant crunch (noise crack, 1-8 kHz grit), a thump that starts at full level and
    decays, a continuous wet micro-burst squelch (no tonal drone), small clicky splats only where the
    original has them (quiet), and a thin diffuse tail of faint drips ending in silence."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    v = c.name[-1]
    m = Mix(c, N)
    # hard front: crack + crunch
    cb = {"a": 1.9, "b": 1.6, "c": 2.2}[v]
    m.add(crack_burst(sr, rng, tau=0.006, lo=800, hi=12000, dur=0.08), 0.0, cb)
    m.add(_noise_click(sr, rng, dur=0.01, lo=5000, hi=9000), 0.0, 0.5 * cb)
    ncr = sfx.n_of(sr, 0.18)
    m.add(sfx.band_noise(ncr, sr, rng, 1000, 8000, order=1) * sfx.exp_env(ncr, sr, 0.04, attack=0.001) *
          (0.6 + 0.4 * sfx.rand_mod(ncr, sr, rng, 80, 1.0)), 0.0, 1.0)
    if v == "a":
        t20, t40, dur_w, wet_g, thump = 0.8, 1.8, 1.9, 0.9, (110.0, 50.0, 0.12, 0.9)
        splats = [0.93, 1.03, 1.13, 1.22, 1.31]
        end_t = 2.2
    elif v == "b":
        t20, t40, dur_w, wet_g, thump = 0.45, 1.0, 1.4, 1.0, (120.0, 45.0, 0.1, 1.0)
        splats = [t for t in tg.onsets if t > 0.5][:10]
        end_t = 1.9
    else:
        t20, t40, dur_w, wet_g, thump = 0.5, 1.5, 1.7, 1.8, (90.0, 55.0, 0.09, 0.8)
        splats = []
        end_t = 2.2
    # thump: pitched drop that starts at full level and decays
    m.add(_thump(sr, thump[0] * 1.3, thump[1], 0.5, thump[2], k=0.04), 0.0, thump[3])
    nb = sfx.n_of(sr, 1.2)
    sub = _lp(sfx.noise(nb, rng), sr, 150, order=2) * sfx.decay_env(nb, sr, t20 * 0.8, t40 * 0.8, attack=0.004)
    m.add(_hp(sub, sr, 30) / (np.std(sub) + 1e-9), 0.0, 0.18)
    # wet squelch body
    m.add(_gore_wet(sr, rng, dur_w, 500, 4500, 32, t20 * 0.45, t40 * 0.6), 0.02, 0.55 * wet_g)
    m.add(_gore_wet(sr, rng, dur_w, 1500, 8000, 45, t20 * 0.3, t40 * 0.4, depth=0.95), 0.0, 0.35 * wet_g)
    # splats: small clicky pops at the original's times (about -25 dB under the opening burst)
    for t in splats:
        m.add(_splat(sr, rng, 1.0), t, 0.07 * rng.uniform(0.7, 1.0))
    # sparse faint drips (gore_c: 8 of them between 1.2 s and the end, one slightly louder blip near 1.89 s)
    drip_lo = 1.2 if v == "c" else end_t * 0.45
    if v == "c":
        m.add(_splat(sr, rng, 1.0, dur=0.01, lo=1000, hi=3000), 1.89, 0.03)
    for i in range(8 if v == "c" else 4):
        m.add(_splat(sr, rng, 1.0, dur=0.012, lo=1000, hi=3000), drip_lo + (end_t - drip_lo) * rng.uniform(0.05, 0.95), 0.012)
    # low rumble fade to silence
    nr = sfx.n_of(sr, end_t)
    rb = _hp(_lp(sfx.noise(nr, rng), sr, 140, order=2), sr, 35) * sfx.decay_env(nr, sr, end_t * 0.4, end_t * 0.9, attack=0.01)
    rb = rb * np.clip((end_t - sfx.tvec(nr, sr)) / 0.15, 0, 1)
    m.add(rb / (np.std(rb) + 1e-9), 0.0, 0.1)
    out = m.out
    tg_ = np.arange(len(out)) / sr
    fade_w = {"a": 0.1, "b": 0.04, "c": 0.3}[v]
    out = out * np.clip((end_t + fade_w - tg_) / fade_w, 0, 1)   # block stops at end_t (b: a 40 ms cut like A)
    if v == "a":
        _contour(c, smooth=0.08, head=[(0.0, -3.0), (0.02, 0.0), (0.2, -2.0)], t_from=0.2)
    else:
        _contour(c, smooth=0.08)
    c.env_smooth = 0.02
    return out


@recipe("disintegrate")
def disintegrate(c, N):
    """Hot sharp burst (crack, 120->45 Hz thump) into a sizzling bed that decays exponentially, riddled
    with Poisson crackle clicks that thin out over time, ~14 louder pops late, and a low rumble that fades
    while the hiss brightens."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    m.add(crack_burst(sr, rng, tau=0.012, lo=800, hi=12000, dur=0.1), 0.0, 1.4)
    nk = sfx.n_of(sr, 0.05)
    m.add(sfx.band_noise(nk, sr, rng, 1000, 6000, order=1) * sfx.exp_env(nk, sr, 0.014, attack=0.0008), 0.0, 1.0)
    m.add(sub_drop(sr, 120, 45, 1.2, 0.4, k=0.1, attack=0.002), 0.0, 1.0)
    m.add(_modes(sr, rng, 4, 700, 3000, dur=0.06), 0.0, 0.5)
    # sizzling hiss bed: 3-12 kHz with fast 20-60 Hz irregular jitter, decays with the contour
    hs = sfx.band_noise(N, sr, rng, 3000, 12000, order=2)
    hs = hs * _flutter(N, sr, rng, 40, 0.75, jitter=0.4)
    m.add(hs, 0.0, 0.55)
    # Poisson crackle: dense early, thinning to a few per second
    tgrid = np.arange(N) / sr
    dens = np.where(tgrid < 1.5, 40.0, np.where(tgrid < 2.5, 15.0, np.where(tgrid < 4.7, 5.0, 0.2)))
    ck = sfx.grain_cloud(N, sr, rng, dens * 2.5, 0.002, 2000, 8000, amp_var=0.95)
    m.add(ck, 0.0, 1.3)
    # rumble: low-passed noise with slow modulation, fading faster than the hiss
    lo = sfx.fftfilt(sfx.noise(N, rng), sr, hp=30, lp=300, order=2) * churn(N, sr, rng, 6, 0.6) * sfx.exp_env(N, sr, 1.1)
    m.add(lo / (np.std(lo) + 1e-9), 0.0, 0.2)
    for t in tg.onsets[:14]:
        n = sfx.n_of(sr, 0.05)
        pp = sfx.band_noise(n, sr, rng, 1500, 9000, order=1) * sfx.exp_env(n, sr, 0.011, attack=0.0004)
        m.add(pp, t, 1.8 * rng.uniform(0.6, 1.0))
    _contour(c, head=[(0.0, -3.0), (0.03, 0.0), (0.12, -1.0)], t_from=0.12, smooth=0.1)
    c.env_smooth = 0.05
    return m.out


# --------------------------------------------------------------------------- explosions
@recipe("explosion1_*")
def explosion1(c, N):
    """Clipped crack, huge dark sub boom, a low-passed noise roar whose cutoff keeps closing (the tail
    darkens), debris crackle grains and soft low bumps at the original's onsets, smooth exponential fade."""
    tg = get_tg(c)
    v = c.name[-1]
    on = [t for t in tg.onsets if t > 0.3]
    if v == "b":
        on = [t for t in on if t < 1.4]   # no sharp late burst at 1.5 s
    bumps = tuple((t - 0.02, 0.5 if v != "e" else 0.35) for t in on[:8])
    if v == "a":    # slower ramp (roar peaks ~0.08 s), darkening tail, debris at the original's times
        out = _boom(c, N, att=0.06, crack=0.8, crack_dur=0.012, sub=(100.0, 40.0, 0.3), sub_gain=0.8, body_f=(1500.0, 200.0),
                    body_lp=600, t20=1.2, t40=2.0, rumble_gain=0.9, churn_rate=9, churn_depth=0.35, bumps=bumps,
                    bump_lp=700, tail_gain=0.7, debris=0.12, debris_hi=1500, tail_end=2.5)
        _contour(c, head=[(0.0, -30.0), (0.015, -8.0), (0.06, -1.0), (0.09, 0.0), (0.2, -0.5)], t_from=0.2, smooth=0.2)
    elif v == "b":
        out = _boom(c, N, att=0.006, crack=1.6, crack_dur=0.03, sub=(100.0, 40.0, 0.25), sub_gain=1.0, body_f=(4000.0, 300.0),
                    body_lp=400, t20=0.9, t40=1.45, rumble_gain=1.0, churn_rate=14, churn_depth=0.5, bumps=bumps,
                    bump_lp=800, tail_gain=0.8, debris=0.4, debris_hi=5000, tail_end=1.55)
        _contour(c, head=[(0.0, -26.0), (0.01, -8.0), (0.04, 0.0), (0.1, -2.0), (0.4, -8.0)], t_from=0.4, smooth=0.25,
                 extra=[(0.6, -14.0), (1.0, -20.0), (1.5, -40.0), (1.6, -70.0)])
    elif v == "c":
        out = _boom(c, N, att=0.004, crack=1.7, crack_dur=0.05, sub=(70.0, 40.0, 0.25), sub_gain=1.1, body_f=(2000.0, 350.0),
                    body_lp=900, t20=0.65, t40=1.4, rumble_gain=1.0, churn_rate=8, churn_depth=0.4,
                    bumps=tuple((t - 0.02, 0.9) for t in on[:8]), bump_lp=800, bump_dur=0.05, tail_gain=0.8, debris=0.1, tail_end=1.65)
        _contour(c, head=[(0.0, -26.0), (0.01, -8.0), (0.05, 0.0), (0.1, -3.0), (0.4, -9.0)], t_from=0.4, smooth=0.2,
                 extra=[(0.5, -13.0), (0.77, -20.0), (1.5, -40.0), (1.62, -72.0)])
    else:           # e: continuous dense crackling rumble, no gated bursts in the tail
        out = _boom(c, N, att=0.01, crack=1.0, crack_dur=0.03, sub=(85.0, 34.0, 0.4), sub_gain=1.1, body_f=(1500.0, 200.0),
                    body_lp=500, t20=0.5, t40=2.3, rumble_gain=1.8, churn_rate=9, churn_depth=0.3, bumps=bumps,
                    bump_lp=600, tail_gain=1.8, debris=0.35, debris_hi=1200, tail_end=2.9)
        sr, rng = c.sr, c.rng
        m = Mix(c, N)
        nc = sfx.n_of(sr, 2.5)
        ck = sfx.grain_cloud(nc, sr, rng, np.linspace(90, 25, nc), 0.006, 200, 1400, env=sfx.exp_env(nc, sr, 1.0), amp_var=0.9)
        m.add(ck, 0.1, 0.2)
        # darken everything after the first 0.3 s: low-pass sweeping ~3 kHz -> 400 Hz by 2 s (no bright chop)
        tt_ = np.arange(len(out)) / sr
        dark = _lp(out, sr, 700, order=4)
        w = np.clip((tt_ - 0.2) / 0.5, 0, 1)
        out = out * (1 - w) + dark * w
        out = out + _lp(fit(m.out, len(out)), sr, 1500, order=2)
        sbn = _hp(_lp(sfx.noise(len(out), rng), sr, 120, order=2), sr, 35) * sfx.exp_env(len(out), sr, 1.1, attack=0.2)
        out = out + sbn / (np.std(sbn) + 1e-9) * 0.5 * np.clip((2.7 - tt_) / 0.4, 0, 1)
        _contour(c, head=[(0.0, -20.0), (0.01, -6.0), (0.05, -1.0), (0.28, 0.0), (0.4, -3.0)], t_from=0.4, smooth=0.3,
                 extra=[(2.35, -30.0), (2.6, -50.0), (2.75, -75.0)])
    c.env_smooth = 0.03
    return out


@recipe("nukeexplosion")
def nuke(c, N):
    """Crack and a second hit at the start, a sub boom that swells and falls from 80 to 35 Hz over 1.5 s,
    a continuous noise bed whose low-pass slides 1.5 kHz -> 250 Hz over 9 s so the tail never gaps and always
    darkens, soft low debris thumps late (no full-band streaks), steep 10 kHz low-pass, silence by ~9.4 s."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    m.add(crack_burst(sr, rng, tau=0.025, lo=200, hi=6000, dur=0.2), 0.05, 1.2)
    m.add(crack_burst(sr, rng, tau=0.03, lo=150, hi=4500, dur=0.25), 0.69, 0.8)
    # sub boom: 0.3 s swell, falls 80 -> 35 Hz over 1.5 s, ~3 s decay
    nsb = sfx.n_of(sr, 6.0)
    ts = sfx.tvec(nsb, sr)
    ph = np.cumsum(sfx.TAU * (35 + 45 * np.exp(-ts / 0.7)) / sr)
    boom = np.sin(ph) * (1 - np.exp(-ts / 0.12)) * np.exp(-ts / 2.2)
    m.add(boom, 0.05, 1.2)
    # noise bed: low-pass closes 1.5 kHz -> 250 Hz across the whole sound, lumpy but never gated
    bed = sfx.dark_noise(N, sr, rng, 1500, 250, steps=10, hp=35)
    bed = _lp(bed, sr, 1200, order=2)
    bed = bed * churn(N, sr, rng, 3, 0.4) * sfx.decay_env(N, sr, 2.4, 7.7, attack=0.03)
    m.add(bed, 0.0, 1.1)
    rb = _hp(_lp(sfx.noise(N, rng), sr, 120, order=2), sr, 22) * sfx.decay_env(N, sr, 3.0, 8.0, attack=0.2) * churn(N, sr, rng, 2.5, 0.5)
    m.add(rb / (np.std(rb) + 1e-9), 0.0, 0.8)
    tl2 = _hp(_lp(sfx.noise(N, rng), sr, 220, order=2), sr, 25) * churn(N, sr, rng, 1.5, 0.5)
    tl2 = tl2 * np.exp(-np.arange(N) / sr / 5.0) * np.clip((9.6 - np.arange(N) / sr) / 0.6, 0, 1)
    m.add(tl2 / (np.std(tl2) + 1e-9), 0.0, 1.0)   # dark continuous tail bed so it never gaps
    # late debris: 18 irregular low rumble bursts (150-300 Hz noise, 80-200 ms decays, a few with a 40-70 Hz throb)
    for t in sorted(rng.uniform(3.6, 8.8, 18)):
        nn = sfx.n_of(sr, 0.6)
        tn = sfx.tvec(nn, sr)
        th = _lp(sfx.noise(nn, rng), sr, rng.uniform(150, 300), order=2) * np.exp(-tn / rng.uniform(0.08, 0.2)) * np.minimum(tn / 0.01, 1.0)
        if rng.random() < 0.4:
            th = th + 0.6 * np.sin(sfx.TAU * rng.uniform(40, 70) * tn) * np.exp(-tn / 0.2)
        m.add(th / (np.std(th) + 1e-9), t, 0.5 * rng.uniform(0.6, 1.0))
    get_tg(c).trail = 3.85   # A is still audible at 9.6 s: keep conform from cutting the tail early
    out = _lp(m.out, sr, 10000, order=4)
    # after ~3 s the whole tail slides under a 900 Hz low-pass so it keeps rumbling (centroid ~110-150 Hz)
    tt_ = np.arange(len(out)) / sr
    w = np.clip((tt_ - 2.5) / 1.5, 0, 1)
    out = out * (1 - w) + _lp(out, sr, 800, order=4) * w
    _contour(c, head=[(0.0, -18.0), (0.04, -12.0), (0.12, -6.0), (0.69, -4.0), (0.75, -2.0), (1.1, 0.0), (2.0, -1.0)], t_from=2.0,
             smooth=0.8, extra=[(2.5, -6.0), (3.3, -14.0), (7.7, -27.0), (9.0, -36.0), (9.4, -44.0), (9.6, -80.0)])
    c.env_smooth = 0.1
    return out


# --------------------------------------------------------------------------- steam armor jump
@recipe("steamarmor_jump_*")
def steam_jump(c, N):
    """Starts at 0 s from about -20 dB with a short valve click and a 60 ms swell to the peak at ~0.07 s;
    a gritty churning rumble whose 100-400 Hz body keeps its weight, irregular (jittered) flutter instead of
    a steady beat, a steam hiss that decays faster than the low end, and a short irregular tail."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    v = c.name[-1]
    m = Mix(c, N)
    n = N
    # valve click + thin 6-10 kHz puff
    m.add(_noise_click(sr, rng, dur=0.008, lo=2000, hi=6000), 0.0, 0.5 if v in "cd" else 0.7)
    npf = sfx.n_of(sr, 0.05)
    m.add(sfx.band_noise(npf, sr, rng, 6000, 10000, order=1) * sfx.exp_env(npf, sr, 0.014), 0.0, 0.08)
    # low body: 60-250 Hz noise with irregular flutter, falling edge gentle
    ts_ = sfx.tvec(n, sr)
    t20, t40 = {"a": (0.63, 1.05), "b": (0.66, 1.08), "c": (0.72, 1.06), "d": (0.82, 1.1)}[v]
    envm = sfx.decay_env(n, sr, t20, t40, attack=0.06)
    sub = _hp(_lp(sfx.noise(n, rng), sr, 90, order=2), sr, 25) * _flutter(n, sr, rng, 28, 0.55)
    # ±3 Hz random drift tone only as a faint rumble base, never a steady sine
    drift = 52 + 3 * (sfx.rand_mod(n, sr, rng, 3, 1.0) - 0.5) * 2
    tone = np.sin(np.cumsum(sfx.TAU * drift / sr)) * _flutter(n, sr, rng, 30, 0.6)
    m.add((sub / (np.std(sub) + 1e-9) * 0.7 + tone * 0.35) * envm, 0.0, 0.8)
    mid = _hp(_lp(sfx.noise(n, rng), sr, 420, order=2), sr, 90) * _flutter(n, sr, rng, 33, 0.65, jitter=0.3)
    m.add(mid / (np.std(mid) + 1e-9) * envm, 0.0, 0.7)
    # body of grit 400 Hz-2 kHz
    gr = sfx.band_noise(n, sr, rng, 400, 2000, order=1) * _flutter(n, sr, rng, 45, 0.8, jitter=0.35)
    m.add(gr / (np.std(gr) + 1e-9) * envm * np.exp(-ts_ / 0.5), 0.0, 0.18)
    # steam hiss 500 Hz-6 kHz, decays faster than the low end
    hs = sfx.band_noise(n, sr, rng, 500, 6000, order=1) * _flutter(n, sr, rng, 38, 0.7)
    m.add(hs / (np.std(hs) + 1e-9) * sfx.decay_env(n, sr, 0.30, 0.7, attack=0.02), 0.0, 0.17)
    # sustained churning plateau (150-900 Hz) that holds to ~0.45 s and then slides away: keeps the chuff dense
    plat = {"a": 0.0, "b": 0.3, "c": 0.7, "d": 1.1}[v]
    if plat:
        pm = _hp(_lp(sfx.noise(n, rng), sr, 900, order=2), sr, 120) * _flutter(n, sr, rng, 26, 0.6, jitter=0.4)
        pe = np.clip(1 - (ts_ - 0.45) / 0.55, 0, 1) ** 1.5 * np.minimum(ts_ / 0.03, 1.0)
        m.add(pm / (np.std(pm) + 1e-9) * pe, 0.0, 0.7 * plat)
    # second surge / accents per variant
    if v == "d":
        m.add(_lp(sfx.noise(sfx.n_of(sr, 0.1), rng), sr, 500) * sfx.smooth_env(sfx.n_of(sr, 0.1), sr, 0.03, 0.06), 0.27, 0.35)
    if v == "b":
        m.add(_lp(sfx.noise(sfx.n_of(sr, 0.1), rng), sr, 600) * sfx.smooth_env(sfx.n_of(sr, 0.1), sr, 0.03, 0.06), 0.31, 0.3)
        for tc in (0.09, 0.17, 0.26, 0.37):   # a few short metallic clank resonances
            f0 = rng.uniform(600, 1500)
            nc = sfx.n_of(sr, 0.04)
            tcv = sfx.tvec(nc, sr)
            m.add(np.sin(sfx.TAU * f0 * tcv) * np.exp(-tcv / 0.012) * (sfx.noise(nc, rng) * 0.4 + 0.6), tc + rng.uniform(-0.01, 0.01), 0.06)
    # jump thump
    m.add(_thump(sr, 105, 45, 0.3, 0.07, k=0.03), 0.0, 0.35)
    if v == "a":
        _contour(c, head=[(0.0, -20.0), (0.03, -9.0), (0.07, 0.0), (0.2, -2.0)], t_from=0.2, smooth=0.12)
    elif v == "b":
        _contour(c, head=[(0.0, -18.0), (0.03, -5.0), (0.05, 0.0), (0.2, -3.0), (0.35, -1.5)], t_from=0.35, smooth=0.12)
    elif v == "c":
        _contour(c, head=[(0.0, -20.0), (0.02, -6.0), (0.07, 0.0), (0.15, -2.0)], t_from=0.15, smooth=0.12,
                 extra=[(0.5, -17.0)])
    else:
        _contour(c, head=[(0.0, -20.0), (0.04, -9.0), (0.10, -2.0), (0.3, 0.0)], t_from=0.3, smooth=0.12,
                 extra=[(0.4, -3.0), (0.5, -5.0), (0.65, -8.0), (0.8, -12.0)])
    c.env_smooth = 0.03
    return m.out
