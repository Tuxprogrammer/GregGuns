"""Score a calibration run: per-group mean/range, ordering, and AUC separation per signal.

  python calibration_stats.py --jsonl af_cal.jsonl [--emb-dir af_emb] [--emb-jsonl af_emb.jsonl]

--jsonl is an ab_listen.py output (judge/caption/embed columns); --emb-dir is the folder written by
`ab_listen.py --save-emb` (one layers x dim .npy per clip) for the layer sweep, with raw and corpus-centered cosine.
"""
import argparse
import json
from pathlib import Path

import numpy as np

GROUPS = ["identical", "good_shots", "weak_shots", "rejected_static", "rejected_tinkly", "mismatched"]
ORDER = [g for g in GROUPS if g != "weak_shots"]  # weak_shots is supplementary (loop verdicts, not owner-heard)


def auc(pos, neg):
    """P(score of a random pos > score of a random neg), ties count half."""
    pos, neg = np.asarray(pos, float), np.asarray(neg, float)
    if not len(pos) or not len(neg):
        return float("nan")
    return float(np.mean([(p > n) + 0.5 * (p == n) for p in pos for n in neg]))


def group_table(rows, key):
    out = {}
    for g in GROUPS:
        v = [r[key] for r in rows if r["label"] == g and r.get(key) is not None]
        out[g] = (float(np.mean(v)), min(v), max(v)) if v else None
    return out


def summarize(rows, key, title):
    t = group_table(rows, key)
    print(f"\n{title} ({key})")
    for g in GROUPS:
        if t[g]:
            print(f"  {g:16s} mean {t[g][0]:8.4f}  range {t[g][1]:.4f} .. {t[g][2]:.4f}")
    sign = -1 if key.startswith("dist") else 1  # distances: smaller means closer, so flip for AUC and ordering
    good = [sign * r[key] for r in rows if r["label"] == "good_shots"]
    rej = [sign * r[key] for r in rows if r["label"].startswith("rejected")]
    mm = [sign * r[key] for r in rows if r["label"] == "mismatched"]
    ident = [sign * r[key] for r in rows if r["label"] == "identical"]
    print(f"  AUC good>rejected {auc(good, rej):.2f} | good>mismatched {auc(good, mm):.2f} | "
          f"rejected>mismatched {auc(rej, mm):.2f} | identical>good {auc(ident, good):.2f}")
    weak = [sign * r[key] for r in rows if r["label"] == "weak_shots" and r.get(key) is not None]
    if weak:
        print(f"  AUC good>weak_shots {auc(good, weak):.2f} | weak_shots>mismatched {auc(weak, mm):.2f}")
    means = [sign * t[g][0] for g in ORDER if t[g]]
    print("  ordering identical>good>rejected>mismatched:", all(a > b for a, b in zip(means, means[1:])))


def layer_sweep(rows, emb_dir):
    emb_dir = Path(emb_dir)
    clips = {}
    for r in rows:
        for p in (r["a"], r["b"]):
            f = emb_dir / (p.replace("/", "__") + ".npy")
            if f.exists():
                clips[p] = np.load(f)
    allv = np.stack(list(clips.values()))  # clips x layers x dim
    mean = allv.mean(0)
    std = allv.std(0) + 1e-6
    print("\nlayer sweep: AUC good>rejected (raw / centered / z-scored), AUC good>mismatched (centered)")
    best = None
    for L in range(allv.shape[1]):
        res = {}
        for name, f in (("raw", lambda v: v), ("cen", lambda v: v - mean[L]), ("z", lambda v: (v - mean[L]) / std[L])):
            sc = []
            for r in rows:
                u, v = f(clips[r["a"]][L]), f(clips[r["b"]][L])
                sc.append(dict(label=r["label"], s=float(u @ v / (np.linalg.norm(u) * np.linalg.norm(v) + 1e-12))))
            good = [x["s"] for x in sc if x["label"] == "good_shots"]
            rej = [x["s"] for x in sc if x["label"].startswith("rejected")]
            mm = [x["s"] for x in sc if x["label"] == "mismatched"]
            res[name] = (auc(good, rej), auc(good, mm), sc)
        print(f"  layer {L:2d}  raw {res['raw'][0]:.2f}  cen {res['cen'][0]:.2f}  z {res['z'][0]:.2f}"
              f"   good>mm raw {res['raw'][1]:.2f} cen {res['cen'][1]:.2f} z {res['z'][1]:.2f}")
        if best is None or res["z"][0] > best[0]:
            best = (res["z"][0], L)
    print("  best z-scored layer for good>rejected:", best)


def add_caption_similarity(rows):
    """Cheap text proxy for caption agreement: TF-IDF cosine between the two captions (fit on all captions)."""
    from sklearn.feature_extraction.text import TfidfVectorizer

    docs = [r["caption_a"] for r in rows] + [r["caption_b"] for r in rows]
    m = TfidfVectorizer(stop_words="english", ngram_range=(1, 2), sublinear_tf=True).fit_transform(docs)
    n = len(rows)
    for i, r in enumerate(rows):
        r["cap_tfidf"] = float(m[i].multiply(m[n + i]).sum())


def log_mel_cosine(rows, root):
    """Baseline with no model: cosine of the mean log-mel spectra of A and B."""
    import librosa

    cache = {}

    def vec(p):
        if p not in cache:
            y, sr = librosa.load(str(Path(root) / p), sr=16000, mono=True)
            cache[p] = np.log(librosa.feature.melspectrogram(y=y, sr=sr, n_mels=64) + 1e-6).mean(1)
        return cache[p]

    for r in rows:
        u, v = vec(r["a"]), vec(r["b"])
        r["base_logmel"] = float(u @ v / (np.linalg.norm(u) * np.linalg.norm(v) + 1e-12))


def layer_groups(rows, emb_dir, layers):
    """Raw mean-pooled cosine per group at chosen encoder layers."""
    emb_dir = Path(emb_dir)
    for L in layers:
        for r in rows:
            u = np.load(emb_dir / (r["a"].replace("/", "__") + ".npy"))[L]
            v = np.load(emb_dir / (r["b"].replace("/", "__") + ".npy"))[L]
            r[f"cos_L{L}"] = float(u @ v / (np.linalg.norm(u) * np.linalg.norm(v) + 1e-12))


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--jsonl", required=True)
    ap.add_argument("--emb-dir")
    ap.add_argument("--layers", default="2,4,6", help="encoder layers to tabulate (raw cosine)")
    ap.add_argument("--root", help="workspace root; enables the log-mel baseline")
    args = ap.parse_args()
    rows = [json.loads(line) for line in Path(args.jsonl).read_text(encoding="utf-8").splitlines() if line]
    for r in rows:
        if r.get("grade") is not None and r.get("grade_swapped") is not None:
            r["grade_mean"] = (r["grade"] + r["grade_swapped"]) / 2
    keys = [k for k in ("grade", "grade_swapped", "grade_mean", "cos_last", "cos_mid", "cos_early", "dist_early_milli") if any(k in r for r in rows)]
    if all("caption_a" in r for r in rows):
        add_caption_similarity(rows)
        keys.append("cap_tfidf")
    if args.root:
        log_mel_cosine(rows, args.root)
        keys.append("base_logmel")
    if args.emb_dir:
        layers = [int(x) for x in args.layers.split(",") if x]
        layer_groups(rows, args.emb_dir, layers)
        keys += [f"cos_L{L}" for L in layers]
    for key in keys:
        if any(key in r for r in rows):
            summarize(rows, key, "signal")
    if args.emb_dir:
        layer_sweep(rows, args.emb_dir)


if __name__ == "__main__":
    main()
