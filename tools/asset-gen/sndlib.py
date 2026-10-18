"""Shared helpers for the per-family sound recipe modules (sounds_guns.py, sounds_npcs.py, ...).

Every recipe is original synthesis built from ``sfx`` blocks.  What this module adds is the
*design-by-reference* layer: the numeric measurements of the sound being replaced (length, decay
points, onset times, band balance, loudness contour) live in ``sound-targets.json``.  They are plain
numbers read off the originals' published analysis; no audio is read, sampled or convolved.

A recipe is ``f(c, N) -> signal`` (``N`` samples, longer than the sound; ``synth`` trims).  It is
registered with ``@recipe("exact_name", "prefix*", ...)`` in its family module.  After the recipe
returns, ``conform`` (called from ``gen_sounds.synth``) reshapes the slow loudness contour and the
static band balance toward the measured targets, unless the recipe switched that off with
``c.conform_env = False`` / ``c.conform_bands = False``.
"""
from __future__ import annotations

import json
import os

import numpy as np

import sfx

HERE = os.path.dirname(os.path.abspath(__file__))
TARGETS_PATH = os.path.join(HERE, "sound-targets.json")
_TARGETS = None

REGISTRY = []  # (matcher, func) in registration order


def recipe(*names):
    """Register a recipe for exact names or prefixes (a trailing ``*``)."""

    def deco(fn):
        for nm in names:
            REGISTRY.append((nm, fn))
        return fn

    return deco


def lookup(name):
    name = name.lower()
    for nm, fn in REGISTRY:
        nm = nm.lower()
        if nm.endswith("*"):
            if name.startswith(nm[:-1]):
                return fn
        elif name == nm:
            return fn
    return None


def targets():
    global _TARGETS
    if _TARGETS is None:
        _TARGETS = json.load(open(TARGETS_PATH, encoding="utf8")) if os.path.exists(TARGETS_PATH) else {}
    return _TARGETS


class Tg:
    """Measured facts about the sound being replaced."""

    def __init__(self, d):
        d = d or {}
        self.ok = bool(d)
        self.d = d
        self.dur = d.get("duration_s") or 1.0
        self.peak_t = d.get("peak_time_s") or 0.0
        self.attack = d.get("attack_s") or 0.0
        self.dec20 = d.get("decay_to_minus20db_s")
        self.dec40 = d.get("decay_to_minus40db_s")
        self.onsets = list(d.get("onset_times_s") or [])
        self.env10 = list(d.get("envelope_db_by_tenth") or [])
        self.cen = d.get("spectral_centroid_hz") or 1500
        self.flat = d.get("spectral_flatness") or 0.1
        self.bands = d.get("band_energy_pct") or {}
        self.quarters = d.get("centroid_by_quarter_hz") or []
        self.trail = d.get("trailing_silence_s") or 0.0
        self.lead = d.get("leading_silence_s") or 0.0
        self.active = d.get("active_fraction") or 0.5
        self.crest = d.get("crest_factor_db") or 15.0
        self.peaks = d.get("prominent_peaks_hz") or []

    # fraction of energy in a band (0..1)
    def band(self, name):
        return float(self.bands.get(name, 0.0)) / 100.0

    @property
    def end_t(self):
        """End of audible content (s)."""
        return max(0.05, self.dur - self.trail)

    def rel_env_pts(self):
        """(times, rel_db): target loudness contour in dB relative to the loudest frame, or None."""
        if not self.env10 or len(self.env10) < 11:
            return None
        e = np.array(self.env10, dtype=float)
        # the loudest 10 ms frame sits about 7 dB under the sample peak for typical sounds
        pk_abs = max(float(e.max()), float(self.d.get("peak_dbfs") or -3.0) - 7.4)
        pts = {}
        nf = max(1.0, self.dur / 0.010)
        for k in range(11):
            tk = k * (nf - 1) / 10.0 * 0.010
            pts[round(tk, 3)] = max(float(e[k]) - pk_abs, -80.0)
        pts[round(self.peak_t, 3)] = 0.0
        # decay markers refine the shape between the 0.1 s samples, but a marker that contradicts the
        # samples around it (a dropout in the original) is ignored
        base_t = np.array(sorted(pts))
        base_v = np.array([pts[t] for t in base_t])
        for dec, val in ((self.dec20, -20.0), (self.dec40, -40.0)):
            if dec:
                tm = round(dec + self.peak_t, 3)
                if tm not in pts and abs(float(np.interp(tm, base_t, base_v)) - val) <= 12.0:
                    pts[tm] = val
        ts = np.array(sorted(pts))
        return ts, np.array([pts[t] for t in ts])


