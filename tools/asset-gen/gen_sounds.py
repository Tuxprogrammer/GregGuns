"""Generate every manifest sound entry as an original, procedurally synthesized .ogg.

Usage (from the repo root):
    python -I -X utf8 tools/asset-gen/gen_sounds.py [--only SUBSTR] [--prio P1,P2,P3] [--jobs N]
                                                   [--only-file PATHS.txt] [--non-keep] [--no-write]

Layout (round 1 of the A/B revision):
    gen_sounds.py     shared machinery: context, synthesis dispatch, finishing (loudness, true peak, Vorbis),
                      sounds.json fragment, credits and report.  Also the pre-revision text-driven interpreter
                      (``synth_legacy``), which is now only used by sounds without a family recipe
                      (flashbang_bang, which was never reviewed and must stay byte-identical).
    sndlib.py         recipe registry, measured targets (sound-targets.json), the conforming stage
                      (band balance, loudness contour, crest, silent end) and small reusable voices.
    sndengines.py     shared engines: blast (shots, impacts, explosions), events (mechanical foley),
                      rumble (sub-heavy textures), electronic beeps.
    sounds_guns.py, sounds_npcs.py, sounds_effects.py, sounds_misc.py
                      per-family recipes, registered by lower-case name prefix.  One agent per family.

Design reference: the written text in docs/techguns-spec/AUDIO-PROFILES.md and the numeric measurements of
the sound being replaced (length, decay points, onset times, band energies, loudness contour; stored in
sound-targets.json).  No audio file is read, sampled or convolved.  Each sound is seeded from its id, so a
re-run reproduces the same files.

``--non-keep`` renders only sounds that tools/asset-gen/sound-status.json lists with a verdict other than
keep; keep sounds (and sounds with no review status) are never touched and stay byte-identical.
"""
from __future__ import annotations

import csv
import json
import os
import time
import re
import subprocess
import sys
import zlib
from multiprocessing import Pool

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import numpy as np  # noqa: E402

import sfx  # noqa: E402
import sndlib  # noqa: E402
# Per-family recipe modules register themselves with sndlib.recipe(); import order only matters
# when two patterns could match the same name (first registered wins).
import sounds_guns  # noqa: E402,F401
import sounds_npcs  # noqa: E402,F401
import sounds_effects  # noqa: E402,F401
import sounds_misc  # noqa: E402,F401

ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
MANIFEST = os.path.join(HERE, "manifest.json")
PROFILES = os.path.join(ROOT, "docs", "techguns-spec", "AUDIO-PROFILES.md")
OUT_ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "gregguns")
FRAGMENT = os.path.join(HERE, "sounds.fragment.json")
CREDITS = os.path.join(HERE, "credits", "sounds.csv")
REPORT = os.path.join(ROOT, "docs", "assets", "previews", "sounds-report.md")
STATS = os.path.join(HERE, "credits", ".sound-stats.json")
STATUS = os.path.join(HERE, "sound-status.json")
SCRATCH = os.environ.get("GG_SCRATCH", os.path.join(HERE, ".cache"))
GEN_NAME = "tools/asset-gen/gen_sounds.py + sfx.py"
GEN_NAME_R1 = "tools/asset-gen/gen_sounds.py + sndlib.py + sndengines.py + sounds_*.py + sfx.py"
METHOD_OLD = "Procedural numpy synthesis (noise/FM/modal layers + synthetic-IR reverb) from written profile; libvorbis q5"
METHOD_R1 = ("Procedural numpy synthesis: per-family noise/transient/foley recipes conformed to the measured band balance, "
             "loudness contour and crest of the sound it replaces; true-peak limiter; libvorbis q7")

TWO_PI = 2 * np.pi


# =========================================================================== profile parsing
def parse_profiles():
    txt = open(PROFILES, encoding="utf8").read()
    events = {}
    for m in re.finditer(r"^\| `(sounds/[^`]+)` \| ([^|]+?) \|", txt, re.M):
        events[m.group(1)] = m.group(2).strip()
    out = {}
    parts = re.split(r"^### `(sounds/[^`]+)`\s*$", txt, flags=re.M)
    for i in range(1, len(parts), 2):
        path, body = parts[i], parts[i + 1]
        p = {"event": events.get(path), "text": body}
        m = re.search(r"centroid (\d+) Hz", body)
        p["centroid"] = float(m.group(1)) if m else None
        m = re.search(r"-20 dB by (?:about )?([\d.]+) s", body)
        p["dec20"] = float(m.group(1)) if m else None
        m = re.search(r"-40 dB by (?:about )?([\d.]+) s", body)
        p["dec40"] = float(m.group(1)) if m else None
        m = re.search(r"\*\*Loop:\*\* ([^.]+)\.", body)
        p["loop"] = m.group(1).strip() if m else ""
        m = re.search(r"- \*\*Elements:\*\* (.*)", body)
        els = []
        if m:
            for piece in re.split(r";\s+(?=\d+\.\d+\s*\D{1,3}\s*\d+\.\d+\s*s\b)", m.group(1)):
                mm = re.match(r"(\d+\.\d+)\s*\D{1,3}?\s*(\d+\.\d+)\s*s\s*(.*)", piece.strip(), re.S)
                if mm:
                    els.append((float(mm.group(1)), float(mm.group(2)), mm.group(3)))
                elif els:
                    t0, t1, tx = els[-1]
                    els[-1] = (t0, t1, tx + " " + piece)
        p["elements"] = els
        out[path] = p
    return out


# =========================================================================== context
class Ctx:
    def __init__(self, entry, prof):
        self.e = entry
        self.id = entry["id"]
        self.path = entry["path"]
        self.base = os.path.splitext(os.path.basename(self.path))[0]
        self.name = self.base.lower()
        self.sr = int(entry["sample_rate"])
        self.dur = float(entry["duration_s"])
        self.prof = prof or {}
        self.rng = np.random.default_rng(zlib.crc32(self.id.encode()))
        self.n = sfx.n_of(self.sr, self.dur)
        self.scifi = False
        self.looping = False
        self.tg = None            # measured targets of the original (see sndlib.Tg)
        self.loop_xf = 0.0
        self.conform_env = True   # reshape slow loudness contour to the original's
        self.conform_bands = True  # match static band balance to the original's


def T(w):
    return re.compile(r"\b(?:%s)" % w)


TAGS = {
    "click": T(r"click|tick|tap\b|taps\b|pop\b|clicky"),
    "clack": T(r"clack|clank|latch|clink|ping|snap|slap|stamp|slam|metallic|bolt|slide|rack|shell|cock"),
    "thunk": T(r"thunk|thud|thump|clunk|knock|bump|stomp|footstep|step\b|impact|hit\b|slam|seat"),
    "crack": T(r"crack|muzzle|blast|bang|detonat|explo|shot|report|discharge|boom|snap\b"),
    "boom": T(r"boom|blast|explo|thump|bass|sub\b|detonat|low-end|heavy|body|thud|punch"),
    "rumble": T(r"rumbl|growl|low-frequency|churn|engine|motor|drone|sub\b|bass|low rumble"),
    "noise": T(r"noise|noisy|hiss|wash|broadband|spray|whoosh|rush|wind|turbulen|debris|texture|static|sizzl|fizz|grain|breath|steam|puff|air\b"),
    "tail": T(r"tail|decay|fading|fade|reverb|echo|dying|reflection|ring-out|residue|residual"),
    "rattle": T(r"rattl|flutter|crackl|chatter|pulsing|pulses|granular|stutter|patter|chitter|scatter"),
    "scrape": T(r"scrap|grind|friction|scuff|rustl|handling|dragg|creak|squeak|rasp|churn|wet|squelch"),
    "tone": T(r"beep|tone|tonal|whine|hum\b|humming|whirr|sine|chirp|blip|zap|warble|sweep|siren|squeal|buzz|whistle|charge|ringing"),
    "silent": T(r"silen|gap|nothing|pause|quiet gap"),
}
LOUD = re.compile(r"\b(loudest|loud|strong|strongest|heavy|big|huge|dominant|hard|main|massive|full|biggest)\b")
QUIET = re.compile(r"\b(faint|quiet|tiny|weak|residual|barely|trace|thin|soft|light|minor|low-level|lower-level|small|softer|quieter|weaker)\b")
VQUIET = re.compile(r"\b(very quiet|near-silent|barely|very faint|trace|tiny)\b")
HZ = re.compile(r"(\d[\d,.]*)\s*(?:[-\u2013]\s*(\d[\d,.]*)\s*)?(k?)Hz")


