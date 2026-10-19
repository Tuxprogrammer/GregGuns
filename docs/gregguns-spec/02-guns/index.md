# 02-guns: GregGuns integration

The Techguns baseline is [02-guns](../../techguns-spec/02-guns/index.md). This folder records only where GregGuns differs from it and how it plugs into the GTNH pack. See [CONVENTIONS.md](../CONVENTIONS.md) for the sheet format and [INTEGRATION.md](../INTEGRATION.md) for the evidence rules.

## Overview

Guns touch the pack in five places: how they are built (GT recipe maps), how energy weapons store and gain charge (IC2 electric items and GT chargers), how damage meets GT's and other mods' systems, how the offhand slot (Backhand) interacts with right-click, and how fuel guns take GT fluids. Everything else about guns (stats, firing, reload, projectiles, tooltips, NPC firing, the catalog) has no integration point; see the Techguns spec and the design records.

All evidence is pinned to GT5-Unofficial `5.09.54.205`, Backhand `1.8.16`, Battlegear2-for-Backhand `1.6.8-backhand` and IC2 `2.2.2.828` (API source plus decompiled internals, hedged where used).

## Contents

| Sheet | Topic | Requirements |
|---|---|---|
| [recipes.md](recipes.md) | Every weapon recipe (guns, akimbo, melee, grenades): GT recipe maps, Assembly Line for LuV and ZPM, assumed components. Replaces the old `gt-recipes.md` (now a pointer) | GGS-02-01 to 08, 35 to 42 |
| [energy-electric-items.md](energy-electric-items.md) | Energy guns as `IElectricItem`, GT charging rules, capacity baselines | GGS-02-09 to 16 |
| [damage-interop.md](damage-interop.md) | Damage sources, GT hazmat, other mods' armor, `LivingAttackEvent` listeners | GGS-02-17 to 23 |
| [offhand-backhand.md](offhand-backhand.md) | Right-click fallback, offhand blacklist, Battlegear2, enchantments | GGS-02-24 to 28 |
| [fluid-ammo.md](fluid-ammo.md) | Fuel, bio and air tanks on GT fluids and the Fluid Canner | GGS-02-29 to 34 |

## Integration map

