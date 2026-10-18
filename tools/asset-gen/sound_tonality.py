"""Flag spurious tonal lines (pings, beeps, sine whines, tinkle) in generated sounds.

The owner rejected reloads that "sound like tinkling noise with beeps or tones". This gate measures it. For each 20 ms
STFT frame between 1.5 and 16 kHz, it takes each bin's level against the median of its +/- 1/3-octave neighborhood (its
"prominence"). A line is a bin at >= THRESH_DB prominence for >= MIN_FRAMES consecutive frames. A generated sound fails
when it has a line that the reference sound (the Techguns original it replaces, measured only, never used for
synthesis) lacks within +/- 1/12 octave and at a comparable prominence. The tinkle rate (high clicks per second with no
body underneath, see tinkle()) is printed for information only: calibrated on the round-1 reloads the owner rejected,
the tonal lines separate good from bad and the tinkle rate does not.

Run:
  python -I -X utf8 tools/asset-gen/sound_tonality.py [--ref-assets <techguns assets dir>] [--only <file of paths>]
         [--match <substring>] [--json <out>]
Without --ref-assets every line in B counts as spurious.
"""

import argparse
import json
import os
import subprocess
import sys

import numpy as np

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "gregguns")
MANIFEST = os.path.join(ROOT, "tools", "asset-gen", "manifest.json")

SR = 48000
FRAME = 960  # 20 ms
FMIN, FMAX = 1500.0, 16000.0
THRESH_DB = 12.0
MIN_FRAMES = 2  # 40 ms: short pings count too
FLOOR_DB = -70.0  # ignore bins quieter than this (dBFS-ish), so silence can't make lines


def decode(path):
    """Decode to mono float32 at SR through ffmpeg (measurement only)."""
    raw = subprocess.run(["ffmpeg", "-v", "error", "-i", path, "-ac", "1", "-ar", str(SR), "-f", "f32le", "-"],
                         check=True, capture_output=True).stdout
    return np.frombuffer(raw, dtype=np.float32).astype(np.float64)


def lines(x):
    """Return [(freq_hz, start_s, frames, max_prominence_db)] for every persistent narrow line."""
    if len(x) < FRAME * MIN_FRAMES:
        return []
    n = len(x) // FRAME
    win = np.hanning(FRAME * 2)
    freqs = np.fft.rfftfreq(FRAME * 2, 1.0 / SR)
    band = np.where((freqs >= FMIN) & (freqs <= FMAX))[0]
    lo = np.searchsorted(freqs, freqs[band] / 2 ** (1 / 3))
    hi = np.searchsorted(freqs, freqs[band] * 2 ** (1 / 3))
    prom = np.zeros((n, len(band)))
    pad = np.concatenate([x, np.zeros(FRAME * 2)])
    for i in range(n):
        seg = pad[i * FRAME:i * FRAME + FRAME * 2] * win
        mag = 20 * np.log10(np.abs(np.fft.rfft(seg)) / (FRAME / 2) + 1e-12)
        for j, b in enumerate(band):
            if mag[b] < FLOOR_DB:
                continue
            prom[i, j] = mag[b] - np.median(mag[lo[j]:hi[j]])
    hot = prom >= THRESH_DB
    found = []
    for j in range(len(band)):
        run = 0
        for i in range(n + 1):
            if i < n and hot[i, j]:
                run += 1
                continue
            if run >= MIN_FRAMES:
                s = i - run
                found.append((float(freqs[band[j]]), s * FRAME / SR, run, float(prom[s:i, j].max())))
            run = 0
    # merge adjacent bins of one line: keep the strongest per (start frame, 1/12-octave cell)
    found.sort(key=lambda t: -t[3])
    kept = []
    for f in found:
        if not any(abs(np.log2(f[0] / k[0])) < 1 / 12 and abs(f[1] - k[1]) < 0.06 for k in kept):
            kept.append(f)
    return sorted(kept, key=lambda t: (t[1], t[0]))


TICK_HOP = 240  # 5 ms


