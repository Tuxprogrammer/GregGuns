"""A/B listening tool: judge, caption and embed sound pairs with open audio-language models.

Backends (see CALIBRATION.md for which signals are trustworthy):
  af    Audio Flamingo Next (nvidia/audio-flamingo-next-hf, 8B, transformers >= 5.x, venv_af)
  mimo  MiMo-Audio-7B-Instruct (XiaomiMiMo, its own repo + venv_mimo)

Modes (comma separated, --modes):
  judge    both clips in one prompt, original first: ADDED / MISSING / DIFFERENT / CHANGES / GRADE (0-10).
           --swap also asks with the order swapped, to expose position bias.
  caption  each clip described alone (events in time order, timbre, pitch, loudness, tail).
  embed    audio-encoder embedding, mean-pooled over time, cosine(A, B) from the last layer and a middle layer.

Input: a TSV, one pair per line: "label<TAB>expected<TAB>A path<TAB>B path" (calibration-pairs.tsv format).
Lines starting with # are skipped; a two-column "A<TAB>B" file also works (label and expected stay empty).
Paths are relative to --root. Output: JSONL, one row per pair: label, expected, a, b, model, grade, added, missing,
different, changes, answer, grade_swapped, answer_swapped, caption_a, caption_b, cos_last, cos_mid, cos_early (+ dist_early_milli = 1000*(1-cos_early)).
Resumable: pairs already in --out are skipped. A is only listened to here, never used to synthesize anything.

Run on the server (<audio-server>) from the workspace /var/tmp/gregguns-audio:

  ssh -o BatchMode=yes <user>@<audio-server>
  cd /var/tmp/gregguns-audio && . ./env.sh
  CUDA_VISIBLE_DEVICES=1 venv_af/bin/python ab_listen.py --backend af --pairs my_pairs.tsv --root . \\
      --out out/my_run.jsonl --modes judge,caption,embed --swap
  (long runs: wrap in `setsid nohup ... > out/run.log 2>&1 &` and `touch` a done-marker afterwards)

Check `nvidia-smi` first and pick the GPU with at least 18 GB free; the GPUs are shared. Never `pkill -f` with a
pattern that also occurs in your own ssh command line.
"""
import argparse
import json
import re
import sys
from pathlib import Path

SECTIONS = ("ADDED", "MISSING", "DIFFERENT", "CHANGES", "GRADE")
JUDGE_PROMPT = (
    "Audio 1 is the {first}. Audio 2 is the {second}. The RECREATION was made to replace the ORIGINAL video-game "
    "sound effect in the same role. Listen to both carefully and compare them. Answer in exactly this format:\n"
    "ADDED: sounds or qualities in the recreation that the original does not have (for example beeps, tones, "
    "ringing, hiss, static, extra hits, extra low rumble)\n"
    "MISSING: sounds or qualities in the original that the recreation lacks (for example clicks, metal clanks, "
    "rattles, body, punch, tail)\n"
    "DIFFERENT: things both have but that differ (timing, length, loudness, pitch, brightness, texture, attack, tail)\n"
    "CHANGES: what to change in the recreation so it matches the original, most important first\n"
    "GRADE: a number from 0 to 10 for how well the recreation matches the original's role and feel "
    "(10 = a player would not notice the swap, 0 = a different sound)\n"
    "Be specific, describe what you actually hear, and say when unsure."
)
CAPTION_PROMPT = (
    "This is a short video-game sound effect. Describe it in detail: the events in time order (each hit, click, "
    "burst, sweep or tone), the timbre (metallic, noisy, tonal, hollow, thumpy, bright, dull), the pitch (low, "
    "mid, high), the loudness over time, and how it ends (sharp cut, short decay, long tail or rumble). "
    "Do not guess what game it is from."
)
ORIGINAL_ROLE = "ORIGINAL video-game sound effect"
RECREATION_ROLE = "RECREATION"
EMBED_MAX_S = 30.0
EARLY_LAYER = 2  # the layer whose mean-pooled cosine separated the calibration groups best (see CALIBRATION.md)


def parse(text):
    """Split an answer into its labeled sections; the grade becomes an int 0-10 or None."""
    out = {k.lower(): "" for k in SECTIONS}
    pattern = re.compile(r"^\s*[*#]*\s*(%s)\s*[*#]*\s*[:\-]" % "|".join(SECTIONS), re.I | re.M)
    marks = [(m.start(), m.end(), m.group(1).upper()) for m in pattern.finditer(text)]
    for i, (_, end, name) in enumerate(marks):
        stop = marks[i + 1][0] if i + 1 < len(marks) else len(text)
        out[name.lower()] = text[end:stop].strip().strip("*").strip()
    grade = None
    m = re.search(r"(\d+(?:\.\d+)?)\s*(?:/\s*10|out of 10)?", out["grade"]) if out["grade"] else None
    if m is None:
        m = re.search(r"GRADE\s*[:\-]?\s*\**\s*(\d+(?:\.\d+)?)", text, re.I)
    if m is not None:
        grade = max(0, min(10, round(float(m.group(1)))))
    out["grade"] = grade
    return out