| Techguns feature | GregGuns approach | Pack mod or system | Evidence |
|---|---|---|---|
| Gun crafting-table recipes | Final assembly on `assemblerRecipes` at the 0001 tier voltage; Assembly Line for LuV and ZPM; Hand Cannon and akimbo stay in the crafting table | GT5-Unofficial recipe maps | [recipes.md](recipes.md) |
| Ammo Press, Fabricator, Metal Press | GT Assembler, Lathe, Extruder, Bender, Hammer; GT `round` prefix for slugs | GT5-Unofficial | [recipes.md](recipes.md) |
| Energy Cell ammo, Charging Station | The gun is an `IElectricItem`; GT chargers and battery buffers charge it | IC2 API, GT5-Unofficial | [energy-electric-items.md](energy-electric-items.md) |
| Cancel-and-reimplement damage event | Keep the model, keep other mods' listeners and armor working | GT5-Unofficial (hazmat, armor), GT++, Forge | [damage-interop.md](damage-interop.md) |
| Unflagged fire damage | Fire flag set; secondary effects respect GT hazmat | GT5-Unofficial `HazardProtection` | [damage-interop.md](damage-interop.md) |
| RMB zoom and charge; akimbo as one item | Stop-list entries so the offhand is not also used; guns blacklisted from the offhand | Backhand, Battlegear2 | [offhand-backhand.md](offhand-backhand.md) |
| Fuel, bio and air tanks filled in the Chem Lab | `IFluidContainerItem` tanks filled in the GT Fluid Canner; fuels from GT fuel maps | GT5-Unofficial | [fluid-ammo.md](fluid-ammo.md) |
| Gun enchantability, enchanting | Unchanged: not enchantable (enchantability 0). No enchantment is registered, so no ID collides. | Battlegear2 (IDs 126 to 132), Infernal Mobs | [offhand-backhand.md](offhand-backhand.md#battlegear2-backhand-edition) |

**No integration point** (see [the Techguns spec](../../techguns-spec/02-guns/index.md) and the [design records](../../design/README.md)): gun stats and catalog, firing algorithm, spread and zoom math, reload rules, NBT layout, tooltips and lang keys, NPC firing, the weapon-stats config, grenade behavior. Their pack-facing parts (keybinds, networking, config library) are in [01-core](../01-core/).

## Bolt Action Rifle (M2): deviations

The rifle is the first scoped gun (`gregguns:bolt_action_rifle`, GR-04, LV). Its stats are the Techguns sheet's: 16 damage falling to 10 between 20 and 30 blocks, speed 2.25 blocks per tick, spread 0.05, 15% armor penetration, 6 rounds, a 25 tick rechamber, a 50 tick reload and a zoom of x0.35 FOV and x0.125 spread. What differs:

- **Loading.** One `ammo_rifle_bullet` (RC-30, Ammo Press, 16 per run, Steel rounds as the gate) fills all 6 rounds, as the sheet says, so it uses `ReloadMode.MAGAZINE` like the Revolver and not `INCREMENTAL`.
- **Rechamber sound.** `guns.bolt_action_rifle.rechamber` plays with every shot, beside the fire sound (a new optional `ItemGun` event). The 25 tick fire interval is the rechamber; the bolt is static geometry and the whole rifle cants in the firing transform.
- **Recipe.** GR-04 adds the Iron Receiver (`part_receiver_iron`, RC-13, new) and a glass lens for the scope.
- **Scope.** The overlay is `testscope.png` (downsampled to 1024 at load). The overlay's recoil shove is 0.04 up and 0.02 sideways as fractions of the screen height, much smaller than the sheet's (0.2, 0.25), which is in Techguns' own units.
- **First person and third person.** The sheet's scale 1.5 was recorded for Techguns' 53 texel model; the GregGuns model is 82 texels long, so the profile uses its own scales (see the `// Deviation:` notes in `BoltActionRifleProfile`).
- **NPC attack time.** The sheet's variance of 20 ticks is dropped; `GunStats` has no such field.
- **Camo.** Three textures (default, Olive, Tactical Black) are chosen by the stack's `camo` byte; the Camo Bench that sets it is M4's.

## MV rifles and SMGs, batch B1 (M2): deviations

Thompson SMG (`thompson_smg`, GR-10), Mac10 (`mac10`, GR-11), M4 (`m4`, GR-12) and AK47 (`ak47`, GR-13), all MV, with the SMG and Assault Rifle magazine families (RC-38, RC-45; RC-39, RC-46). Stats are the Techguns sheets' unchanged. What differs:

- **Flat-icon fallback.** None of the four has a clean-room model yet, so `ClientGunDefinitions.FLAT_ICON_FALLBACK` lists them and they draw as their flat item icon (nothing is registered in `GunRenderers`; `ItemGun.isFull3D` holds the icon upright). Zoom, sounds, recoil timing and the HUD all run without a profile. `GunDefinitionsTest` pins the list to the guns that lack a profile, and **the M2 close must change that expectation to an empty list**.
- **Registry name.** The Thompson is `thompson_smg` (the asset names), not `thompson`; display name "Thompson SMG".
- **M4 scope.** The first scope on a non-bolt gun: FOV x0.75 and spread x0.75 while zoomed, through the shared `ZoomState`; no overlay (the future profile sets no `ScopeSpec`). Camos: five (Swamp, Desert, Ice, Hazard); AK47 two (Blue).
- **Magazines.** The loaded SMG magazine is worth 2 loose 9mm bullets and the Assault Rifle magazine 3 rifle bullets; the refund of a forced reload is derived from each gun's own clip (Thompson 10 rounds per bullet, Mac10 16, M4 and AK47 10), so GUN-14's wrong factor does not recur on the Mac10.
- **Recipes.** Redstone and iron nuggets of the Techguns grids are dropped; the AK47's foregrip is two wooden sticks. The Assembler circuits 1, 2, 4, 5 repeat the LV table's numbers, so the uniqueness tests are per tier.
- **Sounds.** Events point at the existing `thompson_*`, `mac10_*`, `ar1_*` (M4) and `ak47_*` files. The M4 has two fire variants, as in Techguns.

## Combat Shotgun and the akimbo pair, batch B2 (M2): deviations

Combat Shotgun (`combat_shotgun`, GR-14, MV), Akimbo Pistols (`akimbo_pistols`, crafting recipes GR-15) and Akimbo Revolvers (`akimbo_revolvers`, crafting recipes GR-16). Stats are the Techguns sheets' unchanged. What differs:

- **Flat-icon fallback.** All three are on `ClientGunDefinitions.FLAT_ICON_FALLBACK`. The akimbo guns need no notes agent: the model pass wires them to the single gun's profile, which draws the model twice (`ModelAkimbo`). Until then they draw their flat item icons.
- **Textures and fire sound.** An akimbo gun is `GunDefinition.akimboOf(single)`: it reuses the single gun's model textures (`textureName()`) and fires the single gun's `guns.<single>.fire` event, as Techguns does (one muffler entry silences both). It has its own reload event (`*_reload_akimbo` files).
- **Tier.** The akimbo pair has no machine recipe, so its tier is its inputs' (LV), not 0001's MV. Note on [0001](../../design/0001-tier-map.md).
- **Combine and split (GGS-02-07, GUN-26 fixed).** `AkimboRecipes` registers two shapeless recipes per pair (NEI lists them): any two single guns combine, one akimbo gun splits. `AkimboNbt` does the arithmetic. The akimbo tag is the first gun's tag with the rounds summed and the **second gun's whole tag (camo included) stored inside it**, so a split returns both guns' own camo and extra keys (Techguns lost the second camo). The rounds are conserved and re-balanced on split (floor half to the recipe's result, the remainder to the second gun), as in the baseline. The second gun is given by the crafted event on the **server only** (no ghost item on the client) and dropped at the player if the inventory is full.
- **Refund rate.** `GunStats.roundsPerItem()` is now clip / items per reload, so an akimbo clip's two items are half each and the forced-reload refund stays at the single gun's 6 rounds per bullet (GUN-14's wrong factor does not recur).
- **Combat Shotgun.** The pump's rechamber sound plays with each shot (`guns.combat_shotgun.rechamber`); 8 pellets spawn together from one round; 15% armor penetration. Recipe: the pump tube is two steel sticks.
- **Recipes.** The Combat Shotgun's Assembler circuit is 3 at MV (circuit 3 is free at MV; the LV Sawed Off's 3 is a different tier).
## Launchers, batch B5 (M2): deviations

