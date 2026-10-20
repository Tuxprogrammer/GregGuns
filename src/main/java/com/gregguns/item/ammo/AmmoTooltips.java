package com.gregguns.item.ammo;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;

import com.gregguns.core.registry.Naming;

/**
 * Client glue that turns {@link AmmoCompatibility} into localized tooltip lines. Item names come from the lang files,
 * so a gun or magazine rename follows without a change here. Call from {@code addInformation} only (client side).
 */
// Spec: docs/gregguns-spec/06-items/ammo.md (tooltip compatibility notes)
public final class AmmoTooltips {

    private AmmoTooltips() {}

    /**
     * Adds "Fills magazines" and "Loaded directly by" lines for a loose round.
     *
     * @param tooltip   the tooltip list to append to
     * @param roundName the round's registry name
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void forRound(List tooltip, String roundName) {
        AmmoCompatibility c = AmmoCompatibility.DEFAULT;
        tooltip.addAll(lines("gregguns.tooltip.ammo.fills", c.magazinesFilledWith(roundName)));
        tooltip.addAll(lines("gregguns.tooltip.ammo.direct", c.gunsTaking(roundName)));
    }

    /**
     * Adds "Loaded directly by" for an item guns consume without a magazine (a tank).
     *
     * @param tooltip  the tooltip list to append to
     * @param itemName the item's registry name
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void forDirectAmmo(List tooltip, String itemName) {
        tooltip.addAll(lines("gregguns.tooltip.ammo.direct", AmmoCompatibility.DEFAULT.gunsTaking(itemName)));
    }

    /**
     * Adds "Used by" for a magazine, empty or loaded.
     *
     * @param tooltip  the tooltip list to append to
     * @param magazine the magazine family
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void forMagazine(List tooltip, MagazineType magazine) {
        tooltip.addAll(lines("gregguns.tooltip.magazine.guns", AmmoCompatibility.DEFAULT.gunsForMagazine(magazine)));
    }

    private static List<String> lines(String labelKey, List<String> itemNames) {
        List<String> names = new ArrayList<>(itemNames.size());
        for (String name : itemNames) {
            names.add(StatCollector.translateToLocal(Naming.itemLangKey(name)));
        }
        return AmmoTooltipFormat.lines(
            StatCollector.translateToLocal(labelKey),
            names,
            GuiScreen.isShiftKeyDown(),
            StatCollector.translateToLocal("gregguns.tooltip.ammo.more"));
    }
}
