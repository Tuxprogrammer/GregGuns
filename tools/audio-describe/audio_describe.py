"""Describe a folder of game sound effects with Claude, for the GregGuns spec.

Claude cannot take audio input, so each sound is turned into evidence it can read:
  * measured acoustics (loudness, envelope, onsets, spectral balance, stereo image), computed with ffmpeg + numpy
  * a log-frequency spectrogram and a waveform image (rendered by ffmpeg), read through vision
Claude (Sonnet by default) turns that evidence into a structured "what it sounds like" profile.

Subcommands
  extract   measure every .ogg and render its images              (no API calls, no cost)
  describe  send each sound's evidence to Claude, resumable       (Message Batches API by default)
  report    write AUDIO-PROFILES.md and per-area summaries into the spec
  selftest  check the measurement code against synthetic signals

See README.md in this folder for usage.
"""
import argparse
import base64
import json
import os
import re
import subprocess
import sys
import time
from pathlib import Path

import numpy as np

DEFAULT_MODEL = "claude-sonnet-5-5"
ANALYSIS_BANDS = [("sub", 20, 60), ("bass", 60, 250), ("low_mid", 250, 1000), ("mid", 1000, 4000),
                  ("presence", 4000, 8000), ("air", 8000, 24000)]
# Owning spec area per Techguns sound folder (see docs/techguns-spec/CONVENTIONS.md and the area indexes).
AREA_BY_FOLDER = {"guns": "03-gun-visuals-audio", "effects": "04-projectiles-fx", "items": "06-items",
                  "blocks": "07-blocks", "machines": "08-machines-production", "npcs": "10-entities-npcs"}


# --------------------------------------------------------------------------------------------- measurement

def run(cmd):
    """Run a command, returning (stdout bytes, stderr text); raise with stderr on failure."""
    proc = subprocess.run(cmd, capture_output=True)
    if proc.returncode != 0:
        raise RuntimeError(f"{cmd[0]} failed: {proc.stderr.decode(errors='replace')[-800:]}")
    return proc.stdout, proc.stderr.decode(errors="replace")


def probe(path):
    out, _ = run(["ffprobe", "-v", "error", "-show_streams", "-show_format", "-of", "json", str(path)])
    info = json.loads(out)
    stream = next(s for s in info["streams"] if s["codec_type"] == "audio")
    return {"sample_rate": int(stream["sample_rate"]), "channels": int(stream["channels"]),
            "codec": stream.get("codec_name"), "duration_s": float(info["format"]["duration"]),
            "bitrate_kbps": round(int(info["format"].get("bit_rate", 0)) / 1000, 1)}


def decode(path, channels):
    """Decode to float32 samples shaped (frames, channels) at the file's native rate."""
    out, _ = run(["ffmpeg", "-v", "error", "-i", str(path), "-f", "f32le", "-acodec", "pcm_f32le", "-"])
    data = np.frombuffer(out, dtype=np.float32)
    return data.reshape(-1, channels) if channels > 1 else data.reshape(-1, 1)


def ebur128(path):
    """Integrated loudness (LUFS), loudness range (LU) and true peak (dBTP) per EBU R128."""
    _, err = run(["ffmpeg", "-nostats", "-hide_banner", "-i", str(path), "-filter_complex", "ebur128=peak=true",
                  "-f", "null", "-"])
    summary = err[err.rfind("Summary:"):]

    def grab(pattern):
        m = re.search(pattern, summary)
        return None if not m or "inf" in m.group(1) else float(m.group(1))
    return {"integrated_lufs": grab(r"I:\s+(-?[\d.]+|-inf) LUFS"), "loudness_range_lu": grab(r"LRA:\s+([\d.]+) LU"),
            "true_peak_dbtp": grab(r"Peak:\s+(-?[\d.]+|-inf) dBFS")}


def db(x, floor=1e-10):
    return 20.0 * np.log10(np.maximum(x, floor))


