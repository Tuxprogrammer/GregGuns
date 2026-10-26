package com.gregguns.icbm.recipe;

import com.gregguns.api.TierUpgrade;

/**
 * A missile guidance computer. It is a {@link TierUpgrade}: a better computer than its body's tier is the point (each
 * step above gives the body one step of guidance, D24), so the dev self-check does not flag a Mk II computer going
 * into a T1 body. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/build-and-recipes.md (Guidance)
public final class ItemGuidanceComputer extends ItemIcbmPart implements TierUpgrade {

    /** @param part which computer this is */
    public ItemGuidanceComputer(IcbmPart part) {
        super(part);
    }
}
