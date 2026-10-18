"""Gun sounds: shots, reloads, handling, melee/charge items and sci-fi weapons.

Every recipe is original synthesis.  A recipe only reads the numeric targets of the sound it replaces
(length, decay points, onset times, band balance, loudness contour; see sndlib / sound-targets.json).
Recipes are registered by lower-case name prefix; ``synth_family`` in gen_sounds.py then conforms the
loudness contour and band balance and finishes the file.

Round 1 design rules (from the first A/B review):
* nothing tonal unless the original is tonal: transients are noise bursts, bodies are shaped noise;
* modulation is irregular (random), never a fixed-rate tremolo;
* every layer decays naturally; the contour and the silent end come from conform, not from gates;
* sharp transients (sub-millisecond attack) where the original has them.
"""
from __future__ import annotations

import numpy as np

import sfx
from sndlib import Mix, get_tg, recipe, REGISTRY, crack_burst, sub_drop, mech_click, mech_clack
from sndengines import blast, events, rumble, churn, chug_train, foley_event, rel_env_at


def _reg(prefix, fn):
    REGISTRY.append((prefix, fn))


# =========================================================================== acoustic gunshots
ACOUSTIC = {
    "ak47_shot_": dict(),
    "scar_shot_": dict(),
    "ar1_shot_": dict(),
    "aug_shot_": dict(debris=0.7),
    "as50_shot_": dict(sub=(110.0, 38.0), sub_gain=0.9, thunk=0.5, debris=0.9, tail_gain=0.9),
    "boltaction_shot_": dict(tail_gain=0.9, debris=0.8),
    "combatshotgun_shot_": dict(sub=(100.0, 40.0), sub_gain=0.8, thunk=0.5),
    "goldenrevolver_shot_": dict(sub_gain=0.7, thunk=0.3, debris=0.7),
    "grenadelauncher_shot_": dict(sub=(90.0, 42.0), sub_gain=0.8, thunk=0.6),
    "handgun_shot_": dict(),
    "lmg_shot_": dict(debris=0.6),
    "mac10_shot_": dict(brightness=1.1),
    "pdw_shot_": dict(),
    "pistol_shot_": dict(),
    "revolver_shot_": dict(sub_gain=0.6),
    "rocket_shot_": dict(whoosh=1.4, sub=(70.0, 45.0), sub_gain=0.9),
    "sawedoff_shot_": dict(sub=(100.0, 38.0), sub_gain=0.9, thunk=0.5),
    "thompson_shot_": dict(),
    "vector_shot_": dict(brightness=1.1),
    # suppressed: soft hissy 'pfft' crack, strong low thump, faint dark tail
    "silencedm4_shot_": dict(crack_gain=0.8, brightness=0.8, sub=(140.0, 50.0), sub_gain=1.0, rumble=0.4, debris=0.3),
}

