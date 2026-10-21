# 0016: Damage balance pass

- **Status:** Accepted
- **Amends:** [0004](0004-damage-model.md) ("Initial numbers")
- **Date:** 2026-10-21
- **Origin:** owner request, "full damage review of all weapons"; amended the same day: "detach our damage values from Techguns"

## Problem

The Techguns per-shot damage numbers were carried over unchanged. In Techguns the high-end guns paid for their power in ammo; here they pay in GT materials and EU. The result in play was that the expensive tiers barely moved on damage per shot: a ZPM Nuclear Death Ray (6 per tick) was within a factor of two of an LV Revolver's per-shot figure, and an IV Deatomizer Pistol (16) did only twice what a Pistol does. The ammo-free convenience of the energy tiers is correct and stays, but it is a perk on top of a damage curve, not a substitute for one.

## Decision

1. **GregGuns owns its damage numbers.** Techguns parity is no longer a goal. The Techguns spec values are history only (the spec sheets stay as the record of what Techguns did; the deviation notes point here).
2. **The curve.** Per-shot damage, relative to the LV/MV baseline:

   | Tier | Per shot |
   |---|---|
   | LV, MV | about 1x (small nudges only) |
   | HV | about 1.35x |
   | EV | about 1.7 to 2x |
   | IV | about 2 to 2.6x |
   | LuV | about 2.8x |
   | ZPM | about 3.6 to 4x (the Death Ray, per tick), and total annihilation (the Nuke Launcher) |

3. **Rates, spreads, energy costs and recipes are unchanged.** Only `.damage(...)` and the drop-off minimum (the third `damageDrop` number, scaled by the same ratio and rounded to 0.5) change.
4. **Ammo-free is a perk on top.** Energy and fuel guns keep their ammo-free convenience; the curve is not discounted for it.
5. **Tools are unchanged:** Chainsaw, Power Hammer, Power Hammer Advanced and Tool Probe.

## Pack anchors

Calibrated against what the pack already gives a player at similar progression, read from source at the tag the pack ships (half hearts, per hit).

| Anchor | Value | Source |
|---|---|---|
| EnderIO Dark Steel sword | tool material damage 2, so a 6 hit | `// Source:` https://github.com/GTNewHorizons/EnderIO/blob/2.10.48/src/main/java/crazypants/enderio/item/darksteel/ItemDarkSteelSword.java#L51-L52 |
| Draconic Evolution Wyvern sword | tool material damage 20, so a 24 hit before upgrades; each attack upgrade point adds 5, up to 8 points (64 at the cap) | `// Source:` https://github.com/GTNewHorizons/Draconic-Evolution/blob/1.5.33-GTNH/src/main/java/com/brandon3055/draconicevolution/common/ModItems.java#L66 and https://github.com/GTNewHorizons/Draconic-Evolution/blob/1.5.33-GTNH/src/main/java/com/brandon3055/draconicevolution/common/items/tools/baseclasses/ToolHandler.java#L336-L348 |
| Draconic Evolution Draconic sword | tool material damage 40, so a 44 hit before upgrades; up to 16 upgrade points (124 at the cap) | same files; upgrade caps in https://github.com/GTNewHorizons/Draconic-Evolution/blob/1.5.33-GTNH/src/main/java/com/brandon3055/draconicevolution/common/handler/BalanceConfigHandler.java#L32-L43 |

Both Draconic swords also add a health-based bonus hit. A sword lands about 1.7 swings a second; a gun lands many more shots, so a gun's per-shot value sits below a sword's per-hit value at the same tier while its sustained DPS sits above.

Where each GregGuns tier lands:

- **LV and MV (6 to 20).** At and above Dark Steel (6). The Bolt Action's 20 is a slow, scoped, 6-round weapon.
- **HV (8 to 14; AS50 42).** Between Dark Steel and a Wyvern base hit; the AS50 reaches well above the base hit as a slow heavy rifle.
- **EV (9 to 20; Sonic Rifle 44).** Up to about a Wyvern base hit (24), below its upgraded cap.
- **IV (12 to 32).** At and just above a Wyvern base hit; below an upgraded Wyvern (64).
- **LuV (40 to 44; Grim Reaper 90).** At a Draconic base hit (44); the Grim Reaper's 90 sits between a base and a fully upgraded Draconic sword (124) as a single slow shot.
- **ZPM.** The Death Ray at 30 per tick is 600 per second and the Nuke Launcher annihilates; both are the apex weapons and the most expensive in EU.

No tier is clearly out of line with its anchors, so **no numbers were nudged** from the orchestrator's table. The one deliberate exception below a tier floor is the Minigun (see the notes).

## Numbers

