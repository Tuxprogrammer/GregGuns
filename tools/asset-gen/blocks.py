"""Generate GregGuns block and particle textures from tools/asset-gen/manifest.json.

    python -I -X utf8 tools/asset-gen/blocks.py            # everything, P1 first
    python -I -X utf8 tools/asset-gen/blocks.py --only ammo_press

Outputs
    src/main/resources/assets/gregguns/<manifest path>          PNG (+ .png.mcmeta for animated strips)
    tools/asset-gen/credits/blocks.csv                           provenance rows
    docs/assets/previews/blocks.png, particles.png               labelled contact sheets
    docs/assets/previews/ammo_press_active.gif                   animated Ammo Press preview

Everything is painted from code with seeds derived from the manifest id (blocktex.seed_for), so
re-running reproduces the files. No Techguns art is read or traced; only the footprint numbers
and written briefs in the manifest are used.

Helpers: blocktex.py (canvas), machines.py (GT-style overlays), blockart.py (casings, decor),
fx.py (particle sheets).
"""
from __future__ import annotations

import argparse
import csv
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))   # python -I does not add the script dir

import numpy as np
from PIL import Image, ImageDraw, ImageFont

import blockart as ba
import fx
import machines
from blocktex import Canvas, hx, mcmeta, rng_for, seed_for, strip, upscale

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "src" / "main" / "resources" / "assets" / "gregguns"
PREV = ROOT / "docs" / "assets" / "previews"
CREDITS = ROOT / "tools" / "asset-gen" / "credits" / "blocks.csv"
DATE = "2026-10-18"
PRIO = {"P1": 0, "P2": 1, "P3": 2}

# ---------------------------------------------------------------- registries
CASINGS = {
    "works_casing": ba.works_casing, "ram_housing": ba.ram_housing, "ram_housing_active": ba.ram_housing_active,
    "reaction_casing": ba.reaction_casing, "gantry_casing": ba.gantry_casing, "gantry_beam": ba.gantry_beam,
    "gantry_hoist": ba.gantry_hoist, "repair_cradle_side": ba.repair_cradle_side,
    "repair_cradle_top": ba.repair_cradle_top, "bastion_plating_iron": ba.bastion_iron,
    "bastion_plating_steel": ba.bastion_steel, "bastion_plating_aluminium": ba.bastion_aluminium,
    "bastion_plating_carbon": ba.bastion_carbon,
}

DECOR = {
    "air_marker": ba.air_marker, "bugnestblock": ba.bugnestblock, "bugnestblock_hole": ba.bugnestblock_hole,
    "bugnesteggs": ba.bugnesteggs, "bugnestslimy": ba.bugnestslimy,
    "camo_net": ba.camo_net("forest"), "camo_net_desert": ba.camo_net("desert"), "camo_net_snow": ba.camo_net("snow"),
    "concrete01": ba.concrete_numbered(1), "concrete02": ba.concrete_numbered(2),
    "concrete03": ba.concrete_numbered(3), "concrete04": ba.concrete_numbered(4),
    "concrete_hole": ba.concrete_hole, "concrete_old2_top": ba.concrete_old2_top,
    "concrete_old2_ctmv": ba.concrete_old2_ctmv,
    "controlpanel_front": ba.controlpanel_front, "controlpanel_overlay_front": ba.controlpanel_overlay,
    "controlpanel_side": ba.controlpanel_side, "controlpanel_top": ba.controlpanel_top,
    "door01": ba.door01, "door01_lower": ba.door01_lower, "door01_upper": ba.door01_upper,
    "ladder01_inv": ba.ladder01_inv, "lamp01": ba.lamp01, "lamp02": ba.lamp02,
    "lantern01_side": ba.lantern01_side, "lantern01_top": ba.lantern01_top,
    "lantern02_side": ba.lantern02_side, "lantern02_top": ba.lantern02_top,
    "metal_ladder": ba.metal_ladder, "metal_ladder_carbon": ba.metal_ladder_carbon,
    "metal_ladder_rusty": ba.metal_ladder_rusty, "metal_ladder_shiny": ba.metal_ladder_shiny,
    "metalgrate_dark": ba._grate("dark"), "metalgrate_hazard": ba._grate("hazard"),
    "metalgrate_rusty": ba._grate("rusty"), "metalgrate_rusty2": ba._grate("rusty2"),
    "metalpanel_dark": ba._metalpanel("dark"), "metalpanel_hazard": ba._metalpanel("hazard"),
    "metalpanel_rusty": ba._metalpanel("rusty"), "metalpanel_rusty2": ba._metalpanel("rusty2"),
    "metalpanel_techgrid_v9": ba.techgrid_v9, "sandbags_side": ba.sandbags_side, "sandbags_top": ba.sandbags_top,
    "screen_front": ba.screen_front, "screen_overlay_front": ba.screen_overlay, "screen_side": ba.screen_side,
    "screen_top": ba.screen_top,
}

