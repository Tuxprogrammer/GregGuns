"""Shared sound engines used by several family modules.

``blast``   shots, impacts, explosions: crack + noisy body + thump + dark tail
``events``  mechanical foley (reloads, handling, latches) placed at the original's onset times
``rumble``  irregular sub-heavy noise textures (beam loops, chainsaw, rotor, flames)
Everything is original synthesis; the only references are the numeric targets (see sndlib).
"""
from __future__ import annotations

import numpy as np

import sfx
from sndlib import (Mix, get_tg, crack_burst, sub_drop, mech_click, mech_clack, mech_slam)


# --------------------------------------------------------------------------- small helpers
def fit(x, n):
    """Pad or trim x to exactly n samples."""
    if len(x) >= n:
        return x[:n]
    return np.pad(x, (0, n - len(x)))


def churn(n, sr, rng, rate, depth):
    """Irregular (never periodic) amplitude modulation in [1-depth, 1]."""
    return sfx.rand_mod(n, sr, rng, rate, depth)


def chug_train(n, sr, rng, rate, jitter=0.45, tau=0.012, lo=60, hi=1800, amp_var=0.6):
    """Irregular engine/gas-burst pulses: jittered intervals, varied levels, short filtered grains."""
    return sfx.pulse_train(n, sr, rng, rate, jitter=jitter, tau=tau, bp=(lo, hi), amp_var=amp_var)


def tenth_times(tg):
    nf = max(1.0, tg.dur / 0.010)
    return [k * (nf - 1) / 10.0 * 0.010 for k in range(11)]


def rel_env_at_exact(tg, t):
    """Level of the original's tenth sample nearest t (dB re its loudest tenth)."""
    e = tg.env10
    if not e or len(e) < 11:
        return -99.0
    ts = tenth_times(tg)
    k = int(np.argmin([abs(t - x) for x in ts]))
    return float(e[k] - max(e))


def rel_env_at(tg, t):
    """Original's loudness at time t in dB relative to its loudest tenth (rough, from 11 samples)."""
    e = tg.env10
    if not e or len(e) < 11:
        return -15.0
    top = max(e)
    ts = np.linspace(0, tg.dur, 11)
    return float(np.interp(t, ts, np.maximum(np.array(e, dtype=float) - top, -80.0)))


