"""The later guide pages: ammo, armor, machines, multiblock build guides, enemies, questbook and the README hero."""

from __future__ import annotations

import data as D
import kit as K
import scenes as S
from kit import chip, esc, url
from pages import PAGE_CSS, POINT, arsenal, bubble, getting_started, gti, ico, img, page_doc, slot, tier_var

ARMOR = K.ASSETS / "textures/models/armor"


def vset(name):
    return ("vanilla", ARMOR / f"{name}_1.png", ARMOR / f"{name}_2.png")


def figure(skin="army_soldier", model="biped", **kw):
    return K.mob(model, skin, **kw)


def tex_swatch(path, px=34):
    return slot(K.icon(path, 64), px)


def guide_row(text, pose=None, gun=None, armor=None, flip=False, yaw=55, h=300, speaker="Sergeant"):
    g = figure(pose=pose if pose is not None else K.AIM, gun_name=gun, armor=armor, yaw=yaw, pitch=8, h=h, flip=flip)
    return f"""<div style="display:flex;align-items:flex-end;gap:26px;margin-top:22px"><div class="guide">{img(g, h=int(h * .85))}</div>
{bubble(speaker, text, side="left", tail=50, style="max-width:1000px;margin-bottom:70px")}</div>"""


def muted(text, size=17, top=6):
    return f'<div class="muted" style="font-size:{size}px;margin-top:{top}px">{text}</div>'


# ---------------------------------------------------------------- ammo


def ammo():
    arrow = '<span class="arrow">&rarr;</span>'
    mats = "".join(
        f'<div style="display:flex;align-items:center;gap:10px">{gti(k, m, 30)}{n}</div>'
        for k, m, n in (("ingot", "Brass", "Brass"), ("ingot", "Lead", "Lead"), ("ingot", "Steel", "Steel"),
                        ("dust", "Gunpowder", "Gunpowder"), ("ingot", "Aluminium", "Aluminum"),
                        ("ingot", "StainlessSteel", "Stainless Steel"), ("ingot", "Titanium", "Titanium")))
    parts = "".join(
        f'<div style="display:flex;align-items:center;gap:10px">{gti(k, m, 30)}{n}</div>'
        for k, m, n in (("foil", "Brass", "Brass Foil (small cases)"), ("casingSmall", "Brass", "Brass Casing (large cases)"),
                        ("round", "Lead", "Rounds (the slugs)"), ("plate", "Steel", "Plates"),
                        ("springSmall", "Steel", "Small Springs")))
    gt_machines = "".join(
        f'<div class="stack">{img(S.machine_block(m, "LV", greg=True), h=80)}<small>{n}</small></div>'
        for m, n in (("lathe", "Lathe"), ("bender", "Bending Machine"), ("extruder", "Extruder"),
                     ("assembler", "Assembler")))
    flow = f"""<div class="card" style="display:grid;grid-template-columns:1fr auto 1.15fr auto 1.3fr auto 1.25fr auto .8fr;align-items:center;gap:14px">
<div><div class="label">1 &middot; GregTech materials</div><div style="margin-top:10px;display:flex;flex-direction:column;gap:6px;font-size:17px">{mats}</div></div>{arrow}
<div><div class="label">2 &middot; GregTech parts</div><div style="margin-top:10px;font-size:17px;display:flex;flex-direction:column;gap:6px">{parts}
<div style="display:flex;align-items:center;gap:10px">{ico("part_mechanical_2", 30)}Mechanical Parts</div></div>
<div style="display:grid;grid-template-columns:1fr 1fr;gap:6px 8px;margin-top:12px">{gt_machines}</div></div>{arrow}
<div style="text-align:center"><div class="label">3 &middot; Ammo Press</div>
<div style="display:flex;align-items:flex-end;justify-content:center;gap:10px;margin-top:8px">{img(S.machine_block("ammo_press", "LV"), h=120)}{img(S.ammo_works(4).render(ppu=14), h=110)}</div>
<div class="muted" style="font-size:15px">or the Ammunition Works, for bulk</div></div>{arrow}
<div style="text-align:center"><div class="label">4 &middot; Rounds and magazines</div>
<div style="display:flex;align-items:center;justify-content:center;gap:10px;margin-top:14px">
<div class="stack">{ico("ammo_9mm_bullet", 54)}<small>bullets</small></div><span class="plus">+</span>
<div class="stack">{ico("mag_9mm_pistol_empty", 54)}<small>empty</small></div>{arrow}
<div class="stack">{ico("mag_9mm_pistol", 60)}<small>loaded</small></div></div></div>{arrow}
<div style="text-align:center"><div class="label">5 &middot; Reload</div>{img(K.gun("pistol_9mm", w=200, h=120), h=90, style="margin:12px auto 0")}
<div style="margin-top:10px"><span class="key">R</span></div></div></div>"""
    mags = "".join(f"""<div class="wcard" style="align-items:center;text-align:center">
<div style="display:flex;align-items:center;height:100px">{ico(stem, 70)}</div><h3 style="font-size:19px">{esc(name)}</h3>
<div style="display:flex;align-items:center;justify-content:center;gap:6px;margin:6px 0;color:#c9d1da;font-size:15px"><b style="font-size:18px">{n}</b>&times;{ico(r, 28)}<span>fills it</span></div>
<div class="o cond" style="font-size:24px;font-weight:700">{held}<span class="muted" style="font-size:15px;font-weight:400"> rounds</span></div>
<div class="role" style="font-size:14px">{esc(used)}</div></div>""" for stem, name, n, r, held, used in D.MAGAZINES)
    tiers = "".join(f"""<div style="display:flex;flex-direction:column;gap:8px;align-items:flex-start">{chip(t)}
<div style="display:flex;flex-wrap:wrap;gap:4px">{''.join(ico(i, 46, n) for i, n in items)}</div>
<div class="muted" style="font-size:15px;line-height:1.25">{esc(', '.join(n for _, n in items))}</div></div>""" for t, items in D.AMMO_BY_TIER)
    side = f"""<div class="grid" style="grid-template-columns:repeat(4,minmax(0,1fr));margin-top:18px">
<div class="card"><h2>Energy weapons</h2><div style="display:flex;align-items:center;gap:12px;margin:10px 0">{img(K.gun("laser_rifle", w=260, h=110), h=80)}{chip("EV")}</div>
{muted("No cells to craft: the gun is the battery. Charge it in a GT <b style='color:var(--txt)'>Charger</b> or <b style='color:var(--txt)'>Battery Buffer</b> of exactly the gun's tier. A wrong-tier charger won't touch it.")}</div>
<div class="card"><h2>Fluid tanks</h2><div style="display:flex;align-items:center;gap:12px;margin:10px 0">{ico("tank_fuel", 58)}{ico("tank_bio", 58)}{ico("tank_air", 58)}</div>
{muted("Fill <b style='color:var(--txt)'>Fuel</b> (diesel, gasoline, ethanol), <b style='color:var(--txt)'>Bio</b> (biomass) and <b style='color:var(--txt)'>Compressed Air</b> tanks in a GT Fluid Canner. Empties come back to you.")}</div>
<div class="card"><h2>Loose-round guns</h2><div style="display:flex;align-items:center;gap:10px;margin:10px 0">{img(K.gun("revolver", w=200, h=100), h=70)}{img(K.gun("combat_shotgun", w=260, h=100), h=64)}</div>
{muted("Revolvers, shotguns, the Bolt Action Rifle and the launchers take rounds straight from your inventory. No magazines needed.")}</div>
<div class="card"><h2>Endgame ammo</h2><div style="display:flex;align-items:center;gap:12px;margin:10px 0">{ico("ammo_nether_charge", 58)}{chip("LuV")}{ico("ammo_rocket_nuke", 58)}{chip("ZPM")}</div>
{muted("Nether Charges and Tactical Nuke Rockets are too big for the press. Build them on the GT <b style='color:var(--txt)'>Assembly Line</b>.")}</div></div>"""
    body = f"""{flow}
<h2 style="margin:26px 0 12px">Magazines</h2>
<div class="grid" style="grid-template-columns:repeat(7,minmax(0,1fr))">{mags}</div>
<div class="card" style="margin-top:18px"><div class="label" style="margin-bottom:12px">What to stock at each tier</div>
<div class="grid" style="grid-template-columns:repeat(7,minmax(0,1fr));gap:14px">{tiers}</div></div>
{side}
{guide_row("Sneak and right-click a magazine to empty it back into loose rounds. Press <b>R</b> early to swap out a half-used magazine; the leftover rounds come back too.", gun="pistol_9mm")}"""
    return page_doc("Ammo and <b>magazines</b>",
                    "Bullets are a production line. Feed GregTech parts into an Ammo Press, load magazines, and keep the line running.",
                    body)


