"""The player guide infographics. Run build.py to render them all."""

from __future__ import annotations

import data as D
import gt
import kit as K
import scenes as S
from kit import chip, esc, url

PAGE_CSS = """
.grid { display: grid; gap: 18px; }
.key { display: inline-flex; align-items: center; justify-content: center; min-width: 46px; height: 46px; padding: 0 12px;
  border-radius: 8px; background: linear-gradient(#f2f4f7, #c9ced6); color: #1a1f26; font-family: Bahnschrift, sans-serif;
  font-weight: 700; font-size: 20px; box-shadow: 0 4px 0 #7d8590, 0 6px 10px rgba(0,0,0,.4); }
.ctrl { display: flex; align-items: center; gap: 14px; }
.ctrl > span:not(.key) { color: var(--muted); font-size: 18px; }
.ico { image-rendering: pixelated; vertical-align: middle; }
.flow { display: flex; align-items: center; justify-content: center; gap: 14px; min-height: 170px; border-radius: 12px;
  background: radial-gradient(ellipse 62% 64% at 50% 52%, rgba(190,210,235,.30), rgba(190,210,235,.08) 62%, transparent 78%); }
.flow .flow { background: none; }
.guide { display: flex; align-items: flex-end; justify-content: center; padding: 0 18px; border-radius: 50%;
  background: radial-gradient(ellipse 50% 52% at 50% 56%, rgba(190,210,235,.36), rgba(190,210,235,.10) 62%, transparent 78%); }
.stack { display: flex; flex-direction: column; align-items: center; gap: 4px; }
.stack small { color: var(--muted); font-size: 15px; text-align: center; line-height: 1.2; }
.plus { color: var(--muted); font-size: 30px; font-weight: 300; }
.wcard { background: rgba(255,255,255,.03); border: 1px solid var(--line); border-radius: 12px; padding: 12px 14px 12px;
  display: flex; flex-direction: column; }
.wcard .pic img { max-width: 100%; }
.wcard .pic { background: radial-gradient(ellipse 60% 62% at 50% 52%, rgba(190,210,235,.36), rgba(190,210,235,.09) 62%, transparent 75%); border-radius: 10px; height: 118px; display: flex; align-items: center; justify-content: center; margin-bottom: 6px; position: relative; }
.wcard h3 { font-size: 22px; }
.wcard .role { color: var(--muted); font-size: 16px; line-height: 1.25; margin-top: 2px; }
.wcard .ammo { margin-top: auto; padding-top: 8px; font-size: 15px; color: #c9d1da; display: flex; align-items: center; gap: 7px; }
.band { display: grid; grid-template-columns: 230px 1fr; gap: 20px; padding: 20px; border-radius: 16px; border: 1px solid var(--line);
  background: linear-gradient(90deg, color-mix(in srgb, var(--tc) 13%, transparent), rgba(255,255,255,.015) 40%); }
.band .rail { border-left: 6px solid var(--tc); padding-left: 16px; }
.band .rail .tn { font-family: Bahnschrift, sans-serif; font-weight: 700; font-size: 56px; line-height: 1; color: var(--tc); }
.band .rail .gate { margin-top: 10px; font-size: 16px; color: var(--muted); }
.band .rail .gate b { display: block; color: var(--txt); font-size: 21px; font-weight: 600; }
"""


POINT = {"rightArm": (-95, 20, 0), "leftArm": (0, 0, -6)}


def img(path, h=None, w=None, cls="r", style=""):
    dims = (f"height:{h}px;" if h else "") + (f"width:{w}px;" if w else "")
    return f'<img class="{cls}" src="{url(path)}" style="{dims}object-fit:contain;{style}">'


