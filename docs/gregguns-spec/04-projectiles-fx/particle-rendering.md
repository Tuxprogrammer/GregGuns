# Particles under Angelica and the pack's particle cap

- **Techguns baseline:** [particle-system.md](../../techguns-spec/04-projectiles-fx/particle-system.md) (especially [§3.3 `EntityParticle`](../../techguns-spec/04-projectiles-fx/particle-system.md#33-entityparticle-one-particle) and [§3.4 render types](../../techguns-spec/04-projectiles-fx/particle-system.md#34-render-types)), [fx-catalog.md](../../techguns-spec/04-projectiles-fx/fx-catalog.md)
- **Design records:** none specific
- **Pack mods:** Angelica (`angelica`, 2.2.28), Hodgepodge (`hodgepodge`, 2.7.215)
- **Roadmap:** M5 exit gate ("performance is checked with many emitters on screen")

## What changes from Techguns

The data-driven emitter model (`FXList.txt`, `EntityParticleSystem`, `EntityParticle`) is kept as the Techguns spec describes it. Two pack facts change how the particle class must render and how many particles it may create.

| Techguns behavior | GregGuns behavior | Why |
|---|---|---|
| Each `EntityParticle` flushes the effect renderer's tessellator, binds its own texture, draws one quad, rebinds `particles.png` and restarts the batch (one draw call per particle) | One quad per particle, written to the `Tessellator` that `renderParticle` receives, with UVs set before each vertex and one color. Texture state is set before the first vertex. | Angelica turns such particles into GPU-instanced draws. Anything else falls back to the slow path. |
| No particle budget. A burst spawns `rand(ParticleCount)` particles each tick | A global active-particle budget below the pack's cap, shared with other mods | Hodgepodge raises the vanilla cap of 4000 to 8000, and every mod's particles count against it. |

## Integration points

### Angelica: instanced particles, with a capture path for custom particles

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/angelica-modules.cfg`. No particle key exists in this file. The batching is on whenever Angelica's `enableDeferredBatching` option (in its Sodium-style options, not in this file) is true; that option was read in code at [ParticleInstancer.java#L99-L114](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/gtnewhorizons/angelica/rendering/particles/ParticleInstancer.java#L99-L114).
- **Source:**
  - [MixinEffectRenderer_ParticleBatch.java#L17-L31](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/mixin/java/com/gtnewhorizons/angelica/mixins/early/angelica/particles/MixinEffectRenderer_ParticleBatch.java#L17-L31) wraps the `renderParticle` call inside `EffectRenderer.renderParticles` and the final `draw()`.
  - [ParticleDescriptorRegistry.java#L60-L70](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/gtnewhorizons/angelica/rendering/particles/ParticleDescriptorRegistry.java#L60-L70): a particle class not registered by name, and which overrides `renderParticle`, gets the **capture** path. A class that does not override it gets the vanilla descriptor.
  - [ParticleInstancer.java#L145-L165](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/gtnewhorizons/angelica/rendering/particles/ParticleInstancer.java#L145-L165): the capture path calls the particle's own `renderParticle` with a recording tessellator. If it captured exactly one quad and decoded it, the quad is instanced. If the recorder spilled, the quad is replayed through the real tessellator (still correct, just slower).
  - [ParticleCaptureTessellator.java#L12](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/gtnewhorizons/angelica/rendering/particles/ParticleCaptureTessellator.java#L12), [L137-L152](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/gtnewhorizons/angelica/rendering/particles/ParticleCaptureTessellator.java#L137-L152), [L193-L196](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/gtnewhorizons/angelica/rendering/particles/ParticleCaptureTessellator.java#L193-L196), [L220-L230](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/gtnewhorizons/angelica/rendering/particles/ParticleCaptureTessellator.java#L220-L230): the recorder holds at most four vertices. It spills when a vertex arrives with no UV set, after the quad is closed, or as the fifth; when the color changes after a vertex; and on `setNormal` or `disableColor`. `draw()` after exactly four vertices closes the quad and is accepted.
- **GregGuns does:** `EntityParticle.renderParticle` sets blend and texture state, then does `setTextureUV`, `addVertex` four times, with a single `setColorRGBA`, and never calls `setNormal` or `disableColor`. A `draw()` straight after the fourth vertex is allowed, so the Techguns bind, draw and rebind pattern should survive (traced by reading the recorder, not run). The state sampled at the first vertex groups the instanced draws.
- **Pack-side change:** none.

### Hodgepodge: the 8000 particle cap

- **Pack config:** `hodgepodge.cfg`, keys `increaseParticleLimit` = `true` (L1322) and `particleLimit` = `8000` (L1358).
- **Source:** [MixinEffectRenderer.java#L14-L20](https://github.com/GTNewHorizons/Hodgepodge/blob/2.7.215/src/main/java/com/mitchej123/hodgepodge/mixins/early/minecraft/MixinEffectRenderer.java#L14-L20) replaces the constant 4000 in `EffectRenderer.addEffect(EntityFX)`. The range is 4000 to 16000 ([TweaksConfig.java#L174-L177](https://github.com/GTNewHorizons/Hodgepodge/blob/2.7.215/src/main/java/com/mitchej123/hodgepodge/config/TweaksConfig.java#L174-L177)).
- **What it means:** particles added through `Minecraft.effectRenderer` are limited. Vanilla drops the oldest when the limit is hit; that detail is from vanilla knowledge and not re-read here. `EntityParticleSystem` itself is a normal entity and is not counted, only the `EntityParticle`s it emits.
- **GregGuns does:** keeps a client-side counter of live GregGuns particles with a configured ceiling, default 2000, so that other mods keep room. Bursts are scaled down as the counter approaches the ceiling. Effects keep their spec'd counts when the counter is low.
- **Pack-side change:** none. The cap is a pack-owner setting; no change is needed.

## Requirements

- **GGS-04-06.** `EntityParticle.renderParticle` emits exactly one quad through the supplied `Tessellator`. UV is set before each vertex, color is set once, and `setNormal` and `disableColor` are not called.
- **GGS-04-07.** A particle subclass that cannot meet GGS-04-06 (the legacy `EntityParticleAnimated`, `EntityLaserBeamFX`) is allowed but listed as slow-path, and not used in shipped effects.
- **GGS-04-08.** The count of live GregGuns particles never exceeds `particleCeiling` (config, default 2000, range 100 to 8000).
- **GGS-04-09.** All FX packets are handled on the client thread (fixes the class of bug behind [PFX-04](../../techguns-spec/04-projectiles-fx/index.md#suspected-bugs)); the particle counter is only touched there.

## Tests

- GGS-04-06: unit test that drives `renderParticle` with a recording `Tessellator` subclass and asserts four vertices, UV set first, one color call and no `setNormal`.
- GGS-04-07: registry test that fails if a shipped FX list uses a slow-path class.
- GGS-04-08: unit test of the budget function.
- GGS-04-09: code review, plus a `runClient` test spawning 20 emitters.
- Performance: `runClient` with Angelica and the debug counters from `ParticleInstancer` (`getCaptured`, `getSpilled`). Spilled should be zero for GregGuns effects.

## Open questions

- Whether Angelica's capture path preserves a `NO_Z_TEST` particle's disabled depth test when instanced. The state is sampled at the first vertex ([ParticleCaptureTessellator.java#L144](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/gtnewhorizons/angelica/rendering/particles/ParticleCaptureTessellator.java#L144)), which suggests yes, but the sampled fields were not read. Check visually in `runClient`.
- The 2000 default ceiling is a proposal. Tune it against a big Nuke FX list in M5.
