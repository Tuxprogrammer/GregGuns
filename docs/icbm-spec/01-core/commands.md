# Commands

Source root: `src/main/java/icbm/classic/command/` at `9c1d985a`. Classes: `ICBMCommands`, `CommandUtils`; `system/` (`CommandEntryPoint`, `CommandGroup`, `ICommandGroup`, `ISubCommand`, `SubCommand`); `sub/` (`CommandHelp`, `CommandLag`, `CommandRemove`); `sub/blast/` (`CommandBlastList`, `CommandBlastTrigger`, `CommandBlastSpread`).

The command base classes (`CommandBase`, `ICommandSender`) are *1.12-only mechanisms*; the 1.7.10 equivalent is `CommandBase` with a similar shape. The behavior below is what players see.

## 1. Structure

One vanilla command, `/icbm`, registered when the server starts. Inside it is a tree of groups and subcommands that ICBM manages itself:

```
/icbm
  help                      (default for anything unknown)
  remove  <type> ...
  lag     [radius]
  blast
    help
    list
    trigger <id> ...
    spread  <count> <distance> <id> <dim> <x> <y> <z> <scale>
```

| Item | Behavior |
|---|---|
| Permission | `/icbm` requires permission level 2 (command-block / op level), checked by the vanilla layer. **There is no per-subcommand check**, so `lag`, `remove` and `blast trigger` all share level 2 |
| Dispatch | The first argument selects the subcommand (case-insensitive). With no argument, or an unknown name, the group runs `help` |
| `help` | Prints the usage line(s) of every subcommand in the group. Each subcommand gives up to three help variants: "for all", "world only" (printed only if the sender is an entity), "server only" (printed only if the sender is the server console) |
| Tab completion | Group level completes subcommand names; `remove` completes `all`, `missile`, `explosion` for the first argument only; other subcommands give no suggestions |
| Usage errors | Wrong argument counts throw the vanilla `WrongUsage` error with lang key `command.icbmclassic:icbm.error.unknown.command` = "Unknown command. See %s help for more info." |
| Relative positions | `~` and `~<number>` work for dimension, x, y, z in the long forms; they fail from the console with "'~' can't be used from console" |
| Dimension argument | An integer dimension ID, or `~` for the sender's world. Unknown IDs throw "Dimension with ID[...] was not found!" |

All numeric parsing outside of `<count>` and `<distance>` uses Java's raw parsers. A non-number throws a `NumberFormatException` that the server reports as a generic command exception rather than a usage message.

## 2. `/icbm blast list`

No arguments. Sends one chat line: "Explosive Types: " followed by every registered explosive's registry key (for example `icbmclassic:condensed`) sorted alphabetically and joined with ", ". The string is not translated (TODO in code). Works for any sender.

## 3. `/icbm blast trigger`

Creates and starts a blast through the same path the explosive blocks and missiles use: look up the explosive, build its action with an `AREA_SIZE` field equal to the size, run it with an anonymous action source. See [actions framework](actions-framework.md) and [02-explosives](../02-explosives/).

| Form | Arguments | Notes |
|---|---|---|
| Long (6 args) | `<id> <dim> <x> <y> <z> <size>` | Usable from the console. Position uses the exact `x/y/z` values (not block-snapped) |
| Short (2 args) | `<id> <size>` | Only when the sender is not the console. Blast is centered on the sender's exact position in the sender's world |

- `<id>`: a registry name such as `icbmclassic:nuclear` (a missing domain defaults to `minecraft`, so a bare `nuclear` fails). Unknown IDs throw lang `command.icbmclassic:icbm.blast.error.explosive.id` = "Explosive with ID '%1$s' could not be found".
- `<size>`: float. Must be greater than zero, else `icbm.blast.error.scale.zero` = "Scale must be greater than zero!". There is **no upper bound**, no config cap, and no dimension permission (ICBM-01-13).
- Result: the status of the action is sent back as a chat component (`result.getTooltip()`), for example a blocked-by-event, threaded-queued or triggered message. Related lang keys (existing but unused in this class): `icbm.blast.triggered`, `.threaded`, `.error`, `.error.blocked`, `.error.null`, `.error.triggered`, `.error.unknown`.

