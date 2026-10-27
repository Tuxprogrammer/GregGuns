# Blast visuals: gas clouds and smoke plumes

- **ICBM baseline:** [07-visuals-audio](../../icbm-spec/07-visuals-audio/index.md)
- **Decisions:** [D56](../DECISIONS.md), [D50](../DECISIONS.md)
- **Roadmap:** [I2](../../ROADMAP-ICBM.md#i2--missiles-and-the-galacticraft-pad-t1t2) (carried over from I1)

## What changes

I1's gas warheads showed a few vanilla "spell" puffs every half second, which did not read as a cloud; large conventional blasts had only the vanilla huge-explosion burst. I2 gives each gas type a dense, colored cloud that fills its radius for its duration, and large conventional blasts a smoke plume sized by their radius. The long-range nuke flash stays for annihilating explosives only.

## Design

- **Kinds** (`CloudKind`): Chemical (sickly yellow), Debilitation (violet), Contagious (dull green), Smoke (gray, twice the particles), Plume (dark, a rising column). Colors are abstract tints per gameplay type; no real agent is named or imitated (D56).
- **Server** sends one `PacketBlastCloud` (19 bytes: kind, x, y, z, radius, whole length, ticks left) to players within 128 blocks when a gas blast forms, and again every 100 ticks while it runs, so a player who walks into range sees it. A 30-second gas cloud costs a nearby player at most 7 packets. A plume is one packet.
- **Client** (`CloudFxClient`) keeps at most 4 clouds and spawns at most 20 smoke particles per cloud per tick, colored and 2.2 blocks across, with a fixed life of 30 to 50 ticks (so about 800 live per cloud, inside vanilla's 4,000 per layer). Gas particles are uniform in the blast sphere so the cloud is filled, not a shell; the plume's column rises and widens with age. Particles are added to the effect renderer directly, because the world's own `spawnParticle` drops anything more than 16 blocks from the camera, which would hide a large cloud seen from outside. The player's particle setting scales the count (Decreased half, Minimal a quarter); a cloud more than 128 blocks plus its radius away spawns nothing.
- **Plume** (`CloudPlan.wantsPlume`): every non-annihilating blast of radius 6 or more; its life is 60 ticks plus 6 per block of radius, capped at 260.
- All numbers are in `CloudPlan`, unit tested (`CloudPlanTest`): rates inside bounds for every kind and radius, uniform filling, plume growth, packets per player, the bounded registry that refreshes a re-announced cloud and drops the oldest.

## Tests

`CloudPlanTest`; the look is checked in the batched client session (gas clouds and large blasts read clearly).

## Orbs

The annihilation warheads (I5, D135) are drawn as one translucent sphere each, not as particles. `PacketBlastOrb` (kind byte, x, y, z floats, radius short, ticks-left short: 17 bytes) tells players within 256 blocks. The **antimatter flash** is a white-cyan ball that swells with an ease-out curve to the blast radius over 60 ticks while fading out. The **red matter body** is a dark core with a red translucent rim at the carved radius, steady until its last second, re-announced every 20 ticks so late arrivals see it. The client (`OrbFxClient`) keeps at most 4 orbs, draws each with one reused GLU quadric (24 segments) in the world-last render pass with depth writes off, and does no block scan and no per-frame allocation. The numbers live in `OrbPlan` with unit tests.
