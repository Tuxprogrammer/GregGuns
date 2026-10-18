"""Items, machines and blocks: night vision, anti-grav, glider, ammo press, RC machine sounds, metal door."""
from __future__ import annotations

import numpy as np

import sfx
from sndlib import Mix, get_tg, recipe, crack_burst, sub_drop, mech_click, mech_clack, mech_slam
from sndengines import (fit, blast, events, rumble, churn, tone_burst, click_thump, foley_event, rel_env_at, _sum)


# =========================================================================== items
@recipe("nightvision_on")
def nightvision_on(c, N):
    """Bright noisy metallic burst, second hit ~0.1 s later, inharmonic ringing partials (1-5 kHz) with a small
    fall-rise glide, low thunk, shimmering 8-12 kHz tail."""
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    for t, g in ((0.05, 1.0), (0.13, 0.8)):
        m.add(crack_burst(sr, rng, tau=0.012, lo=1500, hi=15000, dur=0.1), t, 1.2 * g)
        m.add(sfx.metal_ring(sfx.n_of(sr, 0.5), sr, rng, rng.uniform(1100, 1500), 0.18, nm=5, spread=0.9), t, 0.25 * g)
    n = sfx.n_of(sr, 0.9)
    fall = 1.0 + 0.04 * np.sin(np.pi * np.linspace(0, 1, n))
    for f in (1250, 2300, 3700, 5100):
        t = sfx.tvec(n, sr)
        m.add(np.sin(sfx.TAU * f * t * fall + rng.uniform(0, 6)) * np.exp(-t / 0.22), 0.05, 0.05)
    m.add(sub_drop(sr, 210, 80, 0.15, 0.04, k=0.02), 0.05, 0.5)
    nt = sfx.n_of(sr, 0.9)
    m.add(sfx.band_noise(nt, sr, rng, 7000, 14000, order=1) * sfx.exp_env(nt, sr, 0.3) * churn(nt, sr, rng, 50, 0.7), 0.1, 0.35)
    return m.out


@recipe("nightvision_off")
def nightvision_off(c, N):
    """Loud broadband noise body (0.2-15 kHz, ~0.14 s) with a short metallic snap, a 9 -> 6 kHz downward streak,
    a noisy tail with low flutter and a bright air shimmer."""
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    nb = sfx.n_of(sr, 0.2)
    body = sfx.band_noise(nb, sr, rng, 200, 15000, order=1) * sfx.exp_env(nb, sr, 0.05, attack=0.012)
    m.add(body, 0.02, 1.0)
    m.add(crack_burst(sr, rng, tau=0.004, lo=1500, hi=14000, dur=0.06), 0.03, 1.0)
    n = sfx.n_of(sr, 0.12)
    streak = sfx.dark_noise(n, sr, rng, 9000, 5000, steps=5, hp=4500) * sfx.exp_env(n, sr, 0.05)
    m.add(streak, 0.04, 0.5)
    nt = sfx.n_of(sr, 0.3)
    m.add(sfx.band_noise(nt, sr, rng, 800, 9000, order=1) * sfx.exp_env(nt, sr, 0.09) * churn(nt, sr, rng, 30, 0.8), 0.14, 0.3)
    m.add(sfx.band_noise(nt, sr, rng, 11000, 19000, order=1) * sfx.exp_env(nt, sr, 0.07), 0.04, 0.3)
    return m.out


@recipe("antigrav_start_*")
def antigrav_start(c, N):
    """Instant clipped noisy burst (hits at 0.05 and 0.23 s), saturated sub boom, a rising-then-falling 1-10 kHz
    electric whoosh, pulsing rumble in the decay."""
    sr, rng = c.sr, c.rng
    out = blast(c, N, crack_gain=0.9, sub=(95.0, 42.0), sub_gain=1.1, rumble=0.9, churn_rate=15, churn_depth=0.6,
                whoosh=1.1, whoosh_lo=1200, whoosh_hi=9000, tail_gain=0.7, debris=0.3, bumps=True)
    m = Mix(c, N)
    n = sfx.n_of(sr, 0.5)
    sw = sfx.dark_noise(n, sr, rng, 1500, 9000, steps=6, hp=900) * sfx.smooth_env(n, sr, 0.18, 0.3)
    m.add(sw, 0.1, 0.6)
    out = out + fit(m.out, len(out))
    return sfx.waveshape(out / (np.max(np.abs(out)) + 1e-9), 2.2)


