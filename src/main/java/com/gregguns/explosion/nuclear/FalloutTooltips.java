package com.gregguns.explosion.nuclear;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;

import com.gregguns.api.Tier;
import com.gregguns.core.text.AddedBy;
import com.gregguns.core.text.TierText;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Builds the D41 tooltip of a fallout item or block from lang keys: {@code gregguns.fallout.tip.<name>} is the
 * summary and {@code gregguns.fallout.tip.<name>.1} to {@code .3} are the details behind Shift. Client side only.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/nuclear-and-fallout.md (The byproduct chain)
@SideOnly(Side.CLIENT)
final class FalloutTooltips {

    private FalloutTooltips() {}

    /**
     * Adds the tooltip.
     *
     * @param tooltip the list the game shows
     * @param name    the key suffix of the thing, such as {@code trinitite}
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    static void add(List tooltip, String name) {
        boolean shift = GuiScreen.isShiftKeyDown();
        String base = "gregguns.fallout.tip." + name;
        List<String> details = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            if (StatCollector.canTranslate(base + "." + i)) {
                details.add(StatCollector.translateToLocal(base + "." + i));
            }
        }
        tooltip.addAll(
            FalloutTooltipFormat.lines(
                String.format(
                    StatCollector.translateToLocal("gregguns.icbm.part.tooltip.tier"),
                    TierText.colored(Tier.ZPM)),
                StatCollector.translateToLocal(base),
                details,
                StatCollector.translateToLocal("gregguns.icbm.part.tooltip.shift"),
                shift));
        if (shift) {
            tooltip.add(AddedBy.line());
        }
    }
}
