"""Shared pieces for the player guide infographics: renders on demand, the page shell, and rasterizing.

Every infographic is an HTML page built in Python, laid out with CSS and photographed by Edge through Playwright.
Renders and textures are written to build/infographics/ (git-ignored) and referenced from the page by file URL;
only the finished PNGs are committed, under docs/assets/infographics/.
"""

from __future__ import annotations

import hashlib
import html
import json
import os
import re
import sys
from pathlib import Path

import numpy as np
from PIL import Image

import blocks as B
import render3d as R
import vanilla

REPO = R.REPO
ASSETS = R.ASSETS
OUT_DIR = Path(os.environ.get("GREGGUNS_INFOGRAPHICS_OUT") or REPO / "docs/assets/infographics")
WORK = REPO / "build/infographics"
RENDERS = WORK / "r"
TEXCACHE = WORK / "tex"
ENTITY = ASSETS / "textures/entity"
ITEMS = ASSETS / "textures/items"


def export_dir() -> Path:
    """Gun models exported by ModelExportTest (GREGGUNS_EXPORT_MODELS)."""
    d = os.environ.get("GREGGUNS_EXPORT_MODELS")
    p = Path(d) if d else WORK / "export"
    if not (p / "ak47.json").exists():
        raise SystemExit(f"No exported gun models in {p}. Run:\n  GREGGUNS_EXPORT_MODELS={p} ./gradlew test "
                         "--tests com.gregguns.tools.ModelExportTest")
    return p


def esc(s: str) -> str:
    return html.escape(s, quote=True)


def url(p: Path) -> str:
    return Path(p).resolve().as_uri()


# ---------------------------------------------------------------- render cache


def _cached(tag: str, params: dict, make) -> Path:
    RENDERS.mkdir(parents=True, exist_ok=True)
    h = hashlib.sha1(json.dumps(params, sort_keys=True, default=str).encode()).hexdigest()[:10]
    p = RENDERS / f"{tag}-{h}.png"
    if not p.exists():
        make().save(p)
    return p


# A light rim baked around every model, so dark guns and armor stay legible on the dark sheets. Renders are made at
# about twice their display size, so two pixels here show as about one on the page.
RIM = (214, 224, 238, 150, 2)

AIM = {"rightArm": (-90, -5.73, 0), "leftArm": (-90, 28.65, 0)}
HOLD = {"rightArm": (-28, 0, 0), "leftArm": (-50, 40, 0)}

def _third_person_lengths() -> dict:
    """Each gun's third-person length in world blocks, read from the mod's in-hand sizing table."""
    src = REPO / "src/main/java/com/gregguns/client/render/profile/InHandTargets.java"
    num = r"\s*(-?[\d.]+)"
    rows = re.findall(r'put\("(\w+)",' + ",".join([num] * 8) + r"\)", src.read_text(encoding="utf-8"))
    return {r[0]: float(r[8]) for r in rows}


# How long each gun looks in a mob's hands, in blocks: the mod's own third-person sizes, with a fallback for any gun
# not on its table (the exported models are not to a common scale).
HELD_LENGTH_FALLBACK = {
    "pistol_9mm": 0.55, "revolver": 0.6, "golden_revolver": 0.6, "deatomizer_pistol": 0.6, "mac10": 0.6,
    "handcannon": 0.9, "sawed_off": 0.8, "thompson_smg": 1.05, "pdw": 0.8, "vector": 0.85, "alien_blaster": 0.75,
    "nether_blaster": 0.9, "power_hammer": 1.0, "chainsaw": 1.2, "combat_shotgun": 1.15, "bolt_action_rifle": 1.35,
    "ak47": 1.3, "m4": 1.25, "aug": 1.15, "scar": 1.25, "infiltrator": 1.25, "as50": 1.6, "lmg": 1.45,
    "grenade_launcher": 1.1, "rocket_launcher": 1.4, "nuke_launcher": 1.4, "flamethrower": 1.2, "laser_rifle": 1.25,
    "pulse_rifle": 1.3, "blaster_rifle": 1.25, "tesla_rifle": 1.3, "sonic_rifle": 1.3, "scatter_laser_rifle": 1.35,
    "bio_gun": 1.0, "grim_reaper": 1.3, "nuclear_death_ray": 1.35,
}
HELD_LENGTH = {**HELD_LENGTH_FALLBACK, **_third_person_lengths()}


