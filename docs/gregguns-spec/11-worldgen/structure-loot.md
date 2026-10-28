# Structure loot: which category each structure uses

- **Techguns baseline:** [loot.md § How chests get filled](../../techguns-spec/11-worldgen/loot.md#how-chests-get-filled) (the per-structure table), [structure-catalog.md Part A](../../techguns-spec/11-worldgen/structure-catalog.md#part-a-structures-that-spawn-naturally); bug [WGEN-25](../../techguns-spec/11-worldgen/index.md#suspected-bugs)
- **Design records:** [0005](../../design/0005-world-presence.md) ("tier-appropriate"), [0001](../../design/0001-tier-map.md)
- **Pack mods:** TooMuchLoot (`TML`, 4.3.0-GTNH)
- **Roadmap:** M8 (exit gate: loot is tier-appropriate for where the structure spawns)
- **Owner of the tables:** [06-items/pack-loot.md](../06-items/pack-loot.md) holds every table's contents, the TooMuchLoot rules and the category names. This sheet only says which category a structure's chests draw from, and the tier ceiling of each.

## What changes from Techguns

| Techguns behavior | GregGuns behavior | Why |
|---|---|---|
| Vanilla chests in structures draw from `ChestGenHooks.DUNGEON_CHEST`, which Techguns also adds its guns to | Vanilla chests draw from a GregGuns category, never from `DUNGEON_CHEST` | TooMuchLoot loads every vanilla category with `OVERRIDE`, so additions made from code are wiped (below) |
| `lootTier` 0/1/2 picks a T1/T2/T3 table for `tgchest` and `tgchest_weapon` | `lootTier` still picks richness (item count and weights). A separate **tier ceiling** caps the item tier | 0005: loot never exceeds the tier the location implies. Techguns T3 holds flamethrowers and rocket launchers, which 0001 puts at HV |
| Tables hold raw tin, copper, lead, silver and steel ingots | GT materials of the same names (06 decides the exact items) | GT first |
| Chest contents roll from an unseeded `Random` (WGEN-25) | Chest contents roll from the structure's seeded `Random` | Same seed, same structure (see GGS-11-10) |

### Ceiling per structure

The default ceiling is **LV to MV for surface structures** (0005). Ships are surface structures too.

| Structure | Techguns chests ([loot.md](../../techguns-spec/11-worldgen/loot.md#how-chests-get-filled)) | Techguns `lootTier` | GregGuns ceiling | Notes |
|---|---|---|---|---|
| [Small Factory Building](../../techguns-spec/11-worldgen/structure-catalog.md#a1-small-factory-building-factoryhousesmall) | 2 vanilla | 0 | LV | Stone bullets, 9mm, shotgun shells, Hand Cannon, Revolver, Sawed Off |
| [Small Train Station](../../techguns-spec/11-worldgen/structure-catalog.md#a2-small-train-station-smalltrainstation) | 1 vanilla | 0 | LV | As above |
| [Medium House](../../techguns-spec/11-worldgen/structure-catalog.md#a3-medium-house-housemedium) | 8 vanilla, 6 `tgchest`, 4 `tgchest_weapon` | 0 | LV | The Thompson sits in Techguns' T1 table but is MV in 0001, so 06 moves it to the MV tables |
| Gas station (dormant in Techguns; see the index) | 2 vanilla | 0 | LV | |
| [Big Bunker](../../techguns-spec/11-worldgen/structure-catalog.md#a4-big-bunker-bigbunker) | 3 vanilla, 3 `tgchest`, 8 `tgchest_weapon` | 1 | MV | |
| [Military Base](../../techguns-spec/11-worldgen/structure-catalog.md#a6-military-base-militarybasestructure--militarycamp) camp crates and bunkers; barracks chests | 5 % of interior columns hold `tgchest` or `tgchest_weapon`; barracks vanilla chests | 2; barracks table | MV | Barracks table drops the glider and oxygen gear (HV in 0001); keeps gas mask, night vision, T1 and T2 combat armor |
| [Aircraft Carrier](../../techguns-spec/11-worldgen/structure-catalog.md#a7-aircraft-carrier-aircraftcarriernew) | 6 vanilla, 8 `tgchest`, 12 `tgchest_weapon` | 2 | MV | Flamethrower, rocket launcher and rockets leave the T3 table |
| [Submarine](../../techguns-spec/11-worldgen/structure-catalog.md#a8-submarine-submarine) | 3 vanilla, 2 `tgchest`, 4 `tgchest_weapon` | 2 | MV | As the carrier |
| [Alien Bug Nest](../../techguns-spec/11-worldgen/structure-catalog.md#a5-alien-bug-nest-alienbugneststructure--alienbugnest) | none | none | none | Spawners and eggs only |

### Ross128b and Ross128ba: a rolled ceiling (design record 0019)

On the two Ross128 dimensions every structure rolls its own ceiling from MV (2) to UHV (9), as a GT voltage index, from its plan's seed (`RolledCeiling`, GGS-11-10 holds). The weights are the config `worldgen.ross128CeilingWeights` for MV, HV, EV, IV, LuV, ZPM, UV and UHV; the default is 40, 28, 14, 8, 4, 3, 2, 1, so about 40 % of structures stay at MV and 1 % reach UHV. The structure's own `lootTierCeiling` is a floor. The ICBM silos (D38) have no code on master yet; when they land they should call `RolledCeiling`.

- **Tables:** the rolled ceiling picks `weaponchest_t<n>` and `reinforcedchest_t<n>`. M9 adds tiers 4 to 7 (EV, IV, LuV, ZPM), built from the GregGuns guns of those tiers in [0001](../../design/0001-tier-map.md) and the tier's GT alloys. GregGuns has no loot above ZPM, so UV and UHV draw from the ZPM tables. A Military Base's weapon and reinforced crates follow the roll there; its barracks and factory chests stay fixed.
- **Defenders and spawners:** a spawner stamped by a rolled structure carries the ceiling's danger in its spawn data (`GGSpawner.MinDanger`, [structure-placement § Spawner marks](structure-placement.md#spawner-marks)). Its NPCs start at no less than danger 1 (MV), 2 (HV), 3 (EV, IV), 4 (LuV, ZPM) or 5 (UV, UHV), which raises their equipment tier (M7). Counts and caps are unchanged.
- **Deviation:** the spec said spawners "scale with the tier"; only equipment danger scales, not the number of spawners.

An admin can raise a ceiling with the config `lootTierCeiling.<structure>`. The default stays as above. Raising it above the tier that the location implies is the admin's call, not the mod's.

## Integration points

### TooMuchLoot: why GregGuns uses its own categories

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/TooMuchLoot/loot/dungeonChest.xml` starts with `<LootGroup category="dungeonChest" loading_mode="OVERRIDE" …>`. All 16 `.xml` files in `reference/gtnh/2.9.0-RC-2/.minecraft/config/TooMuchLoot/loot/` use `OVERRIDE`.
- **Source:** [`LootLoadingMode`](https://github.com/GTNewHorizons/TooMuchLoot/blob/4.3.0-GTNH/src/main/java/dmillerw/tml/data/LootLoadingMode.java) defines `OVERRIDE`, `ADD` and `REMOVE`. [`ChestLootLoader`](https://github.com/GTNewHorizons/TooMuchLoot/blob/4.3.0-GTNH/src/main/java/dmillerw/tml/data/chest/ChestLootLoader.java) applies them, and the hook is [`TooMuchLoot.java#L71-L104`](https://github.com/GTNewHorizons/TooMuchLoot/blob/4.3.0-GTNH/src/main/java/dmillerw/tml/TooMuchLoot.java#L71-L104). An `OVERRIDE` replaces the vanilla category, so any `ChestGenHooks.addItem(DUNGEON_CHEST, …)` made earlier by GregGuns is lost. The loader also writes every registered category, ours included, to `loot/generated/`.
- **GregGuns does:**
  - registers its own categories with `ChestGenHooks` in `postInit` (names and contents: [pack-loot.md](../06-items/pack-loot.md));
  - fills every chest it places, vanilla `chest` blocks included, with `WeightedRandomChestContent.generateChestContents` from that category and the structure's seeded `Random`;
  - never reads or writes `DUNGEON_CHEST`.
- **Pack-side change:** none for structures. Putting GregGuns items into vanilla dungeons, mineshafts and villages is a separate `ADD` XML, owned by 06-items.

## Requirements

- **GGS-11-19** No GregGuns structure fills a chest from a vanilla `ChestGenHooks` category.
- **GGS-11-20** Every chest a structure places is filled from a GregGuns category, using the structure's seeded `Random`.
- **GGS-11-21** Each GregGuns loot category declares a maximum tier. Every item in it is at or below that tier by the [0001](../../design/0001-tier-map.md) table.
- **GGS-11-25** A structure on Ross128b or Ross128ba rolls a ceiling from MV to UHV, seeded by its plan; every chest of it draws from the table of that ceiling (UV and UHV from the ZPM table), and no chest holds an item above it.
- **GGS-11-22** The default ceiling of every structure in the table above is the tier shown. No default category used by a surface structure holds an item above MV.
- **GGS-11-23** `lootTier` (0 to 2) changes only the count and weights of a draw. It never adds items above the ceiling.
- **GGS-11-24** The Alien Bug Nest places no chest.

## Tests

- GGS-11-19, 20: unit test with a stub world that records which category each placed chest was filled from.
- GGS-11-21, 22, 23: the tier self-check from 0001, run over every category: it fails if an item's declared tier is above the category ceiling. One test per structure draws 1000 chests and asserts the same.
- GGS-11-24: unit test of the nest placement.

- GGS-11-25: `RolledCeilingTest`, `ChestTagsTest`, `StructurePopulatorTest` (rolled categories), `LootCategoriesTest` (tier 4 to 7 tables); scenarios `worldgen.ross128b_structures` and `worldgen.ross128ba_structures` (chest category equals the table of the plan's rolled ceiling), `worldgen.loot_tier_ceiling` (1000 chests per table, including 4 to 7).

## Open questions

- Should ships have an HV ceiling by default? 0005 says surface structures are LV to MV, and the Techguns T3 table was written for a late-game ship. Left at MV; revisit with the balance pass (M9).
