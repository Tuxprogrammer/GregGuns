# Adding a gun

How to add a ballistic gun. Since the gun-definitions restructure, **a gun is one definition class plus one line in each of two lists**; everything else (the item, its recipe, its scenarios, the tests) follows from the definition. Worked examples: `PistolDef` (magazine, machine recipe), `RevolverDef` (loose-round ammo), `SawedOffDef` (pellets), `HandCannonDef` (hand-crafted, gravity projectile), `BoltActionDef` (scope, rechamber, camos).

Read first: the gun's sheet in [techguns-spec/02-guns](../techguns-spec/02-guns/index.md) (`sheets-*.md`, `per-gun-behavior.md`, `gun-lifecycle.md`), its visual sheet in [03-gun-visuals-audio/gun-sheets.md](../techguns-spec/03-gun-visuals-audio/gun-sheets.md), and its row in [gregguns-spec/02-guns/recipes.md](../gregguns-spec/02-guns/recipes.md). Never copy Techguns code or box lists (CLAUDE.md, rule 1).

## What a new gun touches

| What | File | Kind |
|---|---|---|
| The definition: stats, tier, damage type, camo count, recipe row, recipe and recycling hooks, scenarios | `gun/def/<Name>Def.java` | new file |
| The registry line | `gun/def/GunDefinitions.java`, `ALL` | one line |
| The client definition: model, textures, transform rows (the render profile) | `client/render/profile/<Name>Profile.java` | new file |
| The client registry line | `client/render/profile/ClientGunDefinitions.java`, `ALL` | one line |
| Recycling data and any crafting-table recipe (GT-facing code) | `recipe/<Name>Recipes.java` | new file, only if the gun needs it |
| Scenarios | `gun/server/<Name>Scenarios.java` | new file (copy `RevolverScenarios`) |
| A new caliber | `item/ammo/def/<Name>Ammo.java` plus one line in `AmmoDefinitions.ALL` | new file and one line |
| A new part | one `PartDefinition` entry in `item/part/PartDefinitions.java` | one entry |
| Append-only resources | `en_US.lang`, `sounds.json`, `docs/testing/README.md` scenario rows, the two Backhand rows of `PACK-CHANGES.md` | appended lines |

