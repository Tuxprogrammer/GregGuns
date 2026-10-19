# GregGuns

Guns, armor and military tech for [GT New Horizons](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack) on Minecraft 1.7.10.

GregGuns is a from-scratch rewrite inspired by [Techguns](https://github.com/pWn3d1337/Techguns). It is not a port. GTNH has changed 1.7.10 so much that the original code would not carry over cleanly. GregGuns is written against GTNH's own APIs instead, and its weapons, components and recipes are gated by GregTech tiers rather than vanilla-style crafting.

> [!WARNING]
> Early alpha and very much a work in progress. An [alpha pre-release](https://github.com/Tuxprogrammer/GregGuns/releases) for GTNH 2.9.0-RC-2 is out for testing. Expect bugs and changes that break worlds, and back up your saves. Its content is switched off by default; the release notes say how to turn it on. See [docs/ROADMAP.md](docs/ROADMAP.md) for where it's headed.

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

The MIT License covers only what is in this repository. It grants no rights to Techguns, which is not included here (see [Relationship to Techguns](#relationship-to-techguns)).
