# GregGuns: agent guide

A from-scratch rewrite of Techguns for **GT New Horizons (Minecraft 1.7.10, Forge)**, rebuilt on GregTech progression. This file is a map. Follow its links to the detail you need, and don't load everything up front.

## Non-negotiables

1. **Never copy Techguns code or assets.** It is All Rights Reserved. Build from the spec. See [CONTRIBUTING § Originality](CONTRIBUTING.md#originality-the-techguns-boundary).
2. **GT first.** Use GT materials, recipe maps, EU and tiers. Never duplicate what GT provides. See [CONTRIBUTING § GTNH integration](CONTRIBUTING.md#gtnh-integration).
3. **Java 8 API.** Modern syntax is compiled down by Jabel, but the runtime is Java 8: no records, no `List.of`. See [CONTRIBUTING § Toolchain](CONTRIBUTING.md#toolchain-and-build).
4. **Tests and comments ship with the code.** See [CONTRIBUTING § Testing](CONTRIBUTING.md#testing) and [§ Definition of done](CONTRIBUTING.md#definition-of-done).
5. **Work only on what the roadmap allows.** Gates exist for a reason. See [docs/ROADMAP.md](docs/ROADMAP.md).

## Where to look

| You need… | Read |
|---|---|
| What to build next, and its entry and exit gates | [docs/ROADMAP.md](docs/ROADMAP.md) |
| Accepted design decisions (tiers, machines, energy, damage) | [docs/design/](docs/design/README.md) |
| Coding, testing, GTNH and asset rules | [CONTRIBUTING.md](CONTRIBUTING.md) |
| How a Techguns feature behaves | [docs/techguns-spec/README.md](docs/techguns-spec/README.md), then the area's `index.md` |
| How spec sheets are laid out | [docs/techguns-spec/CONVENTIONS.md](docs/techguns-spec/CONVENTIONS.md) |
| Techguns bugs not to reproduce | [docs/techguns-spec/BUGS.md](docs/techguns-spec/BUGS.md) |
| Asset footprints (sizes, frames, colours, sound lengths) | [docs/techguns-spec/ASSET-FACTS.md](docs/techguns-spec/ASSET-FACTS.md); how each sound sounds: [AUDIO-PROFILES.md](docs/techguns-spec/AUDIO-PROFILES.md) |
| How GregGuns differs from Techguns and plugs into GTNH, per area | [docs/gregguns-spec/README.md](docs/gregguns-spec/README.md), then the area's `index.md` |
| Config and data changes the GTNH modpack must make for us | [docs/gregguns-spec/PACK-CHANGES.md](docs/gregguns-spec/PACK-CHANGES.md) |
| How a GT, IC2, NEI or other pack mod's API behaves (integration work) | [docs/gregguns-spec/INTEGRATION.md](docs/gregguns-spec/INTEGRATION.md), then the extracted pack in `reference/gtnh/` |
| Build settings, mod ID, package | [gradle.properties](gradle.properties) |
| Dependencies and repositories | [dependencies.gradle](dependencies.gradle), [repositories.gradle](repositories.gradle) |
| CI | [.github/workflows/](.github/workflows/) |

## Spec areas by topic

| Topic | Spec area |
|---|---|
| Lifecycle, config, packets, keybinds, HUD, events | [01-core](docs/techguns-spec/01-core/index.md) |
| Gun stats, firing and reload, damage system, gun recipes | [02-guns](docs/techguns-spec/02-guns/index.md) |
| Gun models, renderers, animations, gun textures and sounds | [03-gun-visuals-audio](docs/techguns-spec/03-gun-visuals-audio/index.md) |
| Projectiles, explosions, particles and FX, death effects | [04-projectiles-fx](docs/techguns-spec/04-projectiles-fx/index.md) |
| Armour, set bonuses, special slots, flight gear | [05-armor](docs/techguns-spec/05-armor/index.md) |
| Items, ammo, components, tools, fluids, loot | [06-items](docs/techguns-spec/06-items/index.md) |
| Blocks, connected textures, block models | [07-blocks](docs/techguns-spec/07-blocks/index.md) |
| Machine framework, production machines, GUIs, NEI | [08-machines-production](docs/techguns-spec/08-machines-production/index.md) |
| Repair and camo benches, turret, ore drill, charges | [09-machines-utility](docs/techguns-spec/09-machines-utility/index.md) |
| Mobs, AI, factions, spawning | [10-entities-npcs](docs/techguns-spec/10-entities-npcs/index.md) |
| Structures, ore clusters, worldgen loot | [11-worldgen](docs/techguns-spec/11-worldgen/index.md) |

Every area also has a GregGuns counterpart, `docs/gregguns-spec/<area>/index.md`, which says what changes and how it integrates with the pack. Read both.

## The GTNH reference pack

The extracted **GTNH 2.9.0-RC-2** pack is at `reference/gtnh/2.9.0-RC-2/`. It is git-ignored: never commit anything under `reference/`. Dig through it whenever you integrate with GT or any other mod in the pack.

- `reference/gtnh/2.9.0-RC-2/MOD-INDEX.md` lists all 242 mods, with each mod's version, jar, mod IDs, licence and **where its source is** (the GitHub repo and the tag matching the pack).
- `.minecraft/mods/` holds the jars, and `.minecraft/config/` shows how the pack configures each mod.
- [tools/gtnh-reference/](tools/gtnh-reference/README.md) has the helper: `python -I tools/gtnh-reference/gtnh_ref.py mod|where|clone|jars|decompile …`. If `reference/gtnh/` is missing, its `fetch` command recreates it.
- Grep and Glob skip ignored folders, so point them at `reference/` explicitly.

Rules (the detail is in [docs/gregguns-spec/INTEGRATION.md](docs/gregguns-spec/INTEGRATION.md)):

1. These rules apply only to external integrations, meaning code that touches GTNH or other mods. For Techguns behaviour, use the spec.
2. **Source first.** Read the mod's source on GitHub with `gh`, at the tag the pack ships.
3. **Decompile only as a fallback**, for mods with no source. Never paste decompiled code.
4. **Comment what you relied on.** Add a `// Source:` permalink pinned to the tag, and a `// Decompiled:` hedge wherever decompilation was the evidence.

## Commands

```sh
./gradlew spotlessApply build   # format, compile, test: run before every commit
./gradlew test                  # unit tests
./gradlew runServer             # boot smoke test (side safety, GT loaded)
./gradlew runClient             # visual checks
```

## Housekeeping

- Never edit `build.gradle.kts` or `settings.gradle.kts`; they are GTNH-managed.
- Branch per change, never commit to `master`, and use the [PR template](.github/pull_request_template.md).
- When behaviour changes, update the spec deviation notes or the design records in the same PR.