def hz_cues(text):
    out = []
    for m in HZ.finditer(text):
        try:
            v = float(m.group(1).replace(",", ""))
        except ValueError:
            continue
        if m.group(3) == "k" or "k" in text[m.start():m.end()].lower().replace("hz", ""):
            v *= 1000.0
        out.append(v)
    return out


NUMW = {"two": 2, "three": 3, "four": 4, "five": 5, "several": 4, "few": 3, "cluster": 4, "series": 5, "rapid": 5}


# =========================================================================== generic element interpreter
def lvl_of(w):
    if VQUIET.search(w):
        return 0.14
    lo, qu = LOUD.search(w), QUIET.search(w)
    if lo and not qu:
        return 1.0
    if qu and not lo:
        return 0.3
    return 0.6


def env_for(w, n, sr, d):
    swell = re.search(r"swell|build|rising|grow|surge|ramp|fades? in|fast swell", w)
    fall = re.search(r"fad|decay|dying|drops?|thin|trail|settl|away|dies|steadily dropping|falls", w)
    if swell and fall:
        a = max(0.1, d * 0.35)
        return sfx.smooth_env(n, sr, a, d * 0.5)
    if swell:
        return np.linspace(0, 1, n) ** 1.4
    if fall:
        return sfx.exp_env(n, sr, d * 0.38, attack=min(0.01, d * 0.2))
    return sfx.smooth_env(n, sr, min(0.03, d / 3), min(0.06, d / 3))


def render_element(c, out, t0, t1, text):
    sr, rng = c.sr, c.rng
    w = text.lower()
    d = max(t1 - t0, 0.02)
    n = sfx.n_of(sr, d)
    lvl = lvl_of(w)
    tags = {k: bool(rx.search(w)) for k, rx in TAGS.items()}
    other = [k for k, v in tags.items() if v and k != "silent"]
    if tags["silent"] and not other:
        return
    # explicit onset times inside the window
    times = [float(x) for x in re.findall(r"(\d+\.\d+)\s*s?\b", w)]
    times = [x for x in times if t0 - 0.03 <= x <= t1 + 0.05 and abs(x - d) > 1e-6]
    count = 1
    for k, v in NUMW.items():
        if re.search(r"\b%s\b" % k, w):
            count = max(count, v)
    if not times:
        if count > 1:
            times = list(t0 + np.sort(rng.random(count)) * d * 0.9)
        else:
            times = [t0]
    elif len(times) > 8:
        times = times[:8]
    metallic = bool(re.search(r"metal|ring|clank|clang|bright", w))
    hits = tags["click"] or tags["clack"] or tags["thunk"]
    # ---- discrete hits
    if hits and not (tags["crack"] and re.search(r"muzzle|blast|boom|explo", w)):
        for i, ti in enumerate(times):
            kinds = []
            if tags["click"]:
                kinds.append("click")
            if tags["clack"]:
                kinds.append("clack")
            if tags["thunk"]:
                kinds.append("thunk")
            kind = kinds[int(rng.integers(len(kinds)))]
            g = lvl * (1.0 if i == len(times) - 1 or count == 1 else rng.uniform(0.5, 0.9))
            cue = [h for h in hz_cues(w) if 150 < h < 6000]
            if kind == "click":
                s = sfx.click(sr, rng, bright=rng.uniform(0.8, 1.3))
            elif kind == "clack":
                s = sfx.clack(sr, rng, f0=(cue[0] if cue and rng.random() < .6 else None), tau=rng.uniform(0.02, 0.06))
            else:
                heavy = lvl > 0.8
                s = sfx.thunk(sr, rng, f0=rng.uniform(70, 130) if heavy else rng.uniform(150, 260), tau=0.1 if heavy else 0.04, dur=0.3 if heavy else 0.16)
                if metallic:
                    cl = sfx.clack(sr, rng, tau=0.05)
                    s = np.pad(s, (0, max(0, len(cl) - len(s))))
                    s[:len(cl)] += 0.5 * cl
            sfx.place(out, s, ti, sr, g * (1.2 if kind == "thunk" else 1.0))
    # ---- muzzle blast / explosion
    if tags["crack"] and (re.search(r"muzzle|blast|bang|explo|shot|report|crack|detonat|boom|discharge", w)):
        k = 0.45 if c.scifi else 1.0
        t = times[0]
        sfx.place(out, sfx.crack(sr, rng, tau=rng.uniform(0.008, 0.018), bright=rng.uniform(0.7, 1.1)), t, sr, lvl * 1.1 * k)
        if tags["boom"] or d > 0.2:
            big = c.name.startswith(("sawedoff", "combatshotgun", "as50", "rocket", "goldenrev", "bolt", "explosion", "nuke", "apache"))
            bd = min(max(d * 1.2, 0.3), 2.5)
            sfx.place(out, sfx.boom(sr, rng, dur=bd, f0=rng.uniform(55, 80) if big else rng.uniform(80, 130),
                                    tau=min(bd * 0.3, 0.45 if big else 0.2), grit=0.55), t, sr, lvl * 1.15 * (0.7 if c.scifi else 1.0))
    # ---- rumble bed
    if tags["rumble"] and not tags["crack"]:
        cue = [h for h in hz_cues(w) if h < 400]
        top = rng.uniform(160, 380)
        bed = sfx.band_noise(n, sr, rng, 35, top, order=2) * sfx.rand_mod(n, sr, rng, 14, 0.6)
        if re.search(r"engine|motor|drone|hum|churn|pulsing|grind", w):
            f0 = cue[0] if cue else rng.uniform(55, 95)
            bed = bed * 0.7 + 0.5 * sfx.engine(n, sr, rng, f0=f0, rough=0.7, nh=18) * 0.8
            bed = sfx.fftfilt(bed, sr, lp=2200, order=1)
        sfx.place(out, bed * env_for(w, n, sr, d), t0, sr, lvl * 1.3)
    elif tags["rumble"] and tags["crack"]:
        bed = sfx.dark_noise(n, sr, rng, 900, 120, hp=30)
        sfx.place(out, bed * sfx.exp_env(n, sr, d * 0.4), t0, sr, lvl * 0.9)
    # ---- noise bed
    if tags["noise"]:
        bright = bool(re.search(r"bright|high|hiss|air|presence|sizzl|fizz", w))
        dark = bool(re.search(r"dark|muffled|low|boomy|rumbl", w))
        lo, hi = (3000, 12000) if bright else ((60, 700) if dark else (300, 4500))
        bed = sfx.band_noise(n, sr, rng, lo, min(hi, sr * 0.45), order=2)
        bed *= sfx.rand_mod(n, sr, rng, 25, 0.5) if re.search(r"crack|grit|rough|irregular|turbul", w) else 1.0
        sfx.place(out, bed * env_for(w, n, sr, d), t0, sr, lvl * 0.9)
    # ---- tail
    if tags["tail"]:
        tail = sfx.dark_noise(n, sr, rng, 2500, 150, hp=40)
        sfx.place(out, tail * sfx.exp_env(n, sr, d * 0.36) * sfx.smooth_env(n, sr, min(0.12, d * 0.3), 0.0), t0, sr, lvl * 0.3)
    # ---- rattle / crackle train
    if tags["rattle"]:
        rate = 55 if re.search(r"rapid|fast|dense|flutter", w) else 22
        low = bool(re.search(r"low|bass|thud|rumbl", w))
        pt = sfx.pulse_train(n, sr, rng, rate, tau=0.005 if not low else 0.012, bp=(250, 1800) if low else (1200, 7500))
        sfx.place(out, pt * env_for(w, n, sr, d) * 2.0, t0, sr, lvl * 0.9)
    # ---- scrape / grind / handling
    if tags["scrape"]:
        sc = sfx.band_noise(n, sr, rng, 400, 5000, order=2) * sfx.rand_mod(n, sr, rng, 11, 1.0) ** 2
        sfx.place(out, sc * env_for(w, n, sr, d), t0, sr, lvl * 0.7)
    # ---- tonal
    if tags["tone"]:
        cue = [h for h in hz_cues(w) if 60 < h < 9000]
        f = cue[0] if cue else rng.uniform(500, 1500)
        if re.search(r"fall|sink|sinks|downward|drops|dropping pitch|descend", w):
            f0, f1 = f * 1.15, f * 0.8
        elif re.search(r"ris(e|ing)|upward|climb|swell", w):
            f0, f1 = f * 0.8, f * 1.15
        else:
            f0 = f1 = f
        ph = sfx.sweep(f0, f1, n, sr, k=d * 0.6) if f0 != f1 else sfx.osc_phase(np.full(n, f), sr)
        tone = np.sin(ph) + 0.25 * np.sin(2 * ph) * (1 if re.search(r"buzz|harsh|whine|whirr", w) else 0)
        if re.search(r"warble|waver|flutter", w):
            tone *= 0.7 + 0.3 * np.sin(TWO_PI * 9 * sfx.tvec(n, sr))
        sfx.place(out, tone * sfx.smooth_env(n, sr, min(0.01, d / 4), min(0.04, d / 3)), t0, sr, lvl * 0.55)
    if not other:
        bed = sfx.band_noise(n, sr, rng, 80, 2500, order=1)
        sfx.place(out, bed * env_for(w, n, sr, d), t0, sr, lvl * 0.3)