def read_pairs(path):
    pairs = []
    for line in Path(path).read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("#"):
            continue
        cols = line.split("\t")
        if len(cols) == 2:
            cols = ["", ""] + cols
        pairs.append(dict(label=cols[0], expected=cols[1], a=cols[2], b=cols[3]))
    return pairs


def cosine(u, v):
    import numpy as np

    return float(np.dot(u, v) / (np.linalg.norm(u) * np.linalg.norm(v) + 1e-12))


def load_wav(path, rate, max_s=None):
    import librosa

    wav, _ = librosa.load(str(path), sr=rate, mono=True)
    return wav[: int(max_s * rate)] if max_s else wav


class AudioFlamingo:
    """Audio Flamingo Next via transformers (MusicFlamingoForConditionalGeneration)."""

    name = "nvidia/audio-flamingo-next-hf"
    SYSTEM = (
        "You are Audio Flamingo-Next, a multimodal assistant for language and audio. On each turn you receive an "
        "optional audio clip which may contain speech, music, or ambient sounds and optional text, you will receive "
        "at least one or both; use your world knowledge and reasoning to help the user with any task. Interpret the "
        "entirety of the content of any input audio—regardless of whether the user calls it audio, speech, "
        "music, or sound."
    )

    def __init__(self, model_id=None, max_new_tokens=500):
        import torch
        from transformers import AutoProcessor, MusicFlamingoForConditionalGeneration

        self.torch = torch
        self.name = model_id or self.name
        self.max_new_tokens = max_new_tokens
        self.processor = AutoProcessor.from_pretrained(self.name)
        # The processor insists on one clip per text; the model itself handles several <sound> slots in a text.
        self.processor.validate_inputs = lambda *a, **k: None
        # The model card says AutoModel, but that returns the bare backbone without lm_head and cannot generate.
        self.model = MusicFlamingoForConditionalGeneration.from_pretrained(self.name, dtype=torch.bfloat16, device_map="auto").eval()
        self.rate = self.processor.feature_extractor.sampling_rate

    def _generate(self, user_text, wavs):
        text = "<|im_start|>system\n%s<|im_end|>\n<|im_start|>user\n%s<|im_end|>\n<|im_start|>assistant\n" % (
            self.SYSTEM, user_text)
        inputs = self.processor(text=[text], audio=wavs, return_tensors="pt", sampling_rate=self.rate)
        inputs = inputs.to(self.model.device)
        if "input_features" in inputs:
            inputs["input_features"] = inputs["input_features"].to(self.model.dtype)
        with self.torch.no_grad():
            ids = self.model.generate(**inputs, max_new_tokens=self.max_new_tokens, do_sample=False,
                                      repetition_penalty=1.1)
        return self.processor.batch_decode(ids[:, inputs["input_ids"].shape[1]:], skip_special_tokens=True,
                                           clean_up_tokenization_spaces=False)[0].strip()

    def judge(self, wav_a, wav_b, swap=False):
        """wav_a is always the original. With swap the recreation is presented first."""
        if not swap:
            prompt = JUDGE_PROMPT.format(first=ORIGINAL_ROLE, second=RECREATION_ROLE)
            return self._generate("Audio 1: <sound>\nAudio 2: <sound>\n" + prompt, [wav_a, wav_b])
        prompt = JUDGE_PROMPT.format(first=RECREATION_ROLE, second=ORIGINAL_ROLE)
        return self._generate("Audio 1: <sound>\nAudio 2: <sound>\n" + prompt, [wav_b, wav_a])

    def caption(self, wav):
        return self._generate("<sound>\n" + CAPTION_PROMPT, [wav])

    def embed(self, wav):
        """Mean-pooled audio-encoder states: {"last": vec, "mid": vec} (numpy float32)."""
        torch = self.torch
        wav = wav[: int(EMBED_MAX_S * self.rate)]
        feats = self.processor.feature_extractor([wav], sampling_rate=self.rate, return_tensors="pt",
                                                 return_attention_mask=True)
        mask = feats["attention_mask"].to(self.model.device)
        x = feats["input_features"].to(self.model.device, self.model.dtype)
        tower = self.model.model.audio_tower if hasattr(self.model, "model") else self.model.audio_tower
        with torch.no_grad():
            out = tower(x, input_features_mask=mask, output_hidden_states=True, return_dict=True)
        n_mel = int(mask.sum())
        hs = out.hidden_states
        layers = []
        for h in hs:
            h = h[0].float()  # (frames, dim); frames are conv-downsampled mel frames
            valid = max(1, min(h.shape[0], (n_mel - 1) // 2 + 1))
            layers.append(h[:valid].mean(0).cpu().numpy())
        return {"last": layers[-1], "mid": layers[len(layers) // 2], "early": layers[EARLY_LAYER], "layers": layers}


class MiMoAudio:
    """MiMo-Audio-7B-Instruct through the XiaomiMiMo/MiMo-Audio repo (Apache-2.0).

    Needs torch 2.6 + transformers 4.49 + flash-attn 2.7.4 (venv_mimo), the cloned repo, and the Instruct and
    Tokenizer weights under models/. The repo's own helper takes one clip per question, so the multi-clip prompt is
    assembled here from its InputSegment pieces.
    """

    name = "XiaomiMiMo/MiMo-Audio-7B-Instruct"

    def __init__(self, repo, model_dir, tokenizer_dir, max_new_tokens=500):
        import torch

        sys.path.insert(0, str(repo))
        from src.mimo_audio.mimo_audio import MimoAudio
        from src.mimo_audio.modeling_mimo_audio import MiMoSampler, MiMoStopper
        from src.mimo_audio.process_speechdata import InputSegment

        self.torch = torch
        self.InputSegment = InputSegment
        self.mimo = MimoAudio(str(model_dir), str(tokenizer_dir))
        self.mimo.task_sampler_configs["audio_understanding"]["global"] = MiMoSampler(
            do_sample=False, temperature=1.0, top_p=1.0)  # greedy: reproducible grades
        self.stop = [MiMoStopper(stop_tokens=[self.mimo.tokenizer.eos_token_id, self.mimo.im_end_idx],
                                 group_size=self.mimo.group_size, audio_channels=self.mimo.audio_channels)]
        self.max_new_tokens = max_new_tokens
        self.rate = self.mimo.mimo_audio_tokenizer.config.sampling_rate

    def _seg(self, text=None, audio=None):
        return self.InputSegment(text=text if audio is None else "", audio=audio,
                                 speech_zeroemb_idx=self.mimo.speech_zeroemb_idx,
                                 text_zeroemb_idx=self.mimo.empty_token)

    def _ask(self, parts):
        """parts: strings and 1-D numpy waveforms (at self.rate), in prompt order."""
        torch = self.torch
        segs = [self._seg("<|im_start|>user\n")]
        for part in parts:
            if isinstance(part, str):
                segs.append(self._seg(part))
            else:
                segs.append(self._seg(audio=self.mimo.preprocess_input(torch.from_numpy(part).float())))
        segs += [self._seg("<|im_end|>\n"), self._seg("<|im_start|>assistant\n"), self._seg("<think>\n\n</think>\n")]
        ids = self.mimo.get_input_ids(segs)
        with torch.no_grad():
            text = self.mimo.forward(ids, stopping_criteria=self.stop, task_name="audio_understanding",
                                     max_new_tokens=self.max_new_tokens)
        return text.split("<|im_end|>")[0].strip()

    def judge(self, wav_a, wav_b, swap=False):
        if not swap:
            prompt = JUDGE_PROMPT.format(first=ORIGINAL_ROLE, second=RECREATION_ROLE)
            return self._ask(["Audio 1: ", wav_a, "\nAudio 2: ", wav_b, "\n" + prompt])
        prompt = JUDGE_PROMPT.format(first=RECREATION_ROLE, second=ORIGINAL_ROLE)
        return self._ask(["Audio 1: ", wav_b, "\nAudio 2: ", wav_a, "\n" + prompt])

    def caption(self, wav):
        return self._ask([wav, "\n" + CAPTION_PROMPT])

    def embed(self, wav):
        """Mean-pooled states of the MiMo-Audio-Tokenizer encoder (1.2B transformer) via forward hooks."""
        torch = self.torch
        enc = self.mimo.mimo_audio_tokenizer.encoder
        sums, counts, hooks = {}, {}, []

        def hook(i):
            def fn(_m, _inp, out):
                h = (out[0] if isinstance(out, tuple) else out).float()
                sums[i] = sums.get(i, 0) + h.sum(0)
                counts[i] = counts.get(i, 0) + h.shape[0]
            return fn

        mods = list(enc.layers) + [enc.layer_norm]
        for i, m in enumerate(mods):
            hooks.append(m.register_forward_hook(hook(i)))
        try:
            mel = self.mimo.wav2mel(torch.from_numpy(wav[: int(EMBED_MAX_S * self.rate)]).float().to(self.mimo.device))
            mel = mel.transpose(0, 1)
            self.mimo.encode_batch(mel, torch.tensor([mel.size(0)]))
        finally:
            for h in hooks:
                h.remove()
        layers = [(sums[i] / counts[i]).cpu().numpy() for i in range(len(mods))]
        return {"last": layers[-1], "mid": layers[len(layers) // 2], "early": layers[EARLY_LAYER], "layers": layers}


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--backend", choices=["af", "mimo"], default="af")
    ap.add_argument("--pairs", required=True)
    ap.add_argument("--root", default=".", help="folder the TSV paths are relative to")
    ap.add_argument("--out", required=True)
    ap.add_argument("--modes", default="judge,caption,embed")
    ap.add_argument("--swap", action="store_true", help="judge: also ask with the order swapped")
    ap.add_argument("--max-new-tokens", type=int, default=500)
    ap.add_argument("--save-emb", default=None, help="embed: folder for per-clip (layers x dim) .npy files")
    ap.add_argument("--mimo-dir", default="/var/tmp/gregguns-audio", help="mimo: workspace with mimo_repo/ and models/")
    ap.add_argument("--limit", type=int, default=0)
    ap.add_argument("--model-id", default=None, help="af: override the HF repo (for example the captioner variant)")
    args = ap.parse_args()
    modes = set(args.modes.split(","))

    out = Path(args.out)
    done = set()
    if out.exists():
        done = {(r["a"], r["b"]) for r in map(json.loads, filter(None, out.read_text(encoding="utf-8").splitlines()))}
    pairs = [p for p in read_pairs(args.pairs) if (p["a"], p["b"]) not in done]
    pairs = pairs[: args.limit or None]
    print(f"{len(done)} done, {len(pairs)} pairs to run, modes {sorted(modes)}", flush=True)
    if not pairs:
        return

    if args.backend == "af":
        lm = AudioFlamingo(args.model_id, args.max_new_tokens)
    else:
        ws = Path(args.mimo_dir)
        lm = MiMoAudio(ws / "mimo_repo", ws / "models/MiMo-Audio-7B-Instruct", ws / "models/MiMo-Audio-Tokenizer",
                       args.max_new_tokens)
    root = Path(args.root)
    wavs, captions, embeds = {}, {}, {}

    def wav(p):
        if p not in wavs:
            wavs[p] = load_wav(root / p, lm.rate)
        return wavs[p]

    for i, p in enumerate(pairs, 1):
        a, b = p["a"], p["b"]
        if not (root / a).exists() or not (root / b).exists():
            print(f"[{i}/{len(pairs)}] missing file for {a} / {b}, skipped", flush=True)
            continue
        row = dict(p, model=lm.name)
        if "judge" in modes:
            answer = lm.judge(wav(a), wav(b))
            row["answer"] = answer
            row.update(parse(answer))
            if args.swap:
                answer = lm.judge(wav(a), wav(b), swap=True)
                row["answer_swapped"] = answer
                row["grade_swapped"] = parse(answer)["grade"]
        if "caption" in modes:
            for key, path in (("caption_a", a), ("caption_b", b)):
                if path not in captions:
                    captions[path] = lm.caption(wav(path))
                row[key] = captions[path]
        if "embed" in modes:
            for path in (a, b):
                if path not in embeds:
                    embeds[path] = lm.embed(wav(path))
            row["cos_last"] = cosine(embeds[a]["last"], embeds[b]["last"])
            row["cos_mid"] = cosine(embeds[a]["mid"], embeds[b]["mid"])
            row["cos_early"] = cosine(embeds[a]["early"], embeds[b]["early"])
            row["dist_early_milli"] = 1000 * (1 - row["cos_early"])
        if "embed" in modes and args.save_emb:
            import numpy as np

            emb_dir = Path(args.save_emb)
            emb_dir.mkdir(parents=True, exist_ok=True)
            for path in (a, b):
                np.save(emb_dir / (path.replace("/", "__") + ".npy"), np.stack(embeds[path]["layers"]))
        with out.open("a", encoding="utf-8") as fh:
            fh.write(json.dumps(row) + "\n")
        print(f"[{i}/{len(pairs)}] {p['label']:16s} grade {row.get('grade')} swapped {row.get('grade_swapped')} "
              f"cos {row.get('cos_last', float('nan')):.3f}/{row.get('cos_mid', float('nan')):.4f}/"
              f"early d={row.get('dist_early_milli', float('nan')):.2f}e-3 {b}", flush=True)


if __name__ == "__main__":
    main()
