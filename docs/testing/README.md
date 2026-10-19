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
| `machines.reaction.place` | machines (M4) | The EV (20106) and IV (20107) chambers and the Retort (20108) are registered; tiers 4 and 5; both chambers run `gregguns.recipe.reactionchamber`; intensity clamps to 0 to 10; the Anti Gravity Core recipe matches at IV voltage and not at EV (GGS-08-46). |
| `machines.reaction.run` | machines (M4) | A powered EV chamber, kept on the drifting preferred intensity, makes one Laser Focus in 300 to 599 ticks (early finish) and goes idle. Multi-tick. |
| `machines.reaction.run.iv` | machines (M4 exit gate) | A powered IV chamber, kept on the drifting preferred intensity (8), makes one Anti Gravity Core from a Nether Star, a Heat Ray Focus and molten Enderium in 420 to 599 ticks and goes idle. Multi-tick. |
| `machines.reaction.failure` | machines (M4) | A chamber started on target and then left on intensity 10 runs the full 600 ticks, consumes the diamond, makes nothing and goes idle (GGS-08-21). Multi-tick. |
| `machines.reaction.radiation` | machines (M4) | `ReactionEffects.applyFailure` with `RAD_MEDIUM` slows an unprotected pig through GT `applyRadioactivity` and does not explode the machine (GGS-08-24). |
| `machines.reaction.retort` | machines (M4) | The Retort is built with `construct` (2 bulb layers) plus hatches, forms, reports 4 parallels and a glass tier, and stops forming when its Muffler Hatch is removed. |
| `machines.reaction.retort.run` | machines (M4 exit gate) | A Retort whose glass is upgraded to the top tier, with an EV hatch and a tuned intensity, makes one Laser Focus into its output bus and goes idle. Multi-tick. |
| `production.ammoworks.registered` | Ammunition Works (M4) | MTE 20105 is an `MTEAmmoWorks`, `RecipeMapWorkable` on the ammo map (NEI catalyst); `gregguns:casings` exists with page-50 texture ids filled; the Works Casing (x2) and Ram Housing Assembler recipes and the controller hand recipe exist. |
| `production.ammoworks.run` | Ammunition Works (M4) | A three-bay hall built by the controller's `construct` forms with real GT hatches, reports 36 parallels at HV, runs three RC-28 recipes in one batch (48 bullets) within the belt-sped duration. Also asserts no item entity is left in its own arena. |
| `production.components` | components (M4) | Every `ComponentTable` row is in its GT map (Assembler, Chemical Bath, Assembly Line) with the sheet's inputs, EU/t, duration and output, found at its tier's voltage and not one tier below; Assembly Line rows carry their research item, scan time and voltage. |
| `production.ammoworks.broken` | Ammunition Works (M4) | A two-bay hall forms; a missing wall casing, a second belt tier, a bay without its Ram Housing, no muffler or no output bus stops it forming, and restoring each part forms it again. Also asserts no item entity is left in its own arena. |
| `machines.repair.registered` | Repair Bench, Gantry, Camo Bench (M4) | The five Repair Benches (20109 to 20113), the Camo Bench (20115) and the Armory Gantry (20114) are registered with the right class and GT tier and each has a hand recipe. |
| `machines.repair.bench.lv` | Repair Bench (M4) | A worn Soldier's Vest at damage 400 is repaired at a placed LV bench: damage 0, exactly the listed plates and cloth taken (extras stay), exactly the job's EU spent, other tags kept. Also asserts no item entity is left in its own arena. |
| `machines.repair.bench.refusals` | Repair Bench (M4) | One cloth short takes nothing (all or nothing); too little energy repairs nothing; an empty slot, a held stick, an undamaged piece and a player 40 blocks away are all refused with the item untouched. Also asserts no item entity is left in its own arena. |
| `machines.repair.bench.tiers` | Repair Bench (M4 exit gate) | The MV, HV, EV and IV benches each repair a worn Soldier's Vest to damage 0, spend exactly `RepairMath.benchEu` of their tier, and the price rises with the tier. Also asserts no item entity is left in its own arena. |
| `machines.repair.gantry.forms` | Armory Gantry (M4) | A four-bay Gantry built by the controller's `construct` forms with all hatches, reports 4 bays and the HV repair tier; a missing roof beam or a missing output bus stops it forming and restoring the beam forms it again. Also asserts no item entity is left in its own arena. |
| `machines.repair.gantry.run` | Armory Gantry (M4) | A three-bay Gantry on two HV hatches repairs a helmet, vest and pants from the input bus in one cycle: all three in the output bus at damage 0 with their other tags, parts consumed, within the worst-case duration. Also asserts no item entity is left in its own arena. |
| `machines.camo.cycle` | Camo Bench (M4) | `PacketCamoCycle` through its own pipeline (validation, authorization, handler) cycles a worn Bandit vest forward, back and around, and the bench-slot item; keeps other tags; ignores one-camo armor; is rejected from 40 blocks, for an unknown target, with no bench or in another dimension. Also asserts no item entity is left in its own arena. |
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
| `gun.sawedoff.fire` | sawed off (M2) | A real `sawed_off` fired point blank through `ServerGunHandler`: 8 projectiles from one shot, rounds 2 to 1, and a 200-health zombie one block ahead loses close to 8 times the `DamageModel` figure for 4 (at least 75% of it, since a pellet or two may miss at plus or minus 8 degrees). |
| `gun.sawedoff.reload` | sawed off (M2) | An empty Sawed Off with 3 `ammo_shotgun_shell`: the dry trigger starts a reload, 0 rounds until the 28th tick, then 2; two shells used, one left. |
| `gun.sawedoff.reload.forced` | sawed off (M2) | The reload key at 1 round with 2 shells: 2 rounds after 28 ticks, exactly one shell used (INCREMENTAL keeps the loaded round). |
| `gun.sawedoff.recipe` | sawed off (M2) | `RecipeMaps.assemblerRecipes` holds the Sawed Off (GR-03) at `RECIPE_LV` and 10 s; an LV query with the sheet inputs and circuit 3 finds it; a voltage below LV does not. |
| `gun.sawedoff.shells.recipe` | sawed off (M2) | The ammo map finds RC-29 (8 foil Brass, 24 round Lead, 4 Gunpowder) at `RECIPE_LV` for 6 s making 8 shotgun shells, and not at ULV. |
| `mp.visual.sawedoff` | sawed off (M2) | The shared `GunVisualScenario` walkthrough for the Sawed Off (first and third person, dropped, inventory, NEI); not part of `test all`. |
| `gun.handcannon.fire` | hand cannon (M2) | A real `handcannon` fired through `ServerGunHandler`: exactly one projectile, a `SimpleProjectile` (the gravity bullet); a zombie 6 blocks away loses the `DamageModel` figure for 8; the only round is spent. |
| `gun.handcannon.fall` | hand cannon (M2) | The ball shot level in a 30 block corridor accelerates downwards by 0.010 to 0.020 blocks per tick squared (gravity 0.015; measured as the change of vertical speed so the random spread does not matter) and has gone 8 to 11 blocks forward after 10 ticks at speed 1.0. |
| `gun.handcannon.reload` | hand cannon (M2) | An empty hand cannon with two `ammo_stone_bullet`: the dry trigger starts a reload, 0 rounds until the 30th tick, then 1; one bullet is consumed, nothing comes back, no projectile is launched. |
| `gun.handcannon.damage` | hand cannon (M2) | A zombie 12 blocks away (the ball has dropped about a block by then) takes the drop-off figure (8 falling to 5 between 10 and 25 blocks), below the full 8. |
| `gun.handcannon.recipe` | hand cannon (M2) | The crafting table makes the Hand Cannon with item damage 1 (an empty gun), the Stone Barrel and 16 stone bullets; the ammo map holds one stone bullet row at `RECIPE_ULV`, 5 s, 24 bullets, 1 output and at most 4 inputs. |
| `mp.visual.handcannon` | visuals (M2) | Needs one connected client; not part of `test all`. The reusable `GunVisualScenario` walkthrough for the Hand Cannon with stone bullets: first person idle, firing and reloading, third person back and front, dropped, inventory, NEI. Evidence is the screenshots; read them. |
| `gun.boltaction.fire` | bolt action rifle (M2) | A real `bolt_action_rifle` fired through `ServerGunHandler`: one bullet, a zombie 6 blocks away loses the `DamageModel` figure for 16 at 15% armor penetration, rounds 6 to 5. |
| `gun.boltaction.rechamber` | bolt action rifle (M2) | The 25 tick rechamber: a click at tick 10 is dropped, a click at tick 24 is remembered and fires at tick 25, nothing auto-fires after; the gun owns a `guns.bolt_action_rifle.rechamber` sound event. |
| `gun.boltaction.reload` | bolt action rifle (M2) | An empty rifle with one `ammo_rifle_bullet`: the dry trigger starts a reload, 0 rounds until the 50th tick, then 6; the bullet is consumed and nothing comes back. |
| `gun.boltaction.damage` | bolt action rifle (M2) | A zoomed shot hits a zombie 25 blocks away for the drop-off figure (16 falling to 10 between 20 and 30 blocks), below the full 16. |
| `gun.boltaction.zoom` | bolt action rifle (M2) | A zoom intent is accepted: the server zoom state turns on, the FOV factor is 0.35 and the shot spread is 0.125 of 0.05; a second intent or a reload key turns it off. |
| `gun.boltaction.recipe` | bolt action rifle (M2) | `RecipeMaps.assemblerRecipes` holds the rifle at `RECIPE_LV` and 20 s; an LV query with the seven sheet inputs (barrel, Iron Receiver, stock, glass lens, plates, screws, spring) and circuit 4 finds it; a voltage below LV does not. |
| `gun.thompson_smg.auto` | Thompson SMG (M2 B1) | Holding the trigger for 13 ticks fires on the pull and then every interval (3 ticks, Mac10 2), one round and one bullet per shot. |
| `gun.thompson_smg.reload` | Thompson SMG (M2 B1) | An empty gun with one loaded magazine: the dry trigger starts the reload, the clip fills at the end of the reload time, the magazine is consumed and an empty one returned. |
| `gun.thompson_smg.reload.forced` | Thompson SMG (M2 B1) | The reload key on a part-loaded gun swaps the magazine and refunds whole loose bullets only (rounds / rounds-per-bullet, floored). |
| `gun.thompson_smg.recipe` | Thompson SMG (M2 B1) | The MV assembly row is in GT's assembler map at its time and voltage, and not below MV. |
| `gun.mac10.auto` | Mac10 (M2 B1) | Holding the trigger for 13 ticks fires on the pull and then every interval (3 ticks, Mac10 2), one round and one bullet per shot. |
| `gun.mac10.reload` | Mac10 (M2 B1) | An empty gun with one loaded magazine: the dry trigger starts the reload, the clip fills at the end of the reload time, the magazine is consumed and an empty one returned. |
| `gun.mac10.reload.forced` | Mac10 (M2 B1) | The reload key on a part-loaded gun swaps the magazine and refunds whole loose bullets only (rounds / rounds-per-bullet, floored). |
| `gun.mac10.recipe` | Mac10 (M2 B1) | The MV assembly row is in GT's assembler map at its time and voltage, and not below MV. |
| `gun.m4.auto` | M4 (M2 B1) | Holding the trigger for 13 ticks fires on the pull and then every interval (3 ticks, Mac10 2), one round and one bullet per shot. |
| `gun.m4.reload` | M4 (M2 B1) | An empty gun with one loaded magazine: the dry trigger starts the reload, the clip fills at the end of the reload time, the magazine is consumed and an empty one returned. |
| `gun.m4.reload.forced` | M4 (M2 B1) | The reload key on a part-loaded gun swaps the magazine and refunds whole loose bullets only (rounds / rounds-per-bullet, floored). |
| `gun.m4.recipe` | M4 (M2 B1) | The MV assembly row is in GT's assembler map at its time and voltage, and not below MV. |
| `gun.ak47.auto` | AK47 (M2 B1) | Holding the trigger for 13 ticks fires on the pull and then every interval (3 ticks, Mac10 2), one round and one bullet per shot. |
| `gun.ak47.reload` | AK47 (M2 B1) | An empty gun with one loaded magazine: the dry trigger starts the reload, the clip fills at the end of the reload time, the magazine is consumed and an empty one returned. |
| `gun.ak47.reload.forced` | AK47 (M2 B1) | The reload key on a part-loaded gun swaps the magazine and refunds whole loose bullets only (rounds / rounds-per-bullet, floored). |
| `gun.ak47.recipe` | AK47 (M2 B1) | The MV assembly row is in GT's assembler map at its time and voltage, and not below MV. |
| `gun.m4.zoom` | M4 (M2 B1) | A zoom intent turns the server zoom state on with FOV factor 0.75 and 0.75 of the 0.015 spread; a second intent or a reload turns it off. |
| `mp.visual.boltaction` | bolt action rifle (M2) | The visual walkthrough (first person, third person, inventory, and the zoomed scope view) for the rifle; see the autotest section. |
| `armor.showcase` | armor (M3) | Dresses a connected client in every armor set and camo in turn and screenshots it in third person (front for all, back for the five custom-model sets), with the HUD hidden; not part of `test all`; see "Armor showcase". |
| `gun.camo.cycle` | gun definitions | Every gun in `GunDefinitions.ALL` is a `CamoChangeable` with the definition's camo count; the Camo Bench routine (`MTECamoBench.cycleStack`) steps it forward through every skin and around, and back below zero, keeping the loaded rounds (GUN-01); a tag-less gun stays full; an out-of-range index stores 0. |
| `gun.accuracy.armor` | gun definitions | A fake player in a stub set giving `GUN_ACCURACY` 0.2 reads that bonus through `GunAccuracy` and shoots exactly 20 percent tighter than one in no armor (pistol bullet and Sawed Off pellets, same seeded rolls); with `modules.armor` off the bonus reads 0. |
| `gun.recipe` | pistol slice (M1) | `RecipeMaps.assemblerRecipes` (the map GT's NEI handler lists) holds the pistol at `RECIPE_LV` and 15 s; an LV query with the sheet inputs and circuit 1 finds it; a voltage below LV does not. |
| `armor.equip` | armor (M3) | A fake player wears the Scout set: speed attribute x1.5, full set detected, jump +0.16, vanilla bar 13; boots off gives x1.375 and no full set; naked clears the modifier; sprinting doubles the bonus on top of vanilla's sprint modifier; full Combat gives 0.40 knockback resistance. |
| `armor.damage` | armor (M3) | Full Combat set through the real damage handler and Forge `ApplyArmor`: 8 projectile loses 3.2, energy 4.4, poison 8, 15% penetration 3.92, unresistable and fall 8, a plain vanilla source uses the physical value; each piece wears once; broken pieces give nothing and never lose the last point. |
| `armor.lifecycle` | armor (M3) | Scout bonuses are stripped by the logout, death, respawn and dimension-change handlers and come back on the next tick; a broken boot drops its bonus and the full set. |
| `armor.compat` | armor (M3) | CORE-14: a step height of 1.25 survives equip and unequip. CORE-15: a 6000-tick Night Vision on a mob survives armor upkeep, and armor grants and removes only its own. Jump, fall (relative to the pack's baseline) and break-speed bonuses fire through the Forge event bus. |
| `armor.hazmat` | armor (M3) | Every catalog set is GT hazmat against exactly the hazards its four pieces list (T1 none; Commando ELECTRICAL; Riot RADIOLOGICAL; steam HEAT; HEV BIOLOGICAL, GAS and RADIOLOGICAL while uncharged); a test set with partial HEAT and RADIOLOGICAL flags is full hazmat through `GregTechHazmat` and `HazardProtection`, a helmet with a full GAS flag covers GAS alone, nothing claims SPACE or ELECTRICAL. |
| `armor.recipes` | armor (M3) | The 8 steam-age hand recipes and Heavy Cloth are in the crafting manager; the 4 LV Combat recipes are on `assemblerRecipes` at `RECIPE_LV` with the sheet's durations. |
| `armor.slots.goggles` | armor slots (M3) | Night Vision Goggles in the Baubles `head` slot: type `head`, night vision and exactly 2 EU/t only while on and charged; the key switches them off (no drain) and on; empty gives none; unequipped gives none. |
| `armor.slots.pouch` | armor slots (M3) | A worn Ammo Pouch in `quiver`: its magazines count with the main inventory, a 3-magazine removal empties the pouch first and persists in the stack NBT, and an unequipped pouch is not seen (the reload ammo hook). |
| `armor.slots.back` | armor slots (M3) | A test back device in `wings` ticks while worn, gets the action key, and is told once on removal; the fallback Oxygen Tanks add 20 air and 1 charge only with an Oxygen Mask worn (dev runs have no Galacticraft). |
| `armor.slots.masks` | armor slots (M3) | Gas Mask in the helmet slot is full GT gas hazmat, never SPACE, loses it with a spent filter and regains it renewed; Tactical Mask is gas hazmat, gives night vision and drains EU while charged, stays hazmat when empty, and is oxygen gear only while charged. |
| `armor.slots.recipes` | armor slots (M3) | Every special-slot item has an Assembler recipe at its tier voltage (`RECIPE_MV` or `RECIPE_HV`), and the Gas Mask and Oxygen Tanks refill recipes exist. |
| `armor.showcase.accessories` | visuals (M3) | Needs one connected client with the autotest hooks; not part of `test all`. Puts each worn accessory on Alice (first player if she is offline) and signals third-person front and back screenshots: Gas Mask, the four Tactical Mask camos, Night Vision Goggles, Oxygen Mask, Oxygen Tanks, Jump Pack, the five Anti Gravity camos and the Glider (stowed on the ground, and with wings out while falling). Evidence is the `acc-*` screenshots; read them. Items worn in Galacticraft slots are skipped with a note. |
| `armor.slots.automation` | armor slots (M3 exit gate) | A worn Ammo Pouch exposes no inventory, sided inventory or fluid handle (item and player classes), and hoppers either side of its wearer's cell neither push into nor pull from it over 60 ticks. |
| `armor.flight.glider` | armor flight (M3) | Glider in `wings`: with no sneak and no key nothing changes; sneaking or the held back-device key caps the descent near 0.18 blocks per tick (vanilla gravity simulated), pushes forward along the look direction and clears the fall distance every tick; no glide on the ground or while flying through another mod's ability, which it never writes; the fall after a glide is negated while a bare player keeps it; removal stops it and the tracker forgets the player. |
| `armor.flight.jump_pack` | armor flight (M3) | Jump Pack in `wings`: IC2 tier is the HV index and the transfer limit `GTValues.V[HV]`; a jump adds 0.8 and costs `flight.jumpPackJumpCostEu`; a 12 block fall (relative to the pack's baseline) loses 8 blocks and 20% of the rest for `flight.jumpPackFallCostEu`; a 3 block fall is free; an empty pack boosts and absorbs nothing and reports unpowered; the pack never clears another mod's `allowFlying`. |
| `armor.flight.anti_gravity` | armor flight (M3) | Anti Gravity Device in `wings`: tier IV with an IV transfer limit; off it does nothing; the key switches it on and 10 airborne ticks hold the fall speed near zero, clear the fall distance and drain exactly `flight.antiGravityDriveEuPerTick` each; another mod's `allowFlying` is untouched and `isFlying` never set; the fall right after powered flight is cancelled; `isFlying` from another source makes it defer with no drain; grounded costs nothing; running dry switches it off; removal stops it at once; 5 camos with out-of-range stored as 0. |
| `armor.flight.recipes` | armor flight (M3) | The Glider Backpack, Glider Wing, Glider and Jump Pack have Assembler recipes at `RECIPE_HV`; the Anti Gravity Device at `RECIPE_IV` once `part_mechanical_3` is registered, and no recipe while it is not. |
| `armor.t2.combat` | armor (M3 wave B) | Full T2 Combat: speed x1.4, jump +0.10, knockback 0.60, vanilla bar 20; 8 projectile loses 6.08 (19 armor), 8 energy 4.56 (14.25), poison unarmored; six camos (camo 4 is SWAT), bench-repairable; unequip clears everything. |
| `armor.t2.commando` | armor (M3 wave B) | Full Commando: speed x1.4, knockback 0.40, accuracy 0.20, oxygen gear; underwater mining x6 only in water; poison loses 4.8 (10), radiation 6.4 (5), projectile 18 armor; full GT ELECTRICAL hazmat and not RADIOLOGICAL; the accuracy ends on unequip. |
| `armor.t2.riot` | armor (M3 wave B) | Full Ranger Veteran: speed x1.5, knockback 0.70, penetration resistance 0.10; projectile at 15% penetration loses 1.92 (only 5% gets through), radiation 16 and ice 17 armor; full GT RADIOLOGICAL hazmat and not ELECTRICAL. |
| `armor.t2.beret` | armor (M3 wave B) | The Beret alone is a complete set: speed x1.1, vanilla bar 2, 8 projectile loses 6.0; camo 0 and 2 map to `t2_beret.png` and `t2_beret_green.png`. |
| `armor.t2.recipes` | armor (M3 wave B) | The 4 + 4 + 4 + 1 T2 Combat, Commando, Riot and Beret recipes are on `assemblerRecipes` at `RECIPE_MV` with the sheet's durations; the Riot recipes take the Combat piece at wildcard damage. |
| `armor.t3.combat` | armor (M3 wave B) | Full Advanced Combat: speed x1.4, knockback 0.80, vanilla bar 23; projectile loses 0.96 at 22 armor, and still 0.96 at 15% penetration (fully resisted); explosion 18, poison 10; camo 1 is silver. |
| `armor.t3.hev` | armor (M3 wave B) | HEV: empty chestplate gives no bonus and only passive GAS cover, protection stays; charged (10,000 units = 400,000 EU) gives speed x1.4 (x6.4 in water), knockback 0.60, jump 0.51, step assist, oxygen gear, full cover against HEAT, FROST, ELECTRICAL, GAS, BIOLOGICAL, RADIOLOGICAL and never SPACE; every type loses 1.28 of 8; cooling cancels lava and fire hits for 1 unit and puts a burning wearer out; walking costs 4 units a second, a jump 5, a fall over 3 blocks 5; discharged returns to unpowered and the chestplate is never broken. |
| `armor.t3.recipes` | armor (M3 wave B) | The 4 T3 Combat and 4 HEV recipes are on `assemblerRecipes` at `RECIPE_EV` with the sheet's durations. |
| `armor.steam.power` | armor (M3 wave B) | Steam armor: an empty chestplate gives no bonus but full protection (8 projectile loses 1.6 at 20 armor) and HEAT cover; 3,600 units give speed x1.2, +5 health, knockback 0.80, jump 0.24, mining x1.2, step assist; walking costs 4 a second, a jump 5, a fall over 3 blocks 5 and a 3-block fall is free; an empty chestplate burns a Compressed Air Tank from the inventory and returns an empty cell; a worn-out chestplate switches the set off; unequip restores 20 health. |
| `armor.steam.recipes` | armor (M3 wave B) | Steam Armor Plating and the four steam pieces are on `assemblerRecipes` at `RECIPE_EV` with the sheet's durations. |
| `armor.power.power_armor` | armor (M3 wave C) | Power Armor (IV): empty chestplate gives no bonus or cover but full protection (24 armor, 25% penetration resistance, bar 24); charged (3,600 units = 720,000 EU) gives speed x1.2, knockback 1.0, +5 health, jump 0.24, mining x1.2, step assist, full HEAT, FROST and ELECTRICAL cover, never GAS or SPACE; walking costs 4 units (800 EU) a second, a jump 5, a fall over 3 blocks 5; camo 1 is the Outcast sheet; the server writes no step height or potion (CORE-14, CORE-15); discharged returns to unpowered. |
| `armor.power.exo` | armor (M3 wave C) | Exo Suit (IV): empty gives only protection (22 armor, bar 23); charged (7,200 units = 1,440,000 EU) gives speed x1.8, knockback 0.80, jump 0.60, mining x1.3, step assist, no extra health, full HEAT, FROST and ELECTRICAL cover; a 12 block fall is halved and loses 5 blocks and costs 5 units; camo 2 is Dark Green; no step height or potion written; discharged returns to unpowered. |
| `armor.power.charger` | armor (M3 wave C) | M3 gate, GT chargers: the HEV (EV), Power Armor (IV) and Exo Suit (IV) chestplates are accepted by the GT Battery Buffer of their own tier and charge with EU over 60 ticks; the buffers one tier below and one above refuse them (`allowPutStack`) and a stack forced into them stays at 0 EU (design 0008). |
| `armor.power.recipes` | armor (M3 wave C) | The Power Armor and Exo Suit rows are on `assemblerRecipes` at `RECIPE_IV` with the sheet's durations (Exo takes the Advanced Combat piece at wildcard damage) exactly when Power Plating and Mechanical Parts T3 are registered; while either is missing no recipe exists and the scenario notes it. |
| `armor.power.slot_gate` | armor (M3 wave C) | M3 gate, special slots: the goggles name `head` and the pouch `quiver`; a charged chestplate and a filled Ammo Pouch survive a stack NBT round trip (the Baubles save format) with charge and contents intact. |
| `melee.knife` | melee (M2) | A fake player hits a zombie with the Combat Knife through `attackTargetEntityWithCurrentItem`: it loses the damage model's figure for 7.5 PHYSICAL damage, the hurt source is a GregGuns PHYSICAL source naming the player, the knife loses 1 durability and has 1000 uses. |
| `melee.crowbar` | melee (M2) | The same with the Crowbar: 5.5 PHYSICAL damage, durability loss 1 to 2, harvest level 2 for `pickaxe`, the `pickaxe` tool class, and it harvests iron ore. |
| `grenade.fuse` | grenades (M2) | A Stielgranate at rest (thrown by a zombie standing 2 blocks away) is live and harmless until 20 ticks before its 200 tick fuse, then explodes at tick 200; zombies at 1.5, 5 and 8 blocks lose `BlastFalloff` damage (full, ramp, none) through the damage model, the thrower inside the radius is hurt too (PFX-25), the floor is intact with `explosions.blockDamage` off. Waits about 10 s. |
| `grenade.bounce` | grenades (M2) | A grenade dropped from 3 blocks bounces (bounces left falls, still live), then lies on the floor with no vertical speed instead of spending every bounce at once. Waits for ticks. |
| `grenade.protected` | explosions (M2) | `ProjectileExplosion` trials: block damage off breaks nothing but still hurts; on breaks the floor with the same entity damage; a cancelled `ExplosionEvent.Start` stops everything; a `Detonate` listener that clears the block list keeps every block and does not change entity damage (GGS-04-10, 11); cancelled `BlockEvent.BreakEvent`s (one per block for a player owner, GGS-04-12) keep the blocks; `explosions.selfDamage` off spares the owner. |
| `grenade.frag` | grenades (M2) | A Frag Grenade at rest explodes: zombies at 1.5 and 5 blocks lose at least the 25 damage / radius 6 blast figure through the damage model, the near one more than the far one, the near one also takes fragment damage on top of the blast, and the floor is intact. |
| `grenade.flash` | grenades (M2) | A Flashbang at rest goes off: a fake player facing it 4 blocks away would get about 80 ticks of Blindness (40 of Nausea), one with their back to it none (measured with `FlashBlast.intensityAt`: a fake player has no connection for the potion packet); a cow at 2 blocks gets Slowness IV and Weakness, one at 6.5 blocks Slowness II, one behind a stone wall and one past 8 blocks nothing; no cow loses health. |
| `grenade.flash.noblocks` | grenades (M2) | With `explosions.blockDamage` on, a Flashbang beside a stone pillar, a glass pane and dirt changes no block in the surrounding box, hurts no cow, and still slows it. |
| `utility.plates_recipes` | turret armor plates (M6) | The four plates are registered with armor values 5, 10, 15 and 20; Iron has a crafting-table recipe and no Assembler recipe; Steel, Aluminum and Carbon each have one Assembler recipe at their tier's EU/t and 10 s, with one input (two for Carbon) and no hand recipe. Needs `GG_MODULES=guns,armor,machines,utility`. |
| `utility.charge_bores_stone` | explosive charges (M6) | An armed Explosive Charge counts a 60-tick fuse down, removes itself, bores a 3x3x3 tunnel into a stone mass, drops items and leaves the stone around and below. Needs `GG_MODULES=guns,armor,machines,utility`. |
| `utility.charge_gt_ore_drop` | explosive charges (M6) | A charge bores a GT iron ore block and the drops equal `getDrops` at fortune 0 (GGS-09-13). |
| `utility.charge_respects_cancel` | explosive charges (M6) | A listener cancels two columns; each stops at its block, nothing past a canceled block breaks, and no break event has a null player (GGS-09-11, MUTL-30). |
| `utility.charge_offline_owner` | explosive charges (M6) | With the owner offline the events carry a FakePlayer with the owner's UUID; an ownerless charge still gets a real player (GGS-09-10). |
| `utility.charge_foreign_button` | explosive charges (M6) | `PacketChargeButton` from a non-owner is rejected (fuse and arm), an unknown id is rejected, the owner's press is applied (MUTL-31). |
| `utility.turret_registered` | Turret (M6) | The four Turrets are `MTETurret` at 20116 to 20119 with the right tiers; the entity is registered as `gregguns.turret`, is not `IMob`, is `IEntityOwnable` and has 30 max health. |
| `utility.turret_kills_zombie` | Turret (M6) | A powered MV Turret with a 9mm Pistol (one round loaded) and four magazines shoots a fire-resistant zombie dead; exactly one empty magazine is handed back and three remain. |
| `utility.turret_spares_owner` | Turret (M6) | At security 0 no player is a target; at 2 the owner and a stub friend are spared, a stranger and a zombie are shot; at 1 without friends the old friend is shot (GGS-09-04); animals follow the Target Animals toggle; only the owner may cycle the security level. |
| `utility.turret_no_npe` | Turret (M6) | Killing the entity starts the repair and it respawns 200 ticks later; a link to a zombie's UUID respawns a new turret; a turret with no base dies quietly (NPC-12); a malformed saved UUID reads as unlinked; breaking the base kills the turret. No exception anywhere (MUTL-25). |
| `utility.chest_roundtrip` | chests (M6) | Both chests store, stack and return items; the Weapon Chest refuses dirt, vanilla items and item blocks and takes a GregGuns item; NBT round trip; the GUI dispatcher gives a chest container; shift-click respects the filter; the viewer count rises on open and falls on close; neither chest gives redstone power. |
| `utility.bioblob_lifecycle` | Bio Blob (M6) | A size-3 blob bursts on a hit and a neighboring stone survives; a projectile on the floor places a size-1 blob with the face as metadata; a power-2 hit makes size 3; then one decay step per `bioBlobDecayTicks` (3 to 2 to 1 to gone). |
| `utility.bioblob_poison` | Bio Blob (M6) | A creeper (zombies are poison-immune) and a player get Poison III on contact; `bioBlobSparesPlayers` spares the player but not the zombie; the collision box follows the size. |
| `utility.campflag_spawns` | Camp Flag (M6) | With a counting stub port, an initialized flag spawns once per 200 ticks up to its cap of 2 at its configured points; an uninitialized flag spawns nothing; the cap survives NBT; the default cap formula gives 24 for 32 x 32. |
| `utility.campflag_break_drop` | Camp Flag (M6) | Breaking a flag drops exactly one iron ingot (damage 0) and calls the break port once; with `campFlagBreakHelicopter` off the port is not called. |

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
| `mp.flashbang` | visuals (M2) | Needs one connected client; not part of `test all`. Gives the first player 16 Frag Grenades and 16 Flashbangs, signals a screenshot of each held item, throws a Flashbang through the item's release path (gentle lob, fuse 40 ticks, one screenshot in flight), sets one off two blocks in front of the player and signals screenshots of the white-out, two stages of the fade and the blind aftermath (the client logs `AUTOTEST flash white-out <n> ticks`), then throws a Frag Grenade with player damage off. |
| `mp.give` | items and tooltips (M1) | Needs one or two connected clients. Gives each player every registered GregGuns item and the LV Ammo Press one at a time by direct server-side insert (`InventoryGrant`: `addItemStackToInventory`, slot fallback, container sync, server-side check; vanilla `/give` only drops an entity whose pickup is racy) (inventory, armour and Baubles slots cleared first, item in the selected hotbar slot), has the client wait until it holds the item, build its tooltip and draw it for a few ticks, and confirm to the server with a hidden chat ack; the next item only starts after every client confirmed (100-tick timeout naming the item); then repeats through the client-side creative slot packet. Any exception in a log fails the orchestrator run. Waits for ticks. |
| `guns.inventory_grant` | server-side give helper | Headless. A fake player receives every give-list stack through `InventoryGrant` and must carry it the same tick; a full inventory must report failure. |
| `mp.slots` | armor slots (M3 exit gate) | Needs clients named Alice and Bob; not part of `test all`. Alice gets goggles (on) in `head`, an Ammo Pouch (7 bullets, 2 magazines) in `quiver` and a Glider in `wings`; her empty pistol reloads from the pouch (one magazine left). Both clients are signalled `slots Alice` and answer with `[gg-report]` chat lines (`AutotestReport`, swallowed by a dev server handler); every slot must equal the server's `SlotSyncProbe` description (item, count, goggles switch, pouch inner NBT), re-asked every 40 ticks up to 5 rounds. The expected state is stored in Alice's entity data. Run with `GG_MP_RELOG=1` for the next row. |
| `mp.slots.after_relog` | armor slots (M3 exit gate) | Runs after `mp.slots` once Alice's client was killed and rejoined (looks her up by name). The server's copy must equal the recorded state and the literals (goggles on, 7 bullets, 1 magazine, Glider), and the new client (and Bob's) must report the same. Fails with a clear message when Alice is not online. |

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

Signals run strictly in order on the client, and a `shot` waits for four rendered frames before the next command, so a GUI that stalls the client is still drawn and photographed before `close`. Signal commands: `view <0|1|2>` (first, third back, third front), `shot <label>` (screenshot at the end of the next frame), `arm <shot|reload> <label> <delayTicks>` (screenshot that many ticks after the next shot or reload event the client sees), `fire <n>` (hold the fire button for two ticks, n times, six ticks apart, through `GunInputHandler`, which also works without window focus), `gui inventory | creative <tab> | press <x> <y> <z> | nei <item>`, `hold <item>` (log the held stack and build its tooltip), `hud <on|off>` (off hides the GUI layer for clean worn-model shots), `creativegive <modid:name[:meta]>` (client-side creative slot packet), `close`, `quit`. Screenshots use vanilla `ScreenShotHelper` into `run/client/screenshots/<player>-<label>.png`.

### Armor showcase

`armor.showcase` (dev only, one client) settles how every worn model looks in game, including the 128 x 64 vanilla-layout sets. It sends `hud off`, then for each set of `ArmorCatalog` and each camo: equips the player (creative mode, so nothing hurts), `view 2` (third person from the front), `shot armor-<set>-<camo>-front`, and for the sets with a custom model also `view 1` and `shot armor-<set>-<camo>-back`; at the end it restores the first-person view, the HUD and survival mode. The timetable is `ShowcasePlan` (pure, unit-tested). The player is the first connected one by name, or the one named by `-Dgregguns.showcase.player=<name>` on the server.

Exact command (server worktree on the same commit as the client; the orchestrator runs it under the client lock):

```sh
# 1. server: GG_MODULES=guns,armor,machines, offline mode, flat peaceful world; one client (Alice) with
#    -Dgregguns.autotest=a and -Dgregguns.autotest.connect=127.0.0.1:<port> (see below), 960 x 540.
# 2. when the client log has "AUTOTEST joined Alice", on the server console:
gregguns test armor.showcase
# 3. wait for the scenario's PASS line (about 1,000 server ticks, under a minute), then:
say [gregguns-autotest] quit
```

Screenshots land in `run/client/screenshots/Alice-armor-<set>-<camo>-<front|back>.png`, for example `Alice-armor-steam-2-back.png` and `Alice-armor-t1_scout-0-front.png`. Read them: the script only proves the files were written. Look for: on the 128 x 64 sets, the helmet on the head, the chest on the torso and the belt line at the waist; on the five custom sets, the silhouette described in ARM-DEV-24.

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

It boots the server (flat, peaceful world; both users op), boots the two clients (users `Alice` and `Bob`), waits for `AUTOTEST joined` in both logs, runs `gregguns test mp.sync` and `gregguns test mp.visual` on the server console, checks the logs for the evidence lines, then shuts down (the clients quit on a `quit` signal, the server on `stop`; leftovers started from those checkouts are killed by command line). The orchestrator runs `mp.sync`, `mp.visual` and `mp.give` (every registered GregGuns item and the Ammo Press by direct server-side insert, then through the creative slot packet, each held for a few ticks so tooltip, HUD and renderer run). It aborts at once, with the exact line, on any failure signature in a log (`Connection Lost`, `Mod rejections`, `fatal error`, `lost connection`, `Crash Report`, `Exception`, `Caused by:`, `at com.gregguns`). Known noise it ignores: GT's NEI handler cache race in worker threads (`NEI-worker-thread`, not mentioning gregguns). Output in `<out-dir>`: `server.log`, `client-a.log`, `client-b.log`, `*-autotest.txt` (only the AUTOTEST lines), `summary.txt` and `shots/*.png`. Exit code 0 means both scenarios passed and every expected log line was present. Allow 10 to 15 minutes, most of it booting.

What a screenshot proves is judged by looking at it: the script only proves a file was written. Read the PNGs.