def interpret(c, elements):
    N = c.n + sfx.n_of(c.sr, 1.5)
    out = np.zeros(N)
    for t0, t1, tx in elements:
        if t0 >= c.dur:
            continue
        render_element(c, out, t0, min(t1, c.dur), tx)
    return out


# =========================================================================== sci-fi flavor layers
def scifi_layer(c, N):
    sr, rng, nm = c.sr, c.rng, c.name
    out = np.zeros(N)
    d = c.dur

    def add(sig, t=0.0, g=1.0):
        sfx.place(out, sig, t, sr, g)

    if nm.startswith("lasergun"):
        add(sfx.fm_zap(sr, rng, min(d, 0.55), rng.uniform(3000, 3800), rng.uniform(380, 600), ratio=2.0, index=3.5), 0, 1.0)
        add(sfx.fm_zap(sr, rng, 0.9, 900, 160, ratio=1.0, index=1.5, tau=0.3), 0, 0.35)
    elif nm.startswith(("blasterrifle", "alienblaster", "mib_blaster")):
        add(sfx.fm_zap(sr, rng, min(d, 0.7), rng.uniform(1800, 2400), rng.uniform(150, 260), ratio=1.41, index=5.0, ring=0.35), 0, 1.0)
        add(sfx.thump_sub(sr, 70, 0.4, 0.15), 0.0, 0.7)
    elif nm.startswith("cdb"):
        add(sfx.fm_zap(sr, rng, 1.2, rng.uniform(900, 1300), 90, ratio=0.5, index=7.0, tau=0.5, ring=0.3), 0, 1.0)
        add(sfx.boom(sr, rng, 0.9, 60, 0.3), 0, 0.8)
    elif nm.startswith("pulserifle"):
        add(sfx.fm_zap(sr, rng, 0.5, rng.uniform(1500, 2000), 280, ratio=3.0, index=3.0), 0, 0.9)
        add(sfx.thump_sub(sr, 60, 0.5, 0.2), 0.0, 0.9)
    elif nm.startswith("biogun"):
        n = sfx.n_of(sr, min(d, 0.9))
        gur = sfx.fftfilt(sfx.noise(n, rng), sr, hp=200, lp=1400) * (0.5 + 0.5 * np.sin(TWO_PI * rng.uniform(22, 34) * sfx.tvec(n, sr)))
        add(gur * sfx.exp_env(n, sr, 0.25), 0, 1.2)
        add(sfx.fm_zap(sr, rng, 0.5, 1100, 300, ratio=1.0, index=2.0), 0, 0.5)
    elif nm.startswith("teslagun"):
        n = sfx.n_of(sr, min(d, 2.0))
        cr = sfx.pulse_train(n, sr, rng, 160, tau=0.0015, bp=(2500, 11000)) * sfx.exp_env(n, sr, 0.6)
        add(cr, 0, 1.5)
        add(sfx.fm_zap(sr, rng, 0.4, 6500, 450, ratio=2.5, index=4.0, ring=0.5), 0, 0.8)
        bz = sfx.electric_buzz(n, sr, rng, 100.0, 0.3) * sfx.exp_env(n, sr, 0.8)
        add(bz, 0.05, 0.6)
    elif nm.startswith("sonicshotgun"):
        add(sfx.fm_zap(sr, rng, 0.9, 520, 55, ratio=0.5, index=6.0, tau=0.35, ring=0.6), 0, 1.1)
        add(sfx.boom(sr, rng, 0.6, 70, 0.15), 0, 0.7)
    elif nm.startswith("powerhammer_shot"):
        add(sfx.fm_zap(sr, rng, 1.0, 600, 90, ratio=0.5, index=5.0, tau=0.4), 0, 0.9)
        add(sfx.boom(sr, rng, 0.9, 55, 0.3), 0, 0.9)
    return out


SCIFI_PREFIX = ("lasergun_s", "blasterrifle", "alienblaster_s", "mib_blaster_s", "cdb_", "pulserifle_s", "biogun_s",
                "teslagun_s", "sonicshotgun_s", "powerhammer_shot")


# =========================================================================== named custom recipes
def explosion(c, N, power=1.0, dur=None, debris=0.6):
    sr, rng = c.sr, c.rng
    dur = dur or c.dur
    out = np.zeros(N)
    sfx.place(out, sfx.crack(sr, rng, tau=0.02, bright=0.8, dur=0.2), 0.0, sr, 0.9)
    sfx.place(out, sfx.boom(sr, rng, dur=min(dur, 3.0), f0=rng.uniform(42, 60), tau=min(0.18 + 0.12 * dur, 1.4), grit=0.8), 0.0, sr, 1.2 * power)
    n = sfx.n_of(sr, dur)
    roar = sfx.dark_noise(n, sr, rng, 3500, 120, hp=25) * sfx.exp_env(n, sr, dur * 0.28, attack=0.01)
    out[:n] += roar * 1.3
    if debris:
        nd = sfx.n_of(sr, dur * 0.8)
        deb = sfx.pulse_train(nd, sr, rng, 28, jitter=0.9, tau=0.012, bp=(300, 4000), amp_var=0.9) * sfx.exp_env(nd, sr, dur * 0.3)
        sfx.place(out, deb, dur * 0.12, sr, debris * 1.4)
    return out


