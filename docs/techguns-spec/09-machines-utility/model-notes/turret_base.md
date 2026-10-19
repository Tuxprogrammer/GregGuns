# Turret base: clean-room model notes

Design notes for rebuilding the Techguns **Turret Base** block look-alike in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer and textures. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related: [turret_head](turret_head.md) (the part that sits on top, as an entity).

**Units and axes.** One unit = 1/16 block. **Y points up.** Origin = the block's bottom-north-west corner; the block spans 0..16 on each axis. The model is **symmetric on all four sides** (four identical faces), so it has no front and needs no facing. Converting from the original: it is built Y-down; its renderer moves it to the block center, one and a half blocks up, and flips it 180 degrees about the Z axis, with no scale. The result is the same as the numbers below. Rounded to the nearest half unit unless it matters.

## 1. Gestalt

A **compact armored cube** with a hollow, industrial look. At a glance you read: a squat, light-gray steel box with a **dark steel frame** (corner posts and a bottom rail), a **dark recessed square port** in the middle of every side face, and a flat **top plate that is narrower than the box**, braced to the corners by four diagonal struts, so the top looks like an X-braced pedestal with a dark hole in its middle. It is where the turret head plugs in.

Defining features in order of visual importance:

1. **Four tall dark corner posts** framing a lighter, speckled gray cube body that sits slightly inset from them.
2. **A square dark socket on each side face**, with a smaller square inside it (a port with a plug).
3. **A narrow raised top plate** (half the block wide) with a dark, round-looking hole in its middle, joined to the posts by four diagonal struts.
4. **A dark rail along the bottom** of each side.
5. Five armor tiers shown only by **texture recolor** (light steel down to near-black), never by geometry.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Footprint | 16 x 16 (full block) |
| Height | 16 (posts reach 15, the top plate reaches 16) |
| Body cube | 14 x 14 x 14, on the floor, centered, 1 unit inset from each block edge |
| Top plate | 8 x 8 footprint, 2 tall, at the very top (Y 14 to 16), centered |
| Corner posts | 2 x 2 footprint, 15 tall (Y 0 to 15) |
| Side ports | 6 x 6 face, 1 deep, centered on each side |

The top silhouette is stepped: posts at 15, plate at 16 in the center.

## 3. Part by part

All parts are boxes (the struts are boxes turned 45 degrees about the vertical axis). X and Z are measured from the block's north-west corner.

### Body cube
- 14 x 14 x 14, X 1 to 15, Z 1 to 15, **Y 0 to 14**. Its top is exactly where the top plate begins.
- No windows or cut-outs. The faces are flat speckled steel.
- Touches: the corner posts overlap its edges; the plate sits on its top.

### Corner posts
- Four posts, each **2 x 2 x 15**, in the four corners: X and Z ranges 0..2 and 14..16, **Y 0 to 15**. Flush with the block's outer edges.
- They overlap the body cube's corner edges, so the cube's faces are 1 unit recessed behind the post faces.
- Post tops are 1 below the top plate's top.

### Top plate
- **8 x 2 x 8**, X 4 to 12, Z 4 to 12, **Y 14 to 16**, centered.
- Its top face is a dark, dished look (section 5); the model has no actual hole, the hole is painted.
- It is what the head's foot plate stands on.