def export_sig(name: str) -> str:
    """A short hash of an exported model, so renders are redone whenever the model changes."""
    return hashlib.sha1((export_dir() / f"{name}.json").read_bytes()).hexdigest()[:10]


def _held(mats, gun: str, length: float | None = None, grip=(0.3, 0.45)):
    """The gun in the right hand. grip: where the hand holds it, as fractions of its length from the rear and of its
    height from the bottom. The world flip in gun_scene puts the muzzle at -X, so -X runs along the arm."""
    g = R.gun_scene(export_dir() / f"{gun}.json")
    # Measure without a laser sight's beam, which the mod's length table leaves out too.
    mn, mx = R.gun_scene(export_dir() / f"{gun}.json", skip=("beam",)).bounds()
    L = mx[0] - mn[0]
    k = (length or HELD_LENGTH.get(gun, 1.2)) / L
    gp = np.array([mx[0] - grip[0] * L, mn[1] + grip[1] * (mx[1] - mn[1]), (mn[2] + mx[2]) / 2])
    M = mats["rightArm"]
    Rm = M[:3, :3]
    hand = (M @ np.array([-1, 10, 0, 1.0]))[:3]
    bx = Rm @ np.array([0, 1.0, 0])
    bx /= np.linalg.norm(bx)
    uy = Rm @ np.array([0, 0, -1.0])
    uy /= np.linalg.norm(uy)
    Bm = np.eye(4)
    Bm[:3, 0], Bm[:3, 1], Bm[:3, 2] = -bx, uy, np.cross(-bx, uy)
    return g, R.tr(*hand) @ Bm @ R.sc(k) @ R.tr(*(-gp))


def gun(name: str, yaw=18, pitch=16, w=420, h=200, outline=RIM) -> Path:
    def make():
        s = R.gun_scene(export_dir() / f"{name}.json")
        return R.trim(R.render(s, yaw=yaw, pitch=pitch, fit=(w * 2, h * 2), ss=2, outline=outline), 2)
    return _cached(f"gun-{name}", dict(n=name, y=yaw, p=pitch, w=w, h=h, o=outline, m=export_sig(name), v=4), make)


def mob(model: str, skin: str, pose=None, gun_name=None, yaw=-30, pitch=10, h=420, armor=None, accessories=(),
        flip=False) -> Path:
    """A mob or armored figure. armor: ("vanilla", layer1, layer2[, pieces]) or ("custom", set_id[, texture]), or a list of them."""
    def make():
        s, mats = R.mob_scene(model, ENTITY / f"{skin}.png" if not str(skin).endswith(".png") else Path(skin),
                              pose or {})
        for a in (armor if isinstance(armor, list) else [armor] if armor else []):
            if a[0] == "vanilla":
                R.vanilla_armor(s, mats, a[1], a[2], *a[3:])
            else:
                R.custom_armor(s, mats, a[1], a[2] if len(a) > 2 else None)
        for acc in accessories:
            accessory(s, mats, acc)
        if gun_name:
            g, T = _held(mats, gun_name)
            s.extend(g, T)
        im = R.trim(R.render(s, yaw=yaw, pitch=pitch, ppu=h / 2.2, ss=2, outline=RIM), 3)
        return im.transpose(Image.FLIP_LEFT_RIGHT) if flip else im
    return _cached(f"mob-{model}-{Path(str(skin)).stem}",
                   dict(m=model, s=str(skin), p=pose, g=gun_name, y=yaw, pi=pitch, h=h, a=str(armor),
                        acc=list(accessories), f=flip, gm=export_sig(gun_name) if gun_name else None,
                        gl=HELD_LENGTH.get(gun_name), v=8), make)


