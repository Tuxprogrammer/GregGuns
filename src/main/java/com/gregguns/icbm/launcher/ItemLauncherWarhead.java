package com.gregguns.icbm.launcher;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;
import com.gregguns.core.text.AddedBy;
import com.gregguns.core.text.TierText;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A launcher warhead (D150): four charges of an ICBM blast type. It does nothing alone; put it in the crafting grid
 * with a launcher it fits ({@link LauncherWarheadRecipe}) and that launcher's next shots detonate the blast instead of
 * their own. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/launcher-warheads.md
public final class ItemLauncherWarhead extends Item implements Tiered {

    private final LauncherWarhead warhead;

    /** @param warhead which one this item is */
    public ItemLauncherWarhead(LauncherWarhead warhead) {
        this.warhead = warhead;
    }

    /** @return which warhead this item is */
    public LauncherWarhead warhead() {
        return warhead;
    }

    @Override
    public Tier getTier() {
        return warhead.tier();
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        boolean shift = net.minecraft.client.gui.GuiScreen.isShiftKeyDown();
        String tierLine = String
            .format(StatCollector.translateToLocal("gregguns.icbm.part.tooltip.tier"), TierText.colored(getTier()));
        List<String> details = new ArrayList<>();
        List<String> gunNames = new ArrayList<>();
        for (String gun : warhead.kind()
            .guns()) {
            gunNames.add(StatCollector.translateToLocal("item.gregguns." + gun + ".name"));
        }
        details.add(
            String.format(
                StatCollector.translateToLocal("gregguns.icbm.launcher.tooltip.fits"),
                LauncherTooltipFormat.gunList(gunNames)));
        // D41: the blast's radius and center damage at fill 1, capped for the launcher (D154).
        details.add(
            String.format(
                StatCollector.translateToLocal("gregguns.icbm.launcher.tooltip.blast"),
                String.format("%.1f", LauncherWarheads.blastRadius(warhead)),
                String.format("%.0f", LauncherWarheads.centerDamage(warhead))));
        details.add(
            String.format(
                StatCollector.translateToLocal("gregguns.icbm.launcher.tooltip.charges"),
                LauncherRules.CHARGES_PER_ITEM,
                LauncherRules.MAX_CHARGES));
        details.add(StatCollector.translateToLocal("gregguns.icbm.launcher.tooltip.how"));
        details.add(StatCollector.translateToLocal("gregguns.icbm.launcher.tooltip.replaces"));
        tooltip.addAll(
            LauncherTooltipFormat.lines(
                tierLine,
                StatCollector.translateToLocal("gregguns.icbm.launcher.summary." + warhead.key()),
                details,
                StatCollector.translateToLocal("gregguns.icbm.part.tooltip.shift"),
                shift));
        if (shift) {
            tooltip.add(AddedBy.line());
        }
    }
}