# sci-fi weapons are noisy blasts with a buried zap, not clean sweeps (round-1 reviews)
SCIFI = {
    "mib_blaster_shot_": dict(sub_gain=0.8, rumble=0.4, churn_rate=22, churn_depth=0.4,
                              zap=dict(f0=5000, f1=600, dur=0.5, gain=0.25, t=0.0)),
    "alienblaster_shot_": dict(sub_gain=0.9, rumble=0.6, churn_rate=20, churn_depth=0.5, tail_gain=0.9,
                               zap=dict(f0=3000, f1=300, dur=0.45, gain=0.22)),
    "biogun_shot_": dict(sub_gain=0.7, rumble=1.0, churn_rate=26, churn_depth=0.65, tail_gain=0.8,
                         crackle=(90, 900, 4500, 0.9, 0.5), zap=dict(f0=2200, f1=300, dur=0.3, gain=0.15)),
    "blasterrifle_shot_": dict(crack_gain=1.2, sub_gain=0.7, rumble=0.3, churn_rate=25, churn_depth=0.4,
                               zap=dict(f0=8000, f1=400, dur=0.3, gain=0.28)),
    "cdb_shot_": dict(sub_gain=0.8, rumble=0.9, churn_rate=12, churn_depth=0.55, tail_gain=0.9,
                      zap=dict(f0=3000, f1=500, dur=0.4, gain=0.12)),
    "lasergun_shot_": dict(sub_gain=0.9, rumble=0.7, churn_rate=24, churn_depth=0.5,
                           crackle=(60, 1000, 6000, 0.5, 0.35), zap=dict(f0=6000, f1=500, dur=0.3, gain=0.25)),
    "pulserifle_shot_": dict(sub_gain=0.9, rumble=0.8, churn_rate=20, churn_depth=0.5, tail_gain=0.9,
                             crackle=(120, 1500, 9000, 0.5, 0.35), zap=dict(f0=8000, f1=300, dur=0.4, gain=0.18)),
    "sonicshotgun_shot_": dict(sub_gain=0.8, rumble=0.8, churn_rate=28, churn_depth=0.6, tail_gain=0.8,
                               crackle=(80, 1000, 5000, 0.5, 0.3)),
    "teslagun_shot_": dict(sub_gain=0.9, rumble=0.7, churn_rate=30, churn_depth=0.6, tail_gain=0.8,
                           crackle=(170, 2500, 11000, 2.4, 1.1), zap=dict(f0=5000, f1=400, dur=0.4, gain=0.3)),
    "powerhammer_shot_": dict(sub=(100.0, 40.0), sub_gain=1.0, rumble=1.0, churn_rate=14, churn_depth=0.6,
                              thunk=0.5, tail_gain=0.9, crackle=(50, 800, 8000, 0.8, 0.5)),
}


def _blast_recipe(**kw):
    def f(c, N):
        return blast(c, N, **kw)
    return f


for _p, _kw in {**ACOUSTIC, **SCIFI}.items():
    _reg(_p + "*", _blast_recipe(**_kw))


# =========================================================================== minigun, flamethrower
@recipe("minigun_shot_*")
def minigun_shot(c, N):
    """Dense burst of overlapping shots (about 0.3 s), a cliff, then a dark reverberant tail."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    burst = 0.3
    k = int(rng.integers(7, 10))
    t = 0.0
    for i in range(k):
        g = rng.uniform(0.75, 1.0)
        m.add(crack_burst(sr, rng, tau=rng.uniform(0.004, 0.008), lo=700, hi=12000, dur=0.08), t, 1.4 * g)
        m.add(sub_drop(sr, 130, 55, 0.14, 0.05, k=0.03), t, 0.55 * g)
        nb = sfx.n_of(sr, 0.09)
        m.add(sfx.dark_noise(nb, sr, rng, 6000, 700, steps=5, hp=200) * sfx.exp_env(nb, sr, 0.035), t, 0.9 * g)
        t += burst / k * rng.uniform(0.8, 1.2)
    # cliff, then reverberant low-mid tail
    nt = N
    tail = sfx.dark_noise(nt, sr, rng, 1800, 200, steps=7, hp=60) * sfx.decay_env(nt, sr, 0.25, 0.55, attack=0.01) * churn(nt, sr, rng, 14, 0.4)
    m.add(tail, burst * 0.9, 0.45)
    # persistent noise above 5 kHz through the burst
    nh = sfx.n_of(sr, burst + 0.05)
    m.add(sfx.band_noise(nh, sr, rng, 5000, 11000, order=1) * sfx.smooth_env(nh, sr, 0.002, 0.04), 0.0, 0.25)
    return m.out


@recipe("minigun_start_*")
def minigun_start(c, N):
    """Spin-up: gritty mechanical whirr over a rotor rumble, pulsing at the barrel rate, cut while loud."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    base = rumble(c, N, lo=40, hi=900, rate=16, depth=0.5, grit=0.4, hiss=0.5, hiss_lo=1200, hiss_hi=10000, chug=0.9, chug_rate=11)
    puls = 0.45 + 0.55 * churn(N, sr, rng, 11, 1.0)
    return base * puls


