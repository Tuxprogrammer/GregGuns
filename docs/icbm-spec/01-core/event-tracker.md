# Event Tracker

Source root (relative to `src/main/java/icbm/classic/` at `9c1d985a`): `lib/tracker/` (9 classes, all read in full). Users of it, read to see what it actually records: `lib/network/PacketEvents`, `lib/network/lambda/PacketEventUtils`, `lib/network/lambda/tile/PacketTileEvents`, and the single instance `ICBMClassic.MAIN_TRACKER`.

## 1. What it is and why

The event tracker is a **read-only observation channel**: code posts typed, structured log events, and listeners (loggers, admin tools, external sinks) consume them. It exists beside the Forge event bus, not in place of it. The design notes in the source say:
- Mods that want to *react to or change* the game must use the Forge events ([public-api.md](public-api.md)). The tracker never hands back mutable game objects, so a listener cannot alter game state.
- Main use cases: audit what players did (clicks, triggers, launches, including long chains such as "built a launcher, days later triggered it by redstone"), record launches and failures, and give developers a trace of what a missile or machine did at a given method without a debugger. The author compares it to feeding a log-aggregation service.
- It is explicitly **never** for anonymous user tracking.

In the shipped mod it is almost unused: the one tracker instance (`MAIN_TRACKER`) only receives four *packet-handling* diagnostics (section 5). No missile, launcher or blast posts to it. The audit and chain features are intent, not behavior.

## 2. Data model

| Class | Role |
|---|---|
| `EventTracker` | A set of global listeners plus the posting logic. Methods: `listen(name, listener)`, `post(type, dataSupplier)`, `post(type, conditionSupplier, dataSupplier)`, `post(entry)`, `consumes(type)` |
| `EventTrackerType` | The kind of event: a registry name (`ResourceLocation`), a severity (plain, warn or error; setting one clears the other), an ordered list of **fields** (columns), and a list of **type-level listeners** (attached by the event's author, always run) |
| `EventTrackerField<T>` | One column: a key, a Java type, a position index, a back-reference to its type, and an accessor that pulls the value out of an entry's raw data array. `copy()` lets types share common columns (player name, position) |
| `EventTrackerEntry` | One occurrence: its type and the raw `Object[]` data. `get(key)` reads a column through its accessor; `getString(key, fallback)` formats it |
| `IEventTrackerListener` | `accept(entry)` and a `consumes(type)` filter (default: everything) |
| `ListenerEntry` | A named listener with an optional enable condition; wraps the callback so one failing listener cannot stop the others |
| `EventTrackerData` | The "is this payload read-only" validator (section 4) |
| `EventTrackerHelpers` | Shared column keys and accessors: `tile.pos` (x, y, z), `level.id`, `level.name`, `level.side` (`client` or `server`), and lookup helpers for world id, world name and side |
| `EventTrackerNode` | A planned "chain of events" holder (start, a list of steps, end) for tracing a method. Declared but unused |

Builder (`EventTrackerType.Builder`): `with` / `withString` / `withInt` / `withDouble` add columns; `withBlockPos(prefix, accessor)` and `withVec3d(prefix, accessor)` expand one value into three columns suffixed `.x`, `.y`, `.z`. `asWarn()` / `asError()` set severity, `listen(...)` adds a type-level listener, `build()` freezes the lists and assigns each field its index and parent type. The source leaves a note to fire an event so other mods can add columns and listeners; it does not exist.

Column key convention (from the source comments): the lang key of a column is built from `event.` plus the type name plus the column key, with the column domain included only when it differs from the type's. For example the display name key looks like `event.<type>.<column>.column` and a database column name like `...sql=<name>`. No such lang entries ship.

## 3. Posting behavior

1. `post(type, supplier)` first asks whether anyone wants the event: `consumes(type)` is false if there are no global listeners and the type has none, otherwise true if any listener says it consumes that type. **The data supplier is not called when nobody listens**, which keeps debug posts cheap.
2. The supplier builds the data array. Each non-null element's class is validated (section 4). If any element is rejected, the event is dropped with a logged error and a stack trace saying it is a developer bug in the type's domain.
3. The entry goes first to the tracker's global listeners and then to the type's own listeners; each is skipped if its filter says no.
4. A listener that throws is caught per listener and logged (without the exception detail, ICBM-01-66). Listeners may be called from any thread, so they must be thread safe.
5. The conditional form `post(type, condition, supplier)` posts only when a boolean supplier says so (typically "debug mode is on").

## 4. The read-only check

Goal: nothing mutable reaches a listener. A class is accepted when:
- it is a `Throwable` (treated as effectively immutable), or
- it is on the valid list: `Integer`, `Double`, `Float`, `Long`, `Byte`, `Short`, `String`, `Date`, `Object`, `BlockPos`, `Vec3d`, `ResourceLocation`; or
- it passes a **structural scan**: its superclass chain is valid, and every declared field (static ones too) is `final` and of a valid type. Fields whose type is currently being scanned (cycles) are skipped as valid.

Known-bad exact classes: `Entity`, `World`, `NBTBase`. (Subclasses are not listed; they fail the scan because they have mutable fields.) Results are cached in a valid set and an invalid set. Weaknesses: interfaces and arrays have no declared fields so they always pass even when mutable (a `List`, a `Map`, `Object[]`), `Date` is on the valid list although it is mutable, `Boolean` and `Character` are not on the list and most likely fail the scan (their static `TYPE` field points at `Class`, which has non-final fields; not run), and the caches are plain hash sets used from any thread (ICBM-01-67, ICBM-01-68). A debug flag prints the scan as an indented trace.

## 5. Events that exist in 1.12 ICBM

All four are posted to `MAIN_TRACKER`, from packet code (the packet system is a different area; these only report problems). Each type logs through a type-level listener named `icbmclassic:logger`.

| Type (name) | Severity | Columns (data index) | Posted when | Logger output |
|---|---|---|---|---|
| tile packet error (`ERROR_HANDLING`) | error | packet id (0), packet key (1), world id (2), world name (3), tile position x/y/z (4), the exception (5) | A packet threw while writing into a tile | Error log with world and position, plus the exception |
| invalid tile (`INVALID_TILE`) | warn | same without the exception | The packet's target tile is missing or the wrong type | Debug only |
| wrong world (`WRONG_WORLD`) | warn | packet id, packet key, side (client or server), world id, expected world id | A tile packet arrived for a different dimension than the receiver's | Debug only |
| not server world (`NOT_SERVER_WORLD`) | warn | packet id, packet key, world id | A server-side packet's world is not a real server world | Debug only |

Each of these was meant to log only when debug is on (the author passes a condition), but the condition is dropped (ICBM-01-65), and all four share one type name (ICBM-01-69). Packet id and key come from the packet codex; both are null-safe.

## 6. What a rebuild needs

Nothing here is a gameplay mechanic. A rebuild gets the same value from ordinary structured logging at the places listed in section 5. The only idea worth keeping is the **lazy payload**: do not build a diagnostic's data unless a listener is attached. If admin audit of launches is wanted (who fired what at where), post such events from the launcher and missile code and keep the read-only payload rule; ICBM never did, so there is no behavior to match.