## 4. `/icbm blast spread`

`<count> <distance> <id> <dim> <x> <y> <z> <scale>`: exactly 8 arguments, usable from the console. Builds a square grid of blasts centered on the position.

- `count`: integer >= 1 (vanilla `parseInt` with minimum 1, no maximum). `distance`: integer >= 1 (blocks between neighbors).
- Grid: offsets `-count..+count` on X and Z, step `distance`, same Y. Number of blasts = `(2 * count + 1)^2` (the chat header states this number).
- Relative `~` coordinates use the **block position of the sender plus 0.5** (so they snap to a block center), unlike `trigger` which uses the exact position.
- `scale`: float > 0 (same rule and message as `trigger`).
- Output: first a header, `command.icbmclassic:icbm.spread.started` = "Triggering %10$d blasts with explosive %1$s and scale %2$f at [%3$s@%4$d, %5$d, %6$d, %7$d] with a spread of %8$d over a distance %9$d."; then one chat line per blast with its action status.
- No cap on `count` or total blasts: `count = 100` queues 40,401 blasts in one tick (ICBM-01-13).

## 5. `/icbm remove`

Forms (usage text from the help output):

| Form | Arguments | When |
|---|---|---|
| Long | `<type> <dim> <x> <y> <z>` and optionally `<radius>` as a sixth argument | any sender; the position arguments are 5 or 6 total after `remove` |
| Short | `<type>` or `<type> <radius>` | only when the sender is not the console |

`<type>` (case-insensitive): `all`; `missile` or `missiles`; `explosion`, `explosions`, `explosive`, `explosives` or `ex`.

| Type | Matches (living entities only) |
|---|---|
| `all` | any "ICBM entity": fragments, flying blocks, missiles (anything with the missile capability), bomb droplets, explosive entities, `EntityExplosion` blast entities, grenades, red matter |
| `missiles` | any entity with the missile capability |
| `explosions` | only the primed-explosive entity type (`EntityExplosive`); **not** the running-blast entity `EntityExplosion` (ICBM-01-12) |
| anything else | matches nothing (no error is printed; it still reports "Removed 0") |

Radius: parsed with vanilla `parseInt`. **Omitted or zero or negative means the whole world**: every entity in the loaded entity list of that dimension. Positive radius means the axis-aligned cube of half-size `radius` around the point (not a sphere). Matching entities are set dead. Output lang `command.icbmclassic:icbm.remove` = "Removed %1%d entities within %2$d meters"; the format is malformed so the count never appears (ICBM-01-11).

Note: the long form uses the radius only as argument 6; with 5 arguments the radius is global. The short form with exactly 2 arguments reads the second as the radius.

## 6. `/icbm lag`

Emergency cleanup. Removes all ICBM entities (same "all" selector as above) and **cancels queued or running blast work** near the sender (`WorkTickingActionHandler.removeNear`, see [actions framework](actions-framework.md)).

- Help text: `[radius]`, shown only to entity senders. Default radius 1000.
- The radius is read from the wrong argument index (the second argument after `lag`, not the first), so `/icbm lag 50` silently uses 1000 and `/icbm lag 50 x` would parse `x` (ICBM-01-10).
- Center: the sender's position. From the console that is the world origin.
- Output: lang `command.icbmclassic:icbm.lag.remove` = "Removed %1$d blast and %2$d entities within %3$d meters" (blast count, entity count, radius).

## 7. For the GTNH rebuild

- A single op command with `remove`, `lag` and `blast trigger/spread` is enough for admins and for testing. Keep the blast list for debugging.
- Add a size cap and a count cap (config) to `trigger` and `spread`.
- The original has one shared permission level for every subcommand; split them if the pack has a permission mod.
