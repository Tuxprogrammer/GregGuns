#!/usr/bin/env python
"""Build the GregGuns asset manifest.

Reads the measured Techguns footprints (docs/techguns-spec/ASSET-FACTS.md and AUDIO-PROFILES.md)
and the GT material colors, applies the GregGuns scope decisions (design records 0001, 0002,
0006, 0009, 0010, 0011) and writes:

    tools/asset-gen/manifest.json   machine-readable manifest
    docs/assets/MANIFEST.md         human-readable tables
    docs/assets/CREDITS.md          skeleton, only if it does not exist

Run:  python -I tools/asset-gen/build_manifest.py

Nothing here generates an asset. Techguns paths appear only as footprint references (size,
frames, color, duration). No Techguns pixel or sample is read.
"""
import json
import os
import re
import sys

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
FACTS = os.path.join(ROOT, "docs", "techguns-spec", "ASSET-FACTS.md")
AUDIO = os.path.join(ROOT, "docs", "techguns-spec", "AUDIO-PROFILES.md")
MATS = os.path.join(
    ROOT, "reference", "src", "GT5-Unofficial@5.09.54.205", "src", "main", "java",
    "gregtech", "loaders", "materials", "MaterialsInit.java")

# ----------------------------------------------------------------------------------------------
# Inputs
# ----------------------------------------------------------------------------------------------


def read(p):
    with open(p, encoding="utf8") as f:
        return f.read()


def parse_facts():
    tex, snd = {}, {}
    for line in read(FACTS).splitlines():
        if not line.startswith("| `"):
            continue
        c = [x.strip() for x in line.strip().strip("|").split("|")]
        p = c[0].strip("`")
        if p.startswith("textures/"):
            m = re.match(r"(\d+)\S(\d+)", c[1])
            fr = re.match(r"(\d+) frames", c[2])
            tex[p] = dict(w=int(m.group(1)), h=int(m.group(2)),
                          frames=int(fr.group(1)) if fr else 1, avg=c[3])
        elif p.startswith("sounds/"):
            snd[p] = dict(dur=float(c[1].split()[0]), rate=int(c[2].split()[0]),
                          ch=c[3], event=c[4])
    return tex, snd


def parse_audio():
    out = {}
    for line in read(AUDIO).splitlines():
        if line.startswith("| `sounds/"):
            c = [x.strip() for x in line.strip().strip("|").split("|")]
            out[c[0].strip("`")] = dict(desc=c[2], lufs=float(c[5]))
    return out


def parse_mats():
    cols = {}
    s = read(MATS)
    for m in re.finditer(r'setName\("(\w+)"\)(.*?)constructMaterial', s, re.S):
        a = re.search(r"setARGB\((0x[0-9a-fA-F]+)\)", m.group(2))
        if a:
            cols[m.group(1)] = "#%06X" % (int(a.group(1), 16) & 0xFFFFFF)
    # GT names polyethylene "Plastic" internally and wrought iron is an alias of cast iron.
    cols["Polyethylene"] = cols.get("Plastic", "#C8C8C8")
    cols["WroughtIron"] = cols.get("CastIron", "#C8B4B4")
    return cols


TEX, SND = parse_facts()
AUD = parse_audio()
MC = parse_mats()


def mc(*names):
    return [MC[n] for n in names]


TIER_MAT = {"ULV": "Bronze", "LV": "Steel", "MV": "Aluminium", "HV": "StainlessSteel",
            "EV": "Titanium", "IV": "TungstenSteel", "LuV": "HSSG", "ZPM": "NaquadahAlloy"}

ENTRIES = []
_ids = set()


def add(id, kind, path, w=None, h=None, frames=1, frametime=None, dur=None, ch=None, sr=None,
        lufs=None, palette=None, brief="", fp="", pri="P2"):
    assert id not in _ids, id
    _ids.add(id)
    ENTRIES.append(dict(
        id=id, kind=kind, path=path, width=w, height=h, frames=frames, frametime=frametime,
        duration_s=dur, channels=ch, sample_rate=sr, target_lufs=lufs,
        palette=palette or [], brief=brief, footprint_ref=fp, priority=pri))


def ttex(name):
    """Techguns texture footprint path (reference only)."""
    return "textures/" + name


def snake(s):
    s = re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", s)
    return s.replace("-", "_").lower()


def icon(reg, display, brief, tier=None, mats=(), fp="", pri="P2", extra_palette=(), group=None):
    pal = [MC[m] for m in mats] + list(extra_palette)
    if not pal and tier:
        pal = [MC[TIER_MAT[tier]]]
    add("icon." + reg, "icon", "textures/items/%s.png" % reg, 16, 16, palette=pal,
        brief="%s: %s" % (display, brief), fp=fp, pri=pri)


# ----------------------------------------------------------------------------------------------
# 1. Item icons (16x16)
# ----------------------------------------------------------------------------------------------
G = "GT-style 16x16 item icon, soft top-left light, dark outline. "

# Guns, melee, grenades. (reg, display, tier, model texture base or None, priority, description)
GUNS = [
    ("handcannon", "Hand Cannon", "ULV", "handgun", "P2", "stubby stone-barrel pistol with a wooden stock"),
    ("pistol_9mm", "9mm Pistol", "LV", "pistol3", "P1", "compact black polymer pistol, steel slide"),
    ("revolver", "Revolver", "LV", "revolver", "P2", "six-shot revolver with wooden grip"),
    ("sawed_off", "Sawed Off", "LV", "sawedOff", "P2", "double-barrel sawed-off shotgun"),
    ("bolt_action_rifle", "Bolt Action Rifle", "LV", "boltactionrifle", "P2", "long bolt-action rifle with scope"),
    ("combat_knife", "Combat Knife", "LV", None, "P2", "black-bladed combat knife, rubber grip"),
    ("crowbar", "Crowbar", "LV", None, "P2", "steel crowbar, hooked end"),
    ("stielgranate", "Stielgranate", "LV", "stielgranate", "P2", "stick grenade with wooden handle"),
    ("thompson_smg", "Thompson SMG", "MV", "thompson", "P2", "drum-less submachine gun, wooden stock"),
    ("mac10", "Mac10", "MV", "mac10texture", "P2", "boxy machine pistol"),
    ("combat_shotgun", "Combat Shotgun", "MV", "combatShotgun", "P2", "pump shotgun, polymer stock"),
    ("m4", "M4 Assault Rifle", "MV", "m4Texture", "P2", "M4-style rifle with optic"),
    ("ak47", "AK47", "MV", "ak47Texture", "P2", "AK-style rifle, curved magazine, wood foregrip"),
    ("frag_grenade", "Frag Grenade", "MV", "fragGrenade_texture", "P2", "olive fragmentation grenade with pin"),
    ("flashbang", "Flashbang", "MV", None, "P2", "gray cylinder grenade with magnesium-white band (new design)"),
    ("akimbo_pistols", "Akimbo Pistols", "MV", "pistol3", "P2", "two 9mm pistols crossed"),
    ("akimbo_revolvers", "Akimbo Revolvers", "MV", "revolver", "P2", "two revolvers crossed"),
    ("aug", "AUG", "HV", "AugTexture", "P2", "bullpup rifle with optic"),
    ("scar", "SCAR", "HV", "scar_texture", "P2", "modular tan battle rifle"),
    ("vector", "Vector", "HV", "vector_texture", "P2", "angular compact SMG"),
    ("infiltrator", "The Infiltrator", "HV", "m4_uq_Texture", "P2", "suppressed M4 with long rubber-wrapped suppressor"),
    ("as50", "AS50 Sniper Rifle", "HV", "as50Texture", "P2", "heavy anti-materiel sniper rifle with large scope"),
    ("lmg", "LMG", "HV", "mg2_texture", "P2", "belt-fed light machine gun with bipod"),
    ("grenade_launcher", "Grenade Launcher", "HV", "grenadelauncher", "P2", "revolving 40 mm launcher"),
    ("rocket_launcher", "Rocket Launcher", "HV", "rocketlauncher", "P2", "shoulder tube launcher"),
    ("flamethrower", "Flamethrower", "HV", "Flamethrower", "P2", "nozzle gun with fuel line and igniter coil"),
    ("akimbo_mac10s", "Akimbo Mac10s", "HV", "mac10texture", "P2", "two Mac10 crossed"),
    ("chainsaw", "Chainsaw", "HV", "chainsaw", "P3", "gasoline chainsaw with steel bar"),
    ("power_hammer", "Power Hammer", "HV", "powerHammer", "P3", "pneumatic hammer with air hose"),
    ("laser_rifle", "Laser Rifle", "EV", "Lasergun", "P3", "warm-gray sci-fi rifle with red emitter"),
    ("pdw", "PDW", "EV", "PDW", "P3", "dark compact carbon PDW"),
    ("pulse_rifle", "Pulse Rifle", "EV", "pulseRifle", "P3", "carbon rifle with diamond-lens fire control"),
    ("minigun", "Minigun", "EV", "minigun", "P3", "six-barrel rotary gun with drum"),
    ("tesla_rifle", "Tesla Rifle", "EV", "TeslaGun", "P3", "rifle with a coil emitter and cyan arcs"),
    ("sonic_rifle", "Sonic Rifle", "EV", "sonicshotgun", "P3", "slate and teal rifle with cyan glow strip"),
    ("bio_gun", "Bio Gun", "EV", "BioGun", "P3", "green-tanked fluid gun"),
    ("power_hammer_adv", "Power Hammer (Advanced)", "EV", "powerHammerAdv", "P3", "diamond-studded power hammer"),
    ("blaster_rifle", "Blaster Rifle", "IV", "blasterRifle", "P3", "heavy blaster rifle, orange emitter"),
    ("deatomizer_pistol", "De-Atomizer Pistol", "IV", "mibgun", "P3", "chrome sci-fi pistol with blue lamp"),
    ("scatter_laser_rifle", "Scatter Laser Rifle", "IV", "laserGunNew", "P3", "gray multi-emitter laser rifle (new design)"),
    ("golden_revolver", "Golden Revolver", "IV", "goldenrevolver", "P2", "gold-plated revolver"),
    ("akimbo_deatomizer_pistols", "Akimbo De-Atomizer Pistols", "IV", "mibgun", "P3", "two De-Atomizer pistols crossed"),
    ("akimbo_golden_revolvers", "Akimbo Golden Revolvers", "IV", "goldenrevolver", "P2", "two golden revolvers crossed"),
    ("nether_blaster", "Nether Blaster", "LuV", "cyberdemonblaster", "P3", "worn gray blaster with lava-orange core"),
    ("alien_blaster", "Alien Blaster", "LuV", "alien_blaster", "P3", "pale alloy alien pistol, violet glow"),
    ("grim_reaper", "Grim Reaper", "LuV", "grimreaper", "P3", "quad-tube rocket battery"),
    ("nuclear_death_ray", "Nuclear Death Ray", "ZPM", "NDR", "P3", "bulky ray projector, golden plasma core"),
    ("nuke_launcher", "Nuke Launcher", "ZPM", "nukerocketlauncher", "P3", "olive launcher with hazard trefoil"),
    ("golden_gun", "Golden Gun", "LuV", "pistol3", "P2", "polished gold 9mm pistol (the pistol's model in gold; GregGuns original)"),
]

