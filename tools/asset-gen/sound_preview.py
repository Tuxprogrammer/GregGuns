"""Render labelled spectrogram + waveform thumbnails of the generated sounds.

    python -I -X utf8 tools/asset-gen/sound_preview.py --out docs/assets/previews/sounds.png
    python -I -X utf8 tools/asset-gen/sound_preview.py --only pistol --cols 2 --cell 640x200 --out some.png
"""
import argparse
import json
import os
import subprocess
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "gregguns")


def decode(path):
    r = subprocess.run(["ffmpeg", "-nostdin", "-loglevel", "error", "-i", path, "-f", "f32le", "-ac", "1",
                        "-ar", "44100", "-"], capture_output=True)
    return np.frombuffer(r.stdout, dtype="<f4").astype(np.float64), 44100


def spectrogram(x, sr, w, h, fmax=16000.0):
    nfft = 1024
    hop = max(1, (len(x) - nfft) // max(w, 1))
    win = np.hanning(nfft)
    cols = []
    for i in range(w):
        s = i * hop
        seg = x[s:s + nfft]
        if len(seg) < nfft:
            seg = np.pad(seg, (0, nfft - len(seg)))
        cols.append(np.abs(np.fft.rfft(seg * win)))
    S = np.array(cols).T  # freq x time
    f = np.fft.rfftfreq(nfft, 1 / sr)
    # log-frequency rows from 40 Hz to fmax
    rows = np.geomspace(40, fmax, h)
    idx = np.clip(np.searchsorted(f, rows), 0, len(f) - 1)
    S = S[idx][::-1]
    db = 20 * np.log10(S + 1e-6)
    db = np.clip((db - (db.max() - 70)) / 70, 0, 1)
    # simple heat palette
    r = np.clip(db * 3 - 1, 0, 1)
    g = np.clip(db * 3 - 2, 0, 1)
    b = np.clip(db * 2.2, 0, 1) * (1 - np.clip(db * 3 - 2, 0, 1))
    img = np.stack([r, g, b], axis=-1)
    return (img * 255).astype(np.uint8)


def cell(path, label, w, h):
    x, sr = decode(path)
    wave_h = max(20, h // 4)
    spec = Image.fromarray(spectrogram(x, sr, w, h - wave_h - 12))
    im = Image.new("RGB", (w, h), (12, 12, 16))
    d = ImageDraw.Draw(im)
    d.text((2, 0), label, fill=(230, 230, 230))
    im.paste(spec, (0, 12))
    y0 = 12 + spec.height
    mid = y0 + wave_h // 2
    n = len(x)
    for i in range(w):
        seg = x[i * n // w:(i + 1) * n // w + 1]
        if len(seg):
            d.line([(i, mid - int(seg.max() * wave_h / 2)), (i, mid - int(seg.min() * wave_h / 2))], fill=(120, 200, 255))
    return im


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--only", default="")
    ap.add_argument("--cols", type=int, default=5)
    ap.add_argument("--cell", default="300x110")
    a = ap.parse_args()
    cw, ch = [int(v) for v in a.cell.split("x")]
    man = [e for e in json.load(open(os.path.join(HERE, "manifest.json"), encoding="utf8")) if e["kind"] == "sound"]
    man = [e for e in man if a.only in e["id"]]
    rows = (len(man) + a.cols - 1) // a.cols
    sheet = Image.new("RGB", (a.cols * (cw + 4), rows * (ch + 4)), (30, 30, 36))
    for i, e in enumerate(man):
        p = os.path.join(ASSETS, e["path"])
        if not os.path.exists(p):
            continue
        im = cell(p, "%s %.2fs" % (e["id"][4:], e["duration_s"]), cw, ch)
        sheet.paste(im, ((i % a.cols) * (cw + 4), (i // a.cols) * (ch + 4)))
    os.makedirs(os.path.dirname(os.path.abspath(a.out)), exist_ok=True)
    sheet.save(a.out)
    print(a.out, sheet.size)


if __name__ == "__main__":
    main()
