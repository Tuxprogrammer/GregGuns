"""Effects: charges, impacts, explosions, gore, disintegration, steam jump.

Original synthesis only; see sounds_guns.py for the round-1 design rules.  The C4 / demolition charge
sounds are the one place where the originals really contain electronic beeps, so they use gated beeps
(with click-sharp attacks and thumps), everything else is shaped noise.
"""
from __future__ import annotations

import numpy as np

import sfx
from sndlib import Mix, get_tg, recipe, REGISTRY, crack_burst, sub_drop, mech_click
from sndengines import (fit, blast, events, rumble, churn, tone_burst, click_thump, foley_event, rel_env_at, _sum)


# =========================================================================== C4 / demolition charges
@recipe("c4charge_init")
def c4_init(c, N):
    """Three tight clicks with a small thump (0, 0.10, 0.21 s), then a ~3.85 kHz beep with a hard gate."""
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    for t, g in ((0.0, 1.0), (0.10, 0.9), (0.21, 1.0)):
        m.add(click_thump(sr, rng, 1.0, 0.7, bright=1.2), t, g)
    m.add(tone_burst(sr, rng, 3850, 0.12, ratios=(1.0, 2.0), gains=(1.0, 0.12), release=0.002), 0.30, 0.3)
    m.add(click_thump(sr, rng, 0.5, 0.0, bright=1.0), 0.6, 0.35)
    nb = sfx.n_of(sr, 0.3)
    m.add(sfx.band_noise(nb, sr, rng, 2500, 11000, order=1) * sfx.exp_env(nb, sr, 0.09) * churn(nb, sr, rng, 60, 0.7), 0.3, 0.12)
    return m.out


@recipe("c4charge_plant")
def c4_plant(c, N):
    """Crunchy slap with a heavy thump, two sharp clicks, rustle, then 4.5 kHz beeps (three short, one long)."""
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    m.add(crack_burst(sr, rng, tau=0.012, lo=400, hi=12000, dur=0.15), 0.0, 1.1)
    nb = sfx.n_of(sr, 0.2)
    m.add(sfx.dark_noise(nb, sr, rng, 5000, 400, steps=5, hp=120) * sfx.exp_env(nb, sr, 0.05), 0.0, 0.9)
    m.add(sub_drop(sr, 260, 55, 0.25, 0.07, k=0.03), 0.0, 1.0)
    m.add(click_thump(sr, rng, 1.0, 0.0, bright=1.3), 0.21, 0.7)
    m.add(click_thump(sr, rng, 1.0, 0.0, bright=1.3), 0.28, 0.6)
    ns = sfx.n_of(sr, 0.23)
    m.add(sfx.band_noise(ns, sr, rng, 800, 6000, order=1) * churn(ns, sr, rng, 40, 0.9) ** 2 * sfx.smooth_env(ns, sr, 0.02, 0.05), 0.37, 0.25)
    for t, d in ((0.63, 0.07), (0.72, 0.07), (0.80, 0.07), (0.89, 0.28)):
        m.add(tone_burst(sr, rng, 4500, d, ratios=(1.0, 2.0), gains=(1.0, 0.1), release=0.003), t, 0.8)
        m.add(sub_drop(sr, 200, 90, 0.08, 0.025, k=0.015), t, 0.4)
    return m.out


@recipe("c4charge_tick")
def c4_tick(c, N):
    """Clean 1.7 kHz beep with 3.4 and 6.9 kHz harmonics, two-hump accent, sharp drop into a quiet ringing tail."""
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    m.add(click_thump(sr, rng, 0.8, 0.3, bright=1.0), 0.05, 1.0)
    d = 0.15
    b = tone_burst(sr, rng, 1700, d, ratios=(1.0, 2.0, 4.06), gains=(1.0, 0.35, 0.15), release=0.002)
    t = sfx.tvec(len(b), sr)
    hump = 0.75 + 0.25 * np.exp(-((t - 0.035) / 0.02) ** 2) + 0.25 * np.exp(-((t - 0.11) / 0.02) ** 2)
    m.add(b * hump, 0.05, 1.0)
    nr = sfx.n_of(sr, 0.45)
    tr = sfx.tvec(nr, sr)
    ring = (np.sin(sfx.TAU * 1700 * tr) + 0.3 * np.sin(sfx.TAU * 3400 * tr)) * np.exp(-tr / 0.12)
    m.add(ring, 0.2, 0.06)
    return m.out


