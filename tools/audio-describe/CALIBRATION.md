# Listening-instrument calibration: Audio Flamingo Next and MiMo-Audio

Qwen2-Audio-7B failed calibration: it graded an original against itself 8 to 9, an explosion against a reload 7,
rejected tinkly reloads 8 to 10, and it answered yes to every question. This note tests its two replacements,
**Audio Flamingo Next** (`nvidia/audio-flamingo-next-hf`, 8B) and **MiMo-Audio-7B-Instruct** (Xiaomi, Apache-2.0), on
three uses each: an A/B judge, a captioner, and an encoder embedding. The tool is `ab_listen.py`; the scoring script
is `calibration_stats.py`; raw outputs are in `calibration-results/`.

## Verdict

| Use | Audio Flamingo Next | MiMo-Audio | Trust? |
|---|---|---|---|
| A/B judge, grade 0 to 10 | flat 8 to 9 for everything, position bias | flat 6 to 7 for everything | **No, either model** |
| A/B judge, forced choice or category word (AF only, tried as fixes) | always picks "Audio 2"; calls identical pairs "different" | not tried | **No** |
| Captioner | readable, plausible, shorter; hallucinates sources | long, templated, timestamped; hallucinates | Store only; do not score |
| Encoder embedding, **early layer** (AF layer 2) | **good>rejected AUC 1.00, good>weak 0.94** | AUC 0.86 / 0.86 | **Yes, AF early layer** |
| Encoder embedding, last layer | AUC 0.76; compresses the "close" range | AUC 0.69 | Only as a coarse "different sound" flag |

**Trust one signal: Audio Flamingo's early-layer embedding distance.** The audio-LLM answers (grades, choices,
verdicts) are no better than Qwen2-Audio's, which matches the SonicBench finding that LLM heads are near chance at
comparing physical attributes while the encoders keep the cues.

## Calibration set (fixed): `calibration-pairs.tsv`

48 pairs, paths relative to the workspace root (`ref/` = Techguns originals A, `b/` = current GregGuns B, `b_r1/`
= round 1 at commit `2c77051`, `b_r2/` = round 2 at `c52e299`).

| Group | n | Content | Expected |
|---|---|---|---|
| identical | 8 | A against itself (shots, reloads, explosion, beep, door, gore) | highest |
| good_shots | 10 | current shots the owner called great (`keep` in `sound-status.json` when picked) | high |
| weak_shots | 8 | current shots the review loop marked `rework` (**supplementary**: loop verdicts, not owner-heard) | lower than good_shots |
| rejected_static | 6 | round-2 reloads (flat white-noise static) against A | low to medium |
| rejected_tinkly | 8 | round-1 reloads (tinkly) against A | low to medium |
| mismatched | 8 | different sound classes (reload vs explosion, shot vs beep, door vs gore, ...) | very low |

