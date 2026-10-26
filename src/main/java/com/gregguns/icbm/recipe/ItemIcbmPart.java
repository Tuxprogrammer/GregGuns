package com.gregguns.icbm.recipe;

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
 * One ICBM hardware part (see {@link IcbmPart}). It has a tier and a tooltip and nothing else; the warhead recipes
 * consume it. It is deliberately not a {@code GregGunsComponent}: the dev self-check would demand its recipe even when
 * the ICBM module is off, and the recipes only exist for an enabled module. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (Parts)
public final class ItemIcbmPart extends Item implements Tiered {

    private final IcbmPart part;

    /** @param part which part this item is */
    public ItemIcbmPart(IcbmPart part) {
        this.part = part;
        setMaxStackSize(64);
    }

    /** @return which part this is */
    public IcbmPart part() {
        return part;
    }

    @Override
    public Tier getTier() {
        return part.tier();
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        boolean shift = net.minecraft.client.gui.GuiScreen.isShiftKeyDown();
        String tierLine = String
            .format(StatCollector.translateToLocal("gregguns.icbm.part.tooltip.tier"), TierText.colored(part.tier()));
        String summary = StatCollector.translateToLocal("gregguns.icbm.part.summary." + part.registryName());
        String usedIn = String.format(
            StatCollector.translateToLocal("gregguns.icbm.part.tooltip.used"),
            StatCollector.translateToLocal(part.usedInKey()));
        String hint = StatCollector.translateToLocal("gregguns.icbm.part.tooltip.shift");
        tooltip.addAll(PartTooltipFormat.lines(tierLine, summary, usedIn, hint, shift));
        if (shift) {
            tooltip.add(AddedBy.line());
        }
    }
}