def recipe_named(c, N):
    """Return a signal for sounds with a dedicated recipe, else None."""
    sr, rng, nm, d = c.sr, c.rng, c.name, c.dur
    t = sfx.tvec(N, sr)
    out = np.zeros(N)

    def add(sig, at=0.0, g=1.0):
        sfx.place(out, sig, at, sr, g)

    # ----- loops / stationary
    if nm.startswith("beamgun_loop"):
        c.looping = True
        n = N
        f0 = rng.uniform(95, 140)
        add(sfx.electric_buzz(n, sr, rng, f0, crackle=0.8))
        add(sfx.servo_whine(n, sr, rng, 640 * rng.uniform(.9, 1.1), 700 * rng.uniform(.9, 1.1), wobble=0.03), 0, 0.25)
        add(sfx.band_noise(n, sr, rng, 2000, 9000) * sfx.rand_mod(n, sr, rng, 40, 0.7), 0, 0.25)
        return out
    if nm.startswith("beamgun_start"):
        n = N
        sw = sfx.sweep(150, 900, sfx.n_of(sr, d), sr, k=d * 0.5)
        sig = np.sin(sw) + 0.4 * np.sin(2 * sw)
        add(sig * np.linspace(0.2, 1, len(sig)) ** 1.5, 0, 0.7)
        add(sfx.electric_buzz(sfx.n_of(sr, d), sr, rng, 120, 0.7) * np.linspace(0, 1, sfx.n_of(sr, d)) ** 2, 0, 0.9)
        add(sfx.click(sr, rng), 0, 1.0)
        add(sfx.thump_sub(sr, 60, 0.4, 0.15), d * 0.6, 0.7)
        return out
    if nm in ("chainsaw_loop_a", "chainsaw_loop_b", "chainsaw_loop_c"):
        c.looping = True
        f0 = {"a": 62, "b": 68, "c": 75}[nm[-1]] * rng.uniform(.97, 1.03)
        add(sfx.engine(N, sr, rng, f0=f0, rough=1.0, nh=34))
        add(sfx.band_noise(N, sr, rng, 800, 6000) * sfx.rand_mod(N, sr, rng, f0 / 2, 0.9), 0, 0.35)
        return out
    if nm == "chainsaw_loopstart":
        n = sfx.n_of(sr, d)
        f = np.linspace(35, 70, n)
        eng = sfx.harmonic_stack(f, n, sr, nh=30, rolloff=0.9, rng=rng) * (0.65 + 0.35 * np.sin(sfx.osc_phase(f * 0.5, sr)))
        env = sfx.smooth_env(n, sr, d * 0.5, d * 0.5)
        add(eng * env)
        add(sfx.band_noise(n, sr, rng, 150, 3000) * env, 0, 0.5)
        return out
    if nm == "chainsaw_loopend":
        n = sfx.n_of(sr, d)
        f = np.linspace(72, 28, n)
        eng = sfx.harmonic_stack(f, n, sr, nh=30, rolloff=0.9, rng=rng) * (0.65 + 0.35 * np.sin(sfx.osc_phase(f * 0.5, sr)))
        env = np.exp(-sfx.tvec(n, sr) / (d * 0.4))
        add(eng * env)
        add(sfx.band_noise(n, sr, rng, 150, 3000) * env, 0, 0.5)
        add(sfx.thunk(sr, rng, 70, 0.1), d * 0.6, 0.3)
        return out
    if nm.startswith("chainsaw_hit"):
        n = sfx.n_of(sr, d)
        f0 = rng.uniform(70, 110)
        eng = sfx.engine(n, sr, rng, f0=f0, rough=1.2, nh=30)
        rasp = sfx.band_noise(n, sr, rng, 600, 7000) * sfx.rand_mod(n, sr, rng, f0, 1.0) ** 2
        env = sfx.smooth_env(n, sr, 0.01, 0.35) * (0.7 + 0.3 * sfx.rand_mod(n, sr, rng, 12, 0.8))
        add(eng * env * 0.8)
        add(rasp * env, 0, 0.9)
        add(sfx.clack(sr, rng, tau=0.05), 0, 0.7)
        return out
    if nm.startswith("chainsaw_swing"):
        n = sfx.n_of(sr, d)
        f = np.linspace(55, 95, n) * (1 + 0.1 * np.sin(np.pi * np.linspace(0, 1, n)))
        eng = sfx.harmonic_stack(f, n, sr, nh=28, rolloff=0.9, rng=rng) * (0.65 + 0.35 * np.sin(sfx.osc_phase(f * 0.5, sr)))
        whoosh = sfx.dark_noise(n, sr, rng, 800, 3500, hp=200) * np.sin(np.pi * np.linspace(0.05, 0.95, n)) ** 2
        add(eng * sfx.smooth_env(n, sr, d * 0.4, d * 0.4) * 0.7)
        add(whoosh, 0, 0.8)
        return out
    if nm.startswith("glider_loop"):
        c.looping = True
        L = sfx.dark_noise(N, sr, rng, 2500, 2500, hp=60)
        R = sfx.dark_noise(N, sr, rng, 2500, 2500, hp=60)
        sw = (0.55 + 0.45 * np.sin(np.pi * np.linspace(0.0, 1.0, N)) ** 0.5)
        L = L * (0.7 + 0.3 * sfx.rand_mod(N, sr, rng, 0.8, 1.0)) * sw
        R = R * (0.7 + 0.3 * sfx.rand_mod(N, sr, rng, 0.8, 1.0)) * sw
        c.stereo = (L, R)
        return (L + R) * 0.5
    if nm == "rc_heatray_work":
        c.looping = True
        add(sfx.engine(N, sr, rng, f0=50, rough=0.3, nh=16), 0, 0.8)
        add(sfx.band_noise(N, sr, rng, 1800, 9000) * sfx.rand_mod(N, sr, rng, 6, 0.5), 0, 0.5)
        add(np.sin(sfx.osc_phase(np.full(N, 330.0) * (1 + 0.01 * np.sin(TWO_PI * 0.5 * t)), sr)), 0, 0.25)
        return out
    if nm == "rc_beep":
        n = sfx.n_of(sr, d)
        add(sfx.beep(sr, 540, d, attack=0.002, release=0.01))
        add(sfx.click(sr, rng, 0.7), 0, 0.5)
        add(sfx.thump_sub(sr, 120, 0.03, 0.01), 0, 0.5)
        return out
    if nm == "rc_warning":
        for i in range(3):
            add(sfx.beep(sr, 880 if i % 2 == 0 else 660, 0.3, attack=0.004, release=0.03, harm=0.5), i * 0.42, 1.0)
        return out
    if nm.startswith("antigrav_start"):
        n = sfx.n_of(sr, d)
        wh = sfx.dark_noise(n, sr, rng, 400, 6000, hp=60) * sfx.smooth_env(n, sr, 0.5, d * 0.5)
        add(wh, 0, 1.0)
        sw = sfx.sweep(60, 180, n, sr, k=d * 0.5)
        add(np.tanh(2.5 * np.sin(sw)) * sfx.smooth_env(n, sr, 0.3, d * 0.6), 0, 0.9)
        add(sfx.fm_zap(sr, rng, d * 0.8, 200, 900, ratio=1.0, index=2.0, tau=d * 0.5, ring=0.3), 0.1, 0.35)
        return out
    if nm == "nightvision_on":
        add(sfx.crack(sr, rng, 0.01, 0.8, 0.1), 0.0, 0.9)
        add(sfx.pulse_train(sfx.n_of(sr, 0.15), sr, rng, 150, tau=0.002, bp=(3000, 11000)), 0.0, 1.0)
        add(sfx.crack(sr, rng, 0.012, 0.8, 0.1), 0.12, 0.9)
        n = sfx.n_of(sr, 1.3)
        ph = sfx.sweep(5200, 4300, n, sr, k=0.7)
        add(np.sin(ph) * sfx.exp_env(n, sr, 0.45, attack=0.01) * (0.8 + 0.2 * np.sin(TWO_PI * 11 * sfx.tvec(n, sr))), 0.2, 0.35)
        return out
    if nm == "nightvision_off":
        add(sfx.crack(sr, rng, 0.012, 0.9, 0.1), 0, 1.0)
        add(sfx.clack(sr, rng, 1800, 1.2, 0.03), 0, 0.7)
        n = sfx.n_of(sr, 0.3)
        ph = sfx.sweep(5200, 1800, n, sr, k=0.1)
        add(np.sin(ph) * sfx.exp_env(n, sr, 0.12), 0.05, 0.3)
        for i, tt in enumerate((0.28, 0.34, 0.39)):
            add(sfx.beep(sr, 3200 - i * 300, 0.025, attack=0.002, release=0.01), tt, 0.15)
        return out
    if nm == "minigun_start_a":
        n = sfx.n_of(sr, d)
        puls = (0.5 + 0.5 * np.sin(TWO_PI * 11 * sfx.tvec(n, sr) - 1.2)) ** 2
        add(sfx.harmonic_stack(np.linspace(34, 52, n), n, sr, nh=20, rng=rng) * puls * sfx.smooth_env(n, sr, 0.04, 0.1), 0, 1.0)
        add(sfx.band_noise(n, sr, rng, 150, 2500) * puls * sfx.smooth_env(n, sr, 0.04, 0.1), 0, 0.7)
        return out
    if nm.startswith("turret_beep"):
        k = int(nm[-2:])
        if k == 1:  # long chatter over a steady whine
            n = sfx.n_of(sr, 2.05)
            add(np.sin(TWO_PI * 7300 * sfx.tvec(n, sr)), 0, 0.12)
            tt = 0.0
            while tt < 2.0:
                add(sfx.click(sr, rng, 1.2, 0.003), tt, rng.uniform(0.5, 1.0))
                tt += rng.uniform(0.018, 0.04)
            for j in range(5):
                add(sfx.beep(sr, rng.uniform(900, 1700), 0.08, attack=0.005, release=0.02, harm=0.4), 0.8 + j * 0.11, 0.5)
        else:
            segs = max(2, min(4, int(d / 0.1)))
            tt = 0.0
            per = d / segs
            for j in range(segs):
                f = rng.uniform(1800, 4200)
                n = sfx.n_of(sr, per * 0.75)
                ph = sfx.sweep(f * rng.uniform(.85, 1.15), f * rng.uniform(.85, 1.15), n, sr, k=per * 0.4)
                add(np.sin(ph) * sfx.smooth_env(n, sr, 0.004, 0.015), tt, 0.9)
                tt += per
        return out
    if nm.startswith("turret_gunequip"):
        k = int(nm[-2:])
        nclk = 3 + k
        tt = 0.0
        for j in range(nclk):
            add(sfx.clack(sr, rng, tau=0.04, dur=0.1), tt, rng.uniform(.5, 1.0))
            tt += rng.uniform(0.04, 0.09)
        add(sfx.thunk(sr, rng, 95, 0.08), min(d * 0.68, d - 0.25), 1.2)
        add(sfx.clack(sr, rng, 1100, 0.9, 0.06), min(d * 0.68, d - 0.25), 0.9)
        return out
    if nm == "flashbang_bang":
        add(sfx.crack(sr, rng, 0.01, 1.2, 0.15), 0, 1.3)
        add(sfx.thump_sub(sr, 70, 0.4, 0.12), 0, 1.0)
        n = sfx.n_of(sr, 0.55)
        ring = sfx.metal_ring(n, sr, rng, 4200, 0.25, nm=3) + 0.5 * np.sin(TWO_PI * 6800 * sfx.tvec(n, sr)) * sfx.exp_env(n, sr, 0.2)
        add(ring * sfx.exp_env(n, sr, 0.22), 0.01, 0.35)
        add(sfx.fftfilt(sfx.noise(n, rng), sr, hp=2500) * sfx.exp_env(n, sr, 0.12), 0, 0.6)
        return out
    # ----- npc voices
    if nm.startswith("cyberdemon"):
        return cyberdemon(c, N)
    if nm.startswith("alienbug"):
        return alienbug(c, N)
    if nm.startswith("apache"):
        return apache(c, N)
    if nm.startswith(("explosion1", "nukeexplosion")):
        return explosion(c, N, power=1.0, dur=d * 0.92, debris=0.8)
    return None


