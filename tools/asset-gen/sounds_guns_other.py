"""Gun handling sounds (round 5 slice ``guns-other``): reloads, rechamber, pins, impacts.

Imported at the end of sounds_guns.py.  Every recipe is original synthesis from noise and damped modal clicks; the
only thing read from the original is its numeric targets (onset times, loudness contour).  The helpers are private
copies of the round-3 handling helpers so edits to the shot recipes in sounds_guns.py cannot change these sounds.
Per-sound parameters were fitted against the measured gate and the Audio Flamingo embedding distance
(tools/asset-gen/sound_gate.py); the recipes are seeded, so output is reproducible.
"""
from __future__ import annotations

import json
import os

import numpy as np

import sfx
from sndlib import Mix, get_tg, REGISTRY, sub_drop
from sndengines import blast, events, rumble, churn, chug_train, foley_event, rel_env_at

_RATIOS = np.array([1.0, 1.47, 1.93, 2.52, 3.17, 3.9, 4.7])


_MT = 1.0  # damped-mode decay scale (set per sound by events5)
_CLK = 1.0  # transient gain of a handling hit (set per sound by events5)


def _u(x):
    """Unit-RMS copy."""
    return x / (np.sqrt(np.mean(x * x)) + 1e-12)


def _setup(c, first_floor=None):
    """Targets of the original, with conform's hard cut at its trailing silence disabled (tails fade on their
    own) and, optionally, a floor under the first loudness point so a sharp crack at t=0 is not ironed out."""
    tg = get_tg(c)
    if not hasattr(tg, "trail_real"):
        tg.trail_real = tg.trail
        tg.trail = 0.0
        if first_floor is not None and tg.env10 and len(tg.env10) >= 11:
            tg.env10[0] = max(tg.env10[0], max(tg.env10) - first_floor)
    return tg


def _band(x, sr, lo, hi, order=2):
    return sfx.fftfilt(x, sr, hp=lo, lp=min(hi, sr * 0.45), order=order)


def _impulse_click(sr, rng, bright=1.0, lp=10500.0):
    n = sfx.n_of(sr, 0.02)
    x = sfx.noise(n, rng) * sfx.exp_env(n, sr, rng.uniform(0.001, 0.0025), attack=0.0001)
    return _u(_band(x, sr, 500, lp * bright, 2))


def _body_burst(sr, rng, lo=150.0, hi=1200.0, tau=0.03, dur=None):
    n = sfx.n_of(sr, dur or tau * 7)
    x = sfx.noise(n, rng) * sfx.exp_env(n, sr, tau, attack=0.0005)
    return _u(_band(x, sr, lo, hi, 2))


def _thump(sr, f0=110.0, f1=45.0, tau=0.07):
    return sub_drop(sr, f0, f1, tau * 6, tau, k=0.03, attack=0.002)


def _rattle(sr, rng, count=None, span=None, lo=200.0, hi=2500.0, bright=1.0):
    """Dense train of 5-20 small modal clicks over 30-150 ms, jittered, decaying."""
    count = count or int(rng.integers(6, 18))
    span = span or rng.uniform(0.04, 0.14)
    out = np.zeros(sfx.n_of(sr, span + 0.1))
    ts = np.sort(rng.uniform(0, span, count))
    for i, t in enumerate(ts):
        g = _modal(sr, rng, int(rng.integers(2, 4)), (700.0, 1800.0), (0.003, 0.006), 0.03)
        ic = _impulse_click(sr, rng, 0.9 * bright)
        k = min(len(g), len(ic))
        g = _u(g)
        g[:k] += 0.8 * ic[:k]
        sfx.place(out, g, t, sr, (1 - i / (count + 1.0)) ** 1.2 * rng.uniform(0.4, 1.0))
    return out / (np.max(np.abs(out)) + 1e-9)


