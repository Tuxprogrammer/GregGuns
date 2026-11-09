# 0027 · Combat difficulty knobs: knockback, NPC damage by tier, NPC accuracy by distance, EV armor

- Status: Accepted (by default)
- Gate: none
- Date: 2026-11-08
- Decisions: D357 (knockback), D358 (NPC damage), D359 (NPC accuracy), D360 (armor)

## Context

The owner's beta playthrough (0.4.0-beta) asked for two things:

1. "Knockback should move to a config option and be turned off by default."
2. "Rebalance damage & armor tiers around GTNH; less bullet damage from high-tier mobs + Lower accuracy at distances greater than 5 blocks; all with configurable options so that players can make it as 'hard' as they like."

Before this record, every gun and projectile hit pushed its target the vanilla way, scaled by the gun's knockback multiplier ([GUN-38](../techguns-spec/02-guns/damage-system.md)). An NPC shot dealt the gun's NPC damage times 0.5 times the difficulty factor (Easy 0.5, Normal 0.6, Hard 0.75), times `damagefactors.damageFactorNPC`, with the gun's full armor penetration. NPC aim was the gun's spread times the difficulty accuracy factor, the same at 3 blocks as at 30. Armor absorbs `points / 25`, capped at 24 points (96%) ([0004](0004-damage-model.md), [0021](0021-tier-balance-pass.md)).

### GTNH armor anchors

What a player wears at each tier, and how much of a GregGuns hit it absorbs (vanilla-bar armor at 4% per point; pack ISpecialArmor at its own ratio):

| Tier | What players wear | Absorbs |
|---|---|---|
| Steam / LV | Iron, bronze or steel armor | 60% |
| LV | Diamond | 80% |
| MV | EnderIO Dark Steel; our T2 Combat (76%) | 60–80% |
| HV | IC2 NanoSuit | 90% |
| HV / EV | Galacticraft titanium; our T3 Combat (88% before this record) | ~96% |
| IV | IC2 QuantumSuit; our Power Armor (96%) and Exo Suit | 100% (Quantum) |
| ZPM | GraviSuite | 100% |

The problem the owner hit: a high-tier NPC gun is tuned to kill a player in high-tier armor, and at the tier the player meets that NPC (often a tier or two early, in a structure) the shots shred them. Pack armor at HV and up already absorbs 90% or more, so the part that hurts is the gun's armor penetration and its raw damage.

## Decision

### 1. Knockback: one switch, off by default (D357)

- `combat.knockback` (default `false`) decides whether a GregGuns gun or projectile hit pushes its target, both ways: player shots at mobs and NPC shots at players. `combat.knockbackMultiplier` (default `1.0`, 0 to 10) scales the push while it is on, times the gun's own knockback (GUN-38: Rocket Launcher 3, Sonic Rifle 2, Chainsaw 0).
- Every hit that goes through `DamageSources.hit` takes it: bullets, pellets, beams, the Tesla arc, shrapnel, a rocket's direct hit and the Power Hammer's shockwave. The hit records the target's motion, `isAirBorne` and `velocityChanged` before the vanilla `attackEntityFrom` and, when the effective multiplier is 0, puts all five back, so no velocity packet jolts a player. Otherwise the push is scaled (`Knockback.scale`).
- **Explosion blasts are excluded.** The rocket's and grenade's blast, the ICBM blasts and the Nuke Launcher push as blasts whatever the switch says: being thrown by a blast is what an explosion is, and vanilla's own explosions push too. Melee (`DamageSources.strike`) is unchanged.

### 2. NPC damage: per tier of the gun, elites, penetration (D358)

The damage of an NPC shot is now:

```
gun NPC damage × 0.5 × difficulty (Easy 0.5, Normal 0.6, Hard 0.75)
               × combat.npcDamage<tier of the gun>
               × combat.npcEliteDamage (elite or boss only)
               × damagefactors.damageFactorNPC
```

and the gun's penetration against a player is `penetration × damagefactors.armorPenetrationMultiplierNPCtoPlayer` (default now **0.5**, was 1.0) less the armor's penetration resistance. A turret skips the difficulty factor and uses `damagefactors.damageTurretToPlayer` as before; the penetration multiplier covers turret shots at players too.

