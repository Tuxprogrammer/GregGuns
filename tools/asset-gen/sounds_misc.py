"""Items, machines and blocks: night vision, anti-grav, glider, ammo press, RC machine sounds, metal door."""
from __future__ import annotations

import json
import os
import zlib

import numpy as np

import sfx
from sndlib import Mix, get_tg, recipe, crack_burst, sub_drop, mech_click, mech_clack, mech_slam
from sndengines import (fit, blast, events, rumble, churn, tone_burst, click_thump, foley_event, rel_env_at, _sum)


# --------------------------------------------------------------------------- private helpers (round 2)
def _tune():
    """Tuning knobs for the round-5 embedding search: a JSON dict in the GG_P environment variable overrides recipe
    defaults (unset in normal runs, so output is deterministic)."""
    return json.loads(os.environ.get("GG_P", "{}"))


# Round 7: per-sound knobs found by the gate-aware embedding search (merged under GG_P, which still overrides while searching).
_TUNED7 = {}


def _tune7(c):
    """Round-7 baked table for this sound, overridden by GG_P; a ``seed`` entry re-draws the noise stream."""
    P = {**_TUNED7.get(c.name, {}), **_tune()}
    if "seed" in P:
        c.rng = np.random.default_rng([int(P["seed"]), zlib.crc32(c.path.encode())])
    return P


def _reseed(c, seed):
    """Replace the recipe RNG with a seed picked by embedding distance (GG_P ``seed`` overrides while searching)."""
    k = _tune().get("seed", seed)
    c.rng = np.random.default_rng([int(k), zlib.crc32(c.path.encode())])
    return c.rng


def _gauss(n, sr, t0, width):
    """Bell envelope centered at t0 seconds (sample 0 is time 0)."""
    t = sfx.tvec(n, sr)
    return np.exp(-0.5 * ((t - t0) / width) ** 2)


def _arch(c, dur, bands):
    """Bright noise sweep drawn as a chain of band-passed noise bells: (lo, hi, center_s, width_s, gain) each.
    Reads as a band-pass whose center rises and falls (an arch) without a time-varying filter."""
    sr, rng = c.sr, c.rng
    n = sfx.n_of(sr, dur)
    out = np.zeros(n)
    for lo, hi, t0, w, g in bands:
        if lo >= sr * 0.44:
            continue
        out += g * sfx.band_noise(n, sr, rng, lo, min(hi, sr * 0.44), order=2) * _gauss(n, sr, t0, w)
    return out


def _gliding_partials(c, freqs, dur, tau, glide_pts, gains):
    """Thin inharmonic partials that share one frequency contour (multipliers at times) and ring out."""
    sr, rng = c.sr, c.rng
    n = sfx.n_of(sr, dur)
    t = sfx.tvec(n, sr)
    mult = np.interp(t, [p[0] for p in glide_pts], [p[1] for p in glide_pts])
    out = np.zeros(n)
    for f, g in zip(freqs, gains):
        if f * 1.05 > sr * 0.45:
            continue
        out += g * np.sin(sfx.osc_phase(f * mult, sr) + rng.uniform(0, sfx.TAU)) * np.exp(-t / tau)
    return out



def _clank(sr, rng, gain=1.0, lo=600.0, hi=3500.0, nm=5, tau=(0.004, 0.009), crack_hi=9000.0, crack=1.0):
    """Metal click: a 1-3 ms broadband transient plus 3-6 damped inharmonic modes (mostly 600-3500 Hz, each
    decaying in 15-28 ms; modes above 5 kHz in under 10 ms), a fresh mode set every call.  Too short for the
    tonality gate to see a line, and far from white noise."""
    n = sfx.n_of(sr, 0.09)
    t = sfx.tvec(n, sr)
    out = np.zeros(n)
    cr = crack_burst(sr, rng, tau=0.0018, lo=1200, hi=min(crack_hi, sr * 0.44), dur=0.03)
    out[:len(cr)] += crack * cr
    for _ in range(nm):
        f = float(np.exp(rng.uniform(np.log(lo), np.log(hi))))
        if f > sr * 0.44:
            continue
        tt = rng.uniform(0.003, 0.007) if f > 5000 else rng.uniform(*tau)
        fm = f * (1 + rng.uniform(-0.04, 0.04) * np.exp(-t / 0.01))
        md = np.sin(sfx.osc_phase(fm, sr) + rng.uniform(0, sfx.TAU))
        md = 0.4 * md + 0.6 * sfx.band_noise(n, sr, rng, f * 0.85, f * 1.18, order=1) * 2.5
        out += rng.uniform(0.35, 0.8) * md * np.exp(-t / tt)
    return gain * out


def _rattle(sr, rng, dur, freqs, rate=30.0, width=0.07):
    """Rattle from a few narrow resonant noise bands, each chopped irregularly (peaky spectrum, not white noise)."""
    n = sfx.n_of(sr, dur)
    out = np.zeros(n)
    for f in freqs:
        out += sfx.band_noise(n, sr, rng, f * (1 - width), f * (1 + width), order=2) * churn(n, sr, rng, rate, 1.0) ** 2
    return out / max(len(freqs), 1) ** 0.5


def _modal_noise(sr, rng, freqs, tau, dur=None, width=0.07):
    """Damped mid-band body resonances made from narrow noise bands (no sustained sine lines)."""
    dur = dur or tau * 4
    n = sfx.n_of(sr, dur)
    out = np.zeros(n)
    for f in freqs:
        out += sfx.band_noise(n, sr, rng, f * (1 - width), f * (1 + width), order=2) * sfx.exp_env(n, sr, tau * rng.uniform(0.8, 1.2), attack=0.001)
    return out / max(len(freqs), 1) ** 0.5


# =========================================================================== items
LADDER_GAIN = 0.0072