# ---------------------------------------------------------------- armor and gear

ARMOR_SETS = [
    ("Steam", "Bandit set", "Faster, jumps higher; light protection", dict(armor=vset("t1_scout"))),
    ("Steam", "Miner set", "Faster, and digs faster", dict(armor=vset("t1_miner"))),
    ("LV", "Soldier set", "Solid bullet protection, resists knockback", dict(armor=vset("t1_combat"))),
    ("MV", "Combat set", "Faster and tougher, in many camo patterns", dict(armor=vset("t2_combat"))),
    ("MV", "Commando set", "Better aim, digs fast underwater", dict(armor=vset("t2_commando"))),
    ("MV", "Ranger Veteran", "Best at MV; shrugs off poison and radiation", dict(armor=("custom", "t2_riot"))),
    ("EV", "Advanced Combat", "High protection, resists armor piercing", dict(armor=vset("t3_combat"))),
    ("EV", "HEV suit", "Powered: fireproof, hazard-proof, big jumps", dict(armor=vset("t3_hev"))),
    ("EV", "Steam armor", "Burns compressed air for extra hearts", dict(armor=("custom", "steam"))),
    ("IV", "Power Armor", "Powered: the toughest set, extra hearts", dict(armor=("custom", "t3_power"))),
    ("IV", "Exo Suit", "Powered: fast, light, soft landings", dict(armor=("custom", "t3_exo"))),
    ("MV", "Beret", "A hat with style, and 10% more speed", dict(armor=("custom", "t2_beret"))),
]

GEAR = [
    ("MV", "Night Vision Goggles", "Head slot, electric. Toggle with N.", dict(accessories=["night_vision_goggles"], yaw=-25)),
    ("MV", "Gas Mask", "Blocks gas hazards, cures poison while the filter lasts.", dict(accessories=["gas_mask"], yaw=-25)),
    ("HV", "Tactical Mask", "Gas mask, night vision and oxygen in one.", dict(accessories=["tactical_mask"], yaw=-25)),
    ("HV", "Oxygen Mask and Tanks", "Breathe in space. Refill the tanks with compressed air.",
     dict(accessories=["oxygen_mask", "oxygen_tanks"], yaw=150)),
    ("HV", "Glider", "Back slot. Hold J or sneak while falling to glide.", dict(accessories=["glider"], yaw=150)),
    ("HV", "Jump Pack", "Back slot, electric. Big jumps, soft landings.", dict(accessories=["jump_pack"], yaw=150)),
    ("IV", "Anti Gravity Device", "Back slot, electric. Press J, then fly.", dict(accessories=["anti_gravity_pack"], yaw=150)),
]