Damage is per projectile (per pellet or beam where the gun fires several). DPS is damage x pellets x 20 / fire interval in ticks, for one barrel; it ignores reloads and rechambering.

| Tier | Gun | Old dmg | New dmg | Interval (ticks) | Pellets | Old DPS | New DPS |
|---|---|---|---|---|---|---|---|
| STEAM | Hand Cannon | 8 | 8 | 12 | 1 | 13 | 13 |
| LV | Sawed-Off | 4 | 4 | 4 | 7 | 140 | 140 |
| LV | Akimbo Pistols | 8 | 8 | 3 | 1 | 53 | 53 |
| LV | Pistol | 8 | 8 | 4 | 1 | 40 | 40 |
| LV | Akimbo Revolvers | 8 | 10 | 4 | 1 | 40 | 50 |
| LV | Revolver | 8 | 10 | 6 | 1 | 27 | 33 |
| LV | Bolt Action | 16 | 20 | 25 | 1 | 13 | 16 |
| MV | Combat Shotgun | 4 | 5 | 14 | 7 | 40 | 50 |
| MV | Akimbo Mac-10s | 5 | 6 | 2 | 1 | 50 | 60 |
| MV | Mac-10 | 5 | 6 | 2 | 1 | 50 | 60 |
| MV | Thompson | 5 | 6 | 3 | 1 | 33 | 40 |
| MV | M4 | 8 | 9 | 3 | 1 | 53 | 60 |
| MV | AK-47 | 9 | 10 | 3 | 1 | 60 | 67 |
| HV | Flamethrower | 5 | 6 | 2 | 1 | 50 | 60 |
| HV | Vector | 6 | 8 | 2 | 1 | 60 | 80 |
| HV | LMG | 8 | 10 | 2 | 1 | 80 | 100 |
| HV | AUG | 8 | 11 | 3 | 1 | 53 | 73 |
| HV | Infiltrator | 8 | 11 | 3 | 1 | 53 | 73 |
| HV | SCAR | 12 | 14 | 4 | 1 | 60 | 70 |
| HV | Grenade Launcher | 30 | 36 | 5 | 1 | 120 | 144 |
| HV | AS50 | 32 | 42 | 10 | 1 | 64 | 84 |
| HV | Rocket Launcher | 50 | 60 | 10 | 1 | 100 | 120 |
| EV | Minigun | 5 | 7 | 1 | 1 | 100 | 140 |
| EV | PDW | 6 | 9 | 2 | 1 | 60 | 90 |
| EV | Pulse Rifle | 10 | 15 | 7 | 1 | 29 | 43 |
| EV | Bio Gun | 10 | 16 | 6 | 1 | 33 | 53 |
| EV | Laser Rifle | 12 | 18 | 5 | 1 | 48 | 72 |
| EV | Tesla Rifle | 12 | 20 | 8 | 1 | 30 | 50 |
| EV | Sonic Rifle | 25 | 44 | 12 | 1 | 42 | 73 |
| IV | Scatter Laser Rifle | 6 | 12 | 7 | 5 | 86 | 171 |
| IV | Blaster Rifle | 10 | 24 | 5 | 1 | 40 | 96 |
| IV | Akimbo Golden Revolvers | 16 | 28 | 6 | 1 | 53 | 93 |
| IV | Golden Revolver | 14 | 28 | 8 | 1 | 35 | 70 |
| IV | Akimbo Deatomizer Pistols | 16 | 32 | 5 | 1 | 64 | 128 |
| IV | Deatomizer Pistol | 16 | 32 | 8 | 1 | 40 | 80 |
| LUV | Nether Blaster | 14 | 40 | 8 | 1 | 35 | 100 |
| LUV | Alien Blaster | 16 | 44 | 8 | 1 | 40 | 110 |
| LUV | Grim Reaper | 50 | 90 | 6 | 1 | 167 | 300 |
| ZPM | Nuclear Death Ray | 6 | 30 | 1 | 1 | 120 | 600 |
| ZPM | Nuke Launcher | 250 | 400 | 10 | 1 | 500 | 800 |

Notes on guns whose damage means something different:

