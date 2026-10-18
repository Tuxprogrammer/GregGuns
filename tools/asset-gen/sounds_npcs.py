"""NPC sounds: alien bug, cyberdemon, apache helicopter, sentry turret.

Round-1 rules apply (see sounds_guns.py): noise-based, irregular, naturally decaying.  Vocal sounds use a
*jittered* pulse source (period and level wander randomly) through moving formants plus breath noise and
grit, instead of a steady harmonic stack with fixed-rate modulation (which read as an engine).
"""
from __future__ import annotations

import numpy as np

import sfx
from sndlib import Mix, get_tg, recipe, crack_burst, sub_drop, mech_click, mech_clack
from sndengines import (fit, blast, events, rumble, churn, tone_burst, click_thump, foley_event, rel_env_at, _sum,
                        chug_train, tenth_times, rel_env_at_exact)


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
@recipe("cyberdemon_step_*")
def cyberdemon_step(c, N):
    """Swelling stomp, secondary heel settle, gravel/armor crunch, irregular rumble, clean fading tail."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    surges = tuple((t - 0.015, 0.1, 0.6) for t in tg.onsets if t > 0.2)
    out = blast(c, N, crack_gain=0.35, sub=(75.0, 32.0), sub_gain=1.2, rumble=1.3, churn_rate=22, churn_depth=0.6,
                thunk=0.8, crackle=(70, 800, 6000, 0.3, 0.45), surges=surges, tail_gain=0.5, debris=0.3, bumps=False)
    return out


@recipe("cyberdemon_aggro_*", "cyberdemon_idle_*", "cyberdemon_hurt_*", "cyberdemon_death_*")
def cyberdemon_voice(c, N):
    """Growls and roars.  Phrases follow the original's loudness contour (conform); each phrase onset gets a
    sharp breathy accent; deeper and rougher for idle/death, higher with a snarl for hurt/aggro."""
    tg = get_tg(c)
    sr, rng, nm = c.sr, c.rng, c.name
    m = Mix(c, N)
    heavy = "idle" in nm or "death" in nm
    f_a = rng.uniform(60, 80) if heavy else rng.uniform(90, 125)
    f_b = f_a * (0.55 if "death" in nm else 0.8)
    vow = ("o", "a") if "idle" in nm else ("a", "u") if "death" in nm else ("ae", "o")
    # split into phrases at the quiet spots of the original's contour
    d = tg.end_t
    phrases = [(0.0, d)]
    if tg.onsets and len(tg.onsets) >= 3 and d > 1.2:
        gaps = [(t, t) for t in tg.onsets[1:3]]
        phrases = [(0.0, tg.onsets[1] if tg.onsets[1] > 0.4 else d * 0.5), (tg.onsets[1] if tg.onsets[1] > 0.4 else d * 0.5, d)]
    for (t0, t1) in phrases:
        dd = max(t1 - t0, 0.25)
        g = growl(c, N, dd, f_a * rng.uniform(0.9, 1.1), f_b * rng.uniform(0.9, 1.1), vowels=vow,
                  breath=0.55 if "aggro" in nm or "hurt" in nm else 0.4, grit=1.0, am_rate=float(rng.uniform(9, 16)),
                  am_depth=0.6, formant_wander=0.15)
        m.add(g * sfx.smooth_env(len(g), sr, min(0.08, dd * 0.2), min(0.3, dd * 0.4)), t0, 1.0)
    # breathy attack accents at the original's onsets
    for t in tg.onsets[:6]:
        n = sfx.n_of(sr, 0.14)
        m.add(sfx.band_noise(n, sr, rng, 600, 8000, order=1) * sfx.exp_env(n, sr, 0.04, attack=0.002), t, 0.5)
        m.add(sub_drop(sr, 110, 55, 0.2, 0.06, k=0.03), t, 0.25)
    # chest layer
    nb = sfx.n_of(sr, d)
    m.add(sfx.band_noise(nb, sr, rng, 40, 320, order=2) * churn(nb, sr, rng, 8, 0.7), 0.0, 0.35)
    return m.out


# =========================================================================== alien bug
def _rasp(c, dur, f_lo=1500, f_hi=3500, grit=0.8, rate=45, depth=0.8):
    """Raspy screech: noise through a sliding formant band with irregular flutter (no whistle, no sine)."""
    sr, rng = c.sr, c.rng
    n = sfx.n_of(sr, dur)
    src = sfx.noise(n, rng)
    a = sfx.formant_filter(src, sr, [(f_lo, 500), (f_lo * 2.0, 700)])
    b = sfx.formant_filter(src, sr, [(f_hi, 500), (f_hi * 1.6, 800)])
    w = np.linspace(0, 1, n)
    y = a * (1 - w) + b * w
    y = y / (np.std(y) + 1e-9)
    y = np.tanh(y * (1 + 2.5 * grit))
    return y * churn(n, sr, rng, rate, depth)


@recipe("alienbug_*")
def alienbug(c, N):
    """Chitinous clicks and snaps at the original's onsets over a dense irregular chitter bed; raspy screech for
    aggro/hit, wet crackle for death, skittering ticks for steps."""
    tg = get_tg(c)
    sr, rng, nm = c.sr, c.rng, c.name
    m = Mix(c, N)
    kind = nm.split("_")[1][:5]
    ons = list(tg.onsets) if tg.onsets else [0.05]
    big = min(range(len(ons)), key=lambda i: abs(ons[i] - tg.peak_t)) if ons else 0
    # 1. discrete clicks / snaps
    for i, t in enumerate(ons):
        lvl = 10 ** (max(rel_env_at(tg, t), -30) * 0.5 / 20)
        k = "clack" if (i == big and kind in ("step", "bite", "aggro")) else ("click" if rng.random() < 0.6 else "tick")
        m.add(foley_event(sr, rng, k, 1.0, 1.4), t, 0.6 * lvl + 0.15)
        if kind in ("bite", "step", "hit-0") or i == big:
            m.add(crack_burst(sr, rng, tau=0.005, lo=1500, hi=12000, dur=0.06), t, 0.45)
    c.env_smooth = 0.02
    for t in tenth_times(tg):  # a loud original frame here means an event here
        if rel_env_at_exact(tg, t) > -16 and all(abs(t - o) > 0.045 for o in ons):
            m.add(foley_event(sr, rng, "click", 1.0, 1.4), max(t - 0.004, 0.0), 0.5 * 10 ** (rel_env_at_exact(tg, t) * 0.5 / 20) + 0.1)
    # a snap at t=0 where the original has one (steps)
    if kind == "step" and tg.peak_t < 0.02:
        m.add(crack_burst(sr, rng, tau=0.006, lo=1200, hi=13000, dur=0.08), 0.0, 1.0)
    # 2. chitter / skitter bed following the loudness contour
    ts = np.arange(N) / sr
    lv = np.array([rel_env_at(tg, t) for t in ts[::256]])
    dens = np.interp(ts, ts[::256], 15 + 90 * 10 ** (np.maximum(lv, -45) / 20.0))
    dens = np.where(ts <= tg.dur, dens, 0.0)
    bed = sfx.grain_cloud(N, sr, rng, np.maximum(dens, 0.5), 0.0025, 2500 if kind != "death" else 1500, 11000, amp_var=0.9)
    gate = 10 ** (np.interp(ts, ts[::256], np.maximum(lv, -60)) / 20.0)
    m.add(bed * gate, 0.0, 0.8)
    # 3. raspy screech (aggro / hit) and wet crackle (death / hit)
    if kind == "aggro":
        d = min(0.5, tg.end_t * 0.5)
        m.add(_rasp(c, d, 1500, 2800) * sfx.smooth_env(sfx.n_of(sr, d), sr, 0.03, 0.2), max(tg.onsets[0] if tg.onsets else 0.05, 0.05), 0.6)
        nl = sfx.n_of(sr, d)
        m.add(sfx.band_noise(nl, sr, rng, 120, 450, order=2) * churn(nl, sr, rng, 25, 0.8) * sfx.smooth_env(nl, sr, 0.05, 0.2), 0.05, 0.4)
    elif kind == "hit-0" or nm.startswith("alienbug_hit"):
        d = 0.3
        m.add(_rasp(c, d, 2000, 4500, rate=60) * sfx.exp_env(sfx.n_of(sr, d), sr, 0.1), 0.0, 0.5)
    elif kind == "death":
        nq = sfx.n_of(sr, min(1.2, tg.end_t))
        m.add(sfx.band_noise(nq, sr, rng, 600, 6000, order=1) * churn(nq, sr, rng, 30, 0.9) ** 2 * sfx.decay_env(nq, sr, 0.35, 0.9), 0.05, 0.6)
        if tg.band("sub") > 0.1:
            m.add(sub_drop(sr, 120, 50, 0.4, 0.12, k=0.04), 0.0, 0.9)
        nh = sfx.n_of(sr, tg.end_t)
        m.add(sfx.band_noise(nh, sr, rng, 6000, 16000, order=1) * sfx.decay_env(nh, sr, 0.4, 1.0), 0.0, 0.3)
    return m.out


# =========================================================================== apache
def _chops(c, n, rate, jitter, lo=40, hi=250, amp_var=0.35, tau=0.03):
    return sfx.pulse_train(n, c.sr, c.rng, rate, jitter=jitter, tau=tau, bp=(lo, hi), amp_var=amp_var)


@recipe("apache_rotor")
def apache_rotor(c, N):
    """Blade chop at ~11.5 Hz with irregular flutter over a dense low-mid body and a faint engine hum."""
    sr, rng = c.sr, c.rng
    c.looping = True
    m = Mix(c, N)
    m.add(_chops(c, N, 11.5, 0.08, 50, 400, 0.35, 0.03) * 3.0, 0.0, 1.0)
    m.add(sfx.band_noise(N, sr, rng, 150, 900, order=2) * churn(N, sr, rng, 7, 0.45), 0.0, 0.9)
    m.add(sfx.band_noise(N, sr, rng, 700, 2500, order=1) * churn(N, sr, rng, 9, 0.8), 0.0, 0.1)
    f = np.full(N, 180.0) * (1 + 0.01 * (churn(N, sr, rng, 2, 1.0) - 0.5))
    m.add(sfx.fftfilt(sfx.harmonic_stack(f, N, sr, nh=6, rolloff=1.0, rng=rng), sr, lp=1200, order=1), 0.0, 0.12)
    return m.out


@recipe("apache_burst")
def apache_burst(c, N):
    """About ten hard rotor chops over 0.85 s with a thump on each, an abrupt drop, then a dark rumble tail
    with a couple of crackle onsets."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    t = tg.peak_t * 0.0
    k = 10
    per = 0.85 / k
    for i in range(k):
        g = rng.uniform(0.8, 1.0)
        tt = i * per + rng.uniform(-0.005, 0.005)
        nb = sfx.n_of(sr, 0.075)
        m.add(sfx.dark_noise(nb, sr, rng, 4500, 500, steps=5, hp=120) * sfx.exp_env(nb, sr, 0.03, attack=0.008), tt, 0.9 * g)
        m.add(sub_drop(sr, 120, 45, 0.15, 0.05, k=0.03, attack=0.006), tt, 1.0 * g)
    m.add(crack_burst(sr, rng, tau=0.01, lo=600, hi=9000, dur=0.1), 0.0, 0.7)
    nbed = sfx.n_of(sr, 0.9)
    m.add(sfx.band_noise(nbed, sr, rng, 100, 6000, order=1) * (0.5 + 0.5 * churn(nbed, sr, rng, 12, 1.0)) * sfx.smooth_env(nbed, sr, 0.01, 0.05), 0.0, 0.5)
    nt = N
    m.add(sfx.dark_noise(nt, sr, rng, 900, 120, steps=6, hp=30) * sfx.decay_env(nt, sr, 0.5, 1.2) * churn(nt, sr, rng, 10, 0.5), 0.85, 0.7)
    for tc in (1.33, 1.72):
        nc = sfx.n_of(sr, 0.05)
        m.add(sfx.band_noise(nc, sr, rng, 1000, 4000, order=1) * sfx.exp_env(nc, sr, 0.012, attack=0.0005), tc, 0.25)
    return m.out


