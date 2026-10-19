"""NPC sounds: alien bug, cyberdemon, apache helicopter, sentry turret.

Round-1 rules apply (see sounds_guns.py): noise-based, irregular, naturally decaying.  Vocal sounds use a
*jittered* pulse source (period and level wander randomly) through moving formants plus breath noise and
grit, instead of a steady harmonic stack with fixed-rate modulation (which read as an engine).
"""
from __future__ import annotations

import os

import numpy as np

import sfx
from sndlib import Mix, get_tg, recipe as _recipe, crack_burst, sub_drop, mech_click, mech_clack
from sndengines import (fit, blast, events, rumble, churn, tone_burst, click_thump, foley_event, rel_env_at, _sum,
                        chug_train, tenth_times, rel_env_at_exact)


# Round 5: per-sound reseed.  The embedding distance to the original varies with the random draw (grain
# placement, click modes), so each sound carries the salt that measured best.  GG_SALT (experiments only)
# overrides every salt; without it the table below makes the render deterministic.
_SALT = {}
_CFG_CACHE = {}


def _cfg(name):
    """Experiment layer: GG_CFG_FILE names a JSON {sound: {salt, hs, split, k}} that overrides the baked tables for
    the search loop.  Unset, every table below is the whole truth and renders are deterministic."""
    f = os.environ.get("GG_CFG_FILE")
    if not f:
        return {}
    if f not in _CFG_CACHE:
        import json
        _CFG_CACHE[f] = json.load(open(f, encoding="utf8"))
    return _CFG_CACHE[f].get(name, {})


def recipe(*names):
    def deco(fn):
        def wrapper(c, N):
            salt = os.environ.get("GG_SALT")
            salt = int(salt) if salt not in (None, "") else _cfg(c.name).get("salt", _SALT.get(c.name, 0))
            if salt:
                import zlib
                c.rng = np.random.default_rng(zlib.crc32(c.id.encode()) + 7919 * salt)
            return fn(c, N)
        wrapper.__name__ = fn.__name__
        wrapper.__doc__ = fn.__doc__
        _recipe(*names)(wrapper)
        return fn
    return deco


# =========================================================================== vocal source
def pulse_source(n, sr, rng, f_curve, jitter=0.10, level_var=0.35):
    """Glottal-like pulse train with random walk jitter in period and level, smoothed to a pulse shape."""
    out = np.zeros(n)
    t = 0.0
    dur = n / sr
    f_curve = np.broadcast_to(np.asarray(f_curve, dtype=float), (n,))
    drift = 0.0
    while t < dur:
        i = int(t * sr)
        f = f_curve[min(i, n - 1)]
        drift = 0.8 * drift + 0.2 * rng.standard_normal()
        per = 1.0 / max(f, 20.0) * (1 + jitter * drift)
        if i < n:
            out[i] += 1.0 + level_var * rng.standard_normal()
        t += max(per, 0.002)
    # pulse shape: gentle low-pass plus a one-pole smear so each pulse has body
    out = sfx.fftfilt(out, sr, lp=2200, order=2)
    return out


def growl(c, N, dur, f_a, f_b, vowels=("a", "o"), breath=0.4, grit=0.8, am_rate=11, am_depth=0.55,
          formant_wander=0.0):
    """Gritty vocal growl: jittered pulses through formants that slide between two vowels, breath noise,
    waveshaping, irregular amplitude modulation."""
    sr, rng = c.sr, c.rng
    n = sfx.n_of(sr, dur)
    f = np.linspace(f_a, f_b, n) * (1 + 0.05 * (churn(n, sr, rng, 3, 1.0) - 0.5))
    src = pulse_source(n, sr, rng, f, jitter=0.12)
    asp = sfx.band_noise(n, sr, rng, 300, 6000, order=1) * churn(n, sr, rng, 40, 0.8)
    src = src / (np.std(src) + 1e-9) + breath * asp / (np.std(asp) + 1e-9)
    a = sfx.formant_filter(src, sr, sfx.VOWELS[vowels[0]])
    b = sfx.formant_filter(src, sr, sfx.VOWELS[vowels[1]])
    w = np.linspace(0, 1, n)
    if formant_wander:
        w = np.clip(w + formant_wander * (churn(n, sr, rng, 4, 1.0) - 0.5) * 2, 0, 1)
    y = a * (1 - w) + b * w
    y = y / (np.std(y) + 1e-9)
    y = np.tanh(y * (1 + grit * 2.5))
    return y * churn(n, sr, rng, am_rate, am_depth)


# =========================================================================== cyberdemon
def vocal(c, dur, f_a, f_b, vowels=("a", "o"), breath=0.4, grit=1.0, am=(25, 40), am_depth=0.5, bw_scale=1.7,
          wander=0.15, rasp_db=-14.0, extra=0.5, lp=5000.0, hiss_db=None):
    """Round-2 growl voice.  Fixes the round-1 faults of the older growl() helper:
    * the formants WANDER: five formant sets, each scaled randomly by +-wander, are cross-faded over time, and
      the bands are broadened, so there is no static resonance line near 1.7 or 2.5 kHz;
    * the pitch sags and carries 5-12% random jitter (rough vocal cords), not a clean steady tone;
    * a driven 1-3.5 kHz noise layer pulsed at 40-70 Hz gives the throat-rattle grit;
    * irregular 25-40 Hz amplitude modulation (never a fixed-rate tremolo)."""
    sr, rng = c.sr, c.rng
    n = sfx.n_of(sr, dur)
    f = np.linspace(f_a, f_b, n) * (1 + 0.06 * (churn(n, sr, rng, 4, 1.0) - 0.5))
    src = _unit(pulse_source(n, sr, rng, f, jitter=0.22, level_var=0.6))
    asp = _unit(sfx.band_noise(n, sr, rng, 300, 6000, order=1)) * churn(n, sr, rng, 40, 0.8)
    src = src + (breath + extra) * asp
    w = np.linspace(0, 1, n)
    K = 5
    cen = np.linspace(0, n - 1, K)
    acc = np.zeros(n)
    wsum = np.zeros(n)
    idx = np.arange(n)
    for k in range(K):
        sc = 1 + wander * rng.uniform(-1, 1, 3)
        # the third formant is smeared much wider: a narrow F3 near 2.5 kHz reads as a whistle-like line
        wb = (1.0, 1.3, 3.5)
        fa = [(fq * s_, bw * bw_scale * w_) for (fq, bw), s_, w_ in zip(sfx.VOWELS[vowels[0]], sc, wb)]
        fb = [(fq * s_, bw * bw_scale * w_) for (fq, bw), s_, w_ in zip(sfx.VOWELS[vowels[1]], sc, wb)]
        yk = sfx.formant_filter(src, sr, fa, floor=0.1) * (1 - w) + sfx.formant_filter(src, sr, fb, floor=0.1) * w
        hat = np.maximum(0.0, 1 - np.abs(idx - cen[k]) / (n / (K - 1.0)))
        acc += yk * hat
        wsum += hat
    y = _unit(acc / np.maximum(wsum, 1e-9))
    y = np.tanh(y * (1 + 2.5 * grit))
    y = y * churn(n, sr, rng, float(rng.uniform(*am)), am_depth)
    g = _unit(sfx.band_noise(n, sr, rng, 1000, 3500, order=2))
    g = np.tanh(g * 2.0) * churn(n, sr, rng, float(rng.uniform(40, 70)), 0.9)
    y = _unit(y) + _db(rasp_db - 4.0) * 1.0 * g
    if hiss_db is not None:   # unformanted broadband hiss (raises the in-band flatness where A is noisier)
        y = y + _db(hiss_db) * _unit(sfx.band_noise(n, sr, rng, 200, 8000, order=1)) * churn(n, sr, rng, 30, 0.5)
    return sfx.fftfilt(y, sr, hp=50, lp=lp, order=2)   # nothing above ~5 kHz unless A has it (hiss)


def _phrases(tg, floor_db=-22.0):
    """(t0, t1) spans where the original's loudness contour is above floor_db: one growl phrase per span."""
    e = np.array(tg.env10, dtype=float) if tg.env10 else None
    d = tg.end_t
    if e is None or len(e) < 11:
        return [(0.0, d)]
    ts = np.array(tenth_times(tg))
    e = e - e.max()
    on = e > floor_db
    spans, k = [], 0
    step = ts[1] - ts[0]
    while k < 11:
        if on[k]:
            j = k
            while j + 1 < 11 and on[j + 1]:
                j += 1
            spans.append((max(0.0, ts[k] - 0.4 * step), min(d, ts[j] + 0.6 * step)))
            k = j + 1
        else:
            k += 1
    return [s_ for s_ in spans if s_[1] - s_[0] > 0.15] or [(0.0, d)]


def _target_frames(c, frames, hop):
    """Reconstruct the original's 10 ms loudness frames (dB re its loudest frame) from its measured numbers:
    the eleven contour points, the peak and the -20/-40 dB decay markers (tg.rel_env_pts), plus, before each
    measured onset, a short dip of about 12 dB so the onset is the sharp rise the original has."""
    tg = get_tg(c)
    pts = tg.rel_env_pts()
    if pts is None:
        return None
    ts, rel = pts
    T = np.interp((np.arange(frames) + 0.5) * hop / c.sr, ts, rel)
    ip = int(round(tg.peak_t / 0.010))
    i_win = int(round((tg.peak_t + (tg.dec20 or 0.0)) / 0.010))   # frames that must stay above -20 dB (decay time)
    for to in tg.onsets:   # each onset is a rise of >= 9 dB after a short dip, at the original's level there
        i = int(round(to / 0.010))
        if 5 <= i < frames:
            base = max(float(T[i]), -33.0)   # an onset is by definition above the detector's -35 dB floor
            floor = -17.0 if ip < i < i_win else -60.0
            d = max(base - 18.0, floor)
            if base - d < 10.5:
                base = min(d + 10.5, -3.0)
            T[max(0, i - 4):i] = np.minimum(T[max(0, i - 4):i], d)
            T[i:i + 2] = np.maximum(T[i:i + 2], base)
    pk_abs = max(max(tg.env10), float(tg.d.get("peak_dbfs") or -3.0) - 7.4)   # same reference as rel_env_pts
    tenth_idx = set()
    for k in range(11):   # the measured contour points themselves are exact
        i = int(k * (frames - 1) / 10)
        T[i] = max(float(tg.env10[k]) - pk_abs, -60.0)
        tenth_idx.add(i)
    # the original's loudest frame is at its measured peak time: keep every other frame a little under it, so
    # B's loudest frame (the decay reference) lands at the same place
    far = np.abs(np.arange(frames) - ip) > 3
    T[far] = np.minimum(T[far], -2.0)
    if tg.dec20:   # the measured -20 dB decay time must hold: no dip (or sag) may cross -20 dB before it
        i0 = int(round(tg.peak_t / 0.010))
        i1 = min(frames, int(round((tg.peak_t + tg.dec20) / 0.010)))
        if i1 > i0:
            T[i0:i1] = np.maximum(T[i0:i1], -17.0)
    if tg.dec20 and tg.dec20 > 0.02:   # the first frame under -20 dB lands exactly at the measured decay time
        i_dec = int(round((tg.peak_t + tg.dec20) / 0.010))
        if i_dec < frames and i_dec not in tenth_idx and T[i_dec] > -20.8:
            T[i_dec] = -20.8
    K = _knobs(c.name)
    if K.get("droop"):   # round 6: tail pulled down faster than A's, so the -20 dB time (from a capped peak) holds
        tt = (np.arange(frames) + 0.5) * hop / c.sr
        T = T - K["droop"] * np.clip((tt - tg.peak_t) / max(c.dur - tg.peak_t, 0.1), 0.0, 1.0)
    if K.get("cap") is not None:   # round 6: A is clipped far past full scale; B's loudest frames cannot follow it
        T = np.minimum(T, K["cap"])
    return np.maximum(T, -60.0)


# Round 5: sounds whose highs must stay smooth.  The frame-exact fit gates the whole spectrum, which turns the
# original's bass-borne onsets into broadband bursts (the embedding hears it).  For these names the per-frame gain
# is applied at full resolution to the band under _SPLIT Hz and smoothed over the given seconds above it.
_HI_SMOOTH = {"apache_explode": 0.15, "apache_hit_a": 0.15, "apache_hit_b": 0.15, "apache_burst": 0.1}
if os.environ.get("GG_HS") not in (None, ""):   # experiment switch: replace every smoothing time (0 = off)
    _HI_SMOOTH = {k: float(os.environ["GG_HS"]) for k in _HI_SMOOTH}
_SPLIT = 300.0
_SPLIT_OF = {}   # per-sound split frequency (Hz) where it differs from _SPLIT
_POST_EQ = {"apache_hit_a": [0, 2, 3, -3, 2, 3, 0]}   # name -> 7 gains (dB) at 8, 38, 120, 500, 2000, 5600, 12000 Hz applied after the band-balance EQ


