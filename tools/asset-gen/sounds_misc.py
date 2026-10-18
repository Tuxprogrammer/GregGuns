"""Items, machines and blocks: night vision, anti-grav, glider, ammo press, RC machine sounds, metal door."""
from __future__ import annotations

import numpy as np

import sfx
from sndlib import Mix, get_tg, recipe, crack_burst, sub_drop, mech_click, mech_clack, mech_slam
from sndengines import (fit, blast, events, rumble, churn, tone_burst, click_thump, foley_event, rel_env_at, _sum)


# --------------------------------------------------------------------------- private helpers (round 2)
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


# =========================================================================== items
@recipe("nightvision_on")
def nightvision_on(c, N):
    """Noisy broadband first burst (~85 ms, slightly soft-clipped), a louder crunchy second burst at ~0.10 s,
    thin 2.2/4.5 kHz partials that glide down ~5% then drift back up, a faint low rumble, and an 8-18 kHz
    shimmer that fades continuously (no gated re-entry)."""
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    n1 = sfx.n_of(sr, 0.10)
    b1 = sfx.band_noise(n1, sr, rng, 1200, 15000, order=1) * sfx.exp_env(n1, sr, 0.03, attack=0.0004)
    m.add(sfx.waveshape(b1 / (np.max(np.abs(b1)) + 1e-9), 1.6), 0.03, 1.1)
    # second burst: crunchy 1-12 kHz noise, ~40 ms, only ~4 dB under the first
    n2 = sfx.n_of(sr, 0.06)
    b2 = sfx.band_noise(n2, sr, rng, 1000, 12000, order=1) * sfx.exp_env(n2, sr, 0.016, attack=0.0003)
    m.add(sfx.waveshape(b2 / (np.max(np.abs(b2)) + 1e-9), 2.0) * churn(n2, sr, rng, 220, 0.6), 0.10, 0.75)
    # shimmer: wide, noisy 8-14 kHz haze plus a 15-18 kHz sparkle (no tonal lines), fading about 40 dB
    ns = sfx.n_of(sr, 1.2)
    if sr * 0.44 > 15500:
        m.add(sfx.band_noise(ns, sr, rng, 15000, min(18000, sr * 0.44), order=1) * sfx.exp_env(ns, sr, 0.35)
              * churn(ns, sr, rng, 60, 0.7), 0.15, 0.12)
    nsh = sfx.n_of(sr, 1.3)
    m.add(sfx.band_noise(nsh, sr, rng, 8000, 13000, order=1) * sfx.exp_env(nsh, sr, 0.4) * churn(nsh, sr, rng, 25, 0.8), 0.1, 0.12)
    # noise bed fades continuously: bright haze, no hard stop
    nt = sfx.n_of(sr, 1.4)
    m.add(sfx.band_noise(nt, sr, rng, 6500, 14000, order=1) * sfx.exp_env(nt, sr, 0.22) * churn(nt, sr, rng, 50, 0.7), 0.1, 0.3)
    # faint low rumble under everything
    nr = sfx.n_of(sr, 1.3)
    m.add(sfx.band_noise(nr, sr, rng, 40, 120, order=2) * sfx.exp_env(nr, sr, 0.5), 0.04, 0.05)
    return m.out


