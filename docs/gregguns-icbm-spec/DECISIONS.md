# GregGuns ICBM: decisions and open questions

Each question here lists its context, the options and a recommendation. Context means how GregGuns already does it (design records) and how GTNH does it (pack precedent). Answered questions keep the option the owner chose. Once the spec settles, decided rows become design records in [docs/design/](../design/README.md). This branch reserves no record numbers.

To decide an open question, reply with its number and an option letter, or write your own answer under it.

## Decided

The owner decided all of these on 2026-10-20.

| # | Question | Decision |
|---|---|---|
| D1 | How the spec is laid out | Two folders: [icbm-spec](../icbm-spec/README.md) (how ICBM does it) and this one (how we do it) |
| D2 | Code policy | Clean room by default. Named hard-math algorithms may be ported from ICBM-Classic's MIT 1.12 code with the notice kept. Nothing comes from the unlicensed `prod/1.7`. **Quality gate:** ported code is not committed or pushed until it meets the existing GregGuns bar: comments, tests, CONTRIBUTING patterns, the definition of done, and every known `ICBM-xx-nn` bug in it fixed. Never clone for speed |
| D3 | Spec depth | Full sheets for mechanics; one catalog line per asset; full asset entries only for kept assets |
| D4 | Baseline | ICBM-Classic `prod/1.12` @ `9c1d985a`, plus an ICBM 2 modular-missile appendix and a 1.7.10 lineage appendix |
| D5 | Tiers on the GT ladder | Spread over HV to UHV. T1 HV; T2 EV–IV; T3 LuV–ZPM, with nuclear at ZPM beside the Nuke Launcher ([0001](../design/0001-tier-map.md)); T4 antimatter at UV; red matter at UHV (D10) |
| D6 | Roadmap | Spec now; build after 1.0 as an ICBM track (M10 and later) behind `modules.icbm`. The MVP (M0–M9) is unchanged |
| D7 | Galacticraft | The GTNH Galacticraft fork is an **integration target**: launch pad, fuel loader, per-tier fuels and the NASA Workbench |
| D8 | How missiles are built and launched | Galacticraft workbench build, with the GC pad and our own silo. See [D8 in detail](#d8-in-detail) |
| D9 | Galacticraft dependency | Required by `modules.icbm` only. Without GC the ICBM module turns itself off with a log line, and the core mod is unaffected. The GC API is compiled against as an optional dependency |
| D10 | Which ICBM-Classic 1.12 content to keep (was Q2) | See [D10 in detail](#d10-in-detail) |
| D11 | Griefing defaults (was Q3) | **Terrain damage on by default**, with claimed territory respected through the existing pipeline (`ExplosionEvent.Detonate` and `BlockBreakGuard`). Config: `icbm.maxBlastTier` and per-tier `icbm.terrainDamage`. A missile in flight holds one chunk ticket and releases it on impact or timeout. A config caps how many missiles fly at once |
| D12 | Overlap with GregGuns weapons (was Q4) | **One shared explosive registry.** ICBM blast types become warheads that existing GregGuns launchers can carry later. The tactical nuke and the ICBM nuclear warhead share one nuclear blast implementation at different scales. ICBM's own handheld launcher and grenades are dropped |
| D13 | Launch control (was Q5) | **OpenComputers first.** The ComputerCraft methods that ICBM 1.6.4 gave its launcher, cruise launcher, radar and EMP tower are ported as OC components. GregGuns ships ready-made programs on **craftable floppies**, so computer launches need no Lua from players. Also available: a ModularUI2 targeting GUI, data-stick target copying, and firing by redstone or the GT machine-controller cover. The floppy and its programs are D18 |
| D14 | Radar form (was Q7a) | **Short-range single-block radars in HV and EV hulls**, none below HV, because missiles start at HV just as GTNH moon rockets do. **A multiblock radar tower from IV up**, with range scaling by voltage tier. Both scan the missile tracker's list of missiles in flight rather than chunks, and treat a missile as "incoming" only if its predicted impact point falls inside the protected radius. Outputs: redstone, a GT cover, an OC component, and a data-stick link to SAM batteries |
| D15 | Where SAMs fire from (was Q7b) | **A separate air-defense battery**: a single-block machine that holds SAMs and fires when a linked radar reports a threat. It is independent of the silo |
| D16 | What an EMP does (was Q7c) | It **drains stored EU** in GT machines, cables and batteries, and gives **each multiblock one random maintenance issue**. Missiles use ICBM's kill-chance table. It loops over loaded tile entities, not block positions. **Each pulse affects at most N devices** (config, default 64), nearest first, so a dense base can't stall the server. **EU cost grows continuously with radius.** 1.6.4 and 1.7 divided the radius as integers, so every radius below the maximum cost the minimum ([A2-lineage](../icbm-spec/A2-lineage/index.md)) |
| D17 | Worldgen (was Q8) | **Abandoned Missile Silos**, on the M8 placement system. They are rare, Overworld-only and config-gated. See [D17 in detail](#d17-in-detail). Silos on Galacticraft planets are **approved but deferred** until after the MVP |
| D18 | OpenComputers programs (was Q10) | **One floppy, "Golden Dome"**, holding every program and the shared library. It is crafted in the GT Assembler from a blank OC floppy plus an MV circuit. The spec is [01-core/golden-dome.md](01-core/golden-dome.md) |
| D19 | Which fuel each missile needs (was Q6a) | **Keyed to the missile's GT voltage, matching GTNH rockets:** HV needs RP-1 (or Cetane-Boosted Diesel); EV–IV Dense Hydrazine; LuV Purple (CN3H7O3); ZPM and up Green (H8N4C2O4). Each tier accepts "this fuel or better". Missile classes register these lists in GC's `RocketFuels`, so the GC pad and loader enforce them |
| D20 | How much fuel (was Q6b) | **A fixed amount per missile tier**, shown in NEI and on the tooltip, times one config factor. **Unburned fuel strengthens the blast:** a missile that hits a closer target arrives with fuel left, and that fuel adds to its blast radius, as leftover propellant does in real life. The bonus is capped (config). See [D20 in detail](#d20-in-detail) |
| D21 | Fallout (was Q9) | Option A, with owner amendments: hard server-safety caps, protection from any hazmat-capable armor, damaging rubble, and radiation kept within 2 blocks of the surface. See [D21 in detail](#d21-in-detail) |
| D22 | How the silo is fueled (was Q6c) | **A GT fluid input hatch** on the silo, checking the same `RocketFuels` lists as the GC pad, so fuel rules match exactly |

### D8 in detail

GTNH builds every rocket the same way, and missiles reuse that path:

1. A **missile chip Tn** is made in the GT Circuit Assembler at the tier's voltage, the way GTNH makes rocket schematic chips (T2 HV … T8 UV).
2. The Laser Engraver turns the chip into a **missile schematic Tn**. Using the schematic unlocks our page in the GC **NASA Workbench**, registered through GC's `SchematicRegistry`.
3. The workbench page has fixed slots:

   | Slot | Source |
   |---|---|
   | Warhead | GT chemistry (Chemical Reactor, Implosion Compressor) |
   | Body plates × N | The tier's plate |
   | Engine | — |
   | Fins × 4 | — |
   | Guidance computer | Assembler, at the circuit tier |

   No new crafting machine.
4. **T1–T2 launch from the GC launch pad**, fueled by the GC fuel loader. The missile entity implements `IDockable`.
5. **T3 and up need the GregGuns silo**, a GT multiblock built under [0011](../design/0011-machine-policy.md) and powered by EU.

Evidence: the GTNH rocket recon (Galacticraft `3.4.34-GTNH`, NewHorizonsCoreMod `2.9.84`). Fuel: the GC fuel loader checks its tank against a `RocketFuels` whitelist keyed by entity class, so our missiles register their own fuel lists there. See Q6.

### D10 in detail

| Content | Decision |
|---|---|
| ICBM crafting items, battery, sulfur and saltpeter, concrete and reinforced glass, handheld rocket launcher and grenades, colors gas, Christmas mobs and the Nightmare missile | **Dropped.** GT, IC2 or GregGuns already provide them, or they are joke content |
| Red matter | **Kept as a UHV missile.** The owner tests it in-game, because it is resource-heavy (500 ray-traces a tick). Unit tests and headless boots only on our side. Our rebuild must not inherit ICBM's red matter ([red-matter.md](../icbm-spec/02-explosives/red-matter.md)), which: never ends or idles (starvation works only at size ≤ 1); keeps tracing 501 rays a tick forever once the ball is clear; reloads at full size (`ICBM-02-80`); has no range on its max-size config; and restarts its sound every tick. Ours needs a bounded lifetime, an idle state, correct save and load, and a tick budget shared with the blast engine |
| Antimatter | Kept, UV |
| Ender blast | Kept, T3 |
| Smoke | Kept, T1 |
| Rot and mutation | Kept. Nuclear blasts leave **radioactive blocks**, not just GT pollution, because many servers switch pollution off. Their debris yields GT radioactive byproducts; details in [Q9](#q9--radioactive-blocks-and-their-byproducts) |
| Antidote | Kept, mapped to GT hazards |
| Cargo parachute, balloon, cargo hook, held-item missile ("supply drops") | **Deferred.** Not a core feature |
| Spikes, glass button and pressure plate | Deferred |

### D17 in detail

- **Placement:**
  - M8's grid system ([structure-placement](../gregguns-spec/11-worldgen/structure-placement.md)) on its own sparse grid: one cell per 64 × 64 chunks, 25% chance per cell, so roughly one silo per 2,000 × 2,000 blocks.
  - Overworld only, with a biome whitelist (plains, desert, tundra) and its own `enabled` flag.
  - It never cascades, so a silo bigger than the populate window is built across several populate calls, as M8 already allows.
- **Size: huge**, big enough for a player to build a megabase inside. The starting target to settle in the sheet:
  - a vertical shaft about 21 blocks across, running from the surface to about y = 12;
  - a main hall about 64 × 64 × 24 off the shaft's base;
  - side rooms for a control room and storage.
- **Mostly underground, with a clear surface marker.** One of three variants is chosen per silo:

  | Variant | Surface |
  |---|---|
  | **O, open** | The shaft is fully open to the sky |
  | **D, half open** | Half of the shaft mouth is open; the other half is a collapsed cap |
  | **Closed** | A capped shaft with a random selection of ceiling blocks replaced by **air columns that run all the way to build height**, so nothing generated later (trees, other structures) hides it |

- **Contents:** inert decorative GT casings, not working machines. No live warheads.
- **Loot:**
  - Capped at HV, one notch above the surface cap in [0005](../design/0005-world-presence.md), because silos are rare and dug in.
  - The main find is a salvageable damaged T1 missile body (GT recycling recipes).
  - A rare T1 missile schematic chip is a shortcut, never a requirement.
- **No NPC guards** in the first build. This was the recommended default; the owner can override it.

### D20 in detail

- **The tank is full at launch.** Every missile of a tier carries the same fuel (D19 fuel, fixed amount × config factor), whatever its target.
- **Burn grows with distance.** The missile uses a share of the fuel equal to the distance flown divided by its tier's maximum range.
- **Leftover fuel adds to the blast.** At impact, the unburned fraction `f = 1 − distance / maxRange` adds to the warhead's radius: `radius × (1 + bonusMax × f)`. `bonusMax` is a config value with a starting default of 0.25, so a point-blank strike is up to 25% bigger.
- **The minimum launch range still applies,** so firing at your own feet can't farm the bonus.
- **Tooltips and the launch GUI show the expected radius** for the current target, so the bonus is never hidden.

### D21 in detail

**Blocks and rewards.** Option A, as proposed:

| Block | Made by | Cleanup yields |
|---|---|---|
| **Irradiated earth**: dirt, stone, sand, gravel and cobble variants | Common. Converts ~20% of matching blocks in the rot radius | Mined as **irradiated rubble**. GT Centrifuge (HV): lead, thorium, uranium-238, a little uranium-235 and plutonium-239 |
| **Trinitite** | Sand within the inner fireball radius | Macerated and centrifuged: small radioactive dust yields |
| **Fallout goo** | Rare, about 1% of converted blocks, concentrated near ground zero. While goo remains in a chunk, that chunk's irradiated earth does not decay | Dissolved in nitric acid in a GT Chemical Reactor (LuV), then centrifuged: technetium, promethium, neptunium, plutonium-241, americium, curium, californium, plus krypton and xenon |
| Goo from ZPM+ warheads | The Ivy Mike analog | Adds trace fermium |

- Yields scale with warhead tier and with distance from ground zero.
- Antimatter and red matter leave no fallout.
- GT pollution is still added when it is enabled.

**Owner amendments:**
- **Surface only.** Conversion touches only the top solid block of each terrain column inside the rot radius, plus at most **2 blocks below it**. Caves and deep stone are never converted, so the reward is on the surface, not in a dig.
- **Any hazmat-capable armor protects.** This uses GT's `HazardProtection` against `RADIOLOGICAL`, exactly as [05-armor/hazard-protection](../gregguns-spec/05-armor/hazard-protection.md) already does: GT hazmat, GregGuns armor that declares radiological protection, the Hazmat enchantment, or any `IHazardProtector`. ICBM's per-item ratings are dropped; we aren't modeling fallout dose.
- **Exposure:** unprotected players standing on or near irradiated blocks get GT's radioactivity effect (`GTUtility.applyRadioactivity`).
- **Rubble and goo hurt while carried.** They are GT radioactive items, so an unprotected player carrying them takes radiation through GT's existing radioactive-item mechanism.
- **Hard caps, so the server never crashes.** Every number is config with a safe default:
  - A per-blast budget of converted blocks, applied over several ticks through the blast engine's shared edit budget.
  - **No spreading.** ICBM's radioactive blocks spread; ours don't. This is how the conversion budget stays an upper bound.
  - **No random ticking.** Decay is a per-chunk countdown, kept as a record per irradiated chunk. It reverts a bounded number of blocks per tick, server-wide.
  - Exposure is checked per player about once a second in a small radius, not per block.
  - A cap on goo blocks per blast.

### Fuel context (D19, D20, D22)

Context: how GTNH fuels rockets. The evidence is the SpaceRace quest line, Galacticraft `3.4.34-GTNH` `RecipeManagerGC.addUniversalRecipes`, and GT++ `RecipeLoaderRocketFuels`.

- **Four GT++ rocket fuels form an "or better" ladder.** Each is made in the GT++ Chemical Plant, gated by **casing tier** rather than voltage (every final step runs at 240–480 EU/t):

  | Fuel (fluid name) | Chemical Plant casing | Rocket tiers that need at least this |
  |---|---|---|
  | Cetane-Boosted Diesel (`nitrofuel`) or RP-1 (`rocketfuelmixb`) | 3, Stainless | T1–T2 (moon) |
  | Dense Hydrazine (`rocketfuelmixd`) | 4, Titanium | T3–T4 |
  | CN3H7O3, "Purple" (`rocketfuelmixc`) | 5, Tungstensteel | T5–T6 |
  | H8N4C2O4, "Green" (`rocketfuelmixa`) | 6 | T7–T8 |

  Rocket schematic chips by voltage, for comparison: T2 HV, T3 EV, T4 IV, T5–T6 LuV, T7 ZPM, T8 UV.
- **Enforcement.**
  - The GC fuel loader checks its tank against a whitelist, `RocketFuels`, keyed by rocket entity class. Galacticraft registers four lists for its rockets (t2, t4, t6, t8).
  - **Our missile classes can register in the same whitelist** with `RocketFuels.addFuel`, so the GC pad and loader enforce missile fuel tiers for free.
  - The rocket's own tank stores all fuels as generic GC `fuel`. The loader is the only gate.
- **ICBM itself uses no fuel.** Its launchers spend energy. Fuel is new GregGuns design that comes from D8.

## Open

These come from the ICBM 2 appendix ([A1, "Ideas worth borrowing"](../icbm-spec/A1-icbm2-modular/index.md)). They fill in what D8's workbench slots actually do.

### Q11 · Warhead size: fixed or a quantity knob?

Context:
- ICBM 2 fills the warhead with 1, 5, 20, 64 or 128 explosive items, capped by casing size, and the blast scales with the count.
- GT recipes and NEI favor fixed inputs and outputs.
- D20 already gives a continuous knob: leftover fuel adds to the blast radius.

**Options:**
- **A (recommended).** **Fixed warhead items**, one per blast type and tier, each made by a fixed GT recipe. Blast size comes from the tier and the D20 fuel bonus. Clear in NEI.
- **B.** ICBM 2's quantity knob: the warhead holds N units of explosive material up to a tier cap, and the blast scales with N.

### Q12 · What fins and the guidance computer do

Context:
- ICBM 2's guidance is two numbers, a scatter radius and a failure chance. They run from 100 blocks / 50% (wood gears) down to 20 blocks / 3% (best chip).
- Its scatter is triangular per axis (the difference of two uniform rolls, MIT-portable), so hits cluster near the aim point. Its failure chance is never actually rolled.
- ICBM-Classic 1.12 has the launcher inaccuracy `2 + 10·(d/range)² + (launchers − 1)` ([launch sequence](../icbm-spec/04-launch-control/launch-sequence.md)).
- Galacticraft fins are plain structural parts.

**Options:**
- **A (recommended).**
  - The **guidance computer's circuit tier sets scatter**: a triangular distribution whose radius shrinks with each tier above the missile's own tier.
  - Distance adds ICBM-Classic's quadratic term, with **no failure roll** (no duds).
  - **Fins are structural**, a tier-material gate like GC's fins.
- **B.** As A, plus a small failure chance (the missile falls short as a dud) when the guidance tier is below the missile's tier.
- **C.** Fins matter too: a better fin tier trims scatter further, or raises flight speed.

### Q13 · Flight model and chunk loading

Context:
- **ICBM-Classic** flies a real entity on an arc. Its tracker simulates missiles in unloaded chunks and hands off at y = 500. That tracker is buggy ([targeting-and-tracking](../icbm-spec/03-missiles/targeting-and-tracking.md)): only explosive missiles and the deprecated flight logic are handled, and all landings share one chunk ticket.
- **ICBM 2** "falls from orbit": the missile leaves the world at y = 400 and re-enters over the target after a travel time computed from speed and distance.
- D11 allows one chunk ticket per missile in flight, and D14's radar reads the missile tracker's list.

**Options:**
- **A (recommended).** **Three phases.**
  1. A visible launch and ascent to about y = 300.
  2. A **simulated mid-course** with no entity: a server-side record holding position, ETA and predicted impact, which radar can track.
  3. A **re-entry entity** spawned above the target, with one chunk ticket on the target area for the descent and impact.

  SAMs engage in the terminal phase only, as real terminal defense does. Chunk loading stays bounded, and the cost doesn't grow with distance.
- **B.** A real entity for the whole flight, ICBM-Classic style, with a chunk ticket that follows it. The most faithful option, and the heaviest; long flights load a corridor of chunks.
- **C.** A instead without a visible ascent: the missile despawns at the pad and only re-enters.

### Q14 · Fusing (airburst and bunker-buster variants)

Context: ICBM 2 puts a trigger inside the warhead with an impact kinetic-energy window, which could give "bunker buster" (high minimum) and "airburst" (low maximum) variants. ICBM never filled in the numbers.

**Options:**
- **A (recommended).** **Defer.** Impact fusing only for the first build.
- **B.** An optional fuse slot on the workbench page, with airburst and penetrator fuses.

### Q15 · Automating missile assembly

Context:
- The NASA Workbench is manual. GTNH players automate everything, and silos need feeding (ICBM 2 used a magazine that moves 1 missile per 2 seconds).
- GT recipes can take non-consumed items, the way a programmed circuit or a mold is not used up.

**Options:**
- **A (recommended).** The workbench page for hand building, **plus a GT Assembler recipe for every missile** that uses the missile schematic as a non-consumed input, so missiles can be mass-produced. Silos accept missiles through a GT input bus.
- **B.** Workbench only.