def _hit(sr, rng, kind, bright=1.0, bass=0.0, ts=1.0):
    """One handling hit, peak-normalized to 1: transient + damped inharmonic modes + short body."""
    tr = _CLK * _impulse_click(sr, rng, bright)
    if kind == "click":
        parts = [tr, 0.4 * _u(_modal(sr, rng, 3, (800, 1600), (0.003, 0.006), 0.05)),
                 0.5 * _body_burst(sr, rng, 250, 1500, rng.uniform(0.012, 0.025) * ts)]
    elif kind == "clack":
        parts = [tr, 0.42 * _u(_modal(sr, rng, 5, (600, 1300), (0.003, 0.006), 0.05)),
                 0.7 * _body_burst(sr, rng, 200, 1400, rng.uniform(0.02, 0.04) * ts),
                 0.35 * _rattle(sr, rng, count=int(rng.integers(5, 10)), span=0.05, bright=bright)]
    elif kind == "slam":
        parts = [1.1 * tr, 0.35 * _u(_modal(sr, rng, 6, (600, 1200), (0.003, 0.007), 0.06)),
                 0.9 * _body_burst(sr, rng, 150, 1200, rng.uniform(0.04, 0.06) * ts),
                 0.5 * _rattle(sr, rng, count=int(rng.integers(8, 16)), span=0.12, bright=bright)]
        if bass > 0:
            parts.append(bass * _u(_thump(sr, 120, 45, rng.uniform(0.05, 0.1))))
    elif kind == "thunk":
        parts = [0.6 * _impulse_click(sr, rng, 0.7, 6000), 0.35 * _u(_modal(sr, rng, 3, (400, 800), (0.003, 0.006), 0.05)),
                 1.0 * _body_burst(sr, rng, 120, 700, rng.uniform(0.03, 0.05) * ts)]
        if bass > 0:
            parts.append(0.4 * bass * _u(_thump(sr, 110, 55, 0.05)))
    else:
        raise ValueError(kind)
    out = _sumn(parts)
    out = out / (np.max(np.abs(out)) + 1e-9)
    # the originals' hits are dense (peak only ~10 dB over their own RMS); the soft saturation keeps the crest low so
    # the finishing crest limiter has nothing tall to flatten (it would splatter low-frequency energy)
    return np.tanh(4.0 * out) / np.tanh(4.0)


def _sumn(parts):
    n = max(len(p) for p in parts)
    out = np.zeros(n)
    for p in parts:
        out[:len(p)] += p
    return out


def _grain_texture(N, sr, rng, dens, bands=None):
    """Dense train of small modal clicks (Poisson times), unit RMS: the handling bed between and around the hits,
    peaky like rattling metal instead of white."""
    d = np.broadcast_to(np.asarray(dens, dtype=float), (N,))
    out = np.zeros(N)
    i = int(rng.exponential(sr / max(d[0], 1.0)))
    while i < N:
        g = _modal(sr, rng, int(rng.integers(2, 4)), (600.0, 2200.0), (0.003, 0.006), 0.03)
        ic = _impulse_click(sr, rng, 0.9)
        k = min(len(g), len(ic))
        g = _u(g)
        g[:k] += 0.9 * ic[:k]
        seg = min(len(g), N - i)
        out[i:i + seg] += g[:seg] * rng.uniform(0.25, 1.0)
        i += int(rng.exponential(sr / max(d[min(i, N - 1)], 1.0))) + 1
    return _u(out)


def _modal(sr, rng, n_modes=4, base=(600.0, 1400.0), tau=(0.0025, 0.006), dur=0.05, top=3500.0):
    """Damped inharmonic modes.  The tonality gate sees any mode that rings 40 ms or longer as a line, so each mode
    decays in 4-9 ms and its phase is jittered (random walk) so it is a narrow noise band (about 8-15 % wide), not a
    pure sinusoid.  The metal character comes from several overlapping short modes and from click density."""
    n = sfx.n_of(sr, dur)
    t = np.arange(n) / sr
    b = rng.uniform(*base)
    out = np.zeros(n)
    for i in range(n_modes):
        if i == n_modes - 1 and n_modes >= 4 and rng.random() < 0.5:
            f, ti = rng.uniform(5200.0, 8000.0), rng.uniform(0.001, 0.003)   # air mode: very short
        else:
            f, ti = min(b * _RATIOS[i] * rng.uniform(0.93, 1.07), top), rng.uniform(*tau) * _MT / (1.0 + 0.15 * i)
        wob = np.cumsum(rng.standard_normal(n)) * (0.4 * 2 * np.pi * f / sr) * 0.5
        out += np.sin(2 * np.pi * f * t + rng.uniform(0, 2 * np.pi) + wob) * np.exp(-t / ti) / (1.0 + 0.35 * i)
    return out