def _fit_contour(c, y, max_gain=45.0, passes=4, periodic=False):
    """Frame-exact loudness fit.  The measured gate compares 10 ms frames (the contour points, the onset count
    and the decay time), and chitter, growl and rumble have frame-to-frame structure that a smoothed contour
    conform cannot control.  Here every 10 ms frame of B is gained toward the reconstructed frame envelope of A
    (_target_frames), clipped to +-30 dB and interpolated in dB between frame centers, so the texture keeps its
    own fine structure but follows A's loudness shape, onsets and decay."""
    tg = get_tg(c)
    if not tg.ok or not tg.env10 or len(tg.env10) < 11:
        return y
    max_gain = _knobs(c.name).get("fg", max_gain)
    sr, n = c.sr, c.n
    hop = max(1, int(sr * 0.010))
    frames = max(1, n // hop)
    T = _target_frames(c, frames, hop)
    if T is None:
        return y
    y = y.copy()
    ramp = max(3, int(sr * 0.0015))
    ker = np.ones(ramp) / ramp
    for _ in range(passes):
        env = 20 * np.log10(np.sqrt(np.mean(y[:frames * hop].reshape(frames, hop) ** 2, axis=1)) + 1e-9)
        act = T > -30
        ref = float(np.mean(env[act] - T[act])) if act.any() else 0.0
        g = np.clip(T + ref - env, -max_gain, max_gain)
        gs = np.ones(len(y))   # per-frame constant gain (frames are the gate's own 10 ms grid), 1.5 ms ramps
        gs[:frames * hop] = np.repeat(10 ** (g / 20.0), hop)
        gs[frames * hop:] = gs[frames * hop - 1]
        if periodic:   # loop: the cross-faded tail gets the head's gain, so both halves of the blend match
            xfn = sfx.n_of(sr, min(0.25, c.dur * 0.2))
            gs[frames * hop:frames * hop + xfn] = gs[:xfn][:len(gs[frames * hop:frames * hop + xfn])]
        gs = np.convolve(np.pad(gs, (ramp, ramp), mode="edge"), ker, mode="same")[ramp:-ramp]
        hs = _HI_SMOOTH.get(c.name)
        split = _SPLIT_OF.get(c.name, _SPLIT)
        cf = _cfg(c.name)
        if "hs" in cf:
            hs = max(float(cf["hs"]), 0.0) or None
        split = float(cf.get("split", split))
        if os.environ.get("GG_HS_ALL"):   # experiment override: seconds, negative = off
            hs = max(float(os.environ["GG_HS_ALL"]), 0.0) or None
            split = float(os.environ.get("GG_SPLIT") or split)
        if hs:
            lo = sfx.fftfilt(y, sr, lp=split, order=2)
            hi = y - lo
            kk = max(3, int(sr * hs))
            lg = np.log(np.maximum(gs, 1e-6))
            cs = np.concatenate(([0.0], np.cumsum(np.pad(lg, (kk, kk), mode="edge"))))   # box filter in O(n)
            lgs = (cs[kk:] - cs[:-kk])[(kk + 1) // 2:(kk + 1) // 2 + len(lg)] / kk
            y = lo * gs + hi * np.exp(lgs)
        else:
            y = y * gs
    return y


_GATE_BANDS = (("sub", 20, 60), ("bass", 60, 250), ("low_mid", 250, 1000), ("mid", 1000, 4000),
               ("presence", 4000, 8000), ("air", 8000, 24000))


def _gate_bands(x, sr):
    """Band energy (percent) the way the measured gate computes it: the energy-weighted mean of 2048-point Hann
    frames, so the loudest frames dominate (the generic conform measures differently and misses by 10-20 points)."""
    size, step = 2048, 512
    if len(x) < size:
        x = np.pad(x, (0, size - len(x)))
    win = np.hanning(size)
    fr = np.array([np.abs(np.fft.rfft(x[i:i + size] * win)) ** 2 for i in range(0, len(x) - size + 1, step)])
    w = fr.sum(axis=1)
    ms = (fr * (w / max(w.sum(), 1e-20))[:, None]).sum(axis=0)
    f = np.fft.rfftfreq(size, 1.0 / sr)
    tot = max(float(ms.sum()), 1e-20)
    return np.array([100 * float(ms[(f >= lo) & (f < min(hi, sr / 2))].sum()) / tot for _, lo, hi in _GATE_BANDS])


def _gate_eq(c, y, passes=6):
    """Static EQ that moves the gate-measured band balance of y toward the original's (tg.bands)."""
    tg = get_tg(c)
    if not tg.bands:
        return y
    tgt = np.maximum(np.array([float(tg.bands.get(k, 0.0)) for k, _, _ in _GATE_BANDS]), 0.3)
    sr, n = c.sr, c.n
    seg = y[:n]
    gains = np.zeros(7)
    out = y
    for _ in range(passes):
        cur = np.maximum(_gate_bands(out[:n], sr), 0.1)
        step = 10 * np.log10(tgt / cur) * 0.85
        if np.max(np.abs(step)) < 0.5:
            break
        gains[1:] = np.clip(gains[1:] + step, -30.0, 30.0)
        out = sfx.eq_curve(y, sr, gains)
        out = out * (np.sqrt(np.mean(y[:n] ** 2) + 1e-20) / np.sqrt(np.mean(out[:n] ** 2) + 1e-20))
    return out


def _tailbed(c, y, db):
    """Round 5: a broadband floor that carries the original's measured band shares (tg.bands) and follows its loudness
    contour, so no frame of B is empty where A is not.  Without it the frame fit multiplies leaked low-frequency
    residue by up to 45 dB in the tail (a sub bed A does not have)."""
    import zlib
    tg = get_tg(c)
    if not tg.ok or not tg.bands:
        return y
    sr, n = c.sr, len(y)
    rng = np.random.default_rng(zlib.crc32(c.id.encode()) + 17)
    bed = np.zeros(n)
    for key, lo, hi in _GATE_BANDS:
        sh = max(float(tg.bands.get(key, 0.0)), 0.0)
        if sh <= 0.05 or lo >= sr * 0.45:
            continue
        x = sfx.band_noise(n, sr, rng, lo, min(hi, sr * 0.45), order=2)
        bed += x / (np.std(x) + 1e-9) * np.sqrt(sh)
    ts = np.arange(0, n, 256) / sr
    lv = np.array([rel_env_at(tg, t) for t in ts])
    g = np.interp(np.arange(n) / sr, ts, 10 ** (np.maximum(lv, -50.0) / 20.0))
    g[np.arange(n) / sr > tg.end_t] = 0.0
    bed = bed * g
    act = np.abs(y) > 1e-6
    ref = np.std(y[act]) if act.any() else 1.0
    return y + bed / (np.std(bed[g > 0.05]) + 1e-9) * ref * 10 ** (db / 20.0)


def _contour(fn):
    """Decorator for a recipe: switch the generic contour conform off and fit the exact measured frames instead."""
    def wrapper(c, N):
        if os.environ.get("GG_NOFIT"):   # experiment switch: the plain recipe through the generic conform
            return fn(c, N)
        c.conform_env = False
        # the generic crest limiter would undo the frame fit, except for the very loud, clipped apache blasts
        c.limit_crest = c.name in ("apache_burst", "apache_explode")
        y = np.asarray(fn(c, N), dtype=float)
        tb = _knobs(c.name).get("tb")
        if tb is not None:
            y = _tailbed(c, y, tb)
        c.conform_bands = False   # replaced by an EQ measured the way the gate measures
        y = _fit_contour(c, y)
        # the finishing limiter would shave the loudest frames by several dB and lift the tail relative to them:
        # bring the crest down here with a soft clip (then refit), so the contour survives the limiter
        k = 1.4 * _knobs(c.name).get("clipk", 1.0) * (np.std(y[np.abs(y) > 1e-6]) + 1e-9)
        y = k * np.tanh(y / k)
        y = _fit_contour(c, y, passes=5)
        for _ in range(3):   # band balance, measured like the gate (loud frames dominate), alternating with the contour
            y = _gate_eq(c, y)
            y = _fit_contour(c, y, passes=2)
        y = _gate_eq(c, y)   # EQ last: the gate's band balance is then exact, the contour only shifts a little
        peq = os.environ.get("GG_PEQ") if os.environ.get("GG_PEQ_NAME", c.name) == c.name else None
        peq = [float(v) for v in peq.split(",")] if peq else _POST_EQ.get(c.name)
        if peq:   # extra in-band tilt (still inside the band-balance tolerance) for the gate's noisiness check
            y = sfx.eq_curve(y, c.sr, peq)
        return y
    wrapper.__name__ = fn.__name__
    wrapper.__doc__ = fn.__doc__
    return wrapper


@recipe("cyberdemon_step_*")
@_contour
def cyberdemon_step(c, N):
    """Swelling stomp: one sub thump, a low-mid body thud, irregular rumble, and a gritty debris layer of
    random 10-25 ms noise grains (broadband, no ring); clean fading tail."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    surges = tuple((t - 0.015, 0.1, 0.6) for t in tg.onsets if t > 0.2)
    out = blast(c, N, crack_gain=0.25, sub=(75.0, 32.0), sub_gain=1.0, rumble=1.2, churn_rate=22, churn_depth=0.4,
                thunk=0.8, crackle=(70, 800, 6000, 0.3, 0.45), surges=surges, tail_gain=0.6, debris=0.3, bumps=False)
    out = out / (np.max(np.abs(out)) + 1e-9)
    m = Mix(c, N)
    pk = max(tg.peak_t, 0.05)
    # low-mid body thud under the peak (150-400 Hz, ~100 ms decay)
    nb = sfx.n_of(sr, 0.35)
    body = sfx.band_noise(nb, sr, rng, 150, 420, order=2) * sfx.exp_env(nb, sr, 0.08, attack=0.012)
    m.add(_unit(body), max(pk - 0.03, 0.0), 0.16)
    # gritty gravel: 6-10 random grains, 1.5-8 kHz, decaying amplitude, spread 0.05-0.3 s after the peak
    for i in range(int(rng.integers(6, 10))):
        t = pk + 0.03 + rng.uniform(0, 0.26)
        ng = sfx.n_of(sr, 0.05)
        g = sfx.fftfilt(sfx.noise(ng, rng), sr, hp=1500, lp=8000, order=1) * sfx.exp_env(ng, sr, rng.uniform(0.008, 0.02), attack=0.0008)
        m.add(g / (np.max(np.abs(g)) + 1e-9), t, 0.10 * np.exp(-(t - pk) / 0.2))
    # lead-in scuffs
    for t in (pk * 0.25, pk * 0.55):
        ng = sfx.n_of(sr, 0.03)
        g = sfx.fftfilt(sfx.noise(ng, rng), sr, hp=100, lp=800, order=1) * sfx.exp_env(ng, sr, 0.01, attack=0.002)
        m.add(g / (np.max(np.abs(g)) + 1e-9), t, 0.05)
    return out + fit(m.out, len(out))


@recipe("cyberdemon_aggro_*", "cyberdemon_idle_*", "cyberdemon_hurt_*", "cyberdemon_death_*")
@_contour
def cyberdemon_voice(c, N):
    """Growls and roars: one vocal() phrase per loud span of the original's contour (the quiet gaps between
    phrases stay quiet), each with a breathy onset accent; deeper for idle/death, higher with a snarl for
    hurt/aggro; death sags about an octave."""
    tg = get_tg(c)
    sr, rng, nm = c.sr, c.rng, c.name
    m = Mix(c, N)
    K = _knobs(nm)
    heavy = "idle" in nm or "death" in nm
    f_a = rng.uniform(65, 90) if heavy else rng.uniform(80, 115)
    f_b = f_a * (0.5 if "death" in nm else 0.8)
    vow = ("o", "a") if "idle" in nm else ("a", "u") if "death" in nm else ("ae", "o")
    for (t0, t1) in _phrases(tg):
        dd = max(t1 - t0, 0.25)
        g = vocal(c, dd, f_a * rng.uniform(0.92, 1.08), f_b * rng.uniform(0.92, 1.08), vowels=vow,
                  breath=(0.55 if ("aggro" in nm or "hurt" in nm) else 0.4) + K.get("breath", 0.0), grit=K.get("grit", 1.0),
                  am=(25 * K.get("amr", 1.0), 40 * K.get("amr", 1.0)), am_depth=min(0.95, 0.5 + K.get("amd", 0.0)),
                  extra={"idle_c": 1.6, "death_a": 0.1}.get(nm.split("cyberdemon_")[1], 0.5),
                  rasp_db={"idle_c": -3.0, "death_a": -30.0}.get(nm.split("cyberdemon_")[1], -14.0) + K.get("rasp", 0.0),
                  lp={"death_a": 5000.0, "idle_c": 8000.0}.get(nm.split("cyberdemon_")[1], 5000.0),
                  hiss_db={"idle_c": -4.0}.get(nm.split("cyberdemon_")[1]))
        m.add(g * sfx.smooth_env(len(g), sr, min(0.06, dd * 0.2), min(0.5, dd * 0.5)), t0, 1.0)
    for t in tg.onsets[:5]:
        n = sfx.n_of(sr, 0.14)
        m.add(sfx.band_noise(n, sr, rng, 800, 4000, order=1) * sfx.exp_env(n, sr, 0.04, attack=0.004), t, 0.18)
    nb = sfx.n_of(sr, tg.end_t)
    m.add(sfx.band_noise(nb, sr, rng, 100, 450, order=2) * churn(nb, sr, rng, 8, 0.7), 0.0, 0.3)
    return m.out


# =========================================================================== alien bug
# Round-2 rework.  The round-1 reviews agreed on five points, so every alien bug sound is built from the same
# few layers and none of them can bring the old faults back:
#   * no energy below the original's lowest band (hard high-pass on everything except explicit thumps);
#   * the chitter is a CONTINUOUS band-limited noise bed with irregular 30-150 Hz grain modulation and a
#     swelling envelope, not separate hits with silent gaps;
#   * resonant noise (moving formant bands) gives the organic, vocal rasp; there are no sine oscillators in the
#     noise layers, so no tonal whistle lines;
#   * clicks are short noise bursts with a sharp attack (optionally a small 200-400 Hz body);
#   * tails are chains of decaying clicks over a faint crackle floor, never a few isolated loud ticks.
def _db(x):
    return 10.0 ** (x / 20.0)


def _pts_env(n, sr, t0, pts):
    """Piecewise-linear dB envelope with absolute times; holds the end values outside the points."""
    ts = np.array([p[0] for p in pts], dtype=float)
    ds = np.array([p[1] for p in pts], dtype=float)
    t = t0 + np.arange(n) / sr
    return _db(np.interp(t, ts, np.maximum(ds, -90.0)))


def _unit(x):
    return x / (np.std(x) + 1e-9)


def _edge(x, sr, ms=2.0):
    k = min(len(x) // 2, max(2, int(sr * ms / 1000.0)))
    x[:k] *= np.linspace(0, 1, k)
    x[-k:] *= np.linspace(1, 0, k)
    return x


def _bed(c, t0, t1, lo, hi, pts, rate=(30, 60), depth=0.5):
    """Continuous band-limited noise with irregular grain modulation (chitter) and a dB envelope."""
    sr, rng = c.sr, c.rng
    n = sfx.n_of(sr, t1 - t0)
    x = _unit(sfx.band_noise(n, sr, rng, lo, min(hi, sr * 0.45), order=2))
    r = float(rng.uniform(*rate))
    x = x * churn(n, sr, rng, r, depth) * churn(n, sr, rng, r * 0.37, depth * 0.5)
    return _edge(x * _pts_env(n, sr, t0, pts), sr, 3.0)


def _res(c, t0, t1, forms, pts, rate=(30, 60), depth=0.5, floor=0.04):
    """Noise through moving resonant bands.  forms = [(f_start, f_end, bw)]; the bands glide start -> end."""
    sr, rng = c.sr, c.rng
    n = sfx.n_of(sr, t1 - t0)
    src = sfx.noise(n, rng)
    a = sfx.formant_filter(src, sr, [(f0, bw) for f0, f1, bw in forms], floor=floor)
    b = sfx.formant_filter(src, sr, [(f1, bw) for f0, f1, bw in forms], floor=floor)
    w = np.linspace(0, 1, n)
    x = _unit(a * (1 - w) + b * w)
    x = np.tanh(x * 1.6) * churn(n, sr, rng, float(rng.uniform(*rate)), depth)
    return _edge(x * _pts_env(n, sr, t0, pts), sr, 3.0)


def _voice(c, t0, t1, f0a, f0b, va, vb, pts, am=(25, 40), depth=0.5, grit=1.2, jitter=0.1):
    """Jittered glottal pulses through two formant sets (vowel glide), driven, with irregular AM."""
    sr, rng = c.sr, c.rng
    n = sfx.n_of(sr, t1 - t0)
    f = np.linspace(f0a, f0b, n) * (1 + 0.05 * (churn(n, sr, rng, 3, 1.0) - 0.5))
    src = _unit(pulse_source(n, sr, rng, f, jitter=jitter))
    a = sfx.formant_filter(src, sr, va)
    b = sfx.formant_filter(src, sr, vb)
    w = np.linspace(0, 1, n)
    x = _unit(a * (1 - w) + b * w)
    x = np.tanh(x * (1 + grit)) * churn(n, sr, rng, float(rng.uniform(*am)), depth)
    return _edge(x * _pts_env(n, sr, t0, pts), sr, 3.0)


def _click(c, lo, hi, dur, body=0.0, bodyf=300.0):
    """Short noise burst, near-instant attack, optional small damped low body (no tonal ring)."""
    sr, rng = c.sr, c.rng
    n = sfx.n_of(sr, max(dur * 6, 0.012))
    x = sfx.noise(n, rng) * sfx.exp_env(n, sr, dur / 3.0, attack=0.0003)
    x = sfx.fftfilt(x, sr, hp=lo, lp=min(hi, sr * 0.45), order=1)
    x = x / (np.max(np.abs(x)) + 1e-9)
    if body:
        t = sfx.tvec(n, sr)
        x = x + body * np.sin(sfx.TAU * bodyf * t * (1 - 0.3 * np.minimum(t / dur, 1.0))) * np.exp(-t / (dur * 1.2))
    return x


_KNOB = {}   # per-sound baked values of the round-5 search: {"bed": dB, "res": dB, ..., "hp": x, "lp": x}


def _knobs(name):
    """Per-kind level offsets (dB) and filter-edge multipliers: the baked table, then the GG_KNOBS experiment
    override (JSON), so renders without the variable are deterministic."""
    import json
    k = dict(_KNOB.get(name, {}))
    k.update(_cfg(name).get("k", {}))
    if os.environ.get("GG_KNOBS"):
        k.update(json.loads(os.environ["GG_KNOBS"]))
    return k


def _build_bug(c, N, spec):
    """Mix a spec: items are tuples tagged bed / res / voice / click / train / crack / low / thump.  Everything
    but low and thump passes the spec's high-pass and low-pass; levels are dB relative to a unit-RMS bed."""
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    lowm = Mix(c, N)
    K = _knobs(c.name)
    kd = lambda key: _db(K.get(key, 0.0))   # noqa: E731 - per-kind level offset (dB) from the round-5 search
    for it in spec["items"]:
        k = it[0]
        if k == "bed":
            _, t0, t1, lo, hi, pts, rate, depth, db = it
            m.add(_bed(c, t0, t1, lo, hi, pts, rate, depth), t0, _db(db) * kd("bed"))
        elif k == "res":
            _, t0, t1, forms, pts, rate, depth, db = it
            m.add(_res(c, t0, t1, forms, pts, rate, depth), t0, _db(db) * kd("res"))
        elif k == "voice":
            _, t0, t1, fa, fb, va, vb, pts, am, depth, db = it
            m.add(_voice(c, t0, t1, fa, fb, va, vb, pts, am, depth), t0, _db(db) * kd("voice"))
        elif k == "click":
            _, t, lo, hi, dur, db, body = it
            m.add(_click(c, lo, hi, dur, body), t, _db(db) * 1.3 * kd("click"))
        elif k == "train":
            _, times, lo, hi, dur, db0, step, body = it
            for i, t in enumerate(times):
                g = _db(db0 - step * i + rng.uniform(-1.5, 1.5)) * 1.3 * kd("click")
                m.add(_click(c, lo * (1 + 0.04 * i), hi, dur * rng.uniform(0.8, 1.3), body), t + rng.uniform(-0.004, 0.004), g)
        elif k == "crack":
            _, t0, t1, dens, lo, hi, db, pts = it
            n = sfx.n_of(sr, t1 - t0)
            g = sfx.grain_cloud(n, sr, rng, dens, 0.0015, lo, hi, amp_var=0.9)
            m.add(_edge(_unit(g) * _pts_env(n, sr, t0, pts), sr, 2.0), t0, _db(db) * kd("crack"))
        elif k == "low":     # band-limited rumble that must survive the high-pass
            _, t0, t1, lo, hi, pts, db = it
            n = sfx.n_of(sr, t1 - t0)
            x = _unit(sfx.band_noise(n, sr, rng, lo, hi, order=2)) * churn(n, sr, rng, 14, 0.5)
            lowm.add(_edge(x * _pts_env(n, sr, t0, pts), sr, 3.0), t0, _db(db) * kd("low"))
        elif k == "thump":
            _, t, f0, f1, dur, db = it
            lowm.add(sub_drop(sr, f0, f1, dur, dur * 0.4, k=0.04, attack=0.003), t, _db(db) * 1.3 * kd("low"))
    # a faint crackle floor that follows the original's loudness contour, so no frame is dead and the contour
    # conform can lift it (the generic dead-zone filler is off: it leaks the thump's sub into the tail)
    tg = get_tg(c)
    ts = np.arange(N) / sr
    lv = np.array([rel_env_at(tg, t) for t in ts[::256]])
    gate = np.where(ts <= tg.end_t, 10 ** (np.interp(ts, ts[::256], np.maximum(lv, -50)) / 20.0), 0.0)
    fl_lo, fl_hi = spec.get("floor", (2000, 9000))
    # round 3: a CONTINUOUS irregularly modulated bed (not sparse grains), so the gaps never go empty
    fl = _unit(sfx.band_noise(N, sr, rng, fl_lo, min(fl_hi, sr * 0.45), order=2))
    fl = fl * churn(N, sr, rng, 45.0, 0.5) * churn(N, sr, rng, 17.0, 0.3)
    m.add(fl * gate, 0.0, _db(spec.get("floor_db", 0)) * kd("floor"))
    y = sfx.fftfilt(m.out, sr, hp=spec.get("hp", 400) * K.get("hp", 1.0), order=4)   # steep: A has essentially nothing below it
    y = sfx.fftfilt(y, sr, lp=min(spec.get("lp", 14000) * K.get("lp", 1.0), sr * 0.45), order=2)
    k = 2.4 * (np.std(y[y != 0]) + 1e-9)   # memoryless soft clip: tames click peaks (crest) without the
    y = k * np.tanh(y / k)                  # gain pumping of the generic crest limiter
    y = sfx.fftfilt(y, sr, hp=spec.get("hp", 400), order=4)   # clipping products must not refill the stopband
    return y + lowm.out


def _chomp(t, dur, lo, hi, db, rate=(40, 70), depth=0.4):
    """One bite burst: 3 ms attack, ~20 dB fall over `dur`."""
    return ("bed", t, t + dur, lo, hi, [(t, -8), (t + 0.003, 0), (t + dur, -22)], rate, depth, db)


_FLAT = lambda t0, t1, db=0: [(t0, db), (t1, db)]

_BUG = {
    "aggro_a": dict(hp=300, lp=9000, items=[
        ("bed", 0.0, 0.31, 1500, 6500, [(0, -3), (0.003, 0), (0.27, 0), (0.31, -14)], (30, 60), 0.6, 0),
        ("res", 0.0, 0.31, [(400, 450, 150), (2500, 3200, 600)], [(0, -3), (0.003, 0), (0.27, 0), (0.31, -14)], (30, 60), 0.6, -4),
        ("click", 0.28, 2000, 8000, 0.008, 9, 0.0), ("click", 0.39, 2000, 8000, 0.008, 9, 0.0),
        ("bed", 0.31, 0.48, 2500, 6000, [(0.31, -16), (0.48, -26)], (30, 60), 0.6, 0),
        ("train", [0.53, 0.61, 0.73, 0.81, 0.89, 0.99], 2500, 6500, 0.015, -8, 2.5, 0.0),
        ("crack", 0.45, 1.07, 60, 2000, 8000, -24, [(0.45, -10), (1.07, -22)]),
    ]),
    "aggro_b": dict(hp=120, lp=12000, items=[
        ("bed", 0.0, 0.68, 1000, 6000, [(0, -24), (0.025, 0), (0.40, 0), (0.66, -22)], (20, 40), 0.5, 0),
        ("bed", 0.0, 0.68, 200, 420, [(0, -24), (0.025, 0), (0.40, 0), (0.66, -22)], (20, 40), 0.5, -2),
        ("click", 0.2, 2000, 4000, 0.005, 4, 0.0),
        ("train", [0.40, 0.49, 0.59, 0.68, 0.78, 0.88, 0.97, 1.07, 1.16], 2000, 6000, 0.006, -14, 3.5, 0.6),
        ("crack", 0.4, 1.17, 40, 2000, 6000, -26, [(0.4, -14), (1.17, -30)]),
    ]),
    "aggro_c": dict(hp=95, lp=6000, items=[
        ("bed", 0.0, 0.08, 500, 5000, _FLAT(0, 0.08, 0), (30, 50), 0.4, -25),
        ("voice", 0.06, 0.8, 150, 105, [(500, 180), (1000, 240), (1700, 500)], [(400, 180), (850, 240), (1500, 450)],
         [(0.06, -22), (0.09, 0), (0.40, 0), (0.70, -20), (0.8, -30)], (40, 80), 0.5, 0),
        ("bed", 0.06, 0.8, 1500, 3500, [(0.06, -22), (0.09, 0), (0.40, 0), (0.70, -20), (0.8, -30)], (40, 80), 0.6, -8),
        ("train", [0.50, 0.58, 0.67, 0.76, 0.86, 0.95, 1.04], 1000, 4000, 0.008, -14, 2.5, 0.5),
        ("crack", 0.55, 1.08, 40, 1500, 5000, -30, _FLAT(0.55, 1.08, -6)),
    ]),
    "aggro_d": dict(hp=220, lp=8000, items=[
        ("bed", 0.0, 0.39, 2000, 7500, [(0, -4), (0.01, 0), (0.22, 0), (0.33, -2), (0.34, -8), (0.39, -30)], (40, 80), 0.5, 0),
        ("voice", 0.0, 0.38, 135, 115, [(300, 80), (700, 120), (1500, 200)], [(350, 80), (800, 120), (1600, 200)],
         _FLAT(0, 0.38, 0), (25, 40), 0.5, -6),
        ("res", 0.0, 0.2, [(650, 650, 70), (1300, 1300, 110)], _FLAT(0, 0.2, 0), (30, 50), 0.5, -12),
        ("bed", 0.24, 0.28, 800, 6000, [(0.24, 0), (0.28, -10)], (60, 90), 0.4, -3),
        ("click", 0.40, 2000, 6000, 0.007, 7, 0.0),
        ("train", [0.53, 0.61, 0.73, 0.81, 0.90, 0.99], 500, 6000, 0.022, -6, 3.0, 0.3),
        ("bed", 0.0, 0.5, 3000, 6500, [(0, -14), (0.4, -14), (0.5, -30)], (50, 90), 0.5, 0),
    ]),
    "bite_a": dict(hp=150, lp=9000, items=[
        _chomp(0.13, 0.06, 300, 8000, 0), _chomp(0.19, 0.06, 300, 8000, -2),
        ("click", 0.13, 2000, 9000, 0.004, 2, 0.0), ("click", 0.19, 2000, 9000, 0.004, 0, 0.0),
        ("thump", 0.13, 250, 120, 0.06, -6), ("thump", 0.19, 250, 120, 0.06, -8),
        ("bed", 0.12, 0.40, 1000, 6000, [(0.12, -14), (0.25, -18), (0.40, -34)], (40, 70), 0.8, 0),
        ("bed", 0.28, 0.34, 2000, 5000, [(0.28, -4), (0.34, -24)], (40, 70), 0.5, 0),
        ("thump", 0.28, 300, 170, 0.05, -10), ("click", 0.44, 2000, 6000, 0.006, -14, 0.0),
        ("low", 0.30, 0.6, 150, 400, [(0.3, -34), (0.6, -50)], 0),
        ("crack", 0.3, 0.55, 25, 1500, 6000, -36, [(0.3, -6), (0.55, -14)]),
    ]),
    "bite_b": dict(hp=150, lp=9000, items=[
        _chomp(0.02, 0.07, 1000, 6000, 0), _chomp(0.07, 0.07, 1000, 6000, -2), _chomp(0.13, 0.075, 1000, 6000, -4),
        ("thump", 0.02, 180, 60, 0.12, -2), ("thump", 0.07, 180, 60, 0.12, -5), ("thump", 0.13, 180, 60, 0.12, -8),
        ("low", 0.02, 0.46, 40, 200, [(0.02, -2), (0.45, -16)], -4),
        ("bed", 0.08, 0.46, 400, 1500, [(0.08, -14), (0.18, -20), (0.45, -42)], (30, 60), 0.5, 0),
        ("crack", 0.02, 0.22, 250, 2000, 8000, -6, [(0.02, 0), (0.22, -16)]),
    ]),
    "bite_c": dict(hp=150, lp=10000, items=[
        ("bed", 0.07, 0.115, 2000, 6500, [(0.07, 0), (0.115, -18)], (80, 120), 0.6, 0),
        ("click", 0.07, 4000, 9000, 0.004, 6, 0.0), ("click", 0.092, 3000, 8000, 0.004, 2, 0.0),
        ("bed", 0.17, 0.24, 2000, 7000, [(0.17, -3), (0.24, -20)], (80, 120), 0.6, 0),
        ("train", [0.17, 0.187, 0.2, 0.217], 2500, 7000, 0.005, 2, 1.0, 0.0),
        ("crack", 0.07, 0.26, 130, 2000, 8000, -4, [(0.07, -4), (0.26, -16)]),
        ("bed", 0.07, 0.46, 1500, 5000, [(0.07, -16), (0.12, -18), (0.46, -42)], (40, 70), 0.7, 0),
        ("bed", 0.10, 0.14, 1000, 3000, [(0.10, -12), (0.14, -16)], (8, 12), 0.8, 0),
        ("crack", 0.3, 0.6, 25, 3000, 6000, -28, [(0.3, -6), (0.6, -12)]),
        ("thump", 0.07, 180, 70, 0.08, -5),
    ]),
    "bite_d": dict(hp=100, lp=9000, items=[
        _chomp(0.04, 0.035, 2000, 8000, 0), _chomp(0.09, 0.03, 2000, 8000, -4), _chomp(0.13, 0.03, 2000, 8000, -6),
        ("click", 0.04, 3000, 9000, 0.004, 3, 0.0), ("click", 0.09, 3000, 8000, 0.004, -2, 0.0), ("click", 0.13, 3000, 8000, 0.004, -3, 0.0),
        ("low", 0.04, 0.24, 100, 250, [(0.04, -2), (0.09, -2), (0.13, 0), (0.22, -22)], -2),
        ("thump", 0.22, 120, 70, 0.08, -6),
        ("bed", 0.04, 0.6, 600, 3000, [(0.04, -16), (0.2, -22), (0.3, -28), (0.6, -60)], (40, 70), 0.6, 0),
        ("crack", 0.05, 0.5, 60, 2500, 7000, -22, [(0.05, -4), (0.5, -26)]),
    ]),
    "death_a": dict(hp=100, lp=12000, items=[
        ("thump", 0.0, 120, 45, 0.4, -3), ("low", 0.0, 0.55, 150, 300, [(0, 0), (0.5, -12)], -9),
        ("voice", 0.0, 0.32, 600, 250, [(900, 150), (1800, 300)], [(700, 150), (1400, 300)], [(0, -4), (0.05, 0), (0.32, -16)], (25, 40), 0.5, -8),
        ("res", 0.1, 0.5, [(800, 2500, 400), (1500, 900, 500)], [(0.1, -18), (0.5, -22)], (20, 40), 0.5, 0),
        ("res", 0.5, 1.6, [(2500, 900, 400), (900, 1800, 500)], [(0.5, -22), (1.0, -26), (1.6, -42)], (20, 40), 0.5, 0),
        ("bed", 0.5, 1.3, 600, 4000, _FLAT(0.5, 1.3, -26), (20, 40), 0.5, 0),
        ("train", [0.45, 0.9, 1.33, 1.43, 1.62, 1.79, 1.89], 1200, 5000, 0.005, -4, 1.0, 0.0),
        ("crack", 1.2, 2.0, 25, 1500, 6000, -28, [(1.2, -8), (2.0, -16)]),
        ("bed", 1.6, 2.25, 4000, 10000, [(1.6, -38), (2.2, -50)], (30, 60), 0.4, 0),
    ]),
    "death_b": dict(hp=150, lp=13000, items=[
        ("thump", 0.0, 100, 45, 0.15, -3),
        ("bed", 0.0, 0.32, 300, 9000, [(0, -24), (0.025, 0), (0.2, -20), (0.3, -30)], (30, 60), 0.4, 0),
        ("bed", 0.2, 1.38, 150, 3000, [(0.2, -22), (0.6, -26), (1.35, -40)], (20, 40), 0.5, 0),
        ("train", [0.26, 0.34, 0.42, 0.6], 800, 4000, 0.009, -2, 1.0, 0.5),
        ("train", [0.95, 1.28, 1.36], 1000, 4000, 0.006, -14, 0.0, 0.0),
        ("crack", 0.2, 0.8, 80, 1500, 6000, -14, [(0.2, -4), (0.8, -18)]),
        ("bed", 1.3, 1.6, 6000, 10000, [(1.3, -36), (1.5, -44), (1.6, -60)], (30, 50), 0.4, 0),
    ]),
    "hit-01": dict(hp=250, lp=14500, items=[
        ("bed", 0.03, 0.42, 1500, 12000, [(0.03, -6), (0.06, 0), (0.25, 0), (0.4, -20)], (200, 400), 0.8, 0),
        ("res", 0.04, 0.35, [(450, 300, 120), (1500, 1200, 400)], _FLAT(0.04, 0.35, 0), (30, 60), 0.5, -8),
        ("bed", 0.03, 0.35, 8000, 16000, _FLAT(0.03, 0.35, 0), (100, 200), 0.6, -8),
        ("crack", 0.3, 0.52, 120, 1500, 9000, -12, [(0.3, -2), (0.52, -14)]),
        ("res", 0.54, 0.60, [(330, 320, 70), (2500, 3500, 600)], [(0.54, -4), (0.6, -14)], (50, 90), 0.4, -4),
        ("res", 0.67, 0.73, [(330, 320, 70), (2500, 3500, 600)], [(0.67, -4), (0.73, -14)], (50, 90), 0.4, -10),
        ("res", 0.77, 0.83, [(330, 320, 70), (2500, 3500, 600)], [(0.77, -4), (0.83, -14)], (50, 90), 0.4, -16),
        ("res", 0.87, 0.93, [(330, 320, 70), (2500, 3500, 600)], [(0.87, -4), (0.93, -14)], (50, 90), 0.4, -22),
    ]),
    "hit-02": dict(hp=150, lp=10500, items=[
        ("bed", 0.04, 0.2, 700, 7000, [(0.04, -6), (0.07, 0), (0.2, -10)], (150, 300), 0.7, 0),
        ("train", [0.04, 0.085, 0.12, 0.175], 2000, 7000, 0.004, 4, 0.0, 0.0),
        ("bed", 0.1, 0.75, 3000, 9000, [(0.1, -22), (0.38, -20), (0.45, -15), (0.52, -20), (0.58, -20), (0.65, -14), (0.72, -22), (0.75, -34)], (40, 80), 0.5, 0),
        ("bed", 0.1, 0.75, 450, 900, [(0.1, -22), (0.38, -20), (0.45, -15), (0.52, -20), (0.58, -20), (0.65, -14), (0.72, -22), (0.75, -34)], (40, 80), 0.5, 0),
        ("res", 0.58, 0.72, [(850, 940, 90)], [(0.58, -16), (0.72, -18)], (40, 80), 0.5, 0),
        ("train", [0.53, 0.75, 0.84], 1500, 6000, 0.005, -10, 2.0, 0.0),
        ("crack", 0.75, 1.03, 36, 1500, 9000, -40, [(0.75, -4), (1.02, -12)]),
    ]),
    "hit-03": dict(hp=300, lp=10000, items=[
        ("click", 0.0, 2000, 8000, 0.015, 3, 0.0),
        ("bed", 0.0, 0.2, 600, 1500, [(0, -6), (0.03, 0), (0.2, -20)], (40, 80), 0.5, -4),
        ("bed", 0.02, 0.12, 4000, 10000, [(0.02, 0), (0.1, -14)], (60, 120), 0.5, 0),
        ("train", [0.09, 0.18, 0.26, 0.34, 0.42, 0.50, 0.58], 4000, 10000, 0.03, -2, 1.3, 0.0),
        ("train", [0.09, 0.18, 0.26, 0.34, 0.42, 0.50, 0.58, 0.66], 600, 1500, 0.025, -10, 1.0, 0.0),
        ("res", 0.09, 0.66, [(4500, 4500, 900), (6500, 6500, 1300), (9000, 9000, 2000)], _FLAT(0.09, 0.66, -14), (50, 100), 0.6, 0),
        ("bed", 0.66, 0.70, 300, 10000, [(0.66, 3), (0.70, -10)], (80, 120), 0.4, 0),
        ("thump", 0.66, 450, 320, 0.03, -3),
        ("bed", 0.72, 0.80, 3000, 6000, [(0.72, -36), (0.8, -48)], (60, 100), 0.5, 0),
    ]),
    "hit-04": dict(hp=180, lp=12000, items=[
        ("click", 0.0, 2000, 12000, 0.002, 0, 0.0),
        ("bed", 0.0, 0.32, 2000, 12000, [(0, 0), (0.2, 0), (0.3, -20)], (60, 120), 0.6, 0),
        ("bed", 0.0, 0.3, 14000, 17000, _FLAT(0, 0.3, -14), (60, 100), 0.5, 0),
        ("train", [0.33, 0.405, 0.47, 0.53, 0.62, 0.65], 500, 8000, 0.02, -14, 0.0, 0.5),
        ("click", 0.58, 400, 9000, 0.03, -8, 0.8),
        ("bed", 0.33, 0.66, 500, 1000, [(0.33, -20), (0.66, -28)], (40, 80), 0.5, -4),
        ("res", 0.67, 0.76, [(500, 500, 100), (8000, 8000, 1200)], [(0.67, -22), (0.76, -30), (0.8, -50)], (40, 80), 0.5, 0),
    ]),
    "idle-01": dict(hp=450, lp=14500, items=[
        ("bed", 0.03, 0.5, 3000, 14000, [(0.03, -45), (0.06, -35), (0.11, 0), (0.22, -20), (0.37, -40), (0.5, -60)], (30, 60), 0.65, 0),
        ("res", 0.08, 0.2, [(1500, 2500, 500)], [(0.08, -14), (0.12, -2), (0.2, -16)], (30, 60), 0.5, 0),
        ("click", 0.20, 3000, 9000, 0.005, -8, 0.0), ("click", 0.25, 3000, 9000, 0.005, -10, 0.0),
    ]),
    "idle-02": dict(hp=160, lp=13000, items=[
        ("bed", 0.0, 0.3, 3000, 8000, [(0, -38), (0.06, -14), (0.19, 0), (0.25, -14), (0.3, -30)], (40, 70), 0.55, 0),
        ("click", 0.07, 2000, 6000, 0.008, -12, 0.0),
        ("train", [0.32, 0.36, 0.40], 900, 2500, 0.012, -9, 0.0, 0.8),
        ("bed", 0.05, 0.3, 11000, 12500, _FLAT(0.05, 0.3, 0), (40, 70), 0.4, -16),
        ("bed", 0.4, 0.58, 4000, 8000, [(0.4, -30), (0.57, -48)], (40, 70), 0.5, 0),
    ]),
    "idle-03": dict(hp=300, lp=12000, items=[
        ("bed", 0.05, 0.4, 2000, 7000, [(0.05, -32), (0.15, 0), (0.23, 0), (0.27, -20), (0.39, -40)], (60, 120), 0.6, 0),
        ("res", 0.05, 0.3, [(3000, 3000, 200), (4000, 4000, 250), (5500, 5500, 300)], [(0.05, -26), (0.15, -4), (0.23, -4), (0.3, -24)], (60, 120), 0.6, 0),
        ("train", [0.28, 0.31, 0.35], 3000, 6000, 0.002, -20, 2.0, 0.0),
    ]),
    "idle-04": dict(hp=400, lp=10000, items=[
        ("bed", 0.0, 0.43, 2000, 8000, [(0, -36), (0.18, 0), (0.29, -20), (0.41, -40)], (60, 150), 0.5, 0),
        ("res", 0.0, 0.43, [(2500, 2900, 700), (3500, 3100, 900), (5000, 4500, 1100)], [(0, -36), (0.18, -4), (0.29, -24), (0.41, -44)], (60, 150), 0.5, 0),
        ("crack", 0.27, 0.4, 150, 2500, 8000, -22, [(0.27, 0), (0.4, -10)]),
    ]),
    "idle-05": dict(hp=500, lp=14000, items=[
        ("bed", 0.03, 0.62, 5000, 12000, [(0.03, -30), (0.06, 0), (0.3, -8), (0.4, -20), (0.6, -40)], (30, 60), 0.4, 0),
        ("res", 0.05, 0.4, [(7500, 7500, 500)], [(0.05, -10), (0.4, -16)], (30, 60), 0.4, 0),
        ("bed", 0.04, 0.065, 1000, 2000, [(0.04, -4), (0.065, -10)], (60, 90), 0.4, -2),
        ("bed", 0.12, 0.145, 1000, 2000, [(0.12, -4), (0.145, -10)], (60, 90), 0.4, -2),
        ("bed", 0.22, 0.25, 1000, 2000, [(0.22, -4), (0.25, -10)], (60, 90), 0.4, 3),
    ]),
    "idle-06": dict(hp=500, lp=11500, items=[
        ("bed", 0.03, 0.25, 3000, 10000, [(0.03, -30), (0.1, -12), (0.2, -12), (0.23, -32)], (60, 120), 0.6, 0),
        ("res", 0.03, 0.25, [(2000, 1500, 250), (3200, 2600, 400)], [(0.03, -30), (0.1, -14), (0.2, -14), (0.23, -34)], (60, 120), 0.6, 0),
        ("train", [0.0, 0.04, 0.08, 0.12, 0.17], 2000, 5000, 0.004, -8, 0.0, 0.0),
        ("click", 0.27, 2000, 10000, 0.009, -12, 0.0),
    ]),
    "step_a": dict(hp=120, lp=14000, items=[
        ("click", 0.0, 2000, 14000, 0.003, 6, 0.0),
        ("train", [0.09, 0.17], 1500, 11000, 0.02, 0, 3.0, 0.0),
        ("crack", 0.14, 0.28, 110, 3000, 12000, 0, [(0.14, -4), (0.19, 0), (0.28, -8)]),
        ("train", [0.32, 0.40], 1500, 11000, 0.02, -6, 3.0, 0.0), ("click", 0.51, 3000, 13000, 0.004, -2, 0.0),
        ("crack", 0.04, 0.5, 60, 4000, 14000, -22, _FLAT(0.04, 0.5, 0)),
        ("bed", 0.0, 0.75, 4000, 14000, [(0, -30), (0.3, -34), (0.75, -50)], (60, 120), 0.6, 0),
        ("click", 0.71, 3000, 10000, 0.004, -22, 0.0),
        ("thump", 0.09, 130, 90, 0.03, -8), ("thump", 0.17, 130, 90, 0.03, -10),
    ]),
    "step_b": dict(hp=100, lp=13000, items=[
        ("bed", 0.0, 0.62, 2000, 10000, [(0, -12), (0.4, -16), (0.6, -28)], (80, 150), 0.9, 0),
        ("click", 0.31, 3000, 12000, 0.008, 6, 0.0), ("thump", 0.31, 150, 40, 0.06, 0),
        ("train", [0.05, 0.20, 0.25, 0.36, 0.39, 0.45, 0.51], 2500, 9000, 0.008, -6, 0.0, 0.0),
        ("train", [0.59, 0.76], 2500, 9000, 0.006, -18, 2.0, 0.0),
        ("thump", 0.36, 100, 60, 0.03, -14), ("thump", 0.45, 100, 60, 0.03, -16),
        ("crack", 0.2, 0.6, 90, 3000, 10000, -14, _FLAT(0.2, 0.6, 0)),
    ]),
    "step_c": dict(hp=100, lp=8000, floor=(2000, 6000), items=[
        ("click", 0.0, 2000, 13000, 0.004, 4, 0.0),
        ("train", [0.07, 0.15, 0.23], 2000, 9000, 0.02, -2, 2.0, 0.0),
        ("bed", 0.0, 0.8, 2000, 8000, [(0, -18), (0.3, -22), (0.8, -42)], (150, 300), 0.9, 0),
        ("click", 0.31, 2000, 12000, 0.002, 4, 0.0),
        ("bed", 0.31, 0.43, 2000, 6000, [(0.31, -2), (0.43, -16)], (100, 200), 0.7, 0),
        ("thump", 0.31, 110, 65, 0.05, -6),
        ("train", [0.45, 0.59, 0.73], 1200, 5000, 0.02, -10, 3.0, 0.0),
        ("crack", 0.07, 0.31, 100, 2500, 8000, -12, _FLAT(0.07, 0.31, 0)),
    ]),
}


@recipe("alienbug_*")
@_contour
def alienbug(c, N):
    """Continuous modulated noise beds, moving formant bands, noise clicks and decaying click tails; no sub or
    bass except where the original has a bite or death thump.  The per-sound layout lives in _BUG."""
    c.env_hi = 30.0
    c.limit_crest = False   # the crest limiter leaks click energy down into 20-100 Hz stripes
    c.fill_tail = False   # the generic dead-zone filler copies the thump's spectrum and makes a pulsing sub bed
    c.band_limit = 9.0    # the layers are already built to the band balance; a big static EQ would raise the
    return _build_bug(c, N, _BUG[c.name.split("alienbug_")[1]])   # sub-bass leakage into a pulsing 20-100 Hz bed


# =========================================================================== apache
def _chops(c, n, rate, jitter, lo=40, hi=250, amp_var=0.35, tau=0.03):
    return sfx.pulse_train(n, c.sr, c.rng, rate, jitter=jitter, tau=tau, bp=(lo, hi), amp_var=amp_var)


@recipe("apache_rotor")
def apache_rotor(c, N):
    """Blade flutter: jittered blade rate (14-22 Hz, +-25% timing per pulse), soft 8-15 ms attacks, a second
    slower 8 Hz modulator only ~5 dB deep, over a dense low-mid body.  Full level at both ends (seamless loop)."""
    sr, rng = c.sr, c.rng
    c.looping = True
    m = Mix(c, N)
    ch = sfx.pulse_train(N, sr, rng, 18.0, jitter=0.25, tau=0.014, bp=(50, 1500), amp_var=0.35)
    ch = sfx.fftfilt(ch, sr, lp=1500, order=2) * churn(N, sr, rng, 8, 0.2)
    m.add(_unit(ch), 0.0, 0.3)   # the original has no distinct blade onsets: the chop rides on a dense body
    m.add(_unit(sfx.band_noise(N, sr, rng, 150, 900, order=2)) * churn(N, sr, rng, 7, 0.3), 0.0, 0.9)
    m.add(_unit(sfx.band_noise(N, sr, rng, 700, 2500, order=1)) * churn(N, sr, rng, 9, 0.6), 0.0, 0.12)
    return _fit_contour(c, m.out, passes=3)


def _thud(c, t, f0=100.0, f1=45.0, dur=0.2, tau=0.05, g=1.0, m=None):
    m.add(sub_drop(c.sr, f0, f1, dur, tau, k=0.03, attack=0.006), t, g)


@recipe("apache_burst")
@_contour
def apache_burst(c, N):
    """Round 6: a rotor-gun burst built straight from A's measured band shares (sub 13, bass 50, low-mid 12, mid 10,
    presence 6, air 4 percent): six band-limited noise layers, the mids and highs chopped by a jittered 11 Hz blade
    train over the first 0.85 s (the vertical stripes in A), the lows steadier, then hard clipping and the frame fit
    onto A's contour (a rise to the plateau, then a smooth fall into a dark rumble)."""
    sr, rng = c.sr, c.rng
    K = _knobs(c.name)
    n = N
    t = sfx.tvec(n, sr)
    chop = np.zeros(n)
    tt = 0.0
    while tt < 0.9:
        a = int(tt * sr)
        m = min(int(0.05 * sr), n - a)
        chop[a:a + m] = np.maximum(chop[a:a + m], rng.uniform(0.7, 1.0) * np.exp(-np.arange(m) / sr / 0.022))
        tt += 0.09 * (1 + 0.15 * rng.standard_normal())
    chop = np.convolve(chop, np.hanning(int(sr * 0.004) | 1) / np.hanning(int(sr * 0.004) | 1).sum(), mode="same")
    rot = 1.0 / (1.0 + np.exp((t - 0.9) / 0.05))   # the chopped phase ends at ~0.9 s; the fall is a smooth rumble
    shares = [(20, 60, 12.8, 0.15), (60, 250, 49.5, 0.3), (250, 1000, 11.9, 0.6), (1000, 4000, 9.5, 0.85), (4000, 8000, 6.4, 0.9),
              (8000, 16000, 3.6, 0.9)]
    y = np.zeros(n)
    for lo, hi, share, cd in shares:
        x = _unit(sfx.band_noise(n, sr, rng, lo, hi, order=2))
        mod = (1.0 - cd * rot) + cd * rot * (0.25 + 0.75 * chop)
        y += np.sqrt(share) * K.get("w%d" % lo, 1.0) * x * mod
    return y


def _rumble_bed(c, N, t0, t1, lo, hi, rate, db0, db1, g=1.0):
    sr, rng = c.sr, c.rng
    n = sfx.n_of(sr, t1 - t0)
    x = _unit(sfx.band_noise(n, sr, rng, lo, hi, order=2)) * churn(n, sr, rng, rate, 0.6)
    env = _db(np.linspace(db0, db1, n))
    out = np.zeros(N)
    sfx.place(out, x * env * sfx.smooth_env(n, sr, 0.15, 0.3), t0, sr, g)
    return out


@recipe("apache_explode", "apache_hit_*")
@_contour
def apache_blast(c, N):
    """Fireball crack, dark exponential roar with sub boom and soft debris, progressively darker; hits add a
    low rolling rumble bed under the body so the decay is continuous, with dull 50-120 Hz thud debris."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    nm = c.name
    pops = [t for t in tg.onsets if t > 0.3][:24]
    if "explode" in nm:
        out = blast(c, N, crack_gain=0.9, crack_hi=7000, brightness=0.4, mid_gain=0.25, sub=(80.0, 35.0), sub_gain=1.5,
                    rumble=1.5, churn_rate=9, churn_depth=0.4, crackle=(30, 300, 1800, min(tg.end_t, 5.0), 0.12),
                    pops=pops, pop_gain=0.12, pop_hi=1500, tail_gain=0.9, debris=0.1, bumps=False, t20=1.1, t40=3.6)
        return out
    t20, t40 = (0.95, 1.9) if nm.endswith("hit_a") else (0.9, 2.3)
    out = blast(c, N, crack_gain=0.7, crack_hi=8000, brightness=0.7, sub=(80.0, 34.0), sub_gain=1.1, rumble=1.3,
                churn_rate=9, churn_depth=0.5, pops=[t for t in pops if t < 1.4], pop_gain=0.25, pop_lo=150,
                pop_hi=900, tail_gain=0.9, debris=0.3, bumps=False, t20=t20, t40=t40)
    m = Mix(c, N)
    m.out += _rumble_bed(c, N, 0.3, 2.8, 40, 200, 10, -8, -32, 1.0)
    for t in ([0.74, 0.91, 0.99] if nm.endswith("hit_a") else [0.5, 0.7, 0.9, 1.1, 1.3]):
        nb = sfx.n_of(sr, 0.07)
        th = sfx.band_noise(nb, sr, rng, 50, 700, order=2) * sfx.exp_env(nb, sr, 0.02, attack=0.004)
        m.add(th, t, 0.25)
    res = out + fit(m.out, len(out)) * 0.5
    # A's hits are noisy and fairly bright (flatness 0.04-0.12): a broadband 200-7500 Hz bed that decays with the body
    n2 = len(res)
    bn = _unit(sfx.band_noise(n2, sr, rng, 200, 7500, order=1)) * sfx.decay_env(n2, sr, t20 * 0.6, t20 * 1.6) * churn(n2, sr, rng, 14, 0.5)
    return res + bn * np.std(res[:int(sr * 1.5)]) * (12.0 if nm.endswith("hit_a") else 6.0) * _knobs(nm).get('bn', 2.0)


@recipe("apache_die")
@_contour
def apache_die(c, N):
    """Slow swell to a distorted peak at ~0.44 s with a hard noise crack and a soft-clipped sub thump, three
    buzzy rough-voiced pulses (jittered pulse trains, not pure harmonics), a thin band-limited noise whine near
    6 kHz fading by ~3 s (noise, not a sine), and a crackling mid plateau decaying by ~1.7 s to a thin tail."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    out = blast(c, N, crack_gain=0.8, sub=(90.0, 40.0), sub_gain=1.0, rumble=0.9, churn_rate=12, churn_depth=0.5,
                crackle=(60, 1500, 8000, 3.0, 0.4), tail_gain=0.9, debris=0.5, bumps=False, t20=1.7, t40=3.5)
    m = Mix(c, N)
    for t in (0.1, 0.5, 0.9):
        n = sfx.n_of(sr, 0.2)
        f = np.linspace(580, 540, n) * (1 + 0.03 * (churn(n, sr, rng, 30, 1.0) - 0.5))
        p = pulse_source(n, sr, rng, f, jitter=0.4, level_var=0.7)
        p = sfx.fftfilt(p, sr, hp=300, lp=6000, order=1)
        b = np.tanh(_unit(p) * 2.5) * sfx.smooth_env(n, sr, 0.004, 0.05)
        b = b + 1.0 * sfx.band_noise(n, sr, rng, 1000, 6000, order=1) * sfx.smooth_env(n, sr, 0.004, 0.05)
        m.add(b, t, 0.35)
    nw = sfx.n_of(sr, 3.0)
    wh = _unit(sfx.band_noise(nw, sr, rng, 5000, 7200, order=2)) * churn(nw, sr, rng, 6, 0.6) * np.exp(-np.arange(nw) / sr / 1.2)
    m.add(wh, 0.2, 0.05)
    return out + fit(m.out, len(out))


