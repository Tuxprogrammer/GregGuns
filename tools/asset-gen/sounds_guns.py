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


# =========================================================================== ROUND 2
# Round-1 reviews of the gun family kept coming back to the same few faults.  They are fixed once here, in
# private engines (``blast2``, ``events2``, ``engine_tone`` ...), and the per-sound notes are applied through
# parameters.  The 33 sounds that already earned ``keep`` still run through the round-1 recipes above (KEEP),
# so their bytes cannot change.
#
# Recurring faults and their fixes:
# * hard cut of the tail while the original fades to -70 dB: conform zeroes everything after the original's
#   trailing silence.  ``_setup`` turns that cut off (the tails are shaped by the layers themselves and by
#   conform's contour pinning) so tails fade out naturally;
# * plateaus, cliffs, dips and bumps in the loudness contour: they came from conform stretching a differently
#   shaped recipe onto 11 control points.  The layers are now multiplied by the original's own contour
#   (``_contour``), so conform has almost nothing to correct;
# * soft attack / missing crack: every shot gets a 2-10 kHz crack of 20-40 ms at t=0, then a dense (softly
#   saturated) opening like the originals' flat-topped bursts, and the lead-in silence is gone;
# * sub-heavy thump that holds like a plateau: the pitch-dropping sine gets a short time constant and its gain
#   is fitted so the 20-60 Hz share matches the original's;
# * discrete reflection bumps / grain-cloud crackle in tails: tails are now continuous, low-passed noise whose
#   corner follows the original's centroid per quarter, with at most a few soft puffs;
# * reloads: pure-sine pings (tonal lines at 1-12 kHz) are replaced by noise-excited narrow resonances, noise
#   floors between hits are kept low, one single-transient event per original onset;
# * continuous textures (beam, chainsaw, flame): irregular modulation from t=0, no layer that switches on late.

KEEP = frozenset(n.lower() for n in (
    "MIB_blaster_shot_c ak47_shot_b ak47_shot_c ar1_shot_b boltaction_shot_a boltaction_shot_b boltaction_shot_c "
    "cdb_shot_b cdb_shot_d chainsaw_hit_a chainsaw_hit_b chainsaw_hit_c chainsaw_hit_e chainsaw_loopend "
    "chainsaw_loopstart flamethrower_shot_a flamethrower_shot_c flamethrower_shot_d flamethrower_shot_e "
    "mac10_shot_a pdw_shot_a pdw_shot_b pdw_shot_c powerhammer_charge_a powerhammer_impactGround_b "
    "powerhammer_impactGround_c powerhammer_impact_a powerhammer_impact_d pulserifle_shot_c sawedoff_shot_a "
    "sawedoff_shot_b sawedoff_shot_c").split())

_LEGACY = list(REGISTRY)  # everything registered above this line is the round-1 behavior


def _legacy_for(name):
    name = name.lower()
    for nm, fn in _LEGACY:
        nm = nm.lower()
        if nm.endswith("*"):
            if name.startswith(nm[:-1]):
                return fn
        elif name == nm:
            return fn
    return None


def _guard(new_fn):
    """Round-2 recipe for everything except the sounds already at keep."""
    def f(c, N):
        if c.name in KEEP:
            return _legacy_for(c.name)(c, N)
        return new_fn(c, N)
    return f


def _reg2(prefix, new_fn):
    REGISTRY.insert(0, (prefix, _guard(new_fn)))


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


def _end_t(tg):
    return max(0.05, tg.dur - getattr(tg, "trail_real", tg.trail))


_BAND_IDX = {"sub": 1, "bass": 2, "low_mid": 3, "mid": 4, "presence": 5, "air": 6}


def _band_gain(rest, layer, tg, sr, band, gmax=4.0):
    """Gain for ``layer`` such that ``band`` of rest + g * layer holds the original's share."""
    if not tg.bands:
        return 1.0
    idx = _BAND_IDX[band]
    tgt = float(tg.bands.get(band, 0.0)) / 100.0
    lo, hi = 0.0, gmax
    for _ in range(9):
        g = 0.5 * (lo + hi)
        fr = sfx.band_fractions(rest + g * layer, sr)[idx]
        if fr > tgt:
            hi = g
        else:
            lo = g
    return 0.5 * (lo + hi)


def _zap2(sr, rng, dur, f0, f1, wave="saw", grit=0.0):
    """Clearly audible falling laser line: band-limited saw (or square) gliding exponentially f0 -> f1."""
    n = sfx.n_of(sr, dur)
    t = sfx.tvec(n, sr)
    f = f1 + (f0 - f1) * np.exp(-t / (dur * 0.3))
    st = sfx.harmonic_stack(f, n, sr, nh=10 if wave == "saw" else 7, rolloff=1.0, rng=rng)
    if grit:
        st = sfx.waveshape(st / (np.std(st) + 1e-9), 1.0 + 3 * grit) * np.std(st)
    env = np.minimum(t / 0.003, 1.0) * np.exp(-t / (dur * 0.5))
    return st * env


def _pulses(sr, rng, N, t0, t1, count, lo, hi, tau, lp0=None, lp1=None, amp_var=0.5, jitter=0.35):
    """A few short electric crackles scattered irregularly between t0 and t1 (never a regular train)."""
    out = np.zeros(N)
    ts = np.linspace(t0, t1, count) + rng.uniform(-jitter, jitter, count) * (t1 - t0) / max(count, 1)
    for t in ts:
        nb = sfx.n_of(sr, tau * 7)
        g = sfx.noise(nb, rng) * sfx.exp_env(nb, sr, tau * rng.uniform(0.7, 1.3), attack=0.0006)
        h = hi if lp0 is None else float(np.interp(t, [t0, t1], [lp0, lp1]))
        g = sfx.fftfilt(g, sr, hp=lo, lp=min(h, sr * 0.45), order=1)
        sfx.place(out, g, max(t, 0.0), sr, (1 - amp_var * rng.random()))
    return out


def _dense(x, sr, drive, win=0.08):
    """The originals are loud, flat-topped bursts: the 10 ms frames at the start sit within a few dB of the sample
    peak.  Soft-saturating the opening (scaled by its own RMS) gives the same dense, broadband start instead of a
    spiky crack that the crest limiter later has to flatten (which would shift the band balance)."""
    if not drive:
        return x
    n0 = min(len(x), sfx.n_of(sr, win))
    ref = np.sqrt(np.mean(x[:n0] ** 2)) + 1e-12
    return np.tanh(drive * x / (3.0 * ref)) * (3.0 * ref / drive)