- **Sonic Rifle.** One pull fires a ring volley of 16 projectiles (1 + 5 + 10), but an entity is hit once per volley (`VolleyHits`), so a single target takes one hit per pull, not 16. Its effective damage per pull is therefore the `damage` value. The Combat Shotgun now deals 7 pellets x 5 = 35 on a full hit. Rule: the Sonic Rifle's per-pull damage is at least 1.25x the shotgun's full hit (43.75). At 25 it was below that, so it is raised to **44** (drop-off minimum 5 to 9, same distances 5 to 15). It stays a short-range weapon.
- **Akimbo pairs** fire two guns; the table lists the per-shot damage and the interval of the pair's item. The Akimbo Golden Revolvers now equal the single Golden Revolver (28), where Techguns had a quirk of 16 against 14.
- **Scatter Laser Rifle:** 5 beams per pull, 12 each.
- **Nuclear Death Ray:** damage is per tick of beam (fire interval 1).
- **Minigun (7).** The tier floor test skips it: at one shot per tick, 7 per shot is 140 damage a second, already the top of the EV tier. Raising it to the EV floor of 9 would make it 180.
- **Flamethrower:** a stream, so it is also skipped by the floor test.
- **Launchers (Grenade, Rocket):** their number is the direct hit and the blast's peak.

## Monotonic curve test

`DamageCurveTest` checks, over all gun definitions, that from HV up the maximum per-shot damage of each tier is at least 1.2x the previous tier's, and that no gun is below its tier floor (HV 8, EV 9, IV 12 per beam or pellet, LuV 40, ZPM 18 per tick). The curve comparison leaves out tools, the explosive launchers, the continuous beam (held to its floor instead) and the single-shot or volley specials (AS50, Grim Reaper, Sonic Rifle), whose one pull is a different class from the tier's rifles. Without those exclusions the curve cannot hold: the AS50's 42 alone is above every EV rifle.

## NPCs do not get stronger

NPC-dealt damage stays at the old values. `GunStats.Builder.npcDamage(damage, min)` gives a gun a second stats object (`GunStats.forNpc()`) with the pre-pass damage and drop-off minimum and every other field identical. `NpcEntity` (all mobs that hold a gun) and `NpcTurret` (hostile turret mobs) fire with `forNpc()`; a gun without `npcDamage` returns itself. Players use `damage()`. The attack helicopter has its own gun definitions, which are untouched.

The player-owned **Rampart turret** (`TurretEntity`) uses the **player (new) values**: the owner pays for the turret and loads it with a player's gun, and the turret already has its own `damageTurretToPlayer` and `damagefactors` settings. A Nuke Launcher fired by an NPC follows the same annihilation rule below, with the old 250 as the ramp peak.

The Nuclear Death Ray is not used by an NPC, so its armor-ignoring change affects only the player's gun.

## Nuke Launcher: total annihilation

Damage 400 (NPC peak 250). The blast keeps its radius of 10 and its block destruction.

- **Kill zone.** Inside 60% of the blast radius (`NukeAnnihilation.KILL_ZONE_SHARE`, 6 blocks), a living non-boss entity takes health plus absorption plus 1 damage from an `UNRESISTABLE` source (bypasses armor, absolute) that ignores hurt-resistance frames, so it dies outright.
- **Bosses** (`IBossDisplayData`, plus the vanilla Wither and Ender Dragon) take 50% of their maximum health instead.
- **Players** in the kill zone die too. The `damagefactors` gate still applies: a zero factor (PvP off, for instance) blocks the hit, and `explosions.selfDamage` still decides whether the shooter is a victim.
- **Beyond the kill zone,** damage follows the existing ramp (`BlastFalloff`): the full 400 up to 5 blocks, falling linearly to zero at 15.
- A rocket that meets an entity directly explodes on it: the target takes the kill-zone rule, not a separate direct hit.
- The pure decision is `NukeAnnihilation` (unit tested); the scenarios `gun.nuke_launcher.fire` and `gun.nuke_launcher.killzone` cover it in game.

## Golden Gun: one shot, one kill

The Golden Gun (LuV, a GregGuns original) applies the same decision as the Nuke Launcher's kill zone to a bullet hit: a living non-boss takes health plus absorption plus 1 from an `UNRESISTABLE` source that ignores hurt-resistance frames, a boss takes 50% of its maximum health, and the `damagefactors` gate still applies. Both callers use one implementation, `NukeAnnihilation.lethal` through `DamageSources.annihilateLiving`. Its per-shot damage (1000) is nominal and the gun is exempt from the curve and floor tests (`DamageCurveTest.KILL_RULE`). The scenarios `gun.golden_gun.kill`, `gun.golden_gun.boss` and `gun.golden_gun.reload` cover it in game.

## Nuclear Death Ray

Damage 30 per tick (drop-off minimum 5) and penetration 1.0, so the radiation beam ignores armor. Its identity stays `RADIATION`.

## Consequences

- The Techguns spec sheets in `docs/techguns-spec/` are unchanged (they record Techguns). The GregGuns spec in `docs/gregguns-spec/02-guns/` carries the new numbers and points here.
- Unit tests that pinned the old numbers pin the new ones; the NPC scenarios are unchanged.
- Infographic data (`tools/asset-gen/infographics/data.py`) holds no damage numbers, so nothing there changes.