def bone_skin() -> Path:
    """The Skeleton Soldier's skin: the vanilla skeleton texture it wears in game, read from the vanilla jar."""
    return vanilla.texture("entity/skeleton/skeleton")


def pigman_skin() -> Path:
    """The Zombie Pigman Soldier's skin: the vanilla zombie pigman texture it wears in game, read from the vanilla jar."""
    return vanilla.texture("entity/zombie_pigman")


def accessory(scene, mats, name: str):
    """Exported accessory boxes (masks, packs) on a biped. The export lists each box's part and texture."""
    m = json.loads((export_dir() / "accessories" / f"{name}.json").read_text())
    t = scene.texture(ASSETS / "textures/models/armor" / f"{m['textures'][0]}.png")
    tw, th = m["textureWidth"], m["textureHeight"]
    part = "head" if m.get("mount") == "HEAD" else "body"
    for b in m["boxes"]:
        M = mats[part] @ R.tr(*b.get("rotationPoint", [0, 0, 0])) @ R.rot_zyx(b.get("rotate", [0, 0, 0]))
        scene.quads += R.box_quads(M, b["offset"], b["size"], b["uv"], tw, th, b.get("mirror", False),
                                   b.get("inflate", 0.0), t)


def icon(path: Path, px: int = 64) -> Path:
    """A 16x16 item or block texture scaled up with hard pixels (first frame of an animated strip)."""
    def make():
        im = Image.open(path).convert("RGBA")
        im = im.crop((0, 0, im.width, im.width))
        return im.resize((px, px), Image.NEAREST)
    return _cached(f"icon-{Path(path).stem}", dict(p=str(path), px=px, v=1), make)


def item(name: str, px: int = 64) -> Path:
    return icon(ITEMS / f"{name}.png", px)


def fx_frame(sheet: str, col: int, row: int, grid: int, px: int, black_to_alpha=False) -> Path:
    def make():
        im = Image.open(ASSETS / "textures/fx" / f"{sheet}.png").convert("RGBA")
        cw = im.width // grid
        fr = im.crop((col * cw, row * cw, (col + 1) * cw, (row + 1) * cw)).resize((px, px), Image.BICUBIC)
        if black_to_alpha:
            a = np.array(fr).astype(float)
            lum = a[..., :3].max(-1)
            a[..., 3] = np.minimum(a[..., 3], lum * 1.4)
            fr = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))
        return fr
    return _cached(f"fx-{sheet}", dict(s=sheet, c=col, r=row, g=grid, px=px, b=black_to_alpha, v=1), make)


MARK_HATCH, MARK_CONTROLLER, MARK_LAMP = (255, 138, 31), (56, 198, 217), (255, 235, 60)


def voxels(tag: str, blocks_: dict, textures: dict, yaw=-35, pitch=30, ppu=36, key=None, tags=None) -> Path:
    """tags: {block key: 1 for a hatch, 2 for the controller}; those faces get colored rings where they show."""
    def make():
        marks = {1: MARK_HATCH, 2: MARK_CONTROLLER, 3: MARK_LAMP} if tags else None
        return R.trim(R.render(R.voxel_scene(blocks_, textures, tags), yaw=yaw, pitch=pitch, ppu=ppu, ss=2, outline=RIM,
                               marks=marks), 3)
    params = dict(k=key or sorted(map(str, blocks_.items())), t=sorted(map(str, textures.items())), g=str(tags), y=yaw, p=pitch, u=ppu, v=5)
    return _cached(f"vox-{tag}", params, make)


# ---------------------------------------------------------------- page shell