@recipe("flamethrower_shot_*", "flamethrower_start_*")
def flamethrower(c, N):
    """Continuous gas roar: present from the first sample, fast irregular flutter, crackle scatter."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    n = N
    sw = 0.06 + 0.1 * rng.random()
    env = np.minimum(sfx.tvec(n, sr) / sw, 1.0) ** 1.2
    roar = sfx.dark_noise(n, sr, rng, 6500, 3200, steps=4, hp=220) * churn(n, sr, rng, 48, 0.55)
    m.add(roar * env, 0.0, 1.0)
    body = sfx.band_noise(n, sr, rng, 250, 900, order=2) * churn(n, sr, rng, 30, 0.6)
    m.add(body * env, 0.0, 0.8)
    low = sfx.band_noise(n, sr, rng, 50, 250, order=2) * churn(n, sr, rng, 14, 0.6)
    m.add(low * env, 0.0, 0.35 + (0.6 if "start" in c.name else 0.0))
    m.add(sfx.grain_cloud(n, sr, rng, 70, 0.003, 1200, 5000, amp_var=0.9) * env, 0.0, 0.5)
    if "start" in c.name:
        m.add(sub_drop(sr, 90, 40, 0.5, 0.18, k=0.08, attack=0.03), 0.0, 0.9)
    return m.out


# =========================================================================== beam gun
@recipe("beamgun_loop_*", "beamgun_start_*")
def beamgun(c, N):
    """Sub-heavy churning rumble that swells in and fades; irregular flutter, falling highs."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    start = "start" in c.name
    m = Mix(c, N)
    m.add(rumble(c, N, lo=28, hi=300, rate=28 if start else 22, depth=0.85, grit=0.3,
                 glide=(450, 160) if start else (200, 330), hiss=0.06 if not start else 0.12, hiss_lo=2500, hiss_hi=8000), 0.0, 1.0)
    m.add(sub_drop(sr, 70, 46, 0.5, 0.25, k=0.15, attack=0.04), 0.0, 0.7)
    if start:
        # bright start burst (2-6 kHz, ~90 ms) and a small second burst
        nb = sfx.n_of(sr, 0.12)
        m.add(sfx.band_noise(nb, sr, rng, 2000, 6500, order=1) * sfx.exp_env(nb, sr, 0.03, attack=0.001), 0.0, 0.7)
        nb2 = sfx.n_of(sr, 0.06)
        m.add(sfx.band_noise(nb2, sr, rng, 1500, 5000, order=1) * sfx.exp_env(nb2, sr, 0.02, attack=0.001), 0.55, 0.3)
    return m.out


# =========================================================================== chainsaw
def _chain_comb(c, N, fa, fb, g):
    sr, rng = c.sr, c.rng
    f = np.linspace(fa, fb, N) * (1 + 0.03 * (churn(N, sr, rng, 4, 1.0) - 0.5))
    st = sfx.harmonic_stack(f, N, sr, nh=36, rolloff=0.9, rng=rng)
    return sfx.fftfilt(st, sr, hp=70, lp=4500, order=1) * g


@recipe("chainsaw_loopstart")
def chainsaw_loopstart(c, N):
    m = Mix(c, N)
    m.add(rumble(c, N, lo=40, hi=2200, rate=30, depth=0.8, grit=0.4, chug=0.7, chug_rate=14, hiss=0.1), 0.0, 1.0)
    m.add(_chain_comb(c, N, 45, 70, 1.0), 0.0, 0.18)
    return m.out


@recipe("chainsaw_loopend")
def chainsaw_loopend(c, N):
    m = Mix(c, N)
    m.add(rumble(c, N, lo=35, hi=1800, rate=18, depth=0.8, grit=0.3, chug=0.7, chug_rate=22, hiss=0.05), 0.0, 1.0)
    m.add(_chain_comb(c, N, 72, 30, 1.0), 0.0, 0.2)
    return m.out