def _contour(tg, N, sr, mono=True, smooth_s=0.02):
    """Amplitude envelope (peak 1) that follows the original's measured loudness contour, interpolated in dB and
    smoothed.  Shaping the layers with it up front means conform has almost nothing left to correct, so the
    plateaus, cliffs and dips that conform's gain interpolation used to add do not appear.  ``mono`` forbids
    any re-swell after the peak (the originals' shot tails decay monotonically)."""
    pts = tg.rel_env_pts()
    t = np.arange(N) / sr
    if pts is None:
        return np.exp(-t / 0.3)
    ts, rel = pts
    rel = np.array(rel, dtype=float)
    if mono:
        k = int(np.argmax(rel))
        rel[k:] = np.minimum.accumulate(rel[k:])
    db = np.interp(t, ts, rel)
    k = max(3, int(smooth_s * sr))
    ker = np.hanning(2 * k + 1)
    db = np.convolve(np.pad(db, (k, k), mode="edge"), ker / ker.sum(), mode="valid")
    return 10 ** (np.maximum(db, -95.0) / 20.0)


def _sweepnoise(N, sr, rng, knots, hp=None, steps=10):
    """White noise through a low-pass whose corner follows ``knots`` = [(t, Hz), ...] (log-interpolated)."""
    t = np.arange(N) / sr
    kt = [k[0] for k in knots]
    kf = np.log([k[1] for k in knots])
    f = np.exp(np.interp(t, kt, kf))
    base = sfx.noise(N, rng)
    if f.max() <= f.min() * 1.05:
        return _u(sfx.fftfilt(base, sr, hp=hp, lp=float(f.min()), order=2))
    grid = np.geomspace(max(f.min(), 80.0), max(f.max(), 90.0), steps)
    bank = [sfx.fftfilt(base, sr, hp=hp, lp=g, order=2) for g in grid]
    pos = np.interp(np.log(f), np.log(grid), np.arange(len(grid)))
    out = np.zeros(N)
    for i, b in enumerate(bank):
        out += b * np.clip(1 - np.abs(pos - i), 0, 1)
    return _u(out)


def _centroid_knots(tg, dur, hi0, k=2.2, floor=220.0, cap=9000.0):
    """Low-pass corner schedule for the main noise body: bright at the hit, then the original's centroid per
    quarter times ``k`` (a flat noise low-passed at f has its centroid near f / 2)."""
    q = tg.quarters if (tg.quarters and len(tg.quarters) >= 4) else None
    e = max(dur, 0.3)
    if q is None:
        return [(0.0, hi0), (e * 0.3, hi0 * 0.35), (e, 500.0)]
    ks = [(0.0, hi0)]
    for i, cq in enumerate(q):
        ks.append((e * (i + 0.5) / 4.0 if i else min(0.08, e * 0.12), float(np.clip(cq * k, floor, cap))))
    out = [ks[0]]  # a mostly falling schedule: the body never re-brightens in the tail
    for tt, ff in ks[1:]:
        out.append((tt, min(ff, out[-1][1] * 1.15)))
    out.append((e * 1.5, out[-1][1] * 0.8))
    return out


def blast2(c, N, *, sub=(125.0, 50.0), sub_tau=0.08, sub_gain=1.0, crack_gain=1.0, crack_hi=11000.0, crack_lo=1800.0,
           crack2_gain=0.55, body_hi=7000.0, body_gain=1.0, low_gain=1.0, tail_gain=1.0, rumble_gain=0.5,
           zap=None, zap2=None, crackle=None, churn_rate=0.0, churn_depth=0.0, thunk=0.0, hits=(), puffs=(),
           whoosh=0.0, pulses=None, buzz=None, first_floor=14.0, fit_sub=True, dense=1.8, mono=True,
           cen_k=2.2, knots=None, rumble_hp=35.0, low_lp=300.0):
    """Round-2 workhorse for shots and impacts.

    One coherent event.  A single noise body whose low-pass corner follows the original's centroid per quarter,
    shaped by the original's own loudness contour (see ``_contour``), a sub-bass rumble under it, a 20-40 ms
    2-10 kHz crack at t=0, a short pitch-dropping thump whose gain is fitted to the original's 20-60 Hz share.
    Optional: ``hits`` (second transients), ``puffs`` (soft dark bumps), ``zap`` (clear laser sweep),
    ``crackle``/``pulses`` (electric tails), ``buzz`` (saw drone)."""
    tg = _setup(c, first_floor)
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    E = _contour(tg, N, sr, mono=mono)
    n = N
    cg = crack_gain
    m.add(_u(crack_burst(sr, rng, tau=0.008, lo=crack_lo, hi=crack_hi, dur=0.12)), 0.0, 2.2 * cg)
    m.add(_u(crack_burst(sr, rng, tau=0.025, lo=600, hi=6500, dur=0.2)), 0.0005, 0.7 * crack2_gain * cg)
    kn = knots or _centroid_knots(tg, _end_t(tg), body_hi, cen_k)
    body = _sweepnoise(n, sr, rng, kn, hp=120) * E
    if churn_depth:
        body = body * churn(n, sr, rng, churn_rate, churn_depth)
    m.add(body, 0.0, 1.0 * body_gain)
    low = _u(sfx.fftfilt(sfx.noise(n, rng), sr, hp=60, lp=low_lp, order=2)) * E
    if churn_depth:
        low = low * churn(n, sr, rng, churn_rate * 0.7, churn_depth)
    m.add(low, 0.0, 0.7 * low_gain * tail_gain)
    rb = _u(sfx.fftfilt(sfx.noise(n, rng), sr, hp=rumble_hp, lp=240, order=2)) * E
    m.add(rb * churn(n, sr, rng, 7, 0.3), 0.0, 0.5 * rumble_gain)
    if whoosh:
        wh = _u(sfx.dark_noise(n, sr, rng, 6000, 700, steps=8, hp=250)) * E
        m.add(wh * np.minimum(np.arange(n) / sr / 0.09, 1.0), 0.0, whoosh)
    if buzz:  # saw drone (f0, gain, decay_s)
        f0, g0, dsc = buzz
        f = np.linspace(f0, f0 * 0.6, n)
        bz = _u(sfx.fftfilt(sfx.harmonic_stack(f, n, sr, nh=14, rolloff=1.0, rng=rng), sr, hp=60, lp=3500, order=1))
        m.add(sfx.waveshape(bz, 2.0) * sfx.exp_env(n, sr, dsc, attack=0.002), 0.0, g0)
    if thunk:
        m.add(_u(sfx.thunk(sr, rng, rng.uniform(55, 80), 0.1, 0.35, grit=0.8)), 0.0, thunk * 0.8)
    for (th, gh) in hits:  # second transient: 2-8 kHz burst + short 150 Hz knock, softer than the main one
        nb = sfx.n_of(sr, 0.09)
        bh = sfx.fftfilt(sfx.noise(nb, rng), sr, hp=1800, lp=8500, order=1) * sfx.exp_env(nb, sr, 0.014, attack=0.0006)
        m.add(_u(bh), th, 1.6 * gh)
        m.add(_u(sub_drop(sr, 190, 95, 0.12, 0.035, k=0.02)), th, 0.9 * gh)
    for (tp, dp, gp) in puffs:  # soft dark bumps (30-80 ms, low-passed), never clicks
        npf = sfx.n_of(sr, dp * 4)
        pf = sfx.fftfilt(sfx.noise(npf, rng), sr, hp=120, lp=1500, order=2) * sfx.exp_env(npf, sr, dp, attack=0.006)
        m.add(_u(pf), tp, gp)
    if zap:
        zd = zap.get("dur", 0.35)
        zs = _zap2(sr, rng, zd, zap.get("f0", 6000), zap.get("f1", 500), zap.get("wave", "saw"), zap.get("grit", 0.3))
        zn = sfx.fftfilt(sfx.noise(len(zs), rng), sr, hp=zap.get("f1", 500), lp=zap.get("f0", 6000), order=1) * sfx.exp_env(len(zs), sr, zd * 0.3)
        m.add(_u(zs) * 0.85 + _u(zn) * 0.25, zap.get("t", 0.0), zap.get("gain", 0.9))
    if zap2:
        zs = _zap2(sr, rng, zap2.get("dur", 0.4), zap2.get("f0", 4000), zap2.get("f1", 500), zap2.get("wave", "saw"), 0.1)
        m.add(_u(zs), zap2.get("t", 0.05), zap2.get("gain", 0.3))
    if crackle:  # (density/s, lo, hi, t_end, gain): smooth, decaying
        dens, lo_, hi_, te, g_ = crackle
        nc = sfx.n_of(sr, min(te, N / sr))
        ck = sfx.grain_cloud(nc, sr, rng, np.linspace(dens, dens * 0.3, nc), 0.0025, lo_, hi_, env=sfx.exp_env(nc, sr, te * 0.45), amp_var=0.8)
        m.add(_u(ck), 0.0, g_)
    if pulses:
        pp = _pulses(sr, rng, N, **pulses["args"])
        m.add(pp / (np.max(np.abs(pp)) + 1e-9), 0.0, pulses.get("gain", 0.5))
    # pitch-dropping thump, gain fitted to the original's 20-60 Hz share
    th_sig = sub_drop(sr, sub[0], sub[1], max(0.3, sub_tau * 6), sub_tau, k=0.03, attack=0.0015)
    th_full = np.zeros(N)
    th_full[:len(th_sig)] = th_sig
    ref = np.std(m.out) * 3
    if fit_sub and tg.bands:
        g = float(np.clip(_band_gain(m.out, th_full * ref, tg, sr, "sub"), 0.25, 4.0))
    else:
        g = 1.0
    m.out += th_full * ref * g * sub_gain
    return _dense(m.out, sr, dense)