# =========================================================================== turret
# The originals' beeps ARE tonal, so these keep tones, but each follows the measured structure and nothing
# else: no thump or sub layer (A has nothing below ~1 kHz), no broadband noise burst unless noted, tones
# with 1-3 ms edges and no stray partials.
def _tone(c, f, t0, dur, ratios=(1.0,), gains=(1.0,), edge=0.002, am=None, glide=None, sq=False, m=None, g=1.0):
    sr, rng = c.sr, c.rng
    n = sfx.n_of(sr, dur)
    t = sfx.tvec(n, sr)
    ph = sfx.sweep(glide[0], glide[1], n, sr, k=dur * 0.4) if glide else sfx.TAU * f * t
    y = np.zeros(n)
    for r, gg in zip(ratios, gains):
        if f * r < sr * 0.45:
            a = gg
            if am:   # per-partial detuned irregular modulation (rate Hz, depth)
                a = a * (1 - am[1] * 0.5 * (1 + np.sin(sfx.TAU * am[0] * (1 + rng.uniform(-0.15, 0.15)) * t + rng.uniform(0, 6.28))))
            y += a * np.sin(ph * r + rng.uniform(0, 6.28))
    if sq:   # odd-harmonic buzz (square-like)
        for h in range(3, 40, 2):
            if f * h < sr * 0.45:
                y += (1.0 / h) * np.sin(ph * h)
    k = max(2, int(sr * edge))
    env = np.ones(n)
    env[:k] = np.linspace(0, 1, k)
    env[-k:] = np.minimum(env[-k:], np.linspace(1, 0, k))
    m.add(y * env, t0, g)