@recipe("nightvision_off")
def nightvision_off(c, N):
    """Metallic click at the start, loud noisy burst (peak 0.03 s, -20 dB by 0.10 s, high-passed), a 9 -> 5 kHz
    downward streak, a hissy bright tail, then a tonal whine gliding 5 -> 3 kHz with three soft blips and a
    faint ~17 kHz shimmer.  No second noise burst."""
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    # metallic click: 2-6 kHz noise snap plus a short FM ring
    m.add(crack_burst(sr, rng, tau=0.003, lo=2000, hi=6000, dur=0.03), 0.0, 1.3)
    # body: 250 Hz-15 kHz noise, 4 ms attack, -20 dB by ~0.10 s
    nb = sfx.n_of(sr, 0.25)
    body = sfx.band_noise(nb, sr, rng, 250, 15000, order=1) * sfx.exp_env(nb, sr, 0.04, attack=0.004)
    m.add(body, 0.0, 1.0)
    n = sfx.n_of(sr, 0.12)
    streak = sfx.dark_noise(n, sr, rng, 9000, 5000, steps=5, hp=4500) * sfx.exp_env(n, sr, 0.05)
    m.add(streak, 0.04, 0.5)
    # hissy tail, bright and rising in centroid
    nh = sfx.n_of(sr, 0.32)
    m.add(sfx.band_noise(nh, sr, rng, 6000, min(16000, sr * 0.44), order=1) * sfx.smooth_env(nh, sr, 0.01, 0.12), 0.12, 0.18)
    # whine on A's measured path (about 4.85 kHz at 0.24 s -> 3.4 kHz at 0.36 s): noisy (jittered phase, noise
    # mixed in) and quiet, so it reads as a hissy glide rather than a pure tone
    nw = sfx.n_of(sr, 0.18)
    tw = sfx.tvec(nw, sr)
    fw = np.interp(tw, [0, 0.08, 0.10, 0.12, 0.18], [4850, 3900, 3650, 3375, 3200])
    fw = fw * (1 + 0.04 * (sfx.noise(nw, rng) - 0) * 0.3)
    blips = 0.5 + sum(_gauss(nw, sr, tb, 0.011) for tb in (0.05, 0.09, 0.13))
    whine = np.sin(sfx.osc_phase(fw, sr)) * 0.5 + 0.8 * sfx.band_noise(nw, sr, rng, 2800, 5200, order=1)
    m.add(whine * blips * sfx.smooth_env(nw, sr, 0.01, 0.05), 0.24, 0.05)
    # faint ~17 kHz shimmer over the last 0.15 s
    if sr * 0.44 > 17500:
        ns = sfx.n_of(sr, 0.15)
        m.add(sfx.band_noise(ns, sr, rng, 15500, 19500, order=1) * sfx.smooth_env(ns, sr, 0.04, 0.05), 0.29, 0.05)
    return m.out


@recipe("antigrav_start_*")
def antigrav_start(c, N):
    """Hard clipped power-up: sub thumps on both onsets (0.05 / 0.23 s; one onset on the b variant), a saturated
    sub boom, an arched noise sweep (rises to ~6-9 kHz near 0.45 s, falls back), pulsing rumble in the decay,
    a faint 2-3 kHz air line."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    two = len(tg.onsets) >= 2
    out = blast(c, N, crack_gain=1.0, sub=(95.0, 42.0), sub_gain=1.1, rumble=0.9, churn_rate=15, churn_depth=0.6,
                whoosh=0.0, whoosh_lo=1200, whoosh_hi=9000, tail_gain=0.7, debris=0.2, bumps=True)
    m = Mix(c, N)
    t_a = tg.onsets[0] if tg.onsets else 0.05
    m.add(sub_drop(sr, 75, 48, 0.2, 0.07, k=0.03, attack=0.002), t_a, 1.1)
    m.add(crack_burst(sr, rng, tau=0.012, lo=300, hi=7000, dur=0.06), t_a, 0.7)
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
    m.add(_arch(c, 1.2, arch), 0.0, 1.5)
    # thin air line, about -45 dB
    na = sfx.n_of(sr, 0.7)
    air = sfx.band_noise(na, sr, rng, 2000, 2500, order=2) * sfx.smooth_env(na, sr, 0.1, 0.3)
    m.add(air, 0.6, 0.02)
    out = out + fit(m.out, len(out))
    return sfx.waveshape(out / (np.max(np.abs(out)) + 1e-9), 3.0)


@recipe("glider_loop_stereo")
def glider_loop(c, N):
    """Wind: a slow swell (quiet dark edges, brighter middle) with the filter following the swell, low-frequency
    buffeting on the bass, a steep ~2 kHz top, a faint air band only in the middle, a few faint flecks near 8 s,
    and partially correlated channels (about 0.43)."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    c.looping = True
    dur = N / sr
    f_pts = np.array([0, 0.1, 0.2, 0.4, 0.5, 0.65, 0.75, 0.85, 0.93, 1.0])
    db_pts = np.array([-29, -30, -25, -24, -21, -21, -26, -24, -29, -29], dtype=float)
    u = np.linspace(0, 1, N)
    env = 10 ** (np.interp(u, f_pts, db_pts) / 20)
    env = env / env.max()
    bright = np.clip((np.interp(u, f_pts, db_pts) + 29.0) / 8.0, 0.0, 1.0)

    def layer():
        dark = sfx.band_noise(N, sr, rng, 60, 700, order=3)
        mid = sfx.band_noise(N, sr, rng, 60, 1500, order=3)
        air = sfx.band_noise(N, sr, rng, 1000, 4000, order=1)
        low = sfx.band_noise(N, sr, rng, 25, 450, order=2) * churn(N, sr, rng, 10, 0.3)
        y = dark * (1 - bright) + mid * bright + 0.06 * air * bright ** 2 + 0.5 * low
        return y * (0.75 + 0.25 * churn(N, sr, rng, 0.8, 1.0))

    shared = layer()
    chans = []
    for k in range(2):
        y = 0.75 * shared + 0.85 * layer()
        y = sfx.conform_bands(y, sr, tg.bands, rng=rng)
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
    """Small machine clunk: 80 Hz thump, 200-800 Hz noise body and a 1-3 kHz click (about 40 ms)."""
    n = sfx.n_of(sr, 0.05)
    t = sfx.tvec(n, sr)
    body = sfx.band_noise(n, sr, rng, 200, 800, order=2) * sfx.exp_env(n, sr, 0.014, attack=0.0008)
    thump = np.sin(sfx.TAU * 80 * t) * np.exp(-t / 0.02)
    click = sfx.band_noise(n, sr, rng, 1000, 3000, order=2) * sfx.exp_env(n, sr, 0.004, attack=0.0003)
    return gain * (body + 0.9 * thump + 0.7 * click)