Rocket Launcher (`rocket_launcher`, GR-20) and Grenade Launcher (`grenade_launcher`, GR-21), both HV, with the Rocket (`ammo_rocket`, RC-34, HV press) and the 40 mm Grenade (`ammo_40mm_grenade`, RC-33, MV press, defined here with its first gun). Stats are the Techguns sheets' unchanged. What differs:

- **Projectiles.** `RocketProjectile` (entity 6) and `GrenadeLauncherProjectile` (entity 19) are registered through `BulletLauncher.register` for `ProjectileKind.ROCKET` and `GRENADE`; `GunStats` gains `blast(radius, gravity)`. Both render as their ammo item's sprite and both launchers use the flat-icon fallback, until the model batches land.
- **Explosions.** Both go through `ProjectileExplosion` (the frag grenade's path): `explosions.blockDamage` (default off), `explosions.selfDamage`, Forge's explosion events, the per-block `BreakEvent` check and the `mobGriefing` rule for NPC shooters all apply. PFX-14 (no checks), PFX-25 (shooter immune) and CORE-19 (no knockback packet) are not reproduced; tests are named `doesNotReproduce<ID>_...`.
- **Rocket direct hit versus splash.** A rocket that meets an entity deals the gun's full damage to it once (with the x3 knockback) and the target is left out of the splash, so it is not hit twice; everything else within 1.5 radii takes the ramp (full to half the radius). Techguns hit the target directly and then let it take the blast as well. A rocket that runs out of lifetime in the air ends without exploding, as in Techguns. The blast centre stands 0.15 blocks off the wall so it is in open air.
- **40 mm grenade.** Falls under the gun's 0.01 gravity, two bounces, each costing 20 ticks of fuse (fuse 100); explodes on an entity, on a hard hit with no bounces left, or when the fuse ends. As in the grenade family, the blast alone damages a direct hit (GGS-04 grenades deviation).
- **GUN-04 not reproduced.** Techguns' launcher ignored the gun's lifetime, accuracy, penetration and damage bonus. The stats here hold the figures it hard-coded (100 ticks, 0.05, 0), so they are honored like any gun's and the NPC damage scale applies.
- **Reload.** The rocket launcher takes one loose rocket and returns nothing (GUN-13 has nothing to refund); the grenade launcher loads up to 6 grenades, keeping the rounds already in it.
- **Recipes.** GR-20 (circuit 8) and GR-21 (circuit 7) follow recipes.md. RC-33 and RC-34 use the vanilla TNT block for the TNT, as RC-26 does. No GT recycling data is attached to the two rounds yet.
- **Sounds.** Events point at the existing `rocket_*` and `grenadelauncher_*` files; the explosion plays the grenade family's `random.explode`.

## AS50, LMG and Akimbo Mac10s, batch B4 (M2): deviations

AS50 Sniper Rifle (`as50`, GR-40, HV), LMG (`lmg`, GR-41, HV) and Akimbo Mac10s (`akimbo_mac10s`, crafting recipes GR-42), with High Caliber Rounds (`ammo_high_caliber_round`, RC-31), the AS50 Magazine (`mag_as50`, RC-40, RC-47) and the LMG Magazine (`mag_lmg`, RC-41, RC-48), all HV presses. Stats are the Techguns sheets' unchanged. What differs:

- **Flat-icon fallback.** All three are on `ClientGunDefinitions.FLAT_ICON_FALLBACK` until their model pass. The Akimbo Mac10s will take the generic akimbo renderer, which draws the Mac10 profile twice.
- **Ammo.** The AS50 Magazine holds 10 rounds from 2 High Caliber Rounds (5 rounds per round on a forced reload). The LMG Magazine holds 100 rounds from 8 Rifle Bullets; the LMG reuses the existing `ammo_rifle_bullet`, so only the magazine pair is new. A forced reload on the LMG refunds one bullet per 12 rounds left (100 / 8 floored), not Techguns' 12.5: the refund is in whole bullets either way, and the integer step can give one bullet more than the sheet's floor in rare cases (97 rounds left: 8 bullets, sheet 7).
- **Scope.** The AS50 (FOV x0.35, spread x0.125) and the LMG (x0.75, x0.75) use the shared zoom state. Neither has a scope overlay yet: the overlay belongs to the model pass.
- **Akimbo Mac10s: double shot.** Every pull spends 2 rounds and launches 2 bullets, one from each hand (`akimbo(true)` with `ammoPerShot(2)`); the hands do not alternate, unlike the Akimbo Pistols and Revolvers. A full 64-round clip is 32 pulls. The refund is the single Mac10's 16 rounds per bullet (GUN-14 not reproduced); the reload takes two SMG Magazines and returns two empty ones.
- **Tier.** The Akimbo Mac10s have no machine row, so their tier is the input gun's (MV), not the HV listed in the recipe sheet; see the note on [0001](../../design/0001-tier-map.md). The AS50 and LMG are HV with a stainless steel gate.
- **Recipes.** GR-40 (circuit 5) and GR-41 (circuit 6) follow recipes.md (seven and six distinct inputs; GT's assembler map takes up to nine). No GT recycling data is attached to the new magazines or the round yet.
- **Sounds.** Events point at the existing `as50_*`, `lmg_*` and `mac10_reload_akimbo` files; the Akimbo Mac10s fire the Mac10's `guns.mac10.fire` event.

## Pack-side changes

These land as a PR to the [GTNH modpack repository](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack) and are collected in [PACK-CHANGES.md](../PACK-CHANGES.md).

| File | Change | Why | Sheet |
|---|---|---|---|
| `config/backhand.cfg` | In `general`, add `gregguns:<registry name>` for each gun with right-click zoom, charge or reload (and each hold-to-charge tool gun) to `mainhandUseStopsOffhandFallback` | Stops one right-click from also using the offhand item | [offhand-backhand.md](offhand-backhand.md) |
| `config/backhand.cfg` | In `general`, add `gregguns:<registry name>` for each gun item to `offhandBlacklist` | A gun in the offhand cannot fire; the slot would only hide a loaded weapon | [offhand-backhand.md](offhand-backhand.md) |

Registry names are fixed when M1 and M2 register the items. GregGuns never writes to the pack's configs at runtime.

## Related areas

- [01-core](../01-core/): keybinds (the zoom decision in the offhand sheet's open questions), config library.
- [05-armor](../05-armor/): which GregGuns armor sets implement GT's `IHazardProtector`; the baubles sheet for equipment slots.
- [06-items](../../techguns-spec/06-items/index.md): ammo and tank items, whose recipes also use the GT maps described here.
- [10-entities-npcs](../../techguns-spec/10-entities-npcs/index.md): NPC-held guns (no EU spend), infernal-mob drops of guns.

## Open questions

| Question | Needs | Sheet |
|---|---|---|
| LuV and ZPM final recipes: GT's own LuV motor uses `RECIPE_IV` on the Assembly Line, one tier below the item. Proposed answer: keep 0001 (the item's own tier), because the GT pattern exists to avoid a hatch-and-motor loop that weapons do not have. | Confirmation; no amendment to 0001 | [recipes.md](recipes.md#open-questions) |
| Stielgranate is not in 0001 (placed at LV); akimbo tier cannot be enforced by a crafting-table combine; upgrade recipes lose camo; Alien Blaster has no Techguns recipe. | Notes to 0001 and decisions | [recipes.md](recipes.md#open-questions) |
| 0003 says an item charges in a charger "of that tier or higher". GT's chargers and buffers accept the **exact** tier only. | Amendment to 0003 | [energy-electric-items.md](energy-electric-items.md) |
| How the damage model runs inside vanilla's hit: keep Techguns' cancel-and-reimplement, or run in `LivingHurtEvent` with an unblockable source and a direct `ApplyArmor` call. The sheets assume the second. | New record under D4 (addendum to 0004) | [damage-interop.md](damage-interop.md) |
| RMB zoom conflicts with an offhand item (stop list) and blocks an offhand grenade. Moving zoom to a key removes the conflict. | Input decision in [01-core](../01-core/) | [offhand-backhand.md](offhand-backhand.md) |

Closed by [0010](../../design/0010-material-remap-and-tier-additions.md): the material remap (Obsidian Steel and carbon parts, tier-material hardware), tank capacity (1,000 mB, see [fluid-ammo.md](fluid-ammo.md)) and the dropped Energy Cell (energy weapons are their own batteries). See [recipes.md](recipes.md#open-questions).