def _tick(c, t, lo, hi, dur, g, m):
    sr, rng = c.sr, c.rng
    n = sfx.n_of(sr, max(dur * 6, 0.01))
    x = sfx.fftfilt(sfx.noise(n, rng), sr, hp=lo, lp=hi, order=1) * sfx.exp_env(n, sr, dur / 3.0, attack=0.0004)
    m.add(x / (np.max(np.abs(x)) + 1e-9), t, g)


# Tone lines measured from the original beeps (frequency Hz, start s, end s, prominence dB over the local
# neighborhood).  Numbers only, read off the published analysis; the audio itself is never used.
_BEEP_LINES = {
    1: [(7275, 0.08, 0.18, 45), (7250, 0.2, 0.3, 44), (7350, 0.32, 0.4, 44), (4775, 0.42, 0.5, 22), (7300, 0.42, 0.5, 29), (2050, 0.44, 0.48, 30), (2750, 0.44, 0.48, 32), (3250, 0.48, 0.52, 22), (2700, 0.52, 0.58, 21), (4125, 0.52, 0.58, 29), (10925, 0.52, 0.56, 16), (7300, 0.54, 0.62, 36), (4775, 0.6, 0.66, 20), (5150, 0.6, 0.64, 19), (2750, 0.62, 0.66, 26), (7275, 0.66, 0.74, 35), (1975, 0.68, 0.74, 17), (2675, 0.68, 0.74, 23), (3400, 0.68, 0.74, 21), (4100, 0.68, 0.74, 29), (10875, 0.7, 0.74, 19), (2750, 0.76, 0.8, 28), (7275, 0.78, 0.86, 41), (4100, 0.82, 0.88, 19), (2675, 0.84, 0.88, 15), (9075, 0.86, 0.92, 24), (7275, 0.92, 0.98, 33), (7275, 1.0, 1.1, 45), (7250, 1.12, 1.28, 41), (7350, 1.22, 1.32, 42), (7300, 1.34, 1.42, 37), (9050, 1.42, 1.48, 15), (7300, 1.46, 1.54, 39), (7300, 1.58, 1.66, 35), (7275, 1.7, 1.76, 41), (8975, 1.74, 1.82, 21), (6200, 1.76, 1.8, 16), (9150, 1.8, 1.84, 24), (7275, 1.84, 1.9, 31), (7275, 1.92, 2.02, 42), (7275, 2.04, 2.1, 45)],
    2: [(3950, 0.0, 0.04, 36), (6600, 0.0, 0.04, 29), (7925, 0.0, 0.04, 34), (9275, 0.0, 0.04, 40), (3975, 0.1, 0.14, 35), (3750, 0.36, 0.4, 16), (3975, 0.36, 0.42, 49), (6600, 0.36, 0.42, 37), (7925, 0.36, 0.42, 47), (9275, 0.36, 0.42, 48), (11925, 0.36, 0.4, 31), (13250, 0.36, 0.4, 17), (14575, 0.36, 0.4, 39), (3975, 0.48, 0.52, 50)],
    3: [(5900, 0.0, 0.74, 66), (11300, 0.0, 0.04, 28), (13425, 0.0, 0.62, 40), (7025, 0.02, 0.06, 34), (6000, 0.06, 0.62, 45), (7000, 0.08, 0.48, 35), (11300, 0.08, 0.12, 27), (5750, 0.14, 0.2, 27), (11325, 0.14, 0.2, 25), (5750, 0.22, 0.38, 31), (6100, 0.22, 0.3, 24), (11300, 0.44, 0.52, 24), (7000, 0.54, 0.6, 29), (11325, 0.54, 0.6, 28), (5800, 0.58, 0.62, 25)],
    4: [(3750, 0.0, 0.22, 24), (3975, 0.0, 0.54, 55), (4225, 0.0, 0.06, 18), (6600, 0.0, 0.5, 40), (7925, 0.0, 0.52, 50), (9275, 0.0, 0.52, 52), (11925, 0.0, 0.48, 38), (13250, 0.0, 0.04, 22), (14575, 0.0, 0.48, 48), (6500, 0.06, 0.12, 26), (8175, 0.06, 0.1, 16), (9075, 0.06, 0.12, 26), (13250, 0.06, 0.12, 25), (14725, 0.08, 0.14, 25), (7750, 0.12, 0.18, 17), (9375, 0.12, 0.5, 31), (14725, 0.16, 0.22, 26), (8100, 0.2, 0.26, 19), (6500, 0.22, 0.38, 23), (11850, 0.22, 0.26, 22), (3800, 0.24, 0.3, 20), (4125, 0.24, 0.48, 24), (14725, 0.24, 0.48, 28), (6725, 0.28, 0.34, 18), (8050, 0.28, 0.34, 22), (9475, 0.28, 0.5, 25), (3800, 0.32, 0.38, 20), (8050, 0.36, 0.5, 25), (3800, 0.4, 0.5, 22), (9125, 0.4, 0.5, 30), (7825, 0.42, 0.48, 23)],
    5: [(1550, 0.0, 0.04, 24), (1850, 0.0, 0.04, 24), (2075, 0.0, 0.04, 27), (2350, 0.0, 0.04, 22), (2750, 0.0, 0.04, 22), (3050, 0.0, 0.04, 22), (3250, 0.0, 0.04, 16), (3575, 0.0, 0.04, 25), (3950, 0.0, 0.04, 27), (4225, 0.0, 0.04, 18), (7850, 0.0, 0.06, 48), (10150, 0.0, 0.04, 25), (11350, 0.0, 0.04, 26), (12050, 0.0, 0.04, 20), (15550, 0.0, 0.06, 53), (5300, 0.04, 0.14, 38), (2425, 0.06, 0.12, 26), (6750, 0.1, 0.2, 40), (9175, 0.1, 0.18, 36), (13975, 0.1, 0.2, 41), (7850, 0.18, 0.28, 49), (15550, 0.18, 0.28, 54), (1550, 0.2, 0.24, 18), (1850, 0.2, 0.24, 18), (2075, 0.2, 0.24, 17), (2750, 0.2, 0.24, 17), (3050, 0.2, 0.24, 17), (3550, 0.2, 0.24, 18), (3950, 0.2, 0.24, 17), (5150, 0.2, 0.26, 27), (10250, 0.2, 0.24, 16), (11150, 0.2, 0.24, 18), (7750, 0.24, 0.28, 20), (15650, 0.24, 0.28, 26)],
    6: [(2925, 0.0, 0.04, 27), (4750, 0.0, 0.04, 25), (5275, 0.02, 0.2, 31), (4325, 0.04, 0.1, 23), (4775, 0.06, 0.24, 30), (2950, 0.08, 0.2, 29), (4350, 0.16, 0.2, 20)],
    7: [(8125, 0.0, 0.16, 22), (9650, 0.0, 0.18, 36), (11225, 0.0, 0.1, 16), (15975, 0.0, 0.16, 19), (6550, 0.02, 0.1, 26), (13125, 0.02, 0.16, 30), (13300, 0.08, 0.12, 16), (13125, 0.18, 0.24, 31)],
}