DAMAGE = [("Physical", "melee, chainsaw, hammers"), ("Projectile", "bullets and pellets"), ("Fire", "flamethrower, blasters"),
          ("Explosion", "rockets, grenades, nukes"), ("Energy", "lasers and blasters"), ("Poison", "Bio Gun"),
          ("Lightning", "Tesla Rifle"), ("Radiation", "Nuclear Death Ray"), ("Ice", "resisted by some armor"),
          ("Unresistable", "ignores armor")]


def armor():
    sets = "".join(f"""<div class="wcard" style="align-items:center;text-align:center;padding-top:16px">
<div class="spot" style="width:100%;display:flex;justify-content:center;padding:6px 0">{img(figure(yaw=-28, pitch=8, h=320, **kw), h=230)}</div>
<div style="margin-top:10px">{chip(t)}</div><h3 style="margin-top:6px">{esc(n)}</h3><div class="role">{esc(d)}</div></div>"""
                   for t, n, d, kw in ARMOR_SETS)
    gear = "".join(f"""<div class="wcard" style="align-items:center;text-align:center">
<div class="spot" style="width:100%;display:flex;justify-content:center;padding:6px 0">{img(figure(pitch=8, h=300, **kw), h=180)}</div>
<div style="margin-top:8px">{chip(t)}</div><h3 style="margin-top:6px;font-size:20px">{esc(n)}</h3><div class="role">{esc(d)}</div></div>"""
                   for t, n, d, kw in GEAR)
    key_p = '<span class="key" style="min-width:32px;height:32px;font-size:16px">P</span>'
    extras = f"""<div class="grid" style="grid-template-columns:repeat(3,minmax(0,1fr));margin-top:18px">
<div class="card" style="display:flex;gap:16px;align-items:center">{ico("ammo_pouch", 72)}<div><h3>Ammo Pouch</h3>{muted("Quiver slot. Holds six stacks of ammo; open it with " + key_p)}</div></div>
<div class="card" style="display:flex;gap:16px;align-items:center">{ico("bandages", 64)}{ico("medkit", 64)}<div><h3>Bandages and Medkit</h3>{muted("Heal on the spot, with Regeneration. Bandages at LV, the Medkit at MV.")}</div></div>
<div class="card" style="display:flex;gap:16px;align-items:center">{img(S.machine_block("camo_bench", "LV"), h=90)}<div><h3>Camo Bench</h3>{muted("Cycles the camo on your gun and all four armor pieces. No power needed.")}</div></div></div>"""
    dmg = "".join(f'<div><b style="font-size:17px">{esc(n)}</b><div class="muted" style="font-size:15px">{esc(d)}</div></div>'
                  for n, d in DAMAGE)
    body = f"""<div class="grid" style="grid-template-columns:repeat(6,minmax(0,1fr))">{sets}</div>
<h2 style="margin:26px 0 12px">Masks, goggles and back gear</h2>
<div class="grid" style="grid-template-columns:repeat(7,minmax(0,1fr))">{gear}</div>
{extras}
<div class="card" style="margin-top:18px"><div class="label" style="margin-bottom:10px">Ten damage types. Armor resists each one separately.</div>
<div class="grid" style="grid-template-columns:repeat(5,minmax(0,1fr));gap:12px 20px">{dmg}</div></div>
{guide_row("Worn-out armor protects nothing until it's repaired, so keep a <b>Repair Bench</b> handy. Powered perks need a charged chestplate; the base protection never does.", gun="scar", armor=vset("t2_combat"))}"""
    return page_doc("Armor and <b>gear</b>",
                    "Twelve armor sets from Steam to IV, plus masks, goggles and back gear for the equipment slots GTNH already has.",
                    body)


# ---------------------------------------------------------------- machines

MACHINE_ROWS = [
    ("Ammo Press", lambda: S.machine_block("ammo_press", "LV"), ["Steam", "LV", "MV", "HV", "EV"],
     "Presses bullets, shells, rockets and magazines from GT parts. Its own recipe tab in NEI.",
     "Ammunition Works", lambda: S.ammo_works(4).render(ppu=16), "HV",
     "A production hall. Every press bay you add multiplies how much it makes at once."),
    ("Reaction Chamber", lambda: S.machine_block("reaction_chamber", "EV"), ["EV", "IV"],
     "Holds a drifting reaction steady to make Laser Focuses and Anti Gravity Cores. A failed run loses the input.",
     "Retort", lambda: S.retort(3).render(ppu=22), "EV",
     "A glass flask on stilts. Each bulb layer adds two parallel reactions."),
    ("Repair Bench", lambda: S.machine_block("repair_bench", "LV"), ["LV", "MV", "HV", "EV", "IV"],
     "Restores worn armor and guns with GT plates and cloth. Its tier must be at least the item's.",
     "Armory Gantry", lambda: S.gantry(4).render(ppu=18), "HV",
     "Repairs from buses, one item per bay per cycle, keeping camo, charge and enchantments."),
    ("Turret", lambda: S.turret("steel", "m4"), ["MV", "HV", "EV", "IV"],
     "Give it a gun, ammo and an armor plate. It guards your base and rebuilds itself after a hit.",
     "Rampart", lambda: S.rampart(6).render(ppu=16), "HV",
     "A battlement of up to ten turrets on one shared ammo and power feed."),
]


