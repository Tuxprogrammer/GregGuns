# Adding a gun

A checklist of every file and table a new ballistic gun touches, written from the 9mm Pistol (M1) and the Revolver (M2). Do the steps in order. Both guns are worked examples: grep for `revolver` (or `REVOLVER`) to find each place.

Read first: the gun's sheet in [techguns-spec/02-guns](../techguns-spec/02-guns/index.md) (`sheets-*.md`, `per-gun-behavior.md`, `gun-lifecycle.md`), its visual sheet in [03-gun-visuals-audio/gun-sheets.md](../techguns-spec/03-gun-visuals-audio/gun-sheets.md), and its row in [gregguns-spec/02-guns/recipes.md](../gregguns-spec/02-guns/recipes.md). Never copy Techguns code or box lists (CLAUDE.md, rule 1).

## What the framework already does

You do not write firing, reloading, zoom, spread, sync, sound or animation code. `ItemGun` is driven entirely by `GunStats`; `ServerGunHandler` runs it; the client mirror, HUD, prediction and animation read it. A new gun is **data**: stats, a model profile, sound rows, a recipe row, lang and tests.

| Extension | What it gives a gun | Where |
|---|---|---|
| `ProjectileKind` | Which projectile entity it fires. `BULLET` (default, `GenericProjectile`) or `GRAVITY_BULLET` (`SimpleProjectile`, the Hand Cannon). Other kinds (`ROCKET`, `GRENADE`, `FLAME`, `LASER`, ...) are reserved. A kind with no factory drops the shot and logs one error. | `GunStats.Builder.projectileKind(...)`; factories in `BulletLauncher.register(kind, ctor)` |
| Pellets | Extra projectiles per shot, each with its own spread offset from `SpreadModel`, optionally staggered along the path. | `GunStats.Builder.pellets(extra, pelletSpread, inLine)` |
| Scope | FOV factor and spread multiplier while zoomed, and an overlay texture. | `GunStats.Builder.zoom(fovFactor, spreadMultiplier)`; `GunTransforms.Builder.scope(new ScopeSpec(texture, kickUp, kickSide))` |
| Knockback | A per-hit push multiplier that really changes strength (GUN-38). | `GunStats.Builder.knockback(m)` (1 vanilla, 0 none) |
| Muzzle flash frames | How many painted cells of the FX sheet play. | `new SpriteSheet(4, 4, frames)` in the profile; `MuzzleSheetsTest` checks it against the PNG |

## Checklist

### 1. Stats: `gun/logic/GunCatalog.java`

Add a `public static final GunStats NAME = GunStats.builder("id")...build();` with a Javadoc that cites the sheet (`// Spec:`) and lists the numbers. Take every value from section 3 of the gun's sheet: damage and drop-off (`damage`, `damageDrop(start, end, min)`), `spread`, `fire(mode, rateTicks)` or `burst`, `magazine`, `ammoType`, `looseAmmoType`, `emptyContainerType`, `reloadMode`, `reloadTicks`, `projectile(speed, ttl)`, `penetration`, `animation(recoil, flash)`, `ai(...)`. Note the Techguns "later builder call wins" trap (GUN-16): define each value once, with the winning one. Add ammo ids to `GunCatalog.AmmoIds` if needed; `AmmoIdsMatchItemsTest` ties them to the ammo items.

Ammo that fills the gun straight from a loose round (Revolver: one 9mm bullet = 6 rounds) uses `ammoType = loose item`, `reloadMode(MAGAZINE, 1)` and no empty container. `GunItems.roundsPerLooseItem` already treats that as `magazineSize` rounds per item, so a forced reload never turns one bullet into several rounds.

Test: add a `...MatchesItsSheet` test to `GunCatalogTest` (every number of section 3).

### 2. The item: `item/gun/GunItems.java`

Add a field, an accessor and a `registrar.registerItem(GunSoundEvents.NAME, new ItemGun(GunSoundEvents.NAME, GunCatalog.NAME, Tier.LV, DamageType.PROJECTILE))` line in `register`. The registry name is the content name (`revolver`, so `gregguns:revolver`); the same string is the texture name, so `textures/items/<name>.png` must exist. Pick the tier from [design 0001](../design/0001-tier-map.md).

### 3. Lang: `assets/gregguns/lang/en_US.lang`