@recipe("nightvision_on")
def nightvision_on(c, N):
    """Noisy broadband double hit (0.03-0.12 s, soft-clipped, with 30-60 Hz flutter in the highs), thin ringing
    partials from A's measured line times (short damped modes, a little downward glide), a narrow 11.5 kHz noise
    shimmer line and sparkle, a quiet fading haze.  High-passed at 150 Hz."""
    P7 = _tune7(c)
    sr, rng = c.sr, c.rng
    P = {**dict(hz=0.3, sh=0.3, md=2.0, lg=3.0, ltau=0.5, fl=0.2, jit=0.2, b1hi=7000), **P7}   # round 5: sustained quiet ladder, less haze
    m = Mix(c, N)
    n1 = sfx.n_of(sr, 0.25)
    b1 = sfx.band_noise(n1, sr, rng, P.get('b1lo', 1500), P.get('b1hi', 15000), order=1) * sfx.exp_env(n1, sr, P.get('t1', 0.07), attack=0.0004)
    b1 = b1 + P.get('b1l', 0.3) * sfx.band_noise(n1, sr, rng, 250, 1500, order=1) * sfx.exp_env(n1, sr, 0.05, attack=0.0004)
    if P.get("tb"):
        ntb = sfx.n_of(sr, 0.4)
        m.add(sfx.band_noise(ntb, sr, rng, P.get("tblo", 800), P.get("tbhi", 6000), order=1) * sfx.exp_env(ntb, sr, P.get("tbtau", 0.12), attack=0.01), 0.1, P["tb"])
    m.add(sfx.waveshape(b1 / (np.max(np.abs(b1)) + 1e-9), 2.0) * churn(n1, sr, rng, 45, 0.4), 0.0, 1.1 * P.get('h1', 1.0))
    n2 = sfx.n_of(sr, 0.07)
    b2 = sfx.band_noise(n2, sr, rng, 1200, 14000, order=1) * sfx.exp_env(n2, sr, 0.02, attack=0.0003)
    m.add(sfx.waveshape(b2 / (np.max(np.abs(b2)) + 1e-9), 2.5) * churn(n2, sr, rng, 45, 0.5), 0.10, 0.9)
    # ringing layer: short damped modes at A's measured line times (about 2.2 / 3.4 / 4.6 kHz family), gliding down
    for f, t0, g in ((2375, 0.16, 0.9), (3375, 0.22, 1.0), (4900, 0.22, 0.6), (2425, 0.28, 0.8), (5375, 0.28, 0.5),
                     (3500, 0.32, 0.9), (1800, 0.38, 0.6), (3575, 0.38, 0.8), (6075, 0.38, 0.4), (3650, 0.44, 0.7)):
        nm_ = sfx.n_of(sr, 0.12)
        tm = sfx.tvec(nm_, sr)
        fm = f * (1 + 0.03 * np.exp(-tm / 0.03) * rng.choice([-1, 1]))
        mode = 0.5 * np.sin(sfx.osc_phase(fm, sr) + rng.uniform(0, 6)) * np.exp(-tm / 0.008)
        mode = mode + 0.6 * sfx.band_noise(nm_, sr, rng, f * 0.8, f * 1.25, order=1) * np.exp(-tm / 0.01)
        m.add(mode, t0, 0.03 * g * P.get('md', 1.0))
    # ringing ladder: a slowly rising ~2 kHz fundamental with harmonics 2-4 (A's measured path, 1.6 -> 2.3 kHz over
    # 0.16-1.4 s); not a clean sine: a little frequency jitter and a noise skirt on every partial
    nl = sfx.n_of(sr, 1.3)
    tl = sfx.tvec(nl, sr)
    f0 = np.interp(tl, [0, 0.12, 0.22, 0.34, 0.42, 0.5, 0.58, 0.66, 0.72, 0.8, 0.9, 0.96, 1.22, 1.3],
                   [1625, 1725, 1800, 1875, 1925, 1975, 2025, 2075, 2100, 2150, 2200, 2225, 2325, 2340])
    if P.get('fl') is not None:
        f0 = P.get('f0c', 1918.0) + (f0 - P.get('f0c', 1918.0)) * P['fl']
    f0 = f0 * (1 + 0.003 * P.get('jit', 1.0) * sfx.band_noise(nl, sr, rng, 1, 40, order=1) * 3)
    lad = np.zeros(nl)
    for k, g in ((1, 1.0), (2, 0.5), (3, 0.4), (4, 0.15)):
        if f0.max() * k > sr * 0.44:
            continue
        lad += g * np.sin(k * sfx.osc_phase(f0, sr) + rng.uniform(0, 6))
    ladder_env = sfx.smooth_env(nl, sr, 0.08, 0.25) * np.exp(-tl / P.get('ltau', 0.34))
    m.add(lad * ladder_env, 0.16, LADDER_GAIN * P.get('lg', 1.0))
    # shimmer: narrow 11.5 kHz noise band (A has a long line there) plus a 15-16 kHz sparkle
    nsh = sfx.n_of(sr, 1.0)
    if sr * 0.44 > 12000:
        m.add(sfx.band_noise(nsh, sr, rng, 11350, 11750, order=2) * sfx.exp_env(nsh, sr, 0.35) * churn(nsh, sr, rng, 5, 0.5), 0.18, 0.5 * P.get('sh', 1.0))
    if sr * 0.44 > 15900:
        ns = sfx.n_of(sr, 0.3)
        m.add(sfx.band_noise(ns, sr, rng, 15300, 15900, order=2) * sfx.exp_env(ns, sr, 0.08), 0.26, 0.25)
    # quiet haze (cut 10-15 dB after 0.15 s): 7-14 kHz, decaying continuously
    nt = sfx.n_of(sr, 1.4)
    m.add(sfx.band_noise(nt, sr, rng, 3000, 12000, order=1) * sfx.exp_env(nt, sr, 0.3) * churn(nt, sr, rng, 50, 0.7), 0.0, 0.1 * P.get('hz', 1.0))
    return sfx.fftfilt(m.out, sr, hp=150, order=2)