@recipe("glider_loop_stereo")
def glider_loop(c, N):
    """Wind: a bass-and-mid rumble with low-frequency buffeting, a gentle mid-clip swell, partially correlated
    channels (shared + independent noise)."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    c.looping = True
    shared = sfx.band_noise(N, sr, rng, 60, 2200, order=2)
    chans = []
    swell = 1.0 + 0.25 * np.sin(np.pi * np.linspace(0, 1, N))
    for k in range(2):
        own = sfx.band_noise(N, sr, rng, 60, 2200, order=2)
        base = 0.55 * shared + 0.85 * own
        base = base * (0.7 + 0.3 * churn(N, sr, rng, 0.8, 1.0)) * swell
        buf = sfx.band_noise(N, sr, rng, 25, 300, order=2) * churn(N, sr, rng, 3, 0.8)
        y = base + 0.35 * buf
        y = sfx.conform_bands(y, sr, tg.bands, rng=rng)
        chans.append(y)
    return np.stack(chans)


# =========================================================================== machines
def _press(c, N, slam_t, rings=(2100.0, 2900.0, 4400.0), churn_gain=1.0, grind=0.5, clunks=0.6, swell=0.0):
    """Machine press: a heavy slam (crack + sub + body + metal rings), a churning grind that decays,
    and small unpitched clunks at the original's later onsets."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    if swell:
        nb = sfx.n_of(sr, swell + 0.4)
        m.add(sfx.band_noise(nb, sr, rng, 150, 1200, order=2) * sfx.smooth_env(nb, sr, swell, 0.3), 0.0, 0.7)
    m.add(rumble(c, N, lo=30, hi=700, rate=18, depth=0.55, grit=0.2, hiss=0.05, hiss_lo=2000, hiss_hi=8000), slam_t, 0.8)
    s = mech_slam(sr, rng, dur=0.7, body_f=(150, 700), tau=0.07, ring_f=rings, ring_tau=0.06, bass=0.8)
    m.add(s, slam_t, 1.0)
    m.add(sub_drop(sr, 120, 42, 0.4, 0.12, k=0.04), slam_t, 0.6)
    n = N
    base = sfx.dark_noise(n, sr, rng, 3500, 400, steps=7, hp=40)
    base = base * churn(n, sr, rng, 22, 0.6)
    m.add(base * sfx.decay_env(n, sr, 0.6, 1.8, attack=0.01), slam_t, churn_gain * 0.6)
    gr = sfx.band_noise(n, sr, rng, 500, 3000, order=1) * churn(n, sr, rng, 45, 0.8) ** 2
    m.add(gr * sfx.decay_env(n, sr, 0.5, 1.4), slam_t + 0.05, grind * 0.4)
    for t in [t for t in tg.onsets if t > slam_t + 0.3][:6]:
        m.add(foley_event(sr, rng, "thunk", 1.0, 0.8), t, clunks * 0.5)
    return m.out


@recipe("ammopress_work1_*")
def ammopress_work1(c, N):
    return _press(c, N, 0.25)


@recipe("ammopress_work2_*")
def ammopress_work2(c, N):
    tg = get_tg(c)
    return _press(c, N, max(tg.peak_t * 0.8, 0.12), rings=(2400.0, 3600.0), churn_gain=1.2, grind=0.9, swell=0.15)


@recipe("rc_beep")
def rc_beep(c, N):
    """A short 540 Hz beep with a thump and a 1-2.5 kHz burst in the first ~30 ms, a slow dip and swell on the tone."""
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    d = 0.11
    tn = tone_burst(sr, rng, 540, d, ratios=(1.0, 2.0), gains=(1.0, 0.12), attack=0.001, release=0.01)
    t = sfx.tvec(len(tn), sr)
    dip = 1.0 - 0.65 * np.exp(-((t - 0.07) / 0.018) ** 2)
    m.add(tn * dip, 0.0, 0.9)
    m.add(click_thump(sr, rng, 0.6, 0.8, bright=0.5, thump_f=(300, 120), thump_dur=0.03), 0.0, 1.0)
    nb = sfx.n_of(sr, 0.03)
    m.add(sfx.band_noise(nb, sr, rng, 1000, 2500, order=1) * sfx.exp_env(nb, sr, 0.008), 0.0, 0.6)
    return m.out


