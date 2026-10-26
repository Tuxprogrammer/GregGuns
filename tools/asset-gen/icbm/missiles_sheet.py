"""The missile preview sheet: each tier's body alone and stacked with its warhead, at one shared scale.

    python tools/asset-gen/icbm/missiles_sheet.py [out.png]        # default docs/assets/previews/icbm/missiles.png (git-ignored: D50)

Reads the shipped OBJ files under assets/gregguns/models/thirdparty/ntm through the strict Forge reader
(render3d.obj_scene). The warhead is moved up by the body's top (the nose seat) exactly as the game seats it.
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
PPU = 90
# (body, warhead, label)
TIERS = (("missile_body_t1", "warhead_t1", "Tier 1"), ("missile_body_t2", "warhead_t2", "Tier 2"),
         ("missile_body_t3", "warhead_t3", "Tier 3"))


def font(size, bold=False):
    for n in ("DejaVuSans-Bold.ttf" if bold else "DejaVuSans.ttf", "NotoSans-Bold.ttf" if bold else "NotoSans-Regular.ttf"):
        try:
            return ImageFont.truetype(n, size)
        except OSError:
            pass
    return ImageFont.load_default()


def load(name):
    return R.obj_scene(ASSETS / f"models/thirdparty/ntm/{name}.obj", ASSETS / f"textures/models/thirdparty/ntm/{name}.png")


def stacked(body, warhead):
    seat = body.bounds()[1][1]
    s = R.Scene()
    s.extend(body)
    s.extend(warhead, R.tr(0, seat, 0))
    return s, seat


def card(body_name, wh_name, label, H):
    body, wh = load(body_name), load(wh_name)
    full, seat = stacked(body, wh)
    lo, hi = full.bounds()
    W = 1180
    img = Image.new("RGBA", (W, H), CARD)
    d = ImageDraw.Draw(img)
    d.text((18, 12), label, font=font(24, True), fill=FG)
    d.text((18, 44), f"body {body.bounds()[1][1]:.2f} tall, nose seat at y = {seat:.2f}; stacked {hi[1] - lo[1]:.2f} tall, {hi[0] - lo[0]:.2f} wide", font=font(15), fill=DIM)
    base = H - 22
    x = 56
    for scene, yaw, pitch, cap in ((body, 90, 0, "body, side"), (full, 90, 0, "stacked, side"), (full, 35, 14, "stacked, 3/4"), (full, 0, 70, "stacked, top")):
        v = R.render(scene, yaw=yaw, pitch=pitch, ppu=PPU, outline=OUTLINE)
        img.alpha_composite(v, (x, base - v.height + 8))
        d.text((x, base + 2), cap, font=font(13), fill=DIM)
        x += v.width + 24
    d.line([(40, base), (40, base - int((hi[1] - lo[1]) * PPU + 0.999))], fill=ACCENT, width=2)
    for k in range(int(hi[1] - lo[1] + 0.999) + 1):
        yy = base - k * PPU
        d.line([(34, yy), (46, yy)], fill=ACCENT, width=2)
        d.text((8, yy - 8) if k < 4 else (8, yy - 8), str(k), font=font(13, True), fill=ACCENT)
    tex = Image.open(ASSETS / f"textures/models/thirdparty/ntm/{body_name}.png").convert("RGBA")
    tex = tex.resize((tex.width * 2, tex.height * 2), Image.NEAREST)
    pw = Image.new("RGBA", (tex.width + 8, tex.height + 8), (60, 66, 76, 255))
    pw.alpha_composite(tex, (4, 4))
    img.alpha_composite(pw, (W - pw.width - 18, 18))
    d.text((W - pw.width - 18, 22 + pw.height), "body texture atlas", font=font(13), fill=DIM)
    return img


def main():
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else REPO / "docs/assets/previews/icbm/missiles.png"
    hs = []
    for b, w, _ in TIERS:
        lo, hi = stacked(load(b), load(w))[0].bounds()
        hs.append(int((hi[1] - lo[1]) * PPU) + 100)
    cards = [card(*t, H=h) for t, h in zip(TIERS, hs)]
    pad, head, foot = 24, 90, 56
    sheet = Image.new("RGBA", (cards[0].width + 2 * pad, head + sum(hs) + len(hs) * pad + foot), BG)
    d = ImageDraw.Draw(sheet)
    d.text((pad, 18), "ICBM missiles by tier", font=font(32, True), fill=FG)
    d.text((pad, 60), "Same scale in every view; ruler is one tick per block. The warhead seats on the body's nose; the flying missile uses this same model.", font=font(15), fill=DIM)
    y = head
    for c in cards:
        sheet.alpha_composite(c, (pad, y))
        y += c.height + pad
    d.text((pad, sheet.height - 36), "Attribution: missile and warhead models and textures (c) HBM's Nuclear Tech Mod contributors, GNU LGPL v3; modified.", font=font(14), fill=DIM)
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.convert("RGB").save(out)
    print("wrote", out.resolve(), sheet.size)


if __name__ == "__main__":
    main()