`item.gregguns.<name>.name=...` (the display name from the Techguns lang sheet, minus its typos, GUN-46). Every part item you add needs one too. The dev self-check fails the boot on a missing key, texture or creative-tab entry.

### 4. Sounds: `gun/sound/GunSoundEvents.java` and `assets/gregguns/sounds.json`

- Constants: `public static final String NAME = "name";`, `NAME_FIRE = gun(NAME, FIRE)`, `NAME_RELOAD = gun(NAME, RELOAD)`. `ItemGun` derives its two events from the content name, so the constants are for tests and scenarios.
- `sounds.json`: rows `guns.<name>.fire` (the `_a`, `_b`, `_c` shot files) and `guns.<name>.reload`, category `player`, paths relative to `sounds/` without `.ogg`. The files exist already in `sounds/guns/`; **never edit or add `.ogg` files** (the asset agent owns them). `GunSoundsJsonTest` checks names, mono files and existence.

### 5. Render profile and model: `client/render/profile/<Name>Profile.java`

Copy `RevolverProfile` (X-family) or `Pistol9mmProfile` (Z-family). It holds:

- **Boxes.** Original geometry only. `tools/asset-gen/model_layouts.md` lists, per gun, each box as `u v w h d` in texels for the painted model sheet `textures/models/guns/<name>.png`. Use those sizes and texture origins exactly and invent only the positions, from the words of the Techguns `gun-models.md` (never its box lists). A box is `new BoxSpec(name, part, u, v, w, h, d, x, y, z, tiltPivot, tiltZ)`; the model is authored muzzle toward +X, Y down. Parts are groups that the transform rows move separately (the pistol's slide).
- **Family.** The sheet says X or Z family in `rendering-framework.md` section 6.1. Z-family: `new BoxGunModel(..., muzzle)`, the model turns a quarter turn about Y. X-family: pass `true` as the last argument, so boxes draw as authored with the barrel along +X.
- **Transforms.** `GunTransforms.builder()` with the sheet's rows: `timing(recoil, flash, reload)`, `firstPerson(base, idle, fire, reload)`, `thirdPerson(base, aimTranslate, recoil, reload)`, `world(ground, icon)`, `muzzleFlash(spec)`, and `scope(...)` for scoped guns. Rows are `TransformProgram.builder().rotX(..).translate(..)` in GL call order; curves (`yF`, `yR`, `p66`, `kick`) come from `GunCurves`.
- **Muzzle flash.** `MuzzleFlashSpec(texture, new SpriteSheet(4, 4, frames), x, y, z, scale, rot, additive, thirdPersonLength)`. The frame count is the number of **painted** cells: `muzzleflash2.png` has 7 (cell 7 is empty), `muzzleflash4x4.png` has 9, not the manifest's 10. Add a case to `MuzzleSheetsTest`. `MUZZLE` is the third-person flash origin in the gun frame, blocks.
- **Scope** (scoped guns only): `ScopeSpec("gregguns:textures/fx/testscope.png", kickUp, kickSide)`. The FOV factor is `GunStats.zoom`. The overlay PNGs are 3072 and 2048 pixels square; `ScopeTextures` shrinks them to at most 1024 when it loads them (`ImageScaler`), and never changes the files. With no `ScopeSpec` a zoom changes the FOV only.

Register it in `client/render/GunRenderers.java` (`registerName(Item)`, a copy of `registerRevolver`) and call it from `client/gun/GunClientSetup.init()`.

Tests (copy `RevolverProfileTest`): boxes equal the layout sheet; unwraps fit the sheet and do not overlap; every face lands on painted pixels of the PNG; timing and rows follow the sheet.

### 6. Recipe: `recipe/GunTable.java` (+ `ProductionTable.java` for new parts)

Add a `ProductionTable.Row` to `GunTable` and to `GunTable.ROWS`: id (`GR-nn`), output name, count, `Tier`, `Target.ASSEMBLER` (or the right GT map for the tier, see [design 0001](../design/0001-tier-map.md) and the gun's table in `gregguns-spec/02-guns/recipes.md`), seconds, programmed circuit (**unique per gun**, `GunTableTest` checks), then inputs as `Input.mod("part_...", n)` and `Input.gt("plate", "Steel", 2)`. At least one input must be the tier's gate material (`GateRule`; the dev self-check enforces it over every registered recipe). `GunRecipes` registers the rows and the GT recycling data; add the recycling sums to `GunComposition` and `GunRecipes.addRecyclingData`.

A part the gun needs that does not exist yet: register it in `item/part/PartItems.java` (name, tier), add its lang key, and add its recipe: a machine row in `ProductionTable.ROWS` (spec rows `RC-nn` in `gregguns-spec/06-items/recipes.md`) or, for hand-made ULV parts, a crafting-table recipe in `recipe/PartCraftingRecipes.java` and its name in `ProductionTable.CRAFTED_PARTS`. Every GT input (`Input.gt`) must resolve through the unificator: a missing one skips the recipe with an error that names it, and the smoke test fails on it (the Iron Barrel's `pipeSmall.Iron` taught us this: GT has no such part).

Tests: a `...RowMatchesTheSheet` test in `GunTableTest`; `GunRecipeScenarios` registers an in-game `gun.<name>.recipe` for the assembler map (add one `GunAssemblerRecipe` line).

### 7. Ammo

Most guns reuse existing ammo (`AmmoTypes`, `AmmoItems`, `ProductionTable` rows RC-28 and up). A new caliber or magazine is one row in `AmmoTypes`, its items in `AmmoItems`, textures (already in `textures/items/`), lang keys, press recipes, and an `AmmoIdsMatchItemsTest` case. See `gregguns-spec/06-items/ammo.md` and `recipes.md`.

### 8. Scenarios and tests

- Build the gun's in-game scenarios on `GunRig` (`gun/server/GunRig.java`): a fake player holding the gun in a cleared corridor, with `press`, `release`, `tick`, `give`, `count`, `loaded`, `bullets`. Copy `RevolverScenarios`: `gun.<name>.fire` (damage per `DamageModel`, rounds, cooldown), `gun.<name>.reload` (timing, ammo consumed, containers returned), `gun.<name>.reload.forced` if ammo is loose, `gun.<name>.damage` for drop-off. Register them from the gun's own `registerAll` and add one line to `GunsModule` if it is a new class.
- Behavior unique to the gun gets its own scenario or unit test (pellet counts, burst timing, scope FOV). Framework checks already exist: `gun.kind`, `gun.pellets`, `gun.knockback`.
- A visual walkthrough is one line: `registry.register(new GunVisualScenario("mp.visual.<name>", "<prefix>", GunItems.<name>(), "<ammo item>"))`.
- Bug regressions are named `doesNotReproduce<ID>_...` (CONTRIBUTING, Testing).
- Add a row per scenario to the scenario table in [../testing/README.md](../testing/README.md).

### 9. Pack changes: `docs/gregguns-spec/PACK-CHANGES.md`

Add the registry name `gregguns:<name>` to the two Backhand rows: `general.mainhandUseStopsOffhandFallback` (every gun with a right-click use: scoped guns, charge guns; precautionary for the rest) and `general.offhandBlacklist` (every gun). Anything else the pack must configure for the gun goes in the same file.

### 10. Docs

- Deviations from the sheet go in the gun's code Javadoc (`// Deviation:`) and in the area's spec under `docs/gregguns-spec/02-guns/`.
- Tick the gun in the milestone's batch in [ROADMAP](../ROADMAP.md) when the batch's exit gate is met.

## Verification

```sh
./gradlew spotlessApply build                      # format, compile, unit tests
GG_MODULES=guns python -I run_scenarios.py <worktree> <port>   # in-game scenarios, headless
GG_MODULES=guns python -I smoke_server.py <worktree> <port>    # boot + dev self-check
```

Then a visual check of the first-person and third-person model with the dev autotest (`mp.visual.<name>`, see [../testing/README.md](../testing/README.md), "Multiplayer and visual autotest"). Read the screenshots: a model that loads but floats, faces the wrong way or has a flash on the wrong axis is only visible there.

## Gotchas

- `@SideOnly(CLIENT)` vanilla methods (`Item.getCreativeTab`, `getIconString`) must never be called from common code. Put client work under `com.gregguns.client`.
- Java 8 API only (no records, no `List.of`).
- Shared files (`GunItems`, `GunCatalog`, `GunTable`, `GunRenderers`, `GunClientSetup`, `PartItems`, `sounds.json`, `en_US.lang`, `GunsModule`) are touched by every gun: add lines, do not reorder.
- Do not edit textures or `.ogg` files; ask the asset agent for a missing one.