@recipe("apache_explode", "apache_hit_*")
def apache_blast(c, N):
    """Fireball crack, dark exponential-decay roar with irregular debris crackles/thumps, progressive darkening."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    big = "explode" in c.name
    m = Mix(c, N)
    pops = [t for t in tg.onsets if t > 0.3][:24]
    out = blast(c, N, crack_gain=1.1 if big else 0.8, sub=(80.0, 34.0), sub_gain=1.1, rumble=1.3, churn_rate=9,
                churn_depth=0.5, crackle=(40, 500, 4000, min(tg.end_t, 5.0), 0.5), pops=pops, pop_gain=0.4,
                tail_gain=0.9, debris=0.7, bumps=False)
    return out


@recipe("apache_die")
def apache_die(c, N):
    """Slow swell to a distorted peak at ~0.44 s, three buzzy harmonic pulses, a thin high whine fading by ~3 s,
    a mid-level crackling plateau."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    out = blast(c, N, crack_gain=0.6, sub=(90.0, 40.0), sub_gain=0.9, rumble=0.9, churn_rate=12, churn_depth=0.5,
                crackle=(60, 1500, 8000, 3.0, 0.5), tail_gain=0.9, debris=0.5, bumps=False)
    m = Mix(c, N)
    for t in (0.1, 0.5, 0.9):
        b = tone_burst(sr, rng, rng.uniform(900, 1300), 0.22, ratios=(1, 2, 3, 4, 5), gains=(1, 0.8, 0.6, 0.5, 0.4),
                       attack=0.004, release=0.05, grit=0.2)
        m.add(b, t, 0.35)
    nw = sfx.n_of(sr, 3.0)
    tw = sfx.tvec(nw, sr)
    m.add(np.sin(sfx.TAU * 6000 * tw) * np.exp(-tw / 1.2) * churn(nw, sr, rng, 4, 0.4), 0.2, 0.04)
    return out + fit(m.out, len(out))


