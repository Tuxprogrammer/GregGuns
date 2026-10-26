"""The warhead preview sheet: every tier's model in three views at one shared scale, with its texture.

    python tools/asset-gen/icbm/warheads_sheet.py [out.png]        # default docs/assets/previews/icbm/warheads.png (git-ignored: previews stay local, D50)

Reads the shipped OBJ files under assets/gregguns/models/thirdparty/ntm through the strict Forge reader
(render3d.obj_scene), so the sheet shows what the game draws. All views use the same pixels-per-block, and the
side view carries a block ruler (a tick and light guide per block of height) so the size reads at a glance.
"""

import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / "infographics"))

import render3d as R  # noqa: E402

REPO = HERE.parents[2]
ASSETS = REPO / "src/main/resources/assets/gregguns"
OUTLINE = (10, 12, 16, 230, 2)
BG, CARD, FG, DIM = (24, 27, 32, 255), (34, 38, 45, 255), (238, 240, 243, 255), (176, 182, 192, 255)
ACCENT = (255, 196, 64, 255)
PPU = 110
VIEWS = (("3/4", 30, 25), ("side", 90, 0), ("top", 0, 70))
# (file, tier label, footprint text)
TIERS = (
    ("warhead_t1", "Tier 1", "fits 1 x 1 x 1"),
    ("warhead_t2", "Tier 2", "fits 1 x 1 x 1"),
    ("warhead_t3", "Tier 3 conventional", "1 x 1 x 2 blocks"),
    ("warhead_nuclear", "Nuclear", "1 x 1 x 2 blocks"),
    ("warhead_antimatter", "Antimatter", "2 x 2 x 4 blocks"),
    ("warhead_redmatter", "Red matter", "2 x 2 x 4 blocks"),
)


def font(size, bold=False):
    names = ["DejaVuSans-Bold.ttf" if bold else "DejaVuSans.ttf", "NotoSans-Bold.ttf" if bold else "NotoSans-Regular.ttf"]
    for n in names:
        try:
            return ImageFont.truetype(n, size)
        except OSError:
            pass
    return ImageFont.load_default()


def card(name, tier, foot, H=None):
    scene = R.obj_scene(ASSETS / f"models/thirdparty/ntm/{name}.obj", ASSETS / f"textures/models/thirdparty/ntm/{name}.png")
    lo, hi = scene.bounds()
    size = [hi[i] - lo[i] for i in range(3)]
    views = [R.render(scene, yaw=y, pitch=p, ppu=PPU, outline=OUTLINE) for _, y, p in VIEWS]
    faces = len(scene.quads)
    W = 880
    H = H or int(4.0 * PPU) + 96
    img = Image.new("RGBA", (W, H), CARD)
    d = ImageDraw.Draw(img)
    d.text((18, 12), tier, font=font(24, True), fill=FG)
    d.text((18, 44), f"{foot}   model {size[0]:.2f} x {size[2]:.2f} wide, {size[1]:.2f} tall   {faces} faces", font=font(15), fill=DIM)
    base = H - 18  # y of the model's base line
    x = 54
    for (label, _, _), v in zip(VIEWS, views):
        # the render has pad 8 under the model's lowest point for the side view; others are centered, so align by bottom pad
        img.alpha_composite(v, (x, base - v.height + 8))
        d.text((x, base + 0), label, font=font(13), fill=DIM)
        x += v.width + 22
    # block ruler on the left, one tick per block of height
    d.line([(40, base), (40, base - int(size[1] * PPU + 0.999))], fill=ACCENT, width=2)
    for k in range(int(size[1] + 0.999) + 1):
        yy = base - k * PPU
        if True:
            d.line([(34, yy), (46, yy)], fill=ACCENT, width=2)
            d.text((8, yy - 8), f"{k}", font=font(13, True), fill=ACCENT)
    tex = Image.open(ASSETS / f"textures/models/thirdparty/ntm/{name}.png").convert("RGBA")
    s = max(1, 150 // tex.width)
    tex = tex.resize((tex.width * s, tex.height * s), Image.NEAREST)
    pw = Image.new("RGBA", (tex.width + 8, tex.height + 8), (60, 66, 76, 255))
    pw.alpha_composite(tex, (4, 4))
    img.alpha_composite(pw, (W - pw.width - 18, 18))
    d.text((W - pw.width - 18, 22 + pw.height), f"texture {Image.open(ASSETS / f'textures/models/thirdparty/ntm/{name}.png').size[0]} px", font=font(13), fill=DIM)
    return img


def main():
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else REPO / "docs/assets/previews/icbm/warheads.png"
    cols, rows = 2, 3
    heights = []
    for r in range(rows):
        hs = []
        for name, _, _ in TIERS[r * cols:(r + 1) * cols]:
            lo, hi = R.obj_scene(ASSETS / f"models/thirdparty/ntm/{name}.obj", ASSETS / f"textures/models/thirdparty/ntm/{name}.png").bounds()
            hs.append(hi[1] - lo[1])
        heights.append(max(int(-(-max(hs) // 1) * PPU) + 100, 270))
    cards = [card(*t, H=heights[i // cols]) for i, t in enumerate(TIERS)]
    cw = cards[0].width
    pad, head, foot_h = 24, 90, 56
    sheet = Image.new("RGBA", (pad + cols * (cw + pad), head + sum(heights) + rows * pad + foot_h), BG)
    d = ImageDraw.Draw(sheet)
    d.text((pad, 18), "ICBM warheads by tier", font=font(32, True), fill=FG)
    d.text((pad, 60), "Same scale in every view. Ruler: one tick per block of height. Models and textures from HBM's Nuclear Tech Mod, LGPL-3.0, scaled and recolored; labels removed.", font=font(15), fill=DIM)
    for i, c in enumerate(cards):
        sheet.alpha_composite(c, (pad + (i % cols) * (cw + pad), head + sum(heights[: i // cols]) + (i // cols) * pad))
    d.text((pad, sheet.height - 36), "Attribution: warhead models and textures (c) HBM's Nuclear Tech Mod contributors, GNU LGPL v3; modified.", font=font(14), fill=DIM)
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.convert("RGB").save(out)
    print("wrote", out.resolve(), sheet.size)


if __name__ == "__main__":
    main()
