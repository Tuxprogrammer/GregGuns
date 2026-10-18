# Researching GTNH and other mods' APIs

How to find out how GregTech, IC2, NEI, Thaumcraft or any other mod in the pack really behaves before GregGuns integrates with it.

> [!IMPORTANT]
> **Scope.** This guide covers **external integrations only**: GregGuns code that calls, extends, registers with or reacts to GT5-Unofficial, GTNHLib, ModularUI, IC2, NEI, other mods or the GTNH modpack itself. It does **not** apply to Techguns. Techguns behavior comes from the [spec](../techguns-spec/README.md), under the [originality rule](../../CONTRIBUTING.md#originality-the-techguns-boundary).

## Contents

1. [The reference pack](#the-reference-pack)
2. [Order of evidence](#order-of-evidence)
3. [Step 1: find the mod and its pinned source](#step-1-find-the-mod-and-its-pinned-source)
4. [Step 2: read the source on GitHub (preferred)](#step-2-read-the-source-on-github-preferred)
5. [Step 3: decompile (fallback only)](#step-3-decompile-fallback-only)
6. [Comment conventions](#comment-conventions)
7. [Licenses and copying](#licenses-and-copying)
8. [Updating the reference pack](#updating-the-reference-pack)

## The reference pack

A full, extracted GTNH client pack lives in the repo at `reference/gtnh/`. That folder is **git-ignored**, so it exists only on machines where someone has fetched it. Never commit anything under `reference/`.

| | |
|---|---|
| **Current version** | `2.9.0-RC-2` (also in `reference/gtnh/CURRENT`) |
| **Fetched from** | `https://downloads.gtnewhorizons.com/Multi_mc_downloads/betas/GT_New_Horizons_2.9.0-RC-2_Java_17-26.zip` |
| **Mod jars** | `reference/gtnh/2.9.0-RC-2/.minecraft/mods/` (242 jars) |
| **Pack config** | `reference/gtnh/2.9.0-RC-2/.minecraft/config/`: how the pack actually configures each mod |
| **Mod index** | `reference/gtnh/2.9.0-RC-2/MOD-INDEX.md` (and `mod-index.json`): every mod with its version, jar, mod IDs, side, license and source link |
| **Release manifest** | `reference/gtnh/2.9.0-RC-2/manifest.json`, from [DreamAssemblerXXL](https://github.com/GTNewHorizons/DreamAssemblerXXL/tree/master/releases/manifests) |
| **Cloned sources** | `reference/src/<Mod>@<tag>/` (created on demand) |
| **Dev, API and sources jars** | `reference/maven/<Mod>/` (created on demand by `jars`) |
| **Decompiled output** | `reference/decompiled/<jar>/` (created on demand) |

If `reference/gtnh/` is missing, fetch it (about 730 MB to download, 870 MB extracted):

```sh
python -I tools/gtnh-reference/gtnh_ref.py fetch https://downloads.gtnewhorizons.com/Multi_mc_downloads/betas/GT_New_Horizons_2.9.0-RC-2_Java_17-26.zip --keep-archive
```

The pack's mod versions are the versions GregGuns targets. Pin every dependency in [`dependencies.gradle`](../../dependencies.gradle) to the version listed in `MOD-INDEX.md`.

Searches with `rg`, and the Grep and Glob tools, skip git-ignored folders by default. To search the pack, point them at `reference/` explicitly; with `rg`, also pass `--no-ignore`.

## Order of evidence

Use the first source that answers the question. Go further down the list only when the one above can't.

1. **The mod's source code on GitHub, at the tag the pack ships.** Source is the authority: it has the comments, the history and the intent, and it's what you link to.
2. **The shipped jar's signatures** (`javap`). Use it to confirm that a class or method from the source really exists in the pack's build. This doesn't replace reading the source.
3. **Decompiled bytecode.** A fallback, **only for mods whose source isn't available**. Code that relies on it carries a hedge comment.

Prefer a mod's documented API (an `api` package, events, IMC messages, registries) over its internals. Before inventing an integration, search for how other GTNH mods already do the same thing (see [the org-wide search](#step-2-read-the-source-on-github-preferred)); a precedent in the pack is the strongest evidence of the intended use.

## Step 1: find the mod and its pinned source

Use the helper in [`tools/gtnh-reference/`](../../tools/gtnh-reference/README.md):

```sh
T=tools/gtnh-reference/gtnh_ref.py
python -I $T mod gregtech                      # by mod ID, pack name or jar name
python -I $T where gregtech.api.enums.GTValues # which jar ships a class (also takes fragments: IElectricItem)
```

`mod` prints the jar, mod IDs (for `Loader.isModLoaded`), license, source status, notes, extras and ready-to-run `gh` commands. The index combines GTNH's own release metadata with hand-verified entries from [`tools/gtnh-reference/source-overrides.json`](../../tools/gtnh-reference/source-overrides.json). The **Source** column says what's available, best first:

| Source | Meaning | What to do |
|---|---|---|
| `gtnh-github` | A GTNH repository. Its git tag equals the pack version. | Read the source at that tag (Step 2). |
| `upstream-tag` | Another GitHub repository, with a tag verified to match the jar. | Same as `gtnh-github`. |
| `upstream-branch` | GitHub source on a branch that's close to the jar's version, but not pinned to it. | Read it, confirm anything that matters with `javap`, and pin permalinks to the commit you read. |
| `api-source` | Only the mod's **API** has published source (IC2, Thaumcraft). | Use the API source for the API. For internals, use the extras below, then decompile. |
| `decompiled-source` | Someone else's decompiled source (Witchery). | Treat it like Step 3: the hedge comment applies. |
| `upstream-github` | GTNH's index names an upstream repository, but nobody has checked which tag matches. | Find the matching tag, then add it to `source-overrides.json`. |
| `none-known` / `unlisted` | No source found. | Search again (below). If there's still none, decompile (Step 3). |

The **Extras** column lists other evidence, which `mod` prints with URLs and `jars <mod>` downloads into `reference/maven/<mod>/`:

| Extra | What it is |
|---|---|
| `api-source` | Official `.java` source for the API. For example, IC2's `-api.jar` holds the real source of all of `ic2.api`. |
| `dev-jar` | Deobfuscated classes with MCP names. Decompile these instead of the pack jar when one exists: the output is cleaner. |
| `decompiled-source` | A pre-decompiled `-sources.jar`. For example, Thaumcraft's is Fernflower output, not Azanor's code. The hedge applies. |
| `maven` | The coordinate to put in `dependencies.gradle`, as GTNH's own mods use it. `curse.maven:` jars are obfuscated, so wrap them in `rfg.deobf(...)`. |

In 2.9.0-RC-2, 214 of 242 mods are `gtnh-github` and 12 are `upstream-tag`. Ten have no known source: BiblioCraft and its three BiblioWoods add-ons, Extra Utilities, Advanced Solar Panels, GraviSuite, Ztones, UniLib, and JourneyMap Server (which isn't in the client pack).

**Where else to look before decompiling.** The GTNH index often misses source that exists. Look in these places, and when you find something, add it to `source-overrides.json` and run `index`:

- **GitHub.** Use `gh search repos <mod name>`, and check the author's account and forks. [GTNH-Museum](https://github.com/GTNH-Museum) archives old pack sources.
- **GTNH's own build files.** Run `gh search code '<mod>' --owner GTNewHorizons --filename dependencies.gradle` to reveal the maven coordinate GTNH developers compile against.
- **Mavens.** GTNH's nexus (`https://nexus.gtnewhorizons.com/repository/public/`) publishes `-sources.jar` and `-dev.jar` for GTNH mods. The legacy GTNH maven (`https://gregtech.overminddl1.com/`) and mod authors' own mavens (`maven.ic2.player.to`, `mvn.falsepattern.com`, `files.vexatos.com`) host dev, API and sources jars.
- **The jar itself.** Some jars ship `.java` files or an API package alongside their classes.

## Step 2: read the source on GitHub (preferred)

Use the `gh` CLI. Replace `OWNER/REPO` and `TAG` with the values from `mod`.

```sh
# Discover: where is a symbol defined or used? (searches the default branch, not the tag)
gh search code 'GTOreDictUnificator' --repo GTNewHorizons/GT5-Unofficial --language java
# Precedent: how do other GTNH mods integrate with it?
gh search code 'IElectricItem' --owner GTNewHorizons --language java

# Read: fetch a file at the exact tag the pack ships
gh api 'repos/OWNER/REPO/contents/PATH?ref=TAG' -H 'Accept: application/vnd.github.raw'
# Browse: list the files at that tag
gh api 'repos/OWNER/REPO/git/trees/TAG?recursive=1' --jq '.tree[].path' | grep -i recipe

# History: why does it work this way?
gh api 'repos/OWNER/REPO/commits?path=PATH&sha=TAG' --jq '.[].commit.message' | head
```

For heavy research on one mod, such as GT5-Unofficial, clone it once and search locally:

```sh
python -I $T clone GT5-Unofficial   # shallow clone at the pinned tag -> reference/src/GT5-Unofficial@5.09.54.205/
```

Rules:

- **Read at the pinned tag.** Code search covers only the default branch, which may be ahead of the pack. Confirm what you found at the tag before relying on it.
- **Confirm against the jar** when it matters (a recently added method, an `upstream-github` mod):
  `javap -p -cp reference/gtnh/2.9.0-RC-2/.minecraft/mods/<jar> <fqcn>`.
- **Link what you used** (see [Comment conventions](#comment-conventions)).

## Step 3: decompile (fallback only)

Decompile only when there is **no source** for the code in question at the version the pack ships, after checking the index and [searching further](#step-1-find-the-mod-and-its-pinned-source). For example, IC2's API has source, so read it; IC2's internals don't, so decompiling them is allowed. A decompiled API surface is a guess: names of parameters and locals are lost, inlined constants disappear, and decompilers sometimes get control flow wrong.

```sh
python -I $T jars IC2                                            # fetch the dev jar, if the mod has one
python -I $T decompile "reference/maven/Industrial Craft 2/industrialcraft-2-2.2.828-experimental-dev.jar" --class ic2.core.item.ElectricItemManager
python -I $T decompile Witchery --class com.emoniph.witchery.Witchery   # the pack jar, when there's no dev jar
python -I $T decompile Thaumcraft                                # a whole jar (slow and large; prefer --class)
```

The helper refuses to decompile a `gtnh-github` or `upstream-tag` mod unless you pass `--force`; use that only when the source genuinely can't answer the question, and say why in your PR. It:

- uses [Vineflower](https://github.com/Vineflower/vineflower) (pinned, checksum-verified, cached in `reference/tools/`);
- writes to `reference/decompiled/<jar>/`, with a banner at the top of each file;
- remaps Minecraft's obfuscated SRG names (`func_77973_b` becomes `getItem`) using the MCP `stable_12` CSVs that the GTNH buildscript unpacks into the Gradle cache. Run `./gradlew build` once if the helper reports them missing.

Hard rules for decompiled material:

- **Never paste decompiled code into GregGuns**, not even a "small helper". Learn the contract (class names, signatures, constants, call order) and write our own code against it.
- **Never commit** anything from `reference/decompiled/`.
- **Every use carries a hedge comment** (below), so a reviewer knows the claim is unverified.

## Comment conventions

Put these on the class Javadoc, or directly above the call site, override or registration that depends on the external behavior. One comment per external contract relied on.

**Source link (should, wherever source exists).** A permalink **pinned to the pack's tag**, never `master` or `HEAD`, with line anchors when it points at specific behavior:

```java
// Source: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTRecipeBuilder.java#L1201-L1206
// addTo() registers the recipe with every given map, so one builder can feed several maps.
```

For an `upstream-branch`, `api-source` or `decompiled-source` repository the ref is a branch, which moves; pin the permalink to the commit you read (`clone` prints it), not the branch name.

**Decompilation hedge (must, wherever decompilation was the evidence).** This includes `decompiled-source` repositories and jars made by others. Name the jar and pack version, the class and member examined, and the fact that it's unverified:

```java
// Decompiled: industrialcraft-2-2.2.828-experimental-dev.jar (GTNH 2.9.0-RC-2), ic2.core.item.ElectricItemManager#charge.
// No source is available for IC2 internals; this behavior was inferred from decompiled bytecode and may be
// wrong or change without notice. Re-check when the pack updates IC2.
```

When a pack update changes the version, update the tag in `// Source:` links and re-check each `// Decompiled:` hedge for the affected mods.

## Licenses and copying

- Reading any mod's source or bytecode to learn how to interoperate with it is fine.
- **Copying is not.** GregGuns is MIT. The pack's mods are mostly LGPL or GPL, and some are All Rights Reserved (see the License column of `MOD-INDEX.md`). Write integration code from your understanding of the API; don't transplant implementation code.
- Calling a mod's public API, implementing its interfaces and reading its constants is integration, not copying.

## Updating the reference pack

When the project moves to a new GTNH release:

1. `python -I tools/gtnh-reference/gtnh_ref.py fetch <new-pack-zip-url>`. This extracts the pack to `reference/gtnh/<version>/`, downloads its manifest, rebuilds the index and updates `CURRENT`.
2. Update **Current version** and **Fetched from** above, and the pack version in [CLAUDE.md](../../CLAUDE.md).
3. Re-check every entry in [`source-overrides.json`](../../tools/gtnh-reference/source-overrides.json) whose mod version changed, by updating the tag and the extras' URLs, then run `index` again.
4. Re-pin [`dependencies.gradle`](../../dependencies.gradle) to the new `MOD-INDEX.md` versions, then update the tags in `// Source:` links and re-check `// Decompiled:` hedges for mods whose version changed.
5. Delete the old `reference/gtnh/<old-version>/` folder when nothing needs it.

Pack zips are listed at <https://downloads.gtnewhorizons.com/Multi_mc_downloads/> (stable releases at the top level, release candidates and betas under `betas/`). Use a `Java_17-*` build.