# connected-texture families: name -> factory(rng)->(base tile, ctm sheet); both files come from one build
CTM = {
    "concrete_r": ba._concrete_frame(True), "concrete_r02": ba._concrete_frame(False),
    "container_red": ba.container_ctm("#a03030", 16), "container_green": ba.container_ctm("#2f6a34", 16),
    "container_blue": ba.container_ctm("#34409a", 16), "container_orange": ba.container_ctm("#c07828", 16),
    "container_scifi1": ba.container_scifi(False), "container_scifi_rusty": ba.container_scifi(True),
    "metalpanel16_largeborder": ba.metalpanel16_largeborder(), "metalpanel2_hazard": ba.metalpanel2_ctm("hazard"),
    "metalpanel2_rusty": ba.metalpanel2_ctm("rusty"), "steelframe_blue": ba.steelframe("blue"),
    "steelframe_dark": ba.steelframe("dark"), "steelframe_scaffold": ba.steelframe("scaffold"),
}
_ctm_cache: dict = {}

METHOD = {
    "machine": "procedural GT-style transparent overlay (numpy/Pillow pixel painter, machines.py); hull shows through",
    "casing": "procedural 16x16 casing panel (blockart.py): tone-noise fill, bevel, rivets, per-machine motif",
    "decor": "procedural decorative block texture (blockart.py): value-noise tone fill with hand-coded motifs",
    "ctm": "procedural connected-texture tile set (blockart.py): base tile plus 2x2 CTM sheet from one interior painter",
    "particle": "procedural FX sprite sheet (fx.py): analytic radial/noise fields through a colour ramp",
}


def build(spec) -> tuple[Image.Image, int | None, str]:
    """Return (image, mcmeta frametime or None, method key) for one manifest entry."""
    i = spec["id"]
    rng = rng_for(i)
    kind = spec["kind"]
    if kind == "particle":
        name = i.split(".", 1)[1]
        np.random.seed(seed_for(i))
        img = fx.PAINTERS[name](rng)
        ft = spec.get("frametime")
        return img, (ft if ft and spec["frames"] > 1 else None), "particle"
    parts = i.split(".")
    if parts[1] == "machine":
        frames = machines.frames_for(parts[2], parts[3], spec["frames"])
        assert len(frames) == spec["frames"], i
        c = strip(frames) if len(frames) > 1 else frames[0]
        return c.image(), spec.get("frametime") if spec["frames"] > 1 else None, "machine"
    name = parts[2]
    if parts[1] == "casing":
        fr = CASINGS[name](rng)
        c = strip(fr) if len(fr) > 1 else fr[0]
        return c.image(), spec.get("frametime") if len(fr) > 1 else None, "casing"
    # decor
    for base, idx in ((name[:-4], 1) if name.endswith("_ctm") else (None, 1), (name, 0)):
        if base in CTM:
            if base not in _ctm_cache:
                _ctm_cache[base] = CTM[base](rng_for("block.decor." + base))
            return _ctm_cache[base][idx].image(), None, "ctm"
    fr = DECOR[name](rng)
    c = strip(fr) if len(fr) > 1 else fr[0]
    return c.image(), spec.get("frametime") if len(fr) > 1 else None, "decor"


def check(spec, img: Image.Image) -> None:
    exp = (spec["width"], spec["height"])
    if img.size != exp:
        raise SystemExit(f"{spec['id']}: produced {img.size}, manifest wants {exp}")


# ---------------------------------------------------------------- previews
def _font(size=11):
    for name in ("arial.ttf", "DejaVuSans.ttf", "consola.ttf"):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            continue
    return ImageFont.load_default()


