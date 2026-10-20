package com.gregguns.worldgen.layout;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

/** Test doubles: a block per registry name, and PaletteKeys built over them with Chisel absent. */
final class TestKeys {

    private TestKeys() {}

    /** Hands out one distinct Block per name, so tests can tell which key a write came from. */
    static final class Blocks implements PaletteKeys.BlockLookup {

        final Map<String, Block> byName = new HashMap<>();

        @Override
        public Block byName(String registryName) {
            if (registryName.startsWith("minecraft:no_")) {
                return null;
            }
            return byName.computeIfAbsent(registryName, n -> new Block(Material.rock) {});
        }
    }

    static PaletteKeys chiselAbsent(Blocks blocks) {
        return PaletteKeys.resolve(null, blocks, s -> {});
    }
}