# --------------------------------------------------------------------------- reloads and handling (round 2)
# The originals' handling sounds are dense, grainy bursts of broadband noise (strong body 200 Hz - 4 kHz, air up to
# about 11 kHz), clustered around each action, separated by digital silence, with heavy hits carrying a sub thump
# and some reloads a rhythmic 30-100 Hz pulse bed.  There are NO tones in them.  The round-1 version built
# sparse clicks with sine/ring resonances and a high grain cloud, which sounded like tinkling.  This engine builds
# the same events from noise only:
#   click   = 1-4 ms broadband noise impulse (low-passed near 10 kHz) + 150-1200 Hz noise body (15-60 ms)
#   heavy   = click + sub thump (40-120 Hz sine drop, 40-120 ms)
#   rattle  = 5-15 coarse, body-weighted impacts over 30-150 ms with jittered spacing and decaying level
#   cluster = onsets of the original closer than 0.22 s share one grainy texture with a decaying envelope
#   gaps    = digital silence, except where the original has a handling bed (quiet mid-heavy noise)


def _band(x, sr, lo, hi, order=2):
    return sfx.fftfilt(x, sr, hp=lo, lp=min(hi, sr * 0.45), order=order)


def _impulse_click(sr, rng, bright=1.0, lp=10500.0):
    """1-4 ms broadband noise impulse."""
    n = sfx.n_of(sr, 0.02)
    x = sfx.noise(n, rng) * sfx.exp_env(n, sr, rng.uniform(0.0012, 0.003), attack=0.0001)
    return _u(_band(x, sr, 350, lp * bright, 2))


def _body_burst(sr, rng, lo=150.0, hi=1200.0, tau=0.03, dur=None):
    n = sfx.n_of(sr, dur or tau * 7)
    x = sfx.noise(n, rng) * sfx.exp_env(n, sr, tau, attack=0.0005)
    return _u(_band(x, sr, lo, hi, 2))


def _thump(sr, f0=110.0, f1=45.0, tau=0.07):
    return sub_drop(sr, f0, f1, tau * 6, tau, k=0.03, attack=0.002)


def _rattle(sr, rng, count=None, span=None, lo=200.0, hi=2500.0, bright=1.0):
    """5-15 coarse impacts over 30-150 ms, jittered spacing, decaying level, body-weighted."""
    count = count or int(rng.integers(5, 14))
    span = span or rng.uniform(0.04, 0.14)
    out = np.zeros(sfx.n_of(sr, span + 0.08))
    ts = np.sort(rng.uniform(0, span, count))
    for i, t in enumerate(ts):
        g = _body_burst(sr, rng, lo * rng.uniform(0.8, 1.6), hi * bright * rng.uniform(0.6, 1.4), rng.uniform(0.003, 0.009), 0.04)
        ic = _impulse_click(sr, rng, 0.8)
        k = min(len(g), len(ic))
        g[:k] += 0.5 * ic[:k]
        sfx.place(out, g, t, sr, (1 - i / (count + 1.0)) ** 1.3 * rng.uniform(0.5, 1.0))
    return out / (np.max(np.abs(out)) + 1e-9)


