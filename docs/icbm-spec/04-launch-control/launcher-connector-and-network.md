# Launcher Connector and the Launcher Network

The connector block, and the shared network that joins pads, cruise launchers, screens, frames and connectors: how it forms, what it shares (energy and items), and how launchers are grouped and selected for a fire mission. Source root: `src/main/java/icbm/classic/` at `9c1d985a`. Classes in `content/blocks/launcher/network/` and `content/blocks/launcher/connector/`.

## 1. Identity

| Item | Value |
|---|---|
| Block classes | `connector/BlockLaunchConnector`, `connector/TileLauncherConnector` (empty subclass of `TileLauncherFrame`) |
| Network classes | `network/LauncherNetwork`, `LauncherNode`, `LauncherEntry`, `NetworkEnergyStorage`, `NetworkInventory`, `ILauncherComponent` |
| Registry name | block and tile `icbmclassic:launcher_connector` |
| English name | "Launcher Connector" (`tile.icbmclassic:launcher_connector.name`); tooltip "Optional decorative launcher connection block" (wrong, ICBM-04-28) |
| Block properties | iron, hardness 10, resistance 10, not opaque, cutout layer, model render. Six boolean properties `up down north east south west` (computed, *1.12-only mechanism*). No meta (always 0) |
| Oredict | none |

## 2. Obtaining

Shaped, 3 x 3: `i w i` / `w h w` / `i w i`; i = `ingotSteel`, w = `wireCopper`, h = hopper. Result **4** (`recipes/launcher/connector.json`). Recipes belong to `06-blocks-items`.

## 3. Numbers

| Name | Value |
|---|---|
| Block hardness / resistance | 10 / 10 |
| Network size limit | none |
| Energy accepted per receive call | unlimited by the network; each pad buffer holds 1,200 FE |

## 4. Behavior

### The connector block