def slot(src, px, title=""):
    """An icon in an inventory slot; px is the icon size, the slot adds a bevelled border around it."""
    pad = max(3, px // 9)
    return (f'<span class="slot" style="padding:{pad}px" title="{esc(title)}"><img class="ico" src="{url(src)}" '
            f'style="width:{px}px;height:{px}px;display:block"></span>')


def ico(stem, px=40, title=""):
    return slot(K.item(stem, 64), px, title)


def gti(kind, material, px=40, title=""):
    """A GregTech material item icon (foil, round, plate, springSmall, ingot, dust...)."""
    return slot(K.icon(gt.item(kind, material), 64), px, title)


def bubble(speaker, text, side="left", tail=40, style=""):
    return (f'<div class="bubble {side}" style="--tail:{tail}px;{style}"><div class="speaker">{esc(speaker)}</div>'
            f"{text}</div>")


def tier_var(t):
    return f"var(--{t.lower()})"


# ---------------------------------------------------------------- weapon pictures


def weapon_pic(stem: str, h=104, w=270):
    if stem.startswith("item:"):
        return slot(K.item(stem[5:], 64), 76)
    if stem.startswith("akimbo:"):
        g = K.gun(stem[7:], w=w, h=h)
        return (f'<div style="position:relative;width:{w}px;height:{h}px">'
                f'{img(g, h=int(h * .72), style="position:absolute;left:4px;top:0")}'
                f'{img(g, h=int(h * .72), style="position:absolute;right:4px;bottom:0")}</div>')
    if ":" in stem:  # model:texture variant
        model, tex = stem.split(":")
        return img(K._cached(f"gun-{tex}", dict(m=model, t=tex, v=4), lambda: _gun_with_tex(model, tex, w, h)),
                   h=h, w=w)
    return img(K.gun(stem, w=w, h=h), h=h, w=w)


def _gun_with_tex(model, tex, w, h):
    import render3d as R
    s = R.gun_scene(K.export_dir() / f"{model}.json", K.ASSETS / f"textures/models/guns/{tex}.png")
    return R.trim(R.render(s, yaw=18, pitch=16, fit=(w * 2, h * 2), ss=2, outline=K.RIM), 3)


def weapon_card(stem, name, role, ammo, ammo_label):
    if ammo:
        am = ico(ammo, 30)
    elif "charge" in ammo_label:
        am = '<span class="chip" style="background:#2f3a48;color:#cfe3ff">EU</span>'
    else:
        am = '<span class="chip" style="background:#2f3a48;color:#cfd6de">&ndash;</span>'

    return (f'<div class="wcard"><div class="pic">{weapon_pic(stem)}</div><h3>{esc(name)}</h3>'
            f'<div class="role">{esc(role)}</div><div class="ammo">{am}<span>{esc(ammo_label)}</span></div></div>')


def tier_band(tier, cols=4):
    cards = "".join(weapon_card(*w) for w in D.WEAPONS[tier])
    return f"""<div class="band" style="--tc:{tier_var(tier)}">
<div class="rail"><div class="tn">{esc(tier)}</div><div class="gate">Gate material<b>{esc(D.GATES[tier])}</b></div>
<div class="gate">{len(D.WEAPONS[tier])} weapon{'s' if len(D.WEAPONS[tier]) != 1 else ''}</div></div>
<div class="grid" style="grid-template-columns:repeat({cols},minmax(0,1fr))">{cards}</div></div>"""


def arsenal(part: int):
    tiers = ["Steam", "LV", "MV", "HV"] if part == 1 else ["EV", "IV", "LuV", "ZPM"]
    bands = "".join(tier_band(t) for t in tiers)
    if part == 1:
        title = "The <b>arsenal</b> &middot; Steam to HV"
        sub = "Ballistic weapons. Each one is built from GregTech parts and needs its tier's gate material, so a gun arrives exactly when you can make it."
        guide = K.mob("biped", "army_soldier", pose=K.AIM, gun_name="m4", yaw=60, pitch=8, h=330)
        tip = ("Hold <b>Shift</b> over any gun to see its damage type, range, spread, magazine size and armor "
               "penetration. Guns can't go in the offhand, and they don't take enchantments.")
    else:
        title = "The <b>arsenal</b> &middot; EV to ZPM"
        sub = "Energy weapons, heavy ordnance and the endgame. Energy guns are their own batteries: charge them in a GT Charger or Battery Buffer of exactly their tier."
        guide = K.mob("biped", "army_soldier", pose=K.AIM, gun_name="laser_rifle", yaw=60, pitch=8, h=330,
                      armor=("vanilla", K.ASSETS / "textures/models/armor/t3_hev_1.png",
                             K.ASSETS / "textures/models/armor/t3_hev_2.png"))
        tip = ("An EV gun charges only in an <b>EV</b> charger or buffer. Plan your power before you plan your "
               "loadout. Fuel, Bio and Air tank guns refill in a GT Fluid Canner instead. "
               "*Bio Gun blobs only appear when <b>explosions.blockDamage</b> is true (default false). With it off the gun still poisons but leaves no blob, and its tooltip says "
               "\"Blobs disabled: explosions.blockDamage is off\".")
    body = f"""
<div style="display:flex;flex-direction:column;gap:18px">{bands}</div>
<div style="display:flex;align-items:flex-end;gap:26px;margin-top:22px">
  <div class="guide">{img(guide, h=300)}</div>
  {bubble("Sergeant", tip, side="left", tail=60, style="max-width:880px;margin-bottom:90px")}
</div>"""
    return page_doc(title, sub, body)


def page_doc(title, sub, body, width=1600, extra=""):
    return K.page(title, sub, body, width=width, extra_css=PAGE_CSS + extra)


# ---------------------------------------------------------------- getting started


def step(n, title, visual, text):
    return f"""<div class="card" style="display:flex;flex-direction:column;gap:12px">
<div style="display:flex;align-items:center;gap:14px"><span class="num">{n}</span><h2>{esc(title)}</h2></div>
<div class="flow">{visual}</div><div class="muted" style="font-size:18px">{text}</div></div>"""


def getting_started():
    A = K.ASSETS / "textures/models/armor"
    guide = K.mob("biped", "army_soldier", pose=POINT, yaw=-20, pitch=6, h=460, flip=True)
    journey = "".join(
        f'<div class="stack spot" style="flex:1;min-width:0;padding:6px 2px">{img(K.gun(g, w=250, h=100), h=70, style="max-width:100%")}{chip(t)}</div>'
        for g, t in (("handcannon", "Steam"), ("pistol_9mm", "LV"), ("m4", "MV"), ("as50", "HV"),
                     ("laser_rifle", "EV"), ("blaster_rifle", "IV"), ("nether_blaster", "LuV"),
                     ("nuclear_death_ray", "ZPM")))
    arrow = '<span class="arrow">&rarr;</span>'
    s1 = step(1, "Steam age: the Hand Cannon",
              f'{img(K.gun("handcannon", w=300, h=110), h=96)}<span class="plus">+</span>'
              f'<div class="stack">{ico("ammo_stone_bullet", 56)}<small>Stone Bullets</small></div>',
              "Craft a <b>Stone Barrel</b> and a <b>Wooden Stock</b>, then the Hand Cannon. It fires one Stone "
              "Bullet per shot. Make bullets by hand or 24 at a time in a <b>Steam Ammo Press</b>.")
    s2 = step(2, "LV: your first pistol",
              f'<div class="stack">{ico("part_barrel_hardened", 52)}<small>Barrel</small></div><span class="plus">+</span>'
              f'<div class="stack">{ico("part_mechanical_2", 52)}<small>Mechanical Parts</small></div>{arrow}'
              f'{img(K.gun("pistol_9mm", w=200, h=120), h=96)}',
              "Guns are put together from GregTech parts in an <b>Assembler</b>. Each tier needs its gate "
              "material: <b>Steel</b> for LV, Aluminum for MV, Stainless Steel for HV.")
    s3 = step(3, "Build an Ammo Press",
              img(S.machine_block("ammo_press", "LV"), h=160),
              "The press has its own recipe tab in NEI. A press runs every recipe at or below its tier, the "
              "Steam press runs LV recipes too, and higher tiers overclock as usual.")
    s4 = step(4, "Press rounds and magazines",
              f'<div style="display:flex;flex-direction:column;gap:10px;align-items:center">'
              f'<div class="flow" style="min-height:0;gap:8px"><div class="stack">{gti("foil", "Brass", 46)}<small>Brass Foil</small></div>'
              f'<div class="stack">{gti("round", "Lead", 46)}<small>Lead Round</small></div>'
              f'<div class="stack">{gti("dust", "Gunpowder", 46)}<small>Gunpowder</small></div>{arrow}'
              f'<div class="stack">{ico("ammo_9mm_bullet", 52)}<small>16 &times; 9mm</small></div></div>'
              f'<div class="flow" style="min-height:0;gap:8px"><div class="stack">{ico("mag_9mm_pistol_empty", 46)}<small>Empty magazine</small></div>'
              f'<span class="plus">+</span><div class="stack">{ico("ammo_9mm_bullet", 46)}<small>bullets</small></div>{arrow}'
              f'<div class="stack">{ico("mag_9mm_pistol", 54)}<small>Loaded magazine</small></div></div></div>',
              "Brass foil, lead rounds and gunpowder become bullets. Plates, springs and Mechanical Parts become "
              "empty magazines. Fill them in the press, or craft 1 magazine with 3 bullets.")
    s5 = step(5, "Reload and fight",
              f'{img(K.mob("biped", "army_soldier", pose=K.AIM, gun_name="pistol_9mm", yaw=40, pitch=6, h=260), h=180)}'
              f'<div class="stack"><span class="key" style="min-width:64px;height:64px;font-size:30px">R</span>'
              f'<small>Reload</small></div>',
              "Press <b>R</b> to swap in a loaded magazine. The empty one comes back to your inventory, so refill "
              "it and go again. Shoot the soldiers, bandits and mutants for loot.")
    s6 = step(6, "Repair, then climb the tiers",
              f'<div class="stack">{gti("plate", "Steel", 46)}<small>Plates</small></div><span class="plus">+</span>'
              f'{img(S.machine_block("repair_bench", "LV"), h=120)}{arrow}'
              f'<div class="stack">{ico("t1_combat_chestplate", 56)}<small>Good as new</small></div>',
              "Worn armor and guns go back on a <b>Repair Bench</b> of at least their tier, with GT plates and "
              "cloth. Then follow the <b>GregGuns</b> questbook tab up to the next tier.")
    controls = "".join(
        f'<div class="ctrl"><span class="key">{k}</span><span>{esc(v)}</span></div>'
        for k, v in (("LMB", "Fire (hold for auto)"), ("RMB", "Scope, or charge a beam"), ("R", "Reload"),
                     ("N", "Night vision"), ("P", "Ammo pouch"), ("J", "Glider or anti-grav")))
    body = f"""
<div style="display:grid;grid-template-columns:380px 1fr;gap:28px;align-items:end;margin-bottom:24px">
  <div class="guide">{img(guide, h=440)}</div>
  <div style="display:flex;flex-direction:column;gap:26px;padding-bottom:20px">
    {bubble("Sergeant", "Welcome, recruit. Everything in GregGuns is made from <b>GregTech</b> parts on <b>GregTech</b> machines, and it unlocks with <b>GregTech</b> tiers. If you can make Steel, you can make a pistol. Here's how.", side="left", tail=70, style="font-size:22px;max-width:980px")}
    <div class="card" style="padding:16px 20px"><div class="label" style="margin-bottom:8px">The road ahead</div>
      <div style="display:flex;align-items:flex-end;justify-content:space-between;gap:8px">{journey}</div></div>
  </div>
</div>
<div class="grid" style="grid-template-columns:repeat(3,1fr)">{s1}{s2}{s3}{s4}{s5}{s6}</div>
<div class="card" style="margin-top:20px;display:flex;align-items:center;gap:30px;flex-wrap:wrap">
  <div class="label">Default keys</div>{controls}</div>"""
    return page_doc("Getting <b>started</b>", "From a Steam-age hand cannon to a magazine-fed pistol, in six steps.",
                    body)