def cyberdemon(c, N):
    sr, rng, nm, d = c.sr, c.rng, c.name, c.dur
    out = np.zeros(N)

    def add(sig, at=0.0, g=1.0):
        sfx.place(out, sig, at, sr, g)

    if "step" in nm:
        add(sfx.thunk(sr, rng, rng.uniform(42, 58), 0.14, 0.45, grit=0.8), 0, 1.4)
        n = sfx.n_of(sr, 0.35)
        add(sfx.dark_noise(n, sr, rng, 2500, 200, hp=60) * sfx.exp_env(n, sr, 0.1), 0.06, 0.8)
        add(sfx.pulse_train(n, sr, rng, 60, tau=0.004, bp=(500, 4000)) * sfx.exp_env(n, sr, 0.12), 0.08, 0.7)
    elif "aggro" in nm:
        nb = 3
        per = d / (nb + 0.8)
        for i in range(nb):
            v = sfx.voice(sr, rng, per * 0.85, rng.uniform(52, 72), rng.uniform(38, 52), "a", "o", rasp=0.5, dist=0.7, shape="bump")
            add(v, i * per * 1.05, 1.0 - 0.1 * i)
        add(sfx.dark_noise(sfx.n_of(sr, d), sr, rng, 600, 150, hp=30) * sfx.exp_env(sfx.n_of(sr, d), sr, d * 0.4), 0, 0.5)
    elif "death" in nm:
        n = sfx.n_of(sr, d * 0.7)
        v = sfx.voice(sr, rng, d * 0.7, 95, 32, "a", "u", rasp=0.6, dist=0.8, shape="bump")
        add(v, 0, 1.0)
        add(sfx.dark_noise(sfx.n_of(sr, d), sr, rng, 500, 100, hp=25) * sfx.exp_env(sfx.n_of(sr, d), sr, d * 0.4), d * 0.25, 0.8)
        add(sfx.boom(sr, rng, 0.8, 45, 0.3), d * 0.55, 0.5)
    elif "hurt" in nm:
        v = sfx.voice(sr, rng, d * 0.7, rng.uniform(110, 150), rng.uniform(70, 90), "ae", "o", rasp=0.6, dist=0.6, shape="bump")
        add(v, 0, 1.0)
        n = sfx.n_of(sr, d * 0.5)
        add(sfx.band_noise(n, sr, rng, 500, 4000) * sfx.exp_env(n, sr, 0.25), d * 0.55, 0.2)
    else:  # idle
        h = d * 0.5
        add(sfx.voice(sr, rng, h, rng.uniform(55, 70), 48, "o", "a", rasp=0.6, dist=0.5, shape="bump"), 0, 0.7)
        add(sfx.voice(sr, rng, h * 1.0, 42, 35, "u", "o", rasp=0.4, dist=0.5, shape="bump"), h * 0.95, 1.0)
    return out


def alienbug(c, N):
    sr, rng, nm, d = c.sr, c.rng, c.name, c.dur
    out = np.zeros(N)

    def add(sig, at=0.0, g=1.0):
        sfx.place(out, sig, at, sr, g)

    def chitter(dur, rate=45, lo=1500, hi=8000):
        n = sfx.n_of(sr, dur)
        am = (0.5 + 0.5 * np.sin(TWO_PI * rate * sfx.tvec(n, sr) + rng.uniform(0, 6))) ** 2
        return sfx.band_noise(n, sr, rng, lo, hi) * am * sfx.rand_mod(n, sr, rng, 25, 0.6)

    def sparse_clicks(dur, rate, lo=2000):
        n = sfx.n_of(sr, dur)
        return sfx.pulse_train(n, sr, rng, rate, jitter=0.9, tau=0.003, bp=(lo, 10000), amp_var=0.8) * sfx.exp_env(n, sr, dur * 0.45)

    if "step" in nm:
        add(sfx.snap(sr, rng, 1500, 9000, 0.01), 0, 1.0)
        for i in range(int(rng.integers(3, 6))):
            add(sfx.click(sr, rng, rng.uniform(.8, 1.4)), rng.uniform(0.04, d * 0.6), rng.uniform(.3, .8))
        add(sfx.thunk(sr, rng, 150, 0.04, 0.12), 0, 0.5)
        add(sfx.dark_noise(sfx.n_of(sr, 0.3), sr, rng, 6000, 1500, hp=800) * sfx.exp_env(sfx.n_of(sr, 0.3), sr, 0.08), 0.02, 0.5)
    elif "bite" in nm:
        k = int(rng.integers(2, 4))
        for i in range(k):
            tt = 0.05 + i * rng.uniform(0.09, 0.14)
            add(sfx.snap(sr, rng, 500, 5000, 0.02, 0.12), tt, 0.9)
            add(sfx.thunk(sr, rng, rng.uniform(70, 110), 0.05, 0.2), tt, 0.9)
            add(sfx.click(sr, rng, 0.8), tt, 0.5)
        n = sfx.n_of(sr, d)
        add(sfx.dark_noise(n, sr, rng, 3000, 300, hp=100) * sfx.exp_env(n, sr, 0.18), 0.1, 0.4)
    elif "aggro" in nm:
        add(chitter(0.45, rate=rng.uniform(55, 80), lo=900, hi=7000), 0, 1.0)
        n = sfx.n_of(sr, 0.5)
        f = np.linspace(rng.uniform(1700, 2100), rng.uniform(2600, 3200), n) * (1 + 0.03 * sfx.rand_mod(n, sr, rng, 30, 1.0))
        scr = sfx.formant_filter(sfx.glottal(f, n, sr, rng, rasp=0.9), sr, [(2400, 400), (4500, 600)])
        add(scr * sfx.smooth_env(n, sr, 0.03, 0.2), 0.05, 0.6)
        add(sparse_clicks(d - 0.4, 40), 0.4, 0.9)
    elif "death" in nm:
        n = sfx.n_of(sr, 0.7)
        gur = sfx.voice(sr, rng, 0.7, 220, 80, "e", "o", rasp=0.8, dist=0.5, shape="decay") * (0.6 + 0.4 * np.sin(TWO_PI * 13 * sfx.tvec(n, sr)))
        add(gur, 0, 1.0)
        add(sfx.thunk(sr, rng, 60, 0.12, 0.4, 0.8), 0, 1.2)
        add(sparse_clicks(d - 0.5, 30, 1200), 0.5, 1.1)
        add(sfx.dark_noise(sfx.n_of(sr, d), sr, rng, 2000, 200, hp=80) * sfx.exp_env(sfx.n_of(sr, d), sr, d * 0.25), 0, 0.5)
    elif "hit" in nm:
        n = sfx.n_of(sr, 0.45)
        sq = sfx.voice(sr, rng, 0.45, rng.uniform(900, 1300), rng.uniform(350, 550), "ae", "e", rasp=0.9, dist=0.4, shape="decay")
        add(sq, 0, 1.0)
        add(chitter(0.3, 60, 800, 6000), 0, 0.6)
        add(sparse_clicks(d - 0.35, 35), 0.3, 0.9)
        add(sfx.thunk(sr, rng, 90, 0.05, 0.2), 0, 0.8)
    else:  # idle
        add(chitter(min(d * 0.8, 0.5), rate=rng.uniform(30, 70), lo=rng.uniform(1200, 3000), hi=9000) * sfx.smooth_env(sfx.n_of(sr, min(d * 0.8, 0.5)), sr, 0.08, 0.2), 0.0, 1.0)
        for i in range(int(rng.integers(2, 5))):
            add(sfx.click(sr, rng, rng.uniform(.8, 1.4)), rng.uniform(0.1, d * 0.8), rng.uniform(.4, .9))
    return out


