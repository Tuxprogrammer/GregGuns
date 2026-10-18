# M3 wave A: armor framework and T1 sets (implementation notes and deviations)

Covers `com.gregguns.armor` (framework, bonuses, API), `com.gregguns.armor.logic` (pure data and math) and the T1 Scout, Miner and Combat sets. Branch `m3/armor-core`. Sibling sheets: [recipes.md](recipes.md), [hazard-protection.md](hazard-protection.md).

## Shape

- A set is data: `ArmorCatalog` holds `SetDef` objects (material, tier, pieces, camos, full-set bonus). `ArmorItems.register` creates one `ItemGregGunsArmor` per piece from the catalog. T2, T3 and powered sets are added by appending to `ArmorCatalog.ALL`.
- Protection is plain Java (`ArmorProtection`, `ArmorMaterialDef`) and unit-tested against the [armor-catalog](../../techguns-spec/05-armor/armor-catalog.md) tables. `ItemGregGunsArmor` implements Forge `ISpecialArmor` (ratio `points / 25` per hit type), `TypedArmor` (penetration resistance, read once per hit by the M1 damage handler), GT `IHazardProtector` and `Tiered`.
- Bonuses: `SetEvaluator` sums piece bonuses and adds a set's full-set bonus once when every piece is worn and intact; `BonusLifecycle` remembers what is applied so attribute modifiers are rewritten only on change; `ArmorEvents` applies and strips them.
- Read API: `Armor.armorValue`, `Armor.penetrationResistance`, `Armor.bonus`, `Armor.bonuses`, `Armor.isWearingFullSet`, `Armor.evaluate`.

## Deviations

| Id | Techguns or spec | GregGuns | Why |
|---|---|---|---|
| ARM-DEV-01 | Penetration resistance is per piece and compounds (ARM-02) | A piece carries `resistance * slot share`; the damage handler sums the worn pieces once per hit | Already required by [0004](../../design/0004-damage-model.md); a full set equals the material value |
| ARM-DEV-02 | No full-set bonuses | `SetDef.fullSetBonus` exists (empty for all T1 sets) and is detected and applied by `SetEvaluator` | Hook for later sets; the task asks for full-set detection |
| ARM-DEV-03 | Step height reset to 0.5 every tick (CORE-14/ARM-07) | `StepHeightPolicy`: raises only when lower than the assist height, restores only a value it set. Applied by the client module for the local player. No T1 set grants step assist | Test `doesNotReproduceCORE14_*`, scenario `armor.compat` |
| ARM-DEV-04 | Toggle-off removes any Night Vision (CORE-15/ARM-29) | `NightVisionPolicy`: refreshes a 220-tick effect only when none exists or it owns it; removes only what it granted. No T1 set grants Night Vision (goggles are the slots branch) | Test `doesNotReproduceCORE15_*`, scenario `armor.compat` |
| ARM-DEV-05 | Bonus modifiers re-created every tick | Modifiers are `setSaved(false)` with fixed UUIDs, rewritten only when the wanted bonuses change, and stripped on logout, death, respawn and dimension change | No leftover bonus in the player save |
| ARM-DEV-06 | Repair metal is iron | Repair metal is a GT plate: Steel for Combat, Cast Iron for Miner; Scout is cloth only. Repair part is `part_heavy_cloth`. `ItemGregGunsArmor.getIsRepairable` matches the unified GT plate | GGS-09-20, GGS-09-21. The Repair Bench (M4) reads `PieceDef.repair()` and `RepairSpec.cost` |
| ARM-DEV-07 | Miner's Pants cost 2 repair parts (ARM-33) | 3 | Looks like a Techguns typo |
| ARM-DEV-08 | Vanilla damage sources are converted by a config guess | `DamageTypes.of`: GregGuns sources keep their type; explosion, fire, projectile, magic (as Energy), else Physical | Fixed, documented rule |
| ARM-DEV-09 | Out-of-range camo index crashes rendering (ARM-12); missing camo tooltips (ARM-11, ARM-13) | The camo index is clamped to 0 at read and in `SetDef.layerTexture`; Scout camos have lang names | |
| ARM-DEV-10 | Spec: Miner gate is `plate.WroughtIron` and `nugget.WroughtIron` | `plate.CastIron` and `nugget.CastIron` | In GT 5.09.54.205 `Materials.WroughtIron` is an alias of Cast Iron (`MaterialsInit`: `Materials.WroughtIron = Materials.CastIron = loadCastIron()`), whose ore name is `CastIron`. The ore names `plateWroughtIron` and `nuggetWroughtIron` have no items; the dev self-check caught it. [recipes.md](recipes.md) still says Wrought Iron and should be read as Cast Iron |
| ARM-DEV-11 | Heavy Cloth owned by area 06 | `part_heavy_cloth` and its RC-21 recipe are registered by the armor module (`ArmorParts`) | No other branch had it; it is a steam-tier hand recipe. If area 06 adds it to `PartItems`, delete `ArmorParts` and keep the registry name |
| ARM-DEV-12 | Armor layer textures are Techguns-style 128 x 64 sheets | `getArmorTexture` returns `gregguns:textures/models/armor/<base>_1.png` (`_2` for leggings) | Vanilla's biped armor model samples 64 x 32 UVs, so these sheets will not map correctly on the vanilla model. 3D models are out of scope; see Open questions |