@recipe("rc_warning")
def rc_warning(c, N):
    """One continuous dense, bass-heavy buzz: a rich harmonic comb (about 165 Hz drifting up a few percent) over a
    sub rumble, with noise bursts and a fade.  Not gated beeps."""
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    f = np.linspace(160.0, 178.0, N) * (1 + 0.004 * (churn(N, sr, rng, 3, 1.0) - 0.5))
    st = sfx.harmonic_stack(f, N, sr, nh=48, rolloff=0.75, rng=rng)
    m.add(sfx.fftfilt(st, sr, hp=70, lp=9000, order=1), 0.0, 1.0)
    m.add(sfx.band_noise(N, sr, rng, 25, 160, order=2) * churn(N, sr, rng, 12, 0.5), 0.0, 1.2)
    for t, d in ((0.2, 0.1), (0.3, 0.1), (0.56, 0.1), (0.66, 0.1), (0.76, 0.1)):
        n = sfx.n_of(sr, d)
        m.add(sfx.band_noise(n, sr, rng, 500, 7000, order=1) * sfx.smooth_env(n, sr, 0.01, 0.05), t, 0.35)
    return m.out


@recipe("rc_heatray_work")
def rc_heatray(c, N):
    """Dark grainy low rumble (almost nothing above 1 kHz), slow swell, irregular bumps, faint crackle."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    m.add(rumble(c, N, lo=28, hi=600, rate=9, depth=0.6, grit=0.2, glide=(500, 250), crackle=0.08), 0.0, 1.0)
    for t in [t for t in tg.onsets if t > 0.8][:6]:
        n = sfx.n_of(sr, 0.14)
        m.add(sfx.band_noise(n, sr, rng, 35, 320, order=2) * sfx.smooth_env(n, sr, 0.03, 0.08), t, 0.4)
    return m.out


# =========================================================================== blocks
@recipe("metaldoor_open_*")
def metaldoor(c, N):
    """Latch click, small knock, heavy hinge/door-body hit with resonant ringing, scrape/creak bridging the
    events, bright inharmonic rattle tail with micro-impacts."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    ons = tg.onsets or [0.05, 0.13, 0.24]
    t_latch = ons[0]
    t_knock = ons[1] if len(ons) > 2 else 0.13
    t_hit = tg.peak_t if tg.peak_t > 0.1 else ons[-1]
    # latch click: quiet, noisy, not pitched
    m.add(mech_click(sr, rng, bright=1.2, ring_amp=0.15), t_latch, 0.55)
    # small metallic knock 3-4 kHz inside the rattle
    m.add(mech_clack(sr, rng, f0=rng.uniform(3000, 3800), tau=0.03, bright=1.2), t_knock, 0.5)
    # door body / hinge hit: crack + 150-800 Hz resonant body + ringing
    n = sfx.n_of(sr, 0.6)
    tt = sfx.tvec(n, sr)
    body = sfx.fftfilt(sfx.noise(n, rng), sr, hp=120, lp=900, order=2) * sfx.exp_env(n, sr, 0.09, attack=0.0008)
    ring = sfx.metal_ring(n, sr, rng, rng.uniform(260, 420), 0.18, nm=5)
    m.add(body, t_hit, 1.0)
    m.add(ring, t_hit, 0.45)
    m.add(crack_burst(sr, rng, tau=0.006, lo=800, hi=11000, dur=0.1), t_hit, 0.9)
    m.add(sub_drop(sr, 120, 55, 0.15, 0.05, k=0.02), t_hit, 0.35)
    # scrape / creak bridging events (sliding band, irregular)
    ns = sfx.n_of(sr, 0.45)
    cr = sfx.dark_noise(ns, sr, rng, 3200, 1200, steps=5, hp=700) * churn(ns, sr, rng, 28, 0.9) ** 2 * sfx.smooth_env(ns, sr, 0.04, 0.2)
    m.add(cr, t_knock + 0.02, 0.35)
    # rattle tail: dense micro-impacts thinning out
    nr = sfx.n_of(sr, 0.5)
    m.add(sfx.grain_cloud(nr, sr, rng, np.linspace(160, 20, nr), 0.003, 1500, 7000, env=sfx.exp_env(nr, sr, 0.18), amp_var=0.9), t_hit + 0.08, 0.5)
    return m.out