# =========================================================================== turret
# per variant: list of (t, dur, f, ratios, gains) beeps; extras handled below
def _beep(sr, rng, f, d, ratios, gains, glide=None, release=0.003, grit=0.0, flutter=0.0):
    return tone_burst(sr, rng, f, d, ratios=ratios, gains=gains, release=release, grit=grit, glide=glide, flutter=flutter)


@recipe("turret_beep-*")
def turret_beep(c, N):
    """Electronic chirps: click-sharp attacks with a small thump, glassy partials, gated bursts (see per-variant
    structure from the original's onsets)."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    k = int(c.name[-2:])
    m = Mix(c, N)
    c.conform_env = False   # the gating pattern is built explicitly below
    ons = tg.onsets
    if k == 1:  # long chatter: many short chirps
        for t in ons:
            f = float(rng.choice([1031, 2742, 7266]) * rng.uniform(0.98, 1.02))
            m.add(_beep(sr, rng, f, rng.uniform(0.04, 0.08), (1.0, 2.0, 3.9), (1.0, 0.4, 0.2)), t, rng.uniform(0.5, 1.0))
            m.add(click_thump(sr, rng, 0.7, 0.3, bright=1.0), t, 0.4)
    elif k == 2:  # two clusters, second louder; faint echo blip after each beep
        for t0, g in ((0.0, 0.5), (0.36, 1.0)):
            for j in range(2):
                t = t0 + j * 0.065
                m.add(_beep(sr, rng, 1200, 0.045, (1.0, 3.3, 6.6, 7.7), (1.0, 0.5, 0.35, 0.3)), t, g)
                m.add(_beep(sr, rng, 1200, 0.03, (1.0, 3.3, 6.6), (1.0, 0.4, 0.3)), t + 0.025, g * 0.2)
                m.add(click_thump(sr, rng, 0.8, 0.5, bright=1.0), t, g * 0.5)
    elif k == 3:  # fast gated stutter of ~5.9 kHz pulses that shrink in level and spacing
        t = 0.0
        sp = 0.1
        for j in range(7):
            m.add(_beep(sr, rng, 5900, 0.04, (1.0, 2.1), (1.0, 0.15)), t, 1.0 - 0.1 * j)
            m.add(_beep(sr, rng, 500, 0.04, (1.0, 2.0), (1.0, 0.5)), t, 0.1)
            t += sp
            sp *= 0.93
        m.add(_beep(sr, rng, 5900, 0.06, (1.0, 2.1), (1.0, 0.2)), 0.57, 1.0)
        m.add(click_thump(sr, rng, 0.8, 0.0, bright=2.0), 0.0, 0.5)
        m.add(click_thump(sr, rng, 0.8, 0.0, bright=1.5), 0.53, 0.4)
    elif k == 4:  # 12-13 pulses, ~40 ms on / 40 ms off, buzzy 1.2 kHz body with 4 / 7.9 / 9.3 kHz partials
        for j in range(13):
            t = 0.0 + j * 0.0415
            f = 1200 * rng.uniform(0.9, 1.1)
            m.add(_beep(sr, rng, f, 0.022, (1.0, 3.3, 6.6, 7.7), (1.0, 0.8, 0.6, 0.5), grit=0.1), t, 0.9)
            m.add(click_thump(sr, rng, 0.5, 0.0, bright=1.4), t, 0.4)
        m.add(_beep(sr, rng, 1200, 0.03, (1.0, 3.3, 6.6), (1.0, 0.6, 0.4)), 0.52, 0.15)
    elif k == 5:  # steady bright tone ~5.3 kHz with 7.85 and 15.5 kHz partials, hard end
        m.add(_beep(sr, rng, 5300, 0.275, (1.0, 1.48, 2.94), (1.0, 0.3, 0.2), release=0.004), 0.0, 1.0)
    elif k == 6:  # four-step ascending chirp 1.5 -> 5 kHz in ~70 ms, click, quiet ringing tail
        for j, f in enumerate((1500, 2400, 3500, 5000)):
            m.add(_beep(sr, rng, f, 0.016, (1.0, 2.0), (1.0, 0.2), release=0.002), 0.0 + j * 0.017, 1.0)
        m.add(click_thump(sr, rng, 0.7, 0.0, bright=1.0), 0.0, 0.5)
        nr = sfx.n_of(sr, 0.22)
        tr = sfx.tvec(nr, sr)
        ring = sum(np.sin(sfx.TAU * f * tr) for f in (1500, 3000, 5000)) * np.exp(-tr / 0.08)
        m.add(ring, 0.07, 0.015)
    else:  # 7: ~16 kHz tone with ~30 Hz irregular gating into 4-5 bursts, abrupt end, faint tail
        n = sfx.n_of(sr, 0.16)
        tt = sfx.tvec(n, sr)
        tone = np.sin(sfx.TAU * 16200 * tt) + 0.5 * np.sin(sfx.TAU * 13100 * tt) + 0.4 * np.sin(sfx.TAU * 9600 * tt)
        gate = (churn(n, sr, rng, 30, 1.0) > 0.45).astype(float)
        gate = np.convolve(gate, np.hanning(41) / np.hanning(41).sum(), mode="same")
        m.add(tone * gate, 0.0, 1.0)
        nr = sfx.n_of(sr, 0.1)
        m.add(sfx.band_noise(nr, sr, rng, 9000, 17000, order=1) * sfx.exp_env(nr, sr, 0.03), 0.16, 0.08)
    return sfx.fftfilt(m.out, sr, hp=350, order=2)


@recipe("turret_gunequip-*")
def turret_gunequip(c, N):
    """Ratchet of metallic clacks and ticks, a louder cluster, a heavy bolt-slam clunk with a short thud; noisy,
    dry, bright; no tones."""
    return events(c, N, mix=(("clack", 0.45), ("click", 0.35), ("tick", 0.2)), bright=1.35, rattle=0.9, rustle=0.0,
                  peak_kind="slam", level_pow=0.7, slam_bass=(0.02, 0.08))