def machines():
    rows = []
    for name, single, tiers, does, mname, multi, ctier, mdoes in MACHINE_ROWS:
        chips = " ".join(chip(t) for t in tiers)
        rows.append(f"""<div class="card" style="display:grid;grid-template-columns:200px 1fr 60px 380px 1fr;gap:20px;align-items:center">
<div class="spot" style="display:flex;justify-content:center;padding:8px 0">{img(single(), h=170, w=190)}</div>
<div><h2>{esc(name)}</h2><div style="margin:8px 0">{chips}</div>{muted(esc(does), top=0)}</div>
<div class="arrow" style="text-align:center;font-size:44px">&rarr;</div>
<div class="spot" style="display:flex;justify-content:center;padding:8px 0">{img(multi(), h=190, w=370)}</div>
<div><h2>{esc(mname)} <span class="label" style="color:var(--orange)">multiblock</span></h2><div style="margin:8px 0">Controller {chip(ctier)}</div>
{muted(esc(mdoes), top=0)}</div></div>""")
    others = f"""<div class="grid" style="grid-template-columns:repeat(3,minmax(0,1fr));margin-top:18px">
<div class="card" style="display:flex;gap:16px;align-items:center">{img(S.machine_block("camo_bench", "LV"), h=100)}<div><h3>Camo Bench {chip("LV")}</h3>{muted("Swaps camo patterns on your gun and armor. One click per piece, so it never needs a bigger version.")}</div></div>
<div class="card"><h3>Explosive Charges {chip("MV")} {chip("HV")}</h3>{muted("Stick one on a wall, set depth and fuse, arm it and step back. It bores a clean tunnel and drops the blocks as items. The Advanced charge cuts obsidian.")}</div>
<div class="card"><h3>Storage and camp gear</h3>{muted("Weapon and Reinforced Chests to store your kit. Break an enemy camp's flag and an Attack Helicopter comes looking for you.")}</div></div>"""
    body = f"""<div style="display:flex;flex-direction:column;gap:16px">{''.join(rows)}</div>{others}
{guide_row("Start with the single blocks. Once you're running a stack of them all day, build the multiblock: same recipes, many at once.", pose=POINT, flip=True, yaw=-20)}"""
    return page_doc("Machines",
                    "Every GregGuns machine starts as a single block. The ones you'll run all day also come as a multiblock.",
                    body)


# ---------------------------------------------------------------- multiblock build guides

MB = {
    "ammunition-works": dict(
        title="Ammunition <b>Works</b>",
        sub="The Ammo Press, built as a production hall. It runs every Ammo Press recipe, many at once.",
        build=lambda: S.ammo_works(4), view=dict(yaw=-38, pitch=28, ppu=30), back=dict(yaw=148, pitch=28, ppu=16),
        pieces=[("FEEDER", "Feeder", "once, at the front"), ("BAY", "Press bay", "2 to 8 in a row"),
                ("STATION", "Packing station", "once, at the back")],
        piece=lambda f: S.ammo_works(only=f), piece_view=dict(yaw=-38, pitch=28, ppu=18),
        size="5 wide &times; 7 tall &times; 13 to 31 long", ctier="HV",
        hatches=[("Input Bus and Input Hatch", "on the feeder; the two towers on its roof are a handy spot"),
                 ("Energy, Maintenance and Muffler Hatches", "anywhere on the feeder. It does need a Muffler"),
                 ("Output Bus and Output Hatch", "on the packing station's back wall"),
                 ("The belt", "Item Pipe Casings down the middle of the floor, all of one tier")],
        grow=[("More press bays", "Parallels = 4 &times; bays &times; voltage tier. At HV, 4 bays run 48 at once and 8 bays run 96."),
              ("A faster belt", "A higher-tier Item Pipe Casing belt shortens every run."),
              ("Bigger hatches", "Power is the real limit. Feed a long hall with more amps or a higher-tier Energy Hatch.")],
        tip="Hold a hologram projector with a stack of 2 to 8 to preview the hall at that many bays. A screwdriver toggles the ram animation."),
    "retort": dict(
        title="The <b>Retort</b>",
        sub="The Reaction Chamber, built as a glass flask on stilts. It runs Reaction Chamber recipes in parallel.",
        build=lambda: S.retort(3), view=dict(yaw=-38, pitch=24, ppu=40), back=None,
        pieces=[("BASE", "Base and feet", "once, at the bottom"), ("BULB", "Bulb layer", "2 to 6 stacked"),
                ("TOP", "Shoulder, neck and cap", "once, on top")],
        piece=lambda f: S.retort(only=f), piece_view=dict(yaw=-38, pitch=26, ppu=26),
        size="5 &times; 5, from 9 to 13 tall", ctier="EV",
        hatches=[("Input Bus", "on the floor ring. It holds the Heat Ray Focus and the inputs"),
                 ("Output Bus, Input Hatch, Energy and Maintenance", "on the floor ring"),
                 ("Muffler Hatch", "on the cap at the very top"),
                 ("Energy Hatch tier", "EV or better, up to ZPM")],
        grow=[("More bulb layers", "Each layer adds two parallel reactions: 4 at the smallest, 12 at the biggest."),
              ("Better glass", "The glass is the pressure rating. Its lowest tier must reach the recipe's tier."),
              ("One shared reaction", "All parallels share one intensity, so a failure ruins the batch. A big flask is no more explosive than a small one.")],
        tip="The glass doesn't all have to match, but only the weakest pane counts. Upgrade the whole flask together."),
    "armory-gantry": dict(
        title="Armory <b>Gantry</b>",
        sub="The Repair Bench, built as a row of repair arches. It mends armor and guns from buses, ready for automation.",
        build=lambda: S.gantry(4), view=dict(yaw=-38, pitch=26, ppu=34), back=None,
        pieces=[("HEAD", "Booth", "once, at the front"), ("BAY", "Repair bay", "2 to 8 in a row"),
                ("TAIL", "Back wall", "once, at the back")],
        piece=lambda f: S.gantry(only=f), piece_view=dict(yaw=-38, pitch=26, ppu=22),
        size="7 wide &times; 7 tall &times; 7 to 19 long", ctier="HV",
        hatches=[("Input Bus", "damaged items and repair parts go in"),
                 ("Output Bus", "repaired items come out"),
                 ("Energy and Maintenance Hatches", "on any Gantry Casing of the booth or the back wall"),
                 ("No Muffler", "it makes no pollution")],
        grow=[("More bays", "One item per bay per cycle. A cycle takes about 2 seconds, plus up to 8 for badly damaged items."),
              ("A higher Energy Hatch", "The weakest Energy Hatch sets the highest item tier it repairs. LuV and ZPM gear needs a LuV or ZPM hatch."),
              ("Nothing lost", "Repaired items keep their camo, charge, enchantments and names.")],
        tip="Hook it up to an ME system and it becomes a repair service for your whole base."),
    "rampart": dict(
        title="The <b>Rampart</b>",
        sub="The Turret, built as a battlement. Up to ten turrets share one ammo feed and one power feed.",
        build=lambda: S.rampart(6), view=dict(yaw=-32, pitch=24, ppu=34), back=None,
        pieces=[("GATE", "Gatehouse", "once, at one end"), ("BAY", "Wall bay with a mount", "2 to 10 in a row"),
                ("CAP", "End cap", "once, at the other end")],
        piece=lambda f: S.rampart(only=f), piece_view=dict(yaw=-32, pitch=24, ppu=26),
        size="6 tall &times; 3 deep &times; 8 to 24 long", ctier="HV",
        hatches=[("Input Bus", "the shared ammo store for every mount (ME works)"),
                 ("Output Bus", "empty magazines come back here"),
                 ("Energy and Maintenance Hatches", "on the gatehouse plating"),
                 ("Gun Mount Hatch", "exactly one on each bay's sill. Right-click it with a gun to arm it")],
        grow=[("More mounts", "Another turret every two blocks of wall, on the same feed."),
              ("Better plating", "Every mount gets the armor of the weakest Bastion Plating used: Iron 5, Steel 10, Aluminum 15, Carbon 20."),
              ("Security", "A screwdriver cycles who it spares: everyone, your friends, or only you.")],
        tip="A mount takes any GregGuns gun up to its own tier, except akimbo pairs. Right-click it empty-handed to take the gun back."),
}


