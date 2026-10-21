# Contributing to GregGuns

GregGuns is a from-scratch rewrite of Techguns for **GT New Horizons (Minecraft 1.7.10)**. These rules apply to every change, whether a person or an AI agent makes it. They exist so that the mod feels like part of GTNH rather than an add-on bolted to it, and so that the code stays readable, tested and maintainable.

> [!IMPORTANT]
> These rules use **must** for hard rules and **should** for strong defaults. A PR that breaks a *must* is not merged. To depart from a *should*, explain why in the PR.

## Contents

1. [Workflow](#workflow)
2. [Originality: the Techguns boundary](#originality-the-techguns-boundary)
3. [Toolchain and build](#toolchain-and-build)
4. [Architecture](#architecture)
5. [GTNH integration](#gtnh-integration)
6. [Code standards](#code-standards)
7. [Comments and documentation](#comments-and-documentation)
8. [Testing](#testing)
9. [Assets and localization](#assets-and-localization)
10. [Commits and pull requests](#commits-and-pull-requests)
11. [Definition of done](#definition-of-done)

---

## Workflow

1. **Start from the roadmap.** Every change belongs to a milestone or decision gate in [docs/ROADMAP.md](docs/ROADMAP.md).
   - Don't start a milestone whose entry gate isn't met.
   - Don't build anything a decision gate blocks until its record in [docs/design/](docs/design/README.md) is `Accepted`.
2. **Read the spec sheet** for the thing you're building in [docs/techguns-spec/](docs/techguns-spec/README.md). Start from the area's `index.md`. [CONVENTIONS.md](docs/techguns-spec/CONVENTIONS.md) explains the sheet layout.
3. **Check the bug register.** Before implementing a feature, read its rows in [BUGS.md](docs/techguns-spec/BUGS.md). Inherited Techguns bugs **must not** be reproduced, and where it's practical, a test proves that.
4. **Record deviations.** When GregGuns deliberately behaves differently from the spec (a GT tier gate, the EU energy model, a replaced machine, a bug fix), record it in the relevant design record. For a small deviation, a `// Deviation:` comment that cites the spec section is enough (see [Comments](#comments-and-documentation)).
5. **Keep it small.** One feature or fix per branch and PR. Work on a branch; never commit directly to `master`.

## Originality: the Techguns boundary

Techguns is **All Rights Reserved**. This boundary is a **must**:

- **No Techguns code.** Don't copy it, translate it line by line, or closely paraphrase it. Implement from the spec's behavioral descriptions.
- **No Techguns assets.** No textures, models, sounds or structure files, and nothing traced or edited from them. Every asset is original (see [Assets](#assets-and-localization)).
- **Facts are fine.** Names, numbers, formulas and recipes as gameplay facts are allowed, and the spec records them for this purpose.
- **Keep the reference clone out of the repo.** If you consult the Techguns source to settle a question the spec doesn't answer, never add the clone to the repo. Write the answer back into the spec in your own words.

## Toolchain and build

| Item | Rule |
|---|---|
| JDK | 25, as in [`.java-version`](.java-version). Gradle provisions it. |
| Language level | Modern **syntax** only. `enableModernJavaSyntax = jabel` compiles to **Java 8 bytecode, running on the Java 8 API**. Allowed: `var`, switch expressions, text blocks, `instanceof` patterns. **Not allowed:** records, sealed types, and any API newer than Java 8 (`List.of`, `Optional.isEmpty`, `String.isBlank`, `Stream.toList`…). If unsure, check the Java 8 Javadoc. |
| Formatting | Spotless, using the GTNH shared Eclipse config. Run `./gradlew spotlessApply` before every commit; CI rejects unformatted code. |
| Static checks | Checkstyle: no wildcard imports. |
| Build files | **Never edit** `build.gradle.kts` or `settings.gradle.kts`, which come from the GTNH buildscript. Put dependencies in [`dependencies.gradle`](dependencies.gradle), repositories in [`repositories.gradle`](repositories.gradle), and mod settings in [`gradle.properties`](gradle.properties). |
| Dependencies | Use GTNH Maven coordinates with the `:dev` classifier, pinned to the versions in the current GTNH pack (see `reference/gtnh/<version>/MOD-INDEX.md` and [docs/gregguns-spec/INTEGRATION.md](docs/gregguns-spec/INTEGRATION.md)). Use `compileOnly` for optional integrations, and `devOnlyNonPublishable` for things only needed at runtime in development. Never shade a mod. |

Commands:

```sh
./gradlew spotlessApply build   # format, compile, test, package
./gradlew test                  # unit tests only
./gradlew runClient             # dev client (username from gradle.properties)
./gradlew runServer             # dev dedicated server (smoke test)
```

## Architecture

Root package: `com.gregguns`. Target layout:

| Package | Contents |
|---|---|
| `com.gregguns` | `@Mod` entry class, proxies, `Tags` (generated) |
| `com.gregguns.api` | Stable interfaces other mods may use, kept minimal and documented |
| `com.gregguns.core` | Module system, registration helpers, config, logging, the creative tab |
| `com.gregguns.network` | Channel, packet base classes, packet types |
| `com.gregguns.combat` | The damage system, damage types, armor penetration (pure logic where possible) |
| `com.gregguns.gun` | Gun items, firing and reload logic, ammo handling |
| `com.gregguns.projectile` | Projectile entities, ballistics, explosions |
| `com.gregguns.armor` | Armor, set bonuses, special slots, flight equipment |
| `com.gregguns.item` | Components, ammo and magazines, tools, miscellaneous items |
| `com.gregguns.block` | Blocks and item blocks |
| `com.gregguns.machine` | GT MetaTileEntities and their GUIs |
| `com.gregguns.entity` | NPCs, AI, spawning |
| `com.gregguns.world` | Structures and loot |
| `com.gregguns.recipe` | All recipe registration, one class per production chain |
| `com.gregguns.compat` | Optional integrations, each guarded (see below) |
| `com.gregguns.client` | **All** client-only code: renderers, models, HUD, FX, sounds, keybinds |

Rules:

- **Side safety (must).** Client-only classes live under `client`, and common code never references them, even through imports in code paths that load on the server. Reach the client through the proxy or `@SideOnly` methods. Each feature is checked with `runServer` (see [Testing](#testing)).
- **Modules.** Each feature area registers through its own module class, controlled by a config flag (`modules.<name>`). A flag never changes what is registered: content always exists, so client/server flag differences and flipped flags cannot break joining or delete items. Disabling a module switches its behavior off (no recipes, spawning or worldgen; machines and items do nothing; hidden in NEI and the creative tab). Ask `ModuleState.isActive(id)`, never `ModuleFlags`, for behavior.
- **Logic and Minecraft glue are separate.** Formulas, state machines and tables (damage, drop-off, spread, reload timing, spawn weights, tier checks) live in plain Java classes with no Minecraft types in their signatures, so they can be unit-tested. Items, entities and tile entities stay thin and call into them.
- **No mutable statics** except registries and config. Per-player and per-entity state lives in NBT or extended properties, never in static maps.
- **Registration order** follows the FML lifecycle:
  - `preInit`: config, items and blocks, entities, network;
  - `init`: recipes, renderers on the client, event handlers;
  - `postInit`: cross-mod recipes and the dev self-check.

## GTNH integration

GregGuns must feel native to GTNH. These are the coupling rules.

> [!NOTE]
> GT5-Unofficial renamed its API classes in 2024 (for example `GT_Values` became `GTValues`, and `GT_OreDictUnificator` became `GTOreDictUnificator`). The names below follow the current scheme. **Always check them against the GT5-Unofficial version pinned in `dependencies.gradle`.**

**Researching external APIs (must)**
- These rules cover code that touches GT, IC2, NEI, other mods or the pack. The full workflow is in [docs/gregguns-spec/INTEGRATION.md](docs/gregguns-spec/INTEGRATION.md). The extracted pack and its mod index live in the git-ignored `reference/gtnh/`.
- **Source first.** Read the mod's source on GitHub (`gh search code`, `gh api …?ref=<tag>`) at the tag the pack ships.
- **Decompile only as a fallback**, for mods or internals with no available source. Never paste decompiled code into GregGuns.
- **Cite it.**
  - Put a `// Source:` permalink, pinned to the tag (or the commit, for branches), where code relies on another mod's behavior.
  - Put a `// Decompiled:` hedge comment wherever decompiled bytecode or decompiled source was the evidence.

**Materials and parts (must)**
- Never register an ingot, plate, wire, rod, screw, circuit, ore or fluid that GT already provides.
- Use GT materials and parts through ore-dict unification, for example `GTOreDictUnificator.get(OrePrefixes.plate, Materials.Steel, 1)`.
- GregGuns-only components (gun barrels, receivers, magazines) are GregGuns items, but they are *made from* GT parts.

**Recipes (must)**
- Machine processing uses GT recipe maps through the recipe builder (`GTValues.RA.stdBuilder()…addTo(RecipeMaps.…)`), with an EU/t from `TierEU` that matches the item's tier in the D1 record.
- Crafting-table recipes follow GTNH conventions: GT tools in the grid, GT parts as ingredients.
- No recipe may let an item be obtained before its tier.

**Tier gating (must)**
- Every item has exactly one tier in the D1 tier map.
- The gate is a machine voltage, a tier-locked material, or both.
- The dev self-check fails if a recipe uses a higher-tier input or machine than its output's tier allows.

**Energy (must)**
- Use EU only. No RF, and no new energy APIs.
- Electric items implement the IC2 electric-item API as GT items do, so GT chargers and battery buffers work with them.
- Fuel-based items take GT fluids.

**Machines**
- Kept machines are GT MetaTileEntities (single blocks or GT multiblocks). They are not custom tile entities with their own energy code.
- MetaTileEntity **IDs must be reserved** with the GTNH team before use, and recorded in `docs/design/`.

**GUIs**
- New GUIs use **ModularUI2**, the GTNH standard. GT MetaTileEntities use GT's own GUI framework.

**Config**
- Use GTNHLib's annotation-based config. Every option has a comment, a default and a range where relevant, and is listed in the module's docs.

**NEI**
- GT recipe maps appear in NEI automatically. Add handlers only for non-GT logic.
- Hide unobtainable and technical items.

**Mixins**
- Only when no event or API exists. Use UniMixins in the GTNH style, through the mixin configuration in `gradle.properties` (`usesMixins`).
- No ASM coremods.
- Each mixin has a comment saying why no event works.

**Optional mods**
- Guard with `Loader.isModLoaded` or `@Optional`, and keep the code under `compat`.
- Hard dependencies are declared in `@Mod(dependencies=…)` and `mcmod.info`.

**World**
- No ore generation; the GTNH ore system owns ores.
- Structures and spawning are config-gated, respect dimension rules, and never generate across chunks (no cascading worldgen).

**Pack fit**
- Tooltips, names, sounds and textures follow GTNH style (see D6).
- Quest-book content belongs in the GTNH modpack repository, not here.
- Before adding a mechanic, check the pack doesn't already provide it.

## Code standards

- **Readable first.** Use clear names: no abbreviations beyond common Minecraft ones (`te`, `nbt`, `pos`). Keep methods small, prefer early returns, and use one class per concept.
- **No magic numbers.** Use named constants, or config for anything a pack maintainer might tune. Spec numbers become constants named after what they mean.
- **Validate everything the client sends (must).**
  - Packet handlers check that the player can act: the same dimension, within reach, the block or entity exists and is the expected type, and the player has permission.
  - They check every index and enum against its bounds.
  - Techguns' unvalidated packets (CORE-06, CORE-08, CORE-40) are the counter-example.
- **Server authority.** The server decides hits, ammo, damage and inventory changes. The client predicts only for responsiveness (animations, sounds) and never mutates authoritative state.
- **No swallowed exceptions.** Never `catch (Exception e) {}`. Catch the narrowest type, then handle it or log it with `GregGuns.LOG`. Never use `System.out`.
- **Null safety.** Check stacks, NBT and entity lookups. Use a helper for "stack or empty" rather than repeating checks.
- **NBT.** Keys are constants. Readers tolerate missing keys and old saves with sensible defaults (see CORE-28).
- **Performance.** No per-tick allocation in hot paths, no world scans in tick handlers, and no chunk loading from entity or tile code (WGEN-27). Profile anything that ticks per player or per projectile.
- **Thread and side discipline.** Don't touch world state from network threads, and keep client and server effects separate (CORE-05 and PFX-04 are the counter-examples).

## Comments and documentation

- **Javadoc (must)** on every public class and every public method that isn't trivial. Say what it does, what each parameter means, units (ticks, blocks, EU), and side (client, server or both).
- **Comments explain why**, not what. Explain constraints, GT conventions and non-obvious math.
- **Spec traceability.** A class that implements a spec sheet cites it at the top: `// Spec: docs/techguns-spec/02-guns/gun-lifecycle.md`.
- **Deviations.** When behavior intentionally differs from the spec, say so where it happens: `// Deviation: energy is EU (D3), Techguns used RF.` or `// Avoids GUN-34: penetration applies to all mobs.`
- **External behavior.** Where code depends on how another mod behaves, cite it.
  - Use `// Source: <pinned GitHub permalink>` when the source exists.
  - Use `// Decompiled: <jar, pack version, member>; inferred from decompiled bytecode and may be wrong` when it doesn't.
  - See [docs/gregguns-spec/INTEGRATION.md § Comment conventions](docs/gregguns-spec/INTEGRATION.md#comment-conventions).
- **No commented-out code** in commits. Delete it; git keeps history.
- **Docs.** When you change behavior that a doc describes, update the doc in the same PR. Design decisions go in [docs/design/](docs/design/README.md).

## Testing

Each layer catches different bugs, and features need all the layers that apply to them.

### 1. Unit tests: must

- **Framework:** JUnit 5 in `src/test/java`, mirroring the main package structure; run with `./gradlew test`.
- **What they cover:** every formula, state machine and table in the logic classes. That includes damage and penetration, drop-off, spread, fire rate, reload timing, ammo accounting, set-bonus sums, recipe and tier tables, spawn weighting and NBT round-trips.
- **Bug regressions:** each inherited bug a feature could reproduce gets a regression test named for it, for example `doesNotReproduceGUN13_forcedReloadKeepsLoadedRounds`.
- **Speed:** tests are deterministic (seeded randomness, no wall-clock time) and fast.

### 2. Integration tests: must, for anything that touches the world

- **Server boot smoke test:** `./gradlew runServer` must start with GT5-Unofficial and the other pack dependencies loaded, with no GregGuns errors or warnings. CI runs this on every push.
- **Dev self-check:** in the dev environment, a `postInit` check that:
  - every GregGuns recipe resolves;
  - no recipe breaks the tier map;
  - every item has a lang key, a texture and a creative-tab entry or an explicit hidden flag;
  - every sound event exists.
  A failure stops the dev server.
- **In-game scenarios:** `/gregguns test <scenario>` (op only, dev only) sets up a reproducible situation and checks the result. For example, it can fire N rounds at a dummy and compare the damage dealt, or form a multiblock and run one recipe. Each feature adds a scenario, listed in `docs/testing/`.

### 3. Multiplayer and visual checks: must, for synced or rendered features

- **Multiplayer:** a dedicated server with two clients. Check that other players see the animations, sounds and effects, that inventories and NBT stay in sync, and that nothing desyncs after a relog.
- **Visual:** `runClient` checks of first person, third person, inventory and dropped-item rendering. Attach screenshots to the PR.

## Assets and localization

- **Original assets only (must).** Every texture, model and sound is original work, with its provenance recorded in `docs/assets/CREDITS.md` (source, author, license).
- **MIT for everything (must).** The whole repository (code, assets, docs and tools) is MIT-licensed; see [README § License](README.md#license). By contributing, you release your contribution under the MIT License. Never add anything that can't be released under MIT, such as All Rights Reserved, CC-BY-NC or share-alike material.
- **Spec as the brief.** [ASSET-FACTS.md](docs/techguns-spec/ASSET-FACTS.md) and each area's `textures.md` give the *footprint* an asset must fill: size, layout and frame count. Match the function, not the look.
- **Style.** Follow D6. Item icons are 16×16 unless the spec's footprint needs more, and they should sit naturally next to GT items.
- **Sounds.** Register every sound in `sounds.json` with a category. Keep loudness in line with D6, and use mono for positional sounds (see GVA-23).
- **Lang keys (must) for every player-visible string:** names, tooltips, chat, GUI text and death messages.
  - `en_US.lang` is the source of truth.
  - No hard-coded English (NPC-19, MUTL-32).
  - Use the key namespace `item.gregguns.<name>`, `tile.gregguns.<name>`, `gregguns.<area>.<key>`.

## Commits and pull requests

- **Commits.** Use an imperative subject (`Add pistol reload state machine`), a body explaining why, and one logical change per commit. Agents add their `Co-Authored-By` line.
- **PRs.**
  - Fill in the [PR template](.github/pull_request_template.md) and link the roadmap item and spec sheets.
  - List deviations and the inherited bugs avoided.
  - Attach test output and, for visual work, screenshots.
- **Review.** At least one review. During the MVP push, design records are accepted by default and revisited later.

## Definition of done

A change is done when all of these hold:

- [ ] It implements its spec sheet, with any deviations recorded.
- [ ] `./gradlew spotlessApply build` passes, and unit tests cover the new logic and the relevant BUGS.md regressions.
- [ ] `runServer` boots cleanly, and the dev self-check passes.
- [ ] An in-game scenario exists for world-touching features, and multiplayer was checked for synced features.
- [ ] Recipes are tier-gated per D1 and visible in NEI. No GT material is duplicated.
- [ ] Every string is a lang key, and assets are original with their provenance recorded.
- [ ] Javadoc and spec-traceability comments are in place. There is no commented-out code.
- [ ] The roadmap box is ticked and docs are updated in the same PR.
