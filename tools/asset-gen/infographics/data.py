"""Player-facing facts the infographics print. Written for GTNH players, describing the finished mod."""

GATES = {
    "Steam": "Bronze, Wrought Iron", "LV": "Steel", "MV": "Aluminum", "HV": "Stainless Steel", "EV": "Titanium",
    "IV": "Tungstensteel", "LuV": "HSS-G", "ZPM": "Naquadah Alloy",
}
EU = {"Steam": "steam", "LV": "32 EU/t", "MV": "128 EU/t", "HV": "512 EU/t", "EV": "2,048 EU/t",
      "IV": "8,192 EU/t", "LuV": "32,768 EU/t", "ZPM": "131,072 EU/t"}

# (stem, display name, role, ammo item stem or None, ammo label)
WEAPONS = {
    "Steam": [
        ("handcannon", "Hand Cannon", "Slow, single-shot stone cannon", "ammo_stone_bullet", "Stone Bullets"),
    ],
    "LV": [
        ("pistol_9mm", "9mm Pistol", "Semi-auto sidearm, 18 rounds", "mag_9mm_pistol", "9mm Pistol Magazine"),
        ("revolver", "Revolver", "Six shots, hits harder", "ammo_9mm_bullet", "9mm Bullets"),
        ("sawed_off", "Sawed Off", "Two barrels, close range", "ammo_shotgun_shell", "Shotgun Shells"),
        ("bolt_action_rifle", "Bolt Action Rifle", "Scoped long-range rifle", "ammo_rifle_bullet", "Rifle Bullets"),
        ("item:combat_knife", "Combat Knife", "Fast steel melee", None, "no ammo"),
        ("item:crowbar", "Crowbar", "Melee that mines like iron", None, "no ammo"),
        ("item:stielgranate", "Stielgranate", "Thrown stick grenade", None, "thrown"),
    ],
    "MV": [
        ("thompson_smg", "Thompson SMG", "Full auto, 20 rounds", "mag_smg", "SMG Magazine"),
        ("mac10", "Mac10", "Very fast, loose spread", "mag_smg", "SMG Magazine"),
        ("combat_shotgun", "Combat Shotgun", "Eight-shell pump action", "ammo_shotgun_shell", "Shotgun Shells"),
        ("m4", "M4", "Accurate, scoped, 30 rounds", "mag_assault_rifle", "Assault Rifle Magazine"),
        ("ak47", "AK47", "Hits harder, 30 rounds", "mag_assault_rifle", "Assault Rifle Magazine"),
        ("akimbo:pistol_9mm", "Akimbo Pistols", "Two pistols, alternating", "mag_9mm_pistol", "2 Pistol Magazines"),
        ("akimbo:revolver", "Akimbo Revolvers", "Two revolvers, alternating", "ammo_9mm_bullet", "9mm Bullets"),
        ("akimbo:mac10", "Akimbo Mac10s", "Both fire every pull", "mag_smg", "2 SMG Magazines"),
        ("item:frag_grenade", "Frag Grenade", "Hold to throw harder", None, "thrown"),
        ("item:flashbang", "Flashbang", "Blinds anyone looking", None, "thrown"),
    ],
    "HV": [
        ("aug", "AUG", "Scoped, very accurate", "mag_assault_rifle", "Assault Rifle Magazine"),
        ("scar", "SCAR", "Heavy hits, pierces armor", "mag_assault_rifle", "Assault Rifle Magazine"),
        ("vector", "Vector", "Fast-firing SMG", "mag_smg", "SMG Magazine"),
        ("infiltrator", "The Infiltrator", "Silenced M4", "mag_assault_rifle", "Assault Rifle Magazine"),
        ("as50", "AS50", "Anti-armor sniper rifle", "mag_as50", "AS50 Magazine"),
        ("lmg", "LMG", "100-round suppressor", "mag_lmg", "LMG Magazine"),
        ("grenade_launcher", "Grenade Launcher", "Six bouncing 40mm shells", "ammo_40mm_grenade", "40mm Grenades"),
        ("rocket_launcher", "Rocket Launcher", "One big explosive rocket", "ammo_rocket", "Rockets"),
        ("flamethrower", "Flamethrower", "Short-range fire stream", "tank_fuel", "Fuel Tank"),
        ("chainsaw", "Chainsaw", "Fuel-powered melee saw", "tank_fuel", "Fuel Tank"),
        ("power_hammer", "Power Hammer", "Air-powered smash", "tank_air", "Compressed Air Tank"),
    ],
    "EV": [
        ("laser_rifle", "Laser Rifle", "Instant beam, four shots a second", None, "EV charge"),
        ("pdw", "PDW", "Armor-piercing bursts", "mag_advanced", "Advanced Magazine"),
        ("pulse_rifle", "Pulse Rifle", "Long-range three-round bursts", "mag_advanced", "Advanced Magazine"),
        ("minigun", "Minigun", "200 rounds, 20 shots a second", "mag_minigun_drum", "Minigun Ammo Drum"),
        ("tesla_rifle", "Tesla Rifle", "Lightning jumps to three more targets", None, "EV charge"),
        ("sonic_rifle", "Sonic Rifle", "Ring-shaped shockwave volley", None, "EV charge"),
        ("bio_gun", "Bio Gun", "Poison blobs*, charge to grow", "tank_bio", "Bio Tank"),
        ("power_hammer_adv", "Power Hammer (Advanced)", "Harder, faster smash", "tank_air",
         "Compressed Air Tank"),
    ],
    "IV": [
        ("blaster_rifle", "Blaster Rifle", "Scoped energy rifle", None, "IV charge"),
        ("deatomizer_pistol", "De-Atomizer Pistol", "Armor-piercing energy sidearm", None, "IV charge"),
        ("akimbo:deatomizer_pistol", "Akimbo De-Atomizers", "Two of them, twice the charge", None, "IV charge"),
        ("scatter_laser_rifle", "Scatter Laser Rifle", "Five-beam spread", None, "IV charge"),
        ("golden_revolver", "Golden Revolver", "Gold-plated heavy revolver", "ammo_9mm_bullet", "9mm Bullets"),
        ("akimbo:golden_revolver", "Akimbo Golden Revolvers", "A matched pair", "ammo_9mm_bullet", "9mm Bullets"),
    ],
    "LuV": [
        ("nether_blaster", "Nether Blaster", "Slow bolts that set targets alight", "ammo_nether_charge", "Nether Charge"),
        ("alien_blaster", "Alien Blaster", "Full-auto energy pistol", None, "LuV charge"),
        ("grim_reaper", "Grim Reaper", "Four rockets per load", "ammo_rocket", "Rockets"),
    ],
    "ZPM": [
        ("nuclear_death_ray", "Nuclear Death Ray", "Held radiation beam, 48 blocks", None,
         "ZPM charge"),
        ("nuke_launcher", "Nuke Launcher", "One tactical nuke", "ammo_rocket_nuke", "Tactical Nuke Rocket"),
    ],
}