def noise_glide(sr, rng, t0, t1, f0, f1, bw=1.4, steps=10):
    """Band-limited NOISE gliding f0 -> f1 (an electronic charge sound that is not a tone: wide, grainy)."""
    n = sfx.n_of(sr, t1 - t0)
    t = np.arange(n) / sr
    f = f0 * (f1 / f0) ** (t / max(t1 - t0, 1e-3))
    base = sfx.noise(n, rng)
    grid = np.geomspace(min(f0, f1), max(f0, f1), steps) if abs(f1 - f0) > 1 else np.array([f0])
    bank = [_band(base, sr, g / bw, g * bw, 2) for g in grid]
    pos = np.interp(np.log(f), np.log(grid), np.arange(len(grid))) if len(grid) > 1 else np.zeros(n)
    out = np.zeros(n)
    for i, b in enumerate(bank):
        out += b * np.clip(1 - np.abs(pos - i), 0, 1)
    env = np.minimum(t / max((t1 - t0) * 0.25, 1e-3), 1.0) * np.minimum((t1 - t0 - t) / 0.06, 1.0)
    return _u(out) * np.clip(env, 0, 1) * (0.5 + 0.5 * churn(n, sr, rng, 40, 0.8))


def low_pulses(sr, rng, t0, t1, rate, N, f0=95.0, f1=36.0, tau=0.06, fade=True):
    """Rhythmic 30-100 Hz thumps (jittered rate), level fading over the span."""
    out = np.zeros(N)
    t = t0
    while t < t1:
        g = (1.0 - 0.6 * (t - t0) / max(t1 - t0, 1e-3)) if fade else 1.0
        sfx.place(out, _thump(sr, f0 * rng.uniform(0.9, 1.1), f1, tau * rng.uniform(0.8, 1.2)), t, sr, g * rng.uniform(0.7, 1.0))
        t += (1.0 / rate) * rng.uniform(0.8, 1.2)
    return out


def low_rumble(sr, rng, t0, t1, N, lo=20.0, hi=190.0, rate=10.0):
    """Irregular sub/bass rumble over a span (smooth fades at both ends)."""
    n = sfx.n_of(sr, t1 - t0)
    x = _u(sfx.fftfilt(sfx.noise(n, rng), sr, hp=lo, lp=hi, order=2)) * churn(n, sr, rng, rate, 0.7)
    x = x * sfx.smooth_env(n, sr, min(0.08, (t1 - t0) * 0.3), min(0.2, (t1 - t0) * 0.4))
    out = np.zeros(N)
    sfx.place(out, x, t0, sr, 1.0)
    return out


def _frame_comp(x, sr, gamma, max_db, win):
    """Downward range compression at frame level: lifts the quiet frames between and after the hits toward the peak.
    The originals' handling bursts are dense, so their 10 ms frames sit much closer to the loudest frame than a
    train of sparse spiky clicks does.  gain (dB) = gamma * (distance below the peak frame), capped at max_db."""
    k = max(2, int(win * sr))
    ker = np.hanning(2 * k + 1)
    e = np.sqrt(np.convolve(x * x, ker / ker.sum(), mode="same")) + 1e-12
    rel = 20.0 * np.log10(e / e.max())
    g = np.clip(-rel * gamma, 0.0, max_db)
    g[rel < -78.0] = 0.0  # true gaps stay empty
    k2 = max(3, int(0.02 * sr))
    g = np.convolve(np.pad(g, (k2, k2), mode="edge"), np.hanning(2 * k2 + 1) / np.hanning(2 * k2 + 1).sum(), mode="valid")
    return x * 10.0 ** (g / 20.0)


