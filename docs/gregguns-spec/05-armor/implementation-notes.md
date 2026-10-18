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