def apache(c, N):
    sr, rng, nm, d = c.sr, c.rng, c.name, c.dur
    out = np.zeros(N)

    def add(sig, at=0.0, g=1.0):
        sfx.place(out, sig, at, sr, g)

    def chop(dur, rate, depth=0.9):
        n = sfx.n_of(sr, dur)
        ph = TWO_PI * rate * sfx.tvec(n, sr)
        am = (0.5 + 0.5 * np.cos(ph)) ** 3
        thumps = sfx.band_noise(n, sr, rng, 30, 220, order=2) * 3.0
        bed = sfx.band_noise(n, sr, rng, 200, 3500, order=1)
        return (thumps * am * depth + bed * 0.35 * (0.4 + 0.6 * am)), n

    if nm == "apache_rotor":
        c.looping = True
        sig, n = chop(N / sr, 11.5)
        add(sig)
        add(sfx.engine(N, sr, rng, f0=92, rough=0.4, nh=14), 0, 0.35)
    elif nm == "apache_burst":
        sig, n = chop(d, 14)
        env = np.minimum(sfx.tvec(n, sr) / 0.8, 1.0) ** 1.2 * np.where(sfx.tvec(n, sr) > 0.8, np.exp(-(sfx.tvec(n, sr) - 0.8) / 0.5), 1.0)
        add(sig * env, 0, 1.0)
        add(sfx.dark_noise(n, sr, rng, 1500, 150, hp=30) * env, 0, 0.8)
    elif nm == "apache_die":
        add(explosion(c, N, power=0.9, dur=d * 0.9, debris=0.9))
        for i in range(3):
            n = sfx.n_of(sr, 0.18)
            f = rng.uniform(180, 260)
            add(np.sign(np.sin(TWO_PI * f * sfx.tvec(n, sr))) * 0.3 * sfx.smooth_env(n, sr, 0.01, 0.06) +
                np.sin(TWO_PI * f * sfx.tvec(n, sr)) * 0.5 * sfx.smooth_env(n, sr, 0.01, 0.06), 0.25 + i * 0.3, 0.7)
        n = sfx.n_of(sr, d * 0.8)
        f = np.linspace(2200, 900, n) * (1 + 0.04 * np.sin(TWO_PI * 3.2 * sfx.tvec(n, sr)))
        add(np.sin(sfx.osc_phase(f, sr)) * sfx.exp_env(n, sr, d * 0.3, 0.2), 0.8, 0.12)
    elif nm == "apache_explode":
        add(explosion(c, N, power=1.2, dur=d * 0.95, debris=1.0))
        add(sfx.dark_noise(N, sr, rng, 400, 80, hp=25) * sfx.exp_env(N, sr, d * 0.3), 0, 0.8)
    else:  # hit_a / hit_b
        add(explosion(c, N, power=0.9, dur=d * 0.85, debris=1.2))
    return out


def gunshot(c, N):
    """Shot / blast built from the profile envelope: crack + falling body + boom + tail."""
    sr, rng, nm, d, prof = c.sr, c.rng, c.name, c.dur, c.prof
    out = np.zeros(N)
    d20 = prof.get("dec20") or 0.35
    d40 = prof.get("dec40") or d20 * 2.2
    cen = prof.get("centroid") or 1200.0
    sil = "silenced" in nm
    sc = c.scifi
    tau_b = max(0.04, d20 / 2.3) * rng.uniform(0.9, 1.1)
    n = min(N, sfx.n_of(sr, max(d40 * 1.3, 0.4)))
    f_hi = rng.uniform(5000, 9000) if not sil else 2500
    body = sfx.dark_noise(n, sr, rng, f_hi, rng.uniform(150, 300), hp=40) * sfx.exp_env(n, sr, tau_b, attack=0.002)
    out[:n] += body * 1.0
    # slower, darker tail that carries the profile's -40 dB point
    tau_t = max(0.1, d40 / 4.6)
    tail = sfx.dark_noise(n, sr, rng, 900, 120, hp=35) * sfx.exp_env(n, sr, tau_t, attack=0.02)
    out[:n] += tail * 0.45
    if not sil:
        k = 0.4 if sc else 1.0
        sfx.place(out, sfx.crack(sr, rng, tau=rng.uniform(0.006, 0.014), bright=rng.uniform(0.8, 1.1)), 0.0, sr, 1.5 * k)
    f0 = float(np.clip(48 + cen * 0.03, 52, 120)) * rng.uniform(0.92, 1.08)
    sfx.place(out, sfx.boom(sr, rng, dur=min(max(d20 * 2, 0.25), 1.2), f0=f0, tau=max(0.05, d20 * 0.45), grit=0.35),
              0.0, sr, 1.1 * (0.6 if sc else 1.0) * (0.5 if sil else 1.0))
    if sil:
        out = sfx.fftfilt(out, sr, lp=3500, order=2)
        sfx.place(out, sfx.snap(sr, rng, 800, 4000, 0.01), 0.0, sr, 0.5)
    if "shotgun" in nm or "sawedoff" in nm or nm.startswith("combatshotgun"):
        sfx.place(out, sfx.thunk(sr, rng, 60, 0.12, 0.4, 0.6), 0.0, sr, 0.8)
    return out


# =========================================================================== main synthesis
def synth(c):
    sr = c.sr
    prof = c.prof
    N = c.n + sfx.n_of(sr, 1.5)
    c.stereo = None
    base = c.name
    c.scifi = base.startswith(SCIFI_PREFIX) or base == "pulserifle_shot_stereo"
    fam = sndlib.lookup(base)
    if fam is not None:
        return synth_family(c, fam, N)
    return synth_legacy(c, N)


def synth_family(c, fam, N):
    """Round-1+ path: a per-family recipe, then contour/band conforming to the measured targets."""
    sr = c.sr
    sndlib.get_tg(c)
    x = np.asarray(fam(c, N))
    if x.ndim == 2:  # recipe supplied explicit stereo (L, R)
        c.stereo = (x[0], x[1])
        x = x.mean(axis=0)
    if len(x) < N:
        x = np.pad(x, (0, N - len(x)))
    if c.looping:
        c.loop_xf = min(0.25, c.dur * 0.2)
        L = c.n
        xfn = sfx.n_of(sr, c.loop_xf)
        seg = x[:L + xfn]
        if getattr(c, "conform_bands", True) and c.tg.bands:
            seg = sfx.conform_bands(seg, sr, c.tg.bands, strength=getattr(c, "band_strength", 1.0), rng=c.rng)
        x = sfx.make_loop(seg, sr, c.loop_xf)
        if c.stereo is not None:
            c.stereo = tuple(sfx.make_loop(s[:L + xfn], sr, c.loop_xf) for s in c.stereo)
    else:
        x = sndlib.conform(c, x)
        # where the original ends while still audible (cut, not faded) only de-click the end
        # a quiet tail (end_rel <= -42 dB) needs no long fade either: the conformed envelope already ends there
        end_rel = (c.tg.env10[-1] - max(c.tg.env10)) if c.tg.env10 else -99
        fout = 0.004 if end_rel > -42 else min(0.05, c.dur * 0.15)
        # a tail that is already quiet (last 10 ms at or under -50 dB of the peak) only needs a 4 ms de-click:
        # a long fade on top of it leaves the last frames silent
        n10 = max(1, sfx.n_of(sr, 0.010))
        if np.sqrt(np.mean(x[-n10:] ** 2)) <= 10 ** (-50 / 20) * (np.max(np.abs(x)) + 1e-12):
            fout = 0.004
        x = sfx.fade(x, sr, 0.0005, fout)
    if c.stereo is not None and c.e["channels"] == 2:
        L, R = c.stereo
        m = np.max(np.abs(np.concatenate([L, R]))) + 1e-9
        return np.stack([L / m, R / m])
    return x / (np.max(np.abs(x)) + 1e-9)


