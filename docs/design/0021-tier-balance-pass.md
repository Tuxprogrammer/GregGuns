# 0021: Tier balance pass

- **Status:** Accepted (change 1 superseded by [0022](0022-steam-armor-steam-age.md): the plating is retired)
- **Extends:** [0001](0001-tier-map.md), [0003](0003-energy-model.md), [0008](0008-exact-tier-charging.md), [0010](0010-material-remap-and-tier-additions.md)
- **Builds on:** [0016](0016-damage-balance-pass.md) (damage per shot; not redone here)
- **Date:** 2026-10-25
- **Origin:** M9 polish item "a balance pass along the GTNH tier progression" ([0015](0015-m9-self-contained-polish.md))

## Context

[0016](0016-damage-balance-pass.md) fixed damage per shot. Everything else was still Techguns-shaped or written in parallel by the area agents: recipe cost, armor, NPC stats, energy capacity and ammo throughput. This pass audits those five against the GT tier progression and changes only what is a clear outlier. Where the call is the owner's, it is listed under [Judgment calls](#judgment-calls-for-the-owners-playthrough) and not guessed.

Method and data sources:

- **Recipes:** `RecipeExportTest` with `GREGGUNS_EXPORT_RECIPES` (140 machine rows and 8 hand rows; see `tools/asset-gen/infographics/README.md`) plus a read of `GunLineTable`, `AmmoLineTable` and `ComponentTable` for the Assembly Line rows the dump leaves out. Time and EU are `seconds x 20 x TierEU.RECIPE_<tier>` (LV 30, MV 120, HV 480, EV 1,920, IV 7,680, LuV 30,720, ZPM 122,880). Ammo rows carry the owner's 4x duration multiplier (`ProductionTable.AMMO_DURATION_MULTIPLIER`). `tools/recipe-clash` needs a booted pack dump and was not run: no Minecraft server on the laptop. This pass changes no inputs that could newly clash (it removes titanium from one row).
- **Armor, NPCs, energy, ammo:** the Java definitions (`ArmorCatalog`, `NpcProfiles` and the mob classes, the gun `*Def` classes, `AmmoDefinitions`), cross-checked against the spec deviation notes.
- Armor math is the 0004 model: a piece absorbs `points / 25`, the set total caps at 24 (96%), a hit's penetration share skips armor.

## Decision

Three clear outliers are changed. Everything else the audit found is recorded as "in line" or handed to the owner.

### Changes

