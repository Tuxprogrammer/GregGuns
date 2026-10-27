"""I4 art from HBM's Nuclear Tech Mod (LGPL-3.0): detection and defense overlays, casing, SAM model and tool icons.

    python -I tools/asset-gen/icbm/convert_ntm_defense.py <extracted ntm jar dir> <repo root>

The jar dir holds assets/hbm/{models,textures}, taken from the NTM jar at tag 1.0.27_X5846. Items are plain copies
under new names. The SAM model is NTM's anti-ballistic missile, rewritten in Forge's strict grammar, scaled to one
block tall and centered on x = z = 0. Overlays and the casing are composed here from NTM block textures, with
small hand-drawn lamps and sweep marks added in the code below; every face is fully opaque, like the Launch
Control's, and FRONT_ACTIVE / TOP_ACTIVE are four-frame strips.
"""
import shutil
import sys
from pathlib import Path

from PIL import Image

OVERLAY_DIRS = ("radar_hv", "radar_ev", "radar_tower", "air_defense_battery", "emp_tower")
ACCENT = {
    "radar_hv": (60, 255, 80),
    "radar_ev": (70, 220, 255),
    "radar_tower": (255, 176, 40),
    "air_defense_battery": (255, 64, 48),
    "emp_tower": (110, 150, 255),
}
ITEMS = {  # our name: NTM item
    "sam_missile": "missile_anti_ballistic",
    "radar_gun": "designator",
    "remote_detonator": "detonator_multi",
    "defuser": "defuser",
}
SAM_SCALE = 1.0 / 8.5


def tex(jar, name):
    return Image.open(jar / "assets/hbm/textures/blocks" / f"{name}.png").convert("RGBA")


def over(base, top, at=(0, 0)):
    out = base.copy()
    out.alpha_composite(top, at)
    return out


def put(im, pts, col):
    for x, y in pts:
        im.putpixel((x, y), tuple(col) + (255,))


def scale_col(col, k):
    return tuple(max(0, min(255, int(c * k))) for c in col)


def strip(frames):
    out = Image.new("RGBA", (16, 16 * len(frames)))
    for i, f in enumerate(frames):
        out.paste(f, (0, 16 * i))
    return out


def hull(jar):
    """The steel framed panel used for sides and bottoms."""
    return tex(jar, "cm_block_steel")


def leds(im, col, on=True):
    c = col if on else scale_col(col, 0.3)
    put(im, [(3, 13), (4, 13), (11, 13), (12, 13)], c)


def front(jar, name, k):
    """Dark terminal panel with the machine's emblem; k is a brightness 0..1 for the animated marks."""
    col = ACCENT[name]
    panel = tex(jar, "cm_terminal_side")
    if name.startswith("radar"):
        # A screen tinted with the accent, the NTM dish brightened so it reads on it, and pings that grow.
        tint = Image.new("RGBA", (16, 16), scale_col(col, 0.16) + (255,))
        im = panel.copy()
        for x in range(2, 14):
            for y in range(2, 14):
                im.putpixel((x, y), tint.getpixel((x, y)))
        emblem = tex(jar, "machine_radar")
        for x in range(16):
            for y in range(16):
                r, g, b, a = emblem.getpixel((x, y))
                if a:
                    emblem.putpixel((x, y), (min(255, int(r * 1.6)), min(255, int(g * 1.6)), min(255, int(b * 1.6)), a))
        im = over(im, emblem)
        pings = ([(12, 3)], [(12, 3), (13, 5)], [(12, 3), (13, 5), (12, 7)], [(12, 3), (13, 5), (12, 7), (13, 9)])
        put(im, pings[min(3, int(k * 3.99))] if k > 0 else [], col)
        put(im, [(x, 12) for x in range(3, 13)], scale_col(col, 0.5))
        if name == "radar_tower":
            put(im, [(14, y) for y in range(3, 13)], (190, 194, 200))
            put(im, [(14, 2)], col if k > 0.3 else scale_col(col, 0.3))
    elif name == "air_defense_battery":
        im = tex(jar, "machine_satlinker_top")
        for x in range(2, 14):
            for y in range(2, 14):
                im.putpixel((x, y), (44, 46, 52, 255))
        # An upright SAM in the bay: red nose, white body, dark fins.
        put(im, [(8, 2), (7, 3), (8, 3)], col)
        put(im, [(7, y) for y in range(4, 11)] + [(8, y) for y in range(4, 11)], (226, 228, 232))
        put(im, [(8, y) for y in range(4, 11)], (176, 180, 188))
        put(im, [(6, 9), (6, 10), (9, 9), (9, 10), (5, 11), (10, 11)], (120, 124, 132))
        put(im, [(7, 11), (8, 11), (7, 12), (8, 12)], (255, 150, 40) if k > 0.5 else (140, 70, 20))
        lamp = col if k > 0.5 else scale_col(col, 0.25)
        put(im, [(3, 3), (12, 3)], lamp)
    else:  # emp_tower
        im = tex(jar, "bomb_emp_side")
        if k != 1.0:
            for x in range(16):
                for y in range(16):
                    r, g, b, a = im.getpixel((x, y))
                    if r > 150 and g > 150 and b < 120:  # the yellow bolt
                        im.putpixel((x, y), tuple(int(min(255, c * (0.45 + 0.55 * k))) for c in (r, g, b)) + (a,))
    return im


