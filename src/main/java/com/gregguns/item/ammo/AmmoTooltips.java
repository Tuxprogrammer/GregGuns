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
     * Adds "Fills magazines" (with the loose items each takes) and "Loaded directly by" (with the rounds one round
     * gives each gun) lines for a loose round.
     *
     * @param tooltip   the tooltip list to append to
     * @param roundName the round's registry name
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void forRound(List tooltip, String roundName) {
        AmmoCompatibility c = AmmoCompatibility.DEFAULT;
        List<String> costs = new ArrayList<>();
        for (AmmoCompatibility.Use use : c.magazineCosts(roundName)) {
            costs.add(
                StatCollector.translateToLocalFormatted("gregguns.tooltip.ammo.cost", name(use.name()), use.count()));
        }
        tooltip.addAll(
            AmmoTooltipFormat.block(
                StatCollector.translateToLocal("gregguns.tooltip.ammo.fills"),
                costs,
                GuiScreen.isShiftKeyDown(),
                StatCollector.translateToLocal("gregguns.tooltip.ammo.more")));
        tooltip.addAll(gunBlock("gregguns.tooltip.ammo.direct", roundName));
    }

    /**
     * Adds "Loaded directly by" for an item guns consume without a magazine (a tank).
     *
     * @param tooltip  the tooltip list to append to
     * @param itemName the item's registry name
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void forDirectAmmo(List tooltip, String itemName) {
        tooltip.addAll(gunBlock("gregguns.tooltip.ammo.direct", itemName));
    }

    /**
     * Adds "Used by" for a magazine, empty or loaded: each gun with the rounds one magazine gives it.
     *
     * @param tooltip  the tooltip list to append to
     * @param magazine the magazine family
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void forMagazine(List tooltip, MagazineType magazine) {
        tooltip.addAll(gunBlock("gregguns.tooltip.magazine.guns", magazine.loadedItemName()));
    }

    /**
     * @param itemName a registry name
     * @return its localized display name
     */
    public static String name(String itemName) {
        return StatCollector.translateToLocal(Naming.itemLangKey(itemName));
    }

    private static List<String> gunBlock(String labelKey, String itemName) {
        List<String> entries = new ArrayList<>();
        for (AmmoCompatibility.Use use : AmmoCompatibility.DEFAULT.roundsPerGun(itemName)) {
            entries.add(
                AmmoTooltipFormat.roundsEntry(
                    name(use.name()),
                    use.count(),
                    StatCollector.translateToLocal("gregguns.tooltip.ammo.rounds_one"),
                    StatCollector.translateToLocal("gregguns.tooltip.ammo.rounds_many")));
        }
        return AmmoTooltipFormat.block(
            StatCollector.translateToLocal(labelKey),
            entries,
            GuiScreen.isShiftKeyDown(),
            StatCollector.translateToLocal("gregguns.tooltip.ammo.more"));
    }
}