for reg, disp, tier, tex, pri, desc in GUNS:
    if reg in ("combat_knife",):
        fp = ttex("items/combatKnife.png")
    elif reg == "crowbar":
        fp = ttex("items/crowbar.png")
    elif tex:
        fp = ttex("guns/%s.png" % tex) + " (model sheet, silhouette reference only)"
    else:
        fp = ttex("guns/fragGrenade_texture.png") + " (model sheet, silhouette reference only)"
    icon(reg, "%s (%s)" % (disp, tier), G + desc + ". Metal parts take the %s gate color." % TIER_MAT[tier].replace("Aluminium", "Aluminum"),
         tier=tier, fp=fp, pri=pri)

# Components (06 recipes component index). (reg, display, tier, material, Techguns icon, pri, description)
PARTS = [
    ("part_barrel_stone", "Stone Barrel", "ULV", ["Bronze", "Stone"], "stoneBarrel", "P2", "stone barrel with a bronze liner"),
    ("part_barrel_iron", "Iron Barrel", "LV", ["Iron", "Steel"], "ironBarrel", "P2", "plain iron barrel, two steel rings"),
    ("part_barrel_hardened", "Hardened Steel Barrel", "LV", ["Steel"], "obsidianSteelBarrel", "P1", "dark steel barrel with jacket plate"),
    ("part_barrel_carbon", "Carbon Barrel", "EV", ["Titanium", "EpoxidFiberReinforced"], "carbonBarrel", "P3", "black fiber-wrapped barrel"),
    ("part_barrel_laser", "Laser Barrel", "EV", ["Titanium", "Electrum"], "laserBarrel", "P3", "barrel with a glass lens and electrum foil"),
    ("part_stock_wooden", "Wooden Stock", "ULV", ["Wood"], "woodStock", "P2", "carved wooden gun stock"),
    ("part_stock_plastic", "Plastic Gun Stock", "MV", ["Polyethylene", "Aluminium"], "plasticGunstock", "P2", "off-white polymer stock"),
    ("part_stock_carbon", "Carbon Gunstock", "EV", ["EpoxidFiberReinforced", "Titanium"], "carbonGunstock", "P3", "black fiber stock"),
    ("part_pump_mechanism", "Pump Mechanism", "LV", ["Steel", "Rubber"], "pumpMechanism", "P2", "spring and rubber-seal pump slide"),
    ("part_receiver_iron", "Iron Receiver", "LV", ["Iron"], "ironReceiver", "P2", "boxy iron receiver block"),
    ("part_receiver_steel", "Steel Receiver", "MV", ["Steel"], "steelReceiver", "P2", "machined steel receiver"),
    ("part_receiver_hardened", "Hardened Receiver", "HV", ["StainlessSteel"], "obsidianSteelReceiver", "P2", "dark stainless receiver"),
    ("part_receiver_carbon", "Carbon Receiver", "EV", ["Titanium", "EpoxidFiberReinforced"], "carbonReceiver", "P3", "black fiber receiver"),
    ("part_mechanical_1", "Mechanical Parts", "ULV", ["Bronze"], "mechanicalParts", "P2", "bronze gears and a small spring"),
    ("part_mechanical_2", "Mechanical Parts T2", "LV", ["Steel"], "mechanicalPartsT2", "P1", "steel gears, spring and screws"),
    ("part_mechanical_3", "Mechanical Parts T3", "EV", ["Titanium"], "mechanicalPartsT3", "P3", "titanium gears, spring and screws"),
    ("part_laser_focus", "Laser Focus", "EV", ["Titanium", "Diamond"], "laserFocus", "P3", "faceted focus crystal in a titanium cradle"),
    ("part_uv_emitter", "UV Emitter", "EV", ["Titanium", "Diamond"], "uvemitter", "P3", "violet emitter lens on a titanium plate"),
    ("part_sonic_emitter", "Sonic Emitter", "EV", ["Titanium"], "sonicEmitter", "P3", "ringed oscillator disc"),
    ("part_rad_emitter", "Rad Emitter", "ZPM", ["NaquadahAlloy", "Uranium235"], "radEmitter", "P3", "lead-shielded emitter with green glow"),
    ("part_anti_gravity_core", "Anti Gravity Core", "IV", ["TungstenSteel"], "antiGravityCore", "P3", "cyan core sphere in a steel cage"),
    ("part_cybernetic", "Cybernetic Parts", "EV", ["Electrum", "Titanium"], "cyberneticParts", "P3", "wires and plates, warm gray"),
    ("part_heavy_cloth", "Heavy Cloth", "ULV", ["Wood"], "heavyCloth", "P2", "folded brown heavy cloth"),
    ("part_treated_leather", "Treated Leather", "LV", ["Steel"], "treatedLeather", "P2", "dark green-black treated leather"),
    ("part_power_plating", "Power Plating", "IV", ["TungstenSteel", "EpoxidFiberReinforced"], "powerPlating", "P2", "slate plate with fiber layer"),
    ("part_gas_mask_filter", "Gas Mask Filter", "MV", ["Aluminium"], "gasMaskFilter", "P2", "round filter canister"),
    ("part_nuclear_warhead", "Small Nuclear Warhead", "ZPM", ["NaquadahAlloy", "Lead"], "tacticalNuclearWarhead", "P3", "olive warhead with hazard band"),
    ("part_glider_backpack", "Glider Backpack", "HV", ["StainlessSteel", "Wood"], "gliderBackback", "P2", "canvas backpack with steel frame"),
    ("part_glider_wing", "Glider Wing", "HV", ["StainlessSteel", "Wood"], "gliderWing", "P2", "canvas wing panel on steel struts"),
]
for reg, disp, tier, mats, fpn, pri, desc in PARTS:
    icon(reg, "%s (%s)" % (disp, tier), G + desc + ".", mats=mats, pri=pri,
         fp=ttex("items/%s.png" % fpn) if fpn else "")

# Ammunition
AMMO = [
    ("ammo_stone_bullet", "Stone Bullet", "ULV", ["Stone"], "stonebullets", "P2", "round gray stone shot"),
    ("ammo_9mm_bullet", "9mm Bullets", "LV", ["Brass"], "bullets9mm", "P1", "short brass-cased pistol rounds with gray tips"),
    ("ammo_shotgun_shell", "Shotgun Shell", "LV", ["Brass"], "shotgunShell", "P2", "red plastic shell with brass base"),
    ("ammo_rifle_bullet", "Rifle Bullets", "LV", ["Brass", "Steel"], "rifleBullets", "P2", "long brass rifle cartridges"),
    ("ammo_high_caliber_round", "High Caliber Rounds", "HV", ["Brass", "StainlessSteel"], "highCalibreBullets", "P2", "large brass anti-materiel rounds"),
    ("ammo_advanced_bullet", "Advanced Bullets", "EV", ["Titanium"], "advBullets", "P3", "slim violet-tipped caseless rounds"),
    ("ammo_40mm_grenade", "40mm Grenade", "MV", ["Brass", "Aluminium"], "40mmGrenade", "P2", "squat brass 40 mm grenade with red band"),
    ("ammo_rocket", "Rocket", "HV", ["StainlessSteel"], None, "P2", "finned rocket with red nose"),
    ("ammo_rocket_nuke", "Rocket (Tactical Nuke)", "ZPM", ["NaquadahAlloy"], None, "P3", "olive finned rocket with hazard trefoil"),
    ("ammo_nether_charge", "Nether Charge", "LuV", ["HSSG"], "nethercharge", "P3", "tan charge pack with orange glow"),
    ("golden_round", "Golden Round", "LuV", ["Gold"], None, "P2", "single fat gold-cased round with a bright gold tip (GregGuns original)"),
]
for reg, disp, tier, mats, fpn, pri, desc in AMMO:
    icon(reg, "%s (%s)" % (disp, tier), G + desc + ".", mats=mats, pri=pri,
         fp=ttex("items/%s.png" % fpn) if fpn else "")

# Magazines (full + empty), tanks (full + empty), nuclear cells
MAGS = [
    ("mag_9mm_pistol", "9mm Pistol Magazine", "LV", "pistolMag9mm", "P1"),
    ("mag_smg", "SMG Magazine", "MV", "smgMag", "P2"),
    ("mag_assault_rifle", "Assault Rifle Magazine", "MV", None, "P2"),
    ("mag_as50", "AS50 Magazine", "HV", None, "P2"),
    ("mag_lmg", "LMG Magazine", "HV", None, "P2"),
    ("mag_advanced", "Advanced Magazine", "EV", "pdwMag", "P3"),
    ("mag_minigun_drum", "Minigun Ammo Drum", "EV", "minigunDrum", "P3"),
]
for reg, disp, tier, fpn, pri in MAGS:
    for empty in (False, True):
        r = reg + ("_empty" if empty else "")
        fp = ""
        if fpn:
            fp = ttex("items/%s%s.png" % (fpn, ("Empty" if fpn != "minigunDrum" else "_empty") if empty else ""))
        icon(r, "%s%s (%s)" % (disp, " (Empty)" if empty else "", tier),
             G + ("Empty magazine: same silhouette, no rounds visible, darker interior."
                  if empty else "Loaded magazine: visible brass round tops.") + " Body in the tier gate color.",
             tier=tier, pri=pri, fp=fp)
TANKS = [
    ("tank_fuel", "Fuel Tank", "HV", "flamethrowerTank", "P2", ["StainlessSteel"], "orange-brown fuel canister"),
    ("tank_bio", "Bio Tank", "EV", "bioTank", "P3", ["Titanium"], "green-filled glass canister"),
    ("tank_air", "Compressed Air Tank", "HV", "compressedAirTank", "P2", ["StainlessSteel"], "pale-blue air bottle"),
]
for reg, disp, tier, fpn, pri, mats, desc in TANKS:
    for empty in (False, True):
        icon(reg + ("_empty" if empty else ""), "%s%s (%s)" % (disp, " (Empty)" if empty else "", tier),
             G + desc + (", drained: gray glass, no fill" if empty else ", full") + ".", mats=mats, pri=pri,
             fp=ttex("items/%s%s.png" % (fpn, "Empty" if empty else "")))
icon("cell_nuclear", "Nuclear Power Cell (ZPM)", G + "green-glow uranium cell in a lead sleeve.", mats=["NaquadahAlloy", "Uranium235", "Lead"],
     pri="P3", fp=ttex("items/nuclearPowerCell.png"))
icon("cell_nuclear_depleted", "Nuclear Power Cell, Depleted (ZPM)", G + "same cell, glow gone, dull gray-brown.", mats=["NaquadahAlloy", "Lead"],
     pri="P3", fp=ttex("items/nuclearPowerCellDepleted.png"))

# Consumables, tools, placers, misc
icon("bandages", "Bandages (LV)", G + "rolled white bandage with a small red mark.", extra_palette=["#FFFFFF", "#C80000"],
     pri="P2", fp=ttex("items/medkit_0.png"))
icon("medkit", "Medkit (MV)", G + "white case with red cross.", extra_palette=["#FAFAFA", "#C80000"], pri="P2",
     fp=ttex("items/medkit_1.png"))