def _hit(sr, rng, kind, bright=1.0, bass=0.0):
    """One handling hit, peak-normalized to 1.  kind: click (light), clack, slam (heavy), thunk (dull)."""
    if kind == "click":
        parts = [_impulse_click(sr, rng, bright), 0.7 * _body_burst(sr, rng, 250, 1800, rng.uniform(0.012, 0.025))]
    elif kind == "clack":
        parts = [_impulse_click(sr, rng, bright), 0.9 * _body_burst(sr, rng, 200, 1400, rng.uniform(0.02, 0.04)),
                 0.35 * _rattle(sr, rng, count=int(rng.integers(4, 8)), span=0.05, bright=bright)]
    elif kind == "slam":
        parts = [1.1 * _impulse_click(sr, rng, bright), 1.0 * _body_burst(sr, rng, 150, 1200, rng.uniform(0.04, 0.06)),
                 0.5 * _rattle(sr, rng, count=int(rng.integers(8, 14)), span=0.12, bright=bright),
                 max(bass, 0.25) * _u(_thump(sr, 120, 45, rng.uniform(0.05, 0.1)))]
    elif kind == "thunk":
        parts = [0.6 * _impulse_click(sr, rng, 0.7, 6000), 1.0 * _body_burst(sr, rng, 120, 700, rng.uniform(0.03, 0.05)),
                 0.4 * _u(_thump(sr, 110, 55, 0.05))]
    else:
        raise ValueError(kind)
    out = _sumn(parts)
    return out / (np.max(np.abs(out)) + 1e-9)


def _sumn(parts):
    n = max(len(p) for p in parts)
    out = np.zeros(n)
    for p in parts:
        out[:len(p)] += p
    return out


def _grain_texture(N, sr, rng, dens, tau=(0.002, 0.006), bands=((200, 1400, 0.7), (1000, 4500, 1.0), (3500, 10500, 0.55))):
    """Dense grainy broadband noise: band noise multiplied by a random-impact envelope (Poisson impulses with
    2-6 ms decays), so it crackles like rattling metal instead of hissing.  ``dens`` = impacts/s (scalar or array)."""
    d = np.broadcast_to(np.asarray(dens, dtype=float), (N,))
    imp = np.zeros(N)
    i = 0
    while i < N:
        i += int(rng.exponential(sr / max(d[min(i, N - 1)], 1.0))) + 1
        if i < N:
            imp[i] = rng.uniform(0.3, 1.0)
    k = sfx.n_of(sr, 0.03)
    kern = np.exp(-np.arange(k) / (sr * np.mean(tau)))
    env = np.convolve(imp, kern)[:N]
    out = np.zeros(N)
    for lo, hi, g in bands:
        out += g * _u(_band(sfx.noise(N, rng), sr, lo, hi, 2)) * (0.25 + env / (np.max(env) + 1e-9) * 1.5)
    return _u(out)


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


def _clusters(ons, gap=0.22):
    out = []
    for t in sorted(ons):
        if out and t - out[-1][-1] < gap:
            out[-1].append(t)
        else:
            out.append([t])
    return out


def events3(c, N, *, bright=1.0, bass=0.35, bed=0.0, bed_floor=-46.0, tau=0.075, tex=1.0, tex_dens=420.0,
            lows=(), glides=(), ons=None, drop=(), add=(), kinds_at=None, big_kind="slam", big_gain=1.0,
            heavy_top=0.2, floor_db=-36.0, level_pow=0.7, tail=0.12):
    """Round-2 handling sequence from noise only.

    ``lows``   = ('pulses', t0, t1, rate, gain) or ('rumble', t0, t1, gain), in units of the loudest hit;
    ``glides`` = (t0, t1, f0, f1, gain): wide noise glide for an electronic charge sound (kept below 2.5 kHz);
    ``bed``    = level of the mid-heavy handling bed where the original is not silent between clusters."""
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
        g = (big_gain if t == big else 1.0) * (0.35 + 0.65 * amp)
        m.add(_hit(sr, rng, kind, bright, bass), t, g)
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
    m.add(_grain_texture(N, sr, rng, tex_dens, bands=((200, 1400, 0.7), (1000, 4500, 1.0), (3500, 10500, 0.55 * bright))) * env_tex, 0.0, 0.8 * tex)
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
    return out


def _reload3(**kw):
    def f(c, N):
        k = dict(kw)
        k.update(PER_RELOAD.get(c.name, {}))
        return events3(c, N, **k)
    return f


PER_RELOAD = {}


# --------------------------------------------------------------------------- round 3: modal metal clicks
# Round 2 removed the tones but overcorrected: noise-only clicks and a white grain texture measured flatness
# 0.45-0.63 (A's metal clicks: 0.004-0.01) and read as "static".  Each click is now a 1-3 ms broadband transient
# plus 3-6 damped inharmonic modes (mostly 600-3500 Hz, 15-35 ms decays, modes above 5 kHz under 10 ms) and a
# short 150-1000 Hz body.  The mode set is drawn per click, so no two clicks share a pitch, and the texture between
# hits is a train of such clicks instead of white noise.  These definitions replace the round-2 ones above.
_RATIOS = np.array([1.0, 1.47, 1.93, 2.52, 3.17, 3.9, 4.7])


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
            f, ti = min(b * _RATIOS[i] * rng.uniform(0.93, 1.07), top), rng.uniform(*tau) / (1.0 + 0.15 * i)
        wob = np.cumsum(rng.standard_normal(n)) * (0.4 * 2 * np.pi * f / sr) * 0.5
        out += np.sin(2 * np.pi * f * t + rng.uniform(0, 2 * np.pi) + wob) * np.exp(-t / ti) / (1.0 + 0.35 * i)
    return out


def _impulse_click(sr, rng, bright=1.0, lp=10500.0):
    n = sfx.n_of(sr, 0.02)
    x = sfx.noise(n, rng) * sfx.exp_env(n, sr, rng.uniform(0.001, 0.0025), attack=0.0001)
    return _u(_band(x, sr, 500, lp * bright, 2))


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


def _hit(sr, rng, kind, bright=1.0, bass=0.0):
    """One handling hit, peak-normalized to 1: transient + damped inharmonic modes + short body."""
    tr = _impulse_click(sr, rng, bright)
    if kind == "click":
        parts = [tr, 0.4 * _u(_modal(sr, rng, 3, (800, 1600), (0.003, 0.006), 0.05)),
                 0.5 * _body_burst(sr, rng, 250, 1500, rng.uniform(0.012, 0.025))]
    elif kind == "clack":
        parts = [tr, 0.42 * _u(_modal(sr, rng, 5, (600, 1300), (0.003, 0.006), 0.05)),
                 0.7 * _body_burst(sr, rng, 200, 1400, rng.uniform(0.02, 0.04)),
                 0.35 * _rattle(sr, rng, count=int(rng.integers(5, 10)), span=0.05, bright=bright)]
    elif kind == "slam":
        parts = [1.1 * tr, 0.35 * _u(_modal(sr, rng, 6, (600, 1200), (0.003, 0.007), 0.06)),
                 0.9 * _body_burst(sr, rng, 150, 1200, rng.uniform(0.04, 0.06)),
                 0.5 * _rattle(sr, rng, count=int(rng.integers(8, 16)), span=0.12, bright=bright)]
        if bass > 0:
            parts.append(bass * _u(_thump(sr, 120, 45, rng.uniform(0.05, 0.1))))
    elif kind == "thunk":
        parts = [0.6 * _impulse_click(sr, rng, 0.7, 6000), 0.35 * _u(_modal(sr, rng, 3, (400, 800), (0.003, 0.006), 0.05)),
                 1.0 * _body_burst(sr, rng, 120, 700, rng.uniform(0.03, 0.05))]
        if bass > 0:
            parts.append(0.4 * bass * _u(_thump(sr, 110, 55, 0.05)))
    else:
        raise ValueError(kind)
    out = _sumn(parts)
    out = out / (np.max(np.abs(out)) + 1e-9)
    # the originals' hits are dense (peak only ~10 dB over their own RMS); the soft saturation keeps the crest low so
    # the finishing crest limiter has nothing tall to flatten (it would splatter low-frequency energy)
    return np.tanh(4.0 * out) / np.tanh(4.0)


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


