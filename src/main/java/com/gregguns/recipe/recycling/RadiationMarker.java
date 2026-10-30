package com.gregguns.recipe.recycling;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The GregGuns components that must irradiate their holder (GGS-06-23) although their recycling data is derived, and
 * the size of the Uranium-235 entry that makes them do so.
 *
 * <p>
 * GT reads an item's radiation from the <b>main</b> material of its {@code ItemData} only, through that material's
 * radioactivity enchantment. The recycling data of these components, worked out from their recipe rows, leads with a
 * metal (Naquadah Alloy, Lead) and leaves out the Uranium-235 dust of their recipes (GGS-06-46), so they read 0.
 * {@link RecyclingDerivation#withRadiationMarker} therefore puts a Uranium-235 entry first as the main material and
 * keeps the composition as the by-products; the component recycling data ({@code ComponentRecipes}) and the derivation
 * pass both call it. The entry
 * is 1/200 of a material unit. That is below a tiny dust and a nugget (1/9) and below one millibucket of molten metal
 * (a unit is 144 mB), so no Macerator, Arc Furnace, furnace or Fluid Extractor recycling recipe can return any uranium,
 * and it is far below the whole plates these components are made of, so GT's own sorting for the recycler outputs puts
 * it last and every plate still comes back. Plain Java, unit tested.
 */
// Spec: docs/gregguns-spec/06-items/hazards-and-tooltips.md (GGS-06-23, GGS-06-24); DECISIONS D293
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTUtility.java#L1971-L1980
// (getRadioactivityLevel reads only ItemData.mMaterial, the main material)
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L608-L638
// (getDust and getIngot return nothing below a ninth of a unit: no dust, tiny dust or nugget)
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTRecipeRegistrator.java#L217-L222
// (the Fluid Extractor recycling recipe reads the main material and stops below one millibucket)
public final class RadiationMarker {

    /** The divisor of GT's material unit for the Uranium-235 entry: {@code M / 200}. */
    public static final int UNIT_DIVISOR = 200;

    /**
     * The registry names (and {@link RecyclingCatalog} keys) that take the marker. The tactical nuke rocket is not
     * here:
     * its data is hand-written Uranium-235 already ({@code AmmoRecycling.nukeRocket}).
     */
    public static final Set<String> NAMES = Collections.unmodifiableSet(
        new LinkedHashSet<>(Arrays.asList("cell_nuclear", "part_nuclear_warhead", "part_rad_emitter")));

    private RadiationMarker() {}

    /**
     * @param key a {@link RecyclingCatalog} key
     * @return whether that stack's derived data leads with the Uranium-235 marker
     */
    public static boolean marks(String key) {
        return NAMES.contains(key);
    }

    /**
     * @param materialUnit GT's material unit ({@code GTValues.M})
     * @return the marker's amount in material units
     */
    public static long amount(long materialUnit) {
        return materialUnit / UNIT_DIVISOR;
    }
}