for i, nm in enumerate(("Woodland", "Snow", "Desert")):
    icon("camo_net_placer_%d" % i, "Camo Net Placer: %s" % nm,
         G + "folded net roll, %s camouflage pattern." % nm.lower(), pri="P3",
         extra_palette={"Woodland": ["#3C5A28", "#6E7A3C"], "Snow": ["#E6EBEF", "#AEB9C4"], "Desert": ["#B49664", "#8C7448"]}[nm],
         fp=ttex("items/camoNetPlacer%d.png" % i))
icon("bunker_door", "Bunker Door (item)", G + "gray riveted steel door slab.", mats=["Iron"], pri="P3",
     fp=ttex("items/metalDoor.png"))
icon("ammo_pouch", "Ammo Pouch (Baubles quiver)", G + "small leather pouch with brass snaps and three bullets showing.",
     mats=["Brass"], extra_palette=["#6B4A2B"], pri="P2", fp="(new item, no Techguns icon)")
icon("spawn_egg", "Spawn Egg (base)", G + "grayscale mottled egg, transparent outside, tintable per mob.",
     pri="P3", extra_palette=["#898989"], fp=ttex("items/spawn_egg.png"))
icon("spawn_egg_overlay", "Spawn Egg (overlay)", G + "white 'GG' lettering with a 1 px dark outline, grayscale so tint works.",
     pri="P3", extra_palette=["#FFFFFF"], fp=ttex("items/spawn_egg_overlay.png"))
for mat, disp, tier in (("Iron", "Iron", "LV"), ("Steel", "Steel", "LV"), ("Aluminium", "Aluminum", "MV"),
                         ("EpoxidFiberReinforced", "Carbon", "EV")):
    icon("turret_armor_" + mat.lower().replace("epoxidfiberreinforced", "carbon").replace("aluminium", "aluminum"), "Turret Armor Plate, %s" % disp,
         G + "curved armor plate with bolt holes.", mats=[mat], pri="P3",
         fp=ttex("items/turretArmor%s.png" % {"Iron": "Iron", "Steel": "Steel", "Aluminium": "ObsidianSteel", "EpoxidFiberReinforced": "Carbon"}[mat]))

# Armor: sets of four, plus single pieces. (set id, display prefix, tier, materials, pieces, Techguns item prefix, description)
PIECES = (("helmet", "Helmet"), ("chestplate", "Chestplate"), ("leggings", "Leggings"), ("boots", "Boots"))
SETS = [
    ("t1_scout", "Bandit", "ULV", ["Bronze"], ("Mask", "Vest", "Pants", "Boots"), "t1_scout", "rough cloth with bronze studs"),
    ("t1_miner", "Miner's", "ULV", ["WroughtIron"], ("Helmet", "Jacket", "Pants", "Boots"), "t1_miner", "blue canvas, yellow helmet with lamp, wrought-iron rivets"),
    ("t1_combat", "Soldier's", "LV", ["Steel"], ("Helmet", "Vest", "Pants", "Boots"), "t1_combat", "steel plates on heavy cloth"),
    ("t2_combat", "Combat", "MV", ["Aluminium"], ("Helmet", "Vest", "Pants", "Boots"), "t2_combat", "dark olive plates with aluminum trim"),
    ("t2_commando", "Commando", "MV", ["Aluminium"], ("Helmet", "Vest", "Pants", "Boots"), "t2_commando", "near-black tactical gear"),
    ("t2_riot", "Riot", "MV", ["Aluminium"], ("Helmet", "Coat", "Pants", "Boots"), "t2_riot", "brown long coat, visor helmet, aluminum shield plates"),
    ("t3_combat", "Advanced Combat", "EV", ["Titanium", "EpoxidFiberReinforced"], ("Helmet", "Chestplate", "Leggings", "Boots"), "t3_combat", "blue-gray fiber armor with titanium panels"),
    ("t3_hev", "Hazardous Environment", "EV", ["Titanium", "EpoxidFiberReinforced"], ("Helmet", "Chestplate", "Leggings", "Boots"), "t3_miner", "sealed brown-orange hazmat suit with glass visor"),
    ("steam", "Steam", "EV", ["Titanium", "Bronze"], ("Helmet", "Chestplate", "Legplates", "Boots"), None, "bronze-and-iron steam armor with pipes and vents"),
    ("t3_power", "Power Armor", "IV", ["TungstenSteel"], ("Helmet", "Chestplate", "Leggings", "Boots"), None, "bulky slate power armor with glass visor"),
    ("t3_exo", "Exo Suit", "IV", ["TungstenSteel", "Titanium"], ("Helmet", "Chestplate", "Leggings", "Boots"), "t3_exo", "light exoskeleton frame over combat gear"),
]
for sid, pre, tier, mats, names, fpb, desc in SETS:
    for (slot, _), nm in zip(PIECES, names):
        fp = ttex("items/%s_%s.png" % (fpb, slot)) if fpb else "(no Techguns icon)"
        icon("%s_%s" % (sid, slot), "%s %s (%s)" % (pre, nm, tier), G + desc + ". Slot: %s." % slot,
             mats=mats, pri="P2", fp=fp)
SINGLES = [
    ("t2_beret", "Beret", "MV", ["Aluminium"], "t2_beret", "red beret with a small badge"),
    ("gas_mask", "Gas Mask", "MV", ["Aluminium", "Rubber"], "gasMask", "rubber mask with round filter"),
    ("night_vision_goggles", "Night Vision Goggles", "MV", ["Aluminium", "Rubber"], "nightVisionGoggles", "black goggles with green lenses"),
    ("oxygen_mask", "Oxygen Mask", "HV", ["StainlessSteel", "Rubber"], "oxygenMask", "gray half-mask with hose port"),
    ("oxygen_tanks", "Oxygen Tanks", "HV", ["StainlessSteel"], "scubaTanks", "twin gray oxygen cylinders"),
    ("glider", "Glider", "HV", ["StainlessSteel", "Wood"], "glider", "folded canvas glider pack"),
    ("jump_pack", "Jump Pack", "HV", ["StainlessSteel"], "jumppack", "boxy back unit with twin thruster cones"),
    ("tactical_mask", "Tactical Mask", "HV", ["Polyethylene", "StainlessSteel"], "tacticalMask", "black full-face mask with green lens"),
    ("anti_gravity_device", "Anti Gravity Device", "IV", ["TungstenSteel"], "antiGravPack", "gray pack with glowing cyan core"),
]
for reg, disp, tier, mats, fpn, desc in SINGLES:
    pri = "P2"
    icon(reg, "%s (%s)" % (disp, tier), G + desc + ".", mats=mats, pri=pri, fp=ttex("items/%s.png" % fpn))

# ----------------------------------------------------------------------------------------------
# 2. Model textures: guns, armor, entities, machine models
# ----------------------------------------------------------------------------------------------
GUN_TEX = {  # Techguns model sheet -> (gregguns gun id, priority, note)
    "handgun": ("handcannon", "P2"), "pistol3": ("pistol_9mm", "P1"), "revolver": ("revolver", "P2"),
    "sawedOff": ("sawed_off", "P2"), "boltactionrifle": ("bolt_action_rifle", "P2"),
    "thompson": ("thompson_smg", "P2"), "mac10texture": ("mac10", "P2"), "combatShotgun": ("combat_shotgun", "P2"),
    "m4Texture": ("m4", "P2"), "ak47Texture": ("ak47", "P2"), "AugTexture": ("aug", "P2"),
    "scar_texture": ("scar", "P2"), "vector_texture": ("vector", "P2"), "m4_uq_Texture": ("infiltrator", "P2"),
    "as50Texture": ("as50", "P2"), "mg2_texture": ("lmg", "P2"), "grenadelauncher": ("grenade_launcher", "P2"),
    "rocketlauncher": ("rocket_launcher", "P2"), "Flamethrower": ("flamethrower", "P2"),
    "goldenrevolver": ("golden_revolver", "P2"), "stielgranate": ("stielgranate", "P2"),
    "fragGrenade_texture": ("frag_grenade", "P2"),
    "chainsaw": ("chainsaw", "P3"), "powerHammer": ("power_hammer", "P3"), "powerHammerAdv": ("power_hammer_adv", "P3"),
    "Lasergun": ("laser_rifle", "P3"), "PDW": ("pdw", "P3"), "pulseRifle": ("pulse_rifle", "P3"), "minigun": ("minigun", "P3"),
    "TeslaGun": ("tesla_rifle", "P3"), "sonicshotgun": ("sonic_rifle", "P3"), "BioGun": ("bio_gun", "P3"),
    "blasterRifle": ("blaster_rifle", "P3"), "mibgun": ("deatomizer_pistol", "P3"),
    "laserGunNew": ("scatter_laser_rifle", "P3"), "cyberdemonblaster": ("nether_blaster", "P3"),
    "alien_blaster": ("alien_blaster", "P3"), "grimreaper": ("grim_reaper", "P3"), "NDR": ("nuclear_death_ray", "P3"),
    "nukerocketlauncher": ("nuke_launcher", "P3"),
}
GUN_TIER = {g[0]: g[2] for g in GUNS}
GUN_SHARE = {"pistol3": "also Akimbo Pistols", "revolver": "also Akimbo Revolvers", "mac10texture": "also Akimbo Mac10s",
             "mibgun": "also Akimbo De-Atomizer Pistols", "goldenrevolver": "also Akimbo Golden Revolvers",
             "fragGrenade_texture": "also the Flashbang (recolored)"}
UNUSED_GUN_TEX = {"pistol", "pistol2Texture", "mp5Texture", "neutrinogun", "drillTexture"}
for p, f in sorted(TEX.items()):
    if not p.startswith("textures/guns/"):
        continue
    stem = p[len("textures/guns/"):-4]
    if stem in UNUSED_GUN_TEX:
        continue
    m = re.match(r"(.+)_(\d+)$", stem)
    base, var = (m.group(1), int(m.group(2))) if (m and m.group(1) in GUN_TEX) else (stem, 0)
    gid, pri = GUN_TEX[base]
    tier = GUN_TIER[gid]
    name = gid + ("_camo%d" % var if var else "")
    pal = [MC[TIER_MAT[tier]]]
    add("model.gun." + name, "model", "textures/models/guns/%s.png" % name, f["w"], f["h"],
        palette=pal + ([f["avg"]] if not var else []),
        brief="%s model sheet%s. Keep the Techguns UV layout and size so the rebuilt model maps 1:1. %s%s" % (
            dict((g[0], g[1]) for g in GUNS)[gid], " camo variant %d" % var if var else "",
            ("Shared: %s. " % GUN_SHARE[base]) if (base in GUN_SHARE and not var) else "",
            "Metal parts take the %s gate color; glow regions stay pure emissive colors." % TIER_MAT[tier].replace("Aluminium", "Aluminum")),
        fp=p, pri="P3" if var else pri)

# Armor model textures (worn layers)
ARMOR_REN = [("t2_riotArmor", "t2_riot"), ("AntiGravPack", "anti_gravity_pack"), ("hevsuit", "t3_hev"),
             ("powerarmor", "t3_power"), ("beret_texture", "t2_beret"), ("gasMask", "gas_mask"),
             ("nightVisionGoggles", "night_vision_goggles"), ("oxygenFlasks", "oxygen_tanks"),
             ("oxygenMask", "oxygen_mask"), ("tacticalmask", "tactical_mask"), ("tacticalMask", "tactical_mask"),
             ("steam_armor", "steam")]