@recipe("nightvision_off")
def nightvision_off(c, N):
    """Sharp metallic snap at 0 s (4 ms attack, damped modes 2-12 kHz), noisy body high-passed at 200 Hz with a
    -20 dB drop in ~70 ms, a faint 9 -> 6 kHz sweep, hissy tail above 4 kHz that stays alive until the end, three
    soft damped blips plus a noisy falling whine on A's measured path, a little metallic content in the mix."""
    P7 = _tune7(c)
    sr, rng = c.sr, c.rng
    P = {**dict(h2=0.0, hz=0.02, bl=0.0, wh=0.0, sp=0.0, pq=0.25, ring=0.3, rtau=0.06, rf=1033.0), **P7}   # round 5: quieter second hiss and blips, stronger whine
    c.conform_env = False   # clean-room: the coarse tenth-second contour pushed energy late; the recipe shapes it
    get_tg(c).trail = 0.0   # keep the hiss alive to the last frame (A's last tenth is -56 dB, not silent)
    m = Mix(c, N)
    m.add(_clank(sr, rng, 1.6, lo=1500, hi=9000, nm=6, tau=(0.008, 0.02), crack_hi=14000), 0.0, 1.0)
    npre = sfx.n_of(sr, 0.03)
    m.add(sfx.band_noise(npre, sr, rng, 2000, 12000, order=1), 0.0, 1.5)
    if P.get("ig"):
        nig = sfx.n_of(sr, 0.012)
        m.add(sfx.band_noise(nig, sr, rng, 1500, 12000, order=1) * sfx.exp_env(nig, sr, 0.004, attack=0.0002), 0.0, P["ig"])
    if P.get("mid"):
        nmd = sfx.n_of(sr, 0.12)
        m.add(sfx.band_noise(nmd, sr, rng, 1500, 9000, order=1) * sfx.smooth_env(nmd, sr, 0.02, 0.05), 0.2, P["mid"])
    nb = sfx.n_of(sr, 0.25)
    body = sfx.band_noise(nb, sr, rng, 250, 15000, order=2) * sfx.exp_env(nb, sr, 0.03, attack=0.003)
    m.add(body, 0.0, 0.9 * P.get('bd', 1.0))
    n = sfx.n_of(sr, 0.12)
    streak = sfx.dark_noise(n, sr, rng, 9000, 6000, steps=5, hp=4500) * sfx.exp_env(n, sr, 0.05)
    m.add(streak, 0.02, 0.5)
    # hissy tail above 4 kHz: -35 dB at 0.14 s falling to -50 dB near 0.28 s, still alive at the end
    nh = sfx.n_of(sr, 0.32)
    m.add(sfx.band_noise(nh, sr, rng, 4000, min(16000, sr * 0.44), order=1) * sfx.exp_env(nh, sr, 0.3), 0.10, 0.12 * P.get('hz', 1.0))
    nh2 = sfx.n_of(sr, 0.1)
    m.add(sfx.band_noise(nh2, sr, rng, 4000, 12000, order=1) * sfx.smooth_env(nh2, sr, 0.02, 0.04), 0.24, 0.35 * P.get('h2', 1.0))
    # pitched ring: a damped ~1 kHz partial pair under the snap (the sound reads as pitched)
    nr = sfx.n_of(sr, 0.5)
    tr = sfx.tvec(nr, sr)
    ring = np.sin(sfx.TAU * P['rf'] * tr) + 0.3 * np.sin(sfx.TAU * 2 * P['rf'] * tr + 1.0)
    m.add(ring * sfx.smooth_env(nr, sr, 0.004, 0.45) * np.exp(-tr / P['rtau']), 0.0, 0.08 * P['ring'])
    # pitched blip chain at 0.27-0.42 s: steady tones 970 / 1033 / 1281 / 1184 Hz that stay put long enough to read as pitched
    nq = sfx.n_of(sr, 0.16)
    tq = sfx.tvec(nq, sr)
    fq = np.interp(tq, [0, 0.045, 0.05, 0.085, 0.09, 0.125, 0.13, 0.16], [970, 970, 1033, 1033, 1281, 1281, 1184, 1184])
    aq = np.interp(tq, [0, 0.01, 0.05, 0.09, 0.12, 0.16], [0.15, 0.6, 1.0, 0.45, 0.3, 0.0])
    m.add(np.sin(sfx.osc_phase(fq, sr)) * aq, 0.27, 0.12 * P.get('pq', 1.0))
    # soft blips at 0.29 / 0.33 / 0.37 s (damped modes near 4-5 kHz, gliding down, each 3 dB quieter)
    for k, (f, tb) in enumerate(((4850, 0.29), (4300, 0.33), (3650, 0.37))):
        nm_ = sfx.n_of(sr, 0.05)
        tm = sfx.tvec(nm_, sr)
        fm = f * (1 - 0.08 * tm / 0.05)
        mode = np.sin(sfx.osc_phase(fm, sr)) * np.exp(-tm / 0.012) + 0.3 * sfx.band_noise(nm_, sr, rng, f * 0.8, f * 1.2, order=1) * np.exp(-tm / 0.012)
        m.add(mode, tb, 0.02 * P.get('bl', 1.0) * 10 ** (-3.0 * k / 20))
    # falling whine, noisy, on A's measured path
    nw = sfx.n_of(sr, 0.18)
    tw = sfx.tvec(nw, sr)
    fw = np.interp(tw, [0, 0.08, 0.10, 0.12, 0.18], [4850, 3900, 3650, 3375, 3200])
    fw = fw * (1 + 0.012 * sfx.noise(nw, rng))
    whine = np.sin(sfx.osc_phase(fw, sr)) * 0.5 + 0.8 * sfx.band_noise(nw, sr, rng, 2800, 5200, order=1)
    m.add(whine * sfx.smooth_env(nw, sr, 0.01, 0.05), 0.24, 0.022 * P.get('wh', 1.0))
    if sr * 0.44 > 17500:
        ns = sfx.n_of(sr, 0.15)
        m.add(sfx.band_noise(ns, sr, rng, 15500, 19500, order=1) * sfx.smooth_env(ns, sr, 0.04, 0.05), 0.29, 0.015 * P.get('sp', 1.0))
    return sfx.fftfilt(m.out, sr, hp=200, order=2)



