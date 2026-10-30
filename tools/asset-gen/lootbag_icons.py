"""Draw the 16x16 icons of the GregGuns loot bags (EnhancedLootBags groups 1701-1712, LootbagIcon gregguns:lootbag/<key>).

    python -I -X utf8 tools/asset-gen/lootbag_icons.py [--sheet]

An olive ammo crate with a band in the GregTech tier color (GTValues.TIER_COLORS: LV dark green, MV gold, HV yellow,
EV dark gray, IV blue, LuV light purple, ZPM aqua; Steam bronze). Spoils of War is a gunmetal crate with a gold band and a
red star; the Ordnance crates add a hazard-striped lid and one to three white pips. Procedural and deterministic; no
reference art. Output: src/main/resources/assets/gregguns/textures/items/lootbag/<key>.png, credit rows in
tools/asset-gen/credits/lootbag_icons.csv, contact sheet docs/assets/previews/lootbag_icons.png with --sheet.
"""
import csv
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from pixel import BRASS, Canvas, ramp, BS, HL, LT, SH, DK  # noqa: E402
import outroot  # noqa: E402

OLIVE_CRATE = ramp((96, 108, 60))
GUNMETAL = ramp((70, 74, 84))
STEEL = ramp((150, 154, 162))
BLACK = (28, 28, 30)
YELLOW = (236, 196, 40)
WHITE = (240, 240, 244)

# Band colors: the Minecraft chat colors GT uses per tier (EV's dark gray is lifted so it reads on olive).
TIERS = {
    'recruit': (205, 127, 50),     # Steam: bronze
    'rifleman': (0, 170, 0),       # LV: dark green
    'sergeant': (255, 170, 0),     # MV: gold
    'lieutenant': (255, 255, 85),  # HV: yellow
    'captain': (128, 128, 136),    # EV: dark gray, lifted
    'major': (85, 85, 255),        # IV: blue
    'colonel': (255, 85, 255),     # LuV: light purple
    'general': (85, 255, 255),     # ZPM: aqua
}
ORDNANCE = {'ordnance_1': ((255, 255, 85), 1), 'ordnance_2': ((85, 85, 255), 2), 'ordnance_3': ((255, 85, 255), 3)}


def crate(body, band):
    """The shared crate: lid, body, rope handles, latch and a band in the given color."""
    c = Canvas()
    c.rect(1, 5, 14, 13, body)            # body
    c.rect(1, 3, 14, 5, body)             # lid
    c.hline(1, 14, 5, body[DK])           # lid seam
    for x in (4, 11):                     # corner planks
        c.vline(x, 6, 13, body[SH])
    b = ramp(band)
    c.rect(1, 8, 14, 10, b)               # tier band
    c.rect(7, 4, 8, 6, BRASS)             # latch
    c.pix(7, 4, BRASS[HL])
    for y in (7, 11):                     # side handles
        c.pix(0, y, STEEL[BS])
        c.pix(15, y, STEEL[BS])
    c.outline()
    return c


def star(c, cx, cy, rp):
    for (dx, dy) in ((0, -2), (0, -1), (-2, 0), (-1, 0), (0, 0), (1, 0), (2, 0), (0, 1), (-1, 2), (1, 2)):
        c.pix(cx + dx, cy + dy, rp[LT] if dy < 0 else rp[BS])


def draw(key):
    if key in TIERS:
        return crate(OLIVE_CRATE, TIERS[key])
    if key == 'spoils':
        c = crate(GUNMETAL, (230, 190, 50))
        star(c, 7, 9, ramp((200, 40, 36)))  # a red star on the gold band
        return c
    band, pips = ORDNANCE[key]
    c = crate(OLIVE_CRATE, band)
    for x in range(1, 15):                # hazard stripes on the lid
        for y in range(3, 5):
            c.pix(x, y, YELLOW if (x + y) % 4 < 2 else BLACK)
    c.rect(7, 4, 8, 6, BRASS)
    for i in range(pips):                 # one pip per Ordnance grade, on the band
        c.pix(3 + 2 * i, 9, WHITE)
    return c


KEYS = list(TIERS) + ['spoils'] + list(ORDNANCE)


def main():
    out = outroot.RES / 'textures' / 'items' / 'lootbag'
    out.mkdir(parents=True, exist_ok=True)
    rows, sheet = [], []
    for key in KEYS:
        c = draw(key)
        c.save(out / (key + '.png'))
        sheet.append((key, c.to_image()))
        rows.append(['icon.lootbag.' + key, 'src/main/resources/assets/gregguns/textures/items/lootbag/%s.png' % key,
                     'tools/asset-gen/lootbag_icons.py', 0,
                     'Procedural 16x16 pixel art (pixel.py crate, GT tier color band, outline pass)', 'MIT',
                     '2026-10-29'])
    outroot.CREDITS_DIR.mkdir(parents=True, exist_ok=True)
    with open(outroot.CREDITS_DIR / 'lootbag_icons.csv', 'w', newline='', encoding='utf-8') as f:
        w = csv.writer(f, lineterminator='\n')
        w.writerow(['asset', 'path', 'generator', 'seed', 'method', 'license', 'date'])
        w.writerows(rows)
    print('rendered %d loot bag icons to %s' % (len(rows), out))
    if '--sheet' in sys.argv:
        from PIL import Image
        scale = 8
        im = Image.new('RGBA', (len(sheet) * (16 * scale + 8), 16 * scale + 8), (46, 48, 54, 255))
        for i, (_, img) in enumerate(sheet):
            im.paste(img.resize((16 * scale, 16 * scale), Image.NEAREST), (i * (16 * scale + 8) + 4, 4))
        outroot.PRE.mkdir(parents=True, exist_ok=True)
        im.save(outroot.PRE / 'lootbag_icons.png')
        print('sheet:', outroot.PRE / 'lootbag_icons.png')


if __name__ == '__main__':
    main()