It does nothing a frame does not. The block differs in looks: each of the six sides shows a "connected" or "disconnected" texture. A side is "connected" when the neighbor on that side has the Forge Energy **or** item-handler capability facing back at the connector. Frames and screens that are on a network count (they forward the network's capabilities); so does any pad, cruise launcher, hopper, chest or cable. So the connector shows where a cable or inventory can attach. Its tile forwards the network's energy and inventory exactly like a frame (see [launcher-frame.md](launcher-frame.md)).

### Joining a network (MIT-portable)

Permalink: [`LauncherNode.java#L51-L90`](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/blocks/launcher/network/LauncherNode.java#L51-L90) and [`LauncherNetwork.java#L34-L97`](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/blocks/launcher/network/LauncherNetwork.java#L34-L97)

Each component owns a **node** (tile, a flag "accepts items", an invalid flag, and the current network or none). The network is never saved; it is rebuilt every time tiles load.

`connectToTiles()` runs on tile load. For each of the six neighbors that is **loaded** and has a valid network component:

| This node | Neighbor | Action |
|---|---|---|
| no network | has one | join the neighbor's network |
| has one | none | pull the neighbor into this network |
| none | none | create a new network containing both |
| has one | a different one | merge |
| same | same | nothing |

A neighbor whose node is flagged invalid is skipped. A lone component with no component neighbors never gets a network.

**Merge** builds a brand-new network, re-adds every node of both, then empties the old one (the other network object is left stale but unreferenced).

**Add** records the node in a hash set (keyed by block position), points the node at the network, and if the tile has the launcher capability, appends a launcher entry (launcher, tile, last status/package/time) and rebuilds the shared inventory slot list.

**Removal** (`onTileRemoved`, called from a block break and from the tile's invalidation): only acts if the node has a network. It marks the node invalid, then rebuilds: clear the whole network, null every node's network, and run `connectToTiles` for every remaining node. The result is correct connected components, at O(n^2) neighbor lookups. Chunk unloads invalidate tiles too, so unloading one tile re-splits the network and the next load reconnects. The cruise launcher tile forgets to do this on invalidation (ICBM-04-10).

### Shared energy (`NetworkEnergyStorage`)

A write-only sink: `receiveEnergy` splits the offered FE across the network's **energy-capable launcher** tiles (pads and cruise launchers; screens and frames are skipped), in hash order:

```
for each launcher k of n, in order:
    offer = min(left, floor(left / (n - k)) + 1)      // the +1 prevents 0 when left < n
    left -= storage.receive(offer)
return total accepted
```

Only what a buffer accepts is subtracted, and what a full buffer refuses is not offered again, so the split is lossy when some pads are full. `extractEnergy` returns 0 and `canExtract` is false. The reported stored and maximum energy are the sums over the launchers. External power can enter through any frame, connector or screen on the network (the screen is the only one whose own capability is overridden by the network, see [launch-screen.md](launch-screen.md)). Each pad spends only its **own** buffer when firing; nothing moves energy between pads (ICBM-04-24 for the split rounding).

### Shared inventory (`NetworkInventory`)

A flat list of slot references: for every node flagged "accepts items" (pads and cruise launchers) whose tile has both the launcher and the item-handler capabilities, every slot of its inventory (missile slot, then battery slot). The list is rebuilt whenever the network gains a launcher or is cleared. Reads, inserts and extracts are forwarded to the real slot, so slot rules apply: only missiles into the missile slot, only energy items into the battery slot. Hoppers and pipes touching any frame or connector can therefore load every pad on the network and also pull missiles and batteries out of any pad (extraction is not blocked). A slot index outside the list throws a runtime exception instead of refusing (ICBM-04-23). Screens do not accept items from the network; they are flagged out to avoid a loop when an inventory node feeds the network from itself.

### Group and selection

Fire missions select launchers from the network's list. `getLaunchers(group)` returns every entry for a negative group or the entries whose launcher's group equals the group. The screen always passes -1, and neither launcher implementation reports a group, so **every launcher on the network always fires**. The pad GUI has group id and index fields that are saved and shown but not connected to anything (ICBM-04-01).

`LauncherNetwork.launch(solution, cause, simulate)`: for each selected launcher, store a fresh firing package (target, cause) as its "last firing" record, stamp the time (wall-clock), call `launcher.launch(solution, cause, simulate)` and store the status. The stream is lazy and the screen consumes it with a counting terminal operation; the order is list order (the order launchers joined). Launchers are not ordered by index, there is no chain-fire spacing beyond each pad's own firing delay, and the stored "last firing" data is not read anywhere in this area.

## 5. Persistence and sync

None for the network. Each member saves itself ([launcher-base.md](launcher-base.md), [cruise-launcher.md](cruise-launcher.md), [launch-screen.md](launch-screen.md)).

## 6. UI

None. The screen is the network's UI.

## 7. Visuals

- `models/block/launcher_connector.json` (parent `cube`): six faces, default texture `blocks/silo_connector/disconnected` (16 x 16). `blockstates/launcher_connector.json` swaps each face to `blocks/silo_connector/connected` (16 x 16) per direction boolean.
- Item model: the same.

## 8. Audio

None.

## 9. Relationships

- [launcher-frame.md](launcher-frame.md): shares the tile class and capabilities.
- [launch-sequence.md](launch-sequence.md): consumes `launch` on the network.
- `01-core`: the energy buffer (`consumePower`, `use_energy`) and the item-handler wrapper.

## 10. Bugs

ICBM-04-01 (groups), -10 (cruise unload), -18 (debug right click), -23 (inventory exceptions), -24 (energy split), -28 (tooltip).

## Lang

`tile.icbmclassic:launcher_connector.name` / `.info`; `gui.icbmclassic:error.no_network` = "Failed to connect to launcher network" (not referenced by code in this area). Network status strings are in [launch-sequence.md](launch-sequence.md).