def synth_legacy(c, N):
    sr = c.sr
    prof = c.prof
    base = c.name
    named = recipe_named(c, N)
    if named is not None:
        x = named
    else:
        els = prof.get("elements") or []
        if not els:
            els = [(0.0, c.dur, c.e["brief"].split("Reads as:")[-1])]
        if re.search(r"_shot|_fire", base) and not base.startswith(("beamgun", "flamethrower", "minigun")):
            x = gunshot(c, N)
        else:
            x = interpret(c, els)
        if c.scifi:
            fl = scifi_layer(c, N)
            ref = np.max(np.abs(x)) + 1e-9
            x = x + fl / (np.max(np.abs(fl)) + 1e-9) * ref * 0.9
        if base.startswith("ammopress_work1"):
            c.looping = True
        # Reverb on explosions / gunshots whose profile mentions it
        txt = (prof.get("text") or "").lower()
        is_big = bool(re.search(r"_shot|explosion|nuke|impact|blast|fire", base)) or "explo" in base
        if is_big and re.search(r"reverb|echo|room|reflection", txt):
            rt = float(np.clip((prof.get("dec40") or 0.8) * 0.9, 0.25, 2.2))
            if "flashbang" not in base:
                x = sfx.reverb(x, sr, c.rng, rt60=rt, wet=0.16, bright=0.25)
    # tonal balance to the profile centroid
    cen = prof.get("centroid")
    if cen and named is None:
        x = sfx.match_centroid(x[:c.n + sfx.n_of(sr, 0.5)], sr, cen)
    elif cen and base.startswith(("cyberdemon", "alienbug", "apache", "chainsaw")):
        x = sfx.match_centroid(x, sr, cen, lo=-0.7, hi=0.7)
    x = np.asarray(x)[:N]
    if len(x) < N:
        x = np.pad(x, (0, N - len(x)))
    if c.looping:
        xf = min(0.25, c.dur * 0.2)
        L = c.n
        seg = x[:L + sfx.n_of(sr, xf)]
        if len(seg) < L + sfx.n_of(sr, xf):
            seg = np.pad(seg, (0, L + sfx.n_of(sr, xf) - len(seg)))
        x = sfx.make_loop(seg, sr, xf)
        if c.stereo is not None:
            ch = []
            for s in c.stereo:
                s = s[:L + sfx.n_of(sr, xf)]
                ch.append(sfx.make_loop(s, sr, xf))
            c.stereo = tuple(ch)
    else:
        x = x[:c.n]
        x = sfx.fade(x, sr, 0.001, min(0.05, c.dur * 0.15))
    if c.stereo is not None and c.e["channels"] == 2:
        L, R = c.stereo
        m = np.max(np.abs(np.concatenate([L, R]))) + 1e-9
        return np.stack([L / m, R / m])
    return x / (np.max(np.abs(x)) + 1e-9)


# =========================================================================== encode / measure
FF = ["ffmpeg", "-nostdin", "-hide_banner", "-loglevel", "error"]


def _encode_ogg_once(x, sr, ch, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    data = np.ascontiguousarray((x.T if ch == 2 else x).astype("<f4")).tobytes()
    cmd = FF + ["-y", "-f", "f32le", "-ar", str(sr), "-ac", str(ch), "-i", "-", "-map_metadata", "-1",
                "-fflags", "+bitexact", "-flags:a", "+bitexact", "-c:a", "libvorbis", "-q:a", "7", path]
    r = subprocess.run(cmd, input=data, capture_output=True)
    if r.returncode:
        raise RuntimeError(r.stderr.decode(errors="replace"))


def _decoded_len(path, ch):
    r = subprocess.run(FF + ["-i", path, "-f", "f32le", "-"], capture_output=True)
    if r.returncode:
        raise RuntimeError(r.stderr.decode(errors="replace"))
    return len(r.stdout) // (4 * ch)


def encode_ogg(x, sr, ch, path):
    """Encode to Ogg Vorbis. For some lengths the encoder's last packet(s) are not trimmed by the end granule,
    so the file decodes with up to ~1000 extra (silent or tail) samples (about 22 ms). Normal block rounding is
    -128..+127 samples. If the decoded length is outside that, retry with 1..255 zero samples appended, which
    moves the end onto a well-formed block boundary; the added silence is under 6 ms."""
    n = x.shape[-1]
    _encode_ogg_once(x, sr, ch, path)
    d = _decoded_len(path, ch)
    if -128 <= d - n <= 127:
        return
    for k in range(1, 256):
        xp = np.pad(x, [(0, 0)] * (x.ndim - 1) + [(0, k)])
        _encode_ogg_once(xp, sr, ch, path)
        d = _decoded_len(path, ch)
        if -128 <= d - (n + k) <= 127 and d >= n:
            return
    _encode_ogg_once(x, sr, ch, path)  # no good padding found: keep the unpadded encode


def measure(path, tries=3):
    """EBU R128 loudness + true peak + stream facts. ffmpeg/ffprobe occasionally fail transiently on Windows
    when many workers run at once, so retry instead of crashing the pool."""
    last = None
    for attempt in range(tries):
        try:
            r = subprocess.run(["ffmpeg", "-nostdin", "-hide_banner", "-nostats", "-i", path, "-af",
                                "ebur128=peak=true", "-f", "null", "-"], capture_output=True)
            s = r.stderr.decode(errors="replace")
            if r.returncode or "Summary:" not in s:
                raise RuntimeError("ffmpeg measure failed: " + s[-200:])
            s = s[s.rfind("Summary:"):]
            I = re.search(r"I:\s+(-?[\d.]+|-inf) LUFS", s)
            P = re.search(r"Peak:\s+(-?[\d.]+|-inf) dBFS", s)
            f = lambda m: (-70.0 if (not m or m.group(1) == "-inf") else float(m.group(1)))
            pr = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "stream=duration,channels,sample_rate",
                                 "-of", "json", path], capture_output=True)
            j = json.loads(pr.stdout.decode())["streams"][0]
            return {"lufs": f(I), "tp": f(P), "dur": float(j["duration"]), "ch": int(j["channels"]),
                    "sr": int(j["sample_rate"])}
        except (RuntimeError, ValueError, KeyError, IndexError) as ex:
            last = ex
            time.sleep(0.2 * (attempt + 1))
    raise last


def limit_tp(y, ceil, sr):
    """Apply the true-peak limiter per channel."""
    if y.ndim == 2:
        return np.stack([sfx.tp_limit(ch, ceil, sr) for ch in y])
    return sfx.tp_limit(y, ceil, sr)


def finalize(x, c, outpath):
    sr = c.sr
    ch = 2 if x.ndim == 2 else 1
    target = float(c.e["target_lufs"])
    note = ""
    short = target <= -60 or c.dur < 0.41
    if short:
        # integrated loudness is not measurable under 0.4 s: peak-normalize
        # the manifest loudness of these files is the -70 sentinel (original shorter than one R128 block), so use
        # the original's own measured peak instead of a fixed -3 dBFS (a quiet original stays quiet)
        gdb = -3.0
        tg = getattr(c, "tg", None)
        opk = tg.d.get("peak_dbfs") if tg is not None and getattr(tg, "d", None) else None
        if opk is not None and target <= -60:
            gdb = float(np.clip(opk, -30.0, -3.0))
        for _ in range(5):
            y = limit_tp(x * 10 ** (gdb / 20), 10 ** (-1.6 / 20), sr)
            encode_ogg(y, sr, ch, outpath)
            m = measure(outpath)
            if m["tp"] <= -1.0:
                break
            gdb -= (m["tp"] + 1.4)
        m["note"] = "too short for integrated LUFS (or -70 sentinel target): peak-normalized to the original's measured peak (at most -3 dBFS), true peak checked"
        m["target"] = target
        m["limited"] = False
        return m
    # target above what -1 dBTP allows is clamped (heavily clipped originals)
    eff = min(target, -3.5)
    if eff != target:
        note = "target %.1f LUFS is unreachable under -1 dBTP; aimed at %.1f" % (target, eff)
    mono = x if ch == 1 else x.mean(axis=0)
    est = sfx.lufs_estimate(mono, sr)
    g = 10 ** ((eff - (est if est is not None else -20)) / 20)
    ceil = 10 ** (-1.6 / 20)  # pre-encode true-peak ceiling; Vorbis adds a little overshoot
    # never let the limiter take more than ~3.5 dB off the peak: a quieter file (still within the +-3 LU
    # tolerance) keeps the original's envelope better than a squashed one
    pk = float(np.max(np.abs(x))) + 1e-12
    g_cap = ceil * 10 ** (3.5 / 20) / pk

    def cap(gu):  # at most 3.5 dB of limiting, unless that would leave us more than 2 LU under target
        return min(gu, max(g_cap, gu * 10 ** (-2.0 / 20)))

    g = cap(g)
    best = None
    for it in range(8):
        y = limit_tp(x * g, ceil, sr)
        encode_ogg(y, sr, ch, outpath)
        m = measure(outpath)
        err = eff - m["lufs"]
        ok = abs(err) <= 0.5 and m["tp"] <= -1.0
        if best is None or (m["tp"] <= -1.0 and abs(err) < abs(best[1])):
            best = (g, err, ceil)
        if ok:
            break
        if m["tp"] > -1.0:
            ceil *= 10 ** ((-1.2 - m["tp"]) / 20)
            if abs(err) <= 0.5:
                continue
        g = cap(g * 10 ** (np.clip(err, -9, 9) * 0.9 / 20))
    # final pass uses the best (g, ceil) found
    if not ok:
        g, _, ceil = best if best else (g, 0, ceil)
        y = limit_tp(x * g, ceil, sr)
        encode_ogg(y, sr, ch, outpath)
        m = measure(outpath)
        # last-ditch peak safety
        guard = 0
        while m["tp"] > -1.0 and guard < 4:
            ceil *= 0.9
            y = limit_tp(x * g, ceil, sr)
            encode_ogg(y, sr, ch, outpath)
            m = measure(outpath)
            guard += 1
    m["note"] = note
    m["target"] = target
    m["limited"] = True
    return m


