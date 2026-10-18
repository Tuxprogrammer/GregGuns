"""Numeric A/B check of generated sounds against the measured targets of the originals.

    python -I -X utf8 tools/asset-gen/sound_compare.py [--only SUBSTR] [--file PATHS.txt] [-v]

Measures each generated .ogg with the same feature extractor the A/B review uses
(tools/audio-describe/audio_describe.py) and prints the differences from sound-targets.json.
A rough ``dist`` (lower is better) sorts the worst offenders first.  No original audio is read.
"""
import argparse
import importlib.util
import json
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "gregguns")


def load_ad():
    spec = importlib.util.spec_from_file_location("audio_describe", os.path.join(ROOT, "tools", "audio-describe", "audio_describe.py"))
    m = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(m)
    return m


def feats(ad, path):
    info = ad.probe(path)
    s = ad.decode(path, info["channels"])
    f = ad.measure(s, info["sample_rate"])
    f.update(ad.ebur128(path))
    f["duration_s"] = info["duration_s"]
    return f


def tenth_dist(a, b):
    if not a or not b:
        return 0.0
    a = np.array(a, dtype=float)
    b = np.array(b, dtype=float)
    a = np.maximum(a - a.max(), -60)
    b = np.maximum(b - b.max(), -60)
    return float(np.mean(np.abs(a - b)))


def dist(a, b):
    d = {}
    d["env"] = tenth_dist(a.get("envelope_db_by_tenth"), b.get("envelope_db_by_tenth"))
    d["cen"] = abs(np.log2(max(b["spectral_centroid_hz"], 50) / max(a["spectral_centroid_hz"], 50))) * 4
    ba, bb = a["band_energy_pct"], b["band_energy_pct"]
    d["band"] = sum(abs(ba.get(k, 0) - bb.get(k, 0)) for k in ba) / 10.0
    for k in ("decay_to_minus20db_s", "decay_to_minus40db_s"):
        x, y = a.get(k), b.get(k)
        if x and y:
            d[k[9:13]] = min(abs(np.log2(y / x)) * 3, 6)
        elif (x is None) != (y is None):
            d[k[9:13]] = 3
    d["trail"] = abs(a.get("trailing_silence_s", 0) - b.get("trailing_silence_s", 0)) * 4
    d["ons"] = abs(len(a.get("onset_times_s") or []) - len(b.get("onset_times_s") or [])) * 0.4
    d["lufs"] = abs((a.get("integrated_lufs") or 0) - (b.get("integrated_lufs") or 0)) / 2
    d["flat"] = abs(a.get("spectral_flatness", 0) - b.get("spectral_flatness", 0)) * 6
    return {k: float(v) for k, v in d.items()}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", default="")
    ap.add_argument("--file", default="")
    ap.add_argument("-v", action="store_true")
    ap.add_argument("--top", type=int, default=0)
    a = ap.parse_args()
    ad = load_ad()
    tg = json.load(open(os.path.join(HERE, "sound-targets.json"), encoding="utf8"))
    want = None
    if a.file:
        want = {ln.strip() for ln in open(a.file, encoding="utf8") if ln.strip()}
    rows = []
    for p in sorted(tg):
        if a.only not in p or (want is not None and p not in want):
            continue
        fp = os.path.join(ASSETS, p)
        if not os.path.exists(fp):
            continue
        b = feats(ad, fp)
        d = dist(tg[p], b)
        rows.append((sum(d.values()), p, tg[p], b, d))
    rows.sort(key=lambda r: -r[0])
    if a.top:
        rows = rows[:a.top]
    for tot, p, A, B, d in rows:
        print("%5.1f %-42s cen %5d/%5d  lufs %6.1f/%6.1f  d20 %s/%s d40 %s/%s  trail %.2f/%.2f  ons %d/%d  flat %.2f/%.2f" % (
            tot, p[7:-4], A["spectral_centroid_hz"], B["spectral_centroid_hz"], A["integrated_lufs"] or 0, B["integrated_lufs"] or 0,
            A.get("decay_to_minus20db_s"), B.get("decay_to_minus20db_s"), A.get("decay_to_minus40db_s"), B.get("decay_to_minus40db_s"),
            A.get("trailing_silence_s", 0), B.get("trailing_silence_s", 0), len(A.get("onset_times_s") or []),
            len(B.get("onset_times_s") or []), A.get("spectral_flatness", 0), B.get("spectral_flatness", 0)))
        if a.v:
            print("      bands A", [A["band_energy_pct"].get(k) for k in ("sub", "bass", "low_mid", "mid", "presence", "air")],
                  "B", [B["band_energy_pct"].get(k) for k in ("sub", "bass", "low_mid", "mid", "presence", "air")])
            print("      env A", [round(x) for x in A["envelope_db_by_tenth"]], "\n      env B", [round(x) for x in B["envelope_db_by_tenth"]])
            print("      onsets A", A.get("onset_times_s"), "B", B.get("onset_times_s"), " dist", {k: round(v, 1) for k, v in d.items()})
    if rows:
        print("mean dist %.2f over %d" % (np.mean([r[0] for r in rows]), len(rows)))
        comp = {}
        for r in rows:
            for k, v in r[4].items():
                comp.setdefault(k, []).append(v)
        print("by component:", {k: round(float(np.mean(v)), 2) for k, v in comp.items()})


if __name__ == "__main__":
    main()