def top(jar, name, on):
    col = ACCENT[name]
    if name == "emp_tower":
        im = tex(jar, "bomb_emp_top")
        if not on:
            return im
        for x in range(16):
            for y in range(16):
                r, g, b, a = im.getpixel((x, y))
                if b > r + 60 and b > 150:  # the blue ring
                    im.putpixel((x, y), (min(255, r + 70), min(255, g + 70), 255, a))
        return im
    im = tex(jar, "cm_port_steel")
    lamp = col if on else scale_col(col, 0.3)
    put(im, [(2, 2), (3, 2), (2, 3), (3, 3)], lamp)
    return im


def side(jar, name):
    im = hull(jar)
    leds(im, ACCENT[name])
    return im


def bottom(jar):
    return tex(jar, "cm_sheet_steel")


def convert_obj(src):
    out = ["# Converted from HBM's Nuclear Tech Mod (LGPL-3.0); scaled and recentered. See CREDITS.md", "g body"]
    vt, vn, faces = [], [], []
    for raw in src.read_text().splitlines():
        p = raw.split()
        if not p:
            continue
        if p[0] == "v":
            out.append("v %.6f %.6f %.6f" % tuple(float(c) * SAM_SCALE for c in p[1:4]))
        elif p[0] == "vt":
            vt.append("vt %.6f %.6f" % (float(p[1]), float(p[2])))
        elif p[0] == "vn":
            vn.append("vn %.6f %.6f %.6f" % tuple(float(x) for x in p[1:4]))
        elif p[0] == "f":
            assert len(p) == 4, "triangles only"
            faces.append("f " + " ".join(p[1:]))
    return "\n".join(out + vt + vn + faces) + "\n"


def casing(jar):
    """Radar casing: framed steel with a white and red band, like an aircraft-warning mast."""
    im = tex(jar, "cm_sheet_steel")
    frame = tex(jar, "cm_block_steel")
    out = frame.copy()
    out.paste(im.crop((2, 2, 14, 14)), (2, 2))
    for y, col in ((5, (205, 208, 212)), (6, (205, 208, 212)), (7, (200, 52, 44)), (8, (200, 52, 44)),
                   (9, (205, 208, 212)), (10, (205, 208, 212))):
        for x in range(2, 14):
            put(out, [(x, y)], col)
    return out


def main(jar, repo):
    jar, repo = Path(jar), Path(repo)
    res = repo / "src/main/resources/assets/gregguns"
    mdl = res / "models/thirdparty/ntm"
    (mdl / "sam_missile.obj").write_text(convert_obj(jar / "assets/hbm/models/missile_abm.obj"))
    tm = res / "textures/models/thirdparty/ntm"
    shutil.copyfile(jar / "assets/hbm/textures/models/missile/missile_abm.png", tm / "sam_missile.png")
    items = res / "textures/items/thirdparty/ntm"
    for ours, theirs in ITEMS.items():
        shutil.copyfile(jar / f"assets/hbm/textures/items/{theirs}.png", items / f"{ours}.png")
    cas = res / "textures/blocks/casings/thirdparty/ntm"
    cas.mkdir(parents=True, exist_ok=True)
    casing(jar).save(cas / "radar_casing.png")
    lic = items
    for f in ("LICENSE.txt", "GPL-3.0.txt"):
        shutil.copyfile(lic / f, cas / f)
    for name in OVERLAY_DIRS:
        dst = res / "textures/blocks/machines/thirdparty/ntm" / name
        dst.mkdir(parents=True, exist_ok=True)
        front(jar, name, 1.0).save(dst / "OVERLAY_FRONT.png")
        strip([front(jar, name, k) for k in (0.0, 0.34, 0.67, 1.0)] if name != "emp_tower"
              else [front(jar, name, k) for k in (1.0, 0.6, 0.3, 0.6)]).save(dst / "OVERLAY_FRONT_ACTIVE.png")
        side(jar, name).save(dst / "OVERLAY_SIDE.png")
        top(jar, name, False).save(dst / "OVERLAY_TOP.png")
        strip([top(jar, name, on) for on in (True, True, False, False)]).save(dst / "OVERLAY_TOP_ACTIVE.png")
        bottom(jar).save(dst / "OVERLAY_BOTTOM.png")
        for n in ("OVERLAY_FRONT_ACTIVE", "OVERLAY_TOP_ACTIVE"):
            (dst / f"{n}.png.mcmeta").write_text('{"animation":{"frametime":10}}\n')
        for f in ("LICENSE.txt", "GPL-3.0.txt"):
            shutil.copyfile(lic / f, dst / f)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
