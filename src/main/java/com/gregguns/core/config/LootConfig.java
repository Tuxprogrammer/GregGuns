package com.gregguns.core.config;

import com.gtnewhorizon.gtnhlib.config.Config;

/**
 * The world-loot tier cap, the {@code loot} category of {@code config/gregguns.cfg}. Read on the server, not synced.
 * Fields only; no Minecraft types. {@code com.gregguns.worldgen.loot.ObtainTier} reads it.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-loot.md (Loot tier cap); D363, D364
@Config(modid = "gregguns", category = "loot")
public class LootConfig {

    @Config.Comment({
        "Highest GT tier of anything GregGuns puts in the world for a player to take, as a GT voltage index",
        "(1 LV, 2 MV, 3 HV, 4 EV, 5 IV, 6 LuV, 7 ZPM, 8 UV, 9 UHV, 10 UEV, 11 UIV, 12 UMV). An item's tier is what it",
        "takes to make it at all, its whole crafting chain, never the voltage of the recipe that outputs it. It caps",
        "structure and silo complex chests, furniture, furnaces and GT machine outputs in complexes, item frames,",
        "armories and GregGuns NPC drops; the ceilings of planets and Ross128 apply up to it. Quest loot bags and",
        "Vending trades follow their quest tier instead. Raise it for a harder, richer late game. Default: 4 (EV)." })
    @Config.RangeInt(min = 1, max = 12)
    public static int maxTier = 4;
}