ARMOR_BASE = {"t1_scout_1", "t1_scout_2", "t1_miner_1", "t1_miner_2", "t1_combat_1", "t1_combat_2", "t2_combat_1",
              "t2_combat_2", "t2_commando_1", "t2_commando_2", "t2_riotArmor", "t2_riotArmor_1", "t2_riotArmor_2",
              "t3_combat_1", "t3_combat_2", "t3_exo_1", "t3_exo_2", "hevsuit_1", "hevsuit_2", "steam_armor",
              "powerarmor", "beret_texture", "gasMask", "nightVisionGoggles", "oxygenFlasks", "oxygenMask",
              "tacticalMask", "glider", "AntiGravPack"}
ARMOR_DESC = {
    "t1_scout": ("T1 Scout / Bandit", ["Bronze"], "rough bandit cloth, bronze studs"),
    "t1_miner": ("T1 Miner", ["WroughtIron"], "blue canvas, yellow lamp helmet"),
    "t1_combat": ("T1 Combat / Soldier", ["Steel"], "olive cloth with steel plates"),
    "t2_combat": ("T2 Combat", ["Aluminium"], "dark olive plated combat gear"),
    "t2_commando": ("T2 Commando", ["Aluminium"], "near-black tactical gear"),
    "t2_riot": ("T2 Riot / Ranger Veteran coat", ["Aluminium"], "long brown coat with shield plates"),
    "t3_combat": ("T3 Advanced Combat", ["Titanium", "EpoxidFiberReinforced"], "blue-gray fiber armor"),
    "t3_exo": ("T3 Exo Suit", ["TungstenSteel", "Titanium"], "exoskeleton frame"),
    "t3_hev": ("T3 Hazardous Environment (HEV)", ["Titanium", "EpoxidFiberReinforced"], "sealed brown-orange suit"),
    "steam": ("Steam armor", ["Titanium", "Bronze"], "pipes, vents and bronze plates"),
    "t3_power": ("T3 Power Armor", ["TungstenSteel"], "bulky slate power armor"),
    "t2_beret": ("Beret", ["Aluminium"], "wool beret"),
    "gas_mask": ("Gas Mask", ["Rubber"], "rubber mask with filter"),
    "night_vision_goggles": ("Night Vision Goggles", ["Rubber"], "strap goggles with lenses"),
    "oxygen_tanks": ("Oxygen Tanks", ["StainlessSteel"], "twin cylinders and harness"),
    "oxygen_mask": ("Oxygen Mask", ["StainlessSteel", "Rubber"], "half mask with hose"),
    "tactical_mask": ("Tactical Mask", ["Polyethylene"], "full-face mask"),
    "glider": ("Glider", ["StainlessSteel", "Wood"], "canvas pack with wing panels"),
    "anti_gravity_pack": ("Anti Gravity Device", ["TungstenSteel"], "metal pack with cyan core"),
}
for p, f in sorted(TEX.items()):
    if not p.startswith("textures/armor/"):
        continue
    stem = p[len("textures/armor/"):-4]
    if stem.lower().startswith("jetpack"):
        continue  # dropped by 0009
    new = stem
    for a, b in ARMOR_REN:
        if new.startswith(a):
            new = b + new[len(a):]
            break
    new = new.replace("_texture", "").lower().replace("darkgreen", "dark_green")
    key = next(k for k in sorted(ARMOR_DESC, key=len, reverse=True) if new.startswith(k))
    disp, mats, desc = ARMOR_DESC[key]
    base = stem in ARMOR_BASE
    add("model.armor." + new, "model", "textures/models/armor/%s.png" % new, f["w"], f["h"],
        palette=[MC[m] for m in mats],
        brief="%s worn-model texture%s: %s. Keep the Techguns UV layout (%dx%d)." % (
            disp, "" if base else " (camo/color variant of the base set)", desc, f["w"], f["h"]),
        fp=p, pri="P2" if base else "P3")
add("model.armor.jump_pack", "model", "textures/models/armor/jump_pack.png", 64, 64,
    palette=mc("StainlessSteel"),
    brief="Jump Pack worn-model texture (new; Techguns had no back-pack sheet for it): steel boxy pack, twin thruster cones, strap harness. Same 64x64 layout as the other back gear.",
    fp=ttex("armor/glider.png") + " (layout reference only)", pri="P2")

# Entities: mobs, projectiles
MOBS = [
    ("army_soldier", "entity/army_soldier.png", "Army Soldier, base skin of Outcast, Psycho Steve, Commando and Stormtrooper", "olive camo fatigues, tan skin, dark-brown hair"),
    ("zombie_soldier", "entity/zombie_soldier.png", "Zombie Soldier, Zombie Farmer and Zombie Miner", "rotting green skin, red eyes, torn gray-brown fatigues"),
    ("bandit", "entity/bandit.png", "Bandit", "dark skin, black dreadlocks, light-gray vest"),
    ("dictator", "entity/dictator.png", "Dictator Dave", "bandit base with sunglasses and mustache-beard"),
    ("cyberdemon", "entity/cyberdemon.png", "Cyberdemon", "red muscle, gray cybernetic implants, orange cables"),
    ("gore", "entity/gore.png", "generic gibs", "flayed dark-red flesh in biped layout"),
]
for mid, src, who, desc in MOBS:
    f = TEX["textures/" + src]
    add("model.entity." + mid, "model", "textures/entity/%s.png" % mid, f["w"], f["h"], palette=[f["avg"]],
        brief="%s: %s. Keep the Techguns UV layout and size." % (who, desc), fp="textures/" + src, pri="P3")
PROJ = [
    ("bullet", "entity/bullet.png", "generic bullet tracer sprite (brass capsule, orange twin)", ["#B45A0A"], "P1"),
    ("handgun_bullet", "entity/handgunBullet.png", "Hand Cannon stone ball", mc("Stone"), "P2"),
    ("launcher_grenade", "entity/launchergrenade.png", "40 mm grenade strip map (cream casing, red band, brass)", ["#C04030"], "P2"),
    ("laser_beam", "entity/Laser.png", "red laser bolt gradient, white-hot center", ["#C00000", "#E06000"], "P3"),
    ("blue_beam", "entity/BlueBeam.png", "blue energy bolt gradient, white-hot center", ["#004080", "#2060C0"], "P3"),
    ("bioblob", "entity/bioblob.png", "glossy lime-green Bio Gun glob", ["#408000", "#A0E0A0"], "P3"),
]
for mid, src, desc, pal, pri in PROJ:
    f = TEX["textures/" + src]
    add("model.entity." + mid, "model", "textures/entity/%s.png" % mid, f["w"], f["h"], palette=pal,
        brief="Projectile texture, %s." % desc, fp="textures/" + src, pri=pri)
add("model.obj.launcher_grenade", "model", "models/entity/launcher_grenade.obj", brief="40 mm grenade projectile OBJ mesh, UV-mapped to the launchergrenade strip map.",
    fp="models/entity/launchergrenade.obj", pri="P2")

# Machine / utility model textures (TESR + entity models)
def mt(id, path, ref, brief, pal, pri="P3", w=None, h=None):
    f = TEX[ref] if ref in TEX else None
    add(id, "model", path, w or (f and f["w"]), h or (f and f["h"]), palette=pal, brief=brief, fp=ref, pri=pri)

mt("model.machine.ammo_press_parts", "textures/models/machines/ammo_press_parts.png", ttex("blocks/AmmoPress.png"),
   "Ammo Press TESR parts sheet: ram piston with flat head and glow face, hood cutout, feed-belt box models, small ejected round models, steam-puff plane. Footprint of the Techguns press sheet; only the moving parts are drawn on it (the cube comes from overlays).",
   mc("Steel") + ["#E8A040"], "P2")
for tname, tfp, tdesc, tmat in (("", "TurretBase.png", "no armor: mid-gray concrete body, charcoal trim", None),
                                 ("_iron", "TurretBase_2.png", "iron armor: clean light gray", "Iron"),
                                 ("_steel", "TurretBase_3.png", "steel armor: darker speckled gray", "Steel"),
                                 ("_aluminum", "TurretBase_4.png", "aluminum armor (was obsidian steel: remapped by 0010): cool light-blue gray", "Aluminium"),
                                 ("_carbon", "TurretBase_5.png", "carbon armor: near-black fiber", "EpoxidFiberReinforced")):
    mt("model.machine.turret_base" + tname, "textures/models/machines/turret_base%s.png" % tname, ttex("blocks/" + tfp),
       "Turret base and head model sheet, %s. Same UV layout in every variant." % tdesc,
       [MC[tmat]] if tmat else ["#717171"], "P3")
for nm, fpf, desc in (("explosive_charge", "ExpCharge", "dark olive plastic charge pack, steel-blue straps, gray detonator (LCD 'RDY' green)"),
                      ("advanced_explosive_charge", "tntCharge", "dynamite bundle in dark red, gray detonator (LCD 'RDY' green)")):
    for armed in (False, True):
        fn = fpf + ("_2" if armed else "")
        mt("model.machine.%s%s" % (nm, "_armed" if armed else ""), "textures/models/machines/%s%s.png" % (nm, "_armed" if armed else ""),
           ttex("blocks/%s.png" % fn), "%s model sheet, %s: %s%s." % (nm.replace("_", " ").title(), "armed" if armed else "unarmed", desc,
                                                                    "; LCD text red 'ARM'" if armed else ""), ["#901010" if nm == "advanced_explosive_charge" else "#303030"], "P3")
for nm, fpf, desc, pal in (("reinforced_chest", "reinforced_chest", "oak planks framed by dark iron bands", mc("Steel", "Wood")),
                           ("weapon_chest", "weapon_chest", "olive-drab military crate with black bands", ["#303010", "#101010"])):
    mt("model.machine.%s" % nm, "textures/models/machines/%s.png" % nm, ttex("blocks/%s.png" % fpf),
       "%s single-chest sheet (vanilla chest skin layout at 2x): %s." % (nm.replace("_", " ").title(), desc), pal, "P3")
    mt("model.machine.%s_double" % nm, "textures/models/machines/%s_double.png" % nm, ttex("blocks/%s_double.png" % fpf),
       "%s double-chest sheet (vanilla double layout at 2x), same art." % nm.replace("_", " ").title(), pal, "P3")
mt("model.machine.flagpost", "textures/models/machines/flagpost.png", ttex("blocks/flagpost.png"),
   "Camp flag post model sheet: gray concrete foot, pale poles, cloth strip with red/white/navy flag.", ["#D0D0D0", "#C03030", "#203060"], "P3")
mt("model.machine.bio_blob", "textures/blocks/bio_blob.png", ttex("blocks/bio.png"),
   "Bio Blob block, all faces: saturated slime green, mottled.", ["#33A40C"], "P3")

