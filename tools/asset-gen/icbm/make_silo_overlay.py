"""Derives the Missile Silo controller overlays from the Launch Control overlays (NTM, LGPL-3.0).

Usage: python make_silo_overlay.py <repo root>

Reads machines/thirdparty/ntm/launch_control/OVERLAY_FRONT.png and writes machines/thirdparty/ntm/missile_silo/
OVERLAY_FRONT.png and OVERLAY_FRONT_ACTIVE.png (+ .mcmeta): the same terminal face with a yellow and black hazard
border so the silo reads differently from the Launch Control. The active overlay is a four-frame strip whose border
stripes march. The licence files are copied beside them.
"""
import shutil
import sys
from pathlib import Path

from PIL import Image

YELLOW = (240, 200, 20, 255)
BLACK = (20, 20, 20, 255)


def bordered(src, phase):
    out = src.copy()
    for x in range(16):
        for y in range(16):
            if x in (0, 15) or y in (0, 15):
                out.putpixel((x, y), YELLOW if ((x + y + phase) // 2) % 2 == 0 else BLACK)
    return out


def main(repo):
    res = Path(repo) / "src/main/resources/assets/gregguns/textures/blocks/machines/thirdparty/ntm"
    src_dir, dst = res / "launch_control", res / "missile_silo"
    dst.mkdir(parents=True, exist_ok=True)
    front = Image.open(src_dir / "OVERLAY_FRONT.png").convert("RGBA")
    bordered(front, 0).save(dst / "OVERLAY_FRONT.png")
    active = Image.open(src_dir / "OVERLAY_FRONT_ACTIVE.png").convert("RGBA")
    frames = active.height // 16
    strip = Image.new("RGBA", (16, 16 * frames))
    for i in range(frames):
        strip.paste(bordered(active.crop((0, 16 * i, 16, 16 * i + 16)), i * 2), (0, 16 * i))
    strip.save(dst / "OVERLAY_FRONT_ACTIVE.png")
    (dst / "OVERLAY_FRONT_ACTIVE.png.mcmeta").write_text('{"animation":{"frametime":10}}\n')
    for n in ("LICENSE.txt", "GPL-3.0.txt"):
        shutil.copyfile(src_dir / n, dst / n)


if __name__ == "__main__":
    main(sys.argv[1])
