# M3 exit gate audit

Evidence for the four M3 exit-gate boxes in [ROADMAP.md](../../ROADMAP.md#m3--armor-and-equipment), plus the scope items the boxes do not name, for the beta review gate. Audited on master at `354236f4`, after the icon consistency pass (`6faa3669`, `53468cfb`) and the HEV boots recipe change (`20533ebf`). Every test and scenario name below was grepped in `src/`; no gradle build and no scenario run was made for this audit. Scenario meanings are in [docs/testing/README.md](../../testing/README.md). The per-wave evidence is in [implementation-notes.md](implementation-notes.md#m3-exit-gate). The format follows [the M2 audit](../02-guns/m2-exit-gate.md) and [the M5 audit](../04-projectiles-fx/m5-exit-gate.md).

Status: PASS means proven by a test or scenario that runs headless. PARTIAL means the logic is proven but something needs eyes or a pack run. MISSING means nothing exists. "Needs beta client pass" means the evidence is client-only and unverified.

## Verdicts

| # | Line | Status |
|---|---|---|
| 1 | Protection and penetration follow the D4 model, with unit tests | PASS |
| 2 | Bonuses apply and are removed cleanly (CORE-14, CORE-15) | PASS |
| 3 | The special-slot inventory syncs, persists and works with automation | PASS |
| 4 | Powered items run on EU through GT chargers | PASS |
| 5 | Scope: T1 to T3 sets, steam armor, Power Armor, Exo Suit | PASS |
| 6 | Scope: set bonuses | PASS |
| 7 | Scope: the Techguns extra inventory (face, back, hand, ammo) | PASS |
| 8 | Scope: masks, goggles and tanks | PARTIAL |
| 9 | Scope: glider, jump pack, anti-grav (jetpack dropped by design) | PASS |
| 10 | Worn armor rendering | PARTIAL, needs beta client pass |
| 11 | Worn accessory rendering | PARTIAL, needs beta client pass |
| 12 | Icon consistency pass (`6faa3669`, `53468cfb`) | PARTIAL, needs beta client pass |
| 13 | HEV boots recipe change (`20533ebf`) | PASS |
| 14 | NEI visibility, tooltips and the Ammo Pouch GUI | PARTIAL, needs beta client pass |
| 15 | GT recycling data (`ItemData`) on armor items | MISSING |

Totals: 9 PASS, 5 PARTIAL, 1 MISSING. The four roadmap boxes are all PASS and stay ticked.

## 1. Protection and penetration follow the D4 model, with unit tests

| Check | Evidence | Status |
|---|---|---|
| Full-set math | `ArmorProtectionTest`: `fullSetTotalEqualsTheMaterialTotal`, `fullSetRatioSumGivesTheD4Reduction`, `anUnresistableHitSeesNoArmor`, `penetrationResistanceOfAFullSetEqualsTheMaterialValue`, `vanillaPointsMatchTheTable` | PASS |
| ARM-02 not reproduced | `ArmorProtectionTest.doesNotReproduceARM02_penetrationResistanceDoesNotCompoundAcrossPieces` | PASS |
| Material rows match the spec | `ArmorTiersTest`: `t2CombatMaterialIsTheSpecRow`, `t2CommandoMaterialIsTheSpecRow`, `t2RiotMaterialIsTheSpecRow`, `t3CombatMaterialIsTheSpecRow`, `hevMaterialIsTheSpecRowCalledT3Miner`, `steamMaterialIsTheSpecRowCalledT1Steam`; `PowerArmorTest`: `powerArmorMaterialIsTheT3PowerRow`, `exoSuitMaterialIsTheT3ExoRow`; `ArmorCatalogTest` | PASS |
| Protection never depends on charge | `PowerArmorTest.protectionNeverDependsOnPowerAndAChestplateCarriesThirtyPercent` | PASS |
| Through the real damage handler | `armor.damage`, `armor.t2.combat`, `armor.t2.commando`, `armor.t2.riot`, `armor.t2.beret`, `armor.t3.combat`, `armor.t3.hev`, `armor.steam.power`, `armor.power.power_armor`, `armor.power.exo`; `combat.player` (each armor piece counted once) | PASS |

Notes: there is no T1 `...MaterialIsTheSpecRow` test; the three T1 sets are covered by `ArmorCatalogTest` and `armor.equip` (Scout). Verdict: PASS.

## 2. Bonuses apply and are removed cleanly (CORE-14, CORE-15)

| Check | Evidence | Status |
|---|---|---|
| CORE-14 step height | `Core14And15RegressionTest`: `doesNotReproduceCORE14_anotherModsStepHeightIsNeverLowered`, `doesNotReproduceCORE14_equippingOverAHigherStepHeightLeavesItAlone`, `stepAssistRaisesAndRestoresExactlyWhatItReplaced`; scenario `armor.compat` | PASS |
| CORE-15 potions | `doesNotReproduceCORE15_aBrewedPotionSurvivesTheToggleGoingOff`, `doesNotReproduceCORE15_aPotionThatOutlastsOurGrantIsNotRemoved`, `doesNotReproduceCORE15_armorDoesNotRefreshOrShortenAForeignPotion`; `armor.compat` (a 6000-tick Night Vision survives) | PASS |
| Lifecycle | `BonusLifecycleTest`, `SetEvaluatorTest`, `PowerArmorTest.bonusesNeedAPoweredChestplateButTheSetStillCounts`; `armor.lifecycle` (logout, death, respawn, dimension change), `armor.equip` | PASS |
| Powered sets write neither step height nor potions | `armor.power.power_armor`, `armor.power.exo` (`checkUnequipped` clears every attribute) | PASS |

Notes: step assist is a bonus value read by the client (`StepHeightClient` through `StepHeightPolicy`); the server never writes it. How it feels is client-only (line 10). Verdict: PASS.

## 3. The special-slot inventory syncs, persists and works with automation

The spec replaces the Techguns tab with Baubles Expanded slots (GGS-05-01), so syncing worn items is Baubles' own packet (GGS-05-04). GregGuns owns the state in the stack: the electric charge and the Ammo Pouch's six slots.

| Check | Evidence | Status |
|---|---|---|
| Persists | `armor.power.slot_gate` (charged chestplate and filled pouch survive an NBT round trip); `armor.slots.pouch`, `armor.slots.goggles`, `armor.slots.back`; `SlotPayloadTest`, `SlotLogicTest` | PASS |
| Syncs to every client and survives a relog | `mp.slots` and `mp.slots.after_relog` (`SlotSyncScenarios`) through `mp_check` with a relog: both clients match the server, 2026-10-18 (as recorded in the roadmap; this audit did not rerun it) | PASS |
| Automation | `armor.slots.automation`: a worn pouch exposes no inventory, sided inventory or fluid handle, and hoppers move nothing. Decision: no hopper or pipe access to a worn bauble. The recipes are on GT maps (`armor.slots.recipes`, `armor.flight.recipes`) | PASS |

Notes: the multiplayer run predates the icon pass and the HEV boots change; neither touches slots. A re-run is cheap if the beta pass has a two-client session anyway. Charge is deliberately not compared in `mp.slots` (GGS-05-05). Verdict: PASS.

## 4. Powered items run on EU through GT chargers

| Check | Evidence | Status |
|---|---|---|
| Exact-tier charging in real GT Battery Buffers | `armor.power.charger`: HEV (EV), Power Armor (IV) and Exo Suit (IV) chestplates are accepted by the matching tier and charge; one tier up or down refuses, even when forced in (0008) | PASS |
| Rates and capacities | `PowerArmorTest.ivSetsCostTenEuPerTickPerWalkingPieceAndFitAnIvCharger`; `ArmorTiersTest.electricUnitsRoundDownAndMatchTheMechArmorRate`; `armor.t3.hev`, `armor.power.exo` | PASS |
| Other powered items | `armor.flight.jump_pack` (HV), `armor.flight.anti_gravity` (IV), `armor.slots.goggles` (MV) check tier and transfer limit on the same `ElectricGear` path; they are not put in a real buffer | PASS |

Verdict: PASS.

## 5. Scope: T1 to T3 sets, steam armor, Power Armor, Exo Suit

| Set group | Evidence |
|---|---|
| T1 Miner, Scout, Combat; T2 Combat, Commando, Riot, Beret | `ArmorCatalogTest`, `ArmorTiersTest`; `armor.equip`, `armor.t2.*`; recipes in `armor.recipes`, `armor.t2.recipes`; `ArmorTableTest`, `ArmorTierTableTest` |
| T3 Combat and HEV | `armor.t3.combat`, `armor.t3.hev`, `armor.t3.recipes` |
| Steam armor | `armor.steam.power`, `armor.steam.recipes` |
| Power Armor and Exo Suit | `armor.power.power_armor`, `armor.power.exo`, `armor.power.recipes`; `PowerArmorTest` |
| Hazard flags for every set | `armor.hazmat` |

Notes: the Power Armor and Exo Suit assembler rows (AR-38 to AR-45) are registered at postload only when every input exists (PWR-DEV-06). Whether `part_power_plating` and `part_mechanical_3` are now registered on master, so the rows are no longer deferred, was not re-checked; `armor.power.recipes` is the scenario that shows it. Verdict: PASS.

## 6. Scope: set bonuses

`SetEvaluatorTest` and `BonusLifecycleTest`, plus the bonus values asserted per set in `armor.equip` (Scout speed x1.5, jump, vanilla bar), `armor.t2.*`, `armor.t3.*` and `armor.power.*` (full-set speed, jump, knockback, mining, health). Passive hazard flags do not depend on charge (GGS-05-11); powered bonuses need a charged chestplate. Verdict: PASS.

## 7. Scope: the Techguns extra inventory (face, back, hand, ammo)

| Techguns slot | GregGuns | Evidence |
|---|---|---|
| Face (masks) | Gas Mask and Tactical Mask are helmet items; Night Vision Goggles and the fallback Oxygen Mask use Baubles `head` | `armor.slots.masks`, `armor.slots.goggles` |
| Back | One Baubles `wings` slot: Glider, Jump Pack, Anti Gravity Device | `armor.slots.back`, `armor.flight.*` |
| Ammo | Ammo Pouch in Baubles `quiver`, six ammo slots, scanned first by reload | `armor.slots.pouch`, `SlotLogicTest` |
| Food | Dropped (SLOT-DEV-08) | [baubles-slots.md](baubles-slots.md) |

The tab, its GUI and the two DataWatcher entries are intentionally not built (GGS-05-01). Deviations: SLOT-DEV-01 to SLOT-DEV-14 in [implementation-notes.md](implementation-notes.md). Verdict: PASS.

## 8. Scope: masks, goggles and tanks

| Item | Evidence | Status |
|---|---|---|
| Gas Mask, Tactical Mask (helmet-slot hazmat, filter wear) | `armor.slots.masks`, `armor.slots.recipes` | PASS |
| Night Vision Goggles (Baubles `head`, 2 EU/t, key toggle) | `armor.slots.goggles` | PASS |
| Oxygen Mask and Oxygen Tanks (fallback items, refill recipe SLOT-DEV-10) | `armor.slots.back`, `armor.slots.recipes` | PASS |
| Oxygen Mask and Tanks in a Galacticraft dimension | `OxygenMaskGC` and `OxygenTanksGC` compile but have never run: Galacticraft is not on the dev runtime classpath | PARTIAL |

Needs a pack run: both GC items register; the mask fits the GC mask slot; the tanks fit a tank slot and drain one point per 9 ticks; `ItemTacticalMask` answers `IBreathableArmor`; the HEV and Commando helmets' oxygen gear works against Oxygen Tanks (so far checked only as a bonus value). Verdict: PARTIAL.

## 9. Scope: glider, jump pack, anti-grav

| Item | Evidence |
|---|---|
| Glider (HV, no energy) | `armor.flight.glider`, `FlightPhysicsTest`, `FlightPolicyTest` |
| Jump Pack (HV, electric) | `armor.flight.jump_pack`, `JumpPackFallTest` |
| Anti Gravity Device (IV, electric, 5 camos) | `armor.flight.anti_gravity`, `PacketFlightStatusTest` |
| Recipes | `armor.flight.recipes` |

The Jetpack is intentionally not built: GT Mech Armor ships a jetpack augment, and `FlightItems` records it as design 0009. Flight feel (glide angle, boost height, hover) is client-only; see line 10. Verdict: PASS.

## 10. Worn armor rendering. Needs beta client pass

The box models (ARM-DEV-24) are validated offline by silhouette IoU (0.75 to 0.90 per view, in implementation-notes) and by `ArmorModelDefTest` (boxes fit sheets, no overlapping unwrap rectangles, every camo's sheet exists, every worn piece has boxes) and `RiotBeretModelTest`. No game frame has been read. Run `gregguns test armor.showcase` and read `armor-<set>-<camo>-front|back.png`, or look in a client:

- Power Armor slab: does it clip the ground. Steam Armor chimneys: do they swing with the arm.
- All three camos of Power Armor (Paladin, Outcast, Outcast 2) and Exo Suit (White, Silver, Dark Green), cycled at a Camo Bench.
- The 128 x 64 vanilla-layout sets: ARM-DEV-12 stays open until someone looks.
- Exo Suit speed +80% and jump +0.6 feel; step assist on Power Armor and Exo boots.

Verdict: PARTIAL.

## 11. Worn accessory rendering. Needs beta client pass

Every accessory except the Ammo Pouch has a client-only box model (`com.gregguns.client.accessory`, `AccessoryDefsTest`). Look at (`armor.showcase.accessories`, one client, not in `test all`):

- Goggles and the fallback Oxygen Mask on the head; Gas Mask and Tactical Mask on the helmet slot (head turn, sneak).
- Glider, Jump Pack, Anti Gravity Device and Oxygen Tanks on the back (`wings`): placement, and no clipping with a chestplate.
- A second player's view (Bob sees Alice's accessories).

Verdict: PARTIAL.

## 12. Icon consistency pass. Needs beta client pass

Landed in `6faa3669` (armor icon colors from the worn textures' sources, per slot, with a luma floor so dark sets stay readable; HEV charcoal with orange bands, blue headband and black visor; beret, gas mask, night vision goggles, oxygen tanks, glider, jump pack and tactical mask recolored) and `53468cfb` (boots take the worn leg color; HEV leggings charcoal with orange bands). Together they touch the armor and accessory item PNGs, `tools/asset-gen/icons.py`, `tools/asset-gen/pixel.py` and the preview sheet `docs/assets/previews/icons.png`. No test covers a color, so the proof so far is the preview sheet. Look at, in the creative tab and in an inventory:

- Each icon reads as the worn piece it stands for: HEV (charcoal with orange), Exo Suit, Power Armor, Steam, Riot and Beret.
- Dark sets stay readable on a dark slot.
- Boots and leggings of one set look like a set.

Verdict: PARTIAL.

## 13. HEV boots recipe change

`20533ebf` made the HEV boots take one EV circuit (`circuit.EV` x1 beside the programmed circuit 4), because the T3 Combat and HEV boots were the same assembler recipe. Evidence: `RecipeClashGuardTest.theT3BootsDiffer` and `noTwoMachineRowsShareMapInputsCircuitAndFluids`; `ArmorTierTableTest` was updated in the same commit; the row is in [recipes.md](recipes.md) (T3 Hazardous Environment) and `armor.t3.recipes` covers the four HEV rows. The same commit also moved the Works and Gantry casings to circuits 6 and 7, outside the armor area. Verdict: PASS.

## 14. NEI visibility, tooltips and the Ammo Pouch GUI. Needs beta client pass

Recipes are on the GT maps (`assemblerRecipes`, and the crafting manager for the steam-age hand recipes), which is what NEI reads. NEI's own display is not checked headless, as in the M2 audit. Look at:

- Every armor and equipment recipe appears in NEI under the right machine and tier.
- Tooltips: energy line, camo line and hazard lines on Power Armor and Exo Suit, empty and full.
- `GuiAmmoPouch` (vanilla chest sheet with three slot cells painted over) has never been looked at.
- A charged chestplate in a real GT Battery Buffer GUI of the matching tier.
- Keys: goggles toggle (default N, `key.gregguns.night_vision`) and the back-device action key.

Verdict: PARTIAL.

## 15. GT recycling data on armor items

The open question "armor items have no GT `ItemData`" in implementation-notes is still true: a grep of `src/main/java/com/gregguns/armor` finds no `ItemData`, while guns, ammo and parts have it. No roadmap box requires it and the Repair Bench path does not depend on it, but a player cannot macerate or arc-furnace armor for its metals. Decide before 1.0 whether that is wanted. Verdict: MISSING.

## Other gaps carried from the notes

- Armor camo switching has no in-hand route; it needs the Camo Bench.
- The `wings` slot is shared with GT's MagLev Harness (0009 #2).
- The multiplayer slot-sync evidence is from 2026-10-18 and was not re-run after the icon and recipe commits.