# ----------------------------------------------------------------------------------------------
# 3. Block textures: machine overlays, multiblock casings, decorative blocks
# ----------------------------------------------------------------------------------------------
def overlay(mid, mname, slot, active, frames, frametime, brief, pri, pal=None, glow=False, fp=""):
    nm = "OVERLAY_%s%s%s" % (slot.upper(), "_ACTIVE" if active else "", "_GLOW" if glow else "")
    add("block.machine.%s.%s" % (mid, nm.lower()), "block", "textures/blocks/machines/%s/%s.png" % (mid, nm),
        16, 16 * frames, frames=frames, frametime=frametime, palette=pal or [], brief=brief, fp=fp, pri=pri)

PRESS_FP = ttex("blocks/AmmoPress.png") + " (model sheet, silhouette reference only)"
# Ammo Press, LV and up: ammo-press.md#animation layer 1 (8 slots + glow)
AP = {"front": (8, 2), "side": (4, 2), "top": (6, 3)}
for tag, steam, pri_active in (("ammo_press", False, "P1"), ("ammo_press_steam", True, "P2")):
    pri = pri_active
    for slot in ("front", "side", "top", "bottom"):
        for active in (False, True):
            if active and slot == "bottom":
                continue
            if steam:
                d = {"front": "ram strip, 8 frames, rivets and a pressure gauge",
                     "side": "belt strip scrolling, 4 frames, brass pipework",
                     "top": "hopper with steam puffs, 6 frames",
                     "bottom": "bare plate"}[slot]
            else:
                d = {"front": "press-ram strip, 8 frames, ram strikes on frame 5 and returns" if active else "closed ram hood with output slot",
                     "side": "ammo belt strip, 4 frames, boxed rounds scrolling" if active else "belt guide rail, dark steel",
                     "top": "hopper with rounds dropping, 6 frames" if active else "empty hopper mouth",
                     "bottom": "bare plate"}[slot]
            fr, ft = AP[slot] if (active and slot != "bottom") else (1, None)
            overlay(tag, "Ammo Press", slot, active, fr, ft,
                    "Ammo Press %soverlay, %s %s: %s. 16 px wide%s; per-tier tint comes from the GT hull casing, not this overlay."
                    % ("(Steam/Bronze) " if steam else "(LV-EV) ", slot, "active" if active else "inactive", d,
                       (", vertical strip of %d frames at %d ticks per frame (loops with the stroke)" % (fr, ft)) if fr > 1 else ""),
                    pri if (not steam) else "P2",
                    pal=["#808080", "#404040", "#E8A040"] if not steam else mc("Bronze", "Steel"), fp=PRESS_FP)
    if not steam:
        overlay(tag, "Ammo Press", "front", True, 8, 2,
                "Ammo Press glow layer for the active front: the ram head heats orange on the strike frames, transparent elsewhere. Same 8-frame strip as the front active overlay.",
                "P1", pal=["#FF8000", "#FFD060"], glow=True, fp=PRESS_FP)

# Repair Bench (LV-IV)
for slot in ("front", "side", "top", "bottom"):
    for active in (False, True):
        if active and slot == "bottom":
            continue
        fr, ft = ((4, 4) if (active and slot in ("front", "side", "top")) else (1, None))
        d = {"front": "shelf with jars, saw blade and screwdriver" + ("; a spark flickers at the bench vice (4 frames)" if active else ""),
             "side": "plain scratched metal" + ("; the clamp arm raises and lowers with a status lamp (4 frames)" if active else ""),
             "top": "pegboard with tools" + ("; a scanning light bar sweeps the bench top (4 frames)" if active else ""), "bottom": "bare plate"}[slot]
        overlay("repair_bench", "Repair Bench", slot, active, fr, ft,
                "Repair Bench overlay, %s %s: %s." % (slot, "active" if active else "inactive", d), "P3",
                pal=["#505050", "#705030", "#909090"], fp=ttex("blocks/repairbench_textures.png") + " (cube atlas, role reference only)")
# Camo Bench (LV), no active state
for slot in ("front", "side", "top", "bottom"):
    d = {"front": "three paint canisters (red, green, blue)", "side": "plain mottled metal",
         "top": "gray housing with lamps, louverd vent and rollers", "bottom": "bare plate"}[slot]
    overlay("camo_bench", "Camo Bench", slot, False, 1, None, "Camo Bench overlay, %s: %s. Cosmetic machine." % (slot, d), "P3",
            pal=["#515150", "#C03030", "#30A030", "#3050C0"], fp=ttex("blocks/paintmachine_textures.png") + " (cube atlas, role reference only)")
    if slot in ("front", "top"):   # polish pass: subtle active animation (spray LEDs, scanning light)
        overlay("camo_bench", "Camo Bench", slot, True, 4, 4,
                "Camo Bench overlay, %s active: %s (4-frame vertical strip at 4 ticks per frame)." % (
                    slot, "status LEDs step along the canister row and the nozzle puffs paint" if slot == "front" else "a scanning light bar sweeps the stencil tray"),
                "P3", pal=["#515150", "#C03030", "#30A030", "#3050C0"], fp="(new, polish pass)")
# Multiblock controllers
for mid, disp, tier, desc, pal in (
        ("ammunition_works", "Ammunition Works", "HV", "ammo crate and press-ram emblem; active: indicator row runs", mc("StainlessSteel", "Brass")),
        ("armory_gantry", "Armory Gantry", "HV", "hoist hook and cradle emblem; active: hook lowers", mc("Titanium", "StainlessSteel")),
        ("rampart", "Rampart", "HV", "crenellation emblem with a gun silhouette; active: red status light", mc("Steel", "Aluminium"))):
    overlay(mid, disp, "front", False, 1, None, "%s controller front overlay, inactive: %s. Controller tier %s." % (disp, desc.split(";")[0], tier), "P3", pal=pal, fp="(new, GT controller overlay pattern)")
    overlay(mid, disp, "front", True, 4, 3, "%s controller front overlay, active: %s. 4-frame strip, 3 ticks per frame." % (disp, desc.split(";")[1].strip()), "P3", pal=pal, fp="(new, GT controller overlay pattern)")
    overlay(mid, disp, "front", True, 4, 3, "%s controller active glow layer, same 4-frame strip." % disp, "P3", pal=pal, glow=True, fp="(new)")
# Gun Mount Hatch (MV-IV)
for active in (False, True):
    overlay("gun_mount_hatch", "Gun Mount Hatch", "front", active, 4 if active else 1, 3 if active else None,
            "Gun Mount Hatch overlay (Rampart hatch, MV-IV, tier tint from GT hatch casing): gun-clamp cradle %s." % ("with a gun silhouette locked in; clamp lamps chase around the ring (4 frames, 3 ticks per frame)" if active else "empty"),
            "P3", pal=["#505050", "#E0E0E0"], fp="(new)")

# Multiblock casings, GregGuns texture page 50
CASINGS = [
    ("works_casing", "Works Casing", "Ammunition Works hull. Ribbed gray-steel panel with rivet lines; plain, hatches allowed on feeder/packing positions.", mc("StainlessSteel"), 1, None),
    ("ram_housing", "Ram Housing", "Ammunition Works press column. Dark steel box with a slotted top hood, inactive.", mc("StainlessSteel", "Steel"), 1, None),
    ("ram_housing_active", "Ram Housing (active)", "Ram Housing active state: the same box with a piston rod stroking behind the hood slot, glow strip on the downstroke. 4-frame strip.", mc("StainlessSteel") + ["#FF8000"], 4, 4),
    ("reaction_casing", "Reaction Casing", "Retired inert decorative block (formerly the Retort hull). Smooth white-gray beveled panels with hazard-striped corner marks.", mc("Titanium", "Glass"), 1, None),
    ("gantry_casing", "Gantry Casing", "Armory Gantry hull. Painted blue-gray steel panel with yellow edge stripe.", mc("Titanium", "StainlessSteel"), 1, None),
    ("gantry_beam", "Gantry Beam", "Armory Gantry roof beam. I-beam end and side, yellow-black safety stripe.", mc("Titanium"), 1, None),
    ("gantry_hoist", "Gantry Hoist", "Armory Gantry hoist. Motor housing with cable drum; one face shows the hook block.", mc("Titanium", "Steel"), 1, None),
    ("repair_cradle_side", "Repair Cradle (side)", "Armory Gantry repair cradle side: padded clamp frame.", mc("Titanium", "Rubber"), 1, None),
    ("repair_cradle_top", "Repair Cradle (top)", "Armory Gantry repair cradle pad: gray rubber mat with clamp slots.", mc("Rubber", "Steel"), 1, None),
    ("bastion_plating_iron", "Bastion Plating, Iron (meta 0)", "Rampart wall plating, iron meta: plain riveted iron plate, armor value 5.", mc("Iron"), 1, None),
    ("bastion_plating_steel", "Bastion Plating, Steel (meta 1)", "Rampart wall plating, steel meta: darker riveted steel plate, armor value 10.", mc("Steel"), 1, None),
    ("bastion_plating_aluminum", "Bastion Plating, Aluminum (meta 2)", "Rampart wall plating, aluminum meta: light blue-gray plate, armor value 15.", mc("Aluminium"), 1, None),
    ("bastion_plating_carbon", "Bastion Plating, Carbon (meta 3)", "Rampart wall plating, carbon meta: near-black fiber-weave plate, armor value 20.", mc("EpoxidFiberReinforced"), 1, None),
]
for i, (cid, disp, desc, pal, fr, ft) in enumerate(CASINGS):
    add("block.casing." + cid, "block", "textures/blocks/casings/%s.png" % cid, 16, 16 * fr, frames=fr, frametime=ft, palette=pal,
        brief="%s - GT casing, texture page 50 (GregGuns casings, see metatileentity-framework.md). %s" % (disp, desc),
        fp="(new, GT casing 16x16)", pri="P3")

# Decorative blocks (07). Techguns footprints kept (32 px, ctm 64 px sheets).
DECOR = [  # (prefix pattern, family, brief, palette, pri)
    (r"container_", "Shipping container (Chisel CTM group)", "Corrugated steel container panel in the stated color; -ctm sheet carries the connected-texture tiles.", ["#8B2A2A"], "P3"),
    (r"concrete", "Concrete (Chisel CTM group)", "Poured concrete in the stated variant; hole = Zombie Hole top; -ctm/-ctmv sheets are connected tiles.", mc("Stone"), "P3"),
    (r"metalpanel2_", "Connected metal panel (metalpanel2)", "Connected-texture metal panel, stated variant.", mc("Iron"), "P3"),
    (r"metalpanel", "Metal panel", "Riveted metal panel, stated variant (dark, hazard-stripe, rusty).", mc("Iron"), "P3"),
    (r"metalgrate_", "Metal grate", "Floor grate, stated variant.", mc("Iron"), "P3"),
    (r"metalLadder|ladder01_inv", "Metal ladder", "Ladder model UV skin (rails and rungs), stated finish.", mc("Iron"), "P3"),
    (r"lamp0|lantern0", "Lamp / lantern", "Lamp or lantern face; lit, warm yellow or white.", ["#FFE88A", "#FAFAFA"], "P3"),
    (r"camoNet", "Camo net (pane and roof)", "Camouflage net in the stated pattern, transparent holes.", ["#3C5A28", "#E6EBEF", "#B49664"], "P3"),
    (r"sandbags", "Sandbags", "Stacked burlap sandbags, tan; top face is a flat bag top.", ["#B49664"], "P3"),
    (r"door01", "Bunker door", "Riveted gray steel door half (upper has window slit).", mc("Iron"), "P3"),
    (r"screen_|controlpanel_", "Radar screen / control panel", "Radar screen or control-panel face; overlay is an animated scan/blink strip.", ["#101010", "#30F060"], "P3"),
    (r"steelframe_", "Steel frame (Chisel CTM group)", "Open steel scaffold/frame block, stated finish.", mc("Steel"), "P3"),
    (r"bugnest", "Alien nest block", "Alien nest sand/slime/egg block (worldgen structure parts, hole = Bug Hole top).", ["#B0A070", "#303020"], "P3"),
    (r"airMarker", "Air marker", "Transparent block with a dark frame and the letters 'AIR'.", ["#303030"], "P3"),
]
for p, f in sorted(TEX.items()):
    if not p.startswith("textures/blocks/"):
        continue
    stem = p[len("textures/blocks/"):-4]
    if stem in ("ladder01", "steelframe_glass", "bio"):
        continue
    for pat, fam, brief, pal, pri in DECOR:
        if re.match(pat, stem):
            rid = snake(stem).replace("-", "_")
            add("block.decor." + rid, "block", "textures/blocks/%s.png" % rid, f["w"], f["h"], frames=f["frames"],
                frametime=(2 if f["frames"] > 1 else None), palette=pal,
                brief="%s, '%s'. %s Footprint kept (%dx%d%s)." % (fam, stem, brief, f["w"], f["h"],
                                                                 ", %d-frame strip" % f["frames"] if f["frames"] > 1 else ""),
                fp=p, pri=pri)
            break

