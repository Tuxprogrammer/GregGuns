package com.gregguns.worldgen.nest;

import com.gregguns.core.registry.ModuleRegistrar;

/**
 * Registers the bug-nest blocks (worldgen module): {@code bug_nest} (resin and eggs by meta) and
 * {@code bug_nest_slime}. The textures keep the names the asset generator wrote ({@code bugnestblock},
 * {@code bugnesteggs}, {@code bugnestslimy}). Common side.
 */
// Spec: docs/techguns-spec/11-worldgen/structure-notes/alien_bug_nest.md (block roles)
public final class NestBlockRegistration {

    private static BlockBugNest nest;
    private static BlockBugNestSlime slime;

    /** Registry name of the resin-and-eggs block. */
    public static final String NEST_NAME = "bug_nest";
    /** Registry name of the slime trail block. */
    public static final String SLIME_NAME = "bug_nest_slime";

    private NestBlockRegistration() {}

    /** @return the resin-and-eggs block; null before preInit */
    public static BlockBugNest nest() {
        return nest;
    }

    /** @return the slime trail block; null before preInit */
    public static BlockBugNestSlime slime() {
        return slime;
    }

    /** @param registrar the worldgen module's registrar */
    public static void preInit(ModuleRegistrar registrar) {
        nest = registrar.registerBlock(NEST_NAME, new BlockBugNest(), ItemBlockBugNest.class);
        nest.setBlockTextureName("gregguns:bugnestblock");
        slime = registrar.registerBlock(SLIME_NAME, new BlockBugNestSlime());
        slime.setBlockTextureName("gregguns:bugnestslimy");
    }
}