def multiblock(key):
    m = MB[key]
    b = m["build"]()
    hero = b.render(**m["view"], tag="hero", marks=True)
    legend = "".join(f"""<div style="display:grid;grid-template-columns:48px 1fr auto;gap:12px;align-items:center;padding:7px 0;border-bottom:1px solid var(--line)">
{tex_swatch(t)}<div><b>{esc(label)}</b>{f'<div class="muted" style="font-size:15px">{esc(note)}</div>' if note else ''}</div>
<div class="cond o" style="font-size:24px;font-weight:700">{n}</div></div>""" for _, label, t, n, note in b.legend())
    hatches = "".join(f'<div style="margin:8px 0"><b>{esc(h)}</b>{muted(esc(w), 16, 0)}</div>' for h, w in m["hatches"])
    pieces = "".join(f"""<div class="wcard" style="align-items:center;text-align:center">
<div class="spot" style="height:230px;width:100%;display:flex;align-items:center;justify-content:center">{img(m["piece"](f).render(**m["piece_view"], tag="piece"), h=200, style="max-width:100%")}</div>
<h3>{esc(n)}</h3><div class="o cond" style="font-weight:700;font-size:19px">{esc(c)}</div></div>""" for f, n, c in m["pieces"])
    grow = "".join(f'<div style="margin-bottom:12px"><h3 style="font-size:21px">{esc(h)}</h3>{muted(t, 17, 2)}</div>'
                   for h, t in m["grow"])
    back = ""
    if m["back"]:
        back = (f'<div style="position:absolute;right:16px;bottom:12px;text-align:center">'
                f'{img(b.render(**m["back"], tag="back", marks=True), h=150)}<div class="muted" style="font-size:15px">rear: the packing station</div></div>')
    body = f"""<div style="display:grid;grid-template-columns:1fr 470px;gap:20px">
<div class="card" style="position:relative;display:flex;flex-direction:column;min-height:620px">
<div style="display:flex;gap:28px;align-items:baseline;flex-wrap:wrap"><span class="label">Default size shown</span><span style="font-size:18px">{m["size"]}</span><span>Controller {chip(m["ctier"])}</span>
<span style="font-size:16px;margin-left:auto"><b style="color:#38c6d9">&#9632;</b> controller &nbsp; <b style="color:var(--orange)">&#9632;</b> hatches</span></div>
<div class="spot" style="flex:1;display:flex;align-items:center;justify-content:center;padding:10px 0;margin-top:8px">{img(hero, h=500, style="max-width:100%")}</div>{back}</div>
<div class="card"><div class="label" style="margin-bottom:6px">Blocks for the size shown</div>{legend}
<div class="muted" style="font-size:15px;margin-top:8px">Hatches can go on any casing spot that allows them. The picture shows one layout.</div></div></div>
<h2 style="margin:24px 0 12px">Build it in sections</h2>
<div class="grid" style="grid-template-columns:repeat(3,minmax(0,1fr))">{pieces}</div>
<div class="grid" style="grid-template-columns:1fr 1fr;margin-top:18px">
<div class="card"><h2 style="margin-bottom:6px">Hatches</h2>{hatches}</div>
<div class="card"><h2 style="margin-bottom:10px">Make it bigger</h2>{grow}</div></div>
{guide_row(m["tip"], pose=POINT, flip=True, yaw=-20)}"""
    return page_doc(m["title"], m["sub"], body)