### Diagonal struts
- Four flat bars, each **6 long, 1 tall, 2 wide**, at Y 14 to 15 (the plate's lower half). Each starts near a post's inner corner (about 1 unit in from the block corner on both axes) and runs **diagonally inward at 45 degrees toward the block center**. Its far end reaches about 3 units from the center on each axis, inside the plate's footprint (so it overlaps the plate).
- Seen from above they form an **X** from the four corners to the plate. From the side they read as short dark bars joining the posts to the plate edge.
- Same dark steel as the posts.

### Bottom rails
- Four rails, each **1 thick, 2 tall, 12 long**, one per side face, at **Y 0 to 2**, flush with the outer faces (the same plane as the posts' outer faces), spanning post to post (2 to 14 along the side).
- They look like a dark kick plate along the bottom edge.

### Side ports (sockets)
- Four plates, each **1 thick, 6 x 6**, one per side, centered (5 to 11 along the side, **Y 5 to 11**), flush with the posts' outer plane (so 1 proud of the body cube's face).
- Painted as a dark square with a smaller inner square (section 5). They are not holes.

## 4. Silhouette checks

- **Side, front, back:** an almost square outline, 16 wide by 16 tall. The posts are dark verticals at both edges; the bottom rail is a dark band; between them a lighter 12-wide field with the dark 6-wide port in the middle. The top edge has a **step**: posts end at 15, the plate pokes up to 16 across the middle 8 units.
- **Top:** a square with four dark corner posts, four dark diagonal struts forming an X, and in the center the 8 x 8 plate with a dark round-looking hole. Light speckled steel shows between the struts (the body cube's top, as four triangles).
- **Bottom:** a flat speckled gray square with a dark frame border (posts and rails).
- **Corner view:** a cube with dark vertical corners, a port on each face, and a raised dark-centered pedestal on top.

## 5. Materials and palette

Material: machined steel with fine speckle noise on the large faces and flat dark metal on the frame. Five armor tiers recolor everything; the geometry never changes.

| Tier (armor value) | Body | Frame (posts, rails, struts) | Notes |
|---|---|---|---|
| 0 (none) | #949494 base, #A4A4A4 and #ACACAC highlights, #8C8C8C shade | #242424 to #2C2C2C | Light gray steel, near-black frame. |
| 5 (iron) | #A4A4A4 to #BCBCBC, brighter | #444444 to #4C4C4C | Shiniest tier, lighter frame. |
| 10 (steel) | #848484 to #9C9C9C | #1C1C1C to #343434 | Darker body, very dark frame. |
| 15 (obsidian steel) | purple-gray, #443444 to #54445C | #1C141C to #2C2C2C | Violet tint. |
| 20 (top tier) | near-black, #0C0C0C to #242424 | #040404 to #141414 | Almost all black, faint relief only. |

Shading style: speckled per-pixel noise of about three gray levels on the body; flat uniform dark on the frame. The port is a very dark square (about #101010 to #1C1C1C) with a slightly lighter inner square (about #2C2C2C to #3C3C3C), reading as a recessed plug. The plate top is painted as a radial fade: black in the middle (about #000000) lightening to dark gray (about #3C3C3C) at the rim, which makes the "hole". No wear, stencils or text.

## 6. In-game presentation

- **Placed block:** drawn with this model, no rotation by facing. The texture follows the turret's installed armor plate (none, iron, steel, obsidian steel, top tier).
- **Item (inventory, hand, ground):** the same model, about 0.9 scale in first person, lowered and offset so it sits in the hand; the icon is the 3D model in a three-quarter view, not a sprite.
- The head (a separate entity) stands on the plate; the plate's top (Y 16) is the head's floor.

## 7. Motion

None. The block is static. The head rotates above it as a separate entity (see turret_head).

## 8. What our earlier attempts got wrong

GregGuns has only a **placeholder cube** for the turret base so far (one full-block box with the texture sheet); there is no real model attempt to compare. The builder should avoid: a plain full-block cube (the original has recessed faces and frame posts), a flat top (it has the raised 8 x 8 plate and the X struts), and a light frame (the posts and rails are the darkest part).

## 9. Acceptance checklist

- [ ] Overall 16 x 16 x 16, fits the block; the same on all four sides.
- [ ] Body cube is 14 x 14 x 14, inset 1 from each block edge, Y 0 to 14.
- [ ] Four corner posts, 2 x 2, up to Y 15, flush with the block edges.
- [ ] Top plate is 8 x 8 x 2 at Y 14 to 16, centered, with a painted dark center.
- [ ] Four diagonal struts at 45 degrees run from the corners to the plate, forming an X from above.
- [ ] Four bottom rails, 1 thick, 2 tall, 12 long, flush with the posts.
- [ ] Four ports, 6 x 6, 1 proud of the body face, centered; a dark square with an inner square.
- [ ] Side silhouette shows the step: posts at 15, plate at 16.
- [ ] Frame is darker than the body in every tier.
- [ ] Five tier textures map onto one geometry; only color changes.
- [ ] No part floats; every box overlaps a neighbor.

## 10. Unsure

- **Strut end points.** They come from a rotated box; the exact ends at the plate are my reading, within about 1 unit. The rotation direction was inferred from the renders (X from above).
- **Plate top relief.** I believe the "hole" is painted shading only, with no depression.
- **Tier names.** The five tiers follow the original's armor values 0/5/10/15/20; I did not confirm which plate item maps to which.
- **Wall mounting.** I did not check whether the block is ever placed on anything but the floor.