def measure(samples, rate):
    """Time- and frequency-domain features of a decoded sound. Pure function: easy to self-test."""
    mono = samples.mean(axis=1)
    # Anti-phase stereo cancels in a mono mix; analyse the left channel instead when the mix collapses.
    if samples.shape[1] > 1 and np.sqrt(np.mean(mono ** 2)) < 0.1 * np.sqrt(np.mean(samples[:, 0] ** 2)):
        mono = samples[:, 0].copy()
    n = len(mono)
    peak = float(np.max(np.abs(samples))) if n else 0.0
    rms = float(np.sqrt(np.mean(mono ** 2))) if n else 0.0
    feats = {"peak_dbfs": round(float(db(peak)), 1), "rms_dbfs": round(float(db(rms)), 1),
             "crest_factor_db": round(float(db(peak) - db(rms)), 1),
             "clipped_samples": int(np.sum(np.abs(samples) >= 0.999)),
             "dc_offset": round(float(np.mean(mono)), 5)}

    # Envelope: RMS in 10 ms frames.
    hop = max(1, int(rate * 0.010))
    frames = max(1, n // hop)
    env = np.sqrt(np.mean(mono[:frames * hop].reshape(frames, hop) ** 2, axis=1)) if n >= hop else np.array([rms])
    env_db = db(env)
    top = float(env_db.max())
    t = lambda i: round(i * hop / rate, 3)  # noqa: E731 - frame index -> seconds
    active = np.where(env_db > top - 40)[0]
    peak_i = int(env_db.argmax())
    start_i = int(active[0]) if len(active) else 0
    end_i = int(active[-1]) if len(active) else frames - 1

    def first_below(drop):
        after = np.where(env_db[peak_i:] < top - drop)[0]
        return t(int(after[0])) if len(after) else None
    # Onsets: a rise of >= 9 dB over the previous 50 ms, near the top of the dynamic range, 80 ms apart.
    onsets, last = [], -10_000
    for i in range(5, frames):
        if env_db[i] > top - 35 and env_db[i] - env_db[i - 5:i].min() >= 9 and i - last >= 8:
            onsets.append(t(i))
            last = i
    feats.update({
        "leading_silence_s": t(start_i), "trailing_silence_s": round(max(0.0, (frames - 1 - end_i) * hop / rate), 3),
        "attack_s": round(t(peak_i) - t(start_i), 3), "peak_time_s": t(peak_i),
        "decay_to_minus20db_s": first_below(20), "decay_to_minus40db_s": first_below(40),
        "active_fraction": round(float(np.mean(env_db > top - 30)), 2),
        "onset_times_s": onsets[:24], "onset_count": len(onsets),
        "envelope_db_by_tenth": [round(float(x), 1) for x in
                                 [env_db[int(f * (frames - 1) / 10)] for f in range(11)]],
    })

    # Spectrum: energy-weighted mean of 2048-point Hann frames.
    size, step = 2048, 512
    if n < size:
        mono_p = np.pad(mono, (0, size - n))
    else:
        mono_p = mono
    window = np.hanning(size)
    starts = range(0, max(1, len(mono_p) - size + 1), step)
    spec = np.array([np.abs(np.fft.rfft(mono_p[s:s + size] * window)) ** 2 for s in starts])
    freqs = np.fft.rfftfreq(size, 1.0 / rate)
    weights = spec.sum(axis=1)
    mean_spec = (spec * (weights / max(weights.sum(), 1e-20))[:, None]).sum(axis=0)
    total = max(float(mean_spec.sum()), 1e-20)
    cum = np.cumsum(mean_spec) / total
    centroid = float((freqs * mean_spec).sum() / total)
    flatness = float(np.exp(np.mean(np.log(mean_spec[1:] + 1e-20))) / (np.mean(mean_spec[1:]) + 1e-20))
    bands = {name: round(100 * float(mean_spec[(freqs >= lo) & (freqs < min(hi, rate / 2))].sum()) / total, 1)
             for name, lo, hi in ANALYSIS_BANDS if lo < rate / 2}
    # Prominent peaks of the mean spectrum (local maxima >= 10 dB above their +-1/3-octave neighbourhood median).
    sdb = 10 * np.log10(mean_spec + 1e-20)
    peaks = []
    for i in np.argsort(sdb)[::-1]:
        f = freqs[i]
        if f < 30 or any(abs(f - p) / p < 0.06 for p in peaks):
            continue
        hood = sdb[(freqs > f / 1.26) & (freqs < f * 1.26)]
        if sdb[i] - np.median(hood) >= 10:
            peaks.append(float(f))
        if len(peaks) == 3:
            break
    # Brightness over time: centroid per quarter of the active region (reveals sweeps).
    quarter = []
    a0, a1 = start_i * hop, max(start_i * hop + size, (end_i + 1) * hop)
    for q in range(4):
        seg = mono_p[a0 + (a1 - a0) * q // 4: a0 + (a1 - a0) * (q + 1) // 4]
        if len(seg) < 256:
            quarter.append(None)
            continue
        ps = np.abs(np.fft.rfft(seg * np.hanning(len(seg)))) ** 2
        fq = np.fft.rfftfreq(len(seg), 1.0 / rate)
        quarter.append(round(float((fq * ps).sum() / max(ps.sum(), 1e-20))))
    feats.update({"spectral_centroid_hz": round(centroid), "rolloff85_hz": round(float(freqs[min(len(freqs) - 1, int(np.searchsorted(cum, 0.85)))])),
                  "spectral_flatness": round(flatness, 3), "band_energy_pct": bands,
                  "prominent_peaks_hz": [round(p) for p in peaks], "centroid_by_quarter_hz": quarter})

    if samples.shape[1] == 2:
        left, right = samples[:, 0], samples[:, 1]
        corr = float(np.corrcoef(left, right)[0, 1]) if np.std(left) > 0 and np.std(right) > 0 else 1.0
        mid, side = (left + right) / 2, (left - right) / 2
        feats["stereo"] = {"lr_correlation": round(corr, 3),
                           "side_to_mid_db": round(float(db(np.sqrt(np.mean(side ** 2))) - db(np.sqrt(np.mean(mid ** 2)))), 1)}
    return feats


def render_images(path, out_base):
    spec_png, wave_png = Path(f"{out_base}.spec.png"), Path(f"{out_base}.wave.png")
    run(["ffmpeg", "-v", "error", "-y", "-i", str(path), "-lavfi",
         "showspectrumpic=s=960x360:legend=1:fscale=log:scale=log:color=intensity", str(spec_png)])
    run(["ffmpeg", "-v", "error", "-y", "-i", str(path), "-lavfi",
         "showwavespic=s=1100x220:split_channels=1:colors=0x4FC3F7|0xFFB74D", str(wave_png)])
    return spec_png, wave_png


def load_events(assets):
    """Map 'folder/name' (no extension) -> list of 'event [category]' from sounds.json."""
    sj = Path(assets) / "sounds.json"
    events = {}
    if sj.exists():
        for ev, spec in json.loads(sj.read_text(encoding="utf-8")).items():
            for s in spec.get("sounds", []):
                name = s if isinstance(s, str) else s.get("name")
                events.setdefault(name, []).append(f"{ev} [{spec.get('category', '')}]")
    return events


def cmd_extract(args):
    assets, work = Path(args.assets), Path(args.work)
    work.mkdir(parents=True, exist_ok=True)
    events = load_events(assets)
    files = sorted((assets / "sounds").rglob("*.ogg")) if (assets / "sounds").exists() else sorted(assets.rglob("*.ogg"))
    manifest = []
    for i, f in enumerate(files[: args.limit or None], 1):
        rel = f.relative_to(assets).as_posix()
        key = rel[len("sounds/"):-4] if rel.startswith("sounds/") else rel[:-4]
        base = work / rel[:-4].replace("/", "__")
        info = probe(f)
        feats = measure(decode(f, info["channels"]), info["sample_rate"])
        feats.update(ebur128(f))
        spec_png, wave_png = render_images(f, base)
        folder = key.split("/")[0]
        rec = {"id": f"snd-{i:04d}", "path": rel, "events": events.get(key, []),
               "area": AREA_BY_FOLDER.get(folder, "01-core"), "probe": info, "features": feats,
               "spectrogram": spec_png.name, "waveform": wave_png.name}
        manifest.append(rec)
        print(f"[{i}/{len(files)}] {rel}  {feats['integrated_lufs']} LUFS  peak {feats['peak_dbfs']} dBFS", flush=True)
    (work / "manifest.jsonl").write_text("\n".join(json.dumps(r) for r in manifest) + "\n", encoding="utf-8")
    print(f"wrote {len(manifest)} records to {work / 'manifest.jsonl'}")


# --------------------------------------------------------------------------------------------- description

SYSTEM_PROMPT = """You are a game-audio analyst writing reference notes for sound designers.
A Minecraft mod is being rebuilt with all-new sounds, and the designers need to know what each original sound is like so they can make an original replacement with the same role.

You cannot hear the audio. For each sound you get:
1. A spectrogram: time runs left to right, frequency runs bottom to top on a logarithmic axis, and brighter means louder. The legend shows the scales.
2. A waveform: amplitude over time, one lane per channel.
3. Measured features: loudness (dBFS/LUFS), the envelope (attack, decay, onsets, silence), spectral balance (centroid, flatness, energy per band, prominent peaks, centroid per quarter) and the stereo image.
4. Context: the file path and the sounds.json event and category that play it.
5. Sometimes, listening notes from an audio-language model (Qwen2-Audio) that actually heard the file. Treat them as a second witness. They capture timbre and recognisable sources well, but can invent detail. Where they agree with the spectrogram and measurements, use them confidently. Where they conflict, trust the measurements and say so.

Describe what a listener would hear, grounded in that evidence.
- Spectral flatness near 1 means noise-like; near 0 means tonal. Steady horizontal lines are tones or hums, and evenly spaced lines are harmonics. Vertical bright strokes are clicks and transients. A rising centroid is a pitch or brightness sweep upward.
- Read loudness from the measurements: peak, RMS and integrated LUFS.
- Use the file and event names only as context about the sound's role. Never contradict the evidence because of a name.
- When something is inferred rather than visible, say so and lower your confidence."""

PROFILE_SCHEMA = {
    "type": "object", "additionalProperties": False,
    "required": ["summary", "character_tags", "elements", "envelope", "frequency_profile", "pitch_movement",
                 "perceived_loudness", "stereo_image", "loopability", "likely_role", "recreation_notes", "confidence"],
    "properties": {
        "summary": {"type": "string", "description": "One sentence: what it sounds like."},
        "character_tags": {"type": "array", "items": {"type": "string"},
                           "description": "3-8 short tags, e.g. transient, noisy, tonal, metallic, electronic, mechanical, boomy, whoosh, hum, crackle."},
        "elements": {"type": "array", "description": "Distinct audible events in time order.",
                     "items": {"type": "object", "additionalProperties": False,
                               "required": ["start_s", "end_s", "description"],
                               "properties": {"start_s": {"type": "number"}, "end_s": {"type": "number"},
                                              "description": {"type": "string"}}}},
        "envelope": {"type": "string", "description": "Attack, sustain, decay and tail in words, with times."},
        "frequency_profile": {"type": "string", "description": "Where the energy sits and its tone or texture."},
        "pitch_movement": {"type": "string", "enum": ["none", "rising", "falling", "rise-fall", "fall-rise", "wobble", "stepped", "unclear"]},
        "perceived_loudness": {"type": "string", "enum": ["very quiet", "quiet", "moderate", "loud", "very loud"]},
        "stereo_image": {"type": "string", "description": "mono, narrow, wide, ping-pong, etc. and why it matters in game."},
        "loopability": {"type": "string", "enum": ["one-shot", "seamless loop", "loop with audible seam", "unclear"]},
        "likely_role": {"type": "string", "description": "What it represents in game, inferred from evidence plus context."},
        "recreation_notes": {"type": "string", "description": "Guidance for making an original replacement: layers, duration, loudness target."},
        "confidence": {"type": "string", "enum": ["low", "medium", "high"]},
    },
}


def b64(path):
    return base64.standard_b64encode(Path(path).read_bytes()).decode("ascii")


def load_listening(work):
    """Qwen2-Audio notes (listen.jsonl from listen_qwen.py), keyed by asset path, if that stage was run."""
    path = Path(work) / "listen.jsonl"
    if not path.exists():
        return {}
    return {r["path"]: r for r in (json.loads(line) for line in path.read_text(encoding="utf-8").splitlines() if line)}


def build_params(rec, work, model, effort, listening=None):
    evidence = {k: rec[k] for k in ("path", "events", "probe", "features")}
    heard = (listening or {}).get(rec["path"])
    if heard:
        evidence["listening_notes"] = {"model": heard["model"], "notes": heard["notes"]}
    content = [
        {"type": "text", "text": "Spectrogram:"},
        {"type": "image", "source": {"type": "base64", "media_type": "image/png", "data": b64(work / rec["spectrogram"])}},
        {"type": "text", "text": "Waveform:"},
        {"type": "image", "source": {"type": "base64", "media_type": "image/png", "data": b64(work / rec["waveform"])}},
        {"type": "text", "text": "Measured features and context (JSON):\n" + json.dumps(evidence, indent=1)},
    ]
    return {"model": model, "max_tokens": 8000, "system": SYSTEM_PROMPT,
            "output_config": {"effort": effort, "format": {"type": "json_schema", "schema": PROFILE_SCHEMA}},
            "messages": [{"role": "user", "content": content}]}


def parse_message(msg):
    """Return (profile dict | None, error str | None) from a Messages API response."""
    if msg.stop_reason == "refusal":
        return None, f"refusal: {getattr(msg.stop_details, 'category', None)}"
    if msg.stop_reason == "max_tokens":
        return None, "max_tokens reached"
    text = next((b.text for b in msg.content if b.type == "text"), None)
    try:
        return json.loads(text), None
    except (TypeError, json.JSONDecodeError) as exc:
        return None, f"unparseable output: {exc}"


def load_dotenv(path):
    """Set KEY=VALUE pairs from a .env file into os.environ, never overriding variables that are already set."""
    if not path.exists():
        return
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line and not line.startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            os.environ.setdefault(key.strip(), value.strip().strip('"').strip("'"))


def cmd_describe(args):
    import anthropic  # imported here so extract/report/selftest work without the SDK

    load_dotenv(Path(__file__).resolve().parents[2] / ".env")  # repo-root .env (git-ignored)

    work = Path(args.work)
    records = [json.loads(line) for line in (work / "manifest.jsonl").read_text(encoding="utf-8").splitlines() if line]
    out_path = work / "descriptions.jsonl"
    done = {}
    if out_path.exists():
        for line in out_path.read_text(encoding="utf-8").splitlines():
            r = json.loads(line)
            if r.get("profile"):
                done[r["id"]] = r
    todo = [r for r in records if r["id"] not in done][: args.limit or None]
    print(f"{len(done)} already described, {len(todo)} to go, model {args.model}")
    if not todo:
        return
    listening = load_listening(work)
    print(f"listening notes available for {len(listening)} sounds")
    client = anthropic.Anthropic()

    def save(rec, profile, error, usage=None):
        with out_path.open("a", encoding="utf-8") as f:
            f.write(json.dumps({"id": rec["id"], "path": rec["path"], "model": args.model, "profile": profile,
                                "error": error, "usage": usage}) + "\n")

    if args.sync:
        for rec in todo:
            try:
                msg = client.messages.create(**build_params(rec, work, args.model, args.effort, listening))
                profile, error = parse_message(msg)
                save(rec, profile, error, msg.usage.model_dump())
            except anthropic.APIStatusError as exc:
                save(rec, None, f"API {exc.status_code}: {exc.message}")
            print(f"{rec['path']}: {'ok' if profile else error}", flush=True)
        return

    from anthropic.types.message_create_params import MessageCreateParamsNonStreaming
    from anthropic.types.messages.batch_create_params import Request

    batch_file = work / "batch.json"
    if batch_file.exists() and not args.new_batch:
        batch_id = json.loads(batch_file.read_text())["id"]
        print(f"resuming batch {batch_id}")
    else:
        requests = [Request(custom_id=r["id"], params=MessageCreateParamsNonStreaming(**build_params(r, work, args.model, args.effort, listening)))
                    for r in todo]
        batch = client.messages.batches.create(requests=requests)
        batch_id = batch.id
        batch_file.write_text(json.dumps({"id": batch_id, "count": len(requests), "model": args.model}))
        print(f"submitted batch {batch_id} with {len(requests)} requests")
    while True:
        batch = client.messages.batches.retrieve(batch_id)
        c = batch.request_counts
        print(f"  {batch.processing_status}: processing {c.processing}, succeeded {c.succeeded}, errored {c.errored}", flush=True)
        if batch.processing_status == "ended":
            break
        time.sleep(args.poll)
    by_id = {r["id"]: r for r in records}
    for res in client.messages.batches.results(batch_id):
        rec = by_id[res.custom_id]
        if res.result.type == "succeeded":
            profile, error = parse_message(res.result.message)
            save(rec, profile, error, res.result.message.usage.model_dump())
        else:
            save(rec, None, f"batch result {res.result.type}")
    batch_file.unlink()
    print(f"results appended to {out_path}; re-run describe to retry any failures")


# --------------------------------------------------------------------------------------------- report

def fmt(v, unit=""):
    return "–" if v is None else f"{v}{unit}"


def cmd_report(args):
    work, spec = Path(args.work), Path(args.spec)
    records = [json.loads(line) for line in (work / "manifest.jsonl").read_text(encoding="utf-8").splitlines() if line]
    profiles = {}
    for line in (work / "descriptions.jsonl").read_text(encoding="utf-8").splitlines():
        r = json.loads(line)
        if r.get("profile"):
            profiles[r["id"]] = r
    by_area = {}
    for rec in records:
        by_area.setdefault(rec["area"], []).append(rec)

    lines = ["# Audio profiles", "",
             "What every Techguns sound is like, so that original GregGuns replacements can fill the same role.",
             "Generated by [`tools/audio-describe`](../../tools/audio-describe/README.md). Each sound was measured "
             "(loudness, envelope, spectrum, stereo), rendered as a spectrogram and a waveform, and described by "
             f"Claude (`{next(iter(profiles.values()))['model'] if profiles else DEFAULT_MODEL}`) from that evidence.",
             "", "> [!NOTE]", "> Claude can't take audio input. The descriptions are read from the spectrogram, the "
             "waveform and the measurements, not heard. The measured columns are exact; the prose is an informed "
             "reading, with a confidence level for each sound. **Peak** and **RMS** are dBFS; **LUFS** is EBU R128 "
             "integrated loudness. The *volume the code passes* is in each area's `sounds.md`.", "",
             f"Coverage: {len(profiles)} of {len(records)} sounds described.", ""]
    for area in sorted(by_area):
        recs = by_area[area]
        lines += [f"## {area}", "", "| Sound | Event | Sounds like | Peak | RMS | LUFS | Length | Attack | Decay to −20 dB |",
                  "|---|---|---|---|---|---|---|---|---|"]
        for rec in recs:
            ft, p = rec["features"], profiles.get(rec["id"], {}).get("profile")
            lines.append(f"| `{rec['path']}` | {', '.join(e.split(' [')[0] for e in rec['events']) or '–'} | "
                         f"{(p['summary'] if p else '*not yet described*').replace('|', '/')} | {ft['peak_dbfs']} | "
                         f"{ft['rms_dbfs']} | {fmt(ft['integrated_lufs'])} | {rec['probe']['duration_s']:.2f} s | "
                         f"{ft['attack_s']} s | {fmt(ft['decay_to_minus20db_s'], ' s')} |")
        lines.append("")
        for rec in recs:
            p = profiles.get(rec["id"], {}).get("profile")
            if not p:
                continue
            ft = rec["features"]
            lines += [f"### `{rec['path']}`", "",
                      f"**{p['summary']}** *({', '.join(p['character_tags'])}; confidence {p['confidence']})*", "",
                      f"- **Measured:** {rec['probe']['duration_s']:.2f} s, {rec['probe']['sample_rate']} Hz, "
                      f"{rec['probe']['channels']} ch; peak {ft['peak_dbfs']} dBFS, RMS {ft['rms_dbfs']} dBFS, "
                      f"{fmt(ft['integrated_lufs'], ' LUFS')}, true peak {fmt(ft['true_peak_dbtp'], ' dBTP')}; "
                      f"centroid {ft['spectral_centroid_hz']} Hz, flatness {ft['spectral_flatness']}; "
                      f"{ft['onset_count']} onset(s).",
                      f"- **Perceived loudness:** {p['perceived_loudness']}. **Stereo:** {p['stereo_image']}. "
                      f"**Pitch:** {p['pitch_movement']}. **Loop:** {p['loopability']}.",
                      f"- **Envelope:** {p['envelope']}",
                      f"- **Frequency profile:** {p['frequency_profile']}",
                      f"- **Role:** {p['likely_role']}"]
            if p["elements"]:
                lines.append("- **Elements:** " + "; ".join(f"{e['start_s']:.2f}–{e['end_s']:.2f} s {e['description']}"
                                                            for e in p["elements"]))
            lines += [f"- **Recreation notes:** {p['recreation_notes']}", ""]
    (spec / "AUDIO-PROFILES.md").write_text("\n".join(lines), encoding="utf-8", newline="\n")
    print(f"wrote {spec / 'AUDIO-PROFILES.md'}")

    # Per-area summary block in each area's sounds.md, replaced between markers so re-runs are idempotent.
    start, end = "<!-- audio-profiles:start -->", "<!-- audio-profiles:end -->"
    for area, recs in by_area.items():
        target = spec / area / "sounds.md"
        if not target.exists():
            continue
        block = [start, "## How each sound sounds", "",
                 "Generated from [AUDIO-PROFILES.md](../AUDIO-PROFILES.md), which has full profiles with elements, "
                 "envelope and recreation notes. Claude described these from spectrograms and measurements, so treat the "
                 "prose as an informed reading.", "",
                 "| Sound | Sounds like | Character | Loudness (LUFS / peak dBFS) | Confidence |", "|---|---|---|---|---|"]
        for rec in recs:
            p = profiles.get(rec["id"], {}).get("profile")
            ft = rec["features"]
            anchor = re.sub(r"[^\w\- ]", "", rec["path"].lower()).replace(" ", "-")
            block.append(f"| [`{rec['path']}`](../AUDIO-PROFILES.md#{anchor}) | "
                         f"{(p['summary'] if p else '*not yet described*').replace('|', '/')} | "
                         f"{', '.join(p['character_tags'][:4]) if p else '–'} | {fmt(ft['integrated_lufs'])} / {ft['peak_dbfs']} | "
                         f"{p['confidence'] if p else '–'} |")
        block.append(end)
        text = target.read_text(encoding="utf-8")
        new = "\n".join(block)
        text = (text[:text.index(start)] + new + text[text.index(end) + len(end):]) if start in text \
            else text.rstrip("\n") + "\n\n" + new + "\n"
        target.write_text(text, encoding="utf-8", newline="\n")
        print(f"updated {target}")


# --------------------------------------------------------------------------------------------- self-test

def cmd_selftest(_args):
    rate = 44100
    t = np.arange(int(rate * 1.0)) / rate
    tone = 0.5 * np.sin(2 * np.pi * 440 * t) * np.exp(-3 * t)          # decaying 440 Hz tone
    f = measure(tone[:, None].astype(np.float32), rate)
    assert abs(f["prominent_peaks_hz"][0] - 440) < 30, f["prominent_peaks_hz"]
    assert f["spectral_flatness"] < 0.1, f["spectral_flatness"]
    assert abs(f["peak_dbfs"] - (-6.0)) < 0.5, f["peak_dbfs"]
    assert f["attack_s"] <= 0.02 and f["decay_to_minus20db_s"] is not None
    noise = np.random.default_rng(1).uniform(-0.25, 0.25, rate).astype(np.float32)
    g = measure(np.stack([noise, -noise], axis=1), rate)                 # wide, noisy, anti-phase stereo
    assert g["spectral_flatness"] > 0.5 and g["stereo"]["lr_correlation"] < -0.99
    clicks = np.zeros(rate, np.float32)
    clicks[[4410, 13230, 22050]] = 0.9
    h = measure(np.convolve(clicks, np.exp(-np.arange(441) / 40.0))[:rate, None].astype(np.float32), rate)
    assert h["onset_count"] == 3, h["onset_times_s"]
    print("selftest passed")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    e = sub.add_parser("extract", help="measure sounds and render images (no API)")
    e.add_argument("--assets", required=True, help="assets/<modid> folder containing sounds/ and sounds.json")
    e.add_argument("--work", required=True, help="working folder for images and JSON (keep outside the repo)")
    e.add_argument("--limit", type=int, default=0)
    d = sub.add_parser("describe", help="describe extracted sounds with Claude")
    d.add_argument("--work", required=True)
    d.add_argument("--model", default=DEFAULT_MODEL)
    d.add_argument("--effort", default="medium", choices=["low", "medium", "high", "xhigh", "max"])
    d.add_argument("--sync", action="store_true", help="one request at a time instead of the Batches API")
    d.add_argument("--new-batch", action="store_true", help="ignore a pending batch.json and submit a new batch")
    d.add_argument("--poll", type=int, default=60, help="batch poll interval, seconds")
    d.add_argument("--limit", type=int, default=0)
    r = sub.add_parser("report", help="write AUDIO-PROFILES.md and per-area summaries")
    r.add_argument("--work", required=True)
    r.add_argument("--spec", required=True, help="docs/techguns-spec folder")
    sub.add_parser("selftest", help="check the measurement code")
    args = ap.parse_args()
    {"extract": cmd_extract, "describe": cmd_describe, "report": cmd_report, "selftest": cmd_selftest}[args.cmd](args)


if __name__ == "__main__":
    sys.exit(main())