# ---------------------------------------------------------------- enemies


def battle_scene(height=460):
    soldier = figure(pose=K.AIM, gun_name="ak47", yaw=55, pitch=6, h=340)
    commando = figure(pose=K.AIM, gun_name="m4", yaw=45, pitch=6, h=300, armor=vset("t2_commando"))
    mutant = K.mob("super_mutant", "super_mutant_2", pose=K.AIM, gun_name="lmg", yaw=-50, pitch=6, h=330)
    demon = K.mob("cyberdemon", "cyberdemon", yaw=-35, pitch=6, h=400)
    heli = K.mob("attack_helicopter", "attack_helicopter", yaw=-120, pitch=14, h=170)
    boom = K.fx_frame("explosion", 3, 1, 5, 360, black_to_alpha=True)
    boom2 = K.fx_frame("explosion", 1, 1, 5, 220, black_to_alpha=True)
    return f"""<div style="position:relative;height:{height}px;border-radius:16px;overflow:hidden;border:1px solid var(--line);
background:radial-gradient(ellipse 70% 80% at 50% 45%,rgba(255,170,90,.28),transparent 70%),linear-gradient(180deg,#3d3350 0%,#93613f 52%,#7a6a55 53%,#5a4d3e 100%)">
<div style="position:absolute;inset:auto 0 0 0;height:48%;background:repeating-linear-gradient(90deg,rgba(255,255,255,.025) 0 32px,transparent 32px 64px)"></div>
<img src="{url(heli)}" style="position:absolute;right:300px;top:18px;height:120px;filter:brightness(.8)">
<img src="{url(boom)}" style="position:absolute;left:640px;top:70px;height:330px;mix-blend-mode:screen">
<img src="{url(boom2)}" style="position:absolute;right:120px;top:30px;height:200px;mix-blend-mode:screen;opacity:.8">
<img class="r" src="{url(demon)}" style="position:absolute;right:30px;bottom:6px;height:380px">
<img class="r" src="{url(mutant)}" style="position:absolute;right:300px;bottom:4px;height:310px">
<img class="r" src="{url(commando)}" style="position:absolute;left:250px;bottom:4px;height:280px">
<img class="r" src="{url(soldier)}" style="position:absolute;left:40px;bottom:0;height:330px">
<div style="position:absolute;left:330px;top:200px;width:480px;height:3px;background:linear-gradient(90deg,transparent,#ffd27a);transform:rotate(-2deg);box-shadow:0 0 10px #ffb64d"></div>
</div>"""


MOBS = [
    ("Low", [
        ("Zombie Soldier", "Armored zombie with a revolver or Thompson. Its Farmer and Miner cousins carry tools or a Hand Cannon.",
         dict(model="biped", skin="zombie_soldier", pose={"rightArm": (-90, 0, 0), "leftArm": (-90, 0, 0)})),
        ("Skeleton Soldier", "A skeleton in a scout or combat helmet, with a revolver, Thompson or Hand Cannon.",
         dict(model="skeleton_soldier", skin=str(K.bone_skin()), pose=K.AIM, gun_name="thompson_smg",
              armor=("vanilla", ARMOR / "t1_scout_1.png", ARMOR / "t1_scout_2.png", ("helmet", "boots")))),
        ("Bandit", "Quick gunman with pistols, rifles and shotguns. Roams the overworld.",
         dict(model="biped", skin="bandit", pose=K.AIM, gun_name="sawed_off"))]),
    ("Medium", [
        ("Army Soldier", "Guards camps and bases with an M4, a shotgun or a rifle.",
         dict(model="biped", skin="army_soldier", pose=K.AIM, gun_name="m4")),
        ("Commando", "Fast, with a silenced M4 and commando armor.",
         dict(model="biped", skin="army_soldier", pose=K.AIM, gun_name="infiltrator", armor=vset("t2_commando"))),
        ("Alien Bug", "Very fast, climbs walls, bites. Swarms out of bug nests.", dict(model="alien_bug", skin="alien_bug"))]),
    ("High", [
        ("Stormtrooper", "HEV suit and a Blaster or Laser Rifle.",
         dict(model="biped", skin="army_soldier", pose=K.AIM, gun_name="blaster_rifle", armor=vset("t3_hev"))),
        ("Super Mutant", "Tall brutes in three ranks, with rockets, rifles and lasers.",
         dict(model="super_mutant", skin="super_mutant_3", pose=K.AIM, gun_name="rocket_launcher")),
        ("Outcast", "Power armor and the heavy guns: Minigun, Tesla Rifle, Grim Reaper.",
         dict(model="biped", skin="army_soldier", pose=K.AIM, gun_name="tesla_rifle", armor=("custom", "t3_power")))]),
    ("Boss", [
        ("Psycho Steve", "Very fast, very angry, and carrying a chainsaw.",
         dict(model="biped", skin="army_soldier", pose=K.AIM, gun_name="chainsaw",
              armor=("vanilla", ARMOR / "t1_miner_red_1.png", ARMOR / "t1_miner_red_2.png"))),
        ("Dictator Dave", "A golden revolver and a hundred hit points.",
         dict(model="biped", skin="dictator", pose=K.AIM, gun_name="golden_revolver")),
        ("Cyberdemon", "Fireproof brute with a nether blaster. Drops Cybernetic Parts.", dict(model="cyberdemon", skin="cyberdemon")),
        ("Attack Helicopter", "Machine-gun bursts and rockets. Comes when you break a camp flag.",
         dict(model="attack_helicopter", skin="attack_helicopter", yaw=-120, pitch=14))]),
]
DANGER = {"Low": "#7bd88f", "Medium": "#ffd24a", "High": "#ff8a1f", "Boss": "#ff5555"}


