"""Fast Audio Flamingo Next early-layer distance: loads only the audio encoder, not the language model.

Same numbers as `ab_listen.py --backend af --modes embed` (dist_early_milli = 1000 x (1 - cosine) of the
mean-pooled layer-2 encoder states), but it starts in seconds and fits beside other jobs on a shared GPU, so the
sound revision agents can check a candidate after every edit. Thresholds and calibration: CALIBRATION.md.

A-side (reference) embeddings are cached in --cache, keyed by path, size and mtime.

Usage (on the GPU host, inside the AF venv):
    python af_embed_fast.py --pairs pairs.tsv --root . --out out.jsonl [--cache emb-cache] [--device cuda]
The TSV has `a<TAB>b` per line (an optional third `label` column is kept), paths relative to --root.
"""

import argparse
import hashlib
import json
import sys
import time
from pathlib import Path

EARLY_LAYER = 2  # matches ab_listen.EARLY_LAYER
EMBED_MAX_S = 30.0  # matches ab_listen.EMBED_MAX_S
MODEL_ID = "nvidia/audio-flamingo-next-hf"


def load_encoder(device, dtype_name):
    import torch
    from huggingface_hub import snapshot_download
    from safetensors import safe_open
    from transformers import AutoConfig, AutoFeatureExtractor, AutoModel

    snap = Path(snapshot_download(MODEL_ID, allow_patterns=["*.json", "*.safetensors", "*.jinja"]))
    cfg = AutoConfig.from_pretrained(snap)
    dtype = getattr(torch, dtype_name)
    enc = AutoModel.from_config(cfg.audio_config)
    state = {}
    with safe_open(str(snap / "model.safetensors"), "pt") as f:
        for k in f.keys():
            if k.startswith("audio_tower."):
                state[k[len("audio_tower."):]] = f.get_tensor(k)
    missing, unexpected = enc.load_state_dict(state, strict=False)
    if unexpected or [m for m in missing if "embed_positions" not in m]:
        sys.exit("encoder weights do not match: missing %s unexpected %s" % (missing[:5], unexpected[:5]))
    enc = enc.to(device, dtype).eval()
    fe = AutoFeatureExtractor.from_pretrained(snap)
    return enc, fe, torch


def embed(enc, fe, torch, wav):
    """Layer-2 encoder states mean-pooled over the valid frames, as ab_listen.AudioFlamingo.embed does."""
    wav = wav[: int(EMBED_MAX_S * fe.sampling_rate)]
    feats = fe([wav], sampling_rate=fe.sampling_rate, return_tensors="pt", return_attention_mask=True)
    p = next(enc.parameters())
    mask = feats["attention_mask"].to(p.device)
    x = feats["input_features"].to(p.device, p.dtype)
    with torch.no_grad():
        out = enc(x, input_features_mask=mask, output_hidden_states=True, return_dict=True)
    n_mel = int(mask.sum())
    h = out.hidden_states[EARLY_LAYER][0].float()
    valid = max(1, min(h.shape[0], (n_mel - 1) // 2 + 1))
    return h[:valid].mean(0).cpu().numpy()


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--pairs", required=True)
    ap.add_argument("--root", default=".")
    ap.add_argument("--out", required=True)
    ap.add_argument("--cache", default="emb-cache", help="folder for cached A-side embeddings")
    ap.add_argument("--device", default="cuda")
    ap.add_argument("--dtype", default="bfloat16", help="bfloat16 matches the calibration runs")
    args = ap.parse_args()

    import librosa
    import numpy as np

    t0 = time.time()
    enc, fe, torch = load_encoder(args.device, args.dtype)
    print("encoder loaded in %.1f s" % (time.time() - t0), flush=True)
    root, cache = Path(args.root), Path(args.cache)
    cache.mkdir(parents=True, exist_ok=True)

    def vec(rel, cached):
        p = root / rel
        st = p.stat()
        key = hashlib.sha1(("%s|%d|%d|%s" % (rel, st.st_size, st.st_mtime_ns, args.dtype)).encode()).hexdigest()
        cf = cache / (key + ".npy")
        if cached and cf.exists():
            return np.load(cf)
        wav, _ = librosa.load(str(p), sr=fe.sampling_rate, mono=True)
        v = embed(enc, fe, torch, wav)
        if cached:
            np.save(cf, v)
        return v

    rows = []
    for line in Path(args.pairs).read_text(encoding="utf-8").splitlines():
        parts = line.split("\t")
        if len(parts) < 2 or not parts[0].strip():
            continue
        a, b = parts[0].strip(), parts[1].strip()
        if not (root / a).exists() or not (root / b).exists():
            print("missing file for %s / %s, skipped" % (a, b), flush=True)
            continue
        va, vb = vec(a, True), vec(b, False)
        cos = float(np.dot(va, vb) / (np.linalg.norm(va) * np.linalg.norm(vb) + 1e-12))
        row = {"a": a, "b": b, "label": parts[2].strip() if len(parts) > 2 else "", "cos_early": cos,
               "dist_early_milli": 1000 * (1 - cos), "model": MODEL_ID + "#encoder"}
        rows.append(row)
        print("%6.2f  %s" % (row["dist_early_milli"], b), flush=True)
    Path(args.out).write_text("".join(json.dumps(r) + "\n" for r in rows), encoding="utf-8")
    print("%d pairs in %.1f s" % (len(rows), time.time() - t0), flush=True)


if __name__ == "__main__":
    main()