def _checker(w, h, a="#2a2c30", b="#34373c", s=8):
    im = Image.new("RGB", (w, h), a)
    d = ImageDraw.Draw(im)
    for y in range(0, h, s):
        for x in range(0, w, s):
            if ((x // s) + (y // s)) % 2:
                d.rectangle([x, y, x + s - 1, y + s - 1], fill=b)
    return im


def _show(img: Image.Image, scale: int, backdrop: str, frames: int, vertical_strip: bool, maxw: int):
    """Render an image for the contact sheet: strips become a horizontal row of frames."""
    if vertical_strip and frames > 1:
        fh = img.height // frames
        img = Image.fromarray(np.concatenate([np.asarray(img)[k * fh:(k + 1) * fh] for k in range(frames)], axis=1), "RGBA")
        # 1px gap between frames is drawn by the paste below via padding
    up = img.resize((img.width * scale, img.height * scale), Image.NEAREST)
    if backdrop == "hull":
        bg = Image.new("RGB", up.size, "#8c929c")
    elif backdrop == "black":
        bg = Image.new("RGB", up.size, "#000000")
    else:
        bg = _checker(*up.size)
    bg.paste(up, (0, 0), up)
    if bg.width > maxw:
        bg = bg.resize((maxw, max(1, int(bg.height * maxw / bg.width))), Image.NEAREST)
    return bg


def contact_sheet(items, path: Path, width: int, scale_fn, title: str):
    font = _font(11)
    pad = 10
    rows, x, y, rowh = [], pad, 30, 0
    placed = []
    for label, tile in items:
        w, h = tile.size
        if x + max(w, 175) + pad > width and x > pad:
            x = pad
            y += rowh + pad
            rowh = 0
        placed.append((label, tile, x, y))
        x += max(w, 175) + pad
        rowh = max(rowh, h + 16)
    H = y + rowh + pad
    sheet = Image.new("RGB", (width, H), "#16181c")
    d = ImageDraw.Draw(sheet)
    d.text((pad, 8), title, fill="#e8e8e8", font=_font(13))
    for label, tile, px, py in placed:
        sheet.paste(tile, (px, py + 14))
        d.text((px, py), label, fill="#b8bec8", font=font)
    path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(path, optimize=True)


def make_previews(results):
    blocks, parts = [], []
    for spec, img in results:
        short = spec["id"].replace("block.machine.", "m.").replace("block.casing.", "c.").replace("block.decor.", "d.")
        short = short.replace("overlay_", "ov_")
        short = short.replace("particle.", "")
        label = f"{short} {img.width}x{img.height}"
        fr = spec["frames"]
        if spec["kind"] == "block":
            bd = "hull" if ".machine." in spec["id"] else "checker"
            w = img.width
            sc = 4
            if fr > 1:
                sc = 4 if w <= 16 else 3 if w <= 32 else 2
            tile = _show(img, sc, bd, fr, True, 1900)
            blocks.append((label, tile))
        else:
            frames_info = f" f{fr}" if fr > 1 else ""
            big = max(img.size)
            sc = max(1, min(4, 360 // big)) if big < 360 else 1
            tile = _show(img, sc, "black" if _is_additive(img) else "checker", 1, False, 360)
            if big > 360:
                k = 360 / big
                im2 = img.resize((max(1, int(img.width * k)), max(1, int(img.height * k))), Image.LANCZOS)
                bg = Image.new("RGB", im2.size, "#000000") if _is_additive(img) else _checker(*im2.size)
                bg.paste(im2, (0, 0), im2)
                tile = bg
            parts.append((label + frames_info, tile))
    contact_sheet(blocks, PREV / "blocks.png", 1900, None, "GregGuns blocks: machine overlays shown over a grey hull, 4x nearest")
    contact_sheet(parts, PREV / "particles.png", 1700, None, "GregGuns particles / FX (additive sheets on black, alpha sheets on checker)")


def _is_additive(img: Image.Image) -> bool:
    a = np.asarray(img)[..., 3]
    return bool((a == 255).mean() > 0.97)


def hull_tile(base="#b6bac6") -> Image.Image:
    c = Canvas(16, fill=hx(base))
    from blocktex import shade
    c.bevel(0, 0, 16, 16, shade(hx(base), 1.3), shade(hx(base), 0.55))
    c.frame(1, 1, 15, 15, shade(hx(base), 0.85))
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        c.px(x, y, shade(hx(base), 0.5))
    return c.image()


def make_gif(results):
    got = {s["id"]: im for s, im in results}
    p = "block.machine.ammo_press.overlay_"

    def frames_of(key, n):
        im = got[p + key]
        h = im.height // n
        return [im.crop((0, k * h, im.width, (k + 1) * h)) for k in range(n)]
    front, glowf = frames_of("front_active", 8), frames_of("front_active_glow", 8)
    side, top = frames_of("side_active", 4), frames_of("top_active", 6)
    hull = hull_tile()
    S = 12
    out = []
    # texture animation ticks: front 2/frame x8, side 2 x4, top 3 x6 -> LCM 144 ticks; sample every 2 ticks
    for tick in range(0, 144, 2):
        f = (tick // 2) % 8
        s = (tick // 2) % 4
        t = (tick // 3) % 6
        faces = []
        for layers in ((front[f], glowf[f]), (side[s],), (top[t],)):
            tile = hull.copy().convert("RGBA")
            for L in layers:
                tile.alpha_composite(L)
            faces.append(tile.resize((16 * S, 16 * S), Image.NEAREST))
        W = 3 * 16 * S + 4 * 14
        H = 16 * S + 2 * 14 + 22
        frame = Image.new("RGB", (W, H), "#16181c")
        d = ImageDraw.Draw(frame)
        for k, (face, name) in enumerate(zip(faces, ("front", "side", "top"))):
            x = 14 + k * (16 * S + 14)
            frame.paste(face, (x, 28))
            d.text((x, 10), name + " (active)", fill="#c8ccd4", font=_font(12))
        out.append(frame.quantize(colors=64, method=Image.MEDIANCUT, dither=Image.NONE))
    PREV.mkdir(parents=True, exist_ok=True)
    out[0].save(PREV / "ammo_press_active.gif", save_all=True, append_images=out[1:], duration=100, loop=0,
                optimize=False, disposal=1)


# ---------------------------------------------------------------- verification
GRIDS = {  # particle sheets laid out as grids: (cell, columns, frames)
    "muzzleflash2": (64, 4, 8), "muzzleflash4x4": (64, 4, 10), "explosion": (96, 5, 20), "fireblast": (102, 5, 20),
    "dustclouds02": (85, 3, 9), "largesmoke": (256, 2, 4), "flamethrower": (128, 4, 16), "flamethrower2": (128, 4, 16),
    "flamethrower_a": (128, 4, 16), "alienflare": (128, 4, 14), "blasterflare01": (128, 4, 14),
    "teslaflare01": (128, 4, 14), "laserflare02": (128, 4, 7), "blobs": (128, 4, 16), "splat": (128, 3, 6),
    "blood4x4": (128, 2, 4), "impact": (128, 2, 4), "muzzleflash_blue": (64, 4, 8), "nukebeamflare": (128, 4, 16),
    "sonicwave4x4": (256, 4, 16), "nukebeam": (128, 1, 17),
}


def verify(results) -> list:
    """Frame-border checks. Strips must be square frames stacked exactly; grid particle sheets must have no
    light or opacity on the outer ring of any cell (nothing bleeds into a neighbour under linear filtering)."""
    problems = []
    for spec, img in results:
        fr = spec["frames"]
        a = np.asarray(img)
        if spec["kind"] == "block" and fr > 1:
            if img.height != img.width * fr:
                problems.append(f"{spec['id']}: strip {img.size} is not {fr} square frames")
        if spec["kind"] == "particle":
            name = spec["id"].split(".", 1)[1]
            if name == "nukebeam" and img.size != (128, 128 * 17):
                problems.append(f"{name}: not square frames")
            if name in GRIDS:
                cell, cols, n = GRIDS[name]
                additive = _is_additive(img)
                for k in range(n):
                    x0, y0 = (k % cols) * cell, (k // cols) * cell
                    c = a[y0:y0 + cell, x0:x0 + cell]
                    ring = np.concatenate([c[0], c[-1]] + ([] if name == "nukebeam" else [c[:, 0], c[:, -1]]))
                    worst = int(ring[:, :3].max()) if additive else int(ring[:, 3].max())
                    if worst > (10 if additive else 6):
                        problems.append(f"{name}: frame {k} edge value {worst}")
    return problems


# ---------------------------------------------------------------- main
def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", help="substring filter on the manifest id")
    ap.add_argument("--no-preview", action="store_true")
    args = ap.parse_args()
    manifest = json.loads((ROOT / "tools" / "asset-gen" / "manifest.json").read_text(encoding="utf-8"))
    specs = [s for s in manifest if s["kind"] in ("block", "particle")]
    if args.only:
        specs = [s for s in specs if args.only in s["id"]]
    specs.sort(key=lambda s: (PRIO.get(s["priority"], 9), s["id"]))
    rows, results, counts = [], [], {}
    for spec in specs:
        img, ft, mkey = build(spec)
        check(spec, img)
        out = RES / spec["path"]
        out.parent.mkdir(parents=True, exist_ok=True)
        img.save(out, optimize=True)
        meta = Path(str(out) + ".mcmeta")
        if ft:
            meta.write_text(mcmeta(ft), encoding="utf-8", newline="\n")
        elif meta.exists():
            meta.unlink()
        rows.append([spec["id"], "src/main/resources/assets/gregguns/" + spec["path"], "tools/asset-gen/blocks.py",
                     seed_for(spec["id"]), METHOD[mkey], "MIT", DATE])
        results.append((spec, img))
        counts[spec["priority"]] = counts.get(spec["priority"], 0) + 1
    if not args.only:
        CREDITS.parent.mkdir(parents=True, exist_ok=True)
        with CREDITS.open("w", encoding="utf-8", newline="") as fh:
            w = csv.writer(fh, lineterminator="\n")
            w.writerow(["asset", "path", "generator", "seed", "method", "licence", "date"])
            w.writerows(rows)
        if not args.no_preview:
            make_previews(results)
            make_gif(results)
    for msg in verify(results):
        print("BORDER:", msg)
    print("generated", sum(counts.values()), dict(sorted(counts.items())))


if __name__ == "__main__":
    main()