## Stats carried over

T1 Combat (A = 15), Scout and Miner (A = 13); other types 75% of A, poison and radiation 0; 825 durability per piece; vanilla bar 4/5/4/3 and 3/4/3/3; bonuses from [armor-catalog 5.1 to 5.3](../../techguns-spec/05-armor/armor-catalog.md). The speed bonus is doubled while sprinting (Techguns rule), on top of vanilla's own sprint modifier.

## Open questions

- **Armor layer model.** The existing layer sheets are 128 x 64. Either a small `ModelBiped` subclass with 128 x 64 UVs is needed in the client module, or the sheets must be redrawn at 64 x 32. Needs a visual check; not done (no client run allowed in this wave).
- **Hazard flags.** T1 sets carry none, as the flag table says. Higher sets add `PieceDef.Builder.partialHazards` and `fullHazards`.
- **Recycling data.** The armor items have no GT `ItemData` yet (GGS-06-34 style); add when the Repair Bench and recycling pass is done.
- **Camo switching.** `ItemGregGunsArmor.getCamo` and `setCamo` exist for the Camo Bench (M4); there is no in-hand switch.
- **Gun accuracy.** `ArmorBonus.GUN_ACCURACY` is readable through `Armor.bonus`; the server's shot context reads it through `GunAccuracy.armorBonus` (0 when `modules.armor` is off, clamped to 0..1), so a worn set that gives it tightens every shot's spread by that fraction. No shipped T1 set gives it; `gun.accuracy.armor` uses a stub set.
- **Pack fall handlers.** The server has another fall handler that shortens a 5-block fall to 4 before ours, so scenario fall figures are measured against that baseline.


---

# M3 wave A: special slots and equipment (implementation notes and deviations)

Branch `m3/slots-equipment`. Code: `com.gregguns.armor.slots` (items, Baubles access, GUI, per-player tick), `com.gregguns.armor.slots.logic` (pure rules), `com.gregguns.armor.oxygen` (fallback items) and `.oxygen.gc` (Galacticraft subclasses), `com.gregguns.recipe.EquipmentTable` and `EquipmentRecipes`.

## Shape

- `BaublesCompat` is the only class that links Baubles Expanded (nested `Impl`, loaded after `Loader.isModLoaded`). Equipment is found by slot type through `BaubleExpandedSlots.getIndexesOfAssignedSlotsOfType`, never by a fixed index.
- `ItemBauble` is the base of the goggles, the Ammo Pouch, back devices and the fallback Oxygen Mask. `@Optional.Interface` strips `IBaubleExpanded` and `@Optional.Method` strips every method that names a Baubles type when the mod is absent.
- `SlotEquipment` (one instance, owned by `ArmorEvents`) runs once per server player tick, from the same `ArmorEvents.update` that applies armor bonuses: goggles and Tactical Mask night vision (the result is fed into the existing `NightVisionPolicy`, so potions are safe), Gas Mask and Tactical Mask cure, and the `wings` back device.
- Back-device hook: `BackDevice` (`onBackTick`, `onBackRemoved`, `onBackKey`) and the base class `ItemBackDevice`. A flight item extends `ItemBackDevice` and registers itself; its keybind calls `SlotEquipment.backKey(player, down)`.
- Ammo hook: `com.gregguns.item.gun.AmmoSources` (a provider list). `PlayerAmmoStore` searches the providers' inventories before the main inventory in `count` and `remove`. The armor module registers `AmmoPouch.SOURCE` (the worn `quiver` pouch). This is purely additive to the gun framework: two small loops in `PlayerAmmoStore`; `add` (refunds) still goes to the main inventory only.
- Masks are helmet-slot `ItemArmor` subclasses (`ItemHelmetMask`) so GT's hazard check sees them; they have no armor value and take no wear from hits.
- Electric items: `ElectricGear` wraps IC2's `ElectricItem.manager`; the charge is IC2's standard NBT, `getTier` is the GT voltage index, `getTransferLimit` is `GTValues.V[tier]` (GGS-05-14).