# --------------------------------------------------------------------------- blast
def blast(c, N, *, crack_tau=None, crack_lo=800, crack_hi=13000, crack_gain=1.0, sub=(130.0, 45.0),
          sub_gain=0.55, sub_tau=None, body_gain=1.0, mid_gain=1.0, swell=None, whoosh=0.0, whoosh_lo=300,
          whoosh_hi=6000, echoes=(), debris=0.5, tail_gain=0.6, brightness=1.0, thunk=0.0, ring=0.0,
          second=None, churn_rate=0.0, churn_depth=0.0, zap=None, crackle=None, surges=(), lead_ok=True,
          rumble=0.0, crack_on=True, t20=None, t40=None, bumps=True, pops=(), pop_gain=0.8, pop_lo=500, pop_hi=4500):
    """The workhorse for shots, impacts and explosions.

    Instant crack + noisy mid body + pitch-dropping thump + darkening noise tail, all with exponential
    decays that start at the hit.  Options add a swell attack (``swell`` seconds; default taken from the
    original's measured attack), irregular churn, a noisy zap, crackle, extra surges and reflections.
    ``conform`` afterwards pins the slow contour and band balance to the original's measurements."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    if swell is None:
        swell = tg.attack * 0.8 if (tg.peak_t > 0.1 and tg.attack > 0.08) else 0.0
    lead = 0.0
    if lead_ok and not swell and tg.onsets and 0.02 < tg.onsets[0] < 0.15 and tg.attack < 0.12:
        lead = tg.onsets[0] - 0.004
    N0 = N
    N = N - sfx.n_of(sr, lead)
    m = Mix(c, N)
    t20 = max(t20 or tg.dec20 or 0.3, 0.05)
    t40 = max(t40 or tg.dec40 or t20 * 2.2, t20 * 1.3)
    ctau = crack_tau or rng.uniform(0.004, 0.009)
    full = N / sr
    cg = crack_gain * (0.5 if swell else 1.0)
    if crack_on:
        m.add(crack_burst(sr, rng, tau=ctau, lo=crack_lo, hi=crack_hi * brightness, dur=0.12), 0.0, 1.6 * cg)
        m.add(crack_burst(sr, rng, tau=ctau * 3.0, lo=1500, hi=9000 * brightness, dur=0.2), 0.0005, 0.6 * cg)
    n = N
    att = swell if swell else 0.0008
    mid = sfx.dark_noise(n, sr, rng, rng.uniform(5500, 8000) * brightness, 650, steps=8, hp=180)
    mid = mid * sfx.decay_env(n, sr, t20 * 0.55, t20 * 1.4, attack=att)
    if churn_depth:
        mid = mid * churn(n, sr, rng, churn_rate, churn_depth)
    m.add(mid, 0.0, 1.15 * mid_gain)
    low = sfx.dark_noise(n, sr, rng, 1300, 140, steps=7, hp=45) * sfx.decay_env(n, sr, t20 * 0.9, t40 * 0.8, attack=att)
    if churn_depth:
        low = low * churn(n, sr, rng, churn_rate * 0.7, churn_depth)
    m.add(low, 0.0, 1.0 * body_gain)
    st = sub_tau or max(0.07, t20 * 0.4)
    m.add(sub_drop(sr, sub[0], sub[1], max(0.3, st * 5), st, k=0.035, attack=0.0015 if not swell else swell * 0.6), 0.0, sub_gain)
    if rumble:  # unpitched sub-bass noise body (20-130 Hz), irregular
        rb = sfx.fftfilt(sfx.noise(n, rng), sr, hp=22, lp=130, order=2) * sfx.decay_env(n, sr, t20 * 1.2, t40, attack=att + 0.01)
        rb = rb * churn(n, sr, rng, 9, 0.5)
        m.add(rb, 0.0, rumble)
    if t40 > 0.35:
        nt = sfx.n_of(sr, min(t40 * 1.6 + 0.3, full))
        tail = sfx.dark_noise(nt, sr, rng, 2600 * brightness, 220, steps=8, hp=60)
        tail = tail * sfx.decay_env(nt, sr, t20 * 1.1, t40, attack=0.02 + (swell * 0.5)) * churn(nt, sr, rng, 14, 0.35)
        m.add(tail, 0.0, tail_gain)
        if debris:
            nd = sfx.n_of(sr, min(t40 * 1.3, full))
            deb = sfx.grain_cloud(nd, sr, rng, np.linspace(160, 12, nd), 0.004, 500, 5000, env=sfx.decay_env(nd, sr, t20, t40), amp_var=0.9)
            m.add(deb, 0.02, debris * 0.55)
    if whoosh:
        nw = sfx.n_of(sr, min(t40, full))
        wh = sfx.dark_noise(nw, sr, rng, whoosh_hi, whoosh_lo * 2, steps=8, hp=whoosh_lo * 0.6)
        wh = wh * sfx.decay_env(nw, sr, t20 * 0.9, t40 * 0.9, attack=0.1)
        m.add(wh, 0.0, whoosh)
    if thunk:
        m.add(sfx.thunk(sr, rng, rng.uniform(55, 80), 0.1, 0.35, grit=0.6), 0.0, thunk)
    if ring:
        m.add(sfx.ping(sr, rng, [rng.uniform(5200, 7000), rng.uniform(9000, 11000)], 0.05, n=sfx.n_of(sr, 0.3)), 0.0, ring)
    if zap:  # noisy falling zap: FM carrier plus a bandpassed noise tail that follows it
        zd = zap.get("dur", 0.3)
        zs = sfx.fm_zap(sr, rng, zd, zap.get("f0", 3000), zap.get("f1", 400), ratio=zap.get("ratio", 2.0), index=zap.get("index", 5.0), ring=0.3)
        zn = sfx.fftfilt(sfx.noise(len(zs), rng), sr, hp=zap.get("f1", 400), lp=zap.get("f0", 3000), order=1) * sfx.exp_env(len(zs), sr, zd * 0.35)
        m.add(zs * 0.6 + zn * 0.8, zap.get("t", 0.0), zap.get("gain", 0.3))
    if crackle:  # (density/s, lo, hi, t_end, gain)
        dens, lo_, hi_, te, g_ = crackle
        nc = sfx.n_of(sr, min(te, full))
        ck = sfx.grain_cloud(nc, sr, rng, np.linspace(dens, dens * 0.25, nc), 0.0025, lo_, hi_, env=sfx.exp_env(nc, sr, te * 0.5), amp_var=0.85)
        m.add(ck, 0.0, g_)
    for (ts, ds, gs) in surges:  # extra swells / bumps of rumble
        ns = sfx.n_of(sr, ds)
        sg = sfx.dark_noise(ns, sr, rng, 2400, 160, steps=5, hp=40) * sfx.smooth_env(ns, sr, ds * 0.3, ds * 0.6)
        m.add(sg, ts - lead, gs)
    for tp in pops:  # sharp debris / crackle pops at given times (noise bursts, 1 ms attack)
        npp = sfx.n_of(sr, 0.07)
        pp = sfx.fftfilt(sfx.noise(npp, rng), sr, hp=pop_lo, lp=pop_hi * rng.uniform(0.8, 1.3), order=1) * sfx.exp_env(npp, sr, 0.014, attack=0.0008)
        m.add(pp, tp - lead, pop_gain * rng.uniform(0.6, 1.0))
    for (te, ge) in echoes:
        ne = sfx.n_of(sr, 0.35)
        ec = sfx.fftfilt(sfx.noise(ne, rng), sr, hp=300, lp=3500, order=1) * sfx.exp_env(ne, sr, 0.09, attack=0.003)
        m.add(ec, te, ge)
    first = tg.onsets[0] if tg.onsets else 0.0
    if bumps:  # reflections / debris bumps where the original has secondary onsets (noise, never a tone)
        for to in [t for t in tg.onsets if t > first + 0.06][:8]:
            nb = sfx.n_of(sr, 0.09)
            bump = sfx.fftfilt(sfx.noise(nb, rng), sr, hp=250, lp=rng.uniform(2500, 5000), order=1) * sfx.exp_env(nb, sr, 0.03, attack=0.003)
            m.add(bump, to - lead, 0.32)
    if second:
        m.add(mech_clack(sr, rng, tau=0.04, thump=0.3), second[0], second[1])
    if not lead:
        return fit(m.out, N0)
    nl = sfx.n_of(sr, lead + 0.01)
    pre = sfx.band_noise(nl, sr, rng, 40, 700, order=2) * np.linspace(0.05, 1.0, nl) ** 2
    out = np.concatenate([np.zeros(N0 - N), m.out])
    out[:nl] += pre * 0.03 * np.max(np.abs(m.out))
    return fit(out, N0)


# --------------------------------------------------------------------------- mechanical foley
def _sum(a, b):
    n = max(len(a), len(b))
    out = np.zeros(n)
    out[:len(a)] += a
    out[:len(b)] += b
    return out


def foley_event(sr, rng, kind, gain=1.0, bright=1.0, slam_bass=(0.15, 0.5)):
    """One handling event; every kind is a noise transient (never a held tone)."""
    if kind == "click":
        return mech_click(sr, rng, bright=rng.uniform(0.8, 1.3) * bright, ring_amp=0.1) * gain
    if kind == "tick":
        return mech_click(sr, rng, bright=rng.uniform(1.0, 1.6) * bright, dur=0.006, ring_amp=0.08) * gain * 0.8
    if kind == "clack":
        return mech_clack(sr, rng, tau=rng.uniform(0.02, 0.05), bright=bright, thump=rng.uniform(0.1, 0.4)) * gain
    if kind == "slam":
        return mech_slam(sr, rng, dur=0.5, bass=rng.uniform(*slam_bass)) * gain
    if kind == "thunk":
        return _sum(sfx.thunk(sr, rng, rng.uniform(110, 230), 0.04, 0.16, grit=0.8) * gain * 0.8,
                    mech_click(sr, rng, bright=0.8, ring_amp=0.2) * gain * 0.6)
    if kind == "scrape":
        n = sfx.n_of(sr, rng.uniform(0.04, 0.09))
        sc = sfx.band_noise(n, sr, rng, 900, 6500, order=1) * churn(n, sr, rng, 90, 0.9) ** 2
        return sc * sfx.smooth_env(n, sr, 0.008, 0.03) * gain * 0.7
    raise ValueError(kind)


def events(c, N, *, mix=(("click", 0.5), ("clack", 0.3), ("thunk", 0.1), ("scrape", 0.1)), bright=1.0,
           rattle=1.0, rustle=0.2, peak_kind="slam", extra=(), floor_db=-30.0, level_pow=0.5, tail_ticks=0,
           slam_bass=(0.15, 0.5)):
    """Foley sequence: an event at every onset of the original, a rattle/handling bed that follows its
    loudness contour, and a low-level rustle so gaps are not digital silence (unless the original's are)."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    kinds = [k for k, _ in mix]
    probs = np.array([p for _, p in mix], dtype=float)
    probs /= probs.sum()
    ons = list(tg.onsets) if tg.onsets else [0.05]
    # the loudest event of the original gets the heavy hit
    pk = tg.peak_t
    big = min(range(len(ons)), key=lambda i: abs(ons[i] - pk)) if ons else -1
    if ons and abs(ons[big] - pk) > 0.15:
        big = -1
    for i, t in enumerate(ons):
        lvl = rel_env_at(tg, t)
        amp = 10 ** (max(lvl, floor_db) * level_pow / 20.0) * rng.uniform(0.75, 1.0)
        if i == big:
            m.add(foley_event(sr, rng, peak_kind, 1.0, bright, slam_bass), t, 1.0)
            continue
        k = kinds[int(rng.choice(len(kinds), p=probs))]
        m.add(foley_event(sr, rng, k, 1.0, bright), t, 0.55 * amp + 0.15)
        # most events are followed by a tiny rattle
        if k in ("clack", "thunk") and rng.random() < 0.7:
            nr = sfx.n_of(sr, 0.08)
            m.add(sfx.grain_cloud(nr, sr, rng, 220, 0.002, 1800, 9000, env=sfx.exp_env(nr, sr, 0.03), amp_var=0.8), t + 0.01, 0.18 * amp)
    for (t, kind, g) in extra:
        m.add(foley_event(sr, rng, kind, 1.0, bright), t, g)
    # the original's loudness is sampled at 11 instants; where one of those frames is near its top there is an
    # event right there, so make sure we have one too
    c.env_smooth = 0.02
    for t in tenth_times(tg):
        rl = rel_env_at_exact(tg, t)
        if rl > -16 and all(abs(t - o) > 0.045 for o in ons):
            k = kinds[int(rng.choice(len(kinds), p=probs))]
            m.add(foley_event(sr, rng, k, 1.0, bright), max(t - 0.004, 0.0), 0.55 * 10 ** (rl * level_pow / 20.0) + 0.1)
    # rattle bed: grains whose density follows the original's loudness contour
    if rattle:
        n = N
        ts = np.arange(n) / sr
        lv = np.array([rel_env_at(tg, t) for t in ts[::256]])
        dens = np.interp(ts, ts[::256], 12.0 + 130.0 * 10 ** (np.maximum(lv, -45) / 20.0))
        dens = np.where(ts <= tg.dur, dens, 0.0)
        bed = sfx.grain_cloud(n, sr, rng, np.maximum(dens, 0.5), 0.0022, 1500, 8500, amp_var=0.9)
        gate = 10 ** (np.interp(ts, ts[::256], np.maximum(lv, -60)) / 20.0)
        m.add(bed * gate, 0.0, 0.9 * rattle)
    if rustle:
        # handling bed: bandpassed noise whose level tracks the original's contour (rms ~ ``rustle`` of the
        # loudest frame), so gaps carry low-level texture unless the original is silent there too
        n = N
        ts = np.arange(n) / sr
        lv = np.array([rel_env_at(tg, t) for t in ts[::256]])
        gate = 10 ** (np.interp(ts, ts[::256], np.maximum(lv, -70)) / 20.0)
        rs = sfx.band_noise(n, sr, rng, 500, 7000, order=1) * churn(n, sr, rng, 30, 0.9) ** 2
        rs = rs / (np.std(rs) + 1e-9)
        m.add(rs * gate, 0.0, rustle)
    for j in range(tail_ticks):
        m.add(foley_event(sr, rng, "tick", 1.0, bright), tg.end_t - rng.uniform(0.05, 0.5), 0.12)
    return m.out


# --------------------------------------------------------------------------- rumble / texture
def rumble(c, N, *, lo=25, hi=250, rate=18, depth=0.7, grit=0.0, hiss=0.0, hiss_lo=2500, hiss_hi=9000,
           comb=None, comb_gain=0.15, glide=None, chug=0.0, chug_rate=22, crackle=0.0):
    """Irregular sub-heavy noise texture.  ``glide=(f_start, f_end)`` sweeps the low-pass corner;
    ``comb=f0`` adds a quiet harmonic stack (engines); ``chug`` adds jittered firing pulses."""
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    if glide:
        base = sfx.dark_noise(N, sr, rng, glide[0], glide[1], steps=8, hp=lo)
    else:
        base = sfx.fftfilt(sfx.noise(N, rng), sr, hp=lo, lp=hi, order=2)
    base = base * churn(N, sr, rng, rate, depth)
    if grit:
        base = sfx.waveshape(base / (np.std(base) + 1e-9), 1.0 + grit * 3) * np.std(base)
    m.add(base, 0.0, 1.0)
    if hiss:
        hs = sfx.band_noise(N, sr, rng, hiss_lo, hiss_hi, order=1) * churn(N, sr, rng, 40, 0.8)
        m.add(hs, 0.0, hiss)
    if comb:
        f = np.full(N, float(comb)) * (1 + 0.02 * (churn(N, sr, rng, 5, 1.0) - 0.5))
        st = sfx.harmonic_stack(f, N, sr, nh=24, rolloff=1.0, rng=rng)
        m.add(sfx.fftfilt(st, sr, hp=60, lp=4500, order=1), 0.0, comb_gain)
    if chug:
        m.add(chug_train(N, sr, rng, chug_rate, jitter=0.5, lo=70, hi=1500), 0.0, chug)
    if crackle:
        m.add(sfx.grain_cloud(N, sr, rng, 60, 0.003, 1200, 7000, amp_var=0.9), 0.0, crackle)
    return m.out


# --------------------------------------------------------------------------- electronic beeps (original is tonal)
def tone_burst(sr, rng, f, dur, ratios=(1.0,), gains=(1.0,), attack=0.0008, release=0.004, grit=0.0,
               glide=None, flutter=0.0):
    """Gated beep: a few partials with a click-sharp attack and short release; optional grit noise,
    pitch glide (f0 -> f1) and irregular amplitude flutter.  Used only where the original IS a beep."""
    n = sfx.n_of(sr, dur)
    t = sfx.tvec(n, sr)
    out = np.zeros(n)
    if glide:
        ph = sfx.sweep(glide[0], glide[1], n, sr, k=dur * 0.4)
    else:
        ph = sfx.TAU * f * t
    for r, g in zip(ratios, gains):
        if f * r < sr * 0.45:
            out += g * np.sin(ph * r)
    if grit:
        out += grit * sfx.fftfilt(sfx.noise(n, rng), sr, hp=max(f * 0.5, 300), lp=min(f * 4, sr * 0.45), order=1)
    env = np.minimum(t / max(attack, 1e-4), 1.0) * np.minimum((dur - t) / max(release, 1e-4), 1.0)
    if flutter:
        env = env * churn(n, sr, rng, 90, flutter)
    return out * np.clip(env, 0, 1)


def click_thump(sr, rng, g_click=1.0, g_thump=0.6, bright=1.0, thump_f=(220, 90), thump_dur=0.04):
    """Broadband click with an optional short low thump under it."""
    n = sfx.n_of(sr, 0.03)
    cl = sfx.fftfilt(sfx.noise(n, rng), sr, hp=2500 * bright, lp=min(14000, sr * 0.45), order=2) * sfx.exp_env(n, sr, 0.004, attack=0.0002)
    out = cl * g_click
    if g_thump:
        th = sub_drop(sr, thump_f[0], thump_f[1], thump_dur * 4, thump_dur, k=0.012)
        out = _sum(out, th * g_thump)
    return out
