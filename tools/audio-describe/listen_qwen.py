"""Listening stage: have Qwen2-Audio-7B-Instruct actually hear each sound and write free-text notes.

Runs on a GPU host with ~17 GB of free VRAM (bf16), or ~8 GB with --load-4bit. The output, listen.jsonl, is merged
into the Claude `describe` step by audio_describe.py, so the final profile combines what a model heard with
the spectrogram and the measurements.

  python listen_qwen.py --sounds <folder containing sounds/...> --out listen.jsonl [--load-4bit] [--limit N]

Resumable: files already in --out are skipped.
"""
import argparse
import json
from pathlib import Path

MODEL_ID = "Qwen/Qwen2-Audio-7B-Instruct"
PROMPT = (
    "You are helping sound designers recreate this video-game sound effect with original audio. "
    "Listen carefully, then describe: (1) what you hear, in time order (distinct events and layers); "
    "(2) timbre and texture (for example metallic, electronic, mechanical, organic, noisy, tonal); "
    "(3) pitch and any pitch movement; (4) loudness and how it changes; (5) how it starts and ends, "
    "including reverb or tail; (6) what it most likely represents. Be specific and concise, and say when unsure."
)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--sounds", required=True, help="folder that contains the sounds/ tree (e.g. assets/techguns)")
    ap.add_argument("--out", required=True)
    ap.add_argument("--load-4bit", action="store_true", help="4-bit quantisation (bitsandbytes) for smaller GPUs")
    ap.add_argument("--max-new-tokens", type=int, default=400)
    ap.add_argument("--limit", type=int, default=0)
    args = ap.parse_args()

    import librosa
    import torch
    from transformers import AutoProcessor, Qwen2AudioForConditionalGeneration

    root, out = Path(args.sounds), Path(args.out)
    done = set()
    if out.exists():
        done = {json.loads(line)["path"] for line in out.read_text(encoding="utf-8").splitlines() if line}
    files = [f for f in sorted(root.rglob("*.ogg")) if f.relative_to(root).as_posix() not in done][: args.limit or None]
    print(f"{len(done)} done, {len(files)} to listen to")
    if not files:
        return

    kwargs = {"device_map": "auto", "torch_dtype": torch.bfloat16}
    if args.load_4bit:
        from transformers import BitsAndBytesConfig
        kwargs["quantization_config"] = BitsAndBytesConfig(load_in_4bit=True, bnb_4bit_compute_dtype=torch.bfloat16)
    processor = AutoProcessor.from_pretrained(MODEL_ID)
    model = Qwen2AudioForConditionalGeneration.from_pretrained(MODEL_ID, **kwargs).eval()
    rate = processor.feature_extractor.sampling_rate

    for i, f in enumerate(files, 1):
        rel = f.relative_to(root).as_posix()
        audio, _ = librosa.load(str(f), sr=rate, mono=True)
        conversation = [{"role": "user", "content": [{"type": "audio", "audio_url": str(f)},
                                                       {"type": "text", "text": PROMPT}]}]
        text = processor.apply_chat_template(conversation, add_generation_prompt=True, tokenize=False)
        try:  # newer transformers name the argument `audio`, older ones `audios`
            inputs = processor(text=text, audio=[audio], sampling_rate=rate, return_tensors="pt", padding=True)
        except TypeError:
            inputs = processor(text=text, audios=[audio], sampling_rate=rate, return_tensors="pt", padding=True)
        inputs = inputs.to(model.device)
        with torch.no_grad():
            ids = model.generate(**inputs, max_new_tokens=args.max_new_tokens, do_sample=False)
        notes = processor.batch_decode(ids[:, inputs["input_ids"].shape[1]:], skip_special_tokens=True)[0].strip()
        with out.open("a", encoding="utf-8") as fh:
            fh.write(json.dumps({"path": rel, "model": MODEL_ID, "notes": notes}) + "\n")
        print(f"[{i}/{len(files)}] {rel}: {notes[:100]}", flush=True)


if __name__ == "__main__":
    main()
