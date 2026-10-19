# 9mm Pistol: clean-room model notes

Design notes for rebuilding the Techguns **9mm Pistol** look-alike in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [ModelPistol3](../gun-models.md#modelpistol3-pistol-declared-6432-used-with-a-12864-png-family-z-9-boxes-2-parts), [pistol sheet](../gun-sheets.md#pistol-pistol), [rendering framework](../rendering-framework.md), [Techguns pistol stats](../../02-guns/gun-catalog.md#table-a-identity-and-ammunition).

**Units and axes used in this document.** One unit = one texel at the native 64x32 scale = 1/16 block. Muzzle toward **+X**. **Y points down** (Minecraft model convention), with **Y = 0 at the top face of the slide**. **Z = 0 is the gun's mid-plane**; sizes across Z are given as total width, centered. **X = 0 is the rear face of the slide.** Rounded to the nearest half unit unless it matters. (The original is built along its own Z axis with the muzzle toward -Z; these notes have already converted that.)

## 1. Gestalt

A slim, flat-black, **Glock-style polymer service pistol**, drawn with Minecraft-scale boxes. At a glance you read: a long, straight, rectangular **slide** as the lightest part of the gun; a **black band** under it (the frame rail); a **raked, slim grip** dropping from the rear; a **square-cornered trigger guard** (a vertical front post and a flat bottom bar) with a small **slanted trigger** floating in the opening; **two tiny sight nubs** on the slide's top, one near each end; and a **short, thin barrel stub** poking out of the slide's front. The attitude is modern, plain and utilitarian. The whole gun is dark: the lightest thing on it is a charcoal gray slide, and everything else is near-black. It is small and thin: only 3 units across at its widest.

Defining features in order of visual importance:

1. **A long, boxy slide, 14 units long and only 2 tall, with a black rail strip under it.** The two-tone side (charcoal over black) is the signature look.
2. **A slim grip raked back about 12 degrees**, narrower than the slide, with a checkered, near-black texture.
3. **An open, square-cornered trigger guard** with a slanted trigger inside it.
4. **Two small sight cubes** on top of the slide, one near the rear and one near the front.
5. **A small diamond barrel stub** just past the slide's front face, visible from the front as a dark diamond.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total length (slide rear to barrel tip) | 15 |
| Slide and rail length | 14 (flush with each other at both ends) |
| Total height (front-sight top to grip bottom) | about 10.4 |
| Total thickness (slide and rail) | 3 (the grip is 2; guard, trigger and sights are 1) |
| Length : height : thickness | about 5 : 3.5 : 1 |
| Slide plus rail block | 14 long, 3 tall, 3 wide: about 4.7 : 1 : 1 |
| Slide alone | 14 long, 2 tall, 3 wide |
| Grip length (along its own axis) | 7, hanging about 6.8 below the rail's underside |
| Grip thickness (across Z) | 2, a third narrower than the slide |
| Barrel stub beyond the slide | 1 |

How the pieces sit along the length, from the rear: the slide's back face is the rearmost plane. The grip's lowest rear corner is about **0.5 in front of that plane** and its upper rear edge is about **2 in front of it**, so the grip does not stick out behind the slide. The front sight is about 1.5 behind the slide's front face and the rear sight about 1.5 in front of the slide's rear face. The guard's front post stands at X 7 to 8, which is just past the middle of the gun. Ahead of that the underside is flat rail for another 6 units.

The **top line is flat**: the slide's top is one long unbroken plane, and only the two sight cubes rise above it, by about half a unit.

## 3. Part by part

All parts are boxes unless noted. Z widths are centered on the mid-plane. "Touches" means faces in contact or overlapping.

### Slide
- A plain box, **14 long (X 0 to 14), 2 tall (Y 0 to 2), 3 wide**. No taper, no cut-outs, no ejection port, no rear notch, no chamfer.
- It is the **lightest-colored part** of the gun. Its top face is lighter still.
- Surface detail is painted only: a group of fine vertical **serration lines** on the rearmost ~4 units of each side, and a very faint darkening on the last ~2 units at the front end (section 5).
- Touches: sits directly on the rail (flush on all four edges: same length, same width), carries both sights, and the barrel stub emerges from its front face.

### Frame rail
- A flat bar directly under the slide: **14 long, 1 tall (Y 2 to 3), 3 wide**, exactly the slide's footprint. Together with the slide it forms a clean 14 x 3 x 3 block.
- Nearly black, much darker than the slide. This is what produces the two-tone side view.
- The grip's top, the trigger and the guard's front post all attach to or under this rail.

### Barrel stub
- A **1 x 1 square bar, 6 long, turned 45 degrees about its length** so that seen end-on it is a **diamond**, about 1.4 across in both directions.
- Runs from X 9 (buried inside the slide) to X 15, so only the front **1 unit** protrudes past the slide's front face.
- Axis centered on Z = 0, about **1 unit below the slide's top** (axis at Y about 1; the diamond's top corner is about 0.25 below the slide top and the bottom corner about 0.35 above the slide's bottom).
- It is a lighter, neutral gray than the slide, and the end face is dark (a bore). It does **not** move when the slide moves.
- No threads, no ribs, no suppressor look, no muzzle crown.

### Rear sight
- A **1 x 1 x 1 cube**, centered on Z = 0, at **X 1.5 to 2.5**, sitting on the slide's top. It rises about 0.5 above the top face and is sunk 0.5 into the slide.
- Plain: no notch, no outline.

### Front sight
- The same 1 x 1 x 1 cube at **X 11.5 to 12.5**, rising about 0.5 above the slide's top.
- Same color as the rear sight. Both are a dark gray, slightly darker than the slide's side.

### Grip
- A single box, **2 wide (Z), about 3 deep along the gun's axis, 7 long**, tilted **12.5 degrees backward** (the bottom swings toward the rear, pivoting at its top front edge).
- The top front edge sits at X 5, Y 3 (right under the rail). After the tilt, the bottom front corner is at about X 3.5, Y 9.8 and the bottom rear corner at about X 0.5, Y 9.2. The upper rear edge is at about X 2, hidden inside the rail's height. The bottom face therefore slopes slightly upward toward the rear.
- It is a straight slab with no finger grooves, no stepping and no beavertail. It carries a **fine dark checkered or woven texture** on both sides and the front and back faces, and a flat, plain bottom.
- Touches: its top disappears into the rail; the guard's bottom bar meets its front face about 4.5 units from the slide's rear.
- There is **no separate magazine** visible: no baseplate, no step, no stick. The grip alone reads as the grip-with-magazine.

### Trigger guard
- Built from two narrow parts, each **1 wide** (a third of the slide's width, centered):
  - **Front post:** 1 long, 3 tall, at **X 7 to 8**, from the rail's underside (Y 3) down to Y 6.
  - **Bottom bar:** 4 long, 1 tall, at **X 4 to 8, Y 5 to 6**, running from the grip's front face forward to the post. Its front end is flush with the post's front face; its rear end is buried in the grip.
- The opening inside is bounded by the rail (top), the grip (rear), the post (front) and the bar (bottom): about **2.5 long and 2 tall** at its widest.
- Seen from the side the guard is a clear **square-cornered loop** (an upside-down-L plus a bar), not a solid slab, with open background through it. Its sides are narrow (1 wide) so it is much thinner than the grip.
- Color: the same near-black as the rail.

### Trigger
- A small bar **1 x 2 x 1** (1 wide, 2 long, 1 deep), hanging from the rail inside the guard's opening at about **X 5.4 to 6.7**, and tilted about **23 degrees with the bottom swung forward**. The lowest point is about Y 4.7, so there is a clear gap (about 0.3 to 0.7) above the guard's bottom bar.
- Near-black, same as the rail and guard. It reads as a slanted tick mark floating in the opening.

## 4. Silhouette checks

- **Side (the main view):** a long, straight bar (slide, 2 units tall) with a charcoal side over a thin black band (rail), two small dark bumps on top (sights), a tiny stub past the front, and the raked, narrow grip hanging at the rear. In the loop below the rail: front post, bottom bar, the slanted trigger tick in the middle, and open background above the bar. Negative space: the loop of the guard; the long empty stretch under the rail ahead of the guard post (6 units); the wedge between the grip's back and the slide's rear plane (the grip leans away, leaving a small triangle of background below the slide's back end).
- **Top:** a thin, straight rectangle, 3 wide and 14 long, a lighter brown-tinted gray. Two small dark squares on the center line near each end (the sights). A tiny nub at the very front (the barrel stub). The grip is hidden except for a sliver.
- **Front:** a small, narrow shape: a 3-wide, 2-tall charcoal rectangle with a **dark diamond** centered in it (the barrel end), two sight nubs stacked on top (the front one over the rear one), a darker 1-tall band underneath (the rail), and below that the grip as a narrower, dark column 2 wide, about 7 tall, ending in a sloped bottom. The guard post is in front of the grip but hidden in this view (same black).
- **Back:** the slide's back face as a 3 x 2 charcoal block with the rear sight on top, the black 3 x 1 rail strip under it, and a narrower (2-wide) dark column of grip about 7 tall. There is no hammer, no spur and no backstrap detail.
- **Underside:** a long dark 3-wide bar; the guard post and bottom bar are a 1-wide stripe down the center; the grip's bottom is a small 2 x 3 rectangle at the rear; a diamond nub at the front.

## 5. Materials and palette

The original texture is a **flat, dark, charcoal-and-black palette**, with a small amount of faint per-pixel noise. The values below were sampled from its regions.

| Part | Material | Base | Highlight | Shadow / seam |
|---|---|---|---|---|
| Slide sides | dark charcoal steel or polymer | #2E2C2C | #343232 | #282626 |
| Slide top face | same, lit | #413A3A | #494444 | #322E2E |
| Slide front and back faces | same | #282626 | #2E2A2A | |
| Slide serrations | painted ribs on the rearmost 4 units of each side | rib #343232 to #3A3A3A | up to #3A2E2E | groove #222020 to #242222 |
| Frame rail sides | near-black polymer | #141313 | | |
| Frame rail top and bottom faces | near-black, slightly lighter | #191717 | #242222 | #141313 |
| Grip sides, front and back | near-black, checkered | #141313 | #211E1E to #242222 | #171616 / #1B1919 mid-tones |
| Grip top and bottom faces | near-black | #141313 | | |
| Guard (post and bar) and trigger | flat near-black | #141313 | | |
| Barrel stub sides | neutral mid gray | #383838 | | |
| Barrel end face (bore) | dark | #141313 | | |
| Sights | dark gray | #222121 | | |

Shading style:
- **Whole gun:** flat and dark. The overall average is about #252323, nearly black. There are **no gradients, no scratches, no wear, no logos, and no colored accents** (the original has no red or other accent color in any rendered face).
- **Slide:** charcoal with very subtle per-pixel noise (a few levels of difference). The top face is a little lighter and slightly warm (red channel a touch above blue). The edges of the side faces are a half step darker (#282626), which keeps the box crisp. It is the **only part with visible lighter metal**.
- **Serrations:** on both sides, near the rear end: about 4 alternating light and dark thin vertical stripes over roughly 4 units, strongest right under and ahead of the rear sight. Very faint, low-contrast. At the front end there is a similar, fainter patch about 2 units long, mostly just a half-step darker.
- **Rail, guard, trigger:** uniformly flat near-black, no noise. The rail must contrast clearly with the slide (a visible step from charcoal to black), but all three blacks must look like the same material.
- **Grip:** near-black, covered with a **small, soft checkerboard** (alternating 1-texel cells differing by only 3 to 6 levels), with a one-texel darker border around each face. It reads as a textured polymer grip, but only up close.
- **Barrel stub:** a plain, slightly lighter and more neutral gray than the slide with a dark end face.

## 6. In-game presentation

All transforms apply to the model as a whole, in block units after the model's 1/16 scale. The model is first rotated 180 degrees about its length-wise lateral axis (Y-down becomes screen-up).

| View | Orientation and scale | Offset and notes |
|---|---|---|
| First person | yaw 225 degrees about vertical (the gun is turned about 45 degrees off straight, a three-quarter rear view), scale **2.0** | idle offset about (-0.55, -0.45, -0.8) in the scaled frame. You look at the gun's side and back, with the muzzle pointing away and slightly across. The gun is held low and to the right. |
| First person firing | whole gun pitches about -5 degrees at mid-animation and moves back about 0.15 block (frame) | the slide moves back further, about 0.25 block, so relative to the frame it travels about 0.1 block (1.6 units). Details in section 7. |
| First person reloading | tilts about 50 degrees at mid-animation, smoothly up and back | the pivot is near the model origin, about 2 units behind the muzzle, so the grip end swings most. The offset stays at idle. |
| First person aimed | no scope and no zoom overlay | the pistol has no zoom. |
| Third person (held) | yaw 135 degrees, scale **1.5** | offset (0.05, -0.55, 0.2). The grip sits in the hand and the muzzle points forward. |
| Third person aimed | extra offset (0, 0, 0.2) | plus the shared aimed-arm rotation. |
| Ground (dropped) | no scale (1.0) | lowered by 0.35 and shifted 0.3 to center it; lies flat on its side, long axis horizontal. |
| Inventory icon | yaw 90 degrees, scale 1.5 | offset (0.3, -0.5, 0); drawn side-on, muzzle toward the right, filling most of the slot. There is no sprite; the icon is the 3D model. |

**Muzzle flash point:** the front of the barrel stub on its axis (model position X about 15, Y about 1, Z = 0). In first person the flash is a yellow sprite (the standard "muzzle flash 2" sheet) at scale 1.0, drawn at a fixed offset from the idle gun position. In third person the flash is drawn slightly forward of the hand at about (0, 0.05, -0.45).

**Akimbo:** the same model is used twice for the Akimbo Pistols. In first person the second (left) gun is shifted about 1.85 sideways in the scaled frame. Guns alternate shots; only the gun that fires animates. Reload and recoil are identical to the single pistol.

## 7. Motion

- **The model has two groups.** Group A (the frame group): the rail, the grip, the guard parts, the trigger and the **barrel stub**. Group B (the slide group): the slide and both sight cubes. Group B moves further than group A when firing.
- **First-person firing (3 ticks total):** a triangle-shaped curve: out and back over 3 ticks, peak at about 1.5 ticks. At the peak the whole gun pitches about **5 degrees** (pivot near the muzzle end) and shifts back by about **0.15 block** (frame group), while the slide group shifts back by about **0.25 block**. So the slide slides back about **1.6 units (0.1 block) relative to the frame**, and returns. Because the barrel stub is in the frame group, at the peak it shows about 2.6 units past the slide instead of 1. The slide moves smoothly back and forward, with no hold at the rear.
- **Third-person recoil:** the gun pitches up about **15 degrees** around the wrist (a point about 0.9 block behind the model origin) in roughly the first eighth of the animation, then settles back to zero by about the halfway point. The slide group also slides back linearly by up to about **0.15 block (2.4 units)** across the first half and then **snaps forward in one frame**; there is no smooth return in third person.
- **Reload:** 35 ticks (1.75 s). First person: the gun tilts about 50 degrees in a V shape (linear up to mid, linear back) while held near the idle spot. Third person: it tilts about 40 degrees about the wrist at mid-animation, then returns. **No part (slide, trigger) moves during a reload.** There is no slide lock-back and no visible magazine drop.
- **Fire rate:** 5 shots per second (4 tick minimum); recoil animation 3 ticks, flash 5 ticks, 18-round magazine.
- **Trigger and sights never move.** The trigger does not pull back during fire.
- **For the builder:** keep the slide group (slide plus both sights) and the frame group (everything else) as two separate named parts so the firing code can move the slide group about 1.6 units rearward relative to the frame group. The rest pose must match this document.

## 8. What our earlier attempts got wrong

(From the fidelity review of our current pistol, restated.)

- **Light-gray speckle instead of near-black.** Our slide atlas is a high-contrast speckle of light and dark squares with white dots, so it reads as camo or static. The original is flat charcoal with a near-black rail, and nothing on it is brighter than #494444. Make the metal dark, calm and low-contrast; reserve light pixels for faint edge highlights and serration ribs.
- **The trigger guard is a solid slab.** Ours is a 12x6x3 box under the frame, and it is also thinner than the frame, leaving a step. The original is a **loop**: a narrow front post and a narrow bottom bar with an open opening of about 2.5 x 2, and a small slanted trigger inside it.
- **A buried magazine.** Ours has a magazine box inside the grip volume so only a few texels show. The original has **no magazine part at all**: just a slim raked grip with a checkered texture.
- **A threaded suppressor-like barrel.** Ours pokes about 8 texels past the slide with ribs. The original's barrel is a tiny **1-unit diamond stub**, not ribbed, with a dark bore.
- **A wrong sight.** Ours has a single raised block near the muzzle. The original has **two** 1-unit cubes: one near each end of the slide.
- **Chunky proportions.** Ours is a thick, tall block. The original's slide is only **2 tall and 3 wide against 14 long** (with a 1-tall rail), a flat bar. Keep it thin and long.
- **Fat grip.** Keep the grip at about two thirds of the slide's width (2 against 3), and raked about 12.5 degrees.
- Do not add things the original lacks: no rear notch, no ejection port, no underlug or rail, no hammer, no magazine baseplate, no threads, no colored accents.

## 9. Acceptance checklist

- [ ] Length : height : thickness is within about 10 percent of 5 : 3.5 : 1 (15 : 10.4 : 3).
- [ ] The slide plus rail is a 14 x 3 x 3 block, flush at both ends, with the slide the upper 2 units and the rail the lower 1 unit.
- [ ] The slide is clearly **lighter** (charcoal, about #2E2C2C) than the rail (near-black, about #141313); the side view shows two tones.
- [ ] The barrel is a short diamond-section stub protruding about 1 unit from the slide's front face, with a dark bore end and no ribs.
- [ ] There are exactly two sight cubes (about 1 x 1 x 1), one near each end of the slide, each rising about 0.5 above the top, with the front one about 1.5 behind the front face.
- [ ] The grip is a single 2-wide slab tilted about 12.5 degrees backward, reaching about 6.8 below the rail; its bottom rear corner is within about 1 unit of the slide's rear plane (not behind it).
- [ ] The grip has a fine dark checkered texture and **no separate magazine** shows.
- [ ] The trigger guard is an open square-cornered loop made of a 1-wide front post and a 1-wide bottom bar, with a 2-tall opening you can see through from the side.
- [ ] A small slanted trigger (bottom tilted forward, about 23 degrees) hangs inside the opening without touching the bar.
- [ ] The guard's front post is at about X 7 to 8 (just past mid-length).
- [ ] The palette is almost black overall (average about #252323); the lightest area is the slide's top face (about #494444). No bright, white or colored pixels anywhere.
- [ ] The slide has faint vertical serration ribs on the rearmost ~4 units of both sides.
- [ ] The slide and both sights are one movable part, separate from the frame; the barrel stub stays with the frame.
- [ ] The side silhouette matches section 4: flat top, two nubs, loop guard with a clear opening, narrow raked grip.
- [ ] No part floats; every box overlaps or touches a neighbor (the trigger attaches to the rail).

## 10. Unsure

- **First-person framing.** The numbers come from formulas read, not seen in game. The scale of 2.0 on a 15-unit gun makes it large in view. Our earlier version halved the scale because the larger gun ran off screen, but with the original's slim, foreshortened shape it may fit. Treat the offsets as approximate and judge visually.
- **Direction of the reload and recoil tilt.** The spec says "muzzle up", but the first-person pivot is near the muzzle, not the wrist, so the grip end swings more than the muzzle. Verify visually.
- **Serration placement.** Both side faces carry the pattern, but I could not tell exactly which end each side shows it on from the low-resolution render. The rear is by far the strongest, and a faint front patch is optional.
- **Grip checker.** The pattern is only 3 to 6 levels off the base, so it is almost invisible at normal size. It is fine to treat it as a very subtle diagonal weave.
- **Barrel stub color.** It reads slightly lighter than the slide in renders; a builder who matches it to the slide would lose very little.
- **Texture red.** An earlier spec note mentions dark-red accents in the texture. I found no red pixels in any region mapped to the model, so I treat the palette as pure neutral or warm charcoal.