| # | Item | Old | New | Why |
|---|---|---|---|---|
| 1 | Steam Armor Plating (RC-23, EV Assembler) | 2 titanium plate, 1 bronze plate, 1 steel plate | **1** titanium plate, 1 bronze plate, 1 steel plate | The Steam set (EV) protects 20 (80%), the same as the MV Ranger Veteran (riot) set, and below the EV T3 Combat set (22). Its 24 platings cost 48 titanium plates, against 9 titanium plus 15 epoxy plates for T3 Combat and 14 tungsten steel plates for the IV Power Armor. The set now costs 24 titanium plates. The gate stays titanium, so it is still an EV item (0001's "recipe is never weakened to fit" is about tier gating, which is untouched: the cut keeps `plate.Titanium` as the gate input). Repair and recycling follow automatically (`ArmorRecycling` reads the row). |
| 2 | Alien Blaster (LuV) shots per full charge | 10 shots at 100,000,000 EU (4 s of fire) | **25** shots at 40,000,000 EU (10 s of fire); capacity unchanged, 1,000,000,000 EU | Fire time per charge fell from 12.5 s (IV Blaster Rifle) and 8 s (IV De-Atomizer) to 4 s at LuV, and a LuV charge takes about 25 minutes through one charger amp. A tier up must not shoot for less. It now beats the IV De-Atomizer's shot count (20) and time (8 s). Damage, interval and recipe unchanged. |
| 3 | IV Power Armor and Exo Suit chestplate charge | 3,600 and 7,200 units (720,000 and 1,440,000 EU), 15 and 30 minutes of walking in a full set | **10,000 and 20,000** units (2,000,000 and 4,000,000 EU), 42 and 83 minutes; still 200 EU per unit, 10 EU/t per walking piece | The EV HEV suit lasts 42 minutes (10,000 units at 40 EU). The IV sets lasted 15 and 30. A tier up must not run dry sooner. A full charge still fills in under a minute at one IV amp (2,000,000 EU is 244 ticks, 4,000,000 EU is 488 ticks). The Steam set keeps 3,600 units (refilled with compressed air, not EU). |

A new `TierBalanceTest` pins the ordering these three rely on, as floors and orderings rather than exact numbers: energy rifles fire at least 4 s per charge and the Alien Blaster outlasts the De-Atomizer; gun recipe EU rises tier over tier; the best armor protection never falls going up a tier; the powered IV sets walk at least as long as the HEV.

## 1. Recipe cost against tier

Recipe EU per category (one craft, ammo with the 4x duration included):

| Tier | Guns: count, seconds, EU | Armor and gear: count, seconds, EU | Parts: count, seconds, EU | Ammo rows: count, seconds, EU |
|---|---|---|---|---|
| Steam | none (Hand Cannon is hand-crafted) | 8 hand-crafted rows | | 1 row (stone bullet): 80 s, 12,800 |
| LV | 7, 10 to 20 s, 6,000 to 12,000 | 4, 10 to 16 s, 6,000 to 9,600 | 6, 4 to 6 s, 2,400 to 3,600 | 5, 32 to 96 s, 19,200 to 57,600 |
| MV | 7, 15 to 25 s, 36,000 to 60,000 | 16, 10 to 18 s, 24,000 to 43,200 | 4, 4 to 10 s, 9,600 to 24,000 | 5, 32 to 128 s, 76,800 to 307,200 |
| HV | 11, 30 to 35 s, 288,000 to 336,000 | 7, 10 to 18 s, 96,000 to 172,800 | 3, 8 to 10 s, 76,800 to 96,000 | 6, 32 to 160 s, 307,200 to 1,536,000 |
| EV | 8, 30 to 40 s, 1.15M to 1.54M | 12, 10 to 16 s, 384,000 to 614,400 | 11, 5 to 30 s, 192,000 to 1.15M | 5, 32 to 96 s, 1.23M to 3.69M |
| IV | 4, 30 to 45 s, 4.6M to 6.9M | 9, 10 to 20 s, 1.54M to 3.07M | 2, 15 to 30 s, 2.3M to 4.6M | none (above the press ceiling) |
| LuV | 3, 30 to 60 s, 18.4M to 36.9M; plus the Nether Blaster Assembly Line row (45 s) | none | none | Nether Charge (Assembly Line) |
| ZPM | Assembly Line: Nuke Launcher, Nuclear Death Ray (60 s each, about 147M EU) | none | 4, 20 to 30 s, 49M to 74M | Tactical Nuke Rocket (Assembly Line) |

**Finding:** gun EU rises about 4x per tier with no overlap (the cheapest gun of a tier costs more than the dearest of the tier below), which is what GT's own EU/t ladder does. Armor and parts follow the same ladder. Circuit tier is right wherever a circuit appears (Good at MV, Advanced at HV, Data at EV, Elite at IV, Master at LuV, Ultimate at ZPM). Material gates match 0001 and 0010 (Steel, Aluminium, Stainless Steel, Titanium, Tungsten Steel, HSS-G, Naquadah Alloy). No item is craftable a tier early. Machines (Ammo Press tiers, Repair Bench, Turret, Rampart hatches) are built around GT's own `Hull_<tier>`, tier circuit, motor and piston, so none is buildable below its tier ([08 recipes](../gregguns-spec/08-machines-production/recipes.md)).

Outliers and decisions:

| Item | Observation | Decision |
|---|---|---|
| Steam armor (EV) | 48 titanium plates, protection equal to an MV set | **Changed** (1 above) |
| HV guns (AUG, SCAR, Vector, LMG, AS50, launchers) | No circuit at all; the gate is stainless steel only. The first circuit in a gun is at EV. | Judgment call |
| Grim Reaper (LuV) | No circuit, 45 s, 4 HSS-G plates and a LuV piston; hits for 90 per shot and is the Nuke Launcher's research item | Judgment call |
| Golden Gun (LuV, one-shot kill) | A 9mm Pistol, 8 gold plates, 1 HSS-G plate and 2 Master circuits in 30 s, the cheapest LuV gun; its round is a 9mm round, 2 gold foil and a gold plate | Judgment call |
| Exo Suit (IV) | Built from the EV T3 Combat set plus 4 mechanical parts, 4 Elite circuits, 4 IV motors and 6 tungsten steel plates; about a third of the Power Armor's material | In line: it protects 22, one point under the Power Armor's 24, and has no passive health |
| Ammo Press recipe ceiling | The press stops at EV; IV and above ammo is an Assembly Line row | In line |

## 2. Armor against tier and the damage curve

Full-set values (projectile/physical total; energy, explosion, fire, ice and lightning are 75% of it unless the set lists them):

| Set | Tier | Protection | Absorbs | Effective HP multiple | Durability | Notes |
|---|---|---|---|---|---|---|
| T1 Scout, T1 Miner | Steam | 13 | 52% | 2.1x | 60 | speed, mining bonuses |
| T1 Combat | LV | 15 | 60% | 2.5x | 60 | knockback resistance |
| T2 Combat | MV | 19 | 76% | 4.2x | 72 | |
| T2 Commando | MV | 18 | 72% | 3.6x | 72 | energy, explosion, fire 15 |
| T2 Riot (Ranger Veteran) | MV | 20 | 80% | 5.0x | 96 | 10% penetration resistance |
| T3 Combat | EV | 22 | 88% | 8.3x | 240 | 15% penetration resistance |
| Steam armor | EV | 20 | 80% | 5.0x | 200 | powered by air, health bonus |
| T3 HEV | EV | 21 | 84% | 6.3x | 300 | powered, partial hazard cover |
| T3 Exo Suit | IV | 22 | 88% | 8.3x | 300 | powered, +20% speed |
| T3 Power Armor | IV | 24 | 96% | 25x | 360 | powered, 25% penetration resistance |

Hits to kill a 20 HP player with NPC guns (NPC damage is the pre-0016 value, see 0016 "NPCs do not get stronger"; physical or projectile, penetration 0):

| Armor | Pistol 8 | AK-47 9 | Thompson 5 | Laser Rifle 12 (energy) | Grim Reaper 50 |
|---|---|---|---|---|---|
| none | 3 | 3 | 4 | 2 | 1 |
| T1 Combat | 7 | 6 | 10 | 4 | 1 |
| T2 Combat | 11 | 10 | 17 | 4 | 2 |
| T3 Combat | 21 | 19 | 34 | 6 | 4 |
| T3 Power Armor | 63 | 56 | 100 | 9 | 10 |

**Findings.** Protection rises with tier through EV and IV, with one plateau (EV and IV Exo share 22) and one outlier (Steam at EV, handled). The curve is steep at the top because absorption is `points / 25`: going from 22 to 24 triples effective HP (8.3x to 25x). That is Techguns' design and the 0004 clamp; it is not changed here. Armor stops at IV: 0001 gives no LuV or ZPM armor, while LuV and ZPM guns exist. Durability goes 60, 72, 240, 300, 360 (the MV step is small, the EV step large); the repair bench and Armory Gantry cost scale with tier, so it is left for the playthrough.

## 3. NPC health, damage and loot against tier

NPC damage is the old Techguns number on purpose (0016). Health is Techguns' too, and does not scale with spawn danger; danger picks the gear (`EquipmentProfile` `minDifficulty`, `SpawnDanger`). Loot tier is the `NpcProfile` tier.

| NPC | Loot tier | Health | Gear when it spawns | Hits to kill with the same-tier player gun (armor included) |
|---|---|---|---|---|
| Zombie Farmer, Miner | Steam | 18, 20 | hoe or pickaxe, Hand Cannon; T1 Miner camo | 3 Hand Cannon hits |
| Bandit | MV | 20 | pistol to AK-47; T1 Scout (13) | 5 M4 hits |
| Zombie and Skeleton Soldier | MV | 25 | revolver, Thompson; T1 Combat (15) | 7 M4 hits |
| Army Soldier | MV | 30 | M4, shotgun, bolt action; T2 Combat (19) | 14 M4 hits |
| Commando | MV | 30 | M4; T2 Commando (18) | 12 M4 hits |
| Zombie Pigman Soldier | HV | 25 | up to LMG and Rocket Launcher; T2 Combat | 10 AUG hits |
| Psycho Steve | HV | 75 | Chainsaw; T1 Miner | boss, fixed drops |
| Super Mutant Basic, Heavy, Elite | EV | 30, 40, 50 | rocket launcher, AK-47, shotgun, Laser Rifle; vanilla armor 7, 12, 10 | 3 Pulse Rifle hits (basic) |
| Stormtrooper | IV | 35 | Blaster Rifle, Laser Rifle; HEV (21) | 10 Blaster Rifle hits |
| Dictator Dave | IV | 100 | Golden Revolver; mixed T1 and T2 | boss |
| Outcast | LuV | 30 | Grim Reaper, Laser, Minigun; Power Armor (24) | 18 Alien Blaster hits (3 with its 0.2 penetration) |
| Cyber Demon | LuV | 30 | per-mob sheet | not profiled |

**Findings.** Same-tier gun against same-tier NPC lands between 3 and 14 hits for ordinary soldiers, which is a sensible fight. Every tier's NPC has an answer in the gun a player has by then, and the NPC's own damage is cut to a few points a hit by the armor a player has by then, so late fights are about attrition, not burst. No health number is clearly wrong, so none changes. The two soft spots (Outcast tankiness, high-tier loot) are judgment calls below.

## 4. Energy

Energy guns (capacity is the tier's GT battery; EU per shot is `capacity / shots`). Charge time is one amp at the tier voltage (EV 2,048, IV 8,192, LuV 32,768, ZPM 131,072 EU/t); a battery buffer slot charges faster.

| Gun | Tier | Capacity EU | Shots | EU per shot | Fire time per charge | One-amp charge |
|---|---|---|---|---|---|---|
| Laser Rifle | EV | 6,400,000 | 45 | 142,222 | 11.3 s | 156 s |
| Tesla Rifle | EV | 6,400,000 | 25 | 256,000 | 10.0 s | 156 s |
| Sonic Rifle | EV | 6,400,000 | 8 | 800,000 | 4.8 s | 156 s |
| De-Atomizer Pistol | IV | 100,000,000 | 20 | 5,000,000 | 8.0 s | 10.2 min |
| Blaster Rifle | IV | 100,000,000 | 50 | 2,000,000 | 12.5 s | 10.2 min |
| Scatter Laser Rifle | IV | 100,000,000 | 40 | 2,500,000 | 14.0 s | 10.2 min |
| Alien Blaster | LuV | 1,000,000,000 | **25** (was 10) | **40,000,000** (was 100,000,000) | **10.0 s** (was 4.0 s) | 25.4 min |
| Nuclear Death Ray | ZPM | 10,000,000,000 | 500 ticks of beam | 20,000,000 per tick | 25 s | 63.6 min |

Powered armor (walking costs 1 unit per worn piece per second; a full set is 4 units per second):

| Set | Tier | Units | EU per unit | Capacity EU | Walk time, full set | EU/t, full set |
|---|---|---|---|---|---|---|
| HEV | EV | 10,000 | 40 | 400,000 | 41.7 min | 8 |
| Steam armor | EV | 3,600 | air | n/a | 15 min | n/a |
| Power Armor | IV | **10,000** (was 3,600) | 200 | **2,000,000** (was 720,000) | **41.7 min** (was 15 min) | 40 |
| Exo Suit | IV | **20,000** (was 7,200) | 200 | **4,000,000** (was 1,440,000) | **83.3 min** (was 30 min) | 40 |

**Findings.** Guns hold what the tier's battery holds, so total energy rises 15x or more per tier while charge time rises about 4x per tier. Fire time per charge is flat at 8 to 14 s from EV to IV, which is the right feel; the Alien Blaster was the one tier-up that regressed, and the Sonic Rifle (a close-range volley gun) is left. The armor capacities were far below the tier's battery (HEV 6% of an EV battery, Power Armor 0.7% of an IV battery) but fill from a single charger amp in seconds, and the walking time is the real limit, so the fix is on walk time. 0003's "baseline is the battery" stays a guideline for items that spend EU in shots.

## 5. Ammo, magazines and Ammo Press throughput

One loose-round row per ammo type (EU includes the 4x time multiplier):

| Ammo | Tier | Run | Per round: seconds, EU | Shots per round |
|---|---|---|---|---|
| Stone bullet | Steam | 24 in 80 s | 3.3 s, 533 | 1 |
| 9mm bullet | LV | 16 in 80 s | 5 s, 3,000 | 6 (pistol), 10 (Thompson), 16 (Mac-10); Revolver 1 |
| Rifle bullet | LV | 16 in 96 s | 6 s, 3,600 | 10 (M4, AK, AUG), 12.5 (LMG, drum); bolt action 1 |
| Shotgun shell | LV | 8 in 96 s | 12 s, 7,200 | 1 |
| 40mm grenade | MV | 8 in 128 s | 16 s, 38,400 | 1 |
| High caliber round | HV | 8 in 128 s | 16 s, 153,600 | 5 (AS50) |
| Rocket | HV | 4 in 160 s | 40 s, 384,000 | 1 |
| Advanced bullet | EV | 16 in 64 s | 4 s, 153,600 | 6.7 (PDW), 4 (Pulse Rifle) |

Magazines add a one-time empty-magazine row (reusable and recycled) and a fill row of 8 to 12 s per magazine.

Press seconds needed per second of firing (one press at the recipe tier, loose rounds only; "per DPS" divides by the 0016 DPS):

| Tier | Gun | Shots/s | Shots per round | Press s per round | Press s per fire s | Per DPS |
|---|---|---|---|---|---|---|
| LV | Pistol | 5.0 | 6 | 5 | 4.2 | 0.10 |
| LV | Revolver | 3.3 | 1 | 5 | 16.7 | 0.51 |
| LV | Sawed-Off | 5.0 | 1 | 12 | 60.0 | 0.43 |
| LV | Bolt Action | 0.8 | 1 | 6 | 4.8 | 0.30 |
| MV | Thompson | 6.7 | 10 | 5 | 3.3 | 0.08 |
| MV | Mac-10 | 10.0 | 16 | 5 | 3.1 | 0.05 |
| MV | M4 | 6.7 | 10 | 6 | 4.0 | 0.07 |
| MV | Combat Shotgun | 1.4 | 1 | 12 | 17.1 | 0.34 |
| HV | AUG | 6.7 | 10 | 6 | 4.0 | 0.05 |
| HV | LMG | 10.0 | 12.5 | 6 | 4.8 | 0.05 |
| HV | AS50 | 2.0 | 5 | 16 | 6.4 | 0.08 |
| HV | Grenade Launcher | 4.0 | 1 | 16 | 64.0 | 0.44 |
| HV | Rocket Launcher | 2.0 | 1 | 40 | 80.0 | 0.67 |
| EV | PDW | 10.0 | 6.7 | 4 | 6.0 | 0.07 |
| EV | Pulse Rifle | 2.9 | 4 | 4 | 2.9 | 0.07 |
| EV | Minigun | 20.0 | 12.5 | 6 | 9.6 | 0.07 |
| LuV | Grim Reaper | 3.3 | 1 | 40 | 133.3 | 0.44 |

**Findings.** Every magazine-fed gun sits between 3 and 10 press seconds per second of fire, flat across tiers, so one press (or a few) keeps a player firing and the Ammunition Works parallels cover a base. Guns that burn one loose round per shot (Revolver, Sawed-Off, shotguns, launchers, Grim Reaper) cost 4 to 10 times more press time per damage. That is the Techguns design (those guns have no magazine), and the ammo is also found as NPC loot. It is not changed: the owner set the 4x ammo time deliberately and these guns are the intended heavy ammo burners. It is the first thing to watch (below). Ammo EU per round rises 3,000 (LV) to 384,000 (HV rocket) to 153,600 (EV advanced bullet), in step with damage and tier.

## Consequences

- Three numbers change; every damage number stays as 0016 left it.
- Tests that pinned the old numbers pin the new ones (`ArmorTierTableTest`, `ArmorCompositionTest`, `PowerArmorTest`, `AlienBlasterDefinitionTest`). `TierBalanceTest` is new.
- The `PowerArmorScenarios` full-charge checks use the new unit counts.
- Spec deviation notes updated: [06 recipes RC-23](../gregguns-spec/06-items/recipes.md), [05 recipes](../gregguns-spec/05-armor/recipes.md), [05 implementation notes PWR-DEV-01](../gregguns-spec/05-armor/implementation-notes.md), [02 guns index](../gregguns-spec/02-guns/index.md).
- Infographic data holds none of these three numbers except the Steam Armor Plating row, which reads the generated `recipes.json`; rebuild the armory sheet with the export step when the sheets are next regenerated.

## Alternatives considered

- **Raise Steam armor protection instead of cutting its titanium:** rejected. It would put an EV set above the T3 Combat set or force a new protection curve, and it moves damage-side balance that 0016 and 0004 own.
- **Match armor energy to the tier's battery (0003's baseline):** rejected for armor. Walking time, not stored EU, limits armor, and an IV battery of 100,000,000 EU would last about a day and a half of walking.
- **Scale NPC health with spawn danger or tier:** rejected for now. 0016 deliberately keeps NPCs where they were, and the hits-to-kill table shows no tier where the same-tier gun fails.
- **Cut shell and launcher press times:** rejected for now (judgment call 4). It undoes part of the owner's 4x ammo nerf.

## Judgment calls for the owner's playthrough

1. **HV and MV guns carry no circuits.** The first circuit in a gun recipe is EV (Data). GT HV machines need Advanced circuits. Add Good (MV) and Advanced (HV) circuits to the stronger rows if HV feels too easy to reach.
2. **Golden Gun and Grim Reaper are cheap for LuV.** The Golden Gun (one-shot kill) is the cheapest LuV gun; the Grim Reaper has no circuit. Consider an Assembly Line row or more HSS-G plates and a third Master circuit.
3. **Loose-round guns burn 4 to 10 times more press time per damage** (Revolver, Sawed-Off, shotguns, launchers, Grim Reaper). Options if it hurts: shell base time 24 s to 12 s, or 16 shells per run; Revolver magazines.
4. **Outcast tankiness.** Power Armor (96%) makes the 30 HP Outcast take 18 Alien Blaster hits (3 with penetration). It is meant to be a hard NPC; lower its armor chance if it is a wall.
5. **Natural spawns top out at MV gear.** Danger 5 spawns the same mobs with the same health; only structures and eggs bring Stormtroopers, Outcasts and Cyber Demons.
6. **Stormtrooper loot is LV-grade** (basic circuit, basic battery) for an IV mob in HEV. Raise to Advanced if desired.
7. **Armor stops at IV.** There is no LuV or ZPM armor; the Power Armor's 96% is effectively the cap (0004 clamps at 24).
8. **Durability steps** 60, 72, 240, 300, 360: T1 to T2 is small, T2 to T3 large.
9. **Charge times at one amp** grow to 25 min (Alien Blaster) and 64 min (Nuclear Death Ray). Battery buffers charge several items at once; check that a base feels workable.
10. **Sonic Rifle** holds 8 shots (4.8 s of fire). It is a close-range special; raise if it runs dry too fast.
11. **Steam armor** at 24 titanium plates and 80% protection is still the weakest EV set; its worth is the health bonus and the air-powered bonuses.

## What to watch in the playthrough

- Time to first MV and HV gun: does the lack of circuits make HV feel skipped?
- Ammo supply on a shotgun, Revolver or launcher build: how many presses does one fight need?
- Alien Blaster: does 25 shots (10 s) feel right against LuV mobs, and is the 25 minute one-amp charge workable?
- Power Armor and Exo walking time: 42 and 83 minutes per charge, and the 10 EU/t per piece drain on a base's grid.
- Steam armor: whether 24 titanium plates is worth 80% protection.
- Outcast and Stormtrooper fights with same-tier guns: hits to kill from the table above.
- Armor and damage at the top: whether anything past IV needs a counter to LuV and ZPM guns.
