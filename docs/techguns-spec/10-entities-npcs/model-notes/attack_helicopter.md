# Attack Helicopter: clean-room model notes

Design notes for rebuilding the Techguns **Attack Helicopter** (boss) look-alike in GregGuns, as our own boxes or our own mesh. Written by the "dirty room" analyst from study of the original's OBJ mesh, renderer, entity class and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no vertex lists and no texture coordinates; the builder designs its own UV layout and paints its own texture.

Related facts: [renderer](../rendering-and-models.md#5-renderhelicopter-extends-rendererlivingentity), [per-mob sheet](../per-mob.md#16-attack-helicopter-attackhelicopter-boss), [flight and attack AI](../ai.md#6-attack-helicopter), [texture sheet](../textures.md#texturesentityapachepng-256256-opaque).

**Frame used in this document.** The Minecraft entity-model frame. One unit = 1/16 block. **Y points down**. The mob **faces -Z** (the nose), and its **right side is -X**. The model origin is placed at the entity's feet (so the ground is at y = 24 only by convention; height is easier to give as "height above the feet").

**Conversion from the original.** The original is an OBJ in its own units: **1 OBJ unit = 0.25 block = 4 model units**, Y up, nose toward +X, the right side toward +Z, and its origin on the rotor mast, at the entity's feet position. To get the model frame: model Z = -4 x OBJ X, model X = -4 x OBJ Z, model Y = 24 - 4 x OBJ Y. This page gives everything in **blocks** first, using three simple numbers per shape, so no conversion is needed to build from it:

- **f**: distance **forward** of the rotor axis in blocks (negative = behind it). Model Z = -16 x f.
- **h**: height **above the feet** in blocks. Model Y = 24 - 16 x h.
- **w**: distance sideways from the centerline, in blocks (symmetric left and right unless noted). Model X = +-16 x w.

Sizes are given as length (along f) x height x width. The unit value is blocks x 16.

## 1. Gestalt

A **heavy olive-drab attack helicopter**, long and narrow like an Apache. At a glance you read: a **huge four-blade rotor** (18.5 blocks across, dark and thin) spinning over a **slim, angular fuselage**; a **tapered, pointed nose with a sloped glass canopy** and a **small chin gun** hanging under it; **short stub wings with a rocket pod under each**; and a **long, thin tail boom ending in a tall swept fin** and a small tailplane. Everything is matte olive green except the pale blue-gray glass, the near-black rotor and gun, and the mid-gray mast.

Defining features in order of visual importance:

1. **The rotor:** four flat blades in a plus shape, each 9.25 blocks long from the axis, near-black, on a short mast over the cabin.
2. **The nose:** a tapering cone with a sloped canopy behind it, and a gun below. The fuselage is a flying wedge: tall at the back of the cabin, low and pointed at the front.
3. **The tail:** a 5.75-block boom, then a tall swept fin whose tip is the highest point of the model (higher than the rotor).
4. **Stub wings with pods:** a thin wing each side at mid-cabin, a rocket pod hanging under each by a short pylon.
5. **Olive-drab hull** with a subtle mottle and darker engine cowlings.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Length, nose tip (f = +6.5) to fin tip (f = -11.75) | **18.25 blocks** (292 units) |
| Rotor diameter | **18.5 blocks** (296 units); the rotor is the widest thing |
| Wingspan (tip to tip) | **6.0 blocks** (96 units) |
| Height, lowest fuselage point (h = 0.5) to the fin tip (h = 5.0) | 4.5 blocks; the chin gun's barrel tip dips to about h = -0.6 when pitched |
| Rotor plane | h = 4.0 |
| Cabin | 5.5 long x 2 tall x 1.5 wide |
| Boom | 5.75 long x 1 tall x 1 wide |
| Render scale | the model above is the final size (the original renders its mesh at 0.25 scale; ignore that) |
| Shadow | radius **5.0** blocks, but it only matters near the ground; at its normal 25 blocks of altitude it fades out |
| Hitbox (for reference) | 10 wide x 4 tall, centered on the rotor axis. The model overhangs it by a lot: the nose reaches 6.5 blocks forward, the tail 11.75 back. The hitbox is centered on the rotor axis, not on the fuselage. |

Ratios: fuselage length (nose to fin) is about 3.4 times the cabin's length; the rotor diameter equals the length to within one block. The cabin runs from 3.5 blocks behind the rotor axis to 2.0 ahead of it, so the axis is over the cabin's front half, and the whole airframe's middle is about 2.6 blocks behind the axis.

## 3. Part by part

Everything is a plain box, wedge or flat strip, with no curves. Every shape is symmetric about the centerline unless noted. The helicopter has three logical groups: the **airframe** (fixed), the **rotor** (spins about the vertical axis at f = 0, w = 0) and the **chin gun** (pivots, see motion).

### Hierarchy
- **Airframe (fixed):** cabin, deck, engine cowling, sponsons, belly plate, nose tub, nose cone, canopy, transition, boom, spine, tailplane, fin, wings, pylons, pods.
- **Rotor group (spins):** mast, hub plate, four blades.
- **Gun group (pivots at the nose):** gun mount block and barrel.

### Cabin (fuselage center)
- Box **5.5 long x 2.0 tall x 1.5 wide**: f -3.5 to 2.0, h 0.75 to 2.75, w +-0.75. Flat sides, flat top.
- On the top of its front 3.75 blocks (f -1.75 to 2.0) sits a **0.25-block deck plate**, h 2.75 to 3.0, same width.
- Its front face (at f = 2.0) is a flat wall that the canopy and nose attach to. Its rear face (f = -3.5) attaches to the tail transition.

### Engine cowling (on the roof)
- A narrow hump **5 long x ~0.5 tall x 1.0 wide**: f -3.0 to 2.0, w +-0.5. Its top is at h = 3.25 at the front 3.75 blocks, and ramps down from h 3.25 at f = -1.75 to h 3.0 at its rear end (f = -3.0). It sits on the deck.
- The rotor mast stands on it.

### Sponsons (side engine pods)
- Two boxes, one each side, **3.25 long x 0.75 tall x 0.75 wide**: f -3.25 to 0, h 2.25 to 3.0, w from 0.75 to 1.5 (they touch the cabin's side, w = 0.75, and extend out to 1.5).
- They are the darkest olive parts. They look like engine intakes bolted onto the shoulders, level with the cabin roof.

### Belly plate
- A thin flat plate under the cabin: **2.25 long x 0.25 tall x 1.0 wide**, f -2.75 to -0.5, h 0.5 to 0.75. Below the cabin, hugging it.

### Nose tub (lower cockpit)
- A box **4.5 long x 0.75 tall x 2.0 wide**: f -0.25 to 4.25, h 0.5 to 1.25, w +-1.0. It is **wider than the cabin** (2.0 versus 1.5).
- Both ends are chamfered by a quarter block: at its two ends it narrows to 1.5 wide. It is the helicopter's belly/chin.
- Above its front half, a sill (the bottom of the canopy) runs f 2.0 to 4.5, h 1.25 to 1.75, w +-0.75, and the nose bridge rises from h 1.75 at f 2.0 to h 2.0 at f 4.5, where the cone starts.

### Nose cone
- A **tapered frustum 2.0 long**: its base at f = 4.5 is 1.5 wide x 1.5 tall (w +-0.75, h 0.5 to 2.0); its tip at f = 6.5 is 0.5 wide x 0.5 tall (w +-0.25, h 1.0 to 1.5). It narrows evenly on all four sides to a blunt flat tip.
- It is the foremost part (f = 6.5). It is NOT a sharp point; it ends in a 0.5-block square.

### Canopy (cockpit glazing)
- A glass wedge **2.5 long x up to 1.125 tall x 1.25 wide**: f 2.0 to 4.5, w +-0.625. Its base is the sill (h 1.75). The **flat top** is at h = 2.875 for the rear 1.25 blocks (f 2.0 to 3.25), then it **slopes down** to h = 1.875 at f = 4.5, where it meets the nose bridge.
- The two sides are vertical glass panels. The slope and the top are glass, too. The roof is a hair lower than the deck (h 3.0), so the canopy appears to rise out of the cabin's front wall.
- Glass is a pale blue-gray, slightly transparent-looking, but is drawn opaque (see section 5). It is NOT a bubble: all its faces are flat.

### Tail transition
- A box **1.5 long x 1.5 tall x 1.5 wide**: f -5.0 to -3.5, h 1.0 to 2.5, w +-0.75. It joins the back of the cabin to the front of the boom. Narrower than the cabin at the top because the cabin is taller.

### Tail boom
- A bar **5.75 long x 1.0 wide**: f -10.75 to -5.0, w +-0.5. The top is flat at h = 2.25. The bottom is at h 1.25 for the front, rising to h 1.5 at the rear end: so the boom is 1.0 tall at the front and 0.75 tall at the rear.
- On top of the boom a **spine** runs its full length: 0.5 wide, 0.25 tall (h 2.25 to 2.5), continuing forward over the transition (h 2.5 to 2.75, f -5.25 to -3.5).

### Tail fin (vertical stabilizer)
- A thin swept plate **0.25 thick** (w +-0.125) standing on the boom spine at its rear end. At its base (h = 2.5) it spans f -10.75 to -9.0 (1.75 blocks long). At its top (h = 5.0) it spans f -11.75 to -10.75 (1.0 block long). Its leading edge slopes back strongly (from f -9.0 at the base to f -10.75 at the tip), and its trailing edge slopes back less (from f -10.75 at the base to f -11.75 at the tip), so the fin narrows as it rises. The base also has a small foot, 0.5 wide, that overlaps the boom's spine.
- The fin tip, h = 5.0, is the **highest point** of the whole model.
- **There is no tail rotor in the original model.** The user brief asks about one: the original has none. The builder may add a small tail rotor (a 2-block disc or two crossed blades on the fin's left side at h ~3.5) as a deliberate, labeled embellishment, but it is not a feature of the look-alike and must not be required.

### Tailplane (horizontal stabilizer)
- A flat plate **0.75 long (chord) x 0.25 thick**, f -11.0 to -10.25, h 1.75 to 2.0, extending from w = 0.25 to w = 2.25 on each side (span 4.5 blocks overall). The inner 0.25 of each half overlaps the boom. It sits near the rear end of the boom, below the fin base.

### Stub wings
- Two flat plates **1.25 long (chord) x 0.25 thick**, f -0.75 to 0.5, h 1.5 to 1.75, extending from w = 0.75 (the cabin side) to w = 3.0. Total wingspan 6.0 blocks.
- Rectangular, no sweep, no taper. They attach to the cabin's flank at mid-length.

### Pylons and rocket pods
- Under each wing at w = 1.75 to 2.0 (a pylon **0.75 long x 0.25 tall x 0.25 wide**, f -0.5 to 0.25, h 1.25 to 1.5) hangs a **pod** **2.0 long x 0.75 tall x 0.75 wide**, f -1.0 to 1.0, h 0.5 to 1.25, w 1.5 to 2.25. The pod bottom is level with the nose tub's bottom (h = 0.5).
- Pods are plain rectangular tubes. The original has no visible rocket tubes, just boxes with texture detail.

### Rotor group (spins)
- **Mast:** a gray post **0.7 x 0.7**, h 3.25 to 4.0 (0.75 tall), on the engine cowling, at f = 0, w = 0.
- **Hub plate:** a flat square slab **2.1 x 0.5 x 2.1** (w +-1.05), h 4.0 to 4.5, centered on the mast.
- **Blades:** four flat strips at h = 4.0 (the bottom of the hub plate), each **9.25 blocks long x 0.75 wide** (measured from 0.5 blocks off the axis to 9.25 from it) and with **no thickness** (a single flat quad drawn both sides). They point along the airframe's forward, backward, left and right directions (a "+"), i.e. 90 degrees apart. They have no twist or pitch.
- All four are the same near-black rubber color.
- Total diameter 18.5 blocks.

### Chin gun (pivoting group)
- **Mount block:** a box **1.0 long x 0.5 tall x 0.5 wide**: f 3.5 to 4.5, h 0 to 0.5, w +-0.25. It hangs under the nose tub (the tub's bottom is h = 0.5) at its front end, centered at f = 4.0.
- **Barrel:** a box **1.5 long x ~0.35 tall x ~0.35 wide** (24 x 5.6 x 5.6 units): f 4.5 to 6.0, h 0.075 to 0.425, centered on the centerline. It protrudes forward from the mount's front face. It may be round if the builder prefers.
- **Pivot:** at f = 4.0, **h = -0.25** (a quarter block below the mount's bottom), w = 0. The whole gun group (mount and barrel) rotates about this point.
- A tiny flat plane (1 x 0.5) at h = 0.5 under the nose, at f 3.5 to 4.5, marks the gun's cover plate; it is not needed.

## 4. Silhouettes

- **Side:** a long, low wedge. The nose points forward and slightly low; the canopy rises behind it to the cabin roof; the cabin and cowling form a block under the mast; the boom narrows backward and ends in a swept fin that rises above everything. The rotor is a thin horizontal line well above the cabin at h = 4.0 (0.75 above the cowling). The wing and pod are a short bar and a block under the cabin center.
- **Front or back:** a narrow vertical body (cabin 1.5 wide, nose tub 2.0 wide) with a **horizontal wing 6.0 across**, a pod hanging under each wing end area, and the rotor blades as a thin line 18.5 across, far wider than the body. The fin shows as a thin vertical line above the body when seen from behind.
- **Top:** a plus-shaped rotor (two crossed strips) over a slim dart: cabin 1.5 wide, nose cone narrowing to 0.5, a boom 1.0 wide, a 6-block wing across the middle and a 4.5-block tailplane at the rear. The rotor covers the whole fuselage except the nose and tail.
- **Three-quarter:** reads as a gunship: glass wedge up front, pods and wing, engines on the shoulders, a spindly tail.
- Negative space: lots of open space under the boom and between the wing and boom; the boom is a thin bar.

## 5. Materials and palette

One 256 x 256 opaque texture atlas in the original. There are no cutouts: the glass and rotor are painted, not transparent. The builder can make its own layout and size.

| Part | Base | Highlight | Shadow |
|---|---|---|---|
| General hull (cabin, nose, wings, boom, fin, tailplane, belly) | #3B4127 | #4A5036 | #2B2F1C |
| Cabin and fin tone variants | #373D23 to #3E442A | #454B31 | #32381E |
| Spine, boom top | #40462C | #4B5137 | #373D23 |
| Rocket pods | #42482E | #464C32 | #3B4127 |
| Sponsons and engine cowling | #363B26 | #3B4127 | #2B2F1C |
| Canopy glass | #6B8896 | #95BCE7 | #375E89 |
| Rotor blades | #171714 | #21221D | #0A0A09 |
| Hub plate | #26281F | #3D4032 | #0B0B09 |
| Mast | #575757 | #6D6D6D | #535353 |
| Chin gun mount and barrel | #212121 | #242424 | #1E1E1E |

- Shading style: a soft per-pixel mottle in the olive, about 3 to 4 brightness steps between #2F351B and #4B5137, with occasional darker panel lines (about #303030) between plates. No stripes, no insignia, no text.
- The glass is a vertical gradient from a light blue (#95BCE7) at the top to a deeper blue (#375E89) at the bottom, with a gray cast. It reads as reflective glass.
- The atlas has a few pure-red pixels (#BE0000 to #FF0000, under 100 in all) that no part showed up as using. Ignore them.
- No glow, no emissive areas. The model does not blend.
- No variants: one texture.

## 6. In-game presentation

- The renderer draws the mesh itself, with no vanilla living-entity pipeline: **no name tag, no red hurt flash and no vanilla death topple.** A **boss health bar** is shown whenever it is rendered.
- Drawn at 0.25 of the OBJ's unit size; this is what makes the sizes above.
- Shadow radius 5.0 (practically invisible at its cruising altitude).
- Cruising altitude is about 25 blocks above the ground, so it is seen mostly from below: **the underside matters** (belly, nose tub, pods, gun and the rotor's underside).
- It does not tilt, bank or pitch with movement. It turns on the spot to face its target or its direction of travel.
- No armor layers, no held items.

## 7. Motion

- **Rotor:** spins about the vertical axis through the mast (f = 0, w = 0). Speed is **24 degrees per tick**: a full turn every 15 ticks, 1.33 turns per second (480 degrees per second). The direction is counterclockwise as seen from above. It spins at all times while alive, whether flying or attacking.
- **Airframe:** otherwise rigid. It turns about the vertical axis through the rotor mast (not through the fuselage center) to face the target when attacking, or its direction of travel when not. No bobbing or tilt.
- **Chin gun:** the gun group is **always pitched 25 degrees nose-down** about its pivot (the barrel tip then sits at about h = -0.6, roughly f 6.0). In addition it yaws about the vertical axis through its pivot by the difference between the head yaw and body yaw. In practice the AI keeps head and body yaw almost the same, so the gun mostly points straight ahead along the body, 25 degrees down. It does not pitch to follow the target. The gun group moves rigidly as one piece (mount and barrel).
- **Firing:** no recoil or barrel spin is animated. Shots and rockets are spawned by the entity, not by the model. Bullets appear about 8 blocks ahead; a muzzle flash appears about 4 blocks ahead of the center at about 0.25 blocks below the feet.
- **Death (100 ticks):** let t be the death time in ticks (0 to 100) and p = (t / 100) squared.
  - The whole airframe **sinks** by 10 x p blocks (up to 10 blocks at the end), so it falls slowly at first and fast at the end. The hitbox itself does not move.
  - It **spins** about the vertical axis through the rotor mast by 720 x p degrees, i.e. two full turns by the end, speeding up from rest to 14.4 degrees per tick at the very end. Since the axis is the mast, the tail swings around in a wide circle (up to 11.75 blocks of radius).
  - The rotor is drawn in the same falling and spinning frame and keeps spinning on its own but **at half speed (12 degrees per tick)**.
  - The chin gun is drawn **level and un-pitched** (its rest pose) during the death, with no aiming.
  - Effects, spawned by the entity and not the model: a puff of large smoke every 5 ticks, three fireballs shooting up and outward every 6 ticks near the falling hull, and a final burst of 10 huge explosions plus smoke, then the entity vanishes at tick 100.

## 8. What to avoid

- **A short, stubby fuselage.** It is very long and thin; the tail boom is nearly as long as the cabin.
- **A rotor mounted on the fuselage center.** The rotor axis is **0.75 block forward of the cabin's center**, over the cabin's front half and the cowling, with the cabin's front wall 2.0 ahead of it.
- **Rotor blades that are thick or twisted.** They are flat, zero-thickness strips; with the black color they read as a thin cross.
- **Two blades or six.** Exactly four, in a plus.
- **A rounded bubble canopy or round fuselage.** All shapes are flat-faced boxes and wedges.
- **Pitching the gun to the target.** It is fixed 25 degrees down.
- **Banking or tilting in flight,** or a model-driven recoil.
- **A transparent glass.** The glass is painted opaque.
- **Making the tail rotor mandatory.** The original has none.
- **Making the hitbox match the model.** The hitbox is 10 x 4 and the model is much larger.

## 9. Acceptance checklist

- [ ] Overall length 18.25 blocks from nose tip to fin tip; rotor diameter 18.5 blocks.
- [ ] Cabin 5.5 x 2.0 x 1.5 blocks, h 0.75 to 2.75.
- [ ] Rotor axis 2.0 blocks behind the cabin's front wall (cabin runs f -3.5 to +2.0 around it); the rotor does not sit at the cabin's center.
- [ ] Four flat black blades in a plus, each 9.25 blocks from the axis, 0.75 wide, at h = 4.0, on a mast (h 3.25 to 4.0) and a 2.1 x 2.1 hub plate.
- [ ] Nose cone narrows from 1.5 x 1.5 to 0.5 x 0.5 over 2 blocks; tip at f = 6.5.
- [ ] Glass canopy 2.5 long, 1.25 wide, rear flat top at h 2.875, sloping to h 1.875 at the front.
- [ ] Tail boom 5.75 long and 1.0 wide, with a spine on top.
- [ ] Swept fin rising to h = 5.0, the highest point of the model.
- [ ] Tailplane 4.5 blocks across.
- [ ] Stub wings 6.0 blocks across at h 1.5 to 1.75, with a pod under each (2.0 x 0.75 x 0.75).
- [ ] Chin gun under the nose, pivoting at f = 4.0, h = -0.25, pitched 25 degrees nose-down.
- [ ] Rotor spins 24 degrees per tick (one turn per 15 ticks).
- [ ] Hull olive-drab (#3B4127 family), glass pale blue-gray, rotor and gun near-black.
- [ ] Death: sink 10 blocks and spin 720 degrees on a quadratic curve over 100 ticks, rotor at half speed, gun level.
- [ ] No tail rotor required.

## 10. Unsure

- **Right and left.** The original model is symmetric except for the texture, so I did not need to decide which side is which.
- **Rotor direction.** I read the sign of the original's rotation from a right-hand rule in its own Y-up frame. Counterclockwise seen from above is my best reading; at 24 degrees per tick the eye cannot tell, so either direction is acceptable.
- **Gun yaw.** I could not settle whether the head yaw stays equal to the body yaw while attacking. If it does, the gun never yaws; if not, it swivels a little. The builder may make it swivel freely.
- **Barrel cross-section.** The original's barrel is a box. I do not know how it was meant to look; round is fine.
- **Glass.** Whether the canopy was meant to be see-through is unknown; the original draws it opaque.
- **Fine shapes.** Small bevels (the nose tub's quarter-block chamfers, the fin's foot) are within about a quarter block of what I describe.
- **Shadow.** The renderer's shadow size is 5.0, but the engine only draws it when close to the ground.