Caveats. (1) n is 6 to 10 per group, so AUCs have wide error bars; one swapped pair moves them by 0.01 to 0.05.
(2) The owner-judged groups differ in category: good_shots are shots and the rejected groups are reloads, so a
shot-versus-reload effect could masquerade as quality. `weak_shots` is the same-category control, and the ranking
good < weak holds in the embedding (AUC 0.94). Its labels come from the loop's verdicts, which changed during this
work (the `keep` list shifted between rounds), so treat it as supporting evidence only. (3) All matched pairs have
the same duration as A (B was built to A's footprint), so duration does not explain the embedding results: a
duration-only score gets AUC 0.41 (good vs rejected).

## Results: Audio Flamingo Next (instruct), 48 pairs

Cells are mean (min to max). Run time: about 5 s per pair with every mode on one 3090, about 17 GB VRAM.

### 1. A/B judge (original first; "swapped" presents the recreation first)

| Group | grade | grade swapped | mean of both |
|---|---|---|---|
| identical | 8.6 (7 to 10) | 7.6 (6 to 10) | 8.1 |
| good_shots | 8.8 (8 to 10) | 8.0 (7 to 9) | 8.4 |
| weak_shots | 8.9 (8 to 10) | 8.3 (7 to 10) | 8.6 |
| rejected_static | 9.2 (8 to 10) | 7.7 (6 to 8) | 8.4 |
| rejected_tinkly | 9.0 (8 to 10) | 8.1 (6 to 10) | 8.6 |
| mismatched | 8.1 (5 to 10) | 7.9 (7 to 8) | 8.0 |

AUC good>rejected 0.42 (below chance), good>mismatched 0.63, identical>good 0.46. Ordering identical > good >
rejected > mismatched: **no**. A turn-order bias is visible (mean grade 8.75 original-first, 7.94 swapped;
correlation between the two orders only 0.34). The ADDED/MISSING/DIFFERENT text is generic and often
self-contradictory (for identical clips it listed "a slightly more pronounced click" as a difference).

Fixes tried on the same set (`calibration-results/af_judge_variants.jsonl`):
* **Forced choice** ("Audio 1 original; which of Audio 2 / Audio 3 is closer?"), the true B against a
  different-class foil, both orders: it chose "2" in 48 of 48 asked-first and never "3" when swapped. Pure position
  bias.
* **Category verdict** (identical / very similar / similar / different / very different): identical pairs got
  "different" or "very different" 8 of 8 times; good shots "very different" 6 of 10. Anti-informative.

### 2. Captioner

Each clip described alone (events, timbre, pitch, loudness, tail). The captions are fluent and sometimes right about
gross structure (it described the round-1 reload as "a single sharp metallic impact followed by a brief resonant
ring", unlike A's "series of clicks"), but they hallucinate sources ("glass breaking", "stylus on glass") and use
the same vocabulary for A, good B and rejected B. TF-IDF cosine between the A and B captions: good 0.07, weak 0.16,
rejected 0.05 to 0.06, mismatched 0.10; AUC good>rejected 0.64, good>mismatched 0.51. Not a usable score. Captions are
stored per pair as `caption_a`/`caption_b` for later reading by a text model. I used the instruct checkpoint, not the
`-captioner-hf` variant (another 16 GB does not fit the disk budget next to the venvs).

### 3. Encoder embedding (mean-pooled over time, cosine between A and B)

| Group | last layer cos | middle layer (16) cos | **early layer (2) cos** | **early distance, 1000 x (1 - cos)** |
|---|---|---|---|---|
| identical | 1.0000 | 1.0000 | 1.0000 | 0.00 |
| good_shots | 0.939 (0.842 to 0.967) | 0.9988 | 0.9990 (0.9983 to 0.9993) | **1.04 (0.73 to 1.71)** |
| weak_shots | 0.917 (0.828 to 0.978) | 0.9984 | 0.9976 | 2.39 (1.30 to 3.81) |
| rejected_static | 0.916 (0.889 to 0.948) | 0.9974 | 0.9970 | 3.04 (1.99 to 4.09) |
| rejected_tinkly | 0.926 (0.884 to 0.956) | 0.9979 | 0.9968 | 3.24 (1.76 to 5.34) |
| mismatched | 0.678 (0.390 to 0.892) | 0.9954 | 0.9888 (0.9805 to 0.9944) | **11.21 (5.62 to 19.52)** |

| AUC | last | middle | early (2) | log-mel baseline |
|---|---|---|---|---|
| good > rejected | 0.76 | 0.96 | **1.00** | 0.49 |
| good > weak_shots | 0.57 | 0.75 | **0.94** | 0.69 |
| good > mismatched | 0.95 | 1.00 | 1.00 | 0.99 |
| rejected > mismatched | 0.98 | 0.97 | 1.00 | 1.00 |

The mean ordering identical > good > weak > rejected > mismatched holds for the early layer and **fails for the last
layer** (good 0.939 vs rejected 0.916-0.926 overlap and one good shot sits at 0.842, below two mismatched pairs). The log-mel baseline (cosine
of mean log-mel spectra) cannot tell good from rejected (0.49), so the early encoder layers carry something a
spectrum average does not. Layers 0 to 6 all work (good>rejected 0.98 to 1.00 raw); centering or z-scoring the
vectors made it worse. Values are tightly packed near 1 (anisotropy), hence the distance column.

## Results: MiMo-Audio-7B-Instruct, 48 pairs

Run time: about 35 s per pair (long generations), about 18 GB VRAM. Prompts are multi-clip, built from the repo's
`InputSegment` pieces; decoding is greedy. Embedding comes from the MiMo-Audio-Tokenizer encoder (1.2B transformer)
through forward hooks.

| Group | grade | grade swapped | mid-layer cos (16) | early-layer cos (2) | early distance x1000 |
|---|---|---|---|---|---|
| identical | 7.1 (6 to 8) | 7.0 | 1.0000 | 1.0000 | 0 |
| good_shots | 6.8 (6 to 7) | 7.3 | 0.9952 (0.9907 to 0.9978) | 0.9921 (0.9872 to 0.9959) | 7.9 (4.1 to 12.8) |
| weak_shots | 6.9 | 7.1 | 0.9910 | 0.9837 | 16.3 (8.5 to 33.1) |
| rejected_static | 7.0 | 6.8 | 0.9903 | 0.9854 | 14.6 (9.9 to 24.9) |
| rejected_tinkly | 7.1 | 6.8 | 0.9898 | 0.9852 | 14.8 (6.9 to 24.3) |
| mismatched | 6.1 (6 to 7) | 6.9 | 0.9542 (0.8704 to 0.9822) | 0.9367 (0.8624 to 0.9720) | 63.3 (28.0 to 137.6) |

* **Judge:** grades live in a 6 to 8 band for everything (AUC good>rejected 0.63 order-averaged, identical>good 0.47;
  the two orders correlate at 0.08, i.e. noise). Not usable.
* **Captioner:** about 1,600 characters, structured "Hit 1 (0:00-0:01) ..." lists that look templated and describe A
  and B with the same words. TF-IDF AUC good>rejected 0.66. Store only.
* **Embedding:** the ordering identical > good > rejected > mismatched holds for the mid and early layers. AUC
  good>rejected 0.86 to 0.89, good>weak 0.84 to 0.86. Its dynamic range is wider (mismatched 63 vs good 8), but the
  good/rejected overlap is larger (good max 12.8 vs rejected min 6.9), so it is a weaker discriminator than AF's early
  layer. It is not part of the shipped path (MiMo weights were removed to stay under the disk budget; see below).

## Recommended use and thresholds

Use **`dist_early_milli`** from `ab_listen.py --modes embed` (backend `af`): 1000 x (1 - cosine of the mean-pooled
layer-2 encoder states of A and B). Lower is closer. Read it against these bands (calibration extremes in
parentheses):

| dist_early_milli | Meaning | Evidence |
|---|---|---|
| 0 | the same file | identical: 0.00 |
| **<= 1.5** | close: as near to A as the shots the owner called great | good_shots 0.73 to 1.71 (8 of 10 at or below 1.5; weak shots 1.30 and 1.34 also land here) |
| **1.5 to 4.5** | same class of sound, noticeably off; the zone where the owner rejected tinkly and static reloads and where loop-`rework` shots live | rejected 1.76 to 5.34, weak 1.30 to 3.81 |
| **>= 5.5** | a different sound class | mismatched 5.62 to 19.52; nothing else above 5.34 |

Treat the 1.5 line as a screening threshold, not a guarantee: the nearest rejected clip is 1.76 and the farthest
good shot is 1.71, so the margin is thin. Use it to **rank candidate revisions and reject regressions** (a revision
whose distance fell is closer; one that crossed above about 4.5 should not ship), not as the final accept test. The
owner's ears stay the arbiter.

Also usable: `cos_last` below 0.80 flags a different sound with no false alarms (4 of 8 mismatched pairs, none of the
40 others), but the other 4 mismatched pairs sit at 0.85 to 0.89, inside the good-shot range (0.84 to 0.97), so it adds
nothing over the early layer.

**Do not trust:** the judge grade (either model, either order), the forced choice, the category verdict, and any
score derived from captions. Keep calling the judge only if you want its free-text ADDED/MISSING lists as
hints; they hallucinate and contradict themselves, so verify against measurements.

Sanity check on live files with the shipped tool: current `guns/ak47_reload.ogg` scores 3.04 (rejected band) and
`guns/ak47_shot_b.ogg` scores 0.75 (close).

## Fast check during revision

`af_embed_fast.py` computes the same `dist_early_milli` while loading only the audio encoder (487 tensors read
straight from the checkpoint, no language model). It starts in about 10 s and uses about 1.5 GB of GPU memory, so it
fits beside other users' jobs, and it caches the A-side embeddings. Checked against `ab_listen.py --modes embed` on 8
live pairs: identical to the printed precision (for example `guns/ak47_shot_b.ogg` 0.75). The revision loop calls
it after every candidate render, so a change is kept only if it moves the distance down.

## Server setup (left in place)

Server `<user>@<audio-server>`, workspace `/var/tmp/gregguns-audio/` (about 28 GB used of the 35 GB budget).

| Path | Content |
|---|---|
| `env.sh` | exports `HF_HOME`, `TMPDIR`, uv and pip caches into the workspace; `. ./env.sh` before running |
| `venv_af/` | Python 3.12 (uv-managed in `pythons/`), torch 2.14 cu130, transformers 5.20.1, librosa, soundfile, scikit-learn (5.8 GB) |
| `hf/hub/models--nvidia--audio-flamingo-next-hf` | AF-Next instruct weights (16 GB) |
| `venv_mimo/`, `mimo_repo/` | MiMo env (torch 2.6, transformers 4.49, flash-attn 2.7.4) and the cloned repo (commit `691ce54`); **weights removed** |
| `ref/`, `b/`, `b_r1/`, `b_r2/` | A, current B, round-1 B, round-2 B (`sounds/...` trees) |
| `ab_listen.py`, `calibration_stats.py`, `calibration-pairs.tsv` | this folder's tools |
| `out/` | calibration outputs (`af_cal.jsonl`, `mimo_cal.jsonl`, per-clip embeddings `af_emb/`, `mimo_emb/`) |

### Run on new pairs

```sh
ssh -o BatchMode=yes <user>@<audio-server>
nvidia-smi --query-gpu=index,memory.used --format=csv     # pick a GPU with >= 18 GB free (1 and 2 usually)
cd /var/tmp/gregguns-audio && . ./env.sh
# put the new B files under a folder next to ref/ (for example new_b/sounds/...), write a TSV of
# "A path<TAB>B path" lines (or the 4-column calibration format), then:
CUDA_VISIBLE_DEVICES=1 venv_af/bin/python ab_listen.py --backend af --pairs new.tsv --root . \
    --out out/new.jsonl --modes embed,caption
```

Embedding only is about 1 s per clip. Add `judge` (and `--swap`) only for free-text hints. For long runs:
`CUDA_VISIBLE_DEVICES=1 setsid nohup sh -c "...; touch out/new.done" > out/new.log 2>&1 &` and poll with short ssh
calls. The tool resumes: pairs already in `--out` are skipped. Score a calibration-style run with
`venv_af/bin/python calibration_stats.py --jsonl out/new.jsonl --emb-dir out/new_emb --root .` (needs `--save-emb`).
Copy `ab_listen.py` up with `scp` after editing it in the repo.

Do not `pkill -f` or `pgrep -f` with a pattern that appears in your own ssh command line.

### Recreate the MiMo weights if wanted (about 20 GB, needs free disk first)

```sh
cd /var/tmp/gregguns-audio && . ./env.sh && HF_HUB_DISABLE_XET=1 venv_boot/bin/python -c "
from huggingface_hub import snapshot_download as s
s('XiaomiMiMo/MiMo-Audio-7B-Instruct', local_dir='models/MiMo-Audio-7B-Instruct')
s('XiaomiMiMo/MiMo-Audio-Tokenizer', local_dir='models/MiMo-Audio-Tokenizer')"
CUDA_VISIBLE_DEVICES=1 venv_mimo/bin/python ab_listen.py --backend mimo --pairs new.tsv --root . --out out/new_mimo.jsonl
```

Install recipe used for `venv_mimo` (uv): `torch==2.6.0 torchaudio==2.6.0 transformers==4.49.0 accelerate librosa
soundfile scipy "numpy<2.3"` plus the prebuilt `flash_attn-2.7.4.post1+cu12torch2.6cxx11abiFALSE-cp312` wheel from
the flash-attention releases (einops may also be needed).

## Notes and gotchas

* The AF model card says `AutoModel`, but that returns the bare backbone without `lm_head` (no `generate`). The tool
  uses `MusicFlamingoForConditionalGeneration` (the class named in `config.json`).
* The AF processor accepts one clip per text; the tool disables its length check and puts two or three `<sound>`
  slots in one prompt, which the model handles (it builds per-clip timestamps).
* HF downloads with Xet enabled left a duplicate copy in `HF_HOME/hub` and ran the disk out of space once; set
  `HF_HUB_DISABLE_XET=1` for `local_dir` downloads.
* `/var` is shared. At one point only 4 GB was free; check `df -h /var` before downloads.
* AF is licensed NVIDIA OneWay Noncommercial; our internal research and QA use is fine. MiMo is Apache-2.0.
* A is only listened to or measured here. Nothing from A feeds generation.

## MVP accept tier

`tools/asset-gen/sound_gate.py` has a fourth verdict, **accept**: every measured check passes (tonality included) and the
embedding distance is below 4.5, the "should not ship" line from the results above. **keep** is unchanged (all checks pass,
distance 1.75 or less), and tweak and rework are unchanged for everything else.

Why: the owner closed the audio loop for MVP on 2026-10-19. The 1.75 line was calibrated on gunshots only (approved shots
reach 1.71, rejected reloads start at 1.76), so it is stricter than needed for other families. Sounds that pass every
measured check and sit between 1.75 and 4.5 are good enough to ship now and stay on the list of generators to revisit later.
