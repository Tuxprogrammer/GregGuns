"""Local preview sheet of the I4 detection and defense art (git-ignored output).

    python -I tools/asset-gen/icbm/defense_sheet.py [out.png]

Also checks every block face: 16 px wide, fully opaque, no magenta.
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / "infographics"))
import render3d as R  # noqa: E402

REPO = HERE.parents[2]
A = REPO / "src/main/resources/assets/gregguns"
DIRS = ("radar_hv", "radar_ev", "radar_tower", "air_defense_battery", "emp_tower")
FILES = ("OVERLAY_FRONT", "OVERLAY_SIDE", "OVERLAY_TOP", "OVERLAY_BOTTOM")
BG, FG, DIM = (30, 33, 39, 255), (238, 240, 243, 255), (176, 182, 192, 255)
Z = 6


def font(n):
    for p in ("/usr/share/fonts/TTF/DejaVuSans.ttf", "/usr/share/fonts/dejavu/DejaVuSans.ttf"):
        try:
            return ImageFont.truetype(p, n)
        except OSError:
            pass
    return ImageFont.load_default()


def check(path):
    im = Image.open(path).convert("RGBA")
    a = np.array(im)
    assert im.width == 16 and im.height % 16 == 0, path
    assert (a[..., 3] == 255).all(), f"transparent pixel in {path}"
    assert not ((a[..., 0] > 200) & (a[..., 1] < 60) & (a[..., 2] > 200)).any(), f"magenta in {path}"
    return im


def zoom(im, z=Z):
    return im.resize((im.width * z, im.height * z), Image.NEAREST)


def main():
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else REPO / "docs/assets/previews/icbm/i4-detection-defense.png"
    cell = 16 * Z + 8
    cols = len(FILES) + 4  # faces + 4 active frames
    W = 150 + cols * cell + 330
    rows_h = 16 * Z + 30
    sheet = Image.new("RGBA", (W, 60 + len(DIRS) * rows_h + 40 + 260 + 40 + 360), BG)
    d = ImageDraw.Draw(sheet)
    d.text((16, 14), "ICBM I4: detection and defense art (NTM-derived, LGPL-3.0)", font=font(22), fill=FG)
    d.text((16, 44), "Left to right: front, side, top, bottom, then FRONT_ACTIVE frames 1-4. True 16x16 beside the 6x zoom.", font=font(13), fill=DIM)
    y = 70
    for name in DIRS:
        base = A / "textures/blocks/machines/thirdparty/ntm" / name
        d.text((16, y + 40), name, font=font(13), fill=FG)
        x = 150
        for f in FILES:
            im = check(base / f"{f}.png")
            sheet.paste(zoom(im), (x, y))
            sheet.paste(im, (x + 16 * Z - 18, y + 16 * Z + 4))
            x += cell
        act = check(base / "OVERLAY_FRONT_ACTIVE.png")
        for i in range(4):
            sheet.paste(zoom(act.crop((0, 16 * i, 16, 16 * i + 16))), (x, y))
            x += cell
        check(base / "OVERLAY_TOP_ACTIVE.png")
        y += rows_h
    # casing + icons
    d.text((16, y + 4), "radar casing", font=font(13), fill=FG)
    cas = check(A / "textures/blocks/casings/thirdparty/ntm/radar_casing.png")
    sheet.paste(zoom(cas), (150, y))
    sheet.paste(cas, (150 + 16 * Z + 6, y + 70))
    x = 330
    for n in ("sam_missile", "radar_gun", "remote_detonator", "defuser"):
        im = Image.open(A / f"textures/items/thirdparty/ntm/{n}.png").convert("RGBA")
        assert im.size == (16, 16)
        tile = Image.new("RGBA", (16 * Z, 16 * Z), (60, 64, 72, 255))
        tile.alpha_composite(zoom(im))
        sheet.paste(tile, (x, y))
        sheet.paste(im, (x + 16 * Z - 18, y + 16 * Z + 4))
        d.text((x, y + 16 * Z + 4), n, font=font(11), fill=DIM)
        x += 16 * Z + 24
    y += 16 * Z + 40
    # SAM model
    d.text((16, y), "SAM missile model (one block tall, nose up) and its texture", font=font(13), fill=FG)
    sc = R.obj_scene(A / "models/thirdparty/ntm/sam_missile.obj", A / "textures/models/thirdparty/ntm/sam_missile.png")
    lo, hi = sc.bounds()
    d.text((16, y + 20), "height %.2f, width %.2f x %.2f" % (hi[1] - lo[1], hi[0] - lo[0], hi[2] - lo[2]), font=font(12), fill=DIM)
    x = 16
    for yaw, pitch in ((30, 25), (90, 0), (0, 70)):
        v = R.trim(R.render(sc, yaw=yaw, pitch=pitch, ppu=300, outline=(10, 12, 16, 230, 2)))
        sheet.alpha_composite(v, (x, y + 44))
        x += v.width + 30
    t = Image.open(A / "textures/models/thirdparty/ntm/sam_missile.png").convert("RGBA")
    tt = Image.new("RGBA", t.size, (90, 90, 96, 255))
    tt.alpha_composite(t)
    sheet.paste(tt.resize((t.width * 3, t.height * 3), Image.NEAREST), (x + 20, y + 44))
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.convert("RGB").save(out)
    print("wrote", out, sheet.size)


if __name__ == "__main__":
    main()
