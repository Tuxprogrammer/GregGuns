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
        ctx.spawn(dummy);                       // removed automatically afterwards
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
| `combat.types` | damage system | Each of the ten damage types hits an unarmoured pig through a real source: health lost equals the damage, and the type's secondary effect (fire, slowness, poison, radiation sickness, shock) appears only where it should. |
| `combat.penetration` | damage system | A mob in full iron armour (15 points) takes 8 damage at 0/15/100% penetration as 3.2 / 3.92 / 8.0 (GUN-34), every blockable type alike (GUN-45), unresistable in full; the source keeps the shooter and the projectile (GUN-37). |
| `combat.player` | damage system | A fake player in full iron armour takes the same 3.92 through Forge `ApplyArmor`, each armour piece is worn exactly once (armour not applied twice), and unresistable skips armour. |
| `combat.iframes` | damage system | Melee 6 then 8 inside the frames loses 8, not 14 (GUN-33); ignoring sources land every time and leave the frames as they found them. |
| `combat.listeners` | damage system | A foreign `LivingAttackEvent`/`LivingHurtEvent` listener sees our source once, and the raw pre-armour amount at HIGHEST priority (GGS-02-18). |
| `combat.hazmat` | damage system | A pig in a full stub-hazmat set (GT `IHazardProtector`) takes the direct damage but none of the fire, ice, poison, radiation or shock effects (GGS-02-22). |
| `production.ammopress` | production chain (M1) | The LV Ammo Press (MTE 20101) is registered as an `MTEAmmoPress` at LV on `gregguns.ammoPress`, has an item form, and places as a GT machine block. |
| `production.recipes` | production chain (M1) | The ammo map finds RC-28, RC-37 and RC-44 at `RECIPE_LV` with the sheet's counts and durations, and the assembler holds RC-05 and RC-11 at LV. |
| `projectile.hit` | projectile framework | A 9mm (`GunCatalogue.PISTOL`) shot fired through `Projectiles.launcher()` from a fake player skips a dead zombie in front and takes the live zombie behind it down by the damage model's figure for 8 damage; the bullet is gone and the entity is registered (PFX-01). Waits for ticks. |
| `projectile.falloff` | projectile framework | The same shot at a zombie 21 blocks away (past the 18 block drop-off start): health lost equals `DamageFalloff` at the measured distance run through `DamageModel`, strictly between the maximum and the minimum. Waits for ticks. |
| `projectile.blocks` | projectile framework | A bullet flies through tall grass (PFX-27), ends at a stone wall without leaving an entity behind, and the zombie behind the wall is unhurt (PFX-23). Waits for ticks. |
| `projectile.shooter` | projectile framework | A pig fires from inside its own box: the shooter is unhurt (grace period) and a pig three blocks away takes the full damage. Waits for ticks. |

| `mp.sync` | multiplayer (M1) | Needs two connected clients. Players A and B (by username) face each other four blocks apart. A's client fires twice through the real input path and trigger packet, the server reloads A's pistol through `ServerGunHandler.onReloadKey`, and the scenario checks that a bullet was launched, B took damage, and A's rounds and magazines changed. Client B logs and screenshots what it sees. Waits for ticks. |
| `mp.visual` | visuals (M0, M1) | Needs one connected client. Signals the first player to take screenshots of the first-person pistol with ammo HUD, third person, a dropped pistol, the inventory, the creative GregGuns tab, the LV Ammo Press GUI and NEI's recipe view for the pistol and the bullet. Passes once the signals are sent; the evidence is in the client's log and screenshots. Waits for ticks. |

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

Signal commands: `view <0|1|2>` (first, third back, third front), `shot <label>` (screenshot at the end of the next frame), `arm <shot|reload> <label> <delayTicks>` (screenshot that many ticks after the next shot or reload event the client sees), `fire <n>` (hold the fire button for two ticks, n times, six ticks apart, through `GunInputHandler`, which also works without window focus), `gui inventory | creative <tab> | press <x> <y> <z> | nei <item>`, `close`, `quit`. Screenshots use vanilla `ScreenShotHelper` into `run/client/screenshots/<player>-<label>.png`.

### Launching a client that joins a server by itself

The GTNH buildscript's `runClient` task (RetroFuturaGradle) takes everything on the command line, so nothing needs editing:

```sh
./gradlew runClient --username=Alice \
    --mcJvmArgs=-Dgregguns.autotest=a \
    "--mcArgs=--server 127.0.0.1 --port 25613 --width 960 --height 540"
```

- `--username` overrides `developmentEnvironmentUserName` for this run (the offline-mode name; the server must have `online-mode=false`).
- `--mcArgs` goes to Minecraft's own launch arguments: `--server`/`--port` join at startup, `--width`/`--height` size the window.
- `--mcJvmArgs` adds JVM system properties.
- The run directory is `run/client` of the project directory, and **two Gradle builds cannot run at once in one project directory**. Use one git worktree per process: one for the server, one per client (all at the same commit).
- The client needs `modules.guns` on in its own `run/client/config/gregguns.cfg` (the orchestrator writes it), and `pauseOnLostFocus:false` in `options.txt` so an unfocused window keeps ticking.

### Running the whole check

```sh
python -I build/mp_check.py <server-worktree> <client-a-worktree> <client-b-worktree> [port] [out-dir]
```

It boots the server (flat, peaceful world; both users op), boots the two clients (users `Alice` and `Bob`), waits for `AUTOTEST joined` in both logs, runs `gregguns test mp.sync` and `gregguns test mp.visual` on the server console, checks the logs for the evidence lines, then shuts down (the clients quit on a `quit` signal, the server on `stop`; leftovers started from those checkouts are killed by command line). Output in `<out-dir>`: `server.log`, `client-a.log`, `client-b.log`, `*-autotest.txt` (only the AUTOTEST lines), `summary.txt` and `shots/*.png`. Exit code 0 means both scenarios passed and every expected log line was present. Allow 10 to 15 minutes, most of it booting.

What a screenshot proves is judged by looking at it: the script only proves a file was written. Read the PNGs.
