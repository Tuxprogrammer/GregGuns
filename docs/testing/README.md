# In-game scenario tests

Scenarios are reproducible in-world situations with a checked outcome ([CONTRIBUTING § Testing](../../CONTRIBUTING.md#testing)). They exist only in the **dev environment** (`runClient` / `runServer`); the command is not registered in a released jar.

## Running scenarios

### In game (op only)

```
/gregguns test list          # every scenario and its description
/gregguns test <scenario>    # one scenario, origin at your feet
/gregguns test all           # every scenario
```

Tab completion offers `all`, `list` and the scenario names. The player must be op (permission level 2).

### Dedicated-server console

Type the same commands without the slash into the `runServer` console:

```
gregguns test all
```

There is no player, so the origin is the surface at the overworld spawn. The output ends with a line you can grep:

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
- **Clean up:** everything made through `ctx.spawn`, `ctx.setBlock` and `ctx.clearArea` is undone by the runner, even if the scenario throws. Do not change the world any other way. `clearArea` does not preserve tile entities, so scenarios run where there are none (the spawn area).
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
- Scenarios run strictly one after another, because they share the origin area. The next one starts in the same tick the previous one finishes. Synchronous scenarios behave exactly as before.
- The console summary line and the chat report are printed once, after the last scenario of the run has finished. While a run is waiting, a second `gregguns test` is refused (`gregguns.test.busy`).
- In plain Java: `ScenarioRunner.begin(list)` returns a `ScenarioRun` that you call `tick()` on; `ScenarioRunner.run(list)` is the old synchronous form and fails any scenario that asks to wait. `ScenarioTicker` (server side) drives `ScenarioRun`s from the FML server tick for the command.

The framework logic (registry, argument parsing, runner, report) is plain Java and has unit tests in `src/test/java/com/gregguns/core/testing/`.

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
| `projectile.hit` | projectile framework | A 9mm (`GunCatalogue.PISTOL`) shot fired through `Projectiles.launcher()` from a fake player skips a dead zombie in front and takes the live zombie behind it down by the damage model's figure for 8 damage; the bullet is gone and the entity is registered (PFX-01). Waits for ticks. |
| `projectile.falloff` | projectile framework | The same shot at a zombie 21 blocks away (past the 18 block drop-off start): health lost equals `DamageFalloff` at the measured distance run through `DamageModel`, strictly between the maximum and the minimum. Waits for ticks. |
| `projectile.blocks` | projectile framework | A bullet flies through tall grass (PFX-27), ends at a stone wall without leaving an entity behind, and the zombie behind the wall is unhurt (PFX-23). Waits for ticks. |
| `projectile.shooter` | projectile framework | A pig fires from inside its own box: the shooter is unhurt (grace period) and a pig three blocks away takes the full damage. Waits for ticks. |
| `gun.fire` | pistol slice (M1) | A fake player holding a real `pistol_9mm` presses the trigger through `ServerGunHandler`: one bullet is launched, a zombie 6 blocks away loses what `DamageModel` gives for 8 damage, the NBT `ammo` goes 18 to 17, a second click 1 tick later is swallowed, and a click made 3 ticks later fires on tick 4 (4 tick cooldown, GUN-29 buffer). Waits for ticks. |
| `gun.reload` | pistol slice (M1) | An empty pistol with a `mag_9mm_pistol` in the inventory: the dry trigger starts a reload, ammo stays 0 until the 35th tick, then 18; the magazine is consumed and a `mag_9mm_pistol_empty` comes back. Waits for ticks. |
| `gun.reload.forced` | pistol slice (M1) | The reload key on a pistol holding 7 rounds: after 35 ticks it holds 18, the loaded rounds come back as `floor(7/6)=1` loose bullet (GUN-13; the 1 leftover round is the documented whole-bullet rounding) and the empty magazine is returned. Waits for ticks. |
| `gun.dryfire` | pistol slice (M1) | An empty pistol with no magazines or bullets, trigger pulled twice: no bullet, no reload, ammo stays 0, nothing throws. Waits for ticks. |
| `gun.player` | pistol slice (M1) | A shot from one fake player hits a second fake player 6 blocks away and costs them the `DamageModel` figure (the victim sits in its chunk only, because a fake player has no network handler for the entity tracker). Waits for ticks. |
| `gun.zoom` | pistol slice (M1) | A zoom intent on the unscoped pistol through `ServerGunHandler.onZoomToggle`: accepted, zoom state stays off, FOV factor stays 1, rounds untouched. |
| `gun.recipe` | pistol slice (M1) | `RecipeMaps.assemblerRecipes` (the map GT's NEI handler lists) holds the pistol at `RECIPE_LV` and 15 s; an LV query with the sheet inputs and circuit 1 finds it; a voltage below LV does not. |

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
| GUN-16 pistol drop-off and muzzle set twice | U: `GunCatalogueTest.doesNotReproduceGUN16_*` | |
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
| GUN-38 knockback ignores strength | Not in M1: knockback is not implemented yet | Add the test when knockback lands |