def _reload3(**kw):
    def f(c, N):
        k = dict(kw)
        k.setdefault("tex", 0.5)
        k.setdefault("bright", 1.2)
        k.setdefault("bass", 0.0)  # low end only where A has it (``lows``)
        k.update(PER_RELOAD.get(c.name, {}))
        c.rng.random(int(k.pop('reseed', 0)) + 1)  # alternate draw when a seed leaves a stray line
        return events3(c, N, **k)
    return f

PER_RELOAD.setdefault("mib_blaster_reload_akimbo", {}).update(reseed=1)
PER_RELOAD.setdefault("alienblaster_reload", {}).update(reseed=1)
PER_RELOAD.setdefault("revolver_reload_akimbo", {}).update(reseed=1)
PER_RELOAD.setdefault("mac10_reload", {}).update(reseed=1)


# --------------------------------------------------------------------------- reload registrations (round 2)
# lows: ('pulses', t0, t1, rate, gain) | ('rumble', t0, t1, gain); glides: (t0, t1, f0, f1, gain)
PER_RELOAD.update({
    "mib_blaster_reload": dict(lows=[("rumble", 1.3, 1.9, 0.12)], glides=[(1.9, 2.28, 1000, 2400, 0.6)]),
    "mib_blaster_reload_akimbo": dict(lows=[("rumble", 1.4, 2.5, 0.1)], glides=[(2.6, 2.99, 1000, 2400, 0.6)]),
    "ak47_reload": dict(lows=[("pulses", 1.62, 2.65, 9.0, 0.5)], bed=0.12),
    "alienblaster_reload": dict(lows=[("rumble", 0.0, 1.5, 0.7), ("rumble", 1.5, 2.4, 0.35)],
                                glides=[(1.5, 2.2, 700, 1100, 0.2)], bed=0.2),
    "as50_reload": dict(lows=[("pulses", 0.1, 1.0, 8.0, 0.12), ("pulses", 1.2, 1.9, 7.0, 0.25), ("pulses", 4.2, 4.9, 6.0, 0.25)], bed=0.1),
    "biogun_reload": dict(lows=[("rumble", 1.4, 3.0, 0.15)], bed=0.12),
    "combatshotgun_pump": dict(lows=[("pulses", 0.5, 0.75, 8.0, 0.25)]),
    "grenadelauncher_reload": dict(lows=[("rumble", 3.5, 4.3, 0.15)], bed=0.08),
    "handgun_reload": dict(lows=[("rumble", 0.0, 1.3, 0.5)], bed=0.2),
    "lasergun_reload": dict(glides=[(2.1, 2.9, 1400, 2300, 0.1)]),
    "lmg_reload": dict(lows=[("rumble", 0.0, 0.8, 0.8)], bed=0.06),
    "minigun_reload": dict(lows=[("rumble", 0.2, 1.5, 0.3), ("rumble", 4.6, 5.9, 0.5)], bed=0.08),
    "powerhammer_reload": dict(lows=[("rumble", 2.3, 3.3, 0.6), ("pulses", 1.75, 2.0, 8.0, 0.3)], bed=0.1),
    "revolver_reload": dict(bed=0.1),
    "revolver_reload_akimbo": dict(bed=0.1),
    "rocket_reload": dict(lows=[("rumble", 0.0, 1.5, 0.6), ("rumble", 2.8, 3.5, 0.6)], bed=0.08),
    "sawedoff_reload": dict(bed=0.08),
    "sonicshotgun_reload": dict(lows=[("rumble", 2.2, 3.8, 0.8)], bed=0.06),
    "teslagun_reload": dict(lows=[("rumble", 0.0, 1.0, 0.4), ("rumble", 1.2, 2.3, 1.0)], bed=0.5),
    "pulserifle_reload": dict(bed=0.1),
    "thompson_reload": dict(bed=0.06),
    "grenade_pin_a": dict(bright=1.1, bass=0.0, kinds_at=None),
    "grenade_pin_b": dict(bright=1.1, bass=0.0),
    "grenade_throw_a": dict(bright=0.55, bed=0.6, tex=0.8),
    "grenade_throw_b": dict(bright=0.6, bed=0.6, tex=0.8),
    "grenade_throw_c": dict(bright=0.8, bed=0.6, tex=0.8),
})

for _p in ("mib_blaster_reload", "alienblaster_reload", "lasergun_reload", "biogun_reload", "pulserifle_reload",
           "powerhammer_reload", "sonicshotgun_reload", "teslagun_reload", "as50_reload", "lmg_reload",
           "grenadelauncher_reload", "rocket_reload", "flamethrower_reload", "minigun_reload", "combatshotgun_reload",
           "sawedoff_reload", "scar_reload", "ak47_reload", "ar1_reload", "aug_reload", "handgun_reload",
           "mac10_reload", "pdw_reload", "pistol_reload", "revolver_reload", "thompson_reload", "vector_reload",
           "boltaction_reload"):
    _reg2(_p + "*", _reload3())
for _p in ("boltaction_rechamber", "combatshotgun_pump", "grenade_pin_*", "grenade_throw_*"):
    _reg2(_p, _reload3())