def events5(c, N, *, bright=1.0, bass=0.35, bed=0.0, bed_floor=-46.0, tau=0.075, tex=1.0, tex_dens=420.0,
            lows=(), glides=(), ons=None, drop=(), add=(), kinds_at=None, big_kind="slam", big_gain=1.0,
            heavy_top=0.2, floor_db=-36.0, level_pow=0.7, tail=0.12, hit_ts=1.0, amp_floor=0.35,
            mt=1.0, clk=1.0, tex_bright=1.0, comp=0.0, comp_max=18.0, comp_win=0.012):
    """Round-2 handling sequence from noise only.

    ``lows``   = ('pulses', t0, t1, rate, gain) or ('rumble', t0, t1, gain), in units of the loudest hit;
    ``glides`` = (t0, t1, f0, f1, gain): wide noise glide for an electronic charge sound (kept below 2.5 kHz);
    ``bed``    = level of the mid-heavy handling bed where the original is not silent between clusters."""
    global _MT, _CLK
    _MT, _CLK = mt, clk  # mode-decay scale and transient gain used by the helpers below
    tg = _setup(c, None)
    c.fill_tail = False
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    ev = list(tg.onsets) if tg.onsets else [0.05]
    if ons is not None:
        ev = list(ons)
    ev = [t for t in ev if all(abs(t - d) > 0.02 for d in drop)]
    ev += [a[0] for a in add]
    ev = sorted(set(ev))
    lv = {t: max(rel_env_at(tg, t), floor_db) for t in ev}
    # contour of the original, used for levels and for the bed
    pk = tg.peak_t
    big = min(ev, key=lambda t: abs(t - pk)) if ev else None
    order = sorted(ev, key=lambda t: -lv[t])
    heavy = set(order[:max(1, int(len(order) * heavy_top))])
    ts = np.arange(N) / sr
    env_tex = np.zeros(N)
    for t in ev:
        amp = 10 ** (lv[t] * level_pow / 20.0)
        if kinds_at and any(abs(t - k) < 0.02 for k in kinds_at):
            kind = [v for kk, v in kinds_at.items() if abs(t - kk) < 0.02][0]
        elif t == big:
            kind = big_kind
        elif t in heavy:
            kind = "clack"
        else:
            kind = "click" if rng.random() < 0.6 else "clack"
        g = (big_gain if t == big else 1.0) * (amp_floor + (1.0 - amp_floor) * amp)
        m.add(_hit(sr, rng, kind, bright, bass, hit_ts), t, g)
        # grain texture envelope: attack at the onset, decays over ``tau`` (longer for heavy hits)
        tt = np.maximum(ts - t, 0.0)
        e = np.where(ts >= t, np.exp(-tt / (tau * (1.8 if t in heavy else 1.0))), 0.0) * amp
        env_tex = np.maximum(env_tex, e)
    # handling bed where the original is audible between clusters
    if bed:
        pts = tg.rel_env_pts()
        if pts is not None:
            rl = np.interp(ts, pts[0], pts[1])
            gate = np.clip((rl - bed_floor) / 8.0, 0.0, 1.0) * (ts < tg.dur - 0.05)
            env_tex = np.maximum(env_tex, bed * 10 ** (np.maximum(rl, -60) / 20.0) * gate)
    k = max(3, int(0.004 * sr))
    env_tex = np.convolve(env_tex, np.hanning(2 * k + 1) / np.hanning(2 * k + 1).sum(), mode="same")
    env_tex = np.where(env_tex > 10 ** (-52 / 20.0), env_tex, 0.0)  # digital silence in the gaps
    m.add(_grain_texture(N, sr, rng, tex_dens, bands=((200, 1400, 0.7), (1000, 4500, 1.0), (3500, 10500, 0.55 * bright * tex_bright))) * env_tex, 0.0, 0.8 * tex)
    for lw in lows:
        if lw[0] == "pulses":
            m.add(low_pulses(sr, rng, lw[1], lw[2], lw[3], N), 0.0, lw[4])
        else:
            m.add(low_rumble(sr, rng, lw[1], lw[2], N), 0.0, lw[3])
    for (t0, t1, f0, f1, g) in glides:
        m.add(noise_glide(sr, rng, t0, t1, min(f0, 2400.0), min(f1, 2400.0)), t0, g)
    # the originals are hard band-limited near 11 kHz (and carry nothing below 100 Hz unless they have a low bed):
    # a steep filter keeps the long-term spectrum as peaky as theirs (flatness near 0, not 0.5)
    out = sfx.fftfilt(m.out, sr, hp=None if lows else 90.0, lp=17500.0, order=6)
    if comp:
        out = _frame_comp(out, sr, comp, comp_max, comp_win)
    return out