def enemies():
    groups = []
    for level, mobs in MOBS:
        cards = ""
        for n, d, kw in mobs:
            kw = dict(kw)
            # A gun is held in the right hand, so a gunman is turned to show that side and mirrored to face left.
            armed = bool(kw.get("gun_name"))
            p = K.mob(kw.pop("model"), kw.pop("skin"), yaw=kw.pop("yaw", 50 if armed else -30), flip=armed, pitch=kw.pop("pitch", 8), h=320, **kw)
            cards += f"""<div class="wcard" style="align-items:center;text-align:center">
<div class="spot" style="height:230px;width:100%;display:flex;align-items:flex-end;justify-content:center;padding-bottom:6px">{img(p, h=210, style="max-width:100%")}</div>
<h3 style="margin-top:8px">{esc(n)}</h3><div class="role">{esc(d)}</div></div>"""
        groups.append(f"""<div style="display:grid;grid-template-columns:130px 1fr;gap:16px;align-items:stretch">
<div style="border-left:6px solid {DANGER[level]};padding-left:14px;display:flex;flex-direction:column;justify-content:center">
<div class="label">Danger</div><div class="cond" style="font-size:38px;font-weight:700;color:{DANGER[level]}">{level}</div></div>
<div class="grid" style="grid-template-columns:repeat(4,minmax(0,1fr))">{cards}</div></div>""")
    structs = "".join(f'<div style="display:flex;justify-content:space-between;gap:12px;padding:6px 0;border-bottom:1px solid var(--line)"><span>{esc(n)}</span>{chip(t)}</div>'
                      for n, t in (("Small Factory, Train Station, House, Gas Station", "LV"), ("Big Bunker", "MV"),
                                   ("Military Base and camps", "MV"), ("Aircraft Carrier, Submarine", "MV")))
    info = f"""<div class="grid" style="grid-template-columns:repeat(3,minmax(0,1fr));margin-top:18px">
<div class="card"><h2>Ruins and bases</h2>{muted("Chests hold loot up to the tier shown. Alien Bug Nests have no chests, only bug holes.", 16, 4)}<div style="margin-top:8px">{structs}</div></div>
<div class="card"><h2>Elite, Ultra, Infernal</h2>{muted("Any soldier or mutant can spawn with an Infernal Mobs rank and better loot: LV for Elite, MV for Ultra, and a rare HV drop from Infernal. Loot never goes above HV.")}</div>
<div class="card"><h2>What they drop</h2>{muted("Ammo for their guns, and sometimes the armor they wear. A player kill has a small chance to drop the gun itself, and Looting helps.")}</div></div>"""
    body = f"""{battle_scene()}
<div style="display:flex;flex-direction:column;gap:16px;margin-top:22px">{''.join(groups)}</div>{info}
{guide_row("They don't fight each other, but they will shoot your turrets. Their guns hit for about a third of what yours do, which is little comfort against a Super Mutant with a rocket launcher.", gun="as50")}"""
    return page_doc("Enemies and <b>loot</b>",
                    "Soldiers, mutants, monsters and a helicopter. Overworld only, and every faction can be switched off in the config.",
                    body)


# ---------------------------------------------------------------- questbook

CHAPTERS = [
    ("Start here", None, None, "Controls, ammo words and how tiers work. No rewards, just the briefing."),
    ("Steam age", "Steam", "handcannon", "Hand Cannon, Stone Bullets, first kills, Bandit and Miner armor."),
    ("Low Voltage", "LV", "pistol_9mm", "Pressed cases and 9mm rounds, your first pistol and magazines, Soldier armor."),
    ("Medium Voltage", "MV", "m4", "SMGs, the M4 and AK47, akimbo pairs, grenades, T2 armor."),
    ("High Voltage", "HV", "as50", "AUG, SCAR, LMG, AS50, launchers, flamethrower, chainsaw, Power Hammer."),
    ("Extreme Voltage", "EV", "laser_rifle", "Charging at the right tier, lasers, Pulse Rifle, Minigun, T3 and Steam armor."),
    ("Insane Voltage", "IV", "blaster_rifle", "Blasters, the De-Atomizer, Power Armor, the Exo Suit, anti-gravity flight."),
    ("Ludicrous Voltage", "LuV", "nether_blaster", "Nether Charges, the Nether and Alien Blasters, the Grim Reaper."),
    ("Zero Point Module", "ZPM", "nuclear_death_ray", "Tactical Nuke Rockets, the Nuclear Death Ray and the Nuke Launcher."),
]
BRANCHES = [
    ("NPCs and bosses", "Hunt each faction, Psycho Steve and the helicopter. A few quests are secret.", ("mob", "cyberdemon")),
    ("Structures", "Ruins, bunkers, the military camp, the carrier and the submarine.", ("item", "bunker_door")),
    ("Machines", "Repair Bench, mending, turret setup, the Reaction Chamber and its risks.", ("block", "repair_bench")),
    ("Armor and equipment", "Equipment slots, medkits, gas masks, night vision, oxygen, back gear.", ("item", "gas_mask")),
    ("Ammo logistics", "Pressing parts, automation, an ammo depot, explosives and fuel.", ("item", "mag_assault_rifle")),
]