# ----------------------------------------------------------------------------------------------
# 4. GUI textures (256x256 sprite sheets used through UITexture.fullImage)
# ----------------------------------------------------------------------------------------------
GUIS = [
    ("hud", "HUD sprite sheet: ammo-type icons, magazine/clip pips, armor pips (full/half/empty), hit-marker, sprint/charge bar caps. Transparent background, 256x256 sheet; only the used regions are painted.", "gui/TGPlayerInventory.png", "P1", ["#FFFFFF", "#C80000", "#FFB400"]),
    ("ammo_press", "Ammo Press panel sprites: progress arrow, ram-stroke gauge (8 pips), input slot overlays (bullet, casing, powder, magazine) and output slot frame. Uses GT panel background; only sprites are painted.", "gui/ammoPress_GUI.png", "P1", mc("Steel") + ["#E8A040"]),
    ("ammo_pouch", "Ammo Pouch 6-slot GUI sprites: pouch frame, slot overlays.", "gui/TGPlayerInventory.png", "P2", ["#6B4A2B"]),
    ("repair_bench", "Repair Bench panel sprites: damage bar, tier badge, input/output frames.", "gui/repairBench_GUI.png", "P3", ["#909090", "#705030"]),
    ("camo_bench", "Camo Bench panel sprites: camo index arrows, preview frame.", "gui/camoBench_GUI.png", "P3", ["#515150"]),
    ("turret", "Turret panel sprites: 9 ammo slots, security toggles, health bar.", "gui/turretBase_GUI.png", "P3", ["#989896"]),
    ("explosive_charge", "Explosive Charge panel sprites: timer digits, arm button, owner lock icon.", "gui/expChargeGui.png", "P3", ["#303030", "#C80000"]),
    ("advanced_explosive_charge", "Advanced Explosive Charge panel sprites: timer digits, arm button, remote-link icon.", "gui/tntChargeGui.png", "P3", ["#901010"]),
    ("storage_chest", "Reinforced / Weapon Chest GUI sprites: slot grid frame and sort-button art.", "gui/tgchest_gui.png", "P3", ["#989898"]),
    ("multiblock_widgets", "Shared multiblock panel sprites for Ammunition Works, Armory Gantry and Rampart: tier-belt indicator, pressure gauge, bay indicators, mount status LEDs.", None, "P3", mc("Titanium")),
    ("gun_mount_hatch", "Gun Mount Hatch panel sprites: single gun slot, lock icon.", None, "P3", ["#505050"]),
]
for gid, brief, ref, pri, pal in GUIS:
    add("gui." + gid, "gui", "textures/gui/%s.png" % gid, 256, 256, palette=pal, brief=brief,
        fp=(ttex(ref) + " (layout reference only)") if ref else "(new, no Techguns GUI)", pri=pri)
# Full-screen scope overlays (512x512), generated by gui.py.
for sid, brief, pal in [
    ("sniper", "Sniper scope overlay (sniper rifles, bolt-action and AS50 style): 512x512 RGBA, transparent circular lens with fine crosshair and mil-dots, black surround with soft vignette ring. Stretched to the screen height.", ["#000000", "#303030"]),
    ("assault", "Assault / reflex scope overlay (rifles with a zoom toggle): 512x512 RGBA, smaller transparent view circle in a thick dark housing, red chevron reticle with drop ticks.", ["#080A0A", "#FF281E"]),
    ("energy", "Energy / tech scope overlay (laser, pulse and plasma weapons): 512x512 RGBA, alpha-blended cyan HUD brackets, ring reticle with ticks, scanline vignette, transparent center.", ["#40E0FF", "#000A10"]),
]:
    add("gui.scope_" + sid, "gui", "textures/gui/scopes/%s.png" % sid, 512, 512, palette=pal, brief=brief,
        fp="(new, no Techguns GUI)", pri="P2")

# ----------------------------------------------------------------------------------------------
# 5. Particles / FX (04)
# ----------------------------------------------------------------------------------------------
FX = [  # name, frames, grid note, pri, role, palette
    ("muzzleflash2", 8, "4x4 grid of 64 px", "P1", "yellow four-pointed muzzle flash: pistol and light ballistic guns", ["#F0D050", "#FFFFFF"]),
    ("muzzleflash4x4", 10, "4x4 grid of 64 px", "P2", "orange explosive muzzle puff (revolver, shotguns, hand cannon)", ["#F07050", "#FFFFFF", "#D0D0D0"]),
    ("Explosion", 20, "5x5 grid of 96 px", "P2", "fireball explosion life cycle: flicker, yellow-white core, red-orange rim, smoke wisps", ["#FFE070", "#FF6020"]),
    ("fireblast", 20, "5x5 grid", "P2", "full explosion cycle: seed, white flash, fire, char, gray smoke", ["#FFFFFF", "#FF9020", "#808080"]),
    ("dustclouds02", 9, "3x3 grid", "P2", "soft smoke puff for launcher trails and explosion smoke", ["#787878"]),
    ("largesmoke", 4, "2x2 grid of 256 px (variations)", "P2", "large billowing smoke clouds, dense to faint", ["#957F80"]),
    ("shockwave", 1, "single image", "P2", "faint thin ring for shockwaves", ["#E0E0E0"]),
    ("flamethrower", 16, "4x4 grid of 128 px", "P2", "turbulent fireball sequence", ["#FF9020", "#FFE070"]),
    ("flamethrower2", 16, "4x4 grid of 128 px", "P2", "upward flame tongues for the flamethrower muzzle", ["#FF9020", "#FFE070"]),
    ("flamethrower_a", 16, "4x4 grid of 128 px", "P2", "fire ball with a soft smoky halo (alpha-blended)", ["#FFC040", "#FFFFFF"]),
    ("Testscope", 1, "single full-screen image", "P2", "sniper scope: opaque black with a circular window and thin cross-hair", ["#000000"]),
    ("alienflare", 14, "4x4 grid of 128 px", "P3", "violet-magenta energy flare life cycle", ["#C040FF", "#FFFFFF"]),
    ("blasterflare01", 14, "4x4 grid of 128 px", "P3", "orange-yellow energy flare life cycle (blaster, nether, nuke)", ["#FFA030", "#FFFFFF"]),
    ("teslaflare01", 14, "4x4 grid of 128 px", "P3", "electric blue flare with a ring mid-phase", ["#40A0FF", "#E0FFFF"]),
    ("laserflare02", 7, "4x2 used of 4x4 grid", "P3", "red lens flare with radial streaks", ["#FF2020", "#FFFFFF"]),
    ("blobs", 16, "4x4 grid of 128 px (variations)", "P3", "lime-green slime globs with highlights", ["#80E020", "#A0FFFF"]),
    ("splat", 6, "3x3 grid of 128 px (variations)", "P3", "thin vein-like splatter lines, grayscale for tinting", ["#E0E0E0"]),
    ("blood4x4", 4, "2x2 grid of 128 px (variations)", "P3", "blood splatter puffs, grayscale for tinting", ["#858585"]),
    ("impact", 4, "2x2 grid of 128 px", "P3", "jagged white impact shards (power hammer)", ["#F0F0F0"]),
    ("muzzleflash_blue", 8, "4x4 grid of 64 px", "P3", "tri-blade blue energy muzzle flash", ["#9CB5FF"]),
    ("nukebeamflare", 16, "4x4 grid of 128 px", "P3", "golden plasma orb with concentric rings (Nuclear Death Ray)", ["#FFD040", "#FFFFFF"]),
    ("sonicwave4x4", 16, "4x4 grid of 256 px", "P3", "sonic ring: tiny ring, bright donut, sparkles, fade", ["#8090FF", "#FFFFFF"]),
    ("nukebeam", 17, "1x17 strip of 128x64", "P3", "horizontal nuclear plasma band, 17 frames, one frame per 2 ticks", ["#FFD040", "#B06020"]),
    ("lensflare1", 1, "single image", "P3", "soft round radial glow, tinted in use", ["#FFFFFF"]),
    ("lensflare3", 1, "single image", "P3", "anamorphic streak flare with a small star core", ["#FFFFFF"]),
    ("laser3", 1, "single image", "P3", "red laser beam band, pink-white core, tiles along u", ["#FF4040", "#FFD0D0"]),
    ("laser_blue", 1, "single image", "P3", "blue tesla beam band, white-cyan core", ["#2060FF", "#E0FFFF"]),
    ("laserDeathfx", 1, "tileable", "P3", "laser death overlay: molten cracks, orange on dark red-black", ["#FF6020", "#300808"]),
    ("bio", 1, "tileable", "P3", "bio death overlay: mottled slime green", ["#5DB049"]),
    ("heatray", 1, "single image", "P3", "heat-ray teardrop flame, retained but unused since 0012, black background", ["#FFE070", "#808030"]),
    ("techscope", 1, "single full-screen image", "P3", "sci-fi scope overlay: black border, cyan hex reticle and brackets", ["#00E0E0", "#021616"]),
]
for name, fr, grid, pri, role, pal in FX:
    p = "textures/fx/%s.png" % name
    f = TEX[p]
    rid = snake(name).replace("4x4", "4x4")
    if name == "nukebeam":   # polish pass: square 128x128 frames so the strip is valid in a 1.7.10 atlas
        f = dict(f, w=128, h=128 * 17)
        grid = "1x17 strip of 128x128 square frames (re-laid out from 128x64 frames; the beam band sits in the middle rows)"
    add("particle." + rid, "particle", "textures/fx/%s.png" % rid, f["w"], f["h"], frames=fr,
        frametime=(2 if name == "nukebeam" else None), palette=pal,
        brief="FX sprite (%s%s): %s. Painted from scratch; additive sheets are black-background, alpha sheets transparent.%s" % (
            grid, ", %d frames" % fr if fr > 1 else "", role,
            " Footprint size is huge; a power-of-two of at least 1024 px is acceptable if the window ratio is kept." if f["w"] >= 2048 else ""),
        fp=p, pri=pri)