@recipe("chainsaw_loop_*", "chainsaw_swing_*", "chainsaw_hit_*")
def chainsaw(c, N):
    """Irregular two-stroke chug with a quiet harmonic stack, chain rasp and rattle."""
    sr, rng = c.sr, c.rng
    nm = c.name
    f0 = float(rng.uniform(62, 72))
    m = Mix(c, N)
    hit = "hit" in nm
    m.add(rumble(c, N, lo=26, hi=2200 if not hit else 1500, rate=34 if hit else 26, depth=0.85, grit=0.5,
                 chug=0.8, chug_rate=float(rng.uniform(34, 50)), hiss=0.12, hiss_lo=2500, hiss_hi=8000, crackle=0.2), 0.0, 1.0)
    m.add(_chain_comb(c, N, f0, f0 * (1.15 if "swing" in nm else 1.0), 1.0), 0.0, 0.16)
    # chain rattle (mid grit) with random flutter
    m.add(sfx.band_noise(N, sr, rng, 800, 3200, order=1) * churn(N, sr, rng, 55, 0.8), 0.0, 0.25)
    if hit:
        m.add(mech_clack(sr, rng, tau=0.04, thump=0.5), 0.05, 0.5)
    return m.out


# =========================================================================== reloads / handling
BRIGHT_LOW = ("rocket", "flamethrower", "handgun", "alienblaster", "minigun", "grenadelauncher")


def _reload(whine=None, heavy=False, mix=None, **kw):
    def f(c, N):
        sr, rng = c.sr, c.rng
        bright = 0.7 if c.name.startswith(BRIGHT_LOW) else 1.15
        mx = mix or ((("click", 0.4), ("clack", 0.3), ("thunk", 0.2), ("scrape", 0.1)) if heavy else
                     (("click", 0.5), ("clack", 0.3), ("thunk", 0.08), ("scrape", 0.12)))
        out = events(c, N, mix=mx, bright=bright, **kw)
        if whine:
            t0, t1, f0, f1, g = whine
            n = sfx.n_of(sr, t1 - t0)
            fr = np.linspace(f0, f1, n)
            # granular, warbling whirr: band-limited noise around a gliding center, random AM (not a sine)
            base = sfx.noise(n, rng)
            w = sfx.fftfilt(base, sr, bp=(f0 * 0.7, f1 * 1.4) if f1 > f0 else (f1 * 0.7, f0 * 1.4), order=2)
            w = w * churn(n, sr, rng, 60, 0.8) * sfx.smooth_env(n, sr, (t1 - t0) * 0.3, (t1 - t0) * 0.3)
            sfx.place(out, w / (np.std(w) + 1e-9), t0, sr, g)
        return out
    return f


for _p, _kw in {
    "mib_blaster_reload": dict(whine=(1.6, 2.28, 1000, 5000, 0.05), rustle=0.2),
    "alienblaster_reload": dict(whine=(1.5, 2.2, 700, 1100, 0.05), rattle=1.3, rustle=0.2),
    "lasergun_reload": dict(whine=(2.5, 3.0, 8000, 9000, 0.03)),
    "biogun_reload": dict(rustle=0.15),
    "pulserifle_reload": dict(),
    "powerhammer_reload": dict(heavy=True),
    "sonicshotgun_reload": dict(rustle=0.15),
    "teslagun_reload": dict(rattle=1.5, rustle=0.15),
    "as50_reload": dict(heavy=True),
    "lmg_reload": dict(heavy=True),
    "grenadelauncher_reload": dict(heavy=True),
    "rocket_reload": dict(heavy=True, rustle=0.12),
    "flamethrower_reload": dict(heavy=True),
    "minigun_reload": dict(heavy=True, rustle=0.15),
    "combatshotgun_reload": dict(heavy=True),
    "sawedoff_reload": dict(),
}.items():
    _reg(_p + "*", _reload(**_kw))

# every remaining *_reload and the mechanical one-shots
for _p in ("scar_reload", "ak47_reload", "ar1_reload", "aug_reload", "handgun_reload", "mac10_reload", "pdw_reload",
           "pistol_reload", "revolver_reload", "thompson_reload", "vector_reload"):
    _reg(_p + "*", _reload())