@recipe("democharge_init")
def demo_init(c, N):
    """Uneven latch clicks, two short harmonic beeps near 1.4 kHz, a swelling low rumble, long fade."""
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    for t, g in ((0.05, 1.0), (0.16, 0.85), (0.24, 1.0)):
        m.add(click_thump(sr, rng, 1.0, 0.0, bright=1.3), t, g)
    for t in (0.34, 0.48):
        m.add(tone_burst(sr, rng, 1400, 0.07, ratios=(1.0, 2.5, 3.9), gains=(1.0, 0.35, 0.2), release=0.004), t, 0.8)
    nr = sfx.n_of(sr, 0.3)
    m.add(sfx.band_noise(nr, sr, rng, 55, 160, order=2) * sfx.smooth_env(nr, sr, 0.1, 0.12), 0.33, 0.5)
    nb = sfx.n_of(sr, 0.9)
    m.add(sfx.band_noise(nb, sr, rng, 3000, 12000, order=1) * sfx.exp_env(nb, sr, 0.25) * churn(nb, sr, rng, 45, 0.8), 0.05, 0.25)
    return m.out


@recipe("democharge_plant")
def demo_plant(c, N):
    """One heavy clipped slap (thick 20-250 Hz body, noise to 10 kHz), soft arming clicks, a faint low hum."""
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    m.add(crack_burst(sr, rng, tau=0.014, lo=300, hi=11000, dur=0.2), 0.0, 1.2)
    nb = sfx.n_of(sr, 0.3)
    m.add(sfx.dark_noise(nb, sr, rng, 6000, 300, steps=6, hp=60) * sfx.exp_env(nb, sr, 0.07), 0.0, 1.0)
    m.add(sub_drop(sr, 220, 45, 0.3, 0.09, k=0.04), 0.0, 1.1)
    for t, g in ((0.35, 0.3), (0.50, 0.4), (0.62, 0.35)):
        m.add(click_thump(sr, rng, 1.0, 0.1, bright=0.9), t, g)
    nh = sfx.n_of(sr, 0.3)
    m.add(sfx.band_noise(nh, sr, rng, 20, 200, order=2) * churn(nh, sr, rng, 25, 0.8), 0.46, 0.35)
    return m.out


@recipe("democharge_tick")
def demo_tick(c, N):
    """Loud buzzy opener (stacked partials near 1.1/2.2/4.6/7-11 kHz) gated at ~0.09 s, then eight tiny
    chirp-like clicks at 6 and 10 kHz, one every 0.1 s, far quieter."""
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    d = 0.09
    op = tone_burst(sr, rng, 1100, d, ratios=(1.0, 2.0, 4.2, 7.0, 9.8), gains=(1.0, 0.7, 0.5, 0.4, 0.3), attack=0.0005,
                    release=0.002, grit=0.15)
    m.add(op, 0.0, 1.0)
    for i in range(8):
        f = 6000 if i % 2 == 0 else 10000
        t0 = 0.11 + i * 0.1
        n = sfx.n_of(sr, 0.025)
        ch = np.sin(sfx.sweep(f * 0.9, f * 1.1, n, sr, k=0.01)) * sfx.exp_env(n, sr, 0.008, attack=0.0005)
        m.add(ch, t0, 0.07)
    return m.out


# =========================================================================== impacts
@recipe("bullet_impact_*")
def bullet_impact(c, N):
    """Crack, two or three quick pitch-dropping sub thumps, crunchy 300 Hz-3 kHz debris, dying tail."""
    sr, rng = c.sr, c.rng
    out = blast(c, N, crack_gain=1.0, sub=(150.0, 55.0), sub_gain=0.9, sub_tau=0.06, crackle=(90, 400, 3500, 0.5, 0.45),
                debris=0.5, tail_gain=0.4, lead_ok=True)
    m = Mix(c, N)
    for t, g in ((0.07, 0.6), (0.145, 0.4)):
        m.add(sub_drop(sr, 130 * rng.uniform(0.9, 1.1), 50, 0.2, 0.05, k=0.025), 0.05 + t, g)
    nb = sfx.n_of(sr, 0.25)
    m.add(sfx.band_noise(nb, sr, rng, 300, 3000, order=1) * sfx.exp_env(nb, sr, 0.06) * churn(nb, sr, rng, 40, 0.7), 0.05, 0.7)
    return out + fit(m.out, len(out)) * 0.6


@recipe("biodeath")
def biodeath(c, N):
    return blast(c, N, crack_gain=0.9, sub=(100.0, 40.0), sub_gain=1.0, rumble=1.2, churn_rate=14, churn_depth=0.6,
                 crackle=(110, 500, 8000, 2.8, 0.75), surges=((0.9, 0.45, 0.5), (1.15, 0.35, 0.45), (2.4, 0.35, 0.35)),
                 tail_gain=0.8, debris=0.4)


def _splat(sr, rng, g=1.0):
    n = sfx.n_of(sr, 0.09)
    lo, hi = rng.uniform(500, 1200), rng.uniform(2500, 6000)
    sp = sfx.fftfilt(sfx.noise(n, rng), sr, hp=lo, lp=hi, order=1) * sfx.exp_env(n, sr, 0.022, attack=0.0008)
    th = sub_drop(sr, rng.uniform(160, 260), 70, 0.08, 0.03, k=0.02)
    return _sum(sp * g, th * g * 0.5)