## Deviations

| Id | Spec | GregGuns | Why |
|---|---|---|---|
| SLOT-DEV-01 | `onWornTick` runs the effects server-side | Effects run from `SlotEquipment.tick`, called by `ArmorEvents.update`; `onWornTick` is empty | One tick path that scenarios can drive on a fake player (Baubles never ticks a fake player) |
| SLOT-DEV-02 | Goggles toggle by "keybind or right-click" | Key (default N, `key.gregguns.night_vision`) toggles the worn goggles or Tactical Mask; right-click equips (Baubles helper). A stack with no stored switch is on | Matches the Techguns default-on `enableNightVision` and the Baubles right-click convention |
| SLOT-DEV-03 | Goggles power unspecified (recipes.md open question) | MV electric item, 100,000 EU, 2 EU/t (GT Mech Armor rate), all config | 0003: capacities are config options |
| SLOT-DEV-04 | Tactical Mask at MV (hazard-protection.md) | HV | recipes.md and 0010 decision 5: it consumes the HV Oxygen Mask. The hazard-protection.md table still says MV |
| SLOT-DEV-05 | Tactical Mask cures Poison and Wither for 1 damage each | Costs the night-vision EU drain per cure, only with charge. GAS and BIOLOGICAL protection stays when empty | Passive flags do not depend on charge (GGS-05-11) |
| SLOT-DEV-06 | Gas Mask works while "not empty" | "Spent" is one point earlier: `FilterWear.spent` is `damage >= max - 1`, as for all GregGuns armor that never breaks | Consistency with `ArmorProtection.isBroken`. A spent filter also gives no GAS protection |
| SLOT-DEV-07 | Ammo Pouch recipe assumed from area 06 | New MV Assembler recipe EQ-03: Heavy Cloth x3, leather x2, `plate.Aluminium`, circuit 1, 10 s | Neither sheet defines one; the quest line lists the pouch at MV |
| SLOT-DEV-08 | Ammo Pouch has food slots in Techguns | Six ammunition slots only; accepts loose rounds, loaded and empty magazines and grenades | Open question in baubles-slots.md answered: dropped |
| SLOT-DEV-09 | Gas Mask Filter (RC-25) and Compressed Air Tank (RC-54) belong to area 06 | Both registered by `ArmorParts` as plain parts with their recipes; the tank has no `IFluidContainerItem` behavior yet | Needed by the Gas Mask and Oxygen Tanks recipes. When area 06 adds them, delete the two registrations and keep the names |
| SLOT-DEV-10 | Oxygen Tanks refill from compressed air | Shapeless recipe: Oxygen Tanks (any damage) plus a Compressed Air Tank gives full tanks; Galacticraft's own compressor is an additional route | The Techguns automatic reload from the inventory is not kept |
| SLOT-DEV-11 | One GUI handler per mod (FML) | `EquipmentGuiHandler` is registered for GregGuns; ids 1 and 2 are the pouch (held, worn) | A later module that needs a vanilla-style GUI adds ids there |
| SLOT-DEV-12 | Space protection | `Hazard.SPACE` is never claimed; tooltips say a full spacesuit may be needed | oxygen-gear.md, GGS-05-28 |
| SLOT-DEV-13 | Recipes use `plate.Polyethylene` | `plate.Plastic` | In GT 5.09.54.205 `Materials.Polyethylene` is only a field name; the material is registered under the name `Plastic` (`MaterialsInit.loadPolyethylene` calls `setName("Plastic")`), which `Materials.get` and the ore dictionary use. The dev boot caught it |
| SLOT-DEV-14 | Request no slot assignments; the pack list has them | `BaublesCompat.requestSlots` (preInit) registers and assigns one slot each of `head`, `wings` and `quiver` | Without `manualSlotSelection` (dev runs, other packs) the types get no slot unless an item asks. In the pack `overrideSlots` replaces the assignment, so nothing changes. The scenarios need real slots |