CSS = """
:root {
  --ink: #0e1217; --ink2: #141a21; --card: #19212b; --card2: #1f2833; --line: #2b3542;
  --txt: #e9edf2; --muted: #b4bfcb; --dim: #97a3b1; --orange: #ff8a1f; --blue: #3d8bff; --cyan: #38c6d9;
  --steam: #b07a3a; --ulv: #8a8f98; --lv: #d0d4da; --mv: #38c6d9; --hv: #ffb020; --ev: #b04fdc; --iv: #4b5bff;
  --luv: #ff6fcf; --zpm: #ff5555;
}
* { box-sizing: border-box; margin: 0; padding: 0; }
body { background: var(--ink); color: var(--txt); font-family: "Segoe UI", "Fira Sans", "Noto Sans", "DejaVu Sans", system-ui, sans-serif; font-size: 19px;
  line-height: 1.38; -webkit-font-smoothing: antialiased; }
.sheet { position: relative; width: var(--w); padding: 44px 48px 30px; overflow: hidden;
  background:
    radial-gradient(1200px 600px at 85% -10%, rgba(61,139,255,.10), transparent 60%),
    radial-gradient(900px 500px at -5% 105%, rgba(255,138,31,.08), transparent 60%),
    var(--ink); }
.sheet::before { content: ""; position: absolute; inset: 0; opacity: .05; pointer-events: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='56' height='97'%3E%3Cpath d='M28 0 56 16v32L28 64 0 48V16zM28 64v33' fill='none' stroke='%23fff' stroke-width='1.5'/%3E%3C/svg%3E"); }
.sheet > * { position: relative; }
h1, h2, h3, .cond { font-family: Bahnschrift, "Fira Sans Condensed", "Segoe UI", "Fira Sans", sans-serif; font-stretch: condensed; letter-spacing: .01em; }
.head { display: flex; align-items: center; gap: 22px; margin-bottom: 26px; }
.head img.logo { width: 84px; height: 84px; image-rendering: pixelated; }
.head h1 { font-size: 54px; line-height: 1; font-weight: 700; text-transform: uppercase; }
.head h1 b { color: var(--orange); font-weight: 700; }
.head .sub { color: var(--muted); font-size: 21px; margin-top: 6px; max-width: 1100px; }
.card { background: linear-gradient(180deg, var(--card2), var(--card)); border: 1px solid var(--line); border-radius: 14px; padding: 20px 22px; }
.label { font-family: Bahnschrift, "Fira Sans Condensed", "Noto Sans", sans-serif; font-weight: 600; font-size: 14px; letter-spacing: .14em; text-transform: uppercase; color: var(--muted); }
h2 { font-size: 30px; font-weight: 700; line-height: 1.1; }
h3 { font-size: 23px; font-weight: 700; line-height: 1.15; }
.muted { color: var(--muted); }
.dim { color: var(--dim); }
.o { color: var(--orange); }
.chip { display: inline-block; font-family: Bahnschrift, "Fira Sans Condensed", "Noto Sans", sans-serif; font-weight: 700; font-size: 15px; padding: 3px 10px 2px;
  border-radius: 6px; color: #0e1217; line-height: 1.25; vertical-align: middle; }
.t-Steam { background: var(--steam); } .t-ULV { background: var(--ulv); } .t-LV { background: var(--lv); }
.t-MV { background: var(--mv); } .t-HV { background: var(--hv); } .t-EV { background: var(--ev); color: #fff; }
.t-IV { background: var(--iv); color: #fff; } .t-LuV { background: var(--luv); } .t-ZPM { background: var(--zpm); }
img.r { display: block; filter: drop-shadow(0 0 9px rgba(165,195,240,.34)) drop-shadow(0 10px 14px rgba(0,0,0,.5)); }
img.px { image-rendering: pixelated; }
/* Item icons sit in a light inventory slot, as players see them in-game, so dark items stay legible. */
.slot { display: inline-flex; align-items: center; justify-content: center; flex: none; vertical-align: middle;
  background: #8b8b8b; box-shadow: inset 2px 2px 0 #373737, inset -2px -2px 0 #ffffff; }
.spot { background: radial-gradient(ellipse 58% 60% at 50% 55%, rgba(190,210,235,.38), rgba(190,210,235,.10) 60%, transparent 76%); border-radius: 12px; }
.foot { display: flex; align-items: center; gap: 12px; margin-top: 26px; color: var(--muted); font-size: 15px; }
.credit { margin-top: 8px; color: var(--dim); font-size: 14px; }
.foot img { width: 26px; height: 26px; image-rendering: pixelated; }
.foot .hex { display: flex; gap: 4px; margin-left: auto; }
.foot .hex i { width: 14px; height: 16px; clip-path: polygon(50% 0,100% 25%,100% 75%,50% 100%,0 75%,0 25%); }
.bubble { position: relative; background: #f3f5f8; color: #151a20; border-radius: 16px; padding: 16px 20px; font-size: 19px;
  box-shadow: 0 10px 24px rgba(0,0,0,.45); }
.bubble b { color: #c25a00; }
.bubble::after { content: ""; position: absolute; width: 26px; height: 26px; background: #f3f5f8; transform: rotate(45deg); }
.bubble.left::after { left: -10px; top: var(--tail, 40px); }
.bubble.right::after { right: -10px; top: var(--tail, 40px); }
.bubble.down::after { bottom: -10px; left: var(--tail, 60px); }
.speaker { font-family: Bahnschrift, "Fira Sans Condensed", "Noto Sans", sans-serif; font-weight: 700; font-size: 14px; letter-spacing: .12em; text-transform: uppercase; color: #6a7380; margin-bottom: 4px; }
.num { display: inline-flex; align-items: center; justify-content: center; width: 44px; height: 44px; border-radius: 50%;
  background: var(--orange); color: #111; font-family: Bahnschrift, "Fira Sans Condensed", "Noto Sans", sans-serif; font-weight: 800; font-size: 26px; flex: none; }
.arrow { color: var(--orange); font-size: 34px; font-weight: 700; }
"""

