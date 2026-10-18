# Angelica-safe gun rendering

- **Techguns baseline:** [rendering-framework.md](../../techguns-spec/03-gun-visuals-audio/rendering-framework.md) (item renderers, §7 muzzle flash, §9 zoom and scopes), [GVA-04 and GVA-05](../../techguns-spec/03-gun-visuals-audio/index.md#suspected-bugs) (GL state leaks)
- **Design records:** [0006 art direction](../../design/0006-art-direction.md)
- **Pack mods:** Angelica (`angelica`, 2.2.28; it also ships `embeddium`, `mcpatcherforge` and `notfine`)
- **Roadmap:** M1 (gun rendering framework), M5 (beam and energy weapon renderers)

## What changes from Techguns

Techguns was written for vanilla GL. In the pack, every GL call from a mod class goes through Angelica's `GLStateManager`, with Iris shaders and fixed-function emulation behind it. The Techguns approach still works. GregGuns adds rules so that it keeps working, and so that the optional fast paths can be used.

| Techguns behaviour | GregGuns behaviour | Why |
|---|---|---|
| Raw `GL11` calls in renderers, with leaked state in places (GVA-04, GVA-05) | Same API, but every attribute or matrix push is paired with a restore on all paths | `GLStateManager` tracks state. A leak survives into the next frame under Angelica and can break shader-pack passes. |
| Muzzle flash and scope overlay drawn from a mixture of item-render and overlay code | Scope overlay and any per-frame HUD effect hang off overlay event types that stay live under HUD caching | HUD caching reuses stale pixels for most elements. |
| Scope zoom changes FOV only | Same, and it must still be correct when Angelica's own zoom key is held | Angelica divides the final FOV by its zoom factor. |

## Integration points

### Angelica: GL calls are redirected at class load

- **Pack config:** none. Angelica's GL layer is always on.
- **Source:** [GLRedirects.java#L9-L30](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/gtnewhorizons/angelica/api/GLRedirects.java#L9-L30) says calls such as `GL11.glPushMatrix()` are rewritten to `GLStateManager` by a bytecode transformer. It names the cases that escape the rewrite: script engines, reflection and method handles.
- **GregGuns does:** renderers call `GL11`, `GL13`, `GL14` and `Tessellator` directly from normal classes. No reflection, `MethodHandle` or script-driven GL. No direct `GLStateManager` import, so the mod runs without Angelica.
- **Pack-side change:** none.

### Angelica: entity batching decides per renderer

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/angelica-modules.cfg`, keys `enableEntityBatching` = `true` and `enableCubeInstancing` = `true`.
- **Source:** [MixinRenderManager_BatchEligibility.java#L16-L30](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/mixin/java/com/gtnewhorizons/angelica/mixins/early/angelica/entity/MixinRenderManager_BatchEligibility.java#L16-L30) wraps every `Render.doRender`. [BatchEligibility.java#L8](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/gtnewhorizons/angelica/rendering/tesr/BatchEligibility.java#L8) states the rule: renderers that mix batched model parts with their own draws are excluded, because batching would reorder their geometry. [L97-L109](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/gtnewhorizons/angelica/rendering/tesr/BatchEligibility.java#L97-L109) shows the verdict is learned per `Render` instance: unknown, safe or denied.
- **What it means:** a projectile or grenade renderer that draws only `ModelRenderer` boxes is batched. A renderer that also issues its own `Tessellator` quads (beams, glow sprites) is denied and drawn the normal way. Both are correct. Denial costs speed, not correctness.
- **GregGuns does:** keep one renderer per entity class, so the verdict is stable. Where cheap, split a glow quad into its own pass after the model, rather than interleaving it.
- **Pack-side change:** none.

### Angelica: HUD caching keeps some elements live

- **Pack config:** `angelica-modules.cfg`, keys `enableHudCaching` = `true`, `hudCachingActive` = `false` (the runtime switch), `hudCachingFPS` = `20` and `enableHudCachingEventTransformer` = `true`.
- **Source:** [HUDCaching.java#L156-L157](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/gtnewhorizons/angelica/hudcaching/HUDCaching.java#L156-L157): with `hudCachingActive` off, the HUD renders normally. With it on, the HUD is drawn into a framebuffer at `hudCachingFPS` and composited. [L217-L237](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/gtnewhorizons/angelica/hudcaching/HUDCaching.java#L217-L237) shows that the vignette, helmet, portal and crosshair elements are re-rendered live each frame. [HUDCachingEarlyReturnTransformer.java#L19-L40](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/gtnewhorizons/angelica/loading/fml/compat/transformers/generic/HUDCachingEarlyReturnTransformer.java#L19-L40) injects an early return into methods of other mods' classes while the cache is being built.
- **GregGuns does:** the scope overlay, hit markers and any recoil-driven HUD effect are drawn on the `HELMET` or `CROSSHAIRS` element events, or in a render-tick handler. Ammo counters and other slow HUD text may use any element. The shipped default is off, so players only see the stale-HUD problem if they switch it on.
- **Pack-side change:** none. How third-party receivers behave under the early-return transformer is not verified here; see Open questions.

### Angelica: zoom key stacks with scope zoom

- **Pack config:** `angelica-modules.cfg`, key `enableZoom` = `true`.
- **Source:** [MixinEntityRenderer_Zoom.java#L17-L21](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/mixin/java/com/gtnewhorizons/angelica/mixins/early/angelica/zoom/MixinEntityRenderer_Zoom.java#L17-L21) divides the return value of `getFOVModifier` by the current zoom. The zoom key has no default binding ([Zoom.java#L33](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/gtnewhorizons/angelica/zoom/Zoom.java#L33), key code 0).
- **GregGuns does:** apply scope FOV through the normal FOV event, not by replacing `getFOVModifier`. The two zooms then multiply. The scope overlay size is computed from the screen, not from FOV, so it is unaffected.
- **Pack-side change:** none.

### Angelica with a shader pack: no special API

Iris shaders (`enableIris` = `true`) draw entities and held items in phases. Angelica 2.2.28 exposes no registry for mod item renderers (its `api` package covers lighting, TESR meshes, clouds and GL redirects). Under a shader pack, `GLStateManager` is still the only GL path, so the rules above are the whole contract. Additive glow sprites (muzzle flash, laser core) use blend state only, with no mod-side shader. Dynamic lights switch off while a shader pack is active; see [dynamic-lights.md](../04-projectiles-fx/dynamic-lights.md).

## Requirements

- **GGS-03-01.** No GregGuns renderer calls GL through reflection, method handles or a script engine.
- **GGS-03-02.** Every GL attribute or matrix push in a renderer has a matching pop on all paths, including exceptions (`try/finally`).
- **GGS-03-03.** Renderers do not import `com.gtnewhorizons.angelica.*`, except in classes guarded by `Loader.isModLoaded("angelica")` or `@Optional`.
- **GGS-03-04.** The scope overlay and recoil-driven HUD effects are drawn from `HELMET`, `CROSSHAIRS` or a render-tick handler, not from `ElementType.ALL` or `TEXT`.
- **GGS-03-05.** Scope FOV is applied through the FOV event so that it multiplies with Angelica's zoom.
- **GGS-03-06.** Each entity renderer class either draws only model parts, or keeps its non-model draws in a clearly separate pass after the model.

## Tests

- GGS-03-01, GGS-03-03: code review checklist, plus a CI grep rule that fails on `MethodHandle` or `GLStateManager` in renderer packages.
- GGS-03-02: raw GL cannot be unit-tested. Verify in `runClient` with Angelica loaded: F3 and the next-frame look show no state drift after 100 shots.
- GGS-03-04: manual `runClient` check with `hudCachingActive=true`. The scope overlay must move at full frame rate.
- GGS-03-05: manual check with the Angelica zoom key bound and held while scoped.
- GGS-03-06: code review.

## Open questions

- How do third-party `RenderGameOverlayEvent` receivers behave under the early-return transformer? Not verified; test in `runClient` with `hudCachingActive=true` before the M1 exit gate.
- Does a held-item `IItemRenderer` that draws additive quads under an active shader pack need a phase hint? Angelica 2.2.28 has no public API for it. Needs a manual check with Complementary shaders; open a design record only if a workaround is needed.
