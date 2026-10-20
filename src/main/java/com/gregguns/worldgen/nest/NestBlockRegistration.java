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
        nest = registrar.registerBlock("bug_nest", new BlockBugNest(), ItemBlockBugNest.class);
        nest.setBlockTextureName("gregguns:bugnestblock");
        slime = registrar.registerBlock("bug_nest_slime", new BlockBugNestSlime());
        slime.setBlockTextureName("gregguns:bugnestslimy");
    }
}
