"""Readable marks on the EMP and endothermic T3 warhead skins (HBM's Nuclear Tech Mod art, LGPL-3.0, modified).

    python -I tools/asset-gen/icbm/paint_warhead_marks.py <dir holding assets/hbm> <repo root>

NTM draws one small "N2" plate on one slice of the endothermic barrel and nothing but bands on the EMP one, so from
most sides a placed warhead shows no mark. This repeats the "N2" plate on three slices 120 degrees apart (the barrel
is 12 slices of 6 texels) and paints a white lightning bolt on a dark magenta plate three times around the EMP
barrel's straight section (12 slices of 4 texels, v 45 to 55).
"""
import sys
from pathlib import Path

from PIL import Image

PARTS = "assets/hbm/textures/models/missile_parts/warheads"
OUT = "src/main/resources/assets/gregguns/textures/models/thirdparty/ntm"

BOLT = [
    "...XXXX.",
    "..XXX...",
    ".XXX....",
    "XXXXXX..",
    "...XXX..",
    "..XX....",
    ".XX.....",
    "XX......",
]
PLATE = (38, 0, 46, 255)
EDGE = (255, 0, 255, 255)
WHITE = (255, 255, 255, 255)


def endothermic(src, dest):
    img = Image.open(src).convert("RGBA")
    plate = img.crop((24, 58, 36, 74))  # the "N2" plate: slices 4 and 5
    for u in (0, 48):
        img.paste(plate, (u, 58))
    img.save(dest)


def emp(src, dest):
    img = Image.open(src).convert("RGBA")
    px = img.load()
    for u0 in (0, 16, 32):  # three plates, 3 slices wide each, 4 slices apart
        for x in range(12):
            for y in range(11):
                edge = x in (0, 11) or y in (0, 10)
                px[u0 + x, 45 + y] = EDGE if edge else PLATE
        for r, row in enumerate(BOLT):
            for c, ch in enumerate(row):
                if ch == "X":
                    px[u0 + 2 + c, 46 + r] = WHITE
    img.save(dest)


def main(src, repo):
    parts = Path(src) / PARTS
    out = Path(repo) / OUT
    emp(parts / "mp_w_10_cloud.png", out / "warhead_emp.png")
    endothermic(parts / "mp_w_15_n2.png", out / "warhead_endothermic.png")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
