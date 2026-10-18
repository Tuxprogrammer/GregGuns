# Village integration

Classes: `worldgen/village/VillageBunker`, `worldgen/village/VillageHandlerTGBunker`.

## Status: not active
The three registration lines in `Techguns.postInit` are commented out:

* `MapGenStructureIO.func_143031_a(VillageBunker.class, "ViTGBunker")`
* creating a `VillageHandlerTGBunker`
* `VillagerRegistry.instance().registerVillageCreationHandler(…)`

So in this version villages never contain Techguns pieces. Techguns also defines **no villager professions, no villager skins and no trade handlers** anywhere in the codebase.

## Intended design (code as written)

### `VillageHandlerTGBunker` (`IVillageCreationHandler`)
* `getVillagePieceWeight` → `PieceWeight(VillageBunker.class, weight 10, max 1 per village)`.
* `getComponentClass` → `VillageBunker`.
* `buildComponent(…)` → `VillageBunker.build(start, pieces, random, x, y, z, coordBaseMode, componentType)`.

### `VillageBunker` (extends vanilla `StructureVillagePieces.House1`)
* **Piece name:** `ViTGBunker` (the name it would be saved under).
* **`build(...)`:** asks vanilla for a component bounding box of size **9 (X) × 9 (Y) × 6 (Z)** at the offered position and orientation. It accepts the piece only if the village may go deeper (`canVillageGoDeeper`) and the box intersects no existing piece.
* **`addComponentParts(world, rnd, bb)`:**
  1. On the first call, measure the average ground level of the piece (vanilla `getAverageGroundLevel`). If that is below 0, give up and return true. Otherwise shift the piece box vertically so that its top is at ground + 8.
  2. Build the camp **Bunker** (catalogue B4) with a random type 0–3, a random direction 0–3, and colour WOODLAND. Its origin is `(bb.minX, bb.minY + 2, bb.minZ)` and its size is `(bb.xSize − 1, bb.ySize − 1, bb.zSize − 1)`.

     The `bb` used here is the **generation clipping box** that vanilla passes in for the current chunk, not the piece's own `boundingBox`. As a result, position and size follow the chunk being populated, and a piece spanning several chunks is built several times (WGEN-13).
  3. Return true. The vanilla `House1` contents (library) are not generated, because the super call is commented out.
