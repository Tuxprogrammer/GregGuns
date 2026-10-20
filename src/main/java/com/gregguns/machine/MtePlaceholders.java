package com.gregguns.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

import com.gregguns.GregGuns;

import gregtech.api.GregTechAPI;

/**
 * Fills every GregGuns MTE id that no module registered with an inert {@link MTEPlaceholder}.
 *
 * <p>
 * Why: all GregGuns machines share GT's one machine block. A client whose {@code modules.*} switches differ from the
 * server's passes Forge's registry check (the block and item are GT's), yet has no machine at the id, and GT's item
 * renderer then throws a NullPointerException in the hotbar. GT registers MTEs by id only, so a placeholder is the
 * only way to keep that stack renderable. Common code, no side check: it behaves the same on both sides and runs
 * inside GT's registration window ({@code CommonProxy.init}, after the modules).
 */
// Source (the unregistered-id read that throws):
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/render/GTRendererBlock.java#L207-L209
// Spec: docs/gregguns-spec/08-machines-production/metatileentity-framework.md
public final class MtePlaceholders {

    private MtePlaceholders() {}

    /**
     * @param registered whether an id already holds a machine
     * @return the GregGuns ids that are still free, in {@link GregGunsMTEIDs#all()} order
     */
    public static List<Integer> missing(IntPredicate registered) {
        List<Integer> out = new ArrayList<>();
        for (int id : GregGunsMTEIDs.all()) {
            if (!registered.test(id)) {
                out.add(id);
            }
        }
        return out;
    }

    /** Registers a placeholder at every free GregGuns id. Call once, after all modules' init. */
    public static void registerMissing() {
        for (int id : missing(i -> GregTechAPI.METATILEENTITIES[i] != null)) {
            new MTEPlaceholder(id, "gregguns.placeholder." + id);
            GregGuns.LOG.info("MTE {} has no machine (its module is off here); registered an inert placeholder", id);
        }
    }
}
