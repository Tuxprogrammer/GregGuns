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
 * Why: all GregGuns machines share GT's one machine block, and GT's item renderer throws a NullPointerException in the
 * hotbar for an id with no machine. Module flags no longer skip any machine, so the live case is the retired ids of old
 * worlds (design record 0012); a machine that failed to construct is covered the same way. GT registers MTEs by id
 * only,
 * so a placeholder is the only way to keep such a stack renderable. Common code, no side check: it behaves the same on
 * both sides and runs
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

    /**
     * @param registered whether an id already holds a machine
     * @return the retired ids (design record 0012) that hold nothing yet
     */
    public static List<Integer> retiredMissing(IntPredicate registered) {
        List<Integer> out = new ArrayList<>();
        for (int id : GregGunsMTEIDs.retired()) {
            if (!registered.test(id)) {
                out.add(id);
            }
        }
        return out;
    }

    /** Registers a placeholder at every free GregGuns id. Call once, after all modules' init. */
    public static void registerMissing() {
        for (int id : missing(i -> GregTechAPI.METATILEENTITIES[i] != null)) {
            // Every module always builds its machines (flags only switch behavior), so this is a safety net for a
            // machine that failed to construct, not a normal path.
            new MTEPlaceholder(id, "gregguns.placeholder." + id);
            GregGuns.LOG.warn("MTE {} has no machine; registered an inert placeholder", id);
        }
        // Retired ids (20106 to 20108): a world may still hold the tile entity or an item stack of the removed machine,
        // and GT's item renderer throws on an id with no machine (the Retort crash). The placeholder is inert.
        for (int id : retiredMissing(i -> GregTechAPI.METATILEENTITIES[i] != null)) {
            new MTEPlaceholder(id, "gregguns.retired." + id, true);
            GregGuns.LOG.info("MTE {} is retired; registered an inert placeholder for old worlds", id);
        }
    }
}