| Key | Default | Why |
|---|---|---|
| `npcDamageSteam`, `npcDamageLV`, `npcDamageMV` | 1.0 | Early NPCs already die to and hurt players fairly; the owner's complaint is about high-tier mobs. |
| `npcDamageHV` | 0.85 | HV players are in Nano (90%) at best. |
| `npcDamageEV` | 0.7 | |
| `npcDamageIV` | 0.55 | |
| `npcDamageLuV` | 0.45 | No GregGuns armor above IV (0021); players meet LuV NPCs in IV gear. |
| `npcDamageZPM` | 0.4 | |
| `npcEliteDamage` | 0.75 | An Infernal Mobs elite, ultra or infernal NPC or a boss (anything `IBossDisplayData`: the Attack Helicopter) already has more health and modifiers; its gun need not also hit harder than a plain NPC's. |

- **The tier is the gun's tier** (`GunDefinition.tier()`), not the NPC's spawn tier: the gun is what decides damage, the same NPC kind can carry guns of several tiers, and NPC drops and gear stay as they are. The NPC turret's AK47 is MV (1.0). The Attack Helicopter's machine gun and rockets are built in, not gun items, and count as HV; as a boss it gets the elite multiplier too, 0.85 × 0.75 ≈ 0.64.
- The player's own Rampart turret, and players' guns, are unchanged.