# --------------------------------------------------------------------------- per-sound parameters
PARAMS = {'ak47_reload': {'amp_floor': 0.16999999999999998,
                 'bass': 0.0,
                 'bed': 0.54,
                 'bright': 1.2,
                 'level_pow': 0.7,
                 'lows': [['pulses', 1.62, 2.65, 9.0, 0.5]],
                 'tau': 0.25,
                 'tex': 1.2322739999999999},
 'alienblaster_reload': {'bass': 0.0,
                         'bed': 0.2,
                         'bright': 1.02,
                         'clk': 0.788,
                         'comp': 0.3,
                         'glides': [[1.5, 2.2, 700, 1100, 0.2]],
                         'level_pow': 0.5399999999999999,
                         'lows': [['rumble', 0.0, 1.5, 0.7], ['rumble', 1.5, 2.4, 0.35]],
                         'mt': 1.102,
                         'tau': 0.04875,
                         'tex': 0.3835,
                         'tex_bright': 1.337,
                         'tex_dens': 409.5},
 'ar1_reload': {'bass': 0.0, 'bright': 1.2, 'tex': 0.325, 'tex_dens': 273.0},
 'as50_reload': {'bass': 0.0,
                 'bed': 0.22,
                 'bright': 1.7999999999999998,
                 'lows': [['pulses', 0.1, 1.0, 8.0, 0.12],
                          ['pulses', 1.2, 1.9, 7.0, 0.25],
                          ['pulses', 4.2, 4.9, 6.0, 0.25]],
                 'tex': 0.75},
 'aug_reload': {'bass': 0.0, 'bright': 1.2, 'tau': 0.029499999999999995, 'tex': 0.59, 'tex_dens': 273.0},
 'biogun_reload': {'amp_floor': 0.08000000000000002,
                   'bass': 0.0,
                   'bed': 0.24,
                   'bright': 1.2,
                   'hit_ts': 0.4,
                   'level_pow': 1.1,
                   'lows': [['rumble', 1.4, 3.0, 0.15]],
                   'tau': 0.0197,
                   'tex': 0.4889625},
 'boltaction_rechamber': {'bass': 0.0, 'bright': 1.2, 'tex': 0.16666666666666666, 'tex_dens': 357.0},
 'boltaction_reload': {'bass': 0.0, 'bright': 1.2, 'hit_ts': 0.85, 'tau': 0.04875, 'tex': 0.5},
 'flamethrower_reload': {'bass': 0.0,
                         'bright': 1.2,
                         'level_pow': 0.5399999999999999,
                         'tau': 0.11249999999999999,
                         'tex': 1.5},
 'grenade_pin_b': {'bass': 0.0, 'bright': 1.1, 'comp': 0.3, 'level_pow': 0.559, 'tex': 0.3666},
 'grenadelauncher_reload': {'amp_floor': 0.53,
                            'bass': 0.0,
                            'bed': 0.6,
                            'bright': 0.78,
                            'hit_ts': 0.85,
                            'level_pow': 0.8599999999999999,
                            'lows': [['rumble', 3.5, 4.3, 0.15]],
                            'tau': 0.04875,
                            'tex': 0.425},
 'handgun_reload': {'bass': 0.0,
                    'bed': 0.6,
                    'bright': 1.2,
                    'comp': 0.3,
                    'level_pow': 1.1,
                    'lows': [['rumble', 0.0, 1.3, 0.5]],
                    'tau': 0.022894218750000004,
                    'tex': 1.3314824999999997},
 'lasergun_reload': {'bass': 0.0,
                     'bed': 0.12,
                     'bright': 1.2,
                     'glides': [[2.1, 2.9, 1400, 2300, 0.1]],
                     'hit_ts': 0.65,
                     'tex': 0.5},
 'lmg_reload': {'bass': 0.0,
                'bed': 0.06,
                'bright': 1.02,
                'lows': [['rumble', 0.0, 0.8, 0.8]],
                'tau': 0.11249999999999999,
                'tex': 0.325,
                'tex_dens': 232.04999999999998},
 'mac10_reload': {'amp_floor': 0.201,
                  'bass': 0.0,
                  'bright': 1.2,
                  'comp': 0.3,
                  'level_pow': 0.3,
                  'reseed': 1,
                  'tex': 0.325},
 'mac10_reload_akimbo': {'amp_floor': 0.95,
                         'bass': 0.0,
                         'bed': 0.0,
                         'bright': 1.2,
                         'comp': 0.3,
                         'hit_ts': 2.416,
                         'level_pow': 0.86,
                         'tau': 0.04875,
                         'tex': 0.16666666666666666,
                         'tex_bright': 1.586},
 'mib_blaster_reload': {'bass': 0.0,
                        'bed': 0.24,
                        'bright': 2.2,
                        'glides': [[1.9, 2.28, 1000, 2400, 0.6]],
                        'level_pow': 0.5399999999999998,
                        'lows': [['rumble', 1.3, 1.9, 0.12]],
                        'tau': 0.15664499999999998,
                        'tex': 0.6394124999999999,
                        'tex_dens': 1200.0},
 'mib_blaster_reload_akimbo': {'bass': 0.0,
                               'bed': 0.8,
                               'bright': 1.2,
                               'glides': [[2.6, 2.99, 1000, 2400, 0.6]],
                               'level_pow': 1.1,
                               'lows': [['rumble', 1.4, 2.5, 0.1]],
                               'tau': 0.13274999999999998,
                               'tex': 1.5},
 'minigun_reload': {'bass': 0.0,
                    'bed': 0.020000000000000004,
                    'bright': 1.2,
                    'level_pow': 0.6199999999999999,
                    'lows': [['rumble', 0.2, 1.5, 0.3], ['rumble', 4.6, 5.9, 0.5]],
                    'tex': 0.75,
                    'tex_dens': 409.5},
 'pdw_reload': {'bass': 0.0, 'bright': 0.9709, 'tex': 0.75, 'tex_dens': 313.5672},
 'pistol_reload': {'bass': 0.0, 'bed': 0.191, 'bright': 1.1453, 'tex': 0.75},
 'pistol_reload_akimbo': {'amp_floor': 0.53,
                          'bass': 0.0,
                          'bed': 0.6,
                          'bright': 1.2,
                          'hit_ts': 2.25,
                          'level_pow': 0.6199999999999999,
                          'tau': 0.13274999999999998,
                          'tex': 0.59},
 'powerhammer_reload': {'bass': 0.0,
                        'bed': 0.1,
                        'bright': 1.2,
                        'lows': [['rumble', 2.3, 3.3, 0.6], ['pulses', 1.75, 2.0, 8.0, 0.3]],
                        'tex': 0.75},
 'revolver_reload': {'amp_floor': 0.08000000000000002,
                     'bass': 0.0,
                     'bed': 0.6,
                     'bright': 0.78,
                     'hit_ts': 0.4,
                     'level_pow': 0.4599999999999998,
                     'tau': 0.19740489743987993,
                     'tex': 1.4540833199999998,
                     'tex_dens': 273.0},
 'revolver_reload_akimbo': {'bass': 0.0,
                            'bed': 0.6,
                            'bright': 1.2,
                            'hit_ts': 0.4,
                            'level_pow': 0.8599999999999999,
                            'tau': 0.13314824999999997,
                            'tex': 0.885,
                            'tex_dens': 322.14},
 'sawedoff_reload': {'bass': 0.0, 'bed': 0.2, 'bright': 1.2, 'clk': 1.47, 'comp': 0.3, 'tex': 0.5},
 'scar_reload': {'amp_floor': 0.439,
                 'bass': 0.0,
                 'bright': 0.6707,
                 'comp': 0.3,
                 'level_pow': 0.65,
                 'mt': 1.172,
                 'tex': 0.75,
                 'tex_bright': 1.249},
 'sonicshotgun_reload': {'bass': 0.0,
                         'bed': 0.06,
                         'bright': 0.8453249999999999,
                         'lows': [['rumble', 2.2, 3.8, 0.8]],
                         'tex': 0.27625,
                         'tex_dens': 100.0},
 'thompson_reload': {'bass': 0.0, 'bed': 0.06, 'bright': 1.5354, 'clk': 0.705, 'comp': 0.3, 'tau': 0.047, 'tex': 0.5},
 'vector_reload': {'amp_floor': 0.16999999999999998,
                   'bass': 0.0,
                   'bed': 0.06,
                   'bright': 2.1239999999999997,
                   'hit_ts': 1.5,
                   'level_pow': 0.3,
                   'tau': 0.04889625,
                   'tex': 0.6961999999999999}}

if os.environ.get("GG_TUNE_FILE"):  # tuning harness override (not used for the shipped files)
    with open(os.environ["GG_TUNE_FILE"], encoding="utf-8") as _f:
        PARAMS = {**PARAMS, **json.load(_f)}


def _recipe(name):
    def f(c, N):
        k = dict(PARAMS[name])
        c.rng.random(int(k.pop("reseed", 0)) + 1)  # alternate draw when a seed leaves a stray line
        return events5(c, N, **k)
    return f


for _nm in PARAMS:
    REGISTRY.insert(0, (_nm, _recipe(_nm)))