def work(args):
    entry, prof = args
    c = Ctx(entry, prof)
    outpath = os.path.join(OUT_ASSETS, entry["path"])
    x = synth(c)
    m = finalize(x, c, outpath)
    m["id"] = entry["id"]
    m["path"] = entry["path"]
    m["priority"] = entry["priority"]
    m["want_dur"] = entry["duration_s"]
    m["want_ch"] = entry["channels"]
    m["want_sr"] = entry["sample_rate"]
    m["loop"] = bool(c.looping)
    return m


# =========================================================================== sounds.json + credits + report
CATEGORY = {"guns": "player", "items": "player", "effects": "player", "blocks": "block", "machines": "block",
            "npcs": "hostile"}


def write_fragment(entries, profiles):
    ev = {}
    for e in entries:
        p = profiles.get(e["path"], {})
        event = p.get("event")
        if not event:
            if e["path"].endswith("flashbang_bang.ogg"):
                event = "guns.flashbangBang"
            else:
                event = e["brief"].split(". ")[0].split(" ")[0]
        sub = e["path"].split("/")[1]
        cat = CATEGORY.get(sub, "player")
        snd = re.sub(r"^sounds/|\.ogg$", "", e["path"])
        item = {"name": snd, "stream": True} if e["duration_s"] > 5 else snd
        d = ev.setdefault(event, {"category": cat, "sounds": []})
        d["sounds"].append(item)
    ordered = {k: ev[k] for k in sorted(ev)}
    with open(FRAGMENT, "w", encoding="utf8", newline="\n") as f:
        json.dump(ordered, f, indent=2, sort_keys=False)
        f.write("\n")


def write_credits(entries):
    os.makedirs(os.path.dirname(CREDITS), exist_ok=True)
    revised = set()
    if os.path.exists(STATUS):
        st = json.load(open(STATUS, encoding="utf8"))
        revised = {p for p, v in st.items() if v.get("verdict") != "keep"}
    with open(CREDITS, "w", encoding="utf8", newline="") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["asset", "path", "generator", "seed", "method", "license", "date"])
        for e in entries:
            seed = zlib.crc32(e["id"].encode())
            r1 = e["path"] in revised
            w.writerow([e["id"], "src/main/resources/assets/gregguns/" + e["path"], GEN_NAME_R1 if r1 else GEN_NAME, seed,
                        METHOD_R1 if r1 else METHOD_OLD, "MIT", "2026-10-18"])


def write_report(results):
    os.makedirs(os.path.dirname(REPORT), exist_ok=True)
    rows = sorted(results, key=lambda r: (r["priority"], r["path"]))
    bad = []
    lines = ["# Sound generation report", "",
             "Generated by `tools/asset-gen/gen_sounds.py`. Loudness is EBU R128 integrated (ffmpeg `ebur128`), measured on the final `.ogg`.",
             "Tolerance: +/-3 LU of target, true peak <= -1 dBFS, duration within 30 ms, channel count and sample rate as in the manifest.", ""]
    body = ["| Prio | Sound | Target LUFS | Measured | dTP (dBFS) | Length (s) | Want (s) | Ch | Rate | Flag |",
            "|---|---|---|---|---|---|---|---|---|---|"]
    for r in rows:
        flags = []
        short = "too short" in (r.get("note") or "")
        if not short and abs(r["lufs"] - r["target"]) > 3.0:
            flags.append("LUFS off by %.1f" % (r["lufs"] - r["target"]))
        if r["tp"] > -1.0:
            flags.append("peak > -1")
        if abs(r["dur"] - r["want_dur"]) > 0.03:
            flags.append("length")
        if r["ch"] != r["want_ch"]:
            flags.append("channels")
        if r["sr"] != r["want_sr"]:
            flags.append("rate")
        if r.get("note"):
            flags.append("note: " + r["note"])
        if r.get("loop"):
            flags.append("seamless loop")
        hard = [f for f in flags if not f.startswith(("note", "seamless"))]
        if hard:
            bad.append(r["id"])
        body.append("| %s | `%s` | %.1f | %.1f | %.1f | %.2f | %.2f | %d | %d | %s |" % (
            r["priority"], r["path"].replace("sounds/", ""), r["target"], r["lufs"], r["tp"], r["dur"], r["want_dur"],
            r["ch"], r["sr"], "; ".join(flags)))
    lines.append("Sounds: %d. Outside tolerance: %d." % (len(rows), len(bad)))
    if bad:
        lines.append("")
        lines.append("Flagged: " + ", ".join("`%s`" % b for b in bad))
    lines.append("")
    lines += body
    with open(REPORT, "w", encoding="utf8", newline="\n") as f:
        f.write("\n".join(lines) + "\n")
    return bad


def main():
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", default="")
    ap.add_argument("--prio", default="P1,P2,P3")
    ap.add_argument("--jobs", type=int, default=max(1, (os.cpu_count() or 2) - 1))
    ap.add_argument("--no-write", action="store_true", help="skip fragment/credits/report (partial runs)")
    ap.add_argument("--only-file", default="", help="text file with one sound path (sounds/...ogg) per line; only those are rendered")
    ap.add_argument("--non-keep", action="store_true",
                    help="render only sounds that sound-status.json lists with a verdict other than keep")
    a = ap.parse_args()
    man = [e for e in json.load(open(MANIFEST, encoding="utf8")) if e["kind"] == "sound"]
    profiles = parse_profiles()
    prios = set(a.prio.split(","))
    todo = [e for e in man if e["priority"] in prios and a.only in e["id"]]
    if a.only_file:
        want = {ln.strip() for ln in open(a.only_file, encoding="utf8") if ln.strip()}
        todo = [e for e in todo if e["path"] in want]
    if a.non_keep:
        st = json.load(open(STATUS, encoding="utf8"))
        # keep sounds (and sounds with no review status) stay byte-identical: never re-rendered
        todo = [e for e in todo if st.get(e["path"], {}).get("verdict") not in (None, "keep")]
    jobs = [(e, profiles.get(e["path"])) for e in todo]
    os.makedirs(SCRATCH, exist_ok=True)
    stats = {}
    if os.path.exists(STATS):
        stats = json.load(open(STATS))
    with Pool(a.jobs) as pool:
        for i, m in enumerate(pool.imap_unordered(work, jobs, chunksize=1)):
            stats[m["id"]] = m
            print("%3d/%d %-40s %6.1f (t %6.1f) tp %5.1f %s" % (i + 1, len(jobs), m["id"][4:], m["lufs"], m["target"], m["tp"], m.get("note", "")[:50]), flush=True)
    os.makedirs(os.path.dirname(STATS), exist_ok=True)
    json.dump(stats, open(STATS, "w"), indent=1)
    if not a.no_write:
        write_fragment(man, profiles)
        write_credits(man)
        bad = write_report([stats[e["id"]] for e in man if e["id"] in stats])
        print("flagged:", bad)


if __name__ == "__main__":
    main()