# ----------------------------------------------------------------------------------------------
# 6. Sounds
# ----------------------------------------------------------------------------------------------
EXCL_SND = ("sounds/test.ogg",)
EXCL_PRE = ("sounds/items/jetpack", "sounds/machines/charginstation", "sounds/machines/chemlab",
            "sounds/machines/fabricator", "sounds/machines/metalpress", "sounds/machines/oredrill")
P1_SND = re.compile(r"sounds/(guns/pistol_(shot_[abc]|reload)\.|guns/pistol_shot_|effects/bullet_impact_|machines/ammopress_work)")
P3_GUN_EVENTS = ("lasergun", "tesla", "sonicshotgun", "biogun", "blasterrifle", "MIBblaster", "alienblaster",
                 "cyberdemonblaster", "beamgun", "pdw", "pulserifle", "minigun", "powerhammer", "chainsaw")
SND_BRIEF_ROLE = {
    "ammopress_work1": "Ammo Press working ambience (loops while active)",
    "ammopress_work2": "Ammo Press ram stroke (plays at each stroke bottom, see ammo-press.md Sound)",
}
skipped = []
for p, f in sorted(SND.items()):
    if p in EXCL_SND or p.startswith(EXCL_PRE) or "no sounds.json" in f["event"]:
        skipped.append(p)
        continue
    a = AUD[p]
    ev = f["event"].split("[")[0].strip()
    cat = p.split("/")[1] if p.count("/") > 1 else "misc"
    if P1_SND.search(p) and "akimbo" not in p:
        pri = "P1"
    elif cat == "guns":
        evn = ev.split(".")[-1]
        pri = "P3" if any(evn.startswith(x) for x in P3_GUN_EVENTS) or ev.startswith("effects.biogun") else "P2"
    elif cat in ("npcs", "blocks") or p.startswith("sounds/effects/gore") or p.endswith("biodeath.ogg") \
            or p.endswith("disintegrate.ogg") or p.endswith("nukeexplosion.ogg") or "Charge" in p or p.startswith("sounds/items/glider"):
        pri = "P3"
    else:
        pri = "P2"
    ch = 2 if p.endswith("glider_loop_stereo.ogg") else 1
    sr = 48000 if f["rate"] == 48000 else 44100
    note = ""
    if f["rate"] not in (44100, 48000):
        note = " Original rate %d Hz is not a delivery target: render at %d Hz." % (f["rate"], sr)
    if f["ch"] == "stereo" and ch == 1:
        note += " Original is stereo; positional sounds are mono (GVA-23), deliver mono."
    role = next((v for k, v in SND_BRIEF_ROLE.items() if k in p), "")
    brief = "%s%s. Reads as: %s Match length (%.2f s) and loudness within +/-3 LU of %.1f LUFS; peak at or below -1 dBFS.%s" % (
        (role + ". ") if role else "", ev, a["desc"][0].lower() + a["desc"][1:] if a["desc"] else "", f["dur"], a["lufs"], note)
    add("snd." + p[len("sounds/"):-4].replace("/", "."), "sound", p, dur=f["dur"], ch=ch, sr=sr, lufs=a["lufs"],
        brief=brief, fp=p, pri=pri)
# New sounds (no Techguns file)
add("snd.guns.flashbang_bang", "sound", "sounds/guns/flashbang_bang.ogg", dur=1.4, ch=1, sr=44100, lufs=-12.0,
    brief="Flashbang detonation (new design, Techguns never registered one): a sharp bright crack with a short ringing high-frequency tail and a quick thump underneath. Mono, within +/-3 LU of -12 LUFS; no reverb beyond 0.6 s.",
    fp="(none; role reference sounds/effects/explosion1_d.ogg, 1.21 s)", pri="P2")

# ----------------------------------------------------------------------------------------------
# 7. Branding and infographics
# docs/assets/branding and docs/assets/infographics are produced by tools/asset-gen/branding.py.
# Files that already exist are listed with their real size and marked delivered.
# ----------------------------------------------------------------------------------------------
import struct

PAL_BRAND = mc("Steel", "Bronze") + ["#101418", "#F2F2F2"]


def png_size(rel):
    fp = os.path.join(ROOT, rel)
    if not os.path.exists(fp) or not rel.endswith(".png"):
        return None
    with open(fp, "rb") as f:
        h = f.read(24)
    return struct.unpack(">II", h[16:24]) if h[1:4] == b"PNG" else None


def asset_doc(id, kind, rel, w, h, brief, pri):
    sz = png_size(rel)
    done = os.path.exists(os.path.join(ROOT, rel))
    if sz:
        w, h = sz
    add(id, kind, rel, w, h, palette=PAL_BRAND,
        brief=brief + (" [Delivered: generated by tools/asset-gen/branding.py.]" if done else " [Not yet produced.]"),
        fp="(new)", pri=pri)


B = "docs/assets/branding/"
asset_doc("brand.logo_svg", "branding", B + "logo.svg", None, None, "Master logo (vector): steel hex casing, rifle silhouette, GT blue cable, orange bullet.", "P2")
asset_doc("brand.logo_128", "branding", B + "logo_128.png", 128, 128, "Mod icon for mcmod.info logoFile (copy to src/main/resources/ at build); export of logo.svg.", "P2")
asset_doc("brand.logo_512", "branding", B + "logo_512.png", 512, 512, "Large export of logo.svg for the Modrinth/CurseForge page.", "P3")
asset_doc("brand.logo_pixel_svg", "branding", B + "logo_pixel.svg", None, None, "Pixel-crisp 32x32 logo variant for small sizes (vector source).", "P3")
for n in (32, 128, 256):
    asset_doc("brand.logo_pixel_%d" % n, "branding", B + "logo_pixel_%d.png" % n, n, n, "Pixel-crisp logo variant export at %d px." % n, "P3")
asset_doc("brand.banner_svg", "branding", B + "banner.svg", None, None, "README banner source (vector).", "P2")
asset_doc("brand.banner", "branding", B + "banner.png", 1280, 320, "README banner (1280x320): wordmark, tier gate-material swatches.", "P2")
asset_doc("brand.social_preview_svg", "branding", B + "social_preview.svg", None, None, "GitHub social preview source (vector).", "P3")
asset_doc("brand.social_preview", "branding", B + "social_preview.png", 1280, 640, "GitHub social preview (1280x640), keep key content inside the central 1000x500.", "P3")
I = "docs/assets/infographics/"
INFO = [
    ("hero", "hero", "README hero banner: armored players across the tiers facing a Super Mutant.", "P2"),
    ("getting_started", "getting-started", "Player guide: the first six steps, from the Hand Cannon to a magazine-fed pistol, plus default keys.", "P2"),
    ("arsenal_steam_to_hv", "arsenal-steam-to-hv", "Player guide: every weapon from Steam to HV, rendered, with role and ammo.", "P2"),
    ("arsenal_ev_to_zpm", "arsenal-ev-to-zpm", "Player guide: every weapon from EV to ZPM, rendered, with role and ammo or charge; notes that Bio Gun blobs need explosions.blockDamage.", "P2"),
    ("ammo_and_magazines", "ammo-and-magazines", "Player guide: GT materials to parts to the Ammo Press to magazines; magazine sizes; ammo per tier.", "P2"),
    ("armor_and_gear", "armor-and-gear", "Player guide: the twelve armor sets on figures, masks and back gear, damage types.", "P2"),
    ("machines", "machines", "Player guide: each single block machine beside its multiblock, rendered; notes the Works' optional filling bench.", "P2"),
    ("multiblock_ammunition_works", "multiblock-ammunition-works", "Build guide: Ammunition Works, with block counts, sections, the required interior lamp, the optional Magazine Filling Bench, hatches and scaling.", "P3"),
    ("multiblock_armory_gantry", "multiblock-armory-gantry", "Build guide: Armory Gantry, with block counts, sections, the required interior lamp, hatches and scaling.", "P3"),
    ("multiblock_rampart", "multiblock-rampart", "Build guide: Rampart, with block counts, sections, hatches and scaling.", "P3"),
    ("enemies", "enemies", "Player guide: mobs by danger, rendered, plus structures and loot.", "P2"),
    ("questbook", "questbook", "Player guide: the GregGuns questbook chapters, side branches and rewards.", "P2"),
    ("recipes_steam_and_lv", "recipes-steam-and-lv", "Player guide: the Steam and LV crafting chains, crafting-table grids and Assembler and Ammo Press rows, read from the mod's recipe tables.", "P2"),
    ("icbm_warheads", "icbm-warheads", "Player guide (DRAFT, stamped; numbers pending): the Tier 1, Tier 2, Tier 3 and nuclear warheads rendered from the shipped models, every T1-T2 blast type, fill and radius, damage falloff, demolition-charge use, and the GT machines that make them. Shows HBM's Nuclear Tech Mod warhead models (LGPL-3.0).", "P2"),
    ("icbm_missile_silo", "icbm-missile-silo", "Build guide (DRAFT, stamped; numbers pending): the Missile Silo multiblock, layer by layer, with block counts, hatches and a Tier 3 missile standing in the shaft. Shows HBM's Nuclear Tech Mod missile models (LGPL-3.0). Built by tools/asset-gen/infographics/build.py; no vector source.", "P2"),
    ("icbm_first_missile", "icbm-first-missile", "Player guide (DRAFT, stamped; numbers pending): the Tier 1 and Tier 2 missiles on the Galacticraft pad, and the steps from chip and schematic to body, stacking, fuel, launch control and impact. Shows HBM's Nuclear Tech Mod missile and warhead models (LGPL-3.0).", "P2"),
]
for iid, fn, brief, pri in INFO:
    path = os.path.join(ROOT, I + fn + ".png")
    w, h = Image.open(path).size if os.path.exists(path) else (1600, None)
    asset_doc("info." + iid, "infographic", I + fn + ".png", w, h,
              brief + " Built by tools/asset-gen/infographics/build.py; no vector source.", pri)

# ----------------------------------------------------------------------------------------------
# Output
# ----------------------------------------------------------------------------------------------
KINDS = ["icon", "model", "block", "gui", "particle", "sound", "branding", "infographic"]


def size_str(e):
    if e["kind"] == "sound":
        return "%.2f s, %s, %d Hz, %.1f LUFS" % (e["duration_s"], "stereo" if e["channels"] == 2 else "mono", e["sample_rate"], e["target_lufs"])
    if e["width"] is None:
        return "vector" if e["path"].endswith(".svg") else "mesh"
    s = "%dx%d" % (e["width"], e["height"])
    if e["frames"] and e["frames"] > 1:
        s += ", %d frames" % e["frames"] + (" @%dt" % e["frametime"] if e["frametime"] else "")
    return s


def esc(s):
    return s.replace("|", "/").replace("\n", " ")


