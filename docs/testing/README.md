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
- **Server thread, synchronous:** a scenario cannot wait for ticks. To test tick behaviour, call the logic's update method directly.
- **No player:** `ctx.player()` is `null` on the console. A scenario that needs a player must say so and fail clearly, or build a fake one.
- Add a row to the table below in the same PR.

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