@recipe("antigrav_start_*")
def antigrav_start(c, N):
    """Hard clipped power-up: sub thumps on both onsets (0.05 / 0.23 s; one onset on the b variant), a saturated
    sub boom, an arched noise sweep (rises to ~6-9 kHz near 0.45 s, falls back), pulsing rumble in the decay,
    a faint 2-3 kHz air line."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    two = len(tg.onsets) >= 2
    out = blast(c, N, crack_gain=1.0, sub=(95.0, 42.0), sub_gain=1.1, rumble=0.9, churn_rate=15, churn_depth=0.6,
                whoosh=0.0, whoosh_lo=1200, whoosh_hi=9000, tail_gain=0.8, debris=0.2, bumps=two,
                swell=(0.1 if c.path.endswith('_b.ogg') else None))
    m = Mix(c, N)
    t_a = tg.onsets[0] if tg.onsets else 0.05
    m.add(sub_drop(sr, 80, 55, 0.2, 0.07, k=0.03, attack=0.002), t_a, 0.8)
    nbs = sfx.n_of(sr, 0.25)
    m.add(sfx.band_noise(nbs, sr, rng, 80, 200, order=2) * sfx.exp_env(nbs, sr, 0.1, attack=0.002), t_a, 1.0)
    m.add(crack_burst(sr, rng, tau=0.012, lo=300, hi=7000, dur=0.06), t_a, 1.3)
    if two:
        m.add(sub_drop(sr, 70, 46, 0.2, 0.07, k=0.03, attack=0.002), tg.onsets[1], 0.9)
        m.add(crack_burst(sr, rng, tau=0.01, lo=400, hi=8000, dur=0.05), tg.onsets[1], 0.6)
    if c.path.endswith("_b.ogg"):
        # 3 -> 9 kHz over 0.15-0.45 s, back to 4 kHz by 0.9 s, bell envelope
        arch = [(2500, 4000, 0.15, 0.10, 0.9), (4500, 7000, 0.30, 0.10, 1.0), (6500, 10000, 0.45, 0.12, 1.0),
                (3000, 6000, 0.65, 0.15, 0.6), (3000, 4500, 0.90, 0.15, 0.3)]
    else:
        # center 1.5 -> 6 kHz by 0.45 s, back to 1 kHz by 0.7 s
        arch = [(1000, 2200, 0.05, 0.07, 0.8), (2000, 4000, 0.22, 0.07, 1.0), (4000, 8000, 0.45, 0.07, 1.0),
                (1500, 3500, 0.60, 0.07, 0.7), (700, 1500, 0.75, 0.09, 0.5)]
    m.add(_arch(c, 1.2, arch), 0.0, 1.5 * (0.7 if c.path.endswith('_a.ogg') else 1.0) * _tune().get('ag', 1.0))
    if not c.path.endswith('_b.ogg'):
        nfl = sfx.n_of(sr, 1.0)
        m.add(sfx.band_noise(nfl, sr, rng, 200, 3000, order=1) * sfx.exp_env(nfl, sr, 0.3, attack=0.002) * churn(nfl, sr, rng, 15, 0.5), 0.0, 0.9)
    P = {**(dict(fl=1.0, ag=0.7) if c.path.endswith('_a.ogg') else {}), **_tune()}   # a only: b is keep
    if P.get("fl"):
        nfl2 = sfx.n_of(sr, 1.2)
        # a broad 200 Hz-8 kHz bed: A is noisier than the sub boom plus arch alone (flatness gate)
        m.add(sfx.band_noise(nfl2, sr, rng, 200, 8000, order=1) * sfx.exp_env(nfl2, sr, 0.4, attack=0.002), 0.05, P["fl"])
    # thin air line, about -45 dB
    na = sfx.n_of(sr, 0.7)
    air = sfx.band_noise(na, sr, rng, 2000, 2500, order=2) * sfx.smooth_env(na, sr, 0.1, 0.3)
    m.add(air, 0.6, 0.02)
    if c.path.endswith("_b.ogg"):
        # clean-room: the original's decay reaches -60 dB only near 1.6 s; a dark noise tail carries it past 1.5 s
        get_tg(c).trail = 0.0
        ntl = sfx.n_of(sr, 1.6)
        m.add(sfx.band_noise(ntl, sr, rng, 120, 2500, order=1) * np.exp(-sfx.tvec(ntl, sr) / 0.42), 0.35, _tune().get('tl', 0.05))
    out = out + fit(m.out, len(out))
    return sfx.waveshape(out / (np.max(np.abs(out)) + 1e-9), 7.0)


@recipe("glider_loop_stereo")
def glider_loop(c, N):
    """Wind: a slow swell (quiet dark edges, brighter middle) with the filter following the swell, low-frequency
    buffeting on the bass, a steep ~2 kHz top, a faint air band only in the middle, a few faint flecks near 8 s,
    and partially correlated channels (about 0.43)."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    c.looping = True
    dur = N / sr
    u = np.clip(np.arange(N) / c.n, 0.0, 1.0)   # N includes 1.5 s of loop crossfade margin; the file is c.n long
    f_pts = np.array([0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0])
    db = np.interp(u, f_pts, np.array([-24, -30, -25, -25, -22, -21, -21, -26, -24, -29, -29], dtype=float))
    env = 10 ** (db / 20)
    env = env / env.max()
    bright = np.clip((db + 29.0) / 8.0, 0.0, 1.0)

    def layer():
        dark = sfx.band_noise(N, sr, rng, 60, 700, order=3)
        mid = sfx.band_noise(N, sr, rng, 60, 750, order=3)
        air = sfx.band_noise(N, sr, rng, 1000, 4000, order=1)
        low = sfx.band_noise(N, sr, rng, 25, 450, order=2) * churn(N, sr, rng, 10, 0.12)
        y = dark * (1 - bright) + mid * bright + 0.06 * air * bright ** 2 + 0.9 * low
        return y * (0.97 + 0.03 * churn(N, sr, rng, 0.8, 1.0))

    shared = layer()
    chans = []
    for k in range(2):
        y = 0.75 * shared + 0.85 * layer()
        y = sfx.conform_bands(y, sr, tg.bands, rng=rng)
        # steady wind: divide by the local RMS (60 ms) so the level follows the swell instead of wandering
        w = np.ones(sfx.n_of(sr, 0.06)) / sfx.n_of(sr, 0.06)
        loc = np.sqrt(np.convolve(y * y, w, mode="same") + 1e-12)
        y = y / loc * 0.15
        y = sfx.fftfilt(y, sr, lp=9000, order=2)   # trim the top (A is low-passed near 1.3 kHz)
        y = y + 0.002 * np.std(y) * sfx.band_noise(N, sr, rng, 5000, 12000, order=1)
        chans.append(y * env)
    out = np.stack(chans)
    # faint flecks near 8 s (same in both channels)
    amp = 0.03 * np.max(np.abs(out))
    for tf in (7.6, 8.0, 8.35, 8.7):
        if tf < dur - 0.2:
            fl = crack_burst(sr, rng, tau=0.003, lo=3500, hi=9000, dur=0.02)
            sfx.place(out[0], fl, tf, sr, amp)
            sfx.place(out[1], fl, tf, sr, amp)
    return out


# =========================================================================== machines
def _clunk(sr, rng, gain=1.0):
    """Small machine clunk: damped 200-800 Hz body modes, an 80 Hz thump and a short 1-3 kHz clank (about 40 ms)."""
    n = sfx.n_of(sr, 0.06)
    t = sfx.tvec(n, sr)
    body = _modal_noise(sr, rng, [rng.uniform(220, 330), rng.uniform(380, 520), rng.uniform(600, 800)], 0.014, dur=0.06)
    body = np.pad(body, (0, max(0, n - len(body))))[:n]
    thump = np.sin(sfx.TAU * 80 * t) * np.exp(-t / 0.02)
    cl = _clank(sr, rng, 0.2, lo=900, hi=3000, nm=2, tau=(0.004, 0.008))
    return gain * (body + 0.7 * thump + np.pad(cl, (0, max(0, n - len(cl))))[:n])


