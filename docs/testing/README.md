# In-game scenario tests

Scenarios are reproducible in-world situations with a checked outcome ([CONTRIBUTING § Testing](../../CONTRIBUTING.md#testing)). They exist only in the **dev environment** (`runClient` / `runServer`); the command is not registered in a released jar.

## Running scenarios

### In game (op only)

```
/gregguns test list          # every scenario and its description
/gregguns test <scenario>    # one scenario, in its own arena offset from your column
/gregguns test all           # every scenario
```

Tab completion offers `all`, `list` and the scenario names. The player must be op (permission level 2).

### Dedicated-server console

Type the same commands without the slash into the `runServer` console:

```
gregguns test all
```

There is no player, so the base origin is the overworld spawn column; each scenario then gets its own slot (see Scenario isolation). The output ends with a line you can grep:

```
[GregGuns] PASS harness.area
[GregGuns] PASS harness.entity
[GregGuns] Scenarios: 2 passed, 0 failed
```

The summary line is printed only after every scenario has finished, including those that wait for server ticks (see below).

A failed scenario prints `[GregGuns] FAIL <name>` followed by indented messages. The orchestrator script waits for `Scenarios: N passed, M failed` and treats `M > 0` as failure.

## Writing a scenario

Implement `com.gregguns.core.testing.Scenario` and register it on `ScenarioRegistry.SCENARIOS`. Register from a place that runs on the server before `FMLServerStartingEvent` finishes; the feature's own server-starting hook is fine.

```java
public final class PistolDamageScenario implements Scenario {

    public String name() { return "gun.pistol.damage"; }

    public String description() { return "Fires 10 rounds at a dummy and checks the damage dealt."; }

    public ScenarioResult run(ScenarioContext ctx) {
        ScenarioResult.Builder out = ScenarioResult.builder(name());
        EntityPig dummy = new EntityPig(ctx.world());
        dummy.setLocationAndAngles(ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5, 0, 0);
        ctx.spawn(dummy);                       // removed automatically afterward
        // ... fire, then:
        out.checkEquals("damage dealt", 40.0F, 10.0F - dummy.getHealth());
        return out.build();
    }
}
```

Rules:

- **Name:** lower case `[a-z0-9_.-]+`, dot-separated by feature. `all` and `list` are reserved. Duplicates throw at registration.
- **Clean up:** everything made through `ctx.spawn`, `ctx.setBlock` and `ctx.clearArea` is undone by the runner, even if the scenario throws. Do not change the world any other way. `clearArea` does not preserve tile entities; the harness gives every scenario a private, pre-cleared area (see Scenario isolation).
- **Determinism:** use `ctx.random()` (seeded from the scenario name), never `new Random()` or the clock.
- **Report with the builder:** `check`, `checkEquals`, `note`. A failed check does not stop the scenario, so one run shows every problem.
- **Server thread:** `run` is called on the server thread. A scenario that needs ticks to pass returns a pending result instead of a final one (next section).
- **No player:** `ctx.player()` is `null` on the console. A scenario that needs a player must say so and fail clearly, or build a fake one.
- Add a row to the table below in the same PR.

### Scenarios that span several ticks

Entities move, projectiles fly and mobs react only as the server ticks. Such a scenario sets everything up in `run`, then returns `ScenarioResult.pending(name, timeoutTicks, poller)` instead of a result:

```java
public ScenarioResult run(ScenarioContext ctx) {
    // ... spawn the shooter and the target, fire ...
    return ScenarioResult.pending(name(), 40, ticks -> {
        if (!bullet.isDead && ticks < 39) {
            return null;                       // not yet: ask again next tick
        }
        ScenarioResult.Builder out = ScenarioResult.builder(name());
        out.checkEquals("target health", 12.0F, target.getHealth());
        return out.build();                    // final result
    });
}
```

- The poller is called once per server tick (at the end of the tick) with `1`, `2`, `3` and so on. It returns the final result, or `null` to wait on.
- If it has not answered after `timeoutTicks` polls, the scenario fails with `timed out after N ticks`. Keep the timeout well above the expected time, and finish early by returning a result as soon as you can.
- The scenario's context, with everything it spawned or changed, stays alive until the poller answers or times out, and is cleaned up then. A poller that throws fails only its scenario.
- Scenarios run strictly one after another. The next one starts in the same tick the previous one finishes. Synchronous scenarios behave exactly as before.
- The console summary line and the chat report are printed once, after the last scenario of the run has finished. While a run is waiting, a second `gregguns test` is refused (`gregguns.test.busy`).
- In plain Java: `ScenarioRunner.begin(list)` returns a `ScenarioRun` that you call `tick()` on; `ScenarioRunner.run(list)` is the old synchronous form and fails any scenario that asks to wait. `ScenarioTicker` (server side) drives `ScenarioRun`s from the FML server tick for the command.

The framework logic (registry, argument parsing, runner, report) is plain Java and has unit tests in `src/test/java/com/gregguns/core/testing/`.

## Scenario isolation

Blocks and entities that a scenario leaves behind (a StructureLib multiblock, GT machines, a projectile corridor) used to break later scenarios, because everything shared one origin. Each scenario now runs in its own arena. The code is `ScenarioArea` (pure, unit-tested in `ScenarioAreaTest`), `ScenarioChunks` (Forge tickets) and `WorldScenarioContext.create`.

- **Own origin.** Scenario names are sorted; the index in the full registry (not just the selected scenarios) is the slot. Slots fill a grid 6 wide, row by row, 80 blocks apart on X and Z, from a base: the overworld spawn column on the console, or the invoking player's column. The slot depends only on the registered names, so a failing scenario reproduces at the same place on every run of the same build (adding a scenario shifts the slots after it). `ctx.originX()`, `originY()` and `originZ()` return the slot's origin, so scenarios need no change.
- **Fixed Y = 100.** Clear of the terrain in a normal dev world, below the nether ceiling, inside a superflat world's build limit. Ground is made, not found: `Y - 1` is stone, so a scenario that expects ground has it.
- **Cleared before it runs.** For the scenario's `areaRadius()` (default 32, clamped to 48) the harness kills every non-player entity, sets 16 layers from the origin Y up to air (which removes tile entities) and lays the stone floor. This writes straight to the world, not through `ctx.setBlock`, so cleanup does not "restore" the residue of an earlier run.
- **Loaded across ticks.** The same area is held with Forge chunk tickets (`ForgeChunkManager.requestTicket` / `forceChunk`, several tickets when the area is over a ticket's 25 chunk limit), so a projectile never meets an unloaded chunk (WGEN-27) and a multi-tick scenario's area stays loaded. The tickets are released in `ctx.cleanup()`, which the runner calls on finish, failure and timeout. The `LoadingCallback` registered for the mod releases any ticket saved in a world file by a killed server. Dev environment only: the callback is registered under `DevEnvironment.isActive()` and nothing in gameplay code requests tickets.
- **Overriding.** A scenario that needs a bigger area overrides `Scenario.areaRadius()`. The maximum, 48, touches a default-sized neighbour but does not overlap it.
- **Proof.** `gregguns test all` run twice in a row in one world passes 33 of 33 both times (`build/run_scenarios_twice.py`, a local variant of the orchestrator's runner that sends the command twice).

Source: Forge 1.7.10-10.13.4.1614 `net.minecraftforge.common.ForgeChunkManager` (Gradle cache, `minecraft/net/minecraftforge/forge/1.7.10-10.13.4.1614-1.7.10/unpacked/src/main/java/`).

## Scenarios

| Scenario | Feature | What it checks |
|---|---|---|
| `harness.entity` | test harness | Spawns a pig at the origin, checks it is alive, in the world and at 10 health, removes it, and checks it is dead. |
| `harness.area` | test harness | Clears an area, places a block, then cleans up and checks the original block is back. |
| `combat.types` | damage system | Each of the ten damage types hits an unarmored pig through a real source: health lost equals the damage, and the type's secondary effect (fire, slowness, poison, radiation sickness, shock) appears only where it should. |
| `combat.penetration` | damage system | A mob in full iron armor (15 points) takes 8 damage at 0/15/100% penetration as 3.2 / 3.92 / 8.0 (GUN-34), every blockable type alike (GUN-45), unresistable in full; the source keeps the shooter and the projectile (GUN-37). |
| `combat.player` | damage system | A fake player in full iron armor takes the same 3.92 through Forge `ApplyArmor`, each armor piece is worn exactly once (armor not applied twice), and unresistable skips armor. |
| `combat.iframes` | damage system | Melee 6 then 8 inside the frames loses 8, not 14 (GUN-33); ignoring sources land every time and leave the frames as they found them. |
| `combat.rejected` | damage system | Ignoring-frames hits that vanilla rejects (dead or invulnerable target, fire against Fire Resistance) leave the target's frames at 15 and its health alone, by raw `attackEntityFrom` and by `DamageSources.hit`; an unprotected control still takes the full hit. |
| `combat.listeners` | damage system | A foreign `LivingAttackEvent`/`LivingHurtEvent` listener sees our source once, and the raw pre-armor amount at HIGHEST priority (GGS-02-18). |
| `combat.hazmat` | damage system | A pig in a full stub-hazmat set (GT `IHazardProtector`) takes the direct damage but none of the fire, ice, poison, radiation or shock effects (GGS-02-22). |
| `production.ammopress` | production chain (M1) | The LV Ammo Press (MTE 20101) is registered as an `MTEAmmoPress` at LV on `gregguns.ammoPress`, has an item form, and places as a GT machine block. |
| `production.recipes` | production chain (M1) | The ammo map finds RC-28, RC-37 and RC-44 at `RECIPE_LV` with the sheet's counts and durations, and the assembler holds RC-05 and RC-11 at LV. |
| `production.ammopress.tiers` | Ammo Press tiers (M4) | Steam, LV, MV, HV and EV (20100 to 20104) have the right class, GT tier, slot counts, the ammo map, `RecipeMapWorkable` (so GT lists them as NEI catalysts) and a hand recipe; RC-28 is found at LV to EV voltage and not at ULV. |
| `production.ammopress.run.steam` | Ammo Press tiers (M4) | A placed Steam Ammo Press with steam runs a scenario-registered ULV recipe (2 cobblestone to flint) and outputs it. |
| `production.ammopress.run.lv` | Ammo Press tiers (M4) | A placed, powered LV press runs RC-28 and outputs 16 bullets. |
| `production.ammopress.run.mv` | Ammo Press tiers (M4) | The same at MV (overclocked). |
| `production.ammopress.run.hv` | Ammo Press tiers (M4) | The same at HV. |
| `production.ammopress.run.ev` | Ammo Press tiers (M4) | The same at EV. |
| `projectile.hit` | projectile framework | A 9mm (`GunCatalog.PISTOL`) shot fired through `Projectiles.launcher()` from a fake player skips a dead zombie in front and takes the live zombie behind it down by the damage model's figure for 8 damage; the bullet is gone and the entity is registered (PFX-01). Waits for ticks. |
| `projectile.falloff` | projectile framework | The same shot at a zombie 21 blocks away (past the 18 block drop-off start): health lost equals `DamageFalloff` at the measured distance run through `DamageModel`, strictly between the maximum and the minimum. Waits for ticks. |
| `projectile.blocks` | projectile framework | A bullet flies through tall grass (PFX-27), ends at a stone wall without leaving an entity behind, and the zombie behind the wall is unhurt (PFX-23). Waits for ticks. |
| `projectile.shooter` | projectile framework | A pig fires from inside its own box: the shooter is unhurt (grace period) and a pig three blocks away takes the full damage. Waits for ticks. |
| `gun.fire` | pistol slice (M1) | A fake player holding a real `pistol_9mm` presses the trigger through `ServerGunHandler`: one bullet is launched, a zombie 6 blocks away loses what `DamageModel` gives for 8 damage, the NBT `ammo` goes 18 to 17, a second click 1 tick later is swallowed, and a click made 3 ticks later fires on tick 4 (4 tick cooldown, GUN-29 buffer). Waits for ticks. |
| `gun.reload` | pistol slice (M1) | An empty pistol with a `mag_9mm_pistol` in the inventory: the dry trigger starts a reload, ammo stays 0 until the 35th tick, then 18; the magazine is consumed and a `mag_9mm_pistol_empty` comes back. Waits for ticks. |
| `gun.reload.forced` | pistol slice (M1) | The reload key on a pistol holding 7 rounds: after 35 ticks it holds 18, the loaded rounds come back as `floor(7/6)=1` loose bullet (GUN-13; the 1 leftover round is the documented whole-bullet rounding) and the empty magazine is returned. Waits for ticks. |
| `gun.dryfire` | pistol slice (M1) | An empty pistol with no magazines or bullets, trigger pulled twice: no bullet, no reload, ammo stays 0, nothing throws. Waits for ticks. |
| `gun.player` | pistol slice (M1) | A shot from one fake player hits a second fake player 6 blocks away and costs them the `DamageModel` figure (the victim sits in its chunk only, because a fake player has no network handler for the entity tracker). Waits for ticks. |
| `gun.zoom` | pistol slice (M1) | A zoom intent on the unscoped pistol through `ServerGunHandler.onZoomToggle`: accepted, zoom state stays off, FOV factor stays 1, rounds untouched. |
| `gun.kind` | gun framework (M2) | Guns with `ProjectileKind.BULLET` and `GRAVITY_BULLET` fired through the launcher spawn a plain bullet and a `SimpleProjectile`; a kind with no registered factory spawns nothing and logs one error (expected in the log). |
| `gun.pellets` | gun framework (M2) | A gun with 7 extra pellets fires 8 projectiles from one shot, and their yaws fan out. |
| `gun.knockback` | gun framework (M2) | Three bullets with knockback 0, 1 and 3 hit fresh AI-less zombies: the push is none, vanilla's, and three times vanilla's (GUN-38). |
| `gun.revolver.fire` | revolver (M2) | A real `revolver` fired through `ServerGunHandler`: one bullet, a zombie 6 blocks away loses the `DamageModel` figure for 8, rounds 6 to 5, an early click is dropped and a click one tick before the 6 tick cooldown ends fires at tick 6. |
| `gun.revolver.reload` | revolver (M2) | An empty revolver with one `ammo_9mm_bullet`: the dry trigger starts a reload, 0 rounds until the 45th tick, then 6; the bullet is consumed and nothing comes back. |
| `gun.revolver.reload.forced` | revolver (M2) | The reload key at 5 rounds with one bullet: 6 rounds after 45 ticks, the bullet consumed and none refunded (a bullet is worth 6 rounds, so no round is multiplied into several). |
| `gun.revolver.damage` | revolver (M2) | A zombie 16 blocks away takes the drop-off figure (8 falling to 6 between 12 and 20 blocks), below the full 8. |
| `gun.revolver.recipe` | revolver (M2) | `RecipeMaps.assemblerRecipes` holds the revolver at `RECIPE_LV` and 15 s; an LV query with the sheet inputs and circuit 2 finds it; a voltage below LV does not. |
| `gun.revolver.parts` | revolver (M2) | The Wooden Stock and Mechanical Parts have crafting-table recipes; the Iron Barrel is on the assembler map at LV. |
| `gun.recipe` | pistol slice (M1) | `RecipeMaps.assemblerRecipes` (the map GT's NEI handler lists) holds the pistol at `RECIPE_LV` and 15 s; an LV query with the sheet inputs and circuit 1 finds it; a voltage below LV does not. |
| `armor.equip` | armor (M3) | A fake player wears the Scout set: speed attribute x1.5, full set detected, jump +0.16, vanilla bar 13; boots off gives x1.375 and no full set; naked clears the modifier; sprinting doubles the bonus on top of vanilla's sprint modifier; full Combat gives 0.40 knockback resistance. |
| `armor.damage` | armor (M3) | Full Combat set through the real damage handler and Forge `ApplyArmor`: 8 projectile loses 3.2, energy 4.4, poison 8, 15% penetration 3.92, unresistable and fall 8, a plain vanilla source uses the physical value; each piece wears once; broken pieces give nothing and never lose the last point. |
| `armor.lifecycle` | armor (M3) | Scout bonuses are stripped by the logout, death, respawn and dimension-change handlers and come back on the next tick; a broken boot drops its bonus and the full set. |
| `armor.compat` | armor (M3) | CORE-14: a step height of 1.25 survives equip and unequip. CORE-15: a 6000-tick Night Vision on a mob survives armor upkeep, and armor grants and removes only its own. Jump, fall (relative to the pack's baseline) and break-speed bonuses fire through the Forge event bus. |
| `armor.hazmat` | armor (M3) | No catalog set is GT hazmat against any hazard; a test set with partial HEAT and RADIOLOGICAL flags is full hazmat through `GregTechHazmat` and `HazardProtection`, a helmet with a full GAS flag covers GAS alone, nothing claims SPACE or ELECTRICAL. |
| `armor.recipes` | armor (M3) | The 8 steam-age hand recipes and Heavy Cloth are in the crafting manager; the 4 LV Combat recipes are on `assemblerRecipes` at `RECIPE_LV` with the sheet's durations. |
| `armor.slots.goggles` | armor slots (M3) | Night Vision Goggles in the Baubles `head` slot: type `head`, night vision and exactly 2 EU/t only while on and charged; the key switches them off (no drain) and on; empty gives none; unequipped gives none. |
| `armor.slots.pouch` | armor slots (M3) | A worn Ammo Pouch in `quiver`: its magazines count with the main inventory, a 3-magazine removal empties the pouch first and persists in the stack NBT, and an unequipped pouch is not seen (the reload ammo hook). |
| `armor.slots.back` | armor slots (M3) | A test back device in `wings` ticks while worn, gets the action key, and is told once on removal; the fallback Oxygen Tanks add 20 air and 1 charge only with an Oxygen Mask worn (dev runs have no Galacticraft). |
| `armor.slots.masks` | armor slots (M3) | Gas Mask in the helmet slot is full GT gas hazmat, never SPACE, loses it with a spent filter and regains it renewed; Tactical Mask is gas hazmat, gives night vision and drains EU while charged, stays hazmat when empty, and is oxygen gear only while charged. |
| `armor.slots.recipes` | armor slots (M3) | Every special-slot item has an Assembler recipe at its tier voltage (`RECIPE_MV` or `RECIPE_HV`), and the Gas Mask and Oxygen Tanks refill recipes exist. |
| `melee.knife` | melee (M2) | A fake player hits a zombie with the Combat Knife through `attackTargetEntityWithCurrentItem`: it loses the damage model's figure for 7.5 PHYSICAL damage, the hurt source is a GregGuns PHYSICAL source naming the player, the knife loses 1 durability and has 1000 uses. |
| `melee.crowbar` | melee (M2) | The same with the Crowbar: 5.5 PHYSICAL damage, durability loss 1 to 2, harvest level 2 for `pickaxe`, the `pickaxe` tool class, and it harvests iron ore. |
| `grenade.fuse` | grenades (M2) | A Stielgranate at rest (thrown by a zombie standing 2 blocks away) is live and harmless until 20 ticks before its 200 tick fuse, then explodes at tick 200; zombies at 1.5, 5 and 8 blocks lose `BlastFalloff` damage (full, ramp, none) through the damage model, the thrower inside the radius is hurt too (PFX-25), the floor is intact with `explosions.blockDamage` off. Waits about 10 s. |
| `grenade.bounce` | grenades (M2) | A grenade dropped from 3 blocks bounces (bounces left falls, still live), then lies on the floor with no vertical speed instead of spending every bounce at once. Waits for ticks. |
| `grenade.protected` | explosions (M2) | `ProjectileExplosion` trials: block damage off breaks nothing but still hurts; on breaks the floor with the same entity damage; a cancelled `ExplosionEvent.Start` stops everything; a `Detonate` listener that clears the block list keeps every block and does not change entity damage (GGS-04-10, 11); cancelled `BlockEvent.BreakEvent`s (one per block for a player owner, GGS-04-12) keep the blocks; `explosions.selfDamage` off spares the owner. |

## M1 coverage

Maps each M1 exit-gate item and each relevant `BUGS.md` row to the tests that cover it. `U:` is a JUnit test (`src/test`), `S:` an in-game scenario (this table above). "Sibling" items belong to other M1 branches.

| Exit-gate item | Covered by |
|---|---|
| Pistol craftable only through its D1 tier gate; recipe in NEI | U: `GunTableTest` (row matches the sheet; `pistolMeetsTheD1GateRule` uses `GateRule`), `GateRuleTest`, `RecipeChecksTest`, `RecipeTierCheckTest`; the dev self-check's tier rule over every registered recipe (`Self-check passed: 6 items/blocks, 28 recipes`); S: `gun.recipe` (in `RecipeMaps.assemblerRecipes`, which GT's NEI integration lists; LV only), `production.recipes` (the parts) |
| Fire, reload and zoom work; hits and damages mobs and players | S: `gun.fire`, `gun.reload`, `gun.reload.forced`, `gun.dryfire`, `gun.player`, `gun.zoom`, `projectile.hit`, `projectile.falloff`, `projectile.blocks`, `projectile.shooter` |
| Unit tests: damage formulas | U: `DamageModelTest`, `DamageTypeTest`, `DamageSourceTest` |
| Unit tests: drop-off | U: `DamageFalloffTest`; S: `projectile.falloff` |
| Unit tests: spread | U: `SpreadAndZoomTest` |
| Unit tests: ammo accounting | U: `StoreAmmoInventoryTest`, `MagazineLogicTest`, `GunNbtTest`, `AmmoIdsMatchItemsTest`, `GunControllerTest`; S: `gun.reload`, `gun.reload.forced`, `gun.fire` (NBT ammo) |
| Unit tests: reload state machine | U: `ReloadStateMachineTest`, `GunControllerTest`; S: `gun.reload` (35 tick timing in the world) |
| Two clients on a dedicated server | Sibling `m1/mp-check` |

| Bug | Test | Notes |
|---|---|---|
| GUN-13 forced reload loses rounds | U: `ReloadStateMachineTest.doesNotReproduceGUN13_*` (2), `StoreAmmoInventoryTest.doesNotReproduceGUN13_forcedReloadKeepsLoadedRounds`; S: `gun.reload.forced` | Rounds return as whole loose bullets; the sub-bullet remainder (at most 5) is lost, recorded as a deviation in `StoreAmmoInventory` |
| GUN-29 semi click swallowed in cooldown | U: `FireControlTest.doesNotReproduceGUN29_semiClickDuringCooldownIsRemembered`; S: `gun.fire` (click at tick 3 fires at tick 4) | |
| GUN-32 client/server fire-delay desync | U: `FireControlTest.doesNotReproduceGUN32_*` (2) | The packet path (`requestShot`) is not driven by the scenarios, which use the held-trigger path; the two-client run belongs to `m1/mp-check` |
| GUN-33 i-frame hit applies full damage | S: `combat.iframes` | No pure-Java seam exists: the fix lives in the Forge event handlers (`CombatEvents`), so it is covered in game only. Hardening (`m1/hardening`) owns any further i-frame work |
| GUN-34 penetration ignored on ordinary mobs | U: `DamageModelTest.doesNotReproduceGUN34_*`; S: `combat.penetration` | |
| GUN-01 camo switch on tag-less gun | U: `GunNbtTest.doesNotReproduceGUN01_*` | |
| GUN-06, GUN-07 NPC accuracy and zoom | U: `NpcFiringTest.doesNotReproduceGUN06_*`, `GUN07_*` | |
| GUN-11 out-of-ammo spam | U: `FireControlTest.doesNotReproduceGUN11_*`; S: `gun.dryfire` | |
| GUN-12 server trusts client zoom | U: `SpreadAndZoomTest.doesNotReproduceGUN12_*`, `GunPacketsTest.doesNotReproduceGUN12_*` (2) | |
| GUN-16 pistol drop-off and muzzle set twice | U: `GunCatalogTest.doesNotReproduceGUN16_*` | |
| GUN-18, GUN-19 tooltip range and short damage | U: `GunTooltipFormatTest.doesNotReproduceGUN18_*`, `GUN19_*` | |
| GUN-35 absorption twice | U: `DamageModelTest.doesNotReproduceGUN35_*` | |
| GUN-37 attacker lost on conversion | U: `DamageSourceTest.doesNotReproduceGUN37_*`; S: `combat.penetration` | |
| GUN-39 penetration compounds | U: `DamageModelTest.doesNotReproduceGUN39_*` | |
| GUN-40 armor above cap inverts | U: `DamageModelTest.doesNotReproduceGUN40_*` | |
| GUN-43 missing UNRESISTABLE lang key | U: `DamageTypeTest.doesNotReproduceGUN43_*` | |
| GUN-44 fire flag | U: `DamageSourceTest.doesNotReproduceGUN44_*` | |
| GUN-45 vanilla armor blocks all types | U: `DamageModelTest.doesNotReproduceGUN45_*`; S: `combat.penetration` | |
| GUN-47 unscoped zoom flag | U: `SpreadAndZoomTest.doesNotReproduceGUN47_*`, `GunKeybindsTest.doesNotReproduceGUN47_*`; S: `gun.zoom` | |
| PFX-01 projectile entity not registered | S: `projectile.hit` | |
| PFX-23, PFX-27 wall and plant hits | S: `projectile.blocks` | |
| PFX-34 gravity not sent to client | U: `BulletSpawnDataTest.doesNotReproducePFX34_*` | |
| GUN-36 converted sources absolute | Not applicable: GregGuns builds its own sources and has no Techguns-source conversion; only UNRESISTABLE is absolute (`DamageSourceTest.onlyUnresistableIsUnblockable`) | |
| GUN-38 knockback ignores strength | U: `KnockbackTest.doesNotReproduceGUN38_theMultiplierChangesTheStrength`; S: `gun.knockback` | Fixed in M2: the per-gun `GunStats.knockback()` multiplier scales the velocity change vanilla gave the target |


| `mp.sync` | multiplayer (M1) | Needs two connected clients. Players A and B (by username) face each other four blocks apart. A's client fires twice through the real input path and trigger packet, the server reloads A's pistol through `ServerGunHandler.onReloadKey`, and the scenario checks that a bullet was launched, B took damage, and A's rounds and magazines changed. Client B logs and screenshots what it sees. Waits for ticks. |
| `mp.visual` | visuals (M0, M1) | Needs one connected client. Signals the first player to take screenshots of the first-person pistol with ammo HUD, third person, a dropped pistol, the inventory, the creative GregGuns tab, the LV Ammo Press GUI and NEI's recipe view for the pistol and the bullet. Passes once the signals are sent; the evidence is in the client's log and screenshots. Waits for ticks. |
| `mp.grenade` | visuals (M2) | Needs one connected client; not part of `test all`. Gives the first player the knife, crowbar and 16 Stielgranaten, signals a screenshot of each held item, throws one grenade through `ItemStielgranate.onPlayerStoppedUsing` (fuse shortened to 40 ticks, `explosions.blockDamage` on for the run), and signals screenshots of its flight, the blast and the crater. Checks one grenade was consumed. Evidence is the screenshots; read them. |
| `mp.give` | items and tooltips (M1) | Needs one or two connected clients. Gives each player every registered GregGuns item and the LV Ammo Press one at a time with the real `/give`, selects it, and has the client hold it and build its tooltip; then repeats through the client-side creative slot packet. Any exception in a log fails the orchestrator run. Waits for ticks. |

## Multiplayer and visual autotest

M1's exit gate ("a dedicated server with two clients shows correct sync") and the visual checks (creative tab, pistol in four views, ammo HUD, Ammo Press GUI, NEI tab) need real clients. This section is a repeatable, scripted way to collect that evidence with nobody at the screen. Dev environment only.

### The pieces

| Piece | Where | What it does |
|---|---|---|
| Client hooks | `com.gregguns.client.autotest` (`ClientAutotest`, `AutotestCommands`, `Screenshots`) | Active only when `DevEnvironment.isActive()` **and** the JVM property `-Dgregguns.autotest=<label>` is set; otherwise `ClientAutotest.init` returns before registering anything, so a released pack never runs any of it. Logs gun events and gregguns sounds, takes screenshots, obeys the server's signals. |
| Signal protocol | `com.gregguns.core.testing.AutotestSignal` (unit-tested) | The server tells a client what to do with a chat line `[gregguns-autotest] <command> <args>`. The client hides the line. A console `say [gregguns-autotest] quit` works too. |
| Server scenarios | `com.gregguns.gun.server.MpScenarios` (`mp.sync`, `mp.visual`) | Drive the clients with signals and check server state. Registered in the dev environment only. |
| Orchestrator | `build/mp_check.py` (not committed; the orchestrator keeps a copy) | Boots the server and both clients, runs the scenarios, collects logs and screenshots, shuts everything down. |

Client log lines all contain `[gregguns] AUTOTEST` and are greppable:

```
AUTOTEST joined Alice
AUTOTEST client Bob saw shot from Alice (remote)          # from PacketGunEvent
AUTOTEST client Alice saw shot from Alice (predicted)     # from the local prediction mirror
AUTOTEST client Bob saw reload start from Alice (remote)
AUTOTEST sound gregguns:pistol_9mm.fire (client Bob)      # every gregguns sound the client starts
AUTOTEST screenshot b-sees-a-shot <path>
```

Signals run strictly in order on the client, and a `shot` waits for four rendered frames before the next command, so a GUI that stalls the client is still drawn and photographed before `close`. Signal commands: `view <0|1|2>` (first, third back, third front), `shot <label>` (screenshot at the end of the next frame), `arm <shot|reload> <label> <delayTicks>` (screenshot that many ticks after the next shot or reload event the client sees), `fire <n>` (hold the fire button for two ticks, n times, six ticks apart, through `GunInputHandler`, which also works without window focus), `gui inventory | creative <tab> | press <x> <y> <z> | nei <item>`, `hold <item>` (log the held stack and build its tooltip), `creativegive <modid:name[:meta]>` (client-side creative slot packet), `close`, `quit`. Screenshots use vanilla `ScreenShotHelper` into `run/client/screenshots/<player>-<label>.png`.

### Launching a client that joins a server by itself

The GTNH buildscript's `runClient` task (RetroFuturaGradle) takes everything on the command line, so nothing needs editing:

```sh
./gradlew runClient --username=Alice     --mcJvmArgs=-Dgregguns.autotest=a     --mcJvmArgs=-Dgregguns.autotest.connect=127.0.0.1:25613     --mcArgs=--width --mcArgs=960 --mcArgs=--height --mcArgs=540
```

- `--username` overrides `developmentEnvironmentUserName` for this run (offline-mode name; the server needs `online-mode=false`).
- `--mcArgs` and `--mcJvmArgs` are **list options**: repeat the flag once per word. One value containing spaces reaches Minecraft as a single argument.
- **Do not use `--mcArgs=--server ... --port ...`.** It connects while the client is still starting and loses an FML race: `FMLClientHandler.connectToServer` creates `playClientBlock` only after it has shown `GuiConnecting`, so the handshake on the netty thread can reach `waitForPlayClient` first and throw a NullPointerException ("Connection Lost: a fatal error has occured"). Instead set `-Dgregguns.autotest.connect=host:port`: the autotest waits until the main menu has settled, primes the latch by reflection and shows `GuiConnecting` itself.
- The run directory is `run/client` of the project directory, and **two Gradle builds cannot run at once in one project directory**. Use one git worktree per process: one for the server, one per client (all at the same commit).
- The client needs `modules.guns` on in its own `run/client/config/gregguns.cfg` and `pauseOnLostFocus:false` in `options.txt` so an unfocused window keeps ticking (the orchestrator writes both). `options.txt` stores `fov` as 0 to 1 (0.0 is 70 degrees): writing `70.0` gives a 2870 degree field of view and an upside-down world.
- **Mod versions.** The buildscript bakes the git state into the mod version, so a server and a client from different worktrees would be refused by FML. `GregGuns.checkRemote` (`@NetworkCheckHandler`, decision in `RemoteVersionCheck`) accepts any GregGuns version in the dev workspace and requires an exact match in a released jar.
- Start the clients one after the other. The orchestrator boots client B only after client A is in the world and requires each join to complete within 20 seconds of the connect attempt.

### Running the whole check

```sh
python -I build/mp_check.py <server-worktree> <client-a-worktree> <client-b-worktree> [port] [out-dir]
```

It boots the server (flat, peaceful world; both users op), boots the two clients (users `Alice` and `Bob`), waits for `AUTOTEST joined` in both logs, runs `gregguns test mp.sync` and `gregguns test mp.visual` on the server console, checks the logs for the evidence lines, then shuts down (the clients quit on a `quit` signal, the server on `stop`; leftovers started from those checkouts are killed by command line). The orchestrator runs `mp.sync`, `mp.visual` and `mp.give` (every registered GregGuns item and the Ammo Press through the real `/give`, then through the creative slot packet, each held for a few ticks so tooltip, HUD and renderer run). It aborts at once, with the exact line, on any failure signature in a log (`Connection Lost`, `Mod rejections`, `fatal error`, `lost connection`, `Crash Report`, `Exception`, `Caused by:`, `at com.gregguns`). Known noise it ignores: GT's NEI handler cache race in worker threads (`NEI-worker-thread`, not mentioning gregguns). Output in `<out-dir>`: `server.log`, `client-a.log`, `client-b.log`, `*-autotest.txt` (only the AUTOTEST lines), `summary.txt` and `shots/*.png`. Exit code 0 means both scenarios passed and every expected log line was present. Allow 10 to 15 minutes, most of it booting.

What a screenshot proves is judged by looking at it: the script only proves a file was written. Read the PNGs.