**Nothing else.** `GunItems`, `GunCatalog`, `GunSoundEvents`, `GunTable`, `GunRecipes`, `GunRenderers`, `GunClientSetup`, `GunsModule`, `AmmoIdsMatchItemsTest`, `GunTableTest`, `MuzzleSheetsTest` and `GunRecipeScenarios` are not edited for a new gun: they read the lists. (`GunCatalog.PISTOL` and friends survive only as aliases of the first five guns' stats.) The table-driven `GunDefinitionsTest` checks each definition against its ammo items, recipe row and gate rule, its sound events in `sounds.json`, its camo count against the texture files on disk, its camo lang names, and its client definition.

## What the framework already does

You do not write firing, reloading, zoom, spread, sync, sound or animation code. `ItemGun` is driven entirely by the `GunDefinition`; `ServerGunHandler` runs it; the client mirror, HUD, prediction and animation read it. A new gun is **data**.

| Extension | What it gives a gun | Where |
|---|---|---|
| `ProjectileKind` | Which projectile entity it fires. `BULLET` (default, `GenericProjectile`) or `GRAVITY_BULLET` (`SimpleProjectile`, the Hand Cannon). Other kinds (`ROCKET`, `GRENADE`, `FLAME`, `LASER`, ...) are reserved. A kind with no factory drops the shot and logs one error. | `GunStats.Builder.projectileKind(...)`; factories in `BulletLauncher.register(kind, ctor)` |
| Pellets | Extra projectiles per shot, each with its own spread offset from `SpreadModel`, optionally staggered along the path. | `GunStats.Builder.pellets(extra, pelletSpread, inLine)` |
| Scope | FOV factor and spread multiplier while zoomed, and an overlay texture. | `GunStats.Builder.zoom(fovFactor, spreadMultiplier)`; `GunTransforms.Builder.scope(new ScopeSpec(texture, kickUp, kickSide))` |
| Knockback | A per-hit push multiplier that really changes strength (GUN-38). | `GunStats.Builder.knockback(m)` (1 vanilla, 0 none) |
| Rechamber | A second sound with every shot (bolt, pump). | `GunDefinition.Builder.rechambers()`; add `guns.<name>.rechamber` to `sounds.json` |
| Camos | The Camo Bench cycles the gun's skins; the item is a `CamoChangeable` and the rounds stay. | `GunDefinition.Builder.camos(n)`; textures `<name>.png`, `<name>_camo1.png`, ...; lang `item.gregguns.<name>.camoname.<i>` |
| Armor accuracy | A worn set's `GUN_ACCURACY` bonus tightens the spread. The server reads it; it is 0 with `modules.armor` off. | `GunAccuracy` (nothing to do per gun) |
| Muzzle flash frames | How many painted cells of the FX sheet play. | `new SpriteSheet(4, 4, frames)` in the profile; `MuzzleSheetsTest` checks it against the PNG |

## Checklist

### 1. The definition: `gun/def/<Name>Def.java`

Copy the nearest existing `*Def`. It holds, in one place:

- `STATS`: `GunStats.builder("id")...build();` with a Javadoc that cites the sheet (`// Spec:`) and lists the numbers. Take every value from section 3 of the gun's sheet: damage and drop-off (`damage`, `damageDrop(start, end, min)`), `spread`, `fire(mode, rateTicks)` or `burst`, `magazine`, `ammoType`, `looseAmmoType`, `emptyContainerType`, `reloadMode`, `reloadTicks`, `projectile(speed, ttl)`, `penetration`, `animation(recoil, flash)`, `ai(...)`. Note the Techguns "later builder call wins" trap (GUN-16): define each value once, with the winning one. Name the ammo with the ammo definition's item names (`NineMmAmmo.TYPE.roundItemName()`), not a new constant.
- `DEFINITION`: `GunDefinition.of(NAME, STATS).tier(...).damageType(...)`, then as needed `.rechambers()`, `.camos(n)`, `.row(Row.assembler(...))`, `.recipes(() -> ...)`, `.recycling(() -> ...)`, `.scenarios(registry -> ...)`. `NAME` is the registry name (`gregguns:<name>`), the sound prefix and the texture name. Pick the tier from [design 0001](../design/0001-tier-map.md).

Ammo that fills the gun straight from a loose round (Revolver: one 9mm bullet = 6 rounds) uses `ammoType = loose item`, `reloadMode(MAGAZINE, 1)` and no empty container. `GunItems.roundsPerLooseItem` already treats that as `magazineSize` rounds per item, so a forced reload never turns one bullet into several rounds.

The hooks are lambdas (`() -> RevolverRecipes.recycling()`), not method references, so GT and Minecraft classes load only when the hook runs and the definition stays readable by plain unit tests.

Then add `<Name>Def.DEFINITION` to `GunDefinitions.ALL`. List order is registry order.

Test: the common checks need nothing. A test of the sheet's numbers goes next to the definition (see `GunCatalogTest` for the style).

### 2. Recipe row and hooks

The assembly row is `Row.assembler(id, output, count, tier, seconds, circuit, inputs...)` inside the definition: id (`GR-nn`), the gun's `NAME`, `Tier`, seconds, programmed circuit (**unique per gun**, `GunDefinitionsTest` checks), then inputs as `Input.mod("part_...", n)` and `Input.gt("plate", "Steel", 2)`. See [design 0001](../design/0001-tier-map.md) and the gun's table in `gregguns-spec/02-guns/recipes.md`. At least one input must be the tier's gate material (`GateRule`; the dev self-check enforces it over every registered recipe). `GunRecipes` registers the rows of every definition.

A gun that is only crafted by hand (the Hand Cannon) has no row and a `.recipes(() -> ...)` hook calling a public method of its `<Name>Recipes`. GT recycling data goes in `.recycling(() -> <Name>Recipes.recycling())`: copy `RevolverRecipes` and add the composition sums to `GunComposition` (pure and unit-tested). The data of the parts it uses belongs to the parts (`PartRecycling`).

A part the gun needs that does not exist yet: add a `PartDefinition` to `PartDefinitions.ALL` (name, tier, optional `.row(...)` machine recipe, `.handCrafted()` plus a `.recipes(...)` hook for ULV crafting-table parts, optional `.recycling(...)`) and its lang key. Every GT input (`Input.gt`) must resolve through the unificator: a missing one skips the recipe with an error that names it, and the smoke test fails on it (the Iron Barrel's `pipeSmall.Iron` taught us this: GT has no such part). Keep the registry names of parts that already exist: other branches reuse them.

### 3. Lang: `assets/gregguns/lang/en_US.lang`

`item.gregguns.<name>.name=...` (the display name from the Techguns lang sheet, minus its typos, GUN-46). Every part item you add needs one too. A gun with camos needs `item.gregguns.<name>.camoname.<i>` for every skin including 0. The dev self-check fails the boot on a missing key, texture or creative-tab entry.

### 4. Sounds: `assets/gregguns/sounds.json`

`ItemGun` derives its events from the name: `guns.<name>.fire`, `guns.<name>.reload` and, for a rechambering gun, `guns.<name>.rechamber`. You add **no constant** to `GunSoundEvents`; add the rows to `sounds.json` (the `_a`, `_b`, `_c` shot files), category `player`, paths relative to `sounds/` without `.ogg`. The files exist already in `sounds/guns/`; **never edit or add `.ogg` files** (the asset agent owns them). `GunSoundsJsonTest` checks names, mono files and existence, and `GunDefinitionsTest` checks that every definition has its events.

### 5. Render profile and model: `client/render/profile/<Name>Profile.java`

Copy `RevolverProfile` (X-family) or `Pistol9mmProfile` (Z-family). It holds the boxes, the transforms and a `DEFINITION` (a `ClientGunDefinition`: gun name, boxes, parts, texture size, scale, **textures, one per camo**, muzzle, family, transforms), which you list in `ClientGunDefinitions.ALL`. `GunClientSetup` registers a renderer for each entry.

- **Boxes.** Original geometry only. `tools/asset-gen/model_layouts.md` lists, per gun, each box as `u v w h d` in texels for the painted model sheet `textures/models/guns/<name>.png`. Use those sizes and texture origins exactly and invent only the positions, from the words of the Techguns `gun-models.md` (never its box lists). A box is `new BoxSpec(name, part, u, v, w, h, d, x, y, z, tiltPivot, tiltZ)`; the model is authored muzzle toward +X, Y down. Parts are groups that the transform rows move separately (the pistol's slide).
- **Family.** The sheet says X or Z family in `rendering-framework.md` section 6.1. Z-family: `xFamily = false` in the `ClientGunDefinition`, and the model turns a quarter turn about Y. X-family: `xFamily = true`, so boxes draw as authored with the barrel along +X.
- **In-hand size and position (the fit).** Never copy the sheet's `S(n)` and `T(...)`. Every M2 gun is placed from a per-gun target in `InHandTargets` (`client/render/profile`): first-person length, box centre and muzzle in camera coordinates, and third-person length, all taken from `docs/techguns-spec/03-gun-visuals-audio/model-notes/in-hand-transforms.md` section 7. `InHandFit.fit(name, boxes, MODEL_SCALE, alongX, fpRotY, tpRotY, tpAim)` solves the first-person scale and idle offset, the third-person scale and translate, and `Placement.flashOffset(MUZZLE, scale)` the first-person muzzle flash offset. Profiles use `FIT.fpScale`, `FIT.fpIdle`, `FIT.tpScale`, `FIT.tpTranslate`. To add a gun, add a row to `InHandTargets`.
  1. **Motion.** Keep the sheet's rotations. For fire and reload `translate(...)` rows use `IDLE + K x (the sheet's delta)` with `K = FirstPersonFit.translationFactor(sheetScale, FP_SCALE)`; since the fit scale is about the sheet's own, K is about 1. A shift written as `d / FP_SCALE` is a constant `d` item-frame blocks on screen.
  2. **Check.** `InHandFitTest` checks every table gun: first-person length, centre and muzzle (10 percent, centre 0.1 camera blocks), the butt reaching the camera plane, third-person length, and the box centre within 0.25 world blocks of the hand. `FirstPersonFit` (the old fixed-length class rule) remains for the energy guns that are not on the table yet.
- **Transforms.** `GunTransforms.builder()` with the sheet's rows: `timing(recoil, flash, reload)`, `firstPerson(base, idle, fire, reload)`, `thirdPerson(base, aimTranslate, recoil, reload)`, `world(ground, icon)`, `muzzleFlash(spec)`, and `scope(...)` for scoped guns. Rows are `TransformProgram.builder().rotX(..).translate(..)` in GL call order; curves (`yF`, `yR`, `p66`, `kick`) come from `GunCurves`.
- **Muzzle flash.** `MuzzleFlashSpec(texture, new SpriteSheet(4, 4, frames), x, y, z, scale, rot, additive, thirdPersonLength)`. The frame count is the number of **painted** cells: `muzzleflash2.png` has 7 (cell 7 is empty), `muzzleflash4x4.png` has 9, not the manifest's 10. Add a case to `MuzzleSheetsTest`. `MUZZLE` is the third-person flash origin in the gun frame, blocks.
- **Scope** (scoped guns only): `ScopeSpec("gregguns:textures/fx/testscope.png", kickUp, kickSide)`. The FOV factor is `GunStats.zoom`. The overlay PNGs are 3072 and 2048 pixels square; `ScopeTextures` shrinks them to at most 1024 when it loads them (`ImageScaler`), and never changes the files. With no `ScopeSpec` a zoom changes the FOV only.

Tests (copy `RevolverProfileTest`): boxes equal the layout sheet; unwraps fit the sheet and do not overlap; every face lands on painted pixels of the PNG; timing and rows follow the sheet. Add your gun's case to `MuzzleSheetsTest` (the flash frame count is per sheet).

### 6. Camo skins

`camos(n)` in the definition and the texture list in the profile must agree with the PNGs: `<name>.png` plus `<name>_camo1.png` and so on (`textures/models/guns/`). `GunDefinitionsTest` fails if the count differs from the files on disk or from the client definition's textures. The Camo Bench then cycles the gun with no further code; the camo index is stored in the stack NBT next to the rounds, and cycling never changes the rounds (GUN-01). `gun.camo.cycle` checks every gun.

### 7. Ammo

Most guns reuse existing ammo. A new caliber is one `item/ammo/def/<Name>Ammo.java` (`AmmoDefinition.of(TYPE)` with `.magazine(...)`, `.row(Row.press(...))`, `.refill(...)` and hooks) and one line in `AmmoDefinitions.ALL`; the items, the `AmmoTypes` index, the press rows (`ProductionTable.ROWS`), the refills and the recycling hooks follow. Textures (already in `textures/items/`) and lang keys are the usual per-item resources. See `gregguns-spec/06-items/ammo.md` and `recipes.md`. The Hand Cannon's stone bullet (`StoneAmmo`) is the worked example for a ULV round: a hand recipe (RC-27) in `HandCannonRecipes` and a `Tier.STEAM` press row RC-27b for the Steam Ammo Press (at most 4 inputs; `Input.ore("cobblestone", 3)` takes a plain ore-dictionary name).

### 8. Scenarios and tests

- Build the gun's in-game scenarios on `GunRig` (`gun/server/GunRig.java`): a fake player holding the gun in a cleared corridor, with `press`, `release`, `tick`, `give`, `count`, `loaded`, `bullets`. Copy `RevolverScenarios`: `gun.<name>.fire` (damage per `DamageModel`, rounds, cooldown), `gun.<name>.reload` (timing, ammo consumed, containers returned), `gun.<name>.reload.forced` if ammo is loose, `gun.<name>.damage` for drop-off. Its `registerAll` also registers the recipe scenario, `GunRecipeScenarios.assembly("gun.<name>.recipe", "...", <Name>Def.DEFINITION.assembly())`, and the visual one below. The definition's `.scenarios(registry -> <Name>Scenarios.registerAll(registry))` is the only hook needed: `GunsModule` registers every definition's scenarios in a loop.
- Behavior unique to the gun gets its own scenario or unit test (pellet counts, burst timing, scope FOV). Framework checks already exist: `gun.kind`, `gun.pellets`, `gun.knockback`, `gun.camo.cycle`, `gun.accuracy.armor`.
- A visual walkthrough is one line in the same `registerAll`: `registry.register(new GunVisualScenario("mp.visual.<name>", "<prefix>", GunItems.get("<name>"), "<ammo item>"))`.
- Bug regressions are named `doesNotReproduce<ID>_...` (CONTRIBUTING, Testing).
- Add a row per scenario to the scenario table in [../testing/README.md](../testing/README.md).

### 9. Pack changes: `docs/gregguns-spec/PACK-CHANGES.md`

Add the registry name `gregguns:<name>` to the two Backhand rows: `general.mainhandUseStopsOffhandFallback` (every gun with a right-click use: scoped guns, charge guns; precautionary for the rest) and `general.offhandBlacklist` (every gun). Anything else the pack must configure for the gun goes in the same file.

### 10. Docs

- Deviations from the sheet go in the gun's code Javadoc (`// Deviation:`) and in the area's spec under `docs/gregguns-spec/02-guns/`.
- Tick the gun in the milestone's batch in [ROADMAP](../ROADMAP.md) when the batch's exit gate is met.

## Verification

```sh
./gradlew spotlessApply build                      # format, compile, unit tests (incl. GunDefinitionsTest)
GG_MODULES=guns,armor,machines python -I run_scenarios.py <worktree> <port>   # in-game scenarios, headless
GG_MODULES=guns python -I smoke_server.py <worktree> <port>    # boot + dev self-check
```

Then a visual check of the first-person and third-person model with the dev autotest (`mp.visual.<name>`, see [../testing/README.md](../testing/README.md), "Multiplayer and visual autotest"). Read the screenshots: a model that loads but floats, faces the wrong way or has a flash on the wrong axis is only visible there.

## Gotchas

- `@SideOnly(CLIENT)` vanilla methods (`Item.getCreativeTab`, `getIconString`) must never be called from common code. Put client work under `com.gregguns.client`.
- Java 8 API only (no records, no `List.of`).
- Definitions must stay free of Minecraft and GT types at class-load time: build rows and stats directly, and reach item, recipe and scenario code only from inside lambda bodies.
- Do not use `ProductionTable` statics (`BULLET`, `ROWS`, ...) inside a definition: `ProductionTable` reads the definitions, so that would be a class-initialisation cycle. Use `Row.assembler` / `Row.press` and the definition's own names.
- The two lists (`GunDefinitions.ALL`, `ClientGunDefinitions.ALL`) and the append-only resources (`en_US.lang`, `sounds.json`, scenario table rows, Backhand rows) are touched by every gun: add lines, do not reorder.
- Do not edit textures or `.ogg` files; ask the asset agent for a missing one.