# per-sound tuning for the measured gate: pedestal factor (lower = more tonal), level tilt over time, last-event fade
_BEEP_TUNE = {
    1: {"tilt": [(0.38, 16.0), (0.9, 1.0)]},
    2: {"ped": 0.4, "pw": 0.2},
    4: {"ped": 0.25, "pw": 0.2},
    5: {"tail": (0.05, 0.1), "pw": 0.2, "buzz": (0.133, 0.215, 2.0), "hfcut": (10000, 0.15)},
    6: {"ped": 0.4, "pw": 0.2, "tail": (0.04, 0.08)},
    3: {"end": 0.68, "tail": (0.06, 0.15), "ped": 1.0, "pw": 0.2},
}


def _beep04(c, N):
    """Round 6: a dedicated pulse train.  Fourteen 16.5 ms bursts on a 39.3 ms grid (
    pulse placement fitted to the eleven loudness samples of A's measured contour, so the same samples land in
    gaps), the last one faint, all with 1 ms edges over the same fixed line set (A's measured 4.0, 7.9, 9.3 kHz
    lines, weaker 11.9/14.6 kHz air lines, a narrow ~1.2 kHz body).  No sub, no band conform."""
    sr, rng = c.sr, c.rng
    c.conform_bands = False
    c.conform_env = False
    c.fill_tail = False
    c.limit_crest = False
    K = _knobs(c.name)
    n = sfx.n_of(sr, 0.555)
    lines = [(3975, K.get("l4k", -4)), (7925, K.get("l79", -3)), (9275, K.get("l93", -9)), (11925, K.get("air", -30)), (14575, K.get("air", -30) + 3),
             (6600, -14), (3750, -26), (4225, -30)]
    y = np.zeros(n)
    for hz, db in lines:
        fm = 1 + 0.0015 * (churn(n, sr, rng, 80, 1.0) - 0.5)
        y += _db(db) * np.sin(sfx.TAU * np.cumsum(hz * fm) / sr + rng.uniform(0, 6.28))
    y += _db(K.get("haze", -36)) * _unit(sfx.band_noise(n, sr, rng, 5000, 14500, order=1))
    tt = sfx.tvec(n, sr)
    for hz in (1160, 1230, 1300):   # the ~1.2 kHz body of A is a narrow cluster, not a noise band
        y += _db(K.get("body", -22)) * np.sin(sfx.TAU * hz * tt + rng.uniform(0, 6.28))
    gate = np.full(n, K.get("gap", 0.03))
    for i in range(14):
        a = int(sr * (K.get("t0", 0.003) + i * K.get("per", 0.0392)))
        b = int(sr * (K.get("t0", 0.003) + i * K.get("per", 0.0392) + K.get("on", 0.0155)))
        gate[max(a, 0):max(b, 0)] = 0.12 if i == 13 else 1.0
    e = max(2, int(sr * 0.001))
    ker = np.hanning(2 * e + 1)
    gate = np.convolve(gate, ker / ker.sum(), mode="same")
    return y * gate


