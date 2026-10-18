"""Procedural SFX building blocks for GregGuns (numpy only, no scipy).

Everything here is original synthesis: noise, oscillators, FFT-domain filters and
synthetic impulse responses.  No recorded audio is read or resampled.

All functions take a sample rate ``sr`` and a ``numpy.random.Generator`` ``rng`` where
they need randomness, so a fixed seed reproduces a sound exactly.  Signals are float64
mono arrays unless stated otherwise.
"""
from __future__ import annotations

import numpy as np

TAU = 2.0 * np.pi


# --------------------------------------------------------------------------- basics
def n_of(sr, seconds):
    return max(1, int(round(seconds * sr)))


def tvec(n, sr):
    return np.arange(n) / sr


def noise(n, rng):
    return rng.standard_normal(n)


def fftfilt(x, sr, hp=None, lp=None, order=2, bp=None):
    """Zero-phase Butterworth-magnitude filter applied in the frequency domain.

    hp / lp are corner frequencies in Hz; bp=(lo, hi) is shorthand for both.
    """
    if bp is not None:
        hp, lp = bp
    n = len(x)
    if n == 0:
        return x
    nfft = 1 << int(np.ceil(np.log2(n + 8192)))
    X = np.fft.rfft(x, nfft)
    f = np.fft.rfftfreq(nfft, 1.0 / sr)
    H = np.ones_like(f)
    if lp:
        H = H / np.sqrt(1.0 + (f / lp) ** (2 * order))
    if hp:
        ff = np.maximum(f, 1e-3)
        H = H / np.sqrt(1.0 + (hp / ff) ** (2 * order))
    return np.fft.irfft(X * H, nfft)[:n]


def exp_env(n, sr, tau, attack=0.0):
    """exp(-t/tau) with an optional linear attack (seconds)."""
    t = tvec(n, sr)
    e = np.exp(-t / max(tau, 1e-4))
    if attack > 0:
        e = e * np.minimum(t / attack, 1.0)
    return e


