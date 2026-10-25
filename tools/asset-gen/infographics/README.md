# Player guide infographics

Builds the PNGs in [`docs/assets/infographics/`](../../../docs/assets/infographics/). They're written for GTNH players, not developers. Each one stands alone and can be posted anywhere, so it never links to the repository, specs, quest IDs or MTE IDs.

## Build

```sh
# 1. Export the gun, armor and accessory models (once, and again after model changes)
GREGGUNS_EXPORT_MODELS="$PWD/build/infographics/export" ./gradlew test --rerun \
    --tests com.gregguns.tools.ModelExportTest --tests com.gregguns.tools.PulseRifleExportTest
# 2. Export the recipe tables (once, and again after recipe changes); the recipes sheet reads them
GREGGUNS_EXPORT_RECIPES="$PWD/build/infographics/recipes.json" ./gradlew test --rerun \
    --tests com.gregguns.tools.RecipeExportTest
# 3. Build every page, or name some
python tools/asset-gen/infographics/build.py [getting-started arsenal-steam-to-hv ...]
```

Set `GREGGUNS_INFOGRAPHICS_OUT=<dir>` to write the PNGs somewhere other than `docs/assets/infographics/`, for example to compare a rebuild with the committed sheets.

On Linux, if `./gradlew` is not executable, run `sh ./gradlew`, and point `JAVA_HOME` at a Java 17 to 25 JDK. On Windows, give the export paths to Java as `C:/...` (for example `$(cygpath -m "$PWD")` in Git Bash), because it reads `/c/...` as `C:\c\...`.

Needs Pillow, numpy and Playwright. On Windows it drives the Edge that ships with the OS. Elsewhere it uses Playwright's bundled Chromium: run `python -m playwright install chromium` once. The sheets use Segoe UI and Bahnschrift, so install them on Linux too (Arch: the AUR package `ttf-ms-win11-auto`); with them a Linux rebuild matches the committed sheets to within a few percent of pixels. Without them the text falls back to Fira Sans and Fira Sans Condensed, then Noto Sans, and a few lines wrap differently. Never commit font files. GregTech textures come from the GT jar in the extracted reference pack (`reference/gtnh/`, or the main checkout's copy when you're in a worktree). If it's missing, run `python -I tools/gtnh-reference/gtnh_ref.py fetch`.

Renders are cached in `build/infographics/r/` by their parameters and a hash of each exported model, so a model change redraws on its own. Delete that folder after you change a texture.

## Files

| File | What it does |
|---|---|
| `render3d.py` | Software renderer for 1.7.10 box models (guns, mobs, armor), Wavefront OBJ meshes with one texture (`obj_scene`, the same files the game loads; strict by default, so it accepts only what Forge's loader accepts) and voxel scenes, output as transparent PNGs |
| `check_obj.py`, `testdata/` | Self-check for the OBJ path: renders a hand-made ogive, asserts the output is plausible, and asserts that files Forge 1.7.10's `WavefrontObject` would reject (n-gons, negative indices, mixed tri/quad groups, integer vertices) are rejected too. Run `python tools/asset-gen/infographics/check_obj.py [preview.png]` |
| `obj_preview.py` | Preview sheet for OBJ models (three views, face count, size, texture): the test ogive plus any shipped OBJ with a texture. Writes `build/previews/obj.png`, or `$GREGGUNS_ASSETS_OUT/previews/obj.png`; never committed. `--obj`/`--tex` pairs add models |
| `blocks.py` | Reads multiblock layouts from the mod's `*Shape.java` strings; composites casing and overlay textures |
| `gt.py` | GregTech textures from the GT jar: tinted material icons, hulls, hatches, frames, glass |
| `scenes.py` | Machine blocks, the turret, and the four multiblocks with their hatches and block counts |
| `kit.py` | Render cache, page shell and CSS, and rasterizing through Playwright |
| `data.py` | Player-facing facts: weapons per tier, ammo, magazines |
| `pages.py`, `pages_more.py` | One function per infographic; `pages_more.PAGES` lists them all |
| `pages_recipes.py` | The recipes sheet: crafting grids for the hand recipes, machine rows from the exported recipe tables |
| `vanilla.py` | Our own simple icons for the vanilla items recipes use; Mojang's textures are never shown |

## Writing rules

- Use plain player language, with GT terms where GT players already know them.
- Describe the mod as finished.
- Use in-game display names and American spelling.
- Give every dark model a spotlight (`.spot`, `.wcard .pic`). Every render (`img.r`) gets a light rim so it stands off the dark cards.
- Guns in a figure's hands use the mod's own third-person lengths, read from `InHandTargets.java`, with the muzzle forward. Turn a gunman side-on so the gun shows; a left-facing one is rendered facing right and mirrored.
- Render only our own textures and GregTech's. The Skeleton Soldier wears the vanilla skeleton texture in game, so it is drawn with a plain bone skin (`kit.bone_skin()`).
- License: our art is MIT. The GregTech textures shown are GT5 Unofficial's (LGPL-3.0), credited in `docs/assets/CREDITS.md`.