# --------------------------------------------------------------------------- shots: per-prefix and per-sound parameters
ACOUSTIC2 = {
    "ak47_shot_": dict(),
    "scar_shot_": dict(),
    "ar1_shot_": dict(),
    "aug_shot_": dict(),
    "as50_shot_": dict(sub=(110.0, 45.0), sub_tau=0.12, tail_gain=1.0, body_hi=9000.0),
    "combatshotgun_shot_": dict(sub=(100.0, 42.0), sub_tau=0.1),
    "goldenrevolver_shot_": dict(tail_gain=1.1),
    "grenadelauncher_shot_": dict(sub=(90.0, 42.0), sub_tau=0.12),
    "handgun_shot_": dict(),
    "lmg_shot_": dict(),
    "mac10_shot_": dict(crack_hi=12000.0),
    "pistol_shot_": dict(),
    "revolver_shot_": dict(),
    "rocket_shot_": dict(whoosh=0.9, sub=(75.0, 45.0), sub_tau=0.14),
    "thompson_shot_": dict(),
    "vector_shot_": dict(crack_hi=12000.0),
    "silencedm4_shot_": dict(crack_gain=0.8, crack_hi=9000.0, body_hi=4500.0, sub=(140.0, 50.0), sub_tau=0.09, tail_gain=0.8),
}

SCIFI2 = {
    "mib_blaster_shot_": dict(body_hi=5500.0, zap=dict(f0=6000, f1=800, dur=0.3, gain=0.8)),
    "alienblaster_shot_": dict(body_hi=5500.0, churn_rate=9, churn_depth=0.2,
                               zap=dict(f0=9000, f1=450, dur=0.4, gain=0.9), zap2=dict(f0=4500, f1=450, dur=0.4, gain=0.35, t=0.0)),
    "biogun_shot_": dict(body_hi=6000.0, churn_rate=18, churn_depth=0.35, crackle=(90, 900, 4500, 0.9, 0.4),
                         zap=dict(f0=4500, f1=500, dur=0.25, gain=0.5)),
    "blasterrifle_shot_": dict(crack_gain=1.1, body_hi=5000.0, zap=dict(f0=9000, f1=450, dur=0.3, gain=0.9)),
    # the laser line is only the opening zap (A has 3-5 lines, all early); no second long sweep
    "lasergun_shot_": dict(body_hi=4000.0, zap=dict(f0=10000, f1=1500, dur=0.2, gain=0.6, grit=0.6)),
    "pulserifle_shot_": dict(body_hi=6000.0, zap=dict(f0=8000, f1=300, dur=0.4, gain=0.45)),
    "sonicshotgun_shot_": dict(churn_rate=14, churn_depth=0.2, body_hi=6000.0),
    "teslagun_shot_": dict(churn_rate=14, churn_depth=0.2, crackle=(170, 2500, 11000, 1.2, 0.8),
                           zap=dict(f0=5000, f1=400, dur=0.35, gain=0.5)),
    "powerhammer_shot_": dict(sub=(100.0, 40.0), sub_tau=0.14, churn_rate=10, churn_depth=0.25, thunk=0.4,
                              crackle=(50, 800, 8000, 0.6, 0.3)),
}

# per-sound overrides, from the round-1 change notes
PER = {}


def _shot_recipe(base, extra):
    kw = dict(base)
    kw.update(extra)

    def f(c, N):
        return blast2(c, N, **kw)
    return f


def _per_for(prefix_name):
    return PER.get(prefix_name, {})


def _make_shot(c, N, prefix, base):
    kw = dict(base)
    kw.update(PER.get(c.name, {}))
    return blast2(c, N, **kw)


for _p, _kw in {**ACOUSTIC2, **SCIFI2}.items():
    _reg2(_p + "*", (lambda p, b: (lambda c, N: _make_shot(c, N, p, b)))(_p, _kw))


# --------------------------------------------------------------------------- sustained textures (round 2)
def _am_periodic(n, sr, rng, rate, depth, jitter=0.03):
    """Regular pulse (two-stroke / rotor) with a few percent of rate jitter: 1 at the peaks, 1-depth in the dips."""
    j = (sfx.rand_mod(n, sr, rng, 4.0, 1.0) - 0.5) * 2.0 * jitter
    phi = np.cumsum(2 * np.pi * rate * (1.0 + j) / sr)
    return 1.0 - depth * (0.5 - 0.5 * np.cos(phi))


def sustain2(c, N, *, cen_k=2.0, body_hi=2500.0, body_gain=1.0, knots=None, hp=60.0, am=None, low=None, buzz=None,
             hiss=None, crack=None, thump=None, mono=False, smooth_s=0.04, first_floor=None, rumble_am=(24.0, 0.6),
             dense=0.0):
    """Round-2 continuous texture (beam, chainsaw, flame, swing, charge): everything is present from t=0 and is
    shaped by the original's own loudness contour, so nothing switches on late and there is no step or dip that is
    not in the original.

    body   = noise whose low-pass corner follows the original's centroid per quarter (or ``knots``);
    am     = (rate, depth) irregular amplitude flutter on the body (use ``_am_periodic`` via ``buzz`` for pulses);
    low    = (lo, hi, gain): sub/bass noise rumble with irregular flutter ``rumble_am``;
    buzz   = dict(f0, f1, gain, lp0, lp1, rate, depth, nh): sawtooth drone with a darkening low-pass and a
             periodic two-stroke pulse (jittered a few percent);
    hiss   = (lo, hi, gain): constant high band; crack = (gain, hi): short opening burst;
    thump  = (gain, f0, f1, tau)."""
    tg = _setup(c, first_floor)
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    E = _contour(tg, N, sr, mono=mono, smooth_s=smooth_s)
    kn = knots or _centroid_knots(tg, max(tg.dur, 0.5), body_hi, cen_k, floor=120.0)
    body = _sweepnoise(N, sr, rng, kn, hp=hp)
    if am:
        body = body * churn(N, sr, rng, am[0], am[1])
    m.add(body * E, 0.0, body_gain)
    if low:
        lo, hi, g = low
        rb = _u(sfx.fftfilt(sfx.noise(N, rng), sr, hp=lo, lp=hi, order=2)) * churn(N, sr, rng, rumble_am[0], rumble_am[1])
        m.add(rb * E, 0.0, g)
    if buzz:
        b = dict(buzz)
        f = np.linspace(b["f0"], b.get("f1", b["f0"]), N)
        f = f * (1 + 0.015 * (sfx.rand_mod(N, sr, rng, 6.0, 1.0) - 0.5))
        st = sfx.harmonic_stack(f, N, sr, nh=b.get("nh", 20), rolloff=1.0, rng=rng)
        lp0, lp1 = b.get("lp0", 3000.0), b.get("lp1", 800.0)
        steps = 8
        grid = np.geomspace(min(lp0, lp1), max(lp0, lp1), steps)
        bank = [sfx.fftfilt(st, sr, hp=60, lp=g_, order=2) for g_ in grid]
        tt = np.arange(N) / sr
        t_end = max(_end_t(tg), 0.5)
        lpt = np.exp(np.interp(tt, b.get("lp_t", [0.0, t_end]), np.log([lp0, lp1])))
        pos = np.interp(np.log(lpt), np.log(grid), np.arange(steps))
        out = np.zeros(N)
        for i, bk in enumerate(bank):
            out += bk * np.clip(1 - np.abs(pos - i), 0, 1)
        out = _u(out)
        if b.get("rate"):
            out = out * _am_periodic(N, sr, rng, b["rate"], b.get("depth", 0.5))
        m.add(out * E, 0.0, b.get("gain", 1.0))
    if hiss:
        lo, hi, g = hiss
        hs = _u(sfx.band_noise(N, sr, rng, lo, hi, order=1)) * churn(N, sr, rng, 40, 0.5)
        m.add(hs * E, 0.0, g)
    if crack:
        m.add(_u(crack_burst(sr, rng, tau=0.02, lo=1500, hi=crack[1], dur=0.2)), 0.0, 2.0 * crack[0])
    if thump:
        g, f0, f1, tau = thump
        m.add(_u(sub_drop(sr, f0, f1, tau * 6, tau, k=0.03, attack=0.004)), 0.0, g)
    return _dense(m.out, sr, dense) if dense else m.out


