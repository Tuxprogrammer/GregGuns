#!/usr/bin/env python3
"""Cuts the Minigun's spin-up and spin-down sounds out of a CC0 field recording.

Source (human-made, CC0 1.0): "minigun.mp3" by scorepion, https://freesound.org/people/scorepion/sounds/424917/
(license text: https://creativecommons.org/publicdomain/zero/1.0/). The recording is a minigun that spools up, fires
for about three seconds and spins down; the recording's quiet lead-in is the spin-up and the tail after the last round
is the spin-down. Only those two windows are used (no rounds of the recording are kept).

Usage:  python tools/asset-gen/minigun_spin_sounds.py <scorepion_424917.mp3> <out_dir>
Writes <out_dir>/minigun_spin_up.ogg and minigun_spin_down.ogg (mono, 44.1 kHz, Vorbis). Needs ffmpeg.
Same input, same output: the cut points and gains below are fixed.
"""
import subprocess
import sys

# (name, start s, length s, fade-in s, fade-out s, gain dB)
CUTS = [
    # the last 0.75 s before the first round: the rotor winding up to speed, matching the 15 tick gate
    ("minigun_spin_up", 0.95, 0.75, 0.20, 0.03, 2.0),
    # from the last round to a rest: 1.6 s, matching the 30 tick coast (the sound dies out a little before rest)
    ("minigun_spin_down", 4.99, 1.60, 0.01, 0.70, 5.0),
]


def main(src, out):
    for name, start, length, fin, fout, gain in CUTS:
        af = "afade=t=in:d=%.3f,afade=t=out:st=%.3f:d=%.3f,volume=%ddB,alimiter=limit=0.8" % (
            fin, length - fout, fout, gain)
        subprocess.run(["ffmpeg", "-v", "error", "-y", "-ss", "%.3f" % start, "-t", "%.3f" % length, "-i", src,
                        "-ac", "1", "-ar", "44100", "-af", af, "-c:a", "libvorbis", "-q:a", "3",
                        "%s/%s.ogg" % (out, name)], check=True)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