## Not verified here

- Galacticraft is on the compile classpath but not on the dev runtime classpath, so `OxygenMaskGC` and `OxygenTanksGC` compile but have never run. The scenarios exercise the fallback items. The pack run (orchestrator) must check: both items register, the mask fits the GC mask slot, the tanks fit a tank slot and drain one point per 9 ticks, and `ItemTacticalMask` answers `IBreathableArmor`.
- The Gas Mask and Tactical Mask `getArmorTexture` returns the Techguns-style model sheet (`textures/models/armor/gas_mask.png`, 128 x 64). Like ARM-DEV-12 it will not map correctly on the vanilla biped model; a model class is future client work. Goggles, the back devices and the pouch have no worn model.
- `GuiAmmoPouch` (vanilla chest sheet with three slot cells painted over) has not been looked at.

## Open questions

- A dedicated `gregguns_back` slot type is not requested (0009 #2). The `wings` slot is shared with GT's MagLev Harness; a player wearing it cannot wear a GregGuns back device.
- Whether the Oxygen Mask needs a "gear" component of its own (oxygen-gear.md open question) is unchanged; only helmets that implement `IBreathableArmor` supply `GEAR`.
- Jump Pack and Anti Gravity Device recipes: done in wave B (below).


---

# M3 wave B: flight gear (implementation notes and deviations)

Branch `m3/flight-gear`. Code: `com.gregguns.armor.flight` (items, server tracker, client mirror, packet, fall and jump events), `com.gregguns.armor.flight.logic` (pure physics, policy and fall rules), `com.gregguns.client.flight.FlightClient` (client motion), `com.gregguns.recipe.FlightTable` and `FlightRecipes`, config `FlightConfig` ([docs/config.md](../../config.md)). Scenarios `armor.flight.*` in `FlightScenarios`.

## Shape

- Three `wings` back devices extend the wave A framework: `ItemGlider` (HV, no energy), `ItemJumpPack` (HV electric) and `ItemAntiGravityDevice` (IV electric, `CamoChangeable`, 5 camos). The electric two share `ItemElectricBackDevice` (IC2 `IElectricItem`, `getTier` = GT voltage index, transfer limit `GTValues.V[tier]`, charge only through `ElectricGear`; GGS-05-14, design 0008). There is no Jetpack (0009).
- The server decides and spends; the client moves. In 1.7.10 a player's motion is client-authoritative, so `FlightClient` applies the glide and flight motion from the shared pure `FlightPhysics`, using the status the server sends in `PacketFlightStatus` (append-only, clientbound, wearer only: device, engaged, powered). The server repeats an unchanged status every 100 ticks, so a late join, respawn or dimension change recovers with no request packet. `FlightMirror` ties the status to one player entity, so a new entity reads as "no device" until the next send.
- The server also runs the same formulas on its own copy of the motion. On a real player that copy is overwritten by position packets, so it matters for the fall distance (cleared every assisted tick) and for fake players in scenarios.
- Fall: `FlightEvents.onFall` at `LOW`, after the pack's fall handler (which shortens a 5-block fall to 4) and after the boots' bonus in `ArmorEvents`, so it sees what they left; a canceled fall is never received, so nothing is charged for it. Glider and Anti Gravity Device zero the distance only for a fall within 3 ticks of their own assistance; the Jump Pack uses `JumpPackFall` and pays only when it can.
- Jump: `FlightEvents.onJump` adds the +0.8 on both sides (the client from the mirror's `powered` flag, the server after paying), as `ArmorEvents.onJump` does for boots.
- Key: a fourth equipment key, `key.gregguns.back_device`, default J (the Techguns jetpack key), appended to `EquipmentKeybinds` (key order is the wire order). It calls `SlotEquipment.backKey` on press and release: the Glider tracks the held state, the Anti Gravity Device toggles on press.

## Deviations

| Id | Techguns or spec | GregGuns | Why |
|---|---|---|---|
| FLT-DEV-01 | Anti Gravity Device grants vanilla creative flight (`allowFlying`), revoked on empty or removal | Motion-based hover and flight; it never writes `capabilities.allowFlying` or `isFlying`. Jump climbs, sneak descends, move keys fly at 125% of creative speed, no input hovers. It defers (no motion, no drain) while `isFlying` is true | GGS-05-17 forbids writing the ability, and the pack has two systems that clear the flag on unequip (Creative Flight, MagLev Harness). Granting nothing is the strongest form of "only undo what we granted" (`NightVisionPolicy`): another mod's flight is preserved by construction. The task wording "flight granted then revoked" is read as "flight state engaged then ended". Scenario `armor.flight.anti_gravity` asserts the ability is untouched |
| FLT-DEV-02 | Glide on sneak only | Sneak or the held back-device key, while airborne | The task asks for a key; sneak still works |
| FLT-DEV-03 | Jump Pack power: electric (powered-flight.md) or compressed air (fluids.md) | Electric (HV). Per boost jump 2000 EU, per absorbed fall 1000 EU, 1,000,000 EU capacity, all config. The recipe keeps the two Compressed Air Tanks as parts | Answers the recipes.md open question. 0003: EU only |
| FLT-DEV-04 | Anti Gravity Device drain 1 durability per flight tick (12,000 ticks) | 100 EU per flying tick, 2,000,000 EU capacity: about 17 minutes of flight (Techguns: 10). No drain on the ground or for a creative player | 0003 |
| FLT-DEV-05 | Glider lift per tick | Unchanged from Techguns (lift = min(0.08, -0.1 - motionY), forward push the same amount). Gravity (0.08) cancels the lift, so only vanilla drag brakes a fast fall (2% per tick): a fall at -1.0 needs about 85 ticks to settle at -0.18. Feel check owed; if it is too sluggish raise `GLIDE_MAX_LIFT` | Keeps the spec numbers; the orchestrator judges feel |
| FLT-DEV-06 | Free height 8 "while not empty" regardless of toggle | Free height 8 and 20% reduction apply together, only to a paid fall longer than 3 blocks | GGS-05-19: no reduction that energy did not pay for |
| FLT-DEV-07 | Anti Gravity Device vertical speed (none specified) | Rise and sink 0.3 blocks per tick, eased at 0.5 per tick per tick | Creative-like feel; our numbers |
| FLT-DEV-08 | Back-device key P (the task) | J. P is the Ammo Pouch key (`EquipmentKeybinds`) | Not rebinding a shipped key; both are configurable |
| FLT-DEV-09 | Camos for all three | Only the Anti Gravity Device has them (5, `anti_gravity_pack*.png`). The Glider and Jump Pack have one texture each (`glider.png`, `jump_pack.png`), so there is nothing to cycle | The spec lists four jetpack camos for the Jump Pack, but those sheets are not in the asset set. There is no worn model yet, so the camo only shows in the tooltip until one exists |
| FLT-DEV-10 | Anti Gravity Device recipe needs `part_mechanical_3` | `FlightTable` row FL-05 is added at postload only when every GregGuns input is registered; otherwise an INFO line says it is deferred | `part_mechanical_3` (RC-12) belongs to the M4 component branch and is not registered on master. The recipe appears by itself once it is; no code change is needed. A boot with `guns,armor` only also defers it (the Anti Gravity Core is a machines item) |
| FLT-DEV-11 | Glider Backpack and Glider Wing | Registered by `ArmorParts` (HV parts, recipes FL-01 and FL-02 as in recipes.md) | Defined by the armor recipes sheet; area 06 links to it. If area 06 registers them, delete the two registrations and keep the names |

## Not verified here

- Flight feel and every client path: `FlightClient` has never run. Needs a client to check: glide on sneak and on the key (fall rate, forward speed); Anti Gravity hover, climb, sink and speed; Jump Pack boost height; the J key; the chat messages; tooltips and the creative tab listing.
- Server `allow-flight`: vanilla 1.7.10 kicks a survival player for "Flying is not enabled on this server" after about 80 ticks of not falling (the floating counter in `NetHandlerPlayServer`) when `allow-flight` is false. A motion-based hover trips it exactly as GT's Jetpack and Angel Rings do. Listed in [PACK-CHANGES](../PACK-CHANGES.md).
- Sounds: no `sounds.json` events were added (the glider loop, jump boost and anti gravity start sounds of Techguns 4.2 to 4.5 are not played). The asset agent owns the audio set.
- No particles or worn model (Techguns draws wings and flames); out of scope here.
- Charging a worn device in the slot: still "take it off and use a charger" (powered-flight.md open question), unchanged.

# M3 wave B: T2 and T3 sets and steam armor (implementation notes and deviations)

Branch `m3/armor-t2-t3`. Sets (all data in `ArmorCatalog`): T2 Combat, T2 Commando, T2 Riot ("Ranger Veteran"), the Beret, T3 Combat, steam armor and the T3 HEV suit. Power armor and the Exo Suit stay with a later branch; they reuse `PowerSpec.electric`, `ItemElectricArmor` and the powered-bonus machinery below.

## Shape

- `PieceDef.poweredBonuses` and `poweredPartialHazards` hold what only a powered set gives; `SetDef.power()` (`PowerSpec`: NONE, STEAM, ELECTRIC; capacity in units) says how the set is fueled. `SetEvaluator.Worn` carries a `powered` flag that adds the piece's powered bonuses.
- `ArmorPower` (runtime) finds the chestplate, answers `setPowered`, spends costs, refuels, and each server tick writes a `powered` tag to the other worn pieces so GT's per-stack hazard answers can follow the charge (GGS-05-11). `PowerRules` (pure, unit-tested) has the numbers: 1 unit per worn piece per second of walking, jump 5, fall over 3 blocks 5, canceled fire hit 1.
- Steam chestplates keep their units in the stack tag (`power`, 3,600 at most). `ItemElectricArmor` (HEV chestplate only) is an IC2 electric item at EV: 10,000 units at 40 EU each (400,000 EU), so one walking piece costs GT Mech Armor's 2 EU/t.
- New bonus `SPEED_WATER` (HEV, added to the speed attribute while in water or lava). `COOLING_SYSTEM` is implemented (`ArmorEvents.onAttack` at HIGH priority cancels `DamageSource.lava`, `inFire` and `onFire` hits and puts a burning wearer out). `OXYGEN_GEAR`, `GUN_ACCURACY` and `STEP_ASSIST` are read by their existing consumers (`SlotEquipment`, the gun code, `StepHeightClient`); nothing here writes step height or potions, so CORE-14 and CORE-15 are untouched.
- `SetDef.TextureLayout.SINGLE` (steam armor, the Beret) maps a camo to `<base>.png` instead of `<base>_1/_2.png`. `PieceDef.registryName` names the Beret `t2_beret` (not `t2_beret_helmet`).
- Recipes: `ArmorTable.MACHINE` lists every Assembler row; `ProductionTable.Input.modAnyDamage` is the wildcard-damage input the Riot recipes use for the Combat piece. Treated Leather (RC-22) is a Chemical Bath recipe in `ArmorRecipes`.

## Deviations

| Id | Techguns or spec | GregGuns | Why |
|---|---|---|---|
| ARM-DEV-13 | Every powered piece holds up to 10 power, is topped up from the chest and loses 1 a second while moving | Only the chestplate holds energy. It loses 1 unit per worn intact piece per second of walking (4 for a full set, the same total). A piece is powered while its set's chestplate is worn, intact and charged | One store, one rule; the hazard answers follow through the `powered` tag |
| ARM-DEV-14 | Steam: "compressed air tank" refuels, an empty tank comes back | The chestplate burns one `tank_air` from the main inventory (the Techguns ammo slots do not exist) and the player gets an empty GT cell (the tank is made from a cell, RC-54) | There is no empty-tank item; the tank is a plain part ([ARM-DEV-11](#deviations), SLOT-DEV-09) |
| ARM-DEV-15 | Steam jump plays `techguns:effects.steamarmorJump` for every powered leggings type | Steam leggings only, `gregguns:effects.steamarmor_jump` (volume 0.66), added to `sounds.json` with the four existing files | HEV leggings are electric and silent |
| ARM-DEV-16 | HEV "Hazardous Environment" is registered as `t3_miner_*`, camo `hevsuit` | Set id `t3_hev`, camos `t3_hev`, `t3_hev_silver`, `t3_hev_black` | The item and armor textures in the repo are named `t3_hev` |
| ARM-DEV-17 | Steam helmet registry name `steam,_helmet` (typo) | `steam_helmet` | Techguns typo |
| ARM-DEV-18 | HEV chestplate wears out like every piece | It has no durability and never breaks; protection does not depend on charge | IC2 shows an electric item's charge in the item damage when it has durability, which would turn "empty" into "broken" (found by `armor.t3.hev`). GT Mech Armor does the same (`setMaxDamage(0)`). The bench has nothing to repair on it |
| ARM-DEV-19 | Repair uses carbon fibers, a carbon plate, an elite circuit board and a steam armor plate with metal ratio 1.0 | T3 Combat and HEV: `plate.EpoxidFiberReinforced` plus Heavy Cloth. Riot: `plate.Aluminium` plus Treated Leather. T2 Combat: `plate.Aluminium` plus Heavy Cloth. Steam: `part_steam_armor_plating` only. Beret: Heavy Cloth. Commando: none | There is no carbon fiber or elite board; the repair spec has one GT metal and one GregGuns part. Heavy Cloth is the soft part of the tier |
| ARM-DEV-20 | Riot and Exo upgrade by shapeless crafting with NBT discarded | Assembler inputs: the Combat piece at wildcard damage (`ProductionTable.Input.modAnyDamage`); the output is a fresh stack, so no camo or damage carries over (GGS-05-35) | Spec recipes.md |
| ARM-DEV-21 | Techguns adds 5.0 to the speed bonus while aiming a gun | Not carried | It is an item-use slowdown fix owned by the gun branch ([ARM-06](../../techguns-spec/05-armor/index.md#suspected-bugs)) |
| ARM-DEV-22 | Treated Leather and Steam Armor Plating belong to area 06 | `ArmorParts` registers `part_treated_leather` (RC-22, Chemical Bath, LV, 500 mB sulfuric acid, 2 leather to 2 Treated Leather) and `part_steam_armor_plating` (RC-23, Assembler, EV). When area 06 adds them, delete the two registrations and keep the names | The Riot set and the steam armor need them now |
| ARM-DEV-23 | Armor values never depend on power (kept) | Same; the powered gate covers bonuses and powered hazard flags only | Spec armor-catalog 9 |

## Decisions where the spec was ambiguous

- Steam armor tier: EV per [0001](../../design/0001-tier-map.md) and recipes.md, even though the armor burns air instead of EU. The HEV suit is EV per [0010](../../design/0010-material-remap-and-tier-additions.md) decision 5.
- Capacities: steam 3,600 units (Techguns `maxpower`), HEV 10,000 units (Techguns). They are constants in `ArmorCatalog`, not config yet (0003 asks for config; open).
- The HEV chestplate comes out of the Assembler empty (GGS-05-34); a steam chestplate comes out with 0 units.
- Kept as bonuses of the powered set, not of the single piece: a lone powered piece gives nothing (it has no chestplate to read).

## Visual gaps and checks for the orchestrator

- Steam armor layer sheets (`textures/models/armor/steam*.png`) are single 256 x 128 sheets drawn for Techguns' custom `ModelSteamArmor`; the Beret sheets are 64 x 64 for `ModelBeret`; the Riot sheets are for `ModelArmorCoat`. None of those models exist here. On the vanilla biped model the steam, Beret and Riot layers will not line up (same cause as ARM-DEV-12). Needs a client look and then either small `ModelBiped` subclasses or redrawn sheets.
- Check the tooltips of a steam chestplate ("Compressed air: n / 3600") and the HEV chestplate (IC2 energy line), and that both appear in creative as empty and full.
- Check step assist and the HEV water speed with a client run (the step height path is client-only, `StepHeightClient`).
- Check that the steam jump sound plays once per jump.
- Galacticraft is not on the dev runtime classpath, so the HEV and Commando helmets' `OXYGEN_GEAR` is checked only as a bonus value, not against Oxygen Tanks in a GC dimension.

## Not done

- Power armor and the Exo Suit (EU-powered, later branch): reuse `PowerSpec.electric`, `ItemElectricArmor` and `PieceDef.poweredBonuses`.
- Recycling data for the new pieces (same open item as wave A).