def table(rows):
    out = ["| ID | Target path | Size / frames / audio | Brief | Palette | Techguns footprint | Pri |", "|---|---|---|---|---|---|---|"]
    for e in rows:
        out.append("| `%s` | `%s` | %s | %s | %s | %s | %s |" % (
            e["id"], e["path"], size_str(e), esc(e["brief"]), " ".join(e["palette"]) or "n/a",
            ("`%s`" % e["footprint_ref"]) if e["footprint_ref"].startswith(("textures", "sounds", "models")) and " " not in e["footprint_ref"] else esc(e["footprint_ref"]) or "n/a",
            e["priority"]))
    return "\n".join(out)


def build_md():
    c = {k: sum(1 for e in ENTRIES if e["kind"] == k) for k in KINDS}
    pc = {p: sum(1 for e in ENTRIES if e["priority"] == p) for p in ("P1", "P2", "P3")}
    L = []
    A = L.append
    A("# Asset manifest\n")
    A("Every non-code asset GregGuns needs, derived from the design records ([0001](../design/0001-tier-map.md), [0002](../design/0002-machine-deconfliction.md), [0006](../design/0006-art-direction.md), [0009](../design/0009-gtnh-integration-defaults.md), [0010](../design/0010-material-remap-and-tier-additions.md), [0011](../design/0011-machine-policy.md)), the spec sheets and the measured Techguns footprints in [ASSET-FACTS.md](../techguns-spec/ASSET-FACTS.md) and [AUDIO-PROFILES.md](../techguns-spec/AUDIO-PROFILES.md).\n")
    A("The machine-readable twin is [tools/asset-gen/manifest.json](../../tools/asset-gen/manifest.json), built by `python -I tools/asset-gen/build_manifest.py`. Edit the script, not the generated files.\n")
    A("**Totals:** %d assets. By kind: %s. By priority: %s.\n" % (
        len(ENTRIES), ", ".join("%s %d" % (k, c[k]) for k in KINDS), ", ".join("%s %d" % (k, v) for k, v in pc.items())))
    A("## Conventions\n")
    A("- **Target paths** are under `src/main/resources/assets/gregguns/` unless they start with `docs/assets/`.")
    A("- **Techguns footprint** is a path in the Techguns resource tree used only for its size, frame count, color or duration. Nothing is copied from it ([CONTRIBUTING § Originality](../../CONTRIBUTING.md#originality-the-techguns-boundary)).")
    A("- **Item icons** are 16x16 GT-style ([0006](../design/0006-art-direction.md)). Palettes come from the GT material color (`MaterialsInit.java`, 5.09.54.205); items made of a material use that color. Rubber is `#000000` in GT: paint it as a lifted dark gray (`#2A2A2E`) so it reads.")
    A("- **Model textures** keep the Techguns size and UV layout. **Decorative block textures** keep the Techguns footprint (32 px, connected-texture sheets 64 px). **Machine overlays and multiblock casings** are GT-native 16 px; animated ones are vertical strips with a `.png.mcmeta` (`frametime` in ticks).")
    A("- **Sounds** match role, length and loudness: integrated loudness within +/-3 LU of the measured original (`target_lufs`), peak at or below -1 dBFS, positional sounds mono (GVA-23). Delivery rate is 44.1 kHz, or 48 kHz where the original was 48 kHz. Stereo only for the glider loop (player-bound ambience).")
    A("- **Priority:** **P1** is the M1 vertical slice (9mm Pistol, its ammo and magazine, its sounds, the LV Ammo Press and its HUD/GUI). **P2** is M2 to M4 content (ballistic arsenal, armor, production chain, branding basics). **P3** is M5 onwards (energy and exotic weapons, utility and defense, NPCs, worldgen), camo and color variants, social and infographic art.")
    A("- Provenance for every delivered asset goes in [CREDITS.md](CREDITS.md).\n")
    groups = [
        ("Item icons", "icon", None),
        ("Model, armor and entity textures", "model", None),
        ("Block textures: machine overlays, multiblock casings, decorative blocks", "block", None),
        ("GUI textures", "gui", None),
        ("Particles and FX", "particle", None),
        ("Sounds", "sound", None),
        ("Branding", "branding", None),
        ("Infographics", "infographic", None),
    ]
    sub = {
        "icon": [("Guns, melee and grenades", lambda e: e["id"].split(".")[1] in [g[0] for g in GUNS]),
                 ("Gun parts, emitters, plating and cloth", lambda e: e["id"].startswith("icon.part_")),
                 ("Ammunition", lambda e: e["id"].startswith("icon.ammo_") and e["id"] != "icon.ammo_pouch"),
                 ("Magazines, tanks and cells", lambda e: e["id"].startswith(("icon.mag_", "icon.tank_", "icon.cell_"))),
                 ("Armor sets", lambda e: re.match(r"icon\.(t\d_|steam_)", e["id"]) and not e["id"].startswith("icon.t2_beret")),
                 ("Equipment, consumables, placers and misc", lambda e: True)],
        "model": [("Gun model sheets", lambda e: e["id"].startswith("model.gun.")),
                  ("Armor worn-model textures", lambda e: e["id"].startswith("model.armor.")),
                  ("Mobs, gibs and projectiles", lambda e: e["id"].startswith(("model.entity.", "model.obj."))),
                  ("Machine, charge, chest and flag model sheets", lambda e: True)],
        "block": [("Machine overlays (GT eight-slot layout plus glow)", lambda e: e["id"].startswith("block.machine.")),
                  ("Multiblock casings (GT texture page 50)", lambda e: e["id"].startswith("block.casing.")),
                  ("Decorative blocks (07)", lambda e: True)],
        "sound": [("Guns", lambda e: e["path"].startswith("sounds/guns/")),
                  ("Machines", lambda e: e["path"].startswith("sounds/machines/")),
                  ("Effects, blocks and items", lambda e: e["path"].startswith(("sounds/effects/", "sounds/blocks/", "sounds/items/"))),
                  ("NPCs and turret", lambda e: True)],
    }
    n = 2
    for title, kind, _ in groups:
        rows = [e for e in ENTRIES if e["kind"] == kind]
        A("## %d. %s (%d)\n" % (n, title, len(rows)))
        n += 1
        if kind in sub:
            left = list(rows)
            for stitle, pred in sub[kind]:
                sel = [e for e in left if pred(e)]
                left = [e for e in left if e not in sel]
                if sel:
                    A("### %s (%d)\n" % (stitle, len(sel)))
                    A(table(sel) + "\n")
        else:
            A(table(rows) + "\n")
    A("## 10. Excluded on purpose\n")
    A("Not in the manifest because GregGuns dropped them or GT supplies them.\n")
    A("- **Ores, ore clusters, oil, fluids** (`oreCluster*`, `oreTitanIron`, `oreUranium`, `ore*Block`, `acid_*`, `milk_*`, creeper acid bucket): 0002/0005, GT owns ores and fluids.")
    A("- **Techguns materials** (ingots, nuggets, plates, wires, circuit boards, rubber, plastic, carbon fibers, coil, ore items, obsidian steel): 0002, GT materials.")
    A("- **Dropped machines** (Metal Press, Chem Lab, Fabricator, Charging Station, Ore Drill, Scrapper): their block sheets, ore-drill frame/rod/head textures, GUIs and work sounds. Machine upgrade item (`machineStackUpgrade`) and the Techguns `paintmachine_*` / legacy `rc_*` block files.")
    A("- **Energy cell** (`cell_energy`, `energyCell*`): 0010. **Jetpack** (item, worn texture and four sounds): 0009. **Electric engine** item: GT motors.")
    A("- **Debug and authoring tools** (`blockReaderTool`, `structureScanTool`, `structureTestTool`, `oreClusterScanner`, `tgx`, `testItem`) and `TGPlayerInventory` as a tab GUI (Baubles Expanded replaces it; only the HUD sprites are kept, as `gui.hud`).")
    A("- **Unused Techguns files** (listed unused in the area texture sheets): `pistol.png`, `pistol2Texture.png`, `mp5Texture.png`, `neutrinogun.png`, `drillTexture.png`, old-style plates, unused fx sheets (laserflare01, laserFlash2x2, lightning*, lensflare2/4, heatray2, glow, blood01, fireball*), `tent`, `flagBlock`, `steelframe_glass`, `Bioblock`.")
    A("- **Sounds dropped:** `test.ogg`, four files in no `sounds.json` event (`beamgun_reload`, `chainsaw_loop_d`, `minigun_start_b/c`), the mono `glider_loop.ogg` (the stereo loop is kept), jetpack and dropped-machine sounds (%d files skipped)." % len(skipped))
    A("- **Vanilla-textured mobs:** Skeleton Soldier and Pigman Soldier use vanilla skins and need no art.\n")
    A("## 11. Notes and non-image data\n")
    A("- **Structures (11-worldgen)** need no textures. Structure templates, loot tables and `sounds.json`/lang files are data, written alongside the code (structure files are written fresh, never copied).")
    A("- **Sounds not in this list:** Rampart, Gantry and Ammunition Works have no sound brief yet ([ammo-press.md § Sound](../gregguns-spec/08-machines-production/ammo-press.md#sound) asks only for the stroke hook). The Ammo Press stroke reuses the `ammopress_work2` role.")
    A("- **Armored-turret variants** use the five Techguns turret sheets; the obsidian-steel variant becomes aluminum (0010).")
    A("- **Recolor rule:** one gun sheet serves its akimbo form; the Flashbang recolors the Frag Grenade sheet.\n")
    return "\n".join(L)


CREDITS = """# Credits

Provenance for every GregGuns asset ([0006](../design/0006-art-direction.md#decision)). One row per delivered file. Generated assets record the tool and prompt or generator and seed. All assets are released under the MIT license (see [LICENSE](../../LICENSE)).

Techguns is All Rights Reserved: no Techguns pixel, sample or model is used. Techguns paths in [MANIFEST.md](MANIFEST.md) are footprint references only.

| Asset | Author / tool | Method / prompt or generator + seed | License | Date |
|---|---|---|---|---|
| _(example)_ `textures/items/pistol_9mm.png` | _tool name and version_ | _prompt text, or generator script and seed_ | MIT | _YYYY-MM-DD_ |
"""


def main():
    for e in ENTRIES:
        assert e["priority"] in ("P1", "P2", "P3"), e
        assert e["kind"] in KINDS, e
    out_json = os.path.join(ROOT, "tools", "asset-gen", "manifest.json")
    with open(out_json, "w", encoding="utf8", newline="\n") as f:
        json.dump(ENTRIES, f, indent=1, ensure_ascii=False)
        f.write("\n")
    os.makedirs(os.path.join(ROOT, "docs", "assets"), exist_ok=True)
    with open(os.path.join(ROOT, "docs", "assets", "MANIFEST.md"), "w", encoding="utf8", newline="\n") as f:
        f.write(build_md() + "\n")
    cp = os.path.join(ROOT, "docs", "assets", "CREDITS.md")
    if not os.path.exists(cp):
        with open(cp, "w", encoding="utf8", newline="\n") as f:
            f.write(CREDITS)
    from collections import Counter
    print(len(ENTRIES), "entries")
    print(Counter(e["kind"] for e in ENTRIES))
    print(Counter(e["priority"] for e in ENTRIES))
    print("skipped sounds:", len(skipped))


if __name__ == "__main__":
    sys.exit(main())
