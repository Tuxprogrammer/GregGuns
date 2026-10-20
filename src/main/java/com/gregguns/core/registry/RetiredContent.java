package com.gregguns.core.registry;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.gregguns.GregGuns;

import cpw.mods.fml.common.event.FMLMissingMappingsEvent;

/**
 * Registry names that earlier GregGuns alphas registered and later builds removed. A world saved with one of them has
 * an id in its registry map that nothing claims any more; without a handler Forge asks the player whether to load the
 * world anyway. Listing the name here makes Forge drop the stack or block silently instead.
 *
 * <p>
 * Design record 0012 retired {@code part_heat_ray_focus} (the Reaction Chamber's non-consumed input). Stacks of it
 * vanish with no refund: the mod is an alpha with no stability promise. The Reaction Chamber and Retort machines are GT
 * MetaTileEntities under GT's own block, so they are not registry names; see {@code GregGunsMTEIDs#retired()}. Keep
 * the names for one alpha, then drop this class. Common side.
 */
// Spec: docs/design/0012-retire-reaction-chamber-and-retort.md (Migration notes for existing worlds)
public final class RetiredContent {

    /** Bare registry names (without {@code gregguns:}) that no longer exist. */
    private static final Set<String> NAMES = Collections
        .unmodifiableSet(new HashSet<>(Arrays.asList("part_heat_ray_focus")));

    private RetiredContent() {}

    /**
     * @param registryName a full registry name such as {@code gregguns:part_heat_ray_focus}
     * @return whether it is a retired GregGuns name that should be dropped without a prompt
     */
    public static boolean isRetired(String registryName) {
        String prefix = GregGuns.MODID + ":";
        return registryName != null && registryName.startsWith(prefix)
            && NAMES.contains(registryName.substring(prefix.length()));
    }

    /** @return the retired bare names, for tests and docs */
    public static Set<String> names() {
        return NAMES;
    }

    /**
     * Ignores every retired name in the event.
     *
     * @param event Forge's missing-mappings event, fired when the world's registry map names something not registered
     */
    public static void handle(FMLMissingMappingsEvent event) {
        for (FMLMissingMappingsEvent.MissingMapping mapping : event.get()) {
            if (isRetired(mapping.name)) {
                // Source: Forge 1.7.10 FMLMissingMappingsEvent.MissingMapping#ignore (drops the entry from the world).
                mapping.ignore();
                GregGuns.LOG.info("Dropped retired item or block {} from the world", mapping.name);
            }
        }
    }
}