for _p in ("boltaction_reload", "boltaction_rechamber", "combatshotgun_pump"):
    _reg(_p, _reload(heavy=True, rustle=0.05))


@recipe("grenade_pin_*")
def grenade_pin(c, N):
    """Two sharp metallic snaps about 80 ms apart (second louder), each with a bright ping and a tiny thump."""
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    t1 = 0.03
    t2 = t1 + 0.08 + rng.uniform(-0.01, 0.01)
    for t, g in ((t1, 0.8), (t2, 1.0)):
        nb = sfx.n_of(sr, 0.12)
        sn = sfx.fftfilt(sfx.noise(nb, rng), sr, hp=2500, lp=14000, order=2) * sfx.exp_env(nb, sr, 0.014, attack=0.0002)
        mid = sfx.fftfilt(sfx.noise(nb, rng), sr, hp=400, lp=5000, order=1) * sfx.exp_env(nb, sr, 0.03, attack=0.0004)
        m.add(sn, t, 1.2 * g)
        m.add(mid, t, 0.5 * g)
        m.add(sfx.ping(sr, rng, [7800 * rng.uniform(0.97, 1.03), 9200, 12600], 0.03), t, 0.12 * g)
        m.add(sub_drop(sr, 200, 100, 0.05, 0.02, k=0.01), t, 0.3 * g)
    for i in range(4):
        m.add(sfx.click(sr, rng, 1.2, 0.002), 0.2 + i * rng.uniform(0.04, 0.07), 0.05)
    return m.out


@recipe("grenade_throw_*")
def grenade_throw(c, N):
    """Handling foley: rustle, pin/spoon clicks, a noisy thump at the throw and a short swish."""
    return events(c, N, mix=(("scrape", 0.4), ("click", 0.35), ("thunk", 0.25)), peak_kind="thunk", bright=0.75,
                  rustle=0.18, rattle=0.8)


# =========================================================================== powerhammer / biogun
@recipe("powerhammer_impactground_*", "powerhammer_impact_*")
def powerhammer_impact(c, N):
    return blast(c, N, sub=(100.0, 40.0), sub_gain=1.0, thunk=0.6, debris=1.0, tail_gain=0.8, rumble=0.7,
                 churn_rate=18, churn_depth=0.5, crackle=(50, 500, 4000, 0.6, 0.5))


@recipe("powerhammer_swing_*")
def powerhammer_swing(c, N):
    """One continuous swelling whoosh with irregular flutter, a sub rumble and mechanical thuds."""
    tg = get_tg(c)
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    m.add(rumble(c, N, lo=28, hi=3500, rate=22, depth=0.7, grit=0.2, hiss=0.3, hiss_lo=1500, hiss_hi=9000,
                 glide=(7000, 600)), 0.0, 1.0)
    for t in [t for t in tg.onsets if t > 0.25][:4]:
        m.add(sfx.thunk(sr, rng, rng.uniform(70, 130), 0.07, 0.2, grit=0.9), t, 0.5)
        m.add(mech_clack(sr, rng, tau=0.03), t, 0.2)
    return m.out


@recipe("powerhammer_charge_*")
def powerhammer_charge(c, N):
    """Swelling dark rumble with gritty mid wash and connected rattles; no ticks or tones."""
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    m.add(rumble(c, N, lo=30, hi=2500, rate=16, depth=0.6, grit=0.3, hiss=0.15, glide=(3500, 500)), 0.0, 1.0)
    m.add(sfx.grain_cloud(N, sr, rng, 50, 0.004, 700, 5000, amp_var=0.9), 0.0, 0.25)
    return m.out


@recipe("biogun_impact_*")
def biogun_impact(c, N):
    return blast(c, N, crack_gain=0.8, sub_gain=0.8, thunk=0.3, rumble=0.8, churn_rate=24, churn_depth=0.7,
                 crackle=(90, 900, 4500, 0.6, 0.5), debris=0.6)