TIERS = ["Steam", "LV", "MV", "HV", "EV", "IV", "LuV", "ZPM"]


def chip(tier: str, text: str | None = None) -> str:
    return f'<span class="chip t-{tier}">{esc(text or tier)}</span>'


def page(title_html: str, sub: str, body: str, width=1600, extra_css="", credit="", overlay="") -> str:
    """credit: extra footer line(s), a string or a list of strings, for third-party art shown on the sheet. overlay:
    markup laid over the sheet (a stamp)."""
    credits = [credit] if isinstance(credit, str) else list(credit)
    logo = url(REPO / "docs/assets/branding/logo_pixel_128.png")
    hexes = "".join(f'<i style="background:var(--{t.lower()})"></i>' for t in TIERS)
    return f"""<!doctype html><html><head><meta charset="utf-8"><style>{CSS}{extra_css}</style></head>
<body><div class="sheet" style="--w:{width}px">
<div class="head"><img class="logo" src="{logo}"><div><h1>{title_html}</h1><div class="sub">{esc(sub)}</div></div></div>
{body}
<div class="foot"><img src="{logo}"><span><b style="color:#cfd6de">GregGuns</b> &middot; guns, armor and military tech for GT New Horizons (Minecraft 1.7.10)</span><span class="hex">{hexes}</span></div>
{"".join(f'<div class="credit">{esc(c)}</div>' for c in credits if c)}{overlay}
</div></body></html>"""


def launch_browser(p):
    """Edge on Windows (ships with the OS); Playwright's bundled Chromium elsewhere."""
    if sys.platform == "win32":
        return p.chromium.launch(channel="msedge")
    return p.chromium.launch()


def rasterize(jobs: list[tuple[str, str, int]]):
    """jobs: (name, html, width). Writes docs/assets/infographics/<name>.png and keeps the HTML in the work dir."""
    from playwright.sync_api import sync_playwright

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    WORK.mkdir(parents=True, exist_ok=True)
    with sync_playwright() as p:
        browser = launch_browser(p)
        pg = browser.new_page(viewport={"width": 1600, "height": 900}, device_scale_factor=1)
        for name, doc, width in jobs:
            src = WORK / f"{name}.html"
            src.write_text(doc, encoding="utf-8")
            pg.set_viewport_size({"width": width, "height": 900})
            pg.goto(src.as_uri())
            pg.wait_for_load_state("networkidle")
            pg.locator(".sheet").screenshot(path=str(OUT_DIR / f"{name}.png"))
            print("wrote", OUT_DIR / f"{name}.png")
        browser.close()