def tinkle(x):
    """Tinkle rate: high-frequency clicks per second that have no body underneath.

    In 5 ms hops, compare the 4-16 kHz band level with its running median (+10 dB = a click). A click counts as
    tinkle when the 150-1200 Hz body band does not rise by at least 6 dB within +/- 10 ms. Real handling hits carry
    body; tinkle is all top."""
    n = len(x) // TICK_HOP
    if n < 8:
        return 0.0, 0
    win = np.hanning(TICK_HOP * 2)
    freqs = np.fft.rfftfreq(TICK_HOP * 2, 1.0 / SR)
    hi_b = (freqs >= 4000) & (freqs <= 16000)
    bo_b = (freqs >= 150) & (freqs <= 1200)
    pad = np.concatenate([x, np.zeros(TICK_HOP * 2)])
    hi = np.zeros(n)
    bo = np.zeros(n)
    for i in range(n):
        spec = np.abs(np.fft.rfft(pad[i * TICK_HOP:i * TICK_HOP + TICK_HOP * 2] * win)) ** 2
        hi[i] = 10 * np.log10(spec[hi_b].sum() + 1e-12)
        bo[i] = 10 * np.log10(spec[bo_b].sum() + 1e-12)
    k = 20  # 100 ms median window
    count = 0
    loud = hi.max() - 45  # ignore clicks far below the file's own peak
    for i in range(1, n - 1):
        if hi[i] < loud or hi[i] < hi[i - 1] or hi[i] < hi[i + 1]:
            continue
        lo_i, hi_i = max(0, i - k), min(n, i + k)
        if hi[i] - np.median(hi[lo_i:hi_i]) < 10:
            continue
        j0, j1 = max(0, i - 2), min(n, i + 3)
        body_rise = bo[j0:j1].max() - np.median(bo[lo_i:hi_i])
        if body_rise < 6:
            count += 1
    return count / (len(x) / SR), count


def spurious(b_lines, a_lines):
    """B lines with no A line within 1/6 octave, overlapping in time (+/- 0.15 s) and at most 6 dB weaker."""
    out = []
    for f, t, fr, p in b_lines:
        dur = fr * FRAME / SR
        ok = any(abs(np.log2(f / af)) <= 1 / 12 and at - 0.05 <= t <= at + afr * FRAME / SR + 0.05 and ap >= p - 3
                 for af, at, afr, ap in a_lines)
        if not ok:
            out.append({"hz": round(f), "t": round(t, 3), "dur": round(dur, 3), "prom_db": round(p, 1)})
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--assets", default=ASSETS, help="GregGuns assets dir to check (default: the repo's)")
    ap.add_argument("--ref-assets", default="", help="Techguns assets dir (read for measurement only)")
    ap.add_argument("--only", default="", help="file with one sounds/... path per line")
    ap.add_argument("--match", default="", help="only paths containing this substring")
    ap.add_argument("--json", default="")
    args = ap.parse_args()
    manifest = json.load(open(MANIFEST, encoding="utf-8"))
    entries = manifest if isinstance(manifest, list) else manifest.get("entries", manifest)
    ref_of = {}
    for e in entries if isinstance(entries, list) else []:
        p, r = e.get("path", ""), e.get("footprint_ref", "")
        if p.startswith("sounds/") and r:
            ref_of[p] = r
    paths = sorted(p for p in ref_of) if ref_of else []
    if args.only:
        wanted = {l.strip() for l in open(args.only, encoding="utf-8") if l.strip()}
        paths = [p for p in paths if p in wanted]
    if args.match:
        paths = [p for p in paths if args.match in p]
    report, fails = {}, 0
    for p in paths:
        b = lines(decode(os.path.join(args.assets, p)))
        a = []
        if args.ref_assets:
            ra = os.path.join(args.ref_assets, ref_of[p])
            if os.path.exists(ra):
                a = lines(decode(ra))
        bad = spurious(b, a)
        xb = decode(os.path.join(args.assets, p))
        tb, _ = tinkle(xb)
        ta = 0.0
        if args.ref_assets and os.path.exists(os.path.join(args.ref_assets, ref_of[p])):
            ta, _ = tinkle(decode(os.path.join(args.ref_assets, ref_of[p])))
        tinkly = tb > max(2.0, 1.5 * ta)
        report[p] = {"b_lines": len(b), "a_lines": len(a), "spurious": bad, "tinkle_b": round(tb, 2),
                     "tinkle_a": round(ta, 2), "tinkly": tinkly}
        fail = bool(bad)  # tinkle rate is informational: originals have bodiless high clicks too
        fails += fail
        worst = max((s["prom_db"] for s in bad), default=0.0)
        print(f"{'FAIL' if fail else 'ok  '} {p}  spurious={len(bad)} worst={worst:.1f} dB  "
              f"tinkle/s B={tb:.1f} A={ta:.1f}  (B lines {len(b)}, A lines {len(a)})")
    print(f"{fails} of {len(paths)} sounds have spurious tonal lines")
    if args.json:
        json.dump(report, open(args.json, "w", encoding="utf-8"), indent=1)
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main())
