# audio-describe

A dev tool that profiles every sound in a mod's assets, describing what each sounds like, how loud it is and what layers it has, so that GregGuns can commission **original** replacements for the Techguns sounds. The output is [`docs/techguns-spec/AUDIO-PROFILES.md`](../../docs/techguns-spec/AUDIO-PROFILES.md), plus a "How each sound sounds" table in each spec area's `sounds.md`.

Claude can't take audio input, so the tool gathers evidence it can read, plus an optional model that actually listens:

| Stage | Script | Runs on | What it produces |
|---|---|---|---|
| 1. Measure | `audio_describe.py extract` | any machine with ffmpeg | Loudness (peak/RMS dBFS, EBU R128 LUFS, true peak), envelope (attack, decay, onsets, silence), spectrum (centroid, flatness, bands, peaks, brightness over time), stereo image; plus a log-frequency spectrogram and a waveform PNG |
| 2. Listen (optional) | `listen_qwen.py` | GPU host with ~17 GB VRAM (bf16), or ~8 GB with `--load-4bit` | Free-text listening notes from Qwen2-Audio-7B-Instruct, which *hears* the audio |
| 3. Describe | `audio_describe.py describe` | any machine with an Anthropic key | One structured profile per sound from Claude (`claude-sonnet-5-5` by default), combining the images, the measurements and the listening notes |
| 4. Report | `audio_describe.py report` | anywhere | `AUDIO-PROFILES.md` and the per-area summary tables (idempotent, between `audio-profiles` markers) |

Measurements are exact. The prose is an informed reading, and each profile carries a confidence level.

## Setup

```sh
python -m venv .venv && .venv/Scripts/pip install -r requirements.txt   # numpy, anthropic
ffmpeg -version                                                          # ffmpeg/ffprobe must be on PATH
python audio_describe.py selftest                                        # checks the measurement code
```

`describe` reads `ANTHROPIC_API_KEY` from the environment, or from the repo-root `.env`, which is git-ignored and must never be committed.

## Run

```sh
WORK=/some/scratch/dir    # keep outside the repo: it holds images derived from third-party audio
python audio_describe.py extract  --assets <path>/assets/techguns --work $WORK

# optional, on the GPU host (copy the sounds/ tree there):
pip install -r requirements-listen.txt
python listen_qwen.py --sounds <path>/assets/techguns --out listen.jsonl     # copy listen.jsonl into $WORK

python audio_describe.py describe --work $WORK            # Batches API (half price); resumable; --sync for small runs
python audio_describe.py report   --work $WORK --spec ../../docs/techguns-spec
```

`describe` is resumable: it skips sounds that already have a profile, and re-running retries failures and refusals. A pending batch ID is kept in `$WORK/batch.json` so polling can resume after an interruption.

**Cost:** about 3.5k input and 1.2k output tokens per sound. For all 319 Techguns sounds that is roughly $3 through the Batches API on `claude-sonnet-5-5`.

## A/B review of generated sounds

`compare` pairs each generated GregGuns sound with the Techguns sound it replaces, through `footprint_ref` in [`tools/asset-gen/manifest.json`](../asset-gen/manifest.json). It then asks Claude to list every difference between the two, and to write change notes for the synthesiser.

Claude still can't hear either file. The comparison has three inputs:
- both sides' spectrograms and waveforms;
- both sides' measurements;
- Qwen2-Audio listening notes on both sides.

```sh
NEW=/some/scratch/dir-gg
python audio_describe.py extract --assets <repo>/src/main/resources/assets/gregguns --work $NEW
python listen_qwen.py --sounds <repo>/src/main/resources/assets/gregguns --out listen.jsonl   # on the GPU host; copy into $NEW
python audio_describe.py compare --ref-work $WORK --new-work $NEW --manifest ../asset-gen/manifest.json --out $NEW/ab
python audio_describe.py compare-report --out $NEW/ab --report ../../docs/assets/previews/sound-ab-review.md
```

**Cost:** each comparison sends four images and about 4k tokens of evidence, and gets about 1.5k tokens back.

**Outputs:**
- `compare.jsonl` holds one review per sound. Each review has a verdict (keep, tweak or rework), a 0–10 score, the differences with severity, missing and extra elements, and change notes.
- The generator reads `compare.jsonl` to apply the change notes.

## Boundaries

- The Techguns audio is All Rights Reserved. This tool only *reads* it. Keep `$WORK` (spectrograms, waveforms, JSON) out of the repo; only the written descriptions and measurements go into the spec.
- The descriptions are a brief for original sound design, not a recipe for imitation. See [CONTRIBUTING § Assets](../../CONTRIBUTING.md#assets-and-localisation).