def _tick(sr, rng, gain=1.0):
    """2-4 ms servo tick, 1.5-3 kHz band."""
    n = sfx.n_of(sr, 0.012)
    return gain * sfx.band_noise(n, sr, rng, 1500, 3000, order=2) * sfx.exp_env(n, sr, 0.003, attack=0.0002)


def _press(c, N, slam_t, rings=None, churn_gain=1.0, grind=0.5, clunks=0.6, swell=0.0, body_f=(110, 360),
           sub_gain=0.25, crack=1.2, soft=False, pre_tone=False, bed_hi=700):
    """Machine press: a heavy slam (crack + sub + body + metal rings), a low gear-grind bed that outlasts a
    mid churn (so the tail darkens), servo ticks and small clunks at the original's later onsets.
    ``soft`` lowpasses the slam and ramps the body in; ``pre_tone`` adds a 450 Hz motor hum before the slam."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    lead = slam_t
    if pre_tone:
        # motor hum rising from -35 dB to -15 dB over the lead-in
        n = sfx.n_of(sr, lead + 0.05)
        t = sfx.tvec(n, sr)
        ramp = 10 ** ((-35 + 20 * np.clip(t / max(lead, 1e-3), 0, 1)) / 20)
        late = t > lead
        ramp[late] *= np.exp(-(t[late] - lead) / 0.03)
        hum = np.sin(sfx.TAU * 450 * t * (1 + 0.004 * np.sin(sfx.TAU * 6 * t))) + 0.25 * np.sin(sfx.TAU * 900 * t)
        motor = (sfx.band_noise(n, sr, rng, 80, 160, order=2) * 1.2
                 + sfx.band_noise(n, sr, rng, 150, 900, order=2) * churn(n, sr, rng, 30, 0.7) * 0.5)
        m.add((hum * 0.45 + motor) * ramp, 0.0, 0.45)
    elif tg.env10 and tg.env10[0] > -48:
        lead = slam_t + 0.1
        # the original is audible before the hit: a quiet low bed
        n = sfx.n_of(sr, lead)
        m.add(sfx.band_noise(n, sr, rng, 40, 150, order=3) * churn(n, sr, rng, 10, 0.6) * sfx.smooth_env(n, sr, slam_t * 0.8, 0.0), 0.0, 0.6)
    if swell:
        nb = sfx.n_of(sr, swell + 0.4)
        m.add(sfx.band_noise(nb, sr, rng, 150, 1200, order=2) * sfx.smooth_env(nb, sr, swell, 0.3), 0.0, 0.7)
    rm = rumble(c, N, lo=30, hi=bed_hi, rate=18, depth=0.55, grit=0.2, hiss=0.0)
    if soft:
        # body ramps in over ~0.15 s ahead of the impact
        t0 = max(0.0, slam_t - 0.15)
        k = N - sfx.n_of(sr, t0)
        m.add(rm[:k] * np.minimum(sfx.tvec(k, sr) / 0.15, 1.0), t0, 0.8)
    else:
        m.add(rm, slam_t, 0.8)
    if rings is None:
        rings = (2100.0, 2900.0) if c.path.endswith("work1_a.ogg") else ()
    s = mech_slam(sr, rng, dur=0.7, body_f=body_f, tau=0.07, ring_f=rings, ring_tau=0.06, bass=0.5)
    if soft:
        s = sfx.fftfilt(s, sr, lp=5500, order=2)
    m.add(s, slam_t, 1.0)
    if soft:
        m.add(crack_burst(sr, rng, tau=0.004, lo=400, hi=4000, dur=0.02), slam_t, 0.4)
    elif crack:
        m.add(crack_burst(sr, rng, tau=0.004, lo=1000, hi=9000, dur=0.03), slam_t, crack)
    m.add(sub_drop(sr, 120, 42, 0.25, 0.06, k=0.04), slam_t, sub_gain)
    n = N
    # mid churn: darkens quickly
    base = sfx.lp_sweep_noise(n, sr, rng, 4000, 500, hp=40, steps=10)
    base = base * churn(n, sr, rng, 22, 0.6)
    m.add(base * sfx.decay_env(n, sr, 0.45, 1.2, attack=0.01), slam_t, churn_gain * 0.5)
    gr = sfx.band_noise(n, sr, rng, 500, 3000, order=1) * churn(n, sr, rng, 45, 0.8) ** 2
    m.add(gr * sfx.decay_env(n, sr, 0.35, 1.0), slam_t + 0.05, grind * 0.3)
    # low gear-grind: 100-250 Hz noise, irregular modulation, outlasts the mid noise
    lg = sfx.band_noise(n, sr, rng, 90, 260, order=3) * churn(n, sr, rng, 18, 0.5)
    m.add(lg * sfx.decay_env(n, sr, 0.9, 1.9, attack=0.05), slam_t + 0.05, 0.9)
    # sustained low motor bed (25-150 Hz, irregular 8-12 Hz modulation) that runs under the whole sound
    lb = sfx.band_noise(n, sr, rng, 25, 150, order=3) * churn(n, sr, rng, 10, 0.5)
    m.add(lb * sfx.decay_env(n, sr, 1.4, 2.6, attack=0.1), slam_t, 0.5 if not soft else 0.2)
    # servo ticks (the original's early onsets, plus a few after the hit on the _a variant)
    tick_ts = [t for t in tg.onsets if t < slam_t - 0.03]
    if c.path.endswith("work1_a.ogg"):
        tick_ts = [0.07, 0.32, 0.40, 0.55]
    for t in tick_ts:
        m.add(_tick(sr, rng), t, 0.3)
    later = [t for t in tg.onsets if t > slam_t + 0.3][:6]
    for i, t in enumerate(later):
        m.add(_clunk(sr, rng), t, clunks * (1.3 if i == 0 else 0.9))
    return m.out


@recipe("ammopress_work1_*")
def ammopress_work1(c, N):
    return _press(c, N, 0.25, clunks=0.8, churn_gain=0.7)


@recipe("ammopress_work2_*")
def ammopress_work2(c, N):
    tg = get_tg(c)
    slam_t = max(tg.peak_t, 0.18)
    out = _press(c, N, slam_t, rings=(), churn_gain=0.8, grind=0.6, swell=0.0, soft=True,
                 pre_tone=True, sub_gain=0.2, crack=0.0, bed_hi=450)
    # tail rattle: 100-400 Hz noise pulsed at 25-40 Hz plus a small 450 Hz clank
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    n = sfx.n_of(sr, 0.45)
    pulse = 0.5 + 0.5 * np.sin(sfx.TAU * 32 * sfx.tvec(n, sr) + rng.uniform(0, 6))
    m.add(sfx.band_noise(n, sr, rng, 100, 400, order=2) * (0.4 + 0.6 * pulse) * sfx.smooth_env(n, sr, 0.05, 0.25), 0.4, 0.5)
    nk = sfx.n_of(sr, 0.04)
    tk = sfx.tvec(nk, sr)
    m.add(np.sin(sfx.TAU * 450 * tk) * np.exp(-tk / 0.012), 0.4, 0.18)
    return out + fit(m.out, len(out))


@recipe("rc_beep")
def rc_beep(c, N):
    """A short 540 Hz beep: near-constant level with a shallow (about 8 dB) mid dip, a short 1-2 kHz body burst
    and a brief low thump at the start, a 5 ms fade-out.  No bright hiss, almost no 1 kHz partial."""
    sr, rng = c.sr, c.rng
    c.conform_env = False
    m = Mix(c, N)
    d = 0.11
    tn = tone_burst(sr, rng, 540, d, ratios=(1.0,), gains=(1.0,), attack=0.001, release=0.005)
    t = sfx.tvec(len(tn), sr)
    dip = 1.0 - 0.6 * np.exp(-((t - 0.07) / 0.02) ** 2)
    sag = 1.0 - 0.25 * np.clip((t - 0.02) / 0.05, 0, 1)
    m.add(tn * dip * sag, 0.0, 1.0)
    # 1-2 kHz body burst, about 25 ms, roughly 10 dB under the tone
    nb = sfx.n_of(sr, 0.03)
    m.add(sfx.band_noise(nb, sr, rng, 1000, 2000, order=2) * sfx.exp_env(nb, sr, 0.008, attack=0.0004), 0.0, 0.5)
    # short low thump (about 30 ms), well under the tone
    m.add(sub_drop(sr, 250, 120, 0.04, 0.012, k=0.01, attack=0.0005), 0.0, 0.35)
    return m.out


@recipe("rc_warning")
def rc_warning(c, N):
    """One continuous dense buzz: a 250 Hz band-limited saw-like ladder (to ~2.5 kHz at -6 dB/octave) over a sub
    rumble, a 10-15 ms attack at 0.05 s with a sub thump, an inharmonic metallic swell at 0.28-0.42 s, two gated
    3-8 kHz noise bursts, a small pitch blip at 0.73 s with a slow upward glide, and the sub dropping out at
    1.18 s so only the thin 250 Hz tone remains."""
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    t = sfx.tvec(N, sr)
    blip = 0.03 * np.clip((t - 0.71) / 0.02, 0, 1)
    gl = 0.04 * np.clip((t - 0.73) / 0.3, 0, 1)
    f = 250.0 * (1 + blip + gl) * (1 + 0.003 * (churn(N, sr, rng, 3, 1.0) - 0.5))
    ph = sfx.osc_phase(f, sr)
    stack = sfx.harmonic_stack(f, N, sr, nh=5, rolloff=0.8, rng=rng)
    fund = np.sin(ph)
    sw = np.clip((t - 1.18) / 0.06, 0, 1)
    tone = stack * (1 - sw) + fund * sw * 1.2
    att = np.clip((t - 0.05) / 0.012, 0, 1)
    m.add(sfx.fftfilt(tone, sr, hp=70, lp=9000, order=1) * att, 0.0, 1.0)
    # sub rumble: drops out at 1.18 s
    sub = sfx.band_noise(N, sr, rng, 25, 160, order=2) * churn(N, sr, rng, 12, 0.5) * (1 - sw) * att
    m.add(sub, 0.0, 1.2)
    m.add(sub_drop(sr, 100, 60, 0.1, 0.02, k=0.015, attack=0.001), 0.05, 0.9)
    # inharmonic metallic swell, 0.28-0.42 s
    ns = sfx.n_of(sr, 0.14)
    ts = sfx.tvec(ns, sr)
    # noisy (not tonal) metallic swell: 400-2500 Hz noise with a rise-and-fall envelope
    swell = sfx.band_noise(ns, sr, rng, 400, 2500, order=1) * (1 + 0.6 * np.sin(sfx.TAU * 55 * ts))
    m.add(swell * np.sin(np.pi * ts / 0.14) ** 2, 0.28, 1.1)
    # gated noise bursts, 3-8 kHz, pulsed at about 70 Hz
    for t0, d in ((0.18, 0.10), (0.55, 0.25)):
        nn = sfx.n_of(sr, d)
        gate = np.clip(np.sin(sfx.TAU * 70 * sfx.tvec(nn, sr)) * 3, 0, 1)
        m.add(sfx.band_noise(nn, sr, rng, 3000, min(8000, sr * 0.44), order=2) * gate * sfx.smooth_env(nn, sr, 0.01, 0.03), t0, 0.35)
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
@recipe("metaldoor_open_*")
def metaldoor(c, N):
    """Soft latch click (faint ~1.7 kHz ping), small knock only where the original has three onsets, a heavy
    hinge/door-body hit with a short inharmonic ring (no drone), scrape and rattle bridging the events, pulsing
    low thumps, a gliding hinge creak and a smooth thinning scrape tail."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    ons = tg.onsets or [0.05, 0.13, 0.24]
    three = len(ons) > 2
    t_latch = ons[0]
    t_knock = ons[1] if three else 0.13
    t_hit = tg.peak_t if tg.peak_t > 0.1 else ons[-1]
    # latch click: quiet, short, rolled off above ~6 kHz, faint 1.7 kHz ping
    lc = sfx.fftfilt(mech_click(sr, rng, bright=0.9, ring_amp=0.0), sr, lp=6000, order=2)
    m.add(lc, t_latch, 0.3)
    if three:
        # small metallic knock inside the rattle, softened
        nk = sfx.n_of(sr, 0.05)
        kn = sfx.band_noise(nk, sr, rng, 900, 5000, order=1) * sfx.exp_env(nk, sr, 0.008, attack=0.0004)
        m.add(kn, t_knock, 0.4)
    else:
        # the original has a thunk, not a click, before the hit: 200-500 Hz body, about 60 ms decay
        nth = sfx.n_of(sr, 0.3)
        th = sfx.band_noise(nth, sr, rng, 200, 500, order=2) * sfx.exp_env(nth, sr, 0.06, attack=0.001)
        m.add(th, t_latch, 0.75)
    # door body / hinge hit: crack + 150-800 Hz resonant body + short metallic ring
    n = sfx.n_of(sr, 0.6)
    body = sfx.fftfilt(sfx.noise(n, rng), sr, hp=120, lp=900, order=2) * sfx.exp_env(n, sr, 0.09, attack=0.0008)
    ring = sfx.metal_ring(n, sr, rng, rng.uniform(260, 420), 0.05, nm=3)
    m.add(body, t_hit, 1.0)
    m.add(ring, t_hit, 0.3)
    m.add(crack_burst(sr, rng, tau=0.006, lo=800, hi=7000, dur=0.1), t_hit, 0.9)
    if not three:
        # brighter second clunk: 2-6 kHz burst over a longer low-mid thump
        nb = sfx.n_of(sr, 0.05)
        m.add(sfx.band_noise(nb, sr, rng, 2000, 6000, order=2) * sfx.exp_env(nb, sr, 0.01, attack=0.0004), t_hit, 0.7)
        nlt = sfx.n_of(sr, 0.3)
        m.add(sfx.band_noise(nlt, sr, rng, 120, 450, order=2) * sfx.exp_env(nlt, sr, 0.05, attack=0.001), t_hit, 0.5)
    m.add(sub_drop(sr, 120, 60, 0.15, 0.05, k=0.02), t_hit, 0.22)
    # irregular low thumps (40-200 Hz bursts, 50-80 ms each) carrying the low end after the hit
    for dt in (-0.09, 0.0, 0.1, 0.19, 0.31, 0.41):
        tb = t_hit + dt + rng.uniform(-0.015, 0.015)
        if tb > 0.06:
            nlw = sfx.n_of(sr, 0.12)
            lw = sfx.band_noise(nlw, sr, rng, 40, 200, order=2) * sfx.exp_env(nlw, sr, rng.uniform(0.03, 0.05), attack=0.003)
            m.add(lw, tb, 0.5 * rng.uniform(0.6, 1.2))
    # scrape / rattle bridging the events (fills the gap between the first onset and the hit)
    t_s0 = t_latch + 0.02
    ns = sfx.n_of(sr, max(t_hit - t_s0 + 0.35, 0.2))
    scr = sfx.band_noise(ns, sr, rng, 500, 3000, order=2) * churn(ns, sr, rng, 28, 0.9) ** 2
    m.add(scr * sfx.smooth_env(ns, sr, 0.03, 0.25), t_s0, 0.28)
    # rattle after the hit: 300 Hz-3 kHz noise in irregular bursts, thinning
    nr = sfx.n_of(sr, 0.6)
    rat = sfx.band_noise(nr, sr, rng, 300, 3000, order=2) * churn(nr, sr, rng, 22, 1.0) ** 2 * sfx.exp_env(nr, sr, 0.2)
    m.add(rat, t_hit + 0.03, 0.45)
    # hinge creak: noisy 800-1600 Hz band with 10-20 Hz wobble, gone by ~0.8 s
    nc = sfx.n_of(sr, 0.35)
    tc = sfx.tvec(nc, sr)
    wob = 0.5 + 0.5 * np.sin(sfx.TAU * 14 * tc + rng.uniform(0, 6))
    m.add(sfx.band_noise(nc, sr, rng, 700, 1400, order=1) * wob * sfx.smooth_env(nc, sr, 0.05, 0.15), t_hit + 0.08, 0.25)
    # smooth scrape tail, lowpass sweep 2 kHz -> 600 Hz
    nt = sfx.n_of(sr, 0.4)
    m.add(sfx.lp_sweep_noise(nt, sr, rng, 2000, 600, hp=300, steps=8) * sfx.exp_env(nt, sr, 0.15) * churn(nt, sr, rng, 25, 0.8), t_hit + 0.2, 0.22)
    return m.out
