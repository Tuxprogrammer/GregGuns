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