**Hits to kill a 20 HP player, Normal** (before → after; one row per gun, NPC damage and penetration from the gun's stats; armor penetration resistance included):

| Gun (tier) | No armor | Dark Steel (MV) 72% | T2 Combat 76% | NanoSuit (HV) 90% | T3 Combat (EV) 88% → 92% | Quantum (IV) 100% |
|---|---|---|---|---|---|---|
| M4 (MV) | 9 → 9 | 22 → 25 | 24 → 29 | 36 → 50 | 70 → 105 | 56 → 112 |
| AK47 (MV) | 8 → 8 | 20 → 23 | 21 → 25 | 32 → 45 | 62 → 93 | 50 → 99 |
| AS50 (HV) | 3 → 3 | 5 → 7 | 5 → 8 | 7 → 12 | 11 → 31 | 9 → 20 |
| Sonic Rifle (EV) | 3 → 4 | 6 → 11 | 7 → 12 | 9 → 18 | 13 → 48 | 11 → 31 |
| Blaster Rifle (IV) | 7 → 13 | 16 → 35 | 18 → 39 | 24 → 64 | 41 → 152 | 34 → 122 |
| Deatomizer (IV) | 5 → 8 | 10 → 22 | 11 → 24 | 15 → 40 | 26 → 95 | 21 → 76 |
| Grim Reaper (LuV) | 2 → 3 | 5 → 11 | 6 → 13 | 14 → 30 | 12 → 38 | ∞ → ∞ |
| Death Ray (ZPM) | 12 → 28 | 12 → 44 | 12 → 45 | 12 → 51 | 13 → 70 | 12 → 56 |

**Hard** (damage × 1.25):

| Gun (tier) | No armor | Dark Steel (MV) 72% | T2 Combat 76% | NanoSuit (HV) 90% | T3 Combat (EV) 88% → 92% | Quantum (IV) 100% |
|---|---|---|---|---|---|---|
| M4 (MV) | 7 → 7 | 18 → 20 | 19 → 23 | 29 → 40 | 56 → 84 | 45 → 89 |
| AK47 (MV) | 6 → 6 | 16 → 18 | 17 → 20 | 26 → 36 | 50 → 75 | 40 → 80 |
| AS50 (HV) | 2 → 2 | 4 → 6 | 4 → 6 | 6 → 10 | 9 → 25 | 7 → 16 |
| Sonic Rifle (EV) | 3 → 4 | 5 → 9 | 5 → 10 | 7 → 15 | 11 → 39 | 9 → 25 |
| Blaster Rifle (IV) | 6 → 10 | 13 → 28 | 14 → 31 | 20 → 52 | 33 → 122 | 27 → 97 |
| Deatomizer (IV) | 4 → 7 | 8 → 18 | 9 → 20 | 12 → 32 | 21 → 76 | 17 → 61 |
| Grim Reaper (LuV) | 2 → 3 | 4 → 9 | 5 → 10 | 11 → 24 | 9 → 30 | ∞ → ∞ |
| Death Ray (ZPM) | 9 → 23 | 9 → 35 | 9 → 36 | 9 → 41 | 11 → 56 | 9 → 45 |

Unarmored, MV and below barely move: early fights stay as they were. At the tier where the player meets them in fitting armor, high-tier NPCs now take two to four times as many hits, mostly from the halved penetration (the Death Ray's 100% penetration made every armor equal). Quantum's 100% only stops the non-penetrating part, so penetration still gets through.

### 3. NPC accuracy: free within 5 blocks, then a growing aim error (D359)

- Within `combat.npcAccuracyRange` (default 5 blocks) an NPC shot keeps its gun's spread, unchanged.
- Beyond it, each shot gets an aim error of up to `min((distance − range) × npcSpreadPerBlock, npcMaxSpread)` degrees (defaults 0.12 per block, at most 4), drawn once per shot, uniform on each axis, and added to every projectile of the shot. One draw per shot means the NPC misjudges where you are: a shotgun's pellets or a Sonic ring keep their pattern and move together.
- Hostile NPCs, the NPC turret and the Attack Helicopter use it. Player accuracy is unchanged. Players and NPCs both roll their gun's spread through `SpreadModel`, but the distance term is NPC-only (`NpcFiring.roll` with an aim error), so nothing ties the two together.

What the error alone does against a standing player (0.6 wide, 1.8 tall), each shot otherwise dead on:

| Distance | Error, ± degrees | Miss at the target, ± blocks | Hits sideways | Hits up and down | Hits |
|---|---|---|---|---|---|
| 5 | 0 | 0 | 100% | 100% | 100% |
| 10 | 0.6 | 0.10 | 100% | 100% | 100% |
| 15 | 1.2 | 0.31 | 95% | 100% | 95% |
| 20 | 1.8 | 0.63 | 48% | 100% | 48% |
| 25 | 2.4 | 1.05 | 29% | 86% | 25% |
| 30 | 3.0 | 1.57 | 19% | 57% | 11% |
| 40 | 4.0 | 2.80 | 11% | 32% | 3% |

So NPCs keep up a fight at room and corridor distances, and a sniper NPC across a valley mostly suppresses. The gun's own spread comes on top.

### 4. Armor: the EV set beats the HV NanoSuit (D360)

- **T3 Combat (EV) and the Exo Suit (IV)** go from 22 to **23** physical (projectile, physical) and from 18 to **19** on explosion, energy, fire, ice and lightning; poison 10, radiation 6 and penetration resistance 0.15 are unchanged. 23 points absorb 92%, so the EV set now beats the HV NanoSuit's 90%, which it did not at 88%. The armor bar is 6 + 7 + 6 + 5 = 24.
- **Unchanged:** T1 and T2 sets already sit with their GTNH peers (T2 Combat 76% between Dark Steel and diamond); the IV Power Armor stays at 24 (the cap, 96%); the **HEV suit** stays, because Stormtroopers wear it and bumping it would make them tankier against players.
- [D314](../gregguns-icbm-spec/DECISIONS.md)'s list of beta-balance caps holds no combat items, so nothing from it is folded in here.

### Making it harder

Every knob is a config key, so players set their own difficulty. [docs/config.md](../config.md#make-combat-harder) lists the values: all tier multipliers and the elite one at 1.0, NPC penetration at 1.0, `npcSpreadPerBlock` at 0, knockback on. Those four together give every NPC gun its full stats, as before this record.

## Consequences

- Tests: `NpcFiringTest` (aim error curve, shared draw, no extra RNG draw at 0, tier table, elite), `KnockbackTest` (off, on, bad input), `CombatConfigTest` (defaults and docs), `ArmorTiersTest` and `PowerArmorTest` (23 / 19 rows). Scenarios: `gun.knockback` (on, multiplier 2), `gun.knockback_off`, `npc.shot_tier_damage`, `npc.distance_aim`; the Sonic Rifle and armor scenarios follow the new values.
- Scenario code that measures a gun-hit push turns `combat.knockback` on and back off.
- The armor section of [0021](0021-tier-balance-pass.md) still shows 22 for T3 Combat and Exo; this record supersedes those two rows.
