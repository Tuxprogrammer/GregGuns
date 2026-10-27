# Handheld tools

- **ICBM baseline:** [handheld-tools](../../icbm-spec/05-detection-defense/handheld-tools.md) (behavior only; the GregGuns build is clean-room)
- **Decisions:** [D41](../DECISIONS.md), [D69](../DECISIONS.md), [D93](../DECISIONS.md), [D118 to D123](../DECISIONS.md)
- **Pack mods:** GT5-Unofficial (`5.09.54.205`): Assembler recipe map, recycling data, `ItemList` emitters and sensors
- **Roadmap:** [I4](../../ROADMAP-ICBM.md#i4--detection-and-defense)

## What changes from ICBM

ICBM ships four handheld items. GregGuns keeps three and renames none of their jobs.

| ICBM item | GregGuns | Why |
|---|---|---|
| Radar gun (Laser Range Finder) | Kept as one HV item (D93), [below](#radar-gun) | It feeds the radar and battery targets |
| Remote detonator | Kept as three tiers (HV, EV, IV), rebuilt as a **link-and-fuse** tool (below) | ICBM's radio bus is gone (D13); a global broadcast with no range, no link and no owner check cannot be a safe tool |
| Laser designator | **Dropped.** Its job (sending a target to a launcher) is the GT data stick plus the radar gun's target (D13) | Nothing left for it to do |
| Defuse kit | Kept as the Defuse Kit with durability | See below |

All items are single stacks with the GregGuns tooltip (D41): tier line, one summary line, the numbers behind Shift, then "Added by". Icons are the NTM art on `icbm/i4-guides` (`remote_detonator`, `defuser`).

## Radar gun

D93; it pairs with the [radar](radar.md).

- **Kept:** yes. A laser range finder, HV item, one per stack.
- **Use:** right-click aims up to 200 blocks and stores the X and Z of the block hit, in the same tag a GT data stick carries for a target (`TargetStick`). The trace runs on the server from the player's eyes, so a client cannot send a position (fixes ICBM-05-07 and ICBM-05-12). Sneak and right-click clears it.
- **Launch Control:** right-clicking a Launch Control with a gun that holds a target copies it there (same dimension, within use distance).
- **Recipe:** Assembler, HV: sensor, emitter, Advanced circuit, 2 stainless plates.
- **Tests:** scenario `icbm.defense.radar_gun`.

## Remote detonator

Three items: `remote_detonator_hv`, `_ev`, `_iv` (registry names), one per tier.

### Numbers

| Tier | Range (blocks, a sphere around the holder) |
|---|---|
| HV | 128 |
| EV | 256 |
| IV | 512 |

No energy, no cooldown, no durability. The link is two clicks and a number, so costing it adds nothing.

### Behavior

- **Link:** sneak-click any cell of a placed warhead (the main block or a tall warhead's filler). The stack stores the main block's position and the dimension (NBT `linked`, `linkX/Y/Z`, `linkDim`). A new link replaces the old one. Sneak-click in the air clears it.
- **Fire:** a plain right-click in the air. The result is always one chat line (`gregguns.icbm.remote.*`):

| Check, in this order | Refusal |
|---|---|
| The stack is linked | `not_linked` |
| The holder is in the link's dimension | `wrong_dimension` |
| The warhead is within the tier's range of the holder | `out_of_range` (names the range) |
| The warhead's chunk is **loaded** | `unloaded` |
| A placed warhead stands at the link | `gone` |
| The holder owns the warhead, or it has no owner, or the holder is in creative | `not_owner` |
| The fuse is not already lit | `already_lit` |

  Every refusal leaves the world untouched. The unloaded-chunk check uses `chunkExists`, so the refusal never loads the chunk.
- **Linking is owner-checked too** (the same ownership test), so a player cannot link, and later fire, another player's warhead.

### How arming applies (D118)

The detonator does **not** detonate a warhead. It lights the warhead's fuse exactly as flint and steel does: `TileWarhead.arm(litFuseTicks)`, the D51 fuse of `20 * (3 + ceil(radius / 3))` ticks at the warhead's fill, with the same beeps and smoke. So:

- the fuse and arming rules of I1 are unchanged: a lit fuse only ever gets *shorter*, never longer (a second press says `already_lit`), and the warhead still goes through `TileWarhead.detonate()` and the blast engine at zero;
- a warhead that is **not** lit is the "unarmed" state; the detonator is how you arm it from afar;
- a warhead that was armed into a missile is no longer a block, so the link reads `gone`;
- the *Defuser* can put the fuse out again during the countdown, which is the player's way out of a remote fuse.

### Recipes

See [Recipes](#recipes). Tooltip lang keys: `gregguns.icbm.remote.*`.

## Defuser

One item, `defuser` (HV).

### Behavior (D120)

| Target | Use | Result |
|---|---|---|
| A **lit** placed warhead (any cell) | Right-click | The fuse goes out; the warhead stays, with its explosive and fill |
| An unlit warhead | Right-click | `not_lit`, no cost |
| A missile standing on a Galacticraft pad or in a silo (`FlightPhase.PAD`) | Left-click | Taken apart exactly like the sneak-click disarm: the body and the warhead drop as items, the fuel is lost |
| A missile whose launch countdown runs (`WARMUP`) | (cannot be clicked) | Abort the launch at the Launch Control first (D88); the defuser API answers `counting_down` |
| A missile that has ignited or flies (`CLIMB`, `ARC`) | Left-click | Refused: `ignited` |

Anyone may use it: defusing only makes things safer. The refusal after ignition is the rule that the missile is a rocket in the air by then, not a device.

### Durability (D121)

32 successful uses, then it breaks (vanilla damage; no energy, no repair, Unbreaking does not apply). Failed or refused uses cost nothing. A creative player pays nothing.

## Recipes

All Assembler recipes (`RecipeMaps.assemblerRecipes`, D69: nothing ICBM below HV), at the tool's tier, one programmed circuit each, 300 ticks, built from `ToolRecipeSpec`.

| Tool | Machine | Inputs |
|---|---|---|
| Remote detonator HV | HV | circuit 1; Advanced circuit, 2 stainless steel plates, HV Emitter, 4 fine electrum wire |
| Remote detonator EV | EV | circuit 2; Data circuit, 2 titanium plates, EV Emitter, 4 fine platinum wire |
| Remote detonator IV | IV | circuit 3; Elite circuit, 2 tungstensteel plates, IV Emitter, 4 fine platinum wire |
| Defuser | HV | circuit 4; Advanced circuit, 2 stainless steel plates, HV Sensor, 2 rubber plates |

- **Cost reading (D63 style):** each is a shell of two plates around a pack-priced emitter or sensor, the same order as one guidance computer of its tier, much less than a warhead. Nothing here is the expensive part of a strike.
- **Recycling (D44):** the two shell plates come back (stainless steel, titanium or tungstensteel); circuits, wire and rubber do not.
- **NEI:** the recipes sit in GT's Assembler map, so NEI shows them and the usages with no extra handler.
- **Tooltips (D41):** `HandheldTooltipFormat` lays them out, with unit tests.

## Scenarios

`icbm.tools.remote_detonator` (link, every refusal, the range per tier, the fuse), `icbm.tools.defuser` (fuse out, missile disarm, refusals after ignition, durability). Unit tests: `HandheldRulesTest`.