def _tick(sr, rng, gain=1.0):
    """2-4 ms servo tick: a short broadband tick with a couple of damped modes."""
    return _clank(sr, rng, 0.3 * gain, lo=1200, hi=3000, nm=1, tau=(0.004, 0.007), crack=1.5)


def _press(c, N, slam_t, rings=None, churn_gain=1.0, grind=0.5, clunks=0.6, swell=0.0, body_f=(110, 360),
           sub_gain=0.25, crack=1.2, soft=False, pre_tone=False, bed_hi=700, tick_gain=0.3, late=None,
           bed=(4000, 500), decay=(0.45, 1.2), pre_db=(-35, -15), impact=1.0, bed_from_zero=False, flat=0.0,
           lg_band=(90, 260), flat_decay=(0.5, 1.5), lb_gain=0.5, low_scale=1.0):
    """Machine press: a heavy slam (crack + damped metal modes + sub + body), a low gear-grind bed that outlasts a
    mid churn (so the tail darkens), servo ticks and small clunks at the original's later onsets.
    ``soft`` lowpasses the slam and ramps the body in; ``pre_tone`` adds a 450 Hz motor hum before the slam."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    if flat and not c.path.endswith('work1_c.ogg'):
        c.band_strength = 0.5
    m = Mix(c, N)
    lead = slam_t
    if pre_tone:
        # motor hum (noisy 450 Hz band, not a sine) rising over the lead-in
        n = sfx.n_of(sr, lead + 0.05)
        t = sfx.tvec(n, sr)
        ramp = 10 ** ((pre_db[0] + (pre_db[1] - pre_db[0]) * np.clip(t / max(lead, 1e-3), 0, 1)) / 20)
        late_ = t > lead
        ramp[late_] *= np.exp(-(t[late_] - lead) / 0.03)
        hum = sfx.band_noise(n, sr, rng, 380, 520, order=2) * 3.0 + 0.4 * sfx.band_noise(n, sr, rng, 800, 1000, order=2) * 3.0
        motor = (sfx.band_noise(n, sr, rng, 80, 250, order=2) * 1.2
                 + sfx.band_noise(n, sr, rng, 150, 900, order=2) * churn(n, sr, rng, 30, 0.7) * 0.5)
        m.add((hum * 0.5 + motor) * ramp, 0.0, 0.5)
    elif tg.env10 and tg.env10[0] > -48:
        n = sfx.n_of(sr, lead + 0.05)
        pre = sfx.band_noise(n, sr, rng, 40, 150, order=6 if flat else 3) * churn(n, sr, rng, 10, 0.6)
        pre = pre + 0.4 * sfx.band_noise(n, sr, rng, 55, 70, order=2) * 4
        m.add(pre * (0.35 + 0.65 * np.clip(sfx.tvec(n, sr) / max(slam_t, 1e-3), 0, 1)), 0.0, 0.6 * low_scale)
    if swell:
        nb = sfx.n_of(sr, swell + 0.4)
        m.add(sfx.band_noise(nb, sr, rng, 150, 1200, order=2) * sfx.smooth_env(nb, sr, swell, 0.3), 0.0, 0.7)
    rm = rumble(c, N, lo=30, hi=bed_hi, rate=18, depth=0.55, grit=0.2, hiss=0.0)
    if flat:
        rm = sfx.fftfilt(rm, sr, lp=bed_hi, order=6)
    if soft:
        t0 = max(0.0, slam_t - 0.15)
        k = N - sfx.n_of(sr, t0)
        m.add(rm[:k] * np.minimum(sfx.tvec(k, sr) / 0.15, 1.0), t0, 0.8)
    else:
        m.add(rm, slam_t, 0.8 * low_scale)
    s = mech_slam(sr, rng, dur=0.7, body_f=body_f, tau=0.07, ring_f=(), ring_tau=0.06, bass=0.5)
    if soft:
        s = sfx.fftfilt(s, sr, lp=5500, order=2)
    m.add(s, slam_t, 1.0)
    # metal impact: damped mid-band modes (600-3500 Hz) on a broadband transient
    if soft:
        m.add(_clank(sr, rng, 0.35 * impact, lo=500, hi=3000, nm=4, crack_hi=4000, crack=0.5), slam_t, 1.0)
    else:
        m.add(_clank(sr, rng, 1.0 * impact, lo=700, hi=3500, nm=6, crack_hi=9000), slam_t, 1.0)
        if crack:
            m.add(crack_burst(sr, rng, tau=0.004, lo=1000, hi=9000, dur=0.03), slam_t, crack)
    m.add(sub_drop(sr, 120, 42, 0.25, 0.06, k=0.04), slam_t, sub_gain * low_scale)
    n = N
    base = sfx.lp_sweep_noise(n, sr, rng, bed[0], bed[1], hp=40, steps=10)
    base = base * churn(n, sr, rng, 22, 0.6)
    m.add(base * sfx.decay_env(n, sr, decay[0], decay[1], attack=0.01), slam_t, churn_gain * 0.5)
    gr = sfx.band_noise(n, sr, rng, 500, 3000, order=1) * churn(n, sr, rng, 45, 0.8) ** 2
    m.add(gr * sfx.decay_env(n, sr, 0.35, 1.0), slam_t + 0.05, grind * 0.3)
    lg = sfx.band_noise(n, sr, rng, lg_band[0], lg_band[1], order=6 if flat else 3) * churn(n, sr, rng, 18, 0.5)
    m.add(lg * sfx.decay_env(n, sr, 0.9, 1.9, attack=0.05), slam_t + 0.05, 0.9 * low_scale)
    lb = sfx.band_noise(n, sr, rng, 25, 150, order=6 if flat else 3) * churn(n, sr, rng, 10, 0.5)
    m.add(lb * sfx.decay_env(n, sr, 1.4, 2.6, attack=0.1), 0.0 if bed_from_zero else slam_t, (lb_gain if not soft else 0.2) * low_scale)
    if flat:
        # broad 200 Hz-8 kHz noise (even spectrum) so the mid band reads as noise, not as a few low peaks
        fl = sfx.band_noise(n, sr, rng, 200, min(8000, sr * 0.44), order=1) * churn(n, sr, rng, 25, 0.5)
        m.add(fl * sfx.decay_env(n, sr, flat_decay[0], flat_decay[1], attack=0.003), slam_t, flat)
    tick_ts = [t for t in tg.onsets if t < slam_t - 0.03]
    if c.path.endswith("work1_a.ogg"):
        tick_ts = [0.07, 0.32, 0.40, 0.55]
    for t in tick_ts:
        m.add(_tick(sr, rng), t, tick_gain)
    later = [t for t in tg.onsets if t > slam_t + 0.3][:6]
    for i, t in enumerate(later):
        g = late[i] if late and i < len(late) else clunks * (1.3 if i == 0 else 0.9)
        m.add(_clunk(sr, rng), t, g)
    return m.out


SEEDS_WORK1 = {}   # no seed beat the original draw (embedding 1.81 / 2.68)


@recipe("ammopress_work1_*")
def ammopress_work1(c, N):
    if c.path[-5] in SEEDS_WORK1:
        _reseed(c, SEEDS_WORK1[c.path[-5]])
    if c.path.endswith("_b.ogg"):
        # one decisive hit (the early clicks are nearly gone), churn bed, subtle late clunks
        kw = dict(slam_t=0.25, tick_gain=0.08, late=(0.3, 0.06), churn_gain=0.7, impact=1.2, flat=1.5, lg_band=(60, 190), bed_hi=300)
    elif c.path.endswith("_c.ogg"):
        # continuous rumble from 0 s, clank rebuilt, small late clunks (first about -9 dB, then ticks)
        kw = dict(slam_t=0.26, tick_gain=0.05, late=(0.8, 0.5, 0.5, 0.5), churn_gain=1.2, grind=0.8, lb_gain=0.2, low_scale=0.3,
                  impact=2.5, bed_from_zero=True, flat=1.6, lg_band=(60, 190), bed_hi=250, flat_decay=(0.3, 0.9))
    else:
        kw = dict(slam_t=0.25, clunks=0.8, churn_gain=1.0, grind=0.7, flat=0.5, lg_band=(60, 190), bed_hi=300)
    if c.path.endswith("_a.ogg"):
        c.conform_env = _tune().get('ce', False)
    if not c.path.endswith("_a.ogg"):
        kw.update({k: v for k, v in _tune7(c).items() if k != "seed"})   # b/c only (a is keep and must stay byte-identical)
    return _press(c, N, **kw)


@recipe("ammopress_work2_*")
def ammopress_work2(c, N):
    tg = get_tg(c)
    slam_t = max(tg.peak_t, 0.18)
    kw = dict(rings=(), churn_gain=0.8, grind=0.6, swell=0.0, soft=True, pre_tone=True, sub_gain=0.2, crack=0.0,
              bed_hi=450, tick_gain=0.1, decay=(0.6, 1.4), bed=(1500, 300))
    if c.path.endswith("_b.ogg"):
        kw.update(sub_gain=0.3, bed_hi=350, tick_gain=0.0, decay=(0.5, 1.2), flat=0.1)
    if c.path.endswith("_c.ogg"):
        kw.update(pre_db=(-25, -13), tick_gain=0.0, impact=0.5, decay=(0.7, 1.3))
    out = _press(c, N, slam_t, **kw)
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    n = sfx.n_of(sr, 0.45)
    pulse = 0.5 + 0.5 * np.sin(sfx.TAU * 32 * sfx.tvec(n, sr) + rng.uniform(0, 6))
    m.add(sfx.band_noise(n, sr, rng, 100, 400, order=2) * (0.4 + 0.6 * pulse) * sfx.smooth_env(n, sr, 0.05, 0.25), 0.4, 0.5)
    nk = sfx.n_of(sr, 0.05)
    m.add(sfx.band_noise(nk, sr, rng, 400, 500, order=2) * sfx.exp_env(nk, sr, 0.012) * 3, 0.4, 0.15)
    if c.path.endswith("_a.ogg"):
        m.add(_clunk(sr, rng), slam_t + 0.06, 0.35)   # second smaller clank, about -5 dB
    return out + fit(m.out, len(out))


@recipe("rc_beep")
def rc_beep(c, N):
    """A short 538 Hz beep (single clean sine, 5 ms fade-out, no trailing silence): a shallow dip (about 7 dB)
    mid-tone, a 25 ms low-passed onset thump (150 -> 50 Hz sweep plus a soft 1.5-2 kHz band)."""
    P = _tune7(c)
    sr, rng = c.sr, c.rng
    c.conform_env = False
    # 4851 samples (A's length) makes libvorbis write a bogus end granule, so decoders emit a 22 ms silent pad that the
    # tenth-second contour then reads as an empty last tenth.  4960 samples (+2.5 ms) decodes cleanly.
    c.n = 4760
    c.conform_bands = False   # the band conform seeded a 2-12 kHz hash over the first 50 ms; A is a bare tone there
    m = Mix(c, N)
    d = N / sr
    # a hair of 2nd harmonic and a -50 dB noise floor: a mathematically pure sine sits farther from A than this
    tn = tone_burst(sr, rng, 538, d, ratios=(1.0, 2.0), gains=(1.0, P.get('h2', 0.01)), attack=0.001, release=0.005)
    t = sfx.tvec(len(tn), sr)
    # A's measured tenth-second contour: held ~-5 dB, an 11 dB sag toward 0.07 s, then back up to -9.5 dB
    sag = 10 ** (np.interp(t, [0, 0.02, 0.033, 0.044, 0.055, 0.066, 0.077, 0.088, 0.099, 0.11],
                           [-6.5, -6.5, -8.5, -10.8, -14.0, -16.3, -14.9, -12.1, -9.5, -9.0]) / 20)
    m.add(tn * sag / sag.max(), 0.0, 1.0)
    nb = sfx.n_of(sr, 0.035)
    band = sfx.band_noise(nb, sr, rng, 1100, 2200, order=2) * sfx.smooth_env(nb, sr, 0.003, 0.0245)
    m.add(sfx.fftfilt(band, sr, lp=2600, order=2), 0.0, 0.35 * P.get('band', 1.0))
    m.add(sfx.band_noise(N, sr, rng, 200, 8000, order=1), 0.0, 0.003 * P.get('floor', 1.0))
    m.add(sub_drop(sr, 150, 50, 0.03, 0.01, k=0.01, attack=0.001), 0.0, 0.5 * P.get('thump', 1.0))
    return m.out


@recipe("rc_warning")
def rc_warning(c, N):
    """One continuous buzz: a 250 Hz saw-like ladder (harmonics 2-12, -6 dB/octave, upper harmonics broken up by
    fast random amplitude modulation so they never form a steady line) with a 0.7 Hz / 1% vibrato, over a
    sub rumble with 30 Hz grit.  A 10-15 ms attack at 0.05 s, a noise burst at 0.18-0.28 s, a 0.12-0.14 s inharmonic
    ring-modulated swell at 0.29-0.42 s, a second burst at 0.55-0.8 s, a +20 Hz blip at 0.73 s then a +3.5% glide,
    the sub ramping out at 1.18 s.  Only tone plus sub from 0.8 s on."""
    P = _tune7(c)
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    t = sfx.tvec(N, sr)
    blip = (20.0 / 226.0) * np.clip((t - 0.71) / 0.02, 0, 1) * np.exp(-np.clip(t - 0.73, 0, None) / 0.05)
    # the ladder starts 6% flat and rises into place over 0.06-0.26 s (A's harmonics bend up out of the onset)
    gl = 0.035 * np.clip((t - 0.73) / 0.3, 0, 1) - 0.06 * np.clip(1 - (t - 0.06) / 0.2, 0, 1)
    vib = 1 + 0.01 * np.sin(sfx.TAU * 0.7 * t)
    f = 226.0 * (1 + gl + blip) * vib * (1 + 0.003 * (churn(N, sr, rng, 3, 1.0) - 0.5))
    ph = sfx.osc_phase(f, sr)
    tone = np.zeros(N)
    prng = np.random.default_rng(5)   # fixed ladder phases (a seed with no spurious tonal line)
    for k in range(1, 6):
        tone += np.sin(k * ph + prng.uniform(0, sfx.TAU)) / k
    # brightness without sustained lines: 1.5-3.2 kHz noise pulsed at the buzz rate (a noisy buzz ladder)
    pulse = np.clip(0.5 + 0.5 * np.sin(ph + 0.6), 0, 1) ** 3
    hi = sfx.band_noise(N, sr, rng, 1500, min(3200, sr * 0.44), order=2) * pulse * churn(N, sr, rng, 20, 0.6)
    tone = tone + 0.15 * P.get('hi', 1.0) * hi
    sw = np.clip((t - 1.18) / 0.05, 0, 1)
    tone = tone * (1 - 0.8 * sw)
    att = np.clip((t - 0.05) / 0.012, 0, 1)
    m.add(sfx.fftfilt(tone, sr, hp=70, lp=9000, order=1) * att, 0.0, 1.0)
    sub = sfx.band_noise(N, sr, rng, 25, 160, order=5) * churn(N, sr, rng, 12, 0.5) * (1 + 0.1 * np.sin(sfx.TAU * 30 * t)) * (1 - sw) * att
    m.add(sub, 0.0, 0.9 * P.get('sub', 1.0))
    m.add(sub_drop(sr, 100, 60, 0.1, 0.02, k=0.015, attack=0.002), 0.05, 0.8)
    # inharmonic swell 0.29-0.42 s: ring-modulated partials between 400 and 2500 Hz, rise then fall
    ns = sfx.n_of(sr, 0.13)
    ts = sfx.tvec(ns, sr)
    swell = np.zeros(ns)
    for fa, fb, g in ((430, 1010, 0.8), (680, 1080, 0.2), (560, 1150, 0.15)):
        swell += g * np.sin(sfx.TAU * fa * ts) * np.sin(sfx.TAU * fb * ts + rng.uniform(0, 6))
    swell = swell + 0.5 * sfx.band_noise(ns, sr, rng, 400, 2400, order=1)
    m.add(swell * np.sin(np.pi * ts / 0.13) ** 2, 0.29, 0.9 * P.get('swell', 1.0))
    # buzzy 4-9 kHz noise bursts pulsed at about 70 Hz
    for t0, d in ((0.18, 0.10), (0.55, 0.25)):
        nn = sfx.n_of(sr, d)
        gate = np.clip(np.sin(sfx.TAU * 70 * sfx.tvec(nn, sr)) * 3, 0, 1)
        m.add(sfx.band_noise(nn, sr, rng, 4000, min(9000, sr * 0.44), order=2) * gate * sfx.smooth_env(nn, sr, 0.015, 0.04), t0, 0.3 * P.get('burst', 1.0))
    return m.out



@recipe("rc_heatray_work")
def rc_heatray(c, N):
    """Dark grainy low rumble (nothing above ~3 kHz), slow swell, irregular bumps, rare soft ticks, and a very
    faint 500-2000 Hz electrical flutter pulsed at 20-40 Hz."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    rm = rumble(c, N, lo=28, hi=600, rate=9, depth=0.6, grit=0.2, glide=(500, 250), crackle=0.015)
    m.add(sfx.fftfilt(rm, sr, lp=3500, order=2), 0.0, 1.0)
    fl = sfx.band_noise(N, sr, rng, 500, 2000, order=1) * churn(N, sr, rng, 30, 0.9)
    m.add(fl, 0.0, 0.025)
    for t in [t for t in tg.onsets if t > 0.8][:6]:
        n = sfx.n_of(sr, 0.14)
        m.add(sfx.band_noise(n, sr, rng, 35, 320, order=2) * sfx.smooth_env(n, sr, 0.03, 0.08), t, 0.55)
    return m.out


