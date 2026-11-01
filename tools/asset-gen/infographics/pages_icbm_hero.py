"""The icbm-hero banner: the ICBM module at a glance, for the README and for posting.

Real renders only: a missile standing on Galacticraft's launch pad (the shipped OBJ meshes), the EV radar with its dish,
the five warhead casings at their true relative scale and a blast from the mod's own explosion sheet. The tier chips
run from the first warhead's voltage to the last, read from the warhead dump.

Run on its own:  python tools/asset-gen/infographics/pages_icbm_hero.py
"""

from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import hashlib  # noqa: E402
import math  # noqa: E402

import kit as K  # noqa: E402
import render3d as R  # noqa: E402
import pages_icbm as WH  # noqa: E402
import pages_icbm_defense as DEF  # noqa: E402
import pages_icbm_missile as MIS  # noqa: E402
import thirdparty as TP  # noqa: E402
from kit import chip, url  # noqa: E402
from pages import PAGE_CSS  # noqa: E402

LADDER = ["HV", "EV", "IV", "LuV", "ZPM", "UV", "UHV"]
HI = {"UV": "#2ee6c8", "UHV": "#b6f060"}


def _chip(t: str) -> str:
    return chip(t) if t in K.TIERS else f'<span class="chip" style="background:{HI[t]}">{t}</span>'


def radar_plain(build, dish_at) -> K.Path:
    """The EV radar with its dish, as the game draws it, without the build-guide rings on its hatches."""
    o, t = DEF.NTM / "radar_dish.obj", DEF.NTM_TEX / "radar_dish.png"

    def make():
        scene = R.voxel_scene(build.blocks, build.tex)
        scene.extend(R.obj_scene(o, t), R.tr(*dish_at) @ R.ry(math.radians(-60)))
        return R.trim(R.render(scene, yaw=-32, pitch=24, ppu=58, ss=2, outline=K.RIM), 3)
    sig = hashlib.sha1(o.read_bytes() + t.read_bytes()).hexdigest()[:10]
    return K._cached("hero-radar-ev", dict(k=sorted(map(str, build.blocks.items())), t=sorted(map(str, build.tex.items())),
                                            d=dish_at, sig=sig, v=1), make)


def hero() -> str:
    logo = url(K.REPO / "docs/assets/branding/logo_pixel_256.png")
    pad2 = MIS.missile_on_pad(2, ppu=120)
    pad1 = MIS.missile_on_pad(1, ppu=120)
    ev_b = DEF.radar_build("radar_ev", "Radar (EV)")
    radar = radar_plain(ev_b, DEF.dish_spot(ev_b, "RadarShape"))
    heads = "".join(f'<img class="r" src="{url(WH.warhead(f"warhead_t{t}", ppu=95))}" style="height:{h}px;margin-right:10px">'
                    for t, h in ((1, 70), (2, 90), (3, 128), (4, 150), (5, 150)))
    boom = K.fx_frame("explosion", 3, 1, 5, 460, black_to_alpha=True)
    tiers = sorted({b["tier"] for b in WH.DUMP["blasts"]})
    if tiers != ["T1", "T2", "T3", "T4", "T5"]:
        raise SystemExit(f"warhead tiers changed: {tiers}; update pages_icbm_hero")
    body = f"""<div style="position:relative;height:440px">
<div style="position:absolute;right:120px;bottom:-30px;width:760px;height:380px;background:radial-gradient(ellipse 50% 50% at 50% 60%,rgba(185,205,230,.24),transparent 75%)"></div>
<div style="position:absolute;left:-20px;bottom:-30px;width:700px;height:240px;background:radial-gradient(ellipse 50% 50% at 50% 60%,rgba(185,205,230,.22),transparent 75%)"></div>
<img src="{url(boom)}" style="position:absolute;right:-40px;top:-40px;height:460px;mix-blend-mode:screen;opacity:.85">
<img class="r" src="{url(radar)}" style="position:absolute;right:560px;bottom:0;height:230px">
<img class="r" src="{url(pad1)}" style="position:absolute;right:330px;bottom:0;height:300px">
<img class="r" src="{url(pad2)}" style="position:absolute;right:40px;bottom:0;height:420px">
<div style="position:absolute;left:0;top:10px;display:flex;align-items:center;gap:24px">
<img src="{logo}" style="width:150px;height:150px;image-rendering:pixelated">
<div><h1 style="font-size:96px;line-height:.9;text-transform:uppercase">Greg<b style="color:var(--orange)">Guns</b> ICBM</h1>
<div style="font-size:26px;color:var(--muted);margin-top:8px;max-width:900px">Warheads, missiles, silos and air defense for GT New Horizons</div></div></div>
<div style="position:absolute;left:20px;bottom:56px;display:flex;align-items:flex-end">{heads}</div>
<div style="position:absolute;left:24px;bottom:16px;display:flex;gap:6px">{''.join(_chip(t) for t in LADDER)}</div>
</div>"""
    credit = ("Missile, warhead and radar art derived from HBM's Nuclear Tech Mod (GNU LGPL v3; modified). " + TP.GC_CREDIT)
    return f"""<!doctype html><html><head><meta charset="utf-8"><style>{K.CSS}{PAGE_CSS}</style></head>
<body><div class="sheet" style="--w:1600px;padding:30px 48px 0">{body}
<div style="font-size:12px;color:var(--dim);padding:6px 0 10px;text-align:right">{credit}</div></div></body></html>"""


PAGES = {"icbm-hero": hero}

if __name__ == "__main__":
    import build
    build.main(["icbm-hero"])