def get_tg(c):
    if getattr(c, "tg", None) is None:
        c.tg = Tg(targets().get(c.path))
    return c.tg


class Mix:
    """Accumulator with time placement in seconds."""

    def __init__(self, c, N):
        self.c = c
        self.sr = c.sr
        self.rng = c.rng
        self.out = np.zeros(N)
        self.N = N

    def add(self, sig, at=0.0, g=1.0):
        sfx.place(self.out, sig, at, self.sr, g)
        return self


def _crest_goal(c, tg, x):
    """Peak-to-RMS goal: the original's (plus a little), so the finishing limiter has little to do."""
    want = min(max(tg.crest or 15.0, 8.0), 24.0) + 1.0
    tl = min(float(c.e["target_lufs"]), -3.5)
    if -60 < tl:
        want -= max(0.0, tl + 12.0) * 1.3   # very loud originals are dense: less crest, or the loudness is unreachable
    return max(want, 6.0)


def conform(c, x):
    """Reshape the slow contour and the static band balance of x (length >= c.n) toward the targets.

    Order: crest control (so the finishing soft clip never crushes the transient), band balance,
    loudness contour (after the EQ, which changes crack-versus-body levels), crest control again, then the original's silent end."""
    tg = get_tg(c)
    sr = c.sr
    n = c.n
    x = x[:n].copy()
    if not tg.ok:
        return x
    lim = bool(getattr(c, "limit_crest", True)) and not c.looping
    if lim:
        x = sfx.limit_crest(x, _crest_goal(c, tg, x))
    def _dbg(tag, y):
        if os.environ.get("GG_DEBUG"):
            ed, _h = sfx.env_frames(y, sr)
            nf = len(ed)
            tenths = [round(float(ed[int(f * (nf - 1) / 10)] - ed.max())) for f in range(11)]
            print("%-8s env %s bands %s crest %.1f" % (tag, tenths, np.round(sfx.band_fractions(y, sr) * 100, 1), sfx.crest_db(y)))

    _dbg("recipe", x)
    do_bands = getattr(c, "conform_bands", True) and bool(tg.bands)
    do_env = getattr(c, "conform_env", True) and not c.looping and tg.rel_env_pts() is not None
    pts = tg.rel_env_pts() if do_env else None
    # low-frequency material must not be gain-modulated quickly (that would splatter it upwards)
    gs = 0.12 if tg.cen < 400 else 0.05 if tg.cen < 1000 else 0.015

    def _bands(y):
        if do_bands and np.max(np.abs(y)) > 1e-6:
            return sfx.conform_bands(y, sr, tg.bands, strength=getattr(c, "band_strength", 1.0),
                                     limit=getattr(c, "band_limit", 45.0), rng=c.rng)
        return y

    def _env(y, passes):
        if not do_env:
            return y
        for _ in range(passes):
            y = sfx.conform_env(y, sr, pts, gain_smooth_s=gs, smooth_s=getattr(c, "env_smooth", 0.02 if (tg.dur < 1.3 and tg.crest > 19) else 0.05),
                                lo=getattr(c, "env_lo", -30.0), hi=getattr(c, "env_hi", 12.0))
        return y

    # the EQ changes crack-versus-body levels and the contour changes how frames weigh in the band
    # balance, so alternate: bands, contour, bands, contour
    if do_env and getattr(c, "fill_tail", True):
        x = sfx.fill_dead(x, sr, pts, c.rng)
        _dbg("fill", x)
    x = _bands(x)
    _dbg("bands1", x)
    x = _env(x, 2)
    _dbg("env1", x)
    x = _bands(x)
    x = _env(x, 2)
    _dbg("env2", x)
    x = _bands(x)
    _dbg("bands3", x)
    if lim:
        x = sfx.limit_crest(x, _crest_goal(c, tg, x))
    # honor the originals' silent end without a hard cut
    quiet_end = bool(tg.env10) and (max(tg.env10[-2:]) - max(tg.env10)) < -62
    if not c.looping and tg.trail > 0.06 and quiet_end:
        end = tg.end_t + 0.04
        i = sfx.n_of(sr, end)
        if i < n:
            rel = sfx.n_of(sr, 0.12)
            x[i:] = 0.0
            r0 = max(0, i - rel)
            x[r0:i] *= np.linspace(1, 0, i - r0) ** 2
    return x