def smooth_env(n, sr, attack, release):
    """Raised-cosine fade in/out envelope that sums to a swelling bump."""
    e = np.ones(n)
    a = min(n_of(sr, attack), n // 2)
    r = min(n_of(sr, release), n // 2)
    if a > 1:
        e[:a] = 0.5 - 0.5 * np.cos(np.linspace(0, np.pi, a))
    if r > 1:
        e[-r:] *= 0.5 + 0.5 * np.cos(np.linspace(0, np.pi, r))
    return e


def place(out, sig, t, sr, gain=1.0):
    """Add sig into out starting at time t (seconds)."""
    i = int(round(t * sr))
    if i >= len(out) or i + len(sig) <= 0:
        return out
    s0 = max(0, -i)
    s1 = min(len(sig), len(out) - i)
    out[i + s0:i + s1] += sig[s0:s1] * gain
    return out


def osc_phase(freq, sr):
    return np.cumsum(TAU * freq / sr)


def sine(freq, n, sr, phase0=0.0):
    f = np.broadcast_to(np.asarray(freq, dtype=float), (n,))
    return np.sin(osc_phase(f, sr) + phase0)


def sweep(f0, f1, n, sr, curve=3.0, k=None):
    """Exponential-ish sweep f0 -> f1 (shape set by curve; k = time constant in s)."""
    t = tvec(n, sr)
    d = n / sr
    if k is None:
        k = d / curve
    w = np.exp(-t / k)
    f = f1 + (f0 - f1) * w
    return osc_phase(f, sr)


def harmonic_stack(f0, n, sr, nh=12, rolloff=1.0, rng=None, detune=0.0):
    f = np.broadcast_to(np.asarray(f0, dtype=float), (n,))
    ph = osc_phase(f, sr)
    out = np.zeros(n)
    for k in range(1, nh + 1):
        fk = f * k
        a = 1.0 / (k ** rolloff)
        sig = np.sin(k * ph + (rng.uniform(0, TAU) if rng is not None else 0))
        out += a * np.where(fk < sr * 0.45, sig, 0.0)
    return out


def tilt(x, sr, alpha, pivot=1000.0):
    """Spectral tilt: gain proportional to (f/pivot)**alpha (energy preserved roughly)."""
    n = len(x)
    nfft = 1 << int(np.ceil(np.log2(n + 8192)))
    X = np.fft.rfft(x, nfft)
    f = np.maximum(np.fft.rfftfreq(nfft, 1.0 / sr), 30.0)
    H = (f / pivot) ** alpha
    return np.fft.irfft(X * H, nfft)[:n]


def centroid(x, sr):
    n = len(x)
    nfft = 1 << int(np.ceil(np.log2(max(n, 256))))
    S = np.abs(np.fft.rfft(x, nfft)) ** 2
    f = np.fft.rfftfreq(nfft, 1.0 / sr)
    return float((S * f).sum() / max(S.sum(), 1e-20))


def match_centroid(x, sr, target, lo=-1.4, hi=1.4, tol=0.07):
    """Apply a spectral tilt so the spectral centroid approaches ``target`` Hz.

    Bisection on the tilt exponent; bounded so that a wrong cue cannot wreck the sound.
    """
    c0 = centroid(x, sr)
    if c0 <= 0 or target <= 0 or abs(np.log(c0 / target)) < tol:
        return x
    a_lo, a_hi = lo, hi
    best = x
    for _ in range(9):
        a = 0.5 * (a_lo + a_hi)
        y = tilt(x, sr, a)
        c = centroid(y, sr)
        best = y
        if abs(np.log(c / target)) < tol:
            break
        if c < target:
            a_lo = a
        else:
            a_hi = a
    # keep original RMS so the mix balance is not disturbed
    r0 = np.sqrt(np.mean(x ** 2) + 1e-20)
    r1 = np.sqrt(np.mean(best ** 2) + 1e-20)
    return best * (r0 / r1)


# --------------------------------------------------------------------------- noise beds
def dark_noise(n, sr, rng, f_start, f_end, steps=6, hp=None):
    """White noise whose low-pass corner glides f_start -> f_end (Hz) over the length."""
    base = noise(n, rng)
    fs = np.geomspace(max(f_start, 60), max(f_end, 60), steps)
    bank = [fftfilt(base, sr, hp=hp, lp=f, order=2) for f in fs]
    pos = np.linspace(0, steps - 1, n)
    out = np.zeros(n)
    for i, b in enumerate(bank):
        w = np.clip(1 - np.abs(pos - i), 0, 1)
        out += b * w
    return out


def band_noise(n, sr, rng, lo, hi, order=2):
    return fftfilt(noise(n, rng), sr, hp=lo, lp=hi, order=order)


def rand_mod(n, sr, rng, rate, depth=1.0):
    """Smooth random amplitude modulation in [1-depth, 1]."""
    k = max(4, int(n / sr * rate * 4) + 4)
    pts = rng.random(k)
    x = np.interp(np.linspace(0, k - 1, n), np.arange(k), pts)
    return 1.0 - depth * (1 - x)


def pulse_train(n, sr, rng, rate, jitter=0.3, tau=0.004, bp=(1500, 6000), amp_var=0.5):
    """Train of tiny noise grains (crackle, rattle, flutter)."""
    out = np.zeros(n)
    t = 0.0
    dur = n / sr
    glen = n_of(sr, tau * 6)
    while t < dur:
        g = noise(glen, rng) * exp_env(glen, sr, tau)
        g = fftfilt(g, sr, hp=bp[0], lp=bp[1], order=1)
        place(out, g, t, sr, 1 - amp_var * rng.random())
        t += (1.0 / rate) * (1 + jitter * (rng.random() * 2 - 1))
    return out


# --------------------------------------------------------------------------- transients
def click(sr, rng, bright=1.0, dur=0.006):
    n = n_of(sr, dur * 4)
    g = noise(n, rng) * exp_env(n, sr, dur * 0.5)
    g = fftfilt(g, sr, hp=2200 * bright, lp=14000, order=2)
    ring = np.sin(TAU * rng.uniform(2800, 4200) * bright * tvec(n, sr)) * exp_env(n, sr, 0.003)
    return g + 0.25 * ring


def metal_ring(n, sr, rng, f0, tau, nm=4, spread=1.0):
    """Inharmonic bar-like partials."""
    ratios = np.array([1.0, 2.76, 5.40, 8.93, 13.3, 18.6])[:nm]
    t = tvec(n, sr)
    out = np.zeros(n)
    for i, r in enumerate(ratios):
        f = f0 * (r ** spread) * rng.uniform(0.97, 1.03)
        if f > sr * 0.45:
            continue
        out += np.sin(TAU * f * t + rng.uniform(0, TAU)) * np.exp(-t / (tau / (1 + 0.6 * i))) / (1 + 0.5 * i)
    return out


def clack(sr, rng, f0=None, bright=1.0, tau=0.035, dur=0.12):
    """Metal-on-metal clack: noise snap plus bar partials."""
    n = n_of(sr, dur)
    f0 = f0 or rng.uniform(900, 1700)
    sn = noise(n, rng) * exp_env(n, sr, 0.004)
    sn = fftfilt(sn, sr, hp=1200 * bright, lp=9000 * bright, order=2)
    return sn * 0.9 + 0.55 * metal_ring(n, sr, rng, f0, tau)


def thunk(sr, rng, f0=110.0, tau=0.07, dur=0.25, grit=0.4):
    """Low body hit: pitch-dropping sine plus filtered noise puff."""
    n = n_of(sr, dur)
    ph = sweep(f0 * 1.7, f0, n, sr, k=0.025)
    body = np.sin(ph) * exp_env(n, sr, tau, attack=0.0015)
    puff = fftfilt(noise(n, rng), sr, hp=80, lp=900, order=2) * exp_env(n, sr, tau * 0.6)
    return body + grit * puff


def snap(sr, rng, lo=900, hi=7000, tau=0.012, dur=0.08):
    n = n_of(sr, dur)
    return fftfilt(noise(n, rng), sr, hp=lo, lp=hi, order=2) * exp_env(n, sr, tau)


def crack(sr, rng, tau=0.012, bright=1.0, dur=0.12):
    """Muzzle crack: very short broadband burst, slightly shaped."""
    n = n_of(sr, dur)
    x = noise(n, rng) * exp_env(n, sr, tau)
    x = fftfilt(x, sr, hp=500, lp=min(16000 * bright, sr * 0.45), order=1)
    return x


def boom(sr, rng, dur=0.6, f0=95.0, tau=0.18, grit=0.5, sub=True):
    """Low boom: pitch-down sub + low-passed noise body."""
    n = n_of(sr, dur)
    ph = sweep(f0 * 2.2, f0 * 0.55, n, sr, k=tau * 0.5)
    s = np.sin(ph) * exp_env(n, sr, tau, attack=0.002)
    body = dark_noise(n, sr, rng, 1800, 180, hp=35) * exp_env(n, sr, tau * 1.1, attack=0.001)
    out = body * grit * 2.0 + (s * 1.1 if sub else 0)
    return out


def thump_sub(sr, f0, dur, tau):
    n = n_of(sr, dur)
    ph = sweep(f0 * 1.8, f0, n, sr, k=0.04)
    return np.sin(ph) * exp_env(n, sr, tau, attack=0.003)


# --------------------------------------------------------------------------- reverb
def make_ir(sr, rng, rt60, bright=0.5, length=None):
    length = length or min(rt60 * 1.15, 4.0)
    n = n_of(sr, length)
    t = tvec(n, sr)
    hi = fftfilt(noise(n, rng), sr, hp=1500, order=1) * np.exp(-6.9 * t / (rt60 * 0.45))
    lo = fftfilt(noise(n, rng), sr, lp=1200, order=1) * np.exp(-6.9 * t / rt60)
    ir = bright * hi + lo
    # a few sparse early reflections
    for _ in range(6):
        i = int(rng.uniform(0.004, 0.05) * sr)
        if i < n:
            ir[i] += rng.choice([-1, 1]) * rng.uniform(0.6, 1.4)
    ir[0] = 0
    ir /= np.sqrt(np.sum(ir ** 2)) + 1e-12
    return ir


def reverb(x, sr, rng, rt60=0.5, wet=0.25, bright=0.4, predelay=0.004, tail=None):
    """FFT convolution with a synthetic impulse response. Output may be longer."""
    ir = make_ir(sr, rng, rt60, bright)
    pd = n_of(sr, predelay)
    ir = np.concatenate([np.zeros(pd), ir])
    n = len(x) + len(ir) + (n_of(sr, tail) if tail else 0)
    nfft = 1 << int(np.ceil(np.log2(n)))
    y = np.fft.irfft(np.fft.rfft(x, nfft) * np.fft.rfft(ir, nfft), nfft)[:max(len(x), n - 1)]
    out = np.zeros(len(y))
    out[:len(x)] += x
    rx = np.sqrt(np.mean(x ** 2) + 1e-20)
    ry = np.sqrt(np.mean(y ** 2) + 1e-20)
    out += y * (rx / ry) * wet * 2.5
    return out


# --------------------------------------------------------------------------- tonal / electronic
def beep(sr, f, dur, level=1.0, attack=0.003, release=0.01, harm=0.0):
    n = n_of(sr, dur)
    s = np.sin(TAU * f * tvec(n, sr))
    if harm:
        s += harm * np.sin(TAU * 3 * f * tvec(n, sr)) / 3
    return s * smooth_env(n, sr, attack, release) * level


def fm_zap(sr, rng, dur, f0, f1, ratio=1.5, index=4.0, tau=None, ring=0.0):
    """Laser/plasma tone: FM carrier gliding f0 -> f1, optional ring-mod."""
    n = n_of(sr, dur)
    tau = tau or dur * 0.4
    cph = sweep(f0, f1, n, sr, k=dur * 0.25)
    mod = np.sin(cph * ratio)
    idx = index * np.exp(-tvec(n, sr) / (dur * 0.5))
    s = np.sin(cph + idx * mod)
    if ring:
        s = (1 - ring) * s + ring * s * np.sin(TAU * f1 * 0.5 * tvec(n, sr))
    return s * exp_env(n, sr, tau, attack=0.002)


def electric_buzz(n, sr, rng, f0=120.0, crackle=0.5, level_mod=0.4):
    """50/60-style hum with strong harmonics, flicker and crackle."""
    flick = rand_mod(n, sr, rng, 18, level_mod)
    h = harmonic_stack(f0 * (1 + 0.01 * rand_mod(n, sr, rng, 6, 1.0)), n, sr, nh=24, rolloff=0.8, rng=rng)
    cr = pulse_train(n, sr, rng, 120, tau=0.002, bp=(2500, 9000)) * crackle
    return h * flick * 0.6 + cr


def servo_whine(n, sr, rng, f0, f1, wobble=0.02):
    t = tvec(n, sr)
    f = np.linspace(f0, f1, n) * (1 + wobble * np.sin(TAU * 7 * t))
    s = np.sin(osc_phase(f, sr)) + 0.4 * np.sin(2 * osc_phase(f, sr)) + 0.2 * np.sin(3 * osc_phase(f, sr))
    return s


def engine(n, sr, rng, f0=70.0, rough=0.5, nh=30):
    """Piston/engine-like harmonic buzz with random cycle roughness."""
    jit = 1 + 0.03 * rough * (rand_mod(n, sr, rng, 9, 1.0) - 0.5) * 2
    h = harmonic_stack(f0 * jit, n, sr, nh=nh, rolloff=0.9, rng=rng)
    am = 0.65 + 0.35 * np.sin(osc_phase(np.full(n, f0 * 0.5), sr))
    gr = fftfilt(noise(n, rng), sr, hp=150, lp=2500, order=1) * 0.35 * rand_mod(n, sr, rng, 30, 0.8)
    return h * am + gr


# --------------------------------------------------------------------------- voice-ish
VOWELS = {
    "a": [(800, 90), (1200, 110), (2600, 160)],
    "o": [(500, 80), (900, 90), (2500, 150)],
    "u": [(330, 70), (800, 80), (2400, 140)],
    "e": [(500, 80), (1800, 120), (2600, 160)],
    "i": [(300, 60), (2300, 130), (3000, 170)],
    "ae": [(700, 90), (1700, 120), (2600, 160)],
}


def formant_filter(x, sr, formants, floor=0.06):
    n = len(x)
    nfft = 1 << int(np.ceil(np.log2(n + 8192)))
    X = np.fft.rfft(x, nfft)
    f = np.fft.rfftfreq(nfft, 1.0 / sr)
    H = np.full_like(f, floor)
    for fc, bw in formants:
        H += 1.0 / (1.0 + ((f - fc) / bw) ** 2)
    return np.fft.irfft(X * H, nfft)[:n]


def glottal(f_curve, n, sr, rng, rasp=0.3, tight=1.0):
    """Buzz source: rich harmonic pulse + aspiration noise (for growls, screeches)."""
    f = np.broadcast_to(np.asarray(f_curve, dtype=float), (n,)).copy()
    f *= 1 + 0.015 * (rand_mod(n, sr, rng, 25, 1.0) - 0.5)
    src = harmonic_stack(f, n, sr, nh=40, rolloff=0.7 * tight, rng=rng)
    asp = fftfilt(noise(n, rng), sr, hp=400, lp=5000, order=1)
    return src + rasp * asp * (0.4 + 0.6 * rand_mod(n, sr, rng, 40, 1.0))


def voice(sr, rng, dur, f0a, f0b, vow_a="a", vow_b="o", rasp=0.3, dist=0.0, shape="bump", tight=1.0):
    n = n_of(sr, dur)
    t = tvec(n, sr)
    f = np.linspace(f0a, f0b, n) * (1 + 0.04 * np.sin(TAU * rng.uniform(4, 8) * t))
    src = glottal(f, n, sr, rng, rasp=rasp, tight=tight)
    h = n // 2
    a = formant_filter(src, sr, VOWELS[vow_a])
    b = formant_filter(src, sr, VOWELS[vow_b])
    w = np.linspace(0, 1, n)
    y = a * (1 - w) + b * w
    y /= np.max(np.abs(y)) + 1e-9
    if dist:
        y = np.tanh(y * (1 + dist * 6)) / np.tanh(1 + dist * 6)
    if shape == "bump":
        env = np.sin(np.pi * np.linspace(0.02, 0.98, n)) ** 0.7
    elif shape == "decay":
        env = np.exp(-t / (dur * 0.45)) * np.minimum(t / 0.01, 1)
    else:
        env = np.ones(n)
    return y * env


# --------------------------------------------------------------------------- finishing
def fade(x, sr, fin=0.002, fout=0.02):
    x = x.copy()
    a = min(n_of(sr, fin), len(x) // 2)
    r = min(n_of(sr, fout), len(x) // 2)
    if a > 1:
        x[:a] *= np.linspace(0, 1, a)
    if r > 1:
        x[-r:] *= np.linspace(1, 0, r) ** 1.5
    return x


def make_loop(x, sr, xf):
    """Turn x (length N+xf) into a seamless loop of length N: the tail is
    cross-faded (equal power) into the head."""
    xfn = n_of(sr, xf)
    N = len(x) - xfn
    head = x[:xfn]
    tail = x[N:N + xfn]
    w = np.linspace(0, np.pi / 2, xfn)
    mixed = head * np.sin(w) + tail * np.cos(w)
    out = x[:N].copy()
    out[:xfn] = mixed
    return out


def soft_clip(x, ceiling):
    """tanh-style limiter that approaches ``ceiling`` (linear) asymptotically."""
    return ceiling * np.tanh(x / ceiling)


# --------------------------------------------------------------------------- loudness (BS.1770 estimate)
_B1 = np.array([1.53512485958697, -2.69169618940638, 1.19839281085285])
_A1 = np.array([1.0, -1.69065929318241, 0.73248077421585])
_B2 = np.array([1.0, -2.0, 1.0])
_A2 = np.array([1.0, -1.99004745483398, 0.99007225036621])


def _kweight_mag(f, sr):
    w = TAU * f / sr  # coefficients are for 48 kHz; fine for a first estimate
    z1 = np.exp(-1j * w)
    z2 = np.exp(-2j * w)

    def h(b, a):
        return (b[0] + b[1] * z1 + b[2] * z2) / (a[0] + a[1] * z1 + a[2] * z2)

    return np.abs(h(_B1, _A1) * h(_B2, _A2))


def lufs_estimate(x, sr):
    """Integrated loudness estimate (mono, gated). None if shorter than one block."""
    n = len(x)
    blk = int(0.4 * sr)
    if n < blk:
        return None
    nfft = 1 << int(np.ceil(np.log2(n + 4096)))
    X = np.fft.rfft(x, nfft)
    f = np.fft.rfftfreq(nfft, 1.0 / sr)
    y = np.fft.irfft(X * _kweight_mag(f, sr), nfft)[:n]
    hop = int(0.1 * sr)
    z = []
    for s in range(0, n - blk + 1, hop):
        z.append(np.mean(y[s:s + blk] ** 2))
    z = np.array(z)
    l = -0.691 + 10 * np.log10(z + 1e-20)
    keep = z[l > -70]
    if len(keep) == 0:
        return -70.0
    rel = -0.691 + 10 * np.log10(keep.mean()) - 10
    keep = z[(l > -70) & (l > rel)]
    if len(keep) == 0:
        return -70.0
    return -0.691 + 10 * np.log10(keep.mean())


# =========================================================================== round-1 revision additions
# Added in the first A/B revision round.  Nothing above this line changed, so older recipes
# (and sounds that must stay byte-identical) render exactly as before.

def ad_spectrum(x, sr, size=2048, step=512):
    """Energy-weighted mean power spectrum, the same estimate the A/B review uses for band energy.

    Returns (freqs, mean_spec)."""
    n = len(x)
    xp = np.pad(x, (0, size - n)) if n < size else x
    win = np.hanning(size)
    starts = np.arange(0, max(1, len(xp) - size + 1), step)
    idx = starts[:, None] + np.arange(size)[None, :]
    spec = np.abs(np.fft.rfft(xp[idx] * win, axis=1)) ** 2
    freqs = np.fft.rfftfreq(size, 1.0 / sr)
    w = spec.sum(axis=1)
    mean = (spec * (w / max(w.sum(), 1e-20))[:, None]).sum(axis=0)
    return freqs, mean


# Seven analysis groups.  The review's "sub" band starts at 20 Hz; what a 2048-point Hann window sees
# below that (infrasonic push of a gun, leakage) is its own group so the named bands can match.
BAND_EDGES = [0.0, 20.0, 60.0, 250.0, 1000.0, 4000.0, 8000.0, 1e9]
BAND_NAMES = ["infra", "sub", "bass", "low_mid", "mid", "presence", "air"]


def band_fractions(x, sr):
    f, m = ad_spectrum(x, sr)
    tot = max(float(m.sum()), 1e-20)
    return np.array([float(m[(f >= lo) & (f < hi)].sum()) / tot for lo, hi in zip(BAND_EDGES[:-1], BAND_EDGES[1:])])


def eq_curve(x, sr, gains_db):
    """Smooth static EQ: one gain (dB) per analysis group, interpolated over log frequency."""
    n = len(x)
    nfft = 1 << int(np.ceil(np.log2(n + 8192)))
    X = np.fft.rfft(x, nfft)
    f = np.fft.rfftfreq(nfft, 1.0 / sr)
    cen = np.array([8.0, 38.0, 120.0, 500.0, 2000.0, 5600.0, 12000.0])
    g = np.interp(np.log(np.maximum(f, 1.0)), np.log(cen), np.asarray(gains_db, dtype=float))
    return np.fft.irfft(X * 10 ** (g / 20.0), nfft)[:n]


def infra_shape(x, sr, rng):
    """Unit-RMS slow (< 5 Hz) noise following x's own loudness contour."""
    n = len(x)
    base = fftfilt(noise(n, rng), sr, lp=4.0, order=2)
    ed, hop = env_frames(x, sr)
    env = np.interp(np.arange(n) / sr, (np.arange(len(ed)) + 0.5) * hop / sr, 10 ** (ed / 20.0))
    k = max(3, int(sr * 0.05))
    ker = np.hanning(2 * k + 1)
    env = np.convolve(np.pad(env, (k, k), mode="edge"), ker / ker.sum(), mode="valid")
    lay = base * env
    return lay / np.sqrt(np.mean(lay ** 2) + 1e-20)


def infra_fit(y, lay, sr, frac):
    """Add ``lay`` to y with a gain found by bisection so that the sub-20 Hz share is ``frac``."""
    r = np.sqrt(np.mean(y ** 2) + 1e-20)
    lo, hi = 0.0, 3.0
    for _ in range(12):
        g = 0.5 * (lo + hi)
        if band_fractions(y + lay * g * r, sr)[0] < frac:
            lo = g
        else:
            hi = g
    return y + lay * 0.5 * (lo + hi) * r


def conform_bands(x, sr, target_pct, iters=6, limit=18.0, strength=1.0, rng=None):
    """Nudge the static band balance of x toward ``target_pct`` (dict of the review's band names).

    Whatever the six named bands leave out of 100 % is the target for the sub-20 Hz group; when
    that is large (> 4 %) a slow infrasonic layer is added to reach it.  ``strength`` < 1 only
    moves part of the way (in dB)."""
    named = [float(target_pct.get(k, 0.0)) for k in BAND_NAMES[1:]]
    resid = max(100.0 - sum(named), 0.0)
    infra_t = resid if resid > 4.0 else 0.0
    tgt = np.array([infra_t] + named)
    tgt = tgt / tgt.sum()
    tgt_f = np.maximum(tgt, 0.002)
    lay = infra_shape(x, sr, rng if rng is not None else np.random.default_rng(7)) if infra_t > 0 else None
    total = np.zeros(7)
    y = x
    for _ in range(iters):
        cur = np.maximum(band_fractions(y, sr), 1e-5)
        step = 10 * np.log10(tgt_f / cur) * 0.9 * strength
        step[0] = 0.0
        if np.max(np.abs(step[1:])) < 0.6:
            break
        total = np.clip(total + step, -limit, limit)
        y = eq_curve(x, sr, total)
        if lay is not None:
            y = infra_fit(y, lay, sr, min(tgt[0], 0.5))
    r0 = np.sqrt(np.mean(x ** 2) + 1e-20)
    r1 = np.sqrt(np.mean(y ** 2) + 1e-20)
    return y * (r0 / r1)


def env_frames(x, sr, hop_s=0.010):
    hop = max(1, int(sr * hop_s))
    nf = max(1, len(x) // hop)
    e = np.sqrt(np.mean(x[:nf * hop].reshape(nf, hop) ** 2, axis=1) + 1e-20)
    return 20 * np.log10(e + 1e-10), hop


def conform_env(x, sr, pts, smooth_s=0.05, lo=-30.0, hi=12.0, floor_db=-72.0, gain_smooth_s=0.015):
    """Reshape the slow loudness contour of x to a target.

    ``pts`` = (times, rel_db): wanted level in dB relative to the loudest 10 ms frame, at those times.
    At each control time the local mean level (+-smooth_s) is compared with the wanted level and a
    gain (dB, limited to lo..hi) is applied, interpolated linearly in dB between control times.
    Transients and fine texture between control times are untouched."""
    ts, rel = pts
    n = len(x)
    ed, hop = env_frames(x, sr)
    nf = len(ed)
    pw = 10 ** (ed / 10.0)
    tt = (np.arange(nf) + 0.5) * hop / sr
    ct = np.unique(np.asarray(ts, dtype=float))
    ct = ct[(ct >= 0) & (ct <= n / sr)]
    w = max(1, int(round(smooth_s / 0.010)))

    def local(t):
        i = int(np.clip(t / (hop / sr), 0, nf - 1))
        return 10 * np.log10(np.mean(pw[max(0, i - w):i + w + 1]) + 1e-20)

    # reference: our local level where the original is loudest (searched +-0.15 s for timing slop)
    pk_a = float(ts[int(np.argmax(rel))])
    nb = int(0.15 / (hop / sr))
    i0 = int(np.clip(pk_a / (hop / sr), 0, nf - 1))
    ref = max(local((j + 0.5) * hop / sr) for j in range(max(0, i0 - nb), min(nf, i0 + nb + 1)))
    gains = []
    for t in ct:
        loc = local(t)
        want = max(float(np.interp(t, ts, rel)), floor_db)
        if want <= floor_db + 0.5:
            gains.append(-40.0)
        elif ref + want - loc > 28.0:
            gains.append(0.0)  # nothing to work with here: boosting would only amplify junk
        else:
            gains.append(float(np.clip(ref + want - loc, lo, hi)))
    gains = np.array(gains)
    # smooth the gain curve at 1 kHz (cheap), then interpolate up to the sample rate
    tk = np.arange(0.0, n / sr + 0.002, 0.001)
    gk1 = np.interp(tk, ct, gains)
    ks = max(2, int(gain_smooth_s * 1000))
    ker = np.hanning(2 * ks + 1)
    gk1 = np.convolve(np.pad(gk1, (ks, ks), mode="edge"), ker / ker.sum(), mode="valid")
    g = np.interp(np.arange(n) / sr, tk, gk1)
    return x * 10 ** (g / 20.0)


def decay_env(n, sr, t20, t40=None, attack=0.0005, t0=0.0):
    """Two-slope exponential decay: -20 dB at t20 and -40 dB at t40 (both measured from t0)."""
    t = np.maximum(tvec(n, sr) - t0, 0.0)
    t40 = t40 or t20 * 2.1
    t40 = max(t40, t20 * 1.2)
    s1 = -20.0 / max(t20, 1e-3)
    s2 = -20.0 / max(t40 - t20, 1e-3)
    db = np.where(t < t20, s1 * t, -20.0 + s2 * (t - t20))
    e = 10 ** (db / 20.0)
    if attack > 0:
        e = e * np.minimum(tvec(n, sr) / attack, 1.0)
    return e


def sharp_attack(n, sr, attack=0.0004):
    return np.minimum(tvec(n, sr) / attack, 1.0)


def ping(sr, rng, freqs, tau, n=None, amp=1.0):
    """Short inharmonic metallic ring at the given partial frequencies (Hz)."""
    n = n or n_of(sr, tau * 6)
    t = tvec(n, sr)
    out = np.zeros(n)
    for i, f in enumerate(freqs):
        if f < sr * 0.45:
            out += np.sin(TAU * f * (1 + 0.002 * rng.standard_normal()) * t + rng.uniform(0, TAU)) * np.exp(-t / (tau / (1 + 0.35 * i))) / (1 + 0.3 * i)
    return out * amp


def grain_cloud(n, sr, rng, density, grain_tau=0.004, lo=1000, hi=8000, env=None, amp_var=0.8):
    """Poisson grain texture; density in grains/s may be an array (length n) or a scalar."""
    out = np.zeros(n)
    dens = np.broadcast_to(np.asarray(density, dtype=float), (n,))
    glen = n_of(sr, grain_tau * 6)
    i = 0
    while i < n:
        d = max(dens[min(i, n - 1)], 0.5)
        i += int(rng.exponential(sr / d)) + 1
        if i >= n:
            break
        g = noise(glen, rng) * exp_env(glen, sr, grain_tau * rng.uniform(0.6, 1.4))
        g = fftfilt(g, sr, hp=lo, lp=min(hi, sr * 0.45), order=1)
        seg = min(glen, n - i)
        out[i:i + seg] += g[:seg] * (1 - amp_var * rng.random())
    if env is not None:
        out = out * env
    return out


def waveshape(x, drive=2.0):
    return np.tanh(x * drive) / np.tanh(drive)


def lp_sweep_noise(n, sr, rng, f0, f1, hp=None, steps=8, order=2):
    """White noise through a low-pass whose corner moves f0 -> f1 (like dark_noise but more steps)."""
    return dark_noise(n, sr, rng, f0, f1, steps=steps, hp=hp)


def rand_walk_mod(n, sr, rng, rate, depth):
    """Random (not periodic) amplitude modulation: no tremolo/beating artefacts."""
    return rand_mod(n, sr, rng, rate, depth)


def crest_db(x):
    pk = float(np.max(np.abs(x)))
    rms = float(np.sqrt(np.mean(x ** 2)))
    return 20 * np.log10(max(pk, 1e-12) / max(rms, 1e-12))


def limit_crest(x, target_db, tol=0.4, ratio=8.0):
    """Compress only the tallest peaks (threshold + 1:ratio above it) until peak/RMS is ~``target_db``.

    Keeps the finishing limiter from having to flatten a tall transient against a quiet body, which would
    flatten the contour.  Unlike tanh saturation it leaves no wide flat tops.  Returns x unchanged when the
    crest is already low enough."""
    pk = float(np.max(np.abs(x))) + 1e-12
    if crest_db(x) <= target_db + tol:
        return x
    a = np.abs(x)
    lo, hi = 0.02, 1.0  # threshold as a fraction of the peak
    best = x
    for _ in range(18):
        t = 0.5 * (lo + hi)
        thr = t * pk
        y = np.where(a > thr, np.sign(x) * (thr + (a - thr) / ratio), x)
        c = crest_db(y)
        best = y
        if abs(c - target_db) < tol:
            break
        if c > target_db:
            hi = t
        else:
            lo = t
    return best


def knee_clip(x, ceiling, knee=0.72):
    """Transparent peak limiter: linear up to knee*ceiling, then tanh-compressed toward ceiling.

    (``soft_clip`` bends the signal from about a third of the ceiling, which squashed transients against
    their body whenever a sound needed a high gain to reach its loudness target.)"""
    t = ceiling * knee
    a = np.abs(x)
    over = a > t
    y = x.copy()
    y[over] = np.sign(x[over]) * (t + (ceiling - t) * np.tanh((a[over] - t) / (ceiling - t)))
    return y


def tp_limit(x, ceil_lin, sr, os=4, look=0.003):
    """Look-ahead true-peak limiter (oversampled peak detection, smoothed minimum gain).

    Gain only dips around the offending peaks (a few ms), so the contour and texture stay intact."""
    n = len(x)
    X = np.fft.rfft(x)
    Y = np.zeros(n * os // 2 + 1, dtype=complex)
    m = min(len(X), len(Y))
    Y[:m] = X[:m]
    y = np.fft.irfft(Y, n * os) * os
    a = np.abs(y[:n * os]).reshape(n, os).max(axis=1)
    need = np.minimum(1.0, ceil_lin / (a + 1e-12))
    if need.min() >= 1.0:
        return x
    w = max(3, int(look * sr))
    pad = np.pad(need, (w, w), mode="edge")
    sw = np.lib.stride_tricks.sliding_window_view(pad, 2 * w + 1).min(axis=1)
    ker = np.hanning(2 * w + 1)
    g = np.convolve(np.pad(sw, (w, w), mode="edge"), ker / ker.sum(), mode="valid")
    return x * np.minimum(g, 1.0)


def shaped_noise_like(x, sr, rng, n=None):
    """Noise whose long-term spectrum follows x's (smoothed over ~1/3 octave), unit RMS."""
    n = n or len(x)
    nfft = 1 << int(np.ceil(np.log2(max(len(x), n) + 1024)))
    X = np.abs(np.fft.rfft(x, nfft))
    f = np.fft.rfftfreq(nfft, 1.0 / sr)
    lf = np.log(np.maximum(f, 20.0))
    # smooth the magnitude in log-frequency with a Gaussian of ~1/3 octave
    edges = np.linspace(np.log(20.0), np.log(sr / 2), 160)
    idx = np.clip(np.digitize(lf, edges) - 1, 0, len(edges) - 2)
    band = np.zeros(len(edges) - 1)
    cnt = np.bincount(idx, minlength=len(edges) - 1)
    band = np.bincount(idx, weights=X ** 2, minlength=len(edges) - 1) / np.maximum(cnt, 1)
    k = np.exp(-0.5 * (np.arange(-12, 13) / 4.0) ** 2)
    band = np.convolve(np.pad(band, (12, 12), mode="edge"), k / k.sum(), mode="valid")
    H = np.sqrt(band[idx] + 1e-20)
    W = np.fft.rfft(rng.standard_normal(nfft))
    y = np.fft.irfft(W * H, nfft)[:n]
    return y / (np.sqrt(np.mean(y ** 2)) + 1e-12)


def fill_dead(x, sr, pts, rng, margin_db=22.0, bed_db=-8.0, floor_rel=-68.0):
    """Where the original is still audible (target above ``floor_rel`` dB under its peak) but x has died away
    (more than ``margin_db`` below the target), add a quiet noise bed with x's own long-term spectrum at the
    target level (minus ``bed_db`` headroom).  Recipes that end their layers early therefore still get a natural
    tail instead of digital silence."""
    ts, rel = pts
    ed, hop = env_frames(x, sr)
    top = float(ed.max())
    tt = (np.arange(len(ed)) + 0.5) * hop / sr
    want = np.interp(tt, ts, rel)
    need = (want > floor_rel) & (ed - top < want - margin_db)
    if not need.any():
        return x
    # smooth the on/off mask and the level so the bed fades in and out
    lvl = np.where(need, top + want + bed_db, -200.0)
    amp = 10 ** (lvl / 20.0)
    amp = np.interp(np.arange(len(x)) / sr, tt, amp)
    k = max(3, int(sr * 0.03))
    amp = np.convolve(np.pad(amp, (k, k), mode="edge"), np.hanning(2 * k + 1) / np.hanning(2 * k + 1).sum(), mode="valid")
    bed = shaped_noise_like(x, sr, rng) * amp
    return x + bed
