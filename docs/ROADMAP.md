# Roadmap

GregGuns brings Techguns-style weapons and gear into GT New Horizons. The code is written fresh against GTNH's APIs, and Techguns serves only as a design reference.

## Principles

- **Fresh code, not a port.** Use the [Techguns behavioural spec](techguns-spec/README.md) to understand *what* a feature does, then build it against GTNH's APIs. Its [bug register](techguns-spec/BUGS.md) lists Techguns bugs not to reproduce. Do not copy its code or assets. Its repository is unlicensed, so all rights are reserved.
- **GregTech owns progression.** Each weapon, component and piece of armor sits at a GT tier.
- **One thing at a time.** Each change should cover one coherent slice that builds and runs.

## Open items

These are known design areas. None is scheduled yet.

### Tier gating
Assign components, weapons and ammo to GTNH tiers. GTNH has two usual ways to gate:
- recipes that need a GT machine at a given voltage (LV, MV, HV, ...), and
- materials that can't be obtained before the target tier.

### Recipes and crafting chains
Rework Techguns component recipes and crafting chains to use GT materials, parts and machines in place of standalone crafting.

### Original assets
Techguns is All Rights Reserved, including on CurseForge, so every texture, model and sound in GregGuns is made fresh for this mod.

### Machine deconfliction
Techguns has its own crafting machines (ammo press, metal press, chem lab, reaction chamber, etc.). Some duplicate GT machines and should be dropped in favour of GT recipe maps. Keep the ones that are distinctive and tasteful in the pack.