# --------------------------------------------------------------------------- reusable voices
def crack_burst(sr, rng, tau=0.006, lo=500, hi=14000, dur=0.1, attack=0.0002):
    """Hard broadband transient with a near-instant attack."""
    n = sfx.n_of(sr, dur)
    g = sfx.noise(n, rng) * sfx.exp_env(n, sr, tau, attack=attack)
    return sfx.fftfilt(g, sr, hp=lo, lp=min(hi, sr * 0.45), order=1)


def sub_drop(sr, f0, f1, dur, tau, k=0.03, attack=0.001):
    n = sfx.n_of(sr, dur)
    return np.sin(sfx.sweep(f0, f1, n, sr, k=k)) * sfx.exp_env(n, sr, tau, attack=attack)


def body_noise(sr, rng, dur, f_hi, f_lo, t20, t40=None, hp=40, attack=0.0008):
    """Noise whose brightness falls f_hi -> f_lo with a two-slope decay."""
    n = sfx.n_of(sr, dur)
    return sfx.dark_noise(n, sr, rng, f_hi, f_lo, steps=7, hp=hp) * sfx.decay_env(n, sr, t20, t40, attack=attack)


def mech_click(sr, rng, bright=1.0, dur=0.012, ring=None, ring_amp=0.2, body=0.0):
    """Small metal/plastic click: noise grain, optional pitched ring and tiny low body."""
    n = sfx.n_of(sr, max(dur * 5, 0.03))
    g = sfx.noise(n, rng) * sfx.exp_env(n, sr, dur * 0.35, attack=0.0002)
    g = sfx.fftfilt(g, sr, hp=1800 * bright, lp=min(12000, sr * 0.45), order=2)
    if ring is None:
        ring = rng.uniform(2400, 5200) * bright
    t = sfx.tvec(n, sr)
    g = g + ring_amp * np.sin(sfx.TAU * ring * t) * np.exp(-t / (dur * 0.9))
    if body:
        g = g + body * np.sin(sfx.TAU * rng.uniform(180, 420) * t) * np.exp(-t / 0.008)
    return g


def mech_clack(sr, rng, f0=None, tau=0.035, bright=1.0, thump=0.0, dur=0.14):
    """Metal-on-metal clack: sharp noise snap, inharmonic ring and optional low thump."""
    n = sfx.n_of(sr, dur)
    f0 = f0 or rng.uniform(900, 1900)
    sn = sfx.noise(n, rng) * sfx.exp_env(n, sr, 0.0035, attack=0.0002)
    sn = sfx.fftfilt(sn, sr, hp=1100 * bright, lp=min(10000 * bright, sr * 0.45), order=2)
    ring = sfx.metal_ring(n, sr, rng, f0, tau, nm=4)
    out = sn * 0.9 + 0.12 * ring
    if thump:
        th = sfx.tvec(n, sr)
        out = out + thump * np.sin(sfx.TAU * rng.uniform(130, 260) * th) * np.exp(-th / 0.018)
    return out


def mech_slam(sr, rng, dur=0.5, body_f=(200, 500), tau=0.04, ring_f=(7800, 9200, 12600), ring_tau=0.04, bass=0.3):
    """Heavy bolt/slide slam: full-spectrum crack, 200-500 Hz body, short metallic ring."""
    n = sfx.n_of(sr, dur)
    t = sfx.tvec(n, sr)
    cr = sfx.noise(n, rng) * sfx.exp_env(n, sr, 0.003, attack=0.0002)
    cr = sfx.fftfilt(cr, sr, hp=600, lp=min(13000, sr * 0.45), order=1)
    bd = sfx.fftfilt(sfx.noise(n, rng), sr, hp=body_f[0] * 0.7, lp=body_f[1] * 1.4, order=2) * sfx.exp_env(n, sr, tau)
    bd = bd + np.sin(sfx.TAU * np.sqrt(body_f[0] * body_f[1]) * t) * np.exp(-t / tau) * 0.3
    ri = sfx.ping(sr, rng, ring_f, ring_tau, n=n) * 0.15
    lo = np.sin(sfx.sweep(150, 60, n, sr, k=0.02)) * np.exp(-t / 0.05) * bass
    return cr * 0.9 + bd * 1.1 + ri + lo


def rattle(sr, rng, dur, density, lo=1500, hi=8000, tau=0.003, decay=None, amp_var=0.8):
    n = sfx.n_of(sr, dur)
    env = sfx.exp_env(n, sr, decay) if decay else None
    return sfx.grain_cloud(n, sr, rng, density, tau, lo, hi, env=env, amp_var=amp_var)