def _sustain_recipe(**kw):
    def f(c, N):
        k = dict(kw)
        k.update(PER_SUS.get(c.name, {}))
        return sustain2(c, N, **k)
    return f


PER_SUS = {
    # beam gun: sub-heavy churn, buzzy 100-300 Hz band and a constant faint hiss, from t=0 (no late switch-on)
    "beamgun_loop_a": dict(cen_k=1.3, body_hi=500.0, low=(28, 120, 1.0), buzz=dict(f0=500, f1=480, gain=0.12, lp0=900, lp1=900, nh=6),
                           hiss=(2000, 7000, 0.03)),
    "beamgun_loop_b": dict(cen_k=1.3, body_hi=500.0, low=(28, 120, 0.9), buzz=dict(f0=180, f1=180, gain=0.25, lp0=900, lp1=700, nh=8),
                           hiss=(1500, 8000, 0.03)),
    "beamgun_loop_c": dict(cen_k=1.3, body_hi=700.0, low=(30, 100, 0.9), buzz=dict(f0=170, f1=190, gain=0.35, lp0=800, lp1=900, nh=8),
                           hiss=(1500, 6000, 0.03)),
    "beamgun_loop_d": dict(cen_k=1.3, body_hi=800.0, low=(30, 100, 0.9), buzz=dict(f0=180, f1=200, gain=0.3, lp0=800, lp1=1000, nh=8),
                           hiss=(1000, 5000, 0.04)),
    "beamgun_loop_e": dict(cen_k=1.3, body_hi=600.0, low=(30, 110, 0.8), buzz=dict(f0=220, f1=240, gain=0.4, lp0=900, lp1=900, nh=8),
                           hiss=(1000, 5000, 0.02)),
    "beamgun_loop_f": dict(cen_k=1.3, body_hi=600.0, low=(30, 110, 0.8), buzz=dict(f0=150, f1=170, gain=0.4, lp0=900, lp1=900, nh=8),
                           hiss=(1000, 4000, 0.04)),
    "beamgun_start_a": dict(cen_k=1.4, body_hi=900.0, low=(30, 110, 1.0), buzz=dict(f0=200, f1=220, gain=0.3, lp0=1000, lp1=900, nh=8, rate=30, depth=0.5),
                            hiss=(1500, 6000, 0.1), crack=(0.5, 6000)),
    "beamgun_start_b": dict(cen_k=1.4, body_hi=900.0, low=(30, 110, 0.9), buzz=dict(f0=200, f1=210, gain=0.3, lp0=1000, lp1=900, nh=8),
                            hiss=(1000, 6000, 0.15)),
    "beamgun_start_c": dict(cen_k=1.5, body_hi=1200.0, low=(30, 120, 0.8), buzz=dict(f0=250, f1=240, gain=0.4, lp0=1200, lp1=900, nh=8),
                            hiss=(800, 8000, 0.15), crack=(0.4, 8000)),
    # chainsaw: clearly tonal two-stroke saw tone (periodic pulse, darkening low-pass, falling pitch) over a thin noise bed
    "chainsaw_loop_a": dict(cen_k=1.0, body_hi=2500.0, body_gain=0.25, hp=800.0,
                            buzz=dict(f0=122, f1=118, gain=1.0, lp0=2500, lp1=600, lp_t=[0.5, 1.2], rate=31, depth=0.55, nh=30)),
    "chainsaw_loop_b": dict(cen_k=1.0, body_hi=2500.0, body_gain=0.25, hp=800.0,
                            buzz=dict(f0=130, f1=115, gain=1.0, lp0=3000, lp1=600, lp_t=[0.7, 1.5], rate=32, depth=0.5, nh=35)),
    "chainsaw_loop_c": dict(cen_k=1.0, body_hi=2500.0, body_gain=0.25, hp=800.0,
                            buzz=dict(f0=130, f1=100, gain=1.0, lp0=4000, lp1=800, lp_t=[0.5, 1.4], rate=45, depth=0.7, nh=35)),
    "chainsaw_swing_a": dict(cen_k=1.0, body_hi=2000.0, body_gain=0.2, hp=600.0, smooth_s=0.03,
                             buzz=dict(f0=95, f1=100, gain=1.0, lp0=2000, lp1=1500, rate=50, depth=0.4, nh=22)),
    "chainsaw_swing_b": dict(cen_k=1.0, body_hi=2000.0, body_gain=0.2, hp=600.0, smooth_s=0.03,
                             buzz=dict(f0=80, f1=84, gain=1.0, lp0=2500, lp1=1500, rate=9, depth=0.0, nh=22)),
    "chainsaw_swing_c": dict(cen_k=1.0, body_hi=2000.0, body_gain=0.2, hp=600.0, smooth_s=0.03,
                             buzz=dict(f0=50, f1=52, gain=1.0, lp0=2500, lp1=1800, rate=40, depth=0.7, nh=30)),
    # powerhammer swing / charge: swelling dark rumble with a gritty mid wash, no ticks or tones
    "powerhammer_swing_a": dict(cen_k=2.0, body_hi=6000.0, low=(25, 200, 0.9), hiss=(2000, 8000, 0.08), crack=(0.25, 8000), smooth_s=0.025),
    "powerhammer_swing_b": dict(cen_k=2.0, body_hi=5000.0, low=(25, 200, 0.7), hiss=(2000, 8000, 0.1), crack=(0.3, 6000), smooth_s=0.025, dense=1.4),
    "powerhammer_swing_c": dict(cen_k=2.0, body_hi=4000.0, low=(25, 200, 0.9), hiss=(2000, 6000, 0.05), crack=(0.2, 4000), smooth_s=0.025),
    "powerhammer_charge_b": dict(cen_k=2.0, body_hi=2500.0, low=(25, 200, 1.0), hiss=(2000, 6000, 0.1), am=(20, 0.3)),
    "powerhammer_charge_c": dict(cen_k=2.0, body_hi=2500.0, low=(25, 200, 1.0), hiss=(1000, 6000, 0.12), am=(30, 0.4)),
    # flamethrower: continuous gas roar from the first sample, fast flutter, no steps
    "flamethrower_shot_b": dict(cen_k=1.6, body_hi=900.0, low=(40, 200, 0.5), am=(30, 0.3)),
    "flamethrower_start_a": dict(cen_k=1.8, body_hi=9000.0, low=(40, 200, 0.5), am=(30, 0.35), crack=(0.4, 6000), smooth_s=0.03),
}
for _p in ("beamgun_loop_*", "beamgun_start_*", "chainsaw_loop_a", "chainsaw_loop_b", "chainsaw_loop_c",
           "chainsaw_swing_*", "powerhammer_swing_*", "powerhammer_charge_*", "flamethrower_shot_b", "flamethrower_start_a"):
    _reg2(_p, _sustain_recipe())