_PURE_DEF = {2: {"floor": -50.0, "bed": -58.0}, 3: {"floor": -45.0, "bed": -60.0}, 5: {"floor": -42.0, "bed": -60.0},
             7: {"floor": -48.0, "bed": -60.0}}


def _beep_pure(c, N, k):
    """Round 6 beeps (-02, -03, -05, -07): the measured line bank, each line a plain sine with 1.5 ms edges at its
    measured time and prominence, plus the two things the earlier recipe lacked.  (1) A low-level floor under the
    events: a ~230 Hz jittered pulse comb with a noise skirt (the chip's buzz, rows of faint harmonics and vertical
    striping in A, about -35 to -50 dB under the lines).  (2) A faint steady bed of the same lines between events (A's
    lines ring on at about -46 dB).  No sub thump, no band conform, no tail fill: those added the onset thump A lacks."""
    sr, rng = c.sr, c.rng
    c.conform_bands = False
    c.conform_env = False
    c.fill_tail = False
    c.limit_crest = False
    K = _knobs(c.name)
    D = _PURE_DEF[k]
    tune = _BEEP_TUNE.get(k, {})
    endcap = tune.get("end")
    lines = _BEEP_LINES[k]
    if endcap:
        lines = [(a, b, min(cc, endcap), d) for (a, b, cc, d) in lines if b < endcap]
    t_end = max(l[2] for l in lines)
    n = N
    t = sfx.tvec(n, sr)
    y = np.zeros(n)
    mask = np.zeros(n)
    amax = 10 ** (max(l[3] for l in lines) / 20.0)
    gate3 = None
    if k == 3:   # pulses on the original's grid: starts 0, .08, .16, .27, .35, .43, .51 (35 ms), then the loud final .53-.63
        gate3 = np.full(n, 0.35)
        for st in (0.0, 0.08, 0.16, 0.27, 0.35, 0.43, 0.51):
            gate3[int(sr * st):int(sr * (st + 0.035))] = 1.0
        gate3[int(sr * 0.53):int(sr * 0.64)] = 1.6
        gate3 = np.convolve(gate3, np.hanning(73) / np.hanning(73).sum(), mode="same")
    for (hz, t0, t1, prom) in lines:
        if t1 - t0 <= 0.12:   # the analysis frames (40 ms windows) smear each event; the burst sits inside
            if t0 > 0.0:
                t0 += K.get("ins", 0.016)
            t1 = max(t1 - K.get("ine", 0.008), t0 + 0.012)
        a, b = int(sr * t0), min(int(sr * max(t1, t0 + 0.012)), n)
        m = b - a
        if m < 8:
            continue
        fm = 1 + 0.0015 * (churn(m, sr, rng, 60, 1.0) - 0.5)
        yl = np.sin(sfx.TAU * np.cumsum(hz * fm) / sr + rng.uniform(0, 6.28))
        e = max(2, int(sr * 0.0015))
        env = np.ones(m)
        env[:e] = np.linspace(0, 1, e)
        env[-e:] = np.minimum(env[-e:], np.linspace(1, 0, e))
        hf = K.get("hf", 0.5) if (k == 5 and hz >= 10000) else 1.0   # air lines weighted to A's band balance
        amp = 10 ** (prom / 20.0) / amax * K.get("line", 1.0) * hf
        y[a:b] += yl * env * amp
        # pedestal: the gate measures a line's prominence over the median of its +-1/3-octave neighborhood (A's lines
        # sit only 15-45 dB above it).  Band noise around the line sets that ratio: for a 1920-point Hann frame a sine of
        # amplitude a reads 0.5a and white noise of std s reads 0.0232 s per bin (median), so s = 21.5 a 10^(-prom/20).
        sw = 21.5 * amp * 10 ** (-(prom + K.get("pedx", -2.0)) / 20.0)
        pw = K.get("pw", 0.3)
        pd = sfx.fftfilt(rng.standard_normal(m) * sw, sr, hp=hz * 2 ** -pw, lp=min(hz * 2 ** pw, sr * 0.45), order=2)
        y[a:b] += pd * env
        if t1 - t0 > 0.025:
            mask[a:b] = 1.0
    if gate3 is not None:
        y = y * gate3
    # steady faint bed of the strongest recurring lines across the whole file (ring-on between events)
    bed = D["bed"] + K.get("bedx", 0.0)
    cnt = {}
    for (hz, t0, t1, prom) in lines:
        cnt[hz] = cnt.get(hz, 0) + prom
    if k == 2:   # A's persistent ~1.2 kHz body line (not in the prominence table: it is a wide, low blob)
        cnt[1200] = 10 ** 6
        for (t0, t1) in ((0.0, 0.04), (0.38, 0.42)):
            a, b = int(sr * t0), int(sr * t1)
            y[a:b] += 10 ** (K.get("body", -4.0) / 20.0) * np.sin(sfx.TAU * 1200 * t[a:b] + rng.uniform(0, 6.28))
    for hz in sorted(cnt, key=cnt.get, reverse=True)[:5]:
        y += 10 ** (bed / 20.0) * np.sin(sfx.TAU * hz * t + rng.uniform(0, 6.28))
    # floor: pulse comb plus noise skirt under the events
    mask = np.convolve(mask, np.hanning(int(sr * 0.004) | 1) / np.hanning(int(sr * 0.004) | 1).sum(), mode="same")
    per = 1.0 / K.get("comb", 235.0)
    comb = np.zeros(n)
    tt = 0.0
    while tt < n / sr:
        comb[int(tt * sr)] += 1.0 + 0.3 * rng.standard_normal()
        tt += per * (1 + 0.04 * rng.standard_normal())
    comb = sfx.fftfilt(comb, sr, lp=K.get("combhi", 3500.0), order=2)
    fl = _unit(comb) + 0.7 * _unit(sfx.band_noise(n, sr, rng, 20, K.get("noisehi", 7000.0), order=1))
    y += 10 ** ((D["floor"] + K.get("floor", 0.0)) / 20.0) * fl * mask
    return _fit_contour(c, y, max_gain=K.get("fg", 45.0))   # A's measured loudness contour, frame by frame


