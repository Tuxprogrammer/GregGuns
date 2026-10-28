/**
 * The silo complex layout model (D128): a pure-Java plan of rooms, tunnels and silos in complex-local coordinates,
 * built
 * deterministically from a world seed and grid cell. It carries typed {@link com.gregguns.icbm.complex.Mark marks}
 * that the activity, guard and loot code fills in; it has no Minecraft dependencies.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (D128)
package com.gregguns.icbm.complex;
