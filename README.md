# GregGuns

![GregGuns](docs/assets/infographics/hero.png)

Guns, armor and military tech for [GT New Horizons](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack) on Minecraft 1.7.10.

GregGuns is a from-scratch rewrite inspired by [Techguns](https://github.com/pWn3d1337/Techguns). It is not a port. GTNH has changed 1.7.10 so much that the original code would not carry over cleanly. GregGuns is written against GTNH's own APIs instead, and its weapons, components and recipes are gated by GregTech tiers rather than vanilla-style crafting.

> [!IMPORTANT]
> **Beta.** [**GregGuns 0.3.2-beta**](https://github.com/Tuxprogrammer/GregGuns/releases/tag/0.3.2-beta) is out for **GTNH 2.9.0-RC-2**. Everything planned for the first full version is in: weapons Steam to ZPM, ammo lines, armor, turrets, NPCs and ruins, and now an optional questbook tab. It has been tested inside the real pack. Expect some rough edges, and back up your world before you add it. The [release notes](https://github.com/Tuxprogrammer/GregGuns/releases/tag/0.3.2-beta) list the known issues (one is with shader packs), and [docs/ROADMAP.md](docs/ROADMAP.md) shows what comes next.

## For players

GregGuns adds guns, ammo, armor, turrets, enemy soldiers and ruins to GTNH. It adds no tech tree of its own. Every weapon is assembled from GregTech parts on GregTech machines and unlocks at a GregTech tier: a Hand Cannon in the Steam age, a 9mm Pistol once you have Steel, assault rifles at MV, and so on up to the Nuclear Death Ray at ZPM. Ammo is a production line. You press rounds and magazines in the **Ammo Press**, or in bulk in the **Ammunition Works** multiblock.

### Install

1. Download `gregguns-0.3.2-beta.jar` from the [release page](https://github.com/Tuxprogrammer/GregGuns/releases/tag/0.3.2-beta).
2. Put it in the `mods` folder of your **GTNH 2.9.0-RC-2** instance. For multiplayer, the server **and** every player need the same jar.
3. Start the game. Every module is on by default. You can switch modules off in `config/gregguns.cfg`, and that never stops anyone joining: the server's settings apply to everyone connected.
4. **Coming from 0.2.0-alpha?** Read "Upgrading" in the release notes first. Your old config may still have modules switched off.

The keys are **R** to reload, **;** for the back device (glider, anti-gravity), **M** for night vision and **]** for the ammo pouch. All of them can be rebound in Controls.

### Getting started

1. **Install** as above.
2. **Steam age.** Craft the Hand Cannon and some Stone Bullets.
3. **LV.** Assemble a 9mm Pistol from GT parts, and build an LV Ammo Press.
4. **Ammo.** Press bullets and magazines, then reload with **R**.
5. **Upkeep.** Mend worn gear on a Repair Bench or in the Armory Gantry. A **GregGuns** questbook tab leads you up the tiers once the server owner switches it on (see below).

![Getting started](docs/assets/infographics/getting-started.png)

### Guides

Each guide is a single image you can share on its own.

#### The arsenal

Every weapon, Steam to ZPM.

![Arsenal, Steam to HV](docs/assets/infographics/arsenal-steam-to-hv.png)

![Arsenal, EV to ZPM](docs/assets/infographics/arsenal-ev-to-zpm.png)

#### Recipes

The Steam and LV crafting chains.

![Recipes, Steam and LV](docs/assets/infographics/recipes-steam-and-lv.png)

#### Ammo and magazines

From GT materials to a loaded gun.

![Ammo and magazines](docs/assets/infographics/ammo-and-magazines.png)

#### Armor and gear

Twelve armor sets, masks and back gear.

![Armor and gear](docs/assets/infographics/armor-and-gear.png)

#### Machines

Each single block and its multiblock.

![Machines](docs/assets/infographics/machines.png)

#### Multiblock build guides

Ammunition Works (with the optional Magazine Filling Bench), Armory Gantry, Rampart. Both halls need an interior lamp.

![Ammunition Works](docs/assets/infographics/multiblock-ammunition-works.png)

![Armory Gantry](docs/assets/infographics/multiblock-armory-gantry.png)

![Rampart](docs/assets/infographics/multiblock-rampart.png)

#### Enemies and loot

Who shoots back, and what they drop.

![Enemies and loot](docs/assets/infographics/enemies.png)

#### The questbook

Chapters, side branches and rewards. The tab ships inside the mod. A server owner switches it on with `questbook.install=true` in `config/gregguns.cfg`, then restarts the server (see [docs/config.md](docs/config.md#hidden-keys)).

![Questbook](docs/assets/infographics/questbook.png)

## Building

Requires JDK 25. The build compiles down to Java 8 bytecode through Jabel.

```sh
./gradlew setupDecompWorkspace   # first time only
./gradlew build                  # jar lands in build/libs/
./gradlew runClient              # dev client
./gradlew spotlessApply          # format before committing
```

Dependencies go in [`dependencies.gradle`](dependencies.gradle), extra repositories in [`repositories.gradle`](repositories.gradle), and mod/build settings in [`gradle.properties`](gradle.properties). Leave `build.gradle.kts` and `settings.gradle.kts` alone: they come from the [GTNH buildscript](https://github.com/GTNewHorizons/ExampleMod1.7.10) and are replaced wholesale on updates.

## Relationship to Techguns

Techguns is by pWn3d1337, and its repository declares no license, so all rights are reserved. GregGuns uses Techguns as a design reference only. **Do not copy Techguns code, textures, models or sounds into this repository** without explicit permission from its author.

What Techguns does is captured in a behavioral spec at [docs/techguns-spec/](docs/techguns-spec/README.md). Implement from that spec, not from the Techguns source.

## License

Everything in this repository is released under the [MIT License](LICENSE). That covers code, textures, models, sounds, animations, logos, infographics, documentation, specs and tools. Anyone may download, use, copy, modify, merge, publish, distribute, sublicense and sell it under the terms of that license.

Exceptions: a few files come from upstream projects and keep their own licenses. These are the Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/`, Apache-2.0) and the GTNH buildscript files (`build.gradle.kts`, `settings.gradle.kts`, `gtnhShared/`, from [GTNewHorizons/ExampleMod1.7.10](https://github.com/GTNewHorizons/ExampleMod1.7.10)).

The infographics in `docs/assets/infographics/` show GregTech 5 Unofficial textures (hulls, hatches, frames, glass and material icons). Those textures stay under GregTech's LGPL-3.0; the rest of each image is MIT.

The MIT License covers only what is in this repository. It grants no rights to Techguns, which is not included here (see [Relationship to Techguns](#relationship-to-techguns)).
