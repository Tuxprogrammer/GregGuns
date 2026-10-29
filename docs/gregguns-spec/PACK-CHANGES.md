# Pack-side changes

Every change GregGuns once needed outside this repo, collected from the area indexes, each with a **Status**. GregGuns never writes another mod's config file at runtime (see [CONVENTIONS § Rules](CONVENTIONS.md#rules)).

**Delivery.** There is no pull request to GT-New-Horizons-Modpack or to GT5-Unofficial ([design record 0015](../design/0015-m9-self-contained-polish.md)). GregGuns is one portable jar. Each row is one of:

- **Done in jar.** The jar does it, by a config default, an event or registry hook, or an in-memory adjustment of another mod's already-loaded settings. Nothing is written to that mod's config file. Every such hook that touches another mod has a `compat.*` key in `config/gregguns.cfg` (default on; see [config.md](../config.md#compat-compat)) and is inert when the other mod is absent. Code: `com.gregguns.compat`.
- **Optional server-owner step.** The jar has no way to do it (the other mod offers no API and the data lives in its own file). GregGuns works without it; the step only tunes the pack. The exact file and lines are given.
- **Not applicable.** Nothing to do, with the reason.

**Paths** are relative to the pack's `.minecraft/` folder. In the reference pack that is `reference/gtnh/2.9.0-RC-2/.minecraft/`. Verified against the pack's mod versions in `MOD-INDEX.md` and the source at those tags.

**Unverified in game.** The in-memory hooks (Backhand, AngerMod, Mobs Info, Thaumcraft) are unit tested for their pure parts and for being inert without the other mod; their effect inside the real pack has not been checked in a running client or server.

## Config and data of other mods

### Loot ([0007](../design/0007-pack-loot-and-infernal-mobs.md))

| File | Change | Status | Sheet |
|---|---|---|---|
| `config/TooMuchLoot/loot/gregguns_<category>.xml` | `loading_mode="ADD"` files for the vanilla chest categories (`bonusChest`, `dungeonChest`, `mineshaftCorridor`, `villageBlacksmith`, `pyramidDesertyChest`, `pyramidJungleChest`, `strongholdCorridor`, `strongholdCrossing`, `chest1`-`chest4`) | **Optional server-owner step.** The pack ships these categories as `OVERRIDE`, which wipes anything added from code, and TooMuchLoot has no API. A pack that wants GregGuns items in vanilla chests uses the drafts in [pack-loot.md](06-items/pack-loot.md#draft-add-files); a missing or misspelled `loading_mode` defaults to `OVERRIDE` and wipes the category. GregGuns' own structures and NPCs carry their own loot in code (`worldgen/loot`, `gregguns:*` categories), so nothing breaks without it | [pack-loot.md](06-items/pack-loot.md#draft-add-files) |
| `config/EnhancedLootBags/LootBags.xml` | `Loot` lines in `steam_age` and `tier_1_lv` to `tier_7_zpm`, each with items of its own tier | **Optional server-owner step.** Bags are data only; EnhancedLootBags has no registration API | [pack-loot.md](06-items/pack-loot.md#enhancedlootbags) |
| `config/roguelike_dungeons/settings/loot_weapons.json`, `loot_supplies.json`, `loot_armor.json` | GregGuns rules per dungeon level (0-1 LV, 2-3 MV, 4 rare HV) | **Optional server-owner step.** Dungeon chests are JSON only | [pack-loot.md](06-items/pack-loot.md#roguelike-dungeons) |
| `config/InfernalMobs.cfg` `droppedItemIDsElite` / `Ultra` / `Infernal` | Append ammunition, magazines, grenades and consumables only (no guns, no armor) | **Not applicable by default; optional owner step for route A.** Route B below (our own handler) is in the jar and on by default. Add to the lists only if the pack also wants GregGuns drops from vanilla mobs, and then set `npcs.infernalTieredLoot=false` or the NPCs drop twice | [pack-loot.md](06-items/pack-loot.md#infernal-mobs) |
| `config/GTNewHorizons/CustomDrops.xml` | `CustomDrop` entries for boss NPCs (full class name; player kills only) | **Optional server-owner step.** Fixed boss rewards are a pack choice | [pack-loot.md](06-items/pack-loot.md#gtnh-customdrops), [mob-drops-nei.md](10-entities-npcs/mob-drops-nei.md) |

### Mobs

| File | Change | Status | Sheet |
|---|---|---|---|
| `config/InfernalMobs.cfg` `permittedentities`: `AttackHelicopter=false` | Keep the helicopter, which has its own boss bar, from rolling infernal | **Done in jar.** `AttackHelicopter` writes the Infernal Mobs opt-out marker in its constructor (`InfernalHooks.markNeverInfernal`), the mechanism already used for spawner NPCs | [infernal-mobs.md](10-entities-npcs/infernal-mobs.md) |
| `config/InfernalMobs.cfg` `permittedentities`: explicit `true` per hostile NPC class | Make every NPC eligible | **Not applicable.** Infernal Mobs treats an unknown class as permitted and writes `true` for it on first sight ([InfernalMobsCore#L438-L466](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/InfernalMobsCore.java#L438-L466)) | [infernal-mobs.md](10-entities-npcs/infernal-mobs.md) |
| `config/InfernalMobs.cfg` route B | GregGuns drops tiered ammo, magazines and one MV gun from its own infernal NPCs | **Done in jar.** `InfernalDropsHandler` (soft dependency, HIGH priority, reflection into `InfernalMobsCore`), switch `npcs.infernalTieredLoot`, default `true`; inert without Infernal Mobs | [infernal-mobs.md](10-entities-npcs/infernal-mobs.md#infernal-mobs-tiered-gregguns-loot) |
| `config/gregguns.cfg` `npcs.infernalAllowSpawnerNpcs` | Spawner-block NPCs do not roll as infernal | **Done in jar.** Default `false`; spawner NPCs carry the opt-out marker | [infernal-mobs.md](10-entities-npcs/infernal-mobs.md#infernal-mobs-opt-out-for-farmable-sources) |
| `config/InfernalMobs.cfg` `entitybasehealth` | Base health per NPC class | **Optional server-owner step.** Without it Infernal Mobs freezes the first spawned instance's health into the file; our NPCs spawn with their fixed base health, so the frozen value matches. Add `<ClassSimpleName>=<hp>` lines only to pin it ahead of time | [infernal-mobs.md](10-entities-npcs/infernal-mobs.md) |
| `config/InfernalMobs.cfg` `mm_berserk.berserkMaxDamage` | A positive value, for example `20.0` | **Optional server-owner step.** The shipped `0.0` makes Berserk mobs deal no damage; a pack-wide bug, not ours | [infernal-mobs.md](10-entities-npcs/infernal-mobs.md) |
| `config/mobsinfo/mobsinfo.cfg` `MobBlacklist` | `gregguns.SimpleTurret` (it would list as an empty mob with `IncludeEmptyMobs=true`) | **Done in jar.** `PackListHooks.mobsInfoBlacklist` appends to `Config$MobHandler.mobBlacklist` in postInit (key `compat.listHooks`). Whether Mobs Info has read the list by then is unverified; if the turret still lists, add the name to the file ([Config.java#L127-L139](https://github.com/GTNewHorizons/Mobs-Info/blob/0.6.0-GTNH/src/main/java/com/kuba6000/mobsinfo/config/Config.java#L127-L139)) | [mob-drops-nei.md](10-entities-npcs/mob-drops-nei.md) |
| `config/angermod.cfg` `kamikaze.butcherItems` | Add `gregguns.` | **Done in jar.** `PackListHooks.angerModButcherItems` (key `compat.listHooks`). AngerMod matches `item.getUnlocalizedName().toLowerCase().contains(entry)`, and ours are `item.gregguns.*` ([KamikazeRevenge#L26-L35](https://github.com/GTNewHorizons/AngerMod/blob/1.0.7/src/main/java/com/github/namikon/angermod/events/KamikazeRevenge.java#L26-L35)) | [spawning.md](10-entities-npcs/spawning.md) |
| `config/ThaumcraftMobAspects/gregguns.cfg` | Aspect values for each NPC | **Done in jar.** `MobAspects` calls `ThaumcraftApi.registerEntityTag` for the table in [registry-and-pack-hooks.md](10-entities-npcs/registry-and-pack-hooks.md) (key `compat.thaumcraftMobAspects`). ThaumcraftMobAspects still writes an empty `gregguns.cfg` on first start; a pack that fills it adds a second registration for the same name, so leave it empty or turn the key off | [registry-and-pack-hooks.md](10-entities-npcs/registry-and-pack-hooks.md), [thaumcraft-aspects.md](06-items/thaumcraft-aspects.md) |
| `config/enderio/PoweredSpawnerConfig_User.json` `blackList` | Every `gregguns.<Name>` NPC (EnderIO's `brokenSpawnerDropChance=1.0` would turn a broken GregGuns structure spawner into a Broken Spawner of its NPC and release up to 6 extra spawn cycles) | **Done in jar.** `EnderIoSpawners` sends `poweredSpawner:blacklist:add` by IMC per NPC in init (key `compat.enderioSpawnerBlacklist`); EnderIO checks the blacklist before both the drop and the burst ([BlockPoweredSpawner#L238-L318](https://github.com/GTNewHorizons/EnderIO/blob/2.10.48/src/main/java/crazypants/enderio/machine/spawner/BlockPoweredSpawner.java#L238-L318)). The user file is read before IMC, so a pack's own list keeps ours (ICBM D273) | [structure-placement.md](11-worldgen/structure-placement.md#spawner-marks) |
| `config/enderzoo/SpawnConfig_User.xml` | `entry` rows `mobName="gregguns.<Name>"` | **Optional server-owner step.** Per-biome or per-dimension spawn tuning; GregGuns has its own spawn weights and caps (`npcs.*`) | [spawning.md](10-entities-npcs/spawning.md) |

### Items, input and UI

| File | Change | Status | Sheet |
|---|---|---|---|
| `config/backhand.cfg` `general.mainhandUseStopsOffhandFallback` | Every gun with right-click zoom, charge or reload, so one click does one thing | **Done in jar.** `BackhandCompat.apply` appends every registered gun (`GunItems.qualifiedNames()`; the unscoped ones too, which is harmless) to the loaded `BackhandConfig` array in postInit (key `compat.backhandGuns`). The file is untouched | [offhand-backhand.md](02-guns/offhand-backhand.md) |
| `config/backhand.cfg` `general.offhandBlacklist` | Every gun (a gun cannot fire from the offhand). Grenades stay out | **Done in jar.** Same hook. This row was listed twice in earlier drafts with different gun lists; the hook covers the union, which is every registered gun | [offhand-backhand.md](02-guns/offhand-backhand.md) |
| `config/NEI/collapsibleitems.cfg` | Collapsible groups for magazines, tanks, ammo by caliber, armor sets; spawn eggs added to the egg group | **Partly done in jar; the collapse itself is an optional step.** NEI has no API for collapsible groups (the file is read by `CollapsibleItems.load`, [L62-L80](https://github.com/GTNewHorizons/NotEnoughItems/blob/2.8.155-GTNH/src/main/java/codechicken/nei/CollapsibleItems.java#L62-L80)), and it is a client file. The jar adds NEI subsets `GregGuns.Guns`, `.Ammo`, `.Armor` and hides technical items. Pack authors who also want collapsing add the lines in [nei-visibility.md](06-items/nei-visibility.md#nei-collapsible-groups-file-only) | [nei-visibility.md](06-items/nei-visibility.md) |
| `config/NEI/handlerordering.csv` | A row for `gregguns.ammoPress` | **Not applicable.** Missing rows default to 0 and NEI has no API for ordering; the tab sits with the other zero-order tabs. A pack that wants it beside the Assembler adds one `gregguns.ammoPress,<n>` line. (The Reaction Chamber tab was removed by [0012](../design/0012-retire-reaction-chamber-and-retort.md).) | [nei-handlers.md](08-machines-production/nei-handlers.md) |
| `config/GTNewHorizons/CustomToolTips.xml` | A `customtooltip.recharge_tier_machine` row per powered item | **Optional server-owner step.** Cosmetic; our own tooltips already show the tier and charge | [powered-flight.md](05-armor/powered-flight.md) |
| `config/Baubles.cfg` | none | **Not applicable.** [0009](../design/0009-gtnh-integration-defaults.md) reuses the existing `head`, `wings` and `quiver` slot types | n/a |

### Questbook ([12-questbook](12-questbook/index.md))

**Done in jar.** The 121 quests ship inside the GregGuns jar, and the hidden key `questbook.install` installs them into the server's BetterQuesting database ([design record 0015](../design/0015-m9-self-contained-polish.md), [index.md](12-questbook/index.md#delivered-from-the-jar)). The one optional owner step is a position file that cross-links `gg.npc.intro` into `QuestLines/KillAlltheThings-AAAAAAAAAAAAAAAAAAAAJQ==/` ([quest-line.md](12-questbook/quest-line.md#relation-to-the-packs-lines)).

### Recipe-chain notes (no file change; Status: Not applicable)

**Bio Gun ammo chain.** No pack file changes: the Bio Tank reads BartWorks' E. coli culture fluid and GregGuns adds one Bacterial Vat recipe in code. The questbook should teach the path: Brewery makes Biomass, the Bio Lab (HV) makes the E. coli dish, the Bacterial Vat (EV glass) turns Biomass into culture fluid, a Fluid Canner fills the Bio Tank. The pack must not remove or override `bacterialVatRecipes` or `bioLabRecipes`; confirm with `/gregguns dumprecipes`.

**Plasma Membrane and Large Fluid Cell inputs.** No pack file changes either. Cybernetic Parts (RC-20) consume BartWorks' Plasma Membrane (`ItemList.PlasmaMembrane`), which the HV Centrifuge makes from 1,000 mB of the same E. coli culture fluid (circuit 17, 60 s), so the pack must keep that Centrifuge recipe (and the LuV Extractor alternative from stem cells). The Fuel, Bio and Compressed Air Tanks (RC-52 to RC-54) consume GT's stainless, titanium and aluminium Large Fluid Cells; the pack must keep their Assembler recipes. A pack script that removes any of these makes the GregGuns row uncraftable, not wrong, so `/gregguns dumprecipes` is the check.

**Recipe scripts (0012).** The Laser Focus (RC-64) is a recipe in GT's `laserEngraverRecipes` and the Anti Gravity Core (RC-65) one in `assemblerRecipes`. Pack scripts that edit or remove entries from those two maps now see two more recipes. GregGuns registers no recipe map of its own for them.

## Server settings

| File | Change | Status | Sheet |
|---|---|---|---|
| `server.properties` | `allow-flight=true` on any server where players use flight gear | **Optional server-owner step.** Vanilla 1.7.10 kicks a survival player who hovers for about 80 ticks ("Flying is not enabled on this server"). The Anti Gravity Device and the Glider are motion-based, like GT's Jetpack, and need this like any other pack flight. A mod cannot change `server.properties` | [implementation-notes.md](05-armor/implementation-notes.md) (FLT-DEV-01) |

## GT5-Unofficial: adopter-only note

**Status: Not applicable to the jar.** GregGuns uses MetaTileEntity IDs in its own range, 20100-20163, recorded in `GregGunsMTEIDs`, the [machine roster](08-machines-production/machine-roster.md) and `GregGunsMTEIDsTest`. It does not edit GT5-Unofficial. Only an adopter who wants these IDs reserved upstream, as CropsNH did for 28001-28055, would send the entries below to the GT5-Unofficial maintainers; GT has no automatic ID allocation for addons, and named entries in `src/main/java/gregtech/api/enums/MetaTileEntityIDs.java` are how external mods reserve IDs. See [metatileentity-framework.md](08-machines-production/metatileentity-framework.md#gt5-unofficial-mte-id-reservation) and the [adopter list](../HANDOFF-PLAN.md#adopter-list).

| Name (ID) | State |
|---|---|
| `GregGunsAmmoPressSteam(20100)`, `GregGunsAmmoPressLV(20101)`, `MV(20102)`, `HV(20103)`, `EV(20104)` | built (M4) |
| `GregGunsAmmoWorks(20105)` | built |
| `GregGunsReactionChamberEV(20106)`, `GregGunsReactionChamberIV(20107)`, `GregGunsRetort(20108)` | **retired, reserved forever** ([0012](../design/0012-retire-reaction-chamber-and-retort.md)): no machine is registered, but a world may still hold one, so the ID must never go to another addon |
| `GregGunsRepairBenchLV(20109)`, `MV(20110)`, `HV(20111)`, `EV(20112)`, `IV(20113)` | built |
| `GregGunsArmoryGantry(20114)`, `GregGunsCamoBench(20115)` | built |
| 20116-20163 | free inside the proposed block |

The same adopter note covers `src/main/java/gregtech/api/enums/Textures.java` (page-owner comment `page 50: GregGuns casings`, proposed).