@recipe("gore_*")
def gore(c, N):
    """Crunchy hit, sub thump, continuous wet squelch, discrete splats where the original has them, sparse drips."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    out = blast(c, N, crack_gain=1.0, sub=(110.0, 45.0), sub_gain=1.0, rumble=0.7, churn_rate=22, churn_depth=0.6,
                crackle=(120, 600, 7000, 1.6, 0.6), tail_gain=0.6, debris=0.3, bumps=False)
    m = Mix(c, N)
    first = tg.onsets[0] if tg.onsets else 0.0
    for t in [t for t in tg.onsets if t > first + 0.1][:12]:
        m.add(_splat(sr, rng, rng.uniform(0.6, 1.0)), t, 0.7)
    # sparse drips in the tail
    for i in range(6):
        m.add(_splat(sr, rng, 0.25), tg.end_t * rng.uniform(0.3, 0.95), 0.25)
    # wet squelch body
    nq = sfx.n_of(sr, min(1.6, tg.dur))
    sq = sfx.band_noise(nq, sr, rng, 500, 4500, order=2) * churn(nq, sr, rng, 28, 0.85) ** 2 * sfx.decay_env(nq, sr, 0.6, 1.3)
    m.add(sq, 0.15, 0.5)
    return out + fit(m.out, len(out))


@recipe("disintegrate")
def disintegrate(c, N):
    """Sub thump into a long hissing, crackling decay: sparse pops, rising brightness as the low end fades."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    m.add(sub_drop(sr, 100, 40, 1.5, 0.5, k=0.08), 0.0, 1.0)
    m.add(crack_burst(sr, rng, tau=0.02, lo=500, hi=12000, dur=0.2), 0.0, 1.0)
    hs = sfx.band_noise(N, sr, rng, 3000, 14000, order=1) * churn(N, sr, rng, 14, 0.7)
    m.add(hs, 0.0, 0.7)
    dens = np.linspace(120, 60, N)
    m.add(sfx.grain_cloud(N, sr, rng, dens, 0.0025, 2000, 11000, amp_var=0.95), 0.0, 0.9)
    lo = sfx.dark_noise(N, sr, rng, 900, 120, steps=6, hp=30) * sfx.exp_env(N, sr, 1.2)
    m.add(lo, 0.0, 0.7)
    # sparse late pops
    for t in [t for t in tg.onsets][:14]:
        n = sfx.n_of(sr, 0.05)
        m.add(sfx.band_noise(n, sr, rng, 1500, 9000, order=1) * sfx.exp_env(n, sr, 0.01, attack=0.0004), t, 1.0)
    return m.out


@recipe("explosion1_*")
def explosion1(c, N):
    """Clipped crack, huge dark sub roar, irregular debris bumps at the original's onsets, darkening tail."""
    tg = get_tg(c)
    surges = tuple((t - 0.02, rng_d, g) for t, rng_d, g in
                   [(t, 0.22, 0.45) for t in tg.onsets if t > 0.3][:8])
    return blast(c, N, crack_gain=1.1, sub=(85.0, 34.0), sub_gain=1.2, rumble=1.4, churn_rate=9, churn_depth=0.5,
                 crackle=(35, 400, 3500, min(tg.dur, 2.4) * 0.8, 0.45), surges=surges, pops=[t for t in tg.onsets if t > 0.3],
                 pop_gain=0.5, tail_gain=0.9, debris=0.6,
                 bumps=False)


@recipe("nukeexplosion")
def nuke(c, N):
    """Swell to a huge dark blast, lumpy low debris rumble, dense crackle, long fading tail."""
    tg = get_tg(c)
    surges = tuple((t - 0.02, 0.5, 0.35) for t in tg.onsets if t > 1.5)
    pops = [t for t in tg.onsets if t > 4.5]
    return blast(c, N, crack_gain=0.8, sub=(70.0, 28.0), sub_gain=1.3, rumble=1.6, churn_rate=3, churn_depth=0.6,
                 crackle=(25, 300, 3000, 9.0, 0.35), surges=surges, pops=pops, pop_gain=0.5, tail_gain=1.0, debris=0.5, bumps=False)


@recipe("steamarmor_jump_*")
def steam_jump(c, N):
    """Sub thump into a dense churning steam roar: noisy grit, fluttering puffs, fading hiss."""
    tg = get_tg(c)
    surges = tuple((t - 0.02, 0.25, 0.5) for t in tg.onsets if t > 0.2)
    return blast(c, N, crack_gain=0.6, sub=(90.0, 38.0), sub_gain=1.3, rumble=1.5, churn_rate=25, churn_depth=0.6,
                 whoosh=0.45, whoosh_lo=1500, whoosh_hi=9000, crackle=(60, 800, 6000, 0.7, 0.3), surges=surges,
                 tail_gain=0.8, bumps=False)
