"""Round 6, slice guns-shots: per-sound parameter sets found by a scripted search against the Audio Flamingo
early-layer distance (see sound-rounds/r6).  The sets replace the per-sound entries in the ``sounds_guns`` tables
(``PER`` for blast2 shots, ``PER_SUS`` for sustained textures, ``PER_MSA`` for the minigun spin-up), so every render
stays seeded and reproducible.  ``GG_TUNE6`` (a JSON file in the same shape) overrides them for the tuning harness
only."""
import json
import os

import sounds_guns as _sg
from sounds_guns import REGISTRY, _sustain_recipe

# name -> (table name, parameters)
_TUNED6 = {
    'as50_shot_b': ('PER', {"body_hi": 14000.0, "low_gain": 0.15934567035156247, "cen_k": 1.5895000000000001, "crack_gain": 0.4545, "thunk": 0.1019, "reseed": 68}),
    'biogun_shot_b': ('PER', {"ons_gain": 0.30000000000000004, "low_lp": 284.1071, "reseed": 43}),
    'biogun_shot_c': ('PER', {"tail_gain": 0.9141, "reseed": 26, "rumble_gain": 0.8949, "crack_hi": 7676.5937, "sub_gain": 0.8307, "low_gain": 1.3737, "ons_tau": 0.0387, "sub_tau": 0.0523, "dense": 1.1253, "ons_gain": 0.0995, "crack_lo": 1289.6466}),
    'blasterrifle_shot_c': ('PER', {"low_lp": 301.8916, "thunk": 0.1101, "crack_hi": 7540.8684, "body_hi": 3345.6369, "sub_gain": 1.2132, "crack_lo": 1689.2266}),
    'chainsaw_loop_a': ('PER_SUS', {"cen_k": 1.0, "body_hi": 3230.0106, "body_gain": 0.25, "hp": 800.0, "buzz": {"f0": 122, "f1": 118, "gain": 0.8104, "lp0": 2500, "lp1": 600, "lp_t": [0.5, 1.2], "rate": 31, "depth": 0.55, "nh": 30}}),
    'chainsaw_loop_b': ('PER_SUS', {"cen_k": 1.0, "body_hi": 2500.0, "body_gain": 0.25, "hp": 800.0, "buzz": {"f0": 118.0707, "f1": 115, "gain": 1.0, "lp0": 3000, "lp1": 600, "lp_t": [0.7, 1.5], "rate": 32, "depth": 0.5, "nh": 35}, "reseed": 59}),
    'chainsaw_swing_a': ('PER_SUS', {"cen_k": 1.214, "body_hi": 2392.2897, "body_gain": 0.4957, "hp": 414.6293, "smooth_s": 0.0314, "buzz": {"f0": 95, "f1": 127.3805, "gain": 0.6741, "lp0": 1591.7355, "lp1": 1607.6553, "rate": 50, "depth": 0.2842, "nh": 22}, "reseed": 54, "dense": 0.2754}),
    'chainsaw_swing_c': ('PER_SUS', {"cen_k": 1.1896, "body_hi": 2490.4318, "body_gain": 0.2616, "hp": 788.8823, "smooth_s": 0.0399, "buzz": {"f0": 58.865, "f1": 45.6758, "gain": 1.0, "lp0": 1726.1677, "lp1": 1800, "rate": 40, "depth": 0.504, "nh": 30}, "reseed": 52, "dense": 0.6609}),
    'combatshotgun_shot_a': ('PER', {"tail_gain": 0.9377, "low_gain": 0.65, "cen_k": 1.6874, "body_hi": 12390.0, "crack_gain": 0.5525, "low_lp": 214.2987, "dense": 1.615, "crack_lo": 2488.0273}),
    'grenadelauncher_shot_a': ('PER', {"ons_gain": 0.3027, "dense": 1.7519, "cen_k": 1.3827, "body_hi": 2136.7937500000003, "low_gain": 0.3305, "crack_gain": 0.9235, "tail_gain": 1.0507, "sub_tau": 0.1003, "ons_tau": 0.0587, "crack_hi": 9375.9809, "crack2_gain": 0.8239, "reseed": 58, "low_lp": 348.1126, "body_gain": 0.8067}),
    'handgun_shot_a': ('PER', {"body_gain": 0.9083, "cen_k": 2.6641}),
    'handgun_shot_c': ('PER', {"ons_tau": 0.046, "tail_gain": 1.3735, "sub_gain": 0.9839, "low_lp": 308.7467, "ons_gain": 0.105, "reseed": 37, "rumble_gain": 0.4492, "crack_lo": 3000, "body_gain": 0.7818, "cen_k": 2.9567, "thunk": 0.093, "sub_tau": 0.0713, "body_hi": 8485.7307}),
    'lmg_shot_a': ('PER', {"ons_gain": 0.3106, "tail_gain": 1.1599, "reseed": 25}),
    'lmg_shot_b': ('PER', {"cen_k": 5.3642, "crack_gain": 1.1459, "reseed": 26, "rumble_hp": 36.2971, "low_lp": 249.847, "tail_gain": 1.0659}),
    'mac10_shot_c': ('PER', {"sub_gain": 1.1294, "low_gain": 0.602, "cen_k": 3.3000000000000003, "crack_gain": 3, "crack_lo": 1139.3941, "rumble_gain": 0.7138, "reseed": 61, "dense": 1.9606, "thunk": 0.1352, "low_lp": 316.5022}),
    'pistol_shot_a': ('PER', {"tail_gain": 1.3289, "sub_gain": 1.4512, "rumble_gain": 0.6034, "cen_k": 2.451, "crack_lo": 1564.7894, "sub_tau": 0.1182}),
    'pistol_shot_b': ('PER', {"body_hi": 5503.0031, "reseed": 28, "dense": 2.0592, "crack2_gain": 0.4435, "thunk": 0.0823, "tail_gain": 1.2235}),
    'powerhammer_swing_b': ('PER_SUS', {"cen_k": 0.9946, "body_hi": 515.7796, "low": [25, 200, 0.7], "hiss": [2000, 8000, 0.1], "crack": [0.3, 6000], "smooth_s": 0.0201, "dense": 2.9977, "body_gain": 3.958, "hp": 62.1813, "reseed": 30}),
    'vector_shot_c': ('PER', {"sub_gain": 1.4568, "cen_k": 4.7283, "crack_hi": 10869.1823, "rumble_hp": 20.4233, "thunk": 0.2119, "body_hi": 3100.8637, "ons_gain": 0.2047, "reseed": 16, "crack_gain": 0.3834, "body_gain": 1.4195, "sub_tau": 0.0604, "tail_gain": 1.8399, "low_lp": 211.136, "ons_tau": 0.0261, "rumble_gain": 0.372, "crack_lo": 1789.383, "crack2_gain": 0.7022}),
}

if os.environ.get("GG_TUNE6"):  # tuning harness override (not used for the shipped files)
    with open(os.environ["GG_TUNE6"], encoding="utf-8") as _f:
        _TUNED6 = {**_TUNED6, **{k: tuple(v) for k, v in json.load(_f).items()}}

for _nm, (_tbl, _kw) in _TUNED6.items():
    getattr(_sg, _tbl)[_nm] = dict(_kw)
    if _nm == "chainsaw_loopstart":
        # a round-1 recipe by default; the sustained-texture engine once a tuned set exists
        REGISTRY.insert(0, ("chainsaw_loopstart", _sustain_recipe()))