# =========================================================================== blocks
SEEDS_DOOR = {"a": 1, "b": 7}
DOOR_TUNE = {"a": dict(cg=10.0), "b": dict(cg=4.0, rg=0.6)}   # a stronger damped-mode clank and less rattle: noisiness and embedding pass


@recipe("metaldoor_open_*")
def metaldoor(c, N):
    """Quiet latch (soft, rolled off, damped modes), clanks made of a short transient plus damped mid-band modes
    (more of them in the latch phase on the b variant), a heavy hit with damped 250-780 Hz body resonances, a
    hinge creak (noise band gliding 1.6 -> 0.8 kHz with a 25 Hz scrape wobble), a continuous rattle fill, a
    smooth low-mid scrape tail, and only small low thumps."""
    tg = get_tg(c)
    P = {**DOOR_TUNE.get(c.path[-5], {}), **_tune()}
    if c.path[-5] in SEEDS_DOOR:
        _reseed(c, SEEDS_DOOR[c.path[-5]])   # a/b: seeds picked by embedding distance (c is already keep: untouched)
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    c.conform_env = False   # shape the contour here: A's body decays slowly after the hit
    tau_b = {"a": 0.17, "b": 0.14, "c": 0.13}[c.path[-5]]
    ons = tg.onsets or [0.05, 0.13, 0.24]
    three = len(ons) > 2
    var = c.path[-5]
    t_latch = ons[0]
    t_knock = ons[1] if three else 0.13
    t_hit = tg.peak_t if tg.peak_t > 0.1 else ons[-1]
    # latch: quiet, low-passed at ~3 kHz, 3-5 ms attack, about 40 ms decay so it joins the rattle
    nl = sfx.n_of(sr, 0.06)
    lt = sfx.band_noise(nl, sr, rng, 600, 3000, order=2) * sfx.exp_env(nl, sr, 0.014, attack=0.004)
    m.add(lt, t_latch, 0.3)
    if three:
        m.add(_clank(sr, rng, 0.55 if var != "b" else 0.22, lo=700, hi=3500, nm=4, crack=0.4), t_knock, 1.0)
    else:
        nth = sfx.n_of(sr, 0.4)
        th = sfx.band_noise(nth, sr, rng, 250, 450, order=2) * sfx.exp_env(nth, sr, 0.12, attack=0.004)
        m.add(th, t_latch, 2.0)
        # scrape bridging the gap before the second clunk
        ng = sfx.n_of(sr, max(t_hit - t_latch, 0.1))
        m.add(sfx.band_noise(ng, sr, rng, 800, 3000, order=2) * (0.5 + 0.5 * np.sin(sfx.TAU * 20 * sfx.tvec(ng, sr))) ** 2, t_latch + 0.02, 0.3)
    if var == "b":
        for tb, g in ((0.07, 0.07), (0.13, 0.2), (0.19, 0.14)):
            m.add(_clank(sr, rng, g, lo=800, hi=4500, nm=3, tau=(0.01, 0.022), crack_hi=5000), tb, 1.0)
    # door body / hinge hit: attack 3 ms, 120-900 Hz body, damped 250-780 Hz resonances, short clank on top
    n = sfx.n_of(sr, 0.6)
    body = sfx.fftfilt(sfx.noise(n, rng), sr, hp=120 if var != "b" else 90, lp=900, order=2) * sfx.exp_env(n, sr, tau_b, attack=0.003)
    m.add(body, t_hit, 1.2)
    m.add(_modal_noise(sr, rng, [190, 310, 480, 700], tau_b, dur=0.6, width=0.12), t_hit, (2.0 if var != "b" else 3.6) * P.get("mg", 1.0))
    res = _modal_noise(sr, rng, [250 * rng.uniform(0.95, 1.05), 420 * rng.uniform(0.95, 1.05), 610 * rng.uniform(0.95, 1.05), 780 * rng.uniform(0.95, 1.05)], 0.15, dur=0.5)
    m.add(res, t_hit, 2.0 * P.get('mg', 1.0))
    m.add(_clank(sr, rng, (0.45 if var != "b" else 0.25) * P.get("cg", 1.0), lo=600, hi=3500, nm=5, crack_hi=7000, crack=0.4), t_hit, 1.0)
    if not three:
        # brighter second clunk: 2-5 kHz knock over a 200-500 Hz thump
        m.add(_clank(sr, rng, 0.5, lo=2000, hi=5000, nm=3, tau=(0.008, 0.02)), t_hit, 1.0)
        nlt = sfx.n_of(sr, 0.3)
        m.add(sfx.band_noise(nlt, sr, rng, 200, 500, order=2) * sfx.exp_env(nlt, sr, 0.06, attack=0.002), t_hit, 0.6)
    m.add(sub_drop(sr, 120, 60, 0.12, 0.04, k=0.02), t_hit, 0.08 if var != "a" else 0.02)
    # a few short low thumps (about 40 ms)
    for dt in (0.09, 0.19, 0.31):
        tb = t_hit + dt + rng.uniform(-0.015, 0.015)
        nlw = sfx.n_of(sr, 0.07)
        lw = sfx.band_noise(nlw, sr, rng, 60, 200, order=2) * sfx.exp_env(nlw, sr, 0.02, attack=0.003)
        m.add(lw, tb, (0.3 if var != "a" else 0.12) * rng.uniform(0.6, 1.2))
    # continuous rattle after the hit: 400 Hz-3 kHz, 25-40 Hz bursts, stronger and longer than before
    nr = sfx.n_of(sr, 0.65)
    rat = _rattle(sr, rng, 0.65, [520, 830, 1250, 1900, 2700] if var != "a" else [520, 830, 1250, 1700]) * sfx.decay_env(nr, sr, 0.2 if var != "c" else 0.08, 0.4 if var != "c" else 0.16)
    m.add(rat, t_hit + 0.03, (2.2 if var != "c" else 1.2) * P.get('rg', 1.0))
    # rattle bed between the first onset and the hit (A is busy there)
    if t_hit - t_latch > 0.06:
        npre = sfx.n_of(sr, t_hit - t_latch)
        pre = _rattle(sr, rng, t_hit - t_latch, [520, 830, 1250, 1900] if var != "a" else [520, 830, 1250], rate=26.0)
        m.add(pre * sfx.smooth_env(npre, sr, 0.03, 0.02), t_latch + 0.02, {"a": 1.1, "b": 0.9, "c": 2.2}[var] * P.get("pg", 1.0))
    # hinge creak: noise band gliding 1.6 -> 0.8 kHz with a 25 Hz scrape wobble
    nc = sfx.n_of(sr, 0.4)
    tc = sfx.tvec(nc, sr)
    cr = sfx.dark_noise(nc, sr, rng, 1600, 800, steps=8, hp=700) * (0.5 + 0.5 * np.sin(sfx.TAU * 25 * tc + rng.uniform(0, 6)))
    m.add(cr * sfx.smooth_env(nc, sr, 0.05, 0.2), t_hit + 0.1, 0.2)
    # smooth low-mid scrape tail
    nt = sfx.n_of(sr, 0.45)
    m.add(sfx.lp_sweep_noise(nt, sr, rng, 1500, 500, hp=200, steps=8) * sfx.exp_env(nt, sr, 0.18) * churn(nt, sr, rng, 15, 0.5), t_hit + 0.15, 0.2 if var != "c" else 0.05)
    if var == "b":
        nbc = sfx.n_of(sr, 0.02)
        m.add(sfx.band_noise(nbc, sr, rng, 3000, 8000, order=1) * sfx.exp_env(nbc, sr, 0.005), 0.40, 0.3)
    return m.out
