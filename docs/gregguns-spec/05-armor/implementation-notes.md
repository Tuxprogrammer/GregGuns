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
- **Gun accuracy.** `ArmorBonus.GUN_ACCURACY` is readable through `Armor.bonus`; the gun spread code does not read it yet (gun framework is M2's).
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
- Jump Pack and Anti Gravity Device recipes stay with the flight branch (they need the Glider Backpack part).