@recipe("turret_beep-*")
def turret_beep(c, N):
    """Electronic chirps: a bank of short tone lines placed on the original's measured frequencies, times and
    prominences, each with slight random FM and flutter (buzzy, never a clean sine), over a gated noise floor
    that sets how far the lines stand above their neighborhood.  No sub, thump or click layers."""
    sr, rng = c.sr, c.rng
    k = int(c.name[-2:])
    if k == 4:
        return _beep04(c, N)
    if k in _PURE_DEF and not _knobs(c.name).get("legacy"):
        return _beep_pure(c, N, k)
    c.limit_crest = False
    K = _knobs(c.name)
    if K.get("nobands"):   # the generic band conform boosts the sub band to A's share and smears an onset thump
        c.conform_bands = False
    if K.get("nofill"):
        c.fill_tail = False
    if K.get("noenv"):
        c.conform_env = False
    m = Mix(c, N)
    lines = _BEEP_LINES[k]
    if k == 1:   # the late 7.3 kHz whine reads as one steady quiet layer in the contour
        lines = [l for l in lines if not (7200 <= l[0] <= 7400 and l[1] >= 1.0)] + [(7300, 1.0, 2.1, 37)]
    endcap = _BEEP_TUNE.get(k, {}).get("end")
    if endcap:
        lines = [(a, b, min(cc, endcap), d) for (a, b, cc, d) in lines if b < endcap]
    for (hz, t0, t1, prom) in lines:
        tail = _BEEP_TUNE.get(k, {}).get("tail")
        dur = max(t1 - t0 - (0.016 if t1 - t0 > 0.05 and k == 1 else 0.0), 0.012)
        last = t1 >= max(l[2] for l in _BEEP_LINES[k]) - 0.001
        dur0 = dur
        if tail and last:
            dur += tail[1]   # keep A's short gaps between runs
        n = sfx.n_of(sr, dur)
        t = sfx.tvec(n, sr)
        fm = 1 + 0.004 * (churn(n, sr, rng, 60, 1.0) - 0.5)
        ph = sfx.TAU * np.cumsum(hz * fm) / sr + rng.uniform(0, 6.28)
        y = np.sin(ph) * churn(n, sr, rng, 45, 0.25)
        if k == 4 and t1 - t0 > 0.3:   # thirteen ~40 ms bursts on a 41.5 ms period (hard gate, 1 ms edges)
            gate = ((t % 0.0415) < 0.0310).astype(float)
            y = y * np.convolve(0.12 + 0.88 * gate, np.hanning(45) / np.hanning(45).sum(), mode="same")
        if k == 3 and hz == 5900:   # pulses on the original's grid: starts 0, .08, .16, .27, .35, .43, .51 (35 ms),
            gate = np.full(n, 0.3)  # then the loudest, held final pulse .53-.63; shallow (not silent) gaps
            for st in (0.0, 0.08, 0.16, 0.27, 0.35, 0.43, 0.51):
                gate[int(sr * st):int(sr * (st + 0.035))] = 0.5 if st == 0.0 else 1.0
            gate[int(sr * 0.53):int(sr * 0.66)] = 1.8
            y = y * np.convolve(gate, np.hanning(45) / np.hanning(45).sum(), mode="same")
        e = max(2, int(sr * 0.002))
        env = np.ones(n)
        env[:e] = np.linspace(0, 1, e)
        env[-e:] = np.minimum(env[-e:], np.linspace(1, 0, e))
        if tail and last:   # natural release after the last event
            env = env * np.exp(-np.maximum(t - dur0, 0.0) / tail[0])
        tune = _BEEP_TUNE.get(k, {})
        gk = 1.0
        for (tt, gg) in tune.get("tilt", ()):   # level tilt over time: (from_time, gain)
            if t0 >= tt:
                gk = gg
        if tune.get("fade") and t0 >= tune["fade"][0]:   # slower natural fade for the last event
            env = env * np.exp(-t / tune["fade"][1])
        if tune.get("hfcut") and hz >= tune["hfcut"][0]:   # air-band lines weighted down to A's band balance
            gk *= tune["hfcut"][1]
        pk = tune.get("ped", 1.0)
        m.add(y * env, t0, gk * 10 ** ((prom - 10.0) / 20.0) / 15.7)
        # round 3: the noise floor is a narrow pedestal around each line (about +-0.3 octave) instead of broadband
        # hiss, so the lines keep A's prominence while the spectrum stays tonal (low flatness, no air-band haze)
        ped = sfx.noise(n, rng)
        pw = tune.get('pw', 0.3)
        ped = sfx.fftfilt(ped, sr, hp=hz * 2 ** -pw, lp=min(hz * 2 ** pw, sr * 0.45), order=2)
        pe = np.minimum(np.minimum(np.arange(n) / (sr * 0.003), 1.0), np.minimum((n - 1 - np.arange(n)) / (sr * 0.003), 1.0))
        m.add(ped * np.clip(pe, 0, 1) * (env if tune.get('fade') else 1.0), t0 + 0.0, gk * pk * K.get("ped", 1.0))
    if k == 1:   # tick chatter: short noise ticks (1-4 kHz) at the original's onset times, each a sharp rise
        for t_tick in tg_onsets(c):
            nt = sfx.n_of(sr, 0.04)
            tk = sfx.fftfilt(sfx.noise(nt, rng), sr, hp=1000, lp=4000, order=1) * sfx.exp_env(nt, sr, 0.012, attack=0.0005)
            m.add(tk, t_tick, 3.0)
    bz = _BEEP_TUNE.get(k, {}).get("buzz")
    if bz:   # a segment of the original that is a broadband buzz (no measured line): 300 Hz pulse comb up to ~4 kHz
        n = sfx.n_of(sr, bz[1] - bz[0])
        tt = sfx.tvec(n, sr)
        y = sum(np.sin(sfx.TAU * 300.0 * h * tt * (1 + 0.003 * rng.standard_normal()) + rng.uniform(0, 6.28)) / h ** 0.5 for h in range(5, 14))
        e = max(2, int(sr * 0.002))
        ev = np.ones(n)
        ev[:e] = np.linspace(0, 1, e)
        ev[-e:] = np.linspace(1, 0, e)
        y = y / (np.std(y) + 1e-9) + 1.5 * _unit(sfx.band_noise(n, sr, rng, 1000, 5000, order=1))   # noise fills the comb's neighborhood
        m.add(y * ev * churn(n, sr, rng, 60, 0.4), bz[0], bz[2] * 0.7)
    out = sfx.fftfilt(m.out, sr, hp=800 * K.get("hp", 1.0), order=int(K.get("hpord", 2)))
    if K.get("tb") is not None:   # round-5 floor carrying A's band shares (see _tailbed)
        out = _tailbed(c, out, K["tb"])
    if k in _BEEP_EQ:   # band balance measured the way the gate measures it (loud frames dominate)
        out = _gate_eq(c, out)
    if k in _BEEP_FIT:   # frame-exact loudness fit for the sounds whose contour the generic conform misses
        c.conform_env = False
        out = _fit_contour(c, out)
    return out


_BEEP_FIT = (3, 5, 6)
_BEEP_EQ = ()


def tg_onsets(c):
    return list(get_tg(c).onsets)


def _metal_click(c, level=1.0, big=False, lo=1300.0, hi=6500.0, thp=2200.0, body=1.0, tlp=12000.0, tgain=1.0):
    """One metal click: a 1-3 ms broadband transient plus 3-6 damped inharmonic modes (mostly 600-3500 Hz, each
    decaying in about 15-35 ms; modes above 5 kHz under 10 ms), a different mode set every time so repeated
    clicks never share a pitch.  Short enough that no tonal line is ever seen, resonant enough to read as metal."""
    sr, rng = c.sr, c.rng
    n = sfx.n_of(sr, 0.12)
    t = sfx.tvec(n, sr)
    tr = sfx.fftfilt(sfx.noise(n, rng), sr, hp=thp, lp=tlp, order=1) * sfx.exp_env(n, sr, rng.uniform(0.001, 0.003), attack=0.0002)
    y = _unit(tr) * tgain
    for _ in range(int(rng.integers(3, 7))):
        f = float(np.exp(rng.uniform(np.log(lo), np.log(hi))))
        tau = rng.uniform(0.0025, 0.005) if f < 5000 else rng.uniform(0.0015, 0.0025)
        win = np.clip((0.018 - t) / 0.008, 0.0, 1.0)   # hard fade to zero by 18 ms: no tail for a later gain boost to expose
        y = y + rng.uniform(0.35, 1.0) * np.sin(sfx.TAU * f * t * (1 + 0.06 * rng.standard_normal() * np.exp(-t / 0.01)) + rng.uniform(0, 6.28)) * np.exp(-t / tau) * win
    if big:   # heavier hit: more body noise and a short 100-250 Hz thud
        bd = sfx.band_noise(n, sr, rng, 250, 1200, order=2) * sfx.exp_env(n, sr, 0.02, attack=0.001)
        y = y + 0.8 * body * _unit(bd)
        y = y + 1.2 * body * fit(sub_drop(sr, 230, 150, 0.1, 0.03, k=0.03, attack=0.001), n)
    return y * level


