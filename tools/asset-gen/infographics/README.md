# Player guide infographics

Builds the PNGs in [`docs/assets/infographics/`](../../../docs/assets/infographics/). They're written for GTNH players, not developers. Each one stands alone and can be posted anywhere, so it never links to the repository, specs, quest IDs or MTE IDs.

## Build

```sh
# 1. Export the gun, armor and accessory models (once, and again after model changes)
GREGGUNS_EXPORT_MODELS="$(cygpath -m "$PWD")/build/infographics/export" ./gradlew test --rerun \
    --tests com.gregguns.tools.ModelExportTest --tests com.gregguns.tools.PulseRifleExportTest
# 2. Build every page, or name some
python tools/asset-gen/infographics/build.py [getting-started arsenal-steam-to-hv ...]
```

Give the export directory as a Windows path (`C:/...`). Java reads `/c/...` as `C:\c\...`.

Needs Pillow, numpy and Playwright (it drives the Edge that ships with Windows). GregTech textures come from the GT jar in the extracted reference pack (`reference/gtnh/`, or the main checkout's copy when you're in a worktree). If it's missing, run `python -I tools/gtnh-reference/gtnh_ref.py fetch`.

Renders are cached in `build/infographics/r/` by their parameters. Delete that folder after you change a texture.

## Files

| File | What it does |
|---|---|
| `render3d.py` | Software renderer for 1.7.10 box models (guns, mobs, armor) and voxel scenes, output as transparent PNGs |
| `blocks.py` | Reads multiblock layouts from the mod's `*Shape.java` strings; composites casing and overlay textures |
| `gt.py` | GregTech textures from the GT jar: tinted material icons, hulls, hatches, frames, glass |
| `scenes.py` | Machine blocks, the turret, and the four multiblocks with their hatches and block counts |
| `kit.py` | Render cache, page shell and CSS, and rasterizing through Playwright |
| `data.py` | Player-facing facts: weapons per tier, ammo, magazines |
| `pages.py`, `pages_more.py` | One function per infographic; `pages_more.PAGES` lists them all |

## Writing rules

- Use plain player language, with GT terms where GT players already know them.
- Describe the mod as finished.
- Use in-game display names and American spelling.
- Give every dark model a spotlight (`.spot`, `.wcard .pic`). Every render (`img.r`) gets a light rim so it stands off the dark cards.
- License: our art is MIT. The GregTech textures shown are GT5 Unofficial's (LGPL-3.0), credited in `docs/assets/CREDITS.md`.