def questbook():
    nodes = []
    for i, (name, tier, gun, what) in enumerate(CHAPTERS):
        color = tier_var(tier) if tier else "var(--orange)"
        nodes.append(f"""<div style="display:grid;grid-template-columns:70px 220px 1fr;gap:18px;align-items:center;padding:10px 18px;border-radius:12px;background:rgba(255,255,255,.03);border:1px solid var(--line);border-left:6px solid {color}">
<div class="cond" style="font-size:40px;font-weight:700;color:{color};text-align:center">{i + 1}</div>
<div class="spot" style="display:flex;justify-content:center;padding:6px 0">{img(K.gun(gun, w=240, h=90), h=62, style="max-width:100%") if gun else img(K.REPO / "docs/assets/branding/logo_pixel_128.png", h=64, cls="px")}</div>
<div><h3>{esc(name)} {chip(tier) if tier else ''}</h3>{muted(esc(what), 17, 2)}</div></div>""")
    branches = ""
    for name, what, (kind, ref) in BRANCHES:
        if kind == "mob":
            pic = img(K.mob(ref, ref, yaw=-30, pitch=8, h=200), h=110)
        elif kind == "block":
            pic = img(S.machine_block(ref, "LV"), h=96)
        else:
            pic = ico(ref, 80)
        branches += f"""<div class="card" style="display:grid;grid-template-columns:110px 1fr;gap:14px;align-items:center">
<div class="spot" style="display:flex;justify-content:center;padding:6px 0">{pic}</div><div><h3>{esc(name)}</h3>{muted(esc(what), 16, 2)}</div></div>"""
    body = f"""<div style="display:grid;grid-template-columns:1fr 520px;gap:22px">
<div><div class="label" style="margin-bottom:10px">The main path</div><div style="display:flex;flex-direction:column;gap:10px">{''.join(nodes)}</div></div>
<div><div class="label" style="margin-bottom:10px">Side branches, open whenever you are</div><div style="display:flex;flex-direction:column;gap:12px">{branches}</div>
<div class="card" style="margin-top:14px"><h2>Rewards</h2>{muted("Early quests pay ammo or a magazine for the gun they teach. Chapter ends and boss hunts pay a GregTech loot bag of their tier. A reward never outruns its tier.")}</div></div></div>
{guide_row("Each chapter opens with the pack's own quest for that tier. Already at HV? Jump straight in; nothing makes you replay the Steam age.", pose=POINT, flip=True, yaw=-20)}"""
    return page_doc("The GregGuns <b>questbook</b>",
                    "Its own tab in the GTNH questbook, after Kill All the Things. It walks you up the tiers one weapon at a time.",
                    body)


# ---------------------------------------------------------------- README hero


def hero():
    logo = url(K.REPO / "docs/assets/branding/logo_pixel_256.png")
    lineup = [
        figure(pose=K.AIM, gun_name="handcannon", yaw=60, pitch=6, h=300, armor=vset("t1_scout")),
        figure(pose=K.AIM, gun_name="m4", yaw=60, pitch=6, h=300, armor=vset("t2_combat")),
        figure(pose=K.AIM, gun_name="laser_rifle", yaw=60, pitch=6, h=300, armor=vset("t3_hev")),
        figure(pose=K.AIM, gun_name="nuclear_death_ray", yaw=60, pitch=6, h=300, armor=("custom", "t3_power")),
    ]
    figs = "".join(f'<img class="r" src="{url(p)}" style="height:{196 + i * 16}px;margin-left:-18px">'
                   for i, p in enumerate(lineup))
    boom = K.fx_frame("explosion", 3, 1, 5, 420, black_to_alpha=True)
    mutant = K.mob("super_mutant", "super_mutant_1", pose=K.AIM, gun_name="lmg", yaw=-60, pitch=6, h=300)
    body = f"""<div style="position:relative;height:430px">
<div style="position:absolute;left:-20px;bottom:-30px;width:640px;height:330px;background:radial-gradient(ellipse 50% 50% at 50% 60%,rgba(185,205,230,.26),transparent 75%)"></div>
<div style="position:absolute;right:-20px;bottom:-30px;width:300px;height:330px;background:radial-gradient(ellipse 50% 50% at 50% 60%,rgba(185,205,230,.22),transparent 75%)"></div>
<img src="{url(boom)}" style="position:absolute;right:120px;top:-10px;height:420px;mix-blend-mode:screen;opacity:.9">
<img class="r" src="{url(mutant)}" style="position:absolute;right:20px;bottom:0;height:300px">
<div style="position:absolute;left:0;top:10px;display:flex;align-items:center;gap:24px">
<img src="{logo}" style="width:150px;height:150px;image-rendering:pixelated">
<div><h1 style="font-size:96px;line-height:.9;text-transform:uppercase">Greg<b style="color:var(--orange)">Guns</b></h1>
<div style="font-size:26px;color:var(--muted);margin-top:8px">Guns, armor and military tech for GT New Horizons</div></div></div>
<div style="position:absolute;left:30px;bottom:0;display:flex;align-items:flex-end">{figs}</div>
<div style="position:absolute;left:700px;bottom:24px;display:flex;gap:6px">{''.join(chip(t) for t in K.TIERS)}</div>
</div>"""
    return f"""<!doctype html><html><head><meta charset="utf-8"><style>{K.CSS}{PAGE_CSS}</style></head>
<body><div class="sheet" style="--w:1600px;padding:30px 48px 0">{body}</div></body></html>"""


PAGES = {
    "hero": hero,
    "getting-started": getting_started,
    "arsenal-steam-to-hv": lambda: arsenal(1),
    "arsenal-ev-to-zpm": lambda: arsenal(2),
    "ammo-and-magazines": ammo,
    "armor-and-gear": armor,
    "machines": machines,
    "multiblock-ammunition-works": lambda: multiblock("ammunition-works"),
    "multiblock-retort": lambda: multiblock("retort"),
    "multiblock-armory-gantry": lambda: multiblock("armory-gantry"),
    "multiblock-rampart": lambda: multiblock("rampart"),
    "enemies": enemies,
    "questbook": questbook,
    "recipes-steam-and-lv": lambda: __import__("pages_recipes").recipes(),
}
