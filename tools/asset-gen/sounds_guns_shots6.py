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


# --------------------------------------------------------------------------- round 7
# Gate-aware search (sound-rounds/r7/search7.py): score = embedding distance plus a penalty per measured check that
# fails.  table "OTHER" is sounds_guns_other.PARAMS, "IMPACT7" the impact/chainsaw-hit table in sounds_guns.
_TUNED7 = {
    'ak47_reload': ('OTHER', {'amp_floor': 0.16999999999999998, 'bass': 0.0, 'bed': 0.8, 'bright': 1.2, 'fill': 0.127, 'fill_lo': 104.3, 'level_pow': 0.7, 'lows': [['pulses', 1.62, 2.65, 9.0, 0.5]], 'tau': 0.25, 'tex': 2.5, 'fill_db': 20.0, 'tex_bright': 0.8082, 'fill_churn': 0.2}),
    'aug_reload': ('OTHER', {'bass': 0.0, 'bright': 1.2, 'fill': 0.155, 'fill_db': 26.0, 'reseed': 57, 'tau': 0.028, 'tex': 0.59, 'tex_dens': 273.0, 'fill_churn': 0.73, 'mt': 0.6439}),
    'biogun_impact_a': ('IMPACT7', {'engine': 'blast', 'p': {'whoosh': 0.122, 'tail_gain': 1.4761, 'low_gain': 1.2413, 'reseed': 15}}),
    'biogun_impact_b': ('IMPACT7', {'engine': 'blast', 'p': {'ons_gain': 0.1467, 'low_lp': 211.3548}}),
    'biogun_impact_c': ('IMPACT7', {'engine': 'blast', 'p': {'crack2_gain': 0.643, 'body_gain': 1.4682, 'rumble_gain': 0.685}}),
    'blasterrifle_shot_c': ('PER', {'low_lp': 301.8916, 'thunk': 0.1101, 'crack_hi': 7540.8684, 'body_hi': 3345.6369, 'sub_gain': 1.2132, 'crack_lo': 1689.2266, 'dense': 1.4793, 'crack_gain': 0.946, 'sub_tau': 0.1182}),
    'boltaction_reload': ('OTHER', {'amp_floor': 0.112, 'bass': 0.0, 'bright': 1.5847, 'clk': 1.499, 'hit_ts': 1.2112, 'tau': 0.04875, 'tex': 0.9798, 'tex_bright': 1.4047}),
    'chainsaw_hit_a': ('IMPACT7', {'engine': 'blast', 'p': {'ons_gain': 0.3397, 'whoosh': 0.1213}}),
    'chainsaw_loopstart': ('PER_SUS', {'cen_k': 1.0, 'body_hi': 2000.0, 'body_gain': 0.3351, 'hp': 100.0, 'smooth_s': 0.03, 'buzz': {'f0': 100, 'f1': 120, 'gain': 1.0, 'lp0': 2500, 'lp1': 900, 'rate': 30, 'depth': 0.55, 'nh': 30}}),
    'chainsaw_swing_a': ('PER_SUS', {'cen_k': 1.5552, 'body_hi': 2392.2897, 'body_gain': 0.4957, 'hp': 414.6293, 'smooth_s': 0.0402, 'buzz': {'f0': 95, 'f1': 127.3805, 'gain': 0.6741, 'lp0': 1591.7355, 'lp1': 1607.6553, 'rate': 50, 'depth': 0.3908, 'nh': 22}, 'reseed': 10, 'dense': 0.2754}),
    'chainsaw_swing_c': ('PER_SUS', {'cen_k': 1.1896, 'body_hi': 1692.0177, 'body_gain': 0.2244, 'hp': 788.8823, 'smooth_s': 0.0399, 'buzz': {'f0': 73.2399, 'f1': 45.6758, 'gain': 1.0, 'lp0': 1726.1677, 'lp1': 1800, 'rate': 40, 'depth': 0.504, 'nh': 30}, 'reseed': 52, 'dense': 0.6609}),
    'combatshotgun_reload': ('OTHER', {'tex': 0.5, 'fill_churn': 0.44, 'tex_dens': 418.2977, 'fill': 0.206, 'fill_db': 20.0, 'level_pow': 0.7}),
    'combatshotgun_shot_b': ('PER', {'cen_k': 3.3000000000000003, 'body_hi': 4388.0414, 'ons_tau': 0.041, 'body_gain': 1.3993, 'reseed': 14, 'low_lp': 249.7673, 'low_gain': 1.0197}),
    'grenade_pin_a': ('OTHER', {'bass': 0.0, 'bright': 1.1, 'clk': 1.1501, 'fill': 0.156, 'fill_db': 20.0, 'fill_lo': 70.9, 'tau': 0.0474, 'tex': 0.2713}),
    'grenade_pin_b': ('OTHER', {'amp_floor': 0.341, 'bass': 0.0, 'bright': 1.1, 'comp': 0.3, 'fill': 0.879, 'fill_db': 39.6, 'level_pow': 0.559, 'tex': 0.3666, 'tex_bright': 1.2781, 'tex_dens': 318.1117, 'fill_hi': 4898.2}),
    'grenadelauncher_reload': ('OTHER', {'amp_floor': 0.514, 'bass': 0.0, 'bed': 0.6, 'bright': 0.78, 'fill': 0.134, 'fill_db': 40.0, 'hit_ts': 0.85, 'level_pow': 0.97, 'lows': [['rumble', 3.5, 4.3, 0.15]], 'mt': 0.9973, 'tau': 0.0283, 'tex': 0.5657, 'tex_dens': 425.9484}),
    'grenadelauncher_shot_a': ('PER', {'ons_gain': 0.3027, 'dense': 1.7519, 'cen_k': 1.3827, 'body_hi': 1658.1194, 'low_gain': 0.3305, 'crack_gain': 0.9235, 'tail_gain': 1.0507, 'sub_tau': 0.1003, 'ons_tau': 0.0587, 'crack_hi': 9375.9809, 'crack2_gain': 0.8239, 'reseed': 58, 'low_lp': 348.1126, 'body_gain': 0.8067, 'rumble_gain': 0.4815}),
    'handgun_reload': ('OTHER', {'bass': 0.0, 'bed': 0.6, 'bright': 1.2, 'comp': 0.3, 'level_pow': 1.1, 'lows': [['rumble', 0.0, 1.3, 0.5]], 'tau': 0.022894218750000004, 'tex': 1.3314824999999997, 'mt': 0.64, 'fill_dens': 60}),
    'handgun_shot_a': ('PER', {'body_gain': 0.9083, 'cen_k': 2.6641, 'dense': 2.0793, 'body_hi': 4378.3263, 'crack2_gain': 0.4339}),
    'handgun_shot_c': ('PER', {'ons_tau': 0.046, 'tail_gain': 1.3735, 'sub_gain': 0.9839, 'low_lp': 308.7467, 'ons_gain': 0.105, 'reseed': 25, 'rumble_gain': 0.4492, 'crack_lo': 3000, 'body_gain': 0.7818, 'cen_k': 2.9567, 'thunk': 0.093, 'sub_tau': 0.0713, 'body_hi': 8485.7307, 'dense': 2.2434}),
    'lmg_reload': ('OTHER', {'bass': 0.0, 'bed': 0.06, 'bright': 1.02, 'fill': 0.267, 'fill_churn': 0.31, 'fill_db': 20.0, 'lows': [['rumble', 0.0, 0.8, 0.8]], 'reseed': 23, 'tau': 0.11249999999999999, 'tex': 0.325, 'tex_dens': 218.6694, 'clk': 0.9331}),
    'lmg_shot_a': ('PER', {'ons_gain': 0.3106, 'tail_gain': 1.1599, 'reseed': 25, 'cen_k': 2.8561, 'rumble_hp': 43.0071}),
    'mac10_reload_akimbo': ('OTHER', {'amp_floor': 0.95, 'bass': 0.0, 'bed': 0.0, 'bright': 1.4031, 'clk': 1.2562, 'comp': 0.3, 'fill': 0.311, 'fill_churn': 0.28, 'fill_db': 39.0, 'hit_ts': 2.416, 'level_pow': 0.86, 'mt': 1.1925, 'reseed': 41, 'tau': 0.04875, 'tex': 0.16666666666666666, 'tex_bright': 1.586, 'tex_dens': 665.9214, 'fill_dens': 60}),
    'mac10_shot_c': ('PER', {'sub_gain': 0.9989, 'low_gain': 0.602, 'cen_k': 3.0466, 'crack_gain': 3, 'crack_lo': 1139.3941, 'rumble_gain': 0.7138, 'reseed': 61, 'dense': 1.9606, 'thunk': 0.1352, 'low_lp': 316.5022, 'body_hi': 6145.0649}),
    'mib_blaster_reload_akimbo': ('OTHER', {'bass': 0.0, 'bed': 0.8, 'bright': 1.2, 'glides': [[2.6, 2.99, 1000, 2400, 0.6]], 'level_pow': 1.1, 'lows': [['rumble', 1.4, 2.5, 0.1]], 'tau': 0.13274999999999998, 'tex': 1.5, 'tex_dens': 475.7783}),
    'pdw_reload': ('OTHER', {'bass': 0.0, 'bright': 0.7636, 'clk': 1.2715, 'fill': 0.149, 'fill_db': 18.3, 'fill_lo': 212.1, 'mt': 0.6928, 'tex': 1.1455, 'tex_dens': 421.2973}),
    'pistol_shot_b': ('PER', {'body_hi': 5503.0031, 'reseed': 28, 'dense': 2.0592, 'crack2_gain': 0.4435, 'thunk': 0.0584, 'tail_gain': 1.6876, 'ons_tau': 0.0449, 'body_gain': 1.0128}),
    'powerhammer_impact_c': ('IMPACT7', {'engine': 'blast', 'p': {'ons_gain': 0.0813, 'crack_gain': 0.947}}),
    'powerhammer_impactground_a': ('IMPACT7', {'engine': 'blast', 'p': {'ons_tau': 0.0267, 'churn_rate': 18.5011}}),
    'powerhammer_reload': ('OTHER', {'bass': 0.0, 'bed': 0.467, 'bright': 1.2, 'clk': 1.5464, 'fill': 0.105, 'fill_db': 23.9, 'lows': [['rumble', 2.3, 3.3, 0.6], ['pulses', 1.75, 2.0, 8.0, 0.3]], 'tex': 0.75}),
    'revolver_reload': ('OTHER', {'amp_floor': 0.08000000000000002, 'bass': 0.0, 'bed': 0.8, 'bright': 0.78, 'clk': 0.6021, 'fill': 0.27, 'fill_db': 40.0, 'hit_ts': 0.4, 'level_pow': 0.709, 'mt': 0.6134, 'tau': 0.19740489743987993, 'tex': 1.4540833199999998, 'tex_dens': 273.0, 'tex_bright': 1.5773}),
    'revolver_reload_akimbo': ('OTHER', {'bass': 0.0, 'bed': 0.8, 'bright': 1.2, 'clk': 1.1522, 'fill': 0.176, 'fill_churn': 0.6, 'fill_lo': 196.8, 'hit_ts': 0.4, 'level_pow': 0.8599999999999999, 'mt': 0.4972, 'tau': 0.13314824999999997, 'tex': 0.885, 'tex_dens': 322.14}),
    'scar_reload': ('OTHER', {'amp_floor': 0.439, 'bass': 0.0, 'bright': 0.6707, 'comp': 0.3, 'level_pow': 0.65, 'mt': 1.172, 'tex': 0.75, 'tex_bright': 1.249, 'fill': 0.162, 'fill_db': 26.0, 'fill_dens': 60}),
    'scar_shot_c': ('PER', {'reseed': 59, 'cen_k': 2.0113}),
    'teslagun_shot_a': ('PER', {'cen_k': 3.3000000000000003, 'crack_gain': 1.6435, 'low_gain': 0.6807, 'crack_hi': 11267.0162, 'body_hi': 7525.4656, 'sub_tau': 0.0691, 'body_gain': 0.789, 'tail_gain': 0.6749, 'crack2_gain': 1.3104, 'sub_gain': 1.3051, 'crack_lo': 1107.0086, 'ons_tau': 0.0393}),
    'thompson_reload': ('OTHER', {'amp_floor': 0.145, 'bass': 0.0, 'bed': 0.384, 'bright': 1.8178, 'clk': 0.705, 'comp': 0.3, 'fill': 0.083, 'fill_churn': 0.78, 'fill_db': 20.2, 'fill_hi': 4059.8, 'hit_ts': 1.0026, 'tau': 0.047, 'tex': 0.5, 'reseed': 46}),
    'thompson_shot_c': ('PER', {'sub_gain': 0.425, 'low_gain': 0.5525, 'rumble_gain': 0.615, 'cen_k': 3.3000000000000003, 'body_hi': 6997.9616, 'crack_gain': 0.65, 'sub_tau': 0.0928, 'tail_gain': 1.2148, 'rumble_hp': 25.7631, 'reseed': 39, 'body_gain': 1.6971, 'low_lp': 140.5793, 'ons_tau': 0.0125}),
    'vector_reload': ('OTHER', {'amp_floor': 0.16999999999999998, 'bass': 0.0, 'bed': 0.06, 'bright': 2.4771, 'clk': 0.9256, 'fill': 0.446, 'fill_churn': 0.78, 'fill_db': 24.1, 'fill_dens': 115.7302, 'fill_lo': 61.3, 'hit_ts': 1.5, 'level_pow': 0.299, 'reseed': 57, 'tau': 0.04889625, 'tex': 1.0607, 'tex_bright': 1.0295, 'tex_dens': 498.0386}),
    'vector_shot_a': ('PER', {'sub_gain': 0.818, 'low_gain': 0.1447, 'cen_k': 3.1261, 'body_hi': 2113.5983, 'crack_gain': 0.1618, 'rumble_gain': 0.10235416666666665, 'thunk': 0.3288, 'dense': 1.6833, 'reseed': 76, 'crack_hi': 5000, 'sub_tau': 0.0801, 'crack_lo': 2375.0697}),
    'vector_shot_c': ('PER', {'sub_gain': 1.4568, 'cen_k': 4.7283, 'crack_hi': 10869.1823, 'rumble_hp': 20.4233, 'thunk': 0.2119, 'body_hi': 3100.8637, 'ons_gain': 0.2047, 'reseed': 16, 'crack_gain': 0.3834, 'body_gain': 1.4195, 'sub_tau': 0.0415, 'tail_gain': 1.8399, 'low_lp': 211.136, 'ons_tau': 0.0261, 'rumble_gain': 0.372, 'crack_lo': 1789.383, 'crack2_gain': 0.7022}),
}

import sounds_guns_other as _so  # noqa: E402

for _nm, (_tbl, _kw) in _TUNED7.items():
    if _tbl == "OTHER":
        _so.PARAMS[_nm] = dict(_kw)
    elif _tbl == "IMPACT7":
        _sg.IMPACT7[_nm] = dict(_kw)
    else:
        getattr(_sg, _tbl)[_nm] = dict(_kw)
        if _nm == "chainsaw_loopstart":
            REGISTRY.insert(0, ("chainsaw_loopstart", _sustain_recipe()))