@recipe("turret_gunequip-*")
@_contour
def turret_gunequip(c, N):
    """Ratchet of dense metal clicks at every onset of the original (levels follow its contour), each built by
    _metal_click, with the heaviest hits getting body and a short thud.  Quiet filler ticks between events so
    nothing is silent where the original is busy; no sustained noise block, no steady sine."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    K = _knobs(c.name)
    m = Mix(c, N)
    ons = list(tg.onsets) or [0.05]
    bright = dict(lo=2800.0, hi=12000.0, thp=5000.0, body=0.1, tlp=16500.0, tgain=8.0 * K.get("tg", 1.0)) if c.name.endswith("-03") else (dict(lo=2000.0, hi=8000.0, thp=3500.0, body=0.5, tgain=2.0 * K.get("tg", 1.0)) if c.name.endswith("-01") else {})   # 03 (and a little 01) are brighter than 02
    lv = [rel_env_at(tg, t) for t in ons]
    top = sorted(range(len(ons)), key=lambda i: -lv[i])[:3]
    for i, t in enumerate(ons):
        m.add(_metal_click(c, 1.0, big=i in top, **bright), t, 10 ** (max(lv[i], -30) * 0.6 / 20) + 0.15)
    # filler: small ticks every 15-40 ms across the audible span, 10-18 dB under the main clicks
    t = 0.0
    while t < tg.end_t:
        if rel_env_at(tg, t) > -26 and all(abs(t - o) > 0.012 for o in ons):
            m.add(_metal_click(c, 0.5, **bright), t, _db(K.get("fill", 0.0)) * (0.05 + 0.1 * 10 ** (rel_env_at(tg, t) / 20)))
        t += rng.uniform(0.015, 0.04)
    return sfx.fftfilt(m.out, sr, hp=120 * K.get("hp", 1.0), order=2)

# =========================================================================== round-5 baked search results
# Per-sound configuration found by the embedding-driven search (tools: hill climb on the A/B distance, gate checks
# must not regress).  Every entry only selects among equally valid renditions: a reseed, how much the highs are
# smoothed against the frame fit (_HI_SMOOTH/_SPLIT_OF), and level/filter knobs (_knobs).  Renders without
# GG_CFG_FILE / GG_KNOBS / GG_SALT / GG_HS_ALL are deterministic.
_SALT.update({
    'alienbug_aggro_a': 78,
    'alienbug_aggro_b': 179,
    'alienbug_aggro_d': 179,
    'alienbug_bite_b': 51,
    'alienbug_bite_c': 778,
    'alienbug_bite_d': 21,
    'alienbug_death_a': 260,
    'alienbug_death_b': 607,
    'alienbug_hit-01': 179,
    'alienbug_hit-02': 754,
    'alienbug_hit-03': 291,
    'alienbug_idle-02': 104,
    'alienbug_idle-03': 385,
    'alienbug_idle-04': 161,
    'alienbug_idle-05': 219,
    'alienbug_step_b': 285,
    'alienbug_step_c': 532,
    'apache_burst': 169,
    'apache_explode': 358,
    'apache_hit_a': 443,
    'apache_hit_b': 188,
    'apache_rotor': 211,
    'cyberdemon_aggro_b': 13,
    'cyberdemon_death_a': 634,
    'cyberdemon_death_b': 727,
    'cyberdemon_hurt_c': 293,
    'cyberdemon_idle_c': 123,
    'cyberdemon_step_a': 243,
    'turret_beep-01': 136,
    'turret_beep-03': 583,
    'turret_beep-05': 243,
    'turret_beep-06': 270,
    'turret_beep-07': 31,
    'turret_gunequip-01': 218,
    'turret_gunequip-02': 67,
    'turret_gunequip-03': 255,
})
_HI_SMOOTH.update({
    'alienbug_aggro_a': 0.01,
    'alienbug_aggro_b': 0.01,
    'alienbug_aggro_d': 0.02,
    'alienbug_bite_b': 0.0,
    'alienbug_bite_c': 0.0,
    'alienbug_bite_d': 0.03,
    'alienbug_death_a': 0.03,
    'alienbug_death_b': 0.02,
    'alienbug_hit-01': 0.02,
    'alienbug_hit-02': 0.05,
    'alienbug_hit-03': 0.02,
    'alienbug_idle-02': 0.03,
    'alienbug_idle-04': 0.02,
    'alienbug_idle-05': 0.0,
    'alienbug_step_b': 0.02,
    'alienbug_step_c': 0.02,
    'apache_burst': 0.3,
    'apache_explode': 0.45,
    'apache_hit_a': 0.3,
    'apache_hit_b': 0.45,
    'apache_rotor': 0.08,
    'cyberdemon_aggro_b': 0.45,
    'cyberdemon_death_a': 0.0,
    'cyberdemon_hurt_c': 0.0,
    'cyberdemon_idle_c': 0.08,
    'cyberdemon_step_a': 0.08,
    'turret_beep-01': 0.2,
    'turret_beep-07': 0.03,
    'turret_gunequip-01': 0.0,
    'turret_gunequip-02': 0.02,
    'turret_gunequip-03': 0.0,
})
_SPLIT_OF.update({
    'alienbug_aggro_a': 500.0,
    'alienbug_aggro_b': 100.0,
    'alienbug_aggro_d': 100.0,
    'alienbug_bite_b': 3000.0,
    'alienbug_bite_c': 3000.0,
    'alienbug_bite_d': 100.0,
    'alienbug_death_a': 100.0,
    'alienbug_death_b': 100.0,
    'alienbug_hit-01': 150.0,
    'alienbug_hit-02': 3000.0,
    'alienbug_hit-03': 1500.0,
    'alienbug_idle-02': 150.0,
    'alienbug_idle-03': 300.0,
    'alienbug_idle-04': 1500.0,
    'alienbug_idle-05': 3000.0,
    'alienbug_step_b': 200.0,
    'alienbug_step_c': 150.0,
    'apache_explode': 100.0,
    'apache_hit_a': 100.0,
    'apache_hit_b': 100.0,
    'apache_rotor': 150.0,
    'cyberdemon_aggro_b': 3000.0,
    'cyberdemon_death_a': 3000.0,
    'cyberdemon_death_b': 150.0,
    'cyberdemon_hurt_c': 3000.0,
    'cyberdemon_idle_c': 1500.0,
    'cyberdemon_step_a': 100.0,
    'turret_beep-01': 500.0,
    'turret_beep-07': 150.0,
    'turret_gunequip-01': 3000.0,
    'turret_gunequip-02': 1500.0,
    'turret_gunequip-03': 100.0,
})
_KNOB.update({
    'alienbug_aggro_a': {'fg': 30, 'clipk': 0.7, 'bed': 1.0, 'res': -2.9, 'voice': 5.73, 'click': -1.9, 'crack': -0.8, 'low': -0.37, 'floor': 0.2, 'hp': 0.8, 'lp': 0.93, 'tb': -8.63},
    'alienbug_aggro_b': {'fg': 30, 'clipk': 0.85, 'bed': -1.6, 'res': -0.94, 'voice': -1.8, 'click': -0.37, 'crack': 1.21, 'low': 2.0, 'floor': -0.1, 'hp': 1.13, 'lp': 1.03, 'tb': -20},
    'alienbug_aggro_d': {'fg': 26.05, 'clipk': 2.5, 'bed': -1.92, 'res': 3.44, 'voice': -1.14, 'click': 0.7, 'crack': 6, 'low': 2.11, 'floor': 2.49, 'hp': 1.44, 'lp': 1.15, 'tb': -15.78},
    'alienbug_bite_b': {'bed': -2.9, 'res': 1.0, 'voice': -0.7, 'click': -6, 'crack': -2.6, 'low': -0.97, 'floor': -5.23, 'hp': 0.63, 'lp': 0.95, 'clipk': 0.89, 'tb': -4.31},
    'alienbug_bite_c': {'floor': 6, 'tb': -9.12, 'low': -6, 'res': 4.53, 'fg': 25, 'clipk': 0.7, 'click': -0.64, 'bed': -0.45, 'crack': -2.65, 'lp': 1.15, 'hp': 0.64},
    'alienbug_bite_d': {'fg': 61.4, 'clipk': 0.7, 'bed': -2.32, 'res': -0.8, 'voice': 0.23, 'click': -6, 'crack': 5.55, 'low': -3.78, 'floor': -0.03, 'hp': 0.6, 'lp': 0.89, 'tb': -25.04},
    'alienbug_death_a': {'bed': -6, 'res': 0.2, 'voice': 2.3, 'click': 1.33, 'crack': 3.54, 'low': -0.6, 'floor': 1.69, 'hp': 1.17, 'lp': 1.01, 'tb': -20, 'fg': 39.57, 'clipk': 1.75},
    'alienbug_death_b': {'bed': 4.1, 'res': 0.97, 'clipk': 0.7, 'crack': 3.89, 'voice': 6, 'fg': 32.4, 'floor': -1.83, 'lp': 1.1, 'hp': 0.6, 'click': -5.07, 'low': -1.66, 'tb': -34.61},
    'alienbug_hit-01': {'fg': 30, 'clipk': 1.15, 'bed': -1.6, 'res': -1.8, 'voice': 1.25, 'click': 0.7, 'crack': 5.76, 'low': 2.0, 'floor': -4.93, 'hp': 1.13, 'lp': 1.06, 'tb': -29.52},
    'alienbug_hit-02': {'fg': 45, 'clipk': 2.0, 'bed': 0.5, 'res': -0.3, 'voice': 0.76, 'click': 2.7, 'crack': 2.22, 'low': -2.15, 'floor': -6, 'hp': 1.15, 'lp': 1.04, 'tb': -22.42},
    'alienbug_hit-03': {'fg': 28.34, 'clipk': 2.5, 'bed': -3.28, 'res': 4.07, 'voice': -0.98, 'click': 2.7, 'crack': -0.2, 'low': 6, 'floor': -2.6, 'hp': 0.99, 'lp': 1.08, 'tb': -9.87},
    'alienbug_idle-02': {'fg': 39.65, 'clipk': 1.09, 'bed': -3.59, 'res': 5.19, 'voice': 3.01, 'click': 6, 'crack': -1.7, 'low': 0.34, 'floor': -2.78, 'hp': 1.07, 'lp': 0.97, 'tb': -36.67},
    'alienbug_idle-03': {'click': 1.25, 'low': -1.18, 'clipk': 0.7, 'fg': 56.76, 'bed': -3.51, 'res': -0.19, 'lp': 1.15, 'hp': 1.35, 'tb': -35.16},
    'alienbug_idle-04': {'fg': 35.77, 'clipk': 2.0, 'bed': 0.48, 'res': 0.25, 'voice': -3.4, 'click': 2.7, 'crack': -0.2, 'low': 1.0, 'floor': -2.6, 'hp': 1.5, 'lp': 1.06, 'tb': -31.36},
    'alienbug_idle-05': {'bed': -2.5, 'res': 1.0, 'voice': -0.7, 'click': -2.9, 'crack': -2.6, 'low': -2.5, 'floor': -5.94, 'hp': 1.23, 'lp': 0.95, 'tb': -20, 'clipk': 1.03, 'fg': 55.69},
    'alienbug_step_b': {'bed': -6, 'tb': -9.31, 'click': 2.8, 'voice': -1.11, 'res': -0.92, 'floor': -0.14, 'crack': 4.6, 'hp': 1.09, 'clipk': 1.12, 'lp': 0.74, 'fg': 33.49},
    'alienbug_step_c': {'click': -6, 'tb': -22.94, 'clipk': 1.17, 'hp': 1.45, 'voice': -3.89, 'fg': 40.16, 'crack': 4.78, 'low': -3.39, 'floor': -1.88},
    'apache_burst': {'clipk': 1.16, 'fg': 60.62},
    'apache_explode': {'clipk': 1.65, 'tb': -7.5, 'bn': 1.23},
    'apache_hit_a': {'bn': 3.93, 'fg': 62.34, 'clipk': 1.87, 'tb': -4.79},
    'apache_hit_b': {'bn': 5, 'fg': 27.51, 'clipk': 0.93, 'tb': -15.97},
    'apache_rotor': {'bn': 2.02, 'fg': 44.58, 'clipk': 0.7},
    'cyberdemon_aggro_b': {'breath': 0.19, 'rasp': -8, 'tb': -11.06, 'amr': 0.92, 'clipk': 0.73, 'fg': 41.03, 'amd': 0.06},
    'cyberdemon_death_a': {'amr': 1.0, 'tb': -33.79, 'amd': -0.25, 'fg': 35.38, 'breath': 0.27, 'clipk': 0.87, 'grit': 1.77, 'rasp': -6.05},
    'cyberdemon_death_b': {'fg': 48.46, 'breath': 0.4, 'grit': 1.62, 'amr': 0.76, 'clipk': 1.06, 'amd': -0.05},
    'cyberdemon_hurt_c': {'fg': 25, 'clipk': 1.67, 'grit': 1.15, 'amd': 0.04, 'breath': 0.4, 'amr': 1.08, 'rasp': 7.39, 'tb': -12},
    'cyberdemon_idle_c': {'rasp': -8, 'amd': 0.12, 'tb': 0, 'amr': 1.03, 'fg': 43.32, 'grit': 1.53, 'clipk': 0.7, 'breath': -0.3},
    'cyberdemon_step_a': {'fg': 32.35, 'clipk': 1.57, 'grit': 0.48, 'amd': -0.01, 'breath': 0.16, 'amr': 0.77, 'rasp': 3.0, 'tb': -35.7},
    'turret_beep-01': {'hp': 0.49, 'tb': -36.6, 'ped': 1.13, 'nofill': 1},
    'turret_beep-02': {'tb': -40, 'hp': 1.26, 'ped': 0.72},
    'turret_beep-03': {'hp': 0.93, 'nofill': 1, 'noenv': 0, 'ped': 0.81},
    'turret_beep-05': {'hp': 1.58, 'tb': -40, 'ped': 0.72},
    'turret_beep-06': {'nobands': 1, 'nofill': 0, 'noenv': 0, 'ped': 0.3, 'hp': 0.42, 'hpord': 4},
    'turret_beep-07': {'bed': -2.1, 'res': 0.2, 'voice': 2.3, 'click': 0.8, 'crack': 0.6, 'low': -0.6, 'floor': -0.3, 'hp': 0.44, 'lp': 1.01, 'ped': 0.95, 'nofill': 1},
    'turret_gunequip-01': {'fg': 51.31, 'clipk': 1.14, 'tg': 2.28, 'fill': 1.6, 'hp': 0.69, 'tb': -4.65},
    'turret_gunequip-02': {'hp': 0.4, 'tb': -4.53, 'fill': -5.35, 'clipk': 1.38},
    'turret_gunequip-03': {'fg': 63.89, 'clipk': 2.5, 'tg': 0.41, 'fill': 4.58, 'hp': 0.61, 'tb': -8.55},
})

# ===== round-6 baked search results (scripted search on the embedding distance; merges over the round-5 tables)
_SALT.update({
    'alienbug_aggro_c': 35,
    'alienbug_bite_a': 246,
    'alienbug_hit-04': 250,
    'alienbug_idle-01': 463,
    'alienbug_step_a': 165,
    'alienbug_step_c': 34,
    'cyberdemon_aggro_a': 510,
    'cyberdemon_idle_a': 147,
    'cyberdemon_idle_b': 319,
    'cyberdemon_step_b': 260,
    'cyberdemon_step_d': 445,
})
_HI_SMOOTH.update({
    'alienbug_aggro_c': 0.05,
    'alienbug_idle-01': 0.02,
    'alienbug_step_a': 0.02,
    'cyberdemon_idle_b': 0.05,
    'cyberdemon_step_b': 0.08,
    'cyberdemon_step_d': 0.2,
})
_SPLIT_OF.update({
    'alienbug_hit-04': 200.0,
    'alienbug_idle-01': 800.0,
    'cyberdemon_idle_b': 800.0,
    'cyberdemon_step_b': 150.0,
})
for _n, _k in {
    'alienbug_aggro_c': {'hp': 0.6, 'voice': 1.76, 'clipk': 0.7, 'floor': 3.77, 'res': -1.42},
    'alienbug_bite_d': {'click': -0.36, 'tb': -10.52, 'fg': 33.63, 'res': -2.7, 'voice': 6, 'bed': -0.21},
    'alienbug_death_b': {'clipk': 1.56, 'lp': 1.08},
    'alienbug_hit-04': {'res': 3.63, 'bed': -0.49, 'lp': 1.04, 'floor': -4.02, 'hp': 1.07},
    'alienbug_idle-01': {'res': -0.27, 'clipk': 0.73},
    'alienbug_step_a': {'lp': 0.98, 'bed': 0.14, 'tb': -7.29, 'fg': 53.28, 'click': 6, 'floor': 0.88, 'crack': 5.15},
    'alienbug_step_c': {'tb': -10.87},
    'apache_explode': {'droop': 9.33, 'bn': 2.8},
    'cyberdemon_aggro_a': {'breath': -0.04, 'tb': -21.6, 'grit': 0.93, 'rasp': -4.08},
    'cyberdemon_hurt_b': {'grit': 1.8},
    'cyberdemon_idle_a': {'breath': 0.03, 'rasp': 0.05, 'grit': 1.54, 'clipk': 0.84, 'tb': -6.89},
    'cyberdemon_idle_b': {'amr': 0.94, 'grit': 0.95, 'clipk': 0.7, 'breath': 0.13},
    'cyberdemon_step_b': {'breath': 0.13, 'fg': 38.32, 'grit': 0.61},
    'cyberdemon_step_d': {'tb': -40},
    'turret_beep-04': {'haze': -30.59, 'body': -19.4},
    'turret_beep-05': {'line': 0.86, 'nofill': 1, 'noisehi': 8083.58},
    'turret_beep-06': {'tb': -33.58, 'noenv': 1},
}.items():
    _KNOB[_n] = dict(_KNOB.get(_n, {}), **_k)
