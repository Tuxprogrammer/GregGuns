"""Objective A/B gate for generated sounds: compare each GregGuns sound (B) with the Techguns sound it replaces (A) on
measured features, and give a verdict without any model listening or API call.

Why: audio language models proved unable to compare two clips (they rated an original against itself 8/10 and an
explosion against a reload 7/10), and the owner asked to stop spending Batch API tokens on the review notes. The
features come from `tools/audio-describe/audio_describe.py extract` (both sides), plus `sound_tonality.py`.

  python -I -X utf8 tools/asset-gen/sound_gate.py --ref-work <A extract dir> --new-work <B extract dir>
         [--only <file of sounds/... paths>] [--tonality <sound_tonality.py --json output>]
         [--json out.json] [--report out.md]

Verdict: keep when every check passes (the owner's spot check can still overrule it), tweak when one or two fail,
rework when three or more fail or the tonality check fails. The report lists each failing check with A's and B's
numbers, which is what the revision agents work from.
"""
import argparse
import json
import math
import os
import subprocess

import numpy as np

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
MANIFEST = os.path.join(ROOT, "tools", "asset-gen", "manifest.json")
BANDS = ("sub", "bass", "low_mid", "mid", "presence", "air")
# Audio Flamingo Next layer-2 embedding distance, 1000 * (1 - cosine), calibrated in tools/audio-describe/CALIBRATION.md:
# the owner-approved shots reach 1.71 at most and the owner-rejected reloads start at 1.76; mismatched sounds sit near 11.
EMBED_PASS = 1.75
EMBED_DIFFERENT = 5.5


SR = 32000  # common analysis rate: most Techguns files are 32 kHz or lower, so compare only what both sides have