# --------------------------------------------------------------------------- minigun and impacts (round 2)
def minigun2(c, N, *, rate=11.0, burst=0.30, jitter=0.04, tail_db=-14.0, tail_hi=2500.0, tail_lo=450.0, wash=0.25,
             shot_lo=60.0, cliff_s=0.012):
    """Burst of equal-level shots at a steady rate (small timing jitter), a fast cliff at ``burst`` s, then one
    smooth dark noise decay that follows the original's contour; a bright 2-10 kHz band stays through the burst."""
    tg = _setup(c, None)
    sr, rng = c.sr, c.rng
    m = Mix(c, N)
    k = max(2, int(round(rate * burst)))
    t = 0.0
    step = burst / (rate * burst)
    gate = np.ones(N)
    ts = np.arange(N) / sr
    gate = np.clip(1.0 - (ts - burst) / cliff_s, 0.0, 1.0)
    for i in range(k + 1):
        tt = i * step + rng.uniform(-jitter, jitter) * step
        tt = max(tt, 0.0)
        if tt >= burst:
            break
        g = rng.uniform(0.85, 1.0)
        m.add(_u(crack_burst(sr, rng, tau=0.006, lo=800, hi=11000, dur=0.06)), tt, 1.6 * g)
        m.add(_u(sub_drop(sr, 180, shot_lo, 0.12, 0.028, k=0.02)), tt, 1.0 * g)
        nb = sfx.n_of(sr, 0.09)
        m.add(_u(sfx.dark_noise(nb, sr, rng, 6000, 700, steps=5, hp=150) * sfx.exp_env(nb, sr, 0.03)), tt, 0.8 * g)
    m.out *= gate
    hiw = _u(sfx.band_noise(N, sr, rng, 2000, 10000, order=1)) * np.clip(1.0 - (ts - burst) / 0.05, 0.0, 1.0)
    m.add(hiw, 0.0, wash)
    # tail: smooth dark noise, starting about 14 dB under the burst, decaying with the original's contour
    pts = tg.rel_env_pts()
    t20 = max(tg.dec20 or 0.4, 0.1)
    t40 = max(tg.dec40 or 0.8, t20 * 1.3)
    ref = np.std(m.out[:sfx.n_of(sr, burst)]) + 1e-9
    kn = [(burst, tail_hi), (burst + 0.3, tail_lo * 2), (burst + 0.9, tail_lo)]
    tail = _sweepnoise(N, sr, rng, kn, hp=70) * sfx.decay_env(N, sr, t20 - burst * 0.5, t40 - burst * 0.5, t0=burst) * (ts >= burst) * 10 ** (tail_db / 20.0) * ref
    tail = tail * np.minimum((ts - burst) / 0.01, 1.0).clip(0, 1)
    m.out += tail
    return m.out


PER_MINIGUN = {
    "minigun_shot_a": dict(rate=11.0, wash=0.3),
    "minigun_shot_b": dict(rate=20.0, wash=0.25, tail_db=-14.0),
    "minigun_shot_c": dict(rate=20.0, wash=0.5, tail_db=-12.0, tail_hi=2000.0),
    "minigun_shot_d": dict(rate=37.0, wash=0.3, jitter=0.12),
}


def _minigun_recipe(c, N):
    return minigun2(c, N, **PER_MINIGUN.get(c.name, {}))


def minigun_start2(c, N):
    """Spin-up burst: rumble present from t=0, regular 11 Hz pulse (10-15 dB dips), held level, cut at the end."""
    tg = _setup(c, None)
    sr, rng = c.sr, c.rng
    base = _u(sfx.fftfilt(sfx.noise(N, rng), sr, hp=40, lp=900, order=2)) + 0.4 * _u(sfx.band_noise(N, sr, rng, 1200, 9000, order=1))
    base = sfx.waveshape(base / 3.0, 2.0)
    puls = _am_periodic(N, sr, rng, 11.0, 0.8, jitter=0.01)
    ts = np.arange(N) / sr
    return base * puls * np.minimum(ts / 0.035, 1.0)


_reg2("minigun_shot_*", _minigun_recipe)
_reg2("minigun_start_*", minigun_start2)

# impacts: the same one-event engine as the shots
PER_IMPACT = {
    "powerhammer_impact_b": dict(), "powerhammer_impact_c": dict(sub_gain=0.8),
    "powerhammer_impactground_a": dict(), "powerhammer_impactground_d": dict(),
}


def _impact_recipe(c, N):
    kw = dict(sub=(100.0, 40.0), sub_tau=0.12, thunk=0.5, churn_rate=14, churn_depth=0.25, crackle=(50, 500, 4000, 0.5, 0.3))
    kw.update(PER_IMPACT.get(c.name, {}))
    return blast2(c, N, **kw)


def _biogun_impact_recipe(c, N):
    return blast2(c, N, crack_gain=0.8, sub_gain=0.8, thunk=0.3, churn_rate=18, churn_depth=0.35,
                  crackle=(90, 900, 4500, 0.6, 0.4), puffs=[(0.8, 0.06, 0.25), (0.95, 0.06, 0.2), (1.12, 0.06, 0.15)])


_reg2("powerhammer_impactground_*", _impact_recipe)
_reg2("powerhammer_impact_*", _impact_recipe)
_reg2("biogun_impact_*", _biogun_impact_recipe)
