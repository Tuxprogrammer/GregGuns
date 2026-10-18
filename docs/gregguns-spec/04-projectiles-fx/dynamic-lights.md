# Dynamic lights for muzzle flashes and projectiles

- **Techguns baseline:** [rendering-framework.md §7](../../techguns-spec/03-gun-visuals-audio/rendering-framework.md#7-muzzle-flash-first-person-screen-effect) (muzzle flash sprite), [projectiles.md](../../techguns-spec/04-projectiles-fx/projectiles.md), [fx-catalogue.md](../../techguns-spec/04-projectiles-fx/fx-catalogue.md). Techguns has no dynamic light. Its flashes are full-bright sprites only.
- **Design records:** [0006 art direction](../../design/0006-art-direction.md)
- **Pack mods:** Angelica (`angelica`, 2.2.28)
- **Roadmap:** M5 (energy and exotic weapons); the held-gun part may land with M1

## What changes from Techguns

Techguns' effects glow on screen but do not light the world. Angelica ships dynamic lights, so a muzzle flash, a plasma bolt or a burning rocket can light nearby blocks. This is optional polish that costs nothing when Angelica or the player's setting is off. It is not a gameplay feature, and the server never knows about it.

| Techguns behaviour | GregGuns behaviour | Why |
|---|---|---|
| Muzzle flash is a sprite only | A held gun may report a luminance for a few ticks after a shot | Light on nearby walls sells the shot. |
| Energy and fire projectiles are lit sprites only | Slow, rare projectiles (rockets, plasma, flame) may emit light. Bullets never do. | Each moving light rebuilds chunk sections every tick. |

## Integration points

### Angelica: how dynamic light is gathered

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/angelica-modules.cfg`, key `enableDynamicLights` = `true` (needs a restart to change). The player picks the update mode in Angelica's video options; the code default is `OFF` ([DynamicLights.java#L48](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/gtnewhorizons/angelica/dynamiclights/DynamicLights.java#L48)).
- **Source:**
  - [DynamicLights.java#L76-L82](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/gtnewhorizons/angelica/dynamiclights/DynamicLights.java#L76-L82): lights are active only if the config is on, the mode is not `OFF`, and either no shader pack is in use or `ShaderForce` is set (default `false`, [L49](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/gtnewhorizons/angelica/dynamiclights/DynamicLights.java#L49)). **With a shader pack, dynamic lights are off by default.**
  - [L55](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/gtnewhorizons/angelica/dynamiclights/DynamicLights.java#L55) and [L318-L327](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/gtnewhorizons/angelica/dynamiclights/DynamicLights.java#L318-L327): radius is 7.75 blocks, luminance falls off linearly, maximum 15.
  - [DynamicLights.java#L584-L591](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/gtnewhorizons/angelica/dynamiclights/DynamicLights.java#L584-L591): an item emits light if its `Item` implements `IDynamicLightProducer` ([IDynamicLightProducer.java#L8-L10](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/gtnewhorizons/angelica/api/IDynamicLightProducer.java#L8-L10), method `int getLuminance(ItemStack)`), or is an `ItemBlock` with light.
  - [L605-L651](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/gtnewhorizons/angelica/dynamiclights/DynamicLights.java#L605-L651): any burning entity gives 15 (L610) and nothing in water. A living entity takes the maximum over its five equipment slots (held item and armour), Backhand's off-hand item and Baubles slots. This covers NPCs as well as players.
  - [MixinEntity.java#L84-L117](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/mixin/java/com/gtnewhorizons/angelica/mixins/early/angelica/dynamiclights/MixinEntity.java#L84-L117): the mixin sits on `Entity`, so every entity type is a light source. It runs on the client only, at the end of `onEntityUpdate`.
  - [MixinEntity.java#L125-L172](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/mixin/java/com/gtnewhorizons/angelica/mixins/early/angelica/dynamiclights/MixinEntity.java#L125-L172): when a source moves more than 0.1 blocks or its luminance changes, it schedules chunk-section rebuilds for its own section and seven neighbours.
  - [EntityLightConfig.java#L29-L48](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/gtnewhorizons/angelica/dynamiclights/config/EntityLightConfig.java#L29-L48): a player can disable lights per entity class, stored in `angelica-dynamiclights-entities.txt`.
- **GregGuns does:** see the next two sections.
- **Pack-side change:** none. Players choose the mode. Defaults leave lights off.

### Held guns and armour: `IDynamicLightProducer`

- **Source:** the producer contract above. Equipment, off-hand and bauble slots are all scanned, so one implementation lights the player's own gun, other players' guns and NPCs' guns.
- **GregGuns does:** the base gun `Item` carries `@Optional.Interface(iface = "com.gtnewhorizons.angelica.api.IDynamicLightProducer", modid = "angelica")`, so the interface is dropped when Angelica is missing. `getLuminance(stack)` returns 0 except for a short window after a shot (a proposal: 10 to 12 for 3 ticks), and a steady low value for powered energy weapons while they are charged. Glowing armour pieces (a lamp helmet) use the same hook.
- **Pack-side change:** none.

### Projectiles: two options, both soft

- **Option A, the burning flag.** Any entity that reports `isBurning()` gets luminance 15 (DynamicLights.java L610). This costs no Angelica code, but the entity also renders vanilla fire, which is wrong for a plasma bolt. Use it only for flame and incendiary projectiles, where fire is the look. The fire overlay for a non-living entity was not checked in vanilla code here; test it.
- **Option B, override the light source.** `IDynamicLightSource` ([IDynamicLightSource.java#L71](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/gtnewhorizons/angelica/dynamiclights/IDynamicLightSource.java#L71)) is implemented on `Entity` by the mixin. A projectile class can override `angelica$getLuminance()` to return a fixed value. The interface is in `dynamiclights`, not in the `api` package, so it is internal and may change between Angelica versions. Isolate it in one class behind `@Optional.Interface` and pin the Angelica version in `dependencies.gradle`. Two things are unverified: whether a subclass can compile against the interface without stubbing its other abstract methods, and whether the override is picked up when the mixin also supplies the method on `Entity`. If either is awkward, fall back to Option A. The projectile must also reach `Entity.onEntityUpdate`, because that is where the mixin hooks in.
- **GregGuns does:** Option B for plasma, laser-bolt and rocket projectiles, Option A only for flame. Bullets and pellets never emit.
- **Pack-side change:** none.

## Requirements

- **GGS-04-01.** Every use of an Angelica type is behind `@Optional.Interface` or a `Loader.isModLoaded("angelica")` guard. A dedicated server with no Angelica boots and loads the gun and projectile classes.
- **GGS-04-02.** `getLuminance` and `angelica$getLuminance` do no allocation, no NBT parse and no world access. They run every client tick for every equipped item.
- **GGS-04-03.** Luminance is at most 12 for muzzle flashes and lasts at most 3 ticks. Steady glow is at most 8.
- **GGS-04-04.** Bullets, pellets and beams that cross the world in one tick never emit light. Only projectiles with a lifetime of at least 10 ticks and a speed below 1.5 blocks per tick may.
- **GGS-04-05.** Dynamic light never affects gameplay: no server code, packet or save data depends on it.

## Tests

- GGS-04-01: `runServer` smoke test without Angelica in the dev run configuration (already the server default); plus a unit test that loads the gun `Item` class with a classloader that hides `com.gtnewhorizons.angelica`.
- GGS-04-02, GGS-04-03: unit tests on the luminance functions.
- GGS-04-04: unit test over the projectile registry that rejects an emitting projectile outside the speed and lifetime limits.
- GGS-04-05: code review.
- Visual check: `runClient` with dynamic lights on `Fancy`, no shader pack; then with a shader pack to confirm the lights switch off cleanly.

## Open questions

- **How does a held gun know it just fired?** `getLuminance` receives only the `ItemStack`. For other players' guns, the client sees the stack from the equipment packet, so a server-written NBT key would change the stack identity every shot. Options: a client-only map keyed by player id, written by the existing shot-FX packet, read through the stack's holder (not available in the call); or a transient client-side NBT key set by the shot-FX handler on the local copy. Needs a decision before M1; it may become design record 0007.
- Does the pack want a `ShaderForce` entry in an Angelica profile so lights survive shader packs? It is a player option and out of scope for GregGuns.
- `@Optional.Interface` is standard FML behaviour for 1.7.10. It was not re-verified against the pack's Forge source in this pass.