AMMO_BY_TIER = [
    ("Steam", [("ammo_stone_bullet", "Stone Bullets")]),
    ("LV", [("ammo_9mm_bullet", "9mm Bullets"), ("ammo_shotgun_shell", "Shotgun Shells"),
            ("ammo_rifle_bullet", "Rifle Bullets"), ("mag_9mm_pistol", "9mm Pistol Magazine")]),
    ("MV", [("ammo_40mm_grenade", "40mm Grenades"), ("mag_smg", "SMG Magazine"),
            ("mag_assault_rifle", "Assault Rifle Magazine")]),
    ("HV", [("ammo_high_caliber_round", "High Caliber Rounds"), ("ammo_rocket", "Rockets"), ("mag_as50", "AS50 Magazine"),
            ("mag_lmg", "LMG Magazine"), ("tank_fuel", "Fuel Tank"), ("tank_air", "Compressed Air Tank")]),
    ("EV", [("ammo_advanced_bullet", "Advanced Bullets"), ("mag_advanced", "Advanced Magazine"),
            ("mag_minigun_drum", "Minigun Ammo Drum"), ("tank_bio", "Bio Tank")]),
    ("LuV", [("ammo_nether_charge", "Nether Charge")]),
    ("ZPM", [("ammo_rocket_nuke", "Tactical Nuke Rocket")]),
]

MAGAZINES = [
    ("mag_9mm_pistol", "9mm Pistol Magazine", "3", "ammo_9mm_bullet", "18", "9mm Pistol, Akimbo Pistols"),
    ("mag_smg", "SMG Magazine", "2", "ammo_9mm_bullet", "20 to 32", "Thompson, Mac10, Vector"),
    ("mag_assault_rifle", "Assault Rifle Magazine", "3", "ammo_rifle_bullet", "30", "M4, AK47, AUG, SCAR, Infiltrator"),
    ("mag_as50", "AS50 Magazine", "2", "ammo_high_caliber_round", "10", "AS50"),
    ("mag_lmg", "LMG Magazine", "8", "ammo_rifle_bullet", "100", "LMG"),
    ("mag_advanced", "Advanced Magazine", "3", "ammo_advanced_bullet", "12 to 20", "PDW, Pulse Rifle"),
    ("mag_minigun_drum", "Minigun Ammo Drum", "16", "ammo_rifle_bullet", "200", "Minigun"),
]
