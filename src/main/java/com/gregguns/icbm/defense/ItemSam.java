package com.gregguns.icbm.defense;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;
import com.gregguns.core.text.AddedBy;
import com.gregguns.core.text.TierText;
import com.gregguns.icbm.recipe.PartTooltipFormat;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The surface-to-air missile as an item (D15, D92): ammunition for the air-defense battery, nothing else. It is not an
 * explosive and does nothing in hand. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/air-defense-battery.md (SAM)
public final class ItemSam extends Item implements Tiered {

    /** The bare registry name. */
    public static final String NAME = "sam";
    /** SAMs per stack, so a battery holds {@link SamRules#CAPACITY} stacks. */
    public static final int MAX_STACK = 16;

    /** Creates the item; stacks to 16. */
    public ItemSam() {
        setMaxStackSize(MAX_STACK);
    }

    @Override
    public Tier getTier() {
        return Tier.HV;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        boolean shift = net.minecraft.client.gui.GuiScreen.isShiftKeyDown();
        String tierLine = String
            .format(StatCollector.translateToLocal("gregguns.icbm.part.tooltip.tier"), TierText.colored(Tier.HV));
        String summary = StatCollector.translateToLocal("gregguns.icbm.sam.summary");
        String details = StatCollector
            .translateToLocalFormatted("gregguns.icbm.sam.details", (int) SamRules.DAMAGE, (int) SamRules.SPEED);
        String hint = StatCollector.translateToLocal("gregguns.icbm.part.tooltip.shift");
        tooltip.addAll(PartTooltipFormat.lines(tierLine, summary, details, hint, shift));
        if (shift) {
            tooltip.add(AddedBy.line());
        }
    }
}