def band_flatness(path, lo=200.0, hi=8000.0):
    """Spectral flatness inside lo-hi Hz, median over active 32 ms frames (within 40 dB of the loudest frame).

    The extract's whole-band flatness reads 0.000 for most Techguns files, because their low sample rates and
    encoder low-passes leave the top bins empty and zero the geometric mean. Measuring both sides in the same band,
    at the same rate, makes the numbers comparable."""
    raw = subprocess.run(["ffmpeg", "-v", "error", "-i", path, "-ac", "1", "-ar", str(SR), "-f", "f32le", "-"],
                         check=True, capture_output=True).stdout
    x = np.frombuffer(raw, dtype=np.float32).astype(np.float64)
    n = 1024
    if len(x) < n:
        return None
    win = np.hanning(n)
    freqs = np.fft.rfftfreq(n, 1.0 / SR)
    band = (freqs >= lo) & (freqs <= hi)
    vals, energies = [], []
    for i in range(0, len(x) - n, n // 2):
        p = np.abs(np.fft.rfft(x[i:i + n] * win)) ** 2
        pb = p[band] + 1e-20
        energies.append(10 * np.log10(pb.sum()))
        vals.append(float(np.exp(np.mean(np.log(pb))) / np.mean(pb)))
    e = np.array(energies)
    keep = e >= e.max() - 40
    return float(np.median(np.array(vals)[keep])) if keep.any() else None


def load_features(work):
    out = {}
    with open(os.path.join(work, "manifest.jsonl"), encoding="utf-8") as fh:
        for line in fh:
            r = json.loads(line)
            if r.get("features"):
                f = dict(r["features"])
                f["duration_s"] = r.get("probe", {}).get("duration_s")
                out[r["path"]] = f
    return out


def corr(a, b):
    n = min(len(a), len(b))
    if n < 3:
        return 1.0
    a, b = [max(x, -60.0) for x in a[:n]], [max(x, -60.0) for x in b[:n]]
    ma, mb = sum(a) / n, sum(b) / n
    va = sum((x - ma) ** 2 for x in a)
    vb = sum((y - mb) ** 2 for y in b)
    if va < 1e-9 or vb < 1e-9:
        return 1.0 if abs(ma - mb) < 3 else 0.0
    return sum((x - ma) * (y - mb) for x, y in zip(a, b)) / math.sqrt(va * vb)


def checks(a, b, target_lufs):
    """Return [(name, passed, detail)] comparing B with A."""
    out = []

    def add(name, ok, detail):
        out.append((name, bool(ok), detail))

    if target_lufs is not None and b.get("integrated_lufs") is not None and b["integrated_lufs"] > -60:
        d = b["integrated_lufs"] - target_lufs
        add("loudness", abs(d) <= 3.0, "B %.1f LUFS vs target %.1f (%+.1f LU, limit 3)" % (b["integrated_lufs"],
                                                                                        target_lufs, d))
    if b.get("true_peak_dbtp") is not None:
        add("true_peak", b["true_peak_dbtp"] <= -0.9, "B %.1f dBTP (limit -1)" % b["true_peak_dbtp"])
    da, db = a.get("duration_s") or 0, b.get("duration_s") or 0
    if da:
        add("length", abs(db - da) <= max(0.1, 0.1 * da), "B %.2f s vs A %.2f s" % (db, da))
    ea, eb = a.get("envelope_db_by_tenth") or [], b.get("envelope_db_by_tenth") or []
    if len(ea) >= 3:
        c = corr(ea, eb)
        n = min(len(ea), len(eb))
        mad = sum(abs(max(x, -60) - max(y, -60)) for x, y in zip(ea[:n], eb[:n])) / max(n, 1)
        add("envelope", c >= 0.8 and mad <= 6.0, "tenth-second contour r=%.2f (min 0.80), mean |dB| diff %.1f (max 6)"
            % (c, mad))
    na, nb = a.get("onset_count") or 0, b.get("onset_count") or 0
    add("onsets", abs(nb - na) <= max(2, round(0.2 * na)), "B %d onsets vs A %d" % (nb, na))
    ba, bb = a.get("band_energy_pct") or {}, b.get("band_energy_pct") or {}
    if ba and bb:
        l1 = sum(abs(ba.get(k, 0) - bb.get(k, 0)) for k in BANDS)
        worst = max(BANDS, key=lambda k: abs(ba.get(k, 0) - bb.get(k, 0)))
        add("band_balance", l1 <= 25.0, "band-energy distance %.0f points (max 25); biggest gap %s: B %.0f%% vs A %.0f%%"
            % (l1, worst, bb.get(worst, 0), ba.get(worst, 0)))
    ca, cb = a.get("spectral_centroid_hz") or 0, b.get("spectral_centroid_hz") or 0
    if ca and cb:
        r = math.log2(cb / ca)
        add("brightness", abs(r) <= 0.35, "centroid B %d Hz vs A %d Hz (%+.0f%%)" % (cb, ca, (2 ** r - 1) * 100))
    fa, fb = a.get("band_flatness"), b.get("band_flatness")
    if fa is not None and fb is not None:
        r = math.log10(fb + 0.002) - math.log10(fa + 0.002)
        add("noisiness", abs(r) <= 0.4, "flatness in 200 Hz-8 kHz B %.3f vs A %.3f (higher = more noise-like, "
            "lower = more tonal or resonant)" % (fb, fa))
    ta, tb = a.get("decay_to_minus20db_s"), b.get("decay_to_minus20db_s")
    if ta and tb and ta > 0.02:
        add("decay", 0.5 <= tb / ta <= 2.0, "decay to -20 dB B %.2f s vs A %.2f s" % (tb, ta))
    return out


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--ref-work", required=True)
    ap.add_argument("--new-work", required=True)
    ap.add_argument("--assets", default=os.path.join(ROOT, "src", "main", "resources", "assets", "gregguns"))
    ap.add_argument("--ref-assets", required=True, help="Techguns assets dir (read for measurement only)")
    ap.add_argument("--only", default="")
    ap.add_argument("--tonality", default="", help="JSON written by sound_tonality.py --json")
    ap.add_argument("--embed", default="", help="JSONL from tools/audio-describe/ab_listen.py --modes embed (Audio "
                    "Flamingo Next early-layer distance, see tools/audio-describe/CALIBRATION.md)")
    ap.add_argument("--json", default="")
    ap.add_argument("--report", default="")
    args = ap.parse_args()

    manifest = json.load(open(MANIFEST, encoding="utf-8"))
    entries = {e["path"]: e for e in manifest if str(e.get("path", "")).startswith("sounds/")}
    fa, fb = load_features(args.ref_work), load_features(args.new_work)
    tonal = json.load(open(args.tonality, encoding="utf-8")) if args.tonality else {}
    embed = {}
    if args.embed:
        for line in open(args.embed, encoding="utf-8"):
            r = json.loads(line)
            if r.get("dist_early_milli") is not None:
                embed[r["b"][2:] if r["b"].startswith("b/") else r["b"]] = r["dist_early_milli"]
    paths = sorted(p for p in fb if p in entries and entries[p].get("footprint_ref") in fa)
    if args.only:
        wanted = {l.strip() for l in open(args.only, encoding="utf-8") if l.strip()}
        paths = [p for p in paths if p in wanted]

    results, counts = {}, {"keep": 0, "tweak": 0, "rework": 0}
    for p in paths:
        e = entries[p]
        a_feat, b_feat = dict(fa[e["footprint_ref"]]), dict(fb[p])
        ra = os.path.join(args.ref_assets, e["footprint_ref"])
        a_feat["band_flatness"] = band_flatness(ra) if os.path.exists(ra) else None
        b_feat["band_flatness"] = band_flatness(os.path.join(args.assets, p))
        cs = checks(a_feat, b_feat, e.get("target_lufs"))
        if p in tonal:
            sp = tonal[p].get("spurious") or []
            cs.append(("tonality", not sp, "%d spurious tonal lines" % len(sp) if sp else "no spurious tonal lines"))
        if p in embed:
            d = embed[p]
            band = "close to A" if d <= EMBED_PASS else ("a different sound" if d >= EMBED_DIFFERENT else
                                                         "same kind of sound but off")
            cs.append(("embedding", d <= EMBED_PASS, "Audio Flamingo early-layer distance %.2f (pass <= %.2f; %s)"
                       % (d, EMBED_PASS, band)))
        fails = [c for c in cs if not c[1]]
        tonal_fail = any(c[0] == "tonality" for c in fails)
        verdict = "keep" if not fails else ("tweak" if len(fails) <= 2 and not tonal_fail else "rework")
        score = round(10.0 * (len(cs) - len(fails)) / max(len(cs), 1), 1)
        counts[verdict] += 1
        results[p] = {"verdict": verdict, "score": score, "ref": e["footprint_ref"],
                      "checks": [{"name": n, "pass": ok, "detail": d} for n, ok, d in cs]}

    print("%d sounds: keep %d, tweak %d, rework %d" % (len(paths), counts["keep"], counts["tweak"], counts["rework"]))
    if args.json:
        json.dump(results, open(args.json, "w", encoding="utf-8", newline="\n"), indent=1)
    if args.report:
        lines = ["# Sound gate report", "", "Measured A/B checks per sound (no model listening). Verdict: keep when all pass, "
                 "tweak with one or two failures, rework with three or more or any tonality failure.", "",
                 "%d sounds: keep %d, tweak %d, rework %d." % (len(paths), counts["keep"], counts["tweak"],
                                                              counts["rework"]), ""]
        for p in sorted(results, key=lambda q: (results[q]["score"], q)):
            r = results[p]
            lines.append("## `%s`: %s (%s/10)" % (p, r["verdict"], r["score"]))
            for c in r["checks"]:
                if not c["pass"]:
                    lines.append("- **%s:** %s" % (c["name"], c["detail"]))
            lines.append("")
        open(args.report, "w", encoding="utf-8", newline="\n").write("\n".join(lines))


if __name__ == "__main__":
    main()
